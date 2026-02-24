# Issue Analysis Template

## Issue Overview
**Issue Number:** #2055  
**Title:** External Refs with same name are ignored  
**Status:** Open  
**Created:** 2024-02-09T12:29:31Z  
**Updated:** 2024-02-21T19:46:47Z  
**URL:** https://github.com/swagger-api/swagger-parser/issues/2055

## Summary

When multiple external references point to components with identical names but different definitions across separate files, the swagger-parser incorrectly treats them as the same component. Only the first encountered definition is processed and added to the components section, while subsequent definitions with the same name are silently ignored. This results in incorrect API specification parsing where operations reference the wrong component definitions, violating the principle that external references should be uniquely identified by their full reference path (file + JSON pointer), not just the component name.

## Problem Statement

The swagger-parser resolves external references by deriving component names from the last element of the JSON pointer path (e.g., `limit` from `external_ref_1.json#/limit`). This approach creates name collisions when different external files contain components with identical names but distinct definitions.

**Concrete Example:**
- `external_ref_1.json#/limit` defines a parameter with `maximum: 1, minimum: 0`
- `external_ref_2.json#/limit` defines a parameter with `maximum: 2, minimum: 0`

Both references resolve to the component name `limit`, causing the parser to:
1. Process `external_ref_1.json#/limit` and add it to `components.parameters.limit`
2. Encounter `external_ref_2.json#/limit`, derive the same name `limit`
3. Find that `limit` already exists in `components.parameters`
4. Skip adding the second definition, leaving references to `external_ref_2.json#/limit` pointing to the wrong parameter

This violates the OpenAPI specification's intent that `$ref` values should be unique identifiers, and causes runtime errors where operations receive incorrect parameter schemas.

## Root Cause Analysis

The root cause lies in the `RefUtils.computeDefinitionName()` method, which implements a simplistic naming strategy:

```java
// RefUtils.java:43-45
if (definitionPath != null) { 
    //the name will come from the last element of the definition path
    final String[] jsonPathElements = definitionPath.split("/");
    plausibleName = jsonPathElements[jsonPathElements.length - 1];
}
```

**Why this is problematic:**

1. **Loss of Context**: The method extracts only the final segment of the JSON pointer (e.g., `limit` from `#/limit`), discarding the file path that provides uniqueness.

2. **JSON Pointer Semantics Violation**: JSON pointers are designed to be evaluated within a specific document context. The same pointer in different documents refers to different objects, but `computeDefinitionName()` treats them as identical.

3. **Incomplete Collision Handling**: While `ExternalRefProcessor` has collision detection logic:
   - For **schemas** (`finalNameRec` method): Implements recursive renaming with suffixes (`_1`, `_2`, etc.)
   - For **parameters, responses, requestBodies**: Only checks if a component exists but does NOT implement renaming logic when content differs

4. **Cache Inconsistency**: The `ResolverCache.putRenamedRef()` is called with the same name for different refs:
   ```java
   // Both calls store the same newRef = "limit"
   cache.putRenamedRef("external_ref_1.json#/limit", "limit");
   cache.putRenamedRef("external_ref_2.json#/limit", "limit"); // Overwrites!
   ```

The comment in `RefUtils.java` acknowledges this is intentional ("the name will come from the last element") but provides no justification, and this behavior is not documented in the OpenAPI specification.

## Affected Components

### Modules
- `swagger-parser-v3`

### Files
- **`modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/util/RefUtils.java`**  
  Lines 29-62, specifically `computeDefinitionName()` method (lines 29-62)

- **`modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/processors/ExternalRefProcessor.java`**  
  Multiple methods affected:
  - `processRefToExternalParameter()` (lines ~505-570)
  - `processRefToExternalResponse()` (lines 411-495)
  - `processRefToExternalRequestBody()` (lines ~570-640)
  - `processRefToExternalExample()` (lines ~640-710)
  - `processRefToExternalHeader()` (lines ~700-770)
  - `processRefToExternalLink()` (lines ~758-820)
  - `processRefToExternalCallback()` (lines ~815+)

### Classes
- `io.swagger.v3.parser.util.RefUtils`
- `io.swagger.v3.parser.processors.ExternalRefProcessor`

## Technical Details

### Current Behavior

When parsing an OpenAPI specification with external references:

1. **Reference Resolution**: `ExternalRefProcessor` calls `RefUtils.computeDefinitionName($ref)` to determine the component name
   ```java
   final String possiblyConflictingDefinitionName = computeDefinitionName($ref);
   ```

2. **Name Derivation**: For ref `external_ref_2.json#/limit`, the method:
   - Splits by `#/` → `["external_ref_2.json", "limit"]`
   - Takes the definition path: `"limit"`
   - Splits by `/` → `["limit"]`
   - Returns last element: `"limit"`

3. **Collision Check**: For parameters (and most component types except schemas):
   ```java
   Parameter existingParameters = parameters.get(possiblyConflictingDefinitionName);
   if (existingParameters != null) {
       LOGGER.debug("A model for " + existingParameters + " already exists");
       if(existingParameters.get$ref() != null) {
           existingParameters = null; // Use new model
       }
       // NOTE: No else clause with recursive renaming!
   }
   newRef = possiblyConflictingDefinitionName; // Always uses same name
   cache.putRenamedRef($ref, newRef);
   ```

4. **Result**: The second parameter is NOT added to components, and its reference is cached with the same name as the first parameter.

### Expected Behavior

1. **Unique Component Names**: Each distinct external reference should result in a unique component name that preserves the source file context.

2. **Proper Resolution**: References like:
   - `external_ref_1.json#/limit` → `components.parameters.external_ref_1_limit`
   - `external_ref_2.json#/limit` → `components.parameters.external_ref_2_limit`

3. **Correct Updates**: All original `$ref` values should be updated to point to the correct renamed component.

4. **Cache Consistency**: The renamed ref cache should maintain the mapping:
   ```
   "external_ref_1.json#/limit" → "external_ref_1_limit"
   "external_ref_2.json#/limit" → "external_ref_2_limit"
   ```

### Reproduction Steps

1. Create `openapi.json` with multiple operations referencing external files:
   ```json
   {
     "paths": {
       "/a-path": {
         "post": {
           "parameters": [{"$ref": "./external_ref_1.json#/limit"}]
         },
         "put": {
           "parameters": [{"$ref": "./external_ref_2.json#/limit"}]
         }
       }
     }
   }
   ```

2. Create `external_ref_1.json` and `external_ref_2.json` with identically-named but different parameter definitions:
   ```json
   // external_ref_1.json
   {"limit": {"name": "limit", "schema": {"type": "integer", "maximum": 1}}}
   
   // external_ref_2.json
   {"limit": {"name": "limit", "schema": {"type": "integer", "maximum": 2}}}
   ```

3. Parse with resolve enabled:
   ```java
   ParseOptions options = new ParseOptions();
   options.setResolve(true);
   OpenAPI openAPI = new OpenAPIV3Parser().read("openapi.json", null, options);
   System.out.println("Parameters: " + openAPI.getComponents().getParameters().keySet());
   ```

4. **Observe**: Only `[limit]` is printed, not `[external_ref_1_limit, external_ref_2_limit]` or similar

5. **Verify**: The PUT operation's parameter incorrectly uses the POST operation's parameter definition (maximum: 1 instead of maximum: 2)

## Impact Assessment

**Severity:** High

**Affected Users:** 
- API developers using external file references with modular OpenAPI specifications
- Organizations with reusable component libraries where different modules define components with common names (e.g., `limit`, `error`, `id`)
- Code generators and validation tools that rely on accurate component resolution
- Any application where parameter/response differences are semantically important (different validation rules, different schemas)

**Real-World Impact:**
- **Silent Data Corruption**: Generated clients/servers use wrong schemas without warning
- **Validation Failures**: Runtime validation fails because actual data doesn't match the incorrectly substituted schema
- **Security Issues**: If different endpoints have different validation rules (e.g., different maximum values), using the wrong definition could allow invalid inputs
- **Debugging Difficulty**: The issue is silent (only DEBUG logging) and difficult to trace since the error manifests at runtime, not parse time

**Workarounds:**

1. **Manually Rename Components**: Ensure all components across all external files have globally unique names (e.g., `limit_v1`, `limit_v2`)
   - **Limitation**: Requires coordination across teams, defeats purpose of modular design

2. **Inline All Definitions**: Avoid external references by copying all component definitions into the main OpenAPI file
   - **Limitation**: Increases file size, reduces maintainability, eliminates reusability

3. **Single External File**: Consolidate all shared components into one external file
   - **Limitation**: Not scalable for large organizations with multiple component libraries

4. **Use Unique JSON Pointers**: Structure external files with unique paths (e.g., `#/v1/limit`, `#/v2/limit`)
   - **Limitation**: Works but requires specific file structure conventions; still creates collisions if files use same structure

**Current Status:** None of these workarounds are ideal, and the issue fundamentally violates the principle of reference resolution.

## Proposed Solution

### Primary Solution: Include File Context in Component Names

Modify `RefUtils.computeDefinitionName()` to incorporate the source file name when deriving component names from external references.

**Implementation:**

```java
public static String computeDefinitionName(String ref) {
    final String[] refParts = ref.split(REFERENCE_SEPARATOR);
    
    if (refParts.length > 2) {
        throw new RuntimeException("Invalid ref format: " + ref);
    }
    
    final String file = refParts[0];
    final String definitionPath = refParts.length == 2 ? refParts[1] : null;
    
    String plausibleName;
    
    if (definitionPath != null) {
        // Extract component name from JSON pointer
        final String[] jsonPathElements = definitionPath.split("/");
        String componentName = jsonPathElements[jsonPathElements.length - 1];
        
        // For external refs, prepend sanitized file identifier
        if (!file.isEmpty() && !file.startsWith("#")) {
            String fileIdentifier = extractFileIdentifier(file);
            plausibleName = fileIdentifier + "_" + componentName;
        } else {
            plausibleName = componentName;
        }
    } else {
        // Original logic for file-only refs
        final String[] filePathElements = file.split("/");
        plausibleName = filePathElements[filePathElements.length - 1];
        
        final String[] split = plausibleName.split("\\.");
        if (split.length > 2) {
            plausibleName = String.join("", Arrays.copyOf(split, split.length - 1));
        } else {
            plausibleName = split[0];
        }
    }
    
    return plausibleName;
}

private static String extractFileIdentifier(String file) {
    // Extract base filename without path and extension
    String[] pathParts = file.split("/");
    String filename = pathParts[pathParts.length - 1];
    
    // Remove extension
    String[] fileParts = filename.split("\\.");
    String baseName = fileParts[0];
    
    // Sanitize for use in component name (replace invalid chars)
    return baseName.replaceAll("[^a-zA-Z0-9_-]", "_");
}
```

**Result:**
- `external_ref_1.json#/limit` → `external_ref_1_limit`
- `external_ref_2.json#/limit` → `external_ref_2_limit`
- `#/components/schemas/User` → `User` (unchanged, internal ref)

### Implementation Approach

1. **Update `RefUtils.computeDefinitionName()`**
   - Add file context to external reference names
   - Preserve existing behavior for internal references
   - Sanitize file names to valid component name characters

2. **Extend Collision Detection**
   - Apply `finalNameRec`-style recursive renaming to all component types, not just schemas
   - Ensure content comparison before deciding to skip or rename

3. **Update Cache Management**
   - Verify `ResolverCache.putRenamedRef()` correctly handles the new naming scheme
   - Ensure reference updates propagate to all usage locations

4. **Backward Compatibility**
   - Add configuration option `ParseOptions.setPreserveExternalRefNames(boolean)` to control behavior
   - Default to new behavior for correctness, allow opt-in to legacy behavior for compatibility

5. **Testing**
   - Unit tests for `RefUtils.computeDefinitionName()` with various ref formats
   - Integration tests with multiple external files containing same-named components
   - Regression tests to ensure existing specifications parse identically (when using legacy mode)

### Alternatives Considered

#### Alternative 1: Hash-Based Naming
Generate component names using content hash:
```java
plausibleName = componentName + "_" + sha256(ref).substring(0, 8);
```

**Pros:**
- Guaranteed uniqueness
- Deterministic

**Cons:**
- Non-human-readable component names
- Harder to debug
- Changes to file content change component names unexpectedly
- **Rejected**: Poor developer experience

#### Alternative 2: Full Path Encoding
Encode the entire reference path:
```java
plausibleName = ref.replace("/", "_").replace(".", "_").replace("#", "_");
```
Example: `external_ref_1_json_limit`

**Pros:**
- Complete uniqueness
- Reversible

**Cons:**
- Very long component names for deep paths
- URLs would create extremely long names
- **Rejected**: Names become unwieldy

#### Alternative 3: Implement Recursive Renaming for All Types
Keep `computeDefinitionName()` unchanged but implement `finalNameRec`-like logic for parameters, responses, etc.

**Pros:**
- Minimal change to existing code
- Already proven pattern (used for schemas)

**Cons:**
- Doesn't address root cause
- Results in names like `limit_1`, `limit_2` which don't indicate source
- Harder to debug which component comes from which file
- **Considered**: Could be combined with primary solution as fallback

#### Alternative 4: Maintain Separate Component Namespaces per File
Create sub-objects in components keyed by file:
```json
{
  "components": {
    "parameters": {
      "external_ref_1": {
        "limit": {...}
      },
      "external_ref_2": {
        "limit": {...}
      }
    }
  }
}
```

**Cons:**
- Not compliant with OpenAPI specification structure
- Breaks existing tooling
- **Rejected**: Specification violation

**Selected Approach:** Primary solution with Alternative 3 as collision fallback provides best balance of correctness, readability, and compatibility.

## Dependencies

### Related Issues
- Issue #1621: Referenced in `RefUtils.java` line 51 (filename dot handling)
- Issue #1865: Referenced in `RefUtils.java` line 51 (filename dot handling)
- Potentially related to any issue involving external reference resolution

### External Dependencies
- **OpenAPI Specification**: https://spec.openapis.org/oas/v3.0.3#reference-object
  - Section 4.8.26 (Reference Object): "$ref" string must be a valid JSON Reference
  - JSON Reference (RFC 6901): JSON Pointers are document-relative
- **JSON Reference**: https://datatracker.ietf.org/doc/html/draft-pbryan-zyp-json-ref-03
- **JSON Pointer**: https://datatracker.ietf.org/doc/html/rfc6901

### Code Dependencies
- `ResolverCache`: Manages reference-to-component name mappings
- `ExternalRefProcessor`: All `processRefToExternal*` methods
- `OpenAPIV3Parser`: Entry point for parsing with resolution
- Component models: `Parameter`, `ApiResponse`, `RequestBody`, `Example`, `Header`, `Link`, `Callback`, `Schema`

## Testing Considerations

### Unit Tests

**File:** `RefUtilsTest.java`

1. **Test Collision Detection:**
   ```java
   @Test
   public void testComputeDefinitionNameWithDifferentFilesAndSameName() {
       String ref1 = "file1.json#/components/parameters/limit";
       String ref2 = "file2.json#/components/parameters/limit";
       String name1 = RefUtils.computeDefinitionName(ref1);
       String name2 = RefUtils.computeDefinitionName(ref2);
       assertNotEquals(name1, name2);
       assertTrue(name1.contains("file1"));
       assertTrue(name2.contains("file2"));
   }
   ```

2. **Test Internal Reference Preservation:**
   ```java
   @Test
   public void testComputeDefinitionNameInternalRef() {
       String ref = "#/components/schemas/User";
       String name = RefUtils.computeDefinitionName(ref);
       assertEquals("User", name);
   }
   ```

3. **Test Path Sanitization:**
   ```java
   @Test
   public void testComputeDefinitionNameSpecialChars() {
       String ref = "./my-file!@#.json#/limit";
       String name = RefUtils.computeDefinitionName(ref);
       assertTrue(name.matches("[a-zA-Z0-9_-]+"));
   }
   ```

### Integration Tests

**File:** `ExternalRefProcessorTest.java` or new test file

1. **Test Multiple External Files:**
   - Create test scenario matching issue #2055
   - Verify all components are added with unique names
   - Verify references are correctly updated
   - Verify resolved OpenAPI contains all distinct definitions

2. **Test Nested External References:**
   - External file A references external file B
   - Both have components named "error"
   - Verify both are preserved with unique names

3. **Test Cache Consistency:**
   - Parse same specification twice
   - Verify renamed references are consistent across parses
   - Verify cache returns correct mappings

### Edge Cases

1. **URL References:** `https://example.com/schemas/v1.json#/User`
2. **Deep Paths:** `../../shared/common/parameters.json#/deeply/nested/param`
3. **Same File, Different Pointers:** `file.json#/limit` vs `file.json#/params/limit`
4. **Circular References:** File A refs B, file B refs A, both have same-named components
5. **Case Sensitivity:** `File.json#/Limit` vs `file.json#/limit`
6. **Unicode in Filenames:** `fichier-données.json#/paramètre`
7. **Reserved Characters:** Files/paths with `#`, `$`, `/`

## Documentation Updates

### Files Requiring Updates

1. **README.md**
   - Add note about external reference resolution behavior
   - Document naming conventions for resolved components

2. **JavaDoc**
   - `RefUtils.computeDefinitionName()`: Update documentation to explain file-based naming
   - `ExternalRefProcessor`: Document component naming strategy

3. **Migration Guide** (if breaking change)
   - Create migration document explaining:
     - What changed and why
     - How to opt-in to legacy behavior
     - How to update specifications that rely on old behavior
     - Component name format examples

4. **CHANGELOG.md**
   - Add entry describing the fix and potential breaking changes

### User-Facing Documentation

If swagger-parser has a documentation site, add:
- Best practices for external references
- Explanation of component name derivation
- Examples of modular OpenAPI specifications
- Troubleshooting guide for reference resolution issues

## Additional Notes

### Why This Issue Matters

This is not merely a cosmetic issue about naming. The incorrect component resolution can lead to:

1. **Contract Violations**: Clients and servers generated from the parsed specification will implement incorrect interfaces
2. **Runtime Failures**: Requests that should be valid are rejected (or vice versa) due to wrong validation schemas
3. **Security Implications**: If different endpoints have different security constraints encoded in parameters (e.g., rate limits), mixing them up could bypass security controls
4. **Silent Failures**: The parser doesn't warn or error, making this very difficult to debug

### Historical Context

The current implementation appears to prioritize simplicity and human-readable names over correctness. The comment "the name will come from the last element of the definition path" suggests this was an intentional design decision, but:

1. No specification or documentation justifies this choice
2. The OpenAPI spec doesn't mandate this behavior
3. The behavior contradicts the principle that `$ref` is a unique identifier

The schema processor's `finalNameRec` method shows awareness of collision issues and implements proper handling, but this pattern wasn't applied to other component types, creating inconsistent behavior.

### Specification Compliance

According to the OpenAPI Specification 3.0.3:
- **Reference Object**: "The $ref keyword is a JSON Reference"
- **JSON Reference**: URIs that identify a specific value within a JSON document

The spec treats `$ref` as a unique locator. Two different `$ref` strings (even if they point to similarly-structured objects) should be treated as potentially different references. The parser's current behavior of conflating them violates this principle.

### Community Feedback

The issue has received positive reactions (+1), and a comment from @kota65535 asking "Is this an expected behavior?" indicates confusion in the community about whether this is a bug or intended behavior. This ambiguity suggests the behavior is not well-documented and potentially surprising to users.

### Implementation Complexity

The proposed fix is relatively straightforward:
- **Low Risk**: Changes are localized to `RefUtils` and don't require modifications to the core parsing logic
- **Testable**: Easy to verify with unit and integration tests
- **Backward Compatible**: Can be made optional with a configuration flag

The main challenge is deciding on the naming convention (should it be `file_component`, `file.component`, or something else) and ensuring the chosen format doesn't create new collision scenarios.

### Performance Considerations

The proposed solution has negligible performance impact:
- String manipulation overhead is minimal
- No additional network requests or file I/O
- Cache behavior unchanged
- Memory footprint may slightly increase due to longer component names, but this is insignificant for typical specifications

### Recommendation

**Priority**: High  
**Effort**: Medium (2-3 days for implementation, testing, and documentation)  
**Risk**: Low (with proper testing and optional backward compatibility mode)

This issue should be addressed in the next minor version release, as it represents incorrect behavior that can cause significant problems for users with modular OpenAPI specifications.
