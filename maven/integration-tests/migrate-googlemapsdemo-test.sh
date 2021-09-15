#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source $SCRIPTPATH/inc/env.sh
cd $SCRIPTPATH/build
if [ -d googlemapsdemo ]; then
  rm -rf googlemapsdemo
fi

curl -L https://github.com/codenameone/codenameone-google-maps/archive/refs/tags/v1.0.2-snap2.zip > master.zip
unzip master.zip
rm master.zip
mvn com.codenameone:codenameone-maven-plugin:${CN1_VERSION}:generate-app-project \
  -DarchetypeGroupId=com.codename1 \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeVersion=${CN1_VERSION} \
  -DartifactId=googlemapsdemo \
  -DgroupId=com.example \
  -Dversion=1.0-SNAPSHOT \
  -DinteractiveMode=false \
  -DsourceProject=codenameone-google-maps-1.0.2-snap2/GoogleMapsTest

rm -rf codenameone-google-maps-1.0.2-snap2

cd googlemapsdemo
chmod 755 build.sh
./build.sh jar
if [ -d /Applications/Xcode.app ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-source" -Dopen=false
fi
if [ -d $HOME/Library/Android/sdk ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=android-source" -Dopen=false
fi


