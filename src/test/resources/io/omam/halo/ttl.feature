Feature: DNS record time-to-live

  Scenario Outline: DNS <type> record remaining TTL
    Given a DNS <type> record has been created at '2018-01-20T11:05:00.00Z' with a ttl of 'PT30M'
    When the time is '2018-01-20T11:21:34.00Z'
    Then the DNS record remaining TTL shall be 'PT13M26S'

    Examples: 
      | type |
      | A    |
      | AAAA |
      | PTR  |
      | SRV  |
      | TXT  |

  Scenario Outline: DNS <type> record remaining Zero TTL
    Given a DNS <type> record has been created at '2018-01-20T11:05:00.00Z' with a ttl of 'PT30M'
    When the time is '2018-01-20T11:35:01.00Z'
    Then the DNS record remaining TTL shall be 'PT0S'

    Examples: 
      | type |
      | A    |
      | AAAA |
      | PTR  |
      | SRV  |
      | TXT  |

  Scenario Outline: Non expired DNS <type> record
    Given a DNS <type> record has been created at '2018-01-20T11:05:00.00Z' with a ttl of 'PT30M'
    When the time is '2018-01-20T11:34:59.00Z'
    Then the DNS record is not expired

    Examples: 
      | type |
      | A    |
      | AAAA |
      | PTR  |
      | SRV  |
      | TXT  |

  Scenario Outline: Just expired DNS <type> record
    Given a DNS <type> record has been created at '2018-01-20T11:05:00.00Z' with a ttl of 'PT30M'
    When the time is '2018-01-20T11:35:00.00Z'
    Then the DNS record is expired

    Examples: 
      | type |
      | A    |
      | AAAA |
      | PTR  |
      | SRV  |
      | TXT  |

  Scenario Outline: Expired DNS <type> record
    Given a DNS <type> record has been created at '2018-01-20T11:05:00.00Z' with a ttl of 'PT30M'
    When the time is '2018-01-20T11:35:01.00Z'
    Then the DNS record is expired

    Examples: 
      | type |
      | A    |
      | AAAA |
      | PTR  |
      | SRV  |
      | TXT  |
