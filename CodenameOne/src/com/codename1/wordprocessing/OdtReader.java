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

import com.codename1.util.StringUtil;
import com.codename1.impl.xml.XmlScanner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Reads OpenDocument text (`.odt`), the format of LibreOffice Writer.
///
/// Reads paragraph and text styles (named and automatic), headings, nested lists with their
/// start values, tables with spans, framed images, notes, annotations, tracked insertions and
/// deletions, and the common fields. Sections and frames of text are read as their content.
final class OdtReader {
    private static final int MAX_COLUMNS = 1024;

    private static final int MAX_ROWS = 32767;

    private final Map<String, byte[]> parts;
    private final long[] budget = {XmlScanner.MAX_DOCUMENT};
    private final Ids ids = new Ids();
    /// Cells in all tables of the file together, repeats expanded. A repeat count turns a few
    /// bytes into a row or column of copies, so the totals are what bound memory.
    private static final int MAX_CELLS = 200000;
    private int cells;
    private final Map<String, Style> styles = new HashMap<String, Style>();
    private final Map<String, String[]> listStyles = new HashMap<String, String[]>();
    private final Map<String, Revision> changes = new HashMap<String, Revision>();
    private final Map<String, RichText> deletions = new HashMap<String, RichText>();
    private final Map<String, Comment> openComments = new HashMap<String, Comment>();
    private final List<Comment> comments = new ArrayList<Comment>();
    private String masterLayout;
    private final Map<String, String> masterPages = new HashMap<String, String>();
    private final Map<String, PageSpec> layouts = new HashMap<String, PageSpec>();
    private boolean trackChanges;
    /// Header and footer blocks: default header, first header, default footer, first footer.
    @SuppressWarnings("unchecked")
    private final List<Block>[] furniture = new List[4];
    private boolean inBody;
    private boolean bodyStarted;
    private PageSpec firstPage;
    private PageSpec currentPage;
    private int noteCounter;
    private int commentCounter;

    private OdtReader(Map<String, byte[]> parts) {
        this.parts = parts;
    }

    static Document read(InputStream in) throws IOException {
        return new OdtReader(ZipParts.read(in, "OpenDocument text")).read();
    }

    /// A style of either family; every field optional so a chain can be overlaid.
    static final class Style {
        String name;
        String display;
        String parent;
        String listStyle;
        Integer outline;
        String master;
        String wrap;
        String horizontal;
        float distance = -1;
        int columns;
        float columnGap = 36;
        float columnWidth;
        final DocxReader.Props run = new DocxReader.Props();
        final DocxReader.Para para = new DocxReader.Para();
    }

    private XmlScanner scanner(String part) throws IOException {
        byte[] b = parts.get(part);
        return b == null ? null : new XmlScanner(new ByteArrayInputStream(b), budget);
    }

    private Document read() throws IOException {
        byte[] mime = parts.get("mimetype");
        if (mime != null) {
            String m = StringUtil.newString(mime).trim();
            if (!m.startsWith("application/vnd.oasis.opendocument.text")) {
                throw new IOException("Not an OpenDocument text file: " + m);
            }
        }
        if (!parts.containsKey("content.xml")) {
            throw new IOException("Not an OpenDocument text file: it has no content.xml");
        }
        XmlScanner s = scanner("styles.xml");
        if (s != null) {
            readStyles(s);
        }
        String title = readTitle();
        readSettings();
        XmlScanner x = scanner("content.xml");
        List<Block> blocks = new ArrayList<Block>();
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("automatic-styles".equals(n)) {
                readStyleChildren(x);
            } else if ("tracked-changes".equals(n)) {
                trackChanges = !"false".equals(x.attr("track-changes"));
                readChanges(x);
            } else if ("text".equals(n)) {
                inBody = true;
                content(x, blocks, null);
                inBody = false;
            } else if ("font-face-decls".equals(n) || "scripts".equals(n)) {
                x.skipElement();
            }
        }
        PageSpec page = firstPage;
        String layout = masterLayout;
        if (page == null && layout != null) {
            page = layouts.get(layout);
        }
        Document d = new Document(blocks, page, title, ids.peek(), comments, trackChanges);
        if (furniture[0] != null || furniture[1] != null || furniture[2] != null
                || furniture[3] != null) {
            d = d.withHeaders(furniture[0], furniture[2], furniture[1], furniture[3]);
        }
        return d.withNextId(Math.max(d.nextId, ids.peek()));
    }

    private PageSpec masterPage(String master) {
        String layout = master == null ? null : masterPages.get(master);
        return layout == null ? null : layouts.get(layout);
    }

    private PageSpec pageNow() {
        if (currentPage != null) {
            return currentPage;
        }
        PageSpec p = masterLayout == null ? null : layouts.get(masterLayout);
        return p == null ? PageSpec.A4 : p;
    }

    private String readTitle() throws IOException {
        XmlScanner x = scanner("meta.xml");
        if (x == null) {
            return null;
        }
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START && "title".equals(x.name)) {
                return x.elementText().trim();
            }
        }
        return null;
    }

    private void readSettings() throws IOException {
        XmlScanner x = scanner("settings.xml");
        if (x == null) {
            return;
        }
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START && "config-item".equals(x.name)
                    && "RecordChanges".equals(x.attr("name"))) {
                trackChanges = "true".equals(x.elementText().trim());
            }
        }
    }

    // ------------------------------------------------------------------ styles

    private void readStyles(XmlScanner x) throws IOException {
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("styles".equals(n) || "automatic-styles".equals(n)) {
                readStyleChildren(x);
            } else if ("master-styles".equals(n)) {
                for (int f = x.next(); f != XmlScanner.END && f != XmlScanner.EOF; f = x.next()) {
                    if (f == XmlScanner.START) {
                        if ("master-page".equals(x.name)) {
                            String layout = x.attr("page-layout-name");
                            boolean standard = masterLayout == null
                                    || "Standard".equals(x.attr("name"));
                            masterPages.put(x.attr("name"), layout);
                            if (standard) {
                                masterLayout = layout;
                            }
                            masterFurniture(x, standard);
                        } else {
                            x.skipElement();
                        }
                    }
                }
            }
        }
    }

    /// Reads a master page's header and footer; the model keeps the standard page's.
    private void masterFurniture(XmlScanner x, boolean keep) throws IOException {
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The OpenDocument file is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            int slot = "header".equals(n) ? 0 : "header-first".equals(n) ? 1
                    : "footer".equals(n) ? 2 : "footer-first".equals(n) ? 3 : -1;
            if (slot < 0 || !keep || "false".equals(x.attr("display"))) {
                x.skipElement();
                continue;
            }
            List<Block> b = new ArrayList<Block>();
            content(x, b, null);
            furniture[slot] = b;
        }
    }

    private void readStyleChildren(XmlScanner x) throws IOException {
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The OpenDocument file is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("style".equals(n) || "default-style".equals(n)) {
                Style s = new Style();
                s.name = "default-style".equals(n) ? "!default-" + x.attr("family")
                        : x.attr("name");
                s.display = x.attr("display-name");
                s.parent = x.attr("parent-style-name");
                if (s.parent == null && !"default-style".equals(n)) {
                    s.parent = "!default-" + x.attr("family");
                }
                s.listStyle = x.attr("list-style-name");
                String outline = x.attr("default-outline-level");
                if (outline != null) {
                    try {
                        s.outline = Integer.valueOf(Integer.parseInt(outline.trim()));
                    } catch (NumberFormatException ignored) {
                        // not a level
                    }
                }
                String master = x.attr("master-page-name");
                if (master != null && master.length() > 0) {
                    s.master = master;
                }
                for (int f = x.next(); f != XmlScanner.END; f = x.next()) {
                    if (f == XmlScanner.EOF) {
                        throw new IOException("The OpenDocument file is truncated");
                    }
                    if (f != XmlScanner.START) {
                        continue;
                    }
                    String c = x.name;
                    if ("text-properties".equals(c)) {
                        textProps(x, s.run);
                    } else if ("paragraph-properties".equals(c)) {
                        paraProps(x, s.para);
                        s.para.tabs = tabStops(x);
                        continue;
                    } else if ("graphic-properties".equals(c)) {
                        String wrap = x.attr("wrap");
                        String run = x.attr("run-through");
                        s.wrap = "parallel".equals(wrap) || "dynamic".equals(wrap)
                                || "left".equals(wrap) || "right".equals(wrap)
                                || "biggest".equals(wrap) ? Picture.SQUARE
                                : "none".equals(wrap) ? Picture.TOP_BOTTOM
                                : "background".equals(run) ? Picture.BEHIND : Picture.IN_FRONT;
                        String h = x.attr("horizontal-pos");
                        s.horizontal = "right".equals(h) || "outside".equals(h) ? Picture.RIGHT
                                : "center".equals(h) ? Picture.CENTER : Picture.LEFT;
                        s.distance = Math.max(length(x.attr("margin-left"), 0),
                                length(x.attr("margin-right"), 0));
                    } else if ("section-properties".equals(c)) {
                        s.columns = columnsOf(x, s);
                        continue;
                    } else if ("table-column-properties".equals(c)) {
                        s.columnWidth = length(x.attr("column-width"), 0);
                    }
                    x.skipElement();
                }
                if (s.name != null) {
                    styles.put(s.name, s);
                }
            } else if ("list-style".equals(n)) {
                String name = x.attr("name");
                String[] kinds = new String[10];
                for (int f = x.next(); f != XmlScanner.END; f = x.next()) {
                    if (f == XmlScanner.EOF) {
                        throw new IOException("The OpenDocument file is truncated");
                    }
                    if (f != XmlScanner.START) {
                        continue;
                    }
                    int level = x.intAttr("level", 0);
                    if (level >= 1 && level <= 10) {
                        if ("list-level-style-bullet".equals(x.name)) {
                            kinds[level - 1] = Paragraph.BULLET;
                        } else if ("list-level-style-number".equals(x.name)) {
                            String fmt = x.attr("num-format");
                            kinds[level - 1] = fmt == null || fmt.length() == 0
                                    ? Paragraph.NO_LIST : Paragraph.NUMBERED;
                        }
                    }
                    x.skipElement();
                }
                if (name != null) {
                    listStyles.put(name, kinds);
                }
            } else if ("page-layout".equals(n)) {
                String name = x.attr("name");
                for (int f = x.next(); f != XmlScanner.END; f = x.next()) {
                    if (f == XmlScanner.EOF) {
                        throw new IOException("The OpenDocument file is truncated");
                    }
                    if (f != XmlScanner.START) {
                        continue;
                    }
                    if ("page-layout-properties".equals(x.name) && name != null) {
                        float w = length(x.attr("page-width"), 595.28f);
                        float h = length(x.attr("page-height"), 841.89f);
                        PageSpec spec = null;
                        try {
                            spec = new PageSpec(w, h,
                                    length(x.attr("margin-top"), 72),
                                    length(x.attr("margin-right"), 72),
                                    length(x.attr("margin-bottom"), 72),
                                    length(x.attr("margin-left"), 72));
                        } catch (IllegalArgumentException ignored) {
                            // a layout with no room: keep the default
                        }
                        Style cols = new Style();
                        int count = columnsOf(x, cols);
                        if (spec != null) {
                            layouts.put(name, spec.withColumns(count, cols.columnGap));
                        }
                        continue;
                    }
                    x.skipElement();
                }
            } else {
                x.skipElement();
            }
        }
    }

    /// The tab stops among a paragraph-properties element's children, read to its end.
    private static List<TabStop> tabStops(XmlScanner x) throws IOException {
        List<TabStop> out = null;
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The OpenDocument file is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            if (!"tab-stops".equals(x.name)) {
                x.skipElement();
                continue;
            }
            out = new ArrayList<TabStop>();
            for (int f = x.next(); f != XmlScanner.END; f = x.next()) {
                if (f == XmlScanner.EOF) {
                    throw new IOException("The OpenDocument file is truncated");
                }
                if (f == XmlScanner.START) {
                    tabStop(x, out);
                }
            }
        }
        return out;
    }

    private static void tabStop(XmlScanner x, List<TabStop> out) throws IOException {
        if ("tab-stop".equals(x.name) && out.size() < 64) {
            String type = x.attr("type");
            String a = "char".equals(type) ? TabStop.DECIMAL : "right".equals(type)
                    ? TabStop.RIGHT : "center".equals(type) ? TabStop.CENTER : TabStop.LEFT;
            String text = x.attr("leader-text");
            String ls = x.attr("leader-style");
            char leader = text != null && text.length() == 1 ? text.charAt(0)
                    : "dotted".equals(ls) ? '.' : "solid".equals(ls) ? '_' : 0;
            out.add(new TabStop(length(x.attr("position"), 0), a, leader));
        }
        x.skipElement();
    }

    /// The column count among an element's children, read to its end; the gap goes into
    /// `into`.
    private static int columnsOf(XmlScanner x, Style into) throws IOException {
        int count = 1;
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The OpenDocument file is truncated");
            }
            if (e == XmlScanner.START) {
                if ("columns".equals(x.name)) {
                    count = x.intAttr("column-count", 1);
                    into.columnGap = length(x.attr("column-gap"), 36);
                }
                x.skipElement();
            }
        }
        return count;
    }

    /// A length in points from an ODF measure such as `2cm`, `12pt` or `0.5in`.
    static float length(String v, float fallback) {
        if (v == null) {
            return fallback;
        }
        String s = v.trim();
        float scale = 1;
        int cut = s.length();
        if (s.endsWith("pt")) {
            cut -= 2;
        } else if (s.endsWith("cm")) {
            cut -= 2;
            scale = 72 / 2.54f;
        } else if (s.endsWith("mm")) {
            cut -= 2;
            scale = 72 / 25.4f;
        } else if (s.endsWith("in")) {
            cut -= 2;
            scale = 72;
        } else if (s.endsWith("pc")) {
            cut -= 2;
            scale = 12;
        } else if (s.endsWith("px")) {
            cut -= 2;
            scale = 0.75f;
        }
        try {
            float f = Float.parseFloat(s.substring(0, cut)) * scale;
            return Float.isNaN(f) || Float.isInfinite(f) ? fallback : f;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void textProps(XmlScanner x, DocxReader.Props p) {
        String font = x.attr("font-name");
        if (font == null) {
            font = x.attr("font-family");
            if (font != null && font.length() > 1 && font.charAt(0) == '\'') {
                font = font.substring(1, font.length() - 1);
            }
        }
        if (font != null) {
            p.font = font;
        }
        String w = x.attr("fo:font-weight");
        if (w != null) {
            p.bold = Boolean.valueOf("bold".equals(w) || w.length() == 3 && w.charAt(0) >= '6');
        }
        String st = x.attr("fo:font-style");
        if (st != null) {
            p.italic = Boolean.valueOf("italic".equals(st) || "oblique".equals(st));
        }
        String u = x.attr("text-underline-style");
        if (u != null) {
            p.underline = Boolean.valueOf(!"none".equals(u));
        }
        String l = x.attr("text-line-through-style");
        if (l != null) {
            p.strike = Boolean.valueOf(!"none".equals(l));
        }
        String color = x.attr("fo:color");
        if (color != null && color.length() == 7 && color.charAt(0) == '#') {
            p.color = Integer.valueOf(DocxReader.hex(color.substring(1), 0));
        }
        String bg = x.attr("fo:background-color");
        if (bg != null) {
            p.background = Integer.valueOf(bg.length() == 7 && bg.charAt(0) == '#'
                    ? DocxReader.hex(bg.substring(1), TextStyle.NO_BACKGROUND)
                    : TextStyle.NO_BACKGROUND);
        }
        String size = x.attr("fo:font-size");
        if (size != null && !size.endsWith("%")) {
            float f = length(size, 0);
            if (f > 0 && f < 1638) {
                p.size = Float.valueOf(f);
            }
        }
        String pos = x.attr("text-position");
        if (pos != null) {
            p.script = pos.startsWith("super") || pos.length() > 0 && pos.charAt(0) >= '1'
                    && pos.charAt(0) <= '9' ? TextStyle.SUPERSCRIPT : pos.startsWith("sub")
                    || pos.startsWith("-") ? TextStyle.SUBSCRIPT : TextStyle.BASELINE;
        }
        String display = x.attr("text:display");
        if ("none".equals(display)) {
            p.hidden = Boolean.TRUE;
        }
    }

    private static void paraProps(XmlScanner x, DocxReader.Para p) {
        String align = x.attr("fo:text-align");
        if (align != null) {
            p.align = "center".equals(align) ? Paragraph.CENTER : "end".equals(align)
                    || "right".equals(align) ? Paragraph.RIGHT : "justify".equals(align)
                    ? Paragraph.JUSTIFY : Paragraph.LEFT;
        }
        if (x.attr("fo:margin-top") != null) {
            p.before = Float.valueOf(length(x.attr("fo:margin-top"), 0));
        }
        if (x.attr("fo:margin-bottom") != null) {
            p.after = Float.valueOf(length(x.attr("fo:margin-bottom"), 0));
        }
        if (x.attr("fo:margin-left") != null) {
            p.indent = Float.valueOf(length(x.attr("fo:margin-left"), 0));
        }
        if (x.attr("fo:margin-right") != null) {
            p.rightIndent = Float.valueOf(length(x.attr("fo:margin-right"), 0));
        }
        if (x.attr("fo:text-indent") != null) {
            p.firstLine = Float.valueOf(length(x.attr("fo:text-indent"), 0));
        }
        String lh = x.attr("fo:line-height");
        if (lh != null && lh.endsWith("%")) {
            try {
                float v = Float.parseFloat(lh.substring(0, lh.length() - 1)) / 100;
                if (v > 0 && v < 10) {
                    p.spacing = Float.valueOf(Math.round(v * 100) / 100f);
                }
            } catch (NumberFormatException ignored) {
                // not a percentage
            }
        }
        if ("page".equals(x.attr("fo:break-before"))) {
            p.pageBreak = Boolean.TRUE;
        }
        if ("always".equals(x.attr("fo:keep-with-next"))) {
            p.keepNext = Boolean.TRUE;
        }
    }

    private DocxReader.Props runOf(String name) {
        DocxReader.Props out = new DocxReader.Props();
        runOf(name, out, 0);
        return out;
    }

    private void runOf(String name, DocxReader.Props out, int depth) {
        Style s = name == null ? null : styles.get(name);
        if (s == null || depth > 20) {
            return;
        }
        runOf(s.parent, out, depth + 1);
        out.overlay(s.run);
    }

    private DocxReader.Para paraOf(String name) {
        DocxReader.Para out = new DocxReader.Para();
        paraOf(name, out, 0);
        return out;
    }

    private void paraOf(String name, DocxReader.Para out, int depth) {
        Style s = name == null ? null : styles.get(name);
        if (s == null || depth > 20) {
            return;
        }
        paraOf(s.parent, out, depth + 1);
        out.overlay(s.para);
    }

    /// The first named (non-automatic) style up a chain, for the paragraph's style name.
    private String namedStyle(String name, Integer[] outline) {
        String n = name;
        for (int depth = 0; n != null && depth < 20; depth++) {
            Style s = styles.get(n);
            if (s == null) {
                return n;
            }
            if (s.outline != null && outline[0] == null) {
                outline[0] = s.outline;
            }
            if (s.display != null || !isAutomatic(n)) {
                return s.display != null ? s.display : n;
            }
            n = s.parent;
        }
        return null;
    }

    private static boolean isAutomatic(String name) {
        // automatic styles are named P1, T12, ... by every writer that matters
        if (name.length() < 2 || name.charAt(0) != 'P' && name.charAt(0) != 'T') {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) < '0' || name.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    private String styleName(String odfName, int headingLevel) {
        if (headingLevel > 0) {
            return "Heading" + Math.min(6, headingLevel);
        }
        Integer[] outline = new Integer[1];
        String name = namedStyle(odfName, outline);
        if (name == null) {
            return "Normal";
        }
        String fold = Ids.asciiLower(name).replace("_20_", "").replace(" ", "");
        if (fold.startsWith("heading") && fold.length() == 8 && fold.charAt(7) >= '1'
                && fold.charAt(7) <= '6') {
            return "Heading" + fold.charAt(7);
        }
        if ("title".equals(fold)) {
            return "Title";
        }
        if ("quotations".equals(fold) || "quote".equals(fold)) {
            return "Quote";
        }
        if ("preformattedtext".equals(fold) || "code".equals(fold)) {
            return "Code";
        }
        if ("standard".equals(fold) || "textbody".equals(fold) || "default".equals(fold)
                || "normal".equals(fold) || "defaultparagraphstyle".equals(fold)) {
            return "Normal";
        }
        return name.replace("_20_", " ");
    }

    // ------------------------------------------------------------------ tracked changes

    private void readChanges(XmlScanner x) throws IOException {
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The OpenDocument file is truncated");
            }
            if (e != XmlScanner.START || !"changed-region".equals(x.name)) {
                if (e == XmlScanner.START) {
                    x.skipElement();
                }
                continue;
            }
            String id = x.attr("id");
            String kind = null;
            String author = null;
            String date = null;
            RichText.Builder deleted = new RichText.Builder();
            int depth = 1;
            while (depth > 0) {
                int f = x.next();
                if (f == XmlScanner.EOF) {
                    throw new IOException("The OpenDocument file is truncated");
                }
                if (f == XmlScanner.END) {
                    depth--;
                    continue;
                }
                if (f != XmlScanner.START) {
                    continue;
                }
                String n = x.name;
                if ("insertion".equals(n) || "deletion".equals(n)
                        || "format-change".equals(n)) {
                    kind = n;
                    depth++;
                } else if ("creator".equals(n)) {
                    author = x.elementText().trim();
                } else if ("date".equals(n)) {
                    date = x.elementText().trim();
                } else if (("p".equals(n) || "h".equals(n)) && "deletion".equals(kind)) {
                    List<Block> tmp = new ArrayList<Block>();
                    paragraph(x, tmp, "h".equals(n), null);
                    for (Block b : tmp) {
                        if (b instanceof Paragraph) {
                            if (deleted.length() > 0) {
                                deleted.append("\n", TextStyle.DEFAULT);
                            }
                            deleted.append(((Paragraph) b).text);
                        }
                    }
                } else {
                    depth++;
                }
            }
            if (id == null || kind == null || "format-change".equals(kind)) {
                continue;
            }
            Revision r = new Revision(id, "deletion".equals(kind) ? Revision.DELETE
                    : Revision.INSERT, author, date);
            changes.put(id, r);
            if (r.isDeletion()) {
                deletions.put(id, deleted.build());
            }
        }
    }

    // ------------------------------------------------------------------ blocks

    /// Reads block content to the end of the current element. `list` is the list context
    /// (style name, level, start) or null outside a list.
    private void content(XmlScanner x, List<Block> out, ListContext list) throws IOException {
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The OpenDocument file is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("p".equals(n) || "h".equals(n)) {
                paragraph(x, out, "h".equals(n), list);
                if (list != null) {
                    list.start = 0;
                }
            } else if ("list".equals(n)) {
                ListContext inner = new ListContext();
                String style = x.attr("style-name");
                inner.style = style != null ? style : list != null ? list.style : null;
                inner.level = list == null ? 0 : Math.min(8, list.level + 1);
                content(x, out, inner);
            } else if ("list-item".equals(n) || "list-header".equals(n)) {
                if (list != null) {
                    int start = x.intAttr("start-value", 0);
                    if (start > 0) {
                        list.start = start;
                    }
                }
                content(x, out, list);
            } else if ("table".equals(n)) {
                Table t = table(x);
                if (t != null) {
                    out.add(t);
                }
            } else if ("section".equals(n) && inBody && list == null) {
                Style st = styles.get(x.attr("style-name"));
                int cols = st == null ? 0 : st.columns;
                if (cols > 1) {
                    PageSpec around = pageNow();
                    out.add(new SectionBreak(ids.next('p'), around.withColumns(cols,
                            st.columnGap), false));
                    content(x, out, null);
                    out.add(new SectionBreak(ids.next('p'), around, false));
                } else {
                    content(x, out, null);
                }
            } else if ("section".equals(n) || "index-body".equals(n) || "deletion".equals(n)
                    || "table-of-content".equals(n) || "alphabetical-index".equals(n)) {
                if ("table-of-content".equals(n) || "alphabetical-index".equals(n)) {
                    // read only the generated body, not the template
                    for (int f = x.next(); f != XmlScanner.END && f != XmlScanner.EOF;
                            f = x.next()) {
                        if (f == XmlScanner.START) {
                            if ("index-body".equals(x.name)) {
                                content(x, out, list);
                            } else {
                                x.skipElement();
                            }
                        }
                    }
                } else {
                    content(x, out, list);
                }
            } else if ("tracked-changes".equals(n)) {
                readChanges(x);
            } else {
                x.skipElement();
            }
        }
    }

    static final class ListContext {
        String style;
        int level;
        int start;
    }

    // ------------------------------------------------------------------ paragraphs

    private final class Para {
        final String id;
        final RichText.Builder text = new RichText.Builder();
        String link = "";
        Revision revision;
        boolean space = true;

        Para(String id) {
            this.id = id;
        }

        void add(String s, DocxReader.Props props) {
            if (Boolean.TRUE.equals(props.hidden) || s.length() == 0) {
                return;
            }
            text.append(s, style(props, link, revision));
        }
    }

    private void paragraph(XmlScanner x, List<Block> out, boolean heading, ListContext list)
            throws IOException {
        String styleName = x.attr("style-name");
        int level = heading ? Math.max(1, x.intAttr("outline-level", 1)) : 0;
        Style own = styleName == null ? null : styles.get(styleName);
        if (inBody && own != null && own.master != null && masterPage(own.master) != null) {
            PageSpec mp = masterPage(own.master);
            if (!bodyStarted) {
                firstPage = mp;
            } else {
                out.add(new SectionBreak(ids.next('p'), mp, true));
            }
            currentPage = mp;
        }
        if (inBody) {
            bodyStarted = true;
        }
        Para p = new Para(ids.next('p'));
        DocxReader.Props base = runOf(styleName);
        spans(x, p, base);
        DocxReader.Para props = paraOf(styleName);
        String kind = Paragraph.NO_LIST;
        int listLevel = 0;
        int start = 0;
        if (list != null) {
            Style ps = styleName == null ? null : styles.get(styleName);
            String ls = list.style != null ? list.style : ps == null ? null : ps.listStyle;
            String[] kinds = ls == null ? null : listStyles.get(ls);
            kind = kinds == null || kinds[list.level] == null ? Paragraph.BULLET
                    : kinds[list.level];
            listLevel = list.level;
            start = Paragraph.NUMBERED.equals(kind) ? list.start : 0;
        }
        RichText text = trimEnd(p.text.build());
        if (text.length() == 1 && text.objectAt(0) instanceof Picture
                && Paragraph.NO_LIST.equals(kind)) {
            out.add((Picture) text.objectAt(0));
            return;
        }
        out.add(new Paragraph(p.id, styleName(styleName, level), props.align, kind, listLevel,
                start, props.before == null ? 0 : props.before.floatValue(),
                props.after == null ? 0 : props.after.floatValue(),
                props.indent == null ? 0 : Math.max(0, props.indent.floatValue()),
                props.spacing == null ? 1 : props.spacing.floatValue(),
                props.pageBreak != null && props.pageBreak.booleanValue(),
                props.keepNext != null && props.keepNext.booleanValue(), text)
                .withIndents(props.indent == null ? 0 : Math.max(0, props.indent.floatValue()),
                        props.firstLine == null ? 0 : props.firstLine.floatValue(),
                        props.rightIndent == null ? 0 : Math.max(0,
                        props.rightIndent.floatValue()))
                .withTabs(props.tabs));
    }

    private static RichText trimEnd(RichText t) {
        int end = t.length();
        String s = t.plainText();
        while (end > 0 && s.charAt(end - 1) == ' ' && t.objectAt(end - 1) == null) {
            end--;
        }
        return end == t.length() ? t : t.substring(0, end);
    }

    private TextStyle style(DocxReader.Props p, String link, Revision revision) {
        String font = p.font == null ? "Serif" : p.font;
        if ("Times New Roman".equals(font) || "Liberation Serif".equals(font)) {
            font = "Serif";
        } else if ("Arial".equals(font) || "Liberation Sans".equals(font)) {
            font = "SansSerif";
        } else if ("Courier New".equals(font) || "Liberation Mono".equals(font)) {
            font = "Monospace";
        }
        return new TextStyle(font, p.size == null ? 12 : p.size.floatValue(),
                p.color == null ? TextStyle.DEFAULT.color : p.color.intValue(),
                Boolean.TRUE.equals(p.bold), Boolean.TRUE.equals(p.italic),
                Boolean.TRUE.equals(p.underline), Boolean.TRUE.equals(p.strike),
                p.background == null ? TextStyle.NO_BACKGROUND : p.background.intValue(), link,
                p.script, revision);
    }

    /// Reads mixed content to the end of the current element.
    private void spans(XmlScanner x, Para p, DocxReader.Props props) throws IOException {
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The OpenDocument file is truncated");
            }
            if (e == XmlScanner.TEXT) {
                text(p, x.text, props);
                continue;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("span".equals(n)) {
                DocxReader.Props inner = props.copy();
                inner.overlay(runOf(x.attr("style-name")));
                spans(x, p, inner);
            } else if ("a".equals(n)) {
                String saved = p.link;
                String href = x.attr("href");
                p.link = href == null ? "" : href;
                spans(x, p, props);
                p.link = saved;
            } else if ("s".equals(n)) {
                int c = Math.max(1, Math.min(1000, x.intAttr("c", 1)));
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < c; i++) {
                    b.append(' ');
                }
                p.add(b.toString(), props);
                p.space = false;
                x.skipElement();
            } else if ("tab".equals(n)) {
                p.add("\t", props);
                p.space = false;
                x.skipElement();
            } else if ("line-break".equals(n)) {
                p.add("\n", props);
                p.space = true;
                x.skipElement();
            } else if ("soft-page-break".equals(n) || "bookmark".equals(n)
                    || "bookmark-start".equals(n) || "bookmark-end".equals(n)
                    || "reference-mark".equals(n) || "alphabetical-index-mark".equals(n)) {
                x.skipElement();
            } else if ("note".equals(n)) {
                note(x, p, props);
            } else if ("frame".equals(n)) {
                Picture pic = frame(x);
                if (pic != null) {
                    p.text.append(pic, style(props, p.link, p.revision));
                    p.space = false;
                }
            } else if ("annotation".equals(n)) {
                annotation(x, p);
            } else if ("annotation-end".equals(n)) {
                Comment c = openComments.remove(x.attr("name"));
                if (c != null) {
                    comments.set(comments.indexOf(c), new Comment(c.id, c.author, c.date, c.text,
                            false, c.startId, c.start, p.id, p.text.length()));
                }
                x.skipElement();
            } else if ("change-start".equals(n)) {
                p.revision = changes.get(x.attr("change-id"));
                x.skipElement();
            } else if ("change-end".equals(n)) {
                p.revision = null;
                x.skipElement();
            } else if ("change".equals(n)) {
                Revision r = changes.get(x.attr("change-id"));
                RichText deleted = deletions.get(x.attr("change-id"));
                if (r != null && deleted != null) {
                    for (int i = 0; i < deleted.runCount(); i++) {
                        if (deleted.runObject(i) == null) {
                            p.text.append(deleted.runText(i), deleted.runStyle(i).withRevision(r));
                        }
                    }
                }
                x.skipElement();
            } else if (isField(n)) {
                String instruction = x.attr("cn1:instruction");
                boolean fixed = "true".equals(x.attr("fixed"));
                String shown = x.elementText();
                p.text.append(new Field(instruction != null ? instruction : fieldInstruction(n),
                        shown, fixed),
                        style(props, p.link, p.revision));
                p.space = false;
            } else if ("ruby".equals(n) || "meta".equals(n) || "sequence".equals(n)
                    || "bibliography-mark".equals(n)) {
                spans(x, p, props);
            } else {
                x.skipElement();
            }
        }
    }

    private static void text(Para p, String raw, DocxReader.Props props) {
        StringBuilder b = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                if (!p.space) {
                    b.append(' ');
                    p.space = true;
                }
            } else {
                b.append(c);
                p.space = false;
            }
        }
        p.add(b.toString(), props);
    }

    private static final String[] FIELDS = {"page-number", "PAGE", "page-count", "NUMPAGES",
        "date", "DATE", "time", "TIME", "title", "TITLE", "author-name", "AUTHOR",
        "initial-creator", "AUTHOR", "file-name", "FILENAME", "word-count", "NUMWORDS",
        "subject", "SUBJECT", "chapter", "STYLEREF 1", "sequence-ref", "REF"};

    private static boolean isField(String n) {
        for (int i = 0; i < FIELDS.length; i += 2) {
            if (FIELDS[i].equals(n)) {
                return true;
            }
        }
        return false;
    }

    private static String fieldInstruction(String n) {
        for (int i = 0; i < FIELDS.length; i += 2) {
            if (FIELDS[i].equals(n)) {
                return FIELDS[i + 1];
            }
        }
        return n;
    }

    private void note(XmlScanner x, Para p, DocxReader.Props props) throws IOException {
        String kind = "endnote".equals(x.attr("note-class")) ? Note.ENDNOTE : Note.FOOTNOTE;
        String mark = null;
        List<Block> body = new ArrayList<Block>();
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The OpenDocument file is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            if ("note-citation".equals(x.name)) {
                String label = x.attr("label");
                String shown = x.elementText().trim();
                if (label != null && label.length() > 0) {
                    mark = label;
                } else if (shown.length() > 0 && !isNumber(shown)) {
                    mark = shown;
                }
            } else if ("note-body".equals(x.name)) {
                content(x, body, null);
            } else {
                x.skipElement();
            }
        }
        p.text.append(new Note("n" + ++noteCounter, kind, mark, body),
                style(props, p.link, p.revision));
        p.space = false;
    }

    private static boolean isNumber(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return false;
            }
        }
        return s.length() > 0;
    }

    private void annotation(XmlScanner x, Para p) throws IOException {
        String name = x.attr("name");
        String author = null;
        String date = null;
        StringBuilder text = new StringBuilder();
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The OpenDocument file is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            if ("creator".equals(x.name)) {
                author = x.elementText().trim();
            } else if ("date".equals(x.name)) {
                date = x.elementText().trim();
            } else if ("p".equals(x.name)) {
                if (text.length() > 0) {
                    text.append('\n');
                }
                text.append(x.elementText());
            } else {
                x.skipElement();
            }
        }
        int at = p.text.length();
        Comment c = new Comment("c" + ++commentCounter, author, date, text.toString(), false,
                p.id, at, p.id, at);
        comments.add(c);
        if (name != null) {
            openComments.put(name, c);
        }
    }

    private Picture frame(XmlScanner x) throws IOException {
        float w = length(x.attr("width"), 0);
        float h = length(x.attr("height"), 0);
        String alt = x.attr("name");
        String anchor = x.attr("anchor-type");
        Style gs = styles.get(x.attr("style-name"));
        float y = length(x.attr("y"), 0);
        String href = null;
        String desc = null;
        int depth = 1;
        while (depth > 0) {
            int e = x.next();
            if (e == XmlScanner.EOF) {
                throw new IOException("The OpenDocument file is truncated");
            }
            if (e == XmlScanner.END) {
                depth--;
                continue;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            if ("image".equals(x.name) && href == null) {
                href = x.attr("href");
                depth++;
            } else if ("desc".equals(x.name) || "title".equals(x.name)) {
                String t = x.elementText().trim();
                if (t.length() > 0 && desc == null) {
                    desc = t;
                }
            } else {
                depth++;
            }
        }
        if (href == null) {
            return null;
        }
        byte[] data = parts.get(ZipParts.resolve("", href));
        if (data == null || data.length == 0) {
            return null;
        }
        if (Float.isNaN(w) || Float.isNaN(h) || w <= 0 || h <= 0 || w > 20000 || h > 20000) {
            w = 100;
            h = 100;
        }
        Picture pic = new Picture(ids.next('i'), data, null, w, h, desc != null ? desc : alt);
        boolean floats = "paragraph".equals(anchor) || "char".equals(anchor);
        if (floats && gs != null && gs.wrap != null) {
            pic = pic.withWrap(gs.wrap, gs.horizontal, y, gs.distance < 0 ? 9 : gs.distance);
        } else if (floats) {
            pic = pic.withWrap(Picture.SQUARE, Picture.LEFT, y, 9);
        }
        return pic;
    }

    // ------------------------------------------------------------------ tables

    /// Tables nested deeper than this are refused: Word stops well short of it, and each
    /// level costs stack frames a hostile file could otherwise exhaust.
    private static final int MAX_TABLE_DEPTH = 16;
    private int tableDepth;

    private Table table(XmlScanner x) throws IOException {
        if (tableDepth >= MAX_TABLE_DEPTH) {
            throw new IOException("Tables are nested too deeply");
        }
        tableDepth++;
        try {
            return readTable(x);
        } finally {
            tableDepth--;
        }
    }

    private Table readTable(XmlScanner x) throws IOException {
        String id = ids.next('t');
        List<List<List<Block>>> rows = new ArrayList<List<List<Block>>>();
        List<int[]> anchors = new ArrayList<int[]>();
        int headerRows = 0;
        List<Float> grid = new ArrayList<Float>();
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The OpenDocument file is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("table-column".equals(n)) {
                Style st = styles.get(x.attr("style-name"));
                int repeat = Math.max(1, Math.min(MAX_COLUMNS, x.intAttr("number-columns-repeated", 1)));
                for (int i = 0; i < repeat && grid.size() < MAX_COLUMNS; i++) {
                    grid.add(Float.valueOf(st == null ? 0 : st.columnWidth));
                }
                x.skipElement();
            } else if ("table-header-rows".equals(n)) {
                int before = rows.size();
                rows(x, rows, anchors);
                headerRows = before == 0 ? rows.size() : headerRows;
            } else if ("table-rows".equals(n) || "table-row-group".equals(n)) {
                rows(x, rows, anchors);
            } else if ("table-row".equals(n)) {
                row(x, rows, anchors);
            } else {
                x.skipElement();
            }
        }
        if (rows.isEmpty()) {
            return null;
        }
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
        if ((long) rows.size() * columns > MAX_CELLS) {
            // ragged rows: one wide row over many narrow ones still makes a full grid here
            throw new IOException("The table is too large");
        }
        boolean[][] used = new boolean[rows.size()][columns];
        for (int[] a : anchors) {
            int r = a[0];
            int c = a[1];
            int h = Math.min(a[2], rows.size() - r);
            int w = Math.min(a[3], columns - c);
            boolean free = true;
            for (int rr = r; rr < r + h && free; rr++) {
                for (int cc = c; cc < c + w; cc++) {
                    if (used[rr][cc]) {
                        free = false;
                        break;
                    }
                }
            }
            if (!free || h * w <= 1) {
                continue;
            }
            for (int rr = r; rr < r + h; rr++) {
                for (int cc = c; cc < c + w; cc++) {
                    used[rr][cc] = true;
                }
            }
            merges.add(new Table.Merge(r, c, h, w));
        }
        return DocxReader.withGrid(new Table(id, rows, merges, Math.min(headerRows, rows.size())),
                grid);
    }

    private void rows(XmlScanner x, List<List<List<Block>>> rows, List<int[]> anchors)
            throws IOException {
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The OpenDocument file is truncated");
            }
            if (e == XmlScanner.START) {
                if ("table-row".equals(x.name)) {
                    row(x, rows, anchors);
                } else {
                    x.skipElement();
                }
            }
        }
    }

    private void row(XmlScanner x, List<List<List<Block>>> rows, List<int[]> anchors)
            throws IOException {
        int repeat = Math.max(1, Math.min(100, x.intAttr("number-rows-repeated", 1)));
        List<List<Block>> row = new ArrayList<List<Block>>();
        int r = rows.size();
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The OpenDocument file is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            boolean covered = "covered-table-cell".equals(x.name);
            if (!"table-cell".equals(x.name) && !covered) {
                x.skipElement();
                continue;
            }
            int cols = Math.max(1, Math.min(MAX_COLUMNS, x.intAttr("number-columns-repeated", 1)));
            int spanC = Math.max(1, Math.min(MAX_COLUMNS, x.intAttr("number-columns-spanned", 1)));
            int spanR = Math.max(1, Math.min(MAX_ROWS, x.intAttr("number-rows-spanned", 1)));
            List<Block> cell = new ArrayList<Block>();
            content(x, cell, null);
            for (int k = 0; k < cols && row.size() < MAX_COLUMNS; k++) {
                if (!covered && (spanC > 1 || spanR > 1)) {
                    anchors.add(new int[] {r, row.size(), spanR, spanC});
                }
                row.add(k == 0 ? cell : new ArrayList<Block>(cell));
            }
        }
        if (row.isEmpty()) {
            return;
        }
        for (int i = 0; i < repeat && rows.size() < MAX_ROWS; i++) {
            cells += row.size();
            if (cells > MAX_CELLS) {
                throw new IOException("The tables have too many cells");
            }
            List<List<Block>> copy = i == 0 ? row : new ArrayList<List<Block>>(row);
            rows.add(copy);
        }
    }
}
