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
package com.codename1.desktopcompat.org.jdesktop.swingx.renderer;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.javax.swing.JList;

/// The context of a list cell.
public class ListCellContext extends CellContext {

    public ListCellContext() {
    }

    public void installContext(JList component, Object value, int row, int column, boolean selected,
            boolean focused, boolean expanded, boolean leaf) {
        this.component = component;
        installState(value, row, column, selected, focused, expanded, leaf);
    }

    @Override
    public JList getComponent() {
        return component instanceof JList ? (JList) component : null;
    }

    @Override
    protected Color getSelectionBackground() {
        JList l = getComponent();
        return l != null ? l.getSelectionBackground() : null;
    }

    @Override
    protected Color getSelectionForeground() {
        JList l = getComponent();
        return l != null ? l.getSelectionForeground() : null;
    }

    @Override
    protected String getUIPrefix() {
        return "List.";
    }
}
