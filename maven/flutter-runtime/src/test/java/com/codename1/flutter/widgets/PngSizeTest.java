/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * A PNG's size comes from its IHDR chunk, wherever that chunk is.
 *
 * <p>IHDR is first in a standards-conforming PNG, and this used to read its
 * payload at a fixed offset. It is NOT first in the PNGs an iOS app ships:
 * Xcode rewrites every bundled PNG into Apple's CgBI form, which puts a
 * four-byte CgBI chunk in front. The fixed offset then read that chunk's
 * payload as the width and its checksum as the height, and the picture was
 * drawn to a garbage aspect -- the gallery's category icons were squashed to
 * half their height on device and pixel-exact in every desktop sweep.</p>
 */
class PngSizeTest {

    private static void be32(byte[] d, int off, int v) {
        d[off] = (byte) (v >>> 24);
        d[off + 1] = (byte) (v >>> 16);
        d[off + 2] = (byte) (v >>> 8);
        d[off + 3] = (byte) v;
    }

    private static void type(byte[] d, int off, String t) {
        for (int i = 0; i < 4; i++) {
            d[off + i] = (byte) t.charAt(i);
        }
    }

    /** A PNG signature followed by the given chunks, each header-only. */
    private static byte[] png(boolean cgbiFirst, int w, int h) {
        int size = 8 + (cgbiFirst ? 16 : 0) + 25;
        byte[] d = new byte[size];
        d[0] = (byte) 0x89;
        d[1] = 'P';
        d[2] = 'N';
        d[3] = 'G';
        int off = 8;
        if (cgbiFirst) {
            // Apple's chunk: 4 bytes of payload, then the 4-byte CRC.
            be32(d, off, 4);
            type(d, off + 4, "CgBI");
            be32(d, off + 8, 0x50000200);
            off += 16;
        }
        be32(d, off, 13);
        type(d, off + 4, "IHDR");
        be32(d, off + 8, w);
        be32(d, off + 12, h);
        return d;
    }

    @Test
    void anOrdinaryPngReadsItsSize() {
        assertArrayEquals(new int[] {64, 64},
                ImageRenderElement.pngSize(png(false, 64, 64)));
    }

    @Test
    void aCgbiPngReadsItsSizeToo() {
        // The layout an iOS bundle actually contains. Read at the old fixed
        // offsets 16 and 20 this answers with the CgBI chunk's payload and
        // whatever follows it -- never the picture's size.
        assertArrayEquals(new int[] {64, 64},
                ImageRenderElement.pngSize(png(true, 64, 64)));
    }

    @Test
    void aFileWithNoIhdrReportsUnknown() {
        byte[] d = new byte[40];
        d[0] = (byte) 0x89;
        d[1] = 'P';
        d[2] = 'N';
        d[3] = 'G';
        be32(d, 8, 4);
        type(d, 12, "CgBI");
        assertArrayEquals(new int[] {-1, -1}, ImageRenderElement.pngSize(d));
    }

    @Test
    void aTruncatedChunkLengthDoesNotRunAway() {
        byte[] d = new byte[40];
        d[0] = (byte) 0x89;
        d[1] = 'P';
        d[2] = 'N';
        d[3] = 'G';
        be32(d, 8, Integer.MAX_VALUE);
        type(d, 12, "junk");
        assertArrayEquals(new int[] {-1, -1}, ImageRenderElement.pngSize(d));
    }
}
