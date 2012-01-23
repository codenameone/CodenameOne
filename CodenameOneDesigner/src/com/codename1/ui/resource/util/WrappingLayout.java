/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.ui.resource.util;

import com.codename1.designer.HorizontalList;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.prefs.Preferences;

/**
 * Simple layout manager that arranges icons in a changeable grid similar to grid
 * layout in a sense but it doesn't impose the same limitation. Elements will flow
 * vertically if there is no room to grow horizontally.
 *
 * @author Shai Almog
 */
public class WrappingLayout implements LayoutManager {
    private int maxButtonWidth;

    private static final SizeGetter PREF_SIZE = new SizeGetter();
    private static final SizeGetter MIN_SIZE = new SizeGetter() {

        public Dimension getSize(Component c, int max) {
            if(max > -1) {
                Dimension d = c.getMinimumSize();
                if(d.width > max) {
                    d.width = max;
                }
                return d;
            }
            return c.getMinimumSize();
        }
    };

    public WrappingLayout(int maxButtonWidth) {
        this.maxButtonWidth = maxButtonWidth;
    }

    public void addLayoutComponent(String name, Component comp) {
    }

    public void removeLayoutComponent(Component comp) {
    }

    private Dimension layoutSize(Container parent, SizeGetter getter) {
        int ncomponents = parent.getComponentCount();

        int w = 0;
        int h = 0;
        int v = Preferences.userNodeForPackage(HorizontalList.class).getInt("previewIconWidth", 24);
        if(v > maxButtonWidth && maxButtonWidth > -1) {
            maxButtonWidth = v;
        } 
        for (int i = 0; i < ncomponents; i++) {
            Component comp = parent.getComponent(i);
            Dimension d = getter.getSize(comp, maxButtonWidth);
            if (w < d.width) {
                w = d.width;
            }
            if (h < d.height) {
                h = d.height;
            }
        }

        if (ncomponents < 1) {
            return new Dimension(10, 10);
        }

        int parentWidth = Math.max(w, parent.getWidth());
        int columns = parentWidth / w;
        int rows = ncomponents / columns;
        if (ncomponents % columns != 0) {
            rows += 1;
        }

        return new Dimension(columns * w + (2) + columns, rows * h + (rows - 1) + rows);
    }

    public Dimension preferredLayoutSize(Container parent) {
        return layoutSize(parent, PREF_SIZE);
    }

    public Dimension minimumLayoutSize(Container parent) {
        return layoutSize(parent, MIN_SIZE);
    }

    public void layoutContainer(Container parent) {
        int ncomponents = parent.getComponentCount();

        if (ncomponents == 0) {
            return;
        }

        int w = 0;
        int h = 0;
        for (int i = 0; i < ncomponents; i++) {
            Component comp = parent.getComponent(i);
            Dimension d = PREF_SIZE.getSize(comp, maxButtonWidth);
            if (w < d.width) {
                w = d.width;
            }
            if (h < d.height) {
                h = d.height;
            }
        }

        int parentWidth = parent.getWidth();

        // use up all the width or at least one column
        int columns = Math.max(1, Math.min(ncomponents, parentWidth / w));
        int rows = ncomponents / columns;
        if (ncomponents % columns != 0) {
            rows++;
        }

        int x = 1;
        int currentRow = 0;
        for (int i = 0; i < ncomponents; i++) {
            Component c = parent.getComponent(i);
            int currentWidth = w;
            if (x + currentWidth > parent.getWidth()) {
                x = 1;
                currentRow++;
            }

            // add 1 pixel margin to every component
            //int x = currentColumn * w + 1 + currentColumn;
            int y = currentRow * h + 1 + currentRow;

            c.setBounds(x, y, currentWidth, h);
            x += currentWidth + 1;
        }
    }

    /**
     * This class allows the code to calculate preferred and minimum size to use the 
     * same method code
     */
    private static class SizeGetter {

        public Dimension getSize(Component c, int max) {
            if(max > -1) {
                Dimension d = c.getPreferredSize();
                if(d.width > max) {
                    d.width = max;
                }
                return d;
            }
            return c.getPreferredSize();
        }
    }
}
