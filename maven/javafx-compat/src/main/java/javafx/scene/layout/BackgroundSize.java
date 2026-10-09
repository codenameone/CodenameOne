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
package javafx.scene.layout;

import javafx.beans.NamedArg;

/// The size a background image is drawn at. A width or height is a
/// length, a share of the region written 0 to 1, or [#AUTO]: the size of
/// the picture, or what keeps its shape when the other is given.
/// `contain` and `cover` overrule both: the largest size that fits whole,
/// and the smallest that leaves nothing bare.
public final class BackgroundSize {

    /// The size the picture has, or the one that keeps its shape.
    public static final double AUTO = -1;

    /// The picture at its own size.
    public static final BackgroundSize DEFAULT = new BackgroundSize(AUTO, AUTO, true, true, false, false);

    private final double width;
    private final double height;
    private final boolean widthAsPercentage;
    private final boolean heightAsPercentage;
    private final boolean contain;
    private final boolean cover;

    /// Creates a size.
    public BackgroundSize(@NamedArg("width") double width, @NamedArg("height") double height,
            @NamedArg("widthAsPercentage") boolean widthAsPercentage,
            @NamedArg("heightAsPercentage") boolean heightAsPercentage, @NamedArg("contain") boolean contain,
            @NamedArg("cover") boolean cover) {
        if (width < 0 && width != AUTO) {
            throw new IllegalArgumentException("Width cannot be < 0, except when AUTO");
        }
        if (height < 0 && height != AUTO) {
            throw new IllegalArgumentException("Height cannot be < 0, except when AUTO");
        }
        this.width = width;
        this.height = height;
        this.widthAsPercentage = widthAsPercentage;
        this.heightAsPercentage = heightAsPercentage;
        this.contain = contain;
        this.cover = cover;
    }

    /// Returns the width, or [#AUTO].
    public final double getWidth() {
        return width;
    }

    /// Returns the height, or [#AUTO].
    public final double getHeight() {
        return height;
    }

    /// Returns whether the width is a share of the region, 0 to 1.
    public final boolean isWidthAsPercentage() {
        return widthAsPercentage;
    }

    /// Returns whether the height is a share of the region, 0 to 1.
    public final boolean isHeightAsPercentage() {
        return heightAsPercentage;
    }

    /// Returns whether the picture is as large as fits whole.
    public final boolean isContain() {
        return contain;
    }

    /// Returns whether the picture is as small as covers the region.
    public final boolean isCover() {
        return cover;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BackgroundSize)) {
            return false;
        }
        BackgroundSize s = (BackgroundSize) o;
        return width == s.width && height == s.height && widthAsPercentage == s.widthAsPercentage
                && heightAsPercentage == s.heightAsPercentage && contain == s.contain && cover == s.cover;
    }

    @Override
    public int hashCode() {
        return (int) (width * 31 + height * 17) + (widthAsPercentage ? 1 : 0) + (heightAsPercentage ? 2 : 0)
                + (contain ? 4 : 0) + (cover ? 8 : 0);
    }
}
