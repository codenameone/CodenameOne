# Game Development Reference

`com.codename1.gaming` is a game-oriented surface that fits how games are written: a tight update loop, sprite primitives, pollable input, low-latency sound, and optional 2D rigid-body physics. Rendering is GPU-driven (it sits on top of `com.codename1.gpu` — see `references/3d-graphics.md`), so **there is no `paint()` to implement and no frame rate to manage**. You move objects in `update(deltaSeconds)`; the engine draws them.

Use this for arcade / casual / scroller / board games and any real-time animated canvas. For static UI, use normal CN1 components — not this.

## The shape of a game

Subclass `GameView`, populate the scene, advance state in `update`, add the view to a `Form`, and `start()`:

```java
import com.codename1.gaming.*;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BorderLayout;

class MyGame extends GameView {
    private final Sprite player = new Sprite(playerImage);

    MyGame() {
        getScene().add(player);
        player.setPosition(160, 240);
    }

    @Override
    protected void update(double dt) {           // dt = seconds since last frame
        GameInput in = getInput();
        if (in.isGameKeyDown(Display.GAME_RIGHT)) player.setX(player.getX() + 200 * dt);
        if (in.isGameKeyDown(Display.GAME_LEFT))  player.setX(player.getX() - 200 * dt);
    }
}

Form f = new Form("Game", new BorderLayout());
MyGame game = new MyGame();
f.add(BorderLayout.CENTER, game);
f.show();
game.start();          // begins the loop; stop()/pause()/resume() control it
```

Multiply movement by `dt` so speed is frame-rate independent. For deterministic physics use a fixed step: `setFixedTimestep(1.0/60)` (then `getInterpolationAlpha()` gives a 0..1 blend for smooth rendering between steps).

## GameView — the loop and the world

| Method | Purpose |
| --- | --- |
| `protected abstract void update(double dt)` | Override. Advance game state; `dt` is seconds (or the fixed step). |
| `Scene getScene()` | The z-ordered sprite collection to `add`/`remove`/`clear`. |
| `GameInput getInput()` | Pollable keyboard / pointer / analog input. |
| `TouchControls getControls()` | On-screen joystick + buttons for touch devices. |
| `GameCamera getCamera()` | 2D ortho (default) or 3D perspective view. |
| `Light getLight()` | Directional light for lit 3D models. |
| `void addModel(Model)` / `removeModel(Model)` | Add/remove a 3D mesh (perspective rendering). |
| `void setClearColor(int argb)` | Background clear color. |
| `void start()` / `stop()` / `pause()` / `resume()` | Loop lifecycle. `isRunning()` / `isPaused()` query it. |
| `void setFixedTimestep(double s)` | Switch to a deterministic fixed step (0 = variable, the default). |
| `protected void onSetup(GraphicsDevice device)` | Override to allocate GPU resources once, before the first frame. |

`GameView` *is* a CN1 `Component`, so it lives inside a normal `Form`/layout. Call `stop()` in your form's cleanup so the loop doesn't run in the background.

## GameInput — poll, don't listen

All state is read on the EDT inside `update`. Levels vs. edges:

- `isKeyDown(int keyCode)` / `isGameKeyDown(int gameAction)` — held this frame. Game actions: `Display.GAME_UP/DOWN/LEFT/RIGHT/FIRE`.
- `wasKeyPressed(int)` / `wasKeyReleased(int)` — true only on the single transition frame (cleared after `update`).
- `getPointerX()` / `getPointerY()` — pointer position relative to the view.
- `isPointerDown()` / `wasPointerPressed()` / `wasPointerReleased()` — pointer level + edges.
- `getAxisX()` / `getAxisY()` — analog `-1..1` from the virtual joystick (x right, y down).

## Sprites

`Sprite` is a lightweight holder: image + position + rotation + scale + tint + normalized anchor. The renderer turns each into a GPU quad per frame.

```java
Sprite s = new Sprite(image);
s.setPosition(x, y);          // anchor point; default anchor is center (0.5, 0.5)
s.setRotation(45f);           // degrees, clockwise
s.setScale(2f);               // or setScale(sx, sy)
s.setColor(0xffff8800);       // ARGB tint; opaque white = no tint
s.setAlpha(180);              // 0..255
s.setZOrder(10);              // higher draws on top
boolean hit = s.intersects(other);   // AABB overlap test
```

`AnimatedSprite` cycles frames over time:

```java
SpriteSheet sheet = new SpriteSheet(atlasImage, 32, 32);          // grid of 32x32 frames
AnimatedSprite hero = new AnimatedSprite(sheet, new int[]{0,1,2,3}, 0.12);  // 0.12s/frame
hero.setLooping(true);
hero.setPlaying(true);
getScene().add(hero);
```

`SpriteSheet` slices a texture atlas: `getFrame(index)` or `getFrame(col, row)`, plus `getColumns()/getRows()/getFrameCount()`. Frames are cut and cached on first access.

**Don't add or remove sprites during `Scene.update()`** (i.e. mid-frame) — mutate the scene from your `update(dt)` body, which runs before the scene advances. Use `Scene.setCameraX/Y` to scroll a 2D world.

## Touch controls

Wire on-screen controls once; they feed the same `GameInput` so touch and keyboard code paths are identical:

```java
TouchControls c = getControls();
c.addJoystick(88, TouchControls.LEFT, TouchControls.BOTTOM, 30);  // read via getAxisX/Y + GAME_* keys
VirtualButton fire = c.addButton(Display.GAME_FIRE,
        TouchControls.RIGHT, TouchControls.BOTTOM, 26);
fire.setLabel("A");      // while pressed, isKeyDown(GAME_FIRE) is true
```

A `VirtualJoystick` reads as both analog (`getAxisX/Y`) and digital (`isGameKeyDown` past its dead zone, default 0.2). A `VirtualButton` holds its mapped key code down while touched.

## Low-latency sound

`MediaManager` is fine for music; for overlapping short effects use `SoundPool` (native low-latency backend where available, MediaManager fallback otherwise):

```java
SoundPool pool = SoundPool.create(8);              // up to 8 simultaneous voices
SoundEffect blip = pool.load("/blip.wav");
int voice = pool.play(blip);                       // or play(fx, volume, pan, rate, loop)
// pool.stop(voice); blip.unload();
```

`pool.isNativeAccelerated()` tells you whether pan/rate are honored (they are ignored on the fallback path, but playback still works everywhere).

## 2D physics (`com.codename1.gaming.physics`)

An idiomatic wrapper over a Box2D simulation (derived from JBox2D, pure Java, runs on every platform). **Everything is in screen pixels, y-down** — the meter/y-up conversion is internal. Link a body to a sprite and the body drives the sprite each step.

```java
import com.codename1.gaming.physics.*;

PhysicsWorld world = new PhysicsWorld(0, 600);                 // gravity px/s^2, down
PhysicsBody ground = world.createBox(0, 460, 320, 40, BodyType.STATIC);
PhysicsBody crate  = world.createBox(160, 0, 32, 32, BodyType.DYNAMIC);
crate.setLinkedSprite(crateSprite);                           // body pushes its transform into the sprite

world.addContactListener(new ContactListener() {
    public void beginContact(PhysicsContact c) { /* landed */ }
    public void endContact(PhysicsContact c) { }
});

// in update(dt):
world.step((float) dt);     // integrates and syncs every linked sprite
```

- `BodyType`: `STATIC` (walls/ground), `KINEMATIC` (app-moved platforms), `DYNAMIC` (fully simulated).
- Body factories: `createBox`, `createCircle`, `createPolygon`, `createShape(Shape)`. Joints: `createRevoluteJoint`, `createDistanceJoint`, `createWeldJoint`, `createPrismaticJoint`, `createMouseJoint`.
- `PhysicsBody`: `applyForce`, `setLinearVelocity`, `setAngularVelocity`, `setDensity/setFriction/setRestitution`, `setTransform`.
- Debugging: `world.setDebugDrawFlags(...)` + `world.debugDraw(g)`.
- **Don't create/destroy bodies inside a contact callback** — defer until after `step()` returns.

## 3D in a game

`GameView` can render 3D meshes through the camera: switch `getCamera().setMode(GameCamera.MODE_PERSPECTIVE)`, set it up with `setPerspective/setPosition/setTarget`, build a `Model` from a `Mesh` (see `references/3d-graphics.md` for `Primitives`, `GltfLoader`, materials), and `addModel(model)`. For raw 3D without the game loop, use `com.codename1.gpu` directly.

## Platform support

The gaming API is part of `codenameone-core` — no extra dependency, no build hint. Rendering uses the platform GPU backend (OpenGL ES on Android, Metal on iOS, WebGL on web, OpenGL/software on the simulator) and degrades gracefully where 3D is unavailable. Box2D physics is pure Java and identical everywhere.

## What NOT to do

- Don't implement `paint()` / drive your own frame loop — override `update(dt)` and let `GameView` render.
- Don't move objects by a fixed pixel count per frame — multiply by `dt` (or use a fixed timestep) so speed is consistent across devices.
- Don't mutate the scene or physics-body set mid-step; do it from `update(dt)` (scene) or after `world.step()` returns (bodies).
- Don't reach into `com.codename1.gaming.physics.box2d.*` directly — use the `PhysicsWorld`/`PhysicsBody` wrapper (the box2d package is the internal engine).
- Don't forget to `stop()` the `GameView` when leaving the screen.
