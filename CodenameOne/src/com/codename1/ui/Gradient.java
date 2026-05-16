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

import com.codename1.ui.geom.Rectangle2D;

/// Abstract description of a CSS-style gradient that can be used as a
/// background or painted directly via `Graphics#fillGradient`. Three
/// concrete subclasses mirror the corresponding CSS functions:
///
/// - `LinearGradient` for `linear-gradient(<angle>, <stops>)`
/// - `RadialGradient` for `radial-gradient([shape] [extent] [at pos], <stops>)`
/// - `ConicGradient` for `conic-gradient([from <angle>] [at pos], <stops>)`
///
/// A Gradient is a [Paint], so it can also be assigned via `Graphics#setColor(Paint)`
/// and consumed by `fillRect` / `fillShape`. The dedicated
/// `Graphics#fillGradient(Gradient, int, int, int, int)` entry point gives
/// the platform port the rectangle bounds up front so it can pick the
/// fastest native shader path (Java2D `LinearGradientPaint` /
/// `RadialGradientPaint`, Android `LinearGradient` / `RadialGradient` /
/// `SweepGradient`, Core Graphics `CGGradient`).
///
/// Subclass instances are intended to be immutable after construction;
/// modify via builder-style setters before handing the gradient off to
/// `Graphics` or `Style`. `copy()` produces a defensive deep clone for
/// places that must outlive caller mutation (e.g. async paint queues).
///
/// #### Since
///
/// 8.1
public abstract class Gradient implements Paint {
    /// Sentinel returned by `getKind()` for `LinearGradient` instances.
    public static final byte KIND_LINEAR = 0;
    /// Sentinel returned by `getKind()` for `RadialGradient` instances.
    public static final byte KIND_RADIAL = 1;
    /// Sentinel returned by `getKind()` for `ConicGradient` instances.
    public static final byte KIND_CONIC = 2;

    /// Cycle modes mirroring `MultipleGradientPaint.CycleMethod`. Repeated as
    /// byte constants here so that this class (and the .res serializer) does
    /// not pull the enum across the resource format boundary.
    public static final byte CYCLE_NONE = 0;
    public static final byte CYCLE_REPEAT = 1;
    public static final byte CYCLE_REFLECT = 2;

    private int[] colors;
    private float[] positions;
    private byte cycleMethod = CYCLE_NONE;

    Gradient(int[] colors, float[] positions) {
        if (colors == null || positions == null || colors.length != positions.length || colors.length < 2) {
            throw new IllegalArgumentException("colors and positions must be same length, at least 2");
        }
        this.colors = colors;
        this.positions = positions;
    }

    /// Returns one of `KIND_LINEAR`, `KIND_RADIAL`, `KIND_CONIC`.
    public abstract byte getKind();

    /// ARGB stop colors (length >= 2).
    public final int[] getColors() {
        return colors;
    }

    /// Stop positions in [0,1] aligned with `getColors()`.
    public final float[] getPositions() {
        return positions;
    }

    /// One of `CYCLE_NONE` / `CYCLE_REPEAT` / `CYCLE_REFLECT`. Defaults to NONE.
    public final byte getCycleMethod() {
        return cycleMethod;
    }

    /// Sets the cycle method. Returns `this` for chaining.
    public final Gradient setCycleMethod(byte cycleMethod) {
        this.cycleMethod = cycleMethod;
        return this;
    }

    /// Returns a defensive deep copy. Implemented by each concrete subclass
    /// so async-paint queues can capture an immutable snapshot.
    public abstract Gradient copy();

    @Override
    public final void paint(Graphics g, Rectangle2D bounds) {
        paint(g, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    @Override
    public final void paint(Graphics g, double x, double y, double w, double h) {
        g.fillGradient(this, (int) x, (int) y, (int) w, (int) h);
    }

    /// Software-rasterizer hook used by the default port implementation when
    /// no native gradient shader is available. Samples an ARGB color for the
    /// pixel at (px, py) within a rectangle of the given width / height.
    /// Ports overriding `fillGradient` directly do not call this.
    public abstract int sampleArgb(int px, int py, int width, int height);

    /// Samples one of the stops at fractional position t. Honors the
    /// configured cycle method. Shared by the three subclasses' sampling
    /// implementations.
    ///
    /// CSS `repeating-*-gradient` stops define one period from
    /// `positions[0]` to `positions[last]`, not `[0, 1]`. For
    /// `white 0%, red 16%` the period is 0.16 of the gradient extent and
    /// the pattern must wrap on that range; collapsing to `t - floor(t)`
    /// would leak the final color across the rest of the rect.
    protected final int sampleStops(float t) {
        float p0 = positions[0];
        float pN = positions[positions.length - 1];
        float period = pN - p0;
        switch (cycleMethod) {
            case CYCLE_REPEAT:
                if (period > 0) {
                    float rel = (t - p0) / period;
                    rel = rel - (float) Math.floor(rel);
                    t = p0 + rel * period;
                }
                break;
            case CYCLE_REFLECT:
                if (period > 0) {
                    float rel = Math.abs((t - p0) / period);
                    float intp = (float) Math.floor(rel);
                    float frac = rel - intp;
                    if ((((int) intp) & 1) != 0) {
                        frac = 1f - frac;
                    }
                    t = p0 + frac * period;
                }
                break;
            default:
                if (t <= p0) {
                    return colors[0];
                }
                if (t >= pN) {
                    return colors[colors.length - 1];
                }
                break;
        }
        for (int i = 1; i < positions.length; i++) {
            if (t <= positions[i]) {
                float span = positions[i] - positions[i - 1];
                float local = span <= 0 ? 0 : (t - positions[i - 1]) / span;
                return blendArgb(colors[i - 1], colors[i], local);
            }
        }
        return colors[colors.length - 1];
    }

    static int blendArgb(int c0, int c1, float t) {
        int a0 = (c0 >> 24) & 0xff;
        int r0 = (c0 >> 16) & 0xff;
        int g0 = (c0 >> 8) & 0xff;
        int b0 = c0 & 0xff;
        int a1 = (c1 >> 24) & 0xff;
        int r1 = (c1 >> 16) & 0xff;
        int g1 = (c1 >> 8) & 0xff;
        int b1 = c1 & 0xff;
        int a = (int) (a0 + (a1 - a0) * t + 0.5f);
        int r = (int) (r0 + (r1 - r0) * t + 0.5f);
        int g = (int) (g0 + (g1 - g0) * t + 0.5f);
        int b = (int) (b0 + (b1 - b0) * t + 0.5f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
