(ns clj-webdriver.test.helpers
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty :refer [run-jetty]]
            [com.stuartsierra.component :as component]
            [clj-webdriver.test.example-app :as web-app])
  (:import java.io.File))

(def ^:const test-port 5744)

(defn base-url
  "Allow overriding base URL for testing from command line."
  []
  (get (System/getenv) "TEST_BASE_URL" (str "http://127.0.0.1:" test-port "/")))

;; System checks
(defn chromium-installed?
  []
  (.exists (File. "/usr/lib/chromium-browser/chromium-browser")))

(defn chromium-preferred?
  "If a Chromium installation can be detected and the `WEBDRIVER_USE_CHROMIUM` environment variable is defined, return true."
  []
  (log/info "Chromium installation detected. Using Chromium instead of Chrome.")
  (and (chromium-installed?)
       (get (System/getenv) "WEBDRIVER_USE_CHROMIUM")))

;; Utilities
(defmacro thrown?
  "Return truthy if the exception in `klass` is thrown, otherwise return falsey (nil) (code adapted from clojure.test)"
  [klass & forms]
  `(try ~@forms
        false
        (catch ~klass e#
          true)))

(defn exclusive-between
  "Ensure a number is between a min and a max, both exclusive"
  [n min max]
  (and (> n min)
       (< n max)))

(defrecord WebServerComponent [port]
  component/Lifecycle
  (start [component]
    (let [start-server (fn [] (run-jetty #'web-app/routes {:port port, :join? false}))]
      (if-let [server (:server component)]
        (if (.isRunning server)
          component
          (assoc component :server (start-server)))
        (assoc component :server (start-server)))))

  (stop [component]
    (when-let [server (:server component)]
      (.stop server))
    (dissoc component :server)))

(defn test-system
  "Return a system map that the component library can use."
  []
  (component/system-map
   :web (WebServerComponent. test-port)))

(def system (test-system))

(defn start-system! [f]
  (alter-var-root #'system component/start)
  (f))

(defn stop-system! [f]
  (f)
  (alter-var-root #'system component/stop))
