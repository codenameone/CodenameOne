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

import java.util.HashMap;
import java.util.Map;

/// Glyph outlines of a TrueType font embedded in a PDF (`FontFile2`).
///
/// Lenient on purpose: an embedded subset often carries only `head`,
/// `loca`, `glyf` and `maxp`, and sometimes a `cmap` for one platform
/// only, so nothing else is required.
public final class TrueTypeOutlines {
    private static final int MAX_COMPONENT_DEPTH = 8;

    private final byte[] d;
    private final Map<String, int[]> tables = new HashMap<String, int[]>();
    private int unitsPerEm = 1000;
    private int numGlyphs;
    private boolean longLoca;
    private int cmap31 = -1;
    private int cmap30 = -1;
    private int cmap10 = -1;

    /// Parses the table directory of `data`.
    public TrueTypeOutlines(byte[] data) throws java.io.IOException {
        d = data;
        if (data.length < 12) {
            throw new java.io.IOException("Not a TrueType font");
        }
        int count = u16(4);
        for (int i = 0; i < count; i++) {
            int rec = 12 + i * 16;
            if (rec + 16 > d.length) {
                break;
            }
            StringBuilder tag = new StringBuilder(4);
            for (int k = 0; k < 4; k++) {
                tag.append((char) (d[rec + k] & 0xff));
            }
            int off = u32(rec + 8);
            int len = u32(rec + 12);
            if (off >= 0 && len >= 0 && (long) off + len <= d.length) {
                tables.put(tag.toString(), new int[] {off, len});
            }
        }
        int[] head = tables.get("head");
        int[] maxp = tables.get("maxp");
        if (head == null || tables.get("loca") == null || tables.get("glyf") == null) {
            throw new java.io.IOException("TrueType font has no outlines");
        }
        unitsPerEm = u16(head[0] + 18);
        if (unitsPerEm < 16) {
            unitsPerEm = 1000;
        }
        longLoca = s16(head[0] + 50) != 0;
        int locaEntries = tables.get("loca")[1] / (longLoca ? 4 : 2) - 1;
        numGlyphs = maxp != null ? Math.min(u16(maxp[0] + 4), locaEntries) : locaEntries;
        int[] cmap = tables.get("cmap");
        if (cmap != null) {
            int n = u16(cmap[0] + 2);
            for (int i = 0; i < n; i++) {
                int rec = cmap[0] + 4 + i * 8;
                if (rec + 8 > d.length) {
                    break;
                }
                int platform = u16(rec);
                int encoding = u16(rec + 2);
                int off = cmap[0] + u32(rec + 4);
                if (off < 0 || off + 4 > d.length) {
                    continue;
                }
                if (platform == 3 && (encoding == 1 || encoding == 10) || platform == 0) {
                    if (cmap31 < 0 || u16(off) == 12) {
                        cmap31 = off;
                    }
                } else if (platform == 3 && encoding == 0) {
                    cmap30 = off;
                } else if (platform == 1 && encoding == 0) {
                    cmap10 = off;
                }
            }
        }
    }

    /// True when the font has a Unicode cmap.
    public boolean hasUnicodeCmap() {
        return cmap31 >= 0;
    }

    /// True when the font has a (3,0) symbol cmap.
    public boolean hasSymbolCmap() {
        return cmap30 >= 0;
    }

    /// The glyph of Unicode `cp`, or 0.
    public int glyphForUnicode(int cp) {
        return cmap31 < 0 ? 0 : lookup(cmap31, cp);
    }

    /// The glyph of single-byte `code` through the (3,0) cmap, trying the
    /// F000, F100 and F200 pages symbol fonts use; or 0.
    public int glyphForSymbol(int code) {
        if (cmap30 < 0) {
            return 0;
        }
        int g = lookup(cmap30, code);
        for (int page = 0xf000; g == 0 && page <= 0xf200; page += 0x100) {
            g = lookup(cmap30, page | code);
        }
        return g;
    }

    /// The glyph of single-byte `code` through the (1,0) Mac cmap, or 0.
    public int glyphForMac(int code) {
        return cmap10 < 0 ? 0 : lookup(cmap10, code);
    }

    /// The number of glyphs.
    public int getNumGlyphs() {
        return numGlyphs;
    }

    private int lookup(int off, int cp) {
        int format = u16(off);
        int g = 0;
        if (format == 0) {
            g = cp >= 0 && cp < 256 && off + 6 + cp < d.length ? d[off + 6 + cp] & 0xff : 0;
        } else if (format == 4) {
            int segX2 = u16(off + 6);
            int ends = off + 14;
            int starts = ends + segX2 + 2;
            int deltas = starts + segX2;
            int ranges = deltas + segX2;
            for (int s = 0; s < segX2; s += 2) {
                int end = u16(ends + s);
                if (cp > end) {
                    continue;
                }
                int start = u16(starts + s);
                if (cp < start) {
                    break;
                }
                int delta = s16(deltas + s);
                int ro = u16(ranges + s);
                if (ro == 0) {
                    g = (cp + delta) & 0xffff;
                } else {
                    int p = ranges + s + ro + (cp - start) * 2;
                    int v = p + 2 <= d.length ? u16(p) : 0;
                    g = v == 0 ? 0 : (v + delta) & 0xffff;
                }
                break; // NOPMD AvoidBranchingStatementAsLastInLoop - the loop scans for the first match
            }
        } else if (format == 6) {
            int first = u16(off + 6);
            int count = u16(off + 8);
            if (cp >= first && cp < first + count) {
                g = u16(off + 10 + (cp - first) * 2);
            }
        } else if (format == 12) {
            int n = u32(off + 12);
            int lo = 0;
            int hi = n - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int rec = off + 16 + mid * 12;
                int start = u32(rec);
                int end = u32(rec + 4);
                if (cp < start) {
                    hi = mid - 1;
                } else if (cp > end) {
                    lo = mid + 1;
                } else {
                    g = u32(rec + 8) + cp - start;
                    break;
                }
            }
        }
        return g < numGlyphs ? g : 0;
    }

    /// Writes the outline of glyph `g` to `sink`, in em units.
    public void outline(int g, GlyphSink sink) {
        outline(g, sink, 1, 0, 0, 1, 0, 0, 0);
    }

    private void outline(int g, GlyphSink sink, double a, double b, double c, double dd, double e, double f,
            int depth) {
        if (g < 0 || g >= numGlyphs || depth > MAX_COMPONENT_DEPTH) {
            return;
        }
        int loca = tables.get("loca")[0];
        int start;
        int end;
        if (longLoca) {
            start = u32(loca + g * 4);
            end = u32(loca + g * 4 + 4);
        } else {
            start = u16(loca + g * 2) * 2;
            end = u16(loca + g * 2 + 2) * 2;
        }
        int glyf = tables.get("glyf")[0];
        if (end <= start || glyf + end > d.length || end - start < 10) {
            return;
        }
        int p = glyf + start;
        int contours = s16(p);
        if (contours < 0) {
            composite(p + 10, glyf + end, sink, a, b, c, dd, e, f, depth);
        } else {
            simple(p, contours, glyf + end, sink, a, b, c, dd, e, f);
        }
    }

    private void simple(int p, int contours, int limit, GlyphSink sink, double a, double b, double c, double dd,
            double e, double f) {
        int q = p + 10;
        if (q + contours * 2 + 2 > limit) {
            return;
        }
        int[] endPts = new int[contours];
        for (int i = 0; i < contours; i++) {
            endPts[i] = u16(q + i * 2);
        }
        int n = contours == 0 ? 0 : endPts[contours - 1] + 1;
        q += contours * 2;
        int instr = u16(q);
        q += 2 + instr;
        byte[] flags = new byte[n];
        for (int i = 0; i < n && q < limit;) {
            byte fl = d[q++];
            flags[i++] = fl;
            if ((fl & 8) != 0 && q < limit) {
                int rep = d[q++] & 0xff;
                for (int k = 0; k < rep && i < n; k++) {
                    flags[i++] = fl;
                }
            }
        }
        int[] xs = new int[n];
        int[] ys = new int[n];
        int v = 0;
        for (int i = 0; i < n; i++) {
            int fl = flags[i];
            if ((fl & 2) != 0) {
                int dx = q < limit ? d[q++] & 0xff : 0;
                v += (fl & 16) != 0 ? dx : -dx;
            } else if ((fl & 16) == 0) {
                v += q + 2 <= limit ? s16(q) : 0;
                q += 2;
            }
            xs[i] = v;
        }
        v = 0;
        for (int i = 0; i < n; i++) {
            int fl = flags[i];
            if ((fl & 4) != 0) {
                int dy = q < limit ? d[q++] & 0xff : 0;
                v += (fl & 32) != 0 ? dy : -dy;
            } else if ((fl & 32) == 0) {
                v += q + 2 <= limit ? s16(q) : 0;
                q += 2;
            }
            ys[i] = v;
        }
        double u = unitsPerEm;
        int first = 0;
        for (int ci = 0; ci < contours; ci++) {
            int last = endPts[ci];
            if (last < first || last >= n) {
                break;
            }
            int count = last - first + 1;
            // Find a start point that is on the curve, or synthesize the
            // midpoint of the first two off-curve points.
            int startIdx = -1;
            for (int k = 0; k < count; k++) {
                if ((flags[first + k] & 1) != 0) {
                    startIdx = k;
                    break;
                }
            }
            double sx;
            double sy;
            if (startIdx < 0) {
                sx = (xs[first] + xs[first + (count > 1 ? 1 : 0)]) / 2.0;
                sy = (ys[first] + ys[first + (count > 1 ? 1 : 0)]) / 2.0;
                startIdx = 0;
            } else {
                sx = xs[first + startIdx];
                sy = ys[first + startIdx];
            }
            sink.moveTo(tx(a, c, e, sx, sy, u), ty(b, dd, f, sx, sy, u));
            boolean pendingOff = false;
            double ox = 0;
            double oy = 0;
            boolean allOff = (flags[first + startIdx] & 1) == 0;
            for (int k = 1; k <= count; k++) {
                int idx = first + (startIdx + k) % count;
                if (allOff && k == count) {
                    idx = first + startIdx;
                }
                double x = xs[idx];
                double y = ys[idx];
                boolean on = (flags[idx] & 1) != 0;
                if (k == count && !allOff) {
                    x = sx;
                    y = sy;
                    on = true;
                }
                if (on) {
                    if (pendingOff) {
                        sink.quadTo(tx(a, c, e, ox, oy, u), ty(b, dd, f, ox, oy, u), tx(a, c, e, x, y, u),
                                ty(b, dd, f, x, y, u));
                    } else {
                        sink.lineTo(tx(a, c, e, x, y, u), ty(b, dd, f, x, y, u));
                    }
                    pendingOff = false;
                } else {
                    if (pendingOff) {
                        double mx = (ox + x) / 2;
                        double my = (oy + y) / 2;
                        sink.quadTo(tx(a, c, e, ox, oy, u), ty(b, dd, f, ox, oy, u), tx(a, c, e, mx, my, u),
                                ty(b, dd, f, mx, my, u));
                    }
                    ox = x;
                    oy = y;
                    pendingOff = true;
                }
            }
            if (pendingOff) {
                sink.quadTo(tx(a, c, e, ox, oy, u), ty(b, dd, f, ox, oy, u), tx(a, c, e, sx, sy, u),
                        ty(b, dd, f, sx, sy, u));
            }
            sink.closePath();
            first = last + 1;
        }
    }

    private static double tx(double a, double c, double e, double x, double y, double u) {
        return (a * x + c * y) / u + e;
    }

    private static double ty(double b, double dd, double f, double x, double y, double u) {
        return (b * x + dd * y) / u + f;
    }

    private void composite(int p, int limit, GlyphSink sink, double a, double b, double c, double dd, double e,
            double f, int depth) {
        while (p + 4 <= limit) {
            int flags = u16(p);
            int glyph = u16(p + 2);
            p += 4;
            double dx;
            double dy;
            if ((flags & 1) != 0) {
                dx = s16(p);
                dy = s16(p + 2);
                p += 4;
            } else {
                dx = (byte) d[p];
                dy = (byte) d[p + 1];
                p += 2;
            }
            double m0 = 1;
            double m1 = 0;
            double m2 = 0;
            double m3 = 1;
            if ((flags & 8) != 0) {
                m0 = f2dot14(p);
                m3 = m0;
                p += 2;
            } else if ((flags & 0x40) != 0) {
                m0 = f2dot14(p);
                m3 = f2dot14(p + 2);
                p += 4;
            } else if ((flags & 0x80) != 0) {
                m0 = f2dot14(p);
                m1 = f2dot14(p + 2);
                m2 = f2dot14(p + 4);
                m3 = f2dot14(p + 6);
                p += 8;
            }
            if ((flags & 2) == 0) {
                // Point matching is rare and needs hinting-free point
                // lookup; place the component at the origin instead.
                dx = 0;
                dy = 0;
            }
            double u = unitsPerEm;
            // component matrix [m0 m1 m2 m3 dx/u dy/u] composed with ours
            double na = m0 * a + m1 * c;
            double nb = m0 * b + m1 * dd;
            double nc = m2 * a + m3 * c;
            double nd = m2 * b + m3 * dd;
            double ne = (dx / u) * a + (dy / u) * c + e;
            double nf = (dx / u) * b + (dy / u) * dd + f;
            outline(glyph, sink, na, nb, nc, nd, ne, nf, depth + 1);
            if ((flags & 0x20) == 0) {
                break;
            }
        }
    }

    private double f2dot14(int p) {
        return s16(p) / 16384.0;
    }

    private int u16(int p) {
        return p + 1 < d.length && p >= 0 ? ((d[p] & 0xff) << 8) | (d[p + 1] & 0xff) : 0;
    }

    private int s16(int p) {
        return (short) u16(p);
    }

    private int u32(int p) {
        return p + 3 < d.length && p >= 0
                ? ((d[p] & 0xff) << 24) | ((d[p + 1] & 0xff) << 16) | ((d[p + 2] & 0xff) << 8) | (d[p + 3] & 0xff)
                : 0;
    }
}
