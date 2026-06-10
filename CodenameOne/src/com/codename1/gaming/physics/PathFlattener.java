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
package com.codename1.gaming.physics;

import com.codename1.ui.geom.PathIterator;
import com.codename1.ui.geom.Shape;
import java.util.ArrayList;
import java.util.List;

/// Flattens a Codename One `com.codename1.ui.geom.Shape` into one or more polylines
/// (in pixel coordinates) so `PhysicsWorld#createShape` can turn each into a Box2D
/// collision fixture.
///
/// This is the bridge between Codename One's drawing geometry and the physics engine:
/// the shape's `com.codename1.ui.geom.PathIterator` is walked, quadratic and cubic
/// Bezier segments are subdivided into straight edges, and the result is split into
/// subpaths (each `#SEG_MOVETO` starts a new one) tagged as closed (a `#SEG_CLOSE`
/// loop) or open. It deliberately knows nothing about Box2D so it can be unit tested
/// on plain coordinate arrays.
final class PathFlattener {
    /// Number of straight segments a single Bezier curve is subdivided into. Collision
    /// outlines do not need the smoothness a rendered curve does, so a modest fixed
    /// count keeps the vertex count (and the fixture count) reasonable.
    private static final int CURVE_SEGMENTS = 12;

    /// Two coordinates closer than this (in pixels) are treated as the same point, used
    /// to drop a redundant closing vertex.
    private static final float EPSILON = 0.01f;

    private PathFlattener() {
    }

    /// A single flattened subpath: interleaved x,y pixel coordinates and whether the
    /// outline forms a closed loop.
    static final class Subpath {
        final float[] xy;
        final boolean closed;

        Subpath(float[] xy, boolean closed) {
            this.xy = xy;
            this.closed = closed;
        }
    }

    /// Flattens the shape into subpaths. Curves are subdivided; each `SEG_MOVETO`
    /// begins a new subpath, and a `SEG_CLOSE` marks the preceding one as a loop.
    static List flatten(Shape shape) {
        List result = new ArrayList();
        PathIterator it = shape.getPathIterator();
        float[] coords = new float[6];
        FloatBuf buf = new FloatBuf();
        float startX = 0;
        float startY = 0;
        float curX = 0;
        float curY = 0;
        boolean have = false;
        while (!it.isDone()) {
            int seg = it.currentSegment(coords);
            switch (seg) {
                case PathIterator.SEG_MOVETO:
                    if (have && buf.size() >= 2) {
                        result.add(new Subpath(buf.trimmed(), false));
                    }
                    buf.reset();
                    startX = coords[0];
                    startY = coords[1];
                    curX = startX;
                    curY = startY;
                    buf.add(curX);
                    buf.add(curY);
                    have = true;
                    break;
                case PathIterator.SEG_LINETO:
                    curX = coords[0];
                    curY = coords[1];
                    buf.add(curX);
                    buf.add(curY);
                    break;
                case PathIterator.SEG_QUADTO:
                    quad(buf, curX, curY, coords[0], coords[1], coords[2], coords[3]);
                    curX = coords[2];
                    curY = coords[3];
                    break;
                case PathIterator.SEG_CUBICTO:
                    cubic(buf, curX, curY, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    curX = coords[4];
                    curY = coords[5];
                    break;
                case PathIterator.SEG_CLOSE:
                    if (have) {
                        dropClosingDuplicate(buf, startX, startY);
                        if (buf.size() >= 2) {
                            result.add(new Subpath(buf.trimmed(), true));
                        }
                        buf.reset();
                        have = false;
                    }
                    break;
                default:
                    break;
            }
            it.next();
        }
        if (have && buf.size() >= 2) {
            result.add(new Subpath(buf.trimmed(), false));
        }
        return result;
    }

    /// Returns true if the polygon (interleaved x,y, n vertices) is convex -- the cross
    /// product of consecutive edges keeps a single sign all the way around. Collinear
    /// (zero) turns are tolerated.
    static boolean isConvex(float[] xy, int n) {
        if (n < 3) {
            return false;
        }
        int sign = 0;
        for (int i = 0; i < n; i++) {
            float ax = xy[(i * 2)];
            float ay = xy[(i * 2) + 1];
            int j = (i + 1) % n;
            int k = (i + 2) % n;
            float bx = xy[j * 2];
            float by = xy[(j * 2) + 1];
            float cx = xy[k * 2];
            float cy = xy[(k * 2) + 1];
            float cross = (bx - ax) * (cy - by) - (by - ay) * (cx - bx);
            if (cross > EPSILON) {
                if (sign < 0) {
                    return false;
                }
                sign = 1;
            } else if (cross < -EPSILON) {
                if (sign > 0) {
                    return false;
                }
                sign = -1;
            }
        }
        return true;
    }

    /// Drops the final vertex if it coincides with the subpath start, so a path that
    /// explicitly lines back to its origin before closing does not feed Box2D a
    /// duplicate vertex.
    private static void dropClosingDuplicate(FloatBuf buf, float startX, float startY) {
        int s = buf.size();
        if (s >= 4) {
            float lastX = buf.get(s - 2);
            float lastY = buf.get(s - 1);
            if (Math.abs(lastX - startX) < EPSILON && Math.abs(lastY - startY) < EPSILON) {
                buf.removeLastPoint();
            }
        }
    }

    private static void quad(FloatBuf buf, float x0, float y0, float cx, float cy, float x1, float y1) {
        for (int i = 1; i <= CURVE_SEGMENTS; i++) {
            float t = i / (float) CURVE_SEGMENTS;
            float u = 1 - t;
            float a = u * u;
            float b = 2 * u * t;
            float c = t * t;
            buf.add(a * x0 + b * cx + c * x1);
            buf.add(a * y0 + b * cy + c * y1);
        }
    }

    private static void cubic(FloatBuf buf, float x0, float y0, float c1x, float c1y,
            float c2x, float c2y, float x1, float y1) {
        for (int i = 1; i <= CURVE_SEGMENTS; i++) {
            float t = i / (float) CURVE_SEGMENTS;
            float u = 1 - t;
            float a = u * u * u;
            float b = 3 * u * u * t;
            float c = 3 * u * t * t;
            float d = t * t * t;
            buf.add(a * x0 + b * c1x + c * c2x + d * x1);
            buf.add(a * y0 + b * c1y + c * c2y + d * y1);
        }
    }

    /// A tiny growable float array, to flatten paths without boxing.
    private static final class FloatBuf {
        private float[] a = new float[16];
        private int n;

        void add(float v) {
            if (n == a.length) {
                float[] b = new float[a.length * 2];
                System.arraycopy(a, 0, b, 0, n);
                a = b;
            }
            a[n++] = v;
        }

        float get(int i) {
            return a[i];
        }

        int size() {
            return n;
        }

        void removeLastPoint() {
            n -= 2;
        }

        void reset() {
            n = 0;
        }

        float[] trimmed() {
            float[] b = new float[n];
            System.arraycopy(a, 0, b, 0, n);
            return b;
        }
    }
}
