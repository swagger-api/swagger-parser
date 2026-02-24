# Issue Analysis Template

## Issue Overview
**Issue Number:** #2147  
**Title:** Cannot deserialize from Object value (GraalVM native executable)  
**Status:** Open  
**Created:** N/A (accessed 2024)  
**Updated:** N/A  
**URL:** https://github.com/swagger-api/swagger-parser/issues/2147

## Summary
The swagger-parser library fails to properly parse OpenAPI/Swagger files when running in GraalVM native image executables. The parser returns empty OpenAPI objects without errors in most cases, and in some cases throws `InvalidDefinitionException` for Jackson deserialization of `io.swagger.v3.oas.models.media.ArraySchema` due to missing reflection configuration required by GraalVM native images.

## Problem Statement
When using swagger-parser in a GraalVM native executable (compiled using `native-image`), the library encounters severe limitations:

1. **Silent failures**: Most OpenAPI files are parsed but return empty OpenAPI objects with no error messages, making debugging extremely difficult.
2. **Deserialization errors**: In at least one case (books.yaml example), the parser throws a Jackson `InvalidDefinitionException` indicating that `ArraySchema` cannot be instantiated because GraalVM cannot deserialize from Object value without proper reflection metadata.
3. **Insufficient documentation**: There is no guidance on how to configure reflection for GraalVM native image compilation with swagger-parser.

The error message explicitly states:
```
Cannot construct instance of `io.swagger.v3.oas.models.media.ArraySchema`: 
cannot deserialize from Object value (no delegate- or property-based Creator): 
this appears to be a native image, in which case you may need to configure 
reflection for the class that is to be deserialized
```

The user attempted to initialize swagger models at runtime using `--initialize-at-run-time=io.swagger.v3.oas.models` but this did not resolve the issue.

## Root Cause Analysis

### Primary Cause: Missing GraalVM Reflection Configuration
GraalVM's native image compilation uses static analysis to determine which classes, methods, and fields should be available at runtime. However, Jackson's dynamic reflection-based deserialization requires runtime reflection that is disabled by default in GraalVM native images.

The root causes are:

1. **Jackson Reflection Requirements**: The swagger-parser library uses Jackson (`com.fasterxml.jackson.databind.ObjectMapper`) for deserializing YAML/JSON into Java objects. Jackson heavily relies on reflection to:
   - Discover constructors (default, property-based, delegate-based)
   - Access fields and getter/setter methods
   - Instantiate objects dynamically

2. **Swagger Model Classes**: The OpenAPI model classes from `io.swagger.core.v3:swagger-models` (version 2.2.37) include complex class hierarchies:
   - `io.swagger.v3.oas.models.media.Schema` and its subclasses
   - `io.swagger.v3.oas.models.media.ArraySchema`
   - `io.swagger.v3.oas.models.media.ComposedSchema`
   - Many other model classes for paths, operations, parameters, responses, etc.

3. **Dynamic Type Conversion**: The issue manifests specifically at line 1188 of `SwaggerConverter.java`:
   ```java
   ArraySchema arraySchema = Json.mapper().convertValue(v2Model, ArraySchema.class);
   ```
   This `convertValue()` call requires Jackson to deserialize the v2 model into an ArraySchema instance, which requires reflection metadata that is not available in the native image.

4. **Missing Reflection Metadata**: GraalVM's native-image tool cannot automatically detect:
   - All the OpenAPI model classes that need reflection
   - Their constructors, fields, and methods
   - Jackson annotations and their effects
   - Polymorphic type hierarchies (e.g., Schema -> ArraySchema, ComposedSchema, etc.)

## Affected Components

### Modules:
- **swagger-parser-v2-converter**: The conversion from Swagger 2.0 to OpenAPI 3.x uses Jackson's `convertValue()` method
- **swagger-parser-v3**: Parsing and deserializing OpenAPI 3.x specifications
- **swagger-parser-core**: Core parsing utilities
- **swagger-parser**: Main entry point wrapper

### File(s):
- `modules/swagger-parser-v2-converter/src/main/java/io/swagger/v3/parser/converter/SwaggerConverter.java` (line 1188 and similar patterns)
- `modules/swagger-parser-v3/src/main/java/io/swagger/v3/parser/util/OpenAPIDeserializer.java`
- All parser utilities that use Jackson for deserialization

### Class(es):
**Swagger Parser Classes:**
- `io.swagger.v3.parser.converter.SwaggerConverter`
- `io.swagger.v3.parser.util.OpenAPIDeserializer`
- `io.swagger.v3.parser.util.InlineModelResolver`
- `io.swagger.v3.parser.util.ResolverFully`
- `io.swagger.v3.parser.processors.*` (all processor classes)

**OpenAPI Model Classes (from io.swagger.core.v3:swagger-models:2.2.37):**
- `io.swagger.v3.oas.models.OpenAPI`
- `io.swagger.v3.oas.models.info.*` (Info, Contact, License)
- `io.swagger.v3.oas.models.media.*` (Schema, ArraySchema, ComposedSchema, StringSchema, IntegerSchema, ObjectSchema, etc.)
- `io.swagger.v3.oas.models.parameters.*` (Parameter, QueryParameter, PathParameter, etc.)
- `io.swagger.v3.oas.models.responses.*` (ApiResponse, ApiResponses)
- `io.swagger.v3.oas.models.security.*` (SecurityScheme, OAuthFlows, etc.)
- `io.swagger.v3.oas.models.servers.*` (Server, ServerVariable)
- `io.swagger.v3.oas.models.tags.Tag`
- `io.swagger.v3.oas.models.PathItem`
- `io.swagger.v3.oas.models.Paths`
- `io.swagger.v3.oas.models.Components`
- `io.swagger.v3.oas.models.ExternalDocumentation`
- `io.swagger.v3.oas.models.headers.Header`
- `io.swagger.v3.oas.models.Operation`
- `io.swagger.v3.oas.models.callbacks.*`
- `io.swagger.v3.oas.models.examples.*`
- `io.swagger.v3.oas.models.links.*`

**Swagger v2 Model Classes (from io.swagger:swagger-core:1.6.16):**
- `io.swagger.models.Swagger`
- `io.swagger.models.Model` and subclasses
- `io.swagger.models.ArrayModel`
- `io.swagger.models.ComposedModel`
- `io.swagger.models.properties.*`
- All other v2 model classes

**Jackson Classes:**
- `com.fasterxml.jackson.databind.ObjectMapper`
- Jackson's internal deserialization infrastructure

## Technical Details

### Current Behavior
1. When parsing a Swagger 2.0 file (like books.yaml), the SwaggerConverter attempts to convert v2 models to v3 models
2. At line 1188, `Json.mapper().convertValue(v2Model, ArraySchema.class)` is called
3. Jackson attempts to deserialize the v2Model into an ArraySchema instance
4. GraalVM native image lacks reflection metadata for ArraySchema's constructors and methods
5. Jackson fails with `InvalidDefinitionException: cannot deserialize from Object value (no delegate- or property-based Creator)`
6. The error propagates up the stack, causing parsing to fail

In other cases where the error is caught or different code paths are taken, parsing appears to succeed but returns empty or incomplete OpenAPI objects.

### Expected Behavior
1. The swagger-parser should successfully parse OpenAPI/Swagger files in GraalVM native images
2. All model classes should be properly instantiated through reflection
3. The library should either:
   - Provide comprehensive GraalVM reflection configuration files
   - Document the required GraalVM configuration for users
   - Use GraalVM-compatible deserialization approaches

### Reproduction Steps
1. Create a GraalVM native image application that uses swagger-parser
2. Include the swagger-parser dependency (version 2.1.x)
3. Add a Swagger 2.0 file (like the books.yaml example from the issue)
4. Use `OpenAPIParser` or similar to parse the file
5. Compile with GraalVM native-image tool
6. Run the native executable
7. Observe either:
   - Empty OpenAPI objects returned with no error
   - `InvalidDefinitionException` for ArraySchema or other model classes

## Impact Assessment

**Severity:** High

**Affected Users:** 
- Any developers building GraalVM native images that need to parse OpenAPI/Swagger specifications
- Applications using frameworks like Quarkus, Micronaut, or Spring Native that compile to native images
- Microservices and cloud-native applications leveraging GraalVM for faster startup and lower memory footprint
- CLI tools built with GraalVM that need OpenAPI parsing capabilities

**Workarounds:**
1. **Use JVM instead of Native Image**: Run the application on a standard JVM instead of compiling to native image (defeats the purpose of using GraalVM)
2. **Pre-parse specifications at build time**: Parse OpenAPI files during build and serialize the parsed objects, though this is inflexible
3. **Manual reflection configuration**: Users can attempt to manually create `reflect-config.json` for all required classes, but this is:
   - Time-consuming and error-prone
   - Requires deep knowledge of the library internals
   - Must be updated when swagger-parser or swagger-models versions change
   - Difficult to ensure completeness

Current workarounds are inadequate for production use.

## Proposed Solution

### Implementation Approach

#### Solution 1: Provide Comprehensive GraalVM Reflection Configuration (Recommended)

**Steps:**
1. Create `src/main/resources/META-INF/native-image/io.swagger.parser.v3/swagger-parser/` directories in relevant modules
2. Generate complete reflection configuration files:
   - `reflect-config.json`: Register all OpenAPI model classes, constructors, fields, and methods
   - `resource-config.json`: Include any resource files needed
   - `jni-config.json`: If any JNI is used (likely not needed)
   - `proxy-config.json`: For dynamic proxies if needed

3. **Generate reflection configuration using GraalVM tracing agent:**
   ```bash
   java -agentlib:native-image-agent=config-output-dir=config \
        -jar swagger-parser-test-app.jar
   ```
   Then refine and include the generated configuration files

4. **Key classes to register for reflection:**
   - All classes in `io.swagger.v3.oas.models.*` packages
   - All classes in `io.swagger.models.*` packages  
   - Jackson mixin classes
   - Any classes referenced via `@JsonTypeInfo` or `@JsonSubTypes`

5. **Sample reflect-config.json structure:**
   ```json
   [
     {
       "name": "io.swagger.v3.oas.models.media.ArraySchema",
       "allDeclaredConstructors": true,
       "allPublicConstructors": true,
       "allDeclaredMethods": true,
       "allPublicMethods": true,
       "allDeclaredFields": true,
       "allPublicFields": true
     },
     {
       "name": "io.swagger.v3.oas.models.media.Schema",
       "allDeclaredConstructors": true,
       "allPublicConstructors": true,
       "allDeclaredMethods": true,
       "allPublicMethods": true,
       "allDeclaredFields": true,
       "allPublicFields": true
     }
     // ... hundreds more entries for all model classes
   ]
   ```

6. Add automated tests that compile and run with GraalVM native-image to ensure the configuration remains valid

**Pros:**
- Standard GraalVM approach
- No code changes required
- Works with existing Jackson deserialization
- Configuration is automatically loaded by GraalVM

**Cons:**
- Requires comprehensive class enumeration
- Configuration must be maintained as model classes evolve
- Large configuration files (could be hundreds of classes)

#### Solution 2: Use Jackson's ParameterNamesModule and @JsonCreator

**Steps:**
1. Add Jackson's `jackson-module-parameter-names` dependency
2. Ensure all model classes have proper `@JsonCreator` annotations
3. Compile with `-parameters` flag to retain parameter names
4. Configure ObjectMapper to use ParameterNamesModule

**Pros:**
- Reduces reflection requirements
- More GraalVM-friendly

**Cons:**
- Requires changes to swagger-models (upstream dependency)
- May not fully eliminate reflection needs

#### Solution 3: Hybrid Approach with Build-Time Initialization

**Steps:**
1. Provide reflection configuration (Solution 1)
2. Use `--initialize-at-build-time` for safe classes
3. Document which classes should be initialized at build time vs runtime

**Pros:**
- Optimizes native image size and startup time
- Balances flexibility and performance

**Cons:**
- Requires careful analysis of initialization dependencies
- Complex configuration

### Recommended Implementation

**Implement Solution 1 (Comprehensive Reflection Configuration) with these specific steps:**

1. **For each module, create reflection configurations:**
   - `swagger-parser-v2-converter/src/main/resources/META-INF/native-image/io.swagger.parser.v3/swagger-parser-v2-converter/reflect-config.json`
   - `swagger-parser-v3/src/main/resources/META-INF/native-image/io.swagger.parser.v3/swagger-parser-v3/reflect-config.json`
   - `swagger-parser/src/main/resources/META-INF/native-image/io.swagger.parser.v3/swagger-parser/reflect-config.json`

2. **Create automated reflection config generation:**
   - Add a test module with GraalVM agent tracing
   - Include comprehensive test cases covering all OpenAPI features
   - Run tests with native-image-agent to capture all reflection usage
   - Merge and deduplicate configurations

3. **Add native image integration tests:**
   - Create a separate test module that builds a native image
   - Include tests that parse various OpenAPI specifications
   - Run as part of CI/CD pipeline
   - Use GraalVM's native-image-maven-plugin

4. **Document GraalVM usage:**
   - Add a dedicated documentation page for GraalVM native image support
   - Include examples of common configurations
   - Provide troubleshooting guide
   - List tested GraalVM versions

### Alternatives Considered

1. **Complete rewrite without Jackson**: Replace Jackson with a GraalVM-compatible parser
   - **Rejected**: Too invasive, would require massive changes and break compatibility

2. **Lazy deserialization with custom deserializers**: Write custom Jackson deserializers that don't rely on reflection
   - **Rejected**: Extremely complex, would need custom deserializers for hundreds of classes

3. **Use Jackson's build-time code generation**: Leverage ahead-of-time compilation for Jackson
   - **Rejected**: Not mature enough, limited Jackson version support

4. **Require users to provide their own reflection config**: Document the issue and let users handle it
   - **Rejected**: Poor user experience, unrealistic for most users to create comprehensive configuration

## Dependencies

### Related Issues:
- Likely affects all OpenAPI parsing when used in GraalVM native images
- May be related to similar issues in other Jackson-based libraries
- Check swagger-core repository for related GraalVM issues

### External Dependencies:
- **GraalVM Native Image**: Requires GraalVM 21.0+ (or compatible)
- **Jackson**: Currently using 2.19.0 - need to verify GraalVM compatibility
- **io.swagger.core.v3:swagger-models**: Version 2.2.37 - all model classes need reflection
- **io.swagger:swagger-core**: Version 1.6.16 - v2 model classes need reflection

### Dependency on Upstream Projects:
- If swagger-models were to add GraalVM annotations (`@RegisterForReflection` or similar), it would simplify this
- Consider contributing reflection configuration upstream to swagger-core project

## Testing Considerations

### Unit Tests:
- Existing unit tests should continue to pass
- Add tests specifically for verifying reflection configuration completeness
- Mock GraalVM reflection checks if possible

### Integration Tests:
- **Critical**: Create integration tests that actually build and run native images
- Test parsing various OpenAPI specifications:
  - OpenAPI 3.0.x files
  - OpenAPI 3.1.x files
  - Swagger 2.0 files (with conversion)
  - Files with all schema types (array, object, composed, etc.)
  - Files with complex references and inheritance
  - Files with security schemes, callbacks, links, etc.

### Native Image Test Setup:
```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <version>0.10.1</version>
    <executions>
        <execution>
            <id>test-native</id>
            <goals>
                <goal>test</goal>
            </goals>
            <phase>test</phase>
        </execution>
    </executions>
</plugin>
```

### Edge Cases:
1. **Polymorphic deserialization**: Schema subtypes (ArraySchema, ObjectSchema, etc.)
2. **Circular references**: $ref handling with complex reference chains
3. **Custom extensions**: x- prefixed vendor extensions
4. **All HTTP methods**: Ensure all operation types work
5. **All parameter types**: Path, query, header, cookie parameters
6. **All security schemes**: API Key, OAuth2, OpenID Connect, HTTP Auth
7. **Complex composed schemas**: allOf, oneOf, anyOf combinations
8. **Discriminators**: Polymorphic schema discrimination
9. **Callbacks**: Callback definitions and references
10. **Links**: Response links
11. **Examples**: Example objects and references

### Test Matrix:
- GraalVM versions: 21.0.x, 22.0.x, 23.0.x
- Java versions: 11, 17, 21
- OpenAPI versions: 2.0, 3.0.0, 3.0.1, 3.0.2, 3.0.3, 3.1.0
- Platforms: Linux x64, macOS x64/arm64, Windows x64

## Documentation Updates

### Required Documentation:

1. **README.md**:
   - Add "GraalVM Native Image Support" section
   - Link to detailed GraalVM guide

2. **New File: docs/graalvm-native-image.md**:
   ```markdown
   # GraalVM Native Image Support
   
   ## Overview
   Swagger Parser provides full support for GraalVM native image compilation...
   
   ## Requirements
   - GraalVM 21.0+ or compatible
   - native-image tool installed
   
   ## Configuration
   The library includes built-in reflection configuration...
   
   ## Building Native Images
   [Step-by-step guide]
   
   ## Troubleshooting
   [Common issues and solutions]
   
   ## Tested Configurations
   [Matrix of tested versions]
   ```

3. **API Documentation**:
   - Add JavaDoc comments mentioning GraalVM compatibility
   - Document any GraalVM-specific considerations

4. **Migration Guide**:
   - If configuration format changes, provide migration guide
   - Document any breaking changes

5. **Examples**:
   - Create example project demonstrating GraalVM usage
   - Include sample native-image build configuration
   - Provide docker-based build example

## Additional Notes

### Performance Considerations:
- Reflection configuration may increase native image size
- Build time will increase due to reflection processing
- Runtime performance should not be significantly affected
- Consider documenting expected image size impact

### Maintenance:
- Reflection configuration must be updated when:
  - Swagger-models version changes
  - New model classes are added
  - Jackson version changes significantly
  - GraalVM introduces breaking changes

### Future Improvements:
1. **Annotation-based configuration**: Investigate using GraalVM's annotation processors for automatic configuration generation
2. **Feature detection**: Add runtime detection of GraalVM native image environment with better error messages
3. **Minimal configuration**: Create minimal reflection configs for common use cases
4. **Dynamic feature usage**: Explore using GraalVM's dynamic features with reduced reflection
5. **Upstream contributions**: Work with swagger-core team to add native image support upstream

### Community Engagement:
- Once implemented, announce GraalVM support in release notes
- Create blog post or tutorial
- Share in GraalVM community channels
- Consider presenting at conferences (e.g., JavaOne, Devoxx)

### Backwards Compatibility:
- All changes should be backwards compatible
- Reflection configuration doesn't affect JVM runtime behavior
- Ensure existing applications continue to work unchanged

### Security Considerations:
- Reflection can expose internal implementation details
- Review reflection configuration for sensitive data exposure
- Consider using sealed types where appropriate (Java 17+)
- Document security implications in GraalVM guide

### References:
- [GraalVM Native Image Documentation](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Jackson GraalVM Support](https://github.com/FasterXML/jackson-databind/blob/2.19/docs/graalvm-native-image.md)
- [GraalVM Reflection Configuration](https://www.graalvm.org/latest/reference-manual/native-image/metadata/)
- [Spring Native Lessons Learned](https://spring.io/blog/2021/03/11/announcing-spring-native-beta)
