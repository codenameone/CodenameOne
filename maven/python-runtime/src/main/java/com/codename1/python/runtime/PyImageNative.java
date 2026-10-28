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

import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// PIL's client [PyImageEngine]: Codename One's `Image`, `ImageIO`,
/// `Graphics` and `Font`, installed by the generated registry of a client
/// build. A backend build has no UI package and leaves this class out; it
/// gets the generated `BackendImage` instead.
///
/// Everything here is drawn by the platform, so results are close to
/// Pillow's but are not its exact pixels: resizing is the platform's
/// scaling whatever filter was asked for, PNG files are decoded by the
/// platform, and text is the platform's native font. A device cannot load
/// a TrueType file from bytes, so `ImageFont.truetype` answers the native
/// font at the requested size as well.
public final class PyImageNative implements PyImageEngine {
    private static final String NATIVE_FONT = "native:MainRegular";

    private PyImageNative() {
    }

    /// Makes PIL decode, encode, resize and draw text through the platform;
    /// idempotent.
    public static void install() {
        PyImage.setEngine(new PyImageNative());
    }

    @Override
    public boolean exact() {
        return false;
    }

    @Override
    public boolean available() {
        return Display.isInitialized();
    }

    private static PyImageData fromArgb(int[] argb, int w, int h) {
        PyImageData r = new PyImageData(w, h, 4);
        for (int p = 0; p < w * h; p++) {
            int c = argb[p];
            r.d[p * 4] = (byte) (c >> 16);
            r.d[p * 4 + 1] = (byte) (c >> 8);
            r.d[p * 4 + 2] = (byte) c;
            r.d[p * 4 + 3] = (byte) (c >>> 24);
        }
        return r;
    }

    /// Any image as platform ARGB; `L`, `LA`, `RGB` and `RGBA` are accepted.
    private static int[] toArgb(PyImageData s) {
        int[] argb = new int[s.w * s.h];
        int n = s.bands;
        for (int p = 0; p < argb.length; p++) {
            int o = p * n;
            int r = s.d[o] & 0xff;
            int g = n >= 3 ? s.d[o + 1] & 0xff : r;
            int b = n >= 3 ? s.d[o + 2] & 0xff : r;
            int a = n == 4 ? s.d[o + 3] & 0xff : n == 2 ? s.d[o + 1] & 0xff : 0xff;
            argb[p] = a << 24 | r << 16 | g << 8 | b;
        }
        return argb;
    }

    private static PyImageData bands(PyImageData rgba, int bands) {
        if (bands == 4) {
            return rgba;
        }
        PyImageData r = new PyImageData(rgba.w, rgba.h, bands);
        for (int p = 0; p < rgba.w * rgba.h; p++) {
            int red = rgba.d[p * 4] & 0xff;
            int green = rgba.d[p * 4 + 1] & 0xff;
            int blue = rgba.d[p * 4 + 2] & 0xff;
            if (bands == 3) {
                r.d[p * 3] = (byte) red;
                r.d[p * 3 + 1] = (byte) green;
                r.d[p * 3 + 2] = (byte) blue;
            } else {
                // ITU-R 601-2 luma, as Pillow's "L" conversion
                int l = (red * 19595 + green * 38470 + blue * 7471 + 0x8000) >> 16;
                r.d[p * bands] = (byte) l;
                if (bands == 2) {
                    r.d[p * 2 + 1] = rgba.d[p * 4 + 3];
                }
            }
        }
        return r;
    }

    @Override
    public PyImageData decode(byte[] b) {
        Image ci = Image.createImage(b, 0, b.length);
        return fromArgb(ci.getRGB(), ci.getWidth(), ci.getHeight());
    }

    @Override
    public byte[] encode(PyImageData s, String format, float quality) {
        Image ci = Image.createImage(toArgb(s), s.w, s.h);
        ImageIO io = ImageIO.getImageIO();
        if (io == null) {
            throw Py.error(Py.OS_ERROR, "this platform cannot encode " + format);
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try {
            io.save(ci, bo, format, quality);
        } catch (IOException e) {
            throw Py.error(Py.OS_ERROR, String.valueOf(e.getMessage())); // NOPMD PreserveStackTrace - surfaced as a Python OSError
        }
        return bo.toByteArray();
    }

    @Override
    public PyImageData resample(PyImageData s, int w, int h, double[] box, int filter, boolean premultiplied) {
        int x0 = (int) Math.floor(box[0]);
        int y0 = (int) Math.floor(box[1]);
        int x1 = (int) Math.ceil(box[2]);
        int y1 = (int) Math.ceil(box[3]);
        Image ci = Image.createImage(toArgb(s), s.w, s.h);
        if (x0 > 0 || y0 > 0 || x1 < s.w || y1 < s.h) {
            ci = ci.subImage(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0), true);
        }
        Image scaled = ci.scaled(w, h);
        return bands(fromArgb(scaled.getRGB(), w, h), s.bands);
    }

    @Override
    public byte[] pngUnfilter(byte[] data, int row, int h, int bpp) {
        throw Py.error(Py.OS_ERROR, "PNG files are decoded by the platform on this target");
    }

    @Override
    public PyImageData pngExpand(byte[] raw, int w, int h, int ctype, int depth) {
        throw Py.error(Py.OS_ERROR, "PNG files are decoded by the platform on this target");
    }

    @Override
    public PyImageData jpegDecode(byte[] data) {
        PyImageData rgba = decode(data);
        return bands(rgba, 3);
    }

    @Override
    public byte[] jpegEncode(PyImageData s, float quality) {
        return encode(s, ImageIO.FORMAT_JPEG, quality);
    }

    @Override
    public Object loadFont(byte[] data, int index, double size) {
        return defaultFont(size);
    }

    @Override
    public byte[] defaultFontData() {
        return new byte[0];
    }

    @Override
    public Object defaultFont(double size) {
        Font f = Font.createTrueTypeFont(NATIVE_FONT, NATIVE_FONT);
        return f.derive((float) size, Font.STYLE_PLAIN);
    }

    private static Font font(Object f) {
        if (f instanceof Font) {
            return (Font) f;
        }
        throw Py.typeError("not a font");
    }

    @Override
    public Object deriveFont(Object f, double size) {
        return font(f).derive((float) size, Font.STYLE_PLAIN);
    }

    @Override
    public String[] fontName(Object f) {
        font(f);
        return new String[] {"Native", "Regular"};
    }

    @Override
    public int[] fontMetrics(Object f) {
        Font ft = font(f);
        return new int[] {ft.getAscent(), ft.getDescent()};
    }

    @Override
    public long textLength(Object f, String text) {
        return font(f).stringWidth(text) * 64L;
    }

    @Override
    public int[] textBox(Object f, String text, String anchor) {
        Font ft = font(f);
        int w = ft.stringWidth(text);
        int asc = ft.getAscent();
        int h = asc + ft.getDescent();
        String a = anchor == null ? "la" : anchor;
        int x = a.charAt(0) == 'm' ? -w / 2 : a.charAt(0) == 'r' ? -w : 0;
        int y;
        switch (a.charAt(1)) {
            case 'm':
                y = -h / 2;
                break;
            case 's':
                y = -asc;
                break;
            case 'b':
            case 'd':
                y = -h;
                break;
            default:
                y = 0;
                break;
        }
        return new int[] {w, h, x, y};
    }

    @Override
    public PyImageData textMask(Object f, String text, String anchor, boolean mono) {
        Font ft = font(f);
        int[] b = textBox(ft, text, anchor);
        int w = Math.max(1, b[0]);
        int h = Math.max(1, b[1]);
        Image canvas = Image.createImage(w, h, 0xff000000);
        Graphics g = canvas.getGraphics();
        g.setAntiAliased(!mono);
        g.setAntiAliasedText(!mono);
        g.setFont(ft);
        g.setColor(0xffffff);
        g.drawString(text, 0, 0);
        int[] argb = canvas.getRGB();
        PyImageData r = new PyImageData(Math.max(0, b[0]), Math.max(0, b[1]), 1);
        for (int y = 0; y < r.h; y++) {
            for (int x = 0; x < r.w; x++) {
                int v = argb[y * w + x] >> 8 & 0xff;
                r.d[y * r.w + x] = (byte) (mono ? (v >= 128 ? 255 : 0) : v);
            }
        }
        return r;
    }
}
