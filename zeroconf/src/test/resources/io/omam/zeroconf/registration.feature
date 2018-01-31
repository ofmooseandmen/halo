Feature: Service registration
  
  Zeroconf implementation tested against JmDNS

  Scenario: Service registered before client started
    Given a "Zeroconf" instance has been created
    And a "JmDNS" instance has been created
    And the following service has been registered with "Zeroconf":
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
    When the service "Living Room Speaker._music._tcp." is resolved by "JmDNS"
    Then the following resolved service shall be returned:
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |

  Scenario: Service registered after client started
    Given a "Zeroconf" instance has been created
    And the following service has been registered with "Zeroconf":
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
    And a "JmDNS" instance has been created
    When the service "Living Room Speaker._music._tcp." is resolved by "JmDNS"
    Then the following resolved service shall be returned:
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |

  Scenario: Service registration with unresolved instance name collision
    Given a "Zeroconf" instance has been created
    And the following service has been registered with "Zeroconf":
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
    When the following service is registered with "Zeroconf" not allowing instance name change:
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9010 |        5 | Some Text |      0 |
    # Note: depending on whether another zeroconf (e.g. Bonjour) service is running on
    #       the machine, the collision can come from the cache or from the registered services.
    Then a "java.io.IOException" shall be thrown with message containing "collision (...) Living Room Speaker (...) _music._tcp."

  Scenario: Service registration with unresolved instance name collision from cache
    Given a "Zeroconf" instance has been created
    And a "JmDNS" instance has been created
    And the following service has been registered with "JmDNS":
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
    And the service "Living Room Speaker._music._tcp." has been resolved by "Zeroconf"
    When the following service is registered with "Zeroconf" not allowing instance name change:
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9010 |        5 | Some Text |      0 |
    Then a "java.io.IOException" shall be thrown with message containing "Cache collision (...) Living Room Speaker (...) _music._tcp."

  Scenario: Service registration with conflict during probing
    Given a "JmDNS" instance has been created
    And the following service has been registered with "JmDNS":
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
    And a "Zeroconf" instance has been created
    When the following service is registered with "Zeroconf" allowing instance name change:
      | instanceName        | registrationType | port | priority | text      | weight | hostname |
      | Living Room Speaker | _music._tcp.     | 9010 |        5 | Some Text |      0 | FooBar   |
    Then a "java.io.IOException" shall be thrown with message containing "Found conflicts (...) Living Room Speaker (...) _music._tcp."

  Scenario: Service registration with resolved instance name collision
    Given a "Zeroconf" instance has been created
    And the following service has been registered with "Zeroconf":
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
    And a "JmDNS" instance has been created
    When the following service is registered with "Zeroconf" allowing instance name change:
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9010 |        5 | Some Text |      0 |
    Then the following registered service shall be returned:
      | instanceName            | registrationType | port | priority | text      | weight |
      | Living Room Speaker (2) | _music._tcp.     | 9010 |        5 | Some Text |      0 |
    And the service "Living Room Speaker._music._tcp." shall be resolved by "JmDNS"
    And the service "Living Room Speaker (2)._music._tcp." shall be resolved by "JmDNS"
