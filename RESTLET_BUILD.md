*TODO, for the next release:*
Transform this README to a bash script accompanied with its Jenkins job :)


# How to deploy a new release to Restlet's Nexus

```
version=TO BE DEFINED # Example: 1.0.26.2-restlet

# Prepare release (check and accept default values). 
mvn release:clean release:prepare 
# To change the version, you can use the following properties
# mvn release:clean release:prepare -B -Dtag="v$version" -DreleaseVersion="$version"

# Release version. If release fails with a 400 error from nexus, trying allowing temporary to Redeploy Artefact on the repository (weird, but it works for me - issue in maven plugin??) 
mvn release:perform

#Commit the new POMs with the message
git commit -m 'Prepare next development version'

# Push with tags
git push --follow-tags
```

