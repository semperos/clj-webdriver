(in-ns 'clj-webdriver.core)

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

(defn add-cookie
  "Add a new cookie to the browser session"
  [driver cookie]
  (.addCookie (.manage driver) cookie))

(defn delete-cookie-named
  "Delete a cookie given its name"
  [driver name]
  (.deleteCookieNamed (.manage driver) name))

(defn delete-cookie
  "Delete a cookie given a cookie instance"
  [driver cookie]
  (.deleteCookie (.manage driver) cookie))

(defn delete-all-cookies
  "Delete all cookies defined in the current session"
  [driver]
  (.deleteAllCookies (.manage driver)))

(defn cookies
  "Retrieve a set of cookies defined in the current session"
  [driver]
  (into #{} (.getCookies (.manage driver))))

(defn cookie-named
  "Retrieve a cookie object given its name"
  [driver name]
  (.getCookieNamed (.manage driver) name))