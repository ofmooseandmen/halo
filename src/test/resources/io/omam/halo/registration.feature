Feature: Service registration
  
  Halo implementation tested against JmDNS

  Scenario: Service registered before client started
    Given a "Halo" instance has been created
    And the following services have been registered with "Halo":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    And a "JmDNS" instance has been created
    When the service "Living Room Speaker._music._tcp." is resolved by "JmDNS"
    Then the following resolved services shall be returned:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |

  Scenario: Service registered after client started
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following services have been registered with "Halo":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    When the service "Living Room Speaker._music._tcp." is resolved by "JmDNS"
    Then the following resolved services shall be returned:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |

  Scenario: Service registration with unresolved instance name collision from registered services
    Given a "Halo" instance has been created
    And the following services have been registered with "Halo":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    When the following service is registered with "Halo" not allowing instance name change:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9010 | Some Text |
    Then a "java.io.IOException" shall be thrown with message containing "Registered service collision (...) Living Room Speaker (...) _music._tcp."

  Scenario: Service registration with unresolved instance name collision from cache
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following services have been registered with "JmDNS":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    And the service "Living Room Speaker._music._tcp." has been resolved by "Halo"
    When the following service is registered with "Halo" not allowing instance name change:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9010 | Some Text |
    Then a "java.io.IOException" shall be thrown with message containing "Cache collision (...) Living Room Speaker (...) _music._tcp."

  Scenario: Service registration with conflict during probing
    Given a "JmDNS" instance has been created
    And the following services have been registered with "JmDNS":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    And a "Halo" instance has been created
    When the following service is registered with "Halo" allowing instance name change:
      | instanceName        | registrationType | port | text      | hostname |
      | Living Room Speaker | _music._tcp.     | 9010 | Some Text | FooBar   |
    Then a "java.io.IOException" shall be thrown with message containing "Found conflicts (...) Living Room Speaker (...) _music._tcp."

  Scenario: Service registration with resolved instance name collision
    Given a "Halo" instance has been created
    And the following services have been registered with "Halo":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    And a "JmDNS" instance has been created
    When the following service is registered with "Halo" allowing instance name change:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9010 | Some Text |
      | Living Room Speaker | _music._tcp.     | 9011 | Some Text |
    Then the following registered services shall be returned:
      | instanceName            | registrationType | port | text      |
      | Living Room Speaker     | _music._tcp.     | 9009 | Some Text |
      | Living Room Speaker (2) | _music._tcp.     | 9010 | Some Text |
      | Living Room Speaker (3) | _music._tcp.     | 9011 | Some Text |
    And the service "Living Room Speaker._music._tcp." shall be resolved by "JmDNS"
    And the service "Living Room Speaker (2)._music._tcp." shall be resolved by "JmDNS"
    And the service "Living Room Speaker (3)._music._tcp." shall be resolved by "JmDNS"

  Scenario: Service de-registration
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following services have been registered with "Halo":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    When the service "Living Room Speaker._music._tcp." is de-registered
    Then after at least "PT1S", the service "Living Room Speaker._music._tcp." cannot resolved by "JmDNS"
