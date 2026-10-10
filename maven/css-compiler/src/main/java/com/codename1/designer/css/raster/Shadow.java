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

/// A single CSS `box-shadow`. Immutable.
public final class Shadow {
    /// Horizontal offset in CSS px, positive moves the shadow right.
    public final double offsetX;
    /// Vertical offset in CSS px, positive moves the shadow down.
    public final double offsetY;
    /// The CSS blur radius in px. The gaussian standard deviation is half of it.
    public final double blur;
    /// The CSS spread distance in px, may be negative.
    public final double spread;
    /// The shadow colour as non-premultiplied ARGB.
    public final int color;
    /// `true` for an `inset` shadow.
    public final boolean inset;

    public Shadow(double offsetX, double offsetY, double blur, double spread, int color, boolean inset) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.blur = blur;
        this.spread = spread;
        this.color = color;
        this.inset = inset;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getBlur() {
        return blur;
    }

    public double getSpread() {
        return spread;
    }

    public int getColor() {
        return color;
    }

    public boolean isInset() {
        return inset;
    }
}
