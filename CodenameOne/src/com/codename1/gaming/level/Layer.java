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

import java.util.LinkedHashMap;
import java.util.Map;

/// A named drawing band in a `GameLevel`: the editor's "Background / Terrain / Items /
/// Actors" rows, and the rendering order behind them.
///
/// A layer has a `#getKind()` -- `#KIND_TILE` layers paint a grid of asset ids into
/// cells (stored as a `"col,row" -> assetId` map), while `#KIND_ENTITY` and
/// `#KIND_MODEL` layers just group freely-placed `GameElement`s. `#isVisible()` and
/// `#isLocked()` mirror the editor toggles; `#getBand()` is the layer's slot in the
/// stack and is folded into the rendered z-order (`band * 1000 + per-sprite z`) so a
/// higher layer always draws over a lower one.
public class Layer {
    /// A grid of tiles addressed by cell, each cell holding an asset id.
    public static final int KIND_TILE = 0;
    /// A group of freely placed 2D entities (sprites).
    public static final int KIND_ENTITY = 1;
    /// A group of freely placed 3D models.
    public static final int KIND_MODEL = 2;

    private String name;
    private int kind = KIND_ENTITY;
    private boolean visible = true;
    private boolean locked;
    private int band;

    /// cellKey ("col,row") -> assetId, only used by `#KIND_TILE` layers. Insertion
    /// ordered so a saved level reloads its tiles in a stable order.
    private final Map<String, String> tiles = new LinkedHashMap<String, String>();

    public Layer() {
    }

    public Layer(String name, int kind) {
        this.name = name;
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public Layer setName(String name) {
        this.name = name;
        return this;
    }

    public int getKind() {
        return kind;
    }

    public Layer setKind(int kind) {
        this.kind = kind;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public Layer setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public boolean isLocked() {
        return locked;
    }

    public Layer setLocked(boolean locked) {
        this.locked = locked;
        return this;
    }

    public int getBand() {
        return band;
    }

    public Layer setBand(int band) {
        this.band = band;
        return this;
    }

    /// The raw `"col,row" -> assetId` map for a tile layer.
    public Map<String, String> tiles() {
        return tiles;
    }

    /// The cell key used in the tile map for a column and row.
    public static String cellKey(int col, int row) {
        return col + "," + row;
    }

    /// Paints an asset into a tile cell (no-op safety: a null assetId clears it).
    public Layer putTile(int col, int row, String assetId) {
        if (assetId == null) {
            tiles.remove(cellKey(col, row));
        } else {
            tiles.put(cellKey(col, row), assetId);
        }
        return this;
    }

    /// The asset id painted at a cell, or null if the cell is empty.
    public String getTile(int col, int row) {
        return tiles.get(cellKey(col, row));
    }

    public Layer removeTile(int col, int row) {
        tiles.remove(cellKey(col, row));
        return this;
    }
}
