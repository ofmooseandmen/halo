Feature: Service resolution
  
  Zeroconf implementation tested against JmDNS

  Scenario: service resolved from cache
    Given a "Zeroconf" instance has been created
    And a "JmDNS" instance has been created
    And the following service has been registered with "JmDNS":
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
    When the service "Living Room Speaker._music._tcp." is resolved by "Zeroconf"
    Then the following service shall be returned
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |

  Scenario: service resolved from messages
    Given a "JmDNS" instance has been created
    And the following service has been registered with "JmDNS":
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
    And a "Zeroconf" instance has been created
    When the service "Living Room Speaker._music._tcp." is resolved by "Zeroconf"
    Then the following service shall be returned
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |

  Scenario: unresolved service
    Given a "Zeroconf" instance has been created
    When the service "Living Room Speaker._music._tcp." is resolved by "Zeroconf"
    Then no service shall be returned
