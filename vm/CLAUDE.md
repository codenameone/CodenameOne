# vm/ (ParparVM)

## GC memory: measure the steady state, not the peak

Every GC workload in `vm/tests` measures a **peak under load**, and a peak cannot express
the failure mode issue #5537 reported: a heap that grows forever at a modest rate passes
`GcOverflowSpiralIntegrationTest`'s "peak < 2GB over 50 rounds" without difficulty. When
investigating memory, the question to ask is whether the growth **stops**.

`-DCN1_GC_CONFORM` adds the instrument for that. Unlike `CN1_GC_VERIFY` it changes **no**
allocator behaviour -- which matters, because `CN1_GC_VERIFY` forces
`cn1BibopReleaseOffset()` to return 0 and therefore compiles out the page-release path,
the major sweep and every `madvise` call. Those are exactly the paths a footprint
investigation is about, so they cannot be measured in a verifier build.

Build with `-DCN1_GC_CONFORM` and set `CN1_GC_PROBE=<n>` at runtime (every nth cycle;
unset = off, so probe-on and probe-off are the same binary). Two emitters:

- `[GCPROBE]` per cycle, on the GC thread after the sweep. It **partitions the
  footprint** -- `residentPgKb`, `legBlockKb`, `legTableKb`, `sideKb` -- and prints the
  residual `residKb` that the four do not account for. Read the residual first: if it
  carries the drift, the growth is not in the Java heap and every heap hypothesis is dead
  in one run. It also breaks the mark down by phase (`waitMs stackMs tdrainMs migrateMs
  satbMs poolMs graceMs drainMs`), which is what localises a lengthening pause to a
  subsystem rather than to a guess.
- `[GCPROBE-T]` once a second, atomics only. This is the series that survives a collector
  that has stopped finishing cycles -- the state in which the per-cycle emitter goes
  silent, and the state being investigated.

`vm/benchmarks/src/com/bench/GcSteadyState.java` is the churn workload, parameterised
through the environment (`CN1_WL_SECONDS`, `CN1_WL_THREADS`, `CN1_WL_DEPTH`,
`CN1_WL_BRANCH`, `CN1_WL_SLEEP_MS`, ...) because the clean target's generated `main()`
passes `JAVA_NULL` for args. Sweeping `CN1_WL_SLEEP_MS` over `{0,1,10,100,1000}` is the
cheapest discriminator between a rate problem and a retention problem, and needs no
rebuild.

Every GC ablation is a **compile-time** macro, so each A/B arm is a rebuild; use
`vm/benchmarks/translate-and-build.sh` with `CN1_BENCH_CFLAGS` (see `ab-adopt.sh`), which
is ~15s per arm. Useful arms: `-DCN1_ADOPT_POLICY=0`, `-DCN1_DISABLE_BIBOP`,
`-DCN1_BIBOP_NO_FASTSWEEP`, `-DCN1_BIBOP_NO_PAGE_RELEASE`, `-DCN1_DISABLE_SATB`,
`-DCN1_SATB_LOG_FRESH`, and `-DCN1_DISABLE_CONSERVATIVE_GC_ROOTS` (which also needs the
translator run with `-Dcn1.frameless.objects=false -Dcn1.frameless.instance=false`, so it
is confounded with a codegen change -- make it the last arm, not the first).

Two traps worth knowing before believing a number:

- **`[GC-INSTR] outOfLineAllocs=` is not an allocation count.** `CN1_FAST_NEW`'s inlined
  bump path never reaches that counter, so on a small-object workload it understates
  allocation by orders of magnitude. `CN1_ALLOC_CENSUS` counts at every entry point.
- **Physical footprint moves with the host's memory pressure.** A/B by interleaving both
  builds inside one session on a non-swapping host; two soaks an hour apart measure the
  machine (see the note at `vm/JavaAPI/src/java/lang/System.java`).

`GcSteadyStateIntegrationTest` is the gate. It asserts that the SATB log stays sized by
the live set rather than by the allocation rate, and that the page heap stops growing in
the second half of the run; then it rebuilds with `-DCN1_SATB_LOG_FRESH` and **requires
both assertions to fail**, so the gate cannot go inert.

**Under a per-process ceiling, budget headroom is not a footprint bound.** Admission
against `os_proc_available_memory()` answers only "is there budget left", so on its own it
keeps saying yes until the budget is gone and the process converges on ceiling minus
`CN1_PACING_HEADROOM_MARGIN` however small its live set is. The collector therefore also
defends a reserve — `CN1_PACING_RESERVE_SHIFT`, a quarter of the budget — by clamping how
far the mutator may run ahead of it once headroom drops inside that reserve. It is a
control loop, not a tax — `volumeParks` in the `[PACING]` report is 0 for a run that never
enters the reserve — and the whole branch is unreachable on a platform with no per-process
budget, which is why the `vm/benchmarks` numbers are untouched by it. Note the ceiling is
not special: given an 8GB budget the unbounded build rides to 7.5GB, because admission has
no footprint *target*. `-DCN1_PACING_NO_RESERVE` compiles it out for
A/B, and is what the gate's third scenario re-injects to prove it can fail.

Reach for `CN1_SIMULATE_PROC_MEMORY_LIMIT=<bytes>` to exercise any of this off-device —
without it the budgeted pacing path never runs, which is how the original bug survived.

## GC latency: the mutator's clock, not the collector's

Everything above measures MEMORY. The reporter of #5537 ended up passing all of it and still
could not use the VM: "no long term memory buildup, and no crashes, but the pauses for GC
become very frequent and very long". Nothing in the runtime measured a pause. `[GCPROBE]`
times the COLLECTOR; `waitMs` is its inverse (the collector waiting on a mutator);
`[PACING]` and `[LOWMEM]` count parks and record no duration. A build could stop every
worker for most of a run with every gate green.

`[GCSTALL]` is the other side, and like the footprint probe it is `-DCN1_GC_CONFORM` only.
Every site where a mutator can be stopped is bracketed and charged to a cause --
`pacingVolume`, `pacingBudget`, `lowMemory`, `handshake`, `pendingFull`, `nativeResume`,
`signalStop` -- with a log2-microsecond histogram behind p50/p99/max. `[GCSTALL-T]` prints
the same thing per second next to `[GCPROBE-T]`, including **dutyPct**: the share of wall
time the mutator threads were RUNNING. That single number is what the whole issue was
about, and no earlier instrument could produce it.

Read `cyclesOnDemand` / `cyclesAfterIdle` first. They say how the collector decided to
start each cycle, and unlike any pause threshold they mean the same thing on a slow runner:
a machine with fewer cores makes cycles longer, it does not make the collector idle through
demand. Under sustained churn a healthy build is essentially all on-demand.

**The defect they were added to catch.** `bibopBytesSinceGc` is zeroed at cycle START, so a
mutator re-crosses the collection trigger throughout every cycle -- and `cn1BibopMaybeGc`
discarded all of those crossings behind a `!gcCurrentlyRunning` gate. By the time a cycle
ended, every mutator was parked on the run-ahead cap and therefore allocating nothing, so
no crossing was left to raise the request; `forceGc` was false, and the GC thread took its
200ms idle wait with the whole application blocked on it. Mark was 40ms and the measured
mutator park was 212ms. The legacy trigger in `codenameOneGcMalloc` had already solved
exactly this with a per-cycle latch and says so in its comment -- the BiBOP side simply
never got the same treatment.

Fixing both halves (a latch instead of the suppression, and `gcIdleWaitMillis` answering a
pending request instead of clearing it and sleeping) measured, interleaved in one session
on the churn workload, median of three: **2.8x the search throughput, duty cycle 51% ->
90%, mean mutator stall 213ms -> 15ms, and footprint DOWN 16%** -- a collector that runs
when asked keeps less garbage, so this does not trade memory for latency.
`-DCN1_GC_NO_DEMAND_SIGNAL` restores both halves for A/B and is what scenario 6 of
`GcSteadyStateIntegrationTest` re-injects.

The request is answered only while it STILL STANDS -- the uncollected byte count at the
end of a cycle is what the mutator produced DURING it, so at or above the trigger means
the mutator is outrunning the collector and below it means the ordinary idle is right.
Answering unconditionally costs 8-9% on the allocation-heavy microbenchmarks for an
application that was never blocked; with the test, `vm/benchmarks` geomean is 1.011, and
the residual is `hashMapChurn` paying honestly for a collector that no longer sleeps
through its garbage.

Two things worth knowing before reading a number from this workload:

- **Small arrays are BiBOP objects.** `codenameOneGcMalloc` serves "small objects AND small
  arrays" from the page heap, so the search's own `int[64]` board copy never reaches
  `allObjectsInHeap`. Only allocations over `CN1_BIBOP_MAX_OBJECT` (512 bytes) take the
  legacy calloc + table-registration + extent-snapshot path -- and a real game-tree search
  crosses that line routinely, since a 15x15 board of ints is 900 bytes. `CN1_WL_BIGARRAY`
  (ints per throwaway array per node, default 0) is the knob that puts the workload on that
  path. It is a materially harder shape: at 256 the same fix is worth +78% throughput and
  -38% footprint, but duty cycle only reaches ~55%, because the per-cycle legacy costs are
  large and are NOT what the demand-signal fix addresses.
- **`RESULT=` is only a parity check in the fixed-round fixture.** The `vm/benchmarks`
  driver is time-bounded, so its `RESULT=` legitimately differs run to run and cannot be
  used to compare two builds. `vm/tests`' `GcSteadyStateApp` is fixed-round precisely so it
  can be.

### The rest of the mark, and three traps

Fixing the demand signal exposed what the collector actually spends a cycle on. Under the
legacy-heavy shape (`CN1_WL_BIGARRAY=256`, arrays over `CN1_BIBOP_MAX_OBJECT`) a 159ms mark
was 39% grace pass, 36% conservative-root snapshot and 19% per-thread drain, against a
**1.5MB live set** with a 1.9M-slot legacy table.

- **The legacy-table rescan was the per-thread drain.** `gcMarkDrain` ends every call with a
  linear walk of `allObjectsInHeap` that re-pushes each already-marked object so its mark
  function runs again. That is an OVERFLOW recovery -- `gcMarkObject` pushes every object it
  marks, and a push is dropped only when the worklist overflows -- but it ran on all
  `(threads + 3 + SATB rounds)` calls a cycle makes. Measured before the gate: 8.9 passes per
  cycle, **16.5 million slot visits and 290,000 mark functions re-run per cycle**, and across
  883 passes it found something new exactly **zero** times. Gating it on
  `gcMarkOverflowSeen` -- the gate the BiBOP half of the same loop always had -- is worth
  **+27% throughput and -21% footprint** on that shape. `-DCN1_GC_ALWAYS_RESCAN_LEGACY`
  restores it.
- **The extent sort and the search in front of it.** `cn1ConsExt` is rebuilt and re-sorted
  every cycle; the qsort alone was 34ms of a 57ms snapshot build. It is now an inlined
  introsort (1.28x libc qsort on the same data, validated element-for-element against qsort
  by `CN1_CONS_EXT_SORT_TEST`), and a Bloom filter over the 64KB address block answers the
  binary search outright -- **1.5M searches with 0 hits became 66k searches**. Neither shows
  up in end-to-end throughput; both remove work whose cost grows with the heap, which is
  what #5585 was about. `-DCN1_CONS_EXT_LIBC_SORT`, `-DCN1_CONS_EXT_NO_BLOOM`.
- **A mutator could wait a whole collection for pending-table space.** Legacy allocations go
  into a per-thread table only the collector empties, and when it filled the thread waited
  for any running cycle to FINISH, then requested another and waited for that too -- when a
  running cycle is precisely what migrates the table. Both thresholds involved come from one
  free-RAM reading taken at the first collection, so they are unreachable on any machine CI
  runs on. `CN1_SIMULATE_FREE_MEMORY` now pins that reading too (one knob, one meaning), and
  with it pinned to a device-like 16MB the fix takes the **worst** stall from 1579ms to
  136ms. `-DCN1_GC_PENDING_WAIT_FULL_CYCLE`.

Three things that cost real time here, all of them measurement rather than code:

- **Never call `System.gc()` from inside a parked wait.** It enters a Java monitor, and
  `monitorEnter` is a GC safepoint -- so the request takes the thread back OUT of the parked
  state the collector is spinning on in its own `while(threadActive)` loop, and the cycle
  that was about to migrate its table gets longer instead. Asking every 200ms from inside
  the wait turned a 55ms mean stall into 400ms. Ask once, before parking.
- **A blocked mutator is not always demand the collector can answer.** Returning 0 from
  `gcIdleWaitMillis` whenever a mutator was parked looked obviously right and was not: a
  thread parked because the process BUDGET is exhausted is waiting for memory collecting
  will not produce, and treating it as demand ran the collector back-to-back at 100% and
  starved the threads it was serving -- the `-DCN1_PACING_NO_RESERVE` arm under a ceiling
  stopped finishing at all. `cn1GcBlockedMutators` is kept for the duty-cycle figure and the
  idle decision deliberately does not read it.
- **This host cannot resolve a 5% throughput difference.** `objectAllocation` measured 1.201
  and 0.892 against the same baseline in two sessions an hour apart, and at a 3GB footprint
  the runs push the machine into swap and stop measuring the collector at all. Assert on the
  COUNTERS (`rescanSlots`, `extSearches`, `cyclesOnDemand`, stall histograms), which are
  stable, and treat any per-benchmark ratio under ~5% as noise; the whole-suite geomean over
  13 interleaved reps is 0.999.
