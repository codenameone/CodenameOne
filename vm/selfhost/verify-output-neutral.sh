#!/bin/bash
# Proves a translator source change does not alter the emitted C.
#
#   verify-output-neutral.sh capture <tag>     # run the JVM translator, save the tree
#   verify-output-neutral.sh compare <a> <b>   # diff two captured trees
#
# Gate A (in verify-selfhost.sh) compares the JVM translator against the native one
# and CANNOT see this: a refactor lands on both sides at once, so both move together
# and the gate stays green while every emitted signature changes. This runs the JVM
# translator alone, before and after, over the same corpus.
#
# Same fixed output path and constructed environment as verify-selfhost.sh, and for
# the same reasons: the generated CMakeLists embeds srcRoot.getAbsolutePath(), and
# the translator reads its knobs from getenv.
set -e
cd "$(dirname "$0")"
REPO="$(cd ../.. && pwd)"
J8="${JDK_8_HOME:?set JDK_8_HOME to a working JDK 8}"
W="$REPO/vm/selfhost/target/neutral"
OUT="$W/out"

case "${1:?usage: capture <tag> | compare <a> <b>}" in
capture)
    TAG="${2:?}"
    TR="$REPO/vm/ByteCodeTranslator/target/classes"
    ASM="$(cat "$REPO/vm/ByteCodeTranslator/target/selfhost-asm-classpath.txt")"
    JAPI="$REPO/vm/selfhost/target/javaapi-classes"
    # Same staleness guard verify-selfhost.sh carries: a source newer than its class
    # would capture the OLD translator under the NEW tag and report a real change as
    # neutral -- the exact failure this script exists to prevent.
    newest="$(find "$REPO/vm/ByteCodeTranslator/src" -name '*.java' -newer "$TR" -print -quit 2>/dev/null || true)"
    [ -z "$newest" ] || { echo "STALE: $TR older than $newest -- run mvn package first" >&2; exit 1; }
    rm -rf "$W/$TAG-tree" "$OUT"; mkdir -p "$OUT"
    ( cd "$W" && env -i PATH=/usr/bin:/bin HOME="$HOME" TMPDIR=/tmp LC_ALL=C \
        CN1_RESOURCE_PATH="$REPO/vm/ByteCodeTranslator/src" \
        "$J8/bin/java" -cp "$TR:$ASM" com.codename1.tools.translator.ByteCodeTranslator \
        clean "$JAPI;$REPO/vm/selfhost/target/asm-classes;$REPO/vm/selfhost/target/classes" \
        "$OUT" com_codename1_tools_translator_ByteCodeTranslator \
        com.codename1.tools.translator com_codename1_tools_translator_ByteCodeTranslator \
        1.0 clean none ) > "$W/$TAG.log" 2>&1 \
        || { echo "capture $TAG FAILED"; tail -20 "$W/$TAG.log"; exit 1; }
    mv "$OUT" "$W/$TAG-tree"
    n=$(find "$W/$TAG-tree" -type f | wc -l | tr -d ' ')
    [ "$n" -gt 10 ] || { echo "VACUOUS: only $n files"; exit 1; }
    echo "captured $TAG: $n files"
    ;;
compare)
    A="$W/${2:?}-tree"; B="$W/${3:?}-tree"
    for d in "$A" "$B"; do [ -d "$d" ] || { echo "no $d"; exit 1; }; done
    na=$(find "$A" -type f | wc -l | tr -d ' ')
    if diff -rq "$A" "$B" > "$W/neutral.txt" 2>&1; then
        echo "OUTPUT-NEUTRAL: PASS -- $na files byte-identical"
    else
        echo "OUTPUT-NEUTRAL: FAIL -- $(grep -c . "$W/neutral.txt") differing paths"
        head -20 "$W/neutral.txt"; exit 1
    fi
    ;;
*) echo "usage: capture <tag> | compare <a> <b>" >&2; exit 1 ;;
esac
