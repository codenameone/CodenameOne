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
package com.codename1.wordprocessing;

import java.util.ArrayList;
import java.util.List;

/// A document broken into pages: every line, table, picture and footnote placed, in points.
///
/// Layout is pure: it reads a `Document` and a `Measurer` and touches no user interface, so the
/// same result paints `DocumentView`, is written by the PDF export, and can run on a server.
///
/// ```java
/// DocumentLayout layout = new DocumentLayout(doc, new PdfMeasurer());
/// for (int i = 0; i < layout.getPageCount(); i++) {
///     for (LayoutItem item : layout.getPage(i).getItems()) {
///         // draw it
///     }
/// }
/// ```
///
/// Paragraphs break at spaces (a word wider than the line breaks between letters) and honor
/// alignment, justification, indents, spacing, line spacing, page breaks and keep-with-next.
/// List items get bullets or numbers, restarted where the paragraph says so. Table rows move to
/// the next page whole, repeating the header rows; a row taller than a page is split. Footnotes
/// are placed at the bottom of the page that references them and endnotes after the text. `PAGE`
/// and `NUMPAGES` fields show the real page number and count.
public final class DocumentLayout {
    private static final float TAB = 36;
    private static final float LIST_INDENT = 18;
    private static final float CELL_PAD = 4;
    private static final int BORDER = 0x9aa3ad;
    private static final int HEADER_FILL = 0xeef1f4;
    private static final float NOTE_SCALE = 0.8f;

    private static final float MIN_LINE = 36;

    private final Measurer measurer;
    private final PageSpec spec;
    private final Document doc;
    /// The page of the section being sliced or paged.
    private PageSpec cur;
    private final List<LayoutPage> pages = new ArrayList<LayoutPage>();
    private final int[] listCounters = new int[9];
    private int noteCounter;
    private int fieldDigits = 1;
    private boolean hasFields;

    /// Lays a document out.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document
    ///
    /// - `measurer`: measures text; use the same one for everything that must match
    public DocumentLayout(Document doc, Measurer measurer) {
        if (doc == null || measurer == null) {
            throw new IllegalArgumentException("Document and measurer are required");
        }
        this.measurer = measurer;
        this.doc = doc;
        this.spec = doc.page == null ? PageSpec.A4 : doc.page;
        this.cur = spec;
        run(doc);
        if (hasFields && digits(pages.size()) > fieldDigits) {
            // page numbers grew wider than the room the fields were given: lay out again
            fieldDigits = digits(pages.size());
            run(doc);
        }
    }

    private static int digits(int n) {
        return String.valueOf(n).length();
    }

    private void run(Document doc) {
        pages.clear();
        for (int i = 0; i < listCounters.length; i++) {
            listCounters[i] = 0;
        }
        noteCounter = 0;
        List<Note> endnotes = new ArrayList<Note>();
        List<Slice> slices = new ArrayList<Slice>();
        cur = spec;
        List<Block> section = new ArrayList<Block>();
        for (Block b : doc.blocks) {
            if (b instanceof SectionBreak) {
                slices.addAll(blocks(section, new Flow(bodyWidth()), endnotes));
                section.clear();
                SectionBreak br = (SectionBreak) b;
                Slice mark = new Slice(0);
                mark.section = br.page;
                mark.sectionNewPage = br.newPage;
                slices.add(mark);
                cur = br.page;
            } else {
                section.add(b);
            }
        }
        slices.addAll(blocks(section, new Flow(bodyWidth()), endnotes));
        if (!endnotes.isEmpty()) {
            Slice rule = new Slice(12);
            rule.items.add(LayoutItem.line(0, 6, bodyWidth() / 3, 0, BORDER));
            slices.add(rule);
            for (Note n : endnotes) {
                slices.addAll(noteSlices(n, bodyWidth()));
            }
        }
        paginate(slices);
    }

    /// The page geometry used.
    ///
    /// #### Returns
    ///
    /// the document's page, or A4 when it has none
    public PageSpec getPageSpec() {
        return spec;
    }

    /// The number of pages; at least one.
    ///
    /// #### Returns
    ///
    /// the page count
    public int getPageCount() {
        return pages.size();
    }

    /// A page.
    ///
    /// #### Parameters
    ///
    /// - `index`: from zero
    ///
    /// #### Returns
    ///
    /// the page
    public LayoutPage getPage(int index) {
        return pages.get(index);
    }

    /// The measurer this layout was made with.
    ///
    /// #### Returns
    ///
    /// the measurer
    public Measurer getMeasurer() {
        return measurer;
    }

    /// The width of a column of the section being laid out.
    private float bodyWidth() {
        return cur.columnWidth();
    }

    private float bodyHeight() {
        return cur.contentHeight();
    }

    // ------------------------------------------------------------------ slices

    /// An unbreakable horizontal band: one line, one table row or one picture, with its items
    /// relative to the band's top left.
    static final class Slice {
        float height;
        final List<LayoutItem> items = new ArrayList<LayoutItem>();
        final List<Note> notes = new ArrayList<Note>();
        boolean keepNext;
        boolean breakBefore;
        Table table;
        boolean header;
        /// How far floating pictures reach below the slice's top; at least `height` is needed.
        float extent;
        /// The page of a section that starts here, on a marker slice of no height.
        PageSpec section;
        boolean sectionNewPage;

        Slice(float height) {
            this.height = height;
        }

        void shift(float dx, float dy) {
            for (int i = 0; i < items.size(); i++) {
                items.set(i, items.get(i).moved(dx, dy));
            }
            extent += dy;
        }

        float need() {
            return Math.max(height, extent);
        }
    }

    /// One column-wide stream being sliced: how far down it has got and the floating pictures
    /// its text wraps around, each `{left, top, right, bottom}` in the stream's coordinates.
    static final class Flow {
        final float width;
        float y;
        final List<float[]> floats = new ArrayList<float[]>();

        Flow(float width) {
            this.width = width;
        }

        /// The free stretch `{left, right}` of a band, between `lo` and `hi`. A float takes the
        /// side of the band it sits nearer to, so text runs down the wider side.
        float[] free(float top, float h, float lo, float hi) {
            float l = lo;
            float r = hi;
            for (float[] f : floats) {
                if (f[3] <= top + 0.01f || f[1] >= top + h - 0.01f || f[2] <= l || f[0] >= r) {
                    continue;
                }
                if (f[0] - l <= r - f[2]) {
                    l = Math.max(l, f[2]);
                } else {
                    r = Math.min(r, f[0]);
                }
            }
            return new float[] {l, Math.max(l, r)};
        }

        /// The nearest float bottom below `top` among the floats that cross the band, or
        /// `top` when none does.
        float nextClear(float top, float h) {
            float best = Float.MAX_VALUE;
            for (float[] f : floats) {
                if (f[3] > top + 0.01f && f[1] < top + h - 0.01f) {
                    best = Math.min(best, f[3]);
                }
            }
            return best == Float.MAX_VALUE ? top : best;
        }

        /// The bottom of the lowest float, or `y` when that is higher.
        float clearAll() {
            float b = y;
            for (float[] f : floats) {
                b = Math.max(b, f[3]);
            }
            return b;
        }
    }

    private List<Slice> blocks(List<Block> list, float width, List<Note> endnotes) {
        return blocks(list, new Flow(width), endnotes);
    }

    private List<Slice> blocks(List<Block> list, Flow flow, List<Note> endnotes) {
        List<Slice> out = new ArrayList<Slice>();
        float width = flow.width;
        for (Block b : list) {
            if (b instanceof Paragraph) {
                out.addAll(paragraph((Paragraph) b, flow, endnotes, 1));
                continue;
            }
            List<Slice> made = new ArrayList<Slice>();
            if (b instanceof Table) {
                // a table is not wrapped: it starts below every float
                float clear = flow.clearAll();
                if (clear > flow.y + 0.01f) {
                    made.add(new Slice(clear - flow.y));
                }
                made.addAll(table((Table) b, width, endnotes));
            } else if (b instanceof Picture) {
                Picture p = (Picture) b;
                if (p.isFloating()) {
                    Slice s = new Slice(0);
                    addFloat(flow, p, flow.y, s, flow.y, p.id, 0);
                    made.add(s);
                } else {
                    float[] size = fit(p.width, p.height, width, bodyHeight());
                    Slice s = new Slice(size[1] + 6);
                    s.items.add(new LayoutItem(LayoutItem.PICTURE, 0, 3, size[0], size[1], 0,
                            null, null, p.id, 0, p, null, 0, null));
                    made.add(s);
                }
            }
            for (Slice s : made) {
                flow.y += s.height;
                out.add(s);
            }
        }
        return out;
    }

    /// Places a floating picture in a slice and, unless it lies behind or over the text, keeps
    /// its box out of the lines that follow.
    private void addFloat(Flow flow, Picture pic, float top, Slice slice, float sliceTop,
            String paragraphId, int start) {
        float[] size = fit(pic.width, pic.height, flow.width, bodyHeight() * 0.9f);
        float w = size[0];
        float h = size[1];
        float x = Picture.CENTER.equals(pic.align) ? (flow.width - w) / 2
                : Picture.RIGHT.equals(pic.align) ? flow.width - w : 0;
        float y = top + Math.max(0, pic.offsetY);
        slice.items.add(new LayoutItem(LayoutItem.PICTURE, x, y - sliceTop, w, h, 0, null, null,
                paragraphId, start, pic, null, 0, null));
        slice.extent = Math.max(slice.extent, y - sliceTop + h);
        float d = pic.distance;
        if (Picture.SQUARE.equals(pic.wrap)) {
            flow.floats.add(new float[] {x - d, y, x + w + d, y + h + d});
        } else if (Picture.TOP_BOTTOM.equals(pic.wrap)) {
            flow.floats.add(new float[] {-1, y - d, flow.width + 1, y + h + d});
        }
    }

    private static float[] fit(float w, float h, float maxW, float maxH) {
        float scale = Math.min(1, Math.min(maxW / w, maxH / h));
        return new float[] {Math.max(1, w * scale), Math.max(1, h * scale)};
    }

    /// One unit of a line: a word with its trailing spaces, a tab, a line break or a box.
    static final class Piece {
        static final int WORD = 0;
        static final int TAB = 1;
        static final int BREAK = 2;
        static final int BOX = 3;

        int kind;
        String text = "";
        TextStyle style;
        int start;
        float width;
        float spaceWidth;
        float ascent;
        float descent;
        Picture picture;
        EmbeddedComponent component;
        String field;
        Note note;
        char leader;
    }

    /// A picture that floats out of a paragraph, with the offset of its character.
    static final class Anchored {
        final Picture picture;
        final int start;

        Anchored(Picture picture, int start) {
            this.picture = picture;
            this.start = start;
        }
    }

    private String marker(Paragraph p) {
        if (Paragraph.NO_LIST.equals(p.list)) {
            return null;
        }
        int level = Math.max(0, Math.min(8, p.listLevel));
        if (Paragraph.BULLET.equals(p.list)) {
            return level % 3 == 0 ? "\u2022" : level % 3 == 1 ? "o" : "-";
        }
        if (p.listStart > 0) {
            listCounters[level] = p.listStart;
        } else {
            listCounters[level]++;
        }
        for (int i = level + 1; i < listCounters.length; i++) {
            listCounters[i] = 0;
        }
        int n = listCounters[level];
        switch (level % 3) {
            case 1:
                return alpha(n) + ".";
            case 2:
                return roman(n) + ".";
            default:
                return n + ".";
        }
    }

    private static String alpha(int n) {
        StringBuilder b = new StringBuilder();
        int v = Math.max(1, n);
        while (v > 0) {
            v--;
            b.insert(0, (char) ('a' + v % 26));
            v /= 26;
        }
        return b.toString();
    }

    private static String roman(int n) {
        int v = Math.max(1, Math.min(3999, n));
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] digits = {"m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i"};
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (v >= values[i]) {
                b.append(digits[i]);
                v -= values[i];
            }
        }
        return b.toString();
    }

    private TextStyle scaled(TextStyle s, float scale) {
        return scale == 1 ? s : s.withSize(Math.max(1, s.size * scale));
    }

    /// A run's style under its paragraph's named style. A heading's runs that carry no size or
    /// weight of their own take the heading's; anything set directly on a run wins.
    static TextStyle styled(int heading, TextStyle s) {
        if (heading <= 0) {
            return s;
        }
        TextStyle out = s;
        if (Float.floatToIntBits(s.size) == Float.floatToIntBits(TextStyle.DEFAULT.size)) {
            out = out.withSize(HtmlFormat.headingSize(heading));
        }
        return out.bold ? out : out.withBold(true);
    }

    private List<Piece> pieces(RichText text, float avail, float scale, int heading,
            List<Anchored> floats) {
        List<Piece> out = new ArrayList<Piece>();
        for (int r = 0, at = 0; r < text.runCount(); r++) {
            String s = text.runText(r);
            TextStyle st = scaled(styled(heading, text.runStyle(r)), scale);
            Inline obj = text.runObject(r);
            if (obj instanceof Picture && ((Picture) obj).isFloating()) {
                floats.add(new Anchored((Picture) obj, at));
                at++;
                continue;
            }
            if (obj != null) {
                out.add(objectPiece(obj, st, at, avail, scale));
                at++;
                continue;
            }
            int i = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                Piece p = new Piece();
                p.style = st;
                p.start = at + i;
                p.ascent = measurer.ascent(st);
                p.descent = measurer.descent(st);
                if (c == '\n') {
                    p.kind = Piece.BREAK;
                    p.text = "";
                    i++;
                } else if (c == '\t') {
                    p.kind = Piece.TAB;
                    p.text = "\t";
                    i++;
                } else {
                    int end = i;
                    while (end < s.length() && s.charAt(end) != ' ' && s.charAt(end) != '\t'
                            && s.charAt(end) != '\n') {
                        end++;
                    }
                    int spaces = end;
                    while (spaces < s.length() && s.charAt(spaces) == ' ') {
                        spaces++;
                    }
                    p.kind = Piece.WORD;
                    p.text = s.substring(i, spaces);
                    float word = measurer.width(s.substring(i, end), st);
                    p.width = measurer.width(p.text, st);
                    p.spaceWidth = p.width - word;
                    i = spaces;
                }
                out.add(p);
            }
            at += s.length();
        }
        return out;
    }

    private Piece objectPiece(Inline obj, TextStyle st, int at, float avail, float scale) {
        Piece p = new Piece();
        p.style = st;
        p.start = at;
        p.ascent = measurer.ascent(st);
        p.descent = measurer.descent(st);
        p.kind = Piece.WORD;
        if (obj instanceof Picture || obj instanceof EmbeddedComponent) {
            float w;
            float h;
            if (obj instanceof Picture) {
                p.picture = (Picture) obj;
                w = p.picture.width;
                h = p.picture.height;
            } else {
                p.component = (EmbeddedComponent) obj;
                w = p.component.width;
                h = p.component.height;
            }
            float[] size = fit(w * scale, h * scale, Math.max(1, avail), bodyHeight() * 0.9f);
            p.kind = Piece.BOX;
            p.width = size[0];
            p.ascent = size[1];
            p.descent = 0;
            return p;
        }
        if (obj instanceof Note) {
            Note n = (Note) obj;
            p.note = n;
            p.style = st.withScript(TextStyle.SUPERSCRIPT);
            p.text = n.mark != null && n.mark.length() > 0 ? n.mark : String.valueOf(++noteCounter);
        } else if (obj instanceof Field) {
            Field f = (Field) obj;
            String kind = fieldKind(f.instruction);
            p.field = kind;
            if (kind != null) {
                hasFields = true;
                StringBuilder room = new StringBuilder();
                for (int d = 0; d < fieldDigits; d++) {
                    room.append('0');
                }
                p.text = room.toString();
            } else {
                p.text = f.result;
            }
        } else {
            p.text = obj.displayText();
        }
        p.width = measurer.width(p.text, p.style);
        return p;
    }

    private static String fieldKind(String instruction) {
        String s = Ids.asciiLower(instruction.trim());
        if ("page".equals(s) || s.startsWith("page ")) {
            return "PAGE";
        }
        if ("numpages".equals(s) || s.startsWith("numpages ")) {
            return "NUMPAGES";
        }
        return null;
    }

    /// A paragraph's lines, each a slice. The first carries the space before and the last the
    /// space after. Each line takes the stretch of the column the floats leave free at its
    /// height, and moves down past them when that stretch is too narrow to hold text.
    private List<Slice> paragraph(Paragraph p, Flow flow, List<Note> endnotes, float scale) {
        float width = flow.width;
        String marker = marker(p);
        float left = Math.min(width * 0.8f, Math.max(0, p.indent * scale));
        if (marker != null) {
            left = Math.min(width * 0.8f, left + LIST_INDENT * (Math.max(0, p.listLevel) + 1));
        }
        float right = Math.max(0, Math.min(width * 0.5f, p.rightIndent * scale));
        if (left + right > width - 10) {
            right = Math.max(0, width - 10 - left);
        }
        List<Anchored> floats = new ArrayList<Anchored>();
        List<Piece> pieces = pieces(p.text, Math.max(10, width - left - right), scale,
                p.headingLevel(), floats);
        List<Slice> out = new ArrayList<Slice>();
        TextStyle base = scaled(styled(p.headingLevel(), p.text.runCount() > 0
                ? p.text.runStyle(0) : TextStyle.DEFAULT), scale);
        float paraTop = flow.y;
        // spacing never exceeds a page: Word caps it there too, and more would only add pages
        // of nothing
        float before = Math.min(p.before * scale, bodyHeight());
        Slice floatHolder = new Slice(0);
        for (Anchored a : floats) {
            addFloat(flow, a.picture, paraTop + before, floatHolder, paraTop, p.id, a.start);
        }
        float lineGuess = (measurer.ascent(base) + measurer.descent(base))
                * Math.max(0.5f, p.lineSpacing);
        float y = paraTop + before;
        int i = 0;
        boolean first = true;
        float carry = 0;
        do {
            float lineLeft = left;
            if (first) {
                lineLeft = Math.max(0, Math.min(width * 0.8f, left + p.firstLine * scale));
            }
            float hi = width - right;
            float[] fr = flow.free(y, lineGuess, lineLeft, hi);
            float gap = carry;
            carry = 0;
            for (int guard = 0; fr[1] - fr[0] < Math.min(MIN_LINE, hi - lineLeft) && guard < 64;
                    guard++) {
                float next = flow.nextClear(y, lineGuess);
                if (next <= y + 0.01f) {
                    break;
                }
                gap += next - y;
                y = next;
                fr = flow.free(y, lineGuess, lineLeft, hi);
            }
            float x0 = fr[0];
            float avail = Math.max(10, fr[1] - fr[0]);
            List<Piece> line = new ArrayList<Piece>();
            float x = 0;
            boolean hard = false;
            boolean narrowed = fr[1] - fr[0] < hi - lineLeft - 0.01f;
            boolean pushDown = false;
            while (i < pieces.size()) {
                Piece q = pieces.get(i);
                if (q.kind == Piece.BREAK) {
                    line.add(q);
                    i++;
                    hard = true;
                    break;
                }
                if (q.kind == Piece.TAB) {
                    q.width = tabWidth(p, pieces, i, x0 + x, left, scale);
                }
                float w = q.width;
                if (x + w - q.spaceWidth > avail + 0.01f && !line.isEmpty()) {
                    break;
                }
                if (x + w - q.spaceWidth > avail + 0.01f && narrowed && line.isEmpty()) {
                    // too wide for the room beside a float: go below it instead of breaking
                    pushDown = true;
                    break;
                }
                if (x + w - q.spaceWidth > avail + 0.01f && q.kind == Piece.WORD
                        && q.text.length() > 1) {
                    // a word wider than the line: break it between letters
                    Piece head = splitWord(q, avail - x);
                    line.add(head);
                    break;
                }
                line.add(q);
                x += w;
                i++;
            }
            if (pushDown) {
                float next = flow.nextClear(y, lineGuess);
                if (next > y + 0.01f) {
                    carry = gap + next - y;
                    y = next;
                    continue;
                }
                // nothing to go below: break the word after all
                Piece head = splitWord(pieces.get(i), avail);
                line.add(head);
            }
            boolean last = i >= pieces.size();
            Slice s = line(p, line, marker != null && first ? marker : null, left, x0, avail,
                    base, hard || last, endnotes);
            float pre = gap + (first ? before : 0);
            if (pre > 0) {
                s.height += pre;
                s.shift(0, pre);
                s.extent = 0;
            }
            y += s.height - pre;
            out.add(s);
            first = false;
        } while (i < pieces.size());
        Slice firstSlice = out.get(0);
        for (LayoutItem it : floatHolder.items) {
            firstSlice.items.add(it);
        }
        firstSlice.extent = Math.max(firstSlice.extent, floatHolder.extent);
        firstSlice.breakBefore = p.pageBreak;
        Slice lastSlice = out.get(out.size() - 1);
        lastSlice.height += Math.min(p.after * scale, bodyHeight());
        lastSlice.keepNext = p.keepNext;
        float total = 0;
        for (Slice s : out) {
            total += s.height;
        }
        flow.y = paraTop + total;
        return out;
    }

    /// The width a tab takes: up to the next stop past `at`, less the text the stop aligns.
    private float tabWidth(Paragraph p, List<Piece> pieces, int index, float at, float left,
            float scale) {
        Piece tab = pieces.get(index);
        tab.leader = 0;
        TabStop stop = null;
        for (TabStop t : p.tabs) {
            if (left + t.position * scale > at + 0.5f) {
                stop = t;
                break;
            }
        }
        if (stop == null) {
            float rel = at - left;
            float next = left + ((int) Math.floor(rel / TAB) + 1) * TAB;
            return Math.max(1, next - at);
        }
        tab.leader = stop.leader;
        float target = left + stop.position * scale - at;
        if (TabStop.LEFT.equals(stop.alignment)) {
            return Math.max(1, target);
        }
        float seg = 0;
        boolean decimal = TabStop.DECIMAL.equals(stop.alignment);
        for (int k = index + 1; k < pieces.size(); k++) {
            Piece q = pieces.get(k);
            if (q.kind == Piece.TAB || q.kind == Piece.BREAK) {
                break;
            }
            int dot = decimal ? q.text.indexOf('.') : -1;
            if (dot >= 0) {
                seg += measurer.width(q.text.substring(0, dot), q.style);
                break;
            }
            boolean lastOfSegment = k + 1 >= pieces.size() || pieces.get(k + 1).kind != Piece.WORD;
            seg += lastOfSegment ? q.width - q.spaceWidth : q.width;
        }
        float w = TabStop.CENTER.equals(stop.alignment) ? target - seg / 2 : target - seg;
        return Math.max(1, w);
    }

    private Piece splitWord(Piece q, float room) {
        int n = 1;
        while (n < q.text.length() - 1
                && measurer.width(q.text.substring(0, n + 1), q.style) <= room) {
            n++;
        }
        Piece head = new Piece();
        head.kind = Piece.WORD;
        head.style = q.style;
        head.start = q.start;
        head.ascent = q.ascent;
        head.descent = q.descent;
        head.text = q.text.substring(0, n);
        head.width = measurer.width(head.text, q.style);
        q.text = q.text.substring(n);
        q.start += n;
        float word = measurer.width(q.text.trim(), q.style);
        q.width = measurer.width(q.text, q.style);
        q.spaceWidth = q.width - word;
        return head;
    }

    private Slice line(Paragraph p, List<Piece> line, String marker, float left, float x0,
            float avail, TextStyle base, boolean lastLine, List<Note> endnotes) {
        float ascent = measurer.ascent(base);
        float descent = measurer.descent(base);
        float used = 0;
        int gaps = 0;
        for (int k = 0; k < line.size(); k++) {
            Piece q = line.get(k);
            if (q.kind == Piece.BREAK) {
                continue;
            }
            ascent = Math.max(ascent, q.ascent);
            descent = Math.max(descent, q.descent);
            used += q.width;
            if (k < line.size() - 1 && q.spaceWidth > 0) {
                gaps++;
            }
        }
        Piece tail = line.isEmpty() ? null : line.get(line.size() - 1);
        if (tail != null && tail.kind == Piece.BREAK && line.size() > 1) {
            tail = line.get(line.size() - 2);
        }
        if (tail != null && tail.kind == Piece.WORD) {
            used -= tail.spaceWidth;
        }
        float extra = Math.max(0, avail - used);
        float x = x0;
        float gapExtra = 0;
        if (Paragraph.CENTER.equals(p.align)) {
            x += extra / 2;
        } else if (Paragraph.RIGHT.equals(p.align)) {
            x += extra;
        } else if (Paragraph.JUSTIFY.equals(p.align) && !lastLine && gaps > 0) {
            gapExtra = extra / gaps;
        }
        float natural = ascent + descent;
        float height = natural * Math.max(0.5f, p.lineSpacing);
        float baseline = ascent;
        Slice s = new Slice(height);
        if (marker != null) {
            TextStyle ms = base.withScript(TextStyle.BASELINE).withLink(null).withRevision(null)
                    .withUnderline(false).withStrike(false).withBackground(TextStyle.NO_BACKGROUND);
            float mw = measurer.width(marker, ms);
            s.items.add(new LayoutItem(LayoutItem.TEXT, Math.max(0, left - LIST_INDENT * 0.85f
                    - Math.max(0, mw - LIST_INDENT * 0.7f)), 0, mw, height, baseline, marker, ms,
                    null, 0, null, null, 0, null));
        }
        for (int k = 0; k < line.size(); k++) {
            Piece q = line.get(k);
            if (q.kind == Piece.BREAK) {
                s.items.add(new LayoutItem(LayoutItem.TEXT, x, 0, 0, height, baseline, "",
                        q.style, p.id, q.start, null, null, 0, null));
                continue;
            }
            float w = q.width + (q.spaceWidth > 0 && k < line.size() - 1 ? gapExtra : 0);
            if (q.kind == Piece.TAB && q.leader != 0) {
                s.items.add(leader(q, x, w, height, baseline));
            }
            if (q.kind == Piece.BOX) {
                float top = baseline - q.ascent;
                if (q.picture != null) {
                    s.items.add(new LayoutItem(LayoutItem.PICTURE, x, top, q.width, q.ascent,
                            baseline, null, q.style, p.id, q.start, q.picture, null, 0, null));
                } else {
                    s.items.add(new LayoutItem(LayoutItem.COMPONENT, x, top, q.width, q.ascent,
                            baseline, null, q.style, p.id, q.start, null, q.component, 0, null));
                }
            } else {
                if (q.style.background != TextStyle.NO_BACKGROUND && q.kind == Piece.WORD) {
                    s.items.add(LayoutItem.fill(x, baseline - q.ascent, w, q.ascent + q.descent,
                            q.style.background));
                }
                float shift = TextStyle.SUPERSCRIPT.equals(q.style.script) ? -q.style.size * 0.35f
                        : TextStyle.SUBSCRIPT.equals(q.style.script) ? q.style.size * 0.15f : 0;
                s.items.add(new LayoutItem(LayoutItem.TEXT, x, 0, w, height, baseline + shift,
                        q.kind == Piece.TAB ? "" : q.text, q.style, p.id, q.start, null, null, 0,
                        q.field));
            }
            if (q.note != null) {
                if (Note.ENDNOTE.equals(q.note.kind)) {
                    endnotes.add(markedNote(q.note, q.text));
                } else {
                    s.notes.add(markedNote(q.note, q.text));
                }
            }
            x += w;
        }
        return s;
    }

    /// The dots, dashes or rule that fill a tab with a leader. The item belongs to no paragraph,
    /// so the caret and the hit testing never land on it.
    private LayoutItem leader(Piece q, float x, float w, float height, float baseline) {
        TextStyle ls = q.style.withUnderline(false).withStrike(false).withLink(null)
                .withRevision(null).withBackground(TextStyle.NO_BACKGROUND);
        float one = Math.max(0.5f, measurer.width(String.valueOf(q.leader), ls));
        int n = (int) Math.floor((w - one) / one);
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < n && k < 2000; k++) {
            b.append(q.leader);
        }
        String t = b.toString();
        float tw = measurer.width(t, ls);
        return new LayoutItem(LayoutItem.TEXT, x + w - tw - one * 0.5f, 0, tw, height, baseline,
                t, ls, null, 0, null, null, 0, null);
    }

    /// A note whose first paragraph starts with its reference mark.
    private static Note markedNote(Note n, String mark) {
        List<Block> blocks = new ArrayList<Block>(n.blocks);
        TextStyle sup = TextStyle.DEFAULT.withScript(TextStyle.SUPERSCRIPT);
        if (!blocks.isEmpty() && blocks.get(0) instanceof Paragraph) {
            Paragraph p = (Paragraph) blocks.get(0);
            TextStyle s = p.text.runCount() > 0 ? p.text.runStyle(0) : TextStyle.DEFAULT;
            sup = s.withScript(TextStyle.SUPERSCRIPT);
            RichText t = new RichText.Builder().append(mark, sup).append(" ", s).append(p.text)
                    .build();
            blocks.set(0, p.withText(t));
        } else {
            blocks.add(0, new Paragraph(n.id + "-mark", new RichText.Builder()
                    .append(mark, sup).build()));
        }
        return new Note(n.id, n.kind, mark, blocks);
    }

    private List<Slice> noteSlices(Note n, float width) {
        List<Slice> out = new ArrayList<Slice>();
        for (Block b : n.blocks) {
            if (b instanceof Paragraph) {
                out.addAll(paragraph((Paragraph) b, new Flow(width), new ArrayList<Note>(),
                        NOTE_SCALE));
            } else {
                List<Block> one = new ArrayList<Block>();
                one.add(b);
                out.addAll(blocks(one, width, new ArrayList<Note>()));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ tables

    private List<Slice> table(Table t, float width, List<Note> endnotes) {
        int rows = t.rows();
        int cols = Math.max(1, t.columns());
        float[] colX = columnEdges(t, cols, width);
        float[] rowH = new float[rows];
        List<List<Slice>> content = new ArrayList<List<Slice>>();
        List<int[]> anchors = new ArrayList<int[]>();
        for (int r = 0; r < rows; r++) {
            rowH[r] = 0;
            for (int c = 0; c < cols; c++) {
                if (t.covered(r, c)) {
                    continue;
                }
                Table.Merge m = t.span(r, c);
                int spanR = m == null ? 1 : m.rows;
                int spanC = m == null ? 1 : m.columns;
                List<Slice> cell = blocks(t.cell(r, c), Math.max(4, colX[Math.min(cols, c + spanC)] - colX[c] - 2 * CELL_PAD),
                        endnotes);
                float h = 2 * CELL_PAD;
                for (Slice s : cell) {
                    h += s.height;
                }
                content.add(cell);
                anchors.add(new int[] {r, c, spanR, spanC, Math.round(h * 100)});
                if (spanR == 1) {
                    rowH[r] = Math.max(rowH[r], h);
                }
            }
            if (rowH[r] == 0) {
                rowH[r] = 2 * CELL_PAD + measurer.ascent(TextStyle.DEFAULT)
                        + measurer.descent(TextStyle.DEFAULT);
            }
        }
        for (int[] a : anchors) {
            if (a[2] > 1) {
                float h = a[4] / 100f;
                float sum = 0;
                for (int r = a[0]; r < Math.min(rows, a[0] + a[2]); r++) {
                    sum += rowH[r];
                }
                if (h > sum) {
                    rowH[Math.min(rows, a[0] + a[2]) - 1] += h - sum;
                }
            }
        }
        List<Slice> out = new ArrayList<Slice>();
        for (int r = 0; r < rows; r++) {
            Slice s = new Slice(rowH[r]);
            s.table = t;
            s.header = r < t.headerRows;
            out.add(s);
        }
        for (int k = 0; k < anchors.size(); k++) {
            int[] a = anchors.get(k);
            Slice s = out.get(a[0]);
            float h = 0;
            for (int r = a[0]; r < Math.min(rows, a[0] + a[2]); r++) {
                h += rowH[r];
            }
            for (int r = a[0]; r < Math.min(rows, a[0] + a[2]) - 1; r++) {
                out.get(r).keepNext = true;
            }
            float x = colX[a[1]];
            float w = colX[Math.min(cols, a[1] + a[3])] - x;
            if (s.header) {
                s.items.add(LayoutItem.fill(x, 0, w, h, HEADER_FILL));
            }
            float y = CELL_PAD;
            for (Slice c : content.get(k)) {
                for (LayoutItem item : c.items) {
                    s.items.add(item.moved(x + CELL_PAD, y));
                }
                s.notes.addAll(c.notes);
                y += c.height;
            }
            s.items.add(LayoutItem.line(x, 0, w, 0, BORDER));
            s.items.add(LayoutItem.line(x, h, w, 0, BORDER));
            s.items.add(LayoutItem.line(x, 0, 0, h, BORDER));
            s.items.add(LayoutItem.line(x + w, 0, 0, h, BORDER));
        }
        if (!out.isEmpty()) {
            Slice gap = out.get(out.size() - 1);
            gap.height += 6;
        }
        return out;
    }

    /// Where each column of a table starts, plus where the last one ends. Columns without
    /// widths share the space equally; with widths, a table that fits keeps them and one that
    /// does not is narrowed in proportion, as Word fits a table to its column.
    static float[] columnEdges(Table t, int cols, float width) {
        float[] x = new float[cols + 1];
        List<Float> w = t.columnWidths();
        float total = 0;
        for (Float f : w) {
            total += f.floatValue();
        }
        float scale = w.size() == cols && total > width ? width / total : 1;
        for (int c = 0; c < cols; c++) {
            x[c + 1] = x[c] + (w.size() == cols ? w.get(c).floatValue() * scale : width / cols);
        }
        return x;
    }

    /// Splits a band taller than a page into page-high pieces. No text line is cut: a cut that
    /// would cross one moves up to its top, and lines and fills are clipped to each piece.
    private List<Slice> split(Slice s, float max) {
        List<Slice> out = new ArrayList<Slice>();
        float from = 0;
        while (from < s.height - 0.01f) {
            float cut = Math.min(s.height, from + max);
            if (cut < s.height) {
                for (LayoutItem item : s.items) {
                    boolean whole = item.kind == LayoutItem.TEXT || item.kind == LayoutItem.PICTURE
                            || item.kind == LayoutItem.COMPONENT;
                    if (whole && item.y < cut && item.y + item.height > cut && item.y > from) {
                        cut = item.y;
                    }
                }
                // a line taller than the page itself, starting the piece, cannot move up: it
                // takes one overfull piece, clipped at the page edge as Word clips it, instead
                // of leaving a blank page for every page-height of it
                for (LayoutItem item : s.items) {
                    boolean whole = item.kind == LayoutItem.TEXT || item.kind == LayoutItem.PICTURE
                            || item.kind == LayoutItem.COMPONENT;
                    if (whole && item.y >= from - 0.01f && item.y <= from + 0.01f
                            && item.y + item.height > cut) {
                        cut = Math.min(s.height, Math.max(cut, item.y + item.height));
                    }
                }
            }
            Slice part = new Slice(cut - from);
            part.table = s.table;
            for (LayoutItem item : s.items) {
                float top = item.y;
                float bottom = item.y + Math.max(0, item.height);
                if (item.kind == LayoutItem.LINE || item.kind == LayoutItem.FILL) {
                    float a = Math.max(top, from);
                    float b = Math.min(bottom, cut);
                    if (b < a || item.height == 0 && (top < from || top > cut)) {
                        continue;
                    }
                    part.items.add(item.withBox(item.x, a - from, item.width,
                            item.height == 0 ? 0 : b - a));
                } else if (top >= from && (top < cut || top == from)) {
                    part.items.add(item.moved(0, -from));
                }
            }
            if (from == 0) {
                part.notes.addAll(s.notes);
                part.breakBefore = s.breakBefore;
            }
            // nothing but the space after an overfull line: no page of its own
            if (from == 0 || !part.items.isEmpty()) {
                out.add(part);
            }
            from = cut;
        }
        out.get(out.size() - 1).keepNext = s.keepNext;
        return out;
    }

    // ------------------------------------------------------------------ pages

    /// A page being filled: its frame (the body between header and footer), its columns and
    /// what each column holds so far.
    private static final class PageBuilder {
        final List<LayoutItem> items = new ArrayList<LayoutItem>();
        final List<LayoutItem> behind = new ArrayList<LayoutItem>();
        final PageSpec page;
        final int number;
        final float top;
        final float bottom;
        final List<Slice> head;
        final List<Slice> foot;
        PageSpec section;
        int column;
        float[] y;
        final List<List<Slice>> notes = new ArrayList<List<Slice>>();
        float[] notesHeight;
        boolean[] columnEmpty;
        boolean empty = true;
        float sectionTop;
        int sectionItems;
        final List<Slice> sectionSlices = new ArrayList<Slice>();
        boolean sectionHasNotes;

        PageBuilder(DocumentLayout owner, PageSpec page, int number) {
            this.page = page;
            this.number = number;
            boolean first = number == 1;
            Document doc = owner.doc;
            head = owner.furniture(first && doc.firstHeader != null ? doc.firstHeader
                    : doc.header, page);
            foot = owner.furniture(first && doc.firstFooter != null ? doc.firstFooter
                    : doc.footer, page);
            float edge = Math.min(36, page.top * 0.5f);
            float t = page.top;
            if (!head.isEmpty()) {
                t = Math.max(t, edge + height(head) + 6);
            }
            float footEdge = Math.min(36, page.bottom * 0.5f);
            float b = page.height - page.bottom;
            if (!foot.isEmpty()) {
                b = Math.min(b, page.height - footEdge - height(foot) - 6);
            }
            // a header or footer may not take more than half the page from the body
            float minBody = page.contentHeight() * 0.5f;
            if (b - t < minBody) {
                t = page.top;
                b = page.height - page.bottom;
            }
            top = t;
            bottom = b;
            startSection(page, 0);
        }

        void startSection(PageSpec s, float at) {
            section = s;
            column = 0;
            y = new float[s.columns];
            notesHeight = new float[s.columns];
            columnEmpty = new boolean[s.columns];
            notes.clear();
            for (int c = 0; c < s.columns; c++) {
                y[c] = at;
                // only a column at the top of the frame must take what it is given
                columnEmpty[c] = at <= 0.01f;
                notes.add(new ArrayList<Slice>());
            }
            sectionTop = at;
            sectionItems = items.size();
            sectionSlices.clear();
            sectionHasNotes = false;
        }

        float columnX(int c) {
            return section.left + c * (section.columnWidth() + section.columnGap);
        }

        float frame() {
            return bottom - top;
        }

        float lowest() {
            float m = sectionTop;
            for (float v : y) {
                m = Math.max(m, v);
            }
            return m;
        }
    }

    private List<Slice> furniture(List<Block> blocks, PageSpec page) {
        if (blocks.isEmpty()) {
            return new ArrayList<Slice>();
        }
        PageSpec saved = cur;
        cur = page.withColumns(1, page.columnGap);
        try {
            return blocks(blocks, page.contentWidth(), new ArrayList<Note>());
        } finally {
            cur = saved;
        }
    }

    private static float height(List<Slice> slices) {
        float h = 0;
        for (Slice s : slices) {
            h += s.height;
        }
        return h;
    }

    private static boolean sameSheet(PageSpec a, PageSpec b) {
        return Float.floatToIntBits(a.width) == Float.floatToIntBits(b.width)
                && Float.floatToIntBits(a.height) == Float.floatToIntBits(b.height)
                && Float.floatToIntBits(a.top) == Float.floatToIntBits(b.top)
                && Float.floatToIntBits(a.bottom) == Float.floatToIntBits(b.bottom)
                && Float.floatToIntBits(a.left) == Float.floatToIntBits(b.left)
                && Float.floatToIntBits(a.right) == Float.floatToIntBits(b.right);
    }

    private void paginate(List<Slice> all) {
        List<Slice> slices = new ArrayList<Slice>(all);
        PageSpec sheet = spec;
        PageBuilder page = new PageBuilder(this, sheet, 1);
        List<Slice> headers = new ArrayList<Slice>();
        Table headersOf = null;
        for (int i = 0; i < slices.size(); i++) {
            Slice s = slices.get(i);
            if (s.section != null) {
                balance(page);
                PageSpec next = s.section;
                if (!page.empty && (s.sectionNewPage || !sameSheet(next, page.page))) {
                    finish(page);
                    page = new PageBuilder(this, next, pages.size() + 1);
                } else if (page.empty && !sameSheet(next, page.page)) {
                    page = new PageBuilder(this, next, pages.size() + 1);
                } else {
                    page.startSection(next, page.lowest());
                }
                continue;
            }
            float colH = page.frame() - page.sectionTop;
            if (s.need() > page.frame() + 0.01f && s.height > 0) {
                List<Slice> parts = split(s, Math.max(10, page.frame()));
                slices.remove(i);
                slices.addAll(i, parts);
                s = slices.get(i);
            }
            if (s.table != headersOf) { //NOPMD CompareObjectsWithEquals - identity of the table being paged
                headers.clear();
                headersOf = s.table;
            }
            float noteW = page.section.columnWidth();
            List<Slice> noteParts = new ArrayList<Slice>();
            float noteH = 0;
            for (Note n : s.notes) {
                for (Slice ns : noteSlices(n, noteW)) {
                    noteParts.add(ns);
                    noteH += ns.height;
                }
            }
            float need = s.need();
            float chain = s.height;
            for (int k = i; k < slices.size() - 1 && slices.get(k).keepNext
                    && slices.get(k + 1).section == null; k++) {
                chain += slices.get(k + 1).need();
            }
            if (chain <= colH * 0.8f) {
                need = Math.max(need, chain);
            }
            int c = page.column;
            float separator = noteParts.isEmpty() || !page.notes.get(c).isEmpty() ? 0 : 10;
            float room = page.frame() - page.y[c] - page.notesHeight[c] - separator - noteH;
            if (s.breakBefore && !page.empty) {
                finish(page);
                page = new PageBuilder(this, page.section, pages.size() + 1);
            } else if (need > room + 0.01f && !page.columnEmpty[c]) {
                if (c + 1 < page.section.columns) {
                    page.column = c + 1;
                } else {
                    finish(page);
                    page = new PageBuilder(this, page.section, pages.size() + 1);
                }
                if (s.table != null && !s.header && !headers.isEmpty()) {
                    for (Slice h : headers) {
                        place(page, h);
                    }
                }
            }
            place(page, s);
            if (s.header && s.table != null) {
                headers.add(s);
            }
            if (!noteParts.isEmpty()) {
                int col = page.column;
                if (page.notes.get(col).isEmpty()) {
                    page.notesHeight[col] += 10;
                }
                page.notes.get(col).addAll(noteParts);
                page.notesHeight[col] += noteH;
                page.sectionHasNotes = true;
            }
        }
        finish(page);
        String count = String.valueOf(pages.size());
        for (int p = 0; p < pages.size(); p++) {
            LayoutPage lp = pages.get(p);
            List<LayoutItem> items = null;
            for (int k = 0; k < lp.getItems().size(); k++) {
                LayoutItem it = lp.getItems().get(k);
                if ("NUMPAGES".equals(it.field)) {
                    if (items == null) {
                        items = new ArrayList<LayoutItem>(lp.getItems());
                    }
                    items.set(k, it.withText(count));
                }
            }
            if (items != null) {
                pages.set(p, new LayoutPage(p, lp.getWidth(), lp.getHeight(), items));
            }
        }
    }

    /// Evens out the columns of the section that ends on this page, as a continuous section
    /// break does, so the text below starts under the shortest arrangement rather than under
    /// a full first column.
    private void balance(PageBuilder page) {
        int cols = page.section.columns;
        if (cols < 2 || page.sectionSlices.isEmpty() || page.sectionHasNotes) {
            return;
        }
        float total = 0;
        for (Slice s : page.sectionSlices) {
            total += s.height;
        }
        float target = total / cols;
        List<LayoutItem> placed = new ArrayList<LayoutItem>();
        float[] y = new float[cols];
        for (int c = 0; c < cols; c++) {
            y[c] = page.sectionTop;
        }
        int c = 0;
        boolean columnEmpty = true;
        for (Slice s : page.sectionSlices) {
            if (!columnEmpty && c < cols - 1 && y[c] - page.sectionTop + s.height > target + 0.5f) {
                c++;
                columnEmpty = true;
            }
            if (y[c] + s.need() > page.frame() + 0.01f) {
                return;
            }
            addItems(page, placed, s, page.columnX(c), page.top + y[c]);
            y[c] += s.height;
            columnEmpty = false;
        }
        while (page.items.size() > page.sectionItems) {
            page.items.remove(page.items.size() - 1);
        }
        page.items.addAll(placed);
        page.y = y;
        page.column = c;
    }

    private void place(PageBuilder page, Slice s) {
        int c = page.column;
        addItems(page, page.items, s, page.columnX(c), page.top + page.y[c]);
        page.y[c] += s.height;
        page.empty = false;
        page.columnEmpty[c] = false;
        page.sectionSlices.add(s);
    }

    private void addItems(PageBuilder page, List<LayoutItem> into, Slice s, float dx, float dy) {
        String number = String.valueOf(page.number);
        for (LayoutItem item : s.items) {
            LayoutItem moved = item.moved(dx, dy);
            if ("PAGE".equals(moved.field)) {
                moved = moved.withText(number);
            }
            if (moved.picture != null && Picture.BEHIND.equals(moved.picture.wrap)
                    && moved.kind == LayoutItem.PICTURE
                    && into == page.items) { //NOPMD CompareObjectsWithEquals - which list, not its contents
                page.behind.add(moved);
            } else {
                into.add(moved);
            }
        }
    }

    private void addFurniture(PageBuilder page, List<Slice> slices, float y) {
        String number = String.valueOf(page.number);
        for (Slice s : slices) {
            for (LayoutItem item : s.items) {
                LayoutItem moved = item.moved(page.page.left, y).detached();
                if ("PAGE".equals(moved.field)) {
                    moved = moved.withText(number);
                }
                page.items.add(moved);
            }
            y += s.height;
        }
    }

    private void finish(PageBuilder page) {
        for (int c = 0; c < page.notes.size(); c++) {
            List<Slice> list = page.notes.get(c);
            if (list.isEmpty()) {
                continue;
            }
            float x = page.columnX(c);
            float y = page.bottom - page.notesHeight[c];
            page.items.add(LayoutItem.line(x, y + 4, page.section.columnWidth() / 3, 0, BORDER));
            y += 10;
            String number = String.valueOf(page.number);
            for (Slice ns : list) {
                for (LayoutItem item : ns.items) {
                    LayoutItem moved = item.moved(x, y);
                    if ("PAGE".equals(moved.field)) {
                        moved = moved.withText(number);
                    }
                    page.items.add(moved);
                }
                y += ns.height;
            }
        }
        if (!page.head.isEmpty()) {
            addFurniture(page, page.head, Math.min(36, page.page.top * 0.5f));
        }
        if (!page.foot.isEmpty()) {
            addFurniture(page, page.foot, page.page.height - Math.min(36, page.page.bottom * 0.5f)
                    - height(page.foot));
        }
        List<LayoutItem> all = new ArrayList<LayoutItem>(page.behind);
        all.addAll(page.items);
        pages.add(new LayoutPage(pages.size(), page.page.width, page.page.height, all));
    }

    // ------------------------------------------------------------------ hit testing

    /// The caret position nearest a point on a page.
    ///
    /// #### Parameters
    ///
    /// - `page`: the page index
    ///
    /// - `x`: from the page's left edge, in points
    ///
    /// - `y`: from the page's top edge, in points
    ///
    /// #### Returns
    ///
    /// the position, or null when the page holds no text
    public TextPosition positionAt(int page, float x, float y) {
        if (page < 0 || page >= pages.size()) {
            return null;
        }
        LayoutItem bestLine = null;
        float bestDy = Float.MAX_VALUE;
        float bestLineDx = Float.MAX_VALUE;
        for (LayoutItem it : pages.get(page).getItems()) {
            if (it.paragraphId == null || it.kind == LayoutItem.FILL
                    || it.kind == LayoutItem.LINE) {
                continue;
            }
            float dy = y < it.y ? it.y - y : y > it.y + it.height ? y - it.y - it.height : 0;
            // the nearest line wins; with columns or text beside a float several lines share a
            // height, and then the horizontally nearest of them
            float dx = x < it.x ? it.x - x : x > it.x + it.width ? x - it.x - it.width : 0;
            if (dy < bestDy - 0.01f || Math.abs(dy - bestDy) <= 0.01f && dx < bestLineDx) {
                bestDy = dy;
                bestLineDx = dx;
                bestLine = it;
            }
        }
        if (bestLine == null) {
            return null;
        }
        LayoutItem best = null;
        float bestDx = Float.MAX_VALUE;
        for (LayoutItem it : pages.get(page).getItems()) {
            if (it.paragraphId == null || it.kind == LayoutItem.FILL
                    || it.kind == LayoutItem.LINE || Math.abs(it.y - bestLine.y) > 0.01f
                    || Math.abs(it.height - bestLine.height) > 0.01f) {
                continue;
            }
            float dx = x < it.x ? it.x - x : x > it.x + it.width ? x - it.x - it.width : 0;
            if (dx < bestDx || dx == bestDx && best != null && it.x > best.x && dx == 0) {
                bestDx = dx;
                best = it;
            }
        }
        if (best == null) {
            best = bestLine;
        }
        if (best.kind != LayoutItem.TEXT || best.text == null || best.text.length() == 0) {
            return new TextPosition(best.paragraphId, best.start
                    + (best.kind != LayoutItem.TEXT && x > best.x + best.width / 2 ? 1 : 0));
        }
        if (best.field != null) {
            return new TextPosition(best.paragraphId, best.start
                    + (x > best.x + best.width / 2 ? 1 : 0));
        }
        String t = best.text;
        float prev = 0;
        for (int i = 1; i <= t.length(); i++) {
            float w = measurer.width(t.substring(0, i), best.style);
            if (best.x + (prev + w) / 2 > x) {
                return new TextPosition(best.paragraphId, best.start + i - 1);
            }
            prev = w;
        }
        return new TextPosition(best.paragraphId, best.start + t.length());
    }

    /// Where the caret for a position is drawn.
    ///
    /// #### Parameters
    ///
    /// - `position`: the position
    ///
    /// #### Returns
    ///
    /// `{page, x, top, height}` in points, or null when the paragraph is not laid out
    public float[] caretRect(TextPosition position) {
        float[] fallback = null;
        for (int p = 0; p < pages.size(); p++) {
            for (LayoutItem it : pages.get(p).getItems()) {
                if (!position.paragraphId.equals(it.paragraphId)) {
                    continue;
                }
                int len = it.kind == LayoutItem.TEXT ? it.field != null ? 1
                        : it.text.length() : 1;
                if (position.offset >= it.start && position.offset < it.start + len) {
                    float dx = it.kind == LayoutItem.TEXT && it.field == null
                            ? measurer.width(it.text.substring(0, position.offset - it.start),
                            it.style) : 0;
                    return new float[] {p, it.x + dx, it.y, it.height};
                }
                if (position.offset == it.start + len) {
                    fallback = new float[] {p, it.x + it.width, it.y, it.height};
                    if (it.kind == LayoutItem.TEXT && it.text.endsWith(" ")) {
                        fallback[1] = it.x + measurer.width(it.text, it.style);
                    }
                } else if (fallback == null && position.offset <= it.start) {
                    fallback = new float[] {p, it.x, it.y, it.height};
                }
            }
        }
        return fallback;
    }
}
