/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.io.gzip;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;
import com.codename1.junit.FormTest;

public class JZlibTest extends UITestBase {

    @FormTest
    public void testJZlibConstants() {
        // JZlib seems to be a class with constants and version info
        Assertions.assertNotNull(JZlib.version());
        // Version changed to 1.1.0
        Assertions.assertEquals("1.1.0", JZlib.version());

        // Check constants
        Assertions.assertEquals(0, JZlib.Z_OK);
        Assertions.assertEquals(1, JZlib.Z_STREAM_END);
        Assertions.assertEquals(2, JZlib.Z_NEED_DICT);
        Assertions.assertEquals(-1, JZlib.Z_ERRNO);
        Assertions.assertEquals(-2, JZlib.Z_STREAM_ERROR);
        Assertions.assertEquals(-3, JZlib.Z_DATA_ERROR);
        Assertions.assertEquals(-4, JZlib.Z_MEM_ERROR);
        Assertions.assertEquals(-5, JZlib.Z_BUF_ERROR);
        Assertions.assertEquals(-6, JZlib.Z_VERSION_ERROR);
    }
    /// A complete raw stream whose last code is shorter than the literal
    /// table's index width, and whose final byte holds fewer bits than that
    /// width: inflate used to wait for input that never comes and answer
    /// Z_BUF_ERROR, with Z_FINISH and without it.
    @FormTest
    public void testRawStreamEndingInShortCode() {
        String hex = "93efe66060582a91c5f4fff4c933dea127ce7b3f3dc568e875eaf34e86eeeb5f4a8518181800";
        byte[] in = new byte[hex.length() / 2];
        for (int i = 0; i < in.length; i++) {
            in[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        int[] flushes = {JZlib.Z_FINISH, JZlib.Z_NO_FLUSH};
        for (int flush : flushes) {
            ZStream z = new ZStream();
            Assertions.assertEquals(JZlib.Z_OK, z.inflateInit(-15));
            z.setInput(in, 0, in.length, false);
            byte[] out = new byte[1024];
            z.setOutput(out, 0, out.length);
            Assertions.assertEquals(JZlib.Z_STREAM_END, z.inflate(flush));
            Assertions.assertEquals(0, z.availIn);
            Assertions.assertEquals(35, out.length - z.availOut);
            Assertions.assertEquals(0x1f, out[0] & 0xff);
            Assertions.assertEquals(0x8b, out[1] & 0xff);
        }
    }
}
