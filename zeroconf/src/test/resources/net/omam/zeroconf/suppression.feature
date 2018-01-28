Feature: DNS record suppression
  
  A record is suppressed by another record if they share the same service name, type and class, and
  if the other record's TTL is at least half of the reference record's TTL.
  
  A record is suppressed by a message if any of the message answer, authority or additional record
  comply with the above

  Scenario: DNS record suppressed by another record
    Given the following DNS record has been created:
      | serviceName     | recordType | recordClass | ttl  | address       |
      | some.authority. | A          | IN          | PT1H | 192.168.154.0 |
    And the following other DNS record has been created:
      | serviceName     | recordType | recordClass | ttl   | address       |
      | some.authority. | A          | IN          | PT30M | 192.168.154.0 |
    When the record to record suppression check is performed
    Then the DNS record shall be suppressed

  Scenario: DNS record not suppressed by another record due to TTL
    Given the following DNS record has been created:
      | serviceName     | recordType | recordClass | ttl  | address       |
      | some.authority. | A          | IN          | PT1H | 192.168.154.0 |
    And the following other DNS record has been created:
      | serviceName     | recordType | recordClass | ttl   | address       |
      | some.authority. | A          | IN          | PT29M | 192.168.154.0 |
    When the record to record suppression check is performed
    Then the DNS record shall not be suppressed

  Scenario: DNS record not suppressed by another record due to service name
    Given the following DNS record has been created:
      | serviceName      | recordType | recordClass | ttl  | address       |
      | other.authority. | A          | IN          | PT1H | 192.168.154.0 |
    And the following other DNS record has been created:
      | serviceName     | recordType | recordClass | ttl   | address       |
      | some.authority. | A          | IN          | PT29M | 192.168.154.0 |
    When the record to record suppression check is performed
    Then the DNS record shall not be suppressed

  Scenario: DNS record not suppressed by another record due to type
    Given the following DNS record has been created:
      | serviceName      | recordType | recordClass | ttl  | address       |
      | other.authority. | A          | IN          | PT1H | 192.168.154.0 |
    And the following other DNS record has been created:
      | serviceName     | recordType | recordClass | ttl   | address                                 |
      | some.authority. | AAAA       | IN          | PT29M | 2001:0db8:85a3:0000:0000:8a2e:0370:7334 |
    When the record to record suppression check is performed
    Then the DNS record shall not be suppressed

  Scenario: DNS record not suppressed by another record due to class
    Given the following DNS record has been created:
      | serviceName      | recordType | recordClass | ttl  | address       |
      | other.authority. | A          | IN          | PT1H | 192.168.154.0 |
    And the following other DNS record has been created:
      | serviceName     | recordType | recordClass | ttl   | address       |
      | some.authority. | A          | ANY         | PT29M | 192.168.154.0 |
    When the record to record suppression check is performed
    Then the DNS record shall not be suppressed

  Scenario: DNS record suppressed by message due to answer
    Given the following DNS record has been created:
      | serviceName | recordType | recordClass | ttl  | text      |
      | other.foo.  | TXT        | IN          | PT1H | some text |
    And a DNS query has been created
    And the following answers have been added:
      | serviceName | recordType | recordClass | ttl  | text      |
      | other.foo.  | TXT        | IN          | PT1H | some text |
    When the record to message suppression check is performed
    Then the DNS record shall be suppressed

  Scenario: DNS record suppressed by message due to authority
    Given the following DNS record has been created:
      | serviceName | recordType | recordClass | ttl  | text      |
      | other.foo.  | TXT        | IN          | PT1H | some text |
    And a DNS query has been created
    And the following authorities have been added:
      | serviceName | recordType | recordClass | ttl  | text      |
      | other.foo.  | TXT        | IN          | PT1H | some text |
    When the record to message suppression check is performed
    Then the DNS record shall be suppressed

  Scenario: DNS record suppressed by message due to additional
    Given the following DNS record has been created:
      | serviceName | recordType | recordClass | ttl  | target      |
      | other.foo.  | PTR        | IN          | PT1H | some target |
    And a DNS query has been created
    And the following additional have been added:
      | serviceName | recordType | recordClass | ttl  | target      |
      | other.foo.  | PTR        | IN          | PT1H | some target |
    When the record to message suppression check is performed
    Then the DNS record shall be suppressed
