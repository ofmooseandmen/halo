Feature: Service attributes
  
  From RFC6763:
  - Attribute not present (Absent)
  - Attribute present, with no value (e.g., "passreq" -- password required for this service)
  - Attribute present, with empty value (e.g., "PlugIns=" -- the server supports plugins, but none are presently installed)
  - Attribute present, with non-empty value (e.g., "PlugIns=JPEG,MPEG2,MPEG4")
  - Duplicated keys: only first key is considered, others are ignored
  - Empty keys are ignored

  Scenario: Attributes encoding
    Given attributes are created with the following key and value pairs:
      | Foo=thing   |
      | Bar=bar     |
      | =ignored    |
      | Foo=ignored |
      | EmptyValue= |
      | NoValue     |
    When the attributes are encoded
    Then the packet shall contain the following bytes:
      | 0x7  | 0x42 | 0x61 | 0x72 | 0x3d | 0x62 | 0x61 | 0x72 |
      | 0x9  | 0x46 | 0x6f | 0x6f | 0x3d | 0x74 | 0x68 | 0x69 |
      | 0x6e | 0x67 | 0x7  | 0x4e | 0x6f | 0x56 | 0x61 | 0x6c |
      | 0x75 | 0x65 | 0xb  | 0x45 | 0x6d | 0x70 | 0x74 | 0x79 |
      | 0x56 | 0x61 | 0x6c | 0x75 | 0x65 | 0x3d |      |      |

  Scenario: Attributes decoding
    Given the following packet has been received:
      | 0x7  | 0x42 | 0x61 | 0x72 | 0x3d | 0x62 | 0x61 | 0x72 |
      | 0x7  | 0x42 | 0x61 | 0x72 | 0x3d | 0x62 | 0x61 | 0x72 |
      | 0x9  | 0x46 | 0x6f | 0x6f | 0x3d | 0x74 | 0x68 | 0x69 |
      | 0x6e | 0x67 | 0x7  | 0x4e | 0x6f | 0x56 | 0x61 | 0x6c |
      | 0x75 | 0x65 | 0xb  | 0x45 | 0x6d | 0x70 | 0x74 | 0x79 |
      | 0x56 | 0x61 | 0x6c | 0x75 | 0x65 | 0x3d | 0x2  | 0x3d |
    When the packet is decoded into attributes
    Then the following attributes shall be returned:
      | Foo=thing   |
      | Bar=bar     |
      | EmptyValue= |
      | NoValue     |
