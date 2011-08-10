;;; ## Form Helpers ##
;;;
;;; The functions in this namespace are designed to make working with HTML forms
;;; faster and more intuitive for "common" use-cases.
;;;
(ns clj-webdriver.form-helpers)

(defn quick-fill
  ([fields-to-vals] (quick-fill fields-to-vals {:auto-submit true}))
  ([fields-to-vals opts]
     ;; run through fields-to-vals entries
     ;; do intuitive things for field type (type into a text field, click a radio/checkbox, select a select dropdown, etc
     ))