(ns clj-webdriver.firefox
  (:use [clj-webdriver.properties :only [*properties*]])
  (:require [clojure.java.io :as io])
  (:import org.openqa.selenium.firefox.FirefoxProfile))

(defn new-profile
  "Create an instance of `FirefoxProfile`"
  ([] (FirefoxProfile.))
  ([profile-dir] (FirefoxProfile. (io/file profile-dir))))

(defn enable-extension
  "Given a `FirefoxProfile` object, enable an extension.

   The `extension` parameter should either be (1) a File object pointing to an extension, (2) a String representation of the full path to an object, or (3) a keyword like `:firebug` which, by convention, will make clj-webdriver check an environment variable `FIREFOX_EXTENSION_FIREBUG`, hence `FIREFOX_EXTENSION_` plus the name of the plugin (keyword to string, dashes to underscores and uppercase)"
  [profile extension]
  (let [property (keyword (str "FIREFOX_EXTENSION_"
                               (-> extension
                                   name
                                   (.replace "-" "_")
                                   .toUpperCase)))
        ext-file (if (keyword? extension)
                   (if-not (empty? *properties*)
                     (io/file (*properties* property))
                     (io/file (System/getenv (name property))))
                   (io/file extension))]
    (.addExtension profile ext-file)))

(defn set-preferences
  "Given a `FirefoxProfile` object and a map of preferences, set the preferences for the profile."
  [profile pref-map]
  (doseq [entry pref-map]
    (.setPreference profile (name (key entry)) (val entry))))

(defn accept-untrusted-certs
  "Set whether or not Firefox should accept untrusted certificates."
  [profile bool]
  (.setAcceptUntrustedCertificates profile bool))

(defn enable-native-events
  "Set whether or not native events should be enabled (true by default on Windows, false on other platforms)."
  [profile bool]
  (.setEnableNativeEvents profile bool))

(defn is-running
  "Return true if the profile in the given `profile-dir` is the one running"
  [profile profile-dir]
  (.isRunning profile (io/file profile-dir)))

(defn write-to-disk
  "Write the given profile to disk. Makes sense when building up an anonymous profile via clj-webdriver."
  [profile]
  (.layoutOnDisk profile))

(defn json
  "Return JSON representation of the given `profile` (can be used to read the profile back in via `profile-from-json`"
  [profile]
  (.toJson profile))

(defn profile-from-json
  "Instantiate a new FirefoxProfile from a proper JSON representation."
  [^String json]
  (FirefoxProfile/fromJson json))
