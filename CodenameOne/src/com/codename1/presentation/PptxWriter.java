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

import com.codename1.impl.xml.XmlOut;
import com.codename1.io.zip.ZipArchiveOutputStream;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.Table;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

/// PowerPoint 2007+ (`.pptx`, OOXML PresentationML). One slide master with one blank layout
/// carries the theme; every slide states its geometry and run formatting itself, so the deck
/// reads the same in any application whether or not it resolves inheritance. Charts are real
/// chart parts with literal data caches, diagrams are written as the group of shapes they
/// render to, and builds become a minimal main animation sequence.
final class PptxWriter {
    static final String NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    static final String REL_HYPERLINK = NS_R + "/hyperlink";
    private static final String NS_A = "http://schemas.openxmlformats.org/drawingml/2006/main";
    private static final String NS_P = "http://schemas.openxmlformats.org/presentationml/2006/main";
    private static final String NAMESPACES = " xmlns:a=\"" + NS_A + "\" xmlns:r=\"" + NS_R
            + "\" xmlns:p=\"" + NS_P + "\"";
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
    private static final String CT = "application/vnd.openxmlformats-officedocument.";
    private static final String CT_PML = CT + "presentationml.";
    private static final String NOTES_W = "6858000";
    private static final String NOTES_H = "9144000";

    /// A relationships part being collected.
    static final class Rels {
        private final List<String[]> list = new ArrayList<String[]>();

        /// Adds a relationship, or finds an equal one, and answers its id.
        String add(String type, String target, boolean external) {
            for (String[] r : list) {
                if (r[1].equals(type) && r[2].equals(target)) {
                    return r[0];
                }
            }
            String id = "rId" + (list.size() + 1);
            list.add(new String[] {id, type, target, external ? "1" : null});
            return id;
        }

        byte[] toBytes() throws IOException {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            XmlOut x = new XmlOut(b);
            x.raw(XML).raw("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/"
                    + "2006/relationships\">");
            for (String[] r : list) {
                x.raw("<Relationship").attr("Id", r[0]).attr("Type", r[1]).attr("Target", r[2])
                        .attr("TargetMode", r[3] != null ? "External" : null).raw("/>");
            }
            x.raw("</Relationships>");
            x.flush();
            return b.toByteArray();
        }
    }

    private final Presentation pres;
    private final List<String[]> overrides = new ArrayList<String[]>();
    private final List<String> extensions = new ArrayList<String>();
    private final Map<Picture, String> media = new HashMap<Picture, String>();
    private final ZipArchiveOutputStream zip;
    private int charts;

    // per-slide state
    private Rels rels;
    private int nextShape;
    private final Map<String, Integer> shapeIds = new HashMap<String, Integer>();
    private final Map<String, Boolean> textShapes = new HashMap<String, Boolean>();

    private PptxWriter(Presentation p, OutputStream out) {
        pres = p;
        zip = new ZipArchiveOutputStream(out);
    }

    static void write(Presentation p, OutputStream out) throws IOException {
        new PptxWriter(p, out).run();
    }

    static String emu(float points) {
        return String.valueOf(Math.round((double) points * 12700));
    }

    static String hex(int c) {
        String h = Integer.toHexString(c & 0xffffff);
        while (h.length() < 6) {
            h = "0" + h;
        }
        return h;
    }

    private static String pct(float fraction) {
        return String.valueOf(Math.round(fraction * 100000));
    }

    private static String rel(String name) {
        return NS_R + "/" + name;
    }

    private void put(String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    private void part(String name, String contentType, ByteArrayOutputStream b, XmlOut x)
            throws IOException {
        x.flush();
        put(name, b.toByteArray());
        overrides.add(new String[] {"/" + name, contentType});
    }

    private static String relsName(String part) {
        int slash = part.lastIndexOf('/');
        return part.substring(0, slash + 1) + "_rels/" + part.substring(slash + 1) + ".rels";
    }

    private void run() throws IOException {
        Rels presRels = new Rels();
        presRels.add(rel("slideMaster"), "slideMasters/slideMaster1.xml", false);
        boolean anyNotes = false;
        for (Slide s : pres.slides) {
            anyNotes |= s.notes != null && !s.notes.isEmpty();
        }
        List<String> slideRels = new ArrayList<String>();
        for (int i = 0; i < pres.slides.size(); i++) {
            Slide s = pres.slides.get(i);
            slideRels.add(presRels.add(rel("slide"), "slides/slide" + (i + 1) + ".xml", false));
            slide(s, i + 1);
        }
        String notesMasterRel = null;
        if (anyNotes) {
            notesMasterRel = presRels.add(rel("notesMaster"), "notesMasters/notesMaster1.xml",
                    false);
            notesMaster();
        }
        presRels.add(rel("presProps"), "presProps.xml", false);
        presRels.add(rel("viewProps"), "viewProps.xml", false);
        presRels.add(rel("theme"), "theme/theme1.xml", false);
        presRels.add(rel("tableStyles"), "tableStyles.xml", false);
        presentation(slideRels, notesMasterRel);
        put("ppt/_rels/presentation.xml.rels", presRels.toBytes());
        master();
        theme("ppt/theme/theme1.xml");
        simpleParts();
        contentTypes();
        zip.finish();
    }

    private void presentation(List<String> slideRels, String notesMasterRel) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        XmlOut x = new XmlOut(b);
        x.raw(XML).raw("<p:presentation").raw(NAMESPACES).raw(" saveSubsetFonts=\"1\">")
                .raw("<p:sldMasterIdLst><p:sldMasterId id=\"2147483648\" r:id=\"rId1\"/>"
                        + "</p:sldMasterIdLst>");
        if (notesMasterRel != null) {
            x.raw("<p:notesMasterIdLst><p:notesMasterId").attr("r:id", notesMasterRel)
                    .raw("/></p:notesMasterIdLst>");
        }
        if (!slideRels.isEmpty()) {
            x.raw("<p:sldIdLst>");
            for (int i = 0; i < slideRels.size(); i++) {
                x.raw("<p:sldId").attr("id", 256 + i).attr("r:id", slideRels.get(i)).raw("/>");
            }
            x.raw("</p:sldIdLst>");
        }
        long cx = Math.max(914400, Math.min(51206400, Math.round((double) pres.width * 12700)));
        long cy = Math.max(914400, Math.min(51206400, Math.round((double) pres.height * 12700)));
        x.raw("<p:sldSz").attr("cx", String.valueOf(cx)).attr("cy", String.valueOf(cy))
                .raw("/><p:notesSz").attr("cx", NOTES_W).attr("cy", NOTES_H)
                .raw("/></p:presentation>");
        part("ppt/presentation.xml", CT_PML + "presentation.main+xml", b, x);
    }

    private static void emptyTree(XmlOut x) throws IOException {
        x.raw("<p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>"
                + "<p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/>"
                + "<a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>");
    }

    private static String colorMap(String tag) {
        return "<" + tag + " bg1=\"lt1\" tx1=\"dk1\" bg2=\"lt2\" tx2=\"dk2\" accent1=\"accent1\""
                + " accent2=\"accent2\" accent3=\"accent3\" accent4=\"accent4\""
                + " accent5=\"accent5\" accent6=\"accent6\" hlink=\"hlink\" folHlink=\"folHlink\"/>";
    }

    private void master() throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        XmlOut x = new XmlOut(b);
        x.raw(XML).raw("<p:sldMaster").raw(NAMESPACES).raw("><p:cSld><p:bg><p:bgPr><a:solidFill>"
                + "<a:schemeClr val=\"bg1\"/></a:solidFill><a:effectLst/></p:bgPr></p:bg><p:spTree>");
        emptyTree(x);
        x.raw("</p:spTree></p:cSld>").raw(colorMap("p:clrMap"))
                .raw("<p:sldLayoutIdLst><p:sldLayoutId id=\"2147483649\" r:id=\"rId1\"/>"
                        + "</p:sldLayoutIdLst><p:txStyles><p:titleStyle/><p:bodyStyle/>"
                        + "<p:otherStyle/></p:txStyles></p:sldMaster>");
        part("ppt/slideMasters/slideMaster1.xml", CT_PML + "slideMaster+xml", b, x);
        Rels r = new Rels();
        r.add(rel("slideLayout"), "../slideLayouts/slideLayout1.xml", false);
        r.add(rel("theme"), "../theme/theme1.xml", false);
        put("ppt/slideMasters/_rels/slideMaster1.xml.rels", r.toBytes());

        b = new ByteArrayOutputStream();
        x = new XmlOut(b);
        x.raw(XML).raw("<p:sldLayout").raw(NAMESPACES)
                .raw(" type=\"blank\" preserve=\"1\"><p:cSld name=\"Blank\"><p:spTree>");
        emptyTree(x);
        x.raw("</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>"
                + "</p:sldLayout>");
        part("ppt/slideLayouts/slideLayout1.xml", CT_PML + "slideLayout+xml", b, x);
        r = new Rels();
        r.add(rel("slideMaster"), "../slideMasters/slideMaster1.xml", false);
        put("ppt/slideLayouts/_rels/slideLayout1.xml.rels", r.toBytes());
    }

    private void notesMaster() throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        XmlOut x = new XmlOut(b);
        x.raw(XML).raw("<p:notesMaster").raw(NAMESPACES).raw("><p:cSld><p:spTree>");
        emptyTree(x);
        x.raw("</p:spTree></p:cSld>").raw(colorMap("p:clrMap")).raw("</p:notesMaster>");
        part("ppt/notesMasters/notesMaster1.xml", CT_PML + "notesMaster+xml", b, x);
        Rels r = new Rels();
        r.add(rel("theme"), "../theme/theme2.xml", false);
        put("ppt/notesMasters/_rels/notesMaster1.xml.rels", r.toBytes());
        theme("ppt/theme/theme2.xml");
    }

    private void theme(String name) throws IOException {
        Theme t = pres.theme;
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        XmlOut x = new XmlOut(b);
        x.raw(XML).raw("<a:theme xmlns:a=\"" + NS_A + "\"").attr("name", t.name)
                .raw("><a:themeElements><a:clrScheme").attr("name", t.name).raw(">");
        for (int i = 0; i < Theme.NAMES.length; i++) {
            x.raw("<a:").raw(Theme.NAMES[i]).raw("><a:srgbClr").attr("val", hex(t.color(i)))
                    .raw("/></a:").raw(Theme.NAMES[i]).raw(">");
        }
        x.raw("</a:clrScheme><a:fontScheme").attr("name", t.name).raw(">");
        fonts(x, "a:majorFont", t.majorFont);
        fonts(x, "a:minorFont", t.minorFont);
        x.raw("</a:fontScheme><a:fmtScheme name=\"Codename One\"><a:fillStyleLst>");
        String solid = "<a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill>";
        x.raw(solid).raw(solid).raw(solid).raw("</a:fillStyleLst><a:lnStyleLst>");
        for (int i = 1; i <= 3; i++) {
            x.raw("<a:ln").attr("w", 9525 * i).raw(">").raw(solid).raw("</a:ln>");
        }
        x.raw("</a:lnStyleLst><a:effectStyleLst>");
        for (int i = 0; i < 3; i++) {
            x.raw("<a:effectStyle><a:effectLst/></a:effectStyle>");
        }
        x.raw("</a:effectStyleLst><a:bgFillStyleLst>").raw(solid).raw(solid).raw(solid)
                .raw("</a:bgFillStyleLst></a:fmtScheme></a:themeElements></a:theme>");
        part(name, CT + "theme+xml", b, x);
    }

    private static void fonts(XmlOut x, String tag, String face) throws IOException {
        x.raw("<").raw(tag).raw("><a:latin").attr("typeface", face == null ? "Arial" : face)
                .raw("/><a:ea typeface=\"\"/><a:cs typeface=\"\"/></").raw(tag).raw(">");
    }

    private void simpleParts() throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        XmlOut x = new XmlOut(b);
        x.raw(XML).raw("<p:presentationPr").raw(NAMESPACES).raw("/>");
        part("ppt/presProps.xml", CT_PML + "presProps+xml", b, x);
        b = new ByteArrayOutputStream();
        x = new XmlOut(b);
        x.raw(XML).raw("<p:viewPr").raw(NAMESPACES).raw("/>");
        part("ppt/viewProps.xml", CT_PML + "viewProps+xml", b, x);
        b = new ByteArrayOutputStream();
        x = new XmlOut(b);
        x.raw(XML).raw("<a:tblStyleLst xmlns:a=\"" + NS_A + "\""
                + " def=\"{5C22544A-7EE6-4342-B048-85BDC9FD1C3A}\"/>");
        part("ppt/tableStyles.xml", CT_PML + "tableStyles+xml", b, x);
        b = new ByteArrayOutputStream();
        x = new XmlOut(b);
        x.raw(XML).raw("<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/"
                + "2006/metadata/core-properties\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        if (pres.title != null && pres.title.length() > 0) {
            x.raw("<dc:title>").text(pres.title, false).raw("</dc:title>");
        }
        x.raw("</cp:coreProperties>");
        part("docProps/core.xml", "application/vnd.openxmlformats-package.core-properties+xml",
                b, x);
        b = new ByteArrayOutputStream();
        x = new XmlOut(b);
        x.raw(XML).raw("<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/"
                + "2006/extended-properties\"><Application>Codename One</Application><Slides>")
                .raw(pres.slides.size()).raw("</Slides></Properties>");
        part("docProps/app.xml", CT + "extended-properties+xml", b, x);
        Rels r = new Rels();
        r.add(rel("officeDocument"), "ppt/presentation.xml", false);
        r.add("http://schemas.openxmlformats.org/package/2006/relationships/metadata/"
                + "core-properties", "docProps/core.xml", false);
        r.add(rel("extended-properties"), "docProps/app.xml", false);
        put("_rels/.rels", r.toBytes());
    }

    private void contentTypes() throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        XmlOut x = new XmlOut(b);
        x.raw(XML).raw("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/"
                + "content-types\"><Default Extension=\"rels\" ContentType=\"application/"
                + "vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\""
                + " ContentType=\"application/xml\"/>");
        for (String ext : extensions) {
            x.raw("<Default").attr("Extension", ext).attr("ContentType", mimeOf(ext)).raw("/>");
        }
        for (String[] o : overrides) {
            x.raw("<Override").attr("PartName", o[0]).attr("ContentType", o[1]).raw("/>");
        }
        x.raw("</Types>");
        x.flush();
        put("[Content_Types].xml", b.toByteArray());
    }

    private static String mimeOf(String ext) {
        if ("jpeg".equals(ext)) {
            return "image/jpeg";
        }
        if ("svg".equals(ext)) {
            return "image/svg+xml";
        }
        return "bin".equals(ext) ? "application/octet-stream" : "image/" + ext;
    }

    private static String extensionOf(String mime) {
        if (mime == null) {
            return "bin";
        }
        String m = mime.trim();
        if ("image/jpeg".equalsIgnoreCase(m) || "image/jpg".equalsIgnoreCase(m)) {
            return "jpeg";
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
            return "tiff";
        }
        return "image/svg+xml".equalsIgnoreCase(m) ? "svg" : "bin";
    }

    /// The relationship id of a picture from the current part, writing its media once.
    private String picture(Picture p) throws IOException {
        if (p.isLinked()) {
            return rels.add(rel("image"), p.source, true);
        }
        String target = media.get(p);
        if (target == null) {
            String ext = extensionOf(p.mimeType);
            if (!extensions.contains(ext)) {
                extensions.add(ext);
            }
            target = "media/image" + (media.size() + 1) + "." + ext;
            media.put(p, target);
            put("ppt/" + target, p.data());
        }
        return rels.add(rel("image"), "../" + target, false);
    }

    // ------------------------------------------------------------------ slides

    private void slide(Slide s, int n) throws IOException {
        rels = new Rels();
        nextShape = 2;
        shapeIds.clear();
        textShapes.clear();
        rels.add(rel("slideLayout"), "../slideLayouts/slideLayout1.xml", false);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        XmlOut x = new XmlOut(b);
        x.raw(XML).raw("<p:sld").raw(NAMESPACES).attr("show", s.hidden ? "0" : null)
                .raw("><p:cSld>");
        if (s.background != null && s.background.kind != Fill.NONE) {
            x.raw("<p:bg><p:bgPr>");
            fill(x, s.background);
            x.raw("<a:effectLst/></p:bgPr></p:bg>");
        }
        x.raw("<p:spTree>");
        emptyTree(x);
        for (SlideElement e : s.elements) {
            element(x, e);
        }
        x.raw("</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>");
        transition(x, s.transition);
        PptxTiming.write(x, s.builds, shapeIds, textShapes);
        x.raw("</p:sld>");
        String name = "ppt/slides/slide" + n + ".xml";
        if (s.notes != null && !s.notes.isEmpty()) {
            rels.add(rel("notesSlide"), "../notesSlides/notesSlide" + n + ".xml", false);
        }
        part(name, CT_PML + "slide+xml", b, x);
        put(relsName(name), rels.toBytes());
        if (s.notes != null && !s.notes.isEmpty()) {
            notes(s.notes, n);
        }
    }

    private void notes(List<Block> notes, int n) throws IOException {
        rels = new Rels();
        rels.add(rel("notesMaster"), "../notesMasters/notesMaster1.xml", false);
        rels.add(rel("slide"), "../slides/slide" + n + ".xml", false);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        XmlOut x = new XmlOut(b);
        x.raw(XML).raw("<p:notes").raw(NAMESPACES).raw("><p:cSld><p:spTree>");
        emptyTree(x);
        x.raw("<p:sp><p:nvSpPr><p:cNvPr id=\"2\" name=\"Slide Image\"/><p:cNvSpPr><a:spLocks"
                + " noGrp=\"1\" noRot=\"1\" noChangeAspect=\"1\"/></p:cNvSpPr><p:nvPr>"
                + "<p:ph type=\"sldImg\"/></p:nvPr></p:nvSpPr><p:spPr><a:xfrm>"
                + "<a:off x=\"1143000\" y=\"685800\"/><a:ext cx=\"4572000\" cy=\"3429000\"/>"
                + "</a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></p:spPr></p:sp>");
        x.raw("<p:sp><p:nvSpPr><p:cNvPr id=\"3\" name=\"Notes\"/><p:cNvSpPr><a:spLocks"
                + " noGrp=\"1\"/></p:cNvSpPr><p:nvPr><p:ph type=\"body\" idx=\"1\"/></p:nvPr>"
                + "</p:nvSpPr><p:spPr><a:xfrm><a:off x=\"685800\" y=\"4343400\"/>"
                + "<a:ext cx=\"5486400\" cy=\"4114800\"/></a:xfrm><a:prstGeom prst=\"rect\">"
                + "<a:avLst/></a:prstGeom></p:spPr><p:txBody><a:bodyPr/><a:lstStyle/>");
        PptxText.paragraphs(x, notes, rels);
        x.raw("</p:txBody></p:sp></p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/>"
                + "</p:clrMapOvr></p:notes>");
        String name = "ppt/notesSlides/notesSlide" + n + ".xml";
        part(name, CT_PML + "notesSlide+xml", b, x);
        put(relsName(name), rels.toBytes());
    }

    private static void transition(XmlOut x, Transition t) throws IOException {
        if (t == null || Transition.DEFAULT.equals(t)) {
            return;
        }
        String spd = t.duration <= 500 ? "fast" : t.duration <= 750 ? "med" : "slow";
        x.raw("<p:transition").attr("spd", spd).attr("advClick", t.advanceOnClick ? null : "0")
                .attr("advTm", t.advanceAfter >= 0 ? String.valueOf(t.advanceAfter) : null)
                .raw(">");
        String dir = direction(t.direction);
        if (Transition.FADE.equals(t.type)) {
            x.raw("<p:fade/>");
        } else if (Transition.PUSH.equals(t.type)) {
            x.raw("<p:push").attr("dir", dir).raw("/>");
        } else if (Transition.COVER.equals(t.type)) {
            x.raw("<p:cover").attr("dir", dir).raw("/>");
        } else if (Transition.UNCOVER.equals(t.type)) {
            x.raw("<p:pull").attr("dir", dir).raw("/>");
        } else if (Transition.WIPE.equals(t.type)) {
            x.raw("<p:wipe").attr("dir", dir).raw("/>");
        } else if (Transition.ZOOM.equals(t.type)) {
            x.raw("<p:zoom/>");
        }
        x.raw("</p:transition>");
    }

    /// The way the incoming slide moves, from the side it enters from.
    private static String direction(String from) {
        if (Transition.FROM_LEFT.equals(from)) {
            return "r";
        }
        if (Transition.FROM_TOP.equals(from)) {
            return "d";
        }
        return Transition.FROM_BOTTOM.equals(from) ? "u" : "l";
    }

    private int register(SlideElement e, boolean text) {
        int id = nextShape++;
        if (e.id != null) {
            shapeIds.put(e.id, Integer.valueOf(id));
            if (text) {
                textShapes.put(e.id, Boolean.TRUE);
            }
        }
        return id;
    }

    private static void nv(XmlOut x, int id, SlideElement e, String kind) throws IOException {
        String name = e.name != null && e.name.length() > 0 ? e.name
                : kind + " " + (id - 1);
        x.raw("<p:cNvPr").attr("id", id).attr("name", name).raw("/>");
    }

    private static void xfrm(XmlOut x, String tag, SlideElement e, boolean group)
            throws IOException {
        x.raw("<").raw(tag);
        if (e.rotation != 0) {
            x.attr("rot", String.valueOf(Math.round((double) e.rotation * 60000)));
        }
        x.attr("flipH", e.flipH ? "1" : null).attr("flipV", e.flipV ? "1" : null)
                .raw("><a:off").attr("x", emu(e.x)).attr("y", emu(e.y)).raw("/><a:ext")
                .attr("cx", emu(Math.max(0, e.width))).attr("cy", emu(Math.max(0, e.height)))
                .raw("/>");
        if (group) {
            x.raw("<a:chOff").attr("x", emu(e.x)).attr("y", emu(e.y)).raw("/><a:chExt")
                    .attr("cx", emu(Math.max(0, e.width))).attr("cy", emu(Math.max(0, e.height)))
                    .raw("/>");
        }
        x.raw("</").raw(tag).raw(">");
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
        int id = register(original, false);
        x.raw("<p:grpSp><p:nvGrpSpPr>");
        nv(x, id, original, "Group");
        x.raw("<p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr>");
        xfrm(x, "a:xfrm", g, true);
        x.raw("</p:grpSpPr>");
        for (SlideElement c : g.children) {
            element(x, c);
        }
        x.raw("</p:grpSp>");
    }

    private void shape(XmlOut x, ShapeElement s) throws IOException {
        int id = register(s, s.text != null);
        x.raw("<p:sp><p:nvSpPr>");
        nv(x, id, s, s.placeholder != null ? "Placeholder" : "Shape");
        x.raw("<p:cNvSpPr/><p:nvPr>");
        if (s.placeholder != null && s.placeholder.length() > 0) {
            x.raw("<p:ph").attr("type", s.placeholder)
                    .attr("idx", ShapeElement.TITLE.equals(s.placeholder) ? null : "1").raw("/>");
        }
        x.raw("</p:nvPr></p:nvSpPr><p:spPr>");
        xfrm(x, "a:xfrm", s, false);
        String geometry = s.geometry == null || s.geometry.length() == 0 ? "rect" : s.geometry;
        x.raw("<a:prstGeom").attr("prst", geometry).raw("><a:avLst>");
        int n = s.adjustCount();
        for (int i = 0; i < n; i++) {
            x.raw("<a:gd").attr("name", n == 1 ? "adj" : "adj" + (i + 1))
                    .attr("fmla", "val " + pct(s.adjust(i, 0))).raw("/>");
        }
        x.raw("</a:avLst></a:prstGeom>");
        if (s.isLine()) {
            x.raw("<a:noFill/>");
        } else {
            fill(x, s.fill);
        }
        line(x, s.line);
        x.raw("</p:spPr>");
        if (s.text != null) {
            PptxText.body(x, "p:txBody", s.text, rels);
        }
        x.raw("</p:sp>");
    }

    private void pictureElement(XmlOut x, PictureElement p) throws IOException {
        int id = register(p, false);
        x.raw("<p:pic><p:nvPicPr>");
        nv(x, id, p, "Picture");
        x.raw("<p:cNvPicPr><a:picLocks noChangeAspect=\"1\"/></p:cNvPicPr><p:nvPr/></p:nvPicPr>"
                + "<p:blipFill>");
        blip(x, p.picture);
        if (p.isCropped()) {
            x.raw("<a:srcRect").attr("l", pct(p.cropLeft)).attr("t", pct(p.cropTop))
                    .attr("r", pct(p.cropRight)).attr("b", pct(p.cropBottom)).raw("/>");
        }
        x.raw("<a:stretch><a:fillRect/></a:stretch></p:blipFill><p:spPr>");
        xfrm(x, "a:xfrm", p, false);
        x.raw("<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom>");
        line(x, p.line);
        x.raw("</p:spPr></p:pic>");
    }

    private void blip(XmlOut x, Picture p) throws IOException {
        if (p == null) {
            x.raw("<a:blip/>");
            return;
        }
        String r = picture(p);
        x.raw("<a:blip").attr(p.isLinked() ? "r:link" : "r:embed", r).raw("/>");
    }

    private static void color(XmlOut x, int rgb, float alpha) throws IOException {
        x.raw("<a:srgbClr").attr("val", hex(rgb));
        if (alpha < 1) {
            x.raw("><a:alpha").attr("val", pct(Math.max(0, alpha))).raw("/></a:srgbClr>");
        } else {
            x.raw("/>");
        }
    }

    private void fill(XmlOut x, Fill f) throws IOException {
        if (f == null || f.kind == Fill.NONE) {
            x.raw("<a:noFill/>");
        } else if (f.kind == Fill.SOLID) {
            x.raw("<a:solidFill>");
            color(x, f.color, f.alpha);
            x.raw("</a:solidFill>");
        } else if (f.kind == Fill.GRADIENT) {
            x.raw("<a:gradFill rotWithShape=\"1\"><a:gsLst><a:gs pos=\"0\">");
            color(x, f.color, f.alpha);
            x.raw("</a:gs><a:gs pos=\"100000\">");
            color(x, f.color2, f.alpha);
            float a = f.angle % 360;
            if (a < 0) {
                a += 360;
            }
            x.raw("</a:gs></a:gsLst><a:lin")
                    .attr("ang", String.valueOf(Math.round((double) a * 60000)))
                    .attr("scaled", "0").raw("/></a:gradFill>");
        } else {
            x.raw("<a:blipFill rotWithShape=\"1\">");
            blip(x, f.picture);
            x.raw("<a:stretch><a:fillRect/></a:stretch></a:blipFill>");
        }
    }

    private static String arrow(String a) {
        if (LineStyle.ARROW_TRIANGLE.equals(a) || LineStyle.ARROW_OPEN.equals(a)
                || LineStyle.ARROW_OVAL.equals(a)) {
            return a;
        }
        return null;
    }

    private static void line(XmlOut x, LineStyle l) throws IOException {
        if (l == null || !l.isVisible()) {
            x.raw("<a:ln><a:noFill/></a:ln>");
            return;
        }
        x.raw("<a:ln").attr("w", emu(l.width)).raw("><a:solidFill>");
        color(x, l.color, l.alpha);
        x.raw("</a:solidFill>");
        if (LineStyle.DASH.equals(l.dash)) {
            x.raw("<a:prstDash val=\"dash\"/>");
        } else if (LineStyle.DOT.equals(l.dash)) {
            x.raw("<a:prstDash val=\"sysDot\"/>");
        } else {
            x.raw("<a:prstDash val=\"solid\"/>");
        }
        String head = arrow(l.head);
        String tail = arrow(l.tail);
        if (head != null) {
            x.raw("<a:headEnd").attr("type", head).raw("/>");
        }
        if (tail != null) {
            x.raw("<a:tailEnd").attr("type", tail).raw("/>");
        }
        x.raw("</a:ln>");
    }

    private void frameStart(XmlOut x, SlideElement e, String kind, String uri)
            throws IOException {
        int id = register(e, false);
        x.raw("<p:graphicFrame><p:nvGraphicFramePr>");
        nv(x, id, e, kind);
        x.raw("<p:cNvGraphicFramePr><a:graphicFrameLocks noGrp=\"1\"/></p:cNvGraphicFramePr>"
                + "<p:nvPr/></p:nvGraphicFramePr>");
        xfrm(x, "p:xfrm", e, false);
        x.raw("<a:graphic><a:graphicData").attr("uri", uri).raw(">");
    }

    private void chart(XmlOut x, ChartElement c) throws IOException {
        charts++;
        String name = "charts/chart" + charts + ".xml";
        String r = rels.add(rel("chart"), "../" + name, false);
        frameStart(x, c, "Chart", "http://schemas.openxmlformats.org/drawingml/2006/chart");
        x.raw("<c:chart xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\"")
                .attr("r:id", r).raw("/></a:graphicData></a:graphic></p:graphicFrame>");
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        XmlOut cx = new XmlOut(b);
        PptxChart.write(cx, c);
        part("ppt/" + name, CT + "drawingml.chart+xml", b, cx);
    }

    private void table(XmlOut x, TableElement te) throws IOException {
        Table t = te.table;
        int rows = Math.max(1, t.rows());
        int cols = Math.max(1, t.columns());
        frameStart(x, te, "Table", "http://schemas.openxmlformats.org/drawingml/2006/table");
        x.raw("<a:tbl><a:tblPr").attr("firstRow", t.headerRows > 0 ? "1" : null)
                .raw("/><a:tblGrid>");
        List<Float> widths = t.columnWidths();
        float total = 0;
        if (widths.size() == cols) {
            for (Float w : widths) {
                total += w.floatValue();
            }
        }
        for (int c = 0; c < cols; c++) {
            float w = total > 0 ? te.width * widths.get(c).floatValue() / total : te.width / cols;
            x.raw("<a:gridCol").attr("w", emu(w)).raw("/>");
        }
        x.raw("</a:tblGrid>");
        String rowH = emu(te.height / rows);
        for (int r = 0; r < rows; r++) {
            x.raw("<a:tr").attr("h", rowH).raw(">");
            for (int c = 0; c < cols; c++) {
                cell(x, t, r, c);
            }
            x.raw("</a:tr>");
        }
        x.raw("</a:tbl></a:graphicData></a:graphic></p:graphicFrame>");
    }

    private void cell(XmlOut x, Table t, int r, int c) throws IOException {
        x.raw("<a:tc");
        boolean covered = r < t.rows() && c < t.columns() && t.covered(r, c);
        if (covered) {
            for (Table.Merge m : t.merges) {
                if (m.contains(r, c)) {
                    x.attr("hMerge", c > m.column ? "1" : null)
                            .attr("vMerge", r > m.row ? "1" : null);
                    break;
                }
            }
        } else if (r < t.rows() && c < t.columns()) {
            Table.Merge m = t.span(r, c);
            if (m != null) {
                x.attr("gridSpan", m.columns > 1 ? String.valueOf(m.columns) : null)
                        .attr("rowSpan", m.rows > 1 ? String.valueOf(m.rows) : null);
            }
        }
        x.raw("><a:txBody><a:bodyPr/><a:lstStyle/>");
        List<Block> blocks = r < t.rows() && c < t.columns() ? t.cell(r, c) : null;
        PptxText.paragraphs(x, covered ? null : blocks, rels);
        x.raw("</a:txBody><a:tcPr>");
        String border = "<a:solidFill><a:srgbClr val=\"808080\"/></a:solidFill>";
        String[] sides = {"a:lnL", "a:lnR", "a:lnT", "a:lnB"};
        for (String side : sides) {
            x.raw("<").raw(side).raw(" w=\"12700\">").raw(border).raw("</").raw(side).raw(">");
        }
        if (r < t.headerRows) {
            x.raw("<a:solidFill><a:srgbClr val=\"E7ECF2\"/></a:solidFill>");
        }
        x.raw("</a:tcPr></a:tc>");
    }
}
