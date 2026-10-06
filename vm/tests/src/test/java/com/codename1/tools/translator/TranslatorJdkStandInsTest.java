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

import java.security.MessageDigest;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stand-ins the translator carries because ParparVM's JavaAPI, which a
 * self-hosted translator runs on, lacks the JDK original; each must agree with it.
 */
class TranslatorJdkStandInsTest {

    @Test
    void sha256MatchesMessageDigestAcrossBlockBoundaries() throws Exception {
        Random r = new Random(7);
        // Every length up to three blocks, so each padding case (the length field
        // fitting in the last block or spilling into a new one) is crossed.
        for (int length = 0; length <= 200; length++) {
            byte[] data = new byte[length];
            r.nextBytes(data);
            assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(data), Sha256.digest(data), "length " + length);
        }
    }

    @Test
    void decodeIntMatchesIntegerDecode() {
        String[] inputs = {"0", "7", "-7", "+7", "010", "-010", "0x1F", "0X1f", "#ff", "-#10", "2147483647",
            "-2147483648", "0x7fffffff", "-0x80000000", "00", "-0"};
        for (String s : inputs) {
            assertEquals(Integer.decode(s).intValue(), Util.decodeInt(s), s);
        }
        for (String bad : new String[] {"", "-", "0x", "--1", "+-1", "2147483648", "0xg", "08"}) {
            assertThrows(NumberFormatException.class, () -> Integer.decode(bad), bad);
            assertThrows(NumberFormatException.class, () -> Util.decodeInt(bad), bad);
        }
    }

    @Test
    void integerPropertyMatchesIntegerGetInteger() {
        String key = "cn1.test.integerProperty";
        String[] values = {null, "12", "0x10", "bogus", "-5"};
        for (String v : values) {
            if (v == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, v);
            }
            try {
                assertEquals(Integer.getInteger(key), Util.integerProperty(key), String.valueOf(v));
                assertEquals(Integer.getInteger(key, 99).intValue(), Util.integerProperty(key, 99), String.valueOf(v));
            } finally {
                System.clearProperty(key);
            }
        }
    }
}
