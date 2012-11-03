(ns clj-webdriver.test.example-app.core
  (:use net.cgrand.moustache
        clj-webdriver.test.example-app.templates
        [ring.middleware.http-basic-auth :only [wrap-with-auth wrap-require-auth]]
        [ring.util.response :only [response]]))

(defn view-frontpage
  [r]
  (->> (page nil nil (welcome-page))
       response))

(defn view-clojure-page
  [r]
  (->> (page nil nil (clojure-page))
       response))

(defn view-example-form
  [r]
  (->> (page nil nil (example-form))
       response))

(defn view-javascript-playground
  [r]
  (->> (page nil nil (javascript-playground-page))
       response))

(defn view-admin-page
  [r]
  (->> (page nil nil (admin-page))
       response))

(defn authenticate [username password]
  (when (and (= username "webdriver")
           (= password "test"))
    {:username username}))

(def routes
  (app
   [""] view-frontpage
   ["clojure"] view-clojure-page
   ["example-form"] view-example-form
   ["js-playground"] view-javascript-playground
   ["admin" &] (app
                (wrap-with-auth authenticate)
                (wrap-require-auth authenticate
                                   "Admin Area"
                                   {:body "You must enter correct credentials to view this area."})
                [""] view-admin-page)))