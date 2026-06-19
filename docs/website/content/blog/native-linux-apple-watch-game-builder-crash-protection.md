---
title: "Native Linux, Apple Watch, A Game Builder And Crash Protection"
slug: native-linux-apple-watch-game-builder-crash-protection
url: /blog/native-linux-apple-watch-game-builder-crash-protection/
date: '2026-06-19'
author: Shai Almog
description: This week adds a native Linux desktop port, an Apple Watch and Wear OS port, a visual Game Builder with a high-level gaming API, and seamless crash protection that symbolicates native crashes and files them as GitHub issues. We also rebuilt the build cloud from the ground up.
feed_html: '<img src="https://www.codenameone.com/blog/native-linux-apple-watch-game-builder-crash-protection.jpg" alt="Native Linux, Apple Watch, A Game Builder And Crash Protection" /> This week adds a native Linux desktop port, an Apple Watch and Wear OS port, a visual Game Builder with a high-level gaming API, and seamless crash protection that symbolicates native crashes and files them as GitHub issues.'
---

![Native Linux, Apple Watch, A Game Builder And Crash Protection](/blog/native-linux-apple-watch-game-builder-crash-protection.jpg)

This week brings a native Linux desktop port, an Apple Watch and Wear OS port, a visual Game Builder with a high-level gaming API, and a new crash-protection system, with a tutorial following each one over the coming days. There is also a large piece of work that you mostly should not have noticed: we rebuilt the build cloud, and that rebuild caused a few failed builds along the way. More on that below.

## A native Linux desktop port

[PR #5239](https://github.com/codenameone/CodenameOne/pull/5239) adds a native Linux port, the structural twin of the native Windows port we shipped last week. The same ParparVM pipeline that turns your Java/Kotlin bytecode into C, here targeting Linux through GTK3, Cairo, Pango and GdkPixbuf for rendering, OpenGL ES for 3D, GStreamer for media and camera, and WebKitGTK for the browser component. There is no JVM on the target machine: it is a single self-contained ELF you launch like any other program.

We expected this to be painful, and parts of it were, but Linux turned out to be mostly developer friendly. A non-trivial app fits in 5MB, and on my Linux machine it started faster than most of the GNOME native apps already installed; thanks to the default material design theme it tends to look better too:

![A Codename One app rendering natively on Linux via GTK3 and Cairo](/blog/java-to-a-native-linux-app/chatview-linux.png)

Most things just work, including the camera and 3D. The hard part of Linux is not the rendering, it is packaging and dependencies. We deliberately stayed out of the package-manager rabbit hole and ship one native binary you launch directly, the same model as Windows. The other classic Linux problem is glibc; if that sentence means nothing to you, consider yourself fortunate. We solved it by compiling against a very old glibc (around `GLIBC_2.17`, from 2013) and linking GTK3 dynamically, both of which are present on essentially every desktop. We also support **musl for Alpine**, and both **x64 and arm64**. Like the other ports, this is not a local-only path: the Linux target builds on the build cloud, and new projects from the Initializr come wired for it. The architecture and the build are covered in {{< post-link path="/blog/java-to-a-native-linux-app" text="Saturday's post" >}}.

## Codename One on your wrist: Apple Watch and Wear OS

[PR #5252](https://github.com/codenameone/CodenameOne/pull/5252) adds a proper native Apple Watch (watchOS) port, alongside Wear OS support on the Android side. Apple treats watch programming as a completely separate discipline from phone or desktop programming: a different API, a different lifecycle, and UI metaphors we take for granted that simply do not exist on the watch (no text field as you know it, and no browser). That is a defensible design, and it raises a fair question: what is the point of a watch API at all?

The answer is that reuse still happens. Many well known apps skip the watch entirely because it is such a chore, yet the amount of work a watch UI actually needs is small, and smaller still with Codename One. watchOS has no UIKit views, no OpenGL ES and no Metal, so the port ships a dedicated **Core Graphics rendering backend** and hosts the Codename One runtime inside a SwiftUI shell. The same Java code, branched with `CN.isWatch()`, renders real Codename One UI on the watch:

![A Codename One UI rendered on the Apple Watch simulator through the Core Graphics backend](/blog/native-apple-watch-and-wear/watch-bezel.png)

That is a screenshot from our test framework, which was never designed for a watch: it still has a text field. Because that is a Codename One text field it renders correctly and "just works" right up until you try to edit in it, which on a watch would not give the result you want; a real watch UI would simply leave it out.

Wear OS is simpler: a Wear OS app is an ordinary Android app, so the existing Android port renders it with the same pipeline it uses on phones. You enable each side with one build hint, and with the hints off your phone build is byte-for-byte unchanged. Both wearables are covered in detail in {{< post-link path="/blog/native-apple-watch-and-wear" text="Sunday's post" >}}.

## A visual Game Builder

[PR #5253](https://github.com/codenameone/CodenameOne/pull/5253) adds a visual game level editor on top of the `com.codename1.gaming` API from last week, plus a high-level data model and a streaming engine for large worlds. Instead of hand-placing every sprite in code, you draw the level, tag objects with the numbers your game needs (`lives`, `value`, `speed`), and the editor saves a small data file the runtime plays. Your code shrinks to the part that is actually yours: the rules.

This is the special case in this week's release. Tuesday's post is a full overview of the editor, the data model, and the streaming engine for large worlds, and then **a three-part tutorial series starts Thursday** rather than a single follow-up. The first tutorial builds a playable 2D platformer, "Duke's Coffee Run", from an empty scene to a running game, including the part most tutorials skip: bringing in real art and slicing an animated sprite sheet for Duke.

![Duke's Coffee Run, a 2D platformer built with the Game Builder](/blog/gamebuilder/game-platformer.gif)

Two more parts follow on the next Thursdays: a blackjack card game, then a first-person 3D dungeon that scales up to large streaming worlds. The full picture of what the builder is and how it fits together is in {{< post-link path="/blog/the-codename-one-game-builder" text="Tuesday's overview" >}}. One important caveat: **the Game Builder and its high-level APIs are beta**. We are actively improving them and we want your feedback on the editor, the API shape, and the asset workflow. Tell us what works and what gets in your way through the [issue tracker](https://github.com/codenameone/CodenameOne/issues).

## Seamless crash protection

[PR #5001](https://github.com/codenameone/CodenameOne/pull/5001) replaces the old email-based crash protection tool with a new on-device client, `com.codename1.crash`, that we think leapfrogs the dedicated tools in this space. As we add more platforms, testing across all of them becomes a bigger challenge; this is the tool for dealing with that, and it is built to be seamless.

It is seamless in three senses. You do not wire up anything complex: the build servers do the heavy lifting. It connects to GitHub issues instead of sending a barrage of emails. And it symbolicates native crashes on every operating system we support, so a faulting address on iOS, Android, Windows or Linux comes back as a readable stack. On the device, reports are written to storage before they are sent and only deleted after the server confirms receipt, so nothing is lost to a flaky connection; a failed send is retried on the next launch and the server deduplicates it.

{{< mermaid >}}
flowchart LR
    A["App crashes on device"] --> B["Write report to Storage with eventId"]
    B --> C["POST to the crash endpoint"]
    C -->|2xx confirmed| D["Delete the stored report"]
    C -->|offline or failed| E["Retry on next launch, server dedups by eventId"]
    F["Release build on the cloud"] --> G["Upload mapping.txt / dSYM symbols"]
    C --> H["Server symbolicates the native stack"]
    G --> H
    H --> I["A GitHub issue, not an email"]
{{< /mermaid >}}

Personal data is scrubbed on the device before anything leaves it: emails are partially redacted and long digit runs are collapsed, with rules you can override. It is opt-in and off by default; turning it on is two lines, and the full walkthrough is in {{< post-link path="/blog/seamless-crash-protection" text="Monday's post" >}}.

```java
CrashProtection.install();
CrashProtection.setEnabled(true);
```

## Behind the scenes: a rebuilt build cloud

Codename One was founded in 2012, back when Docker was a brand-new alpha product that few people had heard of, and a lot of the infrastructure underneath the build cloud dates back to that era. This week we did a major rebuild and re-architecture of it. The transition caused a few failed builds, and if one of yours was among them, we are sorry for the disruption.

The end result is completely new infrastructure that is far easier to update going forward, with better isolation and security. We are nearly finished with the Mac side (that work continues over this weekend), and every other platform is done except for UWP and the Windows desktop builds. If a build behaves differently from how it did before, please tell us right away, ideally through the chat on our website, so we can act quickly.

## Initializr now runs on the JavaScript port

We quietly switched the [Codename One Initializr](https://start.codenameone.com) over to the new JavaScript port, and we hope you did not notice the difference. That is the goal: the JavaScript port should feel like every other port.

It is also one of the hardest ports we have ever worked on. Every percentage point of compatibility and every bit of performance is a genuine fight. The work is ongoing, and the aim is to get the JavaScript port aligned with the rest and launch it in the near future.

## From the community

Francesco Galgani published another thoughtful piece, [Java was supposed to free us from the operating system; today Codename One is getting there](https://www.informatica-libera.net/content/java-was-supposed-to-free-us-from-the-operating-system-today-codename-one-is-getting-there). It is worth your time, and we are grateful for the writing and the perspective.

A note on cadence: community activity is slowing down as everyone heads into the summer holidays, and we are not sure how quickly progress will continue through these months. We are still very much here, and we are looking for feedback on all of the above, so this is a good time to send it.

## Upcoming attractions

The tutorials follow this post; each link below goes live on its day:

- **Saturday.** {{< post-link path="/blog/java-to-a-native-linux-app" text="The native Linux desktop port" >}}. PR [#5239](https://github.com/codenameone/CodenameOne/pull/5239).
- **Sunday.** {{< post-link path="/blog/native-apple-watch-and-wear" text="The Apple Watch and Wear OS ports" >}}. PR [#5252](https://github.com/codenameone/CodenameOne/pull/5252).
- **Monday.** {{< post-link path="/blog/seamless-crash-protection" text="Seamless crash protection" >}}. PR [#5001](https://github.com/codenameone/CodenameOne/pull/5001).
- **Tuesday.** {{< post-link path="/blog/the-codename-one-game-builder" text="The Game Builder overview" >}}. PR [#5253](https://github.com/codenameone/CodenameOne/pull/5253).
- **Thursday.** {{< post-link path="/blog/game-builder-2d-platformer" text="Game Builder tutorial 1, a 2D platformer" >}}. The first of three parts. PR [#5253](https://github.com/codenameone/CodenameOne/pull/5253).

## Wrapping up

The issue tracker is [here](https://github.com/codenameone/CodenameOne/issues) and it is the best place to reach us right now. The discussion forum is [here](https://www.codenameone.com/discussion-forum.html), and the Build Cloud console is at [`/console/`](https://cloud.codenameone.com/console/index.html). The [Playground](/playground/), [Initializr](/initializr/), and [Skin Designer](/skindesigner/) are where they have always been.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
