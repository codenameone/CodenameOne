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
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/// One layer of a background: a paint, the corner radii of the area it
/// fills and how far that area is inset from the edges of the region.
public final class BackgroundFill {

    private final Paint fill;
    private final CornerRadii radii;
    private final Insets insets;

    /// Creates a fill; a missing paint is transparent, missing radii are
    /// square and missing insets are none.
    public BackgroundFill(@NamedArg("fill") Paint fill, @NamedArg("radii") CornerRadii radii,
            @NamedArg("insets") Insets insets) {
        this.fill = fill == null ? Color.TRANSPARENT : fill;
        this.radii = radii == null ? CornerRadii.EMPTY : radii;
        this.insets = insets == null ? Insets.EMPTY : insets;
    }

    /// Returns the paint.
    public final Paint getFill() {
        return fill;
    }

    /// Returns the corner radii.
    public final CornerRadii getRadii() {
        return radii;
    }

    /// Returns the insets from the edges of the region.
    public final Insets getInsets() {
        return insets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BackgroundFill)) {
            return false;
        }
        BackgroundFill that = (BackgroundFill) o;
        return fill.equals(that.fill) && insets.equals(that.insets) && radii.equals(that.radii);
    }

    @Override
    public int hashCode() {
        return (fill.hashCode() * 31 + radii.hashCode()) * 31 + insets.hashCode();
    }
}
