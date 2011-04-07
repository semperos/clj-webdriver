(in-ns 'clj-webdriver.core)

(declare deselect-by-index)
(declare all-options)
(defn deselect-all
  "Deselect all options for a given select list. Does not leverage WebDriver method because WebDriver's isMultiple method is faulty."
  [element]
  (let [cnt-range (->> (all-options element)
                       count
                       (range 0))]
    (doseq [idx cnt-range]
      (deselect-by-index element idx))
    element))

(defn deselect-by-index
  "Deselect the option at index `idx` for the select list described by `by`. Indeces begin at 0"
  [element idx]
  (let [select-list (Select. element)]
    (.deselectByIndex select-list idx)
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
  "Retrieve the first selected option (or the only one for single-select lists) from the given select list"
  [element]
  (let [select-list (Select. element)]
    (.getFirstSelectedOption select-list)))

(defn all-options
  "Retrieve all options from the given select list"
  [element]
  (let [select-list (Select. element)]
    (lazy-seq (.getOptions select-list))))

(defn multiple?
  "Return true if the given select list allows for multiple selections"
  [element]
  (let [value (attribute element "multiple")]
    (or (= value "true")
        (= value "multiple"))))

(declare select-by-index)
(defn select-all
  "Select all options for a given select list"
  [element]
  (let [cnt-range (->> (all-options element)
                       count
                       (range 0))]
    (doseq [idx cnt-range]
      (select-by-index element idx))
    element))

(defn select-by-index
  "Select an option by its index in the given select list. Indeces begin at 0."
  [element idx]
  (let [select-list (Select. element)]
    (.selectByIndex select-list idx)
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