# Evidence map

Source: `docs/website/content/blog/game-builder-board-game.md`
Canonical: https://www.codenameone.com/blog/game-builder-board-game/

## Thesis

Building blackjack as data-driven cards plus explicit Java rules

## Supported beats

- **Why board mode for cards?:** Board mode is the Game Builder's grid mode: you place elements on a flat board of cells instead of a free-scrolling world.
- **Step 1 — A card-table scene:** Pick New scene → Board. You get a Board (tile) layer for the table surface and a Pieces (entity) layer for the cards.
- **Step 2 — Lay the felt:** Select the Board layer, pick the green tile from the palette, and paint the whole grid. That green surface is your table; everything else sits on top of it.
- **Step 3 — Deal the hands:** Switch to the Pieces layer and place Card actors: a row for the dealer along the top and a row for Duke along the bottom.
- **Step 4 — Hit or stand:** Duke's hand here is on 15 (and his Jack already wears the Duke mascot) — too low to stand on. He hits, and a third card brings him to 19, a total worth standing on.
- **Where the card art comes from:** Let's be exact about this, because "the faces are generated" explains nothing. There is no card generator in the framework. Your companion class draws each card itself with the ordinary Graphics API — the same one you'd use to custom-paint any Codename One component.

## Referenced evidence

- https://wiki.openjdk.org/display/duke/Main

## Independent problem evidence

- React State as a Snapshot: https://react.dev/learn/state-as-a-snapshot — React's state model describes rendering as a snapshot derived from state, the same separation a rules-driven board game needs.
- Java 2D API: https://docs.oracle.com/javase/tutorial/2d/ — Java's two-dimensional graphics model separates shapes, text, images, transforms, and compositing from higher-level widgets.

## Product proof

- `docs/website/static/blog/gamebuilder/board-1-new-scene.png`
- `docs/website/static/blog/gamebuilder/board-4-hit.png`
- `docs/website/static/blog/gamebuilder/board-5-play.png`
