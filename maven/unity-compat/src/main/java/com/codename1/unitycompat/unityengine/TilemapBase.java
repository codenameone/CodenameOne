/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.unitycompat.unityengine;

import UnityEngine.Color;
import UnityEngine.Vector3Int;

/// What a `UnityEngine.Tilemaps.Tilemap` is made of: which tile is in
/// which cell, how the tiles are drawn, and which pixels of them are
/// solid. The class a script sees extends this one and adds the members
/// that speak of tiles as objects.
///
/// A cell does not hold a tile. It holds the number of a *variant* -- a
/// tile asset, the sprite it shows, how that sprite is turned, mirrored
/// and moved within the cell, and its colour -- because a painted map is
/// thousands of cells showing a few dozen different things. The cells are
/// one array over the map's bounds, so that drawing can walk exactly the
/// cells the camera sees.
///
/// One draw command is written for each visible cell, not for each cell.
@SuppressWarnings("PMD.MethodNamingConventions") // $-names are what generated and runtime code call
public class TilemapBase extends GridLayout {
    /// How a variant collides, by the numbers a tile asset uses.
    static final int COLLIDES_NOT = 0;
    static final int COLLIDES_AS_SPRITE = 1;
    static final int COLLIDES_AS_CELL = 2;

    int originX;
    int originY;
    int width;
    int height;
    /// A variant's number plus one for each cell, row by row from the
    /// bottom; zero is an empty cell.
    int[] cells = new int[0];
    int tileCount;

    int variantCount;
    // Object alone is this package's, which a tile is not known to be here.
    java.lang.Object[] variantTiles = new java.lang.Object[8]; // NOPMD UnnecessaryFullyQualifiedName
    Sprite[] variantSprites = new Sprite[8];
    int[] variantColliders = new int[8];
    /// Seven to a variant: scale x and y, turn in degrees, offset x and y,
    /// and two spare.
    float[] variantPlaces = new float[8 * 7];
    /// Four to a variant: red, green, blue, alpha.
    float[] variantColors = new float[8 * 4];
    private int[] variantArgb = new int[8];
    private boolean argbStale = true;

    float anchorX = 0.5f;
    float anchorY = 0.5f;
    private final Color color = new Color();
    /// How many cells beyond its own a sprite may reach, for culling.
    private int reach = 1;
    private GridLayout grid;

    protected TilemapBase() {
        color.r = 1f;
        color.g = 1f;
        color.b = 1f;
        color.a = 1f;
    }

    @Override
    GridLayout layout() {
        if (grid == null || grid.destroyed) {
            java.lang.Object g = gameObject == null ? null : gameObject.GetComponentInParent(Grid.class);
            grid = g instanceof GridLayout ? (GridLayout) g : null;
        }
        return grid == null ? this : grid;
    }

    // ------------------------------------------------------- generated code

    /// What a scene file sets: where in a cell a tile's pivot goes, as a
    /// fraction of the cell, and the colour every tile is multiplied by.
    public void $setup(float anchorAtX, float anchorAtY, float r, float g, float b, float a) {
        anchorX = anchorAtX;
        anchorY = anchorAtY;
        color.r = r;
        color.g = g;
        color.b = b;
        color.a = a;
        argbStale = true;
    }

    /// Adds a variant and answers its number.
    public int $variant(java.lang.Object tile, Sprite sprite, int collides, float scaleX, float scaleY, float turn,
            float offsetX, float offsetY, float r, float g, float b, float a) {
        if (variantCount == variantSprites.length) {
            int n = variantCount * 2;
            java.lang.Object[] tiles = new java.lang.Object[n]; // NOPMD UnnecessaryFullyQualifiedName
            Sprite[] sprites = new Sprite[n];
            int[] colliders = new int[n];
            float[] places = new float[n * 7];
            float[] colors = new float[n * 4];
            System.arraycopy(variantTiles, 0, tiles, 0, variantCount);
            System.arraycopy(variantSprites, 0, sprites, 0, variantCount);
            System.arraycopy(variantColliders, 0, colliders, 0, variantCount);
            System.arraycopy(variantPlaces, 0, places, 0, variantCount * 7);
            System.arraycopy(variantColors, 0, colors, 0, variantCount * 4);
            variantTiles = tiles;
            variantSprites = sprites;
            variantColliders = colliders;
            variantPlaces = places;
            variantColors = colors;
            variantArgb = new int[n];
        }
        int v = variantCount++;
        variantTiles[v] = tile;
        variantSprites[v] = sprite;
        variantColliders[v] = collides;
        variantPlaces[v * 7] = scaleX;
        variantPlaces[v * 7 + 1] = scaleY;
        variantPlaces[v * 7 + 2] = turn;
        variantPlaces[v * 7 + 3] = offsetX;
        variantPlaces[v * 7 + 4] = offsetY;
        variantColors[v * 4] = r;
        variantColors[v * 4 + 1] = g;
        variantColors[v * 4 + 2] = b;
        variantColors[v * 4 + 3] = a;
        argbStale = true;
        if (sprite != null) {
            GridLayout g2 = layout();
            float least = g2.cellWidth < g2.cellHeight ? g2.cellWidth : g2.cellHeight;
            float most = (sprite.width > sprite.height ? sprite.width : sprite.height) / sprite.pixelsPerUnit;
            float sx = scaleX < 0f ? -scaleX : scaleX;
            float sy = scaleY < 0f ? -scaleY : scaleY;
            most = most * (sx > sy ? sx : sy);
            int across = least > 0f ? (int) (most / least) + 1 : 1;
            if (across > reach) {
                reach = across > 64 ? 64 : across;
            }
        }
        return v;
    }

    /// The cells the map covers. Everything in them is cleared.
    public void $bounds(int fromX, int fromY, int cellsAcross, int cellsUp) {
        originX = fromX;
        originY = fromY;
        width = cellsAcross > 0 ? cellsAcross : 0;
        height = cellsUp > 0 ? cellsUp : 0;
        cells = new int[width * height];
        tileCount = 0;
    }

    /// Fills runs of cells: four whole numbers to a run, separated by
    /// spaces -- the column and the row of its first cell, how many cells
    /// it is along the row, and the variant in all of them.
    public void $fill(String runs) {
        int at = 0;
        int n = runs.length();
        int[] four = new int[4];
        int have = 0;
        while (at < n) {
            while (at < n && runs.charAt(at) == ' ') {
                at++;
            }
            if (at >= n) {
                break;
            }
            boolean negative = runs.charAt(at) == '-';
            if (negative) {
                at++;
            }
            int value = 0;
            while (at < n && runs.charAt(at) != ' ') {
                value = value * 10 + (runs.charAt(at) - '0');
                at++;
            }
            four[have++] = negative ? -value : value;
            if (have == 4) {
                have = 0;
                for (int i = 0; i < four[2]; i++) {
                    put(four[0] + i, four[1], four[3] + 1);
                }
            }
        }
    }

    // --------------------------------------------------------------- cells

    /// What a cell holds: a variant's number plus one, or zero.
    final int cell(int x, int y) {
        int cx = x - originX;
        int cy = y - originY;
        if (cx < 0 || cy < 0 || cx >= width || cy >= height) {
            return 0;
        }
        return cells[cy * width + cx];
    }

    private void grow(int x, int y) {
        int fromX = width == 0 ? x : x < originX ? x : originX;
        int fromY = height == 0 ? y : y < originY ? y : originY;
        int toX = width == 0 ? x + 1 : x >= originX + width ? x + 1 : originX + width;
        int toY = height == 0 ? y + 1 : y >= originY + height ? y + 1 : originY + height;
        // Room to spare on the side that grew, so that painting a row cell
        // by cell does not copy the map once a cell.
        if (width > 0 && fromX < originX) {
            fromX -= 8;
        }
        if (height > 0 && fromY < originY) {
            fromY -= 8;
        }
        if (width > 0 && toX > originX + width) {
            toX += 8;
        }
        if (height > 0 && toY > originY + height) {
            toY += 8;
        }
        int w = toX - fromX;
        int h = toY - fromY;
        int[] grown = new int[w * h];
        for (int row = 0; row < height; row++) {
            System.arraycopy(cells, row * width, grown, (row + originY - fromY) * w + (originX - fromX), width);
        }
        cells = grown;
        originX = fromX;
        originY = fromY;
        width = w;
        height = h;
    }

    private void put(int x, int y, int value) {
        int cx = x - originX;
        int cy = y - originY;
        if (cx < 0 || cy < 0 || cx >= width || cy >= height) {
            if (value == 0) {
                return;
            }
            grow(x, y);
            cx = x - originX;
            cy = y - originY;
        }
        int at = cy * width + cx;
        int was = cells[at];
        if (was == value) {
            return;
        }
        if (was == 0) {
            tileCount++;
        } else if (value == 0) {
            tileCount--;
        }
        cells[at] = value;
    }

    /// Puts a variant in a cell, or with -1 empties it, and has whatever
    /// collides as this map made again.
    public void $set(int x, int y, int variant) {
        int before = cell(x, y);
        put(x, y, variant + 1);
        if (before != variant + 1 && gameObject != null) {
            PhysicsWorld.changed(gameObject);
            GameObject above = gameObject.transform.parent == null ? null : gameObject.transform.parent.gameObject;
            if (above != null) {
                PhysicsWorld.changed(above);
            }
        }
    }

    /// The variant showing a tile asset as its own file describes it, or
    /// -1: what a script that sets a tile by its asset gets.
    public int $variantOf(java.lang.Object tile) {
        for (int i = 0; i < variantCount; i++) {
            if (variantTiles[i] == tile) { // NOPMD CompareObjectsWithEquals
                return i;
            }
        }
        return -1;
    }

    public java.lang.Object $tileAt(int x, int y) {
        int v = cell(x, y);
        return v == 0 ? null : variantTiles[v - 1];
    }

    public Sprite $spriteAt(int x, int y) {
        int v = cell(x, y);
        return v == 0 ? null : variantSprites[v - 1];
    }

    public boolean HasTile(Vector3Int position) {
        return cell(position.x, position.y) != 0;
    }

    public Sprite GetSprite(Vector3Int position) {
        return $spriteAt(position.x, position.y);
    }

    public void ClearAllTiles() {
        if (tileCount == 0) {
            return;
        }
        for (int i = 0; i < cells.length; i++) {
            cells[i] = 0;
        }
        tileCount = 0;
        if (gameObject != null) {
            PhysicsWorld.changed(gameObject);
        }
    }

    /// Nothing to do: a cell is drawn from what it holds every frame.
    public void RefreshAllTiles() {
    }

    /// Nothing to do: the bounds only say which cells have storage.
    public void CompressBounds() {
    }

    public Vector3Int get_origin(Vector3Int ret) {
        ret.x = originX;
        ret.y = originY;
        ret.z = 0;
        return ret;
    }

    public Vector3Int get_size(Vector3Int ret) {
        ret.x = width;
        ret.y = height;
        ret.z = 1;
        return ret;
    }

    public Grid get_layoutGrid() {
        GridLayout g = layout();
        return g instanceof Grid ? (Grid) g : null;
    }

    public Color get_color(Color ret) {
        ret.$assign(color);
        return ret;
    }

    public void set_color(Color value) {
        color.$assign(value);
        argbStale = true;
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        TilemapBase m = (TilemapBase) source;
        originX = m.originX;
        originY = m.originY;
        width = m.width;
        height = m.height;
        cells = new int[m.cells.length];
        System.arraycopy(m.cells, 0, cells, 0, cells.length);
        tileCount = m.tileCount;
        variantCount = m.variantCount;
        variantTiles = new java.lang.Object[m.variantTiles.length];
        System.arraycopy(m.variantTiles, 0, variantTiles, 0, variantTiles.length);
        variantSprites = new Sprite[m.variantSprites.length];
        System.arraycopy(m.variantSprites, 0, variantSprites, 0, variantSprites.length);
        variantColliders = new int[m.variantColliders.length];
        System.arraycopy(m.variantColliders, 0, variantColliders, 0, variantColliders.length);
        variantPlaces = new float[m.variantPlaces.length];
        System.arraycopy(m.variantPlaces, 0, variantPlaces, 0, variantPlaces.length);
        variantColors = new float[m.variantColors.length];
        System.arraycopy(m.variantColors, 0, variantColors, 0, variantColors.length);
        variantArgb = new int[m.variantArgb.length];
        argbStale = true;
        anchorX = m.anchorX;
        anchorY = m.anchorY;
        color.$assign(m.color);
        reach = m.reach;
        grid = null;
    }

    // -------------------------------------------------------------- drawing

    private void tint() {
        for (int v = 0; v < variantCount; v++) {
            variantArgb[v] = Camera.argb(variantColors[v * 4] * color.r, variantColors[v * 4 + 1] * color.g,
                    variantColors[v * 4 + 2] * color.b, variantColors[v * 4 + 3] * color.a);
        }
        argbStale = false;
    }

    /// Draws the cells the camera sees, at an order within a sorting layer.
    public void $draw(DrawView view, int sortingOrder, int sortingLayerID) {
        if (tileCount == 0 || width == 0 || height == 0) {
            return;
        }
        if (argbStale) {
            tint();
        }
        Transform t = gameObject.transform;
        t.update();
        GridLayout g = layout();
        float pitchX = g.cellWidth + g.gapX;
        float pitchY = g.cellHeight + g.gapY;
        int fromX = 0;
        int fromY = 0;
        int toX = width;
        int toY = height;
        // With neither the map nor the camera turned, the cells on screen
        // are a rectangle of the array and nothing else is looked at.
        if (t.wrot == 0f && !view.$turned() && t.wsx != 0f && t.wsy != 0f && pitchX > 0f && pitchY > 0f) {
            float halfW = view.$halfWidth();
            float halfH = view.$halfHeight();
            float left = (view.eyeX - halfW - t.wx) / t.wsx;
            float right = (view.eyeX + halfW - t.wx) / t.wsx;
            float bottom = (view.eyeY - halfH - t.wy) / t.wsy;
            float top = (view.eyeY + halfH - t.wy) / t.wsy;
            if (left > right) {
                float swap = left;
                left = right;
                right = swap;
            }
            if (bottom > top) {
                float swap = bottom;
                bottom = top;
                top = swap;
            }
            int a = floor(left / pitchX) - reach - originX;
            int b = floor(right / pitchX) + reach + 1 - originX;
            int c = floor(bottom / pitchY) - reach - originY;
            int d = floor(top / pitchY) + reach + 1 - originY;
            fromX = a > 0 ? a : 0;
            toX = b < width ? b : width;
            fromY = c > 0 ? c : 0;
            toY = d < height ? d : height;
        }
        float ax = anchorX * g.cellWidth;
        float ay = anchorY * g.cellHeight;
        for (int cy = fromY; cy < toY; cy++) {
            int row = cy * width;
            float baseY = (cy + originY) * pitchY;
            for (int cx = fromX; cx < toX; cx++) {
                int v = cells[row + cx];
                if (v == 0) {
                    continue;
                }
                v--;
                Sprite sprite = variantSprites[v];
                if (sprite == null) {
                    continue;
                }
                int p = v * 7;
                float baseX = (cx + originX) * pitchX;
                float lx = baseX + ax + variantPlaces[p + 3];
                float ly = baseY + ay + variantPlaces[p + 4];
                float px = lx * t.wsx;
                float py = ly * t.wsy;
                float xc = px * t.wcos;
                float ys = py * t.wsin;
                float xs = px * t.wsin;
                float yc = py * t.wcos;
                float sx = t.wsx * variantPlaces[p];
                float sy = t.wsy * variantPlaces[p + 1];
                view.$sprite(sprite, t.wx + (xc - ys), t.wy + (xs + yc), t.wz, sx, sy, t.wrot + variantPlaces[p + 2],
                        variantArgb[v], false, false, sortingOrder, sortingLayerID);
            }
        }
    }
}
