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
package com.codename1.gaming.level;

import com.codename1.gaming.AnimatedSprite;
import com.codename1.gaming.Scene;
import com.codename1.gaming.Sprite;
import com.codename1.gaming.SpriteSheet;
import com.codename1.io.JSONParser;
import com.codename1.ui.Image;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A saved, mode-aware game level or map: the data the visual editor writes and the
/// runtime loads, deliberately decoupled from how it is drawn.
///
/// A level has a `#getMode()` -- `Mode#TWO_D` (tiles + sprites), `Mode#THREE_D` (meshes in a
/// perspective world with terrain and lights) or `Mode#BOARD` (an isometric grid) --
/// an `#getAssetPack()` it draws from, a grid size, ordered `Layer`s, freely placed
/// `GameElement`s, and mode-specific extras (a camera rig, `#lights()`,
/// `#getTerrain()`). It is pure data: load it with `#load(String)`, edit it,
/// `#toJson()` it back. Turning it into live objects is the realizer's job --
/// `#realizeSprites(Scene, AssetCatalog)` builds the 2D / board sprites here, while
/// `GameSceneView` wires the full lifecycle (including the 3D camera and lights, which
/// need the GPU device).
public class GameLevel {
    /// The kind of a level, which decides how its elements are realized and rendered. Each
    /// constant carries its JSON wire token (`#wireName()`) and a display `#label()`.
    public enum Mode {
        /// Orthographic 2D: tile layers + sprites in pixel space.
        TWO_D("2d", "2D"),
        /// Perspective 3D: meshes, terrain and lights in a world.
        THREE_D("3d", "3D"),
        /// Isometric board: sprites placed through an `IsoProjection`.
        BOARD("board", "Board");

        private final String wire;
        private final String label;

        Mode(String wire, String label) {
            this.wire = wire;
            this.label = label;
        }

        /// The lowercase token this mode is written as in the `.game` JSON (`"2d"` / `"3d"` /
        /// `"board"`).
        public String wireName() {
            return wire;
        }

        /// A human-readable label for menus (`"2D"` / `"3D"` / `"Board"`).
        public String label() {
            return label;
        }

        /// The mode for a JSON wire token (case-insensitive); defaults to `#TWO_D` for an
        /// unknown or missing token, so an older or hand-edited file still loads.
        public static Mode fromWire(String name) {
            for (Mode m : values()) {
                if (m.wire.equalsIgnoreCase(name)) {
                    return m;
                }
            }
            return TWO_D;
        }
    }

    private Mode mode = Mode.TWO_D;
    private String assetPack;
    private int cols = 20;
    private int rows = 12;
    private int tileSize = 32;

    private final Map<String, Object> props = new HashMap<String, Object>();
    private final List<Layer> layers = new ArrayList<Layer>();
    private final List<GameElement> elements = new ArrayList<GameElement>();
    private final List<LevelLight> lights = new ArrayList<LevelLight>();
    private TerrainGrid terrain;
    private transient GameWorld world;

    // 3D camera rig
    private float eyeX;
    private float eyeY = 6f;
    private float eyeZ = 12f;
    private float targetX;
    private float targetY;
    private float targetZ;
    private float fov = 60f;
    private float near = 0.1f;
    private float far = 1000f;

    public GameLevel() {
    }

    public GameLevel(Mode mode) {
        this.mode = mode == null ? Mode.TWO_D : mode;
    }

    // ---- mode ----------------------------------------------------------------

    /// This level's `Mode`.
    public Mode getMode() {
        return mode;
    }

    public GameLevel setMode(Mode mode) {
        this.mode = mode == null ? Mode.TWO_D : mode;
        return this;
    }

    /// True for an orthographic 2D level (`Mode#TWO_D`).
    public boolean is2D() {
        return mode == Mode.TWO_D;
    }

    /// True for a perspective 3D level (`Mode#THREE_D`).
    public boolean is3D() {
        return mode == Mode.THREE_D;
    }

    /// True for an isometric board level (`Mode#BOARD`).
    public boolean isBoard() {
        return mode == Mode.BOARD;
    }

    // ---- simple accessors ----------------------------------------------------

    /// The id of the `AssetPack` this level's elements draw from.
    public String getAssetPack() {
        return assetPack;
    }

    /// Sets the asset-pack id (see `#getAssetPack()`).
    public GameLevel setAssetPack(String assetPack) {
        this.assetPack = assetPack;
        return this;
    }

    /// The grid width in cells.
    public int getCols() {
        return cols;
    }

    /// The grid height in cells.
    public int getRows() {
        return rows;
    }

    /// The size of one grid cell -- pixels per tile in 2D / board, world units per cell in 3D.
    public int getTileSize() {
        return tileSize;
    }

    /// Sets the grid dimensions and cell size in one call.
    public GameLevel setGrid(int cols, int rows, int tileSize) {
        this.cols = cols;
        this.rows = rows;
        this.tileSize = tileSize;
        return this;
    }

    /// The level-wide authoring property bag (gravity, background, ...).
    public Map<String, Object> props() {
        return props;
    }

    /// A level property as a double, coerced from a `Number`/numeric `String`, else `def`.
    public double getDouble(String key, double def) {
        return Json.num(props.get(key), def);
    }

    /// A level property as an int, coerced from a `Number`/numeric `String`, else `def`.
    public int getInt(String key, int def) {
        return Json.intval(props.get(key), def);
    }

    /// A level property as a String (any value's `toString()`), or `def` if absent.
    public String getString(String key, String def) {
        Object v = props.get(key);
        return v == null ? def : v.toString();
    }

    // ---- layers & elements ---------------------------------------------------

    /// The ordered `Layer` stack (low band first); mutable.
    public List<Layer> layers() {
        return layers;
    }

    /// Appends a layer to the stack.
    public GameLevel addLayer(Layer layer) {
        layers.add(layer);
        return this;
    }

    /// The layer with the given name, or `null` if there is none.
    public Layer getLayer(String name) {
        for (Layer l : layers) {
            if (name == null ? l.getName() == null : name.equals(l.getName())) {
                return l;
            }
        }
        return null;
    }

    /// The placed `GameElement`s, across all layers; mutable.
    public List<GameElement> elements() {
        return elements;
    }

    /// Adds a placed element to the level.
    public GameLevel addElement(GameElement element) {
        elements.add(element);
        return this;
    }

    // ---- 3D extras -----------------------------------------------------------

    public List<LevelLight> lights() {
        return lights;
    }

    public TerrainGrid getTerrain() {
        return terrain;
    }

    public GameLevel setTerrain(TerrainGrid terrain) {
        this.terrain = terrain;
        return this;
    }

    /// The streaming/region world backing a "large world" level, or null for a bounded level.
    /// When set, the active region's `StreamingTerrain` is the authoritative terrain and the
    /// bounded `#getTerrain()` grid is unused. Serialized inline under the "world" key.
    public GameWorld getWorld() {
        return world;
    }

    public GameLevel setWorld(GameWorld world) {
        this.world = world;
        return this;
    }

    /// True when this level is backed by a streaming `GameWorld` of regions rather than a
    /// single bounded grid.
    public boolean isLargeWorld() {
        return world != null;
    }

    public GameLevel setCamera(float eyeX, float eyeY, float eyeZ,
                               float targetX, float targetY, float targetZ) {
        this.eyeX = eyeX;
        this.eyeY = eyeY;
        this.eyeZ = eyeZ;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        return this;
    }

    public GameLevel setLens(float fov, float near, float far) {
        this.fov = fov;
        this.near = near;
        this.far = far;
        return this;
    }

    public float getEyeX() {
        return eyeX;
    }

    public float getEyeY() {
        return eyeY;
    }

    public float getEyeZ() {
        return eyeZ;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public float getTargetZ() {
        return targetZ;
    }

    public float getFov() {
        return fov;
    }

    public float getNear() {
        return near;
    }

    public float getFar() {
        return far;
    }

    // ---- realization (2D / board) -------------------------------------------

    /// Builds `Sprite`s for the 2D / board content of this level and adds them to the
    /// scene: every painted tile of each visible `Layer.Kind#TILE` layer plus every
    /// `GameElement` whose layer is an entity layer. Each sprite's z-order is its
    /// layer band times 1000 (so a higher layer always draws on top), and its
    /// `Sprite#getUserData()` is the source `GameElement` (for tiles, the source
    /// `Layer`), so an editor can map a picked sprite back to its data.
    ///
    /// In `Mode#THREE_D` this is a no-op (3D content is realized by `GameSceneView` once the
    /// GPU device exists). In `Mode#BOARD` positions go through `projection` (board
    /// elements store their column/row in x/y), so pass a fitted `IsoProjection`.
    public void realizeSprites(Scene scene, AssetCatalog catalog) {
        realizeSprites(scene, catalog, null);
    }

    public void realizeSprites(Scene scene, AssetCatalog catalog, IsoProjection projection) {
        if (mode == Mode.THREE_D) {
            return;
        }
        // tile layers
        for (Layer layer : layers) {
            if (layer.getKind() != Layer.Kind.TILE || !layer.isVisible()) {
                continue;
            }
            int z = layer.getBand() * 1000;
            Map<String, String> tiles = layer.tiles();
            for (Map.Entry<String, String> e : tiles.entrySet()) {
                int[] cr = parseCell(e.getKey());
                if (cr == null) {
                    continue;
                }
                Image img = catalog == null ? null : catalog.image(e.getValue());
                if (img == null) {
                    continue;
                }
                Sprite s = new Sprite(img);
                placeCell(s, cr[0], cr[1], projection);
                s.setZOrder(z);
                s.setUserData(layer);
                s.setParallax(layer.getParallaxX(), layer.getParallaxY());
                scene.add(s);
            }
        }
        // freely placed elements
        for (GameElement el : elements) {
            Layer layer = getLayer(el.getLayer());
            if (layer != null && !layer.isVisible()) {
                continue;
            }
            Image img = catalog == null ? null : catalog.image(el.getAssetId());
            if (img == null) {
                continue;
            }
            Sprite s = makeSprite(catalog, el.getAssetId(), img);
            // render at the asset's intended size, not the raw frame size (a sprite-sheet
            // frame can be far larger than the on-screen sprite)
            AssetDef def = catalog == null ? null : catalog.def(el.getAssetId());
            if (def != null) {
                s.setSize(def.getWidth(), def.getHeight());
            }
            if (mode == Mode.BOARD && projection != null) {
                // board elements are placed by their (col,row) stored as x,y
                placeCell(s, (int) Math.round(el.getX()), (int) Math.round(el.getY()), projection);
            } else {
                s.setAnchor(0.5, 0.5);
                s.setPosition(el.getX(), el.getY(), el.getZ());
            }
            s.setRotation(el.getRotation());
            s.setScale(el.getScaleX(), el.getScaleY());
            s.setZOrder((layer == null ? 0 : layer.getBand() * 1000) + 1);
            s.setUserData(el);
            if (layer != null) {
                s.setParallax(layer.getParallaxX(), layer.getParallaxY());
            }
            scene.add(s);
        }
    }

    /// Builds the runtime sprite for an element: an animated `AnimatedSprite` when the
    /// asset is a sprite sheet (`AssetDef.Type#SHEET`), else a plain `Sprite`.
    private Sprite makeSprite(AssetCatalog catalog, String assetId, Image img) {
        if (catalog != null) {
            AssetDef def = catalog.def(assetId);
            SpriteSheet sheet = catalog.sheet(assetId);
            if (def != null && def.isSheet() && sheet != null) {
                int n = def.getFrameCount() > 0 ? def.getFrameCount() : sheet.getFrameCount();
                int[] idx = new int[Math.max(1, n)];
                for (int i = 0; i < idx.length; i++) {
                    idx[i] = i;
                }
                AnimatedSprite anim = new AnimatedSprite(sheet, idx, 1.0 / Math.max(1.0, def.getFps()));
                anim.setLooping(true);
                anim.play();
                return anim;
            }
        }
        return new Sprite(img);
    }

    private void placeCell(Sprite s, int col, int row, IsoProjection projection) {
        if (mode == Mode.BOARD && projection != null) {
            s.setAnchor(0.5, 0.5);
            s.setPosition(projection.tileCenterX(row, col), projection.tileCenterY(row, col));
        } else {
            s.setAnchor(0, 0);
            s.setPosition(col * tileSize, row * tileSize);
        }
    }

    private static int[] parseCell(String key) {
        int comma = key.indexOf(',');
        if (comma < 0) {
            return null;
        }
        try {
            int col = Integer.parseInt(key.substring(0, comma).trim());
            int row = Integer.parseInt(key.substring(comma + 1).trim());
            return new int[]{col, row};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- serialization -------------------------------------------------------

    /// Parses a level from a JSON string.
    public static GameLevel load(String json) throws IOException {
        return fromMap(JSONParser.parseJSON(json));
    }

    /// Parses a level from a UTF-8 JSON stream and closes it.
    public static GameLevel load(InputStream in) throws IOException {
        Reader r = new InputStreamReader(in, "UTF-8");
        try {
            return fromMap(new JSONParser().parseJSON(r));
        } finally {
            try {
                r.close();
            } catch (IOException ignore) { //NOPMD - closing the reader is best-effort
            }
        }
    }

    @SuppressWarnings("unchecked")
    static GameLevel fromMap(Map<String, Object> root) {
        GameLevel level = new GameLevel();
        if (root == null) {
            return level;
        }
        level.mode = Mode.fromWire(Json.str(root.get("mode"), "2d"));
        level.assetPack = Json.str(root.get("assetPack"), null);
        level.cols = Json.intval(root.get("cols"), level.cols);
        level.rows = Json.intval(root.get("rows"), level.rows);
        level.tileSize = Json.intval(root.get("tileSize"), level.tileSize);

        Map<String, Object> p = Json.asMap(root.get("props"));
        if (p != null) {
            level.props.putAll(p);
        }

        Map<String, Object> cam = Json.asMap(root.get("camera"));
        if (cam != null) {
            float[] eye = readVec3(cam.get("eye"), level.eyeX, level.eyeY, level.eyeZ);
            float[] tgt = readVec3(cam.get("target"), level.targetX, level.targetY, level.targetZ);
            level.eyeX = eye[0];
            level.eyeY = eye[1];
            level.eyeZ = eye[2];
            level.targetX = tgt[0];
            level.targetY = tgt[1];
            level.targetZ = tgt[2];
            level.fov = (float) Json.num(cam.get("fov"), level.fov);
            level.near = (float) Json.num(cam.get("near"), level.near);
            level.far = (float) Json.num(cam.get("far"), level.far);
        }

        List<Object> lts = Json.asList(root.get("lights"));
        if (lts != null) {
            for (Object lightEntry : lts) {
                Map<String, Object> lm = Json.asMap(lightEntry);
                if (lm == null) {
                    continue;
                }
                float[] dir = readVec3(lm.get("dir"), 0, -1, 0);
                level.lights.add(new LevelLight(dir[0], dir[1], dir[2],
                        Json.intval(lm.get("color"), 0xffffffff),
                        Json.intval(lm.get("ambient"), 0xff202020)));
            }
        }

        Map<String, Object> ter = Json.asMap(root.get("terrain"));
        if (ter != null) {
            int tc = Json.intval(ter.get("cols"), 0);
            int tr = Json.intval(ter.get("rows"), 0);
            TerrainGrid grid = new TerrainGrid(tc, tr, (float) Json.num(ter.get("cellSize"), 1));
            List<Object> hs = Json.asList(ter.get("heights"));
            float[] heights = grid.heights();
            if (hs != null && heights != null) {
                for (int i = 0; i < hs.size() && i < heights.length; i++) {
                    heights[i] = (float) Json.num(hs.get(i), 0);
                }
            }
            List<Object> ws = Json.asList(ter.get("walls"));
            float[] walls = grid.walls();
            if (ws != null) {
                for (int i = 0; i < ws.size() && i < walls.length; i++) {
                    walls[i] = (float) Json.num(ws.get(i), 0);
                }
            }
            List<Object> ms = Json.asList(ter.get("materials"));
            String[] mats = grid.materials();
            if (ms != null) {
                for (int i = 0; i < ms.size() && i < mats.length; i++) {
                    mats[i] = materialId(ms.get(i));
                }
            }
            level.terrain = grid;
        }

        Map<String, Object> wm = Json.asMap(root.get("world"));
        if (wm != null) {
            level.world = GameWorld.fromMap(wm);
        }

        List<Object> ls = Json.asList(root.get("layers"));
        if (ls != null) {
            for (int i = 0; i < ls.size(); i++) {
                Map<String, Object> lm = Json.asMap(ls.get(i));
                if (lm == null) {
                    continue;
                }
                Layer layer = new Layer(Json.str(lm.get("name"), "Layer"), Layer.Kind.fromWire(Json.str(lm.get("kind"), "entity")));
                layer.setVisible(Json.bool(lm.get("visible"), true));
                layer.setLocked(Json.bool(lm.get("locked"), false));
                layer.setBand(Json.intval(lm.get("band"), i));
                layer.setParallax((float) Json.num(lm.get("parallaxX"), 1),
                        (float) Json.num(lm.get("parallaxY"), 1));
                Map<String, Object> tm = Json.asMap(lm.get("tiles"));
                if (tm != null) {
                    for (Map.Entry<String, Object> te : tm.entrySet()) {
                        layer.tiles().put(te.getKey(), Json.str(te.getValue(), null));
                    }
                }
                level.layers.add(layer);
            }
        }

        List<Object> es = Json.asList(root.get("elements"));
        if (es != null) {
            for (Object elementEntry : es) {
                Map<String, Object> em = Json.asMap(elementEntry);
                if (em == null) {
                    continue;
                }
                GameElement el = new GameElement(Json.str(em.get("id"), null), Json.str(em.get("assetId"), null));
                el.setName(Json.str(em.get("name"), null));
                el.setLayer(Json.str(em.get("layer"), null));
                el.setPosition(Json.num(em.get("x"), 0), Json.num(em.get("y"), 0), Json.num(em.get("z"), 0));
                el.setRotation((float) Json.num(em.get("rotation"), 0));
                el.setScale((float) Json.num(em.get("scaleX"), 1),
                        (float) Json.num(em.get("scaleY"), 1),
                        (float) Json.num(em.get("scaleZ"), 1));
                Map<String, Object> ep = Json.asMap(em.get("props"));
                if (ep != null) {
                    el.properties().putAll(ep);
                }
                level.elements.add(el);
            }
        }
        return level;
    }

    /// A terrain material id from a JSON value: a String id is used as-is; a legacy numeric
    /// ordinal is mapped onto the original built-in order (0=grass..5=dirt) so older `.game`
    /// files that stored material indices still load.
    private static String materialId(Object v) {
        if (v instanceof String) {
            return (String) v;
        }
        switch (Json.intval(v, 0)) {
            case 1: return MaterialRegistry.ROAD;
            case 2: return MaterialRegistry.STONE;
            case 3: return MaterialRegistry.SAND;
            case 4: return MaterialRegistry.WATER;
            case 5: return MaterialRegistry.DIRT;
            default: return MaterialRegistry.GRASS;
        }
    }

    private static float[] readVec3(Object v, float dx, float dy, float dz) {
        List<Object> l = Json.asList(v);
        if (l == null) {
            return new float[]{dx, dy, dz};
        }
        return new float[]{
                !l.isEmpty() ? (float) Json.num(l.get(0), dx) : dx,
                l.size() > 1 ? (float) Json.num(l.get(1), dy) : dy,
                l.size() > 2 ? (float) Json.num(l.get(2), dz) : dz};
    }


    /// Serializes this level to a compact JSON string (round-trips through
    /// `#load(String)`).
    public String toJson() {
        StringBuilder sb = new StringBuilder(512);
        sb.append('{');
        kv(sb, "mode");
        Json.writeString(sb, mode.wireName());
        if (assetPack != null) {
            sb.append(',');
            kv(sb, "assetPack");
            Json.writeString(sb, assetPack);
        }
        sb.append(',');
        kv(sb, "cols");
        Json.writeNumber(sb, cols);
        sb.append(',');
        kv(sb, "rows");
        Json.writeNumber(sb, rows);
        sb.append(',');
        kv(sb, "tileSize");
        Json.writeNumber(sb, tileSize);

        if (!props.isEmpty()) {
            sb.append(',');
            kv(sb, "props");
            Json.writeValue(sb, props);
        }

        if (mode == Mode.THREE_D) {
            sb.append(',');
            kv(sb, "camera");
            sb.append("{\"eye\":[");
            Json.writeNumber(sb, eyeX);
            sb.append(',');
            Json.writeNumber(sb, eyeY);
            sb.append(',');
            Json.writeNumber(sb, eyeZ);
            sb.append("],\"target\":[");
            Json.writeNumber(sb, targetX);
            sb.append(',');
            Json.writeNumber(sb, targetY);
            sb.append(',');
            Json.writeNumber(sb, targetZ);
            sb.append("],\"fov\":");
            Json.writeNumber(sb, fov);
            sb.append(",\"near\":");
            Json.writeNumber(sb, near);
            sb.append(",\"far\":");
            Json.writeNumber(sb, far);
            sb.append('}');

            if (!lights.isEmpty()) {
                sb.append(',');
                kv(sb, "lights");
                sb.append('[');
                for (int i = 0; i < lights.size(); i++) {
                    LevelLight l = lights.get(i);
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append("{\"dir\":[");
                    Json.writeNumber(sb, l.getDirectionX());
                    sb.append(',');
                    Json.writeNumber(sb, l.getDirectionY());
                    sb.append(',');
                    Json.writeNumber(sb, l.getDirectionZ());
                    sb.append("],\"color\":");
                    Json.writeNumber(sb, l.getColor());
                    sb.append(",\"ambient\":");
                    Json.writeNumber(sb, l.getAmbientColor());
                    sb.append('}');
                }
                sb.append(']');
            }

            if (terrain != null) {
                sb.append(',');
                kv(sb, "terrain");
                sb.append("{\"cols\":");
                Json.writeNumber(sb, terrain.getCols());
                sb.append(",\"rows\":");
                Json.writeNumber(sb, terrain.getRows());
                sb.append(",\"cellSize\":");
                Json.writeNumber(sb, terrain.getCellSize());
                sb.append(",\"heights\":[");
                float[] h = terrain.heights();
                if (h != null) {
                    for (int i = 0; i < h.length; i++) {
                        if (i > 0) {
                            sb.append(',');
                        }
                        Json.writeNumber(sb, h[i]);
                    }
                }
                sb.append("],\"walls\":[");
                float[] w = terrain.walls();
                for (int i = 0; i < w.length; i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    Json.writeNumber(sb, w[i]);
                }
                sb.append("],\"materials\":[");
                String[] mm = terrain.materials();
                for (int i = 0; i < mm.length; i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    Json.writeString(sb, mm[i] == null ? MaterialRegistry.GRASS : mm[i]);
                }
                sb.append("]}");
            }
        }

        sb.append(',');
        kv(sb, "layers");
        sb.append('[');
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append('{');
            kv(sb, "name");
            Json.writeString(sb, layer.getName() == null ? "" : layer.getName());
            sb.append(',');
            kv(sb, "kind");
            Json.writeString(sb, layer.getKind().wireName());
            sb.append(",\"visible\":").append(layer.isVisible() ? "true" : "false");
            sb.append(",\"locked\":").append(layer.isLocked() ? "true" : "false");
            sb.append(",\"band\":");
            Json.writeNumber(sb, layer.getBand());
            if (layer.getParallaxX() != 1f) {
                sb.append(",\"parallaxX\":");
                Json.writeNumber(sb, layer.getParallaxX());
            }
            if (layer.getParallaxY() != 1f) {
                sb.append(",\"parallaxY\":");
                Json.writeNumber(sb, layer.getParallaxY());
            }
            if (layer.getKind() == Layer.Kind.TILE && !layer.tiles().isEmpty()) {
                sb.append(',');
                kv(sb, "tiles");
                sb.append('{');
                boolean first = true;
                for (Map.Entry<String, String> e : layer.tiles().entrySet()) {
                    if (!first) {
                        sb.append(',');
                    }
                    first = false;
                    Json.writeString(sb, e.getKey());
                    sb.append(':');
                    Json.writeString(sb, e.getValue());
                }
                sb.append('}');
            }
            sb.append('}');
        }
        sb.append(']');

        sb.append(',');
        kv(sb, "elements");
        sb.append('[');
        for (int i = 0; i < elements.size(); i++) {
            GameElement el = elements.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append('{');
            kv(sb, "id");
            Json.writeString(sb, el.getId() == null ? "" : el.getId());
            sb.append(',');
            kv(sb, "assetId");
            Json.writeString(sb, el.getAssetId() == null ? "" : el.getAssetId());
            if (el.getName() != null) {
                sb.append(',');
                kv(sb, "name");
                Json.writeString(sb, el.getName());
            }
            if (el.getLayer() != null) {
                sb.append(',');
                kv(sb, "layer");
                Json.writeString(sb, el.getLayer());
            }
            sb.append(",\"x\":");
            Json.writeNumber(sb, el.getX());
            sb.append(",\"y\":");
            Json.writeNumber(sb, el.getY());
            if (el.getZ() != 0) {
                sb.append(",\"z\":");
                Json.writeNumber(sb, el.getZ());
            }
            if (el.getRotation() != 0) {
                sb.append(",\"rotation\":");
                Json.writeNumber(sb, el.getRotation());
            }
            if (el.getScaleX() != 1 || el.getScaleY() != 1 || el.getScaleZ() != 1) {
                sb.append(",\"scaleX\":");
                Json.writeNumber(sb, el.getScaleX());
                sb.append(",\"scaleY\":");
                Json.writeNumber(sb, el.getScaleY());
                sb.append(",\"scaleZ\":");
                Json.writeNumber(sb, el.getScaleZ());
            }
            if (!el.properties().isEmpty()) {
                sb.append(',');
                kv(sb, "props");
                Json.writeValue(sb, el.properties());
            }
            sb.append('}');
        }
        sb.append(']');

        if (world != null) {
            sb.append(',');
            kv(sb, "world");
            sb.append(world.toJson());
        }

        sb.append('}');
        return sb.toString();
    }

    /// Writes this level as UTF-8 JSON to the stream (does not close it).
    public void save(OutputStream out) throws IOException {
        out.write(toJson().getBytes("UTF-8"));
        out.flush();
    }

    private static void kv(StringBuilder sb, String key) {
        Json.writeString(sb, key);
        sb.append(':');
    }
}
