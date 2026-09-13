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

# Renumber label_L<n> by ORDER OF FIRST APPEARANCE within the file, rather than
# erasing every label to one token. Erasing them makes a jump that was retargeted
# from one existing label to another compare EQUAL -- which is precisely the
# code-generation regression this comparison is for. Renumbering keeps the identity
# relationships and still absorbs the switch from identity-hash names to sequential
# ones.
cn1_canon_labels() {
    awk '
        # Reset the mapping at every generated FUNCTION boundary.
        #
        # The branch numbers labels with a METHOD-LOCAL counter, so L0 and L1 recur
        # in every method, while master derives them from ASM identities that are
        # distinct across the whole file. A file-wide map therefore folds the second
        # method'"'"'s L0 onto the first method'"'"'s token on one side and not the other,
        # and reports identical output as a codegen difference -- the mirror of the
        # erase-everything bug, generating false positives instead of hiding real
        # ones. Per-function scope matches how the names are actually minted.
        /^[A-Za-z_][A-Za-z0-9_ \*]*\(/ { delete seen; k = 0 }
        {
            line = $0
            out = ""
            # The number leaks into derived identifiers too -- catch_L<n>,
            # restoreToL<n>, tryBlockOffsetL<n> -- which must share the numbering
            # WITHIN a function or every try/catch file reports as changed forever.
            while (match(line, /(label_L|catch_L|restoreToL|tryBlockOffsetL)[0-9]+/)) {
                pre = substr(line, 1, RSTART - 1)
                tok = substr(line, RSTART, RLENGTH)
                line = substr(line, RSTART + RLENGTH)
                nstart = match(tok, /[0-9]+$/)
                kind = substr(tok, 1, nstart - 1)
                num  = substr(tok, nstart)
                if (!(num in seen)) { seen[num] = ++k }
                out = out pre kind seen[num]
            }
            print out line
        }' "$1"
}

case "${1:?usage: capture <tag> | compare <a> <b> | vs-master}" in
vs-master)
    # Compare THIS branch's translator against MASTER's over one corpus.
    #
    # This is the comparison the other two modes cannot make. They run the same
    # translator twice, so a change that lands on the branch is present on both
    # sides and they stay green while every emitted signature moves. That blind
    # spot cost a full bisect: four native screenshot legs reported a four-pixel
    # layout shift, and the cause was a codegen change this script reported as
    # neutral because it was neutral -- against itself.
    #
    # The corpus is a small app compiled against MASTER's JavaAPI, so JavaAPI is
    # held constant and the translator is the only variable. Differences in
    # deterministic label names and local-variable declaration order are expected
    # and are normalised out; anything else is a real codegen change and should be
    # a deliberate one.
    MW="${CN1_MASTER_WORKTREE:-/tmp/cn3-master}"
    [ -d "$MW/vm" ] || { echo "no master worktree at $MW"; echo "  git worktree add $MW origin/master"; exit 1; }
    MTR="$MW/vm/ByteCodeTranslator/target/classes"
    MAPI="$MW/vm/JavaAPI/target/classes"
    for d in "$MTR" "$MAPI"; do
        [ -d "$d" ] || { echo "missing $d -- build master's translator and JavaAPI first:"; \
            echo "  (cd $MW/vm && mvn -q -B -pl ByteCodeTranslator,JavaAPI package -DskipTests)"; exit 1; }
    done
    APP="${CN1_NEUTRAL_APP:-/tmp/cmpcls}"
    [ -d "$APP" ] || { echo "no corpus app at $APP (set CN1_NEUTRAL_APP)"; exit 1; }
    ASM="$(cat "$REPO/vm/ByteCodeTranslator/target/selfhost-asm-classpath.txt")"
    rm -rf "$W/m-tree" "$W/b-tree" "$OUT"
    for side in m b; do
        [ "$side" = m ] && TR="$MTR" || TR="$REPO/vm/ByteCodeTranslator/target/classes"
        mkdir -p "$OUT"
        ( cd "$W" && env -i PATH=/usr/bin:/bin HOME="$HOME" TMPDIR=/tmp LC_ALL=C \
        CN1_NATIVE_VERIFY="${CN1_NATIVE_VERIFY:-}" \
            "$J8/bin/java" -cp "$TR:$ASM" com.codename1.tools.translator.ByteCodeTranslator \
            clean "$MAPI;$APP" "$OUT" CmpApp com.cmp CmpApp 1.0 clean none ) > "$W/$side.log" 2>&1 \
            || { echo "$side side FAILED"; tail -5 "$W/$side.log"; exit 1; }
        mv "$OUT" "$W/$side-tree"
    done
    # The C runtime is copied verbatim and this branch edits it on purpose, so it is
    # not part of the codegen question.
    RUNTIME='^(cn1_globals\\.[ch]|nativeMethods\\.c|cn1_intrinsics\\.h|java_io_File_runtime\\.c|cn1-source-manifest\\.txt)$'
    # Walk the UNION of both trees, not master's listing. A file the branch emits and
    # master does not would never be visited by a master-only loop, so a whole new
    # generated class could appear and the gate would report neutral.
    ( cd "$W/m-tree/dist/CmpApp-src" 2>/dev/null && ls ) > "$W/m.list" 2>/dev/null || : > "$W/m.list"
    ( cd "$W/b-tree/dist/CmpApp-src" 2>/dev/null && ls ) > "$W/b.list" 2>/dev/null || : > "$W/b.list"
    sort -u "$W/m.list" "$W/b.list" > "$W/all.list"
    n=0
    while read -r base; do
        [ -n "$base" ] || continue
        echo "$base" | grep -qE "$RUNTIME" && continue
        case "$base" in *.c|*.h) ;; *) continue ;; esac
        mf="$W/m-tree/dist/CmpApp-src/$base"; bf="$W/b-tree/dist/CmpApp-src/$base"
        if [ ! -f "$mf" ]; then echo "  ONLY IN BRANCH: $base"; n=$((n+1)); continue; fi
        if [ ! -f "$bf" ]; then echo "  ONLY IN MASTER: $base"; n=$((n+1)); continue; fi
        if ! diff -q <(cn1_canon_labels "$mf") <(cn1_canon_labels "$bf") >/dev/null; then
            [ $n -lt 12 ] && echo "  differs: $base"
            n=$((n+1))
        fi
    done < "$W/all.list"
    echo "VS-MASTER: $n generated file(s) differ beyond label naming"
    if [ "$n" != 0 ]; then
        echo "Each one is a codegen change against master. Confirm every one is intended."
        # Exit NONZERO. Printing a finding and returning 0 makes every caller read a
        # real mismatch as a passing check, which is the failure mode this whole
        # script exists to prevent.
        exit 1
    fi
    ;;
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
        CN1_NATIVE_VERIFY="${CN1_NATIVE_VERIFY:-}" \
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
