;; ## Core Driver-related Functions ##

;; This namespace provides the implementations for the following
;; protocols:

;;  * IDriver
;;  * ITargetLocator
;;  * IAlert
;;  * IOptions
;;  * IFind
(in-ns 'clj-webdriver.core)

(declare find-element* find-elements*)

(extend-type Driver

  ;; Basic Functions
  IDriver
  (back [driver]
    (.back (.navigate (.webdriver driver)))
    driver)

  (close [driver]
    (let [handles (window-handles* (.webdriver driver))]
      (if (> (count handles) 1) ; get back to a window that is open before proceeding
        (let [this-handle (window-handle* (.webdriver driver))
              idx (.indexOf handles this-handle)]
          (cond
           (zero? idx)
           (do                       ; if first window, switch to next
             (.close (.webdriver driver))
             (switch-to-window driver (nth handles (inc idx))))

           :else
           (do                     ; otherwise, switch back one window
             (.close (.webdriver driver))
             (switch-to-window driver (nth handles (dec idx))))))
        (.close (.webdriver driver)))))

  (current-url [driver]
    (.getCurrentUrl (.webdriver driver)))

  (forward [driver]
    (.forward (.navigate (.webdriver driver)))
    driver)

  (get-url [driver url]
    (.get (.webdriver driver) url)
    driver)

  (get-screenshot
    ([driver] (get-screenshot driver :file))
    ([driver format] (get-screenshot driver format nil))
    ([driver format destination]
       {:pre [(or (= format :file)
                  (= format :base64)
                  (= format :bytes))]}
       (let [wd (.webdriver driver)
             output (case format
                      :file (.getScreenshotAs wd OutputType/FILE)
                      :base64 (.getScreenshotAs wd OutputType/BASE64)
                      :bytes (.getScreenshotAs wd OutputType/BYTES))]
         (if destination
           (do
             (io/copy output (io/file destination))
             (log/info "Screenshot written to destination")
             output)
           output))))

  (page-source [driver]
    (.getPageSource (.webdriver driver)))

  (quit [driver]
    (.quit (.webdriver driver)))

  (refresh [driver]
    (.refresh (.navigate (.webdriver driver)))
    driver)

  (title [driver]
    (.getTitle (.webdriver driver)))

  (to [driver url]
    (.to (.navigate (.webdriver driver)) url)
    driver)


  ;; Window and Frame Handling
  ITargetLocator
  ;; TODO (possible): multiple arities; only driver, return current window handle; driver and query, return matching window handle
  (window [driver]
    (win/init-window (.webdriver driver)
                     (.getWindowHandle (.webdriver driver))
                     (title driver)
                     (current-url driver)))

  (windows [driver]
    (let [current-handle (.getWindowHandle (.webdriver driver))
          all-handles (lazy-seq (.getWindowHandles (.webdriver driver)))
          handle-records (for [handle all-handles]
                           (let [b (switch-to-window driver handle)]
                             (win/init-window (.webdriver driver)
                                              handle
                                              (title b)
                                              (current-url b))))]
      (switch-to-window driver current-handle)
      handle-records))

  (other-windows [driver]
    (remove #(= (:handle %) (:handle (window driver)))
            (doall (windows driver))))

  (switch-to-frame [driver frame]
    (.frame (.switchTo (.webdriver driver)) frame)
    driver)

  (switch-to-window [driver window]
    (cond
     (string? window)
     (do
       (.window (.switchTo (.webdriver driver)) window)
       driver)

     (win/window? window)
     (do
       (.window (.switchTo (:driver window)) (:handle window))
       driver)

     (number? window)
     (do
       (switch-to-window driver (nth (windows driver) window))
       driver)

     (nil? window)
     (throw (RuntimeException. "No window can be found"))

     :else
     (do
       (.window (.switchTo (.webdriver driver)) window)
       driver)))

  (switch-to-other-window [driver]
    (if (not= (count (windows driver)) 2)
      (throw (RuntimeException.
              (str "You may only use this function when two and only two "
                   "browser windows are open.")))
      (switch-to-window driver (first (other-windows driver)))))

  (switch-to-default [driver]
    (.defaultContent (.switchTo (.webdriver driver))))

  (switch-to-active [driver]
    (.activeElement (.switchTo (.webdriver driver))))


  ;; Options Interface (cookies)
  IOptions
  (add-cookie [driver cookie-spec]
    (.addCookie (.manage (.webdriver driver)) (:cookie (init-cookie cookie-spec)))
    driver)

  (delete-cookie-named [driver cookie-name]
    (.deleteCookieNamed (.manage (.webdriver driver)) cookie-name)
    driver)

  (delete-cookie [driver cookie-spec]
    (.deleteCookie (.manage (.webdriver driver)) (:cookie (init-cookie cookie-spec)))
    driver)

  (delete-all-cookies [driver]
    (.deleteAllCookies (.manage (.webdriver driver)))
    driver)

  (cookies [driver]
    (set (map #(init-cookie {:cookie %})
                   (.getCookies (.manage (.webdriver driver))))))

  (cookie-named [driver cookie-name]
    (let [cookie-obj (.getCookieNamed (.manage (.webdriver driver)) cookie-name)]
      (init-cookie {:cookie cookie-obj})))

  ;; Alert dialogs
  IAlert
  (accept [driver]
    (-> (.webdriver driver) .switchTo .alert .accept))

  (alert-obj [driver]
    (-> (.webdriver driver) .switchTo .alert))

  (alert-text [driver]
    (-> (.webdriver driver) .switchTo .alert .getText))

  ;; (authenticate-using [driver username password]
  ;;   (let [creds (UserAndPassword. username password)]
  ;;     (-> (.webdriver driver) .switchTo .alert (.authenticateUsing creds))))

  (dismiss [driver]
    (-> (.webdriver driver) .switchTo .alert .dismiss))

  ;; Find Functions
  IFind
  (find-element-by [driver by-value]
    (let [by-value (if (map? by-value)
                     (by-query (build-query by-value))
                     by-value)]
      (.findElement (.webdriver driver) by-value)))

  (find-elements-by [driver by-value]
    (let [by-value (if (map? by-value)
               (by-query (build-query by-value))
               by-value)]
      (.findElements (.webdriver driver) by-value)))

  (find-windows [driver attr-val]
    (if (contains? attr-val :index)
      [(nth (windows driver) (:index attr-val))] ; vector for consistency below
      (filter #(every? (fn [[k v]] (if (= java.util.regex.Pattern (class v))
                                    (re-find v (k %))
                                    (= (k %) v)))
                       attr-val) (windows driver))))

  (find-window [driver attr-val]
    (first (find-windows driver attr-val)))

  (find-table-cell [driver table coords]
    (when (not= (count coords) 2)
      (throw (IllegalArgumentException.
              (str "The `coordinates` parameter must be a seq with two items."))))
    (let [[row col] coords
          row-css (str "tr:nth-child(" (inc row) ")")
          col-css (if (and (find-element-by table (by-tag "th"))
                             (zero? row))
                      (str "th:nth-child(" (inc col) ")")
                      (str "td:nth-child(" (inc col) ")"))
          complete-css (str row-css " " col-css)]
      (find-element-by table (by-query {:css complete-css}))))

  (find-table-row [driver table row]
    (let [row-css (str  "tr:nth-child(" (inc row) ")")
          complete-css (if (and (find-element-by table (by-tag "th"))
                                  (zero? row))
                           (str row-css " " "th")
                           (str row-css " " "td"))]
      ;; WebElement, not Driver, version of protocol
      (find-elements-by table (by-query {:css complete-css}))))

  ;; TODO: reconsider find-table-col with CSS support

  (find-by-hierarchy [driver hierarchy-vec]
    (find-elements driver {:xpath (build-query hierarchy-vec)}))

  (find-elements
    ([driver attr-val]
     (find-elements* driver attr-val)))

  (find-element
    ([driver attr-val]
     (find-element* driver attr-val)))

  IActions

  (click-and-hold
    ([driver]
       (let [act (Actions. (.webdriver driver))]
         (.perform (.clickAndHold act))))
    ([driver webelement]
       (let [act (Actions. (.webdriver driver))]
         (.perform (.clickAndHold act webelement)))))

  (double-click
    ([driver]
       (let [act (Actions. (.webdriver driver))]
         (.perform (.doubleClick act))))
    ([driver webelement]
       (let [act (Actions. (.webdriver driver))]
         (.perform (.doubleClick act webelement)))))

  (drag-and-drop
    [driver webelement-a webelement-b]
    (cond
     (nil? webelement-a) (throw-nse "The first element does not exist.")
     (nil? webelement-b) (throw-nse "The second element does not exist.")
     :else (let [act (Actions. (.webdriver driver))]
             (.perform (.dragAndDrop act
                                     webelement-a
                                     webelement-b)))))

  (drag-and-drop-by
    [driver webelement x-y-map]
    (if (nil? webelement)
      (throw-nse)
      (let [act (Actions. (.webdriver driver))
            {:keys [x y] :or {x 0 y 0}} x-y-map]
        (.perform
         (.dragAndDropBy act webelement x y)))))

  (key-down
    ([driver k]
       (let [act (Actions. (.webdriver driver))]
         (.perform (.keyDown act (key-code k)))))
    ([driver webelement k]
       (let [act (Actions. (.webdriver driver))]
         (.perform (.keyDown act webelement (key-code k))))))

  (key-up
    ([driver k]
       (let [act (Actions. (.webdriver driver))]
         (.perform (.keyUp act (key-code k)))))
    ([driver webelement k]
       (let [act (Actions. (.webdriver driver))]
         (.perform (.keyUp act webelement (key-code k))))))

  (move-by-offset
    [driver x y]
    (let [act (Actions. (.webdriver driver))]
      (.perform (.moveByOffset act x y))))

  (move-to-element
    ([driver webelement]
       (let [act (Actions. (.webdriver driver))]
         (.perform (.moveToElement act webelement))))
    ([driver webelement x y]
       (let [act (Actions. (.webdriver driver))]
         (.perform (.moveToElement act webelement x y)))))

  (release
    ([driver]
       (let [act (Actions. (.webdriver driver))]
         (.release act)))
    ([driver element]
       (let [act (Actions. (.webdriver driver))]
         (.release act element)))))

(extend-type org.openqa.selenium.interactions.Actions

  IActions
  ;; TODO: test coverage
  (click-and-hold
    ([act]
       (.clickAndHold act))
    ([act webelement]
       (.clickAndHold act webelement)))

  ;; TODO: test coverage
  (double-click
    ([act]
       (.doubleClick act))
    ([act webelement]
       (.doubleClick act webelement)))

  ;; TODO: test coverage
  (drag-and-drop
    [act webelement-a webelement-b]
    (.dragAndDrop act webelement-a webelement-b))

  ;; TODO: test coverage
  (drag-and-drop-by
    [act webelement x y]
    (.dragAndDropBy act webelement x y))

  ;; TODO: test coverage
  (key-down
    ([act k]
       (.keyDown act (key-code k)))
    ([act webelement k]
       (.keyDown act webelement (key-code k))))

  ;; TODO: test coverage
  (key-up
    ([act k]
       (.keyUp act (key-code k)))
    ([act webelement k]
       (.keyUp act webelement (key-code k))))

  ;; TODO: test coverage
  (move-by-offset
    [act x y]
    (.moveByOffset act x y))

  ;; TODO: test coverage
  (move-to-element
    ([act webelement]
       (.moveToElement act webelement))
    ([act webelement x y]
       (.moveToElement act webelement x y)))

  ;; TODO: test coverage
  (perform [act] (.perform act))

  ;; TODO: test coverage
  (release
    ([act]
       (.release act))
    ([act webelement]
       (.release act webelement))))

(extend-type CompositeAction

  IActions
  (perform [comp-act] (.perform comp-act)))

(defn find-element* [driver attr-val]
  (first (find-elements driver attr-val)))

(defn find-elements* [driver attr-val]
  (when-not (and (or
                  (map? attr-val)
                  (vector? attr-val))
                 (empty? attr-val))
    (try
      (cond
        ;; Accept by-clauses
        (not (or (vector? attr-val)
                 (map? attr-val)))
        (find-elements-by driver attr-val)

        ;; Accept vectors for hierarchical queries
        (vector? attr-val)
        (find-by-hierarchy driver attr-val)

        ;; Build XPath dynamically
        :else
        (find-elements-by driver (by-query (build-query attr-val))))
      (catch org.openqa.selenium.NoSuchElementException e
        ;; NoSuchElementException caught here to mimic Clojure behavior like
        ;; (get {:foo "bar"} :baz) since the page can be thought of as a kind of associative
        ;; data structure with unique selectors as keys and HTML elements as values
        (lazy-seq nil)))))
