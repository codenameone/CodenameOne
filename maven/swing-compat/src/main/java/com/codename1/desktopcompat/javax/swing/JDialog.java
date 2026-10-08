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
import com.codename1.desktopcompat.java.awt.Dialog;
import com.codename1.desktopcompat.java.awt.Frame;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.awt.event.WindowEvent;

/// The Swing dialog, a skeleton: a root pane and a content pane like a
/// frame's, shown as a form. A modal dialog does not block its caller.
public class JDialog extends Dialog implements WindowConstants {

    protected JRootPane rootPane;
    protected boolean rootPaneCheckingEnabled;

    private int defaultCloseOperation = HIDE_ON_CLOSE;

    public JDialog() {
        this((Frame) null, "", false);
    }

    public JDialog(Frame owner) {
        this(owner, "", false);
    }

    public JDialog(Frame owner, boolean modal) {
        this(owner, "", modal);
    }

    public JDialog(Frame owner, String title) {
        this(owner, title, false);
    }

    public JDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        dialogInit();
    }

    protected void dialogInit() {
        setRootPane(createRootPane());
        setRootPaneCheckingEnabled(true);
    }

    protected JRootPane createRootPane() {
        return new JRootPane();
    }

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

    public Container getContentPane() {
        return rootPane.getContentPane();
    }

    public void setContentPane(Container contentPane) {
        rootPane.setContentPane(contentPane);
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
        if (operation != DO_NOTHING_ON_CLOSE && operation != HIDE_ON_CLOSE && operation != DISPOSE_ON_CLOSE) {
            throw new IllegalArgumentException("defaultCloseOperation must be one of: DO_NOTHING_ON_CLOSE, "
                    + "HIDE_ON_CLOSE, or DISPOSE_ON_CLOSE");
        }
        int old = defaultCloseOperation;
        defaultCloseOperation = operation;
        firePropertyChange("defaultCloseOperation", old, operation);
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            if (defaultCloseOperation == HIDE_ON_CLOSE) {
                setVisible(false);
            } else if (defaultCloseOperation == DISPOSE_ON_CLOSE) {
                dispose();
            }
        }
    }
}
