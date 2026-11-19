#!/usr/bin/env bash
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of Codename One.
#
# Builds one hero application the way a Codename One developer builds theirs:
# a project from the cn1app archetype, `cn1:import-python-project` over the
# application's directory (its requirements come from PyPI, at the pins in
# requirements.txt), then `mvn package` for the native desktop target of the
# host. Nothing here calls the builder directly or links anything by hand, so
# the binary measured is the binary a user's build produces.
#
#   build_app.sh <llm-mobile|markitdown-mobile> <work dir> <maven repo>
#
# The Maven repository is the one the framework was installed into from this
# checkout (8.0-SNAPSHOT); never ~/.m2, which other checkouts overwrite.
# Prints "app=<path>" on success: the executable on Linux, the .app on macOS.
set -euo pipefail

APP="$1"
WORK="$2"
REPO="$3"
HERE="$(cd "$(dirname "$0")" && pwd)"
SRC="$HERE/../$APP"
[ -f "$SRC/requirements.txt" ] || { echo "no application $SRC" >&2; exit 2; }
: "${JAVA17_HOME:?JAVA17_HOME must name a JDK 17}"
export JAVA_HOME="$JAVA17_HOME"
export MAVEN_OPTS="${MAVEN_OPTS:+$MAVEN_OPTS }-Xmx6g -Dmaven.repo.local=$REPO"

ARTIFACT="$(echo "$APP" | tr -d -)"
PROJECT="$WORK/$ARTIFACT"
LOG="$WORK/$ARTIFACT-build.log"
mkdir -p "$WORK"
if [ -e "$PROJECT" ]; then
  echo "$PROJECT already exists; give a fresh work directory" >&2
  exit 2
fi

( cd "$WORK" && mvn -B -q archetype:generate \
    -DarchetypeGroupId=com.codenameone -DarchetypeArtifactId=cn1app-archetype \
    -DarchetypeVersion=8.0-SNAPSHOT -DgroupId=com.codenameone.heroes \
    -DartifactId="$ARTIFACT" -Dversion=1.0-SNAPSHOT -DmainName=HeroApp \
    -DinteractiveMode=false ) > "$LOG" 2>&1 || { tail -40 "$LOG" >&2; exit 1; }

( cd "$PROJECT" && ./mvnw -B cn1:import-python-project -Dsource="$SRC" ) >> "$LOG" 2>&1 \
  || { tail -40 "$LOG" >&2; exit 1; }

case "$(uname -s)" in
  Linux)
    ( cd "$PROJECT" && ./mvnw -B package -DskipTests \
        -Dcodename1.platform=linux -Dcodename1.buildTarget=local-linux-device ) >> "$LOG" 2>&1 \
      || { grep -E "ERROR|error:" "$LOG" | tail -40 >&2; exit 1; }
    BIN="$(sed -n 's/.*Built native Linux executable: //p' "$LOG" | tail -1 | tr -d '\r')"
    [ -n "$BIN" ] && [ -x "$BIN" ] || { echo "the build reported no executable" >&2; exit 1; }
    echo "app=$BIN"
    ;;
  Darwin)
    ( cd "$PROJECT" && ./mvnw -B package -DskipTests -Dopen=false \
        -Dcodename1.platform=ios -Dcodename1.buildTarget=mac-source ) >> "$LOG" 2>&1 \
      || { grep -E "ERROR|error:" "$LOG" | tail -40 >&2; exit 1; }
    # The project the build hands over, beside the common module's jar.
    XCPROJ="$(find "$PROJECT/common/target" -maxdepth 1 -type d -name '*-mac-source' | head -1)"
    [ -d "$XCPROJ" ] || { echo "the build generated no Xcode project" >&2; exit 1; }
    # Release and stripped, as an archive build ships it.
    ( cd "$XCPROJ" && xcodebuild -project ./*.xcodeproj -scheme "$(basename "$XCPROJ"/*.xcodeproj .xcodeproj)" \
        -configuration Release CODE_SIGNING_ALLOWED=NO \
        DEPLOYMENT_POSTPROCESSING=YES STRIP_INSTALLED_PRODUCT=YES STRIP_STYLE=all \
        CONFIGURATION_BUILD_DIR="$XCPROJ/build" build ) >> "$LOG" 2>&1 \
      || { grep -E "error:|BUILD FAILED" "$LOG" | tail -40 >&2; exit 1; }
    BUNDLE="$(find "$XCPROJ/build" -maxdepth 1 -name '*.app' | head -1)"
    [ -d "$BUNDLE" ] || { echo "xcodebuild produced no .app" >&2; exit 1; }
    echo "app=$BUNDLE"
    ;;
  *)
    echo "no native desktop target for $(uname -s)" >&2
    exit 2
    ;;
esac
