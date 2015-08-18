;; ## Core WebDriver-related Functions ##

;; This namespace provides the implementations for the following
;; protocols:

;;  * IDriver
;;  * ITargetLocator
;;  * IAlert
;;  * IOptions
;;  * IFind
(in-ns 'webdriver.core)

(defn ^Actions new-actions
  "Create a new Actions object given a `WebDriver`"
  [^WebDriver wd]
  (Actions. wd))

;; Needed by window and target locator implementations in core_driver and core_window
(defn ^WebDriver$Window window*
  "Return the underyling `WebDriver$Window` object for the `WebDriver`"
  [^WebDriver wd]
  (.window (.manage wd)))

(defn key-code
  "Representations of pressable keys that aren't text. These are stored in the Unicode PUA (Private Use Area) code points, 0xE000-0xF8FF. Refer to http://www.google.com.au/search?&q=unicode+pua&btnG=Search"
  [k]
  (Keys/valueOf (.toUpperCase (name k))))

;; ## JavaScript Execution ##
(defn execute-script*
  "Version of execute-script that uses a WebDriver instance directly."
  [^RemoteWebDriver webdriver js & js-args]
  (.executeScript webdriver ^String js (into-array Object js-args)))

(defn execute-script
  [^WebDriver wd js & js-args]
  (apply execute-script* wd js js-args))

(declare find-element* find-elements*)

(extend-type WebDriver

  ;; Basic Functions
  IDriver
  (back [wd]
    (.back (.navigate wd))
    wd)

  (close [wd]
    (let [handles (into #{} (.getWindowHandles wd))]
      (when (> (count handles) 1) ; get back to a window that is open before proceeding
        (let [current-handle (.getWindowHandle wd)]
          (switch-to-window wd (first (disj handles current-handle)))))
      (.close wd)))

  (current-url [wd]
    (.getCurrentUrl wd))

  (forward [wd]
    (.forward (.navigate wd))
    wd)

  (get-url [wd url]
    (.get wd url)
    wd)

  (get-screenshot
    ([wd] (get-screenshot wd :file))
    ([wd format] (get-screenshot wd format nil))
    ([wd format destination]
     {:pre [(or (= format :file)
                (= format :base64)
                (= format :bytes))]}
     (let [wd ^TakesScreenshot wd
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

  (page-source [wd]
    (.getPageSource wd))

  (quit [wd]
    (.quit wd))

  (refresh [wd]
    (.refresh (.navigate wd))
    wd)

  (title [wd]
    (.getTitle wd))

  (to [wd ^String url]
    (.to (.navigate wd) url)
    wd)


  ;; Window and Frame Handling
  ITargetLocator
  (window [wd]
    (window* wd))

  (window-handle [wd]
    (.getWindowHandle wd))

  (window-handles [wd]
    (into #{} (.getWindowHandles wd)))

  (other-window-handles [wd]
    (let [handles (window-handles wd)]
      (disj handles (.getWindowHandle wd))))

  (switch-to-frame [wd frame]
    ;; reflection warnings
    (cond
      (string? frame) (.frame (.switchTo wd) ^String frame)
      (number? frame) (.frame (.switchTo wd) ^int frame)
      :else (.frame (.switchTo wd) ^WebElement frame))
    wd)

  (switch-to-window [wd window-handle]
    (.window (.switchTo wd) window-handle)
    wd)

  (switch-to-other-window [wd]
    (if (= (count (window-handles wd)) 2)
      (switch-to-window wd (first (other-window-handles wd)))
      (throw (ex-info "You may use this function iff two windows are open."
                      {:window-handles (window-handles wd)}))))

  (switch-to-default [wd]
    (.defaultContent (.switchTo wd)))

  (switch-to-active [wd]
    (.activeElement (.switchTo wd)))

  ;; Options Interface (cookies)
  IOptions
  (add-cookie [wd cookie]
    (.addCookie (.manage wd) cookie)
    wd)

  (delete-cookie-named [wd cookie-name]
    (.deleteCookieNamed (.manage wd) cookie-name)
    wd)

  (delete-cookie [wd cookie]
    (.deleteCookie (.manage wd) cookie)
    wd)

  (delete-all-cookies [wd]
    (.deleteAllCookies (.manage wd))
    wd)

  (cookies [wd]
    (into #{} (.getCookies (.manage wd))))

  (cookie-named [wd cookie-name]
    (.getCookieNamed (.manage wd) cookie-name))

  ;; Alert dialogs
  IAlert
  (accept [wd]
    (.accept (.alert (.switchTo wd))))

  (alert-obj [wd]
    (.alert (.switchTo wd)))

  (alert-text [wd]
    (let [switch (.switchTo wd)
          alert (.alert switch)]
      (.getText alert)))

  ;; (authenticate-using [wd username password]
  ;;   (let [creds (UserAndPassword. username password)]
  ;;     (-> wd .switchTo .alert (.authenticateUsing creds))))

  (dismiss [wd]
    (.dismiss (.alert (.switchTo wd))))

  ;; Find Functions
  IFind
  (find-element-by [wd by-value]
    (let [by-value (if (map? by-value)
                     (by-query (build-query by-value))
                     by-value)]
      (.findElement wd by-value)))

  (find-elements-by [wd by-value]
    (let [by-value (if (map? by-value)
                     (by-query (build-query by-value))
                     by-value)]
      (.findElements wd by-value)))

  (find-table-cell [wd table coords]
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

  (find-table-row [wd table row]
    (let [row-css (str  "tr:nth-child(" (inc row) ")")
          complete-css (if (and (find-element-by table (by-tag "th"))
                                (zero? row))
                         (str row-css " " "th")
                         (str row-css " " "td"))]
      ;; WebElement, not WebDriver, version of protocol
      (find-elements-by table (by-query {:css complete-css}))))

  ;; TODO: reconsider find-table-col with CSS support

  (find-by-hierarchy [wd hierarchy-vec]
    (find-elements wd {:xpath (build-query hierarchy-vec)}))

  (find-elements
    ([wd attr-val]
     (find-elements* wd attr-val)))

  (find-element
    ([wd attr-val]
     (find-element* wd attr-val)))

  IActions

  (click-and-hold
    ([wd]
     (let [act (new-actions wd)]
       (.perform (.clickAndHold act))))
    ([wd webelement]
     (let [act (new-actions wd)]
       (.perform (.clickAndHold act webelement)))))

  (double-click
    ([wd]
     (let [act (new-actions wd)]
       (.perform (.doubleClick act))))
    ([wd webelement]
     (let [act (new-actions wd)]
       (.perform (.doubleClick act webelement)))))

  (drag-and-drop
    [wd webelement-a webelement-b]
    (cond
      (nil? webelement-a) (throw-nse "The first element does not exist.")
      (nil? webelement-b) (throw-nse "The second element does not exist.")
      :else (let [act (new-actions wd)]
              (.perform (.dragAndDrop act
                                      webelement-a
                                      webelement-b)))))

  (drag-and-drop-by
    [wd webelement x-y-map]
    (if (nil? webelement)
      (throw-nse)
      (let [act (new-actions wd)
            {:keys [x y] :or {x 0 y 0}} x-y-map]
        (.perform
         (.dragAndDropBy act webelement x y)))))

  (key-down
    ([wd k]
     (let [act (new-actions wd)]
       (.perform (.keyDown act (key-code k)))))
    ([wd webelement k]
     (let [act (new-actions wd)]
       (.perform (.keyDown act webelement (key-code k))))))

  (key-up
    ([wd k]
     (let [act (new-actions wd)]
       (.perform (.keyUp act (key-code k)))))
    ([wd webelement k]
     (let [act (new-actions wd)]
       (.perform (.keyUp act webelement (key-code k))))))

  (move-by-offset
    [wd x y]
    (let [act (new-actions wd)]
      (.perform (.moveByOffset act x y))))

  (move-to-element
    ([wd webelement]
     (let [act (new-actions wd)]
       (.perform (.moveToElement act webelement))))
    ([wd webelement x y]
     (let [act (new-actions wd)]
       (.perform (.moveToElement act webelement x y)))))

  (release
    ([wd]
     (let [act (new-actions wd)]
       (.release act)))
    ([wd element]
     (let [act (new-actions wd)]
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

(defn find-element* [wd attr-val]
  (first (find-elements wd attr-val)))

(defn find-elements* [wd attr-val]
  (when (seq attr-val)
    (try
      (cond
        ;; Accept by-clauses
        (instance? By attr-val)
        (find-elements-by wd attr-val)

        ;; Accept vectors for hierarchical queries
        (vector? attr-val)
        (find-by-hierarchy wd attr-val)

        ;; Build CSS/XPath dynamically
        :else
        (find-elements-by wd (by-query (build-query attr-val))))
      (catch org.openqa.selenium.NoSuchElementException e
        ;; NoSuchElementException caught here to mimic Clojure behavior like
        ;; (get {:foo "bar"} :baz) since the page can be thought of as a kind of associative
        ;; data structure with unique selectors as keys and HTML elements as values
        nil))))
