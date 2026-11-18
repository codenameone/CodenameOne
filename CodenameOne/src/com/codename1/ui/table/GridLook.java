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
package com.codename1.ui.table;

import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.util.HashMap;

/// The styles a `DataGrid` paints with, looked up by UIID once per theme
/// and not once per cell.
///
/// A grid paints its cells with no component behind them, so a UIID here is
/// a source of colours, a font and a padding, not of a border.
final class GridLook {
    private static final HashMap<String, Style> STYLES = new HashMap<String, Style>();
    private static int generation = -1;

    private GridLook() {
    }

    private static void check() {
        int now = UIManager.getThemeGeneration();
        if (now != generation) {
            generation = now;
            STYLES.clear();
        }
    }

    static int generation() {
        check();
        return generation;
    }

    static Style style(String uiid) {
        check();
        Style s = STYLES.get(uiid);
        if (s == null) {
            s = UIManager.getInstance().getComponentStyle(uiid);
            STYLES.put(uiid, s);
        }
        return s;
    }

    static Style selected(String uiid) {
        check();
        String key = uiid + "#sel";
        Style s = STYLES.get(key);
        if (s == null) {
            s = UIManager.getInstance().getComponentSelectedStyle(uiid);
            STYLES.put(key, s);
        }
        return s;
    }

    static int mm(float mm) {
        return Math.max(1, Display.getInstance().convertToPixels(mm));
    }

    /// Fills with a style's background colour and opacity.
    static void fill(Graphics g, Style s, int x, int y, int w, int h) {
        int op = s.getBgTransparency() & 0xff;
        if (op == 0 || w <= 0 || h <= 0) {
            return;
        }
        g.setColor(s.getBgColor());
        if (op == 255) {
            g.fillRect(x, y, w, h);
        } else {
            int a = g.getAlpha();
            g.setAlpha(a * op / 255);
            g.fillRect(x, y, w, h);
            g.setAlpha(a);
        }
    }

    /// The longest start of `text` that fits `width`, ending in an ellipsis
    /// when anything was cut. Empty when not even that fits.
    static String fit(String text, Font f, int width) {
        if (text == null || text.length() == 0 || width <= 0) {
            return "";
        }
        if (f.stringWidth(text) <= width) {
            return text;
        }
        int dots = f.stringWidth("...");
        if (dots > width) {
            return "";
        }
        int lo = 0;
        int hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (f.substringWidth(text, 0, mid) + dots <= width) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo == 0 ? "" : text.substring(0, lo) + "...";
    }

    /// Mixes two RGB colours: 0 is all `a`, 255 all `b`.
    static int mix(int a, int b, int amount) {
        int inv = 255 - amount;
        int r = (((a >> 16) & 0xff) * inv + ((b >> 16) & 0xff) * amount) / 255;
        int gr = (((a >> 8) & 0xff) * inv + ((b >> 8) & 0xff) * amount) / 255;
        int bl = ((a & 0xff) * inv + (b & 0xff) * amount) / 255;
        return (r << 16) | (gr << 8) | bl;
    }

    /// The colour under a style: its own background, or the grid's when it
    /// has none.
    static int under(Style st) {
        if ((st.getBgTransparency() & 0xff) == 0) {
            return style("DataGrid").getBgColor();
        }
        return st.getBgColor();
    }

    /// A small triangle pointing down (`open`) or right, centred in a box.
    static void arrow(Graphics g, int x, int y, int size, boolean open) {
        int s = Math.max(4, size / 2);
        int cx = x + size / 2;
        int cy = y + size / 2;
        if (open) {
            g.fillTriangle(cx - s / 2, cy - s / 4, cx + s / 2, cy - s / 4, cx, cy + s / 3);
        } else {
            g.fillTriangle(cx - s / 4, cy - s / 2, cx - s / 4, cy + s / 2, cx + s / 3, cy);
        }
    }
}
