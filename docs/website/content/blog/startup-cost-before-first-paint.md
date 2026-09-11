---
title: "Faster Starts, Less JavaScript Overhead"
slug: startup-cost-before-first-paint
url: /blog/startup-cost-before-first-paint/
date: '2026-09-14'
author: Shai Almog
description: "Codename One removes native startup waits, repeated style scans, and unnecessary JavaScript suspension. Profiles and compiler benchmarks expose costs that bundle size and frame rates miss."
feed_html: '<img src="https://www.codenameone.com/blog/startup-cost-before-first-paint.jpg" alt="Start Sooner Wait Less" /> Codename One removes native startup waits, repeated style scans, and unnecessary JavaScript suspension. Profiles and compiler benchmarks expose costs that bundle size and frame rates miss.'
series: ["release-2026-09-11"]
---

![Start Sooner Wait Less](/blog/startup-cost-before-first-paint.jpg)

A margin calculation should not need a synchronous trip to the operating system's main thread. Ours did. Every component that converted padding or margins to pixels could wait for AppKit to tell it which screen contained the window.

That was one of the startup costs found in [PR #5686](https://github.com/codenameone/CodenameOne/pull/5686). The measurements came from a native Mac build, so they identify concrete paths to fix without pretending to be universal iOS or Android startup numbers.

## Publish screen state when it changes

Pixel conversion needs the screen scale. AppKit owns the window-to-screen relationship, but the relationship does not change for every padding calculation.

The port now publishes screen identity and scale together when a window is created or moves. Readers consume that state atomically. Publishing them as one value also avoids pairing the screen from one update with the scale from another.

{{< mermaid >}}
flowchart LR
    A[Window created or moved] --> B[Publish screen and scale together]
    B --> C[Atomic state]
    C --> D[Pixel conversion]
    D --> E[Padding and margin layout]
{{< /mermaid >}}

The PR records 35 ms of blocked event-dispatch-thread time per launch on the old path. Installing an AppKit window observer also used a synchronous dispatch, despite no caller needing a result. Removing that wait addressed another reported 37 ms.

Those are measurements of individual costs. Adding them together and advertising that sum as a measured end-to-end startup improvement would ignore overlap, scheduling, and other work on the path.

## Try the lock before announcing a park

ParparVM's monitor entry announced a GC park before it knew whether acquiring the lock would block. An uncontended lock could therefore wait for the collector's handshake despite having no competing owner.

The implementation now tries the mutex first. Only the path that actually needs to wait enters the park protocol. The measured startup cost was 8.7 ms in the PR's run. Instrumentation also had to change: the stall report previously missed that handshake loop and reported zero.

A profiler cannot explain time it does not observe. Correcting the instrumentation was part of the fix, not an optional reporting improvement.

## Style construction repeated a global query

A UIID selects component styling. Before using a dark variant, `UIManager` needed to know whether one existed. It answered by scanning the whole theme table once for every distinct UIID.

The fix indexes dark keys once per theme generation. A new UIID then performs a lookup against that index, while loading a new theme invalidates the old answer.

| Native Mac measurement | Before | After |
| --- | --- | --- |
| First-use dark-variant lookup | 111,955 ns | 17,378 ns |

The change concerns this lookup in style construction, not the entire cost of constructing a component. It becomes valuable because a first screen often introduces many distinct UIIDs.

For an application-side diagnostic, measure construction separately from the first paint and use a fresh process for cold samples:

```java
import com.codename1.components.Switch;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

long started = System.currentTimeMillis();
Form form = new Form("Settings", BoxLayout.y());
form.add(new Label("Notifications"));
form.add(new Switch());
long constructionMs = System.currentTimeMillis() - started;
System.out.println("Form construction: " + constructionMs + " ms");
form.show();
```

This short example measures Java construction only. It is too coarse to reproduce the nanosecond lookup result and does not measure completion of the first visible frame.

## Preferred size should not generate artwork

`Switch.getPreferredSize()` used to build the artwork, including a Gaussian blur. Layout can ask for a size even if the component will never be painted. A hidden or discarded switch therefore paid for pixels nobody would see.

The preferred size now comes directly from the same dimensions used to draw the switch. Artwork generation stays on the path that needs artwork. This is the sort of optimization a per-frame rendering benchmark misses because the unwanted work happened before the first frame.

## Keep the image on the GPU path

The Metal pipeline also stopped round-tripping each picture through the CPU and retaining a decoded `EncodedImage` copy beside its GPU texture. Rounded corners can be handled in the shader instead of generating another rounded image.

That connects startup to steady-state memory. Loading a screen full of pictures can otherwise create several representations of each picture before the user has interacted with it. Removing a representation removes its construction cost and its lifetime from the memory profile.

This work complements {{< post-link path="/blog/parparvm-gc-small-heaps" text="collector-managed reference retention" >}}. Avoiding an unnecessary copy and deciding when to evict a useful copy are different jobs; both matter for an image-heavy application.

## The same profiling pass found correctness defects

A null socket handle was being unboxed into a long. An image creation path needed its one-pass premultiplication restored. A mismatched native symbol name had left rounded drawing inactive without a linker error. Forked Maven runs also failed to inherit `maven.repo.local`, which could make a developer run stale artifacts while believing a fix was under test.

The PR records 6,145 passing core tests and clean builds of the affected modules. Those checks support the changes, while the timings remain specific to the native Mac measurements recorded in the PR.


## JavaScript was waiting for the wrong reason too

On the native Mac path, a margin calculation waited for screen information that could already have been published. The JavaScript backend had a different unnecessary wait: an unrelated blocking method could make a synchronous call into a suspension point.

One blocking `run()` was enough to affect other `run()` methods, then their callers. [PR #5755](https://github.com/codenameone/CodenameOne/pull/5755) gives suspension analysis the receiver-type information it needs to stop that propagation.

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


## Spend the time on the screen the user asked for

A screen-scale lookup should read the current scale. A size query should calculate dimensions. A call that cannot block should not need generator dispatch. Each fix removes work that grew out of a broader assumption than the operation required.

The native timings describe startup paths; the Node measurements describe compiler and runtime throughput. They do not combine into one application score. They do give us better questions for the next profile, including the iterator regression we have not explained yet.

Across [this week's release](/blog/performance-work-between-benchmarks/), Codename One is reducing those costs inside the shared implementation. App teams can keep their Java screens and benefit as the ports improve. The compiler still takes the conservative path when it cannot resolve a receiver, and the runtime still coordinates with the collector when a lock must wait. Removing unnecessary work should preserve those safeguards.

---

## Discussion

_Which call in your profile spent time waiting when you expected it to finish immediately?_

{{< giscus >}}
