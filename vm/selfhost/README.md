# Self-hosting ParparVM

This directory builds the Java bytecode translator with ParparVM, then compiles
the generated C into a native translator. The benchmark runs complete,
fresh-process translation jobs against the same translator classes on HotSpot.

The translator reads class files with its own `classfile` package and has no
dependency on ASM. ASM still appears here as **corpus**: `build-selfhost.sh`
fetches the ASM jars by the version `vm/pom.xml` pins and extracts them into
`target/asm-classes`, because the benchmarks and Gate A translate the translator's
classes plus ASM's. A translator built before that change still needs ASM on its
classpath, so the comparisons against older trees put `target/asm-classes` there.

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
its supported translation targets: archive scanning and debug-symbol
compression. It translates class directories; it does not read JARs. JavaScript
generation is built in: the JS backend uses the translator's own regex engine
(`regex/`), SHA-256 and `java.io`, so it compiles against `vm/JavaAPI` like the
rest of the translator.
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

## Self-hosting on JavaScript

The translator also runs as a JavaScript program. ParparVM's JavaScript target
translates it (from the same JavaAPI-compiled classes the native build uses), and
`js/run-program.js` runs the bundle under Node as a command-line program:

```bash
CN1_RESOURCE_PATH=vm/ByteCodeTranslator/src \
  node --max-old-space-size=8192 --stack-size=65500 vm/selfhost/js/run-program.js \
  <bundle-dir> clean "<inputs>" <out> <App> <package> <App> 1.0 clean none
```

Nothing in the translator knows it is not running natively. `java.io.File` and
its streams reach the file system through the runtime's file natives, which work
on whatever `jvm.fileSystem` the host installs (`run-program.js` installs one
backed by Node's `fs`; a page can use `jvm.createMemoryFileSystem()`), and
`System.getenv` answers from `jvm.env`. With no file system installed the natives
throw exactly as before, so ordinary JavaScript applications are unaffected.

Three things made the translator translatable at all, and stay load-bearing:

- **It reads class files with its own `classfile` package**, not ASM. That
  package mirrors the visitor API the translator was written against, including
  the subroutine inliner (`tree/JsrInliner`): the Codename One core was compiled
  for a target old enough to use `JSR`/`RET` for `finally`, and the translator's
  output for those methods is built from the inlined layout, so it must not drift.
  `ClassReaderConformanceTest` holds it to ASM, which survives only as that test's
  oracle and as benchmark corpus.
- **The JavaScript backend uses the translator's own regex engine** (`regex/`),
  SHA-256 and `java.io`, because JavaAPI has no `java.util.regex`,
  `java.security` or `java.nio.file`. `TranslatorRegexTest` holds the engine to
  `java.util.regex` over every operation the backend uses.
- **`verify-selfhost-js.sh` is the gate.** Gate C (the C target) and Gate S (the
  JavaScript target) require the Node-hosted translator to emit byte-identical
  output to the JVM translator. Its first runs found four JavaScript-target
  defects no test had: a String built at runtime from Latin-1 characters read back
  with U+FFxx characters, `String.getBytes` answered unsigned bytes
  (`JsLatin1StringApp`), `int` multiplication was computed as a double, so a
  product past 2^53 lost its low bits (`JsIntMultiplyApp`), and the per-method
  peephole passes rewrote string literals that looked like emitted code -- the
  translator's own patterns were the first victims (`JsStringLiteralIntegrityApp`).

## The CI performance gate

Every platform's own build measures ParparVM against JDK 25 and fails on a regression:
the Linux legs of `linux-build-run.yml` (x64, arm64), the Windows capture jobs of
`parparvm-tests-windows.yml` (x64, arm64) and the macOS job of `scripts-macos.yml`. Each
runs `ci-perf-gate.sh` after it has built and run its application, puts the table into
that platform's PR comment beside its screenshots, and fails the job in its last step:

```bash
vm/selfhost/ci-perf-gate.sh run <platform> <outDir> [--hello-workload FILE --hello-app NAME]
vm/selfhost/ci-perf-gate.sh verdict <outDir>
```

`run` fetches JDK 25 into a private directory (no later step sees a different JDK),
builds the self-hosted translator (`build-selfhost.sh -O3`) and the Bench binary
(`build-bench.sh -O3`, compiled through the same `compile-dist.sh`), and runs
`perf-gate.py`. It never fails its own step, so a regression cannot stop the screenshots
and the comment that report it; `verdict` does.

**The benchmarks**, each run once, unpinned, on all of the runner's CPUs with both
runtimes' default thread counts -- the configuration an application runs in. A hosted
runner has a fixed handful of CPUs (4 on Linux and Windows, 3 on macOS), pinning a subset
of an x64 runner's vCPUs picks hyperthread siblings rather than cores, and macOS cannot
pin at all, so a 1/2/4-core sweep there measured the runner, not the VM. `--cores 1,2,4`
still sweeps, pinned, where that means something:

| Benchmark | ParparVM arm | JDK 25 arm |
|---|---|---|
| hello | the self-hosted translator translating this build's application -- the exact translation the build just ran (`CN1_TRANSLATION_RECORD`), or on macOS the corpus `prepare-hello-corpus.py` takes from the macOS build | the same translator classes |
| translator | the self-hosted translator translating itself | the same |
| each Bench workload | `bench-O3 <reps> <workload>`, one process per workload | `java com.bench.Bench <reps> <workload>` |

**JDK 25 is the unit of measure.** Each round runs both arms back to back, alternating
which goes first, and yields a paired ratio; the result is the median. Nothing absolute
is printed or kept, so one baseline holds on a fast runner and a slow one. Translation
time is the whole process; a workload's time is its fastest measured repetition inside
the process, so JVM startup does not count against the JDK. RAM is the process peak
(peak footprint on macOS, maximum RSS on Linux, peak working set on Windows). Every run is
verified: translation output byte for byte, workload checksums across arms and rounds.

In a `--cores` sweep the count is pinned with CPU affinity on Linux and Windows. macOS
has no affinity API, so there it is logical: both arms are told it (`CN1_GC_MARK_THREADS`,
`-XX:ActiveProcessorCount`) and neither is confined to it; the table marks those rows.

**The gate compares against `perf-baseline/`**: a ratio per platform, runner CPU model
and benchmark, and a tolerance per metric. A ratio more than the tolerance away from its
baseline fails the build, in either direction -- above it is a regression, below it an
improvement that has to be recorded, or a later change could give it back without
failing anything. The comment names the benchmark, the metric and the size of the change.

**Calibrating and rebaselining.** Baselines must come from the runners that enforce them:
a ratio depends on the hardware, so one measured on a developer machine is not a baseline
for CI. Both kinds of change go into the pull request's own
`perf-baseline/pr/<number>.json`, written from the failing job's `perf-results.json`:

```bash
# a runner CPU model with no rows yet
python3 vm/selfhost/calibrate-perf-baseline.py --pr 5931 perf-results.json
# a change that moves performance on purpose
python3 vm/selfhost/calibrate-perf-baseline.py --pr 5931 --reason "why" perf-results.json
```

No pull request edits `perf-baseline/base/`. The nightly `fold` job in
`.github/workflows/perf-baseline.yml` moves merged overlays there. The one-file layout
this replaced made unrelated branches conflict on every merge; `perf_baseline.py`
explains how overlays combine and when two of them are a real conflict. A branch still
carrying edits to the old `perf-baseline.json` converts them, from a checkout of this
layout, with `perf_baseline.py import-legacy --pr N --reason "..." --ref origin/<branch>`.
It reads the branch's copy and the copy at its merge base, so only the branch's own edits
are imported.

The Port Status page's ParparVM vs JDK 25 table is rendered from these same rows
(`perf_baseline.py summary`, run by `scripts/website/build.sh`).

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
