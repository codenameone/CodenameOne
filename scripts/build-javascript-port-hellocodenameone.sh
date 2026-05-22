#!/usr/bin/env bash
set -euo pipefail

bj_log() { echo "[build-javascript-port-hellocodenameone] $1"; }

usage() {
  cat <<'EOF' >&2
Usage: build-javascript-port-hellocodenameone.sh [output_zip]

Builds a ParparVM-backed browser bundle for scripts/hellocodenameone using:
  - scripts/hellocodenameone/common
  - Ports/JavaScriptPort runtime sources
  - vm/ByteCodeTranslator via maven/parparvm

Environment:
  SKIP_MAVEN_BUILD=1       Reuse existing target outputs instead of rebuilding
  SKIP_PARPARVM_BUILD=1   Reuse existing maven/parparvm target outputs
  SKIP_COMMON_BUILD=1     Reuse existing scripts/hellocodenameone/common target outputs
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
HELLO_ROOT="$REPO_ROOT/scripts/hellocodenameone"
COMMON_ROOT="$HELLO_ROOT/common"
PORT_ROOT="$REPO_ROOT/Ports/JavaScriptPort"
PARPARVM_ROOT="$REPO_ROOT/maven/parparvm"
OUTPUT_ZIP="${1:-$HELLO_ROOT/parparvm/target/hellocodenameone-javascript-port.zip}"

TMPDIR="${TMPDIR:-/tmp}"
TMPDIR="${TMPDIR%/}"
WORK_DIR="$(mktemp -d "${TMPDIR}/cn1-jsport-build-XXXXXX" 2>/dev/null || echo "${TMPDIR}/cn1-jsport-build")"
if [ "${KEEP_JS_BUILD_DIR:-0}" = "1" ]; then
  bj_log "Keeping build directory at $WORK_DIR"
else
  trap 'rm -rf "$WORK_DIR" 2>/dev/null || true' EXIT
fi

JAVA_HOME="${JAVA_HOME:-}"
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVAC_BIN="${JAVA_HOME:+$JAVA_HOME/bin/javac}"
JAR_BIN="${JAVA_HOME:+$JAVA_HOME/bin/jar}"
if [ -z "$JAVA_BIN" ] || [ ! -x "$JAVA_BIN" ]; then
  JAVA_BIN="$(command -v java)"
fi
if [ -z "$JAVAC_BIN" ] || [ ! -x "$JAVAC_BIN" ]; then
  JAVAC_BIN="$(command -v javac)"
fi
if [ -z "$JAR_BIN" ] || [ ! -x "$JAR_BIN" ]; then
  JAR_BIN="$(command -v jar)"
fi
if [ -z "$JAVA_BIN" ] || [ ! -x "$JAVA_BIN" ] || [ -z "$JAVAC_BIN" ] || [ ! -x "$JAVAC_BIN" ] || [ -z "$JAR_BIN" ] || [ ! -x "$JAR_BIN" ]; then
  bj_log "A working JDK is required (java, javac, jar)." >&2
  exit 2
fi

if [ "${SKIP_MAVEN_BUILD:-0}" != "1" ] && [ "${SKIP_PARPARVM_BUILD:-0}" != "1" ]; then
  bj_log "Building ParparVM compiler bundle"
  mvn -B -f "$REPO_ROOT/maven/pom.xml" -pl parparvm -am -DskipTests -Dmaven.javadoc.skip=true package
fi

if [ "${SKIP_MAVEN_BUILD:-0}" != "1" ] && [ "${SKIP_COMMON_BUILD:-0}" != "1" ]; then
  bj_log "Building HelloCodenameOne common module and compile-scope dependencies"
  mkdir -p "$HOME/.codenameone"
  if [ -f "$REPO_ROOT/maven/UpdateCodenameOne.jar" ]; then
    cp "$REPO_ROOT/maven/UpdateCodenameOne.jar" "$HOME/.codenameone/" 2>/dev/null || true
  fi
  (
    cd "$HELLO_ROOT"
    ./mvnw -q -U -pl common -am -DskipTests -Dautomated=true package dependency:copy-dependencies -DincludeScope=compile -DoutputDirectory=common/target/parparvm-deps
  )
fi

COMMON_CLASSES="$COMMON_ROOT/target/classes"
COMMON_DEPS_DIR="$COMMON_ROOT/target/parparvm-deps"
PARPARVM_JAVA_API="$PARPARVM_ROOT/target/bundle/parparvm-java-api.jar"
PARPARVM_COMPILER="$PARPARVM_ROOT/target/bundle/parparvm-compiler.jar"
CN1_CORE_JAR="$(find "$HOME/.m2/repository/com/codenameone/codenameone-core" -path '*/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT.jar' -type f | head -n 1 || true)"
JAVA_RUNTIME_JAR="$(find "$HOME/.m2/repository/com/codenameone/java-runtime" -path '*/8.0-SNAPSHOT/java-runtime-8.0-SNAPSHOT.jar' -type f | head -n 1 || true)"

for required in "$COMMON_CLASSES" "$PARPARVM_JAVA_API" "$PARPARVM_COMPILER" "$CN1_CORE_JAR" "$JAVA_RUNTIME_JAR"; do
  if [ ! -e "$required" ]; then
    bj_log "Required build artifact missing: $required" >&2
    exit 3
  fi
done

STAGE_CLASSES="$WORK_DIR/stage-classes"
PORT_CLASSES="$WORK_DIR/port-classes"
SOURCE_LIST="$WORK_DIR/javascript-port-sources.txt"
LAUNCHER_SRC="$WORK_DIR/HelloCodenameOneJavaScriptMain.java"
TRANSLATOR_OUT="$WORK_DIR/translator-output"
TRANSLATOR_APP_NAME="HelloCodenameOneJavaScriptMain"
DIST_APP_NAME="HelloCodenameOne"
mkdir -p "$STAGE_CLASSES" "$PORT_CLASSES" "$TRANSLATOR_OUT"

bj_log "Staging JavaAPI and application classes"
(
  cd "$STAGE_CLASSES"
  "$JAR_BIN" xf "$CN1_CORE_JAR"
  "$JAR_BIN" xf "$JAVA_RUNTIME_JAR"
  # The ParparVM Java API jar contains the browser-targeted java.* classes
  # that must override any stale snapshot copies from codenameone-core or
  # java-runtime in ~/.m2.  Extract it last so the staged classes match the
  # intended JS runtime surface.
  "$JAR_BIN" xf "$PARPARVM_JAVA_API"
)
cp -R "$COMMON_CLASSES"/. "$STAGE_CLASSES"/

if [ -d "$COMMON_DEPS_DIR" ]; then
  while IFS= read -r -d '' jar_file; do
    jar_name="$(basename "$jar_file")"
    case "$jar_name" in
      kotlin-*.jar|annotations-*.jar)
        bj_log "Including dependency classes from $jar_name"
        (
          cd "$STAGE_CLASSES"
          "$JAR_BIN" xf "$jar_file"
        )
        ;;
    esac
  done < <(find "$COMMON_DEPS_DIR" -maxdepth 1 -type f -name '*.jar' -print0 | sort -z)
fi

# TeaVM is optional for ParparVM builds. The JavaScriptPort now includes JSO interfaces
# in org.teavm.jso package, so it can compile without external TeaVM dependency.
# TeaVM jars are only needed if the TeaVM compiler needs to run (which we don't use).
TEAVM_VERSION=""
TEAVM_AVAILABLE=0
for candidate in 0.6.0-cn1-006 0.8.1; do
  if [ -f "$HOME/.m2/repository/org/teavm/teavm-jso/$candidate/teavm-jso-$candidate.jar" ]; then
    TEAVM_VERSION="$candidate"
    TEAVM_AVAILABLE=1
    break
  fi
done

if [ "$TEAVM_AVAILABLE" -eq 0 ]; then
  bj_log "Note: Using built-in JSO interfaces (no external TeaVM dependency)"
fi

# Generate the appropriate launcher based on whether TeaVM is available
# Both launchers work - they bootstrap the implementation factory before Display.init()
bj_log "Preparing JavaScript-port launcher"
if [ "$TEAVM_AVAILABLE" -eq 1 ]; then
  cat > "$LAUNCHER_SRC" <<'EOF'
import com.codename1.impl.html5.JavaScriptPortBootstrap;
import com.codenameone.examples.hellocodenameone.HelloCodenameOne;

public final class HelloCodenameOneJavaScriptMain {
    public static void main(String[] args) {
        JavaScriptPortBootstrap.bootstrap(new HelloCodenameOne());
    }
}
EOF
else
  cat > "$LAUNCHER_SRC" <<'EOF'
import com.codename1.impl.html5.ParparVMBootstrap;
import com.codenameone.examples.hellocodenameone.HelloCodenameOne;

public final class HelloCodenameOneJavaScriptMain {
    public static void main(String[] args) {
        ParparVMBootstrap.bootstrap(new HelloCodenameOne());
    }
}
EOF
fi

# Build source list: JavaScriptPort sources plus launcher
# JavaScriptPort now includes org.teavm.jso interfaces built-in
SOURCE_LIST="$WORK_DIR/javascript-port-sources.txt"
bj_log "Building source list for JavaScriptPort"
find "$PORT_ROOT/src/main/java" -type f -name '*.java' ! -name 'Stub.java' | sort > "$SOURCE_LIST"
# Add launcher
echo "$LAUNCHER_SRC" >> "$SOURCE_LIST"

CLASSPATH_ENTRIES=("$STAGE_CLASSES")
TEAVM_JARS=()
if [ "$TEAVM_AVAILABLE" -eq 1 ]; then
  while IFS= read -r -d '' jar_file; do
    jar_name="$(basename "$jar_file")"
    case "$jar_name" in
      teavm-jso-*.jar|teavm-jso.jar|teavm-jso-apis-*.jar|teavm-jso-impl-*.jar|teavm-platform-*.jar|teavm-classlib-*.jar|teavm-interop-*.jar)
        TEAVM_JARS+=("$jar_file")
        ;;
    esac
    CLASSPATH_ENTRIES+=("$jar_file")
  done < <(find "$HOME/.m2/repository/org/teavm" -path "*$TEAVM_VERSION/*.jar" -type f -print0 | sort -z)
fi
if [ -d "$COMMON_DEPS_DIR" ]; then
  while IFS= read -r -d '' jar_file; do
    CLASSPATH_ENTRIES+=("$jar_file")
  done < <(find "$COMMON_DEPS_DIR" -maxdepth 1 -type f -name '*.jar' -print0 | sort -z)
fi

CLASSPATH=""
for entry in "${CLASSPATH_ENTRIES[@]}"; do
  if [ -z "$CLASSPATH" ]; then
    CLASSPATH="$entry"
  else
    CLASSPATH="$CLASSPATH:$entry"
  fi
done

if [ "${#TEAVM_JARS[@]}" -gt 0 ]; then
  bj_log "Staging TeaVM dependency classes for translation"
  for jar_file in "${TEAVM_JARS[@]}"; do
    (
      cd "$STAGE_CLASSES"
      "$JAR_BIN" xf "$jar_file"
    )
  done
  rm -rf "$STAGE_CLASSES/org/teavm/classlib/impl/report"
fi

# Compile JavaScriptPort sources
# JavaScriptPort includes org.teavm.jso interfaces, so it compiles without TeaVM jars
bj_log "Compiling JavaScript-port runtime sources"
"$JAVAC_BIN" -source 8 -target 8 -cp "$CLASSPATH" -d "$PORT_CLASSES" @"$SOURCE_LIST"
cp -R "$PORT_CLASSES"/. "$STAGE_CLASSES"/

bj_log "Running ByteCodeTranslator for HelloCodenameOne"
"$JAVA_BIN" -cp "$PARPARVM_COMPILER" com.codename1.tools.translator.ByteCodeTranslator \
  javascript \
  "$STAGE_CLASSES" \
  "$TRANSLATOR_OUT" \
  "$TRANSLATOR_APP_NAME" \
  "com.codenameone.examples.hellocodenameone" \
  "HelloCodenameOne" \
  "1.0" \
  "ios" \
  "none"

DIST_DIR="$TRANSLATOR_OUT/dist/$TRANSLATOR_APP_NAME-js"
if [ ! -d "$DIST_DIR" ]; then
  DIST_DIR="$(find "$TRANSLATOR_OUT/dist" -mindepth 1 -maxdepth 2 -type f -name worker.js -print | head -n 1 | xargs -I{} dirname "{}" 2>/dev/null || true)"
fi
if [ -z "$DIST_DIR" ] || [ ! -d "$DIST_DIR" ]; then
  bj_log "Expected translated browser bundle directory missing under $TRANSLATOR_OUT/dist" >&2
  exit 5
fi

# ByteCodeTranslator copies non-class resources to the top-level output dir. Move
# the app resources into the served bundle so browser execution can load them.
while IFS= read -r -d '' entry; do
  name="$(basename "$entry")"
  [ "$name" = "dist" ] && continue
  if [ -d "$entry" ]; then
    cp -R "$entry"/. "$DIST_DIR"/
  else
    cp "$entry" "$DIST_DIR"/
  fi
done < <(find "$TRANSLATOR_OUT" -mindepth 1 -maxdepth 1 -print0)

# HTML5Implementation.getResourceAsStream resolves relative resources under
# "assets/", but some bundled artifacts (most notably material-design-font.ttf
# from codenameone-core.jar) land at the bundle root because the translator
# mirrors the jar layout. Relocate those into assets/ so the Java side can
# actually load them without every caller paying a ci-fallback stub tax.
if [ -d "$DIST_DIR" ]; then
  mkdir -p "$DIST_DIR/assets"
  for rel in material-design-font.ttf; do
    if [ -f "$DIST_DIR/$rel" ] && [ ! -f "$DIST_DIR/assets/$rel" ]; then
      mv "$DIST_DIR/$rel" "$DIST_DIR/assets/$rel"
      bj_log "Relocated $rel to assets/"
    fi
  done
fi

FINAL_DIST_DIR="$TRANSLATOR_OUT/dist/$DIST_APP_NAME-js"
if [ "$DIST_DIR" != "$FINAL_DIST_DIR" ]; then
  rm -rf "$FINAL_DIST_DIR"
  mv "$DIST_DIR" "$FINAL_DIST_DIR"
  DIST_DIR="$FINAL_DIST_DIR"
fi

mkdir -p "$(dirname "$OUTPUT_ZIP")"
rm -f "$OUTPUT_ZIP"
(
  cd "$TRANSLATOR_OUT/dist"
  zip -qr "$OUTPUT_ZIP" "$DIST_APP_NAME-js"
)

bj_log "Wrote browser bundle to $OUTPUT_ZIP"
