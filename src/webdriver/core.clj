;; # Clojure API for Selenium-WebDriver #
;;
;; WebDriver is a library that allows for easy manipulation of the Firefox,
;; Chrome, Safari and  Internet Explorer graphical browsers, as well as the
;; Java-based HtmlUnit headless browser.
;;
;; This library provides both a thin wrapper around WebDriver and a more
;; Clojure-friendly API for finding elements on the page and performing
;; actions on them. See the README for more details.
;;
;; Credits to mikitebeka's `webdriver-clj` project on Github for a starting-
;; point for this project and many of the low-level wrappers around the
;; WebDriver API.
;;
(ns webdriver.core
  (:require [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [webdriver.js.browserbot :as browserbot-js]
            [webdriver.firefox :as ff]
            [webdriver.util :refer :all])
  (:import
           [java.lang.reflect Constructor Field]
           java.util.concurrent.TimeUnit
           [org.openqa.selenium By Capabilities Dimension Keys NoSuchElementException OutputType Point TakesScreenshot WebDriver WebElement WebDriver$Window]
           org.openqa.selenium.chrome.ChromeDriver
           [org.openqa.selenium.firefox FirefoxDriver FirefoxProfile]
           org.openqa.selenium.htmlunit.HtmlUnitDriver
           org.openqa.selenium.ie.InternetExplorerDriver
           [org.openqa.selenium.interactions Actions CompositeAction]
           org.openqa.selenium.internal.WrapsDriver
           [org.openqa.selenium.remote DesiredCapabilities RemoteWebDriver]
           [org.openqa.selenium.support.ui ExpectedCondition Select WebDriverWait]))

;; ## Protocols for webdriver API ##

;; ### WebDriver Functions ###
(defprotocol IDriver
  "Basics of driver handling"
  (back [driver] "Go back to the previous page in \"browsing history\"")
  (close [driver] "Close this browser instance, switching to an active one if more than one is open")
  (current-url [driver] "Retrieve the URL of the current page")
  (forward [driver] "Go forward to the next page in \"browsing history\".")
  (get-screenshot [driver] [driver format] [driver format destination] "Take a screenshot using Selenium-WebDriver's getScreenshotAs method")
  (get-url [driver url] "Navigate the driver to a given URL")
  (page-source [driver] "Retrieve the source code of the current page")
  (quit [driver] "Destroy this browser instance")
  (refresh [driver] "Refresh the current page")
  (title [driver] "Retrieve the title of the current page as defined in the `head` tag")
  (to [driver url] "Navigate to a particular URL. Arg `url` can be either String or java.net.URL. Equivalent to the `get` function, provided here for compatibility with WebDriver API."))

;; ### Windows and Frames ###
(defprotocol ITargetLocator
  "Functions that deal with browser windows and frames"
  (window [driver] "Get the only (or first) window")
  (window-handles [driver] "Retrieve a vector of `Window` records which can be used to switch to particular open windows")
  (other-window-handles [driver] "Retrieve window handles for all windows except the current one")
  (switch-to-frame [driver frame] "Switch focus to a particular HTML frame by supplying a `WebElement` or an integer for the nth frame on the page (zero-based index)")
  (switch-to-window [driver handle] "Switch focus to a particular open window")
  (switch-to-other-window [driver] "Given that two and only two browser windows are open, switch to the one not currently active")
  (switch-to-default [driver] "Switch focus to the first first frame of the page, or the main document if the page contains iframes")
  (switch-to-active [driver] "Switch to element that currently has focus, or to the body if this cannot be detected"))

(defprotocol IWait
  "Implicit and explicit waiting"
  (implicit-wait [wd timeout] "Specify the amount of time the WebDriver should wait when searching for an element if it is not immediately present. This setting holds for the lifetime of the driver across all requests. Units in milliseconds.")
  (wait-until
    [wd pred]
    [wd pred timeout]
    [wd pred timeout interval] "Set an explicit wait time `timeout` for a particular condition `pred`. Optionally set an `interval` for testing the given predicate. All units in milliseconds"))

(defprotocol IWindow
  "Functions to manage browser size and position."
  (maximize [this] "Maximizes the current window to fit screen if it is not already maximized. Returns driver or window.")
  (position [this] "Returns map of X Y coordinates ex. {:x 1 :y 3} relative to the upper left corner of screen.")
  (reposition [this coordinates-map] "Excepts map of X Y coordinates ex. {:x 1 :y 3} repositioning current window relative to screen. Returns driver or window.")
  (resize [this dimensions-map] "Resize the driver window with a map of width and height ex. {:width 480 :height 800}. Returns driver or window.")
  (window-size [this] "Get size of current window. Returns a map of width and height ex. {:width 480 :height 800}"))

(defprotocol IOptions
  "Options interface, including cookie and timeout handling"
  (add-cookie [driver cookie] "Add a new cookie to the browser session")
  (delete-cookie-named [driver cookie-name] "Delete a cookie given its name")
  (delete-cookie [driver cookie] "Delete a cookie given a cookie instance")
  (delete-all-cookies [driver] "Delete all cookies defined in the current session")
  (cookies [driver] "Retrieve a set of cookies defined in the current session")
  (cookie-named [driver cookie-name] "Retrieve a cookie object given its name"))

;; ### Alert Popups ###
(defprotocol IAlert
  "Simple interactions with alert popups"
  (accept [driver] "Accept the dialog. Equivalent to pressing 'Ok'")
  (alert-obj [driver] "Return the underlying Java object that can be used with the Alert Java API (exposed until all functionality is ported)")
  (alert-text [driver] "Get the text of the popup dialog's message")
  ;; (authenticate-using [driver username password] "Enter `username` and `password` into fields from a Basic Access Authentication popup dialog")
  (dismiss [driver] "Dismiss the dialog. Equivalent to pressing 'Cancel'"))

;; ### Finding Elements on Page ###
(defprotocol IFind
  "Functions used to locate elements on a given page"
  (find-element-by [this by] "Retrieve the element object of an element described by `by`, optionally limited to elements beneath a parent element (depends on dispatch). Prefer `find-element` to this function unless you know what you're doing.")
  (find-elements-by [this by] "Retrieve a seq of element objects described by `by`, optionally limited to elements beneath a parent element (depends on dispatch). Prefer `find-elements` to this function unless you know what you're doing.")
  (find-table-cell [driver table coordinates] "Given a `driver`, a `table` element, and a zero-based set of coordinates for row and column, return the table cell at those coordinates for the given table.")
  (find-table-row [driver table row-index] "Return all cells in the row of the given table element, `row-index` as a zero-based index of the target row.")
  (find-by-hierarchy [driver hierarchy-vector] "Given a Webdriver `driver` and a vector `hierarchy-vector`, return a sequence of the described elements in the hierarchy dictated by the order of elements in the `hierarchy-vector`.")
  (find-elements [this locator] "Find all elements that match the parameters supplied in the `attr-val` map. Also provides a shortcut to `find-by-hierarchy` if a vector is supplied instead of a map.")
  (find-element [this locator] "Call (first (find-elements args))"))

;; ### Acting on Elements ###
(defprotocol IElement
  "Basic actions on elements"
  (attribute [element attr] "Retrieve the value of the attribute of the given element object")
  (click [element] "Click a particular HTML element")
  (css-value [element property] "Return the value of the given CSS property")
  (displayed? [element] "Returns true if the given element object is visible/displayed")
  (exists? [element] "Returns true if the given element exists")
  (flash [element] "Flash the element in question, to verify you're looking at the correct element")
  (focus [element] "Apply focus to the given element")
  (html [element] "Retrieve the outer HTML of an element")
  (intersects? [element-a element-b] "Return true if `element-a` intersects with `element-b`. This mirrors the Selenium-WebDriver API method, but see the `intersect?` function to compare an element against multiple other elements for intersection.")
  (location [element] "Given an element object, return its location as a map of its x/y coordinates")
  (present? [element] "Returns true if the element exists and is visible")
  (element-size [element] "Return the size of the given `element` as a map containing `:width` and `:height` values in pixels.")
  (tag [element] "Retrieve the name of the HTML tag of the given element object (returned as a keyword)")
  (text [element] "Retrieve the content, or inner HTML, of a given element object")
  (value [element] "Retrieve the `value` attribute of the given element object")
  (visible? [element] "Returns true if the given element object is visible/displayed")
  (xpath [element] "Retrieve the XPath of an element"))

;; ### Acting on Form-Specific Elements ###
(defprotocol IFormElement
  "Actions for form elements"
  (clear [element] "Clear the contents of the given element object")
  (deselect [element] "Deselect a given element object")
  (enabled? [element] "Returns true if the given element object is enabled")
  (input-text [element s] "Type the string of keys into the element object")
  (submit [element] "Submit the form which contains the given element object")
  (select [element] "Select a given element object")
  (selected? [element] "Returns true if the given element object is selected")
  (send-keys [element s] "Type the string of keys into the element object")
  (toggle [element] "If the given element object is a checkbox, this will toggle its selected/unselected state. In Selenium 2, `.toggle()` was deprecated and replaced in usage by `.click()`."))

;; ### Acting on Select Elements ###
(defprotocol ISelectElement
  "Actions specific to select lists"
  (all-options [select-element] "Retrieve all options from the given select list")
  (all-selected-options [select-element] "Retrieve a seq of all selected options from the select list described by `by`")
  (deselect-option [select-element attr-val] "Deselect an option from a select list, either by `:value`, `:index` or `:text`")
  (deselect-all [select-element] "Deselect all options for a given select list. Does not leverage WebDriver method because WebDriver's isMultiple method is faulty.")
  (deselect-by-index [select-element idx] "Deselect the option at index `idx` for the select list described by `by`. Indeces begin at 0")
  (deselect-by-text [select-element text] "Deselect all options with visible text `text` for the select list described by `by`")
  (deselect-by-value [select-element value] "Deselect all options with value `value` for the select list described by `by`")
  (first-selected-option [select-element] "Retrieve the first selected option (or the only one for single-select lists) from the given select list")
  (multiple? [select-element] "Return true if the given select list allows for multiple selections")
  (select-option [select-element attr-val] "Select an option from a select list, either by `:value`, `:index` or `:text`")
  (select-all [select-element] "Select all options for a given select list")
  (select-by-index [select-element idx] "Select an option by its index in the given select list. Indeces begin at 0.")
  (select-by-text [select-element text] "Select all options with visible text `text` in the select list described by `by`")
  (select-by-value [select-element value] "Select all options with value `value` in the select list described by `by`"))

(defprotocol IActions
  "Methods available in the Actions class"
  (click-and-hold
    [this]
    [this element] "Drag and drop, either at the current mouse position or in the middle of a given `element`.")
  (double-click
    [this]
    [this element] "Double click, either at the current mouse position or in the middle of a given `element`.")
  (drag-and-drop [this element-a element-b] "Drag and drop `element-a` onto `element-b`.")
  (drag-and-drop-by [this element x-y-map] "Drag `element` by `x` pixels to the right and `y` pixels down.")
  (key-down
    [this k]
    [this element k] "Press the given key (e.g., (key-press driver :enter))")
  (key-up
    [this k]
    [this element k] "Release the given key (e.g., (key-press driver :enter))")
  (move-by-offset [driver x y] "Move mouse by `x` pixels to the right and `y` pixels down.")
  (move-to-element
    [this element]
    [this element x y] "Move the mouse to the given element, or to an offset from the given element.")
  (perform [this] "Perform the composite action chain.")
  (release
    [this]
    [this element] "Release the left mouse button, either at the current mouse position or in the middle of the given `element`."))

;; ## Starting Browser ##
(def ^{:doc "Map of keywords to available WebDriver classes."}
  webdriver-drivers
  {:firefox FirefoxDriver
   :ie InternetExplorerDriver
   :internet-explorer InternetExplorerDriver
   :chrome ChromeDriver
   :chromium ChromeDriver
   :htmlunit HtmlUnitDriver})

(def phantomjs-enabled?
  (try
    (import '[org.openqa.selenium.phantomjs PhantomJSDriver PhantomJSDriverService])
    true
    (catch Throwable _ false)))

(defmulti new-webdriver
  "Return a Selenium-WebDriver WebDriver instance, with particularities of each browser supported."
  :browser)

(defmethod new-webdriver :default
  [{:keys [browser]}]
  (let [^Class klass (or (browser webdriver-drivers) browser)]
    (.newInstance
     (.getConstructor klass (into-array Class []))
     (into-array Object []))))

(defmethod new-webdriver :firefox
  [{:keys [browser ^FirefoxProfile profile]}]
  (if profile
    (FirefoxDriver. profile)
    (FirefoxDriver.)))

(defmethod new-webdriver :phantomjs
  [{:keys [phantomjs-executable] :as browser-spec}]
  (if-not phantomjs-enabled?
    (throw (RuntimeException. "You do not have the PhantomJS JAR's on the classpath. Please add com.codeborne/phantomjsdriver version 1.2.1 with exclusions for org.seleniumhq.selenium/selenium-java and any other org.seleniumhq.selenium JAR's your code relies on."))
    (let [caps (DesiredCapabilities.)
          klass (Class/forName "org.openqa.selenium.phantomjs.PhantomJSDriver")
          ;; Second constructor takes single argument of Capabilities
          ctors (into [] (.getDeclaredConstructors klass))
          ctor-sig (fn [^Constructor ctor]
                     (= (into-array Class [Capabilities])
                        (.getParameterTypes ctor)))
          phantomjs-driver-ctor (first (filterv ctor-sig ctors))]
      ;; Seems to be able to find it if on PATH by default, like Chrome's driver
      (when phantomjs-executable
        (let [klass (Class/forName "org.openqa.selenium.phantomjs.PhantomJSDriverService")
              field (.getField klass "PHANTOMJS_EXECUTABLE_PATH_PROPERTY")]
          (.setCapability ^DesiredCapabilities caps
                          ^String (.get field klass)
                          ^String phantomjs-executable)))
      (.newInstance ^Constructor phantomjs-driver-ctor (into-array java.lang.Object [caps])))))

(defn desired-capabilities
  ([m] (desired-capabilities (DesiredCapabilities.) m))
  ([^DesiredCapabilities capabilities m]
   (doseq [[^String k v] (java-keys m)]
     (.setCapability capabilities k v))))

;; Borrowed from core Clojure
(defmacro with-driver
  "Given a binding to `WebDriver`, make that binding available in `body` and ensure `quit` is called on it at the end."
  [bindings & body]
  (assert-args
   (vector? bindings) "a vector for its binding"
   (even? (count bindings)) "an even number of forms in binding vector")
  (cond
    (zero? (count bindings)) `(do ~@body)
    (symbol? (bindings 0)) `(let ~(subvec bindings 0 2)
                              (try
                                (with-driver ~(subvec bindings 2) ~@body)
                                (finally
                                  (quit ~(bindings 0)))))
    :else (throw (IllegalArgumentException.
                  "with-driver only allows symbols in bindings"))))

(load "core_by")
(load "core_element")
(load "core_wait")
(load "core_driver")
(load "core_window")
(load "core_actions")
