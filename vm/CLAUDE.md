# vm/ (ParparVM)

## Tagged immediates: six boxed types, two of them partial

`valueOf` on `Integer`, `Long`, `Double`, `Float`, `Character` and `Short` returns an
**immediate** rather than a heap object: the value packed into the pointer word with a type
code in the **low three bits**. `cn1_globals.h` holds the whole encoding; everything else
consumes it.

Three bits, not one, because 8-byte object alignment was **already load-bearing** --
`cn1ConservativeResolve` rejects any word with a bit set in `sizeof(void*) - 1`, and
conservative roots are on by default, so an object at a 4-byte address would already be
invisible to the root scan and freed under a live pointer. Widening the tag rests on an
invariant the collector enforces rather than adding one. `TagProbe` checks it anyway, over
380,000 allocations across 19 shapes (BiBOP bump, the legacy calloc above
`CN1_BIBOP_MAX_OBJECT`, arrays, fused String/StringBuilder, class objects): all 8-aligned.

| code | type | representable |
|---|---|---|
| 0 | an ordinary heap pointer | -- |
| 1 | `Integer` | every value |
| 2 | `Long` | **partial**: `[-2^60, 2^60)` |
| 3 | `Double` | **partial**: bit patterns with three clear low mantissa bits |
| 4 | `Float` | every value (raw 32-bit pattern) |
| 5 | `Character` | every value |
| 6 | `Short` | every value |
| 7 | reserved | -- |

`Byte` and `Boolean` are deliberately NOT tagged: a 256-entry and a 2-entry cache already
never allocate.

`Double` needs no shifting at all. Requiring three clear low mantissa bits means the
immediate IS the bit pattern -- tag is `| 3`, untag is `& ~7` -- and the representable set is
every small integer, half, quarter and eighth, which is what JSON numbers overwhelmingly are.
`0.1` and `3.14` fall out of it and take the heap path.

### A partial encoding must report its coverage, not just its speedup

This is the trap the whole design turns on. Measured on a *mixed* distribution, `Long` is
**78%** taggable and `Double` **56%**. An earlier `jsonLikeParse` used only integers and
exact quarters and reported **100%** for both -- a benchmark measuring the best case and
calling it the case. `BoxBench` now prints a `COVERAGE` line beside its timings and its
numeric mix is deliberately unhelpful (a quarter money values, a quarter irrational).

The `CN1_ALLOC_CENSUS` figure is the one to trust, because it confirms coverage from an
independent instrument. Normalised by a work unit tagging cannot affect (HashMaps allocated),
`jsonLikeParse` goes from **24.02 boxed objects per map to 5.24**. It puts 12 Longs and 12
Doubles per map, so theory says 24 and `12 x 0.44 = 5.28`. `Long` allocations over the run:
346,545 -> **1**.

### What it is worth, and what it costs

`ab-tagged.sh` runs three arms so the two questions stay separate: `HEAP`
(`-DCN1_DISABLE_TAGGED_INT`), `INT` (`-DCN1_DISABLE_TAGGED_VALUES`, i.e. what shipped when
only Integer was tagged) and `ALL`. **Arm 1 is the baseline that matters** -- `HEAP` vs `INT`
re-measures a win that was already taken.

Best-of-6 interleaved, `ALL` against `INT`: `longKeyMap` **2.3x**, `charBoxing` 1.54x,
`doubleListReduce` 1.43x, `mixedBoxedChurn` 1.28x, `jsonLikeParse` 1.14x at 56% coverage.
On `Bench`/`CommonWorkloads` the ratio is **1.00 on every workload** -- the five extra types
cost nothing where they are not used, and `hashMapChurn`'s existing 3.3x from Integer tagging
is untouched.

Peak RSS from `ru_maxrss` did **not** discriminate here (identical to a tenth of a MB across
all three arms); it is cumulative across children in that harness. Use the census.

### The type index is an addressing mode, not a lookup

The worry that self-describing tags cost a table probe is unfounded. `cn1ClassOf` compiles to:

```
and  x8, x1, #7            ; tag code, computed once
cmp  x8, #1                ; the Integer fast path reuses it
add  x9, x9, x8, lsl #4    ; &cn1TaggedProxy[code] -- a shifted add, NOT a load
cmp  x8, #0
csel x8, x1, x9, eq        ; select a VALID pointer first
ldr  x8, [x8]              ; the single header load
```

The `csel`-before-`ldr` shape is not stylistic: a plain ternary lets clang speculate the
faulting `tagged->header` load above the tag test, which was an observed SIGSEGV in interface
dispatch. Do not simplify it into a conditional over the loaded value.

**And the tag is not optional.** The recurring suggestion is that the callsite already knows
the type. It does not: a value is boxed *precisely because* it is flowing through an
`Object`/`Number`/`Comparable` slot, and `map.get(k).hashCode()` has no static type to
consult. Where the callsite does know -- an autobox immediately unboxed -- the fix is
peephole elision, which `scalarReplaceStackAllocations` already does and which never needed a
tag.

### The four choke points, and the one that fails silently

`CN1_IS_TAGGED` masks all three bits, so **every existing call site is correct unedited** --
`gcMarkObject`, `findPointerPosInHeap`, `removeObjectFromHeapCollection`, the SATB barriers.
`cn1ClassOf` covers instanceof, checkcast, aastore, virtual and interface dispatch,
`getClass`, `isInstance` and `String.equals` in one place. Per type there is a `valueOf`
native and a `cn1Value` native.

`cn1Value` **must** be a native. These classes are `final`, so `Invoke.asInlinableFieldAccess`
folds a trivial `return value;` getter into a raw `GETFIELD` -- off a pointer with no fields.
Every read of the boxed value routes through it; there is a scan in the commit that proves
none was missed.

The sharp edge is `BytecodeMethod.java`'s inline `hashCode`/`equals` fast path, which is
emitted into **every** class's thunk. It tests `CN1_TAG_CODE(o) == CN1_TAG_INTEGER`, not "is
tagged", because every other type has a different hashCode contract -- `Long` folds its
halves, `Float`/`Double` go through `*ToIntBits`. Widening it to any tagged receiver returns
a **wrong hash with no crash**, which is the worst thing this scheme can do. The other five
reach the right implementation through the vtable, which is slower and always correct.
`equals` stays Integer-only for a second reason: pointer equality implies value equality for
every tag, but the converse fails for `Float` and `Double`, where two NaN encodings must
compare equal.

`BoxEdge` exists for exactly this and is **proven non-vacuous**: re-widening that guard makes
it diverge on 115 lines, starting with a Double whose hash silently becomes 0.

### identityHashCode has to fold a tagged word, and only a tagged word

`System.identityHashCode` is a truncation to the low 32 bits. That is right for a heap
pointer -- `IdentityHashMap`'s indexing is tuned around exactly that distribution, see the
`IdmProbe` note above -- and wrong for a tagged **Double**, which carries the raw IEEE
pattern whose distinguishing bits for 1.0, 2.0, 3.0 all live in the HIGH word. Every
integral double therefore truncated to the same int: measured **1 distinct identity hash
across 4096 values**, which turns that map's linear probe quadratic. `Long` and `Float`
shift their payload up and survive a truncation; only `Double` is exposed, because it is the
only encoding that does not shift.

The fold is applied to tagged values only, and `TagProbe` asserts the spread (4096/4096 with
it, 1/4096 without). Folding heap pointers too would re-randomise the near-perfect placement
`IdmProbe` exists to protect.

**This is why the CI witness cannot read tag bits.** `identityHashCode(x) & 7` was how
`BoxEdge` and `TagProbe` reported which arm they were in; the fold destroys that. Both now
detect an immediate by its observable consequence -- `valueOf(v) == valueOf(v)` at a value
outside every `-128..127` cache -- which is a better test anyway, and whose one failure mode
(a compiler CSEing the two calls) the untagged arm's expected `000000` catches.

### Monitors on tagged values are never reclaimed, and that is accepted

A monitor lives in an address-keyed side table and is removed when its object dies. A tagged
value never dies, so `synchronized (Float.valueOf(i))` over a loop leaks one entry per
distinct value. Tagging five more types genuinely widens this -- those used to be heap boxes
whose monitors *were* reclaimed.

It is still not fixed, and the reasoning is recorded at `monitorEnter` in `nativeMethods.m`:
the shape was already unbounded for Integer, nothing in this VM reclaims a monitor at
`monitorExit` so removal would be a new mechanism for every monitor rather than a tagged-only
patch, and it would be built in the subsystem that already carries the documented three-way
`monitorEnter` deadlock.

**`BytecodeComplianceMojo` is a partial mitigation, not a proof of unreachability**, and the
first version of that comment said otherwise. It rejects `MONITORENTER` whose operand is
*statically typed* as a wrapper, which is intra-procedural and types-only -- hand the box to
a helper taking `Object` and synchronize on the parameter and the build passes; a field, an
array or a collection defeats it the same way. It is a gate against the obvious mistake. The
leak is therefore reachable from application code and accepted, bounded by the number of
distinct boxed values a program ever locks on, which is zero for anything following the rule.

The rule always named all eight wrappers, but its test only ever exercised Integer. It is a
loop over all eight now -- not `@ParameterizedTest`, because that module carries
`junit-jupiter-api` and `-engine` only and adding `junit-jupiter-params` to a Maven plugin's
pom for one test is not worth it. Its generator also hardcoded `ICONST_1`, so a `(J)`/`(D)`/
`(F)` `valueOf` would have produced a type-incorrect method and asserted nothing.

Framework and JavaAPI code is not scanned by that mojo at all; there is no `synchronized` on
a boxed value anywhere in `CodenameOne/src`, `vm/JavaAPI/src` or `Ports` today.

### The nursery guard belongs in cn1InNursery, and -DCN1_NURSERY did not compile

Every caller of `cn1InNursery` dereferences the header the instant it answers true --
`cn1InNursery(o) && o->__heapPosition == -1` is the shape at all six sites, in the minor
collection's stack-root scan, its `currentThreadObject` and `exception` roots, the promote
path and the write barrier. A tagged `Double` carries a raw IEEE pattern and a tagged `Long`
a shifted payload, either of which can land inside the arena range by coincidence, and the
read that follows is an unaligned load off a word with no header. The guard therefore lives
in `cn1InNursery` itself; guarding only the write barrier -- which is what the first version
of this work did -- left the other five exposed.

**Reachable by construction, not observed.** Instrumenting the range check to count tagged
values that fall inside the arena gives **0** on `BoxEdge`, `GcStress` and `MtStress`: a
tagged Long is `v << 3` and a tagged Double's bit pattern is astronomically larger than a
heap address, so the overlap needs an unusual value. Real, narrow, and cheap to exclude.

**`-DCN1_NURSERY` had not compiled for as long as the bulk SATB barrier has existed.** The
`cn1SatbBulkBegin` / `cn1SatbEnqueueRangeLocked` / `cn1SatbBulkEnd` declarations sat inside
the no-nursery `#else` while `nativeMethods.m` calls all three unconditionally from
`java_lang_System_arraycopy`, so the build died on three implicit declarations. The functions
were always defined; only the declarations were misplaced. That silently retired an ablation
arm this file documents, and it is the reason the missing tag guard could not be caught by
building the configuration it affects. Hoisted above the split, and the configuration now
builds and runs `BoxEdge` byte-identically plus `GcStress`/`MtStress` clean.

### The gate has to be in CI, and it has to know which arm it ran

`BoxEdge` first lived only in `run-gauntlet.sh` -- and **no workflow runs the gauntlet**, so
the feature shipped on by default with nothing in CI exercising it.
`TaggedValueIntegrationTest` closes that: it drives the *same* `BoxEdge` source (read from
`vm/benchmarks`, not copied, so the two cannot drift) through the translator and cmake, builds
it twice from one translation, and compares both against a real JVM. About 67s.

The subtle part is that byte-identity **cannot** tell a correct tagged build from one where
tagging never happened -- the two representations are required to be indistinguishable, which
is the whole contract. So `BoxEdge` prints `[TAGCODES]`, the tag code each type actually got,
and the test asserts `123456` for the default build and `000000` for
`-DCN1_DISABLE_TAGGED_INT`. Compiling the extra types out and re-running proves this is load
bearing: the two byte-identity assertions still **pass**, and only the witness fails, with
`expected: <123456> but was: <100000>`.

Two mechanics worth knowing before touching it:

- **Stderr is not a way to keep a diagnostic out of the compared stream.** On the clean target
  `System.err` also reaches fd 1. The `[`-prefix convention is the answer, and
  `run-gauntlet.sh` now applies that filter to **both** sides -- it used to filter only the
  target, so any torture emitting a diagnostic diverged against its own host run.
- **The reference JVM must be JDK 19+.** JDK 19 replaced `Double.toString`/`Float.toString`
  with the shortest round-tripping representation (JDK-4511638) and ParparVM implements the
  new algorithm, so an older reference reports divergences that are the reference being out of
  date: `4.6116860184273879E18` from JDK 17 against `4.611686018427388E18` from ParparVM and
  JDK 19+. Both round-trip; only the second is the shortest such string. The test selects its
  reference JDK independently of the toolchain that drives the translator, and skips if none
  is new enough.

### Three defects this work surfaced, none of them caused by it

All three were pre-existing and are fixed here, and all three were found by `BoxEdge` on its
first run against the host JVM -- which is the argument for a byte-identical torture over an
assertion suite: nobody thought to assert any of them.

- **`Short.equals` had no null guard** where every other wrapper did, so
  `Short.valueOf(1).equals(null)` dereferenced null -- a hard SIGSEGV on any target that
  installs no signal handler, which is every one except iOS.
- **`Float.toString` never got the fixes `Double.toString` has.** No NaN branch (NaN fell
  through to the scientific-notation path, since every comparison against a NaN is false) and
  no signed-zero branch, so `Float.toString(-0.0f)` printed `0.0` while `Float.compare`,
  `Float.equals` and `Arrays.sort` all still honoured the sign. Confirmed pre-existing by
  reproducing it with `-DCN1_DISABLE_TAGGED_INT`.
- **`Short.compareTo` returned the sign, not the difference.** The JDK defines it as
  `Short.compare`, which -- unlike `Integer.compare` and `Long.compare`, which really do
  return -1/0/1 -- is `x - y`. It disagreed with the JDK and with this class's own
  `compare(short, short)` directly above it.

### An arbitrary word is now a boxed value seven times in eight

A machine word whose low three bits spell a tag code is **indistinguishable** from a genuine
immediate; they are the same bit pattern. The iOS debugger therefore reports a boxed value
for a misread int slot rather than rejecting it. That is a fidelity cost, not a safety one --
it never dereferences, which is the property issue #5333 was about -- and it is not new,
since an odd word was already a tagged Integer when the tag was one bit. Widening to three
bits takes the affected fraction from one in two to seven in eight.

**The GC is completely unaffected by that**, and this is the fact to reach for when the
question comes up: `cn1ConservativeResolve` rejects a word with a bit set in
`sizeof(void*) - 1` -- the same three bits -- so a misread int was never a root before and is
not one now.

Two consequences that did need edits: `DebuggerObjectValidationTest`'s `MISALIGNED` probe
used `ptr | 2`, which is now a valid tagged `Long`, so it uses the reserved code 7; and
`NativeDebuggerHarness` stubbed `cn1TaggedProxy` as all zeros, which -- now that
`cn1_debugger_class_of` resolves through `CN1_CLASS_OF` -- would have made every tagged
assertion an assertion about nothing.

## Narrowing a float to an int is SATURATING, and C's cast is not

JLS 5.1.3: converting a `float` or `double` to an `int` or `long` clamps -- NaN becomes 0,
anything at or above the target's maximum becomes `MAX_VALUE`, anything at or below its
minimum becomes `MIN_VALUE`. A C cast is **undefined** out of range, and the two
architectures this VM ships on disagree about what it actually does:

| | `(int) Double.MAX_VALUE` |
|---|---|
| arm64 (`fcvtzs`) | 2147483647 -- accidentally correct |
| x86-64 (`cvttsd2si`) | **-2147483648** -- the "integer indefinite" value |

So this was right on Apple silicon and wrong everywhere else, for every app, silently. It was
found by running `BoxEdge` under CI's x86 Linux after it had passed on an M-series Mac.

**There are THREE emission sites, and patching two of them looks like it works.** The two
obvious ones are `BC_{F2I,F2L,D2I,D2L}` in `cn1_globals.h` and the statement forms in
`BasicInstruction.java`. The third is `ArithmeticExpression.java`, which renders a conversion
as a raw cast *inside a composed expression* -- and that is the one the optimizer actually
uses for the common shapes, so a fix to the first two changes nothing you can see. All three
now call `cn1SaturateToInt` / `cn1SaturateToLong`. Note `L2I` sits beside them and is NOT one
of these: long-to-int is defined truncation.

`-DCN1_NO_SATURATING_NARROWING` restores the old cast; because all three sites route through
the two helpers, that single macro ablates the whole change and an A/B needs no second
translator build. `vm/benchmarks/ab-narrowing.sh` interleaves the arms: **geomean 1.0073**,
every workload within 3.4%, i.e. free.

**That number had to be re-measured to be believed, and the first attempt was worthless.**
Comparing the new build against ms figures recorded earlier in the same session reported a
uniform 9-14% regression -- including on `intArithmetic` and `stringBuilding`, which perform
no narrowing at all. A slowdown that appears on workloads the change cannot touch is the
host, not the code; this machine cannot resolve 5% across runs minutes apart. Interleave the
arms inside one process-pair or do not quote a number.

### Dimension and Rectangle: investigated, and tagging is the wrong mechanism

The obvious next thought is to extend this to framework value types. It does not work, and
the reasons are worth writing down so it is not re-proposed.

- **`Rectangle` is not a flat value at all.** Its fields are `x`, `y`, a `Dimension size`
  **object reference** and a `GeneralPath path` **object reference** -- an object graph, not
  four ints. It also has a static instance pool, so it has deliberately recycled identity.
- **`Dimension` does not fit.** Two ints is 64 bits against a 61-bit payload, so it would
  need narrowing to 30+30 plus a range fallback.
- **Both are mutable** (`setWidth`, `setHeight`, `setBounds`, `setX`, `setY`). An immediate
  has no storage, so a mutation through one alias is invisible to every other. That is not
  fixable within the scheme.
- **`new` guarantees identity.** `Integer.valueOf` has an explicit spec exemption to return a
  shared instance; `new Dimension(1,2) != new Dimension(1,2)` is guaranteed by the JLS.

The mechanism that would pay off is **codegen, not tagging**, and most of it is already here.
`scalarReplaceStackAllocations` turns `NEW X; DUP; args; <init>; ASTORE` into a pure C local
with no allocation, no tag, no lookup and no fallback -- strictly better than tagging wherever
it applies. `Dimension` already passes `srPrimitiveOnlyDirectObject` (it extends `Object`
directly, has no `__CLINIT__` and holds only ints); `Rectangle` fails it on both its object
fields and its static pool.

What stops `Dimension` is `srValidateLocalUses`, which requires every use of the local to be
`ALOAD n; GETFIELD` and bails on a return, an argument pass or a field store --
and `Component.getPreferredSize()` returns one. There are 127 `new Dimension(` sites and 108
`new Rectangle(` sites in `CodenameOne/src`.

So the follow-up worth scoping is an `@ImmutableValue` contract -- `BytecodeComplianceMojo`
enforcing final fields, no setters and no identity-sensitive use, so the translator may treat
the class as copyable and let scalar replacement survive a return. That is a separate change,
and the thing to measure FIRST is a `CN1_ALLOC_CENSUS` profile of a real app: if `Dimension`
and `Rectangle` are not near the top of it, the answer is no and it cost one run to find out.

### Forcing a boxed class's clinit needs release/acquire, not `volatile`

A tagged value never allocates, so nothing else runs its class's `<clinit>` -- and that is
what fills the vtable a later `hashCode`/`equals`/`compareTo` on the immediate dispatches
through. Each `valueOf` forces it once, behind a flag.

That flag is a fast path only: `__STATIC_INITIALIZER_X` is already double-checked behind the
class monitor. What it does affect is **publication**. `volatile` in C is neither atomic nor
ordered, so a plain flag lets a second thread observe 1 while the initializer's vtable writes
are still invisible to it on a weak-memory target, and the next virtual call dispatches
through stale class state. `CN1_FORCE_BOX_CLINIT` publishes with `__ATOMIC_RELEASE` and reads
with `__ATOMIC_ACQUIRE`, the same pairing `CN1_CONSTANT_POOL_LOAD` documents. Verified in the
emitted arm64: `ldapr` on the read, `stlr` on the publish.

Release/acquire only carries what the PUBLISHING thread saw, and
`__STATIC_INITIALIZER_X` is **not** a reliable synchronisation point: its generated fast path
is `if(__X_LOADED__) return;`, a plain load, and the matching `__X_LOADED__=1` is a plain
store placed AFTER `monitorExitBlock`. A thread returning through that path has taken no lock
and may hold none of the initialising thread's writes. So the slow path takes the class
monitor before publishing, which makes the publisher synchronise-with whoever ran the body;
monitors are reentrant, so the initializer's own enter nests harmlessly. One uncontended lock
per class per process, and the hot path stays a single `ldapr`.

**That fixes the six boxed classes, not the VM.** `__X_LOADED__` is a plain-load/plain-store
double-check on *every* generated class initializer, which predates tagging. Fixing it means
making that flag acquire/release in `ByteCodeClass`, which touches codegen for every class in
every app -- its own change, with its own measurement.

The original defect was in the Integer native and this work copied it to five more types; all
six are converted together, because half a memory-model fix is worse than none.

### Adding a seventh type

Only one code is left, so spend it deliberately. The work is: a proxy entry, `valueOf` +
`cn1Value` natives (each forcing its class's `__STATIC_INITIALIZER_` once, because a tagged
value never allocates and nothing else triggers the clinit that fills the vtable), routing
every value read in the Java class through `cn1Value`, the debugger's
`cn1_debugger_tagged_value` switch, and `BoxEdge` cases. `ByteCodeClass` already force-retains
all six wrapper classes from dead-code elimination.

**And FOUR JavaScript files, not one.** The JS port has no immediates, so `valueOf` binds
straight through to `valueOfHeap` -- which is static and reached only from
`parparvm_runtime.js`, so bytecode-only reachability never sees the edge and the cull deletes
it. All four are needed: `parparvm_runtime.js` (the `bindNative`),
`JavascriptNativeRegistry.RUNTIME_IMPLEMENTED` (the native), and both
`JavascriptReachability.enqueueResolved` and
`JavascriptNativeRegistry.RUNTIME_DELEGATE_TARGETS` (the heap twin). Missing the last two
does not produce a recognisable error -- the fixture returns a wrong value, and
`JavascriptRuntimeSemanticsTest`'s coverage assertion is one of the few in that class that
does not print `rawMessage`/`errorMessage`. `JavascriptNativeAuditTest` is inert and catches
none of it.


## The compact HashMap: benchmark the MISS, not the hit

`java.util.HashMap` here is open-addressed over three parallel arrays with linear probing
and native C hot paths (`vm/JavaAPI/src/java/util/HashMap.java`,
`vm/ByteCodeTranslator/src/nativeMethods.m`). Open addressing has a failure mode chaining
does not, and it is invisible to any benchmark that only looks up keys that are present.

`cn1Marker` spreads the hash with the JDK's `h ^= h >>> 16`, which was designed for a
**chained** map, where colliding keys share a bucket and clustering costs nothing.
`Integer.hashCode()` is the value itself, so under that spread a dense key range -- ids from
zero, epoch seconds, counters, indices -- lands at `slot == value`, one contiguous run of
occupied slots with no gap. Linear probing then walks that run end to end for any probe that
enters it and does not find its key. Measured average probe length for a MISS:

| entries | miss probes, `i + 1` | miss probes, perturbed |
|---|---|---|
| 20,000 | 2,547 | 1.39 |
| 100,000 | 16,742 | 1.53 |
| 1,000,000 | 222,721 | 1.98 |

That is **O(n) per unsuccessful lookup**, and it reaches `get` returning null, `containsKey`
returning false, and `put` of a key not adjacent to the run. In wall time on this Mac, 3M
`containsKey` calls against a 100k-entry map took **32.7 seconds**, against 49.9ms on
HotSpot -- 655x. A remove/insert churn shape took 7.8 seconds against 12.5ms.

**Hits stayed at exactly one probe the whole time.** `hashMapChurn` in `CommonWorkloads` is
get+put on keys that are present, so it reported a healthy 1.12x throughout. That is the
lesson worth keeping: for an open-addressed table, a hit-only benchmark cannot see the
defect that matters, and neither can a checksum.

The fix is the probe SEQUENCE, not the spread (`cn1NextSlot` / `cn1HmNextSlot`, CPython's
dict recurrence). Scrambling the hash instead was tried first and is the wrong trade: it
fixes the miss but destroys the sequential placement, and dense-key **build** and **scan**
shapes regressed 1.8x-2.2x, because inserting keys 0..n in ascending slot order is a
sequential memory walk and HotSpot's `HashMap` gets the same benefit from the same weak
spread. Keeping the first probe at `marker & mask` and perturbing only the steps after it
keeps that locality and still leaves the run immediately.

Residual cost, measured interleaved best-of-N: `largeTable` (1M-entry map, random hits) 1.26x
and `hashMapChurn` 1.07x, against 728x and 471x the other way; `vm/benchmarks` geomean moved
1.005, i.e. not at all. `MapTorture` stays bit-identical.

`vm/benchmarks/src/com/bench/MapBench.java` is the suite that found this -- miss-heavy,
String-keyed, large-table, tombstone-heavy, grow-dominated and identity-keyed shapes. It is
deliberately NOT in `CommonWorkloads`, because
`scripts/hellocodenameone/conformance/port_status.py` requires exactly ten benchmark ids
there.

### The growth rule assumed a load factor it never checked

All three compact maps decided whether to double or to rebuild-in-place with
`elementCount * 2 >= capacity`. That is a capacity test standing in for a threshold
test, and it silently assumes a load factor of 0.5 or more. Below that the threshold is
reached while the table is still less than half full, so the rebuild keeps the same
capacity, the rebuilt table is immediately at its threshold again, and **every
subsequent put rebuilds the whole table**. Inserting 20000 entries at a load factor of
0.25 cost 19999 rebuilds and 200 million rehashed entries -- **22.3 seconds against
16.4ms once fixed, 1362x**. The two-argument constructors accept any positive load
factor, so `new HashMap<>(16, 0.25f)` reached it from ordinary code.

The rule is `elementCount >= threshold` -- grow when the LIVE count has reached the
threshold, and rebuild at the same size only when the threshold was reached because of
tombstones. At 0.75 and 0.5 the two rules agree rebuild for rebuild, which is why no
existing benchmark or torture moved, and why none of them could have caught it:
`MapBench.lowLoadFactorBuild` and `HtTorture`'s `sparse` case exist for this alone.

### The neighbours

`java.util.Hashtable` has since been given the same compact layout (open addressed, no
`Entry` per mapping, the same perturbed probe). Build got 1.74x faster. **Lookup only got
1.09x**, and the reason is worth knowing: with identical probe code, `Hashtable` is still 3x
`HashMap` on the same workload, and the difference is `synchronized`. ParparVM keeps monitors
in an address-keyed side table (`CN1MonitorEntry`, 4096 buckets in `cn1_globals.m`) rather
than an object header word, so an uncontended `synchronized` accessor costs roughly 23ns --
which is more than the entire lookup. Do not look for more in the map; look at the monitor.

`java.util.IdentityHashMap` also got fixed, and it is the cautionary tale of the group.
**Do not port HotSpot's hash function to this VM.** Its `identityHashCode` is a scrambled
per-object value; ours is a truncated object ADDRESS, measured 32-byte aligned (five always-zero
low bits). `java.util.IdentityHashMap` indexes with `(h << 1) - (h << 8)`, i.e. `h * -254` --
an EVEN multiplier, which preserves those zeros and adds a sixth. Copying it reached 2045
distinct home slots out of 65536 and 12.73 probes per lookup, and made the class measurably
SLOWER than the unscrambled modulo it replaced. What works is folding the high half down
first (`h ^= h >>> 16`), which reached 50000/65536 home slots and 1.00 probes. An extra
multiply on top made it worse again (2.36 probes): sequentially allocated objects have
sequentially increasing addresses, so one fold is already near a perfect hash and scrambling
it re-randomizes near-perfect placement into collisions. Net 2.65x on both lookup and build.
`vm/benchmarks/src/com/bench/IdmProbe.java` is the diagnostic that settled this; it must run
ON the target, because the input distribution is a property of the allocator.

`LinkedHashMap` inherits the compact layout but overrides `get`/`put`/`remove`/`clear` in
Java rather than native (the natives are deliberately base-class-only), which measures 1.21x
over `HashMap` on the same shape. Still open. Note #5658 already fixed a different
`LinkedHashMap` cost -- the `CompactEntry` allocated per insertion to feed `removeEldestEntry`
-- so do not confuse the two.

### A trap in the benchmark target itself

A null receiver is a hard SIGSEGV on the `clean` target these benchmarks and tortures run on,
not a `NullPointerException`. On iOS it IS an NPE, but only because the port installs a
SIGSEGV handler (`installSignalHandlers` in `CodenameOne_GLAppDelegate.m`); the clean and
desktop targets install nothing, as `cn1ThrowNullPointerOrDie`'s comment in `cn1_globals.h`
says. So `Hashtable.get(null)` -- which reaches `key.hashCode()` -- crashes the harness with
no output and exit 139. A torture cannot assert on null-receiver NPEs; only on the explicit
`throw new NullPointerException()` paths.

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
`CN1_WL_BRANCH`, `CN1_WL_SLEEP_MS`, ...). It predates `main(String[])` receiving the real
command line -- the clean target's generated `main()` used to pass `JAVA_NULL` for args --
and stays environment-driven because every A/B script already sets it up that way.
Sweeping `CN1_WL_SLEEP_MS` over `{0,1,10,100,1000}` is the cheapest discriminator between a
rate problem and a retention problem, and needs no rebuild.

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

## java.lang.ref: what it cost, and what the ranking did not buy

The collector clears references itself. The referent lives in `java.lang.ref.Reference`
and the translator does NOT emit a `gcMarkObject` for it
(`ByteCodeClass.isReferenceReferent`): it emits `cn1GcDiscoverReference`, which hands the
collector the field addresses and decides soft retention on the spot. Clearing happens in
`cn1GcProcessReferences`, inside the SATB termination loop, using the sweep's own liveness
test -- `mark != -1 && mark < currentGcMarkValue - 1`, both halves of the sweep agree on
it. Clearing a reference the sweep keeps wastes a cache entry; failing to clear one it
frees is a dangling read, which on this VM is a native crash no Java catch can see.

**The clear pass must run with the SATB barrier still ARMED.** A thread scanned and
released early can pull a referent out through `get()` and hold it in a local the
collector has already walked past, and that referent is then neither marked nor fresh --
the one case the sweep's "already marked or FRESH" invariant does not cover. `get()`
therefore carries a load barrier, emitted into
`get_field_java_lang_ref_Reference_objReference`, and a racing read makes the trial clear
of `gcSatbActive` find a non-empty log, which re-arms and re-runs the fixpoint and this
pass with it.

**Filter that barrier or the collector stops converging.** Logging every referent read is
not a cost, it is a failure: `cn1SatbEnqueue` takes a mutex per accepted reference and
`get()` on a hot cache is called far more often than any store barrier sees. Measured on
`RefPolicy` before the filter existed -- over 10,000 log entries per cycle and
`CN1_SATB_MAX_REOPENS` (32) reached on EVERY cycle. `CN1_SATB_REF_LOAD` skips referents
already marked this epoch or fresh, which are exactly the ones the clear pass would refuse
to clear: passes 32 -> 1, keptTouched ~10,000 -> ~330, refMs 0.06 -> 0.005.

### The measurement, and the three ways it lied first

`vm/benchmarks/src/com/bench/RefPolicy.java` + `ab-refs.sh`, arms `-DCN1_NO_WEAK_REFS`
(references strong, what this VM did before) and `-DCN1_REF_POLICY=0|1|2`
(pressure-triggered all-or-nothing / never clear / ranked by age). Five interleaved reps,
`CN1_SIMULATE_PROC_MEMORY_LIMIT`, checksums identical across every arm:

| ceiling | arm | hit rate | footprint | refMs | % of mark | weak cleared |
|---|---|---|---|---|---|---|
| 128MB | noweak | 97.44% | 63.5MB | 0.002 | 0.00% | 0/256 |
| 128MB | pressure | 84.99% | 63.1MB | 2.760 | 2.46% | 255/256 |
| 128MB | never | 97.44% | 63.8MB | 1.289 | 1.88% | 255/256 |
| 128MB | ranked | 96.77% | 62.6MB | 1.197 | 1.79% | 255/256 |
| 160MB | pressure | 87.99% | 91.0MB | 2.611 | 16.32% | 255/256 |
| 160MB | ranked | 97.44% | 82.1MB | 0.421 | 3.63% | 255/256 |

Read it in this order. **References themselves are unambiguous**: 255/256 unreachable
referents reclaimed against 0/256, for 1.8-4% of mark time and a `vm/benchmarks` geomean
of 1.011 over 12 interleaved reps against master. **The pressure-triggered arm is strictly
dominated** -- it gives up 12 points of hit rate and saves no footprint at all, and at
160MB it is worse on BOTH axes. That arm is the model of the iOS port's
`didReceiveMemoryWarning -> flushSoftRefMap`, so it is the thing being replaced, not a
strawman. **The ranking buys nothing over never-clearing here**: same hit rate, ~1MB less.
It is defensible because it costs almost nothing and because it dominates the pressure
arm, not because this measurement shows it winning.

Three wrong conclusions were drawn from single runs before that table existed, and each
survived until the data contradicted it:

- **"Ranking is the difference between finishing and not."** True of the outcome, wrong
  about the cause: the arms that did not finish were not out of memory. `sample` on a
  wedged process put the mutator 100% in `cn1PacingPark` at 44MB of a 96MB ceiling. The
  chain is retain-everything -> the collector cannot shrink the live set -> the pacing loop
  parks the mutator to hold the budget. Reach for the stacks first, as the demand-signal
  note above already says.
- **"The pressure arm fails because all-or-nothing thrashes."** It never fired at all.
  Its trigger was below the pacing reserve, and defending that reserve is what the pacing
  loop DOES, so headroom converges on the trigger and stops falling. **Any
  pressure-triggered cache policy on this collector has that trap waiting: it waits for a
  signal the collector exists to suppress.** The second attempt then wrote the bands as
  multiples of the reserve, where `reserve * 4` IS the whole budget, so the top band was
  unreachable and the arm fired always. Write bands as explicit fractions; reachability is
  then visible on inspection.
- **"The non-trimming arms collapse."** `never` was 4x FASTER than `noweak` at 2,000
  accesses and 100x slower at 6,000. Below roughly 1.8x the cache size this workload is
  **bistable** -- once pacing engages, throughput drops two orders of magnitude, and
  whether a run falls in is timing-sensitive. Single runs there measure the coin. If that
  regime is what you want, count how many of N runs complete; do not time one.

**What is still not measured.** Nothing here separates ranking by RECENCY from "trims at
all" -- there is no random-eviction arm at a matched rate, so the LRU claim is unproven,
only the trimming claim. And the 64MB-cache-against-a-128MB-budget shape is a choice made
to stress the policy, not a measured property of any app.

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
time the mutator threads were RUNNING.

**The collector is not a mutator, and it is easy to leave it in the denominator.**
`threadRunner` sets `lightweightThread = JAVA_TRUE` on every Java thread, the GC thread
included, so summing all of them divided the aggregate stall by six thread-seconds instead
of five on a four-worker run and OVERSTATED duty. `cn1StallSumThreads` excludes
`System.gcThreadInstance` for that reason. Every duty figure quoted here was re-measured
after that correction; earlier drafts of this file, the commit messages on the branch and
the pull request description carry the pre-correction pair (51% -> 90%) and should not be
copied forward. That single number is what the whole issue was
about, and no earlier instrument could produce it.

**Three things about dutyPct that were wrong, because a duty figure is easy to compute and
hard to compute correctly.** All three were found by review or by pointing the instrument at
a workload whose threads come and go, and all three inflated it.

- **The collector was in the denominator.** `threadRunner` sets `lightweightThread =
  JAVA_TRUE` on every Java thread, the GC thread included, so a four-worker run divided the
  aggregate stall by six thread-seconds instead of five. `cn1StallSumThreads` excludes
  `System.gcThreadInstance`. Re-derived on the corrected instrument, interleaved, median of
  three, the headline pair this branch reports is **37% -> 85%**; anything quoting
  51% -> 90% predates the correction.
- **The stall clock died with the thread that earned it.** It used to be summed from a
  per-thread counter over the LIVE threads, and `markDeadThread()` drops a TLD out of
  `allThreads` on exit -- so the next sample's delta went negative, got clamped to zero, and
  the line reported **100% duty exactly at a thread-generation boundary**. Measured at exit
  the live-thread walk returned 0 against a process-wide 9.4-35.2 seconds. The total now
  comes from `cn1StallMutatorNs`, a process-wide accumulator that nothing removes, and the
  per-thread counter is gone.
- **The numerator counted threads the denominator could not.** `cn1StallNs[]` looks like the
  obvious process-wide total and is the wrong one: `cn1GcSignalHandler` charges
  `CN1_STALL_SIGNAL_STOP` to whatever thread the signal interrupted, and under conservative
  roots that includes NATIVE threads, which are not `lightweightThread` and contribute no
  thread-time. Measured, they are **1.9-4.2% of the total on the churn and thread-churn
  shapes and 2.4-15.4% under `CN1_GC_SIGNAL_STOP=1`** -- enough to bias duty down, and to
  drive it negative on anything native-thread-heavy. `cn1StallMutatorNs` counts only
  lightweight, non-collector threads, so numerator and denominator describe one population;
  with it the two stop modes agree (81.4-82.9%) where they previously did not. It excludes
  the collector by comparing thread-local POINTERS, published by `cn1StallSumThreads` as it
  walks, because the filter runs inside a signal handler where reaching into a Java static
  is not safe.
- **The "1Hz" line was not 1Hz, and a short window printed NEGATIVE duty.** `usleep` returns
  early on `EINTR` and the signal-based thread stop delivers to the probe thread too, so the
  series collapsed to ~20ms windows -- and four threads easily accrue more stall than 20ms
  of one thread's wall clock. The loop now sleeps in slices until a second of monotonic time
  has genuinely passed, and integrates the live mutator count across those slices, so the
  denominator is real thread-time rather than one end-of-window count times elapsed wall
  time. That integral is also what makes it correct when threads are created and destroyed
  inside the window. `MutatorChurnDuty` is the driver that shows all of this.

**Bulk reference copies take the SATB mutex once per chunk, and shut down with a
handshake.** `cn1SatbEnqueue` locks per accepted reference, which is right for the per-store
barrier and wrong for `cloneArray` / `arraycopy` on an object array -- the grace-pass audit
put both through it, turning one `memcpy` into an acquisition per element. `cn1SatbEnqueueRange`
filters unlocked and flushes 256 at a time: **1.155 -> 0.0055 acquisitions per logged
reference, 210x fewer** (`satbLocks`/`satbRefs` under `CN1_GC_CONFORM`, driver
`BulkCopyCost`, `-DCN1_SATB_NO_BULK` for the A/B). Normalise per logged reference or the
comparison inverts: the arms do not log the same amount, because a mutator not serialising
on a mutex gets further through its copies inside the same mark, which also makes the bulk
arm's `markMs` and `satbMs` read HIGHER at an unchanged 6.4 -> 6.1ns per drained entry.
Chunked rather than one hold for the whole range, because a single acquisition across a
million-element array would block `cn1SatbTake` for the entire walk.

**Clearing `gcSatbActive` is a TRIAL, not the end of the mark.** The closing catch after the
fixpoint can discover objects, and `gcMarkDrain` scans what it discovers -- so with the flag
already down, an object sits GREY and unwatched while it is scanned. A mutator moving an old
child out of it into a fresh container in that window logs nothing on either side: the drain
scans the object the child has already left, the grace pass is long past the destination, and
the child is unmarked, not fresh, and reachable only from the fresh container. The sweep takes
it. So the clear is provisional -- an empty catch means closed, a non-empty one puts the
barrier back UP before that batch is marked and re-runs the fixpoint.

**The only exit is an empty catch.** There is no "that batch marked nothing new, so stop"
shortcut, and adding one is a bug: entries logged while the barrier was back up would then
never be taken at all, and an unmarked non-fresh reference stored into a live or fresh
container in that window gets swept.

**Know where the regress ends.** Every take leaves a window after it in which a store can
still log, so "drain what was logged during the last drain" has no fixed point a concurrent
collector reaches on its own -- closing it completely means holding the mutators still, which
is the stop-the-world pause this collector exists to avoid. The loop converges anyway because
a cleared flag stops mutators logging within one barrier's worth of instructions and
`cn1SatbBulkQuiesce` holds the bulk writers outright. `CN1_SATB_MAX_REOPENS` bounds it only
against a mutator storming references; reaching it falls back on the invariant the sweep has
always relied on -- a reference stored after the mark reaches its fixpoint is already marked
or FRESH, and the sweep keeps both.

`satbReopens` (`CN1_GC_CONFORM`) counts the re-arms, so all of this is answered by the
instrument rather than by argument. Worst case per cycle: **0 on the churn workload in both
stop modes and on the legacy-heavy shape, 4 on `BulkCopyCost`**, against a cap of 32. Take
that 4 seriously: the window is reachable rather than theoretical, and it shows up on exactly
the bulk copy paths this issue put a barrier on. The cap is set well above the measurement
rather than just above it, because reaching it silently substitutes the weaker invariant.

The flag check and the append are two steps, so the collector can clear `gcSatbActive` and
run its final `cn1SatbTake()` between chunks and strand entries in a log this cycle never
drains again. For a single store that window is argued harmless where the flag is cleared;
for `cloneArray` it is not, because the copy publishes into a brand new array the grace pass
has ALREADY walked past, so a dropped reference is one the sweep can free under a live
pointer. `cn1SatbBulkQuiesce` closes it: the enqueuer registers before re-reading the flag,
the collector clears the flag then waits for the count to reach zero before the final take,
both sides seq_cst. It is safe to spin on because nothing between register and deregister
can block or reach a safepoint. It costs 3.2%, inside this host's noise.
`-DCN1_SATB_NO_BULK_HANDSHAKE` compiles it out. This deliberately does NOT close the same
window on the per-store barrier: there the handshake would have to be an unconditional
atomic on every reference store, which is the exact cost that barrier is designed around.

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
on the churn workload, median of three: **2.8x the search throughput, duty cycle 38% ->
86%, mean mutator stall 213ms -> 15ms, and footprint DOWN 16%** -- a collector that runs
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
  -38% footprint, but duty cycle only reaches ~53%, because the per-cycle legacy costs are
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

### The BiBOP grace pass: investigated, and deliberately not changed

It is the single largest item left in a mark -- 50% on the legacy-heavy shape, ~70% on the
pure-churn one. The conclusion is that it is not doing anything wasteful, and the one
optimization that would help cannot be made safe with the gates this repo has.

**What it costs, measured** (`[GCSTALL] gracePagesWalked/graceSlotsWalked/graceSlotsFresh/
graceMarked`, `-DCN1_GC_CONFORM`): per cycle it skips ~9,950 pages on `gcAllocedSinceSweep`,
walks ~1,900, and of the 1.2M slots it touches **82-91% are genuinely fresh** and 68-77% get
a `gcMarkObject`. So the prune works and the walk is not the cost: the cost is
**828,000-1,520,000 `gcMarkObject` calls per cycle**, at roughly 35ns each including the
subsequent trace. The pass treats the entire fresh generation as roots, because the sweep's
one-cycle grace rule keeps every fresh object whether or not it is reachable, and an OLD
object reachable only through one of them would otherwise be swept under it.

Skipping the non-fresh slots it walks would save ~5ms of ~30ms and needs a new per-page
invariant (fresh slots are contiguous only on pages that have not allocated from their free
list). Not worth it.

**The optimization that would work, and why it is not here.** Objects allocated *while the
SATB barrier is armed* do not need tracing at all: every reference stored into them is
logged by the insertion half, which exists for exactly that case ("the container it is
stored into is a fresh grace object not yet reachable"). Snapshotting each page's
`bumpIndex` when the barrier arms and walking only below it would skip the ~60-70% of the
fresh set allocated during the mark -- allocate-black, the standard answer.

It depends entirely on the barrier being COMPLETE, and auditing that turned up two bulk
copies of object references that bypass the per-element setter and so fire no insertion
barrier at all: `java_lang_System_arraycopy` on an object array (it had the deletion half
only) and `cloneArray` (it had neither). Both are fixed here, and the fix is free (geomean
0.994) because it only runs during a mark.

Then the decisive part: **the verifier cannot see this window.** Two purpose-built drivers --
single-threaded and four-threaded, ~100 verify passes each, the destination made unreachable
so only the grace rule keeps it -- report `violations=0` *with the barrier deliberately
compiled out* (`-DCN1_NO_BULK_INSERTION_BARRIER`). The window is real by inspection and
narrow enough that neither `run-gc-verify.sh` nor the gauntlet can open it. Making the grace
pass depend on an invariant no gate can falsify would trade a measured 50% of mark time for
a correctness risk that would surface as silent heap corruption in a customer app, days
later, with no reproducer. `BulkCopyBarrier` stays in the verifier's driver list because it
exercises both bulk paths; it is NOT a self-test, because a self-test that cannot fail is
worse than none.

If this is ever revisited, the thing to build FIRST is a way to drive an allocation into the
residual window on purpose -- the phases after the grace walk and before `gcSatbActive` is
cleared -- because without that, no version of this change can be validated.

### There are no safepoint polls in generated code

The mark phase stops each lightweight thread cooperatively: it raises `threadBlockedByGC`
and spins on `while(t->threadActive)` until the thread parks itself. **Nothing in the
translator emits a safepoint poll** -- not on method entry, not on loop back-edges; grep
`vm/ByteCodeTranslator/src/com/codename1/tools/translator` for `threadBlockedByGC` and
there are no hits. Every safepoint in this VM lives inside a *runtime function*:

- `codenameOneGcMalloc`'s handshake (legacy allocations),
- `cn1BibopMaybeGc`, reached **once per 64KB PAGE**, not per object -- a thread bumping
  inside a page it already holds passes no safepoint,
- contended `monitorEnter`,
- `Thread.sleep` / `Object.wait`, and the `CN1_YIELD_THREAD` native bracket.

`monitorEnter`'s **first-creation** branch is the odd one out and is worth fixing on its
own: it `pthread_mutex_lock`s with `threadActive` still TRUE, where the contended branch
right below it parks first. A thread that publishes the monitor into the side table, drops
the critical section and is then beaten to the mutex by a second thread -- which parks and
holds it across the GC handshake -- blocks there while still counted active, which is a
three-way deadlock (collector waits for it, it waits for the mutex, the holder waits for the
collector). The escalation below rescues that on POSIX and does not on Windows. Left alone
here deliberately: it is a different bug on a hot path and wants its own change and gate.

So a Java loop that allocates nothing new and enters no contended monitor reaches **no
safepoint at all**, and that spin never ends. It is not a slow GC, it is a whole-VM freeze:
every other thread parks at its next allocation waiting for a cycle that can never start.
Issue #5537's reporter caught it in the debugger with the collector `totalwait =
609491500` microseconds -- **10 minutes 9 seconds** -- into that spin while a game-tree
search ran a compute-only evaluation loop.

`CN1_GC_SAFEPOINT_WAIT_MAX_US` (250ms) bounds the spin, and past it the collector freezes
the thread with the **same SIGUSR2 stop it already uses for genuine native threads**
(`cn1GcSignalStopOne`), which needs no cooperation. Measured by
`GcUncooperativeThreadIntegrationTest`, one 6s compute-only spin against a churning
allocator: **maxStall 6186ms of a 6187ms spin without it, 339ms of 5948ms with it**. The
ablation arm is `-DCN1_GC_NO_FORCE_STOP`, and the gate requires it to reproduce the wedge.

Four things the escalation has to respect, all of them consequences of a thread being
frozen wherever it happened to be rather than at a point it chose:

- **Nothing may allocate while the freeze is held.** The thread can be frozen mid-`malloc`
  holding the libc allocator lock. `cn1GcBuildRootSnapshots` reallocs, so it runs BEFORE
  the freeze and is skipped at both of its usual call sites for the rest of that thread's
  iteration. The scans themselves are malloc-free (fixed-size mark worklist; the
  force-visited side table is only touched on the `force` path, which no root scan takes).
- **The pending-allocation table is not migrated.** The append is `pending[size] = o;
  size++`, so a thread frozen between the two stores has an object the count does not
  cover; migrating and resetting `size` to 0 would orphan it and then hand its slot back
  out. The table is not a root source -- it only decides what the SWEEP may consider -- so
  skipping it for a cycle costs a deferred reclaim and nothing else.
- **The freeze is released as soon as the roots are captured**, not at the
  `threadBlockedByGC` clear. A parked thread waits in `usleep`; a signal-frozen one waits
  in an async-signal-safe BUSY spin, so holding it across the mark drain would burn a core.
  SATB is what makes an early release safe, and it is already the only thing keeping
  genuine native threads honest.
- **Do not signal a thread that is already frozen.** A second SIGUSR2 aimed at a thread
  spinning inside the handler stays pending until the handler returns, so a nested stop
  spins out its whole timeout and then reports failure on a thread that is demonstrably
  stopped. `gcMarkForcedStop` tells `cn1GcScanThreadNativeStack` to reuse the capture.

Two configurations deliberately do NOT get the escalation, and both are enforced in the
`CN1_GC_CAN_FORCE_STOP` guard rather than argued at the use site. **Windows**, which has no
POSIX signals -- proceeding without stopping the thread would miss its roots and free live
objects, which is worse than a hang. And **`-DCN1_DISABLE_SATB`**, because the early
release is what keeps the frozen window small, and only the barrier makes an early release
sound; holding the freeze through the drain instead drags `markStatics` (force-marking,
which mallocs through the force-visited table) and `gcMarkDrainParallel` (lazy
`pthread_create`) inside it, which is a wedge in the middle of the fix for a wedge.

A thread inside its own **nursery minor collection** is never frozen either.
`cn1NurseryWriteBarrier` raises `nurseryPromoting` and leaves `threadActive` TRUE for the
duration, so it is a prime escalation candidate -- and the root scans mark through the
TARGET's thread state, where that flag makes `gcMarkObject` promote-or-return without
marking anything. Freezing one would hand the sweep a thread whose roots were all silently
skipped. The check runs AFTER the stop, because a flag read while the thread is still
running can be raised in the window before the signal lands.
The proper long-term answer is a back-edge poll in the translator; it costs throughput in
every loop the VM ever runs, and this makes the pathological case survivable without paying
that everywhere.

**The diagnostic aimed at exactly this was dead for its whole life.** The spin's warning
computed `long later = time(0) - now` -- SECONDS -- then tested `later > 10000` and printed
`later / 1000` as "seconds". It first became eligible after 2.8 hours and would have
understated by 1000x, so the ten-minute freeze above printed nothing and had to be read out
of a debugger. `totalwait` was also an `int`, which is signed overflow at ~36 minutes of
waiting. If an instrument has never been seen firing, assume it does not.

### Never call into Java from a parked thread

`java_lang_System_gc__` enters `synchronized(LOCK)`, and `monitorEnter` is a GC safepoint.
Calling it from a thread that has already published `threadActive = FALSE` takes that thread
back OUT of the parked state -- which is the state `codenameOneGCMark` is spinning on in its
`while(t->threadActive)` wait -- and can block it inside the monitor while the collector
waits for it to go quiescent. That is a circular wait and it deadlocks the process:
collector in the mark waiting for a mutator, every mutator in `cn1PacingPark` re-requesting
a collection through the monitor.

Two sites did this: the budgeted pacing wait's periodic re-request, and the calloc-failure
path. Both now set `forceGc` directly (`cn1RequestGcFromParkedThread`), which is a plain
store the collector re-reads at the top of every loop pass.

**What that store loses is the notify, and "the collector will pick it up soon" is wrong
twice over.** Both corrections were paid for with a second hang:

- A plain store cannot wake a collector that is already inside `LOCK.wait()`, and a parked
  mutator allocates nothing, so `isHighFrequencyGC()` reads quiet at exactly the moment
  someone is waiting on it. The park therefore issues one real `java_lang_System_gc__`
  BEFORE parking, while still active, so the request carries a notify; the in-park
  re-requests stay plain stores, which is enough to keep an already-running collector going.
- `gcIdleWaitMillis` must never answer a CONSUMED request with the long idle. The code it
  replaced was `if(forceGc || isHighFrequencyGC()) { forceGc = false; LOCK.wait(200); }`, so
  forceGc *guaranteed* a 200ms wait; returning 30000 for a request it just consumed drops
  that guarantee in the one case that matters. `ProcessBudgetPacingIntegrationTest` under a
  120MB ceiling stalled out its entire 300s budget on this, and its own timeout message
  predicts it: "a park that waits on a collection nobody scheduled stalls exactly like
  this".

The window is not new. It was survivable only because the collector used to spend nearly all
of its time inside `LOCK.wait()` with the monitor released; answering the demand signal
removed that idle and made it acquire `LOCK` once per cycle at several hundred cycles a
second, which turned a theoretical race into a reliable hang. **A latency fix can convert a
dormant race into a live one -- the thing to re-run after one is the long soak, not the
microbenchmark.**

Two notes on finding it, because the first three hours went the wrong way:

- **Ablation macros bisect a hang badly.** Every arm hung sometimes, which reads as "not this
  one" for each in turn and is wrong: the hang was probabilistic and none of the ablations
  touched the cause. What settled it in one shot was `sample <pid>` on the wedged process --
  the GC thread in `codenameOneGCMark`, all four workers in `cn1PacingPark`, one of them
  inside `java_lang_System_gc__`. Reach for the stacks first.
- **A wall-clock elapsed figure can lie by minutes.** One soak rep reported `ELAPSED_MS=583031`
  under a 120s `timeout` -- the machine had slept, so `System.currentTimeMillis()` jumped
  while both the process and `timeout` were frozen. It had completed normally.
