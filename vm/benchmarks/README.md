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

./run-bibop-adaptive.sh     # issue-5425 retained-small-array correctness,
                            # adaptive-policy, wall-time, and peak-RSS gate
```

Requirements: Maven and clang on `PATH` (gcc also works:
`CN1_BENCH_CC=gcc-16` — the suite is validated under both; gcc is the
compiler that exposed the setjmp/longjmp try-catch bug, so running it
periodically matters).

## What the benchmarks measure

`CommonWorkloads.java` defines the ten workload shapes shared with the generated
port applications. `Bench.java` is its standalone in-process warmup and repeated
measurement runner (`BENCH <name> rep <n> ns=<t> checksum=<c>` lines):

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

## Adaptive BiBOP regression gate

`run-bibop-adaptive.sh` reproduces the allocator shape from issue 5425 at its
reported scale: 560,000 retained small `byte[]` values, temporary key arrays,
and continued churn across several completed GC epochs. The Java workload checks
every sampled retained array after each collection and asserts a checksum produced
by the host JVM. The harness additionally requires runtime evidence that:

- the BiBOP-only allocator thread graduated to the high-throughput pacing tier;
- the 24 MiB baseline trigger grew under sustained survival;
- a survivor-heavy size class activated the bounded legacy bypass/reprobe path;
- grace marking slot-scanned only pages flagged `gcAllocedSinceSweep` rather
  than every slot of the grow-only page registry.

It then measures best wall time and per-process peak RSS for the production
adaptive collector against the legacy collector and a no-pacing diagnostic build.
Those compile-time variants are QA controls only; applications ship one collector
with the adaptive behavior enabled, not user-selectable GC flags.

## Grace-completeness audit (`-DCN1_GRACE_AUDIT`)

The concurrent collector gives fresh (`gcMark == -1`) BiBOP objects one cycle
of sweep grace, so an object reachable ONLY through a surviving fresh object
must be traced by the mark's grace pass or the sweep frees it while it is
still referenced (the issue-5425 dictionary corruption). `-DCN1_GRACE_AUDIT`
compiles in a QA-only pre-sweep pass that snapshots every page's bump cursor
at mark start and, right before the sweep, full-walks the registry tracing
any pre-snapshot slot that is still fresh. It reports per cycle:

- `missedFresh` — fresh slots the grace pass did not visit. Small counts can
  be benign (a free-list slot re-allocated mid-mark, below the snapshot, after
  the grace pass ran — SATB covers its links this cycle and the sticky
  `gcAllocedSinceSweep` flag re-traces it next cycle).
- `doomedChildren` — objects that became marked ONLY by tracing those missed
  slots. **Any nonzero value is a collector bug**: without the audit pass the
  sweep would free each of them while a surviving object still references it.

`GraceAudit` is the driver shaped to break queue/dedup-based grace schemes:
`System.gc()` is asynchronous, so a single thread allocates dropped fresh
nodes (each holding the only reference to an older object) WHILE the mark
runs, then goes quiet across the next cycle. Gate:

```bash
./translate-and-build.sh GraceAudit target/grace-audit -DCN1_GRACE_AUDIT
./target/grace-audit    # stderr must show doomedChildren=0 on every line
```

The fresh-page-stack grace scheme this audit was written against reported
100-370 missed slots and 100-250 doomed children per cycle; the
`gcAllocedSinceSweep`-pruned registry walk reports zero doomed across the
suite. `StormAB` (sustained single-thread storm) and `LoadLoop` (repeated
dictionary build/drop) are the matching wall-time/RSS A/B drivers.

`ClinitThrow` is a standalone liveness reproducer (not byte-identical to the
host JVM by design — ParparVM's initialization-failure semantics differ): a
throwing `<clinit>` must release the class-init monitor so other threads
don't deadlock. Build and run it directly with `translate-and-build.sh`.

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

### SATB write-barrier cost (concurrent-mark correctness fix)

The concurrent collector gained a Yuasa snapshot-at-the-beginning (SATB) deletion
write barrier on every heap object-reference store, to close a cross-thread mark
race (a released/native mutator moving or nulling the last snapshot-time reference
to a live object before mark completes — the intermittent Linux mid-suite crash).
Off-mark the barrier is a single predicted-not-taken `gcSatbActive` load; the
old-value read + enqueue runs only during the (infrequent) mark. Measured cost is
within run-to-run noise — a same-machine A/B (`-DCN1_DISABLE_SATB` vs default,
best-of-5 interleaved) moved the geomean by **+0.01x (1.00x → 1.01x)**, with the
store-heavy shapes flat or non-monotonic (hashMapChurn 0.96→0.95,
objectAllocation 1.18→1.17, stringBuilding 0.64→0.66 — deltas at the noise floor
of a barrier-free control such as recursion). The barrier can be compiled out with
`-DCN1_DISABLE_SATB` for A/B measurement or as a fallback.
