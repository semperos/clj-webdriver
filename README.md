# Clojure API for Selenium-WebDriver #

This is a Clojure library for driving a web browser using Selenium-WebDriver as the backend. For more comprehensive documentation on all of clj-webdriver's features, read the [Github wiki](https://github.com/semperos/clj-webdriver/wiki). You can generate documentation locally with `lein doc` (API docs) or `lein marg` (annotated source).

<table>
  <thead>
    <tr>
      <th>Release Type</th>
      <th>Date</th>
      <th>Leiningen/Maven</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Stable</td>
      <td>Feb 15 2012</td>
      <td><code>[clj-webdriver "0.5.1"]</code></td>
    </tr>
    <tr>
      <td>Pre-Release</td>
      <td>August 15 2012</td>
      <td><code>[clj-webdriver "0.6.0-alpha11"]</code></td>
    </tr>
  </tbody>
</table>

**NOTE TO LEININGEN 2 USERS:** At least as of Leiningen 2 preview 6, the default behavior for retrieving dependencies that fail a checksum test is *not to download them.* This affect's at least one of Selenium-WebDriver's transitive dependencies, which means you can't download clj-webdriver's dependencies with default Leiningen 2 settings. In order to solve this issue, you need to set the `:checksum` option for either your entire `project.clj` or for a specific Maven `:repositories` entry that contains Selenium-WebDriver's dependencies (e.g., Maven central). See [Leiningen's](https://github.com/technomancy/leiningen) repository README and example `sample.project.clj` file for more information.

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

### Quickstart ###

Here's a complete example of how to log into Github, using the high-level Taxi API:

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

## Documentation ##

For API documentation on the high-level Taxi API (shown above), please [see its wiki page](https://github.com/semperos/clj-webdriver/wiki/Introduction%3A-Taxi).

For reference documentation, run `lein doc` at the root of this repo. For annotated source documentation, run `lein marg`.

## Contributing ##

The `master` branch of clj-webdriver houses code intended for the next **minor-version release.** If you want to propose new features for the next release, you're welcome to fork, make a topic branch and issue a pull request against the `master` branch.

If you want to fix a bug in the **current release**, please pull against the appropriate branch for the current minor version, **0.5.x**.

## Running Tests ##

The namespace `clj-webdriver.test.example-app.core` contains a [Ring](https://github.com/mmcgrana/ring) app (routing by [Moustache](https://github.com/cgrand/moustache)) that acts as the "control application" for this project's test suite.

First, run `lein compile`, then use the `script/test` script to run clj-webdriver's tests. Its usage is as follows:

```
Usage: test {all|core|remote|taxi}
```

The `core` option runs tests against the core API's using Firefox and Chrome. The `remote` option runs tests for Grid and remote server/driver handling. The `taxi` option tests just the Taxi API. The `all` option runs through each of the other options once.

*Note:* If you just want to run the example app that clj-webdriver uses for its testing purposes, do the following:

 * Open a terminal and run `lein repl` or `lein swank` at the root of this project
 * Evaluate `(use 'clj-webdriver.test.example-app.core 'ring.adapter.jetty)`
 * Evaluate `(defonce my-server (run-jetty #'routes {:port 5744, :join? false}))`, making sure to adjust the `test-port` in `test/clj_webdriver/test/core.clj` to whatever you use here.

## Acknowledgements ##

Credits to [mikitebeka/webdriver-clj](https://github.com/mikitebeka/webdriver-clj) for the initial code for this project and many of the low-level wrappers around the Selenium-WebDriver API.

Many thanks to those who have contributed so far (in nick-alphabetical order):

 * [mangaohua](https://github.com/mangaohua)
 * [maxweber](https://github.com/maxweber) (Max Weber)
 * [RobLally](https://github.com/RobLally) (Rob Lally)
 * [smidas](https://github.com/smidas) (Nathan Smith)
 * [ulsa](https://github.com/ulsa) (Ulrik Sandberg)
 * [xeqi](https://github.com/xeqi) (Nelson Morris)

See Github for an [up-to-date list of contributors](https://github.com/semperos/clj-webdriver/contributors)

## YourKit ##

I would also like to thank YourKit, LLC for providing its Java profiler to clj-webdriver developers as part of its contribution to the Open Source community.

YourKit is kindly supporting open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
<a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a> and
<a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>.

## License ##

Distributed under the [Eclipse Public License](http://opensource.org/licenses/eclipse-1.0.php), the same as Clojure.
