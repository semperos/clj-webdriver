(ns clj-webdriver.chrome
  (:require [clj-webdriver.properties :refer [*properties*]]
            [clojure.java.io :as io])
  (:import (org.openqa.selenium.chrome ChromeOptions)
           (java.io.File)))

(defn new-chrome-options
  "Create an instance of ChromeOptions"
  []
  (ChromeOptions.))

(defn crx?
  [extension]
  (re-matches #".*\.crx$" extension))

(defn enable-extension
  "Given a `ChromeOptions` object, enable an extension.

   The `extension` parameter should either be (1) a File object pointing to a packed
   `.crx` extension file, (2) a String representation of the path to an extension
   (either a packed `.crx` file or an unpacked directory), or (3) a keyword like
   `:adblockplus` which, by convention, will make clj-webdriver check an
   environment variable `CHROME_EXTENSION_ADBLOCKPLUS`, hence `CHROME_EXTENSION_`
   plus the name of the plugin (keyword to string, dashes to underscores and uppercase)

   For more info, see https://sites.google.com/a/chromium.org/chromedriver/extensions"
  [options extension]
  (if (instance? java.io.File extension)
    (.addExtensions options extension)
    (let [property (keyword (str "CHROME_EXTENSION_"
                                 (-> extension name (.replace "-" "_") .toUpperCase)))
          ext-path (if (keyword? extension)
                     (if (not (empty? *properties*))
                       (*properties* property)
                       (System/getenv (name property)))
                     extension)]
      (if (crx? ext-path)
        (.addExtensions options (io/file ext-path))
        (.addArguments options [(str "load-extension=" ext-path)])))))
