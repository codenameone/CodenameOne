#!/usr/bin/env bash
#
# Compiles the vendored SQLite WebAssembly module.
#
# The JavaScript port loads its engine at runtime, and a module the browser cannot compile does
# not fail the build: the port reports the engine as unavailable and every database test skips,
# which reads as "this platform has no SQLite" rather than "the bundled file is broken". That is
# how an upstream release built with the compact-imports encoding -- first import kind 0x7f,
# rejected by every shipping engine -- got as far as CI unnoticed.
#
# Compiling it here costs a second and turns that into a build failure with the reason attached.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
wasm="$repo_root/Ports/JavaScriptPort/src/main/webapp/js/sqlite3.wasm"

if [ ! -f "$wasm" ]; then
  echo "check-js-sqlite-wasm: $wasm is missing" >&2
  exit 1
fi

if ! command -v node >/dev/null 2>&1; then
  echo "check-js-sqlite-wasm: node is not on PATH; skipping" >&2
  exit 0
fi

node -e '
const fs = require("fs");
const path = process.argv[1];
const bytes = fs.readFileSync(path);
try {
  new WebAssembly.Module(bytes);
} catch (err) {
  console.error("check-js-sqlite-wasm: " + path + " does not compile: " + err.message);
  console.error("check-js-sqlite-wasm: see Ports/JavaScriptPort/src/main/webapp/js/README-sqlite3mc.md");
  process.exit(1);
}
console.log("check-js-sqlite-wasm: " + bytes.length + " bytes compile cleanly");
' "$wasm"
