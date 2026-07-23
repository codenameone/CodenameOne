# Evidence map

Source: `docs/website/content/blog/game-development-api-box2d.md`
Canonical: https://www.codenameone.com/blog/game-development-api-box2d/

## Thesis

A portable game loop, sprites, Box2D physics, and low-latency sound in Java

## Supported beats

- **The sixty-second tour:** You subclass GameView, implement update(double dt), and you have a game. Here is the complete logic of a demo where balls drop, fall under gravity, and bounce off the floor and walls.
- **The game loop:** GameView drives its loop through the existing animation system, so it cooperates with the rest of the UI instead of fighting it.
- **Sprites and scenes:** Sprite carries position, anchor, rotation, scale, and alpha, plus an axis-aligned bounding box for simple overlap tests. SpriteSheet slices a packed texture into cached frames, AnimatedSprite plays them back, and Scene holds everything in z-order with a camera, so scrolling a level means moving the camera and not repositioning the world.
- **Physics: Box2D in the core:** We did not write a physics engine, and that is the point: we took the industry standard. Box2D is Erin Catto's rigid-body engine, the one behind an entire generation of 2D hits, and JBox2D is its faithful Java port.
- **Sound effects that keep up:** Music and sound effects have opposite requirements: music wants streaming, effects want zero latency and overlapping playback. MediaManager always covered the former; the new SoundPool covers the latter.
- **What can you build?:** The repository ships six game samples, each a complete, playable game in a few hundred lines, and each generating every pixel of its art and every sound at runtime, so there are no assets to manage.

## Referenced evidence

- https://en.wikipedia.org/wiki/Jane%27s_USAF
- https://github.com/codenameone/CodenameOne/pull/5166
- https://github.com/codenameone/CodenameOne/issues/5215
- https://box2d.org/
- https://github.com/jbox2d/jbox2d
- https://github.com/codenameone/CodenameOne/tree/master/Samples/samples/CasualGameSample
- https://github.com/codenameone/CodenameOne/tree/master/Samples/samples/ScrollerGameSample
- https://github.com/codenameone/CodenameOne/tree/master/Samples/samples/CardGameSample
- https://github.com/codenameone/CodenameOne/tree/master/Samples/samples/BoardGameSample
- https://github.com/codenameone/CodenameOne/tree/master/Samples/samples/Gaming3DDemoSample
- https://github.com/codenameone/CodenameOne/tree/master/Samples/samples/GamingDemoSample
- https://github.com/codenameone/CodenameOne/issues
