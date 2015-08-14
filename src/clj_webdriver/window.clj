(ns ^{:doc "Browser window and 'window handle' support"}
  clj-webdriver.window
  (:require clj-webdriver.driver)
  (:import [org.openqa.selenium Dimension Point]
           [org.openqa.selenium WebDriver WebDriver$Window]
           clj_webdriver.driver.Driver))

(defrecord ^{:doc "A record that encapsulates all operations on windows, including what Selenium-WebDriver handles with the `WebDriver.Window` interface and the `getWindowHandle` methods."}
    Window [driver handle title url])

(defn init-window
  "Given a `Driver` instance, the 'window handle' UUID of a given window, as well as its title and URL, instantiate a `Window` record."
  [driver handle title url]
  (Window. driver handle title url))

(defn window?
  "Return true if `(class this)` is our `Window` class."
  [this]
  (= (class this) Window))

(defprotocol IWindow
  "Functions to manage browser size and position."
  (maximize [this] "Maximizes the current window to fit screen if it is not already maximized. Returns driver.")
  (position [this] "Returns map of X Y coordinates ex. {:x 1 :y 3} relative to the upper left corner of screen.")
  (reposition [this coordinates-map] "Excepts map of X Y coordinates ex. {:x 1 :y 3} repositioning current window relative to screen. Returns driver.")
  (resize [this dimensions-map] "Resize the driver window with a map of width and height ex. {:width 480 :height 800}. Returns driver.")
  (size [this] "Get size of current window. Returns a map of width and height ex. {:width 480 :height 800}")
  (window-obj [this] "Return the underlying Java Window object used for manipulating the browser window."))

(extend-type Driver
  IWindow
  (maximize [driver]
    (let [wnd ^WebDriver$Window (window-obj driver)]
      (.maximize wnd)
      driver))

  (position [driver]
    (let [^WebDriver$Window wnd (window-obj driver)
          pnt (.getPosition wnd)]
      {:x (.getX pnt) :y (.getY pnt)}))

  (reposition [driver {:keys [x y]}]
    (let [^WebDriver$Window wnd (window-obj driver)
          pnt (.getPosition wnd)
          x (or x (.getX pnt))
          y (or y (.getY pnt))]
      (.setPosition wnd (Point. x y))
      driver))

  (resize [driver {:keys [width height]}]
    (let [^WebDriver$Window wnd (window-obj driver)
          dim (.getSize wnd)
          width (or width (.getWidth dim))
          height (or height (.getHeight dim))]
      (.setSize wnd (Dimension. width height))
      driver))

  (size [driver]
    (let [^WebDriver$Window wnd (window-obj driver)
          dim (.getSize wnd)]
      {:width (.getWidth dim) :height (.getHeight dim)}))

  (window-obj [^Driver driver]
    (let [^WebDriver webdriver (.webdriver driver)
          manage (.manage webdriver)]
      (.window manage))))

(defmacro ^{:private true
            :doc "Apply the `f` function with the `Driver` contained inside the given `window` record and any other `a-fn-args` provided. Before calling the function, switch to the specified window; after calling the function, switch back to the original window."}
  window-switcher
  [window f & f-args]
  `(let [^Driver driver# (:driver ~window)
         ^WebDriver webdriver# (.webdriver driver#)
         ^String orig-window-handle# (.getWindowHandle webdriver#)
         ^String target-window-handle# (:handle ~window)
         target-current?# (= orig-window-handle# target-window-handle#)]
     ;; If we're already trying to manipulate the currently active window,
     ;; immediately call the implementation of the IWindow function with the driver
     (if target-current?#
       (let [return# (~f driver# ~@f-args)]
         ;; Functions in IWindow that have no logical return value need to return
         ;; the thing being acted on. Since we proxy to the `Driver` implementation
         ;; of each function in the `Window` implementations, we have to check
         ;; for a `Driver` return value and return `Window` instead.
         (if (instance? Driver return#)
           ~window
           return#))
       (do
         ;; We're trying to work with a window other than the active one; make
         ;; our target window the active one first.
         (.switchTo (.window webdriver#) target-window-handle#)
         ;; Then perform the desired action.
         (let [return# (~f driver# ~@f-args)]
           ;; Switch back to the original active window.
           (.switchTo (.window webdriver#) orig-window-handle#)
           ;; Make sure `Window` is returned for appropriate functions
           ;; (see above comment)
           (if (instance? Driver return#)
             ~window
             return#))))))

;; If a Window is passed, it should be the one that is affected,
;; even if it isn't the active one. Since all of these actions
;; only affect the currently active window, we have to switch
;; to the target one, perform the action, and then bring the
;; original active window back into focus.
(extend-type Window
  IWindow
  (maximize [window]
    (window-switcher window maximize))

  (position [window]
    (window-switcher window position))

  (reposition [window coordinates-map]
    (window-switcher window reposition coordinates-map))

  (resize [window dimensions-map]
    (window-switcher window resize dimensions-map))

  (size [window]
    (window-switcher window size)))
