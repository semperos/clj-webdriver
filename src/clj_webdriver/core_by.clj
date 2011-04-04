(in-ns 'clj-webdriver.core)

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
         (= :text attr)   (by-xpath (str "//"
                                         (name tag)
                                         "[text()"
                                         "='" value "']"))
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