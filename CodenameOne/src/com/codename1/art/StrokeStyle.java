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

/// How a shape's outline is drawn: paint, width, caps, joins, dashes and alignment.
public final class StrokeStyle {
    /// Flat ends at the path's end points.
    public static final int CAP_BUTT = 0;
    /// Rounded ends.
    public static final int CAP_ROUND = 1;
    /// Square ends that extend half the width past the end points.
    public static final int CAP_SQUARE = 2;
    /// Sharp corners, cut to a bevel past the miter limit.
    public static final int JOIN_MITER = 0;
    /// Rounded corners.
    public static final int JOIN_ROUND = 1;
    /// Cut corners.
    public static final int JOIN_BEVEL = 2;
    /// The stroke is centered on the path.
    public static final int ALIGN_CENTER = 0;
    /// The stroke lies inside the shape.
    public static final int ALIGN_INSIDE = 1;
    /// The stroke lies outside the shape.
    public static final int ALIGN_OUTSIDE = 2;

    private ArtPaint paint;
    private float width = 1;
    private int cap = CAP_BUTT;
    private int join = JOIN_MITER;
    private float miterLimit = 4;
    private float[] dash;
    private float dashOffset;
    private int alignment = ALIGN_CENTER;
    private boolean enabled = true;

    /// Creates a stroke.
    ///
    /// #### Parameters
    ///
    /// - `paint`: what fills the outline
    /// - `width`: width in the shape's units
    public StrokeStyle(ArtPaint paint, float width) {
        this.paint = paint;
        this.width = width;
    }

    /// A deep copy.
    public StrokeStyle copy() {
        StrokeStyle s = new StrokeStyle(paint == null ? null : paint.copy(), width);
        s.cap = cap;
        s.join = join;
        s.miterLimit = miterLimit;
        if (dash != null) {
            s.dash = new float[dash.length];
            System.arraycopy(dash, 0, s.dash, 0, dash.length);
        }
        s.dashOffset = dashOffset;
        s.alignment = alignment;
        s.enabled = enabled;
        return s;
    }

    /// The paint.
    public ArtPaint getPaint() {
        return paint;
    }

    /// Sets the paint.
    ///
    /// #### Parameters
    ///
    /// - `paint`: the new paint
    public void setPaint(ArtPaint paint) {
        this.paint = paint;
    }

    /// The width.
    public float getWidth() {
        return width;
    }

    /// Sets the width.
    ///
    /// #### Parameters
    ///
    /// - `width`: the new width
    public void setWidth(float width) {
        this.width = width;
    }

    /// One of the `CAP_` constants.
    public int getCap() {
        return cap;
    }

    /// Sets the cap.
    ///
    /// #### Parameters
    ///
    /// - `cap`: one of the `CAP_` constants
    public void setCap(int cap) {
        this.cap = cap;
    }

    /// One of the `JOIN_` constants.
    public int getJoin() {
        return join;
    }

    /// Sets the join.
    ///
    /// #### Parameters
    ///
    /// - `join`: one of the `JOIN_` constants
    public void setJoin(int join) {
        this.join = join;
    }

    /// The miter limit.
    public float getMiterLimit() {
        return miterLimit;
    }

    /// Sets the miter limit.
    ///
    /// #### Parameters
    ///
    /// - `miterLimit`: the ratio past which a miter becomes a bevel
    public void setMiterLimit(float miterLimit) {
        this.miterLimit = miterLimit;
    }

    /// The dash pattern, or null for a solid line.
    public float[] getDash() {
        return dash;
    }

    /// Sets the dash pattern.
    ///
    /// #### Parameters
    ///
    /// - `dash`: alternating on and off lengths; null for solid
    public void setDash(float[] dash) {
        this.dash = dash;
    }

    /// The dash offset.
    public float getDashOffset() {
        return dashOffset;
    }

    /// Sets the dash offset.
    ///
    /// #### Parameters
    ///
    /// - `dashOffset`: how far into the pattern the line starts
    public void setDashOffset(float dashOffset) {
        this.dashOffset = dashOffset;
    }

    /// One of the `ALIGN_` constants.
    public int getAlignment() {
        return alignment;
    }

    /// Sets the alignment.
    ///
    /// #### Parameters
    ///
    /// - `alignment`: one of the `ALIGN_` constants
    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    /// False when the stroke is kept but not drawn, as Sketch allows.
    public boolean isEnabled() {
        return enabled;
    }

    /// Turns the stroke on or off.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: true to draw it
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
