(ns clj-webdriver.element
  (:require [clj-webdriver.util :as util]))

(defn- element-attributes
  [el & attributes]
  (map #(util/element-attribute el %1) attributes))

(defrecord Element [webelement]
  clojure.lang.IFn
  (invoke [self prop-name] (util/element-attribute self prop-name))
  (invoke [self prop-1 prop-2] (element-attributes self prop-1 prop-2))
  (invoke [self prop-1 prop-2 prop-3] (element-attributes self prop-1 prop-2 prop-3))
  (invoke [self prop-1 prop-2 prop-3 prop-4] (element-attributes self prop-1 prop-2 prop-3 prop-4))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14 prop-15] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14 prop-15))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14 prop-15 prop-16] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14 prop-15 prop-16))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14 prop-15 prop-16 prop-17] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14 prop-15 prop-16 prop-17))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14 prop-15 prop-16 prop-17 prop-18] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14 prop-15 prop-16 prop-17 prop-18))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14 prop-15 prop-16 prop-17 prop-18 prop-19] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14 prop-15 prop-16 prop-17 prop-18 prop-19))
  (invoke [self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14 prop-15 prop-16 prop-17 prop-18 prop-19 prop-20] (element-attributes self prop-1 prop-2 prop-3 prop-4 prop-5 prop-6 prop-7 prop-8 prop-9 prop-10 prop-11 prop-12 prop-13 prop-14 prop-15 prop-16 prop-17 prop-18 prop-19 prop-20))
  (applyTo [self prop-names]
    (for [prop-name prop-names]
      (util/element-attribute self prop-name))))

(defn init-element
  "Initialize an Element record"
  ([] (Element. nil))
  ([webelement] (Element. webelement)))

(defn init-elements
  "Given WebElement objects, batch initialize elements (for things like returning all options for select lists)"
  [webelements]
  (for [webel webelements]
    (init-element webel)))

(defn element?
  "Return true if parameter is an Element record"
  [element]
  (= (class element) Element))

(defn element-like?
  "Return true if parameter is either an Element record or a map with a :webelement entry"
  [element]
  (or (element? element)
      (and (map? element)
           (contains? element :webelement))))