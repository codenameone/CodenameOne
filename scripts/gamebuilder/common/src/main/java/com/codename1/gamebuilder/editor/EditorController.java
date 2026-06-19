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
package com.codename1.gamebuilder.editor;

import com.codename1.gaming.level.AssetCatalog;
import com.codename1.gaming.level.AssetDef;
import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameLevel;
import com.codename1.gaming.level.Layer;
import com.codename1.gaming.level.TerrainGrid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// All mutating level edits go through here so they are uniformly undoable and so the
/// UI stays a thin view. Every public mutation snapshots the level first (JSON), marks
/// the model dirty, and is reversible with `#undo()` / `#redo()`.
///
/// It is deliberately free of any Codename One UI type, which keeps it unit-testable on
/// a plain JVM (see {@code EditorControllerTest}).
public class EditorController {
    private static final int MAX_HISTORY = 50;

    private final EditorModel model;
    // Stacks implemented over ArrayList (top == last element); java.util.ArrayDeque is
    // outside the Codename One device API subset.
    private final List<String> undo = new ArrayList<>();
    private final List<String> redo = new ArrayList<>();
    private int idCounter;

    private static String pop(List<String> stack) {
        return stack.remove(stack.size() - 1);
    }

    public EditorController(EditorModel model) {
        this.model = model;
        syncCounter();
    }

    public EditorModel model() {
        return model;
    }

    // ---- new / undo / redo ---------------------------------------------------

    /// Starts a fresh level for the given `GameLevel` mode, clearing history.
    public void newLevel(GameLevel.Mode mode) {
        undo.clear();
        redo.clear();
        model.setLevel(StarterPacks.newLevel(mode));
        model.setDirty(false);
        syncCounter();
    }

    /// Loads an existing level (e.g. switching scenes), clearing history.
    public void loadLevel(GameLevel level, String sceneName) {
        undo.clear();
        redo.clear();
        model.setLevel(level);
        model.setSceneName(sceneName);
        model.setDirty(false);
        syncCounter();
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    public void undo() {
        if (undo.isEmpty()) {
            return;
        }
        redo.add(model.level().toJson());
        applyJson(pop(undo));
    }

    public void redo() {
        if (redo.isEmpty()) {
            return;
        }
        undo.add(model.level().toJson());
        applyJson(pop(redo));
    }

    private void applyJson(String json) {
        String layer = model.getActiveLayer();
        String asset = model.getSelectedAssetId();
        Tool tool = model.getTool();
        try {
            model.setLevel(GameLevel.load(json));
        } catch (IOException e) {
            throw new IllegalStateException("corrupt history snapshot", e);
        }
        if (model.level().getLayer(layer) != null) {
            model.setActiveLayer(layer);
        }
        model.setSelectedAssetId(asset);
        model.setTool(tool);
        model.setDirty(true);
        syncCounter();
    }

    /// Snapshots the current level for undo before a mutation.
    private void beginEdit() {
        undo.add(model.level().toJson());
        if (undo.size() > MAX_HISTORY) {
            undo.remove(0);   // drop the oldest snapshot
        }
        redo.clear();
    }

    private void syncCounter() {
        idCounter = 0;
        List<GameElement> els = model.level().elements();
        for (int i = 0; i < els.size(); i++) {
            String id = els.get(i).getId();
            if (id != null && id.startsWith("e")) {
                try {
                    idCounter = Math.max(idCounter, Integer.parseInt(id.substring(1)));
                } catch (NumberFormatException ignore) {
                    // non-numeric id, leave counter
                }
            }
        }
    }

    // ---- tile editing --------------------------------------------------------

    private Layer activeTileLayer() {
        Layer l = model.level().getLayer(model.getActiveLayer());
        return (l != null && l.getKind() == Layer.Kind.TILE) ? l : null;
    }

    private boolean inGrid(int col, int row) {
        return col >= 0 && row >= 0 && col < model.level().getCols() && row < model.level().getRows();
    }

    /// Paints the selected tile asset into a cell of the active tile layer.
    public boolean paintTile(int col, int row) {
        Layer layer = activeTileLayer();
        String asset = model.getSelectedAssetId();
        if (layer == null || layer.isLocked() || asset == null || !inGrid(col, row)) {
            return false;
        }
        if (asset.equals(layer.getTile(col, row))) {
            return false; // no-op, don't pollute history
        }
        beginEdit();
        layer.putTile(col, row, asset);
        model.setDirty(true);
        return true;
    }

    /// Clears a cell of the active tile layer.
    public boolean eraseTile(int col, int row) {
        Layer layer = activeTileLayer();
        if (layer == null || layer.isLocked() || layer.getTile(col, row) == null) {
            return false;
        }
        beginEdit();
        layer.removeTile(col, row);
        model.setDirty(true);
        return true;
    }

    /// Flood-fills the contiguous region of cells matching the cell's current asset
    /// (4-connected) with the selected asset, bounded by the grid.
    public int floodFill(int col, int row) {
        Layer layer = activeTileLayer();
        String target = model.getSelectedAssetId();
        if (layer == null || layer.isLocked() || target == null || !inGrid(col, row)) {
            return 0;
        }
        String from = layer.getTile(col, row);
        if (target.equals(from)) {
            return 0;
        }
        beginEdit();
        int filled = 0;
        List<int[]> stack = new ArrayList<>();
        stack.add(new int[]{col, row});
        while (!stack.isEmpty()) {
            int[] c = stack.remove(stack.size() - 1);
            int cc = c[0];
            int cr = c[1];
            if (!inGrid(cc, cr)) {
                continue;
            }
            String here = layer.getTile(cc, cr);
            boolean match = from == null ? here == null : from.equals(here);
            if (!match || target.equals(here)) {
                continue;
            }
            layer.putTile(cc, cr, target);
            filled++;
            stack.add(new int[]{cc + 1, cr});
            stack.add(new int[]{cc - 1, cr});
            stack.add(new int[]{cc, cr + 1});
            stack.add(new int[]{cc, cr - 1});
        }
        if (filled == 0) {
            pop(undo); // nothing changed; drop the snapshot
        } else {
            model.setDirty(true);
        }
        return filled;
    }

    // ---- element editing -----------------------------------------------------

    /// The entity/model layer a new element should join: the active layer if it accepts
    /// elements, otherwise the first such layer.
    private Layer targetElementLayer() {
        Layer active = model.level().getLayer(model.getActiveLayer());
        if (active != null && active.getKind() != Layer.Kind.TILE) {
            return active;
        }
        List<Layer> ls = model.level().layers();
        for (int i = 0; i < ls.size(); i++) {
            if (ls.get(i).getKind() != Layer.Kind.TILE) {
                return ls.get(i);
            }
        }
        return null;
    }

    /// Places (stamps) the selected actor asset at a position. Honors `AssetDef#isUnique()`
    /// by relocating the existing instance instead of adding a second. Returns the
    /// affected element, or null if nothing could be placed.
    public GameElement placeElement(double x, double y) {
        AssetCatalog cat = model.catalog();
        String assetId = model.getSelectedAssetId();
        Layer layer = targetElementLayer();
        if (cat == null || assetId == null || layer == null || layer.isLocked()) {
            return null;
        }
        AssetDef def = cat.def(assetId);
        if (def == null || def.isTile()) {
            return null;
        }
        if (def.isUnique()) {
            GameElement existing = findByAsset(assetId);
            if (existing != null) {
                beginEdit();
                existing.setPosition(x, y);
                model.setSelection(existing);
                model.setDirty(true);
                return existing;
            }
        }
        beginEdit();
        GameElement el = new GameElement("e" + (++idCounter), assetId);
        el.setName(def.getName() + " " + idCounter).setLayer(layer.getName());
        el.setPosition(x, y);
        el.properties().putAll(def.defaultProperties());
        model.level().addElement(el);
        model.setSelection(el);
        model.setDirty(true);
        return el;
    }

    private GameElement findByAsset(String assetId) {
        List<GameElement> els = model.level().elements();
        for (int i = 0; i < els.size(); i++) {
            if (assetId.equals(els.get(i).getAssetId())) {
                return els.get(i);
            }
        }
        return null;
    }

    /// Deletes the current selection, if any.
    public boolean deleteSelection() {
        GameElement sel = model.getSelection();
        if (sel == null) {
            return false;
        }
        beginEdit();
        model.level().elements().remove(sel);
        model.setSelection(null);
        model.setDirty(true);
        return true;
    }

    /// Moves the selected element by a world delta.
    public boolean moveSelectionBy(double dx, double dy) {
        GameElement sel = model.getSelection();
        if (sel == null || selectionLocked()) {
            return false;
        }
        beginEdit();
        sel.setPosition(sel.getX() + dx, sel.getY() + dy, sel.getZ());
        model.setDirty(true);
        return true;
    }

    /// Moves the selected element to an absolute world position (its centre), so a drag
    /// keeps the object exactly under the cursor. No-op on a locked layer.
    public boolean moveSelectionTo(double x, double y) {
        GameElement sel = model.getSelection();
        if (sel == null || selectionLocked()) {
            return false;
        }
        beginEdit();
        sel.setPosition(x, y, sel.getZ());
        model.setDirty(true);
        return true;
    }

    /// Hit-tests the freely placed elements (topmost first) for an axis-aligned box
    /// centered on each element, sized from its asset def. Returns the hit element or
    /// null. (2D / 3D entity layers; board picking is projection-specific.)
    public GameElement elementAt(double x, double y) {
        AssetCatalog cat = model.catalog();
        List<GameElement> els = model.level().elements();
        for (int i = els.size() - 1; i >= 0; i--) {
            GameElement el = els.get(i);
            Layer layer = layerOf(el);
            if (layer != null && !layer.isVisible()) {
                continue;   // can't pick something you can't see
            }
            AssetDef def = cat == null ? null : cat.def(el.getAssetId());
            double w = def == null ? 32 : def.getWidth();
            double h = def == null ? 32 : def.getHeight();
            if (x >= el.getX() - w / 2 && x <= el.getX() + w / 2
                    && y >= el.getY() - h / 2 && y <= el.getY() + h / 2) {
                return el;
            }
        }
        return null;
    }

    /// The layer an element belongs to (by name), or null.
    public Layer layerOf(GameElement el) {
        return el == null ? null : model.level().getLayer(el.getLayer());
    }

    private boolean selectionLocked() {
        Layer l = layerOf(model.getSelection());
        return l != null && l.isLocked();
    }

    /// Selects the topmost element under a point (or clears the selection).
    public GameElement selectAt(double x, double y) {
        GameElement el = elementAt(x, y);
        model.setSelection(el);
        return el;
    }

    // ---- scene-level edits ---------------------------------------------------

    /// Resizes the grid (undoable). Ignores non-positive or unchanged dimensions.
    public boolean resizeGrid(int cols, int rows, int tileSize) {
        GameLevel l = model.level();
        if (cols <= 0 || rows <= 0 || tileSize <= 0) {
            return false;
        }
        if (cols == l.getCols() && rows == l.getRows() && tileSize == l.getTileSize()) {
            return false;
        }
        beginEdit();
        l.setGrid(cols, rows, tileSize);
        model.setDirty(true);
        return true;
    }

    /// The elevation/wall change one terrain-brush dab applies, in world (tile) units.
    public static final float TERRAIN_STEP = 0.5f;
    /// Default height of a wall stamped by the WALL brush (a head-height dungeon wall).
    public static final float WALL_HEIGHT = 2.2f;

    /// Returns the level's terrain grid, creating one sized to the grid (and pre-filling it
    /// per game type: a flight level starts as open sky with NO ground, everything else as a
    /// flat floor) if the level has none yet. Only meaningful for MODE_3D.
    public TerrainGrid ensureTerrain() {
        GameLevel l = model.level();
        TerrainGrid t = l.getTerrain();
        int cols = l.getCols();
        int rows = l.getRows();
        if (t == null || t.getCols() != cols || t.getRows() != rows) {
            TerrainGrid grid = new TerrainGrid(cols, rows, 1f);
            boolean sky = "flight".equals(l.getString("view3d", "open"));
            if (t != null) {
                // preserve overlapping cells when the grid was resized
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        grid.setHeight(c, r, t.getHeight(c, r));
                        grid.setWall(c, r, t.getWall(c, r));
                    }
                }
            } else if (sky) {
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        grid.setGround(c, r, false);
                    }
                }
            }
            l.setTerrain(grid);
            t = grid;
        }
        return t;
    }

    /// Applies the current terrain brush to one cell (undoable). Returns true if it changed.
    public boolean paintTerrain(int col, int row, TerrainBrush brush) {
        GameLevel l = model.level();
        if (l.getMode() != GameLevel.Mode.THREE_D || col < 0 || row < 0
                || col >= l.getCols() || row >= l.getRows()) {
            return false;
        }
        TerrainGrid t = ensureTerrain();
        beginEdit();
        switch (brush) {
            case RAISE -> {
                float h = t.hasGround(col, row) ? t.getHeight(col, row) : 0f;
                t.setHeight(col, row, h + TERRAIN_STEP);
            }
            case LOWER -> {
                float h = t.hasGround(col, row) ? t.getHeight(col, row) : 0f;
                t.setHeight(col, row, h - TERRAIN_STEP);
            }
            case GROUND -> t.setGround(col, row, !t.hasGround(col, row));
            case WALL -> t.setWall(col, row, t.getWall(col, row) > 0f ? 0f : WALL_HEIGHT);
            case PAINT -> {
                t.setGround(col, row, true);   // painting a surface implies there is ground
                t.setMaterial(col, row, model.getTerrainMaterial());
            }
        }
        model.setDirty(true);
        return true;
    }

    /// Fills the whole terrain with floor (present=true, at elevation 0) or carves it all
    /// away to open sky (present=false). Undoable. Only meaningful for MODE_3D.
    public void fillGround(boolean present) {
        GameLevel l = model.level();
        if (l.getMode() != GameLevel.Mode.THREE_D) {
            return;
        }
        TerrainGrid t = ensureTerrain();
        beginEdit();
        for (int r = 0; r < l.getRows(); r++) {
            for (int c = 0; c < l.getCols(); c++) {
                t.setGround(c, r, present);
                if (present) {
                    t.setMaterial(c, r, model.getTerrainMaterial());   // fill with the chosen surface
                } else {
                    t.setWall(c, r, 0f);
                }
            }
        }
        model.setDirty(true);
    }

    /// Sets a level-wide property such as gravity or background (undoable).
    public void setLevelProperty(String key, Object value) {
        beginEdit();
        model.level().props().put(key, value);
        model.setDirty(true);
    }

    /// Renames the scene (affects the generated file/class names; not undoable level data).
    public void renameScene(String name) {
        if (name != null && !name.trim().isEmpty()) {
            model.setSceneName(name.trim());
            model.setDirty(true);
        }
    }

    /// Adds a new entity layer above the others and makes it active (undoable).
    public Layer addEntityLayer(String name) {
        beginEdit();
        int band = model.level().layers().size();
        Layer layer = new Layer(name, Layer.Kind.ENTITY).setBand(band);
        model.level().addLayer(layer);
        model.setActiveLayer(name);
        model.setDirty(true);
        return layer;
    }

    /// Renames a layer (undoable), repointing every element that referenced it and the
    /// active-layer pointer. Ignores blanks, duplicates and no-ops.
    public boolean renameLayer(String oldName, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return false;
        }
        newName = newName.trim();
        Layer layer = model.level().getLayer(oldName);
        if (layer == null || newName.equals(oldName) || model.level().getLayer(newName) != null) {
            return false;
        }
        beginEdit();
        layer.setName(newName);
        for (GameElement el : model.level().elements()) {
            if (oldName.equals(el.getLayer())) {
                el.setLayer(newName);
            }
        }
        if (oldName.equals(model.getActiveLayer())) {
            model.setActiveLayer(newName);
        }
        model.setDirty(true);
        return true;
    }

    /// Deletes a layer (undoable). Elements on a deleted entity layer are reassigned to the
    /// nearest remaining entity layer, or removed if none remains. Keeps at least one layer.
    public boolean deleteLayer(String name) {
        List<Layer> layers = model.level().layers();
        Layer layer = model.level().getLayer(name);
        if (layer == null || layers.size() <= 1) {
            return false;
        }
        beginEdit();
        layers.remove(layer);
        if (layer.getKind() != Layer.Kind.TILE) {
            String fallback = null;
            for (Layer l : layers) {
                if (l.getKind() != Layer.Kind.TILE) {
                    fallback = l.getName();
                    break;
                }
            }
            List<GameElement> els = model.level().elements();
            for (int i = els.size() - 1; i >= 0; i--) {
                if (name.equals(els.get(i).getLayer())) {
                    if (fallback != null) {
                        els.get(i).setLayer(fallback);
                    } else {
                        els.remove(i);
                    }
                }
            }
        }
        if (name.equals(model.getActiveLayer())) {
            model.setActiveLayer(layers.get(0).getName());
        }
        model.setSelection(null);
        model.setDirty(true);
        return true;
    }

    /// Reassigns an element to a different layer (undoable) — the hierarchy drag-drop and
    /// the inspector Layer picker both route here.
    public boolean moveElementToLayer(GameElement el, String layerName) {
        if (el == null || layerName == null || layerName.equals(el.getLayer())
                || model.level().getLayer(layerName) == null) {
            return false;
        }
        beginEdit();
        el.setLayer(layerName);
        model.setDirty(true);
        return true;
    }

    /// Reorders an element within the flat element list (undoable) — drag-to-reorder in
    /// the hierarchy. Clamps the target index.
    public boolean reorderElement(GameElement el, int targetIndex) {
        List<GameElement> els = model.level().elements();
        int from = els.indexOf(el);
        if (from < 0) {
            return false;
        }
        int to = Math.max(0, Math.min(els.size() - 1, targetIndex));
        if (to == from) {
            return false;
        }
        beginEdit();
        els.remove(from);
        els.add(to, el);
        model.setDirty(true);
        return true;
    }
}
