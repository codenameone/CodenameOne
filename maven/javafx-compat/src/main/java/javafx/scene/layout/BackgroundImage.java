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
import javafx.scene.image.Image;

/// A picture drawn behind the content of a region, over its fills: where
/// it is placed, at what size, and how it fills the rest of the region.
public final class BackgroundImage {

    private final Image image;
    private final BackgroundRepeat repeatX;
    private final BackgroundRepeat repeatY;
    private final BackgroundPosition position;
    private final BackgroundSize size;

    /// Creates a background image. A `null` repeat is `REPEAT`, a `null`
    /// position the top left corner and a `null` size the picture's own.
    public BackgroundImage(@NamedArg("image") Image image, @NamedArg("repeatX") BackgroundRepeat repeatX,
            @NamedArg("repeatY") BackgroundRepeat repeatY, @NamedArg("position") BackgroundPosition position,
            @NamedArg("size") BackgroundSize size) {
        if (image == null) {
            throw new NullPointerException("Image cannot be null");
        }
        this.image = image;
        this.repeatX = repeatX == null ? BackgroundRepeat.REPEAT : repeatX;
        this.repeatY = repeatY == null ? BackgroundRepeat.REPEAT : repeatY;
        this.position = position == null ? BackgroundPosition.DEFAULT : position;
        this.size = size == null ? BackgroundSize.DEFAULT : size;
    }

    /// Returns the picture.
    public final Image getImage() {
        return image;
    }

    /// Returns how the picture fills the width.
    public final BackgroundRepeat getRepeatX() {
        return repeatX;
    }

    /// Returns how the picture fills the height.
    public final BackgroundRepeat getRepeatY() {
        return repeatY;
    }

    /// Returns where the picture is placed.
    public final BackgroundPosition getPosition() {
        return position;
    }

    /// Returns the size the picture is drawn at.
    public final BackgroundSize getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BackgroundImage)) {
            return false;
        }
        BackgroundImage b = (BackgroundImage) o;
        return image == b.image && repeatX == b.repeatX && repeatY == b.repeatY && position.equals(b.position)
                && size.equals(b.size);
    }

    @Override
    public int hashCode() {
        return position.hashCode() * 7 + size.hashCode() + repeatX.hashCode() + repeatY.hashCode() * 3;
    }
}
