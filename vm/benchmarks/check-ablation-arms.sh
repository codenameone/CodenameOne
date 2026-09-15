#!/bin/bash
# EVERY DOCUMENTED RUNTIME CONFIGURATION MUST STILL COMPILE.
#
#   check-ablation-arms.sh              # discover the arms and build each one
#   check-ablation-arms.sh --list       # print the discovered arms and stop
#   check-ablation-arms.sh CN1_FOO ...  # build only these
#
# WHY THIS EXISTS. ParparVM's runtime carries around thirty compile-time arms: QA
# instruments (CN1_GC_VERIFY, CN1_ALLOC_CENSUS), and ablations that exist so a gate
# can prove it is able to FAIL (GcSteadyStateIntegrationTest rebuilds with
# -DCN1_SATB_LOG_FRESH and REQUIRES its assertions to fail). They are not user-facing
# options and must never become any; they are negative controls and instruments.
#
# The failure mode they have is silent rot. An arm nothing builds stops building, and
# nothing says so, and the first person to reach for it finds it broken at the moment
# they needed it. That is not hypothetical -- a single sweep of this script found four:
#
#   CN1_NURSERY                        did not compile for as long as the bulk SATB
#                                      barrier had existed (3 implicit declarations).
#                                      It retired the one arm that could have caught a
#                                      missing tag guard in the nursery's own code, and
#                                      the whole feature was later deleted.
#   CN1_DISABLE_CONSERVATIVE_GC_ROOTS  gcPthreadValid was declared inside the
#                                      conservative-roots #ifdef while CN1_RESUME_THREAD
#                                      read it unconditionally: 4 errors. This is the
#                                      only arm that can falsify a GC ROOTING claim, so
#                                      losing it costs a whole class of evidence.
#   CN1_DISABLE_BIBOP                  cn1GcCycleState sat inside the BiBOP guard under a
#                                      comment saying it was "defined UNCONDITIONALLY" --
#                                      an undefined symbol at LINK time. run-bibop-adaptive.sh
#                                      builds this arm as its comparison baseline, so that
#                                      script could not have run either.
#   DEBUG_GC_OBJECTS_IN_HEAP           sized its tables with cn1_array_3_id_java_util_Vector,
#                                      which stops existing when an app culls Vector. Deleted
#                                      as superseded by CN1_ALLOC_CENSUS.
#
# THE ARM LIST IS DERIVED FROM THE SOURCE, NEVER HAND-MAINTAINED. A curated list rots
# exactly like the arms do: someone adds an arm, forgets the list, and it is uncovered
# from birth. Anything the preprocessor TESTS but the tree never #defines is a pure -D
# opt-in, i.e. an arm, and is swept. Add one and it is covered the same day.
#
# The check is COMPILE (and link, and a trivial run), not behaviour. What an arm does is
# its own gate's business; this only answers "does the configuration still exist".
set -e
cd "$(dirname "$0")"
REPO="$(cd ../.. && pwd)"
SRC="$REPO/vm/ByteCodeTranslator/src"

# Not arms: these are selected by the PLATFORM or by the translator, not by a developer
# passing -D, so building them here would either be meaningless or need another toolchain.
# Keep this list tiny and justified -- every entry is a hole in the sweep.
SKIP='^(CN1_USE_ARC|CN1_HAS_PROC_AVAILABLE_MEMORY|CN1_HAVE_SB_INTRINSICS|CN1_GC_CAN_FORCE_STOP|CN1_TAGGED_ACTIVE|CN1_TAGGED_EXTRA_ACTIVE|CN1_TAGGED_INT|CN1_CONSERVATIVE_GC_ROOTS|CN1_INTRINSICS_H|CN1_WIN_COMPAT_H|CN1_ON_DEVICE_DEBUG|DEBUG_GC_VARIABLES)$'

discover() {
    python3 - "$SRC" <<'PY'
import os, re, sys
src = sys.argv[1]
files = ['cn1_globals.h','cn1_globals.m','nativeMethods.m','cn1_intrinsics.h','cn1_win_compat.h']
tested, defined = set(), set()
prev_ifndef = None
for f in files:
    p = os.path.join(src, f)
    if not os.path.exists(p):
        continue
    for line in open(p, encoding='utf-8', errors='surrogateescape'):
        s = line.strip()
        m = re.match(r'#\s*(ifdef|ifndef|if|elif)\b(.*)', s)
        if m:
            tested.update(re.findall(r'\b(?:CN1|DEBUG_GC)_[A-Z0-9_]+', m.group(2)))
        d = re.match(r'#\s*define\s+((?:CN1|DEBUG_GC)_[A-Z0-9_]+)', s)
        if d and prev_ifndef == d.group(1):
            # SELF-DEFAULTED ONLY WHEN THE DEFINE IS THE BODY OF ITS OWN #ifndef, which is
            # the tunable idiom (`#ifndef CN1_X` / `#define CN1_X 512`). A define sitting
            # under some OTHER condition is not a default: CN1_NO_WEAK_REFS is defined only
            # inside `#if defined(CN1_DISABLE_SATB) && !defined(...)`, so counting any
            # define as a default silently dropped a real arm out of the sweep.
            defined.add(d.group(1))
        g = re.match(r'#\s*ifndef\s+((?:CN1|DEBUG_GC)_[A-Z0-9_]+)', s)
        if g:
            prev_ifndef = g.group(1)
        elif s and not s.startswith('//') and not s.startswith('*') and not s.startswith('/*'):
            # Comments and blank lines routinely sit between the #ifndef and its #define
            # (the tunables here carry several lines of rationale each), so the guard has
            # to survive them -- but any real code between the two means this is not the
            # self-default idiom.
            prev_ifndef = None
# An arm is TESTED and never SELF-DEFAULTED: the only way to turn it on is -D on a
# compile line.
for name in sorted(tested - defined):
    print(name)
PY
}

ARMS="$*"
if [ "$ARMS" = "--list" ]; then discover | grep -Ev "$SKIP"; exit 0; fi
[ -n "$ARMS" ] || ARMS="$(discover | grep -Ev "$SKIP" | tr '\n' ' ')"

OUT="${CN1_ABLATION_OUT:-target/ablation}"
# The compile shape. Defaults to the release one so a local run matches what ships;
# CI passes -O1, because the question here is whether a configuration still compiles
# and links, and every arm that has ever broken broke on an undeclared identifier or an
# undefined symbol -- neither of which needs -O3 to surface.
CFLAGS="${CN1_ABLATION_CFLAGS:--flto=thin}"
mkdir -p "$OUT"
fail=0
printf '%-38s %s\n' "ARM" "RESULT"
printf -- '-%.0s' $(seq 1 78); echo
for a in $ARMS; do
    log="$OUT/$a.log"
    # CN1_DISABLE_CONSERVATIVE_GC_ROOTS is the one arm that also needs a CODEGEN change:
    # with precise roots the object-bearing frameless shapes have nowhere to be scanned
    # from, so the translator must stop emitting them. Pairing it here is what makes the
    # arm meaningful rather than merely linkable.
    topts=""
    case "$a" in
        CN1_DISABLE_CONSERVATIVE_GC_ROOTS)
            topts="-Dcn1.frameless.objects=false -Dcn1.frameless.instance=false" ;;
    esac
    # A few arms are read as a VALUE rather than tested for presence, so a bare -D leaves
    # an empty expansion and a syntax error that says nothing about the arm. The value is
    # here only to make the configuration compile; what it should BE is its own gate's
    # question.
    define="-D$a"
    case "$a" in
        CN1_GC_MARK_THREADS)  define="-D$a=4" ;;
        CN1_NO_WEAK_REFS)     define="-D$a=1" ;;
    esac
    if CN1_BENCH_TRANSLATOR_OPTS="$topts" CN1_BENCH_CFLAGS="$CFLAGS $define" \
            ./translate-and-build.sh Noop "$OUT/bin-$a" > "$log" 2>&1; then
        if "$OUT/bin-$a" > "$OUT/$a.run" 2>&1; then
            printf '%-38s %s\n' "$a" "ok"
        else
            printf '%-38s %s\n' "$a" "RUNTIME FAIL -> $OUT/$a.run"; fail=1
        fi
    else
        printf '%-38s %s\n' "$a" "BUILD FAILED -> $log"; fail=1
    fi
done
echo
if [ "$fail" = 0 ]; then
    echo "ABLATION ARMS GREEN -- every documented configuration still builds."
else
    echo "ABLATION ARMS FAILED. An arm that does not build is a configuration this tree"
    echo "documents and nothing can use. Fix it, or delete the arm deliberately -- do not"
    echo "leave it broken, which is how it got here."
    exit 1
fi
