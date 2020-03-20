# tnoodle-server module

This part of the TNoodle code contains core model classes and links to the actual scrambling code.

**PLEASE NOTE**: This core server only controls the bare-bone setup for linking to TNoodle-LIB. For actual server _implementations_, go to either the `webscrambles` or `cloudscrambles` module.

## Packages

### config

Common configuration providers. Should not be of any particular interest.

### crypto

Loads basic cryptography functions and keys for allowing the user to apply ciphers to computed scrambles.
Can also verify the RSA signature attached to an official build.

### model

Registers all known puzzles, formats and events.
Provides adapters to the puzzles present in TNoodle-LIB

### routing

Adds some very basic routing functions like the version check.

### serial

Configures the JSON serialization mechanism for [KotlinX serialization](https://github.com/Kotlin/kotlinx.serialization)

### util

Reading stuff out of a JAR file while it's running.
