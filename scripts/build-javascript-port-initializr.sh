#!/usr/bin/env bash
set -euo pipefail

bj_log() { echo "[build-javascript-port-initializr] $1"; }

usage() {
  cat <<'EOF' >&2
Usage: build-javascript-port-initializr.sh [output_zip]

Builds a ParparVM-backed browser bundle for scripts/initializr using:
  - scripts/initializr/common
  - scripts/initializr/cn1libs (ZipSupport, CodeRAD, ...)
  - Ports/JavaScriptPort runtime sources
  - vm/ByteCodeTranslator via maven/parparvm

Environment:
  SKIP_MAVEN_BUILD=1      Reuse existing target outputs instead of rebuilding
  SKIP_PARPARVM_BUILD=1   Reuse existing maven/parparvm target outputs
  SKIP_COMMON_BUILD=1     Reuse existing scripts/initializr/common target outputs
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_ROOT="$REPO_ROOT/scripts/initializr"
COMMON_ROOT="$APP_ROOT/common"
PORT_ROOT="$REPO_ROOT/Ports/JavaScriptPort"
PARPARVM_ROOT="$REPO_ROOT/maven/parparvm"
APP_NATIVE_JS_ROOT="$APP_ROOT/javascript/src/main/javascript"
OUTPUT_ZIP="${1:-$APP_ROOT/javascript/target/initializr-javascript-port.zip}"

APP_MAIN_CLASS="com.codename1.initializr.Initializr"
APP_MAIN_SIMPLE="${APP_MAIN_CLASS##*.}"
APP_PACKAGE="com.codename1.initializr"
TRANSLATOR_APP_NAME="InitializrJavaScriptMain"
DIST_APP_NAME="Initializr"

TMPDIR="${TMPDIR:-/tmp}"
TMPDIR="${TMPDIR%/}"
WORK_DIR="$(mktemp -d "${TMPDIR}/cn1-jsport-initializr-XXXXXX" 2>/dev/null || echo "${TMPDIR}/cn1-jsport-initializr")"
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

# The CSS goal of codenameone-maven-plugin spawns codenameone-designer.jar in
# CLI mode, which still loads enough of the JavaSE port (and on some CSS files,
# CEF) to need an X display. Headless CI runners (e.g. ubuntu-latest) ship
# xvfb-run for exactly this — wrap mvn invocations in it when present so the
# designer can initialise without an attached display.
run_with_display() {
  if [ -z "${DISPLAY:-}" ] && command -v xvfb-run >/dev/null 2>&1; then
    xvfb-run -a "$@"
  else
    "$@"
  fi
}

if [ "${SKIP_MAVEN_BUILD:-0}" != "1" ] && [ "${SKIP_PARPARVM_BUILD:-0}" != "1" ]; then
  # Build + install all the 8.0-SNAPSHOT jars the translator step needs:
  # codenameone-parparvm (compiler + java-api bundles), codenameone-core and
  # java-runtime. parparvm's pom doesn't declare core/runtime as deps, so
  # `-pl parparvm -am` alone doesn't install them — we list them explicitly.
  # `install` (not `package`) so these land in ~/.m2/repository where the
  # find-by-path lookups below pick them up. Locally most developers already
  # have these installed from setup-workspace.sh, which masked this in CI.
  bj_log "Building and installing ParparVM compiler bundle + core + java-runtime"
  run_with_display mvn -B -f "$REPO_ROOT/maven/pom.xml" \
    -pl parparvm,core,java-runtime -am \
    -DskipTests -Dmaven.javadoc.skip=true install
fi

if [ "${SKIP_MAVEN_BUILD:-0}" != "1" ] && [ "${SKIP_COMMON_BUILD:-0}" != "1" ]; then
  bj_log "Building Initializr common module and compile-scope dependencies"
  mkdir -p "$HOME/.codenameone"
  if [ -f "$REPO_ROOT/maven/UpdateCodenameOne.jar" ]; then
    cp "$REPO_ROOT/maven/UpdateCodenameOne.jar" "$HOME/.codenameone/" 2>/dev/null || true
  fi
  (
    cd "$APP_ROOT"
    # No -q here: the codenameone-maven-plugin's css goal runs the designer
    # as a forked Java process and only surfaces its stderr through INFO-level
    # maven output, so suppressing it makes any failure undebuggable.
    run_with_display sh ./mvnw -B -U -pl common -am -DskipTests -Dautomated=true -Dcodename1.platform=javascript \
      package dependency:copy-dependencies -DincludeScope=compile -DoutputDirectory=common/target/parparvm-deps
  )
fi

COMMON_CLASSES="$COMMON_ROOT/target/classes"
COMMON_DEPS_DIR="$COMMON_ROOT/target/parparvm-deps"
PARPARVM_JAVA_API="$PARPARVM_ROOT/target/bundle/parparvm-java-api.jar"
PARPARVM_COMPILER="$PARPARVM_ROOT/target/bundle/parparvm-compiler.jar"
CN1_CORE_JAR="$(find "$HOME/.m2/repository/com/codenameone/codenameone-core" -path '*/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT.jar' -type f 2>/dev/null | head -n 1 || true)"
JAVA_RUNTIME_JAR="$(find "$HOME/.m2/repository/com/codenameone/java-runtime" -path '*/8.0-SNAPSHOT/java-runtime-8.0-SNAPSHOT.jar' -type f 2>/dev/null | head -n 1 || true)"

check_required() {
  local label="$1"
  local path="$2"
  if [ -z "$path" ] || [ ! -e "$path" ]; then
    bj_log "Required build artifact missing for $label (resolved path: '${path:-<empty>}')" >&2
    exit 3
  fi
}
check_required "common classes directory"        "$COMMON_CLASSES"
check_required "ParparVM Java API bundle"        "$PARPARVM_JAVA_API"
check_required "ParparVM compiler bundle"        "$PARPARVM_COMPILER"
check_required "codenameone-core 8.0-SNAPSHOT"   "$CN1_CORE_JAR"
check_required "java-runtime 8.0-SNAPSHOT"       "$JAVA_RUNTIME_JAR"

STAGE_CLASSES="$WORK_DIR/stage-classes"
PORT_CLASSES="$WORK_DIR/port-classes"
SOURCE_LIST="$WORK_DIR/javascript-port-sources.txt"
LAUNCHER_SRC="$WORK_DIR/InitializrJavaScriptMain.java"
NATIVE_IMPL_SRC="$WORK_DIR/WebsiteThemeNativeImpl.java"
NATIVE_IMPL_DIR="$WORK_DIR/native-impl-src/com/codename1/initializr"
TRANSLATOR_OUT="$WORK_DIR/translator-output"
mkdir -p "$STAGE_CLASSES" "$PORT_CLASSES" "$TRANSLATOR_OUT" "$NATIVE_IMPL_DIR"

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

# Pull in cn1lib runtime jars (ZipSupport, CodeRAD, ...) plus any compile-scope
# deps that dependency:copy-dependencies materialised for the common module.
# TeaVM jars are handled separately below, so skip them here.
if [ -d "$COMMON_DEPS_DIR" ]; then
  while IFS= read -r -d '' jar_file; do
    jar_name="$(basename "$jar_file")"
    case "$jar_name" in
      teavm-*.jar)
        # TeaVM runtime bits are staged later via the TeaVM code path.
        continue
        ;;
      codenameone-core-*.jar|java-runtime-*.jar|codenameone-javase-*.jar|parparvm-*.jar)
        # Already staged from the pinned 8.0-SNAPSHOT artifacts above.
        continue
        ;;
    esac
    bj_log "Including dependency classes from $jar_name"
    (
      cd "$STAGE_CLASSES"
      "$JAR_BIN" xf "$jar_file"
    )
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

bj_log "Preparing JavaScript-port launcher"
# Force the WebsiteThemeNativeImpl class into the translator's reachability
# graph and register it with NativeLookup so create() returns our hardcoded
# JS-port stub instead of falling through to Class.forName.
if [ "$TEAVM_AVAILABLE" -eq 1 ]; then
  cat > "$LAUNCHER_SRC" <<EOF
import com.codename1.impl.html5.JavaScriptPortBootstrap;
import com.codename1.system.NativeLookup;
import com.codename1.initializr.WebsiteThemeNative;
import com.codename1.initializr.WebsiteThemeNativeImpl;
import $APP_MAIN_CLASS;

public final class $TRANSLATOR_APP_NAME {
    public static void main(String[] args) {
        NativeLookup.register(WebsiteThemeNative.class, WebsiteThemeNativeImpl.class);
        JavaScriptPortBootstrap.bootstrap(new $APP_MAIN_SIMPLE());
    }
}
EOF
else
  cat > "$LAUNCHER_SRC" <<EOF
import com.codename1.impl.html5.ParparVMBootstrap;
import com.codename1.system.NativeLookup;
import com.codename1.initializr.WebsiteThemeNative;
import com.codename1.initializr.WebsiteThemeNativeImpl;
import $APP_MAIN_CLASS;

public final class $TRANSLATOR_APP_NAME {
    public static void main(String[] args) {
        NativeLookup.register(WebsiteThemeNative.class, WebsiteThemeNativeImpl.class);
        ParparVMBootstrap.bootstrap(new $APP_MAIN_SIMPLE());
    }
}
EOF
fi

# Hardcoded glue: generate WebsiteThemeNativeImpl so NativeLookup.create() can
# resolve com.codename1.initializr.WebsiteThemeNative on the JS port. The
# generic native-interface builder lives only in the JavaScriptBuilder cloud
# tooling, which we no longer go through. Until the new port grows an
# equivalent generator, app-specific stubs are good enough.
bj_log "Generating Initializr native-interface impl stub"
cat > "$NATIVE_IMPL_DIR/WebsiteThemeNativeImpl.java" <<'EOF'
package com.codename1.initializr;

/**
 * Hardcoded JS-port stub for WebsiteThemeNative. Each method bridges to a
 * static native that worker-side bindings (initializr_native_bindings.js)
 * forward to the host-thread impl loaded from
 * native/com_codename1_initializr_WebsiteThemeNative.js via
 * cn1HostBridge / cn1_get_native_interfaces().
 */
public final class WebsiteThemeNativeImpl implements WebsiteThemeNative {
    public boolean isDarkMode() {
        return nativeIsDarkMode();
    }

    public void notifyUiReady() {
        nativeNotifyUiReady();
    }

    public boolean isSupported() {
        return nativeIsSupported();
    }

    private static native boolean nativeIsDarkMode();
    private static native void nativeNotifyUiReady();
    private static native boolean nativeIsSupported();
}
EOF

bj_log "Building source list for JavaScriptPort"
find "$PORT_ROOT/src/main/java" -type f -name '*.java' ! -name 'Stub.java' | sort > "$SOURCE_LIST"
echo "$LAUNCHER_SRC" >> "$SOURCE_LIST"
echo "$NATIVE_IMPL_DIR/WebsiteThemeNativeImpl.java" >> "$SOURCE_LIST"

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

bj_log "Compiling JavaScript-port runtime sources"
"$JAVAC_BIN" -source 8 -target 8 -cp "$CLASSPATH" -d "$PORT_CLASSES" @"$SOURCE_LIST"
cp -R "$PORT_CLASSES"/. "$STAGE_CLASSES"/

bj_log "Running ByteCodeTranslator for $DIST_APP_NAME"
"$JAVA_BIN" -cp "$PARPARVM_COMPILER" com.codename1.tools.translator.ByteCodeTranslator \
  javascript \
  "$STAGE_CLASSES" \
  "$TRANSLATOR_OUT" \
  "$TRANSLATOR_APP_NAME" \
  "$APP_PACKAGE" \
  "$DIST_APP_NAME" \
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

# Copy the app icon into the bundle root. The Hugo website template
# (docs/website/layouts/_default/initializr.html) references
# /initializr-app/icon.png for the page header, and the previous TeaVM-based
# bundle included it at the root by default. The new ParparVM pipeline does
# not, so stage it here from the common module.
if [ -f "$COMMON_ROOT/icon.png" ]; then
  cp "$COMMON_ROOT/icon.png" "$DIST_DIR/icon.png"
  bj_log "Staged icon.png at bundle root"
fi

# Stage application-level native-interface JS shims alongside the bundle so
# the host bridge can dispatch into them via cn1_get_native_interfaces().
if [ -d "$APP_NATIVE_JS_ROOT" ]; then
  native_js_count=0
  mkdir -p "$DIST_DIR/native"
  while IFS= read -r -d '' native_js; do
    cp "$native_js" "$DIST_DIR/native/"
    native_js_count=$((native_js_count + 1))
  done < <(find "$APP_NATIVE_JS_ROOT" -maxdepth 1 -type f -name '*.js' -print0)
  if [ "$native_js_count" -gt 0 ]; then
    bj_log "Staged $native_js_count app native-interface JS file(s) under native/"
  fi
fi

# --- Hardcoded glue: wire WebsiteThemeNative through the host bridge. ---
# Worker side: bind the static natives declared on WebsiteThemeNativeImpl so
# they yield to invokeHostNative() and resume with the value the host returns.
bj_log "Writing initializr_native_bindings.js (worker side)"
cat > "$DIST_DIR/initializr_native_bindings.js" <<'EOF'
// Hardcoded JS-port glue for com.codename1.initializr.WebsiteThemeNative.
// Imported by worker.js after parparvm_runtime.js / translated_app.js so the
// generated WebsiteThemeNativeImpl class' static natives forward to the host
// thread, where the JS impl loaded via native/com_codename1_initializr_*.js
// runs against the real DOM/window.
(function() {
  if (typeof bindNative !== "function" || typeof jvm === "undefined") {
    return;
  }
  function bindBoolean(symbols, hostKey) {
    bindNative(symbols, function*() {
      var result = yield jvm.invokeHostNative(hostKey, []);
      return !!result;
    });
  }
  function bindVoid(symbols, hostKey) {
    bindNative(symbols, function*() {
      yield jvm.invokeHostNative(hostKey, []);
      return null;
    });
  }
  bindBoolean([
    "cn1_com_codename1_initializr_WebsiteThemeNativeImpl_nativeIsDarkMode_R_boolean",
    "cn1_com_codename1_initializr_WebsiteThemeNativeImpl_nativeIsDarkMode___R_boolean"
  ], "initializr.WebsiteThemeNative.isDarkMode");
  bindBoolean([
    "cn1_com_codename1_initializr_WebsiteThemeNativeImpl_nativeIsSupported_R_boolean",
    "cn1_com_codename1_initializr_WebsiteThemeNativeImpl_nativeIsSupported___R_boolean"
  ], "initializr.WebsiteThemeNative.isSupported");
  bindVoid([
    "cn1_com_codename1_initializr_WebsiteThemeNativeImpl_nativeNotifyUiReady",
    "cn1_com_codename1_initializr_WebsiteThemeNativeImpl_nativeNotifyUiReady__"
  ], "initializr.WebsiteThemeNative.notifyUiReady");
})();
EOF

# Patch worker.js to importScripts the worker-side bindings file. The
# translator's JavascriptBundleWriter.writeWorker() already enumerated
# top-level *.js files in DIST_DIR before our bindings landed, so we splice
# the missing import in by hand.
if ! grep -q "initializr_native_bindings.js" "$DIST_DIR/worker.js"; then
  if grep -q "importScripts('translated_app.js');" "$DIST_DIR/worker.js"; then
    awk '
      { print }
      /^importScripts\('"'"'translated_app\.js'"'"'\);/ && !done {
        print "importScripts('"'"'initializr_native_bindings.js'"'"');";
        done = 1;
      }
    ' "$DIST_DIR/worker.js" > "$DIST_DIR/worker.js.patched"
    mv "$DIST_DIR/worker.js.patched" "$DIST_DIR/worker.js"
    bj_log "Patched worker.js to load initializr_native_bindings.js"
  else
    bj_log "WARNING: worker.js missing importScripts('translated_app.js') anchor; native bindings will not load." >&2
  fi
fi

# Main side: register host-bridge handlers that forward to the JS impl
# registered in cn1_get_native_interfaces() by the staged native/ scripts.
bj_log "Writing initializr_native_handlers.js (main thread)"
cat > "$DIST_DIR/initializr_native_handlers.js" <<'EOF'
// Hardcoded JS-port glue: wire host-bridge symbols emitted from the worker
// (initializr_native_bindings.js) to the WebsiteThemeNative JS impl loaded
// from native/com_codename1_initializr_WebsiteThemeNative.js. Loaded after
// browser_bridge.js so cn1HostBridge already exists.
(function(global) {
  function ensureBridge() {
    var bridge = global.cn1HostBridge;
    if (!bridge) {
      bridge = global.cn1HostBridge = {
        handlers: {},
        register: function(symbol, handler) { this.handlers[symbol] = handler; },
        invoke: function(symbol, args) {
          var h = this.handlers[symbol];
          return h ? h.apply(null, args || []) : null;
        }
      };
    }
    return bridge;
  }
  function getImpl() {
    if (typeof cn1_get_native_interfaces !== "function") {
      return null;
    }
    var registry = cn1_get_native_interfaces();
    return registry ? registry["com_codename1_initializr_WebsiteThemeNative"] : null;
  }
  function bridgeMethod(symbol, methodName, defaultValue) {
    ensureBridge().register(symbol, function() {
      var impl = getImpl();
      if (!impl || typeof impl[methodName] !== "function") {
        return defaultValue;
      }
      return new Promise(function(resolve, reject) {
        try {
          impl[methodName]({
            complete: function(value) { resolve(value); },
            error: function(err) { reject(err || new Error("native callback error")); }
          });
        } catch (err) {
          reject(err);
        }
      });
    });
  }
  bridgeMethod("initializr.WebsiteThemeNative.isDarkMode", "isDarkMode_", false);
  bridgeMethod("initializr.WebsiteThemeNative.isSupported", "isSupported_", false);
  bridgeMethod("initializr.WebsiteThemeNative.notifyUiReady", "notifyUiReady_", null);
})(typeof window !== "undefined" ? window : self);
EOF

# Patch index.html to load the JS impl + host-bridge handlers around
# browser_bridge.js. Order matters: fontmetrics.js (already first) defines
# cn1_get_native_interfaces; the native impl registers itself there;
# browser_bridge sets up cn1HostBridge; then handlers register against it.
if [ -f "$DIST_DIR/index.html" ] && ! grep -q "initializr_native_handlers.js" "$DIST_DIR/index.html"; then
  awk '
    {
      if ($0 ~ /<script src="browser_bridge.js"><\/script>/ && !done) {
        print "<script src=\"native/com_codename1_initializr_WebsiteThemeNative.js\"></script>";
        print $0;
        print "<script src=\"initializr_native_handlers.js\"></script>";
        done = 1;
      } else {
        print;
      }
    }
  ' "$DIST_DIR/index.html" > "$DIST_DIR/index.html.patched"
  mv "$DIST_DIR/index.html.patched" "$DIST_DIR/index.html"
  bj_log "Patched index.html to load native impl and host-bridge handlers"
fi

# --- Post-translation minimisation pass -------------------------------------
# A raw ByteCodeTranslator JS bundle for Initializr is ~90 MiB and consists
# overwhelmingly of repeated long identifiers (e.g. "cn1_com_codename1_ui_
# Form_setTitle_java_lang_String" appears thousands of times as both a
# function name and an explicit string literal). esbuild can only mangle
# local variables and whitespace — the repeated identifiers are string
# literals it cannot touch. A dedicated cross-file identifier mangler +
# esbuild after it cuts the output from ~90 MiB to ~20 MiB (brotli: ~1.6
# MiB on the wire), which is what Cloudflare Pages actually uploads.
#
# Both passes are best-effort: if Python or npx/esbuild is missing we just
# emit the unminified bundle so this script still works in development
# environments that don't have the toolchain.
if [ "${SKIP_JS_MINIFICATION:-0}" != "1" ]; then
  # Identifier mangling is on by default: raw translator output for
  # Initializr sits around 50 MiB on the wire once split+minified, and the
  # overwhelming cost is string literals like
  # "cn1_com_codename1_ui_Form_setTitle_java_lang_String" appearing thousands
  # of times. The mangler rewrites each ``cn1_*`` / class-name literal to a
  # short ``$a`` token across every worker-side file in lockstep (including
  # the ``X__impl`` twin for any mangled ``X`` so runtime ``methodId +
  # "__impl"`` lookups still resolve). Set ``DISABLE_JS_IDENT_MANGLING=1``
  # to skip the mangle pass when debugging the unmangled symbol names.
  if [ "${DISABLE_JS_IDENT_MANGLING:-0}" != "1" ] && command -v python3 >/dev/null 2>&1; then
    bj_log "Mangling cn1_* / class-name identifiers across worker-side JS"
    # Write the mangle map next to the zip (not inside the shipped bundle)
    # so stack traces can be demangled without paying a ~6 MiB cost on every
    # page load.
    map_path="$(dirname "$OUTPUT_ZIP")/$(basename "$OUTPUT_ZIP" .zip).mangle-map.json"
    mkdir -p "$(dirname "$map_path")"
    python3 "$SCRIPT_DIR/mangle-javascript-port-identifiers.py" \
      --map-output "$map_path" "$DIST_DIR" || \
      bj_log "WARNING: identifier mangling failed; continuing with unmangled output" >&2
  fi

  # Minify each worker-side JS file in place with esbuild. We deliberately
  # skip browser_bridge.js / port.js / native_handlers (hand-written,
  # main-thread glue that we want to keep readable for integration debugging).
  if command -v npx >/dev/null 2>&1; then
    bj_log "Minifying translated JS chunks with esbuild"
    minified_count=0
    for js in "$DIST_DIR"/*.js; do
      name="$(basename "$js")"
      case "$name" in
        browser_bridge.js|port.js|worker.js|sw.js) continue ;;
        *_native_handlers.js) continue ;;
      esac
      if npx --yes esbuild --minify --log-level=error --allow-overwrite \
          --target=es2020 "$js" --outfile="$js" >/dev/null 2>&1; then
        minified_count=$((minified_count + 1))
      else
        bj_log "WARNING: esbuild minify failed for $name; leaving it as-is" >&2
      fi
    done
    bj_log "Minified $minified_count JS file(s) via esbuild"
  else
    bj_log "npx not found; skipping esbuild minification"
  fi
fi
# ---------------------------------------------------------------------------

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
