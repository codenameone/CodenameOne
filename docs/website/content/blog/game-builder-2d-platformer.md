---
title: "Game Builder Tutorial 1: Build a 2D Platformer From Scratch"
slug: game-builder-2d-platformer
url: /blog/game-builder-2d-platformer/
date: '2026-06-25'
author: Shai Almog
description: A complete, code-included walkthrough that builds a playable 2D platformer — "Coin Run" — with the Codename One Game Builder. Set up the project, lay out a level with coins, an enemy and a goal flag, then wire win/lose logic in the generated companion class.
feed_html: '<img src="https://www.codenameone.com/blog/gamebuilder/game-platformer.gif" alt="Coin Run, a 2D platformer built with the Game Builder" /> A complete, code-included walkthrough that builds a playable 2D platformer with the Codename One Game Builder — level design, an enemy, a goal flag, and the win/lose code.'
---

![Coin Run running in the Game Builder](/blog/gamebuilder/game-platformer.gif)

Most game tutorials make you hand-place every sprite in code. The [Game Builder](/manual/game-builder/) flips that around: you **draw** the level visually, tag objects with the numbers your game needs (`lives`, `value`, `speed`), and the editor saves it as a small data file that the runtime plays. Your code shrinks to the part that's actually *yours* — the rules.

This is the first of three tutorials. We'll build **Coin Run**: a side-scroller where the player runs right across a grassy floor, collects three coins, dodges a slime, and reaches a flag to win. By the end you'll have a *running game* and understand every moving part — the level file, the generated companion class, the default physics you get for free, and where your own logic goes. Tutorials [2 (board game)](/blog/game-builder-board-game/) and [3 (3D dungeon)](/blog/game-builder-3d-dungeon/) build on it.

## Why a game builder?

A Codename One game is a `GameView` holding a `Scene` of sprites, driven by a `GameInput`. Building a level purely in Java means dozens of `new Sprite(...)` / `setX`/`setY` calls that you can't *see* until you run them, and that you must recompile to tweak. The builder replaces that with a visual editor and a plain-data level file, so:

* **Designers and developers** can both touch the level — it's data, not code.
* You **iterate visually** — drag a coin, hit play, repeat — with no rebuild.
* The same file ships to **2D, isometric board, and 3D** runtimes unchanged.

## Step 0 — Create the project and launch the editor

The builder is a Maven goal that attaches to a **Java 17** Codename One project (it uses records and modern APIs the editor needs; your *game* still runs on every Codename One platform). Generate a project from the [Codename One initializr](https://start.codenameone.com) (pick Java 17), or with the archetype:

```bash
mvn -B archetype:generate \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeVersion=LATEST \
  -DgroupId=com.example -DartifactId=coinrun -Dcn1Version=LATEST
cd coinrun
```

Scaffold a 2D scene and open the editor bound to the project:

```bash
mvn cn1:create-game-scene -DclassName=com.example.coinrun.CoinRun -Dmode=2d
mvn cn1:gamebuilder
```

`create-game-scene` writes two files into your project: `src/main/resources/games/CoinRun.game` (an empty 2D level) and `src/main/java/com/example/coinrun/CoinRun.java` (the companion class — more on it below). `cn1:gamebuilder` launches the editor, already pointed at that scene. Three panels matter: the **Hierarchy** (layers and objects), the **Asset Library** (the art you stamp down), and the **Inspector** (the selected object's transform and behavior values).

## Step 1 — A 2D scene

If you scaffolded with `-Dmode=2d` the scene is ready; otherwise pick **New scene → 2D Platformer**. You get four layers — Background, Terrain, Items, Actors — which set draw order (Background paints first, Actors last). Layers also let you hide or lock parts of the level while you work.

![A new 2D platformer scene](/blog/gamebuilder/platformer-1-new-scene.png)

## Step 2 — Paint the ground

Select the **Terrain** layer, pick the **Grass** tile, and drag across the bottom row. *Why a tile layer?* Tiles are a compact grid of `assetId`-per-cell — perfect for a floor of identical blocks — and the runtime batch-renders them, so a long floor costs almost nothing. Leave a gap or raise a few tiles to make a ledge to jump.

![Painting a grass floor](/blog/gamebuilder/platformer-2-ground.png)

## Step 3 — Place the player and give it behavior

Select the **Actors** layer, pick **Player**, and click where it starts. *Actors* are freely positioned objects (not grid-snapped like tiles), because a character lives at an arbitrary point and moves smoothly. With the player selected, open the Inspector's **Behavior** section and set the numbers your code will read: `lives = 3` and `jumpHeight = 110`. Use **Add property** for any custom field — these are just typed key/value pairs stored with the object.

![Placing the player and setting behavior values](/blog/gamebuilder/platformer-3-player.png)

## Step 4 — Add coins to collect

Pick **Coin** and stamp three above the floor. Give each a `value` (say `10`) in its Behavior section — that's the score it's worth. Coins are the *task*: the reason to move across the level. The preview already collects them on contact (a built-in behavior), so you can test immediately.

![Stamping coins to collect](/blog/gamebuilder/platformer-4-coins.png)

## Step 5 — Add an enemy and a goal

A game needs stakes and an end. Pick **Slime** and place it on the floor to the right; give it `speed = 40` so it patrols. Then pick **Flag** and place it at the far edge — reaching it is winning. Now the level has a loop: *run right, grab coins, time your jump past the slime, touch the flag.*

![Adding a patrolling slime enemy and a goal flag](/blog/gamebuilder/platformer-5-enemy-goal.png)

## Step 6 — Scene-wide rules

Deselect everything to edit the whole level. In the Inspector set **Gravity** (try `9.8`) and the **Background** (Sky). Gravity is a level property the platformer physics reads — raise it for a heavier, snappier feel, lower it for floaty moon-jumps.

![Scene-wide gravity and background](/blog/gamebuilder/platformer-6-scene.png)

## Step 7 — Play it

Press **Live**. Move with the **arrow keys**, **Up / Space** to jump. Gravity and tile collision are simulated, coins add to the SCORE, and the slime patrols. **Stop** returns to editing — playing never mutates your level.

![Playing Coin Run in the editor](/blog/gamebuilder/platformer-7-play.png)

## What just got saved? The `.game` file

**Build** (or saving) writes the level to `src/main/resources/games/CoinRun.game`. It's plain JSON — readable, diff-able, and yours to ship as a resource. A trimmed version:

```json
{
  "mode": "2d", "cols": 26, "rows": 16, "tileSize": 32,
  "props": { "gravity": 9.8, "background": "Sky" },
  "layers": [
    { "name": "Terrain", "kind": "tile", "tiles": { "0,14": "grass", "1,14": "grass" } },
    { "name": "Actors", "kind": "entity" }
  ],
  "elements": [
    { "id": "e1", "assetId": "player", "layer": "Actors", "x": 64, "y": 416,
      "props": { "lives": 3, "jumpHeight": 110 } },
    { "id": "e2", "assetId": "coin",  "layer": "Actors", "x": 160, "y": 384, "props": { "value": 10 } },
    { "id": "e7", "assetId": "slime", "layer": "Actors", "x": 352, "y": 416, "props": { "speed": 40 } },
    { "id": "e8", "assetId": "flag",  "layer": "Actors", "x": 768, "y": 416 }
  ]
}
```

Nothing is hard-coded into Java. To re-edit, run `mvn cn1:gamebuilder` again — the editor rewrites this file and *preserves your code*.

## Loading and showing the game

`create-game-scene` generated the companion class. The part between the `DO NOT EDIT` markers loads the `.game` resource; the constructor and `onUpdate` are yours:

```java
public class CoinRun extends GameSceneView {
    public CoinRun(AssetCatalog catalog) {
        super(loadLevel(), catalog);   // realizes the level into a Scene of Sprites
        initScene();
    }

    //-- GAMEBUILDER GENERATED - DO NOT EDIT BELOW
    private static GameLevel loadLevel() {
        try {
            return GameLevel.load(Display.getInstance().getResourceAsStream(CoinRun.class, "/games/CoinRun.game"));
        } catch (java.io.IOException err) {
            throw new RuntimeException("failed to load level /games/CoinRun.game", err);
        }
    }
    //-- GAMEBUILDER GENERATED - DO NOT EDIT ABOVE

    @Override
    protected void onUpdate(double deltaSeconds) {
        // your game logic — see below
    }
}
```

`GameSceneView` is a `GameView` (a Codename One `Component`), so you show it like any other and `start()` its loop:

```java
public class CoinRunApp {
    public void start() {
        Form f = new Form("Coin Run", new BorderLayout());
        CoinRun game = new CoinRun(StarterAssets.catalog());   // your AssetCatalog
        f.add(BorderLayout.CENTER, game);
        f.show();
        game.start();                                          // begins the game loop
    }
}
```

That's a complete, playable game already — the floor, coins, slime and flag are live sprites, with the default behaviors below running. Everything after this is *your* rules.

## What you get for free (default behavior)

`GameSceneView` realizes every `GameElement` into a `Sprite` whose `getUserData()` is the source element — so at runtime you still have the `lives`/`value`/`speed` numbers you typed in the editor. The starter behaviors (gravity, tile collision, arrow-key movement, jump, coin pickup, enemy patrol) are what you saw in the preview. You override or extend them in `onUpdate(double deltaSeconds)`, called once per frame.

## Your rules — coins, the slime, and winning

Here's a complete `onUpdate` that scores coins, costs a life when the slime touches you, and wins at the flag. It reads the same properties you set in the editor and manipulates the live `Scene`:

```java
private int score;
private int lives = -1;          // lazy-init from the player's "lives" property
private boolean won;

@Override
protected void onUpdate(double deltaSeconds) {
    Scene scene = getScene();
    Sprite player = findByAsset(scene, "player");
    if (player == null || won) {
        return;
    }
    if (lives < 0) {
        lives = element(player).getInt("lives", 3);
    }

    for (int i = scene.size() - 1; i >= 0; i--) {
        Sprite s = scene.get(i);
        if (s == player || !overlaps(player, s)) {
            continue;
        }
        String asset = element(s).getAssetId();
        if ("coin".equals(asset)) {
            score += element(s).getInt("value", 10);   // the value you set in the editor
            scene.remove(s);                            // pick it up
        } else if ("slime".equals(asset)) {
            if (--lives <= 0) {
                gameOver();
            }
            scene.remove(s);                            // simple: enemy is consumed on hit
        } else if ("flag".equals(asset)) {
            won = true;
            youWin(score);
        }
    }
}

// helpers
private static GameElement element(Sprite s) {
    return (GameElement) s.getUserData();
}

private Sprite findByAsset(Scene scene, String assetId) {
    for (int i = 0; i < scene.size(); i++) {
        if (assetId.equals(element(scene.get(i)).getAssetId())) {
            return scene.get(i);
        }
    }
    return null;
}

private boolean overlaps(Sprite a, Sprite b) {
    return Math.abs(a.getX() - b.getX()) < 24 && Math.abs(a.getY() - b.getY()) < 24;
}
```

Want custom movement instead of the default? Poll input directly and move the player sprite yourself:

```java
GameInput in = getInput();
if (in.isGameKeyDown(Display.GAME_RIGHT)) {
    player.setX(player.getX() + 120 * deltaSeconds);
}
if (in.wasKeyPressed(Display.GAME_FIRE)) {
    // start a jump
}
```

Because you're editing `onUpdate` and not the generated load block, re-running `cn1:gamebuilder` to tweak the level **keeps this logic intact**.

## Physics, effects and overriding defaults

* **Physics.** The starter movement is lightweight arcade physics driven by the level's `gravity`. For real rigid-body physics — slopes, stacking, bouncing — wrap the scene in the gaming API's Box2D physics world and step it from `onUpdate`; the same `GameElement` properties (mass, restitution) carry over.
* **Effects.** Trigger a `SoundPool` clip on coin pickup, spawn a short `AnimatedSprite` for a sparkle, or shake the camera on a hit — all from the same collision branch above.
* **Overriding defaults.** Don't want auto-collected coins or auto-patrolling enemies? Ignore the defaults and drive everything from `onUpdate` (as the movement snippet shows). The defaults are a convenience, not a constraint.

## The finished game and where to go next

You now have **Coin Run**: a drawn level, an enemy, a goal, and roughly forty lines of rules. Natural next steps:

* **More levels** — ship many `.game` files and load whichever the player picks; the companion pattern is identical.
* **Lives UI and respawn** — draw `score`/`lives` with a `Label` overlay; on `gameOver`, reload the level.
* **Smarter enemies** — read a `patrol` range or `behavior` property per slime and branch in `onUpdate`.
* **Hazards** — the **Spike** tile and a `damage` property make instant-death floors.

Next: [Tutorial 2 — a turn-based board game](/blog/game-builder-board-game/), where the same data-plus-`onUpdate` pattern drives game *rules* instead of arcade motion.
