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

package com.codename1.ui.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThemeStringsTest {

    @Test
    void readUtfRejectsBadContinuationBytes() {
        final byte[][] bad = {{0, 2, (byte) 0xc3, 0x41}, {0, 3, (byte) 0xe2, (byte) 0x82, 0x41}, {0, 1, (byte) 0x80}};
        for (final byte[] b : bad) {
            assertThrows(IOException.class, () -> new ThemeStrings(1).readUTF(new DataInputStream(new ByteArrayInputStream(b))));
        }
    }

    @Test
    void hexAndDecimalMatchTheJdk() {
        ThemeStrings s = new ThemeStrings(0);
        int[] values = {0, 1, 9, 10, 15, 16, 0xff, 0x7aff, 0xffffff, 0x1000000, -1, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (int v : values) {
            assertEquals(Integer.toHexString(v), s.hex(v));
        }
        for (int v = 0; v < 256; v++) {
            assertEquals(String.valueOf(v), s.decimal(v));
        }
    }

    @Test
    void readUtfRoundTripsAndSharesRepeats() throws IOException {
        String[] values = {"", "ffffff", "native:MainRegular", "caf\u00e9", "\u05e9\u05dc\u05d5\u05dd", "\u0000x", "ffffff"};
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bo);
        for (String v : values) {
            out.writeUTF(v);
        }
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bo.toByteArray()));
        ThemeStrings s = new ThemeStrings(0);
        String[] read = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            read[i] = s.readUTF(in);
            assertEquals(values[i], read[i]);
        }
        assertSame(read[1], read[6]);
        assertSame(read[1], s.hex(0xffffff));
        assertSame(read[2], s.share(new String("native:MainRegular")));
    }

    @Test
    void manyDistinctValuesSurviveGrowth() {
        ThemeStrings s = new ThemeStrings(0);
        String[] first = new String[5000];
        for (int i = 0; i < first.length; i++) {
            first[i] = s.share("k" + i);
        }
        for (int i = 0; i < first.length; i++) {
            assertSame(first[i], s.share("k" + i));
        }
    }

    @Test
    void floatsAndIntsMatchConcatenation() {
        ThemeStrings s = new ThemeStrings(16);
        for (int k = -20000; k <= 20000; k++) {
            float f = (float) (k / 10.0);
            assertEquals("" + f + "," + f + "," + f + "," + f, s.floats(f, f, f, f));
        }
        float[] odd = {0.25f, 1.125f, 1e7f, 3.4e38f, -0.0f, 1e-5f, 0.001f, Float.NaN, Float.POSITIVE_INFINITY, 123456.7f, 999999.9f};
        for (float f : odd) {
            assertEquals("" + f + "," + 0f + "," + f + "," + 1f, s.floats(f, 0f, f, 1f));
        }
        for (int v = 0; v < 256; v++) {
            assertEquals("" + v + "," + 255 + "," + 0 + "," + v, s.ints(v, 255, 0, v));
        }
    }
}
