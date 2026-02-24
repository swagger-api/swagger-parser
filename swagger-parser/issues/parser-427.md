# Issue Analysis Template

## Issue Information
- **Issue Number**: #427
- **Title**: create OSGi bundle artifacts
- **Reporter**: borcsokj
- **Created**: 2017-03-27T09:26:20Z
- **Labels**: None
- **Status**: open

## Problem Statement

The swagger-parser project currently generates plain JAR artifacts without OSGi bundle metadata. This prevents the library from being used as a dependency in OSGi containers such as Apache Karaf. The reporter specifically needs to use Swagger parser as a dependency of Swagger request validator inside Apache Karaf OSGi container installed as a feature.

The swagger-core project already provides OSGi bundle artifacts, and swagger-parser should follow the same pattern to maintain consistency across the Swagger ecosystem and enable OSGi deployment scenarios.

## Technical Analysis

### Affected Components

The following modules need to be converted from plain JAR to OSGi bundle packaging:

1. **swagger-parser** (`modules/swagger-parser/pom.xml`) - Main parser module
2. **swagger-parser-v3** (`modules/swagger-parser-v3/pom.xml`) - OpenAPI 3.x parser
3. **swagger-parser-v2-converter** (`modules/swagger-parser-v2-converter/pom.xml`) - Swagger 2.0 to OpenAPI 3.0 converter
4. **swagger-parser-core** (`modules/swagger-parser-core/pom.xml`) - Core parsing utilities
5. **swagger-parser-safe-url-resolver** (`modules/swagger-parser-safe-url-resolver/pom.xml`) - Safe URL resolver

### Root Cause

The current Maven build configuration uses `<packaging>jar</packaging>` for all modules, which produces standard JAR files without OSGi manifest metadata (Bundle-SymbolicName, Export-Package, Import-Package, etc.). OSGi containers require these metadata entries in the JAR's MANIFEST.MF to properly load and wire bundles.

### Current Behavior

- All swagger-parser modules are built as standard JAR files
- No OSGi manifest metadata is included in the JAR files
- Cannot be deployed to OSGi containers like Apache Karaf, Apache Felix, or Eclipse Equinox
- Users attempting to deploy swagger-parser in OSGi environments encounter classloading and dependency resolution issues

### Expected Behavior

- All swagger-parser modules should be built as OSGi bundles
- JAR files should contain proper OSGi manifest metadata including:
  - `Bundle-SymbolicName`: Unique identifier for the bundle
  - `Bundle-Version`: Version of the bundle
  - `Export-Package`: Packages exposed to other bundles (with versions)
  - `Import-Package`: Required external packages
  - Other standard OSGi headers
- Bundles should be deployable to OSGi containers
- Maintain backward compatibility with non-OSGi environments (OSGi bundles are valid JARs)

## Reproduction Steps

1. Attempt to install swagger-parser artifacts in Apache Karaf OSGi container
2. Create a Karaf feature definition including swagger-parser dependencies
3. Execute `feature:install` command in Karaf
4. Observe that the bundles cannot be properly wired due to missing OSGi metadata

## Proposed Solution

### Approach

Implement OSGi bundle packaging using the Apache Felix Maven Bundle Plugin, following the same pattern used by swagger-core. This approach:

1. Changes packaging type from `jar` to `bundle`
2. Adds maven-bundle-plugin configuration
3. Defines explicit Export-Package instructions for public APIs
4. Allows the plugin to auto-generate Import-Package declarations
5. Maintains backward compatibility (OSGi bundles work as regular JARs)

### Implementation Details

For each module that needs OSGi support, the following changes are required:

**1. Change packaging type:**
```xml
<packaging>bundle</packaging>
```

**2. Add maven-bundle-plugin configuration:**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.felix</groupId>
            <artifactId>maven-bundle-plugin</artifactId>
            <extensions>true</extensions>
            <configuration>
                <instructions>
                    <Export-Package>
                        {package.name};version="${project.version}",
                        {additional.packages};version="${project.version}"
                    </Export-Package>
                </instructions>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**3. Add dependency management in parent POM:**
```xml
<dependency>
    <groupId>org.apache.felix</groupId>
    <artifactId>maven-bundle-plugin</artifactId>
    <version>${felix-version}</version>
</dependency>
```

**Recommended Export-Package configurations per module:**

- **swagger-parser**: 
  - `io.swagger.parser`
  - `io.swagger.parser.util`

- **swagger-parser-v3**:
  - `io.swagger.v3.parser`
  - `io.swagger.v3.parser.core`
  - `io.swagger.v3.parser.util`

- **swagger-parser-v2-converter**:
  - `io.swagger.v3.parser.converter`

- **swagger-parser-core**:
  - `io.swagger.v3.parser.core.models`
  - `io.swagger.v3.parser.core.util`

- **swagger-parser-safe-url-resolver**:
  - `io.swagger.v3.parser.urlresolver`

### Code Locations

Files to modify:
1. `/pom.xml` - Add maven-bundle-plugin to dependencyManagement, add felix-version property
2. `/modules/swagger-parser/pom.xml` - Change packaging, add plugin configuration
3. `/modules/swagger-parser-v3/pom.xml` - Change packaging, add plugin configuration
4. `/modules/swagger-parser-v2-converter/pom.xml` - Change packaging, add plugin configuration
5. `/modules/swagger-parser-core/pom.xml` - Change packaging, add plugin configuration
6. `/modules/swagger-parser-safe-url-resolver/pom.xml` - Change packaging, add plugin configuration

### Testing Strategy

1. **Build Verification**: Execute `mvn clean install` to ensure all modules build successfully as bundles
2. **Manifest Inspection**: Verify generated MANIFEST.MF files contain proper OSGi headers:
   ```bash
   unzip -p target/*.jar META-INF/MANIFEST.MF
   ```
3. **OSGi Container Testing**: 
   - Deploy to Apache Karaf and verify bundle installation
   - Create Karaf feature definition
   - Test bundle resolution and activation
   - Verify exported packages are accessible to other bundles
4. **Backward Compatibility**: Ensure bundles work as regular JARs in non-OSGi environments
5. **Integration Tests**: Run existing test suites to ensure no regression
6. **Dependency Analysis**: Use `mvn dependency:tree` to verify transitive dependencies are properly handled

## Potential Risks & Considerations

### Breaking Changes
- **None expected**: OSGi bundles are fully backward compatible with regular JAR usage

### Compatibility Concerns
1. **Build Tool Compatibility**: 
   - Maven Bundle Plugin requires Maven 3.x
   - Current project already uses Maven 3.x (no issue)

2. **Dependency Management**:
   - Transitive dependencies must also be available as OSGi bundles or wrapped
   - swagger-core dependencies already provide OSGi bundles
   - Third-party dependencies (Jackson, SnakeYAML, etc.) mostly have OSGi support

3. **Private Package Access**:
   - Only explicitly exported packages will be accessible to other OSGi bundles
   - Internal implementation packages should not be exported
   - Need careful API boundary definition

### Edge Cases
1. **Version Conflicts**: OSGi enforces strict version semantics, which may expose existing version conflicts
2. **ClassLoader Isolation**: OSGi's class loading model may reveal hidden dependencies
3. **Service Loader**: Java ServiceLoader mechanism may need special OSGi configuration (SPI-Provider headers)
4. **Dynamic Loading**: Any runtime class loading or reflection may need additional Import-Package declarations

### Historical Context
- **PR #428**: Originally implemented OSGi bundle support in March 2017
- **Merged**: October 2018 to v1 branch
- **PR #889**: Reverted in October 2018, same day as merge
- **Reason for Revert**: Issue #890 indicated need for OSGi integration tests before merging
- **Current Status**: OSGi support never re-implemented, integration tests never added

## Related Issues

- **PR #428**: Original implementation "change packaging to (OSGi) bundle" (merged then reverted)
- **PR #889**: "Revert 'change packaging to (OSGi) bundle'" (merged)
- **Issue #890**: "OSGi integration test project based on #428" (still open)
- Related to swagger-core OSGi bundle implementation (reference model)

## Additional Context

### Community Interest
The issue has received continued interest over the years:
- **2017**: Initial request by borcsokj
- **2018**: Implementation merged but reverted due to lack of integration tests
- **2023**: alexwoodgate asked about re-adding the feature
- **2025**: zspitzer requested status update, questioning if integration tests are necessary blocker

### Referenced Projects
- **Apache Karaf**: Primary OSGi container use case
- **swagger-core**: Reference implementation that already provides OSGi bundles
- **Swagger Request Validator**: Dependent project needing OSGi support

### Implementation Precedent
The original PR #428 provides a working implementation that was production-ready. The revert was not due to technical issues but lack of automated testing. Key changes from PR #428:
- Used maven-bundle-plugin version 2.3.7 (current version is much newer: 5.x)
- Configured Export-Package for public APIs
- Changed packaging to "bundle"
- Added Felix plugin to dependencyManagement

### Testing Requirements (from Issue #890)
The maintainers indicated OSGi integration tests are needed before accepting this feature. However, as noted by community members:
- Most OSGi-enabled projects do not have OSGi-specific integration tests in their build
- OSGi metadata generation is deterministic and low-risk
- Manual testing in target environments (Karaf, Felix) may be sufficient
- Integration tests could be provided separately or as part of acceptance

## Complexity Estimate

- **Effort**: Medium
  - Configuration changes are straightforward (5 POMs to modify)
  - Previous implementation (PR #428) can be used as reference
  - Most complexity is in determining correct Export-Package declarations
  - Integration testing setup would add significant effort if required

- **Impact**: Medium
  - Enables new deployment scenarios (OSGi containers)
  - No breaking changes for existing users
  - Increases artifact compatibility and ecosystem integration
  - Aligns with swagger-core implementation

- **Priority**: Medium-High
  - Long-standing request (7+ years old)
  - Active community interest
  - Blocks OSGi adoption for entire Swagger parser stack
  - Low risk due to backward compatibility
  - Previous implementation already validated

## References

### Documentation
- [OSGi Alliance Specifications](https://docs.osgi.org/specification/)
- [Apache Felix Maven Bundle Plugin Documentation](https://felix.apache.org/documentation/subprojects/apache-felix-maven-bundle-plugin-bnd.html)
- [Apache Karaf Feature Definition](https://karaf.apache.org/manual/latest/#_feature_and_resolver)

### Related Projects
- [swagger-core OSGi implementation](https://github.com/swagger-api/swagger-core)
- [Apache Felix](https://felix.apache.org/)
- [Apache Karaf](https://karaf.apache.org/)

### Technical Resources
- [OSGi Bundle Manifest Headers](https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html#framework.module.bundlemanifest)
- [BND Tool (used by maven-bundle-plugin)](https://bnd.bndtools.org/)
- [Maven Bundle Plugin Instructions](https://felix.apache.org/documentation/subprojects/apache-felix-maven-bundle-plugin-bnd.html#instructions)

### Issue URLs
- [Issue #427](https://github.com/swagger-api/swagger-parser/issues/427)
- [PR #428](https://github.com/swagger-api/swagger-parser/pull/428)
- [PR #889](https://github.com/swagger-api/swagger-parser/pull/889)
- [Issue #890](https://github.com/swagger-api/swagger-parser/issues/890)
