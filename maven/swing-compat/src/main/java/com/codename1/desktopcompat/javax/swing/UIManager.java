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

import com.codename1.desktopcompat.com.formdev.flatlaf.FlatLaf;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.java.beans.PropertyChangeSupport;
import com.codename1.desktopcompat.javax.swing.border.Border;
import com.codename1.desktopcompat.rt.CellTheme;
import com.codename1.desktopcompat.rt.EventBridge;
import com.codename1.desktopcompat.rt.Fonts;
import com.codename1.desktopcompat.rt.LafTheme;
import com.codename1.desktopcompat.rt.ScrollDelegate;

/// A table of look and feel defaults, and the look and feel itself.
///
/// ## The look and feel
///
/// The widgets of the layer are Codename One components drawn with the
/// Codename One theme; there are no UI delegates. What a look and feel
/// chooses here is the *palette*: light or dark. Setting one never fails
/// for a name, restyles every open window at once, and fires the
/// `lookAndFeel` property change:
///
///  - a dark one -- FlatLaf's `FlatDarkLaf`, `FlatDarculaLaf`,
///    `FlatMacDarkLaf`, any subclass of `FlatLaf` whose `isDark()` answers
///    true, any class whose name says dark (`Dark`, `Darcula`, `Dracula`,
///    `Night`, `Monokai` and the like) -- selects the dark palette
///  - a light one -- the other FlatLaf classes, Metal, Nimbus, Motif, the
///    Windows, Aqua and GTK names, a name that says light -- the light one
///  - the built-in look, which is what both `get...ClassName` methods
///    name, and `null`, follow the platform's own dark mode
///  - any other keeps the palette as it is and says so once in the log
///
/// The object that was set is answered by [#getLookAndFeel]; a class name
/// is answered by an instance of that class when the application has it,
/// and otherwise by a stand-in that carries the name. `initialize` and
/// `uninitialize` are called as on the desktop, and what a look and feel
/// of the application's own answers from `getDefaults` becomes the look
/// and feel defaults.
///
/// ## The defaults
///
/// There are two tables, as on the desktop. What an application `put`
/// is in the developer defaults, wins, and stays when the look and feel
/// changes. [#getLookAndFeelDefaults] is the table of the look and feel,
/// emptied when another is set. A key in neither answers from the theme
/// and the palette:
///
///  - a key ending in `.background`, and `control`, `window`, `menu`: the
///    window background; content areas (`text`, `List.background`,
///    `TextField.background` ...) the field background
///  - a key ending in `.foreground`, and `controlText`, `windowText`,
///    `menuText`, `textText`: the color of a label's text
///  - a key ending in `.font`: the default font; `defaultFont`, and
///    FlatLaf's `h1.font` ... `h4.font`, `large.font`, `small.font`,
///    `mini.font`, `monospaced.font`
///  - a key ending in `.selectionBackground` or `.selectionForeground`,
///    and `textHighlight`, `textHighlightText`: the colors of a selected
///    table cell
///  - a key ending in `.gridColor`, `.borderColor`, and
///    `Separator.foreground`, `controlShadow`: the color of lines
///  - a key ending in `.disabledForeground`, `.inactiveForeground`:
///    the color of disabled text
///  - FlatLaf's `Component.accentColor`, `Component.focusColor`,
///    `Component.linkColor`, `Component.error...`, `Actions.Red` and the
///    other `Actions.`/`Objects.` colors, `laf.dark`
///  - `TextField.border`, `Button.border`, `ScrollPane.border` and their
///    kin: a line in the palette's line color that follows the palette
///  - the margins and metrics `Button.margin`, `TextField.margin`,
///    `Component.arc`, `Table.rowHeight` and the like
///  - `ScrollBar.width`: the room a scroll pane gives a scroll bar, zero
///    on a touch device
///
/// Any other key gives `null`, or zero or false from the typed getters.
/// The widgets read what was put for the keys they know
/// (`Table.gridColor`, `List.selectionBackground` and so on) when they are
/// made; there are no icons or strings of a look and feel to find here
/// unless the application put them.
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
            if (v != null || key == null) {
                return v;
            }
            return LAF.get(key);
        }
    }

    /// The defaults of the look and feel: what it or the application put
    /// there, and behind that the theme.
    private static final class LafDefaults extends UIDefaults {
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

    /// A look and feel known by its name alone.
    private static final class NamedLookAndFeel extends LookAndFeel {
        private final String name;
        private final String className;

        NamedLookAndFeel(String name, String className) {
            this.name = name;
            this.className = className;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getID() {
            return name;
        }

        @Override
        public String getDescription() {
            return name + " (" + className + "), as a palette of the Codename One theme";
        }

        @Override
        public boolean isNativeLookAndFeel() {
            return false;
        }

        @Override
        public boolean isSupportedLookAndFeel() {
            return true;
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
            return LAF;
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
    private static final LafDefaults LAF = new LafDefaults();
    private static final ThemeDefaults VALUES = new ThemeDefaults();
    private static final PropertyChangeSupport CHANGES = new PropertyChangeSupport(UIManager.class);
    private static final String LAYER = "com.codename1.desktopcompat.";
    /// Words in a class name that say a look is dark, and that it is
    /// light; the dark ones are asked first.
    private static final String[] DARK_WORDS = {"dark", "darcula", "dracula", "night", "monokai", "carbon", "cobalt",
        "nord", "gruvbox", "hiberbee", "spacegray", "vuesion", "oceanic", "contrast", "black", "onedark"};
    private static final String[] LIGHT_WORDS = {"light", "metal", "nimbus", "windows", "aqua", "gtk", "motif",
        "intellij", "synth", "basic", "multi", "white", "cyan", "gray", "grey"};
    private static final LookAndFeel BUILT_IN = new ThemeLookAndFeel();
    /// The look and feel that was set, or `null` for the built-in one.
    private static LookAndFeel current;
    private static LookAndFeelInfo[] installed = {new LookAndFeelInfo(LOOK_NAME, LOOK_CLASS)};

    public UIManager() {
    }

    /// What the theme has for a key nobody put.
    private static Object fromTheme(String k) {
        Object known = LafTheme.value(k);
        if (known != null) {
            return known;
        }
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
        if (v == null) {
            v = LAF.raw(key);
        }
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

    /// The defaults of the look and feel, beneath what the application
    /// put into [#getDefaults]. They are emptied when a look and feel is
    /// set; what is put here does not survive that.
    public static UIDefaults getLookAndFeelDefaults() {
        return LAF;
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

    private static boolean says(String name, String[] words) {
        for (int w = 0; w < words.length; w++) {
            String word = words[w];
            for (int i = 0; i + word.length() <= name.length(); i++) {
                if (name.regionMatches(true, i, word, 0, word.length())) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Whether a look and feel is dark, light, or not known: `TRUE`,
    /// `FALSE`, `null`.
    private static Boolean dark(LookAndFeel laf, String className) {
        if (laf instanceof FlatLaf) {
            return Boolean.valueOf(((FlatLaf) laf).isDark());
        }
        String simple = className.substring(className.lastIndexOf('.') + 1);
        if (says(simple, DARK_WORDS)) {
            return Boolean.TRUE;
        }
        if (says(simple, LIGHT_WORDS) || says(className, LIGHT_WORDS)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static LookAndFeel instance(String className) {
        // The layer's own class of that name first: an application names
        // a look and feel by the name it has on the desktop.
        String[] names = {LAYER + className, className};
        for (int i = 0; i < names.length; i++) {
            try {
                Object o = Class.forName(names[i]).newInstance();
                if (o instanceof LookAndFeel) {
                    return (LookAndFeel) o;
                }
            } catch (ClassNotFoundException e) {
                continue;
            } catch (InstantiationException e) {
                continue;
            } catch (IllegalAccessException e) {
                continue;
            } catch (LinkageError e) {
                continue;
            }
        }
        return null;
    }

    /// Sets the look and feel of that class name and never fails for a
    /// name; see the class description for what each name selects.
    public static void setLookAndFeel(String className) {
        if (className == null || LOOK_CLASS.equals(className)) {
            install(null, LOOK_CLASS);
            return;
        }
        LookAndFeel laf = instance(className);
        if (laf == null) {
            String simple = className.substring(className.lastIndexOf('.') + 1);
            if (simple.endsWith("LookAndFeel") && simple.length() > 11) {
                simple = simple.substring(0, simple.length() - 11);
            }
            laf = new NamedLookAndFeel(simple, className);
        } else if (!laf.isSupportedLookAndFeel()) {
            return;
        }
        install(laf, className);
    }

    /// Sets the look and feel; `null` puts the built-in one back, which
    /// follows the platform's dark mode. See the class description.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedLookAndFeelException`: if the look and feel itself
    ///   answers false from `isSupportedLookAndFeel`
    public static void setLookAndFeel(LookAndFeel newLookAndFeel) throws UnsupportedLookAndFeelException {
        if (newLookAndFeel != null && !newLookAndFeel.isSupportedLookAndFeel()) {
            throw new UnsupportedLookAndFeelException(newLookAndFeel + " not supported on this platform");
        }
        install(newLookAndFeel == BUILT_IN ? null : newLookAndFeel,
                newLookAndFeel == null ? LOOK_CLASS : newLookAndFeel.getClass().getName());
    }

    private static void install(LookAndFeel laf, String className) {
        LookAndFeel old = getLookAndFeel();
        if (current != null && current != laf) {
            current.uninitialize();
        }
        LAF.clear();
        if (laf != null && laf != current) {
            laf.initialize();
        }
        current = laf;
        boolean select = true;
        Boolean dark = null;
        if (laf != null) {
            UIDefaults own = laf.getDefaults();
            if (own != null && own != LAF && own != VALUES) {
                LAF.putAll(own);
            }
            dark = dark(laf, className);
            if (dark == null) {
                select = false;
                LafTheme.unknown(className);
            }
        }
        if (select) {
            LafTheme.select(dark);
        } else {
            LafTheme.restyleAll();
        }
        LookAndFeel now = getLookAndFeel();
        if (old != now) {
            CHANGES.firePropertyChange("lookAndFeel", old, now);
        }
    }

    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        CHANGES.addPropertyChangeListener(listener);
    }

    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        CHANGES.removePropertyChangeListener(listener);
    }

    public static PropertyChangeListener[] getPropertyChangeListeners() {
        return CHANGES.getPropertyChangeListeners();
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

    /// Replaces the list [#getInstalledLookAndFeels] answers.
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
