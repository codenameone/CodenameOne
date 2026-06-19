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

/// A height grid for a 3D `GameLevel`: a `#getCols()` x `#getRows()` array of vertex
/// heights spaced `#getCellSize()` world units apart. It is plain authoring data --
/// turning it into a renderable mesh is the realizer's job and needs the GPU device,
/// so that happens at runtime, not here.
public class TerrainGrid {
    /// Sentinel height meaning "no ground in this cell" (an open hole / sky gap). Lets a
    /// level have a partial floor -- e.g. a flight level with only a few ground patches.
    public static final float NO_GROUND = -1024f;

    private int cols;
    private int rows;
    private float cellSize = 1f;
    private float[] heights;
    /// Per-cell wall height above the ground (0 = no wall); used by dungeon-style levels.
    private float[] walls;
    /// Per-cell surface material id from `MaterialRegistry` (the same pluggable, String-keyed
    /// materials the streaming terrain uses); `null`/`MaterialRegistry#GRASS` by default.
    private String[] materials;

    public TerrainGrid() {
    }

    public TerrainGrid(int cols, int rows, float cellSize) {
        this.cols = cols;
        this.rows = rows;
        this.cellSize = cellSize;
        this.heights = new float[Math.max(0, cols * rows)];
        this.walls = new float[Math.max(0, cols * rows)];
        this.materials = new String[Math.max(0, cols * rows)];
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public float getCellSize() {
        return cellSize;
    }

    public TerrainGrid setCellSize(float cellSize) {
        this.cellSize = cellSize;
        return this;
    }

    /// The raw row-major heights array (length `cols * rows`).
    public float[] heights() {
        return heights;
    }

    /// The raw row-major wall-height array (length `cols * rows`); 0 = no wall.
    public float[] walls() {
        if (walls == null || walls.length != cols * rows) {
            walls = new float[Math.max(0, cols * rows)];
        }
        return walls;
    }

    /// The raw row-major surface-material array (length `cols * rows`) of `MaterialRegistry`
    /// ids; a `null` entry means the default `MaterialRegistry#GRASS`.
    public String[] materials() {
        if (materials == null || materials.length != cols * rows) {
            materials = new String[Math.max(0, cols * rows)];
        }
        return materials;
    }

    void setDimensions(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        if (heights == null || heights.length != cols * rows) {
            heights = new float[Math.max(0, cols * rows)];
        }
        if (walls == null || walls.length != cols * rows) {
            walls = new float[Math.max(0, cols * rows)];
        }
        if (materials == null || materials.length != cols * rows) {
            materials = new String[Math.max(0, cols * rows)];
        }
    }

    /// The surface material id at a cell (a `MaterialRegistry` id); never null -- an unpainted
    /// cell reads as `MaterialRegistry#GRASS`.
    public String getMaterial(int col, int row) {
        String[] m = materials();
        if (col < 0 || row < 0 || col >= cols || row >= rows) {
            return MaterialRegistry.GRASS;
        }
        String v = m[row * cols + col];
        return v == null ? MaterialRegistry.GRASS : v;
    }

    public TerrainGrid setMaterial(int col, int row, String material) {
        String[] m = materials();
        if (col >= 0 && row >= 0 && col < cols && row < rows) {
            m[row * cols + col] = material;
        }
        return this;
    }

    public float getHeight(int col, int row) {
        if (heights == null || col < 0 || row < 0 || col >= cols || row >= rows) {
            return 0f;
        }
        return heights[row * cols + col];
    }

    public TerrainGrid setHeight(int col, int row, float height) {
        if (heights != null && col >= 0 && row >= 0 && col < cols && row < rows) {
            heights[row * cols + col] = height;
        }
        return this;
    }

    /// Whether the given cell has a floor (true) or is an open hole / sky gap (false).
    public boolean hasGround(int col, int row) {
        return getHeight(col, row) != NO_GROUND;
    }

    public TerrainGrid setGround(int col, int row, boolean present) {
        return setHeight(col, row, present ? 0f : NO_GROUND);
    }

    /// Wall height stacked on top of the ground in this cell (0 = none).
    public float getWall(int col, int row) {
        float[] w = walls();
        if (col < 0 || row < 0 || col >= cols || row >= rows) {
            return 0f;
        }
        return w[row * cols + col];
    }

    public TerrainGrid setWall(int col, int row, float height) {
        float[] w = walls();
        if (col >= 0 && row >= 0 && col < cols && row < rows) {
            w[row * cols + col] = Math.max(0f, height);
        }
        return this;
    }
}
