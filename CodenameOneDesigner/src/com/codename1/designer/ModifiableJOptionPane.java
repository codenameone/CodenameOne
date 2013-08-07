/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.designer;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.HeadlessException;
import javax.swing.*;

public class ModifiableJOptionPane extends JOptionPane {

    private boolean resizable = true;

    public ModifiableJOptionPane() {
        super();
    }

    /**
     * @param message
     */
    public ModifiableJOptionPane(Object message) {
        super(message);
    }

    /**
     * @param message
     * @param messageType
     */
    public ModifiableJOptionPane(Object message, int messageType) {
        super(message, messageType);
    }

    /**
     * @param message
     * @param messageType
     * @param optionType
     */
    public ModifiableJOptionPane(Object message, int messageType, int optionType) {
        super(message, messageType, optionType);
    }

    /**
     * @param message
     * @param messageType
     * @param optionType
     * @param icon
     */
    public ModifiableJOptionPane(Object message, int messageType, int optionType,
            Icon icon) {
        super(message, messageType, optionType, icon);
    }

    /**
     * @param message
     * @param messageType
     * @param optionType
     * @param icon
     * @param options
     */
    public ModifiableJOptionPane(Object message, int messageType, int optionType,
            Icon icon, Object[] options) {
        super(message, messageType, optionType, icon, options);
    }

    /**
     * @param message
     * @param messageType
     * @param optionType
     * @param icon
     * @param options
     * @param initialValue
     */
    public ModifiableJOptionPane(Object message, int messageType, int optionType,
            Icon icon, Object[] options, Object initialValue) {
        super(message, messageType, optionType, icon, options, initialValue);
    }

    /**
     * @see javax.swing.JOptionPane#createDialog(java.awt.Component,
     * java.lang.String)
     */
    public JDialog createDialog(Component parentComponent, String title)
            throws HeadlessException {
        JDialog dialog = super.createDialog(parentComponent, title);
        dialog.setResizable(isResizable());
        return dialog;
    }

    public static void reverseOKCancel(JButton ok, JButton cancel) {
        if(ResourceEditorApp.IS_MAC) {
            Container c = ok.getParent();
            c.remove(ok);
            c.add(ok);
        }
    }
    
    /**
     * @see javax.swing.JOptionPane#createInternalFrame(java.awt.Component,
     * java.lang.String)
     */
    public JInternalFrame createInternalFrame(Component parentComponent,
            String title) {
        JInternalFrame frame = super.createInternalFrame(parentComponent, title);
        frame.setMaximizable(true);
        frame.setResizable(isResizable());
        return frame;
    }

    public void setResizable(boolean b) {
        this.resizable = b;
    }

    public boolean isResizable() {
        return resizable;
    }

    public static int showConfirmDialog(Component parent, Object msg, String title) {
        //((JComponent)msg).setPreferredSize(new Dimension(800, 600));
        ModifiableJOptionPane mo = new ModifiableJOptionPane(msg);
        mo.setOptionType(OK_CANCEL_OPTION);
        mo.setMessageType(PLAIN_MESSAGE);
        JDialog d = mo.createDialog(parent, title);
        //this can probably be commented out, but first we should check the effect on Mac
        //d.setSize(new Dimension(800, 700));
        d.pack();
        d.setResizable(true);
        mo.selectInitialValue();
        d.show();
        d.dispose();

        Object selectedValue = mo.getValue();

        if (selectedValue == null) {
            return CLOSED_OPTION;
        }
        if (selectedValue instanceof Integer) {
            return ((Integer) selectedValue).intValue();
        }
        return CLOSED_OPTION;

    }
    
    public static void main(String [] args){
        JFrame f = new JFrame("aaaaa");
        f.setVisible(true);
        showConfirmDialog(f, new JLabel("aaaaaa"), "Hello");
    
    }
}