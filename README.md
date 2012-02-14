# Clojure API for Selenium-WebDriver #

This is a Clojure library for driving a web browser using Selenium-WebDriver as the backend. For more comprehensive documentation on all of clj-webdriver's features, read the [Github wiki](https://github.com/semperos/clj-webdriver/wiki). You can generate documentation locally with `lein doc` (API docs) or `lein marg` (annotated source).

**clj-webdriver Resources**

 * [Project Wiki](https://github.com/semperos/clj-webdriver/wiki)
 * [Google Group](https://groups.google.com/forum/#!forum/clj-webdriver)
 * [Issue Queue](https://github.com/semperos/clj-webdriver/issues)

**External Resources**

 * [Selenium-WebDriver API Javadoc](http://selenium.googlecode.com/svn/trunk/docs/api/java/index.html)
 * [Selenium-WebDriver Changelog](http://code.google.com/p/selenium/source/browse/trunk/java/CHANGELOG)
 * [CSS Selector Syntax](http://www.w3.org/TR/css3-selectors/#selectors)

**Please join the Google group if you use this library.** I regularly post announcements about upcoming releases, and although I ensure all tests are passing and try to maintain good test coverage before releases, user testing is invaluable. Thank you!

## Usage ##

This library is compatible with *Clojure 1.3.0*.
 
### Quickstart ###

Use/require the library in your code:

```clj
(use 'clj-webdriver.core)
```

Start up a browser:

```clj
(def b (start {:browser :firefox} "https://github.com"))
```

Here's an example of logging into Github:

```clj
;; Start the browser and bind it to `b`
(def b (start {:browser :firefox} "https://github.com"))

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
    (find-it {:xpath "//input[@id='password']"}) ; :xpath and :css options
    (input-text "password"))

;; Click the "Log in" button"
(-> b
    (find-it {:tag :input, :value #"(?i)log"}) ; use of regular expressions
    click)
```

Filling out the form can been accomplished more compactly using `clj-webdriver.form-helpers/quick-fill` as follows:

```clj
(require '[clj-webdriver.form-helpers :as form])

(form/quick-fill b [{{:class "text", :name "login"}     "username"}
                    {{:xpath "//input[@id='password']"} "password"}
                    {{:value #"(?i)log"}                click}])
```

If you plan to submit the form, you need to pass a third parameter of `true` to prevent `quick-fill` from trying to return the elements you act upon (since the page will reload, they will be lost in the Selenium-WebDriver cache).

### Finding Elements ###

The `find-it` function provides high-level querying abilities against the DOM using HTML attribute comparisons, XPath and CSS queries, or pure-Clojure hierarchical queries. As parameters it always takes a Driver record first, followed by one of the following:

#### Attribute-Value Map ####

The attribute-value map (`attr-val`) can consist of HTML attributes, or can designate an XPath or CSS query:

```clj
(find-it driver {:class "foo"})
(find-it driver {:tag :a, :class "bar"})

(find-it driver {:xpath "//a[@class='foo']"})
(find-it driver {:css "a.bar"})
```

If the `:xpath` or `:css` options are used, everything else in the `attr-val` map is ignored.

#### Hierarchical Queries ####

If you want to build XPath or CSS-like hierarchical queries in pure Clojure, you can use the following type of forms with `find-it`:

```clj
(find-it driver [{:tag :form}, {:tag :input, :type :radio, :id "foo"}])
```

Note that the usual attribute-value maps are within a vector, which is what lends the ordering to the hierarchical query. On the backend, this is simply converted to XPath.

##### Special Tags #####

By default, the `:tag` option represents a standard HTML tag like `<a>` or `<div>`. Clj-webdriver, however, supports a number of "special" tags to make using `find-it` more intuitive or concise. *(Note that these are not available inside hierarhical queries.)*

Here are all the special tags in action:

```clj
(find-it driver {:tag :radio})
;=> (find-it driver {:tag :input, :type "radio"})

(find-it driver {:tag :checkbox})
;=> (find-it driver {:tag :input, :type "checkbox"})

(find-it driver {:tag :textfield})
;=> (find-it driver {:tag :input, :type "text"})

(find-it driver {:tag :password})
;=> (find-it driver {:tag :input, :type "password"})

(find-it driver {:tag :filefield})
;=> (find-it driver {:tag :input, :type "file"})

(find-it driver {:tag :button*})
```

The `:button*` option, unlike the others, conflates all button-like elements (form submit buttons, actual `<button>` tags, etc.).

#### Find-it Summary ####

To demonstrate how to use arguments in different ways, consider the following example. If I wanted to find `<a href="/contact" id="contact-link" class="menu-item" name="contact">Contact Us</a>` in a page and click on it I could perform any of the following:

```clj
(-> b
    (find-it {:tag :a})    ; assuming its the first <a> on the page
    click)

(-> b
    (find-it {:id "contact-link"})    ; :id is unique, so only one is needed
    click)

(-> b
    (find-it {:class "menu-item", :name "contact"})    ; use multiple attributes
    click)

(-> b
    (find-it {:tag :a, :class "menu-item", :name "contact"})    ; specify tag
    click)

(-> b
    (find-it {:tag :a, :text "Contact Us"})    ; special :text attribute, uses XPath's
    click)                                     ; text() function to find the element

(-> b
    (find-it {:tag :a, :class #"(?i)menu-"})  ; use Java-style regular
    click)                                    ; expressions

(-> b
    (find-it {:xpath "//a[@id='contact-link']"})    ; XPath query
    click)

(-> b
    (find-it {:css "a#contact-link"})    ; CSS selector
    click)
```

So, to describe the general pattern of interacting with the page:

```clj
(-> browser-instance
    (find-it options)
    (do-something-with-the-element))
```

### Firefox Functionality

Support for Firefox currently exceeds that for all other browsers, most notably via support for customizable Firefox profiles. I've included support for several of these advanced featues in the `clj-webdriver.firefox` namespace. Here are a few examples (borrowed from [here](http://code.google.com/p/selenium/wiki/RubyBindings):

```clj
(use 'clj-webdriver.core)
(require '[clj-webdriver.firefox :as ff])

(def b (new-driver {:browser :firefox,
                    :profile (doto (ff/new-profile)
                              ;; Enable Firebug
                              (ff/enable-extension "/path/to/extensions/firebug.xpi")))
                         
                              ;; Auto-download certain file types to a specific folder
                              (ff/set-preferences {:browser.download.dir "C:/Users/semperos/Desktop",
                                                   :browser.download.folderList 2
                                                   :browser.helperApps.neverAsk.saveToDisk "application/pdf"})}))
```
                                  
### Grid Support ###

From a "user" perspective, working with Selenium-WebDriver's Grid 2 support behaves exactly like interacting with a locally-run RemoteWebDriver instance. See the `clj-webdriver.remote-server` and `clj-webdriver.remote-driver` namespaces for details on using this functionality.

For information about configuring your Grid hub and nodes (which is handled at the command-line using the server-standalone jars), read [the Selenium-WebDriver wiki documentation on Grid 2](http://code.google.com/p/selenium/wiki/Grid2).

## Documentation ##

For reference documentation, run `lein doc` at the root of this repo. For annotated source documentation, run `lein marg`.

## Contributing ##

The `master` branch of clj-webdriver houses code intended for the next **minor-version release.** If you want to propose new features for the next release, you're welcome to fork, make a topic branch and issue a pull request against the `master` branch.

If you want to fix a bug in the **current release**, please pull against the appropriate branch for the current minor version, **0.4.x**.

## Running Tests

The namespace `clj-webdriver.test.example-app.core` contains a [Ring](https://github.com/mmcgrana/ring) app (routing by [Moustache](https://github.com/cgrand/moustache)) that acts as the "control application" for this project's test suite.

Use `lein test` to run this library's test suite. Ensure port 5744 is free, or edit `test/clj_webdriver/test/core.clj` before running the tests.

It is **highly** recommended that you run the test suite for each browser separately, as otherwise you will see strange errors. Each supported browser has its own namespace, for example:

```
lein test clj-webdriver.test.firefox
```

*Note:* If you just want to run the example app that clj-webdriver uses for its testing purposes, do the following:

 * Open a terminal and run `lein repl` or `lein swank` at the root of this project
 * Evaluate `(use 'clj-webdriver.test.example-app.core 'ring.adapter.jetty)`
 * Evaluate `(defonce my-server (run-jetty #'routes {:port 5744, :join? false}))`, making sure to adjust the `test-port` in `test/clj_webdriver/test/core.clj` to whatever you use here.

## Acknowledgements

Credits to [mikitebeka/webdriver-clj](https://github.com/mikitebeka/webdriver-clj) for the initial code for this project and many of the low-level wrappers around the Selenium-WebDriver API.

Many thanks to those who have contributed so far (in nick-alphabetical order):

 * [maxweber](https://github.com/maxweber) (Max Weber)
 * [RobLally](https://github.com/RobLally) (Rob Lally)
 * [ulsa](https://github.com/ulsa) (Ulrik Sandberg)
 * [xeqi](https://github.com/xeqi) (Nelson Morris)

See Github for an [up-to-date list of contributors](https://github.com/semperos/clj-webdriver/contributors)

## License

Distributed under the [Eclipse Public License](http://opensource.org/licenses/eclipse-1.0.php), the same as Clojure.
