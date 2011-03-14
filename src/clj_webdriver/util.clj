(ns clj-webdriver.util
  (:require [clojure.string :as str])
  (:import [org.openqa.selenium WebDriver WebElement]))

(defn build-xpath
  "Given a tag and a map of attribute-value pairs, generate XPath"
  [tag attr-val]
  (str "//"
       (name tag)
       (apply str (for [[attr value] attr-val]
                    (if (= :text attr) ; inspired by Watir-WebDriver
                      (str "[text()='" value "']")
                      (str "[@"
                          (name attr)
                          "="
                          "'" value "']"))))))

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

(defmacro when-attr
  "Special `when` macro for checking if an attribute isn't available or is an empty string"
  [obj & body]
  `(when (not (or (nil? ~obj) (empty? ~obj)))
     ~@body))

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
     (when-attr value
                 (str "Value: "  value ", "))
     (when-attr href
                 (str "Href: "   href ", "))
     (when-attr src
                 (str "Source: " src ", "))
     "Object: "                  q ">") w)))

(comment

(defn <find-it>
  "Given a WebDriver `driver`, optional HTML tag `tag`, and an HTML attribute-value pair `attr-val`, return the first WebElement that matches. The values of `attr-val` items must be contained within the target value, e.g. `'log'` would match `'not_logged_in'`."
  ([driver attr-val]
     (<find-it> driver :* attr-val))
  ([driver tag attr-val]
     (if (> (count attr-val) 1)
       (throw (IllegalArgumentException.
               (str "Your attr-val map may only include one attribute-value pair. "
                    "Due to inconsistent XPath behavior, locating an element "
                    "by multiple calls to contains() is not supported.")))
       (let [entry (first attr-val)
             attr (key entry)
             value (val entry)]
         (find-element driver (by-attr-contains tag attr value))))))

(defn <find-it
  "Given a WebDriver `driver`, optional HTML tag `tag`, and an HTML attribute-value pair `attr-val`, return the first WebElement that matches. The values of `attr-val` items must represent the start of the target value, e.g. `'log'` would match `'login'` but not `'not_logged_in'`"
  ([driver attr-val]
     (<find-it driver :* attr-val))
  ([driver tag attr-val]
     (if (> (count attr-val) 1)
       (throw (IllegalArgumentException.
               (str "Your attr-val map may only include one attribute-value pair. "
                    "Due to inconsistent XPath behavior, locating an element "
                    "by multiple calls to starts-with() is not supported.")))
       (let [entry (first attr-val)
             attr (key entry)
             value (val entry)]
         (find-element driver (by-attr-starts tag attr value))))))  

  )