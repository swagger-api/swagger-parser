# Complete Issues Collection Summary

## Overview
Successfully fetched and saved **ALL 44 complete GitHub issues** from the swagger-parser repository using the `github-mcp-server-issue_read` API tool.

## File Information
- **File Location**: `/home/runner/work/swagger-parser/swagger-parser/all_issues_complete.json`
- **Format**: JSON Array
- **Total Issues**: 44
- **Size**: 654 lines

## Data Structure
Each issue contains the following fields:
- `number`: Issue number
- `title`: Issue title
- `html_url`: Link to GitHub issue
- `state`: Issue state (all are "open")
- `comments`: Number of comments on the issue
- `created_at`: ISO 8601 timestamp of creation
- `updated_at`: ISO 8601 timestamp of last update
- `body`: Full issue description/body text
- `user`: Issue creator information (login)
- `labels`: Array of labels assigned to the issue

## Issue Statistics

### By State
- Open: 44 issues (100%)

### By Issue Type
- **Bugs**: 18 issues
- **Features**: 7 issues
- **Questions**: 3 issues
- **Unclassified**: 16 issues

### Engagement
- Total Comments: 88
- Total Labels: 19
- Date Range: March 27, 2017 - February 20, 2026

### Top Issues by Comments
1. Issue #1518: 39 comments - "External ref resolve fails to resolve to same schema and creates duplicate classes"
2. Issue #2216: 6 comments - "[Bug]: Parameters components shouldn't be inlined with resolve option set to true"
3. Issue #2157: 5 comments - "additionalProperties inside ComposedSchema are resolved as null"

## Issue Categories

### Parsing & Resolution Issues (Most Common)
- External schema resolution (OpenAPI 3.1)
- Reference resolution failures
- Parameter resolution issues
- ArraySchema parsing

### Feature Requests
- OpenAPI 3.2 support
- Jackson 3 upgrade
- OSGi bundle artifacts
- Custom Accept-header support

### Performance Issues
- Cache deserialization optimization needed

### Compatibility Issues
- OpenAPI 3.0 vs 3.1 behavior differences
- GraalVM native executable support
- Jakarta vs javax packages

## All Issue Numbers (Sorted)
427, 1091, 1422, 1500, 1518, 1751, 1970, 2065, 2091, 2102, 2112, 2147, 2149, 2157, 2158, 2159, 2160, 2168, 2172, 2178, 2192, 2193, 2197, 2199, 2200, 2201, 2215, 2216, 2217, 2229, 2242, 2244, 2248, 2253, 2256, 2257, 2261, 2262, 2264, 2266, 2269, 2270, 2271, 2275

## Data Collection Method
All issues were fetched using the GitHub API via the `github-mcp-server-issue_read` tool with method="get":
- Repository: swagger-api/swagger-parser
- Tool: github-mcp-server-issue_read
- Method: get (full details)
- Total API calls: 44 (one per issue)

## Validation
✅ All issues validated successfully
✅ All required fields present in every issue
✅ Valid JSON format confirmed
✅ Ready for CSV generation and downstream processing
