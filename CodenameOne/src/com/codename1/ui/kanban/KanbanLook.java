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
package com.codename1.ui.kanban;

import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/// The styles a `KanbanBoard` paints with, looked up by UIID once per theme
/// and given a default when the theme does not define them, so a board looks
/// right in a theme that has never heard of it.
final class KanbanLook {
    // uiid, light fg, light bg, dark fg, dark bg, bg opacity, font: 0 plain, 1 small, 2 bold
    private static final Object[][] DEFAULTS = {
        {"KanbanBoard", 0x1d1b20, 0xf7f7fa, 0xe6e1e5, 0x141317, 255, 0},
        {"KanbanColumn", 0x49454f, 0xece9f1, 0xcac4d0, 0x1f1d23, 255, 0},
        {"KanbanColumnHeader", 0x1d1b20, 0xece9f1, 0xe6e1e5, 0x1f1d23, 0, 2},
        {"KanbanColumnOverLimit", 0xb3261e, 0xf9dedc, 0xf2b8b5, 0x601410, 255, 2},
        {"KanbanCard", 0x1d1b20, 0xffffff, 0xe6e1e5, 0x2b2930, 255, 0},
        {"KanbanCardSelected", 0x1a73e8, 0xffffff, 0x8ab4f8, 0x2b2930, 255, 0},
        {"KanbanCardTag", 0x1d1b20, 0xe8def8, 0xe6e1e5, 0x4a4458, 255, 1},
        {"KanbanLane", 0x49454f, 0xe1dce8, 0xcac4d0, 0x2f2c36, 255, 2},
        {"KanbanAddCard", 0x79747e, 0xffffff, 0x938f99, 0x1c1b1f, 0, 1},
        {"KanbanDropSlot", 0x1a73e8, 0xd3e3fd, 0x8ab4f8, 0x263238, 255, 0},
    };

    private static final HashMap<String, Style> STYLES = new HashMap<String, Style>();
    private static int generation = -1;
    private static Boolean darkSeen;

    private KanbanLook() {
    }

    private static void check() {
        int now = UIManager.getThemeGeneration();
        // dark mode can switch without a new theme generation
        Boolean dark = Display.getInstance().isDarkMode();
        boolean same = darkSeen == null ? dark == null : darkSeen.equals(dark);
        if (now != generation || !same) {
            generation = now;
            darkSeen = dark;
            STYLES.clear();
        }
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

    private static Style fallback(String uiid) {
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

    static int mm(float mm) {
        return Math.max(1, Display.getInstance().convertToPixels(mm));
    }

    static int mix(int a, int b, int amount) {
        int inv = 255 - amount;
        int r = (((a >> 16) & 0xff) * inv + ((b >> 16) & 0xff) * amount) / 255;
        int g = (((a >> 8) & 0xff) * inv + ((b >> 8) & 0xff) * amount) / 255;
        int bl = ((a & 0xff) * inv + (b & 0xff) * amount) / 255;
        return (r << 16) | (g << 8) | bl;
    }

    static int bg(Style s, int fallback) {
        return (s.getBgTransparency() & 0xff) == 0 ? fallback : s.getBgColor();
    }

    /// The text, cut short with an ellipsis when it is wider than `width`.
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
            if (f.stringWidth(text.substring(0, mid)) + dots <= width) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return text.substring(0, lo).trim() + "...";
    }

    /// The text broken at spaces into at most `maxLines` lines of `width`;
    /// the last is cut short when the text is longer.
    static List<String> wrap(String text, Font f, int width, int maxLines) {
        List<String> out = new ArrayList<String>();
        if (text == null || text.length() == 0 || width <= 0) {
            return out;
        }
        String rest = text.trim();
        while (rest.length() > 0 && out.size() < maxLines) {
            if (out.size() == maxLines - 1 || f.stringWidth(rest) <= width) {
                out.add(fit(rest, f, width));
                break;
            }
            // the longest run of whole words that fits
            int cut = -1;
            int from = 0;
            while (true) {
                int sp = rest.indexOf(' ', from);
                if (sp < 0 || f.stringWidth(rest.substring(0, sp)) > width) {
                    break;
                }
                cut = sp;
                from = sp + 1;
            }
            if (cut <= 0) {
                // one word wider than the card: break inside it
                int n = 1;
                while (n < rest.length() && f.stringWidth(rest.substring(0, n + 1)) <= width) {
                    n++;
                }
                out.add(rest.substring(0, n));
                rest = rest.substring(n).trim();
            } else {
                out.add(rest.substring(0, cut));
                rest = rest.substring(cut + 1).trim();
            }
        }
        return out;
    }

    static void shadow(Graphics g, int x, int y, int w, int h, int arc, float level) {
        if (level <= 0 || w <= 0 || h <= 0) {
            return;
        }
        int a = g.getAlpha();
        int reach = Math.max(2, Math.round(mm(0.8f) * Math.max(1f, level)));
        int drop = Math.round(mm(0.5f) * level);
        g.setColor(0);
        g.setAlpha(Math.max(1, Math.round(a * 0.18f * Math.min(1.6f, level) / reach)));
        for (int i = reach; i >= 1; i--) {
            g.fillRoundRect(x - i, y - i + drop, w + 2 * i, h + 2 * i, arc + 2 * i, arc + 2 * i);
        }
        g.setAlpha(a);
    }
}
