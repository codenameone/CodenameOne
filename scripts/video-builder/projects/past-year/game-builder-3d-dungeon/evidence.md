# Evidence map

Source: `docs/website/content/blog/game-builder-3d-dungeon.md`
Canonical: https://www.codenameone.com/blog/game-builder-3d-dungeon/

## Thesis

Building a first-person Java dungeon from visual scene data and a companion class

## Supported beats

- **Why 3D mode (and what "play style" means):** 3D games come in families with very different cameras and controls: a flight sim has no ground, a racer follows a road, a dungeon crawler walks first-person between walls.
- **Step 1 — A 3D scene:** Pick New scene → 3D Map. Placement defaults to an accurate top-down grid (toggle View → 3D: Perspective / Top-down for an angled overview). The 3D Kit pack supplies blocks, pillars, crates, a spawn and scenery.
- **3D assets are meshes:** This is the big difference from Tutorials 1 and 2: in 2D an asset's art is an image or a sprite sheet, but in 3D it's a mesh — geometry, not pixels.
- **Step 2 — Pick the dungeon play style:** With nothing selected, set the Inspector's 3D play style to dungeon. That switches the preview to a first-person walker with wall collision — the genre we're building.
- **Step 3 — Build the maze walls:** A maze wants continuous walls, not a row of separate posts — so build them with the Terrain tool's Wall brush rather than placing individual pillar objects.
- **Step 4 — Add the player spawn:** Place a Spawn where the player starts and tick This is the player in its Behavior section — that marks which element the camera and controls drive. Its Elevation (Z) field raises it for multi-level layouts.

## Independent problem evidence

- Introduction to 3D: https://docs.godotengine.org/en/stable/tutorials/3d/introduction_to_3d.html — Mature game engines organize three-dimensional work around scenes, cameras, lights, meshes, and physics.
- glTF 2.0 Specification: https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html — glTF defines a runtime-neutral structure for scenes, nodes, meshes, materials, cameras, and animation.

## Product proof

- `docs/website/static/blog/gamebuilder/dungeon-5-walk.png`
- `docs/website/static/blog/gamebuilder/dungeon-6-combat.png`
- `docs/website/static/blog/gamebuilder/dungeon-3-walls.png`
- `docs/website/static/blog/gamebuilder/dungeon-4-spawn.png`
- `docs/website/static/blog/gamebuilder/dungeon-1-new-scene.png`
