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
package com.codename1.qrcode;

import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// The matrices pinned below were decoded back to their text by ZXing's QR
/// decoder when this encoder was written, as were codes of every level up to
/// version 40; the checksums hold that output still.
class QrCodeTest {

    private static String crc(QrCode q) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < q.getSize(); y++) {
            for (int x = 0; x < q.getSize(); x++) {
                sb.append(q.isDark(x, y) ? '1' : '0');
            }
        }
        CRC32 c = new CRC32();
        byte[] b = sb.toString().getBytes();
        c.update(b, 0, b.length);
        return Long.toHexString(c.getValue());
    }

    @Test
    void matchesTheDecodedReference() {
        String url = "https://www.codenameone.com";
        String[][] expected = {
            {"2", "3", "de63f8fa"}, {"3", "1", "1d974666"}, {"3", "6", "e68c5902"}, {"4", "1", "cbe23fb1"},
        };
        for (int ecc = 0; ecc < 4; ecc++) {
            QrCode q = QrCode.encode(url, ecc);
            assertEquals(expected[ecc][0], String.valueOf(q.getVersion()), "version at level " + ecc);
            assertEquals(expected[ecc][1], String.valueOf(q.getMask()), "mask at level " + ecc);
            assertEquals(expected[ecc][2], crc(q), "modules at level " + ecc);
        }
        assertEquals("4c9ce7", crc(QrCode.encode("HELLO WORLD", QrCode.ECC_LOW)));
        assertEquals("b847a01e", crc(QrCode.encode("HELLO WORLD", QrCode.ECC_HIGH)));
    }

    @Test
    void capacityIsTheStandardsTable() {
        // Version 1-L holds 19 data codewords, 10-M 216, 40-L 2956, 40-H 1276.
        assertEquals(19, QrCode.dataCodewords(1, QrCode.ECC_LOW));
        assertEquals(216, QrCode.dataCodewords(10, QrCode.ECC_MEDIUM));
        assertEquals(2956, QrCode.dataCodewords(40, QrCode.ECC_LOW));
        assertEquals(1276, QrCode.dataCodewords(40, QrCode.ECC_HIGH));
        assertEquals(40, QrCode.encode(new byte[2953], QrCode.ECC_LOW).getVersion());
        assertThrows(IllegalArgumentException.class, () -> QrCode.encode(new byte[2954], QrCode.ECC_LOW));
        assertEquals(177, QrCode.encode(new byte[2331], QrCode.ECC_MEDIUM).getSize());
    }

    @Test
    void finderPatternsAndQuietZone() {
        QrCode q = QrCode.encode("x", QrCode.ECC_MEDIUM);
        assertEquals(21, q.getSize());
        int[][] corners = {{0, 0}, {q.getSize() - 7, 0}, {0, q.getSize() - 7}};
        for (int[] c : corners) {
            assertTrue(q.isDark(c[0], c[1]));
            assertFalse(q.isDark(c[0] + 1, c[1] + 1));
            assertTrue(q.isDark(c[0] + 3, c[1] + 3));
        }
        assertFalse(q.isDark(-1, 0));
        assertFalse(q.isDark(0, q.getSize()));
        // The module beside the lower left finder is always dark.
        assertTrue(q.isDark(8, q.getSize() - 8));
    }

    @Test
    void svgHasOnePathPerCode() {
        String svg = QrCode.encode("x", QrCode.ECC_LOW).toSvg();
        assertTrue(svg.startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 29 29\""));
        assertTrue(svg.endsWith("</svg>"));
        assertEquals(1, svg.split("<path ").length - 1);
    }

    @Test
    void rejectsBadArguments() {
        assertThrows(IllegalArgumentException.class, () -> QrCode.encode((String) null, QrCode.ECC_LOW));
        assertThrows(IllegalArgumentException.class, () -> QrCode.encode("x", 4));
    }
}
