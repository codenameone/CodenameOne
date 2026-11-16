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

/// The `CCITTFaxDecode` filter: ITU-T T.4 (Group 3, one- and
/// two-dimensional) and T.6 (Group 4) fax coding, to rows of 1-bit
/// samples, eight to a byte, rows padded to a byte. A decoding error ends
/// the image; rows not decoded stay white.
final class CcittDecoder {
    // "code:run" in bits; terminating codes then make-up codes.
    private static final String WHITE = "00110101:0 000111:1 0111:2 1000:3 1011:4 1100:5 1110:6 1111:7 "
            + "10011:8 10100:9 00111:10 01000:11 001000:12 000011:13 110100:14 110101:15 101010:16 "
            + "101011:17 0100111:18 0001100:19 0001000:20 0010111:21 0000011:22 0000100:23 0101000:24 "
            + "0101011:25 0010011:26 0100100:27 0011000:28 00000010:29 00000011:30 00011010:31 "
            + "00011011:32 00010010:33 00010011:34 00010100:35 00010101:36 00010110:37 00010111:38 "
            + "00101000:39 00101001:40 00101010:41 00101011:42 00101100:43 00101101:44 00000100:45 "
            + "00000101:46 00001010:47 00001011:48 01010010:49 01010011:50 01010100:51 01010101:52 "
            + "00100100:53 00100101:54 01011000:55 01011001:56 01011010:57 01011011:58 01001010:59 "
            + "01001011:60 00110010:61 00110011:62 00110100:63 11011:64 10010:128 010111:192 "
            + "0110111:256 00110110:320 00110111:384 01100100:448 01100101:512 01101000:576 "
            + "01100111:640 011001100:704 011001101:768 011010010:832 011010011:896 011010100:960 "
            + "011010101:1024 011010110:1088 011010111:1152 011011000:1216 011011001:1280 "
            + "011011010:1344 011011011:1408 010011000:1472 010011001:1536 010011010:1600 "
            + "011000:1664 010011011:1728";
    private static final String BLACK = "0000110111:0 010:1 11:2 10:3 011:4 0011:5 0010:6 00011:7 "
            + "000101:8 000100:9 0000100:10 0000101:11 0000111:12 00000100:13 00000111:14 "
            + "000011000:15 0000010111:16 0000011000:17 0000001000:18 00001100111:19 00001101000:20 "
            + "00001101100:21 00000110111:22 00000101000:23 00000010111:24 00000011000:25 "
            + "000011001010:26 000011001011:27 000011001100:28 000011001101:29 000001101000:30 "
            + "000001101001:31 000001101010:32 000001101011:33 000011010010:34 000011010011:35 "
            + "000011010100:36 000011010101:37 000011010110:38 000011010111:39 000001101100:40 "
            + "000001101101:41 000011011010:42 000011011011:43 000001010100:44 000001010101:45 "
            + "000001010110:46 000001010111:47 000001100100:48 000001100101:49 000001010010:50 "
            + "000001010011:51 000000100100:52 000000110111:53 000000111000:54 000000100111:55 "
            + "000000101000:56 000001011000:57 000001011001:58 000000101011:59 000000101100:60 "
            + "000001011010:61 000001100110:62 000001100111:63 0000001111:64 000011001000:128 "
            + "000011001001:192 000001011011:256 000000110011:320 000000110100:384 000000110101:448 "
            + "0000001101100:512 0000001101101:576 0000001001010:640 0000001001011:704 "
            + "0000001001100:768 0000001001101:832 0000001110010:896 0000001110011:960 "
            + "0000001110100:1024 0000001110101:1088 0000001110110:1152 0000001110111:1216 "
            + "0000001010010:1280 0000001010011:1344 0000001010100:1408 0000001010101:1472 "
            + "0000001011010:1536 0000001011011:1600 0000001100100:1664 0000001100101:1728";
    private static final String EXTENDED = "00000001000:1792 00000001100:1856 00000001101:1920 "
            + "000000010010:1984 000000010011:2048 000000010100:2112 000000010101:2176 "
            + "000000010110:2240 000000010111:2304 000000011100:2368 000000011101:2432 "
            + "000000011110:2496 000000011111:2560";
    // Built when the class loads, after the code strings above.
    private static final int[][] WHITE_TABLE = table(WHITE);
    private static final int[][] BLACK_TABLE = table(BLACK);


    private final byte[] d;
    private int bit;
    private final int limit;

    private CcittDecoder(byte[] data) {
        this.d = data;
        this.limit = data.length * 8;
    }

    /// `tables[len][code]` is the run, or -1.
    private static int[][] table(String codes) {
        int[][] t = new int[14][];
        for (int i = 1; i < 14; i++) {
            t[i] = new int[1 << i];
            for (int k = 0; k < t[i].length; k++) {
                t[i][k] = -1;
            }
        }
        add(t, codes);
        add(t, EXTENDED);
        return t;
    }

    private static void add(int[][] t, String codes) {
        int pos = 0;
        while (pos < codes.length()) {
            int sp = codes.indexOf(' ', pos);
            if (sp < 0) {
                sp = codes.length();
            }
            int colon = codes.indexOf(':', pos);
            int len = colon - pos;
            int code = 0;
            for (int i = pos; i < colon; i++) {
                code = (code << 1) | (codes.charAt(i) - '0');
            }
            t[len][code] = Integer.parseInt(codes.substring(colon + 1, sp));
            pos = sp + 1;
        }
    }

    private int peek(int n) {
        int v = 0;
        for (int i = 0; i < n; i++) {
            int b = bit + i;
            v <<= 1;
            if (b < limit) {
                v |= (d[b >> 3] >> (7 - (b & 7))) & 1;
            }
        }
        return v;
    }

    private int read(int n) {
        int v = peek(n);
        bit += n;
        return v;
    }

    /// One code of `t`, or -1.
    private int code(int[][] t) {
        for (int len = 1; len < 14; len++) {
            int r = t[len][peek(len)];
            if (r >= 0) {
                bit += len;
                return r;
            }
        }
        return -1;
    }

    /// A whole run: make-up codes then a terminating code; -1 on error.
    private int run(boolean white) {
        int total = 0;
        while (true) {
            int r = code(white ? WHITE_TABLE : BLACK_TABLE);
            if (r < 0) {
                return -1;
            }
            total += r;
            if (r < 64) {
                return total;
            }
        }
    }

    /// Two-dimensional mode codes.
    private static final int PASS = 100;
    private static final int HORIZONTAL = 101;
    private static final int EOL = 102;

    private int mode() {
        if (peek(1) == 1) {
            bit += 1;
            return 0;
        }
        int v3 = peek(3);
        if (v3 == 1) {
            bit += 3;
            return HORIZONTAL;
        }
        if (v3 == 3) {
            bit += 3;
            return 1;
        }
        if (v3 == 2) {
            bit += 3;
            return -1;
        }
        if (peek(4) == 1) {
            bit += 4;
            return PASS;
        }
        int v6 = peek(6);
        if (v6 == 3) {
            bit += 6;
            return 2;
        }
        if (v6 == 2) {
            bit += 6;
            return -2;
        }
        int v7 = peek(7);
        if (v7 == 3) {
            bit += 7;
            return 3;
        }
        if (v7 == 2) {
            bit += 7;
            return -3;
        }
        if (peek(12) == 1) {
            return EOL;
        }
        return Integer.MIN_VALUE;
    }

    /// Skips fill bits and one EOL if they come next; true if an EOL was
    /// there.
    private boolean skipEol() {
        int save = bit;
        int zeros = 0;
        while (bit < limit && peek(1) == 0) {
            bit++;
            zeros++;
        }
        if (zeros >= 11 && bit < limit) {
            bit++;
            return true;
        }
        bit = save;
        return false;
    }

    /// Decodes `data` with the parameters of `p` (null for defaults).
    static byte[] decode(byte[] data, PdfDict p, int width, int height) {
        int k = p == null ? 0 : p.getInt("K", 0);
        int columns = p == null ? 1728 : p.getInt("Columns", 1728);
        int rows = p == null ? 0 : p.getInt("Rows", 0);
        boolean align = p != null && Boolean.TRUE.equals(p.get("EncodedByteAlign"));
        boolean blackIs1 = p != null && Boolean.TRUE.equals(p.get("BlackIs1"));
        if (columns <= 0 || columns > 1 << 16) {
            columns = width;
        }
        int h = rows > 0 ? rows : height;
        int rowBytes = (columns + 7) / 8;
        byte[] out = new byte[rowBytes * Math.max(0, h)];
        if (!blackIs1) {
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) 0xff;
            }
        }
        CcittDecoder c = new CcittDecoder(data);
        int[] ref = new int[columns + 2];
        int[] cur = new int[columns + 2];
        ref[0] = columns;
        ref[1] = columns;
        int refCount = 0;
        for (int y = 0; y < h; y++) {
            if (align && k < 0) {
                c.bit = (c.bit + 7) & ~7;
            }
            boolean twoD = k < 0;
            if (k >= 0) {
                boolean eol = c.skipEol();
                if (align && !eol) {
                    c.bit = (c.bit + 7) & ~7;
                    c.skipEol();
                }
                if (k > 0) {
                    twoD = c.read(1) == 0;
                }
            } else if (c.peek(12) == 1) {
                break;
            }
            if (c.bit >= c.limit) {
                break;
            }
            int n = twoD ? c.row2d(ref, cur, columns) : c.row1d(cur, columns);
            if (n < 0) {
                break;
            }
            fill(out, y * rowBytes, cur, n, columns, blackIs1);
            int[] t = ref;
            ref = cur;
            cur = t;
            refCount = n;
            ref[refCount] = columns;
            ref[refCount + 1] = columns;
        }
        return out;
    }


    /// Writes the black runs between changes `ch[0..n)` into the row.
    private static void fill(byte[] out, int off, int[] ch, int n, int columns, boolean blackIs1) {
        for (int i = 0; i + 1 <= n; i += 2) {
            int a = Math.max(0, ch[i]);
            int b = i + 1 < n ? Math.min(columns, ch[i + 1]) : columns;
            for (int x = a; x < b; x++) {
                int idx = off + (x >> 3);
                int m = 0x80 >> (x & 7);
                if (blackIs1) {
                    out[idx] |= m;
                } else {
                    out[idx] &= ~m;
                }
            }
        }
    }

    /// A one-dimensional row into changes `cur`; the count, or -1.
    private int row1d(int[] cur, int columns) {
        int a0 = 0;
        int n = 0;
        boolean white = true;
        while (a0 < columns) {
            int r = run(white);
            if (r < 0) {
                return n > 0 ? n : -1;
            }
            a0 += r;
            if (a0 > columns) {
                a0 = columns;
            }
            if (a0 < columns || !white) {
                cur[n++] = a0;
            }
            white = !white;
            if (n >= cur.length - 2) {
                break;
            }
        }
        return trim(cur, n, columns);
    }

    /// Drops changes at the right edge, which mean nothing.
    private static int trim(int[] cur, int n, int columns) {
        while (n > 0 && cur[n - 1] >= columns) {
            n--;
        }
        return n;
    }

    /// A two-dimensional row against reference changes `ref`.
    private int row2d(int[] ref, int[] cur, int columns) {
        int a0 = -1;
        boolean white = true;
        int n = 0;
        int bi = 0;
        while (a0 < columns) {
            // b1: first change on the reference line right of a0 with the
            // colour opposite to a0's, i.e. even index for a white a0.
            bi = Math.max(0, bi - 2);
            while (ref[bi] <= a0 && ref[bi] < columns) {
                bi++;
            }
            if ((bi & 1) != (white ? 0 : 1) && ref[bi] < columns) {
                bi++;
            }
            int b1 = ref[bi];
            int b2 = ref[Math.min(bi + 1, ref.length - 1)];
            int m = mode();
            if (m == PASS) {
                a0 = b2;
                bi += 2;
            } else if (m == HORIZONTAL) {
                int r1 = run(white);
                int r2 = run(!white);
                if (r1 < 0 || r2 < 0) {
                    return n > 0 ? trim(cur, n, columns) : -1;
                }
                int start = Math.max(0, a0);
                int a1 = Math.min(columns, start + r1);
                int a2 = Math.min(columns, a1 + r2);
                cur[n++] = a1;
                cur[n++] = a2;
                a0 = a2;
            } else if (m >= -3 && m <= 3) {
                int a1 = Math.max(0, Math.min(columns, b1 + m));
                cur[n++] = a1;
                a0 = a1;
                white = !white;
            } else {
                return n > 0 ? trim(cur, n, columns) : -1;
            }
            if (n >= cur.length - 2) {
                break;
            }
        }
        return trim(cur, n, columns);
    }

    /// Decodes a T.6 (K < 0) bitmap of `columns` x `rows` from `data[off..end)`, as JBIG2's MMR
    /// coding uses it, into `out` at one byte per pixel (1 = black, row stride `columns`).
    /// Returns the number of bytes consumed, an EOFB included when present.
    static int decodeG4Pixels(byte[] data, int off, int end, int columns, int rows, byte[] out) {
        int len = Math.max(0, Math.min(end, data.length) - off);
        byte[] slice = new byte[len];
        System.arraycopy(data, off, slice, 0, len);
        CcittDecoder c = new CcittDecoder(slice);
        int[] ref = new int[columns + 2];
        int[] cur = new int[columns + 2];
        ref[0] = columns;
        ref[1] = columns;
        for (int y = 0; y < rows; y++) {
            if (c.peek(12) == 1 || c.bit >= c.limit) {
                break;
            }
            int n = c.row2d(ref, cur, columns);
            if (n < 0) {
                break;
            }
            int base = y * columns;
            for (int i = 0; i < n; i += 2) {
                int a = Math.max(0, cur[i]);
                int b = i + 1 < n ? Math.min(columns, cur[i + 1]) : columns;
                for (int x = a; x < b; x++) {
                    out[base + x] = 1;
                }
            }
            int[] t = ref;
            ref = cur;
            cur = t;
            ref[n] = columns;
            ref[n + 1] = columns;
        }
        int bits = c.bit;
        if (c.peek(24) == 0x001001) {
            bits += 24;
        }
        return Math.min(len, (bits + 7) >> 3);
    }
}
