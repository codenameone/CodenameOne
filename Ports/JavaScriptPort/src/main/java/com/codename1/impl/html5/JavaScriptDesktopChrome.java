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
package com.codename1.impl.html5;

import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;
import com.codename1.html5.js.dom.HTMLDocument;
import com.codename1.html5.js.dom.HTMLElement;
import com.codename1.ui.Command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/// The HTML title bar and menu bar a desktop browser shows when the `javascript.titleBar=html`
/// build hint asks for them.
///
/// A desktop native theme expects the platform to own the window title and the menu bar: the
/// Windows, Linux, macOS and JavaSE ports hide the Toolbar and hand the form title to the OS
/// window and the commands to the OS menu. A page has neither, so this draws both in the
/// document, above the canvas, and the port then answers the native-title and native-command
/// hooks exactly as those ports do.
///
/// The menus are `<details>` elements, so opening, closing and keyboard activation are the
/// browser's own and cost no round trip to the worker. Only choosing an item comes back here.
///
/// There are no minimize, maximize or close buttons: a page cannot do any of those to the
/// browser window, and a control that does nothing is worse than none.
final class JavaScriptDesktopChrome {

    /// Receives a chosen menu item; called on the worker, not the EDT.
    interface CommandSink {
        void commandChosen(Command cmd);

        /// Draws the command's icon into `into`; returns what must stay referenced while the
        /// element shows it (the rendered image), or null when the command has no icon.
        Object renderIcon(HTMLElement into, Command cmd);
    }

    /// The standard top-level menus and where each placement hint lands, in the order desktop
    /// applications show them. The same table as the Windows port's cn1_windows_menu.cpp, so an
    /// application's menu looks the same in the browser as it does in the native build. A page
    /// has no application menu, so the App, About, Preferences and Quit hints go where Windows
    /// puts them.
    private static final String[][] MENU_HINTS = {
        {"File", "File"},
        {"Edit", "Edit"},
        {"View", "View"},
        {"Window", "Window"},
        {"Help", "Help"},
        {"About", "Help"},
        {"Preferences", "File"},
        {"Quit", "File"},
        {"App", "File"},
    };

    private static final String[] MENU_ORDER = {"File", "Edit", "View", "Window"};

    private final HTMLDocument document;
    private final HTMLElement root;
    private final HTMLElement title;
    private final HTMLElement menuBar;
    private final CommandSink sink;
    private final boolean mac;
    private final List<HTMLElement> openMenus = new ArrayList<HTMLElement>();

    // What the current menus hold, rebuilt by setCommands. The DOM refers to these by index and
    // id only: the listeners are two delegated ones on the menu bar, installed once. A listener
    // per item would be a worker callback per item, which the worker bridge keeps for the life of
    // the page -- every rebuild on every form change would have retained its commands forever.
    private final List<HTMLElement> menus = new ArrayList<HTMLElement>();
    private final List<List<Object[]>> itemsByMenu = new ArrayList<List<Object[]>>();
    private final Map<String, Command> commandsById = new HashMap<String, Command>();
    private int nextCommandId;
    /// The icon images the current menus show, kept reachable until the next rebuild: an image's
    /// host surface is released when the Java image is collected.
    private final List<Object> iconImages = new ArrayList<Object>();

    JavaScriptDesktopChrome(HTMLDocument document, HTMLElement appSurface, String os, CommandSink sink) {
        this.document = document;
        this.sink = sink;
        this.mac = "mac".equals(os);
        root = document.createElement("div");
        root.setAttribute("id", "cn1-desktop-chrome");
        // The platform look, and its dark variant through prefers-color-scheme, live in
        // style.css: an external stylesheet, so the page's content security policy is unaffected.
        root.setAttribute("class", "cn1-chrome-" + os);
        title = document.createElement("div");
        title.setAttribute("class", "cn1-chrome-title");
        root.appendChild(title);
        menuBar = document.createElement("nav");
        menuBar.setAttribute("class", "cn1-chrome-menubar");
        menuBar.setAttribute("aria-label", "Application menu");
        root.appendChild(menuBar);
        document.getBody().appendChild(root);

        menuBar.addEventListener("click", new EventListener() {
            public void handleEvent(Event evt) {
                Command c = commandForTarget(evt.getTarget());
                if (c != null) {
                    closeMenus();
                    sink.commandChosen(c);
                }
            }
        });
        // toggle does not bubble, but it does run the capture phase, so one capturing listener
        // on the menu bar sees every menu open and close.
        menuBar.addEventListener("toggle", new EventListener() {
            public void handleEvent(Event evt) {
                menuToggled(evt.getTarget());
            }
        }, true);

        // Escape closes an open menu, as it does in every native menu bar.
        root.addEventListener("keydown", new EventListener() {
            public void handleEvent(Event evt) {
                if (((JSOImplementations.KeyEvent) evt).getKeyCode() == 27) {
                    closeMenus();
                }
            }
        });
        // A press on the app closes an open menu. Capture phase, so the app's own handling of
        // the press cannot stop it first. The canvas is the whole app surface, so listening
        // there -- rather than on the document and working out whether the target lies inside
        // the chrome -- needs no DOM walk per press.
        appSurface.addEventListener("pointerdown", new EventListener() {
            public void handleEvent(Event evt) {
                if (!openMenus.isEmpty()) {
                    closeMenus();
                }
            }
        }, true);
    }

    /// The height the chrome takes from the top of the page, in CSS pixels.
    int getHeight() {
        return root.getOffsetHeight();
    }

    void setTitle(String text) {
        title.setTextContent(text == null ? "" : text);
    }

    /// Replaces the menus with `commands`, grouped by [Command#getDesktopMenu()].
    void setCommands(Vector commands) {
        closeMenus();
        menus.clear();
        itemsByMenu.clear();
        commandsById.clear();
        iconImages.clear();
        menuBar.setInnerHTML("");
        Map<String, List<Command>> groups = group(commands);
        for (Map.Entry<String, List<Command>> e : groups.entrySet()) {
            menuBar.appendChild(buildMenu(e.getKey(), e.getValue()));
        }
    }

    /// Commands keyed by top-level menu title, in menu-bar order: the standard menus first, then
    /// the application's own titles in the order they first appear, then Help last.
    static Map<String, List<Command>> group(Vector commands) {
        Map<String, List<Command>> byTitle = new HashMap<String, List<Command>>();
        List<String> custom = new ArrayList<String>();
        if (commands != null) {
            for (int i = 0; i < commands.size(); i++) {
                Object o = commands.elementAt(i);
                if (!(o instanceof Command)) {
                    continue;
                }
                Command c = (Command) o;
                // An icon-only command -- new Command("", icon), a common right-bar action -- is
                // kept: with the Toolbar hidden this menu is its only way to be reached. The
                // native desktop ports skip it because their menus take text only; this one can
                // show the icon itself.
                if (!hasLabel(c) && !hasIcon(c)) {
                    continue;
                }
                String menu = titleForHint(c.getDesktopMenu());
                List<Command> list = byTitle.get(menu);
                if (list == null) {
                    list = new ArrayList<Command>();
                    byTitle.put(menu, list);
                    if (!isStandard(menu)) {
                        custom.add(menu);
                    }
                }
                list.add(c);
            }
        }
        Map<String, List<Command>> ordered = new java.util.LinkedHashMap<String, List<Command>>();
        for (String m : MENU_ORDER) {
            if (byTitle.containsKey(m)) {
                ordered.put(m, byTitle.get(m));
            }
        }
        for (String m : custom) {
            ordered.put(m, byTitle.get(m));
        }
        if (byTitle.containsKey("Help")) {
            ordered.put("Help", byTitle.get("Help"));
        }
        return ordered;
    }

    static String titleForHint(String hint) {
        if (hint == null || hint.length() == 0) {
            // Where the Windows port puts a command with no placement hint.
            return "Commands";
        }
        for (String[] row : MENU_HINTS) {
            if (row[0].equalsIgnoreCase(hint)) {
                return row[1];
            }
        }
        // An unrecognised hint is a menu titled with the hint itself.
        return hint;
    }

    private static boolean hasLabel(Command c) {
        String name = c.getCommandName();
        return name != null && name.length() > 0;
    }

    private static boolean hasIcon(Command c) {
        return c.getIcon() != null || c.getMaterialIcon() != 0;
    }

    private static boolean isStandard(String menu) {
        for (String m : MENU_ORDER) {
            if (m.equals(menu)) {
                return true;
            }
        }
        return "Help".equals(menu);
    }

    private HTMLElement buildMenu(String menuTitle, List<Command> commands) {
        final HTMLElement details = document.createElement("details");
        details.setAttribute("class", "cn1-chrome-menu");
        List<Object[]> items = new ArrayList<Object[]>();
        details.setAttribute("data-cn1-menu", String.valueOf(menus.size()));
        menus.add(details);
        itemsByMenu.add(items);
        HTMLElement summary = document.createElement("summary");
        summary.setTextContent(menuTitle);
        details.appendChild(summary);
        HTMLElement list = document.createElement("div");
        list.setAttribute("class", "cn1-chrome-menu-items");
        list.setAttribute("role", "menu");
        for (final Command c : commands) {
            HTMLElement item = document.createElement("button");
            item.setAttribute("type", "button");
            item.setAttribute("role", "menuitem");
            item.setAttribute("class", "cn1-chrome-menu-item");
            // Not the disabled attribute: Command.setEnabled() does not republish the menu, so an
            // item disabled when the menu was built would stay unclickable after the command was
            // enabled again. The look is refreshed each time the menu opens, and a click on a
            // command that is disabled by then is refused by dispatchNativeMenuCommand.
            markEnabled(item, c);
            items.add(new Object[]{item, c});
            String id = String.valueOf(nextCommandId++);
            commandsById.put(id, c);
            item.setAttribute("data-cn1-cmd", id);
            if (hasLabel(c)) {
                HTMLElement label = document.createElement("span");
                label.setTextContent(c.getCommandName());
                item.appendChild(label);
            } else {
                HTMLElement icon = document.createElement("span");
                icon.setAttribute("class", "cn1-chrome-icon");
                item.appendChild(icon);
                Object image = sink.renderIcon(icon, c);
                if (image != null) {
                    iconImages.add(image);
                }
                // Something for assistive technology to announce; the command has no text of its
                // own to offer.
                item.setAttribute("aria-label", "Command " + (items.size()));
            }
            String shortcut = shortcutText(c, mac);
            String binding = acceleratorBinding(c);
            if (binding != null) {
                // browser_bridge.js matches key presses against this on the main thread, where
                // preventDefault still works -- so Ctrl/Cmd+S runs the command instead of also
                // opening the browser's Save dialog -- and activates the item with a click.
                item.setAttribute("data-cn1-accel", binding);
            }
            if (shortcut != null) {
                HTMLElement accel = document.createElement("span");
                accel.setAttribute("class", "cn1-chrome-shortcut");
                accel.setTextContent(shortcut);
                item.appendChild(accel);
            }
            list.appendChild(item);
        }
        details.appendChild(list);
        return details;
    }

    /// The accelerator as text, the way the Windows menu writes it into the label. The page
    /// does not bind the keys itself: the application's own key listeners already see them.
    static String shortcutText(Command c, boolean mac) {
        int key = c.getDesktopShortcutKeyChar();
        if (key <= 0) {
            return null;
        }
        int mods = c.getDesktopShortcutModifiers();
        StringBuilder sb = new StringBuilder();
        if ((mods & Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY) != 0) {
            // The primary modifier is Command on a Mac and Control everywhere else.
            sb.append(mac ? "Cmd+" : "Ctrl+");
        }
        if ((mods & Command.DESKTOP_SHORTCUT_MODIFIER_ALT) != 0) {
            sb.append(mac ? "Option+" : "Alt+");
        }
        if ((mods & Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT) != 0) {
            sb.append("Shift+");
        }
        sb.append(Character.toUpperCase((char) key));
        return sb.toString();
    }

    /// The command a click landed on: the item, or the label or shortcut span inside it.
    private Command commandForTarget(Object target) {
        Object node = target;
        for (int depth = 0; node != null && depth < 4; depth++) {
            com.codename1.html5.js.dom.Element el = (com.codename1.html5.js.dom.Element) node;
            String id = el.getAttribute("data-cn1-cmd");
            if (id != null && id.length() > 0) {
                return commandsById.get(id);
            }
            node = el.getParentNode();
        }
        return null;
    }

    /// Opening one menu closes the others, as in a native menu bar, and refreshes its items'
    /// enabled look: Command.setEnabled() does not republish the menu.
    private void menuToggled(Object target) {
        String index = ((com.codename1.html5.js.dom.Element) target).getAttribute("data-cn1-menu");
        if (index == null || index.length() == 0) {
            return;
        }
        int i = Integer.parseInt(index);
        if (i < 0 || i >= menus.size()) {
            return;
        }
        HTMLElement details = menus.get(i);
        if (details.hasAttribute("open")) {
            for (Object[] entry : itemsByMenu.get(i)) {
                markEnabled((HTMLElement) entry[0], (Command) entry[1]);
            }
            for (HTMLElement other : new ArrayList<HTMLElement>(openMenus)) {
                if (other != details) { //NOPMD CompareObjectsWithEquals
                    other.removeAttribute("open");
                }
            }
            openMenus.clear();
            openMenus.add(details);
        } else {
            openMenus.remove(details);
        }
    }

    private static void markEnabled(HTMLElement item, Command c) {
        if (c.isEnabled()) {
            item.removeAttribute("aria-disabled");
        } else {
            item.setAttribute("aria-disabled", "true");
        }
    }

    /// The key binding browser_bridge.js matches, as `primary[+alt][+shift]+<key>` with the key in
    /// lower case; null for a command without a primary-modifier shortcut, which a page cannot
    /// claim without breaking ordinary typing.
    static String acceleratorBinding(Command c) {
        int key = c.getDesktopShortcutKeyChar();
        int mods = c.getDesktopShortcutModifiers();
        if (key <= 0 || (mods & Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY) == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder("primary");
        if ((mods & Command.DESKTOP_SHORTCUT_MODIFIER_ALT) != 0) {
            sb.append("+alt");
        }
        if ((mods & Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT) != 0) {
            sb.append("+shift");
        }
        char ch = (char) key;
        if (ch >= 'A' && ch <= 'Z') {
            ch = (char) (ch + 32);
        }
        return sb.append('+').append(ch).toString();
    }

    void closeMenus() {
        for (HTMLElement m : new ArrayList<HTMLElement>(openMenus)) {
            m.removeAttribute("open");
        }
        openMenus.clear();
    }
}
