---
title: JUnit 5 Tests For Codename One Apps
slug: junit-5-for-codename-one-apps
url: /blog/junit-5-for-codename-one-apps/
date: '2026-06-03'
author: Shai Almog
description: Write standard JUnit 5 @Test methods against the Codename One simulator. Annotations for @RunOnEdt, @SimulatorProperty, @Theme, @DarkMode, @LargerText, @Orientation, and @RTL run on the EDT in one batch with a single theme refresh. Tests have full JVM reflection, so Mockito and AssertJ work too.
feed_html: '<img src="https://www.codenameone.com/blog/junit-5-for-codename-one-apps.jpg" alt="JUnit 5 Tests For Codename One Apps" /> Write standard JUnit 5 @Test methods against the Codename One simulator. Annotations for @RunOnEdt, @SimulatorProperty, @Theme, @DarkMode, @LargerText, @Orientation, and @RTL run on the EDT in one batch. Tests have full JVM reflection.'
---

![JUnit 5 Tests For Codename One Apps](/blog/junit-5-for-codename-one-apps.jpg)

For years the recommended way to test a Codename One app was the `AbstractTest` / `DeviceRunner` framework. It works, it runs on the device, and it is the right answer when you want a true on-device integration test. It is also a framework you have to learn instead of a framework most Java developers already know. [PR #5032](https://github.com/codenameone/CodenameOne/pull/5032) lands an alternative: standard JUnit 5 `@Test` methods against the Codename One simulator, with first-class annotations for the simulator-specific knobs (theme, dark mode, larger-text accessibility scale, orientation, RTL, simulator properties).

The new package is `com.codename1.testing.junit`, and it lives in the JavaSE port. That last detail is important: it is simulator-only by design. The reason is the upside. Simulator tests run on a real JVM, so they get full reflection, real `java.lang.reflect`, real `java.util.concurrent` (the whole thing, not the subset), and the entire Mockito / AssertJ / WireMock / Testcontainers ecosystem just works.

## What a test looks like

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

The runtime knobs are individual annotations:

- `@RunOnEdt` runs the test body on the Codename One Event Dispatch Thread. The default is "test thread"; you opt in to the EDT when the code you are testing expects it.
- `@SimulatorProperty(name=..., value=...)` (and the container `@SimulatorProperties`) sets a `Display.setProperty(...)` for the duration of the test.
- `@Theme(...)` loads a specific `.res` theme. `@DarkMode` flips the dark-mode toggle. `@LargerText(scale=...)` applies an accessibility text-scale factor. `@Orientation(...)` switches portrait / landscape. `@RTL` enables right-to-left.

All of those resolve method-level over class-level, are applied in a single batched setup on the EDT, and are followed by exactly one theme refresh. The end result is that the test body sees the simulator in the configuration you asked for, with no flicker, no double-refresh, and no leakage into the next test.

## Why JUnit specifically

The honest answer is that JUnit 5 is the lingua franca of Java testing in 2026 and Codename One's previous testing story was a dialect we asked you to learn instead. The new annotation set is a thin layer over JUnit, not a parallel framework: `@Test` is still `org.junit.jupiter.api.Test`; assertions are still `Assertions.assertEquals`; the failure rendering in your IDE is the same one you see on every other project; CI integrations (Surefire, Maven Failsafe, IntelliJ test runner, GitHub Actions test reporters) recognise it without configuration.

The other half of the answer is reflection. Running on the simulator (which is a regular JVM with no bytecode rewriting) means tests can do things that the on-device test runner deliberately cannot, because it tracks the iOS / Android subset. Reflective access to private fields. Mockito-style proxy generation. AssertJ's recursive comparison. Reading a JVM system property the framework would not normally expose. If your test relies on those, run it under JUnit. If you specifically want to validate device-only behaviour (the real iOS keyboard, the real Android share intent, the real platform clipboard), keep using `AbstractTest`.

## The visual annotations

The set of `@Theme`, `@DarkMode`, `@LargerText`, `@Orientation`, `@RTL` annotations are worth a paragraph on their own because they correspond to the four or five things you would otherwise click through in the simulator menu by hand before running a test. The point of having them as annotations is that the test is a complete description of the configuration it needs. CI does not need to know that you usually click *Simulate -> Larger Text -> 1.6x* before running a particular regression; the annotation says so, the simulator applies it, the test runs, and the next test gets a fresh simulator with the next annotation set applied. The configuration is data, not procedure.

`@Orientation` deserves a small note. The simulator's orientation inference reads the canvas dimensions and infers portrait or landscape from them, which means a wide host window (a developer's external monitor) reads as landscape regardless of what your test expected. The PR adds an explicit-portrait flag and an `isPortrait()` override that honours it, so `@Orientation(PORTRAIT)` and `@Orientation(LANDSCAPE)` do the right thing on any host window size. Tiny detail, important when your CI runner picks an arbitrary window size on a headless display.

## Dependency setup

`junit-jupiter` moves from `test` scope to `provided` scope in the JavaSE port's POM. The reason is that the support classes (the extension, the annotations) need to compile against JUnit, but we did not want the JUnit dependency leaking onto the simulator runtime classpath for apps that do not opt in. Apps that want JUnit tests declare `junit-jupiter` in their own `test` scope; apps that do not are unaffected.

Source level on the JavaSE port is 1.7, so `@SimulatorProperty` is not `@Repeatable`. The container `@SimulatorProperties` annotation takes its place when you want to stack more than one property on the same target. Mild paper cut, very intentional.

## What this does not replace

The `AbstractTest` / `DeviceRunner` framework stays. It is the right answer for on-device integration tests, for tests that run against a Codename One Build Cloud-provisioned cloud device, and for the screenshot test pipeline that lives under `Cn1ssDeviceRunner`. The two frameworks are complementary: JUnit for everything you can validate inside the simulator (which is most things, including most regression bugs), `AbstractTest` for the things you genuinely need a real iPhone or a real Android device for.

## Wrapping up

Standard `@Test`. Real JVM under the hood. Mockito and AssertJ welcome. The visual configuration of the simulator is in the test source code, not in a setup script. Tomorrow's post: the declarative router and the bytecode annotation framework that several of the other posts in this series build on top of.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
