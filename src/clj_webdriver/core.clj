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
  (:use [clj-webdriver util window-handle]
        [clj-webdriver.protocols driver-basics target-locator
                                 wait options find])
  (:require [clj-webdriver.js.browserbot :as browserbot-js] :reload)
  (:import [org.openqa.selenium By WebDriver WebElement Cookie
                                NoSuchElementException]
           [org.openqa.selenium.firefox FirefoxDriver]
           [org.openqa.selenium.ie InternetExplorerDriver]
           [org.openqa.selenium.chrome ChromeDriver]
           [org.openqa.selenium.htmlunit HtmlUnitDriver]
           [org.openqa.selenium.support.ui Select WebDriverWait ExpectedCondition]
           [java.util Date]
           [java.io File]
           [java.util.concurrent TimeUnit]))


;; ## Driver Management ##
(def webdriver-drivers
  {:firefox FirefoxDriver
   :ie InternetExplorerDriver
   :chrome ChromeDriver
   :htmlunit HtmlUnitDriver})

(declare window-handles*)
(declare window-handle*)

(declare init-driver)
(defn new-driver
  "Create new driver instance given a browser type. If an additional profile object or string is passed in, Firefox will be started with the given profile instead of the default."
  ([browser]
     (init-driver (.newInstance (webdriver-drivers (keyword browser)))))
  ([browser profile]
     (when (not= :firefox (keyword browser))
       (throw (IllegalArgumentException. "Only Firefox supports profiles")))
     (FirefoxDriver. profile)))

(defn start
  "Shortcut to instantiate a driver, navigate to a URL, and return the driver for further use"
  [browser url]
  (let [driver (new-driver browser)]
    (get-url driver url)
    driver))

;; Include window/frame functions not included in ITargetLocator protocol
(load "core_window")

;; Functions dealing directly with cookie objects
(load "core_cookie")

;; ## By* Functions ##

(load "core_by")

;; ##  Actions on WebElements ##
(declare execute-script)
(defn- browserbot
  [driver fn-name & arguments]
  (let [script (str browserbot-js/script
                    "return browserbot."
                    fn-name
                    ".apply(browserbot, arguments)")
        driver (:webdriver driver) ;; not in a protocol
        execute-js-fn (partial execute-script driver script)]
    (apply execute-js-fn arguments)))

(defn click
  "Click a particular HTML element"
  [element]
  (.click element))

(defn submit
  "Submit the form which contains the given element object"
  [element]
  (.submit element))

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

(defn present?
  "Returns true if element exists and is visible"
  [element]
  (and element (visible? element)))

(defn flash
  "Flash the element in question, to verify you're looking at the correct element"
  [element]
  (let [original-color (if (.getCssValue element "background-color")
                         (.getCssValue element "background-color")
                         "transparent")
        orig-colors (repeat original-color)
        change-colors (interleave (repeat "red") (repeat "blue"))]
    (doseq [flash-color (take 12 (interleave change-colors orig-colors))]
      (execute-script (.getWrappedDriver element)
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
  (execute-script
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
  (.executeScript driver js (to-array js-args)))

;; TODO: Script Timeout (wait functionality)

;; ## Select Helpers ##

(load "core_select")

;; ## Element-finding Utilities ##

;; Helper functions kept in separate file yet in same namespace
;; because of interdependence on `find-them` function
(load "core_find")
(load "core_driver")
