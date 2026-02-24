# Issue Analysis: #2275

## Overview
Analysis of GitHub issue for the swagger-parser project.

## 1. Issue Summary
- **Issue Number**: 2275
- **Title**: [Feature]: Cache the result of deserialization when loading ref
- **Type**: Feature Request / Performance Enhancement
- **Status**: Open
- **URL**: https://github.com/swagger-api/swagger-parser/issues/2275
- **Created**: 2026-02-20
- **Author**: xmourgues

## 2. Problem Description
The ResolverCache currently only caches the raw string content when loading external $ref references, but still deserializes the entire tree on each reference. For projects with many references to the same file (especially large files), this causes significant performance degradation. The user has a project with 260+ OpenAPI spec files and a large "dictionary" YAML file with 1100+ fields that is referenced 3300+ times across specs. Each reference triggers deserialization of the entire file.

## 3. Technical Analysis

### Affected Components
- **Module**: `swagger-parser-v3`
- **Primary Class**: `io.swagger.v3.parser.ResolverCache`
- **Specific Method/Line**: Line 170 in ResolverCache.java where deserialization occurs repeatedly
- **Process**: External reference resolution and caching mechanism

### Root Cause
The current caching strategy only stores the raw file content (String) in the cache, but the deserialization from String to JsonNode happens on every access. This means:
1. File read is cached ✓ (efficient)
2. Deserialization to JsonNode is NOT cached ✗ (inefficient)

For large files referenced many times, the deserialization overhead becomes the bottleneck.

### Impact Assessment
- **Severity**: Medium-High (significant performance impact for large projects)
- **User Impact**: Users with large OpenAPI projects with many cross-file references experience slow build times
- **Performance Impact**: User reports build time reduction from 4m49s to 58s (>80% improvement) with prototype fix
- **Workaround Available**: No - users must accept slow build times or modify source code

## 4. Reproduction
- **Reproducible**: Yes
- **Prerequisites**: 
  - Large OpenAPI project with multiple spec files
  - Large shared schema/dictionary file
  - Many $ref references to the same file
- **Test Case Available**: User has working prototype demonstrating improvement
- **Complexity**: Requires test setup with multiple large files and many references

## 5. Related Issues and Context

### Dependencies
- This is a performance optimization, not a bug fix
- May relate to other caching/resolution performance issues
- No blocking dependencies identified

### Version Information
- **Affected versions**: All versions (appears to be original design)
- **Reported on**: Not version-specific - architectural issue

## 6. Solution Approach

### Proposed Solution
The user suggests caching the deserialized JsonNode tree in addition to (or instead of) the raw string:

**Current flow:**
```
$ref encountered → Check cache → Get String from cache → Deserialize to JsonNode → Use
```

**Proposed flow:**
```
$ref encountered → Check cache → Get JsonNode from cache → Use
```

**Implementation approach:**
1. Modify ResolverCache to store JsonNode objects instead of/alongside Strings
2. Ensure thread safety if parser is used concurrently
3. Consider memory implications - JsonNode trees use more memory than Strings
4. Add configuration option to enable/disable JsonNode caching (backward compatibility)

### Implementation Complexity
- **Effort Estimate**: Low-Medium
  - Code change is relatively localized to ResolverCache
  - Need to ensure thread safety
  - Need to consider memory management
  - Need to handle cache invalidation correctly

- **Risks**: 
  - **Memory consumption**: JsonNode trees consume more memory than raw strings
  - **Thread safety**: Need to ensure cache is thread-safe for concurrent parsing
  - **Cache invalidation**: Need proper cache clearing mechanisms
  - **Backward compatibility**: Existing users might see different memory usage patterns

### Testing Requirements
- **Unit tests needed**: 
  - Cache hit/miss scenarios
  - Multiple references to same file
  - Large file deserialization
  - Thread safety tests
  
- **Performance tests needed**:
  - Benchmark with many references to same file
  - Memory usage profiling
  - Concurrent parsing scenarios
  
- **Integration tests needed**:
  - End-to-end parsing with complex reference chains
  - Large project simulation (multiple files, many refs)

- **Backward compatibility**: 
  - Ensure existing functionality is not broken
  - Consider configuration flag to opt-in/opt-out

## 7. Additional Notes

### Recommendations
1. **Accept the feature request** - The performance improvement is significant and well-documented
2. **Consider a configurable approach** - Allow users to choose between memory efficiency (string caching) and speed (JsonNode caching)
3. **Measure memory impact** - Benchmark memory usage increase to document trade-offs
4. **User willing to contribute** - The issue author offered to contribute a PR, which should be encouraged

### Questions to Address
1. Should JsonNode caching be opt-in or opt-out?
2. What should be the default cache size limits?
3. Should there be memory-based eviction policies?
4. Are there other cache optimization opportunities in the same area?

### Priority Assessment
- **Priority**: Medium-High
- **Justification**: Significant performance improvement for a common use case, with minimal risk if implemented correctly
- **Effort vs Benefit**: High benefit relative to implementation effort

### Community Engagement
- User has already implemented and tested a prototype
- User is willing to contribute PR
- Clear use case with measurable improvements
- Should engage with user to review their implementation approach
