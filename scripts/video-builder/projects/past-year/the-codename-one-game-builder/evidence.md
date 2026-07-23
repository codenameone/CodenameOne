# Evidence map

Source: `docs/website/content/blog/the-codename-one-game-builder.md`
Canonical: https://www.codenameone.com/blog/the-codename-one-game-builder/

## Thesis

Why game levels should be visual data while Java owns the rules

## Supported beats

- **Draw the level, code only the rules:** The core idea is that a level is data, not code. You draw it in the editor, tag each object with the numbers your game needs (lives, value, speed), and the editor saves a small .game file.
- **One editor, three kinds of game:** The same editor and the same .game format author three modes, and the runtime realizes each one without your code changing shape.
- **From .game file to running game:** The data model lives in com.codename1.gaming.level: GameLevel, GameElement, Layer, TileLayer, AssetCatalog, and IsoProjection, with JSON load and save across all three modes.
- **Large worlds that stream:** Some games outgrow a single hand-drawn level. The Game Builder ships a streaming engine for large and open worlds, and the editor has a Large World mode that edits and previews the active region.
- **A note on beta:** The Game Builder and the high-level gaming APIs are beta. They work, the tutorials are real games you can build start to finish, and they run on every platform.
- **Where to go next:** Start with Thursday's platformer; it introduces every moving part you will reuse in the other two.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5253
- https://github.com/codenameone/CodenameOne/issues

## Independent problem evidence

- Godot: Nodes and Scenes: https://docs.godotengine.org/en/stable/getting_started/step_by_step/nodes_and_scenes.html — Godot's scene system composes nodes into reusable trees so spatial structure can be authored and instantiated separately from gameplay scripts.
- Tiled JSON Map Format: https://doc.mapeditor.org/en/stable/reference/json-map-format/ — Tiled exports visual maps with layers, object data, tile sets, and custom properties that runtimes can load independently of editor code.

## Product proof

- `docs/website/static/blog/gamebuilder/platformer-6-scene.png`
- `docs/website/static/blog/gamebuilder/board-5-play.png`
- `docs/website/static/blog/gamebuilder/dungeon-5-walk.png`
