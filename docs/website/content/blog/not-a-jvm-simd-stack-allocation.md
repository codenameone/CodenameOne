---
title: "We're Not a JVM, and That's How We Got SIMD and Stack Allocation into Java"
slug: not-a-jvm-simd-stack-allocation
url: /blog/not-a-jvm-simd-stack-allocation/
date: '2026-06-24'
author: Shai Almog
description: Java hides the machine on purpose, so you get no aligned memory, no stack-allocated arrays, and until recently no SIMD. Because Codename One compiles your bytecode to C instead of running on a JVM, we handed all three back to ourselves, and the benchmarks showed exactly where hand-written SIMD wins and where the C compiler already won.
syndicate_force: ["dzone", "foojay"]
feed_html: '<img src="https://www.codenameone.com/blog/not-a-jvm-simd-stack-allocation.jpg" alt="We are not a JVM, and that is how we got SIMD and stack allocation into Java" /> Java hides the machine on purpose. Because Codename One compiles to C instead of running on a JVM, we gave Java aligned memory, stack allocation, and portable SIMD, and measured exactly where hand-written SIMD wins and where the compiler already won.'
---

![We're not a JVM, and that's how we got SIMD and stack allocation into Java](/blog/not-a-jvm-simd-stack-allocation.jpg)

Can you allocate a Java array on the stack? In a normal JVM the answer is no, and no flag will change it. We do it anyway, in production, on iOS. While we were down there we also gave Java 16-byte-aligned memory and hand-written SIMD, and then got a sharp reminder of how good an optimizing C compiler already is when you measure your handcrafted code against it instead of against nothing. Three surprises, one explanation: Codename One is not a JVM.

Java goes to real trouble to hide the machine from you. You cannot put an array on the stack, you cannot demand aligned memory, and until recently you could not write a SIMD instruction at all. Most of the time that is exactly right, and it is what keeps the garbage collector safe and your code portable. But Codename One does not run on a JVM. It compiles your bytecode to C, and then to a native binary, and that one difference let us hand all three back to ourselves, without waiting for Java to ship them.

## Why Java doesn't give you this

The JVM abstracts memory layout on purpose. The collector moves objects, so you get no alignment guarantee. Arrays are heap objects, so there is no stack allocation. The bytecode has no vector ops. Java's two long-running answers are Project Panama, whose Vector API is still incubating, and Project Valhalla, whose value types and flattening are arriving in pieces. Both are the platform reaching back down to the metal it spent two decades hiding. We needed that metal now, in image blends and codec loops, the hot paths where a phone actually spends its battery.

## Dropping below Java is just emitting different C

Because the output is C, "go to the metal" is not a language fight. It is a code-generation detail. The portable surface is one class, `com.codename1.util.Simd`, with a pure-Java fallback so the same code runs anywhere, and native implementations wired in per platform through our `@Concrete` annotation, which selects `IOSSimd`, `WindowsSimd`, or `LinuxSimd` at build time.

{{< mermaid >}}
flowchart TB
    J["Java / Kotlin"] --> BC["bytecode"]
    BC --> CC["ParparVM<br/>(translate to C)"]
    CC --> NB["native binary"]
    NB --> A["aligned arrays<br/>(allocByte)"]
    NB --> S["stack allocation<br/>(allocaByte)"]
    NB --> V["SIMD intrinsics<br/>(per platform)"]
{{< /mermaid >}}

Two of the three pieces are about *layout*, which is the Valhalla-shaped half of the story:

- **Aligned allocation.** `allocByte`, `allocInt`, and `allocFloat` return blocks the native layer can load and store aligned. A plain `new byte[]` cannot promise alignment, so it cannot be a SIMD target.
- **Stack allocation.** `allocaByte` lowers, on ParparVM, to a stack-backed array, with an automatic heap fallback when it is too large to sit safely on the per-thread stack. It is a poor man's value-on-the-stack, and it is deprecated by design: method-local only, contents undefined until written, footguns in every direction. We ship it because for short-lived scratch buffers it is free, and "free" shows up in a paint loop.

```java
Simd s = Simd.get();
byte[] pixels = s.allocByte(width * height * 4); // aligned, not new byte[]
// a fused alpha blend then runs over the whole array in one native pass
```

## But doesn't escape analysis already do this?

It is the first thing a server-side Java developer asks about the stack-allocation claim, and the answer is the interesting part. HotSpot's escape analysis can remove some allocations through scalar replacement: when the JIT proves an object never escapes its method, it keeps the fields in registers instead of allocating on the heap, and it will even do this for small fixed-size arrays (up to `EliminateAllocationArraySizeLimit`, 64 elements by default). That is real, and on hot server code it is excellent.

Two things make it the wrong tool here. First, scalar replacement is not a buffer. It shatters an allocation into individual values; it does not hand you a contiguous, aligned block to pass to a SIMD kernel, which is exactly what we need. Second, and the bigger one for us, it depends on a JIT that has warmed up, and client code is cold. A mobile screen runs a method a handful of times, not the thousands of iterations the JIT wants before it compiles and analyzes anything, so the optimization that quietly saves you on a server never fires. On iOS there is no JIT to wait for at all, because we compile ahead of time to C.

This is the part of client-side performance that server intuition gets backwards. On the server your code goes hot and the JIT eventually does remarkable things, escape analysis and autovectorization included. On the client most code stays cold for its whole life: you get the interpreter, or on iOS the ahead-of-time baseline, and none of the profile-guided work. If you want aligned buffers, stack scratch space, and SIMD on cold code, you have to ask for them outright. That is what these APIs are.

## The interesting part is where we did less

The easy version of this post is "we hand-wrote SIMD, it is faster, the end." That version is wrong. Hand-written SIMD is not a universal win, so we gate it per kernel and per platform, and the most useful result was learning where to stand down.

{{< mermaid >}}
flowchart TD
    K{"which kernel?"} -- "fused whole-array" --> F{"isSupported()?"}
    F -- "yes" --> FW["native SIMD<br/>(x86-64 and NEON)"]
    K -- "byte-shuffle codec" --> B{"byte-shuffle<br/>accelerated?"}
    B -- "NEON (iOS, Win-on-Arm)" --> BW["native SIMD<br/>(big win)"]
    B -- "x86-64" --> BS["stay scalar<br/>(compiler vectorizes it)"]
{{< /mermaid >}}

The **fused whole-array kernels** win everywhere a native SIMD unit exists. A single pass over the buffer, no per-element call overhead. These are gated on `isSupported()`. Timing the native kernel against the equivalent Java scalar loop, 64K elements per pass, 300 passes:

| Kernel | Windows x86-64 (SSE2) | Mac arm64 (NEON) | iOS arm64 (NEON) |
| --- | --- | --- | --- |
| int add | 12.6x (76ms to 6ms) | 7.1x (86ms to 12ms) | 22.5x (135ms to 6ms) |
| float multiply | 17.7x (71ms to 4ms) | 24.5x (49ms to 2ms) | 46.0x (138ms to 3ms) |

The **chained byte-shuffle codec**, our Base64 path with interleaved pack and unpack, byte shifts, and table lookups, is a different story. On NEON it is dramatically faster. On x86-64 it is not, so we leave it scalar on purpose. That is what `isByteShuffleAccelerated()` reports, kept separate from `isSupported()`:

| Base64, 8KB payload, 6000 iterations | Encode | Decode |
| --- | --- | --- |
| iOS arm64 (NEON), explicit SIMD | 86.6% faster | 57.4% faster |
| Mac arm64 (NEON), explicit SIMD | 81.3% faster | 74.9% faster |
| x86-64 | explicit SIMD gated off | explicit SIMD gated off |

On x86-64 the compiler's `/O2` autovectorizer already matches the scalar loop, and SSE2 has no 3-way interleave to exploit, so shipping the intrinsics would add maintenance for nothing. The right move was to delete them and trust the compiler.

The image kernels (alpha and mask blends, 100 iterations) mostly win, and one does not, which is the kind of thing you only learn by measuring:

| Image op (SIMD off to on) | Windows x86-64 | Mac arm64 | iOS arm64 |
| --- | --- | --- | --- |
| createMask | 65.7% faster | 91.7% faster | 96.9% faster |
| modifyAlpha | 56.4% faster | 19.0% faster | 31.8% faster |
| applyMask | 47.3% faster | 23.3% faster | 7.2% **slower** |

On iOS, `applyMask` is slightly slower with the native kernel on, so it stays off there. We do not get to assume; each kernel earns its place on each architecture.

## The real lesson: measure against the optimizing compiler

Here is the part worth your time, because it is the real lesson of the whole exercise. For a while our iOS and Mac test builds compiled the benchmark in Debug, which on Xcode means `-O0`: the scalar baseline was never optimized at all. So our hand-written SIMD was racing unoptimized code, winning by a wide margin, and we believed that margin. Windows was the reality check. It had been building the benchmark in Release the whole time, `/O2`, with the scalar loop fully autovectorized by the compiler, and against that baseline the margins were far smaller. For the Base64 byte-shuffle codec on x86-64 they nearly closed: the compiler was already doing what our intrinsics did, so the hand-written path was not worth keeping, and we gate it to scalar there.

That is the real reason the Base64 byte codec ships scalar on x86-64. It is gated off not because SIMD is slow, but because the autovectorizer already matches it and SSE2 has no 3-way interleave left for us to add on top. The fused arithmetic and image kernels are a different shape, with an edge the compiler does not find on its own, so they still beat `/O2` and stay on. The durable takeaway: measure your handcrafted code against an optimizing compiler, not against naive scalar, before you trust it.

So we fixed the measurement. The commit is titled, plainly, "optimize the translated C at -O2" ([#5209](https://github.com/codenameone/CodenameOne/pull/5209)), and it makes all three ports compile the scalar baseline the way the shipping app does. Every number above is from those builds on current `master`: Windows [#27900572303](https://github.com/codenameone/CodenameOne/actions/runs/27900572303), Mac [#27904116497](https://github.com/codenameone/CodenameOne/actions/runs/27904116497), iOS [#27904116502](https://github.com/codenameone/CodenameOne/actions/runs/27904116502). We do not capture the same stats on Linux yet, so it is absent from the tables rather than guessed. And we now benchmark the build we ship, not the build that is convenient.

## The point

The JVM hides the machine, and most days you want it to. But "Java" and "a JVM" are not the same thing. Because we compile to C, we got to decide, per platform and per loop, when to drop below Java and when to let the C compiler do the job it does better than we do. That is not a workaround for not being Java. It is the part of not being a JVM that we would keep.
