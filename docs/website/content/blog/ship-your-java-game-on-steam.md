---
title: "Ship Your Game on Steam, and Why the Art Matters More Than the Engine"
slug: ship-your-java-game-on-steam
url: /blog/ship-your-java-game-on-steam/
date: '2026-11-17'
author: Shai Almog
description: "Splat is a full-stack, first-person team paintball shooter template in Java, refereed by its own server, with a mission builder, replays and party voice chat. Building it added fog, PBR materials, shadows and post processing to every GPU backend, and taught us that the art makes the game. Codename One games can now ship to Steam, the Steam Deck and the Steam Machine from the build cloud."
feed_html: "<img src=\"https://www.codenameone.com/blog/ship-your-java-game-on-steam.jpg\" alt=\"Splat, a team paintball shooter, headed to Steam\" /> Splat is a first-person team shooter template in Java with its own referee server. It taught us the art makes the game. And Codename One games now ship to Steam from the build cloud."
series: ["release-2026-11-13"]
---

![Splat, a team paintball shooter, headed to Steam](/blog/ship-your-java-game-on-steam.jpg)

Here's my claim, and the rest of this post is the evidence: the engine is the least important decision in a small game. Art decides whether people play it, the server decides whether it's fair, and the store decides whether anyone finds it. Splat, our third game template after Skirmish and [Dash](/blog/build-an-endless-runner-in-java/), is a first person team paintball shooter, four against four, in 3D, online, written in Java with Codename One. It's the kind of game people assume needs a commercial engine. Watch it first and judge for yourself.

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/blog/release-2026-11-13/splat-desktop-demo-poster.jpg" width="960" height="564" style="width:100%;height:auto;display:block;margin:16px auto" aria-label="Splat on the desktop: menus, a briefing and a match"><source src="/blog/release-2026-11-13/splat-desktop-demo.mp4" type="video/mp4"><a href="/blog/release-2026-11-13/splat-desktop-demo.mp4">Download the desktop video</a>.</video>
{{< /guide-block >}}

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,200px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/splat-home.png" alt="The Splat home screen on a phone" loading="lazy" style="width:100%;height:auto" /><figcaption>Home on a phone.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/splat-match-overhead.png" alt="A Splat match seen from above" loading="lazy" style="width:100%;height:auto" /><figcaption>A match from above.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/splat-mission-editor.png" alt="The Splat mission editor" loading="lazy" style="width:100%;height:auto" /><figcaption>The mission editor.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/splat-mission-playtest.png" alt="Play testing a custom Splat field" loading="lazy" style="width:100%;height:auto" /><figcaption>Play testing a field.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/splat-party-chat.png" alt="A Splat party lobby with voice chat" loading="lazy" style="width:100%;height:auto" /><figcaption>Party lobby and voice.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/splat-instant-replay.png" alt="A Splat instant replay" loading="lazy" style="width:100%;height:auto" /><figcaption>Instant replay.</figcaption></figure>
</div>
{{< /guide-block >}}

It's paintball, and it never stops making fun of itself. Every match opens with a mission briefing read in the gravest voice available, and every hit stamps a big red **PAINTED** across your goggles.

## The lesson: the engine isn't the star

I'll start with what surprised me, because it's the most useful thing in this post.

The first playable Splat drew its fields and players from shapes generated in code. The networking worked, the shots were lag compensated, the referee caught cheaters. It looked like a tech demo, and nobody wanted to play it for more than a minute.

Then we replaced the shapes with real art: CC0 models from [Quaternius](https://quaternius.com) and [Kenney](https://kenney.nl), photo scanned props and ground textures from [Poly Haven](https://polyhaven.com), players built with MakeHuman and type set in Rajdhani. Nothing else changed, and it was suddenly a game.

Only after that did we add the engine features. Fog with height falloff, PBR materials, directional shadows and post processing all went into the framework because the art deserved them, not the other way around. If you're building a game, budget for the artist first. A better renderer makes good art look great. It does nothing for boxes.

CC0 means none of those artists asked for credit. We credit them anyway, in `scripts/splat/ART.md`, along with the steps to re-skin every model.

## What the framework gained

Building Splat added these to `com.codename1.gpu`, on every GPU backend: OpenGL in the simulator and on Linux, Android, WebGL in the browser, Metal on iOS and Direct3D 11 on Windows.

- **`Environment`**: exponential fog with height falloff, hemisphere ambient light, exposure and tone mapping, and a procedural sky through `Environment.setSky`.
- **`Material.Type.PBR`**: metallic and roughness, emissive and vertex color, filled from glTF.
- **Shadows**: directional shadow maps through `Light.setShadows`, softened with percentage closer filtering.
- **`PostProcess`** on a `GameView`: bloom, color grading, vignette and grain.
- **`GpuCapabilities`**: on a device without shadow or offscreen support the scene draws directly instead of failing.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,200px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img width="1600" height="940" src="/developer-guide/img/splat-desktop-match.png" alt="A Splat match on the desktop with fog and shadows" loading="lazy" style="width:100%;height:auto" /><figcaption>A match on the desktop.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/splat-briefing.png" alt="A Splat mission briefing" loading="lazy" style="width:100%;height:auto" /><figcaption>The briefing.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/splat-scoreboard.png" alt="The Splat scoreboard" loading="lazy" style="width:100%;height:auto" /><figcaption>The scoreboard.</figcaption></figure>
</div>
{{< /guide-block >}}

## The server is the referee

In a shooter, whoever decides a hit decides the game. If the phone decides, a modified client wins every fight. Splat's backend runs the match and the phone predicts:

{{< mermaid >}}
flowchart LR
    P[Phone<br/>predicts movement and shots] -->|inputs over WebSocket| S[Backend referee<br/>30 Hz fixed-point rules]
    S -->|authoritative state| P
    S --> R[Rewinds to the shooter's view<br/>to check the hit]
    S --> A[Anti-cheat flags, XP, rating]
    S --> RP[Replays and leaderboards]
{{< /mermaid >}}

The rules module is fixed point and deterministic, and the same code runs on the phone and on the server. A golden corpus of recorded matches replays in CI, so a change that alters the outcome of a past match fails the build. Shots are lag compensated: the server rewinds to what the shooter saw, within a cap, so a hit that looked right on a slow connection counts.

Around the match there's matchmaking by skill, parties with lobby codes and WebRTC voice chat, instant replays, a shop with store receipts checked against Apple and Google, and an admin console with moderation. The sound effects, music and ambience are all synthesized in code, so no audio files ship at all.

## Build your own field

The mission builder lets players design a field on the phone or the desktop, play test it against bots and publish it to friends. You build one half and the other half mirrors it, so every field is fair.

{{< guide-block >}}
<div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,220px),1fr));gap:16px;align-items:start;margin:24px 0">
<video controls playsinline preload="metadata" poster="/blog/release-2026-11-13/splat-mobile-mission-builder-poster.jpg" width="400" height="800" style="width:100%;height:auto;max-width:280px;display:block;margin:0 auto" aria-label="The Splat mission builder on a phone"><source src="/blog/release-2026-11-13/splat-mobile-mission-builder.mp4" type="video/mp4"><a href="/blog/release-2026-11-13/splat-mobile-mission-builder.mp4">Download the phone video</a>.</video>
<video controls playsinline preload="metadata" poster="/blog/release-2026-11-13/splat-mobile-demo-poster.jpg" width="400" height="800" style="width:100%;height:auto;max-width:280px;display:block;margin:0 auto" aria-label="Splat gameplay on a phone"><source src="/blog/release-2026-11-13/splat-mobile-demo.mp4" type="video/mp4"><a href="/blog/release-2026-11-13/splat-mobile-demo.mp4">Download the phone gameplay video</a>.</video>
</div>
{{< /guide-block >}}

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/blog/release-2026-11-13/splat-desktop-mission-builder-poster.jpg" width="960" height="564" style="width:100%;height:auto;display:block;margin:16px auto" aria-label="The Splat mission builder on the desktop"><source src="/blog/release-2026-11-13/splat-desktop-mission-builder.mp4" type="video/mp4"><a href="/blog/release-2026-11-13/splat-desktop-mission-builder.mp4">Download the desktop mission builder video</a>.</video>
{{< /guide-block >}}

## Steam: the console you're allowed to ship on

Last week I wrote that [shipping on consoles needs SDKs behind NDAs](/blog/codename-one-on-xbox/). Steam is the exception. Valve's Steam Machine and the Steam Deck run Linux, Steamworks is open to any developer who pays the app fee, and the same binary runs on Linux desktops. It's the one major open console.

Codename One publishes to Steam with the native Linux port: one native executable, no JVM, packaged as a Steam depot and uploaded with SteamPipe. Set the app id:

```properties
codename1.arg.steam.appId=480
codename1.arg.steam.branch=beta
```

Then build locally, or let the cloud do it:

```bash
mvn cn1:buildSteamLocal                   # depot in linux/target/steam
mvn cn1:steamUpload -Dsteam.user=builder  # upload with SteamCMD
mvn cn1:buildSteam                        # or build in the cloud
```

In the build cloud console you save your Steam builder account once, then press "Submit to Steam" or turn on submitting every build. Valve only lets an upload go live on a beta branch, so promotion to the default branch and Valve's review happen on the Steamworks site, as they do for everyone.

### Controllers and Steam features from portable code

Games on Steam are expected to work with a controller, and players on the Deck and in Big Picture mode often have nothing else. Every gamepad Steam knows about arrives through `Gamepads`, and forms can be navigated with the D-pad with no code at all:

```java {class="cn1-no-try"}
Gamepad pad = Gamepads.getGamepad(0);
if (pad != null && pad.isConnected()) {
    float x = pad.getAxis(Gamepad.AXIS_LEFT_X);   // -1..1, dead zone applied
    boolean jump = pad.isButtonDown(Gamepad.BUTTON_A);
}
```

Achievements, stats, cloud saves, rich presence, DLC ownership and the overlay go through `GameServices`. Off Steam the calls do nothing, so the same game builds for iOS and Android unchanged:

```java {class="cn1-no-try"}
if (GameServices.isAvailable()) {             // true on Steam
    GameServices.unlockAchievement("FIRST_WIN");
    GameServices.setStat("wins", GameServices.getStatInt("wins", 0) + 1);
    GameServices.commit();
    GameServices.writeSave("slot1", saveBytes);
}
```

The app starts full screen, text fields open Steam's on-screen keyboard in Gaming Mode, and `isControllerFirst()` tells you when to show controller prompts instead of mouse hints.

A few things the Steam runtime doesn't provide: there's no embedded browser, no GStreamer media playback and no secure storage. Games rarely need them, but check the [Steam chapter](/developer-guide/#steam) before you plan around one.

## Learn more

Generate Splat from the Initializr, read the implementor's guide in the developer guide, and swap the art for your own. Then point it at Steam. If you ship a game this way, send it to me. I'd love to play it.

{{< giscus >}}
