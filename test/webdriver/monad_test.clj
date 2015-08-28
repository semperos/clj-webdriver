(ns ^:manual-setup webdriver.monad-test
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
  (drive-with @driv (to *base-url*))
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
                    {:current-url current-url
                     :title title
                     :page-source page-source})
        [results driver] (test @driv)]
    (is (map? driver))
    (is (instance? WebDriver (:webdriver driver)))
    (is (= {:current-url *base-url*
            :title "Ministache"}
           (select-keys results [:current-url :title])))
    (is (re-find #"(?i)html>" (:page-source results)))))

(deftest back-forward-should-traverse-browser-history
  (let [test (drive (click "//a[text()='example form']")
                    ;; TODO Again raises question of monadic or not...
                    (wait-until (fn [d]
                                  (= (drive-with d
                                                 u <- (current-url)
                                                 u)
                                     (str *base-url* "example-form"))))
                    url-form <- (current-url)
                    (back)
                    url-orig <- (current-url)
                    (forward)
                    url-form2 <- (current-url)
                    {:url-form url-form
                     :url-orig url-orig
                     :url-form2 url-form2})]
    (is (= {:url-form (str *base-url* "example-form")
            :url-orig *base-url*
            :url-form2 (str *base-url* "example-form")}
           (first (test @driv))))))
