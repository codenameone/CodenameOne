#!/usr/bin/env bash
#
# capture-fixture.sh -- record a scrambled Bluetooth fixture from THIS
# machine's real radio via the cn1-ble-helper subprocess, for replay in
# the JavaSE simulator's virtual Bluetooth stack.
#
# Usage:
#   scripts/bluetooth/capture-fixture.sh --out fixture.json \
#       [--seconds 12] [--seed 42] [--gatt <address> ...] \
#       [--gatt-strongest] [--helper /path/to/cn1-ble-helper]
#
# All arguments are forwarded to
# com.codename1.impl.javase.bluetooth.FixtureCaptureMain (see its javadoc).
# The written JSON is ALWAYS scrambled (FixtureScrambler, deterministic by
# --seed) and verified leak-free before it is written; commit-ready
# fixtures live in maven/javase/src/test/resources/bluetooth-fixtures/.
#
# Prerequisites:
#   * the javase module compiled:  cd maven && mvn install -pl javase \
#         -Plocal-dev-javase -DskipTests   (or any full build)
#   * a cn1-ble-helper binary; by default the local cargo build at
#     Ports/JavaSE/native/cn1-ble-helper/target/release/cn1-ble-helper
#     is used (build it with: cargo build --release). Pass --helper to
#     override, or export CN1_BLE_HELPER.
#   * OS Bluetooth permission for the terminal (macOS: System Settings >
#     Privacy & Security > Bluetooth).
#
# Equivalent mvn incantation (no compiled target/classes needed):
#   cd maven && mvn -q -pl javase -Plocal-dev-javase compile \
#     org.codehaus.mojo:exec-maven-plugin:3.1.0:java \
#     -Dexec.mainClass=com.codename1.impl.javase.bluetooth.FixtureCaptureMain \
#     -Dexec.args="--out /tmp/fixture.json --seconds 12 --seed 42"
#
set -euo pipefail

CN1_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CLASSES="$CN1_ROOT/maven/javase/target/classes"
HELPER="${CN1_BLE_HELPER:-$CN1_ROOT/Ports/JavaSE/native/cn1-ble-helper/target/release/cn1-ble-helper}"

if [ ! -d "$CLASSES" ]; then
    echo "error: $CLASSES not found -- build first:" >&2
    echo "  cd maven && mvn install -pl javase -Plocal-dev-javase -DskipTests" >&2
    exit 1
fi

# the bluetooth core API: prefer the in-repo build output (always in sync
# with the sources), fall back to the newest installed jar
CORE_CP="$CN1_ROOT/maven/core/target/classes"
if [ ! -d "$CORE_CP" ]; then
    CORE_CP="$(ls -t "$HOME"/.m2/repository/com/codenameone/codenameone-core/*/codenameone-core-*.jar 2>/dev/null | grep -v -- '-sources\|-javadoc' | head -1 || true)"
fi
if [ -z "$CORE_CP" ]; then
    echo "error: codenameone-core not built -- build first:" >&2
    echo "  cd maven && mvn install -pl core -DskipTests" >&2
    exit 1
fi

HELPER_ARGS=()
if [ -f "$HELPER" ]; then
    HELPER_ARGS=(-Dcn1.bluetooth.helperPath="$HELPER")
fi

exec java -cp "$CLASSES:$CORE_CP" "${HELPER_ARGS[@]}" \
    com.codename1.impl.javase.bluetooth.FixtureCaptureMain "$@"
