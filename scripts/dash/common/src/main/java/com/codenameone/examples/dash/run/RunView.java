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
package com.codenameone.examples.dash.run;

import com.codename1.gaming.GameCamera;
import com.codename1.gaming.GameInput;
import com.codename1.gaming.GameView;
import com.codename1.gaming.Model;
import com.codename1.gaming.ParticleEmitter;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Texture;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codenameone.examples.dash.rules.Clearance;
import com.codenameone.examples.dash.rules.Input;
import com.codenameone.examples.dash.rules.PickupKind;
import com.codenameone.examples.dash.rules.Run;
import com.codenameone.examples.dash.rules.Runner;
import com.codenameone.examples.dash.rules.Track;
import com.codenameone.examples.dash.rules.ViewMode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/// Draws a run in 3D, and -- for a stretch a level cues as `SIDE_2D` -- as a
/// flat side-scroller.
///
/// There is one scene and one simulation. "Faux 2D" is a camera: off to the
/// side, low and looking across the track, so the runner moves left to right
/// against the skyline and the track reads as a platformer.
/// When the runner reaches a cue, the camera's position, target and field of
/// view are interpolated, over the cue's blend distance, to the chase view
/// behind the runner. Nothing the player does means anything different
/// either side of the swing: [Track#viewAt] is for the renderer, and the
/// rules never read it.
///
/// World units: one per metre. The track runs toward -z; lane 0 is on the
/// left.
// tag::dash-run-view[]
public class RunView extends GameView {
    /// What the run screen wants to hear: a tick happened, or the run ended.
    public interface Hud {
        void ticked(Run run);

        void ended(Run run);
    }

    public static final double TICK = 1.0 / Run.TICKS_PER_SECOND;
    private static final int MAX_STEPS_PER_FRAME = 8;
    private static final float LANE_W = 1.5f;
    private static final float SEGMENT = 10f;
    private static final int SEGMENTS = 16;
    private static final float AHEAD = 90f;
    private static final float BEHIND = 12f;
    private static final float RUNNER_HEIGHT = 1.7f;
    private static final int SWIPE = 24;
    private static final int TOWERS = 2;

    private final Driver driver;
    private Hud hud;
    private GraphicsDevice device;
    private Mesh cube;
    private Mesh quad;
    private Mesh sphere;
    private Mesh gemMesh;
    private Mesh coinMesh;
    private Glow glow;
    private Rig.Kit kit;
    private final Map<String, Mesh> shapes = new HashMap<String, Mesh>();
    private final Model[] floor = new Model[SEGMENTS];
    /// Raised, rounded curbs along both edges, one pair per segment with a
    /// gap between, so they tick past.
    private final Model[] curbs = new Model[SEGMENTS * 2];
    /// What does not change along the track travels with the runner: the
    /// neon tube on each curb, its halo, and its reflection on the floor.
    private final Model[] neon = new Model[6];
    /// Towers of the skyline in three layers. The near ones, TOWERS per
    /// segment with a setback crown and a beacon on top, and the middle ones
    /// stand on the left only, so the side-on camera, which sits out on the
    /// right, looks across the track at them; each layer is hazier than
    /// the one before it. The far layer has a row on the right too, past the
    /// side camera, where it is only ever seen from behind the runner.
    private final Model[] towers = new Model[SEGMENTS * TOWERS];
    /// The side of the causeway the track runs on. In the side view the
    /// city drops away below the track and this face fills the bottom of
    /// the frame, where a bare field of ground would otherwise be.
    private final Model[] decks = new Model[SEGMENTS];
    private Model ground;
    /// How far the city sinks below the track when seen from the side.
    private static final float DROP = 9f;
    private final Model[] crowns = new Model[SEGMENTS * TOWERS];
    private final Model[] beacons = new Model[SEGMENTS * TOWERS];
    private final Model[] midTowers = new Model[SEGMENTS];
    private final Model[] farTowers = new Model[SEGMENTS * 2];
    private final Model[] haze = new Model[3];
    /// Light gantries over the track, one every other segment: two legs, a
    /// rounded beam, the light tube under it, its halo and its pool of light
    /// on the floor.
    private static final int GANTRY_PARTS = 6;
    private final Model[] gantries = new Model[SEGMENTS / 2 * GANTRY_PARTS];
    private Model sky;
    private Model sun;
    private Model sunHalo;
    private Model sunCorona;
    private Theme theme;
    private Rig[] runners = new Rig[0];
    private Rig[] ghostModels = new Rig[0];
    private final Map<Integer, Model[]> entries = new HashMap<Integer, Model[]>();
    private ParticleEmitter streaks;
    private ParticleEmitter trail;
    private Material hurdleFrame;
    private Material hurdleBar;
    private Material hurdleHalo;
    private Material holo;
    private Material warning;
    private Material warningHalo;
    private Material pod;
    private Material podGlow;
    private Material podHalo;
    private Material pit;
    private Material pitRim;
    private Material pitHalo;
    private Material pitDepth;
    private Material coinMaterial;
    private Material coinGlint;
    private final Map<PickupKind, Material[]> gemMaterials = new HashMap<PickupKind, Material[]>();
    private int firstEntry;
    private ParticleEmitter sparks;
    private ParticleEmitter dust;
    private double accumulator;
    private float alpha;
    private float[] prevX = new float[0];
    private float[] prevY = new float[0];
    private float[] prevZ = new float[0];
    private int lastCoins;
    private int lastStumbles;
    private boolean lastAirborne;
    private boolean endedSent;
    private float time;
    private String[] runnerIds = new String[0];
    private int pressX = -1;
    private int pressY = -1;
    private final boolean[] keyWas = new boolean[4];

    public RunView(Driver driver) {
        this.driver = driver;
    }

    public void setHud(Hud hud) {
        this.hud = hud;
    }

    /// The shop id of each runner's character, for its model; missing ones
    /// get a crowd model.
    public void setRunnerIds(String[] ids) {
        runnerIds = ids == null ? new String[0] : ids;
    }

    public Driver driver() {
        return driver;
    }

    @Override
    protected void onSetup(GraphicsDevice d) {
        device = d;
        theme = Theme.of(driver.run().level().theme);
        setClearColor(theme.skyHorizon);
        getLight().setDirection(-0.5f, -1f, -0.35f).setColor(theme.light).setAmbientColor(theme.ambient);
        getCamera().setPerspective(60f, 0.5f, 400f);
        cube = Primitives.cube(d, 1f);
        quad = Primitives.quad(d, 1f);
        sphere = Primitives.sphere(d, 0.5f, 12, 18, false);
        gemMesh = Meshes.gem(0.5f, 1f);
        coinMesh = Meshes.coin(0.34f, 0.1f, 24);
        glow = new Glow(d);
        kit = new Rig.Kit(quad, glow);

        Texture skyTex = d.createTexture(4, 128, Theme.skyTexture(theme, 128));
        skyTex.setFilter(Texture.Filter.LINEAR);
        sky = new Model(Primitives.sphere(d, 0.5f, 16, 16, true), new Material(Material.Type.UNLIT).setTexture(skyTex));
        sky.setScale(700f);
        addModel(sky);
        Texture hazeTex = d.createTexture(4, 64, Theme.hazeTexture(theme));
        hazeTex.setFilter(Texture.Filter.LINEAR);
        Material hazeMaterial = Glow.blended(hazeTex, 0xffffffff);
        for (int i = 0; i < haze.length; i++) {
            haze[i] = Glow.sprite(quad, hazeMaterial, 0f, 28f, 0f, 900f, 66f);
            // Ahead, to the left, and to the right past the side camera.
            haze[i].setRotation(0f, i == 0 ? 0f : i == 1 ? 90f : -90f, 0f);
            addModel(haze[i]);
        }
        sun = new Model(sphere, new Material(Material.Type.UNLIT).setColor(theme.sun));
        sun.setScale(46f, 46f, 1f);
        addModel(sun);
        sunCorona = Glow.sprite(quad, Glow.additive(glow.soft, (theme.sun & 0x00ffffff) | 0xc0000000),
                0f, 0f, 0f, 120f, 120f);
        sunHalo = Glow.sprite(quad, Glow.additive(glow.soft, (theme.sun & 0x00ffffff) | 0x60000000),
                0f, 0f, 0f, 320f, 220f);
        addModel(sunHalo);
        addModel(sunCorona);

        int lanes = Math.max(1, driver.run().level().lanes);
        float half = lanes * LANE_W / 2f + 0.3f;
        Texture lane = d.createTexture(128, 128, Theme.laneTexture(theme, lanes));
        lane.setFilter(Texture.Filter.LINEAR);
        Material floorMaterial = new Material(Material.Type.PHONG).setTexture(lane).setShininess(70f);
        Mesh curb = Meshes.roundedBox(0.38f, 0.26f, SEGMENT - 0.25f, 0.1f, 3);
        Material curbMaterial = new Material(Material.Type.PHONG).setColor(Theme.shade(theme.tower, 0.55f))
                .setShininess(90f);
        for (int i = 0; i < SEGMENTS; i++) {
            Model m = new Model(quad, floorMaterial);
            m.setRotation(-90f, 0f, 0f);
            m.setScale(lanes * LANE_W + 0.6f, SEGMENT, 1f);
            floor[i] = m;
            addModel(m);
            for (int s = 0; s < 2; s++) {
                Model r = new Model(curb, curbMaterial);
                curbs[i * 2 + s] = r;
                addModel(r);
            }
        }
        Material tube = new Material(Material.Type.UNLIT).setColor(Theme.mix(theme.rail, 0xffffffff, 0.3f));
        Material tubeHalo = Glow.additive(glow.streak, (theme.rail & 0x00ffffff) | 0x90000000);
        Material tubeShine = Glow.additive(glow.streak, (theme.rail & 0x00ffffff) | 0x58000000);
        for (int s = 0; s < 2; s++) {
            neon[s] = box(cube, tube, 0f, 0f, 0f, 0.07f, 0.07f, AHEAD + BEHIND);
            neon[2 + s] = Glow.sprite(quad, tubeHalo, 0f, 0f, 0f, AHEAD + BEHIND, 0.7f);
            neon[2 + s].setRotation(0f, 90f, 0f);
            neon[4 + s] = Glow.sprite(quad, tubeShine, 0f, 0f, 0f, AHEAD + BEHIND, 1.5f);
            neon[4 + s].setRotation(-90f, 90f, 0f);
        }
        for (int i = 0; i < neon.length; i++) {
            addModel(neon[i]);
        }
        Texture deckTex = d.createTexture(64, 64, Theme.deckTexture(theme));
        deckTex.setFilter(Texture.Filter.LINEAR);
        Material deckMaterial = new Material(Material.Type.LAMBERT).setTexture(deckTex);
        for (int i = 0; i < decks.length; i++) {
            decks[i] = new Model(cube, deckMaterial);
            decks[i].setVisible(false);
            addModel(decks[i]);
        }
        ground = new Model(quad, new Material(Material.Type.LAMBERT).setColor(theme.ground));
        ground.setRotation(-90f, 0f, 0f);
        ground.setScale(800f, 800f, 1f);
        ground.setPosition(0f, -0.05f, 0f);
        ground.setUserData("ground");
        addModel(ground);
        Material nearTower = towerMaterial(d, 0.08f);
        Material midTower = towerMaterial(d, 0.42f);
        Material farTower = towerMaterial(d, 0.7f);
        Material beacon = Glow.additive(glow.soft, (theme.rail & 0x00ffffff) | 0xe0000000);
        for (int i = 0; i < towers.length; i++) {
            towers[i] = new Model(cube, nearTower);
            crowns[i] = new Model(cube, nearTower);
            beacons[i] = Glow.sprite(quad, beacon, 0f, 0f, 0f, 1.6f, 1.6f);
            addModel(towers[i]);
            addModel(crowns[i]);
            addModel(beacons[i]);
        }
        for (int i = 0; i < midTowers.length; i++) {
            midTowers[i] = new Model(cube, midTower);
            addModel(midTowers[i]);
        }
        for (int i = 0; i < farTowers.length; i++) {
            farTowers[i] = new Model(cube, farTower);
            addModel(farTowers[i]);
        }
        float span = half * 2f + 0.8f;
        Mesh legMesh = Meshes.roundedBox(0.3f, 4.6f, 0.3f, 0.1f, 3);
        Mesh beamMesh = Meshes.capsule(0.14f, span, 4, 12);
        Material leg = new Material(Material.Type.PHONG).setColor(Theme.shade(theme.tower, 0.6f)).setShininess(80f);
        Material beamLight = new Material(Material.Type.UNLIT).setColor(Theme.mix(theme.line, 0xffffffff, 0.3f));
        Material beamHalo = Glow.additive(glow.streak, (theme.line & 0x00ffffff) | 0xa0000000);
        Material pool = Glow.additive(glow.soft, (theme.line & 0x00ffffff) | 0x48000000);
        for (int g = 0; g < SEGMENTS / 2; g++) {
            int o = g * GANTRY_PARTS;
            gantries[o] = new Model(legMesh, leg);
            gantries[o + 1] = new Model(legMesh, leg);
            gantries[o + 2] = new Model(beamMesh, leg);
            gantries[o + 2].setRotation(0f, 0f, 90f);
            gantries[o + 3] = box(cube, beamLight, 0f, 0f, 0f, span - 0.4f, 0.06f, 0.08f);
            gantries[o + 4] = Glow.sprite(quad, beamHalo, 0f, 0f, 0f, span + 2.5f, 1.8f);
            gantries[o + 5] = Glow.sprite(quad, pool, 0f, 0f, 0f, span, 4f);
            gantries[o + 5].setRotation(-90f, 0f, 0f);
            for (int p = 0; p < GANTRY_PARTS; p++) {
                addModel(gantries[o + p]);
            }
        }

        hurdleFrame = new Material(Material.Type.PHONG).setColor(0xff2a2f3d).setShininess(80f);
        hurdleBar = new Material(Material.Type.UNLIT).setColor(0xffff6d88);
        hurdleHalo = Glow.additive(glow.streak, 0xc0ff3d6d);
        Texture holoTex = d.createTexture(64, 64, Theme.holoTexture(0xe0ffd23a, 0x5014141c));
        holoTex.setFilter(Texture.Filter.LINEAR);
        holo = Glow.blended(holoTex, 0xffffffff);
        warning = new Material(Material.Type.UNLIT).setColor(0xffff5a5a);
        warningHalo = Glow.additive(glow.streak, 0xc0ff3b3b);
        Texture podTex = d.createTexture(64, 64, Theme.podTexture(0xff9aa3c0, 0xff35e8ff));
        podTex.setFilter(Texture.Filter.LINEAR);
        pod = new Material(Material.Type.PHONG).setTexture(podTex).setShininess(110f);
        podGlow = new Material(Material.Type.UNLIT).setColor(0xff5af0ff);
        podHalo = Glow.additive(glow.soft, 0x9035e8ff);
        pit = new Material(Material.Type.UNLIT).setColor(0xff05050a);
        pitRim = new Material(Material.Type.UNLIT).setColor(0xffffb44a);
        pitHalo = Glow.additive(glow.streak, 0xd0ff8a2a);
        pitDepth = Glow.additive(glow.soft, 0x70ff5a1a);
        coinMaterial = new Material(Material.Type.PHONG).setColor(0xffffc93a).setShininess(128f);
        coinGlint = Glow.additive(glow.glint, 0xd0fff0b0);

        Run run = driver.run();
        runners = new Rig[run.runnerCount()];
        for (int i = 0; i < runners.length; i++) {
            String id = i < runnerIds.length ? runnerIds[i] : null;
            runners[i] = rig(Outfit.of(id, i == driver.me() ? 0 : i + 1));
        }
        Run[] ghosts = driver.ghosts();
        ghostModels = new Rig[ghosts.length];
        for (int i = 0; i < ghosts.length; i++) {
            ghostModels[i] = rig(Outfit.of(null, i + 2).ghost());
        }
        prevX = new float[runners.length + ghostModels.length];
        prevY = new float[prevX.length];
        prevZ = new float[prevX.length];
        remember();

        sparks = new ParticleEmitter(d, 160).setColor(0xffffd65a).setAdditive(true)
                .setSpeed(1.5f, 4f).setLifetime(0.15f, 0.4f).setSize(0.25f, 0.05f)
                .setGravity(-6f).setDrag(1f);
        dust = new ParticleEmitter(d, 120).setColor((Theme.mix(theme.ground, 0xffffffff, 0.6f) & 0x00ffffff) | 0x80000000).setAdditive(false)
                .setSpeed(0.6f, 1.8f).setLifetime(0.3f, 0.7f).setSize(0.3f, 0.8f)
                .setGravity(0.4f).setDrag(3f).setDirection(0f, 0.3f, 0f, 85f);
        streaks = new ParticleEmitter(d, 200).setColor((Theme.mix(theme.line, 0xffffffff, 0.5f) & 0x00ffffff) | 0x70000000).setAdditive(true)
                .setSpeed(26f, 40f).setLifetime(0.18f, 0.32f).setSize(0.07f, 0.02f)
                .setGravity(0f).setDrag(0f).setDirection(0f, 0f, 1f, 4f);
        trail = new ParticleEmitter(d, 160).setColor(runnerTrim()).setAdditive(true)
                .setSpeed(0.4f, 1.2f).setLifetime(0.2f, 0.45f).setSize(0.14f, 0.02f)
                .setGravity(0.5f).setDrag(2f).setDirection(0f, 0.2f, 1f, 50f);
        dust.addTo(this);
        sparks.addTo(this);
        streaks.addTo(this);
        trail.addTo(this);
        Sfx.load();
    }
    // end::dash-run-view[]

    private Rig rig(Outfit o) {
        Rig r = new Rig(kit, o);
        r.addTo(this);
        return r;
    }

    private Material towerMaterial(GraphicsDevice d, float hazeK) {
        Texture t = d.createTexture(Theme.TOWER_TEX_W, Theme.TOWER_TEX_H, Theme.towerTexture(theme, hazeK));
        t.setFilter(Texture.Filter.LINEAR);
        return new Material(hazeK > 0.5f ? Material.Type.UNLIT : Material.Type.LAMBERT).setTexture(t);
    }

    /// A rounded box or a capsule of the given size, built the first time an
    /// obstacle needs it and shared after that.
    private Mesh rounded(float w, float h, float d, float r) {
        String key = "b" + (int) (w * 100) + "," + (int) (h * 100) + "," + (int) (d * 100) + "," + (int) (r * 100);
        Mesh m = shapes.get(key);
        if (m == null) {
            m = Meshes.roundedBox(w, h, d, r, 3);
            shapes.put(key, m);
        }
        return m;
    }

    private Mesh capsule(float r, float len) {
        String key = "c" + (int) (r * 100) + "," + (int) (len * 100);
        Mesh m = shapes.get(key);
        if (m == null) {
            m = Meshes.capsule(r, Math.max(0.01f, len), 4, 12);
            shapes.put(key, m);
        }
        return m;
    }

    private int runnerTrim() {
        int me = driver.me();
        String id = me < runnerIds.length ? runnerIds[me] : null;
        return (Outfit.of(id, 0).trim & 0x00ffffff) | 0xb0000000;
    }

    // tag::dash-fixed-tick[]
    @Override
    protected void update(double dt) {
        if (device == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        time += (float) dt;
        input();
        if (driver.countdownTicks() > 0) {
            accumulator = 0;
        } else {
            accumulator += dt * driver.speedPercent() / 100.0;
        }
        int steps = 0;
        while (accumulator >= TICK && steps < MAX_STEPS_PER_FRAME) {
            if (!driver.canStep()) {
                // Online and waiting for the server: hold at the last tick.
                accumulator = Math.min(accumulator, TICK);
                break;
            }
            remember();
            driver.step();
            events();
            accumulator -= TICK;
            steps++;
        }
        alpha = (float) Math.min(1, accumulator / TICK);
        // end::dash-fixed-tick[]
        draw((float) dt);
        notifyHud(steps > 0);
    }

    /// Positions before a tick, so a frame can draw between two ticks.
    private void remember() {
        Run run = driver.run();
        for (int i = 0; i < runners.length; i++) {
            Runner r = run.runner(i);
            prevX[i] = laneX(r);
            prevY[i] = r.y / (float) Track.UNIT;
            prevZ[i] = r.z / (float) Track.UNIT;
        }
        Run[] ghosts = driver.ghosts();
        for (int i = 0; i < ghostModels.length && i < ghosts.length; i++) {
            Runner r = ghosts[i].runner(0);
            int k = runners.length + i;
            prevX[k] = laneX(r);
            prevY[k] = r.y / (float) Track.UNIT;
            prevZ[k] = r.z / (float) Track.UNIT;
        }
    }

    private float laneX(Runner r) {
        int lanes = Math.max(1, driver.run().level().lanes);
        return (r.laneX - (lanes - 1) * 500) / (float) Track.UNIT * LANE_W;
    }

    /// Sounds and particles from what changed in the tick.
    private void events() {
        Runner me = driver.run().runner(driver.me());
        float x = laneX(me);
        float z = -me.z / (float) Track.UNIT;
        if (me.coins != lastCoins) {
            sparks.burst(x, 1.2f + me.y / (float) Track.UNIT, z, 10);
            Sfx.coin();
        }
        if (me.stumbles != lastStumbles) {
            dust.burst(x, 0.2f, z, 18);
            if (me.alive) {
                Sfx.stumble();
            } else {
                Sfx.crash();
            }
        }
        boolean air = me.airborne();
        if (air && !lastAirborne) {
            Sfx.jump();
        } else if (!air && lastAirborne) {
            dust.burst(x, 0.1f, z, 8);
        }
        lastAirborne = air;
        lastCoins = me.coins;
        lastStumbles = me.stumbles;
    }

    private void draw(float dt) {
        Run run = driver.run();
        Runner me = run.runner(driver.me());
        float meZ = lerp(prevZ[driver.me()], me.z / (float) Track.UNIT);
        float meX = lerp(prevX[driver.me()], laneX(me));
        float meY = lerp(prevY[driver.me()], me.y / (float) Track.UNIT);

        for (int i = 0; i < runners.length; i++) {
            Runner r = run.runner(i);
            pose(runners[i], r, lerp(prevX[i], laneX(r)), lerp(prevY[i], r.y / (float) Track.UNIT),
                    lerp(prevZ[i], r.z / (float) Track.UNIT), dt);
        }
        Run[] ghosts = driver.ghosts();
        for (int i = 0; i < ghostModels.length && i < ghosts.length; i++) {
            Runner r = ghosts[i].runner(0);
            int k = runners.length + i;
            pose(ghostModels[i], r, lerp(prevX[k], laneX(r)), lerp(prevY[k], r.y / (float) Track.UNIT),
                    lerp(prevZ[k], r.z / (float) Track.UNIT), dt);
        }

        scenery(run, meX, meZ, dt);
        entries(run, me, meZ);
        camera(run, me, meX, meY, meZ);
        sparks.update(dt, getCamera());
        streaks.update(dt, getCamera());
        trail.update(dt, getCamera());
        dust.update(dt, getCamera());
    }

    private void pose(Rig m, Runner r, float x, float y, float z, float dt) {
        m.pose(x, y, -z, r.alive, r.finished, r.airborne(), r.sliding(), r.speed / 12000f, dt);
    }

    /// The floor, the rails, the skyline and the gantries leapfrog each
    /// other as the runner passes them; the sky and the sun travel with the
    /// camera so they stay at the horizon.
    private void scenery(Run run, float meX, float meZ, float dt) {
        int lanes = Math.max(1, run.level().lanes);
        float half = lanes * LANE_W / 2f + 0.3f;
        int first = (int) Math.floor((meZ - BEHIND) / SEGMENT);
        boolean side = run.track().viewAt(run.runner(driver.me()).z) == ViewMode.SIDE_2D
                && run.track().blendAt(run.runner(driver.me()).z) > 500;
        GameCamera cam = getCamera();
        // The city sinks as the camera swings out to the side, and rises
        // back as it swings in behind the runner; the chase camera never
        // strays more than a few metres off centre, so it sees none of it.
        float sink = Math.max(0f, Math.min(1f, (cam.getEyeX() - 6f) / 12f));
        float drop = DROP * sink;
        ground.setPosition(0f, -0.05f - drop, 0f);
        for (int i = 0; i < SEGMENTS; i++) {
            int seg = first + i;
            float cz = -(seg * SEGMENT + SEGMENT / 2f);
            decks[i].setVisible(sink > 0.02f);
            decks[i].setScale(0.4f, drop + 0.1f, SEGMENT);
            decks[i].setPosition(half + 0.5f, (0.1f - drop) / 2f, cz);
            floor[i].setPosition(0f, 0f, cz);
            curbs[i * 2].setPosition(-half - 0.12f, 0.12f, cz);
            curbs[i * 2 + 1].setPosition(half + 0.12f, 0.12f, cz);
            for (int t = 0; t < TOWERS; t++) {
                int k = i * TOWERS + t;
                // Lower from the side, so the skyline stops short of the top
                // of a portrait frame and leaves it sky.
                float h = (6f + jitter(seg, 31 + t) * 22f) * (1f - 0.5f * sink);
                float w = 3.5f + jitter(seg, 41 + t) * 4f;
                float tx = -half - 7f - t * 9f - jitter(seg, 3 + t) * 5f;
                float tz = -(seg * SEGMENT + jitter(seg, 5 + t) * SEGMENT);
                towers[k].setScale(w, h, w);
                towers[k].setPosition(tx, h / 2f - drop, tz);
                // A setback crown on most towers, narrower and offset.
                float ch = h * (0.2f + jitter(seg, 51 + t) * 0.3f);
                float cw = w * (0.45f + jitter(seg, 61 + t) * 0.25f);
                boolean crowned = jitter(seg, 71 + t) > 0.25f;
                crowns[k].setVisible(crowned);
                crowns[k].setScale(cw, ch, cw);
                crowns[k].setPosition(tx + (w - cw) * 0.25f, h + ch / 2f - drop, tz);
                float top = crowned ? h + ch : h;
                beacons[k].setVisible(jitter(seg, 81 + t) > 0.35f);
                beacons[k].setPosition(tx + (crowned ? (w - cw) * 0.25f : 0f), top + 0.5f - drop, tz);
                Glow.face(beacons[k], cam);
            }
            float mh = (14f + jitter(seg, 91) * 32f) * (1f - 0.4f * sink);
            float mw = 6f + jitter(seg, 92) * 8f;
            midTowers[i].setScale(mw, mh, mw);
            midTowers[i].setPosition(-half - 32f - jitter(seg, 93) * 12f, mh / 2f - drop,
                    -(seg * SEGMENT + jitter(seg, 94) * SEGMENT));
            for (int f = 0; f < 2; f++) {
                float fh = 24f + jitter(seg, 101 + f) * 50f;
                float fw = 9f + jitter(seg, 103 + f) * 12f;
                // The right-hand row stands past x = 88, behind the side
                // camera, so it never blocks the side-scroller view.
                float fx = f == 0 ? -half - 62f - jitter(seg, 105) * 25f : 88f + jitter(seg, 106) * 25f;
                farTowers[i * 2 + f].setScale(fw, fh, fw);
                farTowers[i * 2 + f].setPosition(fx, fh / 2f - drop, -(seg * SEGMENT + jitter(seg, 107 + f) * SEGMENT));
            }
        }
        float mid = -meZ - (AHEAD - BEHIND) / 2f;
        for (int s = 0; s < 2; s++) {
            float sx = s == 0 ? -1f : 1f;
            neon[s].setPosition(sx * (half + 0.12f), 0.27f, mid);
            neon[2 + s].setPosition(sx * (half + 0.12f), 0.3f, mid);
            neon[4 + s].setPosition(sx * (half - 0.45f), 0.012f, mid);
        }
        for (int g = 0; g < SEGMENTS / 2; g++) {
            int seg = first + g * 2 + (first & 1);
            float gz = -(seg * SEGMENT);
            int o = g * GANTRY_PARTS;
            gantries[o].setPosition(-half - 0.45f, 2.3f, gz);
            gantries[o + 1].setPosition(half + 0.45f, 2.3f, gz);
            gantries[o + 2].setPosition(0f, 4.6f, gz);
            gantries[o + 3].setPosition(0f, 4.43f, gz);
            gantries[o + 4].setPosition(0f, 4.45f, gz + 0.1f);
            gantries[o + 5].setPosition(0f, 0.015f, gz);
            // Seen from the side, the near leg would sweep across the runner.
            for (int p = 0; p < GANTRY_PARTS; p++) {
                gantries[o + p].setVisible(!side);
            }
        }
        sky.setPosition(cam.getEyeX(), 0f, cam.getEyeZ());
        float sunZ = -meZ - 300f;
        sun.setPosition(0f, 22f, sunZ);
        sunCorona.setPosition(0f, 22f, sunZ + 2f);
        sunHalo.setPosition(0f, 20f, sunZ + 4f);
        haze[0].setPosition(0f, 26f, -meZ - 250f);
        haze[1].setPosition(-150f, 26f, -meZ - 60f);
        haze[2].setPosition(150f, 26f, -meZ - 60f);
        Runner me = run.runner(driver.me());
        if (me.alive && !me.finished && me.speed > 0) {
            float fast = Math.min(1f, me.speed / 16000f);
            int n = (int) (fast * 3f + (Math.random() < fast ? 1 : 0));
            for (int i = 0; i < n; i++) {
                float a = (float) (Math.random() * Math.PI * 2);
                float rr = 2f + (float) Math.random() * 3f;
                streaks.burst(meX + (float) Math.cos(a) * rr, 1.6f + (float) Math.sin(a) * rr * 0.6f,
                        -meZ - 14f - (float) Math.random() * 10f, 1);
            }
            float y = me.y / (float) Track.UNIT;
            trail.burst(meX, 0.15f + y, -meZ + 0.25f, me.airborne() ? 1 : 2);
        }
    }

    /// Obstacles and pickups from just behind the camera to the horizon:
    /// models are made as entries come into view and dropped behind.
    private void entries(Run run, Runner me, float meZ) {
        Track track = run.track();
        while (firstEntry < track.size()) {
            Track.Entry e = track.get(firstEntry);
            if ((e.z + e.depth) / (float) Track.UNIT < meZ - BEHIND) {
                Model[] gone = entries.remove(Integer.valueOf(firstEntry));
                if (gone != null) {
                    remove(gone);
                }
                firstEntry++;
            } else {
                break;
            }
        }
        for (int i = firstEntry; i < track.size(); i++) {
            Track.Entry e = track.get(i);
            if (e.z / (float) Track.UNIT > meZ + AHEAD) {
                break;
            }
            Integer key = Integer.valueOf(i);
            Model[] m = entries.get(key);
            if (m == null) {
                m = model(run, e);
                entries.put(key, m);
                for (int p = 0; p < m.length; p++) {
                    addModel(m[p]);
                }
            }
            if (e.pickup != null) {
                boolean taken = me.took(e.index);
                for (int p = 0; p < m.length; p++) {
                    m[p].setVisible(!taken);
                }
                if (!taken) {
                    float bob = (float) Math.sin(time * 3f + i) * 0.12f;
                    for (int p = 0; p < m.length; p++) {
                        m[p].setPosition(m[p].getX(), 1.0f + bob, m[p].getZ());
                    }
                    boolean coin = e.pickup.kind == PickupKind.COIN;
                    m[0].setRotation(coin ? 90f : 0f, time * 140f + i * 20f, 0f);
                    // The halo faces the camera and, on a coin, twinkles.
                    Glow.face(m[1], getCamera());
                    float pulse = coin ? 0.75f + 0.35f * (float) Math.sin(time * 9f + i * 1.7f) : 1f;
                    m[1].setScale(coin ? pulse : 1.7f, coin ? pulse : 1.7f, 1f);
                }
            }
        }
        // Defensive: anything left over from before a restart.
        Iterator<Map.Entry<Integer, Model[]>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Model[]> en = it.next();
            if (en.getKey().intValue() < firstEntry) {
                remove(en.getValue());
                it.remove();
            }
        }
    }

    private void remove(Model[] parts) {
        for (int i = 0; i < parts.length; i++) {
            removeModel(parts[i]);
        }
    }

    /// The models for one obstacle or pickup. Each kind has a shape a
    /// player learns at a glance from any camera: a low bar with a glowing
    /// top to jump, striped boards hanging overhead to slide under, a cargo
    /// pod to steer around, and a black pit with hot edges.
    private Model[] model(Run run, Track.Entry e) {
        int lanes = Math.max(1, run.level().lanes);
        float width = e.spansAllLanes() ? lanes * LANE_W : LANE_W * 0.85f;
        float x = e.spansAllLanes() ? 0f : (e.lane * 1000 - (lanes - 1) * 500) / (float) Track.UNIT * LANE_W;
        float depth = Math.max(0.2f, e.depth / (float) Track.UNIT);
        float z = -(e.z / (float) Track.UNIT + depth / 2f);
        if (e.pickup != null) {
            float pz = -(e.z / (float) Track.UNIT);
            if (e.pickup.kind == PickupKind.COIN) {
                Model m = new Model(coinMesh, coinMaterial);
                m.setPosition(x, 1.0f, pz);
                return new Model[] {m, Glow.sprite(quad, coinGlint, x, 1.0f, pz + 0.05f, 1f, 1f)};
            }
            Material[] mats = gemMaterials.get(e.pickup.kind);
            if (mats == null) {
                int c = pickupColor(e.pickup.kind);
                mats = new Material[] {
                    new Material(Material.Type.PHONG).setColor(Theme.mix(c, 0xffffffff, 0.15f)).setShininess(110f),
                    Glow.additive(glow.soft, (c & 0x00ffffff) | 0xa0000000),
                };
                gemMaterials.put(e.pickup.kind, mats);
            }
            Model m = new Model(gemMesh, mats[0]);
            m.setScale(0.55f, 0.8f, 0.55f);
            m.setPosition(x, 1.0f, pz);
            return new Model[] {m, Glow.sprite(quad, mats[1], x, 1.0f, pz, 1f, 1f)};
        }
        Clearance c = e.obstacle.clearance;
        if (c == Clearance.JUMP && e.obstacle.height == 0) {
            float w = width + 0.1f;
            Model hole = box(quad, pit, x, 0.02f, z, w, depth, 1f);
            hole.setRotation(-90f, 0f, 0f);
            Model heat = box(quad, pitDepth, x, 0.03f, z, w * 0.9f, depth * 0.8f, 1f);
            heat.setRotation(-90f, 0f, 0f);
            Model near = box(capsule(0.06f, w - 0.12f), pitRim, x, 0.06f, z + depth / 2f, 1f, 1f, 1f);
            Model far = box(capsule(0.06f, w - 0.12f), pitRim, x, 0.06f, z - depth / 2f, 1f, 1f, 1f);
            near.setRotation(0f, 0f, 90f);
            far.setRotation(0f, 0f, 90f);
            Model nearHalo = box(quad, pitHalo, x, 0.04f, z + depth / 2f, w + 0.8f, 0.9f, 1f);
            Model farHalo = box(quad, pitHalo, x, 0.04f, z - depth / 2f, w + 0.8f, 0.9f, 1f);
            nearHalo.setRotation(-90f, 0f, 0f);
            farHalo.setRotation(-90f, 0f, 0f);
            return new Model[] {hole, heat, near, far, nearHalo, farHalo};
        }
        if (c == Clearance.JUMP) {
            float h = e.obstacle.height / (float) Track.UNIT;
            float post = 0.12f;
            Mesh postMesh = rounded(post, h, post, 0.05f);
            Model bar = box(capsule(0.075f, width - 0.15f), hurdleBar, x, h - 0.075f, z, 1f, 1f, 1f);
            bar.setRotation(0f, 0f, 90f);
            return new Model[] {
                box(postMesh, hurdleFrame, x - width / 2f + post, h / 2f, z, 1f, 1f, 1f),
                box(postMesh, hurdleFrame, x + width / 2f - post, h / 2f, z, 1f, 1f, 1f),
                box(rounded(width - post * 2f, h * 0.45f, 0.06f, 0.03f), hurdleFrame, x, h * 0.42f, z, 1f, 1f, 1f),
                bar,
                Glow.sprite(quad, hurdleHalo, x, h - 0.075f, z + 0.1f, width + 1f, 0.9f),
            };
        }
        if (c == Clearance.SLIDE) {
            float d = Math.min(depth, 0.3f);
            Model edge = box(capsule(0.05f, width - 0.1f), warning, x, 1.25f, z, 1f, 1f, 1f);
            edge.setRotation(0f, 0f, 90f);
            Model top = box(capsule(0.07f, width), hurdleFrame, x, 2.0f, z, 1f, 1f, 1f);
            top.setRotation(0f, 0f, 90f);
            Mesh postMesh = rounded(0.12f, 2.2f, 0.12f, 0.05f);
            return new Model[] {
                box(rounded(width, 0.7f, d, 0.06f), holo, x, 1.63f, z, 1f, 1f, 1f),
                edge,
                top,
                box(postMesh, hurdleFrame, x - width / 2f, 1.1f, z, 1f, 1f, 1f),
                box(postMesh, hurdleFrame, x + width / 2f, 1.1f, z, 1f, 1f, 1f),
                Glow.sprite(quad, warningHalo, x, 1.25f, z + d / 2f + 0.05f, width + 1f, 0.8f),
            };
        }
        float pw = width * 0.92f;
        Model under = box(quad, podHalo, x, 0.02f, z, pw * 1.5f, depth * 1.3f, 1f);
        under.setRotation(-90f, 0f, 0f);
        return new Model[] {
            box(rounded(pw, 2.1f, depth, 0.36f), pod, x, 1.4f, z, 1f, 1f, 1f),
            box(rounded(width * 0.8f, 0.32f, depth * 0.9f, 0.1f), hurdleFrame, x, 0.2f, z, 1f, 1f, 1f),
            box(rounded(pw + 0.04f, 0.08f, depth + 0.04f, 0.04f), podGlow, x, 0.62f, z, 1f, 1f, 1f),
            under,
            Glow.sprite(quad, podHalo, x, 1.9f, z + depth / 2f + 0.08f, pw * 1.1f, 0.9f),
        };
    }

    private static Model box(Mesh mesh, Material mat, float x, float y, float z, float sx, float sy, float sz) {
        Model m = new Model(mesh, mat);
        m.setScale(sx, sy, sz);
        m.setPosition(x, y, z);
        return m;
    }

    private static int pickupColor(PickupKind k) {
        switch (k) {
            case COIN:
                return 0xffffcc33;
            case GEM:
                return 0xff3ce0ff;
            case MAGNET:
                return 0xffff3d9a;
            case SHIELD:
                return 0xff8a7bff;
            default:
                return 0xffc8ff3d;
        }
    }

    // tag::dash-camera-blend[]
    /// Places the camera for `mode`, into `out`: eye x, y, z, target x, y, z,
    /// field of view, then the up vector's z (0 for upright, -1 looking down).
    private static void framing(ViewMode mode, float x, float y, float z, float[] out) {
        if (mode == ViewMode.SIDE_2D) {
            // Off to the right, at the deck's height and tilted up: the runner
            // a third in from the left with the track ahead in view, the deck
            // in the lower third, and open sky over the skyline. A portrait
            // frame is narrow along the track, so the camera stands well back
            // to see enough of it ahead.
            out[0] = 24f;
            out[1] = 0.9f + y * 0.3f;
            out[2] = -z - 1.3f;
            out[3] = 0f;
            out[4] = 4.6f + y * 0.3f;
            out[5] = -z - 1.3f;
            out[6] = 42f;
            out[7] = 0f;
        } else if (mode == ViewMode.TOP_DOWN) {
            out[0] = x * 0.4f;
            out[1] = 26f;
            out[2] = -z + 2f;
            out[3] = x * 0.4f;
            out[4] = 0f;
            out[5] = -z - 6f;
            out[6] = 40f;
            out[7] = -1f;
        } else {
            out[0] = x * 0.6f;
            out[1] = 3.4f + y * 0.5f;
            out[2] = -z + 6.5f;
            out[3] = x * 0.6f;
            out[4] = 1.0f + y * 0.3f;
            out[5] = -z - 9f;
            out[6] = 62f;
            out[7] = 0f;
        }
    }

    private final float[] from = new float[8];
    private final float[] to = new float[8];
    private final float[] blended = new float[8];

    private void camera(Run run, Runner me, float x, float y, float z) {
        Track t = run.track();
        int mz = me.z;
        float k = t.blendAt(mz) / 1000f;
        k = k * k * (3f - 2f * k);
        framing(t.previousViewAt(mz), x, y, z, from);
        framing(t.viewAt(mz), x, y, z, to);
        float[] v = blended;
        for (int i = 0; i < 8; i++) {
            v[i] = from[i] + (to[i] - from[i]) * k;
        }
        GameCamera cam = getCamera();
        cam.setPerspective(v[6], 0.5f, 400f);
        cam.setPosition(v[0], v[1], v[2]);
        cam.setTarget(v[3], v[4], v[5]);
        cam.setUp(0f, 1f + v[7], v[7]);
    }
    // end::dash-camera-blend[]

    private float lerp(float a, float b) {
        return a + (b - a) * alpha;
    }

    /// A stable pseudo-random 0..1 per track segment, for scenery: the same
    /// trees stand in the same places every run.
    private static float jitter(int seg, int salt) {
        int h = seg * 73856093 ^ salt * 19349663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return (h & 0xffff) / 65535f;
    }

    // tag::dash-controls[]
    /// Swipes and keys become [Input] bits. A tap jumps; a swipe goes the way
    /// it points. Arrow keys and WASD do the same, Space jumps.
    private void input() {
        if (!driver.interactive()) {
            return;
        }
        GameInput in = getInput();
        int bits = 0;
        if (in.wasPointerPressed()) {
            pressX = in.getPointerX();
            pressY = in.getPointerY();
        }
        if (in.wasPointerReleased() && pressX >= 0) {
            int dx = in.getPointerX() - pressX;
            int dy = in.getPointerY() - pressY;
            int min = CN.convertToPixels(SWIPE / 8f);
            if (Math.abs(dx) < min && Math.abs(dy) < min) {
                bits |= Input.JUMP;
            } else if (Math.abs(dx) > Math.abs(dy)) {
                bits |= dx < 0 ? Input.LEFT : Input.RIGHT;
            } else {
                bits |= dy < 0 ? Input.JUMP : Input.SLIDE;
            }
            pressX = -1;
        }
        bits |= edge(0, in.isGameKeyDown(Display.GAME_LEFT) || in.isKeyDown('a') || in.isKeyDown('A'), Input.LEFT);
        bits |= edge(1, in.isGameKeyDown(Display.GAME_RIGHT) || in.isKeyDown('d') || in.isKeyDown('D'), Input.RIGHT);
        bits |= edge(2, in.isGameKeyDown(Display.GAME_UP) || in.isKeyDown('w') || in.isKeyDown('W')
                || in.isKeyDown(' '), Input.JUMP);
        bits |= edge(3, in.isGameKeyDown(Display.GAME_DOWN) || in.isKeyDown('s') || in.isKeyDown('S'), Input.SLIDE);
        if (bits != 0) {
            driver.input(bits);
        }
    }
    // end::dash-controls[]

    private int edge(int key, boolean down, int bit) {
        boolean was = keyWas[key];
        keyWas[key] = down;
        return down && !was ? bit : 0;
    }

    private int hudTick = -1;

    private void notifyHud(boolean stepped) {
        final Run run = driver.run();
        if (hud == null) {
            return;
        }
        if (stepped && run.tick() / 6 != hudTick) {
            hudTick = run.tick() / 6;
            CN.callSerially(() -> {
                if (hud != null) {
                    hud.ticked(run);
                }
            });
        }
        if (run.over() && !endedSent) {
            endedSent = true;
            CN.callSerially(() -> {
                if (hud != null) {
                    hud.ended(run);
                }
            });
        }
    }
}
