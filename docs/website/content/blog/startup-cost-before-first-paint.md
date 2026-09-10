---
title: "Why Was a Margin Calculation Waiting on AppKit?"
slug: startup-cost-before-first-paint
url: /blog/startup-cost-before-first-paint/
date: '2026-09-14'
author: Shai Almog
description: "Native Mac profiling finds main-thread waits in pixel conversion, repeated theme scans, and artwork built during layout. Codename One removes those costs and shortens the Metal image path."
feed_html: '<img src="https://www.codenameone.com/blog/startup-cost-before-first-paint.jpg" alt="Before The First Paint" /> Native Mac profiling finds main-thread waits in pixel conversion, repeated theme scans, and artwork built during layout. Codename One removes those costs and shortens the Metal image path.'
series: ["release-2026-09-11"]
---

![Before The First Paint](/blog/startup-cost-before-first-paint.jpg)

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

This work complements {{< post-link path="/blog/parparvm-ranked-soft-references" text="collector-managed reference retention" >}}. Avoiding an unnecessary copy and deciding when to evict a useful copy are different jobs; both matter for an image-heavy application.

## The same profiling pass found correctness defects

A null socket handle was being unboxed into a long. An image creation path needed its one-pass premultiplication restored. A mismatched native symbol name had left rounded drawing inactive without a linker error. Forked Maven runs also failed to inherit `maven.repo.local`, which could make a developer run stale artifacts while believing a fix was under test.

The PR records 6,145 passing core tests and clean builds of the affected modules. Those checks support the changes, while the timings remain specific to the native Mac measurements recorded in the PR.

The {{< post-link path="/blog/performance-work-between-benchmarks" text="rest of this week's work" >}} reaches the same problem from the collector and compiler. We own these layers so an application team can improve a shared screen without rebuilding the same investigation for each native target. The next startup profile should spend its time on useful application work, not on a margin asking which monitor it lives on.

---

## Discussion

_Which layout query in your application does more work than its name suggests?_

{{< giscus >}}
