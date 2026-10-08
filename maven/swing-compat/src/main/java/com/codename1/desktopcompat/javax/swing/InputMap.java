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

import java.util.ArrayList;
import java.util.HashMap;

/// Maps key strokes to the names of actions, with a parent that is asked
/// for the strokes this map does not hold.
public class InputMap implements java.io.Serializable {

    private HashMap<KeyStroke, Object> table;
    private InputMap parent;

    public InputMap() {
    }

    public void setParent(InputMap map) {
        parent = map;
    }

    public InputMap getParent() {
        return parent;
    }

    /// Binds the stroke to the name; a `null` name removes the binding.
    public void put(KeyStroke keyStroke, Object actionMapKey) {
        if (keyStroke == null) {
            return;
        }
        if (actionMapKey == null) {
            remove(keyStroke);
            return;
        }
        if (table == null) {
            table = new HashMap<KeyStroke, Object>();
        }
        table.put(keyStroke, actionMapKey);
    }

    public Object get(KeyStroke keyStroke) {
        Object v = table == null ? null : table.get(keyStroke);
        if (v == null && parent != null) {
            return parent.get(keyStroke);
        }
        return v;
    }

    public void remove(KeyStroke key) {
        if (table != null) {
            table.remove(key);
        }
    }

    public void clear() {
        if (table != null) {
            table.clear();
        }
    }

    /// The strokes bound in this map itself, or `null` when there are none.
    public KeyStroke[] keys() {
        if (table == null || table.isEmpty()) {
            return null;
        }
        return table.keySet().toArray(new KeyStroke[table.size()]);
    }

    public int size() {
        return table == null ? 0 : table.size();
    }

    /// The strokes bound in this map and its parents, or `null` when
    /// there are none.
    public KeyStroke[] allKeys() {
        ArrayList<KeyStroke> all = new ArrayList<KeyStroke>();
        for (InputMap m = this; m != null; m = m.parent) {
            KeyStroke[] ks = m.keys();
            if (ks != null) {
                for (int i = 0; i < ks.length; i++) {
                    if (!all.contains(ks[i])) {
                        all.add(ks[i]);
                    }
                }
            }
        }
        return all.isEmpty() ? null : all.toArray(new KeyStroke[all.size()]);
    }
}
