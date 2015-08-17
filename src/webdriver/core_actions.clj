(in-ns 'webdriver.core)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Functions for Actions Class ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: test coverage
(defmacro ->build-composite-action
  "Create a composite chain of actions, then call `.build()`. This does **not** execute the actions; it simply sets up an 'action chain' which can later by executed using `.perform()`.

   Unless you need to wait to execute your composite actions, you should prefer `->actions` to this macro."
  [driver & body]
  `(let [acts# (doto (Actions. (.webdriver ~driver))
                 ~@body)]
     (.build acts#)))

;; TODO: test coverage
(defmacro ->actions
  [driver & body]
  `(let [act# (Actions. (.webdriver ~driver))]
     (doto act#
       ~@body
       .perform)
     ~driver))

;; e.g.
;; Action dragAndDrop = builder.clickAndHold(someElement)
;;       .moveToElement(otherElement)
;;       .release(otherElement)
;;       .build()
