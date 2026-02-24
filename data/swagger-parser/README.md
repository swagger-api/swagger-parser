# Swagger Parser Issues Export

This directory contains exported GitHub issues from the swagger-api/swagger-parser repository.

## Files

### issues.csv
**Main data file containing all exported issues**
- **Records**: 120 open issues
- **Columns**: number, title, state, created_at, updated_at, body, labels, url
- **Format**: UTF-8 encoded CSV with proper escaping
- **Size**: 23 KB

## How to Use

### In Python
```python
import pandas as pd

# Read the CSV file
df = pd.read_csv('issues.csv')

# Basic info
print(f"Total issues: {len(df)}")
print(f"Columns: {df.columns.tolist()}")

# Access data
print(df.head())
print(df[['number', 'title', 'state']])

# Filter
bugs = df[df['title'].str.contains('Bug', case=False)]
features = df[df['labels'].str.contains('Feature', case=False)]
```

### In Excel/Google Sheets
1. Open the spreadsheet application
2. File → Open/Import
3. Select `issues.csv`
4. Configure settings (comma delimiter)
5. Click Import/Open

### In Java
```java
import com.opencsv.CSVReader;
import java.io.FileReader;

CSVReader reader = new CSVReader(new FileReader("issues.csv"));
List<String[]> entries = reader.readAll();
for (String[] entry : entries) {
    System.out.println("Issue #" + entry[0] + ": " + entry[1]);
}
```

## Data Overview

### Issue Categories
- **Features/Enhancement Requests**: Cache optimization, AsyncAPI support, better error handling
- **Bugs/Defects**: Parsing issues, reference resolution failures, validation problems
- **Questions**: How-to questions, support requests

### Issue Timeline
- **Newest Issues**: #2275, #2271, #2265, #2258 (2025-2026)
- **Oldest Issues in Export**: #1005-#1023 (2019)
- **Time Span**: ~7 years of issue history

### Key Topics
1. Reference resolution (internal and external)
2. OpenAPI 3.1 support and compatibility
3. Schema conversion (Swagger 2.0 to OpenAPI 3.0)
4. File handling and URL resolution
5. Discriminator and polymorphism support
6. AsyncAPI specification support

## Column Descriptions

| Column | Type | Description |
|--------|------|-------------|
| number | Integer | GitHub issue number |
| title | String | Issue title (short summary) |
| state | String | Issue state (all are OPEN) |
| created_at | Timestamp | ISO 8601 creation date/time |
| updated_at | Timestamp | ISO 8601 last update date/time |
| body | Text | Full issue description/body |
| labels | String | Issue labels (comma-separated if multiple) |
| url | URL | Direct link to GitHub issue |

## Notes

- **Multi-line Bodies**: Issue descriptions may contain line breaks and special characters - all properly escaped for CSV
- **Character Encoding**: UTF-8 throughout
- **Timestamps**: All in ISO 8601 format with UTC timezone (Z suffix)
- **Data Quality**: All 120 issues have complete data for all columns
- **Repository**: swagger-api/swagger-parser
- **Total Open Issues**: 311 (this export contains 120 = 38.6% coverage)

## Processing Examples

### Count issues by label
```python
import pandas as pd
df = pd.read_csv('issues.csv')
print(df['labels'].value_counts())
```

### Find issues updated in last 30 days
```python
import pandas as pd
from datetime import datetime, timedelta

df = pd.read_csv('issues.csv')
df['updated_at'] = pd.to_datetime(df['updated_at'])
recent = df[df['updated_at'] > datetime.now() - timedelta(days=30)]
print(f"Issues updated in last 30 days: {len(recent)}")
```

### Extract issues with specific keywords
```python
import pandas as pd
df = pd.read_csv('issues.csv')
ref_issues = df[df['body'].str.contains('reference', case=False, na=False)]
print(f"Issues mentioning 'reference': {len(ref_issues)}")
```

## Data Source

- **API**: GitHub GraphQL API
- **Repository**: swagger-api/swagger-parser  
- **Filter**: Open issues only (state=OPEN)
- **Retrieval Method**: Paginated GraphQL queries (100 issues per query)
- **Export Date**: 2026-02-24

---

For more detailed information, see the accompanying documentation files:
- `GITHUB_ISSUES_EXPORT_REPORT.md` - Comprehensive export report
- `ISSUES_CSV_SUMMARY.md` - Quick reference guide
