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

import com.codename1.ui.html.HTMLParser;
import com.codename1.util.Base64;
import com.codename1.xml.Element;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/// HTML in both directions. Reading maps the DOM of the framework's tolerant `HTMLParser`
/// onto the model, so it accepts what browsers and office programs paste as well as what
/// `write` produces.
final class HtmlFormat {
    /// Bounds a hostile `colspan`/`rowspan`.
    private static final int MAX_ROWS = 1000;
    private static final int MAX_COLUMNS = 64;

    private HtmlFormat() {
    }

    // ---------------------------------------------------------------- reading

    /// The state of one walk over the DOM.
    private static final class Reader {
        final Ids ids = new Ids();
        final List<Block> out;
        int depth;
        RichText.Builder text = new RichText.Builder();
        String style = "Normal";
        String align = Paragraph.LEFT;
        String list = Paragraph.NO_LIST;
        int listLevel = -1;
        boolean preformatted;
        Reader parentIds;

        Reader(List<Block> out) {
            this.out = out;
        }

        Reader child(List<Block> into) {
            Reader r = new Reader(into);
            r.parentIds = this;
            return r;
        }

        String next(char prefix) {
            return parentIds != null ? parentIds.next(prefix) : ids.next(prefix);
        }

        void flush() {
            RichText t = text.build();
            if (t.isEmpty()) {
                // An empty element is spacing in HTML, and the item that holds a nested list
                // ends empty once the list is out.
                return;
            }
            if ("\n".equals(t.plainText())) {
                // `<p><br></p>`: a deliberately blank paragraph, as editors write one
                t = RichText.EMPTY;
            }
            out.add(new Paragraph(next('p'), style, align, list, Math.max(0, listLevel), 0,
                    0, 8, 0, 1.15f, false, false, t));
            text = new RichText.Builder();
        }
    }

    static Document read(String html) {
        HTMLParser parser = new HTMLParser(true);
        Element root = parser.parse(new StringReader("<div>" + html + "</div>"));
        List<Block> blocks = new ArrayList<Block>();
        Reader r = new Reader(blocks);
        String title = "";
        if (root != null) {
            title = findTitle(root);
            walkChildren(root, r, TextStyle.DEFAULT);
            r.flush();
        }
        return new Document(blocks, null, title, r.ids.peek());
    }

    private static String findTitle(Element e) {
        if (!e.isTextElement() && "title".equals(tag(e))) {
            return textOf(e).trim();
        }
        for (int i = 0; i < e.getNumChildren(); i++) {
            String t = findTitle(e.getChildAt(i));
            if (t.length() > 0) {
                return t;
            }
        }
        return "";
    }

    private static String textOf(Element e) {
        if (e.isTextElement()) {
            return e.getText();
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < e.getNumChildren(); i++) {
            b.append(textOf(e.getChildAt(i)));
        }
        return b.toString();
    }

    private static String tag(Element e) {
        return Ids.asciiLower(e.getTagName());
    }

    /// Deeper than this, markup is dropped: no real page nests that far, and each level is
    /// a frame on a device thread's small stack.
    private static final int MAX_DEPTH = 100;

    private static void walkChildren(Element e, Reader r, TextStyle style) {
        if (r.depth >= MAX_DEPTH) {
            return;
        }
        r.depth++;
        try {
            for (int i = 0; i < e.getNumChildren(); i++) {
                walk(e.getChildAt(i), r, style);
            }
        } finally {
            r.depth--;
        }
    }

    private static boolean isBlock(String t) {
        return "p".equals(t) || "div".equals(t) || "li".equals(t) || "blockquote".equals(t)
                || "pre".equals(t) || "section".equals(t) || "article".equals(t)
                || (t.length() == 2 && t.charAt(0) == 'h' && t.charAt(1) >= '1'
                && t.charAt(1) <= '6');
    }

    private static void walk(Element e, Reader r, TextStyle style) {
        if (e.isTextElement()) {
            String s = e.getText();
            if (!r.preformatted) {
                s = collapse(s, r.text.length() == 0);
            }
            r.text.append(s, style);
            return;
        }
        String t = tag(e);
        if ("head".equals(t) || "script".equals(t) || "style".equals(t) || "title".equals(t)) {
            return;
        }
        if ("br".equals(t)) {
            r.text.append("\n", style);
            return;
        }
        if ("img".equals(t)) {
            EmbeddedComponent c = component(e, r);
            if (c != null) {
                r.text.append(c, style);
                return;
            }
            Picture p = picture(e, r);
            if (p != null) {
                r.flush();
                r.out.add(p);
            } else {
                String alt = e.getAttribute("alt");
                if (alt != null) {
                    r.text.append(alt, style);
                }
            }
            return;
        }
        if ("table".equals(t)) {
            r.flush();
            Table table = table(e, r);
            if (table != null) {
                r.out.add(table);
            }
            return;
        }
        if ("ul".equals(t) || "ol".equals(t)) {
            r.flush();
            String savedList = r.list;
            int savedLevel = r.listLevel;
            r.list = "ol".equals(t) ? Paragraph.NUMBERED : Paragraph.BULLET;
            r.listLevel++;
            walkChildren(e, r, style);
            r.flush();
            r.list = savedList;
            r.listLevel = savedLevel;
            return;
        }
        if (isBlock(t)) {
            r.flush();
            String savedStyle = r.style;
            String savedAlign = r.align;
            boolean savedPre = r.preformatted;
            if (t.charAt(0) == 'h' && t.length() == 2) {
                r.style = "Heading" + t.charAt(1);
            } else if ("blockquote".equals(t)) {
                r.style = "Quote";
            } else if ("pre".equals(t)) {
                r.style = "Code";
                r.preformatted = true;
            }
            String a = cssValue(e.getAttribute("style"), "text-align");
            if (a == null) {
                a = e.getAttribute("align");
            }
            if (a != null) {
                a = Ids.asciiLower(a.trim());
                if (Paragraph.CENTER.equals(a) || Paragraph.RIGHT.equals(a)
                        || Paragraph.JUSTIFY.equals(a)) {
                    r.align = a;
                }
            }
            TextStyle inner = r.preformatted ? style.withFont("Monospace") : style;
            if (r.style.startsWith("Heading")) {
                inner = inner.withBold(true).withSize(headingSize(r.style.charAt(7) - '0'));
            }
            walkChildren(e, r, inline(e, t, inner));
            r.flush();
            r.style = savedStyle;
            r.align = savedAlign;
            r.preformatted = savedPre;
            return;
        }
        walkChildren(e, r, inline(e, t, style));
    }

    /// The point size `write` gives each heading level, and `read` assumes.
    static float headingSize(int level) {
        switch (level) {
            case 1:
                return 24;
            case 2:
                return 18;
            case 3:
                return 15;
            case 4:
                return 13;
            default:
                return 12;
        }
    }

    private static String collapse(String s, boolean atStart) {
        StringBuilder b = new StringBuilder(s.length());
        boolean space = atStart;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                if (!space) {
                    b.append(' ');
                    space = true;
                }
            } else {
                b.append(c);
                space = false;
            }
        }
        return b.toString();
    }

    private static TextStyle inline(Element e, String t, TextStyle s) {
        if ("b".equals(t) || "strong".equals(t) || "th".equals(t)) {
            s = s.withBold(true);
        } else if ("i".equals(t) || "em".equals(t) || "cite".equals(t)) {
            s = s.withItalic(true);
        } else if ("u".equals(t) || "ins".equals(t)) {
            s = s.withUnderline(true);
        } else if ("s".equals(t) || "strike".equals(t) || "del".equals(t)) {
            s = s.withStrike(true);
        } else if ("sup".equals(t)) {
            s = s.withScript(TextStyle.SUPERSCRIPT);
        } else if ("sub".equals(t)) {
            s = s.withScript(TextStyle.SUBSCRIPT);
        } else if ("code".equals(t) || "tt".equals(t) || "kbd".equals(t)) {
            s = s.withFont("Monospace");
        } else if ("mark".equals(t)) {
            s = s.withBackground(0xffff00);
        } else if ("a".equals(t)) {
            String href = e.getAttribute("href");
            if (href != null && href.length() > 0) {
                s = s.withLink(href).withUnderline(true).withColor(0x1a5fb4);
            }
        }
        String css = e.getAttribute("style");
        if (css != null) {
            String v = cssValue(css, "font-weight");
            if (v != null) {
                v = Ids.asciiLower(v);
                s = s.withBold("bold".equals(v) || "bolder".equals(v) || v.startsWith("6")
                        || v.startsWith("7") || v.startsWith("8") || v.startsWith("9"));
            }
            v = cssValue(css, "font-style");
            if (v != null) {
                s = s.withItalic("italic".equalsIgnoreCase(v) || "oblique".equalsIgnoreCase(v));
            }
            v = cssValue(css, "text-decoration");
            if (v != null) {
                String lv = Ids.asciiLower(v);
                if (lv.indexOf("underline") >= 0) {
                    s = s.withUnderline(true);
                }
                if (lv.indexOf("line-through") >= 0) {
                    s = s.withStrike(true);
                }
            }
            v = cssValue(css, "color");
            int c = v == null ? -1 : color(v);
            if (c >= 0) {
                s = s.withColor(c);
            }
            v = cssValue(css, "background-color");
            c = v == null ? -1 : color(v);
            if (c >= 0) {
                s = s.withBackground(c);
            }
            v = cssValue(css, "font-size");
            float size = v == null ? -1 : points(v, s.size);
            if (size > 0 && size < 1638) {
                s = s.withSize(size);
            }
            v = cssValue(css, "font-family");
            if (v != null) {
                String family = v;
                int comma = family.indexOf(',');
                if (comma >= 0) {
                    family = family.substring(0, comma);
                }
                family = family.trim();
                if (family.length() > 1 && (family.charAt(0) == '"' || family.charAt(0) == '\'')) {
                    family = family.substring(1, family.length() - 1);
                }
                if (family.length() > 0) {
                    s = s.withFont(family);
                }
            }
        }
        return s;
    }

    /// The value of one CSS property in a `style` attribute.
    static String cssValue(String css, String property) {
        if (css == null) {
            return null;
        }
        int pos = 0;
        while (pos < css.length()) {
            int semi = css.indexOf(';', pos);
            if (semi < 0) {
                semi = css.length();
            }
            int colon = css.indexOf(':', pos);
            if (colon > pos && colon < semi
                    && css.substring(pos, colon).trim().equalsIgnoreCase(property)) {
                String v = css.substring(colon + 1, semi).trim();
                int imp = v.indexOf('!');
                return imp >= 0 ? v.substring(0, imp).trim() : v;
            }
            pos = semi + 1;
        }
        return null;
    }

    /// A CSS color as RGB, or -1 when it is not one this reader knows.
    static int color(String v) {
        String s = Ids.asciiLower(v.trim());
        if (s.startsWith("#")) {
            String h = s.substring(1);
            if (h.length() == 3) {
                h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2)
                        + h.charAt(2);
            }
            if (h.length() != 6) {
                return -1;
            }
            try {
                return Integer.parseInt(h, 16);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        if (s.startsWith("rgb")) {
            int open = s.indexOf('(');
            int close = s.indexOf(')');
            if (open < 0 || close < open) {
                return -1;
            }
            String[] parts = split(s.substring(open + 1, close));
            if (parts.length < 3) {
                return -1;
            }
            try {
                int rgb = 0;
                for (int i = 0; i < 3; i++) {
                    int c = Math.max(0, Math.min(255, (int) Double.parseDouble(parts[i].trim())));
                    rgb = rgb << 8 | c;
                }
                return rgb;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        String[] names = {"black", "white", "red", "green", "blue", "yellow", "gray", "grey",
            "orange", "purple", "navy", "maroon", "teal", "silver"};
        int[] values = {0, 0xffffff, 0xff0000, 0x008000, 0x0000ff, 0xffff00, 0x808080, 0x808080,
            0xffa500, 0x800080, 0x000080, 0x800000, 0x008080, 0xc0c0c0};
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(s)) {
                return values[i];
            }
        }
        return -1;
    }

    private static String[] split(String s) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == ',') {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    /// A CSS length in points, or -1.
    static float points(String v, float em) {
        String s = Ids.asciiLower(v.trim());
        float factor = 1;
        int cut = s.length();
        if (s.endsWith("pt")) {
            cut -= 2;
        } else if (s.endsWith("px")) {
            factor = 0.75f;
            cut -= 2;
        } else if (s.endsWith("em")) {
            factor = em;
            cut -= 2;
        } else if (s.endsWith("%")) {
            factor = em / 100f;
            cut -= 1;
        } else if (s.endsWith("in")) {
            factor = 72;
            cut -= 2;
        }
        try {
            return Float.parseFloat(s.substring(0, cut).trim()) * factor;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static Picture picture(Element e, Reader r) {
        String src = e.getAttribute("src");
        if (src == null || !src.startsWith("data:")) {
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
        String mime = src.substring(5, semi);
        float w = dimension(e, "width");
        float h = dimension(e, "height");
        if (w <= 0) {
            w = h > 0 ? h : 144;
        }
        if (h <= 0) {
            h = w;
        }
        return new Picture(r.next('i'), data, mime, w, h, e.getAttribute("alt"));
    }

    /// An image that stands for an embedded component: one with `data-cn1-component`, or a
    /// `cn1-component:` source as a `RichTextArea` gives it back.
    private static EmbeddedComponent component(Element e, Reader r) {
        String src = e.getAttribute("src");
        EmbeddedComponent fromSource = EmbeddedComponents.fromSource(src);
        String type = e.getAttribute("data-cn1-component");
        if (type == null || type.length() == 0) {
            return fromSource;
        }
        String state = e.getAttribute("data-cn1-state");
        Picture snap = picture(e, r);
        float w = dimension(e, "width");
        float h = dimension(e, "height");
        if (w <= 0 || h <= 0) {
            w = fromSource != null ? fromSource.width : snap != null ? snap.width : 144;
            h = fromSource != null ? fromSource.height : snap != null ? snap.height : 72;
        }
        return new EmbeddedComponent(type, state == null ? "" : state, w, h,
                snap == null ? null : snap.data());
    }

    private static float dimension(Element e, String name) {
        String v = cssValue(e.getAttribute("style"), name);
        if (v == null) {
            v = e.getAttribute(name);
            if (v != null && v.trim().length() > 0 && Character.isDigit(v.trim().charAt(
                    v.trim().length() - 1))) {
                v = v.trim() + "px";
            }
        }
        return v == null ? -1 : points(v, 12);
    }

    private static Table table(Element e, Reader r) {
        List<Element> rows = new ArrayList<Element>();
        List<Boolean> header = new ArrayList<Boolean>();
        collectRows(e, rows, header, false);
        if (rows.isEmpty()) {
            return null;
        }
        // Lay the cells onto a grid, honoring spans.
        int nRows = Math.min(MAX_ROWS, rows.size());
        List<int[]> spans = new ArrayList<int[]>();
        List<Element> cellElements = new ArrayList<Element>();
        boolean[][] taken = new boolean[nRows][MAX_COLUMNS];
        int cols = 0;
        for (int ri = 0; ri < nRows; ri++) {
            Element row = rows.get(ri);
            int c = 0;
            for (int i = 0; i < row.getNumChildren(); i++) {
                Element cell = row.getChildAt(i);
                if (cell.isTextElement()) {
                    continue;
                }
                String ct = tag(cell);
                if (!"td".equals(ct) && !"th".equals(ct)) {
                    continue;
                }
                while (c < MAX_COLUMNS && taken[ri][c]) {
                    c++;
                }
                if (c >= MAX_COLUMNS) {
                    break;
                }
                int rs = Math.max(1, Math.min(nRows - ri, cell.getAttributeAsInt("rowspan", 1)));
                int cs = Math.max(1, Math.min(MAX_COLUMNS - c, cell.getAttributeAsInt("colspan",
                        1)));
                for (int y = ri; y < ri + rs; y++) {
                    for (int x = c; x < c + cs; x++) {
                        taken[y][x] = true;
                    }
                }
                spans.add(new int[] {ri, c, rs, cs});
                cellElements.add(cell);
                cols = Math.max(cols, c + cs);
                c += cs;
            }
        }
        if (cols == 0) {
            return null;
        }
        List<List<List<Block>>> grid = new ArrayList<List<List<Block>>>();
        for (int y = 0; y < nRows; y++) {
            List<List<Block>> row = new ArrayList<List<Block>>();
            for (int x = 0; x < cols; x++) {
                row.add(new ArrayList<Block>());
            }
            grid.add(row);
        }
        List<Table.Merge> merges = new ArrayList<Table.Merge>();
        for (int i = 0; i < spans.size(); i++) {
            int[] s = spans.get(i);
            List<Block> content = new ArrayList<Block>();
            Reader cellReader = r.child(content);
            TextStyle base = "th".equals(tag(cellElements.get(i)))
                    ? TextStyle.DEFAULT.withBold(true) : TextStyle.DEFAULT;
            walkChildren(cellElements.get(i), cellReader, base);
            cellReader.flush();
            grid.get(s[0]).set(s[1], content);
            if (s[2] * s[3] > 1) {
                merges.add(new Table.Merge(s[0], s[1], s[2], s[3]));
            }
        }
        int headerRows = 0;
        while (headerRows < nRows && header.get(headerRows).booleanValue()) {
            headerRows++;
        }
        return new Table(r.next('t'), grid, merges, headerRows);
    }

    private static void collectRows(Element e, List<Element> rows, List<Boolean> header,
            boolean inHead) {
        for (int i = 0; i < e.getNumChildren(); i++) {
            Element c = e.getChildAt(i);
            if (c.isTextElement()) {
                continue;
            }
            String t = tag(c);
            if ("tr".equals(t)) {
                rows.add(c);
                header.add(Boolean.valueOf(inHead));
            } else if ("thead".equals(t)) {
                collectRows(c, rows, header, true);
            } else if ("tbody".equals(t) || "tfoot".equals(t)) {
                collectRows(c, rows, header, false);
            }
        }
    }

    // ---------------------------------------------------------------- writing

    /// Notes collected while writing, and how embedded components are written.
    private static final class Notes extends ArrayList<Note> {
        private static final long serialVersionUID = 1L;
        final boolean componentSources;

        Notes(boolean componentSources) {
            this.componentSources = componentSources;
        }
    }

    static String write(Document d) {
        return write(d, false);
    }

    /// Writes HTML; with `componentSources` an embedded component's image names it by a
    /// `cn1-component:` source, which a `RichTextArea` keeps, instead of showing its snapshot.
    static String write(Document d, boolean componentSources) {
        StringBuilder b = new StringBuilder();
        List<Note> notes = new Notes(componentSources);
        b.append("<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\"><title>");
        escape(b, d.title);
        b.append("</title></head>\n<body>\n");
        blocks(b, d.blocks, notes);
        for (int n = 0; n < notes.size(); n++) {
            if (n == 0) {
                b.append("<hr>\n<ol class=\"notes\">\n");
            }
            Note note = notes.get(n);
            b.append("<li id=\"note-").append(n + 1).append("\">");
            List<Note> inner = new Notes(componentSources);
            blocks(b, note.blocks, inner);
            b.append("</li>\n");
            if (n == notes.size() - 1) {
                b.append("</ol>\n");
            }
        }
        b.append("</body></html>\n");
        return b.toString();
    }

    private static void blocks(StringBuilder b, List<Block> blocks, List<Note> notes) {
        // Open lists, innermost last: "ul" or "ol". Every open list also has an open `li`, so a
        // deeper list nests inside the item before it, as HTML requires.
        List<String> open = new ArrayList<String>();
        for (Block block : blocks) {
            Paragraph p = block instanceof Paragraph ? (Paragraph) block : null;
            int want = p != null && !Paragraph.NO_LIST.equals(p.list) ? p.listLevel + 1 : 0;
            String kind = p != null && Paragraph.NUMBERED.equals(p.list) ? "ol" : "ul";
            while (open.size() > want
                    || open.size() == want && want > 0 && !open.get(want - 1).equals(kind)) {
                b.append("</li></").append(open.remove(open.size() - 1)).append(">\n");
            }
            if (want > 0 && open.size() == want) {
                b.append("</li>\n");
            }
            while (open.size() < want) {
                if (!open.isEmpty() && open.size() < want - 1) {
                    b.append("<li>");
                }
                String k = open.size() == want - 1 ? kind : "ul";
                open.add(k);
                b.append('<').append(k).append(">\n");
                if (open.size() < want) {
                    b.append("<li>");
                }
            }
            if (p != null) {
                paragraph(b, p, want > 0, notes);
            } else if (block instanceof Table) {
                table(b, (Table) block, notes);
            } else if (block instanceof Picture) {
                b.append("<p>");
                picture(b, (Picture) block);
                b.append("</p>\n");
            }
        }
        while (!open.isEmpty()) {
            b.append("</li></").append(open.remove(open.size() - 1)).append(">\n");
        }
    }

    private static void paragraph(StringBuilder b, Paragraph p, boolean item,
            List<Note> notes) {
        int h = p.headingLevel();
        String t = item ? "li" : h > 0 ? "h" + h : "Quote".equals(p.style) ? "blockquote"
                : "Code".equals(p.style) ? "pre" : "p";
        b.append('<').append(t);
        if (!Paragraph.LEFT.equals(p.align)) {
            b.append(" style=\"text-align:").append(p.align).append('"');
        }
        b.append('>');
        if (p.text.isEmpty()) {
            b.append("<br>");
        }
        runs(b, p.text, h > 0 ? TextStyle.DEFAULT.withBold(true).withSize(headingSize(h))
                : "Code".equals(p.style) ? TextStyle.DEFAULT.withFont("Monospace")
                : TextStyle.DEFAULT, notes);
        if (!item) {
            b.append("</").append(t).append(">\n");
        }
    }

    private static void runs(StringBuilder b, RichText text, TextStyle base,
            List<Note> notes) {
        for (int i = 0; i < text.runCount(); i++) {
            TextStyle s = text.runStyle(i);
            Inline object = text.runObject(i);
            if (object instanceof Picture) {
                picture(b, (Picture) object);
                continue;
            }
            if (object instanceof EmbeddedComponent) {
                component(b, (EmbeddedComponent) object,
                        notes instanceof Notes && ((Notes) notes).componentSources);
                continue;
            }
            if (object instanceof Note) {
                Note note = (Note) object;
                notes.add(note);
                String mark = note.mark.length() > 0 ? note.mark : String.valueOf(notes.size());
                b.append("<sup><a href=\"#note-").append(notes.size()).append("\">");
                escape(b, mark);
                b.append("</a></sup>");
                continue;
            }
            List<String> close = new ArrayList<String>();
            if (s.revision != null) {
                String tag = s.revision.isDeletion() ? "del" : "ins";
                b.append('<').append(tag);
                if (s.revision.author.length() > 0) {
                    b.append(" title=\"");
                    escape(b, s.revision.author);
                    b.append('"');
                }
                b.append('>');
                close.add(tag);
            }
            if (s.link.length() > 0) {
                b.append("<a href=\"");
                escape(b, s.link);
                b.append("\">");
                close.add("a");
            }
            StringBuilder css = new StringBuilder();
            if (!s.font.equals(base.font)) {
                css.append("font-family:'").append(s.font.replace('\'', ' ')).append("';");
            }
            if (Float.floatToIntBits(s.size) != Float.floatToIntBits(base.size)) {
                css.append("font-size:").append(Ids.fmt(s.size)).append("pt;");
            }
            if (s.color != TextStyle.DEFAULT.color && (s.link.length() == 0
                    || s.color != 0x1a5fb4)) {
                css.append("color:").append(hex(s.color)).append(';');
            }
            if (s.background != TextStyle.NO_BACKGROUND) {
                css.append("background-color:").append(hex(s.background)).append(';');
            }
            if (css.length() > 0) {
                b.append("<span style=\"").append(css).append("\">");
                close.add("span");
            }
            if (s.bold && !base.bold) {
                b.append("<b>");
                close.add("b");
            }
            if (s.italic) {
                b.append("<i>");
                close.add("i");
            }
            if (s.underline && s.link.length() == 0) {
                b.append("<u>");
                close.add("u");
            }
            if (s.strike) {
                b.append("<s>");
                close.add("s");
            }
            if (TextStyle.SUPERSCRIPT.equals(s.script)) {
                b.append("<sup>");
                close.add("sup");
            } else if (TextStyle.SUBSCRIPT.equals(s.script)) {
                b.append("<sub>");
                close.add("sub");
            }
            String run = object instanceof Field ? ((Field) object).result : text.runText(i);
            int start = 0;
            for (int k = 0; k <= run.length(); k++) {
                if (k == run.length() || run.charAt(k) == '\n') {
                    escape(b, run.substring(start, k));
                    if (k < run.length()) {
                        b.append("<br>");
                    }
                    start = k + 1;
                }
            }
            for (int k = close.size() - 1; k >= 0; k--) {
                b.append("</").append(close.get(k)).append('>');
            }
        }
    }

    private static String hex(int rgb) {
        String h = Integer.toHexString(0x1000000 | rgb & 0xffffff);
        return "#" + h.substring(1);
    }

    private static void table(StringBuilder b, Table t, List<Note> notes) {
        b.append("<table border=\"1\" style=\"border-collapse:collapse\">\n");
        for (int r = 0; r < t.rows(); r++) {
            if (r == 0 && t.headerRows > 0) {
                b.append("<thead>\n");
            }
            b.append("<tr>");
            String cellTag = r < t.headerRows ? "th" : "td";
            for (int c = 0; c < t.columns(); c++) {
                if (t.covered(r, c)) {
                    continue;
                }
                b.append('<').append(cellTag);
                Table.Merge m = t.span(r, c);
                if (m != null && m.rows > 1) {
                    b.append(" rowspan=\"").append(m.rows).append('"');
                }
                if (m != null && m.columns > 1) {
                    b.append(" colspan=\"").append(m.columns).append('"');
                }
                b.append('>');
                List<Block> cell = t.cell(r, c);
                if (cell.size() == 1 && cell.get(0) instanceof Paragraph
                        && Paragraph.NO_LIST.equals(((Paragraph) cell.get(0)).list)
                        && !((Paragraph) cell.get(0)).isHeading()) {
                    runs(b, ((Paragraph) cell.get(0)).text, "th".equals(cellTag)
                            ? TextStyle.DEFAULT.withBold(true) : TextStyle.DEFAULT, notes);
                } else {
                    blocks(b, cell, notes);
                }
                b.append("</").append(cellTag).append('>');
            }
            b.append("</tr>\n");
            if (r == t.headerRows - 1) {
                b.append("</thead>\n");
            }
        }
        b.append("</table>\n");
    }

    private static void component(StringBuilder b, EmbeddedComponent c, boolean source) {
        byte[] snap = c.snapshot();
        b.append("<img src=\"");
        if (source || snap == null) {
            escape(b, EmbeddedComponents.toSource(c));
        } else {
            b.append("data:image/png;base64,").append(Base64.encodeNoNewline(snap));
        }
        b.append("\" data-cn1-component=\"");
        escape(b, c.type);
        b.append("\" data-cn1-state=\"");
        escape(b, c.state);
        b.append("\" style=\"width:").append(Ids.fmt(c.width)).append("pt;height:")
                .append(Ids.fmt(c.height)).append("pt\" alt=\"\">");
    }

    private static void picture(StringBuilder b, Picture p) {
        b.append("<img src=\"data:").append(p.mimeType).append(";base64,")
                .append(Base64.encodeNoNewline(p.data())).append("\" style=\"width:")
                .append(Ids.fmt(p.width)).append("pt;height:").append(Ids.fmt(p.height))
                .append("pt\" alt=\"");
        escape(b, p.description);
        b.append("\">");
    }

    static void escape(StringBuilder b, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    b.append("&amp;");
                    break;
                case '<':
                    b.append("&lt;");
                    break;
                case '>':
                    b.append("&gt;");
                    break;
                case '"':
                    b.append("&quot;");
                    break;
                default:
                    b.append(c);
                    break;
            }
        }
    }
}
