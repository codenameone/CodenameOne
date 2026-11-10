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
package com.codename1.impl.javase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Font.createTrueTypeFont(String, InputStream) on the simulator: a real TTF
 * loaded from bytes, derived and measured headless.
 */
public class JavaSEPortFontFromDataTest {
    private JavaSEPort port;
    private JavaSEPort originalInstance;

    @BeforeEach
    void setUp() {
        originalInstance = JavaSEPort.instance;
        port = new JavaSEPort();
    }

    @AfterEach
    void tearDown() {
        JavaSEPort.instance = originalInstance;
    }

    @Test
    void loadsDerivesAndMeasuresAFontFromBytes() throws IOException {
        assertTrue(port.isTrueTypeStreamSupported());
        Object font = port.loadTrueTypeFontFromData("Roboto", readResource("/com/codename1/impl/javase/Roboto-Regular.ttf"));
        assertNotNull(font);
        assertEquals("Roboto", ((java.awt.Font) font).getFamily());

        Object small = port.deriveTrueTypeFont(font, 12f, com.codename1.ui.Font.STYLE_PLAIN);
        Object large = port.deriveTrueTypeFont(font, 48f, com.codename1.ui.Font.STYLE_PLAIN);
        int smallWidth = port.stringWidth(small, "Hello World");
        int largeWidth = port.stringWidth(large, "Hello World");
        assertTrue(smallWidth > 0, "measured " + smallWidth);
        assertTrue(largeWidth > smallWidth * 3, smallWidth + " vs " + largeWidth);
    }

    @Test
    void bytesThatAreNotAFontLoadNothing() {
        assertNull(port.loadTrueTypeFontFromData("Junk", new byte[]{1, 2, 3, 4, 5, 6, 7, 8}));
    }

    @Test
    void aValidHeaderOverCorruptTablesLoadsNothing() throws IOException {
        byte[] header = new byte[]{0, 1, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 7, 7, 7, 7, 7, 7, 7, 7};
        assertNull(port.loadTrueTypeFontFromData("Corrupt", header));
        byte[] roboto = readResource("/com/codename1/impl/javase/Roboto-Regular.ttf");
        byte[] truncated = new byte[64];
        System.arraycopy(roboto, 0, truncated, 0, truncated.length);
        assertNull(port.loadTrueTypeFontFromData("Truncated", truncated));
    }

    private static byte[] readResource(String path) throws IOException {
        InputStream in = JavaSEPortFontFromDataTest.class.getResourceAsStream(path);
        assertNotNull(in, path);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
