# Evidence map

Source: `docs/website/content/blog/game-builder-2d-platformer.md`
Canonical: https://www.codenameone.com/blog/game-builder-2d-platformer/

## Thesis

Building a playable Java platformer from sprite data, collision shapes, and rules

## Supported beats

- **Why a game builder?:** A Codename One game is a GameView holding a Scene of sprites, driven by a GameInput. Building a level purely in Java means dozens of new Sprite(...) / setX/setY calls that you can't see until you run them, and that you must recompile to tweak.
- **What just got saved? The .game file:** Save writes the level to src/main/resources/games/CoffeeRun.game. It's plain JSON — readable, diff-able, and yours to ship as a resource. (Codename One's resource namespace is flat, so at runtime you load it as /CoffeeRun.game, not /games/CoffeeRun.game — the games/ folder just keeps your sources tidy.) A trimmed version.
- **Loading and showing the game:** create-game-scene generated the companion class. The part between the DO NOT EDIT markers loads the .game resource and wires every object you named in the editor to a field — because you named the player player, the editor generated a player field and even seeded lives from its property.
- **Preview, runtime, and the arcade behavior:** One thing worth being precise about, because it trips people up: the editor's Live preview and a shipped GameSceneView are two different runtimes. The preview is a play-test simulator built into the editor; a shipped game is your companion class.
- **Your rules — winning, power-ups, and death:** The arcade behavior already runs Duke's movement, the coffee pickups and the exception monster, so your onUpdate only has to add what the engine can't know: the win condition. Reaching the flag wins.
- **Step 0 — Create the project and scaffold a scene:** The builder attaches to a Java 17 Codename One project. Creating that project and installing the toolchain are covered in the getting-started guide — generate one from the Codename One initializr (pick Java 17) and you're ready. Then scaffold a scene and open the editor.

## Referenced evidence

- https://start.codenameone.com

## Independent problem evidence

- Godot: Using TileMaps: https://docs.godotengine.org/en/stable/tutorials/2d/using_tilemaps.html — Godot's TileMap guidance treats tile placement, collision, navigation, and painting as visual level-authoring data rather than scattered source coordinates.
- Tiled Documentation: https://doc.mapeditor.org/en/stable/ — Tiled's documentation stores maps, layers, objects, and custom properties as map data that game code can load and interpret.

## Product proof

- `docs/website/static/blog/gamebuilder/platformer-6-scene.png`
- `docs/website/static/blog/gamebuilder/platformer-7-play.png`
- `docs/website/static/blog/gamebuilder/platformer-5-enemy-goal.png`
