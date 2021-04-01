#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source $SCRIPTPATH/inc/env.sh
cd $SCRIPTPATH/build
if [ -d androidnativetest ]; then
  rm -rf androidnativetest
fi

curl -L https://github.com/shannah/test-android-native-interfaces/archive/refs/tags/1.0.1.zip > master.zip
unzip master.zip
rm master.zip

# First make sure the thing builds on its own.
mv test-android-native-interfaces-1.0.1 androidnativetest
cd androidnativetest
chmod 755 build.sh
./build.sh jar
if [ -d /Applications/Xcode.app ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-source" -Dopen=false
else
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=none" -Dopen=false
fi
if [ -d $HOME/Library/Android/sdk ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=android-source" -Dopen=false
else
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=none" -Dopen=false
fi

# Now update cn1 version and try to build again.
mvn cn1:update -DnewVersion=${CN1_VERSION}
./build.sh jar
if [ -d /Applications/Xcode.app ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-source" -Dopen=false
else
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=none" -Dopen=false
fi
if [ -d $HOME/Library/Android/sdk ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=android-source" -Dopen=false
else
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=none" -Dopen=false
fi

# Now, let's generate a fresh project, copy in the sources, and make sure she builds
cd ..
if [ -d nativeinterfacestest2 ]; then
  rm -rf nativeinterfacestest2
fi
mkdir nativeinterfacestest2
cd nativeinterfacestest2

mvn archetype:generate \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion=$CN1_VERSION \
  -DartifactId=androidnativetest \
  -DgroupId=com.codename1.androidnativetest1 \
  -Dversion=1.0-SNAPSHOT \
  -DmainName=AndroidNativeTest \
  -DinteractiveMode=false

cp ../androidnativetest/common/codenameone_settings.properties androidnativetest/common/codenameone_settings.properties
for module in common android javase; do
  cp -r ../androidnativetest/$module/src/* androidnativetest/$module/src/
done

cd androidnativetest
./build.sh jar
if [ -d /Applications/Xcode.app ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-source" -Dopen=false
else
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=none" -Dopen=false
fi
if [ -d $HOME/Library/Android/sdk ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=android-source" -Dopen=false
else
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=none" -Dopen=false
fi






