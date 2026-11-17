---
title: "Build a Game Platform in Java with Brickyard"
description: "Brickyard is a user-generated-content game platform in Java: a pure-Java Luau interpreter compatible with Roblox place files, 3D play with friends, an in-app studio on phone, tablet and desktop, an authoritative multiplayer server, an economy and an admin console. Free MIT-0 source."
url: "/brickyard/"
summary: "A whole game platform: players play Luau experiences with friends, creators build them in an in-app 3D studio, and the server keeps everyone honest."
images: ["/developer-guide/img/brickyard-race.png"]
ShowToc: true
---

Brickyard is a game platform where the players make the games. Players browse a home screen of experiences, join friends who are already playing and race them through an obby while they chat. Creators build those experiences in a 3D studio inside the same app, on a phone, a tablet or a desktop, then publish them and earn from them. An AI agent can build one too, through the app's built-in MCP server.

The experiences are Luau scripts in a 3D world of parts, and the platform is compatible with Luau and Roblox place files, so a creator can bring an `.rbxlx` or `.rbxl` export and play it. Under it is everything such a platform needs: a scripting VM that cannot freeze the device, 3D rendering and physics, an authoritative multiplayer server, an economy with a subscription, social features, moderation and analytics. In Brickyard all of it is ordinary Codename One code in one project, and every line is original: the interpreter, the engine API, the renderer and the templates.

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/brickyard/brickyard-desktop-poster.jpg" style="width:100%;max-width:100%;height:auto;display:block;margin:0 auto" aria-label="Brickyard on a desktop: the home screen, a solo obby run with the keyboard, the studio with its menus, typing a Luau coin rain script and play testing it">
<source src="/brickyard/brickyard-desktop.mp4" type="video/mp4">
<a href="/brickyard/brickyard-desktop.mp4">Download the Brickyard desktop video</a>.
</video>

<video controls playsinline preload="metadata" poster="/brickyard/brickyard-friends-poster.jpg" style="width:100%;max-width:360px;height:auto;display:block;margin:1.5rem auto 0" aria-label="Brickyard on a phone: friends playing, joining one, a chat race and a full obby run to the win">
<source src="/brickyard/brickyard-friends.mp4" type="video/mp4">
<a href="/brickyard/brickyard-friends.mp4">Download the Brickyard friends video</a>.
</video>

<video controls playsinline preload="metadata" poster="/brickyard/brickyard-game-types-poster.jpg" style="width:100%;max-width:360px;height:auto;display:block;margin:1.5rem auto 0" aria-label="The game types: survival, roleplay, tycoon, simulator, racing and a fighting arena">
<source src="/brickyard/brickyard-game-types.mp4" type="video/mp4">
<a href="/brickyard/brickyard-game-types.mp4">Download the Brickyard game types video</a>.
</video>

<video controls playsinline preload="metadata" poster="/brickyard/brickyard-engagement-poster.jpg" style="width:100%;max-width:360px;height:auto;display:block;margin:1.5rem auto 0" aria-label="Levels, the season pass, quests, the avatar shop and leaderboards">
<source src="/brickyard/brickyard-engagement.mp4" type="video/mp4">
<a href="/brickyard/brickyard-engagement.mp4">Download the Brickyard engagement video</a>.
</video>

<video controls playsinline preload="metadata" poster="/brickyard/brickyard-studio-phone-poster.jpg" style="width:100%;max-width:360px;height:auto;display:block;margin:1.5rem auto 0" aria-label="Building an experience in the phone studio: a template, inserting a script, editing Luau and play testing">
<source src="/brickyard/brickyard-studio-phone.mp4" type="video/mp4">
<a href="/brickyard/brickyard-studio-phone.mp4">Download the Brickyard studio phone video</a>.
</video>

<video controls playsinline preload="metadata" poster="/brickyard/brickyard-studio-tablet-poster.jpg" style="width:100%;max-width:960px;height:auto;display:block;margin:1.5rem auto 0" aria-label="The tablet studio: explorer, viewport and properties side by side, inserting models, lighting presets and a play test">
<source src="/brickyard/brickyard-studio-tablet.mp4" type="video/mp4">
<a href="/brickyard/brickyard-studio-tablet.mp4">Download the Brickyard studio tablet video</a>.
</video>
{{< /guide-block >}}

**[Read the guide](/developer-guide/brickyard-implementors-guide/)** · **[Browse the source](https://github.com/codenameone/CodenameOne/tree/master/scripts/brickyard)**

The application source is licensed under [MIT-0](https://github.com/codenameone/CodenameOne/blob/master/scripts/brickyard/LICENSE): you can modify it, use it commercially and keep your changes private without royalties.

## What comes with the application?

| Part | Included workflows |
| --- | --- |
| Luau runtime | A pure-Java Luau interpreter with the full syntax, coroutines and the standard libraries, where every script runs under an instruction budget so a runaway loop is a reported error |
| Engine API | The instance tree, signals, services, GUI, tweens, pathfinding, data stores, the marketplace, badges, rigid-body physics and humanoid characters, plus a reader and writer for `.rbxlx` and `.rbxl` place files with a compatibility report |
| Play | 3D experiences drawn on the GPU, played with touch, keyboard or gamepad, with chat bubbles over each player |
| Multiplayer | An authoritative server that keeps server scripts to itself, replicates snapshots, property deltas and remote calls, and checks every move against the character's speed |
| Game types | Templates for an obby, survival, a roleplay town, a tycoon, a simulator, racing and a fighting arena |
| Studio | Explorer, properties, a Luau editor with syntax checks and completion, templates, inserted models, scripts and effects, lighting presets, undo, play test, `.rbxlx` export and publishing, laid out for phone, tablet and desktop |
| Agentic creation | The studio's tools on the app's MCP server, so an AI agent can build, play test and publish an experience |
| Engagement | Levels, daily streaks, a season pass with free and Plus tracks, daily and weekly quests, an avatar shop priced in Bricks, the Brickyard Plus subscription and leaderboards fed by the server |
| Social | Friends, live "playing" counts, joining a friend's server, invites that pay both sides and chat filtered by the reader's age |
| Creators | A dashboard with visits, retention, revenue and grouped errors, version rollback, game passes, products and payouts |
| Administration | Reports, sanctions and appeals, fraud flags, payout approval, home-row curation and an audit log of every staff action |

## What does it look like?

![The home screen with friends playing](/developer-guide/img/brickyard-home-friends.png)
![A chat race in Rainbow Sky Obby](/developer-guide/img/brickyard-race.png)
![The last stage of the obby](/developer-guide/img/brickyard-win.png)
![Daily and weekly quests](/developer-guide/img/brickyard-quests.png)
![The avatar shop](/developer-guide/img/brickyard-shop-owned.png)
![Disaster Island](/developer-guide/img/brickyard-disaster-island.png)
![Brick Factory Tycoon](/developer-guide/img/brickyard-tycoon.png)
![Kart Loop](/developer-guide/img/brickyard-kart.png)
![The Luau script editor](/developer-guide/img/brickyard-script-editor.png)
![A play test](/developer-guide/img/brickyard-playtest.png)
![The tablet studio](/developer-guide/img/brickyard-studio-tablet.png)
![The desktop studio](/developer-guide/img/brickyard-desktop-studio.png)

## From first run to customization

1. **[Run the project](https://github.com/codenameone/CodenameOne/tree/master/scripts/brickyard#running-it).** Install the engine modules, then test the server and compile the app.
2. **[Tour the platform](/developer-guide/brickyard-implementors-guide/).** The guide opens with playing, friends, engagement, the game types and the studio on every form factor.
3. **[Make it your own](/developer-guide/brickyard-implementors-guide/).** The same guide then covers the modules, the Luau runtime, place files, replication, the server, templates, the economy and the theme.

## What does a live platform still need?

Brickyard supplies the platform and its source. Running one also requires your own hosting, a database, store products with a configured receipt validator, ad units, a push key and hosting for the assets place files reference.

## Looking for another kind of game?

[Dash](/dash/) is the endless runner, [Skirmish](/skirmish/) the real-time card battler and [Splat](/splat/) the team paintball shooter.
