#!/usr/bin/env bash
#
# Builds what the Initializr generates from this project.
#
#   check-template.sh [work-directory]
#
# The Initializr offers this application as a template, from resources that
# scripts/sync-initializr-wayline.py derives from it. That the resources match
# is one question; this is the other: whether a project generated from them,
# under another name and in another package, still builds. The real generator
# makes the project, and it is then built against this checkout:
#
#   the server's tests pass against the contract module
#   the app builds from common, without platform modules and with them, and
#     the client half of the contract is generated into it
#
# Needs this checkout's artifacts installed (core, javase, css-compiler, the
# Maven plugin, backend, backend-test), a JDK 17 or newer as JAVA_HOME, python3
# and unzip.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
MVN="${MVN:-mvn}"
WORK="${1:-$HERE/target/template-check}"

log() { echo "[wayline-template] $*" >&2; }

python3 "$ROOT/scripts/sync-initializr-wayline.py"

# The version this checkout installs, which the generated poms are pointed at:
# the release the Initializr generates against does not have this project's
# build support until one that follows this change.
VERSION="$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' "$ROOT/maven/pom.xml" | head -n 1)"
[ -n "$VERSION" ] || { log "could not read the version from maven/pom.xml"; exit 1; }

rm -rf "$WORK"
mkdir -p "$WORK"
log "generating the Initializr's downloads"
python3 "$ROOT/scripts/tests/generate-initializr-fixtures.py" "$WORK/fixtures" > "$WORK/fixtures.log" 2>&1 \
    || { tail -n 60 "$WORK/fixtures.log" >&2; log "the generator failed"; exit 1; }

# extract <layout> -- a generated project, pointed at this checkout's version
extract() {
  local dir="$WORK/$1"
  mkdir -p "$dir"
  unzip -q "$WORK/fixtures/WAYLINE-$1-INTELLIJ.zip" -d "$dir"
  python3 - "$dir/pom.xml" "$VERSION" <<'PY'
import re, sys
path, version = sys.argv[1], sys.argv[2]
text = open(path, encoding='utf-8').read()
for tag in ('cn1.plugin.version', 'cn1.version'):
    text, count = re.subn(r'<%s>[^<]*</%s>' % (tag, tag), '<%s>%s</%s>' % (tag, version, tag), text)
    assert count == 1, (tag, count)
open(path, 'w', encoding='utf-8').write(text)
PY
  echo "$dir"
}

# The fixtures are generated as com.example.probe.LauncherProbe.
PACKAGE=com/example/probe

minimal="$(extract APP_WITH_BACKEND)"
log "running the generated server's tests"
(cd "$minimal" && "$MVN" -B -q -Dcodename1.platform=backend -pl shared,backend -am test)
ls "$minimal"/backend/target/surefire-reports/TEST-*.xml >/dev/null 2>&1 \
    || { log "the generated server's tests did not run"; exit 1; }
[ -f "$minimal/backend/target/generated-sources/cn1-annotations/$PACKAGE/api/RiderApiServer.java" ] \
    || { log "the server's half of the contract was not generated"; exit 1; }

for layout in APP_WITH_BACKEND FULL; do
  project="$WORK/$layout"
  [ -d "$project" ] || project="$(extract "$layout")"
  log "building the generated app ($layout)"
  (cd "$project" && "$MVN" -B -q -Dcodename1.platform=javase -Djava.awt.headless=true -DskipTests package)
  for built in "classes/theme.res" "classes/$PACKAGE/api/RiderApiImpl.class" "classes/cn1app/RestClientBootstrap.class"; do
    [ -f "$project/common/target/$built" ] \
        || { log "$layout: common/target/$built was not built"; exit 1; }
  done
done
log "passed"
