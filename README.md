# Selenium-WebDriver Support for Clojure

This is a Clojure wrapper around the Selenium-WebDriver library. Credits to [mikitebeka/webdriver-clj][webdriver-orig] for the initial code for this project and many of the low-level wrappers around the WebDriver API.

## Usage

Use/require the library in your code:

    (use 'clj-webdriver.core)

Start up a browser:

    (def b (start :firefox "https://github.com"))

At the moment, the best documentation is the source code itself. While there are many functions in the core namespace, they're mostly short and straightforward wrappers around WebDriver API's. For the task of finding elements on the page, I've added some utility functions at the end of the core namespace.

Here's an example of logging into Github:

    ;; Start the browser and bind it to `b`
    (def b (start :firefox "https://github.com"))
    
    ;; Click "Login" link
    (-> b
        (find-it {:text "Login"})
        click)
    
    ;; Input username/email into the "Login or Email" field
    (-> b
        (find-it {:class "text", :name "login"}) ; use multiple attributes
        (input-text "username"))
    
    ;; Input password into the "Password" field
    (-> b
        (find-it {:xpath "//input[@id='password']"}) ; even in "easy" mode, you still
        (input-text "password"))                     ; have :xpath and :css options
    
    ;; Click the "Log in" button"
    (-> b
        (find-it :input {:value #"(?i)log"}) ; use of regular expressions
        click)                         

The key functions for finding an element on the page are `find-it` and `find-them`. The `find-it` function returns the first result that matches the criteria, while `find-them` returns a vector of all matches for the given criteria. Both support the same syntax and set of attributes.

Here is an overview of the arguments you can pass these functions:

* *HTML Tag as keyword:* Pass in the name of an HTML tag as a keyword (`:div`, `:a`, `:span`, `:img`, etc.) `(find-it :a)` will find the first `<a>` tag on the page
* *HTML Tag plus attributes:* Pass in the name of an HTML tag as a keyword plus some attributes to describe it. `(find-it :a {:class "external"})` will return the first `<a>` tag with a class of "external"
* *HTML attributes alone:* You don't have to pass in a tag. `(find-it {:class "external"})` will find the first element of any tag with class "external"
* *Multiple HTML attributes:* You can pass in as many attribute-value pairs as you like. `(find-it {:class "external", :text "Moustache"})` will find the first HTML element on the page with both a class of "external" and visible text of "Moustache"
* *Regular Expressions:* Instead of looking for an exact match, you can use Java-style regular expressions to find elements. `(find-it :a {:class #"exter"})` will find the first `<a>` tag with a class which matches the regular expression `#"exter"`. Currently, you can use a regex if you pass only one attribute-value pair to `find-it` as in this bullet's example (this is actively being worked on and will be fixed soon).
* *XPath and CSS Selectors:* You can use the `:xpath` and `:css` attributes to use such queries in place of simple HTML attributes. If you use one of these attributes, you can't use any others, or an exception will be thrown (e.g. {:xpath "//a", :class "external"} is an illegal expression). `(find-it {:xpath "//a[@class='external']"})` will return the first `<a>` tag with a class of "external"
* *Ancestry-based queries:* Much like XPath or CSS Selectors, clj-webdriver provides a pure-Clojure mechanism for finding an element based on parent elements. `(find-it [:div {:id "content"}, :a {:class "external"}])` will find the first `<a>` tag with a class of "external" that is located within the `<div>` with id "content". This is equivalent to the XPath `//div[@id='content']//a[@class='external']`

As mentioned above, the `find-it` and `find-them` functions share the same features and syntax; `find-it` returns a single element, `find-them` returns a vector of all matched elements.

To demonstrate how to use arguments in different ways, consider the following example. If I wanted to find `<a href="/contact" id="contact-link" class="menu-item" name="contact">Contact Us</a>` in a page and click on it I could perform any of the following:

    (-> b
        (find-it :a)    ; assuming its the first <a> on the page
        click)
    
    (-> b
        (find-it {:id "contact-link"})    ; :id is unique, so only one is needed
        click)
    
    (-> b
        (find-it {:class "menu-item", :name "contact"})    ; use multiple attributes
        click)
    
    (-> b
        (find-it :a {:class "menu-item", :name "contact"})    ; specify tag
        click)
    
    (-> b
        (find-it :a {:text "Contact Us"})    ; special :text attribute, uses XPath's
        click)                               ; text() function to find the element
    
    (-> b
        (find-it :a {:class #"(?i)menu-"})  ; use Java-style regular
        click)                               ; expressions
    
    (-> b
        (find-it [:div {:id "content"}, :a {:id "contact-link"}]) ; hierarchical query
        click)                                                    ; equivalent to
                                                                  ; //div[@id='content']//a[@id='contact-link']
    
    (-> b
        (find-it [:div {:id "content"}, :a {}]) ; hierarchical query, tag with
        click)                                  ; no attributes (empty map required)
    
    (-> b
        (find-it {:xpath "//a[@id='contact-link']"})    ; XPath query
        click)
    
    (-> b
        (find-it {:css "a#contact-link"})    ; CSS selector
        click)

As seen above, the `find-it` function also understands `:xpath` and `:css` attributes, in which case it finds the element on the page described by the XPath or CSS query provided. An `IllegalArgumentException` will be thrown if you attempt to use `:xpath` or `:css` in conjunction with other attributes.

So, to describe the general pattern of interacting with the page:

    (-> browser-instance
        (find-it options)
        (do-something-with-the-element))

## Running Tests

The namespace `clj-webdriver.test.example-app.core` contains a [Ring][ring-github] app (routing by [Moustache][moustache-github]) that acts as my "control application" for this project's test suite. Instead of running my tests against a remote server on the Internet (prone to change, not always available), I've packaged this small web application to be run locally for the purposes of testing.

Due to some Java server/socket issues, you cannot start both this Ring app and the WebDriver browser instance in the test itself (in this situation, the Ring app starts and WebDriver opens the browser, but then a host of errors follow).

Here's how I run these tests:

* Open a terminal and run `lein repl` or `lein swank` at the root of this project
* Evaluate `(use 'clj-webdriver.test.example-app.core 'ring.adapter.jetty)`
* Evaluate `(defonce my-server (run-jetty #'routes {:port 8080, :join? false}))`
* Open a new terminal tab/window and run `lein test` at the root of this project

The last test in the suite closes the WebDriver browser instance; you can stop the Jetty server by evaluating `(.stop my-server)` or just killing the REPL with `Ctrl+C` or `C-c C-c`.

If anyone can figure out how to solve this issue (i.e. be able to run just `lein test` and start both the Ring app and WebDriver browser instance in one go), I'd be most appreciative. Until then, testing multiple server-based apps in separate "sandboxes" is acceptable to me.

## License

Distributed under the Eclipse Public License, the same as Clojure.

[webdriver-orig]: https://github.com/mikitebeka/webdriver-clj
[ring-github]: https://github.com/mmcgrana/ring
[moustache-github]: https://github.com/cgrand/moustache
