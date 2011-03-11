# Selenium WebDriver Support for Clojure

This is a Clojure wrapper around the Selenium-WebDriver library. Almost all code (at this point) taken directly from [mikitebeka/webdriver-clj][webdriver-orig]. I've not forked the repository because mikitebeka added jar files to his repo in past commits, which makes the repo itself rather large given the actual size of the source code.

## Usage

Use/require the library in your code:

    (use 'clj-webdriver.core)

Start up a browser:

    (def my-browser (start :firefox "https://github.com"))

Then read the source to see what functions are available. While there are more than a few functions in the core namespace, they're mostly short and straightforward wrappers around WebDriver API's. Here's an example of logging into Github:

    (def b (start :firefox "https://github.com"))
    (->> "/html/body/div/div/div/ul/li[5]/a"
         by-xpath
         (find-element b)
         click)
    (input-text (->> "login_field"
                      by-id
                      (find-element b))
                "username")
    (input-text (->>  "password"
                      by-name
                      (find-element b))
                "password")
    (->> "Log in"
         by-value
         (find-element b)
         click)

It's very likely that some macros will be coming along to make this more consistent.

## License

Distributed under the Eclipse Public License, the same as Clojure.

[webdriver-orig]: https://github.com/mikitebeka/webdriver-clj
