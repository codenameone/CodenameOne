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
package com.codename1.ui.scheduler;

import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.util.HashMap;

/// The styles the scheduling components paint with, looked up by UIID once
/// per theme and not once per item.
///
/// The surfaces draw thousands of rectangles with no component behind them,
/// so a UIID here is a source of colours and a font, not of a border or a
/// padding.
final class Look {
    private static final HashMap<String, Style> STYLES = new HashMap<String, Style>();
    private static int generation = -1;

    private Look() {
    }

    private static void check() {
        int now = UIManager.getThemeGeneration();
        if (now != generation) {
            generation = now;
            STYLES.clear();
        }
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

    /// The shadow of something held above the surface, `level` of the way
    /// up from 0 to 1.
    static void shadow(Graphics g, int x, int y, int w, int h, float level) {
        if (level <= 0 || w <= 0 || h <= 0) {
            return;
        }
        int a = g.getAlpha();
        int reach = Math.max(2, mm(1.1f));
        int drop = Math.round(mm(0.6f) * level);
        int arc = mm(1.2f);
        g.setColor(0);
        // Each ring is drawn over the ones outside it, which is what makes
        // the shadow darker towards the thing that casts it.
        g.setAlpha(Math.max(1, Math.round(a * 0.2f * level / reach)));
        for (int i = reach; i >= 1; i--) {
            g.fillRoundRect(x - i, y - i + drop, w + 2 * i, h + 2 * i, arc + 2 * i, arc + 2 * i);
        }
        g.setAlpha(a);
    }

    /// The light of the pointer on what it is over: a wash of `rgb` and
    /// a line around it, `level` of the way up from 0 to 1.
    static void glow(Graphics g, int rgb, int x, int y, int w, int h, float level) {
        if (level <= 0 || w <= 0 || h <= 0) {
            return;
        }
        int a = g.getAlpha();
        int arc = mm(0.8f);
        g.setColor(rgb);
        g.setAlpha(Math.round(a * 0.16f * level));
        g.fillRoundRect(x, y, w, h, arc, arc);
        g.setAlpha(Math.round(a * 0.55f * level));
        g.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
        g.setAlpha(a);
    }

    /// The same wash with no line, for an empty place a press would fill.
    static void wash(Graphics g, int rgb, int x, int y, int w, int h, float level) {
        if (level <= 0 || w <= 0 || h <= 0) {
            return;
        }
        int a = g.getAlpha();
        g.setColor(rgb);
        g.setAlpha(Math.round(a * 0.09f * level));
        g.fillRect(x, y, w, h);
        g.setAlpha(a);
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

    /// The colour a style's lines are drawn over. A grid style usually has
    /// no background of its own, and the colour a theme leaves in a
    /// transparent one is black: mixed with that, a pale line came out
    /// nearly black. What is under such a grid is the component.
    static int under(Style st) {
        if ((st.getBgTransparency() & 0xff) == 0) {
            return style("Scheduler").getBgColor();
        }
        return st.getBgColor();
    }

    /// A line of a grid, as strong as `amount` of 255.
    static int line(Style grid, int amount) {
        return mix(under(grid), grid.getFgColor(), amount);
    }

    /// Mixes two RGB colours: 0 is all `a`, 255 all `b`.
    static int mix(int a, int b, int amount) {
        int inv = 255 - amount;
        int r = (((a >> 16) & 0xff) * inv + ((b >> 16) & 0xff) * amount) / 255;
        int gr = (((a >> 8) & 0xff) * inv + ((b >> 8) & 0xff) * amount) / 255;
        int bl = ((a & 0xff) * inv + (b & 0xff) * amount) / 255;
        return (r << 16) | (gr << 8) | bl;
    }

    /// Black or white, whichever reads on `rgb`.
    static int textOn(int rgb) {
        int lum = (((rgb >> 16) & 0xff) * 299 + ((rgb >> 8) & 0xff) * 587 + (rgb & 0xff) * 114) / 1000;
        return lum > 150 ? 0x1c1b1f : 0xffffff;
    }
}
