/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.messaging.kafka;

import com.codename1.backend.ByteSink;
import java.io.IOException;

/// Zstandard decompression, for batches a producer sent with
/// `compression.type=zstd`: the format of RFC 8878, written from the RFC.
///
/// Everything a compressor may emit without a dictionary is read: frames
/// with and without a content size, single- and multi-segment, raw, RLE and
/// compressed blocks, raw, RLE, Huffman and treeless literals in one or four
/// streams, predefined, RLE, FSE and repeated sequence tables, repeat offsets,
/// and several frames or skippable frames one after another. A frame that
/// names a dictionary is refused: Kafka never uses one. Checksums are skipped,
/// as the record batch already carries a CRC over these bytes.
///
/// The whole batch decodes into one buffer, so no window has to be kept; a
/// back-reference is only checked to stay inside its own frame.
final class Zstd {
    private static final int MAGIC = 0xFD2FB528;
    private static final int MAX_LL = 35;
    private static final int MAX_ML = 52;
    private static final int MAX_OF = 31;

    private static final int[] LL_BASE = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        16, 18, 20, 22, 24, 28, 32, 40, 48, 64, 128, 256, 512, 1024, 2048, 4096,
        8192, 16384, 32768, 65536};
    private static final int[] LL_BITS = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 2, 2, 3, 3, 4, 6, 7, 8, 9, 10, 11, 12,
        13, 14, 15, 16};
    private static final int[] ML_BASE = {
        3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
        19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34,
        35, 37, 39, 41, 43, 47, 51, 59, 67, 83, 99, 131, 259, 515, 1027, 2051,
        4099, 8195, 16387, 32771, 65539};
    private static final int[] ML_BITS = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 2, 2, 3, 3, 4, 4, 5, 7, 8, 9, 10, 11,
        12, 13, 14, 15, 16};

    private static final Fse LL_DEFAULT;
    private static final Fse ML_DEFAULT;
    private static final Fse OF_DEFAULT;

    static {
        try {
            LL_DEFAULT = Fse.build(new int[] {
                4, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1,
                2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 2, 1, 1, 1, 1, 1,
                -1, -1, -1, -1}, 36, 6);
            ML_DEFAULT = Fse.build(new int[] {
                1, 4, 3, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1,
                -1, -1, -1, -1, -1}, 53, 6);
            OF_DEFAULT = Fse.build(new int[] {
                1, 1, 1, 1, 1, 1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, -1, -1, -1, -1, -1}, 29, 5);
        } catch (IOException err) {
            throw new IllegalStateException(err.getMessage(), err);
        }
    }

    private Zstd() {
    }

    static byte[] decompress(byte[] in) throws IOException {
        ByteSink out = Inflate.sink(in.length * 4);
        int p = 0;
        while (p < in.length) {
            int magic = (int) Inflate.le(in, p, 4, in.length);
            p += 4;
            if ((magic & 0xfffffff0) == 0x184D2A50) {
                long skip = Inflate.le(in, p, 4, in.length);
                p += 4;
                if (skip > in.length - p) {
                    throw corrupt();
                }
                p += (int) skip;
            } else if (magic == MAGIC) {
                p = new Frame(in, out).decode(p);
            } else {
                throw new IOException("A zstd Kafka batch is not a Zstandard frame");
            }
        }
        return Inflate.toArray(out);
    }

    static IOException corrupt() {
        return new IOException("A zstd Kafka batch is corrupt");
    }

    /// The state one frame carries from block to block.
    private static final class Frame {
        private final byte[] in;
        private final ByteSink out;
        private int floor;
        private final long[] reps = {1, 4, 8};
        private Huffman huffman;
        private Fse ll;
        private Fse of;
        private Fse ml;
        private byte[] literals = new byte[0];
        private int literalCount;

        Frame(byte[] in, ByteSink out) {
            this.in = in;
            this.out = out;
        }

        /// Decodes the frame whose header starts at `at`; answers where it ends.
        int decode(int at) throws IOException {
            int end = in.length;
            int p = at;
            if (p >= end) {
                throw corrupt();
            }
            int descriptor = in[p++] & 0xff;
            int sizeFlag = descriptor >>> 6;
            boolean single = (descriptor & 0x20) != 0;
            boolean checksum = (descriptor & 0x04) != 0;
            int dictFlag = descriptor & 3;
            if ((descriptor & 0x08) != 0) {
                throw corrupt();
            }
            if (!single) {
                p++;
            }
            int dictBytes = dictFlag == 3 ? 4 : dictFlag;
            if (dictBytes > 0 && Inflate.le(in, p, dictBytes, end) != 0) {
                throw new IOException("A zstd Kafka batch needs a dictionary");
            }
            p += dictBytes;
            int sizeBytes = sizeFlag == 0 ? (single ? 1 : 0) : 1 << sizeFlag;
            long contentSize = -1;
            if (sizeBytes > 0) {
                contentSize = Inflate.le(in, p, sizeBytes, end);
                if (sizeBytes == 2) {
                    contentSize += 256;
                }
                p += sizeBytes;
            }
            floor = out.length();
            while (true) {
                long header = Inflate.le(in, p, 3, end);
                p += 3;
                boolean last = (header & 1) != 0;
                int type = (int) ((header >>> 1) & 3);
                int size = (int) (header >>> 3);
                if (type == 0) {
                    if (size > end - p) {
                        throw corrupt();
                    }
                    Inflate.put(out, in, p, size);
                    p += size;
                } else if (type == 1) {
                    if (p >= end) {
                        throw corrupt();
                    }
                    Inflate.fill(out, in[p], size);
                    p++;
                } else if (type == 2) {
                    if (size > end - p) {
                        throw corrupt();
                    }
                    block(p, p + size);
                    p += size;
                } else {
                    throw corrupt();
                }
                if (last) {
                    break;
                }
            }
            if (checksum) {
                p += 4;
                if (p > end) {
                    throw corrupt();
                }
            }
            if (contentSize >= 0 && out.length() - floor != contentSize) {
                throw new IOException("A zstd Kafka batch is not the size it claims");
            }
            return p;
        }

        private void block(int from, int end) throws IOException {
            int p = readLiterals(from, end);
            if (p >= end) {
                throw corrupt();
            }
            int count = in[p++] & 0xff;
            if (count >= 128) {
                if (count == 255) {
                    count = (int) Inflate.le(in, p, 2, end) + 0x7f00;
                    p += 2;
                } else {
                    if (p >= end) {
                        throw corrupt();
                    }
                    count = ((count - 128) << 8) + (in[p++] & 0xff);
                }
            }
            int used = 0;
            if (count > 0) {
                if (p >= end) {
                    throw corrupt();
                }
                int modes = in[p++] & 0xff;
                if ((modes & 3) != 0) {
                    throw corrupt();
                }
                int[] at = {p};
                ll = table(modes >>> 6, at, end, LL_DEFAULT, ll, MAX_LL, 9);
                of = table((modes >>> 4) & 3, at, end, OF_DEFAULT, of, MAX_OF, 8);
                ml = table((modes >>> 2) & 3, at, end, ML_DEFAULT, ml, MAX_ML, 9);
                used = sequences(count, at[0], end);
            } else if (p != end) {
                throw corrupt();
            }
            if (used < literalCount) {
                Inflate.put(out, literals, used, literalCount - used);
            }
        }

        private Fse table(int mode, int[] at, int end, Fse predefined, Fse previous,
                          int maxSymbol, int maxLog) throws IOException {
            if (mode == 0) {
                return predefined;
            }
            if (mode == 1) {
                if (at[0] >= end) {
                    throw corrupt();
                }
                int symbol = in[at[0]++] & 0xff;
                if (symbol > maxSymbol) {
                    throw corrupt();
                }
                return Fse.rle(symbol);
            }
            if (mode == 2) {
                return Fse.read(in, at, end, maxSymbol, maxLog);
            }
            if (previous == null) {
                throw corrupt();
            }
            return previous;
        }

        /// Executes `count` sequences from the bit stream `in[from, end)`;
        /// answers how many literals they took.
        private int sequences(int count, int from, int end) throws IOException {
            Bits bits = new Bits(in, from, end);
            int llState = (int) bits.read(ll.log);
            int ofState = (int) bits.read(of.log);
            int mlState = (int) bits.read(ml.log);
            int lit = 0;
            for (int i = 0; i < count; i++) {
                int ofCode = of.symbol[ofState];
                int llCode = ll.symbol[llState];
                int mlCode = ml.symbol[mlState];
                if (ofCode > MAX_OF || llCode > MAX_LL || mlCode > MAX_ML) {
                    throw corrupt();
                }
                long offset = (1L << ofCode) + bits.read(ofCode);
                int matchLength = ML_BASE[mlCode] + (int) bits.read(ML_BITS[mlCode]);
                int literalLength = LL_BASE[llCode] + (int) bits.read(LL_BITS[llCode]);
                if (offset > 3) {
                    offset -= 3;
                    reps[2] = reps[1];
                    reps[1] = reps[0];
                    reps[0] = offset;
                } else {
                    int index = (int) offset + (literalLength == 0 ? 1 : 0);
                    if (index == 1) {
                        offset = reps[0];
                    } else if (index == 2) {
                        offset = reps[1];
                        reps[1] = reps[0];
                        reps[0] = offset;
                    } else {
                        offset = index == 3 ? reps[2] : reps[0] - 1;
                        if (offset <= 0) {
                            throw corrupt();
                        }
                        reps[2] = reps[1];
                        reps[1] = reps[0];
                        reps[0] = offset;
                    }
                }
                if (i < count - 1) {
                    llState = ll.next[llState] + (int) bits.read(ll.bits[llState]);
                    mlState = ml.next[mlState] + (int) bits.read(ml.bits[mlState]);
                    ofState = of.next[ofState] + (int) bits.read(of.bits[ofState]);
                }
                if (literalLength > literalCount - lit) {
                    throw corrupt();
                }
                Inflate.put(out, literals, lit, literalLength);
                lit += literalLength;
                Inflate.copy(out, offset, matchLength, floor);
            }
            if (bits.remaining() != 0) {
                throw corrupt();
            }
            return lit;
        }

        /// Reads the literals section at `from` into `literals`; answers
        /// where the sequences section starts.
        private int readLiterals(int from, int end) throws IOException {
            if (from >= end) {
                throw corrupt();
            }
            int b0 = in[from] & 0xff;
            int type = b0 & 3;
            int format = (b0 >>> 2) & 3;
            if (type < 2) {
                int header;
                int size;
                if (format == 0 || format == 2) {
                    header = 1;
                    size = b0 >>> 3;
                } else if (format == 1) {
                    header = 2;
                    size = (int) (Inflate.le(in, from, 2, end) >>> 4);
                } else {
                    header = 3;
                    size = (int) (Inflate.le(in, from, 3, end) >>> 4);
                }
                int p = from + header;
                room(size);
                if (type == 0) {
                    if (size > end - p) {
                        throw corrupt();
                    }
                    System.arraycopy(in, p, literals, 0, size);
                    return p + size;
                }
                if (p >= end) {
                    throw corrupt();
                }
                byte v = in[p];
                for (int i = 0; i < size; i++) {
                    literals[i] = v;
                }
                return p + 1;
            }
            int header;
            int size;
            int compressed;
            boolean four = format != 0;
            if (format < 2) {
                long v = Inflate.le(in, from, 3, end);
                header = 3;
                size = (int) ((v >>> 4) & 0x3ff);
                compressed = (int) ((v >>> 14) & 0x3ff);
            } else if (format == 2) {
                long v = Inflate.le(in, from, 4, end);
                header = 4;
                size = (int) ((v >>> 4) & 0x3fff);
                compressed = (int) ((v >>> 18) & 0x3fff);
            } else {
                long v = Inflate.le(in, from, 5, end);
                header = 5;
                size = (int) ((v >>> 4) & 0x3ffff);
                compressed = (int) ((v >>> 22) & 0x3ffff);
            }
            int p = from + header;
            if (compressed > end - p) {
                throw corrupt();
            }
            int streamsEnd = p + compressed;
            if (type == 2) {
                int[] at = {p};
                huffman = Huffman.read(in, at, streamsEnd);
                p = at[0];
            } else if (huffman == null) {
                throw corrupt();
            }
            room(size);
            if (!four) {
                huffman.decode(in, p, streamsEnd, literals, 0, size);
            } else {
                long jump = Inflate.le(in, p, 6, streamsEnd);
                p += 6;
                int s1 = (int) (jump & 0xffff);
                int s2 = (int) ((jump >>> 16) & 0xffff);
                int s3 = (int) ((jump >>> 32) & 0xffff);
                int s4 = streamsEnd - p - s1 - s2 - s3;
                int segment = (size + 3) / 4;
                int lastSegment = size - 3 * segment;
                if (s4 < 0 || lastSegment < 0) {
                    throw corrupt();
                }
                huffman.decode(in, p, p + s1, literals, 0, segment);
                p += s1;
                huffman.decode(in, p, p + s2, literals, segment, segment);
                p += s2;
                huffman.decode(in, p, p + s3, literals, 2 * segment, segment);
                p += s3;
                huffman.decode(in, p, streamsEnd, literals, 3 * segment, lastSegment);
            }
            return streamsEnd;
        }

        private void room(int size) {
            if (literals.length < size) {
                literals = new byte[Math.max(size, 1024)];
            }
            literalCount = size;
        }
    }

    /// A backward bit stream: written forwards, read from its last bit to
    /// its first, the way every entropy-coded stream of zstd is.
    static final class Bits {
        private final byte[] b;
        private final int start;
        private final int end;
        private int position;

        Bits(byte[] b, int start, int end) throws IOException {
            if (end <= start || end > b.length) {
                throw corrupt();
            }
            int last = b[end - 1] & 0xff;
            if (last == 0) {
                throw corrupt();
            }
            this.b = b;
            this.start = start;
            this.end = end;
            position = (end - start - 1) * 8 + 31 - Integer.numberOfLeadingZeros(last);
        }

        /// Bits not yet read; negative once a read went past the start.
        int remaining() {
            return position;
        }

        /// The next `n` bits (at most 56) without consuming them; past the
        /// start of the stream the missing low bits read as zero.
        long peek(int n) {
            if (n == 0) {
                return 0;
            }
            int low = position - n;
            if (low >= 0) {
                return extract(low, n);
            }
            if (position <= 0) {
                return 0;
            }
            return extract(0, position) << -low;
        }

        long read(int n) throws IOException {
            long v = peek(n);
            position -= n;
            if (position < -64) {
                throw corrupt();
            }
            return v;
        }

        void skip(int n) {
            position -= n;
        }

        private long extract(int low, int n) {
            int at = start + (low >>> 3);
            long v = 0;
            int stop = Math.min(at + 8, end);
            for (int i = stop - 1; i >= at; i--) {
                v = (v << 8) | (b[i] & 0xff);
            }
            return (v >>> (low & 7)) & ((1L << n) - 1);
        }
    }

    /// A finite state entropy decoding table.
    static final class Fse {
        final int log;
        final int[] symbol;
        final int[] bits;
        final int[] next;

        private Fse(int log) {
            this.log = log;
            int size = 1 << log;
            symbol = new int[size];
            bits = new int[size];
            next = new int[size];
        }

        static Fse rle(int s) {
            Fse t = new Fse(0);
            t.symbol[0] = s;
            return t;
        }

        /// The table of a normalised distribution, as RFC 8878 4.1.1 builds it.
        static Fse build(int[] norm, int symbols, int log) throws IOException {
            Fse t = new Fse(log);
            int size = 1 << log;
            int high = size - 1;
            int[] counts = new int[symbols];
            for (int s = 0; s < symbols; s++) {
                if (norm[s] == -1) {
                    t.symbol[high--] = s;
                    counts[s] = 1;
                } else {
                    counts[s] = norm[s];
                }
            }
            int step = (size >>> 1) + (size >>> 3) + 3;
            int mask = size - 1;
            int pos = 0;
            for (int s = 0; s < symbols; s++) {
                for (int i = 0; i < norm[s]; i++) {
                    t.symbol[pos] = s;
                    do {
                        pos = (pos + step) & mask;
                    } while (pos > high);
                }
            }
            if (pos != 0) {
                throw corrupt();
            }
            for (int u = 0; u < size; u++) {
                int s = t.symbol[u];
                int x = counts[s]++;
                if (x <= 0) {
                    throw corrupt();
                }
                int n = log - (31 - Integer.numberOfLeadingZeros(x));
                t.bits[u] = n;
                t.next[u] = (x << n) - size;
            }
            return t;
        }

        /// Reads a table description at `at[0]`, advancing it past.
        static Fse read(byte[] in, int[] at, int end, int maxSymbol, int maxLog)
                throws IOException {
            int start = at[0];
            int[] norm = new int[maxSymbol + 1];
            if (start >= end) {
                throw corrupt();
            }
            int log = (in[start] & 15) + 5;
            if (log > maxLog) {
                throw corrupt();
            }
            int bit = 4;
            int remaining = (1 << log) + 1;
            int threshold = 1 << log;
            int width = log + 1;
            int symbol = 0;
            boolean zero = false;
            while (remaining > 1 && symbol <= maxSymbol) {
                if (zero) {
                    while (true) {
                        int repeat = (int) forward(in, start, end, bit, 2);
                        bit += 2;
                        symbol += repeat;
                        if (repeat != 3) {
                            break;
                        }
                    }
                    if (symbol > maxSymbol) {
                        throw corrupt();
                    }
                }
                int max = 2 * threshold - 1 - remaining;
                int v = (int) forward(in, start, end, bit, width);
                int count;
                if ((v & (threshold - 1)) < max) {
                    count = v & (threshold - 1);
                    bit += width - 1;
                } else {
                    count = v & (2 * threshold - 1);
                    if (count >= threshold) {
                        count -= max;
                    }
                    bit += width;
                }
                count--;
                remaining -= count < 0 ? -count : count;
                norm[symbol++] = count;
                zero = count == 0;
                while (remaining < threshold) {
                    width--;
                    threshold >>>= 1;
                }
            }
            int consumed = (bit + 7) >>> 3;
            if (remaining != 1 || consumed > end - start) {
                throw corrupt();
            }
            at[0] = start + consumed;
            return build(norm, symbol, log);
        }

        /// `n` bits of the little-endian bit stream at `start`, from bit `bit`.
        private static long forward(byte[] in, int start, int end, int bit, int n) {
            int at = start + (bit >>> 3);
            long v = 0;
            int stop = Math.min(at + 4, end);
            for (int i = stop - 1; i >= at; i--) {
                v = (v << 8) | (in[i] & 0xff);
            }
            return (v >>> (bit & 7)) & ((1L << n) - 1);
        }
    }

    /// A Huffman decoding table for literals.
    static final class Huffman {
        private final int maxBits;
        private final byte[] symbol;
        private final byte[] bits;

        private Huffman(int maxBits) {
            this.maxBits = maxBits;
            symbol = new byte[1 << maxBits];
            bits = new byte[1 << maxBits];
        }

        /// Reads a tree description at `at[0]`, advancing it past.
        static Huffman read(byte[] in, int[] at, int end) throws IOException {
            int p = at[0];
            if (p >= end) {
                throw corrupt();
            }
            int header = in[p++] & 0xff;
            int[] weights = new int[256];
            int count;
            if (header < 128) {
                if (header > end - p) {
                    throw corrupt();
                }
                int stop = p + header;
                int[] cursor = {p};
                Fse t = Fse.read(in, cursor, stop, 255, 6);
                Bits b = new Bits(in, cursor[0], stop);
                int s1 = (int) b.read(t.log);
                int s2 = (int) b.read(t.log);
                count = 0;
                while (true) {
                    if (count > 253) {
                        throw corrupt();
                    }
                    weights[count++] = t.symbol[s1];
                    s1 = t.next[s1] + (int) b.read(t.bits[s1]);
                    if (b.remaining() < 0) {
                        weights[count++] = t.symbol[s2];
                        break;
                    }
                    weights[count++] = t.symbol[s2];
                    s2 = t.next[s2] + (int) b.read(t.bits[s2]);
                    if (b.remaining() < 0) {
                        weights[count++] = t.symbol[s1];
                        break;
                    }
                }
                p = stop;
            } else {
                count = header - 127;
                int bytes = (count + 1) / 2;
                if (bytes > end - p) {
                    throw corrupt();
                }
                for (int i = 0; i < count; i++) {
                    int v = in[p + i / 2] & 0xff;
                    weights[i] = (i & 1) == 0 ? v >>> 4 : v & 15;
                }
                p += bytes;
            }
            int sum = 0;
            for (int i = 0; i < count; i++) {
                if (weights[i] > 11) {
                    throw corrupt();
                }
                if (weights[i] > 0) {
                    sum += 1 << (weights[i] - 1);
                }
            }
            if (sum == 0) {
                throw corrupt();
            }
            int maxBits = 32 - Integer.numberOfLeadingZeros(sum);
            int rest = (1 << maxBits) - sum;
            if (maxBits > 11 || rest == 0 || (rest & (rest - 1)) != 0) {
                throw corrupt();
            }
            weights[count] = 32 - Integer.numberOfLeadingZeros(rest);
            int symbols = count + 1;
            Huffman h = new Huffman(maxBits);
            int pos = 0;
            for (int w = 1; w <= maxBits; w++) {
                int n = 1 << (w - 1);
                int length = maxBits + 1 - w;
                for (int s = 0; s < symbols; s++) {
                    if (weights[s] == w) {
                        if (n > h.symbol.length - pos) {
                            throw corrupt();
                        }
                        for (int i = 0; i < n; i++) {
                            h.symbol[pos] = (byte) s;
                            h.bits[pos] = (byte) length;
                            pos++;
                        }
                    }
                }
            }
            if (pos != h.symbol.length) {
                throw corrupt();
            }
            at[0] = p;
            return h;
        }

        /// Decodes `count` literals from the stream `in[from, end)` into
        /// `out` at `at`; the stream must hold exactly that many.
        void decode(byte[] in, int from, int end, byte[] out, int at, int count)
                throws IOException {
            if (count == 0 && from == end) {
                return;
            }
            Bits b = new Bits(in, from, end);
            for (int i = 0; i < count; i++) {
                int index = (int) b.peek(maxBits);
                out[at + i] = symbol[index];
                b.skip(bits[index]);
                if (b.remaining() < 0) {
                    throw corrupt();
                }
            }
            if (b.remaining() != 0) {
                throw corrupt();
            }
        }
    }
}
