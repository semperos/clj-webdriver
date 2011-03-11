(ns clj-webdriver.test.example-app.core
  (:use net.cgrand.moustache
        clj-webdriver.test.example-app.templates
        [ring.util.response :only [response]]))

(defn view-frontpage
  [r]
  (->> (page nil nil (welcome-page))
       response))

(defn view-example-form
  [r]
  (->> (page nil nil (example-form))
       response))

(def routes
  (app
   [""] view-frontpage
   ["example-form"] view-example-form))