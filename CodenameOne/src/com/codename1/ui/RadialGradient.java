/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

/// Multi-stop radial gradient with CSS shape / extent / center / radius
/// support. Mirrors CSS `radial-gradient([shape] [extent] [at <pos>], <stops>)`.
///
/// #### Since
///
/// 8.1
public final class RadialGradient extends Gradient {
    /// Radial shape: circular (single radius).
    public static final byte SHAPE_CIRCLE = 0;
    /// Radial shape: elliptical (separate x/y radii).
    public static final byte SHAPE_ELLIPSE = 1;

    /// CSS radial extents.
    public static final byte EXTENT_CLOSEST_SIDE = 0;
    public static final byte EXTENT_CLOSEST_CORNER = 1;
    public static final byte EXTENT_FARTHEST_SIDE = 2;
    public static final byte EXTENT_FARTHEST_CORNER = 3;
    /// Use the configured relativeRadiusX/Y verbatim (times the rectangle's
    /// larger dimension).
    public static final byte EXTENT_EXPLICIT = 4;

    private byte shape = SHAPE_ELLIPSE;
    private byte extent = EXTENT_FARTHEST_CORNER;
    private float relativeCenterX = 0.5f;
    private float relativeCenterY = 0.5f;
    private float relativeRadiusX = 1f;
    private float relativeRadiusY = 1f;

    public RadialGradient(int[] colors, float[] positions) {
        super(colors, positions);
    }

    @Override
    public byte getKind() {
        return KIND_RADIAL;
    }

    public byte getShape() {
        return shape;
    }

    public RadialGradient setShape(byte shape) {
        this.shape = shape;
        return this;
    }

    public byte getExtent() {
        return extent;
    }

    public RadialGradient setExtent(byte extent) {
        this.extent = extent;
        return this;
    }

    public float getRelativeCenterX() {
        return relativeCenterX;
    }

    public RadialGradient setRelativeCenterX(float relativeCenterX) {
        this.relativeCenterX = relativeCenterX;
        return this;
    }

    public float getRelativeCenterY() {
        return relativeCenterY;
    }

    public RadialGradient setRelativeCenterY(float relativeCenterY) {
        this.relativeCenterY = relativeCenterY;
        return this;
    }

    public float getRelativeRadiusX() {
        return relativeRadiusX;
    }

    public RadialGradient setRelativeRadiusX(float relativeRadiusX) {
        this.relativeRadiusX = relativeRadiusX;
        return this;
    }

    public float getRelativeRadiusY() {
        return relativeRadiusY;
    }

    public RadialGradient setRelativeRadiusY(float relativeRadiusY) {
        this.relativeRadiusY = relativeRadiusY;
        return this;
    }

    /// Computes (cx, cy, rx, ry) in pixel coordinates for a rectangle of the
    /// given width / height, applying the configured CSS extent.
    public void computeRadii(int width, int height, float[] out) {
        float cx = relativeCenterX * width;
        float cy = relativeCenterY * height;
        float rx;
        float ry;
        switch (extent) {
            case EXTENT_CLOSEST_SIDE:
                rx = Math.min(cx, width - cx);
                ry = Math.min(cy, height - cy);
                break;
            case EXTENT_FARTHEST_SIDE:
                rx = Math.max(cx, width - cx);
                ry = Math.max(cy, height - cy);
                break;
            case EXTENT_CLOSEST_CORNER: {
                rx = Math.min(cx, width - cx);
                ry = Math.min(cy, height - cy);
                float r = (float) Math.sqrt(rx * rx + ry * ry);
                rx = r;
                ry = r;
                break;
            }
            case EXTENT_FARTHEST_CORNER: {
                rx = Math.max(cx, width - cx);
                ry = Math.max(cy, height - cy);
                float r = (float) Math.sqrt(rx * rx + ry * ry);
                rx = r;
                ry = r;
                break;
            }
            case EXTENT_EXPLICIT:
            default:
                float ref = Math.max(width, height);
                rx = relativeRadiusX * ref;
                ry = relativeRadiusY * ref;
                break;
        }
        if (shape == SHAPE_CIRCLE) {
            float r = (extent == EXTENT_CLOSEST_SIDE || extent == EXTENT_CLOSEST_CORNER)
                    ? Math.min(rx, ry) : Math.max(rx, ry);
            rx = r;
            ry = r;
        }
        if (rx <= 0f) {
            rx = 1f;
        }
        if (ry <= 0f) {
            ry = 1f;
        }
        out[0] = cx;
        out[1] = cy;
        out[2] = rx;
        out[3] = ry;
    }

    /// Computes shader-ready (cx, cy, rx, ry) for native APIs like Android's
    /// `RadialGradient` and Java2D's `RadialGradientPaint`. For NO_CYCLE
    /// this is the same as `computeRadii`. For REPEAT / REFLECT it scales
    /// the radii so the [0, 1] shader range corresponds to exactly one
    /// stop-list period (`getPositions()[0]` to `getPositions()[N-1]`),
    /// matching the CSS `repeating-radial-gradient` semantic.
    ///
    /// Use `getNormalizedPositions()` for the matching stop array when
    /// using these radii with REPEAT/REFLECT.
    public void computeShaderRadii(int width, int height, float[] out) {
        computeRadii(width, height, out);
        byte cycle = getCycleMethod();
        if (cycle == CYCLE_NONE) {
            return;
        }
        float[] positions = getPositions();
        float p0 = positions[0];
        float pN = positions[positions.length - 1];
        if (pN - p0 < 1e-4f) {
            return;
        }
        // The native shader maps its [0, 1] range to [center, center+radius].
        // To make one stop-list period fit in [0, 1], scale the radii by the
        // span; rebase to a new effective inner edge by translating the radii.
        // (Android/Java2D radial gradients are defined out from the center -
        // there's no inner cutoff - so we only scale the outer radius.)
        out[2] *= (pN - p0);
        out[3] *= (pN - p0);
    }

    /// Returns stop positions rescaled to `[0, 1]` within the
    /// `[first_stop, last_stop]` range. Use alongside `computeShaderRadii`
    /// when feeding the native shader API.
    public float[] getNormalizedPositions() {
        float[] positions = getPositions();
        if (getCycleMethod() == CYCLE_NONE) {
            return positions;
        }
        float p0 = positions[0];
        float pN = positions[positions.length - 1];
        float span = pN - p0;
        if (span < 1e-4f) {
            return positions;
        }
        float[] out = new float[positions.length];
        for (int i = 0; i < positions.length; i++) {
            out[i] = (positions[i] - p0) / span;
        }
        return out;
    }

    @Override
    public int sampleArgb(int px, int py, int width, int height) {
        float[] geom = new float[4];
        computeRadii(width, height, geom);
        float dx = (px + 0.5f - geom[0]) / geom[2];
        float dy = (py + 0.5f - geom[1]) / geom[3];
        float t = (float) Math.sqrt(dx * dx + dy * dy);
        return sampleStops(t);
    }

    @Override
    public RadialGradient copy() {
        int[] c = getColors();
        float[] p = getPositions();
        int[] cc = new int[c.length];
        float[] pp = new float[p.length];
        System.arraycopy(c, 0, cc, 0, c.length);
        System.arraycopy(p, 0, pp, 0, p.length);
        RadialGradient g = new RadialGradient(cc, pp);
        g.setCycleMethod(getCycleMethod());
        g.shape = shape;
        g.extent = extent;
        g.relativeCenterX = relativeCenterX;
        g.relativeCenterY = relativeCenterY;
        g.relativeRadiusX = relativeRadiusX;
        g.relativeRadiusY = relativeRadiusY;
        return g;
    }
}
