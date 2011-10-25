(in-ns 'clj-webdriver.core)

(defrecord Driver [webdriver cache-strategy middlewares]

  ;;; Basic Functions ;;;
  IDriverBasics
  (get-url [driver url]
    (.get (:webdriver driver) url)
    driver)

  (to [driver url]
    (.to (.navigate (:webdriver driver)) url)
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
                         (switch-to-window (:webdriver driver) (nth handles (inc idx))))
           :else (do ; otherwise, switch back one window
                   (.close (:webdriver driver))
                   (switch-to-window (:webdriver driver) (nth handles (dec idx))))))
        (.close (:webdriver driver)))))
  
  (quit [driver]
    (.quit (:webdriver driver)))
  
  (back [driver]
    (.back (.navigate (:webdriver driver)))
    driver)

  (forward [driver]
    (.forward (.navigate (:webdriver driver)))
    driver)

  (refresh [driver]
    (.refresh (.navigate (:webdriver driver)))
    driver)


  ;;; Windows and Frames ;;;
  ITargetLocator

  (window-handle [driver]
    (init-window-handle (:webdriver driver)
                        (.getWindowHandle (:webdriver driver))
                        (title (:webdriver driver))
                        (current-url (:webdriver driver))))

  (window-handles [driver]
    (let [current-handle (.getWindowHandle (:webdriver driver))
          all-handles (lazy-seq (.getWindowHandles (:webdriver driver)))
          handle-records (for [handle all-handles]
                           (let [b (switch-to-window (:webdriver driver) handle)]
                             (init-window-handle (:webdriver driver)
                                                 handle
                                                 (title b)
                                                 (current-url b))))]
      (switch-to-window (:webdriver driver) current-handle)
      handle-records))

  (other-window-handles [driver]
    (remove #(= (:handle %) (:handle (window-handle (:webdriver driver))))
            (doall (window-handles (:webdriver driver)))))

  (switch-to-frame [driver frame]
    (.frame (.switchTo (:webdriver driver)) frame)
    (:webdriver driver))

  (switch-to-window [driver handle]
    ([driver handle]
       (cond
        (string? handle) (.window (.switchTo (:webdriver driver)) handle)
        (is-window-handle? handle) (.window (.switchTo (:driver handle)) (:handle handle))
        (number? handle) (switch-to-window (:webdriver driver) (nth (window-handles (:webdriver driver)) handle))
        (nil? handle) (throw (RuntimeException. "No window can be found"))
        :else (.window (.switchTo (:webdriver driver)) handle))))

  (switch-to-other-window [driver]
    (if (not= (count (window-handles (:webdriver driver))) 2)
      (throw (RuntimeException.
              (str "You may only use this function when two and only two "
                   "browser windows are open.")))
      (switch-to-window (:webdriver driver) (first (other-window-handles (:webdriver driver))))))

  (switch-to-default [driver]
    (.defaultContent (.switchTo (:webdriver driver))))

  (switch-to-active [driver]
    (.activeElement (.switchTo (:webdriver driver))))


  ;;; Wait Functionality ;;;
  IWait

  (implicit-wait [driver timeout]
    (.implicitlyWait (.. (:webdriver driver) manage timeouts) timeout TimeUnit/MILLISECONDS))

  (wait-until [driver pred timeout interval]
    (let [wait (WebDriverWait. (:webdriver driver) (/ timeout 1000) interval)]
    (.until wait (proxy [ExpectedCondition] []
                   (apply [d] (pred d))))))


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
          all-elements (find-elements (:webdriver driver) (by-xpath (str "//" (name tag))))] ; get all elements
      (if (= :text attr)
        (filter #(re-find value (text %)) all-elements)
        (filter (fn [el]
                  ((fnil (partial re-find value) "") ; `(attribute)` will return nil if the HTML element in question
                   (attribute el (name attr))))      ; doesn't support the attribute being passed in (e.g. :href on a <p>)
                all-elements))))

  (find-elements-by-regex [driver tag attr-val]
    (if (all-regex? attr-val)
      (let [elements (find-elements (:webdriver driver) (by-xpath "//*"))]
        (filter-elements-by-regex elements attr-val))
      (let [attr-vals-without-regex (into {}
                                          (remove
                                           #(let [[k v] %] (= java.util.regex.Pattern (class v)))
                                           attr-val))
            elements (find-them driver tag attr-vals-without-regex)]
        (filter-elements-by-regex elements attr-val))))

  (find-window-handles [driver attr-val]
    (if (contains? attr-val :index)
      [(nth (window-handles (:webdriver driver)) (:index attr-val))] ; vector for consistency below
      (filter #(every? (fn [[k v]] (if (= java.util.regex.Pattern (class v))
                                     (re-find v (k %))
                                     (= (k %) v)))
                       attr-val) (window-handles (:webdriver driver)))))

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
           (find-elements (:webdriver driver)))))

  (find-semantic-buttons-by-regex [driver attr-val]
    (let [attr-vals-without-regex (into {}
                                        (remove
                                         #(let [[k v] %] (= java.util.regex.Pattern (class v)))
                                         attr-val))
          elements (find-semantic-buttons (:webdriver driver) attr-vals-without-regex)]
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
        (find-elements (:webdriver driver) (by-xpath text-xpath)))))

  (find-table-cells [driver attr-val]
    (let [attr-val-map (apply hash-map attr-val)
          table-xpath (build-xpath :table (:table attr-val-map))
          row-xpath (str "//tr[" (inc (:row attr-val-map)) "]")
          col-xpath (if (and (find-element (:webdriver driver) (by-xpath (str table-xpath "//th")))
                             (zero? (:row attr-val-map)))
                      (str "/th[" (inc (:col attr-val-map)) "]")
                      (str "/td[" (inc (:col attr-val-map)) "]"))
          complete-xpath (str table-xpath row-xpath col-xpath)]
      (find-elements (:webdriver driver) (by-xpath complete-xpath))))

  (find-them* [driver attr-val]
    (cond
     (= attr-val :button*)   (find-them (:webdriver driver) :button* nil)
     (keyword? attr-val)     (find-elements
                              (:webdriver driver)
                              (by-tag-name (name attr-val))) ; supplied just :tag
     (vector? attr-val)      (cond
                              (some #{:row :col} attr-val) (find-table-cells (:webdriver driver) attr-val)
                              (query-with-ancestry-has-regex? attr-val) (if (query-with-ancestry-has-regex? (drop-last 2 attr-val))
                                                                          (throw (IllegalArgumentException.
                                                                                  (str "You may not pass in a regex until "
                                                                                       "the last attribute-value pair")))
                                                                          (filter-elements-by-regex
                                                                           (find-elements (:webdriver driver) (by-xpath (str (build-xpath-with-ancestry attr-val) "//*")))
                                                                           (last attr-val)))
                              :else (find-elements (:webdriver driver) (by-xpath (build-xpath-with-ancestry attr-val)))) ; supplied vector of queries in hierarchy
     (map? attr-val)         (find-them (:webdriver driver) :* attr-val)))
  (find-them* [driver tag attr-val]

    (when (keyword? (:webdriver driver)) ; I keep forgetting to pass in the WebDriver instance while testing
      (throw (IllegalArgumentException.
              (str "The first parameter to find-them must be an instance of WebDriver."))))
    (cond
     (and (> (count attr-val) 1)
          (contains? attr-val :xpath))          (find-them (:webdriver driver) :* {:xpath (:xpath attr-val)})
          (and (> (count attr-val) 1)
               (contains? attr-val :css))            (find-them (:webdriver driver) :* {:css (:css attr-val)})
               (contains? attr-val :tag-name)             (find-them (:webdriver driver)
                                                                     (-> (:tag-name attr-val)
                                                                         .toLowerCase
                                                                         keyword)
                                                                     (dissoc attr-val :tag-name))
               (contains? attr-val :index)                (find-elements (:webdriver driver) (by-xpath (build-xpath tag attr-val)))
               (= tag :radio)                             (find-them (:webdriver driver) :input (assoc attr-val :type "radio"))
               (= tag :checkbox)                          (find-them (:webdriver driver) :input (assoc attr-val :type "checkbox"))
               (= tag :textfield)                         (find-them (:webdriver driver) :input (assoc attr-val :type "text"))
               (= tag :password)                          (find-them (:webdriver driver) :input (assoc attr-val :type "password"))
               (= tag :filefield)                         (find-them (:webdriver driver) :input (assoc attr-val :type "file"))
               (and (= tag :input)
                    (contains? attr-val :type)
                    (or (= "radio" (:type attr-val))
                        (= "checkbox" (:type attr-val)))
                    (or (contains? attr-val :text)
                        (contains? attr-val :label)))     (find-checkables-by-text (:webdriver driver) attr-val)
                        (= tag :window)                            (find-window-handles (:webdriver driver) attr-val)
                        (= tag :button*)                           (if (contains-regex? attr-val)
                                                                     (find-semantic-buttons-by-regex (:webdriver driver) attr-val)
                                                                     (find-semantic-buttons (:webdriver driver) attr-val))
                        (= 1 (count attr-val))                     (let [entry (first attr-val)
                                                                         attr  (key entry)
                                                                         value (val entry)]
                                                                     (cond
                                                                      (= :xpath attr) (find-elements (:webdriver driver) (by-xpath value))
                                                                      (= :css attr)   (find-elements (:webdriver driver) (by-css-selector value))
                                                                      (= java.util.regex.Pattern (class value)) (find-elements-by-regex-alone (:webdriver driver) tag attr-val)
                                                                      :else           (find-elements (:webdriver driver) (by-attr= tag attr value))))
                        (contains-regex? attr-val)                 (find-elements-by-regex (:webdriver driver) tag attr-val)
                        :else                                      (find-elements (:webdriver driver) (by-xpath (build-xpath tag attr-val)))))
  (find-them [driver attr-val]
    (let [elts (find-them* driver attr-val)]
      (if-not (seq elts)
        (throw (NoSuchElementException.
                (str "No element with attributes "
                     attr-val " "
                     "could be found on the page:\n"
                     (page-source (:webdriver driver)))))
        elts)))
  (find-them [driver tag attr-val]
    (let [elts (find-them* driver tag attr-val)]
      (if-not (seq elts)
        (throw (NoSuchElementException.
                (str "No element with tag "
                     tag " and attributes "
                     attr-val " "
                     "could be found on the page:\n"
                     (page-source (:webdriver driver)))))
        elts)))
  (find-it [driver attr-val]
    (first (find-them (:webdriver driver) attr-val)))
  (find-it [driver tag attr-val]
    (first (find-them (:webdriver driver) tag attr-val))))


(defn init-driver
  "Constructor for Driver records"
  ([] (Driver. nil nil nil))
  ([webdriver] (Driver. webdriver nil nil))
  ([webdriver cs] (Driver. webdriver cs nil))
  ([webdriver cs mws] (Driver. webdriver cs mws)))

(defn is-driver?
  "Function to check class of a Driver, to prevent needing to import it"
  [driver]
  (= (class driver) Driver))