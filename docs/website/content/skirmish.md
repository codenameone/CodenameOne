---
title: "Build a Real-Time Card Battle Game in Java with Skirmish"
description: "Skirmish is a 3D real-time card battler with a Java backend: online lockstep matches, replays, a training battle, progression, in-app purchases, rewarded ads and a moderation console. Free MIT-0 source."
url: "/skirmish/"
summary: "A complete full-stack mobile game you can re-skin and extend."
images: ["/developer-guide/img/skirmish-battle.png"]
ShowToc: true
---

Skirmish is a complete, full-stack real-time card battler in Java. Two players drop troops, buildings and spells onto a 3D field, and the first to topple the other's towers wins. Around the battle is everything a live game needs: accounts, decks, matchmaking, challenge links, replays, a training battle, levels, quests, chests, a shop and a console for moderating players. Use it to build your own game with Codename One, or to see how a Codename One app and server share one codebase.

**[Generate your project](/initializr/)** · **[Read the user's guide](/developer-guide/skirmish-users-guide/)** · **[Browse the source](https://github.com/codenameone/CodenameOne/tree/master/scripts/skirmish)**

In the Initializr, choose the **Skirmish** card-battler template, set your application name and package, and generate the project. The application source is licensed under [MIT-0](https://github.com/codenameone/CodenameOne/blob/master/scripts/skirmish/LICENSE): you can modify it, use it commercially and keep your changes private without royalties. The characters and buildings are KayKit models by Kay Lousberg, released under CC0.

## What comes with the application?

| Part | Included workflows |
| --- | --- |
| Battle | A 3D field drawn with `com.codename1.gaming` and glTF characters, drag-and-drop cards, elixir, overtime, particle effects and a finishing camera sweep |
| Shared rules | One deterministic Java module, with no floating point, that the app, the server, the replays and the tests all run |
| Online play | Trophy-banded matchmaking, challenge links, server-authoritative lockstep over WebSocket, spectating and replays with seeking |
| Progression | A guided training battle, levels, daily quests, chests and achievements, all granted by the server |
| Monetization | Gem packs and a season pass through the platform stores with server-side receipt checks, rewarded and interstitial ads |
| Engagement | Push notifications for chests and challenges, and opt-in analytics of the player funnel |
| Administration | Live matches, automatic and reported flags, player lookup, and audited warnings, mutes, suspensions and bans |

The menus are ordinary Codename One components styled through CSS into carved wood and stone, and the same client project targets iOS, Android and desktop.

![Skirmish home screen](/developer-guide/img/skirmish-home.png)
![Skirmish battle](/developer-guide/img/skirmish-battle.png)
![Skirmish finishing camera](/developer-guide/img/skirmish-finale.png)
![Skirmish training battle](/developer-guide/img/skirmish-tutorial-drag.png)

## From first run to customization

1. **[Run the project](https://github.com/codenameone/CodenameOne/tree/master/scripts/skirmish#running-it).** The development profile needs nothing installed: an in-memory database, demo accounts and a simulated store, so you can play the bot, or a second simulator, straight away.
2. **[Play every screen](/developer-guide/skirmish-users-guide/).** The user's guide walks through the training battle, the deck, battles, replays, the shop and the admin console.
3. **[Make it your own](/developer-guide/skirmish-implementors-guide/).** The implementor's guide covers adding cards and their models, balancing, re-skinning, store products, ads, push and moderation.

## What does a live game still need?

Skirmish supplies the game and its source. Publishing one also requires your own hosting, store products, ad units, a push key and balancing.

Development uses a simulated store and Google's test ad units. Before taking real money, configure your products in App Store Connect and the Play Console and the server's receipt settings, and test purchases with sandbox accounts.

## Looking for a business app?

[Savor](/savor/) and [Wayline](/wayline/) are the sibling samples for food delivery and ride hailing, on the same architecture.
