# Issue Analysis Template

## Issue Information
- **Issue Number**: #377
- **Title**: Providing line and column numbers in parsed Swagger object
- **Reporter**: fmueller
- **Created**: 2017-01-06T10:24:35Z
- **Labels**: None
- **Status**: open

## Problem Statement

The issue requests the ability to provide line and column number information in parsed Swagger/OpenAPI objects, particularly for YAML files. This feature is essential for API linters and validation tools that need to report precise locations of violations or errors in Swagger specification files. The reporter is building an API linter (Zally - https://github.com/zalando-incubator/zally) and needs to provide users with exact line and column numbers when reporting violations in their Swagger files.

## Technical Analysis

### Affected Components
- **OpenAPIDeserializer** - Main deserialization logic for OpenAPI specifications
- **DeserializationUtils** - Utility classes for deserialization operations
- **OpenAPIParser** - Entry point for parsing OpenAPI/Swagger files
- **SwaggerParseResult** - Result object that contains parsed OpenAPI specification
- **SnakeYAML integration** - YAML parsing library (version 2.4 as per current dependencies)

### Root Cause

The current parser implementation transforms YAML/JSON input into Java objects without preserving source location metadata (line and column numbers). The transformation process discards positional information during the deserialization phase. When SnakeYAML parses the YAML file, it creates intermediate nodes that contain Mark objects with line/column information, but this metadata is lost when converting to the final OpenAPI model objects.

### Current Behavior

1. YAML/JSON files are parsed using SnakeYAML or Jackson
2. The content is deserialized into OpenAPI model objects (Swagger, Path, Operation, etc.)
3. No positional metadata (line/column numbers) is preserved in the resulting object model
4. Validation errors and linting violations can only reference the object structure, not the source file location
5. Users cannot easily locate the exact position of issues in their specification files

### Expected Behavior

1. Each parsed OpenAPI model object should optionally contain source location metadata
2. Line and column numbers should be available for all major elements (paths, operations, parameters, schemas, etc.)
3. Validation errors and linting tools should be able to report exact file positions
4. The feature should work primarily for YAML files (as mentioned in the issue) but ideally for JSON as well
5. The location metadata should not significantly impact parsing performance or memory usage

## Reproduction Steps

Not applicable - this is a feature request, not a bug report.

## Proposed Solution

### Approach

Implement a source location tracking system that preserves line and column information during the YAML/JSON parsing process. This can be achieved through:

1. **Custom SnakeYAML Constructor**: Create a custom YAML constructor that preserves Mark objects containing line/column information
2. **Location Metadata Model**: Add optional location metadata to OpenAPI model objects (non-intrusive approach using extensions or separate tracking)
3. **Parser Option**: Make location tracking optional via ParseOptions to avoid performance overhead when not needed

### Implementation Details

**Option 1: Extension Properties (Recommended)**
- Add vendor extensions (e.g., `x-source-location`) to model objects during parsing
- Store line/column information as extension properties
- Minimal changes to existing model classes
- Backward compatible

**Option 2: Location Tracking Map**
- Maintain a separate map that correlates object instances to source locations
- Use WeakHashMap to avoid memory leaks
- More complex but keeps model objects clean

**Option 3: Wrapper Objects**
- Create wrapper classes that contain both the model object and location metadata
- Changes the API surface more significantly
- Most intrusive but most type-safe approach

**Recommended Implementation Steps:**

1. Extend ParseOptions to include `preserveSourceLocation` flag
2. Create a `SourceLocation` class to hold line, column, and optional file path
3. Modify OpenAPIDeserializer to preserve Mark information from SnakeYAML
4. For each model object created during deserialization, attach location metadata as vendor extension (x-source-line, x-source-column)
5. Provide utility methods to retrieve location information from parsed objects
6. Add comprehensive tests with sample YAML files

### Code Locations

Files that need modification:

1. **modules/swagger-parser-core/src/main/java/io/swagger/v3/parser/core/models/ParseOptions.java**
   - Add `preserveSourceLocation` boolean field
   - Add getter/setter methods

2. **modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/util/OpenAPIDeserializer.java**
   - Modify deserialization methods to preserve location metadata
   - Extract line/column information from YAML nodes
   - Attach location data to model objects

3. **modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/util/DeserializationUtils.java**
   - Add helper methods for location extraction and attachment

4. **New class: modules/swagger-parser-core/src/main/java/io/swagger/v3/parser/core/models/SourceLocation.java**
   - Simple POJO to hold line, column, and source file information

5. **modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/ObjectMapperFactory.java** (if exists)
   - Configure custom deserializers if needed

### Testing Strategy

1. **Unit Tests**
   - Test location extraction from YAML Mark objects
   - Test location attachment to various model objects
   - Test with and without preserveSourceLocation flag

2. **Integration Tests**
   - Parse complete YAML specifications and verify location information
   - Test with nested structures (paths, operations, schemas, parameters)
   - Test with $ref references (both local and external)
   - Test with multi-file specifications

3. **Edge Cases**
   - Empty files
   - Files with only whitespace
   - Inline vs referenced schemas
   - YAML anchors and aliases
   - Multi-line strings
   - Comments in YAML (should not affect line numbers)

4. **Performance Tests**
   - Compare parsing time with and without location tracking
   - Measure memory overhead

## Potential Risks & Considerations

1. **Security Concerns**
   - SnakeYAML has had security vulnerabilities (as mentioned by fehguy in comments)
   - Current version is 2.4 - ensure it's up to date and monitor for CVEs
   - Custom constructor might introduce new attack vectors if not carefully implemented

2. **Performance Impact**
   - Additional memory overhead for storing location metadata
   - Slight parsing performance degradation
   - Mitigation: Make it opt-in via ParseOptions

3. **Backward Compatibility**
   - Adding vendor extensions shouldn't break existing consumers
   - Ensure default behavior (without flag) remains unchanged
   - Document the feature clearly

4. **JSON Support**
   - Jackson doesn't provide the same location tracking capabilities as SnakeYAML
   - May need JsonParser.getCurrentLocation() for JSON files
   - YAML should be prioritized as per issue request

5. **Model Object Pollution**
   - Adding x-source-* extensions might interfere with users who serialize objects back
   - Consider cleanup utilities or transient storage

6. **Referenced Content**
   - External $ref references span multiple files
   - Need clear semantics for location reporting across files
   - May need file path in addition to line/column

## Related Issues

Based on the comments and context:
- This issue is related to API linting and validation tools
- Zally project (https://github.com/zalando-incubator/zally) has this use case
- mricken mentioned their team uses a "cumbersome system" of parsing YAML separately with SnakeYAML to get line numbers
- Issue has 3 👍 reactions indicating community interest

## Additional Context

**From Comments:**

1. **fehguy (Contributor, 2017-01-06):**
   - Acknowledged the value of the feature
   - Mentioned difficulty of getting line numbers with YAML parsing in Java
   - Welcomed PR contributions
   - Warned about SnakeYAML security issues

2. **nikhilunni (2019-11-27):**
   - Asked if feature is still of interest
   - Team was looking into building this feature

3. **mricken (2019-11-27):**
   - Confirmed continued interest
   - Currently using workaround: parsing YAML separately with SnakeYAML to get line numbers
   - Describes current approach as "very cumbersome"

**Community Interest:**
- Issue has been open since 2017 (7+ years)
- Still receiving comments as recently as 2019
- 3 positive reactions on the original issue
- Multiple teams/organizations interested (Zalando, mricken's team, nikhilunni's team)

## Complexity Estimate

- **Effort**: Medium to High
  - Requires understanding of SnakeYAML internals
  - Need to modify core deserialization logic
  - Comprehensive testing across many scenarios
  - Security considerations add complexity
  - JSON support (if included) adds additional work

- **Impact**: High
  - Highly valuable for API linting and validation tools
  - Benefits entire ecosystem of tools built on swagger-parser
  - Improves developer experience significantly
  - Enables better error reporting across all consumers

- **Priority**: Medium to High
  - Long-standing request with clear use cases
  - Multiple organizations interested
  - Not critical for basic parsing functionality
  - High value for quality tooling ecosystem

## References

1. **SnakeYAML Documentation**: https://bitbucket.org/snakeyaml/snakeyaml/wiki/Home
2. **SnakeYAML Mark Class**: Used for tracking position in source
3. **Jackson Location Tracking**: JsonParser.getCurrentLocation() for JSON support
4. **Zally Project**: https://github.com/zalando-incubator/zally - API linter use case
5. **OpenAPI Specification Extensions**: https://spec.openapis.org/oas/v3.0.0#specification-extensions
6. **Related Pattern**: Similar to how compilers/parsers track AST node locations for error reporting
7. **SnakeYAML Security**: CVE database for known vulnerabilities (https://www.cvedetails.com/vulnerability-list/vendor_id-19929/Snakeyaml-Project.html)
