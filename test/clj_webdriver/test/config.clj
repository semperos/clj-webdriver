(ns clj-webdriver.test.config)

;; ## Setup ##
(def test-port 5744)
(def test-host "localhost")
(def test-base-url (str "http://" test-host ":" test-port "/"))

(defn base-url
  "Allow overriding base URL for testing from command line."
  []
  (get (System/getenv) "TEST_BASE_URL" test-base-url))