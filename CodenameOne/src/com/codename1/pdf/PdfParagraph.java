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
package com.codename1.pdf;

import com.codename1.impl.pdf.PdfBidi;
import java.util.ArrayList;
import java.util.List;

/// A block of text for [PdfLayout], wrapped to the available width. It is
/// made of runs, each with its own font, size, color and optional link:
///
/// ```java
/// layout.add(new PdfParagraph()
///         .append("Total due: ", PdfFont.HELVETICA, 11)
///         .append("$1,240.00", PdfFont.HELVETICA_BOLD, 11)
///         .setAlignment(PdfParagraph.ALIGN_RIGHT));
/// ```
///
/// Lines break at spaces and after hyphens; a word wider than the line is
/// broken between characters. A `\n` in the text forces a line break.
///
/// Text is given in reading order. Each line is ordered for display by the
/// Unicode bidirectional algorithm, so Hebrew and Arabic read right to left
/// with numbers and Latin words inside them kept left to right, and Arabic
/// letters are joined. The paragraph direction comes from its first strong
/// character unless set with [#setDirection(int)]; it decides which side
/// lines start from when no alignment is set and which side the first-line
/// indent is on.
@com.codename1.impl.SharedWithBackend
public final class PdfParagraph {
    /// Lines start at the left edge.
    public static final int ALIGN_LEFT = 0;
    /// Lines are centered.
    public static final int ALIGN_CENTER = 1;
    /// Lines end at the right edge.
    public static final int ALIGN_RIGHT = 2;
    /// Lines fill the width by widening the spaces, except the last.
    public static final int ALIGN_JUSTIFY = 3;
    /// Lines start at the side the paragraph direction reads from: the
    /// left for left-to-right text, the right for right-to-left. The
    /// default.
    public static final int ALIGN_START = 4;

    /// The direction comes from the first strong character. The default.
    public static final int DIRECTION_AUTO = -1;
    /// Left to right.
    public static final int DIRECTION_LTR = 0;
    /// Right to left.
    public static final int DIRECTION_RTL = 1;

    final List<Run> runs = new ArrayList<Run>();
    int alignment = ALIGN_START;
    int direction = DIRECTION_AUTO;
    float lineSpacing = 1.15f;
    float spaceBefore;
    float spaceAfter = 6;
    float indent;

    /// An empty paragraph; add text with the `append` methods.
    public PdfParagraph() {
    }

    /// A paragraph of `text` in one font.
    public PdfParagraph(String text, PdfFont font, float size) {
        append(text, font, size);
    }

    /// Adds black text.
    public PdfParagraph append(String text, PdfFont font, float size) {
        return append(text, font, size, 0);
    }

    /// Adds text in color `rgb` (`0xRRGGBB`).
    public PdfParagraph append(String text, PdfFont font, float size, int rgb) {
        runs.add(new Run(text, font, size, rgb, null));
        return this;
    }

    /// Adds text that opens `uri` when clicked, in color `rgb`.
    public PdfParagraph appendLink(String text, String uri, PdfFont font, float size, int rgb) {
        runs.add(new Run(text, font, size, rgb, uri));
        return this;
    }

    /// Sets the alignment, one of the `ALIGN_` constants.
    public PdfParagraph setAlignment(int alignment) {
        this.alignment = alignment;
        return this;
    }

    /// Sets the paragraph direction, one of the `DIRECTION_` constants.
    public PdfParagraph setDirection(int direction) {
        this.direction = direction;
        return this;
    }

    /// The direction the paragraph resolves to, [#DIRECTION_LTR] or
    /// [#DIRECTION_RTL].
    int resolvedDirection() {
        if (direction != DIRECTION_AUTO) {
            return direction;
        }
        for (int i = 0; i < runs.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            int d = PdfBidi.direction(runs.get(i).text, -1);
            if (d >= 0) {
                return d;
            }
        }
        return DIRECTION_LTR;
    }

    /// Sets the distance between baselines as a multiple of the largest
    /// font's natural line height; 1.15 by default.
    public PdfParagraph setLineSpacing(float multiple) {
        this.lineSpacing = multiple;
        return this;
    }

    /// Sets the space above the paragraph in points; 0 by default.
    public PdfParagraph setSpaceBefore(float points) {
        this.spaceBefore = points;
        return this;
    }

    /// Sets the space below the paragraph in points; 6 by default.
    public PdfParagraph setSpaceAfter(float points) {
        this.spaceAfter = points;
        return this;
    }

    /// Indents the first line by `points`.
    public PdfParagraph setFirstLineIndent(float points) {
        this.indent = points;
        return this;
    }

    static final class Run {
        final String text;
        final PdfFont font;
        final float size;
        final int rgb;
        final String uri;

        Run(String text, PdfFont font, float size, int rgb, String uri) {
            this.text = text == null ? "" : text;
            this.font = font;
            this.size = size;
            this.rgb = rgb;
            this.uri = uri;
        }
    }

    /// A piece of one run on one line.
    static final class Fragment {
        final Run run;
        final String text;
        final float width;
        final int spaces;

        Fragment(Run run, String text) {
            this.run = run;
            this.text = text;
            this.width = run.font.getStringWidth(text, run.size);
            int n = 0;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == ' ') {
                    n++;
                }
            }
            this.spaces = n;
        }
    }

    static final class Line {
        final List<Fragment> fragments = new ArrayList<Fragment>();
        float width;
        float ascent;
        float height;
        boolean last;
        float offset;
    }

    /// Breaks the paragraph into lines no wider than `width`.
    List<Line> lines(float width) {
        List<Line> lines = new ArrayList<Line>();
        Line line = new Line();
        line.offset = indent;
        float avail = width - indent;
        for (int r = 0; r < runs.size(); r++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Run run = runs.get(r);
            String t = run.text;
            int pos = 0;
            while (pos < t.length()) {
                char c = t.charAt(pos);
                if (c == '\n') {
                    line.last = true;
                    finish(lines, line, run);
                    line = new Line();
                    avail = width;
                    pos++;
                    continue;
                }
                int end = pos;
                while (end < t.length() && t.charAt(end) != ' ' && t.charAt(end) != '\n'
                        && !(end > pos && t.charAt(end - 1) == '-')) {
                    end++;
                }
                int withSpace = end;
                while (withSpace < t.length() && t.charAt(withSpace) == ' ') {
                    withSpace++;
                }
                String word = t.substring(pos, end);
                float ww = run.font.getStringWidth(word, run.size);
                if (line.width + ww > avail && !line.fragments.isEmpty()) {
                    trimTrailingSpace(line);
                    finish(lines, line, run);
                    line = new Line();
                    avail = width;
                }
                if (ww > avail) {
                    // A word wider than a whole line: break between characters.
                    // Breaks fall only between clusters (never inside a
                    // surrogate pair or before a combining mark), and the
                    // width is summed cluster by cluster so a 100k character
                    // token costs linear time.
                    int start = pos;
                    float w = 0;
                    int i = pos;
                    int last = pos;
                    while (i < end) {
                        int next = nextCluster(t, i, end);
                        float cw = run.font.getStringWidth(t.substring(i, next), run.size);
                        if (w + cw > avail - line.width && i > start) {
                            // The sum leaves out kerning and joined forms;
                            // the fragment as drawn is measured once, and
                            // gives back a cluster when it is wider.
                            if (last > start && run.font.getStringWidth(t.substring(start, i), run.size)
                                    > avail - line.width) {
                                i = last;
                            }
                            add(line, run, t.substring(start, i));
                            finish(lines, line, run);
                            line = new Line();
                            avail = width;
                            start = i;
                            last = i;
                            w = 0;
                            // Measure the cluster at i again on the new line.
                            continue;
                        }
                        w += cw;
                        last = i;
                        i = next;
                    }
                    add(line, run, t.substring(start, withSpace));
                } else {
                    add(line, run, t.substring(pos, withSpace));
                }
                pos = withSpace;
            }
        }
        if (!line.fragments.isEmpty() || lines.isEmpty()) {
            line.last = true;
            finish(lines, line, runs.isEmpty() ? null : runs.get(runs.size() - 1));
        } else if (!lines.isEmpty()) {
            lines.get(lines.size() - 1).last = true;
        }
        return lines;
    }

    private static void add(Line line, Run run, String text) {
        if (text.length() == 0) {
            return;
        }
        int n = line.fragments.size();
        if (n > 0 && line.fragments.get(n - 1).run == run) { // NOPMD CompareObjectsWithEquals - identity of a sentinel object is intended
            Fragment prev = line.fragments.remove(n - 1);
            line.width -= prev.width;
            text = prev.text + text;
        }
        Fragment f = new Fragment(run, text);
        line.fragments.add(f);
        line.width += f.width;
    }

    private static void trimTrailingSpace(Line line) {
        int n = line.fragments.size();
        if (n == 0) {
            return;
        }
        Fragment f = line.fragments.get(n - 1);
        String t = f.text;
        int end = t.length();
        while (end > 0 && t.charAt(end - 1) == ' ') {
            end--;
        }
        if (end != t.length()) {
            line.fragments.remove(n - 1);
            line.width -= f.width;
            if (end > 0) {
                Fragment g = new Fragment(f.run, t.substring(0, end));
                line.fragments.add(g);
                line.width += g.width;
            }
        }
    }

    private void finish(List<Line> lines, Line line, Run fallback) {
        float asc = 0;
        float h = 0;
        for (int i = 0; i < line.fragments.size(); i++) {
            Run r = line.fragments.get(i).run;
            asc = Math.max(asc, r.font.getAscent(r.size));
            h = Math.max(h, r.font.getLineHeight(r.size) / 1.15f * lineSpacing);
        }
        if (line.fragments.isEmpty() && fallback != null) {
            asc = fallback.font.getAscent(fallback.size);
            h = fallback.font.getLineHeight(fallback.size) / 1.15f * lineSpacing;
        }
        line.ascent = asc;
        line.height = h;
        lines.add(line);
    }

    /// A piece of a line in one run and one direction.
    private static final class Piece {
        Run run;
        String text;
        boolean rtl;
    }

    /// The pieces of `line` from left to right.
    private static List<Piece> visual(Line line, int dir) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.fragments.size(); i++) {
            sb.append(line.fragments.get(i).text);
        }
        String text = sb.toString();
        List<Piece> out = new ArrayList<Piece>();
        if (dir == DIRECTION_LTR && !PdfBidi.hasRtl(text)) {
            for (int i = 0; i < line.fragments.size(); i++) {
                Piece p = new Piece();
                p.run = line.fragments.get(i).run;
                p.text = line.fragments.get(i).text;
                out.add(p);
            }
            return out;
        }
        byte[] levels = PdfBidi.levels(text, dir);
        int[] frag = new int[text.length()];
        int pos = 0;
        for (int i = 0; i < line.fragments.size(); i++) {
            int len = line.fragments.get(i).text.length();
            for (int k = 0; k < len; k++) {
                frag[pos++] = i;
            }
        }
        // Split into runs of one fragment and one level, then order them.
        List<int[]> spans = new ArrayList<int[]>();
        int start = 0;
        for (int i = 1; i <= text.length(); i++) {
            if (i == text.length() || levels[i] != levels[start] || frag[i] != frag[start]) {
                spans.add(new int[] {start, i});
                start = i;
            }
        }
        byte[] sl = new byte[spans.size()];
        for (int i = 0; i < sl.length; i++) {
            sl[i] = levels[spans.get(i)[0]];
        }
        int[] order = PdfBidi.order(sl);
        for (int i = 0; i < order.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            int[] sp = spans.get(order[i]);
            Piece p = new Piece();
            p.run = line.fragments.get(frag[sp[0]]).run;
            p.text = text.substring(sp[0], sp[1]);
            p.rtl = (levels[sp[0]] & 1) != 0;
            out.add(p);
        }
        return out;
    }

    /// Draws `line` with its top at `y` inside `[x, x + width]`.
    void draw(PdfPage page, Line line, float x, float y, float width) {
        PdfCanvas c = page.getCanvas();
        int dir = resolvedDirection();
        boolean rtlPara = dir == DIRECTION_RTL;
        float slack = width - line.offset - line.width;
        float cx = rtlPara ? x : x + line.offset;
        float wordSpace = 0;
        int align = alignment;
        if (align == ALIGN_START || align == ALIGN_JUSTIFY && line.last) {
            align = rtlPara ? ALIGN_RIGHT : ALIGN_LEFT;
        }
        if (align == ALIGN_CENTER) {
            cx += slack / 2;
        } else if (align == ALIGN_RIGHT) {
            cx += slack;
        } else if (align == ALIGN_JUSTIFY && slack > 0) {
            int spaces = 0;
            for (int i = 0; i < line.fragments.size(); i++) {
                spaces += line.fragments.get(i).spaces;
            }
            if (spaces > 0) {
                wordSpace = slack / spaces;
            } else if (rtlPara) {
                cx += slack;
            }
        }
        float baseline = y + line.ascent + (line.height - line.ascent - descent(line)) / 2;
        List<Piece> pieces = visual(line, dir);
        for (int i = 0; i < pieces.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Piece f = pieces.get(i);
            c.setFillColor(f.run.rgb);
            c.setFont(f.run.font, f.run.size);
            float w;
            if (wordSpace > 0) {
                // Draw word by word so justification works with any font
                // encoding (Tw only applies to single-byte code 32). A
                // right-to-left piece places its words from the right.
                List<String> words = new ArrayList<String>();
                String t = f.text;
                int start = 0;
                for (int k = 0; k <= t.length(); k++) {
                    if (k == t.length() || t.charAt(k) == ' ') {
                        words.add(t.substring(start, k));
                        if (k < t.length()) {
                            words.add(" ");
                        }
                        start = k + 1;
                    }
                }
                float wx = cx;
                float space = f.run.font.getStringWidth(" ", f.run.size);
                for (int k = 0; k < words.size(); k++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                    String word = words.get(f.rtl ? words.size() - 1 - k : k);
                    if (" ".equals(word)) {
                        wx += space + wordSpace;
                    } else if (word.length() > 0) {
                        c.drawRun(word, wx, baseline, f.rtl);
                        wx += f.run.font.getStringWidth(word, f.run.size);
                    }
                }
                w = wx - cx;
            } else {
                c.drawRun(f.text, cx, baseline, f.rtl);
                w = f.run.font.getStringWidth(f.text, f.run.size);
            }
            if (f.run.uri != null) {
                page.addLink(cx, y, w, line.height, f.run.uri);
            }
            cx += w;
        }
    }

    private static float descent(Line line) {
        float d = 0;
        for (int i = 0; i < line.fragments.size(); i++) {
            Run r = line.fragments.get(i).run;
            d = Math.max(d, r.font.getDescent(r.size));
        }
        return d;
    }

    /// The index after the cluster starting at `i`: a character with its
    /// low surrogate, and any combining marks, joiners and variation
    /// selectors after it.
    static int nextCluster(String t, int i, int end) {
        int j = i + 1;
        if (j < end && isHigh(t.charAt(i)) && isLow(t.charAt(j))) {
            j++;
        }
        while (j < end) {
            char c = t.charAt(j);
            boolean extend = c >= 0x0300 && c <= 0x036f || c >= 0x0483 && c <= 0x0489 || c >= 0x0591 && c <= 0x05bd || c == 0x05bf || c == 0x05c1 || c == 0x05c2 || c == 0x05c4 || c == 0x05c5 || c == 0x05c7
                    || c >= 0x0610 && c <= 0x061a || c >= 0x064b && c <= 0x065f || c == 0x0670
                    || c >= 0x06d6 && c <= 0x06ed || c == 0x200c || c == 0x200d || c >= 0xfe00 && c <= 0xfe0f
                    || c >= 0x20d0 && c <= 0x20ff;
            if (c == 0x200d && j + 1 < end) {
                // A joiner glues the next character (and its own low
                // surrogate) onto this cluster, as in emoji sequences.
                j++;
                if (j + 1 < end && isHigh(t.charAt(j))) {
                    j++;
                }
                j++;
                continue;
            }
            if (isLow(c)) {
                extend = true;
            }
            if (!extend) {
                break;
            }
            j++;
        }
        return Math.min(j, end);
    }

    private static boolean isHigh(char c) {
        return c >= 0xd800 && c <= 0xdbff;
    }

    private static boolean isLow(char c) {
        return c >= 0xdc00 && c <= 0xdfff;
    }
}
