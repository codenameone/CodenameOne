#!/bin/bash
# The Unity compatibility samples (scripts/unity-compat-samples) built from
# their C# and run, with every trace compared against the one committed beside
# the sample:
#
#   console   a program that exercises the CIL translator one language feature
#             at a time; expected-output.txt is what `dotnet run` prints.
#   scene2d   a Unity project -- scripts, a .unity scene, sprites -- stepped for
#             150 frames with no display; expected-trace.txt is what a JVM
#             prints.
#   arcade2d  a Unity project with project settings, built the way any project
#             is (build-unity-project.sh) and run by the generic headless
#             driver with a scripted keyboard: a prefab instantiated and
#             destroyed, objects parented at run time, a trigger, concave
#             polygon colliders, a sprite sheet, a built-in sprite, UI text
#             under a canvas scaler, and arrays and lists in script fields.
#   query2d   a Unity project that asks the physics world questions: a ray, a
#             line, a circle, a box and a capsule cast against a box, a
#             circle, two capsules, an edge and a concave polygon; the overlap
#             tests; layer masks named in the tag settings; triggers, depth
#             ranges and contact filters; a collider moved behind physics'
#             back. Then scripted fingers: two at once, a tap between two
#             frames, a double tap, and a mouse with its two axes.
#   platformer2d  a Unity project of the kind a platformer is: an Animator
#             with a float, an int, a bool and a trigger, exit times, a tint
#             and a child's scale on curves, and animation events; a Tilemap
#             read and changed by a script, its cells under one composite
#             outline that a box slides along at an unchanged speed; a tile
#             solid only in its top quarter; a one-way platform met from
#             below; a particle burst; TextMesh Pro text on a canvas and in
#             the world; and a Cinemachine camera that follows.
#   menu2d    a Unity project that starts behind a title panel: a button whose
#             click list is the editor's (a method, a method with an int, and
#             SetActive on an object) beside a listener added in code, clicked
#             with a scripted pointer through an event system; images, one of
#             them filled; a scaled rectangle; prefab instances placed in the
#             scene with overrides; a polygon sprite; a physics material; an
#             audio clip, whose plays are lines of the trace; PlayerPrefs;
#             C# events with a tuple payload, LINQ over an array, a switch
#             expression, Enum.IsDefined; a coroutine waiting through
#             Time.timeScale = 0; and a board read with Resources.Load from
#             a text file into an array of two dimensions, with a sprite and
#             a .bytes file loaded the same way, a TextAsset field, a polygon
#             sprite cut to a triangle, arrays the scene writes as hexadecimal
#             and the two camera layers an old project carries.
#   host2d    a Unity project with a Java side, as an application has: the
#             host hands the scripts an object before the first scene is
#             built and calls a script from outside the frame; an object
#             kept with DontDestroyOnLoad, and its coroutine, across two
#             scene loads; OnMouseDown and OnMouseUp on a collider;
#             OnApplicationPause and OnApplicationFocus. Its first scene has a
#             slider of Unity's UI package and a script whose file is gone,
#             and a script declares a message nothing sends: the build
#             succeeds and says so, and the four warnings are held to their
#             text.
#
# Each is compiled by the C# compiler, translated to class files by
# maven/cil-translator and run on the JVM against the runtime jar's classes
# (maven/unity-compat). With --parparvm the same class files also go through
# ParparVM's `clean` C target, built to a native executable, and through its
# `javascript` target, run under Node, and those traces must be the same lines
# again: the point of translating to bytecode is that every Codename One target
# then runs it, so a difference between targets is a failure here.
#
# Needs the .NET SDK (DOTNET names the CLI, else DOTNET_ROOT, else the PATH) and
# the unity-compat profile built:
#
#   cd maven && mvn install -Plocal-dev-javase -Dunity-compat -DskipTests \
#       -pl core,cil-translator,unity-compat
#
# --parparvm also needs vm/JavaAPI and vm/ByteCodeTranslator packaged from the
# current sources, plus cmake, a C compiler, python3 and node.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source "$SCRIPTPATH/inc/env.sh"
ROOT="$( cd "$SCRIPTPATH/../.." ; pwd -P )"
SAMPLES="$ROOT/scripts/unity-compat-samples"

PARPARVM=0
for arg in "$@"; do
  case "$arg" in
    --parparvm) PARPARVM=1 ;;
    *) echo "usage: $(basename "$0") [--parparvm]" >&2; exit 2 ;;
  esac
done

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

# need <path> <what it is> <how to build it>
need() {
  [ -e "$1" ] || fail "$2 is missing ($1). Build it first: $3"
}

if [ -n "${DOTNET:-}" ]; then
  [ -x "$DOTNET" ] || fail "DOTNET names $DOTNET, which is not an executable"
elif [ -n "${DOTNET_ROOT:-}" ] && [ -x "$DOTNET_ROOT/dotnet" ]; then
  DOTNET="$DOTNET_ROOT/dotnet"
else
  DOTNET="$(command -v dotnet || true)"
fi
[ -n "$DOTNET" ] || fail "the .NET SDK was not found: set DOTNET to the dotnet executable, or DOTNET_ROOT, or put dotnet on the PATH"
export DOTNET_CLI_TELEMETRY_OPTOUT=1 DOTNET_NOLOGO=1

UNITY_BUILD="(cd maven && mvn install -Plocal-dev-javase -Dunity-compat -DskipTests -pl core,cil-translator,unity-compat)"
CIL_CLASSES="$ROOT/maven/cil-translator/target/classes"
CIL_DEPS="$ROOT/maven/cil-translator/target/dependency"
RUNTIME="$ROOT/maven/unity-compat/target/classes"
RUNTIME_SRC="$ROOT/maven/unity-compat/src/main/java"
CORE="$ROOT/maven/core/target/classes"
need "$CIL_CLASSES/com/codename1/cil/translate/Translator.class" "the CIL translator" "$UNITY_BUILD"
ls "$CIL_DEPS"/asm-*.jar > /dev/null 2>&1 || fail "ASM is not in $CIL_DEPS. Build it first: $UNITY_BUILD"
need "$RUNTIME/com/codename1/unitycompat/unityengine/UnityRuntime.class" "the Unity runtime" "$UNITY_BUILD"
need "$RUNTIME/UnityEngine/Vector2.class" "the translated UnityEngine value types" "$UNITY_BUILD"
need "$CORE/com/codename1/gaming/physics/box2d/dynamics/World.class" "the Codename One core" "$UNITY_BUILD"
CIL="$CIL_CLASSES:$(ls "$CIL_DEPS"/*.jar | tr '\n' ':')"

WORKDIR="$SCRIPTPATH/build/unity-compat"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"
FAILED=0

# run_logged <log> <what> <command...>: quiet unless it fails.
run_logged() {
  local log="$1" what="$2"
  shift 2
  "$@" > "$log" 2>&1 || { tail -40 "$log"; fail "$what (log: $log)"; }
}

# compare <label> <expected file> <actual file>
compare() {
  if diff "$2" "$3" > "$3.diff"; then
    echo "   $1: $(wc -l < "$3" | tr -d ' ') lines, identical to $(basename "$2")"
  else
    echo "FAIL: $1 differs from $2 (< expected, > actual):"
    head -40 "$3.diff"
    FAILED=1
  fi
}

# ---------------------------------------------------------------- C# and JVM
echo "== C#"
run_logged "$WORKDIR/dotnet-console.log" "dotnet build of the console sample" \
  "$DOTNET" build -c Release --nologo "$SAMPLES/console/Console.csproj"
# Builds the reference UnityEngine and the value types through its references.
run_logged "$WORKDIR/dotnet-scene2d.log" "dotnet build of the scene2d sample" \
  "$DOTNET" build -c Release --nologo "$SAMPLES/scene2d/Scene2d.csproj"
CSHARP="$ROOT/maven/unity-compat/src/main/csharp"
VALUES="$CSHARP/UnityEngine.Values/bin/Release/netstandard2.1/Codename1.UnityValues.dll"
NETSTANDARD="$CSHARP/UnityEngine.Values/bin/Release/netstandard2.1/netstandard-ref/netstandard.dll"
ENGINE="$CSHARP/UnityEngine/bin/Release/netstandard2.1/UnityEngine.dll"
CONSOLE_DLL="$SAMPLES/console/bin/Release/netstandard2.1/SpikeConsole.dll"
SCENE_DLL="$SAMPLES/scene2d/bin/Release/netstandard2.1/Assembly-CSharp.dll"
for f in "$VALUES" "$NETSTANDARD" "$ENGINE" "$CONSOLE_DLL" "$SCENE_DLL"; do
  [ -f "$f" ] || fail "the C# build produced no $f"
done

echo "== console"
C="$WORKDIR/console"
mkdir -p "$C"
run_logged "$C/translate.log" "console: translation" \
  java -cp "$CIL" com.codename1.cil.translate.Translator --out "$C/classes" --runtime "$RUNTIME" \
    --ref "$NETSTANDARD" "$CONSOLE_DLL"
java -Xverify:all -cp "$C/classes:$RUNTIME" Spike.Program > "$C/hotspot.txt" 2> "$C/hotspot.err" \
  || { cat "$C/hotspot.err"; fail "console: the translated program failed on the JVM"; }
compare "console on the JVM" "$SAMPLES/console/expected-output.txt" "$C/hotspot.txt"

echo "== scene2d"
SC="$WORKDIR/scene2d"
mkdir -p "$SC"
run_logged "$SC/translate.log" "scene2d: translation" \
  java -cp "$CIL" com.codename1.cil.translate.Translator --out "$SC/app" --runtime "$RUNTIME" \
    --ref "$ENGINE" --ref "$NETSTANDARD" "$VALUES" "$SCENE_DLL"
run_logged "$SC/scene-compiler.log" "scene2d: scene compiler" \
  java -cp "$CIL" com.codename1.unity.scenecompiler.SceneCompiler --project "$SAMPLES/scene2d" --out "$SC/gen" \
    --resources "$SC/res" --ref "$ENGINE" --ref "$NETSTANDARD" "$SCENE_DLL" "$VALUES"
[ -f "$SC/gen/com/codename1/generated/unity/UnityAppImpl.java" ] || fail "scene2d: the scene compiler wrote no UnityAppImpl.java"
mkdir -p "$SC/main"
find "$SC/gen" "$SAMPLES/scene2d/java" -name '*.java' > "$SC/main.sources"
run_logged "$SC/javac.log" "scene2d: javac of the generated scene" \
  javac -nowarn -source 8 -target 8 -d "$SC/main" -cp "$RUNTIME:$SC/app:$CORE" "@$SC/main.sources"
java -Xverify:all -cp "$SC/main:$SC/app:$RUNTIME:$CORE" scene.SceneMain > "$SC/hotspot.txt" 2> "$SC/hotspot.err" \
  || { cat "$SC/hotspot.err"; fail "scene2d: the scene failed on the JVM"; }
compare "scene2d on the JVM" "$SAMPLES/scene2d/expected-trace.txt" "$SC/hotspot.txt"

echo "== arcade2d"
AR="$WORKDIR/arcade2d"
ARCADE_ARGS=(--seed 3 --frames 300 --size 400x300 --input "$SAMPLES/arcade2d/input.txt" --dump 1,40,100,200 --count Shot)
export DOTNET
run_logged "$WORKDIR/arcade2d-build.log" "arcade2d: build-unity-project.sh" \
  "$SAMPLES/build-unity-project.sh" "$SAMPLES/arcade2d" "$AR" "$SAMPLES/headless/java"
# The sample is written to use only what is implemented: a warning here means
# the scene compiler stopped reading something it used to.
if grep '^warning:' "$WORKDIR/arcade2d-build.log"; then
  fail "arcade2d: the scene compiler warned"
fi
java -Xverify:all -Djava.awt.headless=true -cp "$AR/classes:$AR/resources:$RUNTIME:$CORE" headless.HeadlessMain \
  "${ARCADE_ARGS[@]}" > "$AR/hotspot.txt" 2> "$AR/hotspot.err" \
  || { cat "$AR/hotspot.err"; fail "arcade2d: the project failed on the JVM"; }
compare "arcade2d on the JVM" "$SAMPLES/arcade2d/expected-trace.txt" "$AR/hotspot.txt"

echo "== menu2d"
MN="$WORKDIR/menu2d"
MENU_ARGS=(--seed 3 --frames 240 --size 400x300 --input "$SAMPLES/menu2d/input.txt" --dump 1,42,60,200 --count Ledge)
rm -rf "$MN"
run_logged "$WORKDIR/menu2d-build.log" "menu2d: build-unity-project.sh" \
  "$SAMPLES/build-unity-project.sh" "$SAMPLES/menu2d" "$MN" "$SAMPLES/headless/java"
if grep '^warning:' "$WORKDIR/menu2d-build.log"; then
  fail "menu2d: the scene compiler warned"
fi
java -Xverify:all -Djava.awt.headless=true -cp "$MN/classes:$MN/resources:$RUNTIME:$CORE" headless.HeadlessMain \
  "${MENU_ARGS[@]}" > "$MN/hotspot.txt" 2> "$MN/hotspot.err" \
  || { cat "$MN/hotspot.err"; fail "menu2d: the project failed on the JVM"; }
compare "menu2d on the JVM" "$SAMPLES/menu2d/expected-trace.txt" "$MN/hotspot.txt"

echo "== query2d"
QR="$WORKDIR/query2d"
QUERY_ARGS=(--seed 3 --frames 150 --size 400x300 --input "$SAMPLES/query2d/input.txt")
rm -rf "$QR"
run_logged "$WORKDIR/query2d-build.log" "query2d: build-unity-project.sh" \
  "$SAMPLES/build-unity-project.sh" "$SAMPLES/query2d" "$QR" "$SAMPLES/headless/java"
if grep '^warning:' "$WORKDIR/query2d-build.log"; then
  fail "query2d: the scene compiler warned"
fi
java -Xverify:all -Djava.awt.headless=true -cp "$QR/classes:$QR/resources:$RUNTIME:$CORE" headless.HeadlessMain \
  "${QUERY_ARGS[@]}" > "$QR/hotspot.txt" 2> "$QR/hotspot.err" \
  || { cat "$QR/hotspot.err"; fail "query2d: the project failed on the JVM"; }
compare "query2d on the JVM" "$SAMPLES/query2d/expected-trace.txt" "$QR/hotspot.txt"

echo "== host2d"
HO="$WORKDIR/host2d"
HOST_ARGS=(--seed 3 --frames 130 --size 400x300 --input "$SAMPLES/host2d/input.txt" --count Keeper --host host.SampleHost)
rm -rf "$HO"
run_logged "$WORKDIR/host2d-build.log" "host2d: build-unity-project.sh" \
  "$SAMPLES/build-unity-project.sh" "$SAMPLES/host2d" "$HO" "$SAMPLES/headless/java" "$SAMPLES/host2d/java"
# What the build left out, it names: the object by its name, the component by
# what it is where that is known, and the script and line of a message that
# is never sent. These are the lines a developer reads, so they are held whole.
while IFS= read -r expected; do
  grep -qxF "$expected" "$WORKDIR/host2d-build.log" \
    || { echo "FAIL: host2d: the build did not print: $expected"; grep '^warning:' "$WORKDIR/host2d-build.log"; FAILED=1; }
done < "$SAMPLES/host2d/expected-warnings.txt"
[ "$(grep -c '^warning:' "$WORKDIR/host2d-build.log")" = "$(wc -l < "$SAMPLES/host2d/expected-warnings.txt" | tr -d ' ')" ] \
  || { echo "FAIL: host2d: the build printed warnings beside the expected ones:"; grep '^warning:' "$WORKDIR/host2d-build.log"; FAILED=1; }
java -Xverify:all -Djava.awt.headless=true -cp "$HO/classes:$HO/resources:$RUNTIME:$CORE" headless.HeadlessMain \
  "${HOST_ARGS[@]}" > "$HO/hotspot.txt" 2> "$HO/hotspot.err" \
  || { cat "$HO/hotspot.err"; fail "host2d: the project failed on the JVM"; }
compare "host2d on the JVM" "$SAMPLES/host2d/expected-trace.txt" "$HO/hotspot.txt"

# An animator with a parameter of each kind, exit times and events; a tilemap
# under one composite outline that a box slides along without catching a seam;
# a tile solid only in part; a one-way platform; a particle burst; TextMesh Pro
# text; a Cinemachine camera that follows.
echo "== platformer2d"
PF="$WORKDIR/platformer2d"
PLATFORMER_ARGS=(--seed 3 --frames 300 --size 400x300 --input "$SAMPLES/platformer2d/input.txt" --dump 1,130,200)
rm -rf "$PF"
run_logged "$WORKDIR/platformer2d-build.log" "platformer2d: build-unity-project.sh" \
  "$SAMPLES/build-unity-project.sh" "$SAMPLES/platformer2d" "$PF" "$SAMPLES/headless/java"
if grep '^warning:' "$WORKDIR/platformer2d-build.log"; then
  fail "platformer2d: the scene compiler warned"
fi
java -Xverify:all -Djava.awt.headless=true -cp "$PF/classes:$PF/resources:$RUNTIME:$CORE" headless.HeadlessMain \
  "${PLATFORMER_ARGS[@]}" > "$PF/hotspot.txt" 2> "$PF/hotspot.err" \
  || { cat "$PF/hotspot.err"; fail "platformer2d: the project failed on the JVM"; }
compare "platformer2d on the JVM" "$SAMPLES/platformer2d/expected-trace.txt" "$PF/hotspot.txt"

if [ $PARPARVM -eq 0 ]; then
  [ $FAILED -eq 0 ] || exit 1
  echo "unity-compat-test: OK (JVM only; --parparvm adds the native and JavaScript targets)"
  exit 0
fi

# ------------------------------------------------------------------ ParparVM
API="$ROOT/vm/JavaAPI/target/classes"
API_JAR="$ROOT/vm/JavaAPI/target/JavaAPI-1.0-SNAPSHOT.jar"
BCT_JAR="$ROOT/vm/ByteCodeTranslator/target/ByteCodeTranslator-1.0-SNAPSHOT.jar"
VM_BUILD="mvn -f vm/JavaAPI/pom.xml package -DskipTests && mvn -f vm/ByteCodeTranslator/pom.xml package -DskipTests"
need "$API/java/lang/Object.class" "ParparVM's class library (vm/JavaAPI)" "$VM_BUILD"
need "$API_JAR" "ParparVM's class library jar (vm/JavaAPI)" "$VM_BUILD"
need "$BCT_JAR" "the ParparVM translator (vm/ByteCodeTranslator)" "$VM_BUILD"
# The translator carries its C and JavaScript runtime as resources, so a jar
# older than the sources would translate with yesterday's VM and compare that.
stale=$(find "$ROOT/vm/ByteCodeTranslator/src" "$ROOT/vm/ByteCodeTranslator/pom.xml" -type f -newer "$BCT_JAR" | head -3)
[ -z "$stale" ] || fail "vm/ByteCodeTranslator has sources newer than its jar, for one $(echo "$stale" | head -1). Rebuild it: $VM_BUILD"
stale=$(find "$ROOT/vm/JavaAPI/src" "$ROOT/vm/JavaAPI/pom.xml" -type f -newer "$API_JAR" | head -3)
[ -z "$stale" ] || fail "vm/JavaAPI has sources newer than its jar, for one $(echo "$stale" | head -1). Rebuild it: $VM_BUILD"
for tool in cmake python3 node; do
  command -v $tool > /dev/null || fail "--parparvm needs $tool on the PATH"
done
JOBS=$(getconf _NPROCESSORS_ONLN 2> /dev/null || echo 4)
CMAKE_COMPILERS=()
if command -v clang > /dev/null; then
  CMAKE_COMPILERS=(-DCMAKE_C_COMPILER=clang -DCMAKE_CXX_COMPILER=clang++ -DCMAKE_OBJC_COMPILER=clang)
fi

# native_run <dir> <classes dir> <app name> <package> <trace file>
# The clean target emits a library; the samples are programs, so the generated
# CMakeLists is rewritten to an executable the way vm/tests does it.
native_run() {
  local dir="$1" classes="$2" name="$3" package="$4" trace="$5"
  rm -rf "$dir"
  mkdir -p "$dir"
  run_logged "$dir/translate.log" "$name: ParparVM translation to C" \
    java -cp "$BCT_JAR" com.codename1.tools.translator.ByteCodeTranslator clean "$classes" "$dir/out" \
      "$name" "$package" "$name" 1.0 ios none
  local dist="$dir/out/dist"
  python3 - "$dist/CMakeLists.txt" <<'PY'
import sys
p = sys.argv[1]
c = open(p).read()
at = c.index('add_library(${PROJECT_NAME}')
end = c.index(')', at)
c = (c[:at] + 'add_executable(' + c[at + len('add_library('):end + 1]
     + '\ntarget_link_libraries(${PROJECT_NAME} m)' + c[end + 1:])
open(p, 'w').write(c)
PY
  run_logged "$dir/cmake.log" "$name: cmake" \
    cmake -S "$dist" -B "$dist/build" -DCMAKE_BUILD_TYPE=Release "${CMAKE_COMPILERS[@]}"
  run_logged "$dir/build.log" "$name: native build" cmake --build "$dist/build" -j "$JOBS"
  [ -x "$dist/build/$name" ] || fail "$name: the native build produced no $dist/build/$name"
  "$dist/build/$name" > "$trace" 2> "$trace.err" || { tail -20 "$trace.err"; fail "$name: the native executable failed"; }
}

# js_run <dir> <classes dir> <main class, dots as underscores> <trace file>
js_run() {
  local dir="$1" classes="$2" main="$3" trace="$4"
  rm -rf "$dir"
  mkdir -p "$dir"
  # A copy: the two targets must not see what the other did to the directory.
  cp -R "$classes" "$dir/classes"
  run_logged "$dir/translate.log" "$main: ParparVM translation to JavaScript" \
    java -cp "$BCT_JAR" com.codename1.tools.translator.ByteCodeTranslator javascript "$dir/classes" "$dir/out" \
      "$main" com.example.unity "$main" 1.0 ios none
  node "$ROOT/vm/selfhost/js/run-program.js" "$dir/out/dist/$main-js" > "$trace.raw" 2> "$trace.err" \
    || { tail -20 "$trace.err"; fail "$main: the JavaScript program failed under node"; }
  # The runtime's own lifecycle markers share stdout with the program.
  grep -v '^PARPAR-LIFECYCLE:' "$trace.raw" > "$trace" || true
}

echo "== console through ParparVM"
PC="$WORKDIR/console-parparvm"
mkdir -p "$PC/classes"
# The base library again, this time against the device class library instead
# of the JDK's: what compiles here is what a device can link.
find "$RUNTIME_SRC/com/codename1/unitycompat/system" -name '*.java' > "$PC/runtime.sources"
run_logged "$PC/javac.log" "console: javac of the base library against vm/JavaAPI" \
  javac -nowarn -source 8 -target 8 -bootclasspath "$API" -Xlint:-options -d "$PC/classes" "@$PC/runtime.sources"
cp -R "$C/classes/." "$PC/classes/"
cp -R "$API/." "$PC/classes/"
native_run "$PC/native" "$PC/classes" SpikeConsole com.example.spike "$PC/native.txt"
compare "console, native" "$SAMPLES/console/expected-output.txt" "$PC/native.txt"
js_run "$PC/js" "$PC/classes" Spike_Program "$PC/js.txt"
compare "console, JavaScript" "$SAMPLES/console/expected-output.txt" "$PC/js.txt"

echo "== scene2d through ParparVM"
PS="$WORKDIR/scene2d-parparvm"
mkdir -p "$PS/classes" "$PS/box/com/codename1/gaming/physics"
# Of the core, only the physics engine: it is plain Java, and the rest of the
# core needs a port under it. That is also why the classes that reach it --
# the view and the other UnityGame* classes beside it, and the application
# shell -- stay out.
cp -R "$CORE/com/codename1/gaming/physics/box2d" "$PS/box/com/codename1/gaming/physics/"
find "$RUNTIME_SRC" "$SC/gen" "$SAMPLES/scene2d/java" -name '*.java' -not -name 'UnityGame*.java' \
  -not -path '*/com/codename1/unitycompat/app/*' > "$PS/sources"
run_logged "$PS/javac.log" "scene2d: javac of the runtime and scene against vm/JavaAPI" \
  javac -nowarn -source 8 -target 8 -bootclasspath "$API" -Xlint:-options -cp "$SC/app:$PS/box" -d "$PS/classes" "@$PS/sources"
cp -R "$SC/app/." "$PS/classes/"
cp -R "$PS/box/." "$PS/classes/"
cp -R "$API/." "$PS/classes/"
native_run "$PS/native" "$PS/classes" SceneMain scene "$PS/native.txt"
compare "scene2d, native" "$SAMPLES/scene2d/expected-trace.txt" "$PS/native.txt"
js_run "$PS/js" "$PS/classes" scene_SceneMain "$PS/js.txt"
compare "scene2d, JavaScript" "$SAMPLES/scene2d/expected-trace.txt" "$PS/js.txt"

echo "== arcade2d through ParparVM"
run_logged "$WORKDIR/arcade2d-native.log" "arcade2d: native" \
  "$SAMPLES/headless/parparvm-trace.sh" "$AR" native "$AR/native.txt" "${ARCADE_ARGS[@]}"
compare "arcade2d, native" "$SAMPLES/arcade2d/expected-trace.txt" "$AR/native.txt"
run_logged "$WORKDIR/arcade2d-js.log" "arcade2d: JavaScript" \
  "$SAMPLES/headless/parparvm-trace.sh" "$AR" javascript "$AR/javascript.txt" "${ARCADE_ARGS[@]}"
compare "arcade2d, JavaScript" "$SAMPLES/arcade2d/expected-trace.txt" "$AR/javascript.txt"

echo "== menu2d through ParparVM"
run_logged "$WORKDIR/menu2d-native.log" "menu2d: native" \
  "$SAMPLES/headless/parparvm-trace.sh" "$MN" native "$MN/native.txt" "${MENU_ARGS[@]}"
compare "menu2d, native" "$SAMPLES/menu2d/expected-trace.txt" "$MN/native.txt"
run_logged "$WORKDIR/menu2d-js.log" "menu2d: JavaScript" \
  "$SAMPLES/headless/parparvm-trace.sh" "$MN" javascript "$MN/javascript.txt" "${MENU_ARGS[@]}"
compare "menu2d, JavaScript" "$SAMPLES/menu2d/expected-trace.txt" "$MN/javascript.txt"

echo "== query2d through ParparVM"
run_logged "$WORKDIR/query2d-native.log" "query2d: native" \
  "$SAMPLES/headless/parparvm-trace.sh" "$QR" native "$QR/native.txt" "${QUERY_ARGS[@]}"
compare "query2d, native" "$SAMPLES/query2d/expected-trace.txt" "$QR/native.txt"
run_logged "$WORKDIR/query2d-js.log" "query2d: JavaScript" \
  "$SAMPLES/headless/parparvm-trace.sh" "$QR" javascript "$QR/javascript.txt" "${QUERY_ARGS[@]}"
compare "query2d, JavaScript" "$SAMPLES/query2d/expected-trace.txt" "$QR/javascript.txt"

echo "== host2d through ParparVM"
run_logged "$WORKDIR/host2d-native.log" "host2d: native" \
  "$SAMPLES/headless/parparvm-trace.sh" "$HO" native "$HO/native.txt" "${HOST_ARGS[@]}" --host-source "$SAMPLES/host2d/java"
compare "host2d, native" "$SAMPLES/host2d/expected-trace.txt" "$HO/native.txt"
run_logged "$WORKDIR/host2d-js.log" "host2d: JavaScript" \
  "$SAMPLES/headless/parparvm-trace.sh" "$HO" javascript "$HO/javascript.txt" "${HOST_ARGS[@]}" --host-source "$SAMPLES/host2d/java"
compare "host2d, JavaScript" "$SAMPLES/host2d/expected-trace.txt" "$HO/javascript.txt"

echo "== platformer2d through ParparVM"
run_logged "$WORKDIR/platformer2d-native.log" "platformer2d: native" \
  "$SAMPLES/headless/parparvm-trace.sh" "$PF" native "$PF/native.txt" "${PLATFORMER_ARGS[@]}"
compare "platformer2d, native" "$SAMPLES/platformer2d/expected-trace.txt" "$PF/native.txt"
run_logged "$WORKDIR/platformer2d-js.log" "platformer2d: JavaScript" \
  "$SAMPLES/headless/parparvm-trace.sh" "$PF" javascript "$PF/javascript.txt" "${PLATFORMER_ARGS[@]}"
compare "platformer2d, JavaScript" "$SAMPLES/platformer2d/expected-trace.txt" "$PF/javascript.txt"

[ $FAILED -eq 0 ] || exit 1
echo "unity-compat-test: OK (JVM, native and JavaScript)"
