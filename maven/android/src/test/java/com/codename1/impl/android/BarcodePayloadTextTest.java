/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.android;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;

/// The payloads are synthetic Croatian HUB-3 style text, encoded the two ways
/// real slips were measured to arrive: UTF-8 from one issuer, windows-1250
/// from another. Letters are built from code points so this file stays ASCII.
class BarcodePayloadTextTest {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Charset CP1250 = Charset.forName("windows-1250");
    private static final Charset LATIN2 = Charset.forName("ISO-8859-2");

    private static final char S_CARON = (char) 0x160;
    private static final char C_CARON = (char) 0x10d;
    private static final char C_ACUTE = (char) 0x107;
    private static final char Z_CARON = (char) 0x17e;

    /// Every letter that differs between windows-1250 and ISO-8859-2 (the
    /// S and Z carons) next to ones they share.
    private static final String TEXT = "HRVHUB30\nEUR\nPRIMJER " + S_CARON
            + "UMA 12\nUli" + C_CARON + "ka 7\nGospi" + C_ACUTE
            + "\nBu" + Z_CARON + "imska 10\n" + S_CARON + "ibenik";

    @Test
    void utf8PayloadStaysUtf8WhenAppNamesSingleByteCharset() {
        byte[] utf8 = TEXT.getBytes(UTF8);
        assertEquals(TEXT, BarcodePayloadText.decode(utf8, "windows-1250"));
        assertEquals(TEXT, BarcodePayloadText.decode(utf8, "ISO-8859-2"));
    }

    @Test
    void singleBytePayloadReadsInNamedCharset() {
        assertEquals(TEXT, BarcodePayloadText.decode(TEXT.getBytes(CP1250), "windows-1250"));
        assertEquals(TEXT, BarcodePayloadText.decode(TEXT.getBytes(LATIN2), "ISO-8859-2"));
    }

    @Test
    void withoutCharsetUtf8ThenLatin1() {
        assertEquals(TEXT, BarcodePayloadText.decode(TEXT.getBytes(UTF8), null));
        byte[] cp1250 = TEXT.getBytes(CP1250);
        assertEquals(new String(cp1250, Charset.forName("ISO-8859-1")),
                BarcodePayloadText.decode(cp1250, ""));
    }

    @Test
    void asciiIsTheSameEverywhere() {
        byte[] ascii = "HRVHUB30\nEUR\n000000000002891".getBytes(UTF8);
        assertEquals("HRVHUB30\nEUR\n000000000002891",
                BarcodePayloadText.decode(ascii, "windows-1250"));
    }

    @Test
    void unusableCharsetFallsBack() {
        byte[] cp1250 = TEXT.getBytes(CP1250);
        String latin1 = new String(cp1250, Charset.forName("ISO-8859-1"));
        assertEquals(latin1, BarcodePayloadText.decode(cp1250, "no-such-charset"));
        // 0x81 is unmapped in windows-1250 and not UTF-8.
        byte[] bad = {'A', (byte) 0x81, 'B'};
        assertEquals(new String(bad, Charset.forName("ISO-8859-1")),
                BarcodePayloadText.decode(bad, "windows-1250"));
    }
}
