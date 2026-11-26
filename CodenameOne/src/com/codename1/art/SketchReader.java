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
package com.codename1.art;

import com.codename1.art.effects.Effect;
import com.codename1.art.effects.Effects;
import com.codename1.io.JSONParser;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.util.MathUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Reads Sketch files (Sketch 43 and later: a ZIP of JSON) into an `ArtDocument`.
///
/// Every page becomes a group under the root; the first page is visible and the document is
/// sized to its content. Artboards become clipping frames, groups and shape groups keep their
/// structure, symbol instances are expanded from their masters with text, image and nested
/// symbol overrides applied, and fills, borders, shadows, inner shadows, the four blurs,
/// blend modes, opacity, rotation, flips and clipping masks are read and rendered.
public final class SketchReader {
    /// The deepest symbol nesting expanded.
    public static final int MAX_SYMBOL_DEPTH = 32;
    /// The most layers read.
    public static final int MAX_LAYERS = 200000;

    private Map<String, byte[]> parts;
    private final Map<String, Map<String, Object>> symbols = new HashMap<String, Map<String, Object>>();
    private final Map<String, Raster> images = new HashMap<String, Raster>();
    private final List<ImportNote> notes = new ArrayList<ImportNote>();
    private int layers;
    private int depth;

    private SketchReader() {
    }

    /// Reads a Sketch file.
    ///
    /// #### Parameters
    ///
    /// - `in`: the file; read to its end, not closed
    public static ArtDocument read(InputStream in) throws IOException {
        try {
            return new SketchReader().parse(com.codename1.impl.office.ZipParts.read(in, "Sketch file", ArtFormats.MAX_PACKAGE_BYTES));
        } catch (RuntimeException e) {
            throw ArtFormats.damaged(e);
        }
    }

    private void note(String where, String msg) {
        for (ImportNote n : notes) {
            if (n.getWhere().equals(where) && n.getMessage().equals(msg)) {
                return;
            }
        }
        if (notes.size() < 500) {
            notes.add(new ImportNote(where, msg));
        }
    }

    private Map<String, Object> json(String name) throws IOException {
        byte[] b = parts.get(name);
        if (b == null) {
            return null;
        }
        return JSONParser.parseJSON(b);
    }

    private ArtDocument parse(Map<String, byte[]> p) throws IOException {
        parts = p;
        Map<String, Object> document = json("document.json");
        if (document == null) {
            throw new IOException("Not a Sketch file: document.json is missing");
        }
        List<Map<String, Object>> pages = new ArrayList<Map<String, Object>>();
        List<Object> refs = Json.list(document, "pages");
        if (refs != null) {
            for (int i = 0; i < refs.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                String ref = Json.str(refs.get(i), "_ref");
                Map<String, Object> page = ref == null ? null : json(ref.endsWith(".json") ? ref : ref + ".json");
                if (page != null) {
                    pages.add(page);
                }
            }
        }
        List<Object> foreign = Json.list(document, "foreignSymbols");
        if (foreign != null) {
            for (int i = 0; i < foreign.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                collectSymbols(Json.obj(foreign.get(i), "symbolMaster"));
            }
        }
        for (int i = 0; i < pages.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            collectSymbols(pages.get(i));
        }
        GroupNode root = new GroupNode();
        double[] box = null;
        for (int i = 0; i < pages.size(); i++) {
            Map<String, Object> page = pages.get(i);
            GroupNode g = new GroupNode();
            g.setName(Json.str(page, "name"));
            g.setId(Json.str(page, "do_objectID"));
            children(page, g);
            g.setVisible(i == 0);
            if (i == 0) {
                box = g.getBounds();
            }
            root.add(g);
        }
        if (box == null || box[2] <= 0 || box[3] <= 0) {
            box = new double[]{0, 0, 100, 100};
        }
        Matrix shift = Matrix.translate(-box[0], -box[1]);
        for (int i = 0; i < root.getChildren().size(); i++) {
            root.getChildren().get(i).setTransform(shift);
        }
        ArtDocument doc = new ArtDocument(Math.ceil(box[2]), Math.ceil(box[3]));
        doc.setRoot(root);
        doc.getNotes().addAll(notes);
        return doc;
    }

    private void collectSymbols(Map<String, Object> layer) {
        if (layer == null) {
            return;
        }
        if ("symbolMaster".equals(Json.str(layer, "_class"))) {
            String id = Json.str(layer, "symbolID");
            if (id != null) {
                symbols.put(id, layer);
            }
        }
        List<Object> ls = Json.list(layer, "layers");
        if (ls != null) {
            for (int i = 0; i < ls.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                collectSymbols(Json.asObj(ls.get(i)));
            }
        }
    }

    private void children(Map<String, Object> parent, GroupNode into) throws IOException {
        List<Object> ls = Json.list(parent, "layers");
        if (ls == null) {
            return;
        }
        if (nesting >= MAX_NESTING) {
            throw new IOException("Sketch layers nest deeper than " + MAX_NESTING);
        }
        nesting++;
        try {
            childLayers(ls, into);
        } finally {
            nesting--;
        }
    }

    /// How deep groups may nest before a file is refused.
    public static final int MAX_NESTING = 256;

    private int nesting;

    private void childLayers(List<Object> ls, GroupNode into) throws IOException {
        GroupNode clipGroup = null;
        for (int i = 0; i < ls.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            Map<String, Object> l = Json.asObj(ls.get(i));
            if (l == null) {
                continue;
            }
            ArtNode n = layer(l);
            if (n == null) {
                continue;
            }
            if (clipGroup != null && Json.bool(l, "shouldBreakMaskChain", false)) {
                clipGroup = null;
            }
            if (Json.bool(l, "hasClippingMask", false)) {
                // A mask layer is drawn, and clips every sibling above it until the chain breaks.
                into.add(n);
                clipGroup = new GroupNode();
                clipGroup.setPassThrough(true);
                ArtNode m = n.copy();
                m.setOpacity(1);
                m.setBlendMode(BlendMode.NORMAL);
                m.getEffects().clear();
                m.setVisible(true);
                clipGroup.setMask(m, false);
                into.add(clipGroup);
                continue;
            }
            if (clipGroup != null) {
                clipGroup.add(n);
            } else {
                into.add(n);
            }
        }
    }

    private static double[] frame(Map<String, Object> l) {
        Map<String, Object> f = Json.obj(l, "frame");
        return new double[]{Json.num(f, "x", 0), Json.num(f, "y", 0), Json.num(f, "width", 0),
            Json.num(f, "height", 0)};
    }

    /// Position, then rotation and flips about the center of the frame.
    private static Matrix placement(Map<String, Object> l, double[] f) {
        Matrix m = Matrix.translate(f[0], f[1]);
        double rot = Json.num(l, "rotation", 0);
        boolean fh = Json.bool(l, "isFlippedHorizontal", false);
        boolean fv = Json.bool(l, "isFlippedVertical", false);
        if (rot != 0 || fh || fv) {
            m = m.multiply(Matrix.translate(f[2] / 2, f[3] / 2));
            if (rot != 0) {
                // Sketch measures rotation counterclockwise.
                m = m.multiply(Matrix.rotate(-Math.toRadians(rot)));
            }
            if (fh || fv) {
                m = m.multiply(Matrix.scale(fh ? -1 : 1, fv ? -1 : 1));
            }
            m = m.multiply(Matrix.translate(-f[2] / 2, -f[3] / 2));
        }
        return m;
    }

    private ArtNode layer(Map<String, Object> l) throws IOException {
        layers++;
        if (layers > MAX_LAYERS) {
            throw new IOException("Sketch file has too many layers");
        }
        String cls = Json.str(l, "_class");
        if (cls == null) {
            return null;
        }
        double[] f = frame(l);
        String name = Json.str(l, "name");
        ArtNode n;
        if ("group".equals(cls) || "page".equals(cls)) {
            GroupNode g = new GroupNode();
            children(l, g);
            g.setPassThrough(true);
            n = g;
        } else if ("artboard".equals(cls) || "symbolMaster".equals(cls)) {
            if ("symbolMaster".equals(cls) && !Json.bool(l, "includeInCloudUpload", true)) {
                return null;
            }
            GroupNode g = new GroupNode();
            g.setFrame(0, 0, f[2], f[3], true);
            if (Json.bool(l, "hasBackgroundColor", false)) {
                ShapeNode bg = ShapeNode.rect(0, 0, f[2], f[3], 0);
                bg.fill(new SolidPaint(color(Json.obj(l, "backgroundColor"))));
                bg.setName("Background");
                g.add(bg);
            }
            children(l, g);
            n = g;
        } else if ("shapeGroup".equals(cls)) {
            n = shapeGroup(l);
        } else if ("rectangle".equals(cls) || "oval".equals(cls) || "shapePath".equals(cls)
                || "triangle".equals(cls) || "star".equals(cls) || "polygon".equals(cls)) {
            ShapeNode s = new ShapeNode(shapePath(l, f));
            style(l, s);
            n = s;
        } else if ("text".equals(cls)) {
            n = text(l, f);
        } else if ("bitmap".equals(cls)) {
            n = bitmap(l, f);
        } else if ("symbolInstance".equals(cls)) {
            n = instance(l, f);
        } else if ("slice".equals(cls) || "MSImmutableHotspotLayer".equals(cls) || "hotspot".equals(cls)) {
            return null;
        } else {
            note(name == null ? cls : name, "unknown Sketch layer class " + cls);
            return null;
        }
        if (n == null) {
            return null;
        }
        n.setName(name);
        n.setId(Json.str(l, "do_objectID"));
        n.setVisible(Json.bool(l, "isVisible", true));
        n.setLocked(Json.bool(l, "isLocked", false));
        n.setTransform(placement(l, f));
        Map<String, Object> st = Json.obj(l, "style");
        Map<String, Object> ctx = Json.obj(st, "contextSettings");
        if (ctx != null) {
            n.setOpacity((float) Json.num(ctx, "opacity", 1));
            n.setBlendMode(blend((int) Json.num(ctx, "blendMode", 0)));
        }
        effects(st, n);
        return n;
    }

    static BlendMode blend(int v) {
        BlendMode[] m = {BlendMode.NORMAL, BlendMode.DARKEN, BlendMode.MULTIPLY, BlendMode.COLOR_BURN,
            BlendMode.LIGHTEN, BlendMode.SCREEN, BlendMode.COLOR_DODGE, BlendMode.OVERLAY, BlendMode.SOFT_LIGHT,
            BlendMode.HARD_LIGHT, BlendMode.DIFFERENCE, BlendMode.EXCLUSION, BlendMode.HUE, BlendMode.SATURATION,
            BlendMode.COLOR, BlendMode.LUMINOSITY, BlendMode.PLUS_DARKER, BlendMode.PLUS_LIGHTER};
        return v >= 0 && v < m.length ? m[v] : BlendMode.NORMAL;
    }

    static int color(Map<String, Object> c) {
        if (c == null) {
            return 0xff000000;
        }
        int a = clamp(Json.num(c, "alpha", 1));
        int r = clamp(Json.num(c, "red", 0));
        int g = clamp(Json.num(c, "green", 0));
        int b = clamp(Json.num(c, "blue", 0));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp(double v) {
        int i = (int) Math.round(v * 255);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    /// Parses Sketch's `"{0.5, 1}"` point strings.
    static double[] point(String s) {
        double[] p = new double[2];
        if (s == null) {
            return p;
        }
        int st = s.indexOf('{');
        int comma = s.indexOf(',');
        int en = s.indexOf('}');
        if (st < 0 || comma < st || en < comma) {
            return p;
        }
        try {
            p[0] = Double.parseDouble(s.substring(st + 1, comma).trim());
            p[1] = Double.parseDouble(s.substring(comma + 1, en).trim());
        } catch (NumberFormatException e) {
            p[0] = 0;
            p[1] = 0;
        }
        return p;
    }

    private static GeneralPath shapePath(Map<String, Object> l, double[] f) {
        List<Object> pts = Json.list(l, "points");
        GeneralPath p = new GeneralPath();
        if (pts == null || pts.isEmpty()) {
            if ("oval".equals(Json.str(l, "_class"))) {
                return Shapes.ellipse(f[2] / 2, f[3] / 2, f[2] / 2, f[3] / 2);
            }
            return Shapes.roundRect(0, 0, f[2], f[3], Json.num(l, "fixedRadius", 0), Json.num(l, "fixedRadius", 0));
        }
        if ("rectangle".equals(Json.str(l, "_class")) && pts.size() == 4) {
            double[] r = new double[4];
            boolean any = false;
            for (int i = 0; i < 4; i++) {
                r[i] = Json.num(pts.get(i), "cornerRadius", 0);
                any |= r[i] > 0;
            }
            if (any) {
                return Shapes.roundRect(0, 0, f[2], f[3], r);
            }
        }
        int n = pts.size();
        boolean closed = Json.bool(l, "isClosed", true);
        double[] firstPt = null;
        Object prev = null;
        for (int i = 0; i <= n; i++) {
            if (i == n && !closed) {
                break;
            }
            Object c = pts.get(i % n);
            double[] pt = scale(point(Json.str(c, "point")), f);
            if (prev == null) {
                p.moveTo(pt[0], pt[1]);
                firstPt = pt;
            } else {
                boolean from = Json.bool(prev, "hasCurveFrom", false);
                boolean to = Json.bool(c, "hasCurveTo", false);
                if (from || to) {
                    double[] c1 = from ? scale(point(Json.str(prev, "curveFrom")), f)
                            : scale(point(Json.str(prev, "point")), f);
                    double[] c2 = to ? scale(point(Json.str(c, "curveTo")), f) : pt;
                    p.curveTo(c1[0], c1[1], c2[0], c2[1], pt[0], pt[1]);
                } else {
                    p.lineTo(pt[0], pt[1]);
                }
            }
            prev = c;
        }
        if (closed && firstPt != null) {
            p.closePath();
        }
        return p;
    }

    private static double[] scale(double[] p, double[] f) {
        return new double[]{p[0] * f[2], p[1] * f[3]};
    }

    private ArtNode shapeGroup(Map<String, Object> l) {
        GeneralPath combined = new GeneralPath();
        List<Object> ls = Json.list(l, "layers");
        boolean evenOdd = (int) Json.num(Json.obj(l, "style"), "windingRule", 1) == 0;
        // The region the operations describe, as coverage: null while every operation is a union.
        ArtNode region = null;
        int shapes = 0;
        if (ls != null) {
            for (Object o : ls) {
                Map<String, Object> c = Json.asObj(o);
                if (c == null || !Json.bool(c, "isVisible", true)) {
                    continue;
                }
                int op = (int) Json.num(c, "booleanOperation", -1);
                double[] cf = frame(c);
                GeneralPath cp = Shapes.transform(shapePath(c, cf), placement(c, cf));
                if (shapes > 0 && op >= 1 && op <= 3 && shapes < 64) {
                    if (region == null) {
                        region = coverage(combined, evenOdd);
                    }
                    region = combine(region, coverage(cp, false), op);
                } else if (region != null) {
                    region = combine(region, coverage(cp, false), 0);
                }
                combined.append(cp, false);
                shapes++;
            }
        }
        if (evenOdd) {
            combined.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        }
        ShapeNode s = new ShapeNode(combined);
        style(l, s);
        if (region != null) {
            // Fills and borders follow the union; the mask keeps only the region the subtract,
            // intersect and difference operations leave.
            s.setMask(region, false);
        }
        return s;
    }

    /// An opaque shape covering a path.
    private static ArtNode coverage(GeneralPath p, boolean evenOdd) {
        GeneralPath q = new GeneralPath(p);
        if (evenOdd) {
            q.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        }
        ShapeNode n = new ShapeNode(q);
        n.fill(new SolidPaint(0xffffffff));
        return n;
    }

    /// Combines two coverages with a Sketch boolean operation: 0 union, 1 subtract, 2 intersect,
    /// 3 difference.
    private static ArtNode combine(ArtNode a, ArtNode b, int op) {
        GroupNode g = new GroupNode();
        g.setPassThrough(false);
        if (op == 0) {
            g.getChildren().add(a);
            g.getChildren().add(b);
        } else if (op == 2) {
            g.getChildren().add(a);
            g.setMask(b, false);
        } else if (op == 1) {
            g.getChildren().add(a);
            g.setMask(without(b, a), true);
        } else {
            GroupNode left = new GroupNode();
            left.setPassThrough(false);
            left.getChildren().add(a.copy());
            left.setMask(without(b, a), true);
            GroupNode right = new GroupNode();
            right.setPassThrough(false);
            right.getChildren().add(b.copy());
            right.setMask(without(a, b), true);
            g.getChildren().add(left);
            g.getChildren().add(right);
        }
        return g;
    }

    /// A luminance mask that is white over `over`'s bounds except where `cut` covers.
    private static ArtNode without(ArtNode cut, ArtNode over) {
        double[] b = union(over.getBounds(), cut.getBounds());
        GroupNode m = new GroupNode();
        m.setPassThrough(false);
        if (b != null) {
            ShapeNode white = ShapeNode.rect(b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, 0);
            white.fill(new SolidPaint(0xffffffff));
            m.getChildren().add(white);
        }
        ArtNode black = cut.copy();
        black.getEffects().add(Effects.colorOverlay(0xff000000));
        m.getChildren().add(black);
        return m;
    }

    private static double[] union(double[] a, double[] b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        double x = Math.min(a[0], b[0]);
        double y = Math.min(a[1], b[1]);
        return new double[]{x, y, Math.max(a[0] + a[2], b[0] + b[2]) - x, Math.max(a[1] + a[3], b[1] + b[3]) - y};
    }

    private void style(Map<String, Object> l, ShapeNode s) {
        Map<String, Object> st = Json.obj(l, "style");
        double[] f = frame(l);
        List<Object> fills = Json.list(st, "fills");
        if (fills != null) {
            for (int i = 0; i < fills.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                ArtPaint p = paint(fills.get(i), f);
                if (p != null) {
                    s.fill(p);
                }
            }
        }
        List<Object> borders = Json.list(st, "borders");
        Map<String, Object> opts = Json.obj(st, "borderOptions");
        if (borders != null) {
            for (Object b : borders) {
                ArtPaint p = paint(b, f);
                if (p == null) {
                    continue;
                }
                StrokeStyle ss = new StrokeStyle(p, (float) Json.num(b, "thickness", 1));
                int pos = (int) Json.num(b, "position", 0);
                ss.setAlignment(pos == 1 ? StrokeStyle.ALIGN_INSIDE : pos == 2 ? StrokeStyle.ALIGN_OUTSIDE
                        : StrokeStyle.ALIGN_CENTER);
                if (opts != null) {
                    int cap = (int) Json.num(opts, "lineCapStyle", 0);
                    ss.setCap(cap == 1 ? StrokeStyle.CAP_ROUND : cap == 2 ? StrokeStyle.CAP_SQUARE : StrokeStyle.CAP_BUTT);
                    int join = (int) Json.num(opts, "lineJoinStyle", 0);
                    ss.setJoin(join == 1 ? StrokeStyle.JOIN_ROUND : join == 2 ? StrokeStyle.JOIN_BEVEL
                            : StrokeStyle.JOIN_MITER);
                    List<Object> dash = Json.list(opts, "dashPattern");
                    if (dash != null && !dash.isEmpty()) {
                        float[] d = new float[dash.size()];
                        for (int k = 0; k < d.length; k++) {
                            Object o = dash.get(k);
                            d[k] = o instanceof Number ? ((Number) o).floatValue() : 0;
                        }
                        ss.setDash(d);
                    }
                }
                s.stroke(ss);
            }
        }
    }

    private ArtPaint paint(Object fill, double[] f) {
        if (!Json.bool(fill, "isEnabled", true)) {
            return null;
        }
        int type = (int) Json.num(fill, "fillType", 0);
        ArtPaint p;
        if (type == 1) {
            p = gradient(Json.obj(fill, "gradient"), f);
        } else if (type == 4 || type == 5) {
            Raster img = image(Json.obj(fill, "image"));
            if (img == null) {
                return null;
            }
            ImagePaint ip = new ImagePaint(img);
            int pft = (int) Json.num(fill, "patternFillType", 1);
            if (pft == 0) {
                ip.setTiled(true);
                double sc = Json.num(fill, "patternTileScale", 1);
                ip.setTransform(Matrix.scale(sc, sc));
            } else {
                double sx = f[2] / Math.max(1, img.getWidth());
                double sy = f[3] / Math.max(1, img.getHeight());
                if (pft == 1) {
                    double m = Math.max(sx, sy);
                    sx = m;
                    sy = m;
                } else if (pft == 3) {
                    double m = Math.min(sx, sy);
                    sx = m;
                    sy = m;
                }
                double w = img.getWidth() * sx;
                double h = img.getHeight() * sy;
                ip.setTransform(Matrix.translate((f[2] - w) / 2, (f[3] - h) / 2).multiply(Matrix.scale(sx, sy)));
            }
            p = ip;
        } else {
            p = new SolidPaint(color(Json.obj(fill, "color")));
        }
        Map<String, Object> ctx = Json.obj(fill, "contextSettings");
        if (ctx != null) {
            p.setOpacity((float) Json.num(ctx, "opacity", 1));
            p.setBlendMode(blend((int) Json.num(ctx, "blendMode", 0)));
        }
        return p;
    }

    private static GradientPaint gradient(Map<String, Object> g, double[] f) {
        List<Object> st = Json.list(g, "stops");
        int n = st == null ? 0 : st.size();
        float[] pos = new float[Math.max(n, 2)];
        int[] cols = new int[Math.max(n, 2)];
        if (n < 2) {
            pos[1] = 1;
            cols[0] = 0xff000000;
            cols[1] = 0xffffffff;
        }
        for (int i = 0; i < n; i++) {
            pos[i] = (float) Json.num(st.get(i), "position", 0);
            cols[i] = color(Json.obj(st.get(i), "color"));
        }
        sort(pos, cols);
        double[] from = scale(point(Json.str(g, "from")), f);
        double[] to = scale(point(Json.str(g, "to")), f);
        int type = (int) Json.num(g, "gradientType", 0);
        if (type == 1) {
            double dx = to[0] - from[0];
            double dy = to[1] - from[1];
            double r = Math.sqrt(dx * dx + dy * dy);
            GradientPaint gp = GradientPaint.radial(from[0], from[1], r, pos, cols);
            double el = Json.num(g, "elipseLength", 1);
            if (el > 0 && el != 1 && r > 0) {
                // An ellipse: the radius along the gradient line, `elipseLength` times that across.
                double ang = MathUtil.atan2(dy, dx);
                gp = GradientPaint.radial(0, 0, r, pos, cols);
                gp.setTransform(Matrix.translate(from[0], from[1]).multiply(Matrix.rotate(ang))
                        .multiply(Matrix.scale(1, el)));
            }
            return gp;
        }
        if (type == 2) {
            // Sketch's angular gradient starts pointing right and turns clockwise.
            return GradientPaint.conic(f[2] / 2, f[3] / 2, 0, pos, cols);
        }
        return GradientPaint.linear(from[0], from[1], to[0], to[1], pos, cols);
    }

    private static void sort(float[] pos, int[] cols) {
        for (int i = 1; i < pos.length; i++) {
            for (int j = i; j > 0 && pos[j] < pos[j - 1]; j--) {
                float t = pos[j];
                pos[j] = pos[j - 1];
                pos[j - 1] = t;
                int c = cols[j];
                cols[j] = cols[j - 1];
                cols[j - 1] = c;
            }
        }
    }

    private void effects(Map<String, Object> st, ArtNode n) {
        List<Object> sh = Json.list(st, "shadows");
        if (sh != null) {
            for (Object s : sh) {
                if (!Json.bool(s, "isEnabled", true)) {
                    continue;
                }
                n.getEffects().add(Effects.dropShadow(color(Json.obj(s, "color")), (float) Json.num(s, "offsetX", 0),
                        (float) Json.num(s, "offsetY", 0), (float) Json.num(s, "blurRadius", 0),
                        (float) Json.num(s, "spread", 0)));
            }
        }
        List<Object> ish = Json.list(st, "innerShadows");
        if (ish != null) {
            for (Object s : ish) {
                if (!Json.bool(s, "isEnabled", true)) {
                    continue;
                }
                n.getEffects().add(Effects.innerShadow(color(Json.obj(s, "color")), (float) Json.num(s, "offsetX", 0),
                        (float) Json.num(s, "offsetY", 0), (float) Json.num(s, "blurRadius", 0),
                        (float) Json.num(s, "spread", 0)));
            }
        }
        Map<String, Object> blur = Json.obj(st, "blur");
        if (blur != null && Json.bool(blur, "isEnabled", false)) {
            float r = (float) Json.num(blur, "radius", 0);
            int type = (int) Json.num(blur, "type", 0);
            Effect e;
            if (type == 1) {
                e = Effects.motionBlur((float) Json.num(blur, "motionAngle", 0), r);
            } else if (type == 2) {
                double[] c = point(Json.str(blur, "center"));
                // Sketch's center is relative to the layer's middle, as a fraction of its size.
                e = Effects.zoomBlur(Math.min(1, r / 100f), (float) (0.5 + c[0]), (float) (0.5 + c[1]));
            } else if (type == 3) {
                e = Effects.backgroundBlur(r);
            } else {
                e = Effects.gaussianBlur(r);
            }
            n.getEffects().add(e);
        }
    }

    private Raster image(Map<String, Object> ref) {
        String r = Json.str(ref, "_ref");
        if (r == null) {
            return null;
        }
        if (images.containsKey(r)) {
            return images.get(r);
        }
        byte[] b = parts.get(r);
        if (b == null) {
            b = parts.get(r + ".png");
        }
        Raster img = b == null ? null : ArtImages.decode(b);
        if (img == null) {
            note(r, "image could not be decoded");
        }
        images.put(r, img);
        return img;
    }

    private ArtNode bitmap(Map<String, Object> l, double[] f) {
        Raster img = image(Json.obj(l, "image"));
        if (img == null) {
            return null;
        }
        RasterNode r = new RasterNode(img);
        GroupNode g = new GroupNode();
        r.setTransform(Matrix.scale(f[2] / Math.max(1, img.getWidth()), f[3] / Math.max(1, img.getHeight())));
        g.add(r);
        return g;
    }

    private ArtNode text(Map<String, Object> l, double[] f) {
        Map<String, Object> as = Json.obj(l, "attributedString");
        String s = Json.str(as, "string");
        if (s == null) {
            return null;
        }
        TextNode t = new TextNode();
        List<Object> attrs = Json.list(as, "attributes");
        int align = TextNode.ALIGN_LEFT;
        float lineHeight = 0;
        if (attrs != null) {
            for (Object a : attrs) {
                int loc = (int) Json.num(a, "location", 0);
                int len = (int) Json.num(a, "length", 0);
                if (loc < 0 || loc >= s.length() || len <= 0) {
                    continue;
                }
                String part = s.substring(loc, Math.min(s.length(), loc + len));
                Map<String, Object> at = Json.obj(a, "attributes");
                Map<String, Object> font = Json.obj(Json.obj(at, "MSAttributedStringFontAttribute"), "attributes");
                String fname = Json.str(font, "name");
                float size = (float) Json.num(font, "size", 12);
                TextRun run = new TextRun(part, family(fname), size,
                        new SolidPaint(color(Json.obj(at, "MSAttributedStringColorAttribute"))));
                if (fname != null) {
                    run.setBold(fname.indexOf("Bold") >= 0 || fname.indexOf("Heavy") >= 0 || fname.indexOf("Black") >= 0
                            || fname.indexOf("Semibold") >= 0);
                    run.setItalic(fname.indexOf("Italic") >= 0 || fname.indexOf("Oblique") >= 0);
                }
                run.setLetterSpacing((float) Json.num(at, "kerning", 0));
                run.setUnderline(Json.num(at, "underlineStyle", 0) != 0);
                run.setStrikethrough(Json.num(at, "strikethroughStyle", 0) != 0);
                Map<String, Object> ps = Json.obj(at, "paragraphStyle");
                if (ps != null) {
                    int al = (int) Json.num(ps, "alignment", 0);
                    align = al == 1 ? TextNode.ALIGN_RIGHT : al == 2 ? TextNode.ALIGN_CENTER : TextNode.ALIGN_LEFT;
                    double mx = Json.num(ps, "maximumLineHeight", 0);
                    if (mx > 0 && size > 0) {
                        lineHeight = (float) (mx / size);
                    }
                }
                t.getRuns().add(run);
            }
        }
        if (t.getRuns().isEmpty()) {
            t.getRuns().add(new TextRun(s, null, 12, new SolidPaint(0xff000000)));
        }
        t.setAlign(align);
        if (lineHeight > 0) {
            t.setLineHeight(lineHeight);
        }
        float first = t.getRuns().get(0).getSize();
        float lh = lineHeight > 0 ? lineHeight : 1.2f;
        // The frame is the text's top; the node is placed by its first baseline.
        t.setPosition(0, first * (lh - 1.2f) / 2 + first * 0.95f);
        if ((int) Json.num(l, "textBehaviour", 0) != 0) {
            t.setBoxWidth(f[2]);
        } else if (align != TextNode.ALIGN_LEFT) {
            t.setBoxWidth(f[2]);
        }
        return t;
    }

    /// The family part of a PostScript font name: `Helvetica-Bold` is `Helvetica`.
    private static String family(String ps) {
        if (ps == null) {
            return null;
        }
        int dash = ps.indexOf('-');
        return dash > 0 ? ps.substring(0, dash) : ps;
    }

    private ArtNode instance(Map<String, Object> l, double[] f) throws IOException {
        String id = Json.str(l, "symbolID");
        Map<String, Object> master = id == null ? null : symbols.get(id);
        if (master == null) {
            note(Json.str(l, "name") == null ? "symbol" : Json.str(l, "name"), "symbol master is missing");
            return null;
        }
        if (depth >= MAX_SYMBOL_DEPTH) {
            throw new IOException("Sketch symbols nest too deeply");
        }
        depth++;
        try {
            GroupNode g = new GroupNode();
            double[] mf = frame(master);
            if (Json.bool(master, "hasBackgroundColor", false) && Json.bool(master, "includeBackgroundColorInInstance", false)) {
                ShapeNode bg = ShapeNode.rect(0, 0, mf[2], mf[3], 0);
                bg.fill(new SolidPaint(color(Json.obj(master, "backgroundColor"))));
                g.add(bg);
            }
            children(master, g);
            g.setFrame(0, 0, mf[2], mf[3], false);
            List<Object> ov = Json.list(l, "overrideValues");
            if (ov != null) {
                for (int i = 0; i < ov.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                    override(g, Json.str(ov.get(i), "overrideName"), ov.get(i));
                }
            }
            GroupNode holder = new GroupNode();
            if (mf[2] > 0 && mf[3] > 0 && (mf[2] != f[2] || mf[3] != f[3])) {
                g.setTransform(Matrix.scale(f[2] / mf[2], f[3] / mf[3]));
            }
            holder.add(g);
            holder.setSymbolId(id);
            return holder;
        } finally {
            depth--;
        }
    }

    private void override(GroupNode g, String name, Object ov) throws IOException {
        if (name == null) {
            return;
        }
        int us = name.lastIndexOf('_');
        if (us < 0) {
            return;
        }
        String path = name.substring(0, us);
        String prop = name.substring(us + 1);
        String target = path.lastIndexOf('/') >= 0 ? path.substring(path.lastIndexOf('/') + 1) : path;
        ArtNode n = find(g, target);
        if (n == null) {
            return;
        }
        Object v = ov instanceof Map ? Json.asObj(ov).get("value") : null;
        if ("stringValue".equals(prop) && n instanceof TextNode && v instanceof String) {
            TextNode t = (TextNode) n;
            TextRun first = t.getRuns().isEmpty() ? new TextRun("", null, 12, new SolidPaint(0xff000000))
                    : t.getRuns().get(0).copy();
            first.setText((String) v);
            t.getRuns().clear();
            t.getRuns().add(first);
        } else if ("symbolID".equals(prop) && v instanceof String) {
            GroupNode parent = parentOf(g, n);
            Map<String, Object> m = symbols.get((String) v);
            if (parent != null && m != null) {
                GroupNode repl = new GroupNode();
                children(m, repl);
                repl.setTransform(n.getTransform());
                repl.setName(n.getName());
                repl.setSymbolId((String) v);
                int idx = parent.getChildren().indexOf(n);
                parent.getChildren().set(idx, repl);
            } else if (parent != null && "".equals(v)) {
                n.setVisible(false);
            }
        } else if ("image".equals(prop)) {
            Raster img = image(Json.asObj(v));
            if (img != null && n instanceof GroupNode && !((GroupNode) n).getChildren().isEmpty()
                    && ((GroupNode) n).getChildren().get(0) instanceof RasterNode) {
                RasterNode r = (RasterNode) ((GroupNode) n).getChildren().get(0);
                double[] b = r.getBounds();
                r.setImage(img);
                if (b != null) {
                    r.setTransform(Matrix.scale(b[2] / Math.max(1, img.getWidth()), b[3] / Math.max(1, img.getHeight())));
                }
            }
        }
    }

    private static ArtNode find(GroupNode g, String id) {
        List<ArtNode> ch = g.getChildren();
        for (ArtNode c : ch) {
            if (id.equals(c.getId())) {
                return c;
            }
            if (c instanceof GroupNode) {
                ArtNode f = find((GroupNode) c, id);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    private static GroupNode parentOf(GroupNode g, ArtNode n) {
        List<ArtNode> ch = g.getChildren();
        for (ArtNode c : ch) {
            if (c == n) { // NOPMD CompareObjectsWithEquals - node identity
                return g;
            }
            if (c instanceof GroupNode) {
                GroupNode p = parentOf((GroupNode) c, n);
                if (p != null) {
                    return p;
                }
            }
        }
        return null;
    }
}
