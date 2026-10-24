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
package com.codename1.desktopcompat.shared.jdk.java.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PropertyResourceBundleTest {

    private static String value(byte[] file) throws IOException {
        return new PropertyResourceBundle(new ByteArrayInputStream(file)).getString("k");
    }

    /// A UTF-8 file reads as UTF-8, as the JDK has read one since Java 9.
    @Test void wellFormedUtf8IsReadAsUtf8() throws IOException {
        // "k=" then e-acute and the euro sign in UTF-8.
        assertEquals("\u00e9\u20ac", value(new byte[] {'k', '=', (byte) 0xc3, (byte) 0xa9,
            (byte) 0xe2, (byte) 0x82, (byte) 0xac}));
    }

    /// Bytes that are not UTF-8 are the ISO-8859-1 a pre-9 file was written in.
    @Test void anythingElseIsReadAsLatin1() throws IOException {
        assertEquals("\u00e9t\u00e9", value(new byte[] {'k', '=', (byte) 0xe9, 't', (byte) 0xe9}));
        // A truncated sequence at the end, and an overlong encoding of '/'.
        assertEquals("a\u00c3", value(new byte[] {'k', '=', 'a', (byte) 0xc3}));
        assertEquals("\u00c0\u00af", value(new byte[] {'k', '=', (byte) 0xc0, (byte) 0xaf}));
    }

    @Test void escapesStillWork() throws IOException {
        assertEquals("\u00e9", value("k=\\u00e9".getBytes("ISO-8859-1")));
    }
}
