---
title: "Our HashMap Was Fast Until the Key Was Missing"
slug: hashmap-misses-probe-sequence
url: /blog/hashmap-misses-probe-sequence/
date: '2026-09-12'
author: Shai Almog
description: "A hit-only benchmark hid pathological misses in ParparVM HashMap. Perturbed probing preserves dense-key locality while reducing the reported miss-heavy workload from 32.7 seconds to 44.9 milliseconds."
feed_html: '<img src="https://www.codenameone.com/blog/hashmap-misses-probe-sequence.jpg" alt="The Key Was Missing" /> A hit-only benchmark hid pathological misses in ParparVM HashMap. Perturbed probing preserves dense-key locality while reducing the reported miss-heavy workload from 32.7 seconds to 44.9 milliseconds.'
series: ["release-2026-09-11"]
---

![The Key Was Missing](/blog/hashmap-misses-probe-sequence.jpg)

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

## Test the question the application actually asks

A cache asks whether an item is missing. A decoder builds a map. A registry deletes and replaces entries. Those deserve separate measurements even if the container class is the same.

This is why the {{< post-link path="/blog/performance-work-between-benchmarks" text="weekly work" >}} includes new benchmarks alongside the fixes. We can improve the shared collection implementation once, preserve its Java behavior, and let applications benefit without replacing their maps with native code. First we have to ask the map the uncomfortable questions.

---

## Discussion

_What fraction of your production map lookups miss, and does your benchmark use that fraction?_

{{< giscus >}}
