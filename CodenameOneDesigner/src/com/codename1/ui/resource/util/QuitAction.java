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
import com.codename1.ui.util.EditableResources;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.EventObject;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import org.jdesktop.application.Application;

/**
 * Action representing generic application exit logic
 *
 * @author Shai Almog
 */
public class QuitAction extends AbstractAction implements WindowListener {
    public static QuitAction INSTANCE = new QuitAction();
    
    private EditableResources res;
    private QuitAction() {
        putValue(NAME, "Exit");
        putValue(SHORT_DESCRIPTION, "Exit");
        putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/com/codename1/designer/resources/exit.png")));
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        Application.getInstance().addExitListener(new Application.ExitListener() {

            @Override
            public boolean canExit(EventObject event) {
                if(res != null && res.isModified()) {
                    if(JOptionPane.showConfirmDialog(ResourceEditorApp.getApplication().getMainFrame(), "File was modified, do you want to exit without saving?", 
                        "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void willExit(EventObject event) {
            }
        });
    }
    
    public void setResource(EditableResources res) {
        this.res = res;
    }

    public void bindFrame(JFrame frm) {
        frm.addWindowListener(this);
        frm.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        Preferences pref = Preferences.userNodeForPackage(getClass());
        int width = pref.getInt("width", 600);
        int height = pref.getInt("height", 500);
        int xLocation = pref.getInt("xLocation", -1);
        int yLocation = pref.getInt("yLocation", -1);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        if(xLocation < 0 || yLocation < 0 || width < 10 || height < 10 ||
                width > d.width || height > d.height) {
            frm.setLocationByPlatform(true);
            frm.setSize(600, 500);
        } else {
            frm.setBounds(xLocation, yLocation, width, height);
        }
        
        frm.addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent e) {
                if (e.getComponent() instanceof JFrame) {
                    JFrame f = (JFrame)e.getComponent();
                    if ((f.getExtendedState() & JFrame.MAXIMIZED_BOTH) == 0) {
                        Preferences pref = Preferences.userNodeForPackage(getClass());
                        Rectangle r = f.getBounds();
                        pref.putInt("width", r.width);
                        pref.putInt("height", r.height);
                        pref.putInt("xLocation", r.x);
                        pref.putInt("yLocation", r.y);
                    }
                }
            }

            public void componentMoved(ComponentEvent e) {
            }

            public void componentShown(ComponentEvent e) {
            }

            public void componentHidden(ComponentEvent e) {
            }
        });
        
        frm.validate();
    }
    
    public boolean quit() {
        Application.getInstance().exit();
        return false;
    }

    public void actionPerformed(ActionEvent e) {
        Application.getInstance().exit();
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        actionPerformed(null);
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }
}
