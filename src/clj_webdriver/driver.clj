(ns clj-webdriver.driver
  (require [fogus.clache :as clache]))

(defrecord Driver [webdriver cache-spec element-cache middlewares])

(defn- init-cache
  "Initialize cache based on given strategy"
  ([cache-spec]
     {:pre [(when (nil? (:args cache-spec))
              (= (:strategy cache-spec) :basic))]}
     (let [strategy-legend {:basic clache/->BasicCache,
                            :fifo clache/->FIFOCache,
                            :lru clache/->LRUCache,
                            :lirs clache/->LIRSCache,
                            :ttl clache/->TTLCache,
                            :lu clache/->LUCache}]
       (atom (apply
              (get strategy-legend (:strategy cache-spec))
              (into [{}] (:args cache-spec)))))))

(defn init-driver
  "Constructor for Driver records.

   webdriver - WebDriver instance
   cache-spec - map with keys :strategy, :args, :include and :exclude"
  ([] (Driver. nil nil nil nil))
  ([webdriver] (Driver. webdriver nil nil nil))
  ([webdriver cache-spec]
     (Driver. webdriver cache-spec nil nil))
  ([webdriver cache-spec element-cache]
     (Driver. webdriver cache-spec element-cache nil))
  ([webdriver cache-spec element-cache middlewares]
     (Driver. webdriver cache-spec element-cache middlewares)))

(defn is-driver?
  "Function to check class of a Driver, to prevent needing to import it"
  [driver]
  (= (class driver) Driver))