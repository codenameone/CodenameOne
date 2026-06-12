---
title: "Build Games In Java: Sprites, Box2D Physics And Low-Latency Sound"
slug: game-development-api-box2d
url: /blog/game-development-api-box2d/
date: '2026-06-14'
author: Shai Almog
description: The new com.codename1.gaming package adds a game loop, sprites, pollable input, a low-latency sound pool, and rigid-body physics powered by a bundled Box2D engine, all running unchanged on every platform including iOS.
feed_html: '<img src="https://www.codenameone.com/blog/game-development-api-box2d.jpg" alt="Build Games In Java: Sprites, Box2D Physics And Low-Latency Sound" /> The new com.codename1.gaming package adds a game loop, sprites, pollable input, a low-latency sound pool, and rigid-body physics powered by a bundled Box2D engine, all running unchanged on every platform including iOS.'
---

![Build Games In Java: Sprites, Box2D Physics And Low-Latency Sound](/blog/game-development-api-box2d.jpg)

Games have always been possible in Codename One; the `Graphics` pipeline and the animation system are fast enough. What was missing is the vocabulary: a game loop, sprites, pollable input, sound effects that fire without latency, and physics. [PR #5166](https://github.com/codenameone/CodenameOne/pull/5166) adds all of it as `com.codename1.gaming`, a game-oriented surface built on top of [yesterday's portable 3D API](/blog/portable-3d-graphics-api/) and the existing media and animation systems, rather than replacing any of them.

Judging by [the reactions in the issue tracker](https://github.com/codenameone/CodenameOne/issues/5215), some of you have been waiting for this one.

## The sixty-second tour

You subclass `GameView`, implement `update(double dt)`, and you have a game. Here is the complete logic of a demo where balls drop, fall under gravity, and bounce off the floor and walls:

```java
static class PhysicsDemoView extends GameView {
    private PhysicsWorld world;
    private final Image ballImage = makeBall(60, 0xffff5a5f);

    PhysicsDemoView() {
        setClearColor(0xff101826);
    }

    private void setupWorld() {
        int w = getWidth(), h = getHeight();
        world = new PhysicsWorld(0, 900); // gravity in pixels/s^2, downward
        world.createBox(w / 2f, h - 10, w, 20, BodyType.STATIC);   // floor
        world.createBox(-10, h / 2f, 20, h * 2f, BodyType.STATIC); // walls
        world.createBox(w + 10, h / 2f, 20, h * 2f, BodyType.STATIC);
    }

    private void dropBall(float x, float y) {
        PhysicsBody body = world.createCircle(x, y, 30, BodyType.DYNAMIC);
        body.setRestitution(0.7f);
        Sprite s = new Sprite(ballImage);
        s.setPosition(x, y);
        body.setLinkedSprite(s);
        getScene().add(s);
    }

    protected void update(double dt) {
        if (getInput().wasPointerPressed()) {
            dropBall(getInput().getPointerX(), getInput().getPointerY());
        }
        world.step((float) dt);
    }
}
```

Notice what's absent: there is no render code. The `Scene` draws its sprites, and each sprite tracks the physics body it is linked to. This is the demo running in the simulator:

![Box2D bodies driving sprites in a GameView](/blog/game-development-api-box2d/gaming-demo.gif)

The view drops into a normal form like any component:

```java
Form f = new Form("Gaming Demo", new BorderLayout());
PhysicsDemoView game = new PhysicsDemoView();
f.add(BorderLayout.CENTER, game);
f.show();
game.start();
```

## The game loop

`GameView` drives its loop through the existing animation system, so it cooperates with the rest of the UI instead of fighting it. It manages the frame rate while running and restores the global value when it stops, and it releases everything automatically when the view is detached from the form.

For deterministic simulation there is a fixed-timestep mode: set `setFixedTimestep(1.0 / 60)` and `update` is called with exactly that delta as often as needed, with `getInterpolationAlpha()` available for smooth rendering between simulation steps. This is the standard pattern for physics-driven games, and it is one method call here.

Input is pollable, the way game code wants it. `getInput()` exposes level state (`isKeyDown`, `isPointerDown`, `getPointerX/Y`) and per-frame edges (`wasKeyPressed`, `wasPointerPressed`), aware of game actions (fire, directional pad) across platforms. No listeners, no event-versus-frame mismatch: you ask, every frame, and act.

## Sprites and scenes

`Sprite` carries position, anchor, rotation, scale, and alpha, all applied through the affine transform pipeline, plus an axis-aligned bounding box for simple overlap tests. `SpriteSheet` slices a packed texture into cached frames, `AnimatedSprite` plays them back, and `Scene` holds everything in z-order with a camera, so scrolling a level means moving the camera and not repositioning the world.

And because `GameView` extends the 3D `RenderView`, the same surface scales up to real 3D: a `GameCamera` in perspective mode, `Model` instances for meshes, and billboarded sprites that always face the camera. The `Gaming3DDemoSample` in the repository orbits a camera around a lit 3D scene with floating billboard coins, with touch controls, in about two hundred lines.

## Physics: Box2D in the core

The physics engine is the part we are happiest with. It is JBox2D, the de-facto standard 2D rigid-body engine, shaded into `com.codename1.gaming.physics.box2d` so it ships inside the core with no dependency to add (BSD license retained and attributed). On top of it sits an idiomatic wrapper, `PhysicsWorld` / `PhysicsBody` / `ContactListener`, that makes the two classic Box2D paper cuts disappear: the pixels-to-meters conversion and the flipped y-axis are centralized, so your game code stays entirely in screen coordinates.

Bodies are created from boxes, circles, polygons, or any Codename One `Shape`. Collisions arrive through `ContactListener`. And `setLinkedSprite` closes the loop: after `world.step()`, every linked sprite has already moved to where its body is.

Pure Java matters here. Because the engine is plain bytecode, it runs unchanged through ParparVM on iOS, through the new Windows port, on Android, and on the web. There is no native physics library to bind, version, or debug per platform.

## Sound effects that keep up

Music and sound effects have opposite requirements: music wants streaming, effects want zero latency and overlapping playback. `MediaManager` always covered the former; the new `SoundPool` covers the latter:

```java
SoundPool sfx = SoundPool.create(12); // up to 12 overlapping voices
SoundEffect blip = sfx.load(stream, "audio/wav");
// later, per bounce, with per-drop pitch:
sfx.play(blip, 0.9f, 0f, rate, 0);   // volume, pan, rate, loop
```

Each platform backs this with its native low-latency path: `android.media.SoundPool` on Android, an `AVAudioPlayer` pool on iOS, and a software mixer with volume, pan, and rate control on the desktop. Where no native backend exists, a pure cross-platform fallback over `MediaManager` keeps the API functional, and `isNativeAccelerated()` tells you which path you got.

## Where to go from here

There is a new "Game Development" chapter in the developer guide covering the loop, sprites, physics, and audio in detail, and the `GamingDemoSample` and `Gaming3DDemoSample` in the repository tie everything together; the 2D sample generates its sprite images and sound at runtime, so it needs no assets at all.

If you build something with this, we genuinely want to see it, and if you hit a wall, [file an issue](https://github.com/codenameone/CodenameOne/issues) with the smallest game that reproduces it.

Yesterday's post covered [the portable 3D API](/blog/portable-3d-graphics-api/) this is built on, and the [release post](/blog/native-java-win32-3d-gaming-printing-and-wallet/) has the full index. Tomorrow's post turns your Java into a native Windows executable with no JVM.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
