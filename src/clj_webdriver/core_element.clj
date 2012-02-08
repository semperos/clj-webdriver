;; ## Core Element-related Functions ##
;;
;; This namespace implements the following protocols:
;;
;;  * IElement
;;  * IFormElement
;;  * ISelectElement
(in-ns 'clj-webdriver.core)

(extend-type Element

  ;; Element action basics
  IElement
  (attribute [element attr]
    (let [attr (name attr)
          boolean-attrs ["async", "autofocus", "autoplay", "checked", "compact", "complete",
                         "controls", "declare", "defaultchecked", "defaultselected", "defer",
                         "disabled", "draggable", "ended", "formnovalidate", "hidden",
                         "indeterminate", "iscontenteditable", "ismap", "itemscope", "loop",
                         "multiple", "muted", "nohref", "noresize", "noshade", "novalidate",
                         "nowrap", "open", "paused", "pubdate", "readonly", "required",
                         "reversed", "scoped", "seamless", "seeking", "selected", "spellcheck",
                         "truespeed", "willvalidate"]
          webdriver-result (.getAttribute (:webelement element) (name attr))]
      (if (some #{attr} boolean-attrs)
        (if (= webdriver-result "true")
          attr
          nil)
        webdriver-result)))
  
  (click [element]
    (.click (:webelement element))
    (cache/set-status :check)
    nil)

	(css-value [element property]
		(.getCssValue (:webelement element) property))

  (displayed? [element]
    (.isDisplayed (:webelement element)))

  (drag-and-drop-by [element x y]
    (.dragAndDropBy (:webelement element) x y)
    element)

  (drag-and-drop-on [element-a element-b]
    (.dragAndDropOn (:webelement element-a) (:webelement element-b))
    element-a)

  (exists? [element]
    (not (nil? (:webelement element))))

  (flash [element]
    (let [original-color (if (css-value element "background-color")
                           (css-value element "background-color")
                           "transparent")
          orig-colors (repeat original-color)
          change-colors (interleave (repeat "red") (repeat "blue"))]
      (doseq [flash-color (take 12 (interleave change-colors orig-colors))]
        (execute-script* (.getWrappedDriver (:webelement element))
                         (str "arguments[0].style.backgroundColor = '"
                              flash-color "'")
                         (:webelement element))
        (Thread/sleep 80)))
    element)

  (focus [element]
    (execute-script*
     (.getWrappedDriver (:webelement element)) "return arguments[0].focus()" (:webelement element)))

  (html [element]
    (browserbot (.getWrappedDriver (:webelement element)) "getOuterHTML" (:webelement element)))

  (location [element]
    (let [loc (.getLocation (:webelement element))
          x   (.x loc)
          y   (.y loc)]
      {:x x, :y y}))

  (location-once-visible [element]
    (let [loc (.getLocationOnScreenOnceScrolledIntoView (:webelement element))
          x   (.x loc)
          y   (.y loc)]
      {:x x, :y y}))

  (present? [element]
    (and (exists? element) (visible? element)))
  
  (tag [element]
    (.getTagName (:webelement element)))

  (text [element]
    (.getText (:webelement element)))
  
  (value [element]
    (.getAttribute (:webelement element) "value"))

  (visible? [element]
    (.isDisplayed (:webelement element)))

  (xpath [element]
    (browserbot (.getWrappedDriver (:webelement element)) "getXPath" (:webelement element) []))
  

  IFormElement
  (deselect [element]
    (if (.isSelected (:webelement element))
      (toggle (:webelement element))
      element))
  
  (enabled? [element]
    (.isEnabled (:webelement element)))
  
  (input-text [element s]
    (.sendKeys (:webelement element) (into-array CharSequence (list s)))
    element)
  
  (submit [element]
    (.submit (:webelement element))
    (cache/set-status :flush)
    nil)
  
  (clear [element]
    (.clear (:webelement element))
    element)
  
  (select [element]
    (.click (:webelement element))
    element)
  
  (selected? [element]
    (.isSelected (:webelement element)))

  (send-keys [element s]
    (.sendKeys (:webelement element) (into-array CharSequence (list s)))
    element)
  
  (toggle [element]
    (.click (:webelement element))
    element)


  ISelectElement
  ;; TODO: test coverage
  (all-options [element]
    (let [select-list (Select. (:webelement element))]
      (lazy-seq (init-elements (.getOptions select-list)))))

  ;; TODO: test coverage
  (all-selected-options [element]
    (let [select-list (Select. (:webelement element))]
      (lazy-seq (init-elements (.getAllSelectedOptions select-list)))))

  ;; TODO: test coverage
  (deselect-option [element attr-val]
    {:pre [(or (= (first (keys attr-val)) :index)
               (= (first (keys attr-val)) :value)
               (= (first (keys attr-val)) :text))]}
    (case (first (keys attr-val))
      :index (deselect-by-index element (:index attr-val))
      :value (deselect-by-value element (:value attr-val))
      :text  (deselect-by-text element (:text attr-val))))

  ;; TODO: test coverage
  (deselect-all [element]
    (let [cnt-range (->> (all-options element)
                         count
                         (range 0))]
      (doseq [idx cnt-range]
        (deselect-by-index element idx))
      element))

  ;; TODO: test coverage
  (deselect-by-index [element idx]
    (let [select-list (Select. (:webelement element))]
      (.deselectByIndex select-list idx)
      element))

  ;; TODO: test coverage
  (deselect-by-text [element text]
    (let [select-list (Select. (:webelement element))]
      (.deselectByVisibleText select-list text)
      element))

  ;; TODO: test coverage
  (deselect-by-value [element value]
    (let [select-list (Select. (:webelement element))]
      (.deselectByValue select-list value)
      element))

  ;; TODO: test coverage
  (first-selected-option [element]
    (let [select-list (Select. (:webelement element))]
      (init-element (.getFirstSelectedOption select-list))))

  ;; TODO: test coverage
  (multiple? [element]
    (let [value (attribute element "multiple")]
      (or (= value "true")
          (= value "multiple"))))

  ;; TODO: test coverage
  (select-option [element attr-val]
    {:pre [(or (= (first (keys attr-val)) :index)
               (= (first (keys attr-val)) :value)
               (= (first (keys attr-val)) :text))]}
    (case (first (keys attr-val))
      :index (select-by-index element (:index attr-val))
      :value (select-by-value element (:value attr-val))
      :text  (select-by-text element (:text attr-val))))

  ;; TODO: test coverage
  (select-all [element]
    (let [cnt-range (->> (all-options element)
                         count
                         (range 0))]
      (doseq [idx cnt-range]
        (select-by-index element idx))
      element))

  ;; TODO: test coverage
  (select-by-index [element idx]
    (let [select-list (Select. (:webelement element))]
      (.selectByIndex select-list idx)
      element))

  ;; TODO: test coverage
  (select-by-text [element text]
    (let [select-list (Select. (:webelement element))]
      (.selectByVisibleText select-list text)
      element))

  ;; TODO: test coverage
  (select-by-value [element value]
    (let [select-list (Select. (:webelement element))]
      (.selectByValue select-list value)
      element)))
