(ns clj-webdriver.protocol.target-locator)

(defprotocol ITargetLocator
  "Functions that deal with browser windows and frames"
  (window-handle [driver] "Get the only (or first) window handle, return as a WindowHandler record")
  (window-handles [driver] "Retrieve a vector of `WindowHandle` records which can be used to switchTo particular open windows")
  (other-window-handles [driver] "Retrieve window handles for all windows except the current one")
  (switch-to-frame [driver frame] "Switch focus to a particular HTML frame")
  (switch-to-window [driver handle] "Switch focus to a particular open window")
  (switch-to-other-window [driver] "Given that two and only two browser windows are open, switch to the one not currently active")
  (switch-to-default [driver] "Switch focus to the first first frame of the page, or the main document if the page contains iframes")
  (switch-to-active [driver] "Switch to element that currently has focus, or to the body if this cannot be detected"))
