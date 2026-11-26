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

import com.codename1.impl.office.ZipParts;
import com.codename1.impl.xml.XmlScanner;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.Table;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Reads a PowerPoint 2007 or later `.pptx` (Office Open XML PresentationML).
///
/// Masters and layouts have no place in the model, so everything a slide takes from them --
/// placeholder positions, text styles, background, the art drawn behind the slide -- is
/// resolved into the slide itself. Anything that cannot be converted is left out with a
/// warning on the presentation; a damaged shape loses that shape, never the file.
final class PptxReader {
    private static final String KIND = "PowerPoint presentation";
    private static final int MAX_SLIDES = 5000;
    private static final int MAX_RELS = 10000;
    private static final int MAX_GROUP_DEPTH = 32;
    private static final int MAX_WARNINGS = 200;
    private static final int MAX_SHAPES = 20000;
    private static final double EMU = 1 / 12700.0;

    /// One XML part with its relationships.
    static final class Part {
        final String name;
        final PptxNode root;
        final Map<String, String> targets = new HashMap<String, String>();
        final Map<String, String> externals = new HashMap<String, String>();
        final Map<String, String> types = new HashMap<String, String>();
        final List<String> order = new ArrayList<String>();

        Part(String name, PptxNode root) {
            this.name = name;
            this.root = root;
        }

        /// The part a relationship points at inside the package, or null.
        String target(String id) {
            return targets.get(id);
        }

        /// The target of an external relationship (a hyperlink), or null.
        String external(String id) {
            return externals.get(id);
        }

        /// The first internal target of a relationship type ending with `suffix`.
        String byType(String suffix) {
            for (String id : order) {
                String t = types.get(id);
                if (t != null && t.endsWith(suffix) && targets.get(id) != null) {
                    return targets.get(id);
                }
            }
            return null;
        }
    }

    /// What a slide's shapes are read against.
    private static final class Context {
        Part part;
        Part layout;
        Part master;
        boolean inherited;
        final Set<String> used = new HashSet<String>();
        final Map<String, String> spids = new HashMap<String, String>();
        int shapes;
    }

    private final Map<String, byte[]> parts;
    private final long[] budget = {XmlScanner.MAX_DOCUMENT};
    private final Map<String, Part> cache = new HashMap<String, Part>();
    private final Set<String> warnings = new LinkedHashSet<String>();
    private final List<String> raw = new ArrayList<String>();
    Theme theme = Theme.DEFAULT;
    private long seq;
    private long maxNumber;
    private float slideWidth = 960;
    private float slideHeight = 540;

    private PptxReader(Map<String, byte[]> parts) {
        this.parts = parts;
    }

    static Presentation read(InputStream in) throws IOException {
        return new PptxReader(ZipParts.read(in, KIND)).run();
    }

    void warn(String s) {
        if (warnings.size() < MAX_WARNINGS) {
            warnings.add(s);
        }
    }

    String nextId(char prefix) {
        seq++;
        return String.valueOf(prefix) + seq;
    }

    byte[] data(String name) {
        return parts.get(name);
    }

    private Presentation run() throws IOException {
        Part pkg = rels(new Part("", null));
        String main = pkg.byType("/officeDocument");
        if (main == null) {
            main = "ppt/presentation.xml";
        }
        Part pres = part(main);
        if (pres == null || !"presentation".equals(pres.root.name)) {
            throw new IOException("Not a " + KIND + ": it has no presentation part");
        }
        PptxNode size = pres.root.child("sldSz");
        if (size != null) {
            float w = PptxDrawing.pt(size.num("cx", 12192000));
            float h = PptxDrawing.pt(size.num("cy", 6858000));
            if (w > 0 && h > 0 && w <= 100000 && h <= 100000) {
                slideWidth = w;
                slideHeight = h;
            } else {
                warn("A slide size out of range was replaced by 16:9");
            }
        }
        String themePath = pres.byType("/theme");
        List<String> slidePaths = new ArrayList<String>();
        PptxNode list = pres.root.child("sldIdLst");
        if (list != null) {
            List<PptxNode> ids = list.all("sldId");
            for (PptxNode item : ids) {
                if (slidePaths.size() >= MAX_SLIDES) {
                    warn("Slides past " + MAX_SLIDES + " were left out");
                    break;
                }
                String t = pres.target(item.attr("r:id"));
                if (t == null) {
                    warn("A slide the presentation lists is missing");
                } else {
                    slidePaths.add(t);
                }
            }
        }
        if (themePath == null && !slidePaths.isEmpty()) {
            Part s = part(slidePaths.get(0));
            Part l = s == null ? null : related(s, "/slideLayout");
            Part m = l == null ? null : related(l, "/slideMaster");
            themePath = m == null ? null : m.byType("/theme");
        }
        Part th = themePath == null ? null : part(themePath);
        if (th != null) {
            theme = theme(th.root);
        }
        String title = null;
        String corePath = pkg.byType("/core-properties");
        Part core = part(corePath == null ? "docProps/core.xml" : corePath);
        if (core != null && core.root.child("title") != null) {
            String t = core.root.child("title").text().trim();
            title = t.length() == 0 ? null : t;
        }
        List<Slide> slides = new ArrayList<Slide>();
        for (String item : slidePaths) {
            try {
                Slide s = slide(item, slides.size() + 1);
                if (s != null) {
                    slides.add(s);
                }
            } catch (RuntimeException e) { //NOPMD AvoidCatchingGenericException - one damaged slide must not lose the file
                warn("A slide that could not be read was left out: " + e.getMessage());
            }
        }
        for (String item : raw) {
            warn(item);
        }
        long next = Math.max(seq, Math.max(maxNumber, slides.size())) + 1;
        return new Presentation(slideWidth, slideHeight, slides, theme, title, next,
                new ArrayList<String>(warnings));
    }

    private Part part(String name) {
        if (name == null) {
            return null;
        }
        if (cache.containsKey(name)) {
            return cache.get(name);
        }
        cache.put(name, null);
        byte[] data = parts.get(name);
        if (data == null) {
            return null;
        }
        PptxNode root;
        try {
            root = PptxNode.parse(data, budget, raw);
        } catch (IOException e) {
            warn("The damaged part " + name + " was left out");
            return null;
        }
        if (root == null) {
            warn("The empty part " + name + " was left out");
            return null;
        }
        Part p = rels(new Part(name, root));
        cache.put(name, p);
        return p;
    }

    private Part related(Part p, String type) {
        String t = p.byType(type);
        if (t == null || t.equals(p.name)) {
            return null;
        }
        return part(t);
    }

    private Part rels(Part p) {
        int slash = p.name.lastIndexOf('/');
        String relsName = p.name.substring(0, slash + 1) + "_rels/"
                + p.name.substring(slash + 1) + ".rels";
        byte[] data = parts.get(relsName);
        if (data == null) {
            return p;
        }
        PptxNode root;
        try {
            root = PptxNode.parse(data, budget, raw);
        } catch (IOException e) {
            warn("The damaged relationships " + relsName + " were ignored");
            return p;
        }
        if (root == null) {
            return p;
        }
        List<PptxNode> all = root.all("Relationship");
        for (int i = 0; i < all.size() && i < MAX_RELS; i++) {
            PptxNode r = all.get(i);
            String id = r.attr("Id");
            String target = r.attr("Target");
            if (id == null || target == null) {
                continue;
            }
            p.order.add(id);
            p.types.put(id, r.attr("Type") == null ? "" : r.attr("Type"));
            if ("External".equals(r.attr("TargetMode"))) {
                p.externals.put(id, target);
            } else if (escapes(p.name, target)) {
                warn("A relationship pointing outside the file was ignored");
            } else {
                p.targets.put(id, ZipParts.resolve(p.name, target));
            }
        }
        return p;
    }

    private static boolean escapes(String base, String target) {
        if (target.startsWith("/")) {
            return target.indexOf("/../") >= 0 || target.endsWith("/..");
        }
        int depth = 0;
        for (int i = 0; i < base.length(); i++) {
            if (base.charAt(i) == '/') {
                depth++;
            }
        }
        int start = 0;
        for (int i = 0; i <= target.length(); i++) {
            if (i == target.length() || target.charAt(i) == '/') {
                String seg = target.substring(start, i);
                start = i + 1;
                if ("..".equals(seg)) {
                    depth--;
                    if (depth < 0) {
                        return true;
                    }
                } else if (seg.length() > 0 && !".".equals(seg) && i < target.length()) {
                    depth++;
                }
            }
        }
        return false;
    }

    private Theme theme(PptxNode root) {
        PptxNode el = root.child("themeElements");
        if (el == null) {
            return Theme.DEFAULT;
        }
        int[] colors = new int[12];
        for (int i = 0; i < 12; i++) {
            colors[i] = Theme.DEFAULT.color(i);
        }
        PptxNode scheme = el.child("clrScheme");
        if (scheme != null) {
            for (int i = 0; i < Theme.NAMES.length; i++) {
                PptxNode c = scheme.child(Theme.NAMES[i]);
                long v = PptxDrawing.color(c, this, PptxDrawing.NONE);
                if (v != PptxDrawing.NONE) {
                    colors[i] = PptxDrawing.rgb(v);
                }
            }
        }
        String major = null;
        String minor = null;
        PptxNode fonts = el.child("fontScheme");
        if (fonts != null) {
            PptxNode mj = fonts.path("majorFont", "latin");
            PptxNode mn = fonts.path("minorFont", "latin");
            major = mj == null ? null : mj.attr("typeface");
            minor = mn == null ? null : mn.attr("typeface");
        }
        return new Theme(root.attr("name"), colors, major, minor);
    }

    private Slide slide(String path, int n) {
        Part sp = part(path);
        if (sp == null || !"sld".equals(sp.root.name)) {
            warn("A slide part that is missing or not a slide was left out");
            return null;
        }
        Context c = new Context();
        c.part = sp;
        c.layout = related(sp, "/slideLayout");
        if (c.layout == null) {
            warn("A slide whose layout is missing was read without one");
        } else {
            c.master = related(c.layout, "/slideMaster");
            if (c.master == null) {
                warn("A layout whose master is missing was read without one");
            }
        }
        PptxNode cSld = sp.root.child("cSld");
        List<SlideElement> own = cSld == null || cSld.child("spTree") == null
                ? new ArrayList<SlideElement>()
                : shapes(cSld.child("spTree"), c, identity(), 0);
        List<SlideElement> elements = new ArrayList<SlideElement>();
        if (!"0".equals(sp.root.attr("showMasterSp"))) {
            Part keep = c.part;
            c.inherited = true;
            if (c.master != null && c.layout != null
                    && !"0".equals(c.layout.root.attr("showMasterSp"))) {
                c.part = c.master;
                elements.addAll(shapes(c.master.root.path("cSld", "spTree"), c, identity(), 0));
            }
            if (c.layout != null) {
                c.part = c.layout;
                elements.addAll(shapes(c.layout.root.path("cSld", "spTree"), c, identity(), 0));
            }
            c.part = keep;
            c.inherited = false;
        }
        elements.addAll(own);
        Fill bg = background(sp);
        if (bg == null && c.layout != null) {
            bg = background(c.layout);
        }
        if (bg == null && c.master != null) {
            bg = background(c.master);
        }
        String layoutName = c.layout == null || c.layout.root.child("cSld") == null ? null
                : c.layout.root.child("cSld").attr("name");
        return new Slide("s" + n, layoutName, bg, elements, notes(sp), transition(sp.root),
                builds(sp.root, c), "0".equals(sp.root.attr("show")));
    }

    private static double[] identity() {
        return new double[] {EMU, 0, EMU, 0};
    }

    private List<SlideElement> shapes(PptxNode tree, Context c, double[] xf, int depth) {
        List<SlideElement> out = new ArrayList<SlideElement>();
        if (tree == null) {
            return out;
        }
        for (PptxNode s : tree.children) {
            if (++c.shapes > MAX_SHAPES) { //NOPMD AssignmentInOperand - the count bounds a hostile slide
                warn("Shapes past " + MAX_SHAPES + " on a slide were left out");
                return out;
            }
            if ("AlternateContent".equals(s.name)) {
                PptxNode pick = s.child("Fallback");
                if (pick == null) {
                    pick = s.child("Choice");
                }
                out.addAll(shapes(pick, c, xf, depth + 1));
                continue;
            }
            try {
                SlideElement e = shape(s, c, xf, depth);
                if (e != null) {
                    out.add(e);
                }
            } catch (RuntimeException e) { //NOPMD AvoidCatchingGenericException - one damaged shape must not lose the slide
                warn("A shape that could not be read was left out: " + e.getMessage());
            }
        }
        return out;
    }

    private SlideElement shape(PptxNode s, Context c, double[] xf, int depth) {
        String n = s.name;
        if ("sp".equals(n) || "cxnSp".equals(n)) {
            return sp(s, c, xf);
        }
        if ("pic".equals(n)) {
            return pic(s, c, xf);
        }
        if ("graphicFrame".equals(n)) {
            return frame(s, c, xf);
        }
        if ("grpSp".equals(n)) {
            return group(s, c, xf, depth);
        }
        if ("nvGrpSpPr".equals(n) || "grpSpPr".equals(n) || "extLst".equals(n)) {
            return null;
        }
        warn("Slide content of kind " + n + " was left out");
        return null;
    }

    private String id(PptxNode cNvPr, Context c) {
        String rawId = cNvPr == null ? null : cNvPr.attr("id");
        String base;
        if (rawId != null && rawId.length() > 0 && rawId.length() < 10 && digits(rawId)) {
            base = "e" + rawId;
            maxNumber = Math.max(maxNumber, Long.parseLong(rawId));
        } else {
            base = nextId('e');
        }
        String id = base;
        for (int k = 2; c.used.contains(id); k++) {
            id = base + "_" + k;
        }
        c.used.add(id);
        if (!c.inherited && rawId != null) {
            c.spids.put(rawId, id);
        }
        return id;
    }

    private static boolean digits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    private static String name(PptxNode cNvPr) {
        return cNvPr == null ? null : cNvPr.attr("name");
    }

    private static PptxNode nv(PptxNode s) {
        for (PptxNode c : s.children) {
            if (c.name.startsWith("nv") && c.name.endsWith("Pr")) {
                return c;
            }
        }
        return null;
    }

    /// x, y, w, h, rot, flipH, flipV of an `xfrm`, or null.
    private static float[] place(PptxNode xfrm, double[] xf) {
        if (xfrm == null) {
            return null;
        }
        PptxNode off = xfrm.child("off");
        PptxNode ext = xfrm.child("ext");
        if (off == null || ext == null) {
            return null;
        }
        float rot = (float) (xfrm.num("rot", 0) / 60000.0);
        return new float[] {(float) (xf[0] * off.num("x", 0) + xf[1]),
            (float) (xf[2] * off.num("y", 0) + xf[3]),
            (float) Math.max(0, xf[0] * ext.num("cx", 0)),
            (float) Math.max(0, xf[2] * ext.num("cy", 0)), rot,
            xfrm.flag("flipH", false) ? 1 : 0, xfrm.flag("flipV", false) ? 1 : 0};
    }

    private static String normalType(String t, boolean master) {
        if (t == null || "obj".equals(t)) {
            return "body";
        }
        if ("ctrTitle".equals(t)) {
            return "title";
        }
        if (master && "subTitle".equals(t)) {
            return "body";
        }
        return t;
    }

    /// The placeholder shape in a layout or master that `ph` inherits from.
    private static PptxNode findPh(Part p, PptxNode ph, boolean byIdx) {
        if (p == null || ph == null) {
            return null;
        }
        PptxNode tree = p.root.path("cSld", "spTree");
        if (tree == null) {
            return null;
        }
        String idx = ph.attr("idx");
        String type = ph.attr("type");
        PptxNode loose = null;
        for (int pass = 0; pass < 3; pass++) {
            for (PptxNode s : tree.children) {
                PptxNode nv = nv(s);
                PptxNode other = nv == null ? null : nv.path("nvPr", "ph");
                if (other == null) {
                    continue;
                }
                if (pass == 0) {
                    if (byIdx && idx != null && idx.equals(other.attr("idx"))) {
                        return s;
                    }
                } else if (pass == 1) {
                    if (normalType(type, false).equals(normalType(other.attr("type"), false))) {
                        return s;
                    }
                } else if (loose == null && normalType(type, true)
                        .equals(normalType(other.attr("type"), true))) {
                    loose = s;
                }
            }
        }
        return loose;
    }

    private static PptxNode phOf(PptxNode shape) {
        PptxNode nv = shape == null ? null : nv(shape);
        return nv == null ? null : nv.path("nvPr", "ph");
    }

    private static String placeholderName(PptxNode ph) {
        if (ph == null) {
            return null;
        }
        String t = normalType(ph.attr("type"), false);
        if ("title".equals(t)) {
            return ShapeElement.TITLE;
        }
        if ("subTitle".equals(t)) {
            return ShapeElement.SUBTITLE;
        }
        return "body".equals(t) ? ShapeElement.BODY : t;
    }

    private SlideElement sp(PptxNode s, Context c, double[] xf) {
        PptxNode nv = nv(s);
        PptxNode cNvPr = nv == null ? null : nv.child("cNvPr");
        PptxNode ph = nv == null ? null : nv.path("nvPr", "ph");
        if (c.inherited && ph != null) {
            return null;
        }
        PptxNode layoutPh = null;
        PptxNode masterPh = null;
        if (ph != null) {
            layoutPh = findPh(c.layout, ph, true);
            PptxNode lp = phOf(layoutPh);
            masterPh = findPh(c.master, lp == null ? ph : lp, false);
        }
        PptxNode[] chain = {masterPh, layoutPh, s};
        PptxNode spPr = s.child("spPr");
        float[] at = null;
        for (int i = 2; i >= 0 && at == null; i--) {
            PptxNode src = chain[i];
            at = src == null ? null : place(src.path("spPr", "xfrm"), xf);
        }
        if (at == null) {
            warn("A shape with no position was left out");
            return null;
        }
        String geometry = "rect";
        float[] adjust = null;
        for (int i = 0; i < 3; i++) {
            PptxNode props = chain[i] == null ? null : chain[i].child("spPr");
            PptxNode prst = props == null ? null : props.child("prstGeom");
            if (prst != null && prst.attr("prst") != null) {
                geometry = prst.attr("prst");
                adjust = adjust(prst.child("avLst"));
            } else if (props != null && props.child("custGeom") != null) {
                geometry = "rect";
                adjust = null;
                warn("A freeform shape was read as a rectangle");
            }
        }
        PptxNode style = s.child("style");
        long fillRef = PptxDrawing.color(style == null ? null : style.child("fillRef"), this,
                PptxDrawing.NONE);
        long lnRef = PptxDrawing.color(style == null ? null : style.child("lnRef"), this,
                PptxDrawing.NONE);
        Fill fill = null;
        for (int i = 2; i >= 0 && fill == null; i--) {
            PptxNode props = chain[i] == null ? null : chain[i].child("spPr");
            fill = PptxDrawing.fill(props, this, i == 2 ? c.part : i == 1 ? c.layout : c.master,
                    fillRef, at[2], at[3]);
        }
        if (fill == null && fillRef != PptxDrawing.NONE && style != null
                && style.child("fillRef").num("idx", 0) > 0) {
            fill = Fill.solid(PptxDrawing.rgb(fillRef)).withAlpha(PptxDrawing.alpha(fillRef));
        }
        LineStyle line = null;
        if (lnRef != PptxDrawing.NONE && style != null
                && style.child("lnRef").num("idx", 0) > 0) {
            line = new LineStyle(PptxDrawing.rgb(lnRef), 0.75f);
        }
        for (int i = 0; i < 3; i++) {
            PptxNode props = chain[i] == null ? null : chain[i].child("spPr");
            line = PptxDrawing.line(props == null ? null : props.child("ln"), this, lnRef, line);
        }
        PptxDrawing.Cascade cascade = new PptxDrawing.Cascade();
        String type = ph == null ? null : normalType(phOf(layoutPh) != null
                ? phOf(layoutPh).attr("type") : ph.attr("type"), false);
        cascade.title = "title".equals(type);
        PptxNode txStyles = c.master == null ? null : c.master.root.child("txStyles");
        if (txStyles != null) {
            String which = ph == null ? "otherStyle" : cascade.title ? "titleStyle"
                    : "body".equals(type) || "subTitle".equals(type) ? "bodyStyle" : "otherStyle";
            if (txStyles.child(which) != null) {
                cascade.lists.add(txStyles.child(which));
            }
        }
        for (int i = 0; i < 2; i++) {
            PptxNode body = chain[i] == null ? null : chain[i].child("txBody");
            if (body != null) {
                if (body.child("lstStyle") != null) {
                    cascade.lists.add(body.child("lstStyle"));
                }
                if (body.child("bodyPr") != null) {
                    cascade.bodies.add(body.child("bodyPr"));
                }
            }
        }
        cascade.fontColor = PptxDrawing.color(style == null ? null : style.child("fontRef"),
                this, PptxDrawing.NONE);
        TextFrame text = PptxDrawing.text(s.child("txBody"), cascade, this, c.part);
        String geom = geometry;
        if ("cxnSp".equals(s.name) && "straightConnector1".equals(geom)) {
            geom = "line";
        }
        if (spPr == null && ph == null) {
            warn("A shape with no properties was read with defaults");
        }
        return new ShapeElement(id(cNvPr, c), name(cNvPr), geom, at[0], at[1], at[2], at[3],
                at[4], at[5] > 0, at[6] > 0, fill, line, text, placeholderName(ph), adjust);
    }

    private static float[] adjust(PptxNode avLst) {
        if (avLst == null) {
            return null;
        }
        List<PptxNode> gds = avLst.all("gd");
        int n = Math.min(16, gds.size());
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            String f = gds.get(i).attr("fmla");
            double v = 0;
            if (f != null && f.startsWith("val ")) {
                v = PptxNode.number(f.substring(4), 0) / 100000.0;
            }
            out[i] = (float) Math.max(-100, Math.min(100, v));
        }
        return out;
    }

    private SlideElement pic(PptxNode s, Context c, double[] xf) {
        PptxNode nv = nv(s);
        PptxNode cNvPr = nv == null ? null : nv.child("cNvPr");
        PptxNode ph = nv == null ? null : nv.path("nvPr", "ph");
        if (c.inherited && ph != null) {
            return null;
        }
        float[] at = place(s.path("spPr", "xfrm"), xf);
        if (at == null && ph != null) {
            PptxNode lph = findPh(c.layout, ph, true);
            at = lph == null ? null : place(lph.path("spPr", "xfrm"), xf);
            if (at == null) {
                PptxNode mph = findPh(c.master, ph, false);
                at = mph == null ? null : place(mph.path("spPr", "xfrm"), xf);
            }
        }
        if (at == null) {
            warn("A picture with no position was left out");
            return null;
        }
        if (nv != null && nv.child("nvPr") != null && (nv.child("nvPr").child("videoFile") != null
                || nv.child("nvPr").child("audioFile") != null)) {
            warn("Video and audio were read as their poster pictures");
        }
        PptxNode blipFill = s.child("blipFill");
        Picture p = blipFill == null ? null : PptxDrawing.picture(blipFill, this, c.part, at[2],
                at[3], cNvPr == null ? null : cNvPr.attr("descr"));
        if (p == null) {
            return null;
        }
        PptxNode src = blipFill.child("srcRect");
        float[] crop = new float[4];
        if (src != null) {
            String[] k = {"l", "t", "r", "b"};
            for (int i = 0; i < 4; i++) {
                crop[i] = (float) Math.max(0, Math.min(0.99, src.num(k[i], 0) / 100000.0));
            }
        }
        LineStyle line = PptxDrawing.line(s.path("spPr", "ln"), this, PptxDrawing.NONE, null);
        return new PictureElement(id(cNvPr, c), name(cNvPr), p, at[0], at[1], at[2], at[3],
                at[4], at[5] > 0, at[6] > 0, crop[0], crop[1], crop[2], crop[3], line);
    }

    private SlideElement frame(PptxNode s, Context c, double[] xf) {
        PptxNode nv = nv(s);
        PptxNode cNvPr = nv == null ? null : nv.child("cNvPr");
        float[] at = place(s.child("xfrm"), xf);
        if (at == null) {
            warn("A table or chart with no position was left out");
            return null;
        }
        PptxNode data = s.path("graphic", "graphicData");
        if (data == null) {
            warn("An empty graphic frame was left out");
            return null;
        }
        PptxNode tbl = data.child("tbl");
        if (tbl != null) {
            Table t = PptxChartReader.table(tbl, this, c.part, new PptxDrawing.Cascade());
            return t == null ? null : new TableElement(id(cNvPr, c), name(cNvPr), t, at[0],
                    at[1], at[2], at[3], at[4]);
        }
        PptxNode chart = data.child("chart");
        if (chart != null) {
            String target = c.part.target(chart.attr("r:id"));
            Part cp = target == null ? null : part(target);
            if (cp == null) {
                warn("A chart whose data is missing was left out");
                return null;
            }
            return PptxChartReader.chart(cp.root, id(cNvPr, c), name(cNvPr), at[0], at[1], at[2],
                    at[3], this);
        }
        if (data.child("relIds") != null) {
            warn("A SmartArt diagram was left out");
        } else if (data.child("oleObj") != null || data.find("oleObj") != null) {
            warn("An embedded object was left out");
        } else {
            warn("A graphic frame of an unknown kind was left out");
        }
        return null;
    }

    private SlideElement group(PptxNode s, Context c, double[] xf, int depth) {
        if (depth >= MAX_GROUP_DEPTH) {
            warn("Groups nested deeper than " + MAX_GROUP_DEPTH + " were left out");
            return null;
        }
        PptxNode nv = nv(s);
        PptxNode cNvPr = nv == null ? null : nv.child("cNvPr");
        PptxNode xfrm = s.path("grpSpPr", "xfrm");
        double[] inner = xf;
        float[] at = place(xfrm, xf);
        if (xfrm != null && at != null) {
            PptxNode off = xfrm.child("off");
            PptxNode ext = xfrm.child("ext");
            PptxNode chOff = xfrm.child("chOff");
            PptxNode chExt = xfrm.child("chExt");
            double sx = 1;
            double sy = 1;
            double cx = 0;
            double cy = 0;
            if (chOff != null && chExt != null) {
                double ccx = chExt.num("cx", 0);
                double ccy = chExt.num("cy", 0);
                sx = ccx > 0 ? ext.num("cx", 0) / ccx : 1;
                sy = ccy > 0 ? ext.num("cy", 0) / ccy : 1;
                cx = chOff.num("x", 0);
                cy = chOff.num("y", 0);
            }
            inner = new double[] {xf[0] * sx, xf[0] * (off.num("x", 0) - cx * sx) + xf[1],
                xf[2] * sy, xf[2] * (off.num("y", 0) - cy * sy) + xf[3]};
        }
        String id = id(cNvPr, c);
        List<SlideElement> kids = shapes(s, c, inner, depth + 1);
        if (kids.isEmpty()) {
            return null;
        }
        if (at == null) {
            GroupElement g = GroupElement.of(id, kids);
            return new GroupElement(id, name(cNvPr), kids, g.x, g.y, g.width, g.height, 0,
                    false, false);
        }
        return new GroupElement(id, name(cNvPr), kids, at[0], at[1], at[2], at[3], at[4],
                at[5] > 0, at[6] > 0);
    }

    private Fill background(Part p) {
        PptxNode bg = p.root.path("cSld", "bg");
        if (bg == null) {
            return null;
        }
        PptxNode pr = bg.child("bgPr");
        if (pr != null) {
            Fill f = PptxDrawing.fill(pr, this, p, PptxDrawing.NONE, slideWidth, slideHeight);
            if (f != null) {
                return f;
            }
        }
        PptxNode ref = bg.child("bgRef");
        if (ref != null) {
            long c = PptxDrawing.color(ref, this, PptxDrawing.NONE);
            if (c != PptxDrawing.NONE) {
                if (ref.num("idx", 0) > 1001) {
                    warn("A theme background was read as its main color");
                }
                return Fill.solid(PptxDrawing.rgb(c));
            }
        }
        return null;
    }

    private List<Block> notes(Part sp) {
        Part np = related(sp, "/notesSlide");
        if (np == null) {
            return null;
        }
        PptxNode tree = np.root.path("cSld", "spTree");
        if (tree == null) {
            return null;
        }
        List<PptxNode> shapes = tree.all("sp");
        for (PptxNode item : shapes) {
            PptxNode ph = phOf(item);
            if (ph != null && "body".equals(ph.attr("type"))) {
                return PptxDrawing.blocks(item.child("txBody"),
                        new PptxDrawing.Cascade(), this, np);
            }
        }
        return null;
    }

    private Transition transition(PptxNode sld) {
        PptxNode t = sld.child("transition");
        PptxNode alt = sld.child("AlternateContent");
        if (t == null && alt != null) {
            PptxNode choice = alt.child("Choice");
            PptxNode ct = choice == null ? null : choice.child("transition");
            if (ct != null && kind(ct) != null) {
                t = ct;
            } else {
                PptxNode fb = alt.child("Fallback");
                t = fb == null ? ct : fb.child("transition");
            }
        }
        if (t == null) {
            return null;
        }
        String spd = t.attr("spd");
        int duration = "slow".equals(spd) ? 1000 : "med".equals(spd) ? 750 : 500;
        if (t.attr("dur") != null) {
            duration = (int) Math.max(0, Math.min(60000, t.num("dur", duration)));
        }
        boolean click = t.flag("advClick", true);
        int after = t.attr("advTm") == null ? -1
                : (int) Math.max(0, Math.min(Integer.MAX_VALUE, t.num("advTm", 0)));
        String type = Transition.NONE;
        String dir = Transition.FROM_RIGHT;
        PptxNode effect = null;
        for (PptxNode e : t.children) {
            if (!"sndAc".equals(e.name) && !"extLst".equals(e.name)) {
                effect = e;
                break;
            }
        }
        if (effect != null) {
            String k = kind(t);
            if (k == null) {
                warn("A slide transition of kind " + effect.name + " was read as a fade");
                type = Transition.FADE;
            } else {
                type = k;
            }
            String d = effect.attr("dir");
            // OOXML names the way the slide moves, the model where it comes from: dir="l"
            // slides toward the left, in from the right, which is the default
            if ("r".equals(d)) {
                dir = Transition.FROM_LEFT;
            } else if ("u".equals(d)) {
                dir = Transition.FROM_BOTTOM;
            } else if ("d".equals(d)) {
                dir = Transition.FROM_TOP;
            }
        } else if (after < 0 && click) {
            return null;
        }
        return new Transition(type, duration, dir, click, after);
    }

    private static String kind(PptxNode t) {
        for (int i = 0; i < t.children.size(); i++) {
            String n = t.children.get(i).name;
            if ("fade".equals(n) || "dissolve".equals(n)) {
                return Transition.FADE;
            }
            if ("push".equals(n)) {
                return Transition.PUSH;
            }
            if ("cover".equals(n)) {
                return Transition.COVER;
            }
            if ("pull".equals(n)) {
                return Transition.UNCOVER;
            }
            if ("wipe".equals(n)) {
                return Transition.WIPE;
            }
            if ("zoom".equals(n)) {
                return Transition.ZOOM;
            }
        }
        return null;
    }

    private List<Build> builds(PptxNode sld, Context c) {
        PptxNode timing = sld.child("timing");
        List<Build> out = new ArrayList<Build>();
        if (timing != null) {
            collect(timing, c, out, 0);
        }
        return out;
    }

    private void collect(PptxNode at, Context c, List<Build> out, int depth) {
        if (depth > 64 || out.size() >= 1000) {
            return;
        }
        if ("cTn".equals(at.name) && at.attr("presetClass") != null) {
            Build b = build(at, c);
            if (b != null) {
                out.add(b);
            }
            return;
        }
        for (int i = 0; i < at.children.size(); i++) {
            collect(at.children.get(i), c, out, depth + 1);
        }
    }

    private Build build(PptxNode ctn, Context c) {
        String cls = ctn.attr("presetClass");
        String kind;
        if ("entr".equals(cls)) {
            kind = Build.ENTRANCE;
        } else if ("exit".equals(cls)) {
            kind = Build.EXIT;
        } else if ("emph".equals(cls)) {
            kind = Build.EMPHASIS;
        } else {
            warn("An animation of class " + cls + " was left out");
            return null;
        }
        PptxNode tgt = ctn.find("spTgt");
        String elementId = tgt == null ? null : c.spids.get(tgt.attr("spid"));
        if (elementId == null) {
            warn("An animation of a shape that is not on its slide was left out");
            return null;
        }
        int preset = (int) ctn.num("presetID", 1);
        String effect;
        if (preset == 1) {
            effect = Build.APPEAR;
        } else if (preset == 10) {
            effect = Build.FADE;
        } else if (preset == 2) {
            effect = Build.FLY;
        } else if (preset == 53 || preset == 23) {
            effect = Build.ZOOM;
        } else if (preset == 22) {
            effect = Build.WIPE;
        } else {
            warn("An animation effect PowerPoint numbers " + preset + " was read as a fade");
            effect = Build.FADE;
        }
        String node = ctn.attr("nodeType");
        String trigger = "withEffect".equals(node) ? Build.WITH_PREVIOUS
                : "afterEffect".equals(node) ? Build.AFTER_PREVIOUS : Build.ON_CLICK;
        int duration = 500;
        PptxNode inner = ctn.find("cTn");
        if (inner != null && inner.attr("dur") != null) {
            duration = (int) Math.max(0, Math.min(60000, inner.num("dur", 500)));
        }
        int delay = 0;
        PptxNode cond = ctn.path("stCondLst", "cond");
        if (cond != null) {
            delay = (int) Math.max(0, Math.min(60000, cond.num("delay", 0)));
        }
        int sub = (int) ctn.num("presetSubtype", 0);
        String dir = sub == 8 ? Transition.FROM_LEFT : sub == 2 ? Transition.FROM_RIGHT
                : sub == 1 ? Transition.FROM_TOP : Transition.FROM_BOTTOM;
        return new Build(elementId, kind, effect, trigger, duration, delay, dir);
    }
}
