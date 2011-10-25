(in-ns 'clj-webdriver.core)

;; We've defined our own record type WindowHandler because
;; the String id which WebDriver returns by default to identify
;; a window is not particularly helpful
;;
;; The equivalent starred functions below wrap the WebDriver methods
;; directly, without using a cusotm record.
;; (declare switch-to-window)
;; (defn window-handle
;;   "Get the only (or first) window handle, return as a WindowHandler record"
;;   [driver]
;;   (init-window-handle driver
;;                       (.getWindowHandle driver)
;;                       (title driver)
;;                       (current-url driver)))

(defn window-handle*
  "For WebDriver API compatibility: this simply wraps `.getWindowHandle`"
  [driver]
  (.getWindowHandle driver))

(defn window-handles*
  "For WebDriver API compatibility: this simply wraps `.getWindowHandles`"
  [driver]
  (lazy-seq (.getWindowHandles driver)))

(defn other-window-handles*
  "For consistency with other window handling functions, this starred version just returns the string-based ID's that WebDriver produces"
  [driver]
  (remove #(= % (window-handle* driver))
          (doall (window-handles* driver))))
