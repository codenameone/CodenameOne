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
import com.codename1.desktopcompat.java.awt.GraphicsConfiguration;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.awt.event.WindowEvent;
import com.codename1.desktopcompat.rt.WindowHosts;

/// The Swing frame: a frame whose single child is a root pane, so that
/// adding to the frame adds to its content pane.
///
/// A request to close the frame -- the back command of its form, the
/// close box of its window, the platform ending the application -- runs
/// the default close operation after the window listeners:
/// `HIDE_ON_CLOSE` (the default) hides it, `DISPOSE_ON_CLOSE` disposes of
/// it, `DO_NOTHING_ON_CLOSE` leaves it to the listeners and
/// `EXIT_ON_CLOSE` exits the application.
public class JFrame extends Frame implements WindowConstants, RootPaneContainer {

    public static final int EXIT_ON_CLOSE = 3;

    protected JRootPane rootPane;
    protected boolean rootPaneCheckingEnabled;

    private int defaultCloseOperation = HIDE_ON_CLOSE;

    public JFrame() {
        this("");
    }

    public JFrame(String title) {
        super(title);
        frameInit();
    }

    /// A frame on the screen of `gc`. There is one screen, so this is a
    /// frame like any other.
    public JFrame(GraphicsConfiguration gc) {
        this("");
    }

    public JFrame(String title, GraphicsConfiguration gc) {
        this(title);
    }

    protected void frameInit() {
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

    public JMenuBar getJMenuBar() {
        return rootPane.getJMenuBar();
    }

    /// Sets the menu bar; see `JRootPane.setJMenuBar` for how it shows.
    public void setJMenuBar(JMenuBar menubar) {
        rootPane.setJMenuBar(menubar);
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

    public int getDefaultCloseOperation() {
        return defaultCloseOperation;
    }

    public void setDefaultCloseOperation(int operation) {
        if (operation != DO_NOTHING_ON_CLOSE && operation != HIDE_ON_CLOSE && operation != DISPOSE_ON_CLOSE
                && operation != EXIT_ON_CLOSE) {
            throw new IllegalArgumentException("defaultCloseOperation must be one of: DO_NOTHING_ON_CLOSE, "
                    + "HIDE_ON_CLOSE, DISPOSE_ON_CLOSE, or EXIT_ON_CLOSE");
        }
        int old = defaultCloseOperation;
        defaultCloseOperation = operation;
        firePropertyChange("defaultCloseOperation", old, operation);
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            switch (defaultCloseOperation) {
                case HIDE_ON_CLOSE:
                    setVisible(false);
                    break;
                case DISPOSE_ON_CLOSE:
                    dispose();
                    break;
                case EXIT_ON_CLOSE:
                    WindowHosts.exit();
                    break;
                default:
                    break;
            }
        }
    }
}
