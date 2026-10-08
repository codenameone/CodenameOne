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
package com.codename1.desktopcompat.org.jdesktop.swingx.painter;

import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Rectangle;

/// A painter whose content has a size of its own and is placed inside the
/// painted area: aligned to an edge or centered, kept off the edges by
/// insets, or stretched to fill.
public abstract class AbstractLayoutPainter<T> extends AbstractPainter<T> {

    /// Where the content sits from left to right.
    public enum HorizontalAlignment {
        LEFT,
        CENTER,
        RIGHT
    }

    /// Where the content sits from top to bottom.
    public enum VerticalAlignment {
        TOP,
        CENTER,
        BOTTOM
    }

    private VerticalAlignment verticalAlignment = VerticalAlignment.CENTER;
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.CENTER;
    private Insets insets = new Insets(0, 0, 0, 0);
    private boolean fillVertical;
    private boolean fillHorizontal;

    public AbstractLayoutPainter() {
    }

    public HorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    /// A copy of the insets.
    public Insets getInsets() {
        return new Insets(insets.top, insets.left, insets.bottom, insets.right);
    }

    public VerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
    }

    public boolean isFillHorizontal() {
        return fillHorizontal;
    }

    public boolean isFillVertical() {
        return fillVertical;
    }

    public void setHorizontalAlignment(HorizontalAlignment horizontal) {
        HorizontalAlignment old = horizontalAlignment;
        horizontalAlignment = horizontal == null ? HorizontalAlignment.CENTER : horizontal;
        setDirty(true);
        firePropertyChange("horizontalAlignment", old, horizontalAlignment);
    }

    /// Whether the content takes the whole width inside the insets.
    public void setFillHorizontal(boolean fillHorizontal) {
        boolean old = this.fillHorizontal;
        this.fillHorizontal = fillHorizontal;
        setDirty(true);
        firePropertyChange("fillHorizontal", Boolean.valueOf(old), Boolean.valueOf(fillHorizontal));
    }

    /// Sets the distance the content keeps from the edges; `null` is none.
    public void setInsets(Insets insets) {
        Insets old = this.insets;
        this.insets = insets == null ? new Insets(0, 0, 0, 0)
                : new Insets(insets.top, insets.left, insets.bottom, insets.right);
        setDirty(true);
        firePropertyChange("insets", old, getInsets());
    }

    public void setVerticalAlignment(VerticalAlignment vertical) {
        VerticalAlignment old = verticalAlignment;
        verticalAlignment = vertical == null ? VerticalAlignment.CENTER : vertical;
        setDirty(true);
        firePropertyChange("verticalAlignment", old, verticalAlignment);
    }

    /// Whether the content takes the whole height inside the insets.
    public void setFillVertical(boolean verticalStretch) {
        boolean old = this.fillVertical;
        this.fillVertical = verticalStretch;
        setDirty(true);
        firePropertyChange("fillVertical", Boolean.valueOf(old), Boolean.valueOf(verticalStretch));
    }

    /// The rectangle content of the given size takes in an area of
    /// `width` by `height`: placed by the alignments and insets, and as
    /// wide or tall as the area inside the insets along an axis that is
    /// filled.
    protected final Rectangle calculateLayout(int contentWidth, int contentHeight, int width, int height) {
        Rectangle r = new Rectangle(0, 0, contentWidth, contentHeight);
        if (horizontalAlignment == HorizontalAlignment.LEFT) {
            r.x = insets.left;
        } else if (horizontalAlignment == HorizontalAlignment.RIGHT) {
            r.x = width - contentWidth - insets.right;
        } else {
            r.x = (width - contentWidth) / 2 + insets.left - insets.right;
        }
        if (verticalAlignment == VerticalAlignment.TOP) {
            r.y = insets.top;
        } else if (verticalAlignment == VerticalAlignment.BOTTOM) {
            r.y = height - contentHeight - insets.bottom;
        } else {
            r.y = (height - contentHeight) / 2 + insets.top - insets.bottom;
        }
        if (fillHorizontal) {
            r.x = insets.left;
            r.width = width - insets.left - insets.right;
        }
        if (fillVertical) {
            r.y = insets.top;
            r.height = height - insets.top - insets.bottom;
        }
        return r;
    }
}
