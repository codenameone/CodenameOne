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

# Every workload that allocates enough to drive real collection cycles. The
# point is coverage of ALLOCATION SHAPES, not of answers: page-heap churn,
# monitors, finalizers, threads, oversized/legacy objects, adopted survivors.
DRIVERS="${*:-GraceAudit LegacyGrace GcStress MtStress MapTorture SbTorture FusedTest ThreadChurn LargeArrayLoad}"

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
        if printf '%s' "$out" | grep -q 'GC-VERIFY. DANGLING'; then
            echo "FAILED (verifier reported a dangling reference)"
            printf '%s\n' "$out" | grep -A 4 'DANGLING' | head -20
            fail=1
        else
            echo "clean"
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

[ "$fail" -eq 0 ] && echo "GC-VERIFY GREEN" || { echo "GC-VERIFY FAILED"; exit 1; }
