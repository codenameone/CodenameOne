---
title: "The Collector and the Cache Have to Agree"
slug: parparvm-gc-small-heaps
url: /blog/parparvm-gc-small-heaps/
date: '2026-09-12'
author: Shai Almog
description: "ParparVM lowers memory in a small-backend GC experiment and adds real weak and soft references. Go-inspired pacing and ranked retention address garbage and useful cached data together."
feed_html: '<img src="https://www.codenameone.com/blog/parparvm-gc-small-heaps.jpg" alt="Keep The Cache Lose The Garbage" /> ParparVM lowers memory in a small-backend GC experiment and adds real weak and soft references. Go-inspired pacing and ranked retention address garbage and useful cached data together.'
series: ["release-2026-09-11"]
---

![Keep The Cache Lose The Garbage](/blog/parparvm-gc-small-heaps.jpg)

A backend holding very little live data still reached 98 MB of resident memory. Its collector waited against a 24 MB allocation floor, and lowering that floor did not measurably hurt throughput in the trigger sweep. We were spending memory without buying speed.

[PR #5717](https://github.com/codenameone/CodenameOne/pull/5717) investigates that behavior in ParparVM, the Java-to-C runtime used by Codename One's native ports. It also fixes long-shift code generation and reopens a parallel-marking experiment. This is the collector part of our {{< post-link path="/blog/performance-work-between-benchmarks" text="weekly performance work" >}}.

## Learn from the workload a collector targets

HotSpot sets a demanding standard, but "HotSpot GC" covers different policies. Oracle describes G1 as targeting multiprocessor machines with large memory, while its collector-selection guidance also identifies Serial GC for small data sets. Our problem is the memory budget of a small native process, so copying a large-heap policy without its assumptions would be a poor starting point. [G1 design target](https://docs.oracle.com/en/java/javase/25/gctuning/garbage-first-g1-garbage-collector1.html), [collector selection](https://docs.oracle.com/en/java/javase/17/gctuning/available-collectors.html).

Go offers a useful comparison because its documentation makes the CPU-versus-memory trade explicit. Collecting more frequently reduces space available for new garbage, but repeats collection work more often. Its heap target accounts for live data and roots. That is the idea we explored, not a claim that copying one setting copies Go's collector. [Go GC guide](https://go.dev/doc/gc-guide).

## The experiment, and the version that actually merged

The `/plaintext` backend test used 64 connections. Reported loaded RSS across the trigger sweep was:

| Trigger | Loaded RSS |
| --- | --- |
| 4 MB | 30 MB |
| 8 MB | 49 MB |
| 16 MB | 68 MB |
| 24 MB | 98 MB |
| 48 MB | 102 MB |

Throughput and p99 stayed within run-to-run noise. A subsequent lower-floor configuration reported 38 MB loaded RSS, down from 98 MB. RSS includes resident process memory; it is not a live-object count or a heap-only measurement.

![Reported backend RSS by configured trigger](/blog/gc-trigger-rss.svg)

*Separate trigger configurations from PR #5717. This chart does not show a time series or compare against Go.*

The PR's opening description says the new floor is proportional to the live set. The [merged implementation](https://github.com/codenameone/CodenameOne/blob/e4dc53f55b/vm/ByteCodeTranslator/src/cn1_globals.m) records why that approach was abandoned.

An ordinary sweep samples retired pages. It does not count all the live objects on partial pages, and mutator-owned current pages are not swept. Large objects use another heap path. Even the major sweep's policy counters omit part of that population. The number available to the policy therefore cannot safely stand in for the whole live set.

Three proportional-floor attempts failed in different ways. One kept a departed live set in its estimate long enough to suppress the sweeps that would return pages to the operating system. The merged solution makes the floor a deployment choice and preserves the stock default:

```c
#ifndef CN1_BIBOP_GC_MIN_TRIGGER_BYTES
#define CN1_BIBOP_GC_MIN_TRIGGER_BYTES CN1_BIBOP_GC_TRIGGER_BYTES
#endif
```

That is an excerpt from the runtime, not an application build hint. The backend can select a lower floor because its workload is known. Shipping this change does not automatically switch every mobile application to a 4 MB floor.

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

## Parallel marking improves the tail, but Go still wins the worst pause

The `GcPause` loop allocates 20 million short-lived objects with one mutator and a 4,096-node live set. The PR reports median and p99 comparable to Go. Its worst pauses tell a different story:

| Configuration | Reported worst pause |
| --- | --- |
| ParparVM, one marker | 2.2 to 3.3 seconds |
| ParparVM, four markers | 0.3 to 0.9 seconds |
| Go, same loop | 20 milliseconds |

The four-marker result is useful progress, and it remains far from Go at the worst observed pause. These are results recorded during the PR, not a fresh cross-runtime benchmark for this article.

The work also adds mutator assistance: a thread that would wait at the run-ahead cap can mark a batch instead. It registers as active before releasing the worklist lock so termination cannot race past its work. Assistance applies to the parallel path; it is inactive under the unchanged serial default.

A dedicated workflow tests one marker on arm64, four on arm64, and four on x64. An old arm64 corruption report had frozen the default before later fixes to barriers, object validation, and root handling. Re-testing is warranted. Changing the default requires more than a clean run of one allocation loop.

## A Java long became a C int

The same investigation found that `1L << n` could become a 32-bit C shift. The shift count was masked for a Java long, but the translator emitted the constant as a bare C integer literal. A long variable worked, which helped the defect survive.

```java
long first = 1L << 31;  // 2147483648
long second = 1L << 32; // 4294967296
long third = 1L << 33;  // 8589934592
```

The fix casts the left operand appropriately. `LongShift` checks the shift variants against a reference built by repeated doubling. A collector benchmark that relies on incorrect arithmetic is not evidence of collector performance.

## Collect garbage without throwing away useful work

A lower collection floor addresses objects the application no longer needs. A cache raises a harder question: which objects are worth keeping? Decoding an image again on the next scroll costs time, but keeping every decoded image forever costs memory.

[PR #5732](https://github.com/codenameone/CodenameOne/pull/5732) gives the collector real weak and soft references, including a policy that favors recently accessed data. The pacing and reference work belong together: collecting sooner is only useful if the collector can distinguish disposable cached data from permanent roots.

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

Application code must still handle a cache miss. This example illustrates direct SoftReference use on the updated ParparVM path:

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

The local variable holds the successful result strongly while the caller uses it. A soft cache cannot promise a hit, and it is not suitable for data with no reconstruction path. This small example assumes access from one thread; a shared cache also needs its own synchronization policy.

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

Logging every reference read made the collector repeatedly reopen marking. The filter excludes objects already marked in the current epoch or still fresh, matching the conditions that would prevent clearing them. The PR reports termination passes dropping from 32 to one after that filtering.

## Recency helps keep the cache useful

The rank is the age since the last successful `get()`. A frequently read reference usually has a recent access and is therefore favored for retention. This is recency, not a permanent usage score or a guarantee that the most frequently used image can never be collected.

The [RefPolicy benchmark](https://github.com/codenameone/CodenameOne/blob/b84362c66d/vm/benchmarks/src/com/bench/RefPolicy.java) ran five interleaved repetitions with simulated process-memory ceilings and matching checksums:

| Ceiling | Policy | Cache hit rate | Footprint |
| --- | --- | --- | --- |
| 128 MB | Clear on pressure | 84.99% | 63.1 MB |
| 128 MB | Ranked retention | 96.77% | 62.6 MB |
| 160 MB | Clear on pressure | 87.99% | 91.0 MB |
| 160 MB | Ranked retention | 97.44% | 82.1 MB |

![Cache hit rate and footprint under the simulated 160 MB ceiling](/blog/soft-reference-policy.svg)

*Reported RefPolicy results from PR #5732. The process ceiling is simulated; this is not a device scrolling benchmark.*

Proper weak reclamation is the clearest result: 255 of 256 probe referents were cleared, versus zero under the old strong behavior. Ranked retention also beat the clear-on-pressure arm. It did not establish that recency beats every simpler retention policy; there was no matched-rate random-eviction control.

## The VM change and the image-cache migration are separate

This PR deliberately leaves the iOS `softReferenceMap` override and framework cache call sites in place. It supplies the collector semantics needed to migrate them. Claiming that every image cache already uses ranked references would skip that remaining integration work.

The separation is useful for diagnosis. A collector change can be tested independently of a cache-policy change that affects every iOS application. The next step is to move disposable caches onto the new contract and measure decoding, hit rate, and resident memory together.


## Memory after the burst

The lower-floor experiment reduced resident memory without a measurable throughput penalty in that workload. Ranked references kept more cache hits than a pressure-triggered flush while using less memory in the simulated-budget test. Together they give us a way to reduce garbage without making every return to a screen pay for its images again.

A collector cannot flatten the memory required by a growing live data set. We still need to migrate framework caches, measure real screens, and close the gap in worst pauses. The next article covers another part of that job: {{< post-link path="/blog/hashmap-misses-probe-sequence" text="maps and boxed values" >}} that leave fewer allocations for the collector in the first place.

These changes also put delicate reference handling where we can review it once for every application. A cache read during collection must not become a dangling pointer. Keeping that guarantee inside the runtime is part of making ordinary Java a safe default on native targets, even as we change the representation underneath it.

---

## Discussion

_Which costs more in your image-heavy screen: retaining decoded images or reconstructing them after a cache flush?_

{{< giscus >}}
