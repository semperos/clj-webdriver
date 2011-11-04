(ns clj-webdriver.element
  (:require [clj-webdriver.cache :as cache]))

(defrecord Element [webelement])

(defn init-element
  "Initialize an Element record"
  ([] (Element. nil))
  ([webelement] (Element. webelement)))

