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
import com.codename1.desktopcompat.java.awt.Frame;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.awt.Window;

/// A Swing window without a title bar: a splash screen, a custom popup.
/// Shown first it fills a form like a frame; shown over another window it
/// floats over it like a dialog, or is an undecorated window where there
/// is a window manager.
public class JWindow extends Window implements RootPaneContainer {

    protected JRootPane rootPane;
    protected boolean rootPaneCheckingEnabled;

    public JWindow() {
        this((Frame) null);
    }

    public JWindow(Frame owner) {
        super(owner);
        windowInit();
    }

    public JWindow(Window owner) {
        super(owner);
        windowInit();
    }

    protected void windowInit() {
        setRootPane(createRootPane());
        setRootPaneCheckingEnabled(true);
    }

    protected JRootPane createRootPane() {
        return new JRootPane();
    }

    @Override
    public JRootPane getRootPane() {
        return rootPane;
    }

    protected void setRootPane(JRootPane root) {
        boolean checking = rootPaneCheckingEnabled;
        rootPaneCheckingEnabled = false;
        try {
            if (rootPane != null) {
                remove(rootPane);
            }
            rootPane = root;
            if (root != null) {
                add(root, BorderLayout.CENTER);
            }
        } finally {
            rootPaneCheckingEnabled = checking;
        }
    }

    @Override
    public Container getContentPane() {
        return rootPane.getContentPane();
    }

    @Override
    public void setContentPane(Container contentPane) {
        rootPane.setContentPane(contentPane);
    }

    @Override
    public JLayeredPane getLayeredPane() {
        return rootPane.getLayeredPane();
    }

    @Override
    public void setLayeredPane(JLayeredPane layeredPane) {
        rootPane.setLayeredPane(layeredPane);
    }

    @Override
    public Component getGlassPane() {
        return rootPane.getGlassPane();
    }

    @Override
    public void setGlassPane(Component glassPane) {
        rootPane.setGlassPane(glassPane);
    }

    protected boolean isRootPaneCheckingEnabled() {
        return rootPaneCheckingEnabled;
    }

    protected void setRootPaneCheckingEnabled(boolean enabled) {
        rootPaneCheckingEnabled = enabled;
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (rootPaneCheckingEnabled) {
            getContentPane().add(comp, constraints, index);
        } else {
            super.addImpl(comp, constraints, index);
        }
    }

    @Override
    public void remove(Component comp) {
        if (comp == rootPane || !rootPaneCheckingEnabled) {
            super.remove(comp);
        } else {
            getContentPane().remove(comp);
        }
    }

    @Override
    public void setLayout(LayoutManager manager) {
        if (rootPaneCheckingEnabled) {
            getContentPane().setLayout(manager);
        } else {
            super.setLayout(manager);
        }
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }
}
