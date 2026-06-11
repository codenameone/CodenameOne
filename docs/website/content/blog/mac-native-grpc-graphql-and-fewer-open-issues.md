---
title: Mac Native Builds, Live Protocols, And Open Issues Under 350
slug: mac-native-grpc-graphql-and-fewer-open-issues
url: /blog/mac-native-grpc-graphql-and-fewer-open-issues/
date: '2026-06-05'
author: Shai Almog
description: The open issue count dropped below 350 after a push through the oldest reports, and the same week brought native Mac builds, WebSockets in the core, gRPC and GraphQL integration, a new advertising API, and richer background work.
feed_html: '<img src="https://www.codenameone.com/blog/weekly.jpg" alt="Mac Native Builds, Live Protocols, And Open Issues Under 350" /> The open issue count dropped below 350 after a push through the oldest reports, and the same week brought native Mac builds, WebSockets in the core, gRPC and GraphQL integration, a new advertising API, and richer background work.'
---

![Mac Native Builds, Live Protocols, And Open Issues Under 350](/blog/weekly.jpg)

Our focus was all over the place this week with work that targeted many different directions: desktop, monetization, communication, media, and more. This fits with our roadmap of one platform that delivers the promise Java never delivered: WORA for Everything Everywhere. 

But before we dig into the new features, there's one number I'm particularly proud of…

## Open issues are under 350

The open issue count is now below 350 (332 at the moment of this writing). That is the result of a deliberate pass through the tracker, closing things that were already fixed, reproducing the ones that were not, and fixing a batch of them outright. Some of the reports we closed this week had been open since 2015. We got a little side-tracked in the process (it is hard to read an old report without wanting to fix it on the spot), but the direction is set: we want this number to keep dropping, and by a lot.

We went over the issue tracker starting with the oldest issues and working our way back. You may recall that when we started tracking this, going under the 500-issue mark was a major milestone and that was just a few weeks back!

## What shipped this week

Every one of the bigger items has its own deep-dive tutorial. Here is the tour, with the links to the full posts.

### Your app is now a native Mac app

One build hint, `macNative.enabled=true`, takes the exact same project through the iOS pipeline and emits a **real** native Mac app: no bundled JVM, the same Metal renderer and battle-tested ParparVM output as the iOS target. It pairs with a deeper desktop integration layer (native title bar, native menu bar, interactive scrollbars, desktop notifications), so a desktop target finally stops feeling like a phone in a window. This is the same Java code that produces the iOS and Android builds, running as a native Mac app:

![A Codename One app running as a native Mac app](/blog/mac-native-builds-and-desktop-integration/mac-app.png)

The full tutorial, including the new desktop menu and shortcut APIs and the Mac signing hints, is in [Your Codename One App, Now A Native Mac App](/blog/mac-native-builds-and-desktop-integration/).

### WebSockets, gRPC, and GraphQL in the core

`com.codename1.io.WebSocket` is now part of the framework with no `cn1lib` required, implemented natively on every port. A live connection is a fluent one-liner:

```java
WebSocket ws = WebSocket.build("wss://chat.example.com/room/general")
    .onConnect(() -> Log.p("connected"))
    .onTextMessage(text -> Display.getInstance()
        .callSerially(() -> addBubble(text, false)))
    .connect();
```

On top of that, `cn1:generate-grpc` and `cn1:generate-graphql` join `cn1:generate-openapi`, turning a proto file or a GraphQL schema into a typed client with no `protoc` and no HTTP plumbing. GraphQL subscriptions ride the new core WebSocket support, and enum binding landed in the JSON/XML mapper alongside them. The hands-on tutorial that builds a live chat and both typed clients is [WebSockets, gRPC, And GraphQL In The Core](/blog/websockets-grpc-and-graphql/).

### A new advertising API

Advertising support had quietly rotted across three dead-end legacy mechanisms. A pluggable, format-complete `com.codename1.ads` subsystem replaces all of them, with a modern AdMob reference provider, GDPR consent and iOS App Tracking Transparency built in, and a simulator placeholder provider so you can exercise every format without a device. A rewarded ad, the format people most often ask about, is now this short:

```java
RewardedAd ad = new RewardedAd("your-rewarded-ad-unit-id");
ad.setAdListener(new AdListener() {
    public void onLoaded() {
        ad.show(reward ->
            grantCoins(reward.getType(), reward.getAmount()));
    }
});
ad.load();
```

The full story, including banners, native ads, app-open ads, and the provider SPI, is in [A New Advertising API, Built From The Ground Up](/blog/modern-advertising-api/).

### Background execution and push

Constraint-based background work, foreground services, push topics, shared-content handling, and a much richer local notification API, all of it usable in the simulator so you can debug these flows on your desktop. You describe what the work needs, not when to poll:

```java
WorkRequest req = WorkRequest.builder("daily-sync", SyncWorker.class)
    .setRequiresNetwork(true)
    .setRequiresCharging(true)
    .setPeriodic(6 * 60 * 60 * 1000L)
    .build();
BackgroundWork.schedule(req);
```

The walkthrough, from progress notifications and inline replies to push topics and shared content, is [Background Work, Push Topics, And Richer Notifications](/blog/background-execution-and-push/).

### Agent skills and a simpler Initializr

Covered in this post right below, since it is more about the tooling around your project than the framework inside it.

## Building screens from screenshots, and a simpler Initializr

Generated apps ship with a `codename-one` agent skill under `.claude/skills/`, so an AI agent working in your project already knows how to build, test, and screenshot a Codename One UI. [PR #5161](https://github.com/codenameone/CodenameOne/pull/5161) teaches it the single most common design task: "make this screen look like this mockup."

The hard part of that task was that the agent had no objective measure of how close it had gotten. The existing screenshot test only compares a render to a baseline the system produced itself, which measures consistency, not correctness. The new `CompareToMockup` tool is a single-file, pure-JDK CLI that scores a render against a target image and prints a similarity percentage: a `STRUCTURAL` score (an SSIM-style perceptual measure, robust to font and anti-aliasing noise against a vector mockup) and a `PIXEL` score (the fraction of pixels within the framework's own three-channel "same pixel" tolerance). It has a region mode so device chrome does not sabotage the score, a `--diff` heatmap, and a `--min` gate. That gives the agent a real signal: render, score, read the heatmap, adjust, repeat until the number stops climbing. A companion `DesignImport` tool turns a Figma, Sketch, or Adobe XD file (and, after [PR #5168](https://github.com/codenameone/CodenameOne/pull/5168), the `tokens.css` from an HTML or React mockup) into a starter `theme.css` so the agent adjusts rather than starting from a blank page. The skill can also update itself now through an `UpdateSkills` tool, so a project generated months ago can pull the current guidance instead of carrying a frozen copy.

Agents can now automatically update the skills to the latest versions and also describe the content of Codename One GUIs. This is valuable as they review their work and don't need to use vision which is both more expensive and not as accurate. We also added the ability to check component alignment, which is often a problem that LLMs find difficult. There is also a new linter that I think we should expose to the human developers as well in the future. Right now you can see all of these tools and use them just as an agent would, but they are more CLI oriented.

[PR #5168](https://github.com/codenameone/CodenameOne/pull/5168) also rebuilt the Initializr, the tool that scaffolds a new project, around the Codename One design language, and trimmed it so it is easier to approach. It leads with the essentials (main class, package, and a Java or Kotlin toggle) and tucks IDE, localization, Java version, and current settings into collapsible cards, with a live preview and a single generate bar at the bottom. The four-template picker became the Java/Kotlin toggle, and the accent, rounded-buttons, and custom-CSS controls were dropped. The project model behind it is unchanged, so generated projects are the same; this is purely about lowering the barrier to getting started.

![The redesigned Initializr](/blog/mac-native-grpc-graphql-and-fewer-open-issues/initializr.png)

## Smaller fixes worth knowing about

Several of these came straight out of the issue-tracker pass:

- **`cubic-bezier()` motion now matches CSS.** [PR #5122](https://github.com/codenameone/CodenameOne/pull/5122) fixes [#1524](https://github.com/codenameone/CodenameOne/issues/1524): `Motion.createCubicBezierMotion` was feeding its control points into a 1D polynomial directly, so the curve did not match the CSS `cubic-bezier()` it was modeled on. It does now so animations might act differently in some cases.
- **Always-tensile on the X axis.** [PR #5112](https://github.com/codenameone/CodenameOne/pull/5112) closes [#1399](https://github.com/codenameone/CodenameOne/issues/1399), the next-oldest open issue (filed March 2015): `setAlwaysTensile(true)` now applies horizontally as well as vertically. This means you would see the rubber-band effect also on X axis scrolling.
- **Validation highlights on tap-away.** [PR #5123](https://github.com/codenameone/CodenameOne/pull/5123) closes [#1459](https://github.com/codenameone/CodenameOne/issues/1459): a field with an invalid value is now highlighted when you tap into a different field, not only when you press the virtual keyboard's next/enter.
- **EncodedImage.dispose() actually frees memory.** [PR #5127](https://github.com/codenameone/CodenameOne/pull/5127) makes `dispose()` release the decoded image and the encoded bytes (it was a no-op before) and adds `isDisposed()`. Closes [#3733](https://github.com/codenameone/CodenameOne/issues/3733).
- **NetworkManager.ping().** [PR #5130](https://github.com/codenameone/CodenameOne/pull/5130) adds `ping(url, timeoutMillis)`, a real server-reachability probe to pair with the device-side `isConnected()`. Closes [#3669](https://github.com/codenameone/CodenameOne/issues/3669).
- **ImageViewer drag bubbling.** [PR #5132](https://github.com/codenameone/CodenameOne/pull/5132): a vertical drag on an `ImageViewer` at zoom 1 now scrolls the parent container instead of being swallowed. Closes [#3700](https://github.com/codenameone/CodenameOne/issues/3700). This means you can now include many image viewers in a scrollable Y container.
- **Graphics.isVisible().** [PR #5129](https://github.com/codenameone/CodenameOne/pull/5129) adds a clip-intersection primitive so a zoomed canvas can cull off-screen content and skip the decode/scale. Closes [#3846](https://github.com/codenameone/CodenameOne/issues/3846).
- **Screenshot block now covers peers.** [PR #5107](https://github.com/codenameone/CodenameOne/pull/5107): `ios.blockScreenshotsOnEnterBackground=true` was hiding the render surface but leaving peer components (such as a `BrowserComponent`'s `WKWebView`) visible in the app-switcher snapshot. Fixed.
- **Better site search.** [PR #5090](https://github.com/codenameone/CodenameOne/pull/5090) sorts the on-site search index newest-first and stops the giant developer-guide page from crowding out every result. We also have dates visible next to blog post results in the search and a new highlight explaining we don't search the developer guide/Javadoc. 

## A note on contributions

We stopped accepting community pull requests. We want to be precise about why, because it would be easy to read more into this than is there.

This is **not** about AI-generated PRs. We are not policing how a contribution was written. 

The real reason is mechanical: our CI does not run correctly against pull requests from forks. The screenshot pipeline, the device runners, and the protocol tests all need credentials and a setup that a forked PR cannot get, so a community PR cannot actually be validated by the same gates we hold our own work to. This isn't something we can easily solve without introducing major security vulnerabilities to our process. Recently, a change that looked completely safe slipped through and triggered a CI regression that took the builds down. That was on us, not on the contributor, but it convinced us that merging code we cannot fully run is the wrong trade.

So the door is open the other way. **Please keep filing issues.** A clear issue with a test case is genuinely no trouble for us to pick up, and as the number above shows, we are actively working the tracker. If you have a fix in mind, describe it in the issue, attach the failing case, and we will carry it through CI ourselves.

To everyone who has sent us patches over the years, and especially the people who contributed recently: thank you. The effort was real and appreciated, and this decision is about our pipeline, not your work. 

## Upcoming attractions

Four deep-dive tutorials follow this one, one per day:

- **Saturday.** Native Mac builds and deeper desktop integration. PRs [#5053](https://github.com/codenameone/CodenameOne/pull/5053), [#5136](https://github.com/codenameone/CodenameOne/pull/5136), [#5170](https://github.com/codenameone/CodenameOne/pull/5170).
- **Sunday.** WebSockets, gRPC, and GraphQL in the core. PRs [#5133](https://github.com/codenameone/CodenameOne/pull/5133), [#5099](https://github.com/codenameone/CodenameOne/pull/5099), [#5141](https://github.com/codenameone/CodenameOne/pull/5141).
- **Monday.** The new advertising API. PR [#5169](https://github.com/codenameone/CodenameOne/pull/5169).
- **Tuesday.** Background work, push topics, and richer notifications. PR [#5142](https://github.com/codenameone/CodenameOne/pull/5142).

## Wrapping up

The issue tracker is [here](https://github.com/codenameone/CodenameOne/issues) and it is the best place to reach us right now. The discussion forum is [here](https://www.codenameone.com/discussion-forum.html), and the Build Cloud console is at [`/console/`](https://cloud.codenameone.com/console/index.html). The [Playground](/playground/), [Initializr](/initializr/), and [Skin Designer](/skindesigner/) are where they have always been.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
