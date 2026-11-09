/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.splat.play;


import com.codename1.gaming.AnimatedModel;
import com.codename1.gaming.GameCamera;
import com.codename1.gaming.GameView;
import com.codename1.gaming.Model;
import com.codename1.gaming.ParticleEmitter;
import com.codename1.gaming.TouchControls;
import com.codename1.gaming.VirtualButton;
import com.codename1.gaming.VirtualJoystick;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PostProcess;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Texture;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.util.MathUtil;
import com.codenameone.examples.splat.rules.Arena;
import com.codenameone.examples.splat.rules.Ball;
import com.codenameone.examples.splat.rules.Box;
import com.codenameone.examples.splat.rules.Event;
import com.codenameone.examples.splat.rules.Input;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.rules.Mode;
import com.codenameone.examples.splat.rules.Player;
import com.codenameone.examples.splat.rules.Trig;
import java.util.List;

/// The field in first person, drawn with `com.codename1.gaming`: every
/// operator is a skinned glTF character playing the clip that matches what the
/// rules say they are doing, every bunker is the prop it was sized after, and
/// the marker in your hands sways, kicks and comes up to the eye when you aim
/// down the sights. Paint stays on the field for the whole match.
///
/// The rules count in millimetres and binary angles; this class is the only
/// place they become floats. World x is drawn mirrored, so that a positive
/// strafe, which the rules move toward +x, goes right on the screen.
// tag::splat-arena-view[]
public class ArenaView extends GameView {
    /// What the view tells the HUD once per drawn frame and once per event.
    public interface Hud {
        void frame(Match match, int me, float lookYaw, float lookPitch);

        void event(Event e);
    }
    // end::splat-arena-view[]

    public static final double TICK = 1.0 / Match.TICK_HZ;
    /// The preference that flips vertical look, for pilots.
    public static final String INVERT_KEY = "splat.invertLook";
    /// Paint colours: what a team's balls leave on the field.
    public static final int[] TEAM_COLORS = {0xffff7a1f, 0xff1fc6ff};
    private static final int HAZE = 0xffb9c6cf;
    private static final float SHADOW_AHEAD = 12f;

    public static final String[] TEAM_NAMES = {"Task Force Tangerine", "Teal Directorate"};
    /// Field of view from the hip and down the sights.
    static final float HIP_FOV = 70f;
    static final float ADS_FOV = 46f;
    private static final int MAX_STEPS_PER_FRAME = 12;
    private static final int MAX_BALLS = 120;
    private static final int MAX_DECALS = 260;
    private static final float MM = 0.001f;
    /// The near plane. Further out than looks necessary: depth precision is
    /// spent on the first metre, and at 3 cm paint on the floor fought the
    /// floor for every pixel past twenty metres.
    private static final float NEAR = 0.07f;
    private static final float OPERATOR_HEIGHT = 1.8f;
    private static final float MARKER_LENGTH = 0.28f;
    /// A marker in another operator's hands, in metres. Real ones are about
    /// this long; at the viewmodel's size they vanished into a fist, and
    /// half the field looked unarmed.
    private static final float HELD_LENGTH = 0.55f;

    // Key codes the touch buttons press, read with the keyboard's.
    static final int KEY_FIRE = 'f';
    static final int KEY_JUMP = ' ';
    static final int KEY_CROUCH = 'c';
    static final int KEY_SPRINT = 'e';
    static final int KEY_RELOAD = 'r';
    static final int KEY_THROW = 'g';
    static final int KEY_WIPE = 'x';
    static final int KEY_AIM = 'q';
    static final int KEY_VIEW = 'v';

    private final Driver driver;
    private Hud hud;
    private GraphicsDevice device;
    private Mesh sphere;

    /// Twelve seconds of the match, one snapshot a tick, for the instant
    /// replay, which plays the hit back slowly while the live match runs on:
    /// it reaches a second before the hit until well after it.
    private static final int HISTORY = 360;
    private final int[][] history = new int[HISTORY][];
    private int recordedTick = -1;
    /// The replay plays in its own match, restored from the history, so the
    /// match in play goes on underneath it.
    private Match ghost;
    private int replayShooter = -1;
    private int replayVictim = -1;
    private int replayHit;
    /// The replay's shots, one row each: the angle, the first and last tick
    /// relative to the hit, and the speed it plays at. Over the shooter's
    /// shoulder at half speed, then side on at a quarter for the splash, then
    /// in front of the victim for the performance. Together they last a little
    /// under the time out (Match.RESPAWN), so the replay ends as you go back in.
    static final int ANGLE_SHOOTER = 0;
    static final int ANGLE_SIDE = 1;
    static final int ANGLE_VICTIM = 2;
    private static final float[][] SHOTS = {
        {ANGLE_SHOOTER, -30, 0, 0.5f},
        {ANGLE_SIDE, -9, 9, 0.25f},
        {ANGLE_VICTIM, 3, 27, 0.6f},
    };
    private int shot;
    /// When the current shot began, for the HUD's cut to it.
    private long shotAt;
    private boolean splashed;
    /// The replay camera's heading, in radians, eased toward the shooter's
    /// look; NaN until a replay's first frame sets it.
    private double chaseYaw = Double.NaN;
    private int replayTo;
    private float replayPos;
    private float replaySpeed = 1f;
    /// The match the last frame showed: the live one, or the replay's ghost.
    private Match drawn;
    private long replayStartAt;
    private boolean overheadChosen;
    private boolean viewHeld;
    private boolean overheadNow;
    private float overX;
    private float overZ;
    private boolean overSet;
    /// The jolt of a ball to the goggles: 1 on the hit, settling to 0.
    private float knock;
    private Material[][][] splatPaint;
    private Model sky;
    private boolean controlsHidden;
    private Mesh quad;
    private Material[] teamPaint;
    private AnimatedModel[] operators = new AnimatedModel[0];
    private AnimatedModel[] held = new AnimatedModel[0];
    private float[] opScale = new float[0];
    private float[] heldScale = new float[0];
    private String[] clips = new String[0];
    /// Per operator, smoothed so one jittery tick never changes the clip:
    /// ground speed forward and sideways in m/s, the yaw drawn, and how long
    /// a different clip has been asked for.
    private float[] moveF = new float[0];
    private float[] moveS = new float[0];
    private float[] bodyYaw = new float[0];
    /// Who was alive last frame, so a respawn can leave a puff behind.
    private boolean[] wasAlive = new boolean[0];
    private float[] pending = new float[0];
    /// The ground speed, in m/s, each locomotion clip was authored at; the
    /// clip plays faster or slower so the feet keep pace with the ground.
    private static final float RUN_PACE = 4.2f;
    private static final float WALK_PACE = 1.7f;
    private final Model[] balls = new Model[MAX_BALLS];
    private final Model[] decals = new Model[MAX_DECALS];
    private int nextDecal;
    // Effects. Every one is a pool of soft billboards drawn in one call.
    private final ParticleEmitter[] spray = new ParticleEmitter[2];
    private final ParticleEmitter[] mist = new ParticleEmitter[2];
    private final ParticleEmitter[] goo = new ParticleEmitter[2];
    private ParticleEmitter dust;
    private ParticleEmitter flash;
    private ParticleEmitter motes;
    private float moteClock;
    private int moteSeed = 0x2545f491;
    /// A capture-the-flag flag: a steel pole, a team-coloured cloth that
    /// waves, and a glowing ring on the ground at home so the base reads
    /// from across the yard. Two plain boxes, because no art pack has one.
    private final Model[] flagPole = new Model[2];
    private final Model[] flagCloth = new Model[2];
    private final Model[] flagBase = new Model[2];
    private float flagWave;
    private AnimatedModel viewModel;
    private float viewScale;
    private final float[] hand = new float[16];
    private float recoil;
    private float viewMuzzleX;
    private float viewMuzzleY;
    private float viewMuzzleZ;
    private float ads;
    private float bob;
    private float swayX;
    private float swayY;
    private int lastYaw;
    private int lastPitch;
    private double accumulator;
    // The quality governor: seconds of frames slower than SLOW_FRAME, and how
    // many steps down it has taken (0 everything, 1 no post, 2 no shadows).
    private static final double SLOW_FRAME = 1.0 / 20;
    private double slowFor;
    private int qualityDrops;
    private float alpha;
    private float[] prevX = new float[0];
    private float[] prevY = new float[0];
    private float[] prevZ = new float[0];
    private int lookYaw;
    private int lookPitch;
    private boolean aimed;
    private int lastLookX = -1;
    private int lastLookY = -1;
    private int tapFire;
    private com.codenameone.examples.splat.rules.Rng autopilot;
    private final boolean invert = com.codename1.io.Preferences.get(INVERT_KEY, false);
    private VirtualButton[] buttons = new VirtualButton[0];

    public ArenaView(Driver driver) {
        this.driver = driver;
    }

    public void setHud(Hud hud) {
        this.hud = hud;
    }

    /// Hands the player's thumbs to the rules' own bot, which is how the
    /// end-to-end tests and the promo video play a match; null gives them back.
    public void setAutopilot(com.codenameone.examples.splat.rules.Rng rng) {
        autopilot = rng;
    }

    private int heldButtons;

    /// Holds buttons down on top of whatever plays (the autopilot or your
    /// thumbs), as `Input` bits; 0 lets go. The walkthrough video uses it to
    /// show a control by name: aim, throw, reload, wipe.
    public void hold(int buttons) {
        heldButtons = buttons;
    }

    public Driver driver() {
        return driver;
    }

    /// How far down the sights you are: 0 from the hip, 1 fully aimed.
    public float aimAmount() {
        return ads;
    }

    @Override
    protected void onSetup(GraphicsDevice d) {
        device = d;
        Match match = driver.match();
        Arena arena = match.arena();
        // An overcast, hazy afternoon: the colour of every serious shooter.
        setClearColor(HAZE);
        // A low, warm sun through haze: long shadows, a cool sky fill and fog
        // that swallows the far side of the yard.
        getLight().setDirection(-0.55f, -0.62f, -0.3f).setColor(0xfffff0dc).setAmbientColor(0xff8a929c)
                .setShadows(true).setShadowMap(2048, 24f).setShadowStyle(0.72f, 0.0012f);
        // tag::splat-atmosphere[]
        getEnvironment().setFog(HAZE, 0.0075f, 0.05f).setHemisphere(0xffa9bdd2, 0xff4f5a3c)
                .setToneMapping(true).setExposure(1.3f)
                .setSky(0xff4f7fb8, HAZE, 0xfffff0d0, 0.45f);
        setPostProcess(new PostProcess().setBloom(0.78f, 0.55f)
                .setGrade(1.12f, 0.86f, 0xff0b1720, 0xfffff2dc).setVignette(0.42f).setGrain(0.035f));
        // end::splat-atmosphere[]
        getCamera().setPerspective(HIP_FOV, NEAR, 400f);
        sphere = Primitives.sphere(d, 0.5f, 10, 14, false);
        // the sky is a shader on the inside of a sphere that follows the eye
        sky = new Model(Primitives.sphere(d, 300f, 16, 24, true), new Material(Material.Type.SKYBOX));
        sky.getMaterial().getRenderState().setDepthWrite(false);
        addModel(sky);
        quad = Primitives.quad(d, 1f);
        teamPaint = new Material[2];
        for (int t = 0; t < 2; t++) {
            // wet paint: smooth, and lit a little from within so it blooms
            teamPaint[t] = new Material(Material.Type.PBR).setColor(TEAM_COLORS[t]).setRoughness(0.22f)
                    .setEmissive(dim(TEAM_COLORS[t], 0.18f));
        }
        // Paint on the field: a splat-shaped texture per variant, flat ones for
        // floors and tops, dripping ones for walls, tinted per team.
        splatPaint = new Material[2][2][PaintDecals.VARIANTS];
        for (int v = 0; v < PaintDecals.VARIANTS; v++) {
            for (int wall = 0; wall < 2; wall++) {
                Texture tex = d.createTexture(PaintDecals.SIZE, PaintDecals.SIZE,
                        PaintDecals.splat(31L * v + 7 * wall + 1, wall == 1));
                for (int t = 0; t < 2; t++) {
                    Material m = new Material(Material.Type.PBR).setColor(TEAM_COLORS[t]).setRoughness(0.3f)
                            .setEmissive(dim(TEAM_COLORS[t], 0.12f)).setTexture(tex);
                    m.setRenderState(RenderState.transparent().setDepthWrite(false)
                            .setCullMode(RenderState.CullMode.NONE));
                    m.setCastShadows(false);
                    splatPaint[t][wall][v] = m;
                }
            }
        }
        float w = arena.width * MM;
        float dpt = arena.depth * MM;
        // Photo-scanned surfaces (Poly Haven, CC0), tiled every few metres:
        // the yard is concrete, the street asphalt, and beyond the wire grass.
        boolean street = arena.id.indexOf("sac") >= 0;
        addModel(ground(surface(street ? "asphalt" : "concrete", 0xff4a4d50), 0.92f, -w / 2f, 0f, dpt / 2f,
                w, dpt, street ? 5f : 4f));
        addModel(ground(surface("grass", 0xff55683a), 1f, -w / 2f, -0.02f, dpt / 2f, 400f, 400f, 6f));
        perimeter(w, dpt);
        greenery(w, dpt);
        List<Box> boxes = arena.boxes();
        for (int i = 0; i < boxes.size(); i++) {
            cover(boxes.get(i), arena);
        }
        if (match.mode() == Mode.SNACK_BAR) {
            Model hill = new Model(sphere, new Material(Material.Type.UNLIT).setColor(0xfff2b632));
            float r = arena.hillRadius * MM * 2f;
            hill.setScale(r, 0.01f, r);
            hill.setPosition(-arena.hillX * MM, 0.005f, arena.hillZ * MM);
            addModel(hill);
        }
        if (match.mode() == Mode.CAPTURE_THE_FLAG) {
            Mesh box = Primitives.cube(d, 1f);
            Material steel = new Material(Material.Type.PBR).setColor(0xffb8bec6).setRoughness(0.35f);
            for (int t = 0; t < 2; t++) {
                flagPole[t] = new Model(box, steel);
                flagPole[t].setScale(0.05f, 2.6f, 0.05f);
                addModel(flagPole[t]);
                flagCloth[t] = new Model(box, new Material(Material.Type.PBR).setColor(TEAM_COLORS[t])
                        .setRoughness(0.8f));
                flagCloth[t].setScale(0.95f, 0.6f, 0.03f);
                addModel(flagCloth[t]);
                flagBase[t] = new Model(sphere, new Material(Material.Type.UNLIT).setColor(TEAM_COLORS[t]));
                flagBase[t].setScale(2.4f, 0.01f, 2.4f);
                flagBase[t].setPosition(-arena.baseX(t) * MM, 0.006f, arena.baseZ(t) * MM);
                addModel(flagBase[t]);
            }
        }

        int n = match.playerCount();
        operators = new AnimatedModel[n];
        held = new AnimatedModel[n];
        opScale = new float[n];
        heldScale = new float[n];
        clips = new String[n];
        moveF = new float[n];
        moveS = new float[n];
        bodyYaw = new float[n];
        wasAlive = new boolean[n];
        pending = new float[n];
        prevX = new float[n];
        prevY = new float[n];
        prevZ = new float[n];
        int[] perTeam = new int[2];
        for (int i = 0; i < n; i++) {
            Player p = match.player(i);
            AnimatedModel op = Models.create(d, Models.operator(p.team(), perTeam[p.team() & 1]++));
            op.hideNode(Models.PISTOL);
            opScale[i] = fit(op, OPERATOR_HEIGHT);
            op.play(Models.IDLE, true);
            clips[i] = Models.IDLE;
            bodyYaw[i] = (float) -(p.yaw() * 360.0 / Trig.FULL);
            op.addTo(this);
            operators[i] = op;
            AnimatedModel m = Models.create(d, Models.marker(p.marker.id));
            heldScale[i] = length(m, HELD_LENGTH);
            m.addTo(this);
            held[i] = m;
        }
        for (int i = 0; i < MAX_BALLS; i++) {
            balls[i] = new Model(sphere, teamPaint[0]);
            balls[i].setScale(0.1f);
            balls[i].setVisible(false);
            addModel(balls[i]);
        }
        for (int i = 0; i < MAX_DECALS; i++) {
            decals[i] = new Model(quad, splatPaint[0][0][0]);
            decals[i].setVisible(false);
            addModel(decals[i]);
        }
        effects(d);
        if (driver.interactive()) {
            // The marker in your hands, added last so it draws over the field.
            Player me = match.player(Math.min(driver.me(), n - 1));
            viewModel = Models.create(d, Models.marker(me.marker.id));
            viewScale = length(viewModel, MARKER_LENGTH);
            viewModel.addTo(this);
        }
        remember();
        touchControls();
        Sfx.load();
    }

    /// A tiling texture from the resources, or a plain `fallback` colour
    /// where it cannot be read.
    private Texture surface(String name, int fallback) {
        try {
            java.io.InputStream in = CN.getResourceAsStream("/tex_" + name + ".jpg");
            if (in != null) {
                try {
                    return device.createTexture(com.codename1.ui.Image.createImage(in)).setWrap(Texture.Wrap.REPEAT);
                } finally {
                    in.close();
                }
            }
        } catch (java.io.IOException e) {
            // drawn in the fallback colour below
        }
        return device.createTexture(1, 1, new int[] {fallback});
    }

    /// A flat rectangle of ground with `tex` repeated every `tile` metres.
    private Model ground(Texture tex, float roughness, float x, float y, float z, float sx, float sz, float tile) {
        float u = sx / tile;
        float v = sz / tile;
        float[] vs = {
            -0.5f, -0.5f, 0, 0, 0, 1, 0, v,
            0.5f, -0.5f, 0, 0, 0, 1, u, v,
            0.5f, 0.5f, 0, 0, 0, 1, u, 0,
            -0.5f, 0.5f, 0, 0, 0, 1, 0, 0,
        };
        com.codename1.gpu.VertexBuffer vb = device.createVertexBuffer(
                com.codename1.gpu.VertexFormat.POSITION_NORMAL_TEXCOORD, 4);
        vb.setData(vs);
        com.codename1.gpu.IndexBuffer ib = device.createIndexBuffer(6);
        ib.setData(new int[] {0, 1, 2, 0, 2, 3});
        Mesh mesh = new Mesh(vb, ib, com.codename1.gpu.PrimitiveType.TRIANGLES);
        Material m = new Material(Material.Type.PBR).setColor(0xffffffff).setRoughness(roughness);
        m.setTexture(tex);
        Model model = new Model(mesh, m);
        model.setRotation(-90f, 0f, 0f);
        model.setScale(sx, sz, 1f);
        model.setPosition(x, y, z);
        return model;
    }

    /// Scales `m` to stand `height` tall and returns the scale.
    private static float fit(AnimatedModel m, float height) {
        float[] b = Models.bounds(m);
        float s = height / Math.max(0.01f, b[4] - b[1]);
        m.setScale(s);
        return s;
    }

    /// Scales `m` so its longest side is `len` and returns the scale.
    private static float length(AnimatedModel m, float len) {
        float[] b = Models.bounds(m);
        float s = len / Math.max(0.01f, Math.max(b[3] - b[0], Math.max(b[4] - b[1], b[5] - b[2])));
        m.setScale(s);
        return s;
    }

    /// The prop a box was sized after, turned to lie along the box and scaled
    /// to its height, its middle on the box's middle. Mirrored twins on the far
    /// half face the other way.
    private void cover(Box box, Arena arena) {
        AnimatedModel m;
        try {
            m = Models.create(device, box.kind);
        } catch (IllegalArgumentException e) {
            m = Models.create(device, "crate");
        }
        float[] b = Models.bounds(m);
        float bw = (box.x1 - box.x0) * MM;
        float bd = (box.z1 - box.z0) * MM;
        float mw = b[3] - b[0];
        float md = b[5] - b[2];
        boolean turn = (bw > bd) != (mw > md) && Math.abs(bw - bd) > 0.05f;
        float s = box.height * MM / Math.max(0.01f, b[4] - b[1]);
        // Fit the footprint to the box the rules collide against, so an operator
        // stopped by the box is stopped by the prop and never inside it. A prop
        // whose box is only its trunk (a tree) keeps at least 40% of its width.
        float sx = Math.max(s * 0.4f, (turn ? bd : bw) / Math.max(0.01f, mw));
        float sz = Math.max(s * 0.4f, (turn ? bw : bd) / Math.max(0.01f, md));
        float cx = -(box.x0 + box.x1) * MM / 2f;
        float cz = (box.z0 + box.z1) * MM / 2f;
        float deg = (turn ? 90f : 0f) + ((box.x0 + box.x1) / 2 > arena.width / 2 ? 180f : 0f);
        double r = Math.toRadians(deg);
        float c = (float) Math.cos(r);
        float sn = (float) Math.sin(r);
        float mx = (b[0] + b[3]) / 2f * sx;
        float mz = (b[2] + b[5]) / 2f * sz;
        m.setScale(s);
        com.codename1.gaming.Model[] parts = m.getParts();
        for (int i = 0; i < parts.length; i++) {
            parts[i].setScale(sx, s, sz);
        }
        m.setRotation(0f, deg, 0f);
        m.setPosition(cx - (mx * c + mz * sn), -b[1] * s, cz - (-mx * sn + mz * c));
        m.addTo(this);
    }

    /// Puts the middle of `m`'s footprint at `(x, z)` with its feet on the ground.
    private static void place(AnimatedModel m, float s, float deg, float x, float z, float[] b) {
        double r = Math.toRadians(deg);
        float c = (float) Math.cos(r);
        float sn = (float) Math.sin(r);
        float mx = (b[0] + b[3]) / 2f * s;
        float mz = (b[2] + b[5]) / 2f * s;
        m.setScale(s);
        m.setRotation(0f, deg, 0f);
        m.setPosition(x - (mx * c + mz * sn), -b[1] * s, z - (-mx * sn + mz * c));
    }

    /// A chain-link yard: fence panels on every side, lights at the corners,
    /// and a few big shapes beyond the wire so the horizon is not empty.
    private void perimeter(float w, float d) {
        AnimatedModel probe = Models.create(device, "metal-fence");
        float[] b = Models.bounds(probe);
        float s = 2.6f / Math.max(0.01f, b[4] - b[1]);
        float panel = Math.max(0.5f, (b[3] - b[0]) * s);
        fenceRow(b, s, panel, 0f, -0.3f, -w, -0.3f, 0f);
        fenceRow(b, s, panel, 0f, d + 0.3f, -w, d + 0.3f, 0f);
        fenceRow(b, s, panel, 0.3f, 0f, 0.3f, d, 90f);
        fenceRow(b, s, panel, -w - 0.3f, 0f, -w - 0.3f, d, 90f);
        float[][] corners = {{1f, -1f}, {-w - 1f, -1f}, {1f, d + 1f}, {-w - 1f, d + 1f}};
        for (int i = 0; i < corners.length; i++) {
            AnimatedModel l = Models.create(device, "streetlight");
            float[] lb = Models.bounds(l);
            place(l, 7f / Math.max(0.01f, lb[4] - lb[1]), i * 90f + 45f, corners[i][0], corners[i][1], lb);
            l.addTo(this);
        }
        String[] beyond = {"shack", "watertower", "container", "tank", "shack", "container-small", "tree", "tree-2"};
        float[] height = {7.8f, 9f, 2.13f, 2.14f, 7.8f, 2.13f, 6f, 5f};
        for (int i = 0; i < 12; i++) {
            int k = i % beyond.length;
            float t = (i * 0.618f) % 1f;
            float x;
            float z;
            if (i % 4 == 0) {
                x = -w * t;
                z = -6f - (i % 3) * 3f;
            } else if (i % 4 == 1) {
                x = -w * t;
                z = d + 6f + (i % 3) * 3f;
            } else if (i % 4 == 2) {
                x = 6f + (i % 3) * 3f;
                z = d * t;
            } else {
                x = -w - 6f - (i % 3) * 3f;
                z = d * t;
            }
            AnimatedModel m = Models.create(device, beyond[k]);
            float[] mb = Models.bounds(m);
            place(m, height[k] / Math.max(0.01f, mb[4] - mb[1]), i * 37f, x, z, mb);
            m.addTo(this);
        }
    }

    /// Fields and woods around the yard, and weeds where the asphalt meets
    /// the fence. All of it is scenery: nothing here blocks a player or a ball.
    private void greenery(float w, float d) {
        int seed = 0x5eed;
        String[] trees = {"oak", "pine", "oak-2", "birch", "pine-2", "oak"};
        float[] treeHeight = {7.5f, 11f, 8.5f, 9f, 12f, 6.5f};
        for (int i = 0; i < 26; i++) {
            seed = seed * 1103515245 + 12345;
            float t = ((seed >>> 8) & 1023) / 1023f;
            seed = seed * 1103515245 + 12345;
            float out = 9f + ((seed >>> 8) & 1023) / 1023f * 26f;
            float[] p = around(w, d, i % 4, t, out);
            int k = (seed >>> 4) % trees.length;
            scenery(trees[k], treeHeight[k] * (0.8f + 0.4f * t), i * 53f, p[0], p[1], true);
        }
        String[] low = {"bush", "bush-2", "bush-berries", "grass", "grass-2", "plant", "rock-moss", "log", "stump"};
        float[] lowHeight = {1.1f, 1.3f, 1f, 0.5f, 0.6f, 0.8f, 0.7f, 0.45f, 0.6f};
        for (int i = 0; i < 34; i++) {
            seed = seed * 1103515245 + 12345;
            float t = ((seed >>> 8) & 1023) / 1023f;
            seed = seed * 1103515245 + 12345;
            float out = 1.2f + ((seed >>> 8) & 1023) / 1023f * 9f;
            float[] p = around(w, d, i % 4, t, out);
            int k = (seed >>> 4) % low.length;
            scenery(low[k], lowHeight[k], i * 71f, p[0], p[1], false);
        }
        // weeds through the cracks along the inside of the wire
        String[] weeds = {"grass-short", "grass", "flowers", "grass-2"};
        for (int i = 0; i < 24; i++) {
            seed = seed * 1103515245 + 12345;
            float t = ((seed >>> 8) & 1023) / 1023f;
            float[] p = around(w, d, i % 4, t, -0.5f - ((seed >>> 3) & 7) * 0.08f);
            scenery(weeds[(seed >>> 5) & 3], 0.35f + 0.1f * t, i * 97f, p[0], p[1], false);
        }
    }

    /// A point `out` metres outside side `side` of the yard (negative is
    /// inside), `t` of the way along it.
    private static float[] around(float w, float d, int side, float t, float out) {
        switch (side) {
            case 0:
                return new float[] {-w * t, -out};
            case 1:
                return new float[] {-w * t, d + out};
            case 2:
                return new float[] {out, d * t};
            default:
                return new float[] {-w - out, d * t};
        }
    }

    /// One piece of scenery; only the big pieces are worth a place in the
    /// shadow pass.
    private void scenery(String name, float height, float deg, float x, float z, boolean shadow) {
        AnimatedModel m = Models.create(device, name);
        if (!shadow) {
            Model[] parts = m.getParts();
            for (int i = 0; i < parts.length; i++) {
                parts[i].getMaterial().setCastShadows(false);
            }
        }
        float[] b = Models.bounds(m);
        place(m, height / Math.max(0.01f, b[4] - b[1]), deg % 360f, x, z, b);
        m.addTo(this);
    }

    private void fenceRow(float[] b, float s, float panel, float x0, float z0, float x1, float z1, float deg) {
        float len = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (z1 - z0) * (z1 - z0));
        int count = Math.max(1, (int) Math.ceil(len / panel));
        for (int i = 0; i < count; i++) {
            float t = (i + 0.5f) / count;
            AnimatedModel m = Models.create(device, "metal-fence");
            place(m, s, deg, x0 + (x1 - x0) * t, z0 + (z1 - z0) * t, b);
            m.addTo(this);
        }
    }

    // ------------------------------------------------------------ controls

    /// On a touch screen: a stick for the left thumb, the buttons for the right,
    /// and dragging anywhere else on the right looks around. A desktop gets
    /// none of it: the mouse looks and fires, the keys move.
    private void touchControls() {
        if (!driver.interactive() || !Display.getInstance().isTouchScreenDevice() || CN.isDesktop()) {
            return;
        }
        TouchControls c = getControls();
        float gap = CN.convertToPixels(3);
        c.addJoystick(CN.convertToPixels(11), TouchControls.LEFT, TouchControls.BOTTOM, gap);
        float big = CN.convertToPixels(9);
        float small = CN.convertToPixels(6);
        // The small buttons sit on an arc around FIRE where the right thumb
        // rolls to them: left of it, up and to the left, and above it.
        float fire = gap + big;
        float arc = big + small + CN.convertToPixels(2.5f);
        buttons = new VirtualButton[] {
            c.addButton(KEY_FIRE, big, TouchControls.RIGHT, TouchControls.BOTTOM, gap, gap),
            onArc(c, KEY_AIM, small, fire, arc, 0),
            onArc(c, KEY_THROW, small, fire, arc, 45),
            onArc(c, KEY_JUMP, small, fire, arc, 90),
            // the bird's-eye view, out of the way of the thumb
            c.addButton(KEY_VIEW, CN.convertToPixels(4.5f), TouchControls.RIGHT, TouchControls.CENTER,
                    gap, 0),
        };
        String[] labels = {"FIRE", "AIM", "NADE", "JUMP", "VIEW"};
        for (int i = 0; i < buttons.length; i++) {
            style(buttons[i], labels[i]);
        }
    }

    /// A button `deg` degrees round from straight left of FIRE's centre.
    private static VirtualButton onArc(TouchControls c, int key, float r, float fire, float arc, int deg) {
        double a = Math.toRadians(deg);
        float dx = fire + (float) (Math.cos(a) * arc);
        float dy = fire + (float) (Math.sin(a) * arc);
        return c.addButton(key, r, TouchControls.RIGHT, TouchControls.BOTTOM, dx - r, dy - r);
    }

    /// Takes the touch buttons off the screen for good, for the result card.
    public void hideControls() {
        controlsHidden = true;
        // a desktop has a keyboard and mouse and no touch buttons at all
        if (buttons.length > 0) {
            getControls().setVisible(false);
        }
    }

    /// The buttons show only while there is something to press: not during
    /// the briefing, and the ADS button reads WIPE while you are out.
    private void controlsFor(Match m) {
        // a desktop has a keyboard and mouse and no touch buttons at all
        if (buttons.length == 0) {
            return;
        }
        boolean show = !controlsHidden && driver.countdownTicks() == 0 && !m.over();
        if (show != getControls().isVisible()) {
            getControls().setVisible(show);
        }
        // aiming while you are in, wiping the paint off while you are out
        buttons[1].setLabel(isOut(m) ? "WIPE" : "AIM");
        buttons[4].setLabel(overheadChosen ? "EYES" : "VIEW");
    }

    /// True while your own player is painted and waiting to come back.
    private boolean isOut(Match m) {
        int me = driver.me();
        return me >= 0 && me < m.playerCount() && !m.player(me).alive();
    }

    private static void style(VirtualButton b, String label) {
        int key = b.getKeyCode();
        b.setLabel(label);
        // dark glass with a light label: readable over sky and asphalt alike
        b.setColor(key == KEY_FIRE ? 0xc0b8861e : 0xa80c0e10);
        b.setLabelColor(0xffeef1f4);
    }

    private boolean onControl(int x, int y) {
        for (int i = 0; i < buttons.length; i++) {
            float dx = x - buttons[i].getCenterX();
            float dy = y - buttons[i].getCenterY();
            float r = buttons[i].getRadius() * 1.15f;
            if (dx * dx + dy * dy <= r * r) {
                return true;
            }
        }
        VirtualJoystick j = getControls().getJoystick();
        if (j != null) {
            float dx = x - j.getCenterX();
            float dy = y - j.getCenterY();
            return dx * dx + dy * dy <= j.getRadius() * j.getRadius() * 1.6f;
        }
        return false;
    }

    @Override
    public void pointerPressed(int[] x, int[] y) {
        super.pointerPressed(x, y);
        int i = lookPointer(x, y);
        if (i >= 0) {
            lastLookX = x[i];
            lastLookY = y[i];
            if (buttons.length == 0) {
                // A mouse: the click is the trigger.
                tapFire = 1;
            }
        }
    }

    @Override
    public void pointerDragged(int[] x, int[] y) {
        super.pointerDragged(x, y);
        int i = lookPointer(x, y);
        if (i < 0) {
            return;
        }
        if (lastLookX >= 0) {
            look(x[i] - lastLookX, y[i] - lastLookY);
        }
        lastLookX = x[i];
        lastLookY = y[i];
    }

    @Override
    public void pointerReleased(int[] x, int[] y) {
        super.pointerReleased(x, y);
        lastLookX = -1;
        lastLookY = -1;
        if (buttons.length == 0) {
            tapFire = 0;
        }
    }

    /// The touch that steers the view: on the right side and on no control, or
    /// any pointer at all with a mouse.
    private int lookPointer(int[] x, int[] y) {
        for (int i = 0; i < x.length; i++) {
            int lx = x[i] - getAbsoluteX();
            int ly = y[i] - getAbsoluteY();
            if (buttons.length == 0 || (lx > getWidth() * 2 / 5 && !onControl(lx, ly))) {
                return i;
            }
        }
        return -1;
    }

    private void look(int dx, int dy) {
        int w = Math.max(1, getWidth());
        if (invert) {
            dy = -dy;
        }
        lookYaw = Trig.wrap(lookYaw + dx * Trig.FULL / (w * 2));
        lookPitch = Math.max(-Match.PITCH_LIMIT, Math.min(Match.PITCH_LIMIT, lookPitch - dy * Trig.FULL / (w * 2)));
    }

    /// The input for the coming tick, from the keys, the stick and the buttons.
    private Input input() {
        Player p = driver.match().player(driver.me());
        if (!aimed) {
            lookYaw = p.yaw();
            lookPitch = p.pitch();
            aimed = true;
        }
        int strafe = 0;
        int forward = 0;
        if (key('w') || key(Display.getInstance().getKeyCode(Display.GAME_UP))) {
            forward += 127;
        }
        if (key('s') || key(Display.getInstance().getKeyCode(Display.GAME_DOWN))) {
            forward -= 127;
        }
        if (key('a')) {
            strafe -= 127;
        }
        if (key('d')) {
            strafe += 127;
        }
        if (key(Display.getInstance().getKeyCode(Display.GAME_LEFT))) {
            lookYaw = Trig.wrap(lookYaw - 60);
        }
        if (key(Display.getInstance().getKeyCode(Display.GAME_RIGHT))) {
            lookYaw = Trig.wrap(lookYaw + 60);
        }
        // V (or the VIEW button) flips the bird's-eye view, once a press
        boolean view = key(KEY_VIEW);
        if (view && !viewHeld) {
            toggleOverhead();
        }
        viewHeld = view;
        boolean sprint = key(KEY_SPRINT);
        VirtualJoystick j = getControls().getJoystick();
        if (j != null && j.isActive()) {
            strafe = (int) (j.getAxisX() * 127);
            forward = (int) (-j.getAxisY() * 127);
            sprint = sprint || j.getAxisY() < -0.92f;
        }
        int b = 0;
        if (key(KEY_FIRE) || tapFire > 0) {
            b |= Input.FIRE;
        }
        if (key(KEY_JUMP)) {
            b |= Input.JUMP;
        }
        if (key(KEY_CROUCH)) {
            b |= Input.CROUCH;
        }
        if (sprint) {
            b |= Input.SPRINT;
        }
        if (key(KEY_RELOAD)) {
            b |= Input.RELOAD;
        }
        if (key(KEY_THROW)) {
            b |= Input.THROW;
        }
        boolean out = isOut(driver.match());
        if (key(KEY_WIPE) || (out && key(KEY_AIM))) {
            b |= Input.WIPE;
        }
        if (!out && key(KEY_AIM)) {
            b |= Input.AIM;
        }
        return new Input(clamp(strafe), clamp(forward), lookYaw, lookPitch, b, -1);
    }

    private boolean key(int code) {
        return getInput().isKeyDown(code) || (code >= 'a' && code <= 'z' && getInput().isKeyDown(code - 32));
    }

    private static int clamp(int v) {
        return Math.max(-127, Math.min(127, v));
    }

    // ------------------------------------------------------------ the loop

    // tag::splat-fixed-tick[]
    @Override
    protected void update(double dt) {
        if (device == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        controlsFor(driver.match());
        govern(dt);
        accumulator += dt * driver.speedPercent() / 100.0;
        int steps = 0;
        while (accumulator >= TICK && steps < MAX_STEPS_PER_FRAME) {
            if (!driver.canStep()) {
                // The briefing, or online and the server has gone quiet.
                accumulator = Math.min(accumulator, TICK);
                break;
            }
            remember();
            Input in = !driver.interactive() ? Input.IDLE : autopilot != null
                    ? com.codenameone.examples.splat.rules.Bot.think(driver.match(), driver.match().player(driver.me()), autopilot)
                    : input();
            if (heldButtons != 0 && driver.interactive()) {
                in = new Input(in.strafe, in.forward, in.yaw, in.pitch, in.buttons | heldButtons, in.viewTick);
            }
            driver.step(in);
            // every tick, not every frame: a slow frame steps several
            record();
            accumulator -= TICK;
            steps++;
        }
        alpha = (float) Math.min(1, accumulator / TICK);
        // and once a frame for a driver whose ticks arrive from the server
        record();
        events();
        bodySounds((float) dt);
        draw((float) dt);
        tick((float) dt);
    }
    // end::splat-fixed-tick[]

    /// Steps the look down when frames stay slow, so a weak GPU (or a software
    /// renderer) still plays at speed: the post chain goes first, then the
    /// shadow pass. It never steps back up within a match; a frame time that
    /// recovers after a drop is usually the drop working.
    private void govern(double dt) {
        if (qualityDrops >= 2) {
            return;
        }
        slowFor = dt > SLOW_FRAME ? slowFor + dt : Math.max(0, slowFor - dt);
        if (slowFor < 2.0) {
            return;
        }
        slowFor = 0;
        qualityDrops++;
        if (qualityDrops == 1) {
            setPostProcess(null);
        } else {
            getLight().setShadows(false);
        }
    }

    private void record() {
        Match m = driver.match();
        if (m.tick() != recordedTick) {
            recordedTick = m.tick();
            history[recordedTick % HISTORY] = m.snapshot();
        }
    }

    /// Plays back the moment you were painted, every time, in the [#SHOTS]:
    /// it starts once the stamp has landed and ends when you are back in.
    private void replayFrom(int shooter, int victim, int hitTick) {
        replayShooter = shooter;
        replayVictim = victim;
        replayHit = hitTick;
        replayStartAt = System.currentTimeMillis() + 800;
        startShot(0);
        shotAt = replayStartAt;
    }

    private void startShot(int s) {
        shot = s;
        shotAt = System.currentTimeMillis();
        chaseYaw = Double.NaN;
        splashed = false;
        float[] row = SHOTS[s];
        int from = replayHit + (int) row[1];
        // the first shot starts no earlier than the history goes back
        while (from < replayHit && !recorded(from)) {
            from++;
        }
        replayPos = from;
        replayTo = replayHit + (int) row[2];
        replaySpeed = row[3];
    }

    /// What the camera is looking at, for the HUD to put words over.
    public Match drawnMatch() {
        Match m = drawn;
        return m == null ? driver.match() : m;
    }

    /// The angle of the replay shot on screen, one of the `ANGLE_` values,
    /// or -1 when no replay is playing.
    public int replayAngle() {
        return replayingShooter() >= 0 ? (int) SHOTS[shot][0] : -1;
    }

    /// Milliseconds since the replay cut to its current shot.
    public long sinceCut() {
        return System.currentTimeMillis() - shotAt;
    }

    /// The player the replay is about: the one who was painted.
    public int replayingVictim() {
        return replayVictim;
    }

    /// The most ticks a replay blends across: a frame of four at a slow 8 fps.
    private static final int GAP = 4;

    private boolean recorded(int tick) {
        int[] snap = tick < 0 ? null : history[tick % HISTORY];
        return snap != null && snap[1] == tick;
    }

    /// True from the moment you are painted until your replay has played:
    /// the stamp is up, and the replay is waiting for it or on screen.
    public boolean replayQueued() {
        return replayShooter >= 0;
    }

    /// The shooter of the replay on screen, or -1 when none is playing.
    public int replayingShooter() {
        return replayShooter >= 0 && System.currentTimeMillis() >= replayStartAt ? replayShooter : -1;
    }

    /// Whether the camera is above the field rather than at the eye.
    public boolean isOverhead() {
        return overheadNow;
    }

    /// Switches between your eyes and the view from above.
    public void toggleOverhead() {
        overheadChosen = !overheadChosen;
        overSet = false;
    }

    /// The match to draw this frame: the replay's, positioned between two of
    /// its ticks, or the one in play.
    private Match shown(float dt) {
        if (replayShooter < 0 || System.currentTimeMillis() < replayStartAt) {
            return driver.match();
        }
        Match live = driver.match();
        // An online driver's ticks arrive with the server's snapshots, so some
        // can be missing from the history: blend the nearest recorded ones.
        int t = (int) replayPos;
        int a = t;
        while (a > t - GAP && !recorded(a)) {
            a--;
        }
        int b = t + 1;
        while (b < t + 1 + GAP && !recorded(b)) {
            b++;
        }
        if (t + 1 >= replayTo && shot + 1 < SHOTS.length && !live.player(driver.me()).alive()) {
            // a cut to the next angle
            startShot(shot + 1);
            return shown(0f);
        }
        if (t + 1 >= replayTo || live.player(driver.me()).alive() || !recorded(a) || !recorded(b)) {
            replayShooter = -1;
            replayVictim = -1;
            replaySpeed = 1f;
            remember();
            return live;
        }
        if (ghost == null) {
            ghost = live.fork();
        }
        // where everyone was a tick before, then where they are, for the lerp
        ghost.restore(history[a % HISTORY]);
        int n = Math.min(prevX.length, ghost.playerCount());
        for (int i = 0; i < n; i++) {
            Player p = ghost.player(i);
            prevX[i] = p.x() * MM;
            prevY[i] = p.y() * MM;
            prevZ[i] = p.z() * MM;
        }
        ghost.restore(history[b % HISTORY]);
        alpha = (replayPos - a) / (b - a);
        replayPos += dt * Match.TICK_HZ * replaySpeed;
        if (!splashed && replayPos >= replayHit && replayVictim >= 0) {
            // the events are not replayed, so the splash is thrown again here
            splashed = true;
            Player v = ghost.player(replayVictim);
            burstHit(ghost.player(replayShooter).team() & 1, v.x(), v.y() + 1200, v.z());
            Sfx.splat(0.9f, 0f);
        }
        return ghost;
    }

    private void remember() {
        Match m = driver.match();
        int n = Math.min(prevX.length, m.playerCount());
        for (int i = 0; i < n; i++) {
            Player p = m.player(i);
            prevX[i] = p.x() * MM;
            prevY[i] = p.y() * MM;
            prevZ[i] = p.z() * MM;
        }
    }

    private void events() {
        List<Event> happened = driver.drainEvents();
        Match m = driver.match();
        int me = driver.me();
        int team = m.player(me).team();
        for (int i = 0; i < happened.size(); i++) {
            Event e = happened.get(i);
            if (e.kind == Event.SPLAT || e.kind == Event.GRENADE) {
                decal(e.b & 1, e.x, e.y, e.z, e.kind == Event.GRENADE ? 2.6f : 0.55f);
                burstImpact(e.b & 1, e.x, e.y, e.z, e.kind == Event.GRENADE);
                if (e.kind == Event.GRENADE) {
                    Sfx.grenade(place(e.x, e.z, 45), pan);
                } else {
                    Sfx.splat(place(e.x, e.z, 25) * 0.8f, pan);
                }
            } else if (e.kind == Event.PAINTED) {
                Player shooter = m.player(e.a);
                decal(shooter.team(), e.x, e.y, e.z, 0.7f);
                burstHit(shooter.team() & 1, e.x, e.y, e.z);
                if (e.b == me) {
                    Sfx.painted();
                    Sfx.whistle(0.9f, 0);
                    knock = 1f;
                    if (driver.interactive() && e.a != me) {
                        replayFrom(e.a, e.b, e.tick);
                    }
                } else {
                    Sfx.splat(place(e.x, e.z, 30), pan);
                    Sfx.whistle(Math.max(0.35f, place(e.x, e.z, 60)), pan * 0.5f);
                }
                if (e.a == me) {
                    Sfx.hit();
                }
            } else if (e.kind == Event.SHOT) {
                if (e.a == me && driver.interactive()) {
                    recoil = 1f;
                    Sfx.fire();
                    // your own flash sits at the marker you hold, not the ball
                    flash.burst(viewMuzzleX, viewMuzzleY, viewMuzzleZ, 3);
                } else {
                    Sfx.fireAt(place(e.x, e.z, 35), pan);
                    if (near(e.x, e.z)) {
                        flash.burst(-e.x * MM, e.y * MM, e.z * MM, 3);
                    }
                }
            } else if (e.kind == Event.WIPE_OK && e.a == me) {
                Sfx.wipe();
            } else if (e.kind == Event.WIPE_CAUGHT) {
                if (e.a == me) {
                    Sfx.wipe();
                    Sfx.buzzer();
                }
                Sfx.whistle(e.a == me ? 1f : 0.5f, 0);
            } else if (e.kind == Event.FRIENDLY && (e.a == me || e.b == me)) {
                Sfx.boing();
            } else if (e.kind == Event.RELOAD && e.a == me) {
                Sfx.reload();
            } else if (e.kind == Event.FLAG_TAKEN) {
                Sfx.flag(e.b != team);
            } else if (e.kind == Event.CAPTURE) {
                Sfx.capture(e.b == team);
            } else if (e.kind == Event.HILL_POINT) {
                Sfx.point(e.b == team);
            } else if (e.kind == Event.END) {
                Sfx.whistleTwice();
                Music.stop();
                if (e.b >= 0 && driver.interactive()) {
                    if (e.b == team) {
                        Sfx.win();
                    } else {
                        Sfx.lose();
                    }
                }
            } else if (e.kind == Event.RESPAWN && e.a == me) {
                aimed = false;
            }
            if (hud != null) {
                // The HUD is a component the EDT paints; this runs on the
                // render thread, so its lists change on the EDT's turn.
                final Hud h = hud;
                final Event ev = e;
                CN.callSerially(() -> h.event(ev));
            }
        }
    }

    /// The pan of the last sound [#place] placed: -1 is hard left, 1 hard right.
    private float pan;

    /// How loud a sound at `x z` is where you stand: full within a few metres,
    /// nothing beyond `range` metres. Leaves its direction in [#pan].
    private float place(int x, int z, int range) {
        Player p = driver.match().player(driver.me());
        double dx = x - p.x();
        double dz = z - p.z();
        double d = Math.sqrt(dx * dx + dz * dz) / 1000;
        int yaw = driver.interactive() && p.alive() ? lookYaw : p.yaw();
        // the same frame as the damage arcs: 0 straight ahead, clockwise positive
        double toward = MathUtil.atan2(-dx, dz);
        double facing = -yaw * 2 * Math.PI / Trig.FULL;
        pan = d < 1.5 ? 0 : (float) Math.sin(toward - facing) * 0.8f;
        double v = 1 - (d - 3) / (range - 3);
        return (float) Math.max(0, Math.min(1, v * v));
    }

    private int lastCount = -1;
    private boolean wasGrounded = true;
    private boolean hadGrenade;
    private float stride;

    /// The sounds that come from your own body and the clock rather than from
    /// an event: the countdown, footsteps, landing and the grenade leaving
    /// your hand.
    private void bodySounds(float dt) {
        Match m = driver.match();
        int me = driver.me();
        Player p = m.player(me);
        int count = driver.countdownTicks();
        int secs = (count + Match.TICK_HZ - 1) / Match.TICK_HZ;
        if (lastCount >= 0 && secs != lastCount && driver.interactive()) {
            if (secs == 0) {
                Sfx.horn();
                Sfx.whistleTwice();
                Music.field();
            } else if (secs <= 3) {
                Sfx.beep();
            }
        }
        lastCount = secs;
        if (!p.alive() || m.over()) {
            stride = 0;
            wasGrounded = true;
            hadGrenade = p.hasGrenade();
            return;
        }
        if (driver.interactive() && hadGrenade && !p.hasGrenade()) {
            Sfx.toss();
        }
        hadGrenade = p.hasGrenade();
        boolean grounded = p.grounded();
        if (grounded && !wasGrounded) {
            Sfx.land();
            stride = 0;
        }
        wasGrounded = grounded;
        float dx = p.x() * MM - prevX[me];
        float dz = p.z() * MM - prevZ[me];
        float speed = (float) Math.sqrt(dx * dx + dz * dz) * Match.TICK_HZ;
        if (grounded && speed > 0.8f) {
            stride += speed * dt;
            // a stride is about 1.6 m walking, a little longer at a sprint
            if (stride > (speed > 5 ? 1.9f : 1.6f)) {
                stride = 0;
                Sfx.step(p.crouched() ? 0.25f : speed > 5 ? 0.6f : 0.45f);
            }
        }
    }

    private void effects(GraphicsDevice d) {
        for (int t = 0; t < 2; t++) {
            // the droplets a ball throws off when it bursts
            spray[t] = new ParticleEmitter(d, 220).setColor(TEAM_COLORS[t]).setSpeed(2.5f, 6.5f)
                    .setLifetime(0.2f, 0.45f).setSize(0.035f, 0.01f).setGravity(9.8f).setDrag(1.5f)
                    .setDirection(0f, 1f, 0f, 75f);
            spray[t].addTo(this);
            // and the cloud of it that hangs in the air a moment
            mist[t] = new ParticleEmitter(d, 160).setColor(0x40000000 | (TEAM_COLORS[t] & 0xffffff))
                    .setSpeed(0.3f, 1.0f).setLifetime(0.3f, 0.7f).setSize(0.12f, 0.45f).setGravity(-0.15f)
                    .setDrag(2.5f);
            mist[t].addTo(this);
            // the gobs a hit on a player throws: big, slow, and falling
            goo[t] = new ParticleEmitter(d, 120).setColor(TEAM_COLORS[t]).setSpeed(1.2f, 4.2f)
                    .setLifetime(0.5f, 1.0f).setSize(0.09f, 0.05f).setGravity(9.8f).setDrag(0.6f)
                    .setDirection(0f, 1f, 0f, 85f);
            goo[t].addTo(this);
        }
        dust = new ParticleEmitter(d, 200).setColor(0x509a8f80).setSpeed(0.4f, 1.6f).setLifetime(0.9f, 1.8f)
                .setSize(0.2f, 1.0f).setGravity(-0.25f).setDrag(2f).setDirection(0f, 1f, 0f, 70f);
        dust.addTo(this);
        flash = new ParticleEmitter(d, 48).setColor(0xffffc778).setAdditive(true).setSpeed(0f, 0.6f)
                .setLifetime(0.04f, 0.08f).setSize(0.22f, 0.05f);
        flash.addTo(this);
        // dust caught in the sun: a few faint specks drifting around the eye
        motes = new ParticleEmitter(d, 90).setColor(0x55fff1d6).setAdditive(true).setSpeed(0.02f, 0.12f)
                .setLifetime(3f, 6f).setSize(0.012f, 0.02f).setGravity(-0.01f);
        motes.addTo(this);
    }

    /// A ball bursting on a player: far more than on a wall, since this is the
    /// moment the whole game is about. Drops fly out, fat gobs arc and fall,
    /// and a cloud of paint hangs where they stood.
    private void burstHit(int team, int x, int y, int z) {
        float px = -x * MM;
        float py = Math.max(0.3f, y * MM);
        float pz = z * MM;
        spray[team].burst(px, py, pz, 70);
        goo[team].burst(px, py, pz, 28);
        mist[team].burst(px, py, pz, 22);
    }

    private void burstImpact(int team, int x, int y, int z, boolean grenade) {
        float px = -x * MM;
        float py = Math.max(0.03f, y * MM);
        float pz = z * MM;
        spray[team].burst(px, py, pz, grenade ? 90 : 14);
        mist[team].burst(px, py, pz, grenade ? 40 : 4);
        if (y < 200) {
            dust.burst(px, 0.05f, pz, grenade ? 24 : 3);
        }
    }

    private void drift(float dt) {
        moteClock += dt;
        if (moteClock < 0.12f) {
            return;
        }
        moteClock = 0f;
        GameCamera c = getCamera();
        moteSeed = moteSeed * 1103515245 + 12345;
        float ox = ((moteSeed >>> 8) & 1023) / 1023f * 8f - 4f;
        moteSeed = moteSeed * 1103515245 + 12345;
        float oz = ((moteSeed >>> 8) & 1023) / 1023f * 8f - 4f;
        moteSeed = moteSeed * 1103515245 + 12345;
        float oy = ((moteSeed >>> 8) & 1023) / 1023f * 2.2f - 0.6f;
        motes.burst(c.getEyeX() + ox, c.getEyeY() + oy, c.getEyeZ() + oz, 1);
    }

    private void tick(float dt) {
        GameCamera c = getCamera();
        // paint hangs in the air as slowly as the replay plays
        if (replayVictim >= 0) {
            dt *= replaySpeed;
        }
        for (int t = 0; t < 2; t++) {
            spray[t].update(dt, c);
            mist[t].update(dt, c);
            goo[t].update(dt, c);
        }
        dust.update(dt, c);
        flash.update(dt, c);
        drift(dt);
        motes.update(dt, c);
    }

    /// Close enough to the camera to be worth a sound: 12 m.
    private boolean near(int x, int z) {
        Player p = driver.match().player(driver.me());
        long dx = x - p.x();
        long dz = z - p.z();
        return dx * dx + dz * dz < 144000000L;
    }

    /// Paint where a ball burst, squashed flat against whatever it hit: the
    /// ground, the top of a box or the nearest of its sides.
    /// Props whose sides are the sides of their box, so paint can lie on them.
    private static boolean flatSided(String kind) {
        return "container".equals(kind) || "container-small".equals(kind) || "crate".equals(kind)
                || "boxes".equals(kind) || "dumpster".equals(kind) || "snackbar".equals(kind)
                || "barrier".equals(kind) || "barrier-single".equals(kind) || "watertower".equals(kind);
    }

    private void decal(int team, int x, int y, int z, float size) {
        Model d = decals[nextDecal];
        nextDecal = (nextDecal + 1) % MAX_DECALS;
        int variant = ((x * 31) ^ (z * 17) ^ y) & (PaintDecals.VARIANTS - 1);
        // a mirror on either axis makes four shapes look like sixteen
        float s = size * 1.7f * (0.8f + ((x ^ z) & 7) / 20f);
        float sx = ((x >> 3) & 1) == 0 ? s : -s;
        float sy = ((z >> 3) & 1) == 0 ? s : -s;
        Box hit = null;
        List<Box> boxes = driver.match().arena().boxes();
        for (int i = 0; i < boxes.size(); i++) {
            Box b = boxes.get(i);
            if (x >= b.x0 - 150 && x <= b.x1 + 150 && z >= b.z0 - 150 && z <= b.z1 + 150 && y <= b.height + 150) {
                hit = b;
                break;
            }
        }
        float px = -x * MM;
        float py = y * MM;
        float pz = z * MM;
        boolean wall = false;
        // The quad faces +z; turned to lie on whatever was hit.
        float rx = -90;
        float ry = 0;
        if (hit == null || y < 120) {
            // Each splat a layer of its own, 2 mm apart, so two that overlap
            // never trade places from one frame to the next.
            py = 0.02f + (nextDecal & 15) * 0.002f;
        } else {
            int dx0 = Math.abs(x - hit.x0);
            int dx1 = Math.abs(x - hit.x1);
            int dz0 = Math.abs(z - hit.z0);
            int dz1 = Math.abs(z - hit.z1);
            int dTop = Math.abs(y - hit.height);
            int best = Math.min(Math.min(dx0, dx1), Math.min(Math.min(dz0, dz1), dTop));
            if (!flatSided(hit.kind)) {
                // a car or a barrel is round: paint on its box would float
                // in the air beside it, so the splat lands at its foot
                py = 0.02f + (nextDecal & 15) * 0.002f;
                int out = (int) (size * 400);
                if (Math.min(dx0, dx1) <= Math.min(dz0, dz1)) {
                    px = -(dx0 < dx1 ? hit.x0 - out : hit.x1 + out) * MM;
                } else {
                    pz = (dz0 < dz1 ? hit.z0 - out : hit.z1 + out) * MM;
                }
            } else if (best == dTop) {
                py = hit.height * MM + 0.02f + (nextDecal & 7) * 0.002f;
                // kept inside the top, or its edge hangs in the air
                int half = Math.min((int) (s * 500), Math.min(hit.x1 - hit.x0, hit.z1 - hit.z0) / 2);
                px = -fit(x, hit.x0, hit.x1, half) * MM;
                pz = fit(z, hit.z0, hit.z1, half) * MM;
                s = half / 500f;
                sx = sx < 0 ? -s : s;
                sy = sy < 0 ? -s : s;
            } else {
                // drips run down a wall, so the texture is not mirrored upside down
                wall = true;
                rx = 0;
                // kept inside the face it hit: one past an edge or above the
                // top is paint in mid air
                boolean xFace = best == dx0 || best == dx1;
                int half = Math.min((int) (s * 500), Math.min(xFace ? hit.z1 - hit.z0 : hit.x1 - hit.x0,
                        hit.height) / 2);
                if (xFace) {
                    pz = fit(z, hit.z0, hit.z1, half) * MM;
                } else {
                    px = -fit(x, hit.x0, hit.x1, half) * MM;
                }
                py = fit(y, 0, hit.height, half) * MM;
                s = half / 500f;
                sx = sx < 0 ? -s : s;
                sy = s;
                if (xFace) {
                    // world x is drawn mirrored, so the x0 face looks toward +x on screen
                    px = -(best == dx0 ? hit.x0 - 25 : hit.x1 + 25) * MM;
                    ry = best == dx0 ? 90 : -90;
                } else {
                    pz = (best == dz0 ? hit.z0 - 25 : hit.z1 + 25) * MM;
                    ry = best == dz0 ? 180 : 0;
                }
            }
        }
        d.setMaterial(splatPaint[team & 1][wall ? 1 : 0][variant]);
        d.setRotation(rx, ry, 0);
        d.setScale(sx, sy, 1);
        d.setPosition(px, py, pz);
        d.setVisible(true);
    }

    /// `v` moved so a span of `half` either side of it stays in `[lo, hi]`.
    private static int fit(int v, int lo, int hi, int half) {
        return Math.max(lo + half, Math.min(hi - half, v));
    }

    private void draw(float dt) {
        Match m = shown(dt);
        drawn = m;
        int me = driver.me();
        boolean replaying = m != driver.match();
        Player self = m.player(me);
        // Above the field when asked for, and while you wait to go back in
        // once the replay has played: the floor was all there was to see.
        overheadNow = !replaying && (overheadChosen || driver.interactive() && !self.alive()
                && System.currentTimeMillis() > replayStartAt + 600 && !m.over());
        boolean firstPerson = driver.interactive() && !replaying && !overheadNow && !m.over();
        for (int i = 0; i < operators.length && i < m.playerCount(); i++) {
            operator(m, i, i == me && firstPerson, dt);
        }
        List<Ball> live = m.balls();
        for (int i = 0; i < MAX_BALLS; i++) {
            if (i < live.size()) {
                Ball b = live.get(i);
                // Drawn a fraction of a tick ahead, so a ball moves every frame.
                float bx = (b.x() + b.vx() * alpha) * MM;
                float by = (b.y() + b.vy() * alpha) * MM;
                float bz = (b.z() + b.vz() * alpha) * MM;
                balls[i].setMaterial(teamPaint[b.team()]);
                balls[i].setScale(b.grenade() ? 0.2f : 0.07f);
                balls[i].setPosition(-bx, by, bz);
                balls[i].setVisible(true);
            } else {
                balls[i].setVisible(false);
            }
        }
        if (m.mode() == Mode.CAPTURE_THE_FLAG && flagPole[0] != null) {
            flagWave += dt;
            for (int t = 0; t < 2; t++) {
                float fx = -m.flagX(t) * MM;
                float fy = m.flagY(t) * MM;
                float fz = m.flagZ(t) * MM;
                float yaw = flagWave * 40f + t * 90f;
                int c = m.flagCarrier(t);
                if (c >= 0 && c < bodyYaw.length) {
                    // slung on the carrier's back, the cloth streaming behind,
                    // so whoever has it is obvious from anywhere
                    double r = Math.toRadians(-bodyYaw[c]);
                    fx = -lerp(prevX[c], m.player(c).x() * MM) + (float) Math.sin(r) * 0.3f;
                    fz = lerp(prevZ[c], m.player(c).z() * MM) - (float) Math.cos(r) * 0.3f;
                    fy = lerp(prevY[c], m.player(c).y() * MM) + 0.6f;
                    yaw = bodyYaw[c] + 90f;
                }
                double yr = Math.toRadians(yaw);
                float flap = (float) Math.sin(flagWave * 7f + t) * 12f;
                flagPole[t].setPosition(fx, fy + 1.3f, fz);
                flagCloth[t].setPosition(fx + (float) Math.cos(yr) * 0.48f, fy + 2.25f,
                        fz - (float) Math.sin(yr) * 0.48f);
                flagCloth[t].setRotation(0f, yaw + flap, flap * 0.3f);
            }
        }
        if (replaying && replayVictim >= 0 && shot > 0) {
            angle(m, replayShooter, replayVictim, (int) SHOTS[shot][0]);
        } else if (replaying) {
            chase(m, replayShooter, dt);
        } else if (m.over()) {
            celebrate(m, m.winner(), dt);
        } else if (overheadNow) {
            overhead(m, me, dt);
        } else {
            camera(m, me, dt);
        }
        if (hud != null) {
            hud.frame(driver.match(), me, lookYaw, lookPitch);
        }
    }

    /// One operator: where the rules put them, facing their look, playing the
    /// clip their motion asks for, the marker in the right hand.
    private void operator(Match m, int i, boolean first, float dt) {
        Player p = m.player(i);
        if (p.alive() && !wasAlive[i] && m.tick() > 2) {
            // back in: a puff of smoke where they went down, and one where
            // they reappear, so the jump across the yard reads as a respawn
            float ox = -prevX[i];
            dust.burst(ox, 0.6f, prevZ[i], 30);
            dust.burst(-p.x() * MM, 0.6f, p.z() * MM, 30);
            flash.burst(-p.x() * MM, 1.0f, p.z() * MM, 3);
        }
        wasAlive[i] = p.alive();
        AnimatedModel op = operators[i];
        AnimatedModel gun = held[i];
        op.setVisible(!first);
        gun.setVisible(!first && p.alive() && !m.over());
        if (first) {
            return;
        }
        float x = -lerp(prevX[i], p.x() * MM);
        float y = lerp(prevY[i], p.y() * MM);
        float z = lerp(prevZ[i], p.z() * MM);
        // The body turns smoothly toward the look; the rules turn in steps.
        float target = (float) -(p.yaw() * 360.0 / Trig.FULL);
        float turn = target - bodyYaw[i];
        while (turn > 180f) {
            turn -= 360f;
        }
        while (turn < -180f) {
            turn += 360f;
        }
        bodyYaw[i] += turn * Math.min(1f, dt * 14f);
        float yawDeg = bodyYaw[i];
        double yr = Math.toRadians(-yawDeg);
        // Motion in the operator's own frame, in m/s, smoothed over a fifth
        // of a second so a bot's steering jitter never reaches the legs.
        float vx = -(p.x() * MM - prevX[i]) * Match.TICK_HZ;
        float vz = (p.z() * MM - prevZ[i]) * Match.TICK_HZ;
        float k = Math.min(1f, dt * 6f);
        moveF[i] += ((float) (vx * -Math.sin(yr) + vz * Math.cos(yr)) - moveF[i]) * k;
        moveS[i] += ((float) (vx * -Math.cos(yr) + vz * -Math.sin(yr)) - moveS[i]) * k;
        float fwd = moveF[i];
        float side = moveS[i];
        float speed = (float) Math.sqrt(fwd * fwd + side * side);
        boolean firing = p.input().has(Input.FIRE);
        boolean slow = p.crouched() || p.input().has(Input.AIM);
        String want;
        float clipSpeed = 1f;
        if (m.over()) {
            want = celebration(m, i);
        } else if (!p.alive()) {
            if (p.input().has(Input.WIPE)) {
                want = Models.WAVE;
            } else {
                float since = (m.tick() - p.paintedAt()) / (float) Match.TICK_HZ;
                want = performance(i, p.paintedAt(), since);
                clipSpeed = drama(i, p.paintedAt()) == DRAMA_SLOW ? 0.45f : 1f;
            }
        } else if (speed < 0.6f) {
            want = firing ? Models.IDLE_SHOOT : Models.IDLE;
        } else if (slow || speed < 2.6f) {
            want = Models.WALK;
        } else if (Math.abs(fwd) >= Math.abs(side) * 0.8f) {
            want = fwd < 0 ? Models.RUN_BACK : firing ? Models.RUN_SHOOT : Models.RUN;
        } else {
            want = side > 0 ? Models.RUN_RIGHT : Models.RUN_LEFT;
        }
        // A new clip has to be wanted for a moment before it plays, except
        // being painted, which is never late.
        boolean urgent = !p.alive() || m.over() || clips[i] == null;
        if (!want.equals(clips[i])) {
            pending[i] += dt;
            if (urgent || pending[i] > 0.15f) {
                clips[i] = want;
                pending[i] = 0;
                boolean once = Models.DEATH.equals(want) || Models.HIT.equals(want)
                        || Models.HIT2.equals(want) || Models.ROLL.equals(want);
                op.crossFade(want, once ? 0.08f : 0.25f, !once);
            }
        } else {
            pending[i] = 0;
        }
        String now = clips[i];
        if (Models.WALK.equals(now)) {
            op.setSpeed(Math.max(0.5f, Math.min(1.6f, speed / WALK_PACE)));
        } else if (now != null && now.startsWith("Run")) {
            op.setSpeed(Math.max(0.6f, Math.min(1.5f, speed / RUN_PACE)));
        } else {
            op.setSpeed(clipSpeed);
        }
        op.update(replayVictim >= 0 && replaying(m) ? dt * replaySpeed : dt);
        op.setPosition(x, y, z);
        op.setRotation(0f, yawDeg, 0f);
        if (p.alive() && op.getNodeMatrix(Models.HAND, hand)) {
            float s = opScale[i];
            float c = (float) Math.cos(Math.toRadians(yawDeg));
            float sn = (float) Math.sin(Math.toRadians(yawDeg));
            float hx = hand[12] * s;
            float hz = hand[14] * s;
            gun.setPosition(x + hx * c + hz * sn, op.getY() + hand[13] * s, z - hx * sn + hz * c);
            gun.setRotation(0f, yawDeg + 180f, 0f);
        }
    }

    /// Over the shooter's right shoulder, looking where they look.
    private void chase(Match m, int s, float dt) {
        Player p = m.player(s);
        float x = -lerp(prevX[s], p.x() * MM);
        float y = lerp(prevY[s], p.y() * MM);
        float z = lerp(prevZ[s], p.z() * MM);
        // The rules turn in steps, and a bot's aim twitches every tick: a
        // camera 2.6 m behind on that raw heading swings the shooter about
        // the screen, so it eases after the look instead.
        double want = p.yaw() * 2 * Math.PI / Trig.FULL;
        if (Double.isNaN(chaseYaw)) {
            chaseYaw = want;
        }
        double turn = want - chaseYaw;
        while (turn > Math.PI) {
            turn -= 2 * Math.PI;
        }
        while (turn < -Math.PI) {
            turn += 2 * Math.PI;
        }
        chaseYaw += turn * Math.min(1f, dt * 4f);
        double yr = chaseYaw;
        float fx = (float) -Math.sin(yr);
        float fz = (float) Math.cos(yr);
        float rx = (float) -Math.cos(yr);
        float rz = (float) -Math.sin(yr);
        // Pulled in toward the shooter while a prop is in the way, or the
        // replay is a close-up of the inside of a container.
        float back = 2.6f;
        int eye = p.y() + 1950;
        // tested 0.4 m past the camera, in the rules' millimetres (x mirrored)
        while (back > 0.6f && !m.arena().clear(p.x(), eye, p.z(),
                (int) (-(x - fx * (back + 0.4f) + rx * 0.7f) * 1000f), eye,
                (int) ((z - fz * (back + 0.4f) + rz * 0.7f) * 1000f))) {
            back -= 0.2f;
        }
        float cx = x - fx * back + rx * 0.7f;
        float cy = y + 1.95f;
        float cz = z - fz * back + rz * 0.7f;
        getCamera().setPerspective(HIP_FOV - 8f, NEAR, 400f);
        getCamera().setPosition(cx, cy, cz);
        getCamera().setTarget(x + fx * 9f, y + 1.2f, z + fz * 9f);
        sky.setPosition(cx, cy, cz);
        getLight().setShadowCenter(x + fx * SHADOW_AHEAD * 0.6f, 0f, z + fz * SHADOW_AHEAD * 0.6f);
        if (viewModel != null) {
            viewModel.setVisible(false);
        }
    }

    static final int DRAMA_PLAIN = 0;
    static final int DRAMA_STAGGER = 1;
    static final int DRAMA_ROLL = 2;
    static final int DRAMA_SLOW = 3;

    /// How this operator takes being painted this time. Half the time it is
    /// a plain fall; the rest make a scene. Chosen from who and when, so a
    /// replay of the same hit plays the same performance.
    static int drama(int who, int paintedAt) {
        int r = (who * 7 + paintedAt * 13) & 7;
        return r < 4 ? DRAMA_PLAIN : r < 6 ? DRAMA_STAGGER : r < 7 ? DRAMA_ROLL : DRAMA_SLOW;
    }

    /// The clip a painted operator plays, s seconds after the hit.
    static String performance(int who, int paintedAt, float s) {
        switch (drama(who, paintedAt)) {
            case DRAMA_STAGGER:
                // clutches the chest, staggers the other way, then goes down
                return s < 0.7f ? Models.HIT : s < 1.5f ? Models.HIT2 : Models.DEATH;
            case DRAMA_ROLL:
                // a dive for cover that is a second too late
                return s < 0.9f ? Models.ROLL : Models.DEATH;
            default:
                return Models.DEATH;
        }
    }

    /// The line a drama queen delivers, or null for a quiet fall.
    static String complaint(int who, int paintedAt) {
        int d = drama(who, paintedAt);
        if (d == DRAMA_PLAIN) {
            return null;
        }
        String[] lines = d == DRAMA_STAGGER
                ? new String[] {"TELL MY WIFE... I SPLATTED BRAVELY", "IT'S IN MY EYES! MY BEAUTIFUL EYES!",
                    "I SEE A LIGHT... IT'S MAGENTA"}
                : d == DRAMA_ROLL
                ? new String[] {"I'D LIKE TO SEE THAT AGAIN, REF", "THAT WAS A BOUNCE. EVERYONE SAW IT.",
                    "THE WIND TOOK IT!"}
                : new String[] {"NOOOOOOOOOO", "WHY... ME...", "AVENGE... MY... HOODIE..."};
        return lines[(who + paintedAt) % lines.length];
    }

    /// Game over: the winners cheer, the losers stand there.
    private static String celebration(Match m, int i) {
        int w = m.winner();
        if (w < 0) {
            // a draw: everyone waves at the ref, everyone claims it
            return Models.WAVE;
        }
        if (m.player(i).team() != w) {
            return Models.IDLE;
        }
        int beat = (m.tick() / (Match.TICK_HZ / 2) + i) % 6;
        return (i & 1) == 0 ? beat < 3 ? Models.PUNCH_R : Models.WAVE : beat < 3 ? Models.PUNCH_L : Models.KICK;
    }

    private boolean replaying(Match m) {
        return m != driver.match();
    }

    private float orbit;

    /// Game over: a slow orbit around the winning team, close enough to see
    /// them cheer -- or around everyone, on a draw.
    private void celebrate(Match m, int team, float dt) {
        float cx = 0f;
        float cz = 0f;
        float cy = 0f;
        int n = 0;
        for (int i = 0; i < m.playerCount(); i++) {
            Player p = m.player(i);
            if (team < 0 || p.team() == team) {
                cx += -p.x() * MM;
                cy += p.y() * MM;
                cz += p.z() * MM;
                n++;
            }
        }
        if (n == 0) {
            camera(m, driver.me(), dt);
            return;
        }
        cx /= n;
        cy /= n;
        cz /= n;
        orbit += dt * 0.35f;
        float ex = cx + (float) Math.sin(orbit) * 5.5f;
        float ez = cz + (float) Math.cos(orbit) * 5.5f;
        getCamera().setPerspective(HIP_FOV - 10f, NEAR, 400f);
        getCamera().setPosition(ex, cy + 2.2f, ez);
        getCamera().setTarget(cx, cy + 1.0f, cz);
        sky.setPosition(ex, cy + 2.2f, ez);
        getLight().setShadowCenter(cx, 0f, cz);
        if (viewModel != null) {
            viewModel.setVisible(false);
        }
    }

    /// The replay's later angles: a fixed camera, as a broadcast would cut to.
    /// The side angle stands 4 m off the line from shooter to victim, so the
    /// ball crosses the frame; the victim angle is 2.2 m in front of the
    /// victim at chest height, for the performance.
    private void angle(Match m, int s, int v, int which) {
        Player a = m.player(s);
        Player b = m.player(v);
        float ax = -lerp(prevX[s], a.x() * MM);
        float az = lerp(prevZ[s], a.z() * MM);
        float bx = -lerp(prevX[v], b.x() * MM);
        float by = lerp(prevY[v], b.y() * MM);
        float bz = lerp(prevZ[v], b.z() * MM);
        float dx = bx - ax;
        float dz = bz - az;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01f) {
            dx = 0f;
            dz = 1f;
        } else {
            dx /= len;
            dz /= len;
        }
        float cx;
        float cy;
        float cz;
        float tx;
        float tz;
        if (which == ANGLE_SIDE) {
            cx = (ax + bx) * 0.5f - dz * 4f;
            cz = (az + bz) * 0.5f + dx * 4f;
            cy = by + 1.4f;
            tx = bx - dx * 0.8f;
            tz = bz - dz * 0.8f;
        } else {
            cx = bx - dx * 2.2f + dz * 0.4f;
            cz = bz - dz * 2.2f - dx * 0.4f;
            cy = by + 1.35f;
            tx = bx;
            tz = bz;
        }
        // A prop between the camera and the victim: try the other side of the
        // line, then closer in, and take the first view that sees them. A
        // broadcast never cuts to the inside of a container.
        int vx = b.x();
        int vy = b.y() + 1300;
        int vz = b.z();
        float ox = cx - bx;
        float oz = cz - bz;
        float[] tries = {1f, -1f, 0.6f, -0.6f, 0.35f, -0.35f};
        for (int k = 0; k < tries.length; k++) {
            float f = tries[k];
            // a negative try mirrors the camera across the shooter-victim line
            float px = ox;
            float pz = oz;
            if (f < 0) {
                float along = ox * dx + oz * dz;
                px = 2 * along * dx - ox;
                pz = 2 * along * dz - oz;
                f = -f;
            }
            float tx2 = bx + px * f;
            float tz2 = bz + pz * f;
            if (m.arena().clear(vx, vy, vz, (int) (-tx2 * 1000f), (int) (cy * 1000f), (int) (tz2 * 1000f))) {
                cx = tx2;
                cz = tz2;
                break;
            }
        }
        getCamera().setPerspective(which == ANGLE_VICTIM ? 42f : HIP_FOV - 14f, NEAR, 400f);
        getCamera().setPosition(cx, cy, cz);
        getCamera().setTarget(tx, by + 1.1f, tz);
        sky.setPosition(cx, cy, cz);
        getLight().setShadowCenter(bx, 0f, bz);
        if (viewModel != null) {
            viewModel.setVisible(false);
        }
    }

    /// High above the field, north up, drifting after the player: the whole
    /// fight in one picture.
    private void overhead(Match m, int me, float dt) {
        Player p = m.player(me);
        float x = -lerp(prevX[me], p.x() * MM);
        float z = lerp(prevZ[me], p.z() * MM);
        if (!overSet) {
            overX = x;
            overZ = z;
            overSet = true;
        }
        float k = Math.min(1f, dt * 2.5f);
        overX += (x - overX) * k;
        overZ += (z - overZ) * k;
        getCamera().setPerspective(52f, 0.5f, 400f);
        getCamera().setPosition(overX, 25f, overZ - 15f);
        getCamera().setTarget(overX, 0f, overZ + 1.5f);
        sky.setPosition(overX, 25f, overZ - 15f);
        getLight().setShadowCenter(overX, 0f, overZ + 2f);
        if (viewModel != null) {
            viewModel.setVisible(false);
        }
    }

    private void camera(Match m, int me, float dt) {
        Player p = m.player(me);
        float x = -lerp(prevX[me], p.x() * MM);
        float z = lerp(prevZ[me], p.z() * MM);
        float eye = lerp(prevY[me], p.y() * MM) + p.eyeY() * MM - p.y() * MM;
        // Your own view turns with your hand, not with the tick: the look you are
        // dragging is drawn now, a tick before the rules have it.
        int yaw = driver.interactive() && p.alive() ? lookYaw : p.yaw();
        int pitch = driver.interactive() && p.alive() ? lookPitch : p.pitch();
        knock = Math.max(0f, knock - dt * 1.6f);
        if (!p.alive()) {
            // Painted: the head snaps back, then the view sinks and looks at
            // the floor in shame.
            float sink = 1f - knock * knock;
            eye -= 0.5f * sink;
            pitch = (int) (-500 * sink + 260 * knock * knock);
        }
        double yr = yaw * 2 * Math.PI / Trig.FULL;
        double pr = pitch * 2 * Math.PI / Trig.FULL;
        float dx = (float) (-Math.sin(yr) * Math.cos(pr));
        float dy = (float) Math.sin(pr);
        float dz = (float) (Math.cos(yr) * Math.cos(pr));
        boolean aiming = p.alive() && p.input().has(Input.AIM);
        ads += ((aiming ? 1f : 0f) - ads) * Math.min(1f, dt * 14f);
        getCamera().setPerspective(HIP_FOV + (ADS_FOV - HIP_FOV) * ads + knock * knock * 14f, NEAR, 400f);
        getCamera().setPosition(x, eye, z);
        sky.setPosition(x, eye, z);
        getCamera().setTarget(x + dx, eye + dy, z + dz);
        // the shadow map covers what is in front of the eye, not behind it
        getLight().setShadowCenter(x + dx * SHADOW_AHEAD, 0f, z + dz * SHADOW_AHEAD);
        if (viewModel == null) {
            return;
        }
        recoil = Math.max(0f, recoil - dt * 9f);
        // Sway lags the look; bob follows the feet.
        float turn = Trig.wrap(yaw - lastYaw + Trig.FULL / 2) - Trig.FULL / 2;
        swayX += (-turn * 0.0004f - swayX) * Math.min(1f, dt * 10f);
        swayY += ((pitch - lastPitch) * 0.0004f - swayY) * Math.min(1f, dt * 10f);
        lastYaw = yaw;
        lastPitch = pitch;
        Input in = p.input();
        boolean moving = in.forward != 0 || in.strafe != 0;
        if (moving && p.grounded()) {
            bob += dt * (in.has(Input.SPRINT) ? 14f : 9f);
        }
        float hip = 1f - ads;
        float bobX = (float) Math.sin(bob) * 0.012f * hip;
        float bobY = (float) Math.abs(Math.cos(bob)) * 0.014f * hip;
        // The eye's own frame: right, up and forward.
        float rx = (float) -Math.cos(yr);
        float rz = (float) -Math.sin(yr);
        float ux = (float) (Math.sin(yr) * Math.sin(pr));
        float uy = (float) Math.cos(pr);
        float uz = (float) (-Math.cos(yr) * Math.sin(pr));
        float right = 0.15f * hip + bobX + swayX;
        float up = -0.13f * hip - 0.06f * ads + bobY + swayY - 0.02f * recoil;
        float ahead = 0.42f - 0.06f * ads - 0.07f * recoil;
        viewModel.setPosition(x + dx * ahead + rx * right + ux * up, eye + dy * ahead + uy * up,
                z + dz * ahead + rz * right + uz * up);
        // the barrel's end, where the flash goes
        viewMuzzleX = x + dx * (ahead + MARKER_LENGTH) + rx * right + ux * up;
        viewMuzzleY = eye + dy * (ahead + MARKER_LENGTH) + uy * up;
        viewMuzzleZ = z + dz * (ahead + MARKER_LENGTH) + rz * right + uz * up;
        float yawDeg = -yaw * 360f / Trig.FULL;
        float pitchDeg = -pitch * 360f / Trig.FULL - recoil * 6f;
        viewModel.setRotation(pitchDeg, yawDeg + 180f, 0f);
        viewModel.setScale(viewScale);
        viewModel.setVisible(p.alive());
    }

    private float lerp(float a, float b) {
        // A teleport (a respawn) is not smoothed across the field.
        if (Math.abs(b - a) > 2f) {
            return b;
        }
        return a + (b - a) * alpha;
    }

    private static int dim(int rgb, float k) {
        int r = (int) (((rgb >> 16) & 0xff) * k);
        int g = (int) (((rgb >> 8) & 0xff) * k);
        int b = (int) ((rgb & 0xff) * k);
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }
}
