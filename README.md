# Localcast

[![travis build status](https://img.shields.io/travis/ofmooseandmen/localcast/master.svg?label=travis+build)](https://travis-ci.org/ofmooseandmen/localcast)
[![codecov.io](https://codecov.io/github/ofmooseandmen/localcast/branches/master/graphs/badge.svg)](https://codecov.io/github/ofmooseandmen/localcast)
[![license](https://img.shields.io/badge/license-BSD3-lightgray.svg)](https://opensource.org/licenses/BSD-3-Clause)

Java FX application to stream music from local folders via Google Chromecast.

## Building from Source

You need [JDK-8](http://openjdk.java.net/projects/jdk8/) or higher to build Localcast.
All modules can be built with Gradle using the following command.

```
./gradlew clean build
```

## Modules

All modules are tested with [cucumber](https://cucumber.io). Feature files can be found in the `src/test/resources` folder of each module.

### [Halo](halo/README.md)

 _[WIP]_ Multicast DNS Service Discovery.

### Chromecast

_[Coming soon]_

### App

_[Coming soon]_
