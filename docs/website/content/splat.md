---
title: "Build a Team Paintball Shooter in Java with Splat"
description: "Splat is a first-person team paintball shooter in Java: 3D matches four against four, a server that referees every match while the phone predicts, lag-compensated shots, parties with WebRTC voice, replays and an admin console. Free MIT-0 source."
url: "/splat/"
summary: "A full-stack shooter: server-refereed 3D matches, client prediction, parties with voice, and a big red PAINTED stamp."
images: ["/developer-guide/img/splat-match-practice.png"]
ShowToc: true
---

Splat is a first-person team paintball shooter that takes itself exactly as seriously as a dad in a borrowed tactical vest. Every match opens with a grim mission briefing, every hit lands a big red **PAINTED** stamp, and a player who is out can try to wipe the paint off before the referee notices. Four against four, in a self-storage yard or a suburban cul-de-sac, in Team Paint, Capture the Flag or Hold the Snack Bar.

Under the jokes it's a real-time shooter of the kind that usually needs an engine, a networking product and a separate server. The server referees every match while each phone predicts its own player, so the marker fires with no wait. A shot from a slow connection still lands where it was aimed, and bots, practice matches and replays all run the same rules. In Splat all of it is ordinary Codename One code in one project: the 3D field, its server, the voice chat and the admin console.

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/splat/splat-promo-poster.jpg" style="width:100%;max-width:720px;height:auto;display:block;margin:0 auto" aria-label="A tour of Splat: the marker loadout, a mission briefing, a capture-the-flag match against bots, a player painted and shown again in a slow-motion replay from three angles, the bird's-eye view and the winners celebrating">
<source src="/splat/splat-promo.mp4" type="video/mp4">
<a href="/splat/splat-promo.mp4">Download the Splat tour video</a>.
</video>
{{< /guide-block >}}

**[Generate your project](/initializr/)** · **[Read the guide](/developer-guide/splat-implementors-guide/)** · **[Browse the source](https://github.com/codenameone/CodenameOne/tree/master/scripts/splat)**

In the Initializr, choose the **Splat** paintball template, set your application name and package, and generate the project. The application source is licensed under [MIT-0](https://github.com/codenameone/CodenameOne/blob/master/scripts/splat/LICENSE): you can modify it, use it commercially and keep your changes private without royalties. The players, markers and props are CC0 models from Quaternius, Kenney and Poly Haven, and the type is Rajdhani under the SIL Open Font License, so there is nothing to buy or license.

## What comes with the application?

| Part | Included workflows |
| --- | --- |
| The match | A first-person 3D field drawn with `com.codename1.gaming`, animated operators carrying their markers, a slow-motion instant replay from three angles, paint that stays where it lands, a virtual stick, look by drag, fire, jump, grenade and wipe buttons, and keyboard and mouse on desktop |
| Shared rules | One deterministic Java module, with no floating point, that the app predicts with, the server runs as the authority, and the replays and tests re-run |
| Netcode | Server-authoritative snapshots over WebSocket, client prediction and reconciliation, and lag compensation capped so nobody paints around corners |
| Modes | Team Paint, Capture the Flag and Hold the Snack Bar on two fields, each with a mission briefing, against bots or other players |
| Parties | Quick matches with bots filling the empty slots, parties with share codes, peer-to-peer voice chat through WebRTC, text chat, emotes and replays that follow any player |
| Social | Crews, invitations, friends and pushes when a friend gets painted |
| Monetization | Gem packs, marker side-grades and paint jobs through the platform stores with server-side receipt checks, rewarded and interstitial ads |
| Engagement | Daily quests, chests, levels, trophies and a season, with a seeded field of rivals |
| Live operations | Reports, a player query, hourly engagement pipelines, broadcasts and the home-screen spotlight |
| Administration | Live matches, automatic flags for flooded inputs and impossible turns, player lookup and audited actions, as a phone app or a desktop console |

## What does it look like?

![The main menu](/developer-guide/img/splat-home.png)
![The operations playlist](/developer-guide/img/splat-operations.png)
![A briefing](/developer-guide/img/splat-briefing.png)
![A match in progress](/developer-guide/img/splat-match-practice.png)
![The scoreboard](/developer-guide/img/splat-scoreboard.png)
![A party lobby with chat](/developer-guide/img/splat-party-chat.png)
![A live party match](/developer-guide/img/splat-party-match.png)
![The admin console](/developer-guide/img/splat-admin-overview.png)

## From first run to customization

1. **[Run the project](https://github.com/codenameone/CodenameOne/tree/master/scripts/splat#running-it).** The development profile needs nothing installed: an in-memory database, demo accounts and a simulated store, so you can play against the bots, or against a second simulator, straight away.
2. **[Tour the game](/developer-guide/splat-implementors-guide/).** The guide opens with what the game does, from the first briefing to party matches.
3. **[Make it your own](/developer-guide/splat-implementors-guide/).** The same guide then covers fields, markers, the jokes, re-skinning, store products, ads, push and moderation.

## What does a live game still need?

Splat supplies the game and its source. Publishing one also requires your own hosting, store products, ad units, a push key, and a TURN server for voice between players behind strict networks.

## Looking for another kind of game?

[Dash](/dash/) is the endless runner and [Skirmish](/skirmish/) the real-time card battler, both on the same architecture.
