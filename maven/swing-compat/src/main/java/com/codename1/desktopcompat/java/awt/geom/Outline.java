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

import com.codename1.desktopcompat.java.awt.Shape;

/// The flattened outline of a shape as a list of directed edges, with every
/// subpath closed, for hit testing.
///
/// Point tests count the edges a ray towards positive x crosses, with an
/// edge owning its upper end and not its lower one, so a point on a left or
/// top boundary is inside and one on a right or bottom boundary is not.
/// Rectangle tests are exact for polygons: the open interior of a rectangle
/// that no edge passes through lies wholly inside or wholly outside.
final class Outline {

    private double[] edges = new double[64];
    private int size;

    /// Flattens a shape to within a hundred-thousandth of its larger side.
    static Outline of(Shape s) {
        Rectangle2D b = s.getBounds2D();
        double flatness = Math.max(b.getWidth(), b.getHeight()) * 1e-5;
        if (!(flatness > 0.0)) {
            flatness = 0.0;
        }
        return new Outline(new FlatteningPathIterator(s.getPathIterator(null), flatness));
    }

    /// Reads an iterator that returns only moves, lines and closes.
    private Outline(PathIterator pi) {
        double[] c = new double[6];
        double startX = 0.0;
        double startY = 0.0;
        double curX = 0.0;
        double curY = 0.0;
        while (!pi.isDone()) {
            switch (pi.currentSegment(c)) {
                case PathIterator.SEG_MOVETO:
                    edge(curX, curY, startX, startY);
                    startX = c[0];
                    startY = c[1];
                    curX = startX;
                    curY = startY;
                    break;
                case PathIterator.SEG_LINETO:
                    edge(curX, curY, c[0], c[1]);
                    curX = c[0];
                    curY = c[1];
                    break;
                default:
                    edge(curX, curY, startX, startY);
                    curX = startX;
                    curY = startY;
                    break;
            }
            pi.next();
        }
        edge(curX, curY, startX, startY);
    }

    private void edge(double x1, double y1, double x2, double y2) {
        if (x1 == x2 && y1 == y2) {
            return;
        }
        if (size + 4 > edges.length) {
            double[] e = new double[edges.length * 2];
            System.arraycopy(edges, 0, e, 0, size);
            edges = e;
        }
        edges[size++] = x1;
        edges[size++] = y1;
        edges[size++] = x2;
        edges[size++] = y2;
    }

    /// The signed number of edges a ray from the point towards positive x
    /// crosses: downward edges count one way and upward edges the other.
    private int crossings(double px, double py) {
        int n = 0;
        for (int i = 0; i < size; i += 4) {
            double x1 = edges[i];
            double y1 = edges[i + 1];
            double x2 = edges[i + 2];
            double y2 = edges[i + 3];
            boolean down = y1 < y2;
            if (down ? (py < y1 || py >= y2) : (py < y2 || py >= y1)) {
                continue;
            }
            if (px >= x1 && px >= x2) {
                continue;
            }
            if ((px < x1 && px < x2) || px < x1 + (py - y1) * (x2 - x1) / (y2 - y1)) {
                n += down ? 1 : -1;
            }
        }
        return n;
    }

    boolean contains(double x, double y, int rule) {
        int n = crossings(x, y);
        return rule == PathIterator.WIND_NON_ZERO ? n != 0 : (n & 1) != 0;
    }

    /// Whether any edge passes through the open interior of the rectangle.
    private boolean entersInterior(double rx, double ry, double rw, double rh) {
        double[] range = new double[2];
        for (int i = 0; i < size; i += 4) {
            double x1 = edges[i];
            double y1 = edges[i + 1];
            double dx = edges[i + 2] - x1;
            double dy = edges[i + 3] - y1;
            // the part of the edge inside the closed rectangle, as a
            // parameter range along it
            range[0] = 0.0;
            range[1] = 1.0;
            if (clip(-dx, x1 - rx, range) && clip(dx, rx + rw - x1, range)
                    && clip(-dy, y1 - ry, range) && clip(dy, ry + rh - y1, range)
                    && range[0] < range[1]) {
                double t = (range[0] + range[1]) / 2.0;
                double mx = x1 + dx * t;
                double my = y1 + dy * t;
                if (mx > rx && mx < rx + rw && my > ry && my < ry + rh) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Narrows a parameter range to the half plane `p * t <= q`; false when
    /// nothing is left.
    private static boolean clip(double p, double q, double[] range) {
        if (p == 0.0) {
            return q >= 0.0;
        }
        double t = q / p;
        if (p < 0.0) {
            if (t > range[1]) {
                return false;
            }
            if (t > range[0]) {
                range[0] = t;
            }
        } else {
            if (t < range[0]) {
                return false;
            }
            if (t < range[1]) {
                range[1] = t;
            }
        }
        return true;
    }

    boolean intersects(double x, double y, double w, double h, int rule) {
        if (!(w > 0.0 && h > 0.0)) {
            return false;
        }
        return entersInterior(x, y, w, h) || contains(x + w / 2.0, y + h / 2.0, rule);
    }

    boolean contains(double x, double y, double w, double h, int rule) {
        if (!(w > 0.0 && h > 0.0)) {
            return false;
        }
        return !entersInterior(x, y, w, h) && contains(x + w / 2.0, y + h / 2.0, rule);
    }
}
