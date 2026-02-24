# Issue Analysis: resolveFully breaks 3.0 spec for headers via $ref in request and response

## Issue Overview
**Issue Number:** #2060  
**Title:** resolveFully breaks 3.0 spec for headers via $ref in request and response  
**Status:** Open  
**Created:** (Not specified in issue tracker)  
**Updated:** (Not specified in issue tracker)  
**URL:** https://github.com/swagger-api/swagger-parser/issues/2060

## Summary

When using `resolveFully(true)` option in OpenAPI 3.0.3 specifications, external header references ($ref) in both request parameters and response headers are incorrectly resolved to `#/components/headers/` references that contain an invalid `in` field. This creates OpenAPI documents that violate the OpenAPI 3.0 specification because header objects in the components section should not contain the `in` field, which is only valid for parameter objects.

The issue manifests when an external header reference (e.g., `$ref: 'headers.yml#/XRequestId'`) containing parameter-style properties (`in: header`, `name`, `schema`) is processed. The parser stores this object in `components/headers/` but retains the `in` field, creating an invalid OpenAPI specification.

## Problem Statement

When parsing an OpenAPI 3.0.3 specification with external header references and `resolveFully=true`:

1. External header references in operation parameters are correctly resolved inline without issues
2. External header references in response headers are replaced with internal `$ref: '#/components/headers/XRequestId'`
3. The resolved header in `components/headers/` incorrectly contains the `in: header` field from the original parameter-style definition
4. This violates the OpenAPI 3.0 specification, as response headers should not have an `in` field

**Original External Reference:**
```yaml
# headers.yml
XRequestId:
  in: header
  name: X-Request-Id
  schema:
    type: string
  required: false
```

**Invalid Result after resolveFully:**
```yaml
openapi: 3.0.3
# ...
paths:
  /users/:
    get:
      parameters:
      - name: X-Request-Id
        in: header
        required: false
        schema:
          type: string
      responses:
        "200":
          description: Ok
          headers:
            X-Request-Id:
              $ref: '#/components/headers/XRequestId'  # <- This ref points to invalid header
components:
  parameters:
    XRequestId:
      name: X-Request-Id
      in: header        # <- Valid in parameters section
      required: false
      schema:
        type: string
  headers:
    XRequestId:         # <- PROBLEM: This should not exist or should not have 'in' field
      name: X-Request-Id
      in: header        # <- INVALID: headers should not have 'in' field
      required: false
      schema:
        type: string
```

## Root Cause Analysis

The root cause lies in how the `ExternalRefProcessor` and `ResolverFully` handle header references:

### 1. **Ambiguous External Reference Structure**

The external file (`headers.yml`) contains a parameter-style structure with `in: header` field:
```yaml
XRequestId:
  in: header          # This makes it a Parameter, not a Header
  name: X-Request-Id
  schema:
    type: string
```

This structure is actually a **Parameter** object (valid for `#/components/parameters/`), not a **Header** object (valid for `#/components/headers/`). The OpenAPI specification defines:

- **Parameter Object**: Has `name`, `in`, `schema`, `required` fields
- **Header Object**: Has only `schema`, `description`, `required`, `style` (NO `in` or `name` fields)

### 2. **Processing Flow Issues**

**In ExternalRefProcessor.processRefToExternalHeader() (lines 555-615):**
```java
final Header header = cache.loadRef($ref, refFormat, Header.class);
// ...
openAPI.getComponents().addHeaders(newRef, header);
```

The processor:
1. Loads the external reference as a `Header.class` object
2. The YAML deserializer accepts the parameter-style structure and creates a Header object
3. The Header object retains the `in` and `name` fields (which should not exist for headers)
4. Stores this malformed header in `components/headers/`

**In HeaderProcessor.processHeader() (lines 40-51):**
```java
if(header.get$ref() != null){
    RefFormat refFormat = computeRefFormat(header.get$ref());
    String $ref = header.get$ref();
    if (isAnExternalRefFormat(refFormat)){
        final String newRef = externalRefProcessor.processRefToExternalHeader($ref, refFormat);
        if (newRef != null) {
            header.set$ref(newRef);  // Sets to just the name, not full path
        }
    }
}
```

Unlike `ParameterProcessor.processParameter()` which explicitly adds the prefix:
```java
newRef = "#/components/parameters/" + newRef;  // Line 55 in ParameterProcessor
```

The `HeaderProcessor` does NOT add a prefix, relying on the reference being just the name.

### 3. **Different Treatment for Parameters vs Headers**

When the same external reference is used in:

- **Request parameters**: Correctly resolved and added to `components/parameters/` with `in` field (valid)
- **Response headers**: Incorrectly creates a reference to `components/headers/` with `in` field (invalid)

The parser should either:
- Recognize that a parameter-style reference should only go to `components/parameters/`
- Strip the `in` and `name` fields when storing in `components/headers/`
- Or not create a `components/headers/` entry at all for parameter references

### 4. **OpenAPI 3.1 vs 3.0 Difference**

The issue mentions that OpenAPI 3.1.0 "fixes" this. This is likely because:
- OpenAPI 3.1 has different/relaxed validation rules
- The models may be more permissive in 3.1
- Or swagger-ui's 3.1 parser ignores the invalid `in` field in headers

## Affected Components

### Modules:
- **swagger-parser-v3**: Core parsing and resolution module

### Files:
- `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/processors/ExternalRefProcessor.java`
  - Method: `processRefToExternalHeader()` (lines 548-615)
- `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/processors/HeaderProcessor.java`
  - Method: `processHeader()` (lines 40-78)
- `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/util/ResolverFully.java`
  - Method: `resolveHeaders()` (lines 235-256)
  - Method: `resolveHeader()` (lines 258-268)

### Classes:
- `ExternalRefProcessor`: Handles external reference resolution and component storage
- `HeaderProcessor`: Processes header objects and their references
- `ResolverFully`: Fully resolves all references in the OpenAPI document
- `ResolverCache`: Caches loaded references

## Technical Details

### Current Behavior

1. **External Reference Loading**:
   - External file with parameter-style header definition is loaded
   - Deserialized as `Header.class` by Jackson/YAML parser
   - Header object incorrectly contains `in`, `name` fields

2. **Component Storage**:
   - Header is stored in `components/headers/` map with all fields intact
   - No validation or field stripping occurs
   - Both `components/parameters/` and `components/headers/` may contain the same object

3. **Reference Resolution**:
   - Response headers get `$ref: '#/components/headers/XRequestId'`
   - This reference points to an invalid header object with `in` field
   - Violates OpenAPI 3.0 specification

### Expected Behavior

According to OpenAPI 3.0 specification:

1. **Header Object Schema** (in responses):
   ```yaml
   headers:
     X-Request-Id:
       description: Request identifier
       schema:
         type: string
       # NO 'in' or 'name' fields
   ```

2. **Parameter Object Schema** (in parameters):
   ```yaml
   parameters:
     - name: X-Request-Id
       in: header
       schema:
         type: string
   ```

**Correct resolution should:**
- When resolving external references in **response headers**, the parser should:
  1. Detect that the external reference contains a parameter-style definition
  2. Either:
     - **Option A**: Fully resolve the header inline without creating a component reference
     - **Option B**: Create a cleaned header in `components/headers/` WITHOUT `in` and `name` fields
     - **Option C**: Use `components/parameters/` reference for both request and response usage

### Reproduction Steps

1. Create an OpenAPI 3.0.3 main file with external header references:
   ```yaml
   openapi: 3.0.3
   info:
     title: Sample API
     version: 1.0.0
   paths:
     /users/:
       get:
         parameters:
           - $ref: 'headers.yml#/XRequestId'
         responses:
           '200':
             description: Ok
             headers:
               X-Request-Id:
                 $ref: 'headers.yml#/XRequestId'
   ```

2. Create external header file with parameter-style definition:
   ```yaml
   # headers.yml
   XRequestId:
     in: header
     name: X-Request-Id
     schema:
       type: string
     required: false
   ```

3. Parse with resolveFully option:
   ```java
   OpenAPIV3Parser parser = new OpenAPIV3Parser();
   ParseOptions options = new ParseOptions();
   options.setResolveFully(true);
   var api = parser.read(file, null, options);
   ```

4. Observe the invalid `$ref` in output with `in` field in `components/headers/`

## Impact Assessment

**Severity:** High

**Affected Users:** 
- Developers using OpenAPI 3.0.x specifications with external header references
- Projects that use `resolveFully(true)` option for complete reference resolution
- Tools consuming the resolved OpenAPI documents (validators, code generators)
- Any workflow involving external reusable header definitions

**Workarounds:**
1. **Use OpenAPI 3.1.0**: Upgrade to OpenAPI 3.1 specification (if tooling supports it)
2. **Avoid resolveFully**: Don't use `resolveFully=true` option (keeps external refs)
3. **Inline Definitions**: Don't use external references; define headers inline
4. **Separate Definitions**: Create separate parameter and header definitions:
   ```yaml
   # parameters.yml
   XRequestIdParam:
     in: header
     name: X-Request-Id
     schema:
       type: string
   
   # headers.yml  
   XRequestIdHeader:
     schema:
       type: string
   ```
5. **Post-processing**: Manually clean the resolved document to remove invalid fields

## Proposed Solution

### Solution 1: Smart Header Resolution (Recommended)

Modify `ExternalRefProcessor.processRefToExternalHeader()` to detect and handle parameter-style definitions:

```java
public String processRefToExternalHeader(String $ref, RefFormat refFormat) {
    // ... existing code ...
    
    final Header header = cache.loadRef($ref, refFormat, Header.class);
    
    if(header == null) {
        return $ref;
    }
    
    // NEW: Check if this is actually a parameter-style definition
    if (header.getName() != null || header.getIn() != null) {
        // This is a parameter, not a header - try loading as parameter instead
        final Parameter param = cache.loadRef($ref, refFormat, Parameter.class);
        if (param != null && "header".equals(param.getIn())) {
            // Convert parameter to proper header format by stripping invalid fields
            Header cleanHeader = new Header();
            cleanHeader.setDescription(param.getDescription());
            cleanHeader.setRequired(param.getRequired());
            cleanHeader.setDeprecated(param.getDeprecated());
            cleanHeader.setSchema(param.getSchema());
            cleanHeader.setStyle(param.getStyle());
            cleanHeader.setExplode(param.getExplode());
            cleanHeader.setExample(param.getExample());
            cleanHeader.setExamples(param.getExamples());
            cleanHeader.setContent(param.getContent());
            // Use cleaned header
            header = cleanHeader;
        }
    }
    
    // ... rest of existing code ...
}
```

### Solution 2: Reference Parameters Instead

When detecting parameter-style headers in responses, reference `components/parameters/` instead:

```java
// In HeaderProcessor or ResponseProcessor
if (header.get$ref() != null) {
    // Check if referenced object is actually a parameter
    Object resolved = resolveReference(header.get$ref());
    if (resolved instanceof Parameter && "header".equals(((Parameter)resolved).getIn())) {
        // Keep reference to parameters, not headers
        // This is acceptable as swagger-ui handles it
        String paramRef = processAsParameterReference(header.get$ref());
        header.set$ref("#/components/parameters/" + paramRef);
    }
}
```

### Solution 3: Validation and Warning

Add validation to detect and warn about parameter-style headers:

```java
public String processRefToExternalHeader(String $ref, RefFormat refFormat) {
    final Header header = cache.loadRef($ref, refFormat, Header.class);
    
    // Validate header structure
    if (header.getName() != null || header.getIn() != null) {
        LOGGER.warn("Header reference '" + $ref + "' contains parameter-style fields " +
                    "('in', 'name'). These fields are not valid for header objects in " +
                    "OpenAPI 3.0 and will cause validation errors. Consider using " +
                    "separate parameter and header definitions or upgrading to OpenAPI 3.1.");
    }
    
    // ... existing code ...
}
```

### Implementation Approach

**Phase 1: Detection and Validation**
1. Add detection logic in `ExternalRefProcessor.processRefToExternalHeader()`
2. Implement field validation to identify parameter-style headers
3. Add warning/error logging for OpenAPI 3.0 specifications

**Phase 2: Automatic Cleanup**  
1. Implement field stripping logic to remove `in` and `name` fields
2. Create proper Header objects from Parameter-style definitions
3. Ensure schema and other valid fields are preserved

**Phase 3: Testing**
1. Create test cases with external parameter-style header references
2. Verify correct resolution for both request parameters and response headers
3. Test with both OpenAPI 3.0 and 3.1 specifications
4. Validate output against OpenAPI specification validators

**Phase 4: Documentation**
1. Document the behavior difference between 3.0 and 3.1
2. Add migration guide for users with existing external header definitions
3. Update API documentation for resolveFully option

### Alternatives Considered

**Alternative 1: Do Nothing**
- Rationale: Works in OpenAPI 3.1, users can upgrade
- Rejected: Many tools don't support 3.1 yet, breaks existing 3.0 specs

**Alternative 2: Always Fully Resolve Headers**
- Rationale: Avoid component references entirely for headers
- Rejected: Loses reusability benefits, increases document size

**Alternative 3: Strict Validation Only**
- Rationale: Just throw errors for invalid structures
- Rejected: Too breaking, doesn't help users migrate

**Alternative 4: Deprecate Header References**
- Rationale: Only support parameter references everywhere
- Rejected: Against OpenAPI specification design, not backward compatible

## Dependencies

### Related Issues:
- Potential related issues with parameter/header confusion in other contexts
- OpenAPI 3.1 support and migration issues
- External reference resolution issues

### External Dependencies:
- OpenAPI models library (io.swagger.core.v3.oas.models)
- Jackson YAML deserialization
- OpenAPI Specification 3.0.x and 3.1.x standards

## Testing Considerations

### Unit Tests:
1. **Test Case 1**: External header reference with parameter-style fields
   - Input: External YAML with `in`, `name` fields
   - Expected: Clean header in components or inline resolution
   
2. **Test Case 2**: External header reference with proper header structure
   - Input: External YAML without `in`, `name` fields
   - Expected: Correct resolution to `components/headers/`

3. **Test Case 3**: Same reference used in parameters and response headers
   - Input: `$ref: 'common.yml#/XRequestId'` in both contexts
   - Expected: Proper handling in both locations

4. **Test Case 4**: Nested external references in headers
   - Input: Header with external schema reference
   - Expected: All references properly resolved

### Integration Tests:
1. **Full Document Resolution**: Complete OpenAPI doc with mixed references
2. **Cross-file References**: Multiple external files with interdependencies
3. **OpenAPI 3.0 vs 3.1**: Same document parsed with different spec versions

### Edge Cases:
1. Circular header references
2. Missing external files
3. Invalid YAML structure
4. Headers with both old and new style definitions
5. Content-type based header schemas
6. Headers with examples and complex schemas

### Validation Tests:
1. Output validation against OpenAPI 3.0 JSON Schema
2. Validation against OpenAPI 3.1 JSON Schema
3. swagger-ui compatibility testing
4. openapi-generator compatibility testing

## Documentation Updates

1. **API Documentation**:
   - Document `resolveFully` option behavior with headers
   - Clarify difference between header and parameter objects
   - Add notes about OpenAPI 3.0 vs 3.1 differences

2. **Migration Guide**:
   - How to convert parameter-style headers to proper headers
   - Examples of correct external header definitions
   - Upgrading from 3.0 to 3.1 considerations

3. **Best Practices**:
   - When to use `components/parameters/` vs `components/headers/`
   - Structuring reusable header definitions
   - External reference organization patterns

4. **Changelog**:
   - Breaking changes (if validation added)
   - New warnings/errors
   - Behavior changes in resolveFully

## Additional Notes

### OpenAPI Specification Context

**OpenAPI 3.0 Specification** states:
- Parameter Objects (Section 4.7.9): MUST have `name` and `in` fields
- Header Objects (Section 4.8.14): Do NOT have `name` or `in` fields (name is the map key)
- Response Headers (Section 4.8.13.4): Use Header Objects, not Parameter Objects

**Key Difference**:
```yaml
# VALID Parameter (for operation.parameters)
parameters:
  HeaderParam:
    name: X-Request-Id     # Required
    in: header             # Required  
    schema:
      type: string

# VALID Header (for response.headers)  
headers:
  ResponseHeader:
    # name comes from map key, not a field
    # no 'in' field
    schema:
      type: string
```

### Why swagger-ui Accepts It

swagger-ui may be lenient and ignore extra fields in header objects, but:
- Other tools may fail validation
- Code generators may produce incorrect code
- It violates the OpenAPI specification
- It's not portable across tools

### Backward Compatibility

Any fix must consider:
- Existing documents that rely on current (incorrect) behavior
- Whether to introduce warnings before errors
- Migration path for affected users
- Semantic versioning implications (major vs minor release)

### Performance Considerations

- Field stripping has negligible performance impact
- Validation checks add minimal overhead
- No impact on documents without external header references
- Cache hit rates should remain unchanged

### Security Considerations

- No direct security implications
- Validates document structure, preventing malformed specs
- Could prevent injection of unexpected fields
- Maintains specification compliance for security-sensitive applications
