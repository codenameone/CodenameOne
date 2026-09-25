# Self-hosting ParparVM

This directory builds the Java bytecode translator and ASM with ParparVM, then
compiles the generated C into a native translator. The benchmark runs complete,
fresh-process translation jobs against the same translator classes on HotSpot.

## Build

```bash
export JDK_8_HOME=/path/to/jdk8
export JDK_25_HOME=/path/to/jdk25
export JAVA_HOME="$JDK_8_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
./vm/selfhost/build-selfhost.sh -O3
```

Run these commands from the repository root. Release builds use `-O3 -flto=thin`
and the Java arithmetic flags in `build-selfhost.sh`. The build records source,
class-directory, binary, compiler and toolchain fingerprints in
`target/parpar-O3.build.json`. Source changes during a build reject its manifest.

The self-host build substitutes the classes in `stubs/` for facilities outside
its supported translation targets: JavaScript generation, archive scanning and
debug-symbol compression. It translates class directories; it does not read JARs.
The native executable locates runtime resources through `CN1_RESOURCE_PATH`.

## Reproducible comparisons

```bash
./vm/selfhost/bench-selfhost.sh \
  'vm/selfhost/target/classes;vm/selfhost/target/asm-classes' \
  ByteCodeTranslator com.codename1.tools.translator 7

python3 vm/selfhost/prepare-hello-corpus.py
./vm/selfhost/bench-selfhost.sh vm/selfhost/target/hello-corpus \
  HelloCodenameOne com.codenameone.examples.hellocodenameone 7
```

The HelloCodenameOne preparation script snapshots an **existing macOS build**
under `scripts/hellocodenameone/mac/target`. It records the source path and hash of
every application, dependency and framework class in `target/hello-corpus.json`.
It replaces that build's JavaAPI with the freshly compiled self-host JavaAPI.
This measures translation of that snapshot, not a fresh application build or
application execution.

Each comparison has a correctness preflight followed by seven measured rounds.
Arm order rotates, and each sample starts a new process. Every generated file must
match byte-for-byte across every sample and runtime. Failed jobs, timeouts, stale
builds, changing inputs, missing memory statistics and output differences reject
the comparison. A failed run retains an incomplete `results.json` and its logs.

The harness reports median/minimum/maximum elapsed time, median user+system CPU
time, and maximum total process peak memory across the measured samples. On
POSIX it waits directly for process exit, with a separate timeout watchdog, so
timeout polling does not add up to 50 ms to each elapsed-time sample. On
macOS the memory metric is `/usr/bin/time -l`'s `peak memory footprint`; on Linux
it is peak RSS. These are process metrics, not Java live-heap sizes. macOS may
require permission to collect the process statistics outside a sandbox. Run on an
otherwise idle machine, and retain all samples; do not select the fastest arm's
sample or compare an instrumented binary against a release binary.

Raw evidence is stored under `target/bench/<timestamp>/`. Those directories are
build artifacts. Historical numbers formerly in this document were not tied to
the current source fingerprints and are not a baseline for these changes.
Performance parity with HotSpot remains a measurement goal.

The [2026-09-16 regression investigation](REGRESSION-2026-09-16.md) records the
analysis, GC and string repairs, controlled experiments, failed baseline runs,
and completed comparisons against JDK 25 and JDK 8.
The [native lowering audit](NATIVE-LOWERING-2026-09-16.md) records the subsequent
aligned storage, linked ARM64 checks, allocation-registration repair, and final
whole-workload comparison, including rejected changes and remaining gaps.
The [17 September follow-up](NATIVE-LOWERING-2026-09-17.md) covers native builder
ownership across helper calls and exceptions, concurrent tracing changes, linked
assembly evidence, and the subsequent whole-workload comparison.

## The CI performance gate

`.github/workflows/parparvm-perf.yml` runs `perf-gate.py` on Linux x64, Linux arm64,
macOS arm64 and Windows x64 for every pull request that touches `vm/`. It builds the
self-hosted translator at `-O3`, and measures it translating the HelloCodenameOne
corpus against JDK 25 at 1, 2 and 4 cores (as many as the runner has):

```bash
python3 vm/selfhost/prepare-hello-corpus.py
python3 vm/selfhost/perf-gate.py --cores 1,2,4 --rounds 5
```

**JDK 25 is the unit of measure.** Each round runs both arms back to back, alternating
which goes first, and yields a paired ratio -- ParparVM's elapsed time and peak memory
over the JDK run beside it. The result is the median of those ratios. Nothing absolute
is printed or kept: a shared runner that is slow today slows both halves of a pair, so
one baseline holds on a fast runner and a slow one. Every run's output is compared byte
for byte, as in the comparisons above.

Core counts are pinned with CPU affinity on Linux and Windows. macOS has no affinity
API, so there the count is logical: both arms are told it (`CN1_GC_MARK_THREADS`,
`-XX:ActiveProcessorCount`) and neither is confined to it. That makes the macOS
one-core row a different comparison -- the JDK's compiler threads still run on the
other cores -- and the report marks it.

**The gate compares against `perf-baseline.json`**: a ratio per platform and core count,
and a tolerance per metric. A ratio more than the tolerance above its baseline fails the
job and the pull request. The report job merges every platform into one table, posts it
as a single pull-request comment that later runs update, and is red on a regression or
on a platform that produced no result.

**Calibrating.** Baselines must come from the runners that enforce them: a ratio depends
on the hardware, so one measured on a developer machine is not a baseline for CI. A
platform or core count with no entry is reported as "not gated", and the job log prints
the entry to add. When a change moves performance on purpose, update the entries in the
same pull request, from that pull request's own run.

## Native collection and string implementation

`java.util.NativeStorage` is the common private buffer interface. Its C backing
blocks carry capacity and byte accounting. Hash tables group key, value, metadata
and optional ordering slices into one allocation. Only the root slice owns the
allocation. Generated mark functions trace reference slices; generated ownership
cleanup releases the root. Replaced blocks are retired until an active mark cycle
finishes. Reference updates use the runtime's SATB barriers, including bulk move
and clear operations. Native allocation contributes to GC allocation pressure. Borrowing methods keep
the Java owner live through their final native-buffer access with a compiler
lifetime fence: a raw malloc pointer alone is not a conservative Java root.
Throwing Java finalizers cannot bypass the subsequent native cleanup chain.
Conservative roots accept byte-interior pointers. A failed native-stack capture
invalidates that collection's liveness decisions and prevents its sweep.

ArrayList, ArrayDeque, IdentityHashMap, HashMap, Hashtable and LinkedHashMap use
these buffers. HashSet and LinkedHashSet use their backing maps. Exact Vector and
Stack instances use native storage; Vector subclasses retain the protected
`elementData` array contract.

Validated foreach loops use a common native cursor emitter for array lists,
hash sets, ordered sets and hash-map key/value views. Local allocation proofs
are computed only for methods containing traversal/view calls; methods without
category-sensitive stack operations also avoid frame analysis. Proven exact
receivers omit dispatch. Field, parameter and factory results use a one-time
exact-class guard and an ordinary iterator fallback for unsupported classes. The owner stays in a GC root, and removal/modification checks
remain. Immediately consumed views on proven exact maps are eliminated.

StringBuilder uses Latin-1 bytes until an operation requires UTF-16. String
construction, appending, concatenation and character replacement preserve compact
storage when representable. Latin-1 character replacement uses a native byte
scan and copy with a fused result allocation where possible. StringBuffer delegates
under its monitor. GC store and bulk barriers discard already-marked references
before logging them; older references still enter the marking snapshot.

Stream operations are lazy, with short-circuiting and one-shot consumption.
Immediately consumed `Stream.of(array)` pipelines in straight-line methods lower
filter/map/skip/limit and count/forEach/match terminals into one C loop when their
callbacks are known lambdas. Captures use native stack structs and explicit Java
roots; callbacks call their generated implementations directly, without allocating
stream, cursor or lambda objects. Escaping pipelines, unknown callbacks, sorted,
distinct and other terminals retain the lazy fallback. Other iterator families
and escaping entry objects also retain fallback implementations. C emission alone
does not establish zero overhead or vectorization.

## Validation

```bash
python3 -m unittest discover -s vm/selfhost -p 'test_*.py'
./vm/benchmarks/run-gauntlet.sh
./vm/benchmarks/run-gc-verify.sh
```

Translator tests under `vm/tests` cover reference proofs, native/JVM collection
and stream semantics, compact strings, JavaScript fallbacks and deterministic C
floating-point literals. The native storage test compiles the actual allocation
and retirement code under AddressSanitizer and UndefinedBehaviorSanitizer.
The GC verifier requires real collection cycles and injected-fault detection;
a workload completing zero cycles does not pass.

Allocation census and GC probe builds are diagnostic tools. Their timing and peak
memory cannot substitute for measurements of the release binary. With GC probing
enabled, `[NATIVE-STORAGE]` reports live, retired and released backing-block bytes
alongside managed-heap diagnostics.
