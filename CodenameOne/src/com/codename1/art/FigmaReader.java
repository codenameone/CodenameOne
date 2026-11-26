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

import com.codename1.art.effects.Effects;
import com.codename1.io.JSONParser;
import com.codename1.ui.geom.GeneralPath;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Reads a Figma file from the JSON of Figma's REST API (`GET /v1/files/:key`) into an
/// `ArtDocument`.
///
/// Request the file with `geometry=paths`: each node's outline then comes as SVG path data,
/// which is drawn exactly -- boolean operations and vector networks included. Without it,
/// rectangles, ellipses and frames are rebuilt from their sizes and other vectors are skipped
/// with a note. `FigmaClient` fetches a file that way.
///
/// Every canvas (page) becomes a group under the root; the first is visible and the document
/// is sized to its content. Frames, groups, components and instances keep their structure;
/// fills, strokes, drop and inner shadows, layer and background blurs, blend modes, masks,
/// opacity and text are read and rendered. Image fills are looked up by their `imageRef`.
public final class FigmaReader {
    /// The deepest node nesting read.
    public static final int MAX_DEPTH = 256;
    /// The most nodes read.
    public static final int MAX_NODES = 500000;

    private final Map<String, byte[]> imageBytes;
    private final Map<String, Raster> images = new HashMap<String, Raster>();
    private final List<ImportNote> notes = new ArrayList<ImportNote>();
    private int nodes;

    private FigmaReader(Map<String, byte[]> imageBytes) {
        this.imageBytes = imageBytes == null ? new HashMap<String, byte[]>() : imageBytes;
    }

    /// Reads a file's JSON.
    ///
    /// #### Parameters
    ///
    /// - `json`: the API response; read to its end, not closed
    /// - `images`: image fill bytes keyed by `imageRef`, or null to leave image fills empty
    public static ArtDocument read(InputStream json, Map<String, byte[]> images) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        int n;
        while ((n = json.read(buf)) > 0) {
            bo.write(buf, 0, n);
        }
        return read(bo.toByteArray(), images);
    }

    /// Reads a file's JSON from bytes.
    ///
    /// #### Parameters
    ///
    /// - `json`: the API response
    /// - `images`: image fill bytes keyed by `imageRef`, or null
    public static ArtDocument read(byte[] json, Map<String, byte[]> images) throws IOException {
        try {
            return new FigmaReader(images).parse(JSONParser.parseJSON(json));
        } catch (RuntimeException e) {
            throw ArtFormats.damaged(e);
        }
    }

    /// The `imageRef` of every image fill in a file's JSON, so the caller can fetch them.
    ///
    /// #### Parameters
    ///
    /// - `json`: the parsed API response
    public static List<String> imageRefs(Map<String, Object> json) {
        List<String> out = new ArrayList<String>();
        collectRefs(Json.obj(json, "document"), out, 0);
        return out;
    }

    private static void collectRefs(Map<String, Object> n, List<String> out, int depth) {
        if (n == null || depth > MAX_DEPTH) {
            return;
        }
        List<Object> fills = Json.list(n, "fills");
        if (fills != null) {
            for (int i = 0; i < fills.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                String r = Json.str(fills.get(i), "imageRef");
                if (r != null && !out.contains(r)) {
                    out.add(r);
                }
            }
        }
        List<Object> ch = Json.list(n, "children");
        if (ch != null) {
            for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                collectRefs(Json.asObj(ch.get(i)), out, depth + 1);
            }
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

    private ArtDocument parse(Map<String, Object> json) throws IOException {
        Map<String, Object> document = Json.obj(json, "document");
        if (document == null || !"DOCUMENT".equals(Json.str(document, "type"))) {
            throw new IOException("Not a Figma file: no DOCUMENT node");
        }
        GroupNode root = new GroupNode();
        List<Object> canvases = Json.list(document, "children");
        double[] box = null;
        if (canvases != null) {
            for (int i = 0; i < canvases.size(); i++) {
                Map<String, Object> c = Json.asObj(canvases.get(i));
                if (c == null) {
                    continue;
                }
                GroupNode g = new GroupNode();
                g.setName(Json.str(c, "name"));
                g.setId(Json.str(c, "id"));
                children(c, g, null, 1);
                g.setVisible(i == 0);
                if (i == 0) {
                    box = g.getBounds();
                }
                root.add(g);
            }
        }
        if (box == null || box[2] <= 0 || box[3] <= 0) {
            box = new double[]{0, 0, 100, 100};
        }
        Matrix shift = Matrix.translate(-box[0], -box[1]);
        for (int i = 0; i < root.getChildren().size(); i++) {
            root.getChildren().get(i).setTransform(shift);
        }
        ArtDocument doc = new ArtDocument(Math.ceil(box[2]), Math.ceil(box[3]));
        doc.setTitle(Json.str(json, "name"));
        doc.setRoot(root);
        doc.getNotes().addAll(notes);
        return doc;
    }

    private void children(Map<String, Object> parent, GroupNode into, double[] parentBox, int depth)
            throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("Figma file nests too deeply");
        }
        List<Object> ch = Json.list(parent, "children");
        if (ch == null) {
            return;
        }
        GroupNode masked = null;
        for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            Map<String, Object> c = Json.asObj(ch.get(i));
            if (c == null) {
                continue;
            }
            ArtNode n = node(c, parentBox, depth);
            if (n == null) {
                continue;
            }
            if (Json.bool(c, "isMask", false)) {
                // A mask is not drawn; it masks the siblings above it in the same parent.
                masked = new GroupNode();
                masked.setPassThrough(true);
                n.setVisible(true);
                n.setOpacity(1);
                n.getEffects().clear();
                masked.setMask(n, "LUMINANCE".equals(Json.str(c, "maskType")));
                into.add(masked);
                continue;
            }
            if (masked != null) {
                masked.add(n);
            } else {
                into.add(n);
            }
        }
    }

    private static double[] box(Map<String, Object> n) {
        Map<String, Object> b = Json.obj(n, "absoluteBoundingBox");
        if (b == null) {
            return null;
        }
        return new double[]{Json.num(b, "x", 0), Json.num(b, "y", 0), Json.num(b, "width", 0),
            Json.num(b, "height", 0)};
    }

    private static double[] size(Map<String, Object> n, double[] abs) {
        Map<String, Object> s = Json.obj(n, "size");
        if (s != null) {
            return new double[]{Json.num(s, "x", 0), Json.num(s, "y", 0)};
        }
        return abs == null ? new double[]{0, 0} : new double[]{abs[2], abs[3]};
    }

    private static Matrix transform(Map<String, Object> n, double[] abs, double[] parentBox) {
        List<Object> rt = Json.list(n, "relativeTransform");
        if (rt != null && rt.size() == 2 && rt.get(0) instanceof List && rt.get(1) instanceof List) {
            List<?> r0 = (List<?>) rt.get(0);
            List<?> r1 = (List<?>) rt.get(1);
            if (r0.size() == 3 && r1.size() == 3) {
                return new Matrix(d(r0.get(0)), d(r1.get(0)), d(r0.get(1)), d(r1.get(1)), d(r0.get(2)), d(r1.get(2)));
            }
        }
        if (abs == null) {
            return Matrix.IDENTITY;
        }
        double px = parentBox == null ? 0 : parentBox[0];
        double py = parentBox == null ? 0 : parentBox[1];
        return Matrix.translate(abs[0] - px, abs[1] - py);
    }

    private static double d(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }

    private ArtNode node(Map<String, Object> n, double[] parentBox, int depth) throws IOException {
        nodes++;
        if (nodes > MAX_NODES) {
            throw new IOException("Figma file has too many nodes");
        }
        String type = Json.str(n, "type");
        if (type == null) {
            return null;
        }
        String name = Json.str(n, "name");
        double[] abs = box(n);
        double[] sz = size(n, abs);
        ArtNode out;
        if ("FRAME".equals(type) || "COMPONENT".equals(type) || "COMPONENT_SET".equals(type)
                || "INSTANCE".equals(type) || "SECTION".equals(type)) {
            GroupNode g = new GroupNode();
            g.setFrame(0, 0, sz[0], sz[1], Json.bool(n, "clipsContent", !"SECTION".equals(type)));
            ShapeNode bg = new ShapeNode(outline(n, sz, "fillGeometry"));
            paints(n, bg, sz);
            if (!bg.getFills().isEmpty() || !bg.getStrokes().isEmpty()) {
                bg.setName("Background");
                g.add(bg);
            }
            children(n, g, abs, depth + 1);
            if ("INSTANCE".equals(type)) {
                g.setSymbolId(Json.str(n, "componentId"));
            }
            out = g;
        } else if ("GROUP".equals(type)) {
            GroupNode g = new GroupNode();
            children(n, g, abs, depth + 1);
            out = g;
        } else if ("TEXT".equals(type)) {
            out = text(n, sz);
        } else if ("SLICE".equals(type) || "CONNECTOR".equals(type) || "STICKY".equals(type)
                || "WIDGET".equals(type) || "EMBED".equals(type) || "LINK_UNFURL".equals(type)) {
            return null;
        } else {
            // RECTANGLE, ELLIPSE, VECTOR, STAR, LINE, REGULAR_POLYGON, BOOLEAN_OPERATION...
            GeneralPath p = outline(n, sz, "fillGeometry");
            ShapeNode s = new ShapeNode(p);
            paints(n, s, sz);
            out = s;
        }
        if (out == null) {
            return null;
        }
        // A group in the REST API is positioned in its parent's space but its children are
        // positioned relative to it, so groups need their own offset like any other node.
        out.setTransform(transform(n, abs, parentBox));
        if ("GROUP".equals(type) && Json.list(n, "relativeTransform") != null) {
            // With relativeTransform, children of a group are relative to the group's parent.
            Matrix t = out.getTransform();
            Matrix inv = t.invert();
            if (inv != null) {
                List<ArtNode> ch = ((GroupNode) out).getChildren();
                for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                    ch.get(i).setTransform(inv.multiply(ch.get(i).getTransform()));
                }
            }
        }
        out.setName(name);
        out.setId(Json.str(n, "id"));
        out.setVisible(Json.bool(n, "visible", true));
        out.setLocked(Json.bool(n, "locked", false));
        out.setOpacity((float) Json.num(n, "opacity", 1));
        String bm = Json.str(n, "blendMode");
        if ("PASS_THROUGH".equals(bm) && out instanceof GroupNode) {
            ((GroupNode) out).setPassThrough(true);
        } else if (out instanceof GroupNode) {
            ((GroupNode) out).setPassThrough(false);
            out.setBlendMode(blend(bm));
        } else {
            out.setBlendMode(blend(bm));
        }
        effects(n, out);
        return out;
    }

    static BlendMode blend(String s) {
        if (s == null) {
            return BlendMode.NORMAL;
        }
        String[] k = {"MULTIPLY", "SCREEN", "OVERLAY", "DARKEN", "LIGHTEN", "COLOR_DODGE", "COLOR_BURN", "HARD_LIGHT",
            "SOFT_LIGHT", "DIFFERENCE", "EXCLUSION", "HUE", "SATURATION", "COLOR", "LUMINOSITY", "LINEAR_BURN",
            "LINEAR_DODGE"};
        BlendMode[] m = {BlendMode.MULTIPLY, BlendMode.SCREEN, BlendMode.OVERLAY, BlendMode.DARKEN, BlendMode.LIGHTEN,
            BlendMode.COLOR_DODGE, BlendMode.COLOR_BURN, BlendMode.HARD_LIGHT, BlendMode.SOFT_LIGHT,
            BlendMode.DIFFERENCE, BlendMode.EXCLUSION, BlendMode.HUE, BlendMode.SATURATION, BlendMode.COLOR,
            BlendMode.LUMINOSITY, BlendMode.LINEAR_BURN, BlendMode.LINEAR_DODGE};
        for (int i = 0; i < k.length; i++) {
            if (k[i].equals(s)) {
                return m[i];
            }
        }
        return BlendMode.NORMAL;
    }

    private GeneralPath outline(Map<String, Object> n, double[] sz, String key) {
        List<Object> geo = Json.list(n, key);
        if (geo != null && !geo.isEmpty()) {
            GeneralPath p = new GeneralPath();
            boolean evenOdd = false;
            for (int i = 0; i < geo.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                String d = Json.str(geo.get(i), "path");
                if (d != null) {
                    p.append(PathData.parse(d), false);
                }
                evenOdd |= "EVENODD".equals(Json.str(geo.get(i), "windingRule"));
            }
            if (evenOdd) {
                p.setWindingRule(GeneralPath.WIND_EVEN_ODD);
            }
            return p;
        }
        String type = Json.str(n, "type");
        if ("ELLIPSE".equals(type)) {
            return Shapes.ellipse(sz[0] / 2, sz[1] / 2, sz[0] / 2, sz[1] / 2);
        }
        if ("LINE".equals(type)) {
            GeneralPath p = new GeneralPath();
            p.moveTo(0, 0);
            p.lineTo(sz[0], 0);
            return p;
        }
        if (!"RECTANGLE".equals(type) && !"FRAME".equals(type) && !"COMPONENT".equals(type)
                && !"INSTANCE".equals(type) && !"COMPONENT_SET".equals(type) && !"SECTION".equals(type)
                && "fillGeometry".equals(key)) {
            note(Json.str(n, "name") == null ? "vector" : Json.str(n, "name"),
                    "no geometry in the file; request it with geometry=paths");
        }
        List<Object> radii = Json.list(n, "rectangleCornerRadii");
        if (radii != null && radii.size() == 4) {
            double[] r = new double[4];
            for (int i = 0; i < 4; i++) {
                r[i] = d(radii.get(i));
            }
            return Shapes.roundRect(0, 0, sz[0], sz[1], r);
        }
        double r = Json.num(n, "cornerRadius", 0);
        return Shapes.roundRect(0, 0, sz[0], sz[1], r, r);
    }

    private void paints(Map<String, Object> n, ShapeNode s, double[] sz) {
        List<Object> fills = Json.list(n, "fills");
        if (fills != null) {
            for (int i = 0; i < fills.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                ArtPaint p = paint(fills.get(i), sz);
                if (p != null) {
                    s.fill(p);
                }
            }
        }
        List<Object> strokes = Json.list(n, "strokes");
        if (strokes == null || strokes.isEmpty()) {
            return;
        }
        float w = (float) Json.num(n, "strokeWeight", 1);
        String align = Json.str(n, "strokeAlign");
        for (int i = 0; i < strokes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            ArtPaint p = paint(strokes.get(i), sz);
            if (p == null) {
                continue;
            }
            StrokeStyle st = new StrokeStyle(p, w);
            st.setAlignment("INSIDE".equals(align) ? StrokeStyle.ALIGN_INSIDE
                    : "OUTSIDE".equals(align) ? StrokeStyle.ALIGN_OUTSIDE : StrokeStyle.ALIGN_CENTER);
            String cap = Json.str(n, "strokeCap");
            st.setCap("ROUND".equals(cap) ? StrokeStyle.CAP_ROUND : "SQUARE".equals(cap) ? StrokeStyle.CAP_SQUARE
                    : StrokeStyle.CAP_BUTT);
            String join = Json.str(n, "strokeJoin");
            st.setJoin("ROUND".equals(join) ? StrokeStyle.JOIN_ROUND : "BEVEL".equals(join) ? StrokeStyle.JOIN_BEVEL
                    : StrokeStyle.JOIN_MITER);
            List<Object> dash = Json.list(n, "strokeDashes");
            if (dash != null && !dash.isEmpty()) {
                float[] dd = new float[dash.size()];
                for (int k = 0; k < dd.length; k++) {
                    dd[k] = (float) d(dash.get(k));
                }
                st.setDash(dd);
            }
            s.stroke(st);
        }
    }

    static int color(Map<String, Object> c, double opacity) {
        if (c == null) {
            return 0;
        }
        int a = clamp(Json.num(c, "a", 1) * opacity);
        return (a << 24) | (clamp(Json.num(c, "r", 0)) << 16) | (clamp(Json.num(c, "g", 0)) << 8)
                | clamp(Json.num(c, "b", 0));
    }

    private static int clamp(double v) {
        int i = (int) Math.round(v * 255);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    private ArtPaint paint(Object f, double[] sz) {
        if (!Json.bool(f, "visible", true)) {
            return null;
        }
        String type = Json.str(f, "type");
        float op = (float) Json.num(f, "opacity", 1);
        ArtPaint p;
        if ("SOLID".equals(type)) {
            p = new SolidPaint(color(Json.obj(f, "color"), 1));
        } else if (type != null && type.startsWith("GRADIENT_")) {
            p = gradient(f, type, sz);
        } else if ("IMAGE".equals(type)) {
            p = imagePaint(f, sz);
        } else {
            return null;
        }
        if (p == null) {
            return null;
        }
        p.setOpacity(op);
        p.setBlendMode(blend(Json.str(f, "blendMode")));
        return p;
    }

    private static ArtPaint gradient(Object f, String type, double[] sz) {
        List<Object> st = Json.list(f, "gradientStops");
        int n = st == null ? 0 : st.size();
        float[] pos = new float[Math.max(2, n)];
        int[] cols = new int[Math.max(2, n)];
        if (n < 2) {
            pos[1] = 1;
            cols[1] = 0xff000000;
        }
        for (int i = 0; i < n; i++) {
            pos[i] = (float) Json.num(st.get(i), "position", 0);
            cols[i] = color(Json.obj(st.get(i), "color"), 1);
        }
        List<Object> h = Json.list(f, "gradientHandlePositions");
        double[][] hp = {{0, 0.5}, {1, 0.5}, {0, 1}};
        if (h != null) {
            for (int i = 0; i < Math.min(3, h.size()); i++) {
                hp[i][0] = Json.num(h.get(i), "x", hp[i][0]);
                hp[i][1] = Json.num(h.get(i), "y", hp[i][1]);
            }
        }
        double x0 = hp[0][0] * sz[0];
        double y0 = hp[0][1] * sz[1];
        double x1 = hp[1][0] * sz[0];
        double y1 = hp[1][1] * sz[1];
        double x2 = hp[2][0] * sz[0];
        double y2 = hp[2][1] * sz[1];
        if ("GRADIENT_LINEAR".equals(type)) {
            return GradientPaint.linear(x0, y0, x1, y1, pos, cols);
        }
        // Radial, angular and diamond gradients are an ellipse whose axes are the handles:
        // a unit gradient mapped by the matrix the three handles span.
        Matrix m = new Matrix(x1 - x0, y1 - y0, x2 - x0, y2 - y0, x0, y0);
        GradientPaint g;
        if ("GRADIENT_ANGULAR".equals(type)) {
            g = GradientPaint.conic(0, 0, 0, pos, cols);
        } else if ("GRADIENT_DIAMOND".equals(type)) {
            g = GradientPaint.diamond(0, 0, 1, pos, cols);
        } else {
            g = GradientPaint.radial(0, 0, 1, pos, cols);
        }
        g.setTransform(m);
        return g;
    }

    private ArtPaint imagePaint(Object f, double[] sz) {
        String ref = Json.str(f, "imageRef");
        Raster img = null;
        if (ref != null) {
            if (images.containsKey(ref)) {
                img = images.get(ref);
            } else {
                byte[] b = imageBytes.get(ref);
                img = b == null ? null : ArtImages.decode(b);
                images.put(ref, img);
                if (img == null) {
                    note(ref, b == null ? "image fill was not supplied" : "image fill could not be decoded");
                }
            }
        }
        if (img == null) {
            return null;
        }
        ImagePaint ip = new ImagePaint(img);
        String mode = Json.str(f, "scaleMode");
        double iw = Math.max(1, img.getWidth());
        double ih = Math.max(1, img.getHeight());
        if ("TILE".equals(mode)) {
            double sc = Json.num(f, "scalingFactor", 1);
            ip.setTiled(true);
            ip.setTransform(Matrix.scale(sc, sc));
        } else if ("STRETCH".equals(mode)) {
            ip.setTransform(Matrix.scale(sz[0] / iw, sz[1] / ih));
        } else {
            double sx = sz[0] / iw;
            double sy = sz[1] / ih;
            double s = "FIT".equals(mode) ? Math.min(sx, sy) : Math.max(sx, sy);
            ip.setTransform(Matrix.translate((sz[0] - iw * s) / 2, (sz[1] - ih * s) / 2).multiply(Matrix.scale(s, s)));
        }
        return ip;
    }

    private void effects(Map<String, Object> n, ArtNode out) {
        List<Object> fx = Json.list(n, "effects");
        if (fx == null) {
            return;
        }
        for (Object e : fx) {
            if (!Json.bool(e, "visible", true)) {
                continue;
            }
            String t = Json.str(e, "type");
            float r = (float) Json.num(e, "radius", 0);
            int c = color(Json.obj(e, "color"), 1);
            Map<String, Object> off = Json.obj(e, "offset");
            float dx = (float) Json.num(off, "x", 0);
            float dy = (float) Json.num(off, "y", 0);
            float spread = (float) Json.num(e, "spread", 0);
            if ("DROP_SHADOW".equals(t)) {
                out.getEffects().add(Effects.dropShadow(c, dx, dy, r, spread)
                        .set("blend", blend(Json.str(e, "blendMode"))));
            } else if ("INNER_SHADOW".equals(t)) {
                out.getEffects().add(Effects.innerShadow(c, dx, dy, r, spread)
                        .set("blend", blend(Json.str(e, "blendMode"))));
            } else if ("LAYER_BLUR".equals(t)) {
                out.getEffects().add(Effects.gaussianBlur(r));
            } else if ("BACKGROUND_BLUR".equals(t)) {
                out.getEffects().add(Effects.backgroundBlur(r));
            }
        }
    }

    private ArtNode text(Map<String, Object> n, double[] sz) {
        String s = Json.str(n, "characters");
        if (s == null) {
            return null;
        }
        Map<String, Object> base = Json.obj(n, "style");
        List<Object> overrides = Json.list(n, "characterStyleOverrides");
        Map<String, Object> table = Json.obj(n, "styleOverrideTable");
        List<Object> fills = Json.list(n, "fills");
        TextNode t = new TextNode();
        int start = 0;
        int len = s.length();
        while (start < len) {
            int key = styleAt(overrides, start);
            int end = start + 1;
            while (end < len && styleAt(overrides, end) == key) {
                end++;
            }
            Map<String, Object> st = key == 0 ? null : Json.obj(table, String.valueOf(key));
            t.getRuns().add(run(s.substring(start, end), base, st, fills));
            start = end;
        }
        if (t.getRuns().isEmpty()) {
            t.getRuns().add(run("", base, null, fills));
        }
        String al = Json.str(base, "textAlignHorizontal");
        t.setAlign("CENTER".equals(al) ? TextNode.ALIGN_CENTER : "RIGHT".equals(al) ? TextNode.ALIGN_RIGHT
                : TextNode.ALIGN_LEFT);
        float size = (float) Json.num(base, "fontSize", 12);
        double lh = Json.num(base, "lineHeightPx", size * 1.2);
        if (size > 0) {
            t.setLineHeight((float) (lh / size));
        }
        if (!"WIDTH_AND_HEIGHT".equals(Json.str(base, "textAutoResize")) || t.getAlign() != TextNode.ALIGN_LEFT) {
            t.setBoxWidth(sz[0]);
        }
        t.setPosition(0, (lh - size) / 2 + size * 0.95);
        return t;
    }

    private static int styleAt(List<Object> overrides, int i) {
        if (overrides == null || i >= overrides.size()) {
            return 0;
        }
        Object o = overrides.get(i);
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    private TextRun run(String text, Map<String, Object> base, Map<String, Object> st, List<Object> fills) {
        String family = Json.str(st, "fontFamily");
        if (family == null) {
            family = Json.str(base, "fontFamily");
        }
        float size = (float) Json.num(st, "fontSize", Json.num(base, "fontSize", 12));
        double weight = Json.num(st, "fontWeight", Json.num(base, "fontWeight", 400));
        boolean italic = Json.bool(st, "italic", Json.bool(base, "italic", false));
        List<Object> f = Json.list(st, "fills");
        if (f == null) {
            f = fills;
        }
        ArtPaint paint = null;
        if (f != null) {
            for (int i = 0; i < f.size() && paint == null; i++) {
                paint = paint(f.get(i), new double[]{1, 1});
            }
        }
        if (paint == null) {
            paint = new SolidPaint(0xff000000);
        }
        TextRun r = new TextRun(text, family, size, paint);
        r.setBold(weight >= 600);
        r.setItalic(italic);
        String deco = Json.str(st, "textDecoration");
        if (deco == null) {
            deco = Json.str(base, "textDecoration");
        }
        r.setUnderline("UNDERLINE".equals(deco));
        r.setStrikethrough("STRIKETHROUGH".equals(deco));
        r.setLetterSpacing((float) Json.num(st, "letterSpacing", Json.num(base, "letterSpacing", 0)));
        return r;
    }
}
