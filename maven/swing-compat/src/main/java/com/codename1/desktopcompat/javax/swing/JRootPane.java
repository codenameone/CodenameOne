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

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager;

/// The single child of a frame or dialog; it holds the content pane, which
/// fills it. There is no layered pane, glass pane or menu bar.
public class JRootPane extends JComponent {

    protected Container contentPane;
    protected JButton defaultButton;

    public JRootPane() {
        setLayout(createRootLayout());
        setContentPane(createContentPane());
    }

    protected Container createContentPane() {
        JPanel p = new JPanel(new BorderLayout());
        p.setName(getName() + ".contentPane");
        return p;
    }

    protected LayoutManager createRootLayout() {
        return new RootLayout();
    }

    public Container getContentPane() {
        return contentPane;
    }

    public void setContentPane(Container content) {
        if (content == null) {
            throw new IllegalArgumentException("contentPane cannot be set to null.");
        }
        if (contentPane != null && contentPane.getParent() == this) {
            remove(contentPane);
        }
        contentPane = content;
        add(content);
    }

    public JButton getDefaultButton() {
        return defaultButton;
    }

    /// Recorded only: no key activates the default button.
    public void setDefaultButton(JButton defaultButton) {
        this.defaultButton = defaultButton;
    }

    @Override
    public boolean isValidateRoot() {
        return true;
    }

    /// Gives every child the whole of the root pane.
    private static final class RootLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return size(parent, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return size(parent, false);
        }

        private static Dimension size(Container parent, boolean preferred) {
            Insets in = parent.getInsets();
            int w = 0;
            int h = 0;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component c = parent.getComponent(i);
                Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                w = Math.max(w, d.width);
                h = Math.max(h, d.height);
            }
            return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int w = parent.getWidth() - in.left - in.right;
            int h = parent.getHeight() - in.top - in.bottom;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                parent.getComponent(i).setBounds(in.left, in.top, w, h);
            }
        }
    }
}
