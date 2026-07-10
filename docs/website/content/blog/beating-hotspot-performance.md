---
title: "How We Beat HotSpot Performance (By Cheating, But Not Like That)"
slug: beating-hotspot-performance
url: /blog/beating-hotspot-performance/
date: '2026-07-10'
author: Shai Almog
description: "ParparVM went from 4.21x slower than warmed Java 25 to geomean parity, with six of ten benchmarks at or below HotSpot. What we changed in the generated C, what HotSpot still wins, and why a JIT beats hand-tuned C on some workloads."
feed_html: '<img src="https://www.codenameone.com/blog/beating-hotspot-performance.jpg" alt="ParparVM vs HotSpot performance" /> ParparVM went from 4.21x slower than warmed Java 25 to geomean parity. What we changed in the generated C, and what HotSpot still wins.'
series: ["release-2026-07-10"]
---

![How We Beat HotSpot Performance (By Cheating, But Not Like That)](/blog/beating-hotspot-performance.jpg)

No, we didn't cheat in the benchmark. At least I hope we didn't. Every optimization in this story was gated on bit identical output checksums against HotSpot, and the harness refuses to print a ratio when a checksum differs. If anything, this post is about how good HotSpot actually is. We tilted the table in our favor in every way we could, we hand tuned C code, and we still only beat it on some benchmarks. Getting there was a genuine struggle. If you want to understand the nuts and bolts of what your Java code costs, and the tradeoffs each runtime picks, I hope this is a good read.

The short version: [PR #5327](https://github.com/codenameone/CodenameOne/pull/5327) takes ParparVM, the AOT VM that compiles your Java bytecode to C for iOS and other targets, from 4.21x slower than warmed Java 25 to geomean 1.00x parity across a ten benchmark suite, with six of the ten at or below HotSpot.

But before we get to that, a few announcements.

## Before You Update

The optimizations below landed this week. They were tested obsessively. Every commit was gated on bit identical output vs HotSpot, plus torture suites for maps, string builders, threads and GC stress, in both cooperative and forced signal stop modes, under both clang and gcc. But this is a deep change to code generation, allocation and collection. Like any change of this scale, there's risk.

If a build misbehaves, pin to the previous release with [versioned builds](https://www.codenameone.com/blog/versioned-builds-master/) and let us know through the usual channels. That's exactly the case versioned builds exist for.

There's a lot more shipping this week beyond the VM work; the bottom of this post links to the daily posts covering it.

## The Starting Point

Client VMs are a different beast. The joke around here is that I built ParparVM in two weeks and Steve spent the next three years fixing bugs. When I built it, throughput wasn't a priority at all. I was aiming for simplicity, reliability and consistency. What actually matters for client performance is startup time, memory footprint, low latency and fast native access. Ninety percent of client code time should be spent in rendering and IO.

Occasionally a customer would complain about performance, we'd add an optimization for that specific case, and we'd move along. We profiled common use cases, they were fine, and we never treated VM throughput as a problem. Comparing against HotSpot wasn't even on the table. You can't out-optimize a JIT, and we didn't run on the same platforms anyway. Now that we have desktop ports, people are porting heavier workloads and running direct comparisons on the same hardware. I assumed we were 2x or 3x slower than warmed HotSpot, ignoring startup.

I was really, really off. Here's the starting point, measured on an Apple M2 against OpenJDK 25 with full warmup, best of 25 samples, identical checksums on both runtimes:

| Benchmark | What it stresses | ParparVM vs Java 25 |
|---|---|---:|
| hashMapChurn | HashMap put/get with autoboxing | **36.2x slower** |
| stringBuilding | StringBuilder append and hash | **22.7x slower** |
| objectAllocation | Allocation and GC churn | **19.6x slower** |
| recursion | Deep call chains (fib) | **7.6x slower** |
| arraySequential | Fill and reduce | **3.7x slower** |
| quicksort | Compare, swap, recurse | **1.8x slower** |
| longArithmetic | 64-bit ALU chain | **1.5x slower** |
| intArithmetic | 32-bit ALU chain | **1.3x slower** |
| mathTranscendental | sqrt/sin/cos | **1.1x slower** |
| arrayRandom | Cache-miss gather | **1.0x** |

Geomean: 4.21x slower. This was really bad.

The one consolation was the other half of the report. Runtime memory floor: 2.4 MB vs HotSpot's roughly 40 MB. Startup: effectively zero. Those are the numbers client apps live and die by, and they're why the architecture looks the way it does. But 36x on a HashMap is not a tradeoff, it's a bug you haven't found yet.

Spoiler: after everything below, this is where the same suite landed.

| Benchmark | Final ratio | | Benchmark | Final ratio |
|---|---:|---|---|---:|
| stringBuilding | **0.67x** | | arrayRandom | 0.96x |
| arraySequential | **0.82x** | | intArithmetic | 1.07x |
| quicksort | **0.92x** | | longArithmetic | 1.12x |
| hashMapChurn | **0.95x** | | objectAllocation | 1.19x |
| mathTranscendental | **0.96x** | | recursion | 1.60x |

Geomean 1.00x. Below 1.0 means we beat warmed HotSpot C2. Same Java source, same checksums, best of 5 interleaved runs. The interleaving isn't decoration: early on, sequential A-then-B timing on the M2 carried a 10 to 15% thermal bias that made one change look 1.5x faster when it was worth 3%, so the harness alternates the two VMs within a run.

One methodology note before you ask: the comparison runs on macOS because HotSpot doesn't run on iOS. The same generated C ships to every Apple target, so the codegen wins carry over to devices where the JIT can't follow.

{{< mermaid >}}
xychart-beta
    title "ParparVM time vs warmed Java 25 (1.0 = HotSpot, lower is better)"
    x-axis [hashMap, stringB, objAlloc, recurse, arrSeq, qsort, longA, intA, mathT, arrRand]
    y-axis "ratio (before bars truncated at 8)" 0 --> 8
    bar [8, 8, 8, 7.6, 3.7, 1.8, 1.5, 1.3, 1.1, 1.0]
    bar [0.95, 0.67, 1.19, 1.6, 0.82, 0.92, 1.12, 1.07, 0.96, 0.96]
{{< /mermaid >}}

The light bars are the starting ratios (the three worst are actually 36x, 23x and 20x, truncated so the chart stays readable). The dark bars inside them are where the branch landed.

## How Did We "Cheat"?

The one advantage we have over HotSpot is that we're not Java. Not really. We bill ourselves as a tool that lets Java developers ship their Java apps to mobile devices. See what we did there? You write Java code, so it's a Java app. But we're not really Java in some major ways, and that gives us freedom HotSpot doesn't have.

We compile a closed world. There's no dynamic class loading, so we know every class that will ever exist. Reflection is minimal, so a method nobody calls is a method we can delete, and a virtual call with exactly one reachable implementation is a direct call. The API is smaller, so there's less legacy behavior to preserve. And we can play fast and loose with some nuanced VM behaviors that HotSpot must keep exactly (more on `Integer` identity below).

Without this cheating we would have lost every benchmark in the group. HotSpot carries a quarter century of engineering and it spends none of it on our constraints. Even with these structural advantages, we had to spit blood to get to a competitive point.

## What The C Compiler Actually Sees

ParparVM translates bytecode to C and lets clang optimize it. So the whole game is: how much does the generated C look like the C a human would write? The answer, at the start, was "not at all". Every Java method pushed a GC visible frame of type tagged slots and routed every intermediate value through it:

```c
/* BEFORE: one heap-visible frame per call, every value tagged and in memory */
JAVA_LONG fib(CODENAME_ONE_THREAD_STATE, JAVA_INT n) {
    stack = pushFrameOnThreadStack(threadStateData, locals=2, stack=4);
    memset(stack, 0, 6 * sizeof(elementStruct));      /* on EVERY call */
    locals[0].type = CN1_TYPE_INT; locals[0].data.i = n;
    (*SP).type = CN1_TYPE_INT; (*SP).data.i = 2; SP++;
    if (locals[0].data.i < SP[-1].data.i) ...          /* compare via memory */
    releaseForReturn(threadStateData, ...);
}
```

That frame is how the GC finds live objects. It's also why clang couldn't keep anything in a register. Methods the translator can now prove safe (no try/catch, no synchronization, object roots covered by the native stack scan) compile to the C you'd write by hand:

```c
/* AFTER: locals are C locals, so they become registers */
JAVA_LONG fib(CODENAME_ONE_THREAD_STATE, JAVA_INT n) {
    JAVA_INT ilocals_0_ = n;
    CN1_FRAMELESS_SOE_GUARD(0);        /* stack-overflow check, nothing else */
    if (ilocals_0_ < 2) return ilocals_0_;
    return fib(threadStateData, ilocals_0_ - 1) + fib(threadStateData, ilocals_0_ - 2);
}
```

Between this and its follow-ups, recursion went from 7.6x to 1.6x. More importantly, frameless codegen feeds every other optimization, because once values live in registers the rest of clang's optimizer wakes up.

### Why We Still Lose Recursion

The remaining 1.6x on recursion is HotSpot's speculative inlining, and we accepted it. A JIT watches the program run, notices that `fib` calls `fib`, inlines it into itself several levels deep, and keeps a deoptimization escape hatch in case its bet goes wrong. An ahead-of-time compiler doesn't get to gamble. It has to emit code that's correct for every possible execution, so a real call remains a real call. This is the structural advantage of a JIT and no amount of hand tuning closes it. If your workload is deep recursive call chains, HotSpot is simply the better machine for it.

### The Bounds Check That Poisoned Every Loop

Java requires an index check on every array access. The check itself is cheap. What killed us was the shape of the failure path. Our bounds check helper threw the exception and then returned a dummy value, so the "failed" path rejoined the loop. That means every loop iteration contained a reachable function call, and clang must assume a call can modify any memory. So it reloaded the array pointer and the array length from memory on every single pass:

```c
/* BEFORE: while (a[i] < pivot) i++;  -- quicksort's scan loop */
label_scan:
    if (cn1_array_element_int(ts, locals[0].data.o, i) >= pivot) goto done;
    /* on bounds failure: throwException(...); return 0; ...and REJOIN.
       A call is reachable on every iteration, so clang reloads
       array->data AND array->length every pass: 3 loads per element. */
    i++;
    goto label_scan;
```

The fix is to make the failure path diverge. Throw and return from the method, the same way the stack overflow guard works. Now no cycle of the loop contains a call, and clang hoists the loads out of the loop:

```c
/* AFTER: the throw path leaves the function; the loop is load/compare/branch */
label_scan:
    { JAVA_OBJECT a = locals[0].data.o; JAVA_INT idx = i;
      CN1_ARRAY_CHECK_DIVERGE(a, idx, );   /* null/oob -> throw; return */
      if (((JAVA_ARRAY_INT*)(*(JAVA_ARRAY)a).data)[idx] >= pivot) goto done; }
    i++;
    goto label_scan;
```

We measured the check itself against a pure C control at identical flags: raw C runs the sort in 91ms, C with diverging bounds checks in 98ms. So the safety Java mandates costs about 8% when the surrounding code is right. With the fix, the benchmark sort dropped from 216ms to 164ms, vs HotSpot's 197ms. Quicksort ended at 0.92x, below HotSpot, while keeping every check.

### Where HotSpot Beat Our Hand-Tuned C

Here's the part that humbled us. On intArithmetic and longArithmetic we wrote plain C controls, no VM, no GC, just the loop, compiled with the same clang flags. The generated ParparVM code ran at exact parity with the hand written C: 94.0ms vs 93.7ms, and 59.7ms vs 59.3ms. Zero VM overhead.

HotSpot still won, 1.07x and 1.12x. The residual is C2 reassociating the loop-carried dependency chain better than clang schedules it. On a tight arithmetic loop, HotSpot C2 generates better machine code than clang -O3 given identical semantics. The JIT sees the actual hot loop and its actual dependency graph and optimizes exactly that. We declared those benchmarks done, because when the gap between you and HotSpot is the same as the gap between clang and HotSpot, the VM is no longer the story.

There's one footnote worth stealing for any C-generating project: Java integer semantics require `-fwrapv -fno-strict-aliasing`. Without `-fwrapv`, clang -O3 provably miscompiles overflowing accumulation loops. Our checksum gate caught it, off by exactly 2^32 per overflow.

## The GC, In Plain Terms

Most of the worst starting numbers, the 20x to 36x ones, weren't about code generation at all. They were about allocation and collection. To explain what changed, here's the collector in simple terms.

ParparVM's GC never stops the world. Your app's threads keep running while a background collector thread walks the object graph and marks everything reachable, then sweeps what wasn't marked. The threads cooperate: each thread either checks in at safe points, or the collector briefly interrupts it with a signal, captures its registers and stack for scanning, and lets it continue.

{{< mermaid >}}
sequenceDiagram
    participant UI as App thread (EDT)
    participant GC as Collector thread
    UI->>UI: allocate, render, respond
    GC->>GC: mark reachable objects (concurrent)
    GC-->>UI: brief signal: snapshot registers + stack
    UI->>UI: keeps running
    GC->>GC: sweep unmarked objects
    Note over UI,GC: no stop-the-world pause, no frame drop
{{< /mermaid >}}

Why build it this way? Because on a client, pause time is the only GC metric users can feel. A 30ms collection pause during a scroll animation is two dropped frames, and users see it. A concurrent collector trades throughput for the guarantee that the animation never stutters.

### Why We Don't Want A Generational GC

The standard server answer is a generational collector: allocate new objects in a nursery, collect it often, copy the survivors out. It's a throughput machine, and for servers it's the right call. We tried a nursery on this branch. objectAllocation improved, and hashMapChurn got worse, 32x to 43x. Copying collectors pay for survivors, and a map that holds its entries makes everything survive. UI workloads look like that constantly: the object graph behind a form mostly survives, frame after frame.

Generational collectors also move objects, which is a problem for us in two ways. Native code holds pointers into our heap, and a compacting collector would need to fix those up or pin everything native can see. And copying needs headroom, roughly double the live set during collection, which is exactly the memory a 2.4 MB footprint client doesn't have. Our collector never moves an object. Peak memory under heavy churn on this branch: 290 to 390 MB, versus 508 MB for the JVM on the same workload. The nursery experiment is off by default and stays off.

### The Bug That Made It Look Worse Than It Was

While benchmarking we found that master's GC had a genuine trigger bug in production: the "allocated since last GC" counter was a 32-bit int counting bytes. Workloads that allocate gigabytes per cycle wrapped it negative, the "am I allocating fast?" check answered no, and the collector slept its 30 second idle wait while dead pages piled into the gigabytes. That was the source of master's 1.4 to 2.1 GB peak memory on churn. One int-to-long fix later, the same workload peaks below the JVM.

This is the quiet argument for benchmarking at all. The 36x headline number led us to two real production bugs that had nothing to do with benchmarks. The second was a sibling of the first: workloads that allocate only inside native code skipped the collection trigger entirely.

### Don't Park The Render Thread

One more GC change matters for real apps rather than benchmarks. A concurrent collector needs backpressure: if a thread allocates faster than the collector frees, something must slow it down. The old rule parked any fast-allocating thread in a sleep loop once a fixed 72 MB of uncollected garbage piled up. On an allocation-heavy render (decoding vector map tiles) that meant the UI thread spent most of every GC cycle parked, and the render serialized behind the collector. Measured cost: 20% of wall time, which made it the single biggest tunable in the whole investigation. The mark and sweep passes themselves overlap the app and measure roughly zero.

The cap is now dynamic. The baseline is an eighth of available RAM, and threads the VM knows are high throughput, like the UI thread, get up to half of available RAM of headroom so they keep rendering while the collector catches up. On the vector map workload this branch renders 2.1x faster than master.

## Two Bugs That Earned Their Keep

Chasing benchmarks pays for itself in the bugs it flushes out, and two of them deserve a paragraph each.

The first: on Apple platforms, `setjmp` and `longjmp` save and restore the caller's signal mask, and each side is a `sigprocmask` syscall. Every Java try block entry compiles to a `setjmp` in our codegen. Put a try block inside a hot loop and you're paying a kernel round trip per iteration; on the vector map workload that was 19% of the app's own CPU samples. The VM never changes the signal mask, so Apple targets now use `_setjmp` and `_longjmp`, the variants that skip the mask. Every try/catch on iOS, tvOS, watchOS and macOS just got cheaper.

The second one had been latent for years. A variable assigned after `setjmp` and read after `longjmp` is indeterminate per the C standard. Our exception handler read exactly such a variable. Because clang happens to spill it to memory, every clang-compiled binary we ever shipped worked by luck. gcc keeps it in a register, which `longjmp` rolls back, so after any caught exception the thread's frame bookkeeping was wrong and new frames were allocated on top of live locals. Our Alpine Linux CI job, the only gcc-compiled platform in the matrix, hung deterministically and led us straight to it. Two `volatile` qualifiers fixed a bug that plausibly affected every gcc-built Codename One Linux app that ever caught an exception.

Neither bug is a performance optimization. Both came out of building a benchmark harness strict enough to notice when anything changes.

## A Poor Man's Valhalla

The hashMapChurn benchmark spends its life autoboxing, `map.put(i, i * 2)` style code that allocates an `Integer` per call on a standard JVM (outside the -128 to 127 cache). Project Valhalla is Java's decade-long effort to make such values cheap. We don't have to wait for it, because we control the whole stack.

On 64-bit targets, `Integer.valueOf()` no longer allocates anything. It returns a tagged pointer: the low bit is 1, the integer value lives in the high bits. Real object pointers are aligned, so their low bit is always 0, and the two can never collide:

```
real object:    0x0000600001a2c3f0   (low bit 0, points at a header)
tagged Integer: 0x000000000000001f   (low bit 1, value = 0xf = 15)
```

The GC ignores tagged values entirely, dispatch substitutes `Integer`'s class when it sees one, and unboxing is a shift. Boxing became free: the PR's A/B measures hashMapChurn at 2.8x with tagging disabled vs 0.97x with it on. The embarrassing discovery: an early version of this existed behind an opt-in flag that no shipping configuration ever set. Writing the benchmark scripts is what exposed that deployed apps never had it. It's now on by default, with `-DCN1_DISABLE_TAGGED_INT` as the escape hatch.

There's a semantic price, and it's the same one Valhalla asks: `Integer` loses identity. Two boxes holding the same value are now literally the same value, so `==` on boxes behaves like the JDK's small-value cache extended to the whole range, and `synchronized (someInteger)` cannot mean anything. The JDK itself has deprecated wrapper constructors and warns against locking on value-based classes for years. Rather than let that fail silently at runtime, [PR #5338](https://github.com/codenameone/CodenameOne/pull/5338) makes the build reject `synchronized` on a primitive wrapper at compile time, with a pointer toward a dedicated lock object. If your code locks on an `Integer`, it was already broken on modern JDKs in spirit. Now it's broken loudly, before it ships.

## Objects That Stopped Existing

The rest of the allocation story is three related changes, each with the same theme: the fastest object is the one you never allocate.

**Fused objects.** `new String(...)` used to be two heap objects, the `String` and its `char[]`. They're now one block, one allocation, one GC slot, no pointer hop between them:

```
BEFORE   [ String header | fields ] --> [ char[] header | c0 c1 c2 ... ]
AFTER    [ String header | fields | char[] header | c0 c1 c2 ... ]
```

This ships as `@Fused`, applied internally to `String` and `StringBuilder`, and usable on your own classes that wrap a primitive buffer.

**Escape analysis.** javac compiles `"item-" + i + "/" + n` into a `StringBuilder` chain. A control flow walk proves the builder never escapes the expression, so the builder and its buffer now live on the C stack. The only heap allocation in the whole concatenation is the final `String`. Combined with the rest, stringBuilding landed at 0.67x, finishing in two thirds of HotSpot's time, and this benchmark was rebuilt to be fair to HotSpot first (the original shape let HotSpot's own escape analysis delete the String entirely, so we made both VMs materialize every string).

**Allocation itself.** `new` used to be malloc, a full memset to zero, and an O(n) search for a free tracking slot under a lock. It's now a pointer bump into a size-class page, and the constructor's own field writes replace the zeroing, with the class pointer stored last so the concurrent collector never sees a half-built object. objectAllocation went from 19.6x to 1.19x.

## What We Still Owe

An honest list, because a benchmark suite is ten workloads and the world is bigger:

- **Recursion is 1.6x** and will stay roughly there. Speculative inlining is the JIT's home turf.
- **Tight arithmetic is 1.07x to 1.12x**, and that's clang vs C2, not us vs HotSpot.
- **Cross-file inlining is our next frontier.** The vector map workload is 2.1x faster than master but still trails warmed HotSpot 5x, and the profile says it's a per-byte `readByte()` call chain that HotSpot inlines across compilation units and we don't yet.
- **Warmup is the flip side.** Every HotSpot number here is after full warmup. Cold, the comparison inverts, and client apps live mostly in the cold and warm phases. We start at full speed with a 2.4 MB floor; HotSpot needs ~40 MB and a few thousand iterations to become the machine we benchmarked against.
- **We haven't raced GraalVM native-image yet.** It's the natural AOT peer and it's on the list. This round was about closing the gap to the ceiling, which is warmed C2.

All of the machinery above has a price, and it's small enough to state exactly: the benchmark app's binary grew from 434 KB to 451 KB, a 3.8% increase. Those 17 KB buy the inlined fast paths, the compact HashMap and the escape analysis. Memory moved the other direction: peak use under allocation churn dropped from gigabytes (the trigger bug) to below the JVM's on the same workload, and the no-op floor stayed at 2.4 MB.

The whole suite ships in the repo under `vm/benchmarks`, including the torture tests and the checksum gate. If you think we cheated after all, `vm/benchmarks/run-benchmark.sh` will happily referee:

```bash
export JDK_8_HOME=/path/to/jdk8
export BENCH_JAVA=/path/to/jdk25/bin/java
vm/benchmarks/run-benchmark.sh   # interleaved best-of-5, ratio table + geomean
vm/benchmarks/run-gauntlet.sh    # correctness: byte-identical output + GC stress
```

## The Rest Of This Week

The performance work is the headline, but the week is bigger than one PR:

- **Saturday.** {{< post-link path="/blog/standalone-certificate-wizard" text="The certificate wizard is now a standalone tool with a different approach to Apple login" >}}. It should end the login breakage of the old wizard. PR [#5339](https://github.com/codenameone/CodenameOne/pull/5339).
- **Sunday.** {{< post-link path="/blog/ar-vr-support-simulation" text="AR and VR support, including a simulated AR room you can debug in the simulator" >}}. PR [#5335](https://github.com/codenameone/CodenameOne/pull/5335).
- **Monday.** {{< post-link path="/blog/automated-store-submissions" text="Automated store submissions with your listing as code, including Huawei AppGallery" >}}. The same post covers organization accounts and self service account deletion in the build cloud. PR [#5353](https://github.com/codenameone/CodenameOne/pull/5353).

## What I Got Wrong

I went into this convinced a small AOT VM can't play in HotSpot's arena, and the first table seemed to prove it. The real lesson cuts both ways. HotSpot is a marvel: it beat our hand tuned C on arithmetic and it will out-inline us on recursion forever. But most of our 4.21x gap wasn't HotSpot being fast. It was us paying for costs a closed world VM never needed to pay.

We kept the things clients actually need, instant startup and a small footprint with no pauses, and bought back the throughput we'd been leaving on the table. The +17 KB of binary and the geomean 1.00x say the two goals weren't in conflict after all.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
