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
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.Style;
import java.util.HashMap;

/// The marks of check boxes and radio buttons, drawn here.
///
/// A theme is free to draw both the same way -- a filled circle is a
/// common check box on a phone -- and an application written for a desktop
/// tells its options apart by the mark alone: a square that is ticked can
/// be combined with others, a circle with a dot excludes its neighbours.
/// So the square and the circle are drawn here on every theme, in the
/// theme's colours.
public final class Indicators {
    private static final HashMap<String, Image> CACHE = new HashMap<String, Image>();

    private Indicators() {
    }

    /// The side of the mark that goes with text in the style's font.
    public static int size(Style s) {
        int h = s.getFont() == null ? 0 : s.getFont().getHeight();
        return Math.max(Units.toDevice(13), h * 4 / 5);
    }

    /// The mark, `round` for a radio button.
    public static Image mark(boolean round, boolean on, boolean enabled, int size) {
        int side = Math.max(6, size);
        Color field = CellTheme.fieldBackground();
        Color accent = CellTheme.selectionBackground(null);
        Color tick = CellTheme.selectionForeground(null);
        Color edge = CellTheme.mix(EventBridge.defaultBackground(), EventBridge.defaultForeground(), 0.5f);
        String key = (round ? "r" : "c") + (on ? "1" : "0") + (enabled ? "e" : "d") + side + ':' + field.getRGB()
                + ':' + accent.getRGB() + ':' + tick.getRGB() + ':' + edge.getRGB();
        Image cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        if (CACHE.size() > 64) {
            CACHE.clear();
        }
        Image img = Image.createImage(side, side, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setAlpha(enabled ? 255 : 110);
        int line = Math.max(1, side / 12);
        int w = side - 1;
        if (round) {
            g.setColor((on ? accent : edge).getRGB() & 0xffffff);
            g.fillArc(0, 0, w, w, 0, 360);
            if (on) {
                int dot = Math.max(2, side * 2 / 5);
                int at = (w - dot) / 2;
                g.setColor(tick.getRGB() & 0xffffff);
                g.fillArc(at, at, dot, dot, 0, 360);
            } else {
                g.setColor(field.getRGB() & 0xffffff);
                g.fillArc(line, line, w - 2 * line, w - 2 * line, 0, 360);
            }
        } else {
            int arc = Math.max(2, side / 4);
            g.setColor((on ? accent : edge).getRGB() & 0xffffff);
            g.fillRoundRect(0, 0, w, w, arc, arc);
            if (on) {
                g.setColor(tick.getRGB() & 0xffffff);
                int x1 = side * 22 / 100;
                int y1 = side * 52 / 100;
                int x2 = side * 42 / 100;
                int y2 = side * 72 / 100;
                int x3 = side * 78 / 100;
                int y3 = side * 28 / 100;
                int thick = Math.max(2, side / 7);
                for (int i = 0; i < thick; i++) {
                    g.drawLine(x1, y1 - i, x2, y2 - i);
                    g.drawLine(x2, y2 - i, x3, y3 - i);
                }
            } else {
                int in = Math.max(1, arc - line);
                g.setColor(field.getRGB() & 0xffffff);
                g.fillRoundRect(line, line, w - 2 * line, w - 2 * line, in, in);
            }
        }
        if (img.getWidth() == side) {
            CACHE.put(key, img);
        }
        return img;
    }

    /// Gives the text of a mark the colour of a label unless the
    /// application set one: a theme may tint a check box as something to
    /// tap, and beside a mark that reads as a link.
    public static void labelText(com.codename1.ui.Button peer, PeerSupport support) {
        // Room beside the mark, so that one option does not run into the
        // text of the one before it where the theme gives a check box none.
        int least = Units.toDevice(4);
        Style any = peer.getUnselectedStyle();
        if (any.getPaddingLeftNoRTL() < least || any.getPaddingRightNoRTL() < least) {
            Style all = peer.getAllStyles();
            byte px = Style.UNIT_TYPE_PIXELS;
            int top = any.getPaddingTop();
            int bottom = any.getPaddingBottom();
            int left = Math.max(least, any.getPaddingLeftNoRTL());
            int right = Math.max(least, any.getPaddingRightNoRTL());
            all.setPaddingUnit(px, px, px, px);
            all.setPadding(top, bottom, left, right);
        }
        if (support.owner().isForegroundSet()) {
            return;
        }
        int want = EventBridge.defaultForeground().getRGB() & 0xffffff;
        Style u = peer.getUnselectedStyle();
        if (u.getFgColor() != want) {
            u.setFgColor(want);
            peer.getSelectedStyle().setFgColor(want);
            peer.getPressedStyle().setFgColor(want);
        }
    }
}
