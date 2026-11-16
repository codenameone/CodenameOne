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

import java.io.IOException;
import java.util.HashMap;

/// A JBIG2 (ITU T.88) decoder for the embedded organisation PDF's `JBIG2Decode` filter uses:
/// generic, refinement, text and halftone regions, symbol and pattern dictionaries, arithmetic
/// and Huffman (standard and custom table) coding, and MMR. Bitmaps are held one byte per
/// pixel (1 = black) while decoding; the page is packed at the end.
final class Jbig2Decoder {
    private static final int MAX_PIXELS = 25000000;
    private static final long BUDGET = 150000000L;
    static final int OOB = Integer.MIN_VALUE;

    private final HashMap<Integer, Object> segments = new HashMap<Integer, Object>();
    private Bmp page;
    private boolean pageDefault;
    private boolean unknownHeight;
    private int pageEndRow;
    private boolean pageDone;
    private long allocated;
    private final int hintH;

    private Jbig2Decoder(int hintH) {
        this.hintH = hintH;
    }

    /// Decodes the embedded-organisation JBIG2 stream `data`; `params` may carry
    /// `JBIG2Globals`. Rows of packed 1-bit samples, 1 = white, or null.
    static byte[] decode(byte[] data, PdfDict params, int width, int height) {
        if (data == null) {
            return null;
        }
        Jbig2Decoder d = new Jbig2Decoder(height);
        if (params != null) {
            Object g = params.get("JBIG2Globals");
            if (g instanceof PdfStream) {
                byte[] gd = null;
                try {
                    gd = ((PdfStream) g).getData();
                } catch (IOException e) {
                    gd = null;
                }
                if (gd != null) {
                    try {
                        d.parse(gd, true);
                    } catch (RuntimeException e) {
                        // a broken globals stream leaves what it defined
                    }
                }
            }
        }
        try {
            d.parse(data, false);
        } catch (RuntimeException e) {
            // keep what was decoded
        }
        return d.output(width, height);
    }

    private byte[] output(int width, int height) {
        Bmp p = page;
        if (p == null) {
            return null;
        }
        int w = width > 0 ? width : p.w;
        int h = height > 0 ? height : (unknownHeight ? Math.min(p.h, pageEndRow) : p.h);
        if ((long) w * h > MAX_PIXELS || w <= 0 || h <= 0) {
            return null;
        }
        int rb = (w + 7) >> 3;
        byte[] out = new byte[rb * h];
        for (int y = 0; y < h; y++) {
            int o = y * rb;
            for (int x = 0; x < w; x++) {
                int v;
                if (x < p.w && y < p.h) {
                    v = p.p[y * p.w + x];
                } else {
                    v = pageDefault ? 1 : 0;
                }
                if (v == 0) {
                    out[o + (x >> 3)] |= (byte) (0x80 >> (x & 7));
                }
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- segments

    private static int u8(byte[] d, int i) {
        if (i < 0 || i >= d.length) {
            throw new IllegalStateException("truncated");
        }
        return d[i] & 0xff;
    }

    private static int u16(byte[] d, int i) {
        return (u8(d, i) << 8) | u8(d, i + 1);
    }

    private static int s32(byte[] d, int i) {
        return (u8(d, i) << 24) | (u8(d, i + 1) << 16) | (u8(d, i + 2) << 8) | u8(d, i + 3);
    }

    private static long u32(byte[] d, int i) {
        return s32(d, i) & 0xffffffffL;
    }

    private void parse(byte[] d, boolean global) {
        int n = d.length;
        int pos = 0;
        while (pos + 11 <= n && !pageDone) {
            long num = u32(d, pos);
            pos += 4;
            int flags = u8(d, pos++);
            int type = flags & 63;
            boolean pa4 = (flags & 64) != 0;
            int rb = u8(d, pos);
            int refCount = rb >>> 5;
            if (refCount == 7) {
                refCount = (int) (u32(d, pos) & 0x1fffffffL);
                if (refCount > n) {
                    return;
                }
                pos += 4 + ((refCount + 8) >> 3);
            } else {
                pos++;
            }
            int refSize = num <= 256 ? 1 : (num <= 65536 ? 2 : 4);
            if ((long) pos + (long) refCount * refSize > n) {
                return;
            }
            int[] refs = new int[refCount];
            for (int i = 0; i < refCount; i++) {
                refs[i] = refSize == 1 ? u8(d, pos) : (refSize == 2 ? u16(d, pos) : s32(d, pos));
                pos += refSize;
            }
            pos += pa4 ? 4 : 1;
            if (pos + 4 > n) {
                return;
            }
            long len = u32(d, pos);
            pos += 4;
            int start = pos;
            int end;
            if (len == 0xffffffffL) {
                if (type != 38 && type != 39) {
                    return;
                }
                end = findGenericEnd(d, start);
            } else {
                end = (int) Math.min((long) n, start + len);
            }
            try {
                segment(type, (int) num, refs, d, start, end, len == 0xffffffffL);
            } catch (RuntimeException e) {
                // a damaged segment is skipped
            }
            if (type == 51 || (type == 49 && !global)) {
                return;
            }
            pos = end;
        }
    }

    private static int findGenericEnd(byte[] d, int start) {
        int n = d.length;
        if (start + 18 > n) {
            return n;
        }
        boolean mmr = (d[start + 17] & 1) != 0;
        int m0 = mmr ? 0 : 0xff;
        int m1 = mmr ? 0 : 0xac;
        for (int i = start + 18; i + 6 <= n; i++) {
            if ((d[i] & 0xff) == m0 && (d[i + 1] & 0xff) == m1) {
                return i + 6;
            }
        }
        return n;
    }

    private void segment(int type, int num, int[] refs, byte[] d, int s, int e, boolean unknownLen) {
        switch (type) {
            case 0:
                symbolDictionary(num, refs, d, s, e);
                break;
            case 4:
            case 6:
            case 7:
                textRegion(type, num, refs, d, s, e);
                break;
            case 16:
                patternDictionary(num, d, s, e);
                break;
            case 20:
            case 22:
            case 23:
                halftoneRegion(type, num, refs, d, s, e);
                break;
            case 36:
            case 38:
            case 39:
                genericRegion(type, num, d, s, e, unknownLen);
                break;
            case 40:
            case 42:
            case 43:
                refinementRegion(type, num, refs, d, s, e);
                break;
            case 48:
                pageInfo(d, s);
                break;
            case 49:
                pageDone = true;
                break;
            case 50:
                if (page != null) {
                    pageEndRow = Math.max(pageEndRow, s32(d, s) + 1);
                    ensureHeight(pageEndRow);
                }
                break;
            case 53:
                segments.put(Integer.valueOf(num), Huff.custom(d, s, e));
                break;
            default:
                break;
        }
    }

    private void pageInfo(byte[] d, int s) {
        long w = u32(d, s);
        long h = u32(d, s + 4);
        int flags = u8(d, s + 16);
        pageDefault = ((flags >> 2) & 1) != 0;
        unknownHeight = h == 0xffffffffL;
        if (unknownHeight) {
            h = hintH > 0 ? hintH : 1;
        }
        if (w <= 0 || w * h > MAX_PIXELS) {
            throw new IllegalStateException("page size");
        }
        page = alloc((int) w, (int) h);
        if (pageDefault) {
            fill(page.p, (byte) 1);
        }
    }

    private void ensureHeight(int h) {
        if (!unknownHeight || page == null || h <= page.h) {
            return;
        }
        if ((long) page.w * h > MAX_PIXELS) {
            throw new IllegalStateException("page size");
        }
        Bmp n = alloc(page.w, h);
        if (pageDefault) {
            fill(n.p, (byte) 1);
        }
        System.arraycopy(page.p, 0, n.p, 0, page.p.length);
        page = n;
    }

    private static void fill(byte[] a, byte v) {
        for (int i = 0; i < a.length; i++) {
            a[i] = v;
        }
    }

    private Bmp alloc(int w, int h) {
        if (w < 0 || h < 0 || (long) w * h > MAX_PIXELS) {
            throw new IllegalStateException("bitmap size");
        }
        allocated += (long) w * h + 16;
        if (allocated > BUDGET) {
            throw new IllegalStateException("budget");
        }
        return new Bmp(w, h);
    }

    /// Region segment information field (7.4.1): w, h, x, y, combination operator.
    private static int[] regionInfo(byte[] d, int s) {
        return new int[] {s32(d, s), s32(d, s + 4), s32(d, s + 8), s32(d, s + 12), u8(d, s + 16) & 7};
    }

    private void place(Bmp r, int x, int y, int op) {
        if (page == null) {
            return;
        }
        if (unknownHeight) {
            ensureHeight(y + r.h);
        }
        combine(page, r, x, y, op);
    }

    private static void combine(Bmp dst, Bmp src, int x0, int y0, int op) {
        int ys = Math.max(0, -y0);
        int ye = Math.min(src.h, dst.h - y0);
        int xs = Math.max(0, -x0);
        int xe = Math.min(src.w, dst.w - x0);
        byte[] dp = dst.p;
        byte[] sp = src.p;
        for (int y = ys; y < ye; y++) {
            int so = y * src.w;
            int dO = (y + y0) * dst.w + x0;
            for (int x = xs; x < xe; x++) {
                int a = dp[dO + x];
                int b = sp[so + x];
                int v;
                switch (op) {
                    case 0:
                        v = a | b;
                        break;
                    case 1:
                        v = a & b;
                        break;
                    case 2:
                        v = a ^ b;
                        break;
                    case 3:
                        v = (a ^ b) ^ 1;
                        break;
                    default:
                        v = b;
                        break;
                }
                dp[dO + x] = (byte) v;
            }
        }
    }

    private Bmp sub(Bmp src, int x0, int y0, int w, int h) {
        Bmp r = alloc(w, h);
        for (int y = 0; y < h; y++) {
            int sy = y + y0;
            if (sy < 0 || sy >= src.h) {
                continue;
            }
            for (int x = 0; x < w; x++) {
                int sx = x + x0;
                if (sx >= 0 && sx < src.w) {
                    r.p[y * w + x] = src.p[sy * src.w + sx];
                }
            }
        }
        return r;
    }

    // ---------------------------------------------------------------- generic region

    private void genericRegion(int type, int num, byte[] d, int s, int e, boolean unknownLen) {
        int[] ri = regionInfo(d, s);
        int pos = s + 17;
        int flags = u8(d, pos++);
        boolean mmr = (flags & 1) != 0;
        int tmpl = (flags >> 1) & 3;
        boolean tpgdon = (flags & 8) != 0;
        int[] at = new int[8];
        if (!mmr) {
            int na = tmpl == 0 ? 8 : 2;
            for (int i = 0; i < na; i++) {
                at[i] = (byte) u8(d, pos++);
            }
        }
        int h = ri[1];
        if (unknownLen && e - 4 >= pos) {
            h = s32(d, e - 4);
        }
        Bmp b = alloc(ri[0], h);
        if (mmr) {
            CcittDecoder.decodeG4Pixels(d, pos, e, b.w, b.h, b.p);
        } else {
            Mq m = new Mq(d, pos, e);
            generic(m, new int[1 << 16], b, tmpl, tpgdon, at, null);
        }
        store(type == 36, num, b, ri);
    }

    private void store(boolean intermediate, int num, Bmp b, int[] ri) {
        if (intermediate) {
            segments.put(Integer.valueOf(num), b);
        } else {
            place(b, ri[2], ri[3], ri[4]);
        }
    }

    private static final int AT = 1000;
    private static final int[][] GX = {
        {AT + 3, -1, 0, 1, AT + 2, AT + 1, -2, -1, 0, 1, 2, AT, -4, -3, -2, -1},
        {-1, 0, 1, 2, -2, -1, 0, 1, 2, AT, -3, -2, -1},
        {-1, 0, 1, -2, -1, 0, 1, AT, -2, -1},
        {-3, -2, -1, 0, 1, AT, -4, -3, -2, -1}
    };
    private static final int[][] GY = {
        {-2, -2, -2, -2, -2, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0},
        {-2, -2, -2, -2, -1, -1, -1, -1, -1, -1, 0, 0, 0},
        {-2, -2, -2, -1, -1, -1, -1, -1, 0, 0},
        {-1, -1, -1, -1, -1, -1, 0, 0, 0, 0}
    };
    private static final int[] SLTP = {0x9b25, 0x0795, 0x00e5, 0x0195};

    /// Generic region decoding (6.2.5.7) into `b`; `at` holds the AT pixels as x,y pairs.
    static void generic(Mq m, int[] cx, Bmp b, int tmpl, boolean tpgdon, int[] at, Bmp skip) {
        int w = b.w;
        int h = b.h;
        byte[] p = b.p;
        int[] tx = GX[tmpl];
        int n = tx.length;
        int[] dx = new int[n];
        int[] dy = new int[n];
        int minX = 0;
        int maxX = 0;
        int minY = 0;
        int maxY = 0;
        for (int k = 0; k < n; k++) {
            if (tx[k] >= AT) {
                int a = tx[k] - AT;
                dx[k] = at[2 * a];
                dy[k] = at[2 * a + 1];
            } else {
                dx[k] = tx[k];
                dy[k] = GY[tmpl][k];
            }
            minX = Math.min(minX, dx[k]);
            maxX = Math.max(maxX, dx[k]);
            minY = Math.min(minY, dy[k]);
            maxY = Math.max(maxY, dy[k]);
        }
        int[] off = new int[n];
        for (int k = 0; k < n; k++) {
            off[k] = dy[k] * w + dx[k];
        }
        int ltp = 0;
        int sltp = SLTP[tmpl];
        for (int y = 0; y < h; y++) {
            if (tpgdon) {
                ltp ^= m.decode(cx, sltp);
                if (ltp == 1) {
                    if (y > 0) {
                        System.arraycopy(p, (y - 1) * w, p, y * w, w);
                    }
                    continue;
                }
            }
            boolean rowSafe = y + minY >= 0 && y + maxY < h;
            int row = y * w;
            for (int x = 0; x < w; x++) {
                if (skip != null && skip.p[row + x] != 0) {
                    continue;
                }
                int c = 0;
                if (rowSafe && x + minX >= 0 && x + maxX < w) {
                    int base = row + x;
                    for (int k = 0; k < n; k++) {
                        c = (c << 1) | p[base + off[k]];
                    }
                } else {
                    for (int k = 0; k < n; k++) {
                        int xx = x + dx[k];
                        int yy = y + dy[k];
                        c <<= 1;
                        if (xx >= 0 && xx < w && yy >= 0 && yy < h) {
                            c |= p[yy * w + xx];
                        }
                    }
                }
                p[row + x] = (byte) m.decode(cx, c);
            }
        }
    }

    // ---------------------------------------------------------------- refinement

    // reference pixels flagged by R, AT slots by AT; MSB first
    private static final int[][] RX = {
        {AT + 1, 0, 1, -1, 0, 1, -1, 0, 1, AT, 0, 1, -1},
        {0, -1, 0, 1, 0, 1, -1, 0, 1, -1}
    };
    private static final int[][] RY = {
        {0, -1, -1, 0, 0, 0, 1, 1, 1, 0, -1, -1, 0},
        {-1, 0, 0, 0, 1, 1, -1, -1, -1, 0}
    };
    private static final boolean[][] RREF = {
        {true, true, true, true, true, true, true, true, true, false, false, false, false},
        {true, true, true, true, true, true, false, false, false, false}
    };

    /// Generic refinement region decoding (6.3.5.6).
    Bmp refine(Mq m, int[] cx, int w, int h, int tmpl, Bmp ref, int rdx, int rdy, boolean tpgron,
            int[] at) {
        Bmp b = alloc(w, h);
        byte[] p = b.p;
        int[] tx = RX[tmpl];
        int n = tx.length;
        int[] dx = new int[n];
        int[] dy = new int[n];
        boolean[] isRef = RREF[tmpl];
        for (int k = 0; k < n; k++) {
            if (tx[k] >= AT) {
                int a = tx[k] - AT;
                dx[k] = at[2 * a];
                dy[k] = at[2 * a + 1];
            } else {
                dx[k] = tx[k];
                dy[k] = RY[tmpl][k];
            }
        }
        int rw = ref.w;
        int rh = ref.h;
        byte[] rp = ref.p;
        int ltp = 0;
        int sltp = tmpl == 0 ? 0x0100 : 0x0040;
        for (int y = 0; y < h; y++) {
            if (tpgron) {
                ltp ^= m.decode(cx, sltp);
            }
            for (int x = 0; x < w; x++) {
                int rx = x - rdx;
                int ry = y - rdy;
                if (ltp == 1) {
                    int v = typical(rp, rw, rh, rx, ry);
                    if (v >= 0) {
                        p[y * w + x] = (byte) v;
                        continue;
                    }
                }
                int c = 0;
                for (int k = 0; k < n; k++) {
                    c <<= 1;
                    if (isRef[k]) {
                        int xx = rx + dx[k];
                        int yy = ry + dy[k];
                        if (xx >= 0 && xx < rw && yy >= 0 && yy < rh) {
                            c |= rp[yy * rw + xx];
                        }
                    } else {
                        int xx = x + dx[k];
                        int yy = y + dy[k];
                        if (xx >= 0 && xx < w && yy >= 0 && yy < h) {
                            c |= p[yy * w + xx];
                        }
                    }
                }
                p[y * w + x] = (byte) m.decode(cx, c);
            }
        }
        return b;
    }

    private static int typical(byte[] rp, int rw, int rh, int cx, int cy) {
        int first = -1;
        for (int yy = cy - 1; yy <= cy + 1; yy++) {
            for (int xx = cx - 1; xx <= cx + 1; xx++) {
                int v = xx >= 0 && xx < rw && yy >= 0 && yy < rh ? rp[yy * rw + xx] : 0;
                if (first < 0) {
                    first = v;
                } else if (v != first) {
                    return -1;
                }
            }
        }
        return first;
    }

    private void refinementRegion(int type, int num, int[] refs, byte[] d, int s, int e) {
        int[] ri = regionInfo(d, s);
        int pos = s + 17;
        int flags = u8(d, pos++);
        int tmpl = flags & 1;
        boolean tpgron = (flags & 2) != 0;
        int[] at = new int[4];
        if (tmpl == 0) {
            for (int i = 0; i < 4; i++) {
                at[i] = (byte) u8(d, pos++);
            }
        }
        Bmp ref = null;
        for (int i = 0; i < refs.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Object o = segments.get(Integer.valueOf(refs[i]));
            if (o instanceof Bmp) {
                ref = (Bmp) o;
            }
        }
        if (ref == null) {
            if (page == null) {
                return;
            }
            ref = sub(page, ri[2], ri[3], ri[0], ri[1]);
        }
        Mq m = new Mq(d, pos, e);
        Bmp b = refine(m, new int[1 << 13], ri[0], ri[1], tmpl, ref, 0, 0, tpgron, at);
        store(type == 40, num, b, ri);
    }

    // ---------------------------------------------------------------- symbols and text

    private void symbolDictionary(int num, int[] refs, byte[] d, int s, int e) {
        int pos = s;
        int flags = u16(d, pos);
        pos += 2;
        boolean huff = (flags & 1) != 0;
        boolean refagg = (flags & 2) != 0;
        int dhSel = (flags >> 2) & 3;
        int dwSel = (flags >> 4) & 3;
        int bmSel = (flags >> 6) & 1;
        int aggSel = (flags >> 7) & 1;
        int tmpl = (flags >> 10) & 3;
        int rtmpl = (flags >> 12) & 1;
        int[] at = new int[8];
        if (!huff) {
            int na = tmpl == 0 ? 8 : 2;
            for (int i = 0; i < na; i++) {
                at[i] = (byte) u8(d, pos++);
            }
        }
        int[] rat = new int[4];
        if (refagg && rtmpl == 0) {
            for (int i = 0; i < 4; i++) {
                rat[i] = (byte) u8(d, pos++);
            }
        }
        long numEx = u32(d, pos);
        long numNewL = u32(d, pos + 4);
        pos += 8;
        Bmp[] in = inputSymbols(refs);
        Huff[] custom = customTables(refs);
        int ci = 0;
        if (numNewL > (long) (e - pos) * 8 + 64) {
            throw new IllegalStateException("symbols");
        }
        int numNew = (int) numNewL;
        int total = in.length + numNew;
        int codeLen = log2(total);
        if (huff && codeLen < 1) {
            codeLen = 1;
        }
        Bmp[] all = new Bmp[total];
        System.arraycopy(in, 0, all, 0, in.length);
        Text t = new Text(this, codeLen, huff);
        Huff dhT = null;
        Huff dwT = null;
        Huff bmT = null;
        Huff aggT = null;
        Bits b = null;
        Mq m = null;
        int[] iadh = null;
        int[] iadw = null;
        int[] iaai = null;
        int[] iaex = null;
        int[] gbcx = null;
        if (huff) {
            dhT = dhSel == 0 ? Huff.std(4) : (dhSel == 1 ? Huff.std(5) : custom[ci++]);
            dwT = dwSel == 0 ? Huff.std(2) : (dwSel == 1 ? Huff.std(3) : custom[ci++]);
            bmT = bmSel == 0 ? Huff.std(1) : custom[ci++];
            aggT = aggSel == 0 ? Huff.std(1) : custom[ci++];
            b = new Bits(d, pos, e);
            t.useHuffman(b, Huff.std(6), Huff.std(8), Huff.std(11), Huff.std(15), Huff.std(15),
                    Huff.std(15), Huff.std(15), Huff.std(1), null, 0);
        } else {
            m = new Mq(d, pos, e);
            t.m = m;
            iadh = new int[512];
            iadw = new int[512];
            iaai = new int[512];
            iaex = new int[512];
            gbcx = new int[1 << 16];
        }
        int[] widths = new int[numNew];
        int hcHeight = 0;
        int nsd = 0;
        while (nsd < numNew) {
            int hcdh = huff ? dhT.decode(b) : m.decodeInt(iadh);
            if (hcdh == OOB) {
                throw new IllegalStateException("height");
            }
            hcHeight += hcdh;
            if (hcHeight < 0 || hcHeight > 65535) {
                throw new IllegalStateException("height");
            }
            int symWidth = 0;
            int totWidth = 0;
            int hcFirst = nsd;
            while (true) {
                int dw = huff ? dwT.decode(b) : m.decodeInt(iadw);
                if (dw == OOB) {
                    break;
                }
                if (nsd >= numNew) {
                    throw new IllegalStateException("symbols");
                }
                symWidth += dw;
                if (symWidth < 0 || symWidth > 65535) {
                    throw new IllegalStateException("width");
                }
                totWidth += symWidth;
                if (!huff || refagg) {
                    Bmp sym;
                    if (!refagg) {
                        sym = alloc(symWidth, hcHeight);
                        generic(m, gbcx, sym, tmpl, false, at, null);
                    } else {
                        int ninst = huff ? aggT.decode(b) : m.decodeInt(iaai);
                        Bmp[] avail = new Bmp[in.length + nsd];
                        System.arraycopy(all, 0, avail, 0, avail.length);
                        if (ninst == 1) {
                            int id;
                            int rdx;
                            int rdy;
                            Mq rm = m;
                            int skipTo = -1;
                            if (huff) {
                                id = b.read(codeLen);
                                rdx = t.rdxT.decode(b);
                                rdy = t.rdyT.decode(b);
                                int bmsize = Huff.std(1).decode(b);
                                b.align();
                                int st = b.bytePos();
                                rm = new Mq(d, st, Math.min(e, st + bmsize));
                                skipTo = st + bmsize;
                            } else {
                                id = m.decodeId(t.iaid, codeLen);
                                rdx = m.decodeInt(t.iardx);
                                rdy = m.decodeInt(t.iardy);
                            }
                            if (id < 0 || id >= avail.length || rdx == OOB || rdy == OOB) {
                                throw new IllegalStateException("symbol id");
                            }
                            sym = refine(rm, t.grcx(rtmpl), symWidth, hcHeight, rtmpl, avail[id], rdx, rdy,
                                    false, rat);
                            if (skipTo >= 0) {
                                b.seekByte(skipTo);
                            }
                        } else {
                            if (ninst <= 0) {
                                throw new IllegalStateException("instances");
                            }
                            sym = t.region(symWidth, hcHeight, ninst, 1, false, 0, false, 1, 0, true, rtmpl,
                                    rat, avail);
                        }
                    }
                    all[in.length + nsd] = sym;
                }
                widths[nsd] = symWidth;
                nsd++;
            }
            if (huff && !refagg) {
                int bmsize = bmT.decode(b);
                b.align();
                int st = b.bytePos();
                Bmp coll = alloc(totWidth, hcHeight);
                if (bmsize == 0) {
                    int rbytes = (totWidth + 7) >> 3;
                    for (int y = 0; y < hcHeight; y++) {
                        for (int x = 0; x < totWidth; x++) {
                            int v = u8(d, st + y * rbytes + (x >> 3));
                            coll.p[y * totWidth + x] = (byte) ((v >> (7 - (x & 7))) & 1);
                        }
                    }
                    b.seekByte(st + rbytes * hcHeight);
                } else {
                    CcittDecoder.decodeG4Pixels(d, st, Math.min(e, st + bmsize), totWidth, hcHeight, coll.p);
                    b.seekByte(st + bmsize);
                }
                int x = 0;
                for (int i = hcFirst; i < nsd; i++) {
                    all[in.length + i] = sub(coll, x, 0, widths[i], hcHeight);
                    x += widths[i];
                }
            }
        }
        boolean[] ex = new boolean[total];
        boolean cur = false;
        int i = 0;
        Huff exT = huff ? Huff.std(1) : null;
        while (i < total) {
            int run = huff ? exT.decode(b) : m.decodeInt(iaex);
            if (run == OOB || run < 0 || run > total - i) {
                throw new IllegalStateException("export");
            }
            for (int k = 0; k < run; k++) {
                ex[i++] = cur;
            }
            cur = !cur;
        }
        int cnt = 0;
        for (int k = 0; k < total; k++) {
            if (ex[k] && cnt < numEx) {
                cnt++;
            }
        }
        Bmp[] out = new Bmp[cnt];
        int o = 0;
        for (int k = 0; k < total && o < cnt; k++) {
            if (ex[k]) {
                out[o++] = all[k] != null ? all[k] : new Bmp(0, 0);
            }
        }
        segments.put(Integer.valueOf(num), new SymDict(out));
    }

    private Bmp[] inputSymbols(int[] refs) {
        int n = 0;
        for (int i = 0; i < refs.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Object o = segments.get(Integer.valueOf(refs[i]));
            if (o instanceof SymDict) {
                n += ((SymDict) o).syms.length;
            }
        }
        Bmp[] r = new Bmp[n];
        int k = 0;
        for (int i = 0; i < refs.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Object o = segments.get(Integer.valueOf(refs[i]));
            if (o instanceof SymDict) {
                Bmp[] s = ((SymDict) o).syms;
                System.arraycopy(s, 0, r, k, s.length);
                k += s.length;
            }
        }
        return r;
    }

    private Huff[] customTables(int[] refs) {
        Huff[] r = new Huff[refs.length + 8];
        int k = 0;
        for (int i = 0; i < refs.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Object o = segments.get(Integer.valueOf(refs[i]));
            if (o instanceof Huff) {
                r[k++] = (Huff) o;
            }
        }
        return r;
    }

    static int log2(int n) {
        int l = 0;
        while (l < 31 && (1 << l) < n) {
            l++;
        }
        return l;
    }

    private void textRegion(int type, int num, int[] refs, byte[] d, int s, int e) {
        int[] ri = regionInfo(d, s);
        int pos = s + 17;
        int flags = u16(d, pos);
        pos += 2;
        boolean huff = (flags & 1) != 0;
        boolean refine = (flags & 2) != 0;
        int logStrips = (flags >> 2) & 3;
        int refCorner = (flags >> 4) & 3;
        boolean transposed = (flags & 0x40) != 0;
        int combop = (flags >> 7) & 3;
        boolean defPix = (flags & 0x200) != 0;
        int dsOffset = (flags >> 10) & 0x1f;
        if (dsOffset > 15) {
            dsOffset -= 32;
        }
        int rtmpl = (flags >> 15) & 1;
        int hflags = 0;
        if (huff) {
            hflags = u16(d, pos);
            pos += 2;
        }
        int[] rat = new int[4];
        if (refine && rtmpl == 0) {
            for (int i = 0; i < 4; i++) {
                rat[i] = (byte) u8(d, pos++);
            }
        }
        long numInst = u32(d, pos);
        pos += 4;
        Bmp[] syms = inputSymbols(refs);
        int codeLen = log2(syms.length);
        Text t = new Text(this, codeLen, huff);
        if (huff) {
            Huff[] custom = customTables(refs);
            int ci = 0;
            int sel = hflags & 3;
            Huff fs = sel == 0 ? Huff.std(6) : (sel == 1 ? Huff.std(7) : custom[ci++]);
            sel = (hflags >> 2) & 3;
            Huff ds = sel < 3 ? Huff.std(8 + sel) : custom[ci++];
            sel = (hflags >> 4) & 3;
            Huff dt = sel < 3 ? Huff.std(11 + sel) : custom[ci++];
            Huff[] rd = new Huff[4];
            for (int k = 0; k < 4; k++) {
                sel = (hflags >> (6 + 2 * k)) & 3;
                rd[k] = sel == 0 ? Huff.std(14) : (sel == 1 ? Huff.std(15) : custom[ci++]);
            }
            Huff rsize = ((hflags >> 14) & 1) == 0 ? Huff.std(1) : custom[ci++];
            Bits b = new Bits(d, pos, e);
            Huff idT = Huff.symbolIds(b, syms.length);
            b.align();
            t.useHuffman(b, fs, ds, dt, rd[0], rd[1], rd[2], rd[3], rsize, idT, logStrips);
        } else {
            t.m = new Mq(d, pos, e);
        }
        if (numInst > (long) (e - pos) * 8 + 64) {
            numInst = (long) (e - pos) * 8 + 64;
        }
        Bmp r = t.region(ri[0], ri[1], (int) numInst, 1 << logStrips, defPix, combop, transposed,
                refCorner, dsOffset, refine, rtmpl, rat, syms);
        store(type == 4, num, r, ri);
    }

    // ---------------------------------------------------------------- halftone

    private void patternDictionary(int num, byte[] d, int s, int e) {
        int flags = u8(d, s);
        boolean mmr = (flags & 1) != 0;
        int tmpl = (flags >> 1) & 3;
        int pw = u8(d, s + 1);
        int ph = u8(d, s + 2);
        long grayMax = u32(d, s + 3);
        int pos = s + 7;
        if ((grayMax + 1) * pw * ph > MAX_PIXELS || pw == 0 || ph == 0) {
            throw new IllegalStateException("patterns");
        }
        int n = (int) grayMax + 1;
        Bmp coll = alloc(n * pw, ph);
        if (mmr) {
            CcittDecoder.decodeG4Pixels(d, pos, e, coll.w, coll.h, coll.p);
        } else {
            int[] at = {-pw, 0, -3, -1, 2, -2, -2, -2};
            generic(new Mq(d, pos, e), new int[1 << 16], coll, tmpl, false, at, null);
        }
        Bmp[] pats = new Bmp[n];
        for (int i = 0; i < n; i++) {
            pats[i] = sub(coll, i * pw, 0, pw, ph);
        }
        segments.put(Integer.valueOf(num), new PatDict(pats));
    }

    private void halftoneRegion(int type, int num, int[] refs, byte[] d, int s, int e) {
        int[] ri = regionInfo(d, s);
        int pos = s + 17;
        int flags = u8(d, pos++);
        boolean mmr = (flags & 1) != 0;
        int tmpl = (flags >> 1) & 3;
        boolean enableSkip = (flags & 8) != 0;
        int hcombop = (flags >> 4) & 7;
        boolean defPix = (flags & 0x80) != 0;
        long gw = u32(d, pos);
        long gh = u32(d, pos + 4);
        int gx = s32(d, pos + 8);
        int gy = s32(d, pos + 12);
        int rx = u16(d, pos + 16);
        int ry = u16(d, pos + 18);
        pos += 20;
        Bmp[] pats = null;
        for (int i = 0; i < refs.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Object o = segments.get(Integer.valueOf(refs[i]));
            if (o instanceof PatDict) {
                pats = ((PatDict) o).pats;
            }
        }
        if (pats == null || pats.length == 0 || gw * gh > MAX_PIXELS) {
            throw new IllegalStateException("halftone");
        }
        Bmp r = alloc(ri[0], ri[1]);
        if (defPix) {
            fill(r.p, (byte) 1);
        }
        int pw = pats[0].w;
        int ph = pats[0].h;
        int w = (int) gw;
        int h = (int) gh;
        Bmp skip = null;
        if (enableSkip) {
            skip = alloc(w, h);
            for (int mg = 0; mg < h; mg++) {
                for (int ng = 0; ng < w; ng++) {
                    int x = (gx + mg * ry + ng * rx) >> 8;
                    int y = (gy + mg * rx - ng * ry) >> 8;
                    if (x + pw <= 0 || x >= r.w || y + ph <= 0 || y >= r.h) {
                        skip.p[mg * w + ng] = 1;
                    }
                }
            }
        }
        int bpp = log2(pats.length);
        int[] gray = new int[w * h];
        Bmp prev = null;
        Mq m = mmr ? null : new Mq(d, pos, e);
        int[] cx = mmr ? null : new int[1 << 16];
        int[] at = {tmpl <= 1 ? 3 : 2, -1, -3, -1, 2, -2, -2, -2};
        for (int j = bpp - 1; j >= 0; j--) {
            Bmp plane = alloc(w, h);
            if (mmr) {
                pos += CcittDecoder.decodeG4Pixels(d, pos, e, w, h, plane.p);
            } else {
                generic(m, cx, plane, tmpl, false, at, skip);
            }
            if (prev != null) {
                for (int k = 0; k < plane.p.length; k++) {
                    plane.p[k] ^= prev.p[k];
                }
            }
            for (int k = 0; k < gray.length; k++) {
                gray[k] |= plane.p[k] << j;
            }
            prev = plane;
        }
        for (int mg = 0; mg < h; mg++) {
            for (int ng = 0; ng < w; ng++) {
                int x = (gx + mg * ry + ng * rx) >> 8;
                int y = (gy + mg * rx - ng * ry) >> 8;
                int g = gray[mg * w + ng];
                if (g >= pats.length) {
                    g = pats.length - 1;
                }
                combine(r, pats[g], x, y, hcombop);
            }
        }
        store(type == 20, num, r, ri);
    }

    // ---------------------------------------------------------------- helper types

    static final class Bmp {
        final int w;
        final int h;
        final byte[] p;

        Bmp(int w, int h) {
            this.w = w;
            this.h = h;
            this.p = new byte[w * h];
        }
    }

    static final class SymDict {
        final Bmp[] syms;

        SymDict(Bmp[] s) {
            syms = s;
        }
    }

    static final class PatDict {
        final Bmp[] pats;

        PatDict(Bmp[] p) {
            pats = p;
        }
    }

    /// Text region decoding (6.4.5), shared by text region segments and refinement
    /// aggregation in symbol dictionaries; arithmetic or Huffman.
    static final class Text {
        final Jbig2Decoder owner;
        final boolean huff;
        final int codeLen;
        Mq m;
        final int[] iadt = new int[512];
        final int[] iafs = new int[512];
        final int[] iads = new int[512];
        final int[] iait = new int[512];
        final int[] iari = new int[512];
        final int[] iardw = new int[512];
        final int[] iardh = new int[512];
        final int[] iardx = new int[512];
        final int[] iardy = new int[512];
        final int[] iaid;
        private int[] grcx;
        Bits b;
        Huff fsT;
        Huff dsT;
        Huff dtT;
        Huff rdwT;
        Huff rdhT;
        Huff rdxT;
        Huff rdyT;
        Huff rsizeT;
        Huff idT;
        int logStrips;

        Text(Jbig2Decoder owner, int codeLen, boolean huff) {
            this.owner = owner;
            this.codeLen = Math.min(codeLen, 24);
            this.huff = huff;
            this.iaid = huff ? null : new int[1 << (this.codeLen + 1)];
        }

        void useHuffman(Bits bits, Huff fs, Huff ds, Huff dt, Huff rdw, Huff rdh, Huff rdx, Huff rdy,
                Huff rsize, Huff ids, int logS) {
            b = bits;
            fsT = fs;
            dsT = ds;
            dtT = dt;
            rdwT = rdw;
            rdhT = rdh;
            rdxT = rdx;
            rdyT = rdy;
            rsizeT = rsize;
            idT = ids;
            logStrips = logS;
        }

        int[] grcx(int tmpl) {
            if (grcx == null) {
                grcx = new int[1 << 13];
            }
            return grcx;
        }

        private static int need(int v) {
            if (v == OOB) {
                throw new IllegalStateException("oob");
            }
            return v;
        }

        Bmp region(int w, int h, int numInst, int strips, boolean defPix, int combop, boolean transposed,
                int refCorner, int dsOffset, boolean refine, int rtmpl, int[] rat, Bmp[] syms) {
            Bmp r = owner.alloc(w, h);
            if (defPix) {
                fill(r.p, (byte) 1);
            }
            int stripT = -need(huff ? dtT.decode(b) : m.decodeInt(iadt)) * strips;
            int firstS = 0;
            int ninst = 0;
            int guard = 0;
            while (ninst < numInst) {
                if (++guard > numInst + 16) { // NOPMD AssignmentInOperand - counter bumped where it is tested
                    break;
                }
                stripT += need(huff ? dtT.decode(b) : m.decodeInt(iadt)) * strips;
                firstS += need(huff ? fsT.decode(b) : m.decodeInt(iafs));
                int curS = firstS;
                while (true) {
                    int curT = 0;
                    if (strips > 1) {
                        curT = huff ? b.read(logStrips) : need(m.decodeInt(iait));
                    }
                    int tt = stripT + curT;
                    int id;
                    if (huff) {
                        id = idT != null ? idT.decode(b) : b.read(codeLen);
                    } else {
                        id = m.decodeId(iaid, codeLen);
                    }
                    if (id < 0 || id >= syms.length || syms[id] == null) {
                        throw new IllegalStateException("symbol id");
                    }
                    Bmp ib = syms[id];
                    boolean ri = false;
                    if (refine) {
                        ri = huff ? b.read(1) != 0 : need(m.decodeInt(iari)) != 0;
                    }
                    if (ri) {
                        int rdw;
                        int rdh;
                        int rdx;
                        int rdy;
                        Mq rm = m;
                        int skipTo = -1;
                        if (huff) {
                            rdw = need(rdwT.decode(b));
                            rdh = need(rdhT.decode(b));
                            rdx = need(rdxT.decode(b));
                            rdy = need(rdyT.decode(b));
                            int rsize = need(rsizeT.decode(b));
                            b.align();
                            int st = b.bytePos();
                            rm = new Mq(b.d, st, Math.min(b.endByte(), st + rsize));
                            skipTo = st + rsize;
                        } else {
                            rdw = need(m.decodeInt(iardw));
                            rdh = need(m.decodeInt(iardh));
                            rdx = need(m.decodeInt(iardx));
                            rdy = need(m.decodeInt(iardy));
                        }
                        ib = owner.refine(rm, grcx(rtmpl), ib.w + rdw, ib.h + rdh, rtmpl, ib, (rdw >> 1) + rdx,
                                (rdh >> 1) + rdy, false, rat);
                        if (skipTo >= 0) {
                            b.seekByte(skipTo);
                        }
                    }
                    int wi = ib.w;
                    int hi = ib.h;
                    if (!transposed && refCorner >= 2) {
                        curS += wi - 1;
                    } else if (transposed && (refCorner & 1) == 0) {
                        curS += hi - 1;
                    }
                    int sx;
                    int sy;
                    if (!transposed) {
                        sx = curS - ((refCorner & 2) != 0 ? wi - 1 : 0);
                        sy = tt - ((refCorner & 1) == 0 ? hi - 1 : 0);
                    } else {
                        sx = tt - ((refCorner & 2) != 0 ? wi - 1 : 0);
                        sy = curS - ((refCorner & 1) == 0 ? hi - 1 : 0);
                    }
                    combine(r, ib, sx, sy, combop);
                    if (!transposed && refCorner < 2) {
                        curS += wi - 1;
                    } else if (transposed && (refCorner & 1) != 0) {
                        curS += hi - 1;
                    }
                    ninst++;
                    int ds = huff ? dsT.decode(b) : m.decodeInt(iads);
                    if (ds == OOB || ninst >= numInst + 16) {
                        break;
                    }
                    curS += ds + dsOffset;
                }
            }
            return r;
        }
    }

    /// The MQ arithmetic decoder (Annex E.3).
    static final class Mq {
        private static final int[] QE = {
            0x5601, 0x3401, 0x1801, 0x0AC1, 0x0521, 0x0221, 0x5601, 0x5401, 0x4801, 0x3801, 0x3001, 0x2401,
            0x1C01, 0x1601, 0x5601, 0x5401, 0x5101, 0x4801, 0x3801, 0x3401, 0x3001, 0x2801, 0x2401, 0x2201,
            0x1C01, 0x1801, 0x1601, 0x1401, 0x1201, 0x1101, 0x0AC1, 0x09C1, 0x08A1, 0x0521, 0x0441, 0x02A1,
            0x0221, 0x0141, 0x0111, 0x0085, 0x0049, 0x0025, 0x0015, 0x0009, 0x0005, 0x0001, 0x5601
        };
        private static final int[] NMPS = {
            1, 2, 3, 4, 5, 38, 7, 8, 9, 10, 11, 12, 13, 29, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
            28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 45, 46
        };
        private static final int[] NLPS = {
            1, 6, 9, 12, 29, 33, 6, 14, 14, 14, 17, 18, 20, 21, 14, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 23,
            24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 46
        };
        private static final int[] SW = {
            1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };
        private final byte[] d;
        private final int end;
        private int bp;
        private int chigh;
        private int clow;
        private int ct;
        private int a;
        private int prev;

        Mq(byte[] d, int start, int end) {
            this.d = d;
            this.end = Math.min(end, d.length);
            bp = start;
            chigh = rd(bp);
            clow = 0;
            byteIn();
            chigh = ((chigh << 7) & 0xffff) | ((clow >> 9) & 0x7f);
            clow = (clow << 7) & 0xffff;
            ct -= 7;
            a = 0x8000;
        }

        private int rd(int i) {
            return i >= 0 && i < end ? d[i] & 0xff : 0xff;
        }

        private void byteIn() {
            if (rd(bp) == 0xff) {
                if (rd(bp + 1) > 0x8f) {
                    clow += 0xff00;
                    ct = 8;
                } else {
                    bp++;
                    clow += rd(bp) << 9;
                    ct = 7;
                }
            } else {
                bp++;
                clow += rd(bp) << 8;
                ct = 8;
            }
            if (clow > 0xffff) {
                chigh += clow >> 16;
                clow &= 0xffff;
            }
        }

        int decode(int[] cx, int i) {
            int st = cx[i];
            int idx = st >> 1;
            int mps = st & 1;
            int qe = QE[idx];
            int dv;
            a -= qe;
            if (chigh < qe) {
                if (a < qe) {
                    a = qe;
                    dv = mps;
                    idx = NMPS[idx];
                } else {
                    a = qe;
                    dv = 1 ^ mps;
                    if (SW[idx] == 1) {
                        mps = dv;
                    }
                    idx = NLPS[idx];
                }
            } else {
                chigh -= qe;
                if ((a & 0x8000) != 0) {
                    return mps;
                }
                if (a < qe) {
                    dv = 1 ^ mps;
                    if (SW[idx] == 1) {
                        mps = dv;
                    }
                    idx = NLPS[idx];
                } else {
                    dv = mps;
                    idx = NMPS[idx];
                }
            }
            do {
                if (ct == 0) {
                    byteIn();
                }
                a <<= 1;
                chigh = ((chigh << 1) & 0xffff) | ((clow >> 15) & 1);
                clow = (clow << 1) & 0xffff;
                ct--;
            } while ((a & 0x8000) == 0);
            cx[i] = (idx << 1) | mps;
            return dv;
        }

        private int ib(int[] cx) {
            int b = decode(cx, prev);
            prev = prev < 256 ? (prev << 1) | b : ((((prev << 1) | b) & 511) | 256);
            return b;
        }

        private int ibits(int[] cx, int n) {
            int v = 0;
            for (int i = 0; i < n; i++) {
                v = (v << 1) | ib(cx);
            }
            return v;
        }

        /// Integer decoding procedure (A.2); OOB for out-of-band.
        int decodeInt(int[] cx) {
            prev = 1;
            int s = ib(cx);
            int v;
            if (ib(cx) == 0) {
                v = ibits(cx, 2);
            } else if (ib(cx) == 0) {
                v = ibits(cx, 4) + 4;
            } else if (ib(cx) == 0) {
                v = ibits(cx, 6) + 20;
            } else if (ib(cx) == 0) {
                v = ibits(cx, 8) + 84;
            } else if (ib(cx) == 0) {
                v = ibits(cx, 12) + 340;
            } else {
                v = ibits(cx, 32) + 4436;
            }
            if (s == 1) {
                return v == 0 ? OOB : -v;
            }
            return v;
        }

        /// IAID decoding (A.3).
        int decodeId(int[] cx, int len) {
            int p = 1;
            for (int i = 0; i < len; i++) {
                p = (p << 1) | decode(cx, p);
            }
            return p - (1 << len);
        }
    }

    /// MSB-first bit reader for Huffman coded data.
    static final class Bits {
        final byte[] d;
        private int bit;
        private final int endBit;

        Bits(byte[] d, int start, int end) {
            this.d = d;
            this.bit = start * 8;
            this.endBit = Math.min(end, d.length) * 8;
        }

        int bit() {
            if (bit >= endBit) {
                throw new IllegalStateException("eod");
            }
            int v = (d[bit >> 3] >> (7 - (bit & 7))) & 1;
            bit++;
            return v;
        }

        int read(int n) {
            int v = 0;
            for (int i = 0; i < n; i++) {
                v = (v << 1) | bit();
            }
            return v;
        }

        void align() {
            bit = (bit + 7) & ~7;
        }

        int bytePos() {
            return bit >> 3;
        }

        int endByte() {
            return endBit >> 3;
        }

        void seekByte(int p) {
            bit = p * 8;
        }
    }

    /// A Huffman table (B.3 code assignment); lines of PREFLEN, RANGELEN, RANGELOW, kind.
    static final class Huff {
        private static final int NORMAL = 0;
        private static final int LOWER = 1;
        private static final int UPPER = 2;
        private static final int OOBLINE = 3;
        private final int[] pref;
        private final int[] range;
        private final int[] low;
        private final int[] kind;
        private final int[] code;
        private final int n;
        private static Huff[] std = new Huff[16];

        // PREFLEN, RANGELEN, RANGELOW, kind per line; Annex B.5, tables B.1 to B.15
        private static final int[][] STD = {
            null,
            {1, 4, 0, 0, 2, 8, 16, 0, 3, 16, 272, 0, 3, 32, 65808, 2},
            {1, 0, 0, 0, 2, 0, 1, 0, 3, 0, 2, 0, 4, 3, 3, 0, 5, 6, 11, 0, 6, 32, 75, 2, 6, 0, 0, 3},
            {8, 8, -256, 0, 1, 0, 0, 0, 2, 0, 1, 0, 3, 0, 2, 0, 4, 3, 3, 0, 5, 6, 11, 0, 8, 32, -257, 1,
                7, 32, 75, 2, 6, 0, 0, 3},
            {1, 0, 1, 0, 2, 0, 2, 0, 3, 0, 3, 0, 4, 3, 4, 0, 5, 6, 12, 0, 5, 32, 76, 2},
            {7, 8, -255, 0, 1, 0, 1, 0, 2, 0, 2, 0, 3, 0, 3, 0, 4, 3, 4, 0, 5, 6, 12, 0, 7, 32, -256, 1,
                6, 32, 76, 2},
            {5, 10, -2048, 0, 4, 9, -1024, 0, 4, 8, -512, 0, 4, 7, -256, 0, 5, 6, -128, 0, 5, 5, -64, 0,
                4, 5, -32, 0, 2, 7, 0, 0, 3, 7, 128, 0, 3, 8, 256, 0, 4, 9, 512, 0, 4, 10, 1024, 0,
                6, 32, -2049, 1, 6, 32, 2048, 2},
            {4, 9, -1024, 0, 3, 8, -512, 0, 4, 7, -256, 0, 5, 6, -128, 0, 5, 5, -64, 0, 4, 5, -32, 0,
                4, 5, 0, 0, 5, 5, 32, 0, 5, 6, 64, 0, 4, 7, 128, 0, 3, 8, 256, 0, 3, 9, 512, 0, 3, 10, 1024, 0,
                5, 32, -1025, 1, 5, 32, 2048, 2},
            {8, 3, -15, 0, 9, 1, -7, 0, 8, 1, -5, 0, 9, 0, -3, 0, 7, 0, -2, 0, 4, 0, -1, 0, 2, 1, 0, 0,
                5, 0, 2, 0, 6, 0, 3, 0, 3, 4, 4, 0, 6, 1, 20, 0, 4, 4, 22, 0, 4, 5, 38, 0, 5, 6, 70, 0,
                5, 7, 134, 0, 6, 7, 262, 0, 7, 8, 390, 0, 6, 10, 646, 0, 9, 32, -16, 1, 9, 32, 1670, 2,
                2, 0, 0, 3},
            {8, 4, -31, 0, 9, 2, -15, 0, 8, 2, -11, 0, 9, 1, -7, 0, 7, 1, -5, 0, 4, 1, -3, 0, 3, 1, -1, 0,
                3, 1, 1, 0, 5, 1, 3, 0, 6, 1, 5, 0, 3, 5, 7, 0, 6, 2, 39, 0, 4, 5, 43, 0, 4, 6, 75, 0,
                5, 7, 139, 0, 5, 8, 267, 0, 6, 8, 523, 0, 7, 9, 779, 0, 6, 11, 1291, 0, 9, 32, -32, 1,
                9, 32, 3339, 2, 2, 0, 0, 3},
            {7, 4, -21, 0, 8, 0, -5, 0, 7, 0, -4, 0, 5, 0, -3, 0, 2, 2, -2, 0, 5, 0, 2, 0, 6, 0, 3, 0,
                7, 0, 4, 0, 8, 0, 5, 0, 2, 6, 6, 0, 5, 5, 70, 0, 6, 5, 102, 0, 6, 6, 134, 0, 6, 7, 198, 0,
                6, 8, 326, 0, 6, 9, 582, 0, 6, 10, 1094, 0, 7, 11, 2118, 0, 8, 32, -22, 1, 8, 32, 4166, 2,
                2, 0, 0, 3},
            {1, 0, 1, 0, 2, 1, 2, 0, 4, 0, 4, 0, 4, 1, 5, 0, 5, 1, 7, 0, 5, 2, 9, 0, 6, 2, 13, 0,
                7, 2, 17, 0, 7, 3, 21, 0, 7, 4, 29, 0, 7, 5, 45, 0, 7, 6, 77, 0, 7, 32, 141, 2},
            {1, 0, 1, 0, 2, 0, 2, 0, 3, 1, 3, 0, 5, 0, 5, 0, 5, 1, 6, 0, 6, 1, 8, 0, 7, 0, 10, 0,
                7, 1, 11, 0, 7, 2, 13, 0, 7, 3, 17, 0, 7, 4, 25, 0, 8, 5, 41, 0, 8, 32, 73, 2},
            {1, 0, 1, 0, 3, 0, 2, 0, 4, 0, 3, 0, 5, 0, 4, 0, 4, 1, 5, 0, 3, 3, 7, 0, 6, 1, 15, 0,
                6, 2, 17, 0, 6, 3, 21, 0, 6, 4, 29, 0, 6, 5, 45, 0, 7, 6, 77, 0, 7, 32, 141, 2},
            {3, 0, -2, 0, 3, 0, -1, 0, 1, 0, 0, 0, 3, 0, 1, 0, 3, 0, 2, 0},
            {7, 4, -24, 0, 6, 2, -8, 0, 5, 1, -4, 0, 4, 0, -2, 0, 3, 0, -1, 0, 1, 0, 0, 0, 3, 0, 1, 0,
                4, 0, 2, 0, 5, 1, 3, 0, 6, 2, 5, 0, 7, 4, 9, 0, 7, 32, -25, 1, 7, 32, 25, 2}
        };

        private Huff(int[] lines, int count) {
            n = count;
            pref = new int[n];
            range = new int[n];
            low = new int[n];
            kind = new int[n];
            code = new int[n];
            int maxLen = 0;
            for (int i = 0; i < n; i++) {
                pref[i] = lines[4 * i];
                range[i] = lines[4 * i + 1];
                low[i] = lines[4 * i + 2];
                kind[i] = lines[4 * i + 3];
                maxLen = Math.max(maxLen, pref[i]);
            }
            if (maxLen > 32) {
                throw new IllegalStateException("huffman");
            }
            int[] lenCount = new int[maxLen + 1];
            for (int i = 0; i < n; i++) {
                lenCount[pref[i]]++;
            }
            int first = 0;
            lenCount[0] = 0;
            for (int len = 1; len <= maxLen; len++) {
                first = (first + lenCount[len - 1]) << 1;
                int c = first;
                for (int i = 0; i < n; i++) {
                    if (pref[i] == len) {
                        code[i] = c++;
                    }
                }
            }
        }

        static Huff std(int t) {
            if (std[t] == null) {
                std[t] = new Huff(STD[t], STD[t].length / 4);
            }
            return std[t];
        }

        /// A code table segment (B.2).
        static Huff custom(byte[] d, int s, int e) {
            int flags = u8(d, s);
            boolean oob = (flags & 1) != 0;
            int ps = ((flags >> 1) & 7) + 1;
            int rs = ((flags >> 4) & 7) + 1;
            int lo = s32(d, s + 1);
            int hi = s32(d, s + 5);
            Bits b = new Bits(d, s + 9, e);
            int[] lines = new int[64];
            int k = 0;
            long cur = lo;
            while (cur < hi) {
                if (4 * k + 16 > lines.length) {
                    int[] nl = new int[lines.length * 2];
                    System.arraycopy(lines, 0, nl, 0, lines.length);
                    lines = nl;
                }
                int pl = b.read(ps);
                int rl = b.read(rs);
                lines[4 * k] = pl;
                lines[4 * k + 1] = rl;
                lines[4 * k + 2] = (int) cur;
                lines[4 * k + 3] = NORMAL;
                k++;
                cur += 1L << Math.min(rl, 32);
            }
            if (4 * k + 16 > lines.length) {
                int[] nl = new int[lines.length + 16];
                System.arraycopy(lines, 0, nl, 0, lines.length);
                lines = nl;
            }
            lines[4 * k] = b.read(ps);
            lines[4 * k + 1] = 32;
            lines[4 * k + 2] = lo - 1;
            lines[4 * k + 3] = LOWER;
            k++;
            lines[4 * k] = b.read(ps);
            lines[4 * k + 1] = 32;
            lines[4 * k + 2] = hi;
            lines[4 * k + 3] = UPPER;
            k++;
            if (oob) {
                lines[4 * k] = b.read(ps);
                lines[4 * k + 1] = 0;
                lines[4 * k + 2] = 0;
                lines[4 * k + 3] = OOBLINE;
                k++;
            }
            return new Huff(lines, k);
        }

        /// The symbol ID table of a Huffman text region (7.4.3.1.7).
        static Huff symbolIds(Bits b, int numSyms) {
            int[] rl = new int[35 * 4];
            for (int i = 0; i < 35; i++) {
                rl[4 * i] = b.read(4);
                rl[4 * i + 2] = i;
            }
            Huff run = new Huff(rl, 35);
            int[] lens = new int[numSyms];
            int i = 0;
            int prevLen = 0;
            while (i < numSyms) {
                int c = run.decode(b);
                int rep;
                int v;
                if (c < 32) {
                    lens[i++] = c;
                    prevLen = c;
                    continue;
                } else if (c == 32) {
                    rep = 3 + b.read(2);
                    v = prevLen;
                } else if (c == 33) {
                    rep = 3 + b.read(3);
                    v = 0;
                } else {
                    rep = 11 + b.read(7);
                    v = 0;
                }
                for (int k = 0; k < rep && i < numSyms; k++) {
                    lens[i++] = v;
                }
            }
            int[] lines = new int[numSyms * 4];
            for (int k = 0; k < numSyms; k++) {
                lines[4 * k] = lens[k];
                lines[4 * k + 2] = k;
            }
            return new Huff(lines, numSyms);
        }

        int decode(Bits b) {
            int c = 0;
            for (int len = 1; len <= 32; len++) {
                c = (c << 1) | b.bit();
                for (int i = 0; i < n; i++) {
                    if (pref[i] == len && code[i] == c) {
                        switch (kind[i]) {
                            case OOBLINE:
                                return OOB;
                            case LOWER:
                                return low[i] - b.read(range[i]);
                            default:
                                return low[i] + b.read(range[i]);
                        }
                    }
                }
            }
            throw new IllegalStateException("huffman code");
        }
    }
}
