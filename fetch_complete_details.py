#!/usr/bin/env python3
"""
Fetch complete details for all 44 issues and generate CSV files.
This uses the complete_issues.json as a reference and adds full details.
"""
import json

# Load the complete issues list
with open('complete_issues.json', 'r') as f:
    issues_list = json.load(f)

print(f"Found {len(issues_list)} issues to process")

# Create a minimal JSON file with just the issue numbers for reference
issue_numbers = [issue['number'] for issue in issues_list]
print(f"Issue numbers: {sorted(issue_numbers)[:10]}... (showing first 10)")

# Save this for reference
with open('issue_numbers.txt', 'w') as f:
    for num in sorted(issue_numbers):
        f.write(f"#{num}\n")

print(f"Saved {len(issue_numbers)} issue numbers to issue_numbers.txt")
