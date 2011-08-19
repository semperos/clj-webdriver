# Todo's for clj-webdriver #

The following are features I would like to implement or limitations I would like to eliminate. Several of these may be inappropriate for `clj-webdriver`, but would be fine additions as libraries which depend on `clj-webdriver` for low-level browser manipulation.

## Features ##

### Grid Support ###

Selenium Grid is now part of the standalone jar distribution of Selenium server. It would be nice to provide mechanisms to easily start the grid and child nodes directly from code.

### Test Record ###

Create a record used to compose test suites. Attributes of this record would include:

 * A function that actually runs the steps of driving the browser
 * Wrapper definitions (see below)
 * Test-specific configuration settings for browser/version and OS (useful with Grid support, see below)

### Data-Driven Testing ###

Ostensibly, this library is most useful as a web testing tool (that's certainly how I use it). In that vein, it would be good to support various kinds of data-driven testing, including reading from common formats (CSV, SQL databases, even Excel spreadsheets (using existing Java libraries)).

### Wrappers/Middlewares ###

Beyond simply interacting with the page, this library should allow developers to gather information about the elements, the page or the browser at any given point. To help foster this, it would be nice to be able to "wrap" functionality around the means of interacting with the page, and allow developers to write middlewares that do things like custom reporting, extra auxiliary validation, or even things that might alter the DOM based on context.

Query-wrappers allow wrapping around the query sent to WebDriver, before ever touching the HTML page.

Result-wrappers allow wrapping around the result of an interaction with the page, which is almost always an HTML element.
