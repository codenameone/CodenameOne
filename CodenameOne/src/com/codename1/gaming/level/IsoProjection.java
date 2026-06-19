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

/// Maps between board cells (row, column) and screen pixels for the 2:1 isometric
/// "diamond" layout used by board / strategy games.
///
/// This is the projection the `BoardGameSample` hand-rolled, promoted to a reusable
/// piece so a board-mode `GameLevel` and its editor share one source of truth. A tile
/// is `#getTileWidth()` x `#getTileHeight()` pixels (height is conventionally half the
/// width), and `(#getOriginX(), #getOriginY())` is the screen position of cell (0,0)'s
/// center. `#tileCenterX(int, int)` / `#tileCenterY(int, int)` go cell -> screen and
/// `#pick(int, int)` inverts screen -> cell.
public class IsoProjection {
    private float originX;
    private float originY;
    private float tileWidth = 64f;
    private float tileHeight = 32f;

    public IsoProjection() {
    }

    public IsoProjection(float originX, float originY, float tileWidth, float tileHeight) {
        this.originX = originX;
        this.originY = originY;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
    }

    public float getOriginX() {
        return originX;
    }

    public float getOriginY() {
        return originY;
    }

    public IsoProjection setOrigin(float originX, float originY) {
        this.originX = originX;
        this.originY = originY;
        return this;
    }

    public float getTileWidth() {
        return tileWidth;
    }

    public float getTileHeight() {
        return tileHeight;
    }

    public IsoProjection setTileSize(float tileWidth, float tileHeight) {
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        return this;
    }

    /// Sizes the tiles and centers the board to fit an `n` x `n` grid inside a view of
    /// the given pixel size, matching the `BoardGameSample` fit (width-bound or
    /// height-bound, whichever is tighter, then centered).
    public IsoProjection fit(int n, int viewWidth, int viewHeight) {
        tileWidth = Math.min(viewWidth * 0.95f / n, viewHeight * 0.78f / (n / 2f));
        tileHeight = tileWidth / 2f;
        originX = viewWidth / 2f;
        originY = viewHeight / 2f - (n - 1) * tileHeight / 2f;
        return this;
    }

    /// The screen x of the center of cell (row, col).
    public float tileCenterX(int row, int col) {
        return originX + (col - row) * (tileWidth / 2f);
    }

    /// The screen y of the center of cell (row, col).
    public float tileCenterY(int row, int col) {
        return originY + (col + row) * (tileHeight / 2f);
    }

    /// Inverts a screen pixel to the nearest cell, returned as `{row, col}`. The result
    /// is not clamped to any board size -- callers validate the range.
    public int[] pick(int px, int py) {
        float a = (px - originX) / (tileWidth / 2f);   // = col - row
        float b = (py - originY) / (tileHeight / 2f);  // = col + row
        int col = Math.round((a + b) / 2f);
        int row = Math.round((b - a) / 2f);
        return new int[]{row, col};
    }
}
