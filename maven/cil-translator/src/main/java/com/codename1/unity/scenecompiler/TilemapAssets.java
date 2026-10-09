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
package com.codename1.unity.scenecompiler;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Reads what a `Tilemap` component of a scene holds, and what of a
/// sprite's image is solid.
///
/// A tilemap is a list of cells. Each names, by its place in four tables
/// of the same component, a tile asset, a sprite, a matrix and a colour;
/// the combinations that occur are few -- a terrain of thousands of cells
/// is a few dozen of them -- so the cells are written to the generated
/// code as runs of a variant number, and each variant once.
final class TilemapAssets {
    /// One combination of tile, sprite, matrix and colour that occurs.
    static final class Variant {
        int tile;
        int sprite;
        int matrix;
        int color;
    }

    /// The cells of one tilemap.
    static final class Cells {
        final List<Variant> variants = new ArrayList<Variant>();
        /// Runs of cells, each `column row length variant`, in pieces
        /// short enough to be string constants.
        final List<String> runs = new ArrayList<String>();
        int fromX;
        int fromY;
        int across;
        int up;
        int count;
        /// Cells that are not on the plane z = 0, which are left out.
        int offPlane;
    }

    private TilemapAssets() {
    }

    private static int number(Object o) {
        return (int) Long.parseLong(SceneCompiler.integer(o, "0"));
    }

    /// Reads `m_Tiles`.
    static Cells cells(Object tiles) {
        Cells out = new Cells();
        Map<String, Integer> known = new LinkedHashMap<String, Integer>();
        List<int[]> placed = new ArrayList<int[]>();
        for (Object o : SceneCompiler.list(tiles)) {
            Map<String, Object> cell = SceneCompiler.map(o);
            Map<String, Object> at = SceneCompiler.map(cell.get("first"));
            Map<String, Object> what = SceneCompiler.map(cell.get("second"));
            if (number(at.get("z")) != 0) {
                out.offPlane++;
                continue;
            }
            Variant v = new Variant();
            v.tile = number(what.get("m_TileIndex"));
            v.sprite = number(what.get("m_TileSpriteIndex"));
            v.matrix = number(what.get("m_TileMatrixIndex"));
            v.color = number(what.get("m_TileColorIndex"));
            String key = v.tile + "/" + v.sprite + "/" + v.matrix + "/" + v.color;
            Integer index = known.get(key);
            if (index == null) {
                index = Integer.valueOf(out.variants.size());
                known.put(key, index);
                out.variants.add(v);
            }
            placed.add(new int[] {number(at.get("x")), number(at.get("y")), index.intValue()});
        }
        out.count = placed.size();
        if (placed.isEmpty()) {
            return out;
        }
        Collections.sort(placed, new Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                return a[1] != b[1] ? (a[1] < b[1] ? -1 : 1) : a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0;
            }
        });
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (int[] c : placed) {
            minX = Math.min(minX, c[0]);
            maxX = Math.max(maxX, c[0]);
        }
        out.fromX = minX;
        out.fromY = placed.get(0)[1];
        out.across = maxX - minX + 1;
        out.up = placed.get(placed.size() - 1)[1] - out.fromY + 1;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < placed.size()) {
            int[] first = placed.get(i);
            int length = 1;
            while (i + length < placed.size()) {
                int[] next = placed.get(i + length);
                if (next[1] != first[1] || next[0] != first[0] + length || next[2] != first[2]) {
                    break;
                }
                length++;
            }
            if (sb.length() > 30000) {
                out.runs.add(sb.toString());
                sb.setLength(0);
            }
            sb.append(sb.length() == 0 ? "" : " ").append(first[0]).append(' ').append(first[1]).append(' ')
                    .append(length).append(' ').append(first[2]);
            i += length;
        }
        out.runs.add(sb.toString());
        return out;
    }

    /// What a tile's matrix does to its sprite, in the plane: the scale
    /// across and up, the turn in degrees anticlockwise, and the offset.
    static double[] place(Map<String, Object> m) {
        if (m.isEmpty()) {
            return new double[] {1, 1, 0, 0, 0};
        }
        double e00 = Double.parseDouble(SceneCompiler.number(m.get("e00"), "1"));
        double e01 = Double.parseDouble(SceneCompiler.number(m.get("e01"), "0"));
        double e10 = Double.parseDouble(SceneCompiler.number(m.get("e10"), "0"));
        double e11 = Double.parseDouble(SceneCompiler.number(m.get("e11"), "1"));
        double scaleX = Math.sqrt(e00 * e00 + e10 * e10);
        double turn = scaleX == 0 ? 0 : Math.toDegrees(Math.atan2(e10, e00));
        double determinant = e00 * e11 - e01 * e10;
        double scaleY = scaleX == 0 ? Math.sqrt(e01 * e01 + e11 * e11) : determinant / scaleX;
        return new double[] {
            round(scaleX), round(scaleY), round(turn),
            round(Double.parseDouble(SceneCompiler.number(m.get("e03"), "0"))),
            round(Double.parseDouble(SceneCompiler.number(m.get("e13"), "0"))),
        };
    }

    private static double round(double v) {
        double r = Math.round(v * 1e5) / 1e5;
        return r == 0 ? 0 : r;
    }

    /// The solid pixels of a sprite, as the runtime keeps them: for each
    /// row from the top, the leftmost pixel in the lowest bit of as many
    /// ints as the width needs. Null when every pixel is solid.
    ///
    /// `shape` is the physics shape the sprite's importer records, when a
    /// custom one was drawn: polygons of points in pixels from the middle
    /// of the sprite, y up. Without one, a pixel is solid when it is not
    /// transparent -- Unity traces an outline around those pixels and
    /// loosens it a little; this keeps the pixels themselves.
    static int[] solid(BufferedImage image, int x, int top, int width, int height, List<double[]> shape) {
        int stride = (width + 31) >> 5;
        int[] rows = new int[stride * height];
        boolean all = true;
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                boolean in;
                if (shape != null && !shape.isEmpty()) {
                    double px = column + 0.5 - width / 2.0;
                    double py = height / 2.0 - (row + 0.5);
                    in = false;
                    for (double[] polygon : shape) {
                        in ^= inside(polygon, px, py);
                    }
                } else {
                    int ix = x + column;
                    int iy = top + row;
                    in = ix >= 0 && iy >= 0 && ix < image.getWidth() && iy < image.getHeight()
                            && (image.getRGB(ix, iy) >>> 24) != 0;
                }
                if (in) {
                    rows[row * stride + (column >> 5)] |= 1 << (column & 31);
                } else {
                    all = false;
                }
            }
        }
        return all ? null : rows;
    }

    private static boolean inside(double[] polygon, double px, double py) {
        boolean in = false;
        int n = polygon.length / 2;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon[i * 2];
            double yi = polygon[i * 2 + 1];
            double xj = polygon[j * 2];
            double yj = polygon[j * 2 + 1];
            if ((yi > py) != (yj > py) && px < (xj - xi) * (py - yi) / (yj - yi) + xi) {
                in = !in;
            }
        }
        return in;
    }

    /// The polygons of an importer's `physicsShape`, each x then y.
    static List<double[]> shape(Object physicsShape) {
        List<double[]> out = new ArrayList<double[]>();
        for (Object path : SceneCompiler.list(physicsShape)) {
            List<Object> points = SceneCompiler.list(path);
            if (points.size() < 3) {
                continue;
            }
            double[] polygon = new double[points.size() * 2];
            for (int i = 0; i < points.size(); i++) {
                Map<String, Object> point = SceneCompiler.map(points.get(i));
                polygon[i * 2] = Double.parseDouble(SceneCompiler.number(point.get("x"), "0"));
                polygon[i * 2 + 1] = Double.parseDouble(SceneCompiler.number(point.get("y"), "0"));
            }
            out.add(polygon);
        }
        return out;
    }
}
