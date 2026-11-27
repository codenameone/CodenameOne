/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.merchant;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

/// A dashboard grid: as many equal columns as cells of at least a given width
/// fit in the room the container has -- not the screen's, so a pane beside a
/// sidebar counts its own width -- with every cell of a row as tall as the
/// tallest. A tile row reads eight across on a monitor, four on a tablet and
/// two on a phone; a chart row three, two and one.
final class DashGrid extends Layout {
    private final float cellMm;
    private final int most;

    /// @param cellMm the narrowest a cell may be, in millimetres
    /// @param most the most columns, however wide the container
    DashGrid(float cellMm, int most) {
        this.cellMm = cellMm;
        this.most = most;
    }

    /// A container laid out by a new grid.
    static Container of(float cellMm, int most) {
        return new Container(new DashGrid(cellMm, most));
    }

    private int inner(Container parent) {
        int w = parent.getWidth();
        if (w <= 0 && parent.getParent() != null) {
            Style ps = parent.getParent().getStyle();
            w = parent.getParent().getWidth() - ps.getHorizontalPadding();
        }
        if (w <= 0) {
            w = CN.getDisplayWidth();
        }
        Style s = parent.getStyle();
        return Math.max(1, w - s.getHorizontalPadding());
    }

    /// `fit` columns, fewer when that leaves the last row as full as the
    /// others: eight tiles where seven fit are two rows of four, not seven
    /// and one alone under them.
    static int balanced(int fit, int count) {
        int cols = Math.max(1, Math.min(fit, count));
        int rows = (count + cols - 1) / cols;
        return Math.max(1, (count + rows - 1) / rows);
    }

    /// The space between two columns: the cards' own margins are a phone's,
    /// made for a single column, and side by side they touched.
    private static int gap(int cols) {
        return cols > 1 ? CN.convertToPixels(2.4f) : 0;
    }

    /// How many columns a container of this inner width gets.
    int columns(int innerWidth) {
        int fit = innerWidth / Math.max(1, CN.convertToPixels(cellMm));
        return Math.max(1, Math.min(most, fit));
    }

    @Override
    public void layoutContainer(Container parent) {
        Style s = parent.getStyle();
        int width = inner(parent);
        int count = parent.getComponentCount();
        int cols = balanced(columns(width), count);
        int gap = gap(cols);
        int cell = (width - gap * (cols - 1)) / cols;
        int x0 = s.getPaddingLeft(parent.isRTL());
        int y = s.getPaddingTop();
        for (int first = 0; first < count; first += cols) {
            int rowH = rowHeight(parent, first, cols, cell);
            for (int c = 0; c < cols && first + c < count; c++) {
                Component cmp = parent.getComponentAt(first + c);
                Style cs = cmp.getStyle();
                int col = parent.isRTL() ? cols - 1 - c : c;
                cmp.setX(x0 + col * (cell + gap) + cs.getMarginLeftNoRTL());
                cmp.setY(y + cs.getMarginTop());
                cmp.setWidth(cell - cs.getHorizontalMargins());
                cmp.setHeight(rowH - cs.getVerticalMargins());
            }
            y += rowH;
        }
    }

    private static int rowHeight(Container parent, int first, int cols, int cell) {
        int h = 0;
        for (int c = 0; c < cols && first + c < parent.getComponentCount(); c++) {
            Component cmp = parent.getComponentAt(first + c);
            Style cs = cmp.getStyle();
            int w = cell - cs.getHorizontalMargins();
            // A cell's height depends on its width (wrapped text, a chart's
            // legend): measure it at the width it will get.
            int was = cmp.getWidth();
            cmp.setWidth(w);
            if (cmp instanceof Container) {
                ((Container) cmp).setShouldCalcPreferredSize(true);
            }
            h = Math.max(h, cmp.getPreferredH() + cs.getVerticalMargins());
            cmp.setWidth(was);
        }
        return h;
    }

    @Override
    public Dimension getPreferredSize(Container parent) {
        Style s = parent.getStyle();
        int width = inner(parent);
        int count = parent.getComponentCount();
        int cols = balanced(columns(width), count);
        int cell = (width - gap(cols) * (cols - 1)) / cols;
        int h = 0;
        for (int first = 0; first < count; first += cols) {
            h += rowHeight(parent, first, cols, cell);
        }
        return new Dimension(width + s.getHorizontalPadding(), h + s.getVerticalPadding());
    }
}
