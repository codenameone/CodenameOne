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

/// The background of a region: fills painted one above the other, the
/// first one at the bottom. Background images are not part of this layer.
public final class Background {

    /// A background that paints nothing.
    public static final Background EMPTY = new Background((BackgroundFill[]) null);

    private final List<BackgroundFill> fills;
    private final List<BackgroundImage> images;
    private final Insets outsets;

    /// Creates a background from fills; `null` entries are ignored.
    public Background(@NamedArg("fills") BackgroundFill... fills) {
        this(fills, (BackgroundImage[]) null);
    }

    /// Creates a background of pictures and no fills, bottom first.
    public Background(@NamedArg("images") BackgroundImage... images) {
        this((BackgroundFill[]) null, images);
    }

    /// Creates a background of fills with pictures over them, each list
    /// bottom first; `null` entries are left out.
    public Background(@NamedArg("fills") List<BackgroundFill> fills,
            @NamedArg("images") List<BackgroundImage> images) {
        this(fills == null ? null : fills.toArray(new BackgroundFill[0]),
                images == null ? null : images.toArray(new BackgroundImage[0]));
    }

    /// Creates a background of fills with pictures over them, each array
    /// bottom first; `null` entries are left out.
    public Background(@NamedArg("fills") BackgroundFill[] fills, @NamedArg("images") BackgroundImage[] images) {
        ArrayList<BackgroundImage> pictures = new ArrayList<BackgroundImage>();
        if (images != null) {
            for (int i = 0; i < images.length; i++) {
                if (images[i] != null) {
                    pictures.add(images[i]);
                }
            }
        }
        this.images = Collections.unmodifiableList(pictures);
        ArrayList<BackgroundFill> list = new ArrayList<BackgroundFill>();
        double top = 0;
        double right = 0;
        double bottom = 0;
        double left = 0;
        if (fills != null) {
            for (int i = 0; i < fills.length; i++) {
                BackgroundFill fill = fills[i];
                if (fill != null) {
                    list.add(fill);
                    Insets in = fill.getInsets();
                    top = Math.max(top, -in.getTop());
                    right = Math.max(right, -in.getRight());
                    bottom = Math.max(bottom, -in.getBottom());
                    left = Math.max(left, -in.getLeft());
                }
            }
        }
        this.fills = Collections.unmodifiableList(list);
        this.outsets = new Insets(top, right, bottom, left);
    }

    /// Returns the fills, bottom first; unmodifiable.
    public final List<BackgroundFill> getFills() {
        return fills;
    }

    /// Returns how far the background reaches outside the region.
    public final Insets getOutsets() {
        return outsets;
    }

    /// Returns whether there is nothing to paint.
    public final boolean isEmpty() {
        return fills.isEmpty() && images.isEmpty();
    }

    /// Returns whether any fill has corner radii given as a fraction of
    /// the size.
    public boolean isFillPercentageBased() {
        for (int i = 0; i < fills.size(); i++) {
            if (fills.get(i).getRadii().isTopLeftHorizontalRadiusAsPercentage()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Background && fills.equals(((Background) o).fills)
                && images.equals(((Background) o).images);
    }

    @Override
    public int hashCode() {
        return fills.hashCode();
    }

    /// Returns the pictures, bottom first; unmodifiable. They are drawn
    /// over the fills.
    public final List<BackgroundImage> getImages() {
        return images;
    }
}
