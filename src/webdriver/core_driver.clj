;; ## Core Driver-related Functions ##

;; This namespace provides the implementations for the following
;; protocols:

;;  * IDriver
;;  * ITargetLocator
;;  * IAlert
;;  * IOptions
;;  * IFind
(in-ns 'webdriver.core)

(declare find-element* find-elements*)

(extend-type Driver

  ;; Basic Functions
  IDriver
  (back [driver]
    (.back (.navigate ^WebDriver (.webdriver driver)))
    driver)

  (close [driver]
    (let [handles (into #{} (.getWindowHandles ^WebDriver (.webdriver driver)))]
      (when (> (count handles) 1) ; get back to a window that is open before proceeding
        (let [current-handle (.getWindowHandle ^WebDriver (.webdriver driver))]
          (switch-to-window driver (first (disj handles current-handle)))))
      (.close ^WebDriver (.webdriver driver))))

  (current-url [driver]
    (.getCurrentUrl ^WebDriver (.webdriver driver)))

  (forward [driver]
    (.forward (.navigate ^WebDriver (.webdriver driver)))
    driver)

  (get-url [driver url]
    (.get ^WebDriver (.webdriver driver) url)
    driver)

  (get-screenshot
    ([driver] (get-screenshot driver :file))
    ([driver format] (get-screenshot driver format nil))
    ([driver format destination]
     {:pre [(or (= format :file)
                (= format :base64)
                (= format :bytes))]}
     (let [wd ^TakesScreenshot (.webdriver driver)
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
    (.getPageSource ^WebDriver (.webdriver driver)))

  (quit [driver]
    (.quit ^WebDriver (.webdriver driver)))

  (refresh [driver]
    (.refresh (.navigate ^WebDriver (.webdriver driver)))
    driver)

  (title [driver]
    (.getTitle ^WebDriver (.webdriver driver)))

  (to [driver ^String url]
    (.to (.navigate ^WebDriver (.webdriver driver)) url)
    driver)


  ;; Window and Frame Handling
  ITargetLocator
  ;; TODO (possible): multiple arities; only driver, return current window handle; driver and query, return matching window handle
  (window [driver]
    (window* driver))

  (window-handle [driver]
    (.getWindowHandle ^WebDriver (.webdriver driver)))

  (window-handles [driver]
    (into #{} (.getWindowHandles ^WebDriver (.webdriver driver))))

  (other-window-handles [driver]
    (let [handles (window-handles driver)]
      (disj handles (.getWindowHandle ^WebDriver (.webdriver driver)))))

  (switch-to-frame [driver frame]
    ;; reflection warnings
    (cond
      (string? frame) (.frame (.switchTo ^WebDriver (.webdriver driver)) ^String frame)
      (number? frame) (.frame (.switchTo ^WebDriver (.webdriver driver)) ^int frame)
      :else (.frame (.switchTo ^WebDriver (.webdriver driver)) ^WebElement frame))
    driver)

  (switch-to-window [driver window-handle]
    (.window (.switchTo ^WebDriver (.webdriver driver)) window)
    driver)

  (switch-to-other-window [driver]
    (if (= (count (window-handles driver)) 2)
      (switch-to-window driver (first (other-window-handles driver)))
      (throw (ex-info "You may use this function iff two windows are open."
                      {:window-handles (window-handles driver)}))))

  (switch-to-default [driver]
    (.defaultContent (.switchTo ^WebDriver (.webdriver driver))))

  (switch-to-active [driver]
    (.activeElement (.switchTo ^WebDriver (.webdriver driver))))

  ;; Options Interface (cookies)
  IOptions
  (add-cookie [driver cookie]
    (.addCookie (.manage ^WebDriver (.webdriver driver)) cookie)
    driver)

  (delete-cookie-named [driver cookie-name]
    (.deleteCookieNamed (.manage ^WebDriver (.webdriver driver)) cookie-name)
    driver)

  (delete-cookie [driver cookie]
    (.deleteCookie (.manage ^WebDriver (.webdriver driver)) cookie)
    driver)

  (delete-all-cookies [driver]
    (.deleteAllCookies (.manage ^WebDriver (.webdriver driver)))
    driver)

  (cookies [driver]
    (into #{} (.getCookies (.manage ^WebDriver (.webdriver driver)))))

  (cookie-named [driver cookie-name]
    (.getCookieNamed (.manage ^WebDriver (.webdriver driver)) cookie-name))

  ;; Alert dialogs
  IAlert
  (accept [driver]
    (.accept (.alert (.switchTo ^WebDriver (.webdriver driver)))))

  (alert-obj [driver]
    (.alert (.switchTo ^WebDriver (.webdriver driver))))

  (alert-text [driver]
    (let [^WebDriver webdriver (.webdriver driver)
          switch (.switchTo webdriver)
          alert (.alert switch)]
      (.getText alert)))

  ;; (authenticate-using [driver username password]
  ;;   (let [creds (UserAndPassword. username password)]
  ;;     (-> (.webdriver driver) .switchTo .alert (.authenticateUsing creds))))

  (dismiss [driver]
    (.dismiss (.alert (.switchTo ^WebDriver (.webdriver driver)))))

  ;; Find Functions
  IFind
  (find-element-by [driver by-value]
    (let [by-value (if (map? by-value)
                     (by-query (build-query by-value))
                     by-value)]
      (.findElement ^WebDriver (.webdriver driver) by-value)))

  (find-elements-by [driver by-value]
    (let [by-value (if (map? by-value)
                     (by-query (build-query by-value))
                     by-value)]
      (.findElements ^WebDriver (.webdriver driver) by-value)))

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
     (let [act (Actions. ^WebDriver (.webdriver driver))]
       (.perform (.clickAndHold act))))
    ([driver webelement]
     (let [act (Actions. ^WebDriver (.webdriver driver))]
       (.perform (.clickAndHold act webelement)))))

  (double-click
    ([driver]
     (let [act (Actions. ^WebDriver (.webdriver driver))]
       (.perform (.doubleClick act))))
    ([driver webelement]
     (let [act (Actions. ^WebDriver (.webdriver driver))]
       (.perform (.doubleClick act webelement)))))

  (drag-and-drop
    [driver webelement-a webelement-b]
    (cond
      (nil? webelement-a) (throw-nse "The first element does not exist.")
      (nil? webelement-b) (throw-nse "The second element does not exist.")
      :else (let [act (Actions. ^WebDriver (.webdriver driver))]
              (.perform (.dragAndDrop act
                                      webelement-a
                                      webelement-b)))))

  (drag-and-drop-by
    [driver webelement x-y-map]
    (if (nil? webelement)
      (throw-nse)
      (let [act (Actions. ^WebDriver (.webdriver driver))
            {:keys [x y] :or {x 0 y 0}} x-y-map]
        (.perform
         (.dragAndDropBy act webelement x y)))))

  (key-down
    ([driver k]
     (let [act (Actions. ^WebDriver (.webdriver driver))]
       (.perform (.keyDown act (key-code k)))))
    ([driver webelement k]
     (let [act (Actions. ^WebDriver (.webdriver driver))]
       (.perform (.keyDown act webelement (key-code k))))))

  (key-up
    ([driver k]
     (let [act (Actions. ^WebDriver (.webdriver driver))]
       (.perform (.keyUp act (key-code k)))))
    ([driver webelement k]
     (let [act (Actions. ^WebDriver (.webdriver driver))]
       (.perform (.keyUp act webelement (key-code k))))))

  (move-by-offset
    [driver x y]
    (let [act (Actions. ^WebDriver (.webdriver driver))]
      (.perform (.moveByOffset act x y))))

  (move-to-element
    ([driver webelement]
     (let [act (Actions. ^WebDriver (.webdriver driver))]
       (.perform (.moveToElement act webelement))))
    ([driver webelement x y]
     (let [act (Actions. ^WebDriver (.webdriver driver))]
       (.perform (.moveToElement act webelement x y)))))

  (release
    ([driver]
     (let [act (Actions. ^WebDriver (.webdriver driver))]
       (.release act)))
    ([driver element]
     (let [act (Actions. ^WebDriver (.webdriver driver))]
       (.release act element)))))

(extend-type Actions

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
        nil))))
