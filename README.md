# Halo

[![travis build status](https://img.shields.io/travis/ofmooseandmen/halo/master.svg?label=travis+build)](https://travis-ci.org/ofmooseandmen/halo)
[![appveyor build status](https://ci.appveyor.com/api/projects/status/wugjgsm3gs4jothg?svg=true)](https://ci.appveyor.com/project/ofmooseandmen/halo)
[![codecov.io](https://codecov.io/github/ofmooseandmen/halo/branches/master/graphs/badge.svg)](https://codecov.io/github/ofmooseandmen/halo)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.ofmooseandmen/halo.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.ofmooseandmen%22%20AND%20a%3A%22halo%22)
[![Javadocs](https://www.javadoc.io/badge/com.github.ofmooseandmen/halo.svg?color=lightgrey)](https://www.javadoc.io/doc/com.github.ofmooseandmen/halo)
[![license](https://img.shields.io/badge/license-BSD3-lightgray.svg)](https://opensource.org/licenses/BSD-3-Clause)

> __Halo__ [_Javanese_] is __Bonjour__ [_French_] is __Hello__ [_English_]

An implementation of [Multicast DNS Service Discovery](https://en.wikipedia.org/wiki/Zero-configuration_networking#Service_discovery) in Java.

## Building from Source

You need [JDK-8](http://jdk.java.net/8) or higher to build Halo.
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

### Services Browsing by Registration Type

```java
try (final Halo halo = Halo.allNetworkInterfaces(Clock.systemDefaultZone())) {
    final ServiceBrowserListener l = new ServiceBrowserListener() {

        @Override
        public final void down(final Service service) {
            System.err.println(service + " is down!!!!!");
        }

        @Override
        public final void up(final Service service) {
            System.err.println(service + " is up!!!!!");
        }
    };

    final Browser browser = halo.browse("_http._udp.", l);

    // Wait for some services to be registered on the network...
    Thread.sleep(5000);

    browser.close();
}
```

### Registration Types Browsing

```java
try (final Halo halo = Halo.allNetworkInterfaces(Clock.systemDefaultZone())) {
    final Browser browser = halo.browse(System.err::printLn);

    // Wait for some services to be registered on the network...
    Thread.sleep(5000);

    browser.close();
}
```
