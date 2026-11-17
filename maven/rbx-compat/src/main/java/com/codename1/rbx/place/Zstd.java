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
package com.codename1.rbx.place;

/// A Zstandard (RFC 8878) decompressor written in plain Java so it runs on
/// every Codename One target.
///
/// It decodes any sequence of Zstandard and skippable frames. Dictionaries
/// are not supported, and the optional XXH64 content checksum is skipped
/// rather than verified. Corrupt or unsupported input raises
/// `IllegalArgumentException`.
public final class Zstd {
    private static final int FRAME_MAGIC = 0xFD2FB528;
    private static final int SKIPPABLE_MAGIC = 0x184D2A50;
    private static final int MAX_BLOCK = 128 * 1024;
    private static final int MAX_HUF_BITS = 11;

    private static final int[] LL_BASE = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        16, 18, 20, 22, 24, 28, 32, 40, 48, 64, 128, 256, 512, 1024, 2048, 4096,
        8192, 16384, 32768, 65536
    };
    private static final int[] LL_BITS = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 2, 2, 3, 3, 4, 6, 7, 8, 9, 10, 11, 12,
        13, 14, 15, 16
    };
    private static final int[] ML_BASE = {
        3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
        19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34,
        35, 37, 39, 41, 43, 47, 51, 59, 67, 83, 99, 131, 259, 515, 1027, 2051,
        4099, 8195, 16387, 32771, 65539
    };
    private static final int[] ML_BITS = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 2, 2, 3, 3, 4, 4, 5, 7, 8, 9, 10, 11,
        12, 13, 14, 15, 16
    };
    private static final int[] LL_DEFAULT = {
        4, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 2, 1, 1, 1, 1, 1,
        -1, -1, -1, -1
    };
    private static final int[] ML_DEFAULT = {
        1, 4, 3, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1,
        -1, -1, -1, -1, -1
    };
    private static final int[] OF_DEFAULT = {
        1, 1, 1, 1, 1, 1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, -1, -1, -1, -1, -1
    };

    private static final Fse LL_PREDEFINED = Fse.build(LL_DEFAULT, LL_DEFAULT.length, 6);
    private static final Fse ML_PREDEFINED = Fse.build(ML_DEFAULT, ML_DEFAULT.length, 6);
    private static final Fse OF_PREDEFINED = Fse.build(OF_DEFAULT, OF_DEFAULT.length, 5);

    private final byte[] src;
    private int ip;
    private byte[] out;
    private int outPos;
    private int frameStart;

    private final byte[] lit = new byte[MAX_BLOCK];
    private int litLen;

    private final byte[] hufSym = new byte[1 << MAX_HUF_BITS];
    private final byte[] hufBits = new byte[1 << MAX_HUF_BITS];
    private int hufMaxBits;
    private boolean hufValid;

    private Fse llTable;
    private Fse ofTable;
    private Fse mlTable;
    private int rep1;
    private int rep2;
    private int rep3;

    /// Absolute output ceiling for one decompress call.
    private static final int MAX_UNKNOWN_OUTPUT = 64 * 1024 * 1024;

    private int limit = MAX_UNKNOWN_OUTPUT;

    private Zstd(byte[] src, int capacity) {
        this.src = src;
        this.out = new byte[capacity];
    }

    /// Decompresses `len` bytes of `src` starting at `off`.
    ///
    /// The input may hold several Zstandard frames back to back, mixed with
    /// skippable frames; the decoded content of every frame is concatenated.
    /// `expectedSize` sizes the output buffer up front and may be `-1` when
    /// the size is not known. Returns exactly the decoded bytes, and throws
    /// `IllegalArgumentException` when the input is corrupt, truncated or
    /// needs a dictionary.
    public static byte[] decompress(byte[] src, int off, int len, int expectedSize) {
        if (src == null || off < 0 || len < 0 || off > src.length - len) {
            throw new IllegalArgumentException("zstd: input range out of bounds");
        }
        int capacity = expectedSize > 0 ? expectedSize : Math.max(64, len * 4);
        if (capacity < 0) {
            capacity = len;
        }
        Zstd z = new Zstd(src, capacity);
        // The declared size is only a sizing hint (callers and tests pass
        // short ones), so growth is held to an absolute ceiling instead: a
        // forged frame of RLE blocks expands a few bytes to 128 KB each and
        // must not grow the output until the heap is gone.
        z.limit = MAX_UNKNOWN_OUTPUT;
        z.ip = off;
        int end = off + len;
        while (z.ip < end) {
            z.need(4, end);
            int magic = le32(src, z.ip);
            if ((magic & 0xFFFFFFF0) == SKIPPABLE_MAGIC) {
                z.need(8, end);
                long size = le32(src, z.ip + 4) & 0xFFFFFFFFL;
                if (size > end - z.ip - 8) {
                    throw new IllegalArgumentException("zstd: truncated skippable frame");
                }
                z.ip += 8 + (int) size;
            } else if (magic == FRAME_MAGIC) {
                z.ip += 4;
                z.frame(end);
            } else {
                throw new IllegalArgumentException("zstd: bad frame magic");
            }
        }
        return z.finish();
    }

    /// Hands the output over, trimmed to the decoded length. The decoder is
    /// discarded afterwards, so the buffer is returned as is when it is full.
    private byte[] finish() {
        byte[] buf = out;
        out = new byte[0];
        if (outPos == buf.length) {
            return buf;
        }
        byte[] result = new byte[outPos];
        System.arraycopy(buf, 0, result, 0, outPos);
        return result;
    }

    private static int le32(byte[] b, int p) {
        return (b[p] & 0xFF) | (b[p + 1] & 0xFF) << 8 | (b[p + 2] & 0xFF) << 16 | (b[p + 3] & 0xFF) << 24;
    }

    private static int le16(byte[] b, int p) {
        return (b[p] & 0xFF) | (b[p + 1] & 0xFF) << 8;
    }

    static int highBit(int v) {
        int r = -1;
        int x = v;
        while (x != 0) {
            x >>>= 1;
            r++;
        }
        return r;
    }

    private void need(int n, int end) {
        if (n < 0 || n > end - ip) {
            throw new IllegalArgumentException("zstd: truncated input");
        }
    }

    private void ensure(int n) {
        int required = outPos + n;
        if (required < 0 || required > limit) {
            throw new IllegalArgumentException("zstd: output too large");
        }
        if (required > out.length) {
            int cap = out.length * 2;
            if (cap < required) {
                cap = required;
            }
            byte[] grown = new byte[cap];
            System.arraycopy(out, 0, grown, 0, outPos);
            out = grown;
        }
    }

    private void frame(int end) {
        need(1, end);
        int fhd = src[ip++] & 0xFF;
        int fcsFlag = fhd >>> 6;
        boolean single = (fhd & 0x20) != 0;
        if ((fhd & 0x08) != 0) {
            throw new IllegalArgumentException("zstd: reserved frame header bit set");
        }
        boolean checksum = (fhd & 0x04) != 0;
        int dictFlag = fhd & 3;
        if (!single) {
            need(1, end);
            ip++;
        }
        int dictSize = dictFlag == 3 ? 4 : dictFlag;
        need(dictSize, end);
        long dictId = 0;
        for (int i = 0; i < dictSize; i++) {
            dictId |= (long) (src[ip + i] & 0xFF) << (8 * i);
        }
        ip += dictSize;
        if (dictId != 0) {
            throw new IllegalArgumentException("zstd: dictionaries are unsupported");
        }
        int fcsSize;
        if (fcsFlag == 0) {
            fcsSize = single ? 1 : 0;
        } else {
            fcsSize = 1 << fcsFlag;
        }
        need(fcsSize, end);
        long fcs = -1;
        if (fcsSize > 0) {
            fcs = 0;
            for (int i = 0; i < fcsSize; i++) {
                fcs |= (long) (src[ip + i] & 0xFF) << (8 * i);
            }
            if (fcsSize == 2) {
                fcs += 256;
            }
            ip += fcsSize;
        }
        frameStart = outPos;
        if (fcs > 0 && fcs < Integer.MAX_VALUE - outPos) {
            ensure((int) fcs);
        }
        rep1 = 1;
        rep2 = 4;
        rep3 = 8;
        hufValid = false;
        llTable = null;
        ofTable = null;
        mlTable = null;
        boolean last = false;
        while (!last) {
            need(3, end);
            int h = (src[ip] & 0xFF) | (src[ip + 1] & 0xFF) << 8 | (src[ip + 2] & 0xFF) << 16;
            ip += 3;
            last = (h & 1) != 0;
            int type = (h >>> 1) & 3;
            int size = h >>> 3;
            if (type == 0) {
                need(size, end);
                ensure(size);
                System.arraycopy(src, ip, out, outPos, size);
                outPos += size;
                ip += size;
            } else if (type == 1) {
                need(1, end);
                ensure(size);
                byte b = src[ip++];
                for (int i = 0; i < size; i++) {
                    out[outPos++] = b;
                }
            } else if (type == 2) {
                need(size, end);
                if (size > MAX_BLOCK) {
                    throw new IllegalArgumentException("zstd: block too large");
                }
                int blockEnd = ip + size;
                compressedBlock(blockEnd);
                ip = blockEnd;
            } else {
                throw new IllegalArgumentException("zstd: reserved block type");
            }
        }
        if (checksum) {
            need(4, end);
            ip += 4;
        }
        if (fcs >= 0 && outPos - frameStart != fcs) {
            throw new IllegalArgumentException("zstd: frame content size mismatch");
        }
    }

    private void compressedBlock(int end) {
        literals(end);
        sequences(end);
    }

    private void literals(int end) {
        need(1, end);
        int b0 = src[ip] & 0xFF;
        int type = b0 & 3;
        int sf = (b0 >>> 2) & 3;
        if (type <= 1) {
            int size;
            if ((sf & 1) == 0) {
                size = b0 >>> 3;
                ip += 1;
            } else if (sf == 1) {
                need(2, end);
                size = (b0 >>> 4) + ((src[ip + 1] & 0xFF) << 4);
                ip += 2;
            } else {
                need(3, end);
                size = (b0 >>> 4) + ((src[ip + 1] & 0xFF) << 4) + ((src[ip + 2] & 0xFF) << 12);
                ip += 3;
            }
            if (size > MAX_BLOCK) {
                throw new IllegalArgumentException("zstd: literals too large");
            }
            if (type == 0) {
                need(size, end);
                System.arraycopy(src, ip, lit, 0, size);
                ip += size;
            } else {
                need(1, end);
                byte b = src[ip++];
                for (int i = 0; i < size; i++) {
                    lit[i] = b;
                }
            }
            litLen = size;
            return;
        }
        int hdr;
        if (sf <= 1) {
            hdr = 3;
        } else if (sf == 2) {
            hdr = 4;
        } else {
            hdr = 5;
        }
        need(hdr, end);
        long h = 0;
        for (int i = 0; i < hdr; i++) {
            h |= (long) (src[ip + i] & 0xFF) << (8 * i);
        }
        int fieldBits = hdr == 3 ? 10 : (hdr == 4 ? 14 : 18);
        long mask = (1L << fieldBits) - 1;
        int regen = (int) ((h >>> 4) & mask);
        int comp = (int) ((h >>> (4 + fieldBits)) & mask);
        ip += hdr;
        need(comp, end);
        if (regen > MAX_BLOCK) {
            throw new IllegalArgumentException("zstd: literals too large");
        }
        int cend = ip + comp;
        if (type == 2) {
            huffmanTable(cend);
        } else if (!hufValid) {
            throw new IllegalArgumentException("zstd: treeless literals without a previous table");
        }
        if (sf == 0) {
            huffStream(ip, cend, 0, regen);
        } else {
            need(6, cend);
            int s1 = le16(src, ip);
            int s2 = le16(src, ip + 2);
            int s3 = le16(src, ip + 4);
            int p = ip + 6;
            int s4 = cend - p - s1 - s2 - s3;
            int seg = (regen + 3) / 4;
            int lastSeg = regen - 3 * seg;
            if (s4 < 0 || lastSeg < 0) {
                throw new IllegalArgumentException("zstd: bad literal stream sizes");
            }
            huffStream(p, p + s1, 0, seg);
            p += s1;
            huffStream(p, p + s2, seg, seg);
            p += s2;
            huffStream(p, p + s3, 2 * seg, seg);
            p += s3;
            huffStream(p, cend, 3 * seg, lastSeg);
        }
        litLen = regen;
        ip = cend;
    }

    private void huffmanTable(int end) {
        need(1, end);
        int hb = src[ip++] & 0xFF;
        int[] weights = new int[256];
        int n;
        if (hb < 128) {
            need(hb, end);
            n = fseWeights(ip, ip + hb, weights);
            ip += hb;
        } else {
            n = hb - 127;
            int bytes = (n + 1) / 2;
            need(bytes, end);
            for (int i = 0; i < n; i++) {
                int b = src[ip + i / 2] & 0xFF;
                weights[i] = (i & 1) == 0 ? b >>> 4 : b & 15;
            }
            ip += bytes;
        }
        int sum = 0;
        for (int i = 0; i < n; i++) {
            int w = weights[i];
            if (w > MAX_HUF_BITS) {
                throw new IllegalArgumentException("zstd: bad Huffman weight");
            }
            if (w > 0) {
                sum += 1 << (w - 1);
            }
        }
        if (sum == 0) {
            throw new IllegalArgumentException("zstd: empty Huffman table");
        }
        int maxBits = highBit(sum) + 1;
        if (maxBits > MAX_HUF_BITS) {
            throw new IllegalArgumentException("zstd: Huffman table too deep");
        }
        int rest = (1 << maxBits) - sum;
        if ((rest & (rest - 1)) != 0) {
            throw new IllegalArgumentException("zstd: bad Huffman weights");
        }
        weights[n] = highBit(rest) + 1;
        n++;
        int p = 0;
        for (int w = 1; w <= maxBits; w++) {
            int span = 1 << (w - 1);
            byte nb = (byte) (maxBits + 1 - w);
            for (int s = 0; s < n; s++) {
                if (weights[s] == w) {
                    for (int i = 0; i < span; i++) {
                        hufSym[p] = (byte) s;
                        hufBits[p] = nb;
                        p++;
                    }
                }
            }
        }
        if (p != 1 << maxBits) {
            throw new IllegalArgumentException("zstd: bad Huffman table");
        }
        hufMaxBits = maxBits;
        hufValid = true;
    }

    private int fseWeights(int start, int end, int[] weights) {
        int saved = ip;
        ip = start;
        Fse t = readFse(end, 256, 6);
        Bits br = new Bits(src, ip, end);
        ip = saved;
        int s1 = br.read(t.log);
        int s2 = br.read(t.log);
        int n = 0;
        while (true) {
            if (n > 253) {
                throw new IllegalArgumentException("zstd: too many Huffman weights");
            }
            weights[n++] = t.sym[s1];
            s1 = t.base[s1] + br.read(t.nb[s1]);
            if (br.pos < 0) {
                weights[n++] = t.sym[s2];
                break;
            }
            weights[n++] = t.sym[s2];
            s2 = t.base[s2] + br.read(t.nb[s2]);
            if (br.pos < 0) {
                weights[n++] = t.sym[s1];
                break;
            }
        }
        return n;
    }

    /// Reads an FSE table description at `ip` and advances past it.
    private Fse readFse(int end, int maxSymbols, int maxLog) {
        int start = ip;
        int bit = 0;
        int avail = (end - start) * 8;
        if (avail < 4) {
            throw new IllegalArgumentException("zstd: truncated FSE table");
        }
        int log = (src[start] & 15) + 5;
        bit = 4;
        if (log > maxLog) {
            throw new IllegalArgumentException("zstd: FSE accuracy too high");
        }
        int[] probs = new int[maxSymbols];
        int remaining = 1 << log;
        int sym = 0;
        while (remaining > 0) {
            if (sym >= maxSymbols) {
                throw new IllegalArgumentException("zstd: too many FSE symbols");
            }
            int bits = highBit(remaining + 1) + 1;
            int val = forward(start, end, bit, bits);
            int lowMask = (1 << (bits - 1)) - 1;
            int thr = (1 << bits) - 1 - (remaining + 1);
            if ((val & lowMask) < thr) {
                val &= lowMask;
                bit += bits - 1;
            } else {
                if (val > lowMask) {
                    val -= thr;
                }
                bit += bits;
            }
            int prob = val - 1;
            remaining -= prob < 0 ? -prob : prob;
            probs[sym++] = prob;
            if (prob == 0) {
                int repeat;
                do {
                    repeat = forward(start, end, bit, 2);
                    bit += 2;
                    for (int i = 0; i < repeat; i++) {
                        if (sym >= maxSymbols) {
                            throw new IllegalArgumentException("zstd: too many FSE symbols");
                        }
                        probs[sym++] = 0;
                    }
                } while (repeat == 3);
            }
            if (bit > avail) {
                throw new IllegalArgumentException("zstd: truncated FSE table");
            }
        }
        if (remaining != 0) {
            throw new IllegalArgumentException("zstd: bad FSE table");
        }
        ip = start + (bit + 7) / 8;
        return Fse.build(probs, sym, log);
    }

    private int forward(int start, int end, int bit, int n) {
        int v = 0;
        for (int i = 0; i < n; i++) {
            int b = bit + i;
            int idx = start + (b >>> 3);
            if (idx < end && (src[idx] >>> (b & 7) & 1) != 0) {
                v |= 1 << i;
            }
        }
        return v;
    }

    private void huffStream(int start, int end, int outOff, int count) {
        Bits br = new Bits(src, start, end);
        int mb = hufMaxBits;
        for (int i = 0; i < count; i++) {
            int v = br.peek(mb);
            lit[outOff + i] = hufSym[v];
            br.pos -= hufBits[v];
        }
        if (br.pos != 0) {
            throw new IllegalArgumentException("zstd: corrupt Huffman stream");
        }
    }

    private Fse table(int mode, int end, Fse predefined, Fse previous, int maxSymbol, int maxLog) {
        if (mode == 0) {
            return predefined;
        }
        if (mode == 1) {
            need(1, end);
            int s = src[ip++] & 0xFF;
            if (s > maxSymbol) {
                throw new IllegalArgumentException("zstd: bad RLE symbol");
            }
            return Fse.rle(s);
        }
        if (mode == 2) {
            return readFse(end, maxSymbol + 1, maxLog);
        }
        if (previous == null) {
            throw new IllegalArgumentException("zstd: repeat table without a previous one");
        }
        return previous;
    }

    private void sequences(int end) {
        need(1, end);
        int b0 = src[ip++] & 0xFF;
        int nbSeq;
        if (b0 < 128) {
            nbSeq = b0;
        } else if (b0 < 255) {
            need(1, end);
            nbSeq = ((b0 - 128) << 8) + (src[ip++] & 0xFF);
        } else {
            need(2, end);
            nbSeq = le16(src, ip) + 0x7F00;
            ip += 2;
        }
        int litPos = 0;
        if (nbSeq > 0) {
            need(1, end);
            int modes = src[ip++] & 0xFF;
            if ((modes & 3) != 0) {
                throw new IllegalArgumentException("zstd: reserved sequence mode bits");
            }
            llTable = table(modes >>> 6, end, LL_PREDEFINED, llTable, 35, 9);
            ofTable = table((modes >>> 4) & 3, end, OF_PREDEFINED, ofTable, 31, 8);
            mlTable = table((modes >>> 2) & 3, end, ML_PREDEFINED, mlTable, 52, 9);
            Fse ll = llTable;
            Fse of = ofTable;
            Fse ml = mlTable;
            Bits br = new Bits(src, ip, end);
            int llS = br.read(ll.log);
            int ofS = br.read(of.log);
            int mlS = br.read(ml.log);
            for (int i = 0; i < nbSeq; i++) {
                int ofCode = of.sym[ofS];
                int mlCode = ml.sym[mlS];
                int llCode = ll.sym[llS];
                if (ofCode > 31 || mlCode > 52 || llCode > 35) {
                    throw new IllegalArgumentException("zstd: bad sequence code");
                }
                long offVal = (1L << ofCode) + (br.read(ofCode) & 0xFFFFFFFFL);
                int matchLen = ML_BASE[mlCode] + br.read(ML_BITS[mlCode]);
                int litCount = LL_BASE[llCode] + br.read(LL_BITS[llCode]);
                if (i != nbSeq - 1) {
                    llS = ll.base[llS] + br.read(ll.nb[llS]);
                    mlS = ml.base[mlS] + br.read(ml.nb[mlS]);
                    ofS = of.base[ofS] + br.read(of.nb[ofS]);
                }
                if (br.pos < 0) {
                    throw new IllegalArgumentException("zstd: corrupt sequence stream");
                }
                if (litCount > litLen - litPos) {
                    throw new IllegalArgumentException("zstd: literal overrun");
                }
                ensure(litCount);
                System.arraycopy(lit, litPos, out, outPos, litCount);
                outPos += litCount;
                litPos += litCount;
                long offset = resolveOffset(offVal, litCount == 0);
                if (offset > outPos - frameStart || offset <= 0) {
                    throw new IllegalArgumentException("zstd: match offset out of range");
                }
                copyMatch((int) offset, matchLen);
            }
            if (br.pos != 0) {
                throw new IllegalArgumentException("zstd: corrupt sequence stream");
            }
        }
        int rest = litLen - litPos;
        ensure(rest);
        System.arraycopy(lit, litPos, out, outPos, rest);
        outPos += rest;
    }

    private long resolveOffset(long offVal, boolean noLiterals) {
        if (offVal > 3) {
            long offset = offVal - 3;
            rep3 = rep2;
            rep2 = rep1;
            rep1 = (int) offset;
            return offset;
        }
        int k = (int) offVal + (noLiterals ? 1 : 0);
        int offset;
        if (k == 1) {
            return rep1;
        } else if (k == 2) {
            offset = rep2;
            rep2 = rep1;
        } else {
            offset = k == 3 ? rep3 : rep1 - 1;
            rep3 = rep2;
            rep2 = rep1;
        }
        rep1 = offset;
        return offset;
    }

    private void copyMatch(int offset, int length) {
        ensure(length);
        int from = outPos - offset;
        if (offset >= length) {
            System.arraycopy(out, from, out, outPos, length);
            outPos += length;
            return;
        }
        byte[] o = out;
        int p = outPos;
        for (int i = 0; i < length; i++) {
            o[p + i] = o[from + i];
        }
        outPos = p + length;
    }

    /// A backward bit stream: read from the end towards the start, its last
    /// byte padded with zeros above a marker bit. Bits before the start read
    /// as zeros, and `pos` goes negative once they have been consumed.
    private static final class Bits {
        private final byte[] data;
        private final int start;
        int pos;

        Bits(byte[] data, int start, int end) {
            if (end <= start) {
                throw new IllegalArgumentException("zstd: empty bit stream");
            }
            int last = data[end - 1] & 0xFF;
            if (last == 0) {
                throw new IllegalArgumentException("zstd: missing bit stream marker");
            }
            this.data = data;
            this.start = start;
            this.pos = (end - start - 1) * 8 + highBit(last);
        }

        int read(int n) {
            if (n == 0) {
                return 0;
            }
            pos -= n;
            return at(pos, n);
        }

        int peek(int n) {
            return at(pos - n, n);
        }

        private int at(int p, int n) {
            if (p < 0) {
                int avail = n + p;
                if (avail <= 0) {
                    return 0;
                }
                return get(0, avail) << -p;
            }
            return get(p, n);
        }

        private int get(int p, int n) {
            int idx = start + (p >>> 3);
            int shift = p & 7;
            int count = (shift + n + 7) >>> 3;
            long v = 0;
            for (int i = 0; i < count; i++) {
                v |= (long) (data[idx + i] & 0xFF) << (8 * i);
            }
            return (int) ((v >>> shift) & ((1L << n) - 1));
        }
    }

    /// An FSE decoding table: per state, the symbol, the number of bits to
    /// read and the baseline the bits are added to.
    private static final class Fse {
        final int log;
        final int[] sym;
        final int[] nb;
        final int[] base;

        private Fse(int log) {
            this.log = log;
            int size = 1 << log;
            sym = new int[size];
            nb = new int[size];
            base = new int[size];
        }

        static Fse rle(int symbol) {
            Fse t = new Fse(0);
            t.sym[0] = symbol;
            return t;
        }

        static Fse build(int[] probs, int count, int log) {
            Fse t = new Fse(log);
            int size = 1 << log;
            int high = size - 1;
            int total = 0;
            for (int s = 0; s < count; s++) {
                int p = probs[s];
                if (p == -1) {
                    if (high < 0) {
                        throw new IllegalArgumentException("zstd: bad FSE distribution");
                    }
                    t.sym[high--] = s;
                    total++;
                } else {
                    total += p;
                }
            }
            if (total != size) {
                throw new IllegalArgumentException("zstd: bad FSE distribution");
            }
            int step = (size >>> 1) + (size >>> 3) + 3;
            int mask = size - 1;
            int pos = 0;
            for (int s = 0; s < count; s++) {
                for (int i = 0; i < probs[s]; i++) {
                    t.sym[pos] = s;
                    do {
                        pos = (pos + step) & mask;
                    } while (pos > high);
                }
            }
            if (pos != 0) {
                throw new IllegalArgumentException("zstd: bad FSE distribution");
            }
            int[] next = new int[count];
            for (int s = 0; s < count; s++) {
                next[s] = probs[s] == -1 ? 1 : probs[s];
            }
            for (int u = 0; u < size; u++) {
                int s = t.sym[u];
                int state = next[s]++;
                int bits = log - highBit(state);
                t.nb[u] = bits;
                t.base[u] = (state << bits) - size;
            }
            return t;
        }
    }
}
