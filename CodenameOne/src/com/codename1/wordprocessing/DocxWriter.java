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

import com.codename1.io.zip.ZipArchiveOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

/// Writes a document as Office Open XML (`.docx`).
///
/// Every run carries its complete formatting, so the file looks the same in any reader whatever
/// its defaults. Embedded components are written as their snapshot picture; a component with no
/// snapshot is left out.
final class DocxWriter {
    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String R =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String REL = R + "/";
    private static final String NS = " xmlns:w=\"" + W + "\" xmlns:r=\"" + R + "\""
            + " xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\""
            + " xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\""
            + " xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\"";
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";

    private final Document doc;
    private final Map<String, String> media = new LinkedHashMap<String, String>();
    private final List<byte[]> mediaData = new ArrayList<byte[]>();
    /// The relationships of the part being written; a header has its own.
    private List<String[]> rels = new ArrayList<String[]>();
    /// `{part name, root element, relationships}` of each header and footer written.
    private final List<Object[]> furniture = new ArrayList<Object[]>();
    private String headerRefs = "";
    private final StringBuilder footnotes = new StringBuilder(); //NOPMD AvoidStringBufferField - a single-use writer owns its output
    private final StringBuilder endnotes = new StringBuilder(); //NOPMD AvoidStringBufferField - a single-use writer owns its output
    private final Set<String> styleNames = new LinkedHashSet<String>();
    private final List<Integer> restarts = new ArrayList<Integer>();
    private final Map<String, Integer> commentIds = new LinkedHashMap<String, Integer>();
    private int footnoteCount;
    private int endnoteCount;
    private int drawingCount;
    private int revisionCount = 1000000;
    private int currentNumbered = 2;

    private DocxWriter(Document doc) {
        this.doc = doc;
    }

    static void write(Document doc, OutputStream out) throws IOException {
        new DocxWriter(doc).write(out);
    }

    private void write(OutputStream out) throws IOException {
        int n = 0;
        // a comment whose anchor paragraph is gone would be half a range, which Word rejects
        List<String> paragraphIds = new ArrayList<String>();
        DocumentModel.collectIds(doc.blocks, paragraphIds);
        Set<String> present = new HashSet<String>(paragraphIds);
        for (Comment c : doc.comments) {
            if (present.contains(c.startId) && present.contains(c.endId)) {
                commentIds.put(c.id, Integer.valueOf(n++));
            }
        }
        headerRefs = headerParts();
        StringBuilder body = new StringBuilder();
        body.append(XML).append("<w:document").append(NS).append("><w:body>");
        // Word keeps a section's properties at its end: in the last paragraph of each section
        // but the final one, whose properties close the body
        PageSpec page = doc.page;
        boolean continuous = false;
        List<Block> section = new ArrayList<Block>();
        for (Block block : doc.blocks) {
            if (block instanceof SectionBreak) {
                blocks(body, section);
                section.clear();
                body.append("<w:p><w:pPr>");
                sectPr(body, page, continuous);
                body.append("</w:pPr></w:p>");
                SectionBreak br = (SectionBreak) block;
                page = br.page;
                continuous = !br.newPage;
            } else {
                section.add(block);
            }
        }
        blocks(body, section);
        sectPr(body, page, continuous);
        body.append("</w:body></w:document>");

        ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out);
        entry(zip, "[Content_Types].xml", contentTypes());
        entry(zip, "_rels/.rels", XML + "<Relationships xmlns=\"http://schemas.openxmlformats.org/"
                + "package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"" + REL
                + "officeDocument\" Target=\"word/document.xml\"/><Relationship Id=\"rId2\" "
                + "Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/"
                + "core-properties\" Target=\"docProps/core.xml\"/></Relationships>");
        entry(zip, "docProps/core.xml", core());
        entry(zip, "word/document.xml", body.toString());
        entry(zip, "word/styles.xml", styles());
        entry(zip, "word/numbering.xml", numbering());
        entry(zip, "word/settings.xml", XML + "<w:settings" + NS + ">"
                + (doc.trackChanges ? "<w:trackRevisions/>" : "")
                + "<w:defaultTabStop w:val=\"720\"/><w:compat><w:compatSetting w:name="
                + "\"compatibilityMode\" w:uri=\"http://schemas.microsoft.com/office/word\" "
                + "w:val=\"15\"/></w:compat></w:settings>");
        entry(zip, "word/footnotes.xml", notesPart("footnotes", "footnote", footnotes));
        entry(zip, "word/endnotes.xml", notesPart("endnotes", "endnote", endnotes));
        if (!commentIds.isEmpty()) {
            entry(zip, "word/comments.xml", comments());
        }
        entry(zip, "word/_rels/document.xml.rels", documentRels());
        for (Object[] f : furniture) {
            entry(zip, "word/" + f[0], (String) f[1]);
            @SuppressWarnings("unchecked")
            List<String[]> own = (List<String[]>) f[2];
            StringBuilder r = new StringBuilder(XML);
            r.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/")
                    .append("relationships\">");
            for (String[] x : own) {
                rel(r, x[0], x[1], x[2], "1".equals(x[3]));
            }
            entry(zip, "word/_rels/" + f[0] + ".rels", r.append("</Relationships>").toString());
        }
        int i = 0;
        for (String name : media.values()) {
            byte[] data = mediaData.get(i++);
            zip.putNextEntry(new ZipEntry("word/" + name));
            zip.write(data, 0, data.length);
            zip.closeEntry();
        }
        zip.finish();
    }

    private static void entry(ZipArchiveOutputStream zip, String name, String content)
            throws IOException {
        byte[] b = content.getBytes("UTF-8");
        zip.putNextEntry(new ZipEntry(name));
        zip.write(b, 0, b.length);
        zip.closeEntry();
    }

    private void sectPr(StringBuilder b, PageSpec p, boolean continuous) {
        b.append("<w:sectPr>").append(headerRefs);
        if (continuous) {
            b.append("<w:type w:val=\"continuous\"/>");
        }
        b.append("<w:pgSz w:w=\"").append(tw(p.width)).append("\" w:h=\"").append(tw(p.height))
                .append('"');
        if (p.width > p.height) {
            b.append(" w:orient=\"landscape\"");
        }
        b.append("/><w:pgMar w:top=\"").append(tw(p.top)).append("\" w:right=\"")
                .append(tw(p.right)).append("\" w:bottom=\"").append(tw(p.bottom))
                .append("\" w:left=\"").append(tw(p.left)).append("\" w:header=\"")
                .append(tw(Math.min(36, p.top * 0.5f))).append("\" w:footer=\"")
                .append(tw(Math.min(36, p.bottom * 0.5f))).append("\" w:gutter=\"0\"/>");
        if (p.columns > 1) {
            b.append("<w:cols w:num=\"").append(p.columns).append("\" w:space=\"")
                    .append(tw(p.columnGap)).append("\"/>");
        }
        if (doc.firstHeader != null || doc.firstFooter != null) {
            b.append("<w:titlePg/>");
        }
        b.append("</w:sectPr>");
    }

    /// Writes the header and footer parts and answers the references every section carries.
    private String headerParts() {
        StringBuilder refs = new StringBuilder();
        furniturePart(refs, "header", "hdr", "default", doc.header, !doc.header.isEmpty());
        furniturePart(refs, "header", "hdr", "first", doc.firstHeader, doc.firstHeader != null);
        furniturePart(refs, "footer", "ftr", "default", doc.footer, !doc.footer.isEmpty());
        furniturePart(refs, "footer", "ftr", "first", doc.firstFooter, doc.firstFooter != null);
        return refs.toString();
    }

    private void furniturePart(StringBuilder refs, String kind, String root, String type,
            List<Block> blocks, boolean present) {
        if (!present) {
            return;
        }
        String name = kind + (furniture.size() + 1) + ".xml";
        String id = "rIdH" + (furniture.size() + 1);
        List<String[]> saved = rels;
        rels = new ArrayList<String[]>();
        StringBuilder b = new StringBuilder(XML);
        b.append("<w:").append(root).append(NS).append('>');
        blocks(b, blocks);
        if (blocks.isEmpty()) {
            // an empty first-page header still needs a paragraph
            b.append("<w:p/>");
        }
        b.append("</w:").append(root).append('>');
        furniture.add(new Object[] {name, b.toString(), rels});
        rels = saved;
        rels.add(new String[] {id, kind, name, ""});
        refs.append("<w:").append(kind).append("Reference w:type=\"").append(type)
                .append("\" r:id=\"").append(id).append("\"/>");
    }

    private static String tw(float pt) {
        return String.valueOf(Math.round(pt * 20));
    }

    // ------------------------------------------------------------------ package parts

    private String contentTypes() {
        StringBuilder b = new StringBuilder(XML);
        b.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-")
                .append("package.relationships+xml\"/><Default Extension=\"xml\" ContentType=")
                .append("\"application/xml\"/><Default Extension=\"png\" ContentType=\"image/png\"/>")
                .append("<Default Extension=\"jpeg\" ContentType=\"image/jpeg\"/>")
                .append("<Default Extension=\"gif\" ContentType=\"image/gif\"/>")
                .append("<Default Extension=\"bin\" ContentType=\"application/octet-stream\"/>");
        String wp = "application/vnd.openxmlformats-officedocument.wordprocessingml.";
        override(b, "/word/document.xml", wp + "document.main+xml");
        override(b, "/word/styles.xml", wp + "styles+xml");
        override(b, "/word/numbering.xml", wp + "numbering+xml");
        override(b, "/word/settings.xml", wp + "settings+xml");
        override(b, "/word/footnotes.xml", wp + "footnotes+xml");
        override(b, "/word/endnotes.xml", wp + "endnotes+xml");
        if (!commentIds.isEmpty()) {
            override(b, "/word/comments.xml", wp + "comments+xml");
        }
        for (Object[] f : furniture) {
            String name = (String) f[0];
            override(b, "/word/" + name, wp + (name.startsWith("header") ? "header" : "footer")
                    + "+xml");
        }
        override(b, "/docProps/core.xml", "application/vnd.openxmlformats-package.core-properties+xml");
        return b.append("</Types>").toString();
    }

    private static void override(StringBuilder b, String part, String type) {
        b.append("<Override PartName=\"").append(part).append("\" ContentType=\"").append(type)
                .append("\"/>");
    }

    private String core() {
        StringBuilder b = new StringBuilder(XML);
        b.append("<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/")
                .append("metadata/core-properties\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        if (doc.title != null && doc.title.length() > 0) {
            b.append("<dc:title>");
            esc(b, doc.title);
            b.append("</dc:title>");
        }
        return b.append("</cp:coreProperties>").toString();
    }

    private String documentRels() {
        StringBuilder b = new StringBuilder(XML);
        b.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/")
                .append("relationships\">");
        rel(b, "rIdStyles", "styles", "styles.xml", false);
        rel(b, "rIdNumbering", "numbering", "numbering.xml", false);
        rel(b, "rIdSettings", "settings", "settings.xml", false);
        rel(b, "rIdFootnotes", "footnotes", "footnotes.xml", false);
        rel(b, "rIdEndnotes", "endnotes", "endnotes.xml", false);
        if (!commentIds.isEmpty()) {
            rel(b, "rIdComments", "comments", "comments.xml", false);
        }
        for (String[] r : rels) {
            rel(b, r[0], r[1], r[2], "1".equals(r[3]));
        }
        return b.append("</Relationships>").toString();
    }

    private static void rel(StringBuilder b, String id, String type, String target,
            boolean external) {
        b.append("<Relationship Id=\"").append(id).append("\" Type=\"").append(REL).append(type)
                .append("\" Target=\"");
        esc(b, target);
        b.append('"');
        if (external) {
            b.append(" TargetMode=\"External\"");
        }
        b.append("/>");
    }

    private String styles() {
        StringBuilder b = new StringBuilder(XML);
        b.append("<w:styles").append(NS).append("><w:docDefaults><w:rPrDefault><w:rPr>")
                .append("<w:rFonts w:ascii=\"Times New Roman\" w:hAnsi=\"Times New Roman\"/>")
                .append("<w:sz w:val=\"24\"/></w:rPr></w:rPrDefault><w:pPrDefault/>")
                .append("</w:docDefaults>")
                .append("<w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"Normal\">")
                .append("<w:name w:val=\"Normal\"/><w:qFormat/></w:style>");
        String[] sizes = {"", "40", "32", "28", "26", "24", "22"};
        for (int i = 1; i <= 6; i++) {
            b.append("<w:style w:type=\"paragraph\" w:styleId=\"Heading").append(i)
                    .append("\"><w:name w:val=\"heading ").append(i)
                    .append("\"/><w:basedOn w:val=\"Normal\"/><w:next w:val=\"Normal\"/><w:qFormat/>")
                    .append("<w:pPr><w:keepNext/><w:outlineLvl w:val=\"").append(i - 1)
                    .append("\"/></w:pPr><w:rPr><w:b/><w:sz w:val=\"").append(sizes[i])
                    .append("\"/></w:rPr></w:style>");
        }
        b.append("<w:style w:type=\"paragraph\" w:styleId=\"Title\"><w:name w:val=\"Title\"/>")
                .append("<w:basedOn w:val=\"Normal\"/><w:qFormat/><w:rPr><w:sz w:val=\"56\"/>")
                .append("</w:rPr></w:style>")
                .append("<w:style w:type=\"paragraph\" w:styleId=\"Quote\"><w:name w:val=\"Quote\"/>")
                .append("<w:basedOn w:val=\"Normal\"/><w:qFormat/><w:rPr><w:i/></w:rPr></w:style>")
                .append("<w:style w:type=\"paragraph\" w:styleId=\"Code\"><w:name w:val=\"Code\"/>")
                .append("<w:basedOn w:val=\"Normal\"/><w:rPr><w:rFonts w:ascii=\"Courier New\" ")
                .append("w:hAnsi=\"Courier New\"/></w:rPr></w:style>")
                .append("<w:style w:type=\"paragraph\" w:styleId=\"ListParagraph\"><w:name ")
                .append("w:val=\"List Paragraph\"/><w:basedOn w:val=\"Normal\"/></w:style>")
                .append("<w:style w:type=\"character\" w:styleId=\"FootnoteReference\"><w:name ")
                .append("w:val=\"footnote reference\"/><w:rPr><w:vertAlign w:val=\"superscript\"/>")
                .append("</w:rPr></w:style>");
        for (String name : styleNames) {
            b.append("<w:style w:type=\"paragraph\" w:customStyle=\"1\" w:styleId=\"");
            esc(b, styleId(name));
            b.append("\"><w:name w:val=\"");
            esc(b, name);
            b.append("\"/><w:basedOn w:val=\"Normal\"/><w:qFormat/></w:style>");
        }
        return b.append("</w:styles>").toString();
    }

    private static boolean builtIn(String style) {
        return "Normal".equals(style) || "Title".equals(style) || "Quote".equals(style)
                || "Code".equals(style) || style.length() == 8 && style.startsWith("Heading")
                && style.charAt(7) >= '1' && style.charAt(7) <= '6';
    }

    private static String styleId(String name) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 0x80 && Character.isLetterOrDigit(c)) {
                b.append(c);
            }
        }
        return "S" + b + Integer.toHexString(name.hashCode() & 0xffff);
    }

    private String numbering() {
        StringBuilder b = new StringBuilder(XML);
        b.append("<w:numbering").append(NS).append('>');
        abstractNum(b, 0, true);
        abstractNum(b, 1, false);
        b.append("<w:num w:numId=\"1\"><w:abstractNumId w:val=\"0\"/></w:num>")
                .append("<w:num w:numId=\"2\"><w:abstractNumId w:val=\"1\"/></w:num>");
        for (int i = 0; i < restarts.size(); i += 2) {
            int level = restarts.get(i + 1).intValue() >> 16;
            int start = restarts.get(i + 1).intValue() & 0xffff;
            b.append("<w:num w:numId=\"").append(restarts.get(i)).append("\"><w:abstractNumId ")
                    .append("w:val=\"1\"/><w:lvlOverride w:ilvl=\"").append(level)
                    .append("\"><w:startOverride w:val=\"").append(start)
                    .append("\"/></w:lvlOverride></w:num>");
        }
        return b.append("</w:numbering>").toString();
    }

    private static void abstractNum(StringBuilder b, int id, boolean bullet) {
        String[] bullets = {"\u2022", "o", "\u25aa"};
        String[] formats = {"decimal", "lowerLetter", "lowerRoman"};
        b.append("<w:abstractNum w:abstractNumId=\"").append(id)
                .append("\"><w:multiLevelType w:val=\"hybridMultilevel\"/>");
        for (int l = 0; l < 9; l++) {
            b.append("<w:lvl w:ilvl=\"").append(l).append("\"><w:start w:val=\"1\"/><w:numFmt ")
                    .append("w:val=\"").append(bullet ? "bullet" : formats[l % 3])
                    .append("\"/><w:lvlText w:val=\"")
                    .append(bullet ? bullets[l % 3] : "%" + (l + 1) + ".")
                    .append("\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"")
                    .append(720 * (l + 1)).append("\" w:hanging=\"360\"/></w:pPr></w:lvl>");
        }
        b.append("</w:abstractNum>");
    }

    private String notesPart(String root, String element, StringBuilder notes) {
        return XML + "<w:" + root + NS + "><w:" + element + " w:type=\"separator\" w:id=\"-1\">"
                + "<w:p><w:r><w:separator/></w:r></w:p></w:" + element + "><w:" + element
                + " w:type=\"continuationSeparator\" w:id=\"0\"><w:p><w:r>"
                + "<w:continuationSeparator/></w:r></w:p></w:" + element + ">" + notes + "</w:"
                + root + ">";
    }

    private String comments() {
        StringBuilder b = new StringBuilder(XML);
        b.append("<w:comments").append(NS).append('>');
        for (Comment c : doc.comments) {
            if (!commentIds.containsKey(c.id)) {
                continue;
            }
            b.append("<w:comment w:id=\"").append(commentIds.get(c.id)).append('"');
            if (c.author != null) {
                b.append(" w:author=\"");
                esc(b, c.author);
                b.append('"');
            }
            if (c.date != null && c.date.length() > 0) {
                b.append(" w:date=\"");
                esc(b, c.date);
                b.append('"');
            }
            b.append('>');
            String text = c.text == null ? "" : c.text;
            int from = 0;
            while (true) {
                int nl = text.indexOf('\n', from);
                String line = nl < 0 ? text.substring(from) : text.substring(from, nl);
                b.append("<w:p><w:r>");
                textElement(b, "t", line);
                b.append("</w:r></w:p>");
                if (nl < 0) {
                    break;
                }
                from = nl + 1;
            }
            b.append("</w:comment>");
        }
        return b.append("</w:comments>").toString();
    }

    // ------------------------------------------------------------------ blocks

    private void blocks(StringBuilder b, List<Block> list) {
        for (Block block : list) {
            if (block instanceof Paragraph) {
                paragraph(b, (Paragraph) block);
            } else if (block instanceof Table) {
                table(b, (Table) block);
            } else if (block instanceof Picture) {
                b.append("<w:p>");
                b.append("<w:r>");
                drawing(b, (Picture) block);
                b.append("</w:r></w:p>");
            }
        }
    }

    private void paragraph(StringBuilder b, Paragraph p) {
        b.append("<w:p><w:pPr>");
        if (!"Normal".equals(p.style)) {
            if (!builtIn(p.style)) {
                styleNames.add(p.style);
            }
            b.append("<w:pStyle w:val=\"");
            esc(b, builtIn(p.style) ? p.style : styleId(p.style));
            b.append("\"/>");
        }
        if (p.keepNext) {
            b.append("<w:keepNext/>");
        }
        if (p.pageBreak) {
            b.append("<w:pageBreakBefore/>");
        }
        if (!Paragraph.NO_LIST.equals(p.list)) {
            int num;
            if (Paragraph.BULLET.equals(p.list)) {
                num = 1;
            } else {
                if (p.listStart > 0) {
                    currentNumbered = 3 + restarts.size() / 2;
                    restarts.add(Integer.valueOf(currentNumbered));
                    restarts.add(Integer.valueOf(p.listLevel << 16 | Math.min(0xffff, p.listStart)));
                }
                num = currentNumbered;
            }
            b.append("<w:numPr><w:ilvl w:val=\"").append(p.listLevel).append("\"/><w:numId w:val=\"")
                    .append(num).append("\"/></w:numPr>");
        }
        b.append("<w:spacing w:before=\"").append(tw(p.before)).append("\" w:after=\"")
                .append(tw(p.after)).append("\" w:line=\"").append(Math.round(p.lineSpacing * 240))
                .append("\" w:lineRule=\"auto\"/>");
        if (!p.tabs.isEmpty()) {
            b.append("<w:tabs>");
            for (TabStop t : p.tabs) {
                b.append("<w:tab w:val=\"").append(t.alignment).append('"');
                if (t.leader != 0) {
                    b.append(" w:leader=\"").append(t.leader == '.' ? "dot" : t.leader == '-'
                            ? "hyphen" : "underscore").append('"');
                }
                b.append(" w:pos=\"").append(tw(t.position)).append("\"/>");
            }
            b.append("</w:tabs>");
        }
        if (p.indent != 0 || p.firstLine != 0 || p.rightIndent != 0) {
            b.append("<w:ind w:left=\"").append(tw(p.indent)).append('"');
            if (p.rightIndent != 0) {
                b.append(" w:right=\"").append(tw(p.rightIndent)).append('"');
            }
            if (p.firstLine > 0) {
                b.append(" w:firstLine=\"").append(tw(p.firstLine)).append('"');
            } else if (p.firstLine < 0) {
                b.append(" w:hanging=\"").append(tw(-p.firstLine)).append('"');
            }
            b.append("/>");
        }
        if (!Paragraph.LEFT.equals(p.align)) {
            b.append("<w:jc w:val=\"").append(Paragraph.JUSTIFY.equals(p.align) ? "both"
                    : p.align).append("\"/>");
        }
        b.append("</w:pPr>");
        runs(b, p.id, p.text);
        b.append("</w:p>");
    }

    /// Comment anchors that fall inside this paragraph, as offset to markup.
    private Map<Integer, StringBuilder> anchors(String paragraphId) {
        Map<Integer, StringBuilder> out = new LinkedHashMap<Integer, StringBuilder>();
        for (Comment c : doc.comments) {
            Integer id = commentIds.get(c.id);
            if (id == null) {
                continue;
            }
            if (paragraphId.equals(c.startId)) {
                marker(out, c.start).append("<w:commentRangeStart w:id=\"").append(id)
                        .append("\"/>");
            }
            if (paragraphId.equals(c.endId)) {
                marker(out, c.end).append("<w:commentRangeEnd w:id=\"").append(id)
                        .append("\"/><w:r><w:commentReference w:id=\"").append(id)
                        .append("\"/></w:r>");
            }
        }
        return out;
    }

    private static StringBuilder marker(Map<Integer, StringBuilder> m, int offset) {
        Integer k = Integer.valueOf(Math.max(0, offset));
        StringBuilder b = m.get(k);
        if (b == null) {
            b = new StringBuilder();
            m.put(k, b);
        }
        return b;
    }

    private void runs(StringBuilder b, String paragraphId, RichText text) {
        Map<Integer, StringBuilder> anchors = paragraphId == null
                ? new LinkedHashMap<Integer, StringBuilder>() : anchors(paragraphId);
        int offset = 0;
        for (int i = 0; i < text.runCount(); i++) {
            String s = text.runText(i);
            TextStyle style = text.runStyle(i);
            Inline object = text.runObject(i);
            int start = 0;
            while (start < s.length()) {
                StringBuilder a = anchors.remove(Integer.valueOf(offset + start));
                if (a != null) {
                    b.append(a);
                }
                int end = s.length();
                for (Integer k : anchors.keySet()) {
                    int rel = k.intValue() - offset;
                    if (rel > start && rel < end) {
                        end = rel;
                    }
                }
                if (object != null) {
                    inlineObject(b, object, style);
                } else {
                    run(b, s.substring(start, end), style);
                }
                start = end;
            }
            offset += s.length();
        }
        for (StringBuilder rest : anchors.values()) {
            b.append(rest);
        }
    }

    private void open(StringBuilder b, TextStyle style) {
        if (style.link != null && style.link.length() > 0) {
            if (style.link.startsWith("#")) {
                b.append("<w:hyperlink w:anchor=\"");
                esc(b, style.link.substring(1));
                b.append("\">");
            } else {
                String id = "rIdL" + rels.size();
                rels.add(new String[] {id, "hyperlink", style.link, "1"});
                b.append("<w:hyperlink r:id=\"").append(id).append("\">");
            }
        }
        Revision rev = style.revision;
        if (rev != null) {
            b.append(rev.isDeletion() ? "<w:del" : "<w:ins").append(" w:id=\"")
                    .append(revisionId(rev)).append('"');
            if (rev.author != null) {
                b.append(" w:author=\"");
                esc(b, rev.author);
                b.append('"');
            }
            if (rev.date != null && rev.date.length() > 0) {
                b.append(" w:date=\"");
                esc(b, rev.date);
                b.append('"');
            }
            b.append('>');
        }
    }

    private void close(StringBuilder b, TextStyle style) {
        if (style.revision != null) {
            b.append(style.revision.isDeletion() ? "</w:del>" : "</w:ins>");
        }
        if (style.link != null && style.link.length() > 0) {
            b.append("</w:hyperlink>");
        }
    }

    private String revisionId(Revision r) {
        String id = r.id;
        if (id != null && id.length() > 0 && id.length() < 10) {
            boolean digits = true;
            for (int i = 0; i < id.length(); i++) {
                if (id.charAt(i) < '0' || id.charAt(i) > '9') {
                    digits = false;
                }
            }
            if (digits) {
                return id;
            }
        }
        return String.valueOf(revisionCount++);
    }

    private void run(StringBuilder b, String s, TextStyle style) {
        open(b, style);
        boolean deleted = style.revision != null && style.revision.isDeletion();
        b.append("<w:r>");
        runProps(b, style, null);
        int from = 0;
        for (int i = 0; i <= s.length(); i++) {
            char c = i < s.length() ? s.charAt(i) : 0;
            if (i == s.length() || c == '\t' || c == '\n' || c == 0xad) {
                if (i > from) {
                    textElement(b, deleted ? "delText" : "t", s.substring(from, i));
                }
                if (c == '\t') {
                    b.append("<w:tab/>");
                } else if (c == '\n') {
                    b.append("<w:br/>");
                } else if (c == 0xad) {
                    b.append("<w:softHyphen/>");
                }
                from = i + 1;
            }
        }
        b.append("</w:r>");
        close(b, style);
    }

    private static void textElement(StringBuilder b, String name, String s) {
        b.append("<w:").append(name);
        if (s.length() > 0 && (s.charAt(0) == ' ' || s.charAt(s.length() - 1) == ' '
                || s.indexOf("  ") >= 0)) {
            b.append(" xml:space=\"preserve\"");
        }
        b.append('>');
        esc(b, s);
        b.append("</w:").append(name).append('>');
    }

    private static void runProps(StringBuilder b, TextStyle s, String charStyle) {
        b.append("<w:rPr>");
        if (charStyle != null) {
            b.append("<w:rStyle w:val=\"").append(charStyle).append("\"/>");
        }
        String font = s.font;
        if ("Serif".equals(font)) {
            font = "Times New Roman";
        } else if ("SansSerif".equals(font)) {
            font = "Arial";
        } else if ("Monospace".equals(font)) {
            font = "Courier New";
        }
        b.append("<w:rFonts w:ascii=\"");
        esc(b, font);
        b.append("\" w:hAnsi=\"");
        esc(b, font);
        b.append("\" w:cs=\"");
        esc(b, font);
        b.append("\"/>");
        b.append(s.bold ? "<w:b/>" : "<w:b w:val=\"0\"/>");
        b.append(s.italic ? "<w:i/>" : "<w:i w:val=\"0\"/>");
        if (s.strike) {
            b.append("<w:strike/>");
        }
        b.append("<w:color w:val=\"").append(hex(s.color)).append("\"/>");
        b.append("<w:sz w:val=\"").append(Math.max(2, Math.round(s.size * 2))).append("\"/>");
        if (s.background != TextStyle.NO_BACKGROUND) {
            b.append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"").append(hex(s.background))
                    .append("\"/>");
        }
        if (s.underline) {
            b.append("<w:u w:val=\"single\"/>");
        }
        if (TextStyle.SUPERSCRIPT.equals(s.script) || TextStyle.SUBSCRIPT.equals(s.script)) {
            b.append("<w:vertAlign w:val=\"").append(s.script).append("\"/>");
        }
        b.append("</w:rPr>");
    }

    private static String hex(int rgb) {
        String h = Integer.toHexString(rgb & 0xffffff);
        while (h.length() < 6) {
            h = "0" + h;
        }
        return h;
    }

    private void inlineObject(StringBuilder b, Inline o, TextStyle style) {
        if (o instanceof Field) {
            Field f = (Field) o;
            open(b, style.withLink(null));
            b.append("<w:r>");
            runProps(b, style, null);
            b.append("<w:fldChar w:fldCharType=\"begin\"");
            if (f.locked) {
                b.append(" w:fldLock=\"1\"");
            }
            b.append("/></w:r><w:r>");
            runProps(b, style, null);
            textElement(b, "instrText", " " + f.instruction + " ");
            b.append("</w:r><w:r>");
            runProps(b, style, null);
            b.append("<w:fldChar w:fldCharType=\"separate\"/></w:r>");
            close(b, style.withLink(null));
            run(b, f.result == null ? "" : f.result, style);
            open(b, style.withLink(null));
            b.append("<w:r>");
            runProps(b, style, null);
            b.append("<w:fldChar w:fldCharType=\"end\"/></w:r>");
            close(b, style.withLink(null));
        } else if (o instanceof Note) {
            Note n = (Note) o;
            boolean foot = !Note.ENDNOTE.equals(n.kind);
            int id = foot ? ++footnoteCount : ++endnoteCount;
            StringBuilder target = foot ? footnotes : endnotes;
            String element = foot ? "footnote" : "endnote";
            target.append("<w:").append(element).append(" w:id=\"").append(id).append("\">");
            if (n.blocks.isEmpty()) {
                target.append("<w:p/>");
            } else {
                blocks(target, n.blocks);
            }
            target.append("</w:").append(element).append('>');
            open(b, style);
            b.append("<w:r>");
            runProps(b, style, null);
            b.append("<w:").append(element).append("Reference w:id=\"").append(id).append('"');
            boolean custom = n.mark != null && n.mark.length() > 0;
            if (custom) {
                b.append(" w:customMarkFollows=\"1\"/>");
                textElement(b, "t", n.mark);
            } else {
                b.append("/>");
            }
            b.append("</w:r>");
            close(b, style);
        } else if (o instanceof Picture) {
            open(b, style);
            b.append("<w:r>");
            runProps(b, style, null);
            drawing(b, (Picture) o);
            b.append("</w:r>");
            close(b, style);
        } else if (o instanceof EmbeddedComponent) {
            Picture p = ((EmbeddedComponent) o).toPicture("component");
            if (p != null) {
                inlineObject(b, p, style);
            }
        }
    }

    private void drawing(StringBuilder b, Picture p) {
        String ext = "image/png".equals(p.mimeType) ? "png" : "image/jpeg".equals(p.mimeType)
                ? "jpeg" : "image/gif".equals(p.mimeType) ? "gif" : "bin";
        String id = "rIdM" + mediaData.size();
        String name = "media/image" + mediaData.size() + "." + ext;
        media.put(id, name);
        mediaData.add(p.data());
        rels.add(new String[] {id, "image", name, ""});
        long cx = Math.round(p.width * 12700.0);
        long cy = Math.round(p.height * 12700.0);
        int n = ++drawingCount;
        if (p.isFloating()) {
            long d = Math.round(p.distance * 12700.0);
            b.append("<w:drawing><wp:anchor distT=\"0\" distB=\"").append(d).append("\" distL=\"")
                    .append(d).append("\" distR=\"").append(d).append("\" simplePos=\"0\" ")
                    .append("relativeHeight=\"").append(n).append("\" behindDoc=\"")
                    .append(Picture.BEHIND.equals(p.wrap) ? 1 : 0).append("\" locked=\"0\" ")
                    .append("layoutInCell=\"1\" allowOverlap=\"1\"><wp:simplePos x=\"0\" y=\"0\"/>")
                    .append("<wp:positionH relativeFrom=\"column\"><wp:align>").append(p.align)
                    .append("</wp:align></wp:positionH><wp:positionV relativeFrom=\"paragraph\">")
                    .append("<wp:posOffset>").append(Math.round(p.offsetY * 12700.0))
                    .append("</wp:posOffset></wp:positionV><wp:extent cx=\"").append(cx)
                    .append("\" cy=\"").append(cy).append("\"/>");
            if (Picture.SQUARE.equals(p.wrap)) {
                b.append("<wp:wrapSquare wrapText=\"bothSides\"/>");
            } else if (Picture.TOP_BOTTOM.equals(p.wrap)) {
                b.append("<wp:wrapTopAndBottom/>");
            } else {
                b.append("<wp:wrapNone/>");
            }
            b.append("<wp:docPr id=\"");
        } else {
            b.append("<w:drawing><wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">")
                    .append("<wp:extent cx=\"").append(cx).append("\" cy=\"").append(cy)
                    .append("\"/><wp:docPr id=\"");
        }
        b.append(n).append("\" name=\"Picture ").append(n)
                .append("\" descr=\"");
        esc(b, p.description);
        b.append("\"/><a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/")
                .append("drawingml/2006/picture\"><pic:pic><pic:nvPicPr><pic:cNvPr id=\"")
                .append(n).append("\" name=\"Picture ").append(n)
                .append("\"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed=\"")
                .append(id).append("\"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>")
                .append("<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"").append(cx)
                .append("\" cy=\"").append(cy).append("\"/></a:xfrm><a:prstGeom prst=\"rect\">")
                .append("<a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic>")
                .append(p.isFloating() ? "</wp:anchor></w:drawing>" : "</wp:inline></w:drawing>");
    }

    // ------------------------------------------------------------------ tables

    private void table(StringBuilder b, Table t) {
        int columns = t.columns();
        int width = 9000 / Math.max(1, columns);
        b.append("<w:tbl><w:tblPr><w:tblW w:w=\"0\" w:type=\"auto\"/><w:tblBorders>");
        String[] sides = {"top", "left", "bottom", "right", "insideH", "insideV"};
        for (String side : sides) {
            b.append("<w:").append(side).append(" w:val=\"single\" w:sz=\"4\" w:space=\"0\" ")
                    .append("w:color=\"auto\"/>");
        }
        b.append("</w:tblBorders></w:tblPr><w:tblGrid>");
        for (int c = 0; c < columns; c++) {
            b.append("<w:gridCol w:w=\"").append(width).append("\"/>");
        }
        b.append("</w:tblGrid>");
        for (int r = 0; r < t.rows(); r++) {
            b.append("<w:tr>");
            if (r < t.headerRows) {
                b.append("<w:trPr><w:tblHeader/></w:trPr>");
            }
            int c = 0;
            while (c < columns) {
                Table.Merge m = mergeAt(t, r, c);
                int span = 1;
                String vmerge = null;
                List<Block> cell = t.cell(r, c);
                if (m != null) {
                    span = m.columns;
                    if (m.rows > 1) {
                        vmerge = m.row == r ? "restart" : "continue";
                    }
                    if (m.row != r) {
                        cell = new ArrayList<Block>();
                    }
                    c = m.column;
                }
                b.append("<w:tc><w:tcPr><w:tcW w:w=\"").append(width * span)
                        .append("\" w:type=\"dxa\"/>");
                if (span > 1) {
                    b.append("<w:gridSpan w:val=\"").append(span).append("\"/>");
                }
                if (vmerge != null) {
                    b.append("restart".equals(vmerge) ? "<w:vMerge w:val=\"restart\"/>"
                            : "<w:vMerge/>");
                }
                b.append("</w:tcPr>");
                blocks(b, cell);
                if (cell.isEmpty() || !(cell.get(cell.size() - 1) instanceof Paragraph)) {
                    b.append("<w:p/>");
                }
                b.append("</w:tc>");
                c += span;
            }
            b.append("</w:tr>");
        }
        b.append("</w:tbl>");
    }

    private static Table.Merge mergeAt(Table t, int r, int c) {
        for (Table.Merge m : t.merges) {
            if (m.contains(r, c)) {
                return m;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ escaping

    static void esc(StringBuilder b, String s) {
        if (s == null) {
            return;
        }
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
                    if (c < 0x20 && c != '\t' && c != '\n' && c != '\r' || c == 0xfffe
                            || c == 0xffff) {
                        b.append(' ');
                    } else {
                        b.append(c);
                    }
            }
        }
    }
}
