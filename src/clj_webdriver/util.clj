(ns clj-webdriver.util
  (:require [clojure.string :as str]
            [clj-webdriver.cache :as cache])
  (:import clj_webdriver.driver.Driver
           [org.openqa.selenium WebDriver WebElement]
           java.io.Writer))

(declare build-query)

(defn build-css-attrs
  "Given a map of attribute-value pairs, build the latter portion of a CSS query that follows the tag."
  [attr-val]
  (apply str (for [[attr value] attr-val]
               (cond
                 (= :text attr) (throw (IllegalArgumentException. "CSS queries do not support checking against the text of an element."))
                 (= :index attr) (str ":nth-child(" (inc value) ")") ;; CSS is 1-based
                 :else (str "[" (name attr) "='" value "']")))))

(defn build-xpath-attrs
  "Given a map of attribute-value pairs, build the bracketed portion of an XPath query that follows the tag"
  [attr-val]
  (apply str (for [[attr value] attr-val]
               (cond
                (= :text attr) (str "[text()=\"" value "\"]")
                (= :index attr) (str "[" (inc value) "]") ; in clj-webdriver,
                :else (str "[@"                           ; all indices 0-based
                           (name attr)
                           "="
                           "'" (name value) "']")))))

(defn build-css-with-hierarchy
  "Given a vector of queries in hierarchical order, create a CSS query.
   For example: `[{:tag :div, :id \"content\"}, {:tag :a, :class \"external\"}]` would
   produce the CSS query \"div[id='content'] a[class='external']\""
  [v-of-attr-vals]
  (str/join
   " "
   (for [attr-val v-of-attr-vals]
     (cond
      (or (contains? attr-val :css)
          (contains? attr-val :xpath)) (throw (IllegalArgumentException. "Hierarhical queries do not support the use of :css or :xpath entries."))
          (some #{(:tag attr-val)} [:radio
                                    :checkbox
                                    :textfield
                                    :password
                                    :filefield]) (throw (IllegalArgumentException. "Hierarchical queries do not support the use of \"meta\" tags such as :button*, :radio, :checkbox, :textfield, :password or :filefield. "))
                                    
                                    :else (:css (build-query attr-val :css))))))

(defn build-xpath-with-hierarchy
  "Given a vector of queries in hierarchical order, create XPath.
   For example: `[{:tag :div, :id \"content\"}, {:tag :a, :class \"external\"}]` would
   produce the XPath \"//div[@id='content']//a[@class='external']"
  [v-of-attr-vals]
  (apply str
         (for [attr-val v-of-attr-vals]
           (cond
             (or (contains? attr-val :css)
                 (contains? attr-val :xpath)) (throw (IllegalArgumentException. "Hierarhical queries do not support the use of :css or :xpath entries."))
                 (some #{(:tag attr-val)} [:radio
                                           :checkbox
                                           :textfield
                                           :password
                                           :filefield]) (throw (IllegalArgumentException. "Hierarchical queries do not support the use of \"meta\" tags such as :button*, :radio, :checkbox, :textfield, :password or :filefield. "))
                                           :else (:xpath (build-query attr-val))))))


(declare remove-regex-entries)
(defn build-query
  "Given a map of attribute-value pairs, generate XPath or CSS based on `output`. Optionally include a `prefix` to specify whether this should be a `:global` \"top-level\" query or a `:local`, child query."
  ([attr-val] (build-query attr-val :xpath :global))
  ([attr-val output] (build-query attr-val output :global))
  ([attr-val output prefix]
     (if-not (map? attr-val) ;; dispatch here for hierarhical queries
       (if (= output :xpath)
         (build-xpath-with-hierarchy attr-val)
         (build-css-with-hierarchy attr-val))
       (let [attr-val (remove-regex-entries attr-val)]
         (cond
           (contains? attr-val :xpath)      {:xpath (:xpath attr-val)}
           (contains? attr-val :css)        {:css (:css attr-val)}
           (= (:tag attr-val) :radio)       (build-query (assoc attr-val :tag :input :type "radio"))
           (= (:tag attr-val) :checkbox)    (build-query (assoc attr-val :tag :input :type "checkbox"))
           (= (:tag attr-val) :textfield)   (build-query (assoc attr-val :tag :input :type "text"))
           (= (:tag attr-val) :password)    (build-query (assoc attr-val :tag :input :type "password"))
           (= (:tag attr-val) :filefield)   (build-query (assoc attr-val :tag :input :type "filefield"))
           :else (let [tag (if (nil? (:tag attr-val))
                             :*
                             (:tag attr-val))
                       attr-val (dissoc attr-val :tag)
                       prefix-legend {:local "."
                                      :global ""}]
                   (if (= output :xpath)
                     (let [query-str (str (prefix-legend prefix) "//"
                                          (name tag)
                                          (when-not (empty? attr-val)
                                            (build-xpath-attrs attr-val)))]
                       {:xpath query-str})
                     ;; else, CSS
                     (let [query-str (str (name tag)
                                          (when-not (empty? attr-val)
                                            (build-css-attrs attr-val)))]
                       {:css query-str}))))))))



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

(defn filter-regex-entries
  "Given a map `m`, return a map containing only entries whose values are regular expressions."
  [m]
  (into {} (filter
            #(let [[k v] %] (= java.util.regex.Pattern (class v)))
            m)))

(defn remove-regex-entries
  "Given a map `m`, return a map containing only entries whose values are NOT regular expressions."
  [m]
  (into {} (remove
            #(let [[k v] %] (= java.util.regex.Pattern (class v)))
            m)))

(defn first-60
  "Get first twenty characters of `s`, then add ellipsis"
  [s]
  (str (re-find #"(?s).{1,60}" s)
       (when (> (count s) 60)
         "...")))

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
          "Native Events Enabled: " (boolean (re-find #"nativeEvents=true" (str caps))) ", "
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
                    (str "Value: "  (-> value elim-breaks first-60) ", "))
         (when-attr href
                    (str "Href: "   href ", "))
         (when-attr src
                    (str "Source: " src ", "))
         "Object: "                  q ">") w)))