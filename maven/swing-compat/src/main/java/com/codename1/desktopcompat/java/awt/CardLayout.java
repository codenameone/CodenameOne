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

import java.util.ArrayList;

/// Shows one child of a container at a time, each child filling the
/// container like a card in a stack.
public class CardLayout implements LayoutManager2 {

    private final ArrayList<String> names = new ArrayList<String>();
    private final ArrayList<Component> cards = new ArrayList<Component>();
    private int hgap;
    private int vgap;

    public CardLayout() {
        this(0, 0);
    }

    public CardLayout(int hgap, int vgap) {
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

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints == null) {
            constraints = "";
        }
        if (constraints instanceof String) {
            addLayoutComponent((String) constraints, comp);
        } else {
            throw new IllegalArgumentException("cannot add to layout: constraint must be a string");
        }
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        if (!cards.isEmpty()) {
            comp.setVisible(false);
        }
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).equals(name)) {
                cards.set(i, comp);
                return;
            }
        }
        names.add(name);
        cards.add(comp);
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i) == comp) {
                if (comp.isVisible() && comp.getParent() != null) {
                    next(comp.getParent());
                }
                cards.remove(i);
                names.remove(i);
                break;
            }
        }
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
        int w = 0;
        int h = 0;
        for (int i = 0; i < n; i++) {
            Component c = parent.getComponent(i);
            Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
            if (d.width > w) {
                w = d.width;
            }
            if (d.height > h) {
                h = d.height;
            }
        }
        return new Dimension(in.left + in.right + w + hgap * 2, in.top + in.bottom + h + vgap * 2);
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
        Insets in = parent.getInsets();
        int n = parent.getComponentCount();
        boolean shown = false;
        for (int i = 0; i < n; i++) {
            Component c = parent.getComponent(i);
            c.setBounds(hgap + in.left, vgap + in.top, parent.getWidth() - (hgap * 2 + in.left + in.right),
                    parent.getHeight() - (vgap * 2 + in.top + in.bottom));
            if (c.isVisible()) {
                shown = true;
            }
        }
        if (!shown && n > 0) {
            parent.getComponent(0).setVisible(true);
        }
    }

    private void check(Container parent) {
        if (parent.getLayout() != this) {
            throw new IllegalArgumentException("wrong parent for CardLayout");
        }
    }

    /// Hides whichever child is visible and shows the one at `index`,
    /// counted from the visible child when `relative` is set.
    private void flip(Container parent, int index, boolean relative) {
        check(parent);
        int n = parent.getComponentCount();
        if (n == 0) {
            return;
        }
        int target = index;
        boolean found = false;
        for (int i = 0; i < n; i++) {
            Component c = parent.getComponent(i);
            if (c.isVisible()) {
                if (relative) {
                    target = (i + index + n) % n;
                }
                c.setVisible(false);
                found = true;
                break;
            }
        }
        if (relative && !found) {
            target = index > 0 ? 0 : n - 1;
        }
        if (target < 0) {
            target = n - 1;
        }
        parent.getComponent(target).setVisible(true);
        parent.validate();
    }

    public void first(Container parent) {
        flip(parent, 0, false);
    }

    public void next(Container parent) {
        flip(parent, 1, true);
    }

    public void previous(Container parent) {
        flip(parent, -1, true);
    }

    public void last(Container parent) {
        flip(parent, -1, false);
    }

    public void show(Container parent, String name) {
        check(parent);
        Component next = null;
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).equals(name)) {
                next = cards.get(i);
                break;
            }
        }
        if (next == null || next.isVisible()) {
            return;
        }
        int n = parent.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component c = parent.getComponent(i);
            if (c.isVisible()) {
                c.setVisible(false);
                break;
            }
        }
        next.setVisible(true);
        parent.validate();
    }

    @Override
    public String toString() {
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + "]";
    }
}
