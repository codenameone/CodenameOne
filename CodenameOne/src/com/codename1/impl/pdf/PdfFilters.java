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

import com.codename1.io.gzip.Inflater;
import com.codename1.io.gzip.JZlib;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The stream filters. Image codecs (`DCTDecode`, `JPXDecode`,
/// `CCITTFaxDecode`, `JBIG2Decode`) are not undone here: decoding stops at
/// the first of them and the image code receives its encoded data.
@com.codename1.impl.SharedWithBackend
public final class PdfFilters {
    private PdfFilters() {
    }

    /// The filter names of `s`, normalized from their abbreviations.
    public static List<String> names(PdfDict s) {
        List<String> out = new ArrayList<String>();
        Object f = s.get("Filter");
        if (f == null) {
            f = s.get("F");
        }
        if (f instanceof PdfName) {
            out.add(full(((PdfName) f).value));
        } else if (f instanceof List) {
            List<?> l = (List<?>) f;
            for (int i = 0; i < l.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                Object o = s.file != null ? s.file.resolve(l.get(i)) : l.get(i);
                if (o instanceof PdfName) {
                    out.add(full(((PdfName) o).value));
                }
            }
        }
        return out;
    }

    private static String full(String n) {
        if ("Fl".equals(n)) {
            return "FlateDecode";
        }
        if ("AHx".equals(n)) {
            return "ASCIIHexDecode";
        }
        if ("A85".equals(n)) {
            return "ASCII85Decode";
        }
        if ("LZW".equals(n)) {
            return "LZWDecode";
        }
        if ("RL".equals(n)) {
            return "RunLengthDecode";
        }
        if ("DCT".equals(n)) {
            return "DCTDecode";
        }
        if ("CCF".equals(n)) {
            return "CCITTFaxDecode";
        }
        return n;
    }

    /// True for a filter whose output is an image rather than bytes.
    public static boolean isImageCodec(String n) {
        return "DCTDecode".equals(n) || "JPXDecode".equals(n) || "CCITTFaxDecode".equals(n)
                || "JBIG2Decode".equals(n);
    }

    static PdfDict params(PdfDict s, int index) {
        Object p = s.get("DecodeParms");
        if (p == null) {
            p = s.get("DP");
        }
        if (p instanceof List) {
            List<?> l = (List<?>) p;
            p = index < l.size() ? l.get(index) : null;
            if (s.file != null) {
                p = s.file.resolve(p);
            }
        }
        return p instanceof PdfDict ? (PdfDict) p : null;
    }

    /// Undoes every non-image filter of `s`, in order.
    public static byte[] decode(PdfStream s) throws IOException {
        byte[] data = s.raw;
        List<String> names = names(s);
        for (int i = 0; i < names.size(); i++) {
            String n = names.get(i);
            if (isImageCodec(n)) {
                break;
            }
            PdfDict p = params(s, i);
            if ("FlateDecode".equals(n)) {
                data = predict(inflate(data), p);
            } else if ("LZWDecode".equals(n)) {
                data = predict(lzw(data, p == null ? 1 : p.getInt("EarlyChange", 1)), p);
            } else if ("ASCIIHexDecode".equals(n)) {
                data = asciiHex(data);
            } else if ("ASCII85Decode".equals(n)) {
                data = ascii85(data);
            } else if ("RunLengthDecode".equals(n)) {
                data = runLength(data);
            } else if (!"Crypt".equals(n)) {
                throw new IOException("Unsupported filter " + n);
            }
        }
        return data;
    }

    /// Inflates zlib (or, failing that, raw deflate) data, keeping what was
    /// decoded before an error; truncated streams are common in the wild.
    public static byte[] inflate(byte[] in) {
        byte[] out = inflate(in, false);
        if (out.length == 0 && in.length > 2) {
            out = inflate(in, true);
        }
        return out;
    }

    /// The most any one filter may produce. A few kilobytes of deflated
    /// zeros expand to gigabytes, so a decoder stops here rather than run the
    /// process out of memory; the truncated output is treated like any other
    /// damaged stream.
    static final int MAX_DECODED = 1 << 27;

    private static byte[] inflate(byte[] in, boolean nowrap) {
        Inflater inf = new Inflater();
        inf.init(15, nowrap);
        inf.setInput(in, 0, in.length, false);
        ByteArrayOutputStream bo = new ByteArrayOutputStream((int) Math.min(MAX_DECODED, in.length * 4L + 64));
        byte[] buf = new byte[16384];
        while (true) {
            inf.setOutput(buf, 0, buf.length);
            int err = inf.inflate(JZlib.Z_NO_FLUSH);
            int n = buf.length - inf.getAvailOut();
            bo.write(buf, 0, n);
            if (err != JZlib.Z_OK || n == 0 && inf.getAvailIn() == 0 || bo.size() >= MAX_DECODED) {
                break;
            }
        }
        inf.end();
        return bo.toByteArray();
    }

    /// Undoes a TIFF (2) or PNG (10-15) predictor.
    public static byte[] predict(byte[] data, PdfDict p) throws IOException {
        if (p == null) {
            return data;
        }
        int predictor = p.getInt("Predictor", 1);
        if (predictor < 2) {
            return data;
        }
        int colors = Math.max(1, p.getInt("Colors", 1));
        int bits = Math.max(1, p.getInt("BitsPerComponent", 8));
        int columns = Math.max(1, p.getInt("Columns", 1));
        int bpp = Math.max(1, colors * bits / 8);
        long wide = ((long) columns * colors * bits + 7) / 8;
        if (wide > data.length + 1L) {
            // A row wider than the whole stream cannot be real.
            return data;
        }
        int stride = (int) wide;
        if (predictor == 2) {
            if (bits != 8) {
                return data;
            }
            byte[] out = new byte[data.length];
            System.arraycopy(data, 0, out, 0, data.length);
            for (int row = 0; row + stride <= out.length; row += stride) {
                for (int i = bpp; i < stride; i++) {
                    out[row + i] = (byte) (out[row + i] + out[row + i - bpp]);
                }
            }
            return out;
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream(data.length);
        byte[] prev = new byte[stride];
        byte[] cur = new byte[stride];
        int pos = 0;
        while (pos < data.length) {
            int filter = data[pos++] & 0xff;
            int n = Math.min(stride, data.length - pos);
            System.arraycopy(data, pos, cur, 0, n);
            for (int i = n; i < stride; i++) {
                cur[i] = 0;
            }
            pos += n;
            for (int i = 0; i < stride; i++) {
                int a = i >= bpp ? cur[i - bpp] & 0xff : 0;
                int b = prev[i] & 0xff;
                int c = i >= bpp ? prev[i - bpp] & 0xff : 0;
                int x = cur[i] & 0xff;
                switch (filter) {
                    case 1:
                        x += a;
                        break;
                    case 2:
                        x += b;
                        break;
                    case 3:
                        x += (a + b) >> 1;
                        break;
                    case 4: {
                        int pp = a + b - c;
                        int pa = Math.abs(pp - a);
                        int pb = Math.abs(pp - b);
                        int pc = Math.abs(pp - c);
                        x += pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
                        break;
                    }
                    default:
                        break;
                }
                cur[i] = (byte) x;
            }
            bo.write(cur, 0, n);
            byte[] t = prev;
            prev = cur;
            cur = t;
        }
        return bo.toByteArray();
    }

    static byte[] asciiHex(byte[] in) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream(in.length / 2);
        int hi = -1;
        for (int i = 0; i < in.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            int c = in[i] & 0xff;
            if (c == '>') {
                break;
            }
            int h = c >= '0' && c <= '9' ? c - '0' : c >= 'a' && c <= 'f' ? c - 'a' + 10
                    : c >= 'A' && c <= 'F' ? c - 'A' + 10 : -1;
            if (h < 0) {
                continue;
            }
            if (hi < 0) {
                hi = h;
            } else {
                bo.write(hi * 16 + h);
                hi = -1;
            }
        }
        if (hi >= 0) {
            bo.write(hi * 16);
        }
        return bo.toByteArray();
    }

    static byte[] ascii85(byte[] in) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream(in.length);
        long tuple = 0;
        int count = 0;
        int i = 0;
        if (in.length > 1 && in[0] == '<' && in[1] == '~') {
            i = 2;
        }
        for (; i < in.length; i++) {
            int c = in[i] & 0xff;
            if (c == '~') {
                break;
            }
            if (c == 'z' && count == 0) {
                bo.write(0);
                bo.write(0);
                bo.write(0);
                bo.write(0);
                continue;
            }
            if (c < '!' || c > 'u') {
                continue;
            }
            tuple = tuple * 85 + (c - '!');
            if (++count == 5) { // NOPMD AssignmentInOperand - counter bumped where it is tested
                bo.write((int) (tuple >> 24));
                bo.write((int) (tuple >> 16));
                bo.write((int) (tuple >> 8));
                bo.write((int) tuple);
                tuple = 0;
                count = 0;
            }
        }
        if (count > 1) {
            for (int k = count; k < 5; k++) {
                tuple = tuple * 85 + 84;
            }
            for (int k = 0; k < count - 1; k++) {
                bo.write((int) (tuple >> (24 - k * 8)));
            }
        }
        return bo.toByteArray();
    }

    static byte[] runLength(byte[] in) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream(in.length * 2);
        int i = 0;
        // Each run can expand two bytes to 128, so a chain after Flate
        // needs its own cap.
        while (i < in.length && bo.size() < MAX_DECODED) {
            int len = in[i++] & 0xff;
            if (len == 128) {
                break;
            }
            if (len < 128) {
                int n = Math.min(len + 1, in.length - i);
                bo.write(in, i, n);
                i += n;
            } else if (i < in.length) {
                int n = 257 - len;
                byte b = in[i++];
                for (int k = 0; k < n; k++) {
                    bo.write(b);
                }
            }
        }
        return bo.toByteArray();
    }

    static byte[] lzw(byte[] in, int early) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream((int) Math.min(MAX_DECODED, in.length * 3L));
        byte[][] table = new byte[4096][];
        for (int i = 0; i < 256; i++) {
            table[i] = new byte[] {(byte) i};
        }
        int next = 258;
        int width = 9;
        long bitBuf = 0;
        int bitCount = 0;
        byte[] prev = null;
        int pos = 0;
        while (true) {
            while (bitCount < width && pos < in.length) {
                bitBuf = (bitBuf << 8) | (in[pos++] & 0xff);
                bitCount += 8;
            }
            if (bitCount < width) {
                break;
            }
            int code = (int) ((bitBuf >> (bitCount - width)) & ((1 << width) - 1));
            bitCount -= width;
            if (code == 256) {
                next = 258;
                width = 9;
                prev = null;
                continue;
            }
            if (code == 257) {
                break;
            }
            byte[] entry;
            if (code < next && table[code] != null) {
                entry = table[code];
            } else if (prev != null) {
                entry = new byte[prev.length + 1];
                System.arraycopy(prev, 0, entry, 0, prev.length);
                entry[prev.length] = prev[0];
            } else {
                break;
            }
            bo.write(entry, 0, entry.length);
            if (bo.size() >= MAX_DECODED) {
                break;
            }
            if (prev != null && next < 4096) {
                byte[] add = new byte[prev.length + 1];
                System.arraycopy(prev, 0, add, 0, prev.length);
                add[prev.length] = entry[0];
                table[next++] = add;
            }
            prev = entry;
            if (next + early >= (1 << width) && width < 12) {
                width++;
            }
        }
        return bo.toByteArray();
    }
}
