;; A namespace dedicated to defrecord's

(ns clj-webdriver.window-handle)

(defrecord WindowHandle [driver handle title url])

(defn init-window-handle
  [driver handle title url]
  (WindowHandle. driver handle title url))

(defn window-handle?
  [handle]
  (= (class handle) WindowHandle))