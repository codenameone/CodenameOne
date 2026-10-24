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
import com.codename1.desktopcompat.javax.swing.text.Document;
import com.codename1.desktopcompat.javax.swing.text.JTextComponent;
import com.codename1.desktopcompat.javax.swing.text.PlainDocument;
import com.codename1.desktopcompat.rt.TextFieldPeer;

/// A single line of editable text, shown and edited by a Codename One
/// text field over a [PlainDocument].
///
/// Finishing the edit -- the Enter or Done key of the device's keyboard
/// -- notifies the action listeners. The columns size the field and the
/// horizontal alignment is honoured. Line breaks in inserted text become
/// spaces. Not supported: the scroll offset and horizontal visibility
/// model, and binding an `Action`.
public class JTextField extends JTextComponent implements SwingConstants {

    public static final String notifyAction = "notify-field-accept";

    private int columns;
    private int horizontalAlignment = LEADING;
    private String command;

    public JTextField() {
        this(null, null, 0);
    }

    public JTextField(String text) {
        this(null, text, 0);
    }

    public JTextField(int columns) {
        this(null, null, columns);
    }

    public JTextField(String text, int columns) {
        this(null, text, columns);
    }

    public JTextField(Document doc, String text, int columns) {
        if (columns < 0) {
            throw new IllegalArgumentException("columns less than zero.");
        }
        this.columns = columns;
        if (doc == null) {
            doc = createDefaultModel();
        }
        setDocument(doc);
        if (text != null) {
            setText(text);
        }
    }

    /// The document a field starts with: plain text without line breaks.
    protected Document createDefaultModel() {
        return new PlainDocument();
    }

    @Override
    public void setDocument(Document doc) {
        if (doc != null) {
            doc.putProperty("filterNewlines", Boolean.TRUE);
        }
        super.setDocument(doc);
    }

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        TextFieldPeer p = new TextFieldPeer(this);
        p.setSingleLineTextArea(true);
        if (columns > 0) {
            p.setColumns(columns);
        }
        p.setAlignment(JLabel.nativeAlignment(horizontalAlignment));
        p.addActionListener(new com.codename1.ui.events.ActionListener<com.codename1.ui.events.ActionEvent>() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                cn1PullText();
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

    /// As on the desktop a field can be far narrower than it prefers --
    /// there its minimum is little more than its own edges, here it is as
    /// wide as it is tall -- so a row that is short of room narrows its
    /// fields before it pushes anything out of sight.
    @Override
    public Dimension getMinimumSize() {
        Dimension d = super.getMinimumSize();
        if (isMinimumSizeSet()) {
            return d;
        }
        return new Dimension(Math.min(d.width, d.height), d.height);
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

    /// The width of one column: that of the letter m in the field's font.
    protected int getColumnWidth() {
        return getFontMetrics(getFont()).charWidth('m');
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(int alignment) {
        if (alignment == horizontalAlignment) {
            return;
        }
        if (alignment != LEFT && alignment != CENTER && alignment != RIGHT && alignment != LEADING
                && alignment != TRAILING) {
            throw new IllegalArgumentException("horizontalAlignment");
        }
        int old = horizontalAlignment;
        horizontalAlignment = alignment;
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof com.codename1.ui.TextArea) {
            ((com.codename1.ui.TextArea) p).setAlignment(JLabel.nativeAlignment(alignment));
        }
        firePropertyChange("horizontalAlignment", old, alignment);
        repaint();
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

    @Override
    protected String paramString() {
        return super.paramString() + ",columns=" + columns;
    }
}
