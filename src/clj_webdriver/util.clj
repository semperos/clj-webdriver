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