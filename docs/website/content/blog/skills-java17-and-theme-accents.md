---
title: Skills, Java 17, And Theme Accents
slug: skills-java17-and-theme-accents
url: /blog/skills-java17-and-theme-accents/
date: '2026-05-15'
author: Shai Almog
description: Java 17 is the new Initializr default, generated projects ship a vendor-neutral AGENTS.md authoring skill, native themes get a runtime accent palette, plus Metal follow-ups and iOS push that no longer prompts at launch.
feed_html: '<img src="https://www.codenameone.com/blog/skills-java17-and-theme-accents.jpg" alt="Skills, Java 17, And Theme Accents" /> New projects default to Java 17 and bundle a vendor-neutral authoring skill at <code>.agent-skills/codename-one/</code> with a top-level <code>AGENTS.md</code> any agent can discover. Native themes gain a runtime accent vocabulary: <pre>UIManager.getInstance().addThemeProps(override);</pre> retunes every accent-bearing UIID at once. Metal picks up per-axis transform decomposition, an <code>ios.metal.colorSpace</code> hint, and a new matrix-correct <code>translateMatrix</code> API. iOS push permission no longer fires at launch.'
---

![Skills, Java 17, And Theme Accents](/blog/skills-java17-and-theme-accents.jpg)

Last week was Metal and the Skin Designer — two big structural pieces landing the same week. This week the diff is smaller in line count and bigger in surface area: the projects the Initializr hands you have changed. Generate a new app today and it looks meaningfully different from one you generated three weeks ago, and the difference is the part of Codename One a brand new developer actually touches first.

The two headline changes are the default JDK and the bundled agent skill. Both are about the same thing: the generated project should be the most modern thing we know how to ship, on day one.

## Java 17 is the default

[PR #4946](https://github.com/codenameone/CodenameOne/pull/4946) flips the Initializr's default Java version to **Java 17** and drops the *(Experimental)* label that has sat on it since we first added Java 11+ support. Java 8 is still selectable from the radio panel — it is now labelled **Java 8 (Legacy)** — but the preselected choice for a brand new project is Java 17.

A few practical notes on what that actually means:

* **Generated projects compile and run on Java 17 source/target.** Records, pattern matching for `instanceof`, sealed types, `var`, text blocks — all available in your app code from day one. The framework itself still builds with Java 8 source/target for backwards compatibility, so libraries you bring in keep working; what changes is the language level *your* code can use.
* **The Android port handles JDK 17 automatically.** This was the last lingering reason to keep Java 8 as the default. The Maven plugin now reuses the JVM it is already running on when `JAVA17_HOME` is not set, so the canonical "set `JAVA17_HOME` to a separate JDK" step is no longer mandatory — if your Maven is already on Java 17 (which the Initializr-generated project's setup instructions arrange) the build picks it up directly.
* **iOS keeps translating bytecode the same way.** ParparVM does not care which Java language version the source was written in; it ingests bytecode. So Java 17 features work in the iOS build, including text blocks and records, with no port-side changes.
* **The "Experimental" label is gone everywhere.** If you have CI templates that branch on `JavaVersion.JAVA_17_EXPERIMENTAL`, they continue to compile against the legacy constant but the canonical name is now `JAVA_17`. The label your users saw on the radio button changes from "Java 17 (Experimental)" to "Java 17", which is the more important half of this change.

For anyone running the [Initializr](https://start.codenameone.com) today, you will see Java 17 preselected. Pick Java 8 (Legacy) if you have a hard reason to — you still get a working project — but the new default is what we now recommend.

## The Codename One skill

The bigger surface-area change in the same PR is the **Codename One authoring skill** that now ships inside every generated project — and it's vendor-neutral by design. There's an emerging convention called [`AGENTS.md`](https://agents.md) that proposes a single, universal entry-point file at the root of a repository for *any* AI agent — Claude Code, Cursor, Codex, Aider, future tools we haven't heard of — to discover project-specific guidance. Generated projects now ship one, and the actual skill content lives at a vendor-neutral path beside it.

Every project the Initializr generates from today onwards contains:

```
AGENTS.md                                    # universal entry point any agent can discover
.agent-skills/codename-one/
  SKILL.md                                   # canonical top-level cheat sheet
  references/
    android-to-cn1.md
    build-and-run.md
    build-hints.md
    cn1libs.md
    css.md
    debugging.md
    html-css-cheatsheet.md
    java-api-subset.md
    mobile-adaptability.md
    native-interfaces.md
    snapshot-builds.md
    swing-comparison.md
    testing-and-screenshots.md
    ui-components.md
  tools/
    IsApiSupported.java
    IsCssValid.java
    README.md
.claude/skills/codename-one/SKILL.md         # thin stub that redirects to .agent-skills/
```

The top-level `AGENTS.md` is what any agent sees first. It explains in a dozen lines that this is a Codename One project, points at `.agent-skills/codename-one/SKILL.md` as the canonical content, and gives a four-line orientation: where the app source lives, where the CSS lives, the simulator command, the test command. That's enough for an agent that has never heard of Codename One to find its footing without us picking sides on which vendor's loader format to ship.

The canonical content lives under `.agent-skills/codename-one/`. `SKILL.md` is the entry-point cheat sheet, the seven `references/` files are the deeper context the agent pulls in when the conversation actually touches the topic (no point loading 500 lines of CSS notes when the user is asking a `native-interfaces` question), and the two small Java tools under `tools/` are runnable single-file `java`-source-mode utilities: one checks whether a Java API is part of the Codename One subset, the other validates whether a given CSS snippet is one Codename One's CSS compiler will accept.

The `.claude/skills/codename-one/SKILL.md` you also see in the tree is intentionally just a stub. Claude Code's skill picker indexes files at that path with a `name:` / `description:` frontmatter block, so we ship a tiny one purely so the skill shows up by name in `/skills`. The stub body redirects to the same `.agent-skills/codename-one/SKILL.md` that any other agent reads. If a future agent runtime invents its own well-known location, the fix is one more thin stub at that path — the *content* stays in one place.

Why does this skill matter in practice? Two things:

**Codename One is not stock JVM.** The framework targets a Java 5/8-shaped subset of the JDK so that the same bytecode translates cleanly to iOS via ParparVM, to Android via Gradle, to JavaSE for the simulator, and to JavaScript via the JS port. An agent that has only ever read regular Java idioms will routinely suggest APIs that compile against `rt.jar` but don't exist on the device — `java.nio.file`, `java.time`, half of `java.util.concurrent`. The `java-api-subset.md` reference is the small, dense map of what is *actually* on the device, which is what you want a code-writing agent to be holding in its head.

**Codename One CSS is not browser CSS.** Same shape, different runtime. Our compiler accepts a subset that maps to the framework's `Style` model — UIID-keyed rules, theme constants prefixed `@`, the binding vocabulary you'll see in the theme accents section below, and a handful of platform-specific keys like `cn1-derive`. The `css.md` and `html-css-cheatsheet.md` references spell out what works, and `IsCssValid.java` lets the agent verify a proposed snippet without running the simulator. The number of times "the agent wrote me a perfectly normal-looking CSS rule that the compiler silently dropped" came up in our own testing is the entire reason this file exists.

The skill is authored as plain Markdown under `scripts/initializr/common/src/main/resources/skill/` in the repo, then bundled into a `skill.zip` at build time and unpacked into the generated project by `GeneratorModel.addAgentSkillEntries`. The same template-token replacement that rewrites `MyAppName` and `com.example.myapp` in your `pom.xml` runs over the skill files too — when the agent reads an example snippet that says `package com.acme.todo;`, it's because the project is actually called `todo`.

You don't need to use any agent at all to benefit. The skill is plain Markdown, ASCII-only — `.agent-skills/codename-one/SKILL.md` is also one of the better entry-point reads we have for a developer who wants the framework's mental model in one sitting. Open it in any editor and read top to bottom.

There is one small piece of CN1 plumbing worth pointing out, because it is the kind of thing future-you will hit if you try to extend this: Codename One's classloader doesn't tolerate nested resource directories on the JAR classpath the same way a regular JVM does. So the skill on disk lives under `scripts/initializr/common/src/main/resources/skill/`, but the build excludes that directory from the produced JAR and ships only `skill.zip`. The Initializr unpacks the zip at generation time. If you ever wondered why a couple of resource directories in the repo are shipped as zips, that is why.

## Theme accents at runtime

[PR #4884](https://github.com/codenameone/CodenameOne/pull/4884) is the change I'm most pleased with this week, because it closes a gap that has been quietly annoying me ever since we shipped the new iOS Modern and Material 3 themes [two weeks ago](/blog/liquid-glass-material-3-modern-native-themes/).

When we first introduced those themes, "rebrand the app to your own colours" was a forking exercise: copy the native theme's `theme.css`, change `#007aff` to your magenta, recompile. That works in principle but the native themes ship inside the framework build, so forking is not actually something app developers can do from inside their project — you'd have to fork the framework. We knew it at the time. We shipped the themes anyway because the alternative was holding everything back on a plumbing problem.

The plumbing problem is now solved. The shipped native themes still source-author with `var(--accent-color, fallback)` so the .css file reads cleanly, but the CSS compiler now additionally emits a `@cn1-bind:<UIID>.<key>=accent-color` constant alongside every affected style key. The .res file ships with metadata that says "Button.fgColor follows accent-color, FloatingActionButton.bgColor follows accent-color, RaisedButton.bgColor follows accent-color, ..." and the framework's `UIManager.buildTheme()` gains an `applyThemeBindings()` pass that fans an override out to every bound key in one shot.

Translated into something you actually run, the day-to-day usage is this:

```java
Hashtable override = new Hashtable();
override.put("@accent-color",       "ff2d95");
override.put("@accent-color-dark",  "ff2d95");
override.put("@accent-pressed-color", "c71a75");
override.put("@accent-on-color",      "ffffff");
// Material 3 also has a separate "container" tonal pair; iOS ignores
// it (no bindings reference it) so setting it unconditionally is safe.
override.put("@accent-container-color",     "ff2d95");
override.put("@accent-on-container-color",  "ffffff");
UIManager.getInstance().addThemeProps(override);
Form.getCurrentForm().refreshTheme();
```

One call. Every accent-bearing UIID in your app — `Button.fgColor`, `RaisedButton.bgColor`, `CheckBox.selected`, `RadioButton.selected`, `OnOffSwitch.fgColor`, `BackCommand`, `TitleCommand`, `FloatingActionButton.bgColor`, and all the `.press` / `.dis` state variants — retunes to magenta. Light and dark stay independent (`@accent-color` vs `@accent-color-dark`); leaving one side alone just leaves it on its baked-in default. Values can be passed with or without the leading `#` and in any case — `"ff2d95"`, `"#FF2D95"`, and the 3-digit shorthand `"#f0a"` are all accepted.

There is also a static path that the same machinery supports. If you don't need to swap palettes at runtime — you just want your app to launch in your brand colours — redeclare the variable inside the `#Constants` block of your own `theme.css`:

```css
#Constants {
    includeNativeBool: true;
    darkModeBool: true;

    /* The compiler exports every --name in #Constants as the
       matching @name theme constant, which feeds the same
       binding pass that the runtime override uses. */
    --accent-color: #ff2d95;
    --accent-color-dark: #ff2d95;
    --accent-pressed-color: #c71a75;
}
```

Partial overrides are fine — anything you don't redeclare stays at the framework default.

### The point

I want to single out one sentence from the Native Themes chapter of the developer guide, because it's the part of this change that I think actually matters:

> When the resolved theme constants pick up a new `@accent-color` value (whether from your CSS or via runtime `UIManager.addThemeProps`), the framework fans the override out to every bound UIID at once — no per-UIID rule duplication, no theme recompile.

That sentence is the whole reason we built this. Every previous "rebrand to your own colours" workflow in Codename One has involved either listing every UIID in your `theme.css` and duplicating the framework's accent logic per-state-per-mode, or forking the native theme and recompiling. Both work, both have shipped real apps, both are roughly 200 lines of CSS for what should be five colours.

This change moves the *binding* (UIID X follows accent Y) into the .res metadata once, at the framework level, and lets your app supply the *colour* in five lines. That is the core value proposition: the parts of theming that don't change per app — which UIIDs participate in the "accent" palette, which states they expose, which dark-mode counterparts they have — live inside the framework and stay there. The parts that *do* change per app — actual colours, in your brand — live in your project as five `@accent-*` constants and nothing else.

The new vocabulary is documented in detail in the developer guide's [Native Themes chapter](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Native-Themes.asciidoc#accent-palette-override), including the iOS and Android constant tables, the per-state coverage, and the small set of cases where the binding system intentionally doesn't apply (the iOS `Switch` green stays the system green; the binding system is for *accent* colours, not every colour on screen).

Concrete example: an in-app dark/light brand toggle. Until this week that was forty lines of `addThemeProps` calls, one per UIID-and-state. Today it's the snippet above, called from the toggle's action listener. The Magenta-over-iOS-Modern light/dark screenshot pair from the `PaletteOverrideThemeScreenshotTest` ([same harness we shipped two weeks ago](/blog/liquid-glass-material-3-modern-native-themes/#runtime-palette-overrides)) keeps it honest in CI — the test now uses the same `@accent-*` constants that this section documents, and both `iOSModernTheme.res` and `AndroidMaterialTheme.res` are regenerated through the new binding-aware compiler in the same PR.

## Metal: nuance is now the work

Last week was *shipping* the Metal renderer. This week is the second-week curve: every shape of bug we can find in the new pipeline, characterised, fixed, regression-tested. Three PRs landed against that goal, plus a new API on `Graphics` that I think is going to quietly pay for itself many times over.

### Per-axis scale decomposition (#4939, fixes #3302)

The headline Metal fix this week is the alpha-mask path under non-uniform scale. Long-standing issue [#3302](https://github.com/codenameone/CodenameOne/issues/3302) had a clear repro: `g.translate + g.scale(sx, sy) + fillShape` with `sx != sy` produced shapes that visibly drifted off the axis-aligned `drawRect` / `drawLine` siblings the framework would emit alongside them. Tilted triangles inscribed in rectangles escaped their bounding rect.

The cause: the legacy alpha-mask path rasterised the shape at a uniform scale (the diagonal ratio `h2/h1`) and then stretched the resulting texture non-uniformly through the GPU matrix to recover the requested aspect. The bbox math is exact in real numbers, but the texture is *pixel-rounded* at the intermediate uniform scale, so the stretch drifts the rasterised shape off the pixel grid that `drawRect` / `drawLine` are already on.

The fix is the kind of small change you only land after you've stared at the symptom for a while. Factor the user transform's 2x2 linear part by taking the column norms as `(sx, sy)`. Rasterise the path at `S(sx, sy)` — so the per-axis stretch happens at rasterisation time, on the CPU, against a vector path, not against a pixel grid. Then apply only the residual `transform * S(1/sx, 1/sy)` on the GPU. The residual is pure rotation (and shear in the worst case), so no per-axis stretch happens at sample time, and the alpha-mask texture lands on the same pixel grid as its `drawRect` siblings.

Stroke widening and the radial-gradient bbox use `sqrt(sx*sy)` so the on-screen stroke matches the legacy uniform behaviour when `sx == sy` — the old behaviour stays byte-identical for the symmetric scale case the test suite already covers.

The change is gated to Metal: the GL ES2 path keeps its legacy `h2/h1` branch so the existing GL goldens are byte-identical. A new `InscribedTriangleGrid` screenshot test was registered with `Cn1ssDeviceRunner` that exercises `(sx, sy)` in `{1, 2}` cells under `translate + scale + drawRect + fillShape + drawShape` — the inscribed-triangle property is now visually verifiable in CI.

### Clip-under-rotation diagnostic (#4924, towards #3921)

[PR #4924](https://github.com/codenameone/CodenameOne/pull/4924) is the kind of PR that doesn't fix a bug, it *localises* one. Issue [#3921](https://github.com/codenameone/CodenameOne/issues/3921) is "clip-under-rotation behaves wrong on some ports", and that report is already entangled with a `getClip / setClip(int[])` round-trip limitation that the reporter himself called out as a separate issue.

To split the two, we shipped a screenshot test that uses *only* `pushClip` / `popClip` and `rotateRadians` — no `getClip`, no `setClip(int[])`. The clip becomes non-axis-aligned via `clipRect` inside a 30-degree rotation, which is what forces the framework through its polygon-clip branch. The expected visual outcome is a 30-degree-tilted red fill that overlaps the navy outline at two diagonal corners and falls short at the other two. Two distinguishable failure modes are pre-labelled in the PR: the clip widened to its axis-aligned bbox (red exactly matches the navy outline), or the polygon clip dropped entirely (red fills the whole cell).

When the iOS Metal cell of this test renders, we know within a glance which of the three behaviours we are looking at. The expected-failure cell is also a hypothesis: `ClipRect.m`'s polygon initialiser stores `x = y = w = h = -1`, and the Metal execute path then calls `CN1MetalSetScissor(0, 0, -2, -2)`, whose `width <= 0 / height <= 0` branch sets the scissor to the full framebuffer instead of the intended polygon. If the screenshot confirms the hypothesis, the fix is a one-line replacement of the polygon-scissor fallback. Either way, future-us no longer have to chase this through a sweep of "is it the clip API or is it the renderer".

### iOS Metal colour space hint (#4909, fixes #4908)

[PR #4909](https://github.com/codenameone/CodenameOne/pull/4909) adds an `ios.metal.colorSpace` build hint. Until this week, the Metal layer's `CAMetalLayer.colorspace` was hard-coded to sRGB. For most apps that is the right call — sRGB is what your existing assets are authored in. But on iPhone XR and later, Apple's screens are wide-gamut (Display P3), and a marketing-led brand that ships P3 artwork was visibly losing saturation by being routed through the sRGB pipeline.

Accepted values are `sRGB` (default), `displayP3`, `deviceRGB`, `linearSRGB`, `extendedSRGB`, `extendedLinearSRGB`, and `none` (leaves the layer's colorspace unset and lets iOS pick the system default). Set it in `codenameone_settings.properties`:

```
codename1.arg.ios.metal.colorSpace=displayP3
```

The hint is dormant when `ios.metal=false`, so existing GL builds are unchanged. Unrecognised values produce a warning log and fall back to sRGB, so a typo can't accidentally produce a build that won't launch. Documented under [Working-With-iOS.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Working-With-iOS.asciidoc).

### The new `translateMatrix` API

The Inscribed-Triangle-Grid test in #4939 also surfaced something I think is worth pulling out as its own feature, because it's been a quiet papercut in the `Graphics` API for years.

`Graphics.translate(int, int)` does not compose into the affine transform the way `scale()` and `rotateRadians()` do. It accumulates into a per-Graphics integer offset that is added to draw coordinates *before* the impl matrix is applied. That's a holdover from the very first version of the framework, when `Graphics` did not have a matrix at all. Today the consequence is surprising: a subsequent `g.scale(sx, sy)` multiplies the integer translate too, which means the same code produces visibly different positions depending on whether you scale before or after you translate.

The new `Graphics.translateMatrix(float, float)` composes the translation directly onto the impl matrix, in the same way `scale` and `rotateRadians` already do. The result is "post-multiply translate onto the current transform" semantics across iOS (both GL and Metal), JavaSE, Android, and the JavaScript port. Same code, same on-screen position, whether you're drawing into a `Form`'s `Graphics` or a mutable `Image`'s `Graphics`.

```java
// Matrix-correct composition. Use this when you want translate to
// behave like scale and rotate -- composed into the affine transform.
g.translateMatrix(centerX, centerY);
g.rotateRadians(angle);
g.scale(sx, sy);
g.translateMatrix(-centerX, -centerY);
g.fillShape(path);

// Legacy integer accumulator. Still here, still works, still
// supported -- just be aware it is not part of the matrix.
g.translate(deltaX, deltaY);
```

For app code that's writing affine-transform pipelines — the "translate to pivot, rotate, scale, translate back" idiom from Java2D and AWT — this is the API you want. `isTranslateMatrixSupported()` returns true on every modern port; on the legacy JavaScript port (where the impl genuinely cannot, the matrix lives elsewhere) it returns false and the call falls back to the integer `translate(int, int)` so apps don't silently render at the wrong position.

The old `translate(int, int)` is not deprecated and is not going anywhere — half the framework's internal scrolling code is built on it and that code is fine. The new method is the right one to reach for in *new* drawing code, particularly anything that combines translate with scale or rotate.

## String API: `replace(CharSequence, CharSequence)`, `replaceAll`, `replaceFirst`

[PR #4893](https://github.com/codenameone/CodenameOne/pull/4893) closes a long-standing gap reported in issue [#4878](https://github.com/codenameone/CodenameOne/issues/4878). The JDK 1.5+ overload of `String.replace` that takes `CharSequence` arguments — the one nearly every modern Java tutorial reaches for — was missing from the Codename One subset. So was `String.replaceAll(String, String)` and `String.replaceFirst(String, String)`. Code that depended on them compiled fine against the JDK at build time and then silently produced wrong output (or threw `NoSuchMethodError`) on the device.

All three are now wired in:

* **`String.replace(CharSequence, CharSequence)`** has a real implementation in `vm/JavaAPI` (so iOS gets it through ParparVM) and a matching stub in `Ports/CLDC11` matching the file's stubs-only convention.
* **`String.replaceAll(String, String)`** and **`String.replaceFirst(String, String)`** are wired through the bytecode-compliance rewriter to a new `JdkApiRewriteHelper.replaceAll`/`replaceFirst` pair that delegates to the existing `RE` regex engine. Same pattern we've been using for years on `String.split` — the rewriter rewrites the call at translation time so the device gets a regex-backed implementation without dragging `java.util.regex` onto the device.
* **`BytecodeComplianceMojoTest`** gains two new cases covering the `replaceAll` and `replaceFirst` rewrite rules so the compliance pipeline can't regress.

Tactically this is a small change. Strategically it is a noticeable improvement in how often "I copied a snippet from Stack Overflow and it didn't work on iOS" turns into a real bug for an app developer. Three of the most-reached-for `String` methods in modern Java are now part of the on-device API.

## iOS push permission no longer fires at app launch

[PR #4894](https://github.com/codenameone/CodenameOne/pull/4894) fixes issue [#4876](https://github.com/codenameone/CodenameOne/issues/4876), which is one of those bugs where the right fix is mostly about *when* something happens rather than *whether* it happens.

The setup: with `ios.includePush=true`, the framework used to call `requestAuthorizationWithOptions` from `application:didFinishLaunchingWithOptions:`. That meant the iOS system permission dialog — "AppName Would Like To Send You Notifications" — fired as soon as the app finished launching, before the user had seen *any* of your screens. There is no good way to recover from a "Don't Allow" tap at that point. The user hasn't experienced the app yet, doesn't know why notifications would matter, and tapping Don't Allow is the path of least resistance. Once denied, re-prompting requires sending the user out to the Settings app.

The fix moves the prompt to the natural points it should already have been at:

* **`Push.register()`** triggers the system prompt (this code path already requested permission inside `IOSNative.m`; we just stopped firing it ahead of time).
* **`LocalNotification.schedule()`** also triggers the system prompt, via a new `requestAuthorizationWithOptions` call in `sendLocalNotification`.

Same flow Android has been on for years — `POST_NOTIFICATIONS` is requested when `registerPush` / `scheduleLocalNotification` runs, not at launch. The practical consequence for your app is that you can now show your own rationale screen — a one-card "we'd like to ping you when your order ships" — *before* the system dialog fires. The accept rate difference is real, and well-documented across the iOS dev community.

If you have an app that genuinely needs the legacy launch-time behaviour — there are a few — a backwards-compatibility build hint restores it:

```
codename1.arg.ios.notificationPermissionAtLaunch=true
```

That uncomments a `CN1_NOTIFICATION_PERMISSION_AT_LAUNCH` macro in `CodenameOne_GLAppDelegate.h` which guards the re-introduced `requestAuthorizationWithOptions` block in `CodenameOne_GLAppDelegate.m`. Default is `false`, so existing apps that did not opt in pick up the new (better) behaviour on next rebuild. Documented in [Push-Notifications.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Push-Notifications.asciidoc).

Two things to flag explicitly for anyone updating an existing iOS app:

1. **If your app was relying on the launch-time prompt happening automatically**, your prompt now never fires unless `Push.register()` or `LocalNotification.schedule()` is invoked somewhere. That's almost certainly what you want, but check your onboarding flow to make sure the call lands.
2. **The cloud-side build server change shipped as [BuildDaemon #71](https://github.com/codenameone/BuildDaemon/pull/71)**. If you build locally you already have everything; if you build on our build server, the change is live there too.

## Wrapping up

The Metal port's first week was the big swing. This week was the follow-up: per-axis scale decomposition for non-uniform transforms, a screenshot test that localises the clip-under-rotation question, the colour-space hint that wide-gamut apps have been quietly asking about, and a new matrix-correct `translateMatrix` API that makes the rest of `Graphics` behave less surprisingly. None of those alone is a headline. Together they are what "Metal is mature" looks like a week into the rollout. And as a reminder: **flip `ios.metal=true` on your real app this week** — the default flip is days away and we'd rather find any remaining edge case against your screens than against the install base on launch day.

The Initializr changes — Java 17 as the default, the bundled vendor-neutral authoring skill — are the part of the diff that a new developer will see first, and the part that an existing developer will pick up the next time they generate a project from scratch. Open `.agent-skills/codename-one/SKILL.md` in any project you generate today; even if you don't use an agent, it is a reasonably tight tour of the framework. The top-level `AGENTS.md` is twelve lines — read that one even sooner.

The accent palette work is the small change I'm most pleased with — five constants, one `addThemeProps` call, every accent-bearing UIID retunes at once, light and dark independent. Try it on your own theme this week and let us know what you find.

A specific thank-you this week to the reporter on [#3302](https://github.com/codenameone/CodenameOne/issues/3302) for sticking with the inscribed-triangle bug for as long as the GL pipeline was the only target we had, **Durank** for the iOS push permission report on [#4876](https://github.com/codenameone/CodenameOne/issues/4876), and the reporter on [#4878](https://github.com/codenameone/CodenameOne/issues/4878) who flagged the missing `String.replace(CharSequence, CharSequence)` — that one had been sitting in the gap for a long time.

Issue tracker is [here](https://github.com/codenameone/CodenameOne/issues), the [Playground](/playground) and [Initializr](/initializr/) are the easiest places to poke at the new defaults, and the [Skin Designer](/skindesigner/) from last week is still there if you have a device shape you need a skin for.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
