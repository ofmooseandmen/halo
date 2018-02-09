Feature: Registration types browsing
  
  Halo implementation tested against JmDNS

  Scenario: Registration types browsing
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following services have been registered with "JmDNS":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
      | Bedroom Speaker     | _music._tcp.     | 9010 | Some Text |
      | Living Room Thingy  | _thingy._tcp.    | 9011 | Some Text |
    When the registration types are browsed with "Halo"
    # Note: other DNS services may be running on the machine.
    Then the listener shall be notified of the following registration types:
      | _music._tcp.  |
      | _thingy._tcp. |

  Scenario: Halo supports registration type browsing queries
    Given a "Halo" instance has been created
    And a "JmDNS" instance has been created
    And the following services have been registered with "Halo":
      | instanceName        | registrationType | port | text      |
      | Living Room Speaker | _music._tcp.     | 9009 | Some Text |
      | Bedroom Speaker     | _music._tcp.     | 9010 | Some Text |
      | Living Room Thingy  | _thingy._tcp.    | 9011 | Some Text |
    When the registration types are browsed with "JmDNS"
    # Note: other DNS services may be running on the machine.
    Then the listener shall be notified of the following registration types:
      | _music._tcp.  |
      | _thingy._tcp. |
      