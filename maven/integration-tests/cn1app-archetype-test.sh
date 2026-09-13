#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source $SCRIPTPATH/inc/env.sh
source $SCRIPTPATH/inc/auto-bundle-pref.sh

# Force `cn1.autoDefaultResourceBundle=true` for the duration of the test so
# the `cn1:css` subprocess takes the JavaSEPort.enableAutoLocalizationBundle
# branch -- the same path that crashed for end users in #4850 but is invisible
# in CI under the default-false preference.
set_auto_bundle_pref true
trap 'set_auto_bundle_pref false' EXIT

cd $SCRIPTPATH/build
if [ -d myapp1 ]; then
  rm -rf myapp1
fi
mvn archetype:generate \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion=$CN1_VERSION \
  -DartifactId=myapp1 \
  -DgroupId=com.example \
  -Dversion=1.0-SNAPSHOT \
  -DmainName=MyApp \
  -DinteractiveMode=false

cd myapp1
chmod 755 build.sh
./build.sh jar
if [ -d /Applications/Xcode.app ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-source" -Dopen=false
fi
if [ -d $HOME/Library/Android/sdk ]; then
  "mvn" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=android-source" -Dopen=false
fi
# The backend module, which no other step here reaches: build.sh above builds the
# client, and the two platform builds are behind their own SDK checks.
#
# It earns its place. The generated server is a @RestController with no main of its
# own -- the router and the entry point are generated from it during
# process-classes -- so "does the template still compile" and "does the generator
# still produce an entry point" are two different questions and this asks both. The
# reason it is here at all is that the template once shipped with its copyright
# header missing the closing "*/", which put the package declaration and every
# import inside a comment; it was found by hand, and nothing in CI would have said
# a word.
mvn -pl backend -Dcodename1.platform=backend process-classes

MAIN_CLASS_FILE="backend/target/classes/META-INF/cn1-backend-main"
if [ ! -f "$MAIN_CLASS_FILE" ]; then
  echo "the backend module did not record a generated entry point" >&2
  exit 1
fi
GENERATED_MAIN="$(cat "$MAIN_CLASS_FILE")"
echo "backend entry point: $GENERATED_MAIN"
if [ ! -f "backend/target/classes/$(echo "$GENERATED_MAIN" | tr '.' '/').class" ]; then
  echo "the recorded entry point $GENERATED_MAIN was not compiled" >&2
  exit 1
fi
# The router lands beside the entry point, whatever package the archetype was told
# to use -- derived rather than assumed, since `package` defaults to the groupId.
ROUTER_DIR="$(dirname "$(echo "$GENERATED_MAIN" | tr '.' '/')")"
if [ ! -f "backend/target/classes/$ROUTER_DIR/ApiRouter.class" ]; then
  echo "no router was generated for the @RestController" >&2
  exit 1
fi
