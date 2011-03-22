(ns clj-webdriver.test.example-app.templates
  (:use net.cgrand.enlive-html))

(deftemplate page "page.html" [styles scripts cnt]
  [:div#content] (content cnt))

(defsnippet welcome-page "welcome.html" [:body :> any-node] [])

(defsnippet clojure-page "clojure.html" [:body :> any-node] [])

(defsnippet example-form "form.html" [:body :> any-node] [])