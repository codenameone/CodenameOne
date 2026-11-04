---
title: "Build an Endless Runner in Java with Dash"
description: "Dash is a 3D endless runner in Java: live races with WebRTC voice chat, runs through your own city on a real street map, leaderboards the server re-simulates, a track editor and an admin console. Free MIT-0 source."
url: "/dash/"
summary: "A full-stack runner: voice chat while racing friends, runs through your own city, and honest leaderboards."
images: ["/developer-guide/img/dash-run-chase.png"]
ShowToc: true
---

Dash is an endless runner that does what a game engine leaves to you. Race your friends and talk to them while you run: the voice goes peer to peer over WebRTC, from the same Java code on iPhone, Android, desktop and the web. Pick two points on a real street map and run that route through your own city, every street a stretch of track with its name overhead and the landmarks beside it. And every score on a board was played again on the server from the run's inputs, by the same rules module the game runs, so a cheated run never shows up.

Those are the parts other game frameworks leave to plugins, a second language and a separate backend team. In Dash they're ordinary Codename One code in one project: the game, its server, its maps, its voice chat and its admin console. Around them is everything a live game needs: accounts, levels with stars, ghosts and replays, a track editor, crews, a shop and pushes. Use it to build your own game, or to see how a Codename One app and server share one codebase.

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
