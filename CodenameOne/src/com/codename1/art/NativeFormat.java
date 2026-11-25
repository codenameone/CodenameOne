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
import com.codename1.io.JSONParser;
import com.codename1.io.zip.ZipArchiveOutputStream;
import com.codename1.ui.geom.GeneralPath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

/// The `.cn1gfx` container: a ZIP holding `document.json` and every raster as a PNG under
/// `images/`. It keeps everything an `ArtDocument` holds -- every node, paint, stroke, effect
/// parameter and note -- so a document saved and read back is the same document.
public final class NativeFormat {
    private static final String ENTRY = "document.json";
    private static final int VERSION = 1;

    private final List<Raster> rasters = new ArrayList<Raster>();
    private Map<String, byte[]> parts = new HashMap<String, byte[]>();
    private int depth;

    private NativeFormat() {
    }

    /// Writes a document.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document
    /// - `out`: receives the file; not closed
    public static void write(ArtDocument doc, OutputStream out) throws IOException {
        NativeFormat f = new NativeFormat();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("format", "cn1gfx");
        m.put("version", Integer.valueOf(VERSION));
        m.put("width", Double.valueOf(doc.getWidth()));
        m.put("height", Double.valueOf(doc.getHeight()));
        if (doc.getTitle() != null) {
            m.put("title", doc.getTitle());
        }
        m.put("root", f.node(doc.getRoot()));
        List<Object> notes = new ArrayList<Object>();
        for (int i = 0; i < doc.getNotes().size(); i++) {
            ImportNote n = doc.getNotes().get(i);
            Map<String, Object> nm = new LinkedHashMap<String, Object>();
            nm.put("where", n.getWhere());
            nm.put("message", n.getMessage());
            notes.add(nm);
        }
        m.put("notes", notes);
        ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out);
        zip.putNextEntry(new ZipEntry(ENTRY));
        zip.write(JSONParser.mapToJson(m).getBytes("UTF-8"));
        zip.closeEntry();
        for (int i = 0; i < f.rasters.size(); i++) {
            zip.putNextEntry(new ZipEntry("images/" + i + ".png"));
            zip.write(ArtImages.encodePng(f.rasters.get(i)));
            zip.closeEntry();
        }
        zip.finish();
    }

    /// Reads a document.
    ///
    /// #### Parameters
    ///
    /// - `in`: the file; read to its end, not closed
    public static ArtDocument read(InputStream in) throws IOException {
        NativeFormat f = new NativeFormat();
        f.parts = ArtZip.read(in, "cn1gfx file");
        byte[] json = f.parts.get(ENTRY);
        if (json == null) {
            throw new IOException("Not a cn1gfx file: document.json is missing");
        }
        Map<String, Object> m = JSONParser.parseJSON(json);
        if (!"cn1gfx".equals(Json.str(m, "format"))) {
            throw new IOException("Not a cn1gfx file");
        }
        if (Json.num(m, "version", 1) > VERSION) {
            throw new IOException("cn1gfx version " + (int) Json.num(m, "version", 1) + " is newer than this reader");
        }
        ArtDocument doc = new ArtDocument(Json.num(m, "width", 100), Json.num(m, "height", 100));
        doc.setTitle(Json.str(m, "title"));
        ArtNode root = f.readNode(Json.obj(m, "root"));
        doc.setRoot(root instanceof GroupNode ? (GroupNode) root : new GroupNode());
        List<Object> notes = Json.list(m, "notes");
        if (notes != null) {
            for (int i = 0; i < notes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                String w = Json.str(notes.get(i), "where");
                String msg = Json.str(notes.get(i), "message");
                if (w != null && msg != null) {
                    doc.getNotes().add(new ImportNote(w, msg));
                }
            }
        }
        return doc;
    }

    // ---- writing ----

    private Integer raster(Raster r) {
        rasters.add(r);
        return Integer.valueOf(rasters.size() - 1);
    }

    private static List<Object> matrix(Matrix t) {
        List<Object> l = new ArrayList<Object>();
        l.add(Double.valueOf(t.getA()));
        l.add(Double.valueOf(t.getB()));
        l.add(Double.valueOf(t.getC()));
        l.add(Double.valueOf(t.getD()));
        l.add(Double.valueOf(t.getE()));
        l.add(Double.valueOf(t.getF()));
        return l;
    }

    private static Map<String, Object> path(GeneralPath p) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("d", SvgWriter.pathData(p));
        m.put("evenOdd", Boolean.valueOf(p.getWindingRule() == GeneralPath.WIND_EVEN_ODD));
        return m;
    }

    private Map<String, Object> node(ArtNode n) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        put(m, "id", n.getId());
        put(m, "name", n.getName());
        m.put("visible", Boolean.valueOf(n.isVisible()));
        m.put("locked", Boolean.valueOf(n.isLocked()));
        m.put("opacity", Double.valueOf(n.getOpacity()));
        m.put("blend", n.getBlendMode().name());
        if (!n.getTransform().isIdentity()) {
            m.put("transform", matrix(n.getTransform()));
        }
        if (n.getClipPath() != null) {
            m.put("clip", path(n.getClipPath()));
        }
        if (n.getMask() != null) {
            m.put("mask", node(n.getMask()));
            m.put("luminanceMask", Boolean.valueOf(n.isLuminanceMask()));
        }
        if (n.isKnockout()) {
            m.put("knockout", Boolean.TRUE);
        }
        if (!n.getEffects().isEmpty()) {
            List<Object> fx = new ArrayList<Object>();
            for (int i = 0; i < n.getEffects().size(); i++) {
                fx.add(effect(n.getEffects().get(i)));
            }
            m.put("effects", fx);
        }
        if (n instanceof GroupNode) {
            GroupNode g = (GroupNode) n;
            m.put("type", "group");
            m.put("passThrough", Boolean.valueOf(g.isPassThrough()));
            double[] f = g.getFrame();
            if (f != null) {
                List<Object> fl = new ArrayList<Object>();
                for (int i = 0; i < 4; i++) {
                    fl.add(Double.valueOf(f[i]));
                }
                m.put("frame", fl);
                m.put("clipToFrame", Boolean.valueOf(g.isClipToFrame()));
            }
            put(m, "symbol", g.getSymbolId());
            List<Object> ch = new ArrayList<Object>();
            for (int i = 0; i < g.getChildren().size(); i++) {
                ch.add(node(g.getChildren().get(i)));
            }
            m.put("children", ch);
        } else if (n instanceof ShapeNode) {
            ShapeNode s = (ShapeNode) n;
            m.put("type", "shape");
            m.put("path", path(s.getPath()));
            List<Object> fills = new ArrayList<Object>();
            for (int i = 0; i < s.getFills().size(); i++) {
                fills.add(paint(s.getFills().get(i)));
            }
            m.put("fills", fills);
            List<Object> strokes = new ArrayList<Object>();
            for (int i = 0; i < s.getStrokes().size(); i++) {
                strokes.add(stroke(s.getStrokes().get(i)));
            }
            m.put("strokes", strokes);
        } else if (n instanceof RasterNode) {
            Raster r = ((RasterNode) n).getImage();
            m.put("type", "raster");
            m.put("image", raster(r));
            m.put("x", Integer.valueOf(r.getX()));
            m.put("y", Integer.valueOf(r.getY()));
        } else if (n instanceof TextNode) {
            TextNode t = (TextNode) n;
            m.put("type", "text");
            m.put("x", Double.valueOf(t.getX()));
            m.put("y", Double.valueOf(t.getY()));
            m.put("boxWidth", Double.valueOf(t.getBoxWidth()));
            m.put("align", Integer.valueOf(t.getAlign()));
            m.put("lineHeight", Double.valueOf(t.getLineHeight()));
            List<Object> runs = new ArrayList<Object>();
            for (int i = 0; i < t.getRuns().size(); i++) {
                TextRun r = t.getRuns().get(i);
                Map<String, Object> rm = new LinkedHashMap<String, Object>();
                rm.put("text", r.getText());
                put(rm, "font", r.getFontFamily());
                rm.put("size", Double.valueOf(r.getSize()));
                rm.put("bold", Boolean.valueOf(r.isBold()));
                rm.put("italic", Boolean.valueOf(r.isItalic()));
                rm.put("underline", Boolean.valueOf(r.isUnderline()));
                rm.put("strike", Boolean.valueOf(r.isStrikethrough()));
                rm.put("spacing", Double.valueOf(r.getLetterSpacing()));
                if (r.getFill() != null) {
                    rm.put("fill", paint(r.getFill()));
                }
                runs.add(rm);
            }
            m.put("runs", runs);
        } else if (n instanceof AdjustmentNode) {
            m.put("type", "adjustment");
            m.put("adjustment", effect(((AdjustmentNode) n).getAdjustment()));
        }
        return m;
    }

    private static void put(Map<String, Object> m, String k, String v) {
        if (v != null) {
            m.put(k, v);
        }
    }

    private Map<String, Object> paint(ArtPaint p) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("opacity", Double.valueOf(p.getOpacity()));
        m.put("blend", p.getBlendMode().name());
        m.put("enabled", Boolean.valueOf(p.isEnabled()));
        if (p instanceof SolidPaint) {
            m.put("type", "solid");
            m.put("color", Colors.toHex(((SolidPaint) p).getColor()));
        } else if (p instanceof GradientPaint) {
            GradientPaint g = (GradientPaint) p;
            m.put("type", "gradient");
            m.put("kind", Integer.valueOf(g.getType()));
            m.put("positions", value(g.getPositions()));
            m.put("colors", value(g.getColors()));
            List<Object> geo = new ArrayList<Object>();
            double[] v = {g.getX1(), g.getY1(), g.getX2(), g.getY2(), g.getRadius(), g.getFx(), g.getFy()};
            for (int i = 0; i < v.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                geo.add(Double.valueOf(v[i]));
            }
            m.put("geometry", geo);
            m.put("spread", Integer.valueOf(g.getSpread()));
            m.put("bbox", Boolean.valueOf(g.isObjectBoundingBox()));
            m.put("transform", matrix(g.getTransform()));
        } else if (p instanceof ImagePaint) {
            ImagePaint ip = (ImagePaint) p;
            m.put("type", "image");
            m.put("image", raster(ip.getImage()));
            m.put("tiled", Boolean.valueOf(ip.isTiled()));
            m.put("bbox", Boolean.valueOf(ip.isObjectBoundingBox()));
            m.put("transform", matrix(ip.getTransform()));
        }
        return m;
    }

    private Map<String, Object> stroke(StrokeStyle s) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("paint", paint(s.getPaint()));
        m.put("width", Double.valueOf(s.getWidth()));
        m.put("cap", Integer.valueOf(s.getCap()));
        m.put("join", Integer.valueOf(s.getJoin()));
        m.put("miter", Double.valueOf(s.getMiterLimit()));
        if (s.getDash() != null) {
            m.put("dash", value(s.getDash()));
        }
        m.put("dashOffset", Double.valueOf(s.getDashOffset()));
        m.put("align", Integer.valueOf(s.getAlignment()));
        m.put("enabled", Boolean.valueOf(s.isEnabled()));
        return m;
    }

    private Map<String, Object> effect(Effect e) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("kind", e.getKind());
        m.put("enabled", Boolean.valueOf(e.isEnabled()));
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        Iterator<Map.Entry<String, Object>> it = e.getParameters().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> en = it.next();
            Object v = value(en.getValue());
            if (v != null) {
                p.put(en.getKey(), v);
            }
        }
        m.put("params", p);
        return m;
    }

    /// A typed value: `{"t": tag, "v": value}`, so a float, an int color and a blend mode come
    /// back as what they were.
    @SuppressWarnings("unchecked")
    private Object value(Object v) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        if (v instanceof Float || v instanceof Double) {
            m.put("t", "f");
            m.put("v", Double.valueOf(((Number) v).doubleValue()));
        } else if (v instanceof Integer) {
            m.put("t", "i");
            m.put("v", v);
        } else if (v instanceof String) {
            m.put("t", "s");
            m.put("v", v);
        } else if (v instanceof Boolean) {
            m.put("t", "z");
            m.put("v", v);
        } else if (v instanceof BlendMode) {
            m.put("t", "b");
            m.put("v", ((BlendMode) v).name());
        } else if (v instanceof float[]) {
            float[] a = (float[]) v;
            List<Object> l = new ArrayList<Object>();
            for (int i = 0; i < a.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                l.add(Double.valueOf(a[i]));
            }
            m.put("t", "fa");
            m.put("v", l);
        } else if (v instanceof int[]) {
            int[] a = (int[]) v;
            List<Object> l = new ArrayList<Object>();
            for (int i = 0; i < a.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                l.add(Integer.valueOf(a[i]));
            }
            m.put("t", "ia");
            m.put("v", l);
        } else if (v instanceof Raster) {
            m.put("t", "r");
            m.put("v", raster((Raster) v));
        } else if (v instanceof List) {
            List<Object> src = (List<Object>) v;
            List<Object> l = new ArrayList<Object>();
            for (int i = 0; i < src.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                l.add(value(src.get(i)));
            }
            m.put("t", "l");
            m.put("v", l);
        } else if (v instanceof Map) {
            Map<String, Object> src = (Map<String, Object>) v;
            Map<String, Object> o = new LinkedHashMap<String, Object>();
            Iterator<Map.Entry<String, Object>> it = src.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> en = it.next();
                Object ev = value(en.getValue());
                if (ev != null) {
                    o.put(en.getKey(), ev);
                }
            }
            m.put("t", "m");
            m.put("v", o);
        } else {
            return null;
        }
        return m;
    }

    // ---- reading ----

    private Raster readRaster(Object index) throws IOException {
        int i = index instanceof Number ? ((Number) index).intValue() : -1;
        byte[] b = parts.get("images/" + i + ".png");
        Raster r = b == null ? null : ArtImages.decode(b);
        if (r == null) {
            throw new IOException("cn1gfx image " + i + " is missing or damaged");
        }
        return r;
    }

    private static Matrix readMatrix(List<Object> l) {
        if (l == null || l.size() != 6) {
            return Matrix.IDENTITY;
        }
        return new Matrix(dbl(l.get(0)), dbl(l.get(1)), dbl(l.get(2)), dbl(l.get(3)), dbl(l.get(4)), dbl(l.get(5)));
    }

    private static double dbl(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }

    private static GeneralPath readPath(Map<String, Object> m) {
        GeneralPath p = PathData.parse(Json.str(m, "d") == null ? "" : Json.str(m, "d"));
        if (Json.bool(m, "evenOdd", false)) {
            p.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        }
        return p;
    }

    private static BlendMode blend(String s) {
        if (s != null) {
            BlendMode[] all = BlendMode.values();
            for (int i = 0; i < all.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                if (all[i].name().equals(s)) {
                    return all[i];
                }
            }
        }
        return BlendMode.NORMAL;
    }

    private ArtNode readNode(Map<String, Object> m) throws IOException {
        if (m == null) {
            return null;
        }
        depth++;
        if (depth > 512) {
            throw new IOException("cn1gfx file nests too deeply");
        }
        try {
            String type = Json.str(m, "type");
            ArtNode n;
            if ("group".equals(type)) {
                GroupNode g = new GroupNode();
                g.setPassThrough(Json.bool(m, "passThrough", false));
                List<Object> f = Json.list(m, "frame");
                if (f != null && f.size() == 4) {
                    g.setFrame(dbl(f.get(0)), dbl(f.get(1)), dbl(f.get(2)), dbl(f.get(3)),
                            Json.bool(m, "clipToFrame", false));
                }
                g.setSymbolId(Json.str(m, "symbol"));
                List<Object> ch = Json.list(m, "children");
                if (ch != null) {
                    for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                        ArtNode c = readNode(Json.asObj(ch.get(i)));
                        if (c != null) {
                            g.add(c);
                        }
                    }
                }
                n = g;
            } else if ("shape".equals(type)) {
                ShapeNode s = new ShapeNode(readPath(Json.obj(m, "path")));
                List<Object> fills = Json.list(m, "fills");
                if (fills != null) {
                    for (int i = 0; i < fills.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                        ArtPaint p = readPaint(Json.asObj(fills.get(i)));
                        if (p != null) {
                            s.fill(p);
                        }
                    }
                }
                List<Object> strokes = Json.list(m, "strokes");
                if (strokes != null) {
                    for (int i = 0; i < strokes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                        StrokeStyle st = readStroke(Json.asObj(strokes.get(i)));
                        if (st != null) {
                            s.stroke(st);
                        }
                    }
                }
                n = s;
            } else if ("raster".equals(type)) {
                Raster r = readRaster(Json.asObj(m).get("image"));
                n = new RasterNode(new Raster((int) Json.num(m, "x", 0), (int) Json.num(m, "y", 0), r.getWidth(),
                        r.getHeight(), r.getPixels()));
            } else if ("text".equals(type)) {
                TextNode t = new TextNode();
                t.setPosition(Json.num(m, "x", 0), Json.num(m, "y", 0));
                t.setBoxWidth(Json.num(m, "boxWidth", 0));
                t.setAlign((int) Json.num(m, "align", 0));
                t.setLineHeight((float) Json.num(m, "lineHeight", 1.2));
                List<Object> runs = Json.list(m, "runs");
                if (runs != null) {
                    for (int i = 0; i < runs.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                        Map<String, Object> rm = Json.asObj(runs.get(i));
                        String text = Json.str(rm, "text");
                        TextRun r = new TextRun(text == null ? "" : text, Json.str(rm, "font"),
                                (float) Json.num(rm, "size", 12), readPaint(Json.obj(rm, "fill")));
                        r.setBold(Json.bool(rm, "bold", false));
                        r.setItalic(Json.bool(rm, "italic", false));
                        r.setUnderline(Json.bool(rm, "underline", false));
                        r.setStrikethrough(Json.bool(rm, "strike", false));
                        r.setLetterSpacing((float) Json.num(rm, "spacing", 0));
                        t.getRuns().add(r);
                    }
                }
                n = t;
            } else if ("adjustment".equals(type)) {
                Effect e = readEffect(Json.obj(m, "adjustment"));
                if (e == null) {
                    return null;
                }
                n = new AdjustmentNode(e);
            } else {
                return null;
            }
            n.setId(Json.str(m, "id"));
            n.setName(Json.str(m, "name"));
            n.setVisible(Json.bool(m, "visible", true));
            n.setLocked(Json.bool(m, "locked", false));
            n.setOpacity((float) Json.num(m, "opacity", 1));
            n.setBlendMode(blend(Json.str(m, "blend")));
            n.setTransform(readMatrix(Json.list(m, "transform")));
            Map<String, Object> clip = Json.obj(m, "clip");
            if (clip != null) {
                n.setClipPath(readPath(clip));
            }
            Map<String, Object> mask = Json.obj(m, "mask");
            if (mask != null) {
                n.setMask(readNode(mask), Json.bool(m, "luminanceMask", false));
            }
            if (Json.bool(m, "knockout", false)) {
                n.setKnockout(true);
            }
            List<Object> fx = Json.list(m, "effects");
            if (fx != null) {
                for (int i = 0; i < fx.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                    Effect e = readEffect(Json.asObj(fx.get(i)));
                    if (e != null) {
                        n.getEffects().add(e);
                    }
                }
            }
            return n;
        } finally {
            depth--;
        }
    }

    private ArtPaint readPaint(Map<String, Object> m) throws IOException {
        if (m == null) {
            return null;
        }
        String type = Json.str(m, "type");
        ArtPaint p;
        if ("solid".equals(type)) {
            p = new SolidPaint(Colors.parse(Json.str(m, "color"), 0xff000000));
        } else if ("gradient".equals(type)) {
            Object pos = readValue(Json.asObj(m).get("positions"));
            Object cols = readValue(Json.asObj(m).get("colors"));
            if (!(pos instanceof float[]) || !(cols instanceof int[])
                    || ((float[]) pos).length != ((int[]) cols).length || ((float[]) pos).length == 0) {
                return null;
            }
            GradientPaint g = new GradientPaint((int) Json.num(m, "kind", 0), (float[]) pos, (int[]) cols);
            List<Object> geo = Json.list(m, "geometry");
            if (geo != null && geo.size() == 7) {
                g.setPoints(dbl(geo.get(0)), dbl(geo.get(1)), dbl(geo.get(2)), dbl(geo.get(3)));
                g.setRadial(dbl(geo.get(4)), dbl(geo.get(5)), dbl(geo.get(6)));
            }
            g.setSpread((int) Json.num(m, "spread", 0));
            g.setObjectBoundingBox(Json.bool(m, "bbox", false));
            g.setTransform(readMatrix(Json.list(m, "transform")));
            p = g;
        } else if ("image".equals(type)) {
            ImagePaint ip = new ImagePaint(readRaster(Json.asObj(m).get("image")));
            ip.setTiled(Json.bool(m, "tiled", false));
            ip.setObjectBoundingBox(Json.bool(m, "bbox", false));
            ip.setTransform(readMatrix(Json.list(m, "transform")));
            p = ip;
        } else {
            return null;
        }
        p.setOpacity((float) Json.num(m, "opacity", 1));
        p.setBlendMode(blend(Json.str(m, "blend")));
        p.setEnabled(Json.bool(m, "enabled", true));
        return p;
    }

    private StrokeStyle readStroke(Map<String, Object> m) throws IOException {
        ArtPaint p = readPaint(Json.obj(m, "paint"));
        if (p == null) {
            return null;
        }
        StrokeStyle s = new StrokeStyle(p, (float) Json.num(m, "width", 1));
        s.setCap((int) Json.num(m, "cap", 0));
        s.setJoin((int) Json.num(m, "join", 0));
        s.setMiterLimit((float) Json.num(m, "miter", 4));
        Object dash = readValue(Json.asObj(m).get("dash"));
        if (dash instanceof float[]) {
            s.setDash((float[]) dash);
        }
        s.setDashOffset((float) Json.num(m, "dashOffset", 0));
        s.setAlignment((int) Json.num(m, "align", 0));
        s.setEnabled(Json.bool(m, "enabled", true));
        return s;
    }

    private Effect readEffect(Map<String, Object> m) throws IOException {
        String kind = Json.str(m, "kind");
        if (kind == null) {
            return null;
        }
        Effect e = new Effect(kind);
        e.setEnabled(Json.bool(m, "enabled", true));
        Map<String, Object> p = Json.obj(m, "params");
        if (p != null) {
            Iterator<Map.Entry<String, Object>> it = p.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> en = it.next();
                Object v = readValue(en.getValue());
                if (v != null) {
                    e.set(en.getKey(), v);
                }
            }
        }
        return e;
    }

    private Object readValue(Object o) throws IOException {
        Map<String, Object> m = Json.asObj(o);
        if (m == null) {
            return null;
        }
        String t = Json.str(m, "t");
        Object v = m.get("v");
        if ("f".equals(t)) {
            return Float.valueOf((float) dbl(v));
        }
        if ("i".equals(t)) {
            return Integer.valueOf(v instanceof Number ? ((Number) v).intValue() : 0);
        }
        if ("s".equals(t)) {
            return v instanceof String ? v : null;
        }
        if ("z".equals(t)) {
            return Boolean.valueOf(Json.bool(m, "v", false));
        }
        if ("b".equals(t)) {
            return blend(v instanceof String ? (String) v : null);
        }
        if ("fa".equals(t) || "ia".equals(t)) {
            List<?> l = v instanceof List ? (List<?>) v : new ArrayList<Object>();
            if ("fa".equals(t)) {
                float[] a = new float[l.size()];
                for (int i = 0; i < a.length; i++) {
                    a[i] = (float) dbl(l.get(i));
                }
                return a;
            }
            int[] a = new int[l.size()];
            for (int i = 0; i < a.length; i++) {
                Object x = l.get(i);
                a[i] = x instanceof Number ? ((Number) x).intValue() : 0;
            }
            return a;
        }
        if ("r".equals(t)) {
            return readRaster(v);
        }
        if ("l".equals(t)) {
            List<Object> out = new ArrayList<Object>();
            if (v instanceof List) {
                List<?> l = (List<?>) v;
                for (int i = 0; i < l.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                    out.add(readValue(l.get(i)));
                }
            }
            return out;
        }
        if ("m".equals(t)) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            Map<String, Object> src = Json.asObj(v);
            if (src != null) {
                Iterator<Map.Entry<String, Object>> it = src.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Object> en = it.next();
                    Object x = readValue(en.getValue());
                    if (x != null) {
                        out.put(en.getKey(), x);
                    }
                }
            }
            return out;
        }
        return null;
    }
}
