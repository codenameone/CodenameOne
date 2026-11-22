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

import com.codename1.impl.xml.XmlOut;
import com.codename1.impl.xml.XmlScanner;
import com.codename1.io.zip.ZipArchiveOutputStream;
import com.codename1.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// The `.cn1doc` container: a ZIP whose `document.xml` holds the model.
///
/// The XML vocabulary is the Codename One word processor's, so a file from either side opens in
/// the other. Elements and attributes this model has no place for (sections, comments,
/// revisions, notes, fields) are skipped on reading; an `original.docx` entry is ignored.
final class NativeFormat {
    private static final String ENTRY = "document.xml";

    private NativeFormat() {
    }

    static void write(Document d, OutputStream out) throws IOException {
        ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out);
        zip.putNextEntry(new ZipEntry(ENTRY));
        XmlOut x = new XmlOut(zip);
        x.raw("<?xml version=\"1.0\" encoding=\"UTF-8\"?><document version=\"1\"");
        x.attr("title", d.title).attr("next", String.valueOf(d.nextId));
        x.raw(" trackChanges=\"").raw(String.valueOf(d.trackChanges)).raw("\">");
        page(x, d.page);
        furniture(x, "header", "default", d.header);
        furniture(x, "header", "first", d.firstHeader);
        furniture(x, "footer", "default", d.footer);
        furniture(x, "footer", "first", d.firstFooter);
        blocks(x, d.blocks);
        comments(x, d.comments);
        x.raw("</document>");
        x.flush();
        zip.closeEntry();
        zip.finish();
    }

    private static void page(XmlOut x, PageSpec p) throws IOException {
        x.raw("<page");
        x.attr("width", Ids.fmt(p.width)).attr("height", Ids.fmt(p.height));
        x.attr("top", Ids.fmt(p.top)).attr("right", Ids.fmt(p.right));
        x.attr("bottom", Ids.fmt(p.bottom)).attr("left", Ids.fmt(p.left));
        x.attr("columns", p.columns).attr("columnGap", Ids.fmt(p.columnGap));
        x.raw("/>");
    }

    private static void furniture(XmlOut x, String element, String kind, List<Block> blocks)
            throws IOException {
        if (blocks == null || blocks.isEmpty() && "default".equals(kind)) {
            return;
        }
        x.raw("<").raw(element).attr("kind", kind).raw(">");
        blocks(x, blocks);
        x.raw("</").raw(element).raw(">");
    }

    private static void blocks(XmlOut x, List<Block> blocks) throws IOException {
        for (Block b : blocks) {
            if (b instanceof Paragraph) {
                paragraph(x, (Paragraph) b);
            } else if (b instanceof Table) {
                table(x, (Table) b);
            } else if (b instanceof Picture) {
                picture(x, (Picture) b);
            } else if (b instanceof SectionBreak) {
                SectionBreak br = (SectionBreak) b;
                x.raw("<section").attr("id", br.id)
                        .attr("newPage", String.valueOf(br.newPage)).raw(">");
                page(x, br.page);
                x.raw("</section>");
            }
        }
    }

    private static void paragraph(XmlOut x, Paragraph p) throws IOException {
        x.raw("<p").attr("id", p.id).attr("style", p.style).attr("align", p.align)
                .attr("list", p.list).attr("listLevel", p.listLevel)
                .attr("listStart", p.listStart).attr("before", Ids.fmt(p.before))
                .attr("after", Ids.fmt(p.after)).attr("indent", Ids.fmt(p.indent))
                .attr("spacing", Ids.fmt(p.lineSpacing))
                .attr("pageBreak", String.valueOf(p.pageBreak))
                .attr("keepNext", String.valueOf(p.keepNext))
                .attr("firstLine", Ids.fmt(p.firstLine))
                .attr("rightIndent", Ids.fmt(p.rightIndent)).raw(">");
        for (TabStop tab : p.tabs) {
            x.raw("<tab").attr("pos", Ids.fmt(tab.position)).attr("align", tab.alignment)
                    .attr("leader", tab.leader == 0 ? "" : String.valueOf(tab.leader)).raw("/>");
        }
        RichText t = p.text;
        for (int i = 0; i < t.runCount(); i++) {
            TextStyle s = t.runStyle(i);
            Inline o = t.runObject(i);
            if (o instanceof Field) {
                Field f = (Field) o;
                x.raw("<field").attr("instruction", f.instruction).attr("result", f.result)
                        .attr("locked", String.valueOf(f.locked)).raw(" showCode=\"false\">");
                run(x, s, "");
                x.raw("</field>");
            } else if (o instanceof Note) {
                Note n = (Note) o;
                x.raw("<note").attr("id", n.id).attr("kind", n.kind).attr("mark", n.mark)
                        .attr("label", "").raw(">");
                run(x, s, "");
                blocks(x, n.blocks);
                x.raw("</note>");
            } else if (o instanceof Picture) {
                // the empty run carries the style the object's character has
                run(x, s, "");
                picture(x, (Picture) o);
            } else if (o instanceof EmbeddedComponent) {
                EmbeddedComponent e = (EmbeddedComponent) o;
                run(x, s, "");
                x.raw("<component").attr("type", e.type).attr("width", Ids.fmt(e.width))
                        .attr("height", Ids.fmt(e.height)).raw("><state>");
                x.text(e.state, false);
                x.raw("</state>");
                byte[] snap = e.snapshot();
                if (snap != null) {
                    x.raw("<snapshot>").raw(Base64.encodeNoNewline(snap)).raw("</snapshot>");
                }
                x.raw("</component>");
            } else {
                run(x, s, t.runText(i));
            }
        }
        x.raw("</p>");
    }

    private static void run(XmlOut x, TextStyle s, String text) throws IOException {
        x.raw("<r").attr("font", s.font).attr("size", Ids.fmt(s.size))
                .attr("color", Integer.toHexString(s.color))
                .attr("background", s.background == TextStyle.NO_BACKGROUND ? "ffffff"
                        : Integer.toHexString(s.background))
                .attr("bold", String.valueOf(s.bold)).attr("italic", String.valueOf(s.italic))
                .attr("underline", String.valueOf(s.underline))
                .attr("strike", String.valueOf(s.strike)).attr("link", s.link)
                .attr("script", s.script);
        Revision r = s.revision;
        if (r != null) {
            x.attr("revision-id", r.id.length() == 0 ? "r0" : r.id).attr("revision-kind", r.kind)
                    .attr("revision-author", r.author).attr("revision-date", r.date);
        }
        x.raw(">");
        x.text(text, false);
        x.raw("</r>");
    }

    private static void comments(XmlOut x, List<Comment> comments) throws IOException {
        for (Comment c : comments) {
            x.raw("<comment").attr("id", c.id).attr("author", c.author).attr("startId", c.startId)
                    .attr("start", c.start).attr("endId", c.endId).attr("end", c.end)
                    .attr("resolved", String.valueOf(c.resolved));
            if (c.date.length() > 0) {
                x.attr("date", c.date);
            }
            x.raw("><text>");
            x.text(c.text, false);
            x.raw("</text></comment>");
        }
    }

    private static void table(XmlOut x, Table t) throws IOException {
        x.raw("<table").attr("id", t.id);
        if (t.headerRows > 0) {
            x.attr("headerRows", t.headerRows);
        }
        x.raw(">");
        for (Float w : t.columnWidths()) {
            x.raw("<col").attr("width", Ids.fmt(w.floatValue())).raw("/>");
        }
        for (int r = 0; r < t.rows(); r++) {
            x.raw("<row>");
            for (int c = 0; c < t.columns(); c++) {
                x.raw("<cell>");
                blocks(x, t.cell(r, c));
                x.raw("</cell>");
            }
            x.raw("</row>");
        }
        for (int i = 0; i < t.merges.size(); i++) {
            Table.Merge m = t.merges.get(i);
            x.raw("<merge").attr("row", m.row).attr("column", m.column).attr("rows", m.rows)
                    .attr("columns", m.columns).raw("/>");
        }
        x.raw("</table>");
    }

    private static void picture(XmlOut x, Picture p) throws IOException {
        x.raw("<picture").attr("id", p.id).attr("width", Ids.fmt(p.width))
                .attr("height", Ids.fmt(p.height)).attr("align", p.align)
                .attr("wrap", p.wrap).attr("offsetY", Ids.fmt(p.offsetY))
                .attr("distance", Ids.fmt(p.distance))
                .attr("alt", p.description).attr("type", p.mimeType).raw(">");
        x.raw(Base64.encodeNoNewline(p.data()));
        x.raw("</picture>");
    }

    static Document read(InputStream in) throws IOException {
        ZipInputStream zip = new ZipInputStream(in);
        for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
            if (ENTRY.equals(e.getName())) {
                return parse(new XmlScanner(zip));
            }
        }
        throw new IOException("Not a .cn1doc file: no " + ENTRY);
    }

    private static Document parse(XmlScanner x) throws IOException {
        if (x.next() != XmlScanner.START || !"document".equals(x.name)) {
            throw new IOException("Not a .cn1doc document");
        }
        if (!"1".equals(x.attr("version"))) {
            throw new IOException("Unsupported .cn1doc version " + x.attr("version"));
        }
        String title = x.attr("title");
        boolean track = x.boolAttr("trackChanges");
        List<Comment> comments = new ArrayList<Comment>();
        long next = (long) x.doubleAttr("next", 1);
        PageSpec page = null;
        @SuppressWarnings("unchecked")
        List<Block>[] furniture = new List[4];
        Set<String> ids = new HashSet<String>();
        List<Block> blocks = new ArrayList<Block>();
        for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw new IOException("Truncated .cn1doc document");
            }
            if (ev != XmlScanner.START) {
                continue;
            }
            if ("page".equals(x.name)) {
                page = readPage(x);
            } else if ("header".equals(x.name) || "footer".equals(x.name)) {
                boolean head = "header".equals(x.name);
                boolean first = "first".equals(x.attr("kind"));
                List<Block> b = new ArrayList<Block>();
                for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
                    if (e == XmlScanner.EOF) {
                        throw new IOException("Truncated .cn1doc document");
                    }
                    if (e == XmlScanner.START && !block(x, b, ids)) {
                        x.skipElement();
                    }
                }
                furniture[(head ? 0 : 2) + (first ? 1 : 0)] = b;
            } else if ("comment".equals(x.name)) {
                Comment c = readComment(x);
                if (c != null) {
                    comments.add(c);
                }
            } else if (!block(x, blocks, ids)) {
                x.skipElement();
            }
        }
        Document d = new Document(blocks, page, title, next, comments, track);
        if (furniture[0] != null || furniture[1] != null || furniture[2] != null
                || furniture[3] != null) {
            d = d.withHeaders(furniture[0], furniture[2], furniture[1], furniture[3]);
        }
        return d;
    }

    private static PageSpec readPage(XmlScanner x) throws IOException {
        PageSpec page;
        try {
            page = new PageSpec((float) x.doubleAttr("width", 595.28),
                    (float) x.doubleAttr("height", 841.89), (float) x.doubleAttr("top", 54),
                    (float) x.doubleAttr("right", 54), (float) x.doubleAttr("bottom", 54),
                    (float) x.doubleAttr("left", 54)).withColumns(x.intAttr("columns", 1),
                    (float) x.doubleAttr("columnGap", 36));
        } catch (IllegalArgumentException e) {
            throw damaged("page", e);
        }
        x.skipElement();
        return page;
    }

    /// Reads the block whose start tag the scanner is on into `out`, or answers false when the
    /// element is not a block.
    private static boolean block(XmlScanner x, List<Block> out, Set<String> ids)
            throws IOException {
        if ("p".equals(x.name)) {
            out.add(readParagraph(x, ids));
            return true;
        }
        if ("table".equals(x.name)) {
            out.add(readTable(x, ids));
            return true;
        }
        if ("picture".equals(x.name)) {
            Picture p = readPicture(x, ids);
            if (p != null) {
                out.add(p);
            }
            return true;
        }
        if ("section".equals(x.name)) {
            String id = id(x, ids);
            boolean newPage = !"false".equals(x.attr("newPage"));
            PageSpec page = null;
            for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
                if (e == XmlScanner.EOF) {
                    throw new IOException("Truncated section " + id);
                }
                if (e == XmlScanner.START) {
                    if ("page".equals(x.name)) {
                        page = readPage(x);
                    } else {
                        x.skipElement();
                    }
                }
            }
            out.add(new SectionBreak(id, page, newPage));
            return true;
        }
        return false;
    }

    private static String id(XmlScanner x, Set<String> ids) throws IOException {
        String id = x.attr("id");
        if (id == null || id.length() == 0) {
            throw new IOException("A block without an id");
        }
        if (!ids.add(id)) {
            throw new IOException("Duplicate block id " + id);
        }
        return id;
    }

    private static Paragraph readParagraph(XmlScanner x, Set<String> ids) throws IOException {
        String id = id(x, ids);
        String style = x.attr("style");
        String align = x.attr("align");
        String list = x.attr("list");
        int level = x.intAttr("listLevel", 0);
        int start = x.intAttr("listStart", 0);
        float before = (float) x.doubleAttr("before", 0);
        float after = (float) x.doubleAttr("after", 8);
        float indent = (float) x.doubleAttr("indent", 0);
        float spacing = (float) x.doubleAttr("spacing", 1.15);
        boolean pageBreak = x.boolAttr("pageBreak");
        boolean keepNext = x.boolAttr("keepNext");
        float firstLine = (float) x.doubleAttr("firstLine", 0);
        float rightIndent = (float) x.doubleAttr("rightIndent", 0);
        List<TabStop> tabs = new ArrayList<TabStop>();
        RichText.Builder text = new RichText.Builder();
        int depth = 0;
        TextStyle style0 = null;
        for (int ev = x.next(); ; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw new IOException("Truncated paragraph " + id);
            }
            if (ev == XmlScanner.START) {
                if ("previousFormat".equals(x.name)) {
                    x.skipElement();
                } else if ("tab".equals(x.name)) {
                    String l = x.attr("leader");
                    tabs.add(new TabStop((float) x.doubleAttr("pos", 0), x.attr("align"),
                            l == null || l.length() == 0 ? 0 : l.charAt(0)));
                    x.skipElement();
                } else if ("r".equals(x.name)) {
                    style0 = readStyle(x);
                    depth++;
                } else if ("field".equals(x.name)) {
                    Field f = new Field(x.attr("instruction"), x.attr("result"),
                            x.boolAttr("locked"));
                    text.append(f, objectStyle(x));
                } else if ("note".equals(x.name)) {
                    TextStyle[] noteStyle = {style0};
                    Note note = readNote(x, noteStyle);
                    text.append(note, noteStyle[0]);
                } else if ("picture".equals(x.name)) {
                    Picture pic = readPicture(x, ids);
                    if (pic != null) {
                        text.append(pic, style0);
                    }
                } else if ("component".equals(x.name)) {
                    Inline o = readComponent(x);
                    if (o != null) {
                        text.append(o, style0);
                    }
                } else {
                    x.skipElement();
                }
            } else if (ev == XmlScanner.TEXT) {
                if (depth > 0) {
                    text.append(x.text, style0);
                }
            } else {
                if (depth == 0) {
                    break;
                }
                depth--;
            }
        }
        return new Paragraph(id, style, align, list, level, start, before, after, indent, spacing,
                pageBreak, keepNext, text.build()).withIndents(indent, firstLine, rightIndent)
                .withTabs(tabs);
    }

    /// Reads to the end of a field and answers the style of the run inside it.
    private static TextStyle objectStyle(XmlScanner x) throws IOException {
        TextStyle style = null;
        for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw new IOException("Truncated field");
            }
            if (ev == XmlScanner.START) {
                if ("r".equals(x.name) && style == null) {
                    style = readStyle(x);
                }
                x.skipElement();
            }
        }
        return style;
    }

    private static EmbeddedComponent readComponent(XmlScanner x) throws IOException {
        String type = x.attr("type");
        float w = (float) x.doubleAttr("width", 0);
        float h = (float) x.doubleAttr("height", 0);
        String state = "";
        byte[] snap = null;
        for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw new IOException("Truncated component");
            }
            if (ev == XmlScanner.START) {
                if ("state".equals(x.name)) {
                    state = x.elementText();
                } else if ("snapshot".equals(x.name)) {
                    snap = Base64.decode(Ids.ascii(x.elementText().trim()));
                } else {
                    x.skipElement();
                }
            }
        }
        if (type == null || type.length() == 0 || Float.isNaN(w) || Float.isNaN(h) || w <= 0
                || h <= 0) {
            return null;
        }
        return new EmbeddedComponent(type, state, w, h, snap);
    }

    private static Note readNote(XmlScanner x, TextStyle[] style) throws IOException {
        String id = x.attr("id");
        String kind = x.attr("kind");
        String mark = x.attr("mark");
        List<Block> content = new ArrayList<Block>();
        Set<String> storyIds = new HashSet<String>();
        for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw new IOException("Truncated note");
            }
            if (ev == XmlScanner.START) {
                if ("r".equals(x.name)) {
                    style[0] = readStyle(x);
                    x.skipElement();
                } else if (!block(x, content, storyIds)) {
                    x.skipElement();
                }
            }
        }
        return new Note(id == null || id.length() == 0 ? "n0" : id, kind, mark, content);
    }

    private static Comment readComment(XmlScanner x) throws IOException {
        String id = x.attr("id");
        String author = x.attr("author");
        String date = x.attr("date");
        String startId = x.attr("startId");
        String endId = x.attr("endId");
        int start = x.intAttr("start", 0);
        int end = x.intAttr("end", 0);
        boolean resolved = x.boolAttr("resolved");
        String text = "";
        for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw new IOException("Truncated comment");
            }
            if (ev == XmlScanner.START) {
                if ("text".equals(x.name)) {
                    text = x.elementText();
                } else {
                    x.skipElement();
                }
            }
        }
        if (id == null || id.length() == 0 || startId == null || endId == null) {
            return null;
        }
        return new Comment(id, author, date, text, resolved, startId, start, endId, end);
    }

    private static TextStyle readStyle(XmlScanner x) {
        float size = (float) x.doubleAttr("size", 12);
        if (Float.isNaN(size) || size <= 0 || size > 1638) {
            size = 12;
        }
        int background = hex(x.attr("background"), 0xffffff);
        return new TextStyle(x.attr("font"), size, hex(x.attr("color"), 0x202833),
                x.boolAttr("bold"), x.boolAttr("italic"), x.boolAttr("underline"),
                x.boolAttr("strike"),
                background == 0xffffff ? TextStyle.NO_BACKGROUND : background, x.attr("link"),
                x.attr("script"), revision(x));
    }

    private static Revision revision(XmlScanner x) {
        String id = x.attr("revision-id");
        if (id == null || id.length() == 0) {
            return null;
        }
        return new Revision(id, x.attr("revision-kind"), x.attr("revision-author"),
                x.attr("revision-date"));
    }

    private static int hex(String s, int fallback) {
        if (s == null || s.length() == 0 || s.length() > 8) {
            return fallback;
        }
        int v = 0;
        for (int i = 0; i < s.length(); i++) {
            int d = Character.digit(s.charAt(i), 16);
            if (d < 0) {
                return fallback;
            }
            v = v * 16 + d;
        }
        return v & 0xffffff;
    }

    private static Table readTable(XmlScanner x, Set<String> ids) throws IOException {
        String id = id(x, ids);
        int headerRows = x.intAttr("headerRows", 0);
        List<List<List<Block>>> rows = new ArrayList<List<List<Block>>>();
        List<Table.Merge> merges = new ArrayList<Table.Merge>();
        List<Float> widths = new ArrayList<Float>();
        for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw new IOException("Truncated table " + id);
            }
            if (ev != XmlScanner.START) {
                continue;
            }
            if ("row".equals(x.name)) {
                rows.add(readRow(x, ids));
            } else if ("col".equals(x.name)) {
                widths.add(Float.valueOf((float) x.doubleAttr("width", 0)));
                x.skipElement();
            } else if ("merge".equals(x.name)) {
                try {
                    merges.add(new Table.Merge(x.intAttr("row", -1), x.intAttr("column", -1),
                            x.intAttr("rows", 1), x.intAttr("columns", 1)));
                } catch (IllegalArgumentException e) {
                    throw damaged("Table " + id, e);
                }
                x.skipElement();
            } else {
                x.skipElement();
            }
        }
        if (rows.isEmpty()) {
            throw new IOException("Table " + id + " has no rows");
        }
        try {
            return DocxReader.withGrid(new Table(id, rows, merges, headerRows), widths);
        } catch (IllegalArgumentException e) {
            throw damaged("Table " + id, e);
        }
    }

    private static IOException damaged(String what, IllegalArgumentException cause) {
        IOException e = new IOException(what + ": " + cause.getMessage());
        e.initCause(cause);
        return e;
    }

    private static List<List<Block>> readRow(XmlScanner x, Set<String> ids) throws IOException {
        List<List<Block>> row = new ArrayList<List<Block>>();
        for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw new IOException("Truncated table row");
            }
            if (ev != XmlScanner.START) {
                continue;
            }
            if ("cell".equals(x.name)) {
                List<Block> cell = new ArrayList<Block>();
                for (int e2 = x.next(); e2 != XmlScanner.END; e2 = x.next()) {
                    if (e2 == XmlScanner.EOF) {
                        throw new IOException("Truncated table cell");
                    }
                    if (e2 == XmlScanner.START && !block(x, cell, ids)) {
                        x.skipElement();
                    }
                }
                row.add(cell);
            } else {
                x.skipElement();
            }
        }
        return row;
    }

    private static Picture readPicture(XmlScanner x, Set<String> ids) throws IOException {
        String id = id(x, ids);
        float w = (float) x.doubleAttr("width", 0);
        float h = (float) x.doubleAttr("height", 0);
        String alt = x.attr("alt");
        String type = x.attr("type");
        String wrap = x.attr("wrap");
        String align = x.attr("align");
        float offsetY = (float) x.doubleAttr("offsetY", 0);
        float distance = (float) x.doubleAttr("distance", 9);
        StringBuilder data = new StringBuilder();
        for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw new IOException("Truncated picture " + id);
            }
            if (ev == XmlScanner.TEXT) {
                data.append(x.text);
            } else if (ev == XmlScanner.START) {
                // A text box's story; not part of this model.
                x.skipElement();
            }
        }
        byte[] bytes = Base64.decode(data.toString().trim().getBytes("US-ASCII"));
        if (bytes == null || bytes.length == 0 || Float.isNaN(w) || Float.isNaN(h) || w <= 0
                || h <= 0) {
            return null;
        }
        return new Picture(id, bytes, type, w, h, alt).withWrap(wrap, align, offsetY, distance);
    }
}
