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
package com.codename1.presentation;

import com.codename1.impl.office.OdfText;
import com.codename1.impl.xml.XmlOut;
import com.codename1.io.zip.ZipArchiveOutputStream;
import com.codename1.util.StringUtil;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.Table;
import com.codename1.wordprocessing.TextStyle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

/// OpenDocument presentation (`.odp`). Every shape, paragraph and run carries an automatic
/// style with its complete formatting, so the deck reads the same whether or not an
/// application resolves the master page. Charts are embedded chart objects with their data
/// in a local table, diagrams are written as the group of shapes they render to, and builds
/// become a main animation sequence in the `anim:` vocabulary.
final class OdpWriter {
    private static final String MIME = "application/vnd.oasis.opendocument.presentation";
    private static final String CHART_MIME = "application/vnd.oasis.opendocument.chart";
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
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
            + " xmlns:chart=\"urn:oasis:names:tc:opendocument:xmlns:chart:1.0\""
            + " xmlns:presentation=\"urn:oasis:names:tc:opendocument:xmlns:presentation:1.0\""
            + " xmlns:smil=\"urn:oasis:names:tc:opendocument:xmlns:smil-compatible:1.0\""
            + " xmlns:anim=\"urn:oasis:names:tc:opendocument:xmlns:animation:1.0\""
            + " office:version=\"1.3\"";
    private static final int[] PALETTE = {0x4472c4, 0xed7d31, 0xa5a5a5, 0xffc000, 0x5b9bd5,
        0x70ad47};
    private static final String[][] PRESETS = {{"rect", "rectangle"}, {"ellipse", "ellipse"},
        {"roundRect", "round-rectangle"}, {"triangle", "isosceles-triangle"},
        {"rtTriangle", "right-triangle"}, {"diamond", "diamond"}, {"star5", "star5"},
        {"hexagon", "hexagon"}, {"octagon", "octagon"}, {"rightArrow", "right-arrow"},
        {"leftArrow", "left-arrow"}, {"upArrow", "up-arrow"}, {"downArrow", "down-arrow"},
        {"parallelogram", "parallelogram"}, {"trapezoid", "trapezoid"}, {"cloud", "cloud"},
        {"heart", "heart"}, {"can", "can"}, {"cube", "cube"}, {"star4", "star4"},
        {"star8", "star8"}, {"plus", "cross"}, {"smileyFace", "smiley"}, {"sun", "sun"},
        {"moon", "moon"}, {"lightningBolt", "lightning"}};

    /// A run of builds that start together, at `begin` seconds into their click group.
    private static final class Step {
        final double begin;
        final List<Build> builds = new ArrayList<Build>();

        Step(double begin) {
            this.begin = begin;
        }

        double end() {
            double e = 0;
            for (Build b : builds) {
                e = Math.max(e, (b.delay + duration(b)) / 1000.0);
            }
            return begin + e;
        }
    }

    /// The builds one click (or the slide's start) sets off.
    private static final class Group {
        final boolean click;
        final List<Step> steps = new ArrayList<Step>();

        Group(boolean click) {
            this.click = click;
        }
    }

    private final Presentation pres;
    private final ZipArchiveOutputStream zip;
    private final Map<String, String> autoStyles = new LinkedHashMap<String, String>();
    private final Map<String, String> gradients = new LinkedHashMap<String, String>();
    private final Map<Picture, String> fillImages = new LinkedHashMap<Picture, String>();
    private final Map<Picture, String> media = new HashMap<Picture, String>();
    private final List<String[]> manifest = new ArrayList<String[]>();
    private final Map<String, String> xmlIds = new HashMap<String, String>();
    private int objects;

    private OdpWriter(Presentation p, OutputStream out) {
        pres = p;
        zip = new ZipArchiveOutputStream(out);
    }

    static void write(Presentation p, OutputStream out) throws IOException {
        new OdpWriter(p, out).run();
    }

    // ------------------------------------------------------------------ helpers

    private static String pt(float v) {
        return OdfText.length(v);
    }

    private static String hex(int rgb) {
        return "#" + OdfText.hex(rgb);
    }

    private static String num(double v) {
        if (v == (long) v) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    private static String percent(float fraction) {
        return Math.round(Math.max(0, Math.min(1, fraction)) * 100) + "%";
    }

    private static String seconds(int ms) {
        return num(ms / 1000.0) + "s";
    }

    private static int duration(Build b) {
        return b.duration > 0 ? b.duration : 500;
    }

    private static String attr(String name, String value) {
        StringBuilder b = new StringBuilder();
        b.append(' ').append(name).append("=\"");
        OdfText.escape(b, value);
        return b.append('"').toString();
    }

    private void put(String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data, 0, data.length);
        zip.closeEntry();
    }

    /// The name of an automatic style with these properties, defining it once.
    private String style(String family, String prefix, String properties) {
        String key = family + '\n' + properties;
        String name = autoStyles.get(key);
        if (name == null) {
            name = prefix + (autoStyles.size() + 1);
            autoStyles.put(key, name);
        }
        return name;
    }

    private static String extensionOf(String mime) {
        if (mime == null) {
            return "bin";
        }
        String m = mime.trim();
        if ("image/jpeg".equalsIgnoreCase(m) || "image/jpg".equalsIgnoreCase(m)) {
            return "jpg";
        }
        if ("image/png".equalsIgnoreCase(m)) {
            return "png";
        }
        if ("image/gif".equalsIgnoreCase(m)) {
            return "gif";
        }
        if ("image/bmp".equalsIgnoreCase(m)) {
            return "bmp";
        }
        if ("image/tiff".equalsIgnoreCase(m)) {
            return "tif";
        }
        return "image/svg+xml".equalsIgnoreCase(m) ? "svg" : "bin";
    }

    /// The package path (or external URL) of a picture, writing its bytes once.
    private String picture(Picture p) throws IOException {
        if (p.isLinked()) {
            return p.source;
        }
        String path = media.get(p);
        if (path == null) {
            path = "Pictures/image" + (media.size() + 1) + "." + extensionOf(p.mimeType);
            media.put(p, path);
            put(path, p.data());
            manifest.add(new String[] {path, p.mimeType == null ? "application/octet-stream"
                    : p.mimeType});
        }
        return path;
    }

    // ------------------------------------------------------------------ package

    private void run() throws IOException {
        byte[] mime = StringUtil.getBytes(MIME);
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

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        XmlOut x = new XmlOut(body);
        for (int i = 0; i < pres.slides.size(); i++) {
            page(x, pres.slides.get(i), i + 1);
        }
        x.flush();
        content(body.toByteArray());
        put("styles.xml", StringUtil.getBytes(styles()));
        put("meta.xml", StringUtil.getBytes(meta()));
        put("META-INF/manifest.xml", StringUtil.getBytes(manifestXml()));
        zip.finish();
    }

    private void content(byte[] body) throws IOException {
        StringBuilder b = new StringBuilder(XML);
        b.append("<office:document-content").append(NS).append("><office:automatic-styles>");
        for (Map.Entry<String, String> e : autoStyles.entrySet()) {
            String key = e.getKey();
            int nl = key.indexOf('\n');
            b.append("<style:style").append(attr("style:name", e.getValue()))
                    .append(attr("style:family", key.substring(0, nl))).append('>')
                    .append(key.substring(nl + 1)).append("</style:style>");
        }
        listStyle(b, "L1", true);
        listStyle(b, "L2", false);
        b.append("</office:automatic-styles><office:body><office:presentation>");
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        byte[] head = StringUtil.getBytes(b.toString());
        all.write(head, 0, head.length);
        all.write(body, 0, body.length);
        byte[] tail = StringUtil.getBytes("</office:presentation></office:body>"
                + "</office:document-content>");
        all.write(tail, 0, tail.length);
        put("content.xml", all.toByteArray());
    }

    private static void listStyle(StringBuilder b, String name, boolean bullet) {
        b.append("<text:list-style style:name=\"").append(name).append("\">");
        for (int level = 1; level <= 9; level++) {
            if (bullet) {
                b.append("<text:list-level-style-bullet text:level=\"").append(level)
                        .append("\" text:bullet-char=\"&#8226;\">");
            } else {
                b.append("<text:list-level-style-number text:level=\"").append(level)
                        .append("\" style:num-format=\"1\" style:num-suffix=\".\">");
            }
            b.append("<style:list-level-properties text:space-before=\"")
                    .append(pt(18 * (level - 1))).append("\" text:min-label-width=\"18pt\"/>")
                    .append(bullet ? "</text:list-level-style-bullet>"
                            : "</text:list-level-style-number>");
        }
        b.append("</text:list-style>");
    }

    private String styles() {
        StringBuilder b = new StringBuilder(XML);
        b.append("<office:document-styles").append(NS).append("><office:styles>")
                .append("<draw:marker draw:name=\"Arrow\" svg:viewBox=\"0 0 20 30\""
                        + " svg:d=\"M10 0l-10 30h20z\"/>")
                .append("<draw:marker draw:name=\"Line_20_Arrow\" draw:display-name=\"Line Arrow\""
                        + " svg:viewBox=\"0 0 20 30\" svg:d=\"M10 0L20 27L17 30L10 9L3 30L0 27z\"/>")
                .append("<draw:marker draw:name=\"Circle\" svg:viewBox=\"0 0 20 20\""
                        + " svg:d=\"M10 0A10 10 0 1 1 10 20A10 10 0 1 1 10 0z\"/>")
                .append("<draw:stroke-dash draw:name=\"Dash\" draw:style=\"rect\" draw:dots1=\"1\""
                        + " draw:dots1-length=\"300%\" draw:distance=\"200%\"/>")
                .append("<draw:stroke-dash draw:name=\"Dot\" draw:style=\"rect\" draw:dots1=\"1\""
                        + " draw:dots1-length=\"100%\" draw:distance=\"100%\"/>");
        for (Map.Entry<String, String> g : gradients.entrySet()) {
            b.append("<draw:gradient").append(attr("draw:name", g.getValue())).append(g.getKey())
                    .append("/>");
        }
        for (Map.Entry<Picture, String> f : fillImages.entrySet()) {
            String href = media.get(f.getKey());
            if (href == null) {
                href = f.getKey().source;
            }
            b.append("<draw:fill-image").append(attr("draw:name", f.getValue()))
                    .append(attr("xlink:href", href))
                    .append(" xlink:type=\"simple\" xlink:show=\"embed\" xlink:actuate=\"onLoad\"/>");
        }
        b.append("<style:default-style style:family=\"graphic\">");
        OdfText.textProperties(b, TextStyle.DEFAULT);
        b.append("</style:default-style>");
        b.append("</office:styles><office:automatic-styles>")
                .append("<style:page-layout style:name=\"PM1\"><style:page-layout-properties")
                .append(" fo:margin-top=\"0pt\" fo:margin-bottom=\"0pt\" fo:margin-left=\"0pt\"")
                .append(" fo:margin-right=\"0pt\"").append(attr("fo:page-width", pt(pres.width)))
                .append(attr("fo:page-height", pt(pres.height)))
                .append(attr("style:print-orientation",
                        pres.width >= pres.height ? "landscape" : "portrait"))
                .append("/></style:page-layout>")
                .append("<style:page-layout style:name=\"PM2\"><style:page-layout-properties"
                        + " fo:margin-top=\"0pt\" fo:margin-bottom=\"0pt\" fo:margin-left=\"0pt\""
                        + " fo:margin-right=\"0pt\" fo:page-width=\"595pt\""
                        + " fo:page-height=\"842pt\" style:print-orientation=\"portrait\"/>"
                        + "</style:page-layout>")
                .append("<style:style style:name=\"Mdp\" style:family=\"drawing-page\">"
                        + "<style:drawing-page-properties draw:fill=\"solid\""
                        + " draw:fill-color=\"#ffffff\"/></style:style>")
                .append("</office:automatic-styles><office:master-styles>")
                .append("<style:master-page style:name=\"Default\" style:page-layout-name=\"PM1\""
                        + " draw:style-name=\"Mdp\"><presentation:notes"
                        + " style:page-layout-name=\"PM2\"/></style:master-page>")
                .append("</office:master-styles></office:document-styles>");
        return b.toString();
    }

    private String meta() {
        StringBuilder b = new StringBuilder(XML);
        b.append("<office:document-meta").append(NS).append("><office:meta>")
                .append("<meta:generator>Codename One</meta:generator>");
        if (pres.title != null && pres.title.length() > 0) {
            b.append("<dc:title>");
            OdfText.escape(b, pres.title);
            b.append("</dc:title>");
        }
        return b.append("</office:meta></office:document-meta>").toString();
    }

    private String manifestXml() {
        StringBuilder b = new StringBuilder(XML);
        b.append("<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:")
                .append("manifest:1.0\" manifest:version=\"1.3\"><manifest:file-entry ")
                .append("manifest:full-path=\"/\" manifest:version=\"1.3\" manifest:media-type=\"")
                .append(MIME).append("\"/>");
        String[] xml = {"content.xml", "styles.xml", "meta.xml"};
        for (String part : xml) {
            b.append("<manifest:file-entry manifest:full-path=\"").append(part)
                    .append("\" manifest:media-type=\"text/xml\"/>");
        }
        for (String[] e : manifest) {
            b.append("<manifest:file-entry").append(attr("manifest:full-path", e[0]))
                    .append(attr("manifest:media-type", e[1])).append("/>");
        }
        return b.append("</manifest:manifest>").toString();
    }

    // ------------------------------------------------------------------ fills and lines

    private String gradient(Fill f) {
        float a = (90 - f.angle) % 360;
        if (a < 0) {
            a += 360;
        }
        String key = " draw:style=\"linear\"" + attr("draw:start-color", hex(f.color))
                + attr("draw:end-color", hex(f.color2))
                + " draw:start-intensity=\"100%\" draw:end-intensity=\"100%\""
                + attr("draw:angle", String.valueOf(Math.round(a * 10))) + " draw:border=\"0%\"";
        String name = gradients.get(key);
        if (name == null) {
            name = "Gradient_" + (gradients.size() + 1);
            gradients.put(key, name);
        }
        return name;
    }

    private void fill(StringBuilder g, Fill f) throws IOException {
        if (f == null || f.kind == Fill.NONE || f.kind == Fill.PICTURE && f.picture == null) {
            g.append(" draw:fill=\"none\"");
            return;
        }
        if (f.kind == Fill.SOLID) {
            g.append(" draw:fill=\"solid\"").append(attr("draw:fill-color", hex(f.color)));
        } else if (f.kind == Fill.GRADIENT) {
            g.append(" draw:fill=\"gradient\"").append(attr("draw:fill-gradient-name",
                    gradient(f)));
        } else {
            picture(f.picture);
            String name = fillImages.get(f.picture);
            if (name == null) {
                name = "Image_" + (fillImages.size() + 1);
                fillImages.put(f.picture, name);
            }
            g.append(" draw:fill=\"bitmap\"").append(attr("draw:fill-image-name", name))
                    .append(" style:repeat=\"stretch\"");
        }
        if (f.alpha < 1) {
            g.append(attr("draw:opacity", percent(f.alpha)));
        }
    }

    private static String marker(String arrow) {
        if (LineStyle.ARROW_TRIANGLE.equals(arrow)) {
            return "Arrow";
        }
        if (LineStyle.ARROW_OPEN.equals(arrow)) {
            return "Line_20_Arrow";
        }
        return LineStyle.ARROW_OVAL.equals(arrow) ? "Circle" : null;
    }

    private static void stroke(StringBuilder g, LineStyle l) {
        if (l == null || !l.isVisible()) {
            g.append(" draw:stroke=\"none\"");
            return;
        }
        if (LineStyle.DASH.equals(l.dash) || LineStyle.DOT.equals(l.dash)) {
            g.append(" draw:stroke=\"dash\"").append(attr("draw:stroke-dash",
                    LineStyle.DASH.equals(l.dash) ? "Dash" : "Dot"));
        } else {
            g.append(" draw:stroke=\"solid\"");
        }
        g.append(attr("svg:stroke-width", pt(l.width)))
                .append(attr("svg:stroke-color", hex(l.color)));
        if (l.alpha < 1) {
            g.append(attr("svg:stroke-opacity", percent(l.alpha)));
        }
        String markerWidth = pt(Math.max(6, l.width * 3));
        String head = marker(l.head);
        String tail = marker(l.tail);
        if (head != null) {
            g.append(attr("draw:marker-start", head))
                    .append(attr("draw:marker-start-width", markerWidth));
        }
        if (tail != null) {
            g.append(attr("draw:marker-end", tail))
                    .append(attr("draw:marker-end-width", markerWidth));
        }
    }

    private static void textFrame(StringBuilder g, TextFrame t) {
        String anchor = TextFrame.MIDDLE.equals(t.anchor) ? "middle"
                : TextFrame.BOTTOM.equals(t.anchor) ? "bottom" : "top";
        g.append(attr("draw:textarea-vertical-align", anchor))
                .append(attr("fo:padding-left", pt(t.left)))
                .append(attr("fo:padding-top", pt(t.top)))
                .append(attr("fo:padding-right", pt(t.right)))
                .append(attr("fo:padding-bottom", pt(t.bottom)))
                .append(attr("fo:wrap-option", t.wrap ? "wrap" : "no-wrap"))
                .append(attr("draw:auto-grow-height",
                        TextFrame.FIT_RESIZE.equals(t.autofit) ? "true" : "false"));
        if (TextFrame.FIT_SHRINK.equals(t.autofit)) {
            g.append(" style:shrink-to-fit=\"true\"");
        }
    }

    private String graphicStyle(String properties) {
        return style("graphic", "gr", "<style:graphic-properties" + properties + "/>");
    }

    // ------------------------------------------------------------------ pages

    private void page(XmlOut x, Slide s, int n) throws IOException {
        StringBuilder p = new StringBuilder("<style:drawing-page-properties");
        if (s.background != null && s.background.kind != Fill.NONE) {
            fill(p, s.background);
            p.append(" presentation:background-visible=\"true\"");
        }
        p.append(" presentation:background-objects-visible=\"true\"");
        transition(p, s.transition);
        if (s.hidden) {
            p.append(" presentation:visibility=\"hidden\"");
        }
        p.append("/>");
        String pageStyle = style("drawing-page", "dp", p.toString());
        String name = s.id != null && s.id.length() > 0 ? s.id : "Slide " + n;
        x.raw("<draw:page").attr("draw:name", name).attr("draw:style-name", pageStyle)
                .attr("draw:master-page-name", "Default").raw(">");
        for (SlideElement e : s.elements) {
            element(x, e);
        }
        timing(x, s.builds);
        if (s.notes != null && !s.notes.isEmpty()) {
            float ratio = pres.width > 0 ? pres.height / pres.width : 0.5625f;
            x.raw("<presentation:notes><draw:page-thumbnail")
                    .attr("draw:style-name", graphicStyle(" draw:fill=\"none\""))
                    .attr("draw:page-number", n).attr("presentation:class", "page")
                    .attr("svg:x", "60pt").attr("svg:y", "60pt").attr("svg:width", "475pt")
                    .attr("svg:height", pt(Math.min(300, 475 * ratio)))
                    .raw("/><draw:frame").attr("presentation:class", "notes")
                    .attr("svg:x", "60pt").attr("svg:y", "380pt").attr("svg:width", "475pt")
                    .attr("svg:height", "400pt").raw("><draw:text-box>");
            paragraphs(x, s.notes);
            x.raw("</draw:text-box></draw:frame></presentation:notes>");
        }
        x.raw("</draw:page>");
    }

    private static void transition(StringBuilder p, Transition t) {
        if (t == null || Transition.DEFAULT.equals(t)) {
            return;
        }
        String type = t.advanceAfter < 0 ? "manual"
                : t.advanceOnClick ? "semi-automatic" : "automatic";
        p.append(attr("presentation:transition-type", type));
        if (t.advanceAfter >= 0) {
            p.append(attr("presentation:duration", "PT" + num(t.advanceAfter / 1000.0) + "S"));
        }
        if (Transition.NONE.equals(t.type)) {
            return;
        }
        p.append(attr("presentation:transition-speed",
                t.duration <= 500 ? "fast" : t.duration <= 750 ? "medium" : "slow"));
        String from = t.direction;
        boolean horizontal = !Transition.FROM_TOP.equals(from)
                && !Transition.FROM_BOTTOM.equals(from);
        boolean reverse = Transition.FROM_RIGHT.equals(from) || Transition.FROM_BOTTOM.equals(from);
        String side = Transition.FROM_LEFT.equals(from) ? "fromLeft"
                : Transition.FROM_TOP.equals(from) ? "fromTop"
                : Transition.FROM_BOTTOM.equals(from) ? "fromBottom" : "fromRight";
        if (Transition.FADE.equals(t.type)) {
            p.append(" smil:type=\"fade\" smil:subtype=\"crossfade\"");
        } else if (Transition.PUSH.equals(t.type)) {
            p.append(" smil:type=\"pushWipe\"").append(attr("smil:subtype", side));
        } else if (Transition.COVER.equals(t.type)) {
            p.append(" smil:type=\"slideWipe\"").append(attr("smil:subtype", side));
        } else if (Transition.UNCOVER.equals(t.type)) {
            p.append(" smil:type=\"slideWipe\"").append(attr("smil:subtype", side))
                    .append(" smil:direction=\"reverse\"");
        } else if (Transition.WIPE.equals(t.type)) {
            p.append(" smil:type=\"barWipe\"").append(attr("smil:subtype",
                    horizontal ? "leftToRight" : "topToBottom"));
            if (reverse) {
                p.append(" smil:direction=\"reverse\"");
            }
        } else if (Transition.ZOOM.equals(t.type)) {
            p.append(" smil:type=\"irisWipe\" smil:subtype=\"rectangle\"");
        }
    }

    // ------------------------------------------------------------------ elements

    /// The `xml:id` and `draw:id` an element is targeted by, for elements with a model id.
    private void ids(XmlOut x, SlideElement e) throws IOException {
        if (e.id == null) {
            return;
        }
        String id = xmlIds.get(e.id);
        if (id == null) {
            id = "id" + (xmlIds.size() + 1);
            xmlIds.put(e.id, id);
        }
        x.attr("xml:id", id).attr("draw:id", id);
    }

    private static void name(XmlOut x, SlideElement e) throws IOException {
        if (e.name != null && e.name.length() > 0) {
            x.attr("draw:name", e.name);
        }
    }

    /// Position and size, as a transform when the element is rotated about its centre.
    private static void geometry(XmlOut x, SlideElement e) throws IOException {
        float w = Math.max(0, e.width);
        float h = Math.max(0, e.height);
        if (e.rotation == 0) {
            x.attr("svg:x", pt(e.x)).attr("svg:y", pt(e.y));
        }
        x.attr("svg:width", pt(w)).attr("svg:height", pt(h));
        if (e.rotation != 0) {
            double r = Math.toRadians(e.rotation);
            double cos = Math.cos(r);
            double sin = Math.sin(r);
            double tx = e.x + w / 2.0 - w / 2.0 * cos + h / 2.0 * sin;
            double ty = e.y + h / 2.0 - w / 2.0 * sin - h / 2.0 * cos;
            double odf = Math.toRadians((360 - e.rotation % 360) % 360);
            x.attr("draw:transform", "rotate (" + num(odf) + ") translate ("
                    + pt((float) tx) + " " + pt((float) ty) + ")");
        }
    }

    private void element(XmlOut x, SlideElement e) throws IOException {
        if (e instanceof ShapeElement) {
            shape(x, (ShapeElement) e);
        } else if (e instanceof PictureElement) {
            pictureElement(x, (PictureElement) e);
        } else if (e instanceof TableElement) {
            table(x, (TableElement) e);
        } else if (e instanceof ChartElement) {
            chart(x, (ChartElement) e);
        } else if (e instanceof DiagramElement) {
            group(x, ((DiagramElement) e).toGroup(), e);
        } else if (e instanceof GroupElement) {
            group(x, (GroupElement) e, e);
        }
    }

    private void group(XmlOut x, GroupElement g, SlideElement original) throws IOException {
        x.raw("<draw:g");
        name(x, original);
        ids(x, original);
        x.raw(">");
        for (SlideElement c : g.children) {
            element(x, c);
        }
        x.raw("</draw:g>");
    }

    private static boolean isTextBox(ShapeElement s) {
        if (s.text == null) {
            return false;
        }
        if (s.placeholder != null && s.placeholder.length() > 0) {
            return true;
        }
        boolean rect = s.geometry == null || s.geometry.length() == 0 || "rect".equals(s.geometry);
        return rect && (s.fill == null || !s.fill.isVisible())
                && (s.line == null || !s.line.isVisible());
    }

    private static String presentationClass(String placeholder) {
        if (ShapeElement.TITLE.equals(placeholder)) {
            return "title";
        }
        return ShapeElement.SUBTITLE.equals(placeholder) ? "subtitle" : "outline";
    }

    private static String odfGeometry(String geometry) {
        String g = geometry == null || geometry.length() == 0 ? "rect" : geometry;
        for (String[] p : PRESETS) {
            if (p[0].equals(g)) {
                return p[1];
            }
        }
        return "ooxml-" + g;
    }

    private void shape(XmlOut x, ShapeElement s) throws IOException {
        if (s.isLine()) {
            line(x, s);
            return;
        }
        StringBuilder g = new StringBuilder();
        fill(g, s.fill);
        stroke(g, s.line);
        if (s.text != null) {
            textFrame(g, s.text);
        }
        String st = graphicStyle(g.toString());
        if (isTextBox(s)) {
            x.raw("<draw:frame").attr("draw:style-name", st);
            if (s.placeholder != null && s.placeholder.length() > 0) {
                x.attr("presentation:class", presentationClass(s.placeholder))
                        .attr("presentation:user-transformed", "true");
            }
            name(x, s);
            ids(x, s);
            geometry(x, s);
            x.raw("><draw:text-box>");
            paragraphs(x, s.text.blocks);
            x.raw("</draw:text-box></draw:frame>");
            return;
        }
        x.raw("<draw:custom-shape").attr("draw:style-name", st);
        name(x, s);
        ids(x, s);
        geometry(x, s);
        x.raw(">");
        if (s.text != null) {
            paragraphs(x, s.text.blocks);
        }
        String type = odfGeometry(s.geometry);
        x.raw("<draw:enhanced-geometry svg:viewBox=\"0 0 21600 21600\"").attr("draw:type", type)
                .attr("draw:mirror-horizontal", s.flipH ? "true" : null)
                .attr("draw:mirror-vertical", s.flipV ? "true" : null);
        int n = s.adjustCount();
        if (n > 0 && type.startsWith("ooxml-")) {
            StringBuilder m = new StringBuilder();
            for (int i = 0; i < n; i++) {
                if (i > 0) {
                    m.append(' ');
                }
                m.append(Math.round(s.adjust(i, 0) * 100000));
            }
            x.attr("draw:modifiers", m.toString());
        }
        x.raw("/></draw:custom-shape>");
    }

    private void line(XmlOut x, ShapeElement s) throws IOException {
        StringBuilder g = new StringBuilder(" draw:fill=\"none\"");
        stroke(g, s.line);
        double x1 = s.flipH ? s.x + s.width : s.x;
        double x2 = s.flipH ? s.x : s.x + s.width;
        double y1 = s.flipV ? s.y + s.height : s.y;
        double y2 = s.flipV ? s.y : s.y + s.height;
        if (s.rotation != 0) {
            double cx = s.x + s.width / 2.0;
            double cy = s.y + s.height / 2.0;
            double r = Math.toRadians(s.rotation);
            double cos = Math.cos(r);
            double sin = Math.sin(r);
            double ax = x1 - cx;
            double ay = y1 - cy;
            double bx = x2 - cx;
            double by = y2 - cy;
            x1 = cx + ax * cos - ay * sin;
            y1 = cy + ax * sin + ay * cos;
            x2 = cx + bx * cos - by * sin;
            y2 = cy + bx * sin + by * cos;
        }
        x.raw("<draw:line").attr("draw:style-name", graphicStyle(g.toString()));
        name(x, s);
        ids(x, s);
        x.attr("svg:x1", pt((float) x1)).attr("svg:y1", pt((float) y1))
                .attr("svg:x2", pt((float) x2)).attr("svg:y2", pt((float) y2)).raw(">");
        if (s.text != null) {
            paragraphs(x, s.text.blocks);
        }
        x.raw("</draw:line>");
    }

    private void pictureElement(XmlOut x, PictureElement p) throws IOException {
        StringBuilder g = new StringBuilder(" draw:fill=\"none\"");
        stroke(g, p.line);
        Picture pic = p.picture;
        if (p.isCropped() && pic != null && pic.width > 0 && pic.height > 0) {
            g.append(attr("fo:clip", "rect(" + pt(p.cropTop * pic.height) + ", "
                    + pt(p.cropRight * pic.width) + ", " + pt(p.cropBottom * pic.height) + ", "
                    + pt(p.cropLeft * pic.width) + ")"));
        }
        x.raw("<draw:frame").attr("draw:style-name", graphicStyle(g.toString()));
        name(x, p);
        ids(x, p);
        geometry(x, p);
        x.raw(">");
        if (pic != null) {
            x.raw("<draw:image").attr("xlink:href", picture(pic))
                    .raw(" xlink:type=\"simple\" xlink:show=\"embed\" xlink:actuate=\"onLoad\"/>");
        }
        x.raw("</draw:frame>");
    }

    // ------------------------------------------------------------------ tables

    private void table(XmlOut x, TableElement te) throws IOException {
        Table t = te.table;
        int rows = Math.max(1, t.rows());
        int cols = Math.max(1, t.columns());
        x.raw("<draw:frame").attr("draw:style-name",
                graphicStyle(" draw:fill=\"none\" draw:stroke=\"none\""));
        name(x, te);
        ids(x, te);
        geometry(x, te);
        x.raw("><table:table>");
        List<Float> widths = t.columnWidths();
        float total = 0;
        if (widths.size() == cols) {
            for (Float w : widths) {
                total += w.floatValue();
            }
        }
        for (int c = 0; c < cols; c++) {
            float w = total > 0 ? te.width * widths.get(c).floatValue() / total : te.width / cols;
            x.raw("<table:table-column").attr("table:style-name", style("table-column", "co",
                    "<style:table-column-properties" + attr("style:column-width", pt(w)) + "/>"))
                    .raw("/>");
        }
        String rowStyle = style("table-row", "ro", "<style:table-row-properties"
                + attr("style:row-height", pt(te.height / rows)) + "/>");
        String border = " fo:border=\"1pt solid #808080\"";
        String bodyCell = style("table-cell", "ce", "<style:graphic-properties draw:fill=\"none\"/>"
                + "<style:paragraph-properties" + border + "/>");
        String headCell = style("table-cell", "ce", "<style:graphic-properties draw:fill=\"solid\""
                + " draw:fill-color=\"#e7ecf2\"/><style:paragraph-properties" + border + "/>");
        for (int r = 0; r < rows; r++) {
            x.raw("<table:table-row").attr("table:style-name", rowStyle).raw(">");
            for (int c = 0; c < cols; c++) {
                boolean inside = r < t.rows() && c < t.columns();
                if (inside && t.covered(r, c)) {
                    x.raw("<table:covered-table-cell/>");
                    continue;
                }
                x.raw("<table:table-cell").attr("table:style-name",
                        r < t.headerRows ? headCell : bodyCell);
                Table.Merge m = inside ? t.span(r, c) : null;
                if (m != null) {
                    x.attr("table:number-columns-spanned",
                            m.columns > 1 ? String.valueOf(m.columns) : null)
                            .attr("table:number-rows-spanned",
                                    m.rows > 1 ? String.valueOf(m.rows) : null);
                }
                x.raw(">");
                paragraphs(x, inside ? t.cell(r, c) : null);
                x.raw("</table:table-cell>");
            }
            x.raw("</table:table-row>");
        }
        x.raw("</table:table></draw:frame>");
    }

    // ------------------------------------------------------------------ charts

    private static String column(int index) {
        StringBuilder b = new StringBuilder();
        int n = index + 1;
        while (n > 0) {
            int d = (n - 1) % 26;
            b.insert(0, (char) ('A' + d));
            n = (n - 1) / 26;
        }
        return b.toString();
    }

    private static String chartClass(String kind) {
        if (ChartElement.LINE.equals(kind)) {
            return "chart:line";
        }
        if (ChartElement.AREA.equals(kind)) {
            return "chart:area";
        }
        if (ChartElement.PIE.equals(kind)) {
            return "chart:circle";
        }
        if (ChartElement.DOUGHNUT.equals(kind)) {
            return "chart:ring";
        }
        if (ChartElement.SCATTER.equals(kind)) {
            return "chart:scatter";
        }
        return ChartElement.RADAR.equals(kind) ? "chart:radar" : "chart:bar";
    }

    private static int color(ChartElement c, int i) {
        int rgb = c.seriesColor(i);
        return rgb >= 0 ? rgb : PALETTE[i % PALETTE.length];
    }

    private void chart(XmlOut x, ChartElement c) throws IOException {
        objects++;
        String dir = "Object " + objects;
        x.raw("<draw:frame").attr("draw:style-name",
                graphicStyle(" draw:fill=\"none\" draw:stroke=\"none\""));
        name(x, c);
        ids(x, c);
        geometry(x, c);
        x.raw("><draw:object").attr("xlink:href", "./" + dir)
                .raw(" xlink:type=\"simple\" xlink:show=\"embed\" xlink:actuate=\"onLoad\"/>"
                        + "</draw:frame>");
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        XmlOut o = new XmlOut(b);
        chartContent(o, c);
        o.flush();
        put(dir + "/content.xml", b.toByteArray());
        manifest.add(new String[] {dir + "/", CHART_MIME});
        manifest.add(new String[] {dir + "/content.xml", "text/xml"});
    }

    private static void chartContent(XmlOut o, ChartElement c) throws IOException {
        int cats = c.categoryCount();
        int series = c.seriesCount();
        boolean pie = ChartElement.PIE.equals(c.kind) || ChartElement.DOUGHNUT.equals(c.kind);
        o.raw(XML).raw("<office:document-content").raw(NS).raw("><office:automatic-styles>")
                .raw("<style:style style:name=\"P\" style:family=\"chart\"><style:chart-properties")
                .attr("chart:vertical", ChartElement.BAR.equals(c.kind) ? "true" : "false")
                .attr("chart:stacked", c.stacked ? "true" : null).raw("/></style:style>");
        int styles = pie ? Math.max(cats, series) : series;
        for (int i = 0; i < styles; i++) {
            String h = hex(color(c, i));
            o.raw("<style:style").attr("style:name", "S" + i)
                    .raw(" style:family=\"chart\"><style:graphic-properties draw:fill=\"solid\"")
                    .attr("draw:fill-color", h).attr("svg:stroke-color", h)
                    .raw("/></style:style>");
        }
        o.raw("</office:automatic-styles><office:body><office:chart><chart:chart")
                .attr("svg:width", pt(Math.max(1, c.width)))
                .attr("svg:height", pt(Math.max(1, c.height)))
                .attr("chart:class", chartClass(c.kind)).raw(">");
        if (c.title != null && c.title.length() > 0) {
            o.raw("<chart:title><text:p>").text(c.title, false).raw("</text:p></chart:title>");
        }
        if (c.legend) {
            o.raw("<chart:legend chart:legend-position=\"end\"/>");
        }
        String last = String.valueOf(cats + 1);
        o.raw("<chart:plot-area chart:style-name=\"P\" chart:data-source-has-labels=\"both\"")
                .attr("table:cell-range-address", "local-table.$A$1:.$" + column(series) + "$"
                        + last).raw(">");
        o.raw("<chart:axis chart:dimension=\"x\" chart:name=\"primary-x\"><chart:categories")
                .attr("table:cell-range-address", "local-table.$A$2:.$A$" + last)
                .raw("/></chart:axis>");
        if (!pie) {
            o.raw("<chart:axis chart:dimension=\"y\" chart:name=\"primary-y\">"
                    + "<chart:grid chart:class=\"major\"/></chart:axis>");
        }
        for (int i = 0; i < series; i++) {
            String col = column(i + 1);
            o.raw("<chart:series").attr("chart:style-name", "S" + i)
                    .attr("chart:values-cell-range-address",
                            "local-table.$" + col + "$2:.$" + col + "$" + last)
                    .attr("chart:label-cell-address", "local-table.$" + col + "$1").raw(">");
            if (pie) {
                for (int k = 0; k < cats; k++) {
                    o.raw("<chart:data-point").attr("chart:style-name", "S" + k).raw("/>");
                }
            }
            o.raw("</chart:series>");
        }
        o.raw("</chart:plot-area><table:table table:name=\"local-table\">"
                + "<table:table-header-columns><table:table-column/></table:table-header-columns>"
                + "<table:table-columns><table:table-column")
                .attr("table:number-columns-repeated", Math.max(1, series))
                .raw("/></table:table-columns><table:table-header-rows><table:table-row>"
                        + "<table:table-cell><text:p/></table:table-cell>");
        for (int i = 0; i < series; i++) {
            stringCell(o, c.seriesName(i));
        }
        o.raw("</table:table-row></table:table-header-rows><table:table-rows>");
        for (int k = 0; k < cats; k++) {
            o.raw("<table:table-row>");
            stringCell(o, c.category(k));
            for (int i = 0; i < series; i++) {
                double[] v = c.values(i);
                if (v != null && k < v.length) {
                    String s = num(v[k]);
                    o.raw("<table:table-cell office:value-type=\"float\"")
                            .attr("office:value", s).raw("><text:p>").raw(s)
                            .raw("</text:p></table:table-cell>");
                } else {
                    o.raw("<table:table-cell/>");
                }
            }
            o.raw("</table:table-row>");
        }
        o.raw("</table:table-rows></table:table></chart:chart></office:chart></office:body>"
                + "</office:document-content>");
    }

    private static void stringCell(XmlOut o, String s) throws IOException {
        o.raw("<table:table-cell office:value-type=\"string\"><text:p>")
                .text(s == null ? "" : s, false).raw("</text:p></table:table-cell>");
    }

    // ------------------------------------------------------------------ text

    private void paragraphs(XmlOut x, List<Block> blocks) throws IOException {
        int n = 0;
        if (blocks != null) {
            for (Block b : blocks) {
                if (b instanceof Paragraph) {
                    paragraph(x, (Paragraph) b);
                    n++;
                }
            }
        }
        if (n == 0) {
            x.raw("<text:p/>");
        }
    }

    private static String align(String a) {
        if (Paragraph.CENTER.equals(a)) {
            return "center";
        }
        if (Paragraph.RIGHT.equals(a)) {
            return "end";
        }
        return Paragraph.JUSTIFY.equals(a) ? "justify" : "start";
    }

    private void paragraph(XmlOut x, Paragraph p) throws IOException {
        boolean bullet = Paragraph.BULLET.equals(p.list);
        boolean numbered = Paragraph.NUMBERED.equals(p.list);
        int levels = bullet || numbered ? Math.min(9, Math.max(0, p.listLevel)) + 1 : 0;
        StringBuilder props = new StringBuilder("<style:paragraph-properties");
        props.append(attr("fo:text-align", align(p.align)))
                .append(attr("fo:margin-left", pt(p.indent)))
                .append(attr("fo:margin-right", pt(p.rightIndent)))
                .append(attr("fo:text-indent", pt(p.firstLine)))
                .append(attr("fo:margin-top", pt(p.before)))
                .append(attr("fo:margin-bottom", pt(p.after)));
        if (p.lineSpacing > 0) {
            props.append(attr("fo:line-height", Math.round(p.lineSpacing * 100) + "%"));
        }
        props.append("/>");
        String paraStyle = style("paragraph", "P", props.toString());
        for (int i = 0; i < levels; i++) {
            x.raw("<text:list");
            if (i == 0) {
                x.attr("text:style-name", bullet ? "L1" : "L2");
            }
            x.raw("><text:list-item");
            if (i == levels - 1 && numbered && p.listStart > 1) {
                x.attr("text:start-value", p.listStart);
            }
            x.raw(">");
        }
        x.raw("<text:p").attr("text:style-name", paraStyle).raw(">");
        RichText t = p.text;
        int runs = t == null ? 0 : t.runCount();
        for (int i = 0; i < runs; i++) {
            if (t.runObject(i) != null) {
                continue;
            }
            TextStyle s = t.runStyle(i);
            if (s == null) {
                s = TextStyle.DEFAULT;
            }
            span(x, t.runText(i), s);
        }
        x.raw("</text:p>");
        for (int i = 0; i < levels; i++) {
            x.raw("</text:list-item></text:list>");
        }
    }

    private void span(XmlOut x, String text, TextStyle s) throws IOException {
        if (text == null || text.length() == 0) {
            return;
        }
        StringBuilder props = new StringBuilder();
        OdfText.textProperties(props, s);
        String name = style("text", "T", props.toString());
        boolean link = s.link != null && s.link.length() > 0;
        if (link) {
            x.raw("<text:a xlink:type=\"simple\"").attr("xlink:href", s.link).raw(">");
        }
        x.raw("<text:span").attr("text:style-name", name).raw(">");
        int start = 0;
        int n = text.length();
        boolean space = false;
        for (int k = 0; k < n; k++) {
            char c = text.charAt(k);
            boolean extraSpace = c == ' ' && (space || k == 0);
            if (c == '\n' || c == '\t' || extraSpace) {
                if (k > start) {
                    x.text(text.substring(start, k), false);
                }
                if (c == '\n') {
                    x.raw("<text:line-break/>");
                } else if (c == '\t') {
                    x.raw("<text:tab/>");
                } else {
                    x.raw("<text:s/>");
                }
                start = k + 1;
            }
            space = c == ' ';
        }
        if (start < n) {
            x.text(text.substring(start), false);
        }
        x.raw("</text:span>");
        if (link) {
            x.raw("</text:a>");
        }
    }

    // ------------------------------------------------------------------ builds

    private void timing(XmlOut x, List<Build> builds) throws IOException {
        if (builds == null || builds.isEmpty()) {
            return;
        }
        List<Group> groups = new ArrayList<Group>();
        for (Build b : builds) {
            if (b.elementId == null || !xmlIds.containsKey(b.elementId)) {
                continue;
            }
            boolean click = Build.ON_CLICK.equals(b.trigger);
            Group g = groups.isEmpty() ? null : groups.get(groups.size() - 1);
            if (g == null || click) {
                g = new Group(click);
                groups.add(g);
                g.steps.add(new Step(0));
            } else if (Build.AFTER_PREVIOUS.equals(b.trigger)) {
                Step prev = g.steps.get(g.steps.size() - 1);
                g.steps.add(new Step(prev.end()));
            }
            g.steps.get(g.steps.size() - 1).builds.add(b);
        }
        if (groups.isEmpty()) {
            return;
        }
        x.raw("<anim:par presentation:node-type=\"timing-root\"><anim:seq"
                + " presentation:node-type=\"main-sequence\">");
        for (Group g : groups) {
            x.raw("<anim:par").attr("smil:begin", g.click ? "indefinite" : "0s").raw(">");
            for (Step s : g.steps) {
                x.raw("<anim:par").attr("smil:begin", num(s.begin) + "s").raw(">");
                for (Build b : s.builds) {
                    effect(x, b, xmlIds.get(b.elementId));
                }
                x.raw("</anim:par>");
            }
            x.raw("</anim:par>");
        }
        x.raw("</anim:seq></anim:par>");
    }

    private static String nodeType(String trigger) {
        if (Build.WITH_PREVIOUS.equals(trigger)) {
            return "with-previous";
        }
        return Build.AFTER_PREVIOUS.equals(trigger) ? "after-previous" : "on-click";
    }

    private static String presetId(Build b) {
        boolean exit = Build.EXIT.equals(b.kind);
        if (Build.EMPHASIS.equals(b.kind)) {
            return "ooo-emphasis-spin";
        }
        String prefix = exit ? "ooo-exit-" : "ooo-entrance-";
        if (Build.FADE.equals(b.effect)) {
            return prefix + (exit ? "fade-out" : "fade-in");
        }
        if (Build.FLY.equals(b.effect)) {
            return prefix + (exit ? "fly-out" : "fly-in");
        }
        if (Build.ZOOM.equals(b.effect)) {
            return prefix + "zoom";
        }
        if (Build.WIPE.equals(b.effect)) {
            return prefix + "wipe";
        }
        return prefix + (exit ? "disappear" : "appear");
    }

    private static void effect(XmlOut x, Build b, String target) throws IOException {
        boolean exit = Build.EXIT.equals(b.kind);
        boolean emphasis = Build.EMPHASIS.equals(b.kind);
        String dur = seconds(duration(b));
        x.raw("<anim:par").attr("smil:begin", seconds(Math.max(0, b.delay)))
                .attr("smil:fill", "hold").attr("presentation:node-type", nodeType(b.trigger))
                .attr("presentation:preset-class", emphasis ? "emphasis"
                        : exit ? "exit" : "entrance")
                .attr("presentation:preset-id", presetId(b)).raw(">");
        if (emphasis) {
            x.raw("<anim:animateTransform smil:fill=\"hold\" smil:by=\"360\" svg:type=\"rotate\"")
                    .attr("smil:dur", dur).attr("smil:targetElement", target)
                    .raw(" smil:attributeName=\"transform\"/></anim:par>");
            return;
        }
        if (!exit) {
            visibility(x, target, "0s", "visible");
        }
        if (Build.FADE.equals(b.effect) || Build.WIPE.equals(b.effect)) {
            boolean fade = Build.FADE.equals(b.effect);
            x.raw("<anim:transitionFilter").attr("smil:dur", dur)
                    .attr("smil:targetElement", target)
                    .attr("smil:type", fade ? "fade" : "barWipe")
                    .attr("smil:subtype", fade ? "crossfade" : "leftToRight")
                    .attr("smil:mode", exit ? "out" : null).raw("/>");
        } else if (Build.FLY.equals(b.effect)) {
            String d = b.direction;
            boolean horizontal = Transition.FROM_LEFT.equals(d) || Transition.FROM_RIGHT.equals(d);
            String axis = horizontal ? "x" : "y";
            String away = Transition.FROM_LEFT.equals(d) ? "0-width/2"
                    : Transition.FROM_RIGHT.equals(d) ? "1+width/2"
                    : Transition.FROM_TOP.equals(d) ? "0-height/2" : "1+height/2";
            animate(x, target, dur, axis, exit ? axis + ";" + away : away + ";" + axis);
        } else if (Build.ZOOM.equals(b.effect)) {
            animate(x, target, dur, "width", exit ? "width;0" : "0;width");
            animate(x, target, dur, "height", exit ? "height;0" : "0;height");
        }
        if (exit) {
            visibility(x, target, Build.APPEAR.equals(b.effect) ? "0s" : dur, "hidden");
        }
        x.raw("</anim:par>");
    }

    private static void visibility(XmlOut x, String target, String begin, String to)
            throws IOException {
        x.raw("<anim:set").attr("smil:begin", begin)
                .raw(" smil:dur=\"0.001s\" smil:fill=\"hold\"")
                .attr("smil:targetElement", target)
                .raw(" smil:attributeName=\"visibility\"").attr("smil:to", to).raw("/>");
    }

    private static void animate(XmlOut x, String target, String dur, String attribute,
            String values) throws IOException {
        x.raw("<anim:animate smil:fill=\"hold\"").attr("smil:dur", dur)
                .attr("smil:targetElement", target).attr("smil:attributeName", attribute)
                .attr("smil:values", values).raw(" smil:keyTimes=\"0;1\"")
                .raw(" presentation:additive=\"base\"/>");
    }
}
