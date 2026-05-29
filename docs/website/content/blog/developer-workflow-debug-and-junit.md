---
title: Developer Workflow: On-Device Debugging And JUnit 5
slug: developer-workflow-debug-and-junit
url: /blog/developer-workflow-debug-and-junit/
date: '2026-05-30'
author: Shai Almog
description: JDWP-based on-device debugging for ParparVM iOS apps and Android apps, so jdb / IntelliJ / VS Code / Eclipse / NetBeans attach straight to the device or simulator. Plus standard JUnit 5 @Test methods against the JavaSE simulator, with annotations for the visual configuration (@Theme, @DarkMode, @LargerText, @Orientation, @RTL).
feed_html: '<img src="https://www.codenameone.com/blog/developer-workflow-debug-and-junit.jpg" alt="Developer Workflow: On-Device Debugging And JUnit 5" /> JDWP-based on-device debugging for ParparVM iOS and Android, plus standard JUnit 5 @Test methods against the JavaSE simulator with annotations for the visual configuration.'
---

![Developer Workflow: On-Device Debugging And JUnit 5](/blog/developer-workflow-debug-and-junit.jpg)

Two things this post. Both are about how you iterate on a Codename One app rather than what the app itself does, and both are the kind of change you only notice the impact of after a week of working with them in place. The first is one we have wanted for a long time: on-device debugging that actually treats Java as Java. The second is JUnit 5 as a first-class test framework against the simulator.

## On-device debugging that treats Java as Java

Codename One has always supported on-device debugging in the strict technical sense. You could attach Xcode to a `.ipa`, you could attach Android Studio to a running APK, you could read the native call stack, you could step through Objective-C or the C ParparVM emits. What you could not do is set a breakpoint in `MyForm.java`, hit it on a real iPhone, and inspect a Java field on a Java object as a Java object. The bridge between "this is the Java code you wrote" and "this is what is actually running on the silicon" got chopped in two by the translation step, and the only way back across the gap was to read C and translate it in your head.

[PR #4999](https://github.com/codenameone/CodenameOne/pull/4999) (iOS) and [PR #5012](https://github.com/codenameone/CodenameOne/pull/5012) (Android) close the gap. As of this week you can attach `jdb`, IntelliJ IDEA, VS Code, Eclipse, or NetBeans (anything that speaks JDWP) to a Codename One app running on:

- a real iPhone connected over USB,
- a real iPhone over Wi-Fi after pairing,
- the iOS Simulator,
- a real Android phone over USB,
- a real Android phone over wireless `adb`,
- the Android emulator.

You then set a breakpoint in your Java source, hit it, see the locals as Java values, walk the stack as Java frames, inspect instance fields as Java fields, and invoke methods on live Java objects. The same workflow you have on the simulator, on the device, with nothing in the middle pretending to be something else.

If you would rather just *try* it, the developer guide has the step-by-step under [On-Device-Debugging.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/On-Device-Debugging.asciidoc) for iOS and [On-Device-Debugging-Android.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/On-Device-Debugging-Android.asciidoc) for Android. The rest of this section is how it is wired.

### iOS, in three pieces

The iOS half is structural because ParparVM translates Java bytecode to C and the operating system has no idea any of it ever was Java. The PR adds three independent pieces that together carry Java semantics across the gap:

1. **Translator instrumentation.** When you set `cn1.onDeviceDebug=true` the ParparVM translator emits side-tables next to every method (locals address arrays, variable names, line tables, per-class field offset tables, per-method invoke thunks) plus a `cn1-symbols.txt` sidecar that names every class, method, line, local, and field. Release builds are unaffected; the whole thing is gated by a `CN1_ON_DEVICE_DEBUG` preprocessor define so when the hint is off there is no extra code in the binary.
2. **Device runtime.** A new `cn1_debugger.{h,m}` is compiled into debug builds. It dials out to a desktop proxy over TCP and services a wire protocol from a listener thread. The fast path through the `__CN1_DEBUG_INFO` hook when nothing is attached is a single load and predicted-not-taken branch (`__builtin_expect(cn1DebuggerActive, 0)`), so the cost of having the instrumentation present is in the noise. When something *is* attached, suspend / resume yields the GC bit so paused threads do not block collection; `dup2`-based capture forwards `stdout` and `stderr` into the IDE console; method invocation is queued on the suspended Java thread so it runs in a valid `tsd` context, and the underlying call is wrapped in `setjmp` so an uncaught throw round-trips back as a typed exception instead of `longjmp`-ing past `suspendCurrent`.
3. **Desktop proxy.** `maven/cn1-debug-proxy/` is a minimum-viable JDWP server that translates between our custom wire protocol and standard JDWP. It is the piece that lets *any* JDWP-speaking IDE attach: from the IDE's point of view it is just talking to a JVM. Coverage includes the parts you actually use day to day: `VirtualMachine.*`, `ReferenceType.*`, `ClassType.InvokeMethod`, `Method.LineTable` / `VariableTable`, `ObjectReference.GetValues` / `InvokeMethod`, `ArrayReference.Length` / `GetValues`, `StringReference.Value`, `ThreadReference.*`, `StackFrame.*`, the full `EventRequest` parser with all twelve JDWP modifier kinds, and the `Event.Composite` events that come back.

The generated archetype includes two IntelliJ run configurations in an *On-Device Debug* folder: *CN1 iOS On-Device Debug* (Maven), which builds the debug `.ipa` with the right hints, installs it, and starts the proxy; and *CN1 Attach iOS* (Remote JVM Debug, `localhost:5005`), which is the second click that hooks IntelliJ to the proxy. Two clicks. Same flow for VS Code and Eclipse using their respective remote-JVM-debug profiles pointed at `localhost:5005`.

### Android, in much less code

Dalvik and ART already speak JDWP, which is the reason the Android PR is much smaller. No proxy and no instrumentation; just orchestration:

- A new build hint `android.onDeviceDebug=true` flips the manifest to `debuggable="true"` and disables R8 / Proguard for the debug build. Release builds are unaffected.
- A new `cn1:android-on-device-debugging` Mojo locates `adb`, optionally `adb connect`s a wireless device, installs the APK, sets the debug-app for wait-for-attach, launches the Activity, forwards JDWP onto `localhost:5005`, and streams `logcat --pid=<pid>` into the console with a `[device]` prefix.
- A `cn1:buildAndroidOnDeviceDebug` wrapper forces the hint and triggers the standard `android-device` cloud build, so the same workflow works against cloud-built APKs without you having to remember the flag.
- Two IntelliJ run configurations in the same *On-Device Debug* folder as the iOS pair: *CN1 Android On-Device Debug* (Maven) and *CN1 Attach Android* (Remote JVM Debug, `localhost:5005`).

Wireless debugging works through both the Android 11+ `adb pair` flow and the legacy `adb tcpip` flow. Source resolution covers both the `codenameone-core` and `codenameone-android` sources jars, so breakpoints inside the framework resolve to the right file the same way they would for user code. JNI / NDK code is intentionally out of scope; if you have an NDK component you want to step through alongside the Java half, Android Studio's LLDB attaches to the same process alongside this JDWP session.

### The thing that surprised us

The lack of on-device debugging had silently shaped the way Codename One code gets written, and we did not realise the extent of it until we sat down with the new pipeline in front of us. The implicit assumption "this will only ever be debuggable in the simulator" is one of those things that bends a workflow without you noticing. You start putting platform-specific code behind feature flags so you can exercise it in the simulator. You stop writing test code that depends on real-device state because there is no way to put a breakpoint in it on the device. You reach for `Log.p("got here: " + value)` because the friction of anything more elaborate is too high.

The first run on a real iPhone with the new pipeline made this very concrete. A breakpoint inside the iOS native callback path of a feature we have been working on for weeks. The breakpoint hit. The locals were Java locals. The IDE evaluated an expression on a live Java object on a real iPhone and the result came back as a `String`. That sounds banal. It is the kind of thing you take for granted on a JVM, and after fifteen years of Codename One, seeing it on the device for the first time was a genuinely strange moment.

None of this changes the recommendation that you do most of your iteration in the JavaSE simulator. That is still by a large margin the fastest loop. The on-device path is what you reach for when the bug is platform-specific, when it only happens on a real radio, when it only reproduces against real Touch ID hardware, when it only shows up under iOS's memory pressure. The kind of bug that previously sent you reaching for `Log.p` and a rebuild loop.

## JUnit 5 against the simulator

The matching change on the test side is [PR #5032](https://github.com/codenameone/CodenameOne/pull/5032). For years the recommended way to test a Codename One app was the `AbstractTest` / `DeviceRunner` framework. It works, it runs on the device, and it is the right answer when you want a true on-device integration test. It is also a framework you have to learn instead of one most Java developers already know.

The new package is `com.codename1.testing.junit`, and it lives in the JavaSE port. That last detail is important: it is simulator-only by design. The reason is the upside. Simulator tests run on a real JVM, so they get full reflection, real `java.lang.reflect`, real `java.util.concurrent` (the whole thing, not the subset), and the entire Mockito / AssertJ / WireMock / Testcontainers ecosystem just works.

```java
@CodenameOneTest
@SimulatorProperty(name = "feature.flag", value = "on")
class GreetingFormTest {

    @Test
    @RunOnEdt
    @LargerText(scale = 1.6f)
    @DarkMode
    void formStillFitsAtAccessibilityScaleInDark() {
        Form f = new GreetingForm();
        f.show();
        assertEquals("Hello", Display.getInstance().getCurrent().getTitle());
    }
}
```

`@CodenameOneTest` is the entry point: a meta-`@ExtendWith` that wires the simulator extension into the JUnit 5 lifecycle. The extension boots Display lazily on the first test that needs it and keeps the simulator warm for the rest of the JVM, so subsequent tests do not pay the startup cost again. Cross-test state cleanup is your responsibility (`@AfterEach`) by design; the extension never resets state you did not explicitly ask for.

The runtime knobs are individual annotations: `@RunOnEdt` runs the test body on the Codename One Event Dispatch Thread; `@SimulatorProperty(name=..., value=...)` (and the container `@SimulatorProperties`) sets a `Display.setProperty(...)` for the duration of the test. The visual ones are `@Theme(...)`, `@DarkMode`, `@LargerText(scale=...)`, `@Orientation(...)`, `@RTL`. All of those resolve method-level over class-level, are applied in a single batched setup on the EDT, and are followed by exactly one theme refresh. The end result is that the test body sees the simulator in the configuration you asked for with no flicker, no double-refresh, no leakage into the next test.

### Why JUnit specifically

The honest answer is that JUnit 5 is the lingua franca of Java testing in 2026 and Codename One's previous testing story was a dialect we asked you to learn instead. The new annotation set is a thin layer over JUnit, not a parallel framework: `@Test` is still `org.junit.jupiter.api.Test`; assertions are still `Assertions.assertEquals`; the failure rendering in your IDE is the same one you see on every other project; CI integrations (Surefire, Maven Failsafe, IntelliJ test runner, GitHub Actions test reporters) recognise it without configuration.

The other half of the answer is reflection. Running on the simulator (a regular JVM with no bytecode rewriting) means tests can do things the on-device test runner deliberately cannot, because it tracks the iOS / Android subset. Reflective access to private fields. Mockito-style proxy generation. AssertJ's recursive comparison. Reading a JVM system property the framework would not normally expose. If your test relies on those, run it under JUnit. If you specifically want to validate device-only behaviour (the real iOS keyboard, the real Android share intent, the real platform clipboard), keep using `AbstractTest`.

`junit-jupiter` moves from `test` scope to `provided` scope in the JavaSE port's POM. The reason is that the support classes (the extension, the annotations) need to compile against JUnit, but we did not want the JUnit dependency leaking onto the simulator runtime classpath for apps that do not opt in. Apps that want JUnit tests declare `junit-jupiter` in their own `test` scope; apps that do not are unaffected.

## Why the two changes pair

These two PRs ship in the same release on purpose. JUnit gives you a way to express the test you want to run; the on-device debugger gives you a way to step through what actually happens when you run it on the device. Together they push the dev loop closer to what it looks like on a regular JVM application: tests, breakpoints, REPL-grade interrogation of a running process. The simulator was always close to that; the device used to be far from it; now both are close. That is the thing worth getting used to.

## Wrapping up

Two pieces of paving that should quietly compound over the next few months. The on-device debugger is the one we expect to change behaviour the most, because once it is part of the loop you stop reaching for `Log.p` for the kinds of investigations where a breakpoint and a thread dump are the right tool.

Monday's post covers the run of new platform APIs that moved into the core this week: WiFi, OIDC + passkeys, share-sheet callbacks, and the new AI / LLM package.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
