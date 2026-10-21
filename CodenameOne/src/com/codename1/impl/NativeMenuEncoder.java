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
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/// Turns the commands a form publishes into the ordered rows of a native menu bar. Every port
/// with a native menu uses it, so submenu paths, separator groups and check state are decided
/// once rather than in each native parser.
///
/// The rows come out in the order the native side should append them: depth first, so the
/// first row of a submenu creates that submenu at its place among its parent's items, and a
/// parser that finds or creates each menu by its path and appends to it rebuilds the tree.
///
/// The wire format, for the ports whose menu is built in C, is one row per line:
///
/// `path TAB label TAB shortcutKeyChar TAB shortcutModifiers TAB id TAB flags TAB iconPng`
///
/// The path is the menu path with its segments joined by `#PATH_SEPARATOR` (the first segment
/// is the placement hint the ports already understand, empty for the default menu), flags are
/// the `FLAG_*` bits, and iconPng is base64 PNG data or empty. Tabs, newlines and the path
/// separator are stripped from the text fields: they are delimiters and mean nothing in a menu
/// item, which renders on one line.
///
/// This is an implementation class; applications use the `Command` desktop menu API.
public final class NativeMenuEncoder {
    /// The item can be chosen.
    public static final int FLAG_ENABLED = 1;
    /// The item shows a check state.
    public static final int FLAG_CHECKABLE = 2;
    /// The item is checked.
    public static final int FLAG_CHECKED = 4;
    /// The item belongs to a radio group (always with `#FLAG_CHECKABLE`).
    public static final int FLAG_RADIO = 8;
    /// The row is a separator line, not an item.
    public static final int FLAG_SEPARATOR = 16;
    /// The flags above this many bits carry the item's standard role as a small ordinal, 0 for
    /// none, so a port can put the item where its own stock item of that role is, in place of
    /// it, rather than beside it: the application menu's Quit, the Edit menu's Copy. A port
    /// with no stock items ignores it. Read it with `(flags >> FLAG_ROLE_SHIFT) & 0xff`; the
    /// ordinals are listed by `NativeMenuConventions#ordinalOf(String)`.
    public static final int FLAG_ROLE_SHIFT = 8;

    /// Joins the segments of a menu path on the wire (the ASCII unit separator).
    public static final char PATH_SEPARATOR = (char) 0x1f;

    /// Pixel size icons are rendered at: a 16 point menu icon at twice the density.
    public static final int ICON_PIXELS = 32;

    /// One row of the menu.
    public static final class Row {
        private final String[] path;
        private final String label;
        private final int flags;
        private final Command command;
        private final int id;
        private final int keyChar;
        private final int modifiers;

        Row(String[] path, String label, int flags, Command command, int id, int keyChar, int modifiers) {
            this.path = path;
            this.label = label;
            this.flags = flags;
            this.command = command;
            this.id = id;
            this.keyChar = keyChar;
            this.modifiers = modifiers;
        }

        /// The upper-cased shortcut character, 0 for none: the command's own, or its role's
        /// on this platform.
        ///
        /// #### Returns
        ///
        /// the shortcut character or 0
        public int getShortcutKeyChar() {
            return keyChar;
        }

        /// The shortcut modifier bits, see `Command#DESKTOP_SHORTCUT_MODIFIER_PRIMARY`.
        ///
        /// #### Returns
        ///
        /// the modifier bits
        public int getShortcutModifiers() {
            return modifiers;
        }

        /// The standard role ordinal, 0 for none. See `#FLAG_ROLE_SHIFT`.
        ///
        /// #### Returns
        ///
        /// the role ordinal
        public int getRoleOrdinal() {
            return (flags >> FLAG_ROLE_SHIFT) & 0xff;
        }

        /// The menu path, first segment is the placement hint ("" for the default menu).
        ///
        /// #### Returns
        ///
        /// a copy of the path segments
        public String[] getPath() {
            String[] out = new String[path.length];
            System.arraycopy(path, 0, out, 0, path.length);
            return out;
        }

        /// The placement hint, the path's first segment.
        ///
        /// #### Returns
        ///
        /// the hint, "" for the default menu
        public String getHint() {
            return path[0];
        }

        /// The item label, "" for a separator.
        ///
        /// #### Returns
        ///
        /// the label
        public String getLabel() {
            return label;
        }

        /// The `FLAG_*` bits.
        ///
        /// #### Returns
        ///
        /// the flags
        public int getFlags() {
            return flags;
        }

        /// True for a separator row.
        ///
        /// #### Returns
        ///
        /// true for a separator
        public boolean isSeparator() {
            return (flags & FLAG_SEPARATOR) != 0;
        }

        /// The command the row runs, null for a separator.
        ///
        /// #### Returns
        ///
        /// the command or null
        public Command getCommand() {
            return command;
        }

        /// The id the native item reports when chosen.
        ///
        /// #### Returns
        ///
        /// the id
        public int getId() {
            return id;
        }

        /// The icon of the command, or null.
        ///
        /// #### Returns
        ///
        /// the icon or null
        public Image getIcon() {
            return iconOf(command);
        }
    }

    /// A command as this platform places it.
    private static final class Item {
        final Command command;
        final int group;
        final String label;
        final int keyChar;
        final int modifiers;
        final int ordinal;

        Item(Command command, int group, String label, int keyChar, int modifiers, int ordinal) {
            this.command = command;
            this.group = group;
            this.label = label;
            this.keyChar = keyChar;
            this.modifiers = modifiers;
            this.ordinal = ordinal;
        }
    }

    private static final class Node {
        final String segment;
        final int group;
        final List<Object> children = new ArrayList<Object>();

        Node(String segment, int group) {
            this.segment = segment;
            this.group = group;
        }

        Node child(String seg, int group) {
            for (Object o : children) {
                if (o instanceof Node && ((Node) o).segment.equals(seg)) {
                    return (Node) o;
                }
            }
            Node n = new Node(seg, group);
            children.add(n);
            return n;
        }
    }

    private final List<Row> rows = new ArrayList<Row>();
    private int nextId;

    private NativeMenuEncoder(int firstId) {
        this.nextId = firstId;
    }

    /// Encodes the commands into rows. Ids are handed out from `firstId` upwards, one per row
    /// including separators, so a port keeps its own id counter by adding `#getIdCount()`.
    ///
    /// #### Parameters
    ///
    /// - `commands`: the published commands, entries that are not commands are ignored
    ///
    /// - `firstId`: the id of the first row
    ///
    /// #### Returns
    ///
    /// the encoder holding the rows
    public static NativeMenuEncoder encode(Vector commands, int firstId) {
        return encode(commands, firstId, false);
    }

    /// Encodes the commands into rows, see `#encode(Vector, int)`.
    ///
    /// #### Parameters
    ///
    /// - `commands`: the published commands, entries that are not commands are ignored
    ///
    /// - `firstId`: the id of the first row
    ///
    /// - `keepIconOnly`: true to keep a command with an icon and no label, for a menu that can
    /// show the icon by itself; the native menus take text and drop it
    ///
    /// #### Returns
    ///
    /// the encoder holding the rows
    public static NativeMenuEncoder encode(Vector commands, int firstId, boolean keepIconOnly) {
        return encode(commands, firstId, keepIconOnly, NativeMenuConventions.current());
    }

    /// Encodes the commands into rows under a given platform's menu conventions, see
    /// `#encode(Vector, int, boolean)`. Standard roles are resolved here: a command with a
    /// role gets the platform's menu, group, label and shortcut for anything it does not set
    /// itself, and a command the platform does not publish is left out.
    ///
    /// #### Parameters
    ///
    /// - `commands`: the published commands, entries that are not commands are ignored
    ///
    /// - `firstId`: the id of the first row
    ///
    /// - `keepIconOnly`: see `#encode(Vector, int, boolean)`
    ///
    /// - `convention`: one of the `NativeMenuConventions` platform constants
    ///
    /// #### Returns
    ///
    /// the encoder holding the rows
    public static NativeMenuEncoder encode(Vector commands, int firstId, boolean keepIconOnly, String convention) {
        NativeMenuEncoder e = new NativeMenuEncoder(firstId);
        if (commands == null) {
            return e;
        }
        boolean mac = NativeMenuConventions.MAC.equals(convention);
        List<Node> roots = new ArrayList<Node>();
        for (int i = 0; i < commands.size(); i++) {
            Object o = commands.elementAt(i);
            if (!(o instanceof Command)) {
                continue;
            }
            Command c = (Command) o;
            if (!c.isDesktopPlatform(convention)) {
                continue;
            }
            String hint = c.getDesktopMenu();
            String role = c.getDesktopRole();
            if (role == null) {
                role = legacyRole(hint);
                if (role != null) {
                    // The old About/Preferences/Quit placements name a role, not a menu.
                    hint = null;
                }
            }
            NativeMenuConventions.Entry entry = null;
            if (NativeMenuConventions.ordinalOf(role) != 0) {
                entry = NativeMenuConventions.entry(convention, role);
                if (entry == null) {
                    // A standard command this platform does not have: Hide outside macOS,
                    // Quit in a browser page.
                    continue;
                }
            }
            String label = c.getCommandName();
            if ((label == null || label.length() == 0) && entry != null) {
                label = NativeMenuConventions.labelOf(convention, role);
            }
            boolean sep = c.isDesktopSeparator();
            if (!sep && (label == null || label.length() == 0)
                    && !(keepIconOnly && (c.getIcon() != null || c.getMaterialIcon() != 0))) {
                continue;
            }
            String[] path = parsePath(hint != null || entry == null ? hint : entry.menu);
            path[0] = NativeMenuConventions.canonicalRoot(path[0]);
            if (!mac && Command.DESKTOP_MENU_APP.equals(path[0])) {
                // Only macOS has an application menu; elsewhere its items go to File.
                path[0] = Command.DESKTOP_MENU_FILE;
            }
            int group = c.getClientProperty(Command.DESKTOP_MENU_GROUP) == null && entry != null
                    ? entry.group : c.getDesktopMenuGroup();
            int key = c.getDesktopShortcutKeyChar();
            int mods = c.getDesktopShortcutModifiers();
            if (c.getClientProperty(Command.DESKTOP_SHORTCUT_KEY) == null && entry != null) {
                key = entry.key;
                mods = entry.modifiers;
            }
            Item item = new Item(c, group, clean(label), key, mods, NativeMenuConventions.ordinalOf(role));
            Node node = null;
            for (Node root : roots) {
                if (root.segment.equals(path[0])) {
                    node = root;
                    break;
                }
            }
            if (node == null) {
                node = new Node(path[0], group);
                roots.add(node);
            }
            for (int p = 1; p < path.length; p++) {
                node = node.child(path[p], group);
            }
            node.children.add(item);
        }
        // Top-level menus are batched by their resolved name: roles, the old About/Preferences/
        // Quit placements and the standard menus in any case are all resolved above, so one
        // physical menu is one batch and its groups order it exactly.
        //
        // The batches themselves go in the standard order -- App, File, Edit, View, the
        // application's own menus, Tools, Window, Help -- so every port lays the bar out the
        // way the platform's own applications do, whatever order the commands were added in.
        List<Node> ordered = new ArrayList<Node>(roots.size());
        for (Node root : roots) {
            int rank = NativeMenuConventions.rootRank(root.segment);
            int at = ordered.size();
            while (at > 0 && NativeMenuConventions.rootRank(ordered.get(at - 1).segment) > rank) {
                at--;
            }
            ordered.add(at, root);
        }
        for (Node root : ordered) {
            e.emit(root, new String[]{root.segment});
        }
        return e;
    }

    // The placements that used to stand for a role before roles existed.
    private static String legacyRole(String hint) {
        if (hint == null) {
            return null;
        }
        if (Command.DESKTOP_MENU_ABOUT.equalsIgnoreCase(hint)) {
            return Command.DESKTOP_ROLE_ABOUT;
        }
        if (Command.DESKTOP_MENU_PREFERENCES.equalsIgnoreCase(hint)) {
            return Command.DESKTOP_ROLE_SETTINGS;
        }
        if (Command.DESKTOP_MENU_QUIT.equalsIgnoreCase(hint)) {
            return Command.DESKTOP_ROLE_QUIT;
        }
        return null;
    }

    private static int groupOf(Object o) {
        if (o instanceof Node) {
            return ((Node) o).group;
        }
        return ((Item) o).group;
    }

    private static boolean isSeparator(Object o) {
        return o instanceof Item && ((Item) o).command.isDesktopSeparator();
    }

    private void emit(Node node, String[] path) {
        // An explicit separator sorts with the item added before it (the one after it when it
        // comes first), so it stays at the spot it was added rather than moving with its own
        // group, which is always the default.
        int count = node.children.size();
        int[] groups = new int[count];
        int prev = Integer.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            Object o = node.children.get(i);
            if (isSeparator(o)) {
                groups[i] = prev;
            } else {
                groups[i] = groupOf(o);
                prev = groups[i];
            }
        }
        int next = Integer.MIN_VALUE;
        for (int i = count - 1; i >= 0; i--) {
            Object o = node.children.get(i);
            if (!isSeparator(o)) {
                next = groups[i];
            } else if (groups[i] == Integer.MIN_VALUE) {
                groups[i] = next;
            }
        }
        // Stable sort by group: insertion order wins within a group.
        List<Object> sorted = new ArrayList<Object>(node.children);
        for (int i = 1; i < count; i++) {
            Object cur = sorted.get(i);
            int g = groups[i];
            int j = i - 1;
            while (j >= 0 && groups[j] > g) {
                sorted.set(j + 1, sorted.get(j));
                groups[j + 1] = groups[j];
                j--;
            }
            sorted.set(j + 1, cur);
            groups[j + 1] = g;
        }
        boolean anyItem = false;
        boolean pendingSeparator = false;
        int lastGroup = 0;
        for (Object o : sorted) {
            if (isSeparator(o)) {
                pendingSeparator = anyItem;
                continue;
            }
            int g = groupOf(o);
            if (anyItem && g != lastGroup) {
                pendingSeparator = true;
            }
            // A separator is only written between two items, never leading, trailing or twice.
            // A submenu is emitted first and the separator put in front of it afterwards, so a
            // submenu that turns out empty leaves no separator behind.
            if (o instanceof Node) {
                Node n = (Node) o;
                String[] sub = new String[path.length + 1];
                System.arraycopy(path, 0, sub, 0, path.length);
                sub[path.length] = n.segment;
                int before = rows.size();
                emit(n, sub);
                if (rows.size() == before) {
                    continue;
                }
                if (pendingSeparator) {
                    rows.add(before, new Row(path, "", FLAG_SEPARATOR, null, nextId++, 0, 0));
                    pendingSeparator = false;
                }
            } else {
                if (pendingSeparator) {
                    rows.add(new Row(path, "", FLAG_SEPARATOR, null, nextId++, 0, 0));
                    pendingSeparator = false;
                }
                Item item = (Item) o;
                Command c = item.command;
                int flags = item.ordinal << FLAG_ROLE_SHIFT;
                if (c.isEnabled()) {
                    flags |= FLAG_ENABLED;
                }
                if (c.isDesktopCheckable()) {
                    flags |= FLAG_CHECKABLE;
                    if (c.isDesktopChecked()) {
                        flags |= FLAG_CHECKED;
                    }
                    if (c.getDesktopRadioGroup() != null) {
                        flags |= FLAG_RADIO;
                    }
                }
                rows.add(new Row(path, item.label, flags, c, nextId++, item.keyChar, item.modifiers));
            }
            anyItem = true;
            lastGroup = g;
        }
    }

    /// Splits a placement hint into its path segments. `/` separates segments and `//` is a
    /// literal slash; empty segments are dropped, and a null or empty hint is the default menu.
    ///
    /// #### Parameters
    ///
    /// - `hint`: the hint from `Command#getDesktopMenu()`
    ///
    /// #### Returns
    ///
    /// at least one segment, the first one "" for the default menu
    public static String[] parsePath(String hint) {
        List<String> out = new ArrayList<String>();
        if (hint != null) {
            StringBuilder cur = new StringBuilder();
            int len = hint.length();
            for (int i = 0; i < len; i++) {
                char ch = hint.charAt(i);
                if (ch == '/') {
                    if (i + 1 < len && hint.charAt(i + 1) == '/') {
                        cur.append('/');
                        i++;
                        continue;
                    }
                    String seg = clean(cur.toString());
                    if (seg.length() > 0) {
                        out.add(seg);
                    }
                    cur.setLength(0);
                } else {
                    cur.append(ch);
                }
            }
            String seg = clean(cur.toString());
            if (seg.length() > 0) {
                out.add(seg);
            }
        }
        if (out.isEmpty()) {
            out.add("");
        }
        return out.toArray(new String[out.size()]);
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            sb.append(ch == '\n' || ch == '\r' || ch == '\t' || ch == PATH_SEPARATOR ? ' ' : ch);
        }
        return sb.toString();
    }

    /// The rows in the order they are added to the native menu.
    ///
    /// #### Returns
    ///
    /// the rows
    public List<Row> getRows() {
        return rows;
    }

    /// The number of ids this encoding used, one per row.
    ///
    /// #### Returns
    ///
    /// the row count
    public int getIdCount() {
        return rows.size();
    }

    /// The rows in the wire format described on the class.
    ///
    /// #### Parameters
    ///
    /// - `includeIcons`: true to render each command's icon into the row as base64 PNG
    ///
    /// #### Returns
    ///
    /// the encoded rows separated by newlines
    public String toWireFormat(boolean includeIcons) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (i > 0) {
                sb.append('\n');
            }
            for (int p = 0; p < r.path.length; p++) {
                if (p > 0) {
                    sb.append(PATH_SEPARATOR);
                }
                sb.append(r.path[p]);
            }
            Command c = r.command;
            sb.append('\t').append(r.label)
                    .append('\t').append(r.keyChar)
                    .append('\t').append(r.modifiers)
                    .append('\t').append(r.id)
                    .append('\t').append(r.flags)
                    .append('\t');
            if (includeIcons && c != null) {
                String png = iconPngBase64(c);
                if (png != null) {
                    sb.append(png);
                }
            }
        }
        return sb.toString();
    }

    /// The command's icon, or null. A material (or icon-font) glyph wins over the image when
    /// both are set, as it does on the command's button (`Button#setCommand`).
    ///
    /// #### Parameters
    ///
    /// - `c`: the command, may be null
    ///
    /// #### Returns
    ///
    /// the icon or null
    public static Image iconOf(Command c) {
        if (c == null) {
            return null;
        }
        Image img = c.getIcon();
        if (c.getMaterialIcon() != 0) {
            Style s = UIManager.getInstance().getComponentStyle("Command");
            Font font = c.getIconFont();
            if (font == null) {
                img = FontImage.createMaterial(c.getMaterialIcon(), s, 4);
            } else {
                // A glyph of the command's own icon font, the way Button draws it.
                if (font.isTTFNativeFont()) {
                    font = font.derive(Display.getInstance().convertToPixels(4), Font.STYLE_PLAIN);
                }
                img = FontImage.create("" + c.getMaterialIcon(), s, font);
            }
        }
        return img;
    }

    /// The command's icon as base64 PNG of `#ICON_PIXELS` square, or null when it has none or
    /// the platform cannot write PNG.
    ///
    /// #### Parameters
    ///
    /// - `c`: the command
    ///
    /// #### Returns
    ///
    /// base64 PNG data or null
    public static String iconPngBase64(Command c) {
        Image img = iconOf(c);
        ImageIO io = img == null ? null : ImageIO.getImageIO();
        if (io == null) {
            return null;
        }
        try {
            Image scaled = img.scaled(ICON_PIXELS, ICON_PIXELS);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            io.save(scaled, bo, ImageIO.FORMAT_PNG, 1);
            return Base64.encodeNoNewline(bo.toByteArray());
        } catch (java.io.IOException err) {
            // An icon that cannot be written leaves the item without one; the menu still works.
            return null;
        }
    }
}
