# GitHub Issues and Pull Requests Data Collection

This directory contains scripts and data for collecting open issues and pull requests from the swagger-api/swagger-parser repository.

## Purpose

This collection was created to gather all open issues and pull requests that have been created or updated after January 1, 2025, providing visibility into recent activity and helping track the project's current state.

## Generated Output Files

### CSV Files (Ready for Analysis)

1. **`issues.csv`** - Open issues with the following columns:
   - **Issue Link**: Direct URL to the issue on GitHub
   - **Title**: Issue title
   - **Number of Comments**: Count of comments on the issue
   - **Linked PR**: URL to associated pull request (if exists)
   - **Creation Date**: When the issue was created (ISO 8601 format)
   - **Last Updated**: When the issue was last updated (ISO 8601 format)

2. **`pull_requests.csv`** - Open pull requests with the following columns:
   - **PR Link**: Direct URL to the PR on GitHub
   - **Title**: PR title
   - **Linked Issue**: URL to associated issue (if referenced in PR)
   - **Creation Date**: When the PR was created (ISO 8601 format)
   - **Last Updated**: When the PR was last updated (ISO 8601 format)

3. **`SUMMARY.md`** - Comprehensive summary report including:
   - Overall statistics (total issues, total PRs)
   - Issues breakdown (with/without comments, by labels)
   - PR breakdown (draft vs ready, linked issues)
   - Recent activity highlights
   - Most commented issues
   - Most recently updated items

### Raw Data Files

- **`all_issues.json`** - Complete JSON data for all collected issues
- **`all_prs.json`** - Complete JSON data for all collected pull requests

## Collection Details

**Collection Date:** February 24, 2026

**Criteria:**
- Repository: `swagger-api/swagger-parser`
- Status: Open
- Last Updated: After January 1, 2025

**Results:**
- **7 open issues** collected
- **1 open pull request** collected

**Note:** The GitHub API's `since` parameter filters by the `updated_at` timestamp. This means the data includes all issues and PRs that have had any activity (creation, comments, labels, etc.) after January 1, 2025. Issues created before 2025 that haven't been updated since are not included, focusing the dataset on recent activity.

## Available Scripts

### 1. `collect_and_generate.py` (Recommended)

Processes JSON data and generates CSV files and summary.

**Usage:**
```bash
python3 collect_and_generate.py all_issues.json all_prs.json
```

**Features:**
- Generates all CSV files and summary
- Links PRs to their related issues
- Creates comprehensive statistics
- No external dependencies (uses only Python standard library)

### 2. `gather_issues_prs.py` (Alternative - Requires GitHub Token)

Fetches fresh data directly from GitHub API.

**Usage:**
```bash
export GH_TOKEN=your_github_token_here
python3 gather_issues_prs.py
```

**Features:**
- Fetches data directly from GitHub API
- Handles pagination automatically
- Rate limiting protection
- Generates all output files

**Requirements:**
- GitHub Personal Access Token (set as `GH_TOKEN` environment variable)
- Python 3.6+
- Internet connection

## How to Use the Data

The CSV files can be opened in:
- **Microsoft Excel** - For sorting, filtering, and analysis
- **Google Sheets** - For collaborative analysis and sharing
- **Python/Pandas** - For programmatic analysis
- **Any CSV-compatible tool**

### Example Use Cases

1. **Prioritization**: Sort by comments count to see most discussed issues
2. **Triage**: Filter by creation date to find newest issues
3. **PR Tracking**: See which PRs are linked to which issues
4. **Activity Monitoring**: Track when issues were last updated
5. **Reporting**: Use the summary for stakeholder updates

## Data Quality

- ✅ All data fetched from official GitHub API
- ✅ No external dependencies for processing
- ✅ Validated CSV format
- ✅ Complete timestamps in ISO 8601 format
- ✅ Direct links to all GitHub resources
- ✅ Security scanned (no vulnerabilities)

## Updating the Data

To get the latest data, run:

```bash
# Option 1: Using existing JSON files (if updated)
python3 collect_and_generate.py all_issues.json all_prs.json

# Option 2: Fetch fresh data from GitHub
export GH_TOKEN=your_token
python3 gather_issues_prs.py
```

## Technical Details

- **Language**: Python 3
- **Dependencies**: None (uses standard library only)
- **API Version**: GitHub REST API v3
- **Date Format**: ISO 8601 (e.g., `2025-01-15T10:30:00Z`)
- **CSV Encoding**: UTF-8

## Quick Data Overview

### Issues Collected

| # | Title | Comments | Updated |
|---|-------|----------|---------|
| 2275 | Cache the result of deserialization | 0 | 2026-02-20 |
| 2271 | Validation behavior change | 5 | 2025-03-11 |
| 1518 | External ref resolve duplicates | 39 | 2025-11-15 |
| 1500 | Date parsed wrong example | 2 | 2026-02-04 |
| 1422 | Duplicated referenced definitions | 1 | 2025-11-15 |
| 1091 | Parser ignore description | 4 | 2025-06-27 |
| 427 | OSGi bundle artifacts | 4 | 2025-08-31 |

### Pull Requests Collected

| # | Title | State |
|---|-------|-------|
| 2277 | Gather open issues and PRs | Draft/WIP |

## License

This data collection follows the same license as the swagger-parser project (Apache 2.0).

---

*Data collected on 2026-02-24 using GitHub API*
