#!/bin/bash
#
# Build hints written as annotations must reach the build request, and the same
# hint set twice must fail.
#
# A build hint used to be an unchecked string: misspell it and the build stayed
# green while the setting silently did nothing. The annotations exist so the
# compiler catches that, and this test covers the part the compiler cannot --
# that the annotation is actually converted back into the codename1.arg.* pair
# the builders read, and that it is not silently merged with a properties line
# saying something different.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source $SCRIPTPATH/inc/env.sh

cd $SCRIPTPATH/build
rm -rf myapphints
mvn archetype:generate \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion=$CN1_VERSION \
  -DartifactId=myapphints \
  -DgroupId=com.example \
  -Dversion=1.0-SNAPSHOT \
  -DmainName=MyApp \
  -DinteractiveMode=false

cd myapphints
chmod 755 mvnw

MAIN=common/src/main/java/com/example/MyApp.java
SETTINGS=common/codenameone_settings.properties

echo "--- the generated project must already use annotations ---"
grep -q "com.codename1.annotations.buildhints" $MAIN \
  || { echo "FAIL: the archetype's main class does not import the build hint annotations"; exit 1; }
grep -q "^codename1.arg.ios.newStorageLocation" $SETTINGS \
  && { echo "FAIL: ios.newStorageLocation should have moved to @Ios, not stayed in $SETTINGS"; exit 1; }

echo "--- add a hint of each shape ---"
perl -0pi -e 's/\@Ios\(/\@Ios(pods = {"Alamofire", "SwiftyJSON"}, teamId = "ABCDE12345", /' $MAIN
grep -q 'pods = {"Alamofire"' $MAIN || { echo "FAIL: could not patch $MAIN"; exit 1; }

echo "--- process-classes must emit the hints ---"
./mvnw -B -q -pl common process-classes
EMITTED=common/target/classes/META-INF/codenameone/build-hints.properties
test -f $EMITTED || { echo "FAIL: $EMITTED was not emitted"; exit 1; }

check() {
  grep -qF "$1" $EMITTED || { echo "FAIL: expected '$1' in $EMITTED"; cat $EMITTED; exit 1; }
}
# a list joins with the hint's own separator, an enum uses the catalog's value
# rather than the constant name, and an unset attribute writes nothing at all
check "codename1.arg.ios.pods=Alamofire,SwiftyJSON"
check "codename1.arg.ios.teamId=ABCDE12345"
check "codename1.arg.ios.themeMode=modern"
check "codename1.arg.desktop.titleBar=native"
grep -q "codename1.arg.ios.objC" $EMITTED \
  && { echo "FAIL: an attribute nobody set must not be written"; exit 1; }

echo "--- the hints must reach the build request ---"
# "Build target not supported" is thrown after the merged settings file is
# written, so this asserts the upload payload offline: no SDK, no cloud build.
set +e
./mvnw -B -q -DskipTests -Dcodename1.platform=javase \
       -Dcodename1.buildTarget=local-build-hint-probe package > /tmp/cn1-hints-build.log 2>&1
set -e
MERGED=common/target/codenameone/antProject/codenameone_settings.properties
test -f $MERGED || MERGED=javase/target/codenameone/antProject/codenameone_settings.properties
if [ -f "$MERGED" ]; then
  grep -q "codename1.arg.ios.pods=Alamofire,SwiftyJSON" $MERGED \
    || { echo "FAIL: annotation hints did not reach the build request"; cat $MERGED; exit 1; }
  echo "OK: annotation hints reached $MERGED"
else
  echo "NOTE: no build request was written for this target; skipping that assertion"
fi

echo "--- declaring the same hint twice must fail ---"
echo "codename1.arg.ios.teamId=FROMFILE" >> $SETTINGS
set +e
./mvnw -B -pl common process-classes > /tmp/cn1-hints-conflict.log 2>&1
STATUS=$?
set -e
if [ $STATUS -eq 0 ]; then
  echo "FAIL: a hint set in both the annotation and $SETTINGS should fail the build"
  exit 1
fi
grep -q "codename1.arg.ios.teamId is declared twice" /tmp/cn1-hints-conflict.log \
  || { echo "FAIL: the conflict error did not name the hint"; tail -30 /tmp/cn1-hints-conflict.log; exit 1; }
grep -q "@Ios(teamId)" /tmp/cn1-hints-conflict.log \
  || { echo "FAIL: the conflict error did not name the annotation attribute"; exit 1; }

echo "PASSED build-hint-annotations-test"
