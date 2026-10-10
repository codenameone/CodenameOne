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

/// Arranges components in rows at their preferred sizes, starting a new row
/// when the next component does not fit.
///
/// There is no component orientation in this layer: every container is laid
/// out left to right, so `LEADING` is the left edge and `TRAILING` the right
/// one. Baseline alignment is recorded but rows are always centred
/// vertically.
public class FlowLayout implements LayoutManager {

    public static final int LEFT = 0;

    public static final int CENTER = 1;

    public static final int RIGHT = 2;

    public static final int LEADING = 3;

    public static final int TRAILING = 4;

    private int align;
    private int hgap;
    private int vgap;
    private boolean alignOnBaseline;

    public FlowLayout() {
        this(CENTER, 5, 5);
    }

    public FlowLayout(int align) {
        this(align, 5, 5);
    }

    public FlowLayout(int align, int hgap, int vgap) {
        this.align = align;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    public int getAlignment() {
        return align;
    }

    public void setAlignment(int align) {
        this.align = align;
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

    /// Recorded only: rows are centred vertically whatever this is set to.
    public void setAlignOnBaseline(boolean alignOnBaseline) {
        this.alignOnBaseline = alignOnBaseline;
    }

    public boolean getAlignOnBaseline() {
        return alignOnBaseline;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return measure(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return measure(target, false);
    }

    private Dimension measure(Container target, boolean preferred) {
        int w = 0;
        int h = 0;
        boolean first = true;
        int n = target.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component c = target.getComponent(i);
            if (!c.isVisible()) {
                continue;
            }
            Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
            if (d.height > h) {
                h = d.height;
            }
            if (first) {
                first = false;
            } else {
                w += hgap;
            }
            w += d.width;
        }
        Insets in = target.getInsets();
        int across = in.left + in.right + hgap * 2;
        int limit = wrapWidth(target, w + across);
        if (w + across <= limit) {
            return new Dimension(w + across, h + in.top + in.bottom + vgap * 2);
        }
        // The row does not fit and will be wrapped: say how high the rows
        // are, so the container is given the room and they are not cut.
        int x = 0;
        int y = 0;
        int rowh = 0;
        int widest = 0;
        for (int i = 0; i < n; i++) {
            Component c = target.getComponent(i);
            if (!c.isVisible()) {
                continue;
            }
            Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
            if (d.width > widest) {
                widest = d.width;
            }
            if (x == 0 || x + hgap + d.width <= limit - across) {
                x += (x == 0 ? 0 : hgap) + d.width;
                rowh = Math.max(rowh, d.height);
            } else {
                y += rowh + vgap;
                x = d.width;
                rowh = d.height;
            }
        }
        // The width asked for stays that of one row, which is what the
        // container is given wherever there is room; while a window asks
        // how narrow it can be, it is the widest component.
        int wide = !preferred && com.codename1.desktopcompat.rt.RootPan.measuring() ? widest : w;
        return new Dimension(wide + across, y + rowh + in.top + in.bottom + vgap * 2);
    }

    /// The width rows are wrapped at when sizes are asked for: for a row
    /// wider than the display, that of the container once it has one, and
    /// never more than the display's.
    ///
    /// On a desktop a flow layout answers the size of a single row whatever
    /// width it has, and the rows it wraps into a narrower container are
    /// cut off. A window there is as wide as its content asks; on a phone
    /// it is as wide as the display, so a row of buttons wraps routinely
    /// and has to be given the height for it.
    private static int wrapWidth(Container target, int row) {
        int limit = com.codename1.desktopcompat.rt.RootPan.displayWidth();
        if (row <= limit) {
            // It fits the display: the answer of a desktop.
            return limit;
        }
        int w = target.getWidth();
        if (w > 0 && w < limit) {
            limit = w;
        }
        return limit;
    }

    @Override
    public void layoutContainer(Container target) {
        Insets in = target.getInsets();
        int avail = target.getWidth() - (in.left + in.right + hgap * 2);
        int n = target.getComponentCount();
        int used = 0;
        int y = in.top + vgap;
        int rowHeight = 0;
        int rowStart = 0;
        for (int i = 0; i < n; i++) {
            Component c = target.getComponent(i);
            if (!c.isVisible()) {
                continue;
            }
            Dimension d = c.getPreferredSize();
            c.setSize(d.width, d.height);
            if (used == 0 || used + d.width <= avail) {
                if (used > 0) {
                    used += hgap;
                }
                used += d.width;
                if (d.height > rowHeight) {
                    rowHeight = d.height;
                }
            } else {
                placeRow(target, in.left + hgap, y, avail - used, rowHeight, rowStart, i);
                y += vgap + rowHeight;
                used = d.width;
                rowHeight = d.height;
                rowStart = i;
            }
        }
        placeRow(target, in.left + hgap, y, avail - used, rowHeight, rowStart, n);
    }

    private void placeRow(Container target, int x, int y, int spare, int rowHeight, int from, int to) {
        switch (align) {
            case CENTER:
                x += spare / 2;
                break;
            case RIGHT:
            case TRAILING:
                x += spare;
                break;
            default:
                break;
        }
        for (int i = from; i < to; i++) {
            Component c = target.getComponent(i);
            if (c.isVisible()) {
                c.setLocation(x, y + (rowHeight - c.getHeight()) / 2);
                x += c.getWidth() + hgap;
            }
        }
    }

    @Override
    public String toString() {
        String a;
        switch (align) {
            case LEFT:
                a = ",align=left";
                break;
            case CENTER:
                a = ",align=center";
                break;
            case RIGHT:
                a = ",align=right";
                break;
            case LEADING:
                a = ",align=leading";
                break;
            case TRAILING:
                a = ",align=trailing";
                break;
            default:
                a = "";
                break;
        }
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + a + "]";
    }
}
