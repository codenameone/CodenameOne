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
package com.codename1.backend.image;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// A scalable font read from the bytes of a TrueType file (`.ttf`, an
/// OpenType `.otf` with TrueType outlines, or one face of a `.ttc`
/// collection), drawn by [RasterGraphics#drawString(String, double, double)].
///
/// The font is pure Java and needs no platform font engine, so it renders the
/// same text on a device and on a server. It reads the `head`, `hhea`,
/// `maxp`, `cmap` (formats 4 and 12), `loca`, `glyf` (composite glyphs
/// included), `hmtx`, `kern` (format 0) and `name` tables. Glyphs are not
/// hinted: outlines are scaled exactly, as quadratic curves, and the
/// rasterizer's antialiasing smooths them. OpenType layout (`GSUB`, `GPOS`)
/// is not applied, so ligatures and `GPOS` kerning are absent, and fonts
/// with PostScript (`CFF`) outlines are refused with an `IOException`.
///
/// ```java
/// TrueTypeFont font = TrueTypeFont.createDefault(48);
/// RasterGraphics g = card.createGraphics();
/// g.setFont(font);
/// g.setColor(0xff202020);
/// g.drawString("Hello", 40, 40 + font.getAscent());
/// ```
///
/// [#createDefault(double)] is Aileron Regular, a font embedded in the
/// library, so text can be drawn without shipping a font file.
///
/// An instance keeps a bounded cache of glyph outlines and is not safe to use
/// from two threads at once; [#deriveFont(double)] is cheap and gives each
/// thread its own.
public final class TrueTypeFont implements RasterFont {
    private static final int CACHE_LIMIT = 512;
    private static final int MAX_COMPONENT_DEPTH = 8;

    private static final int OP_MOVE = 0;
    private static final int OP_LINE = 1;
    private static final int OP_QUAD = 2;
    private static final int OP_CLOSE = 3;

    private final Face face;
    private final double size;
    private final double scale;
    private final Map<Integer, Glyph> cache = new HashMap<Integer, Glyph>();

    /// The parsed tables, shared by every size of one face.
    private static final class Face {
        byte[] d;
        int unitsPerEm;
        int locaLong;
        int numGlyphs;
        int ascender;
        int descender;
        int lineGap;
        int numHMetrics;
        int hmtx;
        int loca;
        int locaLen;
        int glyf;
        int glyfLen;
        int cmap12 = -1;
        int cmap4 = -1;
        boolean symbol;
        int kernPairs;
        int kernStart;
        String family = "";
        String style = "";
    }

    /// One glyph in font units: path commands and its control box.
    private static final class Glyph {
        float[] cmds = new float[0];
        int len;
        float xMin;
        float yMin;
        float xMax;
        float yMax;
        boolean empty = true;

        void add(float v) {
            if (len == cmds.length) {
                float[] n = new float[Math.max(16, len * 2)];
                System.arraycopy(cmds, 0, n, 0, len);
                cmds = n;
            }
            cmds[len++] = v;
        }

        void point(int op, float x, float y) {
            add(op);
            add(x);
            add(y);
            include(x, y);
        }

        void include(float x, float y) {
            if (empty) {
                xMin = x;
                xMax = x;
                yMin = y;
                yMax = y;
                empty = false;
            } else {
                xMin = Math.min(xMin, x);
                xMax = Math.max(xMax, x);
                yMin = Math.min(yMin, y);
                yMax = Math.max(yMax, y);
            }
        }
    }

    private TrueTypeFont(Face face, double size) {
        if (Double.isNaN(size) || size <= 0) {
            throw new IllegalArgumentException("font size must be positive: " + size);
        }
        this.face = face;
        this.size = size;
        this.scale = size / face.unitsPerEm;
    }

    /// Reads a font from the bytes of a TrueType or OpenType file.
    ///
    /// #### Parameters
    ///
    /// - `data`: the file's contents; the array is kept, not copied
    /// - `size`: the size in pixels, the height of one em
    ///
    /// #### Returns
    ///
    /// the font
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the data is not a font this class can read
    public static TrueTypeFont load(byte[] data, double size) throws IOException {
        return load(data, 0, size);
    }

    /// Reads one face of a font file, which for a `.ttc` collection may be
    /// any of its faces.
    ///
    /// #### Parameters
    ///
    /// - `data`: the file's contents; the array is kept, not copied
    /// - `index`: the face in a collection, 0 for a single font
    /// - `size`: the size in pixels, the height of one em
    ///
    /// #### Returns
    ///
    /// the font
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the data is not a font this class can read
    public static TrueTypeFont load(byte[] data, int index, double size) throws IOException {
        return new TrueTypeFont(parse(data, index), size);
    }

    /// Returns Aileron Regular, the scalable font embedded in the library, at
    /// `size` pixels. It is the font Pillow's `ImageFont.load_default()`
    /// returns, and it covers printable ASCII.
    ///
    /// #### Parameters
    ///
    /// - `size`: the size in pixels
    ///
    /// #### Returns
    ///
    /// the font
    public static TrueTypeFont createDefault(double size) {
        try {
            return load(DefaultFontData.aileron(), size);
        } catch (IOException e) {
            throw corrupt(e);
        }
    }

    private static IllegalStateException corrupt(IOException e) {
        IllegalStateException x = new IllegalStateException("embedded font is corrupt: " + e.getMessage());
        x.initCause(e);
        return x;
    }

    /// Returns the bytes of the embedded Aileron Regular font file, a new
    /// copy on every call, for code that wants the file itself.
    ///
    /// #### Returns
    ///
    /// the `.ttf` file
    public static byte[] getDefaultFontData() {
        try {
            return DefaultFontData.aileron();
        } catch (IOException e) {
            throw corrupt(e);
        }
    }

    /// Returns this face at another size. The parsed tables are shared; the
    /// glyph cache is not.
    ///
    /// #### Parameters
    ///
    /// - `newSize`: the size in pixels
    ///
    /// #### Returns
    ///
    /// the font at `newSize`
    public TrueTypeFont deriveFont(double newSize) {
        return new TrueTypeFont(face, newSize);
    }

    /// Returns the size in pixels: the height of one em.
    ///
    /// #### Returns
    ///
    /// the size
    public double getSize() {
        return size;
    }

    /// Returns the family name from the `name` table, such as `Aileron`.
    ///
    /// #### Returns
    ///
    /// the family, empty if the font names none
    public String getFamilyName() {
        return face.family;
    }

    /// Returns the style name from the `name` table, such as `Regular`.
    ///
    /// #### Returns
    ///
    /// the style, empty if the font names none
    public String getStyleName() {
        return face.style;
    }

    /// Returns the font units per em: glyph coordinates are scaled by
    /// `getSize() / getUnitsPerEm()`.
    ///
    /// #### Returns
    ///
    /// the units per em
    public int getUnitsPerEm() {
        return face.unitsPerEm;
    }

    /// Returns the number of glyphs in the font.
    ///
    /// #### Returns
    ///
    /// the glyph count
    public int getGlyphCount() {
        return face.numGlyphs;
    }

    /// Returns the ascender of the `hhea` table, in font units.
    ///
    /// #### Returns
    ///
    /// the ascender, positive above the baseline
    public int getAscenderUnits() {
        return face.ascender;
    }

    /// Returns the descender of the `hhea` table, in font units.
    ///
    /// #### Returns
    ///
    /// the descender, negative below the baseline
    public int getDescenderUnits() {
        return face.descender;
    }

    /// Returns the distance from the baseline to the top of the line, in pixels.
    ///
    /// #### Returns
    ///
    /// the ascent
    @Override
    public double getAscent() {
        return face.ascender * scale;
    }

    /// Returns the distance from the baseline to the bottom of the line, in pixels.
    ///
    /// #### Returns
    ///
    /// the descent, positive
    @Override
    public double getDescent() {
        return -face.descender * scale;
    }

    /// Returns the extra space the font asks for between lines, in pixels.
    ///
    /// #### Returns
    ///
    /// the line gap
    public double getLineGap() {
        return face.lineGap * scale;
    }

    /// Returns the glyph that shows a character, from the `cmap` table.
    ///
    /// #### Parameters
    ///
    /// - `codePoint`: a Unicode code point
    ///
    /// #### Returns
    ///
    /// the glyph index, 0 (the missing glyph) when the font has none
    public int glyphIndex(int codePoint) {
        Face f = face;
        int g = 0;
        if (f.cmap12 >= 0) {
            g = lookup12(f, codePoint);
        }
        if (g == 0 && f.cmap4 >= 0 && codePoint >= 0 && codePoint <= 0xffff) {
            g = lookup4(f, codePoint);
            if (g == 0 && f.symbol && codePoint < 0x100) {
                g = lookup4(f, codePoint | 0xf000);
            }
        }
        return g < f.numGlyphs ? g : 0;
    }

    /// Returns whether the font has a glyph for a character.
    ///
    /// #### Parameters
    ///
    /// - `codePoint`: a Unicode code point
    ///
    /// #### Returns
    ///
    /// true if the character maps to a glyph other than the missing glyph
    public boolean canDisplay(int codePoint) {
        return glyphIndex(codePoint) != 0;
    }

    /// Returns a glyph's advance width from the `hmtx` table, in font units.
    ///
    /// #### Parameters
    ///
    /// - `glyph`: the glyph index
    ///
    /// #### Returns
    ///
    /// the advance
    public int glyphAdvance(int glyph) {
        Face f = face;
        int n = f.numHMetrics;
        if (n <= 0 || glyph < 0) {
            return 0;
        }
        int i = glyph < n ? glyph : n - 1;
        return u16(f.d, f.hmtx + i * 4);
    }

    /// Returns the kerning between two glyphs from the `kern` table, in font
    /// units, to add to the advance of the first.
    ///
    /// #### Parameters
    ///
    /// - `left`: the first glyph index
    /// - `right`: the glyph that follows it
    ///
    /// #### Returns
    ///
    /// the adjustment, 0 when the pair is not kerned
    public int kerning(int left, int right) {
        Face f = face;
        int lo = 0;
        int hi = f.kernPairs - 1;
        long key = ((long) left << 16) | right;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int p = f.kernStart + mid * 6;
            long k = ((long) u16(f.d, p) << 16) | u16(f.d, p + 2);
            if (k < key) {
                lo = mid + 1;
            } else if (k > key) {
                hi = mid - 1;
            } else {
                return s16(f.d, p + 4);
            }
        }
        return 0;
    }

    /// Returns a glyph's control box -- the bounds of every point of its
    /// outline, on and off the curve -- in font units, y up.
    ///
    /// #### Parameters
    ///
    /// - `glyph`: the glyph index
    ///
    /// #### Returns
    ///
    /// `{xMin, yMin, xMax, yMax}`, or null for a glyph with no outline
    public double[] glyphBounds(int glyph) {
        Glyph g = glyph(glyph);
        if (g.empty) {
            return null;
        }
        return new double[] {g.xMin, g.yMin, g.xMax, g.yMax};
    }

    /// Appends a glyph's outline to a path, scaled to this size, with its
    /// origin on the baseline at `(x, y)`.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path to append to
    /// - `glyph`: the glyph index
    /// - `x`: the pen position
    /// - `y`: the baseline
    public void appendGlyph(RasterPath path, int glyph, double x, double y) {
        Glyph g = glyph(glyph);
        float[] c = g.cmds;
        double s = scale;
        int i = 0;
        while (i < g.len) {
            int op = (int) c[i];
            if (op == OP_MOVE) {
                path.moveTo(x + c[i + 1] * s, y - c[i + 2] * s);
                i += 3;
            } else if (op == OP_LINE) {
                path.lineTo(x + c[i + 1] * s, y - c[i + 2] * s);
                i += 3;
            } else if (op == OP_QUAD) {
                path.quadTo(x + c[i + 1] * s, y - c[i + 2] * s, x + c[i + 3] * s, y - c[i + 4] * s);
                i += 5;
            } else {
                path.closePath();
                i++;
            }
        }
    }

    /// Returns the outline of `text` with its baseline starting at `(x, y)`,
    /// glyph after glyph by their advances and `kern` table kerning.
    ///
    /// #### Parameters
    ///
    /// - `text`: the string
    /// - `x`: left end of the baseline
    /// - `y`: the baseline
    ///
    /// #### Returns
    ///
    /// the outline, filled with the non-zero rule
    @Override
    public RasterPath getOutline(String text, double x, double y) {
        RasterPath p = new RasterPath();
        layout(text, p, x, y);
        return p;
    }

    /// Returns the advance width of `text`: the sum of its glyph advances and
    /// `kern` table kerning, unrounded.
    ///
    /// #### Parameters
    ///
    /// - `text`: the string
    ///
    /// #### Returns
    ///
    /// the width in pixels
    @Override
    public double stringWidth(String text) {
        return layout(text, null, 0, 0) * scale;
    }

    /// Lays out `text`, appending its glyphs to `path` when it is not null,
    /// and returns the total advance in font units.
    private int layout(String text, RasterPath path, double x, double y) {
        int pen = 0;
        int prev = -1;
        int n = text.length();
        int i = 0;
        while (i < n) {
            int cp = text.charAt(i++);
            if (cp >= 0xd800 && cp < 0xdc00 && i < n) {
                int lo = text.charAt(i);
                if (lo >= 0xdc00 && lo < 0xe000) {
                    cp = ((cp - 0xd800) << 10) + (lo - 0xdc00) + 0x10000;
                    i++;
                }
            }
            int g = glyphIndex(cp);
            if (prev >= 0) {
                pen += kerning(prev, g);
            }
            if (path != null) {
                appendGlyph(path, g, x + pen * scale, y);
            }
            pen += glyphAdvance(g);
            prev = g;
        }
        return pen;
    }

    private Glyph glyph(int index) {
        Integer key = Integer.valueOf(index);
        Glyph g = cache.get(key);
        if (g == null) {
            g = new Glyph();
            if (index >= 0 && index < face.numGlyphs) {
                decode(face, index, g, new double[] {1, 0, 0, 1, 0, 0}, 0);
            }
            if (cache.size() >= CACHE_LIMIT) {
                cache.clear();
            }
            cache.put(key, g);
        }
        return g;
    }

    /// Decodes glyph `index` into `out`, transformed by `m = {a, b, c, d, e, f}`:
    /// `x' = a x + c y + e`, `y' = b x + d y + f`.
    private static void decode(Face f, int index, Glyph out, double[] m, int depth) {
        byte[] data = f.d;
        int start;
        int end;
        if (f.locaLong != 0) {
            if (index * 4 + 8 > f.locaLen) {
                return;
            }
            start = (int) u32(data, f.loca + index * 4);
            end = (int) u32(data, f.loca + index * 4 + 4);
        } else {
            if (index * 2 + 4 > f.locaLen) {
                return;
            }
            start = u16(data, f.loca + index * 2) * 2;
            end = u16(data, f.loca + index * 2 + 2) * 2;
        }
        if (end - start < 10 || start < 0 || end > f.glyfLen) {
            return;
        }
        int p = f.glyf + start;
        int limit = f.glyf + end;
        int contours = s16(data, p);
        if (contours >= 0) {
            simple(data, p, limit, contours, out, m);
        } else if (depth < MAX_COMPONENT_DEPTH) {
            composite(f, data, p + 10, limit, out, m, depth);
        }
    }

    private static void simple(byte[] data, int p, int limit, int contours, Glyph out, double[] m) {
        int q = p + 10;
        if (contours == 0 || q + contours * 2 + 2 > limit) {
            return;
        }
        int[] ends = new int[contours];
        for (int i = 0; i < contours; i++) {
            ends[i] = u16(data, q);
            q += 2;
        }
        int count = ends[contours - 1] + 1;
        q += 2 + u16(data, q);
        byte[] flags = new byte[count];
        int i = 0;
        while (i < count) {
            if (q >= limit) {
                return;
            }
            int fl = data[q++] & 0xff;
            flags[i++] = (byte) fl;
            if ((fl & 8) != 0) {
                if (q >= limit) {
                    return;
                }
                int rep = data[q++] & 0xff;
                while (rep > 0 && i < count) {
                    flags[i++] = (byte) fl;
                    rep--;
                }
            }
        }
        int[] xs = new int[count];
        int[] ys = new int[count];
        q = coords(data, q, limit, flags, xs, 2, 16);
        if (q < 0 || coords(data, q, limit, flags, ys, 4, 32) < 0) {
            return;
        }
        float[] px = new float[count];
        float[] py = new float[count];
        for (i = 0; i < count; i++) {
            px[i] = (float) (m[0] * xs[i] + m[2] * ys[i] + m[4]);
            py[i] = (float) (m[1] * xs[i] + m[3] * ys[i] + m[5]);
        }
        int s = 0;
        for (int k = 0; k < contours; k++) {
            int en = ends[k];
            if (en < s || en >= count) {
                return;
            }
            contour(out, px, py, flags, s, en);
            s = en + 1;
        }
    }

    private static int coords(byte[] data, int start, int limit, byte[] flags, int[] v, int shortBit, int sameBit) {
        int q = start;
        int cur = 0;
        for (int i = 0; i < flags.length; i++) {
            int fl = flags[i];
            if ((fl & shortBit) != 0) {
                if (q >= limit) {
                    return -1;
                }
                int dv = data[q++] & 0xff;
                cur += (fl & sameBit) != 0 ? dv : -dv;
            } else if ((fl & sameBit) == 0) {
                if (q + 2 > limit) {
                    return -1;
                }
                cur += s16(data, q);
                q += 2;
            }
            v[i] = cur;
        }
        return q;
    }

    /// Emits one contour as lines and quadratic curves, inserting the
    /// on-curve point implied between two consecutive off-curve points.
    private static void contour(Glyph out, float[] px, float[] py, byte[] flags, int s, int e) {
        int n = e - s + 1;
        int first = -1;
        for (int i = 0; i < n; i++) {
            if ((flags[s + i] & 1) != 0) {
                first = i;
                break;
            }
        }
        float sx;
        float sy;
        int begin;
        int steps;
        if (first >= 0) {
            sx = px[s + first];
            sy = py[s + first];
            begin = first + 1;
            steps = n - 1;
        } else {
            sx = (px[s] + px[e]) / 2;
            sy = (py[s] + py[e]) / 2;
            begin = 0;
            steps = n;
        }
        out.point(OP_MOVE, sx, sy);
        boolean pending = false;
        float cx = 0;
        float cy = 0;
        for (int k = 0; k < steps; k++) {
            int i = s + (begin + k) % n;
            float x = px[i];
            float y = py[i];
            if ((flags[i] & 1) != 0) {
                if (pending) {
                    quad(out, cx, cy, x, y);
                    pending = false;
                } else {
                    out.point(OP_LINE, x, y);
                }
            } else {
                if (pending) {
                    quad(out, cx, cy, (cx + x) / 2, (cy + y) / 2);
                }
                cx = x;
                cy = y;
                pending = true;
            }
        }
        if (pending) {
            quad(out, cx, cy, sx, sy);
        }
        out.add(OP_CLOSE);
    }

    private static void quad(Glyph out, float cx, float cy, float x, float y) {
        out.add(OP_QUAD);
        out.add(cx);
        out.add(cy);
        out.add(x);
        out.add(y);
        out.include(cx, cy);
        out.include(x, y);
    }

    private static void composite(Face f, byte[] data, int start, int limit, Glyph out, double[] m, int depth) {
        int q = start;
        while (q + 4 <= limit) {
            int flags = u16(data, q);
            int child = u16(data, q + 2);
            q += 4;
            double dx;
            double dy;
            int argLen = (flags & 1) != 0 ? 4 : 2;
            if (q + argLen > limit) {
                return;
            }
            if (argLen == 4) {
                dx = s16(data, q);
                dy = s16(data, q + 2);
            } else {
                dx = data[q];
                dy = data[q + 1];
            }
            q += argLen;
            if ((flags & 2) == 0) {
                // Point-matched placement (anchor points instead of an
                // offset) is rare outside old CJK fonts; the component is
                // placed at the origin rather than refused.
                dx = 0;
                dy = 0;
            }
            double ca = 1;
            double cb = 0;
            double cc = 0;
            double cd = 1;
            if ((flags & 8) != 0) {
                if (q + 2 > limit) {
                    return;
                }
                ca = f2dot14(data, q);
                cd = ca;
                q += 2;
            } else if ((flags & 0x40) != 0) {
                if (q + 4 > limit) {
                    return;
                }
                ca = f2dot14(data, q);
                cd = f2dot14(data, q + 2);
                q += 4;
            } else if ((flags & 0x80) != 0) {
                if (q + 8 > limit) {
                    return;
                }
                ca = f2dot14(data, q);
                cb = f2dot14(data, q + 2);
                cc = f2dot14(data, q + 4);
                cd = f2dot14(data, q + 6);
                q += 8;
            }
            // parent(child(p)), child(p) = (ca x + cc y + dx, cb x + cd y + dy)
            double[] n = new double[] {
                m[0] * ca + m[2] * cb,
                m[1] * ca + m[3] * cb,
                m[0] * cc + m[2] * cd,
                m[1] * cc + m[3] * cd,
                m[0] * dx + m[2] * dy + m[4],
                m[1] * dx + m[3] * dy + m[5]
            };
            decode(f, child, out, n, depth + 1);
            if ((flags & 0x20) == 0) {
                return;
            }
        }
    }

    private static double f2dot14(byte[] data, int p) {
        return s16(data, p) / 16384.0;
    }

    private static int lookup12(Face f, int cp) {
        byte[] d = f.d;
        int t = f.cmap12;
        int lo = 0;
        int hi = (int) u32(d, t + 12) - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int g = t + 16 + mid * 12;
            long first = u32(d, g);
            long last = u32(d, g + 4);
            if (cp < first) {
                hi = mid - 1;
            } else if (cp > last) {
                lo = mid + 1;
            } else {
                return (int) (u32(d, g + 8) + cp - first);
            }
        }
        return 0;
    }

    private static int lookup4(Face f, int cp) {
        byte[] d = f.d;
        int t = f.cmap4;
        int segX2 = u16(d, t + 6);
        int ends = t + 14;
        int starts = ends + segX2 + 2;
        int deltas = starts + segX2;
        int offsets = deltas + segX2;
        if (offsets + segX2 > d.length) {
            return 0;
        }
        int lo = 0;
        int hi = segX2 / 2 - 1;
        int seg = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (cp > u16(d, ends + mid * 2)) {
                lo = mid + 1;
            } else if (cp < u16(d, starts + mid * 2)) {
                hi = mid - 1;
            } else {
                seg = mid;
                lo = hi + 1;
            }
        }
        if (seg < 0) {
            return 0;
        }
        int startCode = u16(d, starts + seg * 2);
        int delta = u16(d, deltas + seg * 2);
        int ro = u16(d, offsets + seg * 2);
        if (ro == 0) {
            return (cp + delta) & 0xffff;
        }
        int gp = offsets + seg * 2 + ro + (cp - startCode) * 2;
        if (gp + 2 > d.length) {
            return 0;
        }
        int g = u16(d, gp);
        return g == 0 ? 0 : (g + delta) & 0xffff;
    }

    private static Face parse(byte[] d, int index) throws IOException {
        if (d == null || d.length < 12) {
            throw new IOException("not a font: too short");
        }
        int base = 0;
        long tag = u32(d, 0);
        if (tag == 0x74746366L) {
            // 'ttcf': a collection of faces
            check(d, 0, 12);
            int n = (int) u32(d, 8);
            if (index < 0 || index >= n) {
                throw new IOException("font index " + index + " out of range; the collection has " + n);
            }
            check(d, 12 + index * 4, 4);
            base = (int) u32(d, 12 + index * 4);
            check(d, base, 12);
            tag = u32(d, base);
        } else if (index != 0) {
            throw new IOException("font index " + index + " out of range; the file has one face");
        }
        if (tag == 0x4f54544fL) {
            // 'OTTO': OpenType with CFF outlines
            throw new IOException("fonts with PostScript (CFF) outlines are not supported;"
                    + " use a TrueType-flavoured font (.ttf)");
        }
        if (tag != 0x00010000L && tag != 0x74727565L) {
            throw new IOException("not a TrueType font");
        }
        int numTables = u16(d, base + 4);
        check(d, base + 12, numTables * 16);
        Face f = new Face();
        f.d = d;
        int[] tables = new int[8];
        int[] lengths = new int[8];
        for (int i = 0; i < tables.length; i++) {
            tables[i] = -1;
        }
        for (int i = 0; i < numTables; i++) {
            int r = base + 12 + i * 16;
            int which = tableSlot(u32(d, r));
            if (which >= 0) {
                int off = (int) u32(d, r + 8);
                int len = (int) u32(d, r + 12);
                check(d, off, len);
                tables[which] = off;
                lengths[which] = len;
            }
        }
        int head = tables[0];
        int hhea = tables[1];
        int maxp = tables[2];
        int cmap = tables[3];
        f.hmtx = tables[4];
        f.loca = tables[5];
        f.locaLen = lengths[5];
        f.glyf = tables[6];
        f.glyfLen = lengths[6];
        if (head < 0 || hhea < 0 || maxp < 0 || cmap < 0 || f.hmtx < 0 || f.loca < 0 || f.glyf < 0) {
            throw new IOException("not a TrueType font: a required table is missing");
        }
        check(d, head, 54);
        check(d, hhea, 36);
        check(d, maxp, 6);
        f.unitsPerEm = u16(d, head + 18);
        if (f.unitsPerEm < 16 || f.unitsPerEm > 16384) {
            throw new IOException("bad unitsPerEm " + f.unitsPerEm);
        }
        f.locaLong = s16(d, head + 50);
        f.ascender = s16(d, hhea + 4);
        f.descender = s16(d, hhea + 6);
        f.lineGap = s16(d, hhea + 8);
        f.numHMetrics = u16(d, hhea + 34);
        f.numGlyphs = u16(d, maxp + 4);
        if (f.numHMetrics * 4 > lengths[4]) {
            throw new IOException("hmtx table is too short");
        }
        parseCmap(f, cmap);
        if (f.cmap12 < 0 && f.cmap4 < 0) {
            throw new IOException("the font has no Unicode cmap (format 4 or 12)");
        }
        if (tables[7] >= 0) {
            parseKern(f, tables[7], lengths[7]);
        }
        parseName(f, d, base, numTables);
        return f;
    }

    private static int tableSlot(long tag) {
        if (tag == 0x68656164L) {
            return 0; // head
        }
        if (tag == 0x68686561L) {
            return 1; // hhea
        }
        if (tag == 0x6d617870L) {
            return 2; // maxp
        }
        if (tag == 0x636d6170L) {
            return 3; // cmap
        }
        if (tag == 0x686d7478L) {
            return 4; // hmtx
        }
        if (tag == 0x6c6f6361L) {
            return 5; // loca
        }
        if (tag == 0x676c7966L) {
            return 6; // glyf
        }
        if (tag == 0x6b65726eL) {
            return 7; // kern
        }
        return -1;
    }

    private static void parseCmap(Face f, int cmap) throws IOException {
        byte[] d = f.d;
        check(d, cmap, 4);
        int n = u16(d, cmap + 2);
        check(d, cmap + 4, n * 8);
        int rank4 = 0;
        for (int i = 0; i < n; i++) {
            int r = cmap + 4 + i * 8;
            int platform = u16(d, r);
            int encoding = u16(d, r + 2);
            int sub = cmap + (int) u32(d, r + 4);
            check(d, sub, 2);
            int format = u16(d, sub);
            boolean unicode = platform == 0 || (platform == 3 && (encoding == 1 || encoding == 10));
            if (format == 12 && unicode && f.cmap12 < 0) {
                check(d, sub, 16);
                check(d, sub + 16, (int) u32(d, sub + 12) * 12);
                f.cmap12 = sub;
            } else if (format == 4) {
                check(d, sub, 14);
                check(d, sub, u16(d, sub + 2));
                int rank = unicode ? 2 : (platform == 3 && encoding == 0 ? 1 : 0);
                if (rank > rank4) {
                    rank4 = rank;
                    f.cmap4 = sub;
                }
            }
        }
        f.symbol = rank4 == 1;
    }

    private static void parseKern(Face f, int kern, int len) {
        byte[] d = f.d;
        if (len < 4 || u16(d, kern) != 0) {
            // Apple's version 1 table, or an empty one: no kerning
            return;
        }
        int n = u16(d, kern + 2);
        int p = kern + 4;
        int end = kern + len;
        for (int i = 0; i < n && p + 6 <= end; i++) {
            int subLen = u16(d, p + 2);
            int coverage = u16(d, p + 4);
            boolean horizontal = (coverage & 1) != 0;
            boolean plain = (coverage & 6) == 0;
            if ((coverage >> 8) == 0 && horizontal && plain && p + 14 <= end) {
                f.kernPairs = Math.min(u16(d, p + 6), (end - (p + 14)) / 6);
                f.kernStart = p + 14;
                return;
            }
            if (subLen < 6) {
                return;
            }
            p += subLen;
        }
    }

    private static void parseName(Face f, byte[] d, int base, int numTables) {
        int name = -1;
        int len = 0;
        for (int i = 0; i < numTables; i++) {
            int r = base + 12 + i * 16;
            if (u32(d, r) == 0x6e616d65L) {
                name = (int) u32(d, r + 8);
                len = (int) u32(d, r + 12);
            }
        }
        if (name < 0 || len < 6 || name > d.length - len) {
            return;
        }
        int count = u16(d, name + 2);
        int strings = name + u16(d, name + 4);
        int end = name + len;
        String[] best = new String[3];
        int[] rank = new int[3];
        for (int i = 0; i < count && name + 6 + i * 12 + 12 <= end; i++) {
            int r = name + 6 + i * 12;
            int platform = u16(d, r);
            int encoding = u16(d, r + 2);
            int id = u16(d, r + 6);
            int l = u16(d, r + 8);
            int o = strings + u16(d, r + 10);
            if ((id != 1 && id != 2) || o + l > end) {
                continue;
            }
            int score;
            if (platform == 3 && (encoding == 0 || encoding == 1 || encoding == 10)) {
                score = u16(d, r + 4) == 0x409 ? 4 : 3;
            } else if (platform == 0) {
                score = 2;
            } else if (platform == 1 && encoding == 0) {
                score = 1;
            } else {
                continue;
            }
            if (score <= rank[id]) {
                continue;
            }
            StringBuilder sb = new StringBuilder(l);
            if (score == 1) {
                for (int k = 0; k < l; k++) {
                    sb.append((char) (d[o + k] & 0xff));
                }
            } else {
                for (int k = 0; k + 1 < l; k += 2) {
                    sb.append((char) u16(d, o + k));
                }
            }
            rank[id] = score;
            best[id] = sb.toString();
        }
        if (best[1] != null) {
            f.family = best[1];
        }
        if (best[2] != null) {
            f.style = best[2];
        }
    }

    private static void check(byte[] d, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off > d.length - len) {
            throw new IOException("truncated font data");
        }
    }

    private static int u16(byte[] d, int p) {
        return ((d[p] & 0xff) << 8) | (d[p + 1] & 0xff);
    }

    private static int s16(byte[] d, int p) {
        return (short) u16(d, p);
    }

    private static long u32(byte[] d, int p) {
        return ((long) u16(d, p) << 16) | u16(d, p + 2);
    }
}
