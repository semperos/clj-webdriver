(ns ^{:doc "Isn't it time we started testing `clj-webdriver.util` ?"}
  clj-webdriver.test.util-test
  (:use clojure.test
        clj-webdriver.util))

;; Functions to test:
;;
;; * build-css-attrs
;; * build-xpath-attrs
;; * build-css-with-hierarchy
;; * build-xpath-with-hierarchy
;; * build-query

(deftest test-contains-regex?
  (is (contains-regex? {:foo #"bar" :bar :boom}))
  (is (not (contains-regex? {:lang "clojure"})))
  (is (not (contains-regex? {}))))

(deftest test-all-regex?
  (is (all-regex? {:foo #"bar" :baz #"boom"}))
  (is (not (all-regex? {:foo #"bar" :baz "boom"})))
  (is (not (all-regex? {:lang "clojure"})))
  (is (not (all-regex? {}))))

(deftest test-filter-regex-entries
  ;; TODO: Research Pattern equality issue requiring only looking at keys here
  (is (= (keys (filter-regex-entries {:foo #"bar" :baz "boom"}))
         (keys {:foo #"bar"})))
  (is (= (filter-regex-entries {:lang "clojure"}) {}))
  (is (= (filter-regex-entries {}) {})))

(deftest test-remove-regex-entries
  (is (= (remove-regex-entries {:foo #"bar" :baz "boom"})
         {:baz "boom"}))
  (is (= (remove-regex-entries {:lang "clojure"})
         {:lang "clojure"}))
  (is (= (remove-regex-entries {}) {})))

(deftest test-first-n-chars
  (is (= (first-n-chars "foo" 2) "fo..."))
  (is (= (first-n-chars "foo" 10) "foo"))
  (is (= (first-n-chars "foo" 0) "...")))

(deftest test-elim-linebreaks
  (is (re-matches #"\s+" (elim-breaks "\n\r\n")))
  (is (re-matches #"foo bar\s+" (elim-breaks "foo bar\r\n"))))

(deftest test-read-config
  (let [conf (read-config "resources/properties.clj")]
    (is (map? conf))
    (is (contains? conf :this-file))
    (is (>= (count conf) 2))))

(deftest test-dashes-to-camel-case
  (let [f dashes-to-camel-case]
    (is (= (f "foo-bar-baz") "fooBarBaz"))
    (is (= (f "foo-bar-bazY") "fooBarBazY"))))

(deftest test-camel-case-to-dashes
  (let [f camel-case-to-dashes]
    (is (= (f "browserName") "browser-name"))
    (is (= (f "version") "version"))
    (is (= (f "trustAllSSLCertificates") "trust-all-sSL-certificates"))
    (is (= (f "wowzaSauceY") "wowza-sauceY"))))

(deftest test-clojure-keys
  (is (= (clojure-keys {"browserName" "firefox"
                        "version" 15
                        "trustAllSSLCertificates" true
                        "wowzaSauceY" false})
         {:browser-name "firefox"
          :version 15
          :trust-all-sSL-certificates true
          :wowza-sauceY false})))

(deftest test-java-keys
  (is (= (java-keys {:browser-name "firefox"
                     :version 15
                     :trust-all-SSL-certificates true
                     :wowza-sauceY false})
         {"browserName" "firefox"
          "version" 15
          "trustAllSSLCertificates" true
          "wowzaSauceY" false})))

(deftest test-clojure-java-keys-complimentary
  (let [c-map {:browser-name "firefox"
               :version 15
               :trust-all-sSL-certificates true
               :wowza-sauceY false
               :safari.clean-session true}
        j-map {"browserName" "firefox"
               "version" 15
               "trustAllSSLCertificates" true
               "wowzaSauceY" false
               "safari.cleanSession" true}]
    (is (= (clojure-keys
            (java-keys c-map))
           c-map))
    (is (= (java-keys
            (clojure-keys j-map))
           j-map))))

