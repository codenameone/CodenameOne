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

/// A pure-Java JPEG 2000 decoder for PDF `JPXDecode` images.
///
/// It reads either a JP2 file (the `jp2h` header boxes and the `jp2c` codestream) or a raw
/// J2K codestream, and decodes every tile into one integer plane per component. The
/// decoder covers what PDF producers emit: all five progression orders, multiple tiles and
/// tile-parts, subsampled components, default and custom precincts, every code-block style
/// switch, the 5/3 and 9/7 wavelets, scalar quantization, the RCT and ICT component
/// transforms and bit depths from 1 to 16. Region of interest shifts and progression order
/// changes (`POC`) are ignored, and High Throughput code-blocks are refused.
///
/// Malformed or truncated data never escapes as an exception: `decode` answers null, or
/// the image as far as it could be decoded.
final class JpxDecoder {
    /// The image width in pixels, from the codestream's `SIZ` marker.
    int width;
    /// The image height in pixels, from the codestream's `SIZ` marker.
    int height;

    private static final int MAX_PIXELS = 25000000;
    private static final RuntimeException EOD = new RuntimeException("jpx: end of data");

    // SIZ
    private int xsiz;
    private int ysiz;
    private int xosiz;
    private int yosiz;
    private int xtsiz;
    private int ytsiz;
    private int xtosiz;
    private int ytosiz;
    private int ntx;
    private int ncomp;
    private int[] prec;
    private boolean[] sgnd;
    private int[] dx;
    private int[] dy;
    private int[][] planes;
    private int[] pw;
    private int[] ph;
    private boolean bad;
    private boolean mctUsed;

    // main header
    private Cod mainCod;
    private Comp[] mainCoc;
    private Quant mainQcd;
    private Quant[] mainQcc;
    private Buf ppm;
    private int ppmPos;
    private Tile[] tiles;

    // JP2 boxes
    private int enumCs = -1;
    private int[][] palette;
    private int[] palDepth;
    private boolean[] palSigned;
    private int[] cmapComp;
    private int[] cmapType;
    private int[] cmapCol;
    private int[] cdefCn;
    private int[] cdefTyp;
    private int[] cdefAsoc;

    // packet reading state
    private byte[] hb;
    private int hpos;
    private int hend;
    private int hcur;
    private int hbits;
    private byte[] bb;
    private int bpos;
    private int bend;
    private boolean sep;
    private CB[] pCb = new CB[64];
    private int[] pLen = new int[64];
    private int[] pN = new int[64];
    private boolean[] pStart = new boolean[64];

    // tier-1 state
    private byte[] sig = new byte[0];
    private byte[] sgn = new byte[0];
    private byte[] vis = new byte[0];
    private byte[] ref = new byte[0];
    private int[] mag = new int[0];
    private final int[] cst = new int[19];
    private final int[] cmps = new int[19];
    private byte[] md;
    private int mp;
    private int me;
    private int ma;
    private int mc;
    private int mct;
    private int rcur;
    private int rbits;

    private static final int[] QE = {0x5601, 0x3401, 0x1801, 0x0AC1, 0x0521, 0x0221, 0x5601, 0x5401,
        0x4801, 0x3801, 0x3001, 0x2401, 0x1C01, 0x1601, 0x5601, 0x5401, 0x5101, 0x4801, 0x3801,
        0x3401, 0x3001, 0x2801, 0x2401, 0x2201, 0x1C01, 0x1801, 0x1601, 0x1401, 0x1201, 0x1101,
        0x0AC1, 0x09C1, 0x08A1, 0x0521, 0x0441, 0x02A1, 0x0221, 0x0141, 0x0111, 0x0085, 0x0049,
        0x0025, 0x0015, 0x0009, 0x0005, 0x0001, 0x5601};
    private static final int[] NMPS = {1, 2, 3, 4, 5, 38, 7, 8, 9, 10, 11, 12, 13, 29, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44, 45, 45, 46};
    private static final int[] NLPS = {1, 6, 9, 12, 29, 33, 6, 14, 14, 14, 17, 18, 20, 21, 14, 14,
        15, 16, 17, 18, 19, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36,
        37, 38, 39, 40, 41, 42, 43, 46};
    private static final int[] SW = {1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final int[] SC_CTX = {13, 12, 11, 10, 9, 10, 11, 12, 13};
    private static final int[] SC_XOR = {1, 1, 1, 1, 0, 0, 0, 0, 0};
    private static final int[][] ZC = new int[4][45];

    static {
        for (int h = 0; h < 3; h++) {
            for (int v = 0; v < 3; v++) {
                for (int d = 0; d < 5; d++) {
                    int i = h * 15 + v * 5 + d;
                    ZC[0][i] = zcLL(h, v, d);
                    ZC[2][i] = ZC[0][i];
                    ZC[1][i] = zcLL(v, h, d);
                    ZC[3][i] = zcHH(h + v, d);
                }
            }
        }
    }

    private static int zcLL(int h, int v, int d) {
        if (h == 2) {
            return 8;
        }
        if (h == 1) {
            return v >= 1 ? 7 : d >= 1 ? 6 : 5;
        }
        if (v == 2) {
            return 4;
        }
        if (v == 1) {
            return 3;
        }
        return d >= 2 ? 2 : d;
    }

    private static int zcHH(int hv, int d) {
        if (d >= 3) {
            return 8;
        }
        if (d == 2) {
            return hv >= 1 ? 7 : 6;
        }
        if (d == 1) {
            return hv >= 2 ? 5 : hv == 1 ? 4 : 3;
        }
        return hv >= 2 ? 2 : hv;
    }

    private static final class Buf {
        byte[] a = new byte[16];
        int n;

        void add(byte[] s, int o, int l) {
            if (l <= 0) {
                return;
            }
            if (n + l > a.length) {
                byte[] b = new byte[Math.max(a.length * 2, n + l)];
                System.arraycopy(a, 0, b, 0, n);
                a = b;
            }
            System.arraycopy(s, o, a, n, l);
            n += l;
        }
    }

    private static final class Comp {
        int levels;
        int xcb;
        int ycb;
        int style;
        boolean rev;
        int[] ppx;
        int[] ppy;
    }

    private static final class Cod {
        int scod;
        int prog;
        int layers;
        int mct;
        Comp comp;
    }

    private static final class Quant {
        int style;
        int guard;
        int[] exps;
        int[] mants;
    }

    private static final class Tile {
        Cod cod;
        Comp[] coc;
        Quant qcd;
        Quant[] qcc;
        Buf data = new Buf();
        Buf hdr;
    }

    private static final class CB {
        int x0;
        int y0;
        int x1;
        int y1;
        boolean included;
        int lblock = 3;
        int zbp;
        int passes;
        Buf data;
        int[] segLen = new int[4];
        int[] segPasses = new int[4];
        int nseg;
    }

    private static final class TagTree {
        int[][] val;
        int[][] low;
        int[] w;
        int levels;
        int w0;

        TagTree(int nx, int ny) {
            int lv = 1;
            int a = nx;
            int b = ny;
            while (a > 1 || b > 1) {
                a = (a + 1) >> 1;
                b = (b + 1) >> 1;
                lv++;
            }
            levels = lv;
            w0 = nx;
            val = new int[lv][];
            low = new int[lv][];
            w = new int[lv];
            a = nx;
            b = ny;
            for (int i = 0; i < lv; i++) {
                w[i] = a;
                int n = a * b;
                val[i] = new int[n];
                low[i] = new int[n];
                for (int j = 0; j < n; j++) {
                    val[i][j] = Integer.MAX_VALUE;
                }
                a = (a + 1) >> 1;
                b = (b + 1) >> 1;
            }
        }
    }

    private static final class Prec {
        CB[] cbs;
        TagTree incl;
        TagTree zbp;
    }

    private static final class Band {
        int type;
        int x0;
        int y0;
        int x1;
        int y1;
        int w;
        int h;
        int cbw;
        int cbh;
        int mb;
        float delta;
        Prec[] precs;
        int[] idata;
        float[] fdata;
    }

    private static final class Res {
        int x0;
        int y0;
        int x1;
        int y1;
        int ppx;
        int ppy;
        int npx;
        int npy;
        int pgx0;
        int pgy0;
        Band[] bands;
    }

    private static final class TC {
        int x0;
        int y0;
        int x1;
        int y1;
        Comp cp;
        Res[] res;
        int[] idata;
        float[] fdata;
    }

    /// Parses a JP2 file or a raw J2K codestream; null when it cannot be decoded.
    static JpxDecoder decode(byte[] data) {
        if (data == null || data.length < 12) {
            return null;
        }
        JpxDecoder d = new JpxDecoder();
        try {
            if (!d.parse(data)) {
                return null;
            }
        } catch (RuntimeException e) {
            if (d.bad || d.planes == null) {
                return null;
            }
        }
        if (d.bad || d.planes == null || d.width <= 0 || d.height <= 0) {
            return null;
        }
        return d;
    }

    private static int u16(byte[] b, int p) {
        return ((b[p] & 0xff) << 8) | (b[p + 1] & 0xff);
    }

    private static long u32(byte[] b, int p) {
        return ((long) (b[p] & 0xff) << 24) | ((b[p + 1] & 0xff) << 16) | ((b[p + 2] & 0xff) << 8)
                | (b[p + 3] & 0xff);
    }

    private static int i32(byte[] b, int p) {
        long v = u32(b, p);
        if (v > Integer.MAX_VALUE) {
            throw EOD;
        }
        return (int) v;
    }

    private boolean parse(byte[] b) {
        if ((b[0] & 0xff) == 0xFF && (b[1] & 0xff) == 0x4F) {
            return codestream(b, 0, b.length);
        }
        int p = 0;
        int n = b.length;
        while (p + 8 <= n) {
            long len = u32(b, p);
            int type = (int) u32(b, p + 4);
            int hdr = 8;
            if (len == 1) {
                if (p + 16 > n) {
                    break;
                }
                len = (u32(b, p + 8) << 32) | u32(b, p + 12);
                hdr = 16;
            } else if (len == 0) {
                len = n - p;
            }
            if (len < hdr) {
                break;
            }
            long el = p + len;
            int end = el > n ? n : (int) el;
            if (type == 0x6A703268) {
                boxes(b, p + hdr, end);
            } else if (type == 0x6A703263) {
                return codestream(b, p + hdr, end);
            }
            p = end;
        }
        return false;
    }

    private void boxes(byte[] b, int p, int n) {
        while (p + 8 <= n) {
            long len = u32(b, p);
            int type = (int) u32(b, p + 4);
            int hdr = 8;
            if (len == 1) {
                if (p + 16 > n) {
                    return;
                }
                len = (u32(b, p + 8) << 32) | u32(b, p + 12);
                hdr = 16;
            } else if (len == 0) {
                len = n - p;
            }
            if (len < hdr) {
                return;
            }
            long el = p + len;
            int end = el > n ? n : (int) el;
            int s = p + hdr;
            try {
                if (type == 0x636F6C72) {
                    if (s + 7 <= end && b[s] == 1 && enumCs < 0) {
                        enumCs = (int) u32(b, s + 3);
                    }
                } else if (type == 0x70636C72) {
                    parsePclr(b, s, end);
                } else if (type == 0x636D6170) {
                    int cnt = (end - s) / 4;
                    cmapComp = new int[cnt];
                    cmapType = new int[cnt];
                    cmapCol = new int[cnt];
                    for (int i = 0; i < cnt; i++) {
                        cmapComp[i] = u16(b, s + 4 * i);
                        cmapType[i] = b[s + 4 * i + 2] & 0xff;
                        cmapCol[i] = b[s + 4 * i + 3] & 0xff;
                    }
                } else if (type == 0x63646566) {
                    int cnt = Math.min(u16(b, s), (end - s - 2) / 6);
                    cdefCn = new int[cnt];
                    cdefTyp = new int[cnt];
                    cdefAsoc = new int[cnt];
                    for (int i = 0; i < cnt; i++) {
                        cdefCn[i] = u16(b, s + 2 + 6 * i);
                        cdefTyp[i] = u16(b, s + 4 + 6 * i);
                        cdefAsoc[i] = u16(b, s + 6 + 6 * i);
                    }
                }
            } catch (RuntimeException e) {
                // a damaged header box is ignored; the codestream still decodes
            }
            p = end;
        }
    }

    private void parsePclr(byte[] b, int s, int end) {
        int ne = u16(b, s);
        int npc = b[s + 2] & 0xff;
        if (ne < 1 || ne > 1024 || npc < 1) {
            return;
        }
        int[] depth = new int[npc];
        boolean[] signed = new boolean[npc];
        int p = s + 3;
        for (int i = 0; i < npc; i++) {
            depth[i] = (b[p] & 0x7f) + 1;
            signed[i] = (b[p] & 0x80) != 0;
            if (depth[i] > 16) {
                return;
            }
            p++;
        }
        int[][] pal = new int[npc][ne];
        for (int e = 0; e < ne; e++) {
            for (int i = 0; i < npc; i++) {
                int nb = (depth[i] + 7) >> 3;
                int v = 0;
                for (int k = 0; k < nb; k++) {
                    v = (v << 8) | (b[p++] & 0xff);
                }
                if (signed[i] && (v & (1 << (depth[i] - 1))) != 0) {
                    v -= 1 << depth[i];
                }
                pal[i][e] = v;
            }
        }
        if (p > end) {
            return;
        }
        palette = pal;
        palDepth = depth;
        palSigned = signed;
    }

    private boolean codestream(byte[] b, int off, int end) {
        if (end - off < 4 || u16(b, off) != 0xFF4F) {
            return false;
        }
        int p = off + 2;
        boolean inTiles = false;
        while (p + 2 <= end) {
            int m = u16(b, p);
            if (m == 0xFFD9) {
                break;
            }
            if (m == 0xFF90) {
                if (planes == null || mainCod == null || mainQcd == null) {
                    return false;
                }
                inTiles = true;
                int np;
                try {
                    np = tilePart(b, p, end);
                } catch (RuntimeException e) {
                    break;
                }
                if (np <= p) {
                    break;
                }
                p = np;
                continue;
            }
            if (inTiles || (m & 0xFF00) != 0xFF00 || p + 4 > end) {
                break;
            }
            int len = u16(b, p + 2);
            int s = p + 4;
            int e = p + 2 + len;
            if (len < 2 || e > end) {
                return false;
            }
            if (m == 0xFF51) {
                if (!siz(b, s, e)) {
                    bad = true;
                    return false;
                }
            } else if (planes == null) {
                if (m != 0xFF64) {
                    return false;
                }
            } else if (m == 0xFF52) {
                mainCod = parseCod(b, s);
            } else if (m == 0xFF53) {
                int c = ncomp < 257 ? b[s] & 0xff : u16(b, s);
                int q = ncomp < 257 ? s + 1 : s + 2;
                if (c < ncomp) {
                    mainCoc[c] = parseSpcod(b, q + 1, (b[q] & 1) != 0);
                }
            } else if (m == 0xFF5C) {
                mainQcd = parseQuant(b, s, e);
            } else if (m == 0xFF5D) {
                int c = ncomp < 257 ? b[s] & 0xff : u16(b, s);
                int q = ncomp < 257 ? s + 1 : s + 2;
                if (c < ncomp) {
                    mainQcc[c] = parseQuant(b, q, e);
                }
            } else if (m == 0xFF60) {
                if (ppm == null) {
                    ppm = new Buf();
                }
                ppm.add(b, s + 1, e - s - 1);
            }
            p = e;
        }
        if (planes == null || tiles == null) {
            return false;
        }
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i] != null) {
                try {
                    decodeTile(tiles[i], i);
                } catch (RuntimeException e) {
                    if (bad) {
                        return false;
                    }
                    // keep the tiles that did decode
                }
            }
        }
        return true;
    }

    private boolean siz(byte[] b, int s, int e) {
        if (e - s < 38) {
            return false;
        }
        xsiz = i32(b, s + 2);
        ysiz = i32(b, s + 6);
        xosiz = i32(b, s + 10);
        yosiz = i32(b, s + 14);
        xtsiz = i32(b, s + 18);
        ytsiz = i32(b, s + 22);
        xtosiz = i32(b, s + 26);
        ytosiz = i32(b, s + 30);
        ncomp = u16(b, s + 34);
        width = xsiz - xosiz;
        height = ysiz - yosiz;
        if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS || ncomp < 1
                || ncomp > 256 || e - s < 36 + 3 * ncomp || xtsiz <= 0 || ytsiz <= 0
                || xtosiz > xosiz || ytosiz > yosiz || (long) xtosiz + xtsiz <= xosiz
                || (long) ytosiz + ytsiz <= yosiz) {
            return false;
        }
        long tx = ((long) xsiz - xtosiz + xtsiz - 1) / xtsiz;
        long ty = ((long) ysiz - ytosiz + ytsiz - 1) / ytsiz;
        if (tx * ty > 65535) {
            return false;
        }
        ntx = (int) tx;
        int nty = (int) ty;
        prec = new int[ncomp];
        sgnd = new boolean[ncomp];
        dx = new int[ncomp];
        dy = new int[ncomp];
        pw = new int[ncomp];
        ph = new int[ncomp];
        long total = 0;
        for (int c = 0; c < ncomp; c++) {
            int ss = b[s + 36 + 3 * c] & 0xff;
            prec[c] = (ss & 0x7f) + 1;
            sgnd[c] = (ss & 0x80) != 0;
            dx[c] = b[s + 37 + 3 * c] & 0xff;
            dy[c] = b[s + 38 + 3 * c] & 0xff;
            if (prec[c] > 16 || dx[c] < 1 || dy[c] < 1) {
                return false;
            }
            pw[c] = ceilDiv(xsiz, dx[c]) - ceilDiv(xosiz, dx[c]);
            ph[c] = ceilDiv(ysiz, dy[c]) - ceilDiv(yosiz, dy[c]);
            total += (long) pw[c] * ph[c];
        }
        if (total > 4L * MAX_PIXELS) {
            return false;
        }
        mainCoc = new Comp[ncomp];
        mainQcc = new Quant[ncomp];
        tiles = new Tile[ntx * nty];
        planes = new int[ncomp][];
        for (int c = 0; c < ncomp; c++) {
            planes[c] = new int[pw[c] * ph[c]];
            if (!sgnd[c]) {
                int mid = 1 << (prec[c] - 1);
                int[] pl = planes[c];
                for (int i = 0; i < pl.length; i++) {
                    pl[i] = mid;
                }
            }
        }
        return true;
    }

    private static int ceilDiv(int a, int b) {
        return (int) (((long) a + b - 1) / b);
    }

    private static int ceilShift(int v, int s) {
        return (int) (((long) v + (1L << s) - 1) >> s);
    }

    private Cod parseCod(byte[] b, int s) {
        Cod c = new Cod();
        c.scod = b[s] & 0xff;
        c.prog = b[s + 1] & 0xff;
        c.layers = u16(b, s + 2);
        c.mct = b[s + 4] & 0xff;
        c.comp = parseSpcod(b, s + 5, (c.scod & 1) != 0);
        if (c.layers < 1 || c.prog > 4) {
            bad = true;
            throw EOD;
        }
        return c;
    }

    private Comp parseSpcod(byte[] b, int p, boolean custom) {
        Comp c = new Comp();
        c.levels = b[p] & 0xff;
        c.xcb = (b[p + 1] & 0xf) + 2;
        c.ycb = (b[p + 2] & 0xf) + 2;
        c.style = b[p + 3] & 0xff;
        c.rev = (b[p + 4] & 0xff) == 1;
        if (c.levels > 32 || c.xcb > 10 || c.ycb > 10 || c.xcb + c.ycb > 12 || (c.style & 0xC0) != 0) {
            bad = true;
            throw EOD;
        }
        c.ppx = new int[c.levels + 1];
        c.ppy = new int[c.levels + 1];
        for (int r = 0; r <= c.levels; r++) {
            if (custom) {
                int v = b[p + 5 + r] & 0xff;
                c.ppx[r] = v & 0xf;
                c.ppy[r] = v >> 4;
            } else {
                c.ppx[r] = 15;
                c.ppy[r] = 15;
            }
        }
        return c;
    }

    private static Quant parseQuant(byte[] b, int s, int e) {
        Quant q = new Quant();
        int sq = b[s] & 0xff;
        q.style = sq & 0x1f;
        q.guard = sq >> 5;
        if (q.style == 0) {
            int n = Math.max(1, e - s - 1);
            q.exps = new int[n];
            q.mants = new int[n];
            for (int i = 0; i < n && s + 1 + i < e; i++) {
                q.exps[i] = (b[s + 1 + i] & 0xff) >> 3;
            }
        } else {
            int n = Math.max(1, (e - s - 1) / 2);
            q.exps = new int[n];
            q.mants = new int[n];
            for (int i = 0; i < n && s + 2 + 2 * i < e; i++) {
                int v = u16(b, s + 1 + 2 * i);
                q.exps[i] = v >> 11;
                q.mants[i] = v & 0x7ff;
            }
        }
        return q;
    }

    private int tilePart(byte[] b, int p, int end) {
        int lsot = u16(b, p + 2);
        int isot = u16(b, p + 4);
        long psot = u32(b, p + 6);
        if (lsot != 10 || isot >= tiles.length) {
            return -1;
        }
        Tile t = tiles[isot];
        if (t == null) {
            t = new Tile();
            t.coc = new Comp[ncomp];
            t.qcc = new Quant[ncomp];
            tiles[isot] = t;
        }
        int q = p + 2 + lsot;
        while (q + 4 <= end) {
            int m = u16(b, q);
            if (m == 0xFF93) {
                q += 2;
                break;
            }
            int len = u16(b, q + 2);
            int s = q + 4;
            int e = q + 2 + len;
            if (len < 2 || e > end) {
                return -1;
            }
            if (m == 0xFF52) {
                t.cod = parseCod(b, s);
            } else if (m == 0xFF53) {
                int c = ncomp < 257 ? b[s] & 0xff : u16(b, s);
                int qq = ncomp < 257 ? s + 1 : s + 2;
                if (c < ncomp) {
                    t.coc[c] = parseSpcod(b, qq + 1, (b[qq] & 1) != 0);
                }
            } else if (m == 0xFF5C) {
                t.qcd = parseQuant(b, s, e);
            } else if (m == 0xFF5D) {
                int c = ncomp < 257 ? b[s] & 0xff : u16(b, s);
                int qq = ncomp < 257 ? s + 1 : s + 2;
                if (c < ncomp) {
                    t.qcc[c] = parseQuant(b, qq, e);
                }
            } else if (m == 0xFF61) {
                if (t.hdr == null) {
                    t.hdr = new Buf();
                }
                t.hdr.add(b, s + 1, e - s - 1);
            }
            q = e;
        }
        int dataEnd;
        if (psot == 0) {
            dataEnd = end;
            if (end - 2 >= q && u16(b, end - 2) == 0xFFD9) {
                dataEnd = end - 2;
            }
        } else {
            long de = p + psot;
            dataEnd = de > end ? end : (int) de;
        }
        if (dataEnd > q) {
            t.data.add(b, q, dataEnd - q);
        }
        if (ppm != null && ppmPos + 4 <= ppm.n) {
            long n = u32(ppm.a, ppmPos);
            int nn = (int) Math.min(n, (long) (ppm.n - ppmPos - 4));
            if (t.hdr == null) {
                t.hdr = new Buf();
            }
            t.hdr.add(ppm.a, ppmPos + 4, nn);
            ppmPos += 4 + nn;
        }
        return dataEnd;
    }

    // ------------------------------------------------------------------ tile structure

    private static float pow2(int e) {
        float r = 1;
        if (e >= 0) {
            while (e-- > 0) {
                r *= 2;
            }
        } else {
            while (e++ < 0) {
                r *= 0.5f;
            }
        }
        return r;
    }

    private TC buildTc(int c, int tx0, int ty0, int tx1, int ty1, Comp cp, Quant qp) {
        TC tc = new TC();
        tc.cp = cp;
        tc.x0 = ceilDiv(tx0, dx[c]);
        tc.y0 = ceilDiv(ty0, dy[c]);
        tc.x1 = ceilDiv(tx1, dx[c]);
        tc.y1 = ceilDiv(ty1, dy[c]);
        int nl = cp.levels;
        tc.res = new Res[nl + 1];
        for (int r = 0; r <= nl; r++) {
            Res rr = new Res();
            int s = nl - r;
            rr.x0 = ceilShift(tc.x0, s);
            rr.y0 = ceilShift(tc.y0, s);
            rr.x1 = ceilShift(tc.x1, s);
            rr.y1 = ceilShift(tc.y1, s);
            rr.ppx = cp.ppx[r];
            rr.ppy = cp.ppy[r];
            if (r > 0) {
                if (rr.ppx == 0) {
                    rr.ppx = 1;
                }
                if (rr.ppy == 0) {
                    rr.ppy = 1;
                }
            }
            rr.npx = rr.x1 > rr.x0 ? ceilShift(rr.x1, rr.ppx) - (rr.x0 >> rr.ppx) : 0;
            rr.npy = rr.y1 > rr.y0 ? ceilShift(rr.y1, rr.ppy) - (rr.y0 >> rr.ppy) : 0;
            if ((long) rr.npx * rr.npy > 4000000L) {
                throw EOD;
            }
            rr.pgx0 = rr.x0 >> rr.ppx;
            rr.pgy0 = rr.y0 >> rr.ppy;
            int nb = r == 0 ? 1 : 3;
            rr.bands = new Band[nb];
            int pb = r == 0 ? 0 : 1;
            for (int bi = 0; bi < nb; bi++) {
                Band bd = new Band();
                bd.type = r == 0 ? 0 : bi + 1;
                if (r == 0) {
                    bd.x0 = rr.x0;
                    bd.y0 = rr.y0;
                    bd.x1 = rr.x1;
                    bd.y1 = rr.y1;
                } else {
                    int xo = (bd.type & 1);
                    int yo = bd.type >> 1;
                    int lev = nl - r + 1;
                    bd.x0 = ceilShift(tc.x0 - (xo << (lev - 1)), lev);
                    bd.y0 = ceilShift(tc.y0 - (yo << (lev - 1)), lev);
                    bd.x1 = ceilShift(tc.x1 - (xo << (lev - 1)), lev);
                    bd.y1 = ceilShift(tc.y1 - (yo << (lev - 1)), lev);
                }
                bd.w = Math.max(0, bd.x1 - bd.x0);
                bd.h = Math.max(0, bd.y1 - bd.y0);
                bd.cbw = Math.min(cp.xcb, rr.ppx - pb);
                bd.cbh = Math.min(cp.ycb, rr.ppy - pb);
                int idx = r == 0 ? 0 : 3 * (r - 1) + bd.type;
                int eps;
                int mant;
                if (qp.style == 1) {
                    eps = qp.exps[0] - (r == 0 ? 0 : r - 1);
                    mant = qp.mants[0];
                } else {
                    int k = Math.min(idx, qp.exps.length - 1);
                    eps = qp.exps[k];
                    mant = qp.mants[k];
                }
                bd.mb = qp.guard + eps - 1;
                int gain = bd.type == 0 ? 0 : bd.type == 3 ? 2 : 1;
                bd.delta = pow2(prec[c] + gain - eps) * (1 + mant / 2048f);
                if (cp.rev) {
                    bd.idata = new int[bd.w * bd.h];
                } else {
                    bd.fdata = new float[bd.w * bd.h];
                }
                int np = rr.npx * rr.npy;
                bd.precs = new Prec[np];
                int ps = rr.ppx - pb;
                int pt = rr.ppy - pb;
                for (int ky = 0; ky < rr.npy; ky++) {
                    for (int kx = 0; kx < rr.npx; kx++) {
                        Prec pr = new Prec();
                        int px0 = (rr.pgx0 + kx) << ps;
                        int py0 = (rr.pgy0 + ky) << pt;
                        int ix0 = Math.max(px0, bd.x0);
                        int iy0 = Math.max(py0, bd.y0);
                        int ix1 = Math.min(px0 + (1 << ps), bd.x1);
                        int iy1 = Math.min(py0 + (1 << pt), bd.y1);
                        if (ix1 > ix0 && iy1 > iy0) {
                            int cx0 = ix0 >> bd.cbw;
                            int cy0 = iy0 >> bd.cbh;
                            int nx = ceilShift(ix1, bd.cbw) - cx0;
                            int ny = ceilShift(iy1, bd.cbh) - cy0;
                            pr.cbs = new CB[nx * ny];
                            for (int j = 0; j < ny; j++) {
                                for (int i = 0; i < nx; i++) {
                                    CB cb = new CB();
                                    cb.x0 = Math.max(ix0, (cx0 + i) << bd.cbw);
                                    cb.y0 = Math.max(iy0, (cy0 + j) << bd.cbh);
                                    cb.x1 = Math.min(ix1, (cx0 + i + 1) << bd.cbw);
                                    cb.y1 = Math.min(iy1, (cy0 + j + 1) << bd.cbh);
                                    pr.cbs[j * nx + i] = cb;
                                }
                            }
                            pr.incl = new TagTree(nx, ny);
                            pr.zbp = new TagTree(nx, ny);
                        } else {
                            pr.cbs = new CB[0];
                        }
                        bd.precs[ky * rr.npx + kx] = pr;
                    }
                }
                rr.bands[bi] = bd;
            }
            tc.res[r] = rr;
        }
        return tc;
    }

    private void decodeTile(Tile t, int ti) {
        Cod cod = t.cod != null ? t.cod : mainCod;
        int p = ti % ntx;
        int q = ti / ntx;
        int tx0 = (int) Math.max((long) xtosiz + (long) p * xtsiz, xosiz);
        int ty0 = (int) Math.max((long) ytosiz + (long) q * ytsiz, yosiz);
        int tx1 = (int) Math.min((long) xtosiz + (long) (p + 1) * xtsiz, xsiz);
        int ty1 = (int) Math.min((long) ytosiz + (long) (q + 1) * ytsiz, ysiz);
        if (tx1 <= tx0 || ty1 <= ty0) {
            return;
        }
        TC[] tcs = new TC[ncomp];
        int maxR = 0;
        for (int c = 0; c < ncomp; c++) {
            Comp cp = t.coc[c];
            if (cp == null) {
                cp = t.cod != null ? t.cod.comp : mainCoc[c] != null ? mainCoc[c] : mainCod.comp;
            }
            Quant qp = t.qcc[c];
            if (qp == null) {
                qp = t.qcd != null ? t.qcd : mainQcc[c] != null ? mainQcc[c] : mainQcd;
            }
            tcs[c] = buildTc(c, tx0, ty0, tx1, ty1, cp, qp);
            maxR = Math.max(maxR, cp.levels);
        }
        bb = t.data.a;
        bpos = 0;
        bend = t.data.n;
        sep = t.hdr != null && t.hdr.n > 0;
        if (sep) {
            hb = t.hdr.a;
            hpos = 0;
            hend = t.hdr.n;
        } else {
            hb = bb;
        }
        boolean sop = (cod.scod & 2) != 0;
        boolean eph = (cod.scod & 4) != 0;
        int nl = cod.layers;
        try {
            if (cod.prog == 0 || cod.prog == 1) {
                int outer = cod.prog == 0 ? nl : maxR + 1;
                int inner = cod.prog == 0 ? maxR + 1 : nl;
                for (int o = 0; o < outer; o++) {
                    for (int i = 0; i < inner; i++) {
                        int l = cod.prog == 0 ? o : i;
                        int r = cod.prog == 0 ? i : o;
                        for (int c = 0; c < ncomp; c++) {
                            TC tc = tcs[c];
                            if (r > tc.cp.levels) {
                                continue;
                            }
                            Res rr = tc.res[r];
                            int np = rr.npx * rr.npy;
                            for (int k = 0; k < np; k++) {
                                packet(rr, k, l, tc.cp.style, sop, eph);
                            }
                        }
                    }
                }
            } else {
                positionOrder(tcs, cod.prog, nl, sop, eph, tx0, ty0);
            }
        } catch (RuntimeException e) {
            if (bad) {
                throw e;
            }
            // truncated stream: decode what arrived
        }
        for (int c = 0; c < ncomp; c++) {
            TC tc = tcs[c];
            for (int r = 0; r < tc.res.length; r++) {
                Res rr = tc.res[r];
                for (int bi = 0; bi < rr.bands.length; bi++) {
                    Band bd = rr.bands[bi];
                    for (int k = 0; k < bd.precs.length; k++) {
                        CB[] cbs = bd.precs[k].cbs;
                        for (int j = 0; j < cbs.length; j++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                            try {
                                decodeCb(cbs[j], bd, tc.cp.style, tc.cp.rev);
                            } catch (RuntimeException e) {
                                // a damaged code-block stays as decoded so far
                            }
                        }
                    }
                }
            }
            idwt(tc);
        }
        storeTile(tcs, cod.mct == 1);
    }

    private void positionOrder(TC[] tcs, int prog, int nl, boolean sop, boolean eph, int tx0, int ty0) {
        int n = 0;
        for (int c = 0; c < ncomp; c++) {
            for (int r = 0; r <= tcs[c].cp.levels; r++) {
                n += tcs[c].res[r].npx * tcs[c].res[r].npy;
            }
        }
        long[][] keys = new long[n][];
        int i = 0;
        for (int c = 0; c < ncomp; c++) {
            TC tc = tcs[c];
            int lv = tc.cp.levels;
            for (int r = 0; r <= lv; r++) {
                Res rr = tc.res[r];
                for (int k = 0; k < rr.npx * rr.npy; k++) {
                    int kx = k % rr.npx;
                    int ky = k / rr.npx;
                    // a precinct clipped by the tile's edge is reached at the tile's own
                    // origin, whatever its resolution rounds that origin to
                    long px = (long) (rr.pgx0 + kx) << rr.ppx;
                    long py = (long) (rr.pgy0 + ky) << rr.ppy;
                    long rx = px <= rr.x0 ? tx0 : (px << (lv - r)) * dx[c];
                    long ry = py <= rr.y0 ? ty0 : (py << (lv - r)) * dy[c];
                    long[] key;
                    if (prog == 2) {
                        key = new long[] {r, ry, rx, c, k};
                    } else if (prog == 3) {
                        key = new long[] {ry, rx, c, r, k};
                    } else {
                        key = new long[] {c, ry, rx, r, k};
                    }
                    keys[i++] = key;
                }
            }
        }
        sortKeys(keys);
        for (i = 0; i < n; i++) {
            long[] key = keys[i];
            int c;
            int r;
            if (prog == 2) {
                r = (int) key[0];
                c = (int) key[3];
            } else if (prog == 3) {
                c = (int) key[2];
                r = (int) key[3];
            } else {
                c = (int) key[0];
                r = (int) key[3];
            }
            int k = (int) key[4];
            for (int l = 0; l < nl; l++) {
                packet(tcs[c].res[r], k, l, tcs[c].cp.style, sop, eph);
            }
        }
    }

    private static int cmpKey(long[] a, long[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return a[i] < b[i] ? -1 : 1;
            }
        }
        return 0;
    }

    private static void sortKeys(long[][] a) {
        int n = a.length;
        long[][] tmp = new long[n][];
        for (int w = 1; w < n; w <<= 1) {
            for (int lo = 0; lo < n - w; lo += w << 1) {
                int mid = lo + w;
                int hi = Math.min(lo + (w << 1), n);
                int i = lo;
                int j = mid;
                int k = lo;
                while (i < mid && j < hi) {
                    tmp[k++] = cmpKey(a[j], a[i]) < 0 ? a[j++] : a[i++];
                }
                while (i < mid) {
                    tmp[k++] = a[i++];
                }
                while (j < hi) {
                    tmp[k++] = a[j++];
                }
                System.arraycopy(tmp, lo, a, lo, hi - lo);
            }
        }
    }

    // ------------------------------------------------------------------ packets

    private int bit() {
        if (hbits == 0) {
            if (hpos >= hend) {
                throw EOD;
            }
            boolean ff = hcur == 0xFF;
            hcur = hb[hpos++] & 0xff;
            hbits = ff ? 7 : 8;
        }
        hbits--;
        return (hcur >> hbits) & 1;
    }

    private int bits(int n) {
        int v = 0;
        while (n-- > 0) {
            v = (v << 1) | bit();
        }
        return v;
    }

    private boolean tagDecode(TagTree tt, int leaf, int thresh) {
        int x = leaf % tt.w0;
        int y = leaf / tt.w0;
        int lowv = 0;
        for (int lv = tt.levels - 1; lv >= 0; lv--) {
            int i = (x >> lv) + (y >> lv) * tt.w[lv];
            int[] low = tt.low[lv];
            int[] val = tt.val[lv];
            if (low[i] < lowv) {
                low[i] = lowv;
            } else {
                lowv = low[i];
            }
            while (lowv < thresh && lowv < val[i]) {
                if (bit() == 1) {
                    val[i] = lowv;
                } else {
                    lowv++;
                }
            }
            low[i] = lowv;
        }
        return tt.val[0][leaf] < thresh;
    }

    private static boolean segStart(int p, int style) {
        if (p == 0 || (style & 4) != 0) {
            return true;
        }
        return (style & 1) != 0 && p >= 10 && (p - 10) % 3 != 1;
    }

    private static int segEnd(int p, int style) {
        if ((style & 4) != 0) {
            return p;
        }
        if ((style & 1) != 0) {
            if (p < 10) {
                return 9;
            }
            return (p - 10) % 3 == 0 ? p + 1 : p;
        }
        return Integer.MAX_VALUE;
    }

    private static int floorLog2(int n) {
        int r = 0;
        while (n > 1) {
            n >>= 1;
            r++;
        }
        return r;
    }

    private void packet(Res rr, int k, int l, int style, boolean sop, boolean eph) {
        if (sop && bpos + 6 <= bend && bb[bpos] == (byte) 0xFF && bb[bpos + 1] == (byte) 0x91) {
            bpos += 6;
        }
        if (!sep) {
            hpos = bpos;
            hend = bend;
        }
        hcur = 0;
        hbits = 0;
        int np = 0;
        if (bit() != 0) {
            for (int bi = 0; bi < rr.bands.length; bi++) {
                Prec pr = rr.bands[bi].precs[k];
                CB[] cbs = pr.cbs;
                for (int i = 0; i < cbs.length; i++) {
                    CB cb = cbs[i];
                    boolean inc;
                    if (!cb.included) {
                        inc = tagDecode(pr.incl, i, l + 1);
                        if (inc) {
                            tagDecode(pr.zbp, i, Integer.MAX_VALUE);
                            cb.zbp = pr.zbp.val[0][i];
                            cb.included = true;
                        }
                    } else {
                        inc = bit() == 1;
                    }
                    if (!inc) {
                        continue;
                    }
                    int n;
                    if (bit() == 0) {
                        n = 1;
                    } else if (bit() == 0) {
                        n = 2;
                    } else {
                        n = bits(2);
                        if (n < 3) {
                            n += 3;
                        } else {
                            n = bits(5);
                            n = n < 31 ? n + 6 : 37 + bits(7);
                        }
                    }
                    while (bit() == 1) {
                        cb.lblock++;
                        if (cb.lblock > 31) {
                            throw EOD;
                        }
                    }
                    int pp = cb.passes;
                    int left = n;
                    while (left > 0) {
                        int se = segEnd(pp, style);
                        int cnt = (int) Math.min((long) left, (long) se - pp + 1);
                        int nbits = cb.lblock + floorLog2(cnt);
                        if (nbits > 31) {
                            throw EOD;
                        }
                        int len = bits(nbits);
                        if (np == pCb.length) {
                            growPending();
                        }
                        pCb[np] = cb;
                        pLen[np] = len;
                        pN[np] = cnt;
                        pStart[np] = segStart(pp, style);
                        np++;
                        pp += cnt;
                        left -= cnt;
                    }
                }
            }
        }
        if (hcur == 0xFF && hbits == 0) {
            hpos++;
        } else if (hcur == 0xFF) {
            hpos++;
        }
        hbits = 0;
        if (eph && hpos + 2 <= hend && hb[hpos] == (byte) 0xFF && hb[hpos + 1] == (byte) 0x92) {
            hpos += 2;
        }
        if (!sep) {
            bpos = hpos;
        }
        for (int i = 0; i < np; i++) {
            int len = pLen[i];
            if (len > bend - bpos) {
                throw EOD;
            }
            CB cb = pCb[i];
            if (cb.data == null) {
                cb.data = new Buf();
            }
            cb.data.add(bb, bpos, len);
            bpos += len;
            if (pStart[i] || cb.nseg == 0) {
                if (cb.nseg == cb.segLen.length) {
                    int[] a = new int[cb.nseg * 2];
                    System.arraycopy(cb.segLen, 0, a, 0, cb.nseg);
                    cb.segLen = a;
                    a = new int[cb.nseg * 2];
                    System.arraycopy(cb.segPasses, 0, a, 0, cb.nseg);
                    cb.segPasses = a;
                }
                cb.segLen[cb.nseg] = len;
                cb.segPasses[cb.nseg] = pN[i];
                cb.nseg++;
            } else {
                cb.segLen[cb.nseg - 1] += len;
                cb.segPasses[cb.nseg - 1] += pN[i];
            }
            cb.passes += pN[i];
        }
    }

    private void growPending() {
        int n = pCb.length * 2;
        CB[] a = new CB[n];
        System.arraycopy(pCb, 0, a, 0, pCb.length);
        pCb = a;
        int[] b = new int[n];
        System.arraycopy(pLen, 0, b, 0, pLen.length);
        pLen = b;
        b = new int[n];
        System.arraycopy(pN, 0, b, 0, pN.length);
        pN = b;
        boolean[] s = new boolean[n];
        System.arraycopy(pStart, 0, s, 0, pStart.length);
        pStart = s;
    }

    // ------------------------------------------------------------------ MQ and raw decoding

    private void resetContexts() {
        for (int i = 0; i < 19; i++) {
            cst[i] = 0;
            cmps[i] = 0;
        }
        cst[0] = 4;
        cst[17] = 3;
        cst[18] = 46;
    }

    private void mqInit(byte[] d, int s, int e) {
        md = d;
        mp = s;
        me = e;
        int b = mp < me ? d[mp] & 0xff : 0xFF;
        mc = b << 16;
        mqByteIn();
        mc <<= 7;
        mct -= 7;
        ma = 0x8000;
    }

    private void mqByteIn() {
        int b = mp < me ? md[mp] & 0xff : 0xFF;
        if (b == 0xFF) {
            int b1 = mp + 1 < me ? md[mp + 1] & 0xff : 0xFF;
            if (b1 > 0x8F) {
                mc += 0xFF00;
                mct = 8;
            } else {
                mp++;
                mc += b1 << 9;
                mct = 7;
            }
        } else {
            mp++;
            int b1 = mp < me ? md[mp] & 0xff : 0xFF;
            mc += b1 << 8;
            mct = 8;
        }
    }

    private int mq(int cx) {
        int st = cst[cx];
        int qe = QE[st];
        int d;
        ma -= qe;
        if ((mc >>> 16) < qe) {
            if (ma < qe) {
                ma = qe;
                d = cmps[cx];
                cst[cx] = NMPS[st];
            } else {
                ma = qe;
                d = 1 - cmps[cx];
                if (SW[st] != 0) {
                    cmps[cx] = d;
                }
                cst[cx] = NLPS[st];
            }
        } else {
            mc -= qe << 16;
            if ((ma & 0x8000) != 0) {
                return cmps[cx];
            }
            if (ma < qe) {
                d = 1 - cmps[cx];
                if (SW[st] != 0) {
                    cmps[cx] = d;
                }
                cst[cx] = NLPS[st];
            } else {
                d = cmps[cx];
                cst[cx] = NMPS[st];
            }
        }
        do {
            if (mct == 0) {
                mqByteIn();
            }
            ma <<= 1;
            mc <<= 1;
            mct--;
        } while ((ma & 0x8000) == 0);
        return d;
    }

    private void rawInit(byte[] d, int s, int e) {
        md = d;
        mp = s;
        me = e;
        rbits = 0;
        rcur = 0;
    }

    private int rawBit() {
        if (rbits == 0) {
            boolean ff = rcur == 0xFF;
            rcur = mp < me ? md[mp++] & 0xff : 0xFF;
            rbits = ff ? 7 : 8;
        }
        rbits--;
        return (rcur >> rbits) & 1;
    }

    // ------------------------------------------------------------------ tier-1

    private void decodeCb(CB cb, Band bd, int style, boolean rev) {
        int w = cb.x1 - cb.x0;
        int h = cb.y1 - cb.y0;
        if (w <= 0 || h <= 0 || cb.nseg == 0 || cb.data == null) {
            return;
        }
        int planesN = bd.mb - cb.zbp;
        if (planesN <= 0) {
            return;
        }
        if (planesN > 30) {
            planesN = 30;
        }
        int stride = w + 2;
        int size = stride * (h + 2);
        if (sig.length < size) {
            sig = new byte[size];
            sgn = new byte[size];
            vis = new byte[size];
            ref = new byte[size];
            mag = new int[size];
        } else {
            for (int i = 0; i < size; i++) {
                sig[i] = 0;
                sgn[i] = 0;
                vis[i] = 0;
                ref[i] = 0;
                mag[i] = 0;
            }
        }
        resetContexts();
        int[] lut = ZC[bd.type];
        boolean vc = (style & 8) != 0;
        int maxPasses = 3 * planesN - 2;
        int pass = 0;
        int off = 0;
        byte[] data = cb.data.a;
        int dlen = cb.data.n;
        for (int s = 0; s < cb.nseg && pass < maxPasses; s++) {
            int len = Math.min(cb.segLen[s], dlen - off);
            if (len < 0) {
                break;
            }
            boolean raw = (style & 1) != 0 && pass >= 10 && (pass - 10) % 3 != 2;
            if (raw) {
                rawInit(data, off, off + len);
            } else {
                mqInit(data, off, off + len);
            }
            int np = cb.segPasses[s];
            for (int j = 0; j < np && pass < maxPasses; j++, pass++) {
                int plane = planesN - 1 - (pass + 2) / 3;
                int type = pass == 0 ? 2 : (pass - 1) % 3;
                if (type == 0) {
                    sigPass(w, h, stride, plane, lut, vc, raw);
                } else if (type == 1) {
                    refPass(w, h, stride, plane, vc, raw);
                } else {
                    cleanPass(w, h, stride, plane, lut, vc);
                    if ((style & 32) != 0) {
                        mq(18);
                        mq(18);
                        mq(18);
                        mq(18);
                    }
                }
                if ((style & 2) != 0) {
                    resetContexts();
                }
            }
            off += len;
        }
        int bx = cb.x0 - bd.x0;
        int by = cb.y0 - bd.y0;
        if (rev) {
            int[] dst = bd.idata;
            for (int y = 0; y < h; y++) {
                int i = (y + 1) * stride + 1;
                int o = (by + y) * bd.w + bx;
                for (int x = 0; x < w; x++, i++, o++) {
                    int v = mag[i] >> 1;
                    dst[o] = sgn[i] != 0 ? -v : v;
                }
            }
        } else {
            float[] dst = bd.fdata;
            float f = bd.delta * 0.5f;
            for (int y = 0; y < h; y++) {
                int i = (y + 1) * stride + 1;
                int o = (by + y) * bd.w + bx;
                for (int x = 0; x < w; x++, i++, o++) {
                    float v = mag[i] * f;
                    dst[o] = sgn[i] != 0 ? -v : v;
                }
            }
        }
    }

    private int zc(int i, int stride, boolean vcm, int[] lut) {
        byte[] s = sig;
        int hh = s[i - 1] + s[i + 1];
        int v = s[i - stride];
        int d = s[i - stride - 1] + s[i - stride + 1];
        if (!vcm) {
            v += s[i + stride];
            d += s[i + stride - 1] + s[i + stride + 1];
        }
        return lut[hh * 15 + v * 5 + d];
    }

    private int scv(int j) {
        return sig[j] == 0 ? 0 : sgn[j] != 0 ? -1 : 1;
    }

    private void decodeSign(int i, int stride, boolean vcm, boolean raw) {
        if (raw) {
            sgn[i] = (byte) rawBit();
            return;
        }
        int hc = scv(i - 1) + scv(i + 1);
        int vv = scv(i - stride) + (vcm ? 0 : scv(i + stride));
        hc = hc < -1 ? -1 : hc > 1 ? 1 : hc;
        vv = vv < -1 ? -1 : vv > 1 ? 1 : vv;
        int t = (hc + 1) * 3 + vv + 1;
        sgn[i] = (byte) (mq(SC_CTX[t]) ^ SC_XOR[t]);
    }

    private void sigPass(int w, int h, int stride, int plane, int[] lut, boolean vc, boolean raw) {
        int one = 3 << plane;
        for (int y0 = 0; y0 < h; y0 += 4) {
            int ymax = Math.min(y0 + 4, h);
            for (int x = 0; x < w; x++) {
                for (int y = y0; y < ymax; y++) {
                    int i = (y + 1) * stride + x + 1;
                    if (sig[i] != 0) {
                        continue;
                    }
                    boolean vcm = vc && (y & 3) == 3;
                    if (zc(i, stride, vcm, lut) == 0) {
                        continue;
                    }
                    vis[i] = 1;
                    int b = raw ? rawBit() : mq(zc(i, stride, vcm, lut));
                    if (b != 0) {
                        decodeSign(i, stride, vcm, raw);
                        sig[i] = 1;
                        mag[i] = one;
                    }
                }
            }
        }
    }

    private void refPass(int w, int h, int stride, int plane, boolean vc, boolean raw) {
        int half = 1 << plane;
        for (int y0 = 0; y0 < h; y0 += 4) {
            int ymax = Math.min(y0 + 4, h);
            for (int x = 0; x < w; x++) {
                for (int y = y0; y < ymax; y++) {
                    int i = (y + 1) * stride + x + 1;
                    if (sig[i] == 0 || vis[i] != 0) {
                        continue;
                    }
                    int b;
                    if (raw) {
                        b = rawBit();
                    } else {
                        int ctx;
                        if (ref[i] != 0) {
                            ctx = 16;
                        } else {
                            boolean vcm = vc && (y & 3) == 3;
                            int n = sig[i - 1] + sig[i + 1] + sig[i - stride] + sig[i - stride - 1] + sig[i - stride + 1];
                            if (!vcm) {
                                n += sig[i + stride] + sig[i + stride - 1] + sig[i + stride + 1];
                            }
                            ctx = n != 0 ? 15 : 14;
                        }
                        b = mq(ctx);
                    }
                    mag[i] += b != 0 ? half : -half;
                    ref[i] = 1;
                }
            }
        }
    }

    private void cleanPass(int w, int h, int stride, int plane, int[] lut, boolean vc) {
        int one = 3 << plane;
        for (int y0 = 0; y0 < h; y0 += 4) {
            int ymax = Math.min(y0 + 4, h);
            for (int x = 0; x < w; x++) {
                int y = y0;
                if (y0 + 3 < h) {
                    boolean rl = true;
                    for (int k = 0; k < 4 && rl; k++) {
                        int i = (y0 + k + 1) * stride + x + 1;
                        rl = sig[i] == 0 && vis[i] == 0 && zc(i, stride, vc && k == 3, lut) == 0;
                    }
                    if (rl) {
                        if (mq(17) == 0) {
                            continue;
                        }
                        int r = (mq(18) << 1) | mq(18);
                        y = y0 + r;
                        int i = (y + 1) * stride + x + 1;
                        decodeSign(i, stride, vc && (y & 3) == 3, false);
                        sig[i] = 1;
                        mag[i] = one;
                        y++;
                    }
                }
                for (; y < ymax; y++) {
                    int i = (y + 1) * stride + x + 1;
                    if (vis[i] != 0) {
                        vis[i] = 0;
                        continue;
                    }
                    if (sig[i] != 0) {
                        continue;
                    }
                    boolean vcm = vc && (y & 3) == 3;
                    if (mq(zc(i, stride, vcm, lut)) != 0) {
                        decodeSign(i, stride, vcm, false);
                        sig[i] = 1;
                        mag[i] = one;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ wavelet

    private static int mirror(int j, int n) {
        if (n == 1) {
            return 0;
        }
        int period = 2 * n - 2;
        j %= period;
        if (j < 0) {
            j += period;
        }
        return j >= n ? period - j : j;
    }

    private static final int PAD = 4;

    private static void idwt53(int[] a, int off, int stride, int n, int i0, int[] buf) {
        if (n == 1) {
            if ((i0 & 1) != 0) {
                a[off] /= 2;
            }
            return;
        }
        for (int k = 0; k < n; k++) {
            buf[PAD + k] = a[off + k * stride];
        }
        for (int j = 1; j <= PAD; j++) {
            buf[PAD - j] = buf[PAD + mirror(-j, n)];
            buf[PAD + n - 1 + j] = buf[PAD + mirror(n - 1 + j, n)];
        }
        int len = n + 2 * PAD;
        int ev = i0 & 1;
        for (int t = (ev == 1 ? 1 : 2); t < len - 1; t += 2) {
            buf[t] -= (buf[t - 1] + buf[t + 1] + 2) >> 2;
        }
        for (int t = (ev == 1 ? 2 : 1); t < len - 1; t += 2) {
            buf[t] += (buf[t - 1] + buf[t + 1]) >> 1;
        }
        for (int k = 0; k < n; k++) {
            a[off + k * stride] = buf[PAD + k];
        }
    }

    private static final float ALPHA = -1.586134342059924f;
    private static final float BETA = -0.052980118572961f;
    private static final float GAMMA = 0.882911075530934f;
    private static final float DELTA = 0.443506852043971f;
    private static final float K = 1.230174104914001f;
    private static final float INV_K = 1f / 1.230174104914001f;

    private static void idwt97(float[] a, int off, int stride, int n, int i0, float[] buf) {
        if (n == 1) {
            if ((i0 & 1) != 0) {
                a[off] /= 2;
            }
            return;
        }
        for (int k = 0; k < n; k++) {
            buf[PAD + k] = a[off + k * stride];
        }
        for (int j = 1; j <= PAD; j++) {
            buf[PAD - j] = buf[PAD + mirror(-j, n)];
            buf[PAD + n - 1 + j] = buf[PAD + mirror(n - 1 + j, n)];
        }
        int len = n + 2 * PAD;
        int e0 = (i0 & 1) == 0 ? 0 : 1;
        int o0 = 1 - e0;
        for (int t = e0; t < len; t += 2) {
            buf[t] *= K;
        }
        for (int t = o0; t < len; t += 2) {
            buf[t] *= INV_K;
        }
        int es = e0 == 0 ? 2 : 1;
        int os = o0 == 0 ? 2 : 1;
        for (int t = es; t < len - 1; t += 2) {
            buf[t] -= DELTA * (buf[t - 1] + buf[t + 1]);
        }
        for (int t = os; t < len - 1; t += 2) {
            buf[t] -= GAMMA * (buf[t - 1] + buf[t + 1]);
        }
        for (int t = es; t < len - 1; t += 2) {
            buf[t] -= BETA * (buf[t - 1] + buf[t + 1]);
        }
        for (int t = os; t < len - 1; t += 2) {
            buf[t] -= ALPHA * (buf[t - 1] + buf[t + 1]);
        }
        for (int k = 0; k < n; k++) {
            a[off + k * stride] = buf[PAD + k];
        }
    }

    private void idwt(TC tc) {
        boolean rev = tc.cp.rev;
        Res r0 = tc.res[0];
        int[] ic = r0.bands[0].idata;
        float[] fc = r0.bands[0].fdata;
        int pw0 = r0.x1 - r0.x0;
        int ph0 = r0.y1 - r0.y0;
        int px0 = r0.x0;
        int py0 = r0.y0;
        for (int r = 1; r < tc.res.length; r++) {
            Res rr = tc.res[r];
            int rw = rr.x1 - rr.x0;
            int rh = rr.y1 - rr.y0;
            if (rw <= 0 || rh <= 0) {
                ic = rev ? new int[0] : null;
                fc = rev ? null : new float[0];
                pw0 = Math.max(0, rw);
                ph0 = Math.max(0, rh);
                px0 = rr.x0;
                py0 = rr.y0;
                continue;
            }
            int[] ia = rev ? new int[rw * rh] : null;
            float[] fa = rev ? null : new float[rw * rh];
            place(ia, fa, rw, rh, rr.x0, rr.y0, ic, fc, px0, py0, pw0, ph0, 0, 0);
            for (int bi = 0; bi < 3; bi++) {
                Band bd = rr.bands[bi];
                place(ia, fa, rw, rh, rr.x0, rr.y0, bd.idata, bd.fdata, bd.x0, bd.y0, bd.w, bd.h,
                        bd.type & 1, bd.type >> 1);
            }
            int m = Math.max(rw, rh) + 2 * PAD;
            if (rev) {
                int[] buf = new int[m];
                for (int y = 0; y < rh; y++) {
                    idwt53(ia, y * rw, 1, rw, rr.x0, buf);
                }
                for (int x = 0; x < rw; x++) {
                    idwt53(ia, x, rw, rh, rr.y0, buf);
                }
            } else {
                float[] buf = new float[m];
                for (int y = 0; y < rh; y++) {
                    idwt97(fa, y * rw, 1, rw, rr.x0, buf);
                }
                for (int x = 0; x < rw; x++) {
                    idwt97(fa, x, rw, rh, rr.y0, buf);
                }
            }
            ic = ia;
            fc = fa;
            pw0 = rw;
            ph0 = rh;
            px0 = rr.x0;
            py0 = rr.y0;
        }
        tc.idata = ic;
        tc.fdata = fc;
    }

    private static void place(int[] ia, float[] fa, int rw, int rh, int rx0, int ry0, int[] is,
            float[] fs, int bx0, int by0, int bw, int bh, int xo, int yo) {
        for (int v = 0; v < bh; v++) {
            int ay = 2 * (by0 + v) + yo - ry0;
            if (ay < 0 || ay >= rh) {
                continue;
            }
            for (int u = 0; u < bw; u++) {
                int ax = 2 * (bx0 + u) + xo - rx0;
                if (ax < 0 || ax >= rw) {
                    continue;
                }
                if (ia != null && is != null) {
                    ia[ay * rw + ax] = is[v * bw + u];
                } else if (fa != null && fs != null) {
                    fa[ay * rw + ax] = fs[v * bw + u];
                }
            }
        }
    }

    private static int roundF(float v) {
        return v >= 0 ? (int) (v + 0.5f) : -(int) (0.5f - v);
    }

    private void storeTile(TC[] tcs, boolean mct) {
        if (mct && ncomp >= 3) {
            TC a = tcs[0];
            TC b = tcs[1];
            TC c = tcs[2];
            boolean same = a.x1 - a.x0 == b.x1 - b.x0 && a.x1 - a.x0 == c.x1 - c.x0
                    && a.y1 - a.y0 == b.y1 - b.y0 && a.y1 - a.y0 == c.y1 - c.y0;
            if (same && a.idata != null && b.idata != null && c.idata != null
                    && a.idata.length == b.idata.length && a.idata.length == c.idata.length) {
                int[] y0 = a.idata;
                int[] y1 = b.idata;
                int[] y2 = c.idata;
                for (int i = 0; i < y0.length; i++) {
                    int g = y0[i] - ((y1[i] + y2[i]) >> 2);
                    int r = y2[i] + g;
                    int bl = y1[i] + g;
                    y0[i] = r;
                    y1[i] = g;
                    y2[i] = bl;
                }
                mctUsed = true;
            } else if (same && a.fdata != null && b.fdata != null && c.fdata != null
                    && a.fdata.length == b.fdata.length && a.fdata.length == c.fdata.length) {
                float[] y0 = a.fdata;
                float[] y1 = b.fdata;
                float[] y2 = c.fdata;
                for (int i = 0; i < y0.length; i++) {
                    float yy = y0[i];
                    float cb = y1[i];
                    float cr = y2[i];
                    y0[i] = yy + 1.402f * cr;
                    y1[i] = yy - 0.34413f * cb - 0.71414f * cr;
                    y2[i] = yy + 1.772f * cb;
                }
                mctUsed = true;
            }
        }
        for (int c = 0; c < ncomp; c++) {
            TC tc = tcs[c];
            int w = tc.x1 - tc.x0;
            int h = tc.y1 - tc.y0;
            int p = prec[c];
            int shift = sgnd[c] ? 0 : 1 << (p - 1);
            int min = sgnd[c] ? -(1 << (p - 1)) : 0;
            int max = sgnd[c] ? (1 << (p - 1)) - 1 : (1 << p) - 1;
            int ox = tc.x0 - ceilDiv(xosiz, dx[c]);
            int oy = tc.y0 - ceilDiv(yosiz, dy[c]);
            int[] dst = planes[c];
            int dw = pw[c];
            int[] is = tc.idata;
            float[] fs = tc.fdata;
            if ((is == null || is.length < w * h) && (fs == null || fs.length < w * h)) {
                continue;
            }
            for (int y = 0; y < h; y++) {
                int py = oy + y;
                if (py < 0 || py >= ph[c]) {
                    continue;
                }
                for (int x = 0; x < w; x++) {
                    int px = ox + x;
                    if (px < 0 || px >= dw) {
                        continue;
                    }
                    int v = is != null ? is[y * w + x] : roundF(fs[y * w + x]);
                    v += shift;
                    dst[py * dw + px] = v < min ? min : v > max ? max : v;
                }
            }
        }
    }

    // ------------------------------------------------------------------ output

    /// ARGB pixels (row-major, `width * height`).
    ///
    /// `cs` is the PDF image's `/ColorSpace`; when it is null the codestream's own components
    /// decide: 1 is gray, 3 is RGB, 4 is CMYK, and a JP2 palette (`pclr`) is applied. When it is
    /// non-null its component count is fed to `toRgb`, each component scaled to 0..1 (an
    /// Indexed space takes the raw index). With `smaskInData` an extra component (named by a
    /// JP2 `cdef` box, or the one after the colour components) becomes the alpha channel.
    /// Returns null on failure.
    int[] argb(PdfColorSpace cs, boolean smaskInData) {
        try {
            return argbImpl(cs, smaskInData);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private int[] argbImpl(PdfColorSpace cs, boolean smask) {
        if (planes == null) {
            return null;
        }
        boolean indexed = cs != null && cs.getKind() == PdfColorSpace.INDEXED;
        int nch;
        int[] chComp;
        int[][] chPal;
        int[] chPrec;
        boolean[] chSigned;
        if (palette != null && !indexed) {
            if (cmapComp != null) {
                nch = cmapComp.length;
            } else {
                nch = palette.length;
            }
            chComp = new int[nch];
            chPal = new int[nch][];
            chPrec = new int[nch];
            chSigned = new boolean[nch];
            for (int i = 0; i < nch; i++) {
                int comp = cmapComp != null ? cmapComp[i] : 0;
                int col = cmapComp != null ? (cmapType[i] == 1 ? cmapCol[i] : -1) : i;
                if (comp >= ncomp) {
                    return null;
                }
                chComp[i] = comp;
                if (col >= 0 && col < palette.length) {
                    chPal[i] = palette[col];
                    chPrec[i] = palDepth[col];
                    chSigned[i] = palSigned[col];
                } else {
                    chPrec[i] = prec[comp];
                    chSigned[i] = sgnd[comp];
                }
            }
        } else {
            nch = ncomp;
            chComp = new int[nch];
            chPal = new int[nch][];
            chPrec = new int[nch];
            chSigned = new boolean[nch];
            for (int i = 0; i < nch; i++) {
                chComp[i] = i;
                chPrec[i] = prec[i];
                chSigned[i] = sgnd[i];
            }
        }
        int alpha = -1;
        int[] order = new int[nch];
        int no = 0;
        if (cdefCn != null) {
            int[] byAsoc = new int[nch];
            for (int i = 0; i < nch; i++) {
                byAsoc[i] = -1;
            }
            boolean[] used = new boolean[nch];
            for (int i = 0; i < cdefCn.length; i++) {
                int cn = cdefCn[i];
                if (cn >= nch) {
                    continue;
                }
                if (cdefTyp[i] == 1 || cdefTyp[i] == 2) {
                    alpha = cn;
                    used[cn] = true;
                } else if (cdefTyp[i] == 0 && cdefAsoc[i] >= 1 && cdefAsoc[i] <= nch) {
                    byAsoc[cdefAsoc[i] - 1] = cn;
                    used[cn] = true;
                }
            }
            for (int i = 0; i < nch; i++) {
                if (byAsoc[i] >= 0) {
                    order[no++] = byAsoc[i];
                }
            }
            for (int i = 0; i < nch; i++) {
                if (!used[i]) {
                    order[no++] = i;
                }
            }
        } else {
            for (int i = 0; i < nch; i++) {
                order[no++] = i;
            }
        }
        int need;
        if (cs != null) {
            need = indexed ? 1 : Math.max(1, cs.getComponents());
        } else if (enumCs == 16 || enumCs == 18) {
            need = 3;
        } else if (enumCs == 17) {
            need = 1;
        } else if (enumCs == 12) {
            need = 4;
        } else {
            need = no <= 2 ? 1 : no == 3 ? 3 : 4;
        }
        if (no < need) {
            if (cs == null && no >= 1) {
                need = no >= 3 ? 3 : 1;
            } else {
                return null;
            }
        }
        if (alpha < 0 && smask && no > need) {
            alpha = order[need];
        }
        if (!smask) {
            alpha = -1;
        }
        int nsel = need + (alpha >= 0 ? 1 : 0);
        int[] sel = new int[nsel];
        for (int i = 0; i < need; i++) {
            sel[i] = order[i];
        }
        if (alpha >= 0) {
            sel[need] = alpha;
        }
        int w = width;
        int h = height;
        int[][] xm = new int[nsel][];
        int[][] ym = new int[nsel][];
        int[][] src = new int[nsel][];
        int[] srcW = new int[nsel];
        int[] mins = new int[nsel];
        int[] maxs = new int[nsel];
        int[][] pals = new int[nsel][];
        for (int s = 0; s < nsel; s++) {
            int ch = sel[s];
            int c = chComp[ch];
            src[s] = planes[c];
            srcW[s] = pw[c];
            pals[s] = chPal[ch];
            int p = chPrec[ch];
            mins[s] = chSigned[ch] ? -(1 << (p - 1)) : 0;
            maxs[s] = (1 << p) - 1;
            xm[s] = axisMap(w, xosiz, dx[c], pw[c]);
            ym[s] = axisMap(h, yosiz, dy[c], ph[c]);
        }
        int[] out = new int[w * h];
        int[] raw = new int[nsel];
        int[] last = new int[nsel];
        int lastRgb = 0;
        boolean haveLast = false;
        double[] cc = cs != null ? new double[need] : null;
        boolean ycc = cs == null && need == 3 && enumCs == 18 && !mctUsed;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                for (int s = 0; s < nsel; s++) {
                    int v = src[s][ym[s][y] * srcW[s] + xm[s][x]];
                    int[] pl = pals[s];
                    if (pl != null) {
                        v = pl[v < 0 ? 0 : v >= pl.length ? pl.length - 1 : v];
                    }
                    raw[s] = v;
                }
                int a = 255;
                if (alpha >= 0) {
                    a = scale8(raw[need], mins[need], maxs[need]);
                }
                boolean same = haveLast;
                for (int s = 0; s < need && same; s++) {
                    same = raw[s] == last[s];
                }
                int rgb;
                if (same) {
                    rgb = lastRgb;
                } else {
                    if (cs != null) {
                        if (indexed) {
                            cc[0] = raw[0];
                        } else {
                            for (int s = 0; s < need; s++) {
                                cc[s] = (raw[s] - mins[s]) / (double) maxs[s];
                            }
                        }
                        rgb = cs.toRgb(cc) & 0xffffff;
                    } else if (need == 1) {
                        int g = scale8(raw[0], mins[0], maxs[0]);
                        rgb = (g << 16) | (g << 8) | g;
                    } else if (need == 3) {
                        int r = scale8(raw[0], mins[0], maxs[0]);
                        int g = scale8(raw[1], mins[1], maxs[1]);
                        int b = scale8(raw[2], mins[2], maxs[2]);
                        if (ycc) {
                            int cb = g - 128;
                            int cr = b - 128;
                            int yy = r;
                            r = clamp8(roundF(yy + 1.402f * cr));
                            g = clamp8(roundF(yy - 0.34413f * cb - 0.71414f * cr));
                            b = clamp8(roundF(yy + 1.772f * cb));
                        }
                        rgb = (r << 16) | (g << 8) | b;
                    } else {
                        if (cc == null) {
                            cc = new double[4];
                        }
                        for (int s = 0; s < 4; s++) {
                            cc[s] = (raw[s] - mins[s]) / (double) maxs[s];
                        }
                        rgb = PdfColorSpace.DEVICE_CMYK.toRgb(cc) & 0xffffff;
                    }
                    for (int s = 0; s < need; s++) {
                        last[s] = raw[s];
                    }
                    lastRgb = rgb;
                    haveLast = true;
                }
                out[y * w + x] = (a << 24) | rgb;
            }
        }
        return out;
    }

    private static int clamp8(int v) {
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }

    private static int scale8(int v, int min, int max) {
        int u = v - min;
        if (u <= 0) {
            return 0;
        }
        if (u >= max) {
            return 255;
        }
        if (max == 255) {
            return u;
        }
        return (int) (((long) u * 255 + (max >> 1)) / max);
    }

    private static int[] axisMap(int n, int off, int d, int size) {
        int[] m = new int[n];
        int base = ceilDiv(off, d);
        for (int i = 0; i < n; i++) {
            int v = (off + i) / d - base;
            m[i] = v < 0 ? 0 : v >= size ? size - 1 : v;
        }
        return m;
    }
}
