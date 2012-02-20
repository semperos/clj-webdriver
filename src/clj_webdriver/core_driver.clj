;; ## Core Driver-related Functions ##

;; This namespace provides the implementations for the following
;; protocols:

;;  * IDriver
;;  * ITargetLocator
;;  * IOptions
;;  * IFind
(in-ns 'clj-webdriver.core)

;; Utility used below
(defn filter-elements-by-regex
  "Given a collection of WebElements, filter the collection by the regular expression values for the respective attributes in the `attr-val` map"
  [elements attr-val]
  (let [attr-vals-with-regex (into {}
                                   (filter
                                    #(let [[k v] %] (= java.util.regex.Pattern (class v)))
                                    attr-val))]
    (loop [elements elements attr-vals-with-regex attr-vals-with-regex]
      (if (empty? attr-vals-with-regex)
        elements
        (let [entry (first attr-vals-with-regex)
              attr (key entry)
              value (val entry)
              matching-elements (if (= :text attr)
                                  (filter #(re-find value (text %)) elements)
                                  (filter (fn [el]
                                            ((fnil (partial re-find value) "")
                                             (attribute el (name attr))))
                                          elements))]
          (recur matching-elements (dissoc attr-vals-with-regex attr)))))))

(extend-type Driver

  ;; Basic Functions
  IDriver
  (back [driver]
    (.back (.navigate (:webdriver driver)))
    (cache/seed driver)
    driver)

  (close [driver]
    (let [handles (window-handles* (:webdriver driver))]
      (if (> (count handles) 1) ; get back to a window that is open before proceeding
        (let [this-handle (window-handle* (:webdriver driver))
              idx (.indexOf handles this-handle)]
          (cond
           (zero? idx) (do ; if first window, switch to next
                         (.close (:webdriver driver))
                         (switch-to-window driver (nth handles (inc idx))))
           :else (do ; otherwise, switch back one window
                   (.close (:webdriver driver))
                   (switch-to-window driver (nth handles (dec idx)))))
          (cache/seed driver {}))
        (do
          (.close (:webdriver driver))
          (cache/seed driver {})))))

  (current-url [driver]
    (.getCurrentUrl (:webdriver driver)))

  (forward [driver]
    (.forward (.navigate (:webdriver driver)))
    (cache/seed driver)
    driver)

  (get-url [driver url]
    (.get (:webdriver driver) url)
    (cache/seed driver)
    driver)

  (get-screenshot
    ([driver] (get-screenshot driver :file))
    ([driver format] (get-screenshot driver format nil))
    ([driver format destination]
       {:pre [(or (= format :file)
                  (= format :base64)
                  (= format :bytes))]}
       (let [wd (:webdriver driver)
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
    (.getPageSource (:webdriver driver)))

  (quit [driver]
    (.quit (:webdriver driver))
    (cache/seed driver {}))

  (refresh [driver]
    (.refresh (.navigate (:webdriver driver)))
    (cache/seed driver)
    driver)

  (title [driver]
    (.getTitle (:webdriver driver)))

  (to [driver url]
    (.to (.navigate (:webdriver driver)) url)
    (cache/seed driver)
    driver)


  ;; Window and Frame Handling
  ITargetLocator
  ;; TODO (possible): multiple arities; only driver, return current window handle; driver and query, return matching window handle
  (window-handle [driver]
    (init-window-handle (:webdriver driver)
                        (.getWindowHandle (:webdriver driver))
                        (title driver)
                        (current-url driver)))

  (window-handles [driver]
    (let [current-handle (.getWindowHandle (:webdriver driver))
          all-handles (lazy-seq (.getWindowHandles (:webdriver driver)))
          handle-records (for [handle all-handles]
                           (let [b (switch-to-window driver handle)]
                             (init-window-handle (:webdriver driver)
                                                 handle
                                                 (title b)
                                                 (current-url b))))]
      (switch-to-window driver current-handle)
      handle-records))

  (other-window-handles [driver]
    (remove #(= (:handle %) (:handle (window-handle driver)))
            (doall (window-handles driver))))

  (switch-to-frame [driver frame]
    (.frame (.switchTo (:webdriver driver)) frame)
    driver)

  (switch-to-window [driver handle]
    (cond
     (string? handle)            (do
                                   (.window (.switchTo (:webdriver driver)) handle)
                                   driver)
     (window-handle? handle)  (do
                                   (.window (.switchTo (:driver handle)) (:handle handle))
                                   driver)
     (number? handle)            (do
                                   (switch-to-window driver (nth (window-handles driver) handle))
                                   driver)
     (nil? handle)               (throw (RuntimeException. "No window can be found"))
     :else                       (do
                                   (.window (.switchTo (:webdriver driver)) handle)
                                   driver)))

  (switch-to-other-window [driver]
    (if (not= (count (window-handles driver)) 2)
      (throw (RuntimeException.
              (str "You may only use this function when two and only two "
                   "browser windows are open.")))
      (switch-to-window driver (first (other-window-handles driver)))))

  (switch-to-default [driver]
    (.defaultContent (.switchTo (:webdriver driver))))

  (switch-to-active [driver]
    (.activeElement (.switchTo (:webdriver driver))))


  ;; Options Interface (cookies)
  IOptions
  (add-cookie [driver cookie-spec]
    (.addCookie (.manage (:webdriver driver)) (:cookie (init-cookie cookie-spec)))
    driver)
  
  (delete-cookie-named [driver cookie-name]
    (.deleteCookieNamed (.manage (:webdriver driver)) cookie-name)
    driver)
  
  (delete-cookie [driver cookie-spec]
    (.deleteCookie (.manage (:webdriver driver)) (:cookie (init-cookie cookie-spec)))
    driver)
  
  (delete-all-cookies [driver]
    (.deleteAllCookies (.manage (:webdriver driver)))
    driver)
  
  (cookies [driver]
    (into #{} (map #(init-cookie {:cookie %})
                   (.getCookies (.manage (:webdriver driver))))))
  
  (cookie-named [driver cookie-name]
    (let [cookie-obj (.getCookieNamed (.manage (:webdriver driver)) cookie-name)]
      (init-cookie {:cookie cookie-obj})))


  ;; Find Functions
  IFind
  (find-element-by [driver by]
    (let [by (if (map? by)
               (by-xpath (build-xpath (:tag by) by))
               by)]
      (init-element (.findElement (:webdriver driver) by))))

  (find-elements-by [driver by]
    (let [by (if (map? by)
               (by-xpath (build-xpath (:tag by) by))
               by)
          els (.findElements (:webdriver driver) by)]
      (if (seq els)
        (lazy-seq (map init-element els))
        (lazy-seq (map init-element [nil])))))

  (find-elements-by-regex-alone [driver tag attr-val]
    (let [entry (first attr-val)
          attr (key entry)
          value (val entry)
          all-elements (find-elements driver (by-css (name tag)))] ; get all elements
      (if (= :text attr)
        (filter #(re-find value (text %)) all-elements)
        (filter (fn [el]
                  ((fnil (partial re-find value) "") ; `(attribute)` will return nil if the HTML element in question
                   (attribute el (name attr))))      ; doesn't support the attribute being passed in (e.g. :href on a <p>)
                all-elements))))

  (find-elements-by-regex [driver tag attr-val]
    (if (all-regex? attr-val)
      (let [elements (find-elements-by driver (by-css "*"))]
        (filter-elements-by-regex elements attr-val))
      (let [attr-vals-without-regex (into {}
                                          (remove
                                           #(let [[k v] %] (= java.util.regex.Pattern (class v)))
                                           attr-val))
            elements (find-elements driver (assoc attr-vals-without-regex :tag tag))]
        (filter-elements-by-regex elements attr-val))))

  (find-windows [driver attr-val]
    (if (contains? attr-val :index)
      [(nth (window-handles driver) (:index attr-val))] ; vector for consistency below
      (filter #(every? (fn [[k v]] (if (= java.util.regex.Pattern (class v))
                                     (re-find v (k %))
                                     (= (k %) v)))
                       attr-val) (window-handles driver))))

  (find-window [driver attr-val]
    (first (find-windows driver attr-val)))

  (find-semantic-buttons [driver attr-val]
    (let [attr-val (dissoc attr-val :tag)
          xpath-parts ["//input[@type='submit']"
                       "//input[@type='reset']"
                       "//input[@type='image']"
                       "//input[@type='button']"
                       "//button"]
          xpath-full (if (or (nil? attr-val) (empty? attr-val))
                       (interpose "|" xpath-parts)
                       (conj
                        (->> (repeat (str (build-xpath-attrs attr-val) "|"))
                             (interleave (drop-last xpath-parts))
                             vec)
                        (str "//button" (build-xpath-attrs attr-val))))]
      (->> (apply str xpath-full)
           by-xpath
           (find-elements-by driver))))

  ;; (find-semantic-buttons [driver attr-val]
  ;;   (let [ending (if (or (nil? attr-val) (empty? attr-val))
  ;;                  ""
  ;;                  (build-css-attrs (dissoc :tag attr-val)))
  ;;         css-full-queries (for [beginning ["input[type='submit']"
  ;;                                           "input[type='reset']"
  ;;                                           "input[type='image']"
  ;;                                           "input[type='button']"
  ;;                                           "button"]]
  ;;                            (str beginning ending))]
  ;;     (remove #(nil? (:webelement %))
  ;;             (flatten (for [query css-full-queries]
  ;;                        (find-elements driver (by-css query)))))))

  (find-semantic-buttons-by-regex [driver attr-val]
    (let [attr-vals-without-regex (into {}
                                        (remove
                                         #(let [[k v] %] (= java.util.regex.Pattern (class v)))
                                         attr-val))
          elements (find-semantic-buttons driver attr-vals-without-regex)]
      (filter-elements-by-regex elements attr-val)))

  (find-checkables-by-text [driver attr-val]
    (if (contains-regex? attr-val)
      (throw (IllegalArgumentException.
              (str "Combining regular expressions and the 'text' attribute "
                   "for finding radio buttons and checkboxes "
                   "is not supported at this time.")))
      (let [text-kw (if (contains? attr-val :text)
                      :text
                      :label)
            other-attr-vals (dissoc attr-val text-kw)
            non-text-xpath (build-xpath :input other-attr-vals)
            text-xpath (str non-text-xpath "[contains(..,'" (text-kw attr-val) "')]")]
        (find-elements-by driver (by-xpath text-xpath)))))
  ;; (find-checkables-by-text [driver attr-val]
  ;;   (if (contains-regex? attr-val)
  ;;     (throw (IllegalArgumentException.
  ;;             (str "Combining regular expressions and the 'text' attribute "
  ;;                  "for finding radio buttons and checkboxes "
  ;;                  "is not supported at this time.")))
  ;;     (let [text-kw (if (contains? attr-val :text)
  ;;                     :text
  ;;                     :label)
  ;;           other-attr-vals (dissoc attr-val text-kw)
  ;;           non-text-css (build-css :input other-attr-vals)
  ;;           text-css (str non-text-css ":contains(\"^" (text-kw attr-val) "$\")")]
  ;;       (find-elements-by driver (by-css text-css)))))

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
      (find-element-by table (by-css complete-css))))

  (find-table-row [driver table row]
    (let [row-css (str  "tr:nth-child(" (inc row) ")")
          complete-css (if (and (find-element-by table (by-tag "th"))
                                  (zero? row))
                           (str row-css " " "th")
                           (str row-css " " "td"))]
      (find-elements-by table (by-css complete-css))))

  ;; TODO: reconsider find-table-col with CSS support

  (find-by-hierarchy [driver hierarchy-vec]
    (if (query-with-ancestry-has-regex? hierarchy-vec)
      (if (query-with-ancestry-has-regex? (drop-last hierarchy-vec))
        (throw (IllegalArgumentException.
                (str "You may not pass in a regex until "
                     "the last attr-val map")))
        (filter-elements-by-regex
         (find-elements-by driver (by-xpath (str (build-xpath-with-ancestry hierarchy-vec) "//*")))
         (last hierarchy-vec)))
      (find-elements-by driver (by-xpath (build-xpath-with-ancestry hierarchy-vec)))))


  (find-elements
    ([driver attr-val]
       (when (keyword? driver) ; I keep forgetting to pass in the WebDriver instance while testing
         (throw (IllegalArgumentException.
                 (str "The first parameter to find-elements must be an instance of WebDriver."))))
       (if (and (or
                 (map? attr-val)
                 (vector? attr-val))
                (empty? attr-val))
         nil
         (try
           (cond
            (not (or (vector? attr-val)
                     (map? attr-val)))   (find-elements-by driver attr-val)
            (vector? attr-val)       (find-by-hierarchy driver attr-val); supplied vector of queries in hierarchy
            (and (= (keys attr-val) '(:tag))
                 (not (some #{(:tag attr-val)} [:button*
                                                :radio
                                                :checkbox
                                                :textfield
                                                :password
                                                :filefield])))            (find-elements-by
                                                                           driver
                                                                           (by-tag (:tag attr-val)))
            (and (not (contains? attr-val :tag))
                 (not (contains? attr-val :xpath))
                 (not (contains? attr-val :css)))     (find-elements driver (assoc attr-val :tag :*))
            (and (> (count attr-val) 1)
                 (contains? attr-val :xpath))          (find-elements driver {:xpath (:xpath attr-val)})
            (and (> (count attr-val) 1)
                 (contains? attr-val :css))            (find-elements driver {:css (:css attr-val)})

          (contains? attr-val :index)                (find-elements-by driver (by-xpath (build-xpath (:tag attr-val) attr-val)))
          (= (:tag attr-val) :radio)                             (find-elements driver (assoc attr-val :tag :input :type "radio"))
          (= (:tag attr-val) :checkbox)                          (find-elements driver (assoc attr-val :tag :input :type "checkbox"))
          (= (:tag attr-val) :textfield)                         (find-elements driver (assoc attr-val :tag :input :type "text"))
          (= (:tag attr-val) :password)                          (find-elements driver (assoc attr-val :tag :input :type "password"))
          (= (:tag attr-val) :filefield)                         (find-elements driver (assoc attr-val :tag :input :type "file"))
          (and (= (:tag attr-val) :input)
               (contains? attr-val :type)
               (or (= "radio" (:type attr-val))
                   (= "checkbox" (:type attr-val)))
               (or (contains? attr-val :text)
                   (contains? attr-val :label)))       (find-checkables-by-text driver attr-val)
          (= (:tag attr-val) :button*)                 (if (contains-regex? attr-val)
                                                         (find-semantic-buttons-by-regex driver attr-val)
                                                         (find-semantic-buttons driver attr-val))
          (= 1 (count attr-val))                     (let [entry (first attr-val)
                                                           attr  (key entry)
                                                           value (val entry)]
                                                       (cond
                                                         (= :xpath attr) (find-elements-by driver (by-xpath value))
                                                         (= :css attr)   (find-elements-by driver (by-css value))
                                                         (= java.util.regex.Pattern (class value)) (find-elements-by-regex-alone driver (:tag attr-val) attr-val)
                                                         :else           (find-elements-by driver (by-attr= (:tag attr-val) attr value))))
          (contains-regex? attr-val)                 (find-elements-by-regex driver (:tag attr-val) attr-val)
          :else                                      (find-elements-by driver (by-xpath (build-xpath (:tag attr-val) attr-val))))
           (catch org.openqa.selenium.NoSuchElementException e
             ;; NoSuchElementException caught here, so we can have functions like `exist?`
             (lazy-seq (init-element nil)))))))

  (find-element
    ([driver attr-val]
       (if (and (cache/cache-enabled? driver) (cache/in-cache? driver attr-val))
         (cache/retrieve driver attr-val)
         (let [el (first (find-elements driver attr-val))]
           (if (and (cache/cache-enabled? driver)
                    (and (not (nil? el))
                         (exists? el))
                    (or (cache/cacheable? driver attr-val) (cache/cacheable? driver el)))
             (do
               (cache/insert driver attr-val el)
               el)
             el)))))

  IActions

  (click-and-hold
    ([driver]
       (let [act (:actions driver)]
         (.perform (.clickAndHold act))))
    ([driver element]
       (let [act (:actions driver)]
         (.perform (.clickAndHold act (:webelement element))))))

  (double-click
    ([driver]
       (let [act (:actions driver)]
         (.perform (.doubleClick act))))
    ([driver element]
       (let [act (:actions driver)]
         (.perform (.doubleClick act (:webelement element))))))
  
  (drag-and-drop
    [driver element-a element-b]
    (let [act (:actions driver)]
      (.perform (.dragAndDrop act (:webelement element-a) (:webelement element-b)))))

  (drag-and-drop-by
    [driver element x-y-map]
    (let [act (:actions driver)]
      (.perform (.dragAndDropBy act (:webelement element) {:x x-y-map} {:y x-y-map}))))

  (key-down
    ([driver k]
       (let [act (:actions driver)]
         (.perform (.keyDown act (key-code k)))))
    ([driver element k]
       (let [act (:actions driver)]
         (.perform (.keyDown act (:webelement element) (key-code k))))))

  (key-up
    ([driver k]
       (let [act (:actions driver)]
         (.perform (.keyUp act (key-code k)))))
    ([driver element k]
       (let [act (:actions driver)]
         (.perform (.keyUp act (:webelement element) (key-code k))))))

  (move-by-offset
    [driver x y]
    (let [act (:actions driver)]
      (.perform (.moveByOffset act x y))))

  (move-to-element
    ([driver element]
       (let [act (:actions driver)]
         (.perform (.moveToElement act (:webelement element)))))
    ([driver element x y]
       (let [act (:actions driver)]
         (.perform (.moveToElement act (:webelement element) x y)))))

  (release
    ([driver]
       (let [act (:actions driver)]
         (.release act)))
    ([driver element]
       (let [act (:actions driver)]
         (.release act element)))))

(extend-type org.openqa.selenium.interactions.Actions

  IActions
  ;; TODO: test coverage
  (click-and-hold
    ([act]
       (.clickAndHold act))
    ([act element]
       (.clickAndHold act (:webelement element))))

  ;; TODO: test coverage
  (double-click
    ([act]
       (.doubleClick act))
    ([act element]
       (.doubleClick act (:webelement element))))

  ;; TODO: test coverage
  (drag-and-drop
    [act element-a element-b]
    (.dragAndDrop act (:webelement element-a) (:webelement element-b)))

  ;; TODO: test coverage
  (drag-and-drop-by
    [act element x y]
    (.dragAndDropBy act (:webelement element) x y))

  ;; TODO: test coverage
  (key-down
    ([act k]
       (.keyDown act (key-code k)))
    ([act element k]
       (.keyDown act (:webelement element) (key-code k))))

  ;; TODO: test coverage
  (key-up
    ([act k]
       (.keyUp act (key-code k)))
    ([act element k]
       (.keyUp act (:webelement element) (key-code k))))

  ;; TODO: test coverage
  (move-by-offset
    [act x y]
    (.moveByOffset act x y))

  ;; TODO: test coverage
  (move-to-element
    ([act element]
       (.moveToElement act (:webelement element)))
    ([act element x y]
       (.moveToElement act (:webelement element) x y)))

  ;; TODO: test coverage
  (perform [act] (.perform act))

  ;; TODO: test coverage
  (release
    ([act]
       (.release act))
    ([act element]
       (.release act (:webelement element)))))

(extend-type CompositeAction

  IActions
  (perform [comp-act] (.perform comp-act)))