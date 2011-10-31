;; ## Form Helpers ##
;;
;; The functions in this namespace are designed to make working with HTML forms
;; faster and more intuitive for "common" use-cases.
;;
(ns clj-webdriver.form-helpers
  (:use [clj-webdriver.core :only [input-text]]
        [clj-webdriver find])
  (:import clj_webdriver.driver.Driver))

(defn- quick-fill*
  [driver entries k v submit?]
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
    (if submit?
      (doseq [el target-els]
        (action el))
      (apply action target-els))))

(defprotocol IFormHelper
  "Useful functions for dealing with HTML forms"
  (quick-fill
    [driver query-action-maps]
    [driver query-action-maps submit?]
    "`driver`              - browser driver
   `query-action-maps`   - a seq of maps of queries to actions (queries find HTML elements, actions are fn's that act on them)
   `submit?`             - (WARNING: CHANGES RETURN TYPE) boolean, whether or not the call to this function will submit the form in question

   Example usage:
   (quick-fill a-driver
     [{\"first_name\" \"Rich\"}
      {{:class \"foobar\"} click}])"))

(extend-type Driver
  IFormHelper
  (quick-fill
    ([driver query-action-maps] (quick-fill driver query-action-maps false))
    ([driver query-action-maps submit?]
       (if submit?
         (doseq [entries query-action-maps,
                 [k v] entries]
           (quick-fill* driver entries k v submit?))
         (for [entries query-action-maps,
               [k v] entries]
           (quick-fill* driver entries k v submit?))))))
