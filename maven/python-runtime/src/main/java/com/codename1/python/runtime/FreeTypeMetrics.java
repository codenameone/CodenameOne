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
package com.codename1.python.runtime;

import com.codename1.image.RasterGraphics;
import com.codename1.image.RasterImage;
import com.codename1.image.RasterPath;
import com.codename1.image.TrueTypeFont;

/// Pillow's basic text layout over a [TrueTypeFont], in FreeType's 26.6
/// fixed point, so `getlength`, `getbbox`, `getmetrics` and the anchors give
/// the integers Pillow gives.
///
/// Pillow loads glyphs with `FT_LOAD_DEFAULT`, so FreeType rounds every
/// advance to a whole pixel, the ascender up and the descender down. Glyph
/// boxes come from the control box of the scaled outline, floored and ceiled
/// to pixels (`FT_GLYPH_BBOX_PIXELS`). Outlines are not hinted here; a font
/// with TrueType instructions can therefore differ from FreeType by a pixel
/// at the edge of a box, never in an advance.
///
/// Kerning follows Pillow's `text_layout_fallback`, which adds the pixel
/// count of a `kern` pair to an advance kept in 64ths of a pixel -- a pair
/// moves the pen by a 64th per pixel of kerning. That is what Pillow does,
/// so it is what this does.
final class FreeTypeMetrics {
    final TrueTypeFont font;
    private final long scale;
    final long ascender;
    final long descender;

    FreeTypeMetrics(TrueTypeFont font) {
        this.font = font;
        long size26 = (long) (font.getSize() * 64);
        int upem = font.getUnitsPerEm();
        this.scale = ((size26 << 16) + upem / 2) / upem;
        this.ascender = ceil(mulFix(font.getAscenderUnits(), scale));
        this.descender = floor(mulFix(font.getDescenderUnits(), scale));
    }

    static long mulFix(long a, long b) {
        long p = a * b;
        return p >= 0 ? (p + 0x8000) >> 16 : -((-p + 0x8000) >> 16);
    }

    static long floor(long v) {
        return v & -64;
    }

    static long ceil(long v) {
        return (v + 63) & -64;
    }

    static long round(long v) {
        return (v + 32) & -64;
    }

    /// Pillow's `PIXEL`: a 26.6 value rounded to whole pixels.
    static int pixel(long v) {
        return (int) (((v + 32) & -64) >> 6);
    }

    /// Text layout: glyph indices and advances in 26.6.
    static final class Layout {
        final int[] glyphs;
        final long[] advances;
        int count;
        long position;

        Layout(int capacity) {
            glyphs = new int[capacity];
            advances = new long[capacity];
        }
    }

    /// Returns whether `anchor` is one of Pillow's horizontal anchors (null
    /// meaning the default, `la`).
    static boolean validAnchor(String anchor) {
        return anchor == null || (anchor.length() == 2 && "lmr".indexOf(anchor.charAt(0)) >= 0
                && "atmsbd".indexOf(anchor.charAt(1)) >= 0);
    }

    Layout layout(String text) {
        int n = text.length();
        Layout l = new Layout(n);
        int last = 0;
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
            int g = font.glyphIndex(cp);
            int k = l.count;
            if (last != 0 && g != 0) {
                long delta = round(mulFix(font.kerning(last, g), scale));
                l.advances[k - 1] += pixel(delta);
            }
            l.glyphs[k] = g;
            l.advances[k] = round(mulFix(font.glyphAdvance(g), scale));
            l.count++;
            last = g;
        }
        for (int k = 0; k < l.count; k++) {
            l.position += l.advances[k];
        }
        return l;
    }

    long length(String text) {
        return layout(text).position;
    }

    /// Returns `{width, height, xOffset, yOffset, xMin, yMax}` as Pillow's
    /// `bounding_box_and_anchors` computes them for horizontal text; the
    /// anchor must pass [#validAnchor(String)].
    int[] box(Layout l, String anchor) {
        long position = 0;
        int xMin = 0;
        int xMax = 0;
        int yMin = 0;
        int yMax = 0;
        for (int i = 0; i < l.count; i++) {
            int px = pixel(position);
            position += l.advances[i];
            int advanced = pixel(position);
            if (advanced > xMax) {
                xMax = advanced;
            }
            double[] b = font.glyphBounds(l.glyphs[i]);
            if (b == null) {
                continue;
            }
            int bx0 = (int) (floor(scaled(b[0])) >> 6) + px;
            int by0 = (int) (floor(scaled(b[1])) >> 6);
            int bx1 = (int) (ceil(scaled(b[2])) >> 6) + px;
            int by1 = (int) (ceil(scaled(b[3])) >> 6);
            xMax = Math.max(xMax, bx1);
            xMin = Math.min(xMin, bx0);
            yMax = Math.max(yMax, by1);
            yMin = Math.min(yMin, by0);
        }
        String a = anchor == null ? "la" : anchor;
        int xAnchor;
        int yAnchor;
        switch (a.charAt(0)) {
            case 'l':
                xAnchor = 0;
                break;
            case 'm':
                xAnchor = l.count == 0 ? 0 : pixel(l.position / 2);
                break;
            default:
                xAnchor = l.count == 0 ? 0 : pixel(l.position);
                break;
        }
        switch (a.charAt(1)) {
            case 'a':
                yAnchor = pixel(ascender);
                break;
            case 't':
                yAnchor = yMax;
                break;
            case 'm':
                yAnchor = pixel((ascender + descender) / 2);
                break;
            case 's':
                yAnchor = 0;
                break;
            case 'b':
                yAnchor = yMin;
                break;
            default:
                yAnchor = pixel(descender);
                break;
        }
        if (l.count == 0) {
            xAnchor = 0;
            yAnchor = 0;
        }
        return new int[] {xMax - xMin, yMax - yMin, xMin - xAnchor, yAnchor - yMax, xMin, yMax};
    }

    private long scaled(double units) {
        return mulFix(Math.round(units), scale);
    }

    /// Renders the layout as an 8-bit coverage mask of `box`'s size, the
    /// glyph origins at Pillow's pixel pen positions.
    byte[] mask(Layout l, int[] box, boolean mono) {
        int w = box[0];
        int h = box[1];
        byte[] out = new byte[w * h];
        if (w <= 0 || h <= 0) {
            return out;
        }
        RasterImage img = new RasterImage(w, h);
        RasterGraphics g = img.createGraphics();
        g.setAntiAlias(!mono);
        g.setColor(0xffffffff);
        RasterPath path = new RasterPath();
        // the mask's left edge is xMin, its top edge yMax above the baseline
        long position = 0;
        for (int i = 0; i < l.count; i++) {
            int px = pixel(position);
            position += l.advances[i];
            font.appendGlyph(path, l.glyphs[i], px - box[4], box[5]);
        }
        g.fill(path);
        int[] px = img.getPixels();
        for (int i = 0; i < out.length; i++) {
            int a = px[i] >>> 24;
            out[i] = (byte) (mono ? (a >= 128 ? 255 : 0) : a);
        }
        return out;
    }
}
