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
import com.codename1.desktopcompat.rt.CellTheme;
import com.codename1.desktopcompat.rt.EventBridge;
import com.codename1.desktopcompat.rt.Fonts;
import com.codename1.desktopcompat.rt.ScrollDelegate;

/// A table of look and feel defaults, and the look and feel itself.
///
/// ## The look and feel
///
/// There is one look, the Codename One theme. Setting a look and feel, by
/// any class name or with an object, changes nothing that is drawn and
/// never fails for a name: the usual
/// `UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())`
/// at the top of `main` runs as it is. A [LookAndFeel] object that is set
/// is remembered and answered by [#getLookAndFeel], and is not asked to
/// install anything. One look and feel is reported installed, under the
/// class name both `get...ClassName` methods answer.
///
/// ## The defaults
///
/// What an application `put` is what it gets back. A key that was never
/// put answers from the theme where the theme has something that means
/// the same:
///
///  - a key ending in `.background`, and `control`, `window`, `menu`: the
///    window background
///  - a key ending in `.foreground`, and `controlText`, `windowText`,
///    `menuText`, `textText`: the color of a label's text
///  - a key ending in `.font`: the default font
///  - a key ending in `.selectionBackground` or `.selectionForeground`,
///    and `textHighlight`, `textHighlightText`: the colors of a selected
///    table cell
///  - a key ending in `.gridColor`: the color of the lines of a table
///  - `ScrollBar.width`: the room a scroll pane gives a scroll bar, zero
///    on a touch device
///
/// Any other key gives `null`, or zero or false from the typed getters.
/// The widgets read what was put for the keys they know
/// (`Table.gridColor`, `List.selectionBackground` and so on) when they are
/// made; there are no borders, icons or strings of a look and feel to
/// find here unless the application put them.
public class UIManager {

    /// The defaults: what was put, and behind it the theme.
    private static final class ThemeDefaults extends UIDefaults {
        private static final long serialVersionUID = 1L;

        Object raw(Object key) {
            return key == null ? null : super.get(key);
        }

        @Override
        public Object get(Object key) {
            Object v = raw(key);
            if (v != null || !(key instanceof String)) {
                return v;
            }
            return fromTheme((String) key);
        }
    }

    /// The one look and feel there is, answered until another is set.
    private static final class ThemeLookAndFeel extends LookAndFeel {
        @Override
        public String getName() {
            return LOOK_NAME;
        }

        @Override
        public String getID() {
            return "CodenameOne";
        }

        @Override
        public String getDescription() {
            return "The Codename One theme";
        }

        @Override
        public boolean isNativeLookAndFeel() {
            return true;
        }

        @Override
        public boolean isSupportedLookAndFeel() {
            return true;
        }

        @Override
        public UIDefaults getDefaults() {
            return VALUES;
        }
    }

    /// The name and class name of a look and feel that is installed.
    public static class LookAndFeelInfo {
        private final String name;
        private final String className;

        public LookAndFeelInfo(String name, String className) {
            this.name = name;
            this.className = className;
        }

        public String getName() {
            return name;
        }

        public String getClassName() {
            return className;
        }

        @Override
        public String toString() {
            return getClass().getName() + "[" + getName() + " " + getClassName() + "]";
        }
    }

    private static final String LOOK_NAME = "Codename One";
    /// The class name of the built-in look and feel, so that the one
    /// reported installed is the one [#getLookAndFeel] answers: programs
    /// find the current look and feel among the installed ones by it.
    private static final String LOOK_CLASS = ThemeLookAndFeel.class.getName();
    private static final ThemeDefaults VALUES = new ThemeDefaults();
    private static final LookAndFeel BUILT_IN = new ThemeLookAndFeel();
    /// The look and feel that was set, or `null` for the built-in one.
    private static LookAndFeel current;
    private static LookAndFeelInfo[] installed = {new LookAndFeelInfo(LOOK_NAME, LOOK_CLASS)};

    public UIManager() {
    }

    /// What the theme has for a key nobody put.
    private static Object fromTheme(String k) {
        if (k.endsWith(".selectionBackground") || "textHighlight".equals(k)) {
            return CellTheme.selectionBackground(null);
        }
        if (k.endsWith(".selectionForeground") || "textHighlightText".equals(k)) {
            return CellTheme.selectionForeground(null);
        }
        if (k.endsWith(".gridColor")) {
            return CellTheme.grid(null);
        }
        if ("List.background".equals(k) || "Tree.background".equals(k) || "Table.background".equals(k)) {
            return CellTheme.background(null);
        }
        if ("Desktop.background".equals(k) || "desktop".equals(k)) {
            return CellTheme.desktopBackground();
        }
        if ("text".equals(k) || "TextArea.background".equals(k) || "TextField.background".equals(k)
                || "EditorPane.background".equals(k) || "TextPane.background".equals(k)
                || "FormattedTextField.background".equals(k) || "PasswordField.background".equals(k)) {
            return CellTheme.fieldBackground();
        }
        if ("ScrollPane.border".equals(k)) {
            return new com.codename1.desktopcompat.javax.swing.border.LineBorder(CellTheme.grid(null), 1);
        }
        if (k.endsWith(".background") || "control".equals(k) || "window".equals(k) || "menu".equals(k)) {
            return EventBridge.defaultBackground();
        }
        if (k.endsWith(".foreground") || "controlText".equals(k) || "windowText".equals(k) || "menuText".equals(k)
                || "textText".equals(k)) {
            return EventBridge.defaultForeground();
        }
        if (k.endsWith(".font")) {
            return Fonts.defaultFont();
        }
        if ("ScrollBar.width".equals(k)) {
            return Integer.valueOf(ScrollDelegate.barThickness());
        }
        return null;
    }

    /// The color an application put under a key, or `null`: never a value
    /// of the theme. This is what a widget asks, so that its own reading
    /// of the theme applies when the application said nothing.
    public static Color cn1PutColor(Object key) {
        Object v = VALUES.raw(key);
        return v instanceof Color ? (Color) v : null;
    }

    public static Object get(Object key) {
        return VALUES.get(key);
    }

    public static Object put(Object key, Object value) {
        return VALUES.put(key, value);
    }

    /// The table behind `get` and `put`.
    public static UIDefaults getDefaults() {
        return VALUES;
    }

    /// The same table as [#getDefaults]: there is no look and feel with
    /// defaults of its own beneath what the application put.
    public static UIDefaults getLookAndFeelDefaults() {
        return VALUES;
    }

    public static Color getColor(Object key) {
        return VALUES.getColor(key);
    }

    public static Font getFont(Object key) {
        return VALUES.getFont(key);
    }

    public static String getString(Object key) {
        return VALUES.getString(key);
    }

    public static int getInt(Object key) {
        return VALUES.getInt(key);
    }

    public static boolean getBoolean(Object key) {
        return VALUES.getBoolean(key);
    }

    public static Icon getIcon(Object key) {
        return VALUES.getIcon(key);
    }

    public static Border getBorder(Object key) {
        return VALUES.getBorder(key);
    }

    public static Insets getInsets(Object key) {
        return VALUES.getInsets(key);
    }

    public static Dimension getDimension(Object key) {
        return VALUES.getDimension(key);
    }

    /// Does nothing, whatever the name; see the class description.
    public static void setLookAndFeel(String className) {
    }

    /// Remembers the look and feel for [#getLookAndFeel]; `null` puts the
    /// built-in one back. Nothing that is drawn changes.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedLookAndFeelException`: if the look and feel itself
    ///   answers false from `isSupportedLookAndFeel`
    public static void setLookAndFeel(LookAndFeel newLookAndFeel) throws UnsupportedLookAndFeelException {
        if (newLookAndFeel != null && !newLookAndFeel.isSupportedLookAndFeel()) {
            throw new UnsupportedLookAndFeelException(newLookAndFeel + " not supported on this platform");
        }
        current = newLookAndFeel;
    }

    public static LookAndFeel getLookAndFeel() {
        return current == null ? BUILT_IN : current;
    }

    public static String getSystemLookAndFeelClassName() {
        return LOOK_CLASS;
    }

    public static String getCrossPlatformLookAndFeelClassName() {
        return LOOK_CLASS;
    }

    /// The look and feels there are to choose from: one, unless the
    /// application installed more names. The array is a copy.
    public static LookAndFeelInfo[] getInstalledLookAndFeels() {
        LookAndFeelInfo[] copy = new LookAndFeelInfo[installed.length];
        System.arraycopy(installed, 0, copy, 0, installed.length);
        return copy;
    }

    /// Replaces the list [#getInstalledLookAndFeels] answers. The names
    /// are only listed; setting one changes nothing.
    public static void setInstalledLookAndFeels(LookAndFeelInfo[] infos) {
        if (infos == null) {
            throw new NullPointerException("infos");
        }
        LookAndFeelInfo[] copy = new LookAndFeelInfo[infos.length];
        System.arraycopy(infos, 0, copy, 0, infos.length);
        installed = copy;
    }

    public static void installLookAndFeel(LookAndFeelInfo info) {
        LookAndFeelInfo[] now = getInstalledLookAndFeels();
        LookAndFeelInfo[] more = new LookAndFeelInfo[now.length + 1];
        System.arraycopy(now, 0, more, 0, now.length);
        more[now.length] = info;
        installed = more;
    }

    public static void installLookAndFeel(String name, String className) {
        installLookAndFeel(new LookAndFeelInfo(name, className));
    }
}
