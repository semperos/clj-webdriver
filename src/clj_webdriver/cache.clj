;; ## Element Caching ##
;;
;; Due to how Selenium-WebDriver deals with elements on a page,
;; even if the same element is queried in an identical way,
;; there is no way to test the equivalence of the element. A new
;; object is created every time an element is "discovered" on the page.
;;
;; The PageObject helper in Selenium-WebDriver attacks this problem by
;; providing a mechanism that sets up a proxy based on "how" you want
;; to find elements on the page. You determine this "how" at the class level,
;; which provides Selenium-WebDriver a means of uniquely identifying a
;; given element and allows it to cache the result based on how you
;; queried for it.
;;
;; While this is understandable, this removes all the flexibility of WebDriver's
;; page querying methods. So instead, this namespace utilizes a simple
;; system for caching elements on the page while still allowing you to use
;; clj-webdriver's find-* functions seemlessly.
;;
;; If you've enabled caching for your Driver and provided a set of cache rules
;; for the cache to abide by, when you call a find-* function, the cache
;; system makes the following checks:
;;
;;  * Is this find-* request on the same page as the previous? If a new page is loaded, the cache is reset anyway.
;;  * Is this find-* request already in the cache? If so, return it. If not, find element on page.
;;  * Once element is found, does this element conform to a cache rule? If so, cache it.
;;
;; Cache rules can be based on element-level attributes or based on the query
;; used to acquire the element. The cache rules are defined as a vector of functions
;; (for element-based rules) or maps (for query-based rules)
;;
;;     {:include [ (fn [element] (= (attribute element class) "foo"))
;;                 {:css "ul.menu a"} ],
;;      :exclude [ {:query [:div {:id "content"}, :a {:class "external"}]} ]}
;;
;; For the sake of API sanity, if you create a cache rule based on css, xpath,
;; or ancestry-based queries, the match must be exact (whitespace excluded).
;; Cache rules are evaluated in order, so put most-frequently-used cache rules
;; at the beginning of the vector. Includes are evaluated before excludes.
;;

(ns clj-webdriver.cache
  (:import clj_webdriver.core.Driver))

(defprotocol IElementCache
  "Cache for WebElement objects over the lifetime of a Driver on a given page"
  (cache-enabled? [driver] "Determine if caching is enabled for this record")
  (cacheable? [driver query] "Based on the driver's cache rules, determine if the given query is allowed to be cached")
  (in-cache? [driver query] "Check if cache contains an element")
  (insert [driver query value] "Insert a value into the cache")
  (retrieve [driver query] "Retrieve an element from the cache")
  (delete [driver query] "Delete the cached value at `query`")
  (seed
    [driver]
    [driver seed-value] "Replace all contents of cache with `seed-value`"))

(extend-type Driver

  IElementCache
  (cache-enabled? [driver]
    (boolean (get-in driver [:cache-specs :strategy])))
  (in-cache? [driver query]
    (contains? @(:element-cache driver) query))
  (insert [driver query value]
    (swap! (:element-cache driver) assoc query value))
  (retrieve [driver query]
    (get @(:element-cache driver) query))
  (delete [driver query]
    (swap! (:element-cache driver) dissoc query))
  (seed
    ([driver]
       (reset! (:element-cache driver) {}))
    ([driver seed-value]
       (reset! (:element-cache driver) seed-value)))
  (cacheable? [driver query]
    (let [rules (dissoc (:cache-specs driver) :args)])))
