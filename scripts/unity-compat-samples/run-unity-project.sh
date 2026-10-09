#!/bin/bash
# Plays a Unity project built by build-unity-project.sh in a desktop window,
# on the Codename One JavaSE port -- a plain desktop application, not the
# skinned simulator:
#
#   run-unity-project.sh <out dir of build-unity-project.sh> [--print] [--software]
#       [--title text] [--size WxH] [--seed n]
#
#   --print     compile the player and print the java command, without running it
#   --software  draw with the port's software rasteriser and not OpenGL: for a
#               machine where the window stays black or the OpenGL binding fails
#   --title     the window title; without it the project's productName
#   --size      the size of the game inside the window, 960x540 unless given
#   --seed      the seed of UnityEngine.Random, for a run that can be repeated
#
# The player is scripts/unity-compat-samples/player/java: player.UnityPlayer, a
# Codename One application that shows the project's first scene in a
# UnityGameView, and player.UnityPlayerDesktop, the main that opens the window.
# It is compiled into <out dir>/player-classes.
#
# Needs a JDK 11 to 25 to run on. One is looked for, in this order, in
# UNITY_PLAYER_JAVA_HOME, JAVA17_HOME (tools/env.sh is read for it when it is
# not set), JAVA_HOME, the PATH and, on macOS, /usr/libexec/java_home. It also
# needs the JavaSE port and the unity-compat runtime built:
#
#   cd maven && mvn install -Plocal-dev-javase -Dunity-compat -DskipTests \
#       -pl core,javase,cil-translator,unity-compat
set -e
HERE="$( cd "$(dirname "$0")" ; pwd -P )"
ROOT="$( cd "$HERE/../.." ; pwd -P )"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

usage() {
  sed -n '2,13p' "$0" >&2
  exit 2
}

[ $# -ge 1 ] || usage
[ -d "$1" ] || fail "$1 is not a directory"
OUT="$( cd "$1" ; pwd -P )"
shift
PRINT=0
SOFTWARE=0
TITLE=""
SIZE="960x540"
SEED=""
while [ $# -gt 0 ]; do
  case "$1" in
    --print) PRINT=1; shift ;;
    --software) SOFTWARE=1; shift ;;
    --title) [ $# -ge 2 ] || usage; TITLE="$2"; shift 2 ;;
    --size) [ $# -ge 2 ] || usage; SIZE="$2"; shift 2 ;;
    --seed) [ $# -ge 2 ] || usage; SEED="$2"; shift 2 ;;
    *) usage ;;
  esac
done
case "$SIZE" in
  *[!0-9x]*|x*|*x|*x*x*) fail "--size is WxH, as in 960x540, not $SIZE" ;;
  *x*) ;;
  *) fail "--size is WxH, as in 960x540, not $SIZE" ;;
esac
WIDTH="${SIZE%x*}"
HEIGHT="${SIZE#*x}"
case "$SEED" in *[!0-9-]*) fail "--seed is a number, not $SEED" ;; esac

[ -f "$OUT/classes/com/codename1/generated/unity/UnityAppImpl.class" ] \
  || fail "$OUT was not built by build-unity-project.sh: it has no classes/com/codename1/generated/unity/UnityAppImpl.class"

RUNTIME="$ROOT/maven/unity-compat/target/classes"
BUILD="(cd maven && mvn install -Plocal-dev-javase -Dunity-compat -DskipTests -pl core,javase,cil-translator,unity-compat)"
[ -e "$RUNTIME/com/codename1/unitycompat/unityengine/ui/UnityGameView.class" ] \
  || fail "the unity-compat runtime is not built. Build it first: $BUILD"
# The port with everything it needs inside it, the core included: one jar,
# built in one go, so the port and the core it runs on are the same build.
JAVASE="$(ls "$ROOT"/maven/javase/target/codenameone-javase-*-jar-with-dependencies.jar 2> /dev/null | head -1)"
[ -n "$JAVASE" ] || fail "the JavaSE port is not built: no maven/javase/target/codenameone-javase-*-jar-with-dependencies.jar. Build it first: $BUILD"

# java_major <java home>: the major version of the JDK there, or nothing.
java_major() {
  [ -n "$1" ] && [ -x "$1/bin/java" ] && [ -x "$1/bin/javac" ] || return 0
  "$1/bin/java" -version 2>&1 | sed -n '1s/.*version "\([0-9]*\)\.\{0,1\}\([0-9]*\).*/\1 \2/p' | {
    read -r major minor || true
    # 1.8 is 8; everything since is its first number.
    if [ "$major" = 1 ]; then echo "$minor"; else echo "$major"; fi
  }
}

CANDIDATES=()
SEEN=""
candidate() {
  [ -n "$1" ] || return 0
  CANDIDATES+=("$1")
}
candidate "${UNITY_PLAYER_JAVA_HOME:-}"
if [ -z "${JAVA17_HOME:-}" ] && [ -f "$ROOT/tools/env.sh" ]; then
  # In a subshell: env.sh also points JAVA_HOME and the PATH at JDK 8.
  candidate "$( . "$ROOT/tools/env.sh" > /dev/null 2>&1; echo "${JAVA17_HOME:-}" )"
fi
candidate "${JAVA17_HOME:-}"
candidate "${JAVA_HOME:-}"
if command -v java > /dev/null 2>&1; then
  candidate "$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.home = //p' | head -1)"
fi
if [ -x /usr/libexec/java_home ]; then
  for v in 21 17 25 11; do
    candidate "$(/usr/libexec/java_home -v "$v" 2> /dev/null || true)"
  done
fi
JDK=""
for c in "${CANDIDATES[@]}"; do
  major="$(java_major "$c")"
  SEEN="$SEEN
  $c (${major:-not a JDK})"
  # java_home -v answers with a newer JDK when it has not the one asked for.
  if [ -n "$major" ] && [ "$major" -ge 11 ] && [ "$major" -le 25 ]; then
    JDK="$c"
    break
  fi
done
[ -n "$JDK" ] || fail "no JDK 11 to 25 was found to run the JavaSE port on. Set UNITY_PLAYER_JAVA_HOME to one. Looked at:$SEEN"

PLAYER="$OUT/player-classes"
rm -rf "$PLAYER"
mkdir -p "$PLAYER"
CP="$OUT/classes:$OUT/resources:$RUNTIME:$JAVASE"
find "$HERE/player/java" -name '*.java' > "$OUT/player.sources"
"$JDK/bin/javac" -nowarn -Xlint:-options -encoding ascii -d "$PLAYER" -cp "$CP" "@$OUT/player.sources" > "$OUT/player-javac.log" 2>&1 \
  || { tail -40 "$OUT/player-javac.log" >&2; fail "javac of the player (log: $OUT/player-javac.log)"; }
CP="$PLAYER:$CP"
if [ "$SOFTWARE" = 1 ]; then
  mkdir -p "$OUT/player-software"
  "$JDK/bin/javac" -nowarn -encoding ascii -d "$OUT/player-software" \
    "$HERE/player/software/com/codename1/impl/javase/JavaSEJoglSurface.java" > "$OUT/player-javac.log" 2>&1 \
    || { tail -40 "$OUT/player-javac.log" >&2; fail "javac of the software switch (log: $OUT/player-javac.log)"; }
  # In front of the port's jar, so that it is the class the port finds.
  CP="$OUT/player-software:$CP"
fi

if [ -z "$TITLE" ]; then
  # The build keeps no project settings, but its .csproj names the project.
  PROJECT="$(sed -n 's|.*<Compile Include="\(.*\)/Assets/\*\*/\*\.cs".*|\1|p' "$OUT/cs/Assembly-CSharp.csproj" 2> /dev/null | head -1)"
  if [ -n "$PROJECT" ] && [ -f "$PROJECT/ProjectSettings/ProjectSettings.asset" ]; then
    TITLE="$(sed -n 's/^ *productName: *//p' "$PROJECT/ProjectSettings/ProjectSettings.asset" | head -1 | tr -d '\r')"
  fi
  [ -n "$TITLE" ] || TITLE="Unity Player"
fi

CMD=("$JDK/bin/java")
if [ "$(uname -s)" = Darwin ]; then
  CMD+=("-Xdock:name=$TITLE" "-Dapple.awt.application.name=$TITLE"
        "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED"
        "--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED")
fi
CMD+=("-Dsun.awt.application.name=$TITLE" "-Dunity.player.title=$TITLE"
      "-Dunity.player.width=$WIDTH" "-Dunity.player.height=$HEIGHT")
[ -z "$SEED" ] || CMD+=("-Dunity.player.seed=$SEED")
CMD+=(-cp "$CP" player.UnityPlayerDesktop)

if [ "$PRINT" = 1 ]; then
  printf '%q ' "${CMD[@]}"
  echo
  exit 0
fi
exec "${CMD[@]}"
