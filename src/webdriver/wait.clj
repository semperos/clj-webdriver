(ns webdriver.wait
  (:import java.util.concurrent.TimeUnit
           org.openqa.selenium.WebDriver
           [org.openqa.selenium.support.ui ExpectedCondition WebDriverWait]))

;; ## Wait Functionality ##
(defprotocol IWait
  "Implicit and explicit waiting"
  (implicit-wait [wd timeout] "Specify the amount of time the WebDriver should wait when searching for an element if it is not immediately present. This setting holds for the lifetime of the driver across all requests. Units in milliseconds.")
  (wait-until
    [wd pred]
    [wd pred timeout]
    [wd pred timeout interval] "Set an explicit wait time `timeout` for a particular condition `pred`. Optionally set an `interval` for testing the given predicate. All units in milliseconds"))

(extend-type WebDriver

  IWait
  (implicit-wait [wd timeout]
    (.implicitlyWait (.. wd manage timeouts) timeout TimeUnit/MILLISECONDS)
    wd)

  (wait-until
    ([wd pred] (wait-until wd pred 5000 0))
    ([wd pred timeout] (wait-until wd pred timeout 0))
    ([wd pred timeout interval]
     (let [wait (WebDriverWait. wd (/ timeout 1000) interval)]
       (.until wait (proxy [ExpectedCondition] []
                      (apply [d] (pred d))))
       wd))))
