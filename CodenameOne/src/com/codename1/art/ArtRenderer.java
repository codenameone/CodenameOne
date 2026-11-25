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
import com.codename1.art.effects.EffectContext;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.GeneralPath;

import java.util.List;

/// Paints an `ArtDocument` into pixels.
///
/// Everything is rasterized in software, so blend modes, masks and effects look the same on
/// every port and render in tests without a display. Text is the one exception: it is drawn
/// with the platform's fonts, so it needs an initialized display and is skipped without one.
public final class ArtRenderer {
    private double tolerance = 0.2;

    /// Renders a whole document.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document
    /// - `scale`: device pixels per document unit
    ///
    /// #### Returns
    ///
    /// a raster of the canvas, `ceil(width * scale)` by `ceil(height * scale)`
    public static Raster render(ArtDocument doc, double scale) {
        int w = (int) Math.ceil(doc.getWidth() * scale);
        int h = (int) Math.ceil(doc.getHeight() * scale);
        return new ArtRenderer().render(doc, Matrix.scale(scale, scale), new int[]{0, 0, w, h});
    }

    /// Renders a whole document to an `Image`.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document
    /// - `scale`: device pixels per document unit
    public static Image toImage(ArtDocument doc, double scale) {
        Raster r = render(doc, scale);
        return Image.createImage(r.getPixels(), r.getWidth(), r.getHeight());
    }

    /// Renders through a view transform into a device rectangle.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document
    /// - `view`: maps document coordinates to device pixels
    /// - `clip`: the device rectangle to produce, as `x, y, w, h`
    public Raster render(ArtDocument doc, Matrix view, int[] clip) {
        Raster target = new Raster(clip[0], clip[1], Math.max(0, clip[2]), Math.max(0, clip[3]));
        renderNode(doc.getRoot(), view, target, clip, 0);
        return target;
    }

    /// Renders one node (and its children) onto `target`.
    ///
    /// #### Parameters
    ///
    /// - `node`: the node
    /// - `parent`: the transform from the node's parent space to device pixels
    /// - `target`: what is painted so far; the node is composited onto it
    public void renderNode(ArtNode node, Matrix parent, Raster target) {
        renderNode(node, parent, target,
                new int[]{target.getX(), target.getY(), target.getWidth(), target.getHeight()}, 0);
    }

    private void renderNode(ArtNode node, Matrix parent, Raster target, int[] clip, int depth) {
        if (!node.isVisible() || node.getOpacity() <= 0 || depth > 256) {
            return;
        }
        Matrix m = parent.multiply(node.getTransform());
        if (node instanceof AdjustmentNode) {
            Effect adj = ((AdjustmentNode) node).getAdjustment();
            if (adj != null && adj.isEnabled()) {
                Raster adjusted = adj.apply(target.copy(), new EffectContext(m.meanScale(), target));
                target.composite(adjusted, node.getBlendMode(), node.getOpacity());
            }
            return;
        }
        boolean isolate = needsLayer(node);
        if (!isolate && node instanceof GroupNode) {
            GroupNode g = (GroupNode) node;
            int[] c = clip;
            if (g.getFrame() != null) {
                c = paintFrame(g, m, target, clip);
                if (c == null) {
                    return;
                }
            }
            List<ArtNode> ch = g.getChildren();
            for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                renderNode(ch.get(i), m, target, c, depth + 1);
            }
            return;
        }
        int pad = 0;
        List<Effect> fx = node.getEffects();
        double s = m.meanScale();
        for (int i = 0; i < fx.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            pad += fx.get(i).padding(s);
        }
        int[] lc = layerClip(node, m, clip, pad);
        if (lc == null) {
            return;
        }
        Raster layer = new Raster(lc[0], lc[1], lc[2], lc[3]);
        paintContent(node, m, layer, lc, depth);
        if (node.getClipPath() != null) {
            Mask cm = Rasterizer.fill(Polylines.flatten(node.getClipPath(), m, tolerance), lc);
            layer.applyMask(full(cm, lc));
        }
        if (node.getMask() != null) {
            Raster mr = new Raster(lc[0], lc[1], lc[2], lc[3]);
            renderNode(node.getMask(), m, mr, lc, depth + 1);
            byte[] mk = new byte[lc[2] * lc[3]];
            int[] mp = mr.getPixels();
            boolean lum = node.isLuminanceMask();
            for (int i = 0; i < mk.length; i++) {
                int p = mp[i];
                int a = p >>> 24;
                if (lum) {
                    int l = (int) (0.2125 * ((p >> 16) & 0xff) + 0.7154 * ((p >> 8) & 0xff) + 0.0721 * (p & 0xff));
                    a = (a * l + 127) / 255;
                }
                mk[i] = (byte) a;
            }
            layer.applyMask(mk);
        }
        Raster out = layer;
        if (!fx.isEmpty()) {
            EffectContext ctx = new EffectContext(s, target);
            for (int i = 0; i < fx.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                out = fx.get(i).apply(out, ctx);
            }
        }
        if (node.isKnockout()) {
            knockOut(out, target);
        }
        target.composite(out, node.getBlendMode(), node.getOpacity());
    }

    /// Erases `target` where `layer` is opaque (destination-out), so the layer is drawn over
    /// its group's backdrop instead of the layers below it.
    private static void knockOut(Raster layer, Raster target) {
        int[] lp = layer.getPixels();
        int[] tp = target.getPixels();
        int lw = layer.getWidth();
        int tw = target.getWidth();
        int ox = layer.getX() - target.getX();
        int oy = layer.getY() - target.getY();
        for (int y = 0; y < layer.getHeight(); y++) {
            int ty = y + oy;
            if (ty < 0 || ty >= target.getHeight()) {
                continue;
            }
            for (int x = 0; x < lw; x++) {
                int tx = x + ox;
                int a = lp[y * lw + x] >>> 24;
                if (a == 0 || tx < 0 || tx >= tw) {
                    continue;
                }
                int i = ty * tw + tx;
                int ta = ((tp[i] >>> 24) * (255 - a) + 127) / 255;
                tp[i] = (ta << 24) | (tp[i] & 0xffffff);
            }
        }
    }

    private static byte[] full(Mask m, int[] lc) {
        byte[] b = new byte[lc[2] * lc[3]];
        for (int y = 0; y < m.h; y++) {
            int ty = m.y + y - lc[1];
            if (ty < 0 || ty >= lc[3]) {
                continue;
            }
            for (int x = 0; x < m.w; x++) {
                int tx = m.x + x - lc[0];
                if (tx >= 0 && tx < lc[2]) {
                    b[ty * lc[2] + tx] = m.a[y * m.w + x];
                }
            }
        }
        return b;
    }

    private static boolean needsLayer(ArtNode n) {
        if (n.isKnockout()) {
            return true;
        }
        if (n.getBlendMode() != BlendMode.NORMAL || n.getMask() != null || n.getClipPath() != null) {
            return true;
        }
        if (!n.getEffects().isEmpty()) {
            return true;
        }
        if (n instanceof GroupNode) {
            return n.getOpacity() < 1 || !((GroupNode) n).isPassThrough();
        }
        return true;
    }

    private int[] layerClip(ArtNode node, Matrix m, int[] clip, int pad) {
        double[] b = ArtNode.transformBounds(node.getLocalBounds(), m);
        if (b == null) {
            return null;
        }
        int x0 = (int) Math.floor(b[0]) - pad - 1;
        int y0 = (int) Math.floor(b[1]) - pad - 1;
        int x1 = (int) Math.ceil(b[0] + b[2]) + pad + 1;
        int y1 = (int) Math.ceil(b[1] + b[3]) + pad + 1;
        x0 = Math.max(x0, clip[0]);
        y0 = Math.max(y0, clip[1]);
        x1 = Math.min(x1, clip[0] + clip[2]);
        y1 = Math.min(y1, clip[1] + clip[3]);
        if (x1 <= x0 || y1 <= y0) {
            return null;
        }
        long px = (long) (x1 - x0) * (y1 - y0);
        if (px > Raster.MAX_PIXELS) {
            return null;
        }
        return new int[]{x0, y0, x1 - x0, y1 - y0};
    }

    private int[] paintFrame(GroupNode g, Matrix m, Raster target, int[] clip) {
        double[] f = g.getFrame();
        GeneralPath fp = Shapes.roundRect(f[0], f[1], f[2], f[3], 0, 0);
        if (g.getBackground() != null && g.getBackground().isEnabled()) {
            Mask mk = Rasterizer.fill(Polylines.flatten(fp, m, tolerance), clip);
            target.composite(g.getBackground().shade(mk, m, f), g.getBackground().getBlendMode(), 1);
        }
        if (!g.isClipToFrame()) {
            return clip;
        }
        double[] b = ArtNode.transformBounds(f, m);
        int x0 = Math.max(clip[0], (int) Math.floor(b[0]));
        int y0 = Math.max(clip[1], (int) Math.floor(b[1]));
        int x1 = Math.min(clip[0] + clip[2], (int) Math.ceil(b[0] + b[2]));
        int y1 = Math.min(clip[1] + clip[3], (int) Math.ceil(b[1] + b[3]));
        if (x1 <= x0 || y1 <= y0) {
            return null;
        }
        return new int[]{x0, y0, x1 - x0, y1 - y0};
    }

    private void paintContent(ArtNode node, Matrix m, Raster layer, int[] lc, int depth) {
        if (node instanceof GroupNode) {
            GroupNode g = (GroupNode) node;
            int[] c = lc;
            if (g.getFrame() != null) {
                c = paintFrame(g, m, layer, lc);
                if (c == null) {
                    return;
                }
            }
            List<ArtNode> ch = g.getChildren();
            for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                renderNode(ch.get(i), m, layer, c, depth + 1);
            }
        } else if (node instanceof ShapeNode) {
            paintShape((ShapeNode) node, m, layer, lc);
        } else if (node instanceof RasterNode) {
            Raster img = ((RasterNode) node).getImage();
            if (img != null) {
                layer.composite(img.transform(m, lc), BlendMode.NORMAL, 1);
            }
        } else if (node instanceof TextNode) {
            paintText((TextNode) node, m, layer, lc);
        }
    }

    private void paintShape(ShapeNode n, Matrix m, Raster layer, int[] lc) {
        Polylines flat = Polylines.flatten(n.getPath(), m, tolerance);
        float[] fb = n.getPath().getBounds2D();
        double[] bbox = {fb[0], fb[1], fb[2], fb[3]};
        Mask fill = null;
        List<ArtPaint> fills = n.getFills();
        for (ArtPaint p : fills) {
            if (!p.isEnabled()) {
                continue;
            }
            if (fill == null) {
                fill = Rasterizer.fill(flat, lc);
            }
            layer.composite(p.shade(fill, m, bbox), p.getBlendMode(), 1);
        }
        List<StrokeStyle> strokes = n.getStrokes();
        for (StrokeStyle st : strokes) {
            if (!st.isEnabled() || st.getPaint() == null || !st.getPaint().isEnabled() || st.getWidth() <= 0) {
                continue;
            }
            Mask sm;
            int align = st.getAlignment();
            if (align == StrokeStyle.ALIGN_CENTER) {
                sm = Rasterizer.fill(Stroker.stroke(flat, st, m.meanScale()), lc);
            } else {
                StrokeStyle wide = st.copy();
                wide.setWidth(st.getWidth() * 2);
                sm = Rasterizer.fill(Stroker.stroke(flat, wide, m.meanScale()), lc);
                if (fill == null) {
                    fill = Rasterizer.fill(flat, lc);
                }
                if (align == StrokeStyle.ALIGN_INSIDE) {
                    sm.intersect(fill);
                } else {
                    sm.subtract(fill);
                }
            }
            layer.composite(st.getPaint().shade(sm, m, bbox), st.getPaint().getBlendMode(), 1);
        }
    }

    private void paintText(TextNode n, Matrix m, Raster layer, int[] lc) {
        if (!ArtFonts.available()) {
            return;
        }
        double s = Math.max(0.01, m.meanScale());
        List<TextLayout.Line> lines = TextLayout.layout(n, s);
        Matrix fromScaled = m.multiply(Matrix.scale(1 / s, 1 / s));
        for (TextLayout.Line l : lines) {
            for (int j = 0; j < l.pieces.size(); j++) {
                TextLayout.Piece p = l.pieces.get(j);
                String text = p.text;
                if (text.trim().length() == 0) {
                    continue;
                }
                float px = (float) Math.max(1, p.run.getSize() * s);
                Font f = ArtFonts.font(p.run, px);
                if (f == null) {
                    continue;
                }
                int w = Math.max(1, (int) Math.ceil(p.width * s) + 4);
                int h = Math.max(1, f.getHeight() + 2);
                if ((long) w * h > Raster.MAX_PIXELS) {
                    continue;
                }
                Image img = Image.createImage(w, h, 0xff000000);
                Graphics g = img.getGraphics();
                g.setAntiAliasedText(true);
                g.setColor(0xffffff);
                g.setFont(f);
                float ls = p.run.getLetterSpacing() * (float) s;
                if (ls == 0) {
                    g.drawString(text, 1, 1);
                } else {
                    int cx = 1;
                    for (int k = 0; k < text.length(); k++) {
                        String ch = text.substring(k, k + 1);
                        g.drawString(ch, cx, 1);
                        cx += f.stringWidth(ch) + Math.round(ls);
                    }
                }
                int asc = f.getAscent();
                if (p.run.isUnderline()) {
                    g.fillRect(1, 1 + asc + 1, w - 2, Math.max(1, h / 16));
                }
                if (p.run.isStrikethrough()) {
                    g.fillRect(1, 1 + asc * 2 / 3, w - 2, Math.max(1, h / 16));
                }
                int[] rgb = img.getRGB();
                int ox = (int) Math.floor(p.x * s) - 1;
                int oy = (int) Math.round(l.baseline * s) - asc - 1;
                Mask mk = new Mask(ox, oy, w, h);
                for (int k = 0; k < rgb.length && k < mk.a.length; k++) {
                    mk.a[k] = (byte) ((rgb[k] >> 8) & 0xff);
                }
                ArtPaint paint = p.run.getFill() == null ? new SolidPaint(0xff000000) : p.run.getFill();
                Raster colored = paint.shade(mk, Matrix.scale(s, s), n.getLocalBounds());
                layer.composite(colored.transform(fromScaled, lc), BlendMode.NORMAL, 1);
            }
        }
    }
}
