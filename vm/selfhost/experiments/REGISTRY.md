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

**No wall-clock figure is quoted, and that is deliberate.** Every attempt to measure fell
on a host with `fileproviderd` and `bird` between them burning ~130% CPU and a load
average near 10. This file's own rule -- run `uptime` first, and this box cannot resolve
5% -- makes any number taken there worthless. The codegen change is verified; the timing
is not, and must be taken on a quiet machine before anything is claimed for it.

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
