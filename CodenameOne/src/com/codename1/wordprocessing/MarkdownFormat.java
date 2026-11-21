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

import com.codename1.util.Base64;

import java.util.ArrayList;
import java.util.List;

/// Markdown with GitHub's extensions that map onto the model: pipe tables, strike-through and
/// fenced code. Headings, block quotes, nested lists, emphasis, code spans, links and `data:`
/// images are read and written; other images are read as their alternative text.
///
/// The rich text editor has its own Markdown importer, but it fills the editor's flat buffer,
/// which has no tables or pictures; this one builds blocks.
final class MarkdownFormat {
    private MarkdownFormat() {
    }

    // ---------------------------------------------------------------- reading

    static Document read(String md) {
        String[] lines = splitLines(md);
        Ids ids = new Ids();
        List<Block> out = new ArrayList<Block>();
        StringBuilder para = new StringBuilder();
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.length() == 0) {
                flush(out, ids, para);
                i++;
                continue;
            }
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                flush(out, ids, para);
                String fence = trimmed.substring(0, 3);
                i++;
                while (i < lines.length && !lines[i].trim().startsWith(fence)) {
                    out.add(new Paragraph(ids.next('p'), "Code", Paragraph.LEFT,
                            Paragraph.NO_LIST, 0, 0, 0, 0, 0, 1, false, false,
                            RichText.of(lines[i], TextStyle.DEFAULT.withFont("Monospace"))));
                    i++;
                }
                i++;
                continue;
            }
            int hashes = headingLevel(trimmed);
            if (hashes > 0) {
                flush(out, ids, para);
                String text = trimmed.substring(hashes).trim();
                while (text.endsWith("#")) {
                    text = text.substring(0, text.length() - 1).trim();
                }
                TextStyle base = TextStyle.DEFAULT.withBold(true)
                        .withSize(HtmlFormat.headingSize(hashes));
                out.add(new Paragraph(ids.next('p'), "Heading" + hashes, Paragraph.LEFT,
                        Paragraph.NO_LIST, 0, 0, 12, 6, 0, 1.15f, false, true,
                        inline(text, base)));
                i++;
                continue;
            }
            if (isRule(trimmed)) {
                flush(out, ids, para);
                i++;
                continue;
            }
            if (trimmed.startsWith("|") && i + 1 < lines.length && isDelimiterRow(lines[i + 1])) {
                flush(out, ids, para);
                int end = i + 2;
                while (end < lines.length && lines[end].trim().startsWith("|")) {
                    end++;
                }
                out.add(table(lines, i, end, ids));
                i = end;
                continue;
            }
            if (trimmed.startsWith(">")) {
                flush(out, ids, para);
                StringBuilder q = new StringBuilder();
                while (i < lines.length && lines[i].trim().startsWith(">")) {
                    String l = lines[i].trim().substring(1);
                    if (l.startsWith(" ")) {
                        l = l.substring(1);
                    }
                    if (l.trim().length() == 0) {
                        addQuote(out, ids, q);
                    } else {
                        if (q.length() > 0) {
                            q.append(' ');
                        }
                        q.append(l.trim());
                    }
                    i++;
                }
                addQuote(out, ids, q);
                continue;
            }
            int[] item = listItem(line);
            if (item != null) {
                flush(out, ids, para);
                String kind = item[1] == 1 ? Paragraph.NUMBERED : Paragraph.BULLET;
                StringBuilder text = new StringBuilder(line.substring(item[2]).trim());
                i++;
                // lazy continuation lines
                while (i < lines.length && lines[i].trim().length() > 0 && listItem(lines[i]) == null
                        && leading(lines[i]) > item[0] && !lines[i].trim().startsWith("|")) {
                    text.append(' ').append(lines[i].trim());
                    i++;
                }
                Picture pic = imageOnly(text.toString(), ids);
                if (pic != null) {
                    out.add(pic);
                    continue;
                }
                out.add(new Paragraph(ids.next('p'), "Normal", Paragraph.LEFT, kind,
                        Math.min(8, item[0] / 2), item[1] == 1 ? item[3] : 0, 0, 2, 0, 1.15f,
                        false, false, inline(text.toString(), TextStyle.DEFAULT)));
                continue;
            }
            Picture pic = para.length() == 0 ? imageOnly(trimmed, ids) : null;
            if (pic != null) {
                out.add(pic);
                i++;
                continue;
            }
            if (para.length() > 0) {
                boolean hard = endsWithHardBreak(para);
                trimHardBreak(para);
                para.append(hard ? '\n' : ' ');
            }
            para.append(line.trim().length() == line.length() ? line : stripLeading(line));
            i++;
        }
        flush(out, ids, para);
        return new Document(out, null, null, ids.peek());
    }

    private static String[] splitLines(String s) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == '\n') {
                int end = i;
                if (end > start && s.charAt(end - 1) == '\r') {
                    end--;
                }
                out.add(s.substring(start, end).replace('\t', ' '));
                start = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    private static String stripLeading(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') {
            i++;
        }
        return s.substring(i);
    }

    private static int leading(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private static boolean endsWithHardBreak(StringBuilder b) {
        int n = b.length();
        return n >= 2 && b.charAt(n - 1) == ' ' && b.charAt(n - 2) == ' '
                || n >= 1 && b.charAt(n - 1) == '\\';
    }

    private static void trimHardBreak(StringBuilder b) {
        int n = b.length();
        if (n >= 1 && b.charAt(n - 1) == '\\') {
            b.setLength(n - 1);
        }
        while (b.length() > 0 && b.charAt(b.length() - 1) == ' ') {
            b.setLength(b.length() - 1);
        }
    }

    private static void flush(List<Block> out, Ids ids, StringBuilder para) {
        if (para.length() == 0) {
            return;
        }
        trimHardBreak(para);
        out.add(new Paragraph(ids.next('p'), inline(para.toString(), TextStyle.DEFAULT)));
        para.setLength(0);
    }

    private static void addQuote(List<Block> out, Ids ids, StringBuilder q) {
        if (q.length() == 0) {
            return;
        }
        out.add(new Paragraph(ids.next('p'), "Quote", Paragraph.LEFT, Paragraph.NO_LIST, 0, 0, 0,
                8, 0, 1.15f, false, false, inline(q.toString(), TextStyle.DEFAULT)));
        q.setLength(0);
    }

    private static int headingLevel(String t) {
        int n = 0;
        while (n < t.length() && n < 7 && t.charAt(n) == '#') {
            n++;
        }
        if (n == 0 || n > 6) {
            return 0;
        }
        return n == t.length() || t.charAt(n) == ' ' ? n : 0;
    }

    private static boolean isRule(String t) {
        if (t.length() < 3) {
            return false;
        }
        char c = t.charAt(0);
        if (c != '-' && c != '*' && c != '_') {
            return false;
        }
        int count = 0;
        for (int i = 0; i < t.length(); i++) {
            char d = t.charAt(i);
            if (d == c) {
                count++;
            } else if (d != ' ') {
                return false;
            }
        }
        return count >= 3;
    }

    /// `{indent, numbered ? 1 : 0, text start, number}` for a list item line, else null.
    private static int[] listItem(String line) {
        int indent = leading(line);
        if (indent >= line.length()) {
            return null;
        }
        char c = line.charAt(indent);
        if ((c == '-' || c == '*' || c == '+') && indent + 1 < line.length()
                && line.charAt(indent + 1) == ' ') {
            if (isRule(line.trim())) {
                return null;
            }
            return new int[] {indent, 0, indent + 2, 0};
        }
        int j = indent;
        while (j < line.length() && j - indent < 9 && Character.isDigit(line.charAt(j))) {
            j++;
        }
        if (j > indent && j + 1 < line.length() && (line.charAt(j) == '.' || line.charAt(j) == ')')
                && line.charAt(j + 1) == ' ') {
            return new int[] {indent, 1, j + 2, Integer.parseInt(line.substring(indent, j))};
        }
        return null;
    }

    private static boolean isDelimiterRow(String line) {
        String t = line.trim();
        if (!t.startsWith("|") && t.indexOf('|') < 0) {
            return false;
        }
        boolean dash = false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '-') {
                dash = true;
            } else if (c != '|' && c != ':' && c != ' ') {
                return false;
            }
        }
        return dash;
    }

    private static List<String> cells(String line) {
        String t = line.trim();
        if (t.startsWith("|")) {
            t = t.substring(1);
        }
        if (t.endsWith("|") && !t.endsWith("\\|")) {
            t = t.substring(0, t.length() - 1);
        }
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '\\' && i + 1 < t.length() && t.charAt(i + 1) == '|') {
                cur.append('|');
                i++;
            } else if (c == '|') {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString().trim());
        return out;
    }

    private static Table table(String[] lines, int start, int end, Ids ids) {
        List<String> aligns = cells(lines[start + 1]);
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(cells(lines[start]));
        for (int i = start + 2; i < end; i++) {
            rows.add(cells(lines[i]));
        }
        int cols = rows.get(0).size();
        String tableId = ids.next('t');
        List<List<List<Block>>> grid = new ArrayList<List<List<Block>>>();
        for (int r = 0; r < rows.size(); r++) {
            List<List<Block>> row = new ArrayList<List<Block>>();
            for (int c = 0; c < cols; c++) {
                String v = c < rows.get(r).size() ? rows.get(r).get(c) : "";
                String a = c < aligns.size() ? aligns.get(c) : "";
                String align = a.startsWith(":") && a.endsWith(":") ? Paragraph.CENTER
                        : a.endsWith(":") ? Paragraph.RIGHT : Paragraph.LEFT;
                List<Block> cell = new ArrayList<Block>();
                String[] parts = splitBr(v);
                for (String part : parts) {
                    TextStyle base = r == 0 ? TextStyle.DEFAULT.withBold(true) : TextStyle.DEFAULT;
                    cell.add(new Paragraph(ids.next('p'), "Normal", align, Paragraph.NO_LIST, 0, 0,
                            0, 0, 0, 1.15f, false, false, inline(part, base)));
                }
                row.add(cell);
            }
            grid.add(row);
        }
        return new Table(tableId, grid, null, 1);
    }

    private static String[] splitBr(String v) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        String lower = Ids.asciiLower(v);
        for (int i = lower.indexOf("<br"); i >= 0; i = lower.indexOf("<br", start)) {
            int close = lower.indexOf('>', i);
            if (close < 0) {
                break;
            }
            out.add(v.substring(start, i));
            start = close + 1;
        }
        out.add(v.substring(start));
        return out.toArray(new String[out.size()]);
    }

    private static Picture imageOnly(String t, Ids ids) {
        if (!t.startsWith("![") || !t.endsWith(")")) {
            return null;
        }
        int close = t.indexOf("](");
        if (close < 0 || t.indexOf(')') != t.length() - 1) {
            return null;
        }
        String alt = t.substring(2, close);
        String src = t.substring(close + 2, t.length() - 1).trim();
        int space = src.indexOf(' ');
        if (space > 0) {
            src = src.substring(0, space);
        }
        if (!src.startsWith("data:")) {
            return null;
        }
        int comma = src.indexOf(',');
        int semi = src.indexOf(";base64");
        if (comma < 0 || semi < 0 || semi > comma) {
            return null;
        }
        byte[] data = Base64.decode(Ids.ascii(src.substring(comma + 1)));
        if (data == null || data.length == 0) {
            return null;
        }
        return new Picture(ids.next('i'), data, src.substring(5, semi), 144, 144, alt);
    }

    /// Parses emphasis, code spans, links and escapes.
    static RichText inline(String s, TextStyle base) {
        RichText.Builder b = new RichText.Builder();
        inline(s, 0, s.length(), base, b);
        return b.build();
    }

    private static void inline(String s, int from, int to, TextStyle style, RichText.Builder b) {
        StringBuilder plain = new StringBuilder();
        int i = from;
        while (i < to) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < to && isPunct(s.charAt(i + 1))) {
                plain.append(s.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c == '`') {
                int end = s.indexOf('`', i + 1);
                if (end > 0 && end < to) {
                    b.append(plain.toString(), style);
                    plain.setLength(0);
                    b.append(s.substring(i + 1, end), style.withFont("Monospace"));
                    i = end + 1;
                    continue;
                }
            }
            if ((c == '*' || c == '_') && i + 1 < to && s.charAt(i + 1) == c) {
                int end = s.indexOf("" + c + c, i + 2);
                if (end > i + 2 && end <= to - 2) {
                    b.append(plain.toString(), style);
                    plain.setLength(0);
                    inline(s, i + 2, end, style.withBold(true), b);
                    i = end + 2;
                    continue;
                }
            }
            if (c == '~' && i + 1 < to && s.charAt(i + 1) == '~') {
                int end = s.indexOf("~~", i + 2);
                if (end > i + 2 && end <= to - 2) {
                    b.append(plain.toString(), style);
                    plain.setLength(0);
                    inline(s, i + 2, end, style.withStrike(true), b);
                    i = end + 2;
                    continue;
                }
            }
            if ((c == '*' || c == '_') && i + 1 < to && s.charAt(i + 1) != ' ') {
                int end = findSingle(s, i + 1, to, c);
                if (end > i + 1) {
                    b.append(plain.toString(), style);
                    plain.setLength(0);
                    inline(s, i + 1, end, style.withItalic(true), b);
                    i = end + 1;
                    continue;
                }
            }
            if (c == '[' || c == '!' && i + 1 < to && s.charAt(i + 1) == '[') {
                int open = c == '!' ? i + 1 : i;
                int close = s.indexOf("](", open);
                int paren = close < 0 ? -1 : s.indexOf(')', close + 2);
                if (close > open && paren > 0 && paren < to) {
                    String label = s.substring(open + 1, close);
                    String href = s.substring(close + 2, paren).trim();
                    b.append(plain.toString(), style);
                    plain.setLength(0);
                    if (c == '!') {
                        b.append(label, style);
                    } else {
                        inline(label, 0, label.length(),
                                style.withLink(href).withUnderline(true).withColor(0x1a5fb4), b);
                    }
                    i = paren + 1;
                    continue;
                }
            }
            if (c == '<' && s.regionMatches(true, i, "<br", 0, 3)) {
                int end = s.indexOf('>', i);
                if (end > 0 && end < to) {
                    plain.append('\n');
                    i = end + 1;
                    continue;
                }
            }
            plain.append(c);
            i++;
        }
        b.append(plain.toString(), style);
    }

    private static int findSingle(String s, int from, int to, char c) {
        for (int i = from; i < to; i++) {
            char d = s.charAt(i);
            if (d == '\\') {
                i++;
            } else if (d == c && s.charAt(i - 1) != ' '
                    && (i + 1 >= to || s.charAt(i + 1) != c)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isPunct(char c) {
        return "\\`*_{}[]()#+-.!|~<>".indexOf(c) >= 0;
    }

    // ---------------------------------------------------------------- writing

    static String write(Document d) {
        StringBuilder b = new StringBuilder();
        List<Note> notes = new ArrayList<Note>();
        List<Block> blocks = d.blocks;
        boolean previousList = false;
        boolean previousCode = false;
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            Paragraph p = block instanceof Paragraph ? (Paragraph) block : null;
            boolean list = p != null && !Paragraph.NO_LIST.equals(p.list);
            boolean code = p != null && "Code".equals(p.style);
            if (previousCode && !code) {
                b.append("```\n");
            }
            if (b.length() > 0 && !(list && previousList) && !(code && previousCode)) {
                b.append('\n');
            }
            if (code) {
                if (!previousCode) {
                    b.append("```\n");
                }
                b.append(p.plainText()).append('\n');
            } else if (p != null) {
                paragraph(b, p, i, blocks, notes);
            } else if (block instanceof Table) {
                table(b, (Table) block, notes);
            } else if (block instanceof Picture) {
                picture(b, (Picture) block);
                b.append('\n');
            }
            previousList = list;
            previousCode = code;
        }
        if (previousCode) {
            b.append("```\n");
        }
        for (int n = 0; n < notes.size(); n++) {
            b.append(n == 0 ? "\n" : "").append("[^").append(n + 1).append("]: ")
                    .append(escape(notes.get(n).plainText().replace('\n', ' '))).append('\n');
        }
        return b.toString();
    }

    private static void paragraph(StringBuilder b, Paragraph p, int index, List<Block> blocks,
            List<Note> notes) {
        int h = p.headingLevel();
        TextStyle base = TextStyle.DEFAULT;
        if (h > 0) {
            for (int i = 0; i < h; i++) {
                b.append('#');
            }
            b.append(' ');
            base = base.withBold(true).withSize(HtmlFormat.headingSize(h));
        } else if ("Quote".equals(p.style)) {
            b.append("> ");
        } else if (!Paragraph.NO_LIST.equals(p.list)) {
            for (int i = 0; i < p.listLevel; i++) {
                b.append("  ");
            }
            if (Paragraph.NUMBERED.equals(p.list)) {
                b.append(listNumber(p, index, blocks)).append(". ");
            } else {
                b.append("- ");
            }
        }
        String text = runs(p.text, base, notes);
        // A line that would read as other syntax is escaped at its start.
        if (h == 0 && Paragraph.NO_LIST.equals(p.list) && text.length() > 0
                && (text.charAt(0) == '#' || text.charAt(0) == '>' || text.charAt(0) == '|'
                || listItem(text) != null)) {
            b.append('\\');
        }
        b.append(text.replace("\n", "  \n")).append('\n');
    }

    private static int listNumber(Paragraph p, int index, List<Block> blocks) {
        int n = 1;
        for (int i = index - 1; i >= 0; i--) {
            Block b = blocks.get(i);
            if (!(b instanceof Paragraph)) {
                break;
            }
            Paragraph q = (Paragraph) b;
            if (Paragraph.NO_LIST.equals(q.list) || q.listLevel < p.listLevel) {
                break;
            }
            if (q.listLevel == p.listLevel && Paragraph.NUMBERED.equals(q.list)) {
                n++;
                if (q.listStart > 0) {
                    return q.listStart + n - 1;
                }
            }
        }
        return p.listStart > 0 ? p.listStart : n;
    }

    private static String runs(RichText text, TextStyle base, List<Note> notes) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < text.runCount(); i++) {
            TextStyle s = text.runStyle(i);
            Inline object = text.runObject(i);
            if (s.revision != null && s.revision.isDeletion()) {
                continue;
            }
            if (object instanceof Picture || object instanceof EmbeddedComponent) {
                Picture pic = object instanceof Picture ? (Picture) object
                        : ((EmbeddedComponent) object).toPicture("c");
                if (pic != null) {
                    StringBuilder img = new StringBuilder();
                    picture(img, pic);
                    b.append(img);
                }
                continue;
            }
            if (object instanceof Note) {
                notes.add((Note) object);
                b.append("[^").append(notes.size()).append(']');
                continue;
            }
            String t = object instanceof Field ? ((Field) object).result : text.runText(i);
            if ("Monospace".equals(s.font) && !"Monospace".equals(base.font)
                    && t.indexOf('`') < 0) {
                b.append('`').append(t).append('`');
                continue;
            }
            String body = escape(t);
            String trimmed = body.trim();
            if (trimmed.length() == 0) {
                b.append(body);
                continue;
            }
            String lead = body.substring(0, body.indexOf(trimmed));
            String tail = body.substring(lead.length() + trimmed.length());
            String open = "";
            String close = "";
            if (s.bold && !base.bold) {
                open += "**";
                close = "**" + close;
            }
            if (s.italic) {
                open += "*";
                close = "*" + close;
            }
            if (s.strike) {
                open += "~~";
                close = "~~" + close;
            }
            String core = open + trimmed + close;
            if (s.link.length() > 0) {
                core = "[" + core + "](" + s.link.replace(" ", "%20").replace(")", "%29") + ")";
            }
            b.append(lead).append(core).append(tail);
        }
        return b.toString();
    }

    private static String escape(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '`' || c == '*' || c == '_' || c == '[' || c == ']'
                    || c == '~' || c == '|' || c == '<') {
                b.append('\\');
            }
            b.append(c);
        }
        return b.toString();
    }

    private static void table(StringBuilder b, Table t, List<Note> notes) {
        for (int r = 0; r < t.rows(); r++) {
            b.append('|');
            for (int c = 0; c < t.columns(); c++) {
                b.append(' ');
                if (!t.covered(r, c)) {
                    List<Block> cell = t.cell(r, c);
                    for (int k = 0; k < cell.size(); k++) {
                        if (k > 0) {
                            b.append("<br>");
                        }
                        Block inner = cell.get(k);
                        if (inner instanceof Paragraph) {
                            b.append(runs(((Paragraph) inner).text, r == 0
                                    ? TextStyle.DEFAULT.withBold(true) : TextStyle.DEFAULT, notes)
                                    .replace("\n", "<br>"));
                        } else {
                            StringBuilder plain = new StringBuilder();
                            Document.appendPlainText(plain, inner);
                            b.append(escape(plain.toString().replace('\n', ' ')));
                        }
                    }
                }
                b.append(" |");
            }
            b.append('\n');
            if (r == 0) {
                b.append('|');
                for (int c = 0; c < t.columns(); c++) {
                    String align = Paragraph.LEFT;
                    List<Block> cell = t.cell(Math.min(1, t.rows() - 1), c);
                    if (!cell.isEmpty() && cell.get(0) instanceof Paragraph) {
                        align = ((Paragraph) cell.get(0)).align;
                    }
                    b.append(Paragraph.CENTER.equals(align) ? " :---: |"
                            : Paragraph.RIGHT.equals(align) ? " ---: |" : " --- |");
                }
                b.append('\n');
            }
        }
    }

    private static void picture(StringBuilder b, Picture p) {
        b.append("![").append(escape(p.description)).append("](data:").append(p.mimeType)
                .append(";base64,").append(Base64.encodeNoNewline(p.data())).append(')');
    }
}
