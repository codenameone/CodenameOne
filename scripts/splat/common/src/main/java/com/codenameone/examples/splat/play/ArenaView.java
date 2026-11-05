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
import com.codename1.ui.CN;
import com.codename1.ui.Display;
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
    private static final float OPERATOR_HEIGHT = 1.8f;
    private static final float MARKER_LENGTH = 0.28f;

    // Key codes the touch buttons press, read with the keyboard's.
    static final int KEY_FIRE = 'f';
    static final int KEY_JUMP = ' ';
    static final int KEY_CROUCH = 'c';
    static final int KEY_SPRINT = 'e';
    static final int KEY_RELOAD = 'r';
    static final int KEY_THROW = 'g';
    static final int KEY_WIPE = 'x';
    static final int KEY_AIM = 'q';

    private final Driver driver;
    private Hud hud;
    private GraphicsDevice device;
    private Mesh sphere;
    private Model sky;
    private boolean controlsHidden;
    private Mesh quad;
    private Material[] teamPaint;
    private AnimatedModel[] operators = new AnimatedModel[0];
    private AnimatedModel[] held = new AnimatedModel[0];
    private float[] opScale = new float[0];
    private float[] heldScale = new float[0];
    private String[] clips = new String[0];
    private final Model[] balls = new Model[MAX_BALLS];
    private final Model[] decals = new Model[MAX_DECALS];
    private int nextDecal;
    // Effects. Every one is a pool of soft billboards drawn in one call.
    private final ParticleEmitter[] spray = new ParticleEmitter[2];
    private final ParticleEmitter[] mist = new ParticleEmitter[2];
    private ParticleEmitter dust;
    private ParticleEmitter flash;
    private ParticleEmitter motes;
    private float moteClock;
    private int moteSeed = 0x2545f491;
    private final AnimatedModel[] flags = new AnimatedModel[2];
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
        getCamera().setPerspective(HIP_FOV, 0.03f, 400f);
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
        float w = arena.width * MM;
        float dpt = arena.depth * MM;
        addModel(flat(new Material(Material.Type.PBR).setColor(0xff4a4d50).setRoughness(0.92f), -w / 2f, 0f, dpt / 2f,
                w, dpt));
        addModel(flat(new Material(Material.Type.PBR).setColor(0xff55683a).setRoughness(1f), -w / 2f, -0.02f, dpt / 2f,
                400f, 400f));
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
            for (int t = 0; t < 2; t++) {
                flags[t] = Models.create(d, "sign");
                fit(flags[t], 2.4f);
                flags[t].setTint(TEAM_COLORS[t]);
                flags[t].addTo(this);
            }
        }

        int n = match.playerCount();
        operators = new AnimatedModel[n];
        held = new AnimatedModel[n];
        opScale = new float[n];
        heldScale = new float[n];
        clips = new String[n];
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
            op.addTo(this);
            operators[i] = op;
            AnimatedModel m = Models.create(d, Models.marker(p.marker.id));
            heldScale[i] = length(m, MARKER_LENGTH * 0.8f);
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
            decals[i] = new Model(sphere, teamPaint[0]);
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

    private Model flat(Material m, float x, float y, float z, float sx, float sz) {
        Model model = new Model(quad, m);
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
        float cx = -(box.x0 + box.x1) * MM / 2f;
        float cz = (box.z0 + box.z1) * MM / 2f;
        float deg = (turn ? 90f : 0f) + ((box.x0 + box.x1) / 2 > arena.width / 2 ? 180f : 0f);
        place(m, s, deg, cx, cz, b);
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
        float big = CN.convertToPixels(8);
        float small = CN.convertToPixels(5.5f);
        buttons = new VirtualButton[] {
            button(c, KEY_FIRE, "FIRE", big, gap),
            button(c, KEY_JUMP, "JUMP", small, gap + big * 2.1f),
            button(c, KEY_THROW, "NADE", small, gap + big * 2.1f + small * 2.3f),
            // aiming while you are in, wiping the paint off while you are out
            button(c, KEY_AIM, "ADS", small, gap + big * 2.1f + small * 4.6f),
        };
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
        buttons[3].setLabel(isOut(m) ? "WIPE" : "ADS");
    }

    /// True while your own player is painted and waiting to come back.
    private boolean isOut(Match m) {
        int me = driver.me();
        return me >= 0 && me < m.playerCount() && !m.player(me).alive();
    }

    private static VirtualButton button(TouchControls c, int key, String label, float r, float margin) {
        VirtualButton b = c.addButton(key, r, TouchControls.RIGHT, TouchControls.BOTTOM, margin);
        b.setLabel(label);
        // dark glass with a light label: readable over sky and asphalt alike
        b.setColor(key == KEY_FIRE ? 0xb0b8861e : 0x8c0c0e10);
        b.setLabelColor(0xf2eef1f4);
        return b;
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
            driver.step(!driver.interactive() ? Input.IDLE : autopilot != null
                    ? com.codenameone.examples.splat.rules.Bot.think(driver.match(), driver.match().player(driver.me()), autopilot)
                    : input());
            accumulator -= TICK;
            steps++;
        }
        alpha = (float) Math.min(1, accumulator / TICK);
        events();
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
        for (int i = 0; i < happened.size(); i++) {
            Event e = happened.get(i);
            if (e.kind == Event.SPLAT || e.kind == Event.GRENADE) {
                decal(e.b & 1, e.x, e.y, e.z, e.kind == Event.GRENADE ? 2.6f : 0.55f);
                burstImpact(e.b & 1, e.x, e.y, e.z, e.kind == Event.GRENADE);
                if (near(e.x, e.z)) {
                    Sfx.splat();
                }
            } else if (e.kind == Event.PAINTED) {
                Player shooter = driver.match().player(e.a);
                decal(shooter.team(), e.x, e.y, e.z, 0.7f);
                burstImpact(shooter.team() & 1, e.x, e.y, e.z, false);
                Sfx.whistle();
                if (e.a == driver.me()) {
                    Sfx.hit();
                }
            } else if (e.kind == Event.SHOT) {
                if (e.a == driver.me() && driver.interactive()) {
                    recoil = 1f;
                    Sfx.pop();
                    // your own flash sits at the marker you hold, not the ball
                    flash.burst(viewMuzzleX, viewMuzzleY, viewMuzzleZ, 3);
                } else if (near(e.x, e.z)) {
                    flash.burst(-e.x * MM, e.y * MM, e.z * MM, 3);
                }
            } else if (e.kind == Event.WIPE_CAUGHT) {
                Sfx.whistle();
            } else if (e.kind == Event.RESPAWN && e.a == driver.me()) {
                aimed = false;
            }
            if (hud != null) {
                hud.event(e);
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
        for (int t = 0; t < 2; t++) {
            spray[t].update(dt, c);
            mist[t].update(dt, c);
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
    private void decal(int team, int x, int y, int z, float size) {
        Model d = decals[nextDecal];
        nextDecal = (nextDecal + 1) % MAX_DECALS;
        d.setMaterial(teamPaint[team]);
        float s = size * (0.8f + ((x ^ z) & 7) / 20f);
        float flat = 0.03f;
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
        if (hit == null || y < 120) {
            d.setScale(s, flat, s);
            py = 0.015f;
        } else {
            int dx0 = Math.abs(x - hit.x0);
            int dx1 = Math.abs(x - hit.x1);
            int dz0 = Math.abs(z - hit.z0);
            int dz1 = Math.abs(z - hit.z1);
            int dTop = Math.abs(y - hit.height);
            int best = Math.min(Math.min(dx0, dx1), Math.min(Math.min(dz0, dz1), dTop));
            if (best == dTop) {
                d.setScale(s, flat, s);
                py = hit.height * MM + 0.015f;
            } else if (best == dx0 || best == dx1) {
                d.setScale(flat, s, s);
                px = -(best == dx0 ? hit.x0 - 15 : hit.x1 + 15) * MM;
            } else {
                d.setScale(s, s, flat);
                pz = (best == dz0 ? hit.z0 - 15 : hit.z1 + 15) * MM;
            }
        }
        d.setPosition(px, py, pz);
        d.setVisible(true);
    }

    private void draw(float dt) {
        Match m = driver.match();
        int me = driver.me();
        for (int i = 0; i < operators.length && i < m.playerCount(); i++) {
            operator(m, i, i == me && driver.interactive(), dt);
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
        if (m.mode() == Mode.CAPTURE_THE_FLAG && flags[0] != null) {
            for (int t = 0; t < 2; t++) {
                flags[t].setPosition(-m.flagX(t) * MM, m.flagY(t) * MM, m.flagZ(t) * MM);
            }
        }
        camera(m, me, dt);
        if (hud != null) {
            hud.frame(m, me, lookYaw, lookPitch);
        }
    }

    /// One operator: where the rules put them, facing their look, playing the
    /// clip their motion asks for, the marker in the right hand.
    private void operator(Match m, int i, boolean first, float dt) {
        Player p = m.player(i);
        AnimatedModel op = operators[i];
        AnimatedModel gun = held[i];
        op.setVisible(!first);
        gun.setVisible(!first && p.alive());
        if (first) {
            return;
        }
        float x = -lerp(prevX[i], p.x() * MM);
        float y = lerp(prevY[i], p.y() * MM);
        float z = lerp(prevZ[i], p.z() * MM);
        double yr = p.yaw() * 2 * Math.PI / Trig.FULL;
        float yawDeg = (float) -Math.toDegrees(yr);
        // Motion in the operator's own frame, from the last tick's step.
        float vx = -(p.x() * MM - prevX[i]);
        float vz = p.z() * MM - prevZ[i];
        float fwd = (float) (vx * -Math.sin(yr) + vz * Math.cos(yr));
        float side = (float) (vx * -Math.cos(yr) + vz * -Math.sin(yr));
        boolean firing = p.input().has(Input.FIRE);
        String want;
        if (!p.alive()) {
            want = p.input().has(Input.WIPE) ? Models.WAVE : Models.DEATH;
        } else if (Math.abs(fwd) + Math.abs(side) < 0.02f) {
            want = firing ? Models.IDLE_SHOOT : Models.IDLE;
        } else if (Math.abs(fwd) >= Math.abs(side)) {
            want = fwd < 0 ? Models.RUN_BACK : firing ? Models.RUN_SHOOT
                    : p.crouched() || p.input().has(Input.AIM) ? Models.WALK : Models.RUN;
        } else {
            want = side > 0 ? Models.RUN_RIGHT : Models.RUN_LEFT;
        }
        if (!want.equals(clips[i])) {
            clips[i] = want;
            op.crossFade(want, Models.DEATH.equals(want) ? 0.08f : 0.18f, !Models.DEATH.equals(want));
        }
        op.update(dt);
        op.setPosition(x, y - (p.crouched() ? 0.25f : 0f), z);
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

    private void camera(Match m, int me, float dt) {
        Player p = m.player(me);
        float x = -lerp(prevX[me], p.x() * MM);
        float z = lerp(prevZ[me], p.z() * MM);
        float eye = lerp(prevY[me], p.y() * MM) + p.eyeY() * MM - p.y() * MM;
        // Your own view turns with your hand, not with the tick: the look you are
        // dragging is drawn now, a tick before the rules have it.
        int yaw = driver.interactive() && p.alive() ? lookYaw : p.yaw();
        int pitch = driver.interactive() && p.alive() ? lookPitch : p.pitch();
        if (!p.alive()) {
            // Painted: the view sinks and looks at the floor in shame.
            eye -= 0.5f;
            pitch = -500;
        }
        double yr = yaw * 2 * Math.PI / Trig.FULL;
        double pr = pitch * 2 * Math.PI / Trig.FULL;
        float dx = (float) (-Math.sin(yr) * Math.cos(pr));
        float dy = (float) Math.sin(pr);
        float dz = (float) (Math.cos(yr) * Math.cos(pr));
        boolean aiming = p.alive() && p.input().has(Input.AIM);
        ads += ((aiming ? 1f : 0f) - ads) * Math.min(1f, dt * 14f);
        getCamera().setPerspective(HIP_FOV + (ADS_FOV - HIP_FOV) * ads, 0.03f, 400f);
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
