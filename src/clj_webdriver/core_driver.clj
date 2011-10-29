(in-ns 'clj-webdriver.core)

(extend-type Driver 

  ;;; Basic Functions ;;;
  IDriver
  (get-url [driver url]
    (.get (:webdriver driver) url)
    (cache/seed driver)
    driver)

  (to [driver url]
    (.to (.navigate (:webdriver driver)) url)
    (cache/seed driver)
    driver)

  (current-url [driver]
    (.getCurrentUrl (:webdriver driver)))

  (title [driver]
    (.getTitle (:webdriver driver)))

  (page-source [driver]
    (.getPageSource (:webdriver driver)))

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
          (cache/seed driver))
        (do
          (.close (:webdriver driver))
          (cache/seed driver)))))
  
  (quit [driver]
    (.quit (:webdriver driver))
    (cache/seed driver))
  
  (back [driver]
    (.back (.navigate (:webdriver driver)))
    (cache/seed driver)
    driver)

  (forward [driver]
    (.forward (.navigate (:webdriver driver)))
    (cache/seed driver)
    driver)

  (refresh [driver]
    (.refresh (.navigate (:webdriver driver)))
    (cache/seed driver)
    driver)


  ;;; Window and Frame Handling ;;;
  ITargetLocator
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
     (is-window-handle? handle)  (do
                                   (.window (.switchTo (:driver handle)) (:handle handle))
                                   driver)
     (number? handle)            (do
                                   driver(switch-to-window driver (nth (window-handles driver) handle)))
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


  ;;; Options Interface (cookies) ;;;
  IOptions
  (add-cookie [driver cookie]
    (.addCookie (.manage (:webdriver driver)) cookie))
  (delete-cookie-named [driver cookie]
    (.deleteCookieNamed (.manage (:webdriver driver)) name))
  (delete-cookie [driver cookie]
    (.deleteCookie (.manage (:webdriver driver)) cookie))
  (delete-all-cookies [driver]
    (.deleteAllCookies (.manage (:webdriver driver))))
  (cookies [driver]
    (into #{} (.getCookies (.manage (:webdriver driver)))))
  (cookie-named [driver name]
    (.getCookieNamed (.manage (:webdriver driver)) name))


  ;;; Find Functions ;;;
  IFind
  (find-element [driver by]
    (.findElement (:webdriver driver) by))
  
  (find-elements [driver by]
    (lazy-seq (.findElements (:webdriver driver) by)))
  
  (find-elements-by-regex-alone [driver tag attr-val]
    (let [entry (first attr-val)
          attr (key entry)
          value (val entry)
          all-elements (find-elements driver (by-xpath (str "//" (name tag))))] ; get all elements
      (if (= :text attr)
        (filter #(re-find value (text %)) all-elements)
        (filter (fn [el]
                  ((fnil (partial re-find value) "") ; `(attribute)` will return nil if the HTML element in question
                   (attribute el (name attr))))      ; doesn't support the attribute being passed in (e.g. :href on a <p>)
                all-elements))))

  (find-elements-by-regex [driver tag attr-val]
    (if (all-regex? attr-val)
      (let [elements (find-elements driver (by-xpath "//*"))]
        (filter-elements-by-regex elements attr-val))
      (let [attr-vals-without-regex (into {}
                                          (remove
                                           #(let [[k v] %] (= java.util.regex.Pattern (class v)))
                                           attr-val))
            elements (find-them driver tag attr-vals-without-regex)]
        (filter-elements-by-regex elements attr-val))))

  (find-window-handles [driver attr-val]
    (if (contains? attr-val :index)
      [(nth (window-handles driver) (:index attr-val))] ; vector for consistency below
      (filter #(every? (fn [[k v]] (if (= java.util.regex.Pattern (class v))
                                     (re-find v (k %))
                                     (= (k %) v)))
                       attr-val) (window-handles driver))))

  (find-semantic-buttons [driver attr-val]
    (let [xpath-parts ["//input[@type='submit']"
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
           (find-elements driver))))

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
        (find-elements driver (by-xpath text-xpath)))))

  (find-table-cells [driver attr-val]
    (let [attr-val-map (apply hash-map attr-val)
          table-xpath (build-xpath :table (:table attr-val-map))
          row-xpath (str "//tr[" (inc (:row attr-val-map)) "]")
          col-xpath (if (and (find-element driver (by-xpath (str table-xpath "//th")))
                             (zero? (:row attr-val-map)))
                      (str "/th[" (inc (:col attr-val-map)) "]")
                      (str "/td[" (inc (:col attr-val-map)) "]"))
          complete-xpath (str table-xpath row-xpath col-xpath)]
      (find-elements driver (by-xpath complete-xpath))))

  (find-them*
    ([driver attr-val]
       (cond
        (= attr-val :button*)   (find-them driver :button* nil)
        (keyword? attr-val)     (find-elements
                                 driver
                                 (by-tag-name (name attr-val))) ; supplied just :tag
        (vector? attr-val)      (cond
                                 (some #{:row :col} attr-val) (find-table-cells driver attr-val)
                                 (query-with-ancestry-has-regex? attr-val) (if (query-with-ancestry-has-regex? (drop-last 2 attr-val))
                                                                             (throw (IllegalArgumentException.
                                                                                     (str "You may not pass in a regex until "
                                                                                          "the last attribute-value pair")))
                                                                             (filter-elements-by-regex
                                                                              (find-elements driver (by-xpath (str (build-xpath-with-ancestry attr-val) "//*")))
                                                                              (last attr-val)))
                                 :else (find-elements driver (by-xpath (build-xpath-with-ancestry attr-val)))) ; supplied vector of queries in hierarchy
        (map? attr-val)         (find-them driver :* attr-val)))
    ([driver tag attr-val]
       (when (keyword? driver) ; I keep forgetting to pass in the WebDriver instance while testing
         (throw (IllegalArgumentException.
                 (str "The first parameter to find-them must be an instance of WebDriver."))))
       (cond
        (and (> (count attr-val) 1)
             (contains? attr-val :xpath))          (find-them driver :* {:xpath (:xpath attr-val)})
             (and (> (count attr-val) 1)
                  (contains? attr-val :css))            (find-them driver :* {:css (:css attr-val)})
                  (contains? attr-val :tag-name)             (find-them driver
                                                                        (-> (:tag-name attr-val)
                                                                            .toLowerCase
                                                                            keyword)
                                                                        (dissoc attr-val :tag-name))
                  (contains? attr-val :index)                (find-elements driver (by-xpath (build-xpath tag attr-val)))
                  (= tag :radio)                             (find-them driver :input (assoc attr-val :type "radio"))
                  (= tag :checkbox)                          (find-them driver :input (assoc attr-val :type "checkbox"))
                  (= tag :textfield)                         (find-them driver :input (assoc attr-val :type "text"))
                  (= tag :password)                          (find-them driver :input (assoc attr-val :type "password"))
                  (= tag :filefield)                         (find-them driver :input (assoc attr-val :type "file"))
                  (and (= tag :input)
                       (contains? attr-val :type)
                       (or (= "radio" (:type attr-val))
                           (= "checkbox" (:type attr-val)))
                       (or (contains? attr-val :text)
                           (contains? attr-val :label)))     (find-checkables-by-text driver attr-val)
                           (= tag :window)                            (find-window-handles driver attr-val)
                           (= tag :button*)                           (if (contains-regex? attr-val)
                                                                        (find-semantic-buttons-by-regex driver attr-val)
                                                                        (find-semantic-buttons driver attr-val))
                           (= 1 (count attr-val))                     (let [entry (first attr-val)
                                                                            attr  (key entry)
                                                                            value (val entry)]
                                                                        (cond
                                                                         (= :xpath attr) (find-elements driver (by-xpath value))
                                                                         (= :css attr)   (find-elements driver (by-css-selector value))
                                                                         (= java.util.regex.Pattern (class value)) (find-elements-by-regex-alone driver tag attr-val)
                                                                         :else           (find-elements driver (by-attr= tag attr value))))
                           (contains-regex? attr-val)                 (find-elements-by-regex driver tag attr-val)
                           :else                                      (find-elements driver (by-xpath (build-xpath tag attr-val))))))

  (find-them
    ([driver attr-val]
       (let [elts (find-them* driver attr-val)]
         (if-not (seq elts)
           (throw (NoSuchElementException.
                   (str "No element with attributes "
                        attr-val " "
                        "could be found on the page:\n"
                        (page-source driver))))
           elts)))
    ([driver tag attr-val]
       (let [elts (find-them* driver tag attr-val)]
         (if-not (seq elts)
           (throw (NoSuchElementException.
                   (str "No element with tag "
                        tag " and attributes "
                        attr-val " "
                        "could be found on the page:\n"
                        (page-source driver))))
           elts))))

  (find-it
    ([driver attr-val]
       (if (and (cache/cache-enabled? driver) (cache/in-cache? driver attr-val))
         (cache/retrieve driver attr-val)
         (let [el (first (find-them driver attr-val))]
           (if (or (cache/cacheable? driver attr-val) (cache/cacheable? driver el))
             (do
               (cache/insert driver attr-val el)
               el)
             el))))
    ([driver tag attr-val]
       (if (and (cache/cache-enabled? driver) (cache/in-cache? driver [tag attr-val]))
         (cache/retrieve driver [tag attr-val])
         (let [el (first (find-them driver tag attr-val))]
           (if (cache/cacheable? driver el)
             (do
               (cache/insert driver [tag attr-val] el)
               el)
             el))))))
