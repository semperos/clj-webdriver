(ns ^{:doc "Browser window and 'window handle' support"}
  clj-webdriver.window
  (:require [clj-webdriver.driver :as driver])
  (:import [org.openqa.selenium Dimension Point]
           [clj_webdriver.driver Driver]))

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
  (position [this] "Returns map of X Y coordinates ex. {:x 1 :y 3} relative to the upper left corner of screen.")
  (reposition [this point-map] "Excepts map of X Y coordinates ex. {:x 1 :y 3} repositioning current window relative to screen. Returns driver.")
  (size [this] "Get size of current window. Returns a map of width and height ex. {:width 480 :height 800}")
  (resize [this dimensions-map] "Resize the driver window with a map of width and height ex. {:width 480 :height 800}. Returns driver.")
  (maximize [this] "Maximizes the current window to fit screen if it is not already maximized. Returns driver."))

(defn- window-obj
  [driver]
  (-> (:webdriver driver)
      (.manage)
      (.window)))

(extend-type Driver
  IWindow
  (position [driver]
    (let [wnd (window-obj driver)
          pnt (.getPosition wnd)]
      {:x (.getX pnt) :y (.getY pnt)}))

  (reposition [driver {:keys [x y]}]
    (let [wnd (window-obj driver)]
      (.setPosition wnd (Point. x y))
      driver))

  (size [driver]
    (let [wnd (window-obj driver)
          dim (.getSize wnd)]
      {:width (.getWidth dim) :height (.getHeight dim)}))

  (resize [driver {:keys [width height]}]
    (let [wnd (window-obj driver)]
      (.setSize wnd (Dimension. width height))
      driver))

  (maximize [driver]
    (let [wnd (window-obj driver)]
      (.maximize wnd)
      driver)))

(defmacro ^{:private true
            :doc "Apply the `a-fn` with the `Driver` contained inside the given `window` record and any other `a-fn-args` provided. Before calling the function, switch to the specified window; after calling the function, switch back to the original window."}
  window-switcher
  [window a-fn & a-fn-args]
  `(let [driver# (:driver ~window)
         webdriver# (:webdriver driver#)
         orig-window-handle# (.getWindowHandle webdriver#)
         target-window-handle# (:handle ~window)
         target-current?# (= orig-window-handle# target-window-handle#)]
     (if target-current?#
       (let [return# (~a-fn driver# ~@a-fn-args)]
         (if (driver/driver? return#)
           ~window
           return#))
       (do
         (.switchTo (.window webdriver#) target-window-handle#)
         (let [return# (~a-fn driver# ~@a-fn-args)]
           (.switchTo (.window webdriver#) orig-window-handle#)
           (if (driver/driver? return#)
             ~window
             return#))))))

;; If a Window is passed, it should be the one that is affected,
;; even if it isn't the active one. Since all of these actions
;; only affect the currently active window, we have to switch
;; to the target one, perform the action, and then bring the
;; original active window back into focus.
(extend-type Window
  IWindow
  (position [window]
    (window-switcher window position))

  (reposition [window point-map]
    (window-switcher window reposition point-map))

  (size [window]
    (window-switcher window size))

  (resize [window dimensions-map]
    (window-switcher window resize dimensions-map))

  (maximize [window]
    (window-switcher window maximize)))