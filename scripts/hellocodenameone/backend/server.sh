#!/usr/bin/env bash
#
# Builds and runs the CI test server: scripts/hellocodenameone/backend.
#
#   server.sh build [--jvm|--native]                         build only
#   server.sh run  [--jvm|--native] [--prebuilt] [--port N] [--out DIR]
#                                                              build (unless --prebuilt), then run
#                                                              in the foreground
#   server.sh dist DIR                                         a runnable JVM copy, for another job
#   server.sh exec-dist DIR [--port N] [--out DIR]             run a copy made by `dist`
#
# One server for every device leg: it receives the suite's screenshots over a
# websocket and serves the REST surface the app's networking tests drive. It
# replaced vm/backend/demo/cn1ss and the hand-written scripts/common/java server.
#
# --jvm (the default) runs it on this JDK; --native translates it with ParparVM and
# runs the binary, which is how it ships -- it needs clang and the OpenSSL, libcurl
# and nghttp2 headers, so only a leg that installs them asks for it. A platform
# with no native backend (Windows) always runs the JVM build.
#
# The runtime it compiles against -- codenameone-backend, codenameone-backend-test
# and the plugin's processors -- is installed from this checkout first, once per
# source digest, because a CI cache can restore an 8.0-SNAPSHOT older than the
# branch under test. CN1SS_BACKEND_SKIP_INSTALL=1 skips that for a developer who
# installed them already. Maven honours MAVEN_OPTS, so a per-checkout
# -Dmaven.repo.local reaches every invocation here.
#
# Readiness: the server prints "listening on http port N" when it accepts.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../../.." && pwd)"
APP="$ROOT/scripts/hellocodenameone"

log() { echo "[cn1ss-backend] $*" >&2; }

mvn_cmd() {
  if [ -n "${MAVEN_HOME:-}" ] && [ -x "$MAVEN_HOME/bin/mvn" ]; then
    echo "$MAVEN_HOME/bin/mvn"
  elif command -v mvn >/dev/null 2>&1; then
    command -v mvn
  else
    log "no Maven: set MAVEN_HOME or put mvn on the PATH"
    return 1
  fi
}

# The JDK to build and run with: 17 when the leg has one (the app's toolchain),
# otherwise whatever JAVA_HOME names.
java_home() {
  if [ -n "${JAVA17_HOME:-}" ] && [ -x "$JAVA17_HOME/bin/java" ]; then
    echo "$JAVA17_HOME"
  elif [ -n "${JAVA_HOME:-}" ]; then
    echo "$JAVA_HOME"
  else
    dirname "$(dirname "$(command -v java)")"
  fi
}

# The JDK to INSTALL the runtime with: 8 when the leg names one. The framework
# build requires it -- core compiles for Java 5, which a newer javac refuses -- and
# the -am fallback below builds core.
install_java_home() {
  if [ -n "${JDK_8_HOME:-}" ] && [ -x "$JDK_8_HOME/bin/java" ]; then
    echo "$JDK_8_HOME"
  elif [ -n "${JAVA_HOME:-}" ]; then
    echo "$JAVA_HOME"
  else
    java_home
  fi
}

is_windows() {
  case "$(uname -s 2>/dev/null)" in
    MINGW*|MSYS*|CYGWIN*) return 0 ;;
  esac
  return 1
}

# Installs the backend runtime, its test support and the plugin, unless this
# exact source tree was installed already.
install_runtime() {
  if [ "${CN1SS_BACKEND_SKIP_INSTALL:-0}" = "1" ]; then
    return 0
  fi
  local digest stamp
  # Every module the server's build reads: the runtime and its test support, and
  # the plugin with the build engine and project model it delegates the annotation
  # and test passes to. A module left out of this digest is one whose change the
  # stamp hides, so the server builds with the stale copy from the local repository.
  digest="$(cd "$ROOT" && { find vm/backend/src vm/backend/impl vm/backend/native vm/backend/test \
      maven/backend maven/backend-test maven/codenameone-maven-plugin/src/main \
      maven/build-engine/src/main maven/build-engine/pom.xml \
      maven/project-model/src/main maven/project-model/pom.xml \
      maven/codenameone-maven-plugin/pom.xml maven/pom.xml \
      -type f -not -path '*/target/*' -print 2>/dev/null; vm/backend/shared-sources.sh 2>/dev/null; } \
      | LC_ALL=C sort | tr '\n' '\0' | xargs -0 cat 2>/dev/null | cksum | awk '{print $1}')"
  stamp="$ROOT/maven/target/cn1ss-backend-installed-$digest"
  if [ -f "$stamp" ]; then
    return 0
  fi
  log "installing the backend runtime, its test support, the build engine and the plugin from this checkout"
  # The three modules alone first: a leg that built the app has installed the
  # plugin's other dependencies (core, the ports, parparvm) from this checkout
  # already, and rebuilding every port with -am would cost minutes for nothing.
  # Where they are missing, the same install with -am builds them.
  local -a install=(-B -q -f "$ROOT/maven/pom.xml" -Plocal-dev-javase install
      -DskipTests -Dmaven.javadoc.skip=true -Dmaven.source.skip=true
      -Dspotbugs.skip=true -Dpmd.skip=true -Dcheckstyle.skip=true)
  if ! JAVA_HOME="$(install_java_home)" "$(mvn_cmd)" "${install[@]}" \
      -pl backend,backend-test,project-model,build-engine,codenameone-maven-plugin >&2; then
    log "the plugin's dependencies are not installed; building them too (-am)"
    JAVA_HOME="$(install_java_home)" "$(mvn_cmd)" "${install[@]}" \
        -pl backend,backend-test,project-model,build-engine,codenameone-maven-plugin -am >&2
  fi
  mkdir -p "$(dirname "$stamp")"
  touch "$stamp"
}

# Compiles the server and copies its runtime dependencies beside it.
build_jvm() {
  install_runtime
  log "building the server for the JVM"
  JAVA_HOME="$(java_home)" "$(mvn_cmd)" -B -q -f "$APP/pom.xml" -Dcodename1.platform=backend \
      -pl backend -Dmaven.test.skip=true package dependency:copy-dependencies \
      -DincludeScope=runtime -DoutputDirectory="$APP/backend/target/server-lib" >&2
}

build_native() {
  install_runtime
  log "translating the server to a native binary"
  JAVA_HOME="$(java_home)" "$(mvn_cmd)" -B -q -f "$APP/pom.xml" -Dcodename1.platform=backend \
      -pl backend -Dmaven.test.skip=true process-classes cn1:backend-package \
      -Dcn1.backend.output="$APP/backend/target/hellocodenameone-backend" >&2
}

# Runs a JVM build: classes, a lib directory and the generated main.
exec_jvm() {
  local classes="$1" lib="$2" port="$3" out="$4" sep=":" main cp
  main="$(tr -d '\r\n' < "$classes/META-INF/cn1-backend-main")"
  if is_windows; then
    sep=";"
    classes="$(cygpath -w "$classes")"
    lib="$(cygpath -w "$lib")"
  fi
  cp="$classes${sep}$lib/*"
  log "running $main on $(java_home)"
  cd "$APP/backend"
  CN1SS_OUT="$out" CN1_SERVER_PORT="$port" exec "$(java_home)/bin/java" -cp "$cp" "$main"
}

command="${1:-run}"
shift || true
mode="jvm"
port="${CN1SS_WS_BIND_PORT:-8765}"
out="${CN1SS_OUT:-$APP/backend/target/cn1ss-out}"
dist=""
prebuilt=0
case "$command" in
  dist|exec-dist)
    dist="${1:?usage: server.sh $command DIR}"
    shift
    ;;
esac
while [ $# -gt 0 ]; do
  case "$1" in
    --jvm) mode="jvm" ;;
    --native) mode="native" ;;
    --prebuilt) prebuilt=1 ;;
    --port) port="$2"; shift ;;
    --out) out="$2"; shift ;;
    *) log "unknown argument $1"; exit 2 ;;
  esac
  shift
done
if is_windows; then
  mode="jvm"
fi
mkdir -p "$out"

case "$command" in
  build)
    if [ "$mode" = "native" ]; then
      build_native
    else
      build_jvm
    fi
    ;;
  run)
    if [ "$mode" = "native" ]; then
      [ "$prebuilt" = 1 ] || build_native
      cd "$APP/backend"
      CN1SS_OUT="$out" CN1_SERVER_PORT="$port" exec "$APP/backend/target/hellocodenameone-backend"
    fi
    [ "$prebuilt" = 1 ] || build_jvm
    exec_jvm "$APP/backend/target/classes" "$APP/backend/target/server-lib" "$port" "$out"
    ;;
  dist)
    build_jvm
    rm -rf "$dist"
    mkdir -p "$dist"
    cp -R "$APP/backend/target/classes" "$dist/classes"
    cp -R "$APP/backend/target/server-lib" "$dist/lib"
    log "a runnable copy of the server is in $dist"
    ;;
  exec-dist)
    exec_jvm "$dist/classes" "$dist/lib" "$port" "$out"
    ;;
  *)
    log "unknown command $command (build, run, dist, exec-dist)"
    exit 2
    ;;
esac
