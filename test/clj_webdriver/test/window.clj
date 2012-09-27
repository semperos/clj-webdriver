(ns clj-webdriver.test.window
		(:use [clojure.test]
			  [clj-webdriver.taxi :only [new-driver to quit]]
        [clj-webdriver.test.config :only [test-base-url]]
			  [clj-webdriver.window]))

(def firefox-driver (atom nil))

;; Fixtures
(defn start-browser-fixture
  [f]
  (reset! firefox-driver
          (new-driver {:browser :firefox}))
  (f))

(defn reset-browser-fixture
  [f]
  (to @firefox-driver test-base-url)
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit @firefox-driver))

(use-fixtures :once start-browser-fixture quit-browser-fixture)
(use-fixtures :each reset-browser-fixture)

(deftest test-browser-size-management-functions
	(let [small {:width 460 :height 800}
        large {:width 1024 :height 800}]
    (resize! @firefox-driver small)
    (is (= (size @firefox-driver) small))
    (resize! @firefox-driver large)
    (is (= (size @firefox-driver) large))))

(deftest test-browser-window-position-functions
  (let [new-position {:x 245 :y 245}
        origin {:x 0 :y 22}]
    (reposition! @firefox-driver origin)
    (is (= (position @firefox-driver) origin))
    (reposition! @firefox-driver new-position)
    (is (= (position @firefox-driver) new-position))))

(deftest test-maximizing-the-browser-window-function
  (maximize! @firefox-driver)
  (is (= (position @firefox-driver) {:x 0 :y 22})))