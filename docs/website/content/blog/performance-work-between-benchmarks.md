---
title: "The Performance Bugs Our Benchmarks Missed"
slug: performance-work-between-benchmarks
url: /blog/performance-work-between-benchmarks/
date: '2026-09-11'
author: Shai Almog
description: "Codename One reduces avoidable allocation, fixes pathological map misses, and removes startup and JavaScript overhead. The measurements explain both the gains and the remaining limits."
feed_html: '<img src="https://www.codenameone.com/blog/performance-work-between-benchmarks.jpg" alt="What The Bench Missed" /> Codename One reduces avoidable allocation, fixes pathological map misses, and removes startup and JavaScript overhead. The measurements explain both the gains and the remaining limits.'
series: ["release-2026-09-11"]
---

![What The Bench Missed](/blog/performance-work-between-benchmarks.jpg)

Our map benchmark looked healthy while an unsuccessful lookup could walk hundreds of thousands of slots. Our collector let a small backend accumulate garbage against a fixed floor that bought no measurable throughput. A layout query built switch artwork, including a blur, before anyone asked to paint it.

These were ordinary paths through ordinary code. They were also outside the questions our benchmarks had been asking.

This week's Codename One work follows those paths from Java collections into ParparVM, through style construction and the Apple renderer, and out to the JavaScript compiler. The aim is an application that starts sooner and keeps less memory tied up while it runs. Getting there means measuring the work between the convenient benchmark loops.

[Last week](/blog/voip-vpn-builders/) we focused on builders and operating-system integration. This week the runtime takes the lead. Continuity, native drag and drop, searchable API documentation, and Android 17 preparation round out the release.

## The week at a glance

The follow-ups publish through September 17. Each link becomes available on its publication date.

| Date | Deep dive | The question it answers |
| --- | --- | --- |
| September 12 | {{< post-link path="/blog/parparvm-gc-small-heaps" text="GC and small heaps" >}} | Why did a smaller collection floor save memory without costing throughput? |
| September 12 | {{< post-link path="/blog/hashmap-misses-probe-sequence" text="The map benchmark that missed misses" >}} | How can a healthy hit benchmark hide a 32-second lookup workload? |
| September 13 | {{< post-link path="/blog/parparvm-ranked-soft-references" text="Weak references and useful caches" >}} | When should a collector keep a decoded image? |
| September 13 | {{< post-link path="/blog/parparvm-tagged-boxed-values" text="Boxed values without an object allocation" >}} | How far can three spare pointer bits take us? |
| September 14 | {{< post-link path="/blog/startup-cost-before-first-paint" text="Startup before the first paint" >}} | Why was a margin calculation waiting on AppKit? |
| September 14 | {{< post-link path="/blog/javascript-suspension-receiver-types" text="JavaScript suspension analysis" >}} | Why did unrelated methods become generators? |
| September 15 | {{< post-link path="/blog/continuity-restoring-work" text="State restoration and continuity" >}} | How do you restore work after the process has disappeared? |
| September 15 | {{< post-link path="/blog/native-drag-drop-clipboard" text="Native drag and drop" >}} | Can a clipboard payload become a drag into another application? |
| September 16 | {{< post-link path="/blog/javadoc-hugo-markdown-doclet" text="Javadoc as part of the website" >}} | Why embed a second website inside the first one? |
| September 16 | {{< post-link path="/blog/pem-keys-and-clearing-recents" text="PEM keys and clearing recents" >}} | Where should security APIs remove ambiguity? |
| September 17 | {{< post-link path="/blog/android-37-readiness-location-button" text="API 37 and the location button" >}} | What can we prepare before a target SDK migration becomes mandatory? |

## A lower memory curve starts with less retained garbage

I have enormous respect for HotSpot's collectors. G1 targets multiprocessor machines with large memories, and the engineering behind it is a useful reference. HotSpot also offers collectors for smaller workloads, so treating it as one universal policy would miss the point. Our immediate problem was much narrower: a small native process retaining memory it did not need. [Oracle's collector guidance](https://docs.oracle.com/en/java/javase/25/gctuning/garbage-first-g1-garbage-collector1.html) describes that G1 design target.

Go gave us another useful reference for pacing collection and charging allocation for collector work. In the backend `/plaintext` workload with 64 connections, lowering ParparVM's collection floor brought reported loaded resident memory from **98 MB to 38 MB**. Throughput and p99 in the trigger sweep stayed within run-to-run noise. That is a useful result for a small process, not a promise that every application will use 38 MB.

![Reported loaded RSS at five collection trigger settings](/blog/gc-trigger-rss.svg)

*Trigger sweep reported in [PR #5717](https://github.com/codenameone/CodenameOne/pull/5717). These are separate configurations, not samples along a time axis. The final lower-floor configuration reported 38 MB; it is distinct from the 30 MB sweep point.*

We also tried sizing the floor from the live set and backed it out. The collector's sweep counters did not describe all the live objects, so the policy was making decisions from an incomplete population. The merged code gives deployments control over the floor and keeps the stock default. The {{< post-link path="/blog/parparvm-gc-small-heaps" text="GC article" >}} follows that failed approach and the parallel-mark experiment, where the reported median and p99 matched Go but the worst pauses still did not.

Flatter memory use remains the goal. Lower allocation pressure and reclaimable caches help, but the live objects your application actually needs still set the lower bound.

## The map was fast when the key existed

[PR #5722](https://github.com/codenameone/CodenameOne/pull/5722) fixes an open-addressed map whose dense integer keys formed a long uninterrupted run. Existing keys usually landed on the first probe. A missing key entering the run had to keep searching.

At 100,000 entries, the reported average miss fell from **16,742 probes to 1.53**. The fix preserves the first slot and changes the sequence after a collision. Scrambling every hash fixed misses too, but damaged the locality that made dense inserts cheap.

Go's Swiss tables are a useful comparison for compact storage and metadata filtering. ParparVM also keeps metadata separate from keys and values. Its merged probe loop remains scalar; SIMD-assisted string equality is a separate path through native `memcmp`, not Swiss-style group probing. The {{< post-link path="/blog/hashmap-misses-probe-sequence" text="map article" >}} shows the distinction, the Hashtable and IdentityHashMap improvements, and the large-table regression accepted alongside the fix.

## A cache must be allowed to forget

[PR #5732](https://github.com/codenameone/CodenameOne/pull/5732) gives ParparVM real weak and soft references. Previously, the VM traced the weak referent like an ordinary strong field. The iOS port also used a separate strong-reference table flushed on a memory warning as a substitute for soft-reference behavior.

The collector can now recognize these references and clear them safely. Soft retention uses the age since the last successful `get()`, so recently accessed data is favored. In the reported 160 MB simulated-budget run, ranked retention kept a **97.44% cache hit rate at 82.1 MB**, compared with **87.99% at 91.0 MB** for clearing on pressure.

Decoded images, resized copies, and rasterized borders make this concrete: throwing away useful artwork means decoding or drawing it again. The {{< post-link path="/blog/parparvm-ranked-soft-references" text="reference article" >}} explains the concurrent-read race and an essential release boundary: this PR supplies the VM machinery; migration of the iOS cache table and framework call sites is separate work.

## Box the number, skip the object when it fits

Our [earlier performance article](/blog/beating-hotspot-performance/) described tagged integers. [PR #5735](https://github.com/codenameone/CodenameOne/pull/5735) extends that representation to the other numeric wrappers and `Character`. Eligible values live in the reference-sized word itself, so there is no separate object for the collector to visit.

```java
Map<Long, Double> readings = new HashMap<Long, Double>();
readings.put(42L, 12.5);
```

This remains ordinary Java. On ParparVM, both values in this example fit the tagged representation. Full-range Long and Double values do not all fit, so they retain a heap fallback. Byte and Boolean already have bounded caches.

The allocation census measures the benefit: a JSON-like workload went from 24.02 boxed allocations per map to 5.24. The {{< post-link path="/blog/parparvm-tagged-boxed-values" text="boxing article" >}} explains the encoding. The familiar "poor man's Valhalla" analogy describes the ambition, but the mechanism is tagged immediates, not general value classes or stack-allocated objects.

## Startup work hiding in layout

[PR #5686](https://github.com/codenameone/CodenameOne/pull/5686) found Apple main-thread round trips inside pixel conversion, an unnecessary wait while installing a window observer, and a GC handshake before an uncontended lock had even tried to acquire its mutex.

Style lookup had its own repeated work. `UIManager` scanned the theme table to discover dark variants for each distinct UIID. An index built once per theme generation reduced the reported first-use lookup from **111,955 ns to 17,378 ns** on the native Mac benchmark. `Switch` now computes its preferred size without generating blurred artwork.

The {{< post-link path="/blog/startup-cost-before-first-paint" text="startup article" >}} also covers the Metal image path, which avoids a CPU round trip and a retained decoded copy beside the texture. These changes remove specific work; the individual timings are not an additive whole-app startup score.

## JavaScript methods that never needed to suspend

The JavaScript backend turns potentially blocking Java methods into generators. Its analysis used to propagate that property by method name and descriptor without considering the receiver class. One blocking `run()` could affect unrelated `run()` methods throughout the application.

[PR #5755](https://github.com/codenameone/CodenameOne/pull/5755) uses receiver-type information already available to the compiler. In the sample application, generated `yield*` sites fell **25.3%**, while bundle size fell only **1.2%**. Counting bytes would have hidden most of the change.

The {{< post-link path="/blog/javascript-suspension-receiver-types" text="JavaScript deep dive" >}} covers the throughput measurements and the unexplained **13.7% iterator regression**. That open result is part of the work still ahead.

## Pick up the work after the process is gone

Saving a `Form` in a field helps a suspended process. It does nothing after the operating system reclaims that process. [PR #5663](https://github.com/codenameone/CodenameOne/pull/5663) adds checkpoints for application state and the router stack, with explicit restoration at the point the application chooses.

Apple Handoff can carry the activity to another signed-in device. An application-owned `StateRelay` provides a route across other devices and accounts. iCloud key-value sync lives in a separate package so ordinary restoration does not silently acquire an entitlement requirement.

The {{< post-link path="/blog/continuity-restoring-work" text="continuity article" >}} includes the account boundary: restore after authentication, and clear and disable continuity on logout so an arriving activity cannot bring the previous account's screen back.

## A drag can cross the application boundary

Our lightweight drag and drop remains useful inside a form. [PR #5662](https://github.com/codenameone/CodenameOne/pull/5662) adds operating-system drag and drop beside it, using `ClipboardContent` as the payload.

That lets the same content offer plain text, HTML, or files according to what the receiving application accepts. Lazy providers delay expensive file generation until someone actually reads the representation. Completion reports whether a move succeeded before the source considers deleting its copy.

The {{< post-link path="/blog/native-drag-drop-clipboard" text="drag and drop article" >}} includes the supported ports and the thread boundary that would otherwise deadlock JavaSE. Native AppKit, Windows, Linux, and JavaScript are outside this initial implementation.

## The API reference belongs to the site

We kept improving the developer guide, but the API reference still looked and behaved like a separate site embedded in ours. [PR #5743](https://github.com/codenameone/CodenameOne/pull/5743) replaces that arrangement with a doclet that emits Hugo content from the Java API model.

The website now owns the theme, member layout, and search. Java 25 supplies the documentation tools; Markdown comments themselves arrived in JDK 23. The downloadable standard Javadoc archive still comes from the same sources.

The {{< post-link path="/blog/javadoc-hugo-markdown-doclet" text="doclet article" >}} includes a small reusable example and the source paths other Java projects need. Preserving old method anchors was a substantial part of the implementation, not an optional redirect cleanup.

## Fewer ambiguous steps in security code

[PR #5707](https://github.com/codenameone/CodenameOne/pull/5707) accepts PEM keys directly through `PublicKey.fromPem` and `PrivateKey.fromPem`. It handles supported container differences and rejects malformed or unsupported input with an explanation. Application code no longer has to improvise its own armor stripping and DER conversion.

[PR #5746](https://github.com/codenameone/CodenameOne/pull/5746) adds `CN.exitAndClearTask()`. Android removes the task from recents before exiting; other ports retain their existing exit behavior. This is useful at the end of sign-out or a kiosk session, but it does not revoke credentials or erase application data.

The {{< post-link path="/blog/pem-keys-and-clearing-recents" text="security article" >}} keeps those responsibilities explicit. Parsing a key and ending a task should each mean one thing.

## Preparing for API 37 without forcing every build to move

[PR #5731](https://github.com/codenameone/CodenameOne/pull/5731) adds compilation checks against API 37 and fixes builder assumptions about minor-versioned Android platforms. [PR #5738](https://github.com/codenameone/CodenameOne/pull/5738) adds the system location button, with an ordinary Codename One button on older platforms.

The distinction matters. Compiling against the new SDK, changing the target SDK, and verifying new runtime behavior are separate jobs. We can do the preparation before requiring customers to migrate. The {{< post-link path="/blog/android-37-readiness-location-button" text="Android article" >}} shows what is verified and where cloud-builder rollout and broader runtime coverage still need checking.

## More work stays in the shared framework

{{< mermaid >}}
flowchart LR
    B[Tagged boxes] --> A[Fewer allocations]
    M[Compact maps] --> A
    R[Reference processing] --> C[Reclaimable cached data]
    A --> G[Less collector work]
    C --> G
    S[Style and layout fixes] --> P[Less work before painting]
    J[Receiver-aware JS analysis] --> D[Fewer suspension points]
{{< /mermaid >}}

A smaller allocation count helps the collector. A cache that can release cold data avoids choosing between permanent retention and a complete flush. A style lookup that stays off the platform main thread leaves that thread available for work only it can do. Those improvements reinforce each other even though their benchmark ratios cannot be multiplied into one score.

The same ownership extends to security. Builders can request the permission a feature needs. A key parser can reject the wrong container before cryptographic code sees it. Continuity can expose an explicit logout boundary, and Android can draw the consent control itself. These are concrete ways to make the safe path easier to use and harder to accidentally bypass.

That is where we intend to keep extending Codename One's advantage: shared, reviewable behavior from the Java API through the generated application. App teams still own authorization and their data, but they should not each have to rediscover a collector race, maintain their own PEM parser, or reconstruct a platform permission handshake. The remaining performance work will be measured against the cases that escaped us this time.

---

## Discussion

_Which workload made your application feel slow while the benchmark still looked fine?_

{{< giscus >}}
