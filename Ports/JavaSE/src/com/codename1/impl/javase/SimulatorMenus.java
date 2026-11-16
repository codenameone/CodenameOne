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
package com.codename1.impl.javase;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

/// Drives and captures the simulator window's real Swing menu bar, for the
/// `sim_menu` MCP tools and the screen recorder.
///
/// Walking a path opens each `JMenu` through `MenuSelectionManager`, exactly the
/// state a mouse leaves it in, so the dropdowns are the real Swing popups.
/// Capturing paints the `JMenuBar` and every open `JPopupMenu` with their own
/// `paint` at their true on-screen offsets; nothing is drawn by hand. A
/// heavyweight popup is its own native window, but its component still paints
/// into any `Graphics`, so the capture does not depend on popup weight.
///
/// On macOS the bar normally lives in the screen menu bar, which no Java code can
/// paint. `setInWindow(true)` moves it into the frame by clearing
/// `apple.laf.useScreenMenuBar` and re-adding the bar, so its UI is installed
/// again; the recorder does this for a desktop-mode recording and undoes it at
/// stop. The `cn1.simulator.inWindowMenus` system property (or the
/// `inWindowMenus` preference) keeps it in the window for the whole session.
public final class SimulatorMenus {
    static final String SCREEN_MENU_BAR = "apple.laf.useScreenMenuBar";

    private static String savedScreenMenuBar;
    private static boolean forcedInWindow;

    private SimulatorMenus() {
    }

    /// Splits "Insert/Models/Coin" into its segments, ignoring empty ones.
    static List<String> splitPath(String path) {
        List<String> out = new ArrayList<String>();
        if (path == null) {
            return out;
        }
        for (String s : path.split("/")) {
            String t = s.trim();
            if (t.length() > 0) {
                out.add(t);
            }
        }
        return out;
    }

    /// The menu elements one level down: the bar's menus, or a menu's items.
    static List<JMenuItem> children(Object parent) {
        List<JMenuItem> out = new ArrayList<JMenuItem>();
        if (parent instanceof JMenuBar) {
            JMenuBar bar = (JMenuBar) parent;
            for (int i = 0; i < bar.getMenuCount(); i++) {
                if (bar.getMenu(i) != null) {
                    out.add(bar.getMenu(i));
                }
            }
        } else if (parent instanceof JMenu) {
            Component[] cs = ((JMenu) parent).getPopupMenu().getComponents();
            for (Component c : cs) {
                if (c instanceof JMenuItem) {
                    out.add((JMenuItem) c);
                }
            }
        }
        return out;
    }

    /// Matches a segment by exact text, then ignoring case and a trailing ellipsis.
    static JMenuItem find(List<JMenuItem> items, String segment) {
        for (JMenuItem i : items) {
            if (segment.equals(i.getText())) {
                return i;
            }
        }
        String want = normalize(segment);
        for (JMenuItem i : items) {
            if (want.equals(normalize(i.getText()))) {
                return i;
            }
        }
        return null;
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        while (t.endsWith(".")) {
            t = t.substring(0, t.length() - 1);
        }
        return t.trim().toLowerCase();
    }

    private static String titles(List<JMenuItem> items) {
        StringBuilder sb = new StringBuilder();
        for (JMenuItem i : items) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append('"').append(i.getText()).append('"');
        }
        return sb.toString();
    }

    /// Resolves a path to its items, top menu first; throws naming the missing
    /// segment and the titles available at that level.
    static List<JMenuItem> resolve(JMenuBar bar, List<String> segments) {
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("path is empty; give e.g. \"File/Open\"");
        }
        List<JMenuItem> out = new ArrayList<JMenuItem>();
        Object parent = bar;
        for (int i = 0; i < segments.size(); i++) {
            List<JMenuItem> kids = children(parent);
            JMenuItem hit = find(kids, segments.get(i));
            if (hit == null) {
                throw new IllegalArgumentException("No menu item \"" + segments.get(i) + "\""
                        + (i == 0 ? " in the menu bar" : " under \"" + segments.get(i - 1) + "\"")
                        + "; available: " + (kids.isEmpty() ? "(none)" : titles(kids)));
            }
            if (i < segments.size() - 1 && !(hit instanceof JMenu)) {
                throw new IllegalArgumentException("\"" + segments.get(i) + "\" is not a submenu");
            }
            out.add(hit);
            parent = hit;
        }
        return out;
    }

    /// The selection path that shows the first `depth` menus of `items` open.
    static MenuElement[] selectionPath(JMenuBar bar, List<JMenuItem> items, int depth, boolean leafSelected) {
        List<MenuElement> path = new ArrayList<MenuElement>();
        path.add(bar);
        for (int i = 0; i < depth; i++) {
            JMenu m = (JMenu) items.get(i);
            path.add(m);
            path.add(m.getPopupMenu());
        }
        if (leafSelected && depth < items.size()) {
            path.add(items.get(depth));
        }
        return path.toArray(new MenuElement[path.size()]);
    }

    /// The menu tree, as maps of text, enabled, and children for submenus.
    static List<Object> tree(Object parent) {
        List<Object> out = new ArrayList<Object>();
        for (JMenuItem i : children(parent)) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("text", i.getText());
            m.put("enabled", Boolean.valueOf(i.isEnabled()));
            if (i instanceof JMenu) {
                m.put("children", tree(i));
            }
            out.add(m);
        }
        return out;
    }

    static JMenuBar menuBar(JFrame f) {
        JMenuBar bar = f == null ? null : f.getJMenuBar();
        if (bar == null) {
            throw new IllegalStateException("The simulator window has no menu bar; native desktop "
                    + "menus need desktop mode and the desktop.titleBar=native build hint, and a "
                    + "form with commands that carry setDesktopMenu");
        }
        return bar;
    }

    /// True while the bar is a component of the window, i.e. not in the macOS
    /// screen menu bar.
    static boolean isInWindow(JMenuBar bar) {
        return bar != null && bar.isShowing() && bar.getHeight() > 0;
    }

    /// Whether the session asked for in-window menus for good.
    static boolean sessionInWindow() {
        if (Boolean.getBoolean("cn1.simulator.inWindowMenus")) {
            return true;
        }
        try {
            return java.util.prefs.Preferences.userNodeForPackage(JavaSEPort.class).getBoolean("inWindowMenus", false);
        } catch (Throwable t) {
            return false;
        }
    }

    /// Moves the menu bar into the window (true) or back where it was (false).
    /// A no-op off macOS, and when the session already keeps menus in the window.
    /// Call on the AWT event thread.
    static void setInWindow(JFrame f, boolean inWindow) {
        if (!JavaSEPort.IS_MAC || sessionInWindow()) {
            return;
        }
        if (inWindow == forcedInWindow) {
            return;
        }
        if (inWindow) {
            savedScreenMenuBar = System.getProperty(SCREEN_MENU_BAR);
            System.setProperty(SCREEN_MENU_BAR, "false");
        } else if (savedScreenMenuBar == null) {
            System.clearProperty(SCREEN_MENU_BAR);
        } else {
            System.setProperty(SCREEN_MENU_BAR, savedScreenMenuBar);
        }
        forcedInWindow = inWindow;
        if (f == null) {
            return;
        }
        JMenuBar bar = f.getJMenuBar();
        if (bar == null) {
            return;
        }
        // Aqua decides screen vs window when the bar's UI is installed and the
        // bar is added, so detach, reinstall the UI, and attach again.
        MenuSelectionManager.defaultManager().clearSelectedPath();
        f.setJMenuBar(null);
        bar.updateUI();
        f.setJMenuBar(bar);
        f.getRootPane().revalidate();
        f.repaint();
    }

    /// Paints `screen` (which shows `canvas`, at `scale` image pixels per
    /// component pixel) with the frame's in-window menu bar above it and every
    /// open popup menu at its on-screen position. Returns `screen` itself when
    /// there is nothing to add. Painting is Swing's own.
    static BufferedImage compose(BufferedImage screen, Component canvas, JFrame f) {
        if (screen == null || canvas == null || f == null || !canvas.isShowing()) {
            return screen;
        }
        JMenuBar bar = f.getJMenuBar();
        boolean barIn = isInWindow(bar);
        List<JPopupMenu> popups = new ArrayList<JPopupMenu>();
        for (MenuElement e : MenuSelectionManager.defaultManager().getSelectedPath()) {
            if (e instanceof JPopupMenu && ((JPopupMenu) e).isShowing()) {
                popups.add((JPopupMenu) e);
            }
        }
        if (!barIn && popups.isEmpty()) {
            return screen;
        }
        double scale = canvas.getWidth() > 0 ? screen.getWidth() / (double) canvas.getWidth() : 1;
        Point c = canvas.getLocationOnScreen();
        int top = 0;
        Point b = null;
        if (barIn) {
            b = bar.getLocationOnScreen();
            top = Math.max(0, c.y - b.y);
        }
        int w = screen.getWidth();
        int h = screen.getHeight() + (int) Math.round(top * scale);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            int originX = c.x;
            int originY = c.y - top;
            g.drawImage(screen, 0, h - screen.getHeight(), null);
            if (barIn) {
                paintAt(g, bar, b.x - originX, b.y - originY, scale);
            }
            for (JPopupMenu p : popups) {
                Point pl = p.getLocationOnScreen();
                paintAt(g, p, pl.x - originX, pl.y - originY, scale);
            }
        } finally {
            g.dispose();
        }
        return out;
    }

    private static void paintAt(Graphics2D g, javax.swing.JComponent comp, int x, int y, double scale) {
        Graphics2D cg = (Graphics2D) g.create();
        try {
            cg.scale(scale, scale);
            cg.translate(x, y);
            cg.clipRect(0, 0, comp.getWidth(), comp.getHeight());
            comp.paint(cg);
        } finally {
            cg.dispose();
        }
    }
}
