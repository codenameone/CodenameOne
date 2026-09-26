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

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CNumberTest {
    @Test void finiteLiteralsRoundTripEveryBit() {
        long[] doubles = {0, Long.MIN_VALUE, 1, 0xfffffffffffffL, 0x10000000000000L,
                0x7fefffffffffffffL, Double.doubleToLongBits(1.99584030953472E292)};
        for (long bits : doubles) checkDouble(bits);
        int[] floats = {0, Integer.MIN_VALUE, 1, 0x7fffff, 0x800000, 0x7f7fffff};
        for (int bits : floats) checkFloat(bits);
        Random random = new Random(42);
        for (int i = 0; i < 10000; i++) { checkDouble(random.nextLong()); checkFloat(random.nextInt()); }
    }
    private void checkDouble(long bits) {
        double value = Double.longBitsToDouble(bits);
        if (Double.isNaN(value) || Double.isInfinite(value)) return;
        assertEquals(bits, Double.doubleToRawLongBits(Double.parseDouble(CNumber.literal(value))));
    }
    private void checkFloat(int bits) {
        float value = Float.intBitsToFloat(bits);
        if (Float.isNaN(value) || Float.isInfinite(value)) return;
        assertEquals(bits, Float.floatToRawIntBits(Float.parseFloat(CNumber.literal(value))));
    }
}
