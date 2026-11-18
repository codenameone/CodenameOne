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
package com.codename1.ui.table;

import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.Style;

/// The renderer a `DataGrid` uses for a column that has none: text aligned
/// by kind, a check box for a boolean, and an image scaled to the row.
///
/// Subclass it to change one thing, such as the colour of a value, without
/// repainting the rest.
public class DefaultGridCellRenderer implements GridCellRenderer {

    @Override
    public void paint(Graphics g, GridCell cell, int x, int y, int width, int height) {
        Object v = cell.getValue();
        Style st = cell.getStyle();
        int padL = st.getPaddingLeftNoRTL();
        int padR = st.getPaddingRightNoRTL();
        GridColumn c = cell.getColumn();
        int align = c.resolveAlignment(v);
        if (v instanceof Image) {
            Image img = (Image) v;
            int ih = Math.max(1, height - st.getPaddingTop() - st.getPaddingBottom());
            int iw = img.getHeight() <= 0 ? ih : img.getWidth() * ih / img.getHeight();
            int ix = place(align, x + padL, width - padL - padR, iw);
            g.drawImage(img, ix, y + (height - ih) / 2, iw, ih);
            return;
        }
        if (!cell.isGroupRow() && c.resolveKind(v) == GridColumn.Kind.BOOLEAN) {
            boolean on = v instanceof Boolean && ((Boolean) v).booleanValue();
            int box = Math.min(height - 2, GridLook.mm(3.4f));
            int bx = place(align, x + padL, width - padL - padR, box);
            paintCheck(g, cell, bx, y + (height - box) / 2, box, on);
            return;
        }
        String text = text(cell);
        if (text.length() == 0) {
            return;
        }
        Font f = font(cell);
        int avail = width - padL - padR;
        String shown = GridLook.fit(text, f, avail);
        g.setFont(f);
        g.setColor(textColor(cell, st.getFgColor()));
        int tx = place(align, x + padL, avail, f.stringWidth(shown));
        g.drawString(shown, tx, y + (height - f.getHeight()) / 2);
    }

    @Override
    public int getPreferredWidth(GridCell cell) {
        Object v = cell.getValue();
        Style st = cell.getStyle();
        int pad = st.getPaddingLeftNoRTL() + st.getPaddingRightNoRTL();
        if (v instanceof Image) {
            return pad + GridLook.mm(6);
        }
        if (cell.getColumn().resolveKind(v) == GridColumn.Kind.BOOLEAN) {
            return pad + GridLook.mm(3.4f);
        }
        return pad + font(cell).stringWidth(text(cell));
    }

    /// The text a cell shows; by default its column's format of the value.
    protected String text(GridCell cell) {
        return cell.getColumn().format(cell.getValue());
    }

    /// The colour of a cell's text, given the row style's.
    protected int textColor(GridCell cell, int defaultColor) {
        return defaultColor;
    }

    /// The font of a cell's text; by default the row style's.
    protected Font font(GridCell cell) {
        return cell.getStyle().getFont();
    }

    /// Paints a check box of side `size` at `x`, `y`.
    protected void paintCheck(Graphics g, GridCell cell, int x, int y, int size, boolean on) {
        Style check = GridLook.style("DataGridCheck");
        int arc = Math.max(2, size / 4);
        int a = g.getAlpha();
        if (!cell.isEditable()) {
            g.setAlpha(a * 3 / 5);
        }
        if (on) {
            g.setColor(check.getBgColor());
            g.fillRoundRect(x, y, size, size, arc, arc);
            g.setColor(check.getFgColor());
            int s = size;
            // The tick, two strokes a pixel apart so it reads at any density.
            for (int d = 0; d < Math.max(1, s / 10); d++) {
                g.drawLine(x + s / 4, y + s / 2 + d, x + s * 5 / 12, y + s * 7 / 10 + d);
                g.drawLine(x + s * 5 / 12, y + s * 7 / 10 + d, x + s * 3 / 4, y + s * 3 / 10 + d);
            }
        } else {
            g.setColor(GridLook.mix(GridLook.under(cell.getStyle()), cell.getStyle().getFgColor(), 150));
            g.drawRoundRect(x, y, size - 1, size - 1, arc, arc);
        }
        g.setAlpha(a);
    }

    /// Where something `w` wide starts in a span from `x`, `avail` wide.
    protected static int place(int align, int x, int avail, int w) {
        if (align == Component.RIGHT) {
            return x + avail - w;
        }
        if (align == Component.CENTER) {
            return x + (avail - w) / 2;
        }
        return x;
    }
}
