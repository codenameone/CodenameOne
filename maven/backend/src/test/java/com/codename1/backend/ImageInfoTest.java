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
package com.codename1.backend;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageInfoTest {
    private static byte[] png(int w, int h, int depth, int type, boolean trns) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a}, 0, 8);
        chunk(o, "IHDR", new byte[] {(byte) (w >> 24), (byte) (w >> 16), (byte) (w >> 8), (byte) w,
            (byte) (h >> 24), (byte) (h >> 16), (byte) (h >> 8), (byte) h, (byte) depth, (byte) type, 0, 0, 0});
        if (trns) {
            chunk(o, "tRNS", new byte[] {0});
        }
        chunk(o, "IDAT", new byte[4]);
        chunk(o, "IEND", new byte[0]);
        return o.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream o, String type, byte[] body) {
        int n = body.length;
        o.write(new byte[] {(byte) (n >> 24), (byte) (n >> 16), (byte) (n >> 8), (byte) n}, 0, 4);
        for (int i = 0; i < 4; i++) {
            o.write(type.charAt(i));
        }
        o.write(body, 0, n);
        o.write(new byte[4], 0, 4);
    }

    private static void segment(ByteArrayOutputStream o, int marker, byte[] body) {
        o.write(0xff);
        o.write(marker);
        int n = body.length + 2;
        o.write(n >> 8);
        o.write(n);
        o.write(body, 0, body.length);
    }

    private static byte[] jpeg(int sof, int w, int h) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(0xff);
        o.write(0xd8);
        segment(o, 0xe0, "JFIF\0\1\1\0\0\1\0\1\0\0".getBytes());
        byte[] exif = new byte[3000];
        exif[0] = 'E';
        exif[1] = 'x';
        exif[2] = 'i';
        exif[3] = 'f';
        exif[100] = (byte) 0xff;
        exif[101] = (byte) 0xc0;
        segment(o, 0xe1, exif);
        segment(o, 0xdb, new byte[65]);
        segment(o, 0xc4, new byte[20]);
        o.write(0xff); // fill byte
        segment(o, sof, new byte[] {8, (byte) (h >> 8), (byte) h, (byte) (w >> 8), (byte) w, 3, 1, 0x22, 0,
            2, 0x11, 1, 3, 0x11, 1});
        segment(o, 0xda, new byte[10]);
        o.write(0xff);
        o.write(0xd9);
        return o.toByteArray();
    }

    @Test
    void png() throws Exception {
        ImageInfo i = ImageInfo.read(png(1290, 2796, 8, 6, false));
        assertEquals(ImageInfo.Format.PNG, i.getFormat());
        assertEquals(1290, i.getWidth());
        assertEquals(2796, i.getHeight());
        assertEquals(8, i.getBitDepth());
        assertEquals(6, i.getColorType());
        assertTrue(i.hasAlpha());
        assertFalse(ImageInfo.read(png(10, 20, 8, 2, false)).hasAlpha());
        assertTrue(ImageInfo.read(new ByteArrayInputStream(png(10, 20, 8, 3, true))).hasAlpha());
    }

    @Test
    void baselineJpegAfterExif() {
        ImageInfo i = ImageInfo.read(jpeg(0xc0, 2048, 1536));
        assertEquals(ImageInfo.Format.JPEG, i.getFormat());
        assertEquals(2048, i.getWidth());
        assertEquals(1536, i.getHeight());
        assertEquals(8, i.getBitDepth());
        assertFalse(i.isProgressive());
    }

    @Test
    void progressiveJpeg() {
        ImageInfo i = ImageInfo.read(jpeg(0xc2, 640, 480));
        assertEquals(640, i.getWidth());
        assertEquals(480, i.getHeight());
        assertTrue(i.isProgressive());
    }

    @Test
    void gifAndWebp() {
        byte[] gif = {'G', 'I', 'F', '8', '9', 'a', 0x40, 0x01, (byte) 0xf0, 0x00, 0, 0, 0, 0};
        ImageInfo g = ImageInfo.read(gif);
        assertEquals(ImageInfo.Format.GIF, g.getFormat());
        assertEquals(320, g.getWidth());
        assertEquals(240, g.getHeight());
        byte[] webp = new byte[30];
        System.arraycopy("RIFF\0\0\0\0WEBPVP8X".getBytes(), 0, webp, 0, 16);
        webp[20] = 0x10;
        webp[24] = 99; // width - 1
        webp[27] = 49; // height - 1
        ImageInfo w = ImageInfo.read(webp);
        assertEquals(ImageInfo.Format.WEBP, w.getFormat());
        assertEquals(100, w.getWidth());
        assertEquals(50, w.getHeight());
        assertTrue(w.hasAlpha());
    }

    @Test
    void unrecognizedOrTruncated() {
        assertNull(ImageInfo.read(new byte[] {1, 2, 3}));
        assertNull(ImageInfo.read("hello, this is not an image".getBytes()));
        byte[] cut = jpeg(0xc0, 10, 10);
        assertNull(ImageInfo.read(java.util.Arrays.copyOf(cut, 40)));
    }
}
