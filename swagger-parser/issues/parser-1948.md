# Issue Analysis Template

## Issue Overview
**Issue Number:** #1948  
**Title:** [BUG] Fix Swagger Parser doesn't handle $refs correctly when they are relatives without '../'  
**Status:** Open  
**Created:** July 30, 2023  
**Updated:** July 30, 2023  
**URL:** https://github.com/swagger-api/swagger-parser/issues/1948

## Summary

The Swagger Parser incorrectly handles relative references that don't use the parent directory notation (`../`). When a reference in the format `$ref: 'product-components.yaml#/components/parameters/param1'` is used from within a subdirectory file (`product/product-api.yaml`), the parser fails to resolve the path correctly, treating the current working directory as the root instead of the directory containing the referring file. This is a regression from version 2.0.31 and prior versions, likely introduced by PR #1629.

## Problem Statement

When OpenAPI/Swagger files are organized in a directory structure with relative references between sibling files (files in the same directory), the parser fails to resolve references that don't explicitly use `../` notation. 

**File Structure Example:**
```
├── openapi.yaml
├── product
    ├── product-api.yaml
    ├── product-components.yaml
```

**Failing Reference (from product-api.yaml):**
```yaml
- $ref: 'product-components.yaml#/components/parameters/param1'  # FAILS
```

**Working Reference:**
```yaml
- $ref: '../product/product-components.yaml#/components/parameters/param1'  # WORKS
```

**Error Message:**
```
java.lang.RuntimeException: Unable to load RELATIVE ref: product-components.yaml 
path: swagger-parser/modules/swagger-parser-v3/src/test/resources/issue
	at io.swagger.v3.parser.util.RefUtils.readExternalRef(RefUtils.java:220)
```

The error shows that the parser is attempting to load the file from the project root directory (`/src/test/resources/issue`) instead of from the directory containing the referring file (`/src/test/resources/issue/product`).

## Root Cause Analysis

The root cause lies in the `RefUtils.readExternalRef()` method, specifically in lines 214-216 of `RefUtils.java`. The problematic code path is triggered when:

1. A file path doesn't exist at the first attempted resolution (line 198)
2. The fallback logic (lines 203-216) attempts to handle the reference
3. For references without `..`, line 215 executes: `url = parentDirectory + url.substring(url.indexOf(".") + 1);`

**Critical Issue in Line 215:**
```java
url = parentDirectory + url.substring(url.indexOf(".") + 1);
```

This line assumes that all relative references without `..` must start with `./`. It searches for the first dot (`.`) in the filename and tries to extract everything after `./`. However, when a reference like `product-components.yaml` is provided:

1. `url.indexOf(".")` finds the dot in `.yaml` (the file extension), not a `./` prefix
2. `url.substring(url.indexOf(".") + 1)` returns `yaml` (everything after the first dot)
3. The result becomes `parentDirectory + "yaml"`, which is incorrect

**The Correct Behavior:**
The parser should recognize that `product-components.yaml` is a relative reference to a sibling file and should resolve it relative to the parent directory of the current file. This worked correctly in version 2.0.31 and earlier.

**Why `../product/product-components.yaml` Works:**
The code has a separate branch for references containing `..` (lines 204-213), which properly handles parent directory navigation. However, this shouldn't be necessary for sibling file references.

**Compounding Issue:**
The `mungedRef()` method (lines 94-104) is supposed to add `./` prefix to relative references, but it has a condition on line 100:
```java
!refString.contains("$") &&
refString.indexOf(".") > 0
```
This checks if the first dot appears after position 0, which is true for `product-components.yaml` (dot at position 18), but the method still doesn't get called appropriately in all contexts.

## Affected Components

### Modules:
- `swagger-parser-v3`

### File(s):
- `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/util/RefUtils.java`
- `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/processors/ExternalRefProcessor.java`
- `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/ResolverCache.java`

### Class(es):
- `io.swagger.v3.parser.util.RefUtils`
  - Method: `readExternalRef()` (lines 184-235)
  - Method: `mungedRef()` (lines 94-104)
  - Method: `computeRefFormat()` (lines 78-92)

### Related PR:
- PR #1629 (likely introduced the regression)

## Technical Details

### Current Behavior

When processing a reference from `product/product-api.yaml` to `product-components.yaml`:

1. **Initial Resolution Attempt (line 198):**
   ```java
   final Path pathToUse = parentDirectory.resolve(file).normalize();
   ```
   - `parentDirectory` = `/path/to/resources/issue/product`
   - `file` = `product-components.yaml`
   - `pathToUse` = `/path/to/resources/issue/product/product-components.yaml`
   - This should work, but if it doesn't exist due to path issues...

2. **Fallback Logic (lines 214-216):**
   ```java
   } else {
       url = parentDirectory + url.substring(url.indexOf(".") + 1);
   }
   ```
   - `url` = `product-components.yaml`
   - `url.indexOf(".")` returns 17 (position of dot in `.yaml`)
   - `url.substring(18)` returns `yaml`
   - Result: `/path/to/resources/issue/product` + `yaml` = incorrect path

3. **Classpath Fallback (line 224):**
   ```java
   result = ClasspathHelper.loadFileFromClasspath(file);
   ```
   - Attempts to load `product-components.yaml` from classpath root
   - Fails because the file is at `issue/product/product-components.yaml`

### Expected Behavior

The parser should:

1. Recognize `product-components.yaml` as a relative reference to a sibling file
2. Resolve it relative to the directory containing the referring file (`product/`)
3. Successfully load `/path/to/resources/issue/product/product-components.yaml`

This behavior was working correctly in swagger-parser version 2.0.31 and earlier.

### Reproduction Steps

1. Create the following file structure:
   ```
   ├── openapi.yaml
   ├── product/
       ├── product-api.yaml
       ├── product-components.yaml
   ```

2. In `openapi.yaml`, reference:
   ```yaml
   paths:
     /findById/{param1}:
       $ref: 'product/product-api.yaml#/paths/findById~1{param1}'
   ```

3. In `product/product-api.yaml`, use a sibling reference:
   ```yaml
   paths:
     findById/{param1}:
       get:
         parameters:
           - $ref: 'product-components.yaml#/components/parameters/param1'
   ```

4. Parse with:
   ```java
   OpenAPIV3Parser parser = new OpenAPIV3Parser();
   ParseOptions options = new ParseOptions();
   options.setResolve(true);
   SwaggerParseResult result = parser.readLocation("openapi.yaml", null, options);
   ```

5. Observe the RuntimeException: "Unable to load RELATIVE ref: product-components.yaml"

## Impact Assessment

**Severity:** High

**Affected Users:** 
- Users who organize OpenAPI specifications in a directory structure with files in subdirectories
- Users who use relative references between sibling files without explicit `../` notation
- Teams migrating from swagger-parser 2.0.31 or earlier versions
- Users following common OpenAPI organization patterns where related schemas/components are kept in the same directory

**Workarounds:** 
1. **Explicit Parent Directory Reference:** Use `../product/product-components.yaml` instead of `product-components.yaml`
   - Drawback: Less intuitive, requires knowledge of the parent directory structure
   - Drawback: Brittle - breaks if directory structure changes

2. **Downgrade:** Revert to swagger-parser version 2.0.31 or earlier
   - Drawback: Lose bug fixes and features from newer versions
   - Drawback: Not a long-term solution

3. **Flatten Directory Structure:** Keep all files in the same directory as the root OpenAPI file
   - Drawback: Poor organization for large API specifications
   - Drawback: Not scalable for complex projects

**Note:** Interestingly, schema references in response bodies or request bodies work correctly in both formats, indicating the issue is specific to parameter references (and potentially other ref types).

## Proposed Solution

### Solution 1: Fix the Fallback Logic in RefUtils.readExternalRef() (RECOMMENDED)

**Implementation Approach:**

Modify the fallback logic in `RefUtils.readExternalRef()` method (lines 214-216) to properly handle relative references without `../`:

```java
} else {
    // For references without "..", attempt to resolve relative to parent directory
    // First check if the file starts with "./" - if so, strip it
    String relativePath = file;
    if (relativePath.startsWith("./")) {
        relativePath = relativePath.substring(2);
    }
    
    // Try resolving as-is first (sibling file)
    final Path siblingPath = parentDirectory.resolve(relativePath).normalize();
    if (Files.exists(siblingPath)) {
        result = readAll(siblingPath);
    } else {
        // If not found, try the old logic as fallback
        // This handles edge cases but shouldn't be the primary path
        url = parentDirectory.toString() + "/" + relativePath;
        final Path pathToUse2 = Paths.get(url).normalize();
        if (Files.exists(pathToUse2)) {
            result = readAll(pathToUse2);
        }
    }
}
```

**Rationale:**
- Removes the broken `url.indexOf(".")` logic that incorrectly identifies file extensions as path separators
- Leverages Java's `Path.resolve()` which properly handles relative path resolution
- Maintains backward compatibility by keeping fallback logic
- Aligns with standard file system path resolution semantics

### Solution 2: Enhance mungedRef() to Consistently Add "./" Prefix

**Implementation Approach:**

Improve the `mungedRef()` method to ensure all relative references without schemes get normalized with `./` prefix:

```java
public static String mungedRef(String refString) {
    // Ref: IETF RFC 3986, Section 5.2.2
    if (!refString.contains(":") &&      // No scheme
            !refString.startsWith("#") && // Not a fragment-only reference
            !refString.startsWith("/") && // Not an absolute path
            !refString.startsWith(".") && // Not already relative with ./ or ../
            !refString.contains("$")) {   // Not a JSON path expression
        // This is a relative reference without explicit path prefix
        return "./" + refString;
    }
    return refString;
}
```

Then ensure this method is called consistently before path resolution in `computeRefFormat()` and throughout the resolution chain.

**Rationale:**
- Normalizes all relative references to have an explicit `./` prefix
- Makes path resolution logic simpler and more predictable
- Aligns with RFC 3986 relative reference resolution standards

### Solution 3: Simplify Path Resolution Logic

**Implementation Approach:**

Remove the complex fallback logic entirely and rely on Java NIO's `Path.resolve()`:

```java
} else {
    // Its assumed to be a relative file ref
    // Normalize the reference first
    String normalizedFile = file;
    if (normalizedFile.startsWith("./")) {
        normalizedFile = normalizedFile.substring(2);
    }
    
    final Path pathToUse = parentDirectory.resolve(normalizedFile).normalize();
    
    if (Files.exists(pathToUse)) {
        result = readAll(pathToUse);
    } else {
        // Only fall back to classpath if file doesn't exist on filesystem
        result = ClasspathHelper.loadFileFromClasspath(file);
    }
}
```

**Rationale:**
- Simplifies the code significantly
- Removes error-prone string manipulation
- Leverages battle-tested Java NIO path resolution
- Easier to maintain and understand

### Alternatives Considered

1. **Change Reference Format Documentation:**
   - Considered: Document that all sibling references must use `../directory/file.yaml` format
   - Rejected: This is counter-intuitive and breaks existing specifications that worked in version 2.0.31

2. **Special Case for Classpath Resolution:**
   - Considered: Use different logic for classpath vs filesystem references
   - Rejected: Adds complexity; the issue exists in both contexts

3. **Use ExternalRefProcessor.join() Method:**
   - Considered: Leverage the existing `join()` method for all path resolution
   - Partially viable: The `join()` method (lines 1107-1134 in ExternalRefProcessor.java) uses URI resolution which might be more robust, but would require refactoring

## Dependencies

### Related Issues:
- Issue #1949: [BUG] nested references to external schema are not resolved properly in swagger parser v3
  - This is the same root cause: nested/chained references compound the path resolution issue
  - When `schema_a.yaml` references `schema/com/wf/test/schema_b.yaml`, the path gets duplicated

### External Dependencies:
- Java NIO file system API (`java.nio.file.Path`, `java.nio.file.Files`)
- Apache Commons IO (`org.apache.commons.io.IOUtils`)
- URI resolution (RFC 3986 standards)

### Regression Source:
- PR #1629 (needs investigation to understand what changes caused this regression)

## Testing Considerations

### Unit Tests:

1. **Test Sibling File References:**
   ```java
   @Test
   public void testSiblingFileReference() {
       // Given: Files in product/product-api.yaml and product/product-components.yaml
       // When: product-api.yaml references 'product-components.yaml'
       // Then: Reference should resolve successfully
   }
   ```

2. **Test Relative References Without Prefix:**
   ```java
   @Test
   public void testRelativeReferenceWithoutDotSlash() {
       // Given: Reference 'components.yaml' (no ./ prefix)
       // When: Resolving from same directory
       // Then: Should resolve successfully
   }
   ```

3. **Test Relative References With ./ Prefix:**
   ```java
   @Test
   public void testRelativeReferenceWithDotSlash() {
       // Given: Reference './components.yaml'
       // When: Resolving from same directory
       // Then: Should resolve successfully (existing behavior)
   }
   ```

4. **Test Parent Directory References:**
   ```java
   @Test
   public void testParentDirectoryReference() {
       // Given: Reference '../product/components.yaml'
       // When: Resolving from subdirectory
       // Then: Should resolve successfully (existing working behavior)
   }
   ```

### Integration Tests:

1. **Test Complete Multi-File Specification:**
   - Create a realistic directory structure with multiple nested references
   - Test that all references resolve correctly
   - Verify that resolved OpenAPI object has all expected components

2. **Test Chained References (Issue #1949):**
   - Test A references B, B references C
   - Verify all references resolve with correct paths
   - Ensure paths don't get duplicated

3. **Test Mixed Reference Styles:**
   - Combine `./`, `../`, and no-prefix references in same specification
   - Verify all resolve correctly

### Edge Cases:

1. **Files with Multiple Dots in Name:**
   - Test references like `my.component.schema.yaml`
   - Ensure file extension detection doesn't break on multiple dots

2. **References with URL Encoding:**
   - Test references with special characters
   - Ensure encoding is handled correctly

3. **Case Sensitivity:**
   - Test on both case-sensitive and case-insensitive file systems
   - Ensure consistent behavior

4. **Symlinks:**
   - Test resolution with symbolic links in path
   - Ensure normalized paths work correctly

5. **Deeply Nested Directories:**
   - Test references in deeply nested directory structures
   - Verify performance and correctness

6. **Circular References:**
   - Test A references B, B references A
   - Ensure no infinite loops

## Documentation Updates

1. **CHANGELOG.md:**
   - Document the bug fix
   - Note that relative references without `../` now work correctly
   - Mention this restores behavior from version 2.0.31

2. **README.md or User Guide:**
   - Add examples of correct relative reference formats
   - Document supported reference patterns:
     - `./sibling.yaml` (explicit relative)
     - `sibling.yaml` (implicit relative, same directory)
     - `../parent/file.yaml` (parent directory)
     - `subdir/file.yaml` (subdirectory)

3. **JavaDoc:**
   - Update `RefUtils.readExternalRef()` documentation
   - Clarify behavior for different reference formats
   - Add examples of supported patterns

4. **Migration Guide:**
   - For users on versions between 2.0.31 and the fix version
   - Explain that relative references now work without `../` workaround
   - Note that existing workarounds will continue to work

## Additional Notes

### Historical Context:
The issue mentions this is a regression from version 2.0.31, indicating that the correct behavior existed previously. Investigating the changes in PR #1629 would provide insight into what specific modification introduced this regression.

### OpenAPI Specification Alignment:
The OpenAPI Specification doesn't prescribe a specific format for relative references, but follows standard URI/URL resolution rules (RFC 3986). The expected behavior (sibling file references without `../`) aligns with standard relative URI resolution.

### Performance Considerations:
The current fallback logic with string manipulation is not only incorrect but also potentially slower than using Java NIO's built-in path resolution. The proposed solutions should improve both correctness and performance.

### Related OpenAPI Tools:
Other OpenAPI tools (swagger-ui, redoc, spectral) typically handle relative references correctly. Users may be confused when swagger-parser has different behavior, expecting consistency across the OpenAPI ecosystem.

### Schema vs Parameter References:
The issue notes that schema references in response bodies work correctly while parameter references fail. This suggests the issue may be in how different reference types are processed, potentially in:
- `ExternalRefProcessor.processRefToExternalParameter()` (line 755 referenced in stack trace)
- Different code paths for schema vs parameter resolution

This discrepancy should be investigated to ensure the fix applies uniformly to all reference types.

### Security Considerations:
When fixing path resolution, ensure that:
- Path traversal attacks are prevented (normalized paths stay within allowed directories)
- The existing `PermittedUrlsChecker` security checks remain effective
- No new attack vectors are introduced by simplified path resolution

### Backward Compatibility:
The fix should maintain backward compatibility with:
- Existing workarounds using `../` notation (should continue to work)
- References with explicit `./` prefix (should continue to work)
- URL references (should not be affected)
- Internal references (should not be affected)
