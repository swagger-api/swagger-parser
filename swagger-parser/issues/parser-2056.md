# Issue Analysis: OpenAPI 3.1.0 Definition Parser Result Drops the License Identifier

## Issue Overview
**Issue Number:** #2056  
**Title:** OpenAPI 3.1.0 definition parser result drops the license identifier  
**Status:** Open  
**Created:** 2024  
**URL:** https://github.com/swagger-api/swagger-parser/issues/2056

## Summary

When parsing an OpenAPI 3.1.0 specification containing a `license.identifier` field and then serializing the parsed result back to JSON using `Json.pretty()`, the `identifier` field is dropped from the output. This issue affects the `swagger-parser` version 2.1.20 and represents a critical loss of metadata during the parse-serialize cycle.

The `license.identifier` field is a new addition in OpenAPI 3.1.0 specification that allows specifying an SPDX license identifier as an alternative to the `license.url` field. This field should be preserved during parsing and serialization operations.

## Problem Statement

### Reproduction Case

Using the following code snippet to parse and pretty-print an OpenAPI 3.1.0 specification:

```java
String openApiFilePath = "<Path>/Petstore-3.1.0.yaml";
OpenAPIV3Parser openAPIParser = new OpenAPIV3Parser();
SwaggerParseResult parseResult = openAPIParser.readLocation(openApiFilePath, null, null);
String parser = Json.pretty(parseResult.getOpenAPI());
```

### Sample OpenAPI Specification

```yaml
openapi: 3.1.0
info:
  title: Sample API
  version: 1.0.0
  description: This is a sample API specification with license identifier.
  license:
    name: Apache 2.0
    identifier: Apache-2.0
  contact:
    name: API Support
    email: support@example.com
    url: https://www.example.com/support
```

**Expected Behavior:** The `identifier: Apache-2.0` field should be preserved in the serialized output.

**Actual Behavior:** The `identifier` field is dropped from the serialized JSON output, resulting in loss of license metadata.

## Root Cause Analysis

The root cause of this issue lies in the **serialization layer** of the swagger-core library (version 2.2.37), not in the swagger-parser's deserialization logic. The analysis reveals:

### 1. Deserialization Works Correctly

The `OpenAPIDeserializer` class properly handles the `license.identifier` field for OpenAPI 3.1.0 specifications:

**File:** `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/util/OpenAPIDeserializer.java`

**Lines 1340-1353:**
```java
if (result.isOpenapi31()) {
    // either the url must be set or the identifier but not both
    boolean needsIdentifier = license.getUrl() == null;
    value = getString("identifier", node, needsIdentifier, location, result);
    
    if (StringUtils.isNotBlank(value)) {
        if (!needsIdentifier) {
            result.extra(location, "identifier", node);
            result.invalid();
        } else {
            license.setIdentifier(value);  // ✓ Field is correctly set
        }
    }
}
```

The parser correctly:
- Detects OpenAPI 3.1.0 version (line 336-337)
- Recognizes `LICENSE_KEYS_31` includes "identifier" (line 144)
- Deserializes and sets the identifier value on the License object
- Validates mutual exclusivity between `url` and `identifier`

**Evidence:** Test file `modules/swagger-parser-v3/src/test/resources/3.1.0/test/basicOAS31.yaml` contains:
```yaml
license:
  name: MIT
  identifier: test identifier
```

And the test `testBasicOAS31()` in `OAI31DeserializationTest.java` successfully asserts:
```java
assertNotNull(openAPI.getInfo().getLicense().getIdentifier());
assertEquals(openAPI.getInfo().getLicense().getIdentifier(), "test identifier");
```

### 2. Serialization Issue in swagger-core

The problem occurs during serialization when `Json.pretty()` is called. The `Json` class is part of the **swagger-core** library (io.swagger.v3.core.util.Json), not swagger-parser.

**Potential causes:**

1. **Missing or Incorrect Jackson Annotations:** The `License` model class in swagger-core may not have proper Jackson annotations for the `identifier` field, particularly the `@JsonInclude` or version-specific serialization annotations.

2. **OpenAPI Version Context Loss:** The serialization process may not have access to the OpenAPI version information (3.0 vs 3.1), causing it to serialize using OpenAPI 3.0 rules which don't include the `identifier` field.

3. **Jackson Mixin Configuration:** The swagger-core library may use Jackson mixins for controlling serialization, and the mixin for the License class may not include the `identifier` field.

4. **@OpenAPI31 Annotation Handling:** The `identifier` field in the License model is marked with `@OpenAPI31` annotation (from swagger-core), but the serialization logic may not properly handle this annotation to include the field when serializing OpenAPI 3.1 documents.

## Affected Components

### Primary Components

- **Module:** swagger-parser-v3
- **External Dependency:** swagger-core (version 2.2.37)
- **File(s):**
  - `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/util/OpenAPIDeserializer.java` (Lines 1319-1371, getLicense method)
  - External: `io.swagger.v3.oas.models.info.License` (swagger-core)
  - External: `io.swagger.v3.core.util.Json` (swagger-core)
  
- **Class(es):**
  - `io.swagger.v3.parser.util.OpenAPIDeserializer` - Handles deserialization (✓ Working)
  - `io.swagger.v3.oas.models.info.License` - License model (❌ Serialization issue)
  - `io.swagger.v3.core.util.Json` - JSON serialization utility (❌ Serialization issue)
  - `io.swagger.v3.parser.core.models.SwaggerParseResult` - Stores parse result and OpenAPI version flag

### Related Code Sections

**OpenAPI Version Detection (Lines 331-343):**
```java
String value = getString("openapi", rootNode, true, location, result);

// we don't even try if the version isn't there
if (value == null || (!value.startsWith("3.0") && !value.startsWith("3.1"))) {
    return null;
} else if (value.startsWith("3.1")) {
    result.openapi31(true);  // ✓ Version correctly detected
    openAPI.setSpecVersion(SpecVersion.V31);
}
```

**License Keys Definition:**
- **OpenAPI 3.0** (Line 89): `LICENSE_KEYS = {"name", "url"}`
- **OpenAPI 3.1** (Line 144): `LICENSE_KEYS_31 = {"name", "url", "identifier"}`

## Technical Details

### Current Behavior

1. **Parsing Phase (✓ Works Correctly):**
   - OpenAPI 3.1.0 specification is loaded
   - Version is detected as 3.1.0
   - `result.openapi31(true)` is set
   - License object is deserialized with identifier field populated
   - `license.getIdentifier()` returns the correct value

2. **Serialization Phase (❌ Fails):**
   - `Json.pretty(openAPI)` is called from swagger-core
   - The License object is serialized to JSON
   - The `identifier` field is omitted from the output
   - Only `name` (and `url` if present) are included

### Expected Behavior

The serialization should:
1. Detect that the OpenAPI object is version 3.1.0
2. Include all OpenAPI 3.1.0-specific fields during serialization
3. Serialize the `license.identifier` field when present
4. Produce output that matches the input specification structure

### OpenAPI 3.1.0 vs 3.0.x License Object Differences

**OpenAPI 3.0.x License Object:**
```yaml
license:
  name: Apache 2.0                    # REQUIRED
  url: https://www.apache.org/licenses/LICENSE-2.0.html  # OPTIONAL
```

**OpenAPI 3.1.0 License Object:**
```yaml
license:
  name: Apache 2.0                    # REQUIRED
  url: https://www.apache.org/licenses/LICENSE-2.0.html  # OPTIONAL (mutually exclusive with identifier)
  identifier: Apache-2.0              # OPTIONAL (mutually exclusive with url) - NEW in 3.1
```

**Key Changes:**
- The `identifier` field is an SPDX license identifier (e.g., "MIT", "Apache-2.0", "GPL-3.0")
- Either `url` OR `identifier` should be specified, not both
- The `identifier` field provides a standardized, machine-readable license reference
- Validation in OpenAPIDeserializer enforces mutual exclusivity (lines 1340-1353)

### Validation Tests

**Test Case 1 - Valid identifier only (OpenAPI 3.1):**
```yaml
license:
  name: MIT
  identifier: MIT
```
✓ Deserialization: PASS  
❌ Serialization: FAIL (identifier dropped)

**Test Case 2 - Both url and identifier (OpenAPI 3.1):**
```yaml
license:
  name: test
  url: http://example.com
  identifier: test
```
✓ Validation: FAIL (correctly reports error - mutually exclusive)
Tested in: `OpenAPIDeserializerTest.testIdentifierAndUrlInvalid()`

**Test Case 3 - identifier in OpenAPI 3.0:**
```yaml
openapi: 3.0.1
license:
  name: test
  identifier: test
```
✓ Validation: FAIL (correctly reports "identifier" as unexpected for 3.0)
Tested in: Multiple OpenAPIDeserializerTest cases

## Impact Assessment

**Severity:** **High**

**Justification:**
- **Data Loss:** Critical metadata (license identifier) is silently dropped during round-trip parsing
- **Specification Compliance:** Violates OpenAPI 3.1.0 specification requirements
- **Downstream Impact:** Tools relying on parsed OpenAPI objects lose important licensing information
- **Legal/Compliance Risk:** SPDX identifiers are used for license compliance automation; losing this data can cause compliance issues

**Affected Users:**
- Developers using OpenAPI 3.1.0 specifications with SPDX license identifiers
- API documentation generators that parse and re-serialize OpenAPI specs
- License compliance tools that rely on SPDX identifiers
- Organizations migrating from OpenAPI 3.0 to 3.1 with identifier-based licensing
- Code generators that consume OpenAPI specifications

**Workarounds:**

1. **Use license.url instead of identifier** (for OpenAPI 3.0 compatibility):
   ```yaml
   license:
     name: Apache 2.0
     url: https://www.apache.org/licenses/LICENSE-2.0.html
   ```
   - **Limitation:** Loses SPDX standardization benefits

2. **Access the License object directly** without re-serialization:
   ```java
   SwaggerParseResult result = parser.readLocation(path, null, null);
   License license = result.getOpenAPI().getInfo().getLicense();
   String identifier = license.getIdentifier(); // Works correctly
   // Don't call Json.pretty() on the OpenAPI object
   ```
   - **Limitation:** Cannot generate complete JSON/YAML output

3. **Post-process the serialized JSON** to add identifier back:
   ```java
   SwaggerParseResult result = parser.readLocation(path, null, null);
   String identifier = result.getOpenAPI().getInfo().getLicense().getIdentifier();
   String json = Json.pretty(result.getOpenAPI());
   // Manually inject identifier into JSON (fragile and error-prone)
   ```
   - **Limitation:** Fragile, requires JSON parsing/manipulation

4. **Use custom Jackson ObjectMapper** with proper configuration:
   ```java
   ObjectMapper mapper = new ObjectMapper();
   // Configure to include all fields for OpenAPI 3.1
   String json = mapper.writerWithDefaultPrettyPrinter()
                       .writeValueAsString(result.getOpenAPI());
   ```
   - **Limitation:** May still require custom serialization mixins

## Proposed Solution

### Solution 1: Fix in swagger-core (Recommended)

**Description:** Update the swagger-core library to properly serialize the `identifier` field for OpenAPI 3.1.0 specifications.

**Implementation Approach:**

1. **Update License Model Jackson Annotations:**
   - Ensure the `identifier` field has proper `@JsonProperty("identifier")` annotation
   - Verify `@JsonInclude` is set appropriately (e.g., `JsonInclude.Include.NON_NULL`)

2. **Version-Aware Serialization:**
   - Modify Jackson serialization configuration to respect OpenAPI version
   - Use custom serializers or mixins that check the OpenAPI spec version
   - Include/exclude fields based on version (3.0 vs 3.1)

3. **Add Serialization Tests:**
   ```java
   @Test
   public void testLicenseIdentifierSerialization31() {
       OpenAPI openAPI = new OpenAPI();
       openAPI.setSpecVersion(SpecVersion.V31);
       Info info = new Info();
       License license = new License();
       license.setName("MIT");
       license.setIdentifier("MIT");
       info.setLicense(license);
       openAPI.setInfo(info);
       
       String json = Json.pretty(openAPI);
       
       assertTrue(json.contains("\"identifier\" : \"MIT\""));
       assertFalse(json.contains("url")); // url should not be present
   }
   ```

4. **Update swagger-parser dependency:**
   - Upgrade to fixed swagger-core version
   - Update pom.xml: `<swagger-core-version>2.2.x</swagger-core-version>`

**Pros:**
- ✅ Fixes root cause
- ✅ Benefits all swagger-core users
- ✅ Maintains backward compatibility
- ✅ Centralized fix

**Cons:**
- ❌ Requires changes in external library (swagger-core)
- ❌ Depends on swagger-core release cycle
- ❌ Requires coordination with swagger-core maintainers

**Estimated Effort:** Medium (requires coordination with swagger-core team)

### Solution 2: Workaround in swagger-parser

**Description:** Implement custom serialization in swagger-parser as a temporary workaround until swagger-core is fixed.

**Implementation Approach:**

1. **Add Custom Serialization Utility:**
   ```java
   public class OpenAPI31Serializer {
       public static String prettyPrint(OpenAPI openAPI) {
           if (openAPI.getSpecVersion() == SpecVersion.V31) {
               ObjectMapper mapper = createOpenAPI31Mapper();
               return mapper.writerWithDefaultPrettyPrinter()
                           .writeValueAsString(openAPI);
           }
           return Json.pretty(openAPI);
       }
       
       private static ObjectMapper createOpenAPI31Mapper() {
           // Create mapper with OpenAPI 3.1 mixins/serializers
           // ensuring all 3.1 fields are included
       }
   }
   ```

2. **Update Documentation:**
   - Document the issue with `Json.pretty()`
   - Recommend using custom serializer for OpenAPI 3.1
   - Add migration guide for OpenAPI 3.0 → 3.1 users

3. **Add Warning/Logging:**
   - Detect when OpenAPI 3.1 object is serialized
   - Log warning about potential field loss
   - Suggest using workaround serializer

**Pros:**
- ✅ Can be implemented immediately in swagger-parser
- ✅ Provides solution for users
- ✅ Doesn't wait for external library updates

**Cons:**
- ❌ Workaround, not a real fix
- ❌ Maintenance burden
- ❌ Duplicates serialization logic
- ❌ May diverge from swagger-core over time

**Estimated Effort:** Low-Medium

### Solution 3: Upstream Fix Request (Parallel with Solution 1)

**Description:** File an issue with swagger-core repository and contribute a fix.

**Implementation Approach:**

1. **Create swagger-core Issue:**
   - Document the serialization problem
   - Provide reproduction case
   - Reference this issue (#2056)

2. **Submit Pull Request to swagger-core:**
   - Implement proper Jackson annotations
   - Add version-aware serialization
   - Include comprehensive tests
   - Follow swagger-core contribution guidelines

3. **Update swagger-parser:**
   - Once fixed in swagger-core, bump dependency version
   - Add integration tests
   - Update documentation

**Pros:**
- ✅ Proper fix at the source
- ✅ Community contribution
- ✅ Benefits entire ecosystem
- ✅ Long-term sustainable solution

**Cons:**
- ❌ Requires approval and merge from swagger-core maintainers
- ❌ Timeline dependent on external team
- ❌ May require multiple iterations

**Estimated Effort:** Medium-High

## Alternatives Considered

### Alternative 1: Ignore OpenAPI Version During Serialization

**Approach:** Always serialize all fields regardless of OpenAPI version.

**Rejected Because:**
- Violates OpenAPI 3.0 specification (identifier not valid in 3.0)
- Would generate invalid OpenAPI 3.0 documents
- Breaks backward compatibility
- Fails validation in strict parsers

### Alternative 2: Store Identifier in Extensions

**Approach:** Store identifier as `x-license-identifier` extension field.

**Rejected Because:**
- Not compliant with OpenAPI 3.1 specification
- Loses standardization benefits of SPDX identifiers
- Requires custom tooling to extract
- Defeats purpose of official identifier field

### Alternative 3: Deprecate Json.pretty() for OpenAPI 3.1

**Approach:** Recommend users not use `Json.pretty()` with OpenAPI 3.1.

**Rejected Because:**
- Poor user experience
- Doesn't solve the underlying problem
- Limits library functionality
- Users expect serialization to work

## Dependencies

### Related Issues

- This issue is specific to OpenAPI 3.1.0 support in swagger-parser
- May be related to other OpenAPI 3.1-specific serialization issues
- Potentially affects other new 3.1 fields:
  - `info.summary`
  - `jsonSchemaDialect`
  - `webhooks`
  - New schema keywords (prefixItems, $id, etc.)

### External Dependencies

- **swagger-core 2.2.37:** Contains the License model and Json utility
- **Jackson:** JSON serialization library used by swagger-core
- **OpenAPI Specification 3.1.0:** Defines the license.identifier field

### Upstream Issues

- Should file issue in swagger-api/swagger-core repository
- Potentially related to: swagger-api/swagger-core (OpenAPI 3.1 support)

## Testing Considerations

### Unit Tests

**Test 1: Round-trip Serialization Test**
```java
@Test
public void testLicenseIdentifierRoundTrip() {
    // Parse OpenAPI 3.1 with identifier
    SwaggerParseResult result = parser.readLocation("spec-with-identifier.yaml", null, null);
    OpenAPI openAPI = result.getOpenAPI();
    
    // Verify parsed correctly
    assertNotNull(openAPI.getInfo().getLicense().getIdentifier());
    assertEquals("Apache-2.0", openAPI.getInfo().getLicense().getIdentifier());
    
    // Serialize back to JSON
    String json = Json.pretty(openAPI);
    
    // Parse again
    SwaggerParseResult result2 = parser.readContents(json, null, null);
    
    // Verify identifier preserved
    assertNotNull(result2.getOpenAPI().getInfo().getLicense().getIdentifier());
    assertEquals("Apache-2.0", result2.getOpenAPI().getInfo().getLicense().getIdentifier());
}
```

**Test 2: Identifier-Only License (No URL)**
```java
@Test
public void testLicenseIdentifierWithoutUrl() {
    String yaml = "openapi: 3.1.0\n" +
                  "info:\n" +
                  "  version: 1.0.0\n" +
                  "  title: Test\n" +
                  "  license:\n" +
                  "    name: MIT\n" +
                  "    identifier: MIT\n" +
                  "paths: {}";
    
    SwaggerParseResult result = parser.readContents(yaml, null, null);
    String json = Json.pretty(result.getOpenAPI());
    
    assertTrue(json.contains("\"identifier\""));
    assertTrue(json.contains("\"MIT\""));
}
```

**Test 3: Version-Specific Serialization**
```java
@Test
public void testIdentifierNotSerializedFor30() {
    // Ensure 3.0 specs don't get identifier serialized
    String yaml = "openapi: 3.0.0\n" +
                  "info:\n" +
                  "  version: 1.0.0\n" +
                  "  title: Test\n" +
                  "  license:\n" +
                  "    name: MIT\n" +
                  "    url: https://opensource.org/licenses/MIT\n" +
                  "paths: {}";
    
    SwaggerParseResult result = parser.readContents(yaml, null, null);
    String json = Json.pretty(result.getOpenAPI());
    
    assertFalse(json.contains("\"identifier\""));
}
```

### Integration Tests

**Integration Test 1: Real-world Petstore 3.1**
- Use official Petstore 3.1 example with identifier
- Parse → Serialize → Parse → Compare
- Verify all 3.1-specific fields preserved

**Integration Test 2: SPDX Identifier Variants**
- Test common SPDX identifiers: MIT, Apache-2.0, GPL-3.0, BSD-3-Clause
- Verify all are correctly preserved

**Integration Test 3: Mutual Exclusivity Validation**
- Verify url + identifier triggers validation error
- Test in both 3.0 and 3.1 contexts

### Edge Cases

1. **Null/Empty Identifier:**
   - `identifier: null` → Should not be serialized
   - `identifier: ""` → Based on allowEmptyStrings setting

2. **Identifier with URL:**
   - Both present → Should fail validation in 3.1
   - Error message should be clear

3. **Identifier in 3.0 Spec:**
   - Should report as unexpected field
   - Should not be included in License object

4. **Mixed Versions in Multi-file Specs:**
   - Main spec: 3.1, Referenced spec: 3.0
   - Verify correct handling of licenses in each

5. **Extension Fields with Identifier:**
   ```yaml
   license:
     name: Custom
     identifier: Custom-1.0
     x-custom-field: value
   ```
   - All fields should be preserved

## Documentation Updates

### API Documentation

1. **SwaggerParseResult Documentation:**
   - Document the `openapi31()` flag
   - Explain its impact on serialization
   - Note known limitations with Json.pretty()

2. **Migration Guide (3.0 → 3.1):**
   - Document license changes
   - Provide examples of url → identifier migration
   - Explain mutual exclusivity rules
   - Known issues and workarounds

### User Guide Updates

1. **Add Section: "Working with OpenAPI 3.1":**
   ```markdown
   ## OpenAPI 3.1 Support
   
   ### License Identifier
   
   OpenAPI 3.1 introduces the `license.identifier` field for SPDX identifiers:
   
   #### Parsing
   The parser correctly reads the identifier field:
   ```java
   SwaggerParseResult result = parser.readLocation("spec.yaml", null, null);
   String id = result.getOpenAPI().getInfo().getLicense().getIdentifier();
   ```
   
   #### Known Issue
   ⚠️ The `Json.pretty()` method may not serialize the identifier field.
   See issue #2056 for workarounds.
   ```

2. **Update Examples:**
   - Add OpenAPI 3.1 examples with identifier
   - Show proper usage patterns
   - Include troubleshooting section

### JavaDoc Updates

1. **OpenAPIDeserializer.getLicense():**
   ```java
   /**
    * Deserializes a License object from the OpenAPI specification.
    * 
    * For OpenAPI 3.1.0+, supports the 'identifier' field as an alternative
    * to 'url'. The identifier field should contain an SPDX license identifier.
    * 
    * Note: identifier and url are mutually exclusive in OpenAPI 3.1.
    * 
    * @param node The ObjectNode containing license data
    * @param location The location in the spec for error reporting
    * @param result The ParseResult for collecting validation messages
    * @return A License object, or null if node is null
    */
   public License getLicense(ObjectNode node, String location, ParseResult result)
   ```

2. **SwaggerParseResult:**
   ```java
   /**
    * Indicates whether the parsed specification is OpenAPI 3.1.x
    * 
    * This flag affects validation rules and field availability:
    * - info.summary
    * - license.identifier  
    * - jsonSchemaDialect
    * - webhooks
    * - New schema keywords (prefixItems, $id, etc.)
    * 
    * @return true if OpenAPI 3.1.x, false otherwise
    */
   public boolean isOpenapi31()
   ```

## Additional Notes

### OpenAPI 3.1 Adoption Impact

The license identifier feature is part of broader OpenAPI 3.1 adoption, which includes:
- JSON Schema 2020-12 alignment
- Improved schema composition
- Webhooks support
- Additional info fields (summary)

This issue may be symptomatic of broader serialization challenges with OpenAPI 3.1 fields. A comprehensive audit of all 3.1-specific fields should be conducted to ensure they're all properly serialized.

### SPDX License Identifiers

The identifier field uses SPDX license identifiers from https://spdx.org/licenses/. Common identifiers include:
- Apache-2.0
- MIT
- GPL-3.0-only
- BSD-3-Clause
- ISC
- MPL-2.0

The use of standardized identifiers enables:
- Automated license compliance checking
- Machine-readable license information
- Consistent license identification across projects
- Integration with dependency scanning tools

### Backward Compatibility Considerations

Any fix must maintain backward compatibility:
- OpenAPI 3.0 specs must not serialize identifier field
- Existing code relying on current behavior should not break
- Version detection must be robust
- Default behavior should be safe (prefer not serializing over serializing incorrectly)

### Performance Implications

The proposed solutions should not significantly impact:
- Parsing performance (already handled correctly)
- Serialization performance (minor overhead for version checking)
- Memory usage (identifier is a small string field)

### Security Considerations

No direct security implications, but:
- Incorrect license information could have legal implications
- License compliance tools rely on accurate data
- Silent data loss is a reliability concern

---

**Analysis Completed By:** Automated Issue Analysis  
**Date:** 2024  
**Version:** swagger-parser 2.1.20, swagger-core 2.2.37
