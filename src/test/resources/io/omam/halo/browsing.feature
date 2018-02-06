Feature: Service browsing by registration type
  
  Halo implementation tested against JmDNS

  Scenario: Service registered before browsing
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following services have been registered with "JmDNS":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    When the following registration types are browsed with "Halo":
      | registrationType | listenerName |
      | _music._tcp.     | music        |
    Then the listener "music" shall be notified of the following "up" services:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |

  Scenario: Service registered after browsing
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following registration types are being browsed with "Halo":
      | registrationType | listenerName |
      | _music._tcp.     | music        |
    When the following services are registered with "JmDNS":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    Then the listener "music" shall be notified of the following "up" services:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
