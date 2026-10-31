---
title: "Build an Endless Runner in Java with Dash"
description: "Dash is a 3D endless runner with a Java backend: levels that swing from 2D to 3D, leaderboards the server re-simulates, live races with voice chat, a track editor, crews, in-app purchases and an admin console. Free MIT-0 source."
url: "/dash/"
summary: "A complete full-stack runner with races, a level editor and honest leaderboards."
images: ["/developer-guide/img/dash-run-chase.png"]
ShowToc: true
---

Dash is a complete, full-stack endless runner in Java. A runner dashes down a track of lanes, jumping hurdles, sliding under barriers and dodging traffic, and a level can start side on like a platformer and swing behind the runner into 3D halfway down the track. Around the run is everything a live game needs: accounts, levels with stars, leaderboards, live races between friends with voice chat, ghosts and replays, a track editor, crews, a shop and a console for running the game. Use it to build your own game with Codename One, or to see how a Codename One app and server share one codebase.

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/dash/dash-promo-poster.jpg" style="width:100%;max-width:420px;height:auto;display:block;margin:0 auto" aria-label="A tour of Dash: a run that swings from side on into 3D, a city route picked on a map and run past its landmarks, a live race with chat, the level editor and the boards">
<source src="/dash/dash-promo.mp4" type="video/mp4">
<a href="/dash/dash-promo.mp4">Download the Dash tour video</a>.
</video>
{{< /guide-block >}}

**[Generate your project](/initializr/)** · **[Read the guide](/developer-guide/dash-implementors-guide/)** · **[Browse the source](https://github.com/codenameone/CodenameOne/tree/master/scripts/dash)**

In the Initializr, choose the **Dash** endless-runner template, set your application name and package, and generate the project. The application source is licensed under [MIT-0](https://github.com/codenameone/CodenameOne/blob/master/scripts/dash/LICENSE): you can modify it, use it commercially and keep your changes private without royalties. The runners, the track and the city are built in code: there are no third-party models or textures to license.

## What comes with the application?

| Part | Included workflows |
| --- | --- |
| The run | A 3D track drawn with `com.codename1.gaming` and runners built and animated in code, swipe, tap and keyboard controls, power-ups, particles, and camera cues that blend between a side view, a chase view and a top-down view |
| Shared rules | One deterministic Java module, with no floating point, that the app, the server, the replays and the tests all run |
| Leaderboards | All-time, weekly, daily, friends and crew boards for every level, holding only runs the server played again from their input logs |
| Races | Quick races and parties with share links, in lockstep over WebSocket, peer-to-peer voice chat through WebRTC, text chat, emotes, spectating and replays |
| City runs | Pick two points on a street map and run the route between them: every street a stretch of track with its name overhead, every turn a corner, and the landmarks along the way beside the track |
| Track editor | Levels painted on a timeline: obstacles, pickups, camera cues and the coach's lines, custom pieces, instant playtests, and publishing that requires the bot to finish the level |
| Social | Crews with their own boards and one-tap parties, invitations, and pushes when a friend beats your score |
| Monetization | Gem packs and outfits through the platform stores with server-side receipt checks, rewarded and interstitial ads |
| Engagement | Daily quests, a daily dash, weekly boards, chests, levels, trophies and achievements, and a seeded field of rivals on every board |
| Live operations | Reports, a player query, hourly engagement pipelines (pushes and gifts to a segment), broadcasts and the home-screen spotlight |
| Administration | Live races, the level moderation queue, automatic and reported flags, player lookup, and audited actions, as a phone app or a desktop console |

## What does it look like?

![The home screen](/developer-guide/img/dash-home.png)
![A run in the chase view](/developer-guide/img/dash-run-chase.png)
![A city run down a real street](/developer-guide/img/dash-city-run.png)
![A landmark beside the track](/developer-guide/img/dash-city-landmark.png)
![A party lobby with chat](/developer-guide/img/dash-party-chat.png)
![A live race](/developer-guide/img/dash-party-race.png)
![The level editor](/developer-guide/img/dash-forge-level-editor.png)
![A leaderboard](/developer-guide/img/dash-scores.png)
![The admin console on a desktop](/developer-guide/img/dash-desktop-admin-pipelines.png)

## From first run to customization

1. **[Run the project](https://github.com/codenameone/CodenameOne/tree/master/scripts/dash#running-it).** The development profile needs nothing installed: an in-memory database, demo accounts and a simulated store, so you can run, race the bots, or race a second simulator straight away.
2. **[Tour the game](/developer-guide/dash-implementors-guide/).** The guide opens with what the game does, from the first level to races and the editor.
3. **[Make it your own](/developer-guide/dash-implementors-guide/).** The same guide then covers levels and their camera, new obstacles and runners, re-skinning, store products, ads, push and moderation.

## What does a live game still need?

Dash supplies the game and its source. Publishing one also requires your own hosting, store products, ad units, a push key, and a TURN server for voice between players behind strict networks.

## Looking for another kind of game?

[Skirmish](/skirmish/) is the real-time card battler on the same architecture.
