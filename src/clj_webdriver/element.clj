(ns clj-webdriver.element)

(defrecord Element [webelement])

(defn init-element
  "Initialize an Element record"
  ([] (Element. nil))
  ([webelement] (Element. webelement)))

(defn is-element?
  "Return true if parameter is an Element record"
  [element]
  (= (class element) Element))
