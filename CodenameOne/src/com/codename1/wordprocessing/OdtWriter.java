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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

/// Writes a document as OpenDocument text (`.odt`).
///
/// Every span carries an automatic style with its complete formatting. Fields ODF has no
/// element for are written as their result; embedded components as their snapshot.
final class OdtWriter {
    private static final String MIME = "application/vnd.oasis.opendocument.text";
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    private static final String NS = " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\""
            + " xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\""
            + " xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\""
            + " xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\""
            + " xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:drawing:1.0\""
            + " xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\""
            + " xmlns:xlink=\"http://www.w3.org/1999/xlink\""
            + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\""
            + " xmlns:meta=\"urn:oasis:names:tc:opendocument:xmlns:meta:1.0\""
            + " xmlns:svg=\"urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0\""
            + " xmlns:config=\"urn:oasis:names:tc:opendocument:xmlns:config:1.0\""
            + " xmlns:cn1=\"https://www.codenameone.com/ns/wordprocessing\""
            + " office:version=\"1.3\"";

    private static final String[] FIELDS = {"PAGE", "page-number", "NUMPAGES", "page-count",
        "DATE", "date", "TIME", "time", "TITLE", "title", "AUTHOR", "author-name",
        "FILENAME", "file-name", "NUMWORDS", "word-count", "SUBJECT", "subject"};

    private final Document doc;
    private final Map<TextStyle, String> textStyles = new LinkedHashMap<TextStyle, String>();
    private final Map<String, String> paraStyles = new LinkedHashMap<String, String>();
    private final Map<String, String> namedStyles = new LinkedHashMap<String, String>();
    private final List<String> pictureNames = new ArrayList<String>();
    private final List<byte[]> pictureData = new ArrayList<byte[]>();
    private final List<String> pictureTypes = new ArrayList<String>();
    private final StringBuilder changes = new StringBuilder(); //NOPMD AvoidStringBufferField - a single-use writer owns its output
    private int changeCount;
    private int noteCount;
    private int frameCount;
    private int tableCount;
    private boolean lastSpace;

    private OdtWriter(Document doc) {
        this.doc = doc;
    }

    static void write(Document doc, OutputStream out) throws IOException {
        new OdtWriter(doc).write(out);
    }

    private void write(OutputStream out) throws IOException {
        StringBuilder body = new StringBuilder();
        blocks(body, doc.blocks);
        StringBuilder content = new StringBuilder(XML);
        content.append("<office:document-content").append(NS).append("><office:automatic-styles>");
        for (Map.Entry<String, String> e : paraStyles.entrySet()) {
            content.append(e.getValue());
        }
        for (Map.Entry<TextStyle, String> e : textStyles.entrySet()) {
            textStyle(content, e.getValue(), e.getKey());
        }
        listStyle(content, "L1", true);
        listStyle(content, "L2", false);
        content.append("</office:automatic-styles><office:body><office:text>");
        if (changes.length() > 0) {
            content.append("<text:tracked-changes text:track-changes=\"")
                    .append(doc.trackChanges).append("\">").append(changes)
                    .append("</text:tracked-changes>");
        }
        content.append(body).append("</office:text></office:body></office:document-content>");

        ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out);
        byte[] mime = MIME.getBytes("UTF-8");
        ZipEntry first = new ZipEntry("mimetype");
        first.setMethod(ZipArchiveOutputStream.STORED);
        first.setSize(mime.length);
        first.setCompressedSize(mime.length);
        CRC32 crc = new CRC32();
        crc.update(mime, 0, mime.length);
        first.setCrc(crc.getValue());
        zip.putNextEntry(first);
        zip.write(mime, 0, mime.length);
        zip.closeEntry();
        entry(zip, "content.xml", content.toString());
        entry(zip, "styles.xml", styles());
        entry(zip, "meta.xml", meta());
        entry(zip, "settings.xml", XML + "<office:document-settings" + NS + "><office:settings>"
                + "<config:config-item-set config:name=\"ooo:configuration-settings\">"
                + "<config:config-item config:name=\"RecordChanges\" config:type=\"boolean\">"
                + doc.trackChanges + "</config:config-item></config:config-item-set>"
                + "</office:settings></office:document-settings>");
        for (int i = 0; i < pictureNames.size(); i++) {
            byte[] data = pictureData.get(i);
            zip.putNextEntry(new ZipEntry(pictureNames.get(i)));
            zip.write(data, 0, data.length);
            zip.closeEntry();
        }
        entry(zip, "META-INF/manifest.xml", manifest());
        zip.finish();
    }

    private static void entry(ZipArchiveOutputStream zip, String name, String content)
            throws IOException {
        byte[] b = content.getBytes("UTF-8");
        zip.putNextEntry(new ZipEntry(name));
        zip.write(b, 0, b.length);
        zip.closeEntry();
    }

    private static String pt(float v) {
        return Ids.fmt(v) + "pt";
    }

    // ------------------------------------------------------------------ package parts

    private String manifest() {
        StringBuilder b = new StringBuilder(XML);
        b.append("<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:")
                .append("manifest:1.0\" manifest:version=\"1.3\"><manifest:file-entry ")
                .append("manifest:full-path=\"/\" manifest:version=\"1.3\" manifest:media-type=\"")
                .append(MIME).append("\"/>");
        String[] xml = {"content.xml", "styles.xml", "meta.xml", "settings.xml"};
        for (String part : xml) {
            b.append("<manifest:file-entry manifest:full-path=\"").append(part)
                    .append("\" manifest:media-type=\"text/xml\"/>");
        }
        for (int i = 0; i < pictureNames.size(); i++) {
            b.append("<manifest:file-entry manifest:full-path=\"").append(pictureNames.get(i))
                    .append("\" manifest:media-type=\"").append(pictureTypes.get(i))
                    .append("\"/>");
        }
        return b.append("</manifest:manifest>").toString();
    }

    private String meta() {
        StringBuilder b = new StringBuilder(XML);
        b.append("<office:document-meta").append(NS).append("><office:meta>")
                .append("<meta:generator>Codename One</meta:generator>");
        if (doc.title != null && doc.title.length() > 0) {
            b.append("<dc:title>");
            DocxWriter.esc(b, doc.title);
            b.append("</dc:title>");
        }
        return b.append("</office:meta></office:document-meta>").toString();
    }

    private String styles() {
        StringBuilder b = new StringBuilder(XML);
        b.append("<office:document-styles").append(NS).append("><office:styles>")
                .append("<style:default-style style:family=\"paragraph\"><style:text-properties ")
                .append("fo:font-family=\"'Times New Roman'\" fo:font-size=\"12pt\"/>")
                .append("</style:default-style>")
                .append("<style:style style:name=\"Standard\" style:family=\"paragraph\"/>");
        String[] sizes = {"", "20pt", "16pt", "14pt", "13pt", "12pt", "11pt"};
        for (int i = 1; i <= 6; i++) {
            b.append("<style:style style:name=\"Heading_20_").append(i)
                    .append("\" style:display-name=\"Heading ").append(i)
                    .append("\" style:family=\"paragraph\" style:parent-style-name=\"Standard\" ")
                    .append("style:default-outline-level=\"").append(i)
                    .append("\"><style:text-properties fo:font-size=\"").append(sizes[i])
                    .append("\" fo:font-weight=\"bold\"/></style:style>");
        }
        b.append("<style:style style:name=\"Title\" style:family=\"paragraph\" ")
                .append("style:parent-style-name=\"Standard\"><style:text-properties ")
                .append("fo:font-size=\"28pt\"/></style:style>")
                .append("<style:style style:name=\"Quotations\" style:family=\"paragraph\" ")
                .append("style:parent-style-name=\"Standard\"/>")
                .append("<style:style style:name=\"Preformatted_20_Text\" style:display-name=")
                .append("\"Preformatted Text\" style:family=\"paragraph\" ")
                .append("style:parent-style-name=\"Standard\"/>");
        for (Map.Entry<String, String> e : namedStyles.entrySet()) {
            b.append("<style:style style:name=\"");
            DocxWriter.esc(b, e.getValue());
            b.append("\" style:display-name=\"");
            DocxWriter.esc(b, e.getKey());
            b.append("\" style:family=\"paragraph\" style:parent-style-name=\"Standard\"/>");
        }
        b.append("</office:styles>");
        if (doc.page != null) {
            PageSpec p = doc.page;
            b.append("<office:automatic-styles><style:page-layout style:name=\"pm1\">")
                    .append("<style:page-layout-properties fo:page-width=\"").append(pt(p.width))
                    .append("\" fo:page-height=\"").append(pt(p.height))
                    .append("\" style:print-orientation=\"")
                    .append(p.width > p.height ? "landscape" : "portrait")
                    .append("\" fo:margin-top=\"").append(pt(p.top))
                    .append("\" fo:margin-right=\"").append(pt(p.right))
                    .append("\" fo:margin-bottom=\"").append(pt(p.bottom))
                    .append("\" fo:margin-left=\"").append(pt(p.left))
                    .append("\"/></style:page-layout></office:automatic-styles>")
                    .append("<office:master-styles><style:master-page style:name=\"Standard\" ")
                    .append("style:page-layout-name=\"pm1\"/></office:master-styles>");
        }
        return b.append("</office:document-styles>").toString();
    }

    private static void listStyle(StringBuilder b, String name, boolean bullet) {
        String[] bullets = {"•", "◦", "▪"};
        String[] formats = {"1", "a", "i"};
        b.append("<text:list-style style:name=\"").append(name).append("\">");
        for (int l = 1; l <= 10; l++) {
            if (bullet) {
                b.append("<text:list-level-style-bullet text:level=\"").append(l)
                        .append("\" text:bullet-char=\"").append(bullets[(l - 1) % 3]).append('"');
            } else {
                b.append("<text:list-level-style-number text:level=\"").append(l)
                        .append("\" style:num-suffix=\".\" style:num-format=\"")
                        .append(formats[(l - 1) % 3]).append('"');
            }
            b.append("><style:list-level-properties text:list-level-position-and-space-mode=")
                    .append("\"label-alignment\"><style:list-level-label-alignment ")
                    .append("text:label-followed-by=\"listtab\" fo:text-indent=\"-18pt\" ")
                    .append("fo:margin-left=\"").append(18 * l).append("pt\"/>")
                    .append("</style:list-level-properties>")
                    .append(bullet ? "</text:list-level-style-bullet>"
                            : "</text:list-level-style-number>");
        }
        b.append("</text:list-style>");
    }

    private static void textStyle(StringBuilder b, String name, TextStyle s) {
        b.append("<style:style style:name=\"").append(name).append("\" style:family=\"text\">")
                .append("<style:text-properties fo:font-family=\"");
        String font = s.font;
        if ("Serif".equals(font)) {
            font = "Times New Roman";
        } else if ("SansSerif".equals(font)) {
            font = "Arial";
        } else if ("Monospace".equals(font)) {
            font = "Courier New";
        }
        DocxWriter.esc(b, font.indexOf(' ') >= 0 ? "'" + font + "'" : font);
        b.append("\" fo:font-size=\"").append(pt(s.size)).append("\" fo:color=\"#")
                .append(hex(s.color)).append("\" fo:font-weight=\"")
                .append(s.bold ? "bold" : "normal").append("\" fo:font-style=\"")
                .append(s.italic ? "italic" : "normal").append('"');
        if (s.underline) {
            b.append(" style:text-underline-style=\"solid\" style:text-underline-width=\"auto\"")
                    .append(" style:text-underline-color=\"font-color\"");
        }
        if (s.strike) {
            b.append(" style:text-line-through-style=\"solid\"");
        }
        if (s.background != TextStyle.NO_BACKGROUND) {
            b.append(" fo:background-color=\"#").append(hex(s.background)).append('"');
        }
        if (TextStyle.SUPERSCRIPT.equals(s.script)) {
            b.append(" style:text-position=\"super 58%\"");
        } else if (TextStyle.SUBSCRIPT.equals(s.script)) {
            b.append(" style:text-position=\"sub 58%\"");
        }
        b.append("/></style:style>");
    }

    private static String hex(int rgb) {
        String h = Integer.toHexString(rgb & 0xffffff);
        while (h.length() < 6) {
            h = "0" + h;
        }
        return h;
    }

    private String textStyleName(TextStyle s) {
        TextStyle key = s.withLink(null).withRevision(null);
        String name = textStyles.get(key);
        if (name == null) {
            name = "T" + (textStyles.size() + 1);
            textStyles.put(key, name);
        }
        return name;
    }

    private String parentName(String style) {
        if ("Normal".equals(style)) {
            return "Standard";
        }
        if (style.length() == 8 && style.startsWith("Heading")) {
            char c = style.charAt(7);
            if (c >= '1' && c <= '6') {
                return "Heading_20_" + c;
            }
        }
        if ("Title".equals(style)) {
            return "Title";
        }
        if ("Quote".equals(style)) {
            return "Quotations";
        }
        if ("Code".equals(style)) {
            return "Preformatted_20_Text";
        }
        String name = namedStyles.get(style);
        if (name == null) {
            StringBuilder b = new StringBuilder("U");
            for (int i = 0; i < style.length(); i++) {
                char c = style.charAt(i);
                if (c < 0x80 && Character.isLetterOrDigit(c)) {
                    b.append(c);
                } else {
                    b.append('_').append(Integer.toHexString(c)).append('_');
                }
            }
            name = b.toString();
            namedStyles.put(style, name);
        }
        return name;
    }

    private String paraStyleName(Paragraph p) {
        StringBuilder key = new StringBuilder();
        key.append(" style:parent-style-name=\"");
        DocxWriter.esc(key, parentName(p.style));
        key.append("\"><style:paragraph-properties fo:margin-top=\"").append(pt(p.before))
                .append("\" fo:margin-bottom=\"").append(pt(p.after))
                .append("\" fo:margin-left=\"").append(pt(p.indent))
                .append("\" fo:line-height=\"").append(Math.round(p.lineSpacing * 100))
                .append("%\" fo:text-align=\"")
                .append(Paragraph.CENTER.equals(p.align) ? "center" : Paragraph.RIGHT.equals(p.align)
                        ? "end" : Paragraph.JUSTIFY.equals(p.align) ? "justify" : "start")
                .append('"');
        if (p.pageBreak) {
            key.append(" fo:break-before=\"page\"");
        }
        if (p.keepNext) {
            key.append(" fo:keep-with-next=\"always\"");
        }
        key.append("/></style:style>");
        String k = key.toString();
        String name = null;
        for (Map.Entry<String, String> e : paraStyles.entrySet()) {
            if (e.getKey().equals(k)) {
                name = e.getValue().substring(18, e.getValue().indexOf('"', 18));
                break;
            }
        }
        if (name == null) {
            name = "P" + (paraStyles.size() + 1);
            paraStyles.put(k, "<style:style style:name=\"" + name
                    + "\" style:family=\"paragraph\"" + k);
        }
        return name;
    }

    // ------------------------------------------------------------------ blocks

    private void blocks(StringBuilder b, List<Block> list) {
        // open lists, one entry per level: the kind of the list at that level
        List<String> open = new ArrayList<String>();
        for (Block block : list) {
            Paragraph p = block instanceof Paragraph ? (Paragraph) block : null;
            boolean listed = p != null && !Paragraph.NO_LIST.equals(p.list);
            if (!listed) {
                closeLists(b, open, 0);
            } else {
                int level = p.listLevel;
                closeLists(b, open, level + 1);
                if (open.size() == level + 1 && (!open.get(level).equals(p.list)
                        || p.listStart > 0)) {
                    closeLists(b, open, level);
                }
                if (open.size() == level + 1) {
                    b.append("</text:list-item><text:list-item>");
                } else {
                    while (open.size() <= level) {
                        boolean last = open.size() == level;
                        String kind = last ? p.list : Paragraph.BULLET;
                        b.append("<text:list text:style-name=\"")
                                .append(Paragraph.BULLET.equals(kind) ? "L1" : "L2").append('"');
                        if (last && Paragraph.NUMBERED.equals(kind) && p.listStart == 0) {
                            b.append(" text:continue-numbering=\"true\"");
                        }
                        b.append("><text:list-item");
                        if (last && p.listStart > 0) {
                            b.append(" text:start-value=\"").append(p.listStart).append('"');
                        }
                        b.append('>');
                        open.add(kind);
                    }
                }
            }
            if (p != null) {
                paragraph(b, p);
            } else if (block instanceof Table) {
                table(b, (Table) block);
            } else if (block instanceof Picture) {
                b.append("<text:p text:style-name=\"Standard\">");
                frame(b, (Picture) block);
                b.append("</text:p>");
            }
        }
        closeLists(b, open, 0);
    }

    private static void closeLists(StringBuilder b, List<String> open, int keep) {
        while (open.size() > keep) {
            b.append("</text:list-item></text:list>");
            open.remove(open.size() - 1);
        }
    }

    private void paragraph(StringBuilder b, Paragraph p) {
        int heading = p.style.length() == 8 && p.style.startsWith("Heading")
                ? p.headingLevel() : 0;
        String element = heading > 0 ? "text:h" : "text:p";
        b.append('<').append(element).append(" text:style-name=\"").append(paraStyleName(p))
                .append('"');
        if (heading > 0) {
            b.append(" text:outline-level=\"").append(heading).append('"');
        }
        b.append('>');
        lastSpace = true;
        Map<Integer, StringBuilder> anchors = anchors(p.id);
        RichText t = p.text;
        int offset = 0;
        for (int i = 0; i < t.runCount(); i++) {
            String s = t.runText(i);
            TextStyle style = t.runStyle(i);
            Inline object = t.runObject(i);
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
                    inline(b, object, style);
                } else {
                    span(b, s.substring(start, end), style);
                }
                start = end;
            }
            offset += s.length();
        }
        for (StringBuilder rest : anchors.values()) {
            b.append(rest);
        }
        b.append("</").append(element).append('>');
    }

    private Map<Integer, StringBuilder> anchors(String paragraphId) {
        Map<Integer, StringBuilder> out = new LinkedHashMap<Integer, StringBuilder>();
        int n = 0;
        for (Comment c : doc.comments) {
            n++;
            String name = "__Annotation__" + n;
            boolean ranged = !(c.startId.equals(c.endId) && c.start == c.end);
            if (paragraphId.equals(c.startId)) {
                StringBuilder a = marker(out, c.start);
                a.append("<office:annotation");
                if (ranged) {
                    a.append(" office:name=\"").append(name).append('"');
                }
                a.append('>');
                if (c.author != null) {
                    a.append("<dc:creator>");
                    DocxWriter.esc(a, c.author);
                    a.append("</dc:creator>");
                }
                if (c.date != null) {
                    a.append("<dc:date>");
                    DocxWriter.esc(a, c.date);
                    a.append("</dc:date>");
                }
                String text = c.text == null ? "" : c.text;
                int from = 0;
                while (true) {
                    int nl = text.indexOf('\n', from);
                    a.append("<text:p>");
                    DocxWriter.esc(a, nl < 0 ? text.substring(from) : text.substring(from, nl));
                    a.append("</text:p>");
                    if (nl < 0) {
                        break;
                    }
                    from = nl + 1;
                }
                a.append("</office:annotation>");
            }
            if (ranged && paragraphId.equals(c.endId)) {
                marker(out, c.end).append("<office:annotation-end office:name=\"").append(name)
                        .append("\"/>");
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

    private String change(Revision r, String deletedText) {
        String id = "ct" + ++changeCount;
        changes.append("<text:changed-region text:id=\"").append(id).append("\">")
                .append(r.isDeletion() ? "<text:deletion>" : "<text:insertion>")
                .append("<office:change-info><dc:creator>");
        DocxWriter.esc(changes, r.author == null ? "" : r.author);
        changes.append("</dc:creator><dc:date>");
        DocxWriter.esc(changes, r.date == null ? "1970-01-01T00:00:00" : r.date);
        changes.append("</dc:date></office:change-info>");
        if (r.isDeletion()) {
            changes.append("<text:p>");
            DocxWriter.esc(changes, deletedText);
            changes.append("</text:p></text:deletion>");
        } else {
            changes.append("</text:insertion>");
        }
        changes.append("</text:changed-region>");
        return id;
    }

    private void span(StringBuilder b, String s, TextStyle style) {
        if (style.revision != null && style.revision.isDeletion()) {
            b.append("<text:change text:change-id=\"").append(change(style.revision, s))
                    .append("\"/>");
            return;
        }
        String change = style.revision == null ? null : change(style.revision, null);
        if (change != null) {
            b.append("<text:change-start text:change-id=\"").append(change).append("\"/>");
        }
        boolean link = style.link != null && style.link.length() > 0;
        if (link) {
            b.append("<text:a xlink:type=\"simple\" xlink:href=\"");
            DocxWriter.esc(b, style.link);
            b.append("\">");
        }
        b.append("<text:span text:style-name=\"").append(textStyleName(style)).append("\">");
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ') {
                int j = i;
                while (j < s.length() && s.charAt(j) == ' ') {
                    j++;
                }
                int count = j - i;
                if (!lastSpace) {
                    b.append(' ');
                    count--;
                }
                if (count > 0) {
                    b.append("<text:s");
                    if (count > 1) {
                        b.append(" text:c=\"").append(count).append('"');
                    }
                    b.append("/>");
                }
                lastSpace = true;
                i = j;
                continue;
            }
            if (c == '\t') {
                b.append("<text:tab/>");
                lastSpace = false;
            } else if (c == '\n') {
                b.append("<text:line-break/>");
                lastSpace = true;
            } else {
                int at = b.length();
                DocxWriter.esc(b, String.valueOf(c));
                if (b.length() == at) {
                    b.append(' ');
                }
                lastSpace = false;
            }
            i++;
        }
        b.append("</text:span>");
        if (link) {
            b.append("</text:a>");
        }
        if (change != null) {
            b.append("<text:change-end text:change-id=\"").append(change).append("\"/>");
        }
    }

    private void inline(StringBuilder b, Inline o, TextStyle style) {
        if (o instanceof Field) {
            Field f = (Field) o;
            String element = null;
            String type = f.type();
            for (int i = 0; i < FIELDS.length; i += 2) {
                if (FIELDS[i].equals(type)) {
                    element = FIELDS[i + 1];
                }
            }
            if (element == null) {
                span(b, f.result == null ? "" : f.result, style);
                return;
            }
            b.append("<text:span text:style-name=\"").append(textStyleName(style))
                    .append("\"><text:").append(element);
            if (f.locked) {
                b.append(" text:fixed=\"true\"");
            }
            if (!f.instruction.equals(type)) {
                // ODF has no place for Word's switches; keep them for our own reader
                b.append(" cn1:instruction=\"");
                DocxWriter.esc(b, f.instruction);
                b.append('"');
            }
            b.append('>');
            DocxWriter.esc(b, f.result);
            b.append("</text:").append(element).append("></text:span>");
            lastSpace = false;
        } else if (o instanceof Note) {
            Note n = (Note) o;
            int id = ++noteCount;
            boolean end = Note.ENDNOTE.equals(n.kind);
            b.append("<text:span text:style-name=\"").append(textStyleName(style))
                    .append("\"><text:note text:id=\"").append(end ? "edn" : "ftn").append(id)
                    .append("\" text:note-class=\"").append(end ? "endnote" : "footnote")
                    .append("\"><text:note-citation");
            if (n.mark != null && n.mark.length() > 0) {
                b.append(" text:label=\"");
                DocxWriter.esc(b, n.mark);
                b.append("\">");
                DocxWriter.esc(b, n.mark);
            } else {
                b.append('>').append(id);
            }
            b.append("</text:note-citation><text:note-body>");
            boolean saved = lastSpace;
            if (n.blocks.isEmpty()) {
                b.append("<text:p/>");
            } else {
                blocks(b, n.blocks);
            }
            lastSpace = saved;
            b.append("</text:note-body></text:note></text:span>");
            lastSpace = false;
        } else if (o instanceof Picture) {
            if (style.link != null && style.link.length() > 0) {
                b.append("<text:a xlink:type=\"simple\" xlink:href=\"");
                DocxWriter.esc(b, style.link);
                b.append("\">");
                frame(b, (Picture) o);
                b.append("</text:a>");
            } else {
                frame(b, (Picture) o);
            }
            lastSpace = false;
        } else if (o instanceof EmbeddedComponent) {
            Picture p = ((EmbeddedComponent) o).toPicture("component");
            if (p != null) {
                inline(b, p, style);
            }
        }
    }

    private void frame(StringBuilder b, Picture p) {
        String ext = "image/png".equals(p.mimeType) ? "png" : "image/jpeg".equals(p.mimeType)
                ? "jpg" : "image/gif".equals(p.mimeType) ? "gif" : "bin";
        String name = "Pictures/image" + pictureNames.size() + "." + ext;
        pictureNames.add(name);
        pictureData.add(p.data());
        pictureTypes.add(p.mimeType);
        int n = ++frameCount;
        b.append("<draw:frame draw:name=\"Image").append(n)
                .append("\" text:anchor-type=\"as-char\" svg:width=\"").append(pt(p.width))
                .append("\" svg:height=\"").append(pt(p.height))
                .append("\" draw:z-index=\"0\"><draw:image xlink:href=\"").append(name)
                .append("\" xlink:type=\"simple\" xlink:show=\"embed\" xlink:actuate=\"onLoad\"/>");
        if (p.description.length() > 0) {
            b.append("<svg:desc>");
            DocxWriter.esc(b, p.description);
            b.append("</svg:desc>");
        }
        b.append("</draw:frame>");
    }

    // ------------------------------------------------------------------ tables

    private void table(StringBuilder b, Table t) {
        int columns = t.columns();
        b.append("<table:table table:name=\"Table").append(++tableCount)
                .append("\"><table:table-column table:number-columns-repeated=\"")
                .append(Math.max(1, columns)).append("\"/>");
        for (int r = 0; r < t.rows(); r++) {
            if (r == 0 && t.headerRows > 0) {
                b.append("<table:table-header-rows>");
            }
            b.append("<table:table-row>");
            for (int c = 0; c < columns; c++) {
                if (t.covered(r, c)) {
                    b.append("<table:covered-table-cell/>");
                    continue;
                }
                Table.Merge m = t.span(r, c);
                b.append("<table:table-cell office:value-type=\"string\"");
                if (m != null && m.columns > 1) {
                    b.append(" table:number-columns-spanned=\"").append(m.columns).append('"');
                }
                if (m != null && m.rows > 1) {
                    b.append(" table:number-rows-spanned=\"").append(m.rows).append('"');
                }
                b.append('>');
                List<Block> cell = t.cell(r, c);
                if (cell.isEmpty()) {
                    b.append("<text:p/>");
                } else {
                    blocks(b, cell);
                }
                b.append("</table:table-cell>");
            }
            b.append("</table:table-row>");
            if (r == t.headerRows - 1) {
                b.append("</table:table-header-rows>");
            }
        }
        b.append("</table:table>");
    }
}
