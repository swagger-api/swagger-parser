# Issue Analysis Template

## Issue Overview
**Issue Number:** #2057  
**Title:** Schema's loaded from other than requested file don't store location  
**Status:** Open  
**Created:** Not specified in available data  
**Updated:** Not specified in available data  
**URL:** https://github.com/swagger-api/swagger-parser/issues/2057

## Summary
When using `setResolve(true)` in `ParseOptions`, the OpenAPI parser loads schemas from external files and consolidates them into the OpenAPI model's `#/components/schemas` section. However, once resolved, the original external file reference is lost, making it impossible to distinguish between schemas that were originally defined in external shared files versus schemas that should be treated as local to a specific API specification. This limitation prevents code generators from implementing logic to avoid duplicating generated code for shared schema files.

## Problem Statement
The current implementation of external reference resolution in `SchemaProcessor` replaces external `$ref` values (e.g., `errors.yaml#/components/schemas/ErrorResponse`) with internal references (e.g., `#/components/schemas/ErrorResponse`). While this successfully consolidates all schemas into a single model, it erases the provenance information about where each schema originated from.

This creates a problem for code generation workflows where:
1. **Shared schemas** (e.g., `errors.yaml`) should only have code generated once and be shared across multiple API specifications
2. **API-specific schemas** from external files should be included in the generated code for that specific API

Without the ability to distinguish between these two cases, code generators must either:
- Generate code for all schemas redundantly (leading to duplication)
- Implement complex heuristics outside the parser to track schema origins
- Manually maintain metadata about which schemas are shared

## Root Cause Analysis

### Current Implementation Flow
1. **SchemaProcessor.processReferenceSchema()** (line 219-235):
   - Detects external references using `computeRefFormat()` and `isAnExternalRefFormat()`
   - Delegates to `ExternalRefProcessor.processRefToExternalSchema()` for resolution
   - Replaces the external `$ref` with an internal reference using `RefType.SCHEMAS.getInternalPrefix()`

2. **ExternalRefProcessor.processRefToExternalSchema()** (line 89-149):
   - Loads the external schema using `cache.loadRef()`
   - Computes a unique name for the schema in the components section
   - Stores the schema in `openAPI.getComponents().addSchemas()`
   - Updates the `$ref` to point to the internal location
   - Maintains a rename cache mapping original refs to new names

3. **ResolverCache.renameCache** (line 81, 404-410):
   - Stores mappings from original external references to renamed internal references
   - Used to avoid reprocessing the same external reference multiple times
   - **Not exposed** in the final Schema objects themselves

### Root Cause
The fundamental issue is that the original external reference information is only stored in the `ResolverCache.renameCache`, which is:
- Internal to the parsing process
- Not persisted in the final `Schema` objects
- Not accessible to downstream consumers like code generators

Once the parsing is complete and the `OpenAPI` model is handed to a code generator, all schemas appear to be internal components, with no way to determine their original source.

## Affected Components

### Module
- `swagger-parser-v3`

### Files
- `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/processors/SchemaProcessor.java`
  - Primary location where `$ref` transformation occurs
  - Lines 219-235: `processReferenceSchema()` method

- `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/processors/ExternalRefProcessor.java`
  - Lines 89-149: `processRefToExternalSchema()` method
  - Handles loading and renaming of external schemas

- `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/ResolverCache.java`
  - Lines 404-410: `getRenamedRef()` and `putRenamedRef()` methods
  - Lines 81, 420-422: `renameCache` storage and accessor

### Classes
- `SchemaProcessor`: Processes schema objects and their references
- `ExternalRefProcessor`: Handles loading and processing of external references
- `ResolverCache`: Caches loaded external content and maintains rename mappings
- `Schema` (from swagger-core): The model object that would need to store `$originalRef`

## Technical Details

### Current Behavior
When an OpenAPI specification contains an external reference:
```yaml
# api1.yaml
components:
  schemas:
    MyResponse:
      $ref: './errors.yaml#/components/schemas/ErrorResponse'
```

After parsing with `setResolve(true)`:
```java
ParseOptions parseOptions = new ParseOptions();
parseOptions.setResolve(true);
OpenAPI openApiModel = new OpenAPIV3Parser().read("api1.yaml", null, parseOptions);
Schema schema = openApiModel.getComponents().getSchemas().get("MyResponse");
// schema.get$ref() returns "#/components/schemas/ErrorResponse"
// NO INFORMATION about the original reference to errors.yaml
```

The `ErrorResponse` schema is now in the components section, but there's no way to know it came from `errors.yaml`.

### Expected Behavior
After implementing the `$originalRef` proposal:
```java
ParseOptions parseOptions = new ParseOptions();
parseOptions.setResolve(true);
OpenAPI openApiModel = new OpenAPIV3Parser().read("api1.yaml", null, parseOptions);
Schema schema = openApiModel.getComponents().getSchemas().get("ErrorResponse");
// schema.get$ref() returns "#/components/schemas/ErrorResponse"
// schema.get("$originalRef") returns "./errors.yaml#/components/schemas/ErrorResponse"
```

Code generators could then check for `$originalRef` to determine:
- If present: schema was loaded from an external file (file path indicates if it's shared)
- If absent: schema was defined locally or is an internal reference

### Reproduction Steps
1. Create `errors.yaml` with a schema definition:
```yaml
components:
  schemas:
    ErrorResponse:
      type: object
      properties:
        message:
          type: string
```

2. Create `api1.yaml` that references `errors.yaml`:
```yaml
components:
  schemas:
    MyResponse:
      $ref: './errors.yaml#/components/schemas/ErrorResponse'
```

3. Parse with resolution enabled:
```java
ParseOptions parseOptions = new ParseOptions();
parseOptions.setResolve(true);
OpenAPI openApiModel = new OpenAPIV3Parser().read("api1.yaml", null, parseOptions);
Schema errorSchema = openApiModel.getComponents().getSchemas().get("ErrorResponse");
```

4. Observe that `errorSchema.get$ref()` is null or points to an internal reference
5. Observe that there is no way to determine that this schema originated from `errors.yaml`

## Impact Assessment

**Severity:** Medium

**Affected Users:** 
- **Code Generator Developers**: Primary impact on developers building code generators (e.g., Swagger Codegen, OpenAPI Generator) who need to avoid generating duplicate code for shared schemas
- **API Platform Teams**: Teams managing multiple API specifications that share common schema files
- **Enterprise Users**: Organizations with large-scale OpenAPI specifications using shared component libraries

**Workarounds:**
1. **Pre-processing approach**: Parse the OpenAPI file before resolution to build a custom mapping of external references, then correlate after resolution
2. **Naming conventions**: Use specific naming patterns in schema names to indicate shared vs. local (brittle and error-prone)
3. **Separate resolution**: Don't use `setResolve(true)`, instead manually resolve references and maintain metadata
4. **File structure analysis**: Parse YAML/JSON files directly to detect `$ref` patterns pointing to specific shared files
5. **Custom parser fork**: Maintain a modified version of swagger-parser with this feature

All workarounds add significant complexity and are fragile to changes in file structure or naming.

## Proposed Solution

### Primary Proposal: Store $originalRef Extension
Add an extension property `x-original-ref` or use the `$originalRef` field to preserve the original external reference information in the resolved Schema object.

**Implementation Approach:**

1. **Modify SchemaProcessor.processReferenceSchema()** (line 219-235):
```java
private void processReferenceSchema(Schema schema) {
    RefFormat refFormat = computeRefFormat(schema.get$ref());
    String $ref = schema.get$ref();

    if (isAnExternalRefFormat(refFormat)){
        final String newRef = externalRefProcessor.processRefToExternalSchema($ref, refFormat);

        if (newRef != null) {
            // NEW: Store original external reference before replacing
            schema.addExtension("x-original-ref", $ref);
            schema.set$ref(RefType.SCHEMAS.getInternalPrefix() + newRef);
        }
    }
}
```

2. **Alternative: Use $originalRef property**:
If the OpenAPI model supports it, use `$originalRef` as a standard property instead of an extension:
```java
if (newRef != null) {
    // Check if Schema model supports $originalRef
    schema.set$originalRef($ref);  // Requires adding this property to Schema class
    schema.set$ref(RefType.SCHEMAS.getInternalPrefix() + newRef);
}
```

3. **Propagate to nested schemas**:
Ensure that when schemas are recursively processed in `ExternalRefProcessor`, the `x-original-ref` is preserved through the entire resolution chain.

**Benefits:**
- Minimal change to existing codebase
- Backward compatible (doesn't break existing consumers)
- Extension properties are part of OpenAPI 3.x specification
- Code generators can easily check for presence of `x-original-ref`
- Provides full path information for sophisticated filtering

**Usage Example:**
```java
// In a code generator
Schema schema = openAPI.getComponents().getSchemas().get("ErrorResponse");
Object originalRef = schema.getExtensions() != null ? 
    schema.getExtensions().get("x-original-ref") : null;

if (originalRef != null && originalRef.toString().contains("errors.yaml")) {
    // This is a shared schema, generate only once
    if (!alreadyGenerated.contains("errors.yaml")) {
        generateSharedCode(schema);
        alreadyGenerated.add("errors.yaml");
    }
} else {
    // Local or API-specific schema, always generate
    generateCode(schema);
}
```

### Implementation Approach

#### Phase 1: Core Implementation
1. Modify `SchemaProcessor.processReferenceSchema()` to store original ref as extension
2. Add unit tests to verify `x-original-ref` is set correctly
3. Test with various ref formats (relative, URL, internal)

#### Phase 2: Comprehensive Coverage
1. Audit other processors (`ExampleProcessor`, `ResponseProcessor`, etc.) for similar patterns
2. Apply same `x-original-ref` pattern to other component types (responses, parameters, etc.)
3. Ensure nested references maintain original ref information

#### Phase 3: Documentation
1. Update parser documentation to describe `x-original-ref` behavior
2. Add examples showing how code generators can use this feature
3. Document in changelog as a new feature

### Alternatives Considered

#### Alternative 1: Add originalRef to Schema Model Class
**Approach:** Modify the swagger-core `Schema` class to include a dedicated `originalRef` property.

**Pros:**
- Type-safe access
- More discoverable than extensions
- Could be part of OpenAPI specification in the future

**Cons:**
- Requires changes to swagger-core dependency
- More invasive change
- Breaks model purity (mixing parser metadata with spec model)
- Harder to maintain backward compatibility

**Verdict:** Not recommended as initial approach, but could be considered if there's broader community support for adding this to the OpenAPI specification itself.

#### Alternative 2: Expose ResolverCache to Consumers
**Approach:** Make `ResolverCache` and its `renameCache` accessible after parsing.

**Pros:**
- No model changes required
- Already contains the needed information

**Cons:**
- Cache is internal implementation detail
- Awkward API for consumers (requires looking up in separate structure)
- Cache may not be retained after parsing completes
- Doesn't integrate with standard OpenAPI tooling

**Verdict:** Not recommended. Too tightly coupled to parser internals.

#### Alternative 3: Generate Metadata Sidecar
**Approach:** Parser could optionally generate a separate JSON/YAML file containing mapping of schema names to original references.

**Pros:**
- No model changes
- Clean separation of concerns

**Cons:**
- Requires managing additional file
- Complex to maintain synchronization
- Not integrated with existing tooling
- Additional I/O overhead

**Verdict:** Not recommended. Adds operational complexity.

#### Alternative 4: Custom ParseOptions Flag
**Approach:** Add a new `ParseOptions` flag like `preserveOriginalRefs` to control behavior.

**Pros:**
- Opt-in behavior
- Maintains backward compatibility by default

**Cons:**
- Still need to decide where to store the information
- Adds API complexity
- Extension approach already provides this flexibility

**Verdict:** Could be combined with Primary Proposal if needed, but extension approach is already non-breaking.

## Dependencies

### Related Issues
- Issue #1658: Related to external reference resolution (found test case in codebase)
- Potentially related to any issues involving external reference resolution or schema consolidation

### External Dependencies
- **swagger-core**: The `Schema` class that would store the extension
- **OpenAPI Specification**: Extensions are part of the spec (properties starting with `x-`)
- **Code Generators**: Would benefit from this feature (Swagger Codegen, OpenAPI Generator)

### Backward Compatibility
- Adding an extension property is **fully backward compatible**
- Existing consumers that don't check for `x-original-ref` will continue to work unchanged
- New consumers can opt-in to using this metadata

## Testing Considerations

### Unit Tests
1. **Basic external reference resolution**:
   - Verify `x-original-ref` is set when resolving an external file reference
   - Verify `x-original-ref` contains the original ref string
   - Verify `$ref` is updated to internal reference

2. **Multiple external files**:
   - Test with multiple files (errors.yaml, common.yaml)
   - Verify each schema has correct `x-original-ref`
   - Verify no cross-contamination of original refs

3. **Nested references**:
   - External file containing schema with another external ref
   - Verify all levels preserve original refs correctly

4. **Internal references**:
   - Verify `x-original-ref` is NOT set for internal refs
   - Verify behavior unchanged for `#/components/schemas/Foo`

5. **Relative vs absolute paths**:
   - Test with `./errors.yaml`, `../common/errors.yaml`
   - Test with URL references (`http://example.com/schemas.yaml`)
   - Verify paths are preserved as-is

### Integration Tests
1. **Code generation workflow**:
   - Create multi-file OpenAPI spec with shared schemas
   - Parse with `setResolve(true)`
   - Verify code generator can identify shared vs local schemas
   - Verify code is generated only once for shared files

2. **Real-world spec patterns**:
   - Test with common patterns (shared errors, common types, base objects)
   - Verify with complex inheritance and composition scenarios
   - Test with circular references

3. **Backward compatibility**:
   - Parse existing specs and verify behavior unchanged
   - Verify specs without external refs work as before
   - Verify existing code generators continue to work

### Edge Cases
1. **Reference chains**: `A -> B -> C` where B is external
2. **Circular references**: Schema referring to itself through external file
3. **Same schema from different files**: Two files both defining `ErrorResponse`
4. **Missing external files**: Verify graceful handling
5. **Mixed resolution**: Some refs resolved, others not
6. **URL encoding**: Special characters in file paths
7. **allOf/oneOf/anyOf**: Composed schemas with external refs

## Documentation Updates

### Parser Documentation
1. **README.md**: Add section on `x-original-ref` extension
2. **ParseOptions documentation**: Explain that resolved schemas preserve original location
3. **Migration guide**: For users who want to leverage this feature

### Code Generator Documentation
1. **Example code**: Show how to use `x-original-ref` in generators
2. **Best practices**: Guidelines for handling shared schemas
3. **Use cases**: Document common patterns (shared errors, common types)

### API Documentation (Javadoc)
1. `SchemaProcessor.processReferenceSchema()`: Document that `x-original-ref` extension is added
2. `ParseOptions.setResolve()`: Mention original ref preservation behavior
3. Add examples in class-level documentation

### Changelog
```markdown
### Added
- Schemas loaded from external files now include an `x-original-ref` extension
  property that preserves the original external reference path. This enables
  code generators to distinguish between shared schema files and API-specific
  external schemas. (#2057)
```

## Additional Notes

### Design Considerations

1. **Extension vs. Standard Property**:
   - Using `x-original-ref` (extension) is OpenAPI-compliant and backward compatible
   - If this becomes widely adopted, could propose adding to OpenAPI specification
   - Extensions are preserved by most OpenAPI tools

2. **What to Store**:
   - Store the original `$ref` value exactly as it appeared in the source file
   - Preserve relative paths, URLs, and fragment identifiers
   - Do NOT normalize or resolve paths (users may want to analyze relative structure)

3. **Performance Impact**:
   - Minimal: just storing one additional string property per external reference
   - No impact on parsing time or memory for specs without external refs
   - Extension properties are standard map entries, no special handling needed

4. **Security Considerations**:
   - `x-original-ref` may expose internal file structure
   - Consider if sensitive paths should be sanitized (probably not - this is metadata about the API spec itself)
   - No execution or evaluation of the original ref value, just storage

### Future Enhancements

1. **Standardization**: Propose `$originalRef` or similar as standard OpenAPI property
2. **Additional metadata**: Could store resolution timestamp, original format, etc.
3. **Apply to all components**: Extend to responses, parameters, examples, etc.
4. **Parser option**: Add opt-in flag if users want to disable this feature
5. **Resolution graph**: Build complete dependency graph of all external references

### Code Generation Use Cases

1. **Shared Error Definitions**:
   ```
   errors.yaml (generate once)
   api1.yaml -> references errors.yaml (don't regenerate)
   api2.yaml -> references errors.yaml (don't regenerate)
   ```

2. **Common Domain Models**:
   ```
   common/user.yaml (shared across all APIs)
   common/address.yaml (shared across all APIs)
   api/orders.yaml (order-specific, include in generation)
   ```

3. **Multi-API Projects**:
   ```
   shared/
     errors.yaml
     pagination.yaml
   apis/
     users-api.yaml -> includes shared/
     orders-api.yaml -> includes shared/
     products-api.yaml -> includes shared/
   ```
   Each API generates code for its own schemas + shared schemas (only once total)

4. **Versioned Schemas**:
   ```
   common/v1/schemas.yaml
   common/v2/schemas.yaml
   api.yaml -> references common/v2/schemas.yaml
   ```
   Generator can track which version of shared schemas is used

### Implementation Priority

**High Priority**:
- SchemaProcessor modification for basic `x-original-ref` support
- Unit tests for common scenarios
- Basic documentation

**Medium Priority**:
- Apply to other component types (responses, parameters)
- Integration tests with code generators
- Comprehensive edge case testing

**Low Priority**:
- ParseOptions flag for opt-in/opt-out
- Advanced metadata (resolution timestamp, etc.)
- Standardization proposal to OpenAPI Initiative

### Community Impact

**Expected adoption**:
- High value for code generator developers
- Moderate value for API platform teams
- Low impact on casual OpenAPI users (transparent feature)

**Breaking changes**: None - fully backward compatible

**Migration path**: Automatic - existing code works, new features available immediately
