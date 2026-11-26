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
package com.codename1.impl.pdf;

import com.codename1.io.gzip.InflaterInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/// Decodes a PNG to ARGB pixels without the UI layer, so a server can
/// place one in a PDF. Every color type, bit depth, the tRNS chunk and
/// Adam7 interlacing are supported; gamma and color profiles are ignored.
@com.codename1.impl.SharedWithBackend
public final class PngDecoder {
    private static final int[][] ADAM7 = {
        {0, 0, 8, 8}, {4, 0, 8, 8}, {0, 4, 4, 8}, {2, 0, 4, 4},
        {0, 2, 2, 4}, {1, 0, 2, 2}, {0, 1, 1, 2}};

    /// Width in pixels.
    public int width;
    /// Height in pixels.
    public int height;
    /// Pixels, row by row from the top, `0xAARRGGBB`.
    public int[] argb;
    /// True when any pixel is not fully opaque.
    public boolean hasAlpha;

    private int colorType;
    private int bitDepth;
    private int[] palette;
    private int[] paletteAlpha;
    private int transparentGray = -1;
    private int[] trnsRgb;

    /// True when `data` starts with the PNG signature.
    public static boolean isPng(byte[] data) {
        return data.length > 8 && (data[0] & 0xff) == 0x89 && data[1] == 'P'
                && data[2] == 'N' && data[3] == 'G';
    }

    /// Decodes `data`, throwing [IOException] for anything that is not a
    /// well formed PNG.
    public static PngDecoder decode(byte[] data) throws IOException {
        PngDecoder d = new PngDecoder();
        d.read(data);
        return d;
    }

    private void read(byte[] data) throws IOException {
        if (!isPng(data)) {
            throw new IOException("Not a PNG");
        }
        int pos = 8;
        int interlace = 0;
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        while (pos + 8 <= data.length) {
            int len = int32(data, pos);
            int type = int32(data, pos + 4);
            int body = pos + 8;
            if (len < 0 || body + len > data.length) {
                throw new IOException("Truncated PNG chunk");
            }
            if (type == 0x49484452) { // IHDR
                width = int32(data, body);
                height = int32(data, body + 4);
                bitDepth = data[body + 8] & 0xff;
                colorType = data[body + 9] & 0xff;
                interlace = data[body + 12] & 0xff;
            } else if (type == 0x504c5445) { // PLTE
                palette = new int[len / 3];
                for (int i = 0; i < palette.length; i++) {
                    palette[i] = ((data[body + i * 3] & 0xff) << 16)
                            | ((data[body + i * 3 + 1] & 0xff) << 8) | (data[body + i * 3 + 2] & 0xff);
                }
            } else if (type == 0x74524e53) { // tRNS
                if (colorType == 3) {
                    paletteAlpha = new int[len];
                    for (int i = 0; i < len; i++) {
                        paletteAlpha[i] = data[body + i] & 0xff;
                    }
                } else if (colorType == 0 && len >= 2) {
                    transparentGray = ((data[body] & 0xff) << 8) | (data[body + 1] & 0xff);
                } else if (colorType == 2 && len >= 6) {
                    trnsRgb = new int[] {sample16(data, body), sample16(data, body + 2), sample16(data, body + 4)};
                }
            } else if (type == 0x49444154) { // IDAT
                idat.write(data, body, len);
            } else if (type == 0x49454e44) { // IEND
                break;
            }
            pos = body + len + 4;
        }
        if (width <= 0 || height <= 0 || (long) width * height > 25000000L) {
            throw new IOException("Bad PNG size " + width + "x" + height);
        }
        // The bit depths the PNG specification allows; any other would
        // divide by zero or misread the rows.
        boolean depthOk = bitDepth == 8 || bitDepth == 16
                || (colorType == 0 || colorType == 3) && (bitDepth == 1 || bitDepth == 2 || bitDepth == 4);
        if (!depthOk || colorType == 3 && bitDepth == 16 || colorType == 1 || colorType == 5 || colorType > 6) {
            throw new IOException("Bad PNG format");
        }
        if (colorType == 3 && palette == null) {
            throw new IOException("Palette PNG without PLTE");
        }
        byte[] raw = inflate(idat.toByteArray());
        argb = new int[width * height];
        if (interlace == 0) {
            decodePass(raw, 0, 0, 0, 1, 1, width, height);
        } else {
            int off = 0;
            for (int p = 0; p < 7; p++) {
                int[] a = ADAM7[p];
                int pw = (width - a[0] + a[2] - 1) / a[2];
                int ph = (height - a[1] + a[3] - 1) / a[3];
                if (pw <= 0 || ph <= 0) {
                    continue;
                }
                off = decodePass(raw, off, a[0], a[1], a[2], a[3], pw, ph);
            }
        }
    }

    private int channels() {
        switch (colorType) {
            case 2:
                return 3;
            case 4:
                return 2;
            case 6:
                return 4;
            default:
                return 1;
        }
    }

    private int decodePass(byte[] raw, int off, int x0, int y0, int dx, int dy, int pw, int ph)
            throws IOException {
        int bitsPerPixel = channels() * bitDepth;
        int bpp = Math.max(1, bitsPerPixel / 8);
        int stride = (pw * bitsPerPixel + 7) / 8;
        byte[] prev = new byte[stride];
        byte[] cur = new byte[stride];
        for (int row = 0; row < ph; row++) {
            if (off + 1 + stride > raw.length) {
                throw new IOException("Truncated PNG data");
            }
            int filter = raw[off] & 0xff;
            System.arraycopy(raw, off + 1, cur, 0, stride);
            off += 1 + stride;
            unfilter(filter, cur, prev, bpp);
            int y = y0 + row * dy;
            for (int col = 0; col < pw; col++) {
                argb[y * width + x0 + col * dx] = pixel(cur, col);
            }
            byte[] t = prev;
            prev = cur;
            cur = t;
        }
        return off;
    }

    private static void unfilter(int filter, byte[] cur, byte[] prev, int bpp) throws IOException {
        int n = cur.length;
        switch (filter) {
            case 0:
                return;
            case 1:
                for (int i = bpp; i < n; i++) {
                    cur[i] = (byte) (cur[i] + cur[i - bpp]);
                }
                return;
            case 2:
                for (int i = 0; i < n; i++) {
                    cur[i] = (byte) (cur[i] + prev[i]);
                }
                return;
            case 3:
                for (int i = 0; i < n; i++) {
                    int left = i >= bpp ? cur[i - bpp] & 0xff : 0;
                    cur[i] = (byte) (cur[i] + ((left + (prev[i] & 0xff)) >> 1));
                }
                return;
            case 4:
                for (int i = 0; i < n; i++) {
                    int a = i >= bpp ? cur[i - bpp] & 0xff : 0;
                    int b = prev[i] & 0xff;
                    int c = i >= bpp ? prev[i - bpp] & 0xff : 0;
                    int p = a + b - c;
                    int pa = Math.abs(p - a);
                    int pb = Math.abs(p - b);
                    int pc = Math.abs(p - c);
                    int pred = (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c);
                    cur[i] = (byte) (cur[i] + pred);
                }
                return;
            default:
                throw new IOException("Bad PNG filter " + filter);
        }
    }

    private int sample(byte[] row, int index) {
        switch (bitDepth) {
            case 16:
                return ((row[index * 2] & 0xff) << 8) | (row[index * 2 + 1] & 0xff);
            case 8:
                return row[index] & 0xff;
            default:
                int bit = index * bitDepth;
                int b = row[bit >> 3] & 0xff;
                int shift = 8 - bitDepth - (bit & 7);
                return (b >> shift) & ((1 << bitDepth) - 1);
        }
    }

    private int to8(int v) {
        switch (bitDepth) {
            case 16:
                return v >> 8;
            case 8:
                return v;
            default:
                return v * 255 / ((1 << bitDepth) - 1);
        }
    }

    private int pixel(byte[] row, int col) {
        int a = 255;
        int r;
        int g;
        int b;
        switch (colorType) {
            case 0: {
                int v = sample(row, col);
                if (v == transparentGray) {
                    a = 0;
                }
                r = g = b = to8(v);
                break;
            }
            case 2: {
                int sr = sample(row, col * 3);
                int sg = sample(row, col * 3 + 1);
                int sb = sample(row, col * 3 + 2);
                if (trnsRgb != null && sr == trnsRgb[0] && sg == trnsRgb[1] && sb == trnsRgb[2]) {
                    a = 0;
                }
                r = to8(sr);
                g = to8(sg);
                b = to8(sb);
                break;
            }
            case 3: {
                int i = sample(row, col);
                int c = i < palette.length ? palette[i] : 0;
                if (paletteAlpha != null && i < paletteAlpha.length) {
                    a = paletteAlpha[i];
                }
                r = (c >> 16) & 0xff;
                g = (c >> 8) & 0xff;
                b = c & 0xff;
                break;
            }
            case 4:
                r = g = b = to8(sample(row, col * 2));
                a = to8(sample(row, col * 2 + 1));
                break;
            default:
                r = to8(sample(row, col * 4));
                g = to8(sample(row, col * 4 + 1));
                b = to8(sample(row, col * 4 + 2));
                a = to8(sample(row, col * 4 + 3));
        }
        if (a != 255) {
            hasAlpha = true;
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int sample16(byte[] d, int p) {
        return ((d[p] & 0xff) << 8) | (d[p + 1] & 0xff);
    }

    private static int int32(byte[] d, int p) {
        return ((d[p] & 0xff) << 24) | ((d[p + 1] & 0xff) << 16) | ((d[p + 2] & 0xff) << 8) | (d[p + 3] & 0xff);
    }

    /// Inflates zlib data completely.
    public static byte[] inflate(byte[] data) throws IOException {
        return inflate(data, PdfFilters.MAX_DECODED);
    }

    /// Inflates zlib data, stopping after `limit` bytes.
    public static byte[] inflate(byte[] data, int limit) throws IOException {
        InputStream in = new InflaterInputStream(new ByteArrayInputStream(data));
        ByteArrayOutputStream bo = new ByteArrayOutputStream(data.length * 3 + 64);
        byte[] buf = new byte[8192];
        try {
            int n;
            while ((n = in.read(buf)) > 0 && bo.size() < limit) {
                bo.write(buf, 0, n);
            }
        } finally {
            in.close();
        }
        return bo.toByteArray();
    }
}
