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

import java.util.Enumeration;
import java.util.Vector;

/// Makes a set of buttons mutually exclusive: at most one of them is
/// selected, and selecting another deselects it.
///
/// Once a button of the group is selected the group cannot be left without a
/// selection by deselecting that button, only by `clearSelection()`.
public class ButtonGroup {

    /// The buttons in the group.
    protected Vector<AbstractButton> buttons = new Vector<AbstractButton>();

    private ButtonModel selection;

    public ButtonGroup() {
    }

    public void add(AbstractButton b) {
        if (b == null) {
            return;
        }
        buttons.addElement(b);
        if (b.isSelected()) {
            if (selection == null) {
                selection = b.getModel();
            } else {
                b.setSelected(false);
            }
        }
        b.getModel().setGroup(this);
    }

    public void remove(AbstractButton b) {
        if (b == null) {
            return;
        }
        buttons.removeElement(b);
        if (b.getModel() == selection) {
            selection = null;
        }
        b.getModel().setGroup(null);
    }

    public void clearSelection() {
        if (selection != null) {
            ButtonModel oldSelection = selection;
            selection = null;
            oldSelection.setSelected(false);
        }
    }

    public Enumeration<AbstractButton> getElements() {
        return buttons.elements();
    }

    public ButtonModel getSelection() {
        return selection;
    }

    public void setSelected(ButtonModel m, boolean b) {
        if (b && m != null && m != selection) {
            ButtonModel oldSelection = selection;
            selection = m;
            if (oldSelection != null) {
                oldSelection.setSelected(false);
            }
            m.setSelected(true);
        }
    }

    public boolean isSelected(ButtonModel m) {
        return m == selection;
    }

    public int getButtonCount() {
        return buttons.size();
    }
}
