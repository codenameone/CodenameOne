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
package com.codename1.desktopcompat.java.awt;

/// Divides a container into a grid of equally sized cells, one component to
/// a cell, filled row by row from the top left.
public class GridLayout implements LayoutManager {

    private int hgap;
    private int vgap;
    private int rows;
    private int cols;

    public GridLayout() {
        this(1, 0, 0, 0);
    }

    public GridLayout(int rows, int cols) {
        this(rows, cols, 0, 0);
    }

    public GridLayout(int rows, int cols, int hgap, int vgap) {
        if (rows == 0 && cols == 0) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
        this.cols = cols;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if (rows == 0 && this.cols == 0) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
    }

    public int getColumns() {
        return cols;
    }

    public void setColumns(int cols) {
        if (cols == 0 && this.rows == 0) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.cols = cols;
    }

    public int getHgap() {
        return hgap;
    }

    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    public int getVgap() {
        return vgap;
    }

    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return measure(parent, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return measure(parent, false);
    }

    private Dimension measure(Container parent, boolean preferred) {
        Insets in = parent.getInsets();
        int n = parent.getComponentCount();
        int nrows = rows;
        int ncols = cols;
        if (nrows > 0) {
            ncols = (n + nrows - 1) / nrows;
        } else {
            nrows = (n + ncols - 1) / ncols;
        }
        int w = 0;
        int h = 0;
        for (int i = 0; i < n; i++) {
            Component c = parent.getComponent(i);
            Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
            if (w < d.width) {
                w = d.width;
            }
            if (h < d.height) {
                h = d.height;
            }
        }
        return new Dimension(in.left + in.right + ncols * w + (ncols - 1) * hgap,
                in.top + in.bottom + nrows * h + (nrows - 1) * vgap);
    }

    @Override
    public void layoutContainer(Container parent) {
        Insets in = parent.getInsets();
        int n = parent.getComponentCount();
        if (n == 0) {
            return;
        }
        int nrows = rows;
        int ncols = cols;
        if (nrows > 0) {
            ncols = (n + nrows - 1) / nrows;
        } else {
            nrows = (n + ncols - 1) / ncols;
        }
        int gapsW = (ncols - 1) * hgap;
        int innerW = parent.getWidth() - (in.left + in.right);
        int cellW = (innerW - gapsW) / ncols;
        int spareW = (innerW - (cellW * ncols + gapsW)) / 2;
        int gapsH = (nrows - 1) * vgap;
        int innerH = parent.getHeight() - (in.top + in.bottom);
        int cellH = (innerH - gapsH) / nrows;
        int spareH = (innerH - (cellH * nrows + gapsH)) / 2;
        for (int i = 0; i < n; i++) {
            int r = i / ncols;
            int c = i % ncols;
            if (r >= nrows) {
                break;
            }
            parent.getComponent(i).setBounds(in.left + spareW + c * (cellW + hgap),
                    in.top + spareH + r * (cellH + vgap), cellW, cellH);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + ",rows=" + rows + ",cols=" + cols + "]";
    }
}
