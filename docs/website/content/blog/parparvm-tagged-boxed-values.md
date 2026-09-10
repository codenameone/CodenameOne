---
title: "Three Pointer Bits Make Boxed Numbers Cheaper"
slug: parparvm-tagged-boxed-values
url: /blog/parparvm-tagged-boxed-values/
date: '2026-09-13'
author: Shai Almog
description: "ParparVM extends tagged immediates beyond Integer. Short, Character, and Float fit directly, while Long and Double retain heap fallbacks for values outside the encoding."
feed_html: '<img src="https://www.codenameone.com/blog/parparvm-tagged-boxed-values.jpg" alt="A Number Without A Box" /> ParparVM extends tagged immediates beyond Integer. Short, Character, and Float fit directly, while Long and Double retain heap fallbacks for values outside the encoding.'
series: ["release-2026-09-11"]
---

![A Number Without A Box](/blog/parparvm-tagged-boxed-values.jpg)

A JSON parser can allocate an object for every number it reads. The application sees a map of values. The collector sees a stream of tiny objects that often disappear with the response.

ParparVM already avoided that allocation for `Integer.valueOf`. [PR #5735](https://github.com/codenameone/CodenameOne/pull/5735) extends tagged immediates to Short, Character, Float, Long, and Double. A reference-sized word can carry an eligible value directly instead of pointing to a separate heap object.

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

That narrow scope is still valuable. Application code keeps its maps and JSON structures. The runtime removes a recurring allocation cost where it can, and the {{< post-link path="/blog/parparvm-ranked-soft-references" text="new reference machinery" >}} gives the collector more control over disposable objects that do remain. Both are part of this week's effort to reduce memory work without asking application developers to abandon ordinary Java data structures.

---

## Discussion

_Does your allocation profile distinguish boxed numbers from the collections that contain them?_

{{< giscus >}}
