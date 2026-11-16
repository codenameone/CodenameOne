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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PdfInternalsTest {

    @Test
    void bidiReordersRightToLeftRunsAndMirrors() {
        String hebrew = "\u05d0\u05d1\u05d2";
        assertTrue(PdfBidi.hasRtl(hebrew));
        assertEquals(PdfBidi.RTL, PdfBidi.direction(hebrew + " abc", PdfBidi.LTR));
        assertEquals(PdfBidi.LTR, PdfBidi.direction("abc " + hebrew, PdfBidi.RTL));
        // Logical "abc ALEF BET GIMEL" displays the Hebrew reversed.
        assertEquals("abc \u05d2\u05d1\u05d0", PdfBidi.reorder("abc " + hebrew, PdfBidi.LTR));
        // Numbers inside right-to-left text keep their order.
        assertEquals("123 \u05d2\u05d1\u05d0", PdfBidi.reorder(hebrew + " 123", PdfBidi.RTL));
        assertEquals(')', PdfBidi.mirror('('));
        byte[] levels = PdfBidi.levels("ab" + hebrew, PdfBidi.LTR);
        assertEquals(0, levels[0]);
        assertEquals(1, levels[2]);
    }

    @Test
    void arabicTakesJoinedForms() {
        // BEH BEH BEH: initial, medial, final.
        String beh = "\u0628\u0628\u0628";
        int[] forms = ArabicShaper.forms(beh);
        assertEquals(ArabicShaper.INITIAL, forms[0]);
        assertEquals(ArabicShaper.MEDIAL, forms[1]);
        assertEquals(ArabicShaper.FINAL, forms[2]);
        assertEquals('\ufe91', ArabicShaper.presentation('\u0628', ArabicShaper.INITIAL));
        assertEquals("\u0628", ArabicShaper.letters('\ufe91'));
    }

    @Test
    void dashSplitsAPath() {
        PdfPath p = new PdfPath();
        p.moveTo(0, 0);
        p.lineTo(100, 0);
        PdfPath d = p.dash(new float[] {10, 10}, 0);
        int moves = 0;
        for (int i = 0; i < d.getOpCount(); i++) {
            if (d.getOp(i) == p.getOp(0)) {
                moves++;
            }
        }
        assertEquals(5, moves);
    }

    @Test
    void multiplyBlendDarkens() {
        int[] back = {0xff808080};
        int[] src = {0xff808080};
        int[] out = PdfBlend.composite(back, src, null, PdfBlend.MULTIPLY, 1, 1, 1);
        int r = (out[0] >> 16) & 0xff;
        assertTrue(r >= 63 && r <= 65, Integer.toHexString(out[0]));
        assertEquals(PdfBlend.MULTIPLY, PdfBlend.parse(new PdfName("Multiply")));
    }

    // A 40x12 bitmap encoded by libtiff (tiffcp) as CCITT G4 and G3 2D.
    private static final String G4 = "26a88e8ba2e8ba23a2e8ba2e88e8ba2e8ba23a096c20984c2d84c2616c26130b613b6104c2d84c2616c"
            + "26130b61309b6105b0984c2d84c2616c2613508131b0984c2d84c2616c26130bb28708136130b6130985b0984c2df61026c2d84c"
            + "2616c26130b6136130813d84c2616c26130b6135082613082b0984c2d84c2616c2613b0985b0984c2d84c2616c26130b616c2613"
            + "0b6130985b0984c2d8416c26130b6130985b0984c2da8008008";
    private static final String G3 = "0019aa1d3a74e874e9d3a1d3a74e874e87001584130985b0984c2d84c2616c270018f8e9d0e9d3a743"
            + "a74e9d0e9d3a700136105b0984c2d84c2616c261350019aa79d3a743a74e9d0e9d3a743a1c004ca1c204d84c2d84c2616c26130b"
            + "70018e9d0fba1d3a74e874e9d3a1d3a70013098409ec26130b6130985b09a800cd53a743a74e9d0e9d3a743a74e9d000984c2d84"
            + "c2616c26130b6130985800dd0e9d3a743a74e9d0e9d3a743a70010416c26130b6130985b0984c2da80";

    @Test
    void ccittDecodesG4AndG3() {
        check(CcittDecoder.decode(hex(G4), params(-1), 40, 12));
        check(CcittDecoder.decode(hex(G3), params(4), 40, 12));
    }

    private static PdfDict params(int k) {
        PdfDict d = new PdfDict();
        d.put("K", Integer.valueOf(k));
        d.put("Columns", Integer.valueOf(40));
        d.put("Rows", Integer.valueOf(12));
        return d;
    }

    private static void check(byte[] bits) {
        assertNotNull(bits);
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 40; x++) {
                boolean black = (x * 7 + y * 3) % 11 < 4 || x == y;
                int bit = (bits[y * 5 + x / 8] >> (7 - x % 8)) & 1;
                assertEquals(black ? 0 : 1, bit, "pixel " + x + "," + y);
            }
        }
    }

    // The same bitmap as a JBIG2 generic region (template 0, TPGDON),
    // written by a T.88 encoder and checked against poppler.
    private static final String JBIG2 = "0000000030000100000013000000280000000c0000000000000000000000000000012600010000003300000028000000"
            + "0c0000000000000000000803fffdff02fefefe3cb99458b014f7cb7258174dba0ae067e4fd9031617edbffac00000002"
            + "31000100000000";

    @Test
    void jbig2DecodesGenericRegion() {
        check(Jbig2Decoder.decode(hex(JBIG2), null, 40, 12));
    }

    // A 16x16 gray image, lossless, from opj_compress.
    private static final String JPX = "ff4fff510029000000000010000000100000000000000000000000100000001000000000000000000001070101ff5200"
            + "0c00000001000204040001ff5c000a4040484850484850ff640025000143726561746564206279204f70656e4a504547"
            + "2076657273696f6e20322e352e34ff90000a0000000001080001ff93cfb4481150d5d1293f40750b469979458b8ce9b6"
            + "27c7da231f687c1f507819c1ca0dae19d6661e0c549414c3a628bf19d9b6b5699910bea0afda43266e431f57b9be0f1d"
            + "5436b7272a8966c53fc7da750fa8d21f69b0372a2dc9d9057dea0fba2ea85225aeb1efae3e6406c16fa326138d8bc305"
            + "ea4e30bff5dbcfc11a445ab4cb948becf4d133fe74a3d6a9fcf31c9f29e21d9928c5fa04de2562f209a6b7a6666ccdeb"
            + "76469f172bbbad8cfc1d7402ef1f83567d2c813f246e8ef988fbebac112fbe5f354b709c6f283a48b81ffe4e26b6c6eb"
            + "9944632d4d7fed02faa23900464e0a9f1da2f3dcd6c1dc57620b6c59899916ae2c45b7579615ffd9";

    @Test
    void jpxDecodesLosslessExactly() {
        JpxDecoder j = JpxDecoder.decode(hex(JPX));
        assertNotNull(j);
        assertEquals(16, j.width);
        assertEquals(16, j.height);
        int[] px = j.argb(null, false);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int v = ((x * 16 + y * 5) ^ (x * y)) & 255;
                assertEquals(0xff000000 | v << 16 | v << 8 | v, px[y * 16 + x], "pixel " + x + "," + y);
            }
        }
    }

    @Test
    void deepNestingAndStrayClosersDoNotOverflowTheStack() {
        byte[] open = new byte[200000];
        java.util.Arrays.fill(open, (byte) '[');
        assertNotNull(new PdfLexer(open, 0, open.length, false).next());
        byte[] close = new byte[200000];
        java.util.Arrays.fill(close, (byte) ']');
        assertSame(PdfLexer.EOF, new PdfLexer(close, 0, close.length, false).next());
        byte[] parens = new byte[200000];
        java.util.Arrays.fill(parens, (byte) ')');
        assertSame(PdfLexer.EOF, new PdfLexer(parens, 0, parens.length, false).next());
    }

    @Test
    void calculatorStopsOnTooFewOperands() {
        double[] out = PdfCalculator.run(PdfCalculator.compile("{ pop pop add }".getBytes()), new double[] {1}, 1);
        assertEquals(1, out.length);
        PdfCalculator.run(PdfCalculator.compile("{ true }".getBytes()), new double[0], 1);
    }

    @Test
    void cidCollectionsMapToUnicode() {
        assertEquals(0x3042, CidToUnicode.lookup("Japan1", 843));
        assertEquals(0x65e5, CidToUnicode.lookup("Japan1", 3284));
        assertEquals(0, CidToUnicode.lookup("Unknown", 1));
    }

    private static byte[] hex(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return b;
    }
}
