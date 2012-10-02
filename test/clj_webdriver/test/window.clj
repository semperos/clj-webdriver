(ns clj-webdriver.test.window
  (:use [clojure.test]
        [clj-webdriver.taxi :only [new-driver to quit]]
        [clj-webdriver.test.config :only [test-base-url]]
        [clj-webdriver.window]))

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

(deftest test-browser-window-size-functions
  (let [small {:width 460 :height 800}
        large {:width 1024 :height 800}]
    (resize @driver small)
    (is (= (size @driver) small))
    (resize @driver large)
    (is (= (size @driver) large))))

(deftest test-browser-window-position-functions
  (let [new-position {:x 245 :y 245}
        origin (position @driver)]
    (reposition @driver new-position)
    (is (= (position @driver) new-position))
    (reposition @driver origin)
    (is (= (position @driver) origin))))

(deftest test-browser-window-maximizing-function
  (let [orig-size (size @driver)
        max-size (size (maximize @driver))]
    (are [max orig] (is (> max orig))
         (:width max-size) (:width orig-size)
         (:height max-size) (:height orig-size))))