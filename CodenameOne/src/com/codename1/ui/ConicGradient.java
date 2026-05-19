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
package com.codename1.ui;

import com.codename1.util.MathUtil;

/// Conic (sweep) gradient. Mirrors CSS `conic-gradient([from <angle>] [at <pos>], <stops>)`.
/// `fromAngleDegrees` follows the CSS convention: 0 degrees points up
/// (toward the top edge), sweep is clockwise.
///
/// Conic gradients have no notion of cycle method - the [0,1] stop range
/// always wraps around the circle once.
public final class ConicGradient extends Gradient {
    private float fromAngleDegrees;
    private float relativeCenterX = 0.5f;
    private float relativeCenterY = 0.5f;

    public ConicGradient(int[] colors, float[] positions) {
        super(colors, positions);
    }

    @Override
    public byte getKind() {
        return KIND_CONIC;
    }

    public float getFromAngleDegrees() {
        return fromAngleDegrees;
    }

    public ConicGradient setFromAngleDegrees(float fromAngleDegrees) {
        this.fromAngleDegrees = fromAngleDegrees;
        invalidateRasterCache();
        return this;
    }

    public float getRelativeCenterX() {
        return relativeCenterX;
    }

    public ConicGradient setRelativeCenterX(float relativeCenterX) {
        this.relativeCenterX = relativeCenterX;
        invalidateRasterCache();
        return this;
    }

    public float getRelativeCenterY() {
        return relativeCenterY;
    }

    public ConicGradient setRelativeCenterY(float relativeCenterY) {
        this.relativeCenterY = relativeCenterY;
        invalidateRasterCache();
        return this;
    }

    @Override
    public int sampleArgb(int px, int py, int width, int height) {
        double cx = relativeCenterX * width;
        double cy = relativeCenterY * height;
        double dx = px + 0.5 - cx;
        double dy = py + 0.5 - cy;
        // CSS conic: 0 degrees at top (north), sweep clockwise.
        double theta = MathUtil.atan2(dx, -dy) - Math.toRadians(fromAngleDegrees);
        double normalized = theta / (Math.PI * 2.0);
        normalized -= Math.floor(normalized);
        return sampleStops((float) normalized);
    }

    @Override
    public ConicGradient copy() {
        int[] c = getColors();
        float[] p = getPositions();
        int[] cc = new int[c.length];
        float[] pp = new float[p.length];
        System.arraycopy(c, 0, cc, 0, c.length);
        System.arraycopy(p, 0, pp, 0, p.length);
        ConicGradient g = new ConicGradient(cc, pp);
        g.setCycleMethod(getCycleMethod());
        g.fromAngleDegrees = fromAngleDegrees;
        g.relativeCenterX = relativeCenterX;
        g.relativeCenterY = relativeCenterY;
        return g;
    }
}
