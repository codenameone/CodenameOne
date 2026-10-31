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
package com.codename1.backend.image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/// Reads and writes PNG files in pure Java.
///
/// [#decode(byte[])] accepts every PNG color type (gray, RGB, palette, gray
/// with alpha, RGBA), bit depths 1 to 16, `tRNS` transparency and Adam7
/// interlacing. 16-bit samples are reduced to their high byte. [#encode(RasterImage)]
/// writes 8-bit RGBA, or RGB when every pixel is opaque.
///
/// The lower-level [#unfilter(byte[], int, int, int, int)] and
/// [#expand(byte[], int, int, int, int)] keep the file's own sample layout
/// instead of converting to ARGB; the Python runtime's `PIL` uses them to
/// keep a palette image a palette image.
public final class PngCodec {
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
    private static final int[][] ADAM7 = {
        {0, 0, 8, 8}, {4, 0, 8, 8}, {0, 4, 4, 8}, {2, 0, 4, 4}, {0, 2, 2, 4}, {1, 0, 2, 2}, {0, 1, 1, 2}
    };

    private PngCodec() {
    }

    /// Returns true when `data` starts with the PNG signature.
    ///
    /// #### Parameters
    ///
    /// - `data`: file bytes
    ///
    /// #### Returns
    ///
    /// whether the data is a PNG file
    public static boolean isPng(byte[] data) {
        if (data == null || data.length < 8) {
            return false;
        }
        for (int i = 0; i < 8; i++) {
            if (data[i] != SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    private static int be32(byte[] d, int p) {
        return (d[p] & 0xff) << 24 | (d[p + 1] & 0xff) << 16 | (d[p + 2] & 0xff) << 8 | (d[p + 3] & 0xff);
    }

    /// Decodes a PNG file to a non-premultiplied ARGB image.
    ///
    /// #### Parameters
    ///
    /// - `data`: the file
    ///
    /// #### Returns
    ///
    /// the image
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the data is not a PNG file this codec can read
    public static RasterImage decode(byte[] data) throws IOException {
        if (!isPng(data)) {
            throw new IOException("not a PNG file");
        }
        int pos = 8;
        int w = 0;
        int h = 0;
        int depth = 0;
        int ctype = -1;
        int interlace = 0;
        byte[] plte = null;
        byte[] trns = null;
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        while (pos + 8 <= data.length) {
            int len = be32(data, pos);
            if (len < 0 || pos + 12 + len > data.length) {
                throw new IOException("truncated PNG chunk");
            }
            int body = pos + 8;
            String kind = new String(new char[] {(char) data[pos + 4], (char) data[pos + 5],
                    (char) data[pos + 6], (char) data[pos + 7]});
            if ("IHDR".equals(kind)) {
                w = be32(data, body);
                h = be32(data, body + 4);
                depth = data[body + 8] & 0xff;
                ctype = data[body + 9] & 0xff;
                interlace = data[body + 12] & 0xff;
            } else if ("PLTE".equals(kind)) {
                plte = slice(data, body, len);
            } else if ("tRNS".equals(kind)) {
                trns = slice(data, body, len);
            } else if ("IDAT".equals(kind)) {
                idat.write(data, body, len);
            } else if ("IEND".equals(kind)) {
                break;
            }
            pos += 12 + len;
        }
        if (ctype < 0 || w <= 0 || h <= 0) {
            throw new IOException("PNG file has no header");
        }
        int channels = channels(ctype);
        if (channels == 0 || (depth != 1 && depth != 2 && depth != 4 && depth != 8 && depth != 16)) {
            throw new IOException("unsupported PNG type " + ctype + "/" + depth);
        }
        byte[] raw = inflate(idat.toByteArray());
        int bpp = Math.max(1, channels * depth / 8);
        int[] argb = new int[w * h];
        if (interlace == 0) {
            int row = (w * channels * depth + 7) / 8;
            toArgb(expand(unfilter(raw, 0, row, h, bpp), w, h, ctype, depth), w, h, ctype, depth,
                    plte, trns, argb, 0, 0, 1, 1, w);
        } else {
            int off = 0;
            for (int[] a : ADAM7) {
                int pw = (w - a[0] + a[2] - 1) / a[2];
                int ph = (h - a[1] + a[3] - 1) / a[3];
                if (pw <= 0 || ph <= 0) {
                    continue;
                }
                int row = (pw * channels * depth + 7) / 8;
                toArgb(expand(unfilter(raw, off, row, ph, bpp), pw, ph, ctype, depth), pw, ph, ctype, depth,
                        plte, trns, argb, a[0], a[1], a[2], a[3], w);
                off += (row + 1) * ph;
            }
        }
        return new RasterImage(w, h, argb);
    }

    private static byte[] slice(byte[] d, int p, int n) {
        byte[] r = new byte[n];
        System.arraycopy(d, p, r, 0, n);
        return r;
    }

    private static int channels(int ctype) {
        switch (ctype) {
            case 0:
            case 3:
                return 1;
            case 2:
                return 3;
            case 4:
                return 2;
            case 6:
                return 4;
            default:
                return 0;
        }
    }

    private static byte[] inflate(byte[] z) throws IOException {
        Inflater inf = new Inflater();
        inf.setInput(z);
        ByteArrayOutputStream out = new ByteArrayOutputStream(z.length * 4 + 64);
        byte[] buf = new byte[8192];
        try {
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) {
                    break;
                }
                out.write(buf, 0, n);
            }
        } catch (DataFormatException e) {
            throw new IOException("corrupt PNG data: " + e.getMessage(), e);
        } finally {
            inf.end();
        }
        return out.toByteArray();
    }

    private static void toArgb(byte[] s, int w, int h, int ctype, int depth, byte[] plte, byte[] trns,
                               int[] out, int x0, int y0, int dx, int dy, int stride) {
        int bands = channels(ctype);
        int tr = -1;
        int tg = -1;
        int tb = -1;
        if (trns != null && ctype == 0 && trns.length >= 2) {
            tr = scaleKey(((trns[0] & 0xff) << 8 | (trns[1] & 0xff)), depth);
        } else if (trns != null && ctype == 2 && trns.length >= 6) {
            tr = scaleKey(((trns[0] & 0xff) << 8 | (trns[1] & 0xff)), depth);
            tg = scaleKey(((trns[2] & 0xff) << 8 | (trns[3] & 0xff)), depth);
            tb = scaleKey(((trns[4] & 0xff) << 8 | (trns[5] & 0xff)), depth);
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int o = (y * w + x) * bands;
                int c;
                switch (ctype) {
                    case 0: {
                        int g = s[o] & 0xff;
                        c = (g == tr ? 0 : 0xff000000) | g << 16 | g << 8 | g;
                        break;
                    }
                    case 2: {
                        int r = s[o] & 0xff;
                        int g = s[o + 1] & 0xff;
                        int b = s[o + 2] & 0xff;
                        c = (r == tr && g == tg && b == tb ? 0 : 0xff000000) | r << 16 | g << 8 | b;
                        break;
                    }
                    case 3: {
                        int i = s[o] & 0xff;
                        int a = trns != null && i < trns.length ? trns[i] & 0xff : 255;
                        int r = 0;
                        int g = 0;
                        int b = 0;
                        if (plte != null && i * 3 + 2 < plte.length) {
                            r = plte[i * 3] & 0xff;
                            g = plte[i * 3 + 1] & 0xff;
                            b = plte[i * 3 + 2] & 0xff;
                        }
                        c = a << 24 | r << 16 | g << 8 | b;
                        break;
                    }
                    case 4: {
                        int g = s[o] & 0xff;
                        c = (s[o + 1] & 0xff) << 24 | g << 16 | g << 8 | g;
                        break;
                    }
                    default:
                        c = (s[o + 3] & 0xff) << 24 | (s[o] & 0xff) << 16 | (s[o + 1] & 0xff) << 8 | (s[o + 2] & 0xff);
                        break;
                }
                out[(y0 + y * dy) * stride + x0 + x * dx] = c;
            }
        }
    }

    /// Maps a tRNS key to the scale [#expand(byte[], int, int, int, int)] produces.
    private static int scaleKey(int v, int depth) {
        if (depth == 16) {
            return v >> 8;
        }
        if (depth < 8) {
            return (v & ((1 << depth) - 1)) * (255 / ((1 << depth) - 1));
        }
        return v & 0xff;
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

    /// Reverses the five PNG row filters.
    ///
    /// #### Parameters
    ///
    /// - `data`: `h` rows, each a filter type byte followed by `row` bytes
    /// - `offset`: where the first row starts in `data`
    /// - `row`: bytes per row, without the filter byte
    /// - `h`: number of rows
    /// - `bpp`: the filter's byte distance, at least 1
    ///
    /// #### Returns
    ///
    /// the `row * h` unfiltered bytes
    ///
    /// #### Throws
    ///
    /// - `IOException`: on truncated data or an unknown filter type
    public static byte[] unfilter(byte[] data, int offset, int row, int h, int bpp) throws IOException {
        if (data.length - offset < (row + 1) * h) {
            throw new IOException("image file is truncated");
        }
        byte[] out = new byte[row * h];
        for (int y = 0; y < h; y++) {
            int ft = data[offset + y * (row + 1)] & 0xff;
            int in = offset + y * (row + 1) + 1;
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
                    default: throw new IOException("unknown PNG filter type " + ft);
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
        return (raw[rowStart + (bit >> 3)] & 0xff) >> (8 - depth - (bit & 7)) & ((1 << depth) - 1);
    }

    /// Unpacks unfiltered rows to one byte per sample: gray to 1 band, RGB to
    /// 3, gray with alpha to 2, RGBA to 4 and palette to its 1-band indices.
    /// Gray below 8 bits is scaled to 0..255, 16-bit samples keep their high
    /// byte.
    ///
    /// #### Parameters
    ///
    /// - `raw`: unfiltered rows
    /// - `w`: width
    /// - `h`: height
    /// - `ctype`: the PNG color type
    /// - `depth`: the PNG bit depth
    ///
    /// #### Returns
    ///
    /// `w * h * bands` bytes
    public static byte[] expand(byte[] raw, int w, int h, int ctype, int depth) {
        int channels = channels(ctype);
        int row = (w * channels * depth + 7) / 8;
        byte[] r = new byte[w * h * channels];
        int scale = depth < 8 ? 255 / ((1 << depth) - 1) : 1;
        for (int y = 0; y < h; y++) {
            int rs = y * row;
            for (int x = 0; x < w; x++) {
                int o = (y * w + x) * channels;
                if (ctype == 3) {
                    r[o] = (byte) sample(raw, rs, x, depth);
                    continue;
                }
                if (ctype == 4) {
                    r[o] = (byte) sample(raw, rs, x * 2, depth);
                    r[o + 1] = (byte) sample(raw, rs, x * 2 + 1, depth);
                    continue;
                }
                for (int b = 0; b < channels; b++) {
                    r[o + b] = (byte) (sample(raw, rs, x * channels + b, depth) * scale);
                }
            }
        }
        return r;
    }

    /// Encodes an image as an 8-bit PNG: RGBA, or RGB when it is opaque.
    ///
    /// #### Parameters
    ///
    /// - `img`: the image
    ///
    /// #### Returns
    ///
    /// the PNG file
    public static byte[] encode(RasterImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] px = img.getPixels();
        boolean opaque = true;
        for (int i = 0, n = w * h; i < n && opaque; i++) {
            opaque = px[i] >>> 24 == 255;
        }
        int bands = opaque ? 3 : 4;
        byte[] rows = new byte[(w * bands + 1) * h];
        int o = 0;
        for (int y = 0; y < h; y++) {
            rows[o++] = 0;
            for (int x = 0; x < w; x++) {
                int c = px[y * w + x];
                rows[o++] = (byte) (c >> 16);
                rows[o++] = (byte) (c >> 8);
                rows[o++] = (byte) c;
                if (!opaque) {
                    rows[o++] = (byte) (c >>> 24);
                }
            }
        }
        return encodeRows(w, h, 8, opaque ? 2 : 6, null, null, rows);
    }

    /// Assembles a PNG file from rows that already carry their filter bytes.
    ///
    /// #### Parameters
    ///
    /// - `w`: width
    /// - `h`: height
    /// - `depth`: bit depth
    /// - `ctype`: color type
    /// - `plte`: the `PLTE` chunk body, or null
    /// - `trns`: the `tRNS` chunk body, or null
    /// - `rows`: `h` rows, each a filter type byte and its samples
    ///
    /// #### Returns
    ///
    /// the PNG file
    public static byte[] encodeRows(int w, int h, int depth, int ctype, byte[] plte, byte[] trns, byte[] rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(rows.length / 2 + 128);
        out.write(SIGNATURE, 0, 8);
        byte[] ihdr = new byte[13];
        put32(ihdr, 0, w);
        put32(ihdr, 4, h);
        ihdr[8] = (byte) depth;
        ihdr[9] = (byte) ctype;
        chunk(out, "IHDR", ihdr);
        if (plte != null) {
            chunk(out, "PLTE", plte);
        }
        if (trns != null) {
            chunk(out, "tRNS", trns);
        }
        Deflater def = new Deflater(6);
        def.setInput(rows);
        def.finish();
        ByteArrayOutputStream z = new ByteArrayOutputStream(rows.length / 2 + 64);
        byte[] buf = new byte[8192];
        while (!def.finished()) {
            int n = def.deflate(buf);
            z.write(buf, 0, n);
        }
        def.end();
        chunk(out, "IDAT", z.toByteArray());
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void put32(byte[] d, int p, int v) {
        d[p] = (byte) (v >>> 24);
        d[p + 1] = (byte) (v >>> 16);
        d[p + 2] = (byte) (v >>> 8);
        d[p + 3] = (byte) v;
    }

    private static void chunk(ByteArrayOutputStream out, String kind, byte[] body) {
        byte[] head = new byte[8];
        put32(head, 0, body.length);
        for (int i = 0; i < 4; i++) {
            head[4 + i] = (byte) kind.charAt(i);
        }
        out.write(head, 0, 8);
        out.write(body, 0, body.length);
        CRC32 crc = new CRC32();
        crc.update(head, 4, 4);
        crc.update(body, 0, body.length);
        byte[] c = new byte[4];
        put32(c, 0, (int) crc.getValue());
        out.write(c, 0, 4);
    }
}
