---
title: Metal Default, A New Build Cloud, And A New Format
slug: metal-default-new-build-cloud-and-a-new-format
url: /blog/metal-default-new-build-cloud-and-a-new-format/
date: '2026-05-29'
author: Shai Almog
description: The iOS Metal renderer is now the default, the new Build Cloud console is wired into every Dashboard link on the site, and the weekly release blog is moving to a shorter format with deeper follow-up posts during the week.
feed_html: '<img src="https://www.codenameone.com/blog/metal-default-new-build-cloud-and-a-new-format.jpg" alt="Metal Default, A New Build Cloud, And A New Format" /> The iOS Metal renderer is now the default, the new Build Cloud console is wired into every Dashboard link on the site, and the weekly release blog is moving to a shorter format with deeper follow-up posts during the week.'
---

![Metal Default, A New Build Cloud, And A New Format](/blog/metal-default-new-build-cloud-and-a-new-format.jpg)

This week's release post looks different on purpose. The Friday omnibus has been getting longer and longer, and that has been working against us in two ways. SEO ignores 5,000 word pages that cover twelve unrelated topics, so the actual material gets buried instead of indexed against the queries that should find it. And when a single release post covers ten things, it becomes hard to point a colleague at "that one Codename One change from a few weeks ago" without scrolling for ten minutes.

So from this week onwards the Friday post is the short one. A quick set of headline items, a "what is coming next" list, and that is it. The specific features get their own posts over the following days, with their own slugs, their own searchable titles, and their own discussion threads. The weekly post lives at the top of the homepage as the index; the deeper posts back-link to it; and you can read whichever ones are actually relevant to your project.

**Important:** it seems that if developer mode is on in your device you might get an information dialog on the right side of your UI. [This issue](https://github.com/codenameone/CodenameOne/issues/5108) explains how you can turn it off.

If you only have thirty seconds, here is what changed this week.

## Metal is the default on iOS

[PR #5065](https://github.com/codenameone/CodenameOne/pull/5065) flips the `ios.metal=true` build hint to the default. New iOS builds now link against `CAMetalLayer` instead of the deprecated `CAEAGLLayer`. We mentioned this [three weeks ago in Metal and Skins](/blog/metal-and-skins/), decided to push it back by one week [last week](/blog/nfc-crypto-biometrics-and-build-cloud/) because a couple of regressions still needed work, and shipped it this week with that list at zero.

If you have not rebuilt since this commit, your next cloud build picks Metal up automatically. No hint to add, no setting to change. The build server flipped at the same time so local builds and cloud builds match.

If you need to opt out for any reason, the hint still works in reverse:

```
ios.metal=false
```

A few things worth a glance on your first Metal build: gradient fidelity (multi-stop, conic, and repeating gradients now hit the GPU directly through [PR #4957](https://github.com/codenameone/CodenameOne/pull/4957)), the color space (sRGB by default, flip to `displayP3` via `ios.metal.colorSpace` if your assets are wide gamut), and anything that draws `filter: blur(...)` or `backdrop-filter`. Everything else should look unchanged. That is the point.

A specific thank you to the community testers who flipped the hint over the past three weeks, took screenshots, and filed issues against real apps. The Metal default landed in materially better shape than it would have without you.

## The new Build Cloud console is now the default link

The [preview of the new Build Cloud UI](/blog/nfc-crypto-biometrics-and-build-cloud/#a-new-build-cloud-ui--preview) went up last week. The bugs you found are fixed, and as of this PR every Dashboard link on the website now points at the new console:

```
https://cloud.codenameone.com/console/index.html
```

The navigation Dashboard link in the header, the Sign Up CTA on the pricing page, and the entries on the site map all moved. Old bookmarks still work; the [legacy console](https://cloud.codenameone.com/secure/index.html) stays online for the time being so you can fall back to it if something is missing or wrong in the new UI. Please tell us when you hit one of those things, because the goal is to retire the legacy URL eventually.

Historical blog posts that mention the `/secure/` URL in their text were left alone.

## Upcoming attractions

Three deeper posts will follow this one over the next week, each one bundling several related PRs under a single theme so the index stays small. Dates are best effort.

- **Developer workflow (Saturday).** On-device debugging on iOS and Android, and JUnit 5 tests for Codename One apps. Codename One always had on-device debugging in the technical sense; you just had to drop into Xcode or Android Studio and jump through a depressing number of hoops. The new pipeline wires JDWP through to the real device so `jdb`, IntelliJ, VS Code, Eclipse, or NetBeans just attaches. The JUnit half lets you write standard `@Test` methods against the simulator with first-class annotations for the visual configuration (`@Theme`, `@DarkMode`, `@LargerText`, `@Orientation`, `@RTL`). PRs [#4999](https://github.com/codenameone/CodenameOne/pull/4999), [#5012](https://github.com/codenameone/CodenameOne/pull/5012), [#5032](https://github.com/codenameone/CodenameOne/pull/5032).
- **Platform APIs in the core (Monday).** Four things that move from "you need a cn1lib for this" to "it is in the framework": built-in WiFi / Bonjour / USB / network-type APIs, a modern OIDC + WebAuthn passkey identity stack (`ASWebAuthenticationSession` on iOS, Custom Tabs on Android), share-sheet result callbacks, and a `com.codename1.ai` package with `LlmClient` for OpenAI / Anthropic / Gemini / Ollama plus a streaming `ChatView`, `SpeechRecognizer` / `TextToSpeech`, and the new ML Kit cn1libs. All four share the same scanner-driven auto-injection of Android permissions and iOS entitlements that NFC and biometrics moved to two weeks ago. PRs [#5021](https://github.com/codenameone/CodenameOne/pull/5021), [#5018](https://github.com/codenameone/CodenameOne/pull/5018), [#5039](https://github.com/codenameone/CodenameOne/pull/5039), [#5036](https://github.com/codenameone/CodenameOne/pull/5036), [#5035](https://github.com/codenameone/CodenameOne/pull/5035), [#5057](https://github.com/codenameone/CodenameOne/pull/5057).
- **Build-time codegen (Wednesday).** The architectural one. A reusable bytecode `AnnotationProcessor` SPI in the Maven plugin, the declarative router (`@Route("/path")`, deep links, route guards, per-tab navigation shells) that is its first concrete consumer, then a SQLite ORM (`@Entity` / `@Id` / `@Column`), a JSON / XML mapper (`@Mapped` / `@JsonProperty` / `@XmlElement`), a component binder (`@Bindable` / `@Bind`) with field-level validation, and the build-time SVG / Lottie transcoder that emits Codename One `Image` subclasses for every asset in `src/main/svg/` or `src/main/lottie/`. The grab-bag PR ([#5055](https://github.com/codenameone/CodenameOne/pull/5055), driven by porting a substantial mobile client app onto Codename One as the regression fixture) lands here too because the ORM and mapping work share the porting exercise that drove it. PRs [#5037](https://github.com/codenameone/CodenameOne/pull/5037), [#5047](https://github.com/codenameone/CodenameOne/pull/5047), [#5062](https://github.com/codenameone/CodenameOne/pull/5062), [#5055](https://github.com/codenameone/CodenameOne/pull/5055), [#5042](https://github.com/codenameone/CodenameOne/pull/5042), [#5049](https://github.com/codenameone/CodenameOne/pull/5049), [#5066](https://github.com/codenameone/CodenameOne/pull/5066).

## Wrapping up

That is the new format. Short post on Friday; deeper posts during the week; every change in its own place. Please tell us how it lands.

Issue tracker is [here](https://github.com/codenameone/CodenameOne/issues), the discussion forum is [here](https://www.codenameone.com/discussion-forum.html), and the new Build Cloud console is at [`/console/`](https://cloud.codenameone.com/console/index.html). The [Playground](/playground/), [Initializr](/initializr/), and [Skin Designer](/skindesigner/) are all still where they were.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
