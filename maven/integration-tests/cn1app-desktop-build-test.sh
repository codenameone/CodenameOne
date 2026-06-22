#!/bin/bash
# Regression test for the maven-jar-plugin "double-attach" failure that broke
# desktop (and every other) `cn1:build` on Maven 3.9+.
#
# Root cause: the CN1 `build` goal used to declare @Execute(phase=package) AND
# is bound to the package phase by the project poms, so invoking it forked a
# *second* package lifecycle. The per-platform modules (javase/ios/android/...)
# carry no classes of their own -- the app code lives in `common` -- so their
# default jar is empty. On Maven 3.9+ the forked run inherits the already
# attached empty main artifact and maven-jar-plugin aborts the build with:
#
#   You have to use a classifier to attach supplemental artifacts to the
#   project instead of replacing them.
#
# Fix: drop @Execute(phase=package) from CN1BuildMojo. The goal is always bound
# to the package phase anyway, so package still runs before it -- just once,
# with no fork and no double-attach.
#
# This test reproduces the exact lifecycle offline, WITHOUT a cloud submission
# or any platform SDK:
#   * It MUST use the project's `mvnw` (Maven 3.9.9). System `mvn` may be an
#     older release that never reproduces the bug, masking regressions.
#   * It uses a deliberately-unsupported `local-*` build target. The package
#     phase (where the double-attach happened) runs first; the build mojo then
#     throws "Build target not supported" *before* contacting any server. So the
#     `mvnw` invocation is EXPECTED to exit non-zero -- we assert on the log
#     contents, not the exit code.
#
# PASS  -> no classifier double-attach error, the package phase ran exactly
#          once (proving the @Execute fork is gone), and the build goal was
#          reached.
# FAIL  -> the classifier error is back, OR the package phase ran twice (the
#          @Execute fork was reintroduced).
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source $SCRIPTPATH/inc/env.sh

PROBE_TARGET="local-desktop-build-probe"
# Per-module package marker: the javase module's generate-javase-sources
# execution (id=add-se-sources) runs once per package lifecycle. With the
# @Execute(phase=package) fork it ran twice; without it, exactly once.
PACKAGE_MARKER="generate-javase-sources (add-se-sources)"

cd $SCRIPTPATH/build
if [ -d myappdesktop ]; then
  rm -rf myappdesktop
fi
mvn archetype:generate \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion=$CN1_VERSION \
  -DartifactId=myappdesktop \
  -DgroupId=com.example \
  -Dversion=1.0-SNAPSHOT \
  -DmainName=MyApp \
  -DinteractiveMode=false

cd myappdesktop
chmod 755 mvnw

LOG=$SCRIPTPATH/build/myappdesktop-desktop-build.log
# Run the desktop build lifecycle. Use the bundled wrapper so we are guaranteed
# to exercise the Maven 3.9+ behaviour that triggered the bug. Expected to fail
# at "Build target not supported" -- that is offline and after the package
# phase, so disable `set -e` around it and inspect the log instead.
set +e
./mvnw -B -DskipTests=true \
  -Dcodename1.platform=javase \
  -Dcodename1.buildTarget=$PROBE_TARGET \
  package 2>&1 | tee "$LOG"
set -e

if grep -q "You have to use a classifier to attach supplemental artifacts" "$LOG"; then
  echo "FAIL: maven-jar-plugin double-attach regression -- 'cn1:build' aborts at jar:jar."
  echo "      CN1BuildMojo must NOT declare @Execute(phase=package) (it forks a"
  echo "      redundant second package run that double-attaches the empty jar)."
  exit 1
fi

PACKAGE_RUNS=$(grep -c -F "$PACKAGE_MARKER" "$LOG")
if [ "$PACKAGE_RUNS" -gt 1 ]; then
  echo "FAIL: the package phase ran $PACKAGE_RUNS times -- the @Execute(phase=package)"
  echo "      fork has been reintroduced on the build goal. It must be removed so"
  echo "      package runs once and the empty per-module jar is not attached twice."
  exit 1
fi
if [ "$PACKAGE_RUNS" -lt 1 ]; then
  echo "FAIL: the package phase did not run ('$PACKAGE_MARKER' not found), so this"
  echo "      test is no longer exercising the build lifecycle. Inspect $LOG."
  exit 1
fi

# Confirm we actually reached the build mojo (proves package completed without
# the double-attach abort and control passed to the build goal).
if ! grep -q "Build target not supported $PROBE_TARGET" "$LOG"; then
  echo "FAIL: build did not reach the build goal as expected; the lifecycle may"
  echo "      have failed earlier for an unrelated reason. Inspect $LOG."
  exit 1
fi

echo "PASS: desktop 'cn1:build' runs the package phase once, does not double-attach,"
echo "      and reaches the build goal."
