# ParparVM benchmarks & correctness gauntlet

The performance suite and torture tests used to drive (and gate) ParparVM's
optimization work. Two invariants govern everything here:

1. **Checksums are the contract.** Every benchmark and torture test computes a
   checksum that must be **bit-identical** to a host JVM running the same
   source. A divergence is a VM bug, never an acceptable trade — the harness
   refuses to print ratios when checksums mismatch.
2. **Interleaved best-of-N.** ParparVM and the host JVM run alternately per
   round, and each benchmark's floor (minimum) is compared. This rides out
   machine noise; on a loaded machine the *ratios* stay meaningful long after
   absolute numbers stop being comparable.

## Running

```bash
export JDK_8_HOME=/path/to/jdk8        # builds JavaAPI + bench sources
export BENCH_JAVA=/path/to/jdk25/bin/java   # the reference JVM (optional; default `java`)

./run-benchmark.sh          # 5 interleaved rounds, ratio table + geomean
./run-benchmark.sh 10       # more rounds
CN1_BENCH_CFLAGS="" ./run-benchmark.sh    # without ThinLTO (debug shape)

./run-gauntlet.sh           # the correctness gate: all tortures byte-identical
                            # + GC stress in cooperative AND forced-signal modes
```

Requirements: Maven and clang on `PATH` (gcc also works:
`CN1_BENCH_CC=gcc-16` — the suite is validated under both; gcc is the
compiler that exposed the setjmp/longjmp try-catch bug, so running it
periodically matters).

## What the benchmarks measure

`Bench.java` — ten workload shapes, each with in-process warmup and repeated
measurement (`BENCH <name> rep <n> ns=<t> checksum=<c>` lines):

| bench | shape |
|---|---|
| intArithmetic / longArithmetic | dependent-chain scalar arithmetic |
| mathTranscendental | sqrt/sin/cos/fmod mix |
| arraySequential | canonical `for(i<arr.length)` fill+reduce (BCE target) |
| arrayRandom | data-dependent indexing |
| objectAllocation | short-lived object churn |
| hashMapChurn | boxed-key map put/get/remove churn |
| stringBuilding | javac-style concat; the built string **escapes** into a ring buffer (an earlier consume-and-drop version measured HotSpot's escape analysis deleting the String, not string building) |
| recursion | call-overhead (fib) — a JIT-inlining shape, accepted above 1x |
| quicksort | mixed array scan/swap + recursion |

## The tortures (run by `run-gauntlet.sh`)

| test | guards |
|---|---|
| MapTorture | compact HashMap/LinkedHashMap: growth, tombstones, null keys, views, 200k PRNG op mix, insertion/access order |
| SbTorture | StringBuilder: every append overload, toString independence under later mutation, editing ops, surrogates, 100k PRNG mix |
| StrCmp | String equals/compareTo/sort incl. unicode + surrogates; charAt logical-length bounds |
| FusedTest | @Fused layout: param/computed sizes, oversize fallback, ctor guard paths, survivors across GC — **also the canary for setjmp/longjmp bugs (deliberate caught exception)** |
| IbpTest | init-before-publish allocation |
| ExcTest | exception paths |
| SoeTest | StackOverflowError must be catchable and the VM functional afterwards (guards the preallocated-SOE design) |
| ThreadChurn | thread lifecycle |
| GcStress / MtStress | allocation storms, single- and multi-threaded, in cooperative and forced-signal (`CN1_GC_SIGNAL_STOP=1`) stop modes |

## Mandatory compiler flags

Generated C **must** be compiled with
`-fwrapv -fno-strict-aliasing -fno-builtin-fmod -fno-builtin-fmodf`.
Java integer arithmetic wraps; without `-fwrapv`, clang -O3 provably
miscompiles accumulation loops (checksum off by 2^32 per overflow). The
build scripts, the Xcode template, and the cmake writer all carry these —
any new build path must too.

## Reference results

Apple M2, best-of-5 interleaved, ThinLTO, vs warmed Azul JDK 25
(2026-07, PR #5327):

| bench | ratio | | bench | ratio |
|---|---:|---|---|---:|
| stringBuilding | 0.67x | | arrayRandom | 0.96x |
| arraySequential | 0.82x | | intArithmetic | 1.07x |
| quicksort | 0.92x | | longArithmetic | 1.12x |
| hashMapChurn | 0.95x | | objectAllocation | 1.19x |
| mathTranscendental | 0.96x | | recursion | 1.60x |

**Geomean 1.00x.** int/long run at exact pure-C parity (same-flags C controls
measured identical); the residual is C2-vs-clang scheduling of the dependency
chain. recursion is HotSpot's speculative inlining — accepted.
