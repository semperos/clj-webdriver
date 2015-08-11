(ns clj-webdriver.driver
  (:import org.openqa.selenium.interactions.Actions))

(defrecord Driver [webdriver capabilities actions])

(defn init-driver
  "Constructor for Driver records. Accepts either an existing WebDriver instance, or a `driver-spec` map with the following keys:

   webdriver - WebDriver instance"
  ([] (init-driver {}))
  ([driver-spec]
     (let [wd-class (Class/forName "org.openqa.selenium.WebDriver")
           uppers (supers (.getClass driver-spec))]
       (if (some #{wd-class} uppers)
         (Driver. driver-spec
                  nil
                  (Actions. driver-spec))
         (let [{:keys [webdriver capabilities]} driver-spec]
           (Driver. webdriver
                    capabilities
                    (Actions. webdriver)))))))

(defn driver?
  "Function to check class of a Driver, to prevent needing to import it"
  [driver]
  (= (class driver) Driver))
