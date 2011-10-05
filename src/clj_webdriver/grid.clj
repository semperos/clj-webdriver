;; ## Grid Support ##
;;
;; In order to use the functions in this namespace, you need to have downloaded
;; the standalone jar for Selenium-WebDriver and followed the instructions
;; at http://code.google.com/p/selenium/wiki/Grid2 to get a hub and child
;; nodes running.
;;
;; Once you have the Grid in place and configured properly, the only thing
;; that should differ about your test runs is that you get your instance of
;; WebDriver via these functions, which will delegate the task to the Grid
;; hub instead of running things locally. The function `new-driver-on-node` is
;; a Grid-based replacement for `clj-webdriver.core/new-driver`, just as
;; `start-on-node` is a Grid-based replacement for `clj-webdriver.core/start`.
;;
(ns clj-webdriver.grid
  (:use [clj-webdriver.core :only [get-url]]
        [clj-webdriver.util :only [call-method]])
  (:import org.openqa.selenium.remote.DesiredCapabilities
           org.openqa.selenium.remote.RemoteWebDriver))

(defn new-driver-on-node
  "Start a new RemoteWebDriver on a node managed by a Grid hub at `hub-url` using `browser` to run the test.

   If only a `browser` is supplied, this function assumes the hub is running locally and will use 'http://localhost:4444/wd/hub' as the value for `hub-url`."
  ([browser] (new-driver-on-node "http://localhost:4444/wd/hub" browser))
  ([hub-url browser]
     (RemoteWebDriver. (java.net.URL. hub-url),
                       (call-method DesiredCapabilities browser nil nil))))

(defn start-on-node
  "Convenience function for starting a new RemoteWebDriver on a Grid node managed by a Grid hub at `hub-url`, running tests with the given `browser` and `start-url`.

   If only a `browser` and `start-url` are supplied, this function assumes the hub is running locally and will use 'http://localhost:4444/wd/hub' as the value for `hub-url`."
  ([browser start-url]
     (let [driver (new-driver-on-node "http://localhost:4444/wd/hub" browser)]
       (get-url driver start-url)
       driver))
  ([hub-url browser start-url]
     (let [driver (new-driver-on-node hub-url browser)]
       (get-url driver start-url)
       driver)))