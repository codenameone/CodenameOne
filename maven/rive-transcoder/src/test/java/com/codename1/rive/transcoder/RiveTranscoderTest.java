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
package com.codename1.rive.transcoder;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RiveTranscoderTest {

    private static byte[] fixture(String name) throws IOException {
        InputStream in = RiveTranscoderTest.class.getResourceAsStream("/rive/" + name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        for (int n = in.read(buf); n > 0; n = in.read(buf)) {
            out.write(buf, 0, n);
        }
        in.close();
        return out.toByteArray();
    }

    @Test
    public void generatesARiveFileSubclassNamingOnlyTheClassesTheFileUses() throws Exception {
        String src = RiveTranscoder.transcode(fixture("juice.riv"), "app.gen", "JuiceRive");
        assertTrue(src.contains("package app.gen;"));
        assertTrue(src.contains("public final class JuiceRive extends com.codename1.rive.RiveFile"));
        assertTrue(src.contains("import com.codename1.impl.rive.Shape;"));
        assertTrue(src.contains("b.begin(new Artboard());"));
        // juice has no bones: the class must not name them, or the stripper keeps them
        assertFalse(src.contains("Bone"));
        assertFalse(src.contains("Skin"));
    }

    @Test
    public void generatedSourceIsAscii() throws Exception {
        String src = RiveTranscoder.transcode(fixture("off_road_car.riv"), "", "CarRive");
        for (int i = 0; i < src.length(); i++) {
            if (src.charAt(i) > 0x7e && src.charAt(i) != '\n') {
                fail("non-ASCII character at " + i);
            }
        }
    }

    @Test
    public void lineBreaksInStringsAreOctalNotUnicodeEscapes() {
        // javac turns a unicode escape of a line break into a real one before
        // it reads the literal, which ends the string mid-line
        assertEquals("\"a\\012b\"", RiveTranscoder.literal("a\nb"));
        assertEquals("\"\\u00e9\\\"\\\\\"", RiveTranscoder.literal("\u00e9\"\\"));
    }

    @Test
    public void floatsRoundTripExactly() {
        assertEquals("0.1f", RiveTranscoder.floatLiteral(Float.valueOf(0.1f)));
        assertEquals("Float.NaN", RiveTranscoder.floatLiteral(Float.valueOf(Float.NaN)));
        assertEquals("Float.NEGATIVE_INFINITY", RiveTranscoder.floatLiteral(Float.valueOf(Float.NEGATIVE_INFINITY)));
    }

    @Test(expected = IOException.class)
    public void rejectsAFileThatIsNotRive() throws Exception {
        RiveTranscoder.transcode(new byte[]{'J', 'U', 'N', 'K', 0, 0}, "", "X");
    }

    @Test
    public void reportsTypesItSkips() throws Exception {
        // BindableProperty is abstract: upstream makes no object of it either
        assertEquals(java.util.Collections.singletonList("BindableProperty (type 9)"),
                RiveTranscoder.skippedTypes(fixture("juice.riv")));
    }
}
