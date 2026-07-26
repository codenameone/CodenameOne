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
any pre-snapshot slot that is still fresh. A cycle in which the grace pass
missed nothing prints nothing -- **silence is success**. A cycle with a miss
prints one `[GRACE-AUDIT]` line reporting:

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
./target/grace-audit    # PASS: no line reports doomedChildren != 0
                        # (an empty stderr is a fully clean run; benign
                        #  missedFresh-only lines may still appear)
```

The fresh-page-stack grace scheme this audit was written against reported
100-370 missed slots and 100-250 doomed children per cycle; the
`gcAllocedSinceSweep`-pruned registry walk reports zero doomed across the
suite.

`LegacyGrace` is the same hazard on the OTHER heap: objects above
`CN1_BIBOP_MAX_OBJECT` never reach the page heap, so the page walk cannot see
them, yet the legacy sweep grants them the identical one-cycle grace. It
reports through `[GRACE-AUDIT-LEGACY]` lines with the same contract. Two things
in that driver are load-bearing and easy to get wrong when writing a new one:
the hazard must be built in a window with **no mark in flight** (during a mark
the SATB barriers cover the very reference move being tested, so a driver that
keeps the collector busy tests nothing), and the driver must **scrub its own
native stack** afterwards, because the conservative root scan marks whatever a
returned frame's leftover words still point at -- an un-scrubbed driver pins
the hazard it just built and comes back green. `CN1_GC_VERIFY_CENSUS` (below)
is how you confirm the hazard set actually ages instead of being retained.

`StormAB` (sustained single-thread storm) and `LoadLoop` (repeated
dictionary build/drop) are the matching wall-time/RSS A/B drivers.
`TimerLatency` is the sleep/timer fidelity probe for the Thread.sleep
signal-truncation defect (a single usleep was EINTR'd by the collector's
sleeping-thread stop signal, cutting Thread.sleep(3000) to ~20ms and firing
every java.util.Timer task almost immediately): it must print
`TIMER_LATENCY_OK`. If `max_early_ms` exceeds 50 or `min_sleep1500_ms` comes
out truncated, the regression is back.
`LargeArrayLoad` models the FINAL issue-5425 Dtest shape -- a persistent
small-object survivor set plus a retained LARGE (>CN1_BIBOP_MAX_OBJECT,
legacy-path) byte[] phase that produces no garbage -- and guards the
legacy-allocation GC-trigger storm (its CI twin is
`LargeArrayGcIntegrationTest` in `vm/tests`). Run it with
`CN1_GC_LOG_CYCLES=1` and count `[GC-CYCLE]` stderr lines: collection must be
volume-driven (~6 cycles on this workload), not re-armed at wall-clock
cadence by the pre-BiBOP 1MB high-frequency threshold (15 cycles when
regressed).

`ClinitThrow` is a standalone liveness reproducer (not byte-identical to the
host JVM by design — ParparVM's initialization-failure semantics differ): a
throwing `<clinit>` must release the class-init monitor so other threads
don't deadlock. Build and run it directly with `translate-and-build.sh`.

## Heap-integrity gate (`-DCN1_GC_VERIFY`, `run-gc-verify.sh`)

The gauntlet proves the VM computes the right answer. It cannot prove the
collector left the heap in a legal state, and that is the failure mode this
subsystem actually has: a dangling reference reads whatever object recycled the
slot, so nothing diverges at the point of the bug -- the damage surfaces later,
somewhere else, as corrupted data (issue 5425's "non word" dictionary entries
and its impossible NPE). Checksums are structurally blind to it.

`-DCN1_GC_VERIFY` compiles in a QA-only mode that makes the invariant directly
observable, by destroying the plausible replacement object:

- **Poison.** Every reclaimed page slot and every legacy block the sweep frees
  is stamped with a poison header and payload. This includes the O(1) all-dead
  page reclaim, which is where nearly all page memory is actually reclaimed and
  which normally drops a page without writing a single slot -- leaving every
  dead object with an intact-looking header. That is exactly why a dangling
  reference in this VM reads plausible data rather than crashing.
- **Quarantine.** Freed legacy blocks go to a ring (`CN1_GC_VERIFY_QUARANTINE`,
  default 65536) instead of back to the C allocator, so a poisoned block stays
  mapped and recognizable rather than being reused or unmapped underneath a
  dangling reference.
- **Verify.** After every sweep, each surviving object is walked through its
  own generated mark function with the collector in verify mode: instead of
  marking, every reference field is classified against the page registry, the
  live-extent index and the quarantine set. A field pointing into a freed slot,
  a recycled page, or a quarantined block is reported with the holder's class,
  the victim's class, the mark call site, and then aborted -- at the cycle that
  created it.

```bash
./run-gc-verify.sh            # tortures + drivers + the self-test
./run-gc-verify.sh GraceAudit # one driver
```

The gate holds CURRENT-EPOCH survivors to the invariant: the sweep either
marked them reachable (marking traces children, so a dangling field is a
mark-completeness bug) or promoted them by the grace rule (tracing the subtree
was the grace pass's job). Both readings make a dangling field unambiguous, so
the gate has no judgement calls in it.

**The self-test is the point of the script.** A gate nobody has watched fail is
not a gate, so the run finishes by re-injecting the exact defect #5442 fixed
(`CN1_GC_FAULT=nograce` disables the grace-subtree pass, reproducing #5436) and
requires the verifier to catch it. It does, immediately:

```
[GC-VERIFY] DANGLING REFERENCE after sweep at epoch 15
            holder  = 0xd38070050 class=com.bench.GraceAudit.Node mark=15 (epoch+0) heapPos=-3
            field   -> 0xd3806cfe0 class=com.bench.GraceAudit.Node mark=-7 heapPos=-3
            victim  = RECYCLED page slot (above bump cursor) (page-resident)
```

### Diagnostics

| variable | effect |
|---|---|
| `CN1_GC_VERIFY_SOFT=1` | report every violating cycle instead of aborting on the first |
| `CN1_GC_VERIFY_LOG=1` | one line per cycle even when clean (holders, refs checked, reclaim counts, referenced-child age histogram) |
| `CN1_GC_VERIFY_AGING=1` | ALSO hold previous-epoch survivors to the invariant. **Perturbs the collector** -- it roughly doubles the post-sweep walk, and the extra GC-thread time changes page ageing enough to move other counters by orders of magnitude (measured: early-freed slots 0 vs 205,958 from the same binary). Read it as a rough survey, never as production behaviour, and confirm anything it suggests in the default mode |
| `CN1_GC_VERIFY_ALL=1` | walk every object, including ones already given up on. Investigation only |
| `CN1_GC_VERIFY_CENSUS=<class>` | per-cycle age histogram of every resident object of a class. Use it to confirm a driver's hazard set actually ages out instead of being pinned |
| `CN1_GC_VERIFY_DUMP=<class>` | print holder/child marks for holders of a class |
| `CN1_GC_TRACE_MARK=<class>` | name the mark pass that re-marks a class each cycle -- the answer to "what is still keeping this alive?", most often `conservative-native-stack`. Add `-DCN1_BIBOP_VALIDATE` to also get the drain parent |
| `CN1_GC_FAULT=nograce` | fault injection: disable the grace-subtree pass |
| `CN1_GC_FAULT=earlyfree` | fault injection: restore the pre-fix O(1) page-reclaim bound, which freed slots the per-slot walk keeps |
| `CN1_GC_DEBUG_EARLY=1` | when `earlyFreed` is nonzero, dump the first few offending pages (epoch, `gcGraceEpoch`, `gcLastMarkedEpoch`, the slot's mark and class) -- which of the page's ageing bounds went stale is the whole diagnosis |

Every run prints a summary at exit, and the harness requires `passes` to be
nonzero -- a workload that never completes a collection cycle never runs the
verifier, so a clean result from it would mean only that nothing was checked:

```
[GC-VERIFY] SUMMARY passes=5 refs=952340 violations=0 earlyFreed=0 resurrected=0 resurrectedDangling=0
```

- `earlyFreed` -- slots the O(1) whole-page reclaim dropped that the per-slot
  walk would have KEPT. That shortcut claims a byte-identical outcome, so any
  nonzero value is the two rules disagreeing about when an object dies, which
  is how a kept object ends up referencing reclaimed memory. Must be 0.
- `resurrected` / `resurrectedDangling` -- objects marked live again after
  ageing past the sweep's keep threshold (overwhelmingly by the conservative
  native-stack scan revisiting a returned frame's leftover word), and how many
  of those still referenced reclaimed memory. The second must be 0; the first
  is informational, and is normally 0 or 1 because a stale word that revives an
  object usually keeps marking it every cycle rather than letting it age out
  and come back.

The mode is deliberately asymmetric about uncertainty: a reference it cannot
place (allocated after the snapshot, unmapped, mid-construction body) is
skipped. It reports only what it can prove, so a violation is never a false
alarm, at the cost of catching some real defects a cycle later than it could.

Cost is a full extra heap walk per cycle plus poison writes, so it is a
correctness gate, never a perf configuration -- run it alongside the gauntlet,
not with it.

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
