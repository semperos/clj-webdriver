(in-ns 'clj-webdriver.core)

(extend-type Element

  ;; Element action basics
  IElement
  (attribute [element attr]
    (.getAttribute (:webelement element) (name attr)))
  
  (click [element]
    (.click (:webelement element))
    (cache/set-status :check)
    nil)

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
    (let [original-color (if (.getCssValue (:webelement element) "background-color")
                           (.getCssValue (:webelement element) "background-color")
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
  
  (tag-name [element]
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
  
  (send-keys [element text]
    :foo)
  
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
    element))