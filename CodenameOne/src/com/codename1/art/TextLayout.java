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
package com.codename1.art;

import com.codename1.ui.Font;

import java.util.ArrayList;
import java.util.List;

/// Breaks a `TextNode` into lines, measured with the fonts the renderer draws with.
final class TextLayout {
    /// One styled piece of a line.
    static final class Piece {
        final TextRun run;
        final String text;
        double x;
        double width;

        Piece(TextRun run, String text) {
            this.run = run;
            this.text = text;
        }
    }

    /// One line.
    static final class Line {
        final List<Piece> pieces = new ArrayList<Piece>();
        double width;
        double baseline;
        double size;
    }

    private TextLayout() {
    }

    static double measure(TextRun run, String s, double scale) {
        float px = (float) Math.max(1, run.getSize() * scale);
        Font f = ArtFonts.font(run, px);
        double w;
        if (f == null) {
            w = s.length() * run.getSize() * 0.55;
        } else {
            double fontScale = px / Math.max(1, f.getHeight() / 1.15);
            w = f.stringWidth(s) / scale;
            if (fontScale < 0.8 || fontScale > 1.25) {
                w = w * fontScale;
            }
        }
        return w + run.getLetterSpacing() * s.length();
    }

    static List<Line> layout(TextNode node, double scale) {
        List<Line> lines = new ArrayList<Line>();
        Line cur = new Line();
        lines.add(cur);
        double box = node.getBoxWidth();
        List<TextRun> runs = node.getRuns();
        for (TextRun run : runs) {
            String t = run.getText();
            int st = 0;
            for (int i = 0; i <= t.length(); i++) {
                boolean end = i == t.length();
                char c = end ? 0 : t.charAt(i);
                if (end || c == '\n' || c == ' ') {
                    String word = t.substring(st, end || c == '\n' ? i : i + 1);
                    if (word.length() > 0) {
                        double ww = measure(run, word, scale);
                        if (box > 0 && cur.width + ww > box && !cur.pieces.isEmpty()) {
                            cur = new Line();
                            lines.add(cur);
                            if (word.startsWith(" ")) {
                                word = word.substring(1);
                                ww = measure(run, word, scale);
                            }
                        }
                        Piece p = new Piece(run, word);
                        p.x = cur.width;
                        p.width = ww;
                        cur.pieces.add(p);
                        cur.width += ww;
                        cur.size = Math.max(cur.size, run.getSize());
                    }
                    if (!end && c == '\n') {
                        cur = new Line();
                        lines.add(cur);
                    }
                    st = i + 1;
                }
            }
        }
        double y = node.getY();
        double lh = node.getLineHeight();
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            if (l.size == 0) {
                l.size = node.maxSize();
            }
            if (i > 0) {
                y += l.size * lh;
            }
            l.baseline = y;
            double off = 0;
            if (node.getAlign() == TextNode.ALIGN_CENTER) {
                off = box > 0 ? (box - l.width) / 2 : -l.width / 2;
            } else if (node.getAlign() == TextNode.ALIGN_RIGHT) {
                off = box > 0 ? box - l.width : -l.width;
            }
            for (int j = 0; j < l.pieces.size(); j++) {
                l.pieces.get(j).x += node.getX() + off;
            }
        }
        return lines;
    }

    static double[] bounds(TextNode node) {
        if (node.getText().length() == 0) {
            return null;
        }
        List<Line> lines = layout(node, 1);
        double[] b = null;
        for (Line l : lines) {
            for (int j = 0; j < l.pieces.size(); j++) {
                Piece p = l.pieces.get(j);
                double[] pb = {p.x, l.baseline - l.size * 0.9, p.width, l.size * 1.15};
                b = ArtNode.union(b, pb);
            }
        }
        return b;
    }
}
