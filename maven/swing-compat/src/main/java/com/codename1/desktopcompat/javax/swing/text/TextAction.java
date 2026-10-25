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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.KeyboardFocusManager;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.javax.swing.AbstractAction;
import com.codename1.desktopcompat.javax.swing.Action;

import java.util.ArrayList;
import java.util.HashMap;

/// An action that works on whichever text component it is fired from: the
/// source of the event when that is a text component, the focused text
/// component otherwise. This is what lets one "copy" action serve every
/// text field of an application's menu.
public abstract class TextAction extends AbstractAction {

    public TextAction(String name) {
        super(name);
    }

    /// The text component the action should work on, or null.
    protected final JTextComponent getTextComponent(ActionEvent e) {
        if (e != null) {
            Object o = e.getSource();
            if (o instanceof JTextComponent) {
                return (JTextComponent) o;
            }
        }
        return getFocusedComponent();
    }

    /// `list1` with every action of `list2` added, an action of `list2`
    /// taking the place of one of the same name.
    public static final Action[] augmentList(Action[] list1, Action[] list2) {
        HashMap<String, Action> byName = new HashMap<String, Action>();
        ArrayList<String> order = new ArrayList<String>();
        cn1Collect(list1, byName, order);
        cn1Collect(list2, byName, order);
        Action[] out = new Action[order.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = byName.get(order.get(i));
        }
        return out;
    }

    private static void cn1Collect(Action[] list, HashMap<String, Action> byName, ArrayList<String> order) {
        for (int i = 0; list != null && i < list.length; i++) {
            Action a = list[i];
            Object value = a.getValue(Action.NAME);
            String name = value instanceof String ? (String) value : "";
            if (!byName.containsKey(name)) {
                order.add(name);
            }
            byName.put(name, a);
        }
    }

    /// The text component that has the focus, or null.
    protected final JTextComponent getFocusedComponent() {
        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (owner instanceof JTextComponent) {
            return (JTextComponent) owner;
        }
        return JTextComponent.cn1LastFocused();
    }
}
