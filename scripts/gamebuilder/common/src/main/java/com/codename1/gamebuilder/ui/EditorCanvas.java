/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.gamebuilder.ui;

import com.codename1.gamebuilder.editor.EditorController;
import com.codename1.gamebuilder.editor.EditorModel;
import com.codename1.gaming.level.AssetCatalog;
import com.codename1.gaming.level.AssetDef;
import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameLevel;
import com.codename1.gaming.level.Layer;
import com.codename1.gaming.SpriteSheet;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import java.util.List;

/// The editing surface: paints the level's scene background, grid, tile layers, placed
/// elements and the selection, and routes pointer gestures to the `EditorController`
/// for the active tool. Supports zoom/pan and a grid-free `#setPlayMode(boolean)`
/// rendering used by the live preview.
public class EditorCanvas extends Component {
    private static final int ACCENT = 0xff4D86FF;

    private final EditorController controller;
    private Runnable onChange = () -> {
    };

    private double zoom = 0.8;
    private int panX = 14;
    private int panY = 10;
    private int lastDragX;
    private int lastDragY;
    private boolean dragging;
    private boolean didDrag;   // true once an actual drag movement happened (vs a plain click)
    private double grabDX;   // world offset cursor->element centre while moving
    private double grabDY;
    private boolean playMode;
    private boolean gridVisible = true;
    // 3D maps EDIT in flat top-down by default (precise grid placement); perspective is an
    // opt-in preview via the View menu. (Play always shows the first-person 3D view.)
    private boolean topDown3D = true;
    private double playClock;

    // play physics
    private GameElement player;
    private double playerStartX;
    private double playerStartY;
    private double vx;
    private double vy;
    private boolean kLeft;
    private boolean kRight;
    private boolean kUp;
    private boolean kDown;
    // Held-key model: a key goes down on press and stays down until a release that is NOT
    // immediately followed by another press. Desktop auto-repeat delivers press+release pairs
    // (or bare repeats); debouncing the release by a short window keeps a held arrow driving
    // continuous motion instead of stuttering, while a real release still stops promptly.
    private static final double KEY_RELEASE_GRACE = 0.07;
    private double relLeft = -1;   // playClock at which a release is pending; -1 = held / up
    private double relRight = -1;
    private double relUp = -1;
    private double relDown = -1;
    private boolean grounded;
    private boolean jumpRequested;   // edge-triggered so a held/stuck Up doesn't bounce forever
    private double orbit;   // 3D preview camera orbit angle (radians)
    // first-person camera for the 3D preview (recentred board units + facing)
    private double fpX;
    private double fpZ;
    private double fpYaw;
    private double fpPitch;   // flight: nose up/down
    private double fpAlt = 0.6;   // flight: altitude
    private double fpSpeed;   // race: current throttle

    // default-behavior play state (score, lives, collected items, enemy direction)
    private int score;
    private int lives;
    private final java.util.Set<String> collected = new java.util.HashSet<>();
    private final java.util.Map<String, Double> enemyDir = new java.util.HashMap<>();
    private final java.util.Map<String, double[]> startPos = new java.util.HashMap<>();

    // 3D preview camera (set per frame in paintScene3D, read by project3D)
    private final double[] p3eye = new double[3];
    private final double[] p3fwd = new double[3];
    private final double[] p3right = new double[3];
    private final double[] p3up = new double[3];
    private double p3f;
    private int p3cx;
    private int p3cy;

    public EditorCanvas(EditorController controller) {
        this.controller = controller;
        setUIID("EditorCanvas");
        setFocusable(true);
    }

    /// Drag the canvas content with NO distance threshold (the default 3% gate made moving
    /// an element / 3D dragging only start after the pointer travelled a long way).
    @Override
    protected int getDragRegionStatus(int x, int y) {
        return DRAG_REGION_IMMEDIATELY_DRAG_XY;
    }

    /// Keep the gesture even if the pointer wanders off the canvas mid-drag.
    @Override
    protected boolean isStickyDrag() {
        return true;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange == null ? () -> {
        } : onChange;
    }

    public void setPlayMode(boolean playMode) {
        this.playMode = playMode;
        kLeft = kRight = kUp = kDown = false;
        relLeft = relRight = relUp = relDown = -1;
        jumpRequested = false;
        vx = vy = 0;
        grounded = false;
        orbit = 0;
        if (playMode) {
            score = 0;
            lives = 3;
            collected.clear();
            enemyDir.clear();
            startPos.clear();
            for (GameElement el : model().level().elements()) {
                startPos.put(el.getId(), new double[]{el.getX(), el.getY(), el.getZ()});
            }
            player = findPlayer();
            if (player != null) {
                playerStartX = player.getX();
                playerStartY = player.getY();
            }
            // first-person camera starts at the player (or board centre), facing the board
            GameLevel l = model().level();
            double ts0 = Math.max(1, l.getTileSize());
            if (player != null) {
                fpX = player.getX() / ts0 - l.getCols() / 2.0;
                fpZ = player.getY() / ts0 - l.getRows() / 2.0;
            } else {
                fpX = 0;
                fpZ = l.getRows() / 2.0 - 1;   // near the front edge, looking in
            }
            fpYaw = 0;
            fpPitch = 0;
            fpAlt = "flight".equals(view3dType()) ? 6 : 0.6;
            fpSpeed = 0;
            setFocusable(true);
            // grab the arrow keys so the form does not traverse focus on the first press
            // (which left the first key "stuck" and stole input from the app)
            setHandlesInput(true);
            requestFocus();
        } else {
            setHandlesInput(false);
            // restore the level — playing must not mutate the design
            for (GameElement el : model().level().elements()) {
                double[] p = startPos.get(el.getId());
                if (p != null) {
                    el.setPosition(p[0], p[1], p[2]);
                }
            }
            player = null;
            collected.clear();
        }
    }

    private GameElement findPlayer() {
        List<GameElement> els = model().level().elements();
        // an explicit "player" flag wins (lets any element be marked the player)
        for (int i = 0; i < els.size(); i++) {
            if (els.get(i).getBoolean("player", false)) {
                return els.get(i);
            }
        }
        for (int i = 0; i < els.size(); i++) {
            String id = els.get(i).getAssetId();
            if ("player".equals(id) || "hero".equals(id) || "spawn".equals(id)) {
                return els.get(i);
            }
        }
        return null;
    }

    public void setGridVisible(boolean gridVisible) {
        this.gridVisible = gridVisible;
    }

    public boolean isGridVisible() {
        return gridVisible;
    }

    public void setTopDown3D(boolean topDown3D) {
        this.topDown3D = topDown3D;
    }

    public boolean isTopDown3D() {
        return topDown3D;
    }

    /// True when the 3D map should be drawn/edited in perspective (vs flat top-down).
    private boolean perspective3D() {
        return model().level().getMode() == GameLevel.MODE_3D && !topDown3D;
    }

    public boolean isPlayMode() {
        return playMode;
    }

    /// Advances the play animation clock and the player physics (called by the preview
    /// timer).
    public void tick(double seconds) {
        playClock += seconds;
        if (!playMode) {
            return;
        }
        // expire keys whose release has been pending longer than the debounce window
        if (relLeft >= 0 && playClock - relLeft > KEY_RELEASE_GRACE) {
            kLeft = false;
            relLeft = -1;
        }
        if (relRight >= 0 && playClock - relRight > KEY_RELEASE_GRACE) {
            kRight = false;
            relRight = -1;
        }
        if (relUp >= 0 && playClock - relUp > KEY_RELEASE_GRACE) {
            kUp = false;
            relUp = -1;
        }
        if (relDown >= 0 && playClock - relDown > KEY_RELEASE_GRACE) {
            kDown = false;
            relDown = -1;
        }
        if (model().level().getMode() == GameLevel.MODE_3D) {
            stepPlay3D(seconds);
        } else {
            if (player != null) {
                stepPlayer(seconds);
            }
            stepEnemies(seconds);
            if (player != null) {
                checkInteractions();
            }
        }
    }

    private static boolean isEnemy(String id) {
        return id != null && (id.equals("slime") || id.equals("enemy") || id.startsWith("enemy")
                || id.startsWith("npc") || id.equals("exception") || id.equals("bug"));
    }

    private static boolean isCollectible(String id) {
        return id != null && (id.equals("coin") || id.equals("gem") || id.equals("star")
                || id.equals("token") || id.equals("coffee"));
    }

    /// Default enemy behavior: patrol horizontally, turning at walls and level edges.
    private void stepEnemies(double dt) {
        GameLevel level = model().level();
        AssetCatalog cat = model().catalog();
        int ts = Math.max(4, level.getTileSize());
        List<GameElement> els = level.elements();
        for (int i = 0; i < els.size(); i++) {
            GameElement el = els.get(i);
            if (collected.contains(el.getId()) || !isEnemy(el.getAssetId())) {
                continue;
            }
            Double d = enemyDir.get(el.getId());
            double dir = d == null ? 1.0 : d;
            AssetDef def = cat == null ? null : cat.def(el.getAssetId());
            double hw = (def == null ? ts : def.getWidth()) / 2.0;
            double hh = (def == null ? ts : def.getHeight()) / 2.0;
            double spd = el.getDouble("speed", 60);
            double nx = el.getX() + dir * spd * dt;
            if (hitsSolid(level, nx, el.getY(), hw, hh) || nx < hw || nx > level.getCols() * ts - hw) {
                dir = -dir;
            } else {
                el.setX(nx);
            }
            enemyDir.put(el.getId(), dir);
        }
    }

    /// Default interactions: collect coins/gems (score), enemies cost a life and respawn.
    private void checkInteractions() {
        GameLevel level = model().level();
        AssetCatalog cat = model().catalog();
        int ts = Math.max(4, level.getTileSize());
        AssetDef pd = cat == null ? null : cat.def(player.getAssetId());
        double phw = (pd == null ? ts : pd.getWidth()) / 2.0;
        double phh = (pd == null ? ts : pd.getHeight()) / 2.0;
        List<GameElement> els = level.elements();
        for (int i = 0; i < els.size(); i++) {
            GameElement el = els.get(i);
            if (el == player || collected.contains(el.getId())) {
                continue;
            }
            String id = el.getAssetId();
            if (!isCollectible(id) && !isEnemy(id)) {
                continue;
            }
            AssetDef def = cat == null ? null : cat.def(id);
            double hw = (def == null ? ts : def.getWidth()) / 2.0;
            double hh = (def == null ? ts : def.getHeight()) / 2.0;
            boolean hit = Math.abs(player.getX() - el.getX()) < phw + hw
                    && Math.abs(player.getY() - el.getY()) < phh + hh;
            if (!hit) {
                continue;
            }
            if (isCollectible(id)) {
                collected.add(el.getId());
                score += el.getInt("value", 10);
            } else {
                lives--;
                if (lives <= 0) {
                    lives = 3;
                    score = 0;
                    collected.clear();
                }
                player.setPosition(playerStartX, playerStartY);
                vy = 0;
            }
        }
    }

    /// The 3D preview sub-mode: "open" (walk a flat arena), "flight" (steer an aerial
    /// fly-over) or "dungeon" (walk with wall collision against scenery).
    private String view3dType() {
        return model().level().getString("view3d", "open");
    }

    /// 3D preview movement — a distinct prototype per game type:
    ///  - flight: cruise forward, Left/Right bank, Up/Down climb/dive (no gravity, no ground).
    ///  - race:   Up accelerate / Down brake, Left/Right steer along the ground.
    ///  - dungeon: walk with Left/Right turn, Up/Down step, blocked by walls.
    ///  - open:   free walk on a flat arena.
    private void stepPlay3D(double dt) {
        GameLevel level = model().level();
        String type = view3dType();
        double hc = level.getCols() / 2.0;
        double hr = level.getRows() / 2.0;
        int lr = (kRight ? 1 : 0) - (kLeft ? 1 : 0);
        int ud = (kUp ? 1 : 0) - (kDown ? 1 : 0);
        double nx;
        double nz;
        if ("flight".equals(type)) {
            fpYaw += lr * 1.4 * dt;
            fpPitch = Math.max(-0.7, Math.min(0.7, fpPitch + ud * 1.2 * dt));
            double sp = 8.0;
            nx = fpX + Math.cos(fpPitch) * Math.sin(fpYaw) * sp * dt;
            nz = fpZ - Math.cos(fpPitch) * Math.cos(fpYaw) * sp * dt;
            fpAlt = Math.max(0.5, Math.min(40, fpAlt + Math.sin(fpPitch) * sp * dt));
        } else if ("race".equals(type)) {
            fpYaw += lr * 1.8 * dt;
            fpSpeed = Math.max(0, Math.min(11, fpSpeed + ud * 9 * dt)) * 0.992;
            nx = fpX + Math.sin(fpYaw) * fpSpeed * dt;
            nz = fpZ - Math.cos(fpYaw) * fpSpeed * dt;
        } else {
            fpYaw += lr * 1.6 * dt;
            double sp = "dungeon".equals(type) ? 3.0 : 4.5;
            nx = fpX + Math.sin(fpYaw) * ud * sp * dt;
            nz = fpZ - Math.cos(fpYaw) * ud * sp * dt;
        }
        double margin = "flight".equals(type) ? 8 : 1;
        nx = Math.max(-hc - margin, Math.min(hc + margin, nx));
        nz = Math.max(-hr - margin, Math.min(hr + margin, nz));
        boolean grounded = !"flight".equals(type);
        // terrain walls stop any walker; an open hole stops a grounded walker (you'd fall).
        boolean blocked = ("dungeon".equals(type) && blocked3D(nx, nz))
                || (grounded && terrainBlocks(level, nx + hc, nz + hr, true));
        if (blocked) {
            fpSpeed = 0;
            return;
        }
        fpX = nx;
        fpZ = nz;
        if (player != null) {
            double ts = Math.max(1, level.getTileSize());
            player.setPosition((fpX + hc) * ts, (fpZ + hr) * ts);
            check3DPickups();
        }
    }

    /// Dungeon collision: is a recentred board point within half a cell of a solid element?
    private boolean blocked3D(double wx, double wz) {
        GameLevel level = model().level();
        double ts = Math.max(1, level.getTileSize());
        double hc = level.getCols() / 2.0;
        double hr = level.getRows() / 2.0;
        List<GameElement> els = level.elements();
        for (int i = 0; i < els.size(); i++) {
            GameElement el = els.get(i);
            String id = el.getAssetId();
            // never collide with yourself — the player rides at (fpX,fpZ), so without this
            // the walker is always "blocked" by its own element and can't move.
            if (el == player || collected.contains(el.getId()) || isCollectible(id)) {
                continue;
            }
            double ex = el.getX() / ts - hc;
            double ez = el.getY() / ts - hr;
            double dx = ex - wx;
            double dz = ez - wz;
            if (dx * dx + dz * dz < 0.45 * 0.45) {
                return true;
            }
        }
        return false;
    }

    /// 3D contact pickups: collect items and lose a life on enemies within ~one cell.
    private void check3DPickups() {
        GameLevel level = model().level();
        double ts = Math.max(1, level.getTileSize());
        List<GameElement> els = level.elements();
        for (int i = 0; i < els.size(); i++) {
            GameElement el = els.get(i);
            if (el == player || collected.contains(el.getId())) {
                continue;
            }
            String id = el.getAssetId();
            double dxw = (el.getX() - player.getX()) / ts;
            double dzw = (el.getY() - player.getY()) / ts;
            if (dxw * dxw + dzw * dzw > 0.7) {
                continue;
            }
            if (isCollectible(id)) {
                collected.add(el.getId());
                score += el.getInt("value", 10);
            } else if (isEnemy(id)) {
                lives--;
                if (lives <= 0) {
                    lives = 3;
                    score = 0;
                    collected.clear();
                }
                player.setPosition(playerStartX, playerStartY);
            }
        }
    }

    private void stepPlayer(double dt) {
        GameLevel level = model().level();
        AssetCatalog cat = model().catalog();
        int ts = Math.max(4, level.getTileSize());
        AssetDef def = cat == null ? null : cat.def(player.getAssetId());
        double hw = (def == null ? ts : def.getWidth()) / 2.0;
        double hh = (def == null ? ts : def.getHeight()) / 2.0;
        // same tunables the runtime GameSceneView arcade behavior reads, so the
        // preview matches a shipped game: gravity prop, walkSpeed prop, player jumpHeight.
        double speed = level.getDouble("walkSpeed", 170);
        double jump = player.getInt("jumpHeight", 110) * 4;
        double grav = level.getDouble("gravity", 9.8) * 132;

        vx = ((kRight ? 1 : 0) - (kLeft ? 1 : 0)) * speed;
        if (jumpRequested && grounded) {   // one jump per key press (edge-triggered)
            vy = -jump;
            grounded = false;
        }
        jumpRequested = false;
        vy += grav * dt;
        if (vy > 1000) {
            vy = 1000;
        }

        double nx = player.getX() + vx * dt;
        if (!hitsSolid(level, nx, player.getY(), hw, hh)) {
            player.setX(nx);
        }
        double ny = player.getY() + vy * dt;
        if (!hitsSolid(level, player.getX(), ny, hw, hh)) {
            player.setY(ny);
            grounded = false;
        } else {
            if (vy > 0) {
                grounded = true;
            }
            vy = 0;
        }
        double maxX = level.getCols() * ts - hw;
        if (player.getX() < hw) {
            player.setX(hw);
        }
        if (player.getX() > maxX) {
            player.setX(maxX);
        }
        if (player.getY() > level.getRows() * ts + hh * 3) {  // fell off -> respawn
            player.setPosition(playerStartX, playerStartY);
            vy = 0;
        }
    }

    private boolean hitsSolid(GameLevel level, double cx, double cy, double hw, double hh) {
        int ts = Math.max(4, level.getTileSize());
        int c0 = (int) Math.floor((cx - hw + 1) / ts);
        int c1 = (int) Math.floor((cx + hw - 1) / ts);
        int r0 = (int) Math.floor((cy - hh + 1) / ts);
        int r1 = (int) Math.floor((cy + hh - 1) / ts);
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                if (solidCell(level, c, r)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean solidCell(GameLevel level, int col, int row) {
        List<Layer> layers = level.layers();
        for (int i = 0; i < layers.size(); i++) {
            Layer l = layers.get(i);
            // a parallax background layer (clouds, mountains) is decoration, not a wall
            if (l.getKind() == Layer.KIND_TILE && l.isVisible()
                    && l.getParallaxX() == 1f && l.getParallaxY() == 1f && l.getTile(col, row) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void keyPressed(int keyCode) {
        setKeyDown(keyCode, true);
    }

    @Override
    public void keyRepeated(int keyCode) {
        setKeyDown(keyCode, true);   // a repeat is still "held" — cancels any pending release
    }

    @Override
    public void keyReleased(int keyCode) {
        setKeyDown(keyCode, false);   // marks a pending release; debounced in tick()
    }

    private void setKeyDown(int keyCode, boolean down) {
        if (!playMode) {
            return;
        }
        int g = Display.getInstance().getGameAction(keyCode);
        if (g == Display.GAME_LEFT) {
            if (down) { kLeft = true; relLeft = -1; } else { relLeft = playClock; }
        } else if (g == Display.GAME_RIGHT) {
            if (down) { kRight = true; relRight = -1; } else { relRight = playClock; }
        } else if (g == Display.GAME_DOWN) {
            if (down) { kDown = true; relDown = -1; } else { relDown = playClock; }
        } else if (g == Display.GAME_UP || g == Display.GAME_FIRE || keyCode == ' ') {
            if (down) {
                if (!kUp) {
                    jumpRequested = true;   // edge only: a held/repeating Up doesn't re-bounce
                }
                kUp = true;
                relUp = -1;
            } else {
                relUp = playClock;
            }
        }
    }

    public double getZoom() {
        return zoom;
    }

    /// On-screen size of a single grid cell at the current zoom and canvas size. Exposed
    /// for tests so a fired pointer gesture can target a known cell regardless of the
    /// auto-fit (the cell size is decoupled from the game's tile size).
    public double cellSize() {
        return cellPx();
    }

    public void zoomBy(double factor) {
        zoom = Math.max(0.1, Math.min(40.0, zoom * factor));   // large boards need deep zoom
    }

    private EditorModel model() {
        return controller.model();
    }

    /// On-screen size of one cell, auto-fit to the canvas and then scaled by zoom. This
    /// is deliberately decoupled from the game's tile size, so a 3D level (tile size 1
    /// world unit) is just as usable as a 2D level (32 px) and zoom always works.
    private double cellPx() {
        GameLevel level = model().level();
        int pad = 18;
        double aw = Math.max(40, getWidth() - pad * 2);
        double ah = Math.max(40, getHeight() - pad * 2);
        double fit = Math.min(aw / Math.max(1, level.getCols()), ah / Math.max(1, level.getRows()));
        if (fit < 8) {
            fit = 8;
        }
        return fit * zoom;
    }

    /// Element render size in cells, clamped so it always reads as an object on the grid
    /// (a 32-unit mesh on a 1-unit 3D board won't swallow the whole map).
    private double elementCells(GameLevel level, double dim) {
        double cells = dim / Math.max(1, level.getTileSize());
        return Math.max(0.3, Math.min(1.8, cells));
    }

    // ---- painting ------------------------------------------------------------

    @Override
    public void paint(Graphics g) {
        if (playMode) {
            paintPlay(g);
            return;
        }
        GameLevel level = model().level();
        AssetCatalog cat = model().catalog();
        if (perspective3D()) {
            paintEdit3D(g, level, cat);
            return;
        }
        double ts = cellPx();
        int ox = getX() + panX;
        int oy = getY() + panY;
        int lw = (int) Math.round(level.getCols() * ts);
        int lh = (int) Math.round(level.getRows() * ts);

        // dark surround so the editable "stage" (the level rectangle) is obvious
        g.setColor(0x04102A);
        g.fillRect(getX(), getY(), getWidth(), getHeight());
        // the scene background fills only the level rectangle
        paintBackdrop(g, backdrop(level), ox, oy, lw, lh);

        if (level.getMode() == GameLevel.MODE_3D && level.getTerrain() != null) {
            drawTerrainTopDown(g, level, ox, oy, ts);
        }
        drawContent(g, cat, level, ox, oy, ts);
        if (gridVisible) {
            drawGrid(g, level, ox, oy, ts);
        }
        // a clear boundary around the paintable area
        g.setColor(0x4D86FF);
        g.drawRect(ox - 1, oy - 1, lw + 2, lh + 2);
        g.drawRect(ox - 2, oy - 2, lw + 4, lh + 4);

        drawSelection(g, cat, level, ox, oy, ts);
    }

    /// Draws the tile layers and placed elements at the given origin and tile size.
    /// `ts` is the on-screen size of one tile; element scale is derived from it so the
    /// same routine serves the editor (zoom) and the live device preview (fit).
    private void drawContent(Graphics g, AssetCatalog cat, GameLevel level, int ox, int oy, double ts) {
        double scale = ts / Math.max(1, level.getTileSize());
        List<Layer> layers = level.layers();
        for (int li = 0; li < layers.size(); li++) {
            Layer layer = layers.get(li);
            if (layer.getKind() != Layer.KIND_TILE || !layer.isVisible()) {
                continue;
            }
            for (var e : layer.tiles().entrySet()) {
                int[] cr = parseCell(e.getKey());
                if (cr == null) {
                    continue;
                }
                drawAsset(g, cat, e.getValue(),
                        (int) Math.round(ox + cr[0] * ts), (int) Math.round(oy + cr[1] * ts),
                        (int) Math.round(ts), (int) Math.round(ts));
            }
        }
        List<GameElement> els = level.elements();
        for (int i = 0; i < els.size(); i++) {
            GameElement el = els.get(i);
            if (playMode && collected.contains(el.getId())) {
                continue;   // picked up during play
            }
            Layer elLayer = level.getLayer(el.getLayer());
            if (elLayer != null && !elLayer.isVisible()) {
                continue;   // honor the layer's visibility (eye toggle) for entities too
            }
            AssetDef def = cat == null ? null : cat.def(el.getAssetId());
            double esc = el.getScaleX();   // per-instance size
            double w = elementCells(level, def == null ? level.getTileSize() : def.getWidth()) * ts * esc;
            double h = elementCells(level, def == null ? level.getTileSize() : def.getHeight()) * ts * esc;
            double bob = playMode ? playBob(el, def) * scale : 0;
            int x = (int) Math.round(ox + el.getX() * scale - w / 2);
            int y = (int) Math.round(oy + el.getY() * scale - h / 2 + bob);
            drawAsset(g, cat, el.getAssetId(), x, y, (int) Math.round(w), (int) Math.round(h));
        }
    }

    /// Clamps a follow-camera origin to [0, max] (centers when the level is smaller
    /// than the view, i.e. max <= 0).
    private static double clampCam(double v, double max) {
        if (max <= 0) {
            return max / 2;
        }
        return v < 0 ? 0 : (v > max ? max : v);
    }

    /// Draws the level through a scrolling follow-camera, applying each layer's parallax
    /// factor to its scroll offset -- the editor preview of the runtime `SpriteRenderer`
    /// behavior, so a background layer drifts behind the foreground as the player moves.
    private void drawContentScrolling(Graphics g, AssetCatalog cat, GameLevel level,
            int sX, int sY, double scale, double camX, double camY) {
        double ts = level.getTileSize() * scale;
        List<Layer> layers = level.layers();
        for (int li = 0; li < layers.size(); li++) {
            Layer layer = layers.get(li);
            if (layer.getKind() != Layer.KIND_TILE || !layer.isVisible()) {
                continue;
            }
            int ox = (int) Math.round(sX - camX * layer.getParallaxX() * scale);
            int oy = (int) Math.round(sY - camY * layer.getParallaxY() * scale);
            for (var e : layer.tiles().entrySet()) {
                int[] cr = parseCell(e.getKey());
                if (cr == null) {
                    continue;
                }
                drawAsset(g, cat, e.getValue(),
                        (int) Math.round(ox + cr[0] * ts), (int) Math.round(oy + cr[1] * ts),
                        (int) Math.round(ts), (int) Math.round(ts));
            }
        }
        List<GameElement> els = level.elements();
        for (int i = 0; i < els.size(); i++) {
            GameElement el = els.get(i);
            if (collected.contains(el.getId())) {
                continue;
            }
            Layer elLayer = level.getLayer(el.getLayer());
            if (elLayer != null && !elLayer.isVisible()) {
                continue;
            }
            float px = elLayer == null ? 1f : elLayer.getParallaxX();
            float py = elLayer == null ? 1f : elLayer.getParallaxY();
            int ox = (int) Math.round(sX - camX * px * scale);
            int oy = (int) Math.round(sY - camY * py * scale);
            AssetDef def = cat == null ? null : cat.def(el.getAssetId());
            double esc = el.getScaleX();
            double w = elementCells(level, def == null ? level.getTileSize() : def.getWidth()) * ts * esc;
            double h = elementCells(level, def == null ? level.getTileSize() : def.getHeight()) * ts * esc;
            double bob = playBob(el, def) * scale;
            int x = (int) Math.round(ox + el.getX() * scale - w / 2);
            int y = (int) Math.round(oy + el.getY() * scale - h / 2 + bob);
            drawAsset(g, cat, el.getAssetId(), x, y, (int) Math.round(w), (int) Math.round(h));
        }
    }

    /// Top-down terrain map: each cell tinted by ground elevation (dark = low, bright =
    /// high), holes shown as a hatched void, walls as a solid stone block with an "X". This
    /// is what makes the Terrain tool's edits visible while authoring 3D levels.
    private void drawTerrainTopDown(Graphics g, GameLevel level, int ox, int oy, double ts) {
        com.codename1.gaming.level.TerrainGrid t = level.getTerrain();
        int cols = level.getCols();
        int rows = level.getRows();
        int cell = (int) Math.round(ts);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = (int) Math.round(ox + c * ts);
                int y = (int) Math.round(oy + r * ts);
                if (!t.hasGround(c, r)) {
                    // open hole / sky gap: dark void with a diagonal hatch
                    g.setColor(0x06101f);
                    g.fillRect(x, y, cell, cell);
                    g.setColor(0x24344f);
                    g.setAlpha(150);
                    for (int d = 0; d < cell; d += 6) {
                        g.drawLine(x + d, y, x, y + d);
                    }
                    g.setAlpha(255);
                    continue;
                }
                float h = t.getHeight(c, r);
                // surface material colour, lightened with elevation so relief reads top-down
                double k = Math.max(0, Math.min(1, (h + 2) / 6.0));
                g.setColor(shade(materialColor(t.getMaterial(c, r)), 0.7 + k * 0.6));
                g.setAlpha(210);
                g.fillRect(x, y, cell, cell);
                g.setAlpha(255);
                float wall = t.getWall(c, r);
                if (wall > 0f) {
                    g.setColor(0x6b6f7a);
                    g.fillRect(x + 1, y + 1, cell - 2, cell - 2);
                    g.setColor(0x9aa0ad);
                    g.drawLine(x + 2, y + 2, x + cell - 3, y + cell - 3);
                    g.drawLine(x + cell - 3, y + 2, x + 2, y + cell - 3);
                }
            }
        }
    }

    private void drawGrid(Graphics g, GameLevel level, int ox, int oy, double ts) {
        int cols = level.getCols();
        int rows = level.getRows();
        for (int c = 0; c <= cols; c++) {
            g.setColor(c % 4 == 0 ? 0x2f5aa0 : 0x3f6fbf);
            g.setAlpha(c % 4 == 0 ? 190 : 120);
            int x = (int) Math.round(ox + c * ts);
            g.drawLine(x, oy, x, (int) Math.round(oy + rows * ts));
        }
        for (int r = 0; r <= rows; r++) {
            g.setColor(r % 4 == 0 ? 0x2f5aa0 : 0x3f6fbf);
            g.setAlpha(r % 4 == 0 ? 190 : 120);
            int y = (int) Math.round(oy + r * ts);
            g.drawLine(ox, y, (int) Math.round(ox + cols * ts), y);
        }
        g.setAlpha(255);
    }

    private void drawSelection(Graphics g, AssetCatalog cat, GameLevel level, int ox, int oy, double ts) {
        GameElement sel = model().getSelection();
        if (sel == null) {
            return;
        }
        double scale = ts / Math.max(1, level.getTileSize());
        AssetDef def = cat == null ? null : cat.def(sel.getAssetId());
        double esc = sel.getScaleX();
        int w = (int) Math.round(elementCells(level, def == null ? level.getTileSize() : def.getWidth()) * ts * esc);
        int h = (int) Math.round(elementCells(level, def == null ? level.getTileSize() : def.getHeight()) * ts * esc);
        int x = (int) Math.round(ox + sel.getX() * scale - w / 2.0);
        int y = (int) Math.round(oy + sel.getY() * scale - h / 2.0);
        g.setColor(ACCENT);
        g.drawRect(x - 2, y - 2, w + 4, h + 4);
        g.drawRect(x - 1, y - 1, w + 2, h + 2);
        handle(g, x - 2, y - 2);
        handle(g, x + w + 2, y - 2);
        handle(g, x - 2, y + h + 2);
        handle(g, x + w + 2, y + h + 2);
    }

    /// The live preview: the level rendered inside a device frame with a HUD, matching
    /// the GameForge "Live Preview" pane.
    private void paintPlay(Graphics g) {
        GameLevel level = model().level();
        AssetCatalog cat = model().catalog();
        int compX = getX(), compY = getY(), compW = getWidth(), compH = getHeight();
        g.setColor(0x0A1530);
        g.fillRect(compX, compY, compW, compH);

        double levelW = Math.max(1, level.getCols() * (double) level.getTileSize());
        double levelH = Math.max(1, level.getRows() * (double) level.getTileSize());
        double aspect = levelW / levelH;
        int margin = Math.max(20, Math.min(compW, compH) / 12);
        int bezel = 14;
        int availW = compW - margin * 2;
        int availH = compH - margin * 2 - 24; // leave room for the header label
        int screenW;
        int screenH;
        if ((double) availW / availH > aspect) {
            screenH = availH - bezel * 2;
            screenW = (int) (screenH * aspect);
        } else {
            screenW = availW - bezel * 2;
            screenH = (int) (screenW / aspect);
        }
        int devW = screenW + bezel * 2;
        int devH = screenH + bezel * 2;
        int devX = compX + (compW - devW) / 2;
        int devY = compY + (compH - devH) / 2 + 12;
        int sX = devX + bezel;
        int sY = devY + bezel;

        // header label above the device
        g.setColor(0x9DB0D6);
        String kind = level.getMode() == GameLevel.MODE_3D ? "3D PREVIEW"
                : level.getMode() == GameLevel.MODE_BOARD ? "BOARD PREVIEW" : "LIVE PREVIEW · iPhone 15";
        g.drawString(kind + " · " + model().getSceneName(), devX, devY - 20);

        // bezel
        g.setColor(0x0E1116);
        g.fillRoundRect(devX, devY, devW, devH, 42, 42);
        g.setColor(0x33394A);
        g.drawRoundRect(devX, devY, devW, devH, 42, 42);

        // scene clipped to the screen
        int ccx = g.getClipX(), ccy = g.getClipY(), ccw = g.getClipWidth(), cch = g.getClipHeight();
        g.setClip(sX, sY, screenW, screenH);
        if (level.getMode() == GameLevel.MODE_3D) {
            // true 3D: perspective projection of the board + element billboards
            paintScene3D(g, level, cat, sX, sY, screenW, screenH);
        } else if (player != null) {
            // a real game scrolls: zoom in and follow the player so the level scrolls
            // past, which is what makes a parallax background drift behind it (this
            // matches the shipped GameSceneView follow camera + per-layer parallax).
            int visRows = Math.min(level.getRows(), 10);
            double scale = screenH / (visRows * (double) level.getTileSize());
            double viewWorldW = screenW / scale;
            double viewWorldH = screenH / scale;
            double camX = clampCam(player.getX() - viewWorldW / 2, levelW - viewWorldW);
            double camY = clampCam(player.getY() - viewWorldH / 2, levelH - viewWorldH);
            paintBackdrop(g, backdrop(level), sX, sY, screenW, screenH);
            drawContentScrolling(g, cat, level, sX, sY, scale, camX, camY);
        } else {
            double scale = Math.min(screenW / levelW, screenH / levelH);
            double ts = level.getTileSize() * scale;
            int ox = (int) Math.round(sX + (screenW - levelW * scale) / 2);
            int oy = (int) Math.round(sY + (screenH - levelH * scale) / 2);
            paintBackdrop(g, backdrop(level), sX, sY, screenW, screenH);
            drawContent(g, cat, level, ox, oy, ts);
        }
        g.setClip(ccx, ccy, ccw, cch);

        // notch
        g.setColor(0x0E1116);
        int notchW = Math.max(40, screenW / 4);
        g.fillRoundRect(sX + (screenW - notchW) / 2, devY + 3, notchW, 12, 10, 10);

        // HUD overlay — real score / lives from the default behaviors
        g.setColor(0x000000);
        g.setAlpha(110);
        g.fillRect(sX, sY, screenW, 28);
        g.fillRect(sX, sY + screenH - 24, screenW, 24);
        g.setAlpha(255);
        g.setColor(0xFFE08A);
        g.drawString("SCORE " + score, sX + 10, sY + 7);
        g.setColor(0xFF7A7A);
        g.drawString("LIVES " + lives, sX + screenW - 90, sY + 7);
        // on-screen controls hint so it's clear how to play
        g.setColor(0xCFE0FF);
        String hint;
        if (level.getMode() == GameLevel.MODE_3D) {
            String t = view3dType();
            hint = "flight".equals(t) ? "Flight — ← → bank, ↑ ↓ climb / dive (no gravity)"
                    : "race".equals(t) ? "Race — ↑ accelerate, ↓ brake, ← → steer"
                    : "dungeon".equals(t) ? "Dungeon — ← → turn, ↑ ↓ walk (walls block you)"
                    : "Walk — ← → turn, ↑ ↓ move";
        } else {
            hint = player != null ? "← →  move    ↑ / Space  jump" : "Add a Player to control the scene";
        }
        g.drawString(hint, sX + 10, sY + screenH - 20);

        if (level.getMode() == GameLevel.MODE_3D) {
            drawRadar(g, level, sX, sY, screenW, screenH);
        }
    }

    /// A bottom-right mini-map of the 3D scene: the board outline, a dot per element
    /// (gold = pickup, red = enemy, slate = scenery), faint marks for terrain walls/holes,
    /// and a heading arrow for the player. Lets you find objects when flying an empty sky.
    private void drawRadar(Graphics g, GameLevel level, int sX, int sY, int screenW, int screenH) {
        int size = Math.max(58, Math.min(132, (int) (Math.min(screenW, screenH) * 0.3)));
        int pad = 8;
        int rx = sX + screenW - size - pad;
        int ry = sY + screenH - size - 28;
        int cols = Math.max(1, level.getCols());
        int rows = Math.max(1, level.getRows());
        double ts = Math.max(1, level.getTileSize());
        g.setColor(0x05101f);
        g.setAlpha(190);
        g.fillRoundRect(rx, ry, size, size, 10, 10);
        g.setAlpha(255);
        g.setColor(0x4D86FF);
        g.drawRoundRect(rx, ry, size, size, 10, 10);
        com.codename1.gaming.level.TerrainGrid t = level.getTerrain();
        if (t != null) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int px = rx + (int) (c * (double) size / cols);
                    int py = ry + (int) (r * (double) size / rows);
                    int w = Math.max(1, size / cols), h = Math.max(1, size / rows);
                    if (!t.hasGround(c, r)) {
                        g.setColor(0x0a1422);
                        g.setAlpha(220);
                        g.fillRect(px, py, w, h);
                    } else if (t.getWall(c, r) > 0f) {
                        g.setColor(0x8a8f9c);
                        g.setAlpha(230);
                        g.fillRect(px, py, w, h);
                    }
                }
            }
            g.setAlpha(255);
        }
        List<GameElement> els = level.elements();
        for (int i = 0; i < els.size(); i++) {
            GameElement el = els.get(i);
            if (el == player || collected.contains(el.getId())) {
                continue;
            }
            String id = el.getAssetId();
            int px = rx + (int) Math.max(0, Math.min(size - 1, el.getX() / ts / cols * size));
            int py = ry + (int) Math.max(0, Math.min(size - 1, el.getY() / ts / rows * size));
            g.setColor(isCollectible(id) ? 0xFFD24A : isEnemy(id) ? 0xFF5A5A : 0xBFD0E8);
            g.fillArc(px - 2, py - 2, 5, 5, 0, 360);
        }
        // player heading arrow (board centre + fp offset). Clamp into the radar when flying off.
        double pc = Math.max(0, Math.min(cols, fpX + cols / 2.0));
        double pr = Math.max(0, Math.min(rows, fpZ + rows / 2.0));
        int ppx = rx + (int) (pc / cols * size);
        int ppy = ry + (int) (pr / rows * size);
        double fx = Math.sin(fpYaw), fz = -Math.cos(fpYaw);
        g.setColor(0x4DF0A0);
        g.fillArc(ppx - 3, ppy - 3, 7, 7, 0, 360);
        g.drawLine(ppx, ppy, ppx + (int) (fx * 9), ppy + (int) (fz * 9));
    }

    // ---- 3D preview ----------------------------------------------------------

    /// Renders the 3D level as real (software-rasterised) 3D geometry: a sky, a shaded
    /// ground plane (except in flight = open sky) and each element as a depth-sorted,
    /// face-shaded box, viewed from a first-person camera whose feel matches the game type.
    /// (The on-device runtime renders the same scene on the GPU via `GameSceneView`.)
    private void paintScene3D(Graphics g, GameLevel level, AssetCatalog cat, int sX, int sY, int sw, int sh) {
        String type = view3dType();
        boolean ground = !"flight".equals(type);
        int[] sky = "dungeon".equals(type) ? new int[]{0xff090a10, 0xff15161e}
                : "flight".equals(type) ? new int[]{0xff2f63ad, 0xff79afe6, 0xffd2e8ff}
                : new int[]{0xff1b2742, 0xff39517a, 0xff86a0c8};
        paintBackdrop(g, sky, sX, sY, sw, sh);

        int cols = Math.max(1, level.getCols());
        int rows = Math.max(1, level.getRows());
        double ts = Math.max(1, level.getTileSize());
        double halfC = cols / 2.0;
        double halfR = rows / 2.0;

        // first-person camera; pitch/altitude vary by type (set in stepPlay3D). On a non-flight
        // level the eye rides on top of whatever ground the walker stands on (terrain-aware).
        double eyeH = "flight".equals(type) ? fpAlt
                : ("dungeon".equals(type) ? 1.4 : ("race".equals(type) ? 1.2 : 1.5));
        if (!"flight".equals(type)) {
            eyeH += groundAt(level, fpX + halfC, fpZ + halfR);
        }
        p3eye[0] = fpX;
        p3eye[1] = eyeH;
        p3eye[2] = fpZ;
        double fov = "dungeon".equals(type) ? 62 : (level.getFov() < 1 ? 72 : level.getFov());
        p3f = (sh / 2.0) / Math.tan(Math.toRadians(fov) / 2.0);
        p3cx = sX + sw / 2;
        p3cy = sY + sh / 2;
        double pitch = "flight".equals(type) ? fpPitch : ("race".equals(type) ? -0.12 : -0.16);
        double[] fwd = norm(new double[]{Math.cos(pitch) * Math.sin(fpYaw), Math.sin(pitch),
                -Math.cos(pitch) * Math.cos(fpYaw)});
        copy(fwd, p3fwd);
        copy(norm(cross(p3fwd, new double[]{0, 1, 0})), p3right);
        copy(cross(p3right, p3fwd), p3up);

        com.codename1.gaming.level.TerrainGrid terrain = level.getTerrain();
        java.util.ArrayList<Face> faces = new java.util.ArrayList<Face>();
        if (level.isLargeWorld() && level.getWorld().getActiveRegion() != null) {
            // large world: render the ACTIVE region's streaming terrain over the visible window
            com.codename1.gaming.level.Terrain rt = level.getWorld().getActiveRegion().terrain();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (!rt.hasGround(c, r)) {
                        continue;
                    }
                    double h = rt.getHeight(c, r);
                    double x0 = c - halfC, x1 = c + 1 - halfC, z0 = r - halfR, z1 = r + 1 - halfR;
                    int base = com.codename1.gaming.level.MaterialRegistry.get(rt.getMaterial(c, r)).getColor();
                    double k = Math.max(0, Math.min(1, (h + 2) / 6.0));
                    addFace(faces, x0, h, z0, x1, h, z0, x1, h, z1, x0, h, z1, shade(base, 0.8 + k * 0.4));
                }
            }
            List<com.codename1.gaming.level.TerrainFeature> fs = rt.features();
            for (int i = 0; i < fs.size(); i++) {
                com.codename1.gaming.level.TerrainFeature f = fs.get(i);
                int col = com.codename1.gaming.level.MaterialRegistry.get(f.getMaterial()).getColor();
                addBox(faces, f.getX() - halfC, f.getY(), f.getZ() - halfR,
                        Math.max(0.1, f.getWidth() / 2), Math.max(0.2, f.getHeight()), col);
            }
        } else if (terrain != null) {
            // heightfield rendered with INTERPOLATED VERTEX heights so painted ramps read as
            // smooth slopes (not stairs). Each cell quad's 4 corners take the shared vertex
            // height (avg of the cells meeting at that corner). Surface colour = material.
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (!terrain.hasGround(c, r)) {
                        continue;
                    }
                    double y00 = vertexHeight(terrain, c, r);
                    double y10 = vertexHeight(terrain, c + 1, r);
                    double y11 = vertexHeight(terrain, c + 1, r + 1);
                    double y01 = vertexHeight(terrain, c, r + 1);
                    double x0 = c - halfC, x1 = c + 1 - halfC;
                    double z0 = r - halfR, z1 = r + 1 - halfR;
                    double k = Math.max(0, Math.min(1, (terrain.getHeight(c, r) + 2) / 6.0));
                    int gc = shade(materialColor(terrain.getMaterial(c, r)), 0.8 + k * 0.4);
                    addFace(faces, x0, y00, z0, x1, y10, z0, x1, y11, z1, x0, y01, z1, gc);
                    float wall = terrain.getWall(c, r);
                    if (wall > 0f) {
                        addBox(faces, c + 0.5 - halfC, terrain.getHeight(c, r),
                                r + 0.5 - halfR, 0.5, wall, 0x6b6f7a);
                    }
                }
            }
        } else if (ground) {
            int gc = "race".equals(type) ? 0x3a3f4a : 0x2c5d3a;   // road grey vs grass
            addFace(faces, -halfC, 0, -halfR, cols - halfC, 0, -halfR,
                    cols - halfC, 0, rows - halfR, -halfC, 0, rows - halfR, gc);
        }
        List<GameElement> els = level.elements();
        for (int i = 0; i < els.size(); i++) {
            GameElement el = els.get(i);
            if (collected.contains(el.getId())) {
                continue;
            }
            // first-person: don't render the player's own body — it sits at the camera and
            // would project to a screen-filling quad that hides the whole scene.
            if (el == player) {
                continue;
            }
            AssetDef def = cat == null ? null : cat.def(el.getAssetId());
            double wx = el.getX() / ts - halfC;
            double wz = el.getY() / ts - halfR;
            // elevation = explicit Z plus the terrain the object stands on
            double baseY = el.getZ() / ts + groundAt(level, el.getX() / ts, el.getY() / ts);
            double sc = el.getScaleX();
            double hwd = 0.42 * sc;
            double height = ("dungeon".equals(type) ? 2.2 : (def == null ? 0.9 : Math.max(0.5,
                    Math.min(2.0, def.getHeight() / ts)))) * sc;
            int col = def == null ? 0x9aa3b0 : (def.getColor() & 0xffffff);
            addBox(faces, wx, baseY, wz, hwd, height, col);
        }
        // painter's sort, far → near (insertion sort; small face count)
        for (int i = 1; i < faces.size(); i++) {
            Face cur = faces.get(i);
            int j = i - 1;
            while (j >= 0 && faces.get(j).depth < cur.depth) {
                faces.set(j + 1, faces.get(j));
                j--;
            }
            faces.set(j + 1, cur);
        }
        g.setAntiAliased(true);
        for (int i = 0; i < faces.size(); i++) {
            Face f = faces.get(i);
            g.setColor(f.color);
            g.fillPolygon(f.xs, f.ys, f.xs.length);
            g.setColor(shade(f.color, 0.45));   // edge so blocks read crisply
            int n = f.xs.length;
            for (int e = 0; e < n; e++) {
                int e2 = (e + 1) % n;
                g.drawLine(f.xs[e], f.ys[e], f.xs[e2], f.ys[e2]);
            }
        }
        // faint ground grid for spatial reference (skip in flight)
        if (ground) {
            g.setColor("race".equals(type) ? 0x556070 : 0x6fae7f);
            g.setAlpha(120);
            for (int c = 0; c <= cols; c += 1) {
                line3D(g, c - halfC, 0.01, -halfR, c - halfC, 0.01, rows - halfR);
            }
            for (int r = 0; r <= rows; r += 1) {
                line3D(g, -halfC, 0.01, r - halfR, cols - halfC, 0.01, r - halfR);
            }
            g.setAlpha(255);
        }
    }

    /// A pre-projected, depth-keyed polygon for the painter's algorithm.
    private static final class Face {
        double depth;
        int[] xs;
        int[] ys;
        int color;
    }

    /// Projects a quad's 4 world corners and, if all are in front of the camera, queues it
    /// as a `Face` (depth = average camera distance).
    private void addFace(java.util.ArrayList<Face> faces,
            double x0, double y0, double z0, double x1, double y1, double z1,
            double x2, double y2, double z2, double x3, double y3, double z3, int color) {
        double[] a = project3D(x0, y0, z0);
        double[] b = project3D(x1, y1, z1);
        double[] c = project3D(x2, y2, z2);
        double[] d = project3D(x3, y3, z3);
        if (a == null || b == null || c == null || d == null) {
            return;
        }
        Face f = new Face();
        f.xs = new int[]{(int) a[0], (int) b[0], (int) c[0], (int) d[0]};
        f.ys = new int[]{(int) a[1], (int) b[1], (int) c[1], (int) d[1]};
        f.depth = (a[2] + b[2] + c[2] + d[2]) / 4.0;
        f.color = color;
        faces.add(f);
    }

    /// Shared height at grid VERTEX (vc,vr) = mean of the up-to-4 cells meeting there (holes
    /// ignored). Interpolating these gives smooth slopes between cells of different elevation.
    private double vertexHeight(com.codename1.gaming.level.TerrainGrid t, int vc, int vr) {
        double sum = 0;
        int n = 0;
        for (int dr = -1; dr <= 0; dr++) {
            for (int dc = -1; dc <= 0; dc++) {
                int c = vc + dc, r = vr + dr;
                if (c >= 0 && r >= 0 && c < t.getCols() && r < t.getRows() && t.hasGround(c, r)) {
                    sum += t.getHeight(c, r);
                    n++;
                }
            }
        }
        return n == 0 ? 0 : sum / n;
    }

    /// Ground elevation (world/tile units) at a fractional cell coordinate — BILINEARLY
    /// interpolated across the vertex heights so a walker glides up a ramp. 0 when there is no
    /// terrain or the cell is a hole — so flat levels and sky gaps behave as before.
    private double groundAt(GameLevel level, double colCells, double rowCells) {
        com.codename1.gaming.level.TerrainGrid t = level.getTerrain();
        if (t == null) {
            return 0;
        }
        int c = (int) Math.floor(colCells);
        int r = (int) Math.floor(rowCells);
        if (c < 0 || r < 0 || c >= t.getCols() || r >= t.getRows() || !t.hasGround(c, r)) {
            return 0;
        }
        double fx = colCells - c, fz = rowCells - r;
        double top = vertexHeight(t, c, r) * (1 - fx) + vertexHeight(t, c + 1, r) * fx;
        double bot = vertexHeight(t, c, r + 1) * (1 - fx) + vertexHeight(t, c + 1, r + 1) * fx;
        return top * (1 - fz) + bot * fz;
    }

    /// Base colour for a painted surface material (see TerrainGrid.MAT_*).
    private static int materialColor(int mat) {
        switch (mat) {
            case com.codename1.gaming.level.TerrainGrid.MAT_ROAD: return 0x3b3e46;
            case com.codename1.gaming.level.TerrainGrid.MAT_STONE: return 0x6f6a62;
            case com.codename1.gaming.level.TerrainGrid.MAT_SAND: return 0xc2a86a;
            case com.codename1.gaming.level.TerrainGrid.MAT_WATER: return 0x2f6fa8;
            case com.codename1.gaming.level.TerrainGrid.MAT_DIRT: return 0x7a5a38;
            default: return 0x3f7d3a;   // grass
        }
    }

    /// Whether the given fractional cell coordinate is blocked for a walker: a terrain wall or
    /// (for grounded game types) an open hole you'd fall through.
    private boolean terrainBlocks(GameLevel level, double colCells, double rowCells, boolean grounded) {
        com.codename1.gaming.level.TerrainGrid t = level.getTerrain();
        if (t == null) {
            return false;
        }
        int c = (int) Math.floor(colCells);
        int r = (int) Math.floor(rowCells);
        if (c < 0 || r < 0 || c >= level.getCols() || r >= level.getRows()) {
            return false;
        }
        if (t.getWall(c, r) > 0f) {
            return true;
        }
        return grounded && !t.hasGround(c, r);
    }

    /// Adds the 5 visible faces (top + 4 sides) of a box centred at (cx, *, cz), resting at
    /// baseY, with horizontal half-extent `hwd` and the given `height`. Faces are shaded so
    /// it reads as a solid 3D block.
    private void addBox(java.util.ArrayList<Face> faces, double cx, double baseY, double cz,
            double hwd, double height, int color) {
        double x0 = cx - hwd, x1 = cx + hwd, z0 = cz - hwd, z1 = cz + hwd;
        double y0 = baseY, y1 = baseY + height;
        addFace(faces, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, shade(color, 1.0));   // top
        addFace(faces, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, shade(color, 0.78));  // +z
        addFace(faces, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, shade(color, 0.62));  // -z
        addFace(faces, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, shade(color, 0.70));  // +x
        addFace(faces, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, shade(color, 0.55));  // -x
    }

    private static int shade(int rgb, double f) {
        int r = (int) Math.min(255, ((rgb >> 16) & 0xff) * f);
        int g = (int) Math.min(255, ((rgb >> 8) & 0xff) * f);
        int b = (int) Math.min(255, (rgb & 0xff) * f);
        return (r << 16) | (g << 8) | b;
    }

    // ---- 3D editing (perspective overview + ground-plane picking) -------------

    /// Renders the 3D level in perspective for EDITING: an angled overview of the whole
    /// board with every placed element as a billboard, the selection highlighted, and a
    /// grid you click to place on. Pan orbits, zoom moves the camera in/out.
    private void paintEdit3D(Graphics g, GameLevel level, AssetCatalog cat) {
        int sX = getX(), sY = getY(), sw = getWidth(), sh = getHeight();
        paintBackdrop(g, new int[]{0xff142036, 0xff2b3f63, 0xff5b769f}, sX, sY, sw, sh);
        setupOverviewCamera3D(level, sX, sY, sw, sh);
        drawGround3D(g, level);
        drawBillboards3D(g, level, cat, model().getSelection());
        g.setColor(0xCFE0FF);
        g.drawString("3D map — click to place · Pan tool orbits · zoom moves in/out", sX + 10, sY + sh - 22);
    }

    /// Overview camera for 3D editing: looks at the board centre from above and behind;
    /// zoom controls distance, the Pan tool's panX orbits the view.
    private void setupOverviewCamera3D(GameLevel level, int sX, int sY, int sw, int sh) {
        double span = Math.max(level.getCols(), level.getRows());
        double dist = Math.max(4, span * 1.25 / Math.max(0.4, zoom));
        double height = dist * 0.8;
        double angle = 0.5 + panX * 0.004;
        p3eye[0] = Math.sin(angle) * dist;
        p3eye[1] = height;
        p3eye[2] = Math.cos(angle) * dist;
        double fov = level.getFov() < 1 ? 60 : level.getFov();
        p3f = (sh / 2.0) / Math.tan(Math.toRadians(fov) / 2.0);
        p3cx = sX + sw / 2;
        p3cy = sY + sh / 2;
        double[] fwd = norm(new double[]{-p3eye[0], -p3eye[1], -p3eye[2]});
        copy(fwd, p3fwd);
        copy(norm(cross(p3fwd, new double[]{0, 1, 0})), p3right);
        copy(cross(p3right, p3fwd), p3up);
    }

    private void drawGround3D(Graphics g, GameLevel level) {
        int cols = Math.max(1, level.getCols());
        int rows = Math.max(1, level.getRows());
        double hc = cols / 2.0;
        double hr = rows / 2.0;
        double[] c0 = project3D(-hc, 0, -hr);
        double[] c1 = project3D(cols - hc, 0, -hr);
        double[] c2 = project3D(cols - hc, 0, rows - hr);
        double[] c3 = project3D(-hc, 0, rows - hr);
        if (c0 != null && c1 != null && c2 != null && c3 != null) {
            g.setColor(0x24507a);
            g.fillPolygon(new int[]{(int) c0[0], (int) c1[0], (int) c2[0], (int) c3[0]},
                    new int[]{(int) c0[1], (int) c1[1], (int) c2[1], (int) c3[1]}, 4);
        }
        if (gridVisible) {
            g.setColor(0x6f93c8);
            for (int c = 0; c <= cols; c++) {
                line3D(g, c - hc, 0, -hr, c - hc, 0, rows - hr);
            }
            for (int r = 0; r <= rows; r++) {
                line3D(g, -hc, 0, r - hr, cols - hc, 0, r - hr);
            }
        }
    }

    private void drawBillboards3D(Graphics g, GameLevel level, AssetCatalog cat, GameElement selected) {
        int cols = Math.max(1, level.getCols());
        int rows = Math.max(1, level.getRows());
        double ts = Math.max(1, level.getTileSize());
        double hc = cols / 2.0;
        double hr = rows / 2.0;
        List<GameElement> els = level.elements();
        java.util.ArrayList<double[]> order = new java.util.ArrayList<double[]>();
        for (int i = 0; i < els.size(); i++) {
            GameElement el = els.get(i);
            Layer ly = level.getLayer(el.getLayer());
            if (ly != null && !ly.isVisible()) {
                continue;
            }
            double[] base = project3D(el.getX() / ts - hc, 0, el.getY() / ts - hr);
            if (base != null) {
                order.add(new double[]{base[2], i});
            }
        }
        for (int i = 1; i < order.size(); i++) {
            double[] cur = order.get(i);
            int j = i - 1;
            while (j >= 0 && order.get(j)[0] < cur[0]) {
                order.set(j + 1, order.get(j));
                j--;
            }
            order.set(j + 1, cur);
        }
        for (int k = 0; k < order.size(); k++) {
            GameElement el = els.get((int) order.get(k)[1]);
            AssetDef def = cat == null ? null : cat.def(el.getAssetId());
            double wx = el.getX() / ts - hc;
            double wz = el.getY() / ts - hr;
            double hWorld = (def == null ? 1.0 : Math.max(0.6, Math.min(2.0, def.getHeight() / ts))) * el.getScaleX();
            double[] base = project3D(wx, 0, wz);
            double[] top = project3D(wx, hWorld, wz);
            if (base == null || top == null) {
                continue;
            }
            int ph = Math.max(8, (int) Math.round(base[1] - top[1]));
            double aspect = def == null || def.getHeight() == 0 ? 1.0 : def.getWidth() / (double) def.getHeight();
            int pw = Math.max(8, (int) Math.round(ph * aspect));
            int bx = (int) Math.round(base[0] - pw / 2.0);
            int by = (int) Math.round(top[1]);
            g.setColor(0x101820);
            g.setAlpha(90);
            g.fillArc((int) base[0] - pw / 2, (int) base[1] - ph / 12, pw, Math.max(4, ph / 6), 0, 360);
            g.setAlpha(255);
            if (cat != null && cat.hasImage(el.getAssetId())) {
                g.drawImage(cat.image(el.getAssetId()), bx, by, pw, ph);
            } else {
                g.setColor(def == null ? 0x888888 : (def.getColor() & 0xffffff));
                g.fillRect(bx, by, pw, ph);
            }
            if (el == selected) {
                g.setColor(ACCENT);
                g.drawRect(bx - 2, by - 2, pw + 4, ph + 4);
                g.drawRect(bx - 1, by - 1, pw + 2, ph + 2);
            }
        }
    }

    /// Inverse-projects a screen point onto the ground plane (y=0) and returns the
    /// corresponding element-space {x,y} (tile-size units), or null if it misses.
    private double[] groundPick(int absX, int absY) {
        GameLevel level = model().level();
        setupOverviewCamera3D(level, getX(), getY(), getWidth(), getHeight());
        double a = (absX - p3cx) / p3f;
        double b = -(absY - p3cy) / p3f;
        double dx = a * p3right[0] + b * p3up[0] + p3fwd[0];
        double dy = a * p3right[1] + b * p3up[1] + p3fwd[1];
        double dz = a * p3right[2] + b * p3up[2] + p3fwd[2];
        if (Math.abs(dy) < 1e-6) {
            return null;
        }
        double t = -p3eye[1] / dy;
        if (t <= 0) {
            return null;
        }
        double wx = p3eye[0] + t * dx;
        double wz = p3eye[2] + t * dz;
        double ts = Math.max(1, level.getTileSize());
        return new double[]{(wx + level.getCols() / 2.0) * ts, (wz + level.getRows() / 2.0) * ts};
    }

    /// Routes a 3D-edit press: places / selects / erases on the ground plane under the cursor.
    private void handle3DPress(int absX, int absY) {
        double[] gp = groundPick(absX, absY);
        if (gp == null) {
            return;
        }
        GameLevel level = model().level();
        AssetCatalog cat = model().catalog();
        double ts = Math.max(1, level.getTileSize());
        int col = (int) Math.floor(gp[0] / ts);
        int row = (int) Math.floor(gp[1] / ts);
        switch (model().getTool()) {
            case BRUSH -> {
                AssetDef def = cat == null ? null : cat.def(model().getSelectedAssetId());
                if (def != null && def.isTile()) {
                    controller.paintTile(col, row);
                } else if (model().isSnap()) {
                    controller.placeElement(col * ts + ts / 2.0, row * ts + ts / 2.0);
                } else {
                    controller.placeElement(gp[0], gp[1]);
                }
            }
            case ERASE -> {
                GameElement hit = pick3D(gp);
                if (hit != null) {
                    model().setSelection(hit);
                    controller.deleteSelection();
                }
            }
            case SELECT, MOVE -> {
                GameElement hit = pick3D(gp);
                model().setSelection(hit);
                if (hit != null) {
                    grabDX = gp[0] - hit.getX();
                    grabDY = gp[1] - hit.getY();
                }
            }
            default -> {
            }
        }
    }

    /// Routes a 3D-edit drag: Pan orbits the camera; Select/Move drags the picked element.
    private void handle3DDrag(int absX, int absY, int dx) {
        if (model().getTool() == com.codename1.gamebuilder.editor.Tool.PAN) {
            panX += dx;   // orbit
            return;
        }
        if (model().getSelection() == null) {
            return;
        }
        com.codename1.gamebuilder.editor.Tool t = model().getTool();
        if (t == com.codename1.gamebuilder.editor.Tool.MOVE || t == com.codename1.gamebuilder.editor.Tool.SELECT) {
            double[] gp = groundPick(absX, absY);
            if (gp != null) {
                controller.moveSelectionTo(gp[0] - grabDX, gp[1] - grabDY);
            }
        }
    }

    /// Nearest element to a ground point, within ~1.2 cells (for 3D picking).
    private GameElement pick3D(double[] gp) {
        GameLevel level = model().level();
        double ts = Math.max(1, level.getTileSize());
        double bestD = (1.2 * ts) * (1.2 * ts);
        GameElement found = null;
        List<GameElement> els = level.elements();
        for (int i = 0; i < els.size(); i++) {
            GameElement el = els.get(i);
            Layer ly = level.getLayer(el.getLayer());
            if (ly != null && (!ly.isVisible() || ly.isLocked())) {
                continue;
            }
            double ddx = el.getX() - gp[0];
            double ddy = el.getY() - gp[1];
            double d = ddx * ddx + ddy * ddy;
            if (d < bestD) {
                bestD = d;
                found = el;
            }
        }
        return found;
    }

    /// Projects a world point to screen using the per-frame camera; returns
    /// {screenX, screenY, cameraDepth} or null if behind the camera.
    private double[] project3D(double x, double y, double z) {
        double rx = x - p3eye[0], ry = y - p3eye[1], rz = z - p3eye[2];
        double cz = rx * p3fwd[0] + ry * p3fwd[1] + rz * p3fwd[2];
        if (cz <= 0.05) {
            return null;
        }
        double cx = rx * p3right[0] + ry * p3right[1] + rz * p3right[2];
        double cy = rx * p3up[0] + ry * p3up[1] + rz * p3up[2];
        return new double[]{p3cx + (cx / cz) * p3f, p3cy - (cy / cz) * p3f, cz};
    }

    private void line3D(Graphics g, double x0, double y0, double z0, double x1, double y1, double z1) {
        double[] a = project3D(x0, y0, z0);
        double[] b = project3D(x1, y1, z1);
        if (a != null && b != null) {
            g.drawLine((int) a[0], (int) a[1], (int) b[0], (int) b[1]);
        }
    }

    private static double[] norm(double[] v) {
        double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (len < 1e-9) {
            return new double[]{0, 0, 1};
        }
        return new double[]{v[0] / len, v[1] / len, v[2] / len};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]};
    }

    private static void copy(double[] src, double[] dst) {
        dst[0] = src[0];
        dst[1] = src[1];
        dst[2] = src[2];
    }

    private void paintBackdrop(Graphics g, int[] stops, int x, int y, int w, int h) {
        int segs = stops.length - 1;
        if (segs < 1) {
            g.setColor(stops[0]);
            g.fillRect(x, y, w, h);
            return;
        }
        for (int row = 0; row < h; row++) {
            double t = (double) row / h * segs;
            int idx = (int) t;
            if (idx >= segs) {
                idx = segs - 1;
            }
            double f = t - idx;
            g.setColor(lerp(stops[idx], stops[idx + 1], f));
            g.drawLine(x, y + row, x + w, y + row);
        }
    }

    private double playBob(GameElement el, AssetDef def) {
        String id = el.getAssetId();
        if (id == null) {
            return 0;
        }
        if (id.equals("coin") || id.equals("gem")) {
            return Math.sin(playClock * 4 + el.getX()) * 3;
        }
        if (id.equals("slime") || id.startsWith("npc") || id.equals("token")) {
            return Math.sin(playClock * 6 + el.getY()) * 4;
        }
        return 0;
    }

    private void paintBackdrop(Graphics g, int[] stops) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int segs = stops.length - 1;
        if (segs < 1) {
            g.setColor(stops[0]);
            g.fillRect(x, y, w, h);
            return;
        }
        for (int row = 0; row < h; row++) {
            double t = (double) row / h * segs;
            int idx = (int) t;
            if (idx >= segs) {
                idx = segs - 1;
            }
            double f = t - idx;
            g.setColor(lerp(stops[idx], stops[idx + 1], f));
            g.drawLine(x, y + row, x + w, y + row);
        }
    }

    /// The background gradient: the scene's chosen "background" theme when set (Sky /
    /// Forest / Night / Cave), otherwise a sensible default for the asset pack.
    private int[] backdrop(GameLevel level) {
        String bg = level.getString("background", null);
        if (bg != null) {
            if ("Forest".equals(bg)) {
                return new int[]{0x7fc06a, 0x4f8f4a, 0x2c5a30};
            }
            if ("Night".equals(bg)) {
                return new int[]{0x0b1030, 0x1b2452, 0x2c3870};
            }
            if ("Cave".equals(bg)) {
                return new int[]{0x33303a, 0x201d28, 0x0e0c14};
            }
            if ("Sky".equals(bg)) {
                return new int[]{0x8fd8ff, 0xbfe9ff, 0xa7dcff};
            }
        }
        String pack = level.getAssetPack();
        if ("topdown".equals(pack)) {
            return new int[]{0x2b2740, 0x1c1a2c, 0x141322};
        }
        if ("board".equals(pack)) {
            return new int[]{0x2f8a57, 0x1f6b41, 0x15532f};
        }
        if ("kit3d".equals(pack)) {
            return new int[]{0x1a2540, 0x0e1730};
        }
        return new int[]{0x8fd8ff, 0xbfe9ff, 0xa7dcff}; // platformer sky
    }

    private static int lerp(int a, int b, double f) {
        int ar = (a >> 16) & 0xff, ag = (a >> 8) & 0xff, ab = a & 0xff;
        int br = (b >> 16) & 0xff, bg = (b >> 8) & 0xff, bb = b & 0xff;
        int r = (int) (ar + (br - ar) * f);
        int gg = (int) (ag + (bg - ag) * f);
        int bl = (int) (ab + (bb - ab) * f);
        return (r << 16) | (gg << 8) | bl;
    }

    private void handle(Graphics g, int cx, int cy) {
        g.setColor(0xffffff);
        g.fillRect(cx - 3, cy - 3, 6, 6);
        g.setColor(ACCENT);
        g.drawRect(cx - 3, cy - 3, 6, 6);
    }

    private void drawAsset(Graphics g, AssetCatalog cat, String assetId, int x, int y, int w, int h) {
        Image img = cat == null ? null : cat.image(assetId);
        if (img != null) {
            AssetDef def = cat.def(assetId);
            // animate a sprite sheet while playing (the editor preview of the runtime
            // AnimatedSprite); edit mode shows the first frame
            if (playMode && def != null && def.isSheet()) {
                SpriteSheet sh = cat.sheet(assetId);
                if (sh != null) {
                    int n = def.getFrameCount() > 0 ? def.getFrameCount() : sh.getFrameCount();
                    img = sh.getFrame(((int) (playClock * def.getFps())) % Math.max(1, n));
                }
            }
            g.drawImage(img, x, y, w, h);
            return;
        }
        AssetDef def = cat == null ? null : cat.def(assetId);
        g.setColor(def == null ? 0x888888 : (def.getColor() & 0xffffff));
        g.fillRect(x, y, w, h);
    }

    private static int[] parseCell(String key) {
        int comma = key.indexOf(',');
        if (comma < 0) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(key.substring(0, comma)), Integer.parseInt(key.substring(comma + 1))};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- input ---------------------------------------------------------------

    private int worldX(int absX) {
        double scale = cellPx() / Math.max(1, model().level().getTileSize());
        return (int) Math.round((absX - getAbsoluteX() - panX) / scale);
    }

    private int worldY(int absY) {
        double scale = cellPx() / Math.max(1, model().level().getTileSize());
        return (int) Math.round((absY - getAbsoluteY() - panY) / scale);
    }

    private int cellCol(int absX) {
        return floorDiv(absX - getAbsoluteX() - panX, (int) Math.round(cellPx()));
    }

    private int cellRow(int absY) {
        return floorDiv(absY - getAbsoluteY() - panY, (int) Math.round(cellPx()));
    }

    private static int floorDiv(int a, int b) {
        if (b == 0) {
            return 0;
        }
        int q = a / b;
        if ((a % b != 0) && (a < 0)) {
            q--;
        }
        return q;
    }

    /// World→screen scale (px per world unit) at the current canvas size and zoom.
    private double worldScale() {
        return cellPx() / Math.max(1, model().level().getTileSize());
    }

    /// The element-centre world position for snapping into cell (col,row). In a 2D
    /// side-scroller it is centred horizontally and bottom-aligned so a character stands
    /// on the cell below (feet on the grid line); for 3D maps / boards it is the cell
    /// centre (bottom-aligning a 32-unit mesh on a 1-unit board would throw it off-grid).
    private double[] snapCenter(int col, int row, AssetDef def) {
        double ts = Math.max(1, model().level().getTileSize());
        if (model().level().getMode() != GameLevel.MODE_2D) {
            return new double[]{col * ts + ts / 2.0, row * ts + ts / 2.0};
        }
        double h = def == null ? ts : def.getHeight();
        return new double[]{col * ts + ts / 2.0, (row + 1) * ts - h / 2.0};
    }

    /// Snaps the selected element to rest inside whatever cell its centre currently lies in.
    private void snapSelectionToCell() {
        GameElement sel = model().getSelection();
        if (sel == null) {
            return;
        }
        double ts = Math.max(1, model().level().getTileSize());
        AssetCatalog cat = model().catalog();
        AssetDef def = cat == null ? null : cat.def(sel.getAssetId());
        int col = (int) Math.floor(sel.getX() / ts);
        int row;
        if (model().level().getMode() == GameLevel.MODE_2D) {
            // the cell whose bottom the feet rest on
            row = (int) Math.floor((sel.getY() + (def == null ? ts : def.getHeight()) / 2.0) / ts) - 1;
        } else {
            row = (int) Math.floor(sel.getY() / ts);
        }
        if (row < 0) {
            row = 0;
        }
        double[] c = snapCenter(col, row, def);
        controller.moveSelectionTo(c[0], c[1]);
    }

    /// Places the currently-selected asset at an absolute screen point (used by drag-from-
    /// the-asset-library). Paints a tile or stamps an actor, honoring snap.
    public void placeAssetAt(int absX, int absY) {
        if (playMode) {
            return;
        }
        int col = cellCol(absX);
        int row = cellRow(absY);
        AssetCatalog cat = model().catalog();
        AssetDef def = cat == null ? null : cat.def(model().getSelectedAssetId());
        if (def != null && def.isTile()) {
            controller.paintTile(col, row);
        } else if (model().isSnap()) {
            double[] c = snapCenter(col, row, def);
            controller.placeElement(c[0], c[1]);
        } else {
            controller.placeElement(worldX(absX), worldY(absY));
        }
        onChange.run();
    }

    @Override
    public void pointerPressed(int absX, int absY) {
        if (playMode) {
            return;
        }
        dragging = true;
        didDrag = false;
        lastDragX = absX;
        lastDragY = absY;
        if (perspective3D()) {
            handle3DPress(absX, absY);
            onChange.run();
            return;
        }
        int col = cellCol(absX);
        int row = cellRow(absY);
        switch (model().getTool()) {
            case BRUSH -> {
                if (!controller.paintTile(col, row)) {
                    if (model().isSnap()) {
                        AssetDef def = model().catalog() == null ? null
                                : model().catalog().def(model().getSelectedAssetId());
                        double[] c = snapCenter(col, row, def);
                        controller.placeElement(c[0], c[1]);
                    } else {
                        controller.placeElement(worldX(absX), worldY(absY));
                    }
                }
            }
            case FILL -> controller.floodFill(col, row);
            case TERRAIN -> controller.paintTerrain(col, row, model().getTerrainBrush());
            case ERASE -> {
                if (!controller.eraseTile(col, row) && controller.selectAt(worldX(absX), worldY(absY)) != null) {
                    controller.deleteSelection();
                }
            }
            case SELECT, MOVE -> {
                GameElement hit = controller.selectAt(worldX(absX), worldY(absY));
                if (hit != null) {
                    grabDX = worldX(absX) - hit.getX();
                    grabDY = worldY(absY) - hit.getY();
                } else {
                    grabDX = grabDY = 0;
                }
            }
            case PAN -> {
            }
        }
        onChange.run();
    }

    @Override
    public void pointerDragged(int absX, int absY) {
        if (playMode || !dragging) {
            return;
        }
        int dx = absX - lastDragX;
        int dy = absY - lastDragY;
        lastDragX = absX;
        lastDragY = absY;
        didDrag = true;
        if (perspective3D()) {
            handle3DDrag(absX, absY, dx);
            onChange.run();
            return;
        }
        switch (model().getTool()) {
            case BRUSH -> controller.paintTile(cellCol(absX), cellRow(absY));
            case TERRAIN -> controller.paintTerrain(cellCol(absX), cellRow(absY), model().getTerrainBrush());
            case ERASE -> controller.eraseTile(cellCol(absX), cellRow(absY));
            // absolute follow so the element stays exactly under the cursor (the grab
            // offset is the world distance from the cursor to the element centre). Both
            // Select and Move drag the grabbed object, so dragging a coin just works.
            case SELECT, MOVE -> {
                if (model().getSelection() != null) {
                    controller.moveSelectionTo(worldX(absX) - grabDX, worldY(absY) - grabDY);
                }
            }
            case PAN -> {
                panX += dx;
                panY += dy;
            }
            default -> {
            }
        }
        onChange.run();
    }

    @Override
    public void pointerReleased(int absX, int absY) {
        com.codename1.gamebuilder.editor.Tool t = model().getTool();
        boolean movable = t == com.codename1.gamebuilder.editor.Tool.MOVE
                || t == com.codename1.gamebuilder.editor.Tool.SELECT;
        // only snap if the user actually DRAGGED — a plain click must not move the element
        if (dragging && didDrag && movable && model().isSnap() && model().getSelection() != null) {
            snapSelectionToCell();
            onChange.run();
        }
        dragging = false;
        didDrag = false;
    }
}
