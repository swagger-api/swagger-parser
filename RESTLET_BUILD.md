*TODO, for the next release:*
Transform this README to a bash script accompanied with its Jenkins job :)


# How to deploy a new release to Restlet's Nexus

- Run `mvn versions:set -DgenerateBackupPoms=false`.
- When prompted for the version, enter `<version-of-swagger-parser>.<release-number>-restlet`. 
  For example, if your branch is based on swagger-parser 1.0.26 and it's the second Restlet release, 
  then the version is `1.0.26.2-restlet`.
- Commit the new POMs with the message `Release <version>`.
- Tag this new commit with `v<version>`. For example, `v1.0.26.2-restlet`.
- Push the new tag with command `git push --follow-tags`.
- Run `mvn deploy`.
- Run `mvn versions:set` again.
- When prompted for the version, enter `<version-of-swagger-parser>.<release+1>-restlet-SNAPSHOT`. 
  For example, `1.0.26.3-restlet-SNAPSHOT`.
- Commit the new POMs with the message `Prepare next development version`.
