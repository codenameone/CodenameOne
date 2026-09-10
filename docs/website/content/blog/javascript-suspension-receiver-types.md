---
title: "One Blocking run() Made Unrelated JavaScript Methods Suspend"
slug: javascript-suspension-receiver-types
url: /blog/javascript-suspension-receiver-types/
date: '2026-09-14'
author: Shai Almog
description: "Codename One narrows JavaScript suspension analysis using receiver types. The sample emits fewer generators and yield sites, while a new throughput benchmark records both gains and an iterator regression."
feed_html: '<img src="https://www.codenameone.com/blog/javascript-suspension-receiver-types.jpg" alt="Does This Call Block?" /> Codename One narrows JavaScript suspension analysis using receiver types. The sample emits fewer generators and yield sites, while a new throughput benchmark records both gains and an iterator regression.'
series: ["release-2026-09-11"]
---

![Does This Call Block?](/blog/javascript-suspension-receiver-types.jpg)

One blocking `run()` method was enough to make unrelated `run()` calls suspension points. Their callers could then become suspending too. In the JavaScript backend, an imprecise answer spread through the call graph and turned ordinary methods into generators.

[PR #5755](https://github.com/codenameone/CodenameOne/pull/5755) gives the analysis the missing receiver-type information. It also adds a benchmark capable of measuring a change that barely affects bundle size.

## A signature does not identify an implementation

The backend translates a Java method that can block into a JavaScript generator. Calls that can suspend need `yield*` so the runtime can resume them later. That is necessary for blocking behavior, but unnecessary generator dispatch adds work to synchronous paths.

The old analysis grouped methods by name and descriptor without the owner class. This small Java example shows the distinction it lost:

```java
final class WaitingTask {
    void run() throws InterruptedException {
        Thread.sleep(10);
    }
}

final class CounterTask {
    private int count;
    void run() {
        count++;
    }
}
```

Both methods have the same name and argument/return descriptor. Knowing that the receiver is a `CounterTask` rules out the blocking implementation. Real code adds interfaces, subclasses, and native bridges, so the compiler must conservatively include every implementation that can actually receive the call.

## Reuse the type information already computed

`JavascriptReachability` already computed possible receiver types, but kept that information private. The new dispatch model exposes it to suspension analysis and code generation.

The analysis follows the receiver's possible implementations instead of every method in the program with the same signature. It also narrows JavaScript-object protection and distinguishes bridge tokens used to replace a method from tokens merely used to look one up.

{{< mermaid >}}
flowchart TD
    C[Virtual call site] --> R[Possible receiver types]
    R --> I[Resolve reachable implementations]
    I --> Q{Any implementation may block?}
    Q -->|Yes| G[Generator call path]
    Q -->|No| S[Synchronous call path]
    I --> U{Resolution incomplete?}
    U -->|Yes| F[Keep conservative fallback]
{{< /mermaid >}}

An unresolved receiver cannot simply be skipped. The runtime can search interfaces and fall back to a native table, so static analysis must account for those possibilities too. Both analysis and emitter use the same dispatch model: emitting `yield*` into a plain JavaScript function is a syntax error.

## Why counting bytes was the wrong measure

In `hellocodenameone`, the PR reports:

| Generated artifact metric | Before | After |
| --- | --- | --- |
| `yield*` sites | 54,549 | 40,741 |
| Generators | 13,068 | 11,044 |
| Suspending virtual dispatch sites | 28,569 | 19,073 |
| Synchronous methods | 8,456 | 9,875 |
| Translated bundle | 8,089,807 bytes | 7,993,916 bytes |

A yield site is cheap to spell. Its runtime dispatch cost does not show up proportionally in source size. The 25.3% reduction in yield sites produced only a 1.2% bundle reduction.

Before this work, screenshots could verify rendering and the lifecycle harness could verify milestones. Neither could put an elapsed-time cost on unnecessary generator dispatch. The new [JavaScript throughput benchmark](https://github.com/codenameone/CodenameOne/blob/0204081e19/scripts/run-javascript-throughput-benchmark.sh) translates Java workloads and runs them under Node:

```bash
./scripts/run-javascript-throughput-benchmark.sh --help
```

Use the script's options to build comparable arms and retain its checksums. Each workload runs in its own process. The comparison refuses changed checksums and counts unexpected generator stepping through the synchronous dispatcher.

## The timing results include a regression

The PR reports interleaved best-of-three measurements against the previous revision. A master-versus-master comparison measured the noise floor at 0.3% to 4.9% across workloads.

| Workload | Elapsed-time change |
| --- | --- |
| `hashCodeHeavy` | 57.6% lower |
| `toStringHeavy` | 18.9% lower |
| `equalsHeavy` | 7.8% lower |
| `mapChurn` | 6.6% lower |
| `iteratorWalk` | 13.7% higher |

The iterator regression was reproducible and above its own noise floor. Its emitted body and the inspected iterator functions were byte-identical between arms. The PR did not establish a cause. It remains an open result, not something a smaller bundle or matching screenshot can explain away.

Node measurements also do not predict every browser's rendering or scheduling behavior. They isolate compiler/runtime throughput. A browser application still needs measurement in the browser and on the devices it targets.

## A narrower answer must remain correct

The local VM tests reported 305 tests, zero failures, and one pre-existing skip. The PR's JavaScript screenshot check subsequently reported 181 matching screenshots. That adds rendering evidence for the changed bridge handling, although a screenshot suite is not exhaustive proof of every dynamic dispatch path.

This is the JavaScript part of our {{< post-link path="/blog/performance-work-between-benchmarks" text="weekly performance work" >}}. The native runtime removes unnecessary allocations and main-thread waits. The web compiler removes unnecessary suspension. In each case the useful optimization comes from describing the program more accurately, while preserving the checks and fallback paths that keep valid application behavior intact.

---

## Discussion

_Have you found a compiler optimization whose effect was almost invisible in bundle size but clear in elapsed time?_

{{< giscus >}}
