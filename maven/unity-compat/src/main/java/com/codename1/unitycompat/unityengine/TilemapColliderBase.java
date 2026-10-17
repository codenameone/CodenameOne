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

/// What a `UnityEngine.Tilemaps.TilemapCollider2D` does: makes collision
/// shapes from the tiles of the [TilemapBase] beside it.
///
/// A tile collides as its sprite -- the pixels of it that are not
/// transparent, which the scene compiler reads from the image -- or as its
/// whole cell, or not at all, as its asset says. The solid pixels of every
/// tile are drawn into one picture and the shapes are made from that:
/// rectangles when the collider stands alone, and for a
/// [CompositeCollider2D] the outlines or the rectangles, as it asks.
///
/// Unity makes a tile's shape from the outline of its sprite, with a
/// tolerance; here it is the pixels themselves. A tile whose sprite is
/// turned other than by quarter turns collides as the box around it.
@SuppressWarnings("PMD.MethodNamingConventions") // $-names are what generated and runtime code call
public class TilemapColliderBase extends Collider2D implements TileOutline.Sink {
    private static final TileOutline picture = new TileOutline();
    private PhysicsWorld.Shapes out;
    private boolean outlines;
    private float unitX;
    private float unitY;
    private float fromX;
    private float fromY;
    private float[] points = new float[64];
    // Kept between calls: a tilemap of plain squares is thousands of boxes.
    private final float[] corners = new float[8]; // NOPMD SingularField

    /// Has the shapes made again, after the tiles changed.
    public void $changed() {
        PhysicsWorld.changed(gameObject);
    }

    private TilemapBase map() {
        java.lang.Object m = gameObject.GetComponent(TilemapBase.class);
        return m instanceof TilemapBase ? (TilemapBase) m : null;
    }

    @Override
    void shape(PhysicsWorld.Shapes shapes) {
        build(shapes, false);
    }

    @Override
    void compose(PhysicsWorld.Shapes shapes, boolean asOutlines) {
        build(shapes, asOutlines);
    }

    private static int round(float v) {
        return (int) (v < 0f ? v - 0.5f : v + 0.5f);
    }

    private void build(PhysicsWorld.Shapes shapes, boolean asOutlines) {
        TilemapBase m = map();
        if (m == null || m.tileCount == 0) {
            return;
        }
        GridLayout g = m.layout();
        float pitchX = g.cellWidth + g.gapX;
        float pitchY = g.cellHeight + g.gapY;
        if (!(pitchX > 0f) || !(pitchY > 0f)) { // NOPMD LogicInversion
            return;
        }
        // How many pixels of the picture make a cell: one when every tile
        // that collides fills its cell, and otherwise the pixels of the
        // tiles' own sprites.
        int fine = 1;
        for (int v = 0; v < m.variantCount; v++) {
            Sprite s = m.variantSprites[v];
            if (m.variantColliders[v] != TilemapBase.COLLIDES_AS_SPRITE || s == null) {
                continue;
            }
            int p = v * 7;
            float w = s.width / s.pixelsPerUnit;
            float h = s.height / s.pixelsPerUnit;
            boolean whole = s.solidAllOver() && m.variantPlaces[p + 2] == 0f && m.variantPlaces[p + 3] == 0f
                    && m.variantPlaces[p + 4] == 0f && (m.variantPlaces[p] == 1f || m.variantPlaces[p] == -1f)
                    && (m.variantPlaces[p + 1] == 1f || m.variantPlaces[p + 1] == -1f)
                    && s.pivotX == m.anchorX && s.pivotY == m.anchorY && near(w, pitchX) && near(h, pitchY);
            if (!whole) {
                int pixels = round(pitchX * s.pixelsPerUnit);
                if (pixels > fine) {
                    fine = pixels > 64 ? 64 : pixels;
                }
            }
        }
        picture.clear(m.width * fine, m.height * fine);
        float pixelX = pitchX / fine;
        float pixelY = pitchY / fine;
        for (int cy = 0; cy < m.height; cy++) {
            for (int cx = 0; cx < m.width; cx++) {
                int v = m.cells[cy * m.width + cx];
                if (v == 0) {
                    continue;
                }
                v--;
                int kind = m.variantColliders[v];
                Sprite s = m.variantSprites[v];
                if (kind == TilemapBase.COLLIDES_NOT || (kind == TilemapBase.COLLIDES_AS_SPRITE && s == null)) {
                    continue;
                }
                if (kind == TilemapBase.COLLIDES_AS_CELL || fine == 1) {
                    picture.fill(cx * fine, cy * fine, (cx + 1) * fine, (cy + 1) * fine);
                    continue;
                }
                stamp(m, v, s, cx, cy, pixelX, pixelY, g);
            }
        }
        out = shapes;
        outlines = asOutlines;
        unitX = pixelX;
        unitY = pixelY;
        fromX = m.originX * pitchX;
        fromY = m.originY * pitchY;
        if (asOutlines) {
            picture.outlines(this);
        } else {
            picture.boxes(this);
        }
        out = null;
    }

    private static boolean near(float a, float b) {
        float d = a - b;
        return d < 0.0005f && d > -0.0005f;
    }

    /// Draws the solid pixels of one tile's sprite into the picture.
    private void stamp(TilemapBase m, int v, Sprite s, int cx, int cy, float pixelX, float pixelY,
            GridLayout g) {
        int p = v * 7;
        float scaleX = m.variantPlaces[p];
        float scaleY = m.variantPlaces[p + 1];
        float turn = m.variantPlaces[p + 2] % 360f;
        if (turn < 0f) {
            turn += 360f;
        }
        int quarter = turn == 0f ? 0 : turn == 90f ? 1 : turn == 180f ? 2 : turn == 270f ? 3 : -1;
        // Where the pivot is, in pixels of the picture.
        float pivotAtX = (cx * g.cellWidth + m.anchorX * g.cellWidth + m.variantPlaces[p + 3]) / pixelX;
        float pivotAtY = (cy * g.cellHeight + m.anchorY * g.cellHeight + m.variantPlaces[p + 4]) / pixelY;
        // The sprite's box about its pivot, in units, before it is turned.
        float left = -s.pivotX * s.width / s.pixelsPerUnit * scaleX;
        float right = (1f - s.pivotX) * s.width / s.pixelsPerUnit * scaleX;
        float bottom = -s.pivotY * s.height / s.pixelsPerUnit * scaleY;
        float top = (1f - s.pivotY) * s.height / s.pixelsPerUnit * scaleY;
        float reachX = (left < 0f ? -left : left) > (right < 0f ? -right : right) ? (left < 0f ? -left : left)
                : (right < 0f ? -right : right);
        float reachY = (bottom < 0f ? -bottom : bottom) > (top < 0f ? -top : top) ? (bottom < 0f ? -bottom : bottom)
                : (top < 0f ? -top : top);
        float reach = reachX > reachY ? reachX : reachY;
        int x0 = GridLayout.floor(pivotAtX - reach / pixelX) - 1;
        int x1 = GridLayout.floor(pivotAtX + reach / pixelX) + 2;
        int y0 = GridLayout.floor(pivotAtY - reach / pixelY) - 1;
        int y1 = GridLayout.floor(pivotAtY + reach / pixelY) + 2;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                // The middle of the picture's pixel, about the pivot, in
                // units, turned back into the sprite's own axes.
                float dx = (x + 0.5f - pivotAtX) * pixelX;
                float dy = (y + 0.5f - pivotAtY) * pixelY;
                float ux;
                float uy;
                switch (quarter) {
                    case 1:
                        ux = dy;
                        uy = -dx;
                        break;
                    case 2:
                        ux = -dx;
                        uy = -dy;
                        break;
                    case 3:
                        ux = -dy;
                        uy = dx;
                        break;
                    default:
                        ux = dx;
                        uy = dy;
                        break;
                }
                if (scaleX == 0f || scaleY == 0f) {
                    continue;
                }
                float sx = ux / scaleX * s.pixelsPerUnit + s.pivotX * s.width;
                float sy = uy / scaleY * s.pixelsPerUnit + s.pivotY * s.height;
                int column = GridLayout.floor(sx);
                int row = s.height - 1 - GridLayout.floor(sy);
                if (quarter < 0 ? (column >= 0 && row >= 0 && column < s.width && row < s.height)
                        : s.solidAt(column, row)) {
                    picture.set(x, y);
                }
            }
        }
    }

    @Override
    public void outline(int[] loop, int count) {
        if (points.length < count * 2) {
            points = new float[count * 2];
        }
        for (int i = 0; i < count; i++) {
            float x = loop[i * 2] * unitX;
            float y = loop[i * 2 + 1] * unitY;
            points[i * 2] = fromX + x;
            points[i * 2 + 1] = fromY + y;
        }
        out.loop(points, count, $offsetX, $offsetY);
    }

    @Override
    public void box(int left, int bottom, int right, int top) {
        float l = left * unitX;
        float r = right * unitX;
        float b = bottom * unitY;
        float t = top * unitY;
        corners[0] = fromX + l;
        corners[1] = fromY + b;
        corners[2] = fromX + r;
        corners[3] = fromY + b;
        corners[4] = fromX + r;
        corners[5] = fromY + t;
        corners[6] = fromX + l;
        corners[7] = fromY + t;
        if (outlines) {
            out.loop(corners, 4, $offsetX, $offsetY);
        } else {
            out.convex(corners, $offsetX, $offsetY);
        }
    }
}
