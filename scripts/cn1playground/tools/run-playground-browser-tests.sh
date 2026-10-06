#!/bin/bash
# Builds the JavaScript Playground and checks it end to end in headless Chromium:
# the in-browser compiler and translator, loading user classes into the running VM,
# and the editor's input path. Needs Node with Playwright resolvable from scripts/
# (cd scripts && npm install playwright && npx playwright install chromium).
#
#   tools/run-playground-browser-tests.sh            build, then test
#   PLAYGROUND_SKIP_BUILD=1 tools/run-playground-browser-tests.sh   reuse the last build
#
# Extra Maven arguments (a -Dmaven.repo.local, say) go in PLAYGROUND_MVN_ARGS.
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

if [ "${PLAYGROUND_SKIP_BUILD:-}" != "1" ]; then
  # Same settings as build.sh and the website build: the bundle is open-world for the
  # API user code may call.
  CN1_TRANSLATOR_OPTS="${CN1_TRANSLATOR_OPTS:-$(cat javascript/translator-opts.txt)}" \
    mvn -nsu -q -B ${PLAYGROUND_MVN_ARGS:-} -pl javascript -am -DskipTests -Dautomated=true \
    -Dcodename1.platform=javascript package
fi

result_zip="$(ls -1 javascript/target/cn1playground-javascript-*.zip 2>/dev/null | head -n1 || true)"
if [ -z "$result_zip" ]; then
  echo "No Playground JavaScript bundle under javascript/target" >&2
  exit 1
fi
site="$ROOT/javascript/target/browser-test-site"
rm -rf "$site"
mkdir -p "$site"
unzip -q -o "$result_zip" -d "$site"
# The bundle may be wrapped in one top-level directory; serve its contents.
if [ ! -f "$site/index.html" ]; then
  inner="$(find "$site" -mindepth 2 -maxdepth 2 -name index.html | head -n1)"
  if [ -z "$inner" ]; then
    echo "The bundle has no index.html" >&2
    exit 1
  fi
  site="$(dirname "$inner")"
fi

port="${PLAYGROUND_BROWSER_PORT:-8793}"
python3 -m http.server "$port" --bind 127.0.0.1 --directory "$site" >/dev/null 2>&1 &
server=$!
trap 'kill "$server" 2>/dev/null || true' EXIT
for _ in $(seq 1 50); do
  if curl -fsS "http://127.0.0.1:$port/index.html" >/dev/null 2>&1; then
    break
  fi
  sleep 0.2
done

# Playwright resolves from scripts/node_modules, so run the checks from there.
cd "$ROOT/.."
node cn1playground/tools/verify-playground-browser.mjs "http://127.0.0.1:$port/index.html"
node cn1playground/tools/verify-lightweight-editor-input.mjs "http://127.0.0.1:$port/index.html"
