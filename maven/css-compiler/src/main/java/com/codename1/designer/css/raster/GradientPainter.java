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

import java.util.List;

/// Samples a CSS gradient per pixel.
///
/// `java.awt.MultipleGradientPaint` is deliberately not used. It rejects two
/// stops at the same fraction, which is how CSS writes a hard edge; it has
/// no conic form and no stops in px; and it interpolates non-premultiplied
/// channels, which turns a fade to `transparent` (transparent black) grey
/// in the middle.
///
/// Stops go through the CSS colour stop fix-up:
///
/// 1. a first stop without a position is at 0, a last one at 100%;
/// 2. a stop positioned before an earlier one is moved up to it, so the
///    positions never decrease;
/// 3. a run of stops without positions is spread evenly between its
///    positioned neighbours.
///
/// Colours are interpolated premultiplied by alpha. Two stops at the same
/// position make a hard edge, and the later one wins exactly on it.
///
/// Each pixel takes the colour at its centre. The gradient box is the box
/// handed in, which the rasterizer makes the border box; sizing a gradient
/// to the padding box (`background-origin`) or with `background-size` is
/// out of scope.
public final class GradientPainter {
    private final GradientSpec spec;
    private final double boxX;
    private final double boxY;
    private final double boxW;
    private final double boxH;

    /// Resolved stop positions as fractions of the gradient line (linear),
    /// the gradient ray (radial) or a full turn (conic).
    private final double[] pos;
    /// Premultiplied stop colours, each channel 0 to 1.
    private final float[] sa;
    private final float[] sr;
    private final float[] sg;
    private final float[] sb;
    private final int[] argb;

    /// Unit vector along the linear gradient line and the line's length.
    private double dirX;
    private double dirY;
    private double lineLength;
    /// Centre and radii of a radial gradient, centre of a conic one.
    private double cx;
    private double cy;
    private double rx;
    private double ry;

    /// Prepares a gradient for a box.
    ///
    /// #### Parameters
    ///
    /// - `spec`: the gradient
    ///
    /// - `boxX`: left edge of the gradient box in image coordinates
    ///
    /// - `boxY`: top edge of the gradient box
    ///
    /// - `boxW`: width of the gradient box, positive
    ///
    /// - `boxH`: height of the gradient box, positive
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: naming the field, for a gradient with
    ///   fewer than two stops, a `null` enum, or a stop unit that does not
    ///   apply to the gradient type
    public GradientPainter(GradientSpec spec, double boxX, double boxY, double boxW, double boxH) {
        if (spec == null) {
            throw new IllegalArgumentException("gradient must not be null");
        }
        if (spec.getType() == null) {
            throw new IllegalArgumentException("gradient.type must not be null");
        }
        List<GradientSpec.Stop> stops = spec.getStops();
        if (stops == null || stops.size() < 2) {
            throw new IllegalArgumentException("gradient.stops needs at least two stops");
        }
        this.spec = spec;
        this.boxX = boxX;
        this.boxY = boxY;
        this.boxW = boxW;
        this.boxH = boxH;
        double pxLength;
        switch (spec.getType()) {
            case LINEAR:
                setUpLinear();
                pxLength = lineLength;
                break;
            case RADIAL:
                setUpRadial();
                pxLength = rx;
                break;
            default:
                cx = resolve(spec.getCenterX(), spec.isCenterXPercent(), boxX, boxW, "gradient.centerX");
                cy = resolve(spec.getCenterY(), spec.isCenterYPercent(), boxY, boxH, "gradient.centerY");
                pxLength = 0;
                break;
        }
        int n = stops.size();
        pos = new double[n];
        sa = new float[n];
        sr = new float[n];
        sg = new float[n];
        sb = new float[n];
        argb = new int[n];
        boolean conic = spec.getType() == GradientSpec.Type.CONIC;
        for (int i = 0; i < n; i++) {
            GradientSpec.Stop s = stops.get(i);
            if (s == null) {
                throw new IllegalArgumentException("gradient.stops[" + i + "] must not be null");
            }
            if (s.unit == null) {
                throw new IllegalArgumentException("gradient.stops[" + i + "].unit must not be null");
            }
            double p;
            switch (s.unit) {
                case NONE:
                    p = Double.NaN;
                    break;
                case PERCENT:
                    p = s.position / 100;
                    break;
                case PX:
                    if (conic) {
                        throw new IllegalArgumentException(
                                "gradient.stops[" + i + "].unit PX is not valid in a conic gradient");
                    }
                    p = pxLength > 0 ? s.position / pxLength : 0;
                    break;
                default:
                    if (!conic) {
                        throw new IllegalArgumentException(
                                "gradient.stops[" + i + "].unit DEGREES is only valid in a conic gradient");
                    }
                    p = s.position / 360;
                    break;
            }
            if (s.unit != GradientSpec.Unit.NONE && (Double.isNaN(p) || Double.isInfinite(p))) {
                throw new IllegalArgumentException("gradient.stops[" + i + "].position is not a number");
            }
            pos[i] = p;
            argb[i] = s.color;
            float a = (s.color >>> 24) / 255f;
            sa[i] = a;
            sr[i] = ((s.color >> 16) & 0xff) / 255f * a;
            sg[i] = ((s.color >> 8) & 0xff) / 255f * a;
            sb[i] = (s.color & 0xff) / 255f * a;
        }
        fixUp(pos);
    }

    /// The CSS colour stop fix-up, in place. `NaN` marks a stop without a
    /// position.
    static void fixUp(double[] pos) {
        int n = pos.length;
        if (Double.isNaN(pos[0])) {
            pos[0] = 0;
        }
        if (Double.isNaN(pos[n - 1])) {
            pos[n - 1] = 1;
        }
        double max = pos[0];
        for (int i = 1; i < n; i++) {
            if (!Double.isNaN(pos[i])) {
                if (pos[i] < max) {
                    pos[i] = max;
                }
                max = pos[i];
            }
        }
        int i = 1;
        while (i < n) {
            if (Double.isNaN(pos[i])) {
                int end = i;
                while (Double.isNaN(pos[end])) {
                    end++;
                }
                double from = pos[i - 1];
                double step = (pos[end] - from) / (end - i + 1);
                for (int j = i; j < end; j++) {
                    pos[j] = from + step * (j - i + 1);
                }
                i = end;
            }
            i++;
        }
    }

    /// The resolved stop positions after fix-up, as fractions. A copy.
    public double[] stopPositions() {
        return pos.clone();
    }

    private static double resolve(double v, boolean percent, double origin, double size, String field) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw new IllegalArgumentException(field + " is not a number");
        }
        return origin + (percent ? size * v / 100 : v);
    }

    private void setUpLinear() {
        double dx;
        double dy;
        if (spec.isToCorner()) {
            int sx = Integer.signum(spec.getCornerX());
            int sy = Integer.signum(spec.getCornerY());
            if (sx == 0 && sy == 0) {
                // No side at all: the CSS default direction, to bottom.
                dx = 0;
                dy = 1;
            } else if (sx == 0 || sy == 0) {
                dx = sx;
                dy = sy;
            } else {
                // The CSS "magic corner": the angle is chosen so that the
                // 50% line runs through the two OTHER corners. That line is
                // the diagonal (sx * W, -sy * H); the gradient line is
                // perpendicular to it, which is (sx * H, sy * W). It only
                // points at the named corner when the box is square.
                dx = sx * boxH;
                dy = sy * boxW;
                double len = Math.sqrt(dx * dx + dy * dy);
                dx /= len;
                dy /= len;
            }
        } else {
            double a = spec.getAngleDeg();
            if (Double.isNaN(a) || Double.isInfinite(a)) {
                throw new IllegalArgumentException("gradient.angleDeg is not a number");
            }
            double rad = Math.toRadians(a);
            dx = Math.sin(rad);
            dy = -Math.cos(rad);
        }
        dirX = dx;
        dirY = dy;
        // The gradient line runs through the centre and is exactly long
        // enough for the 0% and 100% lines to touch the two corners.
        lineLength = Math.abs(boxW * dx) + Math.abs(boxH * dy);
    }

    private void setUpRadial() {
        if (spec.getShape() == null) {
            throw new IllegalArgumentException("gradient.shape must not be null");
        }
        if (spec.getExtent() == null) {
            throw new IllegalArgumentException("gradient.extent must not be null");
        }
        cx = resolve(spec.getCenterX(), spec.isCenterXPercent(), boxX, boxW, "gradient.centerX");
        cy = resolve(spec.getCenterY(), spec.isCenterYPercent(), boxY, boxH, "gradient.centerY");
        double left = Math.abs(cx - boxX);
        double right = Math.abs(boxX + boxW - cx);
        double top = Math.abs(cy - boxY);
        double bottom = Math.abs(boxY + boxH - cy);
        boolean circle = spec.getShape() == GradientSpec.Shape.CIRCLE;
        GradientSpec.Extent e = spec.getExtent();
        if (e == GradientSpec.Extent.EXPLICIT) {
            rx = spec.getRadiusX();
            ry = circle ? rx : spec.getRadiusY();
            if (Double.isNaN(rx) || rx < 0) {
                throw new IllegalArgumentException("gradient.radiusX must not be negative");
            }
            if (Double.isNaN(ry) || ry < 0) {
                throw new IllegalArgumentException("gradient.radiusY must not be negative");
            }
            return;
        }
        boolean closest = e == GradientSpec.Extent.CLOSEST_SIDE || e == GradientSpec.Extent.CLOSEST_CORNER;
        boolean corner = e == GradientSpec.Extent.CLOSEST_CORNER || e == GradientSpec.Extent.FARTHEST_CORNER;
        // The horizontal and vertical distance to the closest (or farthest)
        // side. These are also the offsets of the closest (or farthest)
        // corner, because that corner is where those two sides meet.
        double sideX = closest ? Math.min(left, right) : Math.max(left, right);
        double sideY = closest ? Math.min(top, bottom) : Math.max(top, bottom);
        if (circle) {
            if (corner) {
                rx = Math.sqrt(sideX * sideX + sideY * sideY);
            } else {
                rx = closest ? Math.min(sideX, sideY) : Math.max(sideX, sideY);
            }
            ry = rx;
        } else if (corner) {
            // An ellipse through the corner with the aspect ratio of the
            // matching -side ellipse: (sx/rx)^2 + (sy/ry)^2 = 1 with
            // rx/ry = sx/sy gives rx = sx * sqrt(2), ry = sy * sqrt(2).
            rx = sideX * Math.sqrt(2);
            ry = sideY * Math.sqrt(2);
        } else {
            rx = sideX;
            ry = sideY;
        }
    }

    /// The gradient parameter at a point: 0 at the first default stop
    /// position and 1 at the last. Unbounded for linear and radial
    /// gradients, in `[0, 1)` for a conic one.
    public double parameterAt(double x, double y) {
        switch (spec.getType()) {
            case LINEAR:
                return ((x - (boxX + boxW / 2)) * dirX + (y - (boxY + boxH / 2)) * dirY) / lineLength + 0.5;
            case RADIAL: {
                if (!(rx > 0) || !(ry > 0)) {
                    // A degenerate ending shape: CSS renders it as if it were
                    // vanishingly small, so every point is past the last stop.
                    return Double.POSITIVE_INFINITY;
                }
                double dx = (x - cx) / rx;
                double dy = (y - cy) / ry;
                return Math.sqrt(dx * dx + dy * dy);
            }
            default: {
                // atan2(dx, -dy) is the CSS bearing: 0 up, clockwise.
                double deg = Math.toDegrees(Math.atan2(x - cx, -(y - cy))) - spec.getFromAngleDeg();
                double t = deg / 360;
                t -= Math.floor(t);
                return t;
            }
        }
    }

    /// The colour at a gradient parameter, as non-premultiplied ARGB.
    public int colorAt(double t) {
        int n = pos.length;
        double first = pos[0];
        double last = pos[n - 1];
        if (spec.isRepeating()) {
            double span = last - first;
            if (!(span > 1e-9) || Double.isInfinite(t)) {
                return argb[n - 1];
            }
            double k = (t - first) / span;
            t = first + (k - Math.floor(k)) * span;
        }
        if (t < first) {
            return argb[0];
        }
        if (!(t < last)) {
            return argb[n - 1];
        }
        int i = 0;
        while (i < n - 2 && t >= pos[i + 1]) {
            i++;
        }
        double span = pos[i + 1] - pos[i];
        float f = span > 0 ? (float) ((t - pos[i]) / span) : 1f;
        float a = sa[i] + (sa[i + 1] - sa[i]) * f;
        if (a <= 0f) {
            return 0;
        }
        float r = sr[i] + (sr[i + 1] - sr[i]) * f;
        float g = sg[i] + (sg[i + 1] - sg[i]) * f;
        float b = sb[i] + (sb[i + 1] - sb[i]) * f;
        return (Pixels.clamp8(a * 255f) << 24)
                | (Pixels.clamp8(r / a * 255f) << 16)
                | (Pixels.clamp8(g / a * 255f) << 8)
                | Pixels.clamp8(b / a * 255f);
    }

    /// The colour at a point in image coordinates.
    public int colorAtPoint(double x, double y) {
        return colorAt(parameterAt(x, y));
    }

    /// Paints the gradient over a whole `w` by `h` image, one sample at
    /// each pixel centre. The gradient is not clipped to its box here.
    ///
    /// #### Returns
    ///
    /// `w * h` non-premultiplied ARGB values, row by row
    public int[] paint(int w, int h) {
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                out[row + x] = colorAt(parameterAt(x + 0.5, y + 0.5));
            }
        }
        return out;
    }
}
