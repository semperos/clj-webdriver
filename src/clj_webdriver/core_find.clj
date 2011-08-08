(in-ns 'clj-webdriver.core)

(declare find-them)
(declare find-it)

(defn find-element
  "Retrieve the element object of an element described by `by`"
  [driver by]
  (.findElement driver by))

(defn find-elements
  "Retrieve a seq of element objects described by `by`"
  [driver by]
  (lazy-seq (.findElements driver by)))

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
    [(nth (window-handles driver) (:index attr-val))] ; vector for consistency below
    (filter #(every? (fn [[k v]] (if (= java.util.regex.Pattern (class v))
                                   (re-find v (k %))
                                   (= (k %) v)))
                     attr-val) (window-handles driver))))

;; Possible TODO: Correct XPath's lack of support for text() with <button> buttons.
(defn find-semantic-buttons
  "Find HTML element that is either a `<button>` or an `<input>` of type submit, reset, image or button"
  [driver attr-val]
  (let [xpath-parts ["//input[@type='submit']"
                     "//input[@type='reset']"
                     "//input[@type='image']"
                     "//input[@type='button']"
                     "//button"]
        xpath-full (if (or (nil? attr-val) (empty? attr-val))
                     (interpose "|" xpath-parts)
                     (conj
                      (->> (repeat (str (build-xpath-attrs attr-val) "|"))
                           (interleave (drop-last xpath-parts))
                           vec)
                      (str "//button" (build-xpath-attrs attr-val))))]
    (->> (apply str xpath-full)
         by-xpath
         (find-elements driver))))

(defn find-semantic-buttons-by-regex
  "Semantic buttons are things that look or behave like buttons but do not necessarily consist of a `<button>` tag"
  [driver attr-val]
  (let [attr-vals-without-regex (into {}
                                        (remove
                                         #(let [[k v] %] (= java.util.regex.Pattern (class v)))
                                         attr-val))
        elements (find-semantic-buttons driver attr-vals-without-regex)]
    (filter-elements-by-regex elements attr-val)))

(defn find-checkables-by-text
  "Finding the 'text' of a radio or checkbox is complex. Handle it here."
  [driver attr-val]
  (if (contains-regex? attr-val)
    (throw (IllegalArgumentException.
            (str "Combining regular expressions and the 'text' attribute "
                 "for finding radio buttons and checkboxes "
                 "is not supported at this time.")))
    (let [text-kw (if (contains? attr-val :text)
                   :text
                   :label)
         other-attr-vals (dissoc attr-val text-kw)
         non-text-xpath (build-xpath :input other-attr-vals)
         text-xpath (str non-text-xpath "[contains(..,'" (text-kw attr-val) "')]")]
      (find-elements driver (by-xpath text-xpath)))))

;; Enhancements to find-it for tables

(defn find-table-cells
  "Given a WebDriver `driver` and a vector `attr-val`, find the correct"
  [driver attr-val]
  (let [attr-val-map (apply hash-map attr-val)
        table-xpath (build-xpath :table (:table attr-val-map))
        row-xpath (str "//tr[" (inc (:row attr-val-map)) "]")
        col-xpath (if (and (find-element driver (by-xpath (str table-xpath "//th")))
                           (zero? (:row attr-val-map)))
                    (str "/th[" (inc (:col attr-val-map)) "]")
                    (str "/td[" (inc (:col attr-val-map)) "]"))
        complete-xpath (str table-xpath row-xpath col-xpath)]
    (find-elements driver (by-xpath complete-xpath))))