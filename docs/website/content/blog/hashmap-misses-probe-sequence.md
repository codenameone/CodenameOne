---
title: "Fixing the Map, Then the Objects Inside It"
slug: hashmap-misses-probe-sequence
url: /blog/hashmap-misses-probe-sequence/
date: '2026-09-13'
author: Shai Almog
description: "ParparVM fixes pathological map misses and extends tagged boxed values. Probe counts, allocation coverage, and regressions show where ordinary Java collections became cheaper."
feed_html: '<img src="https://www.codenameone.com/blog/hashmap-misses-probe-sequence.jpg" alt="Faster Maps Fewer Boxes" /> ParparVM fixes pathological map misses and extends tagged boxed values. Probe counts, allocation coverage, and regressions show where ordinary Java collections became cheaper.'
series: ["release-2026-09-11"]
---

![Faster Maps Fewer Boxes](/blog/hashmap-misses-probe-sequence.jpg)

Three million `containsKey` calls took 32.7 seconds. The map benchmark we had been watching still looked healthy. It mostly asked for keys that existed.

[PR #5722](https://github.com/codenameone/CodenameOne/pull/5722) fixes that blind spot and improves Hashtable and IdentityHashMap along the way. The lesson applies to anyone benchmarking a hash table: successful lookup and unsuccessful lookup are different workloads.

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

This illustrates the two operations, not the complete measured workload. The committed [MapBench](https://github.com/codenameone/CodenameOne/blob/963764b5e7/vm/benchmarks/src/com/bench/MapBench.java) controls the key distribution and includes misses, tombstones, growth, string keys, and identity keys.

| Map entries | Mean probes per miss before | After |
| --- | --- | --- |
| 20,000 | 2,547 | 1.39 |
| 100,000 | 16,742 | 1.53 |
| 1,000,000 | 222,721 | 1.98 |

Hits stayed at one probe in this experiment. A checksum verifies the answer; it does not tell you that getting the answer took a linear walk.

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

## What the Swiss-table comparison does and does not say

Go's [Swiss-table explanation](https://go.dev/blog/swisstable) describes compact groups with control metadata used to filter candidate entries. It is a useful reference when a table spends time chasing pointers or comparing keys unnecessarily.

ParparVM already has compact arrays and metadata separated from key/value storage. That structural overlap makes the comparison interesting. It does not make the implementations identical, and this PR does not publish an equivalent-workload ParparVM-versus-Go result.

The merged lookup loop uses scalar perturbed probes. There is SIMD-related work on the string-key path: cached unequal hashes can reject equality immediately, and compatible UTF-16 backing arrays use native `memcmp`, which can use the platform's optimized vector comparison. That comparison path is visible in [nativeMethods.m](https://github.com/codenameone/CodenameOne/blob/963764b5e7/vm/ByteCodeTranslator/src/nativeMethods.m). It is distinct from checking a group of table control bytes at once. A claim that we shipped Swiss-style SIMD probing here would describe code we did not merge.

## Gains, and the cost we accepted

These are the PR's interleaved best-of-N measurements on the development Mac, with matching checksums. They compare before and after this change, not whole applications or Go maps.

| Workload | Before | After |
| --- | --- | --- |
| Miss-heavy | 32,698 ms | 44.9 ms |
| Tombstone-heavy | 7,781 ms | 16.5 ms |
| String keys | 33.6 ms | 25.5 ms |
| Large table, random hits | 26.6 ms | 33.3 ms |
| Hashtable build | 167.8 ms | 96.6 ms |
| IdentityHashMap lookup | 13.4 ms | 5.0 ms |

The large-table result regressed. The broad benchmark geometric mean barely moved. The large gains remove a pathological case that the old suite did not price; they are not a 728-fold improvement to every map operation.

Hashtable now avoids an `Entry` allocation per mapping and uses the compact layout. Its lookup remains more expensive than HashMap with the same probe code because synchronization goes through ParparVM's address-keyed monitor table. The next useful optimization belongs there.

IdentityHashMap supplied a different warning. HotSpot's identity hash is already scrambled; ParparVM's is derived from an aligned address. Copying the JDK's indexing expression preserved zero low bits and worsened collisions. Folding the high bits down worked better on the measured allocator distribution.

## The map is only part of the allocation bill

Fixing the search still leaves the keys and values. A JSON parser may allocate a wrapper for every number it inserts. The application sees a map of values; the collector sees both the container and a stream of small objects that disappear with it.

ParparVM already avoided a separate allocation for `Integer.valueOf`. [PR #5735](https://github.com/codenameone/CodenameOne/pull/5735) extends tagged immediates to Short, Character, Float, Long, and Double. Eligible values fit in the reference-sized word that would otherwise point to a wrapper object.

## The spare bits were already part of the contract

On this 64-bit representation, aligned object addresses leave three low bits available. ParparVM's conservative root scan already rejects words with those alignment bits set. The encoding uses them as a type tag, with the remaining 61 bits carrying the payload.

That alignment assumption is verified by `TagProbe` across more than 380,000 allocations in the implementation work. It is a property of this runtime and its allocator, not a portable trick Java application code should perform on references.

{{< mermaid >}}
flowchart LR
    V[Boxed primitive value] --> F{Fits its tagged encoding?}
    F -->|Yes| W[Type tag and payload in one word]
    F -->|No| H[Ordinary heap wrapper]
    W --> D[Dispatch according to the type tag]
    H --> D
    D --> J[Java wrapper behavior]
{{< /mermaid >}}

A tagged value may reside in a local variable, register, array, or map slot. There is no separate wrapper allocation. A value stored inside a heap collection is not a stack-allocated object.

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

These comments describe the ParparVM representation, not a Java language guarantee. Use `equals()` for wrapper value equality. Code should not infer object identity or lifetime from whether an allocation happened.

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

The whole `Bench` suite remained at 1.00. These are targeted workload results, not an application-wide speed multiplier.

An allocation census checks the mechanism independently of elapsed time. The JSON-like workload fell from **24.02 boxed allocations per map to 5.24**. Its expected remaining Double allocations were about 5.28 per map at the measured coverage. The agreement matters more than a convenient headline: allocations fell by roughly the amount the encoding predicts.

## A type tag is also a dispatch obligation

The existing inline `hashCode` and `equals` fast path was Integer-specific. Changing its condition from "is an Integer tag" to "is any tag" would silently use the wrong hash contract for another wrapper. It might still run quickly and never crash.

The other wrapper types therefore reach their own implementations through the dispatch table. `BoxEdge` exercises the edge cases; deliberately widening that Integer guard made 115 lines of output diverge. The new test also found pre-existing issues in `Short.equals(null)` and Float formatting, including NaN and negative zero.

The nursery write barrier needed a tag guard too. Every place that handles a value as an object must distinguish an immediate from the address of a heap object. Saving an allocation is useful only if reference scanning and dispatch agree about what occupies the slot.

## The useful part of the Valhalla analogy

Our [earlier runtime article](/blog/beating-hotspot-performance/) and [SIMD and allocation discussion](/blog/ios-density-scroll-and-accessibility/) explored ways to remove object overhead without changing application algorithms. "Poor man's Valhalla" captures that motivation.

The boundary is substantial. This is not a general implementation of value classes. Mutable objects such as `Dimension` and `Rectangle` do not become immediate values, and arbitrary objects do not gain automatic stack allocation. The optimization covers specific wrapper representations with a defined fallback.


## Measure the contents as well as the container

A cache asks whether an item is missing. A decoder builds a map and boxes its numbers. A registry deletes and replaces entries. Measuring one successful lookup tells us very little about those other jobs.

The map fixes and wider tagged values address separate costs in the same Java data structures. The probe sequence changes where we search; the encoding changes what occupies a key or value slot. Their benchmark ratios cannot be multiplied into a single speedup, but the allocation census gives the collector a concrete benefit: fewer objects to trace and reclaim.

That is a useful result for the {{< post-link path="/blog/performance-work-between-benchmarks" text="week's performance work" >}}. App developers keep ordinary maps and wrapper APIs. Codename One handles probing, dispatch, and reference scanning together, including the edge cases that must remain correct when a reference no longer points to an object. A faster map is worth shipping only when a missing key, an unusual Double, and a collector scan still get the right answer.

---

## Discussion

_Does your map benchmark measure missing keys and boxed allocations, or only successful lookups?_

{{< giscus >}}
