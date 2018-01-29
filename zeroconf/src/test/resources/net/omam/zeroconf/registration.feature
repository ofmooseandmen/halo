Feature: Service registration
  
  Zeroconf implementation tested against JmDNS

  Scenario: Service registered before client started
    Given a "Zeroconf" instance has been created
    And a "JmDNS" instance has been created
    And the following service has been registered with "Zeroconf":
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
    When the service "Living Room Speaker._music._tcp." is resolved by "JmDNS"
    Then the following service shall be returned
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |

  Scenario: Service registered after client started
    Given a "Zeroconf" instance has been created
    And the following service has been registered with "Zeroconf":
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
    And a "JmDNS" instance has been created
    When the service "Living Room Speaker._music._tcp." is resolved by "JmDNS"
    Then the following service shall be returned
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
