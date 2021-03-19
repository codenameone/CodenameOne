#!/bin/bash
set -e
source env.sh
cd $SCRIPTPATH
ANT_PROJECT_DIR=$SCRIPTPATH/core
cd build
if [ -d core ]; then
  rm -rf core
fi
mvn com.codenameone:codenameone-maven-plugin:${CN1_VERSION}:generate-app-project \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeVersion=${CN1_VERSION} \
  -DartifactId=core \
  -DgroupId=com.codenameone.tests \
  -Dversion=1.0-SNAPSHOT \
  -DinteractiveMode=false \
  -DsourceProject=$ANT_PROJECT_DIR \
  -Dcn1Version=$CN1_VERSION

cd core
mvn package -Dcodename1.platform=javase
