(ns clj-webdriver.options
  (:import org.openqa.selenium.Cookie))

(defprotocol IOptions
  "Options interface, including cookie and timeout handling"
  (add-cookie [driver cookie] "Add a new cookie to the browser session")
  (delete-cookie-named [driver cookie] "Delete a cookie given its name")
  (delete-cookie [driver cookie] "Delete a cookie given a cookie instance")
  (delete-all-cookies [driver] "Delete all cookies defined in the current session")
  (cookies [driver] "Retrieve a set of cookies defined in the current session")
  (cookie-named [driver name] "Retrieve a cookie object given its name"))

(defn new-cookie
  "Create a new cookie instance"
  ([name value] (new-cookie name value "/" nil))
  ([name value path] (new-cookie name value path nil))
  ([name value path date] (new Cookie name value path date)))

(defn cookie-name
  "Retrieve the name of a particular cookie"
  [cookie]
  (.getName cookie))

(defn cookie-value
  "Retrieve the value of a particular cookie"
  [cookie]
  (.getValue cookie))