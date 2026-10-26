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
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Toolkit;
import com.codename1.desktopcompat.javax.swing.text.AttributeSet;
import com.codename1.desktopcompat.javax.swing.text.BadLocationException;
import com.codename1.desktopcompat.javax.swing.text.Element;
import com.codename1.desktopcompat.javax.swing.text.StyleConstants;
import com.codename1.desktopcompat.javax.swing.text.StyledDocument;
import java.util.ArrayList;

/// The lines a styled document is drawn as.
///
/// Every run of the document is set in its own font and color; a
/// paragraph is broken at its spaces where it would pass the width it is
/// given, a word longer than the width is broken where it must be, and a
/// line is as high as the tallest font in it. An attribute a run does not
/// have -- neither itself, nor its paragraph, nor the paragraph's style --
/// comes from the component: its font, and its foreground.
///
/// A layout describes the document as it was when it was made.
public final class StyledText {

    private static final class Seg {
        int start;
        int end;
        int x;
        int width;
        Font font;
        Color fg;
        Color bg;
        boolean underline;
        boolean strike;
    }

    private static final class Line {
        final ArrayList<Seg> segs = new ArrayList<Seg>();
        /// The offsets the line stands for; `end` takes in the line break.
        int start;
        int end;
        int x;
        int y;
        int width;
        int height;
        int ascent;
    }

    private final String text;
    private final ArrayList<Line> lines = new ArrayList<Line>();
    private int width;
    private int height;

    /// Whether any of `doc` asks to be drawn other than as the plain text
    /// of the component.
    public static boolean styled(StyledDocument doc) {
        Element root = doc.getDefaultRootElement();
        for (int p = 0; p < root.getElementCount(); p++) {
            Element para = root.getElement(p);
            if (para == null) {
                continue;
            }
            if (para.getAttributes().getAttribute(StyleConstants.Alignment) != null) {
                return true;
            }
            for (int i = 0; i < para.getElementCount(); i++) {
                Element leaf = para.getElement(i);
                if (leaf != null && rendering(leaf.getAttributes())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean rendering(AttributeSet a) {
        return a.getAttribute(StyleConstants.Foreground) != null || a.getAttribute(StyleConstants.Background) != null
                || a.getAttribute(StyleConstants.Bold) != null || a.getAttribute(StyleConstants.Italic) != null
                || a.getAttribute(StyleConstants.Underline) != null
                || a.getAttribute(StyleConstants.StrikeThrough) != null
                || a.getAttribute(StyleConstants.FontFamily) != null
                || a.getAttribute(StyleConstants.FontSize) != null;
    }

    /// Lays `doc` out in lines no wider than `room`; with `room` of zero
    /// or less only the line breaks of the text end a line.
    public StyledText(StyledDocument doc, Font base, Color foreground, int room) {
        String all;
        try {
            all = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            all = "";
        }
        text = all;
        Element root = doc.getDefaultRootElement();
        int y = 0;
        for (int p = 0; p < root.getElementCount(); p++) {
            Element para = root.getElement(p);
            if (para == null) {
                continue;
            }
            int first = lines.size();
            paragraph(para, base, foreground, room);
            Object align = para.getAttributes().getAttribute(StyleConstants.Alignment);
            int how = align instanceof Integer ? ((Integer) align).intValue() : StyleConstants.ALIGN_LEFT;
            for (int i = first; i < lines.size(); i++) {
                Line line = lines.get(i);
                line.y = y;
                y += line.height;
                if (room > 0 && how == StyleConstants.ALIGN_CENTER) {
                    line.x = Math.max(0, (room - line.width) / 2);
                } else if (room > 0 && how == StyleConstants.ALIGN_RIGHT) {
                    line.x = Math.max(0, room - line.width);
                }
                width = Math.max(width, line.width);
            }
        }
        height = y;
    }

    private static Font font(AttributeSet a, Font base) {
        Object family = a.getAttribute(StyleConstants.FontFamily);
        Object size = a.getAttribute(StyleConstants.FontSize);
        Object bold = a.getAttribute(StyleConstants.Bold);
        Object italic = a.getAttribute(StyleConstants.Italic);
        if (family == null && size == null && bold == null && italic == null) {
            return base;
        }
        int style = base.getStyle();
        if (bold instanceof Boolean) {
            style = ((Boolean) bold).booleanValue() ? style | Font.BOLD : style & ~Font.BOLD;
        }
        if (italic instanceof Boolean) {
            style = ((Boolean) italic).booleanValue() ? style | Font.ITALIC : style & ~Font.ITALIC;
        }
        float points = base.getSize2D();
        if (size instanceof Integer) {
            points = ((Integer) size).intValue();
        }
        if (family instanceof String) {
            return new Font((String) family, style, Math.round(points));
        }
        return base.deriveFont(style, points);
    }

    private Line open(int at, Font base) {
        Line line = new Line();
        line.start = at;
        line.end = at;
        FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(base);
        line.height = fm.getHeight();
        line.ascent = fm.getAscent();
        lines.add(line);
        return line;
    }

    private void put(Line line, Seg like, int from, int to, FontMetrics fm) {
        if (from >= to) {
            return;
        }
        if (!line.segs.isEmpty()) {
            // More of the run the line ends with: one stretch, drawn and
            // measured as one string.
            Seg last = line.segs.get(line.segs.size() - 1);
            if (last.end == from && last.font == like.font && last.fg == like.fg && last.bg == like.bg
                    && last.underline == like.underline && last.strike == like.strike) {
                last.end = to;
                last.width = fm.stringWidth(text.substring(last.start, to));
                line.width = last.x + last.width;
                line.end = to;
                return;
            }
        }
        Seg s = new Seg();
        s.start = from;
        s.end = to;
        s.font = like.font;
        s.fg = like.fg;
        s.bg = like.bg;
        s.underline = like.underline;
        s.strike = like.strike;
        s.x = line.width;
        s.width = fm.stringWidth(text.substring(from, to));
        line.width += s.width;
        line.end = to;
        // An empty line has the height of the component's font; one with
        // text has that of the text.
        if (line.segs.isEmpty()) {
            line.height = fm.getHeight();
            line.ascent = fm.getAscent();
        } else {
            int below = Math.max(line.height - line.ascent, fm.getHeight() - fm.getAscent());
            line.ascent = Math.max(line.ascent, fm.getAscent());
            line.height = line.ascent + below;
        }
        line.segs.add(s);
    }

    private static boolean space(char c) {
        return c == ' ' || c == '\t';
    }

    private void paragraph(Element para, Font base, Color foreground, int room) {
        int pStart = para.getStartOffset();
        int pEnd = Math.min(para.getEndOffset(), text.length());
        // The text without the break that ends it.
        int stop = pEnd > pStart && text.charAt(pEnd - 1) == '\n' ? pEnd - 1 : pEnd;
        // The runs, as stretches with one look each.
        ArrayList<Seg> runs = new ArrayList<Seg>();
        for (int i = 0; i < para.getElementCount(); i++) {
            Element leaf = para.getElement(i);
            if (leaf == null) {
                continue;
            }
            Seg r = new Seg();
            r.start = Math.max(pStart, leaf.getStartOffset());
            r.end = Math.min(stop, leaf.getEndOffset());
            if (r.start >= r.end) {
                continue;
            }
            AttributeSet a = leaf.getAttributes();
            r.font = font(a, base);
            Object fg = a.getAttribute(StyleConstants.Foreground);
            r.fg = fg instanceof Color ? (Color) fg : foreground;
            Object bg = a.getAttribute(StyleConstants.Background);
            r.bg = bg instanceof Color ? (Color) bg : null;
            Object u = a.getAttribute(StyleConstants.Underline);
            r.underline = u instanceof Boolean && ((Boolean) u).booleanValue();
            Object s = a.getAttribute(StyleConstants.StrikeThrough);
            r.strike = s instanceof Boolean && ((Boolean) s).booleanValue();
            runs.add(r);
        }
        Line line = open(pStart, base);
        // A word is taken whole: its characters through every run it
        // crosses, and the spaces after it.
        int at = pStart;
        while (at < stop) {
            int wordEnd = at;
            while (wordEnd < stop && !space(text.charAt(wordEnd))) {
                wordEnd++;
            }
            int next = wordEnd;
            while (next < stop && space(text.charAt(next))) {
                next++;
            }
            if (room > 0 && line.width > 0 && line.width + measure(runs, at, wordEnd) > room) {
                line = open(at, base);
            }
            for (int i = 0; i < runs.size(); i++) {
                Seg r = runs.get(i);
                int from = Math.max(at, r.start);
                int to = Math.min(next, r.end);
                if (from >= to) {
                    continue;
                }
                FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(r.font);
                int part = Math.max(from, Math.min(to, wordEnd));
                while (from < part) {
                    if (room <= 0 || line.width + fm.stringWidth(text.substring(from, part)) <= room) {
                        put(line, r, from, part, fm);
                        from = part;
                        break;
                    }
                    // A word wider than the line: as much of it as fits,
                    // and one character where none does.
                    int fit = from;
                    int w = line.width;
                    while (fit < part) {
                        int cw = fm.charWidth(text.charAt(fit));
                        if (w + cw > room && (fit > from || line.width > 0)) {
                            break;
                        }
                        w += cw;
                        fit++;
                    }
                    if (fit == from) {
                        line = open(from, base);
                        continue;
                    }
                    put(line, r, from, fit, fm);
                    from = fit;
                    if (from < part) {
                        line = open(from, base);
                    }
                }
                // The spaces after the word stay on its line.
                put(line, r, from, to, fm);
            }
            at = next;
        }
        line.end = pEnd;
    }

    private int measure(ArrayList<Seg> runs, int from, int to) {
        int w = 0;
        for (int i = 0; i < runs.size(); i++) {
            Seg r = runs.get(i);
            int a = Math.max(from, r.start);
            int b = Math.min(to, r.end);
            if (a < b) {
                w += Toolkit.getDefaultToolkit().getFontMetrics(r.font).stringWidth(text.substring(a, b));
            }
        }
        return w;
    }

    /// The width of the widest line.
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLineCount() {
        return lines.size();
    }

    /// Draws the lines with their top left corner at `x0`, `y0`.
    public void paint(Graphics g, int x0, int y0) {
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            int base = y0 + line.y + line.ascent;
            for (int j = 0; j < line.segs.size(); j++) {
                Seg s = line.segs.get(j);
                int x = x0 + line.x + s.x;
                if (s.bg != null) {
                    g.setColor(s.bg);
                    g.fillRect(x, y0 + line.y, s.width, line.height);
                }
                if (s.fg != null) {
                    g.setColor(s.fg);
                }
                g.setFont(s.font);
                g.drawString(text.substring(s.start, s.end), x, base);
                if (s.underline) {
                    g.drawLine(x, base + 1, x + s.width - 1, base + 1);
                }
                if (s.strike) {
                    int mid = base - Math.max(1, line.ascent / 3);
                    g.drawLine(x, mid, x + s.width - 1, mid);
                }
            }
        }
    }

    private Line lineAt(int pos) {
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            if (pos < line.end || i == lines.size() - 1) {
                return line;
            }
        }
        return null;
    }

    /// The offset nearest a point, relative to the corner the lines are
    /// drawn from: the character whose nearer half the point is in.
    public int offsetAt(int x, int y) {
        if (lines.isEmpty()) {
            return 0;
        }
        Line line = lines.get(lines.size() - 1);
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            if (y < l.y + l.height) {
                line = l;
                break;
            }
        }
        int px = x - line.x;
        int last = line.start;
        for (int j = 0; j < line.segs.size(); j++) {
            Seg s = line.segs.get(j);
            last = s.end;
            if (px >= s.x + s.width) {
                continue;
            }
            FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(s.font);
            int at = s.x;
            for (int k = s.start; k < s.end; k++) {
                int cw = fm.charWidth(text.charAt(k));
                if (px < at + cw / 2 + (cw & 1)) {
                    return k;
                }
                at += cw;
            }
            return s.end;
        }
        return last;
    }

    /// Where the caret stands before the character at `pos`: one pixel
    /// wide and as high as its line.
    public Rectangle caretAt(int pos) {
        Line line = lineAt(pos);
        if (line == null) {
            return new Rectangle(0, 0, 1, 0);
        }
        int x = line.width;
        for (int j = 0; j < line.segs.size(); j++) {
            Seg s = line.segs.get(j);
            if (pos <= s.start) {
                x = s.x;
                break;
            }
            if (pos < s.end) {
                FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(s.font);
                x = s.x + fm.stringWidth(text.substring(s.start, pos));
                break;
            }
        }
        return new Rectangle(line.x + x, line.y, 1, line.height);
    }
}
