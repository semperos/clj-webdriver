(in-ns 'webdriver.core)

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
