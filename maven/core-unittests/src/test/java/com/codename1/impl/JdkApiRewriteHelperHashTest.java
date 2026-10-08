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
package com.codename1.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The static wrapper hash and compare methods the bytecode rewrite points
/// Kotlin data classes at must answer exactly what the JDK's do, or a value
/// hashed on the device differs from the same value hashed in the simulator.
class JdkApiRewriteHelperHashTest {

    @Test
    void hashesMatchTheBoxedValues() {
        int[] ints = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, 123456789};
        for (int v : ints) {
            assertEquals(Integer.valueOf(v).hashCode(), JdkApiRewriteHelper.hashCode(v));
            assertEquals(Short.valueOf((short) v).hashCode(), JdkApiRewriteHelper.hashCode((short) v));
            assertEquals(Byte.valueOf((byte) v).hashCode(), JdkApiRewriteHelper.hashCode((byte) v));
            assertEquals(Character.valueOf((char) v).hashCode(), JdkApiRewriteHelper.hashCode((char) v));
        }
        long[] longs = {0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 0x123456789abcdefL};
        for (long v : longs) {
            assertEquals(Long.valueOf(v).hashCode(), JdkApiRewriteHelper.hashCode(v));
        }
        double[] doubles = {0.0, -0.0, 1.5, Double.NaN, Double.POSITIVE_INFINITY, -123.456};
        for (double v : doubles) {
            assertEquals(Double.valueOf(v).hashCode(), JdkApiRewriteHelper.hashCode(v));
            assertEquals(Float.valueOf((float) v).hashCode(), JdkApiRewriteHelper.hashCode((float) v));
        }
        assertEquals(Boolean.TRUE.hashCode(), JdkApiRewriteHelper.hashCode(true));
        assertEquals(Boolean.FALSE.hashCode(), JdkApiRewriteHelper.hashCode(false));
    }

    @Test
    void comparesMatchTheJdk() {
        boolean[] bools = {false, true};
        for (boolean a : bools) {
            for (boolean b : bools) {
                assertEquals(Integer.signum(Boolean.valueOf(a).compareTo(b)),
                        Integer.signum(JdkApiRewriteHelper.compare(a, b)));
            }
        }
        assertEquals(Character.valueOf('a').compareTo('z'), JdkApiRewriteHelper.compare('a', 'z'));
        assertEquals(Byte.valueOf((byte) -5).compareTo((byte) 7), JdkApiRewriteHelper.compare((byte) -5, (byte) 7));
    }
}
