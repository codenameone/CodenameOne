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
package com.codename1.presentation;

import com.codename1.wordprocessing.Picture;

/// How the inside of a shape or a slide's background is painted: nothing, one color, a
/// two-color linear gradient, or a picture stretched over the area.
public final class Fill {
    /// Nothing is painted; whatever is behind shows through.
    public static final int NONE = 0;

    /// One color.
    public static final int SOLID = 1;

    /// A linear gradient from `color` to `color2` along `angle`.
    public static final int GRADIENT = 2;

    /// A picture stretched over the area.
    public static final int PICTURE = 3;

    /// No fill.
    public static final Fill NO_FILL = new Fill(NONE, 0, 0, 0, 1, null);

    /// The kind: `NONE`, `SOLID`, `GRADIENT` or `PICTURE`.
    public final int kind;

    /// The color as 0xRRGGBB, the gradient's start.
    public final int color;

    /// The gradient's end color as 0xRRGGBB.
    public final int color2;

    /// The gradient's direction in degrees: 0 runs left to right, 90 top to bottom.
    public final float angle;

    /// The opacity, from 0 (invisible) to 1 (opaque).
    public final float alpha;

    /// The picture of a `PICTURE` fill, or null.
    public final Picture picture;

    private Fill(int kind, int color, int color2, float angle, float alpha, Picture picture) {
        this.kind = kind;
        this.color = color & 0xffffff;
        this.color2 = color2 & 0xffffff;
        this.angle = angle;
        this.alpha = alpha < 0 ? 0 : alpha > 1 ? 1 : alpha;
        this.picture = picture;
    }

    /// An opaque solid fill.
    ///
    /// #### Parameters
    ///
    /// - `color`: 0xRRGGBB
    ///
    /// #### Returns
    ///
    /// the fill
    public static Fill solid(int color) {
        return new Fill(SOLID, color, color, 0, 1, null);
    }

    /// A linear gradient.
    ///
    /// #### Parameters
    ///
    /// - `from`: the start color, 0xRRGGBB
    ///
    /// - `to`: the end color
    ///
    /// - `angle`: degrees; 0 runs left to right, 90 top to bottom
    ///
    /// #### Returns
    ///
    /// the fill
    public static Fill gradient(int from, int to, float angle) {
        return new Fill(GRADIENT, from, to, angle, 1, null);
    }

    /// A picture fill.
    ///
    /// #### Parameters
    ///
    /// - `picture`: the picture, not null
    ///
    /// #### Returns
    ///
    /// the fill
    public static Fill picture(Picture picture) {
        if (picture == null) {
            throw new IllegalArgumentException("A picture fill needs a picture");
        }
        return new Fill(PICTURE, 0xffffff, 0xffffff, 0, 1, picture);
    }

    /// This fill at another opacity.
    ///
    /// #### Parameters
    ///
    /// - `value`: 0 to 1
    ///
    /// #### Returns
    ///
    /// the fill
    public Fill withAlpha(float value) {
        return new Fill(kind, color, color2, angle, value, picture);
    }

    /// True when the fill paints something.
    ///
    /// #### Returns
    ///
    /// true unless the fill is `NONE` or fully transparent
    public boolean isVisible() {
        return kind != NONE && alpha > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Fill)) {
            return false;
        }
        Fill f = (Fill) o;
        return kind == f.kind && color == f.color && color2 == f.color2 && angle == f.angle
                && alpha == f.alpha && (picture == null ? f.picture == null
                : picture.equals(f.picture));
    }

    @Override
    public int hashCode() {
        return ((kind * 31 + color) * 31 + color2) * 31 + Float.floatToIntBits(angle)
                + (picture == null ? 0 : picture.hashCode());
    }

    @Override
    public String toString() {
        return "Fill[" + kind + " #" + Integer.toHexString(color) + "]";
    }
}
