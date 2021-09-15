#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source $SCRIPTPATH/inc/env.sh
cd $SCRIPTPATH/build
if [ -d cssfonts ]; then
  rm -rf cssfonts
fi

curl -L https://github.com/shannah/cn1-css-fonts-test/archive/refs/tags/v1.0.zip > master.zip
unzip master.zip
rm master.zip
mvn com.codenameone:codenameone-maven-plugin:${CN1_VERSION}:generate-app-project \
  -DarchetypeGroupId=com.codename1 \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeVersion=${CN1_VERSION} \
  -DartifactId=cssfonts \
  -DgroupId=com.example \
  -Dversion=1.0-SNAPSHOT \
  -DinteractiveMode=false \
  -DsourceProject=cn1-css-fonts-test-1.0

rm -rf cn1-css-fonts-test-1.0

cd cssfonts

# The project should build fine without the ttf files in the src directories
# because the ttf files should have been copied into the css directory.
rm -f common/src/main/resources/*.ttf

mvn package -Dcodename1.platform=javase



