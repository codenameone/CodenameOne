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
package com.codename1.qrcode;

import com.codename1.impl.SharedWithBackend;
import java.io.UnsupportedEncodingException;

/// A QR code, generated from text or bytes: the module grid, and SVG for a
/// page. The class is shared with the backend, which has no images: a client
/// draws one with `QrCodeImage`, a server serves [#toSvg()].
///
/// A device shows one so a phone can pair with it -- the address and a
/// one-time token in a URL -- and a server puts one on an enrolment page:
///
/// ```java
/// QrCode code = QrCode.encode("https://10.0.0.5:8443/pair?t=4F9A2C", QrCode.ECC_MEDIUM);
/// Image image = QrCodeImage.create(code, 8);
/// ```
///
/// Byte mode with UTF-8 text, versions 1 to 40, the four standard error
/// correction levels and the mask with the lowest penalty (ISO/IEC 18004). The
/// smallest version that holds the data is chosen.
@SharedWithBackend
public final class QrCode {
    /// Recovers about 7% of the code.
    public static final int ECC_LOW = 0;
    /// Recovers about 15%; the usual choice.
    public static final int ECC_MEDIUM = 1;
    /// Recovers about 25%.
    public static final int ECC_QUARTILE = 2;
    /// Recovers about 30%, for a code printed where it gets scuffed.
    public static final int ECC_HIGH = 3;

    // Per level (L, M, Q, H) and version: error correction codewords per
    // block, and the number of blocks. ISO/IEC 18004 table 9.
    private static final byte[][] ECC_PER_BLOCK = {
        {-1, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},
        {-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28},
        {-1, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26, 30, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},
        {-1, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26, 28, 30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},
    };
    private static final byte[][] BLOCKS = {
        {-1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 8, 8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25},
        {-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49},
        {-1, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8, 8, 10, 12, 16, 12, 17, 16, 18, 21, 20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68},
        {-1, 1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25, 25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81},
    };
    // The two format bits of each level, which are not in level order.
    private static final int[] FORMAT_BITS = {1, 0, 3, 2};

    private final int version;
    private final int size;
    private final int ecc;
    private final int mask;
    private final boolean[][] modules;
    private final boolean[][] function;

    private QrCode(int version, int ecc) {
        this.version = version;
        this.ecc = ecc;
        this.size = version * 4 + 17;
        this.modules = new boolean[size][size];
        this.function = new boolean[size][size];
        this.mask = 0;
    }

    private QrCode(QrCode base, int mask) {
        this.version = base.version;
        this.ecc = base.ecc;
        this.size = base.size;
        this.modules = base.modules;
        this.function = base.function;
        this.mask = mask;
    }

    /// Encodes text as UTF-8.
    public static QrCode encode(String text, int ecc) {
        if (text == null) {
            throw new IllegalArgumentException("text");
        }
        try {
            return encode(text.getBytes("UTF-8"), ecc);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8", e);
        }
    }

    /// Encodes bytes. Throws `IllegalArgumentException` when they do not fit
    /// a version 40 code at this level (2953 bytes at [#ECC_LOW]).
    public static QrCode encode(byte[] data, int ecc) {
        if (data == null || ecc < ECC_LOW || ecc > ECC_HIGH) {
            throw new IllegalArgumentException("data and a level from ECC_LOW to ECC_HIGH");
        }
        int version = 1;
        while (true) {
            int countBits = version < 10 ? 8 : 16;
            int capacityBits = dataCodewords(version, ecc) * 8;
            if (4 + countBits + data.length * 8 <= capacityBits) {
                break;
            }
            version++;
            if (version > 40) {
                throw new IllegalArgumentException("Too much data for a QR code: " + data.length + " bytes");
            }
        }
        QrCode qr = new QrCode(version, ecc);
        byte[] codewords = qr.addEccAndInterleave(qr.dataBits(data));
        qr.drawFunctionPatterns();
        qr.drawCodewords(codewords);
        int best = 0;
        int bestPenalty = Integer.MAX_VALUE;
        for (int m = 0; m < 8; m++) {
            qr.applyMask(m);
            qr.drawFormat(m);
            int p = qr.penalty();
            if (p < bestPenalty) {
                best = m;
                bestPenalty = p;
            }
            // Masking is its own inverse.
            qr.applyMask(m);
        }
        qr.applyMask(best);
        qr.drawFormat(best);
        return new QrCode(qr, best);
    }

    /// Modules per side, `17 + 4 * version`, without the quiet zone.
    public int getSize() {
        return size;
    }

    /// The version, 1 to 40.
    public int getVersion() {
        return version;
    }

    /// The error correction level, [#ECC_LOW] to [#ECC_HIGH].
    public int getErrorCorrection() {
        return ecc;
    }

    /// The mask pattern chosen, 0 to 7.
    public int getMask() {
        return mask;
    }

    /// True for a dark module; false outside the grid, which is the quiet zone.
    public boolean isDark(int x, int y) {
        return x >= 0 && y >= 0 && x < size && y < size && modules[y][x];
    }

    /// The code as an SVG document, one unit per module with a four module
    /// quiet zone, for a web page or a printed label.
    public String toSvg() {
        int full = size + 8;
        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(full).append(' ').append(full)
                .append("\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\" fill=\"#fff\"/><path d=\"");
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (modules[y][x]) {
                    sb.append('M').append(x + 4).append(',').append(y + 4).append("h1v1h-1z");
                }
            }
        }
        sb.append("\" fill=\"#000\"/></svg>");
        return sb.toString();
    }

    // ------------------------------------------------------------- capacity

    /// Modules left for data and error correction once every function
    /// pattern is placed.
    static int rawDataModules(int ver) {
        int result = (16 * ver + 128) * ver + 64;
        if (ver >= 2) {
            int align = ver / 7 + 2;
            result -= (25 * align - 10) * align - 55;
            if (ver >= 7) {
                result -= 36;
            }
        }
        return result;
    }

    static int dataCodewords(int ver, int ecc) {
        return rawDataModules(ver) / 8 - ECC_PER_BLOCK[ecc][ver] * BLOCKS[ecc][ver];
    }

    // ---------------------------------------------------------------- data

    private byte[] dataBits(byte[] data) {
        int capacity = dataCodewords(version, ecc);
        byte[] out = new byte[capacity];
        int[] bit = {0};
        put(out, bit, 4, 4);
        put(out, bit, data.length, version < 10 ? 8 : 16);
        for (byte b : data) {
            put(out, bit, b & 0xff, 8);
        }
        // Terminator, at most four zero bits, then to a byte boundary; the
        // array is already zero, so only the position moves.
        bit[0] = Math.min(capacity * 8, bit[0] + 4);
        bit[0] = (bit[0] + 7) / 8 * 8;
        for (int i = bit[0] / 8, pad = 0; i < capacity; i++, pad++) {
            out[i] = (byte) ((pad & 1) == 0 ? 0xec : 0x11);
        }
        return out;
    }

    private static void put(byte[] out, int[] bit, int value, int count) {
        for (int i = count - 1; i >= 0; i--) {
            if (((value >>> i) & 1) != 0) {
                out[bit[0] >>> 3] |= (byte) (0x80 >>> (bit[0] & 7));
            }
            bit[0]++;
        }
    }

    private byte[] addEccAndInterleave(byte[] data) {
        int blocks = BLOCKS[ecc][version];
        int eccLen = ECC_PER_BLOCK[ecc][version];
        int raw = rawDataModules(version) / 8;
        int shortBlocks = blocks - raw % blocks;
        int shortLen = raw / blocks;
        byte[] divisor = rsDivisor(eccLen);
        byte[][] dataBlocks = new byte[blocks][];
        byte[][] eccBlocks = new byte[blocks][];
        for (int i = 0, k = 0; i < blocks; i++) {
            int len = shortLen - eccLen + (i < shortBlocks ? 0 : 1);
            byte[] d = new byte[len];
            System.arraycopy(data, k, d, 0, len);
            k += len;
            dataBlocks[i] = d;
            eccBlocks[i] = rsRemainder(d, divisor);
        }
        byte[] out = new byte[raw];
        int n = 0;
        for (int i = 0; i <= shortLen - eccLen; i++) {
            for (int j = 0; j < blocks; j++) {
                if (i < dataBlocks[j].length) {
                    out[n++] = dataBlocks[j][i];
                }
            }
        }
        for (int i = 0; i < eccLen; i++) {
            for (int j = 0; j < blocks; j++) {
                out[n++] = eccBlocks[j][i];
            }
        }
        return out;
    }

    // Reed-Solomon over GF(2^8) with the polynomial 0x11d. The divisor is
    // stored highest power first, without its leading 1.
    private static byte[] rsDivisor(int degree) {
        byte[] result = new byte[degree];
        result[degree - 1] = 1;
        int root = 1;
        for (int i = 0; i < degree; i++) {
            for (int j = 0; j < degree; j++) {
                result[j] = (byte) gfMultiply(result[j] & 0xff, root);
                if (j + 1 < degree) {
                    result[j] ^= result[j + 1];
                }
            }
            root = gfMultiply(root, 2);
        }
        return result;
    }

    private static byte[] rsRemainder(byte[] data, byte[] divisor) {
        byte[] result = new byte[divisor.length];
        for (byte b : data) {
            int factor = (b ^ result[0]) & 0xff;
            System.arraycopy(result, 1, result, 0, result.length - 1);
            result[result.length - 1] = 0;
            for (int j = 0; j < result.length; j++) {
                result[j] ^= (byte) gfMultiply(divisor[j] & 0xff, factor);
            }
        }
        return result;
    }

    private static int gfMultiply(int x, int y) {
        int z = 0;
        for (int i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * 0x11d);
            z ^= ((y >>> i) & 1) * x;
        }
        return z;
    }

    // ------------------------------------------------------------ patterns

    private void set(int x, int y, boolean dark) {
        modules[y][x] = dark;
        function[y][x] = true;
    }

    private void drawFunctionPatterns() {
        for (int i = 0; i < size; i++) {
            set(6, i, i % 2 == 0);
            set(i, 6, i % 2 == 0);
        }
        finder(3, 3);
        finder(size - 4, 3);
        finder(3, size - 4);
        int[] align = alignmentPositions();
        for (int i = 0; i < align.length; i++) {
            for (int j = 0; j < align.length; j++) {
                boolean nearFinder = (i == 0 && j == 0) || (i == 0 && j == align.length - 1)
                        || (i == align.length - 1 && j == 0);
                if (!nearFinder) {
                    alignment(align[i], align[j]);
                }
            }
        }
        // Reserve the format areas now; drawFormat fills them per mask.
        drawFormat(0);
        if (version >= 7) {
            int rem = version;
            for (int i = 0; i < 12; i++) {
                rem = (rem << 1) ^ ((rem >>> 11) * 0x1f25);
            }
            int bits = version << 12 | rem;
            for (int i = 0; i < 18; i++) {
                boolean dark = ((bits >>> i) & 1) != 0;
                int a = size - 11 + i % 3;
                int b = i / 3;
                set(a, b, dark);
                set(b, a, dark);
            }
        }
    }

    private void finder(int cx, int cy) {
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int x = cx + dx;
                int y = cy + dy;
                if (x >= 0 && y >= 0 && x < size && y < size) {
                    int d = Math.max(Math.abs(dx), Math.abs(dy));
                    set(x, y, d != 2 && d != 4);
                }
            }
        }
    }

    private void alignment(int cx, int cy) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                set(cx + dx, cy + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
            }
        }
    }

    private int[] alignmentPositions() {
        if (version == 1) {
            return new int[0];
        }
        int count = version / 7 + 2;
        int step = version == 32 ? 26 : (version * 4 + count * 2 + 1) / (count * 2 - 2) * 2;
        int[] result = new int[count];
        result[0] = 6;
        for (int i = count - 1, pos = size - 7; i >= 1; i--, pos -= step) {
            result[i] = pos;
        }
        return result;
    }

    private void drawFormat(int m) {
        int data = FORMAT_BITS[ecc] << 3 | m;
        int rem = data;
        for (int i = 0; i < 10; i++) {
            rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        }
        int bits = (data << 10 | rem) ^ 0x5412;
        for (int i = 0; i <= 5; i++) {
            set(8, i, bit(bits, i));
        }
        set(8, 7, bit(bits, 6));
        set(8, 8, bit(bits, 7));
        set(7, 8, bit(bits, 8));
        for (int i = 9; i < 15; i++) {
            set(14 - i, 8, bit(bits, i));
        }
        for (int i = 0; i < 8; i++) {
            set(size - 1 - i, 8, bit(bits, i));
        }
        for (int i = 8; i < 15; i++) {
            set(8, size - 15 + i, bit(bits, i));
        }
        set(8, size - 8, true);
    }

    private static boolean bit(int value, int i) {
        return ((value >>> i) & 1) != 0;
    }

    private void drawCodewords(byte[] data) {
        int i = 0;
        int total = data.length * 8;
        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) {
                right = 5;
            }
            boolean upward = ((right + 1) & 2) == 0;
            for (int vert = 0; vert < size; vert++) {
                int y = upward ? size - 1 - vert : vert;
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    if (!function[y][x] && i < total) {
                        modules[y][x] = bit(data[i >>> 3], 7 - (i & 7));
                        i++;
                    }
                }
            }
        }
    }

    private void applyMask(int m) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (!function[y][x] && masked(m, x, y)) {
                    modules[y][x] = !modules[y][x];
                }
            }
        }
    }

    private static boolean masked(int m, int x, int y) {
        switch (m) {
            case 0:
                return (x + y) % 2 == 0;
            case 1:
                return y % 2 == 0;
            case 2:
                return x % 3 == 0;
            case 3:
                return (x + y) % 3 == 0;
            case 4:
                return (x / 3 + y / 2) % 2 == 0;
            case 5:
                return x * y % 2 + x * y % 3 == 0;
            case 6:
                return (x * y % 2 + x * y % 3) % 2 == 0;
            default:
                return ((x + y) % 2 + x * y % 3) % 2 == 0;
        }
    }

    // ------------------------------------------------------------- penalty

    private int penalty() {
        int result = 0;
        for (int a = 0; a < size; a++) {
            result += linePenalty(a, true) + linePenalty(a, false);
        }
        int dark = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean c = modules[y][x];
                if (c) {
                    dark++;
                }
                if (x + 1 < size && y + 1 < size && c == modules[y][x + 1] && c == modules[y + 1][x]
                        && c == modules[y + 1][x + 1]) {
                    result += 3;
                }
            }
        }
        int total = size * size;
        int k = (Math.abs(dark * 20 - total * 10) + total - 1) / total - 1;
        return result + Math.max(0, k) * 10;
    }

    private boolean at(int a, int b, boolean row) {
        return row ? modules[a][b] : modules[b][a];
    }

    // Runs of five or more, and the 1:1:3:1:1 finder look-alike with four
    // light modules on either side (the quiet zone counts as light).
    private int linePenalty(int a, boolean row) {
        int result = 0;
        int run = 1;
        for (int b = 1; b <= size; b++) {
            if (b < size && at(a, b, row) == at(a, b - 1, row)) {
                run++;
            } else {
                if (run >= 5) {
                    result += run - 2;
                }
                run = 1;
            }
        }
        for (int b = -4; b < size; b++) {
            if (matches(a, b, row, 0x5d0) || matches(a, b, row, 0x05d)) {
                result += 40;
            }
        }
        return result;
    }

    // An 11 module pattern, most significant bit first: 0x5d0 is
    // dark-light-dark-dark-dark-light-dark then four light.
    private boolean matches(int a, int start, boolean row, int pattern) {
        for (int i = 0; i < 11; i++) {
            int b = start + i;
            boolean dark = b >= 0 && b < size && at(a, b, row);
            if (dark != bit(pattern, 10 - i)) {
                return false;
            }
        }
        return true;
    }
}
