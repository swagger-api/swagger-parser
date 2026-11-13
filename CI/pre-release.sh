#!/bin/bash

CUR=$(pwd)

export SC_VERSION=`mvn -q -Dexec.executable="echo" -Dexec.args='${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion}' --non-recursive build-helper:parse-version org.codehaus.mojo:exec-maven-plugin:3.7.0:exec`
export SC_NEXT_VERSION=`mvn -q -Dexec.executable="echo" -Dexec.args='${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.nextIncrementalVersion}' --non-recursive build-helper:parse-version org.codehaus.mojo:exec-maven-plugin:3.7.0:exec`
SC_QUALIFIER=`mvn -q -Dexec.executable="echo" -Dexec.args='${parsedVersion.qualifier}' --non-recursive build-helper:parse-version org.codehaus.mojo:exec-maven-plugin:3.7.0:exec`
SC_LAST_RELEASE=`python $CUR/CI/lastRelease.py`


SC_RELEASE_TAG="v$SC_VERSION"


#####################
### build and test maven ###
#####################
mvn --no-transfer-progress -B install --file pom.xml
