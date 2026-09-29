#!/bin/bash
# ParparVM correctness gauntlet: every torture suite must produce output
# BYTE-IDENTICAL to the host JVM, plus GC stress rounds in both cooperative
# and forced-signal thread-stop modes. This is the gate every VM change must
# pass -- a checksum divergence is a codegen/GC bug by definition.
#
#   run-gauntlet.sh
#
# TWO references, not one, because Java itself has two answers.
#
# JDK-4511638 replaced FloatingDecimal with the Raffaello Giulietti shortest-repr
# algorithm in Java 19, so Double/Float.toString changed for values whose old rendering
# carried a redundant digit. Measured on this machine, all from one unchanged BoxEdge:
#
#     JDK 25 / 21 / 19   4.611686018427388E18    2.1474836E9
#     JDK 17 / 11 / 8    4.6116860184273879E18   2.14748365E9
#     ParparVM           4.611686018427388E18    2.1474836E9   (cn1ShortestDouble)
#
# ParparVM implements the modern algorithm deliberately, so the reference JVM's VERSION
# decides whether BoxEdge passes. That made the gate silently dependent on $PATH: run it
# in a shell that has sourced tools/env.sh -- which every other instruction here tells you
# to do, and which puts JDK 8 on PATH -- and BoxEdge reports DIVERGE on a tree with no bug
# in it. Worse in the other direction: the 'fix' that reading suggests is to make the VM
# emit the pre-19 form, which would break it against every JDK anyone still ships.
#
# So the gate now pins a MODERN reference and checks the LEGACY one as well:
#
#   * the target must be byte-identical to the modern JDK (19+) -- the shipping contract;
#   * the target must also equal the legacy JDK (8) on every line where the two JDKs
#     agree with each other. Lines where they disagree are the era-dependent ones, and
#     they are derived by diffing the two references rather than hand-listed, so no list
#     can go stale.
#
# What that buys: a real divergence that happens to land on a floating-point line is still
# caught, because it would differ from legacy on a line where legacy and modern agree.
#
# Requirements: JDK_8_HOME, a JDK 19+ as BENCH_JAVA or `java` on PATH, Maven, clang.
set -e
cd "$(dirname "$0")"
REF_JAVA="${BENCH_JAVA:-java}"
J8="${JDK_8_HOME:?set JDK_8_HOME}"

# Refuse a pre-19 reference outright rather than reporting its era difference as a VM bug.
# Java 8 spells it "1.8", everything since spells it "17"/"25" -- take the part after
# the "1." when there is one, or the whole number when there is not, so the refusal names
# the version a human recognises instead of reporting JDK 8 as "Java 1".
refFeature="$("$REF_JAVA" -XshowSettings:properties -version 2>&1 \
    | sed -nE 's/.*java\.specification\.version = (1\.)?([0-9]+).*/\2/p' | head -1)"
if [ -z "$refFeature" ] || [ "$refFeature" -lt 19 ] 2>/dev/null; then
    echo "run-gauntlet: reference JVM is Java ${refFeature:-unknown} ($REF_JAVA)" >&2
    echo "  Java 19+ is required: Double/Float.toString changed in 19 (JDK-4511638) and" >&2
    echo "  ParparVM implements the modern form, so an older reference reports BoxEdge as" >&2
    echo "  DIVERGE on a correct tree. Set BENCH_JAVA to a JDK 19 or newer." >&2
    echo "  Note tools/env.sh puts JDK 8 on PATH, which is how this is usually hit." >&2
    exit 2
fi
echo "run-gauntlet: modern reference Java $refFeature ($REF_JAVA); legacy reference $J8"

# TaggedSync guards Java monitor semantics on tagged boxed Integers (mutual
# exclusion + wait/notify) -- regression test for the tagged monitorEnter/Exit
# no-op bug a review caught.
#
# BoxEdge is the gate for the tagged immediates as a whole: all six boxed types
# (Integer/Long/Double/Float/Character/Short) crossed with tagged, heap-allocated,
# null and wrong-type receivers, over getClass/instanceof/equals/hashCode/compareTo,
# the collections, NaN and both zeroes, monitors, and Long/Double values chosen to
# fall OUTSIDE the taggable range so the heap fallback is exercised rather than
# assumed. It catches the failure this scheme makes easy -- a wrong class or a wrong
# hash, returned silently with nothing thrown.
#
# ConcatCorrupt is heap integrity ACROSS the fused string-concat natives, which take
# raw interior pointers into their source strings' byte[] and then ALLOCATE. Read its
# javadoc before trusting it: the sources turn out to be rooted in BOTH configurations
# (conservatively via the native frame, precisely via the caller's stack slots), so its
# non-vacuity is UNPROVEN. It earns its place as a heap-integrity torture -- 256 int[]
# verified across 400,000 concatenations -- not as proof of a rooting claim.
#
# ToCharT gates the toCharArray() elision pass: three shapes it must rewrite and four it
# must REFUSE (mutation, two escapes, a computed index). Rewriting a refused shape is a
# wrong answer rather than a crash, which is precisely what a checksum catches.
#
# ForEachT is the semantics gate for for-each loop specialization: the shapes a loop
# duplicator gets wrong (break, continue, early return, nesting, a try/catch inside the
# body) crossed with receivers that must and must NOT take an ArrayList fast path. It
# prints per receiver rather than only a checksum, so a failure names the shape.
#
# MapTorture2 is the object-keyed companion to MapTorture. MapTorture is
# HashMap<Integer,Integer> throughout and an Integer is a TAGGED IMMEDIATE, so it neither
# allocates nor gets traced -- it never held a real reference across a collection. That
# gap hid a live-object loss: MapTorture2 failed on its first run with "LOST held key k1",
# and MapTorture, the gauntlet and the self-hosting gates were all green at the time.
#
# LambdaT gates the non-capturing-lambda singleton: a lambda with no captured fields has no
# distinguishable instances, so the factory returns a shared one instead of allocating per
# evaluation. It deliberately does NOT assert reference identity between evaluations -- the
# JLS does not guarantee a lambda expression yields a new object and the JDK caches
# non-capturing instances too, so `a == b` is unspecified on both sides. What it does check
# is that capturing lambdas still see their OWN captures, which is what a shared-instance
# bug would break first.
# BceTryCatch pins bounds-check elimination against exception edges. BCE used to be
# refused for any method with a try/catch and is now refused per LOOP, only when a
# handler lands inside one -- so these shapes put a counted array loop on both sides of
# that line. The sharp case is shorterArrayInBody: the loop is proven over a, indexes a
# HALF-LENGTH b with the same i, and the AIOOBE it must still throw is the thing a
# proof widened from "this array" to "any array" would swallow.
# NestThrow is four throws, and it exists because the third one escaped its catch.
# A throw from INSIDE a catch handler was not caught by a try lexically around it:
# nested try/catch made both regions begin at the same instruction, the inner one
# cached its END_TRY depth before the outer one had registered, and entering the inner
# handler therefore deregistered the outer try along with it. It is four lines of Java
# and it was wrong on every platform, which is the argument for keeping the trivial
# shapes in the list beside the elaborate ones.
# BceHoisted is the same pass' other half: the length hoisted into a local, which is
# what nine tenths of this corpus' counted loops compile to. Proving n IS a.length is a
# claim about two slots rather than one, so the shapes that break it are all in there --
# the array replaced after the capture, the length local written twice, one length
# bounding two arrays of different sizes. Those cases must still THROW, and a wrong
# proof does not throw, it reads past the end of a heap object.
# SbTryCatch is the StringBuilder half of the same question BceTryCatch asks. One
# try/catch anywhere in a method used to disable implicit stack allocation for every
# builder in it; it no longer does, so these are the shapes that test the claim rather
# than assert it -- a builder read inside a handler, one built there, one that outlives
# the try that filled it, and two that are stored into a FIELD and therefore must stay
# on the heap. A wrong answer here is a C-stack address reaching the heap, which the
# verifier's escaped-stack-object check (run-gc-verify.sh self-test6) is what catches.
TORTURES="SbTryCatch BceTryCatch BceHoisted NestThrow MapTorture MapTorture2 IdmTorture HtTorture SbTorture StrCmp FusedTest IbpTest ExcTest ThreadChurn SoeTest TaggedSync BoxEdge ConcatCorrupt ToCharT ForEachT LambdaT FeMin Latin1T SbLatin1T SbCapacityT PureClinitT MonitorChurnT SetTorture InstanceOfT WriterT StrQueryT LambdaDevirtT TwinProbe NullDeref ThrowingFinalizer"
mkdir -p target/host-classes target/bin
# FusedTest uses @com.codename1.annotations.Fused -- supply the annotation
# source for the host compile (ParparVM's JavaAPI carries its own copy)
"$J8/bin/javac" -nowarn -encoding UTF-8 -d target/host-classes \
    ../../CodenameOne/src/com/codename1/annotations/Fused.java \
    common/src/main/java/com/bench/CommonWorkloads.java src/com/bench/*.java

fail=0
for t in $TORTURES; do
    ./translate-and-build.sh "$t" "target/bin/$t" > /dev/null
    a="$(./target/bin/$t 2>/dev/null | grep -v '^\[')"
    # The SAME filter on both sides. It used to be applied only to the target, so a
    # torture that emitted a "[...]" diagnostic diverged against its own host run -- and
    # stderr is no way around that, because System.err reaches fd 1 on the clean target.
    b="$("$REF_JAVA" -cp target/host-classes "com.bench.$t" 2>/dev/null | grep -v '^\[')"
    if [ -z "$a" ] || [ "$a" != "$b" ]; then
        echo "$t: DIVERGE"
        diff <(printf '%s\n' "$b") <(printf '%s\n' "$a") | head -8
        fail=1
        continue
    fi
    # Second reference: the legacy JDK. The target already equals modern, so any line
    # where it differs from legacy must be a line where the two JDKs differ from each
    # other -- an era-dependent rendering. A line where the JDKs AGREE and the target
    # does not is a genuine bug that the modern comparison alone would have accepted
    # only if modern itself had regressed; checking both closes that gap.
    l="$("$J8/bin/java" -cp target/host-classes "com.bench.$t" 2>/dev/null | grep -v '^\[')"
    if [ "$a" = "$l" ]; then
        echo "$t: MATCH (both)"
    else
        # Lines that differ between the two REFERENCES: the era-dependent set.
        eraOnly="$(diff <(printf '%s\n' "$l") <(printf '%s\n' "$b") | grep -c '^<' || true)"
        targetVsLegacy="$(diff <(printf '%s\n' "$l") <(printf '%s\n' "$a") | grep -c '^<' || true)"
        if [ "$eraOnly" -gt 0 ] && [ "$targetVsLegacy" -eq "$eraOnly" ] \
           && [ -z "$(diff <(diff <(printf '%s\n' "$l") <(printf '%s\n' "$b")) \
                          <(diff <(printf '%s\n' "$l") <(printf '%s\n' "$a")))" ]; then
            echo "$t: MATCH (modern; $eraOnly line(s) era-dependent vs Java 8)"
        else
            echo "$t: DIVERGE from Java 8 on lines the JDKs agree on"
            diff <(printf '%s\n' "$l") <(printf '%s\n' "$a") | head -8
            fail=1
        fi
    fi
done

./translate-and-build.sh GcStress target/bin/GcStress > /dev/null
./translate-and-build.sh MtStress target/bin/MtStress > /dev/null
for mode in "" "CN1_GC_SIGNAL_STOP=1"; do
    label="${mode:-cooperative}"
    for i in 1 2 3 4 5; do
        out="$(env $mode ./target/bin/GcStress 2>/dev/null | tail -1)"
        case "$out" in *FAIL*|"") echo "GcStress[$label] run $i: FAILED ($out)"; fail=1;; esac
    done
    for i in 1 2 3; do
        out="$(env $mode ./target/bin/MtStress 2>/dev/null | tail -1)"
        case "$out" in *FAIL*|"") echo "MtStress[$label] run $i: FAILED ($out)"; fail=1;; esac
    done
    echo "GcStress+MtStress[$label]: done"
done

[ "$fail" -eq 0 ] && echo "GAUNTLET GREEN" || { echo "GAUNTLET FAILED"; exit 1; }
