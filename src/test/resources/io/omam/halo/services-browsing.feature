Feature: Services browsing by registration type
  
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
    Then the listener "music" shall be notified of the following "added" services:
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
    Then the listener "music" shall be notified of the following "added" services:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |

  Scenario: Service with a different registration type is ignored
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following registration types are being browsed with "Halo":
      | registrationType | listenerName |
      | _music._tcp.     | music        |
    When the following services are registered with "JmDNS":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
      | Living Room Stuff   | _stuff._tcp.     | 9010 | Some Text |
    Then the listener "music" shall be notified of the following "added" services:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |

  Scenario: Browse multiple registration types
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following registration types are being browsed with "Halo":
      | registrationType | listenerName |
      | _music._tcp.     | music        |
      | _thingy._udp.    | thingy       |
    When the following services are registered with "JmDNS":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
      | Bedroom Speaker     | _music._tcp.     | 9010 | Some Text |
      | Living Room Thing   | _thingy._udp.    | 9011 | Some Text |
      | Bedroom Thing       | _thingy._udp.    | 9012 | Some Text |
    Then the listener "music" shall be notified of the following "added" services:
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
      | Bedroom Speaker     | _music._tcp.     | 9010 | Some Text |
    And the listener "thingy" shall be notified of the following "added" services:
      | instanceName      | registrationType | port | text      |
      | Living Room Thing | _thingy._udp.    | 9011 | Some Text |
      | Bedroom Thing     | _thingy._udp.    | 9012 | Some Text |

  Scenario: De-registered services are notified to the listener
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following registration types are being browsed with "Halo":
      | registrationType | listenerName |
      | _thingy._udp.    | thingy       |
    And the following services have been registered with "JmDNS":
      | instanceName  | registrationType | port | text      |
      | Bedroom Thing | _thingy._udp.    | 9012 | Some Text |
    When the service "Bedroom Thing._thingy._udp." is de-registered
    Then the listener "thingy" shall be notified of the following "added" services:
      | instanceName  | registrationType | port | text      |
      | Bedroom Thing | _thingy._udp.    | 9012 | Some Text |
    And the listener "thingy" shall be notified of the following "removed" services:
      | instanceName  | registrationType | port | text      |
      | Bedroom Thing | _thingy._udp.    | 9012 | Some Text |

  Scenario: Service registered after browser stopped are not notified to the listener
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following registration types are being browsed with "Halo":
      | registrationType | listenerName |
      | _thingy._udp.    | thingy       |
    And the following services have been registered with "JmDNS":
      | instanceName  | registrationType | port | text      |
      | Bedroom Thing | _thingy._udp.    | 9012 | Some Text |
    And the browser associated with the listener "thingy" has been stopped
    When the following services are registered with "JmDNS":
      | instanceName      | registrationType | port | text      |
      | Living Room Thing | _thingy._udp.    | 9011 | Some Text |
    Then the listener "thingy" shall be notified of the following "added" services:
      | instanceName  | registrationType | port | text      |
      | Bedroom Thing | _thingy._udp.    | 9012 | Some Text |

  Scenario: Halo supports service browsing queries
    Given a "Halo" instance has been created
    And the following services have been registered with "Halo":
      | instanceName        | registrationType | port | text                       |
      | Bedroom Speaker     | _music._tcp.     | 9010 | Hello from the bedroom     |
      | Living Room Speaker | _music._tcp.     | 9011 | Hello from the living room |
    And a "JmDNS" instance has been created
    When the following registration types are browsed with "JmDNS":
      | registrationType | listenerName |
      | _music._tcp.     | music        |
    Then the listener "music" shall be notified of the following "added" services:
      | instanceName        | registrationType | port | text                       |
      | Bedroom Speaker     | _music._tcp.     | 9010 | Hello from the bedroom     |
      | Living Room Speaker | _music._tcp.     | 9011 | Hello from the living room |
