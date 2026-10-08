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

import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.javax.swing.text.JTextComponent;
import com.codename1.desktopcompat.rt.TextFieldPeer;

/// A single line of editable text, shown and edited by a Codename One
/// text field. Finishing the edit notifies the action listeners.
public class JTextField extends JTextComponent implements SwingConstants {

    public static final String notifyAction = "notify-field-accept";

    private int columns;
    private int horizontalAlignment = LEADING;
    private String command;

    public JTextField() {
        this(null, 0);
    }

    public JTextField(String text) {
        this(text, 0);
    }

    public JTextField(int columns) {
        this(null, columns);
    }

    public JTextField(String text, int columns) {
        if (columns < 0) {
            throw new IllegalArgumentException("columns less than zero.");
        }
        this.columns = columns;
        if (text != null) {
            setText(text);
        }
    }

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        TextFieldPeer p = new TextFieldPeer(this);
        p.setSingleLineTextArea(true);
        if (columns > 0) {
            p.setColumns(columns);
        }
        p.addActionListener(new com.codename1.ui.events.ActionListener<com.codename1.ui.events.ActionEvent>() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                postActionEvent();
            }
        });
        return p;
    }

    @Override
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        if (columns < 0) {
            throw new IllegalArgumentException("columns less than zero.");
        }
        if (columns != this.columns) {
            this.columns = columns;
            com.codename1.ui.Component p = cn1PeerOrNull();
            if (p instanceof com.codename1.ui.TextArea && columns > 0) {
                ((com.codename1.ui.TextArea) p).setColumns(columns);
            }
            revalidate();
        }
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    /// Recorded only.
    public void setHorizontalAlignment(int alignment) {
        horizontalAlignment = alignment;
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    public void setActionCommand(String command) {
        this.command = command;
    }

    protected void fireActionPerformed() {
        ActionListener[] ls = getActionListeners();
        if (ls.length == 0) {
            return;
        }
        ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command != null ? command : getText(),
                System.currentTimeMillis(), 0);
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].actionPerformed(e);
        }
    }

    public void postActionEvent() {
        fireActionPerformed();
    }
}
