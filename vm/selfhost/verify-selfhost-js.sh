#!/bin/bash
# Self-hosting on JavaScript: the translator, translated by ParparVM's JavaScript
# target and run under Node, must emit exactly what the JVM-hosted translator emits.
#
#   verify-selfhost-js.sh <classesDir> <AppName> <package>
#
# Same corpus arguments as verify-selfhost.sh. Requires build-selfhost.sh to have run
# (it leaves the translator compiled against JavaAPI in target/classes and JavaAPI in
# target/javaapi-classes) and `node` on PATH.
#
# The JavaScript translator is built here from those classes by the JVM translator,
# then both translators translate the corpus for the `clean` (C) and `javascript`
# targets, into the SAME absolute output path with the tree moved aside between runs
# -- the generated CMakeLists embeds that path -- under a constructed environment, as
# verify-selfhost.sh does and for the same reasons.
#
#   GATE C (jvm vs js-hosted, C):  byte-identical generated C
#   GATE S (jvm vs js-hosted, JS): byte-identical bundle, the suspension report
#                                   compared without its identity-ordered attribution
#
# The js-hosted translator reaches the file system through the runtime's java.io
# natives over a Node fs bridge (js/run-program.js); nothing in the translator knows
# it is not running natively.
set -e
cd "$(dirname "$0")"
REPO="$(cd ../.. && pwd)"
J8="${JDK_8_HOME:?set JDK_8_HOME to a working JDK 8}"
CLASSES="${1:?usage: verify-selfhost-js.sh <classesDir> <AppName> <package>}"
APP="${2:?}"
PKG="${3:?}"
command -v node >/dev/null 2>&1 || { echo "node is not on PATH"; exit 1; }

T="$REPO/vm/selfhost/target"
JAPI="$T/javaapi-classes"
TR="$REPO/vm/ByteCodeTranslator/target/classes"
for d in "$T/classes" "$JAPI" "$TR"; do
    [ -d "$d" ] || { echo "missing $d -- run build-selfhost.sh first"; exit 1; }
done
newest_src="$(find "$REPO/vm/ByteCodeTranslator/src" -type f -newer "$TR" -print -quit 2>/dev/null || true)"
if [ -n "$newest_src" ]; then
    echo "STALE: $TR is older than $newest_src -- run (cd vm && mvn -q -B -pl ByteCodeTranslator clean package -DskipTests)" >&2
    exit 1
fi

# The JavaScript translator is built from the JavaAPI-compiled classes in target/classes,
# which only build-selfhost.sh refreshes; a source edit since then would compare a new
# JVM translator against an old JavaScript one.
newest_self="$(find "$REPO/vm/ByteCodeTranslator/src" -type f -newer "$T/classes" -print -quit 2>/dev/null || true)"
if [ -n "$newest_self" ]; then
    echo "STALE: $T/classes is older than $newest_self -- run build-selfhost.sh" >&2
    exit 1
fi

W="$T/verify-js"
OUT="$W/out"
rm -rf "$W"; mkdir -p "$W"

# 1. The translator, as a JavaScript program.
TRJS="$W/translator-js"
( cd "$W" && env -i PATH=/usr/bin:/bin HOME="$HOME" TMPDIR=/tmp LC_ALL=C \
    "$J8/bin/java" -Xmx6g -cp "$TR" com.codename1.tools.translator.ByteCodeTranslator \
    javascript "$JAPI;$T/classes" "$TRJS" \
    com_codename1_tools_translator_ByteCodeTranslator com.codename1.tools.translator \
    com_codename1_tools_translator_ByteCodeTranslator 1.0 ios none ) > "$W/translate-translator.log" 2>&1 \
    || { echo "translating the translator to JavaScript FAILED"; tail -20 "$W/translate-translator.log"; exit 1; }
BUNDLE="$TRJS/dist/com_codename1_tools_translator_ByteCodeTranslator-js"
[ -f "$BUNDLE/translated_app.js" ] || { echo "no JavaScript translator bundle in $BUNDLE"; exit 1; }

run() {
    local tag=$1; shift
    mkdir -p "$OUT"
    ( cd "$W" && env -i PATH="/usr/bin:/bin:$(dirname "$(command -v node)")" HOME="$HOME" TMPDIR=/tmp LC_ALL=C \
        CN1_RESOURCE_PATH="$REPO/vm/ByteCodeTranslator/src" "$@" ) > "$W/$tag.log" 2>&1 \
        || { echo "$tag FAILED"; tail -20 "$W/$tag.log"; exit 1; }
    mv "$OUT" "$W/$tag-tree"
}
jvm=( "$J8/bin/java" -Xmx6g -cp "$TR" com.codename1.tools.translator.ByteCodeTranslator )
js=( node --max-old-space-size=8192 --stack-size=65500 "$REPO/vm/selfhost/js/run-program.js" "$BUNDLE" )

fail=0
compare() {
    local gate=$1 a=$2 b=$3
    local files
    files=$(find "$W/$a-tree" -type f | wc -l | tr -d ' ')
    [ "$files" -gt 5 ] || { echo "VACUOUS: only $files files emitted"; exit 1; }
    if diff -rq "$W/$a-tree" "$W/$b-tree" > "$W/$gate.txt" 2>&1; then
        echo "GATE $gate: PASS -- $files files byte-identical"
    else
        echo "GATE $gate: FAIL -- $(grep -c . "$W/$gate.txt") of $files paths differ"
        head -20 "$W/$gate.txt"
        fail=1
    fi
}

start=$(date +%s)
run c-jvm "${jvm[@]}" clean "$JAPI;$CLASSES" "$OUT" "$APP" "$PKG" "$APP" 1.0 clean none
run c-js "${js[@]}" clean "$JAPI;$CLASSES" "$OUT" "$APP" "$PKG" "$APP" 1.0 clean none
echo "js-hosted C translation: $(( $(date +%s) - start ))s including the JVM side"
compare "C (jvm vs js-hosted)" c-jvm c-js

run s-jvm "${jvm[@]}" javascript "$JAPI;$CLASSES" "$OUT" "$APP" "$PKG" "$APP" 1.0 ios none
run s-js "${js[@]}" javascript "$JAPI;$CLASSES" "$OUT" "$APP" "$PKG" "$APP" 1.0 ios none
# See verify-selfhost.sh, Gate J: the report's first-cause attribution follows
# identity-hash order, which differs between runtimes.
for side in s-jvm s-js; do
    find "$W/$side-tree" -name '*-suspension-report.txt' | while read -r report; do
        grep -v '^CAUSE \|^SIG \|^M SUSP ' "$report" > "$report.tmp" && mv "$report.tmp" "$report"
    done
done
compare "S (jvm vs js-hosted, JS)" s-jvm s-js

# Negative control, as in verify-selfhost.sh.
victim=$(find "$W/c-js-tree" -name '*.c' | sort | head -1)
printf 'x' | dd of="$victim" bs=1 seek=40 conv=notrunc status=none
if diff -rq "$W/c-jvm-tree" "$W/c-js-tree" > /dev/null 2>&1; then
    echo "NEGATIVE CONTROL: FAIL -- a corrupted tree still compared equal"; fail=1
else
    echo "NEGATIVE CONTROL: PASS -- corruption detected"
fi
exit $fail
