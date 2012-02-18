(ns clj-webdriver.driver
  (:require [clojure.core.cache :as cache])
  (:import org.openqa.selenium.interactions.Actions))

(defrecord Driver [webdriver cache-spec actions])

(defn- init-cache
  "Initialize cache based on given strategy"
  ([cache-spec]
     (when (and (map? cache-spec)
                (not (empty? cache-spec)))
       (let [strategy-legend {:basic cache/basic-cache-factory,
                              :fifo cache/fifo-cache-factory,
                              :lru cache/lru-cache-factory,
                              :lirs cache/lirs-cache-factory,
                              :ttl cache/ttl-cache-factory,
                              :lu cache/lu-cache-factory}]
         (atom (apply
                (get strategy-legend (:strategy cache-spec))
                (:args cache-spec)))))))

(defn init-driver
  "Constructor for Driver records.

   webdriver - WebDriver instance
   cache-spec - map with keys :strategy, :args, :include and :exclude"
  ([driver-spec]
     (let [{:keys [webdriver cache-spec]} driver-spec]
       (Driver. webdriver
                (assoc cache-spec :cache (init-cache cache-spec))
                (Actions. webdriver)))))

(defn driver?
  "Function to check class of a Driver, to prevent needing to import it"
  [driver]
  (= (class driver) Driver))

(defn get-cache
  [driver]
  (get-in driver [:cache-spec :cache]))