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
package com.codename1.impl.pdf;

/// A path in device space, as the renderer hands it to a [PdfDevice].
public final class PdfPath {
    /// Segment kinds.
    public static final byte MOVE = 0;
    /// A line to one point.
    public static final byte LINE = 1;
    /// A quadratic to two points.
    public static final byte QUAD = 2;
    /// A cubic to three points.
    public static final byte CUBIC = 3;
    /// Closes the current subpath.
    public static final byte CLOSE = 4;

    private byte[] ops = new byte[16];
    private float[] pts = new float[64];
    private int opCount;
    private int ptCount;
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float maxY = -Float.MAX_VALUE;

    private void op(byte o, int coords) {
        if (opCount == ops.length) {
            byte[] n = new byte[ops.length * 2];
            System.arraycopy(ops, 0, n, 0, opCount);
            ops = n;
        }
        ops[opCount++] = o;
        if (ptCount + coords > pts.length) {
            float[] n = new float[Math.max(pts.length * 2, ptCount + coords)];
            System.arraycopy(pts, 0, n, 0, ptCount);
            pts = n;
        }
    }

    private void pt(double x, double y) {
        float fx = (float) x;
        float fy = (float) y;
        pts[ptCount++] = fx;
        pts[ptCount++] = fy;
        if (fx < minX) {
            minX = fx;
        }
        if (fx > maxX) {
            maxX = fx;
        }
        if (fy < minY) {
            minY = fy;
        }
        if (fy > maxY) {
            maxY = fy;
        }
    }

    /// Starts a subpath.
    public void moveTo(double x, double y) {
        op(MOVE, 2);
        pt(x, y);
    }

    /// Adds a line.
    public void lineTo(double x, double y) {
        op(LINE, 2);
        pt(x, y);
    }

    /// Adds a quadratic curve.
    public void quadTo(double cx, double cy, double x, double y) {
        op(QUAD, 4);
        pt(cx, cy);
        pt(x, y);
    }

    /// Adds a cubic curve.
    public void curveTo(double c1x, double c1y, double c2x, double c2y, double x, double y) {
        op(CUBIC, 6);
        pt(c1x, c1y);
        pt(c2x, c2y);
        pt(x, y);
    }

    /// Closes the current subpath.
    public void closePath() {
        op(CLOSE, 0);
    }

    /// True when nothing was added.
    public boolean isEmpty() {
        return opCount == 0;
    }

    /// The number of segments.
    public int getOpCount() {
        return opCount;
    }

    /// The kind of segment `i`.
    public byte getOp(int i) {
        return ops[i];
    }

    /// The coordinates, two per point, in segment order.
    public float[] getPoints() {
        return pts;
    }

    /// The bounds `{minX, minY, maxX, maxY}`, or null when empty.
    public float[] getBounds() {
        return ptCount == 0 ? null : new float[] {minX, minY, maxX, maxY};
    }

    /// This path cut into dashes: `dash` holds alternating on and off
    /// lengths in the path's units, starting `phase` into the pattern.
    /// Curves are flattened first. Each subpath restarts the pattern, as
    /// PDF requires; a closed subpath dashes its closing segment too.
    public PdfPath dash(float[] dash, float phase) {
        float total = 0;
        for (int i = 0; i < dash.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            total += Math.max(0, dash[i]);
        }
        if (dash.length == 0 || total <= 0) {
            return this;
        }
        PdfPath out = new PdfPath();
        Dasher d = new Dasher(out, dash, phase, total);
        int p = 0;
        float cx = 0;
        float cy = 0;
        float sx = 0;
        float sy = 0;
        for (int i = 0; i < opCount; i++) {
            switch (ops[i]) {
                case MOVE:
                    cx = pts[p];
                    cy = pts[p + 1];
                    sx = cx;
                    sy = cy;
                    p += 2;
                    d.start(cx, cy);
                    break;
                case LINE:
                    d.line(pts[p], pts[p + 1]);
                    cx = pts[p];
                    cy = pts[p + 1];
                    p += 2;
                    break;
                case QUAD: {
                    float x1 = pts[p];
                    float y1 = pts[p + 1];
                    float x2 = pts[p + 2];
                    float y2 = pts[p + 3];
                    int steps = steps(cx, cy, x1, y1, x1, y1, x2, y2);
                    for (int k = 1; k <= steps; k++) {
                        float t = k / (float) steps;
                        float u = 1 - t;
                        d.line(u * u * cx + 2 * u * t * x1 + t * t * x2, u * u * cy + 2 * u * t * y1 + t * t * y2);
                    }
                    cx = x2;
                    cy = y2;
                    p += 4;
                    break;
                }
                case CUBIC: {
                    float x1 = pts[p];
                    float y1 = pts[p + 1];
                    float x2 = pts[p + 2];
                    float y2 = pts[p + 3];
                    float x3 = pts[p + 4];
                    float y3 = pts[p + 5];
                    int steps = steps(cx, cy, x1, y1, x2, y2, x3, y3);
                    for (int k = 1; k <= steps; k++) {
                        float t = k / (float) steps;
                        float u = 1 - t;
                        float a = u * u * u;
                        float b = 3 * u * u * t;
                        float c = 3 * u * t * t;
                        float e = t * t * t;
                        d.line(a * cx + b * x1 + c * x2 + e * x3, a * cy + b * y1 + c * y2 + e * y3);
                    }
                    cx = x3;
                    cy = y3;
                    p += 6;
                    break;
                }
                default:
                    d.line(sx, sy);
                    cx = sx;
                    cy = sy;
                    break;
            }
        }
        return out;
    }

    private static int steps(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
        float len = Math.abs(x1 - x0) + Math.abs(y1 - y0) + Math.abs(x2 - x1) + Math.abs(y2 - y1)
                + Math.abs(x3 - x2) + Math.abs(y3 - y2);
        return Math.max(2, Math.min(64, (int) (len / 3)));
    }

    /// Walks segments and emits the "on" parts.
    private static final class Dasher {
        final PdfPath out;
        final float[] dash;
        final float phase;
        final float total;
        int index;
        float left;
        boolean on;
        float x;
        float y;
        boolean penDown;

        Dasher(PdfPath out, float[] dash, float phase, float total) {
            this.out = out;
            this.dash = dash;
            this.phase = phase;
            this.total = total;
        }

        void start(float sx, float sy) {
            x = sx;
            y = sy;
            index = 0;
            on = true;
            float ph = phase % total;
            if (ph < 0) {
                ph += total;
            }
            left = Math.max(0, dash[0]);
            while (ph > 0) {
                if (ph >= left) {
                    ph -= left;
                    advance();
                } else {
                    left -= ph;
                    ph = 0;
                }
            }
            penDown = false;
        }

        private void advance() {
            index = (index + 1) % dash.length;
            on = !on;
            left = Math.max(0, dash[index]);
        }

        void line(float tx, float ty) {
            float dx = tx - x;
            float dy = ty - y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float pos = 0;
            int guard = 0;
            while (pos < len && guard++ < 100000) {
                float step = Math.min(left, len - pos);
                float nx = x + dx * (pos + step) / len;
                float ny = y + dy * (pos + step) / len;
                if (on) {
                    if (!penDown) {
                        out.moveTo(x + dx * pos / len, y + dy * pos / len);
                        penDown = true;
                    }
                    out.lineTo(nx, ny);
                }
                pos += step;
                left -= step;
                if (left <= 0) {
                    advance();
                    penDown = false;
                }
            }
            x = tx;
            y = ty;
        }
    }
}
