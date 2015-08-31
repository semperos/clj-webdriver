(ns webdriver.monad
  "Monadic API for clj-webdriver"
  (:require [clojure.algo.monads :refer [defmonad domonad maybe-t
                                         m-bind m-chain
                                         m-lift m-result
                                         maybe-m
                                         monad-transformer
                                         state-m with-monad]]
            [clojure.string :refer [join]]
            [webdriver.core :as wd])
  (:import clojure.lang.ExceptionInfo
           [org.openqa.selenium WebDriver WebElement]))

(defn ensure-webdriver
  "Accept either a map with `:webdriver` or assume the thing is a WebDriver."
  [driver]
  (if (instance? WebDriver driver)
    driver
    (:webdriver driver)))

(declare wait-element)
(defn ensure-element
  "Make it possible to pass in a WebElement or a selector that `webdriver.core/find-element` can find."
  [{:keys [webdriver] :as driver} selector]
  (cond
    (instance? WebElement selector) selector
    (:wait? driver) (wait-element driver selector)
    ;; TODO these raw wd/find-element calls likely need to be in the wait-until function
    :else (wd/find-element webdriver selector)))

(defn driver
  "This is the base state for the WebDriver monads. It includes an entry for the underlying WebDriver object that does the heavy lifting, as well as configuration options for various features inside the monad."
  ([^WebDriver webdriver] (driver webdriver {}))
  ([^WebDriver webdriver {:keys [record-history?
                                 wait?
                                 wait-pred
                                 wait-timeout
                                 wait-interval
                                 wait-throws?]
                          :or {record-history? true
                               wait? false
                               wait-pred (fn [driver element] (wd/find-element (ensure-webdriver driver) element))
                               wait-timeout wd/wait-timeout
                               wait-interval wd/wait-interval
                               wait-throws? true}
                          :as opts}]
   (cond-> (assoc opts :webdriver webdriver)
     record-history? (assoc :history []))))

(defmulti format-arg :type)

(defmethod format-arg WebElement
  [element]
  (str "WebElement<"
   (pr-str
    (cond-> (:tag element)
      (:id element) (str "#" (:id element))
      (:class element) (str "." (:class element))))
   ">"))

(defmethod format-arg :default
  [arg] (pr-str arg))

(defn format-args
  [args]
  (join ", " (map format-arg args)))

(defn format-step
  [idx item]
  (let [num (inc idx)
        action (:name (meta (:action item)))]
    (str " " num ". Called " action
         (when-let [args (:args item)]
           (str " with " (format-args (:args item)))))))

(defn format-history
  "Format the history of a driver in a human-readable way, limited to `n` steps. The `steps` is a vector of maps."
  ([steps] (format-history 5 steps))
  ([n steps]
   (let [steps (if (> (count steps) n)
                 (subvec steps (- (count steps) n))
                 steps)]
     (->> steps
          (map-indexed format-step)
          (join "\n")))))

(defn handle-webdriver-error
  [throwable monadic-value driver]
  (let [history (:history driver)
        msg "WebDriver error."
        msg (if history
              (str msg " The last few steps in your test were:\n"
                   (format-history history))
              msg)
        msg (str "\nLast attempted action: " monadic-value)]
    (ex-info msg
             (cond-> {:webdriver driver
                      :attempted-action monadic-value}
               (:history driver) (assoc :history (:history driver)))
             throwable)))

;;;;;;;;;;;;
;; Monads ;;
;;;;;;;;;;;;

(def webdriver-m
  "At its simplest, the WebDriver monad can be seen as a simple State monad."
  state-m)

(def webdriver-maybe-m
  "The simple WebDriver stateful monad extended with maybe semantics."
  (maybe-t webdriver-m))

(defmonad webdriver-error-m
   "The simple WebDriver stateful monad extended with error-handling semantics."
   [m-result (fn m-result-state [v]
               (fn [driver] [v driver]))
    ;; Since this is the state monad, `mv` is the function
    ;; which accepts a state (named `driver` here) and returns
    ;; a monadic value.
    m-bind (fn m-bind-state [mv f]
             (fn [driver]
               (let [results (try (mv driver)
                                  (catch Throwable e
                                    (handle-webdriver-error e mv driver)))]
                 (if (instance? ExceptionInfo results)
                   [results driver]
                   ((f (first results)) (second results))))))])

(def webdriver-maybe-error-m
  "The stateful WebDriver monad extended with maybe and error-handling semantics."
  (maybe-t webdriver-error-m))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generic Monad Utilities ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn steps-as-bindings
  "Receives a collection of Clojure forms as `steps`. Statements are standalone, but expressions with bindings are written `a-name <- monadic-application`. This function transforms this collection into a vector of bindings that `domonad` will accept as its steps."
  [steps]
  ;; Support optional Haskell-style `return` for result expression
  (loop [steps (if (= (last steps) 'return) (butlast steps) steps)
         bindings []]
    (if-not (seq steps)
      ;; Return where last thing handled was a `<-` binding
      bindings
      (if (= (count steps) 2)
        ;; Return where last things handled are statements
        (conj bindings
              (gensym "step") (first steps)
              (gensym "step") (second steps))
        (if (= (count steps) 1)
          ;; Return where last thing handled is a statement
          (conj bindings (gensym "step") (first steps))
          ;; Handle either single statement or Haskell-style `name <- value` syntax
          (if (= (nth steps 1) '<-)
            ;; Binding
            (recur (drop 3 steps)
                   (conj bindings (nth steps 0) (nth steps 2)))
            ;; Statement
            (recur (rest steps)
                   (conj bindings (gensym "step") (first steps)))))))))

(defn return-expr
  "Given a sequence of steps, return the \"return\" expression of the monadic computation. This is either a simple last value or a list of `(return <value>)`."
  [steps]
  (let [expr (last steps)]
    (if (and (list? expr)
             (= (first expr) 'return))
      (second expr)
      expr)))

(defmacro drive-in
  "No, not a movie theater. Drive the browser within the given monad. Uses `domonad` under the covers."
  [name & steps]
  (let [do-steps (steps-as-bindings (butlast steps))
        expr (return-expr steps)]
    `(domonad ~name ~do-steps ~expr)))

(defmacro drive
  "Default `drive-in` usage with `webdriver-error-m` monad. Uses `domonad` under the covers.

Example:

```
(let [test (drive (to \"http://example.com\")
                  button <- (find-element {:css \"#login\"})
                  (click button)
                  (current-url))]
  (test my-driver))
```
 "
  [& steps]
  (let [name 'webdriver-error-m
        do-steps (steps-as-bindings (butlast steps))
        expr (return-expr steps)]
    `(domonad ~name ~do-steps ~expr)))

(defmacro drive-with
  "Like `drive` but expects the starting value upfront."
  [value & steps]
  `((drive ~@steps) ~value))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Monadic WebDriver API ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn history
  "If history is enabled in the driver state, append a map with `action` and `args` to it."
  ([driver action] (history driver action nil))
  ([driver action args]
   (if (:history driver)
     (update-in driver [:history] conj {:action action
                                        :args args})
     driver)))

(defn ->element
  "Partially serialize a WebElement into a Clojure map that captures important values. Intended for human consumption at this point."
  [^WebElement element]
  {:type WebElement
   :tag (.getTagName element)
   :location (.getLocation element)
   :class (.getAttribute element "class")
   :id (.getAttribute element "id")})

;; TODO Determine if predicate should accept "driver" or WebDriver
(defn wait-element
  "Implicitly wait for an element before moving on. Uses Selenium-WebDrivers _explicit_ waiting functionality, but is designed to be configured globally (implicitly) for the driver being used."
  [{:keys [webdriver wait-pred wait-timeout wait-interval] :as driver} selector]
  (wd/wait-until webdriver
                 (fn [_]
                   (wait-pred driver selector))
                 wait-timeout
                 wait-interval))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Essentially three types of WebDriver functions: ;;
;;                                                 ;;
;;  1. Driver action for side effects              ;;
;;  2. Driver action for primitive value           ;;
;;  3. Driver action for WebElement                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn copy-docs
  [var-name]
  (let [var-here (resolve var-name)
        var-there (ns-resolve 'webdriver.core var-name)]
    (alter-meta! var-here assoc :doc (:doc (meta var-there)))))

(defn wait-until
  "Set an explicit wait time `timeout` for a particular condition `pred`. Optionally set an `interval` for testing the given predicate. All units in milliseconds."
  ([m-pred] (wait-until m-pred wd/wait-timeout))
  ([m-pred timeout] (wait-until m-pred timeout wd/wait-interval))
  ([m-pred timeout interval]
   (fn [driver]
     (let [webdriver (ensure-webdriver driver)
           value :void
           driver (history driver #'wait-until [pred timeout interval])]
       ;; (wait-until (fn [_driver_] (find-element "//a[text()='Sign in']")))
       ;; Two choices:
       ;;  * I provide monadic equivalents for common predicates like =, >, <, etc.
       ;;  * I make the user provide additional argument: one raw predicate, one monadic expression
       (wd/wait-until webdriver
                      (fn wrapped-predicate [this-driver]
                        ((m-pred))
                        ;; (= (current-url) "https://github.com/login")
                        pred) timeout interval)
       [value driver]))))

(defn back
  ([driver] (wd/back (ensure-webdriver driver)))
  ([]
   (fn [driver]
     (let [webdriver (ensure-webdriver driver)
           value :void
           driver (history driver #'back)]
       (wd/back webdriver)
       [value driver]))))
(copy-docs 'back)

(defn forward
  ([driver] (wd/forward (ensure-webdriver driver)))
  ([]
   (fn [driver]
     (let [webdriver (ensure-webdriver driver)
           value :void
           driver (history driver #'forward)]
       (wd/forward webdriver)
       [value driver]))))
(copy-docs 'forward)

(defn to
  ([driver url] (wd/to (ensure-webdriver driver) url))
  ([url]
   (fn [driver]
     (let [webdriver (ensure-webdriver driver)
           value :void
           driver (history driver #'to [url])]
       (wd/to webdriver url)
       [value driver]))))
(copy-docs 'to)

(defn current-url
  ([driver] (wd/current-url (ensure-webdriver driver)))
  ([]
   (fn [driver]
     (let [webdriver (ensure-webdriver driver)
           value (wd/current-url webdriver)
           driver (history driver #'current-url)]
       [value driver]))))
(copy-docs 'current-url)

(defn page-source
  ([driver] (wd/page-source (ensure-webdriver driver)))
  ([]
   (fn [driver]
     (let [webdriver (ensure-webdriver driver)
           value (wd/page-source webdriver)
           driver (history driver #'page-source)]
       [value driver]))))
(copy-docs 'page-source)

(defn title
  ([driver] (wd/title (ensure-webdriver driver)))
  ([]
   (fn [driver]
     (let [webdriver (ensure-webdriver driver)
           value (wd/title webdriver)
           driver (history driver #'title)]
       [value driver]))))
(copy-docs 'title)

(defn find-element
  ([driver selector] (wd/find-element (ensure-webdriver driver) selector))
  ([selector]
   (fn [driver]
     (let [webdriver (ensure-webdriver driver)
           value (wd/find-element webdriver selector)
           driver (history driver #'find-element [selector])]
       [value driver]))))
(copy-docs 'find-element)

;; TODO Figure out non-monadic element-only functions
(defn click
  [element]
  (fn [driver]
    (let [element (ensure-element driver element)
          value :void
          driver (history driver #'click [(->element element)])]
      (wd/click element)
      [value driver])))
(copy-docs 'click)

(defn send-keys [element text]
  (fn [driver]
    (let [element (ensure-element driver element)
          value :void
          driver (history driver #'send-keys [(->element element) text])]
      (wd/send-keys element text)
      [value driver])))
(copy-docs 'send-keys)


;; Usage
(comment
  (import 'org.openqa.selenium.firefox.FirefoxDriver)
  ;; Create driver (map of WebDriver and history)
  (def d (driver (FirefoxDriver.)))

  ;; Using `domonad`
  (let [test (domonad webdriver-error-m
                      [_ (to "https://github.com")
                       url-a (current-url)
                       sign-in (find-element {:tag :a :text "Sign in"})
                       _ (click sign-in)
                       url-b (current-url)
                       login (find-element {:tag :input :id "login_field"})
                       password (find-element {:tag :input :id "password"})
                       _ (send-keys login "MR.GITHUB")
                       _ (send-keys password "WHO KNOWS?")]
                      {:url-a url-a
                       :url-b url-b})
        [result final-driver] (test d)]
    [result final-driver])

  ;; Using custom `drive` macro with Haskell-style syntax
  (let [test (drive
              (to "https://github.com")
              url-a <- (current-url)
              sign-in <- (find-element {:tag :a :text "Sign in"})
              (click sign-in)
              url-b <- (current-url)
              login <- (find-element {:tag :input :id "login_field"})
              password <- (find-element {:tag :input :id "password"})
              (send-keys login "MR.GITHUB")
              (send-keys password "WHO KNOWS?")
              ;; Can optionally use `(return <form>)`
              ;; or even `return <form>` as in Haskell
              {:url-a url-a
               :url-b url-b})]
    (test d))

  ;; String selectors instead of explicit find-element calls
  (let [test (drive
              (to "https://github.com")
              url-a <- (current-url)
              (click "//a[text()='Sign in']")
              url-b <- (current-url)
              (send-keys "input#login_field" "MR.GITHUB")
              (send-keys "input#password" "WHO KNOWS?")
              {:url-a url-a
               :url-b url-b})]
  (test d))
  )
