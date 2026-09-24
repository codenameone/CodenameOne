# ParparVM selfhost: performance theorems, experiments, red-team

Corpus: `scripts/hellocodenameone/.../macos-build/{classes,macPort}` (5782 classes).
Reference: JDK 25 = 6.56s / 1319MB / 2916 emitted .c files.

Every run is gated on `rc==0` AND `cfiles==2916`. This gate exists because a run
that crashed early first read as a *fast* run: without `CN1_RESOURCE_PATH`,
`Class.getResourceAsStream` returns null and `ByteCodeTranslator.copy()`
dereferences it -- a bare SIGSEGV, no message. Under lldb the first stop is
SIGUSR2 (the GC's stop-the-world signal), which hides the real fault until it is
passed through.

## Measurement hygiene

- Arms are INTERLEAVED across reps, not grouped: sequential A-then-B on this
  hardware carried a 10-15% thermal bias.
- `load1` is recorded per run. Wall clock on this box has been observed to vary
  63s..93s for the SAME binary and config as load moves; PEAK FOOTPRINT is stable
  to ~3%. Treat memory results as tight and wall-clock results as needing reps.
- Memory is `phys_footprint` ("peak memory footprint" from `/usr/bin/time -l`),
  never `ps rss` -- rss gave 151/207/219MB for one unchanged binary previously.
- The output hash is over file CONTENTS keyed by path RELATIVE to the output dir.
  Hashing absolute paths made every arm differ by construction (the dir name IS
  the arm), which would have read as "GC config changes emitted C".

## Known confounds

- `fseventsd` runs at ~100% CPU during every run, reacting to 2916 files written.
  Constant across arms, but it is a real contributor to load.
- Load is largely SELF-inflicted (the benchmark + fseventsd), not an external job.
- `-DCN1_GC_CONFORM` adds instrumentation overhead: use it for ATTRIBUTION AND
  RATIOS WITHIN A RUN, never for absolute wall-clock comparison against x_base.

## Build-flag trap (cost a whole experiment)

`parpar-O3` was built with `-DCN1_GC_MARK_THREADS=4` inherited from
`CN1_SELFHOST_CFLAGS`, NOT the source default of 1. The first worklist experiment
was built without that flag, so it ran serial and conflated worklist size with
marker count. Every arm is now built with fully explicit flags.

---

## T1 -- wall clock is bound by pacing backpressure, not by GC CPU

Two independent methods, both done, agreeing:

- M1 (sampling profiler): main thread 93.3% of samples in
  `cn1BibopAlloc -> cn1BibopMaybeGc -> cn1PacingPark -> usleep -> __semwait_signal`.
- M2 (mechanism removal): raising `CN1_GC_PACING_CAP_MB` so the cap never binds
  cuts wall from ~90s to 25.4s.
- M3 (in-process, pending): `-DCN1_GC_CONFORM` prints
  `[GCSTALL] cause=PACING_VOLUME count= totalMs=`, a non-sampling measure.

VERDICT: confirmed. The premise "GC runs on another core so it is free in wall
clock" does not hold: GC throughput gates the mutator through admission control.

## T2 -- ORIGINAL CLAIM, FALSIFIED

Claimed: the ~10GB footprint is caused by `cn1PacingPastGrowthFloor()` returning
`freeMem/4` (~10GB on this 64GB box), so the cap ceiling never engages.

Falsified by the cap sweep: at `CN1_GC_PACING_CAP_MB=256` the footprint is still
6586MB, and peak RISES with the cap (6.6 -> 10.3 -> 12.6 -> 12.7GB) and saturates.
Footprint is not set by the pacing cap. Note also that the cap override
SHORT-CIRCUITS `cn1BibopPacingCap` entirely, including the growth-floor gate, so
this sweep never tested the growth-floor mechanism in the first place -- the two
claims were being conflated.

## T2' -- the footprint floor is pages never returned to the OS

- M1 (direct trace, done): `CN1_LOG_PAGE_RELEASE=1` shows 7 release events over
  the run, FIVE of them clamped at exactly `taken=1024` = `CN1_BIBOP_RELEASE_PER_SWEEP`.
  Total 6016 pages x 64KB = 376MB returned against a 10GB peak. `rejected=0`,
  `releaseErrno=0`, so `MADV_FREE_REUSABLE` is succeeding where it is attempted.
  Cadence: page-returning "major" sweeps run only when a cycle is QUIET
  (< trigger/4 = 6MB allocated) or every `CN1_BIBOP_MAJOR_SWEEP_CYCLES`=16 cycles.
  This run is 22 cycles and never goes quiet.
- M2 (mechanism perturbation, DONE -- FALSIFIES THE THEOREM): `x_trim` =
  `-DCN1_BIBOP_MAJOR_SWEEP_CYCLES=1 -DCN1_BIBOP_RELEASE_PER_SWEEP=131072`
  released 23790 pages = 1487MB, FOUR TIMES base's 376MB, and never hit its
  budget (largest single sweep 8544 << 131072). Peak footprint did not move:

  | arm  | wall (2 reps) | peak MB       |
  |------|---------------|---------------|
  | base | 62.9 / 64.9   | 10047 / 10003 |
  | trim | 72.4 / 71.2   | 10169 /  9881 |

  So the budget WAS binding and lifting it DOES free 4x more memory, but the
  peak is a high-water mark that is re-dirtied immediately. Trim cadence is not
  the floor. `x_trim` is also ~12% SLOWER, from the extra madvise syscalls.

  All four runs emit byte-identical C (sha `d6e7d4e2e350`): GC configuration
  does not change translator output. That is a validation result in its own
  right, and it is why the hash column is worth carrying.

RIVAL EXPLANATIONS this pair cannot separate on its own, and which "x_trim did
not help" would equally predict:
  (a) FRAGMENTATION -- partially occupied pages cannot be released at any budget.
  (b) REPRESENTATION SIZE -- the translator holds all 5782 parsed classes live for
      the whole run; JDK 25 holds the same graph in 1319MB total. If parpar's
      per-object cost is ~5x, the floor is the live set itself.
Discriminator: `-DCN1_ALLOC_CENSUS` + `CN1_HEAP_REPORT=1` prints
`[LIVE:exit] occupied N objects X.XXMB`, a DIRECT measure of occupied bytes.
If occupied ~= 6GB the floor is (b); if occupied is ~1.5GB with a 10GB footprint
it is trim/float; fragmentation shows as occupied-pages-mostly-empty.

## T3 -- mark helpers are idle; marking is effectively serial

- M1 (profiler, done): 3 helper threads 99.2% idle in `__psynch_cvwait`
  (33538/33798 samples). GC thread ~100% busy: 33.4% `gcMarkDrain` (the SERIAL
  drain), 28.4% `cn1ConservativeResolve`, 20.5% gcMarkObject/gcMarkArrayObject.
- M1b (static, done): only ONE of ~15 `gcMarkDrain*` call sites is
  `gcMarkDrainParallel`. The grace pass, full drains and overflow rescan are all
  serial. Telemetry for the run: graceDrains=521 fullDrains=219 over cycles=22.
- M2 (pending): wall clock for `-DCN1_GC_MARK_THREADS=` 1 vs 4 vs 8 should be
  ~equal if the helpers contribute nothing. Falsified if mt8 is materially faster.

## T4 -- mutator assist almost never fires

- M1 (profiler, done): zero samples in `cn1GcMutatorAssist` anywhere in the file.
  Under `cn1PacingPark`, ~31,000 samples are `usleep`; exactly one park subtree
  shows 15 samples of real marking (`__GC_MARK_java_util_HashMap`). ~0.05%.
- M2 (pending): `CN1_GC_NO_MUTATOR_ASSIST=1` should not change wall clock.

CORRECTION TO AN EARLIER CLAIM: "assist is inert" is too strong for THIS binary.
`cn1GcMutatorAssist` bails when `gcMarkActiveWorkers <= 0`, which the code
documents is "true only on the parallel path" -- so it is fully inert only in a
true serial build (`gcMarkThreadCount == 1`, the SOURCE default). In the
4-marker binary actually profiled it is armed during the one parallel drain site
and idle the rest of the time.

## T5 -- mark-worklist overflow is not the bottleneck

Overflow fires in 9 of 22 cycles (64K-entry fixed worklist), forcing the serial
BiBOP page-rescan.

- M1 (pending, first attempt INVALID -- built without `-DCN1_GC_MARK_THREADS=4`):
  wall clock at worklist 8192 vs 65536 vs 1048576.
- M2 (pending): `-DCN1_GC_CONFORM` prints
  `[GCSTALL] rescanPasses= rescanUseful= rescanSlots= rescanPushes=`, a direct
  measure of how much rescan work each worklist size causes.

Working hypothesis to be tested, not yet evidence: the page-rescan may have
better locality than pointer-chasing a 10GB heap, making overflow a MITIGATION
rather than a cost.

## The efficient frontier (scouting pass, 1 rep, load 17-38)

| cap MB   | wall  | peak MB |
|----------|-------|---------|
| 256      | 90.5  | 6586    |
| 1024     | 47.4  | 10265   |
| 4096     | 30.5  | 12598   |
| 16384    | 25.4  | 12667   |
| default  | 70.3  | 10246   |

The DEFAULT policy is on neither end of this frontier. Needs reps before it is
load-bearing, given the observed wall-clock variance.

---

# Round 2: what actually causes the 8x retention and 10x wall clock

JDK 25 runs the SAME program with the same allocation pattern at 1319MB peak;
parpar peaks at 10486MB. Emitted output is 245MB of C. So the gap is RETENTION
and THROUGHPUT, not allocation volume.

## Assumptions under test

- A1 MARK-THROUGHPUT BOUND (with positive feedback). Cycle duration scales with
  heap size; the mutator allocates all through a cycle; so the heap grows; so
  cycles lengthen. Small changes in mark throughput should produce
  DISPROPORTIONATE changes in end-to-end time.
- A2 DEFERRED RECLAMATION. grace+aging holds dead objects ~3 cycles, so float is
  ~3x per-cycle allocation regardless of cycle length. Predicts allocated-per-cycle
  stays ~constant (pinned to the trigger) and occupancy is a fixed multiple of it.
- A3 SERIAL RESCAN EXPLOSION. Worklist overflow drops the collector into an
  O(heap) page-rescan fixpoint (fires in 9 of 22 cycles).

## EXP-1 (perturbational): mark parallelism dose-response, at TWO SCALES

Small corpus (395 emitted files), 2 reps, all four arms byte-identical output:

| arm     | wall      | peak MB   |
|---------|-----------|-----------|
| mt1     | 1.19/1.10 | 1239/1329 |
| mt4     | 1.16/1.13 | 1128/1271 |
| mt1wl1m | 1.15/1.10 | 1332/1351 |
| mt4wl1m | 1.07/1.06 |  881/893  |

No effect at small scale -- which RULES OUT any fixed per-cycle cost.

Large corpus, fixed 240s budget (a full mt1 run is ~3.5h, so progress rate
replaces time-to-completion):

| arm     | outcome      | .c files | cull phase |
|---------|--------------|----------|------------|
| mt1     | TIMEOUT 240s |    34    | 17s        |
| mt1wl1m | TIMEOUT 240s |    11    | --         |
| mt4     | finished 70s |   2916   |  5s        |

A 4x change in mark parallelism produces a ~60x change in end-to-end time, and
ONLY at scale. That superlinearity is the signature of a feedback loop, and it
supports A1.

A3 IS FALSIFIED: a 16x larger worklist did not rescue mt1, it made it WORSE
(11 files vs 34). Overflow/rescan is not the serial pathology. This also
independently reconfirms the earlier worklist result -- a bigger worklist is
neutral-to-worse everywhere it has been tried, so overflow behaves as a
MITIGATION rather than a cost.

CONFOUND IN EXP-1, still open: `gcMarkThreadCount == 1` removes the helper
threads AND disables `cn1GcMutatorAssist` (which bails on
`gcMarkActiveWorkers <= 0`, true only on the parallel path). mt1-vs-mt4 cannot
say which of the two matters. The `noassist` arm (mt4 + CN1_GC_NO_MUTATOR_ASSIST=1)
separates them: if it behaves like mt4 the helpers are decisive; if it behaves
like mt1 the assist is.

## EXP-2 (observational): per-cycle accounting, no perturbation

`-DCN1_GC_INSTRUMENT` emits per epoch
`[BIBOP-ADAPT] allocatedMB= triggerMB= occupiedMB= liveMB= reclaimedMB=`,
computed by the SWEEP ITSELF rather than by the census walker -- an independent
instrument for the same quantities.

Discriminator registered before the run:
- A1 predicts allocatedMB PER CYCLE GROWS over the run as cycles lengthen, and
  occupancy grows superlinearly.
- A2 predicts allocatedMB per cycle stays ~pinned to triggerMB, with occupancy a
  fixed ~3-4x multiple of it.

## CORRECTION to the earlier T3 claim

"Helpers are 99.2% idle" was a NORMALIZATION ERROR: 99.2% was measured against
total wall clock, which is dominated by the mutator's pacing sleep, not against
mark time. The mt1/mt4 result shows the parallel path is decisive at scale.

## EXP-2 RESULT: A1 CONFIRMED, A2 REFUTED

Per-cycle accounting from `-DCN1_GC_INSTRUMENT` (61.98s run, 2916 files, 10756MB):

| epoch | allocatedMB | triggerMB | occupiedMB | liveMB | reclaimedMB |
|-------|-------------|-----------|------------|--------|-------------|
| 13    |    35.1     |    96     |    157     |   41   |    39.6     |
| 14    |    84.1     |    48     |   2602     |   68   |    35.6     |
| 15    |  2518.8     |    24     |   5082     |  699   |    35.9     |
| 16    |  2427.4     |    24     |   5238     | 1156   |  1844.1     |
| 17-21 |   192.0     |    24     |  338-677   | 41-96  |  47-226     |

ONE CYCLE ALLOCATED 2518MB AGAINST A 24MB TRIGGER -- a 105x overshoot.

- A2 predicted allocatedMB pinned near triggerMB. It is not: the trigger loses
  all control once a cycle is in flight, because a new cycle cannot start until
  the current one finishes. REFUTED.
- A1 predicted allocated-per-cycle grows as cycles lengthen. Confirmed:
  35 -> 84 -> 2519 -> 2427.
- The LIVE set is 41-1156MB for the whole run. Occupied peaks at 5238MB, ~5x
  live, and the 10.3GB footprint is entirely produced by epochs 14-16.
- Epochs 17-21 pin at exactly 192.0MB = `CN1_BIBOP_GC_MAX_TRIGGER_BYTES`.

## EXP-1 CONFOUND CLOSED: it is the HELPERS, not the assist

| arm      | wall (2 reps) | peak MB       |
|----------|---------------|---------------|
| mt4      | 77.1 / 102.1  | 10277 / 10257 |
| noassist | 81.1 /  77.9  | 10170 / 10295 |
| mt8      | 77.0 /  66.6  |  9886 /  9848 |

`CN1_GC_NO_MUTATOR_ASSIST=1` changes nothing, so the mt1 catastrophe is the loss
of the HELPER THREADS, not the loss of the assist. mt8 is marginally better than
mt4 on both time and peak -- diminishing returns past 4, consistent with A1.

Every arm in every round emits byte-identical C (`d6e7d4e2e350`).

## Conclusion

Mark throughput is the gating resource. Allocation is bounded only by the pacing
park, whose cap is sized off AVAILABLE MACHINE RAM (`fm/8`, or `fm/2` for a
thread flagged high-throughput), so a mutator can run GBs ahead of a collector
that has not finished its cycle. That is what turns a ~100MB-1.2GB live set into
a 10GB footprint, and what makes wall clock 93% sleep.

The bound should be a function of the LIVE SET and the collector's measured mark
rate, not of machine RAM. `cn1ConservativeResolve` at 28.4% of GC-thread time is
the largest single mark cost and the obvious first target for raising that rate.

NOT YET TESTED: whether raising mark throughput actually collapses the footprint
(the feedback loop predicts it should improve BOTH time and memory together,
rather than trading them off as the pacing cap does).

## MECHANISM CORRECTION (verified in code, not assumed)

An earlier write-up said the trigger "loses control because a new cycle cannot
start until the current one finishes", implying trigger crossings are lost while
a cycle runs. That is WRONG, and the code says so explicitly: the crossing is
LATCHED to one request per cycle window and is "deliberately NOT suppressed while
a cycle is running". The `!gcCurrentlyRunning` suppression that did behave that
way was removed as issue 5537 -- it starved the collector, because
`bibopBytesSinceGc` is zeroed at cycle START so the mutator re-crosses the
trigger all cycle and every crossing was discarded; by the time the cycle ended
every mutator was parked on the run-ahead cap and no crossing was left to make
the request (measured: mark 40ms, mutator park 212ms).

The real mechanism is plainer. There is ONE collector running ONE cycle at a
time, `bibopBytesSinceGc` resets at cycle start, and the mutator keeps allocating
for the cycle's whole duration, bounded only by the pacing cap. So

    allocated per cycle = cycle duration x allocation rate

At seconds per cycle and ~GB/s that is gigabytes -- the measured 2518MB.

WHY THE CORRECTION MATTERS FOR THE FIX: the trigger cannot be made more
responsive, because it already is. Only two levers remain -- shorten cycles
(mark throughput), or throttle allocation to actual collector progress (a pacing
cap derived from live set and measured mark rate rather than machine RAM).
That is exactly what the mark-throughput dose-response is measuring.

## CORRECTION to the "Conclusion" section above

That section says mark throughput gates everything and implies bounding the heap
and raising mark throughput are the same fix, via a feedback loop:
faster marking -> shorter cycles -> less allocation per cycle -> smaller heap.

THE LOOP DOES NOT CLOSE. Its forward direction is directly testable by varying
mark parallelism, and the heap does not respond:

| arm  | peak MB (2 reps) | wall, min of 2 |
|------|------------------|----------------|
| mt2  |  9904 / 10236    | 62.75          |
| mt4  | 10541 / 10328    | 75.48          |
| mt8  | 10264 / 10366    | 60.14          |
| mt16 | 10211 / ...      | 62.54          |

Going 2 -> 16 markers is a large change in marking speed and peak memory is flat
within 4%. Wall clock is non-monotonic and dominated by machine noise (the same
binary and config measured 62.75s and 93.65s), so it cannot rank these arms at
all; see the load discussion above.

The supported model is a FLOOR, NOT A LOOP:

  - BELOW a mark-throughput floor (mt1, one marker) the system collapses: 60x at
    scale, and no effect whatsoever at small scale.
  - ABOVE the floor, heap size is set by the PACING CAP -- which is derived from
    machine RAM -- and is insensitive to how fast marking runs.

So the two levers are SEPARATE, and the earlier claim that they are one fix was
wrong. The memory lever is the cap. The throughput floor is a distinct constraint
that only binds if you fall below it.

This also weakens the prediction behind `resolveshare.sh`: if faster marking does
not shrink the heap, the converse (a smaller heap making marking cheaper) rests
only on the cache-size argument, not on measurement. The experiment is still
worth running because it measures a different quantity -- resolve's SHARE at a
GIVEN heap size, rather than heap size at a given mark rate -- but a large effect
should no longer be expected.

---

# Round 3: the generational (young-generation) collector

## Starting point, measured before any change

Corpus `asm-classes;classes`, release shape `-O3 -flto=thin`, 5 interleaved rounds:

| arm    | wall (min of 5) | peak footprint |
|--------|-----------------|----------------|
| parpar | 1.28s           | 1261 MB        |
| jdk25  | 1.09s           | 534 MB         |
| jdk8   | 1.65s           | 505 MB         |

1.17x slower than JDK 25 and **2.36x its memory**. Memory is the bigger gap AND
the reproducible one (peak footprint is stable to ~3%; wall clock on this box
varies with load and needs min-of-N), so memory is the target and wall clock is
reported only to show it did not regress.

## THERE WAS ALREADY A YOUNG GENERATION IN THE TREE

Before writing anything: `CN1_NURSERY` is a complete thread-local young
generation -- bump-allocated 64KB blocks in a 64MB arena, objects <= 512 bytes,
minor collection, block tenuring, an adaptive survival-based bypass -- carrying
a comment that it "is not defined anywhere in-tree today". It is compiled by no
gate, so it had rotted.

Its design avoids the hardest part of a generational collector: the write
barrier PROMOTES ON ESCAPE, so an old object can never reference a young one and
**no card table or remembered set is needed**. That is why it was worth
repairing rather than replacing.

## The split is favourable -- this is the number that justifies the work

Minor-collection survival on the self-hosting corpus, once roots were fixed:

    alloc=113715 promoted=20138 survival=17%
    alloc=111235 promoted=23160 survival=20%
    alloc=115809 promoted=27619 survival=23%

**~75-85% of small objects die without ever reaching the main heap.** This is
the opposite of the shape where generational collection is pure overhead, and it
is consistent with the earlier census (~680MB of 1.19GB allocated is garbage).

## Six defects found, each verified by a measurable change

1. **The nursery barrier REPLACED the SATB insertion half instead of composing
   with it.** `CN1_WRITE_BARRIER` is the only barrier the translator emits at an
   object store, so defining `CN1_NURSERY` silently removed insertion from the
   concurrent collector. cn1_globals.m's own deletion-filter argument names this
   build as its one exception -- an exception that existed only because nothing
   compiled it.

2. **The minor collection scanned only PRECISE roots.** That was complete when
   the nursery was written and has not been since: frameless object/instance
   codegen is default-on and exists precisely so a reference need NOT be pushed
   to `threadObjectStack`. Measured: SIGSEGV after ONE minor collection with
   frameless on; a clean Java-level result with it off. Fixed by scanning this
   thread's own C stack + registers conservatively, which needs no signal and no
   stop because it runs ON the mutator. Needs an object-start bitmap (1 bit per
   16-byte granule) so an INTERIOR pointer resolves to its object base; the
   backward scan is bounded by `CN1_NURSERY_MAX_OBJECT`, so it is O(32).

3. **`System.arraycopy` bypassed the nursery barrier**, exactly as it bypasses
   both SATB halves and for the same reason -- no per-element setter runs.
   `ArrayList.grow` copies through it.

4. **`cloneArray` had the identical hole.**

5. **`cn1InNursery()` is an ADDRESS-RANGE test, and promotion does not move the
   object.** A promoted container stays physically in the arena forever, so
   `!cn1InNursery(target)` read it as "still young" and skipped promoting every
   value later stored into it. Replaced with `cn1IsYoungObject()` = in-arena AND
   `__heapPosition == -1`. Promotion rose 14% -> 24% on the same workload.

6. **`FusedFieldInit` emitted a raw C field assignment with NO write barrier**
   -- an INDEPENDENT `allocArray` published into a field of an existing object.
   This is a REAL DEFECT ON MASTER, not just a nursery one: `CN1_WRITE_BARRIER`
   is the SATB insertion half, so an array allocated and installed during a
   concurrent mark is recorded nowhere. `FusedConstructor`'s own children are
   exempt and correctly get no barrier -- those are carved out of the OWNER's
   block by `cn1FusedInstallPrimArray` and have no independent GC identity.
   Five sites in the self-hosting corpus, `java.lang.String`'s `char[]` among
   them. **Cost: zero.** Default build after the fix is 1261 MB -- identical to
   baseline -- with byte-identical emitted C.

## Three diagnostics added, all `#ifdef`-gated QA-only

- **`CN1_NURSERY_POISON`** stamps every non-promoted object in a retiring block
  with a poisoned class pointer, converting a silent use-after-free into an
  immediate fault on the instruction that holds the stale reference. The poison
  ENCODES A CLASS ID and prints a legend, so the fault address decodes back to a
  class name -- a flat sentinel proves a stale reference exists but not what was
  missed, which is the whole diagnosis.
- **`CN1_NURSERY_VERIFY`** checks the generational invariant directly ("nothing
  outside the young generation may reference something inside it") by re-running
  every object's mark function in a reporting mode. It found 1552 violations of
  exactly one shape -- BiBOP owner -> young array -- which is what identified
  defect 6, and reports 0 after the fix.
- **`CN1_NURSERY_PROMOTE_ALL`** promotes every object in every retiring block.
  It answers one question: is a remaining failure a REACHABILITY gap or a defect
  in the promotion machinery?

### Two verifier traps, both of which first reported a confident clean

- The first version walked only `allObjectsInHeap`. **BiBOP objects are
  deliberately absent from that table** -- that is the point of the page heap --
  so every BiBOP holder read clean. It printed 0 violations against a heap that
  was demonstrably corrupt.
- The second still missed **promoted-but-not-yet-registered** objects, which
  reach `allObjectsInHeap` only at the next paused mark. Everything the pass had
  just promoted was in neither walk.
- Both the verifier and the conservative scan therefore SELF-COUNT
  (`holdersScanned=`, `stackScan words= found=`). "0 violations" is exactly what
  a verifier that never ran also prints.

## STATUS: NOT YET CORRECT, AND NOT MEASURED FOR PERFORMANCE

With all six fixes the nursery still faults on the self-hosting corpus, so no
performance number for it is reported -- a speed figure from a run that crashes
would be meaningless.

What is established about the remaining gap:

- It is a **reachability gap, not a promotion-machinery defect**:
  `CN1_NURSERY_PROMOTE_ALL` runs the whole corpus to completion (exit 0).
- It is **not root scanning**: the precise-root build (frameless off) and the
  conservative build now fail identically.
- It is **not an old->young reference at minor-collect time**: the verifier
  reports 0 over BiBOP, the legacy table and pending promotions.
- It is **not memory reuse**. An earlier draft of this section said it was, and
  that was wrong: the no-reclaim arm cannot reuse a byte (every reclaim path
  disabled, 4GB arena, 192MB allocated) and still fails. What separates the two
  no-reuse arms is REGISTRATION -- `PROMOTE_ALL` gives every object a
  `__heapPosition` of -2 and hands it to `cn1AddPending`; `NO_RECLAIM` leaves
  them at -1 forever.

## ROOT CAUSE: the main collector cannot see the young generation

`cn1ConservativeResolve` has no nursery handling, and nothing on the main mark
path traces a young object. So an unpromoted nursery object is INVISIBLE to the
concurrent collector even while it is live -- and every HEAP object reachable
only through it is therefore never marked, and the sweep frees it. The young
object is then holding a dangling pointer into reclaimed heap memory.

This is a design gap, not rot. The nursery's safety argument covers exactly one
direction: eager promotion on escape guarantees old-never-references-young. The
CONVERSE -- young references old, so live young objects must be roots for the
major collection -- has no mechanism at all.

It explains every observation: `PROMOTE_ALL` works because registration makes
the whole young generation visible to the major mark; `NO_RECLAIM` fails because
nothing is registered; the invariant verifier reports 0 because it checks
old->young, which is the direction that IS maintained; and the failure needs
both several minor collections and a major cycle to appear.

### The young-root pass is implemented, and it is NOT sufficient on its own

`cn1NurseryMarkYoungRoots` now runs every young object's mark function during the
major mark's root phase, inside the stopped-thread region, once per cycle (same
idiom as `cn1GcScanParkedVirtualThreads`). The object-start bit is also published
LAST and release-ordered, so "bit set" means "header complete" for that walker.

It runs -- self-counted, `pass=2 blocks=4 objects=1298` -- and the corpus still
faults, at the same site (`String.equals` reached from `findDeclaredMethod`)
after the same 8 minor collections. Note how FEW blocks it sees: with the nursery
absorbing most small objects, BiBOP volume drops and only TWO major cycles happen
before the failure, so this pass is not where the run is spending its risk.

So the analysis above is necessary but incomplete: there is at least one more
defect. The next thing to examine is per-object teardown -- an object dying in
the nursery never reaches `cn1BibopReclaimSlot`-equivalent cleanup, so monitor
data, finalizers and native peers keyed by ADDRESS survive it, and a recycled
address inherits them.

## Also found: an ablation arm that does not compile

`-DCN1_DISABLE_CONSERVATIVE_GC_ROOTS` fails to build: `gcPthreadValid` is
declared only under `CN1_CONSERVATIVE_GC_ROOTS`, while `CN1_RESUME_THREAD` uses
it unconditionally. Same class of rot as the nursery itself -- a documented
configuration that no gate compiles.

## Reproducing

`build-selfhost.sh` gained `CN1_SELFHOST_JAVA_OPTS`, which reaches the
TRANSLATOR that emits the C rather than the C compiler -- that is how a
codegen-level ablation is run:

```bash
CN1_SELFHOST_JAVA_OPTS="-Dcn1.frameless.objects=false -Dcn1.frameless.instance=false" \
CN1_SELFHOST_CFLAGS="-DCN1_NURSERY -DCN1_NURSERY_VERIFY" ./build-selfhost.sh -O3
```

---

# Round 4: where the memory actually is

## Baseline re-measured on a QUIET machine

The 1.17x figure in round 3 was a LOAD ARTIFACT. Re-run at load ~2 (serial marking,
the shipping default):

| arm    | wall (min of 5) | peak footprint |
|--------|-----------------|----------------|
| parpar | 0.92s           | 1241 MB        |
| jdk25  | 0.66s           | 522 MB         |

**1.39x time, 2.38x memory.** Any wall-clock ratio quoted from a loaded run on this
box is worthless -- jdk25 itself moved 1.09s -> 0.66s between the two runs.

## Three memory levers tried; two are dead ends

- **Parallel marking is neutral-to-worse here.** The source default is SERIAL
  (`gcMarkResolveThreadCount` returns a hardcoded 1 from an old isolation
  experiment). mt4: 0.91s / 1325MB against mt1's 0.92s / 1241MB -- same time, MORE
  memory. The round-2 finding that mark parallelism is decisive held only for a
  corpus ~7x larger.
- **The pacing run-ahead cap is NO LONGER BINDING.** Sweeping the multiplier the
  choke fix introduced (1x / 2x / 4x of last cycle's occupied bytes) moves peak
  by less than noise: 1283-1298MB, 1273-1294MB, 1304-1309MB. Whatever sets the
  peak, it is not admission control any more.
- **`malloc_zone_pressure_relief` after every sweep changes nothing.** 1211-1236MB
  with it, 1211-1237MB without. Same result as the earlier page-trim experiment
  and for the same reason: peak is a high-water mark that is re-dirtied at once.
  The call is kept (it is free when it has nothing to return) with
  `CN1_GC_NO_MALLOC_RELIEF` to turn it off.

## The attribution, from vmmap at peak

Sampling `vmmap --summary` in a tight loop against the live process and keeping the
largest sample (footprint 1.2G):

| region                | dirty  | what it is                       |
|-----------------------|--------|----------------------------------|
| MALLOC_LARGE          | 822.5M | the BiBOP arena, genuinely in use |
| MALLOC_SMALL          | 207.2M | legacy heap + VM buffers          |
| MALLOC_LARGE (empty)  | 160.9M | freed by malloc, never returned   |
| MALLOC metadata       |   4.1M |                                   |

BiBOP is NOT wasteful: 11287 pages x 64KB = 705.44MB reserved against 708.00MB of
arena taken from malloc, so the slab allocator loses nothing to alignment. The heap
report now prints `PROCESS footprint`, `MALLOC inUse/allocated/idle` and
`ARENA taken` on the same line as the Java total, because the gap between the Java
heap and the process is the whole question and nothing printed them together.

**~647MB of malloc's reported in-use bytes is attributed by none of these
instruments.** That is recorded as unexplained rather than guessed at.

## THE ONE THAT PAYS: 30% of the heap is dead objects being held

`CN1_ALLOC_CENSUS` + `CN1_HEAP_REPORT` at exit:

```
occupied 7,606,861 objects 705.10MB
  traced 5,249,747 (69%)  fresh 2,710 (0%)  aging 2,319,546 (30%)  dead 34,858 (0%)
```

"aging" is `mark == currentGcMarkValue - 1`: marked last cycle, NOT reached this
cycle, and held one more cycle by the sweep's grace rule alone. It is 30% of
occupied bytes, and it lands on exactly the churn classes:

| class                  | size    | aging |
|------------------------|---------|-------|
| java.lang.Object[]     | 161.7MB |  41%  |
| char[]                 | 134.2MB |  30%  |
| java.lang.String       |  73.6MB |  20%  |
| ArrayListIterator      |  41.6MB |  29%  |
| boolean[]              |  13.4MB |  82%  |
| asm Subroutine         |   9.4MB |  82%  |

That population -- short-lived arrays and iterators -- is precisely what a young
generation removes before it ever reaches the aging pipeline, which is the
independent argument for finishing the nursery.

### The rule now lives in ONE place

The liveness test was spelled out at FIVE sites (legacy table scan, BiBOP per-slot
walk, two reference-clearing passes, the weak-child check), each with a comment
insisting they must agree exactly -- because clearing a reference the sweep then
keeps merely wastes a cache entry, while failing to clear one the sweep frees hands
out a dangling pointer. They are now one predicate, `cn1GcSweepReclaims`, with the
window exposed as `CN1_GC_AGING_SLACK` (default 1 = historical behaviour).

### Measured: CN1_GC_AGING_SLACK=0

| slack | wall (3 reps)      | peak (3 reps)        | files |
|-------|--------------------|----------------------|-------|
| 1     | 1.04 / 0.94 / 0.96 | 1232 / 1212 / 1216MB | 796   |
| 0     | 0.92 / 0.96 / 0.98 | 1051 / 1184 / 1183MB | 796   |

**60-160MB (5-13%) off peak, no time cost, byte-identical output.** Less than the
~210MB the census suggests, because the aging population is re-created as fast as it
is reclaimed.

Gates under `CN1_GC_AGING_SLACK=0`: BibopPageFloor, GcOverflowSpiral,
GcUncooperativeThread and **GcHeapIntegrity** (the dangling-reference /
type-confusion gate, i.e. the one that would catch a premature free) all pass.

DEFAULT NOT CHANGED. The slack is what covers a mark that was incomplete rather
than a heap that was empty -- a page-index miss, a conservative-scan gap -- and on
ParparVM a dangling read is a native crash no Java catch can see. Passing a
minutes-long gate is not the same as a soak.

## Two pre-existing failures found and separated from this work

- **`BibopPageFloorIntegrationTest` and `GcOverflowSpiralIntegrationTest` did not
  build at all.** Both called the 3-arg `runTranslator`, whose default appType is
  `ios` -- which emits the C runtime as OBJECTIVE-C (`cn1_globals.m`,
  `nativeMethods.m`) while the CMake project they then build globs only `*.c`. The
  runtime was silently excluded and the link failed on whichever natives the app
  retained (`cn1Value` for five boxed types, `System.gcIdleWaitMillis`,
  `StandardInputStream.readImpl`). Sibling GC tests pass only because their apps
  cull all of those. FIXED by passing `"clean"`. Confirmed pre-existing by
  stashing every local change and reproducing the identical undefined-symbol set.
- **`GcSteadyStateIntegrationTest` fails on pristine sources too** (its scenario-3
  precondition: "the ceiling is not pressuring this workload", 641MB headroom on
  pristine against 524MB with these changes). Not caused by this work, and the
  pacing change moves it TOWARD binding, not away.

---

# CORRECTION: the fast numbers came from an uncommitted change that is not safe

Every wall-clock figure above that looks good (1.28s, 0.92s, "1.39x") was measured
with an UNCOMMITTED working-tree change -- the occupied-derived pacing ceiling. It
is not in HEAD. The committed branch uses `capCeiling = trigger * MAX_CAP_MULTIPLIER`
and is far slower. With that ceiling restored, on a quiet machine, 3 interleaved
rounds, byte-identical output across all arms:

| arm    | wall (min of 3) | peak footprint |
|--------|-----------------|----------------|
| parpar | 3.36s           | 1230 MB        |
| jdk25  | 0.66s           | 523 MB         |

**5.09x slower, 2.35x the memory.** That is the honest position.

## Why the lift is not a fix, and is now defaulted OFF

It is genuinely faster -- 0.96s against 4.18-6.05s -- but the speed comes from the
BOUND GOING AWAY, not from the collector improving. `bibopLastCycleOccupiedBytes`
grows without limit in a garbage-heavy workload, so a ceiling derived from it grows
with the garbage and the mutator is never throttled at all.
`GcOverflowSpiralIntegrationTest` measures the consequence directly:

```
With no process ceiling the workload peaked at 10863112KB against a live set of a
few hundred bytes ... a number this size means it is tracking the HOST's free RAM
again, and the app grows until the machine complains.
```

10.86GB. The test's own text names the invariant the lift violates.

THREE alternative signals were tried and all measured, so they are not re-tried:

| ceiling signal            | self-hosting wall | verdict                             |
|---------------------------|-------------------|-------------------------------------|
| none (trigger-derived)    | 4.18-6.05s        | correct, slow                       |
| 2x occupied, unbounded    | 0.96-1.01s        | fast, 10.86GB spiral -- UNSAFE      |
| 2x occupied, capped at    | 4.90-6.74s        | correct, and no faster than none    |
|   maxTrigger*multiplier   |                   |                                     |
| 8x bibopLastCycleLiveBytes| 3.96-6.05s        | that field is NOT the live set      |
| 2x (occupied - reclaimed) | 6.53-9.75s        | worse                               |

The capped-lift row is the important one: the run-ahead this workload needs to avoid
parking is LARGER than any trigger-derived bound, so admission control is simply the
wrong place to fix it. The choke is a symptom of cycles that are long because the
heap is large.

`CN1_GC_RUNAHEAD_MULT` keeps the knob (0 = off, the default) so the trade is
measurable, and the gates -- GcOverflowSpiral, BibopPageFloor, GcUncooperativeThread,
GcHeapIntegrity -- all pass with it off.

## Why the trigger cannot simply be allowed to grow

The obvious alternative is to let the adaptive trigger track the heap, since
`cap = trigger * MAX_CAP_MULTIPLIER` would then lift legitimately. It does not grow
here because its survival ratio uses `liveBytes`, which EXCLUDES grace-marked slots
(`policySurvivors = policyLiveCount - graceMarked`).

That exclusion is deliberate and correct: including grace-marked slots is what made a
pure-garbage workload look survivor-heavy. Undo it and the spiral's trigger grows, its
cap grows with it, and the runaway is back. The policy is self-consistent -- a
garbage-heavy workload is supposed to keep a low trigger, and the self-hosting
translation IS garbage-heavy (75-85% of small objects die young).

**Every route out of the choke leads to the same place: stop the garbage reaching the
old generation.** That is the young generation, and it is the one thing that makes the
survival ratio honest rather than gamed.

## The one memory win that IS safe, measured on the shipping default

`CN1_GC_AGING_SLACK=0`, lift off, same binary, 3 reps each:

| slack | peak                 | wall                |
|-------|----------------------|---------------------|
| 1     | 1199 / 1204 / 1166MB | 3.30 / 4.72 / 4.22s |
| 0     | 1085 / 1085 / 1048MB | 4.44 / 3.52 / 4.40s |

**~120MB, ~10% of peak, consistently, with byte-identical output** and time within
this box's noise. Takes memory from 2.35x jdk25 to ~2.10x. Default still 1; see the
note at `cn1GcSweepReclaims` for why removing a grace window is the user's call.

---

# Round 5: the fix. Parallel marking, measured in the RIGHT configuration

## The earlier "parallel marking is neutral" result was measured wrong

Round 4 concluded mt4 was neutral-to-worse. That run had the occupied-derived pacing
lift active, which meant THE CAP NEVER BOUND -- the mutator never parked, so mark
throughput could not affect wall clock by construction. It measured nothing.

With the pacing bound restored (the shipping configuration, where the mutator really
does wait on the collector), 3 reps each, byte-identical output at every arm:

| markers | wall                 | peak          |
|---------|----------------------|---------------|
| 1       | 5.24 / 5.31 / 6.85s  | 1200-1221MB   |
| 4       | 0.99 / 1.07 / 1.12s  | 1070-1173MB   |
| 8       | 0.99 / 1.04 / 1.38s  | 1083-1152MB   |

**~5x faster AND ~10% less memory.** They move together rather than trading off: a
shorter cycle gives the mutator less time to run ahead, so the heap is smaller too.
Flat past 4 markers, which is the same diminishing return earlier rounds saw.

The default was `int n = 1`, hardcoded behind `#elif 1` by an isolation experiment
from 2026-07-03 whose own note already said "Parallel marking was never re-tested
after the experiment". It is now CPU-derived (the existing path, capped at 4), with
`-DCN1_GC_SERIAL_MARK` to restore the serial arm.

## Headline, self-hosting corpus, 5 interleaved rounds

| | wall (min of 5) | peak |
|---|---:|---:|
| parpar | 1.00s | 1093 MB |
| jdk25  | 0.70s |  528 MB |
| jdk8   | 0.99s |  503 MB |

**1.43x JDK 25 on time, 2.07x on memory. Level with JDK 8 (1.01x).**

From where this round started -- 5.09x time, 2.35x memory -- with the pacing bound
INTACT rather than removed.

## ONE UNEXPLAINED RUN, recorded rather than dismissed

During the slack A/B, one run of the parallel build emitted **11 files instead of
796** (0.37s, 434MB) and was not caught because that loop did not check the exit code.
It has not reproduced in **32 subsequent runs** (12 plain + 20 under the identical
invocation, env var and `/usr/bin/time -l` wrapper).

It is on the record because parallel marking is exactly the mechanism the 2026-07-03
isolation experiment suspected of heap corruption, and "did not reproduce in 32 runs"
is not "did not happen". Every benchmark loop now checks the emitted file count.

## The nursery: FIXED, and it does not pay

The remaining defect was found, and it was not roots, barriers or the worklist:

**`gcMarkObject` rejected every nursery object before reaching the promotion branch.**
The conservative-resolve guard --

```c
if(!cn1GcTrustedRoots && cn1ConservativeResolve((void*)obj) != obj
   && !cn1GcImmortalObjContains(obj)) return;
```

-- drops anything the resolver cannot map back to itself, which is how a reference
into a freed slot is refused. A nursery object lives in neither a BiBOP page nor
allObjectsInHeap, so it NEVER resolves. With the nursery branch sitting after that
guard, every promotion routed through `gcMarkObject` was silently discarded:
`cn1PromoteDrain` walked each promoted object and called `gcMarkObject` on its fields,
and every one returned early. Moving the nursery decision to the very top of
`gcMarkObject` fixes it, and the corpus then translates to completion (exit 0, 796
files) for the first time.

The finder that produced this answer is the inverse of the invariant verifier: after
promotion, scan every region for words pointing at objects the collection just
declared dead. It reported `stack=0 bibop=0 legacy=0 nursery=15989`, first holder a
promoted `java.util.ArrayList` (heapPos=-2, mark function present) with the dangling
word at **+32 of 48 bytes** -- exactly `java_util_ArrayList_array`. That ruled out
roots, write barriers and the worklist in one measurement, after four rounds of each
being suspected in turn.

### And with it working, the generational premise does not hold here

True survival is **51-57%**, not the 14-23% measured earlier -- that low figure was an
ARTIFACT of the dropped promotions. Roughly half of all small objects survive, and
promotion moves them from BiBOP (page-based, cheap sweep) into the LEGACY heap
(table-based, per-object malloc/free). The nursery is a third tier whose survivors
land in the slowest one.

Measured against the 1.00s/1093MB default:

| arm                                   | wall        | peak        |
|---------------------------------------|-------------|-------------|
| nursery, 8MB trigger / 64MB arena     | 6.62s       | 1321MB      |
| nursery, 64MB trigger / 256MB arena   | 6.62-8.56s  | 1464-1600MB |
| nursery, static scan skipped          | 5.97-8.56s  | 1464-1474MB |

**A net loss on every axis.** BiBOP already IS the fast young-object path; what this
VM needed was not a fourth heap but a collector that keeps up with the one it has.
`CN1_NURSERY` stays off, now correct rather than broken, with the finding recorded so
it is not re-attempted on the strength of the old survival number.

## Final numbers, and a metric that got noisier

Three independent 5-round bench runs of the shipping configuration:

| run | parpar wall (min) | jdk25 wall | parpar peak (MAX) | per-round peaks        |
|-----|-------------------|------------|-------------------|------------------------|
| 1   | 1.00s             | 0.70s      | 1093 MB           | --                     |
| 2   | 0.98s             | 0.65s      | 1230 MB           | --                     |
| 3   | 0.98s             | 0.65s      | 1225 MB           | 1070/1056/1225/1143/1068 |

**Wall clock: 1.51x JDK 25, 1.08x JDK 8** -- level with the JDK the builders actually
fork. Wall clock is now tight (0.98-1.00s across runs), which it never was before.

**Memory: 2.32x by the MAX definition, ~2.05x by the median round.** Report the MAX,
since a peak is a max -- but note the spread. Peak footprint used to be stable to ~3%
and is now 1056-1225MB run to run. That is a REAL change from parallel marking: cycle
completion now lands at different points relative to the allocation stream, so how much
float is outstanding at the high-water mark varies. Memory conclusions from single runs
are no longer safe; take several rounds.

## Where the remaining memory is, and what it is NOT

Census at exit under parallel marking:

```
occupied 5,714,416 objects 668.75MB
  traced 1,773,219 (31%)  fresh 1,171 (0%)  aging 2,263,476 (40%)  dead 1,676,550 (29%)
  bibop 481.58MB  legacy 187.17MB
```

Only **31% of occupied bytes are reachable**; 69% is float the sweep has not yet
returned. Levers tried against it, all measured, none of which pay:

| lever                          | result                                    |
|--------------------------------|-------------------------------------------|
| `CN1_GC_AGING_SLACK=0`         | 1053-1133MB vs 1032-1054MB -- gone. Shorter cycles already shrank the aging set, so the knob that was worth 10% under serial marking is worth nothing now |
| GC trigger 48 / 24 / 12 MB     | 984-1235 / 1055-1058 / 1119-1159MB -- no trend; 12MB is slower AND larger |
| `malloc_zone_pressure_relief`  | nothing (round 4)                         |
| `CN1_ADOPT_POLICY=0`           | 994-1019MB, ~4% -- but adoption is a CORRECTNESS mechanism (split reachability), so not a knob to turn |

Note the aging-slack result specifically: it was a validated 10% win under serial
marking and is worthless under parallel. A tuning result is only true for the
configuration it was measured in.

## Validation of the shipping configuration

- Full `vm/tests` suite: **584 tests, 1 failure** -- `GcSteadyStateIntegrationTest`,
  which fails identically on pristine sources (verified by stashing every local change).
- GC gates: GcOverflowSpiral, BibopPageFloor, GcUncooperativeThread, GcHeapIntegrity,
  GcMarkCompleteness -- all pass.
- 32 consecutive self-hosting translations, output byte-identical to the JVM
  translator's, with the emitted file count checked every run.
- Pacing: **1 park event** for the whole run. The collector keeps up, which is what
  makes the remaining 1.51x an AOT-vs-JIT throughput gap rather than a GC stall.

---

# Round 6: the mark worklist. 25% of peak memory was a rescan doing nothing

`-DCN1_GC_CONFORM` on the parallel build, one run of the self-hosting corpus
(wallMs=1136, 5 GC cycles):

```
rescanPasses=68  rescanUseful=0  rescanSlots=10,107,804  rescanPushes=3,384,826
extSearches=259367  extHits=0  bloomRejects=2,263,038
graceLegMs=139.2  sortTotalMs=95.1  sortedTotal=2,662,940
cause=pacingVolume count=1 totalMs=117
```

**Sixty-eight rescan passes walked ten million slots and found ZERO objects the drain
had missed.** The rescan is the fallback for a mark worklist that cannot hold the
frontier; at 65536 entries it overflowed constantly on a heap with millions of live
objects, and every fallback was wasted work.

| worklist | wall (3 reps)         | peak          |
|----------|-----------------------|---------------|
| 65536    | 0.97 / 1.01 / 1.24s   | 1050-1265MB   |
| 1048576  | 0.90 / 0.90 / 0.90s   |  791-839MB    |
| 4194304  | 0.88 / 0.90 / 0.90s   |  777-831MB    |

**~25% off peak memory, a faster and far tighter wall clock.** 4M is no better than 1M
and costs 32MB of table against 8MB, so the default is 1M.

Time and memory moved together again, which is now the third time: work the collector
does not waste is cycle time the mutator does not spend running ahead.

## THIS CONTRADICTS ROUNDS 1-2, WHICH WERE RIGHT AT THE TIME

Earlier rounds concluded "a bigger worklist is neutral-to-worse everywhere it has been
tried" and even that overflow behaves as a MITIGATION. Those were measured under SERIAL
marking. Three tuning results have now flipped when the configuration changed:

| knob              | serial marking        | parallel marking     |
|-------------------|-----------------------|----------------------|
| marker count      | 1 (pinned, "isolation")| 4 -- ~5x faster      |
| mark worklist     | bigger = worse        | bigger = 25% less mem|
| CN1_GC_AGING_SLACK| 0 = 10% less memory   | no effect at all     |

**A GC tuning result is only true for the configuration it was measured in.** Every
number in this file should be read with the arm it was taken on.

## Headline

| | wall (min of 5) | peak (MAX of 5) |
|---|---:|---:|
| parpar | 0.91s | 860 MB |
| jdk25  | 0.66s | 532 MB |
| jdk8   | 0.93s | 518 MB |

**1.38x JDK 25 on time, 1.62x on memory -- and FASTER than JDK 8 (0.98x).**
Per-round peaks 810/823/842/853/860MB, so the run-to-run noise parallel marking
introduced in round 5 is gone too.

Session start was 5.09x time / 2.35x memory, with the pacing bound intact throughout.

## Test maintenance this forced

`GcOverflowSpiralIntegrationTest` began failing -- correctly, on its own precondition:
"the grace pass never drained mid-walk, so this workload never pushed enough to
approach the worklist limit and the assertion above proves nothing." A 16x larger
default put the overflow path out of reach of its fixed workload.

Fixed by pinning `-DCN1_GC_MARK_WORKLIST_SIZE=65536` in that test's own cmake flags.
What it tests is the overflow MECHANISM, which must keep working whatever the default
is; pinning also means a future change to the default cannot silently turn the gate
into a no-op.

---

# FINAL: 1.35x JDK 25 on time, FASTER than JDK 8; 1.59x on memory

Self-hosting corpus, 5 interleaved rounds, quiet machine, byte-identical output:

| | wall (min of 5) | peak (MAX of 5)   |
|---|---:|---:|
| parpar | 0.88s | 835 MB (789-835) |
| jdk25  | 0.65s | 525 MB           |
| jdk8   | 0.91s | 518 MB           |

**vs JDK 25: 1.35x time, 1.59x memory. vs JDK 8: 0.97x time (faster), 1.61x memory.**

Session start: 5.09x time, 2.35x memory. The pacing bound is intact throughout -- none
of this came from removing admission control.

## Three changes, all measured, all validated

1. **Marker threads 1 -> CPU-derived (capped 4).** ~5x wall. The `int n = 1` was pinned
   by a 2026-07-03 isolation experiment that was never re-tested.
2. **Mark worklist 65536 -> 1048576 entries.** ~25% peak memory. The 64K worklist
   overflowed constantly, and the rescan fallback did 10.1M slot walks for zero useful
   work across 68 passes.
3. **`FusedFieldInit` emits `CN1_WRITE_BARRIER`.** A live SATB insertion hole,
   independent of any of the above. Zero cost.

Plus: the pacing lift is defaulted OFF (it was disabling admission control -- 10.86GB
spiral), the sweep's liveness rule is one predicate instead of five copies, and
`CN1_NURSERY` is correct rather than broken though it does not pay.

## GC is no longer the bottleneck

`-DCN1_GC_CONFORM` on the final build:

```
wallMs=938  cyclesOnDemand=8
rescanPasses=0  rescanUseful=0  rescanSlots=0
cause=nativeResume count=198 totalMs=0
cause=signalStop  count=12  totalMs=0
(no pacingVolume stall at all)
```

**The mutator's total GC stall is ~0ms**, against 68 rescan passes and a 117ms pacing
park before. The remaining 1.35x is AOT-vs-JIT execution throughput, which is a codegen
problem, not a collector one.

Census at exit went from `traced 31% / aging 40% / dead 29%` to
**`traced 70% / aging 11% / dead 19%`** -- the collector now keeps up, and the Java heap
(576MB resident) is comparable to HotSpot's ENTIRE process (525MB).

## What remains, attributed by vmmap at an 823.5MB peak

| region               | dirty   | note                                |
|----------------------|---------|-------------------------------------|
| MALLOC_LARGE         | 488.5MB | BiBOP arena -- accounted (460MB)    |
| MALLOC_SMALL         | 227.5MB | legacy heap 119.7MB + VM buffers    |
| MALLOC_LARGE (empty) |  92.9MB | freed by malloc, never returned     |
| __DATA / metadata    |  12.0MB |                                     |

`malloc_zone_pressure_relief` was re-tested against the 92.9MB in THIS configuration
(three results having already flipped on configuration) and still does nothing:
798-820MB with it, 800-813MB without. Peak is a high-water mark that is re-dirtied.

## Validation

- Full `vm/tests`: **584 tests, 1 failure** -- `GcSteadyStateIntegrationTest`, which
  fails identically on pristine sources.
- GC gates: GcOverflowSpiral, BibopPageFloor, GcUncooperativeThread, GcHeapIntegrity,
  GcMarkCompleteness -- all pass.
- Output byte-identical to the JVM translator across every benchmark arm and every
  A/B run; the emitted file count is checked on every run.

---

# Round 7: pushed, and what review found

## The performance is now gated, not just recorded

`vm/selfhost/perf-guard.sh` fails when the JDK 25 ratio exceeds 2.00x time or 2.10x
memory. Both defaults it protects were set by a measurement that a later configuration
change invalidated, and -- this is the point -- NEITHER REGRESSION WOULD FAIL A SINGLE
FUNCTIONAL TEST IN THIS TREE. Losing parallel marking costs ~5x wall clock and losing
the 1M worklist costs ~25% of peak, and both look like ordinary code.

The ceilings carry headroom over measured (1.25-1.35x time, 1.59-1.85x memory) while
sitting well under the 5.09x / 2.35x they exist to catch. The bench refuses to print a
ratio unless every arm emitted identical C, so a green guard is also a correctness
result.

## Re-verified across the master merge

A merge that touches GC code can cost the gains silently, so the ratios were re-measured
either side of it: time 1.35x -> 1.33-1.34x, memory medians ~2% apart. Held.

Note how load distorts ABSOLUTE numbers -- jdk25's own peak read 525MB on a quiet box
and 709MB on a loaded one, for the same work. Only ratios measured in one sitting mean
anything here.

## Two review findings, both real, both mine to have caused

**The Windows marker pool was uncapped.** The POSIX branch capped a CPU-derived count at
4 and the `_WIN32` branch did not. Harmless while the hardcoded serial default made both
branches unreachable -- defaulting to CPU-derived marking is what turned it into an
exposure. gcMarkPoolEnsure creates a persistent helper per marker, each reserving 16MB of
stack, so a 64-logical-CPU Windows host would reserve ~1GB for helpers the measurements
say do nothing past 4.

**The staleness guard could not see a DELETION.** It tested existence plus `find -newer`;
a removed or renamed file makes no remaining file newer, so maven never re-ran, `mvn
clean` never ran, and the deleted file survived in target/classes. The JVM translator
then keeps embedding a runtime resource that is gone from the tree -- and because BOTH
sides of the self-host comparison consume that same stale copy, GATE A STILL PASSES. A
gate that cannot fail on a deleted file is not covering deletions. Fixed with a manifest
diff, which the JavaAPI block twenty lines below already used for this exact reason.

### The negative control caught a bug in the FIX

The first version staged the manifest inside `target/`, which `mvn clean` deletes -- so
it was gone before it could be compared, the guard rebuilt on every run, and then failed
on the missing file. Only the "must stay SILENT when nothing changed" half of the control
exposed that; "fires on a deletion" passed happily. Now verified three ways: fires on a
deleted resource, fires when it is restored, silent when nothing changed.

## One failure correctly NOT chased

`BackendJavaSeRuntimeTest.javaSeSelfTest` fails locally and is master's new backend
module meeting a partially-installed local Maven repository. The whole branch touches
nothing under `backend/`, `demo/` or `maven/` -- verified by listing every changed file
against master -- and CI's build-test (17) and (21) pass. The test SKIPS on a fresh
worktree and runs on a populated one, so a worktree A/B is confounded by build state
rather than by code; scope is the honest discriminator here, not the worktree.

---

# Round 8: two corrections to this branch's own work

## The 1M worklist was a 17MB reservation in every application

The array is STATIC, so its size ships with every generated binary -- iOS and Android
included, not just a desktop translation. 1M entries is 17MB of `__bss` against 65536's
1.26MB, and the knee had never been measured between those two points. Re-measured:

| worklist | wall          | peak         | __bss   |
|----------|---------------|--------------|---------|
| 65536    | 1.55 / 1.86s  | 1063-1189MB  |  1.26MB |
| 262144   | 1.24 / 1.28s  |  760-786MB   |  4.41MB |
| 1048576  | 1.31 / 1.44s  |  812-858MB   | 16.99MB |

**262144 is the knee**: as good as 1M on both axes for a quarter of the table. Default
corrected from 1048576 to 262144.

The reservation is ZEROFILL, which is why a table this size is affordable at all -- all
three binaries are byte-identical on disk at 4,529,096 bytes, and pages commit only when
touched. That is a reason the cost is bounded, not a reason to be careless: the pages a
large heap DOES touch are real, and they are what the peak-memory column above measures.

A second trap this created: the file carries TWO `#ifndef CN1_GC_MARK_WORKLIST_SIZE`
blocks by design (the hoisted one the grace pass needs, and a fallback that keeps a -D
override authoritative in both places). Both were 65536, so they could not disagree.
Changing only one left them at 262144 and 65536 -- whichever the preprocessor reaches
first silently wins, so reordering or deleting the hoisted block would drop the default
back with no warning. Both literals are now synced, and say so.

## The young-root scan raced every thread but the one it had paused

`cn1NurseryMarkYoungRoots` walked EVERY thread's young blocks from a loop that pauses
threads ONE AT A TIME and releases each before the next is scanned. The other mutators
keep running: bump-allocating, running their own minor collections, clearing start-bit
ranges and recycling blocks while the walk reads them. Missed root, or a dereference of
a recycled header.

The comment was the worse half:

    // Inside the stopped-thread region on purpose, so a mutator cannot be
    // bump-allocating into a block while it is walked.

False, and it answers exactly the question a reader would have asked -- so it would have
stopped the next person checking. Now scans only the thread that iteration paused, over
that thread's own `nurseryYoungBlocks`. A nursery is thread-local and only its owner
mutates it, so this is safe AND complete: every thread is paused in some iteration.

## A CAUSAL CLAIM THAT DID NOT HOLD, and how it was caught

The Windows commit said the screenshot job "went from green to red on the commit that
made marking CPU-derived". Checking the actual range between the green head (1c784ef8)
and the failing one (9831ac4e), TWO things changed: the Windows marker branch became
CPU-derived (a branch that had never executed at all), AND the mark worklist grew to
17MB of static zerofill. Either could be responsible on a constrained runner.

The comment has been corrected to separate what is established -- the Win32 pthread shim
has never run a marker pool, which alone justifies keeping Windows serial -- from what is
not, which is WHICH of the two changes broke it.

### And the isolation run was destroyed by pushing over it

5e8c6430 was deliberately a single-variable change so the next Windows run would answer
that question. It was then pushed over before its Windows jobs started, and
`cancel-in-progress` cancelled them:

    5e8c6430 -> cancelled   (clean-target arm64 ran; both screenshot jobs cancelled)
    9831ac4e -> failure
    1c784ef8 -> success

So the experiment cost a full CI cycle and produced nothing. On a PR whose CI runs for
~40 minutes, an in-flight run IS the experiment; pushing during one is discarding it.

---

# Round 9: deleting what nothing builds, and a gate so it cannot happen again

Round 8 ended with a correction to this branch's own work. This round is the same
exercise widened: the nursery was not the only configuration nobody compiled, and the
interesting result is how the obvious test turns out to be the wrong one.

## The nursery is gone

The case is closed and was already in this file: `CN1_NURSERY` had no `#define` anywhere,
no workflow/script/pom/test passed `-DCN1_NURSERY`, and `git log -S"CN1_NURSERY" --
.github/` has never had a commit. When it was finally repaired and measured (Round 5) it
lost on both axes -- 6.62s against 1.00s, 1321MB against 1093MB -- and the 14-23%
survival that motivated it was an artefact of dropped promotions, with the true figure
51-57%. Half of all small objects survive, and promotion moves them from BiBOP (page
based, O(1) all-dead-page reclaim) into the legacy heap (table based, per-object
malloc/free). BiBOP already IS the fast young-object path.

1,244 lines of C. **The only codegen change is three lines of dead preprocessor text**,
and that is measured rather than argued: `verify-output-neutral.sh vs-master` over a
corpus app reports exactly one generated file differing, and the whole diff is the
`#ifdef CN1_NURSERY` block `ByteCodeClass` emitted into every `main()`.

## "IS IT BUILT BY A GATE" IS THE WRONG TEST ON ITS OWN

The rule that deletes the nursery is "an arm nothing builds is dead code". Applied
literally to the whole runtime it would have deleted **37 healthy arms**, including
`CN1_ALLOC_CENSUS` -- the instrument every heap conclusion in this file rests on -- and
every A/B arm the earlier rounds were measured with. Ungated is not the same as dead.

The sharp question is whether the arm still **compiles**. Sweeping all 41:

| result | count |
|---|---|
| builds and runs | 37 |
| rotted | 4 |

| rotted arm | failure |
|---|---|
| `CN1_NURSERY` | 3 implicit declarations; deleted |
| `CN1_DISABLE_CONSERVATIVE_GC_ROOTS` | 4 errors -- `gcPthreadValid` declared inside the conservative-roots `#ifdef`, read unconditionally by `CN1_RESUME_THREAD` |
| `CN1_DISABLE_BIBOP` | undefined `_cn1GcCycleState` at LINK time |
| `DEBUG_GC_OBJECTS_IN_HEAP` | 9 errors, then a bus error; deleted as superseded |

Two of those are worse than they look. **`CN1_DISABLE_CONSERVATIVE_GC_ROOTS` is the only
arm in the tree that can falsify a GC rooting claim**, so while it was broken an entire
class of evidence was unavailable. And **`CN1_DISABLE_BIBOP` is what
`run-bibop-adaptive.sh` builds as its comparison baseline**, so that script could not have
run either -- it is pre-existing on master, confirmed by checking the enclosing
conditionals at HEAD before blaming this branch.

`cn1GcCycleState` deserves its own line. It sat inside the BiBOP guard under a comment
reading *"Defined UNCONDITIONALLY ... so a build without CN1_ALLOC_CENSUS must still
link"* -- false for the one configuration that tests it -- ten lines below `bibopGcEpoch`,
which had been hoisted out of that same guard for that same reason, with a comment saying
so. The same bug twice, and the second copy was wearing a comment asserting it could not
happen.

## The gate, and its derivation

`vm/benchmarks/check-ablation-arms.sh`, a job on every PR touching `vm/**`. It compiles
and links every arm; what an arm DOES is its own gate's business.

**The arm list is derived from the source.** Anything the preprocessor tests that the tree
never self-defaults is a pure `-D` opt-in. A hand-written list would rot exactly the way
the arms did -- someone adds an arm, forgets the list, and it is uncovered from birth. Two
corrections were needed to get the derivation right, both worth knowing:

- A `#define` only counts as a DEFAULT when it is the body of its own `#ifndef`.
  `CN1_NO_WEAK_REFS` is defined only inside `#if defined(CN1_DISABLE_SATB) && ...`, so
  treating any define as a default silently dropped a real arm out of the sweep.
- The `#ifndef`/`#define` pair is routinely separated by several lines of rationale, so
  the adjacency test has to survive comments. Without that, four value tunables vanished.

**Proven non-vacuous** by re-injecting the `CN1_DISABLE_BIBOP` defect exactly -- putting
`cn1GcCycleState` back inside the guard. The gate fails on that arm alone, passes the
others, and goes green when it is restored.

## A claim of mine that the repaired arm immediately falsified

`ConcatCorrupt` was added as a torture for the fused concat natives, which take raw
interior pointers into their sources' `byte[]` and then allocate. Its first javadoc said
`-DCN1_DISABLE_CONSERVATIVE_GC_ROOTS` would make it bite. **It does not**: `corrupt=0` in
that arm too, because with precise roots the source Strings are still in the caller's
operand-stack slots. The hazard exists in NO configuration this VM builds -- which is
consistent with the fused-concat GC bracket that was added earlier on this branch and then
withdrawn as "the premise was wrong", and is now the measurement behind that withdrawal
rather than the argument.

So the driver stays, and its javadoc says its non-vacuity is UNPROVEN. It earns its place
as a heap-integrity torture -- 256 `int[]` held live and fully re-verified across 400,000
concatenations -- not as evidence about rooting. A self-test that cannot fail is worse than
none; the fix is to state the scope, not to claim more.

## The performance step could not have passed a single run

The ratchet added at the end of Round 8 had three independent defects, none of them a
performance regression:

- **No JDK 25 in that job.** `parparvm-selfhost.yml` sets up JDK 8 only, so both reference
  arms resolved to it, the labeller de-duplicated them to `jdk8` and `jdk8#2`, and the
  `vs jdk25:` line the guard greps for was never printed. The step then exits 1 on "no
  ratio line" -- which `perf-guard.sh` reports as a CORRECTNESS failure, so the first run
  would have read as the arms emitting different C.
- **The peak-memory probe was Darwin-only.** `/usr/bin/time -l` and "peak memory
  footprint" are BSD/Darwin; GNU time has neither, so on the runner every arm produced an
  empty string and the ratio line divided by nothing. It is per-platform now -- phys_footprint
  on Darwin, `VmHWM` elsewhere -- and every line NAMES the metric, because the two are not
  the same quantity and a Darwin figure must never be compared to a Linux one.
- **It measured the -O1 diff-gate binary.** The gates are built -O1 on purpose; a ratio
  from that binary is not the ratio anything ships. The perf arm is its own -O3 build now.

And the ceilings are back to unreachable. `CN1_PERF_MAX_MEM=99` next to a 3.00x time
ceiling was a disabled gate beside a number guessed from Mac measurements for a Linux
runner nobody has data from. Both are 99, the step is called "Performance report", and
setting them from the runner's own spread is the follow-up. **A threshold chosen without
data from the machine that will enforce it is not a ratchet, it is a future flake.**

## The baseline, re-taken on the current tree -- and the headline needs a qualifier

Nothing in this file had measured the shipping default since the worklist moved in
Round 8. Re-taken, quiet machine (indexers idle, load decaying from 9 to 2.6 -- the first
attempt was abandoned at load 8.7 with `mediaanalysisd` pinning 228%), 5 interleaved
rounds, 834-file corpus, output byte-identical across all arms:

| | wall (min of 5) | peak (MAX of 5) | peak (MEDIAN round) |
|---|---:|---:|---:|
| parpar | 1.40s | 1496 MB | 1124 MB |
| jdk25 | 0.90s | 710 MB | 705 MB |
| jdk8 | 1.14s | 551 MB | 543 MB |

**vs JDK 25: 1.56x time. Memory 2.11x by MAX and 1.59x by MEDIAN.** Read that pair before
reading anything else, because the two numbers disagree and only one of them has moved:

    parpar peaks   1026 1089 1124 1342 1496 MB     spread 46%
    jdk25  peaks    693  697  705  709  710 MB     spread  2%

**The central tendency has not regressed at all** -- 1.59x by the median is exactly the
FINAL headline. What is different is the VARIANCE, and it is entirely on our side. Round 5
first saw this (1056-1225MB) and attributed it to parallel marking landing cycle
completion at different points in the allocation stream; Round 6 reported it "gone"
(810-860MB). On this corpus it is back and larger.

That has a direct consequence for the CI gate: **a MAX-of-N metric over a distribution
with a 46% spread is not a ratchet, it is a coin.** Either the gate reports a median, or
its ceiling has to clear the whole spread -- which is most of the way to not being a gate.
This is the concrete reason the ceilings are parked at 99 rather than tightened.

Note also jdk25 moved 0.65s -> 0.90s and 525MB -> 705MB for the same work. That is the
corpus growing (796 -> 834 files as master moved) plus a slower session, not a JVM
regression -- which is exactly why only ratios are quoted.

## ROUND 8'S WORKLIST KNEE DOES NOT SURVIVE n=10

Round 8 moved the default from 1048576 to 262144 on **two samples per arm** (262144:
760-786MB; 1048576: 812-858MB) and called 262144 the knee. Against a 46% run-to-run
spread, two samples cannot separate those. Re-run properly -- both arms alternating,
two passes of 5 rounds each, n=10:

| worklist | median peak | max peak | min peak | spread | time (min) | time (median) | `__bss` |
|---|---:|---:|---:|---:|---:|---:|---:|
| 262144 (current default) | 1167 MB | 1438 MB | 977 MB | **47%** | 1.32s | 1.40s | 4.20 MB |
| 1048576 | **1091 MB** | **1280 MB** | 987 MB | **30%** | 1.36s | 1.40s | 16.20 MB |

**1M is better on memory on every summary that is not the minimum** -- median -6.5%, max
-11% -- and it has **half the variance**, at an identical median wall clock. The direction
Round 8 chose is the wrong one on this evidence.

**The default is NOT being flipped back, and that is the point.** Round 8's countervailing
argument is untouched by this and does not need a timing measurement: the array is
STATIC, so its size is a reservation every generated application carries -- 16.2MB of
`__bss` against 4.2MB, iOS and Android included. Choosing either constant is a bet about
which target matters, and that bet has now been made twice from underpowered data and
been wrong at least once. A third guess is not an improvement.

### The actual fix is to stop choosing

Size the worklist **at runtime from the machine**, allocate it once, and delete the static
array. An application whose mark frontier never exceeds a few thousand entries would carry
a pointer instead of a multi-megabyte reservation; a heap big enough to overflow would get
a worklist big enough to hold its frontier. No compile-time constant, nothing to guess,
and the knob that has now been set wrong twice stops existing.

Two constraints any implementation has to respect, both already documented in
`vm/CLAUDE.md` and neither optional:

- **Nothing may allocate while a thread freeze is held.** The root scans run with a
  mutator stopped mid-`malloc`, possibly holding the libc allocator lock, which is why the
  worklist is fixed-size today. So the allocation must happen at GC INIT, before any
  freeze, and a grow-on-overflow design must be barred from the frozen window rather than
  merely unlikely to enter it.
- **The serial drain reads the array without the worklist mutex**, by design, while
  parallel workers `memcpy` batches under it. A capacity that can change has to be
  published somewhere both agree on, or the growth has to be confined to the serial phase.

The sizing input is the open question, and it is not a detail: `cn1_available_memory` is a
flat 100MB placeholder on Linux, Windows and the non-Apple fallback, so a naive
"RAM/N entries" rule would hand every non-Apple target the FLOOR -- which this table says
is the worst arm. Deriving real physical memory there (`_SC_PHYS_PAGES` on Linux,
`GlobalMemoryStatusEx` on Windows) is a prerequisite, not a follow-up.

---

# Round 10: the census redirects the optimization work

Round 9 left a corrected baseline (1.56x time, 1.59x memory by median) and a plan whose
memory half was ranked by analogy. This round replaced that ranking with a measurement,
and the measurement moved almost everything.

## Where the heap actually is, at exit, 499.49MB occupied

```
[LIVE:exit] occupied 3844008 objects 499.49MB
            traced 1800985 (47%)  fresh 0%  aging 1333162 (35%)  dead 708439 (18%)
            bibop 327.77MB  legacy 171.72MB
[JHEAP:exit] pages=6782 reserved=423.88MB live=327.77MB slack=96.11MB
             legacy objects=488784 bytes=171.72MB | PROCESS footprint=842.38MB
             MALLOC inUse=645.50MB allocated=710.23MB idle=64.73MB
```

| class | MB | objects | B/obj |
|---|---:|---:|---:|
| char[] | 164.04 | 651,737 | 263 |
| java.lang.Object[] | 105.53 | 642,910 | 172 |
| java.lang.String | 42.28 | 476,028 | 93 |
| java.lang.StringBuilder | 29.84 | 195,552 | 160 |
| byte[] | 23.63 | 27,882 | 888 |
| int[] | 22.34 | 190,113 | 123 |
| java.util.HashMap | 16.00 | 174,786 | 96 |
| ArrayList.ArrayListIterator | 15.16 | 331,205 | 48 |

Four classes are 68% of the live heap. Two of the plan's items die here:

- **32-bit references are not the big swing.** `Object[]` is the only substantially
  reference-dense population and is 105MB gross; halving its payload is ~40MB of an
  842MB footprint, for a change that rewrites every generated object struct and breaks
  every port native that reads a reference field. The census was written to answer
  exactly this question before the work started, and the answer is no.
- **BiBOP slack (96MB, 23% of the arena) is downstream, not a lever.** It is free slots
  inside pages retained because they still hold at least one occupied slot. With 53% of
  occupied slots holding unreachable objects, the slack is a symptom of the float.

## THE AGING SLACK IS WORTH NOTHING, AND THE CENSUS BUCKET PREDICTED OTHERWISE

35% of occupied bytes (about 175MB) sit in the "aging" bucket -- marked last cycle,
unreachable this cycle, held one more cycle by `CN1_GC_AGING_SLACK`. That looks like the
single largest memory item in the collector, and its own comment says so.

Measured, `CN1_GC_AGING_SLACK` 1 vs 0, alternating arms, four passes of five rounds:

| slack | n | median peak | mean peak | range |
|---|---:|---:|---:|---|
| 1 (default) | 15 | 1153 MB | 1218 MB | 1053-1503 |
| 0 | 20 | 1135 MB | 1166 MB | 957-1460 |

**Two-sided permutation test on the means: p = 0.34.** Not a result. This CONFIRMS
Round 5, which found the same knob worthless under parallel marking, and refutes the
hypothesis this round started from.

**The lesson generalises and is the most useful thing in this round: a census bucket's
SIZE does not predict the PEAK.** Freeing 175MB one cycle earlier does not lower the high
water mark, because the peak is set by how far the mutator runs ahead between cycles, not
by how long the collector holds what it has already decided is dead. That is the same
mechanism parallel marking exploited -- shorter cycles cut time and memory together --
and it reframes the memory problem: **the levers are cycle length and allocation VOLUME,
not retention.**

It also explains the 46% run-to-run spread directly. The peak depends on where in the
allocation stream a cycle happens to finish, which is timing.

## So: allocation volume. The array header is 20% of it

Per cycle, 551,580 of 1,401,234 allocated objects are arrays. At a 40-byte header that is
21.0MB of the 105.3MB allocated per cycle before any payload.

`dimensions` (max 4) and `primitiveSize` (max 8) were both `int`, and the struct already
wasted 4 bytes of tail padding before `data`. Narrowed to a byte each, `data` moves into
that padding and the header goes **40 -> 32**.

Measured on allocation volume, which unlike peak is stable to 0.25-0.50% across reps --
three reps per arm, alternating:

| | before | after |
|---|---:|---:|
| bytes allocated | 804.5 MB | **777.0 MB** |
| bytes per object | 100.88 | **97.24** |

**-27.5MB per run, -3.42% of allocation volume, -3.61% per object.** Object count rose
0.19% between arms, so bytes-per-object is the honest figure; it matches the prediction
(a fifth off a fifth of all bytes is ~4%).

Unlike every GC tuning result in this file, this one is not specific to a corpus or a
configuration: it is 8 bytes off every array in every generated application.

**Peak footprint is deliberately NOT quoted for it.** Single-sample peaks across the two
arms read 857MB and 956MB -- in the wrong direction -- which is exactly what a 46% spread
does to a one-shot comparison. The effect is real and below this harness's noise floor on
that metric; allocation volume is the metric that can carry the claim.

### The next item the same census names

`ArrayListIterator`: 100,566 objects allocated per cycle at 48 bytes = 4.6MB/cycle, 4.4%
of allocation volume, every one of them a for-each loop's iterator that a lowered indexed
loop would not allocate at all -- on top of the two interface dispatches per element it
removes, each a five-load chase. `ForEachScan` in `experiments/concat/` exists to size
which receivers are provably indexable before that is written.

## Sizing the next two items, and refuting one of them

The three scanners in `experiments/concat/` were run over the translator + ASM corpus
(the same one every figure above uses). Both planned follow-ups changed shape.

### For-each lowering: REFUTED as planned, and the win was already taken

`ForEachScan`: **434 for-each sites**, by the static type of the `iterator()` receiver:

    353  java/util/List          61  java/util/Set           8  java/util/ArrayList
      5  java/util/Collection     4  java/util/TreeSet       3  (Deque, HashSet, DirectoryStream)

81% are `List`-typed, which reads like an overwhelming case for lowering them to an
indexed loop. It is not, for a reason this runtime settles on its own: **`LinkedList`
exists in `vm/JavaAPI`** (`extends AbstractSequentialList implements List`), so an
indexed loop over a `List` is O(n^2) whenever one shows up. Only the 8 `ArrayList`-typed
sites are provably indexable from the static type alone.

The closed world does better than that -- `Parser.resolveConcreteIteratorType` resolves
`List` to the single reachable implementation once the cull removes `LinkedList`, which
is why `lowerIteratorCalls` works at all. **But that pass has already taken the dispatch
win**: the census names `ArrayList.ArrayListIterator` specifically, which is proof the
devirtualization is firing. What a control-flow rewrite would add on top is the
ALLOCATION, and nothing else.

So the item is not "lower for-each to an indexed loop" -- that is unsafe where it is
tempting and redundant where it is safe. It is **"stop allocating the iterator"**:
100,566 objects per cycle, 4.4% of allocation volume. The iterator is created inside
`ArrayList.iterator()`, so the existing stack-allocation escape analysis has nothing to
work on at the call site; removing it needs the `return new T(this)` body materialised at
the caller first. `lowerIteratorCalls` already RECOGNISES that body shape to devirtualize,
so the recognition exists and only the inlining does not.

### char[] is 26.9% of everything allocated, and the fast path for it is dead code here

Allocation volume over a whole run, 774.1MB across 8,366,066 objects:

| class | MB | objects | share |
|---|---:|---:|---:|
| char[] | 208.6 | 1,285,250 | **26.9%** |
| java.lang.Object[] | 187.3 | 1,415,038 | 24.2% |
| java.lang.String | 53.1 | 833,989 | 6.9% |

`char[]` is the single largest allocation item in the VM, and most of it is StringBuilder:
a chain that grows past the default capacity allocates a new buffer per doubling, plus one
more in `toString`. `String.cn1ConcatN` exists precisely to collapse that into ONE fused
allocation whose length is computed from the parts up front.

**It is reached 8 times in the whole emitted corpus.** `ConcatScan` counts 304 all-String
chains that it could serve. The reason is structural: the lowering lives in
`Parser.visitInvokeDynamicInsn`, so it only fires on JDK 9+ `StringConcatFactory`
invokedynamic -- and `IndyScan` reports **0 indy concat sites** here, because this corpus
is Java 8 bytecode, which is what Codename One applications compile to. The fast path is
dead on the bytecode shape that actually ships.

`ConcatScan`, same corpus: 666 concat sites, 473 fusible, 1519 appends,
`{2=210, 3=126, 4=43, 5+=93}`, append arg types `{String=1305, int=165, Object=28, char=15}`,
non-fusible reasons `{store/return=181, branch=11, no toString=1}`.

**Discount that 473 before quoting it.** The emitted C shows 301 StringBuilder sites are
ALREADY stack-allocated by `stackAllocStringBuilders`, 174 of those with a stack-resident
fused buffer, against 123 still reaching `CN1_FAST_NEW`. A stack builder with a stack
buffer allocates nothing but its final String, so the incremental prize is the heap sites
plus the growth chains of stack builders that outgrew their fixed buffer -- not all 473.

### AND THERE IS A STANDING PRECONDITION ON RETRYING IT

This was attempted on this branch and **withdrawn as a miscompile** (f00db7e6fb). Matching
the chain on the builder's owner type alone mistakes calls on a *different* builder for
calls on the allocated one:

    consume(new StringBuilder(), existing.append(a).append(b).toString())

fuses the wrong chain and hands `consume()` the wrong arguments. It was the third
correctness defect out of that one pass, after maxStack under-reservation (a C stack
overflow) and running after the cull (calls emitted into deleted methods). The commit sets
the condition for its return explicitly: **receiver identity tracked through the operand
stack**, which this translator does not have.

Note what that implies about scope. `stackAllocStringBuilders` avoids the same trap by
tracking through a LOCAL SLOT, which is why it is sound and also why it bails so often. Any
fusion that reaches the chains it cannot take is, by construction, reaching the chains that
live on the operand stack -- so it inherits exactly the defect that was withdrawn. The
infrastructure comes first, or it does not go in.

---

# Round 11: the String allocation surface, and an instrument that was lying

Four targets came out of Round 10's attribution. Working through them produced one real
win, one win on a different axis than expected, two refutations -- and the discovery
that the instrument every allocation figure in this file rests on had a hole in it.

## THE CENSUS WAS BLIND TO EVERY FUSED OBJECT

`CN1_ALLOC_CENSUS` -- the `[ALLOC]` table -- was hooked at three entry points:
`cn1BibopFastAlloc`, `cn1BibopFastAllocNoZero`, `codenameOneGcMalloc`. `cn1AllocFused`
and `cn1FusedLatin1Begin` call `cn1BibopAlloc` directly, which is hooked by **neither**.
So every one-block object was invisible: `String.cn1ConcatN`, `StringBuilder.toString`,
and anything else built fused. On this corpus that is **~50MB of 856MB unreported**, and
`vm/CLAUDE.md`'s claim that "CN1_ALLOC_CENSUS counts at every entry point" was false.

The understated total is the smaller problem. **Any change that MOVES allocations onto
the fused path looked like a reduction.** A fused substring first measured **-6.06%
objects and -3.61% bytes**, essentially all of it the instrument going dark; the honest
figure is -3.10% allocations and no byte change. This is the same trap this file already
documents for `[GC-INSTR] outOfLineAllocs`, which misses the inlined bump path -- and it
bit anyway, because the census was believed to be the one that did not have it.

**A second, self-inflicted error on top.** The first fix re-attributed the fused payload
to the array class so the census would "read like" the two-object path -- which added a
phantom second COUNT and cancelled exactly the saving being measured, reporting -0.23%.
The census reports what happened; it is not the place to preserve an old shape.

Both entry points are hooked now. Every figure below is measured with both arms counted.

## Corpus confirmation changed a target

The Round 10 attribution was taken on the translator, which is string-heavy in an
unusual way. Static call sites settle it:

| | CN1 framework core | ByteCodeTranslator |
|---|---:|---:|
| `substring(` | **866** | 106 |
| `replace('` | 13 | **77** |
| `trim()` | 340 | -- |

`String.replace(char,char)` was the second largest char[] source at 293,958 allocations
and is **not worth chasing**: it is a mangling idiom, not an application one. `substring`
is the reverse and was taken instead. A runtime check on `CommonWorkloads` -- the ten
shapes every generated port app runs -- was attempted and is useless here: 455 char[]
allocations total, because its builders are already stack-allocated.

## What was implemented

**Fused substring.** The Java slice constructor allocates twice; `cn1SubstringFused`
builds the String and its characters as one block, preserving the parent's coder (a
slice of a Latin-1 string is Latin-1 by construction, so unlike `StringBuilder.toString`
there is nothing to scan for). Counters rather than inference: 302,438 calls, 253,000
fused, 49,438 too large and correctly falling back.

    allocations  8,879,218 -> 8,603,893   -3.10%  (275,325 fewer)
    bytes            855.7 MB -> 856.2 MB  +0.06%  (no change)

Bytes do not move and that is not hidden: the array header still exists, just inline,
and one larger block rounds up a BiBOP size class about as much as the second slot cost.
What it buys is 275,325 fewer allocator calls and slots to sweep, plus contiguity.

**A stack buffer a StringBuilder will not outgrow.** The escape analysis already parks a
non-escaping builder's buffer on the C stack, but sized by the ctor -- 32 chars -- and
`enlargeBuffer` always goes to the heap, so the first append past 32 abandoned it and
then climbed the 1.5x ladder. Floor raised to 128 units, swept rather than guessed:

| floor | allocations | bytes (MB) | char[] allocs | stack/site |
|---|---:|---:|---:|---:|
| 64 | 8,566,580 | 848.6 | 1,023,428 | 168 B |
| **128** | **8,528,201** | **845.5** | **984,019** | 296 B |
| 256 | 8,552,324 | 845.2 | 1,008,454 | 552 B |

**128 is the knee**, and 256 being worse on allocations is not noise: at 552 B/site the
per-method 2KB stack budget caps more sites back to the ctor's 32, so fewer sites get a
buffer at all. **The floor and the budget interact; tuning either alone reads wrong.**

Against the 32-unit baseline: **allocations -1.29%, bytes -1.18%, char[] allocations
-9.92%, char[] bytes -5.54%.** The first change in this whole sequence to move bytes.

Only StringBuilder is enlarged, and that restriction is the correctness argument rather
than caution: `value.length` IS the capacity there, unobservable except through
`capacity()`, which the JDK does not specify beyond the minimum. For any other `@Fused`
class the array length may be semantic.

## Refuted, and recorded so they are not re-attempted

- **Compacting `String(char[],int,int)`** -- the busiest String constructor, 355,212
  calls. Measured **+1.00%**, worse. `toCharNoCopy()` returns the backing array with NO
  COPY when it is a char[] of the right length; a byte[]-backed String cannot take that
  path and falls through to `toCharArray()`, which allocates. Compaction is not a storage
  argument, it is a question about CONSUMERS: it pays where the string is read as a
  string and costs where it is read as characters.
- **Eliding `toCharArray()` in the translator** -- built, gated, non-vacuous, and worth
  **+0.01%**, because its target had already been removed by the `String(String)` fix two
  commits earlier. Kept for the 9 sites an application would hit; see that commit.

## Round 12: ATHROW was the only thing keeping the hottest iterator method framed

`ArrayListIterator.next()` runs once per element of every for-each loop over an
ArrayList, and it carried a full named shadow frame -- `DEFINE_INSTANCE_METHOD_STACK`,
i.e. frame push, locals array, SP, frame pop -- on every one of those calls.

The cause was a single opcode. `isFramelessEligible()` rejects any method containing
ATHROW (`isFramelessObjectOpcode` excludes it deliberately), and `next()` contains three
throws that essentially never execute: the modCount check, the past-the-end check and
the bounds check on the backing array.

Splitting the throws into a private `nextSlow()` -- guards unchanged, in place, only the
`throw` statements moved out of line -- makes the method eligible. Confirmed in the
emitted C for the same source:

| | frame macro |
|---|---|
| before | `DEFINE_INSTANCE_METHOD_STACK(3, 4, 0, 691, 628)` |
| after | `DEFINE_METHOD_STACK_FRAMELESS(3, 4, 0)` |

**No check is dropped.** `nextSlow()` re-tests the three conditions in the SAME ORDER, so
the exception a caller sees is identical; the middle one is NoSuchElementException and
the outer two are ConcurrentModificationException, so the order is load bearing. This is
deliberately not the "drop the bounds check" shortcut that cost this class 145 screenshot
tests -- see the comment in `ArrayList.java`, which is still the governing note.

Gates: `run-gauntlet.sh` GREEN (15 tortures byte-identical to JDK 25, both GC stop
modes); self-hosting Gate D PASS, **Gate A PASS -- 798 files byte-identical**, negative
control PASS.

### What it is worth: 1.71x on the shape it governs

`ForEachBench` is deliberately narrow -- 400 passes over a 20,000-element ArrayList, so
`next()` is the overwhelming majority of the work. A whole-program ratio cannot resolve a
change to one method on this host, so the driver makes the method the program.

Interleaved, each binary reporting its own min of 12 inner reps, 7 rounds, at load
average 4.9:

| arm | ms | |
|---|---:|---|
| before (framed `next()`) | 128-129 | |
| after (frameless `next()`) | **75** | **1.71x** |
| JDK 25, same source | 21 | |

**7 of 7 rounds identical to +/-1ms, a 0.8% spread.** That is what makes the figure
usable on a host that cannot normally resolve 5%: the effect is 71%, two orders of
magnitude outside the round-to-round noise, and the arms were interleaved rather than run
back to back.

The JDK 25 column is the part worth acting on. **This shape went from 6.13x JDK 25 to
3.57x**, and the residual is not frame overhead any more -- it is that HotSpot's escape
analysis deletes the iterator object outright and iterates the backing array, which is
exactly the specialization this round's `ForEachT` gate was written for. Removing a frame
from `next()` cannot reach that; not calling `next()` at all can.

**No whole-program figure is quoted, and that is deliberate.** The self-hosting corpus
ratio is a few percent question on a box this file's own rule says cannot resolve 5%, and
every window available had `fileproviderd` and `bird` between them burning ~130% CPU at a
load average near 10. The microbenchmark survives that because of its effect size; a
whole-program A/B would not, and has to wait for a quiet machine.

### Withdrawn in the same round: folding javac's synthetic accessors

An inner class reading a PRIVATE field of its outer class cannot emit a GETFIELD, so
javac synthesizes `static int access$000(Outer o) { return o.field; }` and routes every
read through it. The translator emits the accessor into the outer class's `.c` and the
inner class into its own, so each read is a CROSS-TRANSLATION-UNIT CALL that only LTO can
remove -- and the clean, desktop and CMake targets do not link with LTO.

`Invoke.asInlinableFieldAccess` already folds trivial getters, but its INVOKESTATIC
branch requires a NO-ARGUMENT descriptor, so it only ever saw `GETSTATIC` forwarders and
never these. Teaching it the one-object-argument shape is exact rather than approximate:
a static call with one argument and a non-void return pops one slot and pushes one, which
is GETFIELD's stack effect, and it dereferences the same reference, so a null argument
still throws at the same point.

Measured on this corpus, it worked: 9 files changed, 167 lines of emitted C collapsing to
77, every removed line an accessor call and no added line containing one -- ArrayList 3
call sites (one of them in `next()` itself), ArrayDeque 9, TreeMap's sub-map family, plus
`Parser` and `SourceManifest`, i.e. ordinary application code and not a library special
case.

**It is withdrawn because Gate A went red: 9 of 798 files differed between the JVM
translator and the self-hosted one, on exactly those 9 files.** The JVM side folded and
the ParparVM side did not. What is ruled out, each measured rather than argued:

- Not optimize-order. Memoizing the verdict so it is always read from raw bytecode --
  `updateInlinableFieldDependencies` queries every invoke before any `optimize()` --
  changed nothing; both versions failed identically on the same 9 files.
- Not a stale self-hosted binary. The binary was rebuilt and verified to CONTAIN the
  diagnostic strings.
- Not a swallowed diagnostic. `System.err` reaches fd 1 on the clean target (verified
  with a probe), and `verify-selfhost.sh` captures both streams.
- Not the string predicates. `"(Ljava/util/ArrayList;)I".endsWith(")V")`,
  `"access$000".startsWith("access$")` and `endsWith("ArrayList")` all answer correctly
  on ParparVM, checked on the target.
- Not `DISABLE_INLINE`. `System.getProperty`/`getenv` both return null on the target,
  under `env -i` as the gate runs it, so the flag is false on both sides.
- Not dead-code elimination. The method is emitted in full, not as a `return 0;` stub.

What is left is that the self-hosted translator never queries those instructions at all:
a probe at the very top of the INVOKESTATIC branch printed 65 lines on the JVM side and
**zero** on the ParparVM side, for the same corpus. That is a real divergence in the
translator's own behaviour under ParparVM and it is worth more than the optimization
was -- but it is unexplained, so the fold does not land on top of it.

**The precondition on retrying: root-cause the divergence FIRST.** The next probe to
write is in `ByteCodeClass.updateAllDependencies`, counting the invokes it actually
visits per class on each host -- the question is whether the instruction list differs, or
whether that loop is reached at all. Landing the fold before that answer exists would be
shipping a translator whose output depends on which host ran it.

## Round 13: the iterator is DELETED, not devirtualized -- and the gate was lying

The for-each loop no longer calls anything. Where the receiver is a provable
java.util.ArrayList the translator rewrites the loop into a walk over the backing
array: no Iterator allocated, no hasNext(), no next(), and NO RUNTIME CLASS TEST,
because the class is proven rather than checked.

    iterator(); ASTORE it            ->  ASTORE c (the COLLECTION, same slot)
                                         ICONST_0; ISTORE i
  L: ALOAD it; hasNext(); IFEQ end   ->  L: ILOAD i; ALOAD c; GETFIELD size
                                            IF_ICMPGE end
     ALOAD it; next()                ->     ALOAD c; GETFIELD array
                                            ALOAD c; GETFIELD firstIndex
                                            ILOAD i; IADD; AALOAD; IINC i 1

**The body is never touched.** Only the header and the element fetch are rewritten, in
place, so every label keeps its identity -- `break` still targets the same end label,
`continue` the same condition label, an early `return` is untouched, a try/catch inside
the body keeps its exception range, and a nested loop is just body. The earlier plan to
emit the body twice under a class guard is not needed and not done: there is nothing to
clone, so there is nothing to clone wrongly.

### Proving the class instead of testing it

The blocker was never the rewrite, it was the receiver's type. `resolveConcreteIteratorType`
asks the DECLARED type, and `List` has ~100 reachable `iterator()` implementations, so it
resolves **13 of 295** for-each sites on this corpus. What the receiver actually HOLDS is a
different question, and in a closed world it is answerable:

| receiver shape | sites | provable |
|---|---:|---|
| instance field | 141 | **75 hold only `new ArrayList`**, 23 polymorphic, 28 stored from a call/parameter |
| local | 13 | 13 (single assignment is the allocation) |
| parameter | 51 | not attempted |
| method return | ~60 | not attempted |

A whole-program pass records, for every field of a `java.util.*` type, the concrete class
of every store. Only `NEW X; ...; INVOKESPECIAL X.<init>; PUTFIELD` counts; a store of a
parameter or of another method's return poisons the entry. A field survives only if EVERY
writer in the program agreed, which is a complete answer rather than a common case --
there is no reflection and no class loading here.

**71 sites rewritten** on the self-hosting corpus (ByteCodeClass 38, BytecodeMethod 28,
Parser, ByteCodeTranslator, NativeSignatureVerifier), against 0 before.

The slot rule mattered more than expected. Counting ASTOREs and ALOADs of the iterator
slot across the whole METHOD looks safer and is much worse: javac reuses one slot for the
iterators of sequential for-each loops, so a method with two of them refuses both. That
alone cost **38 of the 71** sites. The check is now the loop's lifetime -- between the
store and the end label the slot is read exactly at hasNext() and next(), and after the
loop it is redefined before it is read again. A third read inside the loop is
`it.remove()`, which an indexed loop cannot express, and is still refused.

### What it is worth

`ForEachBench`, interleaved, each binary reporting its own min of 12 inner reps, 7 rounds,
at load average 29 (the effect is large enough to survive it; the spread is 1ms):

| arm | ms | vs JDK 25 |
|---|---:|---|
| original (framed next()) | 88-92 | 5.9x |
| frameless next() (Round 12) | 53 | 3.5x |
| **indexed, no iterator** | **19** | **1.27x** |
| JDK 25 | 15 | 1.00 |

4.6x over the original and 2.8x over Round 12. The remaining 1.27x is no longer the
iterator -- it is gone -- so the next question on this shape is boxing, not dispatch.

Unchecked element read, and it is sound rather than optimistic: at each read `i < size`
holds and ArrayList maintains `firstIndex + size <= array.length`, so the access is inside
the array; Java arrays cannot be resized, so a concurrent structural modification yields a
STALE element, never an out-of-range access. What is given up is
ConcurrentModificationException, deliberately and by agreement.

### THE GATE WAS VERIFYING A THREE-HOUR-OLD BINARY

Gate A failed on 6 of 798 files -- exactly the files the pass touches -- and the JVM side
had rewritten them while the "self-hosted" side had not. Six causes were ruled out by
measurement before the real one: `verify-selfhost.sh` prefers `target/parpar-O3`, and
`build-selfhost.sh` with no arguments writes `target/parpar`. Every run all evening built
`parpar` and then verified a `parpar-O3` from three hours earlier.

**This had already cost a correct optimization.** Round 12 withdrew the synthetic-accessor
fold -- 9 files, 167 lines of emitted C collapsing to 77 -- because gate A "failed" on it.
It failed against the same stale binary. That withdrawal should be revisited; the finding
it was based on was an artefact.

Two things were wrong with how that was chased. The script PRINTS its subject
(`verify-selfhost: subject ...`) precisely so a run is never ambiguous, and that line was
never read. And every probe written to chase it printed nothing on the parpar side, which
was read as "the code path does not execute" when it meant "this binary predates the code".
`verify-selfhost.sh` now refuses a subject older than the translator sources, and the
refusal is demonstrated rather than assumed -- pointing it at the stale `parpar-O3` fails
with the reason instead of running.

Gates on the final tree: `run-gauntlet.sh` GREEN (15 tortures byte-identical to JDK 25,
both GC stop modes), Gate D PASS, **Gate A PASS -- 798 files byte-identical**, negative
control PASS.

`ForEachT` had to be rebuilt to be worth anything. Its receiver matrix passes the list as
a PARAMETER, which the analysis refuses, so it exercised the fast path once in twenty
sites and would have passed whether or not the rewrite worked. It now drives every shape
down the fast path by BOTH routes -- a local that is the allocation, and a Holder whose
instance field the whole program only ever stores `new ArrayList` into -- 17 fast paths in
the torture, and every shape still byte-identical to the host.

### Reinstated: the synthetic-accessor fold, which was never broken

With the subject binary pinned, the fold withdrawn in Round 12 passes every gate it was
said to fail. It was measured against a `parpar-O3` three hours older than the source;
nothing about the optimization was wrong.

Restored unchanged: the one-object-argument static accessor fold in
`Invoke.asInlinableFieldAccess`, with the verdict memoized so it is always read from raw
bytecode (`updateInlinableFieldDependencies` queries every invoke before any `optimize()`
rewrites a body in place). Accessor CALL sites on the self-hosting corpus: **148 -> 127**.

Gates: `run-gauntlet.sh` GREEN, Gate D PASS, **Gate A PASS -- 798 files byte-identical**,
negative control PASS.

The lesson is not about the fold. Six causes were ruled out by measurement before the real
one was found, and every one of those probes was sound -- they were run against a binary
that predated the code they were probing for. **A probe that prints nothing is not
evidence that the code did not run.** The first thing to check is what the harness is
actually executing, which `verify-selfhost.sh` had been printing on its first line the
whole time.

### CORRECTION to the Round 13 table: that was measured without LTO, and it ships with it

The Round 13 figures (indexed 19ms, 1.27x JDK 25) are real but they are **not the shipping
shape**, and the conclusion drawn from them -- "the residual is no longer dispatch" -- was
wrong about what the residual was. `translate-and-build.sh` links without LTO by default.
The indexed loop reads three ArrayList fields (`size`, `array`, `firstIndex`) and calls
`Integer.intValue()`, all defined in OTHER translation units, so without LTO the loop that
was supposed to have no calls in it has **four calls per element**.

iOS ships ThinLTO -- `LLVM_LTO = YES_THIN` in both Xcode templates and `-flto=thin` on the
CMake Release targets -- so that is the configuration the number should come from.
Interleaved, min of 12 inner reps, 7 rounds:

| arm | ms | vs JDK 25 |
|---|---:|---|
| indexed, no LTO | 19-20 | 1.27x slower |
| **indexed, ThinLTO** | **4-6** | **~3x FASTER** |
| JDK 25 | 15 | 1.00 |

Confirmed structurally, not just by the clock: `get_field_java_util_ArrayList_size` and
`get_field_java_util_ArrayList_array` are **absent from the symbol table** of the ThinLTO
binary and present in the non-LTO one.

Non-vacuity checked, because min-of-N over an unchanging list invites a compiler to compute
the sum once: doubling the outer pass count doubles the time (4-6ms -> 10ms), so the work
is not hoisted across reps.

**Why it beats JDK 25 rather than merely matching it** is the representation, not the
codegen. `Integer.valueOf(i)` is a TAGGED IMMEDIATE here for every value, so the loop
walks one array of words and shifts -- no dereference per element. HotSpot's Integer cache
covers only -128..127, so 19,872 of these 20,000 elements are distinct heap objects and
every `intValue()` is a load from a scattered allocation. That is the "fixed in-place RAM,
zero copying" advantage doing real work, and it is invisible on any benchmark whose
elements are not boxed.

**The measurement lesson, which is the same one as the stale binary above:** a ratio is
only about the VM if the arm being measured is the arm that ships. Two figures in one
session were quoted from a configuration nobody runs -- an -O1 gate binary would have been
a third. `ForEachBench`'s javadoc now carries the LTO requirement and the scaling check.

## Round 14: the whole-program ratio did NOT move, and that is the useful result

Full self-hosting workload, shipping shape (`build-selfhost.sh -O3`, which implies
`-flto=thin`), subject `parpar-O3`, 5 interleaved rounds, time = min, memory = MAX
phys_footprint. **834 files identical across all three arms**, so the ratios are also a
correctness result.

Two runs, three minutes apart:

| | run 1 (load 8.5) | run 2 (load 14.0) | recorded FINAL baseline |
|---|---|---|---|
| parpar time | 1.22s | 1.36s | 0.88s |
| jdk25 time | 0.93s | 0.94s | 0.65s |
| jdk8 time | 1.18s | 1.18s | 0.91s |
| **vs jdk25 time** | **1.31x** | **1.45x** | 1.35x |
| parpar peak | 1262 MB | 1562 MB | 835 MB |
| jdk25 peak | 709 MB | - | 525 MB |
| **vs jdk25 memory** | **1.78x** | **2.22x** | 1.59x |

**Neither run is trustworthy in absolute terms and the two do not agree**, so the honest
statement is a range: time 1.31-1.45x JDK 25, memory 1.78-2.22x. The machine had Spotlight
indexing, a CN1 simulator and another checkout's `core-unittests verify` running; this file
already records that this host cannot resolve 5%.

Worth noting even so: **the parpar arm is far more load-sensitive than the JVM's.** Its
samples spread 1.36-1.83s in run 2 while JDK 25 stayed 0.94-1.05s, and its peak footprint
moved 1050-1262 MB inside a single quiet-ish run. A concurrent collector with CPU-derived
marker threads loses more to contention than a JVM does, which is itself a finding: any
whole-program ratio from this harness on a busy machine is biased AGAINST parpar.

### The for-each work is worth ~3% here, and that was predictable

Round 13 made a pure-iteration loop **3x faster than JDK 25**. The whole-program ratio is
unchanged against the 1.35x baseline. That is not a contradiction and not a measurement
failure -- it is the difference between a microbenchmark and a workload:

- earlier profiling put ITERATION at **19.3%** of main-thread time;
- the rewrite fires on **71 of ~295** for-each sites, i.e. ~24%;
- so the reachable whole-program gain is roughly `0.193 * 0.24 * (2/3)` = **~3%**,

which is below this host's resolution and consistent with seeing nothing. **The
microbenchmark was never evidence about transpilation throughput**, and quoting it as
such would have been the third configuration error in one session.

So: the remaining 1.3-1.45x is not iterator dispatch, and it is not frame overhead. What
is left to attack is what the profile says is left -- and the next measurement worth
taking is a fresh `-DCN1_GC_CONFORM` attribution on a QUIET machine, not more loop work.
Memory is now the larger gap of the two (1.78x+ against 1.31x), and D1/D2 in the plan --
taking BiBOP pages from mmap so the 92.9 MB of malloc-retained free space is returnable,
and sizing the reference-field share of the live heap -- are still untouched and are where
the 700 MB delta actually lives.

## Round 15: the memory census, and the aging slack finally ruled on

Memory is the larger gap, so this is the census the plan made a precondition for any
representation work. Built `-DCN1_ALLOC_CENSUS -O3` and read `[JHEAP]`/`[LIVE]`, which
already existed and had never been pointed at this question.

Process footprint 957MB at the final sweep. Where it is:

| | MB |
|---|---:|
| live Java objects | 502.9 |
| BiBOP slack (reserved 463.4 - live 372.3) | 91.1 |
| legacy heap, 358,717 objects | 130.6 |
| malloc idle (allocated 943.7 - inUse 883.5) | 60.2 |
| object table, 897,369 / 960,000 slots | 7.3 |

And the composition of the 488MB live set, which is the number that matters, POST-SWEEP
rather than at exit (the two agreed, so this is steady state and not a batch program
dying between collections):

| bucket | MB | share |
|---|---:|---:|
| traced (reachable) | 364.3 | 74.6% |
| fresh (grace) | 0.3 | 0.1% |
| aging (marked last cycle, not this one) | 65.7 | 13.5% |
| dead (older, unswept page) | 57.6 | 11.8% |

**A quarter of the heap is unreachable memory being held.** Split by shape: 289.8MB in
1,334,696 arrays and 198.2MB in 2,373,354 objects, which is **40.7MB of 32-byte array
headers and 36.2MB of 16-byte object headers -- 76.9MB, 15.8% of the live set, in headers
alone.** HotSpot's equivalent (16-byte array header, 12-byte compressed object header)
would be 47.6MB, so ~29MB of that is excess rather than inherent.

### Refuted in one build: smaller BiBOP pages

91MB of slack across 7,297 non-empty pages is ~11.5KB unused per 64KB page, which reads
like page-tail waste. `-DCN1_BIBOP_PAGE_SIZE=16384` should then have cut it ~4x. It made
the peak **worse -- 889MB against 854MB** -- so the slack is not tail waste, it is freed
slots inside pages that are still in use, and recovering it needs compaction, which this
VM deliberately does not do. One build, one answer, no further page-size tuning.

### The aging slack: 12%, and 8 of 8 rounds

`cn1GcAgingSlack` was written as a knob with the default left at 1 and a note asking for
the gates to rule on it rather than a census. They have. 8 interleaved rounds, peak
phys_footprint:

    slack=1   872 907 891 789 946 953 842 978 MB
    slack=0   840 749 834 772 870 852 702 695 MB

**Lower in 8 of 8** -- sign test p ~= 0.004, which is what makes the direction certain on
a host that cannot resolve 5% -- mean reduction ~12%, range 2-29%. Wall clock did not
move (0.77-0.82s either way): the slack buys memory, not time.

What ruled on the risk, premature reclamation of an object a live field still points at:
`run-gc-verify.sh` GREEN at slack=0 with all ten drivers clean AND all three self-tests
still detecting their injected faults (grace-pass, early-free, dangling referent), so it
did not pass vacuously; `run-gauntlet.sh` GREEN, 15 tortures byte-identical; and the
self-hosted translator's whole output tree byte-identical to the JVM reference over 797
files. A reference freed a cycle early does not emit identical C.

Default is now 0. The knob stays for diagnosis and is the ablation the GC gates use.

### Where that leaves the gap, honestly

Peak ~800MB against JDK 25's 468MB on the same metric, so ~1.7x, down from 1.79x. The
remaining ~330MB is NOT in reach of another knob, and the census says where it is:
~105MB of reference-field bytes in `java.lang.Object[]` alone (124MB total, 577,239
arrays), ~29MB of header excess, 91MB of uncompactable slack, 60MB of malloc idle.

**The single biggest item is 32-bit references (plan D2), and the census that plan made
its precondition now exists.** It is also the change with the largest blast radius in the
VM -- every generated struct, every port native that reads a field out of one, and a
second encoding for legacy-heap objects outside the window. It should be scoped and
prototyped behind this census, not started on the strength of it.

## Round 16: D6 REFUTED for the cost of one run, and it names its own replacement

Plan item D6 -- "try/catch disables four optimizations at once ... this single exclusion
is plausibly the largest codegen item left" -- said its first step was a measurement
rather than code, and that if the number came back small "the whole line dies for the
cost of one run". It came back small.

`-Dcn1.framelessCensus=true`, self-hosting corpus:

    methods=3350  frameless=2453 (73.2%)
      excluded by try/catch ALONE:  12   (0.36%)
      excluded for other reasons:  885

**Twelve methods.** The exclusion is real, it does cost frameless codegen, bounds-check
elimination and StringBuilder stack allocation together -- and it applies to 0.36% of
methods on a program that is full of try/catch. D6 is dead; do not spend the setjmp
risk on it.

The same run names the replacement, which is why the instrument tallies reasons rather
than just ruling try/catch out:

    excludedOther: ctorOrClinit=557  synchronized=29  onDeviceDebug=0
                   unhandledOpcode=0  empty=50   (rest: object args/returns/locals)

**Constructors and static initializers are 557 methods, 16.6% of all of them, and 46x
the try/catch item.** `isFramelessEligible` defers them deliberately -- "super-call /
field-init / partially-constructed-receiver semantics need more care" -- and every
allocation in the program runs one, so they are hot by construction. That is the
frameless item worth scoping, and it was invisible while the plan was looking at
try/catch.

`unhandledOpcode=0` is worth noting too: the conservative whitelist of opcodes is not
excluding anything on this corpus, so widening it would buy nothing.

The census is `-D` gated, defaults off, and changes no emitted output: gates on the tree
carrying it are GC-VERIFY GREEN (three self-tests still non-vacuous), gauntlet GREEN,
Gate D PASS, Gate A PASS (798 files byte-identical), negative control PASS.

## Round 17: a PRE-EXISTING intermittent SIGSEGV under memory pressure

Found while measuring, not while looking for it: the self-hosted translator crashes
roughly **1 run in 16** when another memory-hungry process runs alongside it. It produced
11 or 748 of 798 files and exited 139 (SIGSEGV) or 138 (SIGBUS).

**It is not caused by anything on this branch, and that was settled by A/B rather than
by argument.** A worktree at 389a7e9341 -- before the frameless split, the for-each
lowering, the accessor fold and the aging-slack default -- was built and run under the
same load:

| binary | runs | crashes |
|---|---:|---:|
| pre-change (389a7e9341) | 16 | **1** |
| this branch, `CN1_GC_AGING_SLACK=1` | 16 | **1** |
| this branch, compiled default (slack=0) | 28 | **0** |

Same rate before and after; the new default has if anything shown fewer, though 0/28
against 1/16 is not a significant difference and should not be read as one.

The crash reports say what it is. The pre-change one is `KERN_INVALID_ADDRESS at
0x0000000000000000` inside `Parser.writeOutput` -- **a null dereference**, which on the
clean target is a hard SIGSEGV rather than a NullPointerException, because that target
installs no signal handler (only the iOS port does). The branch one is
`0xffffffff0000016c` inside `updateInlinableFieldDependencies`, one frame deeper in the
same `writeOutput -> eliminateUnusedMethods -> cullClasses -> updateAllDependencies`
path.

Both appear only under memory pressure, which points at an allocation returning
JAVA_NULL and the generated code dereferencing it: `codenameOneGcMalloc` has a
retry-after-collection loop, but `CN1_FAST_NEW`'s inlined BiBOP bump path does not go
through it. **A VM that segfaults instead of throwing OutOfMemoryError is a robustness
bug, not a tuning one**, and on a device it is the difference between a caught error and
a crash report.

This is recorded rather than fixed here: it predates every optimization on this branch,
it needs its own reproducer (the load generator above is one), and the fix is in the
allocation-failure path rather than anywhere this branch has touched. It is also why the
gauntlet and the GC verifier did not catch it -- neither runs under external memory
pressure.

## Round 18: RETRACTION -- the JVM comparison in Round 15 used the wrong number

Round 15 ended with "our reachable live set is 364MB, the JVM's entire heap is 273MB" and
projected from there that 32-bit references would still leave ~1.5x. **The 273MB was not a
live set.** It was G1's `used` at peak against a 1032MB ceiling -- i.e. mostly uncollected
garbage, because a JVM with that much headroom has no reason to collect. Measured with
`-Xlog:gc`, this program provokes exactly ONE collection in a default JVM:

    GC(0) Pause Young (Normal) (G1 Evacuation Pause) 49M->12M(1032M) 7.241ms

Everything after it accumulates. Projecting a representation saving onto that figure was
arithmetic on a number that did not mean what it was used for, and the conclusion drawn
from it should not have been stated.

### The number that is actually comparable: what each VM can COMPLETE in

    JDK 25  -Xmx32m   OutOfMemory, 0 files
            -Xmx48m   OutOfMemory, 0 files
            -Xmx64m   OutOfMemory, 11 files
            -Xmx96m   798 files, footprint 239MB, 1.28s
            -Xmx128m  798 files, footprint 280MB, 1.14s
            default   798 files, footprint ~485MB, 0.75s

    parpar  default   798 files, footprint ~680MB, 1.00s
            CN1_SIMULATE_PROC_MEMORY_LIMIT=128MB -- did NOT complete, wedged
                      (>2min against 1s; the documented pacing bistability)

So the honest statement is: **the JVM does this job in a 96MB heap / 239MB process, and we
need ~680MB.** That is 2.8x on process footprint against a JVM that has been told to be
frugal, and 1.4x against a JVM left to sprawl. Both are worth quoting; neither is the
1.5x-after-32-bit-refs claim Round 15 made.

### Conservative roots are NOT the explanation, and that is measured now

The obvious hypothesis for holding ~4x what the JVM holds is the conservative stack scan
retaining garbage. `-DCN1_DISABLE_CONSERVATIVE_GC_ROOTS` with the required codegen pairing
(`-Dcn1.frameless.objects=false -Dcn1.frameless.instance=false`, via CN1_SELFHOST_JAVA_OPTS
-- note the variable name, an earlier attempt used a name build-selfhost.sh ignores and
silently measured frameless-with-precise-roots, which is the unsafe combination):

| arm | peak | wall |
|---|---:|---:|
| base (conservative roots, frameless) | 737 680 678 MB | 1.00-1.05s |
| precise roots (shadow stack) | 602 563 670 MB | 1.52-1.58s |

**-12% memory for +55% time.** Conservative roots are worth about a tenth of the gap, not
most of it, and buying that tenth costs more than it is worth. The hypothesis is answered
and the arm should not be re-run for this purpose.

### What the per-object numbers actually say

From the live census, average bytes per object against what HotSpot would spend:

| | ours | HotSpot equivalent |
|---|---:|---:|
| `Object[]`, ~24 refs | 225 B | ~112 B (16B header + 24x4) |
| `char[]` | 270 B | ~270 B (comparable) |
| `String` (fused, payload inline) | 109 B | ~24 B + its payload |

So **roughly 2x per object on reference-bearing shapes, and parity on primitive payload.**
2x is the representation gap and it is real -- but 2x does not explain needing 680MB where
the JVM needs 239MB. The rest is that we HOLD more: 91MB of slack in pages we cannot
compact, freed slots that only a moving collector reclaims, and a legacy heap whose blocks
go back to malloc rather than to the OS.

### The direction this points, which is not compressed oops

Compressed oops is the answer Java reached for BEFORE it had a compacting collector, and it
is worth ~2x on reference fields only. What actually lets a JVM run this in 96MB is that
**it compacts**: live objects are moved together, so there is no slack, no size-class
rounding and no fragmentation, and the heap is exactly as big as the live set.

This VM cannot move objects -- natives hold interior pointers and conservative roots pin
whatever they resolve -- so the equivalent has to be bought a different way, and the closed
world is what makes that possible. The fused String/StringBuilder is the existing proof:
the payload is placed INSIDE the owner at translate time, which removes a header, a
reference and the slack of a second allocation simultaneously, and needs no collector
support at all. The census says where the same trick has the most left to give: `Object[]`
at 124MB over 577,239 arrays, of which the ArrayList and HashMap backing stores are a known
and statically identifiable majority.

That is the line to scope next, and it is a translate-time layout question rather than a
collector one.

### And the intermittent SIGSEGV, localized

Round 17 recorded a crash 1 run in 16 under memory pressure and guessed at the cause --
"an allocation returning JAVA_NULL ... CN1_FAST_NEW's inlined bump path does not go through
[the retry loop]". **That guess was wrong and is retracted.** `CN1_FAST_NEW` falls back to
`__NEW_X` when the bump path returns 0, `__NEW_X` calls `codenameOneGcMalloc`, and that
function never returns null: its retry is unbounded by design ("this VM has no way to fail
an allocation"). Arrays take the same path. There is no allocation-failure bug there.

An AddressSanitizer build caught the real one on the 14th iteration under load:

    SEGV on unknown address 0x1c
      #0 java_lang_String_replace___char_char_R_java_lang_String
      #1 com_codename1_tools_translator_Parser_getClassByName
      #2 com_codename1_tools_translator_Parser_writeOutput

`getClassByName` is `classIndex().get(name.replace('/','_').replace('$','_'))`, so `name`
is NULL and a null receiver on the clean target is a hard SEGV rather than an NPE -- that
target installs no signal handler; only the iOS port does. The call at Parser.java:875 is
`getClassByName(bc.getBaseClass())`.

**The JVM runs this identical code over this identical corpus and never fails**, so
`getBaseClass()` is not legitimately null here. A field reading null intermittently, only
under memory pressure, is a live object being reclaimed or a header being overwritten --
a VM correctness bug, not a translator one. It reproduces on 389a7e9341, so it predates
every optimization on this branch.

Note what did NOT find it: the gauntlet, the GC verifier and the self-hosting gates all
pass, because none of them runs under external memory pressure. The reproducer is the load
generator in Round 17 plus an ASan build; that combination should become a gate.

## Round 19: the crash is an UN-HANDSHAKED MAIN THREAD, and the code already said so

Round 17 recorded an intermittent SIGSEGV and Round 18 retracted its guessed cause. The
investigation has now found it, and the decisive part is three lines that were already in
the tree:

- `cn1_globals.m` ~3898, inside `codenameOneGCMark`: the collector raises
  `threadBlockedByGC` and waits for `threadActive` to fall **only** inside
  `if(t->lightweightThread)`.
- `nativeMethods.m` 2407 and 3347 are the ONLY places that set `lightweightThread =
  JAVA_TRUE`, and both are thread-CREATION paths.
- `ByteCodeClass.java` 1412: "MAIN IS NOT REGISTERED AS A LIGHTWEIGHT THREAD, and that is
  the behaviour every shipping build has always had."

On the `clean` target the whole application runs on the process's initial thread. That
thread is therefore **never cooperatively stopped**: it runs Java at full speed through the
entire mark and the entire sweep, and its roots come from a single SIGUSR2 capture per
cycle that is released as soon as they are taken. A probe measured it directly -- per run,
9-15 thread scans of which **0 or 1** used the cooperative capture (`coop=0 sig=14`).

The A/B is one binary and one environment variable, arms interleaved, three concurrent
JDK 25 translations as load:

    main flagged lightweight   crash=0  of 40
    default (not flagged)      crash=5  of 40

The corruption has many faces, all of them the same cause -- a live object reclaimed and
its slot handed back out: a `ConcurrentModificationException` from a corrupted `modCount`
int; a `NoSuchElementException` from corrupted iterator state; a SIGSEGV at
`0x0000010100000012` where a String's `value` field held a `{mark=18, heapPosition=257}`
header pair, i.e. the slot had been reused by another object; SIGBUS **inside
`codenameOneGCSweep` itself**, walking a corrupt entry. The ASAN trace this started from is
`Parser.java:878`, not 875: `for(String s : bc.getBaseInterfaces())` yielded a null element
out of `Arrays.asList(interfaces)` -- a live array element read as 0.

**Flagging main lightweight is a diagnostic, not the fix.** It costs ~5x (6.7-7.8s against
1.3s) because a single-threaded program whose one mutator parks for the whole mark IS
stop-the-world, and `ByteCodeClass.java` 1419 spells out the second hazard: "lightweight"
is a promise that the thread parks, and the collector migrates `pendingHeapAllocations`
without `threadHeapMutex` on the strength of it. On the native macOS target main becomes
AppKit's event loop and must stay native.

So the bug is not that main is unflagged; it is that **something the collector needs is not
actually covered by SATB for a mutator that never parks**, and the current design gives
main the cheaper treatment on purpose. The leading suspect from the investigation is the
per-cycle BiBOP page-geometry snapshot in `cn1ConservativeResolve`: `CN1ConsPage` caches
`slotSize/firstSlotOffset/slotCount/bumpIndex`, and the acquire-ordering argument at the
refresh loop covers a reformat during the refresh but not one the mutator performs later in
the same cycle -- which only a never-parked mutator can do.

### Why every gate misses it, and what to change

`CN1_GC_VERIFY` is structurally blind here. `cn1BibopSweep` poisons a reclaimed slot and
then immediately pushes it onto the page free list; only LEGACY blocks are quarantined. By
the next verify pass the slot is a live object again and the dangling field resolves
cleanly. Measured: 60 verifier runs under load gave 2 crashes and `violations=0`.

Three things follow, none of them done yet:

1. **`CN1_GC_SIGNAL_STOP=1` is the reproducer** -- a runtime env var, no rebuild, and it
   raises the rate about 8x (8 crashes in 17 runs against 1 in 27).
2. **Quarantine freed BiBOP slots for N cycles** instead of returning them to the free list
   immediately, so the verifier can observe a dangling reference before the slot is reused.
   Without that, `run-gc-verify.sh` cannot catch this class of bug at all.
3. **The gates need external memory pressure.** The gauntlet, the GC verifier and the
   self-hosting gates all pass; none of them runs anything alongside the target.

Ruled out with evidence: allocation returning JAVA_NULL (the Round 17 guess -- the retry is
unbounded); `java.lang.ref` clearing (the program contains no Reference class); stack
allocated arrays (zero `alloca` uses in the generated C); the force-stop escalation (its
message never appeared, `noStop=0`); frameless object codegen (4 crashes either way over
~55 interleaved pairs); and the aging slack (slack=2 crashed at iteration 6).

## Round 20: ONE contract for every collection -- storage, iteration and lambdas

Three separate optimizations have landed on collections so far and each is a special case:
the for-each lowering hard-codes `java/util/ArrayList` and reads its fields by name; the
block storage hard-codes HashMap in a two-entry table; nothing touches the other eight
containers or the functional entry points at all. This is the design that replaces all
three with one mechanism, and the case for it is measured rather than aesthetic.

### What the heap actually holds

From the live census of the self-hosting corpus:

| | objects | note |
|---|---:|---|
| `ArrayList.ArrayListIterator` | **337,322** | 67% dead; one per for-each |
| `HashMap` | 174,903 | |
| `ArrayList` | 160,235 | |
| `Object[]` | 577,239 | 88.4% of them a container's backing store |
| `int[]` | 185,860 | 94.1% HashMap's slot metadata |

**The iterators outnumber the containers they walk, 337,322 against 335,138.** That is the
single largest object population in the program, it exists only to carry an index, and
every one of them is allocated, marked and swept. Storage is where the BYTES are; iterators
are where the OBJECTS are. A strategy that only moves backing arrays leaves the larger half.

### The contract

A container opts in by exposing three things in C, and nothing else:

    blocks      0..n length-prefixed C blocks (cn1RefBlockAlloc), one kind tag each
    cursor      int first(c)          -> first valid index, or -1
                int next(c, int i)    -> next valid index after i, or -1
    element     JAVA_OBJECT at(c, int i)

That is enough to express every container whose iteration order is an index walk, which is
all of the ones that matter: ArrayList and Vector (0..size-1), HashMap, Hashtable,
IdentityHashMap and LinkedHashMap (skip empty and tombstone), HashSet and LinkedHashSet
(delegate), ArrayDeque (wrap the circular buffer). It is NOT enough for LinkedList or
TreeMap, whose order is a pointer chain -- those keep the Iterator they have, and that is
the honest boundary of the mechanism rather than a gap to paper over.

### One registry, four consumers

The translator gets a single table -- class -> {block fields, cursor natives, element kind}
-- and that one table drives everything that is currently bespoke:

    __GC_MARK_        mark each reference block   (today: NATIVE_REF_BLOCKS, 2 entries)
    __FINALIZER_      free the blocks             (today: NATIVE_BLOCK_FREE, 3 entries)
    for-each          Iterator protocol -> cursor (today: ArrayList only, 71 sites)
    forEach(lambda)   the same walk, body inlined (today: nothing)

The for-each lowering stops being ArrayList-specific and becomes container-agnostic,
because the shape it emits is the same for all of them:

    for(int i = first(c); i >= 0; i = next(c, i)) { E e = at(c, i); <body> }

The lambda case is the same loop with one more step. A lambda is created AT the call site,
so its concrete class is statically known -- the translator already emits them as real
classes (`Parser_lambda_0` and friends are in the emitted C) -- which means `accept` is
monomorphic by construction and needs no receiver analysis at all. `coll.forEach(x -> ...)`
therefore lowers to the loop above with a direct call in the body, which ThinLTO inlines to
nothing. Zero allocation, zero dispatch, running straight over the block.

### Why this is the whole win rather than three partial ones

The measured pieces compose. Block storage took HashMap's page heap down 38% (5 of 5
rounds) with per-cycle mark -12% and sweep -22%, because 524,709 array objects left the
heap entirely -- the bytes moved to malloc, which is why process footprint did not move and
why measuring footprint was the wrong axis. The cursor protocol removes 337,322 iterator
allocations on top of that, and it is the same 337,322 objects the mark and sweep walk. The
lambda lowering removes the last interface dispatch from the loop body.

None of the three needs the collector to move objects, and none of them needs a general
escape analysis. They need one thing the closed world gives for free: the concrete class of
a container and of a lambda, known at translate time.

### Order, and the one precondition

1. Generalize the two hard-coded tables into the registry, with ArrayList and HashMap as
   its first two entries and the existing behaviour unchanged. Pure refactor, gated by the
   existing byte-identity suites.
2. Cursor natives per container; retarget the for-each lowering onto them. This is where
   the 337,322 iterators go.
3. `forEach`/`removeIf`/`replaceAll` lowering onto the same walk.
4. The remaining containers' storage, in descending census order.

**The precondition is the collector, not the design.** The self-hosted translator loses
live objects roughly 1 run in 16 under memory pressure because the application thread is
never handshaked (Round 19), and every step above puts MORE live references into storage
the collector reaches only through a generated mark hook. `CN1_GC_FAULT=halfblock` and
MapTorture2 now prove that hook is walked; they cannot prove the collector around it is
sound. Step 1 is safe to do regardless -- it moves no references -- but steps 2-4 should
follow the handshake fix.

### CORRECTION to Round 20: that iterator count was STALE and its conclusion is withdrawn

Round 20 above opens with "ArrayList.ArrayListIterator 337,322 live objects" and concludes
"the iterators outnumber the containers they walk, 337,322 against 335,138". **Both are
wrong on the tree they were written against.** The figure came from a census log taken
earlier in the session and was quoted without re-measuring.

Re-run on the current HEAD, same corpus, same instrument:

| | quoted | actual |
|---|---:|---:|
| `ArrayList.ArrayListIterator` | 337,322 / 15.44MB | **149,776 / 6.86MB** |
| `ArrayList` | 160,235 | 109,623 |
| `HashMap` | 174,903 | 172,996 |

Iterators are **down 56%**, and they do NOT outnumber the containers -- 149,776 against
282,619. Two changes since that census account for it: the for-each lowering (Round 13)
replaced the Iterator with an indexed walk at 71 sites, and the aging slack default going
to 0 (Round 15) reclaims the dead ones a cycle earlier -- the old census showed this class
at 67% dead, so most of the apparent population was garbage awaiting collection rather than
live iterators.

**The design in Round 20 does not depend on that number and stands; the case made FOR it
does not.** Iterators are no longer the largest object population and the cursor protocol
should not be sold as if they were. What is actually left is narrower and worth stating
exactly: the lowering fires on 71 of ~295 for-each sites, and the ~224 it refuses are
receivers the analysis cannot prove -- 51 parameters, ~60 method returns, 23 polymorphic
fields. Those are the remaining iterator allocations, and widening the proof (transitive
type propagation) is a different piece of work from the cursor protocol.

The lesson is the one this file keeps recording: a number re-quoted from an earlier run in
the same session is not a measurement. Three separate claims in this session have now been
wrong that way -- a stale subject binary, a non-LTO benchmark, and this.

## Round 21: zeroing the overhead -- an iterator and a stateless lambda are not objects

The Round 20 case was argued from a stale iterator count and withdrawn. This is the same
target argued from allocation VOLUME, which is the metric that actually describes overhead,
measured on the current tree.

    total allocated over the run    7,627,446 objects, 708.7MB
    ArrayList.ArrayListIterator       724,829 objects  (9.50% of all objects), 27.65MB (3.90%)
    lambdas                                 0          (this corpus is Java-8 era)

**Nearly one allocation in ten is an iterator**, and that is AFTER the for-each lowering
removed 71 of ~295 sites. It is small in bytes and large in events, and events are what
drive the collection trigger, the mark and the sweep. The right way to read the earlier
"6.86MB live" figure is that iterators are churned, not retained -- which is exactly the
shape that costs GC throughput rather than footprint.

An iterator is a parent pointer and an index. A stateless lambda is nothing at all. Neither
needs a heap object, and neither should cost one even at a site the indexed lowering
refuses.

### Three mechanisms, in increasing order of what they need to prove

**1. A non-capturing lambda is a singleton. This needs no analysis whatsoever.**
A lambda is emitted here as a real class whose captured values are its instance fields,
built by a `lambda$factory` that does `CN1_FAST_NEW` + `<init>` on every evaluation.
Measured on this corpus: **4 of 6 lambda classes have ZERO captured fields.** A class with
no instance fields has no distinguishable instances, so one static instance per class is
indistinguishable from a fresh one -- return it from the factory and the allocation is gone
permanently. The test is "does the class declare instance fields", which the translator
already knows; there is no escape analysis and no receiver analysis.

**2. An iterator is a stack object.** `resolveConcreteIteratorType` ALREADY proves the
thing this needs: it only answers when `iterator()`'s whole body is `return new T(...)`,
via `allocatedReturnType()`. So the allocation can be hoisted to the call site -- replacing
`INVOKEINTERFACE iterator()` with `NEW T; DUP; <coll>; INVOKESPECIAL T.<init>` -- and the
iterator local then only ever feeds calls to T's own methods. If those do not store `this`,
it does not escape the loop and can be emitted as a C stack object, which is machinery this
VM already has and uses for StringBuilder (`struct obj__java_lang_StringBuilder __cn1stk_6`
appears in the emitted C). That removes the 724,829 allocations without needing the
receiver to be ArrayList specifically -- only for `iterator()` to resolve.

**3. A capturing lambda is a stack object** wherever the factory result feeds a call that
does not store it, which is the `forEach`/`removeIf`/`sort` shape. Same stack-allocation
mechanism as (2), same escape question.

### The one thing that multiplies all of it

(2) and (3) are gated by the same proof the indexed lowering is gated by: the concrete type
of the receiver. That fires on 71 of ~295 for-each sites, and the ~224 refusals are 51
parameters, ~60 method returns and 23 polymorphic fields. **Transitive type propagation
through parameters and returns is therefore the single change that multiplies every
downstream optimization** -- the indexed walk, the stack iterator, and the lambda inlining
all widen with it, and none of them widens without it.

(1) is gated by nothing and should go first for that reason: it is the only one of the
three that is pure profit with no analysis and no GC interaction.

### Order

1. Non-capturing lambdas -> static singletons. No proof required.
2. Transitive concrete-type propagation for container receivers. The multiplier.
3. Iterator allocation hoisted and stack-allocated. Removes ~9.5% of all allocations.
4. Capturing lambdas stack-allocated at non-escaping call sites.
5. The Round 20 registry, which is now a tidying of 1-4 rather than the headline.

The collector precondition from Round 20 still stands for anything that moves references
into block storage; it does NOT apply to 1, 2 or 3, none of which change where a reference
lives -- they change whether an object is allocated at all.

## Round 22: two of the four landed, and the third measured zero

Round 21 listed four items. Two are done and measured; the third was built and reverted
for buying nothing; the fourth is bounded by an analysis neither of them needed.

### 1. Non-capturing lambdas are singletons -- DONE

A lambda is emitted as a class whose captures are instance fields, built by a synthesized
`lambda$factory` doing `CN1_FAST_NEW` on every evaluation. With no captures the class has
no instance fields, so instances are indistinguishable and one shared instance serves.
Measured on `LambdaT`, where capturing and non-capturing lambdas sit in the SAME loops:

| lambda | captures | allocations over the run |
|---|---|---:|
| lambda_0/1/4/5 | none | **1 each** |
| lambda_2 | 1 | 597 |
| lambda_3 | 1 | 384 |

~2,000 allocations become 4. Costs no analysis: "declares no instance fields" is known
where the class is synthesised. The JLS does not guarantee a lambda yields a new object and
the JDK caches non-capturing instances itself, so identity is not being broken -- and
`LambdaT` deliberately does not assert identity, because that would test the host.

### 2. A getter's return is its field -- DONE

`Invoke.asInlinableFieldAccess` already decides whether a call resolves monomorphically to
`return this.f` and hands back the Field. Asking it in the receiver rule made `x.getFoo()`
exactly as provable as the field behind it:

    for-each sites lowered      71 -> 95        (+34%)
    iterator allocations   724,829 -> 522,140   (-28%)
    all allocations      7,627,446 -> 7,403,935 (-2.9%)

### 3. Transitive field forwarding -- BUILT, MEASURED ZERO, REVERTED

`this.items = other.items` is as provable as the field on the right, which a single pass
cannot say. A fixpoint over the store map resolved 3 more fields and **zero more lowered
sites** -- the 3 were HashSet and TreeSet, which the ArrayList-only lowering skips anyway.
The hypothesis that the 57 refused fields were mostly forwarded stores is refuted: they are
stored from parameters and from values this analysis does not follow. Reverted rather than
kept on "it will help once other containers are supported", which is the justification this
file exists to refuse.

### Where the remaining refusals are, measured

A `-Dcn1.framelessCensus=true` census over every for-each site, by receiver shape:

| receiver | sites | resolved? |
|---|---:|---|
| field | 73 | ArrayList |
| field | 57 | **REFUSED** |
| parameter | 52 | **REFUSED** |
| local | 22 | **REFUSED** |
| `getMethods`/`getInstructions`/`getFields` | 24 | ArrayList |
| local | 12 | ArrayList |
| `entrySet()` | 21 | **REFUSED** -- a fresh view object per call, no field to resolve |
| field | 9 | HashSet / TreeSet |

**Parameters (52) are now the largest tractable group.** Proving one needs call-site
propagation -- every caller passing a provably-identical concrete class -- which needs the
argument-to-parameter mapping, and that needs operand-stack simulation to know which
instruction produced argument i. That is a materially bigger piece of work than items 1-3,
and it is the honest reason it is not in this round.

### 4. Stack-allocating the iterator -- SUBSUMED where it would have applied

Round 21 proposed hoisting the `iterator()` allocation and stack-allocating it. The census
above shows why that is not the next move: wherever the receiver's class is provable the
INDEXED lowering already fires and removes the iterator entirely, which is strictly better
than stack-allocating one. Stack allocation would only help sites that resolve but are
refused on loop SHAPE -- `it.remove()` in the body, a non-canonical loop -- and those are a
handful. The remaining 522,140 iterator allocations are at sites where the type is not
proven, so they need item 2 widened, not a different allocation strategy.

## Round 23: the iterator universe is closed and ours -- survey, and the mechanism

The remaining 522,140 iterator allocations are at for-each sites where the RECEIVER's type
cannot be proven, so widening the receiver proof reaches them only one shape at a time.
There is a better lever, and it does not need the receiver at all: the iterators are our
classes. Surveyed rather than assumed.

### Every Iterator implementation in the closed world

    real implementations of hasNext()Z      34
      ours (java.util.*)                    33
      foreign                                1   org.objectweb.asm.tree.InsnList.InsnListIterator

**One class in a 37.6k-line program is not ours**, and it is itself a plain cursor. Their
shapes, from the emitted structs:

| iterator | own state |
|---|---|
| `ArrayListIterator` | 3 ints + parent |
| `SimpleListIterator` | 3 ints + parent |
| `ArrayDequeIterator` | 2 ints + boolean + parent |
| `HashMap.AbstractMapIterator` (+3 subclasses) | 3 ints + map ref |
| `Hashtable.HashIterator` | 3 ints + boolean + 2 refs |
| `IdentityHashMapIterator` | 4 ints + boolean + 2 refs |
| TreeMap's 11 | no own fields, all inherited |

Every one is a parent reference plus a handful of primitives. The largest,
`Hashtable.HashEnumIterator`, is ~56 bytes including the 16-byte header. **A single uniform
bound covers all of them**, which is what makes a special case possible: the translator
does not need to know WHICH iterator a call returns, only that it is one of a known set
that is small, and that the loop does not let it escape.

### The mechanism this enables

At a canonical for-each whose iterator local is provably non-escaping -- exactly the check
the indexed lowering already performs, the slot being read only by hasNext() and next() --
the translator emits a C stack buffer of the uniform size and hands it to the thread:

    char __cn1iter_N[CN1_MAX_ITER_SIZE];
    cn1IterScopeBegin(threadStateData, __cn1iter_N);   // one-shot
    ... coll.iterator() ...                            // first iterator-class NEW takes it
    ... loop ...

The allocator consumes the pending buffer on the first allocation of a class flagged as an
iterator, exactly once, and constructs there with the header shape the existing stack-object
path already uses (`__cn1stk_` for StringBuilder). The one-shot is what keeps it safe when
an iterator wraps another: the outer takes the buffer, anything allocated later in the body
goes to the heap as usual.

**It needs no receiver type at all**, which is precisely why it reaches the 522,140 that
widening the proof does not.

### Why it is NOT being built yet, and this is a technical reason rather than a hedge

A stack-allocated iterator is reachable to the collector ONLY through the conservative scan
of the native stack. That is the same mechanism Round 19 implicated in the live-object loss:
the application thread is never handshaked, so its roots come from a single SIGUSR2 capture
per cycle and the correctness of everything it holds rests on that capture plus SATB. Adding
a class of objects that exist ONLY on that stack increases the VM's dependence on the exact
mechanism that is currently losing objects roughly 1 run in 16 under memory pressure.

The ordering is therefore: fix the handshake, then build this. Doing it in the other order
means any new corruption is unattributable between the two, and this VM has already shown
that a gate suite can be entirely green while live objects are being freed.

## Round 24: QA first -- the verifier could not see a dangling reference, and the gauntlet's answer depended on $PATH

Round 23 ends by saying the handshake bug must be fixed before the stack-iterator mechanism
is built, because otherwise new corruption is unattributable. That ordering assumed the
instruments work. Two of them did not, and both failures were invisible in exactly the way
that matters: each reported success.

### The verifier was structurally blind to its own headline invariant

`cn1GcVerifyClassify` has had a `CN1_GC_VS_FREE_SLOT` branch since it was written, and it
had never fired. The sweep poisoned a reclaimed slot and immediately pushed it onto the
page free list, so the next allocation handed the slot back; by the time the verify pass
ran, a stale reference into it resolved to a valid live object. Measured before the change:
60 verifier runs under memory pressure produced 2 crashes and `violations=0`. Only legacy
blocks were ever quarantined, and nearly every object is in a BiBOP slot.

Under `CN1_GC_VERIFY` a reclaimed slot now takes a `QUAR` mark and is withheld from the
free list until the next sweep. One cycle suffices: the verify pass runs once per cycle,
immediately after the sweep. `freeCount` covers a quarantined slot the whole time, so page
liveness accounting is unchanged, and an ordinary build is untouched.

**Two fault designs failed before one worked, and the failures are the useful part:**

| fault | result | why |
|---|---|---|
| drop 1 mark in N, in `gcMarkObject` | `violations=0` down to 1-in-50 (2699 drops) | one dropped *visit* is not a missed object -- a second referrer marks it anyway, and the sweep needs two consecutive missed cycles |
| 8 sticky victims, never marked again | `violations=0`; all 8 ended at `mark=currentGcMarkValue` | the marker has redundant paths: conservative stack roots, and the belt pass exists specifically to re-mark reachable-but-unmarked objects |
| free 8 slots the sweep proved live, victims chosen **in the sweep** | fault fired 8 times, 239 verify passes, 161k refs, `violations=0` | a victim picked there is usually referenced only from a stack slot, which this instrument cannot see by construction |
| free 8 proven-live slots, victims nominated **in `gcMarkObject`** | caught | anything arriving there is provably a reference-field child of some holder |

The lesson generalises past this fault: **an instrument that only reads object graphs can
only be tested with defects that live in object graphs.** Three of the four attempts above
injected real corruption and reported nothing, and only the victim-fate dump
(`classify=`/`mark=` per freed slot) distinguished "the verifier missed it" from "there was
nothing to miss".

`CN1_GC_FAULT=freelive` is the survivor, `MapTorture2` is the driver because it holds 4000
entries across forced collections, and self-test5 asserts the corruption is *never* missed
while separately asserting the fault fired. Measured, 6 runs each:

| | verifier caught | driver caught | **missed** |
|---|---:|---:|---:|
| quarantine on | 4 | 2 | **0** |
| quarantine off (`-DCN1_GC_NO_QUARANTINE`) | 0 | 1 | **5** |

The ablation is kept as the permanent negative control. Which of the two notices first is a
race, so the gate does not require the verifier specifically -- it requires that nothing
goes quiet.

### The gauntlet's verdict depended on which JDK was on $PATH

JDK-4511638 replaced FloatingDecimal with the shortest-representation algorithm in Java 19.
One unchanged `BoxEdge`, measured on this machine:

| | double (2^62) | float (2^31) |
|---|---|---|
| JDK 25 / 21 / 19 | `4.611686018427388E18` | `2.1474836E9` |
| JDK 17 / 11 / 8 | `4.6116860184273879E18` | `2.14748365E9` |
| ParparVM (`cn1ShortestDouble`) | `4.611686018427388E18` | `2.1474836E9` |

`REF_JAVA` defaulted to `java`. Sourcing `tools/env.sh` -- which every other instruction in
this tree tells you to do -- puts JDK 8 on `PATH`, so the gate reported `BoxEdge: DIVERGE`
on a tree with no bug in it. The dangerous half is the repair that reading suggests: making
the VM emit the pre-19 form would break it against every JDK anyone still ships. This is the
same class as the `parpar-O3` staleness trap -- the harness quietly chose what it compared,
and the wrong answer looked like a code regression.

The gauntlet now refuses a pre-19 reference with an explanation, and checks each torture
against **both** references: byte-identical to the modern JDK (the shipping contract), and
equal to Java 8 on every line where the two JDKs agree with each other. The era-dependent
lines are derived by diffing the two references rather than hand-listed, so the set cannot
go stale, and a real divergence landing on a floating-point line is still caught.

Result: 16 tortures identical to both JDKs; `BoxEdge` identical to the modern one with
exactly 4 era-dependent lines (plus and minus 2^62 as a double, plus and minus 2^31 as a
float). The target's diff against Java 8 is line-for-line the same as the JDKs' own diff.

### State after this round

GC-VERIFY GREEN with five non-vacuous self-tests; GAUNTLET GREEN dual-era; Gate D PASS,
Gate A PASS (798 files byte-identical), negative control PASS.

Round 23's ordering still stands, with one correction: the handshake bug is next, and it is
now worth attacking with an instrument that can actually observe a freed-but-referenced
slot. What the quarantine does *not* yet do is survive a slot being reused across more than
one cycle, so a reference that dangles for several cycles before being read is still seen
only at the first verify pass after the free.

## Round 25: the iterator moves to the caller's stack frame -- 28% of them, and no measurable time

Round 23 designed this and deferred it behind the handshake bug. It is now built, and the
design survived contact with one significant correction.

### What was built

A for-each offers a C stack buffer; whatever `iterator()` allocates takes it. Neither side
learns anything about the other, which is exactly why it reaches the sites a receiver proof
cannot -- the 522,140 allocations Round 23 counted at for-each sites whose receiver type is
not provable.

Three separate proofs, all COMPUTED by `IteratorEscape` rather than listed:

1. `this` escapes no method of the iterator class, constructor included;
2. every allocation site of that class in the closed world lets the object escape only by
   returning it;
3. the for-each slot is read at `hasNext()` and `next()` and nowhere else -- the same proof
   the indexed lowering needs, now factored into `validateForEach` and shared.

On the self-hosting corpus: 40 Iterator implementations, 37 `this`-safe, 56 allocation
sites, 44 safe, **25 classes eligible**. The analysis pays for itself on the refusals, none
of which are obvious by inspection and all of which would have been dangling pointers:
`LinkedList.removeFirstOccurrenceImpl` passes its iterator to a helper,
`Hashtable.__CLINIT__` stores two in statics, and `Collections$SingletonMap$1$1.next()`
returns `this` as the entry.

### Measured

| | iterator allocations | change |
|---|---:|---:|
| ForEachT, mechanism off | 561 | |
| ForEachT, mechanism on | 93 | **-83%** |
| self-hosting corpus, off | 526,199 | |
| self-hosting corpus, on | 377,123 | **-28%** |

The 526,199 independently confirms Round 23's 522,140. Both arms emit **byte-identical C**
-- the only file that differs is `CMakeLists.txt`, which embeds its own output path.

**Wall clock and footprint did not move measurably.** Ten interleaved rounds, and the
machine was not quiet (another checkout was running `core-unittests`, load 10-13 throughout,
which is why this is reported as a null result rather than a regression):

    wall   min-of-10: on 0.962s  off 0.988s   sign test: on faster in 5 of 10 pairs
    peak   max-of-10: on 869MB   off 897MB    sign test: on smaller in 7 of 10 pairs

3.1% on a metric this file calls noise below ~5%, and a 5/10 sign test is the definition of
no effect. The allocation reduction is real and counted; the throughput win it was supposed
to produce is not there yet. The honest reading is that 149k allocations out of 779 MILLION
total objects is 0.02% of allocation traffic -- the iterator count was never the bottleneck
it looked like when measured as a fraction of *iterators*.

### The correction that cost the first measurement

The hook was placed in `__NEW_X`, which is where a Java-level reading says allocation
happens. It is not: the generated code calls `CN1_FAST_NEW(...)`, which inlines the BiBOP
bump path and reaches `__NEW_X` only when a page is full. The first build measured 460
ArrayListIterator allocations with the mechanism on and 460 with it off -- a perfectly
working mechanism attached to a path that never runs. `CN1_ITER_NEW` replaced it.

Related trap, twice in one session: `translate-and-build.sh` rebuilds the translator only
when its main class file is **missing**, so editing a translator source and running the
script measures the previous translator. Both times the symptom was "the change does
nothing", which is also what a real null result looks like.

### Remaining headroom

**123 of 207 surviving for-each sites are scoped; 84 are refused** by `validateForEach` --
they are not the canonical javac shape, or the slot outlives the loop (`it.remove()` in the
body is the common one). All 377,123 residual allocations are `ArrayListIterator`, so they
are concentrated: these are sites whose receiver is an ArrayList at runtime but not
provably so at translate time, which is precisely the population this mechanism was built
for and has not yet reached. A census counter now reports the scoped/refused split so the
next widening is aimed at a measured shape.

Not yet measured: whether the win shows up on a quiet machine, and whether it shows up at
all on a workload whose iterators are a larger share of allocation than 0.02%.

## Round 26: past HotSpot on both axes, and the instrument that got us there

**Headline, self-hosting corpus, two independent interleaved 5-round runs
(macOS arm64, load ~3, min-of-N wall, max-of-N peak phys_footprint, arms
byte-identical in emitted C):**

    run 1   vs jdk25: elapsed 0.843x   peak 0.599x     vs jdk8: 0.654x / 0.793x
    run 2   vs jdk25: elapsed 0.824x   peak 0.590x     vs jdk8: 0.632x / 0.771x
    run 3   vs jdk25: elapsed 0.759x   peak 0.587x     vs jdk8: 0.552x / 0.771x

Peak footprint is steady to 2% across all three (0.587-0.599); wall clock spreads
more (0.759-0.843) because the machine was not quiet, which is why the time
ceiling is set off the WORST of the three and not the best.

    parpar 0.795s / 421MB   jdk25 0.941s / 703MB   jdk8 1.218s / 531MB

ParparVM is roughly 18% faster and uses 41% less memory than JDK 25 here. The
FINAL table earlier in this file (1.35x time, 1.59x memory) is superseded.

### The instrument: vm/benchmarks/memshape.sh

Whole-program footprint cannot say WHICH shape pays, so nothing could be aimed.
memshape retains exactly n instances of one shape and stops; peak footprint is
then linear in n and the SLOPE is that shape's deep retained cost. Three points,
least squares, and it refuses to quote a shape whose residual exceeds 2%.
Identical driver and source on both VMs, and it measures no time at all, so it
cannot drift the way a benchmark does.

It sampled each point ONCE at first, while its numbers were quoted to 0.01B --
with three equally spaced points the slope is (y3-y1)/2h, so the middle point
cannot catch an endpoint error and the residual gate tolerated ~3B of slope error
on a fix worth 8. It now runs MEMSHAPE_REPS (default 3) per point, fits the
minima AND the maxima, and prints the band. Do not quote a number without it.

### Fix 1: every heap array over-allocated eight bytes

`allocArray` asked for `sizeof(header) + elements + sizeof(void*)` while the
payload has always ended at `sizeof(header) + elements`. Not a consequence of the
40 -> 32 header narrowing, which is what the first version of the comment claimed:
origin/master has a 40-byte header, `data` at 32, the payload at 40, and asks for
40 + n + 8. Master over-allocates the same eight bytes; the bug is older than the
narrowing and independent of it.

Eight bytes understates it, because BiBOP rounds to a size class:

    objArr0  56.84 -> 40.22  (-29%)     intArr32 201.65 -> 169.61  (-16%)
    charArr8 72.59 -> 56.45  (-22%)     objArr8  121.25 -> 104.94  (-13%)
    byteArr32/intArr8 88.8 -> 72.6      strA32/128 137/251 -> 121/221 (-12%)

Every one of those was PREDICTED before it was measured, 10 of 11 exactly; the
one miss was arithmetic on the class ladder (288 rounds to 320, there is no 288).

**What it removed, and what that broke.** The slack used to absorb small
fixed-width overruns. `spare = sizeclass(32+payload) - (32+payload)` is ZERO
whenever 32+payload lands on a class boundary -- char[8] (48), int[8] (64) and
Object[8] (96) all do. IOSNative.m's eight `nsDataToXArray` converters allocate
`[d length]/sizeof(elem)` elements and then `memcpy(..., d.length)`: for a length
that is not a whole number of elements that is up to esz-1 bytes past the payload,
7 for long/double, and it now lands in the next object's header. Found by an
adversarial review pass, not by any gate. All eight now copy
`length * sizeof(elem)`. Any future native that writes more than
`length * primitiveSize` bytes is a heap corruption rather than a scribble on
padding.

### Fix 2: String 48 -> 32 bytes

    offset    4 bytes, ALWAYS ZERO. The only ctor that set it non-zero was the
              package-private (int,int,char[]) aliasing form, and the emitted C
              for the whole 849-file corpus contained that symbol only in its own
              definition and declaration -- no call site anywhere. Deleted; javac
              enumerated all 20 Java uses, and 45 C sites plus 3 JS sites followed.
    nsString  8 bytes on EVERY String caching an NSString peer that only strings
              crossing into Objective-C ever acquire. Every read and write of it
              is already inside #if defined(__APPLE__) && defined(__OBJC__), so
              off-Apple it was eight bytes the binary could not even read. Now
              declared only on ObjC targets (ByteCodeClass.targetGuardFor); iOS is
              bit-for-bit unchanged.

Both or neither: 48 -> 40 still lands in the 48-byte class and buys nothing. The
first attempt removed only `offset`, predicted a 16-byte drop, and measured zero
-- because MemShape's strings are NOT fused (String(char[]) gets its byte[] back
from a call to toLatin1, so @Fused has no inline NEWARRAY to pack) and 40 still
rounds to 48. The model was wrong, not the change.

    strA8   104.90 -> 88.73   ratio 2.01 -> 1.71
    strA32  137.41 -> 105.21  ratio 1.81 -> 1.38
    strA128 251.17 -> 204.82  ratio 1.46 -> 1.19

On the corpus, where strings ARE fused: **112 -> 92 B/obj, an 18% cut on the
largest class in the heap** (472,561 live instances).

### The census was blind to 101MB, and the ranking was read off it

`cn1BlockAlloc` bumps `cn1NativeBlockLiveBytes` and never
`CN1_ALLOC_CENSUS_COUNT`, and `occupied` is exactly bibop + legacy. ArrayList,
HashMap and StringBuilder all keep their element storage in those malloc'd
blocks, so the per-class table could not see any of it:

    occupied 1,843,140 objects 169.00MB | bibop 132.78MB legacy 36.22MB
      | nativeBlocks 101.37MB (cumulative: refs 115.7MB of 168.7MB = 69%)

Live is 270MB, not 169MB. Two claims made off the old table were therefore
unsupportable and are withdrawn: "186,000 mostly-empty HashMaps" (96 B/obj is the
header slot for an empty and a full map alike) and "Object[] is not the mass"
(the reference storage was MOVED OUT of Object[] into exactly the region the
report omitted). **69% of native-block bytes are reference slots**, so 8 -> 4 byte
references would be worth ~35MB there alone -- and that surface is reached only
through cn1RefBlockGet/Set/copy/move plus the GC mark, not the 596 sites an
object-field change would touch.

The reference share is CUMULATIVE, not live, and deliberately so: remembering
each block's kind wants a field on CN1NativeBlock, and widening that header moves
every hash-table slice (cn1TableAlloc pins its layout to sizeof(CN1NativeBlock)
and a hardcoded 16-byte prefix). Measured: it segfaults during class parsing.

### The ratchet could never fire

perf-guard grepped for an arm named `jdk25` while bench-selfhost.py named arms
POSITIONALLY (`jdk-1`), and parsed `time Nx` while the bench prints `elapsed Nx`.
It reported FAIL on a run that was in fact 0.843x and 0.599x. Arms are now named
from the JDK's real feature version -- which also makes the failure it most needs
to expose collide loudly: with JDK_25_HOME unset both reference arms resolve to
the same JVM, and a positional scheme reports them as two different ones.
Ceilings moved from 2.00x/2.10x (which could not catch a doubling) to 0.95x/0.70x,
set from the two runs above.

### Coverage added

`Latin1T` (251 lines) walks every crossing of String's compact representation --
replace widening and narrowing, substring windows either side of the wide char,
all 49 concat pairs, and equality between the SAME text held in different coders.
Matches JDK 25 byte-for-byte. It also found that toUpperCase forks to NSString on
iOS and to an ASCII-only path everywhere else (Character.toUpperCase(int) maps
only a..z, its real body commented out), so "ÿ".toUpperCase() differs by
platform. Left out of the gauntlet rather than shipping a red gate; tracked
separately.

`SbLatin1T` (419 lines) does the same for StringBuilder and StringBuffer, whose
compact representation is a `wide` flag over one native buffer -- the same design
as OpenJDK's byte[]+coder, with no array-type duality. Measured working:
sbNarrow32 153.40 vs sbWide32 185.34, exactly one byte per unit for 32 units.

It found a silent wrong-text bug on its first run. `StringBuffer.append(char[])`
and `insert(int,char[])` were declared PACKAGE-PRIVATE. Overload resolution picks
the most specific APPLICABLE method, and a package-private one is not applicable
from outside java.lang -- so every caller silently bound to the Object overload
and appended "[C@1b6d3586" instead of the characters. No link error, no crash:
right in the simulator (a real JDK) and wrong on the device. This is a worse
failure mode than the missing-API case, because nothing fails at link.

### Leads killed by measurement rather than by effort

    [SB] StringBuilder NEW sites=497 stackAllocated=381 refusedByTryCatch=26
StringBuilder is 8.32MB of peak heap at 0% traced and the try/catch exclusion
looked like the cause. It is 5% of sites; 77% are already stack-allocated. Dead
for the cost of one counter, and the setjmp/longjmp risk was not taken.

HashMap.KeyIterator: 190,914 allocated, 100% dead at peak -- but 2% of the heap
and 1.9% of churn. NativeTraversal already intrinsifies 276 for-each sites with
no iterator allocation at all; the 60 refusals are loop-SHAPE refusals, not
receiver-type ones. Widening the matcher buys under 2%.

Size-class rounding waste, computed exactly over all 416 generated structs: mean
7.21 bytes (149 waste 0, 165 waste 8, 99 waste 16). Worth knowing before anyone
proposes a finer ladder; it needs instance weighting before it is worth acting on.

### Still open

The self-hosting corpus has ZERO stream call sites and 12 lambda classes of 422,
so the lambda/stream half of the pre-written-C idea has no measurement basis here
at all. StreamFusion matches only Stream.of(Object[]) and bails on any branch.
Either a representative app corpus gets wired into the census, or that work is
measured by counters (allocations and indirect calls eliminated) on a
microbenchmark -- never by wall clock.

## Round 27: a set that stopped renting a map, and a symbol that lied

### The waste, measured before it was fixed

Every `HashSet` was a `HashMap` with the set stored as every value
(`backingMap.put(object, this)`). So each set allocated a map object AND that map's
table allocated a full VALUES reference array holding one pointer, repeated. On the
HelloCodenameOne corpus:

    155,787 live HashSets against 366,554 live HashMaps

42% of every map in the heap existed only to back a set, and the collector was
marking ~2.5M reference slots that could only ever point at the set that owned them.

### The fix: the whole set in C

`HashSet` now owns an open-addressed table in the native heap -- one reference block
for elements, one int block for slot markers, **no values array** -- and every
operation is a single C call. It shares HashMap's probe kernel rather than copying
it: `cn1HmMarker` (including the String cached-hashCode fast path that skips the
virtual call) and `cn1HmNextSlot`. `LinkedHashSet` still delegates to a
LinkedHashMap, because it genuinely needs that map's ordering links, so `HashMap`
itself is untouched by this change.

**Why all of it had to be C, and not Java over NativeStorage.** The first attempt
kept the probe in Java. Every `NativeStorage.get/set` is a separate native call, so
a collection can land between any two of them -- and during a rebuild that meant the
marker traced a half-filled table while elements still living only in the old one
were reachable from nothing. Publishing the new table last fixes that specific
window, but the general shape is wrong: in C the whole operation has no safepoint
inside it. That is the same reason HashMap rebuilds through the single
`NativeStorage.rehash` native.

### Measured, by counters rather than by the noisy peak metric

    HashMap live    366,554 objs  33.56MB  ->  215,233 objs  19.71MB
    HashSet live    155,787 objs   4.75MB  ->  153,586 objs   9.37MB  (32 -> 64 B/obj)
    occupied      7,135,981 objs 653.65MB -> 6,945,411 objs 644.47MB
    nativeBlocks              274.57MB    ->             270.95MB

**-151,321 HashMap objects, which is essentially exactly the HashSet count** -- that
correspondence is the proof the mechanism is the claimed one and not a coincidence.
Net -190,570 live objects and ~12.8MB. The set itself grew 32 -> 64 bytes to carry
its own table, which is why the net is smaller than the gross.

Peak footprint is NOT quoted for this change and must not be: on this corpus it
carries 13-21% of run-to-run variance (same binary, same input: 1440/1543/1551/
1554/1568/1649MB over six standalone runs, against JDK 25's 1.6%), so a 12.8MB
result is an order of magnitude below what the metric can resolve. Object counts are
deterministic; that is what is reported.

### THE SYMBOL LIED, AND IT COST HOURS

The corpus SIGSEGV'd while a 582-line differential torture passed. lldb put the
crash in `ByteCodeTranslator.copy(InputStream, OutputStream, int)` -- a function
with no connection to sets. Four hypotheses were built and killed against that
false location: the GC rebuild window, a `toArray()`/`size()` disagreement, a
missing SATB deletion barrier, and a failing resource lookup.

**`-O3` plus ThinLTO folds identical functions**, and the linker had merged the real
culprit into `copy`'s symbol. Rebuilding at `-O1` without LTO gave the true stack on
the first try:

    cn1CollectionMap
    java_util_ArrayList_addAllNative
    java_util_ArrayList.<init>(Collection)
    BytecodeMethod.declarationOrderedLocals
    BytecodeMethod.appendMethodC
    ByteCodeClass.generateCCode

`cn1_collections.h` had a SECOND place assuming every HashSet has a backingMap
(`NativeTraversal` was the first, and was fixed early). `new ArrayList<>(aSet)`
recursed into `cn1CollectionMap` with a null owner, which dereferences
`owner->__codenameOneParentClsReference` on its first line.

Two rules earned here:

- **When a crash symbol stops making sense in an LTO build, rebuild at -O1 before
  forming a single hypothesis.** Every hour after the symbol stopped making sense
  was wasted.
- **The GC verifier had already answered it and was not believed**:
  `violations=0 earlyFreed=0 resurrected=0` over 28.7M references said plainly that
  nothing had been collected early, while three of the four hypotheses assumed
  something had.

### Fixed on the way, and worth keeping independently

`cn1CollectionMap` now returns 0 -- its own documented "not a layout I recognise"
answer -- for a null owner, instead of dereferencing it. A miss should never be a
segfault.

`copy()` and `copyRuntimeResource()` now name a null stream instead of crashing.
`Class.getResourceAsStream` returns null for a missing resource and several callers
piped it straight into `copy`, where ParparVM turns the null receiver into a SIGSEGV
with no Java stack and no message. It now says which resource and what
CN1_RESOURCE_PATH was.

### Coverage that did not exist

`SetTorture`, 582 lines, now in the gauntlet. HashSet and LinkedHashSet had **zero**
differential coverage before this -- none of MapTorture, MapTorture2, HtTorture or
IdmTorture mentions either. It covers forced collisions, tombstones, re-adding
through holes, growth across a resize with holes present, null elements,
`Iterator.remove`, the bulk operations, LinkedHashSet insertion order, and a
size/iterator/`toArray` agreement sweep across 200 sizes. Iteration order is sorted
before printing for HashSet (unspecified, so ours and the JDK's may legitimately
differ) and printed raw for LinkedHashSet (specified).

It did not catch the crash -- the failing path was `new ArrayList<>(aSet)`, which is
a collection CONSTRUCTOR reading the set through the native bulk-copy path, not any
set operation. Worth adding.


## Round 28: four page-heap knobs, all already at their best

Peak footprint against JDK 25 stood at ~1.21x after large native blocks moved to
mmap, and the heap report showed `resident 772MB` against `live 585MB` -- 187MB
dirty and dead. Four cheap knobs were A/B'd against that, interleaved, same source
with only the define changed. **None of them is worth taking**, and the reason is
the same in every case: what is left is not idle memory, it is PARTIALLY FILLED
pages, which a non-moving collector cannot reclaim without evacuating the survivors.

| knob | result |
|---|---|
| `CN1_BIBOP_FREE_POOL_KEEP` 64 -> 8, `MAJOR_SWEEP_CYCLES` 16 -> 4 | no gain, slightly worse |
| `CN1_BLOCK_MMAP_THRESHOLD` 32KB -> 8KB | indistinguishable |
| `CN1_BLOCK_MMAP_THRESHOLD` 32KB -> 128KB | worse in every round; confirms 32KB |
| `CN1_BIBOP_PAGE_SIZE` 64KB -> 32KB | +83.7MB on the median over 6 rounds |

Page release was verified to be FIRING before any of this, with
`CN1_LOG_PAGE_RELEASE=1`: five major sweeps released 720 pages (45MB), with
`rejected=0` and `releaseErrno=0`, so `MADV_FREE_REUSABLE` is being accepted and is
decrementing `phys_footprint`. There was never a stuck release path to fix.

### The page-size result is the one to remember, and not for its sign

Three interleaved rounds said 32KB pages were **11% BETTER**. Six rounds said they
are 6.1% worse, on both the median and the min. Nothing changed but the round count.

This machine was carrying someone else's GraalVM build at 436% CPU throughout, and
peak footprint is not independent of CPU contention the way it looks -- GC pacing
moves with available cores, and the peak moves with the pacing. Three rounds of a
64KB arm spanned 1285MB to 1442MB, which is wider than every effect measured here.

So: check `uptime` AND `ps` before believing a page-heap A/B, and do not conclude
from three rounds. The earlier version of this note would have recorded a 11%
improvement that does not exist.


## Round 29: the interface thunk becomes a switch, and what the bench can say about it

An interface call site cannot see its receiver when the receiver was stored
somewhere first, which is every `addActionListener(e -> ...)` in Codename One: the
lambda goes into a collection and `fireActionEvent` invokes it later through
`Iterator` and `ActionListener`. Call-site devirtualization is structurally blind
to that. The thunk is not -- in a closed world it sees every implementation of the
interface, and a lambda is just another class in the set.

So the thunk now leads with `switch(cn1__cls->classId)` whose arms are direct
calls, falling through to the original indirect dispatch for anything it did not
enumerate. 27 files, 1691 arms, emitted class set unchanged at 424 (the arms
travel over the include-only dependency channel from round 27, so naming a class
does not resurrect it from the cull). Gate A byte-identical at 855 files;
gauntlet and gc-verify green.

Two design mistakes worth keeping, because both were silent:

- **A void arm ended in `break`.** That leaves the switch and falls straight into
  the indirect dispatch below -- calling the method a SECOND time. Invisible for a
  query, state corruption for a mutator. MapTorture, SetTorture and IdmTorture
  diverged and Gate A reported 151 of 853 paths different.
- **Capping on cone size refused the interfaces that matter.** Collection (756
  implementors), Iterable (129), Iterator (116) and Comparable (52) all blew past
  a 24-implementor limit. But a wide cone is not a wide table: those 756 classes
  share a handful of `equals` bodies, so grouping the labels by target and capping
  on DISTINCT TARGETS brought Collection in at 10 switches over 380 labels.
  Iterator and Iterable are still out and genuinely are wide -- every iterator
  class really does have its own `next()`.

### What the benchmark could and could not measure

Wall clock was unusable: someone else's GraalVM job held 334% CPU throughout, and
`perf-guard` refused to gate at an elapsed spread of 107%. That refusal is the
harness working -- the same conditions produced a fictitious 11% in round 28.

Instructions retired is load independent, so the arms were A/B'd on that instead,
interleaved, switch-on against a `CN1_MAX_THUNK_CASES = 0` build of the same tree:

| round | switch | no switch |
|---|---:|---:|
| 1 | 14.53B | 14.04B |
| 2 | 14.53B | 14.31B |
| 3 | 14.63B | 14.49B |
| 4 | 14.62B | 14.39B |
| 5 | 11.71B | 13.91B |

**Min-of-N is the wrong statistic here and round 5 shows why.** Instructions
retired is near-deterministic for a single-threaded program, but this one runs
concurrent GC marker threads whose work varies with timing, so the count carries
real spread. Taking the min reports 0.842x -- a 16% win -- on the strength of one
outlier. Rounds 1-4 are flat to about 1.6% in the other direction. The honest
reading is that this benchmark cannot resolve the change.

There is also a structural reason to distrust it even when quiet: **the benchmark
is the translator, so analysis the translator performs is charged to the number
the optimization is meant to reduce.** Building the tables twice per method cost
enough to show; caching it is a separate commit. A shipped application pays that
cost once at build time and never again, so the self-hosting corpus is
systematically pessimistic about any optimization that thinks harder at translate
time. Round 22's devirtualization read as 19% slower for exactly this reason.

What is not in doubt: the binary got 24KB SMALLER with the switches in, the arms
are direct calls where five dependent loads used to be, and those direct calls are
inlinable where an indirect one never was.

## Round 30: a clean baseline at HEAD, on a machine that was actually idle

Round 29 could not score anything -- 107% elapsed spread against someone else's
334% CPU job. The same harness, same corpus, same binary shape, run at load 1.25
with nothing else on the box:

| arm | elapsed (min of 7) | spread | peak (min of 7) | spread |
|---|---:|---:|---:|---:|
| parpar | 5.372s | 3.1% | 1422MB | 5.7% |
| jdk25 | 4.557s | 7.6% | 1408MB | 4.3% |
| jdk8 | 5.650s | 12.8% | 2035MB | 2.6% |

**vs JDK 25: elapsed 1.158x, peak 1.024x. vs JDK 8: elapsed 0.918x, peak 0.720x.**

Two things worth separating, because they are in very different places.

**Memory is done.** 1.024x against JDK 25 is inside the run-to-run spread of either
arm -- the page heap, the 24-byte array header, the native-block mmap and the
collection slot sizes between them closed a gap that was 1.19x to 1.21x. There is
no longer a memory deficit to explain, and further work here is chasing noise.

**Time is 15.8% behind, and the cause is still parallelism, not codegen.** The
hardware counters from round 21 have not been invalidated by anything since:
parpar retires 0.619x the instructions JDK 25 does and burns 0.70x the cycles, yet
loses on wall clock, because HotSpot uses 3.49 cores to our 1.78. Roughly 31% of
every instruction HotSpot executes is JIT compilation on background threads, and
it buys the result 0.42s; parallel GC buys another 0.5s. Held to one compiler
thread it lands at parity with us while executing 15% MORE instructions.

So the remaining gap is not "the generated C is slow". It is that a short-running
AOT program presents one busy core where HotSpot presents three and a half, and
the only lever of that size left is doing our own remaining work concurrently.

The trajectory across this branch, all against JDK 25 on this corpus:

| | elapsed | peak |
|---|---:|---:|
| start | 1.278x | ~1.20x |
| after string/collection/iterator work | 1.214x | ~1.19x |
| after closed-world devirtualization | ~1.147x | -- |
| HEAD (thunk switch, lambda provenance, memory work) | 1.158x | 1.024x |

The 1.147x and 1.158x are the same number to this harness's resolution; do not read
a regression into it.

## Round 31: both paradoxes have the same answer, and it is the write barrier

Two things that "make no sense" were put to this branch: HotSpot holds the JIT, its
profile data and a far larger class library yet fits in the same bytes, and we
retire fewer instructions and burn fewer cycles yet take longer. Both were
measured this round, and they turn out to be the same mechanism seen twice.

### The mutator, not the collector, is the critical path

`sample` attributes per thread. Read across all threads the collector looks
dominant -- gcMarkObject 947, cn1ConservativeResolve 590, sweep 265 -- and that
reading is wrong. On the MAIN thread, self time:

| | share of main thread |
|---|---:|
| markDependent (the corpus's own dependency walk) | 57.1% |
| open(2) for the 853 output files | 11.4% |
| BytecodeMethod.equals | 10.9% |
| **all GC and allocation** | **9.5%** |

Marking really is on otherwise-idle cores. So the elapsed gap is not collector
work stealing mutator time, and no amount of making marking faster moves it.

### String is already ahead, and was the wrong suspect

The live census says java.lang.String is the largest class at 86-88 B/obj -- but
that is the WHOLE string, payload inline, because @Fused already packs it. Only
49K separate byte[] exist against 475K Strings. HotSpot spends 24 + 16 + payload
across two objects for the same content. There is no String deficit to close.
java.util.HashMap at 96 B/obj is the next target and has ~16 bytes of derivable
fields (cn1Cap, threshold) plus three native blocks that could be one.

### Peak is 42% collector headroom, and the trade is governed by the barrier

Live is ~828MB against a 1422MB peak. Pinning the trigger (CN1_GC_TRIGGER_MB,
3 rounds, quiet machine):

| trigger | elapsed | peak |
|---|---:|---:|
| default (adaptive) | 5.34-5.48s | 1448-1528MB |
| 48MB | 5.94-6.05s | ~1230MB |
| 24MB | 6.09-6.17s | ~1210MB |
| 12MB | 6.24-6.31s | ~1150-1270MB |
| 6MB | 6.50-6.75s | ~1190-1285MB |

**At trigger=12 we are 19% BELOW JDK 25 on peak** -- the memory target, available
today -- and 17% slower, which is not. The adaptive trigger has run away toward
speed and buys 1500MB where 1150MB would do.

Profiling the tight arm says the penalty is DIFFUSE, and that is the finding. GC
and lock share of the main thread rises only 5.5% -> 7.4% across a run that is 10%
longer; the rest is spread in thin slices over every mutator symbol. That is the
signature of the SATB write barrier being ACTIVE a far larger fraction of the
time, not of any one routine getting slower.

And the barrier is expensive by construction: `cn1SatbEnqueue` takes a GLOBAL
`pthread_mutex_lock(&gcSatbMutex)` **per object**. The range form already filters
lock-free and flushes 256 at a time, but the scalar form -- the one every ordinary
reference store goes through -- serializes on one mutex against the marker threads
draining the same log. Under tight pacing the contention is visible directly:
_pthread_mutex_firstfit_lock_wait, __psynch_mutexwait and lock_slow together rise
+1.3 points while cn1SatbEnqueueRangeLocked rises +0.4.

So the memory win is not blocked by pacing policy. It is blocked by a barrier too
expensive to leave on, which is exactly the lever HotSpot pushes and we do not:
G1 gives every thread its OWN SATB buffer and touches shared state only to hand a
full one off. Per-thread buffers make cycles cheap enough to afford, which buys
the 15-19% peak reduction that is already sitting there.

The correctness constraint is the whole design problem: marking terminates on a
fixpoint over the global log, so a thread-local buffer that is never flushed is a
reference the mark never sees and an object swept while live. The flush therefore
has to hang off the points where a thread is already known stopped -- the
handshake park and the uncooperative force-stop -- not off a hope that every
thread reaches a safepoint. gc-verify's self-test4 (injected half-traced block) is
the check that this is right.

## Round 32: ArrayList inline storage is measured to LOSE, and where the 20% actually is

Cumulative allocation over the corpus, 414MB total (CN1_GC_CONFORM + CN1_ALLOC_CENSUS):

| class | MB | count | avg |
|---|---:|---:|---:|
| byte[] | 97.5 | 647,383 | 150 |
| java.lang.String | 61.8 | 1,120,857 | 55 |
| asm Value[] | 32.2 | 208,346 | 154 |
| StringBuilder | 19.8 | 308,637 | 64 |
| HashMap | 16.8 | 190,793 | 88 |
| ArrayList | 15.5 | 483,251 | 32 |

Frame count equals Value[] count exactly (208,346) and LabelNode equals Label
(97,104 vs 97,109): one Value[] per analyzer Frame, one LabelNode per Label. Both
are ASM's tree and analysis machinery, driven by our two Analyzer passes.

### Inline storage for ArrayList does not pay, and [CAPHIST] says why

[CAPHIST] already existed under CN1_ALLOC_CENSUS. Reference blocks by element
capacity, stable across two runs:

| capacity | blocks | MB |
|---|---:|---:|
| <=1 | 146,382 | 5.6 |
| <=2 | 8,111 | 0.4 |
| <=4 | 22,283 | 1.3 |
| <=8 | 24,952 | 1.8 |
| **<=16** | **242,595** | **30.0** |
| <=32 .. >1024 | ~35,300 | 32.3 |

The dominant population is the <=16 bucket -- ArrayList's DEFAULT_CAPACITY of 10
on first growth. Those need a real block whatever we do, and inline slots are paid
for by EVERY instance:

| inline slots | cost (483,251 instances) | blocks removed | net |
|---|---:|---:|---:|
| 2 (+16B) | 7.7MB | 154,493 = 6.0MB | **+1.7MB worse** |
| 4 (+32B) | 15.5MB | 176,776 = 7.3MB | **+8.2MB worse** |

So the idea is refuted for this workload, and refuted for the reason that makes it
attractive elsewhere: it wins only when most instances stay tiny, and here most
grow to 10. StringBuilder's 16 inline bytes pay precisely because most builders
never exceed them. NOT BUILT.

### A claim of mine to retract

"byte[] + String = 38% of allocation, so substring fusion is the big prize" was
wrong, by lumping two unlike things. The byte[] SIZE histogram separates them:
the small modes -- 168,021 at 40 bytes (24-byte array header + 16 payload), 47,706
at 56, 29,956 at 32, 21,357 at 104 -- come to ~12.6MB. That is the substring
fallback (String.java:312, when cn1SubstringFused declines), and it is 3% of
allocation, not 38%. The remaining ~85MB of byte[] is large buffers: reading 5,326
class files and writing 853 outputs. That traffic is inherent to the job.

There is no single 20% item in the allocation profile. It is 414MB spread thin.

### Where the 20% actually is: the representation, not any one site

org.objectweb.asm.Label, measured at 104 B/obj, is the clearest case because it is
nothing but pointers -- 7 reference fields (info, otherLineNumbers,
forwardReferences, frame, nextBasicBlock, outgoingEdges, nextListElement), 6 shorts
and 2 ints:

| | ours | HotSpot |
|---|---:|---:|
| header | 16 (clazz* 8 + mark 4 + heapPosition 4) | 12 (mark 8 + compressed klass 4) |
| 7 references | 56 | 28 (compressed oops) |
| total | ~104 | ~56 |

**1.85x on the same object.** That is the whole answer to "HotSpot holds the JIT,
its profile data and the entire JDK yet fits in the same bytes": not better
algorithms, a denser representation. And it is a multiplier over the whole heap
rather than one allocation site, which is why nothing in the per-class table can
match it.

Two steps, in increasing order of blast radius:

  - **Header 16 -> 12.** Replace the 8-byte `struct clazz*` with a 4-byte class
    index. We already assign dense class ids and the thunk switch already reads
    `cn1__cls->classId`; CN1_CLASS_OF becomes an indexed load into a table small
    enough to stay hot. 4 bytes off EVERY object.
  - **References 8 -> 4.** Needs every BiBOP object inside one reserved window so a
    reference can be a 32-bit offset. The page heap already takes its pages from
    mmap (round 26), which is the half of that this needs. Legacy-heap objects
    (>512 bytes) fall outside and need a second encoding, and every iOS port native
    that reads a reference field out of a struct obj__X changes. This is the large
    one and it should be prototyped behind a measurement, not begun on the analogy.

Also real, and much smaller: CN1NativeBlock is a 32-byte header (next 8,
allocation 8, bytes 8, capacity 4, aligned 16) in front of every collection block,
and there are ~653,000 of them -- 21MB, which for the 146,382 capacity-1 blocks is
32 bytes of header on 8 bytes of data. `allocation` is derivable from the published
pointer (both are 16-byte aligned by construction) and `next` only matters while
the block is retired, when its payload is already dead and can hold the link. That
is 32 -> 16 and about 10MB, 2.5%.

## Round 33: the byte[] population was a miscount, and round 32's premise is retracted

Round 32 read the [ALLOCPROF] table as 647,383 byte[] against 1,120,857 Strings
and concluded that most Strings still allocate a separate array. **That is false.**
cn1FusedLatin1Begin was charging each fused block to TWO census rows -- String for
the fields and byte[] for the payload -- while cn1AllocFused and cn1SubstringFused
charge the whole block to String. The great majority of those byte[] rows were
fused payloads counted a second time.

The tree had already found this exact bug once. cn1SubstringFused carries the
note that an earlier version "split the bytes and added a second COUNT for the
array class so the census would read like the two-object path, which silently
cancelled the very saving this native exists for". The fix never reached the
other copy, and nothing compared the two.

### What found it: an allocation-SITE profiler

CN1_ALLOC_SITES keys a table on the RETURN ADDRESS of the allocation call, so no
call site is touched and generated C is covered like the runtime's own natives.
It reported 33,054 byte[] from allocArray against a census total of 647,383, with
overflow=0. A 95% shortfall in an instrument that says it dropped nothing is not a
coverage hole; it is two instruments disagreeing about what an allocation IS.

Caveat worth keeping: under -O3 -flto=thin an inlined call has no frame, so the
address names the outermost function that was NOT inlined. The report prints the
raw address beside the symbol so `atos -o <binary> <addr>` can resolve the chain.

### The corrected profile, fused blocks counted once

| class | MB | count | avg |
|---|---:|---:|---:|
| java.lang.String | 99.6 | 1,121,715 | 88 |
| byte[] | 61.2 | 42,005 | 1455 |
| asm Value[] | 32.2 | 208,346 | 154 |
| StringBuilder | 19.8 | 308,607 | 64 |
| HashMap | 16.9 | 191,514 | 88 |
| ArrayList | 15.5 | 483,247 | 32 |

byte[] is 42k LARGE buffers -- 5,326 class files read, 853 outputs written --
averaging 1455 bytes, not 647k small ones. The "168,021 arrays of 16 bytes" that
three separate hypotheses chased (substring fallback, StringBuilder, the char[]
constructor) were fused String payloads that were already one object. There was
never a missing-fusion problem.

### What survives

The char[] constructor fusion of the previous commit stands on its own: it turned
~32,438 genuinely two-object constructions into one, which the site profiler
confirms independently of the miscounted table. Its predicted large win never
existed.

### Where String actually sits

String is 24% of all allocation, 1.12M objects, avg 88 bytes -- and each is
ALREADY one fused object. So the lever is not fusion, it is the ~56 bytes of
overhead per String: 16 object header + 8 value reference + 4 count + 4 hash, and
then a 24-BYTE ARRAY HEADER on a payload that only that String references. That
inner header alone is ~27MB across the corpus.

It exists because `value` has to remain a real array object: Java code does
`value instanceof byte[]` and casts it, and natives read it as an array. Removing
it means teaching every one of those readers to address a payload that is not an
object -- which is a representation change of the same family as 4-byte
references, and should be scoped the same way.

## Round 34: SATB is worth 0.19%, and the profile names a bigger one

Two claims about per-thread SATB buffers were made on this branch. The first --
that the global mutex in cn1SatbEnqueue gates the memory win -- was withdrawn in
round 31 when the tight-pacing penalty turned out to be diffuse. This round
settles the rest by measuring what it is worth on its OWN, at the DEFAULT pacing
the VM actually ships, since pacing is not being changed.

Main-thread self time, default pacing, 10,085 samples:

| | share of mutator |
|---|---:|
| Java monitor + mutex machinery | **3.50%** |
| SATB machinery, all of it | 1.12% |
| -- cn1SatbEnqueueRangeLocked (already filters lock-free, flushes 256 at a time) | 0.91% |
| -- **cn1SatbEnqueue (the per-object mutex a per-thread buffer would remove)** | **0.19%** |

**0.19%.** The redesign is sound engineering and it is what G1 does, but on this
VM at its shipping pacing it addresses a fifth of one percent of the critical
path, for a change whose failure mode is collecting a live object. Dropped from
the list rather than carried as a maybe. The range form, which is 5x the scalar
form, was already fixed.

### What the same trace found instead

The mutex waits on the main thread do not come from the collector. Walking each
one to its nearest named caller, they are monitorEnter, monitorExitBlock and
java.util.Hashtable.get -- **Java `synchronized`** -- with cn1SatbEnqueue
appearing exactly once in the whole list.

That is 3.50% of the mutator spent entering and leaving Java monitors in a
program whose real work is single-threaded, and a meaningful part of it is inside
the kernel (__psynch_mutexwait 0.84%, _pthread_mutex_firstfit_lock_wait 0.68%),
not just the userspace fast path. monitorEnter already has a reentrancy fast path
keyed on ownerThread, but a first acquisition goes straight to
pthread_mutex_lock.

This is the shape HotSpot answered with biased locking, and it is a better fit
here than there: CN1 is single-threaded on the EDT by design, so an object that
is never touched by a second thread is the common case rather than a special one.
Two ways at it, and they compose: a thin-lock CAS fast path so an uncontended
acquisition never calls into libsystem_pthread, and closed-world escape
information so a monitor on a provably unshared object is elided outright.

Not started; recorded so the number is not re-derived.

## Round 35: frameless constructors -- correct, and below this benchmark's resolution

The largest exclusion the frameless census found was constructors: 3,850 of
23,445 methods, against 357 for try/catch that the old plan D6 had nominated as
the biggest codegen item left. The exclusion was a deferral, not a proof, and it
turns out to need no more care than an instance method: the receiver is a C
parameter either way, allocation zeroes the object before the constructor runs,
so a partially built receiver traces as itself plus null fields.

Coverage 18,062 -> 21,247 methods (77% -> 90.6%), ctorOrClinit exclusions to zero.

| | round 30 | round 35 |
|---|---:|---:|
| parpar elapsed (median of 7) | 5.475s | 5.365s |
| jdk25 elapsed (median of 7) | 4.730s | 4.581s |
| ratio | 1.158x | 1.171x |

**No measurable change.** Both arms moved -- parpar 2.0% faster in absolute terms,
jdk25 3.2% faster -- and the 1.1% ratio difference is inside the combined spread.
That is the expected size, not a disappointment to explain away: frame setup and
teardown is a few pointer bumps, and roughly 4M constructor calls at ~10
instructions each is ~0.3% of 14B. Kept because it is correct and because
frameless is the precondition for bounds-check elimination and StringBuilder
stack allocation in those 3,185 methods, which is where the value actually is.

Peak was not gateable this run (14.0% spread), so no memory claim is made.

One process note, from a correction taken mid-round: this was first built behind
`cn1.frameless.constructors`, defaulting off. That is the wrong shape for a
feature -- a flag defaulting off is a change nobody runs and a path CI never
exercises. Gating is for debug code. The flag was removed and the behaviour is
unconditional; __CLINIT__ stays excluded on a semantic criterion (entered through
the class-init guard, can re-enter arbitrary other clinits), not on caution.

## Round 36: the String store, four increments in, and the header that blocks the fifth

The goal: a fused String carries a 24-byte JavaArrayPrototype around a payload
nothing else can reach. length duplicates count, dimensions is always 1,
primitiveSize is the coder, dataOffset is a constant for the shape, and two
fields are GC sentinels -- only the class pointer carries information, and that
is one bit. ~27MB across the corpus, and it is what keeps a typical String in
size class 96 instead of 64.

Landed, each separately green (gauntlet, gc-verify, Gate A byte-identical):

| commit | |
|---|---|
| 5712778c2d | Java side behind isLatin1/latin1Value/isUtf16Exact/utf16Value |
| a342ce95a0 | C side: 11 reads through cn1StrChars |
| 99486fcd17 | Twin clazz -- the free coder bit |
| add81dfb52 | Last 10 C readers through the helpers |

### The twin, and why it landed without a layout change

class__java_lang_String_i8 is a byte-for-byte copy of the primary: same classId,
name, vtable POINTER, mark function, type-test row. Everything that decides
behaviour keys on the ID, so the twin is a java.lang.String in every observable
way; only the ADDRESS differs, and that address is the coder bit -- stored in a
word every object already carries. Checked first: there is no classId->clazz
table in the runtime and classId is read in exactly five places.

Twins beat real ByteString/CharString subclasses here because String is final
TODAY, so charAt/length/equals devirtualize to direct calls. Subclasses would
give them two or three implementations and push the hottest methods in the VM
onto the thunk switch -- throughput paid for bytes.

It was landed with NO layout change, allocating real Strings through the twin so
the machinery was exercised rather than dormant, and that immediately caught a
silent bug: the memcpy inherited cn1ClazzRegistered, which means "this clazz
ADDRESS is in the conservative GC's exact registry". The primary is registered by
the time the copy is taken, so CN1_CLAZZ_REGISTER skipped the twin forever,
gcMarkObject's guard stopped believing the twin's address was a clazz, and every
String allocated with it stopped looking like an object. Clean build; FusedTest
aborted under gc-verify and diverged by a few bits of checksum. In a combined
commit that would have been indistinguishable from the new layout misbehaving.

### What blocks the fifth increment, so nobody re-derives it

The flip needs cn1StrChars/cn1StrIsLatin1 visible to BOTH nativeMethods.m and
cn1_intrinsics.h, which is where the remaining raw reads live. Moving them into
cn1_intrinsics.h does not work: that header is included by generated .c files
that do not necessarily include java_lang_String.h, and the existing
`#if __has_include("java_lang_String.h")` guard beside its StringBuilder block
does NOT help, because __has_include tests whether the file EXISTS, not whether
this translation unit included it. The header is on disk in every build.

The way through is the opposite of inlining them: declare both as extern in
cn1_globals.h (which needs no String struct for a declaration) and define them
once in nativeMethods.m. -O3 ships -flto=thin, so the call is inlined across
translation units anyway and the representation stays single-sourced.

Remaining after that: the i16 twin, three fused allocation paths writing the
payload with no JavaArrayPrototype, cn1GcStringIsFusedLeaf keyed on the twin
rather than the embedded child's header, and the Java accessors given native
fallbacks -- with no array object, latin1Value()/utf16Value() cannot hand back an
array and their callers (replace, toCharArray, toCharNoCopy) need rewriting in
terms of charInternal.

## Round 37: the String store, finished -- 88 bytes per String becomes 63

The twin clazz of round 36 carries the coder in a word every object already has,
so the 24-byte JavaArrayPrototype a fused String wrapped around its payload had
nothing left to say: length duplicated count, dimensions was always 1,
primitiveSize was the coder, dataOffset was a constant, two fields were GC
sentinels. It is gone. An inline String has value == JAVA_NULL and its characters
at the first 8-aligned byte after the fields; the coder is _i8 or _i16. Large and
aliased Strings keep a real array, so value is either null or an array.

| | before | after |
|---|---:|---:|
| java.lang.String | 99.56MB | **70.97MB** |
| bytes per String | 88 | **63** |
| total allocation | 415.85MB | **386.60MB (-7.0%)** |

Whole-program, 7 interleaved rounds, quiet machine: 1.168x elapsed and 1.073x
peak against JDK 25. **The peak did not move, and that is expected rather than
disappointing**: round 31 established that peak is set by collector headroom --
live was ~828MB against a 1422MB peak -- and pacing is deliberately unchanged.
Allocating 29MB less per run lowers the rate at which that headroom refills; it
does not lower the headroom. Peak spread was 9.8% this run, so the harness
refused to gate memory at all, which is the right answer for a 4% difference.

### Four bugs, none of them visible to the compiler

  - Two copying constructors read another String's array directly: an inline
    source was a NullPointerException, and Latin1T died on its first line.
  - getChars dereferenced the backing array's class word -- segfault, SbTorture.
  - The lazy twin copy was armed only in cn1FusedLatin1Begin. Whichever of the
    other two paths ran first allocated with an all-zero clazz, so the first
    virtual call went through vtable[5] off a NULL vtable.
  - getClassImpl returns the clazz struct AS the Class object, which is the ONE
    place a twin's address becomes visible to Java. `s.getClass() ==
    String.class` answered false for a fused String and true for an array-backed
    one -- a real observable difference between two Strings of the same class.

### The one that matters for next time

The whole gauntlet was GREEN while getClass() was wrong. 26 tortures comparing
byte-identical output against JDK 25, and not one of them asked whether a String
was still String.class. It was found by a throwaway probe written to answer a
different question, and it is now TwinProbe: both shapes asked the same questions
-- instanceof String/CharSequence/Comparable, getClass identity and name, equals
and compareTo across representations, every read that used to hand back an array,
the copying constructors, survival across 20 collections -- and diffed against
JDK 25.

A representation change needs a test that interrogates the representation. The
existing suite tests what the VM COMPUTES; nothing tested what it IS.

## Round 38: enum values() is a non-issue, and the instrument that said otherwise

Round 37 closed with "the single biggest allocation site in the program is enum
values()" -- 135,074 OutputType[] clones. **Retracted.** OutputType[] is cloned
ONCE per run, by ByteCodeTranslator$1's clinit building a switch map, which is
exactly what javac is supposed to do.

The site profiler keyed on the allocation call's return address alone and kept
the class of whichever allocation claimed the slot first, on the stated reasoning
that a site allocates one array class. cloneArray breaks that: one call site that
clones every array class there is. Its row reported the true CLONE COUNT against
the FIRST CLASS cloned.

CN1_LOG_CLONE_SITES settles such questions in one run by naming the caller.
Keyed on (pc, class), the same run reads:

| site | allocations | MB |
|---|---:|---:|
| Subroutine.<init>(Subroutine) -> boolean[] | 134,768 | 4.56 |
| Frame.<init>(Frame) -> Value[] | 134,768 | 18.89 |
| Analyzer.analyze -> Value[] (two sites) | 71,724 | 13.14 |
| String.getBytes -> byte[] | 19,016 | 25.54 |

So the dominant allocation sites are ASM's Analyzer -- ~341k allocations and
~37MB -- driven by OUR two dataflow passes, LocalReceiverTypes.capture and
resolveDupForms. That is a real cost and worth reducing, but it is translator
work: it makes every customer's build faster and moves the benchmark RATIO not at
all, because the JDK arm runs the same passes.

### Three instrument defects in one session

- cn1FusedLatin1Begin charged a fused block to two census rows, making byte[]
  look like 647k separate arrays when it was 42k (round 33).
- The frameless census existed but its report was never registered, so plan item
  D6 stayed "unmeasured" while the measurement sat in the tree (round 35).
- This one.

Each cost a round of work aimed at a target that was not there. An instrument
that cannot be wrong in a way its own output reveals will eventually send
somebody somewhere expensive, and all three were found by cross-checking one
instrument against another rather than by reading either.

## Round 39: reading C2's output beside ours, for one hot method

hsdis is shipped by no JDK, so -XX:+PrintAssembly had been quietly useless and
this comparison was unavailable. Built from the JDK tree's own capstone backend;
vm/selfhost/jit-disasm.sh makes it repeatable.

BytecodeMethod.equals -- 10.9% of our main thread, 198 bytes of bytecode, C2
compiled it six times and inlined it elsewhere as well. Its last C2 compile
against our AOT C:

| | total | loads | stores | branches |
|---|---:|---:|---:|---:|
| ours | **371** | **92** | **4** | 98 |
| C2 | 466 | 70 | 30 | 96 |

We emit FEWER instructions, and the frameless work shows where it should: 4
stores against 30. C2's 466 also carries 91 movk (64-bit constant chains) and 34
nop (safepoint padding) that do no work. So on shape we are in the same place or
ahead, which is the question that was asked.

**We issue 31% more LOADS, and that is where the time goes.** Loads are what
sets IPC, and the profile's mutator gap is a throughput gap, not a stall.

### Where the extra loads come from, exactly

`o instanceof BytecodeMethod`, as we emit it:

    adrp/add   x8, <tagged class table>
    ands  x9, x2, #0x7        ; is it a tagged immediate?
    add   x8, x8, x9, lsl #4
    csel  x8, x2, x8, eq      ; object pointer, or tagged-class entry
    ldr   x8, [x8]            ; load 1: the clazz pointer
    ldrsw x8, [x8, #0x2c]     ; load 2: classId -- DEPENDENT on load 1
    cmp   w8, #0x1a3

C2 does the same test with one compare against a klass already in a register.

Two costs are ours alone and both are addressable:

  - **The tagged check is 5 instructions on every CN1_CLASS_OF**, including every
    instanceof and every virtual dispatch, and it is paid even where the operand
    cannot be tagged. The translator often knows: a receiver whose declared type
    is not Object/Number/Comparable/Serializable can never be a boxed immediate.
  - **classId is a second dependent load.** We compare IDs where HotSpot compares
    the klass word. For a class with no live subclasses -- which a closed world
    can decide -- `CN1_CLASS_OF(o) == &class__X` is ONE load and no bitset
    lookup. The type-test bitset from round 20 made instanceof O(1); this would
    make the common case O(1) with half the loads.

Neither is a codegen rewrite. Both are the closed world telling us something the
emitted code currently declines to use.

## Round 40: the leaf instanceof, and what the ASM comparison was worth

Round 39 read C2's output beside ours for BytecodeMethod.equals and found the
shape fine -- 371 instructions against 466, 4 stores against 30 -- but 31% more
LOADS. The single biggest contributor was instanceof: we load the class word,
then load classId out of it (a DEPENDENT load), then index the type-test bitmap.
C2 compares the klass word directly, because for HotSpot the klass IS the
identity.

In a closed world we can be too. Nothing live extends a leaf class, so an object
is an instance of it exactly when its class word IS that class. 433 of 529
instanceof sites qualify (82%). On BytecodeMethod.equals loads fall 92 -> 85,
narrowing the gap to C2 from +31% to +21%.

Whole-program: 1.154x against JDK 25, from 1.171x. **Not resolvable** -- parpar's
own elapsed spread was 9.5% that run. The honest statement is that the change
does what the disassembly says it does and the benchmark cannot see it, which is
the expected outcome for one instruction pattern among many.

### Two ways it was wrong, both caught

  - A BOXED type is final, so it passes the subclass test, and the leaf form is
    still wrong: a tagged immediate IS an Integer and has no class word. BoxEdge,
    HtTorture and InstanceOfT failed together. The reasoning that "a tagged
    value's class is never a leaf anyone tests for" was false -- final is exactly
    what those classes are.
  - String is a leaf whose class word is NOT unique, because a fused String
    carries a twin clazz. A plain identity compare answers FALSE for the Strings
    this VM creates most. Round 36's commit said explicitly that identity
    comparisons must ask cn1IsStringClass; this was new code doing the thing that
    warning was written for, three commits later. TwinProbe caught it, alone, in
    a run where the other 26 tortures were green.

The second is the more useful lesson: a warning in a commit message does not
constrain code written afterwards. The test does.

### Process note

Two gate runs overlapped and produced a status file with duplicate lines and
meaningless results, and separately a gauntlet ran against a stale translator and
reported a failure that a direct run of the same test did not reproduce. Run the
gates ONCE, serially, and confirm no gate process is alive before starting
another -- `mvn clean` in one run removes target/ under the other.

## Round 41: the tagged resolve, and ten things the disassembly says

CN1_CLASS_OF masks, tests the tag, indexes a proxy table and selects -- five
instructions before it loads anything -- and only ever finds something for a
boxed Integer/Long/Double/Float/Character/Short. A thunk named virtual_X_m only
receives an X or below, so where no taggable class is assignable to X the test is
dead. 7,387 thunks untagged, 133 left, and the 133 are exactly java_lang_Object
and the boxed types.

1.145x against JDK 25 at a 1.4% spread -- the tightest measurement taken here.
The sequence is 1.158 (r30), 1.171 (r35), 1.154 (r40), 1.145 now.

### BytecodeMethod.equals, ours against C2, measured

| | ours | C2 |
|---|---:|---:|
| instructions | 379 | 466 |
| loads | 85 | 70 |
| calls (bl/blr) | **10** | 31 |
| stack spills | **4** | 28 |
| adrp/movk constants | **8** | 91 |
| csel/ccmp predication | **9** | 0 |
| explicit null checks | 26 | **13** |
| nops (safepoint patching) | 0 | 34 |

### Five things C2 does that we do not

1. **Implicit null checks.** 12 sites annotated "implicit exception": it
   dereferences and lets a SIGSEGV handler synthesise the NPE. We emit 26
   explicit cbz/cbnz. This is the single clearest remaining item -- a compare and
   a branch removed per null check, and the mechanism (a signal handler that maps
   a faulting address to a throw) is one this VM already has the shape for.
2. **Uncommon traps.** 26 UncommonTrapBlob calls. C2 compiles the common path
   only and deoptimises to the interpreter for the rest, so cold code costs
   nothing in the hot body. We must emit every path, always.
3. **Optimized virtual calls.** 4 sites: a monomorphic call site becomes a direct
   call plus a class guard, patched from the observed receiver. Our closed-world
   devirtualization is the static analogue and covers the provable cases; this
   covers the ones that are merely true in practice.
4. **Profile-guided block layout.** C2 places the measured-hot path as
   fall-through. We have no profile and are not taking one, but static heuristics
   are available and unused: an exception path is cold, a null check is
   overwhelmingly not-taken.
5. **Safepoint polls as patchable nops**, 34 of them -- polling that costs
   nothing until armed.

### Four things we already do BETTER, worth not "fixing"

6. **We inline more** -- 10 calls against 31.
7. **We spill far less** -- 4 stack stores against 28. That is frameless codegen
   earning its keep, and it is the clearest win in the table.
8. **We address constants better** -- 8 adrp/movk against 91. A link-time
   constant beats a 64-bit address materialised at runtime.
9. **We predicate more** -- 9 csel/ccmp against 0.

### One neither of us does

10. **String.hashCode is not vectorized in our build** (0 SIMD instructions in
    104). Recent HotSpot has a hand-written vectorizedHashCode intrinsic; whether
    it fires here is unverified and worth checking before assuming a gap. The
    same question applies to String.equals and compareTo, where HotSpot ships
    SIMD intrinsics and we ship byte loops.

The shape of the answer: we are not losing on code quality in the ordinary sense
-- we inline more, spill less and address constants better. We lose on LOADS and
on work that C2 simply does not emit because it may deoptimise.

## Round 42: implicit null checks, and the stub that must not return

Item 1 of round 41's list. C2 annotated 12 sites "implicit exception" against our
26 explicit cbz; it dereferences and lets the hardware say so. The mechanism
transfers directly to array access, because the bounds check ALREADY loads
->length and that load faults at offset 16 on a null array. The load is the null
test, so the compare and branch leave every array access in the program.

Semantics survive on ORDER, not on care: the length load faults before the
comparison it feeds, so a null array with index 999999 is still NPE rather than
ArrayIndexOutOfBounds. NullDeref pins that and matches JDK 25 exactly.

### The finding worth keeping

**The landing stub must not return.** A plain C function returns through the link
register, which on arm64 still points at the faulting instruction -- so the fault
repeats forever. The first standalone probe hung exactly so, before any of this
reached the VM, and it is the kind of thing that would have read as "signals do
not work here" if it had been discovered inside a full build.

The rest fell out easily because throwException already walks the thread's
try-block stack and longjmps. No new unwinder was needed; a signal handler can
reach the existing one.

### What was and was not accepted

A platform capability, not an option -- Windows needs SEH, and there the explicit
test stays. Faults outside the first page restore the default handler and
re-raise, so real corruption still crashes with a real report. What IS accepted:
a wild pointer landing in the first page becomes an NPE instead of a crash. That
is the same exposure HotSpot carries, and it is bounded to one page.

### The rest of round 41's list, after review

Kept: item 2 (uncommon traps) reinterpreted -- not deoptimization, which needs an
interpreter we do not have, but the same IDEA applied statically: a fast common
path with the guardrails lifted, as the exception machinery already does for
methods with no try/catch and as devirtualization does for calls. That is
deepening the static analysis rather than adding speculation.

Dropped, with reasons: profile-guided layout (we are not taking a profile);
patchable safepoint nops (they pay for a JIT we do not have); vectorizing
String.hashCode (the method is small -- if hashing costs too much the answer is a
cheaper hash, not a SIMD rabbit hole).

Undecided: inline caches. Possibly large, and the static half is already done.

No timing this round: the host was at load 27 under Spotlight indexing. The last
trustworthy figure is round 41's 1.145x at 1.4% spread, which predates this
change.

## Round 43: null checks that are proven away rather than moved

Round 42 moved array null checks onto the hardware. This one removes a different
population outright.

The null test on a devirtualized call exists for a precise reason: removing the
virtual dispatch removed the load that would have faulted. So it is emitted
exactly where getProvenDirectOwner() succeeded. But the reason that succeeded is
that the receiver's provenance was an exact ALLOCATION -- and an allocation does
not produce null. A NEW or a lambda's invokedynamic proves the type and the
non-nullness together; a factory call or a field read proves only the type.

That distinction was being discarded: setClosedWorldReceiverTypes kept the type
and not the reason, so every proof looked the same. Carrying both:

    cn1ThrowNullPointerOrDie sites   967 -> 694

273 branches, 28%, on calls hot enough to have been devirtualized. No handler and
no exposure, unlike the implicit form -- the check is not relocated, it is shown
to be dead.

ACONST_NULL needed explicit handling. The exactness test TOLERATES it, because a
null merges with anything without disproving its type, and it is precisely what
must disprove non-nullness.

### Status, and why there is no number

Gauntlet, gc-verify and Gate A green for both this and round 42. NO TIMING: the
host has been between load 30 and 194 throughout, running iOS Simulator runtimes
and Spotlight indexing. The last trustworthy figure remains round 41's 1.145x
against JDK 25 at 1.4% spread, which predates the implicit null checks and this
change. Both are load-bearing enough to want a real measurement before anything
is claimed for them.

Open, in order:

  - Benchmark these two on a quiet host.
  - The fast-common-path idea (round 41 item 2, reinterpreted): lift guardrails
    statically where the analysis can prove they are unnecessary, as the
    exception machinery already does for methods with no try/catch. The two null
    check commits are instances of exactly this shape.
  - Compressed references: the 32GB window is in and holds the whole page heap
    (fallbacks=0); the legacy heap still comes from calloc outside it and a
    reference block can hold either kind.

## Round 44: the three guardrails, finally ranked

The fast-common-path idea is one thing repeated: a guardrail is lifted where the
analysis can prove it unnecessary. Rounds 42 and 43 did it twice for null checks
-- once by moving the check to the hardware, once by proving it dead. The next
candidates are the three things analyzeBoundsChecks' own javadoc says a single
try/catch disables at once. Two had been counted. The third had not, and it is
the one that costs a compare and a branch on every array access:

| guardrail | what one try/catch costs |
|---|---|
| frameless codegen | 361 of 23,449 methods -- **1%** |
| StringBuilder stack allocation | 315 of 1,818 sites -- **17%** |
| **bounds-check elimination** | **3,910 of 19,164 array accesses -- 20%** |

That ordering is the result. The old plan's D6 nominated try/catch as the biggest
codegen item left on the strength of the frameless number alone, which is the
smallest of the three by a factor of twenty.

20% is an upper bound rather than a promise: BCE only fires on the canonical
counted loop, so the recoverable share of those 3,910 is smaller.

### The design, so it is ready rather than remembered

BCE's proof is that falling through `IF_ICMPGE exit` establishes i < a.length for
the body. An exception edge adds exactly one way to enter that body without the
test: a handler whose TARGET lies inside it. So the blanket bail can become a
per-loop check -- no TryCatch.getHandler() resolving to a position within
[loopTop, exit) -- and the existing proof stands unchanged. An exception thrown
inside the body leaves the loop, which is harmless: no later access happens in
that iteration.

TryCatch exposes getStart/getEnd/getHandler, and the pass already builds a
Label->position map for the exit target, so the check is local.

NOT ATTEMPTED HERE, deliberately. A wrong bounds-check elimination is a silent
out-of-bounds access, which is the worst failure class in this VM, and the bail
being replaced is driven by a STATIC hasTryCatch flag whose reset discipline
needs checking before it is trusted. Two subtle errors today were caught only by
tests written for other purposes (boxed types in the leaf instanceof, the String
twin in the same). This one wants a fresh start and a torture that puts an array
loop inside a try/catch.

### Still no timing

The host ran iOS Simulator runtimes and Spotlight indexing at load 30-194
throughout. Round 41's 1.145x at 1.4% spread remains the last trustworthy figure
and predates rounds 42 and 43.

## Round 45: status, on a host that finally went quiet

7 interleaved rounds, parpar spread 5.8% elapsed and 7.4% peak -- the peak
spread is under the gate's 8% threshold, so BOTH axes were gated this time,
which has been rare.

| | vs JDK 25 | vs JDK 8 |
|---|---:|---:|
| elapsed | **1.152x** | 0.882x |
| peak memory | **1.064x** | 0.743x |

Against round 41's 1.145x at 1.4% spread, 1.152x is inside the spread: **rounds
42 and 43 -- implicit null checks and the 273 proven-dead checks -- are not
resolvable by this benchmark.** That is the ordinary outcome here and was the
expectation; both were taken on what the disassembly showed, not on a predicted
whole-program number.

Where the session stands against JDK 25 across its length: 1.278x -> 1.214x ->
1.147x -> 1.158x -> 1.171x -> 1.154x -> 1.145x -> 1.152x on time, and roughly
1.20x -> 1.024x -> 1.064x on peak. The time figure has moved about 10% and the
memory figure from a fifth behind to within noise of parity. We beat JDK 8 on
both axes by 12% and 26%.

The honest reading of the last several rounds: individual changes have been
landing below this benchmark's resolution. The disassembly shows them (92 -> 85
loads on the method examined, 7,387 of 7,520 thunks losing the tagged resolve,
273 of 967 null tests proven dead), and the whole-program number does not. That
is not a reason to stop taking them -- it is the critical-mass argument this
branch started from -- but it does mean the benchmark can no longer arbitrate
single changes, and the ASM comparison has become the more informative
instrument.

## Round 46: bounds-check elimination, four times as much of it, and a throw that escaped

Round 44 nominated the try/catch guardrail on BCE as the largest codegen item
left, at 20% of the corpus' array accesses. Both halves of that turned out to be
wrong, and finding out why is most of this round.

### The 20% was a leak, not a measurement

`TryCatch.isTryCatchInMethod()` is set in `TryCatch.appendInstruction` and cleared
by `TryCatch.reset()` inside `appendMethodC` -- both during EMISSION, while
`analyzeBoundsChecks` runs in `optimize()`, before any of that. Read there it
answers for whichever method was emitted last, so the census over-counted by
however many try/catch-free methods happened to follow one that had them. That
static flag was also the pass' second bail, so it was refusing methods with no
try/catch in them at all.

Scanning instructions instead: **207 methods and 1,424 array accesses, 7%.**

### And 7% was not the ceiling that mattered either

The per-loop refinement landed as designed -- a TryCatch instruction is a
DECLARATION of an exception edge, so it is kept out of the positional view and
its LANDING PAD is what gets checked; a pad inside [header, exit) enters the body
without the test having run, a pad outside is just another way to leave, and a
jump from there back in is an ordinary Jump that bceForeignEntry still refuses.
It recovered **14 array accesses**, and `loopsRefusedByHandlerInside` came back
non-zero, so the new guard does fire rather than decorate.

14. The number that explains that is `cleared=106 of arrayOps=19164` -- BCE was
clearing **half a percent** of the corpus' array accesses, so no guardrail was
what held it back. The single canonical loop shape it recognized was.

### So count the refusals instead of guessing at them

A rejection-reason census -- first failing precondition per candidate loop, plus
the per-access misses inside accepted ones -- answered it in one run:

```
candidateLoops: lengthNotArraylength=2384  accepted=133  (nothing else over 70)
inAcceptedLoops: cleared=106  notALoad=74  arrayNotLoopArray=43  indexNotInductionVar=23
```

Nine tenths of all candidate loops were refused because the comparison's right
side was not a literal ARRAYLENGTH, i.e. the length was hoisted into a local --
`int n = a.length; for (i = 0; i < n; i++)`, which is what javac emits for most
real code. And 28% of the array ops inside the loops it DID accept were stores,
which the pass never marked at all.

### Both, and the result

| | cleared | accepted loops |
|---|---:|---:|
| round 45 | 106 | 133 |
| + per-loop handler check | 106 (+14 in try/catch methods) | 133 |
| + array stores | 143 | 133 |
| + hoisted length | 159 | 149 |
| + array-slot rule relaxed | 426 | 406 |
| + real dominance | **438** | **419** |

**4.1x**, and 438 of 19,164 is 2.3% against 0.55%.

Stores need a different matcher: the value expression sits BETWEEN the index and
the store, so there is nothing adjacent to compare, and the operand stack is
walked back from the store until the depth below it is exactly the array+index
pair. A call in the value expression cannot touch this frame's locals and cannot
change an array's length, so the proof is the same one the load path uses -- only
the way the pair is located differs. The mark then had to be honoured in three
more places: `shouldEmitNullAndArrayBoundsChecks` ignored it outright (so a proven
access kept its check on every path the reduction passes could not fold), and
there was no `CN1_SET_ARRAY_ELEMENT_*_NOCHK` to emit.

### Three tries at one dominance proof

Proving `n` IS `a.length` at the loop is a claim about two slots, and the part
that took three attempts is "reaching the header means the capture ran".

- Refusing any jump into `(q, header]` refuses the **back edge**, which every
  loop has. Proved 0 of 1,154 candidates -- a total refusal that looks exactly
  like "the corpus does not have this shape" from the outside, which is why the
  sub-reason census went in before the second attempt rather than after it.
- Narrowing to `(q, header)` proved only the FIRST loop of any method that hoists
  one length and runs several loops on it: the earlier loops' own exit labels are
  jump targets sitting between the capture and the later headers.
- Asking for dominance directly -- delete the capture from the CFG and see
  whether the header is still reachable, with every exception handler as an entry
  -- is both correct and more permissive. `captureNotDominating` fell from 29 to 9.

The other rule that had to be relaxed rather than tightened: refusing every write
to the array's slot accounted for **307 of 1,134** refusals on its own. A write
BEFORE the capture is harmless, because re-running it means jumping back to it
and re-running the capture with it; only a write the capture has already passed
can leave `n` describing an array the loop no longer indexes.

What is left, for whoever widens this next: `lengthLocalNoCapture=604` (the local
is not set from an ARRAYLENGTH at all -- many are genuinely not array lengths),
`lengthLocalWrittenTwice=211`, and `lengthNotArraylength=1079` where the bound is
a constant, a field or a call.

### The bug this turned up is bigger than the optimization

`BceTryCatch` failed on the target, and it failed identically with BCE compiled
out. Minimized:

```java
try { try { throw a; } catch (E e) { throw e; } } catch (E e) { }   // uncaught
```

Nested try/catch compiles to two exception-table entries beginning at the SAME
instruction, so both register a begin at the same label. TryCatch emitted them in
table order and each asked for its own end label's catch depth as it went -- the
inner region, emitted first, computed that depth while only its own begin was
registered and cached 1 where the answer was 2. END_TRY restores tryBlockOffset
to that depth, so entering the inner handler deregistered the OUTER try with it.

Wrong on every platform, for years, in four lines of Java. Nothing in the
tortures threw from inside a handler. Deferring the computation to label emission
fixes it; `NestThrow` and `NestedTryIntegrationTest` gate it, the latter by
re-translating under `cn1.legacyCatchDepth` and requiring the escape to come back.

### The lesson worth keeping

Three separate numbers in this round were artefacts of the instrument rather than
facts about the code: the 20% guardrail cost (a static flag read in the wrong
phase), the 0-of-1,154 hoisted-length proof (a rule that refused the back edge),
and the first-loop-only result after that. Each looked like a finding. The thing
that separated them from findings was always a second counter, never more
thought about the first one.

## Round 47: StringBuilder stack allocation stops fearing try/catch, and the check that had to exist first

The same guardrail, the other analysis. One try/catch anywhere in a method disabled
implicit stack allocation for EVERY builder in it -- 315 of the corpus' 1,818 sites,
17%, including builders nowhere near the protected range.

**300 of the 315 survive the escape analysis unchanged**: sites stack-allocated go
1,423 -> 1,723, 78% -> 95%. The reason is that the analysis was already
control-flow-INSENSITIVE where it counts. A builder parked in a local is validated by
walking EVERY instruction in the method that touches that slot, in index order, with
no regard for how control reaches it -- so a use inside a handler is checked exactly
like any other, and an escape there (a PUTFIELD, a non-borrowing call) bails the same
way. A builder never parked in a local is consumed inside one expression, and an
exception mid-expression DISCARDS it, because the catch block resets SP to
&stack[1]. What an exception edge cannot do is extend the object's LIFETIME, which is
the only thing stack allocation depends on: the struct is a C local of the same
function as the setjmp, so a longjmp lands in the frame that owns it.

### The residual risk was invisible to every gate in this repo

A wrong escape analysis puts a C-stack address in the heap. Three analyses here do
this kind of proof -- implicitly stack-allocated builders, scalar-replaced
@StackAllocate instances, SIMD stack arrays -- and a wrong one produced NO signal:
the address is in no BiBOP page and no legacy extent, so cn1GcVerifyClassify answered
UNKNOWN and SKIPPED it, the same answer it gives a static or an immortal.

So the check went in first. Each thread's C stack range is recorded beside the
existing nativeStackLimit, and the verifier compares every traced reference against
those ranges -- no dereference, so it runs BEFORE the classifier and cannot be
confused with either of the things UNKNOWN legitimately covers. self-test6 requires
BOTH arms: the ablated build (-Dcn1.sbSkipEscapeValidation) must be caught, and the
normal build must stay silent, because "the ablated arm reports" alone would also be
satisfied by a check that reports everything. Measured: control 5 clean passes and 0
escapes, ablated arm 5 reports.

Worth knowing if that driver is ever changed: a builder escaping a frame that
RETURNS is the real-world shape and does NOT survive to be reported. Later calls
overwrite the dead frame, the collector reads a garbage class word out of it, and the
process dies with SIGBUS before any verify pass runs -- measured, exit 138 with no
output at all. SbEscape publishes from main's frame so the object stays well-formed
and the check is what notices.

### The bug in the check, and why it matters more than the check

The first version walked all NUMBER_OF_SUPPORTED_THREADS (1024) slots for every
traced reference. On MapTorture2 that is 300,000 references a pass, so 300 MILLION
iterations.

**It did not look slow. It made two self-tests go quiet.** The collector became slow
enough that the workload FINISHED FIRST, so the run recorded one verify pass -- the
empty one, before any objects existed -- instead of two, and self-test4 and
self-test5 reported BROKEN because their injected faults never got a second cycle to
be caught in. The visible symptom was `passes=1 refs=0`, which reads like a detection
failure and is nothing of the kind.

The diagnosis order is the transferable part, because the first hypothesis was wrong:

1. A/B the new OPTIMIZATION first (-DCN1_DISABLE_SB_STACK_ALLOC). It failed
   identically with stack allocation off, which ruled out the obvious suspect and the
   plausible story that went with it (fewer allocations -> fewer cycles -> the fault
   has fewer chances to fire).
2. Revert the runtime half: `passes=2 refs=297895`, key k13 lost. Cause confirmed.
3. Split the header from the .m: the header alone was fine, so it was the check
   itself, not the struct layout.

The fix is an envelope compare -- collect the live threads' ranges once per verify
pass, then reject on two comparisons, since stacks are nowhere near the heap.

And a second, smaller one in the same test: `grep -c` EXITS 1 when the count is zero,
and zero is the EXPECTED answer for the clean arm's escape count. Under this script's
`set -e` that killed it between self-test6's label and its verdict -- the run printed
"self-test6" and then nothing, with no GREEN and no FAILED line, which is the shape of
a gate that has stopped existing rather than one that failed.

### No timing

The host has been at load 4-13 all session. perf-guard's last run came back at
elapsed spread 26.8% and peak spread 15.0% against JDK 25's 3.4% and 2.3%, so neither
axis gated and the 1.162x/1.037x it printed is not a figure. Round 45's 1.152x/1.064x
remains the last trustworthy pair, and it predates rounds 46 and 47.

## Round 48: frameless meets try/catch, and the memory gap turns out not to be the heap

The third guardrail, and then the measurement that says what to do next.

### Frameless codegen under try/catch

21,251 -> 21,612 methods, with the extra frame emitted into the 353 that need it.
What a frameless frame lacked was two NAMES the exception macros use --
methodBlockOffset (END_TRY and JUMP_TO restore tryBlockOffset to it) and
currentCodenameOneCallStackOffset (DEFINE_CATCH_BLOCK restores callStackOffset to
it). Both are the entry value, and for a frameless frame that is also the value
the method never changes, so the catch block's restore is a no-op for this frame
and the right thing for any CALLEE frame a longjmp unwound through.

The setjmp requirement was already met: volatileLocals is set by the same
instruction scan and selects the _VSP frame variant. The only genuinely new part
is that a RETURN out of a try block skips the end label where END_TRY would have
run, so it unwinds the block stack itself.

All three guardrails are now lifted:

| guardrail | before | after |
|---|---:|---:|
| bounds-check elimination | 106 accesses | 438 |
| StringBuilder stack allocation | 1,423 sites (78%) | 1,723 (95%) |
| frameless codegen | 21,251 methods | 21,612 |

### Whole-program: nothing, for the fourth round running

1.175x then 1.162x against round 45's 1.152x, parpar elapsed spread 2.3-2.8%.
All inside each other's noise; parpar's own minimum did not move (5.346s ->
5.29-5.41s). Reported as a null result.

### The memory axis is not the heap, and not the collector

perf-guard has said for several rounds that peak is ungateable at 15-21% spread
and called it collector pacing. It is not. Six standalone runs, each VERIFIED at
2,933 emitted .c files and exit 0:

| | min | median | max | spread |
|---|---:|---:|---:|---:|
| parpar peak | 1.431GB | ~1.49GB | 1.703GB | **19.0%** |
| jdk25 peak | ~1.50GB | ~1.53GB | ~1.56GB | 3.2% |

min/min 0.965x, median/median 0.974x, max/max 1.084x -- at or better than parity
on a typical run, losing only on the tail, and perf-guard gates max-of-N.

`[GCPROBE]`'s partition kills the pacing hypothesis in one run, which is exactly
what the residual field was added for:

| (last cycle) | run1 | run2 | run3 |
|---|---:|---:|---:|
| residentPgKb (page heap) | 609MB | 608MB | 617MB |
| sideKb | 269MB | 273MB | 270MB |
| legBlockKb | 133MB | 117MB | 136MB |
| **residKb** | **437MB** | **443MB** | **668MB** |
| footprint | 1442MB | 1461MB | 1698MB |

The footprint swings 256MB and residKb swings 231MB of it. The page heap holds to
1.5% while the process swings 19%, and maxMarkMs barely moves -- so the mutator
is not running ahead of the collector, and a footprint TARGET would have been a
fix for a mechanism that is not operating.

### Where it actually is

`vmmap --summary` at a verified 1.5GB peak:

| region | size | dirty |
|---|---:|---:|
| VM_ALLOCATE (the reserved window -- BiBOP pages) | 812.6M | 683.3M |
| MALLOC_SMALL | 608.0M | 596.3M |
| MALLOC_SMALL (empty) | 152.0M | 151.5M |
| MALLOC_LARGE | 89.5M | 79.6M |
| MALLOC_LARGE (empty) | 75.6M | 69.4M |

and the allocation census at exit: `bibop 457.28MB legacy 134.45MB | nativeBlocks
270.17MB`, with byte[] the dominant live class at ~1300-1700 B/obj (class-file
buffers and emitted C text, all over the 512-byte BiBOP limit) and 1.8M Strings
at ~70 B/obj.

Put together:

| | live payload | dirty pages holding it |
|---|---:|---:|
| BiBOP page heap (reserved window) | 457MB | 683MB |
| legacy heap + native blocks (MALLOC) | 404MB | 816MB |

**The malloc side carries 2x overhead and it is the half that varies; the managed
side carries 1.5x and does not move.** ~220MB of that is dirty memory malloc has
already freed -- which is consistent with T2's finding that
malloc_zone_pressure_relief never moved the peak in three configurations. You
cannot ask malloc to return what its free lists hold; the traffic has to stop
going there.

So the next item is to route the LEGACY HEAP and the NATIVE BLOCK allocator
through the same reserved window with size classes and madvise release, instead
of malloc. The expected win is most of the 220MB of retention plus part of the 2x
overhead -- but the reason to do it first is that it makes peak footprint
REPRODUCIBLE, and until it is, this benchmark cannot resolve any memory change at
all.

Note the earlier plan's D1 aimed at taking BiBOP pages from mmap rather than
malloc. That half is already done -- the pages come from the reserved window and
are the stable half. The malloc user that remains is the block allocator.

### A measurement that measured nothing

Worth recording because it nearly became a finding. Run standalone without
CN1_RESOURCE_PATH, the translator parses the whole corpus and then dies in
copyRuntimeResource before emitting a single file -- exit 1, no .c written. The
peak of that truncated run is 0.92GB at 0.6% spread, which looks exactly like a
clean, stable measurement and supports a completely wrong story about the
collector holding its best case. The column that caught it was a count of emitted
.c files. Every peak figure in this round carries one.

## Round 49: a native block pool, built and reverted

Round 48 ended by proposing that the legacy heap and the native block allocator
move off malloc. The block half was built and measured. It does not work, and
the reason is worth more than the code was.

### The attribution was right

`[JHEAP]` reports malloc directly, which makes this cheap. Baseline at exit:
`MALLOC inUse=575.39MB allocated=917.88MB idle=342.48MB` against `JAVA live
591.73MB`. Rebuilding with `CN1_BLOCK_MMAP_THRESHOLD=256`, so every block
bypasses malloc at an absurd cost in page waste, gave `inUse=147.93MB
allocated=298.44MB idle=150.51MB` -- and a 4,990MB footprint, which is what
mapping 128-byte blocks into 16KB pages costs.

So small native blocks really are ~427MB of malloc's in-use and 56% of its
retention. `[CAPHIST]` names them: 1,235,497 reference blocks of capacity <=16
(128 bytes) and 312,183 tables of the same capacity (384 bytes) -- ArrayList
backing stores, HashMap tables, String payloads. About 2.5 million calloc/free
pairs per translation.

### The pool

Size-classed slots (23 classes, 32B..2KB) inside chunk-aligned mappings,
outside the reserved window on purpose -- block memory has never been resolvable
by cn1ConservativeResolve, and putting it in the window would make a stray stack
word pointing into a block resolve to a fabricated object. Free finds the chunk
by masking the slot address, so the non-mapping fallback had to be
posix_memalign rather than calloc: cn1RefBlockFree picks its path from the same
size test the allocator used, and a plain fallback would hand a pooled pointer
to free().

One bug in it, found by MapTorture2 on the first run and worth repeating because
it is the shape this whole subsystem fails in: a chunk retired to the spare
cache and then reused handed out the previous tenant's bytes, while
cn1BlockAlloc promises calloc semantics. Fresh mmap is zeroed, so the bump path
is right for a NEW chunk and wrong for a REUSED one, and a reference block filled
that way is read by the marker as a page of object pointers.

### And it is not a win

Interleaved, every run verified at 2,933 emitted .c files.

At four rounds it looked like one: median footprint -5.1%, max -8.9%, spread
12.7% -> 6.3%, time flat. At EIGHT rounds:

| arm | peak min | median | max | spread | elapsed min/med |
|---|---:|---:|---:|---:|---:|
| nopool | **1328MB** | 1488MB | **1585MB** | 19.4% | 5.26 / 5.35 |
| pool (64KB chunks) | 1411MB | 1472MB | 1713MB | 21.4% | 5.27 / 5.41 |

Medians 1% apart, no-pool better on BOTH the minimum and the maximum, spread
unchanged, elapsed minima equal. The four-round variance reduction was noise,
and it was predicted to be the kind of claim four samples can manufacture in the
same session it was made.

Why it cannot win, in hindsight: the pool held 250MB of chunks for 177MB of
slots. A chunk returns to the OS only when EVERY slot in it dies, and with
long-lived blocks scattered across 23 size classes, chunks stay partially
occupied -- so malloc's fragmentation was replaced by the pool's, at the same
total. 64KB chunks beat 256KB ones on release granularity (26% slack against
29%, the same reason BiBOP pages are 64KB) and still did not clear the bar.

Malloc's idle did not fall either: 342MB before, 293-391MB after. The retention
that remains is driven by the LEGACY HEAP -- 134-168MB live, high turnover --
which this pool does not touch. The threshold=256 attribution run removed that
traffic too, which is why it looked like the blocks were responsible for all of
it.

REVERTED. Three hundred lines in the allocator, one silent-corruption failure
mode already demonstrated, for nothing measurable. The rule that benchmarks
guard rather than decide covers changes that are better in PRINCIPLE; this one
trades one allocator's fragmentation for another's, and measurement is the only
thing that could have said which wins.

### What is left of the memory question

At exit the footprint partitions roughly as: Java live 592MB, BiBOP slack
183-219MB, malloc idle 342MB. The next candidate is therefore the BiBOP slack --
34% of a managed heap, with 427-519 empty pages retained -- and the legacy heap's
own malloc churn. Neither is the block allocator.

## Round 50: raise the BiBOP ceiling to 2KB

The opposite shape to round 49. Instead of adding an allocator, raise the
ceiling on the one that already works: CN1_BIBOP_MAX_OBJECT 512 -> 2048, eight
more size classes, about ten lines. Objects that move there leave malloc for the
reserved window, swept and madvise-released, rather than moving into a second
heap of my own making.

Interleaved, eight rounds, every run verified at 2,933 emitted .c files:

| | 512 | 2048 |
|---|---:|---:|
| legacy OBJECTS | 28,000 | **2,300** |
| legacy BYTES | 146MB | 140MB |
| MALLOC idle (median) | 386MB | 343MB |
| peak footprint (median) | 1556MB | 1560MB |
| elapsed (median) | 5.59s | **5.54s** |

### It is a throughput change, and the memory model behind it was wrong

26,000 objects moved into managed pages and took about 6MB with them. What is
left on the legacy path is 2,300 objects holding 140MB -- **61KB each** -- the
class-file and emitted-source buffers. So the legacy heap is a handful of very
large buffers, not tens of thousands of medium ones, and no size class worth
having will capture it.

The "1,300-1,700 B/obj" figure that motivated this was an average over a [LIVE]
CLASS GROUP (byte[]), not the legacy heap's distribution. Reading a per-class
average as a per-heap one is how the prediction came out wrong.

What is real is the work removed: the legacy path costs a calloc, an
allObjectsInHeap registration and an extent-snapshot entry per object, and 26,000
of those per run became bump allocations. The 2048 arm won 7 of the 8 paired
rounds. The COUNTER is the durable evidence -- this host cannot resolve 1.5%, as
three rounds running have shown -- and the count is a 92% cut.

### Where the memory question now stands

Two candidates are eliminated rather than tried and failed:

- malloc's ~340MB idle is NOT small collection blocks (round 49 pooled them; the
  slack simply moved) and NOT medium objects (this round moved them; idle fell
  11%). What is left is those 2,300 large buffers.
- BiBOP slack, 183-219MB on a 640MB reserved heap, is the price of non-moving
  collection. A copying collector compacts it; this one cannot, and the branch
  has already recorded why a moving collector is not on the table.

---

## Round 26: every string literal paid for two acquire loads

The pass that looked for the user's "if(x) return ...; split it" shape in C found
the two biggest call-site counts in the binary were **already** that shape and
already correct:

| function | `bl` sites | verdict |
|---|---:|---|
| `cn1MaterializeConstantPoolString` | 4,437 | fast path inline, cold call -- correct |
| `cn1SatbEnqueue` | 4,101 | `gcSatbActive` test inline, cold call -- correct |
| `cn1HmFindSlot` | 117 | a probe LOOP, file-static; out of line is defensible |

So the shape was not the problem. The defect was inside the one that looked
healthiest.

`STRING_FROM_CONSTANT_POOL_OFFSET` named `CN1_CONSTANT_POOL_LOAD(off)` twice --
once in the ternary's condition and once in its true arm -- and clang folded
neither away. The emitted fast path for every string literal in the program was:

```
ldr   x8, [x19]        ; load constantPoolObjects
add   x8, x8, #0x240   ; + off*8
ldapr x8, [x8]         ; acquire load #1
cbz   x8, <cold>       ; null -> materialise
ldr   x8, [x19]        ; reload the base AGAIN
add   x8, x8, #0x240   ; recompute the address AGAIN
ldapr x10, [x8]        ; acquire load #2 -- redundant
```

Six instructions and two ordering primitives where three and one do. clang is not
being dim: an atomic acquire load is a synchronisation point it will not CSE, and
for all the optimiser knows that very acquire synchronises-with a writer to
`constantPoolObjects` itself, so even the base pointer had to be re-loaded.

Folding them is safe, and the safety is a property of the data rather than a
judgement call: a slot is written exactly once, null -> object under
`constantPoolMutex` (`cn1_globals.m:14722` is the only store to any slot), and
never cleared; the base is assigned once at init. The two loads could only ever
have returned the same pointer -- so this was pure redundancy, **not** a latent
null-after-non-null bug, and should not be described as one.

| | before | after |
|---|---:|---:|
| `ldapr` | 13,827 | **9,125** (-4,702, -34%) |
| `ldapur` | 4,891 | 4,857 |
| `ldar` | 640 | 640 |
| total `bl` | 58,552 | 58,548 |
| binary | 5,285,080 | **5,202,520** (-82KB) |

-4,702 acquire loads against 4,431 materialise sites: one per site, which is the
prediction, arriving at the predicted magnitude. Gates green, Gate A byte-identical.

Unlike most rounds here there is no trade to weigh -- instruction count, ordering
primitives and binary size all moved the same way. It is also the cheapest fix on
this branch by a wide margin, and it survived in a macro carrying a nine-line
comment about the acquire/release pairing. The comment was right about the
ordering and silent about the arithmetic, and nobody reads a macro body twice.

**Method note.** Static call-site counts found this; they should not pick the next
one. They are the right instrument for "is this function inlined at its call
sites", and the wrong one for "does this code run" -- the monitor pair
(`monitorEnter` 1,367 + `monitorExitBlock` 2,260) ranks high statically, and this
workload uses `HashMap`/`ArrayList`/`StringBuilder`, none of them synchronised.
Profile before touching it.

---

## Round 27: the biggest mutator cost was a GC barrier, and no gate could see it

> **RETRACTED by Round 37.** The narrowed barrier below is unsound and has been
> removed. Its premise -- a reference that stays in the block needs no barrier --
> ignores a marker scanning the same block while the memmove runs, and that race
> swept live objects out of the self-hosting translator about one run in twenty.
> The arithmetic proof and the self-test built on it checked the narrowed range
> against the narrowed contract, so they could only ever agree with the mistake.

A profile of the mutator (main thread only -- the all-thread view is ~78% idle
GC workers and says nothing) put one function on top:

| self time | |
|---:|---|
| **7.8%** | `cn1SatbEnqueueRangeLocked` |
| 11.8% | `open`/`write`/`close` (emitting 855 files) |
| 4.9% | `memmove` |
| 3.7% | `cn1HmFindSlot` |

Callers: `ArrayList.remove(int)` 19 samples, `ArrayList.add(int,Object)` 14,
`System.arraycopy` 7. All three are **same-array shifts**.

A move within one block PERMUTES references, it does not drop them. Slots
`[to, to+count-1]` are overwritten, and the old value at slot `j` survives
exactly when `j` is in `[from, from+count-1]` -- it gets rewritten at
`j + (to-from)`. So the snapshot is owed

    [to, to+count-1] \ [from, from+count-1]

which is contiguous and holds `min(count, |to-from|)` slots: **one**, for an
ArrayList insert or remove, whatever the list's length. The barrier was loading a
mark word per moved element -- scattered across the heap -- for references that
were still in the array afterwards. The insertion half is owed nothing at all for
a same-block move: every value written was already in that block.

Result: **7.8% -> 1.5%** of mutator self time, out of the top ten. Gauntlet,
gc-verify and Gate A (855 files byte-identical) green.

### The part worth remembering: the gates could not see any of it

Before trusting that green, the barrier was removed OUTRIGHT for same-block moves
-- `cn1SatbMoveLostRange` forced to return 0, a definitely-wrong collector.

**`run-gc-verify.sh` (all six self-tests) and `run-gauntlet.sh` (33 tortures,
both stop modes) were GREEN.**

So the passing run proved nothing, and would have been banked as proof. This is a
coverage hole that **predates this change**: any future edit to the same-block
SATB barrier gets the same false green. It is the `BulkCopyBarrier` lesson --
a self-test that cannot fail is worse than none -- arriving from the other
direction, as a gate that cannot fail.

What replaces it, for the half that admits proof:
`cbench/test_satb_range.sh` EXTRACTS `cn1SatbMoveLostRange` from the header
(never a copy, which would pass while the real code drifted) and brute-forces it
against a model over all 32,500 shapes -- exact on every one, never
under-reporting. Then it injects three faults, including the exact
"barrier always empty" one the GC gates missed, and requires each to be caught.

### What is NOT proved, stated plainly

The arithmetic is proved. The PREMISE under it -- that a reference remaining in
the block needs no barrier of either kind -- is a reachability argument, and no
gate exercises it. A torture could not be constructed that fails without it,
partly because there may be no barrier-free republication path left in the VM,
which would make the claim a theorem rather than a gap. Recorded as reasoned,
not as measured.

Note the shape of the trade before "restoring the insertion half to be safe":
that leaves the barrier O(count) per shift and gives back most of the win. It is
not a free safety margin.

---

## Round 28: the inlining work was already done, and four hypotheses died proving it

The remaining approved items were "devirtualize single-implementation call sites"
and "for sites that cannot be proved, emit the body once as an inline function
with its own signature, called both by the vtable target and by the proven
sites". Both rest on one premise: **that small hot methods are failing to inline
at their call sites.** The premise is false, and it took four refutations to
establish that.

The apparent evidence was out-of-line call counts in the shipping binary:
`ArrayList.get` 167, `ArrayList.add` 152, `String.charAt` 113. Each hypothesis
for why, and what killed it:

| hypothesis | measurement | verdict |
|---|---|---|
| the emitted prologue (locals[]/stack[], `__builtin_frame_address`, the `cleanup` + `"memory"` clobber) blocks inlining | `cbench/test_inline_prologue.c`: 5 variants, 2 TUs, ThinLTO | **refuted** -- all inlined, including a non-leaf variant calling an uninlinable helper the way `get` calls `checkIndex` |
| ThinLTO declines to IMPORT the body (`-import-instr-limit`, default 100) | rebuilt at 400 | **refuted** -- counts IDENTICAL (167/152/113); total `bl` moved, so the flag took effect |
| caller size: real callers median 2,821 instrs, p90 16,339 | synthetic 24,758-instruction caller | **refuted** -- the tiny callee still inlined |
| inliner cost budget | `-inline-threshold=1000` at link | **refuted** -- total `bl` 58,542 -> 93,207 and binary 5.2 -> 7.37MB, yet `get` went 167 -> **172** and `charAt` 113 -> **140** |

A budget increase that inlines 59% more calls program-wide while making these
three WORSE is not a cost decision. That is what finally pointed at the answer.

### The answer: they are cold fallbacks, and the fast paths are already inlined

`BytecodeMethod.c` contains **zero** occurrences of `ArrayList_get`. The calls
come from `cn1_intrinsics.h`:

```c
static inline JAVA_OBJECT cn1InlListGet(..., JAVA_OBJECT owner, JAVA_INT index) {
    if(__builtin_expect((unsigned)index < (unsigned)list->..._size, 1))
        return cn1RefBlockGet(list->..._cn1Storage, index);          /* inlined */
    return java_util_ArrayList_get___int_R_java_lang_Object(...);    /* COLD */
}
```

167 inlined copies of the fast path, each carrying one cold out-of-range call.
`cn1InlListAdd` and `charAt` are the same shape. So all 432 are the *fallback*
half of exactly the two-method split the work proposed to introduce -- the same
shape as `cn1InlSbAppendStr`, and the same one Round 26 described correctly when
narrowing it created 3,264 new fallback calls.

**Reading a cold-fallback count as failed inlining is the error here**, and it is
the inverse of the mistake that would have been made by acting on it: building a
second inline path for methods that already have one.

### What is actually already implemented

- **Single-implementation devirtualization** -- `Invoke.resolveSingleTarget`,
  one resolver shared by the dependency pass and the emitter.
- **Guarded direct calls for small cones** -- `Invoke.buildGuards`,
  `CN1_MAX_GUARDS = 4`.
- **classId-switch dispatch for interfaces**, direct call per arm with a vtable
  fallback (see `virtual_java_util_List_get`, 7 arms).
- **Inline fast path + out-of-line fallback** for the hot collection and string
  operations -- the `cn1Inl*` family.

Note the sibling-arm check that ruled out "these are inlined switch copies":
if they were, `LinkedList`/`Arrays$ArrayList` arms would appear in the same
callers. They are zero in every caller. Worth keeping as a technique -- it
distinguishes an inlined multi-way dispatch from a genuine direct call cheaply.

### Deliberately not done

`@Inline` as an annotation, and deriving the split from emitted size. Both target
a blocker that does not exist. `cn1InlSbAppendStr` remains the real lesson about
intent-vs-size (its author marked it inline and clang still declined until the
BODY was narrowed in Round 26) -- but that is an argument for narrowing bodies,
not for a new annotation.

---

## Round 29: closing the hole Round 27 found, and a second one behind it

Round 27 narrowed the same-block SATB deletion barrier and then discovered that
removing that barrier OUTRIGHT left all six gc-verify self-tests and all 33
gauntlet tortures GREEN. The narrowing shipped on an argument plus an offline
proof of the arithmetic, with nothing in the VM able to contradict either.

That hole was not inherited. `cn1RefBlockMove` and self-tests 4 and 5 all landed
on 2026-09-16 in this same effort: the barrier and the tests meant to cover it
were written in the same week, by the same hand.

### Why a torture could not do it, and what works instead

Round 27 tried to build a GC torture that fails without the barrier and could
not. Reaching the hazard needs a barrier-free republication path -- an object
leaving a block mid-mark and landing somewhere already scanned without firing the
insertion barrier -- and the VM no longer has one.

So the claim is checked directly instead of through the collector's behaviour.
`cn1SatbVerifyMove` is the derivation made executable:

    every overwritten slot whose OLD value survives nowhere in the block
    afterwards must lie inside the range handed to the deletion barrier

A slot inside the range that did survive is merely conservative and is counted,
not reported. A slot outside it that vanished is a reference the snapshot was
owed. Crucially this involves no mark, no thread interleaving and no object dying
at the right moment -- it is a property of the move -- so it runs on EVERY move
rather than only under the bulk handshake, and the fault is deterministic.

    clean arm                      17,200 checks, 0 violations
    CN1_GC_FAULT=moverange          8,640 MOVE-RANGE LOST reports

The reports name the right slot: `remove(at)` shifts left with `from=at+1,
to=at`, and slot `at` holds the removed element, which is exactly what leaves.

### The second hole, found while fixing the first

The first fault run reported ZERO violations -- and so did
`CN1_GC_FAULT=halfblock`, a fault known to work. **Every existing call to
`cn1GcFaultInit` sits inside a mark or a sweep**, so a fault could only ever arm
once a collection had run. Fine for the five existing faults, which all break the
collector. Useless for one that breaks a MUTATOR path.

The failure mode is the exact one this round exists to end: the fault arm ran,
armed nothing, and produced output **identical to a clean run**. Reading that as
"no violations, the barrier is fine" would have been a false green manufactured
by a harness that never engaged -- the original hole one level up. Arming now
happens at startup, idempotent, GC-path callers unaffected.

### The shape to watch for

Three times in two rounds, something reported success because it never ran: the
gates that could not see the barrier, the fault that never armed, and (Round 26)
a selfhost gate that exited 0 after `mvn clean` deleted its classpath file. A
check satisfiable by "nothing happened" is not a check. self-test7 therefore
reads the CHECK COUNT from the clean arm as well as the violation count -- a
driver that shifted nothing would report zero violations and look perfect.

---

## Round 30: I/O was waved away, then measured, and it is not the gap

Round 27's profile put 11.8% of mutator time in open/write/close, and it was
dismissed in passing with "HotSpot does the same I/O". That was never measured,
and the split inside the 11.8% argued against it: `open` 7.0%, `close` 3.9%,
bulk `write` not in the top ten. A per-file syscall cost is exactly where an AOT
runtime's own stream implementation can differ from HotSpot's.

`IoBench` + `run-io-benchmark.sh` measure it with the translator's OWN API
shapes, because the question is whether our implementation of these calls is
slower, not whether a better API exists:

| arm | call site it mirrors | parpar | JDK 25 | ratio |
|---|---|---:|---:|---:|
| writeWhole | `Parser.writeFile`: open, ONE write, close | 15.45ms | 15.36ms | 1.006x |
| openCloseOnly | the per-file syscall path alone | 3.55 | 3.83 | 0.929x |
| readChunked | `FileInputStream` -> ASM `ClassReader` | 4.67 | 5.32 | 0.878x |
| readFully | `DataInputStream.readFully` | 4.13 | 4.48 | 0.921x |
| copyStreams | `ByteCodeTranslator.copy`, 8192B chunks | 17.96 | 21.63 | 0.830x |
| writeManySmall | `ConcatenatingFileOutputStream` | 0.58 | 4.22 | **0.138x** |

**We match or beat HotSpot on every shape.** The worst arm is 1.006x. So the
11.8% is real work that HotSpot pays equally -- the dismissal was right, and was
worth the hour to confirm rather than assert. `writeManySmall` at 0.138x is this
VM's FileOutputStream buffering small writes where HotSpot's does not: 3,200
writes are 3,200 syscalls there and far fewer here.

### The directory-size theory, also dead

`openCloseOnly` measures 8.5us per file while the profile implies far more, and
the one structural difference was that the benchmark used 400 files in a fresh
directory against the translator's ~2,933 in a single one. APFS create cost is
flat in directory size:

| files in one dir | parpar us/file | jdk25 us/file |
|---:|---:|---:|
| 400 | 39.38 | 36.66 |
| 1200 | 41.11 | 38.20 |
| 3000 | 41.63 | 41.99 |

(These include create + 30KB write + close, so they are consistent with the
8.5us open+close figure rather than contradicting it.)

### A flaw in this harness, found before trusting it

The cross-check inherited from `run-benchmark.sh` compares a checksum computed
from bytes handed to `write()` -- not bytes that reached the disk. Given that
writeManySmall is 7.5x faster precisely BECAUSE this VM buffers, a buffer that
dropped its tail on close would post an identical checksum and an even better
time. `IoBench` now reads the bytes back and compares them, and the runner
treats a MISSING verify line as failure: "the check did not run" must not read
as "the check passed". Both arms emit `IOBENCH VERIFY OK bytes=4096`.

That is the fourth instance in four rounds of a check that could report success
without having run. It is worth treating as the default hypothesis about any
green result in this tree, not as a recurring surprise.

---

## Round 31: "we win on every method" was false, and allocation is the gap

Rounds 28-30 each closed a line of inquiry by showing we were already at parity
or ahead -- inlining, dispatch, file I/O. The conclusion drawn from that ("we are
winning on every method, yet losing the total") is not a paradox, it is a
selection error: those were the arms CHOSEN, and they were not the ones that
lose. The suite that already existed says so plainly.

`run-benchmark.sh 5`, all checksums bit-identical to JDK 25:

| bench | ratio | | bench | ratio |
|---|---:|---|---|---:|
| **objectAllocation** | **3.71x** | | arrayRandom | 0.96 |
| **recursion** | **1.97x** | | mathTranscendental | 0.93 |
| **stringBuilding** | **1.72x** | | quicksort | 0.92 |
| **hashMapChurn** | **1.57x** | | valueEscape | 0.54 |
| longArithmetic | 1.09 | | intArithmetic | 1.04 |
| arraySequential | 1.05 | | **GEOMEAN** | **1.24x** |

The four losses are exactly what a translator does all day, which is also why the
self-hosting profile is FLAT: allocation cost is smeared across every function
instead of sitting in one, so no profile entry exceeds 7%. A flat profile was
read for two rounds as "no single thing to fix" when it actually meant "one thing
to fix, everywhere".

It also collapses two problems into one. Allocation being slow and peak memory
being high are the same root -- object representation and the allocation path --
not separate axes to attack separately.

### What the bump does that a TLAB bump does not

`cn1BibopFastAlloc` is already inlined by default (the `-DCN1_INLINE_ALLOC`
comment at cn1_globals.h:2057 is STALE; the real guard is the negative
`CN1_DISABLE_INLINE_ALLOC`). Per object it still pays:

- a `memset` of the body
- TWO store-releases: the mark word, and the bump cursor (`stlr` on arm64)
- a class-registry flag test and three guard loads

HotSpot pays none of these per object: its TLAB top is a plain store, and the
TLAB is bulk-zeroed at refill. The global counters here were already batched per
page-acquire, so they are not the problem.

### The zeroing half, measured

`cbench/test_alloc_zero.c`, interleaved arms, consumer touches every body so no
arm can skip work it is supposed to do:

| slot bytes | perObject | bulkPage | noZero | bulk/perObject |
|---:|---:|---:|---:|---:|
| 32 | 1.95ms | 0.51ms | 0.36ms | **0.26x** |
| 48 | 1.65ms | 0.47ms | 0.32ms | **0.28x** |
| 96 | 0.62ms | 0.46ms | 0.28ms | 0.74x |

Zeroing a 64KB page once costs about a quarter of zeroing its objects one at a
time, and lands close to not zeroing at all -- so bulk captures most of what is
available. This is HotSpot's own answer (bulk-zero at refill), not a new idea.

Two things this does NOT yet establish, and they gate any change:

- what FRACTION of the 3.71x is zeroing rather than the two `stlr` and the guard
  loads. The isolated arm says the zeroing is worth ~4x on itself, not that
  allocation gets 4x faster.
- whether a page can be bulk-zeroed safely. Arena pages come from
  `cn1HeapWindowCarve` (mmap, OS-zeroed on first touch) so a FRESH page may need
  no zeroing at all, but a RECYCLED page holds dead occupants, and the per-object
  memset comment records that skipping it was measured 2x SLOWER via floating
  garbage in the mark==-1 grace window. The free-list path must keep its zero.

Next measurement, before any edit: split the 3.71x into zeroing / ordering /
guard components by ablating each in a scratch build.

### CORRECTION to the above, same night

Two claims in Round 31 were wrong and one framing was.

**The bulk-zeroing lead is dead, and was already dead.** The body zero is ALREADY
elided where the constructor assigns every field: `CN1_FAST_NEW_NOZERO`, emitted
by `InlinableConstructor` at 129 sites. `test_alloc_zero.c` measured a real
effect and proposed an optimisation the tree already implements where it is
provable. Re-deriving an existing optimisation and then "proving" it with a
microbenchmark is worse than not looking, because the benchmark lends it
credibility.

**"objectAllocation is 3.71x" is reproducible but NOT comparable to the README.**
The reference table there is Apple M2; this host is an M4 Max. Established by
elimination, three refutations deep:

- machine load -- refuted, a quiet re-run reproduced it (3.92x, geomean 1.25x)
- under-sampling -- refuted, the documented 13 interleaved reps reproduced it
  (3.70x, geomean 1.25x)
- a regression since July -- refuted by rebuilding `9c7affa412` (the last `vm/`
  commit before August) in a worktree ON THIS MACHINE: geomean 1.25x and
  objectAllocation **4.09x**, i.e. the same total and WORSE allocation than
  current master

So nothing regressed, the measurement is sound, and the M2 table simply does not
transfer. `vm/benchmarks/README.md` now carries both tables and says so.

**What survives.** The allocation gap is real, long-standing, and *widens* on
newer silicon -- the arms that grew (allocation, stringBuilding, hashMapChurn)
are the allocation-bound ones, consistent with HotSpot's TLAB bump and young-gen
copying scaling with the core while a path costing two store-releases and a
memset does not. What does NOT survive is the claim that this is newly
discovered or that zeroing is the lever.

**The process failure worth keeping.** `vm/CLAUDE.md` already says this host
cannot resolve a 5% difference and that `objectAllocation` swung 1.201 to 0.892
in two sessions an hour apart. That file was read THIS SESSION for its memory
metric rule, and the benchmark warning in it was not applied. Before quoting a
ratio from this suite: name the hardware, and check the recorded baseline was
taken on it.

---

## Round 32: two real regressions, found after an entire bisect run on the wrong baseline

### The error first

Eight benchmark runs were spent attributing a `stringBuilding` regression to this
session's first commit. The baseline used, `003228c750`, was taken from a
`git status` snapshot at the top of the session and assumed to be the parent of
that commit. **It is not**: `7a3bcebb68^` is `5db4686983`, and roughly SEVENTY
commits separate them (Rounds 28-49 -- fused Strings, the BiBOP ceiling,
devirtualization, frameless constructors, the array-header work).

So every "before my session" comparison actually compared one commit against code
70 revisions older, and then blamed the three files in that commit. That is why
reverting each of them in turn -- `cn1_globals.h`, `nativeMethods.m`,
`ArrayList.java` -- changed nothing: none of them ever could have. Three
consecutive "the only remaining culprit is impossible" results were evidence the
ENDPOINTS were wrong, and each one was read instead as "try the next file".

**Verify the baseline is the parent before the first bisect step**, with
`git rev-parse <commit>^`, not from a status line.

### What is actually true

Measured against `5db4686983`, the real pre-session commit:

| bench | true baseline | HEAD | verdict |
|---|---:|---:|---|
| stringBuilding | 18.4ms | 18.5ms | unchanged |
| hashMapChurn | 20.7ms | 21.0ms | unchanged |
| arraySequential | 8.9ms | 9.0ms | unchanged |
| objectAllocation | 31.8ms | 27.0-32.8ms | unmeasurable |
| GEOMEAN | 1.26x | 1.25x | marginally better |

This session changed nothing measurable, in either direction.

### The regressions that ARE real, and where they live

Tracked across four points on THIS M4 Max (parpar ms -- the parpar column only;
the host column swings ~50% on these short benchmarks and its ratios are noise):

| bench | Jul `9c7affa412` | R25 `003228c750` | base `5db4686983` | HEAD |
|---|---:|---:|---:|---:|
| **stringBuilding** | 15.3 | **12.7** | **18.4** | 18.5 |
| **hashMapChurn** | **14.9** | 20.2 | 20.7 | 21.0 |
| arraySequential | 16.1 | 16.2 | **8.9** | 9.0 |

- `stringBuilding` **12.7 -> 18.4ms (+45%)**, inside Rounds 28-49.
- `hashMapChurn` **14.9 -> 20.2ms (+36%)**, between July and Round 25.
- `arraySequential` 16.2 -> 8.9ms, a real 1.8x WIN in the same window.

So the String and collection representation work bought one benchmark and cost
two, and nothing in the tree recorded it -- the geomean stayed flat because the
win cancelled the losses, which is exactly how an aggregate hides this.

### Which numbers can be trusted here

- `stringBuilding` repeats to **+/-1%** (12.7, 12.8, 12.9 on one build). Usable.
- `objectAllocation` measured **21.6, 35.9, 36.3ms on ONE binary** -- a 68%
  swing. Every delta quoted from it in Rounds 30-31 is void. Its ~3-5x gap
  against HotSpot is consistent and real; no change in it is measurable.
- RATIOS are unreliable: the host column moved 9.3 -> 13.9ms between runs of the
  same benchmark. Read the parpar millisecond column.

Next: bisect `stringBuilding` across Rounds 28-49 with the parpar column and a
verified parent at each step.

---

## Round 33: a standing matrix, and the first real answer to "why are we losing"

Bisecting was abandoned as the wrong tool: the host column on the short
benchmarks moves ~50% run to run, so a bisect over dozens of commits is a
coin-flip per step. `vm/benchmarks/run-matrix.sh` replaces it -- every workload,
several core counts, printed every session so a regression is seen the day it
lands rather than hunted weeks later.

### What it measures that nothing here did before

- **Core counts, held on BOTH sides.** `-XX:ActiveProcessorCount=N` for HotSpot,
  and a new RUNTIME `CN1_GC_MARK_THREADS` override for us (it was compile-time
  only). macOS has no taskset, so without the second one a "scaling curve" would
  throttle the JVM and leave us at 16 cores -- two experiments in one table.
- **A warm, AOT JDK 25** (JEP 483/514): recorded per workload, then linked.
  Comparing an AOT translator to a cold JVM flatters us and answers nothing.
  The cache needs a JAR -- an exploded directory is refused outright.
- **The hello corpus**, with the arguments perf-guard.sh derives for it. NOT the
  translator corpus: perf-guard's own note records the same binary winning on
  translator and losing 1.263x/1.349x on hello.

### The headline the matrix produced immediately

| | 1 core | 16 cores |
|---|---|---|
| selfhost(hello) time | 6837ms **1.13x** | 6919ms **1.12x** |
| selfhost peak memory | 1409MB **0.98x** | 1407MB **0.94x** |
| objectAllocation | 3.41x | 3.78x |
| stringBuilding | 1.83x | 2.10x |

**On the workload ParparVM exists for we are 1.12-1.13x on time and we WIN on
memory (0.94-0.98x).** The microbenchmarks are much harsher than the real thing.

And the core columns answer the standing question. **Our millisecond column is
flat from 1 to 16 cores while HotSpot's falls**: objectAllocation 2.30x -> 3.69x,
stringBuilding 1.99x -> 1.33x (the host moving, not us). HotSpot converts cores
into speed through parallel GC and JIT compiler threads; a single mutator thread
with a concurrent collector does not. So a large part of the gap on a 16-core M4
Max is parallelism we do not use, and the **1-core column is the honest proxy for
a phone** -- the platform this VM actually ships to. That is where we are
closest.

### Reading it without a quiet machine

Ratios are formed WITHIN a round from an interleaved pair, and the table reports
the MEDIAN OF PER-ROUND RATIOS. Minimising each arm independently across rounds
(the old shape) can pair round 1's parpar with round 5's host and call it a
ratio; an identical binary measured intArithmetic at 56.7ms and 88.8ms an hour
apart here, which is the size of error that admits. Arm order alternates each
round so one arm never always warms the machine for the other.

Two self-checks, because four separate "green" results this session turned out to
be checks that never ran:

- a `!` on any cell whose per-round ratios spread >15% -- it flags
  objectAllocation on both core counts, independently confirming the 68% swing
  measured by hand in Round 32.
- a **machine canary** (intArithmetic at the lowest core count) plus `uptime`.
  Measured 56.7ms cool and 88.9ms after hours of benchmarking: a 57% thermal
  drift that would otherwise read as a regression next session. Over 10% from
  the recorded reference the table says the ABSOLUTE ms are not comparable --
  the ratios still are, which is the point of pairing them.

### The trap this harness fell into while being built

`translate-and-build.sh` runs a Maven clean that removes
`selfhost-asm-classpath.txt`, so the selfhost build goes stale and
`bench-selfhost.sh` REFUSES -- into a log the arm only greps. A cached
`.selfhost-built` marker then certified a build that no longer existed and the
row printed NA. Same shape as `verify-selfhost.sh` exiting 0 having run nothing,
hit earlier in this same session and documented at the time. The build is now
done once per matrix run, after the bench builds have done their cleaning, and a
failure prints SKIPPED with the log path.

---

## Round 34: the wall-time paradox, resolved -- we are not idle, we are not parallel

The standing conflict: ParparVM uses LESS CPU than JDK 25 and takes MORE wall
time. Three explanations were proposed and each was measured rather than argued.

**Sleeping -- dead.** The runtime's own counters, which are counts and therefore
immune to machine load:

    [PACING] bibopParks=0 legacyParks=0 volumeParks=0
    [LOWMEM] parks=0 throttledAllocations=0

Not one park of any kind on a full hello-corpus translation. The mutator is never
throttled and never waits for the collector.

**Teardown -- dead.** Between the last emitted file and process exit: 0.020,
0.021, 0.027s across three runs -- 0.1% of wall. A build whose `main` ends in
`_exit(0)` (no atexit, no teardown, heap deliberately leaked) then disagreed with
itself, min -121ms and median +304ms; the physical bound settles it regardless,
since nothing after the last write takes more than ~25ms.

**Page faults -- dead for this workload.** 106,799 minor faults against JDK 25's
89,096, only 20% more, and our peak footprint is LOWER (1079MB vs 1349MB). The
earlier 2.3x-memory finding (commit 4e3e5e723d) was a runaway-heap scenario at
38.6GB, not this one.

### What it actually is

    parpar  real=21.64s  user=30.80s  sys=2.70s   -> 1.42 cores
    jdk25   real=14.27s  user=38.03s  sys=1.75s   -> 2.67 cores

**HotSpot does MORE total work and finishes sooner.** 38.03 CPU-seconds against
our 30.80, spread across 2.67 cores against our 1.42. Our mutator is the critical
path with nothing beside it but one collector thread; theirs runs ~14.3s where
ours needs ~21.6s -- about **1.5x faster** -- bought with ~23.8 CPU-seconds of
JIT and parallel GC running concurrently with it.

So both halves of the observation are true and consistent: we burn less CPU
because we do not JIT, and we take longer because there is no parallelism to hide
a slower mutator behind. The paradox was only ever a paradox if you assumed equal
parallelism.

This also matches the microbenchmarks rather than contradicting them. We are at
parity on arithmetic and arrays and 1.6-3.7x behind on allocation, string
building and hashmap churn -- which is exactly what a translator does all day, so
the mutator-side deficit concentrates in the workload that matters.

### Where that leaves the levers

Two, and only two:

1. **Make the mutator faster** on the allocation-heavy shapes. The per-allocation
   work is two store-releases, a class-registry test and three guard loads; a
   TLAB bump has none of them.
2. **Use the idle cores.** We leave ~2.5 of 4 busy cores unused at the 4-core
   mark while HotSpot saturates them. Anything that can move off the mutator
   thread -- sweeping, page formatting, zeroing ahead of the bump -- is wall time
   we currently pay in line.

Note which lever is NOT available: matching HotSpot's trick directly. Its 23.8
CPU-seconds go to JIT compilation, work an AOT VM does not have. That is an
advantage we already hold, and it is why we win on CPU while losing on wall.

---

## Round 35: objectAllocation is 73% collector, and the benchmark was never noisy

### The benchmark reports its luckiest rep

`Bench` runs 3 warmup + 5 measured reps and takes the MINIMUM. objectAllocation's
reps within one run:

    32.30  129.62  165.85   29.74  123.80      intArithmetic: 58.16 57.48 56.86 57.04 56.95

A 5x swing on identical work, while another benchmark in the SAME PROCESS holds
to 2%. Every objectAllocation figure quoted in Rounds 30-34 -- 3.44x, 3.68x,
3.71x, 4.09x -- was the fast rep. On the mean it is ~11x HotSpot, not ~3.7x.

The 50-68% "run-to-run variance" that Round 32 declared unmeasurable was never
noise: it is how many of five reps happened to overlap a collection.

### It is the concurrent collector, by 73%

`CN1_GC_TRIGGER_MB` pushed out until collections stop:

| trigger | cycles | reps (ms) | mean |
|---|---:|---|---:|
| default | 25 | 57.8 118.6 146.2 37.1 166.6 | 105.2 |
| 4096MB | 4 | 29.2 29.7 31.2 29.8 35.5 | 31.1 |
| 16384MB | 4 | 28.6 29.0 28.9 28.4 28.9 | **28.8** |

With collections suppressed the bimodality disappears and the spread falls to
**2%**. intArithmetic is flat (57-59) in every arm, so this is not drift. The
allocator is uniform and unremarkable; 73% of the benchmark is the collector.

Note the residual: 28.8ms against HotSpot's 8.2ms is still **3.5x** with the
collector entirely out of the picture. These are two separate problems, and the
nanosecond-level work in Round 31 (two store-releases, guard loads, body zero,
the CN1_FAST_NEW wrapper) was aimed at the smaller one.

### Four mechanisms eliminated, one confirmed

- **page faults** -- REFUTED and ANTI-correlated: the fastest run had the MOST
  faults (25.34ms/50,130) and the slowest the fewest (37.99ms/45,716).
- **safepoint waiting** -- REFUTED. A `[GC] force-stopped thread 1 after 250000us
  at a safepoint it never reached` looked like the answer. Rebuilding with
  CN1_GC_SAFEPOINT_WAIT_MAX_US at 250000 / 20000 / 2000 moved the mean 98.9 ->
  98.5 -> 95.8. The force-stop is a symptom.
- **SATB barriers** -- REFUTED. `-DCN1_DISABLE_SATB` measured slightly WORSE
  (mean 111.0 against 96.2), i.e. no effect outside the variance.
- **machine / mmap / parks** -- REFUTED. intArithmetic holds 0.5-2% across every
  arm; mmap, munmap and park counts are identical run to run.
- **CONFIRMED: concurrent marking competing with the mutator.** What remains
  after the above, and the trigger sweep removes the effect entirely along with
  the collections.

The shape explains it. objectAllocation creates 8M Nodes per rep that die almost
immediately; the live set is tiny and the garbage is enormous. This collector
traces the whole live heap on every cycle -- 25 of them in a run -- concurrently
with a mutator that is still allocating. HotSpot's young generation copies the
few survivors and reclaims the rest for nothing, so its collection cost scales
with SURVIVORS where ours scales with cycles over the whole heap.

### A correction this round forces

Round 34 concluded "we are not idle, we are not parallel" from bibopParks=0 and
LOWMEM parks=0, reading them as "the mutator never waits for the collector".
The mutator does not PARK -- it keeps running while the collector competes with
it for memory bandwidth, and no park counter can see that. The wall-time finding
(1.42 cores against HotSpot's 2.67) stands; the inference that the collector
therefore costs the mutator nothing does not.

### What this does NOT license

A nursery was measured in Rounds 3/5 and lost on both axes, for a reason that
still applies: promotion moved survivors into the slower legacy heap. This round
is not a retraction of that -- it is a measurement of what the current collector
costs an allocation-heavy mutator, which is a different question from whether
that particular nursery design fixes it.

## Round 26: the pending set is a no-op; the grace pass is the flood

A page-granularity "poor man's young generation": pages a thread is filling are
flagged `gcPendingOwned` and hidden from the collector, then graduate when full.
Page granularity was chosen because an unpublished object is already invisible to
the MARK (which follows references) but not to walks that visit slots BY INDEX --
so there is no per-object state and no mark-word change, which is what sank the
earlier attempt.

Measured on AllocOnly (60 reps, 8M-node list), 5 interleaved rounds, in-tree
`-DCN1_NO_PENDING` negative control (verified non-vacuous with `cmp`), both arms
checksum 179987422567680:

| arm | wall (min of 5) | peak (max of 5) |
|---|---:|---:|
| skip sweep + grace pass | 4.14s | 4216 MB |
| both walks skipped, control | 4.63s | 4217 MB |
| **skip sweep only (correct)** | **4.57s** | 4222 MB |
| skip nothing, control | 4.52s | 4232 MB |

**wall 1.011, peak 0.998 -- no win.** The 10.6% the first row shows was entirely
the grace-pass skip, and that skip is a correctness bug: GraceAudit reported
`VIOLATIONS=248 (freeSlot=248)`, live holders at the current epoch pointing at
freed BiBOP slots. Reverted; nothing of the mechanism remains in the tree.

Two results worth keeping:

1. **The sweep is not where allocation-heavy workloads pay.** Skipping it entirely
   is worth nothing measurable. The grace pass is the expensive walk: for every
   page with `gcAllocedSinceSweep`, it walks slots `0..bumpIndex` and calls
   `gcMarkObject` on every fresh object -- it treats each one as a ROOT and traces
   its whole subtree. That is where short-lived objects flood the collector.

2. **The grace pass is NOT newly falsifiable -- I first claimed it was, and that
   was wrong.** GraceAudit caught this shortcut because deleting the pass for a
   whole class of pages is a GROSS fault. The invariant that actually gates the
   allocate-black optimization is a different and much narrower one, and #5609
   measured that the verifier cannot see it: two purpose-built drivers, single-
   and four-threaded, ~100 verify passes each with the destination made
   unreachable so only the grace rule keeps it, report `violations=0` WITH THE
   BARRIER DELIBERATELY COMPILED OUT. Catching gross removal is not evidence the
   subtle window can be opened.

   Read before touching this: #5442 (why the full-registry walk exists -- the
   narrower per-epoch fresh-page scheme of #5436 left an uncovered window and
   produced silent heap corruption in a customer app, issue #5425, corrupted
   dictionary entries and an impossible NPE) and #5609 (the pass instrumented and
   found EFFICIENT: the prune skips ~9,950 pages per cycle and walks ~1,900, and
   82-91% of the slots it touches are genuinely fresh -- there is no redundant
   work to shave, and "THE GRACE PASS ITSELF IS NOT CHANGED, and that is the
   result").

   The winning change is known and already named -- allocate-black, worth 60-70%
   of the walk -- and it is blocked on ONE prerequisite: a way to drive an
   allocation into the residual window on purpose. Build that first or leave it
   closed; narrowing this rule without it has already cost a field corruption.

## Round 27: the stringBuilding/hashMapChurn "regressions" do not exist

Claimed earlier this session: stringBuilding +45% and hashMapChurn +36% regressed
over Rounds 28-49. Both are **refuted**. `ab-bench.sh` against two baselines, 5
interleaved rounds each, median of per-round paired ratios, every control row at
~1.000 so both runs are sound:

| benchmark | vs Round 35 (d733eebbf8) | vs Round 30 (58e0705206) |
|---|---:|---:|
| stringBuilding | 1.054 (5% spread) | **0.964** |
| hashMapChurn | 0.960 | **0.960** |
| arraySequential | 0.546 | 0.548 |
| objectAllocation | 0.861 (183% spread!) | 1.167 (79% spread!) |

stringBuilding is FASTER than the Round 30 clean baseline; hashMapChurn is faster
than both. Nothing to bisect.

Where the bad numbers came from: a baseline in a different configuration -- the
third instance of exactly the error `ab-bench.sh` was written to prevent, and its
header already documents the first two. The guard existed and was not used before
the claim was made. **Quote no per-benchmark ratio that did not come out of
ab-bench.sh with its control rows shown.**

The one row worth acting on is objectAllocation: it is the only benchmark that
moves between runs (0.861 then 1.167) and it is UNSETTLED in both (183%, 79%
per-round spread). It cannot be scored by this harness as it stands, so any claim
about allocation throughput measured on it -- in either direction -- is unusable.
Making that row settle is a prerequisite for the allocation work, not a side quest.

## Round 28: objectAllocation is unmeasurable at MEASURE=5, and measurable at 25

Round 27 left objectAllocation as the one row that moves between runs (0.861 then
1.167) while being UNSETTLED in both (183%, 79% per-round spread). Diagnosed.

`CommonWorkloads.objectAllocation` allocates 8M nodes but nulls the chain every
512 iterations, so the live set is tiny and a rep lasts ~34ms. Only a handful of
GC cycles fit in that window, so whether one lands inside it decides the number.
Same binary, same process, identical checksum -- consecutive reps:

    37.85  50.56  37.15  24.07  38.21
    28.03  46.86  48.35  43.33  34.66
    28.63  46.35  45.95  31.05  21.95
    37.94  45.26  33.01  22.14  44.49

**21.95ms to 50.56ms for provably identical work.** Collector interference, not
measurement noise.

ab-bench already takes min-of-5-reps per round and then the median of per-round
paired ratios -- the right shape -- but against a 2.3x-wide distribution the
min-of-5 is ITSELF a noisy order statistic. Measured floor estimates from four
processes: 24.07 / 28.03 / 21.95 / 22.14, a 27.7% spread in the floor alone.
That is the whole explanation for 0.861-vs-1.167.

MEASURE 5 -> 25, six processes, min-of-25 each:

    28.68  28.60  28.66  28.78  28.83  28.60   ->  0.80% spread

Two orders of magnitude tighter. Note the floor RISES (22 -> 28.6ms): 25
consecutive reps reach a sustained allocator steady state instead of sampling a
lucky cold window, and the higher figure is the honest one -- sustained
allocation throughput is what this row is supposed to report.

This is harness resolution, not workload tuning: the workload is untouched and
both arms see the identical change. Not landed here -- the measure phase gets 5x
longer, so the depth wants to be selective (or applied only to rows ab-bench
flags '!') rather than charged to all eleven benchmarks.

CONSEQUENCE: every allocation-throughput claim made on this row is unusable in
BOTH directions until the depth lands, including the Round 26 pending-set
numbers. Reading a 2.3x-wide distribution through a 5-sample floor is very
likely why the pending set first looked like a 10.6% win when GraceAudit then
proved the mechanism producing it was corrupting the heap.

### Round 28 addendum: the depth landed in ab-bench, and it answers both rows

`Bench` takes an optional argv[0] rep count and argv[1] benchmark filter; the
default is unchanged (11 benchmarks x 5 reps, byte-identical behaviour to before)
and the same file is javac'd for the JDK arm and translated for the parpar arm,
so both arms move together. ab-bench re-measures ONLY the rows it flags '!', at
25 reps, so the depth is never charged to the other ten benchmarks.

Validated on HEAD vs Round 30, the comparison whose shallow pass scored
objectAllocation 0.861, then 1.167, then 0.469:

    objectAllocation   base 31.09ms (+-9%)   new 19.87ms (+-7%)   ratio 0.639  settled
    stringBuilding     base 19.75ms (+-2%)   new 19.85ms (+-1%)   ratio 1.005  settled
    valueEscape        base  3.97ms (+-2%)   new  3.90ms (+-10%)  ratio 0.983  UNSETTLED

**stringBuilding is 1.005 at +-1-2%.** Dead parity with the Round 30 baseline,
which is the precise figure Round 27 could only bound loosely -- two shallow runs
of the identical A/B had disagreed 0.964 vs 1.069 while both self-reported 3-4%
spread, so the 10% '!' threshold was passing rows that are not reproducible.
**objectAllocation is 0.639 settled** -- the tree is ~36% FASTER on allocation
than Round 30, the opposite of the concern that started this.

valueEscape stays flagged and that is correct: it runs in ~4ms, so it is the next
row whose duration is too short to measure through GC quantization.

TWO BUGS IN MY OWN HARNESS CODE, both caught by its output rather than by me:

1. Spread was computed over the POOLED arms, `(max(bv+nv)-min(bv+nv))/min(...)`.
   Base and new genuinely differ, so the real improvement was added to the
   "spread" -- meaning the MORE a change helped, the more certainly the row was
   declared unmeasurable. objectAllocation and arraySequential were both reported
   STILL UNSETTLED at 83%/93% purely from this. Spread is now per-arm.
2. A filtered deep run executes that benchmark ALONE and so starts from a
   different heap state: 19.9ms isolated vs 28.6ms in-suite for identical work.
   The ratio is unaffected (both arms are filtered alike) but the absolute ms are
   not comparable to the table above them, and the output now says so. This also
   corrects the Round 28 claim that 28.6ms is "the honest sustained figure" -- it
   is the honest IN-SUITE figure; absolute ms from this row means nothing without
   its context stated.

## Round 29: the pacing growth floor is refuted by its own gate

The uncommitted `cn1PacingGrowthFloorBytes` change scaled the run-ahead bound off
host free memory (`max(512MB, fm/8)`) instead of a flat 512MB. On AllocOnly (60
reps) it measured 6.75s -> 4.18s with parks falling 61-65 -> 5-6, and peak rising
796MB -> 4197MB.

**It fails `GcOverflowSpiralIntegrationTest`, and that test exists to catch this
exact change.** A/B on this machine, translator reinstalled for each arm:

| arm | result | peak |
|---|---|---:|
| `fm/8` growth floor | **FAILED** | 4,201,619 KB |
| reverted (control) | **PASSED** | -- |

    PEAK_FOOTPRINT_KB=4201619   against BASELINE_FOOTPRINT_KB=3824
    live set: a few hundred bytes

The assertion names the mechanism unprompted: "a number this size means it is
tracking the HOST's free RAM again, and the app grows until the machine
complains ... check cn1PacingPastGrowthFloor". 4.2GB also matches the 4197MB
measured independently on AllocOnly -- two unrelated workloads, same magnitude.

REVERTED. The wall-clock win is real and is recorded here for whoever takes the
proper route, which the code comment already identified: "Removing that cost needs
short-lived garbage kept out of the heap, not a different number here."

A PROCESS NOTE, because it nearly went the other way. I twice reported these two
guard tests as not existing in this tree -- `GcOverflowSpiralIntegrationTest` and
`BibopPageFloorIntegrationTest` are both in `vm/tests/src/test/java/` and both run
from `.github/workflows/parparvm-parallel-mark.yml`. The claim came from a
`grep -rl ... | head -5` whose output was truncated after CLAUDE.md and four
generated-C copies; a truncated list was read as an exhaustive one. Had it stood,
the only argument against this change would have been judgement about a memory
trade, instead of a purpose-built test failing on the precise symptom.

Second trap in the same session: `mvn ... | tail -40` reports `$?` from `tail`,
so a failing Maven run printed `GATES_EXIT=0`. Use `set -o pipefail` and capture
Maven's own status, or a gate that never ran reads as green. Running the tests
also requires `mvn -pl ByteCodeTranslator install` FIRST -- otherwise the `tests`
module compiles against a stale jar in .m2-repo and dies on "cannot find symbol"
in files unrelated to the change.

## Round 30: cn1InlSbResize is unmeasurable and is KEPT; the adaptive depth proves itself

A/B of the working tree (cn1InlSbResize) against HEAD without it. The only
ByteCodeTranslator/src differences are cn1_intrinsics.h and InlineIntrinsics.java,
so the comparison isolates the inline.

    stringBuilding                                             ratio 1.005   5% spread
    intArithmetic      base  56.77ms (+-0%)  new  56.69ms (+-0%)  ratio 0.999  25 reps  settled
    mathTranscendental base 166.42ms (+-1%)  new 166.71ms (+-1%)  ratio 1.002  25 reps  settled
    valueEscape        base   3.55ms (+-0%)  new   3.55ms (+-0%)  ratio 1.000 199 reps  settled
    objectAllocation   base  28.17ms (+-25%) new  28.06ms (+-9%)  ratio 0.996  25 reps  UNSETTLED

**No measurable wall-clock effect.** KEPT anyway, and the reason is a standing
rule rather than this table: benchmarks guard against regression, they do not
decide whether a correct low-level optimization survives, because the gap closes
on a critical mass of individually unmeasurable wins. The change is correct, it
removes a native call, and it takes resize frames from 17% to 4.7% of profile.
Deleting correct work because a 19ms benchmark cannot resolve it is how that
critical mass never accumulates.

THE ADAPTIVE DEPTH IS VALIDATED. valueEscape is the row nothing could settle --
3.55ms, close to scheduling resolution. The duration-scaled rule gave it 199 reps
and it returned +-0% at ratio 1.000. A fixed rep count could not have done this:
25 reps of a 3.55ms workload is 89ms of measurement, which is noise, while 25 reps
of mathTranscendental is 4.2 seconds. Budget, not count.

STILL OPEN: objectAllocation did not settle at 25 reps in this run (+-25% on the
base arm) though it settled at +-5%/+-2% in two earlier ones. Its duration (~28ms)
puts it exactly at the DEEP_REPS floor, so the budget rule never actually raises
it; the floor is doing the work and 800ms is evidently not enough for this row
under load (this run averaged load 5.8). Raising DEEP_BUDGET_MS, or giving the
allocation rows their own floor, is the next harness step -- not attempted here,
because a harness change made to chase one row on one loaded run is how a
threshold gets tuned to noise.

---

## Round 31: the parallel grace pass, and a 27% regression that was TLS

**The sleep.** Profiling objectAllocation put the mutator at 72% of samples in
usleep inside cn1PacingPark, throttled against an allocation cap while ONE
thread held the cycle open running the grace pass, and all three mark helpers
sat in __psynch_cvwait for the whole run. cn1GcMutatorAssist could not help:
it returns 0 unless gcMarkActiveWorkers > 0 AND gcMarkWorklistTop > 0, and a
serial grace drain leaves both false.

**Six earlier attempts to parallelise it crashed, and the cause was found.**
It was never the drain. gcMarkWorklistPush picks its path from gcMarkLocalBuf:

    lb != 0  (every helper)   buffer locally, flush under gcMarkWorklistMutex
    lb == 0  (the producer)   gcMarkWorklist[top] = ...; top++   RAW, no mutex

The raw path carries the precondition "Serial producer: no workers can access
the shared queue in this phase". The grace pass runs on the GC thread, which
had no buffer, so every attempt broke it: a helper's flush interleaves with the
producer's read-write-increment of the same index, the helper's entry is
overwritten, and the object it named is marked but never popped -- its mark
function never runs, its children stay unmarked, and the sweep frees them under
a live holder. That is the CN1_GC_VERIFY report exactly (holder mark=4, child
mark=-8, markSite in the holder's own mark function), and it explains the result
that made no sense: locking only the POP side made the crash deterministic
(12/12) rather than fixing it, because it widened the window the push clobbers.

The fix is a local buffer on the producer. Termination needed no new protocol:
gcMarkParallelDispatch already counts the caller among gcMarkThreadCount, so
gcMarkDone cannot latch until the producer itself goes idle.

    objectAllocation, plain      12/12 clean   (was 6/12; 0/12 with a locked pop)
    CN1_GC_VERIFY                0 violations in 9 runs   (was ~1 in 3)
    run-gc-verify                GREEN, and GREEN on baseline
    run-gauntlet (JDK 25 ref)    GREEN, 33 tortures, both stop modes

Non-vacuity was probed, because a silent fallback to the serial path would pass
every one of those: the default build reports markers=4 with the producer
active, and CN1_GC_MARK_THREADS=1 correctly takes no producer path at all.

**THE 27% REGRESSION ON recursion WAS THE TLS BLOCK, NOT THE GRACE PASS.**
The first A/B scored objectAllocation 0.824 and recursion 1.269 (settled, +-0%).
Switching the parallel producer off at RUNTIME changed recursion not at all --
98-100ms either way -- which eliminated both the dispatch/join handshake and the
gcMarkRunBatch extraction and pointed at the declaration:

    static __thread struct gcMarkLocalBuffer gcMarkProducerBuf;   // several KB

The generated code touches thread-locals on every call for the frame push/pop
and the stack pointer, and recursion is nothing but calls.

    baseline                       99  100   99  100 ms
    buffer as a __thread struct   126  125  123  125 ms   +27%
    buffer as a __thread pointer   98   99  100   99 ms   recovered

GENERAL RULE, worth more than this change: keep large per-thread scratch OFF the
TLS block. A multi-KB __thread object is not free to declare; it taxes every
thread-local read in the VM.

**The win is real but NOT pinned down, and the honest number is a range.**
Re-measured after the TLS fix, the machine was carrying another checkout's
simulator at 331% CPU (load 7.1), and ab-bench reported objectAllocation
UNSETTLED at +-40%/+-38%. Hand-measured at 25 reps, interleaved with alternating
order, 8 rounds, paired ratios:

    1.109  0.909  0.906  0.887  0.916  0.957  1.035  0.961    median 0.937

So ~6% under load, 17.6% at load ~4, 6 of 8 rounds favouring the new arm. This
is expected to be core-dependent -- the whole mechanism is helpers consuming
while the walk produces, so it buys less when there are no spare cores. A quiet
machine is needed to pin it, and NO single figure from this session should be
quoted as the result.

**Dead code, stated rather than hidden:** gcMarkProducerPoll's contribute-a-batch
branch never fired in any workload measured here -- the helpers keep up, so the
shared worklist never reaches the drain threshold. It is kept as the bound
against a producer outrunning them, but it is currently unexercised.

**A pre-existing gate defect, found on the way:** run-gc-verify's self-test5
reported "BROKEN -- freelive never freed a live slot in 4 runs". That is the
non-vacuity guard firing because the fault INJECTOR never fired, not a heap
violation. It is load-sensitive under parallel marking (the same binary fired
1/3 at one moment and 8/8 at another) and the gate only makes 4 attempts.
Baseline and this branch both fired 8/8 when re-measured, so it is not this
change -- but a guard whose reliability depends on machine load will read green
when it should not, and it is worth fixing on its own.

---

## Round 32: the core-scaling matrix, and why SELFHOST read NA

run-matrix.sh 3 "1 2 4", both sides held to the same count
(-XX:ActiveProcessorCount=N for HotSpot, CN1_GC_MARK_THREADS=N for us), JDK 25
warm off its AOT cache. Machine was NOT quiet: load 17.10 at the start, another
checkout's simulator at 534% CPU. Canary intArithmetic@1 = 59.3 ms. Every
objectAllocation cell came back flagged (spread >15%) and those are the
headline numbers -- treat them as indicative only.

    benchmark              cores=1        cores=2        cores=4
    writeManySmall         0.14x          0.14x          0.14x
    valueEscape            0.52x!         0.51x          0.61x!
    readChunked            0.84x          0.91x          0.83x
    copyStreams            0.88x          0.81x          0.83x
    openCloseOnly          0.89x          0.92x          0.88x
    readFully              0.90x!         0.93x          0.93x
    quicksort              0.93x          0.89x          0.96x
    mathTranscendental     0.96x          0.95x          0.97x
    arrayRandom            0.98x          0.92x          0.97x
    writeWhole             1.00x          1.03x          1.08x
    intArithmetic          1.02x          1.03x          1.04x
    arraySequential        1.06x          1.04x          1.09x
    longArithmetic         1.07x          1.09x!         1.07x
    hashMapChurn           1.57x          1.39x          1.46x
    recursion              1.92x          1.89x          1.96x
    stringBuilding         1.97x          1.90x          1.56x!
    objectAllocation       3.79x!         2.94x!         2.69x!

Ten of seventeen rows beat JDK 25. objectAllocation scales with cores --
3.79x/2.94x/2.69x, 53.2/33.9/31.7 ms -- which is the parallel grace pass of
Round 31 doing what it was built for, and the first confirmation of it outside
the A/B. It is still the worst row by a wide margin.

**SELFHOST read NA at all three core counts, and the cause was a bug of ours.**
bench-selfhost refuses a ratio unless every arm emits byte-identical C. It
diverged, and the divergence came from this branch's frame-exit retirement:

    retireCandidates = new java.util.HashMap<TypeInstruction, Integer>();

Phase 2 numbers the guards by walking that map, and TypeInstruction overrides
neither hashCode nor equals, so iteration runs in IDENTITY HASH order -- by
allocation address. Two runs of the SAME self-hosted translator on the SAME
input emitted different C; three files differed (XMLParser, Resources,
CSSEngine), each by the same 8 lines with __cn1dead_1 and __cn1dead_2 swapped.

Latent until Round 31: the parallel grace pass moved allocation addresses,
which moved the identity hashes, which moved the iteration order. The 16-core
matrix of the previous day verified all three arms; the next day's refused.
Note what that means for the gate -- the divergence is reported against the
FIRST arm to run, so it was labelled as the jdk25 arm diverging when the
nondeterministic arm was ours. Reproducing it by running one arm twice is what
identified it; the label did not.

LinkedHashMap fixes it (insertion order is bytecode order). Second effect worth
having: the loop stops at guard >= 8, so WHICH eight sites got retired in a
method with more candidates was arbitrary per run too. Verified with three
consecutive runs of the rebuilt translator, 5871 files each, 0 differing.

**The row, once it could be measured** (3 rounds, load 6.30, parpar elapsed
spread 3.0-3.6%, jdk25 0.8-3.8% -- the tightest arm in this session):

                          cores=1    cores=2    cores=4
    vs jdk25 wall          1.024x     1.038x     1.063x
    vs jdk25 peak mem      1.013x     0.970x     0.876x
    vs jdk8  wall          0.799x     0.822x     0.830x
    vs jdk8  peak mem      0.686x     0.645x     0.596x

Peak memory against JDK 25 improves monotonically with cores and passes 1.00x
at two -- the shorter cycle leaves the mutator less time to run ahead, so time
and memory move together rather than trading off, which is the same effect the
marker-count note records. Wall clock is 1.02-1.06x and moves the WRONG way
with cores: jdk25 gains from them (5.467/5.488/5.356s median) while we stay
flat (5.600/5.699/5.694s). That is the AOT-vs-JIT throughput gap, not a
collector problem, and it is where the remaining work is.

NOT COMPARABLE, and worth stating because it is the obvious mistake to make
with these numbers: the previous day's clean SELFHOST run was at 16 cores
(1.114x wall, 0.944x peak). It cannot be used to claim this session's work
moved the figure. A same-core-count before/after is still unmeasured.

---

## Round 33: the grace pass stops enqueuing, the stalls go, the clock does not

Round 32 left objectAllocation the worst row (2.69-3.79x JDK 25). Profiling it
at HEAD: mutator 63.7% of samples in cn1PacingPark -> usleep, GC thread 70.5% in
codenameOneGCMark with 18.5% in gcMarkFlushLocal, and the three helpers ~35% in
the drain loop, ~27% in __psynch_cvwait and only ~5% inside a real mark function.
Counters: threadStallMs 3233 of wallMs 4367 (74%), 99.3% of it cause=pacingVolume;
graceMarked 165,824,029; 344M Node allocations totalling 11.0GB; occupiedMB
177-608 against liveMB 0-29; triggerMB 24 against allocatedMB 192 per cycle.

So the collector's cost is proportional to what was ALLOCATED, while HotSpot's
young collection is proportional to what SURVIVED -- about nothing here. That
ratio is the 2.7-3.8x, and no amount of marker threads changes it.

**The change.** The grace walk called gcMarkObject on every fresh slot, which
stamped the object and pushed it so the drain could later pop it and run its
mark function. Both halves are waste: the stamp buys no survival (the sweep's
grace rule keeps mark == -1 and promotes it), and the worklist trip is overhead
for an object the walk is holding. It now runs the mark function in place and
marks nothing, enqueuing only OLDER children -- which is the pass's only real
job, finding an old object reachable solely through a fresh one.

An earlier framing of this idea -- "skip fresh CHILDREN" -- was wrong and is
worth recording as such: without the skip a fresh child is simply marked and
pushed by its parent instead of by the walk, so each fresh object's mark
function runs exactly once either way and nothing is saved. The saving is in not
enqueuing at all.

**Correctness: GREEN.** CN1_GC_VERIFY 0 violations in 6 runs (31,075,613 refs,
46.4M FIELDTYPE checks, 0 findings). run-gc-verify GREEN including GraceAudit
clean and the injected grace-pass fault still detected -- the non-vacuity check
for exactly this pass. run-gauntlet GREEN, 33 tortures, both stop modes. That
matters because the change moves fresh slots from the gcLastMarkedEpoch bound to
gcGraceEpoch for page reclamation, and the sweep says both are needed.

**Result: the stalls go, the clock does not.**

    threadStallMs   3233 of 4367 (74%)  ->   323 of 1491 (22%)
    dutyPct         26.0                ->   78.3
    pacingVolume    3211ms total        ->   286ms total
    mean stall      123.5ms             ->   15.9ms
    worst stall     379ms               ->   122ms

    wall clock, median of 10 paired interleaved rounds:   1.000

    base (worklist)      min 23.77  max 36.98 ms   spread 55%
    this (direct trace)  min 29.59  max 31.18 ms   spread  5%

The bad tail went and so did the good case, because this trace is SERIAL: the
worklist had been spreading those mark functions over four markers. Confirmed by
marker-count sensitivity, 40 reps, 3 rounds -- markers=1 gives 29.5/29.6/29.7ms
and markers=4 gives 29.7/29.7/29.4ms, identical, no scaling at all, while the
base arm stays noisy in both.

**Next: parallelise the PAGE WALK.** A shared page cursor with each marker
tracing whole pages directly keeps the removed overhead AND the parallelism.
Pages are independent, which makes them a better unit than the per-object
worklist this replaced.

**Measurement caveat, stated because it limits every wall-clock figure here.**
The host carried another checkout's java at 280-330% plus a Bench.app in an iOS
Simulator at ~100% throughout, load 5-26 with one excursion to 116 that voided a
whole measurement. Paired interleaved ratios are used for that reason. The
counter deltas were taken at matched load (5.10 before, 5.05 after).

---

## Round 34: the parallelism goes back on the page axis, and the sleep ends

Round 33 removed the grace pass's worklist trip and with it the parallelism.
This hands pages out instead of objects: a shared cursor, one CAS per page, each
marker tracing whole pages in place. Pages are INDEPENDENT, which the per-object
worklist was not -- that version paid a lock, a flush and a broadcast per 256
objects to distribute work needing no coordination. A helper joins the walk at
drain-loop entry once it owns the local buffer that makes its pushes safe, and
only OLDER children ever reach the shared worklist.

Termination needed no new protocol, for the same reason the producer needed none
in Round 31: a marker still walking has not entered gcMarkWorkerDrainLoop, so it
is still counted in gcMarkActiveWorkers and gcMarkDone cannot latch under it.

**Correctness GREEN.** 12/12 plain runs, 0 violations in 9 CN1_GC_VERIFY runs,
run-gc-verify GREEN (GraceAudit clean, injected grace-pass fault still
detected), run-gauntlet GREEN, 33 tortures both stop modes. Non-vacuity probed,
because a silent fallback to serial would pass every one of those: at 4 markers
helpers join the walk, at CN1_GC_MARK_THREADS=1 none does.

**THE SLEEP IS GONE.** Both CONFORM arms run back to back, same conditions:

                        serial trace (R33)   parallel walk (R34)
    threadStallMs       802                  153
    dutyPct             67.6                 93.2
    cause=pacingVolume  719ms, 25 events     ABSENT -- 0 events
    cause=handshake     82ms, 44 events      153ms, 39 events

Nought pacing-volume stalls. The mutator no longer waits on the allocation cap
at all, which is the defect this whole line of work started from: 72% of samples
in usleep inside cn1PacingPark, three helpers parked in __psynch_cvwait, one
thread holding the cycle open.

The arc across four rounds, all on objectAllocation:

    mutator stalled     74% of wall  ->  22%  ->  effectively none
    dutyPct             26.0         ->  78.3 ->  93.2
    pacingVolume total  3211ms       ->  286  ->  0

**What is NOT established.** The wall-clock win. Release timing at 40 reps gave
markers=1 28.3/28.7/29.0/28.7ms, markers=2 25.9/24.1/29.3/26.2, markers=4
27.6/28.9/26.5/23.0, against the serial trace's flat 29.5/29.6/30.9 -- it scales
where R33 did not, and it is faster, but markers=4 swings 23.0-28.9 and the host
was never quiet. Every wall-clock figure in Rounds 33-34 was taken on a machine
carrying another checkout's java at 280-330% plus a Bench.app in an iOS
Simulator, load 5-26 with excursions to 116 and 173 that voided two whole
measurements. The counter deltas above are back-to-back and survive that; the
timing curve does not. Do not quote a scaling shape from this session.

An earlier reading of these counters DID misfire and is worth recording: a
parallel-walk run showed cause=handshake at 752ms against 37ms, which looked
like a new bottleneck introduced by the change. It was not -- that arm had been
measured against a differently loaded host and had done 199.7M graceSlotsWalked
against 141.8M. Re-measured back to back, handshake is 153ms against 82ms. Same
error as the stale-baseline ones in Rounds 31-33: two numbers from two runs.

**Next.** The remaining objectAllocation gap is no longer collector stall -- duty
is 93.2% and the mutator does not wait. It is the cost of tracing 197M fresh
objects per 25 reps at all, which is O(allocated) where HotSpot's young
collection is O(survived) and survivors here are ~nothing (liveMB 0-29 against
occupiedMB 177-608). Closing that means not walking dead fresh objects, not
walking them faster.

---

## Round 35: the first quiet-machine matrix, and where the cost actually sits

Host finally idle (load 2.2 at start; canary intArithmetic@1 = 56.6ms against
59.3ms on the loaded run). SELFHOST reports for the first time this session --
the identity-hash divergence of Round 32 was what had blocked it.

    SELFHOST            cores=1      cores=2      cores=4
    parpar wall         5450ms       5475ms       5352ms
    jdk25  wall         5161ms       5131ms       5133ms
    ratio               1.06x        1.07x        1.04x
    parpar peak         1354MB       1320MB       1364MB
    jdk25  peak         1487MB       1477MB       1517MB
    ratio               0.91x        0.89x        0.90x

**Memory beats JDK 25 by 9-11% on the real workload; wall clock trails by 4-7%.**

objectAllocation is flat in absolute terms -- 27.1/29.1/27.7ms at 1/2/4 cores --
while HotSpot, derived from the ratios, goes 13.1/8.5/7.5ms. It scales with
cores and we do not, which is the direct consequence of Round 34: the collector
is off the critical path (duty 97.7%, zero pacing stalls), so extra cores have
nothing left to buy us. Note this INVERTS the loaded-machine reading of Round 32
(3.79x -> 2.69x, apparently improving with cores); that was HotSpot unable to
use its cores either. Quiet-machine numbers are the true ones.

**A contention test that failed, and what it still showed.** Idling the
collector with CN1_GC_TRIGGER_MB=100000 made objectAllocation SLOWER (29.0ms
against 27.6ms with the collector running). The arm is invalid: with no GC the
11GB of allocation never reuses a page, so every allocation faults in fresh
memory. Do not use "disable the collector" as a mutator baseline in this VM.

The controlled half of it stands, though -- same GC work, only marker count
differs, 40 reps, 5 interleaved rounds:

    markers=1   median 27.8ms  (27.4-28.8)
    markers=2   median 25.3ms  (24.9-25.7)   <- best, and tightest
    markers=3   median 27.4ms  (21.5-28.0)
    markers=4   median 26.7ms  (26.0-26.9)

2 markers beats the default 4 by ~5%, with duty HIGHER at 4 (97.7% vs 94.2%).
The machine is 16 logical cores (12P + 4E) and this is 5 threads, so it is not
CPU oversubscription: the extra markers cost the mutator more shared cache and
memory bandwidth than they return. Real, reproducible, and ~5% -- not the 2.7x.

DO NOT retune CN1_GC_MARK_THREAD_CAP on this. The default was set on the
selfhost corpus, where the same matrix gives 5450/5475/5352ms at 1/2/4 -- 4
marginally best. One microbenchmark preferring 2 and the real workload
preferring 4 is exactly the configuration-dependence this file keeps recording.

**Where the remaining gap is.** Not the collector. The mutator's own path, and
the biggest single item in it is the OBJECT HEADER: 16 bytes
(clazz* 8 + gcMark 4 + heapPosition 4) on a 32-byte Node, i.e. HALF of every
byte this benchmark allocates. The array note above this one already records the
same thing from the other side -- 32-byte array headers are 20% of all bytes
allocated per selfhost cycle before any payload.

CORRECTION, and it invalidates the staged plan first written here. Bytes saved
per object are NOT bytes saved in the heap: BiBOP rounds every allocation up to a
size class, and the smallest class is 32 (cn1BibopClassSize = 32, 48, 64, 80,
...). A 32-byte Node is 16 header + 16 fields, so

    remove 8 header bytes  -> 24 -> still a 32-byte slot -> SAVES NOTHING
    remove all 16          -> 16 -> still a 32-byte slot -> SAVES NOTHING

The header cannot pay for itself on this object at all unless a 16-byte size
class is added alongside. The original claim here -- "type-homogeneous pages
alone take Node from 32 to 24 bytes, -25% allocation traffic" -- was wrong, and
wrong in the way that matters: it counted object bytes instead of SLOT bytes.

Partial header reduction pays only for objects that sit just above a class
boundary, which makes the whole question a histogram one:

    field bytes    hdr=16   hdr=8   hdr=0
    16 (Node)      32       32      32     (16 only if a class is added)
    32             48       48      32
    40             64       48      48
    48             64       64      48

So the prerequisites are BOTH full header removal AND a finer size class, and
neither is worth starting before the census below says how many bytes actually
move. The mutator-side argument for doing it is unchanged and still the strongest
one -- the header is half of every Node, and the array note above this round
records 32-byte array headers as 20% of all bytes allocated per selfhost cycle --
but "half the object" is not "half the heap" until the rounding is accounted for.

The measurement that must come first: a per-class histogram of (field bytes ->
slot bytes) over the selfhost corpus, evaluated at hdr = 16, 8 and 0, with and
without a 16-byte class. That gives the real ceiling. Until it exists, the
staged path is a guess.

The steps themselves, for when that number justifies them:
  1. type-homogeneous BiBOP pages -> clazz comes from the page header, one line
     shared by ~2048 objects instead of 8 bytes per object, and CN1_CLASS_OF gets
     FASTER rather than slower -- which is the trap that sinks a naive side-table
     (it is on every virtual dispatch and instanceof);
  2. heapPosition folded into page metadata;
  3. mark bits to a side bitmap -- this one pays regardless of rounding, because
     it stops marking from writing into cache lines the mutator owns, which is
     the ~5% measured above.

---

## Round 36: the header design, reasoned out rather than measured

Directive for this round: small-gain measurement on this host has been
unreliable, so decide by reasoning about mechanism and correctness, then explain
any later mismatch rather than gating the change on a noisy number.

**What the header actually costs, and where it can go.** The object header is 16
bytes: clazz* (8) + gcMark (4) + heapPosition (4). Slot histogram over the
selfhost corpus, 26.8M allocations, NET of the side table the removed bytes
would need:

    today                       hdr=16              100.00%
    clazz* -> 4-byte classId    hdr=12               98.28%
    clazz from page             hdr=8                91.14%
    gcMark+heapPos to side, 8B/slot                 102.38%   LOSES
    gcMark+heapPos to side, 0.25B/slot               91.49%
    whole header out, 0.25B/slot                     82.18%
    whole header out + 16B size class                77.31%

The rule the table teaches: per-PAGE metadata is amortised free (one word shared
by ~2048 objects), per-SLOT metadata is not (one word per object wherever it
lives). clazz is the only header field removable for free.

**Three landmines found by reading the code, each of which would have been found
late and expensively.**

1. THE STRING TWIN USES THE CLASS POINTER'S ADDRESS AS DATA.
   class__java_lang_String, _i8 and _i16 are byte-identical structs sharing one
   classId; which one an object points to IS its Latin-1 coder bit. A 4-byte
   classId cannot distinguish three structs with the same id, so the classId
   plan is dead. Page-derived class is compatible -- a page per twin keeps
   pointer identity -- which is the opposite of what the byte counts suggest.

2. EVERY LEGACY ALLOCATION IS AN ARRAY. Measured: 21,958 legacy allocations,
   21,958 arrays, 0 scalars (largest scalar seen: 0 bytes above the 2048 floor).
   So scalars are ALWAYS page-resident and their class can come from the page by
   arithmetic alone -- pageOf(o) is o & ~(PAGE_SIZE-1), no load. Arrays keep
   their clazz field and their size-classed pages, which also sidesteps arrays
   being one class at many sizes. Scalars are 153.5MB of the 169MB saving.

3. THE PER-THREAD CURRENT PAGE IS KEYED BY SIZE CLASS, NOT CLASS. That is the
   real structural cost: with type-homogeneous pages a thread alternating
   between two classes of the same size would push and re-acquire a page on
   every allocation. It has to become per-class, and CN1_FAST_NEW already passes
   &class__X as a compile-time constant, so the translator can emit a dense
   CN1_ALLOC_IDX_X and the fast path stays a direct index.

**The cost that decides whether this ships:** a current page per class per
thread is 194 classes x 64KB = ~12MB per allocating thread, against 23 size
classes x 64KB = 1.5MB today. Acceptable only because Codename One allocates
essentially on one thread (the EDT); it would be the objection on a
many-mutator-thread VM, and it is the number to watch if the class count grows.

**Plan.**
  A. page->pageClazz, per-class partial pools, per-class current page indexed by
     a translator-emitted constant. Object header UNCHANGED -- pure refactor,
     gates must stay green with no behaviour change.
  B. remove clazz* from the scalar header; CN1_CLASS_OF becomes
     pageOf(o)->pageClazz for scalars, arrays keep the field.
  C. a 16-byte size class (+4.5%, no metadata at all).

**Context for the contention finding of Round 35.** The page header already
carries a cache-line split whose note records the same effect measured there --
78.5/79.9/103.4/103.3ms at 1/2/4/8 markers before it, "32% slower purely from
adding marker threads that have idle cores to run on". Today's 2-vs-4 marker gap
(25.3 vs 26.7ms) is the residual of a known, already-attacked problem, which is
independent support for moving collector-written words off the mutator's lines.

## Round 37: the intermittent self-hosting crash was the Round 27 move barrier

The self-hosted translator failed about one run in twenty on the hello corpus at
`CN1_GC_TRIGGER_MB=4` -- a SIGSEGV or an uncaught NullPointerException, somewhere
different each time. It survived master's #5882 class-literal fix (2 failures in 60
with it cherry-picked), and a bisect over the parallel-grace commits cleared them:
99b49f66f9 failed 5/80 and 9fd88470ad 1/80, both before the fresh-child skip that
looked like the obvious suspect.

**What found it was the verifier, not the crash.** A `-DCN1_GC_VERIFY` build of the
self-hosted translator reported the defect on its first run, and with the same
signature every time:

    holder = java.util.ArrayList mark=51 (epoch+0)
    field  -> com.codename1.tools.translator.bytecodes.LineNumber mark=50
    victim = object AGED OUT by this sweep (page-resident)

A live instruction list held a LineNumber the collector had not marked this cycle.
The list's elements live in a native reference block, and the translator shifts those
blocks constantly (`remove(int)`, `add(int, E)`) -- which went through the barrier
Round 27 narrowed to "only the slot whose value leaves the block".

**The narrowing's premise was wrong.** It holds for a move in isolation and fails
against a marker scanning the same block while the memmove runs. `remove(0)`'s memmove
moves upward faster than the marker walks, overtakes it, and carries one element from
the unscanned side of the scan position to the scanned side, where neither look finds
it. The bulk handshake does not prevent this: it holds off mark START and mark
TERMINATION, not a marker already inside the mark. The arithmetic proof and the
Round 27 self-test both checked the narrowed range against the narrowed contract, so
they could only ever agree with the mistake.

A/B in one verify binary, with the narrowing switched at runtime, interleaved:

| arm | verify runs with a dangling reference |
|---|---|
| narrowed (Round 27) | 4 of 6 (6 violations) |
| whole destination range | 0 of 6 |

and on the shipping `-O3` build afterwards: **0 failures in 120**, against a rate that
predicts ~6.

**The fix** logs the old value of every overwritten slot, `[to, to+count)`, in both
same-block paths (`cn1RefBlockMove` and same-array `System.arraycopy`). That is sound
for any scan order and any store ordering: a moved value either had its old slot inside
that range, so it is logged, or it never leaves its old slot during the move, so a scan
finds it there. No insertion half is needed, for the same reason.

**The gate that can see it.** `MoveRace` rotates a 200,000-element list of old objects
with `add(remove(0))` while another thread runs collections back to back; self-test7
re-injects the narrowed barrier into `cn1RefBlockMove` with `CN1_GC_FAULT=moverange`
and requires the verifier to report dangling references. Measured: 80-100 violations
in every faulted run, none in any clean one. It replaces `ListShift`,
`cn1SatbVerifyMove` and `cbench/test_satb_range`, which verified the arithmetic of a
contract that was itself wrong.

What this gives back is Round 27's win: the per-move barrier is O(count) again during a
mark (never outside one). The cost is measured separately, on a quiet machine.

Two things worth keeping. **A crash that moves around is a missing mark; point the
verifier at the real workload before bisecting** -- a bisect at a 5% failure rate needs
60+ runs per point and still misled here, while one verify run named the class, the
holder and the epoch. And **"a property of the move alone" was the tell**: a barrier
exists because of concurrency, so an argument for weakening one that never mentions the
concurrent reader has not been made.

## Round 38: the main thread's own costs -- barrier work moved off it, locks off the sweep

Attribution from here on is **main-thread only**. The all-thread view counted the
markers' work, which runs on otherwise idle cores and costs the wall clock nothing:
the collector's own marking is 0.1% of the self-hosting main thread. What the main
thread did pay, inclusive, was file I/O 9.2% (both arms pay it), allocation 5.8%, SATB
barrier logging 5.4%, `synchronized` 4.8% and pacing parks 0.5%.

Four changes, each A/B'd interleaved against its parent on the self-hosting corpus:

| commit | what | wall | peak |
|---|---|---|---|
| 1c3c03ba97 | shifts/addAll queue their block for a collector re-scan instead of logging every reference | 0.988 | 0.895 |
| 28b760ea22 | monitor side-table reads take no lock (seqlock + type-stable entries) | 0.940 | 1.07 |
| a057f73c4d | store barriers filter fresh values inline; warm page cache sized by demand | 0.976 | 1.02 |

Matrix against warm AOT JDK 25 after them, 3 rounds, load 7 rising to 20:
**selfhost wall 0.96x / 0.95x / 0.94x and peak 0.87x / 0.90x / 0.91x at 1 / 2 / 4
cores** -- ahead on both axes at every core count, where it was 1.02-1.05x behind on
wall the round before.

The peak rise on the monitor change is unexplained by allocation (none was added); the
working hypothesis is pacing against a mutator that now runs 6% faster, not yet
measured.

### objectAllocation: the mutator is not the bottleneck any more

Profiled under sustained load the main thread spends roughly half its time parked in
`cn1PacingPark`: the collector cannot retire 256MB of young garbage per rep as fast as
the mutator makes it, because its cost follows ALLOCATION (every fresh object is traced
by the grace pass and every page is walked slot by slot three sweeps running), not the
live set, which here is at most 512 nodes. The heap sat at ~630MB. That is why this
benchmark gets worse against JDK 25 as cores are added: HotSpot's young collection
scales with cores and costs what is live.

### Trimming the allocation fast path makes it SLOWER -- measured twice, do not retry

Three reasoned-correct trims of `cn1BibopFastAlloc`, layout-robust A/B (4 alignments x
4 interleaved rounds, controls at 1.00):

- drop the `constantPoolObjects` test (make cn1BibopAlloc refuse pre-init instead) and
  move the per-object `gcAllocedSinceSweep` store to page install: **+10%**
  (21.6/29.4/21.6/21.6 -> 28.3/23.6/23.8/28.1ms);
- the same plus dropping the allocation-site class-init guard for eager classes:
  **+4%**, mixed per layout (one layout 21.8 -> 28.6ms, another 21.8 -> 20.4ms).

This agrees with the earlier finding recorded at CN1_FAST_NEW, where the guard alone
measured 11% on every layout. Removing loads and stores from this loop reshapes how the
core orders the next iteration's bumpIndex load against the previous release store to
the same word, and on this hardware that costs more than the instructions saved. Not
understood well enough to engineer; reverted.

## Round 39: the collector's per-cycle cost on the mutator, and a survival figure that lied

Matrix against warm AOT JDK 25 after it (3 rounds; load 4.8 rising to 32, so the
4-core column is the least trustworthy):

| | 1 core | 2 cores | 4 cores |
|---|---|---|---|
| objectAllocation (was 2.59x / 4.31x / 3.98x) | 1.78x | 3.26x | 2.70x |
| selfhost wall | 0.94x | 0.93x | 0.97x |
| selfhost peak | 0.93x | 0.89x | 0.97x |

What landed, each gated (gauntlet, gc-verify with all seven self-tests biting, Gate A,
60-120 self-host repro runs) and A/B'd interleaved against its parent:

- **43a4c5624a -- handshakes wake in microseconds.** Parked threads polled with
  usleep(500/1000) and the collector polled threadActive the same way, so every
  stop cost the mutator >=512us. Spin, yield, then 50us sleeps: handshake stall at
  1 marker 229ms -> 61ms (p50 512us -> 4us).
- **2c16e75897 -- the init-before-publish fast path takes recycled slots.** It only
  bumped; every allocation into a page the sweep had given a free list fell to the
  full slow path. objectAllocation 0.690 settled, and the code-layout bimodality it
  had shown for weeks disappeared -- the slow layouts were the recycled-page mode.
- **f86906296c -- generated mark functions stop stamping their object.** A leftover
  from when marking chained to Object's mark function. Since Round 33 the grace pass
  traces fresh objects by calling their mark function directly, precisely so as not
  to mark them, and the tail store marked every one: survival read the whole fresh
  generation (75-80% on a 512-node live set), the trigger doubled to its ceiling and
  the heap sat at ~600MB. Found by counting: 0 slots at the current epoch at mark
  start, millions at sweep start, ~1,200 stamps by gcMarkObject. objectAllocation
  peak 1002MB -> 92MB; time +22%, because the collector now runs as often as the
  policy means it to.

Tried and reverted, measured with the same builder on both arms:

- **Parallel page sweep across the marker pool: +19-22%** on objectAllocation, with
  and without batching the pool pushes. Spreading the sweep made the mutator slower,
  so the per-page lock was not the cost.
- **Preferring empty pages over recycled partial ones: +28%.** Reusing just-swept
  partial pages is FASTER than bumping fresh ones.

Two things settled on the way. The mutator is never parked on this workload once the
survival figure is honest (bibopParks=0, and CN1_GC_PACING_CAP_MB=4096 changes
nothing), so synthetic memory pressure is not what the remaining gap is. And more
markers help (37ms at 1, ~27ms at 2 or 4), so it is not interference from them.
What remains is 89% of the main thread inside the benchmark's own loop -- the inlined
allocator, whose fast path measured slower every time it was trimmed (Round 38).

## Round 40: the core-count axis was never applied to selfhost, and what it hid

**The matrix's selfhost rows measured one configuration three times.** run-matrix.sh
sets CN1_GC_MARK_THREADS for ParparVM and CN1_SELFHOST_JDK_OPTS
(-XX:ActiveProcessorCount) for the JVM; bench-selfhost.py filtered every CN1_
variable out of its children's environment and never read the second. Every 1/2/4
core selfhost cell in Rounds 35-39 is default markers against a default JVM. Fixed
in 607215c5db, and the first honest run found two things.

**One marker took 362-415s and 3.2GB on the hello corpus, against 4.8s at four.**
The serial marker is what Windows ships and what any 2-CPU POSIX host derives
(ncpu - 1). markStatics runs once per thread with force set and bumps recursionKey
each time, so the serial path re-walked the entire statics-reachable graph N times a
cycle through the 4096-bucket chained force-visited table: 99% of the collector's
samples in cn1ForceVisitedTestAndSet. Under SATB a marked object was pushed when it
was marked, so the serial path now discards it the way the parallel claim always
has; only -DCN1_DISABLE_SATB keeps the re-trace. One marker is now 4.81s.

Matrix after both fixes (3 rounds, quiet machine, warm AOT JDK 25):

| | 1 core | 2 cores | 4 cores |
|---|---|---|---|
| selfhost wall | 0.74x | 0.76x | 0.84x |
| selfhost peak | **2.05x** (1812MB vs 886MB) | 0.78x | 0.84x |
| objectAllocation | 2.05x | 2.19x | 3.35x |

The 1-core memory cell is the real one now, and it is not a policy knob. JDK 25 at
ActiveProcessorCount=1 selects SerialGC and peaks at ~860MB. Ours at one marker:

| ceiling | wall | peak |
|---|---|---|
| 192MB (default) | 4.67s | 1789MB |
| 128MB | 4.76s | 1439MB |
| 96MB | 4.86s | 1462MB |
| 64MB | 5.01s | 1508MB |

Below 128MB a lower ceiling makes the peak WORSE. The CN1_GC_CONFORM partition at
one marker, mid-run: footprint ~1.5GB = resident pages ~870MB (live slots ~460MB,
**dead free-listed slots ~400MB**) + native storage and side tables 270-440MB
(ArrayList/HashMap backing blocks, ~250MB live, 1.1GB churned through malloc) + a
residual growing to ~380MB. At four markers the same shape, smaller: dead slots
~220MB, residual to ~250MB. MallocNanoZone=0 and MallocSpaceEfficient=1 move the
peak by 0% and -5%. So the gap is fragmentation (dead slots on pages that cannot be
released) plus malloc residue from native storage, on top of a live set already
near the JVM's whole footprint.

At four markers the ceiling IS a clean trade, re-measured on honest survival:

| ceiling | wall vs JDK | peak vs JDK |
|---|---|---|
| 192MB | 0.92x | 0.97x |
| 128MB | 0.93x | 0.91x |
| 96MB | 0.94x | 0.85x |

A 40% survival threshold instead of 25% changed nothing. The default stays at
192MB for now: it wins both axes at 2 and 4 cores, and at 1 core no ceiling closes
the gap.

Also landed: e055d35b77, a CN1_FAST_NEW miss pops the current page's free list out
of line before the full slow path. Selfhost -1..-2% wall at a 16MB trigger, neutral
at adaptive; microbenchmarks neutral.

## Round 41: the ceiling moves to 128MB, and the branch's red tests are green again

**Trigger ceiling 192MB -> 128MB** (c00b8714b7), Shai's call on the Round 40 trade:
6-9% less peak for ~1% wall at four markers.

**Where the 1-core memory goes** (one marker, hello corpus, vmmap + malloc_history at
~1.4GB): VM_ALLOCATE 860MB is the BiBOP arena, about half of it free-listed slots on
partly used pages; malloc holds ~580MB, of which 490MB is ALLOCATED (fragmentation
only 92MB). By stack: ~165MB of whole generated C files as legacy-heap Strings
(StringBuilder.toString, String.getBytes -- over the 2KB page-object limit), ~200MB
of NativeStorage tables and reference blocks, the rest small. The second grace cycle
is not a lever any more: CN1_GC_AGING_SLACK already defaults to 0.

**SATB per-entry mutex: measured and dropped.** 0.49M lock acquisitions per
self-hosting run at four markers, 1.57M at one -- ~0.25% and ~0.8% of wall at
uncontended cost. Thread-local buffers would put entries out of the collector's
reach until a handshake, in the termination protocol, for that.

**Red tests on this branch, each root-caused:**

- gc-verify self-test5 counted a run where the fault never fired (~1 in 150) as a
  miss, and did not recognise a run the fault killed (OutOfMemoryError from a
  clobbered StringBuilder length). 79adb2a0c4.
- GcOverflowSpiral's non-vacuity witness counted mid-walk worklist drains, which the
  in-place grace trace made structurally rare; graceTraced (~171k/cycle, 2.6x the
  65536 worklist the spiral was reported on) is the witness now. GcSteadyState's
  legacy arrays were 640 bytes, sized for the old 512-byte ceiling; 2560 now.
  aafd5fd268.
- LocalReceiverTypesTest asserted try/catch refuses frameless, which 7019abafdc
  deliberately removed. eb0e60465a.
- 32 JavaScript-target tests: String's inline-storage natives had no JS binding, and
  the NativeStorage bindings did not accept the plain-number 0 an unassigned long
  field defaults to in the JS runtime. c167c57bda. 359/359.

GcSteadyState's "page heap compounding" failure was reproduced only under CPU
starvation: with every core loaded the registry reaches 8-10k pages against ~2k, in
the first half of the run, and holds. The failing run had other work started halfway
through it. Capping the warm page cache at a trigger's worth changed nothing
measurable and was not kept -- the growth is mutator run-ahead, which the pacing cap
(free RAM / 8 off a ceiling) permits by design.

## Round 42: a real single core, and Linux never swept

The one-core figures so far were emulated on the Mac (`CN1_GC_MARK_THREADS=1` with
every core still available). This round pins a Linux container to one CPU
(`taskset -c 0` inside podman; rootless podman has no cpuset controller), where the
JVM picks SerialGC itself. Self-hosting hello corpus, interleaved, 3 rounds, every
arm's output byte-identical to the JDK reference.

The first attempt was invalid, and the reason was a production bug: **no Linux build
ever reclaimed anything.** Every cycle printed "incomplete native root capture;
skipped sweep" and every ParparVM arm sat at ~3.7GB. Cause: `getThreadLocalData()`
tested `threadIdKey == 0` for "no key yet", glibc hands out key 0 first, so the main
thread got a second key and a second state and the first was orphaned in
`allThreads`. The collector signal-stopped the orphan every cycle; the handler
answered for the real state; the stop never completed. Darwin never returns key 0.
Fixed with `pthread_once` (bc5c0c769a). `GcOverflowSpiralApp` in the same container:
old runtime 1219 skipped sweeps and no finish in 300s, fixed runtime 0 and a 184MB
peak. `GcOverflowSpiralIntegrationTest` now asserts on that line.

After the fix (min wall, max RSS):

| arm | wall | vs JDK | RSS | vs JDK |
|---|---:|---:|---:|---:|
| JDK 25 (SerialGC) | 18.12s | 1.00 | 554MB | 1.00 |
| HEAD, concurrent | 20.02s | 1.10 | 1224MB | 2.21 |
| single-core WIP, STW only | 31.63s | 1.75 | 675MB | 1.22 |
| single-core WIP, generational | 15.27s | 0.84 | 817MB | 1.47 |

Rounds 2-3 overlapped a second container on other vCPUs; round 1 alone orders the
arms the same way. The generational arm is the only one under JDK on time, and it
still frees a live young object in ~25% of runs on the Mac (none of these three), so
it is not a result yet. STW without generations loses 1.75x: the full mark of an
old heap every cycle is exactly what a single core cannot afford.

## Round 43: the generational use-after-free, and which collector has levers on one core

**The bug.** The single-core generational WIP freed a live young object in ~25% of Mac
runs and 5/16-11/20 of contended Linux runs. Three instruments misled before one
answered. The quarantine build (`CN1_GC_GEN_QUAR`) poisons only finalizer-free objects,
so a wrongly freed collection kept working and hid the fault; `CN1_GC_GEN_CHECK2` does
not trace through old objects, so its "0 missed edges" covered nothing; and the verifier
checks only current-epoch holders, which excludes every untraced old parent in a minor.
What settled it: `CN1_EXP_NOSTOP` (minors trace through old objects) passed 16/16, so it
was a missing remembered-set entry; then `CN1_GC_GEN_SHADOW` -- after a minor's mark,
trace through each old object it stopped at and name every young object reached that
the minor left on a page about to be swept, with that ancestor's remember history.
All 106 misses: an ancestor remembered by the barrier DURING a major, before its
remembered-set scan, while the mutator was still running. The major took the set at scan
time and discarded it, so a young object stored mid-major into an already-traced old
object, surviving unmarked on a thread-owned page, lost its only record. Fix: a major
takes (and discards) its set at cycle start; later entries carry into the next minor.
After: 24/24 contended runs pass, shadow reports 0 misses.

Side finding, committed separately: on Linux the verifier's stack range was "frame +
1MB", which reached past the stack top into BiBOP pages and reported ~60,000 heap
references per cycle as escaped stack objects; the overflow guard assumed 8MB of a 16MB
stack. Both now come from pthread_getattr_np.

**Real single core** (podman, `taskset -c 0`, JVM on SerialGC), interleaved, min wall /
max RSS, all outputs byte-identical to the JDK reference:

| arm | wall vs JDK | RSS vs JDK |
|---|---:|---:|
| concurrent (branch head) | 0.95 | 2.16 |
| STW, no generations | 1.62-1.69 | 1.14-1.21 |
| generational, major every 2 minors | 1.04 | 1.28 |
| generational, every 4 | 0.91 | 1.34 |
| generational, every 8 (WIP default) | 0.81-0.83 | 1.44-1.53 |
| generational, every 32 | 0.80 | 2.24 |
| generational, every 8, 64MB trigger | 0.77 | 1.70 |

Reading it: the concurrent collector has no memory lever on one core. STW sets the
memory floor any policy can reach here (~1.2x) and costs 1.7x in time because every
cycle is a full mark. The generational collector is under JDK on time across the whole
useful range; its major frequency is a direct time-for-memory dial. At the WIP default the
841MB peak is 467MB of BiBOP pages (400 live, 63 dead), 191MB native side storage, 67MB
legacy, 116MB unattributed; mark+sweep is 5.2s of 11.5s over 112 cycles at a fixed 24MB
trigger. So the levers are: majors driven by old-generation growth rather than a count,
a cheaper full mark, and -- for the last 1.2x -- representation, which no collector
policy reaches. One STW run took 9m05s (correct output); unexplained, and majors share
that path.

## Round 44: pushing the single-core generational collector toward its floor

Work on `parparvm-single-core-gen-wip` (a4fb5cacd4), measured in the one-CPU container.
Each change was preceded by a measurement that named the cost:

| cost (whole run) | before | after | change |
|---|---:|---:|---|
| minor sweep | 1.48s | 0.87s | walk only young slots: bump range since last sweep + a 64-byte chunk bit per recycled slot |
| remembered native blocks | 574ms, 1.09M blocks | 142ms | `NativeStorage.setOwned(owner, ...)`: a block store remembers its owner, only when old |
| remembered cards | 826ms, 1.79M objects | 511ms, 183k | 64-byte remembered chunks instead of 1KB cards |

Curve after (JDK 25 SerialGC = 1.00; min wall, max RSS):

| young trigger / majors every | wall | RSS |
|---|---:|---:|
| 24MB / 8 minors (default) | 0.77 | 1.38 |
| 24MB / 4 | 0.84 | 1.27 |
| 48MB / 2 | 0.91 | 1.23 |
| 24MB / 2 | 0.99 | 1.24 |

Tried and dropped: majors driven by promoted bytes or by footprint growth (worse on memory
at every threshold -- RSS ratchets on Linux, so a footprint trigger cannot see what a major
frees); glibc malloc_trim / mmap / trim / arena tunables (no RSS change); mark prefetching
(majors 1,702 / 1,696 / 1,703ms at distance 0 / 4 / 8); larger young triggers (same curve).

**Where the memory floor is.** JDK 25 on this workload runs 117 young collections, 0.89s
of pause in total, and NO full collection: its old generation keeps everything ever
promoted, garbage included, and still peaks at 374MB used of 409MB committed. Ours holds
~386MB of live BiBOP slots plus ~186MB of native collection storage plus legacy arrays.
The remaining memory gap is object representation (8-byte references and 16-byte headers
against compressed 4-byte references and 12-byte headers), not collection policy.

**Where the time is.** Our minors cost ~21ms against the JDK's ~7.6ms young
collections; the rest of the GC time is majors (13 x ~130ms) and the remembered-set
re-trace of large maps (IdentityHashMap alone 324ms: one young insert re-traces the whole
map). Roughly half of each minor's young survives and is promoted on first survival,
and most of that dies as old garbage for majors to collect -- the case a tenuring age
exists for.
