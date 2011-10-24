# Todo's for clj-webdriver #

The following are features I would like to implement or limitations I would like to eliminate. Several of these may be inappropriate for `clj-webdriver`, but would be fine additions as libraries which depend on `clj-webdriver` for low-level browser manipulation.

## Features ##

### Driver Record ###

A number of nice things could be accomplished by wrapping the raw WebDriver Java object in a Clojure record (let's call it Driver):

 * The `clj-wedriver` API could be composed of sane protocols that the Driver record implements
 * The Driver could maintain a per-page cache of WebElement objects for those elements of the page's DOM that are static (basically a memoization scheme, so you pay the price of WebElement lookup once)
 * The Driver record could register "wrapper functions" that act as middlewares (see below)
 * The Driver could house extra meta-data, useful both for pure Clojure programming and for configuration with things like the Selenium Grid

This record would still expose a painless way of retrieving the WebDriver Java object, but I think the above use cases alone justify adding this layer of abstraction. I'm considering this a blocker for the 1.0 release of `clj-webdriver`.

### Wrappers/Middlewares ###

Beyond simply interactions with the page, this library should allow developers to gather information about the elements, the page or the browser at any given point. To help foster this, it would be nice to be able to "wrap" functionality around the means of interacting with the page, and allow developers to write middlewares that do things like custom reporting, extra auxiliary validation, or even things that might alter the DOM based on context.

Query-wrappers allow wrapping around the query sent to WebDriver, before ever touching the HTML page.

Result-wrappers allow wrapping around the result of an interaction with the page, which is almost always an HTML element.

## Tests ##

The following tests need to be written to cover the API more comprehensively:

* Test all available browser drivers (use one for majority of tests, ensure that at least all will open to a URL and close)
* JavaScript execution
* Form submission
* Exceptions thrown when finding elements
* Grid functions
