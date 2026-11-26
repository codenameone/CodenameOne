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
package com.codename1.art;

import com.codename1.util.MathUtil;
/// A linear, radial or conic gradient.
///
/// Coordinates are in the painted shape's space, or relative to its bounding box (0 to 1)
/// when `isObjectBoundingBox` is true, as SVG's `gradientUnits` allows. `getTransform` is
/// applied to the gradient before it is painted.
public final class GradientPaint extends ArtPaint {
    /// Colors change along the line from start to end.
    public static final int LINEAR = 0;
    /// Colors change from the focal point out to the circle.
    public static final int RADIAL = 1;
    /// Colors change with the angle around the center.
    public static final int CONIC = 2;
    /// Colors change with the distance from the center measured along both axes together,
    /// so the bands are squares turned 45 degrees: Photoshop's and Figma's diamond.
    public static final int DIAMOND = 3;
    /// Past the ends the end colors continue.
    public static final int SPREAD_PAD = 0;
    /// Past the ends the gradient repeats mirrored.
    public static final int SPREAD_REFLECT = 1;
    /// Past the ends the gradient repeats.
    public static final int SPREAD_REPEAT = 2;

    private int type;
    private float[] positions;
    private int[] colors;
    private double x1;
    private double y1;
    private double x2;
    private double y2;
    private double radius;
    private double fx;
    private double fy;
    private int spread = SPREAD_PAD;
    private boolean objectBoundingBox;
    private Matrix transform = Matrix.IDENTITY;

    /// Creates a gradient.
    ///
    /// #### Parameters
    ///
    /// - `type`: `LINEAR`, `RADIAL`, `CONIC` or `DIAMOND`
    /// - `positions`: stop offsets from 0 to 1, ascending
    /// - `colors`: one `0xAARRGGBB` color per stop
    public GradientPaint(int type, float[] positions, int[] colors) {
        if (positions.length != colors.length || positions.length == 0) {
            throw new IllegalArgumentException("one color per stop, at least one stop");
        }
        this.type = type;
        this.positions = copyOf(positions);
        this.colors = copyOf(colors);
    }

    // Stops are copied in and out: an undo snapshot shares nothing with what a caller holds.
    private static float[] copyOf(float[] a) {
        float[] c = new float[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    private static int[] copyOf(int[] a) {
        int[] c = new int[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    /// A linear gradient.
    ///
    /// #### Parameters
    ///
    /// - `x1`: start x
    /// - `y1`: start y
    /// - `x2`: end x
    /// - `y2`: end y
    /// - `positions`: stop offsets
    /// - `colors`: stop colors
    public static GradientPaint linear(double x1, double y1, double x2, double y2,
            float[] positions, int[] colors) {
        GradientPaint g = new GradientPaint(LINEAR, positions, colors);
        g.setPoints(x1, y1, x2, y2);
        return g;
    }

    /// A radial gradient with its focus at the center.
    ///
    /// #### Parameters
    ///
    /// - `cx`: center x
    /// - `cy`: center y
    /// - `r`: radius
    /// - `positions`: stop offsets
    /// - `colors`: stop colors
    public static GradientPaint radial(double cx, double cy, double r, float[] positions, int[] colors) {
        GradientPaint g = new GradientPaint(RADIAL, positions, colors);
        g.setPoints(cx, cy, cx, cy);
        g.radius = r;
        g.fx = cx;
        g.fy = cy;
        return g;
    }

    /// A diamond gradient.
    ///
    /// #### Parameters
    ///
    /// - `cx`: center x
    /// - `cy`: center y
    /// - `r`: the distance from the center to each corner of the outermost band
    /// - `positions`: stop offsets
    /// - `colors`: stop colors
    public static GradientPaint diamond(double cx, double cy, double r, float[] positions, int[] colors) {
        GradientPaint g = new GradientPaint(DIAMOND, positions, colors);
        g.setPoints(cx, cy, cx, cy);
        g.radius = r;
        g.fx = cx;
        g.fy = cy;
        return g;
    }

    /// A conic gradient.
    ///
    /// #### Parameters
    ///
    /// - `cx`: center x
    /// - `cy`: center y
    /// - `startAngle`: angle of the first stop in radians, clockwise from 3 o'clock
    /// - `positions`: stop offsets
    /// - `colors`: stop colors
    public static GradientPaint conic(double cx, double cy, double startAngle, float[] positions, int[] colors) {
        GradientPaint g = new GradientPaint(CONIC, positions, colors);
        g.setPoints(cx, cy, cx + Math.cos(startAngle), cy + Math.sin(startAngle));
        return g;
    }

    @Override
    public ArtPaint copy() {
        float[] p = new float[positions.length];
        System.arraycopy(positions, 0, p, 0, p.length);
        int[] c = new int[colors.length];
        System.arraycopy(colors, 0, c, 0, c.length);
        GradientPaint g = new GradientPaint(type, p, c);
        g.x1 = x1;
        g.y1 = y1;
        g.x2 = x2;
        g.y2 = y2;
        g.radius = radius;
        g.fx = fx;
        g.fy = fy;
        g.spread = spread;
        g.objectBoundingBox = objectBoundingBox;
        g.transform = transform;
        return copyBase(g);
    }

    /// Sets the defining points: start and end for linear, center (twice) for radial, center
    /// and a point on the start ray for conic.
    ///
    /// #### Parameters
    ///
    /// - `x1`: first x
    /// - `y1`: first y
    /// - `x2`: second x
    /// - `y2`: second y
    public void setPoints(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    /// Sets the radius and focal point of a radial gradient.
    ///
    /// #### Parameters
    ///
    /// - `r`: radius
    /// - `fx`: focal x
    /// - `fy`: focal y
    public void setRadial(double r, double fx, double fy) {
        this.radius = r;
        this.fx = fx;
        this.fy = fy;
    }

    /// The gradient type.
    public int getType() {
        return type;
    }

    /// Stop offsets, as a copy.
    public float[] getPositions() {
        return copyOf(positions);
    }

    /// Stop colors, as a copy.
    public int[] getColors() {
        return copyOf(colors);
    }

    /// Replaces the stops.
    ///
    /// #### Parameters
    ///
    /// - `positions`: offsets
    /// - `colors`: colors, one per offset
    public void setStops(float[] positions, int[] colors) {
        if (positions.length != colors.length || positions.length == 0) {
            throw new IllegalArgumentException("one color per stop, at least one stop");
        }
        this.positions = copyOf(positions);
        this.colors = copyOf(colors);
    }

    /// First x.
    public double getX1() {
        return x1;
    }

    /// First y.
    public double getY1() {
        return y1;
    }

    /// Second x.
    public double getX2() {
        return x2;
    }

    /// Second y.
    public double getY2() {
        return y2;
    }

    /// Radius of a radial gradient.
    public double getRadius() {
        return radius;
    }

    /// Focal x of a radial gradient.
    public double getFx() {
        return fx;
    }

    /// Focal y of a radial gradient.
    public double getFy() {
        return fy;
    }

    /// One of the `SPREAD_` constants.
    public int getSpread() {
        return spread;
    }

    /// Sets the spread.
    ///
    /// #### Parameters
    ///
    /// - `spread`: one of the `SPREAD_` constants
    public void setSpread(int spread) {
        this.spread = spread;
    }

    /// True when coordinates are fractions of the shape's bounding box.
    public boolean isObjectBoundingBox() {
        return objectBoundingBox;
    }

    /// Sets whether coordinates are relative to the bounding box.
    ///
    /// #### Parameters
    ///
    /// - `objectBoundingBox`: true for fractions of the bounding box
    public void setObjectBoundingBox(boolean objectBoundingBox) {
        this.objectBoundingBox = objectBoundingBox;
    }

    /// The gradient transform.
    public Matrix getTransform() {
        return transform;
    }

    /// Sets the gradient transform.
    ///
    /// #### Parameters
    ///
    /// - `transform`: applied to the gradient's coordinates
    public void setTransform(Matrix transform) {
        this.transform = transform == null ? Matrix.IDENTITY : transform;
    }

    @Override
    public int colorAt(double x, double y, double[] bbox) {
        Matrix m = transform;
        if (objectBoundingBox && bbox != null) {
            m = new Matrix(bbox[2], 0, 0, bbox[3], bbox[0], bbox[1]).multiply(transform);
        }
        double px = x;
        double py = y;
        if (!m.isIdentity()) {
            Matrix inv = m.invert();
            if (inv == null) {
                return colors[colors.length - 1];
            }
            px = inv.mapX(x, y);
            py = inv.mapY(x, y);
        }
        double t;
        if (type == LINEAR) {
            double dx = x2 - x1;
            double dy = y2 - y1;
            double len2 = dx * dx + dy * dy;
            t = len2 == 0 ? 1 : ((px - x1) * dx + (py - y1) * dy) / len2;
        } else if (type == RADIAL) {
            t = radialT(px, py);
        } else if (type == DIAMOND) {
            t = radius <= 0 ? 1 : (Math.abs(px - x1) + Math.abs(py - y1)) / radius;
        } else {
            double a0 = MathUtil.atan2(y2 - y1, x2 - x1);
            double a = MathUtil.atan2(py - y1, px - x1) - a0;
            t = a / (Math.PI * 2);
            t -= Math.floor(t);
        }
        return colorFor(t);
    }

    private double radialT(double px, double py) {
        if (radius <= 0) {
            return 1;
        }
        double cx = x1;
        double cy = y1;
        double ffx = fx;
        double ffy = fy;
        double ddx = px - ffx;
        double ddy = py - ffy;
        if (Math.abs(ffx - cx) < 1e-12 && Math.abs(ffy - cy) < 1e-12) {
            return Math.sqrt(ddx * ddx + ddy * ddy) / radius;
        }
        double fcx = ffx - cx;
        double fcy = ffy - cy;
        double a = ddx * ddx + ddy * ddy;
        double b = 2 * (ddx * fcx + ddy * fcy);
        double c = fcx * fcx + fcy * fcy - radius * radius;
        double disc = b * b - 4 * a * c;
        if (a == 0 || disc < 0) {
            return 0;
        }
        double s = (-b + Math.sqrt(disc)) / (2 * a);
        return s <= 0 ? 1 : 1 / s;
    }

    /// The color at offset `t` after spreading.
    public int colorFor(double t) {
        if (spread == SPREAD_REPEAT) {
            t = t - Math.floor(t);
        } else if (spread == SPREAD_REFLECT) {
            t = Math.abs(t) % 2;
            if (t > 1) {
                t = 2 - t;
            }
        }
        if (t <= positions[0]) {
            return colors[0];
        }
        int last = positions.length - 1;
        if (t >= positions[last]) {
            return colors[last];
        }
        for (int i = 1; i <= last; i++) {
            if (t <= positions[i]) {
                double span = positions[i] - positions[i - 1];
                double f = span <= 0 ? 1 : (t - positions[i - 1]) / span;
                return lerp(colors[i - 1], colors[i], f);
            }
        }
        return colors[last];
    }

    /// Mixes two colors.
    static int lerp(int c0, int c1, double f) {
        int out = 0;
        for (int sh = 24; sh >= 0; sh -= 8) {
            int a = (c0 >>> sh) & 0xff;
            int b = (c1 >>> sh) & 0xff;
            out |= ((int) (a + (b - a) * f + 0.5)) << sh;
        }
        return out;
    }
}
