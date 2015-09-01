(ns webdriver.monad-test
  (:require [clojure.test :as t]
            [webdriver.monad :refer :all]
            [webdriver.test.helpers :refer :all]
            [clojure.java.io :as io]
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

(t/use-fixtures :once start-system! stop-system! quit-browser)
(t/use-fixtures :each restart-browser)

;;;;;;;;;;;;;;;;;;;;
;; Test Utilities ;;
;;;;;;;;;;;;;;;;;;;;

(defn pass?
  "For use with `defdrive`, ensures monad computation finishes to completion successfully."
  [test]
  (let [[result driver] (test @driv)]
    (when (instance? Throwable result)
      (print (format-history (:history driver))))
    (is (= :pass result))))

;;;;;;;;;;;
;; Tests ;;
;;;;;;;;;;;

(t/deftest browser-basics
  (let [test (drive
              current-url <- (current-url)
              title <- (title)
              page-source <- (page-source)
              (identity-map current-url title page-source))
        [results driver] (test @driv)]
    (t/is (map? driver))
    (t/is (instance? WebDriver (:webdriver driver)))
    (t/is (= {:current-url *base-url*
              :title "Ministache"}
             (select-keys results [:current-url :title])))
    (t/is (re-find #"(?i)html>" (:page-source results)))))

(defdrive back-forward-should-traverse-browser-history pass?
  (click "//a[text()='example form']")
  (wait-until (drive
               url <- (current-url)
               (return
                (= url (str *base-url* "example-form")))))
  url-form <- (current-url)
  (is (= (str *base-url* "example-form") url-form))
  (back)
  url-orig <- (current-url)
  (is (= *base-url* url-orig))
  (forward)
  url-form2 <- (current-url)
  (is (= (str *base-url* "example-form") url-form2)))

(t/deftest test-to
  (let [test (drive
              (to (str *base-url* "example-form"))
              url <- (current-url)
              title <- (title)
              [url title])
        ;; Example of simply pulling out values
        [[url title]] (test @driv)]
    (is (= (str *base-url* "example-form") url))
    (is (= "Ministache" title))))

(defdrive test-find-by-and-attributes pass?
  (click {:tag :a, :text "example form"})
  (wait-until (drive
               el <- (find-element-by (by-id "first_name"))
               el))
  id <- (attribute (by-name "first_name") :id)
  name <- (attribute (by-id "first_name") :name)
  link-text-full <- (text (by-link-text "home"))
  link-text-xpath <- (text (by-xpath "//a[text()='home']"))
  link-text-tag <- (text (by-tag "a"))
  link-text-class <- (text (by-class-name "menu-item"))
  link-text-css <- (text (by-css-selector "#footer a.menu-item"))
  (are [x y] (= x y)
    id "first_name"
    name "first_name"
    link-text-full "home"
    link-text-xpath "home"
    link-text-tag "home"
    link-text-class "home"
    link-text-css "home"))

(defdrive test-find-by-and-attributes-part-2 pass?
  (click {:tag :a, :text "example form"})
  (wait-until (drive
               el <- (find-element-by (by-id "first_name"))
               el))
  partial-text <- (text (by-partial-link-text "example"))
  by-contains <- (attribute (by-attr-contains :option :value "cial_")
                            :value)
  by-starts <- (attribute (by-attr-starts :option :value "social_")
                          :value)
  by-ends <- (attribute (by-attr-ends :option :value "_media") :value)
  by-has <- (attribute (by-has-attr :option :value) :value)
  (back)
  by-class <- (attribute (by-class-name "first odd") :class)
  (are [x y] (= x y)
    partial-text "example form"
    by-contains "social_media"
    by-starts "social_media"
    by-ends "social_media"
    by-has "france"
    by-class "first odd"))

(defdrive test-find-elements pass?
  links <- (find-elements {:tag :a})
  (is (= 10 (count links)))
  text-first <- (text (nth links 1))
  (is (= "Moustache" text-first))
  text-external <- (text {:class "external"})
  (is (= "Moustache" text-external))
  class-odd <- (attribute {:class "first odd"} :class)
  (is (= "first odd" class-odd))
  class-odd2 <- (attribute {:tag :li :class "first odd"} :class)
  (is (= "first odd" class-odd2))
  href <- (attribute {:text "Moustache"} :href)
  (is (= "https://github.com/cgrand/moustache" href))
  (click {:tag :a :text "example form"})
  (wait-until (drive
               (find-element {:type "text"})
               :done))
  id <- (attribute {:type "text"} :id)
  (is (= "first_name" id))
  id2 <- (attribute {:tag :input :type "text"} :id)
  (is (= "first_name" id2))
  id3 <- (attribute {:tag :input :type "text" :name "first_name"} :id)
  (is (= "first_name" id3)))

(defdrive test-hierarchical-queries pass?
  text-external <- (text [{:tag :div, :id "content"}, {:tag :a, :class "external"}])
  (is (= "Moustache" text-external))
  text-home <- (text [{:tag :*, :id "footer"}, {:tag :a}])
  (is (= "home" text-home))
  els <- (find-elements [{:tag :*, :id "footer"}, {:tag :a}])
  (is (= 5 (count els))))
