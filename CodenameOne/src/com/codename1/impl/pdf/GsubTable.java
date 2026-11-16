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

import java.util.ArrayList;
import java.util.List;

/// The parts of an OpenType `GSUB` table that Arabic shaping needs:
/// single substitutions (the `init`, `medi`, `fina` and `isol` forms) and
/// ligatures (`rlig`, lam with alef), directly or through extension
/// lookups. Contextual and chaining lookups are not applied.
@com.codename1.impl.SharedWithBackend
final class GsubTable {
    private final byte[] d;
    private final int base;
    private int[] langFeatures;
    private final List<int[]> cache = new ArrayList<int[]>();
    private final List<String> cacheTags = new ArrayList<String>();

    GsubTable(byte[] data, int offset) {
        this.d = data;
        this.base = offset;
    }

    private int u16(int p) {
        if (p < 0 || p + 1 >= d.length) {
            return 0;
        }
        return ((d[p] & 0xff) << 8) | (d[p + 1] & 0xff);
    }

    private int s16(int p) {
        return (short) u16(p);
    }

    private long u32(int p) {
        return ((long) u16(p) << 16) | u16(p + 2);
    }

    private String tag(int p) {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4 && p + i < d.length; i++) {
            sb.append((char) (d[p + i] & 0xff));
        }
        return sb.toString();
    }

    /// The feature indices of the default language of the Arabic script
    /// (or the default script).
    private int[] languageFeatures() {
        if (langFeatures != null) {
            return langFeatures;
        }
        int scripts = base + u16(base + 4);
        int count = u16(scripts);
        int chosen = -1;
        for (int pass = 0; pass < 2 && chosen < 0; pass++) {
            for (int i = 0; i < count; i++) {
                String t = tag(scripts + 2 + i * 6);
                if (pass == 0 && "arab".equals(t) || pass == 1 && "DFLT".equals(t)) {
                    chosen = scripts + u16(scripts + 2 + i * 6 + 4);
                    break;
                }
            }
        }
        if (chosen < 0) {
            langFeatures = new int[0];
            return langFeatures;
        }
        int lang = u16(chosen);
        if (lang == 0) {
            langFeatures = new int[0];
            return langFeatures;
        }
        int ls = chosen + lang;
        int n = u16(ls + 4);
        int[] f = new int[n];
        for (int i = 0; i < n; i++) {
            f[i] = u16(ls + 6 + i * 2);
        }
        langFeatures = f;
        return f;
    }

    /// The lookup indices of feature `feature` in the chosen language.
    private int[] lookups(String feature) {
        int ci = cacheTags.indexOf(feature);
        if (ci >= 0) {
            return cache.get(ci);
        }
        int features = base + u16(base + 6);
        int[] lf = languageFeatures();
        List<Integer> out = new ArrayList<Integer>();
        for (int k = 0; k < lf.length; k++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            int rec = features + 2 + lf[k] * 6;
            if (feature.equals(tag(rec))) {
                int f = features + u16(rec + 4);
                int n = u16(f + 2);
                for (int i = 0; i < n; i++) {
                    out.add(Integer.valueOf(u16(f + 4 + i * 2)));
                }
            }
        }
        int[] r = new int[out.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = out.get(i).intValue();
        }
        cacheTags.add(feature);
        cache.add(r);
        return r;
    }

    /// True when the font has feature `feature` for Arabic.
    boolean has(String feature) {
        return lookups(feature).length > 0;
    }

    /// Subtable offsets of lookup `index`, extensions unwrapped, with
    /// their type in element 0 of each pair.
    private int[] subtables(int index) {
        int list = base + u16(base + 8);
        if (index >= u16(list)) {
            return new int[0];
        }
        int lk = list + u16(list + 2 + index * 2);
        int type = u16(lk);
        int n = u16(lk + 4);
        int[] r = new int[n * 2];
        for (int i = 0; i < n; i++) {
            int st = lk + u16(lk + 6 + i * 2);
            int t = type;
            if (t == 7) {
                t = u16(st + 2);
                st = (int) (st + u32(st + 4));
            }
            r[i * 2] = t;
            r[i * 2 + 1] = st;
        }
        return r;
    }

    private int coverage(int cov, int g) {
        int format = u16(cov);
        if (format == 1) {
            int lo = 0;
            int hi = u16(cov + 2) - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int v = u16(cov + 4 + mid * 2);
                if (v == g) {
                    return mid;
                }
                if (v < g) {
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
        } else if (format == 2) {
            int n = u16(cov + 2);
            for (int i = 0; i < n; i++) {
                int r = cov + 4 + i * 6;
                int start = u16(r);
                if (g >= start && g <= u16(r + 2)) {
                    return u16(r + 4) + g - start;
                }
            }
        }
        return -1;
    }

    /// `g` after the single substitutions of `feature`.
    int substitute(String feature, int g) {
        int[] lk = lookups(feature);
        int out = g;
        for (int i = 0; i < lk.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            int[] st = subtables(lk[i]);
            for (int k = 0; k < st.length; k += 2) {
                if (st[k] != 1) {
                    continue;
                }
                int s = st[k + 1];
                int ci = coverage(s + u16(s + 2), out);
                if (ci < 0) {
                    continue;
                }
                if (u16(s) == 1) {
                    out = (out + s16(s + 4)) & 0xffff;
                } else if (ci < u16(s + 4)) {
                    out = u16(s + 6 + ci * 2);
                }
                break; // NOPMD AvoidBranchingStatementAsLastInLoop - the loop scans for the first match
            }
        }
        return out;
    }

    /// The ligature of `feature` starting at `glyphs[pos]`: `{glyph,
    /// count}`, or null.
    int[] ligature(String feature, int[] glyphs, int pos, int end) {
        int[] lk = lookups(feature);
        for (int i = 0; i < lk.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            int[] st = subtables(lk[i]);
            for (int k = 0; k < st.length; k += 2) {
                if (st[k] != 4) {
                    continue;
                }
                int s = st[k + 1];
                int ci = coverage(s + u16(s + 2), glyphs[pos]);
                if (ci < 0 || ci >= u16(s + 4)) {
                    continue;
                }
                int set = s + u16(s + 6 + ci * 2);
                int n = u16(set);
                for (int j = 0; j < n; j++) {
                    int lig = set + u16(set + 2 + j * 2);
                    int comps = u16(lig + 2);
                    if (pos + comps > end) {
                        continue;
                    }
                    boolean match = true;
                    for (int c = 1; c < comps && match; c++) {
                        match = glyphs[pos + c] == u16(lig + 4 + (c - 1) * 2);
                    }
                    if (match) {
                        return new int[] {u16(lig), comps};
                    }
                }
            }
        }
        return null;
    }
}
