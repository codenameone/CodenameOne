#!/usr/bin/env bash
#
# Builds one desktop Java application two ways and measures both:
#
#   baseline   the application as its developer ships it today -- its jar and
#              dependencies, a jlink runtime image holding exactly the modules
#              it needs, and a jpackage application image;
#   cn1        the same, unmodified sources imported with
#              cn1:import-desktop-project and built as a native executable by
#              the local ParparVM builder (no JVM in the result).
#
# Usage:
#   bench.sh --app DIR --name NAME --kind swing|javafx --main CLASS --work DIR
#            [--out FILE] [--script FILE] [--stage all|build|baseline|measure]
#            [--startup-runs N] [--session-runs N] [--heaps "8 12 16 ..."]
#
#   --app     the application's own Maven project (pom.xml + src/main). Never
#             modified: it is read, and copied by the importer.
#   --work    every file this script writes goes below DIR/NAME.
#   --out     the result (default: DIR/NAME/result.json).
#   --script  input script for x11bench.py (default: input/NAME.txt beside
#             this script if it exists, else input/default.txt).
#   --stage   build = sizes only, nothing is launched; baseline = rebuild the
#             baseline and keep the native build; measure = reuse a finished
#             build. On macOS only "build" exists: run-macos-bench.sh
#             does the launching, by hand.
#
# Environment:
#   JAVA17_HOME          JDK 17, builds the Codename One project   (required)
#   BENCH_JDK21_HOME     JDK 21 with jmods, the baseline's JDK      (required)
#   BENCH_JAVAFX_JMODS   OpenJFX jmods (javafx-jmods.sh)   (required for javafx)
#   BENCH_M2             Maven local repository holding the Codename One
#                        artifacts (default: Maven's own default)
#   BENCH_USER_HOME      user.home for the Maven JVMs. The Codename One plugin
#                        keeps tools in ~/.codenameone; set this to keep a
#                        benchmark run out of the real one.
#   CN1_VERSION          Codename One version to build with (8.0-SNAPSHOT)
#   CN1_CC               C compiler for the native Linux build (the zig wrapper
#                        from install-linux-deps.sh), read by the builder itself
#   BENCH_XVFB           set to 1 to run Maven under xvfb-run (Linux; the CSS
#                        compiler opens an AWT frame)
set -euo pipefail

HERE="$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)"
ROOT="$(CDPATH='' cd -- "$HERE/../.." && pwd)"

APP="" NAME="" KIND="" MAIN="" WORK="" OUT="" SCRIPT="" STAGE="all"
STARTUP_RUNS=7 SESSION_RUNS=3
HEAPS="8 12 16 24 32 48 64 96 128 192 256 384 512"
while [ "$#" -gt 0 ]; do
  case "$1" in
    --app) APP="$2"; shift 2 ;;
    --name) NAME="$2"; shift 2 ;;
    --kind) KIND="$2"; shift 2 ;;
    --main) MAIN="$2"; shift 2 ;;
    --work) WORK="$2"; shift 2 ;;
    --out) OUT="$2"; shift 2 ;;
    --script) SCRIPT="$2"; shift 2 ;;
    --stage) STAGE="$2"; shift 2 ;;
    --startup-runs) STARTUP_RUNS="$2"; shift 2 ;;
    --session-runs) SESSION_RUNS="$2"; shift 2 ;;
    --heaps) HEAPS="$2"; shift 2 ;;
    -h|--help) sed -n '2,45p' "$0"; exit 0 ;;
    *) echo "bench.sh: unknown argument $1" >&2; exit 2 ;;
  esac
done
for REQUIRED in APP NAME KIND MAIN WORK; do
  if [ -z "${!REQUIRED}" ]; then
    echo "bench.sh: --$(printf '%s' "$REQUIRED" | tr '[:upper:]' '[:lower:]') is required" >&2
    exit 2
  fi
done
case "$KIND" in swing|javafx) ;; *) echo "bench.sh: --kind is swing or javafx" >&2; exit 2 ;; esac
: "${JAVA17_HOME:?JAVA17_HOME must name a JDK 17}"
: "${BENCH_JDK21_HOME:?BENCH_JDK21_HOME must name a JDK 21}"
if [ "$KIND" = "javafx" ]; then
  : "${BENCH_JAVAFX_JMODS:?BENCH_JAVAFX_JMODS must name the OpenJFX jmods (javafx-jmods.sh)}"
fi
CN1_VERSION="${CN1_VERSION:-8.0-SNAPSHOT}"
OS="$(uname -s)"

APP="$(CDPATH='' cd -- "$APP" && pwd)"
mkdir -p "$WORK/$NAME"
D="$(CDPATH='' cd -- "$WORK/$NAME" && pwd)"
OUT="${OUT:-$D/result.json}"
PARTS="$D/parts" LOGS="$D/logs" TMP="$D/tmp"
mkdir -p "$PARTS" "$LOGS" "$TMP"
export TMPDIR="$TMP"
if [ -z "$SCRIPT" ]; then
  SCRIPT="$HERE/input/$NAME.txt"
  [ -f "$SCRIPT" ] || SCRIPT="$HERE/input/default.txt"
fi

say() { printf '[bench %s] %s\n' "$NAME" "$*" >&2; }
util() { python3 "$HERE/benchutil.py" "$@"; }
# A failed step is a result, not the end of the run: the reason is recorded
# and the cells that depended on the step stay empty.
fail_part() { # <part> <default classification> <log>
  util diagnose "$3" "$2" > "$PARTS/$1.json"
  say "FAILED $1: $(python3 -c 'import json, sys; d = json.load(open(sys.argv[1])); print("%s: %s" % (d["classification"], d["first_error"]))' "$PARTS/$1.json") (see $3)"
}

# -Dopen=false: the Codename One plugin otherwise opens a generated Xcode or
# Android Studio project in the IDE, and a benchmark must never open a window.
MVN_ARGS=(-B -ntp -Dopen=false)
if [ -n "${BENCH_M2:-}" ]; then
  MVN_ARGS+=("-Dmaven.repo.local=$BENCH_M2")
fi
MVN_JVM="-Djava.io.tmpdir=$TMP"
CN1_HOME_DIR="$HOME/.codenameone"
if [ -n "${BENCH_USER_HOME:-}" ]; then
  MVN_JVM="$MVN_JVM -Duser.home=$BENCH_USER_HOME"
  CN1_HOME_DIR="$BENCH_USER_HOME/.codenameone"
fi
mvn_with() { # <java-home> <maven args...>
  local JDK="$1"
  shift
  if [ "${BENCH_XVFB:-0}" = "1" ]; then
    JAVA_HOME="$JDK" PATH="$JDK/bin:$PATH" MAVEN_OPTS="$MVN_JVM" xvfb-run -a mvn "${MVN_ARGS[@]}" "$@"
  else
    JAVA_HOME="$JDK" PATH="$JDK/bin:$PATH" MAVEN_OPTS="$MVN_JVM" mvn "${MVN_ARGS[@]}" "$@"
  fi
}

BASE="$D/baseline"
CN1="$D/cn1"
J21="$BENCH_JDK21_HOME/bin"

# ---------------------------------------------------------------------------
# Baseline: jar + dependencies, jlink runtime image, jpackage application image

fx_modules() {
  # javafx.controls always (it pulls graphics and base); the rest only when the
  # sources name them, so the runtime image holds what the application uses.
  local MODULES="javafx.controls" PAIR MODULE PATTERN
  for PAIR in "fxml:javafx.fxml" "media:javafx.scene.media" "swing:javafx.embed.swing"; do
    MODULE="${PAIR%%:*}"
    PATTERN="${PAIR#*:}"
    if grep -rqs --include='*.java' "import ${PATTERN}" "$APP/src/main/java" \
        || { [ "$MODULE" = "fxml" ] && find "$APP/src/main" -name '*.fxml' | grep -q .; }; then
      MODULES="$MODULES,javafx.$MODULE"
    fi
  done
  printf '%s' "$MODULES"
}

build_baseline() {
  local LOG="$LOGS/baseline-build.log"
  rm -rf "$BASE"
  mkdir -p "$BASE/app" "$BASE/classes"
  say "baseline: dependencies"
  # The application's own pom decides its dependencies. OpenJFX is left out:
  # it comes from the jmods, as part of the runtime image.
  # The OpenJFX dependencies are cut out of a copy of the pom rather than
  # excluded on the command line: an exclusion still resolves them first, and
  # OpenJFX publishes no artifact at all for some version/architecture pairs.
  mkdir -p "$BASE/pom"
  python3 - "$APP/pom.xml" "$BASE/pom/pom.xml" <<'PY'
import re, sys
with open(sys.argv[1], encoding="utf-8") as handle:
    pom = handle.read()
pom = re.sub(r"<build>.*?</build>", "", pom, flags=re.S)
pom = re.sub(r"<dependency>(?:(?!</dependency>).)*?<groupId>\s*org\.openjfx\s*</groupId>.*?</dependency>",
             "", pom, flags=re.S)
with open(sys.argv[2], "w", encoding="utf-8") as handle:
    handle.write(pom)
PY
  if ! mvn_with "$BENCH_JDK21_HOME" -q -f "$BASE/pom/pom.xml" dependency:copy-dependencies \
      "-DoutputDirectory=$BASE/app" -DincludeScope=runtime \
      "-Dmdep.stripClassifier=false" > "$LOG" 2>&1; then
    fail_part baseline "baseline build" "$LOG"
    return 1
  fi
  local CP="" JAR
  for JAR in "$BASE/app"/*.jar; do
    [ -f "$JAR" ] && CP="$CP:$JAR"
  done
  CP="${CP#:}"
  local FX=() FXMODS=""
  if [ "$KIND" = "javafx" ]; then
    FXMODS="$(fx_modules)"
    FX=(--module-path "$BENCH_JAVAFX_JMODS" --add-modules "$FXMODS")
  fi
  say "baseline: compile"
  # On the class path, without the module descriptor: the same sources then
  # build whether or not their author wrote one, and the importer ignores it too.
  find "$APP/src/main/java" -name '*.java' ! -name 'module-info.java' > "$BASE/sources.txt"
  if ! "$J21/javac" -nowarn -encoding UTF-8 --release 17 -d "$BASE/classes" ${CP:+-cp "$CP"} \
      ${FX[@]+"${FX[@]}"} "@$BASE/sources.txt" >> "$LOG" 2>&1; then
    fail_part baseline "baseline build" "$LOG"
    return 1
  fi
  if [ -d "$APP/src/main/resources" ]; then
    cp -R "$APP/src/main/resources/." "$BASE/classes/"
  fi
  "$J21/jar" --create --file "$BASE/app/$NAME.jar" --main-class "$MAIN" -C "$BASE/classes" . >> "$LOG" 2>&1

  say "baseline: jlink"
  local MODULES
  # jdeps is not given the OpenJFX modules: it cannot read a jmod from a module
  # path. The javafx.* packages are therefore "missing" and ignored here, and
  # the modules the sources name are added below.
  if ! MODULES="$("$J21/jdeps" --ignore-missing-deps --multi-release 17 --print-module-deps \
      ${CP:+-cp "$CP"} "$BASE/app"/*.jar 2>> "$LOG")"; then
    fail_part baseline "baseline build" "$LOG"
    return 1
  fi
  if [ -n "$FXMODS" ]; then
    MODULES="$MODULES,$FXMODS"
  fi
  local LINK_PATH="$BENCH_JDK21_HOME/jmods"
  if [ "$KIND" = "javafx" ]; then
    LINK_PATH="$LINK_PATH:$BENCH_JAVAFX_JMODS"
  fi
  if ! "$J21/jlink" --module-path "$LINK_PATH" --add-modules "$MODULES" \
      --strip-debug --no-header-files --no-man-pages --compress zip-6 \
      --output "$BASE/runtime" >> "$LOG" 2>&1; then
    fail_part baseline "baseline build" "$LOG"
    return 1
  fi
  printf '%s\n' "$MODULES" > "$BASE/modules.txt"

  say "baseline: jpackage"
  local JPACKAGE_OK=1
  if ! "$J21/jpackage" --type app-image --name "$NAME" --input "$BASE/app" \
      --main-jar "$NAME.jar" --main-class "$MAIN" --runtime-image "$BASE/runtime" \
      --dest "$BASE/jpackage" >> "$LOG" 2>&1; then
    JPACKAGE_OK=0
    say "jpackage failed here; its cells stay empty (see $LOG)"
  fi

  # Sizes are taken now, before anything below adds a CDS archive: the archive
  # is a start-up optimisation the developer may or may not ship, and leaving it
  # out is the smaller, more favourable figure for the baseline.
  util size --scratch "$TMP" "$BASE/app" > "$PARTS/size.baseline_jars.json"
  util size --scratch "$TMP" "$BASE/runtime" > "$PARTS/size.baseline_runtime_image.json"
  util size --scratch "$TMP" "$BASE/runtime" "$BASE/app" > "$PARTS/size.baseline_jlink.json"
  if [ "$JPACKAGE_OK" = "1" ]; then
    util size --scratch "$TMP" "$BASE/jpackage" > "$PARTS/size.baseline_jpackage.json"
  fi

  # The JDK's default CDS archive. A distribution JDK has one and a jlink image
  # does not, so without this "default flags" would time a slower JVM than the
  # one a developer starts with `java -jar`.
  "$BASE/runtime/bin/java" -Xshare:dump >> "$LOG" 2>&1 || say "default CDS archive not generated"
  util size --scratch "$TMP" "$BASE/runtime" "$BASE/app" > "$PARTS/size.baseline_jlink_with_cds.json"

  python3 - "$PARTS/baseline.json" "$MODULES" "$MAIN" <<'PY'
import json, sys
with open(sys.argv[1], "w") as handle:
    json.dump({"ok": True, "modules": sys.argv[2].split(","), "main_class": sys.argv[3],
               "jlink_flags": "--strip-debug --no-header-files --no-man-pages --compress zip-6",
               "javac_flags": "--release 17, class path (module-info.java left out)"},
              handle, indent=2)
PY
}

# ---------------------------------------------------------------------------
# Codename One: archetype + import-desktop-project + the local native builder

prepare_cn1_home() {
  mkdir -p "$CN1_HOME_DIR"
  # generate-gui-sources insists on both files existing; neither is used by a
  # project that has no GUI builder forms. setup-workspace.sh does the same.
  [ -f "$CN1_HOME_DIR/guibuilder.jar" ] || : > "$CN1_HOME_DIR/guibuilder.jar"
  if [ ! -f "$CN1_HOME_DIR/CodeNameOneBuildClient.jar" ]; then
    cp "$ROOT/maven/CodeNameOneBuildClient.jar" "$CN1_HOME_DIR/CodeNameOneBuildClient.jar"
  fi
}

generate_cn1_project() {
  local LOG="$LOGS/cn1-generate.log"
  rm -rf "$CN1"
  mkdir -p "$CN1"
  prepare_cn1_home
  say "cn1: archetype"
  if ! (cd "$CN1" && mvn_with "$JAVA17_HOME" archetype:generate \
      -DarchetypeGroupId=com.codenameone -DarchetypeArtifactId=cn1app-archetype \
      "-DarchetypeVersion=$CN1_VERSION" -DgroupId=com.codenameone.bench -DartifactId=app \
      -Dpackage=com.codenameone.bench -Dversion=1.0-SNAPSHOT -DmainName=BenchApp \
      -DjavaVersion=17 -DinteractiveMode=false) > "$LOG" 2>&1; then
    fail_part cn1 "project generation" "$LOG"
    return 1
  fi
  say "cn1: import-desktop-project"
  if ! (cd "$CN1/app" && mvn_with "$JAVA17_HOME" \
      "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:import-desktop-project" \
      "-Dcn1.desktop.import=$APP") >> "$LOG" 2>&1; then
    fail_part cn1 "import" "$LOG"
    return 1
  fi
}

build_cn1_linux() {
  local LOG="$LOGS/cn1-linux-build.log" ARCH
  case "$(uname -m)" in aarch64|arm64) ARCH="arm64" ;; *) ARCH="x64" ;; esac
  # The builder's default is x64 whatever the host is.
  printf '\ncodename1.arg.linux.arch=%s\n' "$ARCH" >> "$CN1/app/common/codenameone_settings.properties"
  say "cn1: native Linux build ($ARCH)"
  if ! (cd "$CN1/app" && mvn_with "$JAVA17_HOME" package -DskipTests \
      -Dcodename1.platform=linux -Dcodename1.buildTarget=local-linux-device) > "$LOG" 2>&1; then
    fail_part cn1 "layer or build-engine" "$LOG"
    return 1
  fi
  local EXE
  EXE="$(sed -n 's/.*Built native Linux executable: //p' "$LOG" | tail -1)"
  if [ -z "$EXE" ] || [ ! -f "$EXE" ]; then
    fail_part cn1 "native compile or link" "$LOG"
    return 1
  fi
  # The distributable is the executable's directory as the builder left it,
  # minus what only a developer wants (a separate debug-symbol companion).
  rm -rf "$CN1/dist"
  mkdir -p "$CN1/dist"
  cp -R "$(dirname "$EXE")/." "$CN1/dist/"
  find "$CN1/dist" -name '*.debug' -delete
  printf '%s\n' "$(basename "$EXE")" > "$CN1/executable.txt"
  record_cn1 "$EXE" "$LOG" linux
}

record_cn1() { # <executable-or-bundle> <log> <target>
  local PRODUCT="$1" LOG="$2" TARGET="$3" JAR TRANSLATED
  util size --scratch "$TMP" "$CN1/dist" > "$PARTS/size.cn1_$TARGET.json"
  JAR="$(find "$CN1/app" -name '*-jar-with-dependencies.jar' -path '*target*' | head -1)"
  if [ -n "$JAR" ]; then
    util layers "$JAR" > "$PARTS/layers.staged_jar.json"
  fi
  TRANSLATED="$(find "$CN1/app" -type d -path '*-build*' -name 'dist' | head -1)"
  if [ -n "$TRANSLATED" ]; then
    util translated "$TRANSLATED" > "$PARTS/layers.translated_$TARGET.json"
  fi
  python3 - "$PARTS/cn1_$TARGET.json" "$PRODUCT" "$LOG" <<'PY'
import json, os, re, subprocess, sys
product, log = sys.argv[2], sys.argv[3]
info = {"ok": True, "product": os.path.basename(product)}
if os.path.isfile(product):
    info["executable_bytes"] = os.path.getsize(product)
    try:
        out = subprocess.run(["size", product], stdout=subprocess.PIPE, universal_newlines=True).stdout
        fields = out.splitlines()[1].split()
        info["text_bytes"], info["data_bytes"], info["bss_bytes"] = (int(f) for f in fields[:3])
    except Exception:
        pass
    try:
        out = subprocess.run(["file", "-b", product], stdout=subprocess.PIPE,
                             universal_newlines=True).stdout.strip()
        info["file"] = out.splitlines()[0] if out else out
        if "ELF" in out:
            info["stripped"] = "not stripped" not in out
    except Exception:
        pass
    try:
        # A universal Mac binary: the size of each slice, since a developer
        # who ships one architecture ships about half of the file.
        out = subprocess.run(["lipo", "-detailed_info", product], stdout=subprocess.PIPE,
                             stderr=subprocess.DEVNULL, universal_newlines=True).stdout
        slices = dict((name, int(size)) for name, size in
                      re.findall(r"architecture (\S+).*?\n\s+size (\d+)", out, re.S))
        if slices:
            info["slice_bytes"] = slices
    except Exception:
        pass
with open(log, errors="replace") as handle:
    text = handle.read()
flags = re.findall(r"-DCMAKE_BUILD_TYPE=\w+", text)
if flags:
    info["cmake_build_type"] = flags[-1].split("=")[1]
with open(sys.argv[1], "w") as handle:
    json.dump(info, handle, indent=2)
PY
}

build_cn1_macos() {
  local LOG="$LOGS/cn1-macos-build.log"
  # Unsigned, on purpose: the builder otherwise looks for a "Developer ID
  # Application" certificate, and a benchmark must neither need one nor use the
  # one it finds in the keychain of whoever runs it. The slice is universal (arm64 +
  # x86_64), the builder's default, so the bundle is what a developer would ship.
  printf '\ncodename1.arg.macos.signingIdentity.developerID=none\n' \
    >> "$CN1/app/common/codenameone_settings.properties"
  say "cn1: native macOS build"
  # The AppKit build rides the "ios" platform profile of the generated project;
  # the build target is what makes it a Mac application.
  if ! (cd "$CN1/app" && mvn_with "$JAVA17_HOME" package -DskipTests \
      -Dcodename1.platform=ios -Dcodename1.buildTarget=local-mac-device) > "$LOG" 2>&1; then
    fail_part cn1 "layer or build-engine" "$LOG"
    return 1
  fi
  local BUNDLE
  BUNDLE="$(find "$CN1/app" -type d -name '*.app' -path '*Release*' -not -path '*/*.app/*' | head -1)"
  if [ -z "$BUNDLE" ]; then
    BUNDLE="$(find "$CN1/app" -type d -name '*.app' -not -path '*/*.app/*' | head -1)"
  fi
  if [ -z "$BUNDLE" ]; then
    fail_part cn1 "native compile or link" "$LOG"
    return 1
  fi
  rm -rf "$CN1/dist"
  mkdir -p "$CN1/dist"
  cp -R "$BUNDLE" "$CN1/dist/"
  find "$CN1/dist" -name '*.dSYM' -prune -exec rm -rf {} +
  printf '%s\n' "$(basename "$BUNDLE")" > "$CN1/executable.txt"
  record_cn1 "$CN1/dist/$(basename "$BUNDLE")/Contents/MacOS/$(basename "$BUNDLE" .app)" "$LOG" macos
}

build_cn1_windows() {
  # Only CI runs this (a windows-latest runner with the MSVC environment and
  # clang-cl on PATH, under Git Bash); it has never been run on a developer box.
  local LOG="$LOGS/cn1-windows-build.log" EXE
  say "cn1: native Windows build"
  if ! (cd "$CN1/app" && mvn_with "$JAVA17_HOME" package -DskipTests \
      -Dcodename1.platform=win -Dcodename1.buildTarget=local-windows-device) > "$LOG" 2>&1; then
    fail_part cn1 "layer or build-engine" "$LOG"
    return 1
  fi
  EXE="$(sed -n 's/.*Native Windows executable: \(.*\) ([a-z0-9]*).*/\1/p' "$LOG" | tail -1 | tr -d '\r')"
  if [ -n "$EXE" ] && command -v cygpath > /dev/null 2>&1; then
    EXE="$(cygpath -u "$EXE")"
  fi
  if [ -z "$EXE" ] || [ ! -f "$EXE" ]; then
    fail_part cn1 "native compile or link" "$LOG"
    return 1
  fi
  # The executable and the libraries beside it; the directory also holds the
  # objects it was linked from, which nobody ships.
  rm -rf "$CN1/dist"
  mkdir -p "$CN1/dist"
  cp "$EXE" "$CN1/dist/"
  find "$(dirname "$EXE")" -maxdepth 1 -name '*.dll' -exec cp {} "$CN1/dist/" \;
  printf '%s\n' "$(basename "$EXE")" > "$CN1/executable.txt"
  record_cn1 "$EXE" "$LOG" windows
}

# ---------------------------------------------------------------------------
# Measurement (Linux): three variants through the same driver

JVM_BASE=()
measure_variant() { # <label> <evict-path> <command...>
  local LABEL="$1" EVICT="$2"
  shift 2
  say "measure: $LABEL"
  python3 "$HERE/x11bench.py" --label "$LABEL" --out "$PARTS/run.$LABEL.json" \
    --log-dir "$LOGS/run-$LABEL" --cwd "$D" --script "$SCRIPT" \
    --startup-runs "$STARTUP_RUNS" --session-runs "$SESSION_RUNS" \
    --screen "${BENCH_SCREEN:-1280x800}" --evict "$EVICT" -- "$@" \
    || say "measure: $LABEL did not complete (recorded in run.$LABEL.json)"
}

exception_lines() { # <log>: how many lines name a Java exception or error
  local COUNT=0
  if [ -f "$1" ]; then
    COUNT="$(grep -c -E '(Exception|Error)(:|$)' "$1" || true)"
  fi
  printf '%s' "${COUNT:-0}"
}

measure_linux() {
  export BENCH_SCREEN="${BENCH_SCREEN:-1280x800}"
  if [ -f "$PARTS/baseline.json" ] && grep -q '"ok": true' "$PARTS/baseline.json"; then
    local CP="" JAR
    for JAR in "$BASE/app"/*.jar; do
      CP="$CP:$JAR"
    done
    CP="${CP#:}"
    JVM_BASE=("$BASE/runtime/bin/java" -cp "$CP")
    measure_variant jvm_default "$BASE" "${JVM_BASE[@]}" "$MAIN"

    # Tuned: the smallest configuration a developer could reasonably ship.
    # AppCDS first (a training run through the same input script), then the
    # smallest heap from the list that still survives that script.
    local TUNED=(-XX:+UseSerialGC -Xss256k -XX:TieredStopAtLevel=1) JSA="$BASE/app.jsa" HEAP FOUND=""
    rm -f "$JSA"
    python3 "$HERE/x11bench.py" --label train --out "$TMP/train.json" --log-dir "$LOGS/run-train" \
      --cwd "$D" --script "$SCRIPT" --check-only -- \
      "${JVM_BASE[@]}" "${TUNED[@]}" "-XX:ArchiveClassesAtExit=$JSA" "$MAIN" || true
    if [ -f "$JSA" ]; then
      TUNED+=("-XX:SharedArchiveFile=$JSA")
    else
      say "AppCDS archive was not written; tuned run uses the default archive only"
    fi
    # A heap is big enough when the application paints, survives the script
    # and logs no more exceptions than it does with no limit at all. Looking
    # for OutOfMemoryError alone is not enough: JavaFX under a heap too small
    # for its textures paints once and then throws a NullPointerException from
    # the renderer on every frame.
    local QUIET LOUD
    QUIET="$(exception_lines "$LOGS/run-train/check.log")"
    for HEAP in $HEAPS; do
      if python3 "$HERE/x11bench.py" --label "heap$HEAP" --out "$TMP/heap$HEAP.json" \
          --log-dir "$LOGS/run-heap-search" --cwd "$D" --script "$SCRIPT" --check-only -- \
          "${JVM_BASE[@]}" "${TUNED[@]}" "-Xmx${HEAP}m" "$MAIN"; then
        LOUD="$(exception_lines "$LOGS/run-heap-search/check.log")"
        if [ "$LOUD" -le "$QUIET" ]; then
          FOUND="$HEAP"
          break
        fi
      fi
    done
    if [ -n "$FOUND" ]; then
      TUNED+=("-Xmx${FOUND}m")
      measure_variant jvm_tuned "$BASE" "${JVM_BASE[@]}" "${TUNED[@]}" "$MAIN"
      python3 - "$PARTS/tuned.json" "$FOUND" "$JSA" "${TUNED[*]}" <<'PY'
import json, os, sys
jsa = sys.argv[3]
with open(sys.argv[1], "w") as handle:
    json.dump({"xmx_mb": int(sys.argv[2]), "flags": sys.argv[4].replace(os.path.dirname(jsa) + "/", ""),
               "appcds_bytes": os.path.getsize(jsa) if os.path.isfile(jsa) else None},
              handle, indent=2)
PY
    else
      say "no heap in [$HEAPS] ran the script; jvm_tuned stays empty"
    fi
  fi
  if [ -f "$CN1/executable.txt" ]; then
    measure_variant cn1_native "$CN1/dist" "$CN1/dist/$(cat "$CN1/executable.txt")"
  fi
}

# ---------------------------------------------------------------------------

BENCH_COMMIT="${BENCH_COMMIT:-$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || true)}"
export BENCH_COMMIT

if [ "$STAGE" = "baseline" ]; then
  rm -f "$PARTS"/baseline.json "$PARTS"/size.baseline_*.json
  build_baseline || true
elif [ "$STAGE" != "measure" ]; then
  rm -f "$PARTS"/*.json
  build_baseline || true
  if generate_cn1_project; then
    case "$OS" in
      Linux) build_cn1_linux || true ;;
      Darwin) build_cn1_macos || true ;;
      MINGW*|MSYS*|CYGWIN*) build_cn1_windows || true ;;
      *) say "no native builder is wired for $OS" ;;
    esac
  fi
fi
if [ "$STAGE" != "build" ] && [ "$STAGE" != "baseline" ]; then
  case "$OS" in
    Linux) rm -f "$PARTS"/run.*.json "$PARTS/tuned.json"; measure_linux ;;
    *) say "nothing is launched on $OS by this script; see run-macos-bench.sh" ;;
  esac
fi

util environment > "$PARTS/environment.json"
case "$OS" in
  Linux) TARGET="linux" ;;
  Darwin) TARGET="macos" ;;
  *) TARGET="windows" ;;
esac
python3 - "$PARTS/app.json" "$NAME" "$KIND" "$MAIN" "$(basename "$SCRIPT")" "$TARGET" <<'PY'
import json, sys
with open(sys.argv[1], "w") as handle:
    json.dump({"name": sys.argv[2], "kind": sys.argv[3], "main_class": sys.argv[4],
               "input_script": sys.argv[5], "target": sys.argv[6]}, handle, indent=2)
PY
util assemble "$PARTS" "$OUT"
say "wrote $OUT"
