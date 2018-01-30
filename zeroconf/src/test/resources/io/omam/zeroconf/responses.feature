Feature: DNS responses
  
  All packets have been generated using jmdns as a reference

  Scenario: Packet from outgoing DNS query with an answer
    Given a DNS response has been created
    And the following answers have been added:
      | serviceName    | recordType | recordClass | ttl   | port | priority | server      | weight |
      | foo.bar.local. | SRV        | IN          | PT45M | 8008 |      154 | server.net. |      2 |
    When the DNS message is encoded
    Then the packet shall contain the following bytes:
      | 0x0  | 0x0  | 0x80 | 0x0  | 0x0  | 0x0  | 0x0  | 0x1  |
      | 0x0  | 0x0  | 0x0  | 0x0  | 0x3  | 0x66 | 0x6f | 0x6f |
      | 0x3  | 0x62 | 0x61 | 0x72 | 0x5  | 0x6c | 0x6f | 0x63 |
      | 0x61 | 0x6c | 0x0  | 0x0  | 0x21 | 0x0  | 0x1  | 0x0  |
      | 0x0  | 0xa  | 0x8c | 0x0  | 0x12 | 0x0  | 0x9a | 0x0  |
      | 0x2  | 0x1f | 0x48 | 0x6  | 0x73 | 0x65 | 0x72 | 0x76 |
      | 0x65 | 0x72 | 0x3  | 0x6e | 0x65 | 0x74 | 0x0  |      |

  Scenario: DNS response with an answer from incoming packet
    Given the following packet has been received:
      | 0x0  | 0x0  | 0x80 | 0x0  | 0x0  | 0x0  | 0x0  | 0x1  |
      | 0x0  | 0x0  | 0x0  | 0x0  | 0x3  | 0x66 | 0x6f | 0x6f |
      | 0x3  | 0x62 | 0x61 | 0x72 | 0x5  | 0x6c | 0x6f | 0x63 |
      | 0x61 | 0x6c | 0x0  | 0x0  | 0x21 | 0x0  | 0x1  | 0x0  |
      | 0x0  | 0xa  | 0x8c | 0x0  | 0x12 | 0x0  | 0x9a | 0x0  |
      | 0x2  | 0x1f | 0x48 | 0x6  | 0x73 | 0x65 | 0x72 | 0x76 |
      | 0x65 | 0x72 | 0x3  | 0x6e | 0x65 | 0x74 | 0x0  |      |
    When the packet is decoded into a DNS message
    Then a DNS response with "QR_RESPONSE" flags shall be returned
    And it contains no question
    And it contains the following answers:
      | serviceName    | recordType | recordClass | ttl   | port | priority | server      | weight |
      | foo.bar.local. | SRV        | IN          | PT45M | 8008 |      154 | server.net. |      2 |
    And it contains no authority
    And it contains no additional