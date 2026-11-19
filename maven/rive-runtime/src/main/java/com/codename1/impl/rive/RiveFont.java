/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import com.codename1.impl.pdf.GlyphSink;
import com.codename1.impl.pdf.TrueTypeOutlines;

import java.util.HashMap;
import java.util.Map;

/// The font behind a `FontAsset`: the role of upstream's `rive::Font`
/// without HarfBuzz. Outlines and the cmap come from the pure-Java
/// `TrueTypeOutlines`; horizontal metrics (`hhea`, `hmtx`) and pair kerning
/// (`kern` format 0) are read here. Every value is in em units (1 = font
/// size) with y pointing down, the convention upstream's glyph paths use.
///
/// Only TrueType outlines (`glyf`) are supported. A CFF-flavoured OpenType
/// font fails to parse and the asset then has no font, so its text draws
/// nothing.
public final class RiveFont {
    private final byte[] d;
    private final TrueTypeOutlines outlines;
    private final Map<Integer, RivePath> paths = new HashMap<Integer, RivePath>();
    private final Map<Integer, Integer> kerning = new HashMap<Integer, Integer>();
    private float unitsPerEm = 1000f;
    private float ascent = -0.8f;
    private float descent = 0.2f;
    private int numHMetrics;
    private int hmtx = -1;

    private RiveFont(byte[] data, TrueTypeOutlines outlines) {
        d = data;
        this.outlines = outlines;
    }

    /// Parses `data`, or answers null when it is not a TrueType font this
    /// runtime can draw.
    public static RiveFont decode(byte[] data) {
        if (data == null || data.length < 12) {
            return null;
        }
        try {
            RiveFont f = new RiveFont(data, new TrueTypeOutlines(data));
            f.readMetrics();
            return f;
        } catch (java.io.IOException e) {
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private int u16(int o) {
        return (d[o] & 0xff) << 8 | d[o + 1] & 0xff;
    }

    private int s16(int o) {
        return (short) u16(o);
    }

    private int u32(int o) {
        return (d[o] & 0xff) << 24 | (d[o + 1] & 0xff) << 16 | (d[o + 2] & 0xff) << 8 | d[o + 3] & 0xff;
    }

    private int table(String tag) {
        int count = u16(4);
        for (int i = 0; i < count; i++) {
            int rec = 12 + i * 16;
            if (rec + 16 > d.length) {
                break;
            }
            boolean match = true;
            for (int k = 0; k < 4; k++) {
                if ((d[rec + k] & 0xff) != tag.charAt(k)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                int off = u32(rec + 8);
                return off >= 0 && off < d.length ? off : -1;
            }
        }
        return -1;
    }

    private void readMetrics() {
        int head = table("head");
        if (head >= 0) {
            int u = u16(head + 18);
            if (u >= 16) {
                unitsPerEm = u;
            }
        }
        int hhea = table("hhea");
        if (hhea >= 0 && hhea + 36 <= d.length) {
            ascent = -s16(hhea + 4) / unitsPerEm;
            descent = -s16(hhea + 6) / unitsPerEm;
            numHMetrics = u16(hhea + 34);
        }
        hmtx = table("hmtx");
        int kern = table("kern");
        if (kern >= 0 && kern + 4 <= d.length && u16(kern) == 0) {
            int nTables = u16(kern + 2);
            int p = kern + 4;
            for (int t = 0; t < nTables && p + 6 <= d.length; t++) {
                int len = u16(p + 2);
                int coverage = u16(p + 4);
                // Horizontal, format 0, not cross-stream or minimum.
                if ((coverage & 0xff07) == 0x0001 && p + 14 <= d.length) {
                    int nPairs = u16(p + 6);
                    int q = p + 14;
                    for (int i = 0; i < nPairs && q + 6 <= d.length; i++, q += 6) {
                        kerning.put(Integer.valueOf(u16(q) << 16 | u16(q + 2)), Integer.valueOf(s16(q + 4)));
                    }
                }
                if (len <= 0) {
                    break;
                }
                p += len;
            }
        }
    }

    /// Ascent above the baseline, negative (up is -y), in em.
    public float getAscent() {
        return ascent;
    }

    /// Descent below the baseline, positive, in em.
    public float getDescent() {
        return descent;
    }

    /// The glyph for a Unicode code point, 0 (notdef) when the font has none.
    public int glyphFor(int codePoint) {
        return outlines.glyphForUnicode(codePoint);
    }

    /// The advance width of `glyph`, in em.
    public float advance(int glyph) {
        if (hmtx < 0 || numHMetrics <= 0) {
            return 0.5f;
        }
        int i = glyph < numHMetrics ? glyph : numHMetrics - 1;
        int o = hmtx + i * 4;
        if (i < 0 || o + 2 > d.length) {
            return 0f;
        }
        return u16(o) / unitsPerEm;
    }

    /// The pair adjustment between `left` and `right`, in em.
    public float kern(int left, int right) {
        if (kerning.isEmpty()) {
            return 0f;
        }
        Integer v = kerning.get(Integer.valueOf(left << 16 | right));
        return v == null ? 0f : v.intValue() / unitsPerEm;
    }

    /// The outline of `glyph` in em units, y down. Cached; never modify it.
    public RivePath glyphPath(int glyph) {
        Integer key = Integer.valueOf(glyph);
        RivePath p = paths.get(key);
        if (p == null) {
            p = new RivePath();
            try {
                outlines.outline(glyph, new Sink(p));
            } catch (RuntimeException ex) {
                // A malformed font must not take the frame down: the glyph
                // draws nothing, and the empty path is cached so it is not
                // parsed again every frame.
                p = new RivePath();
            }
            paths.put(key, p);
        }
        return p;
    }

    /// Converts the outline parser's y-up quadratic and cubic segments into a
    /// y-down `RivePath` of cubics.
    private static final class Sink implements GlyphSink {
        private final RivePath p;
        private float lx;
        private float ly;

        Sink(RivePath p) {
            this.p = p;
        }

        @Override
        public void moveTo(double x, double y) {
            lx = (float) x;
            ly = (float) -y;
            p.moveTo(lx, ly);
        }

        @Override
        public void lineTo(double x, double y) {
            lx = (float) x;
            ly = (float) -y;
            p.lineTo(lx, ly);
        }

        @Override
        public void quadTo(double cx, double cy, double x, double y) {
            float qx = (float) cx;
            float qy = (float) -cy;
            float ex = (float) x;
            float ey = (float) -y;
            p.cubicTo(lx + 2f / 3f * (qx - lx), ly + 2f / 3f * (qy - ly),
                    ex + 2f / 3f * (qx - ex), ey + 2f / 3f * (qy - ey), ex, ey);
            lx = ex;
            ly = ey;
        }

        @Override
        public void curveTo(double c1x, double c1y, double c2x, double c2y, double x, double y) {
            lx = (float) x;
            ly = (float) -y;
            p.cubicTo((float) c1x, (float) -c1y, (float) c2x, (float) -c2y, lx, ly);
        }

        @Override
        public void closePath() {
            p.close();
        }
    }
}
