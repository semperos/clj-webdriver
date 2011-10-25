(ns clj-webdriver.find)

(defprotocol IFind
  "Functions used to locate elements on a given page"
  (find-element [driver by] "Retrieve the element object of an element described by `by`")
  (find-elements [driver by] "Retrieve a seq of element objects described by `by`")
  (find-elements-by-regex-alone [driver tag attr-val] "Given an `attr-val` pair with a regex value, find the elements that match")
  (find-elements-by-regex [driver tag attr-val])
  (find-window-handles [driver attr-val] "Given a browser `driver` and a map of attributes, return the WindowHandle that matches")
  (find-semantic-buttons [driver attr-val] "Find HTML element that is either a `<button>` or an `<input>` of type submit, reset, image or button")
  (find-semantic-buttons-by-regex [driver attr-val] "Semantic buttons are things that look or behave like buttons but do not necessarily consist of a `<button>` tag")
  (find-checkables-by-text [driver attr-val] "Finding the 'text' of a radio or checkbox is complex. Handle it here.")
  (find-table-cells [driver attr-val] "Given a WebDriver `driver` and a vector `attr-val`, find the correct")
  (find-them*
    [driver attr-val]
    [driver tag attr-val] "Given a browser `driver`, return the elements that match the query")
  (find-them
    [driver attr-val]
    [driver tag attr-val] "Call find-them*, then make sure elements are actually returned; if not, throw NoSuchElementException so other code can handle exceptions appropriately")
  (find-it
    [driver attr-val]
    [driver tag attr-val] "Call (first (find-them args))"))
