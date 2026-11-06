---
title: "Dash: An Endless Runner That Races Through Your City"
slug: build-an-endless-runner-in-java
url: /blog/build-an-endless-runner-in-java/
date: '2026-11-07'
author: Shai Almog
description: "Dash is a full-stack endless runner template in Java: levels that turn from 2D into 3D mid-run, runs built from real streets on a map, cheat-proof leaderboards, live races with voice chat, a level editor and an admin console. Generate it from the Initializr and ship it under your own name, royalty free."
feed_html: "<img src=\"https://www.codenameone.com/blog/build-an-endless-runner-in-java.jpg\" alt=\"Dash: a route picked on a city map and the same route as a 3D run\" /> Dash is a full-stack endless runner template in Java: 2D that turns into 3D mid-run, runs built from real streets, cheat-proof leaderboards, races with voice chat and a level editor."
series: ["release-2026-11-06"]
---

![Dash: a route picked on a city map and the same route as a 3D run](/blog/build-an-endless-runner-in-java.jpg)


[Skirmish](/blog/build-a-multiplayer-strategy-game-in-java/) showed that most of a mobile game is app: the sign-in, the menus, the shop, the friends list, the pushes and the server. Dash is the second game template, and it is a different kind of game: an endless runner. A runner dashes down a track of lanes, jumps hurdles, slides under barriers and changes lanes around traffic. You can generate it from the [Initializr](/initializr/) today, under your own name and package, and the source is MIT-0.

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/blog/release-2026-11-06/dash-promo-tour-poster.jpg" width="420" height="908" style="width:100%;height:auto;max-width:300px;display:block;margin:16px auto" aria-label="A tour of Dash: the levels screen, a run that turns from 2D into 3D, and a city run built from a map"><source src="/blog/release-2026-11-06/dash-promo-tour.mp4" type="video/mp4"><a href="/blog/release-2026-11-06/dash-promo-tour.mp4">Download the Dash tour</a>.</video>
{{< /guide-block >}}

## A level that turns into 3D

The trick I like most in Dash is that a level can start as a side-scroller and become a 3D runner halfway through. The first stretch of *Neon Causeway* looks like a flat platformer: one row of hurdles and barriers, seen from the side. At 300 meters the camera swings behind the runner, the lanes open up, and the rest is a chase-camera runner.

There is no second game behind that. The simulation always has lanes, heights and distances, and a hurdle is cleared the same way from either angle. A level only carries *camera cues*, and the renderer blends between the two framings when the runner reaches one. Because the rules never see the camera, a level can change its view anywhere without changing who wins.

## Run your own street

This is my favorite part to demo. Tap *Run your city*, pick a start and a finish on a real street map, and the route becomes a level. Every street is a stretch of track, every turn is a corner on the side it turns to, and the next street's name is called out before you reach it. Landmarks along the way, a station or a museum, show up as boards beside the track.

{{< guide-block >}}
<div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,150px),1fr));gap:16px;margin:24px 0">
<figure style="margin:0"><img width="1125" height="2436" src="/developer-guide/img/dash-city-route.png" alt="A route picked on the street map, with the landmarks along it pinned" loading="lazy" style="width:100%;height:auto" /><figcaption>Pick two points.</figcaption></figure>
<figure style="margin:0"><img width="1125" height="2436" src="/developer-guide/img/dash-city-run.png" alt="The run down Cannon Street, with the street name under the score and the route map" loading="lazy" style="width:100%;height:auto" /><figcaption>Run the streets.</figcaption></figure>
<figure style="margin:0"><img width="1125" height="2436" src="/developer-guide/img/dash-city-landmark.png" alt="A landmark's board beside the track as the runner passes it" loading="lazy" style="width:100%;height:auto" /><figcaption>Pass the landmarks.</figcaption></figure>
</div>
{{< /guide-block >}}

The map is Codename One's `MapView`, which draws OpenStreetMap data itself on every platform, and the route comes from `Routing`. The app reduces the route to streets and turns and hands it to the rules:

```java {class="cn1-no-try"}
CityRoute route = new CityRoute("Old Harbour")
        .leg(420, CityRoute.STRAIGHT, "Quay Street")
        .leg(310, CityRoute.LEFT, "Rope Walk");
LevelDef level = route.level();
```

`level()` is deterministic, so the server doesn't need a map. It rebuilds the same level from the encoded route and checks your run against it. The level's id is a hash of the route, so every route gets its own leaderboard, and nobody can trim a hard route into an easy one and keep the same board.

## Leaderboards you can trust

A run is never reported as a number. The app sends the seed and the input log, a few hundred bytes, and the server plays it through the same Java rules module the app runs. A typed-in score, or a log edited to dodge an obstacle the player hit, does not reproduce. The rules use no floating point and no clock, so the phone, the server and the replay viewer agree to the bit.

{{< mermaid >}}
sequenceDiagram
    participant P as Phone
    participant S as Server
    P->>P: Run on seed + rules module
    P->>S: Seed + input log (a few hundred bytes)
    S->>S: Replay the log on the same rules
    alt Replay matches
        S->>S: Post score to all-time, weekly, friends and crew boards
        S-->>P: Push to the friend you just beat
    else Edited log or typed-in score
        S-->>P: Rejected, it doesn't reproduce
    end
{{< /mermaid >}}

Every level has all-time, weekly, daily, friends and crew boards. Beat a friend's best and they get a push that opens that level's board.

## Racing your friends

Open a party, share the link, and everyone runs the same track at the same moment. The server runs the race in lockstep: each phone sends its inputs, the room broadcasts each tick's inputs to every seat, and its own copy of the race decides the places. A client whose state drifts from the server's is caught by a hash it sends every second.

While you race you can talk. Voice uses Codename One's WebRTC API, peer to peer between each pair of players, with the game's own socket carrying the signalling. The server never touches the audio.

{{< guide-block >}}
<video controls playsinline muted loop preload="metadata" poster="/blog/release-2026-11-06/dash-gameplay-clip-poster.jpg" width="420" height="840" style="width:100%;height:auto;max-width:300px;display:block;margin:16px auto" aria-label="Dash gameplay: lane changes, jumps and slides on a neon track"><source src="/blog/release-2026-11-06/dash-gameplay-clip.mp4" type="video/mp4"><a href="/blog/release-2026-11-06/dash-gameplay-clip.mp4">Download the gameplay clip</a>.</video>
{{< /guide-block >}}

## Build your own levels

The track editor is in the app, on every platform. A level is a strip you paint on: tap a lane to drop a hurdle or a coin, tap the top row for a barrier across every lane, and drop camera cues where the view should change. *Playtest* runs the draft on the real rules. Publishing requires the bot to finish the level, so nobody can publish one that cannot be beaten. Published packs can be rated, raced, remixed and moderated from the admin console.

## Get it

Choose **Dash** in the [Initializr](/initializr/), or read the [guide chapter](/developer-guide/dash-implementors-guide/) first. It walks through the rules, the camera, the referee, races, voice and the editor, and what to change to make the game yours.

If you've ever wanted to ship a game but stalled on everything around the game, this is the part I'd copy. Sign-in, friends, pushes, a shop, moderation and a server that replays every run are already done, so the week you spend is on the fun part.

Tomorrow: {{< post-link path="/blog/launch-your-own-gym-app" text="Pulse: Heart Rate Zones From the Watch to the Trainer's Screen" >}}.

{{< giscus >}}
