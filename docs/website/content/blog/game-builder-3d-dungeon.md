---
title: "Game Builder Tutorial 3: Build a First-Person 3D Dungeon"
slug: game-builder-3d-dungeon
url: /blog/game-builder-3d-dungeon/
date: '2026-07-09'
author: Shai Almog
description: A code-included walkthrough that builds a first-person 3D dungeon with the Codename One Game Builder — pick a play style, sculpt terrain, build walls, place a spawn, then drive first-person logic in the companion class. Includes a gameplay clip and how to scale to large streaming worlds.
feed_html: '<img src="https://www.codenameone.com/blog/gamebuilder/game-dungeon.gif" alt="A first-person 3D dungeon built with the Game Builder" /> A code-included walkthrough that builds a first-person 3D dungeon with the Codename One Game Builder — terrain, walls, a spawn, the companion code, and streaming large worlds.'
---

![A first-person 3D dungeon built with the Game Builder](/blog/gamebuilder/dungeon-hero.jpg)

The final tutorial takes the same data-driven pattern from [Tutorial 1](/blog/game-builder-2d-platformer/) and [Tutorial 2](/blog/game-builder-board-game/) into **3D**. Duke's last adventure sends him underground: **Crypt Walk**, a first-person dungeon where pillars are walls to navigate — and, once you wire them in (see the closing section), the exception monsters from Tutorial 1 to dodge. The headline difference from the 2D tutorial is what happens at runtime — instead of sprites in a `Scene`, each element becomes a GPU-rendered `Model` under a perspective camera with lighting. You author the same way; the runtime renders it in 3D.

Project setup is identical to Tutorial 1 — only the mode changes:

```bash
mvn cn1:create-game-scene -DclassName=com.example.crypt.CryptWalk -Dmode=3d
mvn cn1:gamebuilder
```

## Why 3D mode (and what "play style" means)

3D games come in families with very different cameras and controls: a flight sim has no ground, a racer follows a road, a dungeon crawler walks first-person between walls. Rather than make you wire each, the builder offers a **play style** — *open*, *flight*, *race*, *dungeon* — that picks a sensible camera, movement model, and collision for you, so the preview behaves like the genre while you design.

## Step 1 — A 3D scene

Pick **New scene → 3D Map**. Placement defaults to an accurate top-down grid (toggle **View → 3D: Perspective / Top-down** for an angled overview). The **3D Kit** pack supplies blocks, pillars, crates, a spawn and scenery.

![A new 3D scene with the 3D Kit](/blog/gamebuilder/dungeon-1-new-scene.png)

## Step 2 — Pick the dungeon play style

With nothing selected, set the Inspector's **3D play style** to **dungeon**. That switches the preview to a first-person walker with wall collision — the genre we're building. (Switch it to *open* for a free arena or *flight* for an aerial flyby of the same layout; the level data doesn't change, only how it plays.)

![Choosing the dungeon play style](/blog/gamebuilder/dungeon-2-style.png)

## Step 3 — Build the crypt walls

From the **3D Kit**, place **Pillar** assets to form walls and corridors, and a **Rock** as scenery. *Why pillars as walls?* In dungeon style, solid (non-collectible) elements block movement, so a row of pillars is a wall you can't walk through. Use the Inspector's per-object **Size** to vary them — that scales only the selected object, not the shared asset, so you can mix fat columns and thin posts.

![Building walls from pillars](/blog/gamebuilder/dungeon-3-walls.png)

## Step 4 — Add the player spawn

Place a **Spawn** where the player starts and tick **This is the player** in its Behavior section — that marks which element the camera and controls drive. Its **Elevation (Z)** field raises it for multi-level layouts.

![Adding the player spawn](/blog/gamebuilder/dungeon-4-spawn.png)

## Step 5 — Walk it

Press **Live**. In dungeon style you walk in **first person** — **Left/Right turn, Up/Down walk** — and the pillars stop you like real walls. A radar in the corner shows where everything is, which is essential once a level grows beyond one room.

![Walking the dungeon in first person](/blog/gamebuilder/dungeon-5-walk.png)

Live, you walk the corridor in first person and look around — the pillars stop you like real walls:

![Walking the dungeon, in the Game Builder preview](/blog/gamebuilder/game-dungeon.gif)

## Sculpting terrain (floors, hills, holes, ramps)

3D levels aren't just objects on a flat plane. Select the **Terrain** tool and you can paint the ground itself: **Raise/Lower** elevation, carve **holes** (open sky a flight level can fall through), stamp **walls**, and **Paint** a surface material — grass, road, stone, sand, water. Painted elevation changes render as **smooth slopes**, not stairs, so a road can ramp uphill. The walker rides the terrain height and is stopped by walls and holes.

## What got saved, and how it renders

**Save** writes `src/main/resources/games/CryptWalk.game` (loaded at runtime as `/CryptWalk.game` — the resource namespace is flat). A 3D level stores the play style, the placed elements (with elevation and per-object scale), and any terrain you sculpted:

```json
{
  "mode": "3d", "cols": 16, "rows": 16, "tileSize": 1,
  "props": { "view3d": "dungeon" },
  "camera": { "eye": [0,8,14], "target": [0,0,0], "fov": 60 },
  "lights": [ { "dir": [0.4,-1,0.3], "color": "fff2e0", "ambient": "2a2f3a" } ],
  "elements": [
    { "id": "w1", "assetId": "pillar", "layer": "Models", "x": 6, "y": 5, "scaleX": 1 },
    { "id": "sp", "assetId": "spawn",  "layer": "Models", "x": 8, "y": 11, "props": { "player": true } }
  ],
  "terrain": { "cols": 16, "rows": 16, "heights": [ ], "materials": [ ] }
}
```

The companion is the same `GameSceneView` pattern as the other tutorials. The difference is realization: for a 3D level it builds one `Model` per element inside `onSetup(GraphicsDevice)` under a perspective camera with the level's lighting — on the GPU. Your code still lives in `onUpdate`:

```java
@Override
protected void onUpdate(double deltaSeconds) {
    GameInput in = getInput();
    // first-person controls (or let the built-in dungeon walker handle them)
    if (in.isGameKeyDown(Display.GAME_LEFT))  { turn(-1.4 * deltaSeconds); }
    if (in.isGameKeyDown(Display.GAME_RIGHT)) { turn( 1.4 * deltaSeconds); }
    if (in.isGameKeyDown(Display.GAME_UP))    { walkForward(3.0 * deltaSeconds); }
    // win when the player reaches the exit element, lose on a trap, etc.
}
```

> The in-editor preview is a fast software approximation for iteration; on device the scene is rendered by the GPU-accelerated `GameSceneView`.

## Physics, effects and overriding defaults

**Collision** is handled for you — dungeon style stops the walker at walls and holes. For richer physics (projectiles, doors, movable crates) step a `PhysicsWorld` from `onUpdate` exactly as in [Tutorial 1's physics section](/blog/game-builder-2d-platformer/#physics-effects-and-overriding-defaults).

**Effects** hang off the same loop. A concrete example — a footstep sound and a torch-lit light fade, both reading the level's own data:

```java
private final SoundPool sound = SoundPool.create(4);
private SoundEffect step;            // step = sound.load("/footstep.wav");

// in onUpdate, when the player advances a tile:
sound.play(step, 0.6f, 0f, 1f, 0);   // volume, pan, rate, no loop
getLight().setColor(torchLit ? 0xfff2e0 : 0x404858);   // brighten when a torch is lit
```

**Overriding** — the play styles are presets, not constraints. Want a hybrid (walk like a dungeon but with no wall collision)? Set *open* and add your own collision in `onUpdate`.

## Menus and HUD in 3D

A 3D game has its own interface to manage too — a map toggle, an inventory, a "you died" screen, a pause overlay — and because `GameSceneView` is a Codename One `Component`, all of it is the ordinary UI toolkit, not a 3D-specific layer. Drop a `Dialog` for the death screen, a `Toolbar` command for the map toggle, a `Container` of item buttons for the inventory. [Tutorial 1's menu section](/blog/game-builder-2d-platformer/#menus-hud-and-pause-where-codename-one-spoils-you) applies unchanged — the game underneath happens to be 3D, but the menus are pure Codename One.

## Scaling up: streaming worlds

A single 16×16 room fits in memory, but an open-world RPG does not. The gaming runtime supports a **streaming, region-based world**: terrain is paged in and out as chunks, the map is split into linked **regions** that load and unload around the player for seamless area transitions, and surfaces use pluggable **materials**. In the editor, **New scene → Large World** creates a region graph you grow with *Add region* (north/south/east/west); the active region's streaming terrain is what you edit and preview. The same `.game`/companion pattern loads it.

## The finished game and next steps

You have **Crypt Walk**: a navigable first-person dungeon with sculpted terrain and wall collision. From here:

* **Enemies** — place actors and, in `onUpdate`, move them toward the player and cost a life on contact (the slime logic from [Tutorial 1](/blog/game-builder-2d-platformer/#your-rules-coins-the-slime-and-winning) ports directly).
* **A goal** — mark an *exit* element and end the level when the player reaches it.
* **Items and doors** — collectibles open locked corridors; read a `key`/`locked` property.
* **Bigger maps** — graduate to a Large World of streaming regions for a full crawler.

That's the trilogy — `mvn cn1:create-game-scene` then `mvn cn1:gamebuilder`, and you're building games by drawing them.
