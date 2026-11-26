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
import com.codename1.impl.xml.XmlScanner;
import com.codename1.io.zip.ZipArchiveOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.BlockXml;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.Table;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// `.cn1slides`: a ZIP holding `presentation.xml`. Lossless for the model. Text frames,
/// notes, tables and pictures are written in the `.cn1doc` vocabulary through `BlockXml`,
/// so a slide's text and a document's text are one format.
final class NativeSlidesFormat {
    /// The most nodes, and the most edges, a diagram may have.
    static final int MAX_DIAGRAM_ITEMS = 2000;

    private static final String ENTRY = "presentation.xml";
    private static final int MAX_DEPTH = 32;
    private static final int MAX_SERIES_VALUES = 100000;

    private NativeSlidesFormat() {
    }

    static void write(Presentation p, OutputStream out) throws IOException {
        ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out);
        zip.putNextEntry(new ZipEntry(ENTRY));
        XmlOut x = new XmlOut(zip);
        x.raw("<?xml version=\"1.0\" encoding=\"UTF-8\"?><presentation version=\"1\"");
        x.attr("width", fmt(p.width)).attr("height", fmt(p.height)).attr("title", p.title)
                .attr("next", String.valueOf(p.nextId)).raw(">");
        theme(x, p.theme);
        for (Slide s : p.slides) {
            slide(x, s);
        }
        x.raw("</presentation>");
        x.flush();
        zip.closeEntry();
        zip.finish();
    }

    static String fmt(float f) {
        if (f == (int) f) {
            return String.valueOf((int) f);
        }
        return String.valueOf(Math.round(f * 1000) / 1000.0);
    }

    private static String hex(int c) {
        String h = Integer.toHexString(c & 0xffffff);
        while (h.length() < 6) {
            h = "0" + h;
        }
        return h;
    }

    private static void theme(XmlOut x, Theme t) throws IOException {
        x.raw("<theme").attr("name", t.name).attr("major", t.majorFont)
                .attr("minor", t.minorFont);
        for (int i = 0; i < Theme.NAMES.length; i++) {
            x.attr(Theme.NAMES[i], hex(t.color(i)));
        }
        x.raw("/>");
    }

    private static void slide(XmlOut x, Slide s) throws IOException {
        x.raw("<slide").attr("id", s.id).attr("layout", s.layout);
        if (s.hidden) {
            x.attr("hidden", "true");
        }
        x.raw(">");
        fill(x, "background", s.background);
        Transition t = s.transition;
        if (!Transition.DEFAULT.equals(t)) {
            x.raw("<transition").attr("type", t.type).attr("duration", String.valueOf(t.duration))
                    .attr("direction", t.direction)
                    .attr("onClick", String.valueOf(t.advanceOnClick))
                    .attr("after", String.valueOf(t.advanceAfter)).raw("/>");
        }
        for (SlideElement e : s.elements) {
            element(x, e);
        }
        for (Build b : s.builds) {
            x.raw("<build").attr("element", b.elementId).attr("kind", b.kind)
                    .attr("effect", b.effect).attr("trigger", b.trigger)
                    .attr("duration", String.valueOf(b.duration))
                    .attr("delay", String.valueOf(b.delay)).attr("direction", b.direction)
                    .raw("/>");
        }
        if (!s.notes.isEmpty()) {
            x.raw("<notes>");
            BlockXml.write(x, s.notes);
            x.raw("</notes>");
        }
        x.raw("</slide>");
    }

    private static void fill(XmlOut x, String tag, Fill f) throws IOException {
        if (f == null || f.kind == Fill.NONE) {
            return;
        }
        x.raw("<").raw(tag).attr("kind", f.kind == Fill.SOLID ? "solid"
                : f.kind == Fill.GRADIENT ? "gradient" : "picture")
                .attr("color", hex(f.color));
        if (f.kind == Fill.GRADIENT) {
            x.attr("color2", hex(f.color2)).attr("angle", fmt(f.angle));
        }
        if (f.alpha < 1) {
            x.attr("alpha", fmt(f.alpha));
        }
        if (f.kind == Fill.PICTURE && f.picture != null) {
            x.raw(">");
            picture(x, f.picture);
            x.raw("</").raw(tag).raw(">");
        } else {
            x.raw("/>");
        }
    }

    private static void picture(XmlOut x, Picture p) throws IOException {
        List<Block> one = new ArrayList<Block>();
        one.add(p);
        BlockXml.write(x, one);
    }

    private static void line(XmlOut x, LineStyle l) throws IOException {
        if (l == null || !l.isVisible()) {
            return;
        }
        x.raw("<line").attr("color", hex(l.color)).attr("width", fmt(l.width))
                .attr("dash", l.dash).attr("alpha", fmt(l.alpha)).attr("head", l.head)
                .attr("tail", l.tail).raw("/>");
    }

    private static void frame(XmlOut x, SlideElement e) throws IOException {
        x.attr("id", e.id).attr("name", e.name).attr("x", fmt(e.x)).attr("y", fmt(e.y))
                .attr("w", fmt(e.width)).attr("h", fmt(e.height));
        if (e.rotation != 0) {
            x.attr("rot", fmt(e.rotation));
        }
        if (e.flipH) {
            x.attr("flipH", "true");
        }
        if (e.flipV) {
            x.attr("flipV", "true");
        }
    }

    private static void element(XmlOut x, SlideElement e) throws IOException {
        if (e instanceof ShapeElement) {
            ShapeElement s = (ShapeElement) e;
            x.raw("<shape");
            frame(x, e);
            x.attr("geometry", s.geometry).attr("placeholder", s.placeholder);
            if (s.adjustCount() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < s.adjustCount(); i++) {
                    if (i > 0) {
                        sb.append(' ');
                    }
                    sb.append(fmt(s.adjust(i, 0)));
                }
                x.attr("adjust", sb.toString());
            }
            x.raw(">");
            fill(x, "fill", s.fill);
            line(x, s.line);
            if (s.text != null) {
                TextFrame t = s.text;
                x.raw("<text").attr("l", fmt(t.left)).attr("t", fmt(t.top))
                        .attr("r", fmt(t.right)).attr("b", fmt(t.bottom))
                        .attr("anchor", t.anchor).attr("autofit", t.autofit)
                        .attr("wrap", String.valueOf(t.wrap)).raw(">");
                BlockXml.write(x, t.blocks);
                x.raw("</text>");
            }
            x.raw("</shape>");
        } else if (e instanceof PictureElement) {
            PictureElement p = (PictureElement) e;
            x.raw("<image");
            frame(x, e);
            x.attr("cropL", fmt(p.cropLeft)).attr("cropT", fmt(p.cropTop))
                    .attr("cropR", fmt(p.cropRight)).attr("cropB", fmt(p.cropBottom)).raw(">");
            line(x, p.line);
            picture(x, p.picture);
            x.raw("</image>");
        } else if (e instanceof TableElement) {
            x.raw("<table");
            frame(x, e);
            x.raw(">");
            List<Block> one = new ArrayList<Block>();
            one.add(((TableElement) e).table);
            BlockXml.write(x, one);
            x.raw("</table>");
        } else if (e instanceof ChartElement) {
            chart(x, (ChartElement) e);
        } else if (e instanceof DiagramElement) {
            DiagramElement d = (DiagramElement) e;
            x.raw("<diagram");
            frame(x, e);
            x.attr("arrangement", d.arrangement).attr("color", hex(d.color))
                    .attr("textColor", hex(d.textColor)).raw(">");
            for (DiagramElement.Node n : d.nodes) {
                x.raw("<node").attr("id", n.id).attr("label", n.label).attr("shape", n.shape)
                        .attr("color", n.color < 0 ? null : hex(n.color)).raw("/>");
            }
            for (String[] edge : d.edges) {
                x.raw("<edge").attr("from", edge[0]).attr("to", edge[1]).raw("/>");
            }
            x.raw("</diagram>");
        } else if (e instanceof GroupElement) {
            x.raw("<group");
            frame(x, e);
            x.raw(">");
            for (SlideElement c : ((GroupElement) e).children) {
                element(x, c);
            }
            x.raw("</group>");
        }
    }

    private static void chart(XmlOut x, ChartElement c) throws IOException {
        x.raw("<chart");
        frame(x, c);
        x.attr("kind", c.kind).attr("title", c.title)
                .attr("stacked", String.valueOf(c.stacked))
                .attr("legend", String.valueOf(c.legend)).raw(">");
        for (int i = 0; i < c.categoryCount(); i++) {
            x.raw("<category").attr("name", c.category(i)).raw("/>");
        }
        for (int i = 0; i < c.seriesCount(); i++) {
            x.raw("<series").attr("name", c.seriesName(i));
            int col = c.seriesColor(i);
            if (col >= 0) {
                x.attr("color", hex(col));
            }
            StringBuilder sb = new StringBuilder();
            double[] v = c.values(i);
            for (int j = 0; j < v.length; j++) {
                if (j > 0) {
                    sb.append(' ');
                }
                sb.append(v[j]);
            }
            x.attr("values", sb.toString()).raw("/>");
        }
        x.raw("</chart>");
    }

    // ------------------------------------------------------------------ reading

    static Presentation read(InputStream in) throws IOException {
        ZipInputStream zip = new ZipInputStream(in);
        for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
            if (ENTRY.equals(e.getName())) {
                return parse(new XmlScanner(zip));
            }
        }
        throw new IOException("Not a .cn1slides file: no " + ENTRY);
    }

    private static IOException truncated() {
        return new IOException("Truncated .cn1slides presentation");
    }

    private static Presentation parse(XmlScanner x) throws IOException {
        if (x.next() != XmlScanner.START || !"presentation".equals(x.name)) {
            throw new IOException("Not a .cn1slides presentation");
        }
        if (!"1".equals(x.attr("version"))) {
            throw new IOException("Unsupported .cn1slides version " + x.attr("version"));
        }
        float w = (float) x.doubleAttr("width", Presentation.WIDE_WIDTH);
        float h = (float) x.doubleAttr("height", Presentation.WIDE_HEIGHT);
        // !(w > 0) also refuses NaN, which w <= 0 lets through
        if (!(w > 0) || !(h > 0) || w > 100000 || h > 100000) { // NOPMD LogicInversion
            throw new IOException("Bad slide size");
        }
        String title = x.attr("title");
        long next = (long) x.doubleAttr("next", 1);
        Theme theme = Theme.DEFAULT;
        List<Slide> slides = new ArrayList<Slide>();
        Set<String> ids = new HashSet<String>();
        for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw truncated();
            }
            if (ev != XmlScanner.START) {
                continue;
            }
            if ("theme".equals(x.name)) {
                theme = readTheme(x);
                x.skipElement();
            } else if ("slide".equals(x.name)) {
                slides.add(readSlide(x, ids));
            } else {
                x.skipElement();
            }
        }
        return new Presentation(w, h, slides, theme, title, next, null);
    }

    private static Theme readTheme(XmlScanner x) {
        int[] colors = new int[Theme.NAMES.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = color(x.attr(Theme.NAMES[i]), Theme.DEFAULT.color(i));
        }
        String major = x.attr("major");
        String minor = x.attr("minor");
        return new Theme(x.attr("name"), colors, major == null ? Theme.DEFAULT.majorFont : major,
                minor == null ? Theme.DEFAULT.minorFont : minor);
    }

    static int color(String s, int fallback) {
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

    private static String uniqueId(String id, Set<String> ids, char prefix) {
        String v = id == null || id.length() == 0 ? prefix + "0" : id;
        String c = v;
        for (int i = 1; !ids.add(c); i++) {
            c = v + "_" + i;
        }
        return c;
    }

    private static Slide readSlide(XmlScanner x, Set<String> ids) throws IOException {
        String id = uniqueId(x.attr("id"), ids, 's');
        String layout = x.attr("layout");
        boolean hidden = x.boolAttr("hidden");
        Fill bg = Fill.NO_FILL;
        Transition tr = Transition.DEFAULT;
        List<SlideElement> elements = new ArrayList<SlideElement>();
        List<Build> builds = new ArrayList<Build>();
        List<Block> notes = new ArrayList<Block>();
        Set<String> blockIds = new HashSet<String>();
        for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw truncated();
            }
            if (ev != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("background".equals(n)) {
                bg = readFill(x, blockIds);
            } else if ("transition".equals(n)) {
                tr = new Transition(x.attr("type"), x.intAttr("duration", 0), x.attr("direction"),
                        !"false".equals(x.attr("onClick")), x.intAttr("after", -1));
                x.skipElement();
            } else if ("build".equals(n)) {
                String el = x.attr("element");
                if (el != null && el.length() > 0) {
                    builds.add(new Build(el, x.attr("kind"), x.attr("effect"), x.attr("trigger"),
                            x.intAttr("duration", 500), x.intAttr("delay", 0),
                            x.attr("direction")));
                }
                x.skipElement();
            } else if ("notes".equals(n)) {
                readBlocks(x, notes, blockIds);
            } else {
                SlideElement e = readElement(x, ids, blockIds, 0);
                if (e != null) {
                    elements.add(e);
                }
            }
        }
        return new Slide(id, layout, bg, elements, notes, tr, builds, hidden);
    }

    private static void readBlocks(XmlScanner x, List<Block> out, Set<String> blockIds)
            throws IOException {
        for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw truncated();
            }
            if (ev == XmlScanner.START && !BlockXml.read(x, out, blockIds)) {
                x.skipElement();
            }
        }
    }

    private static Picture firstPicture(List<Block> blocks) {
        for (Block b : blocks) {
            if (b instanceof Picture) {
                return (Picture) b;
            }
        }
        return null;
    }

    private static Fill readFill(XmlScanner x, Set<String> blockIds) throws IOException {
        String kind = x.attr("kind");
        int c = color(x.attr("color"), 0xffffff);
        float alpha = (float) x.doubleAttr("alpha", 1);
        if (Float.isNaN(alpha)) {
            alpha = 1;
        }
        Fill f;
        if ("gradient".equals(kind)) {
            float angle = (float) x.doubleAttr("angle", 90);
            f = Fill.gradient(c, color(x.attr("color2"), c), Float.isNaN(angle) ? 90 : angle);
            x.skipElement();
        } else if ("picture".equals(kind)) {
            List<Block> b = new ArrayList<Block>();
            readBlocks(x, b, blockIds);
            Picture p = firstPicture(b);
            f = p == null ? Fill.NO_FILL : Fill.picture(p);
        } else {
            f = Fill.solid(c);
            x.skipElement();
        }
        return f.kind == Fill.NONE ? f : f.withAlpha(Math.max(0, Math.min(1, alpha)));
    }

    private static LineStyle readLine(XmlScanner x) throws IOException {
        float width = (float) x.doubleAttr("width", 1);
        float alpha = (float) x.doubleAttr("alpha", 1);
        LineStyle l = new LineStyle(color(x.attr("color"), 0), Float.isNaN(width) ? 1
                : Math.max(0, Math.min(200, width)), x.attr("dash"), Float.isNaN(alpha) ? 1
                : Math.max(0, Math.min(1, alpha)), x.attr("head"), x.attr("tail"));
        x.skipElement();
        return l;
    }

    private static float f(XmlScanner x, String name, float fallback) {
        float v = (float) x.doubleAttr(name, fallback);
        return Float.isNaN(v) || Float.isInfinite(v) ? fallback : v;
    }

    private static SlideElement readElement(XmlScanner x, Set<String> ids, Set<String> blockIds,
            int depth) throws IOException {
        String n = x.name;
        if (depth > MAX_DEPTH) {
            throw new IOException("Groups nested too deeply");
        }
        boolean known = "shape".equals(n) || "image".equals(n) || "table".equals(n)
                || "chart".equals(n) || "diagram".equals(n) || "group".equals(n);
        if (!known) {
            x.skipElement();
            return null;
        }
        String id = uniqueId(x.attr("id"), ids, 'e');
        String name = x.attr("name");
        float ex = f(x, "x", 0);
        float ey = f(x, "y", 0);
        float ew = Math.max(0, f(x, "w", 0));
        float eh = Math.max(0, f(x, "h", 0));
        float rot = f(x, "rot", 0);
        boolean flipH = x.boolAttr("flipH");
        boolean flipV = x.boolAttr("flipV");
        try {
            if ("shape".equals(n)) {
                String geometry = x.attr("geometry");
                String placeholder = x.attr("placeholder");
                float[] adjust = adjust(x.attr("adjust"));
                Fill fill = Fill.NO_FILL;
                LineStyle line = LineStyle.NONE;
                TextFrame text = null;
                for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
                    if (ev == XmlScanner.EOF) {
                        throw truncated();
                    }
                    if (ev != XmlScanner.START) {
                        continue;
                    }
                    if ("fill".equals(x.name)) {
                        fill = readFill(x, blockIds);
                    } else if ("line".equals(x.name)) {
                        line = readLine(x);
                    } else if ("text".equals(x.name)) {
                        float l = f(x, "l", 7.2f);
                        float t = f(x, "t", 3.6f);
                        float r = f(x, "r", 7.2f);
                        float b = f(x, "b", 3.6f);
                        String anchor = x.attr("anchor");
                        String autofit = x.attr("autofit");
                        boolean wrap = !"false".equals(x.attr("wrap"));
                        List<Block> blocks = new ArrayList<Block>();
                        readBlocks(x, blocks, blockIds);
                        text = new TextFrame(blocks, l, t, r, b, anchor, autofit, wrap);
                    } else {
                        x.skipElement();
                    }
                }
                return new ShapeElement(id, name, geometry, ex, ey, ew, eh, rot, flipH, flipV,
                        fill, line, text, placeholder, adjust);
            }
            if ("image".equals(n)) {
                float cl = f(x, "cropL", 0);
                float ct = f(x, "cropT", 0);
                float cr = f(x, "cropR", 0);
                float cb = f(x, "cropB", 0);
                LineStyle line = LineStyle.NONE;
                List<Block> b = new ArrayList<Block>();
                for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
                    if (ev == XmlScanner.EOF) {
                        throw truncated();
                    }
                    if (ev != XmlScanner.START) {
                        continue;
                    }
                    if ("line".equals(x.name)) {
                        line = readLine(x);
                    } else if (!BlockXml.read(x, b, blockIds)) {
                        x.skipElement();
                    }
                }
                Picture p = firstPicture(b);
                return p == null ? null : new PictureElement(id, name, p, ex, ey, ew, eh, rot,
                        flipH, flipV, cl, ct, cr, cb, line);
            }
            if ("table".equals(n)) {
                List<Block> b = new ArrayList<Block>();
                readBlocks(x, b, blockIds);
                for (Block blk : b) {
                    if (blk instanceof Table) {
                        return new TableElement(id, name, (Table) blk, ex, ey, ew, eh, rot);
                    }
                }
                return null;
            }
            if ("chart".equals(n)) {
                return readChart(x, id, name, ex, ey, ew, eh, rot);
            }
            if ("diagram".equals(n)) {
                String arrangement = x.attr("arrangement");
                int color = color(x.attr("color"), 0x4472c4);
                int textColor = color(x.attr("textColor"), 0xffffff);
                List<DiagramElement.Node> nodes = new ArrayList<DiagramElement.Node>();
                List<String[]> edges = new ArrayList<String[]>();
                for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
                    if (ev == XmlScanner.EOF) {
                        throw truncated();
                    }
                    if (ev != XmlScanner.START) {
                        continue;
                    }
                    if ("node".equals(x.name) && x.attr("id") != null) {
                        nodes.add(new DiagramElement.Node(x.attr("id"), x.attr("label"),
                                x.attr("shape"), color(x.attr("color"), -1)));
                    } else if ("edge".equals(x.name) && x.attr("from") != null
                            && x.attr("to") != null) {
                        edges.add(new String[] {x.attr("from"), x.attr("to")});
                    }
                    x.skipElement();
                    // a diagram is laid out as a whole when it paints: bounded like a chart
                    if (nodes.size() > MAX_DIAGRAM_ITEMS || edges.size() > MAX_DIAGRAM_ITEMS) {
                        throw new IOException("Diagram too large");
                    }
                }
                return new DiagramElement(id, name, arrangement, ex, ey, ew, eh, rot, nodes,
                        edges, color, textColor);
            }
            List<SlideElement> kids = new ArrayList<SlideElement>();
            for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
                if (ev == XmlScanner.EOF) {
                    throw truncated();
                }
                if (ev == XmlScanner.START) {
                    SlideElement c = readElement(x, ids, blockIds, depth + 1);
                    if (c != null) {
                        kids.add(c);
                    }
                }
            }
            return new GroupElement(id, name, kids, ex, ey, ew, eh, rot, flipH, flipV);
        } catch (IllegalArgumentException e) {
            // CLDC's IOException takes no cause; the message carries it
            throw new IOException("Damaged " + n + " " + id + ": " + e.getMessage()); // NOPMD PreserveStackTrace
        }
    }

    private static float[] adjust(String s) {
        if (s == null || s.trim().length() == 0) {
            return null;
        }
        List<Float> l = new ArrayList<Float>();
        int i = 0;
        int n = s.length();
        while (i < n && l.size() < 16) {
            while (i < n && s.charAt(i) == ' ') {
                i++;
            }
            int st = i;
            while (i < n && s.charAt(i) != ' ') {
                i++;
            }
            if (i > st) {
                try {
                    float v = Float.parseFloat(s.substring(st, i));
                    l.add(Float.valueOf(Float.isNaN(v) || Float.isInfinite(v) ? 0 : v));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        float[] a = new float[l.size()];
        for (int k = 0; k < a.length; k++) {
            a[k] = l.get(k).floatValue();
        }
        return a;
    }

    private static ChartElement readChart(XmlScanner x, String id, String name, float ex,
            float ey, float ew, float eh, float rot) throws IOException {
        String kind = x.attr("kind");
        String title = x.attr("title");
        boolean stacked = x.boolAttr("stacked");
        boolean legend = !"false".equals(x.attr("legend"));
        List<String> cats = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        List<double[]> values = new ArrayList<double[]>();
        List<Integer> colors = new ArrayList<Integer>();
        int total = 0;
        for (int ev = x.next(); ev != XmlScanner.END; ev = x.next()) {
            if (ev == XmlScanner.EOF) {
                throw truncated();
            }
            if (ev != XmlScanner.START) {
                continue;
            }
            if ("category".equals(x.name)) {
                cats.add(x.attr("name"));
            } else if ("series".equals(x.name)) {
                names.add(x.attr("name"));
                colors.add(Integer.valueOf(color(x.attr("color"), -1)));
                double[] v = numbers(x.attr("values"), MAX_SERIES_VALUES - total);
                total += v.length;
                values.add(v);
            }
            x.skipElement();
        }
        int[] c = new int[colors.size()];
        for (int i = 0; i < c.length; i++) {
            c[i] = colors.get(i).intValue();
        }
        return new ChartElement(id, name, kind, ex, ey, ew, eh, rot, title, stacked, legend,
                cats.toArray(new String[cats.size()]), names.toArray(new String[names.size()]),
                values.toArray(new double[values.size()][]), c);
    }

    private static double[] numbers(String s, int max) throws IOException {
        if (s == null) {
            return new double[0];
        }
        List<Double> l = new ArrayList<Double>();
        int i = 0;
        int n = s.length();
        while (i < n) {
            while (i < n && s.charAt(i) == ' ') {
                i++;
            }
            int st = i;
            while (i < n && s.charAt(i) != ' ') {
                i++;
            }
            if (i > st) {
                if (l.size() >= max) {
                    throw new IOException("Chart has too many values");
                }
                try {
                    l.add(Double.valueOf(Double.parseDouble(s.substring(st, i))));
                } catch (NumberFormatException e) {
                    l.add(Double.valueOf(Double.NaN));
                }
            }
        }
        double[] d = new double[l.size()];
        for (int k = 0; k < d.length; k++) {
            d[k] = l.get(k).doubleValue();
        }
        return d;
    }
}
