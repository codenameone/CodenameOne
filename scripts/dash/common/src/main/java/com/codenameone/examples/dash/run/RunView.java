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

import com.codename1.gaming.AnimatedModel;
import com.codename1.gaming.GameCamera;
import com.codename1.gaming.GameInput;
import com.codename1.gaming.GameView;
import com.codename1.gaming.Model;
import com.codename1.gaming.ParticleEmitter;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
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
/// There is one scene and one simulation. "Faux 2D" is a camera: far off to
/// the side, with a field of view so narrow the perspective all but vanishes,
/// so the lanes stack behind each other and the track reads as a platformer.
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

    private final Driver driver;
    private Hud hud;
    private GraphicsDevice device;
    private Mesh cube;
    private Mesh quad;
    private Mesh sphere;
    private final Model[] floor = new Model[SEGMENTS];
    private final AnimatedModel[] props = new AnimatedModel[SEGMENTS * 2];
    private AnimatedModel[] runners = new AnimatedModel[0];
    private AnimatedModel[] ghostModels = new AnimatedModel[0];
    private final Map<Integer, Model> entries = new HashMap<Integer, Model>();
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
        Theme theme = Theme.of(driver.run().level().theme);
        setClearColor(theme.sky);
        getLight().setDirection(-0.4f, -1f, -0.3f).setColor(0xfffff4e0).setAmbientColor(theme.ambient);
        getCamera().setPerspective(60f, 0.5f, 400f);
        cube = Primitives.cube(d, 1f);
        quad = Primitives.quad(d, 1f);
        sphere = Primitives.sphere(d, 0.5f, 10, 14, false);

        int lanes = Math.max(1, driver.run().level().lanes);
        Texture lane = d.createTexture(64, 64, Theme.laneTexture(theme, lanes));
        lane.setFilter(Texture.Filter.LINEAR);
        Material floorMaterial = new Material(Material.Type.LAMBERT).setTexture(lane);
        for (int i = 0; i < SEGMENTS; i++) {
            Model m = new Model(quad, floorMaterial);
            m.setRotation(-90f, 0f, 0f);
            m.setScale(lanes * LANE_W + 0.6f, SEGMENT, 1f);
            floor[i] = m;
            addModel(m);
        }
        Model ground = new Model(quad, new Material(Material.Type.LAMBERT).setColor(theme.ground));
        ground.setRotation(-90f, 0f, 0f);
        ground.setScale(400f, 400f, 1f);
        ground.setPosition(0f, -0.05f, 0f);
        ground.setUserData("ground");
        addModel(ground);
        for (int i = 0; i < props.length; i++) {
            String kind = theme.props[i % theme.props.length];
            props[i] = new AnimatedModel(d, Models.scene(kind));
            float[] b = Models.bounds(props[i]);
            float s = (i % 3 == 0 ? 3.2f : 2.2f) / Math.max(0.01f, Math.max(b[3] - b[0], b[5] - b[2]));
            props[i].setScale(s);
            props[i].addTo(this);
        }

        Run run = driver.run();
        runners = new AnimatedModel[run.runnerCount()];
        for (int i = 0; i < runners.length; i++) {
            String id = i < runnerIds.length ? runnerIds[i] : null;
            runners[i] = runnerModel(Models.modelFor(id, i + 1), i == driver.me() ? 0xffffffff : 0xffb0c8ff);
        }
        Run[] ghosts = driver.ghosts();
        ghostModels = new AnimatedModel[ghosts.length];
        for (int i = 0; i < ghosts.length; i++) {
            ghostModels[i] = runnerModel(Models.modelFor(null, i + 2), 0xff8890a0);
        }
        prevX = new float[runners.length + ghostModels.length];
        prevY = new float[prevX.length];
        prevZ = new float[prevX.length];
        remember();

        sparks = new ParticleEmitter(d, 160).setColor(0xffffe27a).setAdditive(true)
                .setSpeed(1.5f, 4f).setLifetime(0.15f, 0.4f).setSize(0.25f, 0.05f)
                .setGravity(-6f).setDrag(1f);
        dust = new ParticleEmitter(d, 120).setColor(0x90c2a77a).setAdditive(false)
                .setSpeed(0.6f, 1.8f).setLifetime(0.3f, 0.7f).setSize(0.3f, 0.8f)
                .setGravity(0.4f).setDrag(3f).setDirection(0f, 0.3f, 0f, 85f);
        dust.addTo(this);
        sparks.addTo(this);
        Sfx.load();
    }
    // end::dash-run-view[]

    private AnimatedModel runnerModel(String name, int tint) {
        AnimatedModel m = new AnimatedModel(device, Models.scene(name));
        float[] b = Models.bounds(m);
        m.setScale(RUNNER_HEIGHT / Math.max(0.01f, b[4] - b[1]));
        m.setTint(tint);
        String clip = Models.clip(m, "Running_A");
        if (clip != null) {
            m.play(clip, true);
        }
        m.addTo(this);
        return m;
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

        // The floor: segments leapfrog each other as the runner passes them.
        int first = (int) Math.floor((meZ - BEHIND) / SEGMENT);
        for (int i = 0; i < SEGMENTS; i++) {
            int seg = first + i;
            floor[i].setPosition(0f, 0f, -(seg * SEGMENT + SEGMENT / 2f));
            AnimatedModel left = props[i * 2];
            AnimatedModel right = props[i * 2 + 1];
            float side = Math.max(1, run.level().lanes) * LANE_W / 2f + 3f;
            left.setPosition(-side - jitter(seg, 3) * 4f, 0f, -(seg * SEGMENT + jitter(seg, 5) * SEGMENT));
            right.setPosition(side + jitter(seg, 7) * 4f, 0f, -(seg * SEGMENT + jitter(seg, 11) * SEGMENT));
            left.setRotation(0f, jitter(seg, 13) * 360f, 0f);
            right.setRotation(0f, jitter(seg, 17) * 360f, 0f);
        }
        entries(run, me, meZ);
        camera(run, me, meX, meY, meZ);
        sparks.update(dt, getCamera());
        dust.update(dt, getCamera());
    }

    private void pose(AnimatedModel m, Runner r, float x, float y, float z, float dt) {
        m.setPosition(x, y, -z);
        m.setRotation(r.sliding() ? -55f : 0f, 180f, 0f);
        String want = !r.alive ? "Death_A" : r.finished ? "Cheer" : "Running_A";
        if (!want.equals(m.getClipName()) && Models.clip(m, want) != null) {
            m.crossFade(want, 0.2f, r.alive);
        }
        m.setSpeed(r.alive && !r.finished ? Math.max(0.6f, r.speed / 12000f) : 1f);
        m.update(dt);
    }

    /// Obstacles and pickups from just behind the camera to the horizon:
    /// models are made as entries come into view and dropped behind.
    private void entries(Run run, Runner me, float meZ) {
        Track track = run.track();
        while (firstEntry < track.size()) {
            Track.Entry e = track.get(firstEntry);
            if ((e.z + e.depth) / (float) Track.UNIT < meZ - BEHIND) {
                Model gone = entries.remove(Integer.valueOf(firstEntry));
                if (gone != null) {
                    removeModel(gone);
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
            Model m = entries.get(key);
            if (m == null) {
                m = model(run, e);
                entries.put(key, m);
                addModel(m);
            }
            if (e.pickup != null) {
                boolean taken = me.took(e.index);
                m.setVisible(!taken);
                if (!taken) {
                    float bob = (float) Math.sin(time * 3f + i) * 0.12f;
                    m.setPosition(m.getX(), 1.0f + bob, m.getZ());
                    m.setRotation(0f, time * 120f + i * 20f, 0f);
                }
            }
        }
        // Defensive: anything left over from before a restart.
        Iterator<Map.Entry<Integer, Model>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Model> en = it.next();
            if (en.getKey().intValue() < firstEntry) {
                removeModel(en.getValue());
                it.remove();
            }
        }
    }

    private Model model(Run run, Track.Entry e) {
        int lanes = Math.max(1, run.level().lanes);
        float width = e.spansAllLanes() ? lanes * LANE_W : LANE_W * 0.85f;
        float x = e.spansAllLanes() ? 0f : (e.lane * 1000 - (lanes - 1) * 500) / (float) Track.UNIT * LANE_W;
        float depth = Math.max(0.2f, e.depth / (float) Track.UNIT);
        float z = -(e.z / (float) Track.UNIT + depth / 2f);
        if (e.pickup != null) {
            Model m = new Model(sphere, new Material(Material.Type.PHONG).setColor(pickupColor(e.pickup.kind)));
            float s = e.pickup.kind == PickupKind.COIN ? 0.45f : 0.7f;
            m.setScale(s, s, e.pickup.kind == PickupKind.COIN ? 0.12f : s);
            m.setPosition(x, 1.0f, -(e.z / (float) Track.UNIT));
            return m;
        }
        Clearance c = e.obstacle.clearance;
        Model m;
        if (c == Clearance.JUMP && e.obstacle.height == 0) {
            // A gap: a dark pit lying just above the track.
            m = new Model(quad, new Material(Material.Type.UNLIT).setColor(0xff101018));
            m.setRotation(-90f, 0f, 0f);
            m.setScale(width + 0.1f, depth, 1f);
            m.setPosition(x, 0.02f, z);
        } else if (c == Clearance.JUMP) {
            float h = e.obstacle.height / (float) Track.UNIT;
            m = new Model(cube, new Material(Material.Type.PHONG).setColor(0xffff9a3c));
            m.setScale(width, h, depth);
            m.setPosition(x, h / 2f, z);
        } else if (c == Clearance.SLIDE) {
            m = new Model(cube, new Material(Material.Type.PHONG).setColor(0xffe0405a));
            m.setScale(width, 0.9f, depth);
            m.setPosition(x, 1.55f, z);
        } else {
            m = new Model(cube, new Material(Material.Type.LAMBERT).setColor(0xff4a78c8));
            m.setScale(width, 2.6f, depth);
            m.setPosition(x, 1.3f, z);
        }
        return m;
    }

    private static int pickupColor(PickupKind k) {
        switch (k) {
            case COIN:
                return 0xffffcc33;
            case GEM:
                return 0xff3ce0ff;
            case MAGNET:
                return 0xffff4a4a;
            case SHIELD:
                return 0xff6a8cff;
            default:
                return 0xff5cff7a;
        }
    }

    // tag::dash-camera-blend[]
    /// Places the camera for `mode`, into `out`: eye x, y, z, target x, y, z,
    /// field of view, then the up vector's z (0 for upright, -1 looking down).
    private static void framing(ViewMode mode, float x, float y, float z, float[] out) {
        if (mode == ViewMode.SIDE_2D) {
            // Far to the right with a narrow lens: the lanes line up behind
            // each other and the run reads as a side-scroller.
            out[0] = 70f;
            out[1] = 1.6f + y * 0.3f;
            out[2] = -z - 5f;
            out[3] = 0f;
            out[4] = 1.4f + y * 0.3f;
            out[5] = -z - 5f;
            out[6] = 9f;
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
