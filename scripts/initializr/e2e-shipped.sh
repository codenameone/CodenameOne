#!/usr/bin/env bash
#
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Codename One designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# Builds and runs what start.codenameone.com hands a developer, as they would.
#
#   e2e-shipped.sh [--require-latest] [--no-ui] [TEMPLATE ...]
#
# TEMPLATE is BAREBONES, KOTLIN, WAYLINE, SAVOR, SKIRMISH, PARLEY, BASTION, REEL or PULSE; all of them
# by default. Every other check of the templates builds them against this
# checkout's 8.0-SNAPSHOT, which no developer has: the generated pom names the
# release the Initializr pins, and that is the only thing this script builds
# against. Nothing from this checkout is installed. Each download gets a Maven
# repository of its own, empty, so whatever it needs comes from the public
# repository through the project's own pom and wrapper.
#
# For each download:
#   - the values: cn1.version and cn1.plugin.version are the release the
#     Initializr pins, that release is published and complete, and nothing in a
#     build file names a SNAPSHOT of Codename One
#   - the app builds with ./mvnw, as the README says
# and for a full-stack template (an app with its server):
#   - the shortcuts are there: run-server.sh, run-server.bat, "Run Server"
#   - the server's own tests pass
#   - ./run-server.sh starts the server
#   - the sample's end-to-end suite (scripts/<name>/common/src/test) runs in the
#     download against that server: it signs in and drives the real screens.
#     The download is generated under the sample's own name and package, so the
#     tests compile in it unchanged. Its screenshots are captured but not held
#     to the sample's goldens, which were taken on the SNAPSHOT.
#
# --require-latest also fails when the pinned release is not the newest one
# published: the site is then offering an old release, which is what the bump
# after a release is for. --no-ui skips the end-to-end suite.
#
# Needs JDK 17 or newer as JAVA_HOME, python3, curl and unzip. The suite opens the
# simulator: xvfb-run is used where there is one.
set -euo pipefail

# Opt out of the build telemetry: the launchers and the plugin otherwise report
# these builds to production as if a new developer had made them.
export CN1_TELEMETRY=0

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
WORK="${E2E_WORK:-$HERE/target/e2e-shipped}"
REPO="https://repo.codenameone.com/maven2"
PORT="${E2E_PORT:-18090}"
REQUIRE_LATEST=0
UI=1
TEMPLATES=()
for arg in "$@"; do
  case "$arg" in
    --require-latest) REQUIRE_LATEST=1 ;;
    --no-ui) UI=0 ;;
    -*) echo "unknown option $arg" >&2; exit 2 ;;
    *) TEMPLATES+=("$arg") ;;
  esac
done
[ ${#TEMPLATES[@]} -gt 0 ] || TEMPLATES=(BAREBONES KOTLIN WAYLINE SAVOR SKIRMISH PARLEY BASTION REEL PULSE)

log() { echo "[e2e-shipped] $*" >&2; }
fail() { echo "[e2e-shipped] FAIL: $*" >&2; exit 1; }

GENERATOR="$ROOT/scripts/initializr/common/src/main/java/com/codename1/initializr/model/GeneratorModel.java"
PINNED="$(sed -n 's/.*private static final String CN1_PLUGIN_VERSION = "\([^"]*\)";.*/\1/p' "$GENERATOR")"
[ -n "$PINNED" ] || fail "could not read CN1_PLUGIN_VERSION from $GENERATOR"
log "the Initializr pins Codename One $PINNED"
since() {
  sed -n "s/.*static final String $1 = \"\([^\"]*\)\";.*/\1/p" "$GENERATOR"
}
FULL_STACK_SINCE="$(since FULL_STACK_SINCE)"
BASTION_SINCE="$(since BASTION_SINCE)"
REEL_SINCE="$(since REEL_SINCE)"
[ -n "$FULL_STACK_SINCE" ] && [ -n "$BASTION_SINCE" ] && [ -n "$REEL_SINCE" ] || fail "could not read the template floors from $GENERATOR"
# The oldest release a template's generated code compiles against: Bastion
# calls framework API that first shipped in BASTION_SINCE, Reel the protected
# playback API of REEL_SINCE.
template_since() {
  case "$1" in
    BASTION) echo "$BASTION_SINCE" ;;
    REEL) echo "$REEL_SINCE" ;;
    *) echo "$FULL_STACK_SINCE" ;;
  esac
}
# True when release $1 is $2 or newer (numeric, dot by dot).
at_least() {
  [ "$(printf '%s\n%s\n' "$2" "$1" | sort -t. -k1,1n -k2,2n -k3,3n | head -n 1)" = "$2" ]
}

# Published, and whole: the release workflow writes this marker last.
cb="$(date +%s)"
code="$(curl -s -o /dev/null -w '%{http_code}' "$REPO/com/codenameone/_cn1-releases/$PINNED/complete?cb=$cb")"
[ "$code" = 200 ] || fail "release $PINNED is not complete on $REPO (marker: HTTP $code)"
latest="$(curl -fsSL "$REPO/com/codenameone/codenameone-maven-plugin/maven-metadata.xml?cb=$cb" \
    | sed -n 's:.*<release>\(.*\)</release>.*:\1:p' | head -n 1)"
if [ "$latest" != "$PINNED" ]; then
  if [ "$REQUIRE_LATEST" = 1 ]; then
    fail "the newest release is $latest but the Initializr still generates $PINNED; merge the version bump"
  fi
  log "note: the newest release is $latest; the Initializr generates $PINNED"
fi

rm -rf "$WORK"
mkdir -p "$WORK"
log "generating the downloads"
python3 "$ROOT/scripts/tests/generate-initializr-fixtures.py" "$WORK/zips" --shipped > "$WORK/generate.log" 2>&1 \
    || { tail -n 40 "$WORK/generate.log" >&2; fail "the generator failed"; }

RUNNER=()
if command -v xvfb-run >/dev/null 2>&1; then
  RUNNER=(xvfb-run -a -s "-screen 0 1280x1024x24")
fi

SERVER_PID=""
# run-server.sh execs Maven, which forks the server: end the whole tree.
kill_tree() {
  local child
  for child in $(pgrep -P "$1" 2>/dev/null); do kill_tree "$child"; done
  kill "$1" >/dev/null 2>&1 || true
}
stop_server() {
  if [ -n "$SERVER_PID" ]; then
    kill_tree "$SERVER_PID"
    wait "$SERVER_PID" 2>/dev/null || true
    SERVER_PID=""
  fi
}
trap stop_server EXIT

check_values() {
  local dir="$1" name="$2"
  python3 - "$dir" "$PINNED" "$name" "$(template_since "$name")" <<'PY'
import pathlib, re, sys
root, pinned, name, floor = pathlib.Path(sys.argv[1]), sys.argv[2], sys.argv[3], sys.argv[4]
def release(v):
    assert re.fullmatch(r'[0-9]+(\.[0-9]+)*', v), '%s: %s is not a release version' % (name, v)
    return tuple(int(p) for p in v.split('.'))
pom = (root / 'pom.xml').read_text(encoding='utf-8')
for tag in ('cn1.version', 'cn1.plugin.version'):
    found = re.findall(r'<%s>([^<]*)</%s>' % (re.escape(tag), re.escape(tag)), pom)
    assert found == [pinned], '%s: pom.xml %s is %s, the Initializr pins %s' % (name, tag, found, pinned)
    # A release, never a SNAPSHOT, and one that has the API the template calls.
    assert release(found[0]) >= release(floor), '%s: pom.xml %s is %s, older than %s, the first release with the API it uses' % (name, tag, found[0], floor)
assert 'https://repo.codenameone.com/maven2' in pom, name + ': pom.xml does not name the Codename One repository'
# A SNAPSHOT of Codename One anywhere a build reads it. The project's own
# 1.0-SNAPSHOT is fine; the agent skill's notes on building from source are docs.
for path in root.rglob('*'):
    if not path.is_file() or path.suffix not in ('.xml', '.properties', '.sh', '.bat', '.cmd', '.kts', '.json'):
        continue
    if '.agent-skills' in path.parts or '.claude' in path.parts:
        continue
    text = path.read_text(encoding='utf-8', errors='replace')
    for m in re.finditer(r'[0-9]+\.[0-9]+(?:\.[0-9]+)?-SNAPSHOT', text):
        assert m.group(0) == '1.0-SNAPSHOT', '%s: %s names %s' % (name, path.relative_to(root), m.group(0))
PY
}

for template in "${TEMPLATES[@]}"; do
  zip="$WORK/zips/SHIPPED-$template.zip"
  floor="$(template_since "$template")"
  if [ -f "$WORK/zips/NOT-OFFERED-$template" ]; then
    # Withheld is right only while the pinned release predates the template's
    # floor; withheld at or past it means the gate is wrong.
    if at_least "$PINNED" "$floor"; then
      fail "$template: not offered at $PINNED, although it needs only $floor"
    fi
    log "$template: skipped, the Initializr does not offer it at $PINNED (it needs $floor)"
    continue
  fi
  at_least "$PINNED" "$floor" || fail "$template: offered at $PINNED, older than $floor, the first release with the API it uses"
  [ -f "$zip" ] || fail "no download for $template"
  dir="$WORK/$template"
  mkdir -p "$dir"
  unzip -q "$zip" -d "$dir"
  m2="$WORK/m2-$template"
  # Every Maven the download starts -- the wrapper, the forks it makes, the
  # launchers -- reads its repository from here and from nowhere else.
  export MAVEN_OPTS="-Dmaven.repo.local=$m2"
  log "$template: checking the values"
  check_values "$dir" "$template"

  log "$template: building the app"
  (cd "$dir" && ./mvnw -B -q -Dcodename1.platform=javase -Djava.awt.headless=true -DskipTests package) \
      > "$WORK/$template-build.log" 2>&1 || { tail -n 60 "$WORK/$template-build.log" >&2; fail "$template: the app does not build"; }
  [ -f "$dir/common/target/classes/theme.res" ] || fail "$template: the theme was not compiled"

  [ -d "$dir/backend" ] && [ -d "$dir/shared" ] || { log "$template: passed"; continue; }

  for shortcut in run-server.sh run-server.bat .idea/runConfigurations/Run_Server.xml; do
    [ -f "$dir/$shortcut" ] || fail "$template: no $shortcut"
  done
  [ -x "$dir/run-server.sh" ] || fail "$template: run-server.sh is not executable"

  log "$template: running the server's tests"
  (cd "$dir" && ./mvnw -B -q -pl backend -am -Dcodename1.platform=backend test) \
      > "$WORK/$template-server-tests.log" 2>&1 \
      || { tail -n 80 "$WORK/$template-server-tests.log" >&2; fail "$template: the server's tests fail"; }

  log "$template: starting the server with ./run-server.sh"
  server_log="$WORK/$template-server.log"
  # A server left on the port would answer the readiness probe for this one.
  if curl -fs -o /dev/null "http://localhost:$PORT/oauth2/jwks"; then
    fail "$template: something already answers on port $PORT; stop it or set E2E_PORT"
  fi
  (cd "$dir" && CN1_PROFILE=test PORT="$PORT" exec ./run-server.sh) > "$server_log" 2>&1 &
  SERVER_PID=$!
  ready=0
  for _ in $(seq 1 300); do
    if curl -fs -o /dev/null "http://localhost:$PORT/oauth2/jwks"; then ready=1; break; fi
    kill -0 "$SERVER_PID" 2>/dev/null || break
    sleep 1
  done
  [ "$ready" = 1 ] || { tail -n 80 "$server_log" >&2; fail "$template: ./run-server.sh did not start the server"; }

  if [ "$UI" = 1 ]; then
    lower="$(echo "$template" | tr '[:upper:]' '[:lower:]')"
    sample="$ROOT/scripts/$lower"
    [ -d "$sample/common/src/test" ] || fail "$template: $sample has no end-to-end suite"
    cp -R "$sample/common/src/test" "$dir/common/src/"
    log "$template: running the sample's end-to-end suite in the download"
    shots="$WORK/$template-shots"
    mkdir -p "$shots"
    # Surefire 3.0.0-M5, which the generated pom pins because Codename One's
    # provider is built against its API, resolves that provider from Maven
    # Central alone. The plugin's jars came from repo.codenameone.com, so the
    # resolver refuses them as "present, but unavailable" and every test run of
    # a project on a release fails before a test starts. Reading the origin
    # records under another name makes the jars already downloaded count as
    # local. This is a workaround for the suite only: the plugin needs a provider
    # for a current Surefire, and until it has one a developer's own `mvn test`
    # fails the same way.
    set +e
    (cd "$dir" && JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Xmx1536m -D$lower.server.url=http://localhost:$PORT -D$lower.shots.dir=$shots" \
        ${RUNNER[@]+"${RUNNER[@]}"} ./mvnw -B -Daether.enhancedLocalRepository.trackingFilename=_e2e.repositories -Dcodename1.platform=javase -Ptest verify) > "$WORK/$template-e2e.log" 2>&1
    status=$?
    set -e
    if [ "$status" -ne 0 ]; then
      grep -n "Assert failed\|Exception\|failed$" "$WORK/$template-e2e.log" | head -n 40 >&2 || true
      tail -n 40 "$server_log" >&2
      fail "$template: the end-to-end suite fails against the released framework (log: $WORK/$template-e2e.log)"
    fi
  fi
  stop_server
  log "$template: passed"
done
log "every download builds and runs on Codename One $PINNED"
