;; # Clojure Wrapper for Selenium-WebDriver #
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
(ns clj-webdriver.core
  (:use [clj-webdriver driver util window-handle options])
  (:require [clj-webdriver.js.browserbot :as browserbot-js]
            [clj-webdriver.cache :as cache])
  (:import [clj_webdriver.driver Driver]
           [org.openqa.selenium By WebDriver WebElement Cookie
                                NoSuchElementException]
           [org.openqa.selenium.firefox FirefoxDriver]
           [org.openqa.selenium.ie InternetExplorerDriver]
           [org.openqa.selenium.chrome ChromeDriver]
           [org.openqa.selenium.htmlunit HtmlUnitDriver]
           [org.openqa.selenium.support.ui Select]
           [java.util Date]
           [java.io File]))


;; ## Driver Management ##
(def webdriver-drivers
  {:firefox FirefoxDriver
   :ie InternetExplorerDriver
   :chrome ChromeDriver
   :htmlunit HtmlUnitDriver})

(defn new-webdriver*
  "Instantiate a new WebDriver instance given a browser type. If an additional profile object or string is passed in, Firefox will be started with the given profile instead of the default."
  ([browser]
     (.newInstance (webdriver-drivers (keyword browser))))
  ([browser profile]
     {:pre [(= browser :firefox)]}
     (FirefoxDriver. profile)))

(defn new-driver
  "Create new Driver given a browser type. If an additional profile object or string is passed in, Firefox will be started with the given profile instead of the default.

   This is the preferred method for starting up a browser, as it leverages clj-webdriver-specific functionality not available with vanilla WebDriver instances. You can always access the underlying WebDriver instance with the :webdriver key of your Driver record."
  ([browser]
     (init-driver (new-webdriver* browser)))
  ([browser cache-spec]
     (init-driver (new-webdriver* browser) cache-spec))
  ([browser cache-spec cache-args]
     (init-driver (new-webdriver* browser) cache-spec cache-args)))

;;; Protocols for API ;;;
(defprotocol IDriver
  "Basics of driver handling"
  (get-url [driver url] "Navigate the driver to a given URL")
  (to [driver url] "Navigate to a particular URL. Arg `url` can be either String or java.net.URL. Equivalent to the `get` function, provided here for compatibility with WebDriver API.")
  (current-url [driver] "Retrieve the URL of the current page")
  (title [driver] "Retrieve the title of the current page as defined in the `head` tag")
  (page-source [driver] "Retrieve the source code of the current page")
  (close [driver] "Close this browser instance, switching to an active one if more than one is open")
  (quit [driver] "Destroy this browser instance")
  (back [driver] "Go back to the previous page in \"browsing history\"")
  (forward [driver] "Go forward to the next page in \"browsing history\".")
  (refresh [driver] "Refresh the current page"))

;;; ## Windows and Frames ##
(defprotocol ITargetLocator
  "Functions that deal with browser windows and frames"
  (window-handle [driver] "Get the only (or first) window handle, return as a WindowHandler record")
  (window-handles [driver] "Retrieve a vector of `WindowHandle` records which can be used to switchTo particular open windows")
  (other-window-handles [driver] "Retrieve window handles for all windows except the current one")
  (switch-to-frame [driver frame] "Switch focus to a particular HTML frame")
  (switch-to-window [driver handle] "Switch focus to a particular open window")
  (switch-to-other-window [driver] "Given that two and only two browser windows are open, switch to the one not currently active")
  (switch-to-default [driver] "Switch focus to the first first frame of the page, or the main document if the page contains iframes")
  (switch-to-active [driver] "Switch to element that currently has focus, or to the body if this cannot be detected"))

(defprotocol IFind
  "Functions used to locate elements on a given page"
  (find-element [driver by] "Retrieve the element object of an element described by `by`")
  (find-elements [driver by] "Retrieve a seq of element objects described by `by`")
  (find-elements-by-regex-alone [driver tag attr-val] "Given an `attr-val` pair with a regex value, find the elements that match")
  (find-elements-by-regex [driver tag attr-val])
  (find-window-handles [driver attr-val] "Given a browser `driver` and a map of attributes, return the WindowHandle that matches")
  (find-semantic-buttons [driver attr-val] "Find HTML element that is either a `<button>` or an `<input>` of type submit, reset, image or button")
  (find-semantic-buttons-by-regex [driver attr-val] "Semantic buttons are things that look or behave like buttons but do not necessarily consist of a `<button>` tag")
  (find-checkables-by-text [driver attr-val] "Finding the 'text' of a radio or checkbox is complex. Handle it here.")
  (find-table-cells [driver attr-val] "Given a WebDriver `driver` and a vector `attr-val`, find the correct")
  (find-them*
    [driver attr-val]
    [driver tag attr-val] "Given a browser `driver`, return the elements that match the query")
  (find-them
    [driver attr-val]
    [driver tag attr-val] "Call find-them*, then make sure elements are actually returned; if not, throw NoSuchElementException so other code can handle exceptions appropriately")
  (find-it
    [driver attr-val]
    [driver tag attr-val] "Call (first (find-them args))"))

(defn start
  "Shortcut to instantiate a driver, navigate to a URL, and return the driver for further use"
  ([browser url] (start browser url :driver))
  ([browser url driver-type]
     (let [driver (if (= :webdriver driver-type)
                    (new-webdriver* browser)
                    (new-driver browser))]
       (get-url driver url)
       driver)))


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

;; ## By* Functions ##
(load "core_by")

;; ##  Actions on WebElements ##
(declare execute-script)
(declare execute-script*)
(defn- browserbot
  [driver fn-name & arguments]
  (let [script (str browserbot-js/script
                    "return browserbot."
                    fn-name
                    ".apply(browserbot, arguments)")
        execute-js-fn (partial execute-script* driver script)]
    (apply execute-js-fn arguments)))

(defn click
  "Click a particular HTML element"
  [element]
  (.click element)
  (cache/set-status :check))

(defn submit
  "Submit the form which contains the given element object"
  [element]
  (.submit element)
  (cache/set-status :flush))

(defn value
  "Retrieve the `value` attribute of the given element object"
  [element]
  (.getAttribute element "value"))

(defn clear
  "Clear the contents of the given element object"
  [element]
  (.clear element)
  element)

(defn tag-name
  "Retrieve the name of the HTML tag of the given element object"
  [element]
  (.getTagName element))

(defn attribute
  "Retrieve the value of the attribute of the given element object"
  [element attr]
  (.getAttribute element (name attr)))

(defn selected?
  "Returns true if the given element object is selected"
  [element]
  (.isSelected element))

(defn select
  "Select a given element object"
  [element]
  (.click element)
  element)

(defn toggle
  "If the given element object is a checkbox, this will toggle its selected/unselected state. In Selenium 2, `.toggle()` was deprecated and replaced in usage by `.click()`."
  [element]
  (.click element)
  element)

(defn deselect
  "Deselect a given element object"
  [element]
  (if (.isSelected element)
    (toggle element)
    element))

(defn enabled?
  "Returns true if the given element object is enabled"
  [element]
  (.isEnabled element))

(defmacro exists?
  "Returns element matching the find-it-form if it exists, or nil if it does not"
  [find-it-form]
  `(try 
      ~find-it-form
      (catch org.openqa.selenium.NoSuchElementException e#
        nil)))

(defn visible?
  "Returns true if the given element object is visible/displayed"
  [element]
  (.isDisplayed element))

(def displayed? ^{:doc "Returns true if the given element object is visible/displayed"} visible?)
(def present? ^{:doc "Returns true if the given element object is visible/displayed"} visible?)

(defn flash
  "Flash the element in question, to verify you're looking at the correct element"
  [element]
  (let [original-color (if (.getCssValue element "background-color")
                         (.getCssValue element "background-color")
                         "transparent")
        orig-colors (repeat original-color)
        change-colors (interleave (repeat "red") (repeat "blue"))]
    (doseq [flash-color (take 12 (interleave change-colors orig-colors))]
      (execute-script* (.getWrappedDriver element)
                      (str "arguments[0].style.backgroundColor = '"
                           flash-color "'")
                      element)
      (Thread/sleep 80)))
  element)

(defn text
  "Retrieve the content, or inner HTML, of a given element object"
  [element]
  (.getText element))

(defn html
  "Retrieve the outer HTML of an element"
  [element]
  (browserbot (.getWrappedDriver element) "getOuterHTML" element))

(defn xpath
  "Retrieve the XPath of an element"
  [element]
  (browserbot (.getWrappedDriver element) "getXPath" element []))

(defn focus
  "Apply focus to the given element"
  [element]
  (execute-script*
   (.getWrappedDriver element) "return arguments[0].focus()" element))

(defn send-keys
  "Type the string of keys into the element object"
  [element s]
  (.sendKeys element (into-array CharSequence (list s)))
  element)

(def input-text send-keys)

(defn location
  "Given an element object, return its location as a map of its x/y coordinates"
  [element]
  (let [loc (.getLocation element)
        x   (.x loc)
        y   (.y loc)]
    {:x x, :y y}))

(defn location-once-visible
  "Given an element object, return its location on the screen once it is scrolled into view as a map of its x/y coordinates. The window will scroll as much as possible until the element hits the top of the page; thus even visible elements will be scrolled until they reach that point."
  [element]
  (let [loc (.getLocationOnScreenOnceScrolledIntoView element)
        x   (.x loc)
        y   (.y loc)]
    {:x x, :y y}))

(defn drag-and-drop-by
  "Drag an element by `x` pixels to the right and `y` pixels down. Use negative numbers for opposite directions."
  [element ^Integer x ^Integer y]
  (.dragAndDropBy element x y)
  element)

(defn drag-and-drop-on
  "Drag `element-a` onto `element-b`. The (0,0) coordinates (top-left corners) of each element are aligned."
  [element-a element-b]
  (.dragAndDropOn element-a element-b)
  element-a)

;; ## JavaScript Execution ##
(defn execute-script
  [driver js & js-args]
  (.executeScript (:webdriver driver) js (to-array js-args))
  driver)

(defn execute-script*
  "Version of execute-script that uses a WebDriver instance directly."
  [driver js & js-args]
  (.executeScript driver js (to-array js-args))
  driver)

;; ## Select Helpers ##
(load "core_select")

;; Helper function to find-*
(defn filter-elements-by-regex
  "Given a collection of WebElements, filter the collection by the regular expression values for the respective attributes in the `attr-val` map"
  [elements attr-val]
  (let [attr-vals-with-regex (into {}
                                   (filter
                                    #(let [[k v] %] (= java.util.regex.Pattern (class v)))
                                    attr-val))]
    (loop [elements elements attr-vals-with-regex attr-vals-with-regex]
      (if (empty? attr-vals-with-regex)
        elements
        (let [entry (first attr-vals-with-regex)
              attr (key entry)
              value (val entry)
              matching-elements (if (= :text attr)
                                  (filter #(re-find value (text %)) elements)
                                  (filter (fn [el]
                                            ((fnil (partial re-find value) "")
                                             (attribute el (name attr))))
                                          elements))]
          (recur matching-elements (dissoc attr-vals-with-regex attr)))))))

;; API with clj-webdriver's Driver implementation
(load "core_driver")
;; API with Selenium-WebDriver's WebDriver class
(load "core_webdriver")
