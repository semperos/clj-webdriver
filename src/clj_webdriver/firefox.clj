(ns clj-webdriver.firefox
  (:require [clojure.java.io :as io])
  (:import org.openqa.selenium.firefox.FirefoxProfile))

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
  [profile ^Boolean bool]
  (.setAcceptUntrustedCertificates profile bool))

(defn enable-native-events
  "Set whether or not native events should be enabled (true by default on Windows, false on other platforms)."
  [profile ^Boolean bool]
  (.setEnableNativeEvents profile bool))