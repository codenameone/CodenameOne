---
title: "A Cache Should Forget Cold Images, Not Every Image"
slug: parparvm-ranked-soft-references
url: /blog/parparvm-ranked-soft-references/
date: '2026-09-13'
author: Shai Almog
description: "ParparVM adds real weak and soft references, with recency-based retention and barriers for concurrent reads. Benchmarks compare ranked retention with clearing caches under pressure."
feed_html: '<img src="https://www.codenameone.com/blog/parparvm-ranked-soft-references.jpg" alt="Keep The Useful Images" /> ParparVM adds real weak and soft references, with recency-based retention and barriers for concurrent reads. Benchmarks compare ranked retention with clearing caches under pressure.'
series: ["release-2026-09-11"]
---

![Keep The Useful Images](/blog/parparvm-ranked-soft-references.jpg)

A decoded image is worth keeping while the user scrolls back and forth. Keeping every image forever is a leak in the cache policy. Dropping all of them at once means the next scroll pays for decoding again.

ParparVM needed a collector that could make that distinction. [PR #5732](https://github.com/codenameone/CodenameOne/pull/5732) adds real weak and soft references, including a retention policy that favors recently accessed referents. The difficult part was making `get()` safe while collection proceeds concurrently.

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

Cheaper boxing and a suitable collection floor reduce the work around these caches. The articles on {{< post-link path="/blog/parparvm-tagged-boxed-values" text="tagged values" >}} and {{< post-link path="/blog/parparvm-gc-small-heaps" text="GC pacing" >}} cover those changes. We want to keep the screen responsive without retaining every intermediate result that ever helped draw it.

---

## Discussion

_Which costs more in your image-heavy screen: retaining decoded images or reconstructing them after a cache flush?_

{{< giscus >}}
