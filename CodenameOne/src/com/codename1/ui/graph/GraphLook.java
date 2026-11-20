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
package com.codename1.ui.graph;

import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.util.HashMap;

/// The styles a `NodeGraph` paints with, looked up by UIID once per theme
/// rather than once per node, and the drawing helpers the renderers share.
///
/// A UIID here is a source of colours and a font. Nodes have no component
/// behind them, so a border or a padding in the theme is not drawn.
///
/// The defaults live here, not in the theme code: a UIID the installed theme
/// says nothing about is painted from the palette below, light or dark to
/// match the form. A theme that styles one UIID takes it over whole.
final class GraphLook {
    // uiid, light fg, light bg, dark fg, dark bg, bg opacity, font (0 plain, 1 small, 2 bold)
    private static final Object[][] DEFAULTS = {
        {"NodeGraph", 0x1d1b20, 0xf7f7fa, 0xe6e1e5, 0x141317, 255, 0},
        {"NodeGraphGrid", 0xb4b0bb, 0xffffff, 0x5f5b66, 0x1c1b1f, 0, 0},
        {"NodeGraphNode", 0x1d1b20, 0xffffff, 0xe6e1e5, 0x1c1b1f, 255, 0},
        {"NodeGraphNodeSelected", 0x1a73e8, 0xffffff, 0x8ab4f8, 0x1c1b1f, 255, 0},
        {"NodeGraphGroup", 0x49454f, 0xece6f0, 0xcac4d0, 0x26242b, 120, 2},
        {"NodeGraphPort", 0x1a73e8, 0xffffff, 0x8ab4f8, 0x1c1b1f, 255, 1},
        {"NodeGraphEdge", 0x79747e, 0xffffff, 0x938f99, 0x1c1b1f, 0, 0},
        {"NodeGraphEdgeLabel", 0x49454f, 0xffffff, 0xcac4d0, 0x1c1b1f, 255, 1},
        {"NodeGraphMinimap", 0x49454f, 0xf3edf7, 0xcac4d0, 0x2b2930, 255, 0},
        {"NodeGraphLane", 0x79747e, 0xffffff, 0x938f99, 0x1c1b1f, 110, 0},
        {"NodeGraphLaneHeader", 0x1d1b20, 0xe7e0ec, 0xe6e1e5, 0x36343b, 255, 2},
        {"NodeGraphHandle", 0x1a73e8, 0xffffff, 0x8ab4f8, 0x1c1b1f, 255, 0},
        {"NodeGraphPalette", 0x1d1b20, 0xf3edf7, 0xe6e1e5, 0x211f26, 255, 0},
        {"NodeGraphPaletteItem", 0x1d1b20, 0xffffff, 0xe6e1e5, 0x2b2930, 255, 1},
    };

    private static final HashMap<String, Style> STYLES = new HashMap<String, Style>();
    // per UIID, then per pixel size: asked once per node per frame, and an
    // Integer of a font size is a cached one, so a lookup allocates nothing
    private static final HashMap<String, HashMap<Integer, Font>> FONTS = new HashMap<String, HashMap<Integer, Font>>();
    private static int generation = -1;
    private static Boolean darkSeen;

    private GraphLook() {
    }

    private static void check() {
        int now = UIManager.getThemeGeneration();
        // dark mode can switch without a new theme generation, and both the
        // fallbacks and the $Dark lookups depend on it
        Boolean dark = Display.getInstance().isDarkMode();
        boolean same = darkSeen == null ? dark == null : darkSeen.equals(dark);
        if (now != generation || !same) {
            generation = now;
            darkSeen = dark;
            STYLES.clear();
            FONTS.clear();
        }
    }

    /// The font of a UIID at a zoom. Text is part of the drawing, so it
    /// scales with the cards it sits in: at the zoom `fitToView` picks for a
    /// wide graph, a theme-sized label would be cut to a letter or two. A
    /// font that cannot be derived (a bitmap or system font) is used as it
    /// is. Sizes are whole pixels, so a pinch derives a few fonts, not one
    /// per frame.
    static Font font(String uiid, float zoom) {
        Font base = style(uiid).getFont();
        if (base == null || !base.isTTFNativeFont()) {
            return base;
        }
        float px = base.getPixelSize();
        if (px <= 0) {
            px = base.getHeight();
        }
        int size = Math.max(6, Math.round(px * Math.min(zoom, 1.5f)));
        if (size == Math.round(px)) {
            return base;
        }
        HashMap<Integer, Font> sizes = FONTS.get(uiid);
        if (sizes == null) {
            sizes = new HashMap<Integer, Font>();
            FONTS.put(uiid, sizes);
        }
        Integer key = Integer.valueOf(size);
        Font f = sizes.get(key);
        if (f == null) {
            f = base.derive(size, base.getStyle());
            sizes.put(key, f);
        }
        return f;
    }

    static Style style(String uiid) {
        check();
        Style s = STYLES.get(uiid);
        if (s == null) {
            UIManager m = UIManager.getInstance();
            s = m.isThemeStyleDefined(uiid) ? m.getComponentStyle(uiid) : fallback(uiid);
            STYLES.put(uiid, s);
        }
        return s;
    }

    /// The default style of a UIID the theme does not declare.
    static Style fallback(String uiid) {
        UIManager m = UIManager.getInstance();
        Object[] row = null;
        for (int i = 0; i < DEFAULTS.length; i++) { // NOPMD ForLoopCanBeForeach - indexed, as the rest of the class
            if (DEFAULTS[i][0].equals(uiid)) {
                row = DEFAULTS[i];
                break;
            }
        }
        Style s = m.getComponentStyle(uiid);
        if (row == null) {
            return s;
        }
        boolean dark = isDark(m);
        s.setFgColor(((Integer) row[dark ? 3 : 1]).intValue());
        s.setBgColor(((Integer) row[dark ? 4 : 2]).intValue());
        s.setBgTransparency(((Integer) row[5]).intValue());
        int kind = ((Integer) row[6]).intValue();
        Font base = Font.getDefaultFont();
        if (kind == 2) {
            s.setFont(base.isTTFNativeFont() ? base.derive(base.getHeight(), Font.STYLE_BOLD)
                    : Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        } else if (kind == 1) {
            s.setFont(base.isTTFNativeFont() ? base.derive(Display.getInstance().convertToPixels(2f), Font.STYLE_PLAIN)
                    : Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        } else {
            s.setFont(base);
        }
        return s;
    }

    // Dark when the device says so or the theme's forms are dark: a theme
    // can be dark without the dark mode switch.
    private static boolean isDark(UIManager m) {
        Style form = m.getComponentStyle("Form");
        if ((form.getBgTransparency() & 0xff) != 0) {
            int rgb = form.getBgColor();
            int luma = (((rgb >> 16) & 0xff) * 299 + ((rgb >> 8) & 0xff) * 587 + (rgb & 0xff) * 114) / 1000;
            return luma < 96;
        }
        Boolean d = Display.getInstance().isDarkMode();
        return d != null && d.booleanValue();
    }

    /// Screen pixels per world unit at zoom 1: the node font's height over
    /// the 20 pixels world sizes are designed for, never below 1.
    static float unit() {
        Font f = style("NodeGraphNode").getFont();
        int h = f == null ? 0 : f.getHeight();
        return Math.max(1f, h / 20f);
    }

    static int mm(float mm) {
        return Math.max(1, Display.getInstance().convertToPixels(mm));
    }

    /// A length in millimetres at a zoom, for the padding around text that
    /// scales with the zoom.
    static int mm(float mm, float zoom) {
        return Math.max(1, Math.round(mm(mm) * Math.min(zoom, 1.5f)));
    }

    // the last two strokes asked for, newest first: a frame asks for the
    // same two widths for every node it paints. Painting is the EDT's alone.
    private static final Stroke[] STROKES = new Stroke[2];

    /// A stroke for the outline of a shape. It is shared: do not change it.
    static Stroke stroke(float width) {
        Stroke s = STROKES[0];
        if (s != null && s.getLineWidth() == width) {
            return s;
        }
        s = STROKES[1];
        if (s == null || s.getLineWidth() != width) {
            s = new Stroke(width, Stroke.CAP_BUTT, Stroke.JOIN_ROUND, 4f);
        }
        STROKES[1] = STROKES[0];
        STROKES[0] = s;
        return s;
    }

    /// Mixes two RGB colours: 0 is all `a`, 255 all `b`.
    static int mix(int a, int b, int amount) {
        int inv = 255 - amount;
        int r = (((a >> 16) & 0xff) * inv + ((b >> 16) & 0xff) * amount) / 255;
        int g = (((a >> 8) & 0xff) * inv + ((b >> 8) & 0xff) * amount) / 255;
        int bl = ((a & 0xff) * inv + (b & 0xff) * amount) / 255;
        return (r << 16) | (g << 8) | bl;
    }

    /// The background colour a style paints, or `fallback` when the style is
    /// transparent: a transparent style's colour is usually black, which is
    /// not what is under it.
    static int bg(Style s, int fallback) {
        return (s.getBgTransparency() & 0xff) == 0 ? fallback : s.getBgColor();
    }

    /// A line `width` pixels wide, drawn as a filled quad so it is as thick
    /// on every port; a width of one or less is a plain line.
    static void line(Graphics g, double x1, double y1, double x2, double y2, float width) {
        if (width <= 1.25f) {
            g.drawLine((int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2), (int) Math.round(y2));
            return;
        }
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01) {
            return;
        }
        double nx = -dy / len * width / 2;
        double ny = dx / len * width / 2;
        // new arrays per call, never shared scratch: Android's async view
        // queues the arrays it is given and draws them after paint returns
        int[] xs = new int[4];
        int[] ys = new int[4];
        xs[0] = (int) Math.round(x1 + nx);
        xs[1] = (int) Math.round(x2 + nx);
        xs[2] = (int) Math.round(x2 - nx);
        xs[3] = (int) Math.round(x1 - nx);
        ys[0] = (int) Math.round(y1 + ny);
        ys[1] = (int) Math.round(y2 + ny);
        ys[2] = (int) Math.round(y2 - ny);
        ys[3] = (int) Math.round(y1 - ny);
        g.fillPolygon(xs, ys, 4);
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

    /// A soft shadow under a rounded box, `level` from 0 to 1.
    static void shadow(Graphics g, int x, int y, int w, int h, int arc, float level) {
        if (level <= 0 || w <= 0 || h <= 0) {
            return;
        }
        int a = g.getAlpha();
        int reach = Math.max(2, mm(0.8f));
        int drop = Math.round(mm(0.5f) * level);
        g.setColor(0);
        g.setAlpha(Math.max(1, Math.round(a * 0.18f * level / reach)));
        for (int i = reach; i >= 1; i--) {
            g.fillRoundRect(x - i, y - i + drop, w + 2 * i, h + 2 * i, arc + 2 * i, arc + 2 * i);
        }
        g.setAlpha(a);
    }
}
