#!/usr/bin/env bash
# Runs Cn1ssAsyncSinkTest: the JavaScript port's screenshot sink state machine
# (scripts/hellocodenameone/.../tests/Cn1ssAsyncSink.java) against a fake socket,
# on a plain JVM -- no browser, no build.
#
# Also fails when the fidelity app's copy of the class differs from the
# hellocodenameone one by anything but its package line: the two apps share the
# sink, and only one copy is exercised here.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
root="$(cd "$here/../../.." && pwd)"
hello="$root/scripts/hellocodenameone/common/src/main/java/com/codenameone/examples/hellocodenameone/tests/Cn1ssAsyncSink.java"
fidelity="$root/scripts/fidelity-app/common/src/main/java/com/codenameone/fidelity/Cn1ssAsyncSink.java"

if ! diff <(grep -v '^package ' "$hello") <(grep -v '^package ' "$fidelity") >/dev/null; then
  echo "FAIL: $fidelity has drifted from $hello (they must differ only in the package line)" >&2
  diff <(grep -v '^package ' "$hello") <(grep -v '^package ' "$fidelity") >&2 || true
  exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac -encoding ascii -d "$work" "$hello" "$here/Cn1ssAsyncSinkTest.java"
java -cp "$work" Cn1ssAsyncSinkTest
