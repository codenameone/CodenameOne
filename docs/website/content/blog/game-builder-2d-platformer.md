---
title: "Game Builder Tutorial 1: Build a 2D Platformer From Scratch"
slug: game-builder-2d-platformer
url: /blog/game-builder-2d-platformer/
date: '2026-06-25'
author: Shai Almog
description: A complete, code-included walkthrough that builds a playable 2D platformer — "Coin Run" — with the Codename One Game Builder. Set up the project, lay out a level with coins, an enemy and a goal flag, then wire win/lose logic in the generated companion class.
feed_html: '<img src="https://www.codenameone.com/blog/gamebuilder/game-platformer.gif" alt="Coin Run, a 2D platformer built with the Game Builder" /> A complete, code-included walkthrough that builds a playable 2D platformer with the Codename One Game Builder — level design, an enemy, a goal flag, and the win/lose code.'
---

![Coin Run, a 2D platformer built with the Game Builder](/blog/gamebuilder/platformer-hero.jpg)

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

The same level, live in the preview — run right, jump the slime, grab the coins:

![Coin Run gameplay](/blog/gamebuilder/game-platformer.gif)

## What just got saved? The `.game` file

**Save** writes the level to `src/main/resources/games/CoinRun.game`. It's plain JSON — readable, diff-able, and yours to ship as a resource. (Codename One's resource namespace is flat, so at runtime you load it as `/CoinRun.game`, not `/games/CoinRun.game` — the `games/` folder just keeps your sources tidy.) A trimmed version:

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

`create-game-scene` generated the companion class. The part between the `DO NOT EDIT` markers loads the `.game` resource **and wires every object you named in the editor to a field** — because you named the player `player`, the editor generated a `player` field and even seeded `lives` from its property. The constructor and `onUpdate` are yours:

```java
public class CoinRun extends GameSceneView {
    public CoinRun(AssetCatalog catalog) {
        super(loadLevel(), catalog);   // realizes the level into a Scene of Sprites
        initScene();
    }

    //-- GAMEBUILDER GENERATED - DO NOT EDIT BELOW
    /// The "player" object you placed in the editor.
    protected Sprite player;
    private static GameLevel loadLevel() {
        try {
            return GameLevel.load(Display.getInstance().getResourceAsStream(CoinRun.class, "/CoinRun.game"));
        } catch (java.io.IOException err) {
            throw new RuntimeException("failed to load level /CoinRun.game", err);
        }
    }

    private void initScene() {
        player = findByName("player");
        if (player != null) {
            setLives(elementOf(player).getInt("lives", 3));
        }
    }
    //-- GAMEBUILDER GENERATED - DO NOT EDIT ABOVE

    @Override
    protected void onUpdate(double deltaSeconds) {
        // your game logic — see below
    }
}
```

You don't write `findByName`/`setLives` yourself — the editor regenerates that block every time you save, so renaming or adding objects in the Inspector just updates your fields.

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

`GameSceneView` also hands you the small helpers every game loop needs, so you never re-roll them: `findByAsset(id)` and `findByName(name)` locate a sprite, `findAllByAsset(id)` returns every match, `elementOf(sprite)` reads its editor properties, `overlaps(a, b)` is a null-safe collision test, and `addScore`/`getScore`/`loseLife`/`getLives`/`isGameOver` track the usual game state. They're all `protected`, so the rules below read like rules — not plumbing.

## Your rules — coins, the slime, and winning

Here's a complete `onUpdate` that scores coins, costs a life when the slime touches you, and wins at the flag. It uses the generated `player` field and the base-class helpers — no boilerplate — and reads the same properties you set in the editor:

```java
private boolean won;

@Override
protected void onUpdate(double deltaSeconds) {
    if (player == null || won) {
        return;
    }
    // collect every coin the player touches (value comes from the editor)
    for (Sprite coin : findAllByAsset("coin")) {
        if (overlaps(player, coin)) {
            addScore(elementOf(coin).getInt("value", 10));
            getScene().remove(coin);
        }
    }
    // the slime costs a life on contact; GameSceneView counts lives for you
    Sprite slime = findByAsset("slime");
    if (overlaps(player, slime)) {
        getScene().remove(slime);
        if (loseLife() == 0) {
            gameOver();
        }
    }
    // reaching the flag wins
    if (overlaps(player, findByAsset("flag"))) {
        won = true;
        youWin(getScore());
    }
}
```

That's the whole rule set — about twenty lines, all of it *your* game. Compare it to the old way: no `getUserData()` casts, no scene-scanning loops, no hand-written overlap math.

Want custom movement instead of the default? Poll input directly and move the `player` sprite yourself:

```java
GameInput in = getInput();
if (in.isGameKeyDown(Display.GAME_RIGHT)) {
    player.setX(player.getX() + 120 * deltaSeconds);
}
if (in.wasKeyPressed(Display.GAME_FIRE)) {
    // start a jump
}
```

Because you're editing `onUpdate` and not the generated block, re-running `cn1:gamebuilder` to tweak the level **keeps this logic intact**.

## Physics, effects and overriding defaults

The starter movement is lightweight arcade physics. When you want more, the gaming API has the real thing — here are concrete drop-ins.

**Real rigid-body physics (Box2D).** Swap the arcade jump for an actual physics world: gravity, stacking, slopes, and bounce. A `PhysicsBody` linked to your sprite writes its transform back into the sprite on every `step`:

```java
import com.codename1.gaming.physics.PhysicsWorld;
import com.codename1.gaming.physics.PhysicsBody;
import com.codename1.gaming.physics.BodyType;

private PhysicsWorld physics;
private PhysicsBody body;

private void enablePhysics() {                                 // call once, after the level loads
    physics = new PhysicsWorld(0, (float) (getLevel().getDouble("gravity", 9.8) * 100));
    physics.createBox(0, getHeight() - 16, getWidth(), 16, BodyType.STATIC);   // the floor
    body = physics.createBox((float) player.getX(), (float) player.getY(), 24, 32, BodyType.DYNAMIC);
    body.setLinkedSprite(player);                              // step() drives the sprite
}

// in onUpdate:
physics.step((float) deltaSeconds);
if (getInput().wasKeyPressed(Display.GAME_FIRE)) {
    body.applyLinearImpulse(0, -400);                          // jump
}
```

**Sound and effects.** Play a clip on pickup and add a little juice — the gaming API ships a low-latency `SoundPool`:

```java
private final SoundPool sound = SoundPool.create(8);
private SoundEffect coinSfx;                                   // coinSfx = sound.load("/coin.wav");

// inside the coin branch, instead of a bare remove:
sound.play(coinSfx);                                           // ping!
int shake = (int) (Math.random() * 5 - 2);                     // a one-frame screen-shake on a hit
getScene().setCamera(shake, shake);
getScene().remove(coin);
```

**Overriding defaults.** Don't want auto-collected coins or auto-patrolling enemies? Ignore the defaults and drive everything from `onUpdate` (as the custom-movement snippet showed). The defaults are a convenience, not a constraint.

## Menus, HUD and pause — where Codename One spoils you

This is the part most game engines make painful and Codename One makes trivial: **the menus**. A `GameSceneView` is an ordinary Codename One `Component`, so the *entire* Codename One UI toolkit — `Form`, `Toolbar`, `Dialog`, layouts, CSS theming, animations — is right there around your game. The level select, the pause screen, the settings page, the score HUD: all of it is the same UI API you'd use for any app, not a bespoke game-UI framework you have to learn.

A **score/lives HUD** is just a label laid over the game:

```java
Form f = new Form("Coin Run", new BorderLayout());
Label hud = new Label("Score 0   Lives 3");
f.add(BorderLayout.NORTH, hud).add(BorderLayout.CENTER, game);
// update it each frame from onUpdate via callSerially:
hud.setText("Score " + getScore() + "   Lives " + getLives());
```

A **pause menu** is a one-line `Dialog` over the frozen game:

```java
game.stop();                                                   // freeze the loop
Command resume = new Command("Resume");
Command quit = new Command("Quit to menu");
if (Dialog.show("Paused", "Score: " + game.getScore(), resume, quit) == resume) {
    game.start();
} else {
    showLevelMenu();
}
```

And a **level-select screen** is a themed `Form` with a real toolbar — the kind of polish that's a slog elsewhere:

```java
Form menu = new Form("Coin Run", BoxLayout.y());
menu.getToolbar().addCommandToRightBar("Settings", null, e -> showSettings());
for (String level : new String[] {"CoinRun", "Caverns", "SkyRun"}) {
    Button play = new Button(level);
    play.addActionListener(e -> startLevel(level));            // loads /<level>.game
    menu.add(play);
}
menu.show();
```

Because it's all standard Codename One UI, your menus inherit your app's theme, your fonts, right-to-left support, accessibility and the simulator's live preview — for free.

## The finished game and where to go next

You now have **Coin Run**: a drawn level, an enemy, a goal, and roughly forty lines of rules. Natural next steps:

* **More levels** — ship many `.game` files and load whichever the player picks from the level-select screen above; the companion pattern is identical.
* **Respawn and persistence** — on `gameOver`, reload the level; save the high score with `Storage`.
* **Smarter enemies** — read a `patrol` range or `behavior` property per slime and branch in `onUpdate`.
* **Hazards** — the **Spike** tile and a `damage` property make instant-death floors.

Next: [Tutorial 2 — a turn-based board game](/blog/game-builder-board-game/), where the same data-plus-`onUpdate` pattern drives game *rules* instead of arcade motion.
