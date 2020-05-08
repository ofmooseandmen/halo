# Halo

[![CI](https://github.com/ofmooseandmen/halo/workflows/CI/badge.svg)](https://github.com/ofmooseandmen/halo/actions?query=workflow%3ACI)
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

Halo is tested with [cucumber](https://cucumber.io) against [JmDNS](https://github.com/jmdns/jmdns). Feature files can be found in the `src/test/resources` folder.

## Usage

### Service Registration

```java
try (final Halo halo = Halo.allNetworkInterfaces(Clock.systemDefaultZone())) {
    // allowing service instance name to be changed and with a default TTL of 1 hour:
    Registered service = halo.register(RegisterableService.create("Foo Bar", "_http._udp.", 8009).get());
    // registered service is returned:
    System.err.println(service);

    // registering again the service instance and registration type will return a service
    // with an instance name of "Foo Bar (2)":
    service = halo.register(RegisterableService.create("Foo Bar", "_http._udp.", 8010).get());
    System.err.println(service);

    // not allowing service instance name to be changed will throw an IOException at this point:
    halo.register(RegisterableService.create("Foo Bar", "_http._udp.", 8011).get(), false);
    
    // if blocking until the service has been announced is not acceptable:
    ExecutorService es = Executors.newSingleThreadExecutor();
    Future<Registered> future =
            es.submit(() -> halo.register(RegisterableService.create("Future", "_http._udp.", 8009).get()));
}
```

### Service Resolution

```java
try (final Halo halo = Halo.allNetworkInterfaces(Clock.systemDefaultZone())) {
    // default timeout of 6 seconds:
    Optional<ResolvedService> service = halo.resolve("Foo Bar", "_http._udp.");
    // Optional contains the service if it could be resolved, empty otherwise:
    System.err.println(service);
    
    // user defined timeout:
    service = halo.resolve("Foo Bar", "_http._udp.", Duration.ofSeconds(1));
    System.err.println(service);

    // if blocking until the service has been resolved is not acceptable:
    ExecutorService es = Executors.newSingleThreadExecutor();
    Future<Optional<ResolvedService>> future = es.submit(() -> halo.resolved("Foo Bar", "_http._udp."));
}
```

### Services Browsing by Registration Type

```java
try (final Halo halo = Halo.allNetworkInterfaces(Clock.systemDefaultZone())) {
    final ServiceBrowserListener l = new ServiceBrowserListener() {

        @Override
        public final void serviceAdded(final ResolvedService service) {
            System.err.println(service + " has been added to the network!!!!!");
        }

        @Override
        public final void serviceRemoved(final ResolvedService service) {
            System.err.println(service + " has been removed from the network!!!!!");
        }

        @Override
        public final void serviceUpdated(final ResolvedService service) {
            System.err.println(service + " has been updated!!!!!");
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

## Configuration
The following parameters can be configured by system properties:

| Property Key                       | Description                                                           | Default     |
| ---------------------------------- | --------------------------------------------------------------------- | ----------- |
| io.omam.halo.mdns.ipv4             | mDNS IPV4 address                                                     | 224.0.0.251 |
| io.omam.halo.mdns.ipv6             | mDNS IPV6 address                                                     | FF02::FB    |
| io.omam.halo.mdns.port             | mDNS port                                                             | 5353        |
| io.omam.halo.resolution.timeout    | resolution timeout in milliseconds                                    | 6000        |
| io.omam.halo.resolution.interval   | interval between resolution questions in milliseconds                 | 200         |
| io.omam.halo.probing.timeout       | probing timeout in milliseconds                                       | 6000        |
| io.omam.halo.probing.interval      | interval between probe messages in milliseconds                       | 250         |
| io.omam.halo.probing.number        | number of probing messages before announcing a registered service     | 3           |
| io.omam.halo.querying.first        | delay before transmitting the first browsing query in milliseconds    | 50          |
| io.omam.halo.querying.delay        | interval between consecutive browsing queries in milliseconds         | 1000        |
| io.omam.halo.querying.increase     | increase factor between consecutive browsing queries                  | 2           |
| io.omam.halo.querying.max          | maximum interval between consecutive browsing queries in milliseconds | 1200000     |
| io.omam.halo.cancellation.interval | interval between goodbye messages in milliseconds                     | 250         |
| io.omam.halo.cancellation.number   | number of goodbye messages sent when de-registering a service         | 3           |
| io.omam.halo.reaper.interval       | cache record reaper interval in milliseconds                          | 10000       |
| io.omam.halo.ttl.default           | DNS record default time to live in milliseconds                       | 3600000     |
| io.omam.halo.ttl.expiry            | DNS record time to live after expiry in milliseconds                  | 1000        |
