(ns clj-webdriver.firefox
  (:import org.openqa.selenium.firefox.FirefoxProfile))

;; TODO: Provide convenient way to pass in a profile, e.g. to Firefox driver,
;; to enable add-ons when needed
(defn new-profile
  "Create an instance of `FirefoxProfile`"
  []
  (FirefoxProfile.))

(defn enable-extension
  "Given a `FirefoxProfile` object, enable an extension located at `extension-location`"
  [profile extension-location]
  (let [ext-file (io/as-file extension-location)]
    (.addExtension profile ext-file)))

(defn set-preference
  "Given a `FirefoxProfile` object, set a preference with key `pref-k` to value `pref-v`"
  [profile pref-k pref-v]
  (.setPreference profile pref-k pref-v))

(defn accept-untrusted-certs
  [profile ^Boolean bool]
  (.setAcceptUntrustedCertificates profile bool))

(defn enable-native-events
  [profile ^Boolean bool]
  (.setEnableNativeEvents profile bool))