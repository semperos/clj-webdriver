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

(deftest browser-basics
  (let [test (drive current-url <- (current-url)
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
                    (back)
                    url-orig <- (current-url)
                    (forward)
                    url-form2 <- (current-url)
                    (identity-map url-form url-orig url-form2))]
    (let [result (test @driv)]
      (is (= {:url-form (str *base-url* "example-form")
              :url-orig *base-url*
              :url-form2 (str *base-url* "example-form")}
             (first result))))))

(deftest to-should-open-given-url-in-browser
  (let [test (drive
              (to (str *base-url* "example-form"))
              url <- (current-url)
              title <- (title)
              [url title])
        [[url title]] (test @driv)]
    (is (= (str *base-url* "example-form") url))
    (is (= "Ministache" title))))

(deftest should-be-able-to-find-element-bys-using-low-level-by-wrappers
  ;; Get to example form page
  ((drive
    (click {:tag :a, :text "example form"})
    (wait-until (drive
                 el <- (find-element-by (by-id "first_name"))
                 el))
    :done) @driv)
  ;; Exercise both `attribute` and different `by-*` types
  (let [first-name (drive
                    id <- (attribute (by-name "first_name") :id)
                    name <- (attribute (by-id "first_name") :name)
                    (identity-map id name))
        [results] (first-name @driv)]
    ;; TODO Bad test if both values are same.
    (is (= "first_name" (:id results)))
    (is (= "first_name" (:name results))))
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
;; example-form-text <- (text (by-partial-link-text "example"))
;;   (is (= "example form"
;;          (text (find-element-by driver (by-partial-link-text "example")))))
;;   (is (= "social_media"
;;          (attribute (find-element-by driver (by-attr-contains :option :value "cial_")) :value)))
;;   (is (= "social_media"
;;          (attribute (find-element-by driver (by-attr-starts :option :value "social_")) :value)))
;;   (is (= "social_media"
;;          (attribute (find-element-by driver (by-attr-ends :option :value "_media")) :value)))
;;   (is (= "france"
;;          (attribute (find-element-by driver (by-has-attr :option :value)) :value)))
;;   (to driver *base-url*)
;;   (is (= "first odd"
;;          (attribute (find-element-by driver (by-class-name "first odd")) :class)))
