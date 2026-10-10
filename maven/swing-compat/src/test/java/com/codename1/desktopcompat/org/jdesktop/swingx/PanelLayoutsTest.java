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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.BorderFactory;
import com.codename1.desktopcompat.javax.swing.JPanel;
import org.junit.Test;

/// The arithmetic of the vertical and horizontal layouts and of the
/// status bar's constraints.
public class PanelLayoutsTest extends KernelTestBase {

    private static JPanel box(int w, int h) {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(w, h));
        return p;
    }

    @Test
    public void verticalLayoutStacksVisibleChildrenAtFullWidth() {
        JPanel p = new JPanel(new VerticalLayout(5));
        p.setBorder(BorderFactory.createEmptyBorder(1, 2, 3, 4));
        JPanel a = box(30, 10);
        JPanel hidden = box(500, 500);
        hidden.setVisible(false);
        JPanel b = box(50, 20);
        p.add(a);
        p.add(hidden);
        p.add(b);
        assertEquals(new Dimension(50 + 6, 10 + 5 + 20 + 4), p.getPreferredSize());
        assertEquals(p.getPreferredSize(), p.getMinimumSize());
        p.setSize(100, 100);
        p.doLayout();
        assertEquals(new Rectangle(2, 1, 94, 10), a.getBounds());
        assertEquals(new Rectangle(2, 16, 94, 20), b.getBounds());
    }

    @Test
    public void horizontalLayoutPlacesVisibleChildrenAtFullHeight() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setGap(7);
        assertEquals(7, layout.getGap());
        JPanel p = new JPanel(layout);
        p.setBorder(BorderFactory.createEmptyBorder(1, 2, 3, 4));
        JPanel a = box(30, 10);
        JPanel b = box(50, 20);
        p.add(a);
        p.add(b);
        assertEquals(new Dimension(30 + 7 + 50 + 6, 20 + 4), p.getPreferredSize());
        p.setSize(200, 40);
        p.doLayout();
        assertEquals(new Rectangle(2, 1, 30, 36), a.getBounds());
        assertEquals(new Rectangle(39, 1, 50, 36), b.getBounds());
    }

    @Test
    public void statusBarGivesFixedWidthsAndSharesTheRest() {
        JXStatusBar bar = new JXStatusBar();
        JPanel a = box(10, 12);
        JPanel b = box(10, 12);
        JPanel c = box(30, 12);
        JPanel d = box(10, 12);
        bar.add(a, new JXStatusBar.Constraint(50));
        bar.add(b, new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));
        bar.add(c);
        bar.add(d, new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL, new Insets(0, 5, 0, 5)));
        bar.setSize(300, 30);
        bar.doLayout();
        assertEquals(new Rectangle(4, 3, 50, 24), a.getBounds());
        assertEquals(new Rectangle(63, 3, 87, 24), b.getBounds());
        assertEquals(new Rectangle(159, 3, 30, 24), c.getBounds());
        assertEquals(new Rectangle(203, 3, 88, 24), d.getBounds());
        // 4 edge + 50 + 10 + 30 + 10 preferred + 10 insets + 3 gaps of 9 + 4 edge
        assertEquals(new Dimension(145, 18), bar.getPreferredSize());
    }

    @Test
    public void statusBarConstraintAccessors() {
        JXStatusBar.Constraint fixed = new JXStatusBar.Constraint();
        assertEquals(JXStatusBar.Constraint.ResizeBehavior.FIXED, fixed.getResizeBehavior());
        assertTrue(fixed.getFixedWidth() < 0);
        fixed.setFixedWidth(40);
        assertEquals(40, fixed.getFixedWidth());
        Insets in = new Insets(1, 2, 3, 4);
        JXStatusBar.Constraint c = new JXStatusBar.Constraint(20, in);
        in.left = 99;
        assertEquals(new Insets(1, 2, 3, 4), c.getInsets());
        JXStatusBar bar = new JXStatusBar();
        assertTrue(bar.isResizeHandleEnabled());
        bar.setResizeHandleEnabled(false);
        assertFalse(bar.isResizeHandleEnabled());
    }

    @Test
    public void statusBarHidesNothingWhenTooNarrow() {
        JXStatusBar bar = new JXStatusBar();
        JPanel a = box(60, 12);
        JPanel fill = box(10, 12);
        bar.add(a);
        bar.add(fill, new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));
        bar.setSize(40, 20);
        bar.doLayout();
        assertEquals(60, a.getWidth());
        assertEquals(0, fill.getWidth());
    }
}
