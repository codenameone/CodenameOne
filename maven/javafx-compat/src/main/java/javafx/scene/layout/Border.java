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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.NamedArg;
import javafx.geometry.Insets;

/// The border of a region: strokes drawn one above the other. A border
/// takes room: its widths are part of the region's insets. Border images
/// are not part of this layer.
public final class Border {

    /// A border that draws nothing and takes no room.
    public static final Border EMPTY = new Border((BorderStroke[]) null);

    private final List<BorderStroke> strokes;
    private final List<BorderImage> images;
    private final Insets insets;
    private final Insets outsets;

    /// Creates a border from strokes; `null` entries are ignored.
    public Border(@NamedArg("strokes") BorderStroke... strokes) {
        this(strokes, (BorderImage[]) null);
    }

    /// Creates a border of pictures and no strokes, bottom first.
    public Border(@NamedArg("images") BorderImage... images) {
        this((BorderStroke[]) null, images);
    }

    /// Creates a border of strokes with pictures over them, each list
    /// bottom first; `null` entries are left out.
    public Border(@NamedArg("strokes") List<BorderStroke> strokes, @NamedArg("images") List<BorderImage> images) {
        this(strokes == null ? null : strokes.toArray(new BorderStroke[0]),
                images == null ? null : images.toArray(new BorderImage[0]));
    }

    /// Creates a border of strokes with pictures over them, each array
    /// bottom first; `null` entries are left out.
    public Border(@NamedArg("strokes") BorderStroke[] strokes, @NamedArg("images") BorderImage[] images) {
        ArrayList<BorderStroke> list = new ArrayList<BorderStroke>();
        ArrayList<BorderImage> pictures = new ArrayList<BorderImage>();
        double top = 0;
        double right = 0;
        double bottom = 0;
        double left = 0;
        if (images != null) {
            for (int i = 0; i < images.length; i++) {
                BorderImage image = images[i];
                if (image == null) {
                    continue;
                }
                pictures.add(image);
                // A width that is a share of the region cannot be room
                // taken from it before the region has a size.
                Insets in = image.getInsets();
                BorderWidths w = image.getWidths();
                top = Math.max(top, in.getTop() + (w.isTopAsPercentage() ? 0 : Math.max(0, w.getTop())));
                right = Math.max(right, in.getRight() + (w.isRightAsPercentage() ? 0 : Math.max(0, w.getRight())));
                bottom = Math.max(bottom,
                        in.getBottom() + (w.isBottomAsPercentage() ? 0 : Math.max(0, w.getBottom())));
                left = Math.max(left, in.getLeft() + (w.isLeftAsPercentage() ? 0 : Math.max(0, w.getLeft())));
            }
        }
        this.images = Collections.unmodifiableList(pictures);
        double outTop = 0;
        double outRight = 0;
        double outBottom = 0;
        double outLeft = 0;
        if (strokes != null) {
            for (int i = 0; i < strokes.length; i++) {
                BorderStroke stroke = strokes[i];
                if (stroke == null) {
                    continue;
                }
                list.add(stroke);
                Insets in = stroke.getInsets();
                BorderWidths w = stroke.getWidths();
                top = Math.max(top, in.getTop() + side(w.getTop(), stroke.getTopStyle()));
                right = Math.max(right, in.getRight() + side(w.getRight(), stroke.getRightStyle()));
                bottom = Math.max(bottom, in.getBottom() + side(w.getBottom(), stroke.getBottomStyle()));
                left = Math.max(left, in.getLeft() + side(w.getLeft(), stroke.getLeftStyle()));
                outTop = Math.max(outTop, -in.getTop());
                outRight = Math.max(outRight, -in.getRight());
                outBottom = Math.max(outBottom, -in.getBottom());
                outLeft = Math.max(outLeft, -in.getLeft());
            }
        }
        this.strokes = Collections.unmodifiableList(list);
        this.insets = new Insets(Math.max(0, top), Math.max(0, right), Math.max(0, bottom), Math.max(0, left));
        this.outsets = new Insets(outTop, outRight, outBottom, outLeft);
    }

    private static double side(double width, BorderStrokeStyle style) {
        if (style == BorderStrokeStyle.NONE) {
            return 0;
        }
        return width == BorderWidths.AUTO ? 1 : width;
    }

    /// Returns the strokes, bottom first; unmodifiable.
    public final List<BorderStroke> getStrokes() {
        return strokes;
    }

    /// Returns the room the border takes inside the region.
    public final Insets getInsets() {
        return insets;
    }

    /// Returns how far the border reaches outside the region.
    public final Insets getOutsets() {
        return outsets;
    }

    /// Returns whether there is nothing to draw.
    public final boolean isEmpty() {
        return strokes.isEmpty() && images.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Border && strokes.equals(((Border) o).strokes)
                && images.equals(((Border) o).images);
    }

    @Override
    public int hashCode() {
        return strokes.hashCode();
    }

    /// Returns the pictures, bottom first; unmodifiable. They are drawn
    /// over the strokes.
    public final List<BorderImage> getImages() {
        return images;
    }
}
