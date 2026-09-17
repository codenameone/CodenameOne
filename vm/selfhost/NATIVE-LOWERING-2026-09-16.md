# Native lowering and release validation, 2026-09-16

This continues `REGRESSION-2026-09-16.md`. It does not establish HotSpot parity.
The work follows generated C and ARM64 assembly; small correctness fixtures are
not used as performance benchmarks. Performance comparisons translate complete,
frozen HelloCodenameOne and compiler corpora in fresh processes.

## Implementation

- Hash tables now allocate their reference and metadata regions in one aligned
  native allocation. Ordered tables include their link regions in that block.
  Each region begins at a 16-byte boundary; capacity is available from its
  prefix without a Java call. Retirement retains blocks that an active GC scan
  may still reference. The owning Java object marks contained references.
- ArrayList constructors and bulk insertion copy supported native layouts
  directly. Dense copies use `memcpy`/`memmove`; hash and ordered views scan
  their native metadata. Self-insertion needs no temporary Java array.
  Custom/subclass collection behavior retains its Java fallback.
- Direct ArrayList reads, writes, append-with-capacity, size, and native storage
  capacity lower to C expressions. The slow growth, bounds, exception, and
  mutation-barrier paths remain available.
- Foreach lowering uses an owner root and primitive cursor, including identity
  map views and sets backed by maps. Exact allocation/factory proofs omit layout
  selection. Other supported receivers select a layout once; unknown classes
  retain their iterator. Raw field/call receivers are consumed without losing
  their evaluation during later expression folding. List-typed receivers omit
  all set/map-view alternatives; they need only ArrayList and a generic fallback.
- String-key HashMap probing embeds compact String equality and calls `memcmp`
  on byte storage, avoiding virtual Java equality calls on that branch. Custom
  keys retain their hash/equality callbacks and mutation checks.
- Bounded instance field getters omit the native stack-overflow guard because
  their proven body cannot call or recurse. Framed methods cache their source
  line slot instead of reloading the current Java frame index at every update.
- Compact String encoding reads Latin-1 bytes directly for UTF-8, ASCII, and
  Latin-1. It allocates the result byte array without first creating a UTF-16
  array. Unknown encodings and wide strings retain the existing fallback. The
  conformance cases also exposed a fallback bug: valid surrogate pairs now
  produce one replacement byte, rather than two, for ASCII/Latin-1.
- Already-marked references return before redundant class-registry and worker
  TLS checks. Pointer validity is still established first. Native reference
  blocks and Java reference arrays skip tagged immediates at the scanning loop.
  Parallel markers avoid repeatedly storing the same epoch into a shared page
  header. Legacy extent sorting remains in place, without a heap-sized scratch
  allocation.

The factory proof is deliberately bounded: it retains compact call/type facts,
not ASM frame graphs. Declared field/interface types do not establish exact
runtime types. Arbitrary Java callbacks still constrain vectorization and
inlining; this change does not remove every virtual call or lambda object.

## Allocation loss discovered during validation

Repeated release self-translations failed in both the changed executable and
the preserved `indexed-native` baseline. A symbolized failure showed a live
ByteCodeClass referring to two TreeSets with `mark=-1, heapPosition=-1`. Their
former backing TreeMaps had been reclaimed and reused as HashMaps. The failure
was allocation registration, before any question of tracing native table data.

Clean-target main was treated as a native thread. The collector did not park
native threads; allocation appended to their pending table without taking the
table mutex, and migration reset its count after releasing that mutex. A new
entry could therefore disappear at the migration/reset boundary.

Native append and migration/reset now share that mutex. Generated clean-target
main registers itself as an active managed thread before entering Java. Native
host threads retain their separate registration path. The sanitizer regression
injects an append at the unlock boundary: the repaired code preserves it, while
restoring the old reset order loses it and produces the expected failure.

The first repaired release completed 100 whole self-translations, alternating
default GC and a 4 MB early-GC trigger. Every generated file matched the JDK 25
reference. This is a correctness stress run, not a timing dataset.

## Evidence locations

Local artifacts are under `target/native-code-audit/` and
`target/regression-audit/`; they are generated evidence, not checked-in binaries.

- `debug-framed.log` and `debug-framed/objects.json`: symbolized allocation-loss
  failure, live holders, and reused backing objects.
- `reproduce-indexed.log`: baseline crash on repetition 98.
- `registration-repeat/results.json`: 100 complete output-checked translations.
- `final-sanitizers.log`: seven actual-C ASan/UBSan tests, including the failing
  old-reset-order negative control.
- `frame-tests.log`: 31 frame, exception, stack-overflow, and debugger checks.
- `registration-tests.log`: 22 clean-target, debug, collection, and type-proof
  tests, including the runtime main-thread registration assertion.
- `final-receiver-tests.log`: field/call-result traversal, collection semantics,
  type proofs, and stream integration checks.
- `registration-gauntlet.log`: semantic comparisons and repeated cooperative /
  forced-stop GC/thread tests.
- `registration-gc-verify.log`: all ten GC workloads passed, with 239, 120, 21,
  4, 2, 2, 2, 4, 24, and 5 completed verifier passes respectively. All five
  injected-fault checks detected their faults.
- `final-list-layout-tests.log`: native traversal and JVM semantics passed after
  removing impossible layouts from List call sites. Generated-C assertions
  verify that those sites contain no HashMap/HashSet/IdentityHashMap alternatives.
- `final-selfhost-verify.log`: repeated native output and JVM/native output
  agree for 843 generated files (24,401,998 bytes); intentional corruption fails
  the comparison.

## Linked ARM64 checks

`final-assembly/` contains eight functions extracted from the linked O3/ThinLTO
binary, plus its source/compiler/binary manifest. This is executable assembly,
not an estimate from the C source.

- `trivial-getter.asm`: `BytecodeMethod.getMethodName` is one `ldr` and `ret`.
- `field-traversal.asm`: `updateInlinableFieldDependencies` uses indexed native
  loads and a modification-count check on its ArrayList branch. Its iterator
  calls live in the other-List fallback. Pruning impossible layouts reduced this
  function from 631 to 316 instructions, compared with the preserved
  `all-layouts-assembly/` artifact. This is a code-size observation, not a timing.
- `bulk-copy.asm`: the native insertion/copy paths call `memcpy` and `memmove`,
  rather than per-element Java add/get methods.
- `map-find.asm`: String-key probing reaches native `memcmp`; non-String keys
  retain virtual equality dispatch.
- `is-method-used.asm`: the factory-proven ArrayList traversal has no Java
  iterator call. The method still calls its actual application logic.

Source-line metadata stores, GC barriers, subtype checks for unknown elements,
and unknown-implementation fallbacks remain visible. These are not zero-cost
methods as a whole.

The subsequent `combined-assembly/` capture includes the marking and encoding
changes. `compact-encoding.asm` contains NEON `ushr.16b` and `addv.4s` instructions
for UTF-8 output sizing, a single result-allocation call, and `memcpy` for the
ASCII/Latin-1 copy. It contains no call to `toCharArray` or `charsToBytes`.

## Intermediate combined runtime validation

- `marking-sanitizers.log`: eight ASan/UBSan tests. The new extent-sort test
  compares full records with libc sorting across eight address distributions,
  ten sizes around the algorithm boundary, and forced allocation failure.
- `marking-gc-verify.log`: all ten workloads and all five injected-fault checks
  passed after the marking/sorting changes.
- `compact-encoding-final-tests.log`: 42 tests passed, covering native signatures,
  JavaScript native registration/execution, and native/JVM string conformance.
  Encoding cases cover every Latin-1 byte, slices, aliases, empty strings, wide
  strings, supplementary characters, and ownership of returned byte arrays.
- `combined-verify/results.json`: 20 complete self-translations with the 4 MB GC
  trigger and heap verification enabled; 215 completed verifier passes, no
  reported corruption, and every output matched JDK 25.
- `combined-selfhost-verify.log`: the uninstrumented release generated 843 files
  (24,407,089 bytes), byte-identical across native repetitions and to the JVM.
  The intentional-corruption negative control passed.

The first complete comparison after fixing registration is preserved in
`regression-audit/native-final-hello-221054/results.json` and
`native-final-selfhost-221345/results.json`. Its Hello median was 11.170 s versus
4.592 s on JDK 25, with maximum peak footprints of 2.001 and 1.519 GB. That missed
the performance target. A separate full-translation sample
(`final-profile/sample.txt`, output checked against JDK 25) exposed substantial
marking, pointer-validation, extent-sorting, and mutator-wait costs. This motivated
the combined marking/encoding changes above. Sampling results are diagnostic;
their elapsed times are not included in benchmark aggregates.

The radix-sort experiment was **rejected**. In its complete seven-round Hello
comparison, the combined candidate had a 12.127 s median and 2.256 GB maximum
peak footprint, versus 11.132 s and 2.143 GB for the preserved correct native
release. JDK 25 was 4.587 s and 1.525 GB. Background load varied and is recorded
per sample; this does not establish which individual change caused the
regression. The allocating sort was removed rather than accepted on an
algorithmic argument. Its source snapshot, validation, and complete measurements
remain in `radix-experiment-cn1_globals.m`, `marking-sanitizers.log`, and
`regression-audit/marking-final-hello-223407/results.json`. The corresponding
selfhost comparison is `marking-final-selfhost-223753/results.json`.

The earlier successful Hello comparison is preserved as
`regression-audit/lowered-hello-212037/results.json`. Its following selfhost
comparison, `lowered-selfhost-212356/results.json`, failed and is incomplete;
its surviving samples are not a performance result. Neither dataset describes
the final source after the registration and raw-receiver repairs.


## Final retained release

The allocating radix sort is absent from this release. The retained changes
include compact encoding, early already-marked/tagged-reference exits, and
suppression of redundant page-epoch stores. No GC scheduling or pacing setting
was changed for the comparison.

- `retained-sanitizers.log`: seven actual-C ASan/UBSan tests passed, including
  allocation registration and its failing old-order negative control.
- `retained-verify/results.json`: 20 complete self-translations under the 4 MB
  early-GC trigger, with 193 completed heap-verifier passes. All outputs matched
  JDK 25; each translation completed between eight and ten verification passes.
- `retained-gc-verify.log`: all ten workloads passed (239, 120, 20, 4, 2, 2, 2,
  4, 24, and 5 verification passes respectively); all five injected faults were
  detected. The half-traced-block fault was caught by the workload's missing-key
  check, and the dangling-reference fault by the verifier in four of four runs.
- `retained-selfhost-verify.log`: the uninstrumented release produced 843 files
  (24,405,525 bytes), byte-identical across native repetitions and to the JVM.
  Deliberate output corruption failed the comparison as required.
- `retained-assembly/`: ten linked functions, their instruction/call inventory,
  extraction script, and exact build manifest. The getter remains two
  instructions; the field traversal remains 316. Compact encoding retains NEON
  sizing, one result allocation, and native copying.

The release uses O3 and ThinLTO without GC verification instrumentation. Its
`parpar-O3.build.json` binary digest is
`825c8b81f28d3676e68423510eec22de9afea68ec222712c1900ca29607bf5cc`.
The source-tree digests are:

- Translator/runtime: `7c44806d0afde00792edc7d311460c318b94a9dc3ec73388f46678a96abd1ffb`.
- JavaAPI: `ea92a2b00d0a42e4f31c56a8dabe7dd419ced054600a001f22784efbb1dd045e`.
- Self-host stubs: `1b39e5e406ae586c00fe7357658db0a1c91b966b7ba488e8363e04a01dc73c9b`.

These are the harness's path-and-content digests, not bare-file SHA-256 values.
The immutable executable/classes are `regression-audit/native-retained-native`
and `native-retained-classes`. The comparator is the preserved, registration-fixed
`native-final-native` and `native-final-classes`, not a crashing earlier baseline.

### Whole-workload results

Seven rotating fresh-process rounds per workload, three arms, 42 measured runs
in total. Both result files are complete. Every generated file matched that
engine's JDK 25 reference. The frozen compiler and Hello inputs are the same for
all arms; this measures translation, not native C compilation or app execution.

| Workload | Engine | Median elapsed (s) | Range (s) | Median CPU (s) | Median peak (GB) | Maximum peak (GB) |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| HelloCodenameOne | Preserved correct native | 11.127 | 10.823–12.126 | 21.97 | 1.863 | 1.919 |
| HelloCodenameOne | Retained native | 10.540 | 10.196–12.265 | 20.95 | 1.883 | 1.977 |
| HelloCodenameOne | JDK 25 | 4.621 | 4.516–4.883 | 18.13 | 1.506 | 1.525 |
| Self-translation | Preserved correct native | 1.172 | 1.094–1.993 | 1.92 | 0.547 | 0.559 |
| Self-translation | Retained native | 1.160 | 1.137–1.424 | 2.02 | 0.452 | 0.468 |
| Self-translation | JDK 25 | 0.939 | 0.839–1.700 | 5.83 | 0.700 | 0.736 |

GB is decimal. Memory is whole-process peak `phys_footprint` from macOS
`time -l`, not managed-heap size. Background load was substantial and varied
(one-minute load reached 26.28 during the short self-translation jobs). Per-sample
process lists and load averages are retained. The roughly one-percent change
in self-translation elapsed time is not evidence of a speed improvement.

Hello remains **2.28 times** JDK 25's median elapsed time and has a larger peak
footprint. Self-translation is **1.24 times** JDK 25's median elapsed time, with
a smaller peak footprint. The performance/RAM target is not met across both
workloads. These results do not support calling the implementation zero overhead
or treating the assembly improvements as a completed performance solution.

Raw results and reproduction commands:

- `retained-final-hello-224916/results.json` under `target/regression-audit/`: HelloCodenameOne.
- `retained-final-selfhost-225250/results.json` under `target/regression-audit/`: Self-translation.
- `target/regression-audit/run-retained-final.py`: comparison driver, fingerprints,
  JVM references, round order, exact commands, and correctness rejection.

The remaining linked marking path (`retained-assembly/mark.asm`) still includes
TLS access and conservative pointer resolution before its early mark check. The
earlier full-translation profile identified marking/resolution and mutator waits
as major costs; it was captured before the retained marking changes and is not
a quantitative profile of this final binary. Tree collections, unsupported
receiver layouts, escaping iterators, and unfused pipelines still retain Java
objects and calls. Those limits remain work, not achieved optimizations.


## Follow-up diagnosis, 2026-09-17

A fresh whole-Hello profile of the frozen final `native-retained-native` is in
`target/native-code-audit/retained-explanation-20260917-040758/`. The driver is
`profile-retained.py`. GC cycle logging was enabled for this diagnostic run;
its elapsed time and footprint are not benchmark samples. Generated output was
checked against JDK 25 and matched.

The main-thread call tree contains 8,406 sampled observations. Summing exclusive
leaves reproduces that denominator exactly. Of those, 2,227 (26.5%) end in
`__semwait_signal`: 923 beneath `monitorEnter`, 757 beneath `cn1BibopMaybeGc`,
and 547 beneath native file open/write/close/available operations. These paths
wait for GC handshakes before resuming Java. This is sampled main-thread waiting,
not a percentage of total process CPU and not an exact duration. The log records
34 GC cycles. Ordinary file system calls also appear separately in the profile.

Across threads, conservative pointer resolution, object marking, and extent
sorting remain prominent active leaf functions. The collector's block walker
still calls `gcMarkObject` for every non-null, non-tagged reference; the general
marking path resolves untrusted pointers before checking whether they are
already marked. Moving backing arrays out of the managed heap did not remove
that per-reference machinery. The linked `retained-assembly/mark.asm` confirms
this ordering and TLS access.

StringBuilder's compact representation still uses managed byte/char arrays:
growth allocates and copies, and `toStringImpl` allocates a result copy. Native
collection growth can retain both old and new blocks until an active mark cycle
ends. Those allocations count toward whole-process footprint even though the
backing blocks are outside the Java heap. No component-by-component census of
the final release was captured, so the measured native/JDK peak-memory difference
cannot be assigned exact byte amounts among these mechanisms.

The completed Hello cohort's median process CPU totals were 20.95 s native and
18.13 s JDK 25, while elapsed medians were 10.54 s and 4.62 s. Thus the 2.28x
elapsed ratio is not evidence of 2.28x as much instruction execution. Waiting,
parallel work distribution, and runtime overhead must be considered together.
The fresh native profile establishes concrete waiting and tracing costs; it
does not provide a complete differential profile against HotSpot.
