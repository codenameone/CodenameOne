/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.RichTextComponent;
import com.codename1.ui.editor.RichBlocks;
import com.codename1.ui.editor.RichRunPainter;
import com.codename1.ui.editor.TextStyle;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link RichTextComponent} that backs {@link RichText}, extended with
 * {@code TextAlign.justify}. The core component aligns a row only LEFT, CENTER or RIGHT and keeps
 * its rows private, so a justified paragraph is laid out here instead: the same greedy word wrap,
 * the same fonts (through the shared {@link RichRunPainter}) and the same line and paragraph gaps,
 * after which every row but the last of a paragraph has its slack spread over its inter-word gaps,
 * as Flutter does. With justify off every call goes to the core component unchanged.
 *
 * <p>The line breaking and gap distribution are in {@link #layout}, which measures through a
 * {@link Measure} so that it can be tested without a Display.</p>
 */
public class JustifiedRichTextComponent extends RichTextComponent {

    private final List<String> runTexts = new ArrayList<String>();
    private final List<TextStyle> runStyles = new ArrayList<TextStyle>();
    private final RichRunPainter painter = new RichRunPainter();
    private boolean justify;
    private boolean rtl;
    private List<Line> lines;
    private int linesWidth = -1;
    private int contentW;
    private int contentH;

    /** Measures a fragment of run {@code run}; widths and heights are in pixels. */
    public interface Measure {
        int wordWidth(int run, String text);

        int spaceWidth(int run);

        int height(int run);
    }

    /** A positioned fragment of one run on a line. */
    public static final class Frag {
        public final int run;
        public final String text;
        public int x;

        Frag(int run, String text, int x) {
            this.run = run;
            this.text = text;
            this.x = x;
        }
    }

    /** One laid-out line. */
    public static final class Line {
        public final List<Frag> frags = new ArrayList<Frag>();
        public int y;
        public int height;
        /** Natural width, before any justification. */
        public int width;
        /** True for the last line of a paragraph, which keeps the start alignment. */
        public boolean last;
    }

    /** Result of {@link #layout}. */
    public static final class Result {
        public final List<Line> lines = new ArrayList<Line>();
        public int width;
        public int height;
    }

    public JustifiedRichTextComponent() {
        super();
    }

    /** Clears the runs this component lays out when justifying. */
    void clearRuns() {
        runTexts.clear();
        runStyles.clear();
        invalidateLines();
    }

    @Override
    public RichTextComponent append(String runText, TextStyle style) {
        runTexts.add(runText);
        runStyles.add(style);
        invalidateLines();
        return super.append(runText, style);
    }

    /**
     * Turns justification on or off.
     * @param justify true for {@code TextAlign.justify}
     * @param rtl true when the paragraph runs right to left: a justified paragraph's last line
     *            then sits at the right edge
     */
    void setJustify(boolean justify, boolean rtl) {
        if (this.justify != justify || this.rtl != rtl) {
            this.justify = justify;
            this.rtl = rtl;
            invalidateLines();
            setShouldCalcPreferredSize(true);
        }
    }

    boolean isJustify() {
        return justify;
    }

    private void invalidateLines() {
        lines = null;
        linesWidth = -1;
    }

    // ------------------------------------------------------------------
    // Sizing and painting (justify only)
    // ------------------------------------------------------------------

    private void preparePainter() {
        Style style = getStyle();
        Font f = style.getFont();
        painter.setBaseSizePx(0);
        painter.setBaseFont(f == null ? Font.getDefaultFont() : f);
        painter.setTextColor(style.getFgColor());
    }

    private void ensureLines(int avail) {
        if (lines != null && linesWidth == avail) {
            return;
        }
        preparePainter();
        final List<Font> fonts = new ArrayList<Font>();
        for (TextStyle st : runStyles) {
            fonts.add(painter.runFont(RichBlocks.PARAGRAPH, st));
        }
        int base = painter.getBaseSizePx();
        Result r = layout(runTexts, new Measure() {
            @Override
            public int wordWidth(int run, String text) {
                return fonts.get(run).stringWidth(text);
            }

            @Override
            public int spaceWidth(int run) {
                return fonts.get(run).charWidth(' ');
            }

            @Override
            public int height(int run) {
                return fonts.get(run).getHeight();
            }
        }, avail, base, Math.round(base * 0.2f), Math.round(base * 0.45f), true);
        lines = r.lines;
        linesWidth = avail;
        contentW = r.width;
        contentH = r.height;
    }

    @Override
    public Dimension preferredSizeForWidth(int width) {
        if (!justify) {
            return super.preferredSizeForWidth(width);
        }
        Style style = getStyle();
        ensureLines(Math.max(1, width - style.getHorizontalPadding()));
        return new Dimension(contentW + style.getHorizontalPadding(), contentH + style.getVerticalPadding());
    }

    @Override
    protected Dimension calcPreferredSize() {
        if (!justify) {
            return super.calcPreferredSize();
        }
        Style style = getStyle();
        int w = getWidth();
        int avail = w > 0 ? w - style.getHorizontalPadding() : Integer.MAX_VALUE / 4;
        ensureLines(Math.max(1, avail));
        return new Dimension(contentW + style.getHorizontalPadding(), contentH + style.getVerticalPadding());
    }

    @Override
    public void paint(Graphics g) {
        if (!justify) {
            super.paint(g);
            return;
        }
        Style style = getStyle();
        int avail = Math.max(1, getWidth() - style.getHorizontalPadding());
        ensureLines(avail);
        int originX = getX() + style.getPaddingLeftNoRTL();
        int originY = getY() + style.getPaddingTop();
        for (Line line : lines) {
            int shift = line.last && rtl ? Math.max(0, avail - line.width) : 0;
            for (Frag f : line.frags) {
                TextStyle st = runStyles.get(f.run);
                Font font = painter.runFont(RichBlocks.PARAGRAPH, st);
                painter.paintRun(g, f.text, st, font, originX + shift + f.x, originY + line.y, line.height);
            }
        }
    }

    // ------------------------------------------------------------------
    // Pure layout
    // ------------------------------------------------------------------

    /**
     * Greedy word wrap of {@code runs} at {@code avail} pixels, then justification. A word may span
     * runs; a {@code '\n'} ends a paragraph. On every line that is not the last of its paragraph
     * the slack ({@code avail} minus the natural width) is divided over the line's inter-word
     * gaps, the remainder going one pixel each to the first gaps. Words carry no spaces: each
     * gap's position is the word's x.
     *
     * @param runs the run texts
     * @param m measures fragments
     * @param avail the available width
     * @param base the base line height for an empty line
     * @param lineGap the gap after each line
     * @param paraGap the extra gap before each paragraph but the first
     * @param stretch false lays out from the start edge without justifying
     * @return the lines with positioned fragments
     */
    public static Result layout(List<String> runs, Measure m, int avail, int base, int lineGap,
            int paraGap, boolean stretch) {
        Result res = new Result();
        // tokens of the current paragraph: a word is a list of frags, a space is a frag with null text
        List<List<Frag>> words = new ArrayList<List<Frag>>();
        List<Integer> spaceBefore = new ArrayList<Integer>();
        int[] y = {0};
        int pendingSpace = 0;
        List<Frag> word = null;
        boolean firstPara = true;
        for (int r = 0; r < runs.size(); r++) {
            String t = runs.get(r);
            if (t == null) {
                continue;
            }
            int start = 0;
            for (int i = 0; i <= t.length(); i++) {
                char c = i < t.length() ? t.charAt(i) : 0;
                boolean breakChar = i < t.length() && (c == ' ' || c == '\t' || c == '\n');
                if (i == t.length() || breakChar) {
                    if (i > start) {
                        if (word == null) {
                            word = new ArrayList<Frag>();
                            words.add(word);
                            spaceBefore.add(Integer.valueOf(pendingSpace));
                            pendingSpace = 0;
                        }
                        String piece = t.substring(start, i);
                        word.add(new Frag(r, piece, m.wordWidth(r, piece)));
                    }
                    start = i + 1;
                }
                if (breakChar) {
                    word = null;
                    if (c == '\n') {
                        wrapParagraph(words, spaceBefore, m, avail, base, lineGap,
                                firstPara ? 0 : paraGap, stretch, res, y);
                        firstPara = false;
                        words.clear();
                        spaceBefore.clear();
                        pendingSpace = 0;
                    } else {
                        pendingSpace += m.spaceWidth(r);
                    }
                }
            }
        }
        wrapParagraph(words, spaceBefore, m, avail, base, lineGap, firstPara ? 0 : paraGap, stretch, res, y);
        res.height = Math.max(base, y[0]);
        return res;
    }

    private static void wrapParagraph(List<List<Frag>> words, List<Integer> spaceBefore, Measure m,
            int avail, int base, int lineGap, int paraGap, boolean stretch, Result res, int[] y) {
        y[0] += paraGap;
        List<Line> para = new ArrayList<Line>();
        Line line = new Line();
        // per line: word start indices into line.frags and the gaps between words
        List<List<Integer>> wordStarts = new ArrayList<List<Integer>>();
        List<Integer> starts = new ArrayList<Integer>();
        int x = 0;
        for (int w = 0; w < words.size(); w++) {
            List<Frag> word = words.get(w);
            int ww = 0;
            for (Frag f : word) {
                ww += f.x; // measured width was stashed in x
            }
            int sp = spaceBefore.get(w).intValue();
            if (!line.frags.isEmpty() && x + sp + ww > avail) {
                line.width = x;
                para.add(line);
                wordStarts.add(starts);
                line = new Line();
                starts = new ArrayList<Integer>();
                x = 0;
            } else if (!line.frags.isEmpty()) {
                x += sp;
            }
            starts.add(Integer.valueOf(line.frags.size()));
            for (Frag f : word) {
                int fw = f.x;
                f.x = x;
                x += fw;
                line.frags.add(f);
                line.height = Math.max(line.height, m.height(f.run));
            }
        }
        line.width = x;
        para.add(line);
        wordStarts.add(starts);
        para.get(para.size() - 1).last = true;
        for (int i = 0; i < para.size(); i++) {
            Line l = para.get(i);
            if (l.height == 0) {
                l.height = base;
            }
            List<Integer> ws = wordStarts.get(i);
            int gaps = ws.size() - 1;
            if (stretch && !l.last && gaps > 0 && avail > l.width) {
                int slack = avail - l.width;
                int each = slack / gaps;
                int extra = slack % gaps;
                int shift = 0;
                for (int g = 1; g <= gaps; g++) {
                    shift += each + (g <= extra ? 1 : 0);
                    int from = ws.get(g).intValue();
                    int to = g < gaps ? ws.get(g + 1).intValue() : l.frags.size();
                    for (int k = from; k < to; k++) {
                        l.frags.get(k).x += shift;
                    }
                }
            }
            l.y = y[0];
            y[0] += l.height + lineGap;
            // a stretched line fills the width; only a wrapped paragraph has one, so avail is finite
            res.width = Math.max(res.width, stretch && !l.last && gaps > 0 ? avail : l.width);
            res.lines.add(l);
        }
    }
}
