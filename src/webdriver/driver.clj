(ns webdriver.driver
  (:require [clojure.core.protocols :as p])
  (:import java.util.Iterator
           org.openqa.selenium.interactions.Actions))

(deftype Driver [webdriver last]
  Iterable
  (iterator [driver]
    (let [consumed? (atom false)]
      (reify Iterator
        (hasNext [_] (not @consumed?))
        (next [driver]
          (if @consumed?
            nil
            (do
              (swap! consumed? not)
              driver))))))

  p/CollReduce
  (coll-reduce [driver f] (f driver))
  (coll-reduce [driver f val] (f driver val)))
