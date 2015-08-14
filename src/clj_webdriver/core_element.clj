;; ## Core Element-related Functions ##
;;
;; This namespace implements the following protocols:
;;
;;  * IElement
;;  * IFormElement
;;  * ISelectElement
(in-ns 'clj-webdriver.core)

(defn rectangle
  [webelement]
  (let [loc (location element)
        el-size (size element)]
    (java.awt.Rectangle. (:x loc)
                         (:y loc)
                         (:width el-size)
                         (:height el-size))))

(extend-type WebElement

  ;; Element action basics
  IElement
  (attribute [webelement attr]
    (if (= attr :text)
      (text element)
      (let [attr (name attr)
            boolean-attrs ["async", "autofocus", "autoplay", "checked", "compact", "complete",
                           "controls", "declare", "defaultchecked", "defaultselected", "defer",
                           "disabled", "draggable", "ended", "formnovalidate", "hidden",
                           "indeterminate", "iscontenteditable", "ismap", "itemscope", "loop",
                           "multiple", "muted", "nohref", "noresize", "noshade", "novalidate",
                           "nowrap", "open", "paused", "pubdate", "readonly", "required",
                           "reversed", "scoped", "seamless", "seeking", "selected", "spellcheck",
                           "truespeed", "willvalidate"]
            webdriver-result (.getAttribute webelement (name attr))]
        (if (some #{attr} boolean-attrs)
          (when (= webdriver-result "true")
            attr)
          webdriver-result))))

  (click [webelement]
    (.click webelement)
    nil)

  (css-value [webelement property]
    (.getCssValue webelement property))

  (displayed? [webelement]
    (.isDisplayed webelement))

  (exists? [webelement]
    (not (nil? webelement)))

  (flash [webelement]
    (let [original-color (if (css-value element "background-color")
                           (css-value element "background-color")
                           "transparent")
          orig-colors (repeat original-color)
          change-colors (interleave (repeat "red") (repeat "blue"))]
      (doseq [flash-color (take 12 (interleave change-colors orig-colors))]
        (execute-script* (.getWrappedDriver webelement)
                         (str "arguments[0].style.backgroundColor = '"
                              flash-color "'")
                         webelement)
        (Thread/sleep 80)))
    element)

  (focus [webelement]
    (execute-script*
     (.getWrappedDriver webelement) "return arguments[0].focus()" webelement)
    element)

  (html [webelement]
    (browserbot (.getWrappedDriver webelement) "getOuterHTML" webelement))

  (location [webelement]
    (let [loc (.getLocation webelement)
          x   (.x loc)
          y   (.y loc)]
      {:x x, :y y}))

  (location-once-visible [webelement]
    (let [loc (.getLocationOnScreenOnceScrolledIntoView webelement)
          x   (.x loc)
          y   (.y loc)]
      {:x x, :y y}))

  (present? [webelement]
    (and (exists? element) (visible? element)))

  (size [webelement]
    (let [size-obj (.getSize webelement)
          w (.width size-obj)
          h (.height size-obj)]
      {:width w, :height h}))

  (intersects? [webelement-a element-b]
    (let [rect-a (rectangle element-a)
          rect-b (rectangle element-b)]
      (.intersects rect-a rect-b)))

  (tag [webelement]
    (.getTagName webelement))

  (text [webelement]
    (.getText webelement))

  (value [webelement]
    (.getAttribute webelement "value"))

  (visible? [webelement]
    (.isDisplayed webelement))

  (xpath [webelement]
    (browserbot (.getWrappedDriver webelement) "getXPath" webelement []))


  IFormElement
  (deselect [webelement]
    (if (.isSelected webelement)
      (toggle webelement)
      element))

  (enabled? [webelement]
    (.isEnabled webelement))

  (input-text [webelement s]
    (.sendKeys webelement (into-array CharSequence (list s)))
    element)

  (submit [webelement]
    (.submit webelement)
    nil)

  (clear [webelement]
    (.clear webelement)
    element)

  (select [webelement]
    (if-not (.isSelected webelement)
      (.click webelement)
      element))

  (selected? [webelement]
    (.isSelected webelement))

  (send-keys [webelement s]
    (.sendKeys webelement (into-array CharSequence (list s)))
    element)

  (toggle [webelement]
    (.click webelement)
    element)


  ISelectElement
  (all-options [webelement]
    (let [select-list (Select. webelement)]
      (lazy-seq (init-elements (.getOptions select-list)))))

  (all-selected-options [webelement]
    (let [select-list (Select. webelement)]
      (lazy-seq (init-elements (.getAllSelectedOptions select-list)))))

  (deselect-option [webelement attr-val]
    {:pre [(or (= (first (keys attr-val)) :index)
               (= (first (keys attr-val)) :value)
               (= (first (keys attr-val)) :text))]}
    (case (first (keys attr-val))
      :index (deselect-by-index element (:index attr-val))
      :value (deselect-by-value element (:value attr-val))
      :text  (deselect-by-text element (:text attr-val))))

  (deselect-all [webelement]
    (let [cnt-range (->> (all-options element)
                         count
                         (range 0))]
      (doseq [idx cnt-range]
        (deselect-by-index element idx))
      element))

  (deselect-by-index [webelement idx]
    (let [select-list (Select. webelement)]
      (.deselectByIndex select-list idx)
      element))

  (deselect-by-text [webelement text]
    (let [select-list (Select. webelement)]
      (.deselectByVisibleText select-list text)
      element))

  (deselect-by-value [webelement value]
    (let [select-list (Select. webelement)]
      (.deselectByValue select-list value)
      element))

  (first-selected-option [webelement]
    (let [select-list (Select. webelement)]
      (init-element (.getFirstSelectedOption select-list))))

  (multiple? [webelement]
    (let [value (attribute element "multiple")]
      (or (= value "true")
          (= value "multiple"))))

  (select-option [webelement attr-val]
    {:pre [(or (= (first (keys attr-val)) :index)
               (= (first (keys attr-val)) :value)
               (= (first (keys attr-val)) :text))]}
    (case (first (keys attr-val))
      :index (select-by-index element (:index attr-val))
      :value (select-by-value element (:value attr-val))
      :text  (select-by-text element (:text attr-val))))

  (select-all [webelement]
    (let [cnt-range (->> (all-options element)
                         count
                         (range 0))]
      (doseq [idx cnt-range]
        (select-by-index element idx))
      element))

  (select-by-index [webelement idx]
    (let [select-list (Select. webelement)]
      (.selectByIndex select-list idx)
      element))

  (select-by-text [webelement text]
    (let [select-list (Select. webelement)]
      (.selectByVisibleText select-list text)
      element))

  (select-by-value [webelement value]
    (let [select-list (Select. webelement)]
      (.selectByValue select-list value)
      element))

  IFind
  (find-element-by [webelement by]
    (let [by (if (map? by)
               (by-query (build-query by :local))
               by)]
      (init-element (.findElement webelement by))))

  (find-elements-by [webelement by]
    (let [by (if (map? by)
               (by-query (build-query by :local))
               by)
          els (.findElements webelement by)]
      (if (seq els)
        (map init-element els)
        (map init-element [nil]))))

  (find-element [webelement by]
    (find-element-by element by))

  (find-elements [webelement by]
    (find-elements-by element by)))

;;
;; Extend Element-related protocols to `nil`,
;; so our nil-handling is clear.
;;

(extend-protocol IElement
  nil
  (attribute   [n attr] (throw-nse))

  (click       [n] (throw-nse))

  (css-value   [n property] (throw-nse))

  (displayed?  [n] (throw-nse))

  (exists?     [n] false)

  (flash       [n] (throw-nse))

  (focus [n] (throw-nse))

  (html [n] (throw-nse))

  (location [n] (throw-nse))

  (location-once-visible [n] (throw-nse))

  (present? [n] (throw-nse))

  (size [n] (throw-nse))

  (rectangle [n] (throw-nse))

  (intersects? [n m-b] (throw-nse))

  (tag [n] (throw-nse))

  (text [n] (throw-nse))

  (value [n] (throw-nse))

  (visible? [n] (throw-nse))

  (xpath [n] (throw-nse)))

(extend-protocol IFormElement
  nil
  (deselect [n] (throw-nse))

  (enabled? [n] (throw-nse))

  (input-text [n s] (throw-nse))

  (submit [n] (throw-nse))

  (clear [n] (throw-nse))

  (select [n] (throw-nse))

  (selected? [n] (throw-nse))

  (send-keys [n s] (throw-nse))

  (toggle [n] (throw-nse)))

(extend-protocol ISelectElement
  nil
  (all-options [n] (throw-nse))

  (all-selected-options [n] (throw-nse))

  (deselect-option [n attr-val] (throw-nse))

  (deselect-all [n] (throw-nse))

  (deselect-by-index [n idx] (throw-nse))

  (deselect-by-text [n text] (throw-nse))

  (deselect-by-value [n value] (throw-nse))

  (first-selected-option [n] (throw-nse))

  (multiple? [n] (throw-nse))

  (select-option [n attr-val] (throw-nse))

  (select-all [n] (throw-nse))

  (select-by-index [n idx] (throw-nse))

  (select-by-text [n text] (throw-nse))

  (select-by-value [n value] (throw-nse)))

(extend-protocol IFind
  nil
  (find-element-by [n by] (throw-nse))

  (find-elements-by [n by] (throw-nse))

  (find-element [n by] (throw-nse))

  (find-elements [n by] (throw-nse)))
