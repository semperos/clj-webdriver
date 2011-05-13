(in-ns 'clj-webdriver.core)

;; We've defined our own record type WindowHandler because
;; the String id which WebDriver returns by default to identify
;; a window is not particularly helpful
;;
;; The equivalent starred functions below wrap the WebDriver methods
;; directly, without using a cusotm record.
(declare switch-to-window)
(defn window-handle
  "Get the only (or first) window handle, return as a WindowHandler record"
  [driver]
  (WindowHandle. driver
                 (.getWindowHandle driver)
                 (title driver)
                 (current-url driver)))

(defn window-handle*
  "For WebDriver API compatibility: this simply wraps `.getWindowHandle`"
  [driver]
  (.getWindowHandle driver))

(defn window-handles
  "Retrieve a vector of `WindowHandle` records which can be used to switchTo particular open windows"
  [driver]
  (let [current-handle (.getWindowHandle driver)
        all-handles (lazy-seq (.getWindowHandles driver))
        handle-records (for [handle all-handles]
                         (let [b (switch-to-window driver handle)]
                           (WindowHandle. driver
                                          handle
                                          (title b)
                                          (current-url b))))]
    (switch-to-window driver current-handle)
    handle-records))

(defn window-handles*
  "For WebDriver API compatibility: this simply wraps `.getWindowHandles`"
  [driver]
  (lazy-seq (.getWindowHandles driver)))

(defn other-window-handles
  "Retrieve window handles for all windows except the current one"
  [driver]
  (remove #(= (:handle %) (:handle (window-handle driver)))
          (doall (window-handles driver))))

(defn other-window-handles*
  "For consistency with other window handling functions, this starred version just returns the string-based ID's that WebDriver produces"
  [driver]
  (remove #(= % (window-handle* driver))
          (doall (window-handles* driver))))

(defn switch-to-frame
  "Switch focus to a particular HTML frame"
  [driver frame]
  (.frame (.switchTo driver) frame)
  driver)

(defn switch-to-window
  "Switch focus to a particular open window"
  ([handle] (switch-to-window (:driver handle) handle))
  ([driver handle]
     (cond
      (string? handle) (.window (.switchTo driver) handle)
      (= (class handle) WindowHandle) (.window (.switchTo (:driver handle)) (:handle handle))
      (number? handle) (switch-to-window driver (nth (window-handles driver) handle))
      (nil? handle) (throw (RuntimeException. "No window can be found"))
      :else (.window (.switchTo driver) handle))))

(defn switch-to-other-window
  "Given that two and only two browser windows are open, switch to the one not currently active"
  [driver]
  (if (not= (count (window-handles driver)) 2)
    (throw (RuntimeException.
            (str "You may only use this function when two and only two "
                 "browser windows are open.")))
    (switch-to-window driver (first (other-window-handles driver)))))

(defn switch-to-default
  "Switch focus to the first first frame of the page, or the main document if the page contains iframes"
  [driver]
  (.defaultContent (.switchTo driver)))

(defn switch-to-active
  "Switch to element that currently has focus, or to the body if this cannot be detected"
  [driver]
  (.activeElement (.switchTo driver)))