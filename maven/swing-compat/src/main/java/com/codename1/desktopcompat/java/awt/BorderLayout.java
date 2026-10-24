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

/// Lays a container out as a centre surrounded by up to four edge
/// components.
///
/// There is no component orientation in this layer, so the relative
/// positions resolve as they do left to right: `PAGE_START` is the north
/// edge, `PAGE_END` the south, `LINE_START` the west and `LINE_END` the
/// east. A component added under a relative name takes precedence over one
/// added under the absolute name of the same edge.
public class BorderLayout implements LayoutManager2 {

    public static final String NORTH = "North";

    public static final String SOUTH = "South";

    public static final String EAST = "East";

    public static final String WEST = "West";

    public static final String CENTER = "Center";

    public static final String BEFORE_FIRST_LINE = "First";

    public static final String AFTER_LAST_LINE = "Last";

    public static final String BEFORE_LINE_BEGINS = "Before";

    public static final String AFTER_LINE_ENDS = "After";

    public static final String PAGE_START = BEFORE_FIRST_LINE;

    public static final String PAGE_END = AFTER_LAST_LINE;

    public static final String LINE_START = BEFORE_LINE_BEGINS;

    public static final String LINE_END = AFTER_LINE_ENDS;

    private static final int N = 0;
    private static final int S = 1;
    private static final int E = 2;
    private static final int W = 3;
    private static final int C = 4;
    private static final int FIRST_LINE = 5;
    private static final int LAST_LINE = 6;
    private static final int FIRST_ITEM = 7;
    private static final int LAST_ITEM = 8;
    private static final String[] NAMES = {
        NORTH, SOUTH, EAST, WEST, CENTER, BEFORE_FIRST_LINE, AFTER_LAST_LINE, BEFORE_LINE_BEGINS, AFTER_LINE_ENDS
    };

    private int hgap;
    private int vgap;
    private final Component[] slots = new Component[NAMES.length];

    public BorderLayout() {
        this(0, 0);
    }

    public BorderLayout(int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
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

    private static int slotOf(Object name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints == null || constraints instanceof String) {
            addLayoutComponent((String) constraints, comp);
        } else {
            throw new IllegalArgumentException("cannot add to layout: constraint must be a string (or null)");
        }
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        int slot = name == null ? C : slotOf(name);
        if (slot < 0) {
            throw new IllegalArgumentException("cannot add to layout: unknown constraint: " + name);
        }
        slots[slot] = comp;
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == comp) {
                slots[i] = null;
            }
        }
    }

    public Component getLayoutComponent(Object constraints) {
        int slot = slotOf(constraints);
        if (slot < 0) {
            throw new IllegalArgumentException("cannot get component: unknown constraint: " + constraints);
        }
        return slots[slot];
    }

    public Component getLayoutComponent(Container target, Object constraints) {
        int slot = slotOf(constraints);
        if (slot < 0 || slot > C) {
            throw new IllegalArgumentException("cannot get component: invalid constraint: " + constraints);
        }
        return resolve(slot);
    }

    public Object getConstraints(Component comp) {
        if (comp == null) {
            return null;
        }
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == comp) {
                return NAMES[i];
            }
        }
        return null;
    }

    /// The component occupying an absolute edge, a relative name winning
    /// over the absolute one.
    private Component resolve(int edge) {
        Component c = null;
        switch (edge) {
            case N:
                c = slots[FIRST_LINE];
                break;
            case S:
                c = slots[LAST_LINE];
                break;
            case W:
                c = slots[FIRST_ITEM];
                break;
            case E:
                c = slots[LAST_ITEM];
                break;
            default:
                break;
        }
        return c != null ? c : slots[edge];
    }

    private Component visible(int edge) {
        Component c = resolve(edge);
        return c != null && c.isVisible() ? c : null;
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return measure(target, false);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return measure(target, true);
    }

    private static Dimension sizeOf(Component c, boolean preferred) {
        return preferred ? c.getPreferredSize() : c.getMinimumSize();
    }

    private Dimension measure(Container target, boolean preferred) {
        int w = 0;
        int h = 0;
        Component c = visible(E);
        if (c != null) {
            Dimension d = sizeOf(c, preferred);
            w += d.width + hgap;
            h = Math.max(d.height, h);
        }
        c = visible(W);
        if (c != null) {
            Dimension d = sizeOf(c, preferred);
            w += d.width + hgap;
            h = Math.max(d.height, h);
        }
        c = visible(C);
        if (c != null) {
            Dimension d = sizeOf(c, preferred);
            w += d.width;
            h = Math.max(d.height, h);
        }
        c = visible(N);
        if (c != null) {
            Dimension d = sizeOf(c, preferred);
            w = Math.max(d.width, w);
            h += d.height + vgap;
        }
        c = visible(S);
        if (c != null) {
            Dimension d = sizeOf(c, preferred);
            w = Math.max(d.width, w);
            h += d.height + vgap;
        }
        Insets in = target.getInsets();
        return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
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
    public void layoutContainer(Container target) {
        Insets in = target.getInsets();
        int top = in.top;
        int bottom = target.getHeight() - in.bottom;
        int left = in.left;
        int right = target.getWidth() - in.right;

        Component c = visible(N);
        if (c != null) {
            c.setSize(right - left, c.getHeight());
            Dimension d = c.getPreferredSize();
            c.setBounds(left, top, right - left, d.height);
            top += d.height + vgap;
        }
        c = visible(S);
        if (c != null) {
            c.setSize(right - left, c.getHeight());
            Dimension d = c.getPreferredSize();
            c.setBounds(left, bottom - d.height, right - left, d.height);
            bottom -= d.height + vgap;
        }
        c = visible(E);
        if (c != null) {
            c.setSize(c.getWidth(), bottom - top);
            Dimension d = c.getPreferredSize();
            c.setBounds(right - d.width, top, d.width, bottom - top);
            right -= d.width + hgap;
        }
        c = visible(W);
        if (c != null) {
            c.setSize(c.getWidth(), bottom - top);
            Dimension d = c.getPreferredSize();
            c.setBounds(left, top, d.width, bottom - top);
            left += d.width + hgap;
        }
        c = visible(C);
        if (c != null) {
            c.setBounds(left, top, right - left, bottom - top);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + "]";
    }
}
