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
package android.graphics;

import com.codename1.ui.geom.GeneralPath;

/// A geometric path. Kept as a list of move/line/quad/cubic/close commands --
/// arcs are converted to cubic Beziers when added -- so offsetting and
/// transforming are exact, and turned into a Codename One `GeneralPath` when
/// drawn.
public class Path {

    public enum Direction { CW, CCW }

    public enum FillType { WINDING, EVEN_ODD, INVERSE_WINDING, INVERSE_EVEN_ODD }

    public enum Op { DIFFERENCE, INTERSECT, UNION, XOR, REVERSE_DIFFERENCE }

    static final byte MOVE = 0;
    static final byte LINE = 1;
    static final byte QUAD = 2;
    static final byte CUBIC = 3;
    static final byte CLOSE = 4;

    private byte[] ops = new byte[16];
    private float[] pts = new float[64];
    private int nOps;
    private int nPts;
    private FillType fillType = FillType.WINDING;
    private float lastX;
    private float lastY;
    private float startX;
    private float startY;

    public Path() {
    }

    public Path(Path src) {
        set(src);
    }

    public void set(Path src) {
        ops = src.ops.clone();
        pts = src.pts.clone();
        nOps = src.nOps;
        nPts = src.nPts;
        fillType = src.fillType;
        lastX = src.lastX;
        lastY = src.lastY;
        startX = src.startX;
        startY = src.startY;
    }

    public void reset() {
        nOps = 0;
        nPts = 0;
        fillType = FillType.WINDING;
        lastX = lastY = startX = startY = 0;
    }

    public void rewind() {
        nOps = 0;
        nPts = 0;
        lastX = lastY = startX = startY = 0;
    }

    public boolean isEmpty() {
        return nOps == 0;
    }

    public FillType getFillType() {
        return fillType;
    }

    public void setFillType(FillType ft) {
        fillType = ft;
    }

    public boolean isInverseFillType() {
        return fillType == FillType.INVERSE_WINDING || fillType == FillType.INVERSE_EVEN_ODD;
    }

    public void toggleInverseFillType() {
        switch (fillType) {
            case WINDING:
                fillType = FillType.INVERSE_WINDING;
                break;
            case EVEN_ODD:
                fillType = FillType.INVERSE_EVEN_ODD;
                break;
            case INVERSE_WINDING:
                fillType = FillType.WINDING;
                break;
            default:
                fillType = FillType.EVEN_ODD;
                break;
        }
    }

    public void incReserve(int extraPtCount) {
    }

    private void op(byte op, float... coords) {
        if (nOps == ops.length) {
            byte[] n = new byte[nOps * 2];
            System.arraycopy(ops, 0, n, 0, nOps);
            ops = n;
        }
        ops[nOps++] = op;
        if (nPts + coords.length > pts.length) {
            float[] n = new float[Math.max(pts.length * 2, nPts + coords.length)];
            System.arraycopy(pts, 0, n, 0, nPts);
            pts = n;
        }
        System.arraycopy(coords, 0, pts, nPts, coords.length);
        nPts += coords.length;
        if (coords.length >= 2) {
            lastX = coords[coords.length - 2];
            lastY = coords[coords.length - 1];
        }
    }

    private void ensureMove() {
        if (nOps == 0 || ops[nOps - 1] == CLOSE) {
            op(MOVE, lastX, lastY);
            startX = lastX;
            startY = lastY;
        }
    }

    public void moveTo(float x, float y) {
        op(MOVE, x, y);
        startX = x;
        startY = y;
    }

    public void rMoveTo(float dx, float dy) {
        moveTo(lastX + dx, lastY + dy);
    }

    public void lineTo(float x, float y) {
        ensureMove();
        op(LINE, x, y);
    }

    public void rLineTo(float dx, float dy) {
        lineTo(lastX + dx, lastY + dy);
    }

    public void quadTo(float x1, float y1, float x2, float y2) {
        ensureMove();
        op(QUAD, x1, y1, x2, y2);
    }

    public void rQuadTo(float dx1, float dy1, float dx2, float dy2) {
        quadTo(lastX + dx1, lastY + dy1, lastX + dx2, lastY + dy2);
    }

    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        ensureMove();
        op(CUBIC, x1, y1, x2, y2, x3, y3);
    }

    public void rCubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        cubicTo(lastX + x1, lastY + y1, lastX + x2, lastY + y2, lastX + x3, lastY + y3);
    }

    public void close() {
        if (nOps > 0 && ops[nOps - 1] != CLOSE) {
            op(CLOSE);
            lastX = startX;
            lastY = startY;
        }
    }

    public void setLastPoint(float dx, float dy) {
        if (nPts >= 2) {
            pts[nPts - 2] = dx;
            pts[nPts - 1] = dy;
        }
        lastX = dx;
        lastY = dy;
    }

    /// Appends an arc of the ellipse in `oval`, angles in degrees clockwise
    /// from 3 o'clock, as Android measures them.
    public void arcTo(RectF oval, float startAngle, float sweepAngle, boolean forceMoveTo) {
        arcTo(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, forceMoveTo);
    }

    public void arcTo(RectF oval, float startAngle, float sweepAngle) {
        arcTo(oval, startAngle, sweepAngle, false);
    }

    public void arcTo(float left, float top, float right, float bottom, float startAngle, float sweepAngle,
                      boolean forceMoveTo) {
        float cx = (left + right) / 2;
        float cy = (top + bottom) / 2;
        float rx = (right - left) / 2;
        float ry = (bottom - top) / 2;
        double a0 = Math.toRadians(startAngle);
        float sx = cx + rx * (float) Math.cos(a0);
        float sy = cy + ry * (float) Math.sin(a0);
        if (forceMoveTo || nOps == 0 || ops[nOps - 1] == CLOSE) {
            moveTo(sx, sy);
        } else {
            lineTo(sx, sy);
        }
        appendArc(cx, cy, rx, ry, startAngle, sweepAngle);
    }

    public void addArc(RectF oval, float startAngle, float sweepAngle) {
        arcTo(oval, startAngle, sweepAngle, true);
    }

    public void addArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        arcTo(left, top, right, bottom, startAngle, sweepAngle, true);
    }

    private void appendArc(float cx, float cy, float rx, float ry, float startDeg, float sweepDeg) {
        if (sweepDeg == 0) {
            return;
        }
        int segments = (int) Math.ceil(Math.abs(sweepDeg) / 90.0);
        double step = Math.toRadians(sweepDeg) / segments;
        double a = Math.toRadians(startDeg);
        double k = 4.0 / 3.0 * Math.tan(step / 4);
        for (int i = 0; i < segments; i++) {
            double c0 = Math.cos(a);
            double s0 = Math.sin(a);
            double c1 = Math.cos(a + step);
            double s1 = Math.sin(a + step);
            float x1 = (float) (cx + rx * (c0 - k * s0));
            float y1 = (float) (cy + ry * (s0 + k * c0));
            float x2 = (float) (cx + rx * (c1 + k * s1));
            float y2 = (float) (cy + ry * (s1 - k * c1));
            float x3 = (float) (cx + rx * c1);
            float y3 = (float) (cy + ry * s1);
            op(CUBIC, x1, y1, x2, y2, x3, y3);
            a += step;
        }
    }

    public void addRect(float left, float top, float right, float bottom, Direction dir) {
        moveTo(left, top);
        if (dir == Direction.CCW) {
            lineTo(left, bottom);
            lineTo(right, bottom);
            lineTo(right, top);
        } else {
            lineTo(right, top);
            lineTo(right, bottom);
            lineTo(left, bottom);
        }
        close();
    }

    public void addRect(RectF rect, Direction dir) {
        addRect(rect.left, rect.top, rect.right, rect.bottom, dir);
    }

    public void addOval(float left, float top, float right, float bottom, Direction dir) {
        float cx = (left + right) / 2;
        float cy = (top + bottom) / 2;
        float rx = (right - left) / 2;
        moveTo(right, cy);
        appendArc(cx, cy, rx, (bottom - top) / 2, 0, dir == Direction.CCW ? -360 : 360);
        close();
    }

    public void addOval(RectF oval, Direction dir) {
        addOval(oval.left, oval.top, oval.right, oval.bottom, dir);
    }

    public void addCircle(float x, float y, float radius, Direction dir) {
        addOval(x - radius, y - radius, x + radius, y + radius, dir);
    }

    public void addRoundRect(RectF rect, float rx, float ry, Direction dir) {
        addRoundRect(rect.left, rect.top, rect.right, rect.bottom, rx, ry, dir);
    }

    public void addRoundRect(float left, float top, float right, float bottom, float rx, float ry, Direction dir) {
        addRoundRect(left, top, right, bottom, new float[] {rx, ry, rx, ry, rx, ry, rx, ry}, dir);
    }

    public void addRoundRect(RectF rect, float[] radii, Direction dir) {
        addRoundRect(rect.left, rect.top, rect.right, rect.bottom, radii, dir);
    }

    /// `radii` holds x/y pairs for the top-left, top-right, bottom-right and
    /// bottom-left corners.
    public void addRoundRect(float left, float top, float right, float bottom, float[] radii, Direction dir) {
        float w = right - left;
        float h = bottom - top;
        float[] r = new float[8];
        for (int i = 0; i < 8; i++) {
            r[i] = Math.max(0, Math.min(radii[i], (i % 2 == 0 ? w : h) / 2));
        }
        moveTo(left + r[0], top);
        lineTo(right - r[2], top);
        if (r[2] > 0 || r[3] > 0) {
            appendArc(right - r[2], top + r[3], r[2], r[3], -90, 90);
        }
        lineTo(right, bottom - r[5]);
        if (r[4] > 0 || r[5] > 0) {
            appendArc(right - r[4], bottom - r[5], r[4], r[5], 0, 90);
        }
        lineTo(left + r[6], bottom);
        if (r[6] > 0 || r[7] > 0) {
            appendArc(left + r[6], bottom - r[7], r[6], r[7], 90, 90);
        }
        lineTo(left, top + r[1]);
        if (r[0] > 0 || r[1] > 0) {
            appendArc(left + r[0], top + r[1], r[0], r[1], 180, 90);
        }
        close();
    }

    public void addPath(Path src) {
        addPath(src, 0, 0);
    }

    public void addPath(Path src, float dx, float dy) {
        int p = 0;
        for (int i = 0; i < src.nOps; i++) {
            int n = coordsFor(src.ops[i]);
            float[] c = new float[n];
            for (int j = 0; j < n; j += 2) {
                c[j] = src.pts[p + j] + dx;
                c[j + 1] = src.pts[p + j + 1] + dy;
            }
            p += n;
            if (src.ops[i] == MOVE) {
                moveTo(c[0], c[1]);
            } else {
                op(src.ops[i], c);
            }
        }
    }

    public void addPath(Path src, Matrix matrix) {
        Path copy = new Path(src);
        copy.transform(matrix);
        addPath(copy);
    }

    public void offset(float dx, float dy) {
        for (int i = 0; i < nPts; i += 2) {
            pts[i] += dx;
            pts[i + 1] += dy;
        }
        lastX += dx;
        lastY += dy;
        startX += dx;
        startY += dy;
    }

    public void offset(float dx, float dy, Path dst) {
        if (dst != null) {
            dst.set(this);
            dst.offset(dx, dy);
        } else {
            offset(dx, dy);
        }
    }

    public void transform(Matrix matrix) {
        matrix.mapPoints(pts, 0, pts, 0, nPts / 2);
        // The current point and contour start move with the path, so a
        // relative command or a close appended later continues from the
        // transformed position.
        float[] cached = {lastX, lastY, startX, startY};
        matrix.mapPoints(cached);
        lastX = cached[0];
        lastY = cached[1];
        startX = cached[2];
        startY = cached[3];
    }

    public void transform(Matrix matrix, Path dst) {
        if (dst != null) {
            dst.set(this);
            dst.transform(matrix);
        } else {
            transform(matrix);
        }
    }

    public void computeBounds(RectF bounds, boolean exact) {
        if (nPts == 0) {
            bounds.set(0, 0, 0, 0);
            return;
        }
        float l = pts[0];
        float t = pts[1];
        float r = l;
        float b = t;
        for (int i = 2; i < nPts; i += 2) {
            l = Math.min(l, pts[i]);
            r = Math.max(r, pts[i]);
            t = Math.min(t, pts[i + 1]);
            b = Math.max(b, pts[i + 1]);
        }
        bounds.set(l, t, r, b);
    }

    public boolean op(Path path, Op op) {
        // Boolean path operations need a geometry kernel Codename One does not
        // have; union is approximated by appending, the rest are unsupported.
        if (op == Op.UNION) {
            addPath(path);
            return true;
        }
        return false;
    }

    private static int coordsFor(byte op) {
        switch (op) {
            case MOVE:
            case LINE:
                return 2;
            case QUAD:
                return 4;
            case CUBIC:
                return 6;
            default:
                return 0;
        }
    }

    /// Approximates the path with line segments: triples of (fraction of
    /// the total length, x, y), one per point, curves flattened finely
    /// enough that no point is further than `acceptableError` from the curve.
    public float[] approximate(float acceptableError) {
        java.util.ArrayList<float[]> points = new java.util.ArrayList<float[]>();
        float err = acceptableError > 0 ? acceptableError : 0.5f;
        float cx = 0;
        float cy = 0;
        float sx = 0;
        float sy = 0;
        int p = 0;
        for (int i = 0; i < nOps; i++) {
            switch (ops[i]) {
                case MOVE:
                    cx = pts[p];
                    cy = pts[p + 1];
                    sx = cx;
                    sy = cy;
                    points.add(new float[] {cx, cy});
                    break;
                case LINE:
                    cx = pts[p];
                    cy = pts[p + 1];
                    points.add(new float[] {cx, cy});
                    break;
                case QUAD: {
                    float x1 = pts[p];
                    float y1 = pts[p + 1];
                    float x2 = pts[p + 2];
                    float y2 = pts[p + 3];
                    int n = segments(cx, cy, x1, y1, x1, y1, x2, y2, err);
                    for (int k = 1; k <= n; k++) {
                        float t = k / (float) n;
                        float u = 1 - t;
                        points.add(new float[] {u * u * cx + 2 * u * t * x1 + t * t * x2,
                                u * u * cy + 2 * u * t * y1 + t * t * y2});
                    }
                    cx = x2;
                    cy = y2;
                    break;
                }
                case CUBIC: {
                    float x1 = pts[p];
                    float y1 = pts[p + 1];
                    float x2 = pts[p + 2];
                    float y2 = pts[p + 3];
                    float x3 = pts[p + 4];
                    float y3 = pts[p + 5];
                    int n = segments(cx, cy, x1, y1, x2, y2, x3, y3, err);
                    for (int k = 1; k <= n; k++) {
                        float t = k / (float) n;
                        float u = 1 - t;
                        float a = u * u * u;
                        float b = 3 * u * u * t;
                        float c = 3 * u * t * t;
                        float d = t * t * t;
                        points.add(new float[] {a * cx + b * x1 + c * x2 + d * x3,
                                a * cy + b * y1 + c * y2 + d * y3});
                    }
                    cx = x3;
                    cy = y3;
                    break;
                }
                default:
                    cx = sx;
                    cy = sy;
                    points.add(new float[] {cx, cy});
                    break;
            }
            p += coordsFor(ops[i]);
        }
        if (points.isEmpty()) {
            return new float[] {0, 0, 0, 1, 0, 0};
        }
        float total = 0;
        float[] lengths = new float[points.size()];
        for (int i = 1; i < points.size(); i++) {
            float[] a = points.get(i - 1);
            float[] b = points.get(i);
            float dx = b[0] - a[0];
            float dy = b[1] - a[1];
            total += (float) Math.sqrt(dx * dx + dy * dy);
            lengths[i] = total;
        }
        float[] out = new float[points.size() * 3];
        for (int i = 0; i < points.size(); i++) {
            float[] pt = points.get(i);
            out[i * 3] = total == 0 ? (points.size() == 1 ? 0 : i / (float) (points.size() - 1)) : lengths[i] / total;
            out[i * 3 + 1] = pt[0];
            out[i * 3 + 2] = pt[1];
        }
        return out;
    }

    /// How many line segments approximate a curve within `err`, from the
    /// length of its control polygon.
    private static int segments(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                float err) {
        float len = dist(x0, y0, x1, y1) + dist(x1, y1, x2, y2) + dist(x2, y2, x3, y3);
        int n = (int) Math.ceil(Math.sqrt(len / err) * 2);
        return Math.max(4, Math.min(1024, n));
    }

    private static float dist(float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /// The path as a Codename One shape, offset by `dx`/`dy`.
    public GeneralPath toGeneralPath(float dx, float dy) {
        GeneralPath gp = new GeneralPath(fillType == FillType.EVEN_ODD || fillType == FillType.INVERSE_EVEN_ODD
                ? GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO);
        int p = 0;
        for (int i = 0; i < nOps; i++) {
            switch (ops[i]) {
                case MOVE:
                    gp.moveTo(pts[p] + dx, pts[p + 1] + dy);
                    break;
                case LINE:
                    gp.lineTo(pts[p] + dx, pts[p + 1] + dy);
                    break;
                case QUAD:
                    gp.quadTo(pts[p] + dx, pts[p + 1] + dy, pts[p + 2] + dx, pts[p + 3] + dy);
                    break;
                case CUBIC:
                    gp.curveTo(pts[p] + dx, pts[p + 1] + dy, pts[p + 2] + dx, pts[p + 3] + dy,
                            pts[p + 4] + dx, pts[p + 5] + dy);
                    break;
                default:
                    gp.closePath();
                    break;
            }
            p += coordsFor(ops[i]);
        }
        return gp;
    }
}
