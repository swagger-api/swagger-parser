# Issue Analysis Template

## Issue Information
- **Issue Number**: #914
- **Title**: Recursion and resolveFully can give infinite loop while serializing
- **Reporter**: joeljons
- **Created**: 2018-11-06T15:21:24Z
- **Labels**: N/A
- **Status**: closed
- **Type**: Pull Request (Bug Report with Test Case)
- **Merged**: No (closed without merging)
- **Closed**: 2018-11-24T12:58:21Z

## Problem Statement

When using `resolveFully` option with recursive schema references in OpenAPI 3.0 specifications, the parser can enter an infinite loop during JSON serialization. The issue manifests when attempting to serialize an OpenAPI object that has been fully resolved and contains self-referential schemas. The reporter was uncertain whether the problem originated in the `resolveFully` logic or in the swagger-core serialization (`Json.mapper().writeValueAsString(openAPI)`).

## Technical Analysis

### Affected Components
- `OpenAPIV3Parser` - The main parser component that processes OpenAPI 3.0 specifications
- `ResolverFully` - The resolver that dereferences all schema references
- Schema resolution logic - Handles $ref resolution and schema composition
- Jackson serialization - JSON serialization of the fully resolved OpenAPI object
- Test file: `modules/swagger-parser-v3/src/test/java/io/swagger/v3/parser/test/OpenAPIResolverTest.java`

### Root Cause
The root cause is the lack of cycle detection when resolving recursive schema references with the `resolveFully` option enabled. When a schema contains self-references (either direct or indirect through other schemas), the resolver attempts to fully expand all references, creating an infinite object graph. This leads to:

1. **Infinite expansion**: The resolver continuously expands references without detecting that it has already visited a schema
2. **Stack overflow or infinite loop**: During serialization, Jackson attempts to serialize the circular object graph
3. **Missing visited schema tracking**: No mechanism exists to track already-resolved schemas and prevent re-processing

### Current Behavior
When parsing an OpenAPI specification with recursive schemas using `resolveFully`:
1. The parser reads the specification with `ParseOptions.setResolveFully(true)`
2. The resolver attempts to dereference all `$ref` entries
3. For recursive schemas (like `Inner` referencing itself), the resolver creates an infinite object graph
4. When `Json.mapper().writeValueAsString(openAPI)` is called, it enters an infinite loop trying to serialize the circular structure
5. The application either hangs indefinitely or throws a StackOverflowError

### Expected Behavior
The parser should:
1. Detect recursive/circular schema references during resolution
2. Maintain a map of already-resolved schemas to prevent infinite loops
3. Successfully serialize the OpenAPI object without infinite loops
4. Handle both direct self-references and indirect circular references through schema composition

## Reproduction Steps

1. Create an OpenAPI 3.0 specification with recursive schemas (as shown in the test case):
   ```yaml
   openapi: 3.0.0
   info:
     version: "0.0.2"
   paths:
     /myPath:
       get:
         responses:
           "200":
             description: Success
             content:
               application/json:
                 schema:
                   $ref: "#/components/schemas/Outer"
   components:
     schemas:
       Outer:
         allOf:
         - $ref: "#/components/schemas/Inner"
       Inner:
         properties:
           myProp:
             type: array
             items:
               $ref: "#/components/schemas/Inner"
   ```

2. Parse the specification with `resolveFully` enabled:
   ```java
   ParseOptions parseOptions = new ParseOptions();
   parseOptions.setResolveFully(true);
   OpenAPI openAPI = new OpenAPIV3Parser().read("recursive.yaml", null, parseOptions);
   ```

3. Attempt to serialize the result:
   ```java
   Json.mapper().writeValueAsString(openAPI);
   ```

4. Observe infinite loop or stack overflow

## Proposed Solution

### Approach
Implement cycle detection in the schema resolution process by maintaining a map of schemas that are currently being resolved or have already been resolved. This prevents the resolver from processing the same schema multiple times.

### Implementation Details
The solution was implemented in PR #943 by gracekarina:
- Added a "Property Resolved Map" to track schemas during resolution
- Modified the resolver to check if a schema has already been processed before attempting to resolve it
- This prevents infinite loops during both the resolution phase and serialization phase

Key changes in PR #943:
- 4 files changed
- 188 additions, 38 deletions
- 3 commits

### Code Locations
Files that needed modification (based on PR #943):
- Schema resolution logic (likely in resolver classes)
- Property resolution tracking mechanism
- Test cases to validate the fix

The fix addressed both:
- #914: Infinite loop during serialization
- #929: Infinite loop during ResolveFully itself

### Testing Strategy
The PR #914 included a failing test case that demonstrates the issue:
```java
@Test
public void recursiveResolving() throws JsonProcessingException {
    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setResolveFully(true);
    OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/recursive.yaml", null, parseOptions);
    Json.mapper().writeValueAsString(openAPI);
}
```

This test should:
1. Successfully parse the recursive specification
2. Complete serialization without hanging or throwing exceptions
3. Validate that recursive references are handled correctly

## Potential Risks & Considerations

1. **Performance Impact**: Adding cycle detection tracking may have minor performance overhead due to map lookups
2. **Memory Usage**: Maintaining a resolved schema map increases memory consumption during parsing
3. **Backward Compatibility**: The fix should not change the behavior for non-recursive schemas
4. **Complex Recursion Patterns**: Must handle various recursion patterns:
   - Direct self-reference (schema A → schema A)
   - Indirect circular reference (schema A → schema B → schema A)
   - Multi-level circular references through allOf, anyOf, oneOf
5. **Schema Identity**: Must correctly identify when two schema references point to the same schema

## Related Issues

- **PR #943**: "Recursion issue" - The fix that resolved this problem (merged on 2018-11-24)
- **Issue #929**: "Recursion that can lead to an infinite loop in ResolverFully" - Related issue also fixed by PR #943
  - Similar issue but failed during ResolveFully phase rather than serialization
  - Both issues reported by joeljons
  - Both addressed in the same fix

## Additional Context

- The PR #914 was submitted as a failing test case only, demonstrating the bug
- The reporter (joeljons) identified the problem and provided a minimal reproducible test case
- The fix was implemented by gracekarina in PR #943
- This is a critical bug as it causes the parser to hang or crash with recursive schemas
- Recursive schemas are valid in OpenAPI/JSON Schema and should be supported
- The issue affects OpenAPI 3.0 parser specifically

### PR Details
- **Additions**: 32 lines (test case + test resource)
- **Changed Files**: 2
  - Test file: OpenAPIResolverTest.java
  - Test resource: recursive.yaml
- **Commits**: 1
- **Comments**: 1 (gracekarina noting it was fixed by #943)

## Complexity Estimate

- **Effort**: Medium
  - Required understanding of schema resolution logic
  - Needed to implement cycle detection mechanism
  - Required comprehensive testing for various recursion patterns

- **Impact**: High
  - Critical bug causing infinite loops/hangs
  - Affects any specification with recursive schemas
  - Could cause production systems to freeze

- **Priority**: High
  - Causes complete failure for recursive schemas
  - No workaround available (can't use resolveFully with recursive schemas)
  - Common use case in real-world APIs (e.g., tree structures, linked lists)

## References

- PR #914: https://github.com/swagger-api/swagger-parser/pull/914
- PR #943: https://github.com/swagger-api/swagger-parser/pull/943 (Fix)
- Issue #929: https://github.com/swagger-api/swagger-parser/pull/929
- OpenAPI Specification: https://spec.openapis.org/oas/v3.0.0
- JSON Schema Recursion: https://json-schema.org/understanding-json-schema/structuring.html#recursion
