Feature: DNS record time-to-live

  Scenario Outline: DNS <type> record remaining TTL
    Given a DNS <type> record has been created at '2018-01-20T11:05:00.00Z' with a ttl of 'PT30M'
    When the time is '2018-01-20T11:21:34.00Z'
    Then the DNS record remaining TTL shall be 'PT13M26S'

    Examples: 
      | type  |
      | A     |
      | AAAA  |
      | PTR   |
      | SRV   |
      | TXT   |

  Scenario Outline: DNS <type> record remaining Zero TTL
    Given a DNS <type> record has been created at '2018-01-20T11:05:00.00Z' with a ttl of 'PT30M'
    When the time is '2018-01-20T11:35:01.00Z'
    Then the DNS record remaining TTL shall be 'PT0S'

    Examples: 
      | type  |
      | A     |
      | AAAA  |
      | PTR   |
      | SRV   |
      | TXT   |

  Scenario Outline: non expired DNS <type> record
    Given a DNS <type> record has been created at '2018-01-20T11:05:00.00Z' with a ttl of 'PT30M'
    When the time is '2018-01-20T11:34:59.00Z'
    Then the DNS record is not expired

    Examples: 
      | type  |
      | A     |
      | AAAA  |
      | PTR   |
      | SRV   |
      | TXT   |

  Scenario Outline: just expired DNS <type> record
    Given a DNS <type> record has been created at '2018-01-20T11:05:00.00Z' with a ttl of 'PT30M'
    When the time is '2018-01-20T11:35:00.00Z'
    Then the DNS record is expired

    Examples: 
      | type  |
      | A     |
      | AAAA  |
      | PTR   |
      | SRV   |
      | TXT   |

  Scenario Outline: expired DNS <type> record
    Given a DNS <type> record has been created at '2018-01-20T11:05:00.00Z' with a ttl of 'PT30M'
    When the time is '2018-01-20T11:35:01.00Z'
    Then the DNS record is expired

    Examples: 
      | type  |
      | A     |
      | AAAA  |
      | PTR   |
      | SRV   |
      | TXT   |

  Scenario Outline: non stale DNS <type> record
    Given a DNS <type> record has been created at '2018-01-20T11:05:00.00Z' with a ttl of 'PT30M'
    When the time is '2018-01-20T11:19:59.00Z'
    Then the DNS record is not stale

    Examples: 
      | type  |
      | A     |
      | AAAA  |
      | PTR   |
      | SRV   |
      | TXT   |

  Scenario Outline: just stale DNS <type> record
    Given a DNS <type> record has been created at '2018-01-20T11:05:00.00Z' with a ttl of 'PT30M'
    When the time is '2018-01-20T11:20:00.00Z'
    Then the DNS record is stale

    Examples: 
      | type  |
      | A     |
      | AAAA  |
      | PTR   |
      | SRV   |
      | TXT   |

  Scenario Outline: stale DNS <type> record
    Given a DNS <type> record has been created at '2018-01-20T11:05:00.00Z' with a ttl of 'PT30M'
    When the time is '2018-01-20T11:20:01.00Z'
    Then the DNS record is stale

    Examples: 
      | type  |
      | A     |
      | AAAA  |
      | PTR   |
      | SRV   |
      | TXT   |
