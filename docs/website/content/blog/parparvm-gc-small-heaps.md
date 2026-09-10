---
title: "A Small Heap Does Not Need a Large Garbage Budget"
slug: parparvm-gc-small-heaps
url: /blog/parparvm-gc-small-heaps/
date: '2026-09-12'
author: Shai Almog
description: "ParparVM experiments with Go-inspired GC pacing, measures a lower memory floor, and revisits parallel marking. The merged implementation keeps deployment control over the floor."
feed_html: '<img src="https://www.codenameone.com/blog/parparvm-gc-small-heaps.jpg" alt="Less Room For Garbage" /> ParparVM experiments with Go-inspired GC pacing, measures a lower memory floor, and revisits parallel marking. The merged implementation keeps deployment control over the floor.'
series: ["release-2026-09-11"]
---

![Less Room For Garbage](/blog/parparvm-gc-small-heaps.jpg)

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

## Memory after the burst

A lower, steadier memory profile needs several changes working together. Tagged boxes avoid allocations. Real weak references permit reclamation. Soft references let useful cached data survive without turning every cache entry into a permanent root. The {{< post-link path="/blog/parparvm-ranked-soft-references" text="cache follow-up" >}} covers that part.

A collector cannot flatten the memory required by a growing live data set. It can stop letting avoidable garbage dominate a small process, and it can avoid making the application wait unnecessarily for collection. That is the work we are measuring next: after the burst as well as during it.

---

## Discussion

_Do you track memory after a workload subsides, or only its peak under load?_

{{< giscus >}}
