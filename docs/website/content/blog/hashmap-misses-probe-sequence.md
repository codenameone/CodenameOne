---
title: "Faster Maps: Chasing Swiss Speed"
slug: hashmap-misses-probe-sequence
url: /blog/hashmap-misses-probe-sequence/
date: '2026-09-13'
author: Shai Almog
description: "ParparVM fixes pathological map misses and extends tagged boxed values. Probe counts, allocation coverage, and regressions show where ordinary Java collections became cheaper."
feed_html: '<img src="https://www.codenameone.com/blog/hashmap-misses-probe-sequence.jpg" alt="Faster Maps: Chasing Swiss Speed" /> ParparVM fixes pathological map misses and extends tagged boxed values. Probe counts, allocation coverage, and regressions show where ordinary Java collections became cheaper.'
series: ["release-2026-09-11"]
---

![Faster Maps: Chasing Swiss Speed](/blog/hashmap-misses-probe-sequence.jpg)

After looking at Go's collector, we turned to its maps. Swiss tables have an appealing premise: keep entries compact, use a small amount of metadata to narrow the search, and avoid chasing a separate object for every mapping. ParparVM already had compact arrays and separate metadata. We were starting closer than I expected.

Then a missing key spoiled the picture. Three million `containsKey` calls took 32.7 seconds, while the benchmark we usually watched still looked healthy. It mostly asked for keys that existed.

The investigation took us through the probe sequence, string comparisons, and the boxed numbers stored inside the table. [The map changes](https://github.com/codenameone/CodenameOne/pull/5722) and [wider tagged values](https://github.com/codenameone/CodenameOne/pull/5735) ended up attacking different costs in the same Java collection.

## One probe for a hit, thousands for a miss

ParparVM's HashMap stores entries in arrays and resolves collisions by probing other slots. Integer hashes preserve the value, and the familiar `h ^= h >>> 16` spread leaves small dense integer keys close to their original positions.

Keys from zero upward therefore form an uninterrupted run. If the key exists, its home slot is often exactly right. If a missing key's probe enters that run, linear probing walks until it finds an empty slot. Deletions leave tombstones that cannot terminate a search either.

```java
Map<Integer, Integer> values = new HashMap<Integer, Integer>();
for (int i = 0; i < 100000; i++) {
    values.put(i, i);
}
// A benchmark must also exercise absent keys that collide with occupied runs.
boolean present = values.containsKey(-1);
```

To exercise different collision patterns, we expanded [MapBench](https://github.com/codenameone/CodenameOne/blob/963764b5e7/vm/benchmarks/src/com/bench/MapBench.java) with misses, tombstones, growth, string keys, and identity keys, controlling the key distribution for each run.

| Map entries | Mean probes per miss before | After |
| --- | --- | --- |
| 20,000 | 2,547 | 1.39 |
| 100,000 | 16,742 | 1.53 |
| 1,000,000 | 222,721 | 1.98 |

Hits stayed at one probe. That explained why the old benchmark looked so good: it kept landing straight on the key while the missing-key search walked past thousands of entries.

## Keep the first slot, change the collision path

Scrambling hashes more aggressively fixed the miss problem. It also made dense-key construction and scans 1.8 to 2.2 times slower in the measured shapes. Those operations benefited from adjacent keys occupying adjacent slots.

The chosen fix keeps the first probe at `marker & mask`, then uses the recurrence associated with CPython's dictionary probing. The Java helper is small:

```java
static int cn1NextSlot(int i, int perturb, int mask) {
    return ((i << 2) + i + 1 + perturb) & mask;
}
```

The caller shifts the unsigned perturbation right by five bits between probes. Once it decays to zero, the recurrence still traverses the power-of-two table. Native and Java implementations must follow the same sequence, including during growth and deletion.

{{< mermaid >}}
flowchart TD
    K[Hash the key] --> H[Try the home slot]
    H --> Q{Matching entry?}
    Q -->|Yes| V[Return value]
    Q -->|No| E{Empty slot?}
    E -->|Yes| N[Key absent]
    E -->|No| P[Advance with perturbed probe]
    P --> Q
{{< /mermaid >}}

## Where the Swiss idea helped

Go's [Swiss maps](https://go.dev/blog/swisstable) compare compact control metadata before loading full keys. That puts more of the search into a small amount of contiguous memory. Our existing layout already separated metadata from keys and values, so the most urgent fix was the route through that layout after a collision.

We kept scalar perturbed probing for that route. String equality got a separate improvement: cached unequal hashes now reject a match immediately, and compatible UTF-16 arrays go through native `memcmp`. That gives the platform's optimized vector comparison a chance to do the expensive byte work. The implementation lives in [the native equality path](https://github.com/codenameone/CodenameOne/blob/963764b5e7/vm/ByteCodeTranslator/src/nativeMethods.m).

Swiss group probing and our string comparisons use metadata and contiguous storage at different stages of lookup. Looking at Go helped identify where we were already compact and where we were still wasting work.

## Gains, and the cost we accepted

We alternated old and new builds on the development Mac, checked that their answers matched, and compared the best runs:

| Workload | Before | After |
| --- | --- | --- |
| Miss-heavy | 32,698 ms | 44.9 ms |
| Tombstone-heavy | 7,781 ms | 16.5 ms |
| String keys | 33.6 ms | 25.5 ms |
| Large table, random hits | 26.6 ms | 33.3 ms |
| Hashtable build | 167.8 ms | 96.6 ms |
| IdentityHashMap lookup | 13.4 ms | 5.0 ms |

The miss-heavy workload fell from 32.7 seconds to 44.9 ms. Random hits in the large table got slower, and the broad suite's geometric mean barely moved. We accepted that tradeoff to remove the pathological misses and tombstone walks.

Hashtable now avoids an `Entry` allocation per mapping and uses the compact layout. Its lookup remains more expensive than HashMap with the same probe code because synchronization goes through ParparVM's address-keyed monitor table. The next useful optimization belongs there.

IdentityHashMap supplied a different warning. HotSpot's identity hash is already scrambled; ParparVM's is derived from an aligned address. Copying the JDK's indexing expression preserved zero low bits and worsened collisions. Folding the high bits down worked better on the measured allocator distribution.

## The map is only part of the allocation bill

Fixing the search still leaves the keys and values. A JSON parser may allocate a wrapper for every number it inserts. The application sees a map of values; the collector sees both the container and a stream of small objects that disappear with it.

ParparVM already avoided a separate allocation for `Integer.valueOf`. [PR #5735](https://github.com/codenameone/CodenameOne/pull/5735) extends tagged immediates to Short, Character, Float, Long, and Double. Eligible values fit in the reference-sized word that would otherwise point to a wrapper object.

## The spare bits were already part of the contract

On this 64-bit representation, aligned object addresses leave three low bits available. ParparVM's conservative root scan already rejects words with those alignment bits set. The encoding uses them as a type tag, with the remaining 61 bits carrying the payload.

We checked the allocator alignment with `TagProbe` across more than 380,000 allocations. Those spare bits were already part of the runtime's reference-scanning contract; the new encoding puts them to work.

{{< mermaid >}}
flowchart LR
    V[Boxed primitive value] --> F{Fits its tagged encoding?}
    F -->|Yes| W[Type tag and payload in one word]
    F -->|No| H[Ordinary heap wrapper]
    W --> D[Dispatch according to the type tag]
    H --> D
    D --> J[Java wrapper behavior]
{{< /mermaid >}}

A tagged value fits wherever the reference word fits: a local variable, a register, an array element, or a map slot. The separate wrapper object disappears.

## Which values fit?

| Wrapper | Representation covered by this work |
| --- | --- |
| Integer | Existing tagged path |
| Short | Full value range |
| Character | Full value range |
| Float | Full bit-pattern payload fits |
| Long | Values from `-2^60`, inclusive, to `2^60`, exclusive |
| Double | Values whose low three mantissa bits are clear |

Long and Double values outside those conditions use the existing heap path. Byte and Boolean already have bounded caches; they do not need another immediate code to avoid an unbounded allocation stream.

```java
Long small = Long.valueOf(42L);              // Tagged on ParparVM
Long large = Long.valueOf(Long.MAX_VALUE);  // Heap fallback
Double exact = Double.valueOf(12.5);        // Tagged
Double fraction = Double.valueOf(0.1);      // Heap fallback
```

Application code still uses `equals()` for wrapper value equality. The runtime chooses the representation behind those ordinary Java calls.

## The useful benchmark reports coverage

An early workload used round quarters and reported 100% coverage for Long and Double. That made a partial representation look universal. The revised mixture reported 78% coverage for Long and 56% for Double.

The PR records best-of-six interleaved comparisons against the Integer-only build, with matching checksums across the ablation arms:

| Workload | Reported speedup |
| --- | --- |
| Long-key map | 2.30x |
| Character boxing | 1.54x |
| Double-list reduction | 1.43x |
| Mixed boxed churn | 1.28x |
| JSON-like parsing | 1.14x |

The whole `Bench` suite remained at 1.00. The gains were concentrated where boxed values had been creating work.

An allocation census checks the mechanism independently of elapsed time. The JSON-like workload fell from **24.02 boxed allocations per map to 5.24**. Its expected remaining Double allocations were about 5.28 per map at the measured coverage. The allocation count lined up with the encoding: the remaining Double values explained almost all of the surviving boxes.

## A type tag is also a dispatch obligation

The existing inline `hashCode` and `equals` fast path was Integer-specific. Changing its condition from "is an Integer tag" to "is any tag" would silently use the wrong hash contract for another wrapper. It might still run quickly and never crash.

The other wrapper types therefore reach their own implementations through the dispatch table. `BoxEdge` exercises the edge cases; deliberately widening that Integer guard made 115 lines of output diverge. The new test also found pre-existing issues in `Short.equals(null)` and Float formatting, including NaN and negative zero.

The nursery write barrier needed a tag guard too. Every place that handles a value as an object must distinguish an immediate from the address of a heap object. Saving an allocation is useful only if reference scanning and dispatch agree about what occupies the slot.

## Our poor man's Valhalla gets a little richer

[Project Valhalla](https://github.com/openjdk/valhalla-docs/blob/main/site/design-notes/state-of-valhalla/02-object-model.md) tackles the cost of giving values an object identity they do not need. That opens the door to storing values together and passing their contents directly, instead of requiring another object and pointer for each one.

Our [earlier tagged-Integer work](/blog/beating-hotspot-performance/) took a smaller route through the same problem. We already knew the wrapper types and controlled their representation, so we could put an Integer where its pointer would have been. Now more numeric wrappers get that treatment.

The 61-bit payload is the limit of this particular trick. Long and Double retain a heap fallback; mutable objects such as `Dimension` and `Rectangle` keep their existing representation. The payoff is immediate for maps and JSON data full of numbers: ordinary Java APIs, fewer separate objects.

## Measure the contents as well as the container

A cache asks whether an item is missing. A decoder builds a map and boxes its numbers. A registry deletes and replaces entries. Measuring one successful lookup tells us very little about those other jobs.

The map fixes and wider tagged values address separate costs in the same Java data structures. The probe sequence changes where we search; the encoding changes what occupies a key or value slot. The allocation census shows what carries through to the collector: fewer objects to trace and reclaim.

That is a useful result for the {{< post-link path="/blog/performance-work-between-benchmarks" text="week's performance work" >}}. App developers keep ordinary maps and wrapper APIs. Codename One handles probing, dispatch, and reference scanning together, including the edge cases that must remain correct when a reference no longer points to an object. That lets the application keep its data model while we improve the search and allocation work underneath it.

---

## Discussion

_Does your map benchmark measure missing keys and boxed allocations, or only successful lookups?_

{{< giscus >}}
