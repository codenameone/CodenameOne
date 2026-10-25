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

/// Reads another path iterator and replaces every curve with straight lines
/// that stay within a given distance of it.
///
/// A curve is halved until its control points are within the flatness of
/// the chord joining its ends, or until it has been halved `limit` times.
/// Every point returned lies on the original curve. The number of lines a
/// curve becomes is not promised to match any other implementation.
public class FlatteningPathIterator implements PathIterator {

    private final PathIterator src;
    private final double flatness;
    private final double flatnessSq;
    private final int limit;
    private final double[] segment = new double[6];

    /// End points of the lines the current curve became, not yet returned.
    private double[] pending = new double[32];
    private int pendingCount;
    private int pendingIndex;

    private double curX;
    private double curY;
    private double moveX;
    private double moveY;
    private int type;
    private boolean done;

    public FlatteningPathIterator(PathIterator src, double flatness) {
        this(src, flatness, 10);
    }

    public FlatteningPathIterator(PathIterator src, double flatness, int limit) {
        if (flatness < 0.0) {
            throw new IllegalArgumentException("flatness must be >= 0");
        }
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be >= 0");
        }
        this.src = src;
        this.flatness = flatness;
        this.flatnessSq = flatness * flatness;
        this.limit = limit;
        advance();
    }

    public double getFlatness() {
        return flatness;
    }

    public int getRecursionLimit() {
        return limit;
    }

    @Override
    public int getWindingRule() {
        return src.getWindingRule();
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public void next() {
        advance();
    }

    private void advance() {
        if (pendingIndex < pendingCount) {
            curX = pending[pendingIndex++];
            curY = pending[pendingIndex++];
            type = SEG_LINETO;
            return;
        }
        if (src.isDone()) {
            done = true;
            return;
        }
        int t = src.currentSegment(segment);
        src.next();
        switch (t) {
            case SEG_MOVETO:
                curX = segment[0];
                curY = segment[1];
                moveX = curX;
                moveY = curY;
                type = SEG_MOVETO;
                break;
            case SEG_LINETO:
                curX = segment[0];
                curY = segment[1];
                type = SEG_LINETO;
                break;
            case SEG_QUADTO:
                pendingCount = 0;
                pendingIndex = 0;
                quad(curX, curY, segment[0], segment[1], segment[2], segment[3], 0);
                advance();
                break;
            case SEG_CUBICTO:
                pendingCount = 0;
                pendingIndex = 0;
                cubic(curX, curY, segment[0], segment[1], segment[2], segment[3], segment[4], segment[5], 0);
                advance();
                break;
            default:
                curX = moveX;
                curY = moveY;
                type = SEG_CLOSE;
                break;
        }
    }

    private void push(double x, double y) {
        if (pendingCount + 2 > pending.length) {
            double[] p = new double[pending.length * 2];
            System.arraycopy(pending, 0, p, 0, pendingCount);
            pending = p;
        }
        pending[pendingCount++] = x;
        pending[pendingCount++] = y;
    }

    private void quad(double x0, double y0, double cx, double cy, double x1, double y1, int level) {
        if (level >= limit || Line2D.ptSegDistSq(x0, y0, x1, y1, cx, cy) <= flatnessSq) {
            push(x1, y1);
            return;
        }
        double lx = (x0 + cx) / 2.0;
        double ly = (y0 + cy) / 2.0;
        double rx = (cx + x1) / 2.0;
        double ry = (cy + y1) / 2.0;
        double mx = (lx + rx) / 2.0;
        double my = (ly + ry) / 2.0;
        quad(x0, y0, lx, ly, mx, my, level + 1);
        quad(mx, my, rx, ry, x1, y1, level + 1);
    }

    private void cubic(double x0, double y0, double ax, double ay, double bx, double by,
            double x1, double y1, int level) {
        if (level >= limit || (Line2D.ptSegDistSq(x0, y0, x1, y1, ax, ay) <= flatnessSq
                && Line2D.ptSegDistSq(x0, y0, x1, y1, bx, by) <= flatnessSq)) {
            push(x1, y1);
            return;
        }
        double lax = (x0 + ax) / 2.0;
        double lay = (y0 + ay) / 2.0;
        double midx = (ax + bx) / 2.0;
        double midy = (ay + by) / 2.0;
        double rbx = (bx + x1) / 2.0;
        double rby = (by + y1) / 2.0;
        double lbx = (lax + midx) / 2.0;
        double lby = (lay + midy) / 2.0;
        double rax = (midx + rbx) / 2.0;
        double ray = (midy + rby) / 2.0;
        double mx = (lbx + rax) / 2.0;
        double my = (lby + ray) / 2.0;
        cubic(x0, y0, lax, lay, lbx, lby, mx, my, level + 1);
        cubic(mx, my, rax, ray, rbx, rby, x1, y1, level + 1);
    }

    @Override
    public int currentSegment(float[] coords) {
        if (done) {
            throw new NoSuchElementException("flattening iterator out of bounds");
        }
        if (type != SEG_CLOSE) {
            coords[0] = (float) curX;
            coords[1] = (float) curY;
        }
        return type;
    }

    @Override
    public int currentSegment(double[] coords) {
        if (done) {
            throw new NoSuchElementException("flattening iterator out of bounds");
        }
        if (type != SEG_CLOSE) {
            coords[0] = curX;
            coords[1] = curY;
        }
        return type;
    }
}
