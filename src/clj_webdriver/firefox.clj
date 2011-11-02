(ns clj-webdriver.firefox
  (:use [clj-webdriver.core :only [new-webdriver*]]
        [clj-webdriver.driver :only [init-driver]])
  (:require [clojure.java.io :as io])
  (:import org.openqa.selenium.firefox.FirefoxProfile))

(defn new-firefox-driver
  "Create new Driver given a browser type. If an additional profile object or string is passed in, Firefox will be started with the given profile instead of the default.

   This is the preferred method for starting up a browser, as it leverages clj-webdriver-specific functionality not available with vanilla WebDriver instances. You can always access the underlying WebDriver instance with the :webdriver key of your Driver record."
  ([]
     (init-driver (new-webdriver* :firefox)))
  ([profile]
     (init-driver (new-webdriver* :firefox profile)))
  ([profile cache-spec]
     (init-driver (new-webdriver* :firefox profile) cache-spec))
  ([profile cache-spec cache-args]
     (init-driver (new-webdriver* :firefox profile) cache-spec cache-args)))

(defn new-profile
  "Create an instance of `FirefoxProfile`"
  []
  (FirefoxProfile.))

(defn enable-extension
  "Given a `FirefoxProfile` object, enable an extension located at `extension-location`."
  [profile extension-location]
  (let [ext-file (io/as-file extension-location)]
    (.addExtension profile ext-file)))

(defn set-preferences
  "Given a `FirefoxProfile` object and a map of preferences, set the preferences for the profile."
  [profile pref-m]
  (doseq [entry pref-m]
    (.setPreference profile (name (key entry)) (val entry))))

(defn accept-untrusted-certs
  "Set whether or not Firefox should accept untrusted certificates."
  [profile bool]
  (.setAcceptUntrustedCertificates profile bool))

(defn enable-native-events
  "Set whether or not native events should be enabled (true by default on Windows, false on other platforms)."
  [profile bool]
  (.setEnableNativeEvents profile bool))