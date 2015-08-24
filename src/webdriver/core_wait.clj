(in-ns 'webdriver.core)

(def wait-timeout 5000)
(def wait-interval 5)

(extend-type WebDriver

  IWait
  (implicit-wait [wd timeout]
    (.implicitlyWait (.. wd manage timeouts) timeout TimeUnit/MILLISECONDS)
    wd)

  (wait-until
    ([wd pred] (wait-until wd pred wait-timeout))
    ([wd pred timeout] (wait-until wd pred timeout wait-interval))
    ([wd pred timeout interval]
     (let [wait (WebDriverWait. wd (/ timeout 1000) interval)]
       (.until wait (proxy [ExpectedCondition] []
                      (apply [d] (pred d))))
       wd))))
