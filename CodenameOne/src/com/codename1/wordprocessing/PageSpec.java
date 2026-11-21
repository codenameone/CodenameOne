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
package com.codename1.wordprocessing;

/// Immutable page geometry in points: paper size and the four margins.
public final class PageSpec {
    /// ISO A4 with 0.75 inch margins.
    public static final PageSpec A4 = new PageSpec(595.28f, 841.89f, 54, 54, 54, 54);

    /// US Letter with one inch margins.
    public static final PageSpec LETTER = new PageSpec(612, 792, 72, 72, 72, 72);

    /// The paper width.
    public final float width;

    /// The paper height.
    public final float height;

    /// The top margin.
    public final float top;

    /// The right margin.
    public final float right;

    /// The bottom margin.
    public final float bottom;

    /// The left margin.
    public final float left;

    /// Creates a page.
    ///
    /// #### Parameters
    ///
    /// - `width`: the paper width, at least one inch
    ///
    /// - `height`: the paper height, at least one inch
    ///
    /// - `top`: the top margin
    ///
    /// - `right`: the right margin
    ///
    /// - `bottom`: the bottom margin
    ///
    /// - `left`: the left margin
    public PageSpec(float width, float height, float top, float right, float bottom, float left) {
        if (!ok(width) || !ok(height) || !ok(top) || !ok(right) || !ok(bottom) || !ok(left)
                || width < 72 || height < 72 || top + bottom >= height || left + right >= width) {
            throw new IllegalArgumentException("Invalid page geometry");
        }
        this.width = width;
        this.height = height;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    private static boolean ok(float x) {
        return !Float.isNaN(x) && !Float.isInfinite(x) && x >= 0;
    }

    /// The width between the margins.
    ///
    /// #### Returns
    ///
    /// the text width
    public float contentWidth() {
        return width - left - right;
    }

    /// The height between the margins.
    ///
    /// #### Returns
    ///
    /// the text height
    public float contentHeight() {
        return height - top - bottom;
    }

    /// Two pages are equal when their size and margins are.
    ///
    /// #### Parameters
    ///
    /// - `o`: the other object
    ///
    /// #### Returns
    ///
    /// true when equal
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PageSpec)) {
            return false;
        }
        PageSpec p = (PageSpec) o;
        return Float.floatToIntBits(width) == Float.floatToIntBits(p.width)
                && Float.floatToIntBits(height) == Float.floatToIntBits(p.height)
                && Float.floatToIntBits(top) == Float.floatToIntBits(p.top)
                && Float.floatToIntBits(right) == Float.floatToIntBits(p.right)
                && Float.floatToIntBits(bottom) == Float.floatToIntBits(p.bottom)
                && Float.floatToIntBits(left) == Float.floatToIntBits(p.left);
    }

    /// A hash consistent with `equals`.
    ///
    /// #### Returns
    ///
    /// the hash
    @Override
    public int hashCode() {
        return Float.floatToIntBits(width) * 31 + Float.floatToIntBits(height);
    }
}
