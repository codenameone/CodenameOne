/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.play;

import com.codename1.util.MathUtil;
import java.util.ArrayList;
import java.util.List;

/// The whole game, as state and one [#step(float)].
///
/// The arena knows nothing of the screen: positions are in sheet units on a
/// [#WIDTH] by [#HEIGHT] page, and the controller sets [#moveX], [#moveY],
/// [#aimX], [#aimY], [#firing] and [#bombPressed] before each step. What the
/// player should see or hear happen -- a burst, a pick-up, a lost life -- goes
/// to the [Listener], which [ArenaView] turns into paper scraps, ink and sound.
/// That split is what lets a test play a hundred runs with the [Bot] and no
/// display.
///
/// The server never replays a run, so nothing here has to be deterministic
/// across devices; it checks a submitted score against
/// `ScoreBounds` in the shared module instead, and the bound has to stay
/// true of this class. `ArenaTest` holds the two together.
public final class Arena {
    public static final float WIDTH = 1600;
    public static final float HEIGHT = 1000;
    /// How long a creature takes to be drawn onto the page before it moves.
    public static final float SPAWN_TIME = 0.9f;
    public static final float STEP = 1f / 60f;

    public static final int CLASSIC = 0;
    /// No pencil: steer the quill between the ends of paper gates to burst them.
    public static final int FOLD = 1;
    /// Classic, on the day's seed, so everyone faces the same waves.
    public static final int DAILY = 2;

    static final float SHIP_RADIUS = 14;
    static final float SHIP_SPEED = 430;
    static final float SHOT_SPEED = 1100;
    static final float FIRE_INTERVAL = 0.085f;
    public static final int MAX_MULTIPLIER = 20;
    static final int EXTRA_LIFE_EVERY = 100000;
    static final int MAX_LIVES = 6;
    static final int MAX_BOMBS = 6;
    static final float RESPAWN_TIME = 1.6f;
    static final float SHIELD_TIME = 2.2f;

    /// What the game wants seen and heard. Every call happens inside
    /// [#step(float)], on whatever thread steps the arena.
    public interface Listener {
        /// Something was cut up at (x, y), in its own colour; `size` is its radius.
        void burst(float x, float y, int colour, float size, boolean big);

        void fired(int streams);

        void hit(float x, float y);

        void spawned(Enemy e);

        void picked(float x, float y, int multiplier);

        void quillLost(float x, float y, int livesLeft);

        void erased(float x, float y);

        void extraLife();

        void over();
    }

    public final int mode;
    public final Sheet sheet = new Sheet(WIDTH, HEIGHT, 40);
    public final List<Enemy> enemies = new ArrayList<Enemy>();
    public final List<Shot> shots = new ArrayList<Shot>();
    public final List<Drop> drops = new ArrayList<Drop>();
    private final List<Enemy> pool = new ArrayList<Enemy>();
    private Listener listener;
    private long seed;

    // ---- the quill and its controls -----------------------------------------
    public float shipX = WIDTH / 2;
    public float shipY = HEIGHT / 2;
    public float shipVx;
    public float shipVy;
    /// Facing, in degrees clockwise from +x: where it flies, not where it aims.
    public float shipAngle = -90;
    public float aimAngle = -90;
    public boolean shipAlive = true;
    public float respawn;
    public float shield = SHIELD_TIME;

    public float moveX;
    public float moveY;
    public float aimX;
    public float aimY;
    public boolean firing;
    public boolean bombPressed;

    // ---- the run --------------------------------------------------------------
    public long score;
    public int multiplier = 1;
    public int lives;
    public int bombs;
    public float time;
    public int kills;
    public int gates;
    public boolean over;
    private long nextLife = EXTRA_LIFE_EVERY;
    private float fireCooldown;
    private float budget;
    private float eventTimer = 18;
    private float eraseFlash;

    public Arena(int mode, long seed) {
        this.mode = mode;
        this.seed = seed == 0 ? 0x9E3779B97F4A7C15L : seed;
        lives = mode == FOLD ? 1 : 3;
        bombs = mode == FOLD ? 0 : 3;
    }

    public void setListener(Listener l) {
        listener = l;
    }

    /// True while the quill is gone and waiting to be folded again.
    public boolean respawning() {
        return !shipAlive && !over;
    }

    public float eraseFlash() {
        return eraseFlash;
    }

    /// Advances the game by `dt` seconds. Call it with [#STEP]; a larger step
    /// lets fast things pass through each other.
    public void step(float dt) {
        if (over) {
            sheet.update(dt);
            return;
        }
        time += dt;
        eraseFlash = Math.max(0, eraseFlash - dt);
        sheet.update(dt);
        stepShip(dt);
        direct(dt);
        stepEnemies(dt);
        stepShots(dt);
        stepDrops(dt);
        collide();
        compact();
    }

    // ---- the quill --------------------------------------------------------------

    private void stepShip(float dt) {
        if (!shipAlive) {
            respawn -= dt;
            if (respawn <= 0 && lives > 0) {
                shipAlive = true;
                shipX = WIDTH / 2;
                shipY = HEIGHT / 2;
                shipVx = 0;
                shipVy = 0;
                shield = SHIELD_TIME;
            }
            return;
        }
        shield = Math.max(0, shield - dt);
        float mx = moveX;
        float my = moveY;
        float m = (float) Math.sqrt(mx * mx + my * my);
        if (m > 1) {
            mx /= m;
            my /= m;
        }
        // A little inertia, so the quill banks instead of teleporting.
        float k = Math.min(1, dt * 14);
        shipVx += (mx * SHIP_SPEED - shipVx) * k;
        shipVy += (my * SHIP_SPEED - shipVy) * k;
        shipX = clamp(shipX + shipVx * dt, SHIP_RADIUS, WIDTH - SHIP_RADIUS);
        shipY = clamp(shipY + shipVy * dt, SHIP_RADIUS, HEIGHT - SHIP_RADIUS);
        if (m > 0.15f) {
            shipAngle = turn(shipAngle, deg(shipVx, shipVy), 900 * dt);
        }
        float am = (float) Math.sqrt(aimX * aimX + aimY * aimY);
        if (am > 0.3f) {
            aimAngle = deg(aimX, aimY);
        }
        if (mode != FOLD) {
            fireCooldown -= dt;
            if (firing && am > 0.3f && fireCooldown <= 0) {
                fire();
                fireCooldown = FIRE_INTERVAL;
            }
            if (bombPressed && bombs > 0) {
                erase();
            }
        }
        bombPressed = false;
        if (time * 7 % 1 < dt * 7) {
            // The quill's wake ruffles the page.
            sheet.poke(shipX, shipY, 60, 30);
        }
    }

    /// The pencil gets more strokes as the multiplier climbs: the reward for
    /// staying alive is felt in the hand, not just in the score.
    public int streams() {
        return multiplier >= 15 ? 5 : multiplier >= 8 ? 3 : 2;
    }

    private void fire() {
        int n = streams();
        float a = aimAngle;
        float rad = (float) Math.toRadians(a);
        float nx = (float) -Math.sin(rad);
        float ny = (float) Math.cos(rad);
        for (int i = 0; i < n; i++) {
            float spread;
            float side;
            if (n == 2) {
                spread = 0;
                side = i == 0 ? -7 : 7;
            } else {
                spread = (i - (n - 1) / 2f) * 5f;
                side = 0;
            }
            float sa = (float) Math.toRadians(a + spread);
            Shot s = new Shot();
            s.x = shipX + nx * side + (float) Math.cos(sa) * 18;
            s.y = shipY + ny * side + (float) Math.sin(sa) * 18;
            s.vx = (float) Math.cos(sa) * SHOT_SPEED;
            s.vy = (float) Math.sin(sa) * SHOT_SPEED;
            s.angle = a + spread;
            shots.add(s);
        }
        if (listener != null) {
            listener.fired(n);
        }
    }

    private void erase() {
        bombs--;
        eraseFlash = 0.6f;
        // Indexed: a crane cut up here unfolds into the list being walked.
        for (int i = 0; i < enemies.size(); i++) { // NOPMD ForLoopCanBeForeach - grows while walked
            Enemy e = enemies.get(i);
            if (!e.dead && e.kind != Enemy.GATE) {
                kill(e, 2, false);
            }
        }
        sheet.poke(shipX, shipY, 900, 900);
        if (listener != null) {
            listener.erased(shipX, shipY);
        }
    }

    private void loseQuill() {
        shipAlive = false;
        lives--;
        multiplier = 1;
        respawn = RESPAWN_TIME;
        sheet.poke(shipX, shipY, 500, 700);
        // Losing a life clears the page, without points: a fresh start,
        // not a reward.
        for (Enemy e : enemies) {
            if (!e.dead) {
                e.dead = true;
                if (listener != null) {
                    listener.burst(e.x, e.y, Enemy.COLOUR[e.kind], e.r, false);
                }
            }
        }
        drops.clear();
        if (listener != null) {
            listener.quillLost(shipX, shipY, lives);
        }
        if (lives <= 0) {
            over = true;
            if (listener != null) {
                listener.over();
            }
        }
    }

    // ---- the director -------------------------------------------------------------

    /// The pressure of the run: a spawn budget that grows with time, spent on
    /// whatever the clock has unlocked, plus a set piece now and then.
    private void direct(float dt) {
        if (!shipAlive) {
            return;
        }
        if (mode == FOLD) {
            directFold(dt);
            return;
        }
        // It never levels off: past five minutes the page fills faster still, so
        // every run ends and a leaderboard measures skill, not patience.
        budget += dt * (1.1f + Math.min(time, 300) * 0.035f + Math.max(0, time - 300) * 0.03f);
        while (budget >= 1) {
            int kind = pickKind();
            budget -= cost(kind);
            spawnAtEdge(kind);
        }
        eventTimer -= dt;
        if (eventTimer <= 0) {
            eventTimer = 14 + rnd() * 8 - Math.min(time, 240) / 40;
            setPiece();
        }
    }

    private int pickKind() {
        float r = rnd();
        if (time > 70 && r < 0.05f) {
            return Enemy.INKWELL;
        }
        if (time > 45 && r < 0.17f) {
            return Enemy.SNIPPER;
        }
        if (time > 25 && r < 0.32f) {
            return Enemy.CRANE;
        }
        return r < 0.62f ? Enemy.DART : Enemy.PINWHEEL;
    }

    private static float cost(int kind) {
        switch (kind) {
            case Enemy.INKWELL:
                return 6;
            case Enemy.SNIPPER:
                return 2.5f;
            case Enemy.CRANE:
                return 2;
            case Enemy.PINWHEEL:
                return 0.6f;
            default:
                return 1;
        }
    }

    private void setPiece() {
        int which = (int) (rnd() * (time > 40 ? 3 : 2));
        if (which == 0) {
            // A ring of darts folding in from every side at once.
            int n = 10 + (int) Math.min(14, time / 12);
            for (int i = 0; i < n; i++) {
                double t = Math.PI * 2 * i / n;
                spawn(Enemy.DART, shipX + (float) Math.cos(t) * 380, shipY + (float) Math.sin(t) * 380);
            }
        } else if (which == 1) {
            // Pinwheels pouring out of the four corners.
            for (int c = 0; c < 4; c++) {
                float cx = c % 2 == 0 ? 60 : WIDTH - 60;
                float cy = c < 2 ? 60 : HEIGHT - 60;
                for (int i = 0; i < 5; i++) {
                    spawn(Enemy.PINWHEEL, cx + rnd() * 40 - 20, cy + rnd() * 40 - 20);
                }
            }
        } else {
            // A wall of snippers down one side.
            boolean left = shipX > WIDTH / 2;
            for (int i = 0; i < 6; i++) {
                spawn(Enemy.SNIPPER, left ? 50 : WIDTH - 50, 120 + i * (HEIGHT - 240) / 5);
            }
        }
    }

    private void directFold(float dt) {
        int gatesUp = 0;
        for (Enemy e : enemies) {
            if (!e.dead && e.kind == Enemy.GATE) {
                gatesUp++;
            }
        }
        while (gatesUp < 4 + Math.min(4, (int) (time / 30))) {
            spawnAtEdge(Enemy.GATE);
            gatesUp++;
        }
        budget += dt * (1.6f + Math.min(time, 240) * 0.05f);
        while (budget >= 1) {
            budget -= 1;
            spawnAtEdge(Enemy.DART);
        }
    }

    private void spawnAtEdge(int kind) {
        for (int attempt = 0; attempt < 8; attempt++) {
            float x;
            float y;
            if (kind == Enemy.GATE || kind == Enemy.INKWELL) {
                x = 120 + rnd() * (WIDTH - 240);
                y = 120 + rnd() * (HEIGHT - 240);
            } else if (rnd() < 0.5f) {
                x = rnd() < 0.5f ? 40 : WIDTH - 40;
                y = 40 + rnd() * (HEIGHT - 80);
            } else {
                x = 40 + rnd() * (WIDTH - 80);
                y = rnd() < 0.5f ? 40 : HEIGHT - 40;
            }
            float dx = x - shipX;
            float dy = y - shipY;
            if (dx * dx + dy * dy > 320 * 320 || attempt == 7) {
                spawn(kind, x, y);
                return;
            }
        }
    }

    Enemy spawn(int kind, float x, float y) {
        Enemy e = pool.isEmpty() ? new Enemy() : pool.remove(pool.size() - 1);
        e.set(kind, clamp(x, 30, WIDTH - 30), clamp(y, 30, HEIGHT - 30));
        if (kind == Enemy.PINWHEEL) {
            float a = rnd() * 6.2832f;
            e.vx = (float) Math.cos(a) * 90;
            e.vy = (float) Math.sin(a) * 90;
        } else if (kind == Enemy.GATE) {
            e.angle = rnd() * 180;
            e.u = (rnd() - 0.5f) * 40;
        }
        enemies.add(e);
        if (listener != null) {
            listener.spawned(e);
        }
        return e;
    }

    // ---- the creatures --------------------------------------------------------------

    private void stepEnemies(float dt) {
        float ramp = Math.min(2.6f, 1 + Math.min(time, 240) / 400 + Math.max(0, time - 240) / 260);
        // The list grows while it is walked (cranes unfold, inkwells burst).
        for (int i = 0; i < enemies.size(); i++) { // NOPMD ForLoopCanBeForeach - grows while walked
            Enemy e = enemies.get(i);
            if (e.dead) {
                continue;
            }
            e.age += dt;
            if (e.age < SPAWN_TIME) {
                continue;
            }
            float tx = shipAlive ? shipX : WIDTH / 2;
            float ty = shipAlive ? shipY : HEIGHT / 2;
            float dx = tx - e.x;
            float dy = ty - e.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            switch (e.kind) {
                case Enemy.DART: {
                    float want = deg(dx, dy);
                    e.angle = turn(e.angle, want, 220 * dt);
                    float sp = 165 * ramp;
                    float rad = (float) Math.toRadians(e.angle);
                    e.vx = (float) Math.cos(rad) * sp;
                    e.vy = (float) Math.sin(rad) * sp;
                    break;
                }
                case Enemy.PINWHEEL:
                    e.angle += 260 * dt;
                    if (e.x < e.r || e.x > WIDTH - e.r) {
                        e.vx = -e.vx;
                    }
                    if (e.y < e.r || e.y > HEIGHT - e.r) {
                        e.vy = -e.vy;
                    }
                    break;
                case Enemy.CRANE: {
                    e.t += dt;
                    float base = deg(dx, dy) + (float) Math.sin(e.t * 2.2f) * 55;
                    e.angle = turn(e.angle, base, 160 * dt);
                    float rad = (float) Math.toRadians(e.angle);
                    e.vx = (float) Math.cos(rad) * 140 * ramp;
                    e.vy = (float) Math.sin(rad) * 140 * ramp;
                    break;
                }
                case Enemy.CRANELET: {
                    e.t += dt;
                    // Orbits as it closes in, which makes it hard to line up.
                    float ox = -dy / d;
                    float oy = dx / d;
                    float sp = 230 * ramp;
                    e.vx += ((dx / d * 0.7f + ox * 0.7f) * sp - e.vx) * Math.min(1, dt * 3);
                    e.vy += ((dy / d * 0.7f + oy * 0.7f) * sp - e.vy) * Math.min(1, dt * 3);
                    e.angle = deg(e.vx, e.vy);
                    break;
                }
                case Enemy.SNIPPER:
                    e.t += dt;
                    if (e.u == 0) {
                        // Lining up: slow, turning to face the quill.
                        e.angle = turn(e.angle, deg(dx, dy), 240 * dt);
                        e.vx *= 0.9f;
                        e.vy *= 0.9f;
                        if (e.t > 1.1f) {
                            e.u = 1;
                            e.t = 0;
                            float rad = (float) Math.toRadians(e.angle);
                            e.vx = (float) Math.cos(rad) * 720;
                            e.vy = (float) Math.sin(rad) * 720;
                        }
                    } else if (e.t > 0.55f) {
                        e.u = 0;
                        e.t = 0;
                    }
                    break;
                case Enemy.INKWELL:
                    pull(e, dt);
                    e.angle += 20 * dt;
                    e.vx = dx / d * 30;
                    e.vy = dy / d * 30;
                    if (e.u >= 8) {
                        burstInkwell(e);
                    }
                    break;
                case Enemy.GATE:
                    e.angle += e.u * dt;
                    break;
                default:
                    break;
            }
            e.x = clamp(e.x + e.vx * dt, e.r * 0.5f, WIDTH - e.r * 0.5f);
            e.y = clamp(e.y + e.vy * dt, e.r * 0.5f, HEIGHT - e.r * 0.5f);
        }
        // Creatures of a kind push apart a little, so a crowd reads as many
        // shapes instead of one smudge.
        int n = enemies.size();
        for (int i = 0; i < n; i++) {
            Enemy a = enemies.get(i);
            if (!a.live() || a.kind == Enemy.GATE) {
                continue;
            }
            for (int j = i + 1; j < n; j++) {
                Enemy b = enemies.get(j);
                if (!b.live() || b.kind != a.kind) {
                    continue;
                }
                float dx = b.x - a.x;
                float dy = b.y - a.y;
                float min = (a.r + b.r) * 0.8f;
                float d2 = dx * dx + dy * dy;
                if (d2 < min * min && d2 > 0.01f) {
                    float d = (float) Math.sqrt(d2);
                    float push = (min - d) * 0.5f / d;
                    a.x -= dx * push;
                    a.y -= dy * push;
                    b.x += dx * push;
                    b.y += dy * push;
                }
            }
        }
    }

    /// An inkwell drinks: it pulls the quill a little, ink drops and pinwheels
    /// a lot, bends pencil strokes, and swallows pinwheels that reach it.
    private void pull(Enemy w, float dt) {
        if (w.age % 0.25f < dt) {
            sheet.poke(w.x, w.y, 220, -60);
        }
        if (shipAlive) {
            float dx = w.x - shipX;
            float dy = w.y - shipY;
            float d2 = dx * dx + dy * dy;
            if (d2 < 360 * 360) {
                float d = (float) Math.sqrt(d2) + 1;
                shipX += dx / d * 90 * dt;
                shipY += dy / d * 90 * dt;
            }
        }
        for (Drop p : drops) {
            float dx = w.x - p.x;
            float dy = w.y - p.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy) + 1;
            if (d < 300) {
                p.vx += dx / d * 500 * dt;
                p.vy += dy / d * 500 * dt;
            }
        }
        for (Shot s : shots) {
            float dx = w.x - s.x;
            float dy = w.y - s.y;
            float d2 = dx * dx + dy * dy;
            if (d2 < 200 * 200) {
                float d = (float) Math.sqrt(d2) + 1;
                s.vx += dx / d * 2400 * dt;
                s.vy += dy / d * 2400 * dt;
                s.angle = deg(s.vx, s.vy);
            }
        }
        for (Enemy e : enemies) {
            if (e == w || !e.live() || e.kind != Enemy.PINWHEEL) {
                continue;
            }
            float dx = w.x - e.x;
            float dy = w.y - e.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy) + 1;
            if (d < 320) {
                e.vx += dx / d * 260 * dt;
                e.vy += dy / d * 260 * dt;
            }
            if (d < w.r) {
                e.dead = true;
                w.u += 1;
                w.r = Enemy.RADIUS[Enemy.INKWELL] + w.u * 2;
                if (listener != null) {
                    listener.burst(e.x, e.y, Enemy.COLOUR[Enemy.INKWELL], 12, false);
                }
            }
        }
    }

    private void burstInkwell(Enemy w) {
        w.dead = true;
        sheet.poke(w.x, w.y, 300, 600);
        if (listener != null) {
            listener.burst(w.x, w.y, Enemy.COLOUR[Enemy.INKWELL], w.r, true);
        }
        for (int i = 0; i < 7; i++) {
            double t = Math.PI * 2 * i / 7;
            Enemy d = spawn(Enemy.DART, w.x + (float) Math.cos(t) * 30, w.y + (float) Math.sin(t) * 30);
            d.age = SPAWN_TIME - 0.15f;
            d.angle = (float) Math.toDegrees(t);
        }
    }

    // ---- pencil strokes and ink drops ---------------------------------------------------

    private void stepShots(float dt) {
        for (Shot s : shots) {
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            if (s.x < 0 || s.y < 0 || s.x > WIDTH || s.y > HEIGHT) {
                s.dead = true;
                sheet.poke(clamp(s.x, 0, WIDTH), clamp(s.y, 0, HEIGHT), 50, 60);
            }
        }
    }

    private void stepDrops(float dt) {
        for (Drop p : drops) {
            p.life -= dt;
            if (p.life <= 0) {
                p.dead = true;
                continue;
            }
            if (shipAlive) {
                float dx = shipX - p.x;
                float dy = shipY - p.y;
                float d2 = dx * dx + dy * dy;
                if (d2 < 110 * 110) {
                    float d = (float) Math.sqrt(d2) + 1;
                    p.vx += dx / d * 1800 * dt;
                    p.vy += dy / d * 1800 * dt;
                }
                if (d2 < 26 * 26) {
                    p.dead = true;
                    multiplier = Math.min(MAX_MULTIPLIER, multiplier + 1);
                    if (listener != null) {
                        listener.picked(p.x, p.y, multiplier);
                    }
                    continue;
                }
            }
            p.vx *= 1 - Math.min(1, dt * 2.5f);
            p.vy *= 1 - Math.min(1, dt * 2.5f);
            p.x = clamp(p.x + p.vx * dt, 8, WIDTH - 8);
            p.y = clamp(p.y + p.vy * dt, 8, HEIGHT - 8);
        }
    }

    // ---- collisions -------------------------------------------------------------------

    private void collide() {
        for (Shot s : shots) {
            if (s.dead) {
                continue;
            }
            for (int i = 0; i < enemies.size(); i++) { // NOPMD ForLoopCanBeForeach - grows while walked
                Enemy e = enemies.get(i);
                if (!e.live() || e.kind == Enemy.GATE) {
                    continue;
                }
                float dx = e.x - s.x;
                float dy = e.y - s.y;
                float rr = e.r + 6;
                if (dx * dx + dy * dy < rr * rr) {
                    s.dead = true;
                    e.hp--;
                    if (e.hp <= 0) {
                        kill(e, 1, true);
                    } else {
                        sheet.poke(e.x, e.y, 60, 120);
                        if (listener != null) {
                            listener.hit(s.x, s.y);
                        }
                    }
                    break;
                }
            }
        }
        if (!shipAlive) {
            return;
        }
        for (int i = 0; i < enemies.size(); i++) { // NOPMD ForLoopCanBeForeach - grows while walked
            Enemy e = enemies.get(i);
            if (!e.live()) {
                continue;
            }
            if (e.kind == Enemy.GATE) {
                gate(e);
                continue;
            }
            float dx = e.x - shipX;
            float dy = e.y - shipY;
            float rr = e.r * 0.8f + SHIP_RADIUS;
            if (dx * dx + dy * dy < rr * rr && shield <= 0) {
                loseQuill();
                return;
            }
        }
    }

    /// A gate is two posts and the strip between them. Touching a post folds
    /// the quill; crossing the strip bursts the gate and cuts up everything
    /// near it.
    private void gate(Enemy g) {
        float rad = (float) Math.toRadians(g.angle);
        float hx = (float) Math.cos(rad) * g.r;
        float hy = (float) Math.sin(rad) * g.r;
        for (int end = -1; end <= 1; end += 2) {
            float dx = g.x + hx * end - shipX;
            float dy = g.y + hy * end - shipY;
            if (dx * dx + dy * dy < (SHIP_RADIUS + 9) * (SHIP_RADIUS + 9) && shield <= 0) {
                loseQuill();
                return;
            }
        }
        // Distance from the quill to the strip, and where along it.
        float px = shipX - g.x;
        float py = shipY - g.y;
        float along = (px * hx + py * hy) / (g.r * g.r);
        float cross = Math.abs(px * hy - py * hx) / g.r;
        if (along > -0.85f && along < 0.85f && cross < SHIP_RADIUS) {
            g.dead = true;
            gates++;
            multiplier = Math.min(MAX_MULTIPLIER, multiplier + 1);
            sheet.poke(g.x, g.y, 340, 700);
            if (listener != null) {
                listener.burst(g.x, g.y, Enemy.COLOUR[Enemy.GATE], g.r, true);
            }
            addScore(250);
            for (int i = 0; i < enemies.size(); i++) { // NOPMD ForLoopCanBeForeach - grows while walked
                Enemy e = enemies.get(i);
                if (e.live() && e.kind != Enemy.GATE) {
                    float dx = e.x - g.x;
                    float dy = e.y - g.y;
                    if (dx * dx + dy * dy < 230 * 230) {
                        kill(e, 1, true);
                    }
                }
            }
        }
    }

    /// Cuts a creature up: points at `scoreFactor`, a burst, ink drops if
    /// `dropInk`, and whatever it leaves behind.
    private void kill(Enemy e, int scoreFactor, boolean dropInk) {
        e.dead = true;
        kills++;
        addScore(Enemy.POINTS[e.kind] / scoreFactor);
        boolean big = e.kind == Enemy.INKWELL || e.kind == Enemy.SNIPPER || e.kind == Enemy.CRANE;
        sheet.poke(e.x, e.y, big ? 220 : 120, big ? 500 : 260);
        if (listener != null) {
            listener.burst(e.x, e.y, Enemy.COLOUR[e.kind], e.r, big);
        }
        if (dropInk) {
            int n = e.kind == Enemy.INKWELL ? 8 : e.kind == Enemy.PINWHEEL ? 1 : 2;
            for (int i = 0; i < n; i++) {
                Drop p = new Drop();
                p.x = e.x;
                p.y = e.y;
                float a = rnd() * 6.2832f;
                float sp = 60 + rnd() * 120;
                p.vx = (float) Math.cos(a) * sp;
                p.vy = (float) Math.sin(a) * sp;
                drops.add(p);
            }
        }
        if (e.kind == Enemy.CRANE) {
            // It unfolds into two cranelets, flung out to either side.
            float rad = (float) Math.toRadians(e.angle + 90);
            for (int s = -1; s <= 1; s += 2) {
                Enemy c = spawn(Enemy.CRANELET, e.x + (float) Math.cos(rad) * 20 * s, e.y + (float) Math.sin(rad) * 20 * s);
                c.age = SPAWN_TIME;
                c.vx = (float) Math.cos(rad) * 260 * s;
                c.vy = (float) Math.sin(rad) * 260 * s;
            }
        }
    }

    private void addScore(int points) {
        score += (long) points * multiplier;
        if (score >= nextLife) {
            // Each extra quill costs twice the last: at a full multiplier a flat
            // step came round every few dozen cuts and a good run never ended.
            nextLife = nextLife * 2 + EXTRA_LIFE_EVERY;
            if (mode != FOLD) {
                lives = Math.min(MAX_LIVES, lives + 1);
                bombs = Math.min(MAX_BOMBS, bombs + 1);
                if (listener != null) {
                    listener.extraLife();
                }
            }
        }
    }

    private void compact() {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (e.dead) {
                enemies.remove(i);
                pool.add(e);
            }
        }
        for (int i = shots.size() - 1; i >= 0; i--) {
            if (shots.get(i).dead) {
                shots.remove(i);
            }
        }
        for (int i = drops.size() - 1; i >= 0; i--) {
            if (drops.get(i).dead) {
                drops.remove(i);
            }
        }
    }

    // ---- arithmetic ----------------------------------------------------------------------

    /// A xorshift generator: the daily mode hands every player the same seed,
    /// and so the same order of creatures, whatever the platform's Random does.
    float rnd() {
        seed ^= seed << 13;
        seed ^= seed >>> 7;
        seed ^= seed << 17;
        return (float) ((seed >>> 40) & 0xffffff) / 0x1000000;
    }

    static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    /// The direction of (x, y) in degrees clockwise from +x (y points down).
    static float deg(float x, float y) {
        return (float) Math.toDegrees(MathUtil.atan2(y, x));
    }

    /// Turns `from` toward `to` by at most `max` degrees, the short way round.
    static float turn(float from, float to, float max) {
        float d = to - from;
        while (d > 180) {
            d -= 360;
        }
        while (d < -180) {
            d += 360;
        }
        if (d > max) {
            d = max;
        } else if (d < -max) {
            d = -max;
        }
        return from + d;
    }
}
