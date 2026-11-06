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
package com.codename1.tools.translator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The JavaScript port sizes a native image from its encoded header
/// (`ImageHeaderDimensions`), so `Image.getWidth()` never waits on the browser.
/// A wrong answer there is a wrong layout, so each format is checked against an
/// image ImageIO actually encoded.
class JavascriptImageHeaderDimensionsTest {
    private static Method read;

    @BeforeAll
    static void compile() throws Exception {
        Path src = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java",
                "com", "codename1", "impl", "html5", "ImageHeaderDimensions.java").toAbsolutePath().normalize();
        Path out = Files.createTempDirectory("image-header-dimensions");
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertEquals(0, javac.run(null, null, null, "-d", out.toString(), src.toString()), "compile " + src);
        URLClassLoader loader = new URLClassLoader(new URL[]{out.toUri().toURL()});
        read = loader.loadClass("com.codename1.impl.html5.ImageHeaderDimensions")
                .getDeclaredMethod("read", byte[].class, int.class, int.class);
        read.setAccessible(true);
    }

    private static int[] dims(byte[] b) throws Exception {
        return (int[]) read.invoke(null, b, 0, b.length);
    }

    private static byte[] encode(String format, int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(img, format, bo), "no ImageIO writer for " + format);
        return bo.toByteArray();
    }

    @Test
    void readsEveryEncodedFormat() throws Exception {
        for (String format : new String[]{"png", "gif", "jpg", "bmp"}) {
            assertArrayEquals(new int[]{37, 211}, dims(encode(format, 37, 211)), format);
            assertArrayEquals(new int[]{300, 4}, dims(encode(format, 300, 4)), format);
        }
    }

    @Test
    void readsAtAnOffset() throws Exception {
        byte[] png = encode("png", 12, 34);
        byte[] padded = new byte[png.length + 7];
        System.arraycopy(png, 0, padded, 7, png.length);
        assertArrayEquals(new int[]{12, 34}, (int[]) read.invoke(null, padded, 7, png.length));
    }

    @Test
    void swapsForARotatingExifOrientation() throws Exception {
        byte[] jpg = encode("jpg", 40, 10);
        assertArrayEquals(new int[]{10, 40}, dims(withOrientation(jpg, 6)), "orientation 6 turns the image");
        assertArrayEquals(new int[]{40, 10}, dims(withOrientation(jpg, 3)), "orientation 3 does not");
    }

    @Test
    void answersNullForWhatItCannotRead() throws Exception {
        assertNull(dims(new byte[0]));
        assertNull(dims("not an image at all".getBytes("US-ASCII")));
        byte[] png = encode("png", 5, 5);
        byte[] cut = new byte[16];
        System.arraycopy(png, 0, cut, 0, cut.length);
        assertNull(dims(cut), "a PNG cut before IHDR's size");
    }

    /// Inserts a big-endian EXIF APP1 segment holding one orientation tag right
    /// after the SOI marker.
    private static byte[] withOrientation(byte[] jpg, int orientation) {
        byte[] tiff = {'M', 'M', 0, 42, 0, 0, 0, 8,
                0, 1,
                0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0,
                0, 0, 0, 0};
        byte[] exif = {'E', 'x', 'i', 'f', 0, 0};
        int segLen = 2 + exif.length + tiff.length;
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        bo.write(jpg, 0, 2);
        bo.write(0xFF);
        bo.write(0xE1);
        bo.write(segLen >> 8);
        bo.write(segLen & 0xFF);
        bo.write(exif, 0, exif.length);
        bo.write(tiff, 0, tiff.length);
        bo.write(jpg, 2, jpg.length - 2);
        return bo.toByteArray();
    }
}
