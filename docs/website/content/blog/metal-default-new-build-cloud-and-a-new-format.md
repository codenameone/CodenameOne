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

If you only have thirty seconds, here is what changed this week.

## Metal is the default on iOS

[PR #5065](https://github.com/codenameone/CodenameOne/pull/5065) flips the `ios.metal=true` build hint to the default. New iOS builds now link against `CAMetalLayer` instead of the deprecated `CAEAGLLayer`. I trailed this [three weeks ago in Metal and Skins](/blog/metal-and-skins/), held it back [last week](/blog/nfc-crypto-biometrics-and-build-cloud/) because the regression list still had a couple of items on it, and shipped it this week with the list reading zero.

If you have not rebuilt since this commit, your next cloud build picks Metal up automatically. No hint to add, no setting to change. The build server flipped at the same time so local builds and cloud builds match.

If you need to opt out for any reason, the hint still works in reverse:

```
codename1.arg.ios.metal=false
```

A few things worth a glance on your first Metal build: gradient fidelity (multi-stop, conic, and repeating gradients now hit the GPU directly through [PR #4957](https://github.com/codenameone/CodenameOne/pull/4957)), the colour space (sRGB by default, flip to `displayP3` via `ios.metal.colorSpace` if your assets are wide gamut), and anything that draws `filter: blur(...)` or `backdrop-filter`. Everything else should look unchanged. That is the point.

A specific thank you to the community testers who flipped the hint over the past three weeks, took screenshots, and filed issues against real apps. The Metal default landed in materially better shape than it would have without you.

## The new Build Cloud console is now the default link

The [preview of the new Build Cloud UI](/blog/nfc-crypto-biometrics-and-build-cloud/#a-new-build-cloud-ui--preview) went up last week. The bugs you found are fixed, and as of this PR every Dashboard link on the website now points at the new console:

```
https://cloud.codenameone.com/console/index.html
```

The navigation Dashboard link in the header, the Sign Up CTA on the pricing page, and the entries on the site map all moved. Old bookmarks still work; the legacy console at `https://cloud.codenameone.com/secure/index.html` stays online for the time being so you can fall back to it if something is missing or wrong in the new UI. Please tell us when you hit one of those things, because the goal is to retire the legacy URL eventually and we want it to retire empty.

Historical blog posts that mention the `/secure/` URL in their text were left alone. Those are historical text; rewriting them after the fact would be lying about what we said at the time.

## Upcoming attractions

The following are the deeper posts that will go up over the next week and change. Each item is its own post, its own URL, and its own discussion thread. Dates are best effort. If a post needs another day to read right I would rather hold it than ship it half-baked.

- **On-device debugging on iOS and Android.** This is one I have personally wanted for a long time. Codename One always had on-device debugging in the technical sense; you just had to drop into Xcode or Android Studio and jump through a depressing number of hoops. The two PRs ([#4999](https://github.com/codenameone/CodenameOne/pull/4999) for iOS, [#5012](https://github.com/codenameone/CodenameOne/pull/5012) for Android) wire JDWP through to the real device, so `jdb`, IntelliJ, VS Code, Eclipse, or NetBeans just attaches.
- **WiFi and connectivity APIs in the core.** SSID, BSSID, IP, gateway, scan, connect, Bonjour and mDNS, WiFi Direct, USB host, and `NetworkManager` network-type listeners. All in the framework, no cn1lib. ([#5021](https://github.com/codenameone/CodenameOne/pull/5021).)
- **OIDC and WebAuthn / passkeys in the core.** A first-class identity stack that goes through `ASWebAuthenticationSession` on iOS and Custom Tabs on Android, plus a portable passkey client and Auth0 / Firebase helpers. ([#5018](https://github.com/codenameone/CodenameOne/pull/5018), [#5039](https://github.com/codenameone/CodenameOne/pull/5039).)
- **Share result callbacks.** A way to actually find out whether the user shared something, and where to. ([#5036](https://github.com/codenameone/CodenameOne/pull/5036).)
- **JUnit 5 tests for Codename One apps.** Standard `@Test` methods, run on the simulator's JVM, with `@CodenameOneTest`, `@RunOnEdt`, `@Theme`, `@DarkMode`, and friends. ([#5032](https://github.com/codenameone/CodenameOne/pull/5032).)
- **Declarative router and a bytecode annotation framework.** `@Route("/path")`, deep links from cold and warm starts, `Display.setDeepLinkHandler(...)`, and the bytecode-level annotation processor SPI that the ORM and binder pieces build on top of. ([#5037](https://github.com/codenameone/CodenameOne/pull/5037).)
- **AI / LLM core and cn1libs.** `com.codename1.ai`, `LlmClient` for OpenAI / Anthropic / Gemini / Ollama, a streaming `ChatView`, speech and TTS, the simulator Ollama redirect, and the new ML Kit cn1libs for barcode, document scan, and face. ([#5035](https://github.com/codenameone/CodenameOne/pull/5035), [#5057](https://github.com/codenameone/CodenameOne/pull/5057).)
- **POJO ORM, JSON / XML mapping, and component binding.** `@Entity` / `@Id` / `@Column` for SQLite, `@Mapped` / `@JsonProperty` / `@XmlElement` for the network, `@Bindable` / `@Bind` for components, all generated at build time. No `Class.forName`, no field reflection, obfuscation-safe. ([#5047](https://github.com/codenameone/CodenameOne/pull/5047), [#5062](https://github.com/codenameone/CodenameOne/pull/5062), and a lot of related framework groundwork in [#5055](https://github.com/codenameone/CodenameOne/pull/5055).)
- **SVG and Lottie at build time.** Drop an SVG or a Bodymovin JSON into your project and the build emits a Codename One `Image` subclass that draws via the shape API. SMIL animations honoured, Lottie too. *Metal only* on iOS, because the GL path does not have the shape coverage we need. ([#5042](https://github.com/codenameone/CodenameOne/pull/5042), [#5049](https://github.com/codenameone/CodenameOne/pull/5049), [#5066](https://github.com/codenameone/CodenameOne/pull/5066).)

A short note on [#5055](https://github.com/codenameone/CodenameOne/pull/5055): its headline reads "Improvements to baseline based on porting exercise", and the porting exercise (Immich, the Flutter mobile client, into Codename One) was real. The PR carried a lot of smaller framework additions alongside the ORM-adjacent ones, so it gets pulled into the ORM post rather than living on its own.

## Wrapping up

That is the new format. Short post on Friday; deeper posts during the week; every change in its own place. Please tell me how it lands.

Issue tracker is [here](https://github.com/codenameone/CodenameOne/issues), the discussion forum is [here](https://www.codenameone.com/discussion-forum.html), and the new Build Cloud console is at [`/console/`](https://cloud.codenameone.com/console/index.html). The [Playground](/playground/), [Initializr](/initializr/), and [Skin Designer](/skindesigner/) are all still where they were.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
