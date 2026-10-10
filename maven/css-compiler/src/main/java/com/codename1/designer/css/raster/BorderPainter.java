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
package com.codename1.designer.css.raster;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

/// Paints the four CSS borders of a box into the ring between its border
/// edge and its padding edge.
///
/// ### Uniform borders
///
/// When every visible side is `SOLID` and they all share one colour, the
/// ring is filled in one go: the outer path minus the inner path, even-odd.
///
/// ### Mixed borders
///
/// Otherwise each side is painted through a mask, the ring coverage times
/// the coverage of that side's wedge. The wedges split the box along the
/// mitre of each corner, the line from the outer corner to the inner
/// (padding edge) corner, continued inwards across the corner's curve. The
/// four contributions are added up premultiplied before they are blended
/// over the image, so the two halves of an antialiased mitre pixel add up to
/// full coverage instead of leaving a translucent seam.
///
/// ### Styles
///
/// - `DOUBLE`: two lines of a third of the width with a gap of a third;
///   below 3px there is no room and the side is solid.
/// - `DASHED`: a dash of twice the width and a gap of one width, or for a
///   border thinner than 3px a dash of three widths and a gap of two. That
///   is the choice Chrome makes, as is stretching the gap so a side starts
///   and ends on a whole dash.
/// - `DOTTED`: round dots one width across, about one width apart, with the
///   spacing stretched so a dot sits on each end of the side.
/// - `INSET` and `OUTSET`: the top and left sides are darkened for `INSET`,
///   the bottom and right ones for `OUTSET`; the other two keep the colour.
/// - `GROOVE` and `RIDGE`: the outer half of the width is painted as
///   `INSET` and the inner half as `OUTSET` for a groove, the reverse for a
///   ridge.
///
/// "Darkened" is Chrome's rule: every channel is multiplied by
/// `max(0, (v - 0.33) / v)` where `v` is the largest channel, 0 to 1. Black
/// therefore stays black, so a black `inset` border looks solid, as it does
/// in Chrome.
///
/// A dashed or dotted side is laid out along the middle of the ring, and
/// each side owns the half of each corner arc next to it.
public final class BorderPainter {
    private static final int TOP = RoundedBox.TOP;
    private static final int RIGHT = RoundedBox.RIGHT;
    private static final int BOTTOM = RoundedBox.BOTTOM;
    private static final int LEFT = RoundedBox.LEFT;

    private BorderPainter() {
    }

    /// The effective width of each side: zero for an invisible style, and
    /// scaled down together when two opposite borders are wider than the
    /// box, which cannot happen in a laid out CSS box but would otherwise
    /// turn the padding edge inside out.
    ///
    /// #### Parameters
    ///
    /// - `sides`: top, right, bottom, left
    ///
    /// - `boxW`: border box width
    ///
    /// - `boxH`: border box height
    ///
    /// #### Returns
    ///
    /// the widths for top, right, bottom, left
    public static double[] effectiveWidths(BorderSide[] sides, double boxW, double boxH) {
        double[] w = new double[4];
        for (int i = 0; i < 4; i++) {
            w[i] = sides[i].effectiveWidth();
        }
        double f = 1;
        if (w[LEFT] + w[RIGHT] > boxW) {
            f = Math.min(f, boxW / (w[LEFT] + w[RIGHT]));
        }
        if (w[TOP] + w[BOTTOM] > boxH) {
            f = Math.min(f, boxH / (w[TOP] + w[BOTTOM]));
        }
        if (f < 1) {
            for (int i = 0; i < 4; i++) {
                w[i] *= f;
            }
        }
        return w;
    }

    /// Chrome's darker shade of a colour, for the 3D border styles.
    public static int darken(int argb) {
        float r = ((argb >> 16) & 0xff) / 255f;
        float g = ((argb >> 8) & 0xff) / 255f;
        float b = (argb & 0xff) / 255f;
        float v = Math.max(r, Math.max(g, b));
        float m = v > 0f ? Math.max(0f, (v - 0.33f) / v) : 0f;
        return (argb & 0xff000000)
                | (Pixels.clamp8(r * m * 255f) << 16)
                | (Pixels.clamp8(g * m * 255f) << 8)
                | Pixels.clamp8(b * m * 255f);
    }

    /// Paints the borders.
    ///
    /// #### Parameters
    ///
    /// - `dst`: the image, `w * h` non-premultiplied ARGB values
    ///
    /// - `w`: image width
    ///
    /// - `h`: image height
    ///
    /// - `outer`: the border box in image coordinates
    ///
    /// - `sides`: top, right, bottom, left
    ///
    /// - `widths`: the effective widths, from [#effectiveWidths(BorderSide[], double, double)]
    public static void paint(int[] dst, int w, int h, RoundedBox outer, BorderSide[] sides, double[] widths) {
        boolean any = false;
        boolean uniform = true;
        int color = 0;
        for (int i = 0; i < 4; i++) {
            if (widths[i] > 0) {
                if (!any) {
                    color = sides[i].color;
                    any = true;
                }
                if (sides[i].style != BorderStyle.SOLID || sides[i].color != color) {
                    uniform = false;
                }
            }
        }
        if (!any) {
            return;
        }
        RoundedBox inner = outer.inset(widths[TOP], widths[RIGHT], widths[BOTTOM], widths[LEFT]);
        float[] ring = Pixels.coverage(RoundedBox.ring(outer, inner), w, h, 0, 0);
        if (uniform) {
            Pixels.fill(dst, color, ring);
            return;
        }

        int n = w * h;
        float[] acc = new float[n * 4];
        float[] gap = null;
        float[] half = null;
        // The four wedges tile the plane, so their coverages add up to one
        // in every pixel of the ring. The rasterizer rounds each to 8 bits,
        // though, and two halves of a mitre pixel can add up to 254/255.
        // Dividing by the sum takes that out, so the ring mask alone decides
        // how opaque a border pixel is.
        float[][] wedges = new float[4][];
        float[] total = new float[n];
        for (int side = 0; side < 4; side++) {
            wedges[side] = Pixels.coverage(wedge(outer, inner, widths, side, w, h), w, h, 0, 0);
            for (int i = 0; i < n; i++) {
                total[i] += wedges[side][i];
            }
        }
        for (int side = 0; side < 4; side++) {
            double bw = widths[side];
            int c = sides[side].color;
            if (!(bw > 0) || (c >>> 24) == 0) {
                continue;
            }
            float[] m = wedges[side];
            for (int i = 0; i < n; i++) {
                float r = ring[i];
                m[i] = r > 0f && total[i] > 0f ? m[i] / total[i] * r : 0f;
            }
            boolean topLeft = side == TOP || side == LEFT;
            switch (sides[side].style) {
                case DOUBLE:
                    if (bw >= 3) {
                        if (gap == null) {
                            gap = Pixels.coverage(RoundedBox.ring(
                                    outer.inset(widths[TOP] / 3, widths[RIGHT] / 3, widths[BOTTOM] / 3, widths[LEFT] / 3),
                                    outer.inset(widths[TOP] * 2 / 3, widths[RIGHT] * 2 / 3,
                                            widths[BOTTOM] * 2 / 3, widths[LEFT] * 2 / 3)), w, h, 0, 0);
                        }
                        for (int i = 0; i < n; i++) {
                            m[i] *= 1f - gap[i];
                        }
                    }
                    add(acc, m, null, c);
                    break;
                case DASHED:
                case DOTTED: {
                    RoundedBox mid = outer.inset(widths[TOP] / 2, widths[RIGHT] / 2, widths[BOTTOM] / 2, widths[LEFT] / 2);
                    double reach = 2 * Math.max(Math.max(widths[TOP], widths[BOTTOM]),
                            Math.max(widths[LEFT], widths[RIGHT])) + 2;
                    Shape pattern = sides[side].style == BorderStyle.DASHED
                            ? dashes(mid.sidePath(side), bw, reach)
                            : dots(mid.sidePath(side), bw);
                    float[] p = Pixels.coverage(pattern, w, h, 0, 0);
                    for (int i = 0; i < n; i++) {
                        m[i] *= p[i];
                    }
                    add(acc, m, null, c);
                    break;
                }
                case INSET:
                    add(acc, m, null, topLeft ? darken(c) : c);
                    break;
                case OUTSET:
                    add(acc, m, null, topLeft ? c : darken(c));
                    break;
                case GROOVE:
                case RIDGE: {
                    if (half == null) {
                        half = Pixels.coverage(outer.inset(widths[TOP] / 2, widths[RIGHT] / 2,
                                widths[BOTTOM] / 2, widths[LEFT] / 2).toPath(), w, h, 0, 0);
                    }
                    boolean groove = sides[side].style == BorderStyle.GROOVE;
                    // A groove's outer half is an inset border: dark on the
                    // top and left. Its inner half is the opposite.
                    boolean outerDark = groove == topLeft;
                    add(acc, m, invert(half), outerDark ? darken(c) : c);
                    add(acc, m, half, outerDark ? c : darken(c));
                    break;
                }
                default:
                    add(acc, m, null, c);
                    break;
            }
        }

        for (int i = 0; i < n; i++) {
            float sa = acc[i * 4];
            if (sa <= 0f) {
                continue;
            }
            if (sa > 1f) {
                sa = 1f;
            }
            int src = (0xff << 24)
                    | (Pixels.clamp8(acc[i * 4 + 1] / acc[i * 4] * 255f) << 16)
                    | (Pixels.clamp8(acc[i * 4 + 2] / acc[i * 4] * 255f) << 8)
                    | Pixels.clamp8(acc[i * 4 + 3] / acc[i * 4] * 255f);
            Pixels.blend(dst, i, src, sa);
        }
    }

    private static float[] invert(float[] m) {
        float[] out = new float[m.length];
        for (int i = 0; i < m.length; i++) {
            out[i] = 1f - m[i];
        }
        return out;
    }

    /// Adds `color * mask * extra` to the premultiplied accumulator.
    private static void add(float[] acc, float[] mask, float[] extra, int color) {
        float a = (color >>> 24) / 255f;
        float r = ((color >> 16) & 0xff) / 255f * a;
        float g = ((color >> 8) & 0xff) / 255f * a;
        float b = (color & 0xff) / 255f * a;
        for (int i = 0; i < mask.length; i++) {
            float m = mask[i];
            if (extra != null) {
                m *= extra[i];
            }
            if (m > 0f) {
                acc[i * 4] += a * m;
                acc[i * 4 + 1] += r * m;
                acc[i * 4 + 2] += g * m;
                acc[i * 4 + 3] += b * m;
            }
        }
    }

    /// The region of the image that belongs to one side.
    ///
    /// Each corner contributes a boundary of three points shared by the two
    /// sides that meet there: a point far outside the box, the inner
    /// corner, and the point where the mitre leaves the inner corner's
    /// curve. Beyond that the boundary runs to the centre of the padding
    /// box, which is inside the padding edge where there is no ring left to
    /// divide.
    ///
    /// The far point lies on the mitre line continued outwards, so the
    /// wedge edge never runs along the outer edge of the ring; two
    /// coinciding antialiased edges would square the coverage there. When
    /// one of the two sides at a corner has no width, the other owns the
    /// whole corner and the boundary leaves the box straight across the
    /// missing side instead.
    static Path2D.Double wedge(RoundedBox outer, RoundedBox inner, double[] widths, int side, int w, int h) {
        double far = 4.0 * (w + h) + 16;
        double x0 = outer.getX();
        double y0 = outer.getY();
        double x1 = x0 + outer.getWidth();
        double y1 = y0 + outer.getHeight();
        double l = widths[LEFT];
        double t = widths[TOP];
        double r = widths[RIGHT];
        double b = widths[BOTTOM];
        // Per corner (TL, TR, BR, BL): outer corner, inward mitre direction.
        double[] ox = {x0, x1, x1, x0};
        double[] oy = {y0, y0, y1, y1};
        double[] dx = {l, -r, -r, l};
        double[] dy = {t, t, -b, -b};
        // The outward diagonal, for a corner where neither side has a width.
        double[] qx = {-1, 1, 1, -1};
        double[] qy = {-1, -1, 1, 1};
        double cx = inner.getX() + inner.getWidth() / 2;
        double cy = inner.getY() + inner.getHeight() / 2;

        double[] farX = new double[4];
        double[] farY = new double[4];
        double[] inX = new double[4];
        double[] inY = new double[4];
        double[] endX = new double[4];
        double[] endY = new double[4];
        for (int c = 0; c < 4; c++) {
            inX[c] = ox[c] + dx[c];
            inY[c] = oy[c] + dy[c];
            double ax = Math.abs(dx[c]);
            double ay = Math.abs(dy[c]);
            if (ax > 0 && ay > 0) {
                double len = Math.sqrt(ax * ax + ay * ay);
                farX[c] = ox[c] - dx[c] / len * far;
                farY[c] = oy[c] - dy[c] / len * far;
            } else if (ay > 0) {
                farX[c] = inX[c] + qx[c] * far;
                farY[c] = inY[c];
            } else if (ax > 0) {
                farX[c] = inX[c];
                farY[c] = inY[c] + qy[c] * far;
            } else {
                farX[c] = ox[c] + qx[c] * far;
                farY[c] = oy[c] + qy[c] * far;
            }
            double k = Double.POSITIVE_INFINITY;
            if (ax > 0) {
                k = Math.min(k, inner.radiusX(c) / ax);
            }
            if (ay > 0) {
                k = Math.min(k, inner.radiusY(c) / ay);
            }
            if (Double.isInfinite(k)) {
                k = 0;
            }
            endX[c] = inX[c] + dx[c] * k;
            endY[c] = inY[c] + dy[c] * k;
        }
        int a = side;
        int z = (side + 1) % 4;
        Path2D.Double p = new Path2D.Double(Path2D.WIND_NON_ZERO);
        // Out through the corner of a square twice as far away, so the
        // wedge takes in everything beyond its side whichever way the two
        // far points lie.
        p.moveTo(farX[a], farY[a]);
        p.lineTo(ox[a] + qx[a] * 2 * far, oy[a] + qy[a] * 2 * far);
        p.lineTo(ox[z] + qx[z] * 2 * far, oy[z] + qy[z] * 2 * far);
        p.lineTo(farX[z], farY[z]);
        p.lineTo(inX[z], inY[z]);
        p.lineTo(endX[z], endY[z]);
        p.lineTo(cx, cy);
        p.lineTo(endX[a], endY[a]);
        p.lineTo(inX[a], inY[a]);
        p.closePath();
        return p;
    }

    /// Dashes along a side path, as a shape `reach` px wide so that it
    /// overshoots the ring on both sides and only the ring mask shapes the
    /// ends of a dash.
    private static Shape dashes(Path2D path, double width, double reach) {
        double length = length(path);
        double dash = width * (width >= 3 ? 2 : 3);
        double gap = width * (width >= 3 ? 1 : 2);
        if (!(length / dash < MAX_DOTS)) {
            // As for dots: dashes too short to be marks are the line they
            // add up to.
            return new BasicStroke((float) reach, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)
                    .createStrokedShape(path);
        }
        if (!(length > dash)) {
            return new BasicStroke((float) reach, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)
                    .createStrokedShape(path);
        }
        // Fit a whole number of dashes with one at each end, picking the
        // count whose gap is nearest the nominal one.
        int few = Math.max(2, (int) Math.floor((length + gap) / (dash + gap)));
        int many = few + 1;
        double fewGap = (length - few * dash) / (few - 1);
        double manyGap = (length - many * dash) / (many - 1);
        double best = manyGap <= 0 || Math.abs(fewGap - gap) < Math.abs(manyGap - gap) ? fewGap : manyGap;
        if (!(best > 0)) {
            return new BasicStroke((float) reach, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)
                    .createStrokedShape(path);
        }
        return new BasicStroke((float) reach, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10f,
                new float[] {(float) dash, (float) best}, 0f).createStrokedShape(path);
    }

    /// The most dots drawn along one side.
    private static final int MAX_DOTS = 65536;

    /// Round dots of diameter `width` centred on a side path, the first on
    /// its start and the last on its end.
    private static Shape dots(Path2D path, double width) {
        double[] pts = flatten(path);
        double length = length(pts);
        if (!(length / (2 * width) < MAX_DOTS)) {
            // Dots this small are not separate marks in any image the
            // rasterizer paints. They are drawn as the line they add up to,
            // instead of one by one without end.
            return new BasicStroke((float) width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)
                    .createStrokedShape(path);
        }
        int spaces = Math.max(1, (int) Math.round(length / (2 * width)));
        double step = length / spaces;
        Path2D.Double out = new Path2D.Double(Path2D.WIND_NON_ZERO);
        int seg = 0;
        double before = 0;
        for (int i = 0; i <= spaces; i++) {
            double s = i == spaces ? length : i * step;
            double px = pts[0];
            double py = pts[1];
            while (seg < pts.length / 2 - 1) {
                double ax = pts[seg * 2];
                double ay = pts[seg * 2 + 1];
                double bx = pts[seg * 2 + 2];
                double by = pts[seg * 2 + 3];
                double d = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay));
                boolean lastSeg = seg == pts.length / 2 - 2;
                if (s <= before + d || lastSeg) {
                    double f = d > 0 ? Math.min(1, (s - before) / d) : 0;
                    px = ax + (bx - ax) * f;
                    py = ay + (by - ay) * f;
                    break;
                }
                before += d;
                seg++;
            }
            out.append(new Ellipse2D.Double(px - width / 2, py - width / 2, width, width), false);
        }
        return out;
    }

    private static double[] flatten(Path2D path) {
        PathIterator it = new FlatteningPathIterator(path.getPathIterator(null), 0.05);
        double[] pts = new double[64];
        int n = 0;
        double[] c = new double[6];
        while (!it.isDone()) {
            it.currentSegment(c);
            if (n + 2 > pts.length) {
                double[] bigger = new double[pts.length * 2];
                System.arraycopy(pts, 0, bigger, 0, n);
                pts = bigger;
            }
            pts[n++] = c[0];
            pts[n++] = c[1];
            it.next();
        }
        double[] out = new double[n];
        System.arraycopy(pts, 0, out, 0, n);
        return out;
    }

    private static double length(Path2D path) {
        return length(flatten(path));
    }

    private static double length(double[] pts) {
        double len = 0;
        for (int i = 2; i + 1 < pts.length; i += 2) {
            double dx = pts[i] - pts[i - 2];
            double dy = pts[i + 1] - pts[i - 1];
            len += Math.sqrt(dx * dx + dy * dy);
        }
        return len;
    }
}
