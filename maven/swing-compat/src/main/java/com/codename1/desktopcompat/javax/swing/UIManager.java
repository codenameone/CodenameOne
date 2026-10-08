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
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.rt.EventBridge;
import com.codename1.desktopcompat.rt.Fonts;
import java.util.HashMap;

/// A table of look and feel defaults.
///
/// There is one look, the Codename One theme, so setting a look and feel
/// does nothing. A key that was never `put` answers from the theme by its
/// ending: `.background`, `.foreground` and `.font` give the window
/// background, the label color and the default font; any other key gives
/// `null`.
public class UIManager {

    private static final HashMap<Object, Object> VALUES = new HashMap<Object, Object>();

    public UIManager() {
    }

    public static Object get(Object key) {
        Object v = VALUES.get(key);
        if (v != null || !(key instanceof String)) {
            return v;
        }
        String k = (String) key;
        if (k.endsWith(".background")) {
            return EventBridge.defaultBackground();
        }
        if (k.endsWith(".foreground")) {
            return EventBridge.defaultForeground();
        }
        if (k.endsWith(".font")) {
            return Fonts.defaultFont();
        }
        return null;
    }

    public static Object put(Object key, Object value) {
        return value == null ? VALUES.remove(key) : VALUES.put(key, value);
    }

    public static Color getColor(Object key) {
        Object v = get(key);
        return v instanceof Color ? (Color) v : null;
    }

    public static Font getFont(Object key) {
        Object v = get(key);
        return v instanceof Font ? (Font) v : null;
    }

    public static String getString(Object key) {
        Object v = get(key);
        return v instanceof String ? (String) v : null;
    }

    public static int getInt(Object key) {
        Object v = get(key);
        return v instanceof Integer ? ((Integer) v).intValue() : 0;
    }

    public static boolean getBoolean(Object key) {
        Object v = get(key);
        return v instanceof Boolean && ((Boolean) v).booleanValue();
    }

    public static Icon getIcon(Object key) {
        Object v = get(key);
        return v instanceof Icon ? (Icon) v : null;
    }

    /// Does nothing; see the class description.
    public static void setLookAndFeel(String className) {
    }

    public static String getSystemLookAndFeelClassName() {
        return "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    public static String getCrossPlatformLookAndFeelClassName() {
        return "javax.swing.plaf.metal.MetalLookAndFeel";
    }
}
