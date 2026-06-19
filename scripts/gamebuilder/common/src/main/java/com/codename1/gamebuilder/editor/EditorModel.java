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
import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameLevel;

/// The mutable editing session: the level being edited plus the catalog and the
/// transient selection / tool state the UI binds to. Pure data -- no UI dependency --
/// so the `EditorController` operating on it is unit-testable headlessly.
public class EditorModel {
    private GameLevel level;
    private final AssetCatalog catalog;
    private String sceneName = "Level1";

    private Tool tool = Tool.BRUSH;
    private String activeLayer;
    private String selectedAssetId;
    private GameElement selection;
    private boolean dirty;
    private boolean snap = true;
    private TerrainBrush terrainBrush = TerrainBrush.RAISE;
    private String terrainMaterial = com.codename1.gaming.level.MaterialRegistry.GRASS;

    public EditorModel(GameLevel level, AssetCatalog catalog) {
        this.catalog = catalog;
        setLevel(level);
    }

    public GameLevel level() {
        return level;
    }

    /// Replaces the level (e.g. after new / load / undo) and resets the selection,
    /// defaulting the active layer to the last one (the topmost authoring layer).
    public void setLevel(GameLevel level) {
        this.level = level;
        this.selection = null;
        this.activeLayer = level.layers().isEmpty() ? null
                : level.layers().get(level.layers().size() - 1).getName();
        String pack = level.getAssetPack();
        this.selectedAssetId = null;
        if (pack != null && catalog != null && catalog.getPack(pack) != null
                && !catalog.getPack(pack).assets().isEmpty()) {
            this.selectedAssetId = catalog.getPack(pack).assets().get(0).getId();
        }
    }

    public AssetCatalog catalog() {
        return catalog;
    }

    public String getSceneName() {
        return sceneName;
    }

    public void setSceneName(String sceneName) {
        this.sceneName = sceneName;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    /// The action the Terrain tool applies to the cell under the pointer.
    public TerrainBrush getTerrainBrush() {
        return terrainBrush;
    }

    public void setTerrainBrush(TerrainBrush terrainBrush) {
        this.terrainBrush = terrainBrush;
    }

    /// The surface material the PAINT brush / Fill applies (see TerrainGrid.MAT_*).
    public String getTerrainMaterial() {
        return terrainMaterial;
    }

    public void setTerrainMaterial(String terrainMaterial) {
        this.terrainMaterial = terrainMaterial;
    }

    public String getActiveLayer() {
        return activeLayer;
    }

    public void setActiveLayer(String activeLayer) {
        this.activeLayer = activeLayer;
    }

    public String getSelectedAssetId() {
        return selectedAssetId;
    }

    public void setSelectedAssetId(String selectedAssetId) {
        this.selectedAssetId = selectedAssetId;
    }

    public GameElement getSelection() {
        return selection;
    }

    public void setSelection(GameElement selection) {
        this.selection = selection;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /// Whether freely-placed actors snap to grid cells when stamped.
    public boolean isSnap() {
        return snap;
    }

    public void setSnap(boolean snap) {
        this.snap = snap;
    }
}
