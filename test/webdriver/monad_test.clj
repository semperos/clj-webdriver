(ns webdriver.monad-test
  (:require [clojure.test :refer :all]
            [webdriver.monad :refer :all]
            [webdriver.test.helpers :refer :all]
            [webdriver.core :as wd])
  (:import org.openqa.selenium.WebDriver
           org.openqa.selenium.firefox.FirefoxDriver))

(def driv (atom nil))

(defn restart-browser
  [f]
  (when-not @driv
    (reset! driv (driver (FirefoxDriver.))))
  ((drive
    (to *base-url*)
    :done) @driv)
  (f))

(defn quit-browser
  [f]
  (f)
  (wd/quit (:webdriver @driv)))

(use-fixtures :once start-system! stop-system! quit-browser)
(use-fixtures :each restart-browser)

(defmacro pass?
  "Final check on test passing, based on conventional use of monadic computations in this test suite."
  [test]
  `(let [[result# driver#] (~test @driv)]
     (when (instance? Throwable result#)
       (print (format-history (:history driver#))))
     (is (= :pass result#))))

(deftest browser-basics
  (let [test (drive
              current-url <- (current-url)
              title <- (title)
              page-source <- (page-source)
              (identity-map current-url title page-source))
        [results driver] (test @driv)]
    (is (map? driver))
    (is (instance? WebDriver (:webdriver driver)))
    (is (= {:current-url *base-url*
            :title "Ministache"}
           (select-keys results [:current-url :title])))
    (is (re-find #"(?i)html>" (:page-source results)))))

(deftest back-forward-should-traverse-browser-history
  (let [test (drive (click "//a[text()='example form']")
                    (wait-until (drive
                                 url <- (current-url)
                                 (return
                                  (= url (str *base-url* "example-form")))))
                    url-form <- (current-url)
                    (is-m (= (str *base-url* "example-form") url-form))
                    (back)
                    url-orig <- (current-url)
                    (is-m (= *base-url* url-orig))
                    (forward)
                    url-form2 <- (current-url)
                    (is-m (= (str *base-url* "example-form") url-form2))
                    :pass)]
    (pass? test)))

(deftest test-to
  (let [test (drive
              (to (str *base-url* "example-form"))
              url <- (current-url)
              title <- (title)
              [url title])
        ;; Example of simply pulling out values
        [[url title]] (test @driv)]
    (is (= (str *base-url* "example-form") url))
    (is (= "Ministache" title))))

(deftest test-find-by-and-attributes
  ;; Get to example form page
  ((drive
    (click {:tag :a, :text "example form"})
    (wait-until (drive
                 el <- (find-element-by (by-id "first_name"))
                 el))
    :done) @driv)
  ;; Exercise both `attribute` and different `by-*` types
  ;; TODO Bad test if both values are same.
  (let [test (drive
              id <- (attribute (by-name "first_name") :id)
              (is-m (= "first_name" id))
              name <- (attribute (by-id "first_name") :name)
              (is-m (= "first_name" name))
              :pass)]
    (pass? test))
  (let [home (drive
              link-text-full <- (text (by-link-text "home"))
              link-text-xpath <- (text (by-xpath "//a[text()='home']"))
              link-text-tag <- (text (by-tag "a"))
              link-text-class <- (text (by-class-name "menu-item"))
              link-text-css <- (text (by-css-selector "#footer a.menu-item"))
              [link-text-full link-text-xpath link-text-tag link-text-class link-text-css])
        [results] (home @driv)]
    (= (take 5 (repeat "home"))
       results)))

(deftest test-find-by-and-attributes-part-2
  ((drive
    (click {:tag :a, :text "example form"})
    (wait-until (drive
                 el <- (find-element-by (by-id "first_name"))
                 el))
    :done) @driv)
  (let [test (drive
              partial-text <- (text (by-partial-link-text "example"))
              by-contains <- (attribute (by-attr-contains :option :value "cial_")
                                        :value)
              by-starts <- (attribute (by-attr-starts :option :value "social_")
                                      :value)
              by-ends <- (attribute (by-attr-ends :option :value "_media") :value)
              by-has <- (attribute (by-has-attr :option :value) :value)
              by-class <- (attribute (by-class-name "first odd") :class)
              (are-m [x y] (= x y)
                     partial-text "example form"
                     by-contains "social_media"
                     by-starts "social_media"
                     by-ends "social_media"
                     by-has "france"
                     by-class "first odd")
              :pass)]
    (pass? test)))
