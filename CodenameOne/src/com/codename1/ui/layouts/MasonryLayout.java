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
package com.codename1.ui.layouts;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

/// Arranges components in equal width columns, putting each one at the top of
/// whichever column is shortest so far: a masonry, staggered or Pinterest-style
/// grid, where tiles keep their own heights and the columns do not line up.
///
/// ```java
/// Container feed = new Container(new MasonryLayout(2));
/// for (Photo p : photos) {
///     feed.add(new ScaleImageLabel(p.image()));   // tiles of any height
/// }
/// feed.setScrollableY(true);
/// ```
///
/// Each component is as wide as its column and as tall as it prefers at that
/// width. Order is kept in the sense that every component is placed after the
/// ones before it, so the top of the grid reads in order. `TableLayout` and
/// `GridLayout` differ by aligning every row to its tallest cell.
///
/// `#setMinimumColumnWidth(float)` makes the column count follow the width
/// instead: as many columns as fit, never fewer than the count given.
public class MasonryLayout extends Layout {
    private int columns;
    private float minColumnWidthMM;
    private float gapMM = 1f;

    /// A layout with two columns.
    public MasonryLayout() {
        this(2);
    }

    /// A layout with a fixed number of columns.
    ///
    /// #### Parameters
    ///
    /// - `columns`: the column count, at least 1
    public MasonryLayout(int columns) {
        this.columns = Math.max(1, columns);
    }

    /// The column count, or the minimum when columns follow the width.
    ///
    /// #### Returns
    ///
    /// the column count
    public int getColumns() {
        return columns;
    }

    /// Sets the column count.
    ///
    /// #### Parameters
    ///
    /// - `columns`: at least 1
    public void setColumns(int columns) {
        this.columns = Math.max(1, columns);
    }

    /// Makes the column count follow the width: as many columns at least this
    /// wide as fit, and never fewer than `#getColumns()`.
    ///
    /// #### Parameters
    ///
    /// - `mm`: the narrowest column in millimetres, or 0 for a fixed count
    public void setMinimumColumnWidth(float mm) {
        minColumnWidthMM = Math.max(0, mm);
    }

    /// The narrowest column when columns follow the width.
    ///
    /// #### Returns
    ///
    /// millimetres, 0 for a fixed count
    public float getMinimumColumnWidth() {
        return minColumnWidthMM;
    }

    /// The space between columns and between tiles of a column.
    ///
    /// #### Returns
    ///
    /// millimetres, 1 by default
    public float getGap() {
        return gapMM;
    }

    /// Sets the space between columns and between tiles of a column, added to
    /// the components' own margins.
    ///
    /// #### Parameters
    ///
    /// - `mm`: millimetres
    public void setGap(float mm) {
        gapMM = Math.max(0, mm);
    }

    /// The number of columns used at a given content width.
    ///
    /// #### Parameters
    ///
    /// - `width`: the width available, in pixels
    ///
    /// #### Returns
    ///
    /// the column count
    public int columnsFor(int width) {
        if (minColumnWidthMM <= 0) {
            return columns;
        }
        int gap = CN.convertToPixels(gapMM);
        int min = Math.max(1, CN.convertToPixels(minColumnWidthMM));
        return Math.max(columns, (width + gap) / (min + gap));
    }

    @Override
    public void layoutContainer(Container parent) {
        Style s = parent.getStyle();
        int left = s.getPaddingLeftNoRTL();
        int width = parent.getLayoutWidth() - parent.getSideGap() - s.getHorizontalPadding();
        place(parent, left, s.getPaddingTop(), width, true);
    }

    @Override
    public Dimension getPreferredSize(Container parent) {
        Style s = parent.getStyle();
        int widest = 0;
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component c = parent.getComponentAt(i);
            if (!c.isHidden()) {
                widest = Math.max(widest, c.getPreferredW() + c.getStyle().getHorizontalMargins());
            }
        }
        int gap = CN.convertToPixels(gapMM);
        // The width asked for comes from the tiles, never from the width the
        // container happens to have, so it can shrink as well as grow.
        int natural = columns * widest + (columns - 1) * gap;
        // The height is measured at the width the container really has once
        // it has one, since tiles are as tall as they are at their column width.
        int actual = parent.getWidth() - parent.getSideGap() - s.getHorizontalPadding();
        int width = actual > 0 ? actual : natural;
        return new Dimension(natural + s.getHorizontalPadding(),
                place(parent, 0, 0, width, false) + s.getVerticalPadding());
    }

    private static void invalidate(Component c) {
        c.setShouldCalcPreferredSize(true);
        if (c instanceof Container) {
            Container k = (Container) c;
            for (int i = 0; i < k.getComponentCount(); i++) {
                invalidate(k.getComponentAt(i));
            }
        }
    }

    /// The height of a tile at a given width. The component is given that width
    /// first, so text wraps to it; an image label that scales its image is
    /// scaled to it keeping its aspect ratio.
    private static int heightAt(Component c, int w) {
        if (c.getWidth() != w) {
            c.setWidth(w);
            if (c instanceof Container) {
                // A card's text sits in its children; lay them out at the new
                // width first, then drop the sizes they cached at the old one,
                // so a SpanLabel inside measures at its own width.
                // layoutContainer() is a no-op unless the tile is marked for
                // layout, which a width change alone does not do; invalidating
                // its children marks it (Component.setShouldCalcPreferredSize).
                invalidate(c);
                ((Container) c).layoutContainer();
                invalidate(c);
            }
            // Only on a change, so measuring converges instead of relaying out forever.
            c.setShouldCalcPreferredSize(true);
        }
        int h = c.getPreferredH();
        int pw = c.getPreferredW();
        // An image label reports its image's full size and scales it to whatever
        // it is given, so it is sized to the column keeping the aspect ratio of
        // its content box. Named here because no API says a component scales;
        // the dependency on components is one way and costs nothing to apps
        // that never use these classes.
        if (pw > 0 && w > 0 && (c instanceof com.codename1.components.ScaleImageLabel
                || c instanceof com.codename1.components.ScaleImageButton)) {
            Style st = c.getStyle();
            int hp = st.getHorizontalPadding();
            int vp = st.getVerticalPadding();
            int contentW = Math.max(1, pw - hp);
            h = vp + (int) ((long) Math.max(0, h - vp) * Math.max(0, w - hp) / contentW);
        }
        return h;
    }

    /// Packs the children into columns; places them when `apply` is set and
    /// answers the height of the tallest column either way.
    private int place(Container parent, int left, int top, int width, boolean apply) {
        int count = parent.getComponentCount();
        int cols = columnsFor(width);
        int gap = CN.convertToPixels(gapMM);
        int colWidth = Math.max(1, (width - (cols - 1) * gap) / cols);
        int[] heights = new int[cols];
        boolean rtl = parent.isRTL();
        boolean any = false;
        for (int i = 0; i < count; i++) {
            Component c = parent.getComponentAt(i);
            if (c.isHidden()) {
                continue;
            }
            int col = 0;
            for (int k = 1; k < cols; k++) {
                if (heights[k] < heights[col]) {
                    col = k;
                }
            }
            Style cs = c.getStyle();
            int w = Math.max(0, colWidth - cs.getHorizontalMargins());
            int h = heightAt(c, w);
            int y = heights[col] == 0 ? 0 : heights[col] + gap;
            if (apply) {
                int x = col * (colWidth + gap);
                if (rtl) {
                    x = width - x - colWidth;
                }
                c.setX(left + x + cs.getMarginLeftNoRTL());
                c.setY(top + y + cs.getMarginTop());
                c.setWidth(w);
                c.setHeight(h);
            }
            heights[col] = y + h + cs.getVerticalMargins();
            any = true;
        }
        if (!any) {
            return 0;
        }
        int tallest = 0;
        for (int k = 0; k < cols; k++) {
            tallest = Math.max(tallest, heights[k]);
        }
        return tallest;
    }

    @Override
    public String toString() {
        return "MasonryLayout";
    }
}
