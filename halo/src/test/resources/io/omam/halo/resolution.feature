Feature: Service resolution
  
  Halo implementation tested against JmDNS

  Scenario: Service resolved from cache
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following service has been registered with "JmDNS":
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
    When the service "Living Room Speaker._music._tcp." is resolved by "Halo"
    Then the following resolved service shall be returned:
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |

  Scenario: Service resolved from messages
    Given a "JmDNS" instance has been created
    And the following service has been registered with "JmDNS":
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
    And a "Halo" instance has been created
    When the service "Living Room Speaker._music._tcp." is resolved by "Halo"
    Then the following resolved service shall be returned:
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |

  Scenario: Unresolved service
    Given a "Halo" instance has been created
    When the service "Living Room Speaker._music._tcp." is resolved by "Halo"
    Then no resolved service shall be returned

  Scenario: Service resolved from registered services
    Given a "Halo" instance has been created
    And the following service has been registered with "Halo":
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
    When the service "Living Room Speaker._music._tcp." is resolved by "Halo"
    Then the following resolved service shall be returned:
      | instanceName        | registrationType | port | priority | text      | weight |
      | Living Room Speaker | _music._tcp.     | 9009 |        5 | Some Text |      0 |
