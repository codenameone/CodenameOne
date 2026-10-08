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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.ui.Display;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/// The colors, fonts and row sizes of the components that paint their own
/// cells -- tables and trees -- taken from the Codename One theme so that
/// they do not look foreign next to its widgets.
///
/// Cells take the `TableCell` style, headers the `TableHeader` style. A
/// theme that gives a selected cell no background of its own gets a tint
/// of the text color over the cell background, which reads in a light and
/// in a dark theme alike. A color put into the Swing
/// [com.codename1.desktopcompat.javax.swing.UIManager] under the JDK's key
/// (`Table.selectionBackground`, `Table.gridColor`, ...) wins over all of
/// this.
public final class CellTheme {

    private CellTheme() {
    }

    /// Whether rows are sized for a finger.
    public static boolean touch() {
        return Display.isInitialized() && Display.getInstance().isTouchScreenDevice();
    }

    /// `a` with the fraction `t` of `b` mixed in.
    public static Color mix(Color a, Color b, float t) {
        float u = 1 - t;
        return new Color(Math.round(a.getRed() * u + b.getRed() * t), Math.round(a.getGreen() * u + b.getGreen() * t),
                Math.round(a.getBlue() * u + b.getBlue() * t));
    }

    private static Color put(String key) {
        return key == null ? null : com.codename1.desktopcompat.javax.swing.UIManager.getColor(key);
    }

    private static Style style(String uiid, boolean selected) {
        if (!Display.isInitialized()) {
            return null;
        }
        UIManager m = UIManager.getInstance();
        return selected ? m.getComponentSelectedStyle(uiid) : m.getComponentStyle(uiid);
    }

    private static boolean filled(Style s) {
        return s != null && (s.getBgTransparency() & 0xff) != 0;
    }

    /// The background of cells: the cell style's if it has one, else the
    /// window background.
    public static Color background(String key) {
        Color c = put(key);
        if (c != null && !key.endsWith(".background")) {
            return c;
        }
        Style s = style("TableCell", false);
        return filled(s) ? new Color(s.getBgColor() & 0xffffff) : EventBridge.defaultBackground();
    }

    /// The color of cell text.
    public static Color foreground(String key) {
        Color c = put(key);
        if (c != null && !key.endsWith(".foreground")) {
            return c;
        }
        Style s = style("TableCell", false);
        return s != null ? new Color(s.getFgColor() & 0xffffff) : EventBridge.defaultForeground();
    }

    private static boolean ownSelection() {
        Style sel = style("TableCell", true);
        Style plain = style("TableCell", false);
        return filled(sel) && plain != null
                && (!filled(plain) || (sel.getBgColor() & 0xffffff) != (plain.getBgColor() & 0xffffff));
    }

    /// The background of selected cells.
    public static Color selectionBackground(String key) {
        Color c = put(key);
        if (c != null) {
            return c;
        }
        if (ownSelection()) {
            return new Color(style("TableCell", true).getBgColor() & 0xffffff);
        }
        return mix(background(null), foreground(null), 0.18f);
    }

    /// The text color of selected cells.
    public static Color selectionForeground(String key) {
        Color c = put(key);
        if (c != null) {
            return c;
        }
        if (ownSelection()) {
            return new Color(style("TableCell", true).getFgColor() & 0xffffff);
        }
        return foreground(null);
    }

    /// The color of the lines between cells.
    public static Color grid(String key) {
        Color c = put(key);
        return c != null ? c : mix(background(null), foreground(null), 0.2f);
    }

    /// The background of a table header.
    public static Color headerBackground(String key) {
        Color c = put(key);
        if (c != null && !key.endsWith(".background")) {
            return c;
        }
        Style s = style("TableHeader", false);
        if (filled(s)) {
            return new Color(s.getBgColor() & 0xffffff);
        }
        return mix(background(null), foreground(null), 0.08f);
    }

    /// The text color of a table header.
    public static Color headerForeground(String key) {
        Color c = put(key);
        if (c != null && !key.endsWith(".foreground")) {
            return c;
        }
        Style s = style("TableHeader", false);
        return s != null ? new Color(s.getFgColor() & 0xffffff) : EventBridge.defaultForeground();
    }

    /// The font of a table header.
    public static Font headerFont() {
        Style s = style("TableHeader", false);
        if (s != null && s.getFont() != null) {
            return Fonts.fromNative(s.getFont());
        }
        return Fonts.defaultFont();
    }

    /// The font of cells.
    public static Font font() {
        Style s = style("TableCell", false);
        if (s != null && s.getFont() != null) {
            return Fonts.fromNative(s.getFont());
        }
        return Fonts.defaultFont();
    }

    /// The height of a row of text in `f`, in logical pixels: at least
    /// `plain`, or on a touch screen at least `touch` and with room around
    /// the text for a finger.
    public static int rowHeight(Font f, int plain, int touch) {
        int fh = Fonts.metrics(f != null ? f : Fonts.defaultFont()).getHeight();
        if (touch()) {
            return Math.max(touch, fh + 10);
        }
        return Math.max(plain, fh + 2);
    }
}
