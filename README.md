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
      <td>January 20 2014</td>
      <td><code>[clj-webdriver "0.6.1"]</code></td>
    </tr>
    <tr>
      <td>Pre-Release</td>
      <td>(Build locally)</td>
      <td><code>[clj-webdriver "0.7.0-SNAPSHOT"]</code></td>
    </tr>
  </tbody>
</table>

**clj-webdriver Resources**

 * [Project Wiki](https://github.com/semperos/clj-webdriver/wiki)
 * [Google Group](https://groups.google.com/forum/#!forum/clj-webdriver)
 * [Issue Queue](https://github.com/semperos/clj-webdriver/issues)

**External Resources**

 * [Selenium-WebDriver API (Javadoc)](http://selenium.googlecode.com/svn/trunk/docs/api/java/index.html)
 * [Selenium-WebDriver Changelog](https://code.google.com/p/selenium/source/browse/java/CHANGELOG)
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

Please note that the high-level Taxi API is only available in the 0.6.0 release series.

## Documentation ##

For API documentation on the high-level Taxi API (shown above), please [see its wiki page](https://github.com/semperos/clj-webdriver/wiki/Introduction%3A-Taxi).

For reference documentation, run `lein doc` at the root of this repo. For annotated source documentation, run `lein marg`.

## Contributing ##

The `master` branch of clj-webdriver houses code intended for the next **minor-version release.** If you want to propose new features for the next release, you're welcome to fork, make a topic branch and issue a pull request against the `master` branch.

If you want to fix a bug in the **current release**, please pull against the appropriate branch for the current minor version, **0.6.x**.

## Running Tests ##

Follow these steps from the root of this project to run clj-webdriver's tests locally:

```bash
cp resources/properties-example.clj resources/properties.clj

# Now edit that `properties.clj` file according to your local setup

lein compile

./script/test all
```

Note that you can run subsets of the test suite with the `script/test` script as follows:

```
Usage: test {all|core|firefox|remote|saucelabs|taxi|window}
```

The `core` option runs tests against the core API's using Firefox and Chrome. The `remote` option runs tests for Grid and remote server/driver handling. The `taxi` option tests just the Taxi API. The `all` option runs through each of the other options once.

*Note:* If you just want to run the example app that clj-webdriver uses for its testing purposes, you can find the app [on Heroku](http://vast-brushlands-4998.herokuapp.com) or can run it locally as follows:

 * Open a terminal and run `lein repl` or `lein swank` at the root of this project
 * Evaluate `(use 'clj-webdriver.test.example-app.core 'ring.adapter.jetty)`
 * Evaluate `(defonce my-server (run-jetty #'routes {:port 5744, :join? false}))`, making sure to adjust the `test-port` in `test/clj_webdriver/test/core.clj` to whatever you use here.

## Acknowledgements ##

Credits to [mikitebeka/webdriver-clj](https://github.com/mikitebeka/webdriver-clj) for the initial code for this project and many of the low-level wrappers around the Selenium-WebDriver API.

Many thanks to those who have contributed so far (in nick-alphabetical order):

 * [kapman](https://github.com/kapman)
 * [mangaohua](https://github.com/mangaohua)
 * [maxweber](https://github.com/maxweber) (Max Weber)
 * [RobLally](https://github.com/RobLally) (Rob Lally)
 * [smidas](https://github.com/smidas) (Nathan Smith)
 * [ulsa](https://github.com/ulsa) (Ulrik Sandberg)
 * [xeqi](https://github.com/xeqi) (Nelson Morris)

See Github for an [up-to-date list of contributors](https://github.com/semperos/clj-webdriver/contributors)

## Open Source Tools ##

I would like to thank the following companies for providing their tools free of charge to clj-webdriver developers as part of their contribution to the Open Source community.

### JetBrains: Intellij IDEA ###

When I need to do Java, Scala, or even JRuby development, I rely on Intellij IDEA's excellent support for JVM languages. I would like to thank JetBrains for granting clj-webdriver developers a free license to Intellij IDEA Ultimate, now for two years running.

<a href="http://www.jetbrains.com/idea/features/javascript.html" style="display:block; background:#0d3a9e url(http://www.jetbrains.com/idea/opensource/img/all/banners/idea468x60_blue.gif) no-repeat 10px 50%; border:solid 1px #0d3a9e; margin:0;padding:0;text-decoration:none;text-indent:0;letter-spacing:-0.001em; width:466px; height:58px" alt="Java IDE with advanced HTML/CSS/JS editor for hardcore web-developers" title="Intellij IDEA: Java IDE with advanced HTML/CSS/JS editor for hardcore web-developers"><span style="margin:0 0 0 205px;padding:18px 0 2px 0; line-height:13px;font-size:11px;cursor:pointer;  background-image:none;border:0;display:block; width:255px; color: #acc4f9; font-family: trebuchet ms,arial,sans-serif;font-weight: normal;text-align:left;">Intellij IDEA: Java IDE with advanced HTML/CSS/JS editor for hardcore web-developers</span></a>

### YourKit ###

YourKit is kindly supporting open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
<a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a> and
<a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>.

## License ##

Clj-webdriver is distributed under the [Eclipse Public License](http://opensource.org/licenses/eclipse-1.0.php), the same as Clojure.
