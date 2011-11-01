# Changelog #

v0.4.0 (SNAPSHOT)
 - Refactor API to use a Driver record (wrapped around the WebDriver instance)
 - Provide API support for both the new Driver and old plain Java WebDriver
 - Develop tests for both Driver and WebDriver
 - Implement API in terms of protocols that both Driver and WebDriver implement
 - Propogate Driver changes to Firefox and Grid support namespaces
 - Add caching functionality, for caching page elements on a given page based on configurable cache rules
 - Add cursory logging (to "watch" the cache work)

v0.3.2
 - Fix unnecessary memory consumption in `quick-fill` for forms that submit

v0.3.1
 - Make `back` and `forward` fn's return the driver instance
 - Enhance `quick-fill` fn to handle submitting forms correctly

v0.3.0
 - Make `new-driver` accept keywords or strings for browser names
 - Add support for starting WebDriver's on the Selenium Grid
 - Update WebDriver dependency to 2.7

v0.2.16
 - Fix and add test for `flash` fn
 - Fix deprecated Java method call for getting style properties of elements

v0.2.15
 - Update Clojure to 1.3.0 (release)
 - Add acknowledgements section to README

v0.2.14
 - Update Clojure to 1.3.0-RC0
 - Merge fix for `empty?` from RobLally
 - Make `exists?` use Clojure truthiness

v0.2.13
 - Update WebDriver dependency to 2.5
 - Remove deprecated `.setSelected`, replace with `.click`

v0.2.12
 - Update Clojure to 1.3.0-beta3

v0.2.11
 - Merge xeqi's wait functionality into core

v0.2.10
 - Enhance README documentation
 - Add form-helpers namespace, with `quick-fill` fn for filling out forms declaratively
 - Update WebDriver dependency to 2.4

v0.2.9
 - Update Clojure to 1.3.0-beta1
 - Enhance test scripts

v0.2.8
 - Fix bug reported by ulsa with `flash` fn
 - Update WebDriver dependency to 2.3, add needed JNA repo to `project.clj`
 - Replace deprecated `.toggle` with `.click`

v0.2.7
 - Bump Clojure to most recent stable, 1.2.1
 - Update namespace declarations to abide by Clojure 1.2.1 changes

v0.2.6
 - Enhance thrown exception with better message and page source code
 - Replace `exists?` fn with macro (as it otherwise necessarily throws an exception)

v0.2.5
 - Stop intercepting NoSuchElementExceptions, as they should bubble up

v0.2.4
 - Fix improper logic for `:xpath` and `:css` handling in find-* functions

v0.2.3
 - Improve handling of `:xpath` and `:css` for find-* functions

v0.2.2
 - Enhance documentation examples
 - Add function to generate XPath for given element
 - Add table functions (finding cells based on x/y)
 - Add `:index` functionality to find-* functions

v0.2.1
 - Add shortcuts for `:radio`, `:checkbox`, and `:textfield`
 - Add fn's for deselecting checkboxes and shifting focus to a particular element
 - Add support for `:tag-name` in find-* functions
 - Consolidate namespaces into core

v0.2.0
 - Add support for "semantic" button handling with find-* functions, so that different tags rendered as buttons are intuitive to find
 - Fix fn's that load new page, to not return elements (will throw exception at WebDriver level)
 - Add functions that execute JavaScript for some advanced functionality, borrowed from Watir-WebDriver
 - Increase regex support for find-* functions
 - Remove to-be-deprecated speed handling functionality
 - Bump WebDriver dep to 2.0b3

v0.1.4
 - Enhance find-* functions to handle window handles in addition to page elements
 - Make return values of close and exists? more reasonable
 - Add tests for window handling

v0.1.3 (Build-Fixer)
 - Remove extraneous use statement for deleted ordered-set dependency

v0.1.2
 - Remove ordered-set for datastructure for window handles
 - Enhance window-picking functions to use a more dev-friendly set of parameters

v0.1.1
 - Add WindowHandle record for handling .getWindowHandles
 - Use ordered-set for pure-Clojure equivalent of Java datastructure for window handles
 - Add functions to handle switching between browser windows

v0.1.0

 - find-* functionality with support for all query types (pure-Clojure, XPath, CSS)
 - Enhanced by-* support
 - Testing app (Moustache) built
 - Firefox profile support
 - README examples developed
 - Initial test suite developed
 - Keep WebDriver dependency in 2.x range
