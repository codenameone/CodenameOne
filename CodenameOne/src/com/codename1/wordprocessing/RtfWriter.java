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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// Writes Rich Text Format: character formatting, named paragraph styles, lists, tables with
/// merged cells, hyperlinks and fields, footnotes and endnotes, PNG and JPEG pictures,
/// annotations, tracked changes, the title and the page geometry.
final class RtfWriter {
    private final Document doc;
    private final List<String> fonts = new ArrayList<String>();
    private final List<Integer> colors = new ArrayList<Integer>();
    private final List<String> styles = new ArrayList<String>();
    private final List<String> authors = new ArrayList<String>();

    private RtfWriter(Document doc) {
        this.doc = doc;
        fonts.add("Times New Roman");
        styles.add("Normal");
        authors.add("Unknown");
    }

    static void write(Document doc, OutputStream os) throws IOException {
        RtfWriter w = new RtfWriter(doc);
        StringBuilder body = new StringBuilder();
        w.blocks(body, doc.blocks, false);
        StringBuilder head = new StringBuilder("{\\rtf1\\ansi\\ansicpg1252\\deff0\\uc1\n");
        head.append("{\\fonttbl");
        for (int i = 0; i < w.fonts.size(); i++) {
            String f = w.fonts.get(i);
            head.append("{\\f").append(i).append(f.startsWith("Courier") ? "\\fmodern "
                    : f.startsWith("Arial") ? "\\fswiss " : "\\froman ");
            text(head, f);
            head.append(";}");
        }
        head.append("}\n{\\colortbl;");
        for (Integer c : w.colors) {
            int v = c.intValue();
            head.append("\\red").append(v >> 16 & 0xff).append("\\green").append(v >> 8 & 0xff)
                    .append("\\blue").append(v & 0xff).append(';');
        }
        head.append("}\n{\\stylesheet");
        for (int i = 0; i < w.styles.size(); i++) {
            head.append("{\\s").append(i).append(' ');
            text(head, rtfStyleName(w.styles.get(i)));
            head.append(";}");
        }
        head.append("}\n");
        if (w.authors.size() > 1) {
            head.append("{\\*\\revtbl");
            for (String a : w.authors) {
                head.append('{');
                text(head, a);
                head.append(";}");
            }
            head.append("}\n");
        }
        if (doc.title != null && doc.title.length() > 0) {
            head.append("{\\info{\\title ");
            text(head, doc.title);
            head.append("}}\n");
        }
        if (doc.trackChanges) {
            head.append("\\revisions");
        }
        PageSpec p = doc.page;
        if (p != null) {
            head.append("\\paperw").append(tw(p.width)).append("\\paperh").append(tw(p.height))
                    .append("\\margl").append(tw(p.left)).append("\\margr")
                    .append(tw(p.right)).append("\\margt").append(tw(p.top))
                    .append("\\margb").append(tw(p.bottom));
        }
        head.append('\n').append(body).append("}\n");
        byte[] bytes = new byte[head.length()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) head.charAt(i);
        }
        os.write(bytes);
        os.flush();
    }

    private static int tw(float points) {
        return Math.round(points * 20);
    }

    private static String rtfStyleName(String s) {
        if (s.startsWith("Heading") && s.length() == 8) {
            return "heading " + s.charAt(7);
        }
        return s;
    }

    private int index(List<String> list, String v) {
        int i = list.indexOf(v);
        if (i < 0) {
            list.add(v);
            i = list.size() - 1;
        }
        return i;
    }

    private int color(int rgb) {
        Integer v = Integer.valueOf(rgb & 0xffffff);
        int i = colors.indexOf(v);
        if (i < 0) {
            colors.add(v);
            i = colors.size() - 1;
        }
        return i + 1;
    }

    /// Escapes text: the three specials, tabs and line breaks as control words, and everything
    /// beyond ASCII as a backslash-u escape with a question mark fallback.
    static void text(StringBuilder b, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '{' || c == '}') {
                b.append('\\').append(c);
            } else if (c == '\t') {
                b.append("\\tab ");
            } else if (c == '\n') {
                b.append("\\line ");
            } else if (c == '\r') {
                continue;
            } else if (c < 0x20) {
                b.append(' ');
            } else if (c > 0x7e) {
                b.append("\\u").append(c > 32767 ? c - 65536 : c).append('?');
            } else {
                b.append(c);
            }
        }
    }

    // ------------------------------------------------------------------ blocks

    private void blocks(StringBuilder b, List<Block> list, boolean inTable) {
        for (Block block : list) {
            if (block instanceof Paragraph) {
                paragraph(b, (Paragraph) block, inTable);
                b.append("\\par\n");
            } else if (block instanceof Picture) {
                b.append("\\pard\\plain");
                if (inTable) {
                    b.append("\\intbl");
                }
                b.append(' ');
                picture(b, (Picture) block);
                b.append("\\par\n");
            } else if (block instanceof Table) {
                if (inTable) {
                    flatTable(b, (Table) block);
                } else {
                    table(b, (Table) block);
                }
            }
        }
    }

    private void paragraph(StringBuilder b, Paragraph p, boolean inTable) {
        b.append("\\pard\\plain\\s").append(index(styles, p.style));
        if (inTable) {
            b.append("\\intbl");
        }
        if (Paragraph.CENTER.equals(p.align)) {
            b.append("\\qc");
        } else if (Paragraph.RIGHT.equals(p.align)) {
            b.append("\\qr");
        } else if (Paragraph.JUSTIFY.equals(p.align)) {
            b.append("\\qj");
        }
        if (p.before > 0) {
            b.append("\\sb").append(tw(p.before));
        }
        if (p.after > 0) {
            b.append("\\sa").append(tw(p.after));
        }
        if (p.indent > 0) {
            b.append("\\li").append(tw(p.indent));
        }
        if (p.firstLine != 0) {
            // negative is a hanging indent
            b.append("\\fi").append(tw(p.firstLine));
        }
        if (p.rightIndent > 0) {
            b.append("\\ri").append(tw(p.rightIndent));
        }
        for (TabStop t : p.tabs) {
            if (TabStop.CENTER.equals(t.alignment)) {
                b.append("\\tqc");
            } else if (TabStop.RIGHT.equals(t.alignment)) {
                b.append("\\tqr");
            } else if (TabStop.DECIMAL.equals(t.alignment)) {
                b.append("\\tqdec");
            }
            if (t.leader == '.') {
                b.append("\\tldot");
            } else if (t.leader == '-') {
                b.append("\\tlhyph");
            } else if (t.leader == '_') {
                b.append("\\tlul");
            }
            b.append("\\tx").append(tw(t.position));
        }
        if (p.lineSpacing != 1 && p.lineSpacing > 0) {
            b.append("\\sl").append(Math.round(p.lineSpacing * 240)).append("\\slmult1");
        }
        if (p.pageBreak) {
            b.append("\\pagebb");
        }
        if (p.keepNext) {
            b.append("\\keepn");
        }
        if (!Paragraph.NO_LIST.equals(p.list)) {
            b.append("\\ilvl").append(p.listLevel);
            if (Paragraph.BULLET.equals(p.list)) {
                b.append("{\\*\\pn\\pnlvlblt{\\pntxtb \\u8226?}}");
            } else {
                b.append("{\\*\\pn\\pnlvlbody\\pndec");
                if (p.listStart > 0) {
                    b.append("\\pnstart").append(p.listStart);
                }
                b.append("{\\pntxta .}}");
            }
        }
        // a space after a closing brace is text, not a delimiter: each round trip would add
        // one to the start of the item
        if (b.charAt(b.length() - 1) != '}') {
            b.append(' ');
        }
        rich(b, p.text, p.id);
    }

    private void rich(StringBuilder b, RichText t, String paraId) {
        int at = 0;
        for (int r = 0; r < t.runCount(); r++) {
            String s = t.runText(r);
            TextStyle st = t.runStyle(r);
            Inline obj = t.runObject(r);
            markers(b, paraId, at, false);
            if (obj != null) {
                inline(b, obj, st);
            } else if (s.length() > 0) {
                int start = 0;
                for (int i = 1; i < s.length(); i++) {
                    if (hasMarker(paraId, at + i)) {
                        run(b, s.substring(start, i), st);
                        markers(b, paraId, at + i, false);
                        start = i;
                    }
                }
                run(b, s.substring(start), st);
            }
            at += obj != null ? 1 : s.length();
        }
        markers(b, paraId, at, true);
    }

    private boolean hasMarker(String paraId, int at) {
        for (Comment c : doc.comments) {
            if (paraId.equals(c.startId) && c.start == at
                    || paraId.equals(c.endId) && c.end == at) {
                return true;
            }
        }
        return false;
    }

    /// Writes the comment range starts and ends at an offset; an end also writes the
    /// annotation itself. At the end of a paragraph, everything at or past it is written.
    private void markers(StringBuilder b, String paraId, int at, boolean last) {
        List<Comment> list = doc.comments;
        for (int i = 0; i < list.size(); i++) {
            Comment c = list.get(i);
            if (paraId.equals(c.startId) && (c.start == at || last && c.start > at)) {
                b.append("{\\*\\atrfstart ").append(i + 1).append('}');
            }
        }
        for (int i = 0; i < list.size(); i++) {
            Comment c = list.get(i);
            if (paraId.equals(c.endId) && (c.end == at || last && c.end > at)) {
                b.append("{\\*\\atrfend ").append(i + 1).append('}');
                if (c.author != null && c.author.length() > 0) {
                    b.append("{\\*\\atnauthor ");
                    text(b, c.author);
                    b.append('}');
                }
                b.append("\\chatn{\\*\\annotation{\\*\\atnref ").append(i + 1).append('}');
                int dt = dttm(c.date);
                if (dt != 0) {
                    b.append("{\\*\\atndate ").append(dt).append('}');
                }
                b.append("\\pard\\plain ");
                text(b, c.text);
                b.append('}');
            }
        }
    }

    private void style(StringBuilder b, TextStyle s) {
        b.append("\\plain\\f").append(index(fonts, fontName(s.font)));
        b.append("\\fs").append(Math.max(1, Math.round(s.size * 2)));
        b.append("\\cf").append(color(s.color));
        if (s.background != TextStyle.NO_BACKGROUND) {
            b.append("\\cb").append(color(s.background));
        }
        if (s.bold) {
            b.append("\\b");
        }
        if (s.italic) {
            b.append("\\i");
        }
        if (s.underline) {
            b.append("\\ul");
        }
        if (s.strike) {
            b.append("\\strike");
        }
        if (TextStyle.SUPERSCRIPT.equals(s.script)) {
            b.append("\\super");
        } else if (TextStyle.SUBSCRIPT.equals(s.script)) {
            b.append("\\sub");
        }
        Revision r = s.revision;
        if (r != null) {
            int a = r.author == null || r.author.length() == 0 ? 0 : index(authors, r.author);
            int dt = dttm(r.date);
            if (Revision.DELETE.equals(r.kind)) {
                b.append("\\deleted\\revauthdel").append(a);
                if (dt != 0) {
                    b.append("\\revdttmdel").append(dt);
                }
            } else {
                b.append("\\revised\\revauth").append(a);
                if (dt != 0) {
                    b.append("\\revdttm").append(dt);
                }
            }
        }
        b.append(' ');
    }

    private static String fontName(String f) {
        if (f == null || "Serif".equals(f)) {
            return "Times New Roman";
        }
        if ("SansSerif".equals(f)) {
            return "Arial";
        }
        if ("Monospace".equals(f)) {
            return "Courier New";
        }
        return f;
    }

    /// Packs an ISO date into RTF's `\revdttm` form; zero when it cannot be read.
    static int dttm(String iso) {
        if (iso == null || iso.length() < 10) {
            return 0;
        }
        try {
            int yr = Integer.parseInt(iso.substring(0, 4));
            int mon = Integer.parseInt(iso.substring(5, 7));
            int day = Integer.parseInt(iso.substring(8, 10));
            int hr = iso.length() >= 16 ? Integer.parseInt(iso.substring(11, 13)) : 0;
            int min = iso.length() >= 16 ? Integer.parseInt(iso.substring(14, 16)) : 0;
            if (yr < 1900 || yr > 2411 || mon < 1 || mon > 12 || day < 1 || day > 31) {
                return 0;
            }
            return min | hr << 6 | day << 11 | mon << 16 | yr - 1900 << 20;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void run(StringBuilder b, String s, TextStyle st) {
        if (s.length() == 0) {
            return;
        }
        if (st.link != null && st.link.length() > 0) {
            b.append("{\\field{\\*\\fldinst HYPERLINK ");
            if (st.link.startsWith("#")) {
                b.append("\\\\l \"");
                text(b, st.link.substring(1));
            } else {
                b.append('"');
                text(b, st.link);
            }
            b.append("\"}{\\fldrslt {");
            style(b, st);
            text(b, s);
            b.append("}}}");
            return;
        }
        b.append('{');
        style(b, st);
        text(b, s);
        b.append('}');
    }

    private void inline(StringBuilder b, Inline obj, TextStyle st) {
        if (obj instanceof Field) {
            Field f = (Field) obj;
            b.append("{\\field");
            if (f.locked) {
                b.append("\\fldlock");
            }
            b.append("{\\*\\fldinst ");
            text(b, f.instruction);
            b.append("}{\\fldrslt {");
            style(b, st);
            text(b, f.result);
            b.append("}}}");
        } else if (obj instanceof Note) {
            Note n = (Note) obj;
            if (n.mark != null && n.mark.length() > 0) {
                // other readers number the note; this one keeps the custom mark
                b.append("{\\*\\cnonenotemark ");
                text(b, n.mark);
                b.append('}');
            }
            b.append("{");
            style(b, st);
            b.append("\\super\\chftn}{\\footnote");
            if (Note.ENDNOTE.equals(n.kind)) {
                b.append("\\ftnalt");
            }
            b.append(' ');
            blocks(b, n.blocks, false);
            b.append('}');
        } else if (obj instanceof Picture) {
            picture(b, (Picture) obj);
        } else if (obj instanceof EmbeddedComponent) {
            Picture p = ((EmbeddedComponent) obj).toPicture("e");
            if (p != null) {
                picture(b, p);
            }
        }
    }

    private static void picture(StringBuilder b, Picture p) {
        String kind;
        if ("image/png".equals(p.mimeType)) {
            kind = "\\pngblip";
        } else if ("image/jpeg".equals(p.mimeType)) {
            kind = "\\jpegblip";
        } else {
            return;
        }
        b.append("{\\pict").append(kind).append("\\picwgoal").append(tw(p.width))
                .append("\\pichgoal").append(tw(p.height)).append('\n');
        byte[] data = p.data();
        String digits = "0123456789abcdef";
        for (int i = 0; i < data.length; i++) {
            b.append(digits.charAt(data[i] >> 4 & 0xf)).append(digits.charAt(data[i] & 0xf));
            if (i % 64 == 63) {
                b.append('\n');
            }
        }
        b.append('}');
    }

    // ------------------------------------------------------------------ tables

    private void table(StringBuilder b, Table t) {
        int columns = Math.max(1, t.columns());
        int width = 9000 / columns;
        for (int r = 0; r < t.rows(); r++) {
            b.append("\\trowd\\trgaph108");
            if (r < t.headerRows) {
                b.append("\\trhdr");
            }
            for (int c = 0; c < columns; c++) {
                b.append(mergeFlags(t, r, c));
                b.append("\\cellx").append(width * (c + 1));
            }
            b.append('\n');
            for (int c = 0; c < columns; c++) {
                List<Block> cell = t.covered(r, c) ? null : t.cell(r, c);
                if (cell == null || cell.isEmpty()) {
                    b.append("\\pard\\plain\\intbl \\cell\n");
                    continue;
                }
                StringBuilder inner = new StringBuilder();
                blocks(inner, cell, true);
                String s = inner.toString();
                // the last paragraph mark of a cell is the cell mark
                if (s.endsWith("\\par\n")) {
                    s = s.substring(0, s.length() - 5);
                }
                b.append(s).append("\\cell\n");
            }
            b.append("\\row\n");
        }
        b.append("\\pard\\plain\n");
    }

    private static String mergeFlags(Table t, int r, int c) {
        for (Table.Merge m : t.merges) {
            if (!m.contains(r, c)) {
                continue;
            }
            StringBuilder f = new StringBuilder();
            if (r == m.row) {
                if (c == m.column) {
                    if (m.columns > 1) {
                        f.append("\\clmgf");
                    }
                    if (m.rows > 1) {
                        f.append("\\clvmgf");
                    }
                } else {
                    f.append("\\clmrg");
                }
            } else if (c == m.column) {
                f.append("\\clvmrg");
            } else {
                f.append("\\clmrg");
            }
            return f.toString(); //NOPMD AvoidBranchingStatementAsLastInLoop - the first merge holding the cell
        }
        return "";
    }

    /// A table inside a cell: RTF nesting is not written, so each row becomes a paragraph of
    /// tab separated cell text.
    private void flatTable(StringBuilder b, Table t) {
        for (int r = 0; r < t.rows(); r++) {
            b.append("\\pard\\plain\\intbl ");
            StringBuilder line = new StringBuilder();
            for (int c = 0; c < t.columns(); c++) {
                if (c > 0) {
                    line.append('\t');
                }
                line.append(t.cellText(r, c));
            }
            text(b, line.toString());
            b.append("\\par\n");
        }
    }
}
