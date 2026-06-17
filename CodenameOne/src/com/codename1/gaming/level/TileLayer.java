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

import com.codename1.gaming.Scene;
import com.codename1.gaming.Sprite;
import com.codename1.gaming.SpriteSheet;

/// An efficient, single-tileset tile grid: a `#getCols()` x `#getRows()` array of frame
/// indices into one `SpriteSheet` (-1 means "empty cell").
///
/// This is the fast representation a game uses when every tile comes from the same
/// atlas -- contrast with a `Layer` of `Layer#KIND_TILE`, which stores an
/// `assetId`-per-cell map and can mix assets from different packs. `#toScene(Scene,
/// int)` materializes the non-empty cells as `Sprite`s anchored at their top-left at
/// `tileSize` spacing.
public class TileLayer {
    private final SpriteSheet sheet;
    private final int cols;
    private final int rows;
    private final int tileSize;
    private final int[] cells;

    public TileLayer(SpriteSheet sheet, int cols, int rows, int tileSize) {
        if (sheet == null) {
            throw new IllegalArgumentException("sheet is null");
        }
        if (cols <= 0 || rows <= 0) {
            throw new IllegalArgumentException("grid must be positive");
        }
        this.sheet = sheet;
        this.cols = cols;
        this.rows = rows;
        this.tileSize = tileSize;
        this.cells = new int[cols * rows];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = -1;
        }
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public int getTileSize() {
        return tileSize;
    }

    public SpriteSheet getSheet() {
        return sheet;
    }

    /// The frame index painted at a cell, or -1 if empty.
    public int getTile(int col, int row) {
        if (col < 0 || row < 0 || col >= cols || row >= rows) {
            return -1;
        }
        return cells[row * cols + col];
    }

    /// Paints a frame index into a cell (-1 clears it).
    public TileLayer setTile(int col, int row, int frame) {
        if (col >= 0 && row >= 0 && col < cols && row < rows) {
            cells[row * cols + col] = frame;
        }
        return this;
    }

    /// Adds a `Sprite` for every non-empty cell to the scene, anchored at the cell's
    /// top-left corner and z-ordered at `zOrder`.
    public void toScene(Scene scene, int zOrder) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int frame = cells[row * cols + col];
                if (frame < 0) {
                    continue;
                }
                Sprite s = new Sprite(sheet.getFrame(frame));
                s.setAnchor(0, 0);
                s.setPosition(col * tileSize, row * tileSize);
                s.setZOrder(zOrder);
                scene.add(s);
            }
        }
    }
}
