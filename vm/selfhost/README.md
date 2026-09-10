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

`bench-selfhost.sh` runs both translators over the same corpus, interleaved, and
reports the minimum wall clock and the peak `phys_footprint`. Ratios are refused
unless the two emitted identical C -- a speed number from a translator that emits
different output is meaningless.

Translating the self-hosting corpus (ASM + the translator's own classes, ~570
classes) on a 64 GB / 16-core Mac, release shape (`-O3 -flto=thin`), against JDK 8:

| | wall clock | peak footprint |
|---|---:|---:|
| jdk8 | 1.17 s | 509 MB |
| parpar, as shipped | 6.7 - 8.7 s | 1434 MB |
| parpar, pacing growth clamp disarmed | **1.39 - 1.52 s** | 1467 MB |

**Nearly all of the wall-clock gap is one pacing policy, not collection work and
not code quality.** Building at `-O1` instead of `-O3 -flto=thin` measures the
same, and with the clamp disarmed the collector still runs its four cycles.

### Where it goes

`sample` on a default run puts 64% of the process's samples in one stack:

```
Ldc.getValueAsString -> cn1BibopAlloc -> cn1BibopMaybeGc
  -> cn1PacingPark   (3491 of 5476 samples)
     -> usleep -> nanosleep -> __semwait_signal   (3475)
```

The mutator is not marking or sweeping. It is asleep in the allocator's
backpressure loop. `CN1_LOG_PACING_PARKS` reports only **two** park events for the
whole run, so those two parks are seconds long each.

### Why

`cn1BibopPacingCap` computes a generous cap -- `cn1CachedFreeMem / 8`, which is
4 GB on this host -- and then clamps it:

```c
long capCeiling = trigger * CN1_BIBOP_GC_MAX_CAP_MULTIPLIER;   /* 8 */
if(cap > capCeiling && cn1PacingPastGrowthFloor()) cap = capCeiling;
```

`cn1PacingPastGrowthFloor()` is true once the process footprint passes
`CN1_PACING_GROWTH_FLOOR_BYTES`, which is **512 MB**. Early in the run the GC
trigger is still at its own floor of 24 MB, so the ceiling is 24 x 8 = **192 MB**
-- and `CN1_LOG_PACING_PARKS` reports exactly `minCapKb=196608`. A program whose
live set is ~1.4 GB cannot stay inside a 192 MB allocation window, so it parks
waiting for a collector that can never get under it.

This is a policy calibrated for phone-sized heaps, where bounding RSS is worth
real throughput. It has no scaling for a host with 64 GB of RAM: **disarming it
cost 2% more memory (1434 -> 1467 MB) and returned 5x the speed.** Whether and how
to scale it -- with available RAM, with a process budget, or by letting the
trigger rise faster before the clamp engages -- is a policy decision for the VM
owners, not something this project should decide. The reproduction is one
`#define`:

```bash
CN1_SELFHOST_CFLAGS="-flto=thin -DCN1_PACING_GROWTH_FLOOR_BYTES=1099511627776LL" \
  ./build-selfhost.sh -O3
```

A related but secondary defect **is** fixed here: `cn1RefreshFreeMemCache()` had
exactly one caller, inside the mark cycle, so `cn1CachedFreeMem` was 0 until the
first collection and the cap fell to its 72 MB floor rather than 192 MB during the
window with the least reason to throttle anything. It is now primed in
`cn1BibopDoInit`.

### What is left, once pacing is out of the way

Against JDK 8: **1.19x slower** and **2.9x more memory**. The time is ordinary
AOT-versus-warmed-JIT territory. The memory gap is real and separate, and worth
noting that the JVM figure is bounded by its own heap ergonomics -- it collects to
stay under a default maximum, while the native binary has no such ceiling -- so
this compares what each process used, not the live set.

