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

/// Multi-stop linear gradient at an arbitrary angle. Mirrors CSS
/// `linear-gradient(<angle>, <stops>)`. Angle is in CSS degrees: 0 points
/// up (toward the top edge), 90 right, 180 down, 270 left.
///
/// #### Since
///
/// 8.1
public final class LinearGradient extends Gradient {
    private float angleDegrees;

    /// Creates a linear gradient at the given angle with the given stops.
    public LinearGradient(float angleDegrees, int[] colors, float[] positions) {
        super(colors, positions);
        this.angleDegrees = angleDegrees;
    }

    @Override
    public byte getKind() {
        return KIND_LINEAR;
    }

    public float getAngleDegrees() {
        return angleDegrees;
    }

    public LinearGradient setAngleDegrees(float angleDegrees) {
        this.angleDegrees = angleDegrees;
        return this;
    }

    /// Computes the endpoints of the gradient line for a rectangle of the
    /// given width / height (rect origin at (0,0)). Output is x0,y0,x1,y1.
    /// These endpoints span the full bounding box; stop positions are
    /// interpreted relative to them.
    public void computeEndpoints(int width, int height, float[] out) {
        double rad = Math.toRadians(angleDegrees);
        double sinA = Math.sin(rad);
        double cosA = Math.cos(rad);
        double cx = width * 0.5;
        double cy = height * 0.5;
        double half = Math.abs(width * 0.5 * sinA) + Math.abs(height * 0.5 * cosA);
        out[0] = (float) (cx - sinA * half);
        out[1] = (float) (cy + cosA * half);
        out[2] = (float) (cx + sinA * half);
        out[3] = (float) (cy - cosA * half);
    }

    /// Computes shader-ready endpoints for native APIs like Android's
    /// `LinearGradient` and Java2D's `LinearGradientPaint`. For NO_CYCLE
    /// this is the same as `computeEndpoints`. For REPEAT / REFLECT it
    /// clips the endpoint range to span exactly one stop-list period
    /// (from `getPositions()[0]` to `getPositions()[N-1]` of the original
    /// line) so the native shader's tile mode wraps the period across
    /// the rest of the bounding box - matching the CSS
    /// `repeating-linear-gradient` semantic.
    ///
    /// Use `getNormalizedPositions()` for the matching stop array when
    /// using these endpoints with REPEAT/REFLECT.
    public void computeShaderEndpoints(int width, int height, float[] out) {
        computeEndpoints(width, height, out);
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
        float x0 = out[0];
        float y0 = out[1];
        float x1 = out[2];
        float y1 = out[3];
        out[0] = x0 + p0 * (x1 - x0);
        out[1] = y0 + p0 * (y1 - y0);
        out[2] = x0 + pN * (x1 - x0);
        out[3] = y0 + pN * (y1 - y0);
    }

    /// Returns stop positions rescaled to `[0, 1]` within the
    /// `[first_stop, last_stop]` range of the original positions. For
    /// NO_CYCLE this is the same as `getPositions()`; for REPEAT/REFLECT
    /// it is the array to pass alongside `computeShaderEndpoints`.
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
        double rad = Math.toRadians(angleDegrees);
        double sinA = Math.sin(rad);
        double cosA = Math.cos(rad);
        double cx = width * 0.5;
        double cy = height * 0.5;
        double half = Math.abs(width * 0.5 * sinA) + Math.abs(height * 0.5 * cosA);
        double len = Math.max(1.0, 2.0 * half);
        double dx = px + 0.5 - cx;
        double dy = py + 0.5 - cy;
        double proj = dx * sinA - dy * cosA;
        return sampleStops((float) ((proj + half) / len));
    }

    @Override
    public LinearGradient copy() {
        int[] c = getColors();
        float[] p = getPositions();
        int[] cc = new int[c.length];
        float[] pp = new float[p.length];
        System.arraycopy(c, 0, cc, 0, c.length);
        System.arraycopy(p, 0, pp, 0, p.length);
        LinearGradient g = new LinearGradient(angleDegrees, cc, pp);
        g.setCycleMethod(getCycleMethod());
        return g;
    }
}
