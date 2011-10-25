(ns clj-webdriver.protocols.wait)

(defprotocol IWait
  "Implicit and explicit waiting"
  (implicit-wait [driver timeout] "Specify the amount of time the `driver` should wait when searching for an element if it is not immediately present. This setting holds for the lifetime of the driver across all requests. Units in milliseconds.")
  (wait-until
    [driver pred]
    [driver pred timeout]
    [driver pred timeout interval] "Set an explicit wait time `timeout` for a particular condition `pred`. Optionally set an `interval` for testing the given predicate. All units in milliseconds"))