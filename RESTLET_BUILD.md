*TODO, for the next release:*
Transform this README to a bash script accompanied with its Jenkins job :)


# How to deploy a new release to Restlet's Nexus

```
version=TO BE DEFINED # Example: 1.0.26.2-restlet
mvn release:clean release:prepare -B -Dtag="v$version" -DreleaseVersion="$version"
mvn release:perform -B
```

Commit the new POMs with the message
`git commit -m 'Prepare next development version'`.
