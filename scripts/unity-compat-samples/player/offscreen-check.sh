#!/bin/bash
# Runs the desktop player of a Unity project with no window, and checks what
# a window would have shown and done:
#
#   offscreen-check.sh <out dir of build-unity-project.sh> [OffscreenCheck options]
#
# The options are offscreen.OffscreenCheck's (player/offscreen/java), which
# says what is real in this run and what is not:
#
#   [--seed n] [--frames n] [--size WxH] [--input file] [--dump f1,f2,...]
#   [--png dir] [--trace file] [--count Prefix]... [--reference dir]
#
# It opens nothing: the JVM is headless. It is not part of the framework's
# build, because it needs a project built with the .NET SDK; run it by hand
# or from a job that has one, after build-unity-project.sh.
#
# Needs what run-unity-project.sh needs, and the core unit tests' classes,
# whose implementation of the platform this drives:
#
#   cd maven && mvn test-compile -DunitTests -pl core-unittests
set -e
HERE="$( cd "$(dirname "$0")" ; pwd -P )"
ROOT="$( cd "$HERE/../../.." ; pwd -P )"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

[ $# -ge 1 ] || { sed -n '2,12p' "$0" >&2; exit 2; }
OUT="$( cd "$1" ; pwd -P )"
shift

# Compiles the player, and names the JDK and the class path it runs on.
COMMAND="$("$HERE/../run-unity-project.sh" "$OUT" --print)"
JAVA="${COMMAND%% *}"
[ -x "$JAVA" ] || fail "run-unity-project.sh --print did not start with a java executable: $COMMAND"
JAVASE="$(ls "$ROOT"/maven/javase/target/codenameone-javase-*-jar-with-dependencies.jar | head -1)"
TESTS="$ROOT/maven/core-unittests/target/test-classes"
CORE="$ROOT/maven/core/target/classes"
FACTORY="$ROOT/maven/factory/target/classes"
[ -e "$TESTS/com/codename1/testing/TestCodenameOneImplementation.class" ] \
  || fail "the core unit tests are not compiled. Compile them first: (cd maven && mvn test-compile -DunitTests -pl core-unittests)"
[ -e "$FACTORY/com/codename1/impl/ImplementationFactory.class" ] || fail "maven/factory is not built"

# The core and the factory in front of the port's jar, which has its own of
# both: of that jar only the software rasteriser and the fonts are wanted.
CP="$OUT/player-classes:$OUT/classes:$OUT/resources:$ROOT/maven/unity-compat/target/classes:$TESTS:$CORE:$FACTORY:$JAVASE"
rm -rf "$OUT/offscreen-classes"
mkdir -p "$OUT/offscreen-classes"
find "$HERE/offscreen/java" -name '*.java' > "$OUT/offscreen.sources"
"$(dirname "$JAVA")/javac" -nowarn -Xlint:-options -encoding ascii -d "$OUT/offscreen-classes" -cp "$CP" \
  "@$OUT/offscreen.sources" > "$OUT/offscreen-javac.log" 2>&1 \
  || { tail -40 "$OUT/offscreen-javac.log" >&2; fail "javac of the offscreen check (log: $OUT/offscreen-javac.log)"; }
exec "$JAVA" -Djava.awt.headless=true -cp "$OUT/offscreen-classes:$CP" offscreen.OffscreenCheck "$@"
