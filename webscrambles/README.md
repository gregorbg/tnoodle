# webscrambles module

This part of the TNoodle code is the backend that serves the user interface at `tnoodle-ui`.

It is entirely written in [`Kotlin`](https://kotlinlang.org/) and is based on the [`ktor`](https://ktor.io/) framework.
You should have a basic understanding of these two technologies before proceeding.

## Packages

This section aims to give a general overview over the packages in the source code. Specific details will most likely be documented in the source code itself.

### pdf

PDF drawing code. Currently still very complex and heavily customised due to the fact that we are using legacy itext5 architecture.
Try not to touch this unless you absolutely have to.

### platform

Includes platform-specific utility code that is responsible for wrapping the JAR executable on Windows.
Also handles tray icon images and browser calls.

### routing

Sets up the routes for ktor to handle incoming client requests. For the most part, there is no computation logic happening directly.
Instead, the philosophy is for the routers to dispatch the requests to some other part of the code and return its result.

### serial

Contains convenience methods and helper classes for our serialization mechanism.
Take a look at [KotlinX serialization](https://github.com/Kotlin/kotlinx.serialization) if you want to learn more.

### wcif

The TNoodle communication protocol is entirely based on [WCIF](https://github.com/thewca/wcif). This package includes model classes for serialization,
as well as the logic for processing data within the WCIF object.

**Computing scrambles for a competition happens in `WCIFScrambleMatcher`**

The general handling of model objects is built around the philosophy of immutable state, to avoid race conditions.
Please try to adhere to the idea of copying existing objects instead of mutating existing ones. Avoid `var` keywords wherever you can!

### zip

The DSL that is used to create ZIP files. Every sub-folder has its own wrapping class to avoid the "god class" antipattern.

## Remarks

- The entry point to the application is in `WebscramblesServer`. Specifically, our ktor installation is configured to look for the `spinUp()` method. Refer to `resources/application.conf` if you want to find out how. This file is also required by ktor for the `commmandLineEnvironment` call to work.
- Translations are copy/pasta from the way ruby does i18n. Do not try to understand the `Map<String, Map<*, *>>` horrors.
- Extensions are handled specifically, because they have a dynamic (JSON-esque) and static (Kotlin) representation. You can convert between them via `parsedData` and `build` methods.
