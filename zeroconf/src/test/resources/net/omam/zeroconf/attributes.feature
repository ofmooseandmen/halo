Feature: Service attributes
  
  All packets have been generated using jmdns as a reference
  
  From RFC6763:
  - Attribute not present (Absent)
  - Attribute present, with no value (e.g., "passreq" -- password required for this service)
  - Attribute present, with empty value (e.g., "PlugIns=" -- the server supports plugins, but none are presently installed)
  - Attribute present, with non-empty value (e.g., "PlugIns=JPEG,MPEG2,MPEG4")
  - Duplicated keys: only first key is considered, others are ignored
  - Empty keys are ignored

  Scenario: attributes encoding
    Given attributes are created with the following key/value pairs:
      | key  | value   |
      | some | thing   |
      | foo  | bar     |
      |      | ignored |
      | some | ignored |
      | fake |         |
    When the attributes are encoded
    Then the packet shall contain the following bytes:
      | 0xA  | 0x73 | 0x6F | 0x6D | 0x65 | 0x3D | 0x74 | 0x68 |
      | 0x69 | 0x6E | 0x67 | 0x7  | 0x66 | 0x6F | 0x6F | 0x3D |
      | 0x62 | 0x61 | 0x72 | 0x4  | 0x66 | 0x61 | 0x6B | 0x65 |

  Scenario: attributes decoding
    Given the following packet has been received:
      | 0xA  | 0x73 | 0x6F | 0x6D | 0x65 | 0x3D | 0x74 | 0x68 |
      | 0x69 | 0x6E | 0x67 | 0x7  | 0x66 | 0x6F | 0x6F | 0x3D |
      | 0x62 | 0x61 | 0x72 | 0x4  | 0x66 | 0x61 | 0x6B | 0x65 |
    When the packet is decoded into attributes
    Then the following attributes shall be returned:
      | key  | value   |
      | some | thing   |
      | foo  | bar     |
      | fake |         |
