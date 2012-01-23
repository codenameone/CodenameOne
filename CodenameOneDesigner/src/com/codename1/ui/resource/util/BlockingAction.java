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

import com.codename1.designer.ResourceEditorApp;
import com.codename1.designer.ResourceEditorView;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.lang.reflect.InvocationTargetException;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Abstract action that blocks the UI until it completes
 *
 * @author Shai Almog
 */
public abstract class BlockingAction extends AbstractAction implements Runnable {
    private static int rotation;
    private Timer t;
    private java.awt.Component glassPane;
    public final void actionPerformed(ActionEvent e) {
        RootPaneContainer r = (RootPaneContainer)ResourceEditorApp.getApplication().getMainFrame();
        glassPane = r.getGlassPane();
        final ImageIcon progress = new ImageIcon(getClass().getResource("/progress.gif"));
        final JComponent c = new JLabel(progress);
        c.addMouseListener(new MouseAdapter() {});
        c.addKeyListener(new KeyAdapter() {});
        r.setGlassPane(c);
        c.setVisible(true);
        t = new Timer(100, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                rotation += 10;
                if(rotation > 359) {
                    rotation = 0;
                }
                c.repaint();
            }
        });
        t.setRepeats(true);
        t.start();
        start();
        new Thread(this).start();
    }
    
    public void start() {
    }

    public abstract void exectute();

    public void afterComplete() {
    }
    
    public final void run() {
        try {
            exectute();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    afterComplete();
                }
            });
        } catch(Exception err) {
            err.printStackTrace();
        } finally {
            t.stop();
            RootPaneContainer r = (RootPaneContainer)ResourceEditorApp.getApplication().getMainFrame();
            r.setGlassPane(glassPane);
        }
    }
}
