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
package com.codename1.art.effects;

import com.codename1.art.BlendMode;
import com.codename1.art.Raster;
import com.codename1.util.MathUtil;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Runs an SVG filter: every filter primitive of SVG 1.1 plus `feDropShadow`.
///
/// Each primitive is a `Map` holding `type` (the element name) and its attributes as
/// strings, exactly as written in the SVG; `children` holds the nested elements of
/// `feMerge`, `feComponentTransfer` and the lighting primitives. Lengths are in user units.
final class FilterGraph {
    private final Raster source;
    private final EffectContext ctx;
    private final double s;
    private final Map<String, Raster> results = new HashMap<String, Raster>();
    private Raster last;
    private final boolean linearRgb;

    private FilterGraph(Raster source, EffectContext ctx, boolean linearRgb) {
        this.source = source;
        this.ctx = ctx;
        this.s = ctx.getScale();
        this.last = source;
        this.linearRgb = linearRgb;
    }

    static Raster run(Effect e, Raster layer, EffectContext ctx) {
        int pad = Effects.padding(e, ctx.getScale());
        Raster src = Pixels.expand(layer, pad);
        boolean lin = !"sRGB".equals(e.getString("colorInterpolation", "linearRGB"));
        FilterGraph g = new FilterGraph(src, ctx, lin);
        List<Map<String, Object>> prims = e.getPrimitives();
        int max = Math.min(prims.size(), 256);
        for (int i = 0; i < max; i++) {
            Map<String, Object> p = prims.get(i);
            Raster r = g.primitive(p);
            g.last = r;
            String res = str(p, "result", null);
            if (res != null) {
                g.results.put(res, r);
            }
        }
        return g.last;
    }

    static String str(Map<String, Object> p, String k, String def) {
        Object o = p.get(k);
        return o == null ? def : o.toString();
    }

    static double num(Map<String, Object> p, String k, double def) {
        Object o = p.get(k);
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        if (o == null) {
            return def;
        }
        try {
            String v = o.toString().trim();
            int sp = v.indexOf(' ');
            if (sp > 0) {
                v = v.substring(0, sp);
            }
            return Double.parseDouble(v);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    static double[] nums(Map<String, Object> p, String k) {
        Object o = p.get(k);
        if (o == null) {
            return new double[0];
        }
        String v = o.toString().replace(',', ' ').trim();
        if (v.length() == 0) {
            return new double[0];
        }
        List<Double> out = new ArrayList<Double>();
        int i = 0;
        while (i < v.length()) {
            while (i < v.length() && v.charAt(i) <= ' ') {
                i++;
            }
            int st = i;
            while (i < v.length() && v.charAt(i) > ' ') {
                i++;
            }
            if (i > st) {
                try {
                    out.add(Double.valueOf(v.substring(st, i)));
                } catch (NumberFormatException ex) {
                    return new double[0];
                }
            }
        }
        double[] r = new double[out.size()];
        for (int j = 0; j < r.length; j++) {
            r[j] = out.get(j).doubleValue();
        }
        return r;
    }

    private Raster input(Map<String, Object> p, String key) {
        String in = str(p, key, null);
        if (in == null) {
            return last;
        }
        if ("SourceGraphic".equals(in)) {
            return source;
        }
        if ("SourceAlpha".equals(in)) {
            Raster r = source.copy();
            int[] px = r.getPixels();
            for (int i = 0; i < px.length; i++) {
                px[i] = px[i] & 0xff000000;
            }
            return r;
        }
        if ("BackgroundImage".equals(in) || "BackgroundAlpha".equals(in)) {
            Raster bd = ctx.getBackdrop();
            Raster r = bd == null ? blank() : bd.resize(source.getX(), source.getY(), source.getWidth(), source.getHeight());
            if ("BackgroundAlpha".equals(in)) {
                int[] px = r.getPixels();
                for (int i = 0; i < px.length; i++) {
                    px[i] = px[i] & 0xff000000;
                }
            }
            return r;
        }
        if ("FillPaint".equals(in) || "StrokePaint".equals(in)) {
            return blank();
        }
        Raster r = results.get(in);
        return r == null ? last : r;
    }

    private Raster blank() {
        return new Raster(source.getX(), source.getY(), source.getWidth(), source.getHeight());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> children(Map<String, Object> p) {
        Object o = p.get("children");
        return o instanceof List ? (List<Map<String, Object>>) o : new ArrayList<Map<String, Object>>();
    }

    private Raster primitive(Map<String, Object> p) {
        String t = str(p, "type", "");
        if ("feGaussianBlur".equals(t)) {
            double[] sd = nums(p, "stdDeviation");
            double sx = sd.length > 0 ? sd[0] : 0;
            double sy = sd.length > 1 ? sd[1] : sx;
            Raster in = input(p, "in");
            if (sx <= 0 && sy <= 0) {
                return in.copy();
            }
            return Pixels.blurRaster(in, sx * s, sy * s).resize(in.getX(), in.getY(), in.getWidth(), in.getHeight());
        }
        if ("feOffset".equals(t)) {
            Raster in = input(p, "in");
            Raster out = blank();
            int dx = (int) Math.round(num(p, "dx", 0) * s);
            int dy = (int) Math.round(num(p, "dy", 0) * s);
            Raster moved = new Raster(in.getX() + dx, in.getY() + dy, in.getWidth(), in.getHeight(), in.getPixels());
            out.composite(moved, BlendMode.NORMAL, 1);
            return out;
        }
        if ("feFlood".equals(t)) {
            Raster out = blank();
            int c = color(str(p, "flood-color", "black"), num(p, "flood-opacity", 1));
            java.util.Arrays.fill(out.getPixels(), c);
            return out;
        }
        if ("feMerge".equals(t)) {
            Raster out = blank();
            List<Map<String, Object>> ch = children(p);
            for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                out.composite(input(ch.get(i), "in"), BlendMode.NORMAL, 1);
            }
            return out;
        }
        if ("feBlend".equals(t)) {
            Raster out = input(p, "in2").copy();
            out.composite(input(p, "in"), blendMode(str(p, "mode", "normal")), 1);
            return out;
        }
        if ("feComposite".equals(t)) {
            return composite(input(p, "in"), input(p, "in2"), str(p, "operator", "over"), p);
        }
        if ("feColorMatrix".equals(t)) {
            return colorMatrix(input(p, "in"), str(p, "matrixType", "matrix"), p);
        }
        if ("feComponentTransfer".equals(t)) {
            return transfer(input(p, "in"), children(p));
        }
        if ("feMorphology".equals(t)) {
            Raster in = input(p, "in");
            double[] rad = nums(p, "radius");
            int rx = (int) Math.round((rad.length > 0 ? rad[0] : 0) * s);
            int ry = (int) Math.round((rad.length > 1 ? rad[1] : (rad.length > 0 ? rad[0] : 0)) * s);
            boolean dilate = "dilate".equals(str(p, "operator", "erode"));
            float[][] c = Pixels.premul(in);
            for (int k = 0; k < 4; k++) {
                c[k] = Pixels.morph(c[k], in.getWidth(), in.getHeight(), rx, ry, dilate);
            }
            Raster out = blank();
            Pixels.unpremul(c, out);
            return out;
        }
        if ("feConvolveMatrix".equals(t)) {
            return convolve(input(p, "in"), p);
        }
        if ("feDisplacementMap".equals(t)) {
            return displace(input(p, "in"), input(p, "in2"), p);
        }
        if ("feTile".equals(t)) {
            Raster in = input(p, "in");
            Raster out = blank();
            double[] b = contentBounds(in);
            if (b == null) {
                return out;
            }
            int bx = (int) b[0];
            int by = (int) b[1];
            int bw = Math.max(1, (int) b[2]);
            int bh = Math.max(1, (int) b[3]);
            int[] op = out.getPixels();
            int i = 0;
            for (int y = 0; y < out.getHeight(); y++) {
                for (int x = 0; x < out.getWidth(); x++) {
                    int ax = out.getX() + x - bx;
                    int ay = out.getY() + y - by;
                    ax = ((ax % bw) + bw) % bw;
                    ay = ((ay % bh) + bh) % bh;
                    op[i++] = in.get(bx + ax, by + ay);
                }
            }
            return out;
        }
        if ("feImage".equals(t)) {
            Object img = p.get("image");
            Raster out = blank();
            if (img instanceof Raster) {
                Raster ri = (Raster) img;
                double x = num(p, "x", 0) * s;
                double y = num(p, "y", 0) * s;
                double w = num(p, "width", ri.getWidth()) * s;
                double h = num(p, "height", ri.getHeight()) * s;
                com.codename1.art.Matrix m = new com.codename1.art.Matrix(w / Math.max(1, ri.getWidth()), 0, 0,
                        h / Math.max(1, ri.getHeight()), x - ri.getX() * w / Math.max(1, ri.getWidth()),
                        y - ri.getY() * h / Math.max(1, ri.getHeight()));
                out.composite(ri.transform(m, new int[]{out.getX(), out.getY(), out.getWidth(), out.getHeight()}),
                        BlendMode.NORMAL, 1);
            }
            return out;
        }
        if ("feTurbulence".equals(t)) {
            return turbulence(p);
        }
        if ("feDiffuseLighting".equals(t) || "feSpecularLighting".equals(t)) {
            return lighting(input(p, "in"), p, "feSpecularLighting".equals(t));
        }
        if ("feDropShadow".equals(t)) {
            Raster in = input(p, "in");
            double[] sd = nums(p, "stdDeviation");
            double sx = sd.length > 0 ? sd[0] : 2;
            float[] a = Pixels.alpha(in);
            int w = in.getWidth();
            int h = in.getHeight();
            a = Pixels.shift(a, w, h, (int) Math.round(num(p, "dx", 2) * s), (int) Math.round(num(p, "dy", 2) * s), 0);
            Pixels.blur(a, w, h, sx * s, (sd.length > 1 ? sd[1] : sx) * s);
            Raster out = Pixels.fromAlpha(a, color(str(p, "flood-color", "black"), num(p, "flood-opacity", 1)),
                    in.getX(), in.getY(), w, h, 1);
            out.composite(in, BlendMode.NORMAL, 1);
            return out;
        }
        return input(p, "in").copy();
    }

    private static int color(String c, double opacity) {
        int v = com.codename1.art.Colors.parse(c, 0xff000000);
        int a = (int) ((v >>> 24) * Math.max(0, Math.min(1, opacity)) + 0.5);
        return (a << 24) | (v & 0xffffff);
    }

    static BlendMode blendMode(String m) {
        String[] names = {"multiply", "screen", "overlay", "darken", "lighten", "color-dodge", "color-burn",
            "hard-light", "soft-light", "difference", "exclusion", "hue", "saturation", "color", "luminosity"};
        BlendMode[] modes = {BlendMode.MULTIPLY, BlendMode.SCREEN, BlendMode.OVERLAY, BlendMode.DARKEN,
            BlendMode.LIGHTEN, BlendMode.COLOR_DODGE, BlendMode.COLOR_BURN, BlendMode.HARD_LIGHT,
            BlendMode.SOFT_LIGHT, BlendMode.DIFFERENCE, BlendMode.EXCLUSION, BlendMode.HUE,
            BlendMode.SATURATION, BlendMode.COLOR, BlendMode.LUMINOSITY};
        for (int i = 0; i < names.length; i++) {
            if (names[i].equalsIgnoreCase(m)) {
                return modes[i];
            }
        }
        return BlendMode.NORMAL;
    }

    private Raster composite(Raster a, Raster b, String op, Map<String, Object> p) {
        Raster out = blank();
        int[] o = out.getPixels();
        double k1 = num(p, "k1", 0);
        double k2 = num(p, "k2", 0);
        double k3 = num(p, "k3", 0);
        double k4 = num(p, "k4", 0);
        int i = 0;
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                int ax = out.getX() + x;
                int ay = out.getY() + y;
                int pa = a.get(ax, ay);
                int pb = b.get(ax, ay);
                double aa = (pa >>> 24) / 255.0;
                double ab = (pb >>> 24) / 255.0;
                double fa;
                double fb;
                if ("in".equals(op)) {
                    fa = ab;
                    fb = 0;
                } else if ("out".equals(op)) {
                    fa = 1 - ab;
                    fb = 0;
                } else if ("atop".equals(op)) {
                    fa = ab;
                    fb = 1 - aa;
                } else if ("xor".equals(op)) {
                    fa = 1 - ab;
                    fb = 1 - aa;
                } else if ("arithmetic".equals(op)) {
                    double[] res = new double[4];
                    for (int c = 0; c < 4; c++) {
                        int sh = c == 0 ? 24 : 24 - c * 8;
                        double ca = c == 0 ? aa : ((pa >> sh) & 0xff) / 255.0 * aa;
                        double cb = c == 0 ? ab : ((pb >> sh) & 0xff) / 255.0 * ab;
                        res[c] = Math.max(0, Math.min(1, k1 * ca * cb + k2 * ca + k3 * cb + k4));
                    }
                    double ra = res[0];
                    if (ra <= 0) {
                        o[i++] = 0;
                        continue;
                    }
                    o[i++] = Pixels.pack(Pixels.to8((float) ra), (float) Math.min(1, res[1] / ra),
                            (float) Math.min(1, res[2] / ra), (float) Math.min(1, res[3] / ra));
                    continue;
                } else {
                    fa = 1;
                    fb = 1 - aa;
                }
                double ao = aa * fa + ab * fb;
                if (ao <= 0) {
                    o[i++] = 0;
                    continue;
                }
                float[] ch = new float[3];
                for (int c = 0; c < 3; c++) {
                    int sh = 16 - c * 8;
                    double ca = ((pa >> sh) & 0xff) / 255.0 * aa;
                    double cb = ((pb >> sh) & 0xff) / 255.0 * ab;
                    ch[c] = (float) ((ca * fa + cb * fb) / ao);
                }
                o[i++] = Pixels.pack(Pixels.to8((float) ao), ch[0], ch[1], ch[2]);
            }
        }
        return out;
    }

    private Raster colorMatrix(Raster in, String type, Map<String, Object> p) {
        float[] m;
        double[] v = nums(p, "values");
        if ("saturate".equals(type)) {
            float sv = (float) (v.length > 0 ? v[0] : 1);
            m = new float[]{
                0.213f + 0.787f * sv, 0.715f - 0.715f * sv, 0.072f - 0.072f * sv, 0, 0,
                0.213f - 0.213f * sv, 0.715f + 0.285f * sv, 0.072f - 0.072f * sv, 0, 0,
                0.213f - 0.213f * sv, 0.715f - 0.715f * sv, 0.072f + 0.928f * sv, 0, 0,
                0, 0, 0, 1, 0};
        } else if ("hueRotate".equals(type)) {
            double a = v.length > 0 ? Math.toRadians(v[0]) : 0;
            float c = (float) Math.cos(a);
            float sn = (float) Math.sin(a);
            m = new float[]{
                0.213f + c * 0.787f - sn * 0.213f, 0.715f - c * 0.715f - sn * 0.715f, 0.072f - c * 0.072f + sn * 0.928f, 0, 0,
                0.213f - c * 0.213f + sn * 0.143f, 0.715f + c * 0.285f + sn * 0.140f, 0.072f - c * 0.072f - sn * 0.283f, 0, 0,
                0.213f - c * 0.213f - sn * 0.787f, 0.715f - c * 0.715f + sn * 0.715f, 0.072f + c * 0.928f + sn * 0.072f, 0, 0,
                0, 0, 0, 1, 0};
        } else if ("luminanceToAlpha".equals(type)) {
            m = new float[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.2125f, 0.7154f, 0.0721f, 0, 0};
        } else {
            if (v.length < 20) {
                return in.copy();
            }
            m = new float[20];
            for (int i = 0; i < 20; i++) {
                m[i] = (float) v[i];
            }
        }
        Raster out = in.copy();
        if (linearRgb) {
            toLinear(out.getPixels());
        }
        Adjust.apply(new Effect(Effect.COLOR_MATRIX).set("matrix", m), out.getPixels());
        if ("luminanceToAlpha".equals(type)) {
            int[] px = out.getPixels();
            int[] ip = in.getPixels();
            for (int i = 0; i < px.length; i++) {
                if ((ip[i] >>> 24) == 0) {
                    px[i] = 0;
                }
            }
        }
        if (linearRgb) {
            toSrgb(out.getPixels());
        }
        return out;
    }

    private static final float[] TO_LINEAR = new float[256];

    static {
        for (int i = 0; i < 256; i++) {
            double c = i / 255.0;
            TO_LINEAR[i] = (float) (c <= 0.04045 ? c / 12.92 : MathUtil.pow((c + 0.055) / 1.055, 2.4));
        }
    }

    static void toLinear(int[] px) {
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            px[i] = Pixels.pack(p >>> 24, TO_LINEAR[(p >> 16) & 0xff], TO_LINEAR[(p >> 8) & 0xff], TO_LINEAR[p & 0xff]);
        }
    }

    static void toSrgb(int[] px) {
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            px[i] = Pixels.pack(p >>> 24, srgb(((p >> 16) & 0xff) / 255f), srgb(((p >> 8) & 0xff) / 255f),
                    srgb((p & 0xff) / 255f));
        }
    }

    private static float srgb(float c) {
        return (float) (c <= 0.0031308 ? c * 12.92 : 1.055 * MathUtil.pow(c, 1 / 2.4) - 0.055);
    }

    private Raster transfer(Raster in, List<Map<String, Object>> funcs) {
        int[][] lut = new int[4][256];
        String[] names = {"feFuncA", "feFuncR", "feFuncG", "feFuncB"};
        for (int c = 0; c < 4; c++) {
            Map<String, Object> f = null;
            for (int i = 0; i < funcs.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                if (names[c].equals(str(funcs.get(i), "type", ""))) {
                    f = funcs.get(i);
                }
            }
            for (int i = 0; i < 256; i++) {
                double v = i / 255.0;
                if (f != null) {
                    v = func(f, v);
                }
                lut[c][i] = Pixels.to8((float) v);
            }
        }
        Raster out = in.copy();
        int[] px = out.getPixels();
        if (linearRgb) {
            toLinear(px);
        }
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            px[i] = (lut[0][p >>> 24] << 24) | (lut[1][(p >> 16) & 0xff] << 16)
                    | (lut[2][(p >> 8) & 0xff] << 8) | lut[3][p & 0xff];
        }
        if (linearRgb) {
            toSrgb(px);
        }
        return out;
    }

    private static double func(Map<String, Object> f, double c) {
        String ft = str(f, "funcType", "identity");
        double[] tv = nums(f, "tableValues");
        if ("table".equals(ft) && tv.length > 0) {
            int n = tv.length - 1;
            if (n == 0) {
                return tv[0];
            }
            int k = (int) Math.min(n - 1, Math.floor(c * n));
            return tv[k] + (c - (double) k / n) * n * (tv[k + 1] - tv[k]);
        }
        if ("discrete".equals(ft) && tv.length > 0) {
            int n = tv.length;
            int k = (int) Math.min(n - 1, Math.floor(c * n));
            return tv[k];
        }
        if ("linear".equals(ft)) {
            return num(f, "slope", 1) * c + num(f, "intercept", 0);
        }
        if ("gamma".equals(ft)) {
            return num(f, "amplitude", 1) * MathUtil.pow(c, num(f, "exponent", 1)) + num(f, "offset", 0);
        }
        return c;
    }

    private Raster convolve(Raster in, Map<String, Object> p) {
        double[] order = nums(p, "order");
        int ox = order.length > 0 ? (int) order[0] : 3;
        int oy = order.length > 1 ? (int) order[1] : ox;
        if (ox < 1 || oy < 1 || ox > 32 || oy > 32) {
            return in.copy();
        }
        double[] k = nums(p, "kernelMatrix");
        if (k.length != ox * oy) {
            return in.copy();
        }
        double sum = 0;
        for (int i = 0; i < k.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            sum += k[i];
        }
        double div = num(p, "divisor", sum == 0 ? 1 : sum);
        if (div == 0) {
            div = 1;
        }
        double bias = num(p, "bias", 0);
        int tx = (int) num(p, "targetX", Math.floor(ox / 2.0));
        int ty = (int) num(p, "targetY", Math.floor(oy / 2.0));
        boolean keepAlpha = "true".equals(str(p, "preserveAlpha", "false"));
        String edge = str(p, "edgeMode", "duplicate");
        float[][] c = Pixels.premul(in);
        int w = in.getWidth();
        int h = in.getHeight();
        float[][] o = new float[4][c[0].length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                for (int ch = 0; ch < 4; ch++) {
                    if (ch == 0 && keepAlpha) {
                        o[0][y * w + x] = c[0][y * w + x];
                        continue;
                    }
                    double acc = 0;
                    for (int j = 0; j < oy; j++) {
                        for (int i = 0; i < ox; i++) {
                            int sx = x - tx + i;
                            int sy = y - ty + j;
                            float v;
                            if (sx < 0 || sy < 0 || sx >= w || sy >= h) {
                                if ("wrap".equals(edge)) {
                                    sx = ((sx % w) + w) % w;
                                    sy = ((sy % h) + h) % h;
                                    v = c[ch][sy * w + sx];
                                } else if ("none".equals(edge)) {
                                    v = 0;
                                } else {
                                    sx = Math.max(0, Math.min(w - 1, sx));
                                    sy = Math.max(0, Math.min(h - 1, sy));
                                    v = c[ch][sy * w + sx];
                                }
                            } else {
                                v = c[ch][sy * w + sx];
                            }
                            acc += v * k[(oy - 1 - j) * ox + (ox - 1 - i)];
                        }
                    }
                    o[ch][y * w + x] = (float) (acc / div + bias);
                }
            }
        }
        for (int i = 0; i < o[0].length; i++) {
            o[0][i] = Pixels.clamp01(o[0][i]);
            for (int ch = 1; ch < 4; ch++) {
                o[ch][i] = Math.max(0, Math.min(o[0][i], o[ch][i]));
            }
        }
        Raster out = blank();
        Pixels.unpremul(o, out);
        return out;
    }

    private Raster displace(Raster in, Raster map, Map<String, Object> p) {
        double scale = num(p, "scale", 0) * s;
        int xc = channel(str(p, "xChannelSelector", "A"));
        int yc = channel(str(p, "yChannelSelector", "A"));
        Raster out = blank();
        int[] o = out.getPixels();
        int i = 0;
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                int ax = out.getX() + x;
                int ay = out.getY() + y;
                int m = map.get(ax, ay);
                double dx = scale * (((m >>> xc) & 0xff) / 255.0 - 0.5);
                double dy = scale * (((m >>> yc) & 0xff) / 255.0 - 0.5);
                o[i++] = in.get((int) Math.round(ax + dx), (int) Math.round(ay + dy));
            }
        }
        return out;
    }

    private static int channel(String c) {
        if ("R".equals(c)) {
            return 16;
        }
        if ("G".equals(c)) {
            return 8;
        }
        if ("B".equals(c)) {
            return 0;
        }
        return 24;
    }

    private static double[] contentBounds(Raster r) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        int[] px = r.getPixels();
        for (int y = 0; y < r.getHeight(); y++) {
            for (int x = 0; x < r.getWidth(); x++) {
                if ((px[y * r.getWidth() + x] >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return null;
        }
        return new double[]{r.getX() + minX, r.getY() + minY, maxX - minX + 1, maxY - minY + 1};
    }

    private Raster turbulence(Map<String, Object> p) {
        double[] bf = nums(p, "baseFrequency");
        double fx = bf.length > 0 ? bf[0] : 0;
        double fy = bf.length > 1 ? bf[1] : fx;
        int oct = Math.max(1, Math.min(10, (int) num(p, "numOctaves", 1)));
        boolean fractal = "fractalNoise".equals(str(p, "noiseType", "turbulence"));
        Turbulence t = new Turbulence((int) num(p, "seed", 0));
        Raster out = blank();
        int[] o = out.getPixels();
        int i = 0;
        double[] pt = new double[2];
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                pt[0] = (out.getX() + x + 0.5) / s;
                pt[1] = (out.getY() + y + 0.5) / s;
                float[] c = new float[4];
                for (int ch = 0; ch < 4; ch++) {
                    double v = t.turbulence(ch, pt, fx, fy, oct, fractal);
                    c[ch] = (float) (fractal ? (v + 1) / 2 : v);
                }
                float a = Pixels.clamp01(c[3]);
                o[i++] = Pixels.pack(Pixels.to8(a), Pixels.clamp01(c[0]), Pixels.clamp01(c[1]), Pixels.clamp01(c[2]));
            }
        }
        if (linearRgb) {
            toSrgb(o);
        }
        return out;
    }

    private Raster lighting(Raster in, Map<String, Object> p, boolean specular) {
        double surface = num(p, "surfaceScale", 1);
        double kd = num(p, specular ? "specularConstant" : "diffuseConstant", 1);
        double exp = num(p, "specularExponent", 1);
        int lc = color(str(p, "lighting-color", "white"), 1);
        List<Map<String, Object>> ch = children(p);
        Map<String, Object> light = ch.isEmpty() ? null : ch.get(0);
        String lt = light == null ? "feDistantLight" : str(light, "type", "feDistantLight");
        int w = in.getWidth();
        int h = in.getHeight();
        float[] a = Pixels.alpha(in);
        Raster out = blank();
        int[] o = out.getPixels();
        double lr = ((lc >> 16) & 0xff) / 255.0;
        double lg = ((lc >> 8) & 0xff) / 255.0;
        double lb = (lc & 0xff) / 255.0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double nx = -surface * sobelX(a, w, h, x, y);
                double ny = -surface * sobelY(a, w, h, x, y);
                double nl = Math.sqrt(nx * nx + ny * ny + 1);
                nx /= nl;
                ny /= nl;
                double nz = 1 / nl;
                double lx;
                double ly;
                double lz;
                double factor = 1;
                if ("feDistantLight".equals(lt)) {
                    double az = Math.toRadians(light == null ? 0 : num(light, "azimuth", 0));
                    double el = Math.toRadians(light == null ? 0 : num(light, "elevation", 0));
                    lx = Math.cos(az) * Math.cos(el);
                    ly = Math.sin(az) * Math.cos(el);
                    lz = Math.sin(el);
                } else {
                    double px = num(light, "x", 0) * s;
                    double py = num(light, "y", 0) * s;
                    double pz = num(light, "z", 0) * s;
                    double sx = in.getX() + x;
                    double sy = in.getY() + y;
                    double sz = surface * a[y * w + x] * s;
                    lx = px - sx;
                    ly = py - sy;
                    lz = pz - sz;
                    double ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
                    if (ll > 0) {
                        lx /= ll;
                        ly /= ll;
                        lz /= ll;
                    }
                    if ("feSpotLight".equals(lt)) {
                        double tx = num(light, "pointsAtX", 0) * s - px;
                        double ty = num(light, "pointsAtY", 0) * s - py;
                        double tz = num(light, "pointsAtZ", 0) * s - pz;
                        double tl = Math.sqrt(tx * tx + ty * ty + tz * tz);
                        if (tl > 0) {
                            double minusLS = -(lx * tx + ly * ty + lz * tz) / tl;
                            double cone = num(light, "limitingConeAngle", 90);
                            if (minusLS <= 0 || minusLS < Math.cos(Math.toRadians(cone))) {
                                factor = 0;
                            } else {
                                factor = MathUtil.pow(minusLS, num(light, "specularExponent", 1));
                            }
                        }
                    }
                }
                double v;
                if (specular) {
                    double hx = lx;
                    double hy = ly;
                    double hz = lz + 1;
                    double hl = Math.sqrt(hx * hx + hy * hy + hz * hz);
                    double ndoth = hl == 0 ? 0 : (nx * hx + ny * hy + nz * hz) / hl;
                    v = kd * MathUtil.pow(Math.max(0, ndoth), exp);
                } else {
                    v = kd * Math.max(0, nx * lx + ny * ly + nz * lz);
                }
                v *= factor;
                float r = (float) (v * lr);
                float g = (float) (v * lg);
                float b = (float) (v * lb);
                int alpha = specular ? Pixels.to8(Math.max(r, Math.max(g, b))) : 255;
                o[y * w + x] = Pixels.pack(alpha, Math.min(1, r), Math.min(1, g), Math.min(1, b));
            }
        }
        if (linearRgb) {
            toSrgb(o);
        }
        return out;
    }

    private static float px(float[] a, int w, int h, int x, int y) {
        x = Math.max(0, Math.min(w - 1, x));
        y = Math.max(0, Math.min(h - 1, y));
        return a[y * w + x];
    }

    private static double sobelX(float[] a, int w, int h, int x, int y) {
        return (px(a, w, h, x + 1, y - 1) + 2 * px(a, w, h, x + 1, y) + px(a, w, h, x + 1, y + 1)
                - px(a, w, h, x - 1, y - 1) - 2 * px(a, w, h, x - 1, y) - px(a, w, h, x - 1, y + 1)) / 4.0;
    }

    private static double sobelY(float[] a, int w, int h, int x, int y) {
        return (px(a, w, h, x - 1, y + 1) + 2 * px(a, w, h, x, y + 1) + px(a, w, h, x + 1, y + 1)
                - px(a, w, h, x - 1, y - 1) - 2 * px(a, w, h, x, y - 1) - px(a, w, h, x + 1, y - 1)) / 4.0;
    }
}
