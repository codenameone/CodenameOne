# Native lowering follow-up, 17 September 2026

## Current status

The performance target is not met. The final round27 seven-run medians are:

| Whole workload | Native time | JDK25 time | Native peak footprint | JDK25 peak footprint |
|---|---:|---:|---:|---:|
| Hello | 6.473 s | 5.019 s | 1.798 GB | 1.185 GB |
| Self-translation | 0.649 s | 0.830 s | 0.373 GB | 0.601 GB |

Every timed output matches its corresponding JDK25 reference on identical frozen
inputs. These are macOS ARM64 process peak footprints, not RSS or Java heaps.
Host activity is recorded; the runs are not represented as idle-machine trials.
Hello remains about 29% slower and uses about 52% more peak memory. The latest
batch does not demonstrate a whole-workload performance win over round25.
All final correctness/pressure/fault gates pass. Earlier sections are historical
snapshots, including failed experiments and regressions.

The preceding implementation left substantial runtime work in the path: managed
StringBuilder backing arrays, repeated conservative validation during tracing,
mutators held through transitive marking, and per-byte console system calls.
Closed-world knowledge alone did not remove any of those costs. This change
implements the missing ownership and code-generation paths and checks the linked
ARM64 executable.

## Implementation

* StringBuilder uses native primitive storage. Heap builders carry 16 inline
  bytes; larger buffers grow with aligned native allocation/reallocation. Growth,
  widening, trimming, copying, and Latin-1 output preserve Java behavior. The
  collector sees one leaf owner and its cleanup function, without a backing array
  or buffer tracing callback.
* Escape-proven builders use bounded native stack buffers. Native overflow
  blocks have explicit ownership scopes, released on normal returns, repeated
  construction at one loop site, and Java exception unwind. Merely replacing a
  managed buffer with malloc would have leaked these stack owners; GC finalizers
  cannot reclaim objects absent from the heap. The generated self-host executable
  initially recorded 380 such construction sites (the starting snapshot).
* A conservative interprocedural proof permits borrowing helper parameters.
  Retaining/returning helpers, unresolved dispatch, native methods, unsupported
  flow, and unresolved recursive proof cycles retain heap allocation. Proofs are
  frozen before instruction rewriting, so an optimized-away load cannot produce
  a vacuous ownership proof. Array descriptors consume one reference slot,
  including long[] and double[].
* Reference-free classes get a null tracing callback, with inheritance and native
  reference buffers included in that decision. Native cleanup remains present.
  Generated Java finalizer calls retain exception handling; native buffer cleanup
  no longer enters a redundant outer setjmp frame.
* Authoritative Java roots carry precise provenance through the mark queue.
  Their descendants bypass conservative address/class-registry lookup. Raw stack
  words and grace survivors retain validation. Embedded primitive arrays have an
  explicit lifetime tag and are never independently aged.
* With SATB enabled, managed threads resume after root capture. Transitive tracing
  runs concurrently; existing byte/footprint allocation pacing remains. Builds
  without SATB retain the previous hold. This changes no pacing thresholds.
* Marker state shares one TLS object. The release ARM64 gcMarkObject contains one
  TLS resolver call. Allocation extents use in-place radix partitioning with
  bounded stack scratch instead of comparison sorting; no heap-sized scratch
  buffer is introduced.
* NSLogOutputStream uses a bulk fwrite on C targets. The prior putchar loop made
  one system call per byte because clean-target stdout is unbuffered. Output is
  still immediate and unchanged; the benchmark does not suppress logging. The
  Objective-C path also respects the requested byte offset.

## Evidence

Artifacts live under `target/native-code-audit/`; immutable executable, class and
input snapshots live under `target/regression-audit/`. Build manifests hash the
runtime/compiler/API sources, executable, host classes and JavaAPI classes.

* `closed-verification.log` and `closed-verify/results.json`: 20 complete
  self-translations with a 4 MB GC trigger and heap verification, five completed
  verification passes each, every emitted file matching JDK25.
* `marker-sanitizers.log`: eight actual-C ASan/UBSan checks, including native
  growth/shrink/allocation failure, ownership accounting and unwind, and extent
  sorting. Sort cases check complete record preservation across random,
  ascending, descending, equal, clustered and duplicate keys.
* `borrowed-builder-codegen.log`: native/JDK string conformance, with generated-C
  assertions that a borrowing helper keeps stack allocation and a retaining
  helper does not. Exercises overflow, Latin-1 widening/recompression, trimming,
  self-append, surrogate pairs, 1,000 exception unwinds, and console slices.
* `closed-assembly/`: 16 functions extracted from the linked release executable.
  The trivial getter is `ldr; ret`; primitive resizing calls realloc; collection
  fast paths load native storage; console output calls fwrite. Remaining fallback
  dispatch is visible in the artifact and is not claimed to have disappeared.

The intermediate `native-storage-final-*` cohort ran while other host activity
rose substantially: Hello ranged from 7.94–14.71 s natively and 4.54–8.11 s on
JDK25. It is retained as evidence, not used as a clean before/after claim. Its
whole-workload profile, `scoped-profile-20260917-045930/`, identified the TLS,
extent-sort and console-write costs addressed afterward. Sampling counts across
worker threads are not process CPU percentages.

The round14 release passed the complete GC gate, including all five fault
controls, and the throwing-finalizer workload. Self-host identity covered 843
files (24,457,488 bytes), repeat execution and a corrupted-output negative control.

The seven-run round14 cohort remained short of the Hello target:

| Whole workload | Native median time | JDK25 median time | Native median peak | JDK25 median peak |
| --- | ---: | ---: | ---: | ---: |
| Hello translation | 9.503 s | 6.562 s | 1.983 GB | 1.508 GB |
| Self-translation | 1.479 s | 1.697 s | 0.560 GB | 0.714 GB |

These are observed results under competing host activity, not idle-machine or
controlled before/after claims. All 28 timed outputs matched their JVM reference.
Reports: `target/regression-audit/native-closed-final-hello-051809/results.json`
and `native-closed-final-selfhost-052028/results.json`.

The subsequent diagnostic census allocated 12,556,466 Strings (1,289,708,064
bytes) and 1,506,415 ArrayList iterators. Its instrumented process footprint is
not substituted for release benchmark memory. The matching profile still showed
work in devirtualization during class parsing, GC marking and String equality.

The follow-up batch addresses those remaining paths:

* String equality has an inline Latin-1 memcmp path with identity, class, length
  and cached-hash checks. Mixed coders preserve the native fallback.
* Iterator lifetime follows control-flow successors and exception handlers until
  redefinition. Primitive local-slot reuse no longer prevents native traversal;
  jumping around a redefinition cannot falsely prove that the iterator is dead.
* Fused array block sizing removes a redundant pointer-width reservation. Actual
  payload placement is unchanged and tested against exactly sized allocations.
* A fused String instance whose backing bytes are inside its own allocation is
  marked as a leaf without being queued for a separate child traversal. Shared
  or separate backing arrays retain tracing; heap verification still inspects
  their fields directly.
* Virtual-call dependencies are resolved after class loading, in the existing
  dependency rescan. Parsing no longer repeatedly rebuilds the subclass index
  over an incomplete program. Cleanup invalidates that index between runs.

The follow-up pressure checks use the `bounded-verify-native` snapshot:
20 self-translations (five verification passes each) and three complete Hello
translations (10, 11 and 11 passes), with all emitted files matching JDK25.
`compact-leaf-final-tests.log` records 54 compiler/integration tests passing;
`bounded-leaf-sanitizers.log` records ten actual-C ASan/UBSan checks passing.

The first String leaf prototype failed pressure verification because address
adjacency does not establish shared ownership. That failed run is retained under
`compact-verify/`. The corrected shortcut checks BiBOP ownership, the allocation
extent and the embedded-array tag before treating a String as a leaf. A dedicated
negative test covers a separate array immediately following the String slot.
`bounded-assembly/mark.asm` confirms all three checks survive compilation.

`bounded-assembly/` also contains linked method lookup and instruction emission:
Latin-1 equality reaches memcmp directly, mixed coders retain the native fallback,
and the Invoke/CustomInvoke emission loops contain no iterator calls. The marker
still has one TLS resolver call. This does not claim that every virtual call in
the program is eliminated.

The round16 release still missed the Hello target. Seven alternating rounds per
workload, with every output checked, recorded:

| Whole workload | Native median time | JDK25 median time | Native median peak | JDK25 median peak |
| --- | ---: | ---: | ---: | ---: |
| Hello translation | 10.078 s | 6.621 s | 1.936 GB | 1.464 GB |
| Self-translation | 1.151 s | 1.172 s | 0.603 GB | 0.705 GB |

Reports: `target/regression-audit/native-bounded-final-hello-055054/results.json`
and `native-bounded-final-selfhost-055312/results.json`. Different host activity
prevents interpreting the round14/round16 difference as a controlled regression.
The Hello comparison within this cohort is enough to establish that the target
remained unmet. JavaScript validation also passed 186 tests with one skip;
the complete GC verifier and self-host identity/negative-control gates passed.

A separate allocation diagnostic on the whole Hello workload found 4,980,146
String allocations, down from the previous diagnostic's 12,556,466, but also
4,630,251 small-object allocator bypass events. The runtime's survivor policy
was routing blocks of 65,536 allocations per affected size class to individual
legacy allocations. The same policy prevented fused String allocation, forcing
separate String and byte-array allocations. At the final recorded cycle,
1,082,414 legacy table entries included only 46,289 adopted page slots: most
were actual individually allocated objects. This contradicts the earlier
hypothesis that adopted page slots explained the legacy count.

The next runtime change removes that diversion. Small objects consistently use
aligned pages, retaining genuine allocation-failure fallback and the existing
collection triggers, memory-pressure pacing and run-ahead limits. Indexed
adopted page objects also no longer receive redundant conservative extents;
unindexed pages retain the extent fallback. Diagnostic heap accounting now
excludes adopted interior slots from its malloc-backed object totals.

The allocation diagnostic is retained under
`target/native-code-audit/bounded-allocation-sample-20260917-055657/`; it matched
JDK25 output. Its instrumented elapsed time and footprint are not release
performance measurements. The round17 snapshots are `regression-audit/native-pages-native` and
`native-code-audit/page-route-verify-native`. Twenty self-translations completed
six or seven verifier passes each, and three Hello translations completed 13
passes each, with every emitted file matching JDK25. Eleven actual-C sanitizer
checks pass. The release self-host gate matched 843 files (24,514,888 bytes),
including repeat execution and the corruption negative control.

`page-route-assembly/` records linked ARM64 allocation, marker, map and traversal
functions. The survivor-bypass path is gone. Fused Strings still call the general
page allocator; this evidence does not establish zero allocation overhead.

The normal small-object memory gate remains stable. Its old no-reserve negative
control no longer reached its synthetic ceiling, so the pressure-specific cases
now use the existing oversized-array fixture, which explicitly exercises legacy
allocation. The unchanged reserve assertion then observes actual pacing under
pressure. The full heap-integrity gate is green (`page-route-gc-verify.log`), including
all five fault injections. It ran from a copy of the gate using frozen round17
compiler resources and JavaAPI classes, avoiding concurrent writes to Maven's
class directory. The pressure-gate changes and release comparison are recorded below.

The benchmark harness also replaces POSIX timeout polling with a blocking wait
and a separate watchdog, avoiding up to 50 ms of exit-detection delay. Failed and
timed-out jobs still fail the comparison; all four harness tests pass. Earlier
cohorts retain their original timing method.


The round17 controlled cohort used the previous and new native runtimes on the
same compiler bytecode, resources and input classes, with seven rotating rounds.
The Hello medians were 10.366 s / 1.967 GB before, 10.153 s / 1.836 GB after,
and 6.989 s / 1.453 GB for JDK25. The maximum native footprint increased from
2.196 GB to 2.323 GB. The corresponding self-translation medians were 1.165 s /
0.559 GB before, 1.130 s / 0.440 GB after, and 1.180 s / 0.640 GB for JDK25.
The larger workload therefore still missed both targets. Peak means macOS
`time -l` peak memory footprint, not Java heap size or RSS. Reports are
`native-pages-final-hello-062753/results.json` and
`native-pages-final-selfhost-063133/results.json` under `target/regression-audit`.

The next whole-workload profile exposed an ordering error in code generation:
native traversal rewrites replaced raw bytecodes before frameless eligibility
was checked. The rewritten instructions then failed the raw-opcode whitelist,
restoring Java frames around native loops. Eligibility is now frozen before
rewriting. Native traversal owners have an explicit compiler lifetime fence in
frameless methods, including map-view owners whose native buffers contain the
iteration state. Methods with catch handlers retain their Java frames.

Static field accessors now test class-initialization completion before entering
the initializer. The completion flag is distinct from the early recursion flag;
it is loaded with acquire ordering. This removes repeated TLS/initializer calls
from initialized static-field reads and writes.

Round18 passed 20 self-translations but failed on its second Hello pressure run:
a retained JSRInlinerAdapter referred to children reclaimed in the previous
cycle. That failure remains in `frameless-hello-verify/01.log`; round18 is not a
valid performance result. Code inspection found that logically dead slots could
remain physically present on mutator-owned pages and be accepted again by the
conservative resolver. The resolver now rejects marks older than the last
successful sweep's reclamation cutoff. Skipped/incomplete collections do not
advance that cutoff. The failure is consistent with this lifetime hole; the
failing page's ownership state was not captured directly.

Round19 with this fix passed 20 self-translations and 20 full Hello pressure
translations, with every output file matching JDK25. Hello completed 12 verifier
passes per run. The full heap-integrity gate passed all ten workloads and all
five injected-fault controls (`livepages-gc-verify.log`). Self-host identity and
repeat/corruption gates matched 843 files (24,368,082 bytes). Five focused compiler
and string/collection integration tests passed, including owner lifetime fences.
Twelve actual-C sanitizer checks cover the last-sweep cutoff and the prior native
storage changes (`logical-death-sanitizers.log`).

Linked before/after ARM64 functions are in `livepages-assembly/`. Method lookup
loses its Java frame stores but retains the guarded generic-iterator fallback
for unknown field receiver types. The full-workload native sample and JDK25 JFR
are in `livepages-profile-20260917-065754/`; their emitted trees match. Sampling
counts and JFR allocation weights are diagnostic estimates, not benchmark
percentages or exact allocation totals. Release comparison is still pending.


The round19 steady-state gate also passed both tests (`livepages-memory-tests.log`,
12 minutes 51 seconds). Normal page usage stayed at 2,382 pages across its three
reported checkpoints; the fresh-reference fault control grew from 11,766 to
83,038 pages. Pressure, demand signaling, rescan, and pending-table fault controls
all passed. These are correctness/pressure checks, not release benchmarks.

The next batch extends receiver provenance across private collection fields.
Every bytecode write contributes its allocation origins; unknown writes, mixed
implementations, public/package fields and declaring classes with native methods
retain layout guards. Summaries retain type/call signatures rather than ASM
frames and are cleared after resolution and at parser cleanup. Unknown reference
origins now have an explicit sentinel: ASM's empty provenance set would otherwise
vanish when merged with a NEW origin and falsely prove a parameter exact.

Reference-block and reference-array tracing now hoist precise provenance out of
the buffer loop. Already-marked real references are rejected inline before the
general marker call/TLS lookup. Conservative descendants still resolve before
any header read, and forced rescans and verifier/fault modes retain full visits.
The actual-C sanitizer test includes a deliberately unreadable aligned pointer
on those validation paths. Thirteen sanitizer checks pass.

StringBuilder range append recognizes String and StringBuilder storage in C,
resizes once and bulk-copies matching coders. Mixed coders widen or narrow with
native loops; self-append reloads source storage after growth. Java still checks
bounds and preserves arbitrary CharSequence callbacks. The JavaScript target
retains its generic Java loop through an explicit unsupported-fast-path return.
Native/JDK conformance covers range slicing, compact/wide conversion, aliasing,
null sequences and observable custom charAt calls.


The field proof also excludes declaring types mentioned anywhere in the native
source symbol index, because native code in a different class can write their
fields. A regression case exercises that boundary. The final snapshot is
`regression-audit/native-provenance-safe-native` (round21); its verifier snapshot
is `native-code-audit/provenance-safe-verify-native`. Twenty self-translations
and twenty full Hello pressure translations pass, with every output matching
JDK25. The final release identity/repeat/corruption gate covers 847 files
(24,387,875 bytes).

The combined compiler/integration/JavaScript run passed 237 tests with one skip
(`provenance-compiler-tests.log`); the final native-boundary proof checks then
passed separately (`provenance-native-boundary-test.log`). The round20 full GC
gate passed all workloads and five fault controls; round21 also passed that full gate.

`provenance-safe-assembly/` contains linked ARM64 functions, not compiler IR.
Counting instruction addresses (excluding disassembly headers), method lookup
falls from 350 instructions in round17 to 164. It contains direct native-buffer
iteration and Latin-1 comparisons, without iterator dispatch. Builder range
conversion contains NEON `ushll`, `uzp1` and `xtn` instructions; matching coders
use `memmove`. Reference-array tracing compares the mark word inline before
calling `gcMarkObject`. No speedup is inferred from these instruction counts.

The final four-arm cohort compares old/new native executables and old/new
compiler versions on JDK25. Every arm receives the same frozen round17 JavaAPI
input, the last API supported by both resource snapshots. The final compiler's
own round21 JavaAPI is separately covered by the pressure and identity gates.
Each timed output is compared against its matching compiler version on JDK25,
since intended code-generation changes alter emitted C. Input, executable,
compiler resources, harness and driver digests are checked again after the run.


### Round21 completed whole-workload cohort

Seven rotating rounds, four arms, with all outputs verified:

| Corpus | Engine | Median seconds | Median peak footprint bytes |
|---|---|---:|---:|
| Hello | native round17 | 7.163239 | 2094090184 |
| Hello | native round21 | 6.079042 | 2080819120 |
| Hello | JDK25 round17 compiler | 4.801931 | 1520093176 |
| Hello | JDK25 round21 compiler | 4.878135 | 1470121928 |
| Selfhost | native round17 | 0.821954 | 433226448 |
| Selfhost | native round21 | 0.785189 | 411697896 |
| Selfhost | JDK25 round17 compiler | 0.851703 | 730285472 |
| Selfhost | JDK25 round21 compiler | 0.862128 | 722666816 |

Reports: `regression-audit/native-provenance-safe-final-hello-072135/results.json`
and `native-provenance-safe-final-selfhost-072445/results.json`. This is a 15%
within-cohort Hello improvement, with virtually unchanged memory. Hello remains
25% slower and 41% larger than its JDK25 arm. Earlier absolute times are not a
valid comparison across changed host load.

### Temporary analysis trees retained through ASM labels

The round21 allocation census (`provenance-safe-allocation-sample-20260917-072656`)
showed 8,876,284 occupied objects / 811.97 MiB at exit. LabelNode, VarInsnNode,
LineNumberNode, MethodInsnNode and InsnNode alone account for over 100 MiB of
objects marked reachable. This is diagnostic accounting, not release timing.
ParparVM retains ASM Labels for code generation; their `info` fields referenced
LabelNodes and thereby the entire doubly linked temporary MethodNode tree.

After all method analyses finish, Parser now clears only LabelNode-valued info
references from retained IR labels and drops the temporary method/map references.
The regression test fails without this cleanup and passes with it. Collection
receiver provenance now also feeds normal calls and folded calls, including
populated fresh-result factories. The new direct calls evaluate receiver and all
arguments before their explicit null check. Mixed or unproven receivers retain
dispatch. Whole-workload and retained-object results for these changes follow
only after validation; no performance claim is based on source appearance.


Round22 (`native-direct-lifetime-native`, SHA256
`884564f83184be351364126f7ba0295c554998188644b2f614e3f107b75547bc`)
passes all 49 compiler/native integration checks. The preceding JavaScript run
passes 186 cases with one skip. Release self-translation produces 849 files /
24,531,547 bytes identical to JDK25, passes repeatability, and detects deliberate
output corruption. Twenty selfhost pressure runs pass. The Hello pressure and
full GC gate are recorded separately in `direct-lifetime-*` logs.

Linked `BytecodeMethod.equals` now has zero List size/get dispatch calls. Its
normal path reads ArrayList size and native buffer elements directly; the
out-of-bounds path still calls ArrayList.get. Total function size is 302 ARM64
instructions versus 288 before, including explicit null-exception paths. This
is not evidence of a speedup by itself. Inspection also shows that the generic
throwException ABI permits returning on an unhandled exception, preventing C
from treating those new null-check failures as terminal.

The round22 census (`direct-lifetime-allocation-sample-20260917-074529`) verifies
its output against JDK25. At exit LabelNode, LineNumberNode, analysis Frame,
Value[], SourceValue and Subroutine rows are 100% unmarked/dead, confirming
that the analysis-tree reference leak is removed. There are still 1,673,970 dead
occupied objects (21% of 7,967,717 occupied objects). Allocator inspection shows
adopted objects are handed back to page storage by the legacy sweep; partial
pages are physically reclaimed on later sweeps. The process footprint and
allocation totals of this instrumented run are not release benchmark results.


### Final direct-call refinements (round23)

The retained receiver proof now stores a single exact type, instead of wrapping
and copying a HashSet whose only consumers queried singleton exactness. New
direct-call null checks use the existing `cn1ThrowNullPointerOrDie` noreturn
helper, preserving argument evaluation order and catch behavior while exposing
the failure edge to the C optimizer. The source/native conformance checks pass.
Linked `BytecodeMethod.equals` is now 209 instructions; List dispatch is absent
and one repeated bounds check has disappeared from the normal loop. There is
still a direct comparison call and an out-of-bounds fallback; it is not described
as a vectorized or zero-instruction loop.

Frozen release: `regression-audit/native-direct-scalar-native`, SHA256
`21d78859922888ee76b992fed6a324d55eeab74e3f328ee3100e8fbfd82a8ec0`.
Release identity/repeat/corruption validation: 847 files / 24,499,104 bytes.
Twenty selfhost and twenty Hello pressure runs pass with identical JDK25 output.
Round22 passes the full GC gate including all five controls; round23 repeats the
full gate on the final emitter. The instrumented round23 census again confirms
that temporary analysis trees are unmarked; dead occupied page slots remain.
No per-change timing was run for round22 or these two refinements. The next
cohort measures the combined round23 build against frozen round21 and JDK25,
using common round21 JavaAPI inputs supported by both compiler snapshots.


### Round23 completed cohort and correction

This cohort is a regression in Hello elapsed time and is retained as evidence.
All 56 timed translations were checked against their corresponding JDK25 output.

| Corpus | Engine | Median seconds | Median peak footprint bytes |
|---|---|---:|---:|
| hello | native-before | 6.464091 | 1979107224 |
| hello | native-after | 7.147372 | 1841170232 |
| hello | jdk25-before | 5.058625 | 1468368816 |
| hello | jdk25-after | 5.233855 | 1437910984 |

Report: `regression-audit/native-direct-scalar-final-hello-075342/results.json`.

| selfhost | native-before | 0.80814 | 410288872 |
| selfhost | native-after | 1.056203 | 376439480 |
| selfhost | jdk25-before | 0.885594 | 719684976 |
| selfhost | jdk25-after | 0.969048 | 710526248 |

Report: `regression-audit/native-direct-scalar-final-selfhost-075700/results.json`.

The broader fresh-factory prefilter admitted StringBuilder-based string-returning
methods even though a returned method result cannot carry NEW provenance. The
round23 census allocates 1,114,892 analysis frames/value arrays versus 347,767 in
round21, and 2,576,247 SourceValues versus 943,982. The follow-up prefilter requires
an ALOAD, CHECKCAST or constructor before every object return, and still requires
a NEW somewhere plus the full frame proof. Primitive category values are shared;
unknown references retain an explicit provenance marker, including array loads,
reference constants and dynamic results. Regression cases cover unknown merges.

The round24 page change requests reclamation when the legacy sweep relinquishes
an adopted object. The legacy registry sweep now precedes the page sweep, so
only flagged partial pages are spliced into the same cycle's page sweep under the allocator's pool lock. Owned pages are untouched until normal
retirement. Partial-page rescans remain excluded from trigger-policy sampling;
thresholds, grace, aging, adoption and marking rules are unchanged. This closes
the gap between logical death and physical slot/native-buffer reclamation without
waiting for the periodic major sweep. Validation and measurements are pending.


Round24 frozen release SHA256:
`e60f6052588a0426cd015fd44fdb3179ad54a14620c9fbae94813d43d886021e`.
All 49 compiler/native integration checks pass. The release identity/repeat gate
covers 849 files / 24,562,919 bytes and detects deliberate corruption. Twenty
pressure translations of each corpus pass. The full GC gate passes all ten
workloads and five controls. Thirteen actual-C ASan/UBSan checks pass.

The round24 diagnostic census (`reclaim-flow-allocation-sample-20260917-081843`)
checks output identity and logs actual flagged-page resweeps. Dead occupied
objects at exit fall from round23's 1,743,491 to 31,864. Analysis SourceValue
allocations fall from 2,576,247 to 706,331; Frame/Value[] allocation counts fall
from 1,114,892 to 865,496. These counts establish mechanisms, not release speed.
The steady-state and final whole-workload cohort results follow when complete.


`reclaim-flow-jdk25-code/` captures actual HotSpot C2 code from a full Hello
translation with CompileCommand=print for method equality and method lookup.
The installed JDK has no hsdis, so `decode-jdk25-code.py` validates complete
coverage of every main-code byte, writes those bytes into an ARM64 object, and
uses LLVM to decode them. `decoded.json` preserves the original addresses and
compilation identities; extracted branch targets are relative to the new section.
The JIT log retains inline scopes, runtime-call annotations and exception tables.
Generated files match the census reference byte for byte except CMake's exact
absolute output-source-root path, which is checked by a single path replacement.

The captured JDK25 defaults use G1, compressed object/class references and compact
strings, with 8-byte object alignment. UseCompactObjectHeaders is false. The
record is `jdk25-default-flags.log`; no compact-header advantage is assumed.


### Round24 steady-state gate and round25 short-string lowering

Both GcSteadyStateIntegrationTest cases pass (`reclaim-flow-steady-state.log`).
The fixed run holds 6,054 pages across the three checkpoints over 2,733 cycles;
the injected fault grows from 10,923 to 45,882 to 83,422 pages. Reserve/ceiling,
pacing, rescan controls, sorting correctness and pending-table checks pass.
These are collector correctness/stability checks, not application timing claims.

Actual HotSpot C2 disassembly exposed inline word comparisons where our compact
String equality still called memcmp. Round25 adds bounded 1/2/4/8/16-byte loads
for equal-length byte ranges of at most 32 bytes; longer ranges retain memcmp.
The same helper covers same-coder UTF16 equality by byte length. Mixed-coder
behavior is unchanged. Fixed-size memcpy loads avoid alignment/aliasing UB and
never read outside the logical string range.

All 13 actual-C ASan/UBSan tests pass, including lengths 0–96, all eight byte
alignments, every mismatch position and exact allocation boundaries. Native/JVM
collection and StringFormat conformance passes. Twenty selfhost and twenty Hello
pressure translations pass. The final full GC gate passes all workloads and all
five injected-fault controls. Release repeat/output/corruption gates pass across
849 files / 24,564,419 bytes.

Frozen round25 release SHA256:
`30add54b96487851845745693d81779fc32342cd790807a8622eb56ecfa1534e`.
Frozen host-resource digest:
`b89b1c1564eb6ee750fa745bcc4a2cf8f97249b082e21b1eae0b3c245d54270f`.
Linked `short-words-assembly/method-equals.asm` shows q-register loads and SIMD
comparison for lengths 16–32, integer loads/comparisons for smaller strings, and
memcmp only above 32 bytes. Its larger 268-instruction body includes the inlined
comparison and cold paths; static instruction count is not elapsed cost.


### Round25 completed whole-workload cohort

Seven rotating rounds of four engines use identical frozen corpus inputs. All
56 timed translations match their corresponding compiler version on JDK25.
Peak memory is macOS phys_footprint, not RSS or Java heap. Background host
activity is recorded per sample; these are not idle-machine measurements.

| Corpus | Engine | Median seconds | Median peak footprint bytes |
|---|---|---:|---:|
| hello | native-before | 6.105287 | 1966164056 |
| hello | native-after | 6.091346 | 1718781632 |
| hello | jdk25-before | 4.808571 | 1495943136 |
| hello | jdk25-after | 4.837674 | 1179240224 |

Report: `regression-audit/native-short-words-final-hello-084042/results.json`.

| Corpus | Engine | Median seconds | Median peak footprint bytes |
|---|---|---:|---:|
| selfhost | native-before | 0.785194 | 418415336 |
| selfhost | native-after | 0.636498 | 357384816 |
| selfhost | jdk25-before | 0.858853 | 716391792 |
| selfhost | jdk25-after | 0.808964 | 594461992 |

Report: `regression-audit/native-short-words-final-selfhost-084344/results.json`.

Hello elapsed time remains effectively unchanged versus round21 in this cohort;
median peak footprint falls by about 13%. Against the matching JDK25 compiler,
Hello is still about 26% slower and has about 46% greater median peak footprint.
The requested performance target is not met.


### Round26 field tracing work

The round25 whole-Hello sample (`short-words-profile-20260917-084448`) matches
JDK25 output. The collapsed native top-of-stack samples include gcMarkObject
1,766, cn1ConservativeResolve 822, method lookup 668, method equality 358, and
HashMap tracing 338. These are sampled stacks across multiple threads, not
elapsed-time percentages. The corresponding JDK25 flight recording is retained.

Parallel marking already returns on an epoch match regardless of the force flag,
but the reference-range inline skip required force=false. The new context helper
permits the inline skip for a precise parallel traversal, while retaining the
serial force-visited rescan, conservative validation and verifier/fault paths.
Generated field tracing uses this same context once per callback and skips null,
tagged and already-marked precise children before calling the general marker.

Callbacks now flatten inherited field tracing into one function. The initial
attempt used the accessor field list, which omits inherited private fields; the
new native-buffer inheritance assertion caught that defect before freezing a
binary (`field-mark-tests.log`). The corrected callback traverses every declaring
class's actual fields, including private ones, and inherited native reference
buffers. Weak referents still go through cn1GcDiscoverReference. No collector
threshold, aging, grace, adoption or barrier rules change.


Round26 frozen release SHA256:
`d1f201b3ae54fd47fb533874796c5f50e3dc4e6134c3b05114630dcb1daac20a`.
Host-resource digest:
`d236c5e9af0c7a8e3c7899e68a74a7cb78b2c7d5330b2ff1665a6fa825b6834e`.
All 51 compiler/integration tests pass. The strengthened two-case tracing test
also passes after adding a negative control with verifier-only field access;
merely mentioning a field no longer satisfies the tracing check. All 13 actual-C
ASan/UBSan tests pass. Twenty selfhost and twenty Hello pressure translations
pass with verified JDK25 output. Release identity/repeat/corruption checks cover
849 files / 24,738,165 bytes.

The linked HashMap, CustomInvoke and BytecodeMethod tracing callbacks each have
one TLS resolver call. The two object callbacks have no base-marker calls;
inherited private fields are covered directly. Inline acquire epoch comparisons
precede general marker calls. Conservative and serial forced paths still call
the general marker; this is not described as removing GC or all marker calls.
Assembly is under `field-mark-assembly/`.

The round26 full GC verification gate is GREEN, including all ten workloads
and all five injected-fault controls (`field-mark-gc-verify.log`).


### Round26 completed cohort

| Corpus | Engine | Median seconds | Median peak footprint bytes |
|---|---|---:|---:|
| hello | native-before | 6.298816 | 1812956936 |
| hello | native-after | 6.324878 | 1762330328 |
| hello | jdk25-before | 4.929745 | 1173341984 |
| hello | jdk25-after | 4.898831 | 1169786632 |

Report: `regression-audit/native-field-mark-final-hello-085726/results.json`.

| Corpus | Engine | Median seconds | Median peak footprint bytes |
|---|---|---:|---:|
| selfhost | native-before | 0.654909 | 375767712 |
| selfhost | native-after | 0.64543 | 367559352 |
| selfhost | jdk25-before | 0.809192 | 597968144 |
| selfhost | jdk25-after | 0.820188 | 601228560 |

Report: `regression-audit/native-field-mark-final-selfhost-090034/results.json`.

All 56 timed translations match their corresponding JDK25 compiler output.
This does not demonstrate a material elapsed-time improvement from field
tracing. The structural removal of calls is not presented as a speed claim.


### Round27 complete native String equality

The previous intrinsic still called the general Java String.equals entry for
UTF16 and mixed-coder values, leaving an opaque call in otherwise read-only
lookup loops. Both same-coder layouts now use the native bounded byte comparator;
mixed Latin1/UTF16 uses a read-only unsigned-byte/UTF16 comparison with no Java
entry, allocation or safepoint. The native method uses the same mixed helper.

All 13 actual-C ASan/UBSan cases pass. String cases cover both intrinsic and
native entry points, all four coder combinations, offsets, lengths 0–129,
mismatches at every position and UTF16 units above 255. The byte helper retains
its allocation-boundary/alignment tests. Java/native CollectionSemantics and
StringFormat integration tests pass.

Frozen release SHA256:
`b49d3159fbfaab8bf5d060a4aedec694f426a5d68e0bd98be20375e6587bfbab`.
Host-resource digest:
`ca16e9aa31698f39acc23492342016b5823f72fee4a5b4449894c36e4d9e3422`.
Release identity/repeat/corruption gates cover 849 files / 24,739,196 bytes.
`pure-equals-assembly/find-method.asm` has no Java String.equals call. Its normal
comparison calls are memcmp; exception construction remains on failure paths.
The body grows from 277 to 383 instructions including cold paths and all coder
cases. This is evidence of lowering, not a claim that larger inline code is faster.


Round27 pressure validation passes twenty translations of each corpus. The full
GC gate passes all ten workloads and five fault controls. The final timed cohort
compares the complete round26/27 batch against frozen round25, with a JDK25 arm
for each compiler snapshot and byte-identical output checks for every sample.

Scope remains limited: NativeTraversal retains iterator fallback for receivers
whose layout cannot be proved or selected safely, and StreamFusion handles
immediate supported array pipelines rather than arbitrary escaping pipelines.
Builder/iterator escape proofs are not general object scalar replacement. Native
references remain full width. Moving collection storage outside the managed heap
does not remove tracing of its contained Java references. These are remaining
implementation boundaries, not reasons to claim the original target is complete.


### Round27 final whole-workload results

| Corpus | Engine | Median seconds | Median peak footprint bytes |
|---|---|---:|---:|
| hello | native-before | 6.446207 | 1762707160 |
| hello | native-after | 6.473466 | 1798244080 |
| hello | jdk25-before | 4.974875 | 1162888920 |
| hello | jdk25-after | 5.01926 | 1185416944 |

Report: `regression-audit/native-pure-equals-final-hello-090756/results.json`.

| Corpus | Engine | Median seconds | Median peak footprint bytes |
|---|---|---:|---:|
| selfhost | native-before | 0.674223 | 362431112 |
| selfhost | native-after | 0.649355 | 373359264 |
| selfhost | jdk25-before | 0.834182 | 593331424 |
| selfhost | jdk25-after | 0.829956 | 600524000 |

Report: `regression-audit/native-pure-equals-final-selfhost-091105/results.json`.

All 56 timed outputs match their corresponding JDK25 reference. The final
Hello result is about 29% slower than JDK25 with about 52% greater median peak
footprint. Relative to the native control in this cohort, elapsed time is
effectively unchanged (+0.4% median); median peak footprint is +2.0%. These
measurements do not establish a performance win for the round26/27 batch.
The requested whole-workload target remains unmet.


The final whole-Hello profile (`pure-equals-profile-20260917-091239`) again
verifies output against JDK25. Collapsed top-of-stack samples include
`gcMarkObject` 1,317, `cn1ConservativeResolve` 973, method lookup 682, HashMap
tracing 564, method equality 440 and sweeping 383. These are counts across
threads, not wall-time percentages; wait samples are not CPU work. Marking,
conservative validation and lookup remain substantial sampled CPU work. JDK25's
matching flight recording is retained beside it.

The final source digest and original executable digest were rechecked against
the build manifest. The frozen copy is byte-identical to that executable and
matches its recorded SHA256. The harness digest includes the filename, so copies
under different names must use byte/SHA256 comparison, not that path-sensitive
digest. The measured binary matches current runtime/compiler sources; no
production source changes were made after this freeze.
