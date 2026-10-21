/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl;

import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.plaf.UIManager;

/// The menu conventions of each desktop platform: where a standard command (a `Command`
/// desktop role) goes, what it is called and which shortcut it carries. The menu encoder
/// resolves every role through this table, so the ports only ever see ordinary menu paths and
/// a platform's conventions are written down once rather than in each native menu parser.
///
/// This is an implementation class; applications use `Command#setDesktopRole(String)`.
public final class NativeMenuConventions {
    /// The macOS convention, also used by Mac Catalyst.
    public static final String MAC = "mac";
    /// The Windows convention.
    public static final String WINDOWS = "win";
    /// The GNOME convention, used on Linux.
    public static final String LINUX = "linux";
    /// The in-page menu of the JavaScript port. It follows the Windows layout, without the
    /// commands a browser page cannot perform and the shortcuts the browser keeps for itself.
    public static final String WEB = "web";

    private static final int P = Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY;
    private static final int S = Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT;

    // Groups of the standard items relative to the application's own items, which default to
    // group 0: the standard ones open and close their menu the way each platform's own
    // applications do, and the application's items land in between.
    private static final int TOP = -300;
    private static final int UPPER = -200;
    private static final int HIGH = -100;
    private static final int LOW = 100;
    private static final int LAST = 1000;

    private static final String ELLIPSIS = "\u2026";

    /// One role as one platform places it.
    static final class Entry {
        final String menu;
        final int group;
        final String label;
        final boolean appendAppName;
        final int key;
        final int modifiers;

        Entry(String menu, int group, String label, boolean appendAppName, int key, int modifiers) {
            this.menu = menu;
            this.group = group;
            this.label = label;
            this.appendAppName = appendAppName;
            this.key = key;
            this.modifiers = modifiers;
        }
    }

    // The role ordinals the wire format carries (see NativeMenuEncoder#FLAG_ROLE_SHIFT), so a
    // port can put the item where its own stock item of that role is. The values are part of
    // the wire format: never renumber one.
    private static final String[] ORDINALS = {
        null,
        Command.DESKTOP_ROLE_ABOUT,
        Command.DESKTOP_ROLE_HIDE_APP,
        Command.DESKTOP_ROLE_QUIT,
        Command.DESKTOP_ROLE_UNDO,
        Command.DESKTOP_ROLE_REDO,
        Command.DESKTOP_ROLE_CUT,
        Command.DESKTOP_ROLE_COPY,
        Command.DESKTOP_ROLE_PASTE,
        Command.DESKTOP_ROLE_SELECT_ALL,
        Command.DESKTOP_ROLE_MINIMIZE,
        Command.DESKTOP_ROLE_ZOOM,
        Command.DESKTOP_ROLE_FULL_SCREEN,
        Command.DESKTOP_ROLE_SETTINGS,
        Command.DESKTOP_ROLE_NEW,
        Command.DESKTOP_ROLE_OPEN,
        Command.DESKTOP_ROLE_SAVE,
        Command.DESKTOP_ROLE_SAVE_AS,
        Command.DESKTOP_ROLE_CLOSE,
        Command.DESKTOP_ROLE_PRINT,
        Command.DESKTOP_ROLE_FIND,
        Command.DESKTOP_ROLE_HELP,
    };

    private NativeMenuConventions() {
    }

    /// The convention of a platform, by the name `Display#getPlatformName()` reports.
    ///
    /// #### Parameters
    ///
    /// - `platformName`: the platform name, may be null
    ///
    /// #### Returns
    ///
    /// one of `#MAC`, `#WINDOWS`, `#LINUX` and `#WEB`
    public static String forPlatform(String platformName) {
        if (platformName == null) {
            return WINDOWS;
        }
        if ("mac".equalsIgnoreCase(platformName) || "ios".equalsIgnoreCase(platformName)) {
            return MAC;
        }
        if ("linux".equalsIgnoreCase(platformName)) {
            return LINUX;
        }
        if ("html5".equalsIgnoreCase(platformName) || WEB.equalsIgnoreCase(platformName)) {
            return WEB;
        }
        return WINDOWS;
    }

    /// The convention of the running platform.
    ///
    /// #### Returns
    ///
    /// one of `#MAC`, `#WINDOWS`, `#LINUX` and `#WEB`
    public static String current() {
        if (!Display.isInitialized()) {
            return WINDOWS;
        }
        return forPlatform(Display.getInstance().getPlatformName());
    }

    /// The ordinal a role travels as on the wire, 0 for none.
    ///
    /// #### Parameters
    ///
    /// - `role`: the role, may be null
    ///
    /// #### Returns
    ///
    /// the ordinal, 0 for no role
    public static int ordinalOf(String role) {
        if (role != null) {
            for (int i = 1; i < ORDINALS.length; i++) {
                if (ORDINALS[i].equals(role)) {
                    return i;
                }
            }
        }
        return 0;
    }

    /// The label a platform gives a role, localized through the `cn1.menu.<role>` resource
    /// key, or null when the platform has no such command.
    ///
    /// #### Parameters
    ///
    /// - `convention`: one of `#MAC`, `#WINDOWS`, `#LINUX` and `#WEB`
    ///
    /// - `role`: one of the `Command` desktop role constants
    ///
    /// #### Returns
    ///
    /// the label or null
    public static String labelOf(String convention, String role) {
        Entry e = entry(convention, role);
        if (e == null) {
            return null;
        }
        String label = UIManager.getInstance().localize("cn1.menu." + role, e.label);
        if (e.appendAppName) {
            String app = appName();
            if (app != null && app.length() > 0) {
                label = label + " " + app;
            }
        }
        return label;
    }

    private static String appName() {
        if (!Display.isInitialized()) {
            return null;
        }
        return Display.getInstance().getProperty("AppName", null);
    }

    /// How a platform places a role, or null when that platform has no such command: Hide is
    /// macOS only, and a browser page cannot quit or minimize anything.
    static Entry entry(String convention, String role) {
        if (role == null) {
            return null;
        }
        if (MAC.equals(convention)) {
            return mac(role);
        }
        if (LINUX.equals(convention)) {
            return gnome(role);
        }
        Entry e = windows(role);
        if (e == null || !WEB.equals(convention)) {
            return e;
        }
        if (Command.DESKTOP_ROLE_QUIT.equals(role) || Command.DESKTOP_ROLE_MINIMIZE.equals(role)
                || Command.DESKTOP_ROLE_ZOOM.equals(role)) {
            return null;
        }
        // The browser keeps these for its own windows and tabs and never hands them to a page.
        if (e.key == 'N' || e.key == 'W' || e.key == 'P' || e.key == 'O') {
            return new Entry(e.menu, e.group, e.label, e.appendAppName, 0, 0);
        }
        return e;
    }

    // Apple Human Interface Guidelines, "The menu bar".
    private static Entry mac(String role) {
        if (Command.DESKTOP_ROLE_ABOUT.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_APP, TOP, "About", true, 0, 0);
        }
        if (Command.DESKTOP_ROLE_SETTINGS.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_APP, HIGH, "Settings" + ELLIPSIS, false, ',', P);
        }
        if (Command.DESKTOP_ROLE_HIDE_APP.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_APP, LOW, "Hide", true, 'H', P);
        }
        if (Command.DESKTOP_ROLE_QUIT.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_APP, LAST, "Quit", true, 'Q', P);
        }
        if (Command.DESKTOP_ROLE_CLOSE.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_FILE, HIGH, "Close", false, 'W', P);
        }
        if (Command.DESKTOP_ROLE_REDO.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_EDIT, TOP, "Redo", false, 'Z', P | S);
        }
        if (Command.DESKTOP_ROLE_FULL_SCREEN.equals(role)) {
            // Control-Command-F, which the modifier set cannot spell: no shortcut rather than a
            // wrong one.
            return new Entry(Command.DESKTOP_MENU_VIEW, LAST, "Enter Full Screen", false, 0, 0);
        }
        if (Command.DESKTOP_ROLE_MINIMIZE.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_WINDOW, TOP, "Minimize", false, 'M', P);
        }
        if (Command.DESKTOP_ROLE_ZOOM.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_WINDOW, TOP, "Zoom", false, 0, 0);
        }
        if (Command.DESKTOP_ROLE_HELP.equals(role)) {
            Entry e = common(role);
            return new Entry(e.menu, e.group, "Help", false, '?', P);
        }
        return common(role);
    }

    // The Windows desktop guidelines for menus: About under Help, Options under Tools and
    // Exit at the foot of File.
    private static Entry windows(String role) {
        if (Command.DESKTOP_ROLE_ABOUT.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_HELP, LAST, "About", true, 0, 0);
        }
        if (Command.DESKTOP_ROLE_SETTINGS.equals(role)) {
            return new Entry(DESKTOP_MENU_TOOLS, LAST, "Options", false, 0, 0);
        }
        if (Command.DESKTOP_ROLE_QUIT.equals(role)) {
            // Alt+F4 closes the window from the system menu; the key is not a letter the
            // shortcut model can carry, so Exit shows none.
            return new Entry(Command.DESKTOP_MENU_FILE, LAST, "Exit", false, 0, 0);
        }
        if (Command.DESKTOP_ROLE_CLOSE.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_FILE, HIGH, "Close", false, 'W', P);
        }
        if (Command.DESKTOP_ROLE_SAVE_AS.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_FILE, UPPER, "Save As" + ELLIPSIS, false, 0, 0);
        }
        if (Command.DESKTOP_ROLE_REDO.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_EDIT, TOP, "Redo", false, 'Y', P);
        }
        if (Command.DESKTOP_ROLE_FULL_SCREEN.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_VIEW, LAST, "Full Screen", false, 0, 0);
        }
        if (Command.DESKTOP_ROLE_MINIMIZE.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_WINDOW, TOP, "Minimize", false, 0, 0);
        }
        if (Command.DESKTOP_ROLE_ZOOM.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_WINDOW, TOP, "Maximize", false, 0, 0);
        }
        if (Command.DESKTOP_ROLE_HIDE_APP.equals(role)) {
            return null;
        }
        return common(role);
    }

    // The GNOME Human Interface Guidelines: Preferences at the foot of Edit, Quit at the foot
    // of File with Ctrl+Q, About at the foot of Help.
    private static Entry gnome(String role) {
        if (Command.DESKTOP_ROLE_ABOUT.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_HELP, LAST, "About", true, 0, 0);
        }
        if (Command.DESKTOP_ROLE_SETTINGS.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_EDIT, LAST, "Preferences", false, 0, 0);
        }
        if (Command.DESKTOP_ROLE_QUIT.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_FILE, LAST, "Quit", false, 'Q', P);
        }
        if (Command.DESKTOP_ROLE_CLOSE.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_FILE, LAST - 1, "Close", false, 'W', P);
        }
        if (Command.DESKTOP_ROLE_REDO.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_EDIT, TOP, "Redo", false, 'Z', P | S);
        }
        if (Command.DESKTOP_ROLE_FULL_SCREEN.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_VIEW, LAST, "Fullscreen", false, 0, 0);
        }
        if (Command.DESKTOP_ROLE_MINIMIZE.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_WINDOW, TOP, "Minimize", false, 0, 0);
        }
        if (Command.DESKTOP_ROLE_ZOOM.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_WINDOW, TOP, "Maximize", false, 0, 0);
        }
        if (Command.DESKTOP_ROLE_HIDE_APP.equals(role)) {
            return null;
        }
        return common(role);
    }

    // The commands every desktop names and places alike.
    private static Entry common(String role) {
        if (Command.DESKTOP_ROLE_NEW.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_FILE, TOP, "New", false, 'N', P);
        }
        if (Command.DESKTOP_ROLE_OPEN.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_FILE, TOP, "Open" + ELLIPSIS, false, 'O', P);
        }
        if (Command.DESKTOP_ROLE_SAVE.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_FILE, UPPER, "Save", false, 'S', P);
        }
        if (Command.DESKTOP_ROLE_SAVE_AS.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_FILE, UPPER, "Save As" + ELLIPSIS, false, 'S', P | S);
        }
        if (Command.DESKTOP_ROLE_PRINT.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_FILE, LOW, "Print" + ELLIPSIS, false, 'P', P);
        }
        if (Command.DESKTOP_ROLE_UNDO.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_EDIT, TOP, "Undo", false, 'Z', P);
        }
        if (Command.DESKTOP_ROLE_CUT.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_EDIT, UPPER, "Cut", false, 'X', P);
        }
        if (Command.DESKTOP_ROLE_COPY.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_EDIT, UPPER, "Copy", false, 'C', P);
        }
        if (Command.DESKTOP_ROLE_PASTE.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_EDIT, UPPER, "Paste", false, 'V', P);
        }
        if (Command.DESKTOP_ROLE_SELECT_ALL.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_EDIT, UPPER, "Select All", false, 'A', P);
        }
        if (Command.DESKTOP_ROLE_FIND.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_EDIT, HIGH, "Find" + ELLIPSIS, false, 'F', P);
        }
        if (Command.DESKTOP_ROLE_HELP.equals(role)) {
            return new Entry(Command.DESKTOP_MENU_HELP, HIGH, "Help", false, 0, 0);
        }
        return null;
    }

    /// The Tools menu, where Windows applications keep Options.
    public static final String DESKTOP_MENU_TOOLS = "Tools";

    // The order of the standard top-level menus, the same on every desktop; the application's
    // own menus go between View and Tools. Unknown names rank with the application's.
    private static final String[] ROOT_ORDER = {
        Command.DESKTOP_MENU_APP, Command.DESKTOP_MENU_FILE, Command.DESKTOP_MENU_EDIT,
        Command.DESKTOP_MENU_VIEW, null, DESKTOP_MENU_TOOLS, Command.DESKTOP_MENU_WINDOW,
        Command.DESKTOP_MENU_HELP
    };

    /// The rank of a top-level menu in the standard order, see `#canonicalRoot(String)`.
    static int rootRank(String root) {
        for (int i = 0; i < ROOT_ORDER.length; i++) {
            if (ROOT_ORDER[i] != null && ROOT_ORDER[i].equalsIgnoreCase(root)) {
                return i;
            }
        }
        return 4;
    }

    /// The standard spelling of a standard top-level menu named in any case, the name as
    /// written otherwise.
    static String canonicalRoot(String root) {
        for (String r : ROOT_ORDER) {
            if (r != null && r.equalsIgnoreCase(root)) {
                return r;
            }
        }
        return root;
    }
}
