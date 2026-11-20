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
package com.codenameone.examples.papercut.play;

import com.codename1.gaming.GameInput;
import com.codename1.gaming.GameView;
import com.codename1.gaming.Gamepad;
import com.codename1.gaming.Gamepads;
import com.codename1.gaming.Sprite;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import java.util.ArrayList;
import java.util.List;

/// Draws an [Arena] as cut paper on a notebook page, and feeds it the player's
/// thumbs, keys or gamepad.
///
/// Everything is a [Sprite] from `com.codename1.gaming`: the ruled lines are
/// stretched one-pixel sprites strung between the points of the [Sheet], so
/// they ripple when the sheet does; every creature is drawn twice, the first
/// time tinted black and offset, which is the shadow that makes it read as a
/// cut-out lying on the page; and every kill leaves an ink stain, so a long
/// run turns the page into a mess of colour.
///
/// The arena is stepped at a fixed 60 Hz from [#update(double)], on the render
/// thread. The [Hud] reads it from the EDT; it only reads numbers, and a frame
/// that shows a score one step old is not worth a lock.
public class ArenaView extends GameView implements Arena.Listener {
    /// The paper colours: ruled lines, the margin, graphite.
    static final int RULE = 0x6f9fd8;
    static final int MARGIN = 0xe0706a;
    static final int GRAPHITE = 0x3a3f4a;
    static final int DESK = 0x8f6e4c;
    private static final int MAX_STAINS = 170;
    private static final int MAX_SCRAPS = 520;

    /// What the screen around the arena wants to hear.
    public interface Host {
        /// The run ended; called on the EDT.
        void runOver(Arena arena);

        /// Something on the HUD changed; called on the render thread.
        void hudChanged();
    }

    private Arena arena;
    private Bot autopilot;
    private Host host;
    private float zoom = 1;
    private float camX = Arena.WIDTH / 2;
    private float camY = Arena.HEIGHT / 2;
    private float shake;
    private float time;
    private boolean overSent;
    private int hudTick;

    // ---- the thumbs ------------------------------------------------------------
    /// The two floating sticks: a thumb lands anywhere on its half and the
    /// stick is centred where it landed. Written on the EDT by the pointer
    /// callbacks, read on the render thread.
    public final Stick move = new Stick();
    public final Stick aim = new Stick();

    /// One floating thumb stick, in view pixels.
    public static final class Stick {
        public boolean active;
        public float cx;
        public float cy;
        public float x;
        public float y;

        public float axisX(float radius) {
            return active ? clampUnit((x - cx) / radius) : 0;
        }

        public float axisY(float radius) {
            return active ? clampUnit((y - cy) / radius) : 0;
        }

        private static float clampUnit(float v) {
            return v > 1 ? 1 : v < -1 ? -1 : v;
        }
    }

    // ---- sprites -----------------------------------------------------------------
    private final List<Pool> pools = new ArrayList<Pool>();
    private final Pool lines = pool("fx_px", 1);
    private final Pool paper = pool("tex_sheet", 0);
    private final Pool desk = pool("tex_kraft", -2);
    private final Pool slab = pool("fx_px", -1);
    private final Pool[] stains = new Pool[6];
    private final Pool flat = pool("fx_px", 9);

    /// A stain is (x, y, rotation, scale, colour, variant); a ring buffer, so
    /// the oldest soak away when the page is full.
    private final float[][] stainData = new float[MAX_STAINS][];
    private int stainNext;

    private final List<Scrap> scraps = new ArrayList<Scrap>();

    /// A torn bit of paper or a drop of ink, flying.
    static final class Scrap {
        String image;
        float x;
        float y;
        float vx;
        float vy;
        float rot;
        float spin;
        float life;
        float max;
        float size;
        int colour;
        boolean ink;
    }

    /// A set of sprites sharing one image and layer, handed out each frame;
    /// the ones not handed out this frame are hidden, never removed, so the
    /// scene is not re-sorted every frame.
    private final class Pool {
        final String image;
        final int z;
        final List<Sprite> sprites = new ArrayList<Sprite>();
        int used;

        Pool(String image, int z) {
            this.image = image;
            this.z = z;
        }

        Sprite take() {
            Sprite s;
            if (used < sprites.size()) {
                s = sprites.get(used);
            } else {
                s = new Sprite(Art.get(image));
                s.setZOrder(z);
                s.setAnchor(0.5, 0.5);
                sprites.add(s);
                getScene().add(s);
            }
            used++;
            s.setVisible(true);
            s.setRotation(0);
            s.setAnchor(0.5, 0.5);
            s.setColor(0xffffffff);
            return s;
        }

        void end() {
            for (int i = used; i < sprites.size(); i++) {
                sprites.get(i).setVisible(false);
            }
            used = 0;
        }
    }

    private Pool pool(String image, int z) {
        Pool p = new Pool(image, z);
        pools.add(p);
        return p;
    }

    private final java.util.Map<String, Pool[]> layered = new java.util.HashMap<String, Pool[]>();

    /// The shadow pool (layer `z`) and the paper pool (`z + 1`) for an image.
    private Pool[] cut(String image, int z) {
        String key = image + "@" + z;
        Pool[] p = layered.get(key);
        if (p == null) {
            p = new Pool[] {pool(image, z), pool(image, z + 1)};
            layered.put(key, p);
        }
        return p;
    }

    public ArenaView(Arena arena) {
        setName("arena");
        for (int i = 0; i < stains.length; i++) {
            stains[i] = pool("ink_" + i, 2);
        }
        setFixedTimestep(Arena.STEP);
        setArena(arena);
    }

    public void setHost(Host host) {
        this.host = host;
    }

    /// Replaces the run being drawn: a new game, or the menu's background run.
    public final void setArena(Arena arena) {
        this.arena = arena;
        arena.setListener(this);
        overSent = false;
        stainNext = 0;
        for (int i = 0; i < stainData.length; i++) {
            stainData[i] = null;
        }
        scraps.clear();
        camX = arena.shipX;
        camY = arena.shipY;
    }

    public Arena getArena() {
        return arena;
    }

    /// Lets the [Bot] fly instead of the player; null hands control back.
    public void setAutopilot(Bot bot) {
        autopilot = bot;
    }

    public float getZoom() {
        return zoom;
    }

    @Override
    protected void onSetup(GraphicsDevice d) {
        setClearColor(0xff000000 | DESK);
    }

    // ---- the frame ----------------------------------------------------------------

    @Override
    protected void update(double deltaSeconds) {
        float dt = (float) deltaSeconds;
        time += dt;
        if (autopilot != null) {
            autopilot.think(arena, dt);
        } else {
            readControls();
        }
        arena.step(dt);
        if (arena.over && !overSent && host != null) {
            overSent = true;
            final Arena done = arena;
            CN.callSerially(() -> host.runOver(done));
        }
        stepScraps(dt);
        layout(dt);
        draw();
        if (host != null && ++hudTick % 4 == 0) {
            host.hudChanged();
        }
    }

    /// Fits about 1150 by 700 sheet units on screen, whatever its shape, and
    /// follows the quill, but never so far that the page leaves the screen
    /// by more than a margin of desk.
    private void layout(float dt) {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        zoom = Math.max(w / 1150f, h / 700f);
        float halfW = w / zoom / 2;
        float halfH = h / zoom / 2;
        float m = 70;
        float tx = arena.shipAlive ? arena.shipX : Arena.WIDTH / 2;
        float ty = arena.shipAlive ? arena.shipY : Arena.HEIGHT / 2;
        tx = halfW * 2 > Arena.WIDTH + 2 * m ? Arena.WIDTH / 2 : Arena.clamp(tx, halfW - m, Arena.WIDTH - halfW + m);
        ty = halfH * 2 > Arena.HEIGHT + 2 * m ? Arena.HEIGHT / 2 : Arena.clamp(ty, halfH - m, Arena.HEIGHT - halfH + m);
        float k = Math.min(1, dt * 5);
        camX += (tx - camX) * k;
        camY += (ty - camY) * k;
        shake = Math.max(0, shake - dt * 30);
    }

    private float sx(float x) {
        float jitter = shake > 0 ? (float) Math.sin(time * 91) * shake : 0;
        return (x - camX + jitter) * zoom + getWidth() / 2f;
    }

    private float sy(float y) {
        float jitter = shake > 0 ? (float) Math.cos(time * 73) * shake : 0;
        return (y - camY + jitter) * zoom + getHeight() / 2f;
    }

    private void draw() {
        drawPage();
        drawStains();
        drawDrops();
        drawEnemies();
        drawShots();
        drawQuill();
        drawScraps();
        drawFlash();
        for (Pool p : pools) {
            p.end();
        }
    }

    private void drawPage() {
        // The desk: kraft paper tiles under everything.
        float tile = 512 * zoom;
        float ox = sx(0) % tile;
        if (ox > 0) {
            ox -= tile;
        }
        float oy = sy(0) % tile;
        if (oy > 0) {
            oy -= tile;
        }
        for (float y = oy; y < getHeight(); y += tile) {
            for (float x = ox; x < getWidth(); x += tile) {
                Sprite s = desk.take();
                s.setAnchor(0, 0);
                s.setPosition(x, y);
                s.setSize(tile + 1, tile + 1);
                s.setColor(0xffd8c8b0);
            }
        }
        // The page's shadow on the desk, then the page.
        Sprite sh = slab.take();
        sh.setAnchor(0, 0);
        sh.setPosition(sx(0) + 10 * zoom, sy(0) + 14 * zoom);
        sh.setSize(Arena.WIDTH * zoom, Arena.HEIGHT * zoom);
        sh.setColor(0x50000000);
        int tx = 4;
        int ty = 3;
        float pw = Arena.WIDTH / tx;
        float ph = Arena.HEIGHT / ty;
        for (int j = 0; j < ty; j++) {
            for (int i = 0; i < tx; i++) {
                Sprite s = paper.take();
                s.setAnchor(0, 0);
                s.setPosition(sx(i * pw), sy(j * ph));
                s.setSize(pw * zoom + 1, ph * zoom + 1);
            }
        }
        // The ruled lines, strung between the sheet's points.
        Sheet sheet = arena.sheet;
        float thick = Math.max(1, 1.6f * zoom);
        int vw = getWidth();
        int vh = getHeight();
        for (int r = 1; r < sheet.rows - 1; r++) {
            float yr = sy(r * sheet.spacing);
            if (yr < -80 || yr > vh + 80) {
                continue;
            }
            for (int c = 0; c < sheet.cols - 1; c++) {
                float x0 = sx(sheet.x(c, r));
                float x1 = sx(sheet.x(c + 1, r));
                if (x1 < -10 || x0 > vw + 10) {
                    continue;
                }
                segment(x0, sy(sheet.y(c, r)), x1, sy(sheet.y(c + 1, r)), thick, 0xa0000000 | RULE);
            }
        }
        int mc = 3;
        for (int r = 0; r < sheet.rows - 1; r++) {
            segment(sx(sheet.x(mc, r)), sy(sheet.y(mc, r)), sx(sheet.x(mc, r + 1)), sy(sheet.y(mc, r + 1)),
                    thick * 1.4f, 0xb0000000 | MARGIN);
        }
    }

    private void segment(float x0, float y0, float x1, float y1, float thick, int argb) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) {
            return;
        }
        Sprite s = lines.take();
        s.setAnchor(0, 0.5);
        s.setPosition(x0, y0);
        s.setSize(len + 0.5f, thick);
        s.setRotation(Arena.deg(dx, dy));
        s.setColor(argb);
    }

    private void drawStains() {
        for (float[] st : stainData) {
            if (st == null) {
                continue;
            }
            float x = sx(st[0]);
            float y = sy(st[1]);
            float size = st[3] * zoom;
            if (x < -size || y < -size || x > getWidth() + size || y > getHeight() + size) {
                continue;
            }
            Sprite s = stains[(int) st[5]].take();
            s.setPosition(x, y);
            s.setSize(size, size);
            s.setRotation(st[2]);
            s.setColor(((int) st[4]) | 0x78000000);
        }
    }

    private void drawDrops() {
        Pool[] p = cut("p_drop", 3);
        for (Drop d : arena.drops) {
            if (d.life < 1.6f && (int) (d.life * 8) % 2 == 0) {
                continue;
            }
            float bob = (float) Math.sin(time * 6 + d.x) * 2;
            put(p, sx(d.x), sy(d.y) + bob * zoom, 22, 22, 0, 0xffffffff, 3);
        }
    }

    private void drawEnemies() {
        for (Enemy e : arena.enemies) {
            if (e.dead) {
                continue;
            }
            if (e.kind == Enemy.GATE) {
                drawGate(e);
                continue;
            }
            float drawn = Math.min(1, e.age / Arena.SPAWN_TIME);
            float size = e.r * (e.kind == Enemy.INKWELL ? 2.6f : 2.9f);
            float scale = 0.3f + 0.7f * drawn;
            float rot = e.angle;
            int alpha = (int) (255 * drawn);
            float wob = e.kind == Enemy.INKWELL ? 1 + (float) Math.sin(time * 5) * 0.06f : 1;
            Pool[] p = cut(Art.enemyImage(e), 5);
            put(p, sx(e.x), sy(e.y), size * scale * wob, size * scale / wob, rot, (alpha << 24) | 0xffffff,
                    drawn < 1 ? 1 : 5);
            if (drawn < 1) {
                // The pencil circle it is being drawn inside.
                Sprite ring = cut("fx_ring", 4)[1].take();
                float rs = size * (2 - drawn) * zoom;
                ring.setPosition(sx(e.x), sy(e.y));
                ring.setSize(rs, rs);
                ring.setColor(((int) (160 * (1 - drawn)) << 24) | GRAPHITE);
            }
        }
    }

    private void drawGate(Enemy g) {
        float drawn = Math.min(1, g.age / Arena.SPAWN_TIME);
        float rad = (float) Math.toRadians(g.angle);
        float hx = (float) Math.cos(rad) * g.r;
        float hy = (float) Math.sin(rad) * g.r;
        int a = (int) (230 * drawn);
        Sprite strip = flat.take();
        strip.setPosition(sx(g.x), sy(g.y));
        strip.setSize(g.r * 2 * zoom, 7 * zoom);
        strip.setRotation(g.angle);
        strip.setColor((a << 24) | 0x9fd0e8);
        Pool[] p = cut("fx_dot", 6);
        for (int s = -1; s <= 1; s += 2) {
            put(p, sx(g.x + hx * s), sy(g.y + hy * s), 22, 22, 0, (a << 24) | 0x2f6fa0, 3);
        }
    }

    private void drawShots() {
        Pool[] p = cut("fx_stroke", 7);
        for (Shot s : arena.shots) {
            put(p, sx(s.x), sy(s.y), 26, 8, s.angle, 0xff000000 | GRAPHITE, 2);
        }
    }

    private void drawQuill() {
        if (!arena.shipAlive) {
            return;
        }
        Pool[] p = cut("quill", 9);
        float bank = 1 - Math.min(0.25f, Math.abs(arena.shipVx * arena.shipVy) / 400000f);
        put(p, sx(arena.shipX), sy(arena.shipY), 50, 50 * bank, arena.shipAngle, 0xffffffff, 7);
        // Where the pencil points.
        float ar = (float) Math.toRadians(arena.aimAngle);
        Sprite dot = cut("fx_dot", 11)[1].take();
        dot.setPosition(sx(arena.shipX + (float) Math.cos(ar) * 46), sy(arena.shipY + (float) Math.sin(ar) * 46));
        dot.setSize(7 * zoom, 7 * zoom);
        dot.setColor(0x90000000 | GRAPHITE);
        if (arena.shield > 0) {
            Sprite ring = cut("fx_ring", 11)[1].take();
            float rs = (64 + (float) Math.sin(time * 14) * 4) * zoom;
            ring.setPosition(sx(arena.shipX), sy(arena.shipY));
            ring.setSize(rs, rs);
            ring.setColor(((int) (200 * Math.min(1, arena.shield)) << 24) | RULE);
        }
    }

    /// Draws one cut-out: its shadow `lift` sheet units down and right, then
    /// the paper.
    private void put(Pool[] p, float x, float y, float w, float h, float rot, int argb, float lift) {
        Sprite s = p[0].take();
        s.setPosition(x + lift * zoom * 0.7f, y + lift * zoom);
        s.setSize(w * zoom, h * zoom);
        s.setRotation(rot);
        s.setColor((((argb >>> 24) * 80 / 255) << 24));
        Sprite f = p[1].take();
        f.setPosition(x, y);
        f.setSize(w * zoom, h * zoom);
        f.setRotation(rot);
        f.setColor(argb);
    }

    private void drawScraps() {
        for (Scrap s : scraps) {
            float a = Math.min(1, s.life / (s.max * 0.5f));
            Pool[] p = cut(s.image, 12);
            if (s.ink) {
                Sprite d = p[1].take();
                d.setPosition(sx(s.x), sy(s.y));
                d.setSize(s.size * zoom, s.size * zoom);
                d.setColor(((int) (230 * a) << 24) | s.colour);
            } else {
                put(p, sx(s.x), sy(s.y), s.size, s.size, s.rot, ((int) (255 * a) << 24) | s.colour, 4);
            }
        }
    }

    private void drawFlash() {
        float f = arena.eraseFlash();
        if (f <= 0) {
            return;
        }
        Sprite s = flat.take();
        s.setAnchor(0, 0);
        s.setPosition(0, 0);
        s.setSize(getWidth(), getHeight());
        s.setColor(((int) (f / 0.6f * 220) << 24) | 0xfff6ee);
    }

    // ---- effects -------------------------------------------------------------------

    private void stepScraps(float dt) {
        for (int i = scraps.size() - 1; i >= 0; i--) {
            Scrap s = scraps.get(i);
            s.life -= dt;
            if (s.life <= 0) {
                scraps.remove(i);
                continue;
            }
            float drag = 1 - Math.min(1, dt * (s.ink ? 4 : 2.2f));
            s.vx *= drag;
            s.vy *= drag;
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.rot += s.spin * dt;
            s.spin *= drag;
        }
    }

    private void scrap(float x, float y, int colour, float speed, boolean ink) {
        if (scraps.size() >= MAX_SCRAPS) {
            return;
        }
        Scrap s = new Scrap();
        float a = arena.rnd() * 6.2832f;
        float v = speed * (0.3f + arena.rnd());
        s.x = x;
        s.y = y;
        s.vx = (float) Math.cos(a) * v;
        s.vy = (float) Math.sin(a) * v;
        s.rot = arena.rnd() * 360;
        s.spin = (arena.rnd() - 0.5f) * 1400;
        s.max = ink ? 0.5f + arena.rnd() * 0.4f : 0.7f + arena.rnd() * 0.8f;
        s.life = s.max;
        s.size = ink ? 5 + arena.rnd() * 7 : 9 + arena.rnd() * 10;
        s.colour = colour;
        s.ink = ink;
        s.image = ink ? "fx_dot" : "fx_scrap";
        scraps.add(s);
    }

    private void stain(float x, float y, int colour, float size) {
        stainData[stainNext] = new float[] {x, y, arena.rnd() * 360, size * (0.8f + arena.rnd() * 0.6f), colour,
            (int) (arena.rnd() * 6)};
        stainNext = (stainNext + 1) % MAX_STAINS;
    }

    @Override
    public void burst(float x, float y, int colour, float size, boolean big) {
        int n = big ? 30 : 12;
        for (int i = 0; i < n; i++) {
            scrap(x, y, colour, big ? 420 : 280, false);
        }
        for (int i = 0; i < n / 2; i++) {
            scrap(x, y, colour, big ? 520 : 340, true);
        }
        stain(x, y, colour, size * (big ? 4.5f : 3.2f));
        if (big) {
            shake = Math.max(shake, 7);
        }
        Sfx.play(big ? Sfx.SPLASH : (arena.kills % 2 == 0 ? Sfx.POP : Sfx.POP2), big ? 1f : 0.7f,
                0.85f + arena.rnd() * 0.3f);
    }

    @Override
    public void fired(int streams) {
        Sfx.play(Sfx.SHOT, 0.35f, 1.1f + arena.rnd() * 0.2f);
    }

    @Override
    public void hit(float x, float y) {
        scrap(x, y, GRAPHITE, 200, true);
        Sfx.play(Sfx.HIT, 0.5f, 1.2f);
    }

    @Override
    public void spawned(Enemy e) {
        if (e.kind == Enemy.INKWELL || e.kind == Enemy.GATE) {
            Sfx.play(Sfx.FLIP, 0.6f, 1f);
        }
    }

    @Override
    public void picked(float x, float y, int multiplier) {
        Sfx.play(Sfx.SPRING, 0.35f, 0.9f + multiplier * 0.04f);
    }

    @Override
    public void quillLost(float x, float y, int livesLeft) {
        for (int i = 0; i < 46; i++) {
            scrap(x, y, 0xfaf7ee, 520, false);
        }
        stain(x, y, GRAPHITE, 150);
        shake = 16;
        Sfx.play(Sfx.TEAR, 1f, 0.8f);
        Sfx.play(Sfx.BOOM, 0.7f, 1f);
        rumble(0.9f, 400);
    }

    @Override
    public void erased(float x, float y) {
        shake = 12;
        Sfx.play(Sfx.FLIP2, 1f, 0.7f);
        rumble(0.6f, 250);
    }

    @Override
    public void extraLife() {
        Sfx.play(Sfx.BELL, 0.8f, 1f);
    }

    @Override
    public void over() {
        Sfx.play(Sfx.TEAR, 1f, 0.6f);
    }

    private static void rumble(float strength, int ms) {
        Gamepad g = Gamepads.getGamepad(0);
        if (g != null && g.isConnected()) {
            g.rumble(strength, strength * 0.6f, ms);
        }
    }

    // ---- controls -------------------------------------------------------------------

    /// The stick radius in pixels: how far a thumb travels for full speed.
    public float stickRadius() {
        return CN.convertToPixels(9);
    }

    /// Thumbs, keys and gamepad, whichever is pushing hardest, become the
    /// arena's controls.
    ///
    /// - Touch: a floating stick on each half -- left moves, right aims and
    ///   fires while held. The eraser is the button the [Hud] draws top right.
    /// - Keyboard: WASD moves, the arrow keys aim and fire, Space erases.
    /// - Mouse (desktop): the pencil follows the pointer and fires while the
    ///   button is held.
    /// - Gamepad: left stick moves, right stick aims and fires, A or either
    ///   shoulder erases.
    private void readControls() {
        GameInput in = getInput();
        float r = stickRadius();
        float mx = move.axisX(r);
        float my = move.axisY(r);
        float ax = aim.axisX(r);
        float ay = aim.axisY(r);
        boolean fire = aim.active && (ax * ax + ay * ay) > 0.09f;

        float kx = (key(in, 'd') ? 1 : 0) - (key(in, 'a') ? 1 : 0);
        float ky = (key(in, 's') ? 1 : 0) - (key(in, 'w') ? 1 : 0);
        if (kx != 0 || ky != 0) {
            mx = kx;
            my = ky;
        }
        float akx = (in.isGameKeyDown(Display.GAME_RIGHT) ? 1 : 0) - (in.isGameKeyDown(Display.GAME_LEFT) ? 1 : 0);
        float aky = (in.isGameKeyDown(Display.GAME_DOWN) ? 1 : 0) - (in.isGameKeyDown(Display.GAME_UP) ? 1 : 0);
        if (akx != 0 || aky != 0) {
            ax = akx;
            ay = aky;
            fire = true;
        }
        if (CN.isDesktop() && !aim.active && !move.active && akx == 0 && aky == 0) {
            ax = in.getPointerX() - sx(arena.shipX);
            ay = in.getPointerY() - sy(arena.shipY);
            fire = in.isPointerDown();
        }
        Gamepad g = Gamepads.getGamepad(0);
        boolean bomb = in.wasKeyPressed(' ') || bombTapped;
        bombTapped = false;
        if (g != null && g.isConnected()) {
            float gx = g.getAxis(Gamepad.AXIS_LEFT_X);
            float gy = g.getAxis(Gamepad.AXIS_LEFT_Y);
            if (gx * gx + gy * gy > 0.02f) {
                mx = gx;
                my = gy;
            }
            float rx = g.getAxis(Gamepad.AXIS_RIGHT_X);
            float ry = g.getAxis(Gamepad.AXIS_RIGHT_Y);
            if (rx * rx + ry * ry > 0.09f) {
                ax = rx;
                ay = ry;
                fire = true;
            }
            bomb |= in.wasGamepadButtonPressed(Gamepad.BUTTON_A)
                    || in.wasGamepadButtonPressed(Gamepad.BUTTON_LEFT_SHOULDER)
                    || in.wasGamepadButtonPressed(Gamepad.BUTTON_RIGHT_SHOULDER);
        }
        arena.moveX = mx;
        arena.moveY = my;
        arena.aimX = ax;
        arena.aimY = ay;
        arena.firing = fire;
        if (bomb) {
            arena.bombPressed = true;
        }
    }

    private static boolean key(GameInput in, char c) {
        return in.isKeyDown(c) || in.isKeyDown(Character.toUpperCase(c));
    }

    private boolean bombTapped;

    /// The eraser button, in view pixels: centre x, centre y, radius. The [Hud]
    /// draws it here and the touch handling keeps thumbs off it.
    public float[] eraserButton() {
        float r = CN.convertToPixels(6);
        return new float[] {getWidth() - r * 1.6f, r * 1.9f, r};
    }

    @Override
    public void pointerPressed(int[] x, int[] y) {
        super.pointerPressed(x, y);
        float[] b = eraserButton();
        for (int i = 0; i < x.length; i++) {
            float lx = x[i] - getAbsoluteX();
            float ly = y[i] - getAbsoluteY();
            float dx = lx - b[0];
            float dy = ly - b[1];
            if (dx * dx + dy * dy < b[2] * b[2] * 1.4f) {
                bombTapped = true;
                continue;
            }
            Stick s = lx < getWidth() / 2f ? move : aim;
            if (!s.active) {
                s.active = true;
                s.cx = lx;
                s.cy = ly;
            }
            s.x = lx;
            s.y = ly;
        }
    }

    @Override
    public void pointerDragged(int[] x, int[] y) {
        super.pointerDragged(x, y);
        boolean left = false;
        boolean right = false;
        for (int i = 0; i < x.length; i++) {
            float lx = x[i] - getAbsoluteX();
            float ly = y[i] - getAbsoluteY();
            Stick s = nearest(lx, ly);
            if (s == null) {
                continue;
            }
            s.x = lx;
            s.y = ly;
            left |= s == move;
            right |= s == aim;
        }
        // A thumb missing from a multi-touch drag has lifted.
        if (x.length > 1) {
            move.active &= left;
            aim.active &= right;
        }
    }

    @Override
    public void pointerReleased(int[] x, int[] y) {
        super.pointerReleased(x, y);
        if (x.length == 1) {
            Stick s = nearest(x[0] - getAbsoluteX(), y[0] - getAbsoluteY());
            if (s != null) {
                s.active = false;
            }
        } else {
            move.active = false;
            aim.active = false;
        }
    }

    /// The active stick a touch at (x, y) belongs to: the one whose thumb was
    /// last nearest, so a thumb that strays over the middle keeps its stick.
    private Stick nearest(float x, float y) {
        Stick best = null;
        float bd = Float.MAX_VALUE;
        Stick[] both = {move, aim};
        for (Stick s : both) {
            if (!s.active) {
                continue;
            }
            float dx = x - s.x;
            float dy = y - s.y;
            float d = dx * dx + dy * dy;
            if (d < bd) {
                bd = d;
                best = s;
            }
        }
        return best;
    }

    /// Screen position of a sheet point, for the HUD.
    public float screenX(float x) {
        return sx(x);
    }

    public float screenY(float y) {
        return sy(y);
    }
}
