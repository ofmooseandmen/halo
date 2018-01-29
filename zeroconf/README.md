# Zeroconf

[![license](https://img.shields.io/badge/license-BSD3-lightgray.svg)](https://opensource.org/licenses/BSD-3-Clause)

A pure java implementation of the [Zero Configuration Networking protocol](http://www.zeroconf.org).
Supports service resolution, registration and browsing.

## Building from Source

You need [JDK-8](http://openjdk.java.net/projects/jdk8/) or higher to build Localcast.
All modules can be built with Gradle using the following command.

```
gradlew clean build
```

## Usage

### Resolution

```java
try (final Zeroconf zc = Zeroconf.allNetworkInterfaces(Clock.systemDefaultZone())) {
    // using a timeout of 6 seconds.
    Optional<Service> service = zc.resolve("Foo Bar", "_http._udp.");
    // Optional contains the service if it could be resolved, empty otherwise
    System.err.println(service);
    
    // using custom timeout
    Optional<Service> service = zc.resolve("Foo Bar", "_http._udp.", Duration.ofSeconds(1));
    System.err.println(service);
}
```
