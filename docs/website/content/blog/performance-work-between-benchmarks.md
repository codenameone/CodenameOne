---
title: "Lies, Damn Lies and Benchmarks"
slug: performance-work-between-benchmarks
url: /blog/performance-work-between-benchmarks/
date: '2026-09-11'
author: Shai Almog
description: "Codename One tackles misleading benchmarks, GC and cache pressure, boxed allocations, and unnecessary waits. Six deep dives cover the performance work, native integration, docs, and security."
feed_html: '<img src="https://www.codenameone.com/blog/performance-work-between-benchmarks.jpg" alt="Lies, Damn Lies and Benchmarks" /> Codename One tackles misleading benchmarks, GC and cache pressure, boxed allocations, and unnecessary waits. Six deep dives cover the performance work, native integration, docs, and security.'
series: ["release-2026-09-11"]
---

![Lies, Damn Lies and Benchmarks](/blog/performance-work-between-benchmarks.jpg)

There's an old joke about three kinds of lies: [lies, damn lies, and benchmarks](https://www.spec.org/osg/news/articles/news9412/lies.html). Codename One did very well in our benchmarks. But were they accurate? Were they representative?

Performance is the fitted sheet of programming. Just when you get the damn corner into place, the rest of the bed is a complete mess.

This week we made a concentrated effort to get a few more corners into place, and got a lot done. There's still work ahead that we'll discuss next week, but it's getting to a point where I feel confident saying Codename One is pretty fast. Not the best (yet), but fast.

The trouble with a good benchmark score is how little it can tell you about the next operation. Our map was fast at finding keys that existed. Asking for missing keys exposed a lookup that could walk hundreds of thousands of slots. A collector setting that looked reasonable for a larger process let a small backend retain garbage without buying measurable throughput. A rendering benchmark had no reason to notice that a switch generated blurred artwork before anyone asked to paint it.

Those costs meet in an application. Opening a screen constructs styles and lays out components. Loading its data fills maps and boxes numbers. Displaying images creates results worth caching, until memory gets tight. We worked through those layers together because making one loop faster does not tell us whether the screen starts sooner or leaves less memory behind.

[Last week](/blog/voip-vpn-builders/), builders let us take responsibility for native integration that used to belong in each application's platform project. This week we are using the same reach inside the runtime: changing collection, object representation, UI internals, and generated JavaScript while keeping the application code in Java.

## This week

The parent covers the mechanisms below. Six follow-ups take them further, with one article per day through September 17. Links become available on their publication dates.

| Date | Follow-up | What we investigate |
| --- | --- | --- |
| September 12 | {{< post-link path="/blog/parparvm-gc-small-heaps" text="The collector and the cache" >}} | Collection pacing, weak references, and keeping useful images |
| September 13 | {{< post-link path="/blog/hashmap-misses-probe-sequence" text="Maps and the objects inside them" >}} | Missing keys, probe sequences, and wider tagged values |
| September 14 | {{< post-link path="/blog/startup-cost-before-first-paint" text="Unnecessary waiting, from AppKit to JavaScript" >}} | Startup profiles, style construction, and suspension analysis |
| September 15 | {{< post-link path="/blog/continuity-restoring-work" text="Work that moves to another screen" >}} | State restoration, cross-device continuity, and native drag and drop |
| September 16 | {{< post-link path="/blog/javadoc-hugo-markdown-doclet" text="Javadoc inside the website" >}} | A reusable doclet, integrated search, and preserving member links |
| September 17 | {{< post-link path="/blog/android-37-readiness-location-button" text="Android 17 and less security glue" >}} | API 37, location consent, PEM keys, and clearing recents |

## Why keep garbage if it isn't buying speed?

ParparVM translates Java bytecode to C for Codename One's native ports. Its collector therefore has to serve both a mobile application and the small backend processes we have been using to investigate the runtime. Those processes make a large garbage allowance easy to spot: there may be very little live application data underneath it.

I have enormous respect for HotSpot's collectors. G1 is explicitly designed for multiprocessor machines with large memories. HotSpot offers other collectors too, including Serial for small data sets, so “copy HotSpot” is not a collection policy. We needed to examine the assumptions against our own workloads. [Oracle's G1 guidance](https://docs.oracle.com/en/java/javase/25/gctuning/garbage-first-g1-garbage-collector1.html) describes that design target.

Go was a useful reference because its [GC guide](https://go.dev/doc/gc-guide) makes the tradeoff explicit. Let more garbage accumulate and you can do collection work less often. Collect sooner and you spend less memory, but may spend more CPU tracing the same live objects. That extra memory should earn its keep.

In our `/plaintext` backend workload with 64 connections, it wasn't. The stock 24 MB allocation floor was associated with 98 MB of loaded resident memory. Sweeping through lower trigger settings left throughput and p99 within run-to-run noise. A subsequent lower-floor configuration brought loaded RSS down to **38 MB**. [PR #5717](https://github.com/codenameone/CodenameOne/pull/5717) records the experiment.

![Reported resident memory at five GC trigger settings](/blog/gc-trigger-rss.svg)

*Separate trigger configurations, not a time series. The sweep's 4 MB point reported 30 MB RSS; the subsequent configuration reported 38 MB. RSS includes resident process memory beyond the Java heap.*

### The live-set estimate was missing live objects

The tempting next step was to derive the floor from the amount of live data. That would let a small process collect sooner and a larger one keep more allocation headroom.

We tried it and backed it out. The sweep counters described retired pages, not the whole live population. Partial pages and the pages a mutator was still allocating into were outside that count; large objects had another path. Feeding that number into a proportional policy gave the policy an incomplete picture. One attempt retained an old live-set estimate long enough to suppress the sweeps needed to return pages to the OS.

The merged code lets a deployment choose its minimum instead:

```c
#ifndef CN1_BIBOP_GC_MIN_TRIGGER_BYTES
#define CN1_BIBOP_GC_MIN_TRIGGER_BYTES CN1_BIBOP_GC_TRIGGER_BYTES
#endif
```

That is runtime configuration, not an application build hint. The backend can select a smaller floor; the stock default stays unchanged. A policy based on the live set needs a count we can trust first.

Parallel marking is the other part of the investigation. In the recorded allocation loop, ParparVM's median and p99 matched Go. The worst pause still favored Go: about 20 ms, against 0.3 to 0.9 seconds with four ParparVM markers and 2.2 to 3.3 seconds with one. Parallel marking remains experimental. Allocation threads can help drain marking work instead of merely waiting at the run-ahead cap, but that assistance belongs to the parallel path, not the unchanged serial default.

### A useful image should survive longer than a cold one

Collecting sooner cannot help if the collector treats disposable cached data as permanently reachable. ParparVM's old `WeakReference` held its referent in a field that the translator marked like any strong field. As long as the wrapper survived, so did the supposedly weak object.

The iOS port approximated soft references with a strong-reference table and a low-memory warning. `flushSoftRefMap()` replaced the table when pressure arrived. That could free a batch of cached artwork, but it also discarded the image the user was about to scroll back to.

[PR #5732](https://github.com/codenameone/CodenameOne/pull/5732) gives the collector real weak and soft references. It discovers the referent as a special edge, decides whether a soft referent should survive, then clears references eligible for reclamation. Soft retention uses the age since the last successful `get()`. Repeatedly used images tend to stay recent; cold ones lose priority.

The application-facing contract looks like this on the updated ParparVM path:

```java
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

This excerpt uses `java.lang.ref.SoftReference`, `com.codename1.ui.Image`, and `java.io.IOException`, and assumes one calling thread. The local `result` keeps a successful read strongly reachable while it is used. A miss reconstructs the image. A soft cache is appropriate only when that reconstruction is possible.

The difficult case is a read during collection. A thread can call `get()` after the collector has scanned its roots. Freeing the referent without noticing that read would hand Java code a dangling native pointer. Reference reads now participate in the marking barrier, and the collector drains new work before deciding it can sweep.

{{< mermaid >}}
sequenceDiagram
    participant App as Application thread
    participant Ref as Soft reference
    participant GC as Collector
    GC->>App: Scan roots
    App->>Ref: get()
    Ref->>GC: Log referent when marking requires it
    Ref->>App: Return image
    GC->>GC: Drain new work before ending marking
    GC->>Ref: Clear eligible cold referents
    GC->>GC: Sweep
{{< /mermaid >}}

In the simulated 160 MB budget test, ranked retention produced a **97.44% hit rate at 82.1 MB**, compared with **87.99% at 91.0 MB** for clearing on pressure. That is the policy benchmark, not a device scrolling result. The VM machinery is ready; moving the iOS cache table and framework call sites onto it remains separate work.

The {{< post-link path="/blog/parparvm-gc-small-heaps" text="collector and cache article" >}} follows both experiments in detail. Lowering garbage headroom and retaining useful cached results address different sides of the same memory budget.

## The map was fast because we kept asking the easy question

Our open-addressed HashMap stores entries in arrays. A key's hash selects its first slot. If that slot is occupied by another key, the map probes elsewhere until it finds the key or an empty slot.

Dense integer keys happened to make the first probe look excellent. Integer hashes preserve the value, and the usual `h ^= h >>> 16` spread leaves small integers near their original positions. Insert consecutive integers starting at zero and occupied slots form a long run. A successful lookup often lands directly on its key. A missing key entering that run must walk to its end under linear probing. Tombstones left by deletion cannot terminate the search either.

At 100,000 entries, the measured miss averaged **16,742 probes**. At a million entries, it averaged **222,721**. Hits stayed at one probe. A benchmark dominated by hits had almost nothing to say about this failure.

### Preserve the first probe and change what follows

Scrambling every hash fixed the misses, but made dense construction and scans 1.8 to 2.2 times slower in the measured cases. We wanted to keep the locality of that first slot.

[PR #5722](https://github.com/codenameone/CodenameOne/pull/5722) keeps the first probe and changes the collision sequence using the recurrence associated with CPython's dictionary probing:

```java
static int cn1NextSlot(int i, int perturb, int mask) {
    return ((i << 2) + i + 1 + perturb) & mask;
}
```

The caller shifts the unsigned perturbation right by five bits between probes. As those hash bits feed the recurrence, a collision can escape the occupied run. Once the perturbation reaches zero, the recurrence still visits the power-of-two table. Lookup, insertion, growth, and deletion have to agree on that sequence.

| Workload | Before | After |
| --- | --- | --- |
| Mean probes per miss, 100,000 entries | 16,742 | 1.53 |
| Three million calls in the miss-heavy workload | 32,698 ms | 44.9 ms |
| String-key workload | 33.6 ms | 25.5 ms |
| Large table with random hits | 26.6 ms | 33.3 ms |

The last row got worse. That tradeoff belongs beside the spectacular miss result, because the change did not make every map operation hundreds of times faster.

Go's [Swiss maps](https://go.dev/blog/swisstable) were another useful comparison. ParparVM already keeps compact metadata separate from its key and value arrays, so we were closer in structure than a map built around an allocation per entry. The merged lookup remains scalar perturbed probing. The SIMD-related improvement is on the string-key path: unequal cached hashes reject a match early, and compatible UTF-16 storage goes through native `memcmp`, where the platform can use optimized vector comparisons. This is distinct from Swiss-table group probing. These PR results do not establish equal-workload timing parity with Go.

Hashtable also moves away from an `Entry` allocation per mapping. IdentityHashMap needed a different fix: ParparVM's identity hash comes from an aligned address, so copying an indexing expression suited to HotSpot's already-scrambled identity hashes left too many zero low bits. Folding high bits down improved its measured distribution.

### A compact table can still be full of little allocations

A JSON parser filling a map may allocate a wrapper for every number. Our [earlier performance work](/blog/beating-hotspot-performance/) removed much of that cost for Integer. [PR #5735](https://github.com/codenameone/CodenameOne/pull/5735) extends the representation to Short, Character, Float, Long, and Double.

Aligned object addresses leave three low bits free in this 64-bit runtime. The collector's root scan already distinguishes words with those bits set from ordinary object pointers. We use them as a type tag and put the value in the remaining 61 bits. The map slot contains the number's representation directly, instead of a pointer to a separate wrapper.

```java
Long small = Long.valueOf(42L);             // Tagged on ParparVM
Long large = Long.valueOf(Long.MAX_VALUE); // Heap fallback
Double exact = Double.valueOf(12.5);       // Tagged
Double fraction = Double.valueOf(0.1);     // Heap fallback
```

Short, Character, and Float fit across their full ranges. Long fits from `-2^60` through `2^60 - 1`. Double fits when its low three mantissa bits are clear. Byte and Boolean already have bounded caches. Use `equals()` for value equality; application code should not infer identity from the runtime's allocation choices.

{{< mermaid >}}
flowchart LR
    V[Box a primitive value] --> F{Fits the encoding?}
    F -->|Yes| T[Type tag and payload in the reference word]
    F -->|No| H[Allocate a heap wrapper]
    T --> D[Dispatch using the tagged type]
    H --> D
    D --> J[Java wrapper behavior]
{{< /mermaid >}}

The JSON-like allocation census fell from **24.02 boxed allocations per map to 5.24**. That is work the collector never has to do. It is also why the “poor man's Valhalla” callback is useful, provided we keep the mechanism straight: these are tagged values, not general value classes or stack-allocated objects. A tagged value can live inside a heap collection.

The {{< post-link path="/blog/hashmap-misses-probe-sequence" text="maps and boxing article" >}} covers the coverage measurements and dispatch hazards. A type tag must select the right `hashCode` and `equals` implementation. Reusing the Integer fast path for every tag would produce a fast, incorrect map.

## A margin calculation should not wait for AppKit

Before the first frame, a screen asks a lot of components how big they are. Padding and margin conversion needs the screen scale. Our native Mac path synchronously asked AppKit which screen contained the window, even though the answer usually had not changed since the previous component asked.

[PR #5686](https://github.com/codenameone/CodenameOne/pull/5686) publishes screen identity and scale together when the window is created or moves. Layout reads that state atomically. Publishing the pair together also prevents a reader from combining one screen with another screen's scale.

The old query accounted for 35 ms of blocked event-dispatch-thread time in the recorded startup profile. Installing a window observer added another synchronous wait even though the caller needed no return value. That path accounted for 37 ms. These are individual native Mac costs, not numbers to add into a universal startup score.

The same profile found an uncontended lock announcing that it was about to park before trying to acquire its mutex. That announcement could wait on a GC handshake. The runtime now tries the lock first and enters the park protocol when it actually has to wait.

### Layout was doing work that belonged to painting

`UIManager` scanned the whole theme table to find dark variants for each distinct UIID. We now build an index once per theme generation. First-use dark-variant lookup in the benchmark fell from **111,955 ns to 17,378 ns**. The index is invalidated when a new theme changes the answer.

`Switch.getPreferredSize()` had a more surprising cost: generating switch artwork, including a Gaussian blur. A layout query could pay that cost for a component that was never painted. Preferred size now comes from the same dimensions used by the drawing code; generating the pixels waits until they are needed.

{{< mermaid >}}
flowchart LR
    W[Window created or moved] --> S[Publish screen scale]
    S --> L[Layout reads current scale]
    T[Theme loaded] --> I[Index dark UIIDs once]
    I --> C[Construct component style]
    D[Switch drawing dimensions] --> P[Calculate preferred size]
    P --> R[Generate artwork when painting needs it]
{{< /mermaid >}}

The Metal path also avoids taking each picture back through the CPU and retaining a decoded `EncodedImage` copy beside the texture. Rounded corners can be drawn in the shader. Removing an image representation saves both its construction cost and the memory it would occupy after startup.

### JavaScript was preparing to suspend methods that could not block

The web compiler had a similar problem with an overly broad assumption. A Java method that might block becomes a JavaScript generator so execution can yield and later resume. Our analysis used the method name and descriptor without distinguishing its receiver class.

Consider these two unrelated classes:

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

A call on `CounterTask` cannot reach `WaitingTask.run()`. But treating `run()` as one blocking signature made unrelated calls suspension points, then spread that property into their callers.

[PR #5755](https://github.com/codenameone/CodenameOne/pull/5755) reuses the receiver types already computed by reachability analysis. It considers implementations that can actually receive the call. Unresolved dispatch stays conservative, and analysis and code generation share the same model so they agree about where `yield*` is legal.

| Generated sample output | Before | After |
| --- | --- | --- |
| `yield*` sites | 54,549 | 40,741 |
| Generator methods | 13,068 | 11,044 |
| Bundle size | 8,089,807 bytes | 7,993,916 bytes |

Yield sites fell **25.3%** while bundle size fell only **1.2%**. Counting bytes would barely notice the change in dispatch. Node throughput measurements found gains in several workloads, but `iteratorWalk` took **13.7% longer**, with no established cause yet. That is on the list for further investigation.

The {{< post-link path="/blog/startup-cost-before-first-paint" text="native and JavaScript performance article" >}} follows these paths and their tests. The common question is whether an operation needs to wait at all, before trying to make its waiting machinery faster.

## Let the work survive the process, then move it somewhere useful

Lower memory use helps a process survive. It does not guarantee that a mobile operating system will keep it alive. A `Form` saved in a field disappears with that process, and the user returns to the first screen.

[PR #5663](https://github.com/codenameone/CodenameOne/pull/5663) adds `com.codename1.continuity`. It saves application state and the router stack as a checkpoint that a new process can reconstruct. A route identifies the screen; a `StateProvider` supplies the data needed to resume the work. For a draft editor, that can be as small as the text:

```java
Continuity.setStateProvider(new StateProvider() {
    public Map<String, Object> saveState() {
        Map<String, Object> state = new HashMap<String, Object>();
        state.put("draft", draftField.getText());
        return state;
    }

    public void restoreState(Map<String, Object> state) {
        Object draft = state.get("draft");
        draftField.setText(draft instanceof String ? (String) draft : "");
    }
});
```

Here `draftField` is the application's TextArea. Registering the provider enables continuity. Navigation schedules checkpoints; edits can call `Continuity.checkpoint()`. Keep the payload small, exclude credentials, and decide whether sensitive drafts belong in it.

Restoration happens after the application knows which account is active:

```java
// Run after authentication and route registration.
if (!Continuity.restore()) {
    Navigation.navigate("/home");
}
```

Apple Handoff can offer the activity to another signed-in device. An application-owned `StateRelay` can carry state through your existing accounts and server, including across Android, Apple devices, and a browser. Codename One does not operate that relay or choose its account-isolation policy. iCloud key-value sync is a separate package with its own entitlement requirement.

### A drag carries a representation instead of a checkpoint

[PR #5662](https://github.com/codenameone/CodenameOne/pull/5662) adds native drag and drop beside the lightweight in-form API. A file dragged into another app uses `ClipboardContent`, the same payload model as copy and paste. It can offer files, plain text, or HTML and let the receiving app choose a supported representation.

```java
Label file = new Label("report.pdf");
file.setNativeDragOperation(NativeDragOperation.createFileDrag(paths));

inbox.setNativeDropTarget(true);
inbox.setAcceptedDropMimeTypes(ClipboardContent.MIME_FILE);
inbox.addNativeDropListener(event -> {
    String[] received = ((NativeDropEvent) event).getFiles();
    if (received != null) {
        queueImport(received);
    }
});
```

This excerpt assumes existing export `paths`, a receiving component named `inbox`, and an application `queueImport` that validates files and moves expensive parsing off the event dispatch thread. MIME acceptance is not a trust decision.

Provider timing depends on the port. JavaSE can request the representation lazily. Android assembles complete `ClipData` before starting the drag, so every provider runs then. iOS resolves file lists at drag start to count the items. Keep providers cheap enough for that moment, or prepare and cache an expensive export beforehand. Canceling a drag does not guarantee that its data was never generated.

The initial implementation covers JavaSE, Android, and UIKit ports, with cross-application behavior on Android, iPadOS, and Mac Catalyst. iPhone has a narrower interaction model. Native AppKit, Windows, Linux, and JavaScript do not implement this native-drag path yet. Check `NativeDragAndDrop.isSupported()` before relying on it.

The {{< post-link path="/blog/continuity-restoring-work" text="continuity and drag-and-drop article" >}} includes the platform matrix, callback threading, and logout sequence. A received checkpoint must still pass the current account's authorization checks; a completed move must be confirmed before deleting the source. Moving work should not mean losing ownership of it.

## The API reference should not bring a second website with it

We kept improving the developer guide, but the API reference still carried standard Javadoc's layout and stylesheet into a wrapper inside our site. Search and dark mode made that separation particularly obvious.

[PR #5743](https://github.com/codenameone/CodenameOne/pull/5743) changes what the doclet emits. Javadoc supplies the Java API model and documentation trees. The new `maven/javadoc-hugo-doclet` module writes Hugo content with structured front matter for signatures and members. Hugo renders it using the site's own templates and Markdown renderer.

{{< mermaid >}}
flowchart LR
    J[Java sources and Markdown comments] --> D[Javadoc API model]
    D --> C[Hugo content and member metadata]
    C --> H[Website templates]
    D --> I[API search index]
    I --> S[Website search]
    D --> Z[Standard doclet for offline archive]
{{< /mermaid >}}

The tooling uses Java 25 in a separate module from the Java 8 build. Markdown documentation comments themselves arrived in JDK 23. The doclet preserves their prose for Hugo and extracts recognized sections such as parameters and returns into member data. We still generate the standard `javadocs.zip` archive from the same sources.

![Website search finds PublicKey and PrivateKey fromPem members](/blog/javadoc-hugo-search.png)

*A live site capture from September 10. API methods now appear in website search, grouped by type. The full developer guide retains its own navigation and browser search rather than joining this index.*

This is reusable source for Java developers who want their own site generator to own the API pages. The [doclet module](https://github.com/codenameone/CodenameOne/tree/561dab8e05/maven/javadoc-hugo-doclet), [Hugo layouts](https://github.com/codenameone/CodenameOne/tree/561dab8e05/docs/website/layouts/javadoc), and build integration are available in the repository. You still need to adapt the templates and URL conventions to your site.

The migration also checked **2,272 pages and 29,583 fragments** against the old output. A prettier member page is not much use if years of links to that member stop working. The {{< post-link path="/blog/javadoc-hugo-markdown-doclet" text="doclet article" >}} includes a complete sample, build commands, and the anchor defects that parity testing caught.

## Prepare Android 17 without handing customers another migration project

[PR #5731](https://github.com/codenameone/CodenameOne/pull/5731) adds API 37 compilation checks and generated-application assembly. This matters before changing the default target SDK: compiling against the old platform cannot tell us which referenced classes the new one has removed.

The check compiles the same port sources against both platform jars and compares the errors. It removes duplicate `android.jar` entries so an old jar cannot silently supply a symbol missing from the new platform. A fault-injection test using removed fingerprint APIs proved that the check catches the failure it was designed to find.

Builder assumptions needed attention too. Platform names such as `android-37.2` are no longer integer suffixes. Stripping punctuation would turn `37.2` into `372`, which passes minimum-version checks for completely the wrong reason. The builder now extracts the major level correctly and uses the SDK manager belonging to the selected SDK root. The normal SDK floor remains 36 while this preparation continues.

### Ask for location at the moment the user needs it

[PR #5738](https://github.com/codenameone/CodenameOne/pull/5738) adds the Android 17 system location button, with an ordinary Codename One button and existing permission flow on older platforms:

```java
LocationButton location = new LocationButton(
        LocationButton.TEXT_USE_PRECISE_LOCATION);
location.addLocationSharedListener(fix -> {
    if (fix != null) {
        searchNearby(fix.getLatitude(), fix.getLongitude());
    }
});
form.add(location);
```

`searchNearby` belongs to the application. Declined permission or an unavailable result can produce `null`. The component exposes `isSystemRendered()` so an application can verify which path is active.

The OS draws the supported control into a hosted surface and ties the request to a visible user action and a session-scoped location grant. The implementation uses the platform session API through reflection, avoiding an AndroidX dependency that would force a newer Android Gradle Plugin on every application using the button. The builder adds `USE_LOCATION_BUTTON` when it finds the component; the separate cloud-builder mirror still needs to be present in the builder serving the app.

The PR exercised the system button, a human tap, consent, and a returned location on an Android 17 emulator, with fallback on API 36. That is useful runtime evidence for this flow. It is not a claim that every API 37 behavior has been tested, or that a location-policy deadline is the general target-SDK deadline. The {{< post-link path="/blog/android-37-readiness-location-button" text="Android and security article" >}} separates those checks and links the current platform guidance.

### Stop making each application parse its own key armor

[PR #5707](https://github.com/codenameone/CodenameOne/pull/5707) adds `PublicKey.fromPem` and `PrivateKey.fromPem` for text or file bytes:

```java
PublicKey publicKey = PublicKey.fromPem(
        Util.readInputStream(publicStream));
PrivateKey privateKey = PrivateKey.fromPem(
        Util.readInputStream(privateStream));
```

These use the Codename One security classes and the application's existing input streams. The parser strips the armor, decodes Base64, validates a complete DER element, and recognizes the key container. Supported PKCS#1 RSA and SEC1 EC inputs are rewrapped into the container expected downstream. Encrypted keys, certificates, wrong public/private direction, and truncated or trailing DER are rejected with an explanation.

That removes format handling from application glue code. It does not decide whether a public key is trusted or where a private key should be stored.

### Removing a task is part of ending a session

[PR #5746](https://github.com/codenameone/CodenameOne/pull/5746) adds `CN.exitAndClearTask()`. On Android it removes the task from recents before following the process-exit path. Killing the process alone could leave the task in the switcher.

For an application using continuity, the framework calls at logout are:

```java
Continuity.clear();
Continuity.disable();
// Finish account cleanup and any asynchronous sign-out work before this call.
CN.exitAndClearTask();
```

Clearing saved state without disabling continuity leaves a route for an arriving activity to restore it again. Removing the task without revoking credentials leaves a session alive. These calls handle restoration and task lifecycle; your application still owns credential revocation and account data. Other ports retain their existing exit behavior.

## Less runtime work, fewer places to get security wrong

The collector and reference changes let us distinguish garbage from useful cached data. The map changes fix a pathological search and remove wrapper allocations from its contents. Style construction, layout, and JavaScript generation now do less work before the application can get on with its own job. There are still regressions to investigate and cache integrations to finish, which is why we are continuing the performance work next week.

Continuity and native drag and drop expand what the user can do with that application. The doclet puts the API contracts where site search can find them. Android preparation moves platform breakage into our checks before a mandatory migration moves it into a customer's release schedule.

An application can use ordinary Java references while Codename One's runtime handles the race between a cache read and collection. It can read a PEM key without maintaining its own DER conversion. It can ask for a location through the OS consent surface and explicitly shut down restoration when an account signs out.

Secure-by-default programming depends on those details. We maintain the API, translator, runtime, and builders together so app teams do not each have to rediscover them in native code. The application's authorization and data rules still belong to its developers. The shared implementation should leave them more time to get those rules right.

---

## Discussion

_Which workload made your application feel slow while the benchmark still looked fine?_

{{< giscus >}}
