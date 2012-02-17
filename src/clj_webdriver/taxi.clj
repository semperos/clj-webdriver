;; The faster way to use clj-webdriver: take a taxi
(ns clj-webdriver.taxi
  (:use [clj-webdriver.element :only [is-element?]])
  (:require [clj-webdriver.core :as core]
            [clj-webdriver.options :as options]
            [clj-webdriver.wait :as wait])
  (:import clj_webdriver.element.Element))

(declare css-finder)
(def ^:dynamic *driver*)
(def ^:dynamic *finder-fn* css-finder)

(defn- set-driver*
  "Given a `browser-spec`, instantiate a new Driver record and assign to `*driver*`."
  [browser-spec]
  (let [new-driver (core/new-driver browser-spec)]
       (alter-var-root (var *driver*)
                       (constantly new-driver)
                       (when (thread-bound? (var *driver*))
                         (set! *driver* new-driver)))))

(declare to)
(defn set-driver!
  "Set a default `Driver` for this thread, optionally sending it to a starting `url`."
  ([browser-spec] (set-driver* browser-spec))
  ([browser-spec url] (to (set-driver* browser-spec) url)))

(defn set-finder!
  "Set a default finder function, which will be used with all `q` parameters in functions that require an Element."
  [finder-fn]
  (alter-var-root (var *finder-fn*)
                  (constantly finder-fn)
                  (when (thread-bound? (var *finder-fn*))
                    (set! *finder-fn* finder-fn))))

(declare quit)
(defmacro with-driver
  "Given a `browser-spec` to start a browser, execute the forms in `body`, then call `quit` on the browser. Uses the default finder function."
  [browser-spec & body]
  `(binding [*driver* (core/new-driver ~browser-spec)]
    (try
      ~@body
      (finally
        (quit)))))

(defmacro with-driver-fn
  "Given a `browser-spec` to start a browser and a `finder-fn` to use as a finding function, execute the forms in `body`, then call `quit` on the browser."
  [browser-spec finder-fn & body]
  `(binding [*driver* (core/new-driver ~browser-spec)
             *finder-fn* ~finder-fn]
    (try
      ~@body
      (finally
        (quit)))))

(defn css-finder
  "Given a CSS query `q`, return a lazy seq of the elements found by calling `find-elements` with `by-css`. If `q` is an `Element`, it is returned unchanged."
  [q]
  (if (is-element? q)
    q
    (core/find-elements *driver* {:css q})))

(set-finder! css-finder)

(defn xpath-finder
  "Given a XPath query `q`, return a lazy seq of the elements found by calling `find-elements` with `by-xpath`. If `q` is an `Element`, it is returned unchanged."
  [q]
  (if (is-element? q)
    q
    (core/find-elements *driver* {:xpath q})))

;; Be able to get actual element/elements when needed
(defn element
  "Given a query `q`, return the first element that the default finder function returns."
  [q]
  (if (is-element? q)
    q
    (first (*finder-fn* q))))

(defn elements
  "Given a query `q`, return the elements that the default finder function returns."
  [q]
  (if (is-element? q)
    q
    (*finder-fn* q)))

;; Driver functions
(defn to
  "Navigate the browser to `url`."
  [url]
  (core/to *driver* url))

(defn back
  "Navigate back in the browser history, optionally `n` times."
  ([] (back 1))
  ([n]
     (dotimes [m n]
       (core/back *driver*))))

(defn close
  "Close the browser. If multiple windows are open, this only closes the active window."
  []
  (core/close *driver*))

(defn current-url
  "Return the current url of the browser."
  []
  (core/current-url *driver*))

(defn forward
  "Navigate forward in the browser history."
  ([] (forward 1))
  ([n]
     (dotimes [m n]
       (core/forward *driver*))))

(defn get-url
  "Navigate the browser to `url`."
  [url]
  (core/get-url *driver* url))

(defn take-screenshot
  "Take a screenshot of the browser's current page, optionally specifying the format (`:file`, `:base64`, or `:bytes`) and the `destination` (something that `clojure.java.io/file` will accept)."
  ([] (core/get-screenshot *driver*))
  ([format] (core/get-screenshot *driver* format))
  ([format destination] (core/get-screenshot *driver* format destination)))

(defn page-source
  "Return the source code of the current page in the browser."
  []
  (core/page-source *driver*))

(defn quit
  "Quit the browser completely, including all open windows."
  []
  (core/quit *driver*))

(defn refresh
  "Refresh the current page in the browser."
  []
  (core/refresh *driver*))

(defn title
  "Return the title of the current page in the browser."
  []
  (core/title *driver*))

(defn window-handle
  "Return a `WindowHandle` that contains information about the active window and can be used for switching."
  []
  (core/window-handle *driver*))

(defn window-handles
  "Return a `WindowHandle` for all open windows."
  []
  (core/window-handles *driver*))

(defn other-window-handles
  "Return a `WindowHandle` for all open windows except the active one."
  []
  (core/other-window-handles *driver*))

;; TODO: test coverage
(defn switch-to-frame
  "Switch focus to the frame found by the finder query `frame-q`.
 
   If you need the default behavior of `.frame()`, you can use clj-webdriver.core/switch-to-frame. For that function, you can pass either a number (the index of the frame on the page), a string (the `name` or `id` attribute of the target frame), or an `Element` of the frame."
  [frame-q]
  (core/switch-to-frame *driver* (element frame-q)))

(defn switch-to-window
  "Switch focus to the window for the given WindowHandle `handle`."
  [handle]
  (core/switch-to-window *driver* handle))

(defn switch-to-other-window
  "If two windows are open, switch focus to the other."
  []
  (core/switch-to-other-window *driver*))

(defn switch-to-default
  "Switch focus to the first first frame of the page, or the main document if the page contains iframes."
  []
  (core/switch-to-default *driver*))

(defn switch-to-active
  "Switch to the page element that currently has focus, or to the body if this cannot be detected."
  []
  (core/switch-to-active *driver*))

(defn add-cookie
  "Add the given `cookie` to the browser session."
  [cookie]
  (options/add-cookie *driver* cookie))

(defn delete-cookie
  "Provided the name of a cookie or a Cookie record itself, delete it from the browser session."
  [name-or-obj]
  (if (string? name-or-obj)
    (options/delete-cookie-named *driver* name-or-obj)
    (options/delete-cookie *driver* name-or-obj)))

(defn delete-all-cookies
  "Delete all cookies from the browser session."
  []
  (options/delete-all-cookies *driver*))

(defn cookies
  "Return all cookies in the browser session."
  []
  (options/cookies *driver*))

(defn cookie
  "Return the cookie with name `cookie-name`."
  [cookie-name]
  (options/cookie-named *driver* cookie-name))

(defn execute-script
  "Execute the JavaScript code `js` with arguments `js-args`. 

   See http://selenium.googlecode.com/svn/trunk/docs/api/java/org/openqa/selenium/remote/RemoteWebDriver.html#executeScript(java.lang.String, java.lang.Object...) for full details."
  [js & js-args] (apply (partial core/execute-script *driver* js) js-args))

(defn wait-until
  "Make the browser wait until the predicate `pred` returns true, providing an optional `timeout` period of time an optional `interval` on which to attempt the predicate."
  ([pred] (wait/wait-until *driver* pred))
  ([pred timeout] (wait/wait-until *driver* pred timeout))
  ([pred timeout interval] (wait/wait-until *driver* pred timeout interval)))

(defn implicit-wait
  "Set the global `timeout` that the browser should wait when attempting to find elements on the page, before timing out with an exception."
  [timeout]
  (wait/implicit-wait *driver* timeout))

(defn find-element-by
  "Find an element using the provided `by-clause`. See the functions in `core_by.clj`, available as in the `clj-webdriver.core` namespace."
  [by-clause]
  (core/find-element-by *driver* by-clause))

(defn find-elements-by
  "Find elements using the provided `by-clause`. See the functions in `core_by.clj`, available as in the `clj-webdriver.core` namespace."
  [by-clause]
  (core/find-elements-by *driver* by-clause))

(defn find-windows
  "Return all `WindowHandle` records that match the given `attr-val` map.

   Attributes can be anything in a `WindowHandle` record (`:title` or `:url`) or you can pass an `:index` key and a number value to select a window by its open order."
  [attr-val]
  (core/find-windows *driver* attr-val))

(defn find-window
  "Return the first `WindowHandle` record that matches the given `attr-val` map.

   Attributes can be anything in a `WindowHandle` record (`:title` or `:url`) or you can pass an `:index` key and a number value to select a window by its open order."
  [attr-val] (core/find-window *driver* attr-val))

(defn find-table-cell
  "Within the table found with query `table-q`, return the table cell at coordinates `coords`. The top-left cell has coordinates `[0 0]`."
  [table-q coords]
  (core/find-table-cell *driver* (element table-q) coords))

(defn find-table-row
  "Within the table found with query `table-q`, return the cells at row number `row`. The top-most row is row `0`."
  [table-q row]
  (core/find-table-row *driver* (element table-q) row))

;; Need to explain difference between element and find-element fn's
(defn find-elements
  "Return `Element` records that match the given `attr-val`.

   Whereas the `elements` function uses a query `q` with the default finder function, this function requires an `attr-val` parameter which is either a map or a vector of maps with special semantics for finding elements on the page.

   The `attr-val` map can consist of one or more of the following:

    * The key `:css` or `:xpath` and a query value (e.g., `{:css \"a.external\"}`)
    * The key `:tag` and an HTML tag (e.g., `{:tag :a}`)
    * An HTML element attribute and its value (e.g., `{:class \"external\"}`)
    * An HTML element attribute and a regular expression that partially matches the value (e.g., `{:class #\"exter\"}`)
    * A 'meta' tag `:button*`, `:radio`, `:checkbox`, `:textfield`, `:password`, `:filefield` (e.g., `{:tag :button*}`)
    * The key `:index` and the zero-based index (order) of the target element on the page (e.g., `{:index 2}` retrieves the third element that matches)"
  [attr-val]
  (core/find-elements *driver* attr-val))
(defn find-element
  "Return the first `Element` record that matches the given `attr-val`.

   Whereas the `element` function uses a query `q` with the default finder function, this function requires an `attr-val` parameter which is either a map or a vector of maps with special semantics for finding elements on the page.

   The `attr-val` map can consist of one or more of the following:

    * The key `:css` or `:xpath` and a query value (e.g., `{:css \"a.external\"}`)
    * The key `:tag` and an HTML tag (e.g., `{:tag :a}`)
    * An HTML element attribute and its value (e.g., `{:class \"external\"}`)
    * An HTML element attribute and a regular expression that partially matches the value (e.g., `{:class #\"exter\"}`)
    * A 'meta' tag `:button*`, `:radio`, `:checkbox`, `:textfield`, `:password`, `:filefield` (e.g., `{:tag :button*}`)
    * The key `:index` and the zero-based index (order) of the target element on the page (e.g., `{:index 2}` retrieves the third element that matches)"
  [attr-val]
  (core/find-element *driver* attr-val))


;; Element versions of find-element-by and find-elements-by

;; Querying "under" elements
;; This is the part that will see more love once #42 is fixed (decouple by-* determination)
;;
;; You can either use a by-foo function (in clj-webdriver.core), or a map.
;; The map will currently generate a (by-xpath ...) form for you based on the map,
;; but it's not as powerful as the core/find-element map syntax (which handles things
;; like button*, radio, checkbox, etc.).
(defn find-element-under
  "Find the first element that is a child of the element found with query `q-parent`, using the given `by-clause`. If `q-parent` is an `Element`, it will be used as-is. See the functions in `core_by.clj`, available as in the `clj-webdriver.core` namespace.

   Note that this function is intended to fit better with `find-element` by allowing a full `attr-val` map instead of a `by-clause`, which will be implemented pending a re-write of `find-elements`."
  [q-parent by-clause]
  (if (is-element? q-parent)
    (core/find-element q-parent by-clause)
    (core/find-element (element q-parent) by-clause)))

(defn find-elements-under
  "Find the elements that are children of the element found with query `q-parent`, using the given `by-clause`. If `q-parent` is an `Element`, it will be used as-is. See the functions in `core_by.clj`, available as in the `clj-webdriver.core` namespace.

   Note that this function is intended to fit better with `find-element` by allowing a full `attr-val` map instead of a `by-clause`, which will be implemented pending a re-write of `find-elements`."
  [q-parent by-clause]
  (if (is-element? q-parent)
    (core/find-elements q-parent by-clause)
    (core/find-elements (element q-parent) by-clause)))

;; Element functions
;;
;; Unlike their counterparts in core, you don't need to do a `(find-element ...)`
;; with these; just pass in a CSS query followed by other necessary parameters
;; and the first element that matches the query will be used automatically.
;;
;; If a CSS query string is not passed in, it's assumed you're trying to use these
;; functions like their core counterparts, in which case each function will default
;; back to core functionality (expecting that you're passing in an Element record)

(defn attribute
  "For the first element found with query `q`, return the value of the given `attribute`."
  [q attr]
  (core/attribute (element q) attr))

(defn click
  "Click the first element found with query `q`."
  [q]
  (core/click (element q)))

(defn displayed?
  "Return true if the first element found with query `q` is visible on the page."
  [q]
  (core/displayed? (element q)))

(defn drag-and-drop-by
  "Drag the first element found with query `q` by `x` pixels to the right and `y` pixels down. Use negative numbers for `x` or `y` to move left or up respectively."
  [q x y]
  (core/drag-and-drop-by (element q) x y))

(defn drag-and-drop-on
  "Drag the first element found with query `qa` onto the first element found with query `qb`."
  [qa qb]
  (core/drag-and-drop-on (element qa) (element qb)))

(defn exists?
  "Return true if the first element found with query `q` exists on the current page in the browser."
  [q]
  (core/exists? (element q)))

(defn flash
  "Flash the background color of the first element found with query `q`."
  [q]
  (core/flash (element q)))

(defn focus
  "Explicitly give the first element found with query `q` focus on the page."
  [q]
  (core/focus (element q)))

(defn html
  "Return the inner html of the first element found with query `q`."
  [q]
  (core/html (element q)))

(defn location
  "Return a map of `:x` and `:y` coordinates for the first element found with query `q`."
  [q]
  (core/location (element q)))

(defn location-once-visible
  "Return a map of `:x` and `:y` coordinates for the first element found with query `q` once the page has been scrolled enough to be visible in the viewport."
  [q]
  (core/location-once-visible (element q)))

(defn present?
  "Return true if the first element found with query `q` both exists and is visible on the page."
  [q]
  (core/present? (element q)))

(defn size
  "Return the size of the first element found with query `q` in pixels."
  [q]
  (core/size (element q)))

(defn rectangle
  "Return a `java.awt.Rectangle` with the position and dimensions of the first element found with query `q` (using the `location` and `size` functions)."
  [q]
  (core/rectangle (element q)))

(defn intersect?
  "Return true if the first element found with query `q` intersects with any of the elements found with queries `qs`."
  [q & qs]
  (apply (partial core/intersect? (element q))
         (map element qs)))

(defn tag
  "Return the HTML tag for the first element found with query `q`."
  [q]
  (core/tag (element q)))

(defn text
  "Return the text within the first element found with query `q`."
  [q]
  (core/text (element q)))

(defn value
  "Return the value of the HTML value attribute for the first element found with query `q`. The is identical to `(attribute q :value)`"
  [q]
  (core/value (element q)))

(defn visible?
  "Return true if the first element found with query `q` is visible on the current page in the browser."
  [q]
  (core/visible? (element q)))

(defn xpath
  "Return an absolute XPath path for the first element found with query `q`. NOTE: This function relies on executing JavaScript in the browser, and is therefore not as dependable as other functions."
  [q]
  (core/xpath (element q)))

(defn deselect
  "If the first form element found with query `q` is selected, click the element to deselect it. Otherwise, do nothing and just return the element found."
  [q]
  (core/deselect (element q)))

(defn enabled?
  "Return true if the first form element found with query `q` is enabled (not disabled)."
  [q] (core/enabled? (element q)))

(defn input-text
  "Type the string `s` into the first form element found with query `q`."
  [q s]
  (core/input-text (element q) s))

(defn submit
  "Submit the form that the first form element found with query `q` belongs to (this is equivalent to pressing ENTER in a text field while filling out a form)."
  [q]
  (core/submit (element q)))

(defn clear
  "Clear the contents (the HTML value attribute) of the first form element found with query `q`."
  [q]
  (core/clear (element q)))

(defn select
  "If the first form element found with query `q` is not selected, click the element to select it. Otherwise, do nothing and just return the element found."
  [q]
  (core/select (element q)))

(defn selected?
  "Return true if the first element found with the query `q` is selected (works for radio buttons, checkboxes, and option tags within select lists)."
  [q] (core/selected? (element q)))

(defn send-keys
  "Type the string `s` into the first form element found with query `q`."
  [q s]
  (core/send-keys (element q) s))

(defn toggle
  "Toggle is a synonym for click. Click the first element found with query `q`."
  [q]
  (core/toggle (element q)))

(defn options
  "Return all option elements within the first select list found with query `q`."
  [q]
  (core/all-options (element q)))

(defn selected-options
  "Return all selected option elements within the first select list found with query `q`."
  [q]
  (core/all-selected-options (element q)))

(defn deselect-option
  "Deselect the option element matching `attr-val` within the first select list found with query `q`. 

   The `attr-val` can contain `:index`, `:value`, or `:text` keys to find the target option element. Index is the zero-based order of the option element in the list, value is the value of the HTML value attribute, and text is the visible text of the option element on the page."
  [q attr-val]
  (core/deselect-option (element q) attr-val))

(defn deselect-all
  "Deselect all options within the first select list found with query `q`."
  [q] (core/deselect-all (element q)))

(defn deselect-by-index
  "Deselect the option element at index `idx` (zero-based) within the first select list found with query `q`."
  [q idx] (core/deselect-by-index (element q) idx))

(defn deselect-by-text
  "Deselect the option element with visible text `text` within the first select list found with query `q`."
  [q text]
  (core/deselect-by-text (element q) text))

(defn deselect-by-value
  "Deselect the option element with `value` within the first select list found with query `q`."
  [q value]
  (core/deselect-by-value (element q) value))

(defn multiple?
  "Return true if the first select list found with query `q` allows multiple selections."
  [q] (core/multiple? (element q)))

(defn select-option
  "Select the option element matching `attr-val` within the first select list found with query `q`. 

   The `attr-val` can contain `:index`, `:value`, or `:text` keys to find the target option element. Index is the zero-based order of the option element in the list, value is the value of the HTML value attribute, and text is the visible text of the option element on the page."
  [q attr-val]
  (core/select-option (element q) attr-val))

(defn select-all
  "Select all options within the first select list found with query `q`."
  [q]
  (core/select-all (element q)))

(defn select-by-index
  "Select the option element at index `idx` (zero-based) within the first select list found with query `q`."
  [q idx]
  (core/select-by-index (element q) idx))

(defn select-by-text
  "Select the option element with visible text `text` within the first select list found with query `q`."
  [q text]
  (core/select-by-text (element q) text))

(defn select-by-value
  "Deselect the option element with `value` within the first select list found with query `q`."
  [q value]
  (core/select-by-value (element q) value))

;; Helpers
(defn- quick-fill*
  ([k v] (quick-fill* k v false))
  ([k v submit?]
     ;; shortcuts:
     ;; v as string => text to input
     (let [q k
           action (if (string? v)
                    #(input-text % v)
                    v)
           target-els (elements q)]
       (if submit?
         (doseq [el target-els]
           (action el))
         (map action target-els)))))

(defn quick-fill
  "A utility for filling out multiple fields in a form in one go. Returns all the affected elements (if you want a list of unique elements, pass the results through the `distinct` function in clojure.core).

   `query-action-maps`   - a seq of maps of queries to actions (queries find HTML elements, actions are fn's that act on them)

   Note that an \"action\" that is just a String will be interpreted as a call to `input-text` with that String for the target text field.

   Example usage:
   (quick-fill {\"#first_name\" \"Rich\"}
               {\"a.foo\" click})"
  [& query-action-maps]
  (flatten (map (fn [a-map]
                  (let [[k v] (first a-map)] (quick-fill* k v)))
                query-action-maps)))

(defn quick-fill-submit
  "A utility for filling out multiple fields in a form in one go. Always returns nil instead of the affected elements, since on submit all of the elements will be void.

   `query-action-maps`   - a seq of maps of queries to actions (queries find HTML elements, actions are fn's that act on them)

   Note that an \"action\" that is just a String will be interpreted as a call to `input-text` with that String for the target text field.

   Example usage:
   (quick-fill {\"#first_name\" \"Rich\"}
               {\"a.foo\" click})"
  [& query-action-maps]
  (doseq [entry query-action-maps
          [k v] entry]
    (quick-fill* k v true)))
