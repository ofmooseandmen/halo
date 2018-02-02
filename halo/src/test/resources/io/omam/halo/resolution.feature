Feature: Service resolution
  
  Halo implementation tested against JmDNS

  Scenario: Service resolved from cache
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following service has been registered with "JmDNS":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    When the service "Living Room Speaker._music._tcp." is resolved by "Halo"
    Then the following resolved service shall be returned:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |

  Scenario: Service resolved from messages
    Given a "JmDNS" instance has been created
    And the following service has been registered with "JmDNS":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    And a "Halo" instance has been created
    When the service "Living Room Speaker._music._tcp." is resolved by "Halo"
    Then the following resolved service shall be returned:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |

  Scenario: Unresolved service
    Given a "Halo" instance has been created
    When the service "Living Room Speaker._music._tcp." is resolved by "Halo"
    Then no resolved service shall be returned

  Scenario: Service resolved from registered services
    Given a "Halo" instance has been created
    And the following service has been registered with "Halo":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    When the service "Living Room Speaker._music._tcp." is resolved by "Halo"
    Then the following resolved service shall be returned:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
