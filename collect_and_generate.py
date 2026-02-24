#!/usr/bin/env python3
"""
Script to collect GitHub data using MCP tools and generate CSV files.
This script will be called with JSON data piped from MCP tool outputs.
"""

import json
import csv
import re
import sys
from datetime import datetime

def get_linked_issue_from_pr(pr):
    """Extract linked issue number from PR body or title."""
    body = pr.get('body', '') or ''
    title = pr.get('title', '') or ''
    
    # Common patterns for issue references
    patterns = [
        r'(?:fix(?:es|ed)?|close(?:s|d)?|resolve(?:s|d)?)\s*[:#]\s*(\d+)',
        r'Issue\s+#(\d+)',
        r'issue\s+(\d+)',
        r'#(\d+)',
    ]
    
    for pattern in patterns:
        match = re.search(pattern, body, re.IGNORECASE)
        if match:
            return match.group(1)
        match = re.search(pattern, title, re.IGNORECASE)
        if match:
            return match.group(1)
    
    return None

def get_linked_pr_from_issue(issue_number, prs):
    """Find if there's a PR linked to this issue."""
    for pr in prs:
        linked_issue = get_linked_issue_from_pr(pr)
        if linked_issue and int(linked_issue) == issue_number:
            return pr.get('html_url', '')
    return ''

def generate_reports(issues, prs):
    """Generate CSV files and summary."""
    
    print(f"Generating reports for {len(issues)} issues and {len(prs)} PRs...")
    
    # Generate issues.csv
    with open('issues.csv', 'w', newline='', encoding='utf-8') as csvfile:
        fieldnames = ['Issue Link', 'Title', 'Number of Comments', 'Linked PR', 'Creation Date', 'Last Updated']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        
        writer.writeheader()
        for issue in issues:
            linked_pr = get_linked_pr_from_issue(issue.get('number'), prs)
            writer.writerow({
                'Issue Link': issue.get('html_url', ''),
                'Title': issue.get('title', ''),
                'Number of Comments': issue.get('comments', 0),
                'Linked PR': linked_pr,
                'Creation Date': issue.get('created_at', ''),
                'Last Updated': issue.get('updated_at', '')
            })
    
    print(f"✓ Generated issues.csv with {len(issues)} issues")
    
    # Generate pull_requests.csv
    with open('pull_requests.csv', 'w', newline='', encoding='utf-8') as csvfile:
        fieldnames = ['PR Link', 'Title', 'Linked Issue', 'Creation Date', 'Last Updated']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        
        writer.writeheader()
        for pr in prs:
            linked_issue_num = get_linked_issue_from_pr(pr)
            linked_issue_url = ''
            if linked_issue_num:
                linked_issue_url = f"https://github.com/swagger-api/swagger-parser/issues/{linked_issue_num}"
            
            writer.writerow({
                'PR Link': pr.get('html_url', ''),
                'Title': pr.get('title', ''),
                'Linked Issue': linked_issue_url,
                'Creation Date': pr.get('created_at', ''),
                'Last Updated': pr.get('updated_at', '')
            })
    
    print(f"✓ Generated pull_requests.csv with {len(prs)} PRs")
    
    # Generate SUMMARY.md
    with open('SUMMARY.md', 'w', encoding='utf-8') as f:
        f.write("# Swagger Parser - Open Issues and Pull Requests Report\n\n")
        f.write(f"**Generated on:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S UTC')}\n\n")
        f.write(f"**Repository:** [swagger-api/swagger-parser](https://github.com/swagger-api/swagger-parser)\n\n")
        f.write(f"**Criteria:** Open issues and pull requests created or updated after 2025-01-01\n\n")
        
        f.write("## Summary Statistics\n\n")
        f.write(f"- **Total Open Issues:** {len(issues)}\n")
        f.write(f"- **Total Open Pull Requests:** {len(prs)}\n\n")
        
        # Issue statistics
        if issues:
            f.write("### Issues Breakdown\n\n")
            issues_with_comments = sum(1 for i in issues if i.get('comments', 0) > 0)
            issues_without_comments = len(issues) - issues_with_comments
            total_comments = sum(i.get('comments', 0) for i in issues)
            
            f.write(f"- Issues with comments: {issues_with_comments}\n")
            f.write(f"- Issues without comments: {issues_without_comments}\n")
            f.write(f"- Total comments across all issues: {total_comments}\n")
            if len(issues) > 0:
                f.write(f"- Average comments per issue: {total_comments / len(issues):.2f}\n")
            f.write("\n")
            
            # Label breakdown
            label_counts = {}
            for issue in issues:
                for label in issue.get('labels', []):
                    label_name = label.get('name', 'Unknown')
                    label_counts[label_name] = label_counts.get(label_name, 0) + 1
            
            if label_counts:
                f.write("#### Issues by Label\n\n")
                for label, count in sorted(label_counts.items(), key=lambda x: x[1], reverse=True):
                    f.write(f"- {label}: {count}\n")
                f.write("\n")
        
        # PR statistics
        if prs:
            f.write("### Pull Requests Breakdown\n\n")
            draft_prs = sum(1 for pr in prs if pr.get('draft', False))
            ready_prs = len(prs) - draft_prs
            prs_with_linked_issues = sum(1 for pr in prs if get_linked_issue_from_pr(pr))
            
            f.write(f"- Draft PRs: {draft_prs}\n")
            f.write(f"- Ready for review PRs: {ready_prs}\n")
            f.write(f"- PRs with linked issues: {prs_with_linked_issues}\n")
            f.write(f"- PRs without linked issues: {len(prs) - prs_with_linked_issues}\n\n")
        
        # Recent activity
        f.write("### Recent Activity\n\n")
        
        if issues:
            # Most recent issues
            recent_issues = sorted(issues, key=lambda x: x.get('created_at', ''), reverse=True)[:5]
            f.write("#### Most Recently Created Issues\n\n")
            for issue in recent_issues:
                created_date = issue.get('created_at', '')[:10]
                f.write(f"- [#{issue.get('number')}]({issue.get('html_url')}): {issue.get('title')} ({created_date})\n")
            f.write("\n")
            
            # Most commented issues
            most_commented = sorted(issues, key=lambda x: x.get('comments', 0), reverse=True)[:5]
            f.write("#### Most Commented Issues\n\n")
            for issue in most_commented:
                f.write(f"- [#{issue.get('number')}]({issue.get('html_url')}): {issue.get('title')} ({issue.get('comments', 0)} comments)\n")
            f.write("\n")
            
            # Recently updated
            recent_updated = sorted(issues, key=lambda x: x.get('updated_at', ''), reverse=True)[:5]
            f.write("#### Most Recently Updated Issues\n\n")
            for issue in recent_updated:
                updated_date = issue.get('updated_at', '')[:10]
                f.write(f"- [#{issue.get('number')}]({issue.get('html_url')}): {issue.get('title')} (updated {updated_date})\n")
            f.write("\n")
        
        if prs:
            # Most recent PRs
            recent_prs = sorted(prs, key=lambda x: x.get('created_at', ''), reverse=True)[:5]
            f.write("#### Most Recently Created Pull Requests\n\n")
            for pr in recent_prs:
                created_date = pr.get('created_at', '')[:10]
                f.write(f"- [#{pr.get('number')}]({pr.get('html_url')}): {pr.get('title')} ({created_date})\n")
            f.write("\n")
        
        f.write("## Output Files\n\n")
        f.write("- **issues.csv** - Detailed list of all open issues with links, titles, comment counts, linked PRs, and dates\n")
        f.write("- **pull_requests.csv** - Detailed list of all open pull requests with links, linked issues, and dates\n")
        f.write("- **SUMMARY.md** - This summary report\n\n")
        
        f.write("## How to Use This Data\n\n")
        f.write("The CSV files can be opened in Excel, Google Sheets, or any spreadsheet application for further analysis.\n\n")
        
        f.write("---\n")
        f.write(f"*Report generated on {datetime.now().strftime('%Y-%m-%d %H:%M:%S UTC')}*\n")
    
    print(f"✓ Generated SUMMARY.md")
    print("\n✅ All reports generated successfully!")
    print("\nGenerated files:")
    print("  - issues.csv")
    print("  - pull_requests.csv")
    print("  - SUMMARY.md")

def main():
    """Main function to process data from JSON files or stdin."""
    
    # Check if JSON files exist or read from arguments
    if len(sys.argv) >= 3:
        # Read from file arguments
        with open(sys.argv[1], 'r') as f:
            issues_data = json.load(f)
        with open(sys.argv[2], 'r') as f:
            prs_data = json.load(f)
        
        # Extract items if they're in the MCP format
        issues = issues_data.get('items', issues_data) if isinstance(issues_data, dict) else issues_data
        prs = prs_data.get('items', prs_data) if isinstance(prs_data, dict) else prs_data
        
        generate_reports(issues, prs)
    else:
        print("Usage: python3 collect_and_generate.py <issues.json> <prs.json>")
        print("\nOr create issues.json and prs.json files with the GitHub MCP tool output.")
        sys.exit(1)

if __name__ == '__main__':
    main()
