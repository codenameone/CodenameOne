---
title: "What Go Taught Us About Java Garbage Collection"
slug: parparvm-gc-small-heaps
url: /blog/parparvm-gc-small-heaps/
date: '2026-09-12'
author: Shai Almog
description: "A Go performance comparison led us from stack allocation to GC pacing, parallel marking, and image caches. ParparVM can explore those choices while keeping ordinary Java APIs."
feed_html: '<img src="https://www.codenameone.com/blog/parparvm-gc-small-heaps.jpg" alt="What Go Taught Us" /> A Go performance comparison led us from stack allocation to GC pacing, parallel marking, and image caches. ParparVM can explore those choices while keeping ordinary Java APIs.'
series: ["release-2026-09-11"]
---

![What Go Taught Us](/blog/parparvm-gc-small-heaps.jpg)

As Java developers, we take a lot of inspiration from HotSpot. It is the gold standard for many of us, and for good reason. But measuring ParparVM against Go recently sent me down a different path.

My first thought was stack allocation. If Go was keeping more objects off the heap, that would explain a lot: fewer allocations, fewer pointers to follow, less work for the collector. It is the same attraction that makes [Valhalla's value objects](https://github.com/openjdk/valhalla-docs/blob/main/site/design-notes/state-of-valhalla/02-object-model.md) so interesting to Java developers. Why pay for a separate object when what you need is a value?

Looking deeper, allocation wasn't the whole explanation. Go's runtime makes different choices about when to collect and how to share the work. Could we benefit from some of those choices too?

ParparVM is a good place to try. We translate Java to C and compile ahead of time. Our closed-world build sees the application before it runs, and we control the object layout, reference handling, and collector together. In that respect, our constraints are closer to Go's native runtime than to HotSpot's dynamic execution model.

That investigation became [this week's GC work](https://github.com/codenameone/CodenameOne/pull/5717). It started with the memory we allowed garbage to occupy and ended up reaching the way an image cache decides what to keep.

## The memory we were giving away

A collector needs room to let the application allocate between collections. More room can mean fewer collections, which saves CPU time. But a larger allowance only makes sense if the workload benefits from it.

Our minimum allocation threshold was 24 MB. In the test, the process reached 98 MB of resident memory while holding very little live data. We lowered the threshold and watched memory, throughput, and latency together.

Throughput and p99 barely moved beyond the noise between runs. Resident memory did. After tuning the lower-floor configuration, we went from **98 MB to 38 MB**. Those are process RSS measurements, including memory outside the Java heap.

![Reported process RSS by configured trigger](/blog/gc-trigger-rss.svg)

*Resident memory at each GC trigger setting in the experiment. The final tuned configuration reached 38 MB.*

## The smarter formula had the wrong input

The obvious next step was to make the threshold follow the live set. A small live set would get a small allowance; a larger one would get more room. We tried three versions of that idea and ran into the same underlying problem: our counters weren't counting what the policy needed. The investigation is recorded beside [the collector implementation](https://github.com/codenameone/CodenameOne/blob/e4dc53f55b/vm/ByteCodeTranslator/src/cn1_globals.m).

An ordinary sweep samples retired pages. It does not count all the live objects on partial pages, and mutator-owned current pages are not swept. Large objects use another heap path. Even the major sweep's policy counters omit part of that population. The number available to the policy therefore cannot safely stand in for the whole live set.

One attempt held on to an old live-set estimate after the objects were gone. The resulting high threshold suppressed the very sweeps needed to return pages to the operating system.

We made the floor configurable in the runtime instead:

```c
#ifndef CN1_BIBOP_GC_MIN_TRIGGER_BYTES
#define CN1_BIBOP_GC_MIN_TRIGGER_BYTES CN1_BIBOP_GC_TRIGGER_BYTES
#endif
```

A runtime build can now choose the smaller floor. The default remains 24 MB while we work through more workloads. The experiment had already shown that a smaller allowance could release memory without giving up speed.

{{< mermaid >}}
flowchart LR
    A[Allocation pressure] --> P[Collection pacing]
    F[Deployment-selected floor] --> P
    P --> M[Mark reachable objects]
    M --> S[Sweep eligible pages]
    S --> R[Reuse slots or return pages]
    S --> C[Partial policy counters]
    C --> X[Insufficient for a complete live-set estimate]
{{< /mermaid >}}

## Put waiting threads to work

Reducing the garbage allowance also brought us back to the marking work itself. Our `GcPause` loop allocates 20 million short-lived objects while keeping a 4,096-node live set. Median and p99 were comparable to Go, but the long stalls were where we had the most work to do:

| Configuration | Reported worst pause |
| --- | --- |
| ParparVM, one marker | 2.2 to 3.3 seconds |
| ParparVM, four markers | 0.3 to 0.9 seconds |
| Go, same loop | 20 milliseconds |

Four markers cut those stalls substantially. Go still finished its worst pause in about 20 ms, giving us a clear target for the next round.

We also added mutator assistance. When an allocating thread reaches the limit on how far it can run ahead of collection, it can mark a batch of objects instead of waiting. The thread registers as active before releasing the worklist lock, so the collector cannot finish while that batch is still in flight.

An old arm64 corruption report had kept us on the serial default. Since then, we have fixed barriers, object validation, and root handling. A dedicated workflow now exercises one and four markers on arm64 and four on x64. Parallel marking and assistance remain on that experimental path as we work through those runs.

## A Java long became a C int

The same investigation found that `1L << n` could become a 32-bit C shift. The shift count was masked for a Java long, but the translator emitted the constant as a bare C integer literal. A long variable worked, which helped the defect survive.

```java
long first = 1L << 31;  // 2147483648
long second = 1L << 32; // 4294967296
long third = 1L << 33;  // 8589934592
```

The fix casts the left operand appropriately. `LongShift` checks the shift variants against a reference built by repeated doubling. The arithmetic had to be right before its timing meant anything.

## The image you will need again in a second

Imagine scrolling through a gallery and then back up to the image you just passed. If the cache kept it, the next frame can reuse it. If a memory warning emptied the cache, that same gesture sends the application back through decoding.

Collecting sooner is only half the job. We also need to keep useful work around without keeping every image forever.

[PR #5732](https://github.com/codenameone/CodenameOne/pull/5732) gives the collector real weak and soft references, including a policy that favors recently accessed data. The collector can now make that choice instead of treating the entire cache as permanent storage.

## The old low-memory workaround

ParparVM's `WeakReference` used to hold its referent in an ordinary object field. The translator emitted the same mark operation as for any strong reference. As long as the wrapper remained reachable, its supposedly weak referent remained reachable too. There was no SoftReference implementation to provide a separate policy.

The iOS port worked around missing soft-reference behavior with a strong-reference table. A memory warning called `flushSoftRefMap()`, which replaced the table. That low-memory signal approximated "the VM may discard this cache," but only as a bulk operation.

Image decoding, scaled images, RGB copies, rounded borders, and rasterized gradients all have reasons to cache disposable results. Their desired contract is simple: keep the result if useful, and return `null` when it must be reconstructed. A port-wide table held until a warning cannot express that contract precisely.

## A weak field must stop being an ordinary edge

The referent now lives in `Reference`. The translator recognizes that field and emits reference discovery instead of tracing it as an unconditional strong edge. The collector decides whether to retain a soft referent and later clears references whose objects are eligible for reclamation.

| Reference kind | What it means for the referent |
| --- | --- |
| Strong | Keeps the object reachable |
| Weak | Allows reclamation when no stronger reachability retains it |
| Soft | Allows policy-based retention of otherwise disposable data |

A small image cache can now use the Java API directly:

```java
import java.lang.ref.SoftReference;
import com.codename1.ui.Image;
import java.io.IOException;

final class PreviewCache {
    private SoftReference<Image> cached;

    Image get() throws IOException {
        Image result = cached == null ? null : cached.get();
        if (result == null) {
            result = Image.createImage("/preview.png");
            cached = new SoftReference<Image>(result);
        }
        return result;
    }
}
```

The local variable keeps a successful result strongly reachable while the caller uses it. A cleared reference sends us back through image creation. This example assumes one calling thread; a shared cache needs synchronization around its own state.

## Reading during collection is the dangerous case

Suppose the collector scans a thread and moves on. That thread then calls `get()` and keeps the referent in a local. If the collector clears the reference and frees the object without noticing the read, Java code receives a dangling native pointer.

The fix couples reference reads to the snapshot-at-the-beginning barrier machinery. A relevant read is logged while marking is active. Reference processing remains inside the termination loop, with the barrier armed, so new work can force another pass before sweeping.

{{< mermaid >}}
sequenceDiagram
    participant App as Application thread
    participant Ref as Reference
    participant GC as Collector
    GC->>App: Scan roots
    App->>Ref: get()
    Ref->>GC: Log referent if marking still needs it
    Ref->>App: Return referent
    GC->>GC: Drain new work before termination
    GC->>Ref: Clear only sweep-eligible referents
    GC->>GC: Sweep
{{< /mermaid >}}

Logging every reference read made the collector repeatedly reopen marking. The filter excludes objects already marked in the current epoch or still fresh, matching the conditions that would prevent clearing them. After filtering those reads, termination passes fell from 32 to one.

## Recency helps keep the cache useful

Each successful `get()` refreshes the reference's rank. The collector uses the age of that access when choosing what to retain, so the image you keep returning to tends to outlast the one you passed once.

The [RefPolicy benchmark](https://github.com/codenameone/CodenameOne/blob/b84362c66d/vm/benchmarks/src/com/bench/RefPolicy.java) ran five interleaved repetitions with simulated process-memory ceilings and matching checksums:

| Ceiling | Policy | Cache hit rate | Footprint |
| --- | --- | --- | --- |
| 128 MB | Clear on pressure | 84.99% | 63.1 MB |
| 128 MB | Ranked retention | 96.77% | 62.6 MB |
| 160 MB | Clear on pressure | 87.99% | 91.0 MB |
| 160 MB | Ranked retention | 97.44% | 82.1 MB |

![Cache hit rate and footprint under the simulated 160 MB ceiling](/blog/soft-reference-policy.svg)

*RefPolicy results with a simulated 160 MB memory ceiling.*

The weak-reference check cleared 255 of 256 probe referents, where the old implementation cleared none. Ranked retention also beat the bulk flush: the cache kept more of the useful results while occupying less memory. Comparing recency with other eviction policies is a good next experiment.

## Back to the screen

The next step is moving framework caches onto these references. The iOS `softReferenceMap` override and existing cache call sites are still in place; we now have the collector support to replace that workaround. We can measure the change in image decoding and scrolling as well as in memory.

I started this investigation wondering whether Go's allocation choices explained its performance. We found work on both sides of an allocation: how to avoid creating the object, and how to handle it once it exists. Tomorrow's {{< post-link path="/blog/hashmap-misses-probe-sequence" text="map and boxing story" >}} returns to the first question and our “poor man's Valhalla” work.

An AOT runtime gives us room to pursue both ideas together. We can change an object's representation and how the collector handles it, then expose the same Java API. The app still gets a safe reference from `get()` while collection runs. Keeping that promise inside ParparVM lets application developers concentrate on what they want to keep on screen.

---

## Discussion

_Which costs more in your image-heavy screen: retaining decoded images or reconstructing them after a cache flush?_

{{< giscus >}}
