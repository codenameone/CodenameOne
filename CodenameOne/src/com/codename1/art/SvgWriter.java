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
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.PathIterator;
import com.codename1.util.Base64;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Writes an `ArtDocument` as SVG.
///
/// Shapes, paths, groups, gradients, image paints, clips, masks, text and blend modes are
/// written as SVG. Effects with an SVG filter equivalent (blurs, shadows, color matrices and
/// SVG filter graphs) become filters. A node whose effects SVG cannot express (a bevel, a
/// curves adjustment) and every adjustment layer is written as the image it renders to, so
/// the output looks the same in any SVG viewer.
public final class SvgWriter {
    private final StringBuilder defs = new StringBuilder(); //NOPMD AvoidStringBufferField - one buffer per write
    private int nextId;
    private double rasterScale = 2;

    /// Sets the resolution, in pixels per unit, of nodes written as images.
    ///
    /// #### Parameters
    ///
    /// - `scale`: pixels per document unit
    public SvgWriter setRasterScale(double scale) {
        this.rasterScale = Math.max(0.1, scale);
        return this;
    }

    /// Writes a document.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document
    /// - `out`: receives UTF-8 SVG; not closed
    public static void write(ArtDocument doc, OutputStream out) throws IOException {
        out.write(new SvgWriter().toSvg(doc).getBytes("UTF-8"));
    }

    /// The SVG text of a document.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document
    public String toSvg(ArtDocument doc) {
        defs.setLength(0);
        nextId = 0;
        StringBuilder body = new StringBuilder();
        List<ArtNode> ch = doc.getRoot().getChildren();
        for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            node(ch.get(i), body, doc);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<svg xmlns=\"http://www.w3.org/2000/svg\" ")
                .append("xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"").append(num(doc.getWidth()))
                .append("\" height=\"").append(num(doc.getHeight())).append("\" viewBox=\"0 0 ")
                .append(num(doc.getWidth())).append(' ').append(num(doc.getHeight())).append("\">\n");
        if (doc.getTitle() != null) {
            sb.append("<title>").append(esc(doc.getTitle())).append("</title>\n");
        }
        if (defs.length() > 0) {
            sb.append("<defs>\n").append(defs).append("</defs>\n");
        }
        sb.append(body).append("</svg>\n");
        return sb.toString();
    }

    private String id(String prefix) {
        return prefix + (++nextId);
    }

    static String num(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "0";
        }
        long r = Math.round(v * 1000);
        if (r % 1000 == 0) {
            return String.valueOf(r / 1000);
        }
        String s = String.valueOf(r / 1000.0);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '&') {
                sb.append("&amp;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else if (c < 0x20 && c != '\n' && c != '\t' && c != '\r') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String matrix(Matrix m) {
        return "matrix(" + num(m.getA()) + ' ' + num(m.getB()) + ' ' + num(m.getC()) + ' ' + num(m.getD())
                + ' ' + num(m.getE()) + ' ' + num(m.getF()) + ')';
    }

    /// Path data for a path.
    ///
    /// #### Parameters
    ///
    /// - `p`: the path
    public static String pathData(GeneralPath p) {
        StringBuilder sb = new StringBuilder();
        PathIterator it = p.getPathIterator();
        double[] c = new double[6];
        while (!it.isDone()) {
            int t = it.currentSegment(c);
            if (t == PathIterator.SEG_MOVETO) {
                sb.append('M').append(num(c[0])).append(' ').append(num(c[1]));
            } else if (t == PathIterator.SEG_LINETO) {
                sb.append('L').append(num(c[0])).append(' ').append(num(c[1]));
            } else if (t == PathIterator.SEG_QUADTO) {
                sb.append('Q').append(num(c[0])).append(' ').append(num(c[1])).append(' ')
                        .append(num(c[2])).append(' ').append(num(c[3]));
            } else if (t == PathIterator.SEG_CUBICTO) {
                sb.append('C').append(num(c[0])).append(' ').append(num(c[1])).append(' ')
                        .append(num(c[2])).append(' ').append(num(c[3])).append(' ')
                        .append(num(c[4])).append(' ').append(num(c[5]));
            } else {
                sb.append('Z');
            }
            it.next();
        }
        return sb.toString();
    }

    private static String blendName(BlendMode m) {
        String n = m.name();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            sb.append(c == '_' ? '-' : (c >= 'A' && c <= 'Z' ? (char) (c + 32) : c));
        }
        return sb.toString();
    }

    private static boolean expressible(Effect e) {
        String k = e.getKind();
        return Effect.GAUSSIAN_BLUR.equals(k) || Effect.DROP_SHADOW.equals(k) || Effect.COLOR_MATRIX.equals(k)
                || Effect.SVG_FILTER.equals(k) || Effect.INVERT.equals(k) || Effect.COLOR_OVERLAY.equals(k);
    }

    private boolean needsRaster(ArtNode n) {
        if (n instanceof AdjustmentNode) {
            return true;
        }
        List<Effect> fx = n.getEffects();
        for (int i = 0; i < fx.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            if (fx.get(i).isEnabled() && !expressible(fx.get(i))) {
                return true;
            }
        }
        if (n instanceof GroupNode) {
            List<ArtNode> ch = ((GroupNode) n).getChildren();
            for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                if (ch.get(i) instanceof AdjustmentNode) {
                    return true;
                }
            }
        }
        return false;
    }

    private void common(ArtNode n, StringBuilder sb) {
        if (n.getId() != null) {
            sb.append(" id=\"").append(esc(n.getId())).append('"');
        }
        if (!n.getTransform().isIdentity()) {
            sb.append(" transform=\"").append(matrix(n.getTransform())).append('"');
        }
        if (n.getOpacity() < 1) {
            sb.append(" opacity=\"").append(num(n.getOpacity())).append('"');
        }
        if (!n.isVisible()) {
            sb.append(" display=\"none\"");
        }
        StringBuilder style = new StringBuilder();
        if (n.getBlendMode() != BlendMode.NORMAL) {
            style.append("mix-blend-mode:").append(blendName(n.getBlendMode())).append(';');
        }
        if (n instanceof GroupNode && !((GroupNode) n).isPassThrough()) {
            style.append("isolation:isolate;");
        }
        if (style.length() > 0) {
            sb.append(" style=\"").append(style).append('"');
        }
        if (n.getClipPath() != null) {
            String cid = id("clip");
            defs.append("<clipPath id=\"").append(cid).append("\" clipPathUnits=\"userSpaceOnUse\"><path d=\"")
                    .append(pathData(n.getClipPath())).append("\"/></clipPath>\n");
            sb.append(" clip-path=\"url(#").append(cid).append(")\"");
        }
        if (n.getMask() != null) {
            String mid = id("mask");
            StringBuilder mb = new StringBuilder();
            node(n.getMask(), mb, null);
            defs.append("<mask id=\"").append(mid).append("\" maskUnits=\"userSpaceOnUse\" x=\"-100000\" y=\"-100000\" width=\"200000\" height=\"200000\"")
                    .append(n.isLuminanceMask() ? "" : " style=\"mask-type:alpha\"").append('>')
                    .append(mb).append("</mask>\n");
            sb.append(" mask=\"url(#").append(mid).append(")\"");
        }
        String f = filters(n.getEffects());
        if (f != null) {
            sb.append(" filter=\"url(#").append(f).append(")\"");
        }
        if (n.getName() != null) {
            sb.append(" data-name=\"").append(esc(n.getName())).append('"');
        }
    }

    private String filters(List<Effect> fx) {
        StringBuilder fb = new StringBuilder();
        String last = "SourceGraphic";
        int k = 0;
        for (Effect e : fx) {
            if (!e.isEnabled()) {
                continue;
            }
            String res = "r" + (k++);
            String kind = e.getKind();
            if (Effect.GAUSSIAN_BLUR.equals(kind)) {
                fb.append("<feGaussianBlur in=\"").append(last).append("\" stdDeviation=\"")
                        .append(num(e.getFloat("radius", 4) / 2)).append("\" result=\"").append(res).append("\"/>");
            } else if (Effect.DROP_SHADOW.equals(kind)) {
                int c = e.getColor("color", 0x80000000);
                fb.append("<feDropShadow in=\"").append(last).append("\" dx=\"").append(num(e.getFloat("dx", 0)))
                        .append("\" dy=\"").append(num(e.getFloat("dy", 0))).append("\" stdDeviation=\"")
                        .append(num(e.getFloat("radius", 4) / 2)).append("\" flood-color=\"").append(Colors.toHex(c))
                        .append("\" flood-opacity=\"").append(num((c >>> 24) / 255.0)).append("\" result=\"")
                        .append(res).append("\"/>");
            } else if (Effect.COLOR_MATRIX.equals(kind) || Effect.INVERT.equals(kind)) {
                float[] m = Effect.INVERT.equals(kind)
                        ? new float[]{-1, 0, 0, 0, 1, 0, -1, 0, 0, 1, 0, 0, -1, 0, 1, 0, 0, 0, 1, 0}
                        : e.getFloats("matrix", null);
                if (m == null || m.length != 20) {
                    continue;
                }
                fb.append("<feColorMatrix in=\"").append(last).append("\" type=\"matrix\" values=\"");
                for (int j = 0; j < 20; j++) {
                    fb.append(j > 0 ? " " : "").append(num(m[j]));
                }
                fb.append("\" color-interpolation-filters=\"sRGB\" result=\"").append(res).append("\"/>");
            } else if (Effect.COLOR_OVERLAY.equals(kind)) {
                int c = e.getColor("color", 0xffff0000);
                fb.append("<feFlood flood-color=\"").append(Colors.toHex(c)).append("\" flood-opacity=\"")
                        .append(num((c >>> 24) / 255.0 * e.getFloat("opacity", 1))).append("\"/>")
                        .append("<feComposite in2=\"").append(last).append("\" operator=\"in\" result=\"o")
                        .append(res).append("\"/><feMerge result=\"").append(res).append("\"><feMergeNode in=\"")
                        .append(last).append("\"/><feMergeNode in=\"o").append(res).append("\"/></feMerge>");
            } else if (Effect.SVG_FILTER.equals(kind)) {
                List<Map<String, Object>> prims = e.getPrimitives();
                for (int j = 0; j < prims.size(); j++) {
                    primitive(prims.get(j), fb, j == 0 ? last : null);
                }
                if (prims.isEmpty()) {
                    continue;
                }
                fb.append("<feMerge result=\"").append(res).append("\"><feMergeNode/></feMerge>");
            } else {
                continue;
            }
            last = res;
        }
        if (fb.length() == 0) {
            return null;
        }
        String fid = id("filter");
        defs.append("<filter id=\"").append(fid)
                .append("\" x=\"-50%\" y=\"-50%\" width=\"200%\" height=\"200%\" color-interpolation-filters=\"sRGB\">")
                .append(fb).append("</filter>\n");
        return fid;
    }

    private void primitive(Map<String, Object> p, StringBuilder fb, String firstIn) {
        Object type = p.get("type");
        if (!(type instanceof String)) {
            return;
        }
        fb.append('<').append(type);
        java.util.Iterator<Map.Entry<String, Object>> it = p.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> en = it.next();
            String k = en.getKey();
            Object v = en.getValue();
            if ("type".equals(k) || "children".equals(k) || !(v instanceof String)) {
                continue;
            }
            if ("matrixType".equals(k) || "noiseType".equals(k) || "funcType".equals(k)) {
                k = "type";
            }
            if ("href".equals(k)) {
                continue;
            }
            fb.append(' ').append(k).append("=\"").append(esc((String) v)).append('"');
        }
        if (firstIn != null && !(p.get("in") instanceof String)) {
            // The filter's first primitive reads what the effects before it produced.
            fb.append(" in=\"").append(firstIn).append('"');
        }
        if (p.get("image") instanceof Raster) {
            fb.append(" href=\"data:image/png;base64,")
                    .append(Base64.encodeNoNewline(ArtImages.encodePng((Raster) p.get("image")))).append('"');
        }
        Object ch = p.get("children");
        if (ch instanceof List) {
            fb.append('>');
            List<?> l = (List<?>) ch;
            for (int i = 0; i < l.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                if (l.get(i) instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cm = (Map<String, Object>) l.get(i);
                    primitive(cm, fb, null);
                }
            }
            fb.append("</").append(type).append('>');
        } else {
            fb.append("/>");
        }
    }

    private void rasterNode(ArtNode n, StringBuilder sb, ArtDocument doc) {
        double[] b = n instanceof AdjustmentNode && doc != null
                ? new double[]{0, 0, doc.getWidth(), doc.getHeight()} : null;
        ArtNode copy = n.copy();
        Matrix t = copy.getTransform();
        copy.setTransform(Matrix.IDENTITY);
        BlendMode bm = copy.getBlendMode();
        float op = copy.getOpacity();
        copy.setBlendMode(BlendMode.NORMAL);
        copy.setOpacity(1);
        int pad = 0;
        for (int i = 0; i < copy.getEffects().size(); i++) {
            pad += copy.getEffects().get(i).padding(rasterScale);
        }
        if (b == null) {
            b = copy.getLocalBounds();
        }
        if (b == null) {
            return;
        }
        double p = pad / rasterScale;
        double x = b[0] - p;
        double y = b[1] - p;
        double w = b[2] + 2 * p;
        double h = b[3] + 2 * p;
        int pw = (int) Math.ceil(w * rasterScale);
        int ph = (int) Math.ceil(h * rasterScale);
        if (pw <= 0 || ph <= 0 || (long) pw * ph > Raster.MAX_PIXELS) {
            return;
        }
        Raster target = new Raster(0, 0, pw, ph);
        if (n instanceof AdjustmentNode) {
            // An adjustment changes what is under it, which a standalone image cannot do.
            // Bake it over the content painted before it in the document.
            if (doc != null) {
                ArtDocument under = before(doc, n);
                new ArtRenderer().renderNode(under.getRoot(), Matrix.scale(rasterScale, rasterScale), target);
            }
        }
        new ArtRenderer().renderNode(copy, Matrix.scale(rasterScale, rasterScale)
                .multiply(Matrix.translate(-x, -y)), target);
        sb.append("<image");
        ArtNode holder = new RasterNode(target);
        holder.setId(n.getId());
        holder.setName(n.getName());
        holder.setTransform(t);
        holder.setOpacity(op);
        holder.setBlendMode(n instanceof AdjustmentNode ? BlendMode.NORMAL : bm);
        holder.setVisible(n.isVisible());
        common(holder, sb);
        sb.append(" x=\"").append(num(x)).append("\" y=\"").append(num(y)).append("\" width=\"").append(num(w))
                .append("\" height=\"").append(num(h)).append("\" preserveAspectRatio=\"none\" href=\"data:image/png;base64,")
                .append(Base64.encodeNoNewline(ArtImages.encodePng(target))).append("\"/>\n");
    }

    static ArtDocument before(ArtDocument doc, ArtNode stop) {
        ArtDocument d = new ArtDocument(doc.getWidth(), doc.getHeight());
        GroupNode root = new GroupNode();
        collectBefore(doc.getRoot(), stop, root);
        d.setRoot(root);
        return d;
    }

    private static boolean collectBefore(GroupNode g, ArtNode stop, GroupNode into) {
        List<ArtNode> ch = g.getChildren();
        for (ArtNode c : ch) {
            if (c == stop) { // NOPMD CompareObjectsWithEquals - node identity
                return true;
            }
            if (c instanceof GroupNode && contains((GroupNode) c, stop)) {
                GroupNode part = new GroupNode();
                part.setTransform(c.getTransform());
                into.add(part);
                return collectBefore((GroupNode) c, stop, part);
            }
            into.add(c);
        }
        return false;
    }

    private static boolean contains(GroupNode g, ArtNode n) {
        List<ArtNode> ch = g.getChildren();
        for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            if (ch.get(i) == n || ch.get(i) instanceof GroupNode && contains((GroupNode) ch.get(i), n)) { // NOPMD CompareObjectsWithEquals - node identity
                return true;
            }
        }
        return false;
    }

    private void node(ArtNode n, StringBuilder sb, ArtDocument doc) {
        if (needsRaster(n)) {
            // An adjustment is baked over the content before it, so the image it becomes
            // covers that content with the adjusted pixels.
            rasterNode(n, sb, doc);
            return;
        }
        if (n instanceof GroupNode) {
            GroupNode g = (GroupNode) n;
            sb.append("<g");
            common(n, sb);
            sb.append(">\n");
            if (g.getFrame() != null) {
                double[] f = g.getFrame();
                if (g.getBackground() != null) {
                    sb.append("<rect x=\"").append(num(f[0])).append("\" y=\"").append(num(f[1])).append("\" width=\"")
                            .append(num(f[2])).append("\" height=\"").append(num(f[3])).append('"');
                    paintAttr("fill", g.getBackground(), sb, f);
                    sb.append("/>\n");
                }
                if (g.isClipToFrame()) {
                    String cid = id("clip");
                    defs.append("<clipPath id=\"").append(cid).append("\"><rect x=\"").append(num(f[0])).append("\" y=\"")
                            .append(num(f[1])).append("\" width=\"").append(num(f[2])).append("\" height=\"")
                            .append(num(f[3])).append("\"/></clipPath>\n");
                    sb.append("<g clip-path=\"url(#").append(cid).append(")\">\n");
                }
            }
            List<ArtNode> ch = g.getChildren();
            for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                node(ch.get(i), sb, doc);
            }
            if (g.getFrame() != null && g.isClipToFrame()) {
                sb.append("</g>\n");
            }
            sb.append("</g>\n");
        } else if (n instanceof ShapeNode) {
            shape((ShapeNode) n, sb);
        } else if (n instanceof TextNode) {
            text((TextNode) n, sb);
        } else if (n instanceof RasterNode) {
            Raster r = ((RasterNode) n).getImage();
            if (r == null) {
                return;
            }
            sb.append("<image");
            common(n, sb);
            sb.append(" x=\"").append(r.getX()).append("\" y=\"").append(r.getY()).append("\" width=\"")
                    .append(r.getWidth()).append("\" height=\"").append(r.getHeight())
                    .append("\" href=\"data:image/png;base64,").append(Base64.encodeNoNewline(ArtImages.encodePng(r)))
                    .append("\"/>\n");
        }
    }

    private void shape(ShapeNode s, StringBuilder sb) {
        List<ArtPaint> fills = s.getFills();
        List<StrokeStyle> strokes = s.getStrokes();
        float[] fb = s.getPath().getBounds2D();
        double[] bbox = {fb[0], fb[1], fb[2], fb[3]};
        String d = pathData(s.getPath());
        boolean evenOdd = s.getPath().getWindingRule() == PathIterator.WIND_EVEN_ODD;
        int layers = Math.max(1, fills.size()) + strokes.size();
        boolean group = layers > 1;
        if (group) {
            sb.append("<g");
            common(s, sb);
            sb.append(">\n");
        }
        if (fills.isEmpty() && !group) {
            sb.append("<path");
            common(s, sb);
            sb.append(" d=\"").append(d).append("\" fill=\"none\"/>\n");
            return;
        }
        for (ArtPaint p : fills) {
            if (!p.isEnabled()) {
                continue;
            }
            sb.append("<path");
            if (!group) {
                common(s, sb);
            }
            sb.append(" d=\"").append(d).append('"');
            if (evenOdd) {
                sb.append(" fill-rule=\"evenodd\"");
            }
            paintAttr("fill", p, sb, bbox);
            if (p.getBlendMode() != BlendMode.NORMAL) {
                sb.append(" style=\"mix-blend-mode:").append(blendName(p.getBlendMode())).append('"');
            }
            sb.append("/>\n");
        }
        for (StrokeStyle st : strokes) {
            if (!st.isEnabled() || st.getPaint() == null) {
                continue;
            }
            sb.append("<path");
            if (!group) {
                common(s, sb);
            }
            sb.append(" d=\"").append(d).append("\" fill=\"none\"");
            paintAttr("stroke", st.getPaint(), sb, bbox);
            float w = st.getWidth();
            if (st.getAlignment() != StrokeStyle.ALIGN_CENTER) {
                String cid = id("clip");
                if (st.getAlignment() == StrokeStyle.ALIGN_INSIDE) {
                    defs.append("<clipPath id=\"").append(cid).append("\"><path d=\"").append(d).append("\"")
                            .append(evenOdd ? " clip-rule=\"evenodd\"" : "").append("/></clipPath>\n");
                    sb.append(" clip-path=\"url(#").append(cid).append(")\"");
                } else {
                    defs.append("<mask id=\"").append(cid).append("\" maskUnits=\"userSpaceOnUse\" x=\"-100000\" y=\"-100000\" width=\"200000\" height=\"200000\">")
                            .append("<rect x=\"-100000\" y=\"-100000\" width=\"200000\" height=\"200000\" fill=\"white\"/><path d=\"")
                            .append(d).append("\" fill=\"black\"/></mask>\n");
                    sb.append(" mask=\"url(#").append(cid).append(")\"");
                }
                w = w * 2;
            }
            sb.append(" stroke-width=\"").append(num(w)).append('"');
            if (st.getCap() == StrokeStyle.CAP_ROUND) {
                sb.append(" stroke-linecap=\"round\"");
            } else if (st.getCap() == StrokeStyle.CAP_SQUARE) {
                sb.append(" stroke-linecap=\"square\"");
            }
            if (st.getJoin() == StrokeStyle.JOIN_ROUND) {
                sb.append(" stroke-linejoin=\"round\"");
            } else if (st.getJoin() == StrokeStyle.JOIN_BEVEL) {
                sb.append(" stroke-linejoin=\"bevel\"");
            } else if (st.getMiterLimit() != 4) {
                sb.append(" stroke-miterlimit=\"").append(num(st.getMiterLimit())).append('"');
            }
            float[] dash = st.getDash();
            if (dash != null && dash.length > 0) {
                sb.append(" stroke-dasharray=\"");
                for (int k = 0; k < dash.length; k++) {
                    sb.append(k > 0 ? " " : "").append(num(dash[k]));
                }
                sb.append('"');
                if (st.getDashOffset() != 0) {
                    sb.append(" stroke-dashoffset=\"").append(num(st.getDashOffset())).append('"');
                }
            }
            sb.append("/>\n");
        }
        if (group) {
            sb.append("</g>\n");
        }
    }

    private void paintAttr(String attr, ArtPaint p, StringBuilder sb, double[] bbox) {
        if (p instanceof SolidPaint) {
            int c = ((SolidPaint) p).getColor();
            sb.append(' ').append(attr).append("=\"").append(Colors.toHex(c)).append('"');
            double a = (c >>> 24) / 255.0 * p.getOpacity();
            if (a < 1) {
                sb.append(' ').append(attr).append("-opacity=\"").append(num(a)).append('"');
            }
            return;
        }
        String id = p instanceof GradientPaint ? gradient((GradientPaint) p, bbox)
                : p instanceof ImagePaint ? pattern((ImagePaint) p) : null;
        if (id == null) {
            sb.append(' ').append(attr).append("=\"none\"");
            return;
        }
        sb.append(' ').append(attr).append("=\"url(#").append(id).append(")\"");
        if (p.getOpacity() < 1) {
            sb.append(' ').append(attr).append("-opacity=\"").append(num(p.getOpacity())).append('"');
        }
    }

    private String gradient(GradientPaint g, double[] bbox) {
        String id = id("grad");
        float[] pos = g.getPositions();
        int[] cols = g.getColors();
        if (g.getType() == GradientPaint.CONIC || g.getType() == GradientPaint.DIAMOND) {
            // SVG has no conic or diamond gradient: sample it into an image pattern over the bounds.
            if (bbox == null || bbox[2] <= 0 || bbox[3] <= 0) {
                return null;
            }
            int w = Math.max(1, Math.min(1024, (int) Math.ceil(bbox[2] * rasterScale)));
            int h = Math.max(1, Math.min(1024, (int) Math.ceil(bbox[3] * rasterScale)));
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    px[y * w + x] = g.colorAt(bbox[0] + (x + 0.5) * bbox[2] / w, bbox[1] + (y + 0.5) * bbox[3] / h, bbox);
                }
            }
            defs.append("<pattern id=\"").append(id).append("\" patternUnits=\"userSpaceOnUse\" x=\"").append(num(bbox[0]))
                    .append("\" y=\"").append(num(bbox[1])).append("\" width=\"").append(num(bbox[2])).append("\" height=\"")
                    .append(num(bbox[3])).append("\"><image width=\"").append(num(bbox[2])).append("\" height=\"")
                    .append(num(bbox[3])).append("\" preserveAspectRatio=\"none\" href=\"data:image/png;base64,")
                    .append(Base64.encodeNoNewline(ArtImages.encodePng(new Raster(0, 0, w, h, px))))
                    .append("\"/></pattern>\n");
            return id;
        }
        boolean lin = g.getType() == GradientPaint.LINEAR;
        defs.append(lin ? "<linearGradient" : "<radialGradient").append(" id=\"").append(id).append('"');
        if (lin) {
            defs.append(" x1=\"").append(num(g.getX1())).append("\" y1=\"").append(num(g.getY1())).append("\" x2=\"")
                    .append(num(g.getX2())).append("\" y2=\"").append(num(g.getY2())).append('"');
        } else {
            defs.append(" cx=\"").append(num(g.getX1())).append("\" cy=\"").append(num(g.getY1())).append("\" r=\"")
                    .append(num(g.getRadius())).append("\" fx=\"").append(num(g.getFx())).append("\" fy=\"")
                    .append(num(g.getFy())).append('"');
        }
        defs.append(" gradientUnits=\"").append(g.isObjectBoundingBox() ? "objectBoundingBox" : "userSpaceOnUse").append('"');
        if (!g.getTransform().isIdentity()) {
            defs.append(" gradientTransform=\"").append(matrix(g.getTransform())).append('"');
        }
        if (g.getSpread() == GradientPaint.SPREAD_REFLECT) {
            defs.append(" spreadMethod=\"reflect\"");
        } else if (g.getSpread() == GradientPaint.SPREAD_REPEAT) {
            defs.append(" spreadMethod=\"repeat\"");
        }
        defs.append('>');
        for (int i = 0; i < pos.length; i++) {
            defs.append("<stop offset=\"").append(num(pos[i])).append("\" stop-color=\"").append(Colors.toHex(cols[i])).append('"');
            if ((cols[i] >>> 24) != 255) {
                defs.append(" stop-opacity=\"").append(num((cols[i] >>> 24) / 255.0)).append('"');
            }
            defs.append("/>");
        }
        defs.append(lin ? "</linearGradient>\n" : "</radialGradient>\n");
        return id;
    }

    private String pattern(ImagePaint p) {
        Raster r = p.getImage();
        if (r == null) {
            return null;
        }
        String id = id("pat");
        Matrix t = p.getTransform();
        defs.append("<pattern id=\"").append(id).append("\" patternUnits=\"userSpaceOnUse\"");
        if (p.isObjectBoundingBox()) {
            defs.append(" patternContentUnits=\"objectBoundingBox\"");
        }
        defs.append(" width=\"").append(p.isTiled() ? String.valueOf(r.getWidth()) : "100000").append("\" height=\"")
                .append(p.isTiled() ? String.valueOf(r.getHeight()) : "100000").append('"');
        if (!t.isIdentity()) {
            defs.append(" patternTransform=\"").append(matrix(t)).append('"');
        }
        defs.append("><image width=\"").append(r.getWidth()).append("\" height=\"").append(r.getHeight())
                .append("\" href=\"data:image/png;base64,").append(Base64.encodeNoNewline(ArtImages.encodePng(r)))
                .append("\"/></pattern>\n");
        return id;
    }

    private void text(TextNode t, StringBuilder sb) {
        List<TextRun> runs = t.getRuns();
        if (runs.isEmpty()) {
            return;
        }
        sb.append("<text");
        common(t, sb);
        sb.append(" x=\"").append(num(t.getX())).append("\" y=\"").append(num(t.getY())).append('"');
        if (t.getAlign() == TextNode.ALIGN_CENTER) {
            sb.append(" text-anchor=\"middle\"");
        } else if (t.getAlign() == TextNode.ALIGN_RIGHT) {
            sb.append(" text-anchor=\"end\"");
        }
        sb.append(" xml:space=\"preserve\">");
        boolean newLine = false;
        for (TextRun r : runs) {
            String[] parts = splitLines(r.getText());
            for (int k = 0; k < parts.length; k++) {
                if (k > 0) {
                    newLine = true;
                }
                sb.append("<tspan");
                if (newLine) {
                    sb.append(" x=\"").append(num(t.getX())).append("\" dy=\"")
                            .append(num(t.maxSize() * t.getLineHeight())).append('"');
                    newLine = false;
                }
                if (r.getFontFamily() != null) {
                    sb.append(" font-family=\"").append(esc(r.getFontFamily())).append('"');
                }
                sb.append(" font-size=\"").append(num(r.getSize())).append('"');
                if (r.isBold()) {
                    sb.append(" font-weight=\"bold\"");
                }
                if (r.isItalic()) {
                    sb.append(" font-style=\"italic\"");
                }
                if (r.isUnderline() || r.isStrikethrough()) {
                    sb.append(" text-decoration=\"").append(r.isUnderline() ? "underline " : "")
                            .append(r.isStrikethrough() ? "line-through" : "").append('"');
                }
                if (r.getLetterSpacing() != 0) {
                    sb.append(" letter-spacing=\"").append(num(r.getLetterSpacing())).append('"');
                }
                if (r.getFill() != null) {
                    paintAttr("fill", r.getFill(), sb, t.getLocalBounds());
                }
                sb.append('>').append(esc(parts[k])).append("</tspan>");
            }
        }
        sb.append("</text>\n");
    }

    private static String[] splitLines(String s) {
        List<String> out = new ArrayList<String>();
        int st = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == '\n') {
                out.add(s.substring(st, i));
                st = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }
}
