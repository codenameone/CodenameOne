/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.vector;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Places and draws text labels with greedy collision avoidance: a label is
/// only drawn when its bounding box does not overlap one already placed this
/// frame. Each label is rendered with a one-pixel halo for legibility over
/// busy map content. This is a lightweight stand-in for a full label engine,
/// adequate for place names on a basemap.
final class LabelEngine {

    private final List occupied = new ArrayList();
    private final Map fontCache = new HashMap();

    /// Clears placements at the start of a frame.
    void reset() {
        occupied.clear();
    }

    /// Attempts to draw `text` centered at `cx,cy`. Returns false (drawing
    /// nothing) when it would collide with an already placed label.
    boolean place(Graphics g, String text, double sizePx, int textColor, int haloColor, int cx, int cy) {
        if (text == null || text.length() == 0) {
            return false;
        }
        Font font = fontFor(sizePx);
        int w = font.stringWidth(text);
        int h = font.getHeight();
        int x = cx - w / 2;
        int y = cy - h / 2;
        int bx = x - 2;
        int by = y - 2;
        int bw = w + 4;
        int bh = h + 4;
        for (int i = 0; i < occupied.size(); i++) {
            int[] o = (int[]) occupied.get(i);
            if (intersects(bx, by, bw, bh, o[0], o[1], o[2], o[3])) {
                return false;
            }
        }
        occupied.add(new int[]{bx, by, bw, bh});
        draw(g, text, font, textColor, haloColor, x, y);
        return true;
    }

    private void draw(Graphics g, String text, Font font, int textColor, int haloColor, int x, int y) {
        g.setFont(font);
        int prevAlpha = g.getAlpha();
        int haloA = (haloColor >>> 24) & 0xff;
        if (haloA > 0) {
            g.setAlpha(haloA);
            g.setColor(haloColor & 0xffffff);
            g.drawString(text, x - 1, y);
            g.drawString(text, x + 1, y);
            g.drawString(text, x, y - 1);
            g.drawString(text, x, y + 1);
        }
        int textA = (textColor >>> 24) & 0xff;
        if (textA == 0) {
            textA = 255;
        }
        g.setAlpha(textA);
        g.setColor(textColor & 0xffffff);
        g.drawString(text, x, y);
        g.setAlpha(prevAlpha);
    }

    private Font fontFor(double sizePx) {
        int bucket;
        int sizeConst;
        if (sizePx <= 12) {
            bucket = 0;
            sizeConst = Font.SIZE_SMALL;
        } else if (sizePx <= 17) {
            bucket = 1;
            sizeConst = Font.SIZE_MEDIUM;
        } else {
            bucket = 2;
            sizeConst = Font.SIZE_LARGE;
        }
        Integer k = Integer.valueOf(bucket);
        Font f = (Font) fontCache.get(k);
        if (f == null) {
            f = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, sizeConst);
            fontCache.put(k, f);
        }
        return f;
    }

    private static boolean intersects(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }
}
