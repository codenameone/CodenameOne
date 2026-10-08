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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.javax.swing.border.Border;
import java.util.Hashtable;

/// A table of defaults by key, with a getter for each type of value that
/// answers `null` (or zero, or false) when the key holds something else.
///
/// The table [UIManager#getDefaults] answers also knows the values the
/// Codename One theme stands in for; see [UIManager]. One made with `new`
/// holds only what is put into it.
///
/// What differs from the desktop: there are no lazy or active values, no
/// resource bundles and no UI classes; putting `null` removes the key, as
/// it does there.
public class UIDefaults extends Hashtable<Object, Object> {

    private static final long serialVersionUID = 7341222528856548117L;

    public UIDefaults() {
        super();
    }

    /// A table holding the pairs of `keyValueList`: a key, its value, the
    /// next key, and so on.
    public UIDefaults(Object[] keyValueList) {
        super();
        putDefaults(keyValueList);
    }

    @Override
    public Object put(Object key, Object value) {
        return value == null ? super.remove(key) : super.put(key, value);
    }

    public void putDefaults(Object[] keyValueList) {
        for (int i = 0; i + 1 < keyValueList.length; i += 2) {
            put(keyValueList[i], keyValueList[i + 1]);
        }
    }

    public Font getFont(Object key) {
        Object v = get(key);
        return v instanceof Font ? (Font) v : null;
    }

    public Color getColor(Object key) {
        Object v = get(key);
        return v instanceof Color ? (Color) v : null;
    }

    public Icon getIcon(Object key) {
        Object v = get(key);
        return v instanceof Icon ? (Icon) v : null;
    }

    public Border getBorder(Object key) {
        Object v = get(key);
        return v instanceof Border ? (Border) v : null;
    }

    public String getString(Object key) {
        Object v = get(key);
        return v instanceof String ? (String) v : null;
    }

    public int getInt(Object key) {
        Object v = get(key);
        return v instanceof Integer ? ((Integer) v).intValue() : 0;
    }

    public boolean getBoolean(Object key) {
        Object v = get(key);
        return v instanceof Boolean && ((Boolean) v).booleanValue();
    }

    public Insets getInsets(Object key) {
        Object v = get(key);
        return v instanceof Insets ? (Insets) v : null;
    }

    public Dimension getDimension(Object key) {
        Object v = get(key);
        return v instanceof Dimension ? (Dimension) v : null;
    }
}
