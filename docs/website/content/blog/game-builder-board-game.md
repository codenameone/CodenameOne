---
title: "Game Builder Tutorial 2: Build a Turn-Based Board Game"
slug: game-builder-board-game
url: /blog/game-builder-board-game/
date: '2026-07-02'
author: Shai Almog
description: A code-included walkthrough that builds an isometric board game with the Codename One Game Builder — lay the board, place pieces tagged with the data your rules read, and drive turn logic from the generated companion class.
feed_html: '<img src="https://www.codenameone.com/blog/gamebuilder/board-3-pieces.png" alt="A board game built with the Game Builder" /> A code-included walkthrough that builds an isometric board game with the Codename One Game Builder — lay the board, tag the pieces, and drive turn logic from code.'
---

![A board game laid out in the Game Builder](/blog/gamebuilder/board-3-pieces.png)

In [Tutorial 1](/blog/game-builder-2d-platformer/) the player moved with arcade physics. A board game has none of that — pieces sit on squares and the *rules* decide what happens. This tutorial shows how the same Game Builder pattern (visual data + an `onUpdate` companion) handles a turn-based game, where your code reads per-piece properties instead of simulating motion. We'll build **Checkers Start**: a board, two players' tokens, and the scaffolding to drive turns.

If you haven't set up a project yet, the [project setup in Tutorial 1](/blog/game-builder-2d-platformer/#step-0-create-the-project-and-launch-the-editor) applies verbatim — only the mode changes:

```bash
mvn cn1:create-game-scene -DclassName=com.example.checkers.Checkers -Dmode=board
mvn cn1:gamebuilder
```

## Why board mode?

Board mode renders your grid through an **isometric projection** (`IsoProjection`), so the same flat data you author becomes a clean 2.5D board at runtime — no per-piece 3D work. You author in a simple top-down grid; the runtime tilts it. That's the whole appeal: design like a spreadsheet, ship like a board.

## Step 1 — A board scene

Pick **New scene → Board**. You get a **Board** (tile) layer for the squares and a **Pieces** (entity) layer for the movable tokens. Keeping squares and pieces on separate layers matters: the board is static grid data, while pieces are objects your rules move.

![A new board scene](/blog/gamebuilder/board-1-new-scene.png)

## Step 2 — Lay the board

Select the **Board** layer, pick the **Square** tile, and fill the grid. Because it's a tile layer, the whole board is just a compact `cell → assetId` map — cheap to store and render.

![Laying the board tiles](/blog/gamebuilder/board-2-tiles.png)

## Step 3 — Place pieces and tag them with data

Select the **Pieces** layer, pick **Token**, and stamp pieces on their squares. This is the key idea: in the Inspector's **Behavior** section, tag each piece with the data your rules need — `player = 1` (or `2`) for ownership and `cell = a1` for its board coordinate. Your code never hard-codes piece positions; it reads these properties.

![Placing pieces tagged with player and cell](/blog/gamebuilder/board-3-pieces.png)

## Step 4 — Build

**Build** writes `src/main/resources/games/Checkers.game`. The board, the pieces and their properties all live there as data:

```json
{
  "mode": "board", "cols": 8, "rows": 8, "tileSize": 64,
  "layers": [
    { "name": "Board",  "kind": "tile",   "tiles": { "0,0": "boardtile", "1,1": "boardtile" } },
    { "name": "Pieces", "kind": "entity" }
  ],
  "elements": [
    { "id": "p1", "assetId": "token", "layer": "Pieces", "x": 32, "y": 32,   "props": { "player": 1, "cell": "a1" } },
    { "id": "p2", "assetId": "token", "layer": "Pieces", "x": 160, "y": 160, "props": { "player": 2, "cell": "c3" } }
  ]
}
```

![The finished board ready to build](/blog/gamebuilder/board-4-build.png)

## The rules live in code

The companion is the same shape as Tutorial 1 — `GameSceneView` with a generated `loadLevel()` and your `onUpdate`. Board mode realizes the pieces through the isometric projection, and each sprite's `getUserData()` is its source `GameElement`, so you read the `player`/`cell` tags you set in the editor. A minimal turn skeleton:

```java
private int currentPlayer = 1;   // whose turn it is

@Override
protected void onUpdate(double deltaSeconds) {
    GameInput in = getInput();
    if (!in.wasPointerPressed()) {
        return;   // turn-based: act only on a tap
    }
    Sprite tapped = pieceAt(in.getPointerX(), in.getPointerY());
    if (tapped == null) {
        return;
    }
    GameElement piece = (GameElement) tapped.getUserData();
    if (piece.getInt("player", 0) != currentPlayer) {
        return;   // not your piece — ignore
    }
    // ...select it, validate a move against piece.getString("cell", ""), then:
    endTurn();
}

private void endTurn() {
    currentPlayer = currentPlayer == 1 ? 2 : 1;
}

private Sprite pieceAt(int px, int py) {
    Scene scene = getScene();
    for (int i = 0; i < scene.size(); i++) {
        Sprite s = scene.get(i);
        if (Math.abs(s.getX() - px) < 32 && Math.abs(s.getY() - py) < 32) {
            return s;
        }
    }
    return null;
}
```

Moving a piece is just updating its sprite position (and the element's `cell` if you persist state). Because the level is data, you can ship a *King's Court* board and a *Checkers* board as two `.game` files and load whichever the player picks — the rules code is identical.

## Variations and next steps

The board pattern generalizes to most grid games:

* **A card game** — use the **Card** and **Dice** actors with `suit` / `rank` / `sides` properties; deal by spawning sprites from a shuffled list.
* **Capture rules** — when a move lands on an opponent's `cell`, remove that sprite from the `Scene` (exactly like the coin pickup in Tutorial 1).
* **Win check** — after `endTurn`, scan the pieces and test your victory condition (no opposing pieces, a piece on the back rank, and so forth).
* **AI** — for player 2, pick a legal move in code instead of waiting for a tap.

Next: [Tutorial 3 — a first-person 3D dungeon](/blog/game-builder-3d-dungeon/), where the same data drives a 3D `GameView` with walls, terrain, and models.
