*TODO, for the next release:*
Transform this README to a bash script accompanied with its Jenkins job :)


# How to deploy a new release to Restlet's Nexus

```
# Prepare release. 
# Accept all default value execpt \for tag name. Use this pattern: v1.0.26.x-restlet
mvn release:clean release:prepare 

# Release version. 
mvn release:perform

#Commit the new POMs with the message
git commit -m 'Prepare next development version'

# Push with tags
git push --follow-tags
```

