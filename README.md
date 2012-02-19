# Clojure API for Selenium-WebDriver #

This is a Clojure library for driving a web browser using Selenium-WebDriver as the backend. For more comprehensive documentation on all of clj-webdriver's features, read the [Github wiki](https://github.com/semperos/clj-webdriver/wiki). You can generate documentation locally with `lein doc` (API docs) or `lein marg` (annotated source).

**clj-webdriver Resources**

 * [Project Wiki](https://github.com/semperos/clj-webdriver/wiki)
 * [Google Group](https://groups.google.com/forum/#!forum/clj-webdriver)
 * [Issue Queue](https://github.com/semperos/clj-webdriver/issues)

**External Resources**

 * [Selenium-WebDriver API (Javadoc)](http://selenium.googlecode.com/svn/trunk/docs/api/java/index.html)
 * [Selenium-WebDriver Changelog](http://code.google.com/p/selenium/source/browse/trunk/java/CHANGELOG)
 * [CSS Selector Syntax](http://www.w3.org/TR/css3-selectors/#selectors)

**Please join the Google group if you use this library.** I regularly post announcements about upcoming releases, and although I ensure all tests are passing and try to maintain good test coverage before releases, user testing is invaluable. Thank you!

## Usage ##

This library is compatible with *Clojure 1.3.0*.
 
### Quickstart ###

Here's a complete example of how to log into Github:

```clj
(use 'clj-webdriver.taxi)

;; Start up a browser
(set-driver! {:browser :firefox} "https://github.com")

(click "a[href*='login']")

(input-text "#login_field" "your-username")
(input-text "#password" "your-password")

(submit "#password")
(quit)
```

Forms can be filled out en masse using the `quick-fill-submit` function:

```clj
(quick-fill-submit {"#login_field" "your-username"}
                   {"#password" "your-password"}
                   {"#password" submit})
```

## API Documentation ##

For API documentation on the high-level Taxi API (shown above), please [see its wiki page](https://github.com/semperos/clj-webdriver/wiki/Taxi%3A-Concise%2C-High-level-API).

### Firefox Functionality

**Note: This documentation is still valid, but refers to the lower-level core API for clj-webdriver.**

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

From a "user" perspective, working with Selenium-WebDriver's Grid 2 support behaves exactly like interacting with a locally-run RemoteWebDriver instance. See the `clj-webdriver.remote.server` and `clj-webdriver.remote.driver` namespaces for details on using this functionality.

For information about configuring your Grid hub and nodes (which is handled at the command-line using the server-standalone jars), read [the Selenium-WebDriver wiki documentation on Grid 2](http://code.google.com/p/selenium/wiki/Grid2).

## Documentation ##

For reference documentation, run `lein doc` at the root of this repo. For annotated source documentation, run `lein marg`.

## Contributing ##

The `master` branch of clj-webdriver houses code intended for the next **minor-version release.** If you want to propose new features for the next release, you're welcome to fork, make a topic branch and issue a pull request against the `master` branch.

If you want to fix a bug in the **current release**, please pull against the appropriate branch for the current minor version, **0.4.x**.

## Running Tests

The namespace `clj-webdriver.test.example-app.core` contains a [Ring](https://github.com/mmcgrana/ring) app (routing by [Moustache](https://github.com/cgrand/moustache)) that acts as the "control application" for this project's test suite.

Use `lein test` to run this library's test suite. Ensure port 5744 is free, or edit `test/clj_webdriver/test/core.clj` before running the tests. To run tests for the Taxi API, make sure you have the `lein-midje` plugin installed and run `lein midje clj-webdriver.test.taxi`.

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
