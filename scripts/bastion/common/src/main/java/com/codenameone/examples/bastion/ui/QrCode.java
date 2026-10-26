/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.ui;

/// A QR code encoder, small enough to read: byte mode, error correction
/// level M, versions 1 to 10 (up to 213 bytes), mask chosen by the standard's
/// penalty score. Written for one job -- showing an `otpauth://` URI so an
/// authenticator app can scan the TOTP secret -- and written here because the
/// framework scans codes ([com.codename1.codescan]) but does not draw them.
///
/// The steps are the standard's (ISO/IEC 18004), in order:
///
/// 1. the data as bits: mode, length, bytes, terminator, padding;
/// 2. split into blocks, each given Reed-Solomon check bytes over GF(256);
/// 3. the blocks interleaved, and placed in a zigzag around the fixed
///    patterns (finders, timing, alignment, format and version areas);
/// 4. each of the eight masks tried, the one with the lowest penalty kept,
///    and the format bits that name it written in.
///
/// `QrCodeTest` checks the matrices against another encoder's for every
/// version this supports.
public final class QrCode {
    /// Per version 1..10 at level M: check bytes per block, then the blocks of
    /// the short group and their data bytes, then those of the long group.
    private static final int[][] BLOCKS = {
        {10, 1, 16, 0, 0},
        {16, 1, 28, 0, 0},
        {26, 1, 44, 0, 0},
        {18, 2, 32, 0, 0},
        {24, 2, 43, 0, 0},
        {16, 4, 27, 0, 0},
        {18, 4, 31, 0, 0},
        {22, 2, 38, 2, 39},
        {22, 3, 36, 2, 37},
        {26, 4, 43, 1, 44},
    };

    /// The centres of the alignment patterns, per version.
    private static final int[][] ALIGN = {
        {}, {6, 18}, {6, 22}, {6, 26}, {6, 30}, {6, 34},
        {6, 22, 38}, {6, 24, 42}, {6, 26, 46}, {6, 28, 50},
    };

    /// The most bytes [#encode(byte[])] takes: version 10 at level M.
    public static final int MAX_BYTES = 213;

    /// Level M's two bits in the format information.
    private static final int LEVEL_M = 0;

    private final int size;
    private final boolean[][] dark;
    private final boolean[][] fixed;

    private QrCode(int version) {
        size = version * 4 + 17;
        dark = new boolean[size][size];
        fixed = new boolean[size][size];
    }

    /// The code for `data` at the smallest version that holds it.
    public static QrCode encode(byte[] data) {
        return encode(data, -1);
    }

    /// The code for `data`, with mask `mask` (0..7), or the best mask when it
    /// is -1. A fixed mask is for the tests.
    public static QrCode encode(byte[] data, int mask) {
        int version = 1;
        while (version <= BLOCKS.length && capacity(version) < data.length) {
            version++;
        }
        if (version > BLOCKS.length) {
            throw new IllegalArgumentException("Too long for a QR code here: " + data.length);
        }
        return encode(data, version, mask);
    }

    /// The code for `data` at exactly `version`, for the tests.
    public static QrCode encode(byte[] data, int version, int mask) {
        if (capacity(version) < data.length) {
            throw new IllegalArgumentException("Does not fit version " + version);
        }
        QrCode code = new QrCode(version);
        code.drawPatterns(version);
        code.place(codewords(data, version));
        if (mask >= 0) {
            code.applyMask(mask);
            code.drawFormat(mask);
            return code;
        }
        int best = 0;
        int bestScore = Integer.MAX_VALUE;
        for (int candidate = 0; candidate < 8; candidate++) {
            code.applyMask(candidate);
            code.drawFormat(candidate);
            int score = code.penalty();
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
            // Masking is its own inverse: applied again, it takes itself off.
            code.applyMask(candidate);
        }
        code.applyMask(best);
        code.drawFormat(best);
        return code;
    }

    /// Modules per side, without the quiet zone.
    public int size() {
        return size;
    }

    /// Whether the module in column `x`, row `y` is dark.
    public boolean isDark(int x, int y) {
        return dark[y][x];
    }

    /// The bytes version `version` holds in byte mode at level M.
    static int capacity(int version) {
        int[] b = BLOCKS[version - 1];
        int dataBytes = b[1] * b[2] + b[3] * b[4];
        // Four bits of mode, then 8 bits of length below version 10, 16 from it.
        int header = 4 + (version < 10 ? 8 : 16);
        return (dataBytes * 8 - header) / 8;
    }

    // ------------------------------------------------------------- the bytes

    /// The final sequence of codewords: data blocks interleaved, then their
    /// check bytes interleaved.
    private static byte[] codewords(byte[] data, int version) {
        int[] b = BLOCKS[version - 1];
        int dataBytes = b[1] * b[2] + b[3] * b[4];
        byte[] stream = dataStream(data, version, dataBytes);
        int blocks = b[1] + b[3];
        byte[][] blockData = new byte[blocks][];
        byte[][] blockCheck = new byte[blocks][];
        byte[] generator = generator(b[0]);
        int at = 0;
        for (int iter = 0; iter < blocks; iter++) {
            int length = iter < b[1] ? b[2] : b[4];
            blockData[iter] = new byte[length];
            System.arraycopy(stream, at, blockData[iter], 0, length);
            at += length;
            blockCheck[iter] = remainder(blockData[iter], generator);
        }
        byte[] out = new byte[dataBytes + blocks * b[0]];
        int pos = 0;
        int longest = Math.max(b[2], b[4]);
        for (int column = 0; column < longest; column++) {
            for (int iter = 0; iter < blocks; iter++) {
                if (column < blockData[iter].length) {
                    out[pos++] = blockData[iter][column];
                }
            }
        }
        for (int column = 0; column < b[0]; column++) {
            for (int iter = 0; iter < blocks; iter++) {
                out[pos++] = blockCheck[iter][column];
            }
        }
        return out;
    }

    /// Mode, length and the bytes, then the terminator and the pad bytes
    /// that fill the version's data capacity.
    private static byte[] dataStream(byte[] data, int version, int dataBytes) {
        Bits bits = new Bits(dataBytes);
        bits.add(4, 4);
        bits.add(data.length, version < 10 ? 8 : 16);
        for (byte one : data) {
            bits.add(one & 0xff, 8);
        }
        int room = dataBytes * 8 - bits.length;
        bits.add(0, Math.min(4, room));
        bits.add(0, (8 - bits.length % 8) % 8);
        boolean flip = false;
        while (bits.length < dataBytes * 8) {
            bits.add(flip ? 0x11 : 0xec, 8);
            flip = !flip;
        }
        return bits.bytes;
    }

    /// A bit buffer, most significant bit first.
    private static final class Bits {
        final byte[] bytes;
        int length;

        Bits(int capacity) {
            bytes = new byte[capacity];
        }

        void add(int value, int count) {
            for (int iter = count - 1; iter >= 0; iter--) {
                if (((value >>> iter) & 1) != 0) {
                    bytes[length >>> 3] |= (byte) (0x80 >>> (length & 7));
                }
                length++;
            }
        }
    }

    // ------------------------------------------------------- Reed-Solomon

    /// Multiplication in GF(256) with the QR polynomial x^8+x^4+x^3+x^2+1.
    static int multiply(int a, int b) {
        int out = 0;
        for (int iter = 7; iter >= 0; iter--) {
            out = (out << 1) ^ ((out >>> 7) * 0x11d);
            out ^= ((b >>> iter) & 1) * a;
        }
        return out & 0xff;
    }

    /// The generator polynomial of `degree`, the product of (x - 2^i) for i
    /// below it; coefficients from the highest power down, the leading 1 left
    /// out.
    private static byte[] generator(int degree) {
        int[] poly = new int[degree];
        poly[degree - 1] = 1;
        int root = 1;
        for (int iter = 0; iter < degree; iter++) {
            for (int j = 0; j < degree; j++) {
                poly[j] = multiply(poly[j], root);
                if (j + 1 < degree) {
                    poly[j] ^= poly[j + 1];
                }
            }
            root = multiply(root, 2);
        }
        byte[] out = new byte[degree];
        for (int iter = 0; iter < degree; iter++) {
            out[iter] = (byte) poly[iter];
        }
        return out;
    }

    /// The check bytes: the remainder of `data` times x^degree divided by the
    /// generator.
    private static byte[] remainder(byte[] data, byte[] generator) {
        int[] rest = new int[generator.length];
        for (byte one : data) {
            int factor = (one & 0xff) ^ rest[0];
            System.arraycopy(rest, 1, rest, 0, rest.length - 1);
            rest[rest.length - 1] = 0;
            for (int iter = 0; iter < rest.length; iter++) {
                rest[iter] ^= multiply(generator[iter] & 0xff, factor);
            }
        }
        byte[] out = new byte[rest.length];
        for (int iter = 0; iter < rest.length; iter++) {
            out[iter] = (byte) rest[iter];
        }
        return out;
    }

    // ------------------------------------------------------------- layout

    private void set(int x, int y, boolean on) {
        dark[y][x] = on;
        fixed[y][x] = true;
    }

    private void drawPatterns(int version) {
        for (int iter = 0; iter < size; iter++) {
            set(6, iter, iter % 2 == 0);
            set(iter, 6, iter % 2 == 0);
        }
        finder(3, 3);
        finder(size - 4, 3);
        finder(3, size - 4);
        int[] centres = ALIGN[version - 1];
        int last = centres.length - 1;
        for (int i = 0; i <= last; i++) {
            for (int j = 0; j <= last; j++) {
                boolean onFinder = (i == 0 && j == 0) || (i == 0 && j == last)
                        || (i == last && j == 0);
                if (!onFinder) {
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            set(centres[i] + dx, centres[j] + dy,
                                    Math.max(Math.abs(dx), Math.abs(dy)) != 1);
                        }
                    }
                }
            }
        }
        // Reserved now, written once the mask is known.
        drawFormat(0);
        if (version >= 7) {
            int bits = version << 12 | bch(version, 0x1f25, 12);
            for (int iter = 0; iter < 18; iter++) {
                boolean on = ((bits >>> iter) & 1) != 0;
                int a = size - 11 + iter % 3;
                int b = iter / 3;
                set(a, b, on);
                set(b, a, on);
            }
        }
    }

    /// A finder with its light separator, clipped at the edge.
    private void finder(int cx, int cy) {
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int x = cx + dx;
                int y = cy + dy;
                if (x >= 0 && x < size && y >= 0 && y < size) {
                    int ring = Math.max(Math.abs(dx), Math.abs(dy));
                    set(x, y, ring != 2 && ring != 4);
                }
            }
        }
    }

    /// The 15 format bits for level M and `mask`, in both copies, and the
    /// module that is always dark.
    private void drawFormat(int mask) {
        int data = LEVEL_M << 3 | mask;
        int bits = (data << 10 | bch(data, 0x537, 10)) ^ 0x5412;
        for (int iter = 0; iter <= 5; iter++) {
            set(8, iter, bit(bits, iter));
        }
        set(8, 7, bit(bits, 6));
        set(8, 8, bit(bits, 7));
        set(7, 8, bit(bits, 8));
        for (int iter = 9; iter < 15; iter++) {
            set(14 - iter, 8, bit(bits, iter));
        }
        for (int iter = 0; iter < 8; iter++) {
            set(size - 1 - iter, 8, bit(bits, iter));
        }
        for (int iter = 8; iter < 15; iter++) {
            set(8, size - 15 + iter, bit(bits, iter));
        }
        set(8, size - 8, true);
    }

    private static boolean bit(int value, int index) {
        return ((value >>> index) & 1) != 0;
    }

    /// The BCH remainder of `value` shifted up by `degree`, for the format and
    /// version bits.
    private static int bch(int value, int poly, int degree) {
        int rest = value << degree;
        for (int iter = 31 - Integer.numberOfLeadingZeros(rest); iter >= degree; iter--) {
            if (((rest >>> iter) & 1) != 0) {
                rest ^= poly << (iter - degree);
            }
        }
        return rest;
    }

    /// The codewords into every module the patterns left free, two columns at
    /// a time from the right, up then down, stepping over the timing column.
    private void place(byte[] codewords) {
        int total = codewords.length * 8;
        int index = 0;
        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) {
                right = 5;
            }
            boolean upward = ((right + 1) & 2) == 0;
            for (int step = 0; step < size; step++) {
                int y = upward ? size - 1 - step : step;
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    if (!fixed[y][x] && index < total) {
                        dark[y][x] = ((codewords[index >>> 3] >>> (7 - (index & 7))) & 1) != 0;
                        index++;
                    }
                }
            }
        }
    }

    private void applyMask(int mask) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (!fixed[y][x] && masked(mask, x, y)) {
                    dark[y][x] = !dark[y][x];
                }
            }
        }
    }

    private static boolean masked(int mask, int x, int y) {
        switch (mask) {
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

    // ------------------------------------------------------------ penalty

    /// The standard's four penalty rules: long runs, 2x2 blocks, finder-like
    /// patterns, and an imbalance of dark and light.
    private int penalty() {
        int score = 0;
        for (int line = 0; line < size; line++) {
            score += runs(line, true) + runs(line, false);
            score += finderLike(line, true) + finderLike(line, false);
        }
        int darkCount = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (dark[y][x]) {
                    darkCount++;
                }
                if (x + 1 < size && y + 1 < size) {
                    boolean c = dark[y][x];
                    if (c == dark[y][x + 1] && c == dark[y + 1][x] && c == dark[y + 1][x + 1]) {
                        score += 3;
                    }
                }
            }
        }
        int total = size * size;
        // How far the dark share is from half, in whole steps of 5%.
        int deviation = Math.abs(darkCount * 20 - total * 10);
        score += (deviation / total) * 10;
        return score;
    }

    private boolean at(int line, int pos, boolean row) {
        return row ? dark[line][pos] : dark[pos][line];
    }

    private int runs(int line, boolean row) {
        int score = 0;
        int run = 1;
        for (int pos = 1; pos <= size; pos++) {
            if (pos < size && at(line, pos, row) == at(line, pos - 1, row)) {
                run++;
            } else {
                if (run >= 5) {
                    score += 3 + run - 5;
                }
                run = 1;
            }
        }
        return score;
    }

    /// 1:1:3:1:1 dark-light-dark-light-dark with four light modules on
    /// either side; outside the symbol counts as light.
    private int finderLike(int line, boolean row) {
        int score = 0;
        boolean[] pattern = {true, false, true, true, true, false, true};
        for (int start = -4; start + 7 <= size + 4; start++) {
            boolean matches = true;
            for (int iter = 0; iter < 7 && matches; iter++) {
                matches = module(line, start + iter, row) == pattern[iter];
            }
            if (!matches) {
                continue;
            }
            if (lightRun(line, start - 4, row) || lightRun(line, start + 7, row)) {
                score += 40;
            }
        }
        return score;
    }

    private boolean module(int line, int pos, boolean row) {
        return pos >= 0 && pos < size && at(line, pos, row);
    }

    private boolean lightRun(int line, int from, boolean row) {
        for (int iter = from; iter < from + 4; iter++) {
            if (module(line, iter, row)) {
                return false;
            }
        }
        return true;
    }
}
