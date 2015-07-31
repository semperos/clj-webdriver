(ns clj-webdriver.test.example-app
  (:require [net.cgrand.moustache :refer [app]]
            [net.cgrand.enlive-html :refer [content defsnippet deftemplate any-node]]
            [ring.util.response :refer [response]]))

(deftemplate page "page.html" [styles scripts cnt]
  [:div#content] (content cnt))

(def any [:body :> any-node])

(defsnippet welcome-page "welcome.html" any [])
(defsnippet clojure-page "clojure.html" any [])
(defsnippet example-form "form.html" any [])
(defsnippet javascript-playground-page "javascript.html" any [])
(defsnippet admin-page "admin.html" any [])

(defn view-page
  [page-fn]
  (fn [_] (response (page nil nil (page-fn)))))

(def view-frontpage (view-page welcome-page))
(def view-clojure-page (view-page clojure-page))
(def view-example-form (view-page example-form))
(def view-javascript-playground (view-page javascript-playground-page))
(def view-admin-page (view-page admin-page))

(def routes
  (app
   [""] view-frontpage
   ["clojure"] view-clojure-page
   ["example-form"] view-example-form
   ["js-playground"] view-javascript-playground
   ["admin" &] view-admin-page))
