---
title: "Reel: Your Own Streaming Service or Course Platform"
slug: reel-streaming-and-courses-template
url: /blog/reel-streaming-and-courses-template/
date: '2026-11-03'
author: Shai Almog
description: "Reel is a streaming service and its server in one Java project. Flip a preset and it's a subscription service, a creator platform with community uploads, or an academy with courses and quizzes. Codename One also gained a DRM streaming API for HLS, ClearKey, Widevine, and FairPlay."
feed_html: "<img src=\"https://www.codenameone.com/blog/reel-streaming-and-courses-template.jpg\" alt=\"The Reel streaming app catalog and player\" /> Reel is a streaming service and its server in one Java project: a subscription service, a creator platform or a course academy, with a new DRM streaming API."
series: ["release-2026-10-30"]
---

![The Reel streaming app catalog and player](/blog/reel-streaming-and-courses-template.jpg)

Video is how a lot of businesses now talk to their customers. A gym sells classes, a university publishes lectures, a creator community wants its own home instead of renting space on someone else's platform. They all need the same pieces: a catalog, a player that remembers where you stopped, uploads, moderation, payments and some protection for paid content.

**Reel** is those pieces, the app and its server in one Java project. Like last week's [Skirmish, Parley and Savor](/blog/rewrite-everything-in-java-not-rust/), it's a template you generate and brand, not a demo you admire.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,150px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><a href="/developer-guide/img/reel-viewer-home.png"><img width="1080" height="1920" src="/developer-guide/img/reel-viewer-home.png" alt="The Reel catalog" loading="lazy" style="width:100%;height:auto" /></a><figcaption>The catalog.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/reel-viewer-player-tracks.png"><img width="1080" height="1920" src="/developer-guide/img/reel-viewer-player-tracks.png" alt="The Reel player with audio and subtitle tracks" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Audio and subtitle tracks.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/reel-viewer-course.png"><img width="1080" height="1920" src="/developer-guide/img/reel-viewer-course.png" alt="A Reel course with its lessons" loading="lazy" style="width:100%;height:auto" /></a><figcaption>A course and its lessons.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/reel-moderation-queue.png"><img width="1080" height="1920" src="/developer-guide/img/reel-moderation-queue.png" alt="The Reel moderation queue" loading="lazy" style="width:100%;height:auto" /></a><figcaption>The moderation queue.</figcaption></figure>
</div>
{{< /guide-block >}}

## One template, three businesses

Reel's behavior is driven by presets you switch at run time, with no rebuild:

- **Subscription service.** Plans or rentals, profiles, downloads, resume across devices.
- **Creator platform.** Anyone with the creator role uploads video, the server packages it, and moderators approve it before it's public. Think of a community YouTube for your niche.
- **Academy.** Courses, lessons and quizzes, for training and tutorials.

On top of the preset, toggles turn ads, rentals, downloads, uploads, courses, quizzes and polls on or off. The server assigns roles (viewer, creator, moderator and admin) and the app shows each person the screens their role allows.

## Uploads and moderation

Creators upload from the app and the server packages each upload for streaming and checks it against blocklists the moderators maintain. A title only goes live after a moderator approves it, and a moderator can take it down later. Takedowns take effect mid-stream: the player's heartbeat checks with the server, so a removed title stops playing instead of running to the end.

{{< mermaid >}}
flowchart LR
    C[Creator uploads] --> P[Server packages HLS]
    P --> B[Blocklist checks]
    B --> Q[Moderation queue]
    Q -->|approve| L[Live in the catalog]
    Q -->|reject| R[Back to the creator]
    L -->|takedown| X[Stops on the next heartbeat]
{{< /mermaid >}}

## A streaming API with DRM, in core

Reel needed playback for protected video, so Codename One now has a streaming API that takes a DRM configuration:

```java {class="cn1-no-try"}
StreamRequest req = new StreamRequest(manifestUrl)
        .setStartPosition(resumeSeconds * 1000)
        .setDrm(new DrmConfig(DrmScheme.CLEARKEY, licenseUrl)
                .addLicenseHeader("X-Reel-License", licenseToken));
req.createAsync().ready(media -> {
    player.add(BorderLayout.CENTER, media.getVideoComponent());
    player.revalidate();
});
```

`DrmScheme` covers AES-128 HLS, ClearKey, Widevine, and FairPlay. `MediaManager.isDrmSupported()` tells you which ones the current device can play, so you offer the strongest scheme it has and tell the user before playback if it has none.

Out of the box, Reel packages video as AES-128 HLS plus ClearKey. Keys are issued only against a license token tied to the session, and media tokens expire and are scoped to one video and a maximum quality. Concurrent stream limits are enforced by the same heartbeat. Protected video also blocks screenshots and can add a watermark with the account name.

I'll be direct about the limit: AES-128 and ClearKey stop casual copying, not a determined attacker, because the key reaches the device. For studio grade protection you plug in a Widevine or FairPlay license vendor through `reel.drm.widevine.url` and `reel.drm.fairplay.url`. The server forwards the license challenge, and the app code doesn't change.

## Payments and the rest

Payments are simulated in the dev profile; the test card 4242 4242 4242 4242 goes through. Set `reel.payments.provider=stripe` for Stripe, and store purchases are verified with Apple and Google. Push goes through the Codename One push service. Reel also runs in a browser with nothing installed, which is how many viewers will first meet your service.

{{< guide-block >}}
<figure style="margin:24px 0"><a href="/developer-guide/img/reel-viewer-home-desktop.png"><img width="2880" height="1692" src="/developer-guide/img/reel-viewer-home-desktop.png" alt="The Reel catalog on a desktop screen" loading="lazy" style="width:100%;height:auto" /></a><figcaption>The same app on a desktop.</figcaption></figure>
{{< /guide-block >}}

## Try it

Generate Reel from the [Initializr](/initializr/). The [user's guide](/developer-guide/reel-users-guide/) walks through every screen, and the [implementor's guide](/developer-guide/reel-implementors-guide/) covers the packaging pipeline, DRM vendors and what to change before you launch. The template source is free under MIT-0, with no royalties and no revenue share.

Tomorrow: {{< post-link path="/blog/android-apps-everywhere-even-without-google" text="Your Android App on iPhone, and on Phones Without Google" >}}.

{{< giscus >}}
