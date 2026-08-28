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
