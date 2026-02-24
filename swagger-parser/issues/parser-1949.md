# Issue Analysis Template

## Issue Overview
**Issue Number:** #1949  
**Title:** [BUG] nested references to external schema are not resolved properly in swagger parser v3.  
**Status:** open  
**Created:** 2023-07-05T11:13:06Z  
**Updated:** 2024-02-09T15:47:01Z  
**URL:** https://github.com/swagger-api/swagger-parser/issues/1949

## Summary

The swagger-parser v3 fails to correctly resolve nested external references when an external schema file (schema_a.yaml) contains a reference to another external schema file (schema_b.yaml). The path resolution logic incorrectly concatenates the parent directory path with the reference path, resulting in duplicated path segments (e.g., `schema/com/wf/test/schema/com/wf/test/schema_b.yaml` instead of `schema/com/wf/test/schema_b.yaml`). This causes the parser to fail with "Unable to load RELATIVE ref" errors.

## Problem Statement

When parsing OpenAPI specifications with nested external references (references within referenced files), the parser fails to resolve the nested reference correctly. The issue manifests when:

1. A root schema references an external file (schema_a.yaml)
2. That external file (schema_a.yaml) contains a reference to another external file (schema_b.yaml)
3. Both files are located in the same directory or use relative paths from a common root

**Example Structure:**
```
├── schema
    ├── com
          ├── wf
               ├── test
                     ├── schema_a.yaml
                     ├── schema_b.yaml
```

**schema_a.yaml:**
```yaml
components:
  schemas:
    TypeFromB:
      $ref: 'schema/com/wf/test/schema_b.yaml#/components/schemas/MyType'
```

**Root schema:**
```yaml
components:
  schemas:
    in:
      properties:
        schema_a_ref:
          $ref: 'schema/com/wf/test/schema_a.yaml#/components/schemas/TypeFromB'
```

**Error:** `Unable to load RELATIVE ref: schema\com\wf\test\schema\com\wf\test\schema_b.yaml`

The parser incorrectly combines the parent path (`schema/com/wf/test/`) with the full reference path (`schema/com/wf/test/schema_b.yaml`), resulting in path duplication.

## Root Cause Analysis

The root cause is located in the `ExternalRefProcessor.processRefToExternalSchema()` method, specifically at lines 127-145 of `ExternalRefProcessor.java`.

### Problematic Code Flow:

1. **Initial Processing:**
   - When processing `schema_a.yaml`, the parser extracts the parent directory: `schema/com/wf/test/`
   - The schema contains a reference: `schema/com/wf/test/schema_b.yaml#/components/schemas/MyType`

2. **Path Concatenation Logic (Lines 132-143):**
   ```java
   String schemaFullRef = schema.get$ref(); // "schema/com/wf/test/schema_b.yaml#/..."
   String parent = (file.contains("/")) ? file.substring(0, file.lastIndexOf('/')) : ""; // "schema/com/wf/test"
   
   if (!parent.isEmpty() && !schemaFullRef.startsWith("/")) {
       if (schemaFullRef.contains("#/")) {
           String[] parts = schemaFullRef.split("#/");
           String schemaFullRefFilePart = parts[0]; // "schema/com/wf/test/schema_b.yaml"
           String schemaFullRefInternalRefPart = parts[1]; // "components/schemas/MyType"
           // PROBLEMATIC LINE:
           schemaFullRef = Paths.get(parent, schemaFullRefFilePart).normalize() + "#/" + schemaFullRefInternalRefPart;
       }
   }
   ```

3. **The Bug:**
   - `Paths.get("schema/com/wf/test", "schema/com/wf/test/schema_b.yaml")` produces `schema/com/wf/test/schema/com/wf/test/schema_b.yaml`
   - The code assumes `schemaFullRefFilePart` is a relative path from the parent directory
   - However, when the reference in the external file is already an absolute path from the schema root, it should not be concatenated with the parent path

### Why It Happens:

The issue occurs because the code does not check whether `schemaFullRefFilePart` already contains the parent path or is an absolute reference from the schema root. The logic unconditionally combines the parent directory with the reference path, leading to duplication when the reference is already a complete path.

### Expected Behavior:

The parser should detect that the reference path already includes the parent directory structure and avoid duplication. It should either:
- Use the reference path as-is if it's already a complete path from the root
- Only prepend the parent path if the reference is truly relative (e.g., `./schema_b.yaml` or `../test/schema_b.yaml`)

## Affected Components

### Modules:
- `swagger-parser-v3`

### Files:
- `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/processors/ExternalRefProcessor.java` (Primary)
- `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/ResolverCache.java` (Secondary - loads external references)
- `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/util/RefUtils.java` (Utility - ref format detection)

### Classes:
- `io.swagger.v3.parser.processors.ExternalRefProcessor` (Primary)
  - Method: `processRefToExternalSchema(String $ref, RefFormat refFormat)` (Lines 89-201)
  - Specific issue: Lines 127-145 (nested reference path resolution)
  
- `io.swagger.v3.parser.ResolverCache` (Secondary)
  - Method: `loadRef(String ref, RefFormat refFormat, Class<T> expectedType)` (Lines 113-146)
  - Responsible for loading external references but relies on correct paths from ExternalRefProcessor

- `io.swagger.v3.parser.util.RefUtils` (Utility)
  - Method: `computeRefFormat(String ref)` (Lines 78-92)
  - Method: `mungedRef(String refString)` (Lines 94-104)
  - Determines whether a reference is INTERNAL, RELATIVE, or URL format

## Technical Details

### Current Behavior

1. **First-level reference resolution** (root → schema_a.yaml):
   - Reference: `schema/com/wf/test/schema_a.yaml#/components/schemas/TypeFromB`
   - Parent: `` (empty for root)
   - Result: ✅ Correctly resolves to `schema/com/wf/test/schema_a.yaml`

2. **Second-level reference resolution** (schema_a.yaml → schema_b.yaml):
   - File being processed: `schema/com/wf/test/schema_a.yaml`
   - Reference in schema_a.yaml: `schema/com/wf/test/schema_b.yaml#/components/schemas/MyType`
   - Extracted parent: `schema/com/wf/test`
   - schemaFullRefFilePart: `schema/com/wf/test/schema_b.yaml`
   - Paths.get operation: `Paths.get("schema/com/wf/test", "schema/com/wf/test/schema_b.yaml")`
   - Result: ❌ `schema/com/wf/test/schema/com/wf/test/schema_b.yaml` (INCORRECT - path duplication)

### Expected Behavior

The parser should:
1. Detect that `schema/com/wf/test/schema_b.yaml` already contains the parent path `schema/com/wf/test`
2. Skip path concatenation and use the reference as-is
3. Result: ✅ `schema/com/wf/test/schema_b.yaml` (CORRECT)

Alternatively, for truly relative references:
- Reference: `./schema_b.yaml` or `schema_b.yaml`
- Parent: `schema/com/wf/test`
- Result: ✅ `schema/com/wf/test/schema_b.yaml` (CORRECT)

### Reproduction Steps

1. Create the following directory structure:
   ```
   ├── schema
       ├── com
             ├── wf
                  ├── test
                        ├── schema_a.yaml
                        ├── schema_b.yaml
   ```

2. Create schema_b.yaml:
   ```yaml
   openapi: 3.0.0
   info:
     title: SCHEMA B
     version: 1.0.2
   components:
     schemas:
       MyType:
         type: string
   ```

3. Create schema_a.yaml with a reference to schema_b.yaml:
   ```yaml
   openapi: 3.0.0
   info:
     title: SCHEMA A
     version: 1.0.2
   components:
     schemas:
       TypeFromB:
         $ref: 'schema/com/wf/test/schema_b.yaml#/components/schemas/MyType'
   ```

4. Create a root schema referencing schema_a.yaml:
   ```yaml
   openapi: 3.0.0
   info:
     title: REF SCHEMA A
     version: 1.0.1
   components:
     schemas:
       in:
         type: object
         properties:
           schema_a_ref:
             $ref: 'schema/com/wf/test/schema_a.yaml#/components/schemas/TypeFromB'
   ```

5. Parse using:
   ```java
   OpenAPIV3Parser parser = new OpenAPIV3Parser();
   ParseOptions options = new ParseOptions();
   options.setResolve(true);
   parser.readContents(yamlSchema, null, options, schemaDir.toAbsolutePath().toString());
   ```

6. Observe the error: `Unable to load RELATIVE ref: schema\com\wf\test\schema\com\wf\test\schema_b.yaml`

## Impact Assessment

**Severity:** High

**Affected Users:** 
- Users with nested external schema references (multi-level references)
- Teams using modular OpenAPI specifications with schemas split across multiple files
- Organizations with deep directory structures for schema organization
- Any project where external schemas reference other external schemas (6 users confirmed via GitHub reactions)

**Business Impact:**
- Prevents modular schema design and reusability
- Forces users to flatten directory structures or use workarounds
- Blocks migration of complex API specifications to OpenAPI 3.0
- Reduces code reusability and maintainability of API definitions

**Workarounds:**
1. **Use relative references with `./` prefix:**
   - In schema_a.yaml: `$ref: './schema_b.yaml#/components/schemas/MyType'`
   - This may work if files are in the same directory, but doesn't solve the general case

2. **Flatten directory structure:**
   - Place all schemas in the same directory
   - Loses organizational benefits of nested directories

3. **Use absolute references from schema root:**
   - Not a true workaround; this is what fails in the current implementation

4. **Inline nested schemas:**
   - Eliminates the nested reference entirely
   - Defeats the purpose of schema reusability

**Note:** None of the workarounds are satisfactory for complex, well-organized schema hierarchies.

## Proposed Solution

### Implementation Approach

**Solution 1: Check for Path Containment (Recommended)**

Modify the path concatenation logic in `ExternalRefProcessor.java` (lines 132-143) to detect when the reference already contains the parent path:

```java
String schemaFullRef = schema.get$ref();
String parent = (file.contains("/")) ? file.substring(0, file.lastIndexOf('/')) : "";

if (!parent.isEmpty() && !schemaFullRef.startsWith("/")) {
    if (schemaFullRef.contains("#/")) {
        String[] parts = schemaFullRef.split("#/");
        String schemaFullRefFilePart = parts[0];
        String schemaFullRefInternalRefPart = parts[1];
        
        // NEW: Check if reference already contains parent path or is absolute from root
        if (!schemaFullRefFilePart.startsWith(parent) && !schemaFullRefFilePart.startsWith("./")) {
            // Only prepend parent if reference doesn't already include it
            schemaFullRef = Paths.get(parent, schemaFullRefFilePart).normalize() + "#/" + schemaFullRefInternalRefPart;
        } else {
            // Reference already has full path or is explicitly relative
            schemaFullRef = Paths.get(schemaFullRefFilePart).normalize() + "#/" + schemaFullRefInternalRefPart;
        }
    } else {
        if (!schemaFullRef.startsWith(parent) && !schemaFullRef.startsWith("./")) {
            schemaFullRef = Paths.get(parent, schemaFullRef).normalize().toString();
        } else {
            schemaFullRef = Paths.get(schemaFullRef).normalize().toString();
        }
    }
    schemaFullRef = FilenameUtils.separatorsToUnix(schemaFullRef);
}
```

**Solution 2: Normalize Reference Context**

Store the absolute path context when loading each external file and resolve all nested references relative to the original root, not the current file's parent:

```java
// In ResolverCache or ExternalRefProcessor
private String resolveReference(String currentFile, String reference, String rootPath) {
    if (reference.startsWith("./") || reference.startsWith("../")) {
        // Truly relative reference - resolve from current file's directory
        String parent = currentFile.substring(0, currentFile.lastIndexOf('/'));
        return Paths.get(parent, reference).normalize().toString();
    } else if (reference.startsWith("/")) {
        // Absolute reference
        return reference.substring(1);
    } else {
        // Assume reference is from root
        return reference;
    }
}
```

**Solution 3: Enhanced Ref Format Detection**

Improve the `RefUtils.computeRefFormat()` and `mungedRef()` methods to better distinguish between:
- Root-relative references (e.g., `schema/com/wf/test/schema_b.yaml`)
- File-relative references (e.g., `./schema_b.yaml`, `../test/schema_b.yaml`)
- Absolute references (e.g., `/schema/com/wf/test/schema_b.yaml`)

### Alternatives Considered

1. **Always use relative references:**
   - Rejected: Doesn't address the core issue of path duplication
   - Would require users to change their schema files

2. **Track reference resolution context:**
   - Considered: Maintain a stack of file contexts during resolution
   - Rejected: More complex implementation, harder to maintain

3. **Use URI resolution:**
   - Considered: Use Java's URI class for proper relative/absolute resolution
   - Rejected: Would require significant refactoring of existing code

4. **Post-process to remove duplicates:**
   - Considered: Detect and remove duplicate path segments
   - Rejected: Fragile heuristic, doesn't address root cause

### Recommended Approach

**Solution 1** (Check for Path Containment) is recommended because:
- Minimal code changes (focused fix)
- Backward compatible with existing schemas
- Addresses the specific issue without major refactoring
- Easy to test and validate
- Low risk of introducing new bugs

## Dependencies

### Related Issues:
- Potentially related to other external reference resolution issues in the repository
- May share root cause with similar path resolution bugs in other reference types (parameters, responses, etc.)

### External Dependencies:
- `java.nio.file.Paths` - Used for path normalization
- `org.apache.commons.io.FilenameUtils` - Used for separator conversion
- No new dependencies required for the fix

## Testing Considerations

### Unit Tests:
1. **Test nested external references with identical paths:**
   - schema_a.yaml references schema_b.yaml using full path from root
   - Verify no path duplication occurs

2. **Test nested external references with relative paths:**
   - schema_a.yaml references schema_b.yaml using `./schema_b.yaml`
   - Verify correct resolution

3. **Test nested external references with parent navigation:**
   - schema_a.yaml references schema_b.yaml using `../other/schema_b.yaml`
   - Verify correct resolution

4. **Test deeply nested references (3+ levels):**
   - schema_a → schema_b → schema_c
   - Verify all levels resolve correctly

5. **Test mixed reference styles:**
   - Root schema uses full path, nested schema uses relative path
   - Verify both resolve correctly

### Integration Tests:
1. **Test real-world schema structures:**
   - Use the exact structure from the issue report
   - Verify successful parsing with `options.setResolve(true)`

2. **Test cross-directory references:**
   - Schemas in different directories referencing each other
   - Verify path resolution respects directory structure

3. **Test with different file path separators:**
   - Unix-style (/) and Windows-style (\\) paths
   - Verify normalization works correctly

### Edge Cases:
1. **Circular references:**
   - schema_a → schema_b → schema_a
   - Should be handled by existing circular reference detection

2. **References with special characters:**
   - Spaces, unicode characters in paths
   - Verify proper encoding/decoding

3. **Empty parent path:**
   - Root schema in same directory as referenced schemas
   - Verify no path manipulation when parent is empty

4. **Absolute file system paths:**
   - References starting with `/` or drive letters (Windows)
   - Verify proper handling

5. **URL references in nested schemas:**
   - External schema contains HTTP URL reference
   - Verify URL format is preserved

6. **Fragment-only references:**
   - `#/components/schemas/Type` within an external file
   - Verify internal reference resolution still works

## Documentation Updates

### Code Documentation:
- Add comprehensive JavaDoc comments to `processRefToExternalSchema()` method explaining the path resolution logic
- Document the difference between root-relative and file-relative references
- Add inline comments explaining the path containment check

### User Documentation:
- Update OpenAPI parser documentation to clarify supported reference formats:
  - Root-relative: `schema/path/to/file.yaml`
  - File-relative: `./file.yaml`, `../other/file.yaml`
  - Absolute: `/schema/path/to/file.yaml`
  - URL: `http://example.com/schema.yaml`

### Examples:
- Add example showing nested external references
- Add example showing mixed reference styles
- Add example of recommended directory structure for modular schemas

### Migration Guide:
- Document any changes in reference resolution behavior
- Provide guidance for users experiencing this issue
- Show before/after examples

## Additional Notes

### Community Feedback:
- Issue has 6 positive reactions (+1), indicating significant user impact
- Multiple users confirmed experiencing the same issue
- User @latyshas provided detailed root cause analysis (2023-07-18)
- User @DavidShiel suggested code improvement (2024-02-09)

### Investigation Notes:
1. The issue was correctly identified by community members
2. The `Paths.get(parent, schemaFullRefFilePart)` operation is the direct cause
3. Java's `Paths.get()` treats the second parameter as relative to the first, causing concatenation even when the second parameter is already a complete path
4. The `normalize()` method doesn't remove the duplication because it's valid path structure (just not the intended structure)

### Performance Considerations:
- String operations for path checking are minimal and won't impact performance
- Path normalization is already performed in the current code
- No additional file I/O required for the fix

### Security Considerations:
- Path traversal: Ensure `normalize()` still prevents `../../../etc/passwd` style attacks
- Maintain existing security checks in `ResolverCache` and `RefUtils`
- No new security risks introduced by the fix

### Backward Compatibility:
- Fix should not break existing working schemas
- Schemas using truly relative references (`./`, `../`) should continue to work
- Schemas that worked around the bug may need testing, but unlikely to break

### Future Improvements:
1. Consider implementing URI-based resolution for more robust path handling
2. Add comprehensive test suite for all reference resolution scenarios
3. Consider adding reference resolution debugging/tracing mode
4. Evaluate need for reference resolution strategy configuration (strict vs. lenient)
