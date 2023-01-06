# PaperMC Example Plugin

This is a reference project. It includes the bare minimum setup for a plugin.

## Build

`./gradlew build`
May take some time as the first time you build the plugin it requires a copy
of the vanilla minecraft server.

The plugin will be located in `build/libs/`

There will be 2 jar files. The one without the `dev` at the end should be used. 
The one ending in `dev` has not been remapped.
