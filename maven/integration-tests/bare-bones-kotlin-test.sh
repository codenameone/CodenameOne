#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
cd $SCRIPTPATH
if [ ! -d build ]; then
  mkdir build
fi
cd build
if [ -d myapp2 ]; then
  rm -rf myapp2
fi
curl -L https://github.com/shannah/cn1app-archetype-kotlin-template/archive/master.zip > master.zip
unzip master.zip
rm master.zip
mvn com.codenameone:codenameone-maven-plugin:${CN1_VERSION}:generate-app-project \
  -DarchetypeGroupId=com.codename1 \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeVersion=${CN1_VERSION} \
  -DartifactId=myapp2 \
  -DgroupId=com.example \
  -Dversion=1.0-SNAPSHOT \
  -DinteractiveMode=false \
  -DmainName=MyApp2 \
  -DsourceProject=cn1app-archetype-kotlin-template-master

rm -rf cn1app-archetype-kotlin-template-master

cd myapp2
chmod 755 build.sh
#./build.sh jar
if [ -d /Applications/Xcode.app ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-source" -Dopen=false
fi
if [ -d $HOME/Library/Android/sdk ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=android-source" -Dopen=false
fi

