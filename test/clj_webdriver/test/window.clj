(ns clj-webdriver.test.window
  (:use [clojure.test]
        [clj-webdriver.core :only [new-driver title current-url to quit]]
        [clj-webdriver.test.config :only [base-url]]
        clj-webdriver.window))

;; This part of the test suite is system-specific
;; and probably will not pass on your machine.

(def driver (atom nil))

;; Fixtures
(defn start-browser-fixture
  [f]
  (reset! driver
          (new-driver {:browser :firefox}))
  (f))

(defn reset-browser-fixture
  [f]
  (to @driver (base-url))
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit @driver))

(use-fixtures :once start-browser-fixture quit-browser-fixture)
(use-fixtures :each reset-browser-fixture)

(defn test-window-size
  [this]
  (let [small {:width 500 :height 400}
        large {:width 1024 :height 800}]
    (resize this small)
    ;; (is (= (size this) small))
    (resize this large)
    ;; (is (= (size this) large))
    ))

(defn test-window-resize-with-one-dimension
  [this]
  (let [orig-size (size this)
        small {:height 400}
        large {:width 1024}]
    (resize this small)
    ;; (is (= (:width (size this)) (:width orig-size)))
    (resize this orig-size)
    ;; (is (= (size this) orig-size))
    (resize this large)
    ;; (is (= (:height (size this)) (:height orig-size)))
    ))

(defn test-window-position
  [this]
  (let [origin (position this)
        new-position {:x 100 :y 245}]
    (reposition this new-position)
    ;; (is (= (position this) new-position))
    (reposition this origin)
    ;; (is (= (position this) origin))
    ))

(defn test-window-reposition-with-one-coordinate
  [this]
  (let [origin (position this)
        position-y {:y 245}
        position-x {:x 100}]
    (reposition this position-y)
    ;; (is (= (:x (position this)) (:x origin)))
    (reposition this origin)
    ;; (is (= (position this) origin))
    (reposition this position-x)
    ;; (is (= (:y (position this)) (:y origin)))
    ))

(defn test-window-maximizing
  [this]
  (let [orig-size (size (resize this {:width 300 :height 300}))
        max-size (size (maximize this))]
    ;; (is (> (:width max-size) (:width orig-size)))
    ;; (is (> (:height max-size) (:height orig-size)))
    ))

(defn common-window-tests
  [this]
  (doseq [tst [test-window-size
               test-window-resize-with-one-dimension
               test-window-position
               test-window-reposition-with-one-coordinate
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