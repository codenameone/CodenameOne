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

/// Reads and writes JPEG files in pure Java.
///
/// [#decode(byte[])] reads baseline and progressive Huffman-coded JPEG
/// (JFIF, Exif and Adobe files) with any chroma subsampling, restart
/// intervals, 8- and 16-bit quantization tables, gray, YCbCr and Adobe
/// CMYK/YCCK color. Arithmetic coding and 12-bit lossless JPEG are not
/// supported. Chroma is upsampled by replication, so a decode may differ from
/// libjpeg's by a few levels near sharp color edges.
///
/// [#encode(RasterImage, float, boolean)] writes baseline JPEG with the
/// standard tables of the JPEG specification, 4:4:4 (no chroma subsampling),
/// scaled to a quality the way libjpeg scales them, so quality 0.75 here
/// gives the same tables as libjpeg's (and Pillow's) quality 75. Alpha is
/// dropped.
public final class JpegCodec {
    static final int[] ZIGZAG = {
        0, 1, 8, 16, 9, 2, 3, 10, 17, 24, 32, 25, 18, 11, 4, 5, 12, 19, 26, 33, 40, 48, 41, 34, 27, 20, 13, 6, 7,
        14, 21, 28, 35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23, 30, 37, 44, 51, 58, 59, 52, 45, 38, 31, 39, 46,
        53, 60, 61, 54, 47, 55, 62, 63
    };

    private static final double[] COS = new double[64];

    static {
        for (int x = 0; x < 8; x++) {
            for (int u = 0; u < 8; u++) {
                double cu = u == 0 ? 1.0 / Math.sqrt(2.0) : 1.0;
                COS[x * 8 + u] = cu * Math.cos((2 * x + 1) * u * Math.PI / 16.0) / 2.0;
            }
        }
    }

    private JpegCodec() {
    }

    /// Returns true when `data` starts with a JPEG start-of-image marker.
    ///
    /// #### Parameters
    ///
    /// - `data`: file bytes
    ///
    /// #### Returns
    ///
    /// whether the data looks like a JPEG file
    public static boolean isJpeg(byte[] data) {
        return data != null && data.length > 3 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8
                && (data[2] & 0xff) == 0xff;
    }

    /// Returns the number of color components of a JPEG file: 1 for gray,
    /// 3 for color, 4 for CMYK, or 0 when no frame header is found.
    ///
    /// #### Parameters
    ///
    /// - `data`: the file
    ///
    /// #### Returns
    ///
    /// the component count
    public static int componentCount(byte[] data) {
        int p = 2;
        while (p + 4 <= data.length) {
            if ((data[p] & 0xff) != 0xff) {
                p++;
                continue;
            }
            int m = data[p + 1] & 0xff;
            if (m == 0xff) {
                p++;
                continue;
            }
            if (m == 0xd8 || m >= 0xd0 && m <= 0xd7 || m == 0x01) {
                p += 2;
                continue;
            }
            int len = (data[p + 2] & 0xff) << 8 | (data[p + 3] & 0xff);
            if (m >= 0xc0 && m <= 0xcf && m != 0xc4 && m != 0xc8 && m != 0xcc) {
                return p + 9 < data.length ? data[p + 9] & 0xff : 0;
            }
            p += 2 + len;
        }
        return 0;
    }

    // ------------------------------------------------------------------ decoder

    private static final class Huffman {
        final int[] maxcode = new int[18];
        final int[] valptr = new int[17];
        final int[] mincode = new int[17];
        final int[] vals;

        Huffman(byte[] counts, byte[] values) {
            vals = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                vals[i] = values[i] & 0xff;
            }
            int code = 0;
            int k = 0;
            for (int l = 1; l <= 16; l++) {
                int n = counts[l - 1] & 0xff;
                valptr[l] = k;
                mincode[l] = code;
                code += n;
                k += n;
                maxcode[l] = n > 0 ? code - 1 : -1;
                code <<= 1;
            }
            maxcode[17] = Integer.MAX_VALUE;
        }
    }

    private static final class Component {
        int id;
        int h;
        int v;
        int tq;
        int blocksPerLine;
        int blocksPerColumn;
        int blocksPerLineForMcu;
        int blocksPerColumnForMcu;
        int[] coef;
        int pred;
        Huffman dc;
        Huffman ac;
        byte[] plane;
        int planeWidth;
    }

    /// The decoder's state: one instance per file.
    private static final class Decoder {
        final byte[] d;
        int pos;
        int bits;
        int bitCount;
        final int[][] quant = new int[4][];
        final Huffman[] dcTables = new Huffman[4];
        final Huffman[] acTables = new Huffman[4];
        Component[] comps = new Component[0];
        boolean progressive;
        int width;
        int height;
        int maxH;
        int maxV;
        int mcusPerLine;
        int mcusPerColumn;
        int restart;
        int adobe = -1;
        int eobrun;
        int acState;
        int acNext;

        Decoder(byte[] d) {
            this.d = d;
        }

        int u16(int p) throws IOException {
            if (p + 1 >= d.length) {
                throw new IOException("JPEG file is truncated");
            }
            return (d[p] & 0xff) << 8 | (d[p + 1] & 0xff);
        }

        RasterImage run() throws IOException {
            pos = 2;
            boolean frame = false;
            while (pos < d.length) {
                if ((d[pos] & 0xff) != 0xff) {
                    pos++;
                    continue;
                }
                int m = d[pos + 1 < d.length ? pos + 1 : pos] & 0xff;
                if (m == 0xff) {
                    pos++;
                    continue;
                }
                if (m == 0xd9) {
                    break;
                }
                if (m == 0xd8 || m >= 0xd0 && m <= 0xd7 || m == 0x01 || m == 0x00) {
                    pos += 2;
                    continue;
                }
                int len = u16(pos + 2);
                int body = pos + 4;
                int end = pos + 2 + len;
                if (end > d.length) {
                    throw new IOException("JPEG file is truncated");
                }
                switch (m) {
                    case 0xdb:
                        readQuant(body, end);
                        break;
                    case 0xc4:
                        readHuffman(body, end);
                        break;
                    case 0xdd:
                        restart = u16(body);
                        break;
                    case 0xee:
                        if (len >= 12 && d[body] == 'A' && d[body + 1] == 'd' && d[body + 2] == 'o') {
                            adobe = d[body + 11] & 0xff;
                        }
                        break;
                    case 0xc0:
                    case 0xc1:
                    case 0xc2:
                        progressive = m == 0xc2;
                        readFrame(body);
                        frame = true;
                        break;
                    case 0xda:
                        if (!frame) {
                            throw new IOException("JPEG scan before frame header");
                        }
                        pos = readScan(body);
                        continue;
                    default:
                        if (m >= 0xc3 && m <= 0xcf && m != 0xc8 && m != 0xcc) {
                            throw new IOException("unsupported JPEG process (marker 0x" + Integer.toHexString(m) + ")");
                        }
                        break;
                }
                pos = end;
            }
            if (!frame) {
                throw new IOException("JPEG file has no frame");
            }
            return output();
        }

        void readQuant(int p, int end) throws IOException {
            while (p < end) {
                int pq = (d[p] & 0xff) >> 4;
                int tq = d[p] & 3;
                p++;
                int[] q = new int[64];
                for (int i = 0; i < 64; i++) {
                    if (pq == 0) {
                        q[ZIGZAG[i]] = d[p++] & 0xff;
                    } else {
                        q[ZIGZAG[i]] = u16(p);
                        p += 2;
                    }
                }
                quant[tq] = q;
            }
        }

        void readHuffman(int p, int end) {
            while (p + 17 <= end) {
                int tc = (d[p] & 0xff) >> 4;
                int th = d[p] & 3;
                byte[] counts = new byte[16];
                System.arraycopy(d, p + 1, counts, 0, 16);
                int total = 0;
                for (int i = 0; i < 16; i++) {
                    total += counts[i] & 0xff;
                }
                byte[] vals = new byte[total];
                System.arraycopy(d, p + 17, vals, 0, Math.min(total, d.length - p - 17));
                Huffman t = new Huffman(counts, vals);
                if (tc == 0) {
                    dcTables[th] = t;
                } else {
                    acTables[th] = t;
                }
                p += 17 + total;
            }
        }

        void readFrame(int p) throws IOException {
            height = u16(p + 1);
            width = u16(p + 3);
            int n = d[p + 5] & 0xff;
            if (width <= 0 || height <= 0 || n == 0 || n > 4) {
                throw new IOException("unsupported JPEG frame " + width + "x" + height + "x" + n);
            }
            comps = new Component[n];
            maxH = 1;
            maxV = 1;
            for (int i = 0; i < n; i++) {
                Component c = new Component();
                int q = p + 6 + i * 3;
                c.id = d[q] & 0xff;
                c.h = Math.max(1, (d[q + 1] & 0xff) >> 4);
                c.v = Math.max(1, d[q + 1] & 15);
                c.tq = d[q + 2] & 3;
                maxH = Math.max(maxH, c.h);
                maxV = Math.max(maxV, c.v);
                comps[i] = c;
            }
            mcusPerLine = (width + 8 * maxH - 1) / (8 * maxH);
            mcusPerColumn = (height + 8 * maxV - 1) / (8 * maxV);
            for (int i = 0; i < n; i++) {
                Component c = comps[i];
                c.blocksPerLine = ((width * c.h + maxH - 1) / maxH + 7) / 8;
                c.blocksPerColumn = ((height * c.v + maxV - 1) / maxV + 7) / 8;
                c.blocksPerLineForMcu = mcusPerLine * c.h;
                c.blocksPerColumnForMcu = mcusPerColumn * c.v;
                c.coef = new int[c.blocksPerLineForMcu * c.blocksPerColumnForMcu * 64];
            }
        }

        int readBit() {
            if (bitCount == 0) {
                if (pos >= d.length) {
                    return 0;
                }
                int b = d[pos] & 0xff;
                if (b == 0xff) {
                    int next = pos + 1 < d.length ? d[pos + 1] & 0xff : 0xd9;
                    if (next != 0) {
                        // A marker: the scan's data has ended; feed zeros.
                        return 0;
                    }
                    pos += 2;
                } else {
                    pos++;
                }
                bits = b;
                bitCount = 8;
            }
            bitCount--;
            return bits >> bitCount & 1;
        }

        int receive(int n) {
            int v = 0;
            for (int i = 0; i < n; i++) {
                v = v << 1 | readBit();
            }
            return v;
        }

        int extend(int n) {
            if (n == 0) {
                return 0;
            }
            int v = receive(n);
            return v >= 1 << (n - 1) ? v : v - (1 << n) + 1;
        }

        int decode(Huffman t) throws IOException {
            if (t == null) {
                throw new IOException("JPEG scan uses an undefined Huffman table");
            }
            int code = 0;
            for (int l = 1; l <= 16; l++) {
                code = code << 1 | readBit();
                if (code <= t.maxcode[l]) {
                    int idx = t.valptr[l] + code - t.mincode[l];
                    return idx < t.vals.length ? t.vals[idx] : 0;
                }
            }
            return 0;
        }

        int readScan(int p) throws IOException {
            int n = d[p] & 0xff;
            Component[] sc = new Component[n];
            int q = p + 1;
            for (int i = 0; i < n; i++) {
                int id = d[q] & 0xff;
                int tables = d[q + 1] & 0xff;
                q += 2;
                for (Component cand : comps) {
                    if (cand.id == id) {
                        sc[i] = cand;
                    }
                }
                if (sc[i] == null) {
                    throw new IOException("JPEG scan names an unknown component");
                }
                sc[i].dc = dcTables[tables >> 4 & 3];
                sc[i].ac = acTables[tables & 3];
            }
            int ss = d[q] & 0xff;
            int se = d[q + 1] & 0xff;
            int ah = (d[q + 2] & 0xff) >> 4;
            int al = d[q + 2] & 15;
            pos = q + 3;
            bitCount = 0;
            int total = n == 1 ? sc[0].blocksPerLine * sc[0].blocksPerColumn : mcusPerLine * mcusPerColumn;
            int mcu = 0;
            while (mcu < total) {
                for (int i = 0; i < n; i++) {
                    sc[i].pred = 0;
                }
                eobrun = 0;
                acState = 0;
                int count = restart > 0 ? Math.min(restart, total - mcu) : total - mcu;
                for (int i = 0; i < count; i++) {
                    if (n == 1) {
                        Component c = sc[0];
                        int row = mcu / c.blocksPerLine;
                        int col = mcu % c.blocksPerLine;
                        block(c, 64 * (row * c.blocksPerLineForMcu + col), ss, se, ah, al);
                    } else {
                        int mr = mcu / mcusPerLine;
                        int mc = mcu % mcusPerLine;
                        for (int k = 0; k < n; k++) {
                            Component c = sc[k];
                            for (int y = 0; y < c.v; y++) {
                                for (int x = 0; x < c.h; x++) {
                                    int row = mr * c.v + y;
                                    int col = mc * c.h + x;
                                    block(c, 64 * (row * c.blocksPerLineForMcu + col), ss, se, ah, al);
                                }
                            }
                        }
                    }
                    mcu++;
                }
                bitCount = 0;
                while (pos + 1 < d.length && !((d[pos] & 0xff) == 0xff && (d[pos + 1] & 0xff) != 0
                        && (d[pos + 1] & 0xff) != 0xff)) {
                    pos++;
                }
                if (pos + 1 >= d.length) {
                    break;
                }
                int m = d[pos + 1] & 0xff;
                if (m >= 0xd0 && m <= 0xd7) {
                    pos += 2;
                } else {
                    break;
                }
            }
            return pos;
        }

        void block(Component c, int off, int ss, int se, int ah, int al) throws IOException {
            int[] z = c.coef;
            if (!progressive) {
                int t = decode(c.dc);
                c.pred += extend(t);
                z[off] = c.pred;
                int k = 1;
                while (k < 64) {
                    int rs = decode(c.ac);
                    int s = rs & 15;
                    int r = rs >> 4;
                    if (s == 0) {
                        if (r < 15) {
                            break;
                        }
                        k += 16;
                        continue;
                    }
                    k += r;
                    if (k > 63) {
                        break;
                    }
                    z[off + ZIGZAG[k]] = extend(s);
                    k++;
                }
                return;
            }
            if (ss == 0) {
                if (ah == 0) {
                    int t = decode(c.dc);
                    c.pred += extend(t) << al;
                    z[off] = c.pred;
                } else if (readBit() != 0) {
                    z[off] |= 1 << al;
                }
                return;
            }
            if (ah == 0) {
                acFirst(c, z, off, ss, se, al);
            } else {
                acSuccessive(c, z, off, ss, se, al);
            }
        }

        void acFirst(Component c, int[] z, int off, int ss, int se, int al) throws IOException {
            if (eobrun > 0) {
                eobrun--;
                return;
            }
            int k = ss;
            while (k <= se) {
                int rs = decode(c.ac);
                int s = rs & 15;
                int r = rs >> 4;
                if (s == 0) {
                    if (r < 15) {
                        eobrun = receive(r) + (1 << r) - 1;
                        break;
                    }
                    k += 16;
                    continue;
                }
                k += r;
                if (k > 63) {
                    break;
                }
                z[off + ZIGZAG[k]] = extend(s) * (1 << al);
                k++;
            }
        }

        void acSuccessive(Component c, int[] z, int off, int ss, int se, int al) throws IOException {
            int k = ss;
            int r = 0;
            while (k <= se) {
                int o = off + ZIGZAG[k];
                int sign = z[o] < 0 ? -1 : 1;
                switch (acState) {
                    case 0: {
                        int rs = decode(c.ac);
                        int s = rs & 15;
                        r = rs >> 4;
                        if (s == 0) {
                            if (r < 15) {
                                eobrun = receive(r) + (1 << r);
                                acState = 4;
                            } else {
                                r = 16;
                                acState = 1;
                            }
                        } else {
                            acNext = extend(s);
                            acState = r != 0 ? 2 : 3;
                        }
                        continue;
                    }
                    case 1:
                    case 2:
                        if (z[o] != 0) {
                            z[o] += sign * (readBit() << al);
                        } else {
                            r--;
                            if (r == 0) {
                                acState = acState == 2 ? 3 : 0;
                            }
                        }
                        break;
                    case 3:
                        if (z[o] != 0) {
                            z[o] += sign * (readBit() << al);
                        } else {
                            z[o] = acNext << al;
                            acState = 0;
                        }
                        break;
                    default:
                        if (z[o] != 0) {
                            z[o] += sign * (readBit() << al);
                        }
                        break;
                }
                k++;
            }
            if (acState == 4) {
                eobrun--;
                if (eobrun == 0) {
                    acState = 0;
                }
            }
        }

        RasterImage output() throws IOException {
            double[] tmp = new double[64];
            int[] out = new int[64];
            for (Component c : comps) {
                int[] q = quant[c.tq];
                if (q == null) {
                    throw new IOException("JPEG component uses an undefined quantization table");
                }
                int pw = c.blocksPerLineForMcu * 8;
                c.planeWidth = pw;
                c.plane = new byte[pw * c.blocksPerColumnForMcu * 8];
                for (int br = 0; br < c.blocksPerColumnForMcu; br++) {
                    for (int bc = 0; bc < c.blocksPerLineForMcu; bc++) {
                        int off = 64 * (br * c.blocksPerLineForMcu + bc);
                        idct(c.coef, off, q, tmp, out);
                        for (int y = 0; y < 8; y++) {
                            int base = (br * 8 + y) * pw + bc * 8;
                            for (int x = 0; x < 8; x++) {
                                c.plane[base + x] = (byte) out[y * 8 + x];
                            }
                        }
                    }
                }
                c.coef = null;
            }
            int[] px = new int[width * height];
            int n = comps.length;
            boolean transform = n == 3 ? adobe != 0 : n == 4 && adobe == 2;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int c0 = sampleAt(comps[0], x, y);
                    int argb;
                    if (n == 1) {
                        argb = 0xff000000 | c0 << 16 | c0 << 8 | c0;
                    } else if (n == 2) {
                        argb = 0xff000000 | c0 << 16 | c0 << 8 | c0;
                    } else {
                        int c1 = sampleAt(comps[1], x, y);
                        int c2 = sampleAt(comps[2], x, y);
                        int r = c0;
                        int g = c1;
                        int b = c2;
                        if (transform) {
                            double yy = c0;
                            double cb = c1 - 128;
                            double cr = c2 - 128;
                            r = clamp((int) Math.floor(yy + 1.402 * cr + 0.5));
                            g = clamp((int) Math.floor(yy - 0.344136 * cb - 0.714136 * cr + 0.5));
                            b = clamp((int) Math.floor(yy + 1.772 * cb + 0.5));
                        }
                        if (n == 4) {
                            int k = sampleAt(comps[3], x, y);
                            if (adobe >= 0) {
                                r = r * k / 255;
                                g = g * k / 255;
                                b = b * k / 255;
                            } else {
                                r = (255 - r) * (255 - k) / 255;
                                g = (255 - g) * (255 - k) / 255;
                                b = (255 - b) * (255 - k) / 255;
                            }
                        }
                        argb = 0xff000000 | r << 16 | g << 8 | b;
                    }
                    px[y * width + x] = argb;
                }
            }
            return new RasterImage(width, height, px);
        }

        int sampleAt(Component c, int x, int y) {
            int sx = x * c.h / maxH;
            int sy = y * c.v / maxV;
            return c.plane[sy * c.planeWidth + sx] & 0xff;
        }
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }

    private static void idct(int[] coef, int off, int[] q, double[] tmp, int[] out) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                double s = 0;
                for (int u = 0; u < 8; u++) {
                    s += COS[x * 8 + u] * coef[off + y * 8 + u] * q[y * 8 + u];
                }
                tmp[y * 8 + x] = s;
            }
        }
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                double s = 0;
                for (int v = 0; v < 8; v++) {
                    s += COS[y * 8 + v] * tmp[v * 8 + x];
                }
                out[y * 8 + x] = clamp((int) Math.floor(s + 128.5));
            }
        }
    }

    /// Decodes a JPEG file to an opaque ARGB image.
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
    /// - `IOException`: when the data is not a JPEG file this codec can read
    public static RasterImage decode(byte[] data) throws IOException {
        if (!isJpeg(data)) {
            throw new IOException("not a JPEG file");
        }
        try {
            return new Decoder(data).run();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException("corrupt JPEG file", e);
        }
    }

    // ------------------------------------------------------------------ encoder

    private static final int[] STD_LUMA_Q = {
        16, 11, 10, 16, 24, 40, 51, 61, 12, 12, 14, 19, 26, 58, 60, 55, 14, 13, 16, 24, 40, 57, 69, 56,
        14, 17, 22, 29, 51, 87, 80, 62, 18, 22, 37, 56, 68, 109, 103, 77, 24, 35, 55, 64, 81, 104, 113, 92,
        49, 64, 78, 87, 103, 121, 120, 101, 72, 92, 95, 98, 112, 100, 103, 99
    };
    private static final int[] STD_CHROMA_Q = {
        17, 18, 24, 47, 99, 99, 99, 99, 18, 21, 26, 66, 99, 99, 99, 99, 24, 26, 56, 99, 99, 99, 99, 99,
        47, 66, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99
    };
    private static final int[] DC_LUMA_BITS = {0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0};
    private static final int[] DC_CHROMA_BITS = {0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
    private static final int[] DC_VALS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] AC_LUMA_BITS = {0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 0x7d};
    private static final int[] AC_LUMA_VALS = {
        0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12, 0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07,
        0x22, 0x71, 0x14, 0x32, 0x81, 0x91, 0xa1, 0x08, 0x23, 0x42, 0xb1, 0xc1, 0x15, 0x52, 0xd1, 0xf0,
        0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0a, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x25, 0x26, 0x27, 0x28,
        0x29, 0x2a, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49,
        0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69,
        0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89,
        0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7,
        0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5,
        0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe1, 0xe2,
        0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
        0xf9, 0xfa
    };
    private static final int[] AC_CHROMA_BITS = {0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 0x77};
    private static final int[] AC_CHROMA_VALS = {
        0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21, 0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71,
        0x13, 0x22, 0x32, 0x81, 0x08, 0x14, 0x42, 0x91, 0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33, 0x52, 0xf0,
        0x15, 0x62, 0x72, 0xd1, 0x0a, 0x16, 0x24, 0x34, 0xe1, 0x25, 0xf1, 0x17, 0x18, 0x19, 0x1a, 0x26,
        0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
        0x49, 0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68,
        0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
        0x88, 0x89, 0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5,
        0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3,
        0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda,
        0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
        0xf9, 0xfa
    };

    /// Huffman codes by symbol: `codes[sym]` and `sizes[sym]`.
    private static int[][] codeTable(int[] bits, int[] vals) {
        int[] codes = new int[256];
        int[] sizes = new int[256];
        int code = 0;
        int k = 0;
        for (int l = 1; l <= 16; l++) {
            for (int i = 0; i < bits[l - 1]; i++) {
                codes[vals[k]] = code;
                sizes[vals[k]] = l;
                k++;
                code++;
            }
            code <<= 1;
        }
        return new int[][] {codes, sizes};
    }

    private static final class BitWriter {
        final ByteArrayOutputStream out;
        int acc;
        int count;

        BitWriter(ByteArrayOutputStream out) {
            this.out = out;
        }

        void write(int value, int n) {
            for (int i = n - 1; i >= 0; i--) {
                acc = acc << 1 | (value >> i & 1);
                count++;
                if (count == 8) {
                    out.write(acc);
                    if (acc == 0xff) {
                        out.write(0);
                    }
                    acc = 0;
                    count = 0;
                }
            }
        }

        void flush() {
            while (count != 0) {
                write(1, 1);
            }
        }
    }

    private static int[] scaledTable(int[] std, int quality) {
        int scale = quality < 50 ? 5000 / quality : 200 - quality * 2;
        int[] q = new int[64];
        for (int i = 0; i < 64; i++) {
            int v = (std[i] * scale + 50) / 100;
            q[i] = v < 1 ? 1 : v > 255 ? 255 : v;
        }
        return q;
    }

    /// Encodes an image as baseline color JPEG.
    ///
    /// #### Parameters
    ///
    /// - `img`: the image; alpha is ignored
    /// - `quality`: 0 to 1, as libjpeg's 0 to 100 divided by 100
    ///
    /// #### Returns
    ///
    /// the JPEG file
    public static byte[] encode(RasterImage img, float quality) {
        return encode(img, quality, false);
    }

    /// Encodes an image as baseline JPEG.
    ///
    /// #### Parameters
    ///
    /// - `img`: the image; alpha is ignored
    /// - `quality`: 0 to 1, as libjpeg's 0 to 100 divided by 100
    /// - `grayscale`: true to write one gray component (the luma of each pixel)
    ///   instead of three
    ///
    /// #### Returns
    ///
    /// the JPEG file
    public static byte[] encode(RasterImage img, float quality, boolean grayscale) {
        int qi = (int) Math.floor(quality * 100 + 0.5);
        qi = qi < 1 ? 1 : qi > 100 ? 100 : qi;
        int[] ql = scaledTable(STD_LUMA_Q, qi);
        int[] qc = scaledTable(STD_CHROMA_Q, qi);
        int w = img.getWidth();
        int h = img.getHeight();
        int nc = grayscale ? 1 : 3;
        ByteArrayOutputStream out = new ByteArrayOutputStream(w * h / 4 + 1024);
        out.write(0xff);
        out.write(0xd8);
        byte[] jfif = {0x4a, 0x46, 0x49, 0x46, 0, 1, 1, 0, 0, 1, 0, 1, 0, 0};
        segment(out, 0xe0, jfif);
        byte[] dqt = new byte[grayscale ? 65 : 130];
        dqt[0] = 0;
        for (int i = 0; i < 64; i++) {
            dqt[1 + i] = (byte) ql[ZIGZAG[i]];
        }
        if (!grayscale) {
            dqt[65] = 1;
            for (int i = 0; i < 64; i++) {
                dqt[66 + i] = (byte) qc[ZIGZAG[i]];
            }
        }
        segment(out, 0xdb, dqt);
        byte[] sof = new byte[6 + nc * 3];
        sof[0] = 8;
        sof[1] = (byte) (h >> 8);
        sof[2] = (byte) h;
        sof[3] = (byte) (w >> 8);
        sof[4] = (byte) w;
        sof[5] = (byte) nc;
        for (int i = 0; i < nc; i++) {
            sof[6 + i * 3] = (byte) (i + 1);
            sof[7 + i * 3] = 0x11;
            sof[8 + i * 3] = (byte) (i == 0 ? 0 : 1);
        }
        segment(out, 0xc0, sof);
        ByteArrayOutputStream dht = new ByteArrayOutputStream();
        huffSegment(dht, 0x00, DC_LUMA_BITS, DC_VALS);
        huffSegment(dht, 0x10, AC_LUMA_BITS, AC_LUMA_VALS);
        if (!grayscale) {
            huffSegment(dht, 0x01, DC_CHROMA_BITS, DC_VALS);
            huffSegment(dht, 0x11, AC_CHROMA_BITS, AC_CHROMA_VALS);
        }
        segment(out, 0xc4, dht.toByteArray());
        byte[] sos = new byte[4 + nc * 2];
        sos[0] = (byte) nc;
        for (int i = 0; i < nc; i++) {
            sos[1 + i * 2] = (byte) (i + 1);
            sos[2 + i * 2] = (byte) (i == 0 ? 0x00 : 0x11);
        }
        sos[2 + nc * 2] = 63;
        segment(out, 0xda, sos);
        int[][] dcl = codeTable(DC_LUMA_BITS, DC_VALS);
        int[][] acl = codeTable(AC_LUMA_BITS, AC_LUMA_VALS);
        int[][] dcc = codeTable(DC_CHROMA_BITS, DC_VALS);
        int[][] acc = codeTable(AC_CHROMA_BITS, AC_CHROMA_VALS);
        BitWriter bw = new BitWriter(out);
        int[] px = img.getPixels();
        double[] yb = new double[64];
        double[] cbb = new double[64];
        double[] crb = new double[64];
        double[] tmp = new double[64];
        int[] pred = new int[3];
        for (int by = 0; by < h; by += 8) {
            for (int bx = 0; bx < w; bx += 8) {
                for (int y = 0; y < 8; y++) {
                    int sy = Math.min(by + y, h - 1);
                    for (int x = 0; x < 8; x++) {
                        int c = px[sy * w + Math.min(bx + x, w - 1)];
                        int r = c >> 16 & 0xff;
                        int g = c >> 8 & 0xff;
                        int b = c & 0xff;
                        yb[y * 8 + x] = 0.299 * r + 0.587 * g + 0.114 * b - 128;
                        cbb[y * 8 + x] = -0.168736 * r - 0.331264 * g + 0.5 * b;
                        crb[y * 8 + x] = 0.5 * r - 0.418688 * g - 0.081312 * b;
                    }
                }
                pred[0] = encodeBlock(bw, yb, tmp, ql, pred[0], dcl, acl);
                if (!grayscale) {
                    pred[1] = encodeBlock(bw, cbb, tmp, qc, pred[1], dcc, acc);
                    pred[2] = encodeBlock(bw, crb, tmp, qc, pred[2], dcc, acc);
                }
            }
        }
        bw.flush();
        out.write(0xff);
        out.write(0xd9);
        return out.toByteArray();
    }

    private static void segment(ByteArrayOutputStream out, int marker, byte[] body) {
        out.write(0xff);
        out.write(marker);
        out.write((body.length + 2) >> 8);
        out.write(body.length + 2);
        out.write(body, 0, body.length);
    }

    private static void huffSegment(ByteArrayOutputStream out, int tcth, int[] bits, int[] vals) {
        out.write(tcth);
        for (int i = 0; i < 16; i++) {
            out.write(bits[i]);
        }
        for (int v : vals) {
            out.write(v);
        }
    }

    private static int category(int v) {
        int a = v < 0 ? -v : v;
        int n = 0;
        while (a != 0) {
            n++;
            a >>= 1;
        }
        return n;
    }

    private static void emit(BitWriter bw, int v, int n) {
        bw.write(v < 0 ? v + (1 << n) - 1 : v, n);
    }

    private static int encodeBlock(BitWriter bw, double[] blk, double[] tmp, int[] q, int pred, int[][] dc,
                                   int[][] ac) {
        // Forward DCT, the transpose of the decoder's inverse.
        for (int y = 0; y < 8; y++) {
            for (int u = 0; u < 8; u++) {
                double s = 0;
                for (int x = 0; x < 8; x++) {
                    s += COS[x * 8 + u] * blk[y * 8 + x];
                }
                tmp[y * 8 + u] = s;
            }
        }
        int[] zz = new int[64];
        for (int u = 0; u < 8; u++) {
            for (int v = 0; v < 8; v++) {
                double s = 0;
                for (int y = 0; y < 8; y++) {
                    s += COS[y * 8 + v] * tmp[y * 8 + u];
                }
                int idx = v * 8 + u;
                zz[idx] = (int) Math.floor(s / q[idx] + 0.5);
            }
        }
        int diff = zz[0] - pred;
        int cat = category(diff);
        bw.write(dc[0][cat], dc[1][cat]);
        emit(bw, diff, cat);
        int run = 0;
        for (int k = 1; k < 64; k++) {
            int v = zz[ZIGZAG[k]];
            if (v == 0) {
                run++;
                continue;
            }
            while (run > 15) {
                bw.write(ac[0][0xf0], ac[1][0xf0]);
                run -= 16;
            }
            int s = category(v);
            int sym = run << 4 | s;
            bw.write(ac[0][sym], ac[1][sym]);
            emit(bw, v, s);
            run = 0;
        }
        if (run > 0) {
            bw.write(ac[0][0], ac[1][0]);
        }
        return zz[0];
    }
}
