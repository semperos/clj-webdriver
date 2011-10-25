(ns clj-webdriver.driver)

(defrecord Driver [webdriver cache-strategy middlewares])

(defn init-driver
  ([] (Driver. nil nil nil))
  ([webdriver] (Driver. webdriver nil nil))
  ([webdriver cs] (Driver. webdriver cs nil))
  ([webdriver cs mws] (Driver. webdriver cs mws)))

(defn is-driver?
  [driver]
  (= (class driver) Driver))