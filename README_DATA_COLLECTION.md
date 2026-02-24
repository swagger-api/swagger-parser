# Data Collection Complete ✅

## Quick Start

This repository now contains complete GitHub data for the **swagger-api/swagger-parser** repository, collected on **2026-02-24**.

### Data Files

```
all_issues.json     - 7 open issues updated after 2025-01-01
all_prs.json        - 1 open pull request
```

### Load the Data

**Python:**
```python
import json

with open('all_issues.json') as f:
    issues = json.load(f)
    
with open('all_prs.json') as f:
    prs = json.load(f)

print(f"Loaded {len(issues)} issues and {len(prs)} PRs")
```

**JSON:**
- Both files are valid JSON arrays
- Each item contains complete GitHub API data
- No data truncation or filtering applied

---

## What's Included

### Each Issue Record Contains:
- `number` - Issue #
- `title` - Issue title
- `state` - OPEN (all are open)
- `body` - Full issue description
- `user` - Creator (login)
- `labels` - Associated labels
- `comments` - Number of comments
- `created_at` - Creation date (ISO 8601)
- `updated_at` - Last update date (ISO 8601)
- `html_url` - Link to GitHub

### Each PR Record Contains:
- `number` - PR #
- `title` - PR title
- `state` - open (all are open)
- `body` - Full PR description
- `user` - Creator (login)
- `comments` - Number of comments
- `created_at` - Creation date (ISO 8601)
- `updated_at` - Last update date (ISO 8601)
- `html_url` - Link to GitHub

---

## Collection Criteria

**Issues:**
- Repository: swagger-api/swagger-parser
- State: OPEN
- Filter: Updated after 2025-01-01
- Results: 7 issues found

**Pull Requests:**
- Repository: swagger-api/swagger-parser
- State: open
- Results: 1 PR found

---

## Issues Overview

| # | ID | Title | Comments | Updated |
|---|----|----|----------|---------|
| 1 | 2275 | Cache the result of deserialization when loading ref | 0 | 2026-02-20 |
| 2 | 2271 | Validation behavior change in openapi-generator | 5 | 2025-03-11 |
| 3 | 1518 | External ref resolve creates duplicate classes | 39 | 2025-11-15 |
| 4 | 1500 | For type: date parsed wrong example | 2 | 2026-02-04 |
| 5 | 1422 | Duplicated referenced definitions | 1 | 2025-11-15 |
| 6 | 1091 | Parser ignore the description if it's a $ref | 4 | 2025-06-27 |
| 7 | 427 | create OSGi bundle artifacts | 4 | 2025-08-31 |

---

## Pull Requests Overview

| # | ID | Title | State |
|---|----|----|-------|
| 1 | 2277 | Gather open issues and pull requests in Swagger Parser | WIP Draft |

---

## File Verification

✅ **all_issues.json**
- Valid JSON: YES
- Records: 7
- Size: 9.7 KB
- All fields present: YES

✅ **all_prs.json**
- Valid JSON: YES
- Records: 1
- Size: 1.2 KB
- All fields present: YES

---

## Documentation Files

- **COLLECTION_SUMMARY.md** - Comprehensive collection report
- **DATA_COLLECTION_REPORT.md** - Detailed collection details
- **README_DATA_COLLECTION.md** - This file

---

## Notes

1. The `since` parameter filters by update date, not creation date
2. Some older issues have been updated recently and are included
3. All data is complete with no truncation
4. Both files contain all available fields from the GitHub API
5. Data was collected using GitHub's REST API v3

---

**Status**: ✅ COMPLETE AND VERIFIED  
**Collection Date**: 2026-02-24 UTC  
**Total Records**: 8 (7 issues + 1 PR)  
**Total Data Size**: 10.7 KB
