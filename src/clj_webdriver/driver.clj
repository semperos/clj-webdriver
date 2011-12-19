(ns clj-webdriver.driver
  (require [clojure.core.cache :as cache]))

(defrecord Driver [webdriver cache-spec element-cache middlewares])

(defn- init-cache
  "Initialize cache based on given strategy"
  ([cache-spec]
     (when-not (nil? cache-spec)
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
  ([] (Driver. nil nil nil nil))
  ([webdriver] (Driver. webdriver nil nil nil))
  ([webdriver cache-spec]
     (Driver. webdriver cache-spec (init-cache cache-spec) nil))
  ([webdriver cache-spec element-cache]
     (Driver. webdriver cache-spec element-cache nil))
  ([webdriver cache-spec element-cache middlewares]
     (Driver. webdriver cache-spec element-cache middlewares)))

(defn is-driver?
  "Function to check class of a Driver, to prevent needing to import it"
  [driver]
  (= (class driver) Driver))