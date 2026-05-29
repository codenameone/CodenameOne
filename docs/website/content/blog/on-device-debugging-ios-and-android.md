---
title: On-Device Debugging On iOS And Android
slug: on-device-debugging-ios-and-android
url: /blog/on-device-debugging-ios-and-android/
date: '2026-05-30'
author: Shai Almog
description: JDWP-based on-device debugging for ParparVM iOS apps and Android apps. Attach jdb, IntelliJ, VS Code, Eclipse, or NetBeans straight to the device or the iOS Simulator. Set breakpoints in your Java code, walk the stack, inspect locals, invoke methods. No Xcode, no Android Studio, no jumping through hoops.
feed_html: '<img src="https://www.codenameone.com/blog/on-device-debugging-ios-and-android.jpg" alt="On-Device Debugging On iOS And Android" /> JDWP-based on-device debugging for ParparVM iOS apps and Android apps. Attach jdb, IntelliJ, VS Code, Eclipse, or NetBeans straight to the device or the iOS Simulator. Set breakpoints, walk the stack, inspect locals, invoke methods. No Xcode, no Android Studio, no jumping through hoops.'
---

![On-Device Debugging On iOS And Android](/blog/on-device-debugging-ios-and-android.jpg)

This is a feature I have personally wanted for a long time. Long enough that I had honestly given up hoping it would be solved in any way that did not feel like a compromise. So I am going to start this post by saying out loud what we are talking about and why it matters, before I get into the implementation, because the *what* is the part that took me by surprise.

Codename One has always supported on-device debugging in the strict technical sense. You could attach Xcode to a `.ipa`, you could attach Android Studio to a running APK, you could read the native call stack, you could see the C functions ParparVM produced, you could step through Objective-C. What you could not do is set a breakpoint in `MyForm.java`, hit it on a real iPhone, and inspect a Java field on a Java object as a Java object. The bridge between "this is the Java code I wrote" and "this is what is actually running on the silicon" got chopped in two by the translation step, and the only way back across the gap was to read C and translate it in your head.

The PRs that shipped this week close the gap. As of today you can attach `jdb`, IntelliJ IDEA, VS Code, Eclipse, or NetBeans (anything that speaks JDWP) to a Codename One app running on:

- a real iPhone connected over USB,
- a real iPhone over Wi-Fi after pairing,
- the iOS Simulator,
- a real Android phone over USB,
- a real Android phone over wireless `adb`,
- the Android emulator,

and set a breakpoint in your Java source, hit it, see the local variables as Java values, walk the stack as Java frames, inspect instance fields as Java fields, and invoke methods on live Java objects. The same workflow you have on the simulator, on the device, with nothing in the middle pretending to be something else.

If you would rather just *try* it, the developer guide has the step-by-step under [On-Device-Debugging.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/On-Device-Debugging.asciidoc) for iOS and [On-Device-Debugging-Android.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/On-Device-Debugging-Android.asciidoc) for Android. The rest of this post is the why and the how.

## Why this was hard

The reason on-device debugging on iOS was historically a hassle is structural, not lazy. ParparVM translates Java bytecode to C. The C is compiled by Xcode. The thing running on the phone is a regular Mach-O binary, and the operating system has no idea any of it ever was Java. A breakpoint set in `MyForm.java` line 42 has to land at the corresponding generated C line, the C variable that holds the value of the Java local has to be located on the suspended thread's stack, the type tag has to be recovered, and the value has to be marshalled back across the wire in a form that the IDE recognises as a Java value with a class name and a signature.

Android is closer to the JVM (Dalvik / ART already speaks JDWP) but until this PR you still had to know that, set up the manifest correctly, run the right `adb` commands, forward the right port, and wire IntelliJ's remote-debug profile. Doable, but a workflow nobody used because the path of least resistance was always to add a `Log.p` and rebuild.

The PRs make the workflow the path of least resistance. Two clicks in IntelliJ. One run config to launch, one run config to attach.

## iOS, in three pieces

[PR #4999](https://github.com/codenameone/CodenameOne/pull/4999) is the iOS half. It is three independent pieces glued together:

1. **Translator instrumentation.** When you set `cn1.onDeviceDebug=true` the ParparVM translator emits side-tables next to every method (locals address arrays, variable names, line tables, per-class field offset tables, per-method invoke thunks) plus a `cn1-symbols.txt` sidecar that names every class, method, line, local, and field. Release builds are unaffected; the whole thing is gated by a `CN1_ON_DEVICE_DEBUG` preprocessor define so when the hint is off there is no extra code in the binary.
2. **Device runtime.** A new `cn1_debugger.{h,m}` is compiled into debug builds. It dials out to a desktop proxy over TCP and services a wire protocol from a listener thread. The fast path through the `__CN1_DEBUG_INFO` hook when nothing is attached is a single load and predicted-not-taken branch (`__builtin_expect(cn1DebuggerActive, 0)`), so the cost of having the instrumentation present is in the noise. When something *is* attached, suspend / resume yields the GC bit so paused threads do not block collection; `dup2`-based capture forwards `stdout` and `stderr` lines into the IDE console; method invocation is queued on the suspended Java thread so it runs in a valid `tsd` context, and the underlying call is wrapped in `setjmp` so an uncaught throw round-trips back as a typed exception instead of `longjmp`-ing past `suspendCurrent`.
3. **Desktop proxy.** `maven/cn1-debug-proxy/` is a minimum-viable JDWP server that translates between our custom wire protocol and standard JDWP. It is the piece that lets *any* JDWP-speaking IDE attach: from the IDE's point of view it is just talking to a JVM. Coverage includes the parts you actually use day to day: `VirtualMachine.*`, `ReferenceType.*`, `ClassType.InvokeMethod`, `Method.LineTable` / `VariableTable`, `ObjectReference.GetValues` / `InvokeMethod`, `ArrayReference.Length` / `GetValues`, `StringReference.Value`, `ThreadReference.*`, `StackFrame.*`, the full `EventRequest` parser with all twelve JDWP modifier kinds, and the `Event.Composite` events that come back. Breakpoint, step, exception, watchpoint, thread-start / thread-death; all of those land on the IDE's events list as you would expect.

The generated archetype includes two IntelliJ run configurations in an *On-Device Debug* folder: *CN1 iOS On-Device Debug* (Maven), which builds the debug `.ipa` with the right hints, installs it, and starts the proxy; and *CN1 Attach iOS* (Remote JVM Debug, `localhost:5005`), which is the second click that hooks IntelliJ to the proxy. Two clicks. Same flow for VS Code and Eclipse using their respective remote-JVM-debug profiles pointed at `localhost:5005`.

The instrumentation excludes a few packages (`java.io.*`, `java.net.*`, `java.nio.*`, `com.codename1.impl.*`) from the invoke-thunk set because those have hand-written native shims that have drifted away from the modern calling convention. You can still set breakpoints inside them; you just cannot synthesise method invocations on them from the IDE's evaluate-expression window. In practice this never matters; the typical use case is breakpoints in user code.

## Android, in much less code

[PR #5012](https://github.com/codenameone/CodenameOne/pull/5012) is the Android half. Dalvik and ART already speak JDWP, which is the reason this PR is much smaller; there is no proxy and no instrumentation, just orchestration:

- A new build hint `android.onDeviceDebug=true` flips the manifest to `debuggable="true"` and disables R8 / Proguard for the debug build. Release builds are unaffected.
- A new `cn1:android-on-device-debugging` Mojo locates `adb`, optionally `adb connect`s a wireless device, installs the APK, sets the debug-app for wait-for-attach, launches the Activity, forwards JDWP onto `localhost:5005`, and streams `logcat --pid=<pid>` into the console with a `[device]` prefix.
- A `cn1:buildAndroidOnDeviceDebug` wrapper forces the hint and triggers the standard `android-device` cloud build, so the same workflow works against cloud-built APKs without you having to remember the flag.
- Two IntelliJ run configurations in the same *On-Device Debug* folder as the iOS pair: *CN1 Android On-Device Debug* (Maven) and *CN1 Attach Android* (Remote JVM Debug, `localhost:5005`).

Wireless debugging works through both the Android 11+ `adb pair` flow and the legacy `adb tcpip` flow; the Mojo accepts a `-Dcn1.android.onDeviceDebug.wireless=<ip:port>` argument and does the right thing. Source resolution covers both the `codenameone-core` and `codenameone-android` sources jars, so breakpoints inside the framework resolve to the right file in the IDE the same way they would for user code.

JNI / NDK code is intentionally out of scope; JDWP does not speak C / C++. If you have an NDK component you want to step through alongside the Java half, Android Studio's LLDB attaches to the same process alongside this JDWP session.

## The thing that surprised me

What surprised me is how much the lack of on-device debugging had silently shaped the way I wrote code. The implicit assumption "this will only ever be debuggable in the simulator" is one of those things that bends a workflow without your noticing. You start putting platform-specific code behind feature flags so you can exercise it in the simulator. You stop writing test code that depends on real-device state because there is no way to put a breakpoint in it on the device. You start writing `Log.p("got here: " + value)` instead of pulling up a debugger because the friction is too high.

I noticed the pattern only when I sat down with the new pipeline on a real iPhone and put a breakpoint inside the iOS native callback path of a feature I have been working on for weeks. The breakpoint hit. The locals were Java locals. I evaluated an expression on a live Java object on a real iPhone and watched the result come back as a `String`. That sounds banal. It is the kind of thing that you take for granted on a JVM. After fifteen years of Codename One, doing it on the device for the first time was a genuinely strange moment.

The other surprise is the simulator path. The iOS Simulator support means you can run the same JDWP attach against the Apple simulator on macOS, which makes the simulator a *third* useful environment alongside the JavaSE simulator and the device. Things you cannot easily test in the JavaSE simulator (touch ID, the iOS sharing flow, the iOS keyboard) are now testable under a breakpoint without owning the iPhone you would otherwise need.

## A note on what stays in the simulator

None of this changes the recommendation that you do most of your iteration in the JavaSE simulator. That is still by a large margin the fastest loop: edit, hit run, see the result, attach `jdb` (the [Skills, Java 17, and Theme Accents](/blog/skills-java17-and-theme-accents/) post from two weeks ago has the agents-driven walkthrough for that), step through. The on-device path is the one you reach for when the bug is platform-specific, when the bug only happens on a real radio, when the bug only reproduces against a real Touch ID hardware, when the bug only shows up under iOS's memory pressure. The kind of bug that previously sent you reaching for `Log.p` and a rebuild loop. That bug now has a debugger pointed at it.

## Wrapping up

This is the post I was looking forward to writing the most this release cycle. If you build something interesting with the new pipeline (or, more usefully, if you hit something it does not handle correctly), please open an issue: the surface is broad and the only way I am going to know which JDWP edge case our proxy is missing is if someone hits it.

The developer guide chapters are at [On-Device-Debugging.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/On-Device-Debugging.asciidoc) and [On-Device-Debugging-Android.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/On-Device-Debugging-Android.asciidoc). The PRs are [#4999](https://github.com/codenameone/CodenameOne/pull/4999) and [#5012](https://github.com/codenameone/CodenameOne/pull/5012). Tomorrow's post covers the new WiFi and connectivity APIs.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
