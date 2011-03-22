;; Clojure Wrapper for Selenium-WebDriver
;;
;; WebDriver is a library that allows for easy manipulation of the Firefox,
;; Chrome, Safari and  Internet Explorer graphical browsers, as well as the
;; Java-based HtmlUnit headless browser.
;;
;; This library simply wraps around the core API provided by WebDriver, to
;; include things like navigating to and from URL's, finding and interacting
;; with elements within HTML pages, and handling browser cookies.
;;
;; Credits to mikitebeka's `webdriver-clj` project on Github for a starting-
;; point for this project and many of the low-level wrappers around the
;; WebDriver API.
;;
(ns clj-webdriver.core
  (:use [clj-webdriver util record])
  (:import [clj-webdriver.record WindowHandle]
           [org.openqa.selenium By WebDriver WebElement Speed Cookie
                                NoSuchElementException]
           [org.openqa.selenium.firefox FirefoxDriver]
           [org.openqa.selenium.ie InternetExplorerDriver]
           [org.openqa.selenium.chrome ChromeDriver]
           [org.openqa.selenium.htmlunit HtmlUnitDriver]
           [org.openqa.selenium.support.ui Select]
           [java.util Date]
           [java.io File]))

(def *drivers* ^{:doc "Drivers that are available via WebDriver"}
  {
   :firefox FirefoxDriver
   :ie InternetExplorerDriver
   :chrome ChromeDriver
   :htmlunit HtmlUnitDriver })

(defn new-driver
  "Create new driver instance given a browser type. If an additional profile object or string is passed in, Firefox will be started with the given profile instead of the default."
  ([browser]
     (.newInstance (*drivers* browser)))
  ([browser profile]
     (when (not= :firefox browser)
       (throw (IllegalArgumentException. "Only Firefox supports profiles")))
     (FirefoxDriver. profile)))

(defn get-url
  "Navigate the driver to a given URL"
  [driver url]
  (.get driver url))

(defn start
  "Shortcut to instantiate a driver, navigate to a URL, and return the driver for further use"
  [browser url]
  (let [d (new-driver browser)]
    (do
      (get-url d url)
      d)))

(defn current-url
  "Retrieve the URL of the current page"
  [driver]
  (.getCurrentUrl driver))

(defn title
  "Retrieve the title of the current page as defined in the `head` tag"
  [driver]
  (.getTitle driver))

(defn page-source
  "Retrieve the source code of the current page"
  [driver]
  (.getPageSource driver))

(defn close
  "Close this browser instance"
  [driver]
  (.close driver))

; TODO catch webdriver exception
(defn quit
  "Destroy this browser instance"
  [driver]
  (.quit driver))

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
  (WindowHandle. (.getWindowHandle driver)
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
        all-handles (seq (.getWindowHandles driver))
        handle-records (for [handle all-handles]
                         (let [b (switch-to-window driver handle)]
                           (WindowHandle. handle
                                          (title b)
                                          (current-url b))))]
    (switch-to-window driver current-handle)
    handle-records))

(defn window-handles*
  "For WebDriver API compatibility: this simply wraps `.getWindowHandles`"
  [driver]
  (seq (.getWindowHandles driver)))

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

;; ## Navigation Interface
(defn back
  "Go back to the previous page in \"browsing history\""
  [driver]
  (.back (.navigate driver)))

(defn forward
  "Go forward to the next page in \"browsing history\"."
  [driver]
  (.forward (.navigate driver)))

(defn to
  "Navigate to a particular URL. Arg `url` can be either String or java.net.URL. Equivalent to the `get` function, provided here for compatibility with WebDriver API."
  [driver url]
  (.to (.navigate driver) url))

(defn refresh
  "Refresh the current page"
  [driver]
  (.refresh (.navigate driver)))

;; ## TargetLocator Interface
(defn switch-to-frame
  "Switch focus to a particular HTML frame"
  [driver frame]
  (.frame (.switchTo driver) frame))

(defn switch-to-window
  "Switch focus to a particular open window"
  [driver handle]
  (cond
    (string? handle) (.window (.switchTo driver) handle)
    (= (class handle) clj-webdriver.record.WindowHandle) (.window (.switchTo driver) (:handle handle))
    (number? handle) (switch-to-window driver (nth (window-handles driver) handle))
    (nil? handle) (throw (RuntimeException. "No window can be found"))
    :else (.window (.switchTo driver) handle)))

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

;; ## Option Interface

(defn new-cookie
  "Create a new cookie instance"
  ([name value] (new-cookie name value "/" nil))
  ([name value path] (new-cookie name value path nil))
  ([name value path date] (new Cookie name value path date)))

(defn cookie-name
  "Retrieve the name of a particular cookie"
  [cookie]
  (.getName cookie))

(defn cookie-value
  "Retrieve the value of a particular cookie"
  [cookie]
  (.getValue cookie))

(defn add-cookie
  "Add a new cookie to the browser session"
  [driver cookie]
  (.addCookie (.manage driver) cookie))

(defn delete-cookie-named
  "Delete a cookie given its name"
  [driver name]
  (.deleteCookieNamed (.manage driver) name))

(defn delete-cookie
  "Delete a cookie given a cookie instance"
  [driver cookie]
  (.deleteCookie (.manage driver) cookie))

(defn delete-all-cookies
  "Delete all cookies defined in the current session"
  [driver]
  (.deleteAllCookies (.manage driver)))

(defn cookies
  "Retrieve a set of cookies defined in the current session"
  [driver]
  (into #{} (.getCookies (.manage driver))))

(defn cookie-named
  "Retrieve a cookie object given its name"
  [driver name]
  (.getCookieNamed (.manage driver) name))


(def *slow-speed* Speed/SLOW)
(def *medium-speed* Speed/MEDIUM)
(def *fast-speed* Speed/FAST)

(defn speed
  "Set the speed at which the browser should execute commands"
  ([driver] (.getSpeed (.manage driver)))
  ([driver speed] (.setSpeed (.manage driver) speed)))

;; ## By* Functions

(defn by-id
  "Used when finding elements. Returns `By/id` of `expr`"
  [expr]
  (By/id expr))

(defn by-link-text
  "Used when finding elements. Returns `By/linkText` of `expr`"
  [expr]
  (By/linkText expr))

(defn by-partial-link-text
  "Used when finding elements. Returns `By/partialLinkText` of `expr`"
  [expr]
  (By/partialLinkText expr))

(defn by-name
  "Used when finding elements. Returns `By/name` of `expr`"
  [expr]
  (By/name expr))

(defn by-tag-name
  "Used when finding elements. Returns `By/tagName` of `expr`"
  [expr]
  (By/tagName expr))

(defn by-xpath
  "Used when finding elements. Returns `By/xpath` of `expr`"
  [expr]
  (By/xpath expr))

(defn by-class-name
  "Used when finding elements. Returns `By/className` of `expr`"
  [expr]
  (By/className expr))

(defn by-css-selector
  "Used when finding elements. Returns `By/cssSelector` of `expr`"
  [expr]
  (By/cssSelector expr))

;; Inspired by the `attr=`, `attr-contains` in Christophe Grand's enlive
(defn by-attr=
  "Use `value` of arbitrary attribute `attr` to find an element. You can optionally specify the tag.
   For example: `(by-attr= :id \"element-id\")`
                `(by-attr= :div :class \"content\")`"
  ([attr value] (by-attr= :* attr value)) ; default to * any tag
  ([tag attr value]
     (cond
         (= :class attr)  (by-class-name value)
         (= :id attr)     (by-id value)
         (= :name attr)   (by-name value)
         (= :tag attr)    (by-tag-name value)
         (= :text attr)   (by-link-text value)
         :else   (by-xpath (str "//"                  ; anywhere in DOM
                                (name tag)            ; tag from kw
                                "[@" (name attr)      ; attr from kw
                                "='" value "']")))))  ; ="value"

(defn by-attr-contains
  "Match if `value` is contained in the value of `attr`. You can optionally specify the tag.
   For example: `(by-attr-contains :class \"navigation\")`
                `(by-attr-contains :ul :class \"tags\")`"
  ([attr value] (by-attr-contains :* attr value)) ; default to * any tag
  ([tag attr value]
     (by-xpath (str "//"                 ; anywhere in DOM
                    (name tag)           ; tag from kw
                    "[contains(@"        ; xpath "contains" function
                    (name attr)          ; attr from kw
                    ",'" value "')]")))) ; ,'value')]

(defn by-attr-starts
  "Match if `value` is at the beginning of the value of `attr`. You can optionally specify the tag."
  ([attr value] (by-attr-starts :* attr value))
  ([tag attr value]
     (by-xpath (str "//"                 ; anywhere in DOM
                    (name tag)           ; tag from kw
                    "[starts-with(@"     ; xpath "starts-with" function
                    (name attr)          ; attr from kw
                    ",'" value "')]")))) ; ,'value')]

;; I can't add more functions like `by-attr-ends` or `by-attr-matches` (regex) due
;; to lack of uniform XPath support in WebDriver

;; ##  WebElement
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
  (.getValue element))

(defn clear
  "Clear the contents of the given element object"
  [element]
  (.clear element))

(defn tag-name
  "Retrieve the name of the HTML tag of the given element object"
  [element]
  (.getTagName element))

(defn attribute
  "Retrieve the value of the attribute of the given element object"
  [element attr]
  (.getAttribute element (name attr)))

(defn toggle
  "If the given element object is a checkbox, this will toggle its selected/unselected state"
  [element]
  (.toggle element))

(defn selected?
  "Returns true if the given element object is selected"
  [element]
  (.isSelected element))

(defn select
  "Select a given element object"
  [element]
  (.setSelected element))

(defn enabled?
  "Returns true if the given element object is enabled"
  [element]
  (.isEnabled element))

;; I like Watir-WebDriver's "visible?"; "displayed?" also used to align
;; with WebDriver's word choice
(defn visible?
  "Returns true if the given element object is visible/displayed"
  [element]
  (.isDisplayed element))

(def displayed? ^{:doc "Returns true if the given element object is visible/displayed"} visible?)

(defn text
  "Retrieve the content, or inner HTML, of a given element object"
  [element]
  (.getText element))

(defn send-keys
  "Type the string of keys into the element object"
  [element s]
  (.sendKeys element (into-array CharSequence (list s))))

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
  (.dragAndDropBy element x y))

(defn drag-and-drop-on
  "Drag `element-a` onto `element-b`. The (0,0) coordinates (top-left corners) of each element are aligned."
  [element-a element-b]
  (.dragAndDropOn element-a element-b))

;; ## org.openqa.selenium.support.ui.Select class

(defn deselect-all
  "Clear all selected entries for select list described by `by`"
  [element]
  (let [select-list (Select. element)]
    (.deselectAll select-list)))

(defn deselect-by-index
  "Deselect the option at index `idx` for the select list described by `by`. Indeces begin at 1"
  [element idx]
  (let [idx-human (dec idx)
        select-list (Select. element)]
    (.deselectByIndex select-list idx-human)))

(defn deselect-by-value
  "Deselect all options with value `value` for the select list described by `by`"
  [element value]
  (let [select-list (Select. element)]
    (.deselectByValue select-list value)))

(defn deselect-by-text
  "Deselect all options with visible text `text` for the select list described by `by`"
  [element text]
  (let [select-list (Select. element)]
    (.deselectByVisibleText select-list text)))

(defn all-selected-options
  "Retrieve a seq of all selected options from the select list described by `by`"
  [element]
  (let [select-list (Select. element)]
    (seq (.getAllSelectedOptions select-list))))

(defn first-selected-option
  "Retrieve the first selected option (or the only one for single-select lists) from the select list described by `by`"
  [element]
  (let [select-list (Select. element)]
    (.getFirstSelectedOption select-list)))

(defn all-options
  "Retrieve all options in the select list described by `by`"
  [element]
  (let [select-list (Select. element)]
    (seq (.getOptions select-list))))

(defn multiple?
  "Return true if the select list described by `by` allows for multiple selections"
  [element]
  (let [select-list (Select. element)]
    (.isMultiple select-list)))

(defn select-by-index
  "Select an option by its index in the select list described by `by`. Indeces begin at 1."
  [element idx]
  (let [idx-human (dec idx)
        select-list (Select. element)]
    (.selectByIndex select-list idx-human)))

(defn select-by-value
  "Select all options with value `value` in the select list described by `by`"
  [element value]
  (let [select-list (Select. element)]
    (.selectByValue select-list value)))

(defn select-by-text
  "Select all options with visible text `text` in the select list described by `by`"
  [element text]
  (let [select-list (Select. element)]
    (.selectByVisibleText select-list text)))

;; ## Element-finding Utilities

(declare find-it)
(declare find-them)

(defn find-element
  "Retrieve the element object of an element described by `by`"
  [driver by]
  (try (.findElement driver by)
  (catch NoSuchElementException e nil)))

(defn find-elements
  "Retrieve a seq of element objects described by `by`"
  [driver by]
  (try (seq (.findElements driver by))
  (catch NoSuchElementException e [])))

(defn find-elements-by-regex-alone
  "Given an `attr-val` pair with a regex value, find the elements that match"
  [driver tag attr-val]
  (let [entry (first attr-val)
        attr (key entry)
        value (val entry)
        all-elements (find-elements driver (by-xpath (str "//" (name tag))))] ; get all elements
    (if (= :text attr)
      (filter #(re-find value (text %)) all-elements)
      (filter (fn [el]
                ((fnil (partial re-find value) "") ; `(attribute)` will return nil if the HTML element in question
                 (attribute el (name attr))))      ; doesn't support the attribute being passed in (e.g. :href on a <p>)
              all-elements))))                     ; so "" is fnil'ed to avoid a NullPointerException for `re-find`

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

(defn find-elements-by-regex
  [driver tag attr-val]
  (if (all-regex? attr-val)
    (let [elements (find-elements driver (by-xpath "//*"))]
      (filter-elements-by-regex elements attr-val))
    (let [attr-vals-without-regex (into {}
                                        (remove
                                         #(let [[k v] %] (= java.util.regex.Pattern (class v)))
                                         attr-val))
          elements (find-them driver tag attr-vals-without-regex)]
      (filter-elements-by-regex elements attr-val))))

(defn find-window-handles
  "Given a browser `driver` and a map of attributes, return the WindowHandle that matches"
  [driver attr-val]
  (if (contains? attr-val :index)
    (nth (window-handles driver) (:index attr-val))
    (filter #(every? (fn [[k v]] (= (k %) v)) attr-val) (window-handles driver))))

(defn find-it
  "Given a WebDriver `driver`, find the browser element that matches the query"
  ([driver attr-val]
     (cond
      (keyword? attr-val)
      (find-element driver (by-tag-name (name attr-val))) ; supplied just :tag
      (vector? attr-val)
      (if (query-with-ancestry-has-regex? attr-val)
        (if (query-with-ancestry-has-regex? (drop-last 2 attr-val))
          (throw (IllegalArgumentException.
                  (str "You may not pass in a regex until "
                       "the last attribute-value pair")))
          (first
           (filter-elements-by-regex
            (find-elements driver (by-xpath (str (build-xpath-with-ancestry attr-val) "//*")))
            (last attr-val))))
        (find-element driver (by-xpath (build-xpath-with-ancestry attr-val)))) ; supplied vector of queries in hierarchy
      (map? attr-val)
      (find-it driver :* attr-val))) ; no :tag specified, use global *
  ([driver tag attr-val]
     (when (keyword? driver) ; I keep forgetting to pass in the WebDriver instance while testing
       (throw (IllegalArgumentException.
               (str "The first parameter to find-it must be an instance of WebDriver."))))
     (cond
      (and
       (>  (count attr-val) 1)
       (or (contains? attr-val :xpath) (contains? attr-val :css)))      (throw (IllegalArgumentException.
                                                                                (str "If you want to find an element via XPath or CSS, "
                                                                                     "you may pass in one and only one attribute (:xpath or :css)")))
      (= tag :window) (first (find-window-handles driver attr-val))
      (= 1 (count attr-val)) (let [entry (first attr-val)
                                   attr  (key entry)
                                   value (val entry)]
                               (cond
                                (= java.util.regex.Pattern (class value)) (first (find-elements-by-regex-alone driver tag attr-val))
                                (= :xpath attr) (find-element driver (by-xpath value))
                                (= :css attr)   (find-element driver (by-css-selector value))
                                :else           (find-element driver (by-attr= tag attr value))))
      (contains-regex? attr-val) (first (find-elements-by-regex driver tag attr-val))
      :else (find-element driver (by-xpath (build-xpath tag attr-val))))))

(defn find-them
  "Given a browser `driver`, find the browser elements that match the query"
  ([driver attr-val]
     (cond
      (keyword? attr-val)
      (find-elements driver (by-tag-name (name attr-val))) ; supplied just :tag
      (vector? attr-val)
      (if (query-with-ancestry-has-regex? attr-val)
        (if (query-with-ancestry-has-regex? (drop-last 2 attr-val))
          (throw (IllegalArgumentException.
                  (str "You may not pass in a regex until "
                       "the last attribute-value pair")))
          (filter-elements-by-regex
           (find-elements driver (by-xpath (str (build-xpath-with-ancestry attr-val) "//*")))
           (last attr-val)))
        (find-elements driver (by-xpath (build-xpath-with-ancestry attr-val)))) ; supplied vector of queries in hierarchy
      (map? attr-val)
      (find-them driver :* attr-val))) ; no :tag specified, use global *
  ([driver tag attr-val]
     (when (keyword? driver) ; I keep forgetting to pass in the WebDriver instance while testing
       (throw (IllegalArgumentException.
               (str "The first parameter to find-it must be an instance of WebDriver."))))     
     (cond
      (and
       (>  (count attr-val) 1)
       (or (contains? attr-val :xpath) (contains? attr-val :css))) (throw (IllegalArgumentException.
                                                                           (str "If you want to find an element via XPath or CSS, "
                                                                                "you may pass in one and only one attribute (:xpath or :css)")))
      (= tag :window) (find-window-handles driver attr-val)
      (= 1 (count attr-val)) (let [entry (first attr-val)
                                   attr  (key entry)
                                   value (val entry)]
                               (cond
                                (= java.util.regex.Pattern (class value)) (find-elements-by-regex-alone driver tag attr-val)
                                (= :xpath attr) (find-elements driver (by-xpath value))
                                (= :css attr)   (find-elements driver (by-css-selector value))
                                :else           (find-elements driver (by-attr= tag attr value))))
      (contains-regex? attr-val) (find-elements-by-regex driver tag attr-val)
      :else (find-elements driver (by-xpath (build-xpath tag attr-val))))))