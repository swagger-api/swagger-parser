# Issue Analysis Template

## Issue Information
- **Issue Number**: #429
- **Title**: Parser fails when no default value defined
- **Reporter**: dcalap
- **Created**: 2017-03-28T08:24:22Z
- **Labels**: None
- **Status**: Open

## Problem Statement

The Swagger Parser fails when parsing specifications that do not define default values for parameters. The issue manifests as a NullPointerException or error when the parser attempts to set a null default value on parameters. This regression was introduced when upgrading from version 1.0.10 to 1.0.28, specifically affecting users who require vendor extension parsing but do not define default values in their specifications.

The reporter indicates that their JSON specification worked correctly with version 1.0.10, but after upgrading to 1.0.28 (to access vendor extension parsing features), the parser fails with an error related to setting default values to null at `io.swagger.parser.util.SwaggerDeserializer.parameter(SwaggerDeserializer.java:481)` with the line `sp.setDefault(defaultValue)`.

## Technical Analysis

### Affected Components
- **OpenAPIDeserializer.java** (modern equivalent) - Schema deserialization logic
- **SwaggerDeserializer.java** (legacy, version 1.x) - Parameter deserialization in older parser versions
- **Schema class** - Default value handling
- **Parameter classes** - Default value assignment for API parameters

### Root Cause

Based on code analysis and related issues, the root cause is in the schema/parameter deserialization logic that unconditionally calls `setDefault()` even when no default value is present in the specification. The problematic pattern occurs in multiple scenarios:

1. **Explicit null assignment**: The code at line 2998 in OpenAPIDeserializer shows `schema.setDefault(null)` in the else block when no default value is found in the JSON/YAML node
2. **Missing null checks**: The deserialization logic may not properly check whether a default value exists before attempting to set it
3. **Type-specific handling**: When inferring schema types, the code handles various types (array, string, boolean, object, integer, number) but defaults to setting null when no value is present

The issue is particularly problematic because:
- The OpenAPI/Swagger specification does not require default values
- Setting an explicit null may cause downstream issues in code generators or validators
- Different versions handle this inconsistently

### Current Behavior

In the current codebase (OpenAPIDeserializer.java, lines 2938-2999):

```java
if (node.get("default") != null && result.isInferSchemaType()) {
    // ... type-specific default handling ...
} else if (node.get("default") != null) {
    Object defaultObject = getAnyType("default", node, location, result);
    if (defaultObject != null) {
        schema.setDefault(defaultObject);
    }
} else {
    schema.setDefault(null);  // Line 2998 - PROBLEMATIC
}
```

When no default value is defined in the specification:
1. Both conditional branches (`if` and `else if`) are skipped
2. The final `else` block executes and sets the default to `null` explicitly
3. This can cause issues if the Schema class or downstream consumers don't handle explicit null defaults properly

### Expected Behavior

When no default value is defined in the specification, the parser should:
1. **NOT** call `setDefault(null)` explicitly
2. Leave the default value unset (allowing it to remain as the default object state)
3. Only call `setDefault()` when an actual default value exists in the specification
4. Properly differentiate between "no default specified" and "default is null" (which is valid for nullable types)

## Reproduction Steps

Based on the issue description and analysis:

1. Create a Swagger/OpenAPI specification with parameters that do NOT include default values
2. Ensure the specification includes vendor extensions (since the reporter upgraded to get vendor extension support)
3. Parse the specification using swagger-parser version 1.0.28 or later
4. Observe the error/exception when the parser attempts to set null as the default value

Example specification snippet that would trigger the issue:
```yaml
parameters:
  - name: myParam
    in: query
    type: string
    required: false
    # No default value specified
```

## Proposed Solution

### Approach

Remove the unconditional `schema.setDefault(null)` call when no default value is present in the specification. The Schema object should only have its default value set when an explicit default is provided in the specification.

### Implementation Details

**Option 1: Remove the explicit null assignment (Recommended)**

Modify the OpenAPIDeserializer.java logic to simply not set a default when none is present:

```java
if (node.get("default") != null && result.isInferSchemaType()) {
    // ... existing type-specific default handling ...
} else if (node.get("default") != null) {
    Object defaultObject = getAnyType("default", node, location, result);
    if (defaultObject != null) {
        schema.setDefault(defaultObject);
    }
}
// Remove the else block that sets null
```

**Option 2: Only set null for explicitly nullable schemas with null defaults**

Keep the null assignment but only for cases where the specification explicitly sets `default: null`:

```java
if (node.get("default") != null && result.isInferSchemaType()) {
    // ... existing type-specific default handling ...
} else if (node.get("default") != null) {
    Object defaultObject = getAnyType("default", node, location, result);
    // Set the default whether it's null or not, since it's explicitly in the spec
    schema.setDefault(defaultObject);
}
// No else block - don't set anything if no default is in the spec
```

**Option 3: Add a flag to distinguish "not set" from "set to null"**

Use a wrapper or Optional type to differentiate between:
- Default value not specified in the spec
- Default value explicitly set to null in the spec

This would be a more invasive change but provides the most accurate representation.

### Code Locations

Files to modify:
1. **modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/util/OpenAPIDeserializer.java**
   - Lines 2997-2999: Remove or modify the else block that sets null
   - Verify similar patterns in other methods (e.g., parameter deserialization, header deserialization)

2. **Legacy code** (if still maintained):
   - The original SwaggerDeserializer.java file mentioned in the issue (line 481)

### Testing Strategy

1. **Unit Tests**: Create test cases for schemas/parameters without default values
   ```java
   @Test
   public void testSchemaWithoutDefaultValue() {
       String yaml = "type: string\ndescription: A string without default";
       Schema schema = parseSchema(yaml);
       assertNull(schema.getDefault()); // Or assertThat default is not set
   }
   ```

2. **Unit Tests**: Verify schemas with explicit null defaults still work (for nullable types)
   ```java
   @Test
   public void testNullableSchemaWithNullDefault() {
       String yaml = "type: string\nnullable: true\ndefault: null";
       Schema schema = parseSchema(yaml);
       assertNull(schema.getDefault());
   }
   ```

3. **Integration Tests**: Parse complete specifications without default values
   - Test with YAML files that have parameters without defaults
   - Test with JSON files that have schemas without defaults
   - Verify vendor extensions are properly parsed

4. **Regression Tests**: Ensure existing functionality isn't broken
   - Schemas with default values should continue to work
   - All types (string, integer, boolean, array, object, number) should handle defaults correctly
   - Type inference mode should still work properly

5. **Backward Compatibility**: Verify that code consuming the parser handles the change
   - Code generators should handle schemas without defaults
   - Validators should not fail on missing defaults

## Potential Risks & Considerations

### Breaking Changes
- **API Change**: If downstream code expects `getDefault()` to return null for schemas without defaults, removing the explicit null assignment could break that assumption
  - **Mitigation**: Document the change clearly; most well-written code should handle this gracefully

- **Serialization**: If schemas are serialized back to JSON/YAML, ensure that absent defaults don't get serialized as `"default": null`
  - **Mitigation**: Verify Jackson serialization settings exclude null values or use `@JsonInclude(JsonInclude.Include.NON_NULL)`

### Backward Compatibility
- The change aligns better with the OpenAPI specification, which doesn't require defaults
- Existing specifications that don't define defaults will parse successfully
- Specifications with explicit defaults (including `default: null` for nullable fields) should continue to work

### Edge Cases
1. **Nullable types with `default: null`**: Must be preserved as a valid scenario
2. **Type inference mode**: Ensure the logic for `isInferSchemaType()` still handles defaults correctly
3. **All schema types**: Verify the change works for all types (string, number, integer, boolean, array, object)
4. **Referenced schemas**: Check if $ref resolution properly handles defaults
5. **Composed schemas** (allOf, oneOf, anyOf): Verify default handling in composite scenarios

## Related Issues

- **#1454**: "OpenAPIV3Parser setting null instead of parsing default enum values" - Similar issue with enum default values being set to null instead of being parsed correctly
- **#1015**: Closed issue potentially related to default value handling  
- **#973**: "Unable to determine correct format of IntegerSchema" - Related to default format values being set when not specified
- **#733**: NullPointerException in SwaggerConverter - Shows null handling issues in parameter conversion
- **#146**: YAML anchors causing conversion exceptions - Related to how defaults are handled in arrays

## Additional Context

### Historical Context
- Issue was reported in 2017 during transition from 1.0.10 to 1.0.28
- The upgrade was motivated by the need for vendor extension parsing
- The SwaggerDeserializer class mentioned in the original issue has been replaced by OpenAPIDeserializer in the current codebase
- The architecture has evolved from Swagger 2.0 (v1.x parser) to OpenAPI 3.0 (v2.x+ parser)

### OpenAPI Specification Guidance
According to the OpenAPI Specification:
- Default values are OPTIONAL for schema objects
- When present, the default value should be consistent with the schema type
- `default: null` is valid only for nullable schemas

### User Impact
- Users who don't specify default values in their specifications encounter parsing errors
- This forces users to add unnecessary default values or stay on older parser versions
- The issue is particularly problematic for auto-generated specifications or large APIs with many parameters

## Complexity Estimate

- **Effort**: Low to Medium
  - Code change is straightforward (removing 1-2 lines)
  - Testing requires comprehensive coverage across schema types
  - Need to verify behavior in both OpenAPI 3.0 and legacy Swagger 2.0 parsing paths

- **Impact**: Medium
  - Affects core parsing logic used by all parser consumers
  - May require documentation updates
  - Could affect downstream code generators and validators
  - Benefits all users parsing specifications without defaults

- **Priority**: Medium to High
  - Open since 2017, indicating it may not be critical but affects real users
  - Related to specification compliance (parsers should handle optional fields properly)
  - Multiple related issues suggest this is a pattern of problems with null/default handling
  - Relatively low risk if implemented with proper testing

## References

- [OpenAPI 3.0 Specification - Schema Object](https://swagger.io/specification/#schema-object)
- [OpenAPI 3.1 Specification - Schema Object](https://spec.openapis.org/oas/v3.1.0#schema-object)
- [JSON Schema - Default Keyword](https://json-schema.org/understanding-json-schema/reference/generic.html#annotations)
- [Swagger 2.0 Specification](https://swagger.io/specification/v2/)
- Current codebase: `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/util/OpenAPIDeserializer.java`
- Related discussion: Issue #1454 with detailed test cases for enum defaults
