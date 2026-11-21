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

    private final Measurer measurer;
    private final PageSpec spec;
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
        this.spec = doc.page == null ? PageSpec.A4 : doc.page;
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
        List<Slice> slices = blocks(doc.blocks, bodyWidth(), endnotes);
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

    private float bodyWidth() {
        return spec.width - spec.left - spec.right;
    }

    private float bodyHeight() {
        return spec.height - spec.top - spec.bottom;
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

        Slice(float height) {
            this.height = height;
        }

        void shift(float dx, float dy) {
            for (int i = 0; i < items.size(); i++) {
                items.set(i, items.get(i).moved(dx, dy));
            }
        }
    }

    private List<Slice> blocks(List<Block> list, float width, List<Note> endnotes) {
        List<Slice> out = new ArrayList<Slice>();
        for (Block b : list) {
            if (b instanceof Paragraph) {
                out.addAll(paragraph((Paragraph) b, width, endnotes, 1));
            } else if (b instanceof Table) {
                out.addAll(table((Table) b, width, endnotes));
            } else if (b instanceof Picture) {
                Picture p = (Picture) b;
                float[] size = fit(p.width, p.height, width, bodyHeight());
                Slice s = new Slice(size[1] + 6);
                s.items.add(new LayoutItem(LayoutItem.PICTURE, 0, 3, size[0], size[1], 0, null,
                        null, p.id, 0, p, null, 0, null));
                out.add(s);
            }
        }
        return out;
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
    }

    private String marker(Paragraph p) {
        if (Paragraph.NO_LIST.equals(p.list)) {
            return null;
        }
        int level = Math.max(0, Math.min(8, p.listLevel));
        if (Paragraph.BULLET.equals(p.list)) {
            return level % 3 == 0 ? "•" : level % 3 == 1 ? "o" : "-";
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

    private List<Piece> pieces(RichText text, float avail, float scale) {
        List<Piece> out = new ArrayList<Piece>();
        for (int r = 0, at = 0; r < text.runCount(); r++) {
            String s = text.runText(r);
            TextStyle st = scaled(text.runStyle(r), scale);
            Inline obj = text.runObject(r);
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
        if (s.equals("page") || s.startsWith("page ")) {
            return "PAGE";
        }
        if (s.equals("numpages") || s.startsWith("numpages ")) {
            return "NUMPAGES";
        }
        return null;
    }

    /// A paragraph's lines, each a slice. The first carries the space before and the last the
    /// space after.
    private List<Slice> paragraph(Paragraph p, float width, List<Note> endnotes, float scale) {
        String marker = marker(p);
        float left = Math.min(width * 0.8f, p.indent * scale);
        if (marker != null) {
            left = Math.min(width * 0.8f, left + LIST_INDENT * (Math.max(0, p.listLevel) + 1));
        }
        float avail = Math.max(10, width - left);
        List<Piece> pieces = pieces(p.text, avail, scale);
        List<Slice> out = new ArrayList<Slice>();
        TextStyle base = scaled(p.text.runCount() > 0 ? p.text.runStyle(0) : TextStyle.DEFAULT,
                scale);
        int i = 0;
        boolean first = true;
        do {
            List<Piece> line = new ArrayList<Piece>();
            float x = 0;
            boolean hard = false;
            while (i < pieces.size()) {
                Piece q = pieces.get(i);
                if (q.kind == Piece.BREAK) {
                    line.add(q);
                    i++;
                    hard = true;
                    break;
                }
                float w = q.kind == Piece.TAB ? TAB - x % TAB : q.width;
                if (q.kind == Piece.TAB) {
                    q.width = w;
                }
                if (x + w - q.spaceWidth > avail + 0.01f && !line.isEmpty()) {
                    break;
                }
                if (x + w - q.spaceWidth > avail + 0.01f && q.kind == Piece.WORD
                        && q.text.length() > 1) {
                    // a word wider than the line: break it between letters
                    Piece head = splitWord(q, avail - x);
                    line.add(head);
                    x += head.width;
                    break;
                }
                line.add(q);
                x += w;
                i++;
            }
            boolean last = i >= pieces.size();
            out.add(line(p, line, marker != null && first ? marker : null, left, avail, base,
                    hard || last, endnotes));
            first = false;
        } while (i < pieces.size());
        Slice firstSlice = out.get(0);
        float before = p.before * scale;
        firstSlice.height += before;
        firstSlice.shift(0, before);
        firstSlice.breakBefore = p.pageBreak;
        Slice lastSlice = out.get(out.size() - 1);
        lastSlice.height += p.after * scale;
        lastSlice.keepNext = p.keepNext;
        return out;
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

    private Slice line(Paragraph p, List<Piece> line, String marker, float left, float avail,
            TextStyle base, boolean lastLine, List<Note> endnotes) {
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
        float x = left;
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
                out.addAll(paragraph((Paragraph) b, width, new ArrayList<Note>(), NOTE_SCALE));
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
        float colW = width / cols;
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
                List<Slice> cell = blocks(t.cell(r, c), Math.max(4, colW * spanC - 2 * CELL_PAD),
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
            float x = a[1] * colW;
            float w = colW * a[3];
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
            out.add(part);
            from = cut;
        }
        out.get(out.size() - 1).keepNext = s.keepNext;
        return out;
    }

    // ------------------------------------------------------------------ pages

    private final class PageBuilder {
        final List<LayoutItem> items = new ArrayList<LayoutItem>();
        final List<Slice> notes = new ArrayList<Slice>();
        float y;
        float notesHeight;
        boolean empty = true;
    }

    private void paginate(List<Slice> all) {
        float bodyH = bodyHeight();
        List<Slice> slices = new ArrayList<Slice>();
        for (Slice s : all) {
            if (s.height > bodyH) {
                slices.addAll(split(s, bodyH));
            } else {
                slices.add(s);
            }
        }
        PageBuilder page = new PageBuilder();
        List<Slice> headers = new ArrayList<Slice>();
        Table headersOf = null;
        for (int i = 0; i < slices.size(); i++) {
            Slice s = slices.get(i);
            if (s.table != headersOf) {
                headers.clear();
                headersOf = s.table;
            }
            List<Slice> noteParts = new ArrayList<Slice>();
            float noteH = 0;
            for (Note n : s.notes) {
                for (Slice ns : noteSlices(n, bodyWidth())) {
                    noteParts.add(ns);
                    noteH += ns.height;
                }
            }
            float separator = noteParts.isEmpty() || !page.notes.isEmpty() ? 0 : 10;
            float need = s.height;
            float chain = s.height;
            for (int k = i; k < slices.size() - 1 && slices.get(k).keepNext; k++) {
                chain += slices.get(k + 1).height;
            }
            if (chain <= bodyH * 0.8f) {
                need = chain;
            }
            float room = bodyH - page.y - page.notesHeight - separator - noteH;
            if (!page.empty && (s.breakBefore || need > room + 0.01f)) {
                finish(page);
                page = new PageBuilder();
                if (s.table != null && !s.header && !headers.isEmpty()) {
                    for (Slice h : headers) {
                        place(page, h);
                    }
                }
                separator = noteParts.isEmpty() ? 0 : 10;
            }
            place(page, s);
            if (s.header && s.table != null) {
                headers.add(s);
            }
            if (!noteParts.isEmpty()) {
                if (page.notes.isEmpty()) {
                    page.notesHeight += separator;
                }
                page.notes.addAll(noteParts);
                page.notesHeight += noteH;
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

    private void place(PageBuilder page, Slice s) {
        String number = String.valueOf(pages.size() + 1);
        for (LayoutItem item : s.items) {
            LayoutItem moved = item.moved(spec.left, spec.top + page.y);
            if ("PAGE".equals(moved.field)) {
                moved = moved.withText(number);
            }
            page.items.add(moved);
        }
        page.y += s.height;
        page.empty = false;
    }

    private void finish(PageBuilder page) {
        if (!page.notes.isEmpty()) {
            float y = spec.height - spec.bottom - page.notesHeight;
            page.items.add(LayoutItem.line(spec.left, y + 4, bodyWidth() / 3, 0, BORDER));
            y += 10;
            String number = String.valueOf(pages.size() + 1);
            for (Slice ns : page.notes) {
                for (LayoutItem item : ns.items) {
                    LayoutItem moved = item.moved(spec.left, y);
                    if ("PAGE".equals(moved.field)) {
                        moved = moved.withText(number);
                    }
                    page.items.add(moved);
                }
                y += ns.height;
            }
        }
        pages.add(new LayoutPage(pages.size(), spec.width, spec.height, page.items));
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
        for (LayoutItem it : pages.get(page).getItems()) {
            if (it.paragraphId == null || it.kind == LayoutItem.FILL
                    || it.kind == LayoutItem.LINE) {
                continue;
            }
            float dy = y < it.y ? it.y - y : y > it.y + it.height ? y - it.y - it.height : 0;
            if (dy < bestDy) {
                bestDy = dy;
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
