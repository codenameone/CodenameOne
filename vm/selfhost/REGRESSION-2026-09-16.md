# Self-host regression investigation, 2026-09-16

The previous implementation was not an acceptable performance result. Moving
collection backing storage into C did not by itself reduce the compiler's
allocation work or the collector's bookkeeping. This investigation preserves
the slow executable, varies individual mechanisms, and compares fresh processes
translating identical frozen inputs.

## What went wrong and what changed

1. **The optimization itself was expensive.** `ClosedWorldTypes` eagerly built
   an ASM frame analysis and a retained whole-program constraint graph. Turning
   it off in a three-pair diagnostic reduced median Hello translation from
   26.349 to 18.746 seconds, while peak footprint remained 7.33 GB. The replacement,
   `LocalReceiverTypes`, analyzes only methods with relevant traversal calls,
   retains no graph, and proves local allocation origins. Unknown fields,
   parameters and factory results keep the existing exact-class guard. Methods
   without category-sensitive stack instructions also skip unnecessary frame
   analysis. This deliberately narrows the proof rather than assuming that a
   declared Java type identifies an exact implementation.
2. **The SATB barrier stored references already marked in the active cycle.**
   The initial instrumented Hello run recorded 305,482,863 entries in one cycle;
   its drain found all of them already marked. That cycle performed 135,898,957
   barrier lock acquisitions. Scalar and bulk barriers now reject current-epoch
   marked objects before allocating/logging entries. Unmarked older objects
   still enter the snapshot. Bulk allocation failure now also records a dropped
   snapshot entry so reference processing cannot trust an incomplete snapshot.
3. **The fixed mark queue repeatedly overflowed.** The original profile recorded
   651 recovery rescans, a `rescanUseful` counter of zero, and
   363,890,084 scanned slots. The queue now grows geometrically under its existing
   synchronization, retaining the recovery path if allocation fails. In a
   separate three-round comparison after the local-analysis repair, this reduced
   maximum peak footprint from 4.507 to 2.552 GB. Median elapsed time was 10.812
   versus 10.981 seconds and CPU time increased: this was a memory improvement,
   not an established speed improvement. The queue retains its high-water
   capacity for the process lifetime.
4. **Compact storage still used unnecessarily expensive operations.** Latin-1
   `String.replace(char,char)` now scans/copies native bytes and retains compact
   storage; widening keeps the Java fallback. Latin-1 equality now uses `memcmp`,
   and comparison skips equal native words before finding the exact unsigned
   character difference required by Java. Offset, mixed-coder and unsigned-byte
   behavior have targeted tests.
5. **Iterator escape analysis repeatedly scanned unrelated instructions.** A
   separate five-second CPU sample of the repaired build recorded 828 leaf
   samples in `Parser.iteratorStackCensus`. For every iterator implementation,
   that method walked every instruction of every method looking for allocation
   sites. It now builds an allocation-site index in one pass and performs the
   same escape checks on the indexed methods, preserving the rule that any
   escaping site disqualifies the class. A dedicated test covers repeated
   allocations in one method and a leaking site alongside a safe site.

The initial profile allocated 6,582,550,579 managed bytes in total. An intermediate
attempt to make the old whole-program analysis cheaper still allocated
6,583,114,056 bytes. That attempt did not solve the allocation problem and was
replaced; its successful timings are not reported as a completed result.

## Evidence and limits

Raw local artifacts are in `target/regression-audit/`:

- `before-native`, `before-native.build.json`, `before-classes/`, `before-c/`
  preserve the starting implementation; `before.patch` preserves the dirty diff.
- `fixed-javaapi/`, `fixed-hello/`, `fixed-selfhost/`, and `fixed-asm/` are frozen
  benchmark inputs. Hello measures translation of the existing application
  corpus, not app startup or a fresh Maven application build.
- `ablation-184533/results.json` is the initial analysis-off experiment.
- `profile.log` and `cpu-sample.txt` contain the initial GC/allocation/CPU evidence.
- `combined-profile.log` contains the unsuccessful intermediate allocation result.
- `local-compare-191958/results.json` isolates dynamic queue growth.
- `run-final.py` runs seven rotating rounds, hashes inputs/binaries/classes,
  records commands and versions, and checks every generated file against an
  independent JDK 25 translation for the corresponding translator version.

Two intermediate variants failed: `fixes-185326` has a native interface-dispatch
crash, and `combined-190412` has an inconsistent argument-array access in the old
`ClosedWorldTypes.solve`. Both comparisons are incomplete and excluded from
aggregate performance claims. Fresh debugger runs and twelve runs with added
object-shape assertions did not reproduce those failures. Their underlying cause
was not established; removing the old analysis is not proof of a GC-race repair.

The final five-arm Hello comparison (`final-hello-193943`) also failed: the
saved **before** executable crashed in its fourth attempt, in
`ClosedWorldTypes.Node.from`, called by `ClosedWorldTypes.solve`. The crash report
is preserved as `before-native-crash.ips`. The three successful baseline timings
must not be used to claim a completed before/after speedup. A separate seven-round
comparison of the repaired native runtime and both JDKs follows; it does not
silently replace the failed baseline experiment.

Instrumented profile timings are not release benchmarks. Early diagnostic arms
also overlapped unrelated builds in other checkouts and should not be compared
numerically with later cohorts. Final results use uninstrumented O3/ThinLTO native
binaries and default JDK settings, fresh processes, identical frozen inputs,
median elapsed time, and the maximum observed process peak memory footprint
reported by macOS `time -l`. GB means decimal bytes divided by 1,000,000,000.
This is a shared development machine, not an isolated benchmark host.

## Correctness checks

- Release O3/ThinLTO build succeeded; final source and host-class fingerprints
  match the release build manifest.
- 49 focused translator tests passed: local receiver proofs, collection, stream
  and string semantics, and 45 bytecode instruction integration cases.
- 10 Python tests passed, including actual native code under ASan/UBSan for
  native storage, compact comparisons, SATB filtering and mark-queue growth with
  allocation failure.
- The semantic gauntlet passed against JDK 25 and Java 8, with its documented
  four era-dependent formatting lines. Cooperative and signal-stop GC/thread
  stress repetitions also passed.
- The initial final-runtime GC gate verified the grace, legacy, bulk-copy,
  GC/thread stress, map, builder, fused-string and large-array workloads, and
  detected all five injected faults. `ThreadChurn` failed because it completed
  zero cycles. It now yields after each asynchronous GC request while retaining
  the survivor table; the gate still rejects zero cycles. Its rerun completed
  24 verified cycles.

Validation logs and focused JUnit XML reports are retained in
`target/regression-audit/validation/`. Passing checks do not establish performance
parity or explain the unreproduced intermediate corruption failures above.

## Completed comparisons before the allocation-site index

Seven fresh processes per runtime per corpus; every generated output matched
the corresponding JDK 25 reference. Both result files have `complete: true`.

| Corpus / runtime | Median elapsed | Elapsed range | Median CPU | Maximum peak footprint |
| --- | ---: | ---: | ---: | ---: |
| Hello / ParparVM | 14.545 s | 14.186–21.343 s | 36.71 s | 2.648 GB |
| Hello / JDK 25 | 6.534 s | 6.459–13.013 s | 24.89 s | 1.653 GB |
| Hello / JDK 8 | 8.592 s | 7.975–13.444 s | 34.62 s | 2.892 GB |
| Self-translation / ParparVM | 1.895 s | 1.492–2.234 s | 3.24 s | 0.761 GB |
| Self-translation / JDK 25 | 1.996 s | 1.694–2.300 s | 11.77 s | 0.714 GB |
| Self-translation / JDK 8 | 2.610 s | 2.324–3.270 s | 13.89 s | 0.566 GB |

Hardware: Apple M4 Max, 16 CPU cores, 64 GiB RAM. Background load was recorded
before and after each sample. There was a marked slowdown in the final Hello
round affecting all three runtimes; those samples remain included. Self-translation
is the frozen compiler corpus (461 JavaAPI, 102 translator, 117 ASM classes),
while Hello contains the same 461 JavaAPI classes plus 5,326 application and
dependency classes. These are distinct workloads and cannot substitute for one
another.

Hello remains **2.23 times slower and 1.60 times the maximum peak memory of
JDK 25** in this cohort. Self-translation's medians are close, with overlapping
ranges; this is not evidence that the larger-workload problem is solved. No final
before/after speedup ratio is claimed because the saved baseline crashed.

Raw results:

- `target/regression-audit/final-hello-after-194522/results.json`
- `target/regression-audit/final-selfhost-after-194937/results.json`
- Rejected baseline comparison: `target/regression-audit/final-hello-193943/results.json`

Reproduce using the preserved snapshots and inputs:

```sh
python3 vm/selfhost/target/regression-audit/run-final.py hello after-only
python3 vm/selfhost/target/regression-audit/run-final.py selfhost after-only
```

These snapshots precede the allocation-site index; the final indexed-build
comparison is recorded separately below. For subsequent source changes, rebuild and use the maintained
`bench-selfhost.sh` harness described in README.md. The audit snapshots intentionally
remain frozen and do not track future edits.

## Final source: allocation-site index included

Seven rotating rounds per corpus compare the indexed executable, its preserved
pre-index predecessor, JDK 25 and JDK 8. Both JDK preflights must generate the
same output, and every measured native/JDK sample must match it. All **56**
measured samples passed; both result files have `complete: true`.

| Corpus / runtime | Median elapsed | Elapsed range | Median CPU | Maximum peak footprint |
| --- | ---: | ---: | ---: | ---: |
| Hello / ParparVM before index | 14.601 s | 14.476–14.884 s | 36.68 s | 2.352 GB |
| Hello / ParparVM final | 13.421 s | 13.103–13.517 s | 33.09 s | 2.612 GB |
| Hello / JDK 25 final | 6.094 s | 5.844–6.354 s | 24.55 s | 1.505 GB |
| Hello / JDK 8 final | 7.530 s | 7.202–7.743 s | 31.96 s | 2.890 GB |
| Self-translation / ParparVM before index | 1.431 s | 1.418–1.592 s | 2.31 s | 0.682 GB |
| Self-translation / ParparVM final | 1.325 s | 1.314–1.361 s | 2.21 s | 0.679 GB |
| Self-translation / JDK 25 final | 1.101 s | 1.052–1.137 s | 7.71 s | 0.719 GB |
| Self-translation / JDK 8 final | 1.532 s | 1.484–1.953 s | 9.47 s | 0.545 GB |

The index reduced native Hello median elapsed time by 8.1% in this paired
comparison. Its median peak fell from 2.283 to 2.199 GB, but its maximum peak
increased from 2.352 to 2.612 GB. It is not reported as a memory improvement.
Self-translation median elapsed time fell 7.4%.

The final Hello result remains **2.20 times JDK 25 elapsed time and 1.74 times
its maximum peak memory**. Self-translation is **1.20 times JDK 25 elapsed time**,
with a slightly lower maximum peak. These results supersede the pre-index
comparison for the current source; neither supports a general claim of parity.

Raw results and reproducible snapshot commands:

```sh
python3 vm/selfhost/target/regression-audit/run-indexed.py hello
python3 vm/selfhost/target/regression-audit/run-indexed.py selfhost
```

- `target/regression-audit/indexed-hello-195600/results.json`
- `target/regression-audit/indexed-selfhost-200131/results.json`
- `indexed-native.build.json` records the final source, classes and build flags.
  Those fingerprints were checked against the current source and the preserved
  `indexed-classes/` directory.
- After the index change, `IteratorCensusTest`, `LocalReceiverTypesTest`,
  `CollectionSemanticsIntegrationTest` and `StreamApiIntegrationTest` passed.
  The index changes no C runtime code; the earlier runtime GC/sanitizer gates
  cover the same runtime implementation.
- Current-source self-host verification also passed: two native translations
  and the JVM translation produced 836 byte-identical files (23,854,032 bytes).
  The comparator detected an injected one-byte corruption. This check uses the
  current compiler classes, separately from the frozen performance corpus.

## What remains expensive

The separate final diagnostic run at
`target/regression-audit/indexed-profile-200220/` also matched JDK output. Its
five-second sample, starting three seconds after launch, recorded these leading
non-wait leaf counts: `cn1ConservativeResolve` 2,185, `gcMarkObject` 1,763,
`cn1ConsExtSortRange` 634, `IdentityHashMapIterator.next` 426, and `String.equals`
371. These are sampled stack counts across threads in that window, not percentages
of total process CPU or a whole-run allocation census. The earlier profile that
exposed the repeated iterator scan is `final-profile-195116/`.

The remaining costs include validating pointers during marking, building/sorting
conservative root metadata, and iterator paths that still use the Java API
implementation. C-backed collection storage did not eliminate those operations;
treating that storage change as sufficient was a mistake. Further work must address
these measured paths while preserving reachability and escape safety; the current
results do not justify weakening either check. The saved baseline and intermediate
corruption failures remain undiagnosed even though all final measured runs pass.
