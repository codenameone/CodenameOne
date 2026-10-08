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

import java.util.Hashtable;

/// Lays components out in a grid of rows and columns of differing sizes,
/// each component described by a [GridBagConstraints].
///
/// Every container is laid out left to right, so the relative anchors
/// resolve as they do in that orientation (`LINE_START` is the west edge).
/// The baseline anchors are treated as their plain counterparts: `BASELINE`
/// as `CENTER`, `BASELINE_LEADING` as `LINE_START`, `ABOVE_BASELINE` as
/// `PAGE_START`, `BELOW_BASELINE` as `PAGE_END` and so on.
public class GridBagLayout implements LayoutManager2 {

    protected static final int MAXGRIDSIZE = 512;

    protected static final int MINSIZE = 1;

    protected static final int PREFERREDSIZE = 2;

    protected Hashtable<Component, GridBagConstraints> comptable;

    protected GridBagConstraints defaultConstraints;

    public int[] columnWidths;

    public int[] rowHeights;

    public double[] columnWeights;

    public double[] rowWeights;

    /// The grid computed by one measuring pass.
    private static final class Grid {
        int width;
        int height;
        int startx;
        int starty;
        int[] minWidth;
        int[] minHeight;
        double[] weightX;
        double[] weightY;
    }

    private Grid lastGrid;

    public GridBagLayout() {
        comptable = new Hashtable<Component, GridBagConstraints>();
        defaultConstraints = new GridBagConstraints();
    }

    public void setConstraints(Component comp, GridBagConstraints constraints) {
        Object copy = constraints.clone();
        if (copy instanceof GridBagConstraints) {
            comptable.put(comp, (GridBagConstraints) copy);
        }
    }

    public GridBagConstraints getConstraints(Component comp) {
        Object copy = lookupConstraints(comp).clone();
        return copy instanceof GridBagConstraints ? (GridBagConstraints) copy : null;
    }

    protected GridBagConstraints lookupConstraints(Component comp) {
        GridBagConstraints c = comptable.get(comp);
        if (c == null) {
            setConstraints(comp, defaultConstraints);
            c = comptable.get(comp);
        }
        return c;
    }

    public Point getLayoutOrigin() {
        Point origin = new Point(0, 0);
        if (lastGrid != null) {
            origin.x = lastGrid.startx;
            origin.y = lastGrid.starty;
        }
        return origin;
    }

    public int[][] getLayoutDimensions() {
        if (lastGrid == null) {
            return new int[2][0];
        }
        int[][] dim = new int[2][];
        dim[0] = new int[lastGrid.width];
        dim[1] = new int[lastGrid.height];
        System.arraycopy(lastGrid.minWidth, 0, dim[0], 0, lastGrid.width);
        System.arraycopy(lastGrid.minHeight, 0, dim[1], 0, lastGrid.height);
        return dim;
    }

    public double[][] getLayoutWeights() {
        if (lastGrid == null) {
            return new double[2][0];
        }
        double[][] weights = new double[2][];
        weights[0] = new double[lastGrid.width];
        weights[1] = new double[lastGrid.height];
        System.arraycopy(lastGrid.weightX, 0, weights[0], 0, lastGrid.width);
        System.arraycopy(lastGrid.weightY, 0, weights[1], 0, lastGrid.height);
        return weights;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof GridBagConstraints) {
            setConstraints(comp, (GridBagConstraints) constraints);
        } else if (constraints != null) {
            throw new IllegalArgumentException("cannot add to layout: constraints must be a GridBagConstraint");
        }
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        comptable.remove(comp);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return total(parent, measure(parent, PREFERREDSIZE));
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return total(parent, measure(parent, MINSIZE));
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public float getLayoutAlignmentX(Container parent) {
        return 0.5f;
    }

    @Override
    public float getLayoutAlignmentY(Container parent) {
        return 0.5f;
    }

    @Override
    public void invalidateLayout(Container target) {
    }

    @Override
    public void layoutContainer(Container parent) {
        arrange(parent);
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    private static int[] room(int[] a, int size) {
        if (a.length >= size) {
            return a;
        }
        int[] grown = new int[Math.max(size, a.length * 2)];
        System.arraycopy(a, 0, grown, 0, a.length);
        return grown;
    }

    private static int highest(int[] a, int from, int to) {
        int m = 0;
        for (int i = from; i < to; i++) {
            if (a[i] > m) {
                m = a[i];
            }
        }
        return m;
    }

    private static void mark(int[] a, int from, int to, int value) {
        for (int i = from; i < to; i++) {
            a[i] = value;
        }
    }

    /// Works out the grid for the visible children of `parent`: which cells
    /// each one occupies, and the size and weight of every column and row
    /// before any spare space is shared out.
    private Grid measure(Container parent, int sizeflag) {
        Component[] comps = parent.getComponents();
        Grid grid = new Grid();
        int gridW = 0;
        int gridH = 0;
        int[] rowEnd = new int[16];
        int[] colEnd = new int[16];
        int curRow = -1;
        int curCol = -1;

        // First pass: the extent of the grid, every open-ended span counted
        // as a single cell.
        for (int n = 0; n < comps.length; n++) {
            Component comp = comps[n];
            if (!comp.isVisible()) {
                continue;
            }
            GridBagConstraints gc = lookupConstraints(comp);
            int cx = gc.gridx;
            int cy = gc.gridy;
            int cw = gc.gridwidth <= 0 ? 1 : gc.gridwidth;
            int ch = gc.gridheight <= 0 ? 1 : gc.gridheight;
            if (cx < 0 && cy < 0) {
                if (curRow >= 0) {
                    cy = curRow;
                } else if (curCol >= 0) {
                    cx = curCol;
                } else {
                    cy = 0;
                }
            }
            if (cx < 0) {
                rowEnd = room(rowEnd, cy + ch);
                cx = highest(rowEnd, cy, cy + ch) - cx - 1;
                if (cx < 0) {
                    cx = 0;
                }
            } else if (cy < 0) {
                colEnd = room(colEnd, cx + cw);
                cy = highest(colEnd, cx, cx + cw) - cy - 1;
                if (cy < 0) {
                    cy = 0;
                }
            }
            int px = cx + cw;
            int py = cy + ch;
            if (gridW < px) {
                gridW = px;
            }
            if (gridH < py) {
                gridH = py;
            }
            colEnd = room(colEnd, px);
            rowEnd = room(rowEnd, py);
            mark(colEnd, cx, px, py);
            mark(rowEnd, cy, py, px);

            Dimension d = sizeflag == PREFERREDSIZE ? comp.getPreferredSize() : comp.getMinimumSize();
            gc.minWidth = d.width;
            gc.minHeight = d.height;

            if (gc.gridheight == 0 && gc.gridwidth == 0) {
                curRow = -1;
                curCol = -1;
            }
            if (gc.gridheight == 0 && curRow < 0) {
                curCol = cx + cw;
            } else if (gc.gridwidth == 0 && curCol < 0) {
                curRow = cy + ch;
            }
        }
        if (columnWidths != null && gridW < columnWidths.length) {
            gridW = columnWidths.length;
        }
        if (rowHeights != null && gridH < rowHeights.length) {
            gridH = rowHeights.length;
        }
        grid.width = gridW;
        grid.height = gridH;
        grid.minWidth = new int[gridW];
        grid.minHeight = new int[gridH];
        grid.weightX = new double[gridW];
        grid.weightY = new double[gridH];
        if (columnWidths != null) {
            System.arraycopy(columnWidths, 0, grid.minWidth, 0, columnWidths.length);
        }
        if (rowHeights != null) {
            System.arraycopy(rowHeights, 0, grid.minHeight, 0, rowHeights.length);
        }
        if (columnWeights != null) {
            System.arraycopy(columnWeights, 0, grid.weightX, 0, Math.min(gridW, columnWeights.length));
        }
        if (rowWeights != null) {
            System.arraycopy(rowWeights, 0, grid.weightY, 0, Math.min(gridH, rowWeights.length));
        }

        // Second pass: the same walk with the grid's extent known, so the
        // open-ended spans reach the last column or row.
        int longest = 1;
        curRow = -1;
        curCol = -1;
        rowEnd = new int[Math.max(gridH, 1)];
        colEnd = new int[Math.max(gridW, 1)];
        for (int n = 0; n < comps.length; n++) {
            Component comp = comps[n];
            if (!comp.isVisible()) {
                continue;
            }
            GridBagConstraints gc = lookupConstraints(comp);
            int cx = gc.gridx;
            int cy = gc.gridy;
            int cw = gc.gridwidth;
            int ch = gc.gridheight;
            if (cx < 0 && cy < 0) {
                if (curRow >= 0) {
                    cy = curRow;
                } else if (curCol >= 0) {
                    cx = curCol;
                } else {
                    cy = 0;
                }
            }
            if (cx < 0) {
                if (ch <= 0) {
                    ch += gridH - cy;
                    if (ch < 1) {
                        ch = 1;
                    }
                }
                rowEnd = room(rowEnd, cy + ch);
                cx = highest(rowEnd, cy, cy + ch) - cx - 1;
                if (cx < 0) {
                    cx = 0;
                }
            } else if (cy < 0) {
                if (cw <= 0) {
                    cw += gridW - cx;
                    if (cw < 1) {
                        cw = 1;
                    }
                }
                colEnd = room(colEnd, cx + cw);
                cy = highest(colEnd, cx, cx + cw) - cy - 1;
                if (cy < 0) {
                    cy = 0;
                }
            }
            if (cw <= 0) {
                cw += gridW - cx;
                if (cw < 1) {
                    cw = 1;
                }
            }
            if (ch <= 0) {
                ch += gridH - cy;
                if (ch < 1) {
                    ch = 1;
                }
            }
            int px = cx + cw;
            int py = cy + ch;
            colEnd = room(colEnd, px);
            rowEnd = room(rowEnd, py);
            mark(colEnd, cx, px, py);
            mark(rowEnd, cy, py, px);
            if (gc.gridheight == 0 && gc.gridwidth == 0) {
                curRow = -1;
                curCol = -1;
            }
            if (gc.gridheight == 0 && curRow < 0) {
                curCol = cx + cw;
            } else if (gc.gridwidth == 0 && curCol < 0) {
                curRow = cy + ch;
            }
            gc.tempX = cx;
            gc.tempY = cy;
            gc.tempWidth = cw;
            gc.tempHeight = ch;
            // A relative position resolved in this pass can land past the
            // extent the first pass counted. Such a cell gets a size and a
            // place, but as in the JDK it is not part of the grid: it adds
            // nothing to the layout's size and takes no spare space.
            if (px > grid.minWidth.length || py > grid.minHeight.length) {
                grow(grid, Math.max(px, grid.minWidth.length), Math.max(py, grid.minHeight.length));
            }
            if (cw > longest) {
                longest = cw;
            }
            if (ch > longest) {
                longest = ch;
            }
        }

        // Third pass: sizes and weights, components spanning fewer cells
        // first so a wide one only adds what its cells still lack.
        for (int span = 1; span <= longest; span++) {
            for (int n = 0; n < comps.length; n++) {
                Component comp = comps[n];
                if (!comp.isVisible()) {
                    continue;
                }
                GridBagConstraints gc = lookupConstraints(comp);
                Insets in = gc.insets;
                if (gc.tempWidth == span) {
                    share(grid.minWidth, grid.weightX, gc.tempX, gc.tempX + span, gc.weightx,
                            gc.minWidth + gc.ipadx + in.left + in.right);
                }
                if (gc.tempHeight == span) {
                    share(grid.minHeight, grid.weightY, gc.tempY, gc.tempY + span, gc.weighty,
                            gc.minHeight + gc.ipady + in.top + in.bottom);
                }
            }
        }
        return grid;
    }

    /// Makes room in the arrays for cells beyond the grid's extent, which
    /// itself stays as it is.
    private static void grow(Grid grid, int w, int h) {
        int[] mw = new int[w];
        int[] mh = new int[h];
        double[] wx = new double[w];
        double[] wy = new double[h];
        System.arraycopy(grid.minWidth, 0, mw, 0, grid.minWidth.length);
        System.arraycopy(grid.minHeight, 0, mh, 0, grid.minHeight.length);
        System.arraycopy(grid.weightX, 0, wx, 0, grid.weightX.length);
        System.arraycopy(grid.weightY, 0, wy, 0, grid.weightY.length);
        grid.minWidth = mw;
        grid.minHeight = mh;
        grid.weightX = wx;
        grid.weightY = wy;
    }

    /// Raises the weights and sizes of the cells `from` to `to` until
    /// together they carry `wantWeight` and `wantPixels`. What is missing
    /// goes to the cells in proportion to their weight, the last cell
    /// taking the rounding and, when none has weight, all of it.
    private static void share(int[] size, double[] weights, int from, int to, double wantWeight, int wantPixels) {
        double missing = wantWeight;
        for (int k = from; k < to; k++) {
            missing -= weights[k];
        }
        if (missing > 0.0) {
            double weight = 0.0;
            for (int k = from; k < to; k++) {
                weight += weights[k];
            }
            for (int k = from; weight > 0.0 && k < to; k++) {
                double wt = weights[k];
                double dx = (wt * missing) / weight;
                weights[k] += dx;
                missing -= dx;
                weight -= wt;
            }
            weights[to - 1] += missing;
        }
        int pixels = wantPixels;
        for (int k = from; k < to; k++) {
            pixels -= size[k];
        }
        if (pixels > 0) {
            double weight = 0.0;
            for (int k = from; k < to; k++) {
                weight += weights[k];
            }
            for (int k = from; weight > 0.0 && k < to; k++) {
                double wt = weights[k];
                int dx = (int) ((wt * (double) pixels) / weight);
                size[k] += dx;
                pixels -= dx;
                weight -= wt;
            }
            size[to - 1] += pixels;
        }
    }

    private static Dimension total(Container parent, Grid grid) {
        Insets in = parent.getInsets();
        int w = 0;
        for (int i = 0; i < grid.width; i++) {
            w += grid.minWidth[i];
        }
        int h = 0;
        for (int i = 0; i < grid.height; i++) {
            h += grid.minHeight[i];
        }
        return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
    }

    /// Shares `spare` pixels over the cells by weight and answers the width
    /// the cells then add up to, given they added up to `used` before. A
    /// cell squeezed below nothing is held at zero.
    private static int stretch(int[] size, double[] weights, int count, int spare, int used) {
        double weight = 0.0;
        for (int i = 0; i < count; i++) {
            weight += weights[i];
        }
        if (weight > 0.0) {
            for (int i = 0; i < count; i++) {
                int dx = (int) (((double) spare * weights[i]) / weight);
                size[i] += dx;
                used += dx;
                if (size[i] < 0) {
                    used -= size[i];
                    size[i] = 0;
                }
            }
        }
        return used;
    }

    private void arrange(Container parent) {
        Component[] comps = parent.getComponents();
        if (comps.length == 0 && (columnWidths == null || columnWidths.length == 0)
                && (rowHeights == null || rowHeights.length == 0)) {
            return;
        }
        Insets pin = parent.getInsets();
        int pw = parent.getWidth();
        int ph = parent.getHeight();
        Grid grid = measure(parent, PREFERREDSIZE);
        Dimension d = total(parent, grid);
        if (pw < d.width || ph < d.height) {
            grid = measure(parent, MINSIZE);
            d = total(parent, grid);
        }
        lastGrid = grid;

        int spareW = pw - d.width;
        if (spareW != 0) {
            spareW = pw - stretch(grid.minWidth, grid.weightX, grid.width, spareW, d.width);
        }
        int spareH = ph - d.height;
        if (spareH != 0) {
            spareH = ph - stretch(grid.minHeight, grid.weightY, grid.height, spareH, d.height);
        }
        grid.startx = spareW / 2 + pin.left;
        grid.starty = spareH / 2 + pin.top;

        Rectangle r = new Rectangle();
        for (int n = 0; n < comps.length; n++) {
            Component comp = comps[n];
            if (!comp.isVisible()) {
                continue;
            }
            GridBagConstraints gc = lookupConstraints(comp);
            r.x = grid.startx;
            for (int i = 0; i < gc.tempX; i++) {
                r.x += grid.minWidth[i];
            }
            r.y = grid.starty;
            for (int i = 0; i < gc.tempY; i++) {
                r.y += grid.minHeight[i];
            }
            r.width = 0;
            for (int i = gc.tempX; i < gc.tempX + gc.tempWidth; i++) {
                r.width += grid.minWidth[i];
            }
            r.height = 0;
            for (int i = gc.tempY; i < gc.tempY + gc.tempHeight; i++) {
                r.height += grid.minHeight[i];
            }
            settle(gc, r);
            if (r.x < 0) {
                r.width += r.x;
                r.x = 0;
            }
            if (r.y < 0) {
                r.height += r.y;
                r.y = 0;
            }
            if (r.width <= 0 || r.height <= 0) {
                comp.setBounds(0, 0, 0, 0);
            } else if (comp.getX() != r.x || comp.getY() != r.y || comp.getWidth() != r.width
                    || comp.getHeight() != r.height) {
                comp.setBounds(r.x, r.y, r.width, r.height);
            }
        }
    }

    /// Shrinks a display area to the component's bounds: insets off, then
    /// the component's own size unless it fills, then the anchor.
    private static void settle(GridBagConstraints gc, Rectangle r) {
        Insets in = gc.insets;
        r.x += in.left;
        r.width -= in.left + in.right;
        r.y += in.top;
        r.height -= in.top + in.bottom;

        int freeX = 0;
        int ownW = gc.minWidth + gc.ipadx;
        if (gc.fill != GridBagConstraints.HORIZONTAL && gc.fill != GridBagConstraints.BOTH && r.width > ownW) {
            freeX = r.width - ownW;
            r.width = ownW;
        }
        int freeY = 0;
        int ownH = gc.minHeight + gc.ipady;
        if (gc.fill != GridBagConstraints.VERTICAL && gc.fill != GridBagConstraints.BOTH && r.height > ownH) {
            freeY = r.height - ownH;
            r.height = ownH;
        }

        // 0 = start, 1 = middle, 2 = end, on each axis.
        int h;
        int v;
        switch (gc.anchor) {
            case GridBagConstraints.NORTH:
            case GridBagConstraints.PAGE_START:
            case GridBagConstraints.ABOVE_BASELINE:
                h = 1;
                v = 0;
                break;
            case GridBagConstraints.NORTHEAST:
            case GridBagConstraints.FIRST_LINE_END:
            case GridBagConstraints.ABOVE_BASELINE_TRAILING:
                h = 2;
                v = 0;
                break;
            case GridBagConstraints.EAST:
            case GridBagConstraints.LINE_END:
            case GridBagConstraints.BASELINE_TRAILING:
                h = 2;
                v = 1;
                break;
            case GridBagConstraints.SOUTHEAST:
            case GridBagConstraints.LAST_LINE_END:
            case GridBagConstraints.BELOW_BASELINE_TRAILING:
                h = 2;
                v = 2;
                break;
            case GridBagConstraints.SOUTH:
            case GridBagConstraints.PAGE_END:
            case GridBagConstraints.BELOW_BASELINE:
                h = 1;
                v = 2;
                break;
            case GridBagConstraints.SOUTHWEST:
            case GridBagConstraints.LAST_LINE_START:
            case GridBagConstraints.BELOW_BASELINE_LEADING:
                h = 0;
                v = 2;
                break;
            case GridBagConstraints.WEST:
            case GridBagConstraints.LINE_START:
            case GridBagConstraints.BASELINE_LEADING:
                h = 0;
                v = 1;
                break;
            case GridBagConstraints.NORTHWEST:
            case GridBagConstraints.FIRST_LINE_START:
            case GridBagConstraints.ABOVE_BASELINE_LEADING:
                h = 0;
                v = 0;
                break;
            case GridBagConstraints.CENTER:
            case GridBagConstraints.BASELINE:
                h = 1;
                v = 1;
                break;
            default:
                throw new IllegalArgumentException("illegal anchor value");
        }
        if (h == 1) {
            r.x += freeX / 2;
        } else if (h == 2) {
            r.x += freeX;
        }
        if (v == 1) {
            r.y += freeY / 2;
        } else if (v == 2) {
            r.y += freeY;
        }
    }
}
