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


/// The Unicode values of the CIDs of Adobe's CJK character collections
/// (Japan1, GB1, CNS1, Korea1), for text in CID fonts that carry no
/// `ToUnicode` map. Decoded on first use from [CidToUnicodeData], generated
/// by `scripts/pdf/build-cid-to-unicode.py` from Adobe's CMap resources
/// (BSD 3-clause; see the NOTICE file). The table is a class rather than a
/// resource so that it is stripped, with the rest of the PDF code, from an
/// application that never reads a PDF; a resource would ship in every one.
@com.codename1.impl.SharedWithBackend
final class CidToUnicode {
    private static final String[] NAMES = {"Japan1", "GB1", "CNS1", "Korea1"};
    // Filled on first use and published whole per collection.
    private static final int[][] TABLES = new int[4][];
    private static byte[] data;
    private static boolean loaded;

    private CidToUnicode() {
    }

    /// The Unicode value of `cid` in the collection whose `Ordering` is
    /// `ordering` (for example `Japan1`), or 0.
    static int lookup(String ordering, int cid) {
        int index = -1;
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(ordering)) {
                index = i;
            }
        }
        if (index < 0 || cid < 0) {
            return 0;
        }
        int[] t = TABLES[index];
        if (t == null) {
            t = load("Adobe-" + NAMES[index]);
            TABLES[index] = t;
        }
        return cid < t.length ? t[cid] : 0;
    }

    private static int[] load(String name) {
        byte[] d = read();
        if (d == null || d.length == 0) {
            return new int[0];
        }
        int pos = 1;
        int count = d[0] & 0xff;
        for (int c = 0; c < count && pos < d.length; c++) {
            int nl = d[pos++] & 0xff;
            StringBuilder sb = new StringBuilder(nl);
            for (int i = 0; i < nl && pos < d.length; i++) {
                sb.append((char) (d[pos++] & 0xff));
            }
            int cids = u32(d, pos);
            int len = u32(d, pos + 4);
            pos += 8;
            if (!name.equals(sb.toString())) {
                pos += len;
                continue;
            }
            int[] t = new int[Math.max(0, Math.min(cids, 1 << 20))];
            int end = Math.min(d.length, pos + len);
            int cid = 0;
            int prev = 0;
            int[] p = {pos};
            while (p[0] < end && cid < t.length) {
                int z = varint(d, p);
                int delta = (z & 1) == 0 ? z >>> 1 : -((z + 1) >>> 1);
                int run = varint(d, p);
                int u = prev + delta;
                for (int k = 0; k < run && cid < t.length; k++) {
                    t[cid++] = u == 0 ? 0 : u + k;
                }
                prev = u + run;
            }
            return t; // NOPMD AvoidBranchingStatementAsLastInLoop - the loop scans for the first match
        }
        return new int[0];
    }

    private static int u32(byte[] d, int p) {
        if (p + 3 >= d.length) {
            return 0;
        }
        return ((d[p] & 0xff) << 24) | ((d[p + 1] & 0xff) << 16) | ((d[p + 2] & 0xff) << 8) | (d[p + 3] & 0xff);
    }

    private static int varint(byte[] d, int[] p) {
        int v = 0;
        int shift = 0;
        while (p[0] < d.length && shift < 32) {
            int b = d[p[0]++] & 0xff;
            v |= (b & 0x7f) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
        }
        return v;
    }

    private static byte[] read() {
        if (loaded) {
            return data;
        }
        byte[] packed = new byte[CidToUnicodeData.LENGTH];
        int o = 0;
        int acc = 0;
        int bits = 0;
        for (String c : CidToUnicodeData.chunks()) {
            int len = c.length();
            for (int k = 0; k < len && o < packed.length; k++) {
                acc = (acc << 7) | (c.charAt(k) & 0x7f);
                bits += 7;
                if (bits >= 8) {
                    bits -= 8;
                    packed[o++] = (byte) (acc >> bits);
                    acc &= (1 << bits) - 1;
                }
            }
        }
        data = PdfFilters.inflate(packed);
        loaded = true;
        return data;
    }
}
