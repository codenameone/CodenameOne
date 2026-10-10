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
package com.codename1.desktopcompat.java.awt.geom;

import java.util.NoSuchElementException;

/// The one path iterator every shape of the package hands out: a list of
/// segment types with their coordinates, mapped through an optional
/// transform as they are read.
///
/// A shape either fills one through `move`, `line`, `quad`, `cubic` and
/// `close` before returning it, or wraps the arrays it already keeps.
final class SegmentIterator implements PathIterator {

    private byte[] types;
    private double[] coords;
    private int count;
    private int coordCount;
    private final int rule;
    private final AffineTransform at;
    private int index;
    private int coordIndex;

    /// An empty iterator with room for the given number of segments.
    SegmentIterator(int rule, AffineTransform at, int segments) {
        this.rule = rule;
        this.at = at;
        this.types = new byte[segments];
        this.coords = new double[segments * 6];
    }

    /// An iterator over segments a path already holds.
    SegmentIterator(int rule, AffineTransform at, byte[] types, int count, double[] coords) {
        this.rule = rule;
        this.at = at;
        this.types = types;
        this.count = count;
        this.coords = coords;
    }

    /// The number of coordinates a segment of the given type carries.
    static int size(int type) {
        switch (type) {
            case SEG_MOVETO:
            case SEG_LINETO:
                return 2;
            case SEG_QUADTO:
                return 4;
            case SEG_CUBICTO:
                return 6;
            default:
                return 0;
        }
    }

    private void add(int type) {
        if (count == types.length) {
            byte[] t = new byte[count * 2 + 4];
            System.arraycopy(types, 0, t, 0, count);
            types = t;
        }
        if (coordCount + 6 > coords.length) {
            double[] c = new double[coords.length * 2 + 12];
            System.arraycopy(coords, 0, c, 0, coordCount);
            coords = c;
        }
        types[count++] = (byte) type;
    }

    void move(double x, double y) {
        add(SEG_MOVETO);
        coords[coordCount++] = x;
        coords[coordCount++] = y;
    }

    void line(double x, double y) {
        add(SEG_LINETO);
        coords[coordCount++] = x;
        coords[coordCount++] = y;
    }

    void quad(double cx, double cy, double x, double y) {
        add(SEG_QUADTO);
        coords[coordCount++] = cx;
        coords[coordCount++] = cy;
        coords[coordCount++] = x;
        coords[coordCount++] = y;
    }

    void cubic(double cx1, double cy1, double cx2, double cy2, double x, double y) {
        add(SEG_CUBICTO);
        coords[coordCount++] = cx1;
        coords[coordCount++] = cy1;
        coords[coordCount++] = cx2;
        coords[coordCount++] = cy2;
        coords[coordCount++] = x;
        coords[coordCount++] = y;
    }

    void close() {
        add(SEG_CLOSE);
    }

    @Override
    public int getWindingRule() {
        return rule;
    }

    @Override
    public boolean isDone() {
        return index >= count;
    }

    @Override
    public void next() {
        if (index < count) {
            coordIndex += size(types[index]);
            index++;
        }
    }

    @Override
    public int currentSegment(float[] out) {
        if (index >= count) {
            throw new NoSuchElementException("path iterator out of bounds");
        }
        int type = types[index];
        int n = size(type);
        if (n > 0) {
            if (at != null) {
                at.transform(coords, coordIndex, out, 0, n / 2);
            } else {
                for (int i = 0; i < n; i++) {
                    out[i] = (float) coords[coordIndex + i];
                }
            }
        }
        return type;
    }

    @Override
    public int currentSegment(double[] out) {
        if (index >= count) {
            throw new NoSuchElementException("path iterator out of bounds");
        }
        int type = types[index];
        int n = size(type);
        if (n > 0) {
            if (at != null) {
                at.transform(coords, coordIndex, out, 0, n / 2);
            } else {
                System.arraycopy(coords, coordIndex, out, 0, n);
            }
        }
        return type;
    }
}
