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
package com.codename1.pdf.ui;

import com.codename1.impl.pdf.PdfDevice;
import com.codename1.impl.pdf.PdfPath;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import java.util.ArrayList;
import java.util.List;

/// Draws a page on a Codename One [Graphics], in device pixels. The
/// graphics clip is replaced, not intersected, by a shape clip, so the
/// device keeps the current clip itself: a rectangular clip is
/// intersected exactly and a non-rectangular one replaces the clip
/// within its bounds.
final class GraphicsPdfDevice implements PdfDevice {
    private Graphics g;
    private Image surface;
    private int originX;
    private int originY;
    private final List<Object[]> layers = new ArrayList<Object[]>();
    private final boolean shapes;
    private final boolean shapeClip;
    private final boolean transforms;
    private final List<Object> stack = new ArrayList<Object>();
    private Object clip;

    /// A device drawing on `g`. `surface` is the image `g` draws on, when
    /// there is one, so blend modes can read back the backdrop.
    GraphicsPdfDevice(Graphics g, Image surface, int width, int height) {
        this.g = g;
        this.surface = surface;
        shapes = g.isShapeSupported();
        shapeClip = g.isShapeClipSupported();
        transforms = g.isTransformSupported();
        int[] full = new int[] {0, 0, width, height};
        clip = full;
        g.setAntiAliased(true);
        g.setAntiAliasedText(true);
    }

    @Override
    public void beginLayer(int x, int y, int width, int height) {
        layers.add(new Object[] {g, surface, Integer.valueOf(originX), Integer.valueOf(originY)});
        Image layer = Image.createImage(Math.max(1, width), Math.max(1, height), 0);
        surface = layer;
        g = layer.getGraphics();
        g.setAntiAliased(true);
        g.setAntiAliasedText(true);
        originX = x;
        originY = y;
        g.translate(-x, -y);
        apply();
    }

    @Override
    public int[] endLayer() {
        int[] px = surface.getRGB();
        Object[] top = layers.remove(layers.size() - 1);
        g = (Graphics) top[0];
        surface = (Image) top[1];
        originX = ((Integer) top[2]).intValue();
        originY = ((Integer) top[3]).intValue();
        apply();
        return px;
    }

    @Override
    public int[] pixels(int x, int y, int width, int height) {
        if (surface == null) {
            return null;
        }
        int sw = surface.getWidth();
        int sh = surface.getHeight();
        int[] all = surface.getRGB();
        int[] out = new int[width * height];
        for (int row = 0; row < height; row++) {
            int sy = y - originY + row;
            if (sy < 0 || sy >= sh) {
                continue;
            }
            for (int col = 0; col < width; col++) {
                int sx = x - originX + col;
                if (sx >= 0 && sx < sw) {
                    out[row * width + col] = all[sy * sw + sx];
                }
            }
        }
        return out;
    }

    @Override
    public void save() {
        stack.add(clip);
    }

    @Override
    public void restore() {
        if (!stack.isEmpty()) {
            clip = stack.remove(stack.size() - 1);
            apply();
        }
    }

    private void apply() {
        if (clip instanceof int[]) {
            int[] c = (int[]) clip;
            g.setClip(c[0], c[1], c[2], c[3]);
        } else {
            g.setClip((GeneralPath) clip);
        }
    }

    private int[] bounds() {
        if (clip instanceof int[]) {
            return (int[]) clip;
        }
        Rectangle r = ((GeneralPath) clip).getBounds();
        return new int[] {r.getX(), r.getY(), r.getWidth(), r.getHeight()};
    }

    @Override
    public void clip(PdfPath path, boolean evenOdd) {
        float[] b = path.getBounds();
        if (b == null) {
            clip = new int[] {0, 0, 0, 0};
            apply();
            return;
        }
        int[] cur = bounds();
        int x0 = Math.max(cur[0], (int) Math.floor(b[0]));
        int y0 = Math.max(cur[1], (int) Math.floor(b[1]));
        int x1 = Math.min(cur[0] + cur[2], (int) Math.ceil(b[2]));
        int y1 = Math.min(cur[1] + cur[3], (int) Math.ceil(b[3]));
        int[] rect = {x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0)};
        if (isRectangle(path) || !shapeClip || !(clip instanceof int[])) {
            clip = rect;
        } else {
            GeneralPath p = toPath(path, evenOdd);
            clip = p;
        }
        apply();
    }

    private static boolean isRectangle(PdfPath p) {
        int n = p.getOpCount();
        if (n < 4 || n > 6 || p.getOp(0) != PdfPath.MOVE) {
            return false;
        }
        for (int i = 1; i < n; i++) {
            int op = p.getOp(i);
            if (op != PdfPath.LINE && op != PdfPath.CLOSE) {
                return false;
            }
        }
        float[] pt = p.getPoints();
        int points = 0;
        for (int i = 0; i < n; i++) {
            if (p.getOp(i) != PdfPath.CLOSE) {
                points++;
            }
        }
        for (int i = 0; i < points; i++) {
            int j = (i + 1) % points;
            if (pt[i * 2] != pt[j * 2] && pt[i * 2 + 1] != pt[j * 2 + 1]) {
                return false;
            }
        }
        return true;
    }

    private static GeneralPath toPath(PdfPath p, boolean evenOdd) {
        GeneralPath gp = new GeneralPath();
        gp.setWindingRule(evenOdd ? GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO);
        float[] pt = p.getPoints();
        int pi = 0;
        int n = p.getOpCount();
        for (int i = 0; i < n; i++) {
            switch (p.getOp(i)) {
                case PdfPath.MOVE:
                    gp.moveTo(pt[pi], pt[pi + 1]);
                    pi += 2;
                    break;
                case PdfPath.LINE:
                    gp.lineTo(pt[pi], pt[pi + 1]);
                    pi += 2;
                    break;
                case PdfPath.QUAD:
                    gp.quadTo(pt[pi], pt[pi + 1], pt[pi + 2], pt[pi + 3]);
                    pi += 4;
                    break;
                case PdfPath.CUBIC:
                    gp.curveTo(pt[pi], pt[pi + 1], pt[pi + 2], pt[pi + 3], pt[pi + 4], pt[pi + 5]);
                    pi += 6;
                    break;
                default:
                    gp.closePath();
                    break;
            }
        }
        return gp;
    }

    private void color(int argb) {
        g.setColor(argb & 0xffffff);
        g.setAlpha(argb >>> 24);
    }

    @Override
    public void fill(PdfPath path, boolean evenOdd, int argb) {
        if ((argb >>> 24) == 0) {
            return;
        }
        color(argb);
        if (shapes) {
            g.fillShape(toPath(path, evenOdd));
        } else {
            float[] b = path.getBounds();
            if (b != null) {
                g.fillRect((int) b[0], (int) b[1], (int) Math.ceil(b[2] - b[0]), (int) Math.ceil(b[3] - b[1]));
            }
        }
        g.setAlpha(255);
    }

    @Override
    public void stroke(PdfPath path, float width, int cap, int join, float miterLimit, int argb) {
        if ((argb >>> 24) == 0) {
            return;
        }
        color(argb);
        if (shapes) {
            // The PDF and Stroke cap/join constants are numbered alike.
            g.drawShape(toPath(path, false), new Stroke(width, cap, join, Math.max(1, miterLimit)));
        } else {
            float[] b = path.getBounds();
            if (b != null) {
                g.drawRect((int) b[0], (int) b[1], (int) (b[2] - b[0]), (int) (b[3] - b[1]));
            }
        }
        g.setAlpha(255);
    }

    @Override
    public void image(int[] argb, int w, int h, double[] m) {
        Image img = Image.createImage(argb, w, h);
        if (transforms && (m[1] != 0 || m[2] != 0 || m[0] < 0 || m[3] < 0)) {
            Transform old = g.getTransform();
            Transform t = old.copy();
            t.concatenate(Transform.makeAffine(m[0], m[1], m[2], m[3], m[4], m[5]));
            g.setTransform(t);
            g.drawImage(img, 0, 0);
            g.setTransform(old);
            return;
        }
        double x0 = m[4];
        double y0 = m[5];
        double x1 = m[0] * w + m[2] * h + m[4];
        double y1 = m[1] * w + m[3] * h + m[5];
        int x = (int) Math.floor(Math.min(x0, x1));
        int y = (int) Math.floor(Math.min(y0, y1));
        int dw = (int) Math.ceil(Math.max(x0, x1)) - x;
        int dh = (int) Math.ceil(Math.max(y0, y1)) - y;
        if (dw > 0 && dh > 0) {
            g.drawImage(img, x, y, dw, dh);
        }
    }

    @Override
    public int[] decodeJpeg(byte[] data, int[] size) {
        try {
            EncodedImage e = EncodedImage.create(data);
            size[0] = e.getWidth();
            size[1] = e.getHeight();
            if (size[0] <= 0 || size[1] <= 0) {
                return null;
            }
            return e.getRGB();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @Override
    public void text(String text, int family, boolean bold, boolean italic, double[] m, int argb) {
        double sx = Math.sqrt(m[0] * m[0] + m[1] * m[1]);
        double sy = Math.sqrt(m[2] * m[2] + m[3] * m[3]);
        if (sy < 1) {
            return;
        }
        int style = (bold ? Font.STYLE_BOLD : 0) | (italic ? Font.STYLE_ITALIC : 0);
        int face = family == 2 ? Font.FACE_MONOSPACE : family == 1 ? Font.FACE_PROPORTIONAL : Font.FACE_SYSTEM;
        Font f;
        float px = (float) sy;
        if (Font.isNativeFontSchemeSupported() && family != 2) {
            String name = bold ? (italic ? "native:ItalicBold" : "native:MainBold")
                    : italic ? "native:ItalicRegular" : "native:MainRegular";
            f = Font.createTrueTypeFont(name, name).derive(px, style);
        } else {
            f = Font.createSystemFont(face, style, Font.SIZE_MEDIUM);
        }
        color(argb);
        g.setFont(f);
        int ascent = f.getAscent();
        boolean axis = m[1] == 0 && m[2] == 0 && m[0] > 0 && m[3] < 0;
        if (axis || !transforms) {
            g.drawString(text, (int) Math.round(m[4]), (int) Math.round(m[5]) - ascent);
        } else {
            Transform old = g.getTransform();
            Transform t = old.copy();
            // m maps a 1-unit em, y up; draw the font at its pixel size, y down.
            t.concatenate(Transform.makeAffine(m[0] / sx, m[1] / sx, -m[2] / sy, -m[3] / sy, m[4], m[5]));
            g.setTransform(t);
            g.drawString(text, 0, -ascent);
            g.setTransform(old);
        }
        g.setAlpha(255);
    }
}
