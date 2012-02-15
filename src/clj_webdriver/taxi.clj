;; The faster way to use clj-webdriver: take a taxi
(ns clj-webdriver.taxi
  (:require [clj-webdriver.core :as core]
            [clj-webdriver.options :as options])
  (:import clj_webdriver.element.Element))

(declare css-finder-fn)
(def ^:dynamic *driver*)
(def ^:dynamic *finder-fn* css-finder-fn)

(defn- set-driver*
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

(defn set-finder-fn!
  [finder-fn]
  (alter-var-root (var *finder-fn*)
                  (constantly finder-fn)
                  (when (thread-bound? (var *finder-fn*))
                    (set! *finder-fn* finder-fn))))

(declare quit)
(defmacro with-driver
  "Equivalent to core's function with the same name, except no binding form needed. Browser is started at beginning of `body` and `(quit)` is called at the end."
  [browser-spec & body]
  `(binding [*driver* (atom (core/new-driver ~browser-spec))]
    (try
      ~@body
      (finally
        (quit)))))

(defmacro with-driver-fn
  "Like `with-driver`, but pass in a custom finder-fn as well."
  [browser-spec finder-fn & body]
  `(binding [*driver* (core/new-driver ~browser-spec)
             *finder-fn* ~finder-fn]
    (try
      ~@body
      (finally
        (quit)))))

(defn css-finder-fn
  "Given a CSS query `q`, return the first element found. If `q` is not a string, it's assumed to be an Element record and is returned unchanged."
  [q]
  (if (not (string? q))
    q
    (core/find-element *driver* {:css q})))

(set-finder-fn! css-finder-fn)

(defn xpath-finder-fn
  "Given a `driver` and a XPath query `q`, return the first element found. If `q` is not a string, it's assumed to be an Element record and is returned unchanged."
  [q]
  (if (not (string? q))
    q
    (core/find-element *driver* {:xpath q})))

;; Driver functions
(defn to [url] (core/to *driver* url))
(defn back [] (core/back *driver*))
(defn close [] (core/close *driver*))
(defn current-url [] (core/current-url *driver*))
(defn forward [] (core/forward *driver*))
(defn get-url [url] (core/get-url *driver* url))
(defn get-screenshot
  ([] (core/get-screenshot *driver*))
  ([format] (core/get-screenshot *driver* format))
  ([format destination] (core/get-screenshot *driver* format destination)))
(defn page-source [] (core/page-source *driver*))
(defn quit [] (core/quit *driver*))
(defn refresh [] (core/refresh *driver*))
(defn title [] (core/title *driver*))
(defn window-handle [] (core/window-handle *driver*))
(defn window-handles [] (core/window-handles *driver*))
(defn other-window-handles [] (core/other-window-handles *driver*))
(defn switch-to-frame [frame] (core/switch-to-frame *driver* frame))
(defn switch-to-window [handle] (core/switch-to-window *driver* handle))
(defn switch-to-other-window [] (core/switch-to-other-window *driver*))
(defn switch-to-default [] (core/switch-to-default *driver*))
(defn switch-to-active [] (core/switch-to-active *driver*))
(defn add-cookie [cookie] (options/add-cookie *driver* cookie))
(defn delete-cookie-named [cookie-name] (options/delete-cookie-named *driver* cookie-name))
(defn delete-cookie [cookie] (options/delete-cookie *driver* cookie))
(defn delete-all-cookies [] (options/delete-all-cookies *driver*))
(defn cookies [] (options/cookies *driver*))
(defn cookie-named [cookie-name] (options/cookie-named *driver* cookie-name))
(defn find-element-by [by] (core/find-element-by *driver* by))
(defn find-elements-by [by] (core/find-elements-by *driver* by))
(defn find-elements-by-regex-alone [tag attr-val] (core/find-elements-by-regex-alone *driver* tag attr-val))
(defn find-elements-by-regex [tag attr-val] (core/find-elements-by-regex *driver* tag attr-val))
(defn find-windows [attr-val] (core/find-windows *driver* attr-val))
(defn find-window [attr-val] (core/find-window *driver* attr-val))
(defn find-semantic-buttons [attr-val] (core/find-semantic-buttons *driver* attr-val))
(defn find-semantic-buttons-by-regex [attr-val] (core/find-semantic-buttons-by-regex *driver* attr-val))
(defn find-checkables-by-text [attr-val] (core/find-checkables-by-text *driver* attr-val))
(defn find-table-cell [table coords] "Need to write custom here.")
(defn find-table-row [table row] "Need to write custom here.")
(defn find-by-hierarchy [hierarchy-vec] (core/find-by-hierarchy *driver* hierarchy-vec))
(defn find-elements [attr-val] (core/find-elements *driver* attr-val))
(defn find-element [attr-val] (core/find-element *driver* attr-val))

;; Element functions
;;
;; Unlike their counterparts in core, you don't need to do a `(find-element ...)`
;; with these; just pass in a CSS query followed by other necessary parameters
;; and the first element that matches the query will be used automatically.
;;
;; If a CSS query string is not passed in, it's assumed you're trying to use these
;; functions like their core counterparts, in which case each function will default
;; back to core functionality (expecting that you're passing in an Element record)

(defn attribute [q attr] (core/attribute (*finder-fn* q) attr))
(defn click [q] (core/click (*finder-fn* q)))
(defn displayed? [q] (core/displayed? (*finder-fn* q)))
(defn drag-and-drop-by [q x y] (core/drag-and-drop-by (*finder-fn* q) x y))
(defn drag-and-drop-on [qa qb] (core/drag-and-drop-on (*finder-fn* qa) (*finder-fn* qb)))
(defn exists? [q] (core/exists? (*finder-fn* q)))
(defn flash [q] (core/flash (*finder-fn* q)))
(defn focus [q] (core/focus (*finder-fn* q)))
(defn html [q] (core/html (*finder-fn* q)))
(defn location [q] (core/location (*finder-fn* q)))
(defn location-once-visible [q] (core/location-once-visible (*finder-fn* q)))
(defn present? [q] (core/present? (*finder-fn* q)))
(defn size [q] (core/size (*finder-fn* q)))
(defn rectangle [q] (core/rectangle (*finder-fn* q)))
(defn intersect? [q & qs] "Needs custom work.")
(defn tag [q] (core/tag (*finder-fn* q)))
(defn text [q] (core/text (*finder-fn* q)))
(defn value [q] (core/value (*finder-fn* q)))
(defn visible? [q] (core/visible? (*finder-fn* q)))
(defn xpath [q] (core/xpath (*finder-fn* q)))
(defn deselect [q] (core/deselect (*finder-fn* q)))
(defn enabled? [q] (core/enabled? (*finder-fn* q)))
(defn input-text [q s] (core/input-text (*finder-fn* q) s))
(defn submit [q] (core/submit (*finder-fn* q)))
(defn clear [q] (core/clear (*finder-fn* q)))
(defn select [q] (core/select (*finder-fn* q)))
(defn selected? [q] (core/selected? (*finder-fn* q)))
(defn send-keys [q s] (core/send-keys (*finder-fn* q) s))
(defn toggle [q] (core/toggle (*finder-fn* q)))
(defn all-options [q] (core/all-options (*finder-fn* q)))
(defn all-selected-options [q] (core/all-selected-options (*finder-fn* q)))
(defn deselect-option [q attr-val] (core/deselect-option (*finder-fn* q) attr-val))
(defn deselect-all [q] (core/deselect-all (*finder-fn* q)))
(defn deselect-by-index [q idx] (core/deselect-by-index (*finder-fn* q) idx))
(defn deselect-by-text [q text] (core/deselect-by-text (*finder-fn* q) text))
(defn deselect-by-value [q value] (core/deselect-by-value (*finder-fn* q) value))
(defn first-selected-option [q] (core/first-selected-option (*finder-fn* q)))
(defn multiple? [q] (core/multiple? (*finder-fn* q)))
(defn select-option [q attr-val] (core/select-option (*finder-fn* q) attr-val))
(defn select-all [q] (core/select-all (*finder-fn* q)))
(defn select-by-index [q idx] (core/select-by-index (*finder-fn* q) idx))
(defn select-by-text [q text] (core/select-by-text (*finder-fn* q) text))
(defn select-by-value [q value] (core/select-by-value (*finder-fn* q) value))