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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Reads Rich Text Format.
///
/// Reads character formatting, the font and color tables, named paragraph styles, paragraph
/// layout, lists (both the list table and the older `\pn` numbering), tables with merged cells,
/// fields and hyperlinks, footnotes and endnotes, PNG and JPEG pictures, annotations, tracked
/// changes, the title and the page geometry.
final class RtfReader {
    private static final int MAX_DEPTH = 512;

    private static final int MAX_COLUMNS = 64;

    /// The characters 0x80 to 0x9f of Windows code page 1252; zero where it has none.
    private static final String CP1252 = "€\u0000‚ƒ„…†‡"
            + "ˆ‰Š‹Œ\u0000Ž\u0000\u0000‘’“”"
            + "•–—˜™š›œ\u0000žŸ";

    private final String src;
    private int pos;
    private final Ids ids = new Ids();
    private final List<String> fonts = new ArrayList<String>();
    private final Map<Integer, String> fontsById = new HashMap<Integer, String>();
    private final List<Integer> colors = new ArrayList<Integer>();
    private final Map<Integer, String> styleNames = new HashMap<Integer, String>();
    private final Map<Integer, String[]> listFormats = new HashMap<Integer, String[]>();
    private final Map<Integer, Integer> overrides = new HashMap<Integer, Integer>();
    private final Map<Integer, int[]> overrideStarts = new HashMap<Integer, int[]>();
    private final java.util.Set<String> seenLists = new java.util.HashSet<String>();
    private final List<String> authors = new ArrayList<String>();
    private final List<Comment> comments = new ArrayList<Comment>();
    private final Map<String, int[]> rangeStarts = new HashMap<String, int[]>();
    private final Map<String, String> rangeStartParas = new HashMap<String, String>();
    private final Map<String, String[]> rangeEnds = new HashMap<String, String[]>();
    private String title;
    private float paperW = 612;
    private float paperH = 792;
    private float margL = 90;
    private float margR = 90;
    private float margT = 72;
    private float margB = 72;
    private boolean pageSet;
    private boolean trackChanges;
    private int noteCounter;
    private int commentCounter;
    private int ucSkip = 1;

    private RtfReader(String src) {
        this.src = src;
    }

    static Document read(byte[] data) throws IOException {
        StringBuilder b = new StringBuilder(data.length);
        for (byte d : data) {
            b.append((char) (d & 0xff));
        }
        String s = b.toString();
        int start = s.indexOf("{\\rtf");
        if (start < 0 || start > 16) {
            throw new IOException("Not an RTF document");
        }
        RtfReader r = new RtfReader(s);
        r.pos = start;
        return r.read();
    }

    /// Character formatting; copied on every group.
    static final class Chr {
        int font = -1;
        float size = 12;
        int color;
        int background = TextStyle.NO_BACKGROUND;
        boolean bold;
        boolean italic;
        boolean underline;
        boolean strike;
        boolean hidden;
        String script = TextStyle.BASELINE;
        Revision revision;
        boolean deleted;
        int revAuthor = -1;
        String revDate;

        Chr copy() {
            Chr c = new Chr();
            c.font = font;
            c.size = size;
            c.color = color;
            c.background = background;
            c.bold = bold;
            c.italic = italic;
            c.underline = underline;
            c.strike = strike;
            c.hidden = hidden;
            c.script = script;
            c.revision = revision;
            c.deleted = deleted;
            c.revAuthor = revAuthor;
            c.revDate = revDate;
            return c;
        }

        void reset() {
            font = -1;
            size = 12;
            color = 0;
            background = TextStyle.NO_BACKGROUND;
            bold = false;
            italic = false;
            underline = false;
            strike = false;
            hidden = false;
            script = TextStyle.BASELINE;
            revision = null;
            deleted = false;
            revAuthor = -1;
            revDate = null;
        }
    }

    /// Paragraph formatting, which in RTF belongs to the stream, not to groups.
    static final class Par {
        int style;
        String align = Paragraph.LEFT;
        float before;
        float after;
        float indent;
        float spacing = 1;
        boolean pageBreak;
        boolean keepNext;
        int ls;
        int level;
        String legacyList;
        int legacyStart;
        boolean inTable;
        boolean headerRow;

        void reset() {
            style = 0;
            align = Paragraph.LEFT;
            before = 0;
            after = 0;
            indent = 0;
            spacing = 1;
            pageBreak = false;
            keepNext = false;
            ls = 0;
            level = 0;
            legacyList = null;
            legacyStart = 0;
            inTable = false;
        }
    }

    /// Where text goes: the body, a note, or a table cell, each its own block list.
    final class Sink {
        final List<Block> blocks = new ArrayList<Block>();
        RichText.Builder text = new RichText.Builder();
        String paraId = ids.next('p');
        final Par par = new Par();
        // table being collected
        List<List<List<Block>>> rows;
        List<int[]> rowMerges;
        List<Block> cell = new ArrayList<Block>();
        List<List<Block>> row = new ArrayList<List<Block>>();
        int[] cellMerge = new int[MAX_COLUMNS];
        int cellDefs;
        int headerRows;
        boolean rowHeader;
        boolean pendingBreak;

        void flushParagraph(boolean cellEnd) {
            Par p = par;
            String kind = Paragraph.NO_LIST;
            int start = 0;
            if (p.ls > 0) {
                Integer list = overrides.get(Integer.valueOf(p.ls));
                String[] formats = list == null ? null : listFormats.get(list);
                String f = formats == null ? null : formats[Math.min(8, p.level)];
                kind = "bullet".equals(f) ? Paragraph.BULLET : f == null ? Paragraph.BULLET
                        : Paragraph.NUMBERED;
                if (Paragraph.NUMBERED.equals(kind)) {
                    int[] st = overrideStarts.get(Integer.valueOf(p.ls));
                    if (st != null && st[p.level] > 0 && seenLists.add(p.ls + ":" + p.level)) {
                        start = st[p.level];
                    }
                }
            } else if (p.legacyList != null) {
                kind = p.legacyList;
                if (Paragraph.NUMBERED.equals(kind) && p.legacyStart > 0) {
                    start = p.legacyStart;
                }
            }
            RichText t = text.build();
            String style = styleNames.get(Integer.valueOf(p.style));
            Block b;
            if (t.length() == 1 && t.objectAt(0) instanceof Picture
                    && Paragraph.NO_LIST.equals(kind)) {
                b = (Picture) t.objectAt(0);
            } else {
                b = new Paragraph(paraId, styleName(style), p.align, kind, p.level, start,
                        p.before, p.after, Math.max(0, p.indent), p.spacing,
                        p.pageBreak || pendingBreak, p.keepNext, t);
            }
            pendingBreak = false;
            if (p.inTable || cellEnd) {
                cell.add(b);
            } else {
                endTable();
                blocks.add(b);
            }
            text = new RichText.Builder();
            paraId = ids.next('p');
        }

        void endCell() {
            flushParagraph(true);
            if (row.size() < MAX_COLUMNS) {
                cellMerge[row.size()] = cellMergeFlags[Math.min(row.size(), MAX_COLUMNS - 1)];
                row.add(cell);
            }
            cell = new ArrayList<Block>();
        }

        void endRow() {
            if (!text.build().isEmpty() || !cell.isEmpty()) {
                endCell();
            }
            if (rows == null) {
                rows = new ArrayList<List<List<Block>>>();
                rowMerges = new ArrayList<int[]>();
                headerRows = 0;
            }
            if (!row.isEmpty()) {
                if (rowHeader && headerRows == rows.size()) {
                    headerRows++;
                }
                rows.add(row);
                int[] m = new int[MAX_COLUMNS];
                System.arraycopy(cellMerge, 0, m, 0, MAX_COLUMNS);
                rowMerges.add(m);
            }
            row = new ArrayList<List<Block>>();
            cellMerge = new int[MAX_COLUMNS];
        }

        void endTable() {
            if (rows == null) {
                return;
            }
            if (!row.isEmpty()) {
                endRow();
            }
            List<List<List<Block>>> r = rows;
            List<int[]> m = rowMerges;
            rows = null;
            rowMerges = null;
            if (r.isEmpty()) {
                return;
            }
            blocks.add(buildTable(r, m, headerRows));
        }
    }

    /// Merge flags of the cells defined by the current row: 1 starts a horizontal merge,
    /// 2 continues one, 4 starts a vertical merge, 8 continues one.
    private int[] cellMergeFlags = new int[MAX_COLUMNS];

    private Table buildTable(List<List<List<Block>>> rows, List<int[]> flags, int headerRows) {
        int columns = 0;
        for (List<List<Block>> r : rows) {
            columns = Math.max(columns, r.size());
        }
        for (List<List<Block>> r : rows) {
            while (r.size() < columns) {
                r.add(new ArrayList<Block>());
            }
        }
        List<Table.Merge> merges = new ArrayList<Table.Merge>();
        boolean[][] used = new boolean[rows.size()][columns];
        for (int r = 0; r < rows.size(); r++) {
            for (int c = 0; c < columns; c++) {
                int f = flags.get(r)[c];
                if (used[r][c] || (f & 2) != 0 || (f & 8) != 0) {
                    continue;
                }
                int w = 1;
                while (c + w < columns && (flags.get(r)[c + w] & 2) != 0) {
                    w++;
                }
                int h = 1;
                if ((f & 4) != 0) {
                    while (r + h < rows.size() && (flags.get(r + h)[c] & 8) != 0) {
                        h++;
                    }
                }
                boolean free = true;
                for (int rr = r; rr < r + h && free; rr++) {
                    for (int cc = c; cc < c + w; cc++) {
                        if (used[rr][cc]) {
                            free = false;
                            break;
                        }
                    }
                }
                if (!free) {
                    continue;
                }
                for (int rr = r; rr < r + h; rr++) {
                    for (int cc = c; cc < c + w; cc++) {
                        used[rr][cc] = true;
                    }
                }
                if (w > 1 || h > 1) {
                    merges.add(new Table.Merge(r, c, h, w));
                }
            }
        }
        return new Table(ids.next('t'), rows, merges, Math.min(headerRows, rows.size()));
    }

    private static String styleName(String name) {
        if (name == null) {
            return "Normal";
        }
        String fold = Ids.asciiLower(name).replace(" ", "");
        if (fold.startsWith("heading") && fold.length() == 8 && fold.charAt(7) >= '1'
                && fold.charAt(7) <= '6') {
            return "Heading" + fold.charAt(7);
        }
        if ("title".equals(fold)) {
            return "Title";
        }
        if ("quote".equals(fold)) {
            return "Quote";
        }
        if ("code".equals(fold)) {
            return "Code";
        }
        if ("normal".equals(fold)) {
            return "Normal";
        }
        return name;
    }

    private TextStyle style(Chr c, String link) {
        String font = c.font >= 0 ? fontsById.get(Integer.valueOf(c.font)) : null;
        if (font == null) {
            font = fontsById.get(Integer.valueOf(defaultFont));
        }
        if (font == null || "Times New Roman".equals(font)) {
            font = "Serif";
        } else if ("Arial".equals(font)) {
            font = "SansSerif";
        } else if ("Courier New".equals(font)) {
            font = "Monospace";
        }
        Revision r = null;
        if (c.revision != null || c.deleted) {
            String author = c.revAuthor >= 0 && c.revAuthor < authors.size()
                    ? authors.get(c.revAuthor) : null;
            r = new Revision("rv" + (c.deleted ? "d" : "i") + c.revAuthor, c.deleted
                    ? Revision.DELETE : Revision.INSERT, author, c.revDate);
        }
        int color = c.color > 0 && c.color < colors.size() && colors.get(c.color) != null
                ? colors.get(c.color).intValue() : TextStyle.DEFAULT.color;
        return new TextStyle(font, c.size, color, c.bold, c.italic, c.underline, c.strike,
                c.background, link, c.script, r);
    }

    private int defaultFont;

    // ------------------------------------------------------------------ the scanner

    private String word;
    private int param;
    private boolean hasParam;

    /// Reads the next token: returns '{', '}', '\\' (control word in `word`/`param`), 'c' for a
    /// control symbol (in `word`), 't' for text (in `word`), or -1 at the end.
    private int token() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '\r' || c == '\n') {
                pos++;
                continue;
            }
            if (c == '{' || c == '}') {
                pos++;
                return c;
            }
            if (c == '\\') {
                pos++;
                if (pos >= src.length()) {
                    return -1;
                }
                char d = src.charAt(pos);
                if (isLetter(d)) {
                    int start = pos;
                    while (pos < src.length() && isLetter(src.charAt(pos)) && pos - start < 32) {
                        pos++;
                    }
                    word = src.substring(start, pos);
                    hasParam = false;
                    param = 0;
                    if (pos < src.length() && (src.charAt(pos) == '-'
                            || Character.isDigit(src.charAt(pos)))) {
                        boolean neg = src.charAt(pos) == '-';
                        if (neg) {
                            pos++;
                        }
                        long v = 0;
                        int digits = 0;
                        while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                            if (digits++ < 10) {
                                v = v * 10 + (src.charAt(pos) - '0');
                            }
                            pos++;
                        }
                        v = Math.min(v, Integer.MAX_VALUE);
                        param = (int) (neg ? -v : v);
                        hasParam = true;
                    }
                    if (pos < src.length() && src.charAt(pos) == ' ') {
                        pos++;
                    }
                    return '\\';
                }
                pos++;
                if (d == '\'') {
                    int v = pos + 2 <= src.length() ? hex2(src, pos) : -1;
                    pos = Math.min(src.length(), pos + 2);
                    word = v < 0 ? "" : String.valueOf(ansi(v));
                    return 't';
                }
                if (d == '\r' || d == '\n') {
                    word = "par";
                    hasParam = false;
                    return '\\';
                }
                word = String.valueOf(d);
                return 'c';
            }
            int start = pos;
            while (pos < src.length()) {
                char e = src.charAt(pos);
                if (e == '\\' || e == '{' || e == '}' || e == '\r' || e == '\n') {
                    break;
                }
                pos++;
            }
            word = src.substring(start, pos);
            return 't';
        }
        return -1;
    }

    private static boolean isLetter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    private static int hex2(String s, int at) {
        int a = Character.digit(s.charAt(at), 16);
        int b = Character.digit(s.charAt(at + 1), 16);
        return a < 0 || b < 0 ? -1 : a * 16 + b;
    }

    private static char ansi(int v) {
        if (v >= 0x80 && v < 0xa0) {
            char c = CP1252.charAt(v - 0x80);
            return c == 0 ? '?' : c;
        }
        return (char) v;
    }

    /// Skips the rest of the current group, the opening brace already read.
    private void skipGroup() {
        int depth = 1;
        while (depth > 0 && pos < src.length()) {
            char c = src.charAt(pos++);
            if (c == '\\') {
                pos++;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
        }
    }

    /// The plain text of the rest of the current group.
    private String groupText() {
        StringBuilder b = new StringBuilder();
        int depth = 1;
        while (depth > 0) {
            int t = token();
            if (t < 0) {
                break;
            }
            if (t == '{') {
                depth++;
            } else if (t == '}') {
                depth--;
            } else if (t == 't') {
                b.append(word);
            } else if (t == '\\' && "u".equals(word)) {
                b.append((char) (param < 0 ? param + 65536 : param));
                skipFallback();
            } else if (t == 'c' && ("\\".equals(word) || "{".equals(word) || "}".equals(word))) {
                b.append(word);
            }
        }
        return b.toString();
    }

    private void skipFallback() {
        for (int i = 0; i < ucSkip && pos < src.length(); i++) {
            char c = src.charAt(pos);
            if (c == '\\') {
                if (pos + 1 < src.length() && src.charAt(pos + 1) == '\'') {
                    pos += 4;
                } else {
                    break;
                }
            } else if (c == '{' || c == '}') {
                break;
            } else {
                pos++;
            }
        }
    }

    // ------------------------------------------------------------------ the parser

    private Document read() throws IOException {
        if (token() != '{') {
            throw new IOException("Not an RTF document");
        }
        Sink body = new Sink();
        group(body, new Chr(), "", 1);
        body.par.inTable = false;
        if (!body.text.build().isEmpty()) {
            body.flushParagraph(false);
        }
        body.endTable();
        for (Map.Entry<String, int[]> e : rangeStarts.entrySet()) {
            String[] end = rangeEnds.get(e.getKey());
            int[] start = e.getValue();
            Comment c = comments.get(start[1]);
            String startPara = rangeStartParas.get(e.getKey());
            comments.set(start[1], new Comment(c.id, c.author, c.date, c.text, false, startPara,
                    start[0], end == null ? c.endId : end[0], end == null ? c.end
                    : Integer.parseInt(end[1])));
        }
        PageSpec page = null;
        if (pageSet) {
            try {
                page = new PageSpec(paperW, paperH, margT, margR, margB, margL);
            } catch (IllegalArgumentException ignored) {
                page = null;
            }
        }
        return new Document(body.blocks, page, title, ids.peek(), comments, trackChanges);
    }

    /// Reads a group's content up to its closing brace.
    private void group(Sink sink, Chr chr, String link, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("The RTF document nests too deeply");
        }
        boolean first = true;
        String pendingAnnotationId = null;
        while (true) {
            int t = token();
            if (t < 0) {
                return;
            }
            if (t == '}') {
                return;
            }
            if (t == '{') {
                inner(sink, chr, link, depth + 1);
                first = false;
                continue;
            }
            if (t == 't') {
                if (!chr.hidden) {
                    appendText(sink, word, chr, link);
                }
                first = false;
                continue;
            }
            if (t == 'c') {
                String w = word;
                if ("\\".equals(w) || "{".equals(w) || "}".equals(w)) {
                    appendText(sink, w, chr, link);
                } else if ("~".equals(w)) {
                    appendText(sink, " ", chr, link);
                } else if ("-".equals(w)) {
                    appendText(sink, "­", chr, link);
                } else if ("_".equals(w)) {
                    appendText(sink, "-", chr, link);
                } else if ("*".equals(w) && first) {
                    skipGroup();
                    return;
                }
                first = false;
                continue;
            }
            first = false;
            control(sink, chr, link, pendingAnnotationId);
        }
    }

    private void appendText(Sink sink, String s, Chr chr, String link) {
        if (s.length() > 0) {
            sink.text.append(s, style(chr, link));
        }
    }

    private void control(Sink sink, Chr c, String link, String unused) {
        String w = word;
        Par p = sink.par;
        int v = param;
        boolean on = !hasParam || v != 0;
        if ("par".equals(w)) {
            sink.flushParagraph(false);
        } else if ("line".equals(w)) {
            appendText(sink, "\n", c, link);
        } else if ("tab".equals(w)) {
            appendText(sink, "\t", c, link);
        } else if ("page".equals(w)) {
            if (sink.text.length() > 0) {
                sink.flushParagraph(false);
            }
            sink.pendingBreak = true;
        } else if ("u".equals(w)) {
            if (!c.hidden) {
                appendText(sink, String.valueOf((char) (v < 0 ? v + 65536 : v)), c, link);
            }
            skipFallback();
        } else if ("uc".equals(w)) {
            ucSkip = Math.max(0, Math.min(10, v));
        } else if ("emdash".equals(w)) {
            appendText(sink, "—", c, link);
        } else if ("endash".equals(w)) {
            appendText(sink, "–", c, link);
        } else if ("bullet".equals(w)) {
            appendText(sink, "•", c, link);
        } else if ("lquote".equals(w)) {
            appendText(sink, "‘", c, link);
        } else if ("rquote".equals(w)) {
            appendText(sink, "’", c, link);
        } else if ("ldblquote".equals(w)) {
            appendText(sink, "“", c, link);
        } else if ("rdblquote".equals(w)) {
            appendText(sink, "”", c, link);
        } else if ("plain".equals(w)) {
            c.reset();
            c.font = defaultFont;
        } else if ("b".equals(w)) {
            c.bold = on;
        } else if ("i".equals(w)) {
            c.italic = on;
        } else if ("ul".equals(w) || "uld".equals(w) || "uldb".equals(w) || "ulw".equals(w)) {
            c.underline = on;
        } else if ("ulnone".equals(w)) {
            c.underline = false;
        } else if ("strike".equals(w) || "striked".equals(w)) {
            c.strike = on;
        } else if ("v".equals(w)) {
            c.hidden = on;
        } else if ("f".equals(w)) {
            c.font = v;
        } else if ("fs".equals(w)) {
            if (v > 0 && v < 3277) {
                c.size = v / 2f;
            }
        } else if ("cf".equals(w)) {
            c.color = v;
        } else if ("cb".equals(w) || "highlight".equals(w) || "chcbpat".equals(w)) {
            c.background = v > 0 && v < colors.size() && colors.get(v) != null
                    ? colors.get(v).intValue() : TextStyle.NO_BACKGROUND;
        } else if ("super".equals(w)) {
            c.script = TextStyle.SUPERSCRIPT;
        } else if ("sub".equals(w)) {
            c.script = TextStyle.SUBSCRIPT;
        } else if ("nosupersub".equals(w)) {
            c.script = TextStyle.BASELINE;
        } else if ("revised".equals(w)) {
            c.revision = on ? new Revision("i", Revision.INSERT, null, null) : null;
        } else if ("deleted".equals(w)) {
            c.deleted = on;
        } else if ("revauth".equals(w) || "revauthdel".equals(w)) {
            c.revAuthor = v;
        } else if ("revdttm".equals(w) || "revdttmdel".equals(w)) {
            c.revDate = dttm(v);
        } else if ("revisions".equals(w)) {
            trackChanges = true;
        } else if ("pard".equals(w)) {
            p.reset();
        } else if ("s".equals(w)) {
            p.style = v;
        } else if ("ql".equals(w)) {
            p.align = Paragraph.LEFT;
        } else if ("qc".equals(w)) {
            p.align = Paragraph.CENTER;
        } else if ("qr".equals(w)) {
            p.align = Paragraph.RIGHT;
        } else if ("qj".equals(w) || "qd".equals(w)) {
            p.align = Paragraph.JUSTIFY;
        } else if ("sb".equals(w)) {
            p.before = Math.max(0, v / 20f);
        } else if ("sa".equals(w)) {
            p.after = Math.max(0, v / 20f);
        } else if ("li".equals(w)) {
            p.indent = v / 20f;
        } else if ("sl".equals(w)) {
            if (v > 0) {
                p.spacing = Math.round(v * 100f / 240) / 100f;
            }
        } else if ("pagebb".equals(w)) {
            p.pageBreak = on;
        } else if ("keepn".equals(w)) {
            p.keepNext = on;
        } else if ("ls".equals(w)) {
            p.ls = v;
        } else if ("ilvl".equals(w)) {
            p.level = Math.max(0, Math.min(8, v));
        } else if ("pnlvlblt".equals(w)) {
            p.legacyList = Paragraph.BULLET;
        } else if ("pnlvlbody".equals(w) || "pndec".equals(w)) {
            if (p.legacyList == null) {
                p.legacyList = Paragraph.NUMBERED;
            }
        } else if ("intbl".equals(w)) {
            p.inTable = true;
        } else if ("trowd".equals(w)) {
            cellMergeFlags = new int[MAX_COLUMNS];
            sink.cellDefs = 0;
            sink.rowHeader = false;
            if (sink.rows == null) {
                sink.rows = new ArrayList<List<List<Block>>>();
                sink.rowMerges = new ArrayList<int[]>();
                sink.headerRows = 0;
            }
        } else if ("trhdr".equals(w)) {
            sink.rowHeader = true;
        } else if ("clmgf".equals(w)) {
            mergeFlag(sink, 1);
        } else if ("clmrg".equals(w)) {
            mergeFlag(sink, 2);
        } else if ("clvmgf".equals(w)) {
            mergeFlag(sink, 4);
        } else if ("clvmrg".equals(w)) {
            mergeFlag(sink, 8);
        } else if ("cellx".equals(w)) {
            sink.cellDefs = Math.min(MAX_COLUMNS - 1, sink.cellDefs + 1);
        } else if ("cell".equals(w) || "nestcell".equals(w)) {
            sink.endCell();
        } else if ("row".equals(w) || "nestrow".equals(w)) {
            sink.endRow();
        } else if ("paperw".equals(w)) {
            paperW = twips(v, paperW);
            pageSet = true;
        } else if ("paperh".equals(w)) {
            paperH = twips(v, paperH);
            pageSet = true;
        } else if ("margl".equals(w)) {
            margL = twips(v, margL);
            pageSet = true;
        } else if ("margr".equals(w)) {
            margR = twips(v, margR);
            pageSet = true;
        } else if ("margt".equals(w)) {
            margT = twips(v, margT);
            pageSet = true;
        } else if ("margb".equals(w)) {
            margB = twips(v, margB);
            pageSet = true;
        } else if ("deff".equals(w)) {
            defaultFont = v;
        } else if ("chatn".equals(w)) {
            // the annotation's anchor character; the annotation group follows
            pendingAnchor = true;
        }
    }

    private boolean pendingAnchor;

    private void mergeFlag(Sink sink, int flag) {
        int i = Math.min(MAX_COLUMNS - 1, sink.cellDefs);
        cellMergeFlags[i] |= flag;
    }

    private static float twips(int v, float fallback) {
        return v > 0 && v < 20 * 20000 ? v / 20f : fallback;
    }

    private static String dttm(int v) {
        int min = v & 0x3f;
        int hr = v >> 6 & 0x1f;
        int day = v >> 11 & 0x1f;
        int mon = v >> 16 & 0xf;
        int yr = (v >> 20 & 0x1ff) + 1900;
        if (mon < 1 || mon > 12 || day < 1) {
            return null;
        }
        return yr + "-" + two(mon) + "-" + two(day) + "T" + two(hr) + ":" + two(min) + ":00Z";
    }

    private static String two(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    /// A group just opened: dispatch on its first control word.
    private void inner(Sink sink, Chr outer, String link, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("The RTF document nests too deeply");
        }
        int save = pos;
        int t = token();
        String w = t == '\\' ? word : t == 'c' ? word : null;
        boolean starred = false;
        if (t == 'c' && "*".equals(word)) {
            starred = true;
            t = token();
            w = t == '\\' ? word : null;
        }
        if (w == null) {
            pos = save;
            group(sink, outer.copy(), link, depth);
            return;
        }
        if ("fonttbl".equals(w)) {
            fontTable();
        } else if ("colortbl".equals(w)) {
            colorTable();
        } else if ("stylesheet".equals(w)) {
            styleSheet();
        } else if ("listtable".equals(w)) {
            listTable();
        } else if ("listoverridetable".equals(w)) {
            overrideTable();
        } else if ("revtbl".equals(w)) {
            revTable();
        } else if ("info".equals(w)) {
            info();
        } else if ("footnote".equals(w)) {
            footnote(sink, outer, link, depth);
        } else if ("field".equals(w)) {
            field(sink, outer, link, depth);
        } else if ("pict".equals(w)) {
            Picture pic = picture();
            if (pic != null) {
                sink.text.append(pic, style(outer, link));
            }
        } else if ("shppict".equals(w)) {
            group(sink, outer.copy(), link, depth);
        } else if ("nonshppict".equals(w)) {
            skipGroup();
        } else if ("cnonenotemark".equals(w)) {
            pendingNoteMark = groupText();
        } else if ("atnid".equals(w) || "atnauthor".equals(w)) {
            annotationAuthor = groupText();
        } else if ("annotation".equals(w)) {
            annotation(sink);
        } else if ("atrfstart".equals(w)) {
            String id = groupText().trim();
            rangeStarts.put(id, new int[] {sink.text.length(), -1});
            rangeStartParas.put(id, sink.paraId);
        } else if ("atrfend".equals(w)) {
            String id = groupText().trim();
            rangeEnds.put(id, new String[] {sink.paraId, String.valueOf(sink.text.length())});
        } else if ("atnref".equals(w)) {
            annotationRef = groupText().trim();
        } else if ("atndate".equals(w)) {
            String d = groupText().trim();
            try {
                annotationDate = dttm(Integer.parseInt(d));
            } catch (NumberFormatException e) {
                annotationDate = null;
            }
        } else if (starred || "header".equals(w) || "footer".equals(w) || "headerl".equals(w)
                || "headerr".equals(w) || "headerf".equals(w) || "footerl".equals(w)
                || "footerr".equals(w) || "footerf".equals(w) || "pn".equals(w)
                || "pntext".equals(w) || "listtext".equals(w) || "xe".equals(w)
                || "tc".equals(w) || "bkmkstart".equals(w) || "bkmkend".equals(w)
                || "object".equals(w) || "generator".equals(w) || "themedata".equals(w)
                || "latentstyles".equals(w) || "datastore".equals(w)) {
            if ("pn".equals(w)) {
                pnGroup(sink);
            } else {
                skipGroup();
            }
        } else {
            pos = save;
            group(sink, outer.copy(), link, depth);
        }
    }

    private String annotationAuthor;
    private String annotationRef;
    private String annotationDate;

    private void pnGroup(Sink sink) {
        int depth = 1;
        while (depth > 0) {
            int t = token();
            if (t < 0) {
                return;
            }
            if (t == '{') {
                depth++;
            } else if (t == '}') {
                depth--;
            } else if (t == '\\') {
                if ("pnlvlblt".equals(word)) {
                    sink.par.legacyList = Paragraph.BULLET;
                } else if ("pnlvlbody".equals(word) || "pndec".equals(word)) {
                    if (sink.par.legacyList == null) {
                        sink.par.legacyList = Paragraph.NUMBERED;
                    }
                } else if ("pnstart".equals(word) && param > 0) {
                    sink.par.legacyStart = param;
                } else if ("pntxtb".equals(word) || "pntxta".equals(word)) {
                    skipGroup();
                    depth--;
                }
            }
        }
    }

    private void fontTable() {
        int depth = 1;
        int id = -1;
        StringBuilder name = new StringBuilder();
        while (depth > 0) {
            int t = token();
            if (t < 0) {
                return;
            }
            if (t == '{') {
                depth++;
                if (depth > 2) {
                    skipGroup();
                    depth--;
                }
            } else if (t == '}') {
                depth--;
                if (depth >= 1 && id >= 0) {
                    commitFont(id, name);
                    id = -1;
                    name.setLength(0);
                }
            } else if (t == '\\' && "f".equals(word)) {
                if (id >= 0) {
                    commitFont(id, name);
                    name.setLength(0);
                }
                id = param;
            } else if (t == 't') {
                name.append(word);
                int semi = name.indexOf(";");
                if (semi >= 0 && id >= 0) {
                    name.setLength(semi);
                    commitFont(id, name);
                    id = -1;
                    name.setLength(0);
                }
            } else if (t == 'c' && "*".equals(word)) {
                skipGroup();
                depth--;
            }
        }
    }

    private void commitFont(int id, StringBuilder name) {
        String n = name.toString().trim();
        if (n.endsWith(";")) {
            n = n.substring(0, n.length() - 1).trim();
        }
        if (n.length() > 0 && fontsById.size() < 4096) {
            fontsById.put(Integer.valueOf(id), n);
            fonts.add(n);
        }
    }

    private void colorTable() {
        int r = 0;
        int g = 0;
        int b = 0;
        boolean any = false;
        while (true) {
            int t = token();
            if (t < 0 || t == '}') {
                return;
            }
            if (t == '\\') {
                if ("red".equals(word)) {
                    r = param & 0xff;
                    any = true;
                } else if ("green".equals(word)) {
                    g = param & 0xff;
                    any = true;
                } else if ("blue".equals(word)) {
                    b = param & 0xff;
                    any = true;
                }
            } else if (t == 't') {
                for (int i = 0; i < word.length(); i++) {
                    if (word.charAt(i) == ';' && colors.size() < 4096) {
                        colors.add(any ? Integer.valueOf(r << 16 | g << 8 | b) : null);
                        r = 0;
                        g = 0;
                        b = 0;
                        any = false;
                    }
                }
            } else if (t == '{') {
                skipGroup();
            }
        }
    }

    private void styleSheet() {
        int depth = 1;
        while (depth > 0) {
            int t = token();
            if (t < 0) {
                return;
            }
            if (t == '}') {
                depth--;
            } else if (t == '{') {
                int id = 0;
                boolean paragraph = true;
                StringBuilder name = new StringBuilder();
                int d = 1;
                while (d > 0) {
                    int u = token();
                    if (u < 0) {
                        return;
                    }
                    if (u == '{') {
                        skipGroup();
                    } else if (u == '}') {
                        d--;
                    } else if (u == '\\') {
                        if ("s".equals(word)) {
                            id = param;
                        } else if ("cs".equals(word) || "ds".equals(word) || "ts".equals(word)) {
                            paragraph = false;
                        }
                    } else if (u == 't') {
                        name.append(word);
                    }
                }
                String n = name.toString().trim();
                if (n.endsWith(";")) {
                    n = n.substring(0, n.length() - 1).trim();
                }
                if (paragraph && n.length() > 0) {
                    styleNames.put(Integer.valueOf(id), n);
                }
            }
        }
    }

    private void listTable() {
        int depth = 1;
        String[] levels = null;
        int level = -1;
        int listDepth = 0;
        while (depth > 0) {
            int t = token();
            if (t < 0) {
                return;
            }
            if (t == '{') {
                depth++;
            } else if (t == '}') {
                depth--;
                if (listDepth > 0 && depth < listDepth) {
                    listDepth = 0;
                }
            } else if (t == '\\') {
                String w = word;
                if ("list".equals(w)) {
                    levels = new String[9];
                    level = -1;
                    listDepth = depth;
                } else if ("listlevel".equals(w) && levels != null) {
                    level++;
                } else if ("levelnfc".equals(w) || "levelnfcn".equals(w)) {
                    if (levels != null && level >= 0 && level < 9) {
                        levels[level] = param == 23 ? "bullet" : param == 255 ? "none" : "number";
                    }
                } else if ("listid".equals(w) && levels != null) {
                    listFormats.put(Integer.valueOf(param), levels);
                } else if ("leveltext".equals(w) || "levelnumbers".equals(w)
                        || "listname".equals(w)) {
                    skipGroup();
                    depth--;
                }
            } else if (t == 'c' && "*".equals(word)) {
                skipGroup();
                depth--;
            }
        }
    }

    private void overrideTable() {
        int depth = 1;
        int listId = 0;
        int[] starts = null;
        int level = -1;
        while (depth > 0) {
            int t = token();
            if (t < 0) {
                return;
            }
            if (t == '{') {
                depth++;
            } else if (t == '}') {
                depth--;
            } else if (t == '\\') {
                String w = word;
                if ("listoverride".equals(w)) {
                    starts = new int[9];
                    level = -1;
                } else if ("listid".equals(w)) {
                    listId = param;
                } else if ("lfolevel".equals(w)) {
                    level++;
                } else if (("listoverridestartat".equals(w) || "levelstartat".equals(w))
                        && starts != null && level >= 0 && level < 9 && hasParam) {
                    starts[level] = Math.max(1, param);
                } else if ("ls".equals(w)) {
                    overrides.put(Integer.valueOf(param), Integer.valueOf(listId));
                    if (starts != null) {
                        overrideStarts.put(Integer.valueOf(param), starts);
                    }
                }
            }
        }
    }

    private void revTable() {
        int depth = 1;
        StringBuilder name = new StringBuilder();
        while (depth > 0) {
            int t = token();
            if (t < 0) {
                return;
            }
            if (t == '{') {
                depth++;
                name.setLength(0);
            } else if (t == '}') {
                depth--;
                String n = name.toString().trim();
                if (n.endsWith(";")) {
                    n = n.substring(0, n.length() - 1);
                }
                if (depth >= 1 && authors.size() < 4096) {
                    authors.add(n);
                }
                name.setLength(0);
            } else if (t == 't') {
                name.append(word);
            }
        }
    }

    private void info() {
        int depth = 1;
        while (depth > 0) {
            int t = token();
            if (t < 0) {
                return;
            }
            if (t == '{') {
                int u = token();
                if (u == '\\' && "title".equals(word)) {
                    title = groupText().trim();
                } else {
                    skipGroup();
                }
            } else if (t == '}') {
                depth--;
            }
        }
    }

    private void footnote(Sink sink, Chr outer, String link, int depth) throws IOException {
        Sink note = new Sink();
        int save = pos;
        boolean endnote = false;
        int t = token();
        if (t == '\\' && "ftnalt".equals(word)) {
            endnote = true;
        } else {
            pos = save;
        }
        group(note, new Chr(), "", depth);
        note.par.inTable = false;
        if (!note.text.build().isEmpty()) {
            note.flushParagraph(false);
        }
        note.endTable();
        List<Block> body = note.blocks;
        // the note text repeats the reference mark (chftn); drop it from the start
        if (!body.isEmpty() && body.get(0) instanceof Paragraph) {
            Paragraph p = (Paragraph) body.get(0);
            String plain = p.text.plainText();
            int cut = 0;
            while (cut < plain.length() && plain.charAt(cut) == ' ') {
                cut++;
            }
            if (cut > 0) {
                body.set(0, p.withText(p.text.substring(cut, p.text.length())));
            }
        }
        String mark = pendingNoteMark;
        pendingNoteMark = null;
        sink.text.append(new Note("n" + ++noteCounter, endnote ? Note.ENDNOTE : Note.FOOTNOTE,
                mark, body), style(outer, link));
    }

    private String pendingNoteMark;

    private void field(Sink sink, Chr outer, String link, int depth) throws IOException {
        String instruction = "";
        Sink result = null;
        boolean locked = false;
        while (true) {
            int t = token();
            if (t < 0 || t == '}') {
                break;
            }
            if (t == '\\' && "fldlock".equals(word)) {
                locked = true;
            } else if (t == '{') {
                int save = pos;
                int u = token();
                if (u == 'c' && "*".equals(word)) {
                    u = token();
                }
                if (u == '\\' && "fldinst".equals(word)) {
                    instruction = groupText().trim();
                } else if (u == '\\' && "fldrslt".equals(word)) {
                    result = new Sink();
                    result.par.style = sink.par.style;
                    String target = null;
                    if (Ids.asciiLower(instruction).startsWith("hyperlink")) {
                        target = hyperlink(instruction);
                    }
                    group(result, outer.copy(), target != null ? target : link, depth + 1);
                } else {
                    pos = save;
                    skipGroup();
                }
            }
        }
        RichText shown = result == null ? RichText.EMPTY : result.text.build();
        if (Ids.asciiLower(instruction).startsWith("hyperlink")) {
            sink.text.append(shown);
            return;
        }
        if (instruction.length() == 0) {
            sink.text.append(shown);
            return;
        }
        TextStyle s = shown.runCount() > 0 ? shown.runStyle(0) : style(outer, link);
        sink.text.append(new Field(instruction, shown.displayText(), locked), s);
    }

    private static String hyperlink(String instruction) {
        int q = instruction.indexOf('"');
        if (q < 0) {
            String[] parts = instruction.split(" ");
            return parts.length > 1 ? parts[parts.length - 1] : "";
        }
        int end = instruction.indexOf('"', q + 1);
        String url = end > q ? instruction.substring(q + 1, end) : instruction.substring(q + 1);
        int l = instruction.indexOf("\\l");
        return l >= 0 && l < q ? "#" + url : url;
    }

    private Picture picture() {
        float wGoal = 0;
        float hGoal = 0;
        float w = 0;
        float h = 0;
        int scaleX = 100;
        int scaleY = 100;
        boolean known = false;
        java.io.ByteArrayOutputStream data = new java.io.ByteArrayOutputStream();
        int pending = -1;
        int depth = 1;
        while (depth > 0) {
            int t = token();
            if (t < 0) {
                break;
            }
            if (t == '{') {
                skipGroup();
            } else if (t == '}') {
                depth--;
            } else if (t == '\\') {
                String x = word;
                if ("pngblip".equals(x) || "jpegblip".equals(x)) {
                    known = true;
                } else if ("picwgoal".equals(x)) {
                    wGoal = param / 20f;
                } else if ("pichgoal".equals(x)) {
                    hGoal = param / 20f;
                } else if ("picw".equals(x)) {
                    w = param;
                } else if ("pich".equals(x)) {
                    h = param;
                } else if ("picscalex".equals(x)) {
                    scaleX = param;
                } else if ("picscaley".equals(x)) {
                    scaleY = param;
                } else if ("bin".equals(x) && param > 0) {
                    int n = Math.min(param, src.length() - pos);
                    for (int i = 0; i < n; i++) {
                        data.write(src.charAt(pos + i));
                    }
                    pos += n;
                }
            } else if (t == 't') {
                String s = word;
                for (int i = 0; i < s.length(); i++) {
                    int d = Character.digit(s.charAt(i), 16);
                    if (d < 0) {
                        continue;
                    }
                    if (pending < 0) {
                        pending = d;
                    } else {
                        data.write(pending * 16 + d);
                        pending = -1;
                    }
                }
            }
        }
        if (!known || data.size() == 0) {
            return null;
        }
        float pw = wGoal > 0 ? wGoal : w * 0.75f;
        float ph = hGoal > 0 ? hGoal : h * 0.75f;
        pw = pw * scaleX / 100;
        ph = ph * scaleY / 100;
        if (Float.isNaN(pw) || Float.isNaN(ph) || pw <= 0 || ph <= 0 || pw > 20000 || ph > 20000) {
            pw = 100;
            ph = 100;
        }
        return new Picture(ids.next('i'), data.toByteArray(), null, pw, ph, null);
    }

    private void annotation(Sink sink) throws IOException {
        Sink a = new Sink();
        group(a, new Chr(), "", 2);
        a.par.inTable = false;
        if (!a.text.build().isEmpty()) {
            a.flushParagraph(false);
        }
        a.endTable();
        StringBuilder text = new StringBuilder();
        for (Block b : a.blocks) {
            if (text.length() > 0) {
                text.append('\n');
            }
            Document.appendPlainText(text, b);
        }
        String body = text.toString().trim();
        int at = sink.text.length();
        Comment c = new Comment("c" + ++commentCounter, annotationAuthor, annotationDate, body,
                false, sink.paraId, at, sink.paraId, at);
        comments.add(c);
        if (annotationRef != null && rangeStarts.containsKey(annotationRef)) {
            rangeStarts.get(annotationRef)[1] = comments.size() - 1;
        } else if (annotationRef != null) {
            rangeStarts.remove(annotationRef);
        }
        for (Map.Entry<String, int[]> e : rangeStarts.entrySet()) {
            if (e.getValue()[1] < 0) {
                e.getValue()[1] = comments.size() - 1;
            }
        }
        annotationAuthor = null;
        annotationRef = null;
        annotationDate = null;
        pendingAnchor = false;
    }
}
