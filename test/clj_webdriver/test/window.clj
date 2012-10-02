(ns clj-webdriver.test.window
  (:use [clojure.test]
        [clj-webdriver.core :only [new-driver title current-url to quit]]
        [clj-webdriver.test.config :only [test-base-url]]
        clj-webdriver.window))

(def driver (atom nil))

;; Fixtures
(defn start-browser-fixture
  [f]
  (reset! driver
          (new-driver {:browser :firefox}))
  (f))

(defn reset-browser-fixture
  [f]
  (to @driver test-base-url)
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit @driver))

(use-fixtures :once start-browser-fixture quit-browser-fixture)
(use-fixtures :each reset-browser-fixture)

(defn test-window-size
  [this]
  (let [small {:width 460 :height 800}
        large {:width 1024 :height 800}]
    (resize this small)
    (is (= (size this) small))
    (resize this large)
    (is (= (size this) large))))

(defn test-window-position
  [this]
  (let [new-position {:x 245 :y 245}
        origin (position this)]
    (reposition this new-position)
    (is (= (position this) new-position))
    (reposition this origin)
    (is (= (position this) origin))))

(defn test-window-maximizing
  [this]
  (let [orig-size (size this)
        max-size (size (maximize this))]
    (are [max orig] (is (> max orig))
         (:width max-size) (:width orig-size)
         (:height max-size) (:height orig-size))))

(defn common-window-tests
  [this]
  (doseq [tst [test-window-size
               test-window-position
               test-window-maximizing]]
    (tst this)))

(deftest run-window-tests
  (common-window-tests @driver)
  ;; TODO: better test would be to open two windows
  ;; and pass in the second one here.
  (common-window-tests (init-window @driver
                                    (.getWindowHandle (:webdriver @driver))
                                    (title @driver)
                                    (current-url @driver))))