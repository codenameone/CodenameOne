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

import java.awt.image.BufferedImage;

/// A CSS `border-image` whose source is an image. Immutable, apart from the
/// image it refers to, which is never modified by this package.
public final class BorderImage {
    /// The `border-image-repeat` modes.
    public enum Mode { STRETCH, REPEAT, ROUND }

    /// The source image.
    public final BufferedImage image;
    /// The top slice offset in image px.
    public final double sliceTop;
    /// The right slice offset in image px.
    public final double sliceRight;
    /// The bottom slice offset in image px.
    public final double sliceBottom;
    /// The left slice offset in image px.
    public final double sliceLeft;
    /// Whether the middle slice is painted over the padding box.
    public final boolean fill;
    /// How the edge and middle slices cover their regions.
    public final Mode repeat;

    public BorderImage(BufferedImage image, double sliceTop, double sliceRight, double sliceBottom,
            double sliceLeft, boolean fill, Mode repeat) {
        this.image = image;
        this.sliceTop = sliceTop;
        this.sliceRight = sliceRight;
        this.sliceBottom = sliceBottom;
        this.sliceLeft = sliceLeft;
        this.fill = fill;
        this.repeat = repeat;
    }

    public BufferedImage getImage() {
        return image;
    }

    public double getSliceTop() {
        return sliceTop;
    }

    public double getSliceRight() {
        return sliceRight;
    }

    public double getSliceBottom() {
        return sliceBottom;
    }

    public double getSliceLeft() {
        return sliceLeft;
    }

    public boolean isFill() {
        return fill;
    }

    public Mode getRepeat() {
        return repeat;
    }

    /// Same as [#getRepeat()].
    public Mode getMode() {
        return repeat;
    }
}
