# I18n asset-pipeline plugin

The Grails plugin `i18n-asset-pipeline` is an asset-pipeline plugin that
generates a JavaScript file with localized texts which can be used for
client-side i18n.

For more information on how to use asset-pipeline, visit
[asset-pipeline project page][asset-pipeline].

## Version information

Because `asset-pipeline` 2.x and 3.x introduced new APIs and aren't backward
compatible, you must use the following versions of this plugin:

i18n-asset-pipeline version | required for
----------------------------|--------------
 0.x                        | `asset-pipeline` up to version 1.9.9
 1.x                        | `asset-pipeline` version 2.0.0 or higher
 2.x                        | Grails 3.x

## Installation

To use this plugin you have to add the following code to your `build.gradle`:

```groovy
buildscript {
    dependencies {
        classpath 'com.webbfontaine.grails.plugins:i18n-asset-pipeline:2.0.1'
    }
}

dependencies {
    runtime 'com.webbfontaine.grails.plugins:i18n-asset-pipeline:2.0.1'
}

apply plugin: "org.grails.plugins.i18n-asset-pipeline"
```

## CAREFUL the apply plugin must be  applied after the plugin that creates :assetCompile. The order is important !

The first dependency declaration is needed to precompile your assets (e. g.
when building a WAR file).  The second one provides the necessary
`<asset:i18n>` tag and compiles the assets on the fly (e. g. in development)
mode.

## Usage

`i18n-asset-pipeline` uses special files in your asset folders (we recommend
`grails-app/assets/i18n`) with extension '.i18n'.  The names of
these files must contain a language specification separated by underscore, e.
g. `messages_de.i18n` or `messages_en_UK.i18n`.  Files without a language
specification (e. g. `messages.i18n`) are files for the default locale.  These
files mainly contain message codes that are resolved to localized texts.

The plugin generates a JavaScript file, that contains a function named `$L`
which can be called to obtain the localized message by a given code, e. g.:

```javascript
$(".btn").text($L("default.btn.ok"));

or
$(".btn").text($L("default.btn.ok", 25));
or
$(".btn").text($L("default.btn.ok", ['foo':'bar']));

```

## I18n file syntax

Each i18n file must be defined according to the following rules:

* Files are line based.
* Lines are trimmed (i. e. leading and terminating whitespaces are removed).
* Empty lines and lines starting with a hash `#` (comment lines) are ignored.
* Lines starting with `@import` *`url`* are resolved by importing file
  *`url`*, processing it according to these rules, and replacing the
  `@import` statement by its content.  The import file may contain further
  import statements, even circular ones.  You may omit file extension `.i18n`
  in *`url`*.
* All other lines are treated as messsage codes which are translated to the
  required language.
* Comments after import statements and message codes are not allowed.
* Regex can be used. For example `client\.` will import all the keys starting with client like client.sample . 
  The key will be transformed into sample in the JS file.
  To be able to specifix suffix, the first group in the regex will be used as the final key in the js file
  Example : (.*)\.suffix will transform toto.suffix = Test into toto = Test in the js file


Each i18n file may contain asset-pipeline `require` statements to load other
assets such as JavaScript files.  **ATTENTION!** Don't use `require` to load
other i18n files because they will not be processed correctly.  Use the
`@import` declaration instead.

*

## Typical file structure

Typically, you have one i18n file for each language in the application.  Given,
you have the following message resources in `grails-app/i18n`:

* `messages.properties`
* `messages_de.properties`
* `messages_en_UK.properties`
* `messages_es.properties`
* `messages_fr.properties`

Then, you should have the same set of files in e. g. `grails-app/assets/i18n`:

* `messages.i18n`
* `messages_de.i18n`
* `messages_en_UK.i18n`
* `messages_es.i18n`
* `messages_fr.i18n`

Normally, you would have to declare the same set of message codes in each file.
To DRY, add a file `_messages.i18n` to `grails-app/assets/i18n` (the
leading underscore prevents the i18n file to be compiled itself):

```
#
# _messages.i18n
# List of message codes that should be available on client-side.
#

# Add your messages codes here:
default.btn.cancel
default.btn.ok
contact.foo.bar

```

Then, you can import this file in all other files, e. g.:

```
#
# messages.i18n
# Client-side i18n, English messages.
#

@import _messages

```

```
#
# messages_de.i18n
# Client-side i18n, German messages.
#

@import _messages

```

```
#
# messages_es.i18n
# Client-side i18n, Spanish messages.
#

@import _messages

```

## Including localized assets

In order to include a localized asset you can either use an asset-pipeline
`require` directive or the tag `<asset:i18n>`.  The tag supports the following
attributes:

* `locale`.  Either a string or a `java.util.Locale` object representing the
  locale that should be loaded.  This attribute is mandatory.
* `name`.  A string indicating the base name of the i18n files to load
  (defaults to `messages`).

Examples:

```html
<asset:i18n locale="en_UK" />
```

```html
<asset:i18n name="texts" locale="${locale}" />
```

## Author

This plugin was written by [Daniel Ellermann](mailto:d.ellermann@amc-world.de)
([AMC World Technologies GmbH][amc-world]).

## License

This plugin was published under the
[Apache License, Version 2.0][apache-license].

[amc-world]: http://www.amc-world.de
[apache-license]: http://www.apache.org/licenses/LICENSE-2.0
[asset-pipeline]: http://www.github.com/bertramdev/asset-pipeline
