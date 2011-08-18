;; ## Form Helpers ##
;;
;; The functions in this namespace are designed to make working with HTML forms
;; faster and more intuitive for "common" use-cases.
;;
(ns clj-webdriver.form-helpers
  (:use [clj-webdriver.core :only [find-them, input-text]]))

(defn quick-fill
  "`driver`              - browser driver
   `query-action-maps`   - a seq of maps of queries to actions (queries find HTML elements, actions are fn's that act on them)

   Example usage:
   (quick-fill a-driver
     [{\"first_name\" \"Rich\"}
      {{:class \"foobar\"} click}])"
  ([driver query-action-maps]
     (for [entries query-action-maps,
           [k v] entries]
       ;; shortcuts:
       ;; k as string => element's id attribute
       ;; v as string => text to input
       (let [query-map (if (string? k)
                                {:id k}
                                k)
             action (if (string? v)
                      #(input-text % v)
                      v)
             target-els (find-them driver query-map)]
         (apply action target-els)))))
