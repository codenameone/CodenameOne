---
title: "Build a Twin-Stick Arena Shooter in Java with Papercut"
description: "Papercut is a twin-stick arena shooter in Java, drawn in cut paper and ink on a notebook page: three modes, a daily sheet, leaderboards kept honest by single-use run tickets, a shop, achievements and an admin console. Free MIT-0 source."
url: "/papercut/"
summary: "A cut-paper arena shooter: a rippling notebook page, ink stains that never wash off, a multiplier to x20 and a server that keeps the scores honest."
images: ["/developer-guide/img/papercut-arena.png"]
ShowToc: true
---

Papercut is a twin-stick arena shooter played on a page torn out of a school notebook. You fly a paper quill, your right thumb aims the pencil, and folded paper creatures pour in from the margins: darts that chase, pinwheels that crowd, cranes that unfold into cranelets when cut, snippers that dash across the page and inkwells that pull everything toward them. Every creature you cut leaves its ink behind, so by the end of a long run the page is a map of everything that happened on it.

Under the paper it's the kind of game that usually starts with an engine: crowds of sprites, a page that ripples like a spring grid when something bursts, two thumb sticks, keyboard, mouse and gamepad. In Papercut all of it is ordinary Codename One code, drawn with `com.codename1.gaming`, in one project with its server.

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/papercut/papercut-desktop-poster.jpg" style="width:100%;max-width:960px;height:auto;display:block;margin:0 auto" aria-label="Papercut on a desktop: the home screen playing itself behind the menu, a Classic run with creatures cut into ink stains as the multiplier climbs, the result card, then a Fold run steering through paper gates">
<source src="/papercut/papercut-desktop.mp4" type="video/mp4">
<a href="/papercut/papercut-desktop.mp4">Download the Papercut desktop video</a>.
</video>

<video controls playsinline preload="metadata" poster="/papercut/papercut-mobile-poster.jpg" style="width:100%;max-width:360px;height:auto;display:block;margin:1.5rem auto 0" aria-label="The same session on a phone: the home screen, a Classic run, the result card and a Fold run">
<source src="/papercut/papercut-mobile.mp4" type="video/mp4">
<a href="/papercut/papercut-mobile.mp4">Download the Papercut phone video</a>.
</video>
{{< /guide-block >}}

**[Generate your project](/initializr/)** · **[Read the guide](/developer-guide/papercut-implementors-guide/)** · **[Browse the source](https://github.com/codenameone/CodenameOne/tree/master/scripts/papercut)**

In the Initializr, choose the **Papercut** arena shooter template, set your application name and package, and generate the project. The application source is licensed under [MIT-0](https://github.com/codenameone/CodenameOne/blob/master/scripts/papercut/LICENSE): you can modify it, use it commercially and keep your changes private without royalties. The art is drawn by a script in the project over CC0 paper textures from ambientCG, the sounds are CC0 from OpenGameArt, and the type is Gochi Hand and Patrick Hand under the SIL Open Font License, so there is nothing to buy or license.

## What comes with the application?

![The Papercut home screen](/developer-guide/img/papercut-home.png)


| Part | Included workflows |
| --- | --- |
| The arena | Six creature kinds, ink drops that raise the multiplier to x20, an eraser that wipes the page, extra lives, ink stains that stay, and a notebook page simulated as a grid of springs |
| Modes | Classic, Fold (no pencil: steer between the ends of paper gates) and Today's sheet, Classic on a seed every player shares |
| Controls | A floating stick on each half of the screen, WASD and arrows, mouse aim, and gamepads |
| Honest scores | Single-use run tickets and score bounds both sides agree on, with remarkable runs flagged for review, and no copy of the game on the server |
| Leaderboards | All time, this week, today and friends, for every mode |
| Monetization | Paper quills bought with coins, coin packs through the platform stores with server-side receipt checks, and rewarded ads |
| Progression | Achievements worked out from your standing runs |
| Administration | Player search, flags, audited actions, broadcasts and picking the daily seed |
| Art pipeline | One Python script that redraws every creature, stain and icon, so re-skinning is a palette and a texture swap |

## From first run to customization

1. **[Run the project](/developer-guide/papercut-implementors-guide/).** The development profile needs nothing installed: an in-memory database, demo accounts, a field of rivals on every board and a simulated store.
2. **[Learn how it works](/developer-guide/papercut-implementors-guide/).** The guide covers the arena that runs without a screen, the spring page, the two sticks, and how the server keeps scores honest without replaying runs.
3. **[Make it your own](/developer-guide/papercut-implementors-guide/).** Add a creature, change the palette, swap the paper for a blueprint or a chalkboard, and set up your store products.

## What does a live game still need?

Papercut supplies the game and its source. Publishing one also requires your own hosting, store products and ad units.

## Looking for another kind of game?

[Skirmish](/skirmish/) is the real-time card battler, [Dash](/dash/) the endless runner and [Splat](/splat/) the team paintball shooter, all on the same architecture.
