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

/// One side of a CSS border. Immutable.
public final class BorderSide {
    /// A side that paints nothing: zero width, style `NONE`.
    public static final BorderSide NONE = new BorderSide(0, BorderStyle.NONE, 0);

    /// The border width in CSS px.
    public final double width;
    /// The border style.
    public final BorderStyle style;
    /// The border colour as non-premultiplied ARGB.
    public final int color;

    /// Creates a side.
    ///
    /// #### Parameters
    ///
    /// - `width`: the width in CSS px
    ///
    /// - `style`: the style
    ///
    /// - `color`: non-premultiplied ARGB
    public BorderSide(double width, BorderStyle style, int color) {
        this.width = width;
        this.style = style;
        this.color = color;
    }

    public double getWidth() {
        return width;
    }

    public BorderStyle getStyle() {
        return style;
    }

    public int getColor() {
        return color;
    }

    /// The width this side occupies in the layout: its width when the style
    /// is visible, and zero for `NONE` and `HIDDEN` as CSS computes it.
    public double effectiveWidth() {
        if (style == null || !style.isVisible() || !(width > 0)) {
            return 0;
        }
        return width;
    }
}
