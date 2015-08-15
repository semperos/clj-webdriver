(in-ns 'webdriver.core)

(comment "Getting a Window for a WebDriver"
         (let [^WebDriver webdriver (.webdriver driver)
               manage (.manage webdriver)]
           (.window manage)))

(extend-protocol IWindow
  Driver
  (maximize [driver]
    (let [wnd (window* driver)]
      (.maximize wnd)
      driver))

  (position [driver]
    (let [wnd (window* driver)
          pnt (.getPosition wnd)]
      {:x (.getX pnt) :y (.getY pnt)}))

  (reposition [driver {:keys [x y]}]
    (let [wnd (window* driver)
          pnt (.getPosition wnd)
          x (or x (.getX pnt))
          y (or y (.getY pnt))]
      (.setPosition wnd (Point. x y))
      driver))

  (resize [driver {:keys [width height]}]
    (let [^WebDriver$Window wnd (window* driver)
          dim (.getSize wnd)
          width (or width (.getWidth dim))
          height (or height (.getHeight dim))]
      (.setSize wnd (Dimension. width height))
      driver))

  (window-size [driver]
    (let [^WebDriver$Window wnd (window* driver)
          dim (.getSize wnd)]
      {:width (.getWidth dim) :height (.getHeight dim)}))

  WebDriver$Window
  (maximize [window]
    (.maximize window)
    window)

  (position [window]
    (let [pnt (.getPosition window)]
      {:x (.getX pnt) :y (.getY pnt)}))

  (reposition [window {:keys [x y]}]
    (let [pnt (.getPosition window)
          x (or x (.getX pnt))
          y (or y (.getY pnt))]
      (.setPosition window (Point. x y))
      window))

  (resize [window {:keys [width height]}]
    (let [dim (.getSize window)
          width (or width (.getWidth dim))
          height (or height (.getHeight dim))]
      (.setSize window (Dimension. width height))
      window))

  (window-size [window]
    (let [dim (.getSize window)]
      {:width (.getWidth dim) :height (.getHeight dim)})))
