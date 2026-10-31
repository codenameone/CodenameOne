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
package com.codename1.fxcompat.runtime;

import java.util.ArrayList;
import java.util.List;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;

/// The native peer of a JavaFX `Label`: a Codename One label that, while
/// wrapping is on, breaks its text into lines at its own width and paints
/// them itself. With wrapping off it is a plain label in every respect.
///
/// Lines break at spaces and at line feeds; a word wider than the whole
/// line is broken between characters, and only then. Each line is placed
/// by the label's alignment, and the block of lines is centred in the
/// height the label is given, as JavaFX centres a wrapped text.
public class WrappingLabel extends com.codename1.ui.Label {

    private boolean wrap;

    /// Turns wrapping on or off.
    public void setWrap(boolean wrap) {
        if (this.wrap != wrap) {
            this.wrap = wrap;
            repaint();
        }
    }

    /// Whether the text wraps.
    public boolean isWrap() {
        return wrap;
    }

    /// Breaks a text into the lines it takes at a width in device pixels.
    public static List<String> lines(Font font, String text, int width) {
        List<String> out = new ArrayList<String>();
        if (text == null || text.length() == 0) {
            out.add("");
            return out;
        }
        int start = 0;
        while (true) {
            int nl = text.indexOf('\n', start);
            int end = nl < 0 ? text.length() : nl;
            breakParagraph(font, text.substring(start, end), width, out);
            if (nl < 0) {
                break;
            }
            start = nl + 1;
        }
        return out;
    }

    private static void breakParagraph(Font font, String p, int width, List<String> out) {
        if (p.length() == 0 || width <= 0 || font.stringWidth(p) <= width) {
            out.add(p);
            return;
        }
        int pos = 0;
        int n = p.length();
        boolean first = true;
        while (pos < n) {
            // Spaces at the start of a wrapped line are dropped, as JavaFX
            // drops them.
            if (!first) {
                while (pos < n && p.charAt(pos) == ' ') {
                    pos++;
                }
                if (pos >= n) {
                    break;
                }
            }
            first = false;
            int lastFit = -1;
            for (int i = pos + 1; i <= n; i++) {
                if (i == n || p.charAt(i) == ' ') {
                    if (font.stringWidth(p.substring(pos, i)) > width) {
                        break;
                    }
                    lastFit = i;
                }
            }
            if (lastFit > pos) {
                out.add(p.substring(pos, lastFit));
                pos = lastFit;
                continue;
            }
            // A single word wider than the line: as many characters as fit,
            // at least one so the loop always moves on.
            int e = pos + 1;
            while (e < n && p.charAt(e) != ' ' && font.stringWidth(p.substring(pos, e + 1)) <= width) {
                e++;
            }
            out.add(p.substring(pos, e));
            pos = e;
        }
    }

    @Override
    public void paint(Graphics g) {
        String text = getText();
        Style s = getStyle();
        Font f = s.getFont();
        if (!wrap || f == null || text == null || text.length() == 0) {
            super.paint(g);
            return;
        }
        int left = s.getPaddingLeftNoRTL();
        int right = s.getPaddingRightNoRTL();
        int top = s.getPaddingTop();
        int bottom = s.getPaddingBottom();
        int area = getWidth() - left - right;
        if (text.indexOf('\n') < 0 && f.stringWidth(text) <= area) {
            super.paint(g);
            return;
        }
        List<String> lines = lines(f, text, area);
        int lh = f.getHeight();
        int block = lh * lines.size();
        int y = getY() + top + Math.max(0, (getHeight() - top - bottom - block) / 2);
        int oldColor = g.getColor();
        int oldAlpha = g.getAlpha();
        Font oldFont = g.getFont();
        g.setFont(f);
        g.setColor(s.getFgColor());
        g.setAlpha(s.getOpacity() & 0xff);
        int align = getAlignment();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int x = getX() + left;
            if (align == CENTER) {
                x += (area - f.stringWidth(line)) / 2;
            } else if (align == RIGHT) {
                x += area - f.stringWidth(line);
            }
            g.drawString(line, x, y);
            y += lh;
        }
        g.setFont(oldFont);
        g.setColor(oldColor);
        g.setAlpha(oldAlpha);
    }
}
