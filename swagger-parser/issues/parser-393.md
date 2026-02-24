# Issue Analysis Template

## Issue Information
- **Issue Number**: #393
- **Title**: Vendor extension references not resolved
- **Reporter**: arr28
- **Created**: 2017-02-03T22:35:50Z
- **Labels**: None
- **Status**: open

## Problem Statement

When parsing a Swagger/OpenAPI specification file, the swagger-parser correctly parses vendor extensions (custom properties starting with `x-`) and includes them in the swagger model. However, JSON references (`$ref`) within vendor extensions are not being resolved, unlike references in standard Swagger specification locations (e.g., parameters, schemas, responses).

This limitation prevents users from creating reusable vendor extension definitions at the top level of their specification and referencing them from within operations or responses, which is a common pattern for defining custom metadata like error definitions, additional documentation, or custom validation rules.

## Technical Analysis

### Affected Components
- **swagger-parser** core parsing logic
- JSON reference resolution mechanism (`$ref` handling)
- Vendor extensions parsing
- SwaggerDeserializer or related deserialization components

### Root Cause
The swagger-parser implements JSON reference resolution according to the OpenAPI/Swagger specification, which explicitly defines where `$ref` is permitted. The specification does not mandate or define behavior for `$ref` within vendor extensions (`x-*` properties), so the parser does not resolve references in these locations.

This is intentional behavior based on specification compliance - the parser only resolves references where the OpenAPI specification explicitly allows them. Vendor extensions are treated as opaque custom data, and their internal structure (including potential references) is not processed by the generic reference resolver.

### Current Behavior
1. Top-level vendor extensions (e.g., `x-error-defs`) are parsed and included in the swagger model
2. Response-level vendor extensions (e.g., `x-error-refs`) are parsed and included in the response model
3. JSON references within vendor extensions (e.g., `$ref: '#/x-error-defs/credentialTooShort'`) are **not** resolved
4. The raw string reference remains in the vendor extension data structure

### Expected Behavior
Users expect that JSON references within vendor extensions would be resolved in the same way as references in standard specification locations, allowing them to:
- Define reusable vendor extension fragments at the document root
- Reference those fragments from operation/response vendor extensions
- Access fully resolved vendor extension data in code generation templates

## Reproduction Steps

1. Create a Swagger/OpenAPI specification with top-level vendor extensions:
```yaml
x-error-defs:
  credentialTooShort:
    errorID: credentialTooShort
    message: Credential %1 is shorter than the minimum allowed (%2).
    variables:
      - The credential field which is too short.
      - The minimum permitted credential length.
  credentialTooLong:
    errorID: credentialTooLong
    message: Credential %1 is longer than the maximum allowed (%2).
    variables:
      - The credential field which is too long.
      - The maximum permitted credential length.
```

2. Add a response with vendor extension references:
```yaml
'400':
  description: |
    The account could not be created because a credential didn't meet the complexity requirements.
  schema:
    $ref: './shared/error.yaml#/error'
  x-error-refs:
    - '$ref': '#/x-error-defs/credentialTooShort'
    - '$ref': '#/x-error-defs/credentialTooLong'
```

3. Parse the specification using swagger-parser
4. Inspect the vendor extension data in the parsed model
5. Observe that the `$ref` values remain as strings and are not resolved to the actual error definitions

## Proposed Solution

### Approach

Based on the maintainer's response (fehguy), there are two potential approaches:

**Option 1: Parser Extension (Recommended by Maintainer)**
Create a custom parser extension that resolves vendor extension references as a post-processing step. This keeps the core parser specification-compliant while allowing users who need this functionality to opt-in.

**Option 2: Built-in Vendor Extension Reference Resolution (Not Recommended)**
Add generic reference resolution for vendor extensions to the core parser. This was explicitly rejected by the maintainer as it falls outside the project scope and specification compliance.

### Implementation Details

**For Option 1 (Parser Extension):**

1. Create a custom extension/processor that:
   - Takes the parsed swagger model as input
   - Traverses all vendor extensions in the model
   - Identifies properties named `$ref` within vendor extensions
   - Resolves these references against the swagger model (including other vendor extensions)
   - Replaces the reference strings with the resolved content

2. Package the extension as a separate library or module

3. Users include the extension in their classpath when using swagger-codegen or other tools

4. The extension is invoked after standard parsing is complete

### Code Locations

Key areas to investigate for implementing an extension:

- **Reference Resolution Logic**: Examine how standard `$ref` resolution works in the parser
  - Look for classes handling `RefFormat`, `RefUtils`, or similar
  - Study the `ResolverCache` or equivalent caching mechanisms

- **Vendor Extensions Storage**: Understand how vendor extensions are stored in the model
  - Check `VendorExtensible` interface or similar
  - Look at how `x-*` properties are mapped to `vendorExtensions` maps

- **Extension Points**: Identify hooks or extension points in the parser
  - Look for plugin/extension interfaces
  - Check if there's a post-processing phase where custom logic can be injected

Potential classes to examine:
- `SwaggerDeserializer`
- `ResolverFully` or similar resolver classes
- Model classes implementing `VendorExtensible`

### Testing Strategy

1. **Unit Tests**: Create test specifications with various vendor extension reference patterns
   - Top-level to response-level references
   - Nested vendor extension references
   - References to non-existent vendor extensions (error handling)
   - Circular references in vendor extensions

2. **Integration Tests**: Test with swagger-codegen to ensure resolved extensions are available in templates

3. **Edge Cases**:
   - External file references in vendor extensions
   - Mixed vendor extension content (some with refs, some without)
   - Vendor extensions at different levels (top-level, operation, response, parameter)

## Potential Risks & Considerations

1. **Specification Compliance**: Resolving references in vendor extensions is not part of the OpenAPI specification. This could lead to confusion about what is standard behavior vs. custom extension behavior.

2. **Backwards Compatibility**: If implemented as a core feature, existing users who may have `$ref` strings in their vendor extensions for other purposes (not expecting resolution) could experience breaking changes.

3. **Performance**: Additional reference resolution could impact parsing performance, especially for large specifications with many vendor extensions.

4. **Circular References**: Need to handle circular reference detection in vendor extensions to avoid infinite loops.

5. **External References**: Need to decide whether to support external file references in vendor extensions (e.g., `$ref: 'external-file.yaml#/x-custom-def'`).

6. **Maintenance Burden**: Adding this functionality to the core parser increases the maintenance burden and test surface area.

7. **Ambiguity**: Without specification guidance, edge cases and expected behavior may be ambiguous.

## Related Issues

- **swagger-api/swagger-codegen#4527**: The original issue where this problem was first reported
  - Reporter wanted to use vendor extensions to define reusable error definitions
  - Used workaround with JavaScript to fix up cross-references in HTML output
  
- **swagger-api/swagger-codegen#4022**: Previous issue about vendor extensions in responses not working at all (fixed)

## Additional Context

### Maintainer's Response

From fehguy's comment on 2017-02-04:
> "Well.... good news and bad news. The good news is, the parser is parsing the extensions just fine, and the top-level as well as response extensions are in the swagger model. The bad news is that indeed it is not resolving, but unfortunately that's outside the project scope. It'll parse but not do generic resolving outside where the spec says you can."

This indicates a clear project decision to maintain specification compliance and not resolve references outside of spec-defined locations.

### Workaround

The reporter (arr28) mentioned using a workaround:
- Generate HTML output with unresolved references
- Use JavaScript in the HTML to resolve cross-references client-side
- This approach works for their use case but is not a general solution

### Use Case

The primary use case is defining common error information as vendor extensions that can be referenced from multiple operation responses, enabling:
- Documentation generation showing which specific errors each operation can return
- Code generation with complete error metadata
- DRY (Don't Repeat Yourself) principle for error definitions

## Complexity Estimate

- **Effort**: Medium to High
  - Low if implemented as an optional external extension (Medium)
  - High if implemented as core parser feature (requires extensive testing and specification consideration)
  
- **Impact**: Medium
  - Affects users who want to use vendor extensions with references
  - Does not affect users who don't use this pattern
  - Enables new use cases for vendor extensions
  
- **Priority**: Low to Medium
  - The issue has been open since 2017 with only 1 thumbs-up reaction
  - Workarounds exist (client-side resolution, direct inclusion without references)
  - Not breaking existing functionality
  - Nice-to-have feature for specific use cases

## References

- **OpenAPI Specification 2.0**: https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md
  - Section on Specification Extensions (vendor extensions)
  - Section on `$ref` and JSON Reference usage
  
- **JSON Reference Specification (RFC)**: https://tools.ietf.org/html/draft-pbryan-zyp-json-ref-03
  - Defines JSON Reference syntax and resolution
  
- **Related GitHub Issues**:
  - Original issue: https://github.com/swagger-api/swagger-parser/issues/393
  - Swagger Codegen issue: https://github.com/swagger-api/swagger-codegen/issues/4527
  
- **Swagger Parser Project**: https://github.com/swagger-api/swagger-parser
