#!/usr/bin/python

import ghApiClient

def getLastReleaseTag():
    content = ghApiClient.readUrl('repos/swagger-api/swagger-parser/releases')
    for l in content:
        draft = l["draft"]
        tag = l["tag_name"]
        if str(draft) != 'True' and tag.startswith("v1"):
            return tag[1:]

# main
def main():
    result = getLastReleaseTag()
    print(result)

# here start main
main()

