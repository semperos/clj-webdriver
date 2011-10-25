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
    driver))


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