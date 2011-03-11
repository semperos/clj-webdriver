# Selenium-WebDriver Support for Clojure

This is a Clojure wrapper around the Selenium-WebDriver library. Almost all code (at this point) taken directly from [mikitebeka/webdriver-clj][webdriver-orig]. I've not forked the repository because mikitebeka added jar files to his repo in past commits, which makes the repo itself rather large given the actual size of the source code.

## Usage

Use/require the library in your code:

    (use 'clj-webdriver.core)

Start up a browser:

    (def b (start :firefox "https://github.com"))

At the moment, the best API documentation is the source code itself. While there are more than a few functions in the core namespace, they're mostly short and straightforward wrappers around WebDriver API's. Here's an example of logging into Github:

    (def b (start :firefox "https://github.com"))
    
    ;; Click "Login" link
    (->> "//ul[@class='nav logged_out']/li/a[text()='Login']"
         by-xpath                            ; xpath query
         (find-element b)
         click)
    
    ;; Input username/email into the "Login or Email" field
    (input-text (->> "login_field"
                      by-id                  ; id attribute
                      (find-element b))
                "username")
    
    ;; Input password into the "Password" field
    (input-text (->>  "password"
                      (by-attr= :name)       ; name attribute
                      (find-element b))
                "password")
    
    ;; Click the "Log in" button
    (->> "body.logged_out div#login.login_form label.submit_btn input"
         by-css-selector                     ; css selectors
         (find-element b)
         click)

It's very likely that some functions/macros will be coming along to make this more consistent.

## Running Tests

The namespace `clj-webdriver.test.example-app.core` contains a [Ring][ring-github] app (routing by [Moustache][moustache-github]) that acts as my "control application" for this project's test suite. Instead of running my tests against a remote server on the Internet (prone to change, not always available), I've packaged this small web application to be run locally for the purposes of testing.

Due to some Java server/socket issues, you cannot start both this Ring app and the WebDriver browser instance in the test itself (in this situation, the Ring app starts and WebDriver opens the browser, but then a host of errors follow).

Here's how I run these tests:

* Open a terminal and run `lein repl` or `lein swank` at the root of this project
* Evaluate `(use 'clj-webdriver.test.example-app.core)`
* Evaluate `(use 'ring.adapter.jetty)`
* Evaluate `(defonce my-server (run-jetty #'routes {:port 8080, :join? false}))`
* Open a new terminal tab/window and run `lein test` at the root of this project

The last test in the suite closes the WebDriver browser instance; you can stop the Jetty server by evaluating `(.stop my-server)` or just killing the REPL with `Ctrl+C` or `C-c C-c`.

If anyone can figure out how to solve this issue (i.e. be able to run just `lein test` and start both the Ring app and WebDriver browser instance in one go), I'd be most appreciative. Until then, testing multiple server-based apps in separate "sandboxes" is acceptable to me.

## License

Distributed under the Eclipse Public License, the same as Clojure.

[webdriver-orig]: https://github.com/mikitebeka/webdriver-clj
[ring-github]: https://github.com/mmcgrana/ring
[moustache-github]: https://github.com/cgrand/moustache
