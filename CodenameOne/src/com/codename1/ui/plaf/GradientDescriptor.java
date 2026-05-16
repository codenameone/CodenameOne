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
package com.codename1.ui.plaf;

/// Describes a CSS-style gradient with multi-stop colors, arbitrary angles, full
/// radial shape/extent control, conic/sweep gradients and repeating cycle modes.
/// This is attached to a Style via setGradientDescriptor() and supersedes the
/// legacy 2-color start/end fields when the background type is one of the
/// extended gradient types (LINEAR_ANGLED / RADIAL_FULL / CONIC / REPEATING_*).
public final class GradientDescriptor {

    /// Single-direction angled linear gradient. angle is in CSS degrees:
    /// 0 = bottom-to-top, 90 = left-to-right, 180 = top-to-bottom, 270 = right-to-left.
    public static final byte KIND_LINEAR = 0;
    /// Radial gradient with circle or ellipse shape and configurable extent.
    public static final byte KIND_RADIAL = 1;
    /// Conic / sweep gradient that rotates colors around a center point.
    public static final byte KIND_CONIC = 2;

    /// Cycle method for the gradient outside the [0,1] stop range.
    public static final byte CYCLE_NONE = 0;
    /// Repeat the stop pattern (CSS repeating-linear / repeating-radial gradient).
    public static final byte CYCLE_REPEAT = 1;
    /// Mirror the stop pattern.
    public static final byte CYCLE_REFLECT = 2;

    /// Radial shape: circular (single radius).
    public static final byte SHAPE_CIRCLE = 0;
    /// Radial shape: elliptical (separate x/y radii).
    public static final byte SHAPE_ELLIPSE = 1;

    /// CSS radial extents. EXPLICIT means use the relativeRadius{X,Y} fields directly.
    public static final byte EXTENT_CLOSEST_SIDE = 0;
    public static final byte EXTENT_CLOSEST_CORNER = 1;
    public static final byte EXTENT_FARTHEST_SIDE = 2;
    public static final byte EXTENT_FARTHEST_CORNER = 3;
    public static final byte EXTENT_EXPLICIT = 4;

    private byte kind;
    private byte cycleMethod = CYCLE_NONE;
    private int[] colors;
    private float[] positions;

    // linear
    private float angleDegrees;

    // radial / conic
    private float relativeCenterX = 0.5f;
    private float relativeCenterY = 0.5f;

    // radial
    private byte radialShape = SHAPE_ELLIPSE;
    private byte radialExtent = EXTENT_FARTHEST_CORNER;
    private float relativeRadiusX = 1f;
    private float relativeRadiusY = 1f;

    // conic
    private float fromAngleDegrees;

    public GradientDescriptor() {
    }

    public byte getKind() {
        return kind;
    }

    public GradientDescriptor setKind(byte kind) {
        this.kind = kind;
        return this;
    }

    public byte getCycleMethod() {
        return cycleMethod;
    }

    public GradientDescriptor setCycleMethod(byte cycleMethod) {
        this.cycleMethod = cycleMethod;
        return this;
    }

    public int[] getColors() {
        return colors;
    }

    public float[] getPositions() {
        return positions;
    }

    /// Sets the stops. The two arrays must be the same length, with at least two
    /// entries; positions must be monotonically non-decreasing in [0,1].
    public GradientDescriptor setStops(int[] colors, float[] positions) {
        if (colors == null || positions == null || colors.length != positions.length || colors.length < 2) {
            throw new IllegalArgumentException("colors and positions must be same length, at least 2");
        }
        this.colors = colors;
        this.positions = positions;
        return this;
    }

    public float getAngleDegrees() {
        return angleDegrees;
    }

    public GradientDescriptor setAngleDegrees(float angleDegrees) {
        this.angleDegrees = angleDegrees;
        return this;
    }

    public float getRelativeCenterX() {
        return relativeCenterX;
    }

    public GradientDescriptor setRelativeCenterX(float relativeCenterX) {
        this.relativeCenterX = relativeCenterX;
        return this;
    }

    public float getRelativeCenterY() {
        return relativeCenterY;
    }

    public GradientDescriptor setRelativeCenterY(float relativeCenterY) {
        this.relativeCenterY = relativeCenterY;
        return this;
    }

    public byte getRadialShape() {
        return radialShape;
    }

    public GradientDescriptor setRadialShape(byte radialShape) {
        this.radialShape = radialShape;
        return this;
    }

    public byte getRadialExtent() {
        return radialExtent;
    }

    public GradientDescriptor setRadialExtent(byte radialExtent) {
        this.radialExtent = radialExtent;
        return this;
    }

    public float getRelativeRadiusX() {
        return relativeRadiusX;
    }

    public GradientDescriptor setRelativeRadiusX(float relativeRadiusX) {
        this.relativeRadiusX = relativeRadiusX;
        return this;
    }

    public float getRelativeRadiusY() {
        return relativeRadiusY;
    }

    public GradientDescriptor setRelativeRadiusY(float relativeRadiusY) {
        this.relativeRadiusY = relativeRadiusY;
        return this;
    }

    public float getFromAngleDegrees() {
        return fromAngleDegrees;
    }

    public GradientDescriptor setFromAngleDegrees(float fromAngleDegrees) {
        this.fromAngleDegrees = fromAngleDegrees;
        return this;
    }

    /// Computes the absolute radii in pixels for a region with the given width/height.
    /// For EXTENT_EXPLICIT the values are directly relativeRadius{X,Y} times the
    /// region's larger dimension; otherwise the radii follow the CSS spec.
    public void computeRadii(int width, int height, float[] out) {
        float cx = relativeCenterX * width;
        float cy = relativeCenterY * height;
        float rx, ry;
        switch (radialExtent) {
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
        if (radialShape == SHAPE_CIRCLE) {
            float r = (radialExtent == EXTENT_CLOSEST_SIDE || radialExtent == EXTENT_CLOSEST_CORNER)
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

    /// Computes the start/end endpoints of a linear gradient in pixel coordinates
    /// for a region of the given width/height. Angle follows CSS convention:
    /// 0deg = upward, 90deg = rightward, 180deg = downward, 270deg = leftward.
    /// The endpoints are placed so the gradient line covers the full bounding box.
    public void computeLinearEndpoints(int width, int height, float[] out) {
        double rad = Math.toRadians(angleDegrees);
        double sinA = Math.sin(rad);
        double cosA = Math.cos(rad);
        float cx = width * 0.5f;
        float cy = height * 0.5f;
        // Length of the projection of the half-bounding-box onto the gradient line.
        double half = Math.abs(width * 0.5 * sinA) + Math.abs(height * 0.5 * cosA);
        float x0 = (float) (cx - sinA * half);
        float y0 = (float) (cy + cosA * half);
        float x1 = (float) (cx + sinA * half);
        float y1 = (float) (cy - cosA * half);
        out[0] = x0;
        out[1] = y0;
        out[2] = x1;
        out[3] = y1;
    }

    public GradientDescriptor copy() {
        GradientDescriptor g = new GradientDescriptor();
        g.kind = kind;
        g.cycleMethod = cycleMethod;
        if (colors != null) {
            g.colors = new int[colors.length];
            System.arraycopy(colors, 0, g.colors, 0, colors.length);
        }
        if (positions != null) {
            g.positions = new float[positions.length];
            System.arraycopy(positions, 0, g.positions, 0, positions.length);
        }
        g.angleDegrees = angleDegrees;
        g.relativeCenterX = relativeCenterX;
        g.relativeCenterY = relativeCenterY;
        g.radialShape = radialShape;
        g.radialExtent = radialExtent;
        g.relativeRadiusX = relativeRadiusX;
        g.relativeRadiusY = relativeRadiusY;
        g.fromAngleDegrees = fromAngleDegrees;
        return g;
    }
}
