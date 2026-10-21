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

    JavaScriptDesktopChrome(HTMLDocument document, String os, CommandSink sink) {
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
        // A press outside an open menu closes it; browser_bridge.js does that on the main
        // thread for every press on the page, and the toggle listener above keeps openMenus in
        // step.
    }

    /// The height the chrome takes from the top of the page, in CSS pixels.
    int getHeight() {
        return root.getOffsetHeight();
    }

    void setTitle(String text) {
        title.setTextContent(text == null ? "" : text);
    }

    /// Replaces the menus with `commands`, one per top-level menu the encoder resolved.
    void setCommands(Vector commands) {
        closeMenus();
        menus.clear();
        itemsByMenu.clear();
        commandsById.clear();
        iconImages.clear();
        menuBar.setInnerHTML("");
        // Encoded once, under the web convention: standard roles, the menu order and the
        // standard menus' spelling are all resolved by the encoder, and its rows come out
        // menu by menu in menu-bar order.
        com.codename1.impl.NativeMenuEncoder encoded =
                com.codename1.impl.NativeMenuEncoder.encode(commands, 0, true);
        Map<String, List<com.codename1.impl.NativeMenuEncoder.Row>> byMenu =
                new java.util.LinkedHashMap<String, List<com.codename1.impl.NativeMenuEncoder.Row>>();
        for (com.codename1.impl.NativeMenuEncoder.Row row : encoded.getRows()) {
            String menu = row.getHint().length() == 0 ? "Commands" : row.getHint();
            List<com.codename1.impl.NativeMenuEncoder.Row> list = byMenu.get(menu);
            if (list == null) {
                list = new ArrayList<com.codename1.impl.NativeMenuEncoder.Row>();
                byMenu.put(menu, list);
            }
            list.add(row);
        }
        for (Map.Entry<String, List<com.codename1.impl.NativeMenuEncoder.Row>> e : byMenu.entrySet()) {
            menuBar.appendChild(buildMenu(e.getKey(), e.getValue()));
        }
    }

    private static boolean hasIcon(Command c) {
        return c.getIcon() != null || c.getMaterialIcon() != 0;
    }

    private HTMLElement buildMenu(String menuTitle, List<com.codename1.impl.NativeMenuEncoder.Row> rows) {
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
        // Item containers by their submenu path below this menu; "" is the menu's own list.
        Map<String, HTMLElement> containers = new HashMap<String, HTMLElement>();
        containers.put("", list);
        for (com.codename1.impl.NativeMenuEncoder.Row row : rows) {
            HTMLElement parent = containerFor(containers, row.getPath());
            if (row.isSeparator()) {
                HTMLElement sep = document.createElement("div");
                sep.setAttribute("role", "separator");
                sep.setAttribute("class", "cn1-chrome-separator");
                parent.appendChild(sep);
                continue;
            }
            final Command c = row.getCommand();
            int flags = row.getFlags();
            HTMLElement item = document.createElement("button");
            item.setAttribute("type", "button");
            item.setAttribute("class", "cn1-chrome-menu-item");
            if ((flags & com.codename1.impl.NativeMenuEncoder.FLAG_CHECKABLE) != 0) {
                boolean radio = (flags & com.codename1.impl.NativeMenuEncoder.FLAG_RADIO) != 0;
                boolean checked = (flags & com.codename1.impl.NativeMenuEncoder.FLAG_CHECKED) != 0;
                item.setAttribute("role", radio ? "menuitemradio" : "menuitemcheckbox");
                item.setAttribute("aria-checked", checked ? "true" : "false");
                HTMLElement mark = document.createElement("span");
                mark.setAttribute("class", "cn1-chrome-check");
                // Checking an item republishes the menu (Command.setDesktopChecked), so the mark
                // is current every time the menu is rebuilt.
                mark.setTextContent(checked ? (radio ? "\u2022" : "\u2713") : "");
                item.appendChild(mark);
            } else {
                item.setAttribute("role", "menuitem");
            }
            // Not the disabled attribute: Command.setEnabled() does not republish the menu, so an
            // item disabled when the menu was built would stay unclickable after the command was
            // enabled again. The look is refreshed each time the menu opens, and a click on a
            // command that is disabled by then is refused by dispatchNativeMenuCommand.
            markEnabled(item, c);
            items.add(new Object[]{item, c});
            String id = String.valueOf(nextCommandId++);
            commandsById.put(id, c);
            item.setAttribute("data-cn1-cmd", id);
            boolean labelled = row.getLabel().length() > 0;
            if (labelled && hasIcon(c)) {
                HTMLElement icon = document.createElement("span");
                icon.setAttribute("class", "cn1-chrome-icon");
                item.appendChild(icon);
                Object image = sink.renderIcon(icon, c);
                if (image != null) {
                    iconImages.add(image);
                }
            }
            if (labelled) {
                HTMLElement label = document.createElement("span");
                label.setAttribute("class", "cn1-chrome-label");
                // The row's label: a standard role's platform name when the command has none.
                label.setTextContent(row.getLabel());
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
            String shortcut = shortcutText(row.getShortcutKeyChar(), row.getShortcutModifiers(), mac);
            String binding = acceleratorBinding(row.getShortcutKeyChar(), row.getShortcutModifiers());
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
            parent.appendChild(item);
        }
        details.appendChild(list);
        return details;
    }

    /// The element a row's items go in: the menu's own list for a one-segment path, otherwise a
    /// nested submenu created on its first row, which keeps it at its place among the items.
    /// A submenu is a `details` element that expands in place: a flyout beside the panel would
    /// be clipped by the panel's scrolling, and `summary` is focusable and opens with Enter or
    /// Space, so a keyboard reaches the items without a handler of its own.
    private HTMLElement containerFor(Map<String, HTMLElement> containers, String[] path) {
        HTMLElement parent = containers.get("");
        StringBuilder key = new StringBuilder();
        for (int p = 1; p < path.length; p++) {
            // Joined by the unit separator, which no segment can contain: a slash would make
            // "A//B" (one segment "A/B") collide with the two segments "A" and "B".
            key.append('\u001f').append(path[p]);
            String k = key.toString();
            HTMLElement sub = containers.get(k);
            if (sub == null) {
                HTMLElement wrap = document.createElement("details");
                wrap.setAttribute("class", "cn1-chrome-submenu");
                wrap.setAttribute("role", "none");
                HTMLElement head = document.createElement("summary");
                head.setAttribute("class", "cn1-chrome-submenu-title");
                head.setAttribute("role", "menuitem");
                head.setAttribute("aria-haspopup", "menu");
                head.setTextContent(path[p]);
                wrap.appendChild(head);
                sub = document.createElement("div");
                sub.setAttribute("class", "cn1-chrome-submenu-items");
                sub.setAttribute("role", "menu");
                wrap.appendChild(sub);
                parent.appendChild(wrap);
                containers.put(k, sub);
            }
            parent = sub;
        }
        return parent;
    }

    /// The accelerator as text, the way the Windows menu writes it into the label. The page
    /// does not bind the keys itself: the application's own key listeners already see them.
    static String shortcutText(int key, int mods, boolean mac) {
        if (key <= 0) {
            return null;
        }
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

    /// The key binding browser_bridge.js matches, as `[primary][+alt][+shift]+<key>` with the key
    /// in lower case, exactly as configured (a shifted digit stays the digit; the bridge matches
    /// the physical key as well as the typed character). Null for a shortcut with neither the
    /// primary modifier nor Alt: a bare or Shift-only key is ordinary typing, and claiming it
    /// would swallow it.
    static String acceleratorBinding(int key, int mods) {
        boolean primary = (mods & Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY) != 0;
        boolean alt = (mods & Command.DESKTOP_SHORTCUT_MODIFIER_ALT) != 0;
        if (key <= 0 || (!primary && !alt)) {
            return null;
        }
        StringBuilder sb = new StringBuilder(primary ? "primary" : "");
        if (alt) {
            sb.append(primary ? "+alt" : "alt");
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
