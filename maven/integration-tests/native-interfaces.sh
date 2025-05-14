#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source $SCRIPTPATH/inc/env.sh
cd $SCRIPTPATH/build
if [ -d testnativeinterface ]; then
  rm -rf testnativeinterface
fi

curl -L https://github.com/shannah/cn1-native-interface-tests/archive/refs/tags/v1.0.zip > master.zip
unzip master.zip
rm master.zip
mvn com.codenameone:codenameone-maven-plugin:${CN1_VERSION}:generate-app-project \
  -DarchetypeGroupId=com.codename1 \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeVersion=${CN1_VERSION} \
  -DartifactId=testnativeinterface \
  -DgroupId=com.example \
  -Dversion=1.0-SNAPSHOT \
  -DinteractiveMode=false \
  -DsourceProject=cn1-native-interface-tests-1.0

rm -rf cn1-native-interface-tests-1.0

cd testnativeinterface

if [ ! -f ios/src/main/objectivec/com_codename1_testnativeinterface_HelloNativeImpl.h ]; then
  echo "iOS header missing after migration"
  exit 1
fi
if [ ! -f ios/src/main/objectivec/com_codename1_testnativeinterface_HelloNativeImpl.m ]; then
  echo "iOS implementation missing after migration"
  exit 1
fi
if [ ! -f android/src/main/java/com/codename1/testnativeinterface/HelloNativeImpl.java ]; then
  echo "Android native implementation missing after migration"
  exit 1
fi
if [ ! -f javase/src/main/java/com/codename1/testnativeinterface/HelloNativeImpl.java ]; then
  echo "JavaSE native implementation missing after migration"
  exit 1
fi
if [ ! -f win/src/main/csharp/com/codename1/testnativeinterface/HelloNativeImpl.cs ]; then
  echo "Win native implementation missing after migration"
  exit 1
fi
if [ ! -f javascript/src/main/javascript/com_codename1_testnativeinterface_HelloNative.js ]; then
  echo "Javascript native implementation missing after migration"
  exit 1
fi

mvn package -Dcodename1.platform=javase
if [ -d /Applications/Xcode.app ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-source" "-Dopen=false"
fi
if [ -d $HOME/Library/Android/sdk ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=android-source" "-Dopen=false"
fi


