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
package com.codename1.desktopcompat.java.awt;

/// A pen of a given width, with caps, joins and an optional dash pattern.
///
/// A `Graphics2D` strokes with it directly: width, cap and join map onto
/// the Codename One stroke and a dash pattern is laid out along the
/// flattened path. [#createStrokedShape(Shape)] -- turning the stroke into
/// an outline to fill -- is not implemented and throws.
public class BasicStroke implements Stroke {

    public static final int JOIN_MITER = 0;
    public static final int JOIN_ROUND = 1;
    public static final int JOIN_BEVEL = 2;
    public static final int CAP_BUTT = 0;
    public static final int CAP_ROUND = 1;
    public static final int CAP_SQUARE = 2;

    private final float width;
    private final int join;
    private final int cap;
    private final float miterlimit;
    private final float[] dash;
    private final float dashPhase;

    public BasicStroke(float width, int cap, int join, float miterlimit, float[] dash, float dashPhase) {
        if (width < 0.0f) {
            throw new IllegalArgumentException("negative width");
        }
        if (cap != CAP_BUTT && cap != CAP_ROUND && cap != CAP_SQUARE) {
            throw new IllegalArgumentException("illegal end cap value");
        }
        if (join == JOIN_MITER) {
            if (miterlimit < 1.0f) {
                throw new IllegalArgumentException("miter limit < 1");
            }
        } else if (join != JOIN_ROUND && join != JOIN_BEVEL) {
            throw new IllegalArgumentException("illegal line join value");
        }
        if (dash != null) {
            if (dashPhase < 0.0f) {
                throw new IllegalArgumentException("negative dash phase");
            }
            boolean allzero = true;
            for (float d : dash) {
                if (d > 0.0) {
                    allzero = false;
                } else if (d < 0.0) {
                    throw new IllegalArgumentException("negative dash length");
                }
            }
            if (allzero) {
                throw new IllegalArgumentException("dash lengths all zero");
            }
        }
        this.width = width;
        this.cap = cap;
        this.join = join;
        this.miterlimit = miterlimit;
        this.dash = dash == null ? null : copy(dash);
        this.dashPhase = dashPhase;
    }

    public BasicStroke(float width, int cap, int join, float miterlimit) {
        this(width, cap, join, miterlimit, null, 0.0f);
    }

    public BasicStroke(float width, int cap, int join) {
        this(width, cap, join, 10.0f, null, 0.0f);
    }

    public BasicStroke(float width) {
        this(width, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
    }

    public BasicStroke() {
        this(1.0f, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
    }

    private static float[] copy(float[] in) {
        float[] out = new float[in.length];
        System.arraycopy(in, 0, out, 0, in.length);
        return out;
    }

    /// Not implemented: the layer strokes shapes itself and has no stroker
    /// that returns the outline.
    @Override
    public Shape createStrokedShape(Shape s) {
        throw new UnsupportedOperationException(
                "BasicStroke.createStrokedShape is not supported; draw the shape with Graphics2D.draw instead");
    }

    public float getLineWidth() {
        return width;
    }

    public int getEndCap() {
        return cap;
    }

    public int getLineJoin() {
        return join;
    }

    public float getMiterLimit() {
        return miterlimit;
    }

    public float[] getDashArray() {
        return dash == null ? null : copy(dash);
    }

    public float getDashPhase() {
        return dashPhase;
    }

    @Override
    public int hashCode() {
        int hash = Float.floatToIntBits(width);
        hash = hash * 31 + join;
        hash = hash * 31 + cap;
        hash = hash * 31 + Float.floatToIntBits(miterlimit);
        if (dash != null) {
            hash = hash * 31 + Float.floatToIntBits(dashPhase);
            for (float d : dash) {
                hash = hash * 31 + Float.floatToIntBits(d);
            }
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BasicStroke)) {
            return false;
        }
        BasicStroke bs = (BasicStroke) obj;
        if (Float.floatToIntBits(width) != Float.floatToIntBits(bs.width) || join != bs.join || cap != bs.cap
                || Float.floatToIntBits(miterlimit) != Float.floatToIntBits(bs.miterlimit)) {
            return false;
        }
        if (dash == null || bs.dash == null) {
            return dash == null && bs.dash == null;
        }
        if (Float.floatToIntBits(dashPhase) != Float.floatToIntBits(bs.dashPhase) || dash.length != bs.dash.length) {
            return false;
        }
        for (int i = 0; i < dash.length; i++) {
            if (Float.floatToIntBits(dash[i]) != Float.floatToIntBits(bs.dash[i])) {
                return false;
            }
        }
        return true;
    }
}
