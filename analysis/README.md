# Issue Analysis Directory

This directory contains batch analysis of GitHub issues from the swagger-api/swagger-parser repository.

## Directory Structure

```
analysis/
├── README.md          # This file
└── batch4/            # Fourth batch of issue analyses
    ├── README.md      # Batch 4 summary
    └── parser-batch4-issue-*.md  # Individual analysis files
```

## Batch Processing

Issues are processed in batches of 12 from the `data/swagger-parser/issues.csv` file.

### Batch Organization

- **Batch 1**: Issues at CSV rows 1-12
- **Batch 2**: Issues at CSV rows 13-24
- **Batch 3**: Issues at CSV rows 25-36
- **Batch 4**: Issues at CSV rows 37-48 (current batch)

## Analysis Template

All analyses use the standardized prompt template from:
```
.github/analyze-issue.md
```

This template provides a structured framework for comprehensive issue assessment covering:
- Issue classification
- Technical summary
- Root cause analysis
- Impact assessment
- Proposed solutions
- Implementation considerations
- Additional notes

## File Naming Convention

Analysis files follow the pattern:
```
parser-batch{N}-issue-{number}.md
```

Where:
- `{N}` is the batch number (e.g., 4)
- `{number}` is the GitHub issue number (e.g., 1147)

Example: `parser-batch4-issue-1147.md`

## Source Data

All issue data is sourced from `data/swagger-parser/issues.csv`, which contains:
- 120 open issues from swagger-api/swagger-parser
- Issue metadata (number, title, state, dates, labels, URL)
- Full issue body content

## Purpose

These analysis files serve as:
1. Documentation of issue investigation
2. Planning artifacts for issue resolution
3. Knowledge base for the project
4. Training material for new contributors

## Contributing

To add analysis for a new batch:
1. Create a new batch directory (e.g., `batch5/`)
2. Generate analysis files using the template
3. Add a batch-specific README.md
4. Update this main README with the new batch info
