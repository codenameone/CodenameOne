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

import com.codename1.desktopcompat.rt.CheckBoxPeer;

/// A menu item that is checked or not: a Codename One check box in a
/// popup menu, a command whose name carries a check mark in a menu bar.
public class JCheckBoxMenuItem extends JMenuItem {

    public JCheckBoxMenuItem() {
        this(null, null, false);
    }

    public JCheckBoxMenuItem(Icon icon) {
        this(null, icon, false);
    }

    public JCheckBoxMenuItem(String text) {
        this(text, null, false);
    }

    public JCheckBoxMenuItem(Action a) {
        this();
        setAction(a);
    }

    public JCheckBoxMenuItem(String text, Icon icon) {
        this(text, icon, false);
    }

    public JCheckBoxMenuItem(String text, boolean b) {
        this(text, null, b);
    }

    public JCheckBoxMenuItem(String text, Icon icon, boolean b) {
        super(text, icon);
        setModel(new JToggleButton.ToggleButtonModel());
        setSelected(b);
    }

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new CheckBoxPeer(this);
    }

    public boolean getState() {
        return isSelected();
    }

    public void setState(boolean b) {
        setSelected(b);
    }

    @Override
    public Object[] getSelectedObjects() {
        return isSelected() ? new Object[]{getText()} : null;
    }
}
