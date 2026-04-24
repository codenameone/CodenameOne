---
title: iOS Density, Scroll Physics, and Accessibility
date: '2026-04-24'
author: Shai Almog
slug: ios-density-scroll-and-accessibility
url: /blog/ios-density-scroll-and-accessibility/
description: If you have shipped to an iPhone 14 Pro or anything newer, your density calculations have been wrong until this week. That fix lines up with a proper iOS-matching scroll physics rewrite, localized launcher icons, a simulator menu for previewing Dynamic Type, and the new SIMD API.
feed_html: '<img src="https://www.codenameone.com/blog/ios-density-scroll-and-accessibility.jpg" alt="iOS Density, Scroll Physics, and Accessibility" /> If you have shipped to an iPhone 14 Pro or anything newer, your density calculations have been wrong until this week. That fix lines up with a proper iOS-matching scroll physics rewrite, localized launcher icons, a simulator menu for previewing Dynamic Type, and the new SIMD API.'
---

![iOS Density, Scroll Physics, and Accessibility](/blog/ios-density-scroll-and-accessibility.jpg)

Most weeks the blog post opens with a new API I want you to know about. This week I want to open with a correction instead, because if you shipped to an iPhone 14 Pro — or any iPhone newer than that — your density calculations have quietly been wrong until now.

That is the headline fix. Alongside it we landed a proper iOS-matching scroll physics rewrite, per-locale launcher icons on iOS and Android, a simulator control for previewing Dynamic Type, a one-line API for scaling theme fonts, and — at the end of the post, because it is a deep topic that deserves real space — a new vectorised primitive called `Simd` that is already making Codename One's `Base64` meaningfully faster than iOS's own native implementation.

## The iOS PPI Table Was Out of Date

The iOS port converts between physical millimetres and pixels using a per-device PPI lookup keyed on the screen's pixel dimensions. That table had not been updated in a long time.

Missing entries included:

- iPhone 14 Pro, 15, 15 Pro, 16 (2556 × 1179 at 460 ppi)
- iPhone 14 Pro Max, 15 Plus, 15 Pro Max, 16 Plus (2796 × 1290 at 460 ppi)
- iPhone 16 Pro (2622 × 1206 at 460 ppi)
- iPhone 16 Pro Max (2868 × 1320 at 460 ppi)
- iPhone 12 Pro Max, 13 Pro Max, 14 Plus (2778 × 1284 at 458 ppi)

All of those devices fell through to the unknown-device fallback, which was a 2014-era value of `19.25429416` (roughly the iPhone 6 Plus at 401 ppi). For anything `mm`-based in your theme — borders, padding expressed in physical units, icon sizes measured that way — the rendered dimensions were off by a noticeable margin on every modern non-Plus iPhone.

The table is now current, and the fallback for unknown 3× devices defaults to 460 ppi. Apple has held that density steady for every non-Plus iPhone since the iPhone 12, so when a new model ships with an unrecognised resolution, the fallback will be correct to within a few percent instead of off by thirty.

If you were compensating for this in your own theme with magic numbers, this is a good moment to reassess.

## The Simulator Can Now Preview Dynamic Type

If the PPI fix quietly changes what your app looks like on hardware, the simulator's new *Simulate → Larger Text* menu lets you see the full Dynamic Type range without leaving your desk.

The menu exposes every iOS Dynamic Type stop, from *Extra Small* (`0.82×`) through *Large* (`1.00×`, the default) up to *Extra Extra Extra Large* (`1.35×`), plus the five *Larger Accessibility Sizes* that iOS unlocks when you enable that toggle in Settings → Accessibility → Display & Text Size. The accessibility stops go as high as `3.12×`. The simulator returns the exact same ratio that `UIFontMetrics` would report on the matching device, so what you see is what an iOS user sees, subject only to the underlying font metrics.

The selected stop persists across simulator restarts, which turns this into something you can leave on a particular accessibility setting for a few days while you notice what breaks.

If you have ever had a user report that "the text is huge on my phone and the layout is broken" and not been able to reproduce it, this is the tool. On iOS the typical culprit is *Larger Accessibility Sizes* being enabled on the device, because those sizes start at `1.65×` and exceed what the standard Display & Brightness slider can reach. Preview stop AX1 through AX5 in the simulator and you will find the layout issue immediately.

## iOS Scroll, One More Time

Scroll feel on iOS has been through more iterations in this framework than I can reliably count. Every time I have thought we had it right, somebody would flick a list on a new device and the whole thing would feel half a beat off. Too much rubber band. Not enough. Snap-back that arrived with a thud instead of settling. Deceleration that looked right in isolation and wrong next to Safari.

This is another attempt. I think it is the most principled one we have had, and my hope is that it is the one that settles the question.

The rewrite models scroll physics in three explicit stages, each tunable via theme constants:

1. **While the finger drags past an edge**, the over-edge distance is compressed by the iOS `UIScrollView` rubber-band function `c * d * dim / (c * d + dim)`. Content feels heavier the farther you pull it, asymptotically approaching the viewport dimension. `rubberBandCoefficientInt` controls the curve (default `55`, interpreted as hundredths — `0.55` is the iOS reference value). Reduce it toward `35` for a stiffer feel.
2. **After the finger lifts, while there is still velocity**, the `ScrollMotion` constant picks the model. `DECAY` (the default) uses exponential velocity decay with a time constant of `ScrollMotionTimeConstantInt` milliseconds (default `500`) and a distance scale of `DecayMotionScaleFactorInt` (default `950`). The legacy linear-friction model is still available by setting `ScrollMotion=FRICTION`.
3. **When the scroll comes to rest past an edge**, a tensile snap animates the content back. `tensileSnapMotion=SPRING` applies a critically-damped envelope that settles softly (this is the new default when `iosScrollMotionBool=true`); `DECELERATION` keeps the legacy quadratic ease-out.

`iosScrollMotionBool` is the single switch that flips stage 1 and the default of stage 3 to the iOS-matching behaviour. It defaults to `true` on iOS only, but every individual constant is independently overridable. If you want iOS physics on Android for consistency, set `iosScrollMotionBool=true` explicitly. If you specifically want the nonlinear rubber band but the old quadratic snap-back, that combination is also available.

If this version finally feels right on your devices, I would love to hear about it. If it does not, the constants above are where to start looking.

## Localized App Icons

Another long-standing request that landed this week: [per-locale launcher icons on iOS and Android](https://github.com/codenameone/CodenameOne/issues/4770).

The workflow is file-driven. Drop per-locale PNGs into `common/src/main/resources` using the naming convention `cn1_icon_<lang>[_<country>].png`:

- `cn1_icon_fr.png` — French (any region)
- `cn1_icon_en_GB.png` — British English
- `cn1_icon_es_MX.png` — Mexican Spanish

Supply a square source image of at least 432×432 pixels. The build resizes it to every target density.

On Android, the build generates locale-qualified resources (`drawable-<lang>[-r<COUNTRY>]/icon.png` at every density, and the matching `mipmap-*` entries when adaptive icons are enabled). Android's resource framework then switches icons automatically based on the device's current locale. No code changes required.

iOS does not natively localize launcher icons, so we wire up alternate icons instead. The build emits per-locale images at `@2x`, `@3x`, and iPad sizes; injects a `CFBundleIcons` / `CFBundleIcons~ipad` block into `Info.plist` with a `CFBundleAlternateIcons` dictionary; and patches the app delegate to call `-[UIApplication setAlternateIconName:completionHandler:]` at launch based on `[NSLocale preferredLanguages]`. It tries the full `<lang>_<COUNTRY>` key first, falls back to the language-only variant, and reverts to the default if nothing matches.

One thing to be aware of: iOS displays a system alert the first time an app switches to an alternate icon. That is platform-standard behaviour. We cannot suppress it, and we shouldn't try.

## `UIManager.zoomFonts(factor)`

The missing piece alongside the simulator Dynamic Type menu was a programmatic way to apply a scale at runtime. `UIManager.zoomFonts(factor)` does exactly that. It multiplies every scalable font in the current theme (and the default styles) by the factor you pass:

```java
UIManager.getInstance().zoomFonts(1.2f); // 20% larger
UIManager.getInstance().zoomFonts(0.8f); // 20% smaller
```

System fonts are skipped because their size is fixed by the platform — there is nothing to scale. The styles cache gets cleared so subsequent components pick up the new sizes. For already-displayed forms, you still need to call `Form.refreshTheme()` to re-layout; the Javadoc spells that out.

Combined with the simulator's *Larger Text* menu, the pipeline for "show me what the app looks like for a user at accessibility text size 3" is now one preview click and, if needed, one line of code.

## Playground Gets Better at Real Java

The Playground has been moving fast, and a lot of this week's work on it sits in the BeanShell layer that runs user code.

The practical impact: `try-with-resources` now parses and evaluates, including the no-catch/no-finally form and local `AutoCloseable` resources. Inline class declarations work for scripted classes, and superclass invocation correctly propagates checked exceptions and target exceptions. The rewriting pass that used to lean on reflection and regex has been replaced with Codename One's own `RE` and `StringUtil` — important because Playground snippets compile and run in the actual CN1 runtime, and the cleaner the surface, the closer the experiment is to the production code it will eventually become.

Java language coverage and the Playground's design remain our top priorities, and this week's changes move the needle on both.

## The SIMD API — An Advanced Topic for Those Who Want to Dig Deep

This section is long, and the audience for it is narrow. If you just want faster apps without thinking about it, you already have them — `Base64` and parts of the `Image` API call into `Simd` internally, and the numbers at the end of this section are what you get for free. Keep scrolling to the closing thoughts.

If you do hot-loop work — image processing, codecs, custom compositing, audio mixing, cryptographic primitives, anything where you spend significant time inside a tight loop over a primitive array — the rest of this section is for you.

### What SIMD Actually Is

SIMD stands for *Single Instruction, Multiple Data*. Modern CPUs have wide registers (128 bits on ARM NEON and x86 SSE, wider on AVX and SVE) that can hold several values packed side by side: sixteen bytes, eight 16-bit integers, four 32-bit integers, or four 32-bit floats. One instruction — `VADD` on NEON, `PADDD` on SSE — then operates on every packed lane in parallel.

The diagram below is the whole idea in one picture. On the left, a normal scalar loop steps through the buffer one element at a time: four operations, four cycles. On the right, the same four pairs of inputs sit in a single wide register, and one instruction produces all four results at once.

![Scalar versus SIMD: four scalar cycles collapse into a single SIMD cycle](/blog/simd-scalar-vs-parallel.svg)

For workloads where the operation per element is simple and identical — Base64 encode/decode, pixel blending, alpha masks, colour-channel manipulation, table lookups, UTF-8 validation, checksums — this typically means a 3× to 10× speedup over the scalar version. For workloads with per-element branching, pointer chasing, or buffers smaller than a single SIMD register, it means nothing. SIMD is a data-parallel hammer; it is not a general-purpose one.

### What This Looks Like in Modern Java

If you have been following recent OpenJDK work, this all sounds familiar. The Vector API ([JEP 460](https://openjdk.org/jeps/460) and its ongoing successors) has been incubating for several JDK releases and is on track to become a permanent standard API; it is, in essence, portable SIMD in Java. Separately, Project Valhalla is reshaping the JVM so that small value-type objects — and the buffers that hold them — can live on the stack or in registers rather than on the heap, eliminating allocation pressure in exactly the kind of tight loop where SIMD pays off.

Codename One has both of those things today, because we control the translator. `com.codename1.util.Simd` is the portable SIMD API; the stack-allocation story is what we cover in the next subsection. None of this is a replacement for the Vector API or for Valhalla. It is the same set of ideas, delivered now, inside the existing Codename One runtime.

### Architecture

`com.codename1.util.Simd` is designed so you write the same code everywhere and get native speed on platforms that have native SIMD, and correct results on the ones that do not.

![com.codename1.util.Simd routes to NEON on iOS via ParparVM, and falls back to a pure-Java implementation on Android and the JavaSE simulator](/blog/simd-architecture.svg)

On iOS, the ParparVM translator lowers the calls to NEON intrinsics directly. On Android and the JavaSE simulator, `JavaSESimd` provides a pure-Java fallback with identical semantics. `simd.isSupported()` tells you whether the current platform is on the fast path; regardless of the answer, the results match.

### Allocation: `alloc` vs `alloca`, and Why the Distinction Matters

SIMD loads and stores prefer — and on some architectures require — that the address they read from is a multiple of the register width. The JVM gives you no such guarantee for `new byte[64]`. That is why `Simd` exposes its own allocation helpers, and why passing an arbitrary `new`-allocated array is forbidden.

There are two families of helpers, and the distinction between them is the interesting part:

```java
Simd simd = Simd.get();

// Heap-backed, aligned, lives as long as any reference to it.
byte[]  heapBytes  = simd.allocByte(64);

// Scratch buffer. On ParparVM this may be lowered to a
// stack-backed "faux array". Method-local use only.
byte[]  scratch    = simd.allocaByte(64);
```

`allocByte` / `allocInt` / `allocFloat` return ordinary heap arrays, just with alignment guarantees. Nothing unusual about their lifetime.

`allocaByte` / `allocaInt` / `allocaFloat` — note the extra `a`, and yes, the name deliberately echoes C's `alloca()` — are a different beast. On ParparVM the translator is allowed to lower these into *stack-allocated* scratch buffers. No heap allocation. No GC pressure. No retained reference. The contents are undefined until you write them, and the moment the method returns, the backing memory is gone. This is the same core idea that Valhalla's escape analysis will eventually do for you automatically on the standard JVM.

For that to be safe, the value must never escape the method. The compliance mojo enforces this by walking the bytecode of any method that uses `alloca*`. If an alloca-derived value is:

- returned from the method
- stored into a static field
- stored into an instance field
- stored into an object array
- passed to any method that is not on `Simd` itself
- passed to an `invokedynamic` call site

the build fails with `SIMD alloca value ...` and tells you where. The message is specifically `Keep SIMD alloca scratch arrays method-local and only pass them to Simd methods.`

This is the bytecode-verification piece we added as part of the compliance work a few weeks back, applied to a new problem. The same infrastructure that rewrites unsupported API calls and catches platform-incompatible code now catches scratch buffers that try to escape. Without that verifier, `alloca*` would be too dangerous to ship. With it, the common case — scratch buffers for a hot inner loop — is both fast and safe.

### What You Get For It

The new `Simd` API is already wired into `Base64` and several `Image` operations. The numbers on iOS speak for themselves.

Base64 is the headline. Last month we talked about closing the gap with Apple's native implementation. This week, Codename One's Base64 on iOS is comfortably *faster* than the native version:

![Base64 performance on iOS: native encode 1078 ms, CN1 scalar 1224 ms, CN1 SIMD 379 ms; native decode 713 ms, CN1 scalar 903 ms, CN1 SIMD 365 ms](/blog/simd-base64-performance.svg)

Over 6000 iterations on an 8192-byte payload:

| Metric                       | Time     | vs. native iOS          |
| ---                          | ---      | ---                     |
| Base64 native encode         | 1078 ms  | —                       |
| Base64 CN1 scalar encode     | 1224 ms  | 13.5% slower            |
| **Base64 CN1 SIMD encode**   | **379 ms** | **64.8% faster**      |
| Base64 native decode         | 713 ms   | —                       |
| Base64 CN1 scalar decode     | 903 ms   | 26.6% slower            |
| **Base64 CN1 SIMD decode**   | **365 ms** | **48.8% faster**      |

Against our own previous implementation, SIMD gives 69.0% faster encode and 59.6% faster decode.

Image processing follows a similar curve. Same SIMD on / SIMD off comparison on the same hardware:

| Operation                  | SIMD off | SIMD on | Speedup         |
| ---                        | ---      | ---     | ---             |
| `createMask`               | 56 ms    | 10 ms   | 82.1% faster    |
| `applyMask`                | 129 ms   | 56 ms   | 56.6% faster    |
| `modifyAlpha`              | 132 ms   | 54 ms   | 59.1% faster    |
| `modifyAlpha` (removeColor)| 136 ms   | 63 ms   | 53.7% faster    |
| PNG encode                 | 919 ms   | 762 ms  | 17.1% faster    |

The PNG encode number is the most honest one in the table. PNG is dominated by DEFLATE, which is inherently serial and resists vectorisation. A 17% improvement is what SIMD can do for the compositing and filtering stages around the compressor; the compressor itself stays where it was. That asymmetry is a good reminder of what SIMD is and is not.

Everywhere the operation is genuinely data-parallel, the speedup is in the 50–80% range. That is what we were after.

## Closing Thoughts

The through-line this week is that a lot of what landed affects what users actually see and feel on an iPhone: the physical size of things on screen, the way a flicked list settles, the launcher icon they see in their home-screen language, and the type scale they get when they crank up accessibility.

The SIMD work is the odd one out in that list, and that is deliberate. It is infrastructure — the scalar-path performance work we did on `Base64` last month now has a vectorised floor underneath it, and the image-processing paths are steadily moving onto it too. Most apps will never call `Simd` directly. They just get faster.

Issue count is still hovering right around 500. We continue to chip away at it, and as before, if you have old issues in the tracker it is genuinely helpful to close what is no longer relevant and comment on what still matters.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
