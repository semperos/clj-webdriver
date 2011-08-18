;; ## Form Helpers ##
;;
;; The functions in this namespace are designed to make working with HTML forms
;; faster and more intuitive for "common" use-cases.
;;
(ns clj-webdriver.form-helpers
  (:use [clj-webdriver.core :only [find-them, input-text]]))

(defn quick-fill
  "driver              - browser driver
   query-action-maps   - a seq of maps of queries to actions (queries find HTML elements, actions are fn's that act on them)
   opts                - extra options"
  ([driver query-action-maps] (quick-fill driver query-action-maps {:auto-submit true, :form-id nil}))
  ([driver query-action-maps opts]
     (for [entries query-action-maps,
           [k v] entries]
       ;; provide shortcut, if `k` is a string,
       ;; then assume it refers to an element's id
       (let [query-map (if (string? k)
                                {:id k}
                                k)
             action (if (string? v)
                      #(input-text % v)
                      v)
             target-els (find-them driver query-map)]
         (apply action target-els)))))
