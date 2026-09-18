---
title: "The Java Compiler That Became Its Own Test Case"
slug: parparvm-compiles-itself
url: /blog/parparvm-compiles-itself/
date: '2026-09-24'
author: Shai Almog
description: "ParparVM can now run its own translator as a native executable. Self-hosting exposed runtime bugs, reproducible-build problems, and performance gaps against HotSpot."
feed_html: '<img src="https://www.codenameone.com/blog/parparvm-compiles-itself.jpg" alt="ParparVM compiles itself" /> Self-hosting turns ParparVM into a demanding test of its own runtime and generated output.'
series: ["release-2026-09-18"]
---

![ParparVM compiles itself](/blog/parparvm-compiles-itself.jpg)

Our Java-to-C translator is written in Java. That made it an obvious candidate for ParparVM, and a very effective way to discover what our runtime hadn't been asked to do yet.

ParparVM is now self-hosting: it can compile its own translator into a native executable, and that executable can translate Java programs. It's an important step toward building Codename One with Codename One. It also gives us a roughly 37,000-line program that regularly exercises our collections, strings, file handling, exceptions, and collector together.

[This week's backend announcement](/blog/why-another-java-server/) asks the runtime to handle many small requests. The translator asks it to consume a large body of bytecode and construct a correspondingly large output. Those workloads reveal different problems, which is exactly why we want both.

## Make the compiler prove what it emitted

{{< mermaid >}}
flowchart TD
    Source[Translator bytecode plus ASM] --> JVM[Translator running on HotSpot]
    JVM --> NativeBuild[Generated C compiled to native translator]
    Input[Same application bytecode] --> JVMRun[JVM translator]
    Input --> NativeRun[Native translator]
    NativeBuild --> NativeRun
    JVMRun --> C1[Generated C tree A]
    NativeRun --> C2[Generated C tree B]
    C1 --> Compare[Compare output and run resulting programs]
    C2 --> Compare
{{< /mermaid >}}

The self-hosted build covers the `clean`, `ios`, and `macos` translation paths. It substitutes build-time stubs for the JavaScript generator and a few archive or compression helpers that require APIs outside this source set. The [self-hosting README](https://github.com/codenameone/CodenameOne/blob/2697dcfa2f0425170efd08655243032571b65869/vm/selfhost/README.md) lists them.

That still leaves the translator itself and ASM running on ParparVM. It can consume the same classes as the JVM translator, emit C, and let us compare the two trees.

The verification scripts distinguish repeated native output from agreement with the JVM. The recorded JavaAPI-sized corpus had 245 of 247 files byte-identical; the remaining pair concerned different dead-code elimination of HashMap methods. Both resulting programs compiled and ran with matching output. That discrepancy is a concrete comparison target, not something a “self-hosted” label should hide.

## A minus sign is a bad character in a C label

One of the first failures came from ASM's label names. They incorporated object identity hashes. On HotSpot, that had mostly looked like an incidental naming choice. On ParparVM, a negative value could produce output such as:

```c
label_L-180306432001:
```

C reads the minus sign as an operator. A method with a valid Java exception handler had become invalid C.

Generated identifiers now use deterministic naming instead of inheriting identity-based strings. Local-variable declarations also needed stable ordering: iterating a `HashSet` had made the emitted declaration order depend on the runtime.

These are useful bugs to find even if you never run the native translator. A reproducible compiler should generate the same program without depending on where its own objects happened to be allocated.

## Primitive types must remain distinct keys

Another problem was in the runtime's wrapper `TYPE` fields. Values such as `Integer.TYPE` were null. The translator uses primitive classes as map keys when choosing C types, so distinct entries collapsed onto the same null key.

This little table explains the failure:

| Java key | Intended meaning | Broken runtime value |
| --- | --- | --- |
| `Integer.TYPE` | Primitive `int` | `null` |
| `Long.TYPE` | Primitive `long` | `null` |
| `Double.TYPE` | Primitive `double` | `null` |

The map behaved consistently with the values it received. The values were wrong. Running the whole translator made that defect visible in a place where a small collection test might never use primitive-class keys.

## The list that kept getting more expensive

After correctness came profiling. The constant pool accumulated roughly 200,000 strings, and insertion searched the existing list with `indexOf()`. Every new distinct string made future insertions more expensive.

The fix keeps the list for stable output indices and adds a map for lookup. The source now has this shape:

```java
Integer existing = constantPoolIndex.get(s);
if (existing != null) {
    return existing.intValue();
}
int index = constantPool.size();
constantPool.add(s);
constantPoolIndex.put(s, Integer.valueOf(index));
return index;
```

The list still determines the emitted index. The map answers whether the string is already there. That's a change the output comparison can check directly: faster lookup must not reorder the constant pool.

The collector also had a pacing problem. An allocation-heavy process with a large live set was being held behind a growth threshold chosen without enough regard for the machine's available memory. Profiling found the mutator asleep rather than doing useful allocation or collection work. The fix adjusts the unbounded-process path while preserving explicit process-budget enforcement.

## HotSpot gives us the next target

The recorded release-build comparison translated about 570 classes containing ASM and the translator itself on a 16-core Mac with 64 GB of RAM:

| Runtime | Wall time | Peak footprint |
| --- | ---: | ---: |
| Native ParparVM translator | 1.84 s | 1,443 MB |
| JDK 25 | 1.56 s | 516 MB |
| JDK 8 | 2.27 s | 502 MB |

![Recorded self-hosting translation time and peak memory](/blog/selfhost-runtime-comparison.svg)

*Recorded self-hosting workload from the repository, using the native release build with `-O3` and ThinLTO. This is compiler throughput and peak footprint, not the HTTP workload in the opening article.*

The native translator had improved from roughly six times slower, but JDK 25 still led this recorded wall-clock result and used much less memory. The subsequent investigation found avoidable collection objects and retained strings worth attacking. Those gaps give us specific runtime work to do.

I like this milestone partly because it refuses to let us hide behind a friendly benchmark. The compiler needs the answer to be correct, and it needs enough memory to construct it. A fast server loop doesn't make those requirements disappear.

## Run the comparison

From `vm/selfhost`, with `JDK_8_HOME` pointing to a working JDK 8 and Maven using that JDK:

```bash
./build-selfhost.sh
./verify-selfhost.sh /path/to/application/classes AppName com.example
```

The default build uses `-O1` and produces `target/parpar`, which is the executable the verification script expects. The `-O3` build used for the performance comparison produces a separate `target/parpar-O3`; it does not replace `target/parpar`. The verification script compares emitted output for the supplied class directory. Use a complete input corpus and the application name and package expected by the translator.

The benchmark scripts interleave runtimes over the same input and check generated output before reporting performance ratios. A compiler that returns quickly after doing different work has not won that comparison.

[PR #5766](https://github.com/codenameone/CodenameOne/pull/5766) contains the self-hosting work. The current source keeps the deterministic naming and constant-pool lookup fixes in the translator used by normal builds too.

## A larger application for the same runtime

We opened the week with a server built from a mobile runtime. We close it with that runtime compiling its own translator. Between them, the Vault API takes encrypted records across devices, invitations survive installation, desktop themes gain platform behavior, and the builders adapt to Apple's latest requirements.

Those features are useful individually. Maintaining the compiler, runtime, APIs, and builders together also lets a problem found in one target improve the others. A server finds retained collector buffers. A compiler finds a broken primitive-type mapping. An app benefits without carrying a private native patch for either one.

We're applying the same discipline to security: make the vault enforce its requested protection, keep SQL values bound, carry referral codes without guessing identities, and reject invalid build configurations before distribution. There's more performance work ahead. We now have another substantial application helping us decide exactly where it belongs.

---

## Discussion

_What real application would you use to test a runtime after its microbenchmarks pass?_

{{< giscus >}}
