(ns clj-webdriver.driver
  (require [clojure.core.cache :as cache]))

(defrecord Driver [webdriver cache-spec])

(defn- init-cache
  "Initialize cache based on given strategy"
  ([cache-spec]
     (when (and (map? cache-spec)
                (not (empty? cache-spec)))
       (let [strategy-legend {:basic cache/->BasicCache,
                              :fifo cache/->FIFOCache,
                              :lru cache/->LRUCache,
                              :lirs cache/->LIRSCache,
                              :ttl cache/->TTLCache,
                              :lu cache/->LUCache}]
         (atom (apply
                (get strategy-legend (:strategy cache-spec))
                (into [{}] (:args cache-spec))))))))

(defn init-driver
  "Constructor for Driver records.

   webdriver - WebDriver instance
   cache-spec - map with keys :strategy, :args, :include and :exclude"
  ([driver-spec]
     (let [{:keys [webdriver cache-spec]} driver-spec]
       (Driver. webdriver
                (assoc cache-spec :cache (init-cache cache-spec))))))

(defn is-driver?
  "Function to check class of a Driver, to prevent needing to import it"
  [driver]
  (= (class driver) Driver))

(defn get-cache
  [driver]
  (get-in driver [:cache-spec :cache]))