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
package com.codenameone.examples.skirmish.battle;

import com.codename1.gaming.AnimatedModel;
import com.codename1.gaming.GameCamera;
import com.codename1.gaming.GameInput;
import com.codename1.gaming.GameView;
import com.codename1.gaming.Model;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Texture;
import com.codename1.ui.CN;
import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.Cards;
import com.codenameone.examples.skirmish.rules.Event;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.Unit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/// The arena in 3D, drawn with `com.codename1.gaming`: animated glTF
/// characters ([Actor]), glTF towers and scenery, and particle effects
/// ([Effects]) under a perspective camera that looks down the field from
/// behind the player's king.
///
/// The simulation advances in fixed 50 ms ticks -- the rules' clock -- while
/// the screen redraws as fast as the device allows. Between two ticks every
/// unit is drawn part of the way from where it was to where it is, so a 20 Hz
/// simulation still moves smoothly at 60 or 120 Hz.
///
/// Nothing here decides anything. Units, hits and deaths all come from the
/// [Match] the [Driver] steps; this class only turns them into models,
/// animation, effects and sound. Placing a card is the one input: a card
/// chosen in the hand (a styled `Button` under the arena) and a press, drag
/// and release on the field. The press is turned into a point on the ground
/// by casting a ray from the camera through it.
// tag::skirmish-battle-view[]
public class BattleView extends GameView {
    /// The widgets around the arena: told about ticks and about the end.
    public interface Hud {
        void ticked(Match match);

        void placed(String card);

        void ended(Match match);
    }

    public static final double TICK = 1.0 / Match.TICKS_PER_SECOND;
    private static final int MAX_STEPS_PER_FRAME = 12;
    private static final float FOV = 34f;

    private final Driver driver;
    private final Map<Integer, Actor> actors = new HashMap<Integer, Actor>();
    private final Map<Integer, Model> orbs = new HashMap<Integer, Model>();
    private final List<AnimatedModel> ruins = new ArrayList<AnimatedModel>();
    private final Random fx = new Random(7);
    private final float[] inverse = new float[16];
    private Hud hud;
    private GraphicsDevice device;
    private Effects effects;
    private Mesh quad;
    private Mesh sphere;
    private Texture ringTexture;
    private Model ghost;
    private Model zone;
    private double accumulator;
    private float alpha;
    private String selected;
    private int hudTick = -1;
    private int lastTick = -1;
    private boolean ended;
    private boolean cheered;
    /// Seconds into the finishing camera sweep, or -1 before the match ends.
    private float finale = -1f;
    private float finaleX;
    private float finaleZ;
    private static final float FINALE_SECONDS = 3.4f;
    private float sign = 1f;

    public BattleView(Driver driver) {
        this.driver = driver;
        setClearColor(0xff8fc4e8);
        setFocusable(true);
    }

    public void setHud(Hud hud) {
        this.hud = hud;
    }

    public Driver driver() {
        return driver;
    }

    /// The card the next press on the field places, or null.
    public void select(String card) {
        selected = card;
    }

    public String selected() {
        return selected;
    }

    // tag::skirmish-3d-setup[]
    @Override
    protected void onSetup(GraphicsDevice d) {
        device = d;
        sign = driver.me() == 0 ? 1f : -1f;
        getLight().setDirection(-0.35f, -1f, -0.45f).setColor(0xfffff4e0).setAmbientColor(0xff8a8f9a);
        GameCamera cam = getCamera();
        cam.setPerspective(FOV, 1f, 200f);
        cam.setPosition(0f, 33f, 30f);
        cam.setTarget(0f, 0f, 1.6f);

        quad = Primitives.quad(d, 1f);
        sphere = Primitives.sphere(d, 0.45f, 10, 14, false);
        ringTexture = d.createTexture(64, 64, Art.ring(64));
        ringTexture.setFilter(Texture.Filter.LINEAR);

        // The world beyond the field, then the field itself.
        Texture meadow = d.createTexture(Art.meadow());
        meadow.setWrap(Texture.Wrap.REPEAT);
        Model outside = flat(new Material(Material.Type.LAMBERT).setTexture(meadow), 120f, 120f);
        outside.setPosition(0f, -0.05f, 0f);
        addModel(outside);
        Texture fieldTexture = d.createTexture(Art.field());
        fieldTexture.setFilter(Texture.Filter.LINEAR);
        Model field = flat(new Material(Material.Type.LAMBERT).setTexture(fieldTexture), 18f, 32f);
        field.setRotation(-90f, sign > 0 ? 0f : 180f, 0f);
        addModel(field);

        scenery();

        zone = flat(new Material(Material.Type.UNLIT).setColor(0x50e0302a)
                .setRenderState(RenderState.transparent()), 18f, Arena.RIVER_TOP / (float) Arena.TILE);
        zone.setPosition(0f, 0.04f, -16f + Arena.RIVER_TOP / (float) Arena.TILE / 2f);
        zone.setVisible(false);
        addModel(zone);
        ghost = Actor.ringModel(d, quad, ringTexture, true, 1f);
        ghost.setVisible(false);
        addModel(ghost);
        effects = new Effects(d, this);
    }
    // end::skirmish-3d-setup[]

    /// A flat rectangle lying on the ground, `w` by `h` world units.
    private Model flat(Material m, float w, float h) {
        Model model = new Model(quad, m);
        model.setRotation(-90f, 0f, 0f);
        model.setScale(w, h, 1f);
        return model;
    }

    private AnimatedModel prop(String name, float x, float y, float z, float footprint) {
        AnimatedModel m = new AnimatedModel(device, Models.scene(name));
        float[] b = Actor.bounds(m);
        float s = footprint / Math.max(0.01f, Math.max(b[3] - b[0], b[5] - b[2]));
        m.setScale(s);
        m.setPosition(x, y - b[1] * s, z);
        m.addTo(this);
        return m;
    }

    /// Trees, hills and rocks around the field, the same every match.
    private void scenery() {
        Random r = new Random(42);
        String[] kinds = {"trees", "tree", "rock", "hill", "trees", "tree"};
        for (int i = 0; i < 26; i++) {
            float side = i % 2 == 0 ? -1f : 1f;
            float x = side * (10.5f + r.nextFloat() * 6f);
            float z = -18f + r.nextFloat() * 36f;
            String kind = kinds[i % kinds.length];
            float size = "hill".equals(kind) ? 5f : "rock".equals(kind) ? 1.4f : 2.6f + r.nextFloat();
            AnimatedModel m = prop(kind, x, 0f, z, size);
            m.setRotation(0f, r.nextFloat() * 360f, 0f);
        }
        for (int i = 0; i < 10; i++) {
            float x = -9f + i * 2f;
            prop(i % 3 == 0 ? "trees" : "tree", x, 0f, -19.5f - (i % 2), 2.8f);
        }
    }

    /// Rules coordinates to world: the player's own side is toward the camera.
    private float wx(int ax, int ay) {
        return sign * (ax - Arena.WIDTH / 2) / (float) Arena.TILE;
    }

    private float wz(int ax, int ay) {
        return sign * (ay - Arena.HEIGHT / 2) / (float) Arena.TILE;
    }

    @Override
    protected void update(double dt) {
        if (effects == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        // tag::skirmish-fixed-tick[]
        accumulator += dt * driver.speedPercent() / 100.0;
        int steps = 0;
        while (accumulator >= TICK && steps < MAX_STEPS_PER_FRAME) {
            if (!driver.canStep()) {
                // Online and waiting for the server: hold at the last tick.
                accumulator = Math.min(accumulator, TICK);
                break;
            }
            driver.step();
            sync();
            events(driver.match().events());
            accumulator -= TICK;
            steps++;
        }
        alpha = (float) Math.min(1, accumulator / TICK);
        // end::skirmish-fixed-tick[]
        if (lastTick < 0) {
            sync();
        }
        frame((float) dt);
        input();
        notifyHud();
    }

    /// One tick happened: every live unit gets its new position, a unit the
    /// match no longer has starts to die.
    private void sync() {
        Match m = driver.match();
        lastTick = m.tick();
        List<Unit> live = m.units();
        for (int i = 0; i < live.size(); i++) {
            Unit u = live.get(i);
            CardDef card = u.card();
            if (card.kind == CardDef.SPELL) {
                continue;
            }
            Integer key = Integer.valueOf(u.id());
            Actor a = actors.get(key);
            if (a == null) {
                a = create(u);
                actors.put(key, a);
            }
            a.seen = lastTick;
            a.moveTo(wx(u.x(), u.y()), wz(u.x(), u.y()));
        }
        for (Iterator<Actor> it = actors.values().iterator(); it.hasNext();) {
            Actor a = it.next();
            if (a.seen != lastTick && a.dying < 0f) {
                kill(a);
            }
        }
    }

    private Actor create(Unit u) {
        CardDef card = u.card();
        boolean mine = u.owner() == driver.me();
        Models.Look look = Models.of(card.id);
        String name;
        float footprint = 2f;
        if (Cards.KING_TOWER.equals(card.id)) {
            name = mine ? "castle_blue" : "castle_red";
            footprint = 3.6f;
        } else if (Cards.PRINCESS_TOWER.equals(card.id)) {
            name = mine ? "tower_blue" : "tower_red";
            footprint = 2.5f;
        } else {
            name = look == null ? "knight" : look.model;
            footprint = look == null ? 2f : look.size;
        }
        AnimatedModel model = new AnimatedModel(device, Models.scene(name));
        float radius = Math.max(0.35f, card.radius() / (float) Arena.TILE);
        Model ring = Actor.ringModel(device, quad, ringTexture, mine, radius);
        Actor a = new Actor(u, mine, model, look, ring, footprint);
        if (a.structure) {
            float[] b = Actor.bounds(model);
            model.setPosition(0f, -b[1] * a.scale, 0f);
        }
        addModel(ring);
        model.addTo(this);
        getScene().add(a.barBack);
        getScene().add(a.bar);
        if (!a.structure && lastTick > 0) {
            effects.spawn(wx(u.x(), u.y()), wz(u.x(), u.y()), mine);
        }
        return a;
    }

    private void kill(Actor a) {
        a.die();
        if (a.structure) {
            effects.explosion(a.toX, 1.5f, a.toZ, 3f);
            a.model.setVisible(false);
            AnimatedModel ruin = prop("ruin", a.toX, 0f, a.toZ, 3f);
            ruins.add(ruin);
            Sfx.boom();
        }
    }

    // tag::skirmish-events-3d[]
    private void events(List<Event> events) {
        Match m = driver.match();
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            float x = wx(e.x, e.y);
            float z = wz(e.x, e.y);
            switch (e.kind) {
                case Event.ATTACK: {
                    Actor attacker = actors.get(Integer.valueOf(e.source));
                    Actor target = actors.get(Integer.valueOf(e.target));
                    if (attacker != null) {
                        attacker.attacked(x, z);
                    }
                    if (target != null) {
                        target.hit();
                        effects.hit(target.toX, Math.min(1.2f, target.height * 0.6f), target.toZ);
                    } else {
                        effects.hit(x, 0.6f, z);
                    }
                    Sfx.hit();
                    break;
                }
                case Event.SPELL: {
                    Unit spell = m.unit(e.source);
                    CardDef sd = spell == null ? null : spell.card();
                    float size = sd == null ? 2.5f : sd.splash() / (float) Arena.TILE;
                    if (sd != null && "arrow-rain".equals(sd.id)) {
                        effects.volley(x, z, size * 0.7f, 30, fx);
                        effects.dust(x, z);
                    } else {
                        effects.explosion(x, 0.4f, z, size);
                    }
                    Model orb = orbs.remove(Integer.valueOf(e.source));
                    if (orb != null) {
                        removeModel(orb);
                    }
                    Sfx.boom();
                    break;
                }
                case Event.DEATH: {
                    Actor a = actors.get(Integer.valueOf(e.source));
                    if (a != null && a.dying < 0f) {
                        kill(a);
                    }
                    break;
                }
                case Event.TOWER_DOWN:
                    effects.explosion(x, 1.5f, z, 3.5f);
                    break;
                default:
                    break;
            }
        }
    }
    // end::skirmish-events-3d[]

    /// Everything that moves between ticks: actors, falling spells, effects.
    private void frame(float dt) {
        Match m = driver.match();
        for (Iterator<Actor> it = actors.values().iterator(); it.hasNext();) {
            Actor a = it.next();
            Unit u = a.dying < 0f ? m.unit(a.id) : null;
            a.frame(u, alpha, dt);
            if (a.gone()) {
                a.model.removeFrom(this);
                removeModel(a.ring);
                getScene().remove(a.barBack);
                getScene().remove(a.bar);
                it.remove();
            }
        }
        spells(m);
        if (m.isOver() && !cheered) {
            cheered = true;
            startFinale(m);
            int winner = m.result();
            for (Actor a : actors.values()) {
                if ((a.mine && winner == driver.me()) || (!a.mine && winner >= 0 && winner != driver.me())) {
                    a.cheer();
                }
            }
        }
        if (finale >= 0f) {
            finale(dt);
        }
        effects.update(dt, getCamera());
    }

    // tag::skirmish-finale[]
    /// The finishing move: once the match is decided the camera leaves its
    /// fixed battle view, swings around the field and comes down low beside
    /// the fallen king tower, so the last blow is seen from the ground. The
    /// rules are already over, so this is purely the renderer's -- a replay
    /// or a spectator sees the same sweep.
    private void startFinale(Match m) {
        previewFinale(m.result());
    }

    /// Runs the sweep toward `winner`'s victory (or a draw) without the match
    /// being over: the screenshot tests use it to photograph the finale.
    public void previewFinale(int winner) {
        int loser = winner < 0 ? -1 : 1 - winner;
        if (loser < 0) {
            // A draw: look at the bridge between the two sides.
            finaleX = 0f;
            finaleZ = 0f;
        } else {
            int ky = loser == 0 ? Arena.KING_Y_BOTTOM : Arena.mirrorY(Arena.KING_Y_BOTTOM);
            finaleX = wx(Arena.KING_X, ky);
            finaleZ = wz(Arena.KING_X, ky);
        }
        finale = 0f;
    }

    private void finale(float dt) {
        finale = Math.min(FINALE_SECONDS, finale + dt);
        float t = finale / FINALE_SECONDS;
        // Ease in and out, so the sweep starts and settles without a jolt.
        float e = t * t * (3f - 2f * t);
        // Orbit from straight behind the player's side to a three-quarter view
        // beside the tower, dropping from the battle height to eye level.
        double angle = Math.toRadians(e * 125f);
        float radius = 42f - e * 22f;
        float height = 33f - e * 24f;
        float cx = finaleX * e;
        float cz = 1.6f + (finaleZ - 1.6f) * e;
        float ex = cx + (float) Math.sin(angle) * radius;
        float ez = cz + (float) Math.cos(angle) * radius * (30f / 42f + e * (1f - 30f / 42f))
                - 1.6f * (1f - e);
        GameCamera cam = getCamera();
        cam.setPosition(ex, height, ez);
        cam.setTarget(cx, 1.6f * e, cz);
    }

    /// True while the finishing sweep still has frames to show.
    public boolean finaleRunning() {
        return finale >= 0f && finale < FINALE_SECONDS;
    }
    // end::skirmish-finale[]

    /// A fire orb arcs from its caster's king tower to where it will land;
    /// arrows start falling as the volley is about to hit.
    private void spells(Match m) {
        List<Unit> live = m.units();
        for (int i = 0; i < live.size(); i++) {
            Unit u = live.get(i);
            CardDef card = u.card();
            if (card.kind != CardDef.SPELL || !u.isDeploying()) {
                continue;
            }
            float tx = wx(u.x(), u.y());
            float tz = wz(u.x(), u.y());
            float total = Math.max(1, card.deployTicks());
            float p = 1f - Math.max(0f, (u.deployTimer() - alpha)) / total;
            if ("arrow-rain".equals(card.id)) {
                if (p > 0.5f && fx.nextInt(3) == 0) {
                    effects.volley(tx, tz, card.splash() / (float) Arena.TILE * 0.7f, 2, fx);
                }
                continue;
            }
            Integer key = Integer.valueOf(u.id());
            Model orb = orbs.get(key);
            if (orb == null) {
                orb = new Model(sphere, new Material(Material.Type.UNLIT).setColor(0xffffb040));
                orbs.put(key, orb);
                addModel(orb);
            }
            boolean mine = u.owner() == driver.me();
            float sz = mine ? 13f : -13f;
            float x = 0f + (tx - 0f) * p;
            float z = sz + (tz - sz) * p;
            float y = 3f + (0.3f - 3f) * p + 9f * p * (1f - p);
            orb.setPosition(x, y, z);
            effects.trail(x, y, z);
        }
    }

    // tag::skirmish-touch[]
    private void input() {
        GameInput in = getInput();
        String card = selected;
        boolean holding = card != null && driver.interactive() && !driver.match().isOver();
        int px = in.getPointerX();
        int py = in.getPointerY();
        boolean inside = px >= 0 && py >= 0 && px < getWidth() && py < getHeight();
        float[] ground = holding && inside ? pick(px, py) : null;
        if (holding && in.isPointerDown() && ground != null) {
            CardDef def = Cards.get(card);
            int ax = ax(ground[0], ground[1]);
            int ay = ay(ground[0], ground[1]);
            boolean ok = driver.match().canPlay(driver.me(), card, ax, ay);
            float r = Math.max(def.radius(), def.splash()) / (float) Arena.TILE;
            ghost.setScale(Math.max(1f, r * 2f + 0.4f));
            ghost.setPosition(ground[0], 0.06f, ground[1]);
            ghost.getMaterial().setColor(ok ? 0xe0ffffff : 0xe0ff4040);
            ghost.setVisible(true);
            zone.setVisible(def.kind != CardDef.SPELL);
        } else {
            ghost.setVisible(false);
            zone.setVisible(false);
        }
        if (holding && in.wasPointerReleased() && ground != null) {
            int x = ax(ground[0], ground[1]);
            int y = ay(ground[0], ground[1]);
            if (driver.match().canPlay(driver.me(), card, x, y)) {
                driver.play(card, x, y);
                Sfx.deploy();
                selected = null;
                CN.callSerially(() -> {
                    if (hud != null) {
                        hud.placed(card);
                    }
                });
            }
        }
    }
    // end::skirmish-touch[]

    private int ax(float x, float z) {
        return Math.round(x * sign * Arena.TILE) + Arena.WIDTH / 2;
    }

    private int ay(float x, float z) {
        return Math.round(z * sign * Arena.TILE) + Arena.HEIGHT / 2;
    }

    /// Where the ray from the camera through screen point `(px, py)` meets
    /// the ground, as `{x, z}`, or null when it misses.
    // tag::skirmish-pick[]
    float[] pick(int px, int py) {
        GameCamera c = getCamera();
        float aspect = (float) getWidth() / Math.max(1, getHeight());
        float[] proj = Matrix4.perspective((float) Math.toRadians(c.getFov()), aspect, 1f, 200f);
        float[] view = Matrix4.lookAt(c.getEyeX(), c.getEyeY(), c.getEyeZ(),
                c.getTargetX(), c.getTargetY(), c.getTargetZ(), 0f, 1f, 0f);
        float[] vp = new float[16];
        Matrix4.multiply(proj, view, vp);
        if (!Matrix4.invert(vp, inverse)) {
            return null;
        }
        float nx = 2f * (px + 0.5f) / getWidth() - 1f;
        float ny = 1f - 2f * (py + 0.5f) / getHeight();
        float[] near = unproject(nx, ny, -1f);
        float[] far = unproject(nx, ny, 1f);
        float dy = far[1] - near[1];
        if (Math.abs(dy) < 1e-6f) {
            return null;
        }
        float t = -near[1] / dy;
        if (t < 0f) {
            return null;
        }
        return new float[] {near[0] + (far[0] - near[0]) * t, near[2] + (far[2] - near[2]) * t};
    }
    // end::skirmish-pick[]

    /// Where arena point `(ax, ay)` appears on the screen, in absolute
    /// coordinates, or null before the first frame. The tutorial points at
    /// the field with it.
    public int[] screenPoint(int ax, int ay) {
        if (getWidth() <= 0 || getHeight() <= 0 || effects == null) {
            return null;
        }
        GameCamera c = getCamera();
        float aspect = (float) getWidth() / Math.max(1, getHeight());
        float[] proj = Matrix4.perspective((float) Math.toRadians(c.getFov()), aspect, 1f, 200f);
        float[] view = Matrix4.lookAt(c.getEyeX(), c.getEyeY(), c.getEyeZ(),
                c.getTargetX(), c.getTargetY(), c.getTargetZ(), 0f, 1f, 0f);
        float[] m = new float[16];
        Matrix4.multiply(proj, view, m);
        float x = wx(ax, ay);
        float z = wz(ax, ay);
        float cx = m[0] * x + m[8] * z + m[12];
        float cy = m[1] * x + m[9] * z + m[13];
        float cw = m[3] * x + m[11] * z + m[15];
        if (cw <= 0f) {
            return null;
        }
        int px = Math.round((cx / cw + 1f) * 0.5f * getWidth());
        int py = Math.round((1f - cy / cw) * 0.5f * getHeight());
        return new int[] {getAbsoluteX() + px, getAbsoluteY() + py};
    }

    private float[] unproject(float x, float y, float z) {
        float[] m = inverse;
        float rx = m[0] * x + m[4] * y + m[8] * z + m[12];
        float ry = m[1] * x + m[5] * y + m[9] * z + m[13];
        float rz = m[2] * x + m[6] * y + m[10] * z + m[14];
        float rw = m[3] * x + m[7] * y + m[11] * z + m[15];
        if (rw == 0f) {
            rw = 1e-6f;
        }
        return new float[] {rx / rw, ry / rw, rz / rw};
    }

    private void notifyHud() {
        final Match m = driver.match();
        if (hud == null) {
            return;
        }
        if (m.isOver() && !ended) {
            if (finaleRunning()) {
                // The result screen waits for the camera to land.
                return;
            }
            ended = true;
            CN.callSerially(() -> {
                driver.finished();
                hud.ended(m);
            });
            return;
        }
        // The widgets are refreshed four times a second, which is all an
        // elixir bar and a clock need.
        if (m.tick() / 5 != hudTick) {
            hudTick = m.tick() / 5;
            CN.callSerially(() -> hud.ticked(m));
        }
    }
}
// end::skirmish-battle-view[]
