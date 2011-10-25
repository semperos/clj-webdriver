(in-ns 'clj-webdriver.core)

(defrecord Driver [webdriver cache-strategy middlewares]

  ;;; Basic Functions ;;;
  IDriverBasics
  (get-url [driver url]
    (.get (:webdriver driver) url)
    driver)

  (to [driver url]
    (.to (.navigate (:webdriver driver)) url)
    driver)

  (current-url [driver]
    (.getCurrentUrl (:webdriver driver)))

  (title [driver]
    (.getTitle (:webdriver driver)))

  (page-source [driver]
    (.getPageSource (:webdriver driver)))

  (close [driver]
    (let [handles (window-handles* (:webdriver driver))]
      (if (> (count handles) 1) ; get back to a window that is open before proceeding
        (let [this-handle (window-handle* (:webdriver driver))
              idx (.indexOf handles this-handle)]
          (cond
           (zero? idx) (do ; if first window, switch to next
                         (.close (:webdriver driver))
                         (switch-to-window (:webdriver driver) (nth handles (inc idx))))
           :else (do ; otherwise, switch back one window
                   (.close (:webdriver driver))
                   (switch-to-window (:webdriver driver) (nth handles (dec idx))))))
        (.close (:webdriver driver)))))
  
  (quit [driver]
    (.quit (:webdriver driver)))
  
  (back [driver]
    (.back (.navigate (:webdriver driver)))
    driver)

  (forward [driver]
    (.forward (.navigate (:webdriver driver)))
    driver)

  (refresh [driver]
    (.refresh (.navigate (:webdriver driver)))
    driver)


  ;;; Windows and Frames ;;;
  ITargetLocator

  (window-handle [driver]
    (init-window-handle (:webdriver driver)
                        (.getWindowHandle (:webdriver driver))
                        (title (:webdriver driver))
                        (current-url (:webdriver driver))))

  (window-handles [driver]
    (let [current-handle (.getWindowHandle (:webdriver driver))
          all-handles (lazy-seq (.getWindowHandles (:webdriver driver)))
          handle-records (for [handle all-handles]
                           (let [b (switch-to-window (:webdriver driver) handle)]
                             (init-window-handle (:webdriver driver)
                                                 handle
                                                 (title b)
                                                 (current-url b))))]
      (switch-to-window (:webdriver driver) current-handle)
      handle-records))

  (other-window-handles [driver]
    (remove #(= (:handle %) (:handle (window-handle (:webdriver driver))))
            (doall (window-handles (:webdriver driver)))))

  (switch-to-frame [driver frame]
    (.frame (.switchTo (:webdriver driver)) frame)
    (:webdriver driver))

  (switch-to-window [driver handle]
    ([driver handle]
       (cond
        (string? handle) (.window (.switchTo (:webdriver driver)) handle)
        (is-window-handle? handle) (.window (.switchTo (:driver handle)) (:handle handle))
        (number? handle) (switch-to-window (:webdriver driver) (nth (window-handles (:webdriver driver)) handle))
        (nil? handle) (throw (RuntimeException. "No window can be found"))
        :else (.window (.switchTo (:webdriver driver)) handle))))

  (switch-to-other-window [driver]
    (if (not= (count (window-handles (:webdriver driver))) 2)
      (throw (RuntimeException.
              (str "You may only use this function when two and only two "
                   "browser windows are open.")))
      (switch-to-window (:webdriver driver) (first (other-window-handles (:webdriver driver))))))

  (switch-to-default [driver]
    (.defaultContent (.switchTo (:webdriver driver))))

  (switch-to-active [driver]
    (.activeElement (.switchTo (:webdriver driver))))


  ;;; Wait Functionality ;;;
  IWait

  (implicit-wait [driver timeout]
    (.implicitlyWait (.. (:webdriver driver) manage timeouts) timeout TimeUnit/MILLISECONDS))

  (wait-until [driver pred timeout interval]
    (let [wait (WebDriverWait. (:webdriver driver) (/ timeout 1000) interval)]
    (.until wait (proxy [ExpectedCondition] []
                   (apply [d] (pred d))))))
  )


(defn init-driver
  "Constructor for Driver records"
  ([] (Driver. nil nil nil))
  ([webdriver] (Driver. webdriver nil nil))
  ([webdriver cs] (Driver. webdriver cs nil))
  ([webdriver cs mws] (Driver. webdriver cs mws)))

(defn is-driver?
  "Function to check class of a Driver, to prevent needing to import it"
  [driver]
  (= (class driver) Driver))