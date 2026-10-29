---
title: "Reel: Your Own Streaming Service or Course Platform"
slug: reel-streaming-and-courses-template
url: /blog/reel-streaming-and-courses-template/
date: '2026-11-03'
author: Shai Almog
description: "Reel is a streaming service and its server in one Java project. Flip a preset and it's a subscription service, a creator platform with community uploads, or an academy with courses and quizzes. Codename One also gained a streaming API with DRM: AES-128 HLS, ClearKey, Widevine and FairPlay."
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

Reel needed playback for protected video, and that belongs in the framework, not in one template. So Codename One now has a streaming API in `com.codename1.media`. It's small on purpose:

| Class | What it does |
| --- | --- |
| `StreamRequest` | An HLS manifest or a progressive file URL, plus a start position, request headers, a completion callback and an optional DRM configuration. `create()` or `createAsync()` returns an ordinary `Media`. |
| `DrmConfig` | The scheme and license server URL, headers sent with every license request, a FairPlay certificate URL and a content id. |
| `DrmScheme` | `AES_128` (encrypted HLS), `CLEARKEY`, `WIDEVINE` and `FAIRPLAY`. |
| `MediaManager.isDrmSupported(scheme)` | Whether this device can play a scheme, asked before you open the stream. |

Opening a protected stream that resumes where the viewer stopped:

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

Each port maps the scheme to what the device has: AVPlayer decrypts AES-128 HLS and plays FairPlay on Apple, and Android's player and `MediaDrm` handle AES-128, ClearKey and Widevine. Devices differ, so Reel asks first and sends the server the schemes this device can play, strongest first. The server picks one and returns the matching manifest and license URL:

```java {class="cn1-no-try"}
List<String> out = new ArrayList<String>();
DrmScheme[] order = {DrmScheme.WIDEVINE, DrmScheme.FAIRPLAY, DrmScheme.CLEARKEY, DrmScheme.AES_128};
for (int iter = 0; iter < order.length; iter++) {
    if (MediaManager.isDrmSupported(order[iter])) {
        out.add(order[iter].name());
    }
}
```

{{< mermaid >}}
sequenceDiagram
    participant App
    participant Server
    participant License as License server
    App->>App: isDrmSupported for each scheme
    App->>Server: play title, supported schemes
    Server-->>App: manifest URL, scheme, license URL, media token
    App->>License: license request + X-Reel-License
    License-->>App: key (ClearKey) or license (Widevine, FairPlay)
    loop every few seconds
        App->>Server: heartbeat, position
        Server-->>App: keep playing, or stop (takedown, stream limit)
    end
{{< /mermaid >}}

Out of the box, Reel packages each video once, as AES-128 HLS and as CENC encrypted MP4 for ClearKey, under one key. Keys are issued only against a license token tied to the session, and every playlist, segment and key request carries a media token that expires and is scoped to one video and a maximum quality. Concurrent stream limits are enforced by the heartbeat. Protected video also blocks screenshots and screen recording, and can add a watermark with the account name.

I'll be direct about the limit: AES-128 and ClearKey stop casual copying, not a determined attacker, because the key reaches the device. For studio grade protection you plug in a Widevine or FairPlay license vendor through `reel.drm.widevine.url` and `reel.drm.fairplay.url`. The server forwards the license challenge, and the app code doesn't change.

## Audio tracks and subtitles

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,150px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><a href="/developer-guide/img/reel-viewer-lesson-subtitles.png"><img width="1080" height="1920" src="/developer-guide/img/reel-viewer-lesson-subtitles.png" alt="A Reel lesson playing with Spanish subtitles" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Subtitles over a lesson.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/reel-viewer-settings-subtitles.png"><img width="1080" height="1920" src="/developer-guide/img/reel-viewer-settings-subtitles.png" alt="Reel subtitle language and size settings" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Language and size settings.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/reel-studio-video-subtitles.png"><img width="1080" height="1920" src="/developer-guide/img/reel-studio-video-subtitles.png" alt="Adding a WebVTT subtitle track in the Reel studio" loading="lazy" style="width:100%;height:auto" /></a><figcaption>A creator adds a track per language.</figcaption></figure>
</div>
{{< /guide-block >}}

Creators add a subtitle track per language in the studio, as WebVTT. The server checks that it really is WebVTT and stores it beside the video, not inside the stream.

That last part is a deliberate choice. A platform player can render text tracks, but each one finds them differently, styles them differently, and some can't see a track inside an encrypted stream at all. So Reel draws subtitles itself, over the picture, and they look the same on every device and under every DRM scheme. The parser is about a hundred lines of plain Java in the template: it reads each cue's start and end time and its text, and drops WebVTT's positioning and inline tags such as `<i>` and `<v Speaker>`:

```java {class="cn1-no-try"}
Captions.load(track.url(), loaded -> {
    if (wanted.equals(subtitleLanguage)) {
        captions = loaded;
        updateCaption();
    }
});

private void updateCaption() {
    String text = captions == null ? "" : captions.at(media.getTime());
    if (!text.equals(caption.getText())) {
        caption.setText(text);
    }
}
```

`captions.at(millis)` returns the cues that cover that moment, one per line, and the player calls it as the position moves. Switching subtitles in the player's **Audio & subtitles** sheet takes effect at once, because nothing about the stream changes. Switching the audio language or the quality reopens the stream at the same position, and the server marks the viewer's preferred audio track as the playlist default, so it starts in the right language.

## Payments and the rest

Payments are simulated in the dev profile; the test card 4242 4242 4242 4242 goes through. Set `reel.payments.provider=stripe` for Stripe, and store purchases are verified with Apple and Google. Push goes through the Codename One push service. Reel also runs in a browser with nothing installed, which is how many viewers will first meet your service.

{{< guide-block >}}
<figure style="margin:24px 0"><a href="/developer-guide/img/reel-viewer-home-desktop.png"><img width="2880" height="1692" src="/developer-guide/img/reel-viewer-home-desktop.png" alt="The Reel catalog on a desktop screen" loading="lazy" style="width:100%;height:auto" /></a><figcaption>The same app on a desktop.</figcaption></figure>
{{< /guide-block >}}

## Try it

Generate Reel from the [Initializr](/initializr/). The [user's guide](/developer-guide/reel-users-guide/) walks through every screen, and the [implementor's guide](/developer-guide/reel-implementors-guide/) covers the packaging pipeline, DRM vendors and what to change before you launch. The template source is free under MIT-0, with no royalties and no revenue share.

Tomorrow: {{< post-link path="/blog/android-apps-everywhere-even-without-google" text="GrapheneOS, LineageOS, /e/OS and Others Are Now Officially Supported" >}}.

{{< giscus >}}
