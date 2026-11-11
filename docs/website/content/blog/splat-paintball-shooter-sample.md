---
title: "Splat: A First-Person Team Shooter in Java"
slug: splat-paintball-shooter-sample
url: /blog/splat-paintball-shooter-sample/
date: '2026-11-05'
author: Shai Almog
draft: true
---

![Splat](/developer-guide/img/splat-home.png)

Splat is our new full-stack sample: a first-person team shooter that plays paintball and takes itself exactly as seriously as a dad in a borrowed tactical vest. Four against four in a self-storage yard or a suburban cul-de-sac, in Team Paint, Capture the Flag or Hold the Snack Bar. Every match opens on a grim, letterboxed briefing, and every hit lands a big red **PAINTED** stamp.

It's one Java codebase for iOS, Android, desktop and the web, plus its own server.

## What's in it

- **A real 3D shooter.** Skinned, animated operators from glTF, a first-person marker that bobs, sways and kicks, aim down the sights, paint that sticks to cover, and a HUD with a minimap, killfeed, hit markers and damage direction.
- **An authoritative server.** The client predicts its own player and the server, which runs the same deterministic rules, decides. Hits are lag compensated, and replays are just a seed plus inputs.
- **The rest of a live game.** Accounts, parties with chat and voice, matchmaking, ranks, leaderboards, a shop with receipts validated on the server, ads, analytics, push and an operations console for the people running it.

![A briefing](/developer-guide/img/splat-briefing.png)
![A match](/developer-guide/img/splat-match-practice.png)

## Make it your own

Fields and markers are tables, the art is plain glTF and the look is one CSS file. The [Splat Implementor's Guide](/developer-guide/#splat-implementors-guide) walks through swapping each of them, and you can start a project from the Splat template in the [Initializr](https://start.codenameone.com/).
