#!/bin/bash
# A built Codename One application on the REAL JavaSE port, under real input.
#
#   run.sh <application's common dir> <input script> [output dir]
#
# The application is the staged project of desktop-compat-test.sh (or any
# Codename One project whose common module is built): its classes are run in a
# frame set up as the generated desktop stub sets one up, and RealPortDriver
# plays the script against it with the display server's own pointer and keys.
#
# It fails when anything threw, or when a step of input changed no pixel. That
# second one is the point: the layers' unit tests fire events at the node they
# are meant for, and an application whose buttons were dead under a real
# pointer passed every one of them. See RealPortDriver.java for the script.
#
# Needs a display -- run it under xvfb-run where there is none -- and a JDK 11
# or newer, which is what the port itself needs at run time: REALPORT_JAVA_HOME,
# then GRADLE_JAVA_HOME, JAVA17_HOME, JAVA_HOME. The framework jars come from
# the local Maven repository (MAVEN_REPO_LOCAL when the reactor was installed
# with -Dmaven.repo.local). SIZE=WIDTHxHEIGHT is the size the application's area
# is opened at; an application that sizes its own window, as both galleries do,
# then gets the size it asks for.
#
# With no arguments it runs the smoke scripts beside it against the samples
# desktop-compat-test.sh staged: <sample>.txt against build/desktop-compat/<sample>.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
IT="$SCRIPTPATH/.."
set -e
CN1_VERSION="${CN1_VERSION:-$(bash "$IT/../print-version.sh")}"
REPO="${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}"
JDK="${REALPORT_JAVA_HOME:-${GRADLE_JAVA_HOME:-${JAVA17_HOME:-$JAVA_HOME}}}"
SIZE="${SIZE:-1000x800}"

run_one() {
  local common="$1" script="$2" out="$3"
  local classes="$common/target/classes"
  [ -d "$classes" ] || { echo "FAIL: $classes: the application is not built"; return 1; }
  local settings="$common/codenameone_settings.properties"
  local main pkg
  main=$(sed -n 's/^codename1.mainName=//p' "$settings" | tr -d '\r')
  pkg=$(sed -n 's/^codename1.packageName=//p' "$settings" | tr -d '\r')
  [ -n "$main" ] && [ -n "$pkg" ] || { echo "FAIL: $settings names no main class"; return 1; }
  local cp="$classes"
  local artifact jar
  # project-model: the port reads the project's layout through it when it opens the theme.
  for artifact in codenameone-core codenameone-javase codenameone-project-model; do
    jar="$REPO/com/codenameone/$artifact/$CN1_VERSION/$artifact-$CN1_VERSION.jar"
    [ -f "$jar" ] || { echo "FAIL: $jar is not installed"; return 1; }
    cp="$cp:$jar"
  done
  mkdir -p "$out/driver"
  "$JDK/bin/javac" -nowarn -d "$out/driver" -cp "$cp" "$SCRIPTPATH/RealPortDriver.java"
  # The port keeps its preferences and the application's storage under the home directory.
  mkdir -p "$out/home"
  (cd "$common" && "$JDK/bin/java" -Xmx1g "-Duser.home=$out/home" -cp "$out/driver:$cp" RealPortDriver \
      "$pkg.$main" "${SIZE%x*}" "${SIZE#*x}" "$script" "$out/frames") > "$out/realport.log" 2>&1 \
    || { tail -60 "$out/realport.log"; echo "FAIL: $(basename "$script") against $common"; return 1; }
  grep "^REALPORT" "$out/realport.log"
}

if [ $# -ge 2 ]; then
  run_one "$1" "$2" "${3:-$IT/build/desktop-compat-realport/$(basename "$2" .txt)}"
  exit $?
fi

STAGED="${CN1_COMPAT_WORKDIR:-$IT/build}/desktop-compat"
FAILED=0
RAN=0
for script in "$SCRIPTPATH"/*.txt; do
  sample=$(basename "$script" .txt)
  if [ ! -d "$STAGED/$sample/app/common" ]; then
    echo "FAIL: $sample is not staged under $STAGED; run desktop-compat-test.sh first"
    FAILED=1
    continue
  fi
  echo "== $sample on the real port"
  run_one "$STAGED/$sample/app/common" "$script" "$IT/build/desktop-compat-realport/$sample" || FAILED=1
  RAN=$((RAN + 1))
done
[ "$RAN" -gt 0 ] || { echo "FAIL: no smoke script ran"; exit 1; }
exit $FAILED
