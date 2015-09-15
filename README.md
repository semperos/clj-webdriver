# Clojure API for Selenium-WebDriver #

This is a Clojure library for driving a web browser using Selenium-WebDriver.

You **must** add the Selenium-WebDriver JAR's you need explicitly in your project's dependencies. This library _does not_ ship with runtime dependencies on any version of Selenium-WebDriver to allow compatibility with Selenium-WebDriver's upstream releases.

Please see the [Wiki](https://github.com/semperos/clj-webdriver/wiki) for prose documentation or generate API docs using `lein doc` inside this project.

**Latest stable coordinates:**

[![Clojars Project](http://clojars.org/clj-webdriver/latest-version.svg)](http://clojars.org/clj-webdriver)

**clj-webdriver Resources**

 * [Project Wiki](https://github.com/semperos/clj-webdriver/wiki)
 * [Google Group](https://groups.google.com/forum/#!forum/clj-webdriver)
 * [Issue Queue](https://github.com/semperos/clj-webdriver/issues)
 * [Travis CI](https://travis-ci.org/semperos/clj-webdriver) [![Build Status](https://travis-ci.org/semperos/clj-webdriver.svg?branch=master)](https://travis-ci.org/semperos/clj-webdriver)

**External Resources**

 * [Selenium-WebDriver API (Javadoc)](http://selenium.googlecode.com/svn/trunk/docs/api/java/index.html)
 * [Selenium-WebDriver Changelog](https://code.google.com/p/selenium/source/browse/java/CHANGELOG)
 * [CSS Selector Syntax](http://www.w3.org/TR/css3-selectors/#selectors)

**Please join the Google group if you use this library.** I regularly post announcements about upcoming releases, and although I ensure all tests are passing and try to maintain good test coverage before releases, user testing is invaluable. Thank you!

## Contributing ##

The `master` branch of clj-webdriver houses code intended for the next **minor-version release.** If you want to propose new features for the next release, you're welcome to fork, make a topic branch and issue a pull request against the `master` branch.

If you want to fix a bug in the **current release**, please pull against the appropriate branch for the current minor version, **0.7.x**.

## Running Tests ##

To run the default suite:

```
lein test
```

To run the test suite for an existing hub/node setup:

```
./script/grid-hub start
./script/grid-node start
lein test :manual-setup
```

To run the test suite for Saucelabs, first visit the [test app on Heroku](http://vast-brushlands-4998.herokuapp.com) to make sure it's "awake" and then run:

```
lein test :saucelabs
```

## Release ##

There's a Ruby script at `script/release`. It was written using version 2.2.2, no promises that it works with any other.

```
./script/release --release-version 8.8.8 --new-version 9.0.0-SNAPSHOT
```

The `--release-version` can be `-r` and the `--new-version` can be `-n`. Further, the new version must end with `-SNAPSHOT`.

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
