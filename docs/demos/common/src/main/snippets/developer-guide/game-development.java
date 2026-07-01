// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::game-development-java-001[]
class MyGame extends GameView {
    final Sprite player = new Sprite(playerImage);

    MyGame() {
        getScene().add(player);
        player.setPosition(160, 240);
    }

    protected void update(double dt) {
        if (getInput().isGameKeyDown(Display.GAME_RIGHT)) {
            player.setX(player.getX() + 200 * dt);   // 200 px/second
        }
    }
}

Form f = new Form("Game", new BorderLayout());
MyGame game = new MyGame();
f.add(BorderLayout.CENTER, game);
f.show();
game.start();
// end::game-development-java-001[]

// tag::game-development-java-002[]
game.start();   // begin the loop
game.pause();   // freeze game logic, keep rendering
game.resume();  // continue
game.stop();    // end the loop
// end::game-development-java-002[]

// tag::game-development-java-003[]
game.setFixedTimestep(1.0 / 120.0);   // step physics at a steady 120Hz
// end::game-development-java-003[]

// tag::game-development-java-004[]
GameInput in = getInput();
if (in.isGameKeyDown(Display.GAME_FIRE)) { fire(); }
if (in.wasPointerPressed()) { spawnAt(in.getPointerX(), in.getPointerY()); }
// end::game-development-java-004[]

// tag::game-development-java-005[]
TouchControls c = getControls();

// analog stick, bottom-left, 80px radius, 24px in from the safe-area edges
c.addJoystick(80, TouchControls.LEFT, TouchControls.BOTTOM, 24);

// a JUMP button, bottom-right, mapped to GAME_FIRE
int jumpKey = Display.getInstance().getKeyCode(Display.GAME_FIRE);
c.addButton(jumpKey, 55, TouchControls.RIGHT, TouchControls.BOTTOM, 30)
        .setLabel("Jump").setColor(0xc0ff7043);

// then read input exactly as you would for a keyboard:
float ax = getInput().getAxisX();                 // analog steering
boolean jump = getInput().wasKeyPressed(jumpKey);  // or GAME_UP, etc.
// end::game-development-java-005[]

// tag::game-development-java-006[]
Sprite ship = new Sprite(shipImage);
ship.setPosition(160, 240);
ship.setRotation(45);          // degrees, clockwise
ship.setScale(2f);
ship.setAlpha(200);            // 0 (transparent) to 255 (opaque)
getScene().add(ship);
// end::game-development-java-006[]

// tag::game-development-java-007[]
SpriteRenderer r = new SpriteRenderer();
r.getScene().add(mySprite);
RenderView view = new RenderView(r).setContinuous(true);
form.add(BorderLayout.CENTER, view);
// end::game-development-java-007[]

// tag::game-development-java-008[]
SpriteSheet sheet = new SpriteSheet(explosionImage, 64, 64);
AnimatedSprite boom = new AnimatedSprite(
        sheet, new int[]{0, 1, 2, 3, 4, 5}, 0.05); // 50ms per frame
boom.setLooping(false);
getScene().add(boom);
// end::game-development-java-008[]

// tag::game-development-java-009[]
AnimatedSprite hero = new AnimatedSprite(runFrames, 0.09);   // Image[6], 90ms each
hero.play();
getScene().add(hero);

// in update(double dt):
if (moving) {
    hero.play();
    hero.setScale(facing, 1);     // facing is -1 (left) or 1 (right)
} else {
    hero.pause();
    hero.setCurrentFrame(0);      // standing pose
}
// end::game-development-java-009[]

// tag::game-development-java-010[]
// in the card's per-frame animation; flip runs 0 -> 1 over ~0.4s
flip += dt * 5;
float sx = Math.abs(1f - 2f * (float) flip);   // 1 -> 0 -> 1
if (flip >= 0.5 && !showingFace) {
    showingFace = true;
    sprite.setImage(face);     // swap sides while the card is edge-on
}
sprite.setScale(Math.max(0.05f, sx), 1f);
// end::game-development-java-010[]

// tag::game-development-java-011[]
protected void update(double dt) {
    // ... advance flip animations, count down the mismatch timer ...
    if (getInput().wasPointerPressed()) {
        int px = getInput().getPointerX();
        int py = getInput().getPointerY();
        for (int i = 0; i < cards.length; i++) {
            Rectangle b = cards[i].sprite.getBounds();
            if (b.contains(px, py)) {
                flipUp(cards[i]);
                break;
            }
        }
    }
}
// end::game-development-java-011[]

// tag::game-development-java-012[]
private float tileCenterX(int r, int c) {
    return originX + (c - r) * (tileW / 2f);
}

private float tileCenterY(int r, int c) {
    return originY + (c + r) * (tileH / 2f);   // tileH = tileW / 2
}
// end::game-development-java-012[]

// tag::game-development-java-013[]
private int[] pick(int px, int py) {
    float a = (px - originX) / (tileW / 2f);   // = c - r
    float b = (py - originY) / (tileH / 2f);   // = c + r
    int c = Math.round((a + b) / 2f);
    int r = Math.round((b - a) / 2f);
    if (r < 0 || r >= N || c < 0 || c >= N) {
        return null;                            // tap missed the board
    }
    return new int[]{r, c};
}
// end::game-development-java-013[]

// tag::game-development-java-014[]
// shadow sits flat on the tile, the piece floats above it
addDynamic(shadow, cx, cy + tileH * 0.10f, (r + c) * 4 + 1, 0.5, 0.5);
addDynamic(piece,  cx, cy - tileH * 0.30f, (r + c) * 4 + 3, 0.5, 0.62);
// end::game-development-java-014[]

// tag::game-development-java-015[]
getCamera()
    .setPerspective(60, 0.1f, 500f)   // vertical FOV, near, far
    .setPosition(0, 6, 12)            // eye
    .setTarget(0, 0, 0);             // look-at

Sprite tree = new Sprite(treeImage);
tree.setPosition(0, 1, 0);            // world coordinates, y up
tree.setSize(2, 4);                   // size is now in world units, not pixels
getScene().add(tree);
// end::game-development-java-015[]

// tag::game-development-java-016[]
protected void onSetup(GraphicsDevice device) {
    Mesh cubeMesh = Primitives.cube(device, 1f);
    Material gold = new Material(Material.Type.PHONG).setColor(0xffffcc33).setShininess(32);
    crate = new Model(cubeMesh, gold).setPosition(0, 0.5f, 0);
    addModel(crate);

    getLight().setDirection(-1, -1, -0.5f);   // shade the lit material
}

protected void update(double dt) {
    crate.setRotation(0, crate.getRotationY() + (float) (60 * dt), 0);  // spin
}
// end::game-development-java-016[]

// tag::game-development-java-017[]
Mesh ground = Primitives.quad(device, 64f);
Texture grid = device.createTexture(makeGridImage());      // any Image you draw
Material mat = new Material(Material.Type.LAMBERT)
        .setColor(0xff3f7d4f).setTexture(grid);
Model floor = new Model(ground, mat).setRotation(-90, 0, 0); // lay the quad flat
addModel(floor);

// one cube mesh, scaled into buildings of varying height:
Model tower = new Model(cubeMesh, brick).setScale(1.5f, 4f, 1.5f).setPosition(9, 2, 0);
addModel(tower);
// end::game-development-java-017[]

// tag::game-development-java-018[]
SoundPool sfx = SoundPool.create(12);          // up to 12 simultaneous voices
SoundEffect coin = sfx.load("/coin.wav");
// ... in the game loop:
coin.play();                                   // fire and forget
int voice = coin.play(0.8f, -0.3f, 1.2f, 0);   // volume, pan, rate/pitch, loop
sfx.setVolume(voice, 0.5f);                     // adjust a playing voice
// end::game-development-java-018[]

// tag::game-development-java-019[]
if (sfx.isVoiceCompletionSupported()) {
    sfx.setVoiceListener(new VoiceListener() {
        public void onComplete(int voiceId) {
            onEffectFinished(voiceId);
        }
    });
}
// end::game-development-java-019[]

// tag::game-development-java-020[]
PhysicsWorld world = new PhysicsWorld(0, 900);   // gravity 900 px/s^2 downward

// a STATIC floor that never moves
world.createBox(160, 460, 320, 40, BodyType.STATIC);

// a DYNAMIC crate affected by gravity and collisions
PhysicsBody crate = world.createBox(160, 0, 32, 32, BodyType.DYNAMIC);
crate.setRestitution(0.4f);   // bounciness
crate.setFriction(0.5f);

// in update(double dt):
world.step((float) dt);
// end::game-development-java-020[]

// tag::game-development-java-021[]
GeneralPath ramp = new GeneralPath();
ramp.moveTo(0, 0);
ramp.lineTo(240, 0);
ramp.lineTo(240, 80);
ramp.closePath();
world.createShape(40, 380, ramp, BodyType.STATIC);   // collide with exactly what you draw
// end::game-development-java-021[]

// tag::game-development-java-022[]
Sprite crateSprite = new Sprite(crateImage);
crate.setLinkedSprite(crateSprite);
getScene().add(crateSprite);

// in update(double dt):
world.step((float) dt);   // moves crate, which moves crateSprite, which the scene draws
// end::game-development-java-022[]

// tag::game-development-java-023[]
world.addContactListener(new ContactListener() {
    public void beginContact(PhysicsContact c) {
        Object a = c.getSpriteA();   // the linked sprites, if any
        Object b = c.getSpriteB();
        // e.g. flag the bodies for removal after this step
    }
    public void endContact(PhysicsContact c) { }
});
// end::game-development-java-023[]

// tag::game-development-java-024[]
// a hinge the two bodies pivot around (anchor in pixels)
world.createRevoluteJoint(bodyA, bodyB, pivotXpx, pivotYpx);
// a fixed-length link, like a rod between two crates
world.createDistanceJoint(bodyA, bodyB, ax, ay, bx, by, 0f, 0f);
// drag a body toward the finger -- great for "pick up and throw"
PhysicsJoint drag = world.createMouseJoint(ground, body, px, py, 1000f);
// ... while the finger moves:
drag.setTarget(getInput().getPointerX(), getInput().getPointerY());
// ... on release:
drag.destroy();
// end::game-development-java-024[]
