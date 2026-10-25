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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.LayoutManager;

/// A container that lays its children out with a [BoxLayout], and the
/// factory of the invisible struts, rigid areas and glue that space them.
public class Box extends JComponent {

    public Box(int axis) {
        super();
        super.setLayout(new BoxLayout(this, axis));
    }

    public static Box createHorizontalBox() {
        return new Box(BoxLayout.X_AXIS);
    }

    public static Box createVerticalBox() {
        return new Box(BoxLayout.Y_AXIS);
    }

    public static Component createRigidArea(Dimension d) {
        return new Filler(d, d, d);
    }

    public static Component createHorizontalStrut(int width) {
        return new Filler(new Dimension(width, 0), new Dimension(width, 0), new Dimension(width, Short.MAX_VALUE));
    }

    public static Component createVerticalStrut(int height) {
        return new Filler(new Dimension(0, height), new Dimension(0, height), new Dimension(Short.MAX_VALUE, height));
    }

    public static Component createGlue() {
        return new Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
    }

    public static Component createHorizontalGlue() {
        return new Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(Short.MAX_VALUE, 0));
    }

    public static Component createVerticalGlue() {
        return new Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, Short.MAX_VALUE));
    }

    /// A box keeps the layout it was built with. The JDK throws `AWTError`
    /// here; this throws an `IllegalArgumentException`.
    @Override
    public void setLayout(LayoutManager l) {
        throw new IllegalArgumentException("Illegal request");
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (isOpaque()) {
            Color bg = getBackground();
            if (bg != null) {
                g.setColor(bg);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    /// An invisible component with fixed minimum, preferred and maximum
    /// sizes, used to take up space in a layout.
    public static class Filler extends JComponent {

        public Filler(Dimension min, Dimension pref, Dimension max) {
            setMinimumSize(min);
            setPreferredSize(pref);
            setMaximumSize(max);
        }

        public void changeShape(Dimension min, Dimension pref, Dimension max) {
            setMinimumSize(min);
            setPreferredSize(pref);
            setMaximumSize(max);
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (isOpaque()) {
                Color bg = getBackground();
                if (bg != null) {
                    g.setColor(bg);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        }
    }
}
