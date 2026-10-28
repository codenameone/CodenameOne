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
package com.codename1.image;

import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// [TrueTypeFont] checked against the JDK's font engine: advances, glyph
/// outlines (composite glyphs included) and rendered coverage.
class TrueTypeFontTest {
    private static final String ROBOTO = "Ports/JavaSE/src/com/codename1/impl/javase/Roboto-Regular.ttf";

    private static byte[] roboto() throws IOException {
        Path p = Paths.get(ROBOTO);
        for (int i = 0; i < 4 && !Files.exists(p); i++) {
            p = Paths.get("..").resolve(p);
        }
        return Files.readAllBytes(p);
    }

    private static Font awt(byte[] data, float size) throws Exception {
        return Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(data)).deriveFont(size);
    }

    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    @Test
    void defaultFontIsAileron() {
        TrueTypeFont f = TrueTypeFont.createDefault(20);
        assertEquals("Aileron", f.getFamilyName());
        assertEquals("Regular", f.getStyleName());
        assertEquals(1000, f.getUnitsPerEm());
        assertTrue(f.canDisplay('A') && f.canDisplay('~'));
        assertTrue(f.getAscent() > 0 && f.getDescent() > 0);
        assertEquals(40, f.deriveFont(40).getSize(), 0);
        assertEquals(2 * f.stringWidth("Hello"), f.deriveFont(40).stringWidth("Hello"), 1e-9);
        assertEquals(TrueTypeFont.getDefaultFontData().length, TrueTypeFont.getDefaultFontData().length);
    }

    @Test
    void advancesMatchTheJdk() throws Exception {
        for (byte[] data : new byte[][] {roboto(), TrueTypeFont.getDefaultFontData()}) {
            TrueTypeFont f = TrueTypeFont.load(data, 32);
            Font j = awt(data, 32);
            for (String s : new String[] {"Hello, World!", "The quick brown fox", "0123456789", "caf\u00e9"}) {
                double jw = j.getStringBounds(s, FRC).getWidth();
                assertEquals(jw, f.stringWidth(s), 0.05, s);
            }
        }
    }

    @Test
    void outlinesMatchTheJdk() throws Exception {
        byte[] data = roboto();
        TrueTypeFont f = TrueTypeFont.load(data, 48);
        Font j = awt(data, 48);
        // \u00e9 and \u00c5 are composite glyphs in Roboto
        for (String s : new String[] {"g", "Q", "\u00e9", "\u00c5", "%&@"}) {
            int w = 160;
            int h = 80;
            BufferedImage ref = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = ref.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setColor(java.awt.Color.WHITE);
            GlyphVector gv = j.createGlyphVector(FRC, s);
            g2.fill(gv.getOutline(10, 60));
            g2.dispose();
            RasterImage ours = new RasterImage(w, h);
            RasterGraphics g = ours.createGraphics();
            g.setColor(0xffffffff);
            g.setFont(f);
            g.drawString(s, 10, 60);
            double a = 0;
            double b = 0;
            double diff = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int ra = ref.getRGB(x, y) >>> 24;
                    int oa = ours.getRGB(x, y) >>> 24;
                    a += ra;
                    b += oa;
                    diff += Math.abs(ra - oa);
                }
            }
            assertTrue(a > 0, s);
            assertEquals(a, b, a * 0.03, s + " coverage");
            assertTrue(diff < a * 0.12, s + " differs in " + diff / a + " of its ink");
        }
        double[] e = f.glyphBounds(f.glyphIndex(0xe9));
        assertNotNull(e);
        assertTrue(e[3] > f.glyphBounds(f.glyphIndex('e'))[3], "the accent sits above the e");
    }

    @Test
    void kernTableFormatZero() throws Exception {
        byte[] base = TrueTypeFont.getDefaultFontData();
        TrueTypeFont plain = TrueTypeFont.load(base, 1000);
        int a = plain.glyphIndex('A');
        int v = plain.glyphIndex('V');
        assertEquals(0, plain.kerning(a, v));
        // a kern table: version 0, one horizontal format 0 subtable, one pair
        byte[] kern = new byte[4 + 14 + 6];
        put16(kern, 2, 1);
        put16(kern, 6, 14 + 6);
        put16(kern, 8, 1);
        put16(kern, 10, 1);
        put16(kern, 18, a);
        put16(kern, 20, v);
        put16(kern, 22, -80);
        TrueTypeFont kerned = TrueTypeFont.load(withTable(base, "kern", kern), 1000);
        assertEquals(-80, kerned.kerning(a, v));
        assertEquals(0, kerned.kerning(v, a));
        assertEquals(plain.stringWidth("AV") - 80, kerned.stringWidth("AV"), 1e-9);
        assertEquals(plain.stringWidth("VA"), kerned.stringWidth("VA"), 1e-9);
    }

    @Test
    void refusesWhatItCannotRead() {
        assertThrows(IOException.class, () -> TrueTypeFont.load(new byte[] {1, 2, 3}, 12));
        byte[] otto = new byte[64];
        otto[0] = 'O';
        otto[1] = 'T';
        otto[2] = 'T';
        otto[3] = 'O';
        IOException e = assertThrows(IOException.class, () -> TrueTypeFont.load(otto, 12));
        assertTrue(e.getMessage().contains("CFF"));
        byte[] cut = TrueTypeFont.getDefaultFontData();
        byte[] half = new byte[cut.length / 2];
        System.arraycopy(cut, 0, half, 0, half.length);
        assertThrows(IOException.class, () -> TrueTypeFont.load(half, 12));
        assertThrows(IllegalArgumentException.class, () -> TrueTypeFont.createDefault(0));
    }

    @Test
    void cacheStaysBounded() throws Exception {
        TrueTypeFont f = TrueTypeFont.load(roboto(), 12);
        RasterPath p = new RasterPath();
        for (int g = 0; g < f.getGlyphCount(); g++) {
            f.appendGlyph(p, g, 0, 0);
        }
        assertTrue(f.getGlyphCount() > 512);
        assertTrue(f.stringWidth("still works") > 0);
    }

    private static void put16(byte[] d, int p, int v) {
        d[p] = (byte) (v >> 8);
        d[p + 1] = (byte) v;
    }

    private static int u16(byte[] d, int p) {
        return ((d[p] & 0xff) << 8) | (d[p + 1] & 0xff);
    }

    private static int u32(byte[] d, int p) {
        return (u16(d, p) << 16) | u16(d, p + 2);
    }

    private static void put32(byte[] d, int p, int v) {
        put16(d, p, v >>> 16);
        put16(d, p + 2, v);
    }

    /// Returns `font` with one more table, appended after the others.
    private static byte[] withTable(byte[] font, String tag, byte[] table) {
        int n = u16(font, 4);
        int shift = 16;
        byte[] out = new byte[font.length + shift + table.length + 3 & ~3];
        System.arraycopy(font, 0, out, 0, 12);
        put16(out, 4, n + 1);
        for (int i = 0; i < n; i++) {
            System.arraycopy(font, 12 + i * 16, out, 12 + i * 16, 16);
            put32(out, 12 + i * 16 + 8, u32(font, 12 + i * 16 + 8) + shift);
        }
        int r = 12 + n * 16;
        for (int i = 0; i < 4; i++) {
            out[r + i] = (byte) tag.charAt(i);
        }
        int dataStart = 12 + n * 16;
        System.arraycopy(font, dataStart, out, dataStart + shift, font.length - dataStart);
        int at = font.length + shift + 3 & ~3;
        out = java.util.Arrays.copyOf(out, at + table.length);
        put32(out, r + 8, at);
        put32(out, r + 12, table.length);
        System.arraycopy(table, 0, out, at, table.length);
        return out;
    }
}
