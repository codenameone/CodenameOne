---
title: "Funding Open Source Without The Bait And Switch: Analytics, Native Maps, TV And More"
slug: funding-open-source-without-the-bait-and-switch
url: /blog/funding-open-source-without-the-bait-and-switch/
date: '2026-06-26'
author: Shai Almog
description: This week adds a privacy-first analytics API, a pure-vector map engine with pluggable native providers, Apple TV and Android TV support with CSS @media for form factors, rich text and code editors, and device integrity and app review APIs. It is also a good moment to explain how we fund the project without doing the thing open source so often does to you.
feed_html: '<img src="https://www.codenameone.com/blog/funding-open-source-without-the-bait-and-switch.jpg" alt="Funding Open Source Without The Bait And Switch" /> A privacy-first analytics API, vector and native maps, Apple TV and Android TV, rich text and code editors, and device integrity APIs, plus how we fund the project without the usual open source bait and switch.'
---

![Funding Open Source Without The Bait And Switch](/blog/funding-open-source-without-the-bait-and-switch.jpg)

Years ago I wrote a piece called [Open Source Bait and Switch](https://debugagent.com/open-source-bait-and-switch). The short version: a project is released as open source while it has no business model, a community forms around it, and then the bill comes due. The project either rots into abandonware because nobody can afford to maintain it, or it sprouts a home-grown "source available" license that quietly takes back the freedoms it was given on. You have watched this happen. So have I.

We have spent fourteen years trying not to become either of those stories. This is a release post, and most of it is the features that shipped this week, but the thread running through them is worth saying out loud first, because two of this week's headline features, analytics and last week's crash protection, are the clearest example yet of how we intend to keep the lights on without charging you for the freedom you already have.

## The deal we actually offer

Codename One the open source project and Codename One the company are not the same thing, and that distinction is the whole game. The project has to stay genuinely open: it is licensed under the GNU GPL with the Classpath Exception, the same license the JDK ships under, so you can fork it, build closed-source apps on top of it, and run the whole build toolchain yourself. That license choice is the point, not a footnote. The piece I linked above argued that the honest way to fund open source is a strong copyleft license plus a real commercial offering, not a permissive license you quietly relicense once the community is locked in. Codename One has been GPL with the Classpath Exception the entire time. The exception keeps your apps yours; the copyleft keeps a free, forkable Codename One in the world no matter what the company does or who owns it.

The company still has to make enough money to keep paying the people who move the project forward at the pace you have seen this year. That need pulls against the open one, and the usual way companies resolve the tension is to make the open part worse so the paid part looks better. We said years ago in [We Will Not Sabotage Your Code](/blog/we-will-not-sabotage-your-code/) that we would not do that, and the model below is how we avoid it.

The first answer is the build cloud. Compiling your Java to a native iOS binary on our servers costs us real money in machines and maintenance, so charging for build capacity is honest: you are paying for a thing that has a meter on it. But build credits alone do not fund the rate of progress this blog has been documenting, and they never will. We need a second answer that grows.

That second answer is **optional services that sit on top of the open source project and enhance it, without ever standing in your way**. The rule we hold ourselves to is that every one of them ships behind a Service Provider Interface, an SPI, so the open framework has a real seam where any provider can plug in, ours or a third party's. The paid service is one implementation of an open contract, never the only door.

Crash protection last week was the first clear example. Analytics this week is the second, and it is built exactly this way:

{{< mermaid >}}
flowchart TD
    A["Your app calls Analytics.event()"] --> B{"Consent gate<br/>(opt-in by default)"}
    B -->|granted| C["AnalyticsProvider SPI"]
    B -->|denied| Z["Nothing leaves the device"]
    C --> D["GoogleAnalyticsProvider (GA4)"]
    C --> E["MatomoAnalyticsProvider"]
    C --> F["FirebaseAnalyticsProvider"]
    C --> G["CodenameOneAnalyticsProvider<br/>(first-party, part of a paid plan)"]
    style G fill:#1f6feb,color:#fff
{{< /mermaid >}}

You can wire the analytics facade to Google Analytics, to Matomo, to Firebase, or to your own `AnalyticsProvider`, and the framework does not care which. If you pick our first-party provider you get a privacy-oriented analytics backend with the consent handling already done, and you also fund the next port, the next API, the next year of this. That is the entire pitch. It is not a tax on the open source project; it is a better default that happens to also pay for the open source project.

We are going to keep adding services in this shape, and we are genuinely [open to requests](https://github.com/codenameone/CodenameOne/issues) for which ones. If the model makes sense to you, the most useful thing you can do is help us tell people about these services. Promoting them is not a betrayal of the open source ethos; it is the thing that keeps the open source project from becoming one of the two sad endings above.

With that said, here is what shipped.

## A privacy-first analytics API

[PR #5266](https://github.com/codenameone/CodenameOne/pull/5266) replaces the old Google Analytics v1 `AnalyticsService` with the generic provider SPI shown above. You register one or more `AnalyticsProvider` implementations with the `Analytics` facade, and it fans `screen`, `event`, `setUserProperty` and `crash` calls out to all of them, but only after a consent gate that is opt-in by default and persists the user's choice across restarts. The client id is pseudonymous and resettable, and no hardware identifiers are touched.

```java
Analytics.addProvider(new MatomoAnalyticsProvider("https://stats.example.com", 7));
Analytics.setConsent(AnalyticsConsent.builder().analytics(true).build());
Analytics.event(AnalyticsEvent.create("checkout")
        .category("commerce")
        .param("plan", "pro")
        .build());
```

Five providers ship in the box: our first-party `CodenameOneAnalyticsProvider` (batched to the cloud, part of a paid plan), `GoogleAnalyticsProvider` for GA4, the privacy-first non-Google `MatomoAnalyticsProvider`, `FirebaseAnalyticsProvider`, and a `LoggingAnalyticsProvider` for development. The old `AnalyticsService` is still there, deprecated, and now delegates to the new API so existing apps keep working. The full walkthrough is in {{< post-link path="/blog/privacy-first-analytics" text="Monday's post" >}}.

## Maps you control, down to the pixel

[PR #5264](https://github.com/codenameone/CodenameOne/pull/5264) brings mapping back into core and modernizes it, retiring the old tile-based `MapComponent` and the external Google Maps cn1lib. Two components share one `MapSurface` API. The one I am most excited about is `MapView`, a **pure-vector map rendered entirely through `Graphics`**, with a Mapbox Vector Tile engine built on the framework's own `ProtoReader` and `GeneralPath`. There is no native peer, which means no z-order fights, no snapshot-during-animation compromise, and identical rendering everywhere including the simulator and the web. You host your own tile data and metadata, pick a light or dark style, and you control every pixel that gets drawn.

![A pure-vector OpenStreetMap render and the same area in the built-in dark style, both drawn entirely through the Codename One Graphics pipeline with no native peer](/blog/vector-and-native-maps/maps-vector.png)

The second component, `NativeMap`, is the native-provider path for when you want Apple MapKit or Google Maps, and it is wired through an SPI selected by a build hint rather than code. Because the provider is injected at build time, the core and ports carry no map SDK, unused providers cost zero project size, and adding a provider for a device without Google Play, Huawei for instance, is a build-hint change, not a fork. When no provider is configured, `NativeMap` falls back to an embedded `MapView`. The engine and the provider model are covered in {{< post-link path="/blog/vector-and-native-maps" text="Saturday's post" >}}.

## Apple TV, Android TV, and CSS that knows the form factor

[PR #5261](https://github.com/codenameone/CodenameOne/pull/5261) adds Apple TV (tvOS) and Google TV (Android TV) support, modeled on the Apple Watch port from last week. The same `CN.isTV()` branch you would expect, the same single Android APK running on phones, tablets and the television, and on iOS a separate tvOS Xcode target driven by a build hint.

The part with the most day-to-day reach is in the styling layer. The CSS engine now understands device form factors as `@media` variants, so you adapt to the ten-foot living-room view, or to the watch, in your stylesheet instead of in branching code:

```css
@media device-tv {
  Title { font-size: 3rem; }
}
@media device-watch {
  Title { font-size: 0.8rem; }
}
```

That work also finished wiring `@media` for the existing watch port, which never had it. Apple TV builds from the same project once you point `codename1.tvMain` at a tvOS target, and the framework's screenshot suite already renders the full component set on the Apple TV simulator. The form-factor API and the `@media` styling, with real Codename One UI running on the TV, are covered in {{< post-link path="/blog/apple-tv-and-android-tv" text="Tuesday's post" >}}.

## Rich text and code editing

[PR #5272](https://github.com/codenameone/CodenameOne/pull/5272) adds two visual editors. `RichTextArea` is a WYSIWYG HTML editor, bold, italic, lists, links, colors, headings, with `getHtml` and `setHtml`. `CodeEditor` is an IDE-style editor with syntax highlighting for eight languages, a line-number gutter, light and dark themes, bracket and quote auto-close, asynchronous code completion, and diagnostics that render as squiggly underlines with gutter markers and tooltips.

![The CodeEditor with Java syntax highlighting, a gutter, and an async code-completion popup, and the RichTextArea WYSIWYG editor with a formatting toolbar](/blog/rich-text-and-code-editing/components-codeeditor.png)

Both sit on a single `AbstractEditorComponent` with two interchangeable backends: a fully cross-platform engine made of self-contained HTML and JS inside the core jar, and an optional richer native backend a port can supply. The `CodeEditor` is not a toy demo: the Playground already uses it. The deep dive is in {{< post-link path="/blog/rich-text-and-code-editing" text="Sunday's post" >}}.

## Device integrity and app review

Two smaller APIs round out the week, both built in core rather than as cn1libs. [PR #5277](https://github.com/codenameone/CodenameOne/pull/5277) adds `DeviceIntegrity`, a portable runtime self-protection API for high-security apps: Play Integrity and iOS App Attest attestation, root, jailbreak and Frida detection, and an accessibility-service abuse guard, most of it driven by build hints with a runtime API on top. [PR #5268](https://github.com/codenameone/CodenameOne/pull/5268) adds `AppReview`, which uses the platform's native store-review prompt where it exists and falls back to a built-in widget everywhere else, with a feedback split that quietly routes unhappy users to you instead of to a one-star public review.

![The AppReview fallback rating widget, used on platforms without a native in-app review prompt](/blog/device-integrity-and-app-review/app-review-sheet.png)

Both are covered in {{< post-link path="/blog/device-integrity-and-app-review" text="Wednesday's post" >}}.

## The state of the JavaScript port

The Playground is now built with our own JavaScript port. [PR #5250](https://github.com/codenameone/CodenameOne/pull/5250) moved it off the pinned old release and the lagging cloud TeaVM backend onto the local ParparVM `local-javascript` target, the same path the Initializr now uses. The visible payoff is that the Playground tracks the current API directly, so new API demos, including the 3D ones, run in the browser without a special-case build.

Getting here has been whack-a-mole in the most literal sense. The Playground's reflective access registry references nearly the whole API, which exercises paths a normal app never touches, and each one surfaced its own translator bug. Array class literals like `byte[].class` threw `Unsupported ldc constant` because the JS backend only handled object class literals. A `Throwable` caught mid-emit was rethrown only if it was an `Exception`, so an `OutOfMemoryError` could let the translator exit successfully with a truncated, broken bundle. A varargs array of arrays generated malformed `new byte[][len]` instead of `new byte[len][]`. None of these are exotic on their own; the Playground just hits all of them at once.

This is the hardest port we have ever built, and every percentage point of compatibility is earned one fixed bug at a time. The work is ongoing, and the goal is to get the JavaScript port aligned with the others and properly launched in the near future.

## From the community

If you write about Codename One, on a blog, on Stack Overflow, in a forum, this is the season we most appreciate it. Community activity slows over the summer, and the services we described at the top are funded in part by people finding the project in the first place. Pointers to good writing, and new writing of your own, both help.

## Upcoming attractions

A tutorial follows this post each day; each link below goes live on its day:

- **Saturday.** {{< post-link path="/blog/vector-and-native-maps" text="Vector and native maps" >}}. PR [#5264](https://github.com/codenameone/CodenameOne/pull/5264).
- **Sunday.** {{< post-link path="/blog/rich-text-and-code-editing" text="Rich text and code editing" >}}. PR [#5272](https://github.com/codenameone/CodenameOne/pull/5272).
- **Monday.** {{< post-link path="/blog/privacy-first-analytics" text="The privacy-first analytics API" >}}. PR [#5266](https://github.com/codenameone/CodenameOne/pull/5266).
- **Tuesday.** {{< post-link path="/blog/apple-tv-and-android-tv" text="Apple TV and Android TV" >}}. PR [#5261](https://github.com/codenameone/CodenameOne/pull/5261).
- **Wednesday.** {{< post-link path="/blog/device-integrity-and-app-review" text="Device integrity and app review" >}}. PRs [#5277](https://github.com/codenameone/CodenameOne/pull/5277) and [#5268](https://github.com/codenameone/CodenameOne/pull/5268).
- **Thursday.** {{< post-link path="/blog/game-builder-board-game" text="Game Builder tutorial 2, a blackjack card game" >}}. PR [#5253](https://github.com/codenameone/CodenameOne/pull/5253).

## Wrapping up

The issue tracker is [here](https://github.com/codenameone/CodenameOne/issues) and it is the best place to reach us, including for requests on which optional service you would want next. The discussion forum is [here](https://www.codenameone.com/discussion-forum.html), and the Build Cloud console is at [`/console/`](https://cloud.codenameone.com/console/index.html). The [Playground](/playground/), [Initializr](/initializr/), and [Skin Designer](/skindesigner/) are where they have always been.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
