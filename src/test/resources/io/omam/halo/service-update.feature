Feature: Service attributes update
  
  The attributes of a service can be updated after it has been announced on the network

  Scenario: Resolution of updated service
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following services have been registered with "JmDNS":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    And the service "Living Room Speaker._music._tcp." has been resolved by "Halo"
    And the attributes of the service "Living Room Speaker._music._tcp." have been updated to "Another Text"
    When the service "Living Room Speaker._music._tcp." is resolved by "Halo"
    Then the following resolved services shall be returned:
      | instanceName        | registrationType | port | text         |
      | Living Room Speaker | _music._tcp.     | 9009 | Another Text |

  Scenario: Listener is notified about updated services
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following services have been registered with "JmDNS":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    And the following registration types are being browsed with "Halo":
      | registrationType | listenerName |
      | _music._tcp.     | music        |
    And the listener "music" has been notified of the following "added" services:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    When the attributes of the service "Living Room Speaker._music._tcp." are updated to "Another Text"
    Then the listener "music" shall be notified of the following "updated" services:
      | instanceName        | registrationType | port | text         |
      | Living Room Speaker | _music._tcp.     | 9009 | Another Text |

  Scenario: A service is re-announced when its attributes are updated
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following services have been registered with "Halo":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
    And the service "Living Room Speaker._music._tcp." has been resolved by "JmDNS"
    And the attributes of the service "Living Room Speaker._music._tcp." have been updated to "Another Text"
    When the service "Living Room Speaker._music._tcp." is resolved by "JmDNS"
    Then the following resolved services shall be returned:
      | instanceName        | registrationType | port | text         |
      | Living Room Speaker | _music._tcp.     | 9009 | Another Text |
