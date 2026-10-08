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
package com.codename1.desktopcompat.javax.swing.text;

import com.codename1.desktopcompat.javax.swing.JComponent;

/// The base of the text components. The text lives in the Codename One
/// text widget once that exists; there is no document model, no caret and
/// no selection.
public abstract class JTextComponent extends JComponent {

    private String text = "";
    private boolean editable = true;

    public JTextComponent() {
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof com.codename1.ui.TextArea) {
            com.codename1.ui.TextArea t = (com.codename1.ui.TextArea) p;
            t.setText(text);
            t.setEditable(editable);
        }
    }

    public String getText() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof com.codename1.ui.TextArea) {
            String s = ((com.codename1.ui.TextArea) p).getText();
            text = s == null ? "" : s;
        }
        return text;
    }

    public void setText(String t) {
        text = t == null ? "" : t;
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof com.codename1.ui.TextArea) {
            ((com.codename1.ui.TextArea) p).setText(text);
        }
        repaint();
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean b) {
        boolean old = editable;
        editable = b;
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof com.codename1.ui.TextArea) {
            ((com.codename1.ui.TextArea) p).setEditable(b);
        }
        firePropertyChange("editable", old, b);
    }

    /// Does nothing: there is no selection.
    public void selectAll() {
    }
}
