(ns clj-webdriver.util
  (:require [clojure.string :as str]
            [clj-webdriver.cache :as cache])
  (:import clj_webdriver.driver.Driver
           [org.openqa.selenium WebDriver WebElement]
           java.io.Writer))

(defn build-xpath-attrs
  "Given a map of attribute-value pairs, build the bracketed portion of an XPath query that follows the tag"
  [attr-val]
  (apply str (for [[attr value] attr-val]
               (cond
                (= :text attr) (str "[text()=\"" value "\"]")
                (= :index attr) (str "[" (inc value) "]") ; in clj-webdriver,
                :else (str "[@"                           ; all indeces 0-based
                           (name attr)
                           "="
                           "'" value "']")))))

(declare contains-regex?)
(defn build-xpath
  "Given a tag and a map of attribute-value pairs, generate XPath"
  [tag attr-val]
  (when-not (contains-regex? attr-val)
    (let [attr-val (dissoc attr-val :tag)]
      (str "//"
           (name tag)
           (when-not (empty? attr-val)
             (build-xpath-attrs attr-val))))))

(defn build-xpath-with-ancestry
  "Given a vector of queries in hierarchical order, create XPath.
   For example: `[:div {:id \"content\"}, :a {:class \"external\"}]` would
   produce the XPath \"//div[@id='content']//a[@class='external']"
  [attr-val]
  (apply str (let [tag-to-attrs (partition 2 attr-val)]
               (for [xpath-parts tag-to-attrs]
                 (build-xpath (first xpath-parts) (second xpath-parts))))))

(defn contains-regex?
  "Checks if the values of a map contain a regex"
  [m]
  (boolean (some (fn [entry]
                   (let [[k v] entry]
                     (= java.util.regex.Pattern (class v)))) m)))

(defn all-regex?
  "Checks if all values of a map are regexes"
  [m]
  (and (not (empty? m))
       (not (some (fn [entry]
                    (let [[k v] entry]
                      (not= java.util.regex.Pattern (class v)))) m))))

(defn query-with-ancestry-has-regex?
  "Check if any values in maps as part of ancestry-based query have a regex"
  [v]
  (let [maps (flatten (for [chunk (partition 2 v)]
                        (second chunk)))]
    (boolean (some true? (for [m maps]
                           (contains-regex? m))))))

(defn first-60
  "Get first twenty characters of `s`, then add ellipsis"
  [s]
  (str (re-find #"(?s).{1,60}" s)
       (if (> (count s) 60)
         "..."
         nil)))

(defn elim-breaks
  "Eliminate line breaks; used for REPL printing"
  [s]
  (str/replace s #"(\r|\n|\r\n)" "  "))

;; borrowed from Clojure's 1.2 Contrib
(defn call-method
  [klass method-name params obj & args]
  (-> klass (.getDeclaredMethod (name method-name)
                                (into-array Class params))
      (doto (.setAccessible true))
      (.invoke obj (into-array Object args))))

(defmacro when-attr
  "Special `when` macro for checking if an attribute isn't available or is an empty string"
  [obj & body]
  `(when (not (or (nil? ~obj) (empty? ~obj)))
     ~@body))

;; from Clojure's core.clj
(defmacro assert-args
  [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                  (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
     ~(let [more (nnext pairs)]
        (when more
          (list* `assert-args more)))))

;; from Clojure's core.clj
(defn pr-on
  {:private true
   :static true}
  [x w]
  (if *print-dup*
    (print-dup x w)
    (print-method x w))
  nil)

;; from Clojure core_print.clj
(defn- print-sequential [^String begin, print-one, ^String sep, ^String end, sequence, ^Writer w]
  (binding [*print-level* (and (not *print-dup*) *print-level* (dec *print-level*))]
    (if (and *print-level* (neg? *print-level*))
      (.write w "#")
      (do
        (.write w begin)
        (when-let [xs (seq sequence)]
          (if (and (not *print-dup*) *print-length*)
            (loop [[x & xs] xs
                   print-length *print-length*]
              (if (zero? print-length)
                (.write w "...")
                (do
                  (print-one x w)
                  (when xs
                    (.write w sep)
                    (recur xs (dec print-length))))))
            (loop [[x & xs] xs]
              (print-one x w)
              (when xs
                (.write w sep)
                (recur xs)))))
        (.write w end)))))

;; from Clojure core_print.clj
(defn- print-map [m print-one w]
  (print-sequential 
   "{"
   (fn [e  ^Writer w]
     (do (print-one (key e) w) (.append w \space) (print-one (val e) w)))
   ", "
   "}"
   (seq m) w))

;; from Clojure core_print.clj
(defn- print-meta [o, ^Writer w]
  (when-let [m (meta o)]
    (when (and (pos? (count m))
               (or *print-dup*
                   (and *print-meta* *print-readably*)))
      (.write w "^")
      (if (and (= (count m) 1) (:tag m))
          (pr-on (:tag m) w)
          (pr-on m w))
      (.write w " "))))

;; We check the cache at this point, so that interactive REPL dev
;; isn't interrupted at every page load with exceptions due to
;; missing WebElement's from the WebDriver's default cache
(defmethod print-method clj_webdriver.driver.Driver [r, ^Writer w]
  (cache/check-status r)
  (print-meta r w)
  (.write w "#")
  (.write w (.getName (class r)))
  (print-map r pr-on w))

(defmethod print-method WebDriver
  [q w]
  (let [caps (.getCapabilities q)]
    (print-simple
     (str "#<" "Title: "            (.getTitle q) ", "
          "URL: "                   (first-60 (.getCurrentUrl q)) ", "
          "Browser: "               (.getBrowserName caps) ", "
          "Version: "               (.getVersion caps) ", "
          "JS Enabled: "            (.isJavascriptEnabled caps) ", "
          "Native Events Enabled: " (boolean (re-find #"nativeEvents=true" (.toString caps))) ", "
          "Object: "                q ">") w)))

(defmethod print-method WebElement
  [q w]
  (let [tag-name   (.getTagName q)
        text       (.getText q)
        id         (.getAttribute q "id")
        class-name (.getAttribute q "class")
        name-name  (.getAttribute q "name")
        value      (.getAttribute q "value")
        href       (.getAttribute q "href")
        src        (.getAttribute q "src")
        obj        q]
   (print-simple
    (str "#<"
         (when-attr tag-name
                    (str "Tag: "    "<" tag-name ">" ", "))
         (when-attr text
                    (str "Text: "   (-> text elim-breaks first-60) ", "))
         (when-attr id
                    (str "Id: "     id ", "))
         (when-attr class-name
                    (str "Class: "  class-name ", "))
         (when-attr name-name
                    (str "Name: "  name-name ", "))
         (when-attr value
                    (str "Value: "  value ", "))
         (when-attr href
                    (str "Href: "   href ", "))
         (when-attr src
                    (str "Source: " src ", "))
         "Object: "                  q ">") w)))