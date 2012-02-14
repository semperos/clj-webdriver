;; Make things "easier" with state and defaults
(ns clj-webdriver.easy
  (:require [clj-webdriver.core :as core]
            [clj-webdriver.options :as options])
  (:import clj_webdriver.element.Element))

(declare css-easy-fn)
(def ^{:dynamic true} *current-driver* (atom nil))
(def ^{:dynamic true} *current-easy-fn* (atom css-easy-fn))

(defn set-driver!
  "Anti-functional use of clj-webdriver."
  [browser-spec]
  (reset! *current-driver* (core/new-driver browser-spec)))

(defn set-easy-fn!
  "Anti-functional higher-order function application."
  [a-fn]
  (reset! *current-easy-fn* a-fn))

(declare quit)
(defmacro with-driver
  "Equivalent to core's function with the same name, except no binding form needed. Browser is started at beginning of `body` and `(quit)` is called at the end."
  [browser-spec & body]
  `(binding [*current-driver* (atom (core/new-driver ~browser-spec))]
    (try
      ~@body
      (finally
        (quit)))))

(defn css-easy-fn
  "Given a `driver` and a CSS query `q`, return the first element found. If `q` is not a string, it's assumed to be an Element record and is returned unchanged."
  [q]
  (if (not (string? q))
    q
    (core/find-element @*current-driver* {:css q})))

(defn xpath-easy-fn
  "Given a `driver` and a XPath query `q`, return the first element found. If `q` is not a string, it's assumed to be an Element record and is returned unchanged."
  [q]
  (if (not (string? q))
    q
    (core/find-element @*current-driver* {:xpath q})))

;; Driver functions
(defn to [url] (core/to @*current-driver* url))
(defn back [] (core/back @*current-driver*))
(defn close [] (core/close @*current-driver*))
(defn current-url [] (core/current-url @*current-driver*))
(defn forward [] (core/forward @*current-driver*))
(defn get-url [url] (core/get-url @*current-driver* url))
(defn get-screenshot
  ([] (core/get-screenshot @*current-driver*))
  ([format] (core/get-screenshot @*current-driver* format))
  ([format destination] (core/get-screenshot @*current-driver* format destination)))
(defn page-source [] (core/page-source @*current-driver*))
(defn quit [] (core/quit @*current-driver*))
(defn refresh [] (core/refresh @*current-driver*))
(defn title [] (core/title @*current-driver*))
(defn window-handle [] (core/window-handle @*current-driver*))
(defn window-handles [] (core/window-handles @*current-driver*))
(defn other-window-handles [] (core/other-window-handles @*current-driver*))
(defn switch-to-frame [frame] (core/switch-to-frame @*current-driver* frame))
(defn switch-to-window [handle] (core/switch-to-window @*current-driver* handle))
(defn switch-to-other-window [] (core/switch-to-other-window @*current-driver*))
(defn switch-to-default [] (core/switch-to-default @*current-driver*))
(defn switch-to-active [] (core/switch-to-active @*current-driver*))
(defn add-cookie [cookie] (options/add-cookie @*current-driver* cookie))
(defn delete-cookie-named [cookie-name] (options/delete-cookie-named @*current-driver* cookie-name))
(defn delete-cookie [cookie] (options/delete-cookie @*current-driver* cookie))
(defn delete-all-cookies [] (options/delete-all-cookies @*current-driver*))
(defn cookies [] (options/cookies @*current-driver*))
(defn cookie-named [cookie-name] (options/cookie-named @*current-driver* cookie-name))
(defn find-element-by [by] (core/find-element-by @*current-driver* by))
(defn find-elements-by [by] (core/find-elements-by @*current-driver* by))
(defn find-elements-by-regex-alone [tag attr-val] (core/find-elements-by-regex-alone @*current-driver* tag attr-val))
(defn find-elements-by-regex [tag attr-val] (core/find-elements-by-regex @*current-driver* tag attr-val))
(defn find-windows [attr-val] (core/find-windows @*current-driver* attr-val))
(defn find-window [attr-val] (core/find-window @*current-driver* attr-val))
(defn find-semantic-buttons [attr-val] (core/find-semantic-buttons @*current-driver* attr-val))
(defn find-semantic-buttons-by-regex [attr-val] (core/find-semantic-buttons-by-regex @*current-driver* attr-val))
(defn find-checkables-by-text [attr-val] (core/find-checkables-by-text @*current-driver* attr-val))
(defn find-table-cell [table coords] "Need to write custom here.")
(defn find-table-row [table row] "Need to write custom here.")
(defn find-by-hierarchy [hierarchy-vec] (core/find-by-hierarchy @*current-driver* hierarchy-vec))
(defn find-elements [attr-val] (core/find-elements @*current-driver* attr-val))
(defn find-element [attr-val] (core/find-element @*current-driver* attr-val))

;; Element functions
;;
;; Unlike their counterparts in core, you don't need to do a `(find-element ...)`
;; with these; just pass in a CSS query followed by other necessary parameters
;; and the first element that matches the query will be used automatically.
;;
;; If a CSS query string is not passed in, it's assumed you're trying to use these
;; functions like their core counterparts, in which case each function will default
;; back to core functionality (expecting that you're passing in an Element record)

(defn attribute [q attr] (core/attribute (@*current-easy-fn* q) attr))
(defn click [q] (core/click (@*current-easy-fn* q)))
(defn displayed? [q] (core/displayed? (@*current-easy-fn* q)))
(defn drag-and-drop-by [q x y] (core/drag-and-drop-by (@*current-easy-fn* q) x y))
(defn drag-and-drop-on [qa qb] (core/drag-and-drop-on (@*current-easy-fn* qa) (@*current-easy-fn* qb)))
(defn exists? [q] (core/exists? (@*current-easy-fn* q)))
(defn flash [q] (core/flash (@*current-easy-fn* q)))
(defn focus [q] (core/focus (@*current-easy-fn* q)))
(defn html [q] (core/html (@*current-easy-fn* q)))
(defn location [q] (core/location (@*current-easy-fn* q)))
(defn location-once-visible [q] (core/location-once-visible (@*current-easy-fn* q)))
(defn present? [q] (core/present? (@*current-easy-fn* q)))
(defn size [q] (core/size (@*current-easy-fn* q)))
(defn rectangle [q] (core/rectangle (@*current-easy-fn* q)))
(defn intersect? [q & qs] "Needs custom work.")
(defn tag [q] (core/tag (@*current-easy-fn* q)))
(defn text [q] (core/text (@*current-easy-fn* q)))
(defn value [q] (core/value (@*current-easy-fn* q)))
(defn visible? [q] (core/visible? (@*current-easy-fn* q)))
(defn xpath [q] (core/xpath (@*current-easy-fn* q)))
(defn deselect [q] (core/deselect (@*current-easy-fn* q)))
(defn enabled? [q] (core/enabled? (@*current-easy-fn* q)))
(defn input-text [q s] (core/input-text (@*current-easy-fn* q) s))
(defn submit [q] (core/submit (@*current-easy-fn* q)))
(defn clear [q] (core/clear (@*current-easy-fn* q)))
(defn select [q] (core/select (@*current-easy-fn* q)))
(defn selected? [q] (core/selected? (@*current-easy-fn* q)))
(defn send-keys [q s] (core/send-keys (@*current-easy-fn* q) s))
(defn toggle [q] (core/toggle (@*current-easy-fn* q)))
(defn all-options [q] (core/all-options (@*current-easy-fn* q)))
(defn all-selected-options [q] (core/all-selected-options (@*current-easy-fn* q)))
(defn deselect-option [q attr-val] (core/deselect-option (@*current-easy-fn* q) attr-val))
(defn deselect-all [q] (core/deselect-all (@*current-easy-fn* q)))
(defn deselect-by-index [q idx] (core/deselect-by-index (@*current-easy-fn* q) idx))
(defn deselect-by-text [q text] (core/deselect-by-text (@*current-easy-fn* q) text))
(defn deselect-by-value [q value] (core/deselect-by-value (@*current-easy-fn* q) value))
(defn first-selected-option [q] (core/first-selected-option (@*current-easy-fn* q)))
(defn multiple? [q] (core/multiple? (@*current-easy-fn* q)))
(defn select-option [q attr-val] (core/select-option (@*current-easy-fn* q) attr-val))
(defn select-all [q] (core/select-all (@*current-easy-fn* q)))
(defn select-by-index [q idx] (core/select-by-index (@*current-easy-fn* q) idx))
(defn select-by-text [q text] (core/select-by-text (@*current-easy-fn* q) text))
(defn select-by-value [q value] (core/select-by-value (@*current-easy-fn* q) value))