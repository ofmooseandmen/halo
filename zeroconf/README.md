# Zeroconf

[![license](https://img.shields.io/badge/license-BSD3-lightgray.svg)](https://opensource.org/licenses/BSD-3-Clause)

A pure java implementation of the [Zero Configuration Networking protocol](http://www.zeroconf.org).
Supports service resolution, registration and browsing.

## Building from Source

You need [JDK-8](http://openjdk.java.net/projects/jdk8/) or higher to build Zeroconf.
Zeroconf can be built with Gradle using the following command:

```
./gradlew clean build
```

## Usage

### Service Resolution

```java
try (final Zeroconf zc = Zeroconf.allNetworkInterfaces(Clock.systemDefaultZone())) {
    // using a timeout of 6 seconds.
    Optional<Service> service = zc.resolve("Foo Bar", "_http._udp.");
    // Optional contains the service if it could be resolved, empty otherwise.
    System.err.println(service);
    
    // using custom timeout.
    service = zc.resolve("Foo Bar", "_http._udp.", Duration.ofSeconds(1));
    System.err.println(service);
}
```

### Service Registration

```java
try (final Zeroconf zc = Zeroconf.allNetworkInterfaces(Clock.systemDefaultZone())) {
    // allowing service instance name to be changed and with a TTL of 1 hour.
    Service service = zc.register(Service.create("Foo Bar", "_http._udp.", (short) 8009).get());
    // registered service is returned.
    System.err.println(service);

    // registering again the service instance and registration type will return a service
    // with an instance name of "Foo Bar (2)".
    service = zc.register(Service.create("Foo Bar", "_http._udp.", (short) 8010).get());
    System.err.println(service);

    // not allowing service instance name to be changed will throw an IOException at this point.
    zc.register(Service.create("Foo Bar", "_http._udp.", (short) 8011).get(), false);
}
```
