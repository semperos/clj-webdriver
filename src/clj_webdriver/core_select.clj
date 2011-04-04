(in-ns 'clj-webdriver.core)

(defn deselect-all
  "Clear all selected entries for select list described by `by`"
  [element]
  (let [select-list (Select. element)]
    (.deselectAll select-list)
    element))

(defn deselect-by-index
  "Deselect the option at index `idx` for the select list described by `by`. Indeces begin at 1"
  [element idx]
  (let [idx-human (dec idx)
        select-list (Select. element)]
    (.deselectByIndex select-list idx-human)
    element))

(defn deselect-by-value
  "Deselect all options with value `value` for the select list described by `by`"
  [element value]
  (let [select-list (Select. element)]
    (.deselectByValue select-list value)
    element))

(defn deselect-by-text
  "Deselect all options with visible text `text` for the select list described by `by`"
  [element text]
  (let [select-list (Select. element)]
    (.deselectByVisibleText select-list text)
    element))

(defn all-selected-options
  "Retrieve a seq of all selected options from the select list described by `by`"
  [element]
  (let [select-list (Select. element)]
    (lazy-seq (.getAllSelectedOptions select-list))))

(defn first-selected-option
  "Retrieve the first selected option (or the only one for single-select lists) from the select list described by `by`"
  [element]
  (let [select-list (Select. element)]
    (.getFirstSelectedOption select-list)))

(defn all-options
  "Retrieve all options in the select list described by `by`"
  [element]
  (let [select-list (Select. element)]
    (lazy-seq (.getOptions select-list))))

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
    (.selectByIndex select-list idx-human)
    element))

(defn select-by-value
  "Select all options with value `value` in the select list described by `by`"
  [element value]
  (let [select-list (Select. element)]
    (.selectByValue select-list value)
    element))

(defn select-by-text
  "Select all options with visible text `text` in the select list described by `by`"
  [element text]
  (let [select-list (Select. element)]
    (.selectByVisibleText select-list text)
    element))