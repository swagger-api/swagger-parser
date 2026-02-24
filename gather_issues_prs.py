#!/usr/bin/env python3

"""
Script to gather all open issues and pull requests from swagger-api/swagger-parser
that have been created or updated after January 1, 2025.

Outputs:
- issues.csv: List of issues with link, title, comments, PR link (if exists), creation date
- pull_requests.csv: List of PRs with link to PR, link to issue, creation date
- SUMMARY.md: Summary of the data gathered
"""

import json
import csv
import sys
from datetime import datetime
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError
import time

# GitHub API configuration
GITHUB_API_BASE = "https://api.github.com"
REPO_OWNER = "swagger-api"
REPO_NAME = "swagger-parser"
SINCE_DATE = "2025-01-01T00:00:00Z"

def make_github_request(endpoint, params=None):
    """Make a request to GitHub API with proper headers."""
    url = f"{GITHUB_API_BASE}{endpoint}"
    if params:
        param_str = "&".join([f"{k}={v}" for k, v in params.items()])
        url = f"{url}?{param_str}"
    
    request = Request(url)
    request.add_header("Accept", "application/vnd.github.v3+json")
    request.add_header("User-Agent", "swagger-parser-issue-collector")
    
    try:
        time.sleep(0.1)  # Rate limiting
        response = urlopen(request)
        data = json.loads(response.read().decode('utf-8'))
        
        # Check for pagination
        link_header = response.headers.get('Link', '')
        has_next = 'rel="next"' in link_header
        
        return data, has_next
    except HTTPError as e:
        print(f"HTTP Error: {e.code} - {e.reason}")
        print(f"URL: {url}")
        raise
    except URLError as e:
        print(f"URL Error: {e.reason}")
        raise

def fetch_all_issues():
    """Fetch all open issues created or updated after SINCE_DATE."""
    issues = []
    page = 1
    
    print(f"Fetching open issues created/updated after {SINCE_DATE}...")
    
    while True:
        endpoint = f"/repos/{REPO_OWNER}/{REPO_NAME}/issues"
        params = {
            "state": "open",
            "since": SINCE_DATE,
            "per_page": 100,
            "page": page
        }
        
        data, has_next = make_github_request(endpoint, params)
        
        # Filter out pull requests (they come mixed with issues in this endpoint)
        for item in data:
            if 'pull_request' not in item:
                issues.append(item)
        
        print(f"  Page {page}: fetched {len(data)} items, {len([i for i in data if 'pull_request' not in i])} issues")
        
        if not has_next or len(data) == 0:
            break
        
        page += 1
    
    print(f"Total issues fetched: {len(issues)}")
    return issues

def fetch_all_pull_requests():
    """Fetch all open pull requests created or updated after SINCE_DATE."""
    prs = []
    page = 1
    
    print(f"\nFetching open pull requests created/updated after {SINCE_DATE}...")
    
    while True:
        endpoint = f"/repos/{REPO_OWNER}/{REPO_NAME}/pulls"
        params = {
            "state": "open",
            "per_page": 100,
            "page": page,
            "sort": "updated",
            "direction": "desc"
        }
        
        data, has_next = make_github_request(endpoint, params)
        
        # Filter PRs by date
        for pr in data:
            created_at = datetime.fromisoformat(pr['created_at'].replace('Z', '+00:00'))
            updated_at = datetime.fromisoformat(pr['updated_at'].replace('Z', '+00:00'))
            since_dt = datetime.fromisoformat(SINCE_DATE.replace('Z', '+00:00'))
            
            if created_at >= since_dt or updated_at >= since_dt:
                prs.append(pr)
        
        print(f"  Page {page}: fetched {len(data)} PRs, {len([p for p in data if datetime.fromisoformat(p['created_at'].replace('Z', '+00:00')) >= since_dt or datetime.fromisoformat(p['updated_at'].replace('Z', '+00:00')) >= since_dt])} match criteria")
        
        # If we've reached PRs that are too old, stop
        if data and all(datetime.fromisoformat(pr['updated_at'].replace('Z', '+00:00')) < since_dt for pr in data):
            break
        
        if not has_next or len(data) == 0:
            break
        
        page += 1
    
    print(f"Total PRs fetched: {len(prs)}")
    return prs

def get_linked_issue_from_pr(pr):
    """Extract linked issue number from PR body or title."""
    # Try to find issue references like #1234, fixes #1234, closes #1234
    body = pr.get('body') or ''
    title = pr.get('title') or ''
    
    # Common patterns for issue references
    import re
    patterns = [
        r'(?:fix(?:es|ed)?|close(?:s|d)?|resolve(?:s|d)?)\s+#(\d+)',
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
            return pr['html_url']
    return None

def write_issues_csv(issues, prs, filename="issues.csv"):
    """Write issues data to CSV file."""
    print(f"\nWriting issues to {filename}...")
    
    with open(filename, 'w', newline='', encoding='utf-8') as csvfile:
        fieldnames = ['Issue Link', 'Title', 'Number of Comments', 'Linked PR', 'Creation Date', 'Last Updated']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        
        writer.writeheader()
        for issue in issues:
            linked_pr = get_linked_pr_from_issue(issue['number'], prs)
            writer.writerow({
                'Issue Link': issue['html_url'],
                'Title': issue['title'],
                'Number of Comments': issue['comments'],
                'Linked PR': linked_pr or '',
                'Creation Date': issue['created_at'],
                'Last Updated': issue['updated_at']
            })
    
    print(f"  Wrote {len(issues)} issues to {filename}")

def write_prs_csv(prs, filename="pull_requests.csv"):
    """Write PRs data to CSV file."""
    print(f"\nWriting pull requests to {filename}...")
    
    with open(filename, 'w', newline='', encoding='utf-8') as csvfile:
        fieldnames = ['PR Link', 'Title', 'Linked Issue', 'Creation Date', 'Last Updated', 'State']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        
        writer.writeheader()
        for pr in prs:
            linked_issue_num = get_linked_issue_from_pr(pr)
            linked_issue_url = ''
            if linked_issue_num:
                linked_issue_url = f"https://github.com/{REPO_OWNER}/{REPO_NAME}/issues/{linked_issue_num}"
            
            writer.writerow({
                'PR Link': pr['html_url'],
                'Title': pr['title'],
                'Linked Issue': linked_issue_url,
                'Creation Date': pr['created_at'],
                'Last Updated': pr['updated_at'],
                'State': pr['state']
            })
    
    print(f"  Wrote {len(prs)} PRs to {filename}")

def write_summary(issues, prs, filename="SUMMARY.md"):
    """Write summary report."""
    print(f"\nWriting summary to {filename}...")
    
    with open(filename, 'w', encoding='utf-8') as f:
        f.write("# Swagger Parser - Open Issues and Pull Requests Report\n\n")
        f.write(f"**Generated on:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S UTC')}\n\n")
        f.write(f"**Repository:** [{REPO_OWNER}/{REPO_NAME}](https://github.com/{REPO_OWNER}/{REPO_NAME})\n\n")
        f.write(f"**Criteria:** Open issues and pull requests created or updated after {SINCE_DATE}\n\n")
        
        f.write("## Summary Statistics\n\n")
        f.write(f"- **Total Open Issues:** {len(issues)}\n")
        f.write(f"- **Total Open Pull Requests:** {len(prs)}\n\n")
        
        # Issue statistics
        f.write("### Issues Breakdown\n\n")
        issues_with_comments = sum(1 for i in issues if i['comments'] > 0)
        issues_without_comments = len(issues) - issues_with_comments
        total_comments = sum(i['comments'] for i in issues)
        
        f.write(f"- Issues with comments: {issues_with_comments}\n")
        f.write(f"- Issues without comments: {issues_without_comments}\n")
        f.write(f"- Total comments across all issues: {total_comments}\n")
        if issues:
            f.write(f"- Average comments per issue: {total_comments / len(issues):.2f}\n")
        f.write("\n")
        
        # PR statistics
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
            recent_issues = sorted(issues, key=lambda x: x['created_at'], reverse=True)[:5]
            f.write("#### Most Recently Created Issues\n\n")
            for issue in recent_issues:
                f.write(f"- [#{issue['number']}]({issue['html_url']}): {issue['title']} ({issue['created_at'][:10]})\n")
            f.write("\n")
            
            # Most commented issues
            most_commented = sorted(issues, key=lambda x: x['comments'], reverse=True)[:5]
            f.write("#### Most Commented Issues\n\n")
            for issue in most_commented:
                f.write(f"- [#{issue['number']}]({issue['html_url']}): {issue['title']} ({issue['comments']} comments)\n")
            f.write("\n")
        
        if prs:
            # Most recent PRs
            recent_prs = sorted(prs, key=lambda x: x['created_at'], reverse=True)[:5]
            f.write("#### Most Recently Created Pull Requests\n\n")
            for pr in recent_prs:
                f.write(f"- [#{pr['number']}]({pr['html_url']}): {pr['title']} ({pr['created_at'][:10]})\n")
            f.write("\n")
        
        f.write("## Output Files\n\n")
        f.write("- `issues.csv` - Detailed list of all open issues\n")
        f.write("- `pull_requests.csv` - Detailed list of all open pull requests\n")
        f.write("- `SUMMARY.md` - This summary file\n\n")
        
        f.write("---\n")
        f.write(f"*Report generated by gather_issues_prs.py on {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*\n")
    
    print(f"  Summary written to {filename}")

def main():
    """Main execution function."""
    print("=" * 80)
    print("Swagger Parser - Issue and PR Gathering Tool")
    print("=" * 80)
    
    try:
        # Fetch data
        issues = fetch_all_issues()
        prs = fetch_all_pull_requests()
        
        # Generate output files
        write_issues_csv(issues, prs)
        write_prs_csv(prs)
        write_summary(issues, prs)
        
        print("\n" + "=" * 80)
        print("SUCCESS! All files generated successfully.")
        print("=" * 80)
        print("\nGenerated files:")
        print("  - issues.csv")
        print("  - pull_requests.csv")
        print("  - SUMMARY.md")
        
        return 0
        
    except Exception as e:
        print(f"\n" + "=" * 80)
        print(f"ERROR: {str(e)}")
        print("=" * 80)
        import traceback
        traceback.print_exc()
        return 1

if __name__ == "__main__":
    sys.exit(main())
