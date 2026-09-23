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

# Second self-test: restore the faulty O(1) page-reclaim age bound.
# GraceAudit supplies repeated collection cycles independently of collection
# backing-store allocation. LargeArrayLoad's old heap pressure is no longer a
# reliable trigger now that Hashtable backing tables live in native buffers.
# A run without observed early frees still FAILS this gate.
printf '%-16s ' "self-test2"
efOut="$(CN1_GC_FAULT=earlyfree ./target/bin/GraceAudit-verify 2>&1)" && efExit=0 || efExit=$?
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
    # A 4MB collection trigger, and it is what makes this a test rather than a coin flip.
    # The damage only appears if a collection lands while live entries sit in the
    # untraced half of a block, and at the default trigger MapTorture2 runs few enough
    # cycles that it often does not: measured 2 of 8 runs detected on one tree and 5 of
    # 8 on the next, so a single attempt reported BROKEN on code that was fine. At 4MB
    # both trees detected 8 of 8, and at 1MB too.
    #
    # The CONTROL run at the same trigger is what keeps that honest: if a small trigger
    # alone made MapTorture2 lose entries, "detection" would be a real bug and not the
    # injected one, so the unfaulted run must stay clean (measured 4 of 4 on both trees).
    hbCtl="$(CN1_GC_TRIGGER_MB=4 ./target/bin/MapTorture2-verify 2>&1)" || true
    hbOut="$(CN1_GC_TRIGGER_MB=4 CN1_GC_FAULT=halfblock ./target/bin/MapTorture2-verify 2>&1)" || true
    if printf '%s' "$hbCtl" | grep -qE 'LOST|CORRUPT|violations=[1-9]'; then
        echo "BROKEN -- MapTorture2 loses entries at a 4MB trigger WITHOUT the fault (a real bug)"
        printf '%s\n' "$hbCtl" | grep -E 'LOST|CORRUPT|violations=' | head -5
        fail=1
    elif printf '%s' "$hbOut" | grep -qE 'LOST|CORRUPT'; then
        echo "detected the injected half-traced block ($(printf '%s' "$hbOut" | grep -oE 'LOST[^"]*|CORRUPT[^"]*' | head -1))"
    else
        echo "BROKEN -- tracing only half of every reference block lost nothing"
        printf '%s\n' "$hbOut" | tail -5
        fail=1
    fi
fi

# Fifth self-test, for the BiBOP SLOT QUARANTINE -- and, through it, for whether this
# verifier can see a dangling reference at all.
#
# Until the quarantine landed it could not. The sweep poisoned a reclaimed slot and pushed
# it straight onto the page free list, so the next allocation handed the slot back and a
# stale reference into it resolved to a valid live object by the time the verify pass ran.
# Measured before the change: 60 verifier runs under memory pressure produced 2 crashes and
# violations=0. The verifier's own CN1_GC_VS_FREE_SLOT branch existed the whole time and
# simply never fired.
#
# CN1_GC_FAULT=freelive reclaims up to 8 slots the sweep has just proved LIVE, nominating
# them in gcMarkObject so they are provably reference-field children (a victim chosen in
# the sweep is usually referenced only from a stack slot, which this instrument cannot see
# by construction -- that version fired 8 times and reported nothing). MapTorture2 is the
# driver because it holds 4000 entries across forced collections, so a freed victim still
# has a live holder.
#
# The assertion is that the injected corruption is NEVER missed. Measured over 6 runs each:
#
#     quarantine on   verifier caught 4, driver caught 2, missed 0
#     quarantine off  verifier caught 0, driver caught 1, missed 5
#
# so a silent clean run is the failure, and -DCN1_GC_NO_QUARANTINE is the negative control
# that reproduces it. The gate does not require the VERIFIER specifically to be the one
# that catches it on any given run -- which of the two notices first is a race -- but it
# does require the arm without the quarantine to be the only way a run goes quiet.
printf '%-16s ' "self-test5"
if [ ! -x ./target/bin/MapTorture2-verify ]; then
    echo "BROKEN -- self-test4 did not leave a MapTorture2-verify binary"
    fail=1
else
    flAttempts=4
    flMissed=0
    flVerifier=0
    flFired=0
    for flI in $(seq 1 $flAttempts); do
        flRc=0
        flOut="$(CN1_GC_FAULT=freelive CN1_GC_FAULT_EVERY=3000 CN1_GC_VERIFY_SOFT=1 \
                 ./target/bin/MapTorture2-verify 2>&1)" || flRc=$?
        flV="$(printf '%s' "$flOut" | grep -oE 'violations=[0-9]+' | tail -1 | cut -d= -f2)"
        # "the fault fired" and "the fault was caught" are separate claims; a run where
        # nothing was freed proves nothing either way and must count as NEITHER a pass
        # nor a miss. It used to count as a miss: about 1 run in 150 ends before any
        # sweep reaches a 3000th slot, prints the correct checksum with no fault line,
        # and failed the gate as "freed live slots and NOTHING noticed". The per-slot
        # "freed slot" line is read as well as the exit summary, because a run the
        # fault kills never prints the summary.
        flThis=0
        if printf '%s' "$flOut" | grep -qE 'slots freed while live: [1-9]|\[GC-FAULT\] +freed slot'; then
            flFired=$((flFired + 1))
            flThis=1
        fi
        if [ "${flV:-0}" -gt 0 ] 2>/dev/null; then
            flVerifier=$((flVerifier + 1))
        elif [ "$flThis" -eq 1 ] && [ "$flRc" -eq 0 ] \
             && ! printf '%s' "$flOut" | grep -qE 'LOST|CORRUPT'; then
            # A dangling slot that made the run die -- a crash, or the OutOfMemoryError
            # a clobbered StringBuilder length produces -- was noticed; only a run that
            # exits cleanly with nothing reported is a miss.
            flMissed=$((flMissed + 1))
        fi
    done
    if [ "$flFired" -eq 0 ]; then
        echo "BROKEN -- freelive never freed a live slot in $flAttempts runs"
        fail=1
    elif [ "$flMissed" -gt 0 ]; then
        echo "BROKEN -- $flMissed of $flAttempts runs freed live slots and NOTHING noticed"
        fail=1
    else
        echo "caught the injected dangling reference $flAttempts/$flAttempts (verifier $flVerifier)"
    fi
fi

# Sixth self-test, for ESCAPED STACK OBJECTS -- and, through it, for a class of bug the
# verifier could not see at all until this existed.
#
# Three analyses in this translator put objects on the C stack: implicitly stack-allocated
# StringBuilders, scalar-replaced @StackAllocate instances, and SIMD stack arrays. Each
# rests on proving the object does not escape its frame, and a wrong proof produced NO
# signal: the address is in no BiBOP page and no legacy extent, so cn1GcVerifyClassify
# answered UNKNOWN and skipped it -- the same answer it gives a static or an immortal.
# The check compares the address against each live thread's C stack range instead, which
# needs no dereference and cannot be confused with either.
#
# SbEscape publishes a stack builder into a heap object's field. Built normally the
# analysis refuses it, nothing is stack-allocated and the run is clean; built with
# -Dcn1.sbSkipEscapeValidation=true the builder goes on the stack anyway and the escape
# is real. BOTH arms are required, because "the ablated arm reports" alone would also be
# satisfied by a check that reports everything.
#
# The escape is published from main's frame on purpose. A builder escaping a frame that
# RETURNS is the real shape but does not survive to be reported -- later calls overwrite
# the dead frame, the collector reads a garbage class word out of it, and the process
# dies with SIGBUS before any verify pass runs (measured: exit 138, no output).
printf '%-16s ' "self-test6"
seClean=""
seLeak=""
seOk=1
if ! ./translate-and-build.sh SbEscape target/bin/SbEscape-control -DCN1_GC_VERIFY \
        > target/bin/SbEscape-control-build.log 2>&1; then
    echo "BROKEN -- could not build SbEscape"
    tail -25 target/bin/SbEscape-control-build.log
    fail=1
    seOk=0
elif ! CN1_BENCH_TRANSLATOR_OPTS="-Dcn1.sbSkipEscapeValidation=true" \
        ./translate-and-build.sh SbEscape target/bin/SbEscape-leak -DCN1_GC_VERIFY \
        > target/bin/SbEscape-leak-build.log 2>&1; then
    echo "BROKEN -- could not build the escaping arm of SbEscape"
    tail -25 target/bin/SbEscape-leak-build.log
    fail=1
    seOk=0
fi
if [ "$seOk" -eq 1 ]; then
    seClean="$(CN1_GC_VERIFY_SOFT=1 CN1_GC_VERIFY_LOG=1 ./target/bin/SbEscape-control 2>&1)" || true
    seLeak="$(CN1_GC_VERIFY_SOFT=1 CN1_GC_VERIFY_LOG=1 ./target/bin/SbEscape-leak 2>&1)" || true
    # `|| true` on every one of these: grep -c EXITS 1 when the count is zero, and a
    # zero count is the expected answer for seFalse. Under `set -e` that killed the
    # script between this test's label and its verdict -- the run printed
    # "self-test6" and then nothing at all, with no GREEN and no FAILED line.
    sePasses="$(printf '%s' "$seClean" | grep -c '^\[GC-VERIFY\]' || true)"
    seFound="$(printf '%s' "$seLeak" | grep -c 'ESCAPED STACK OBJECT' || true)"
    seFalse="$(printf '%s' "$seClean" | grep -c 'ESCAPED STACK OBJECT' || true)"
    if [ "$sePasses" -eq 0 ]; then
        echo "BROKEN -- vacuous: SbEscape completed no GC cycle, so nothing was verified"
        fail=1
    elif [ "$seFalse" -ne 0 ]; then
        echo "BROKEN -- the check reported an escape in correctly compiled code"
        printf '%s\n' "$seClean" | grep 'ESCAPED STACK OBJECT' | head -3
        fail=1
    elif [ "$seFound" -eq 0 ]; then
        echo "BROKEN -- a stack builder was published into a heap field and NOTHING noticed"
        printf '%s\n' "$seLeak" | tail -5
        fail=1
    else
        echo "caught the escaped stack object ($seFound reports, $sePasses clean passes)"
    fi
fi

# Seventh self-test, for the SAME-BLOCK SATB DELETION BARRIER.
#
# A shift within one reference block (ArrayList.remove / add(int, E)) must log the old
# value of EVERY slot it overwrites. It was once narrowed to the one slot whose value
# leaves the block, on the argument that a shift only permutes references -- and that
# argument forgot the marker scanning the same block while the memmove runs. remove(0)'s
# memmove overtakes an upward scan and carries one element from the unscanned side to the
# scanned side, where neither look finds it. The self-hosting translator lost a live
# LineNumber that way about one run in twenty, while the previous version of this
# self-test stayed green: it checked the ARITHMETIC of the narrowed range against the
# narrowed contract, so it could only ever agree with the mistake.
#
# This one drives the real race. MoveRace rotates a 200,000-element list of old objects
# while another thread runs collections back to back; CN1_GC_FAULT=moverange puts the
# narrowed barrier back into cn1RefBlockMove -- log only what leaves the block, and do
# NOT queue the block for the collector's re-scan -- and the verifier must then report
# the swept elements as dangling. Measured: ~95 dangling references in every faulted
# run, none in any clean one. The same fault is what shows the deferred re-scan (see
# DEFERRED BLOCK RE-SCAN in cn1_globals.m) is load-bearing: the clean arm logs exactly
# as narrowly, and differs only in queueing the block.
printf '%-16s ' "self-test7"
rm -f ./target/bin/MoveRace-verify
if ! ./translate-and-build.sh MoveRace target/bin/MoveRace-verify -DCN1_GC_VERIFY \
        > target/bin/MoveRace-selftest-build.log 2>&1; then
    echo "BROKEN -- could not build MoveRace for the move-barrier self-test"
    tail -25 target/bin/MoveRace-selftest-build.log
    fail=1
elif [ ! -x ./target/bin/MoveRace-verify ]; then
    echo "BROKEN -- could not build MoveRace for the move-barrier self-test"
    fail=1
else
    mrClean="$(./target/bin/MoveRace-verify 2>&1)" || true
    mrFault="$(CN1_GC_FAULT=moverange ./target/bin/MoveRace-verify 2>&1)" || true
    mrCleanHits="$(printf '%s' "$mrClean" | grep -c 'GC-VERIFY. DANGLING' || true)"
    mrFaultHits="$(printf '%s' "$mrFault" | grep -c 'GC-VERIFY. DANGLING' || true)"
    # A clean run that never finished a collection verified nothing, and would look
    # exactly like a correct barrier.
    mrPasses="$(printf '%s' "$mrClean" | sed -n 's/.*SUMMARY passes=\([0-9]*\).*/\1/p' | tail -1)"
    [ -z "$mrPasses" ] && mrPasses=0
    if [ "$mrPasses" -eq 0 ]; then
        echo "BROKEN -- MoveRace finished no verify pass, so the clean arm proved nothing"
        printf '%s\n' "$mrClean" | tail -5
        fail=1
    elif [ "$mrCleanHits" -ne 0 ]; then
        echo "FAILED -- the same-block move barrier lost a live object"
        printf '%s\n' "$mrClean" | grep -A 4 'DANGLING' | head -12
        fail=1
    elif [ "$mrFaultHits" -eq 0 ]; then
        echo "BROKEN -- the narrowed move barrier was re-injected and NOTHING noticed"
        printf '%s\n' "$mrFault" | tail -5
        fail=1
    else
        echo "caught the narrowed move barrier ($mrFaultHits reports, $mrPasses clean passes)"
    fi
fi

# ---- BIBOP INVARIANT VALIDATOR ---------------------------------------------
# -DCN1_BIBOP_VALIDATE existed long before this arm and NO GATE BUILT IT, which
# is how three consecutive attempts at type-homogeneous pages ran without the
# checks that would have caught them -- including the one the sweep already
# carries verbatim ("only RETIRED pages reach the sweep"). Building it here is
# the whole point: an invariant nobody compiles is documentation, not a check.
#
# The rules and the reasoning behind each are in vm/BIBOP-INVARIANTS.md; the
# validator reports by rule number so a failure names the paragraph that says
# what breaks.
printf '%-16s ' "bibop-rules"
bvBin="target/bin/Bench-bibopvalidate"
if ./translate-and-build.sh Bench "$bvBin" -DCN1_BIBOP_VALIDATE > target/bin/bibopvalidate-build.log 2>&1; then
    bvOut="$(./"$bvBin" 3 objectAllocation 2>&1 || true)"
    bvLast="$(printf '%s' "$bvOut" | grep -oE 'VIOLATIONS=[0-9]+' | tail -1 | cut -d= -f2)"
    bvObjs="$(printf '%s' "$bvOut" | grep -oE 'objects=[0-9]+' | tail -1 | cut -d= -f2)"
    [ -z "$bvLast" ] && bvLast=""
    [ -z "$bvObjs" ] && bvObjs=0
    if [ -z "$bvLast" ]; then
        # Silence is the failure mode this arm is most likely to have: the hook is
        # post-sweep, so a run that never swept prints nothing and would otherwise
        # read exactly like a clean one.
        echo "BROKEN -- the validator never ran (no VIOLATIONS line; did a sweep happen?)"
        fail=1
    elif [ "$bvObjs" -eq 0 ]; then
        echo "BROKEN -- validator walked 0 objects, so it proved nothing"
        fail=1
    elif [ "$bvLast" -ne 0 ]; then
        echo "FAILED -- $bvLast invariant violations"
        printf '%s\n' "$bvOut" | grep -E '\[BIBOP-R[0-9]+\]' | head -8
        fail=1
    else
        echo "clean ($bvObjs objects across the page registry)"
    fi
else
    echo "BROKEN -- -DCN1_BIBOP_VALIDATE did not build"
    tail -20 target/bin/bibopvalidate-build.log
    fail=1
fi

[ "$fail" -eq 0 ] && echo "GC-VERIFY GREEN" || { echo "GC-VERIFY FAILED"; exit 1; }
