#!/usr/bin/env bash
#
# Builds and runs the Wayline server.
#
#   server.sh build [--jvm|--native] [--web]
#   server.sh run   [--jvm|--native] [--web] [--prebuilt] [--port N]
#   server.sh web
#
# --jvm (the default) runs the server on this JDK, which is how you work on it.
# --native translates it to a binary, which is how it ships; that needs clang and
# the OpenSSL, libcurl and nghttp2 headers.
#
# The profile comes from CN1_PROFILE. `test` and `dev` need nothing else: an
# in-memory database, four demo accounts, a geocoder that knows ten places
# (`test`) and text messages that are logged, not sent. Anything else is a
# deployment and reads its settings from the environment -- see
# application.properties.
#
# `web` builds the app for the browser and stages it in target/webapp, which
# takes about a minute; `--web` does that as part of a build or a run. A server
# that finds the app there serves it at /, so http://localhost:8080/ is Wayline
# with nothing installed. It stays staged until the next `mvn clean`: build it
# once, and again when the app changed.
#
# The server prints "listening on http port N" when it accepts connections.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP="$(cd "$HERE/.." && pwd)"
# The project's own Maven wrapper where it has one.
if [ -z "${MVN:-}" ]; then
  if [ -x "$APP/mvnw" ]; then MVN="$APP/mvnw"; else MVN="mvn"; fi
fi

log() { echo "[wayline-server] $*" >&2; }

build_jvm() {
  log "building the server for the JVM"
  "$MVN" -B -q -f "$APP/pom.xml" -Dcodename1.platform=backend -pl shared,backend \
      -Dmaven.test.skip=true package dependency:copy-dependencies \
      -DincludeScope=runtime -DoutputDirectory="$APP/backend/target/server-lib" >&2
}

build_native() {
  log "translating the server to a native binary"
  "$MVN" -B -q -f "$APP/pom.xml" -Dcodename1.platform=backend -pl shared,backend \
      -Dmaven.test.skip=true package cn1:backend-package \
      -Dcn1.backend.output="$APP/backend/target/wayline-server" >&2
}

# Compiling the theme opens a window, so the browser build needs a display even
# though nothing is shown. A Linux machine without one (a CI runner, a container)
# gets a virtual display from xvfb-run when that is installed.
build_web() {
  log "building the app for the browser"
  local display=()
  if [ "$(uname -s)" = "Linux" ] && [ -z "${DISPLAY:-}" ]; then
    if command -v xvfb-run >/dev/null 2>&1; then
      display=(xvfb-run -a)
    else
      log "no display and no xvfb-run: the theme may fail to compile (install xvfb)"
    fi
  fi
  # Spelled so that an empty list is not an unset variable to the bash 3 on macOS.
  ${display[@]+"${display[@]}"} "$MVN" -B -q -f "$APP/pom.xml" -Dcodename1.platform=backend -pl backend \
      cn1:backend-webapp >&2
}

# The server looks for the app in `webapp` beside its settings; here it is build
# output, so it is under target and the server is told. An address set by the
# caller is left alone.
serve_web() {
  if [ -z "${CN1_WEBAPP_ROOT:-}" ] && [ -f "$APP/backend/target/webapp/index.html" ]; then
    export CN1_WEBAPP_ROOT="target/webapp"
    log "serving the web app in backend/target/webapp"
  fi
}

command="${1:-run}"
shift || true
mode="jvm"
port="${PORT:-8080}"
prebuilt=0
web=0
while [ $# -gt 0 ]; do
  case "$1" in
    --jvm) mode="jvm" ;;
    --native) mode="native" ;;
    --prebuilt) prebuilt=1 ;;
    --web) web=1 ;;
    --port) port="$2"; shift ;;
    *) log "unknown argument $1"; exit 2 ;;
  esac
  shift
done

case "$command" in
  build)
    if [ "$mode" = "native" ]; then build_native; else build_jvm; fi
    [ "$web" = 0 ] || build_web
    ;;
  web)
    build_web
    ;;
  run)
    # application.properties is read from the working directory.
    cd "$APP/backend"
    if [ "$mode" = "native" ]; then
      [ "$prebuilt" = 1 ] || build_native
      [ "$web" = 0 ] || [ "$prebuilt" = 1 ] || build_web
      serve_web
      PORT="$port" exec "$APP/backend/target/wayline-server"
    fi
    [ "$prebuilt" = 1 ] || build_jvm
    [ "$web" = 0 ] || [ "$prebuilt" = 1 ] || build_web
    serve_web
    main="$(tr -d '\r\n' < "$APP/backend/target/classes/META-INF/cn1-backend-main")"
    log "running $main on port $port"
    PORT="$port" exec java -cp "$APP/backend/target/classes:$APP/backend/target/server-lib/*" "$main"
    ;;
  *)
    log "unknown command $command (build, run, web)"
    exit 2
    ;;
esac
