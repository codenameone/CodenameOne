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
# The beans are wired by a class the build generates beside the entry point; the
# sample's controller takes its Greeter service through the constructor, so a
# missing BackendWiring means dependency injection did not happen at all.
if [ ! -f "backend/target/classes/$ROUTER_DIR/BackendWiring.class" ]; then
  echo "no BackendWiring was generated for the backend's beans" >&2
  exit 1
fi

# `cn1:backend-package` -- the goal that turns the module above into a native
# binary -- ON A JDK THAT IS NOT 8, with JDK_8_HOME deliberately unset.
#
# It used to demand a JDK 8 and refuse to run without one, so a developer on a
# current JDK was told to install a compiler from 2014 to build a server. Nothing
# noticed, because nothing had ever run this goal: the checks above stop at
# process-classes, and the two platform builds are behind their own SDK checks.
# The premise was that a newer javac emits class files the translator cannot read;
# it does not, because the compile passes -source 1.8 -target 1.8 and the class
# file version is 52 whichever javac produces it.
#
# So the variable is REMOVED from the environment rather than left alone. With it
# set -- and CI sets it -- the goal would take that path and this would pass while
# the developer's build still failed.
#
# CN1_BACKEND_PACKAGE_JDK names the JDK to use; without it, whatever is running.
# The point of the check is a JDK that is not 8, so a caller that has one says so.
BACKEND_PACKAGE_JDK="${CN1_BACKEND_PACKAGE_JDK:-$JAVA_HOME}"
if [ -z "$BACKEND_PACKAGE_JDK" ]; then
  echo "neither CN1_BACKEND_PACKAGE_JDK nor JAVA_HOME names a JDK" >&2
  exit 1
fi
if ! command -v clang >/dev/null 2>&1; then
  # Skipping is a result, and a silent one reads exactly like a pass. Required
  # says which machines must not skip: CI sets it, a laptop without clang does not.
  if [ "${CN1_BACKEND_PACKAGE_REQUIRED:-0}" = "1" ]; then
    echo "clang is required to package a backend and is not on PATH" >&2
    exit 1
  fi
  echo "NOTE skipping cn1:backend-package: no clang on PATH"
else
  echo "packaging the backend natively with $BACKEND_PACKAGE_JDK"
  env -u JDK_8_HOME JAVA_HOME="$BACKEND_PACKAGE_JDK" \
    mvn -pl backend -Dcodename1.platform=backend cn1:backend-package

  BACKEND_BIN="backend/target/myapp1-backend"
  if [ ! -x "$BACKEND_BIN" ]; then
    echo "cn1:backend-package produced no executable at $BACKEND_BIN" >&2
    exit 1
  fi
  # RUN it. "The file exists" would have passed on a binary that cannot start,
  # and the whole claim of this goal is a server you can deploy -- so the check is
  # an actual request answered by an actual process. /healthz is the route the
  # generated Api declares.
  BACKEND_PORT="${CN1_BACKEND_PACKAGE_PORT:-18080}"
  PORT="$BACKEND_PORT" "./$BACKEND_BIN" > backend/target/backend-run.log 2>&1 &
  BACKEND_PID=$!
  trap 'kill -9 $BACKEND_PID 2>/dev/null || true; set_auto_bundle_pref false' EXIT
  HEALTH=""
  for attempt in $(seq 1 60); do
    HEALTH="$(curl -s -m 1 "http://127.0.0.1:$BACKEND_PORT/healthz" || true)"
    if [ "$HEALTH" = "ok" ]; then
      break
    fi
    # The process dying is the failure this loop must not sit through for a
    # minute; kill -0 asks whether it is still there.
    if ! kill -0 $BACKEND_PID 2>/dev/null; then
      echo "the packaged backend exited before answering:" >&2
      cat backend/target/backend-run.log >&2
      exit 1
    fi
    sleep 1
  done
  # The route that goes through an injected @Service, while the binary still runs.
  GREETING="$(curl -s --max-time 5 "http://127.0.0.1:$BACKEND_PORT/greet/archetype" || true)"
  kill -9 $BACKEND_PID 2>/dev/null || true
  trap 'set_auto_bundle_pref false' EXIT
  if [ "$HEALTH" = "ok" ] && [ "$GREETING" != "Hello, archetype" ]; then
    echo "the packaged backend did not answer /greet through its injected service (got '$GREETING'):" >&2
    cat backend/target/backend-run.log >&2
    exit 1
  fi
  if [ "$HEALTH" != "ok" ]; then
    echo "the packaged backend did not answer /healthz with ok (got '$HEALTH'):" >&2
    cat backend/target/backend-run.log >&2
    exit 1
  fi
  echo "the packaged backend answered /healthz"
fi
