# Halo

[![travis build status](https://img.shields.io/travis/ofmooseandmen/halo/master.svg?label=travis+build)](https://travis-ci.org/ofmooseandmen/halo)
[![appveyor build status](https://ci.appveyor.com/api/projects/status/wugjgsm3gs4jothg?svg=true)](https://ci.appveyor.com/project/ofmooseandmen/halo)
[![codecov.io](https://codecov.io/github/ofmooseandmen/halo/branches/master/graphs/badge.svg)](https://codecov.io/github/ofmooseandmen/halo)
[![license](https://img.shields.io/badge/license-BSD3-lightgray.svg)](https://opensource.org/licenses/BSD-3-Clause)

> __Halo__ [_Javanese_] is __Bonjour__ [_French_] is __Hello__ [_English_]

[Multicast DNS Service Discovery](http://en.wikipedia.org/wiki/Multicast_DNS) implementation in pure java.

## Building from Source

You need [JDK-8](http://openjdk.java.net/projects/jdk8/) or higher to build Halo.
Halo can be built with Gradle using the following command:

```
./gradlew clean build
```

## Tests

Halo is tested with [cucumber](https://cucumber.io). Feature files can be found in the `src/test/resources` folder.

## Usage

### Service Registration

```java
try (final Halo halo = Halo.allNetworkInterfaces(Clock.systemDefaultZone())) {
    // allowing service instance name to be changed and with a default TTL of 1 hour.
    Service service = halo.register(Service.create("Foo Bar", "_http._udp.", (short) 8009).get());
    // registered service is returned.
    System.err.println(service);

    // registering again the service instance and registration type will return a service
    // with an instance name of "Foo Bar (2)".
    service = halo.register(Service.create("Foo Bar", "_http._udp.", (short) 8010).get());
    System.err.println(service);

    // not allowing service instance name to be changed will throw an IOException at this point.
    halo.register(Service.create("Foo Bar", "_http._udp.", (short) 8011).get(), false);
}
```

### Service Resolution

```java
try (final Halo halo = Halo.allNetworkInterfaces(Clock.systemDefaultZone())) {
    // default timeout of 6 seconds.
    Optional<Service> service = halo.resolve("Foo Bar", "_http._udp.");
    // Optional contains the service if it could be resolved, empty otherwise.
    System.err.println(service);
    
    // user defined timeout.
    service = halo.resolve("Foo Bar", "_http._udp.", Duration.ofSeconds(1));
    System.err.println(service);
}
```
