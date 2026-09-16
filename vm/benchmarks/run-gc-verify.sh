#!/bin/bash
# ParparVM heap-integrity gate (-DCN1_GC_VERIFY).
#
# The gauntlet proves the VM COMPUTES the right answer. This proves the
# collector leaves the heap in a legal state: after every sweep, no object the
# sweep kept may reference memory the sweep reclaimed. Checksums cannot see that
# invariant break -- a dangling reference reads whatever object recycled the
# slot, so the damage surfaces later, somewhere else, as corrupted data rather
# than as a wrong answer (issue 5425). Here it aborts at the cycle that caused
# it, naming the holder class, the victim class and the field's mark call site.
#
#   run-gc-verify.sh              # full gate: tortures + drivers + self-test
#   run-gc-verify.sh GraceAudit   # one driver
#
# Requirements: JDK_8_HOME, Maven, clang.
set -e
cd "$(dirname "$0")"
J8="${JDK_8_HOME:?set JDK_8_HOME}"
mkdir -p target/bin

# This script's result is decided by these variables, and anyone debugging the
# collector has them exported. An inherited CN1_GC_FAULT would fail every
# driver below; an inherited CN1_GC_VERIFY_SOFT would stop the self-test from
# aborting. Either inverts a result instead of failing loudly.
unset CN1_GC_FAULT CN1_GC_VERIFY_SOFT CN1_GC_VERIFY_AGING CN1_GC_VERIFY_ALL \
      CN1_GC_VERIFY_LOG CN1_GC_VERIFY_CENSUS CN1_GC_VERIFY_DUMP CN1_GC_TRACE_MARK \
      CN1_GC_DEBUG_EARLY

# Every workload that allocates enough to drive real collection cycles. The
# point is coverage of ALLOCATION SHAPES, not of answers: page-heap churn,
# monitors, finalizers, threads, oversized/legacy objects, adopted survivors.
# MapTorture2 REPLACES MapTorture here, and the reason is worth keeping.
#
# MapTorture is HashMap<Integer,Integer> throughout, and an Integer is a tagged immediate:
# it never allocates and is never traced. Once HashMap's storage moved from Java arrays
# into C blocks that workload stopped allocating on the Java heap almost entirely and
# completed ZERO GC cycles under the verifier -- this script correctly reported it vacuous
# rather than passing it. MapTorture stays in run-gauntlet.sh, where byte-identity against
# the host is what it is for.
#
# The gap that exposed is the older problem: no driver here ever held a map full of REAL
# references across a collection, which is exactly what the block storage depends on -- a
# C block is reachable to the collector only through the owner's generated __GC_MARK_.
# MapTorture2 is String-keyed and Object-valued for that reason.
DRIVERS="${*:-GraceAudit LegacyGrace BulkCopyBarrier GcStress MtStress MapTorture2 SbTorture FusedTest ThreadChurn LargeArrayLoad}"

fail=0
for d in $DRIVERS; do
    printf '%-16s ' "$d"
    if ! ./translate-and-build.sh "$d" "target/bin/$d-verify" -DCN1_GC_VERIFY > target/bin/$d-build.log 2>&1; then
        echo "BUILD FAILED"
        tail -25 "target/bin/$d-build.log"
        fail=1
        continue
    fi
    if out="$(./target/bin/$d-verify 2>&1)"; then
        # passes=0 means the workload never finished a collection cycle, so the
        # verifier never ran and "no violations" would mean only that nothing
        # was ever checked. Treat a vacuous pass as a failure -- that is the
        # same hollow-gate problem the self-test below exists to prevent.
        passes="$(printf '%s' "$out" | sed -n 's/.*SUMMARY passes=\([0-9]*\).*/\1/p' | tail -1)"
        if printf '%s' "$out" | grep -q 'GC-VERIFY. DANGLING'; then
            echo "FAILED (verifier reported a dangling reference)"
            printf '%s\n' "$out" | grep -A 4 'DANGLING' | head -20
            fail=1
        elif [ -z "$passes" ] || [ "$passes" -eq 0 ]; then
            echo "FAILED (vacuous: 0 verify passes -- the workload never completed a GC cycle)"
            fail=1
        else
            early="$(printf '%s' "$out" | sed -n 's/.*earlyFreed=\([0-9]*\).*/\1/p' | tail -1)"
            resd="$(printf '%s' "$out" | sed -n 's/.*resurrectedDangling=\([0-9]*\).*/\1/p' | tail -1)"
            if [ "${early:-0}" -ne 0 ]; then
                # The O(1) whole-page reclaim and the per-slot walk disagreed
                # about when an object dies -- the pairing that leaves a kept
                # object referencing reclaimed memory.
                echo "FAILED ($early slot(s) freed a cycle early by the O(1) page reclaim)"
                fail=1
            elif [ "${resd:-0}" -ne 0 ]; then
                echo "FAILED ($resd resurrected object(s) still referencing reclaimed memory)"
                fail=1
            else
                echo "clean ($passes verify passes)"
            fi
        fi
    else
        echo "FAILED (exit $?)"
        printf '%s\n' "$out" | tail -25
        fail=1
    fi
done

# SELF-TEST. A gate nobody has watched fail is not a gate: re-inject the defect
# #5442 fixed (grace-subtree pass disabled, the #5436 behavior) and require the
# verifier to catch it. If this run comes back clean the gate above is inert and
# a green result from it means nothing.
printf '%-16s ' "self-test"
# The faulted run is EXPECTED to abort. Capture it in a command substitution so
# the shell does not print its own job-control notice for the SIGABRT -- a line
# that reads like a failure sitting next to a passing gate is how people learn
# to ignore the gate.
faultOut="$(CN1_GC_FAULT=nograce ./target/bin/GraceAudit-verify 2>&1)" && faultExit=0 || faultExit=$?
if [ "$faultExit" -eq 0 ]; then
    echo "BROKEN -- injected grace-pass fault was NOT detected"
    fail=1
elif printf '%s' "$faultOut" | grep -q 'DANGLING REFERENCE'; then
    echo "detected the injected grace-pass fault ($(printf '%s' "$faultOut" | grep -c 'DANGLING REFERENCE') reports)"
else
    echo "BROKEN -- faulted run died (exit $faultExit) without a verifier report"
    printf '%s\n' "$faultOut" | tail -20
    fail=1
fi

# Second self-test, for the early-free check added with the O(1) page-reclaim
# fix. LargeArrayLoad is the workload that exposed it (26,924 slots freed a
# cycle early), so with the old bound restored the gate above must reject it.
printf '%-16s ' "self-test2"
# Keep the exit status and test for the SUMMARY line separately. Reporting only
# "produced no early frees" conflates three different outcomes -- the fault did
# not fire, the faulted run died before it could report, and the binary was never
# built -- and the message names the first, so a crashed run reads as a broken
# FIX. This gate did exactly that once: the run reported BROKEN, and re-running
# the same binary twelve times produced earlyFreed=13452 and exit 0 every time,
# so whatever went wrong was never the thing the message accused.
efOut="$(CN1_GC_FAULT=earlyfree ./target/bin/LargeArrayLoad-verify 2>&1)" && efExit=0 || efExit=$?
efCount="$(printf '%s' "$efOut" | sed -n 's/.*earlyFreed=\([0-9]*\).*/\1/p' | tail -1)"
if [ "${efCount:-0}" -gt 0 ]; then
    echo "detected the injected early-free fault ($efCount slots)"
elif ! printf '%s' "$efOut" | grep -q 'GC-VERIFY. SUMMARY'; then
    echo "BROKEN -- faulted run died (exit $efExit) before the verifier summary"
    printf '%s\n' "$efOut" | tail -20
    fail=1
else
    echo "BROKEN -- restoring the pre-fix page-reclaim bound produced no early frees"
    printf '%s\n' "$efOut" | tail -5
    fail=1
fi

# Third self-test, for java.lang.ref. The referent is the ONE reference field the
# generated mark functions deliberately do not hand to gcMarkObject -- that
# suppression is what makes the edge weak -- and for a while it therefore bypassed
# the verifier completely: a live Reference holding a pointer into reclaimed memory
# passed with violations=0, which is the single defect this collector work most
# needs caught. cn1GcDiscoverReference now routes it to cn1GcVerifyChild, and this
# proves that routing has teeth rather than assuming it.
#
# refnoclear leaves a dead referent in its field instead of clearing it. Note the
# obvious-looking fault is the wrong one: clearing MORE references than liveness
# warrants only produces extra nulls, which are safe, and an attempt at that
# reported violations=0 for exactly that reason. The dangling direction is
# clearing LESS.
printf '%-16s ' "self-test3"
# REMOVE FIRST, THEN BUILD, AND CHECK THE STATUS. Three things are needed and only the
# third is obvious. Rebuilding unconditionally is not enough on its own: translate-and-build
# replaces its output only after the final compiler run succeeds, so a failed rebuild leaves
# the PREVIOUS binary in place. Nor is `|| true` harmless: it discards the status, and the
# -x test below then accepts that stale executable. Either way the self-test runs old code
# and reports green -- the "gate that cannot fail" problem this self-test exists to prevent,
# reintroduced in how the self-test is built.
rm -f ./target/bin/RefPolicy-verify
if ! ./translate-and-build.sh RefPolicy target/bin/RefPolicy-verify -DCN1_GC_VERIFY \
        > target/bin/RefPolicy-selftest-build.log 2>&1; then
    echo "BROKEN -- could not build RefPolicy for the reference self-test"
    tail -25 target/bin/RefPolicy-selftest-build.log
    fail=1
fi
if [ ! -x ./target/bin/RefPolicy-verify ]; then
    echo "BROKEN -- could not build RefPolicy for the reference self-test"
    fail=1
else
    rcOut="$(CN1_GC_FAULT=refnoclear ./target/bin/RefPolicy-verify 128 8192 1500 24 2>&1)" || true
    if printf '%s' "$rcOut" | grep -q 'DANGLING REFERENCE'; then
        echo "detected the injected dangling referent ($(printf '%s' "$rcOut" | grep -c 'DANGLING REFERENCE') reports)"
    elif ! printf '%s' "$rcOut" | grep -q 'GC-VERIFY. SUMMARY'; then
        echo "BROKEN -- faulted run died before the verifier summary"
        printf '%s\n' "$rcOut" | tail -20
        fail=1
    else
        echo "BROKEN -- an uncleared dead referent was NOT reported; the verifier cannot see referents"
        printf '%s\n' "$rcOut" | tail -5
        fail=1
    fi
fi

# Fourth self-test, for NATIVE REFERENCE BLOCKS. A container whose storage is a C block
# (HashMap's three tables) is reachable to the collector ONLY through the owner's generated
# __GC_MARK_ -- the conservative scanner walks native stacks, not arbitrary malloc blocks.
# Nothing else in the VM would notice if that walk were short, which is exactly the failure
# this feature can produce: the real bug it shipped with freed replaced blocks while a
# marker was still walking them, and MapTorture2 reported "LOST held key" while the
# gauntlet and all three self-hosting gates were green.
#
# halfblock traces only the first half of every block. The driver must then LOSE entries.
# Note this is a BEHAVIOURAL self-test rather than a violations= one: a swept element is
# not a dangling pointer the verifier can see, it is an entry that is simply gone, so the
# driver's own read-back is what catches it.
printf '%-16s ' "self-test4"
rm -f ./target/bin/MapTorture2-verify
if ! ./translate-and-build.sh MapTorture2 target/bin/MapTorture2-verify -DCN1_GC_VERIFY \
        > target/bin/MapTorture2-selftest-build.log 2>&1; then
    echo "BROKEN -- could not build MapTorture2 for the block self-test"
    tail -25 target/bin/MapTorture2-selftest-build.log
    fail=1
elif [ ! -x ./target/bin/MapTorture2-verify ]; then
    echo "BROKEN -- could not build MapTorture2 for the block self-test"
    fail=1
else
    hbOut="$(CN1_GC_FAULT=halfblock ./target/bin/MapTorture2-verify 2>&1)" || true
    if printf '%s' "$hbOut" | grep -qE 'LOST|CORRUPT'; then
        echo "detected the injected half-traced block ($(printf '%s' "$hbOut" | grep -oE 'LOST[^"]*|CORRUPT[^"]*' | head -1))"
    else
        echo "BROKEN -- tracing only half of every reference block lost nothing"
        printf '%s\n' "$hbOut" | tail -5
        fail=1
    fi
fi

[ "$fail" -eq 0 ] && echo "GC-VERIFY GREEN" || { echo "GC-VERIFY FAILED"; exit 1; }
