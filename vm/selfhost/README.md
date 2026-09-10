# Self-hosting ParparVM

Builds `ByteCodeTranslator` with ParparVM itself: the translator's own bytecode,
plus ASM's, is translated to C and compiled into a native binary.

It buys two things:

1. **Validation.** The translator is a ~37k-line real program that exercises
   collections, strings, file I/O, exceptions and the GC at scale. Running the
   native build and the JVM build over the same input and diffing the emitted C
   is an end-to-end conformance test of the whole VM, and the corpus grows on its
   own as the translator does.
2. **Performance and memory.** A translation is a short-lived, allocation-heavy
   batch job -- the shape where AOT should beat a cold JVM.

## `stubs/`

The self-hosted binary does the `clean`/`ios`/`macos` translation and nothing
else, so a few classes are replaced by no-op stubs when it is built. They are
never selected at run time; they exist so the source set compiles without
dragging in API that ParparVM's JavaAPI deliberately lacks.

| stub | why |
|---|---|
| `Javascript*` | the JavaScript target, ~12.5k lines. Needs `java.util.regex` and `ConcurrentHashMap`. |
| `ArchiveClassScanner` | `java.util.zip`. Reachable only from `NativeSignatureVerifier`'s command-line entry point; the translator itself never reads an archive. |
| `DebugSymbolCompressor` | `java.util.zip` again, for the on-device-debug symbol sidecar. |

`java.util.zip` cannot simply be added to JavaAPI: JavaAPI is mirrored by
`Ports/CLDC11`, where the package does not belong.

Everything else the translator needs was removed from the translator rather than
added to JavaAPI -- see `Util`'s `splitLiteral`, `collapseWhitespace`,
`rewriteLocalObjectRefs`, `getProperty`, `listFiles` and `writeBytes`. Adding
`String.split`/`replaceAll` to JavaAPI in particular would have collided with
`BytecodeComplianceMojo`, which rewrites those calls onto
`com.codename1.util.regex` precisely because JavaAPI does not declare them.

## Building and verifying

```bash
export JDK_8_HOME=/path/to/a/working/jdk8
./build-selfhost.sh                                   # -> target/parpar
./verify-selfhost.sh <classesDir> <AppName> <package> # gates D and A
```

`build-selfhost.sh` compiles the source set against JavaAPI alone, stages ASM as
class directories (the translator walks directories, never archives), translates,
and clangs the result. The `-fwrapv -fno-strict-aliasing -fno-builtin-fmod(f)`
flags are mandatory for generated C -- Java arithmetic wraps and clang -O3
miscompiles without them.

The binary finds the C runtime it has to copy into its output through
`Class.getResourceAsStream`, which now consults resources linked into the
executable and then a search path named by `CN1_RESOURCE_PATH`. Before this it
returned a hard-coded null on every ParparVM target.

## State

Gate D (the native translator against itself) passes. Gate A (JVM against native)
is at **245 of 247 files byte-identical** on a JavaAPI-sized corpus, and binaries
built from the two trees produce identical output.

The two files that still differ are `java_util_HashMap.c` and `.h`: the native
translator's dead-code pass culls seven more methods than the JVM's
(`cn1PutSlot`, `cn1MaybeGrow`, `clearImpl`, `containsKeyImpl`, `getImpl`,
`putImpl`, `removeImpl`), and emits them as empty stubs. Both trees compile, link
and run correctly, so the extra culling is safe here, but the two runtimes should
not disagree and the cause is not yet found. What is already ruled out: it is not
nondeterminism -- gate D passes on both sides -- and it is not identity-hash
iteration order, which was tested directly by re-running the JVM under
`-XX:hashCode=2` and getting byte-identical output.

## What self-hosting has already found

Three defects that were invisible to every existing test, because each was
self-consistent on HotSpot:

- **`Integer.TYPE` and the other wrapper `TYPE` fields were null.** `TYPE =
  int.class` compiles to `getstatic TYPE; putstatic TYPE`. A `Map` keyed on them
  collapsed onto the single null key. `Util`'s primitive-to-C-type maps are exactly
  that shape.
- **C label names came from identity hash codes.** ASM's `Label.toString()` is
  `"L" + System.identityHashCode(this)`. That made the emitted C irreproducible,
  and on ParparVM -- whose identity hash is the object pointer narrowed to int, so
  often negative -- it emitted `label_L-180306432001`, which C reads as a
  subtraction. Every method with a try/catch failed to compile.
- **C local-variable declarations were emitted in `HashSet` iteration order**, so
  the same input produced different C. `debugVarEntries` had already had to learn
  this for the debug side-table; the declarations had the same defect.

Only the first is a runtime bug. The other two are reproducible-build defects in
the translator that a second runtime made visible.

## Performance

`bench-selfhost.sh` runs each arm over the same corpus, interleaved, and reports the
minimum wall clock and the peak `phys_footprint`. It refuses to print ratios unless
every arm emitted identical C. The reference JVM is **JDK 25** -- what HotSpot can
actually do; JDK 8 is kept only because it is what the builders currently fork.

Translating the self-hosting corpus (ASM + the translator's own classes, ~570
classes) on a 64 GB / 16-core Mac, release shape (`-O3 -flto=thin`):

| | wall clock | peak footprint |
|---|---:|---:|
| parpar | 1.84 s | 1443 MB |
| jdk25 | 1.56 s | 516 MB |
| jdk8 | 2.27 s | 502 MB |

**vs JDK 25: 1.18x slower, 2.79x more memory. vs JDK 8: 1.24x faster.**

Two fixes got it there from 6x slower; both are described below. Wall clock on this
machine is only meaningful when it is quiet -- at load 113 the same benchmark
produced samples from 3.6 s to 24 s for every arm, JVM included. CPU time
(`user+sys`) is far more robust to contention, and by that measure the two are
level or better: parpar 4.35 s against jdk25 4.82 s on a loaded host.

### Fix 1: the mutator slept instead of allocating

`sample` on the original build put 64% of the process's samples in one stack, and
the mutator was not marking or sweeping -- it was asleep:

```
Ldc.getValueAsString -> cn1BibopAlloc -> cn1BibopMaybeGc
  -> cn1PacingPark -> usleep -> nanosleep -> __semwait_signal
```

`CN1_LOG_PACING_PARKS` reported only **two** park events for the whole run, so each
was seconds long. `cn1BibopPacingCap` computed a generous cap -- `cn1CachedFreeMem/8`,
4 GB here -- and then clamped it to `trigger * 8` once the footprint passed
`CN1_PACING_GROWTH_FLOOR_BYTES`. That floor was a flat **512 MB**, and early in the
run the trigger is still at its own 24 MB floor, so the ceiling was **192 MB**
(`minCapKb=196608` confirmed it). A program with a ~1.4 GB live set cannot stay
inside a 192 MB allocation window, so it parked against a collector that could
never get under it.

A fixed 512 MB says "this process has grown"; it does not say the machine is under
pressure, and the bound exists for pressure. The floor now scales:
`max(512MB, availableMemory/4)`. Where `cn1_available_memory` is the flat 100 MB
placeholder (Linux, Windows, the non-Apple fallback) the absolute floor still wins
and behaviour is unchanged; the floor can only ever rise, never fall. This is the
no-per-process-ceiling path only -- where a ceiling exists (iOS's dirty-memory
limit, or an explicit budget) `cn1PacingPark` takes the bounded branch and never
reaches this code. `ProcessBudgetPacingIntegrationTest` confirms both halves: its
control arm reports `minCapKb=4194304` with no parks, and its budget-bounded arm
still holds a 120 MB limit at a 60 MB peak across 427 parks.

`cn1RefreshFreeMemCache()` also had exactly one caller, inside the mark cycle, so
`cn1CachedFreeMem` was 0 until the first collection and both the cap and this floor
fell to their absolute minimums during the window with the least reason to throttle.
It is primed in `cn1BibopDoInit` now.

### Fix 2: the constant pool was O(n^2)

With pacing out of the way, the main thread's own profile was dominated by
`Parser.addToConstantPool`, which did `constantPool.indexOf(s)` -- a `String.equals`
against every string already interned. On a self-hosting translation the pool holds
~200k strings: `String.equals` 11.2%, the list iterator 10.3%, `indexOf` 6.2% and
`ArrayList.get` 5.1% of main-thread samples, all of it there. A `HashMap` side index
answers the same question directly; the list stays the source of truth, so the
emitted indices are unchanged and gate A still passes byte-identical.

### What is left: memory

The remaining gap is peak footprint. Sweeping the GC trigger from 8 MB to 256 MB --
four cycles down to two -- moves peak by less than 15%, so this is retained data
rather than uncollected garbage, and page-pool slack is about 2 MB, so it is not
fragmentation either. `CN1_HEAP_REPORT` on a census build prints the split.

Two allocation defects came out of the per-class census and are fixed:

- **`IdentityHashMap` allocated an `Entry` on every `next()`**, even for key and
  value iteration, where the entry was built only to read one field back out of it
  and drop it. 1,366,140 of them, 43.7 MB, all garbage. `java.util.HashMap` already
  had separate key/value/entry iterators for exactly this reason and this map had
  been missed; it now has the same split.
- **`ArrayList()` eagerly allocated `Object[10]`**, a 128-byte slot for every list,
  including one never added to. It now shares a zero-length array until the first
  growth. The first growth allocates exactly ten and not the twelve the general
  growth path would pick, because ten keeps a small list in the size class it
  already occupied -- growing to twelve would have traded a win on empty lists for
  a loss on every list of one to ten elements.

Measured together on the self-hosting corpus:

| | before | after |
|---|---:|---:|
| allocations | 10,160,401 objects / 991 MB | 8,706,929 / 940 MB |
| legacy-heap objects | 729,174 | 444,783 |
| Java live heap | 860 MB | 770 MB |
| process peak | 1467 MB | 1324 MB |

`CollectionSemanticsIntegrationTest` holds both against a real JDK -- empty-list
operations, the three growth paths, identity semantics, null keys and values
through each of the three views, iterator removal, and a rehash. It was confirmed
to fail when the key iterator stops mapping the table's sentinel back to null.

**`HashMap` was investigated and deliberately left alone.** It eagerly allocates
three arrays (keys, values, meta) at capacity 16, which looks like the same defect,
but the maps in this workload are populated rather than empty. Rebuilding with a
default capacity of 1 -- the cheapest probe for "how much of that table is wasted"
-- made everything worse, because the maps then regrow repeatedly:

| default capacity | Object[] allocs | int[] allocs | Java live |
|---|---:|---:|---:|
| 16 (current) | 1,324,987 | 213,725 | 770 MB |
| 1 (probe) | 1,802,249 | 452,356 | 882 MB |

Growth there is also post-insert by design, so the shared-empty-table trick that
works for ArrayList would have the put path writing into the shared table. Not
worth it for an unmeasured win in the hottest class in the runtime.

### String: the NSString field is free

`java.lang.String` carries a `long nsString` for the Apple targets' direct NSString
mapping, and the obvious question is what that costs everywhere else. Measured:
nothing.

```
sizeof(obj__java_lang_String) = 48      nsString at offset 40
```

The fields before it end at 36 and the struct is 8-aligned, so four of those eight
bytes were padding already. Without the field the struct is 40 bytes -- and BiBOP's
size classes are 32, 48, 64, ..., so 40 and 48 both land in the same 48-byte slot.
Removing it would save zero bytes per String while costing the Apple targets a
side table and a lookup. Keep it.

The strings themselves are still the largest single consumer (`char[]`, 368 MB
allocated). Note that a compact Latin-1 path already exists for the concat
fast path -- `cn1FusedLatin1Begin` allocates the String and a `byte[]` payload in
one BiBOP slot -- so the remaining `char[]` volume is strings built some other way.
That is the next thing to look at.
