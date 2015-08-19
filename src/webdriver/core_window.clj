(in-ns 'webdriver.core)

(comment "Getting a Window for a WebDriver"
         (let [manage (.manage wd)]
           (.window manage)))

(extend-protocol IWindow
  WebDriver
  (maximize [wd]
    (let [wnd (window* wd)]
      (.maximize wnd)
      wd))

  (position [wd]
    (let [wnd (window* wd)
          pnt (.getPosition wnd)]
      {:x (.getX pnt) :y (.getY pnt)}))

  (reposition [wd {:keys [x y]}]
    (let [wnd (window* wd)
          pnt (.getPosition wnd)
          x (or x (.getX pnt))
          y (or y (.getY pnt))]
      (.setPosition wnd (Point. x y))
      wd))

  (resize [wd {:keys [width height]}]
    (let [^WebDriver$Window wnd (window* wd)
          dim (.getSize wnd)
          width (or width (.getWidth dim))
          height (or height (.getHeight dim))]
      (.setSize wnd (Dimension. width height))
      wd))

  (window-size [wd]
    (let [^WebDriver$Window wnd (window* wd)
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
