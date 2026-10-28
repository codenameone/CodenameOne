/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.runtime;

import java.io.ByteArrayOutputStream;

/// The `_cn1image` module: pixel storage and the hot loops behind the
/// bundled `PIL` package. An image is a [Img] handle holding interleaved
/// 8-bit bands (1 for "L", 3 for "RGB", 4 for "RGBA"), exactly the layout of
/// Pillow's `tobytes()`. Every arithmetic here follows Pillow's own fixed
/// point code so results are byte identical with it.
final class PyImage {
    /// Pillow's resampling precision, `32 - 8 - 2`.
    private static final int PRECISION_BITS = 22;

    private PyImage() {
    }

    /// One image: `w` by `h` pixels of `bands` interleaved bytes.
    static final class Img {
        final int w;
        final int h;
        final int bands;
        final byte[] d;

        Img(int w, int h, int bands) {
            this.w = w;
            this.h = h;
            this.bands = bands;
            this.d = new byte[w * h * bands];
        }
    }

    private static Img img(Object o) {
        if (o instanceof PyJavaObject && ((PyJavaObject) o).value instanceof Img) {
            return (Img) ((PyJavaObject) o).value;
        }
        throw Py.typeError("not an image");
    }

    private static Object wrap(Img i) {
        return new PyJavaObject(i);
    }

    private static int i(Object o) {
        return (int) Py.asLong(o);
    }

    private static byte[] bytes(Object o) {
        byte[] b = PyBytes.like(o);
        if (b == null) {
            throw Py.typeError("a bytes-like object is required");
        }
        return b;
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1image", new String[0]);
        m.setAttr("new", PyBuiltin.positional("new", a -> {
            Img r = new Img(i(a[0]), i(a[1]), i(a[2]));
            fill(r, 0, 0, r.w, r.h, bytes(a[3]), null);
            return wrap(r);
        }));
        m.setAttr("frombytes", PyBuiltin.positional("frombytes", a -> {
            Img r = new Img(i(a[0]), i(a[1]), i(a[2]));
            byte[] b = bytes(a[3]);
            if (b.length < r.d.length) {
                throw Py.error(Py.VALUE_ERROR, "not enough image data");
            }
            System.arraycopy(b, 0, r.d, 0, r.d.length);
            return wrap(r);
        }));
        m.setAttr("size", PyBuiltin.positional("size", a -> {
            Img s = img(a[0]);
            return new PyTuple(new Object[] {PyInt.of(s.w), PyInt.of(s.h)});
        }));
        m.setAttr("tobytes", PyBuiltin.positional("tobytes", a -> {
            Img s = img(a[0]);
            byte[] out = new byte[s.d.length];
            System.arraycopy(s.d, 0, out, 0, out.length);
            return new PyBytes(out);
        }));
        m.setAttr("getpixel", PyBuiltin.positional("getpixel", a -> {
            Img s = img(a[0]);
            int x = i(a[1]);
            int y = i(a[2]);
            if (x < 0 || y < 0 || x >= s.w || y >= s.h) {
                throw Py.error(Py.INDEX_ERROR, "image index out of range");
            }
            int p = (y * s.w + x) * s.bands;
            Object[] t = new Object[s.bands];
            for (int b = 0; b < s.bands; b++) {
                t[b] = PyInt.of(s.d[p + b] & 0xff);
            }
            return new PyTuple(t);
        }));
        m.setAttr("putpixel", PyBuiltin.positional("putpixel", a -> {
            Img s = img(a[0]);
            int x = i(a[1]);
            int y = i(a[2]);
            if (x < 0 || y < 0 || x >= s.w || y >= s.h) {
                throw Py.error(Py.INDEX_ERROR, "image index out of range");
            }
            System.arraycopy(bytes(a[3]), 0, s.d, (y * s.w + x) * s.bands, s.bands);
            return Py.NONE;
        }));
        m.setAttr("crop", PyBuiltin.positional("crop", a -> wrap(crop(img(a[0]), i(a[1]), i(a[2]), i(a[3]), i(a[4])))));
        m.setAttr("fill", PyBuiltin.positional("fill", a -> {
            Img s = img(a[0]);
            fill(s, i(a[1]), i(a[2]), i(a[3]), i(a[4]), bytes(a[5]), a[6] == Py.NONE ? null : img(a[6])); //NOPMD CompareObjectsWithEquals - identity is meant
            return Py.NONE;
        }));
        m.setAttr("paste", PyBuiltin.positional("paste", a -> {
            paste(img(a[0]), img(a[1]), i(a[2]), i(a[3]), a[4] == Py.NONE ? null : img(a[4])); //NOPMD CompareObjectsWithEquals - identity is meant
            return Py.NONE;
        }));
        m.setAttr("transpose", PyBuiltin.positional("transpose", a -> wrap(transpose(img(a[0]), i(a[1])))));
        m.setAttr("affine", PyBuiltin.positional("affine", a -> {
            Object[] c = Py.toArray(a[3]);
            double[] k = new double[6];
            for (int j = 0; j < 6; j++) {
                k[j] = Py.asDouble(c[j]);
            }
            return wrap(affine(img(a[0]), i(a[1]), i(a[2]), k, bytes(a[4])));
        }));
        m.setAttr("resample", PyBuiltin.positional("resample", a -> {
            Object[] c = Py.toArray(a[3]);
            double[] box = new double[4];
            for (int j = 0; j < 4; j++) {
                box[j] = Py.asDouble(c[j]);
            }
            Img src = img(a[0]);
            boolean premultiply = a.length > 5 && Py.isTrue(a[5]);
            if (premultiply) {
                src = premultiply(src);
            }
            Img r = resample(src, i(a[1]), i(a[2]), box, a.length > 4 ? i(a[4]) : FILTER_BILINEAR);
            return wrap(premultiply ? unpremultiply(r) : r);
        }));
        m.setAttr("palette", PyBuiltin.positional("palette", a -> wrap(palette(img(a[0]), bytes(a[1])))));
        m.setAttr("scatter", PyBuiltin.positional("scatter", a -> {
            scatter(img(a[0]), img(a[1]), i(a[2]), i(a[3]), i(a[4]), i(a[5]));
            return Py.NONE;
        }));
        m.setAttr("convert", PyBuiltin.positional("convert", a -> wrap(convert(img(a[0]), i(a[1])))));
        m.setAttr("band", PyBuiltin.positional("band", a -> {
            Img s = img(a[0]);
            int b = i(a[1]);
            Img r = new Img(s.w, s.h, 1);
            for (int p = 0, n = s.w * s.h; p < n; p++) {
                r.d[p] = s.d[p * s.bands + b];
            }
            return wrap(r);
        }));
        m.setAttr("merge", PyBuiltin.positional("merge", a -> {
            Object[] parts = Py.toArray(a[0]);
            Img first = img(parts[0]);
            Img r = new Img(first.w, first.h, parts.length);
            for (int b = 0; b < parts.length; b++) {
                Img s = img(parts[b]);
                if (s.w != r.w || s.h != r.h || s.bands != 1) {
                    throw Py.error(Py.VALUE_ERROR, "images do not match");
                }
                for (int p = 0, n = r.w * r.h; p < n; p++) {
                    r.d[p * r.bands + b] = s.d[p];
                }
            }
            return wrap(r);
        }));
        m.setAttr("getbbox", PyBuiltin.positional("getbbox", a -> getbbox(img(a[0]), Py.isTrue(a[1]))));
        m.setAttr("point", PyBuiltin.positional("point", a -> {
            Img s = img(a[0]);
            byte[] lut = bytes(a[1]);
            Img r = new Img(s.w, s.h, s.bands);
            for (int p = 0; p < s.d.length; p += s.bands) {
                for (int b = 0; b < s.bands; b++) {
                    r.d[p + b] = lut[(b << 8) + (s.d[p + b] & 0xff)];
                }
            }
            return wrap(r);
        }));
        m.setAttr("boxblur", PyBuiltin.positional("boxblur", a -> {
            Img s = img(a[0]);
            int r = i(a[1]);
            Img t = blurRows(s, r);
            Img u = blurRows(transpose(t, 5), r);
            return wrap(transpose(u, 5));
        }));
        m.setAttr("png_unfilter", PyBuiltin.positional("png_unfilter", a ->
                new PyBytes(unfilter(bytes(a[0]), i(a[1]), i(a[2]), i(a[3])))));
        m.setAttr("png_expand", PyBuiltin.positional("png_expand", a ->
                wrap(expand(bytes(a[0]), i(a[1]), i(a[2]), i(a[3]), i(a[4])))));
        m.setAttr("png_rows", PyBuiltin.positional("png_rows", a -> {
            Img s = img(a[0]);
            int row = s.w * s.bands;
            byte[] out = new byte[(row + 1) * s.h];
            for (int y = 0; y < s.h; y++) {
                System.arraycopy(s.d, y * row, out, y * (row + 1) + 1, row);
            }
            return new PyBytes(out);
        }));
        m.setAttr("native_available", PyBuiltin.positional("native_available", a ->
                com.codename1.ui.Display.isInitialized() ? Py.TRUE : Py.FALSE));
        m.setAttr("native_decode", PyBuiltin.positional("native_decode", a -> {
            byte[] b = bytes(a[0]);
            com.codename1.ui.Image ci = com.codename1.ui.Image.createImage(b, 0, b.length);
            int w = ci.getWidth();
            int h = ci.getHeight();
            int[] argb = ci.getRGB();
            Img r = new Img(w, h, 4);
            for (int p = 0; p < w * h; p++) {
                int c = argb[p];
                r.d[p * 4] = (byte) (c >> 16);
                r.d[p * 4 + 1] = (byte) (c >> 8);
                r.d[p * 4 + 2] = (byte) c;
                r.d[p * 4 + 3] = (byte) (c >>> 24);
            }
            return wrap(r);
        }));
        m.setAttr("native_encode", PyBuiltin.positional("native_encode", a -> {
            Img s = convert(img(a[0]), 4);
            int[] argb = new int[s.w * s.h];
            for (int p = 0; p < argb.length; p++) {
                argb[p] = (s.d[p * 4 + 3] & 0xff) << 24 | (s.d[p * 4] & 0xff) << 16
                        | (s.d[p * 4 + 1] & 0xff) << 8 | (s.d[p * 4 + 2] & 0xff);
            }
            com.codename1.ui.Image ci = com.codename1.ui.Image.createImage(argb, s.w, s.h);
            com.codename1.ui.util.ImageIO io = com.codename1.ui.util.ImageIO.getImageIO();
            if (io == null) {
                throw Py.error(Py.OS_ERROR, "this platform cannot encode " + Py.toStr(a[1]));
            }
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            try {
                io.save(ci, bo, Py.toStr(a[1]), (float) Py.asDouble(a[2]));
            } catch (java.io.IOException e) {
                throw Py.error(Py.OS_ERROR, String.valueOf(e.getMessage()));
            }
            return new PyBytes(bo.toByteArray());
        }));
        return m;
    }

    private static Img crop(Img s, int x0, int y0, int x1, int y1) {
        Img r = new Img(Math.max(0, x1 - x0), Math.max(0, y1 - y0), s.bands);
        for (int y = 0; y < r.h; y++) {
            int sy = y + y0;
            if (sy < 0 || sy >= s.h) {
                continue;
            }
            for (int x = 0; x < r.w; x++) {
                int sx = x + x0;
                if (sx >= 0 && sx < s.w) {
                    System.arraycopy(s.d, (sy * s.w + sx) * s.bands, r.d, (y * r.w + x) * s.bands, s.bands);
                }
            }
        }
        return r;
    }

    /// Pillow's `DIV255(in1 * (255 - mask) + in2 * mask)`.
    private static int blend(int out, int in, int mask) {
        int t = out * (255 - mask) + in * mask + 128;
        return ((t >> 8) + t) >> 8;
    }

    private static int maskAt(Img mask, int x, int y) {
        return mask.d[(y * mask.w + x) * mask.bands + mask.bands - 1] & 0xff;
    }

    private static void fill(Img s, int x0, int y0, int x1, int y1, byte[] color, Img mask) {
        int ax = Math.max(0, x0);
        int ay = Math.max(0, y0);
        int bx = Math.min(s.w, x1);
        int by = Math.min(s.h, y1);
        for (int y = ay; y < by; y++) {
            for (int x = ax; x < bx; x++) {
                int p = (y * s.w + x) * s.bands;
                if (mask == null) {
                    System.arraycopy(color, 0, s.d, p, s.bands);
                } else {
                    int k = maskAt(mask, x - x0, y - y0);
                    for (int b = 0; b < s.bands; b++) {
                        s.d[p + b] = (byte) blend(s.d[p + b] & 0xff, color[b] & 0xff, k);
                    }
                }
            }
        }
    }

    private static void paste(Img dst, Img src, int x0, int y0, Img mask) {
        for (int y = 0; y < src.h; y++) {
            int dy = y + y0;
            if (dy < 0 || dy >= dst.h) {
                continue;
            }
            for (int x = 0; x < src.w; x++) {
                int dx = x + x0;
                if (dx < 0 || dx >= dst.w) {
                    continue;
                }
                int p = (dy * dst.w + dx) * dst.bands;
                int q = (y * src.w + x) * src.bands;
                if (mask == null) {
                    System.arraycopy(src.d, q, dst.d, p, dst.bands);
                } else {
                    int k = maskAt(mask, x, y);
                    for (int b = 0; b < dst.bands; b++) {
                        dst.d[p + b] = (byte) blend(dst.d[p + b] & 0xff, src.d[q + b] & 0xff, k);
                    }
                }
            }
        }
    }

    /// Pillow's `Transpose` values: 0 flip left-right, 1 flip top-bottom,
    /// 2 rotate 90, 3 rotate 180, 4 rotate 270, 5 transpose, 6 transverse.
    private static Img transpose(Img s, int op) {
        boolean swap = op == 2 || op == 4 || op == 5 || op == 6;
        Img r = swap ? new Img(s.h, s.w, s.bands) : new Img(s.w, s.h, s.bands);
        for (int y = 0; y < r.h; y++) {
            for (int x = 0; x < r.w; x++) {
                int sx;
                int sy;
                switch (op) {
                    case 0: sx = s.w - 1 - x; sy = y; break;
                    case 1: sx = x; sy = s.h - 1 - y; break;
                    case 2: sx = s.w - 1 - y; sy = x; break;
                    case 3: sx = s.w - 1 - x; sy = s.h - 1 - y; break;
                    case 4: sx = y; sy = s.h - 1 - x; break;
                    case 5: sx = y; sy = x; break;
                    case 6: sx = s.w - 1 - y; sy = s.h - 1 - x; break;
                    default: throw Py.error(Py.VALUE_ERROR, "unknown transpose method");
                }
                System.arraycopy(s.d, (sy * s.w + sx) * s.bands, r.d, (y * r.w + x) * s.bands, s.bands);
            }
        }
        return r;
    }

    /// Pillow's `COORD`: truncation, with every negative coordinate outside.
    private static int coord(double v) {
        return v < 0.0 ? -1 : (int) v;
    }

    /// Nearest neighbour affine transform, accumulating the coordinates the
    /// way Pillow's `affine_transform` does so the rounding is the same.
    private static Img affine(Img s, int w, int h, double[] a, byte[] fill) {
        Img r = new Img(w, h, s.bands);
        double xo = a[2] + a[1] * 0.5 + a[0] * 0.5;
        double yo = a[5] + a[4] * 0.5 + a[3] * 0.5;
        for (int y = 0; y < h; y++) {
            double xx = xo;
            double yy = yo;
            for (int x = 0; x < w; x++) {
                int xin = coord(xx);
                int yin = coord(yy);
                int p = (y * w + x) * s.bands;
                if (xin >= 0 && yin >= 0 && xin < s.w && yin < s.h) {
                    System.arraycopy(s.d, (yin * s.w + xin) * s.bands, r.d, p, s.bands);
                } else {
                    System.arraycopy(fill, 0, r.d, p, s.bands);
                }
                xx += a[0];
                yy += a[3];
            }
            xo += a[1];
            yo += a[4];
        }
        return r;
    }

    static final int FILTER_LANCZOS = 1;
    static final int FILTER_BILINEAR = 2;
    static final int FILTER_BICUBIC = 3;
    static final int FILTER_BOX = 4;
    static final int FILTER_HAMMING = 5;

    private static double support(int filter) {
        switch (filter) {
            case FILTER_BOX:
                return 0.5;
            case FILTER_BICUBIC:
                return 2.0;
            case FILTER_LANCZOS:
                return 3.0;
            default:
                return 1.0;
        }
    }

    private static double sinc(double x) {
        if (x == 0.0) {
            return 1.0;
        }
        double px = x * Math.PI;
        return Math.sin(px) / px;
    }

    /// Pillow's filter kernels (`Resample.c`), evaluated at signed `x`.
    private static double kernel(int filter, double x) {
        double t = x < 0 ? -x : x;
        switch (filter) {
            case FILTER_BOX:
                return x > -0.5 && x <= 0.5 ? 1.0 : 0.0;
            case FILTER_HAMMING:
                if (t == 0.0) {
                    return 1.0;
                }
                if (t >= 1.0) {
                    return 0.0;
                }
                t = t * Math.PI;
                return Math.sin(t) / t * (0.54 + 0.46 * Math.cos(t));
            case FILTER_BICUBIC:
                if (t < 1.0) {
                    return (1.5 * t - 2.5) * t * t + 1;
                }
                if (t < 2.0) {
                    return (((t - 5) * t + 8) * t - 4) * -0.5;
                }
                return 0.0;
            case FILTER_LANCZOS:
                return x > -3.0 && x < 3.0 ? sinc(x) * sinc(x / 3.0) : 0.0;
            default:
                return t < 1.0 ? 1.0 - t : 0.0;
        }
    }

    /// Filter coefficients for one axis, as Pillow's `precompute_coeffs`
    /// and `normalize_coeffs_8bpc` make them. Returns `ksize`; `bounds`
    /// receives `xmin, count` per output position.
    private static int coeffs(int inSize, double in0, double in1, int outSize, int[] bounds, int[][] out,
                              int filter) {
        double scale = (in1 - in0) / outSize;
        double filterscale = scale < 1.0 ? 1.0 : scale;
        double support = support(filter) * filterscale;
        int ksize = (int) Math.ceil(support) * 2 + 1;
        int[] kk = new int[outSize * ksize];
        double[] k = new double[ksize];
        for (int xx = 0; xx < outSize; xx++) {
            double center = in0 + (xx + 0.5) * scale;
            double ww = 0.0;
            double ss = 1.0 / filterscale;
            int xmin = (int) (center - support + 0.5);
            if (xmin < 0) {
                xmin = 0;
            }
            int xmax = (int) (center + support + 0.5);
            if (xmax > inSize) {
                xmax = inSize;
            }
            xmax -= xmin;
            for (int x = 0; x < xmax; x++) {
                double w = kernel(filter, (x + xmin - center + 0.5) * ss);
                k[x] = w;
                ww += w;
            }
            for (int x = 0; x < xmax; x++) {
                double v = ww != 0.0 ? k[x] / ww : k[x];
                kk[xx * ksize + x] = v < 0 ? (int) (-0.5 + v * (1 << PRECISION_BITS))
                        : (int) (0.5 + v * (1 << PRECISION_BITS));
            }
            bounds[xx * 2] = xmin;
            bounds[xx * 2 + 1] = xmax;
        }
        out[0] = kk;
        return ksize;
    }

    private static byte clip8(int v) {
        int s = v >> PRECISION_BITS;
        return (byte) (s < 0 ? 0 : s > 255 ? 255 : s);
    }

    private static Img resample(Img s, int w, int h, double[] box, int filter) {
        Img cur = s;
        boolean horizontal = w != s.w || box[0] != 0 || box[2] != w;
        boolean vertical = h != s.h || box[1] != 0 || box[3] != h;
        if (horizontal) {
            int[] bounds = new int[w * 2];
            int[][] kh = new int[1][];
            int ks = coeffs(cur.w, box[0], box[2], w, bounds, kh, filter);
            Img r = new Img(w, cur.h, cur.bands);
            for (int y = 0; y < cur.h; y++) {
                for (int x = 0; x < w; x++) {
                    int xmin = bounds[x * 2];
                    int n = bounds[x * 2 + 1];
                    for (int b = 0; b < cur.bands; b++) {
                        int ss = 1 << (PRECISION_BITS - 1);
                        for (int j = 0; j < n; j++) {
                            ss += (cur.d[(y * cur.w + xmin + j) * cur.bands + b] & 0xff) * kh[0][x * ks + j];
                        }
                        r.d[(y * w + x) * cur.bands + b] = clip8(ss);
                    }
                }
            }
            cur = r;
        }
        if (vertical) {
            int[] bounds = new int[h * 2];
            int[][] kv = new int[1][];
            int ks = coeffs(cur.h, box[1], box[3], h, bounds, kv, filter);
            Img r = new Img(cur.w, h, cur.bands);
            for (int y = 0; y < h; y++) {
                int ymin = bounds[y * 2];
                int n = bounds[y * 2 + 1];
                for (int x = 0; x < cur.w; x++) {
                    for (int b = 0; b < cur.bands; b++) {
                        int ss = 1 << (PRECISION_BITS - 1);
                        for (int j = 0; j < n; j++) {
                            ss += (cur.d[((ymin + j) * cur.w + x) * cur.bands + b] & 0xff) * kv[0][y * ks + j];
                        }
                        r.d[(y * cur.w + x) * cur.bands + b] = clip8(ss);
                    }
                }
            }
            cur = r;
        }
        if (cur == s) { //NOPMD CompareObjectsWithEquals - identity is meant
            cur = crop(s, 0, 0, s.w, s.h);
        }
        return cur;
    }

    /// RGBA to RGBa (or LA to La): Pillow's `MULDIV255` premultiply.
    private static Img premultiply(Img s) {
        Img r = crop(s, 0, 0, s.w, s.h);
        int n = s.bands;
        for (int p = 0, c = s.w * s.h; p < c; p++) {
            int q = p * n;
            int alpha = s.d[q + n - 1] & 0xff;
            for (int b = 0; b < n - 1; b++) {
                int t = (s.d[q + b] & 0xff) * alpha + 128;
                r.d[q + b] = (byte) (((t >> 8) + t) >> 8);
            }
        }
        return r;
    }

    /// RGBa to RGBA (or La to LA), as Pillow's `rgbA2rgba`.
    private static Img unpremultiply(Img s) {
        int n = s.bands;
        for (int p = 0, c = s.w * s.h; p < c; p++) {
            int q = p * n;
            int alpha = s.d[q + n - 1] & 0xff;
            if (alpha == 0 || alpha == 255) {
                continue;
            }
            for (int b = 0; b < n - 1; b++) {
                int v = (255 * (s.d[q + b] & 0xff)) / alpha;
                s.d[q + b] = (byte) (v > 255 ? 255 : v);
            }
        }
        return s;
    }

    /// Maps a 1-band index image through a 256 entry RGBA table.
    private static Img palette(Img s, byte[] lut) {
        Img r = new Img(s.w, s.h, 4);
        for (int p = 0, n = s.w * s.h; p < n; p++) {
            int idx = s.d[p * s.bands] & 0xff;
            System.arraycopy(lut, idx * 4, r.d, p * 4, 4);
        }
        return r;
    }

    /// Writes `src` into `dst` at `x0 + x * dx, y0 + y * dy`: one Adam7 pass.
    private static void scatter(Img dst, Img src, int x0, int y0, int dx, int dy) {
        int n = dst.bands;
        for (int y = 0; y < src.h; y++) {
            for (int x = 0; x < src.w; x++) {
                System.arraycopy(src.d, (y * src.w + x) * n, dst.d, ((y0 + y * dy) * dst.w + x0 + x * dx) * n, n);
            }
        }
    }

    private static Img convert(Img s, int bands) {
        Img r = new Img(s.w, s.h, bands);
        for (int p = 0, n = s.w * s.h; p < n; p++) {
            int q = p * s.bands;
            int cr = s.d[q] & 0xff;
            boolean gray = s.bands < 3;
            int cg = gray ? cr : s.d[q + 1] & 0xff;
            int cb = gray ? cr : s.d[q + 2] & 0xff;
            int ca = s.bands == 4 ? s.d[q + 3] & 0xff : s.bands == 2 ? s.d[q + 1] & 0xff : 255;
            int o = p * bands;
            if (bands <= 2) {
                // Pillow's L24: ITU-R 601-2 luma in 16-bit fixed point
                r.d[o] = gray ? (byte) cr : (byte) ((cr * 19595 + cg * 38470 + cb * 7471 + 0x8000) >> 16);
                if (bands == 2) {
                    r.d[o + 1] = (byte) ca;
                }
            } else {
                r.d[o] = (byte) cr;
                r.d[o + 1] = (byte) cg;
                r.d[o + 2] = (byte) cb;
                if (bands == 4) {
                    r.d[o + 3] = (byte) ca;
                }
            }
        }
        return r;
    }

    private static Object getbbox(Img s, boolean alphaOnly) {
        int x0 = s.w;
        int y0 = s.h;
        int x1 = -1;
        int y1 = -1;
        int from = alphaOnly && s.bands == 4 ? 3 : 0;
        for (int y = 0; y < s.h; y++) {
            for (int x = 0; x < s.w; x++) {
                int p = (y * s.w + x) * s.bands;
                boolean set = false;
                for (int b = from; b < s.bands; b++) {
                    if (s.d[p + b] != 0) {
                        set = true;
                        break;
                    }
                }
                if (set) {
                    x0 = Math.min(x0, x);
                    y0 = Math.min(y0, y);
                    x1 = Math.max(x1, x);
                    y1 = Math.max(y1, y);
                }
            }
        }
        if (x1 < 0) {
            return Py.NONE;
        }
        return new PyTuple(new Object[] {PyInt.of(x0), PyInt.of(y0), PyInt.of(x1 + 1), PyInt.of(y1 + 1)});
    }

    /// One horizontal pass of Pillow's box blur for an integer radius.
    private static Img blurRows(Img s, int r) {
        Img o = new Img(s.w, s.h, s.bands);
        int ww = (1 << 24) / (r * 2 + 1);
        int fw = ((1 << 24) - (r * 2 + 1) * ww) / 2;
        int last = s.w - 1;
        for (int y = 0; y < s.h; y++) {
            for (int b = 0; b < s.bands; b++) {
                int base = y * s.w * s.bands + b;
                for (int x = 0; x < s.w; x++) {
                    long acc = 0;
                    for (int k = x - r; k <= x + r; k++) {
                        acc += s.d[base + Math.max(0, Math.min(last, k)) * s.bands] & 0xff;
                    }
                    long edge = (s.d[base + Math.max(0, Math.min(last, x - r - 1)) * s.bands] & 0xff)
                            + (s.d[base + Math.max(0, Math.min(last, x + r + 1)) * s.bands] & 0xff);
                    o.d[base + x * s.bands] = (byte) ((acc * ww + edge * fw + (1 << 23)) >> 24);
                }
            }
        }
        return o;
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
        if (pa <= pb && pa <= pc) {
            return a;
        }
        return pb <= pc ? b : c;
    }

    /// Reverses the five PNG row filters; `data` holds `h` rows of a filter
    /// byte followed by `row` bytes, and `bpp` is the filter's byte distance.
    private static byte[] unfilter(byte[] data, int row, int h, int bpp) {
        if (data.length < (row + 1) * h) {
            throw Py.error(Py.OS_ERROR, "image file is truncated");
        }
        byte[] out = new byte[row * h];
        for (int y = 0; y < h; y++) {
            int ft = data[y * (row + 1)] & 0xff;
            int in = y * (row + 1) + 1;
            int o = y * row;
            for (int x = 0; x < row; x++) {
                int raw = data[in + x] & 0xff;
                int a = x >= bpp ? out[o + x - bpp] & 0xff : 0;
                int b = y > 0 ? out[o - row + x] & 0xff : 0;
                int c = x >= bpp && y > 0 ? out[o - row + x - bpp] & 0xff : 0;
                int v;
                switch (ft) {
                    case 0: v = raw; break;
                    case 1: v = raw + a; break;
                    case 2: v = raw + b; break;
                    case 3: v = raw + ((a + b) >> 1); break;
                    case 4: v = raw + paeth(a, b, c); break;
                    default: throw Py.error(Py.OS_ERROR, "unknown PNG filter type " + ft);
                }
                out[o + x] = (byte) v;
            }
        }
        return out;
    }

    private static int sample(byte[] raw, int rowStart, int index, int depth) {
        if (depth == 8) {
            return raw[rowStart + index] & 0xff;
        }
        if (depth == 16) {
            return raw[rowStart + index * 2] & 0xff;
        }
        int bit = index * depth;
        int v = (raw[rowStart + (bit >> 3)] & 0xff) >> (8 - depth - (bit & 7)) & ((1 << depth) - 1);
        return v;
    }

    /// Unpacks unfiltered PNG rows into an image: gray to 1 band, RGB to 3,
    /// gray+alpha to 2, RGBA to 4 and palette to its 1-band indices.
    private static Img expand(byte[] raw, int w, int h, int ctype, int depth) {
        int channels = ctype == 0 || ctype == 3 ? 1 : ctype == 2 ? 3 : ctype == 4 ? 2 : 4;
        int row = (w * channels * depth + 7) / 8;
        int bands = ctype == 0 || ctype == 3 ? 1 : ctype == 2 ? 3 : ctype == 4 ? 2 : 4;
        Img r = new Img(w, h, bands);
        for (int y = 0; y < h; y++) {
            int rs = y * row;
            for (int x = 0; x < w; x++) {
                int o = (y * w + x) * bands;
                if (ctype == 3) {
                    r.d[o] = (byte) sample(raw, rs, x, depth);
                    continue;
                }
                int scale = depth < 8 ? 255 / ((1 << depth) - 1) : 1;
                if (ctype == 4) {
                    r.d[o] = (byte) sample(raw, rs, x * 2, depth);
                    r.d[o + 1] = (byte) sample(raw, rs, x * 2 + 1, depth);
                    continue;
                }
                for (int b = 0; b < channels; b++) {
                    r.d[o + b] = (byte) (sample(raw, rs, x * channels + b, depth) * scale);
                }
            }
        }
        return r;
    }
}
