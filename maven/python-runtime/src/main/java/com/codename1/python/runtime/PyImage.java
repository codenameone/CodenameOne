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




/// The `_cn1image` module: pixel storage and the hot loops behind the
/// bundled `PIL` package. An image is a [PyImageData] handle holding interleaved
/// 8-bit bands (1 for "L", 3 for "RGB", 4 for "RGBA"), exactly the layout of
/// Pillow's `tobytes()`. Every arithmetic here follows Pillow's own fixed
/// point code so results are byte identical with it. Codecs, resampling and
/// text go through the [PyImageEngine] the generated registry installs with
/// [#setEngine(PyImageEngine)].
public final class PyImage {
    private PyImage() {
    }

    private static PyImageEngine engine;

    /// Installs the engine behind codecs, resampling and text; the generated
    /// `PythonModules` calls it with the server or the client one.
    public static void setEngine(PyImageEngine e) {
        engine = e;
    }

    private static PyImageEngine requireEngine() {
        PyImageEngine e = engine;
        if (e == null || !e.available()) {
            throw Py.error(Py.OS_ERROR, "this build has no image engine: on a device PIL needs a running "
                    + "Codename One display");
        }
        return e;
    }

    private static PyImageData img(Object o) {
        if (o instanceof PyJavaObject && ((PyJavaObject) o).value instanceof PyImageData) {
            return (PyImageData) ((PyJavaObject) o).value;
        }
        throw Py.typeError("not an image");
    }

    private static Object wrap(PyImageData i) {
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
            PyImageData r = new PyImageData(i(a[0]), i(a[1]), i(a[2]));
            fill(r, 0, 0, r.w, r.h, bytes(a[3]), null);
            return wrap(r);
        }));
        m.setAttr("frombytes", PyBuiltin.positional("frombytes", a -> {
            PyImageData r = new PyImageData(i(a[0]), i(a[1]), i(a[2]));
            byte[] b = bytes(a[3]);
            if (b.length < r.d.length) {
                throw Py.error(Py.VALUE_ERROR, "not enough image data");
            }
            System.arraycopy(b, 0, r.d, 0, r.d.length);
            return wrap(r);
        }));
        m.setAttr("size", PyBuiltin.positional("size", a -> {
            PyImageData s = img(a[0]);
            return new PyTuple(new Object[] {PyInt.of(s.w), PyInt.of(s.h)});
        }));
        m.setAttr("tobytes", PyBuiltin.positional("tobytes", a -> {
            PyImageData s = img(a[0]);
            byte[] out = new byte[s.d.length];
            System.arraycopy(s.d, 0, out, 0, out.length);
            return new PyBytes(out);
        }));
        m.setAttr("getpixel", PyBuiltin.positional("getpixel", a -> {
            PyImageData s = img(a[0]);
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
            PyImageData s = img(a[0]);
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
            PyImageData s = img(a[0]);
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
            return wrap(requireEngine().resample(img(a[0]), i(a[1]), i(a[2]), box,
                    a.length > 4 ? i(a[4]) : FILTER_BILINEAR, a.length > 5 && Py.isTrue(a[5])));
        }));
        m.setAttr("palette", PyBuiltin.positional("palette", a -> wrap(palette(img(a[0]), bytes(a[1])))));
        m.setAttr("scatter", PyBuiltin.positional("scatter", a -> {
            scatter(img(a[0]), img(a[1]), i(a[2]), i(a[3]), i(a[4]), i(a[5]));
            return Py.NONE;
        }));
        m.setAttr("convert", PyBuiltin.positional("convert", a -> wrap(convert(img(a[0]), i(a[1])))));
        m.setAttr("band", PyBuiltin.positional("band", a -> {
            PyImageData s = img(a[0]);
            int b = i(a[1]);
            PyImageData r = new PyImageData(s.w, s.h, 1);
            for (int p = 0, n = s.w * s.h; p < n; p++) {
                r.d[p] = s.d[p * s.bands + b];
            }
            return wrap(r);
        }));
        m.setAttr("merge", PyBuiltin.positional("merge", a -> {
            Object[] parts = Py.toArray(a[0]);
            PyImageData first = img(parts[0]);
            PyImageData r = new PyImageData(first.w, first.h, parts.length);
            for (int b = 0; b < parts.length; b++) {
                PyImageData s = img(parts[b]);
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
            PyImageData s = img(a[0]);
            byte[] lut = bytes(a[1]);
            PyImageData r = new PyImageData(s.w, s.h, s.bands);
            for (int p = 0; p < s.d.length; p += s.bands) {
                for (int b = 0; b < s.bands; b++) {
                    r.d[p + b] = lut[(b << 8) + (s.d[p + b] & 0xff)];
                }
            }
            return wrap(r);
        }));
        m.setAttr("boxblur", PyBuiltin.positional("boxblur", a -> {
            PyImageData s = img(a[0]);
            int r = i(a[1]);
            PyImageData t = blurRows(s, r);
            PyImageData u = blurRows(transpose(t, 5), r);
            return wrap(transpose(u, 5));
        }));
        m.setAttr("png_unfilter", PyBuiltin.positional("png_unfilter", a ->
                new PyBytes(requireEngine().pngUnfilter(bytes(a[0]), i(a[1]), i(a[2]), i(a[3])))));
        m.setAttr("png_expand", PyBuiltin.positional("png_expand", a ->
                wrap(requireEngine().pngExpand(bytes(a[0]), i(a[1]), i(a[2]), i(a[3]), i(a[4])))));
        m.setAttr("jpeg_decode", PyBuiltin.positional("jpeg_decode", a -> wrap(requireEngine().jpegDecode(bytes(a[0])))));
        m.setAttr("jpeg_encode", PyBuiltin.positional("jpeg_encode", a ->
                new PyBytes(requireEngine().jpegEncode(img(a[0]), (float) Py.asDouble(a[1])))));
        m.setAttr("png_rows", PyBuiltin.positional("png_rows", a -> {
            PyImageData s = img(a[0]);
            int row = s.w * s.bands;
            byte[] out = new byte[(row + 1) * s.h];
            for (int y = 0; y < s.h; y++) {
                System.arraycopy(s.d, y * row, out, y * (row + 1) + 1, row);
            }
            return new PyBytes(out);
        }));
        m.setAttr("ft_load", PyBuiltin.positional("ft_load", a ->
                new PyJavaObject(requireEngine().loadFont(bytes(a[0]), i(a[1]), Py.asDouble(a[2])))));
        m.setAttr("ft_default_data", PyBuiltin.positional("ft_default_data", a -> {
            byte[] d = requireEngine().defaultFontData();
            return d.length == 0 ? Py.NONE : new PyBytes(d);
        }));
        m.setAttr("ft_default", PyBuiltin.positional("ft_default", a ->
                new PyJavaObject(requireEngine().defaultFont(Py.asDouble(a[0])))));
        m.setAttr("ft_variant", PyBuiltin.positional("ft_variant", a ->
                new PyJavaObject(requireEngine().deriveFont(ft(a[0]), Py.asDouble(a[1])))));
        m.setAttr("ft_name", PyBuiltin.positional("ft_name", a -> {
            String[] n = requireEngine().fontName(ft(a[0]));
            return new PyTuple(new Object[] {new PyStr(n[0]), new PyStr(n[1])});
        }));
        m.setAttr("ft_metrics", PyBuiltin.positional("ft_metrics", a -> {
            int[] mt = requireEngine().fontMetrics(ft(a[0]));
            return new PyTuple(new Object[] {PyInt.of(mt[0]), PyInt.of(mt[1])});
        }));
        m.setAttr("ft_length", PyBuiltin.positional("ft_length", a ->
                PyInt.of(requireEngine().textLength(ft(a[0]), Py.toStr(a[1])))));
        m.setAttr("ft_bbox", PyBuiltin.positional("ft_bbox", a -> {
            int[] b = requireEngine().textBox(ft(a[0]), Py.toStr(a[1]), anchor(a[2]));
            return new PyTuple(new Object[] {PyInt.of(b[0]), PyInt.of(b[1]), PyInt.of(b[2]), PyInt.of(b[3])});
        }));
        m.setAttr("ft_mask", PyBuiltin.positional("ft_mask", a -> {
            PyImageEngine e = requireEngine();
            Object f = ft(a[0]);
            String text = Py.toStr(a[1]);
            String anchor = anchor(a[2]);
            int[] b = e.textBox(f, text, anchor);
            return new PyTuple(new Object[] {wrap(e.textMask(f, text, anchor, Py.isTrue(a[3]))), PyInt.of(b[2]),
                PyInt.of(b[3])});
        }));
        m.setAttr("exact", PyBuiltin.positional("exact", a -> {
            PyImageEngine e = engine;
            return e != null && e.exact() ? Py.TRUE : Py.FALSE;
        }));
        m.setAttr("native_available", PyBuiltin.positional("native_available", a -> {
            PyImageEngine e = engine;
            return e != null && e.available() ? Py.TRUE : Py.FALSE;
        }));
        m.setAttr("native_decode", PyBuiltin.positional("native_decode", a ->
                wrap(requireEngine().decode(bytes(a[0])))));
        m.setAttr("native_encode", PyBuiltin.positional("native_encode", a ->
                new PyBytes(requireEngine().encode(convert(img(a[0]), 4), Py.toStr(a[1]),
                        (float) Py.asDouble(a[2])))));
        return m;
    }

    private static Object ft(Object o) {
        if (o instanceof PyJavaObject && ((PyJavaObject) o).value != null) {
            return ((PyJavaObject) o).value;
        }
        throw Py.typeError("not a TrueType font");
    }

    /// Pillow's anchor check: null (the default, `la`) or a horizontal
    /// `l`/`m`/`r` followed by a vertical `a`/`t`/`m`/`s`/`b`/`d`.
    private static String anchor(Object o) {
        String a = Py.NONE.equals(o) ? null : Py.toStr(o);
        if (a != null && (a.length() != 2 || "lmr".indexOf(a.charAt(0)) < 0 || "atmsbd".indexOf(a.charAt(1)) < 0)) {
            throw Py.error(Py.VALUE_ERROR, "bad anchor specified: " + a);
        }
        return a;
    }

    private static PyImageData crop(PyImageData s, int x0, int y0, int x1, int y1) {
        PyImageData r = new PyImageData(Math.max(0, x1 - x0), Math.max(0, y1 - y0), s.bands);
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

    private static int maskAt(PyImageData mask, int x, int y) {
        return mask.d[(y * mask.w + x) * mask.bands + mask.bands - 1] & 0xff;
    }

    private static void fill(PyImageData s, int x0, int y0, int x1, int y1, byte[] color, PyImageData mask) {
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

    private static void paste(PyImageData dst, PyImageData src, int x0, int y0, PyImageData mask) {
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
    private static PyImageData transpose(PyImageData s, int op) {
        boolean swap = op == 2 || op == 4 || op == 5 || op == 6;
        PyImageData r = swap ? new PyImageData(s.h, s.w, s.bands) : new PyImageData(s.w, s.h, s.bands);
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
    private static PyImageData affine(PyImageData s, int w, int h, double[] a, byte[] fill) {
        PyImageData r = new PyImageData(w, h, s.bands);
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

    /// Maps a 1-band index image through a 256 entry RGBA table.
    private static PyImageData palette(PyImageData s, byte[] lut) {
        PyImageData r = new PyImageData(s.w, s.h, 4);
        for (int p = 0, n = s.w * s.h; p < n; p++) {
            int idx = s.d[p * s.bands] & 0xff;
            System.arraycopy(lut, idx * 4, r.d, p * 4, 4);
        }
        return r;
    }

    /// Writes `src` into `dst` at `x0 + x * dx, y0 + y * dy`: one Adam7 pass.
    private static void scatter(PyImageData dst, PyImageData src, int x0, int y0, int dx, int dy) {
        int n = dst.bands;
        for (int y = 0; y < src.h; y++) {
            for (int x = 0; x < src.w; x++) {
                System.arraycopy(src.d, (y * src.w + x) * n, dst.d, ((y0 + y * dy) * dst.w + x0 + x * dx) * n, n);
            }
        }
    }

    private static PyImageData convert(PyImageData s, int bands) {
        PyImageData r = new PyImageData(s.w, s.h, bands);
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

    private static Object getbbox(PyImageData s, boolean alphaOnly) {
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
    private static PyImageData blurRows(PyImageData s, int r) {
        PyImageData o = new PyImageData(s.w, s.h, s.bands);
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
}
