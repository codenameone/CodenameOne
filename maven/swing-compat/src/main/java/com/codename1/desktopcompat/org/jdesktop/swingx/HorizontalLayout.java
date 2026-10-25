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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager;

/// Places the visible children side by side from the left, each as high
/// as the container and as wide as it prefers, with a gap between them.
public class HorizontalLayout implements LayoutManager {

    private int gap;

    public HorizontalLayout() {
        this(0);
    }

    public HorizontalLayout(int gap) {
        this.gap = gap;
    }

    public int getGap() {
        return gap;
    }

    public void setGap(int gap) {
        this.gap = gap;
    }

    @Override
    public void layoutContainer(Container parent) {
        Insets in = parent.getInsets();
        int height = Math.max(0, parent.getHeight() - in.top - in.bottom);
        int x = in.left;
        int n = parent.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component c = parent.getComponent(i);
            if (!c.isVisible()) {
                continue;
            }
            int w = c.getPreferredSize().width;
            c.setBounds(x, in.top, w, height);
            x += w + gap;
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        Insets in = parent.getInsets();
        int width = 0;
        int height = 0;
        int shown = 0;
        int n = parent.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component c = parent.getComponent(i);
            if (!c.isVisible()) {
                continue;
            }
            Dimension d = c.getPreferredSize();
            height = Math.max(height, d.height);
            width += d.width;
            shown++;
        }
        if (shown > 1) {
            width += gap * (shown - 1);
        }
        return new Dimension(width + in.left + in.right, height + in.top + in.bottom);
    }

    /// The preferred size: a row does not shrink its columns.
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }
}
