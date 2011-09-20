;; # Clojure Wrapper for Selenium-WebDriver #
;;
;; WebDriver is a library that allows for easy manipulation of the Firefox,
;; Chrome, Safari and  Internet Explorer graphical browsers, as well as the
;; Java-based HtmlUnit headless browser.
;;
;; This library provides both a thin wrapper around WebDriver and a more
;; Clojure-friendly API for finding elements on the page and performing
;; actions on them. See the README for more details.
;;
;; Credits to mikitebeka's `webdriver-clj` project on Github for a starting-
;; point for this project and many of the low-level wrappers around the
;; WebDriver API.
;;
(ns clj-webdriver.core
  (:use [clj-webdriver util record])
  (:require [clj-webdriver.js.browserbot :as browserbot-js] :reload)
  (:import [clj_webdriver.record WindowHandle]
           [org.openqa.selenium By WebDriver WebElement Cookie
                                NoSuchElementException]
           [org.openqa.selenium.firefox FirefoxDriver]
           [org.openqa.selenium.ie InternetExplorerDriver]
           [org.openqa.selenium.chrome ChromeDriver]
           [org.openqa.selenium.htmlunit HtmlUnitDriver]
           [org.openqa.selenium.support.ui Select WebDriverWait ExpectedCondition]
           [java.util Date]
           [java.io File]
           [java.util.concurrent TimeUnit]))


;; ## Driver Management ##
(def webdriver-drivers
  {:firefox FirefoxDriver
   :ie InternetExplorerDriver
   :chrome ChromeDriver
   :htmlunit HtmlUnitDriver})

(defn new-driver
  "Create new driver instance given a browser type. If an additional profile object or string is passed in, Firefox will be started with the given profile instead of the default."
  ([browser]
     (.newInstance (webdriver-drivers browser)))
  ([browser profile]
     (when (not= :firefox browser)
       (throw (IllegalArgumentException. "Only Firefox supports profiles")))
     (FirefoxDriver. profile)))

(defn implicit-wait
  "Specify the amount of time the `driver` should wait when searching for an element if it is not immediately present. This setting holds for the lifetime of the driver across all requests. Units in milliseconds."
  [driver timeout]
  (.implicitlyWait (.. driver manage timeouts) timeout TimeUnit/MILLISECONDS))

(defn wait-until
  "Set an explicit wait time `timeout` for a particular condition `pred`. Optionally set an `interval` for testing the given predicate. All units in milliseconds"
  [driver pred & {:keys [timeout, interval] :or {timeout 5000, interval 0}}]
  (let [wait (WebDriverWait. driver (/ timeout 1000) interval)]
    (.until wait (proxy [ExpectedCondition] []
                   (apply [d] (pred d))))))


;; ## Browser Basics ##
(defn get-url
  "Navigate the driver to a given URL"
  [driver url]
  (.get driver url))

(defn start
  "Shortcut to instantiate a driver, navigate to a URL, and return the driver for further use"
  [browser url]
  (let [d (new-driver browser)]
    (do
      (get-url d url)
      d)))

(defn current-url
  "Retrieve the URL of the current page"
  [driver]
  (.getCurrentUrl driver))

(defn title
  "Retrieve the title of the current page as defined in the `head` tag"
  [driver]
  (.getTitle driver))

(defn page-source
  "Retrieve the source code of the current page"
  [driver]
  (.getPageSource driver))

(declare window-handles*)
(declare window-handle*)
(declare switch-to-window)
(defn close
  "Close this browser instance, switching to an active one if more than one is open"
  [driver]
  (let [handles (window-handles* driver)]
    (if (> (count handles) 1) ; get back to a window that is open before proceeding
      (let [this-handle (window-handle* driver)
            idx (.indexOf handles this-handle)]
        (cond
            (zero? idx) (do ; if first window, switch to next
                          (.close driver)
                          (switch-to-window driver (nth handles (inc idx))))
            :else (do ; otherwise, switch back one window
                    (.close driver)
                    (switch-to-window driver (nth handles (dec idx))))))
      (.close driver))))

;; TODO catch webdriver exception (not consistent)
(defn quit
  "Destroy this browser instance"
  [driver]
  (.quit driver))

;; ## Navigation ##
(defn back
  "Go back to the previous page in \"browsing history\""
  [driver]
  (.back (.navigate driver)))

(defn forward
  "Go forward to the next page in \"browsing history\"."
  [driver]
  (.forward (.navigate driver)))

(defn to
  "Navigate to a particular URL. Arg `url` can be either String or java.net.URL. Equivalent to the `get` function, provided here for compatibility with WebDriver API."
  [driver url]
  (.to (.navigate driver) url)
  driver)

(defn refresh
  "Refresh the current page"
  [driver]
  (.refresh (.navigate driver))
  driver)

;; ## TargetLocator Interface (Windows, Frames) ##

(load "core_window")

;; ## Option Interface ##

(load "core_cookie")

;; ## By* Functions ##

(load "core_by")

;; ##  Actions on WebElements ##
(declare execute-script)
(defn- browserbot
  [driver fn-name & arguments]
  (let [script (str browserbot-js/script
                    "return browserbot."
                    fn-name
                    ".apply(browserbot, arguments)")
        execute-js-fn (partial execute-script driver script)]
    (apply execute-js-fn arguments)))

(defn click
  "Click a particular HTML element"
  [element]
  (.click element))

(defn submit
  "Submit the form which contains the given element object"
  [element]
  (.submit element))

(defn value
  "Retrieve the `value` attribute of the given element object"
  [element]
  (.getAttribute element "value"))

(defn clear
  "Clear the contents of the given element object"
  [element]
  (.clear element)
  element)

(defn tag-name
  "Retrieve the name of the HTML tag of the given element object"
  [element]
  (.getTagName element))

(defn attribute
  "Retrieve the value of the attribute of the given element object"
  [element attr]
  (.getAttribute element (name attr)))

(defn selected?
  "Returns true if the given element object is selected"
  [element]
  (.isSelected element))

(defn select
  "Select a given element object"
  [element]
  (.click element)
  element)

(defn toggle
  "If the given element object is a checkbox, this will toggle its selected/unselected state. In Selenium 2, `.toggle()` was deprecated and replaced in usage by `.click()`."
  [element]
  (.click element)
  element)

(defn deselect
  "Deselect a given element object"
  [element]
  (if (.isSelected element)
    (toggle element)
    element))

(defn enabled?
  "Returns true if the given element object is enabled"
  [element]
  (.isEnabled element))

(defmacro exists?
  "Returns element matching the find-it-form if it exists, or nil if it does not"
  [find-it-form]
  `(try 
      ~find-it-form
      (catch org.openqa.selenium.NoSuchElementException e#
        nil)))

(defn visible?
  "Returns true if the given element object is visible/displayed"
  [element]
  (.isDisplayed element))

(def displayed? ^{:doc "Returns true if the given element object is visible/displayed"} visible?)

(defn present?
  "Returns true if element exists and is visible"
  [element]
  (and element (visible? element)))

(defn flash
  "Flash the element in question, to verify you're looking at the correct element"
  [element]
  (let [original-color (if (.getValueOfCssProperty element "background-color")
                         (.getValueOfCssProperty element "background-color")
                         "transparent")
        orig-colors (repeat original-color)
        change-colors (interleave (repeat "red") (repeat "blue"))]
    (doseq [flash-color (take 12 (interleave change-colors orig-colors))]
      (execute-script (.getWrappedDriver element)
                      (str "arguments[0].style.backgroundColor = '"
                           flash-color "'")
                      element)
      (Thread/sleep 80)))
  element)

(defn text
  "Retrieve the content, or inner HTML, of a given element object"
  [element]
  (.getText element))

(defn html
  "Retrieve the outer HTML of an element"
  [element]
  (browserbot (.getWrappedDriver element) "getOuterHTML" element))

(defn xpath
  "Retrieve the XPath of an element"
  [element]
  (browserbot (.getWrappedDriver element) "getXPath" element []))

(defn focus
  "Apply focus to the given element"
  [element]
  (execute-script
   (.getWrappedDriver element) "return arguments[0].focus()" element))

(defn send-keys
  "Type the string of keys into the element object"
  [element s]
  (.sendKeys element (into-array CharSequence (list s)))
  element)

(def input-text send-keys)

(defn location
  "Given an element object, return its location as a map of its x/y coordinates"
  [element]
  (let [loc (.getLocation element)
        x   (.x loc)
        y   (.y loc)]
    {:x x, :y y}))

(defn location-once-visible
  "Given an element object, return its location on the screen once it is scrolled into view as a map of its x/y coordinates. The window will scroll as much as possible until the element hits the top of the page; thus even visible elements will be scrolled until they reach that point."
  [element]
  (let [loc (.getLocationOnScreenOnceScrolledIntoView element)
        x   (.x loc)
        y   (.y loc)]
    {:x x, :y y}))

(defn drag-and-drop-by
  "Drag an element by `x` pixels to the right and `y` pixels down. Use negative numbers for opposite directions."
  [element ^Integer x ^Integer y]
  (.dragAndDropBy element x y)
  element)

(defn drag-and-drop-on
  "Drag `element-a` onto `element-b`. The (0,0) coordinates (top-left corners) of each element are aligned."
  [element-a element-b]
  (.dragAndDropOn element-a element-b)
  element-a)

;; ## JavaScript Execution ##
(defn execute-script
  [driver js & js-args]
  (.executeScript driver js (to-array js-args)))

;; TODO: Script Timeout (wait functionality)

;; ## Select Helpers ##

(load "core_select")

;; ## Element-finding Utilities ##

;; Helper functions kept in separate file yet in same namespace
;; because of interdependence on `find-them` function
(load "core_find")

(defn find-them*
  "Given a browser `driver`, return the elements that match the query"
  ([driver attr-val]
     (cond
      (= attr-val :button*)   (find-them driver :button* nil)
      (keyword? attr-val)     (find-elements
                               driver
                               (by-tag-name (name attr-val))) ; supplied just :tag
      (vector? attr-val)      (cond
                               (some #{:row :col} attr-val) (find-table-cells driver attr-val)
                               (query-with-ancestry-has-regex? attr-val) (if (query-with-ancestry-has-regex? (drop-last 2 attr-val))
                                                                           (throw (IllegalArgumentException.
                                                                                   (str "You may not pass in a regex until "
                                                                                        "the last attribute-value pair")))
                                                                           (filter-elements-by-regex
                                                                            (find-elements driver (by-xpath (str (build-xpath-with-ancestry attr-val) "//*")))
                                                                            (last attr-val)))
                               :else (find-elements driver (by-xpath (build-xpath-with-ancestry attr-val)))) ; supplied vector of queries in hierarchy
      (map? attr-val)         (find-them driver :* attr-val))) ; no :tag specified, use global *
  ([driver tag attr-val]
     (when (keyword? driver) ; I keep forgetting to pass in the WebDriver instance while testing
       (throw (IllegalArgumentException.
               (str "The first parameter to find-them must be an instance of WebDriver."))))
     (cond
      (and (> (count attr-val) 1)
           (contains? attr-val :xpath))          (find-them driver :* {:xpath (:xpath attr-val)})
      (and (> (count attr-val) 1)
           (contains? attr-val :css))            (find-them driver :* {:css (:css attr-val)})
      (contains? attr-val :tag-name)             (find-them driver
                                                            (-> (:tag-name attr-val)
                                                                .toLowerCase
                                                                keyword)
                                                            (dissoc attr-val :tag-name))
      (contains? attr-val :index)                (find-elements driver (by-xpath (build-xpath tag attr-val)))
      (= tag :radio)                             (find-them driver :input (assoc attr-val :type "radio"))
      (= tag :checkbox)                          (find-them driver :input (assoc attr-val :type "checkbox"))
      (= tag :textfield)                         (find-them driver :input (assoc attr-val :type "text"))
      (= tag :password)                          (find-them driver :input (assoc attr-val :type "password"))
      (= tag :filefield)                         (find-them driver :input (assoc attr-val :type "file"))
      (and (= tag :input)
           (contains? attr-val :type)
           (or (= "radio" (:type attr-val))
               (= "checkbox" (:type attr-val)))
           (or (contains? attr-val :text)
               (contains? attr-val :label)))     (find-checkables-by-text driver attr-val)
      (= tag :window)                            (find-window-handles driver attr-val)
      (= tag :button*)                           (if (contains-regex? attr-val)
                                                   (find-semantic-buttons-by-regex driver attr-val)
                                                   (find-semantic-buttons driver attr-val))
      (= 1 (count attr-val))                     (let [entry (first attr-val)
                                                       attr  (key entry)
                                                       value (val entry)]
                                                   (cond
                                                    (= :xpath attr) (find-elements driver (by-xpath value))
                                                    (= :css attr)   (find-elements driver (by-css-selector value))
                                                    (= java.util.regex.Pattern (class value)) (find-elements-by-regex-alone driver tag attr-val)
                                                    :else           (find-elements driver (by-attr= tag attr value))))
      (contains-regex? attr-val)                 (find-elements-by-regex driver tag attr-val)
      :else                                      (find-elements driver (by-xpath (build-xpath tag attr-val))))))

(defn find-them
  "Call find-them*, then make sure elements are actually returned; if not, throw NoSuchElementException so other code can handle exceptions appropriately"
  ([driver attr-val]
     (let [elts (find-them* driver attr-val)]
       (if-not (seq elts)
         (throw (NoSuchElementException.
                 (str "No element with attributes "
                      attr-val " "
                      "could be found on the page:\n"
                      (page-source driver))))
         elts)))
  ([driver tag attr-val]
     (let [elts (find-them* driver tag attr-val)]
       (if-not (seq elts)
         (throw (NoSuchElementException.
                 (str "No element with tag "
                      tag " and attributes "
                      attr-val " "
                      "could be found on the page:\n"
                      (page-source driver))))
         elts))))

(defn find-it
  "Call (first (find-them args))"
  ([driver attr-val]
     (first (find-them driver attr-val)))
  ([driver tag attr-val]
     (first (find-them driver tag attr-val))))
