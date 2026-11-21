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
        x.raw(" trackChanges=\"false\"><page");
        x.attr("width", Ids.fmt(d.page.width)).attr("height", Ids.fmt(d.page.height));
        x.attr("top", Ids.fmt(d.page.top)).attr("right", Ids.fmt(d.page.right));
        x.attr("bottom", Ids.fmt(d.page.bottom)).attr("left", Ids.fmt(d.page.left));
        x.raw("/>");
        blocks(x, d.blocks);
        x.raw("</document>");
        x.flush();
        zip.closeEntry();
        zip.finish();
    }

    private static void blocks(XmlOut x, List<Block> blocks) throws IOException {
        for (Block b : blocks) {
            if (b instanceof Paragraph) {
                paragraph(x, (Paragraph) b);
            } else if (b instanceof Table) {
                table(x, (Table) b);
            } else if (b instanceof Picture) {
                picture(x, (Picture) b);
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
                .attr("keepNext", String.valueOf(p.keepNext)).raw(">");
        RichText t = p.text;
        for (int i = 0; i < t.runCount(); i++) {
            TextStyle s = t.runStyle(i);
            x.raw("<r").attr("font", s.font).attr("size", Ids.fmt(s.size))
                    .attr("color", Integer.toHexString(s.color))
                    .attr("background", s.background == TextStyle.NO_BACKGROUND ? "ffffff"
                            : Integer.toHexString(s.background))
                    .attr("bold", String.valueOf(s.bold)).attr("italic", String.valueOf(s.italic))
                    .attr("underline", String.valueOf(s.underline))
                    .attr("strike", String.valueOf(s.strike)).attr("link", s.link)
                    .attr("script", s.script).raw(">");
            x.text(t.runText(i), false);
            x.raw("</r>");
        }
        x.raw("</p>");
    }

    private static void table(XmlOut x, Table t) throws IOException {
        x.raw("<table").attr("id", t.id);
        if (t.headerRows > 0) {
            x.attr("headerRows", t.headerRows);
        }
        x.raw(">");
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
                .attr("height", Ids.fmt(p.height)).attr("align", "left")
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
        long next = (long) x.doubleAttr("next", 1);
        PageSpec page = null;
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
                page = new PageSpec((float) x.doubleAttr("width", 595.28),
                        (float) x.doubleAttr("height", 841.89), (float) x.doubleAttr("top", 54),
                        (float) x.doubleAttr("right", 54), (float) x.doubleAttr("bottom", 54),
                        (float) x.doubleAttr("left", 54));
                x.skipElement();
            } else if (!block(x, blocks, ids)) {
                x.skipElement();
            }
        }
        return new Document(blocks, page, title, next);
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
                } else if ("r".equals(x.name)) {
                    style0 = readStyle(x);
                    depth++;
                } else if ("field".equals(x.name) || "note".equals(x.name)) {
                    // A field shows its cached result; a note's mark has no text of its own.
                    String result = "field".equals(x.name) ? x.attr("result") : "";
                    x.skipElement();
                    if (result != null && result.length() > 0) {
                        text.append(result, style0);
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
                pageBreak, keepNext, text.build());
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
                x.attr("script"));
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
        for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw new IOException("Truncated table " + id);
            }
            if (ev != XmlScanner.START) {
                continue;
            }
            if ("row".equals(x.name)) {
                rows.add(readRow(x, ids));
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
            return new Table(id, rows, merges, headerRows);
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
        return new Picture(id, bytes, type, w, h, alt);
    }
}
