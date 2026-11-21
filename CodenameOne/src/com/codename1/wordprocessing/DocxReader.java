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

import com.codename1.impl.xml.XmlScanner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Reads Office Open XML word processing documents (`.docx`, `.docm`, `.dotx`).
///
/// What the model holds is read: paragraphs with their style, alignment, spacing, indent and
/// list membership; runs with fonts, sizes, colors, highlight, emphasis, links and tracked
/// changes; fields (the cached result is kept); footnotes and endnotes; review comments;
/// tables with merged cells; inline and anchored pictures; the page size and margins of the
/// last section. Everything else -- headers, footers, text boxes, charts, equations -- is
/// skipped.
final class DocxReader {
    /// Word's grid has at most 63 columns; anything past this is damage.
    private static final int MAX_COLUMNS = 64;

    private static final int MAX_ROWS = 32767;

    private static final String REL_OFFICE = "/officeDocument";

    private final Map<String, byte[]> parts;
    private final long[] budget = {XmlScanner.MAX_DOCUMENT};
    private final Ids ids = new Ids();
    private String main = "word/document.xml";
    private final Map<String, String[]> rels = new HashMap<String, String[]>();
    private final Map<String, Style> styles = new HashMap<String, Style>();
    private final Props defaults = new Props();
    private String defaultParagraphStyle;
    private final Map<String, String[]> abstractFormats = new HashMap<String, String[]>();
    private final Map<String, int[]> abstractStarts = new HashMap<String, int[]>();
    private final Map<String, String> numToAbstract = new HashMap<String, String>();
    private final Map<String, int[]> numOverrides = new HashMap<String, int[]>();
    private final Set<String> seenAbstracts = new HashSet<String>();
    private final Set<String> seenNums = new HashSet<String>();
    private final Map<String, List<Block>> footnotes = new HashMap<String, List<Block>>();
    private final Map<String, List<Block>> endnotes = new HashMap<String, List<Block>>();
    private final Map<String, String[]> commentData = new HashMap<String, String[]>();
    private final Map<String, String[]> commentStarts = new HashMap<String, String[]>();
    private final Map<String, String[]> commentEnds = new HashMap<String, String[]>();
    private final List<String> commentOrder = new ArrayList<String>();
    private String majorFont = "Calibri Light";
    private String minorFont = "Calibri";
    private PageSpec page;
    private int noteCounter;

    private DocxReader(Map<String, byte[]> parts) {
        this.parts = parts;
    }

    static Document read(InputStream in) throws IOException {
        return new DocxReader(ZipParts.read(in, "Word document")).read();
    }

    /// Reads a package already in memory, as the parts the `.doc` translation produces.
    static Document read(Map<String, byte[]> parts) throws IOException {
        return new DocxReader(parts).read();
    }

    /// Per-property formatting, every field optional, so style layers can be overlaid.
    static final class Props {
        String font;
        String fontTheme;
        Float size;
        Integer color;
        Integer background;
        Boolean bold;
        Boolean italic;
        Boolean underline;
        Boolean strike;
        Boolean hidden;
        String script;

        void overlay(Props p) {
            if (p == null) {
                return;
            }
            if (p.font != null || p.fontTheme != null) {
                font = p.font;
                fontTheme = p.fontTheme;
            }
            size = p.size != null ? p.size : size;
            color = p.color != null ? p.color : color;
            background = p.background != null ? p.background : background;
            bold = p.bold != null ? p.bold : bold;
            italic = p.italic != null ? p.italic : italic;
            underline = p.underline != null ? p.underline : underline;
            strike = p.strike != null ? p.strike : strike;
            hidden = p.hidden != null ? p.hidden : hidden;
            script = p.script != null ? p.script : script;
        }

        Props copy() {
            Props p = new Props();
            p.overlay(this);
            return p;
        }
    }

    /// Paragraph formatting a style or a paragraph sets.
    static final class Para {
        String align;
        Float before;
        Float after;
        Float indent;
        Float spacing;
        Boolean pageBreak;
        Boolean keepNext;
        String numId;
        Integer level;
        Integer outline;

        void overlay(Para p) {
            if (p == null) {
                return;
            }
            align = p.align != null ? p.align : align;
            before = p.before != null ? p.before : before;
            after = p.after != null ? p.after : after;
            indent = p.indent != null ? p.indent : indent;
            spacing = p.spacing != null ? p.spacing : spacing;
            pageBreak = p.pageBreak != null ? p.pageBreak : pageBreak;
            keepNext = p.keepNext != null ? p.keepNext : keepNext;
            numId = p.numId != null ? p.numId : numId;
            level = p.level != null ? p.level : level;
            outline = p.outline != null ? p.outline : outline;
        }
    }

    static final class Style {
        String id;
        String name;
        String basedOn;
        String type;
        final Props run = new Props();
        final Para para = new Para();
    }

    private XmlScanner scanner(String part) throws IOException {
        byte[] b = parts.get(part);
        return b == null ? null : new XmlScanner(new ByteArrayInputStream(b), budget);
    }

    private static boolean root(XmlScanner x) throws IOException {
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START) {
                return true;
            }
        }
        return false;
    }

    private Document read() throws IOException {
        readPackageRels();
        if (!parts.containsKey(main)) {
            if (parts.containsKey("EncryptedPackage") || parts.containsKey("content.xml")) {
                throw new IOException("Not a Word document");
            }
            throw new IOException("Not a Word document: the package has no main document part");
        }
        readRels(main);
        readTheme();
        readStyles();
        readNumbering();
        readNotes("footnotes", footnotes);
        readNotes("endnotes", endnotes);
        readComments();
        String title = readTitle();
        XmlScanner x = scanner(main);
        List<Block> blocks = new ArrayList<Block>();
        if (!root(x) || !"document".equals(x.name)) {
            throw new IOException("Not a Word document: the main part is not a document");
        }
        for (int e = x.next(); e != XmlScanner.END && e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START) {
                if ("body".equals(x.name)) {
                    content(x, blocks, true);
                } else {
                    x.skipElement();
                }
            }
        }
        return new Document(blocks, page, title, ids.peek(), buildComments(), readTrackChanges());
    }

    // ------------------------------------------------------------------ package plumbing

    private void readPackageRels() throws IOException {
        XmlScanner x = scanner("_rels/.rels");
        if (x == null) {
            return;
        }
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START && "Relationship".equals(x.name)) {
                String type = x.attr("Type");
                String target = x.attr("Target");
                if (type != null && target != null && type.endsWith(REL_OFFICE)) {
                    main = ZipParts.resolve("", target);
                }
            }
        }
    }

    private void readRels(String part) throws IOException {
        int slash = part.lastIndexOf('/');
        String relsPart = part.substring(0, slash + 1) + "_rels/" + part.substring(slash + 1)
                + ".rels";
        XmlScanner x = scanner(relsPart);
        if (x == null) {
            return;
        }
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START && "Relationship".equals(x.name)) {
                String id = x.attr("Id");
                String type = x.attr("Type");
                String target = x.attr("Target");
                if (id == null || target == null) {
                    continue;
                }
                boolean external = "External".equals(x.attr("TargetMode"));
                rels.put(id, new String[] {type == null ? "" : type,
                    external ? target : ZipParts.resolve(part, target), external ? "1" : ""});
            }
        }
    }

    private String relPart(String suffix) {
        for (String[] r : rels.values()) {
            if (r[0].endsWith("/" + suffix) && r[2].length() == 0) {
                return r[1];
            }
        }
        String guess = "word/" + suffix + ".xml";
        return parts.containsKey(guess) ? guess : null;
    }

    private void readTheme() throws IOException {
        String part = relPart("theme");
        XmlScanner x = part == null ? null : scanner(part);
        if (x == null) {
            return;
        }
        String which = null;
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START) {
                if ("majorFont".equals(x.name) || "minorFont".equals(x.name)) {
                    which = x.name;
                } else if ("latin".equals(x.name) && which != null) {
                    String face = x.attr("typeface");
                    if (face != null && face.length() > 0) {
                        if ("majorFont".equals(which)) {
                            majorFont = face;
                        } else {
                            minorFont = face;
                        }
                    }
                    which = null;
                }
            }
        }
    }

    private String readTitle() throws IOException {
        XmlScanner x = scanner("docProps/core.xml");
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

    private boolean readTrackChanges() throws IOException {
        String part = relPart("settings");
        XmlScanner x = part == null ? null : scanner(part);
        if (x == null) {
            return false;
        }
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START && "trackRevisions".equals(x.name)) {
                return onOff(x);
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ styles

    private void readStyles() throws IOException {
        String part = relPart("styles");
        XmlScanner x = part == null ? null : scanner(part);
        if (x == null) {
            return;
        }
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e != XmlScanner.START) {
                continue;
            }
            if ("rPrDefault".equals(x.name) || "pPrDefault".equals(x.name)) {
                continue;
            }
            if ("rPr".equals(x.name)) {
                runProps(x, defaults);
            } else if ("style".equals(x.name)) {
                Style s = new Style();
                s.id = x.attr("styleId");
                s.type = x.attr("type");
                boolean isDefault = onOffAttr(x.attr("default"));
                for (int f = x.next(); f != XmlScanner.END && f != XmlScanner.EOF; f = x.next()) {
                    if (f != XmlScanner.START) {
                        continue;
                    }
                    if ("name".equals(x.name)) {
                        s.name = x.attr("val");
                        x.skipElement();
                    } else if ("basedOn".equals(x.name)) {
                        s.basedOn = x.attr("val");
                        x.skipElement();
                    } else if ("rPr".equals(x.name)) {
                        runProps(x, s.run);
                    } else if ("pPr".equals(x.name)) {
                        paraProps(x, s.para, null);
                    } else {
                        x.skipElement();
                    }
                }
                if (s.id != null) {
                    styles.put(s.id, s);
                    if (isDefault && "paragraph".equals(s.type)) {
                        defaultParagraphStyle = s.id;
                    }
                }
            } else if (!"styles".equals(x.name) && !"docDefaults".equals(x.name)) {
                x.skipElement();
            }
        }
    }

    /// The run formatting a style gives, with everything it is based on.
    private Props styleRun(String id) {
        Props out = new Props();
        styleRun(id, out, 0);
        return out;
    }

    private void styleRun(String id, Props out, int depth) {
        Style s = id == null ? null : styles.get(id);
        if (s == null || depth > 20) {
            return;
        }
        styleRun(s.basedOn, out, depth + 1);
        out.overlay(s.run);
    }

    private Para stylePara(String id) {
        Para out = new Para();
        stylePara(id, out, 0);
        return out;
    }

    private void stylePara(String id, Para out, int depth) {
        Style s = id == null ? null : styles.get(id);
        if (s == null || depth > 20) {
            return;
        }
        stylePara(s.basedOn, out, depth + 1);
        out.overlay(s.para);
    }

    /// Our name for a style: `Heading1`..`Heading6`, `Title`, `Quote`, `Code`, `Normal`, or the
    /// style's own name.
    private String styleName(String id, Integer outline) {
        Style s = id == null ? null : styles.get(id);
        String name = s != null && s.name != null ? s.name : id;
        if (name == null) {
            return "Normal";
        }
        String fold = Ids.asciiLower(name).replace(" ", "");
        if (fold.startsWith("heading") && fold.length() == 8 && fold.charAt(7) >= '1'
                && fold.charAt(7) <= '6') {
            return "Heading" + fold.charAt(7);
        }
        if ("title".equals(fold)) {
            return "Title";
        }
        if ("quote".equals(fold) || "intensequote".equals(fold)) {
            return "Quote";
        }
        if ("normal".equals(fold)) {
            return "Normal";
        }
        if (fold.indexOf("code") >= 0 || "htmlpreformatted".equals(fold)) {
            return "Code";
        }
        if (outline != null && outline.intValue() >= 0 && outline.intValue() < 6) {
            return "Heading" + (outline.intValue() + 1);
        }
        return name;
    }

    // ------------------------------------------------------------------ numbering

    private void readNumbering() throws IOException {
        String part = relPart("numbering");
        XmlScanner x = part == null ? null : scanner(part);
        if (x == null) {
            return;
        }
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e != XmlScanner.START) {
                continue;
            }
            if ("abstractNum".equals(x.name)) {
                String id = x.attr("abstractNumId");
                String[] formats = new String[9];
                int[] starts = new int[9];
                int level = -1;
                int depth = 1;
                while (depth > 0) {
                    int f = x.next();
                    if (f == XmlScanner.EOF) {
                        break;
                    }
                    if (f == XmlScanner.END) {
                        depth--;
                        continue;
                    }
                    if (f != XmlScanner.START) {
                        continue;
                    }
                    depth++;
                    if ("lvl".equals(x.name)) {
                        level = x.intAttr("ilvl", -1);
                    } else if (level >= 0 && level < 9 && "numFmt".equals(x.name)) {
                        formats[level] = x.attr("val");
                    } else if (level >= 0 && level < 9 && "start".equals(x.name)) {
                        starts[level] = x.intAttr("val", 1);
                    }
                }
                if (id != null) {
                    abstractFormats.put(id, formats);
                    abstractStarts.put(id, starts);
                }
            } else if ("num".equals(x.name)) {
                String id = x.attr("numId");
                int[] overrides = new int[9];
                int level = -1;
                int depth = 1;
                while (depth > 0) {
                    int f = x.next();
                    if (f == XmlScanner.EOF) {
                        break;
                    }
                    if (f == XmlScanner.END) {
                        depth--;
                        continue;
                    }
                    if (f != XmlScanner.START) {
                        continue;
                    }
                    depth++;
                    if ("abstractNumId".equals(x.name) && id != null) {
                        numToAbstract.put(id, x.attr("val"));
                    } else if ("lvlOverride".equals(x.name)) {
                        level = x.intAttr("ilvl", -1);
                    } else if ("startOverride".equals(x.name) && level >= 0 && level < 9) {
                        overrides[level] = Math.max(1, x.intAttr("val", 1));
                    }
                }
                if (id != null) {
                    numOverrides.put(id, overrides);
                }
            }
        }
    }

    // ------------------------------------------------------------------ notes and comments

    private void readNotes(String kind, Map<String, List<Block>> into) throws IOException {
        String part = relPart(kind);
        XmlScanner x = part == null ? null : scanner(part);
        if (x == null) {
            return;
        }
        String element = kind.substring(0, kind.length() - 1);
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START && element.equals(x.name)) {
                String id = x.attr("id");
                String type = x.attr("type");
                List<Block> blocks = new ArrayList<Block>();
                content(x, blocks, false);
                if (id != null && (type == null || "normal".equals(type))) {
                    into.put(id, blocks);
                }
            }
        }
    }

    private void readComments() throws IOException {
        String part = relPart("comments");
        XmlScanner x = part == null ? null : scanner(part);
        if (x == null) {
            return;
        }
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START && "comment".equals(x.name)) {
                String id = x.attr("id");
                String author = x.attr("author");
                String date = x.attr("date");
                List<Block> blocks = new ArrayList<Block>();
                content(x, blocks, false);
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < blocks.size(); i++) {
                    if (i > 0) {
                        text.append('\n');
                    }
                    Document.appendPlainText(text, blocks.get(i));
                }
                if (id != null) {
                    commentData.put(id, new String[] {author, date, text.toString()});
                }
            }
        }
    }

    private List<Comment> buildComments() {
        List<Comment> out = new ArrayList<Comment>();
        for (String id : commentOrder) {
            String[] data = commentData.get(id);
            String[] start = commentStarts.get(id);
            String[] end = commentEnds.get(id);
            if (start == null) {
                continue;
            }
            if (end == null) {
                end = start;
            }
            out.add(new Comment("c" + id, data == null ? null : data[0], data == null ? null
                    : data[1], data == null ? null : data[2], false, start[0],
                    Integer.parseInt(start[1]), end[0], Integer.parseInt(end[1])));
        }
        return out;
    }

    // ------------------------------------------------------------------ block content

    /// Reads block content up to the end of the current element.
    private void content(XmlScanner x, List<Block> out, boolean body) throws IOException {
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The Word document is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("p".equals(n)) {
                paragraph(x, out);
            } else if ("tbl".equals(n)) {
                Table t = table(x);
                if (t != null) {
                    out.add(t);
                }
            } else if ("sdt".equals(n) || "sdtContent".equals(n) || "customXml".equals(n)
                    || "ins".equals(n) || "moveTo".equals(n)) {
                content(x, out, body);
            } else if ("sectPr".equals(n) && body) {
                section(x);
            } else {
                x.skipElement();
            }
        }
    }

    private void section(XmlScanner x) throws IOException {
        float w = 595.28f;
        float h = 841.89f;
        float top = 72;
        float right = 72;
        float bottom = 72;
        float left = 72;
        for (int e = x.next(); e != XmlScanner.END && e != XmlScanner.EOF; e = x.next()) {
            if (e != XmlScanner.START) {
                continue;
            }
            if ("pgSz".equals(x.name)) {
                w = twips(x.attr("w"), w);
                h = twips(x.attr("h"), h);
            } else if ("pgMar".equals(x.name)) {
                top = Math.abs(twips(x.attr("top"), top));
                right = twips(x.attr("right"), right);
                bottom = Math.abs(twips(x.attr("bottom"), bottom));
                left = twips(x.attr("left"), left);
            }
            x.skipElement();
        }
        try {
            page = new PageSpec(w, h, top, right, bottom, left);
        } catch (IllegalArgumentException ignored) {
            // margins that leave no room: keep the previous geometry
        }
    }

    private static float twips(String v, float fallback) {
        if (v == null) {
            return fallback;
        }
        try {
            return (float) (Double.parseDouble(v) / 20);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ------------------------------------------------------------------ paragraphs

    /// What a paragraph's runs need while they are being read.
    static final class ParaState {
        final String id;
        final RichText.Builder text = new RichText.Builder();
        Props base;
        String link = "";
        Revision revision;
        boolean pageBreak;
        int fieldDepth;
        boolean fieldResult;
        boolean fieldAbandoned;
        boolean fieldLocked;
        final StringBuilder instr = new StringBuilder();
        RichText.Builder result;
        TextStyle fieldStyle;
        ParaState(String id) {
            this.id = id;
        }

        void append(String s, TextStyle style) {
            if (fieldDepth > 0 && !fieldAbandoned) {
                if (fieldResult && result != null) {
                    if (fieldStyle == null) {
                        fieldStyle = style;
                    }
                    result.append(s, style);
                }
                return;
            }
            text.append(s, style);
        }

        void append(Inline o, TextStyle style) {
            if (fieldDepth > 0 && !fieldAbandoned) {
                if (fieldResult && result != null) {
                    result.append(o, style);
                }
                return;
            }
            text.append(o, style);
        }
    }

    private void paragraph(XmlScanner x, List<Block> out) throws IOException {
        Para direct = new Para();
        String styleId = null;
        ParaState st = new ParaState(ids.next('p'));
        boolean propsDone = false;
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The Word document is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            if ("pPr".equals(x.name)) {
                String[] sid = new String[1];
                paraProps(x, direct, sid);
                styleId = sid[0];
                continue;
            }
            if (!propsDone) {
                propsDone = true;
                st.base = paragraphBase(styleId);
            }
            inline(x, st);
        }
        if (!propsDone) {
            st.base = paragraphBase(styleId);
        }
        if (st.fieldDepth > 0 && st.result != null) {
            // a field whose result runs past this paragraph: keep what it showed here
            st.text.append(st.result.build());
            st.result = new RichText.Builder();
        }
        if (st.fieldDepth > 0) {
            st.fieldAbandoned = true;
            pendingField = st;
        }
        Para p = stylePara(styleId != null ? styleId : defaultParagraphStyle);
        p.overlay(direct);
        String list = Paragraph.NO_LIST;
        int level = p.level == null ? 0 : Math.max(0, Math.min(8, p.level.intValue()));
        int start = 0;
        if (p.numId != null && !"0".equals(p.numId)) {
            String abs = numToAbstract.get(p.numId);
            String[] formats = abs == null ? null : abstractFormats.get(abs);
            String fmt = formats == null ? null : formats[level];
            if (fmt != null && !"none".equals(fmt)) {
                list = "bullet".equals(fmt) ? Paragraph.BULLET : Paragraph.NUMBERED;
                if (Paragraph.NUMBERED.equals(list)) {
                    int[] over = numOverrides.get(p.numId);
                    boolean firstNum = seenNums.add(p.numId + ":" + level);
                    if (over != null && over[level] > 0 && firstNum) {
                        start = over[level];
                    } else if (abs != null && seenAbstracts.add(abs + ":" + level)) {
                        int[] starts = abstractStarts.get(abs);
                        int s = starts == null ? 1 : starts[level];
                        start = s > 1 ? s : 0;
                    }
                }
            }
        }
        String style = styleName(styleId != null ? styleId : defaultParagraphStyle, p.outline);
        RichText built = st.text.build();
        if (built.length() == 1 && built.objectAt(0) instanceof Picture
                && Paragraph.NO_LIST.equals(list) && (built.runStyle(0).link == null || built.runStyle(0).link.length() == 0)
                && built.runStyle(0).revision == null) {
            out.add((Picture) built.objectAt(0));
            return;
        }
        out.add(new Paragraph(st.id, style, p.align == null ? Paragraph.LEFT : p.align, list,
                level, start, p.before == null ? 0 : p.before.floatValue(),
                p.after == null ? 0 : p.after.floatValue(),
                p.indent == null ? 0 : Math.max(0, p.indent.floatValue()),
                p.spacing == null ? 1 : p.spacing.floatValue(),
                st.pageBreak || p.pageBreak != null && p.pageBreak.booleanValue(),
                p.keepNext != null && p.keepNext.booleanValue(), built));
    }

    /// A field still open when its paragraph ended; its result continues as ordinary text.
    private ParaState pendingField;

    private Props paragraphBase(String styleId) {
        Props base = defaults.copy();
        base.overlay(styleRun(styleId != null ? styleId : defaultParagraphStyle));
        return base;
    }

    private void paraProps(XmlScanner x, Para p, String[] styleId) throws IOException {
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The Word document is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("pStyle".equals(n) && styleId != null) {
                styleId[0] = x.attr("val");
            } else if ("jc".equals(n)) {
                String v = x.attr("val");
                p.align = "center".equals(v) ? Paragraph.CENTER : "right".equals(v)
                        || "end".equals(v) ? Paragraph.RIGHT : "both".equals(v)
                        || "distribute".equals(v) ? Paragraph.JUSTIFY : Paragraph.LEFT;
            } else if ("spacing".equals(n)) {
                if (x.attr("before") != null) {
                    p.before = Float.valueOf(twips(x.attr("before"), 0));
                }
                if (x.attr("after") != null) {
                    p.after = Float.valueOf(twips(x.attr("after"), 0));
                }
                String line = x.attr("line");
                String rule = x.attr("lineRule");
                if (line != null && (rule == null || "auto".equals(rule))) {
                    float v = Math.round(twips(line, 240) * 2000 / 240) / 100f;
                    if (v > 0 && v < 10) {
                        p.spacing = Float.valueOf(v);
                    }
                }
            } else if ("ind".equals(n)) {
                String left = x.attr("left") != null ? x.attr("left") : x.attr("start");
                if (left != null) {
                    p.indent = Float.valueOf(twips(left, 0));
                }
            } else if ("pageBreakBefore".equals(n)) {
                p.pageBreak = Boolean.valueOf(onOff(x));
            } else if ("keepNext".equals(n)) {
                p.keepNext = Boolean.valueOf(onOff(x));
            } else if ("outlineLvl".equals(n)) {
                p.outline = Integer.valueOf(x.intAttr("val", 9));
            } else if ("numPr".equals(n)) {
                paraProps(x, p, null);
                continue;
            } else if ("ilvl".equals(n)) {
                p.level = Integer.valueOf(x.intAttr("val", 0));
            } else if ("numId".equals(n)) {
                p.numId = x.attr("val");
            } else if ("sectPr".equals(n)) {
                section(x);
                continue;
            }
            x.skipElement();
        }
    }

    private void runProps(XmlScanner x, Props p) throws IOException {
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The Word document is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("rFonts".equals(n)) {
                String f = x.attr("ascii") != null ? x.attr("ascii") : x.attr("hAnsi");
                String theme = x.attr("asciiTheme") != null ? x.attr("asciiTheme")
                        : x.attr("hAnsiTheme");
                if (f != null) {
                    p.font = f;
                    p.fontTheme = null;
                } else if (theme != null) {
                    p.font = null;
                    p.fontTheme = theme;
                }
            } else if ("b".equals(n)) {
                p.bold = Boolean.valueOf(onOff(x));
            } else if ("i".equals(n)) {
                p.italic = Boolean.valueOf(onOff(x));
            } else if ("u".equals(n)) {
                String v = x.attr("val");
                p.underline = Boolean.valueOf(v == null || !"none".equals(v));
            } else if ("strike".equals(n) || "dstrike".equals(n)) {
                p.strike = Boolean.valueOf(onOff(x));
            } else if ("vanish".equals(n)) {
                p.hidden = Boolean.valueOf(onOff(x));
            } else if ("color".equals(n)) {
                String v = x.attr("val");
                p.color = v == null || "auto".equals(v) ? Integer.valueOf(TextStyle.DEFAULT.color)
                        : Integer.valueOf(hex(v, TextStyle.DEFAULT.color));
            } else if ("sz".equals(n)) {
                int half = x.intAttr("val", 0);
                if (half > 0 && half <= 3276) {
                    p.size = Float.valueOf(half / 2f);
                }
            } else if ("highlight".equals(n)) {
                p.background = Integer.valueOf(highlight(x.attr("val")));
            } else if ("shd".equals(n)) {
                String fill = x.attr("fill");
                if (fill != null && !"auto".equals(fill)) {
                    p.background = Integer.valueOf(hex(fill, TextStyle.NO_BACKGROUND));
                }
            } else if ("vertAlign".equals(n)) {
                String v = x.attr("val");
                p.script = "superscript".equals(v) ? TextStyle.SUPERSCRIPT : "subscript".equals(v)
                        ? TextStyle.SUBSCRIPT : TextStyle.BASELINE;
            }
            x.skipElement();
        }
    }

    private static boolean onOff(XmlScanner x) {
        return onOffAttr(x.attr("val") == null ? "true" : x.attr("val"));
    }

    private static boolean onOffAttr(String v) {
        return v != null && !"0".equals(v) && !"false".equals(v) && !"off".equals(v);
    }

    static int hex(String s, int fallback) {
        if (s == null || s.length() != 6) {
            return fallback;
        }
        int v = 0;
        for (int i = 0; i < 6; i++) {
            int d = Character.digit(s.charAt(i), 16);
            if (d < 0) {
                return fallback;
            }
            v = v * 16 + d;
        }
        return v;
    }

    private static final String[] HIGHLIGHTS = {"yellow", "ffff00", "green", "00ff00", "cyan",
        "00ffff", "magenta", "ff00ff", "blue", "0000ff", "red", "ff0000", "darkBlue", "000080",
        "darkCyan", "008080", "darkGreen", "008000", "darkMagenta", "800080", "darkRed",
        "800000", "darkYellow", "808000", "darkGray", "808080", "lightGray", "c0c0c0", "black",
        "000000", "white", "ffffff"};

    private static int highlight(String name) {
        for (int i = 0; i < HIGHLIGHTS.length; i += 2) {
            if (HIGHLIGHTS[i].equals(name)) {
                return hex(HIGHLIGHTS[i + 1], TextStyle.NO_BACKGROUND);
            }
        }
        return TextStyle.NO_BACKGROUND;
    }

    private TextStyle style(Props p, String link, Revision revision) {
        String font = p.font;
        if (font == null) {
            font = p.fontTheme != null && p.fontTheme.startsWith("major") ? majorFont : minorFont;
        }
        if ("Times New Roman".equals(font)) {
            font = "Serif";
        } else if ("Arial".equals(font)) {
            font = "SansSerif";
        } else if ("Courier New".equals(font)) {
            font = "Monospace";
        }
        float size = p.size == null ? 11 : p.size.floatValue();
        return new TextStyle(font, size, p.color == null ? TextStyle.DEFAULT.color
                : p.color.intValue(), Boolean.TRUE.equals(p.bold), Boolean.TRUE.equals(p.italic),
                Boolean.TRUE.equals(p.underline), Boolean.TRUE.equals(p.strike),
                p.background == null ? TextStyle.NO_BACKGROUND : p.background.intValue(), link,
                p.script, revision);
    }

    // ------------------------------------------------------------------ inline content

    private void inline(XmlScanner x, ParaState st) throws IOException {
        String n = x.name;
        if ("r".equals(n)) {
            run(x, st);
        } else if ("hyperlink".equals(n)) {
            String saved = st.link;
            String rid = x.attr("id");
            String anchor = x.attr("anchor");
            String[] rel = rid == null ? null : rels.get(rid);
            st.link = rel != null ? rel[1] : anchor != null ? "#" + anchor : "";
            children(x, st);
            st.link = saved;
        } else if ("ins".equals(n) || "moveTo".equals(n) || "del".equals(n)
                || "moveFrom".equals(n)) {
            Revision saved = st.revision;
            boolean deletion = "del".equals(n) || "moveFrom".equals(n);
            st.revision = new Revision(x.attr("id"), deletion ? Revision.DELETE : Revision.INSERT,
                    x.attr("author"), x.attr("date"));
            children(x, st);
            st.revision = saved;
        } else if ("fldSimple".equals(n)) {
            String instr = x.attr("instr");
            RichText.Builder saved = st.result;
            int depth = st.fieldDepth;
            boolean wasResult = st.fieldResult;
            st.fieldDepth = depth + 1;
            st.fieldResult = true;
            st.result = new RichText.Builder();
            st.fieldStyle = null;
            children(x, st);
            RichText result = st.result.build();
            st.result = saved;
            st.fieldDepth = depth;
            st.fieldResult = wasResult;
            emitField(st, instr, result);
        } else if ("smartTag".equals(n) || "customXml".equals(n) || "sdt".equals(n)
                || "sdtContent".equals(n) || "dir".equals(n) || "bdo".equals(n)) {
            children(x, st);
        } else if ("commentRangeStart".equals(n)) {
            String id = x.attr("id");
            if (id != null && !commentStarts.containsKey(id)) {
                commentStarts.put(id, new String[] {st.id, String.valueOf(st.text.length())});
                commentOrder.add(id);
            }
            x.skipElement();
        } else if ("commentRangeEnd".equals(n)) {
            String id = x.attr("id");
            if (id != null) {
                commentEnds.put(id, new String[] {st.id, String.valueOf(st.text.length())});
            }
            x.skipElement();
        } else {
            x.skipElement();
        }
    }

    private void children(XmlScanner x, ParaState st) throws IOException {
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The Word document is truncated");
            }
            if (e == XmlScanner.START) {
                inline(x, st);
            }
        }
    }

    private void emitField(ParaState st, String instr, RichText result) {
        String trimmed = instr == null ? "" : instr.trim();
        if (Ids.asciiLower(trimmed).startsWith("hyperlink")) {
            String target = hyperlinkTarget(trimmed);
            for (int i = 0; i < result.runCount(); i++) {
                Inline o = result.runObject(i);
                TextStyle s = result.runStyle(i).withLink(target);
                if (o != null) {
                    st.append(o, s);
                } else {
                    st.append(result.runText(i), s);
                }
            }
            return;
        }
        TextStyle s = st.fieldStyle != null ? st.fieldStyle : style(st.base, st.link, st.revision);
        st.append(new Field(trimmed, result.displayText(), st.fieldLocked), s);
        st.fieldLocked = false;
    }

    private static String hyperlinkTarget(String instr) {
        int q = instr.indexOf('"');
        if (q >= 0) {
            int end = instr.indexOf('"', q + 1);
            String url = end > q ? instr.substring(q + 1, end) : instr.substring(q + 1);
            if (instr.indexOf("\\l") >= 0 && instr.indexOf("\\l") < q) {
                return "#" + url;
            }
            return url;
        }
        String[] parts = instr.split(" ");
        return parts.length > 1 ? parts[parts.length - 1] : "";
    }

    private void run(XmlScanner x, ParaState st) throws IOException {
        Props p = st.base == null ? defaults.copy() : st.base.copy();
        Props direct = null;
        String charStyle = null;
        Note pendingNote = null;
        TextStyle pendingStyle = null;
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The Word document is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("rPr".equals(n)) {
                direct = new Props();
                charStyle = runPropsWithStyle(x, direct);
                if (charStyle != null) {
                    p.overlay(styleRun(charStyle));
                }
                p.overlay(direct);
                continue;
            }
            if (Boolean.TRUE.equals(p.hidden)) {
                x.skipElement();
                continue;
            }
            TextStyle style = style(p, st.link, st.revision);
            if (("t".equals(n) || "delText".equals(n)) && pendingNote != null) {
                String mark = x.elementText();
                st.append(new Note(pendingNote.id, pendingNote.kind, mark.length() == 0 ? null
                        : mark, pendingNote.blocks), pendingStyle);
                pendingNote = null;
                continue;
            }
            if ("t".equals(n) || "delText".equals(n)) {
                st.append(x.elementText(), style);
                continue;
            }
            if ("instrText".equals(n) || "delInstrText".equals(n)) {
                String t = x.elementText();
                if (st.fieldDepth == 1 && !st.fieldResult) {
                    st.instr.append(t);
                }
                continue;
            }
            if ("tab".equals(n) || "ptab".equals(n)) {
                st.append("\t", style);
            } else if ("br".equals(n)) {
                String type = x.attr("type");
                if ("page".equals(type)) {
                    if (st.text.length() == 0) {
                        st.pageBreak = true;
                    } else {
                        st.append("\n", style);
                    }
                } else {
                    st.append("\n", style);
                }
            } else if ("cr".equals(n)) {
                st.append("\n", style);
            } else if ("noBreakHyphen".equals(n)) {
                st.append("-", style);
            } else if ("softHyphen".equals(n)) {
                st.append(String.valueOf((char) 0xad), style);
            } else if ("sym".equals(n)) {
                int code = hex4(x.attr("char"));
                if (code >= 0xf000) {
                    code -= 0xf000;
                }
                if (code > 0) {
                    st.append(String.valueOf((char) code), style);
                }
            } else if ("fldChar".equals(n)) {
                if ("begin".equals(x.attr("fldCharType")) && st.fieldDepth == 0) {
                    st.fieldLocked = onOffAttr(x.attr("fldLock"));
                }
                fieldChar(x.attr("fldCharType"), st, style);
            } else if ("footnoteReference".equals(n) || "endnoteReference".equals(n)) {
                boolean foot = "footnoteReference".equals(n);
                String id = x.attr("id");
                List<Block> body = id == null ? null : (foot ? footnotes : endnotes).get(id);
                if (body != null) {
                    Note note = new Note("n" + ++noteCounter, foot ? Note.FOOTNOTE
                            : Note.ENDNOTE, null, body);
                    if (onOffAttr(x.attr("customMarkFollows"))) {
                        pendingNote = note;
                        pendingStyle = style;
                    } else {
                        st.append(note, style);
                    }
                }
            } else if ("drawing".equals(n)) {
                Picture pic = drawing(x);
                if (pic != null) {
                    st.append(pic, style);
                }
                continue;
            } else if ("pict".equals(n) || "object".equals(n)) {
                Picture pic = vml(x);
                if (pic != null) {
                    st.append(pic, style);
                }
                continue;
            }
            x.skipElement();
        }
        if (pendingNote != null) {
            st.append(pendingNote, pendingStyle);
        }
    }

    private String runPropsWithStyle(XmlScanner x, Props p) throws IOException {
        // rStyle sits inside rPr with the direct properties; read both in one pass
        String styleId = null;
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The Word document is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            if ("rStyle".equals(x.name)) {
                styleId = x.attr("val");
                x.skipElement();
            } else {
                Props one = new Props();
                singleRunProp(x, one);
                p.overlay(one);
            }
        }
        return styleId;
    }

    /// Reads one property element of an rPr, the scanner on its start tag.
    private void singleRunProp(XmlScanner x, Props p) throws IOException {
        // Reuse runProps by treating the element as a one-child container: emulate with a
        // small dispatcher over the same names.
        String n = x.name;
        if ("rFonts".equals(n)) {
            String f = x.attr("ascii") != null ? x.attr("ascii") : x.attr("hAnsi");
            String theme = x.attr("asciiTheme") != null ? x.attr("asciiTheme")
                    : x.attr("hAnsiTheme");
            if (f != null) {
                p.font = f;
            } else if (theme != null) {
                p.fontTheme = theme;
            }
        } else if ("b".equals(n)) {
            p.bold = Boolean.valueOf(onOff(x));
        } else if ("i".equals(n)) {
            p.italic = Boolean.valueOf(onOff(x));
        } else if ("u".equals(n)) {
            String v = x.attr("val");
            p.underline = Boolean.valueOf(v == null || !"none".equals(v));
        } else if ("strike".equals(n) || "dstrike".equals(n)) {
            p.strike = Boolean.valueOf(onOff(x));
        } else if ("vanish".equals(n)) {
            p.hidden = Boolean.valueOf(onOff(x));
        } else if ("color".equals(n)) {
            String v = x.attr("val");
            p.color = v == null || "auto".equals(v) ? Integer.valueOf(TextStyle.DEFAULT.color)
                    : Integer.valueOf(hex(v, TextStyle.DEFAULT.color));
        } else if ("sz".equals(n)) {
            int half = x.intAttr("val", 0);
            if (half > 0 && half <= 3276) {
                p.size = Float.valueOf(half / 2f);
            }
        } else if ("highlight".equals(n)) {
            p.background = Integer.valueOf(highlight(x.attr("val")));
        } else if ("shd".equals(n)) {
            String fill = x.attr("fill");
            if (fill != null && !"auto".equals(fill)) {
                p.background = Integer.valueOf(hex(fill, TextStyle.NO_BACKGROUND));
            }
        } else if ("vertAlign".equals(n)) {
            String v = x.attr("val");
            p.script = "superscript".equals(v) ? TextStyle.SUPERSCRIPT : "subscript".equals(v)
                    ? TextStyle.SUBSCRIPT : TextStyle.BASELINE;
        }
        x.skipElement();
    }

    private static int hex4(String s) {
        if (s == null || s.length() == 0 || s.length() > 4) {
            return -1;
        }
        int v = 0;
        for (int i = 0; i < s.length(); i++) {
            int d = Character.digit(s.charAt(i), 16);
            if (d < 0) {
                return -1;
            }
            v = v * 16 + d;
        }
        return v;
    }

    private void fieldChar(String type, ParaState st, TextStyle style) {
        if ("begin".equals(type)) {
            if (st.fieldDepth == 0 && pendingField != null) {
                pendingField = null;
            }
            st.fieldDepth++;
            if (st.fieldDepth == 1) {
                st.fieldResult = false;
                st.fieldAbandoned = false;
                st.instr.setLength(0);
                st.result = null;
                st.fieldStyle = null;
            }
        } else if ("separate".equals(type)) {
            if (st.fieldDepth == 1) {
                st.fieldResult = true;
                st.result = new RichText.Builder();
            }
        } else if ("end".equals(type)) {
            if (st.fieldDepth == 0) {
                // the end of a field that began in an earlier paragraph
                pendingField = null;
                return;
            }
            st.fieldDepth--;
            if (st.fieldDepth == 0) {
                if (!st.fieldAbandoned) {
                    RichText result = st.result == null ? RichText.EMPTY : st.result.build();
                    st.fieldDepth = 0;
                    emitField(st, st.instr.toString(), result);
                }
                st.fieldAbandoned = false;
                st.fieldResult = false;
                st.result = null;
            }
        }
    }

    // ------------------------------------------------------------------ pictures

    private Picture drawing(XmlScanner x) throws IOException {
        float w = 0;
        float h = 0;
        String alt = null;
        String embed = null;
        int depth = 1;
        while (depth > 0) {
            int e = x.next();
            if (e == XmlScanner.EOF) {
                throw new IOException("The Word document is truncated");
            }
            if (e == XmlScanner.END) {
                depth--;
                continue;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            depth++;
            if ("extent".equals(x.name) && w == 0) {
                w = (float) (x.doubleAttr("cx", 0) / 12700);
                h = (float) (x.doubleAttr("cy", 0) / 12700);
            } else if ("docPr".equals(x.name)) {
                alt = x.attr("descr");
                if (alt == null || alt.length() == 0) {
                    alt = x.attr("title");
                }
            } else if ("blip".equals(x.name) && embed == null) {
                embed = x.attr("embed");
            }
        }
        return picture(embed, w, h, alt);
    }

    private Picture vml(XmlScanner x) throws IOException {
        String embed = null;
        float w = 0;
        float h = 0;
        int depth = 1;
        while (depth > 0) {
            int e = x.next();
            if (e == XmlScanner.EOF) {
                throw new IOException("The Word document is truncated");
            }
            if (e == XmlScanner.END) {
                depth--;
                continue;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            depth++;
            if ("shape".equals(x.name) && x.attr("style") != null) {
                String style = x.attr("style");
                w = cssPoints(style, "width");
                h = cssPoints(style, "height");
            } else if ("imagedata".equals(x.name) && embed == null) {
                embed = x.attr("id");
            }
        }
        return picture(embed, w, h, null);
    }

    private static float cssPoints(String style, String key) {
        int i = style.indexOf(key + ":");
        if (i < 0) {
            return 0;
        }
        int end = style.indexOf(';', i);
        String v = (end < 0 ? style.substring(i + key.length() + 1)
                : style.substring(i + key.length() + 1, end)).trim();
        float scale = 1;
        if (v.endsWith("pt")) {
            v = v.substring(0, v.length() - 2);
        } else if (v.endsWith("in")) {
            v = v.substring(0, v.length() - 2);
            scale = 72;
        } else if (v.endsWith("px")) {
            v = v.substring(0, v.length() - 2);
            scale = 0.75f;
        }
        try {
            return Float.parseFloat(v) * scale;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Picture picture(String rid, float w, float h, String alt) {
        String[] rel = rid == null ? null : rels.get(rid);
        if (rel == null || rel[2].length() > 0) {
            return null;
        }
        byte[] data = parts.get(rel[1]);
        if (data == null || data.length == 0) {
            return null;
        }
        if (Float.isNaN(w) || Float.isNaN(h) || w <= 0 || h <= 0 || w > 20000 || h > 20000) {
            w = 100;
            h = 100;
        }
        return new Picture(ids.next('i'), data, null, w, h, alt);
    }

    // ------------------------------------------------------------------ tables

    private Table table(XmlScanner x) throws IOException {
        String id = ids.next('t');
        List<List<List<Block>>> rows = new ArrayList<List<List<Block>>>();
        // per row, per grid column: 0 = own cell, 1 = covered horizontally, 2 = vMerge continue
        List<int[]> spans = new ArrayList<int[]>();
        List<int[]> kinds = new ArrayList<int[]>();
        int headerRows = 0;
        boolean headerRun = true;
        for (int e = x.next(); e != XmlScanner.END; e = x.next()) {
            if (e == XmlScanner.EOF) {
                throw new IOException("The Word document is truncated");
            }
            if (e != XmlScanner.START) {
                continue;
            }
            if (!"tr".equals(x.name)) {
                x.skipElement();
                continue;
            }
            if (rows.size() >= MAX_ROWS) {
                x.skipElement();
                continue;
            }
            List<List<Block>> row = new ArrayList<List<Block>>();
            int[] span = new int[MAX_COLUMNS];
            int[] kind = new int[MAX_COLUMNS];
            boolean header = false;
            int col = 0;
            for (int f = x.next(); f != XmlScanner.END; f = x.next()) {
                if (f == XmlScanner.EOF) {
                    throw new IOException("The Word document is truncated");
                }
                if (f != XmlScanner.START) {
                    continue;
                }
                if ("trPr".equals(x.name)) {
                    for (int g = x.next(); g != XmlScanner.END && g != XmlScanner.EOF;
                            g = x.next()) {
                        if (g == XmlScanner.START) {
                            if ("tblHeader".equals(x.name)) {
                                header = onOff(x);
                            } else if ("gridBefore".equals(x.name)) {
                                int skip = Math.max(0, Math.min(MAX_COLUMNS, x.intAttr("val", 0)));
                                while (row.size() < skip) {
                                    row.add(new ArrayList<Block>());
                                    span[col] = 1;
                                    col++;
                                }
                            }
                            x.skipElement();
                        }
                    }
                    continue;
                }
                if (!"tc".equals(x.name)) {
                    x.skipElement();
                    continue;
                }
                int gridSpan = 1;
                int vmerge = 0;
                List<Block> cell = new ArrayList<Block>();
                for (int g = x.next(); g != XmlScanner.END; g = x.next()) {
                    if (g == XmlScanner.EOF) {
                        throw new IOException("The Word document is truncated");
                    }
                    if (g != XmlScanner.START) {
                        continue;
                    }
                    if ("tcPr".equals(x.name)) {
                        for (int h = x.next(); h != XmlScanner.END && h != XmlScanner.EOF;
                                h = x.next()) {
                            if (h == XmlScanner.START) {
                                if ("gridSpan".equals(x.name)) {
                                    gridSpan = Math.max(1, Math.min(MAX_COLUMNS,
                                            x.intAttr("val", 1)));
                                } else if ("vMerge".equals(x.name)) {
                                    vmerge = "restart".equals(x.attr("val")) ? 1 : 2;
                                }
                                x.skipElement();
                            }
                        }
                    } else if ("p".equals(x.name)) {
                        paragraph(x, cell);
                    } else if ("tbl".equals(x.name)) {
                        Table inner = table(x);
                        if (inner != null) {
                            cell.add(inner);
                        }
                    } else if ("sdt".equals(x.name) || "sdtContent".equals(x.name)
                            || "customXml".equals(x.name)) {
                        content(x, cell, false);
                    } else {
                        x.skipElement();
                    }
                }
                if (col >= MAX_COLUMNS) {
                    continue;
                }
                gridSpan = Math.min(gridSpan, MAX_COLUMNS - col);
                row.add(cell);
                span[col] = gridSpan;
                kind[col] = vmerge == 2 ? 2 : 0;
                col++;
                for (int k = 1; k < gridSpan; k++) {
                    row.add(new ArrayList<Block>());
                    span[col] = 1;
                    kind[col] = 1;
                    col++;
                }
            }
            if (row.isEmpty()) {
                continue;
            }
            if (header && headerRun) {
                headerRows++;
            } else {
                headerRun = false;
            }
            rows.add(row);
            spans.add(span);
            kinds.add(kind);
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
        boolean[][] used = new boolean[rows.size()][columns];
        for (int r = 0; r < rows.size(); r++) {
            int[] span = spans.get(r);
            int[] kind = kinds.get(r);
            for (int c = 0; c < columns; c++) {
                if (kind[c] != 0 || used[r][c]) {
                    continue;
                }
                int w = Math.max(1, span[c]);
                int h = 1;
                while (r + h < rows.size() && kinds.get(r + h)[c] == 2 && spans.get(r + h)[c] == w) {
                    h++;
                }
                boolean free = true;
                for (int rr = r; rr < r + h && free; rr++) {
                    for (int cc = c; cc < c + w && cc < columns; cc++) {
                        if (used[rr][cc]) {
                            free = false;
                            break;
                        }
                    }
                }
                if (!free) {
                    continue;
                }
                for (int rr = r; rr < r + h; rr++) {
                    for (int cc = c; cc < c + w && cc < columns; cc++) {
                        used[rr][cc] = true;
                    }
                }
                if (w > 1 || h > 1) {
                    merges.add(new Table.Merge(r, c, h, Math.min(w, columns - c)));
                }
            }
        }
        return new Table(id, rows, merges, Math.min(headerRows, rows.size()));
    }
}
