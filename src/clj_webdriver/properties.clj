(ns clj-webdriver.properties
  (:use [clj-webdriver.util :only [read-config]]
        [clojure.walk :only [keywordize-keys]])
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:import java.util.Properties))

(def ^{:dynamic true}
  *properties*
  (let [clj-props "resources/properties.clj"
        java-props "resources/clj_webdriver.properties"]
    (keywordize-keys
     (cond
       (.exists (io/as-file clj-props)) (do
                                          (log/debug "Reading clj props file")
                                          (read-config "resources/properties.clj"))
       (.exists (io/as-file java-props)) (do
                                           (log/debug "Reading Java properties file")
                                           (into {} (doto (Properties.)
                                                      (.load (io/reader "resources/clj_webdriver.properties")))))
       :else {}))))