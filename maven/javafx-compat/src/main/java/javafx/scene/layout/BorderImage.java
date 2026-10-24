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
import javafx.geometry.Insets;
import javafx.scene.image.Image;

/// A picture drawn as the border of a region. Four cuts, one from each
/// edge of the picture, divide it into nine pieces: the corners are drawn
/// in the corners of the border, the four sides along its sides, stretched
/// or repeated, and the middle over the inside when the border is filled.
public class BorderImage {

    private final Image image;
    private final BorderRepeat repeatX;
    private final BorderRepeat repeatY;
    private final BorderWidths widths;
    private final BorderWidths slices;
    private final boolean filled;
    private final Insets insets;
    private com.codename1.ui.Image[] pieces;

    /// Creates a border image. `widths` are the widths of the border the
    /// pieces are drawn into, `slices` the cuts into the picture and
    /// `insets` how far inside the region the border starts; `null` is a
    /// width of one, a cut of one and no insets, and a `null` repeat is
    /// `STRETCH`.
    public BorderImage(@NamedArg("image") Image image, @NamedArg("widths") BorderWidths widths,
            @NamedArg("insets") Insets insets, @NamedArg("slices") BorderWidths slices,
            @NamedArg("filled") boolean filled, @NamedArg("repeatX") BorderRepeat repeatX,
            @NamedArg("repeatY") BorderRepeat repeatY) {
        if (image == null) {
            throw new NullPointerException("Image cannot be null");
        }
        this.image = image;
        this.widths = widths == null ? BorderWidths.DEFAULT : widths;
        this.insets = insets == null ? Insets.EMPTY : insets;
        this.slices = slices == null ? BorderWidths.DEFAULT : slices;
        this.filled = filled;
        this.repeatX = repeatX == null ? BorderRepeat.STRETCH : repeatX;
        this.repeatY = repeatY == null ? this.repeatX : repeatY;
    }

    /// Returns the picture.
    public final Image getImage() {
        return image;
    }

    /// Returns how the top and bottom sides are filled.
    public final BorderRepeat getRepeatX() {
        return repeatX;
    }

    /// Returns how the left and right sides are filled.
    public final BorderRepeat getRepeatY() {
        return repeatY;
    }

    /// Returns the widths of the border the pieces are drawn into.
    public final BorderWidths getWidths() {
        return widths;
    }

    /// Returns the cuts into the picture, from each of its edges.
    public final BorderWidths getSlices() {
        return slices;
    }

    /// Returns whether the middle piece is drawn over the inside.
    public final boolean isFilled() {
        return filled;
    }

    /// Returns how far inside the region the border starts.
    public final Insets getInsets() {
        return insets;
    }

    /// The nine pieces once cut, row by row; an entry is `null` for a
    /// piece of no size.
    com.codename1.ui.Image[] pieces() {
        return pieces;
    }

    /// Keeps the nine pieces, which are cut once.
    void pieces(com.codename1.ui.Image[] cut) {
        pieces = cut;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BorderImage)) {
            return false;
        }
        BorderImage b = (BorderImage) o;
        return image == b.image && repeatX == b.repeatX && repeatY == b.repeatY && widths.equals(b.widths)
                && slices.equals(b.slices) && filled == b.filled && insets.equals(b.insets);
    }

    @Override
    public int hashCode() {
        return widths.hashCode() * 7 + slices.hashCode() * 3 + insets.hashCode() + (filled ? 1 : 0)
                + repeatX.hashCode() + repeatY.hashCode();
    }
}
