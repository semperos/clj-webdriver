(ns clj-webdriver.protocol.driver-basics)

(defprotocol IDriverBasics
  "Basics of driver handling"
  (get-url [driver url] "Navigate the driver to a given URL")
  (to [driver url] "Navigate to a particular URL. Arg `url` can be either String or java.net.URL. Equivalent to the `get` function, provided here for compatibility with WebDriver API.")
  (current-url [driver] "Retrieve the URL of the current page")
  (title [driver] "Retrieve the title of the current page as defined in the `head` tag")
  (page-source [driver] "Retrieve the source code of the current page")
  (close [driver] "Close this browser instance, switching to an active one if more than one is open")
  (quit [driver] "Destroy this browser instance")
  (back [driver] "Go back to the previous page in \"browsing history\"")
  (forward [driver] "Go forward to the next page in \"browsing history\".")
  (refresh [driver] "Refresh the current page"))
