# Issue Analysis Template

## Issue Overview
**Issue Number:** #1954
**Title:** CVE-2020-8908 on swagger-compat-spec-parser, json-schema-validator replacement?
**Status:** Open
**Created:** 2023-07-30T01:25:11Z
**Updated:** 2023-07-30T01:25:11Z
**URL:** https://github.com/swagger-api/swagger-parser/issues/1954

## Summary
This issue reports a security vulnerability (CVE-2020-8908) present in the swagger-parser project through a transitive dependency chain. The vulnerability exists in Google's Guava library version 28.2-android, which is pulled in by the json-schema-validator dependency (version 2.2.14) used by swagger-compat-spec-parser. The json-schema-validator library has not been updated since 2020 and appears to be unmaintained, making it impossible to receive security updates through normal dependency updates.

## Problem Statement
The swagger-parser project contains a security vulnerability through the following dependency chain:
- **swagger-parser-v2-converter** depends on **swagger-compat-spec-parser** (v1.0.75)
- **swagger-compat-spec-parser** depends on **json-schema-validator** (v2.2.14)
- **json-schema-validator** depends on **guava** (v28.2-android)

The Guava 28.2-android version contains CVE-2020-8908, a temporary directory creation vulnerability. While the swagger-parser project has updated to Guava 32.1.3-android in other modules (which resolves the CVE), the json-schema-validator library declares Guava 28.2-android as a compile-time dependency. Although Maven's dependency resolution currently selects the newer Guava version (32.1.3-android) due to conflict resolution, this creates an unstable situation that depends on dependency ordering and could break in future builds.

The root problem is that json-schema-validator has been abandoned:
- Last release: 2.2.14 (released in 2020)
- No maintenance or security updates since 2020
- GitHub repository shows no recent activity on the main branch

## Root Cause Analysis
### Primary Cause
The json-schema-validator library (com.github.java-json-tools:json-schema-validator:2.2.14) is no longer maintained. Its last release was in 2020, and it declares dependencies on outdated versions of libraries, including Guava 28.2-android.

### Dependency Chain Analysis
Based on Maven dependency tree analysis:
```
[INFO] +- io.swagger:swagger-compat-spec-parser:jar:1.0.75:compile
[INFO] |  +- com.github.java-json-tools:json-schema-validator:jar:2.2.14:compile
[INFO] |  |  +- (com.google.guava:guava:jar:28.2-android:compile - omitted for conflict with 32.1.3-android)
[INFO] |  |  +- com.github.java-json-tools:jackson-coreutils-equivalence:jar:1.0:compile
[INFO] |  |  |  +- (com.google.guava:guava:jar:28.2-android:runtime - omitted for conflict with 32.1.3-android)
[INFO] |  |  +- com.github.java-json-tools:json-schema-core:jar:1.2.14:compile
[INFO] |  |  |  +- (com.google.guava:guava:jar:28.2-android:compile - omitted for conflict with 32.1.3-android)
```

Currently, Maven resolves the conflict by choosing Guava 32.1.3-android (pulled by swagger-core-v3), but this is fragile and may not be reliable across all build configurations.

### CVE-2020-8908 Details
**CVE-2020-8908: Information Disclosure in Guava**
- **Severity:** Low
- **Affected Versions:** Guava < 32.0.0-android
- **Patched Version:** Guava >= 32.0.0-android
- **CVSS:** Low severity (information disclosure)
- **Description:** Temporary directory creation on Unix-like systems uses predictable names and world-readable permissions, potentially allowing local privilege escalation or information disclosure
- **GitHub Advisory:** GHSA-5mg8-w23w-74h3

While the vulnerability is rated as low severity, it represents a supply chain security risk that should be addressed.

## Affected Components
### Modules
- **modules/swagger-parser-v2-converter** - Primary affected module
- **swagger-compat-spec-parser** (external dependency v1.0.75) - Immediate dependency containing the vulnerable transitive dependency

### Dependencies
- **com.github.java-json-tools:json-schema-validator:2.2.14** - Unmaintained library with outdated dependencies
  - Declares: com.google.guava:guava:28.2-android (vulnerable)
  - Also declares: jackson-coreutils-equivalence:1.0, json-schema-core:1.2.14, joda-time:2.10.5, libphonenumber:8.11.1
- **com.github.java-json-tools:json-schema-core:1.2.14** - Supporting library
  - Also declares: guava:28.2-android (vulnerable)
  - Depends on: uri-template:0.10 (which declares guava:28.1-android)

### Files
The dependency is declared in:
- `/modules/swagger-parser-v2-converter/pom.xml` - Declares swagger-compat-spec-parser dependency

### Affected Functionality
- Swagger 2.0 to OpenAPI 3.x conversion functionality
- JSON Schema validation during spec parsing
- Legacy Swagger specification support

## Technical Details

### Current Behavior
1. swagger-parser-v2-converter includes swagger-compat-spec-parser for Swagger 2.0 compatibility
2. swagger-compat-spec-parser uses json-schema-validator for JSON Schema validation
3. json-schema-validator declares Guava 28.2-android as a dependency
4. Maven currently resolves to Guava 32.1.3-android due to a newer version in the dependency tree (from swagger-core-v3)
5. Security scanners flag the declared dependency on Guava 28.2-android as vulnerable, even though it may not be used at runtime

### Expected Behavior
1. All dependencies should be maintained and receive security updates
2. No vulnerable dependencies should be declared in the dependency tree
3. JSON Schema validation should be performed using a modern, actively maintained library
4. Dependency resolution should be explicit and deterministic, not reliant on conflict resolution

### Reproduction Steps
1. Clone the swagger-parser repository
2. Navigate to modules/swagger-parser-v2-converter
3. Run: `mvn dependency:tree -Dverbose`
4. Observe json-schema-validator:2.2.14 declaring guava:28.2-android
5. Run a security scan (e.g., OWASP Dependency-Check, Snyk, GitHub Dependabot)
6. Observe CVE-2020-8908 reported for the declared Guava 28.2-android dependency

## Impact Assessment
**Severity:** Medium

While CVE-2020-8908 itself is rated as Low severity, the overall impact is elevated due to:
- Unmaintained dependency creates long-term security risk
- Potential for additional undiscovered vulnerabilities in json-schema-validator
- Supply chain security concerns
- Compliance and audit failures
- Dependency resolution fragility

**Affected Users:**
- **Direct Impact:** All users of swagger-parser-v2-converter module
- **Indirect Impact:** Users performing Swagger 2.0 to OpenAPI 3.x conversions
- **Compliance Impact:** Organizations with strict security policies that fail builds on any CVE findings
- **Build Impact:** Users running security scans that flag declared dependencies (not just resolved dependencies)

**Workarounds:**
1. **Explicit Dependency Override (Temporary):**
   ```xml
   <dependency>
       <groupId>com.google.guava</groupId>
       <artifactId>guava</artifactId>
       <version>32.1.3-android</version>
   </dependency>
   ```
   This forces Guava to the patched version but doesn't solve the root cause.

2. **Dependency Exclusion (Risk):**
   Exclude json-schema-validator if Swagger 2.0 support is not needed:
   ```xml
   <dependency>
       <groupId>io.swagger.parser.v3</groupId>
       <artifactId>swagger-parser-v2-converter</artifactId>
       <exclusions>
           <exclusion>
               <groupId>io.swagger</groupId>
               <artifactId>swagger-compat-spec-parser</artifactId>
           </exclusion>
       </exclusions>
   </dependency>
   ```
   This breaks Swagger 2.0 conversion functionality.

3. **Security Scanner Configuration:**
   Suppress the specific CVE in scanner configurations (not recommended for production).

## Proposed Solution

### Option 1: Replace json-schema-validator (Recommended)
Replace the unmaintained json-schema-validator with a modern, actively maintained alternative.

**Candidate Libraries:**
1. **Networknt JSON Schema Validator** (https://github.com/networknt/json-schema-validator)
   - Actively maintained (2024 releases)
   - Supports JSON Schema Draft 4, 6, 7, 2019-09, and 2020-12
   - Good performance
   - Apache 2.0 license
   - ~200k downloads/month on Maven Central
   - Latest version: 1.5.x

2. **Everit JSON Schema** (https://github.com/everit-org/json-schema)
   - Actively maintained
   - Supports JSON Schema Draft 4, 6, and 7
   - Apache 2.0 license
   - Used by several major projects

3. **Snow JSON Schema Validator** (https://github.com/ssilverman/snowy-json)
   - Modern implementation
   - Supports latest JSON Schema drafts
   - Active development

**Pros:**
- Eliminates unmaintained dependency
- Receives ongoing security updates
- Modern features and better performance
- No dependency on outdated Guava versions
- Solves root cause

**Cons:**
- Requires code changes in swagger-compat-spec-parser (or its replacement)
- API differences may require migration effort
- Testing required to ensure compatibility
- May require swagger-compat-spec-parser to be forked/replaced

### Option 2: Fork and Update json-schema-validator
Fork the json-schema-validator library and update its dependencies.

**Pros:**
- Maintains API compatibility
- Minimal changes to swagger-parser codebase
- Can be released under compatible license

**Cons:**
- Takes on maintenance burden for another library
- json-schema-validator has its own technical debt
- Still uses older JSON Schema draft versions
- Not a sustainable long-term solution

### Option 3: Remove Swagger 2.0 Conversion Support
Deprecate and eventually remove the swagger-parser-v2-converter module.

**Pros:**
- Simplifies codebase
- Removes unmaintained dependency
- Focuses on OpenAPI 3.x

**Cons:**
- Breaking change for users still using Swagger 2.0
- May not be acceptable to user base
- Swagger 2.0 specs are still common in legacy systems

### Option 4: Update swagger-compat-spec-parser Dependency
Work with or fork swagger-compat-spec-parser (io.swagger:swagger-compat-spec-parser:1.0.75) to update its dependencies.

**Pros:**
- Addresses issue at the right level
- Can benefit broader Swagger ecosystem
- May be accepted upstream

**Cons:**
- swagger-compat-spec-parser may also be unmaintained
- Requires coordination with external project
- Timeline uncertain

## Implementation Approach

### Recommended: Option 1 + Option 4 (Hybrid Approach)

**Phase 1: Immediate Mitigation (Short-term)**
1. Add explicit Guava dependency override in swagger-parser-v2-converter/pom.xml:
   ```xml
   <dependency>
       <groupId>com.google.guava</groupId>
       <artifactId>guava</artifactId>
       <version>32.1.3-android</version>
   </dependency>
   ```
2. Document the CVE and workaround in release notes
3. Add comment explaining the override is temporary

**Phase 2: Fork swagger-compat-spec-parser (Medium-term)**
1. Fork io.swagger:swagger-compat-spec-parser or create swagger-parser-v2-converter-compat module
2. Replace json-schema-validator with Networknt JSON Schema Validator
3. Update code to use new validator API
4. Comprehensive testing with Swagger 2.0 specs
5. Release as part of swagger-parser with proper versioning

**Phase 3: Upstream Contribution (Long-term)**
1. Offer changes back to swagger-compat-spec-parser project if maintained
2. If unmaintained, document the fork and maintain within swagger-parser
3. Consider deprecation path for Swagger 2.0 support in future major version

### Implementation Steps for Phase 2
1. Create new module or fork swagger-compat-spec-parser
2. Update pom.xml to replace dependencies:
   ```xml
   <!-- Remove -->
   <dependency>
       <groupId>com.github.java-json-tools</groupId>
       <artifactId>json-schema-validator</artifactId>
       <version>2.2.14</version>
   </dependency>
   
   <!-- Add -->
   <dependency>
       <groupId>com.networknt</groupId>
       <artifactId>json-schema-validator</artifactId>
       <version>1.5.1</version>
   </dependency>
   ```
3. Update validation code to use new API
4. Run existing test suite
5. Add tests for edge cases
6. Update documentation

## Alternatives Considered

### Alternative A: Wait for json-schema-validator Update
Wait for the upstream json-schema-validator project to release an update.

**Rejected because:**
- Project appears abandoned (no activity since 2020)
- No indication of future releases
- Cannot rely on unmaintained project for security updates

### Alternative B: Use Dependency Management to Force Guava Version
Use Maven dependency management to force newer Guava version globally.

**Rejected because:**
- Only treats symptom, not root cause
- json-schema-validator may have other security issues
- Does not address the unmaintained dependency problem
- Future vulnerabilities will continue to emerge

### Alternative C: Implement Custom JSON Schema Validation
Build custom JSON Schema validation specific to Swagger needs.

**Rejected because:**
- Significant development effort
- Reinventing the wheel
- Maintenance burden
- Risk of introducing bugs
- Many mature alternatives exist

## Dependencies
### Related Issues
- Check for other issues mentioning CVE-2020-8908
- Check for issues mentioning json-schema-validator
- Search for Swagger 2.0 conversion related issues

### External Dependencies
- **swagger-compat-spec-parser** (io.swagger:swagger-compat-spec-parser:1.0.75)
  - Repository: https://github.com/swagger-api/swagger-parser/tree/1.x
  - May also be unmaintained (part of 1.x branch)
- **json-schema-validator** (com.github.java-json-tools:json-schema-validator:2.2.14)
  - Repository: https://github.com/java-json-tools/json-schema-validator
  - Status: Unmaintained since 2020
- **Replacement library** (e.g., Networknt JSON Schema Validator)
  - Repository: https://github.com/networknt/json-schema-validator
  - Status: Active (regular releases in 2024)

### Breaking Changes
- API changes if json-schema-validator is replaced
- Potential behavior differences in validation
- May affect consumers of swagger-parser-v2-converter

## Testing Considerations

### Unit Tests
- Test JSON Schema validation with various Swagger 2.0 specifications
- Test all JSON Schema draft 4 features used by Swagger specs
- Test error handling and validation messages
- Test with malformed schemas
- Verify no regression in existing functionality

### Integration Tests
- Full Swagger 2.0 to OpenAPI 3.x conversion workflows
- Real-world Swagger 2.0 specifications from popular APIs
- Edge cases: complex schemas, references, nested objects
- Performance testing with large specifications

### Security Tests
- Verify CVE-2020-8908 is resolved
- Run OWASP Dependency-Check
- Run Snyk/GitHub Security scanning
- Verify no new vulnerabilities introduced
- Test dependency tree for conflicting versions

### Edge Cases
- Circular schema references
- Large schemas (performance)
- Special characters in schema definitions
- All JSON Schema validation keywords
- External schema references
- Schema composition (allOf, oneOf, anyOf)

### Regression Testing
- Run full swagger-parser test suite
- Test with major open-source projects using swagger-parser
- Compatibility with existing user code

## Documentation Updates

### Required Documentation Changes
1. **CHANGELOG.md**
   - Document the security fix
   - Note the dependency change
   - Highlight any API changes

2. **README.md**
   - Update dependency information
   - Note Swagger 2.0 support status
   - Security notice if applicable

3. **Migration Guide** (if API changes)
   - Document any behavioral changes
   - Provide migration examples
   - Note deprecated features

4. **Security Advisory**
   - Document CVE-2020-8908
   - Explain impact and mitigation
   - Provide upgrade path

5. **API Documentation**
   - Update Javadoc if methods change
   - Document new dependencies
   - Note version requirements

6. **Release Notes**
   - Highlight security fix
   - Note dependency updates
   - Breaking changes (if any)

## Additional Notes

### Security Considerations
- While CVE-2020-8908 is low severity, the presence of unmaintained dependencies represents a broader security risk
- Future vulnerabilities in json-schema-validator cannot be patched
- Supply chain security best practices recommend replacing unmaintained dependencies
- Automated security scanners will continue to flag this issue

### Compatibility Notes
- Swagger 2.0 is legacy but still widely used
- Breaking backward compatibility may affect significant user base
- Consider providing parallel support during transition period
- Deprecation warnings should precede any removal

### Performance Implications
- Modern JSON Schema validators may have different performance characteristics
- Benchmark testing recommended
- Large schema validation should be profiled
- Consider caching strategies if performance changes

### Community Engagement
- Issue has 1 positive reaction, indicating user concern
- No comments on the issue yet
- Consider soliciting feedback on proposed solution
- Survey users about Swagger 2.0 usage patterns

### Timeline Estimate
- **Phase 1 (Immediate):** 1-2 days - Add Guava override
- **Phase 2 (Medium-term):** 2-4 weeks - Replace json-schema-validator
  - 1 week: Research and select replacement library
  - 1 week: Implementation and initial testing
  - 1 week: Comprehensive testing and bug fixes
  - 1 week: Code review and documentation
- **Phase 3 (Long-term):** Ongoing - Maintenance and upstream contribution

### Risk Assessment
**High Risk:**
- Keeping unmaintained dependency (enables future vulnerabilities)
- Breaking Swagger 2.0 conversion without user notice

**Medium Risk:**
- Replacing validator may introduce subtle behavioral changes
- Performance characteristics may differ

**Low Risk:**
- Migration to well-maintained, widely-used replacement library
- Comprehensive testing mitigates compatibility issues

### Success Criteria
1. CVE-2020-8908 no longer reported by security scanners
2. All transitive dependencies are maintained and up-to-date
3. No regression in Swagger 2.0 conversion functionality
4. Test suite passes completely
5. Performance within acceptable range (< 10% degradation)
6. Positive feedback from early adopters
7. Clean security scan results
