#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
cd $SCRIPTPATH
if [ ! -d build ]; then
  mkdir build
fi
cd build
if [ -d kitchensink ]; then
  rm -rf kitchensink
fi

curl -L https://github.com/codenameone/KitchenSink/archive/v1.0-cn7.0.11.zip > master.zip
unzip master.zip
rm master.zip
mvn com.codenameone:codenameone-maven-plugin:${CN1_VERSION}:generate-app-project \
  -DarchetypeGroupId=com.codename1 \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeVersion=${CN1_VERSION} \
  -DartifactId=kitchensink \
  -DgroupId=com.example \
  -Dversion=1.0-SNAPSHOT \
  -DinteractiveMode=false \
  -DsourceProject=KitchenSink-1.0-cn7.0.11

rm -rf KitchenSink-1.0-cn7.0.11

cd kitchensink
chmod 755 build.sh
./build.sh jar
if [ -d /Applications/Xcode.app ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-source" -Dopen=false
fi
if [ -d $HOME/Library/Android/sdk ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=android-source" -Dopen=false
fi


