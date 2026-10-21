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

/// Every operation that produces a Java `float`. Java rounds each result to the
/// nearest float; a target that computes in double precision and never rounds
/// carries the extra bits into the next operation. `Float.floatToIntBits` alone
/// cannot show that, because it rounds its argument on the way in, so each
/// result is also widened to a double, which keeps whatever bits it really has.
/// The operands are fields rather than constants so javac cannot fold them.
public class JsFloatRoundingApp {
    public static int result;

    static float tenth = 0.1f;
    static float three = 3f;
    static float third = 1f / 3f;
    static float seven = 7.3f;
    static int oddInt = 16777217;
    static long oddLong = 0x1000001000000001L;
    static long negLong = -0x1000001000000001L;
    static long smallLong = 16777217L;
    static double tenthDouble = 0.1d;

    static int mix(int hash, float value) {
        long wide = Double.doubleToLongBits((double) value);
        hash = hash * 31 + Float.floatToIntBits(value);
        hash = hash * 31 + (int) wide;
        hash = hash * 31 + (int) (wide >>> 32);
        return hash;
    }

    /// The straight-line shapes: one operation per statement.
    static int operations(int hash) {
        float a = tenth;
        float b = three;
        float c = third;
        float d = seven;
        hash = mix(hash, a + c);
        hash = mix(hash, a - c);
        hash = mix(hash, a * c);
        hash = mix(hash, a / b);
        hash = mix(hash, d % a);
        hash = mix(hash, -(a * c));
        // An expression javac leaves on the operand stack from end to end.
        hash = mix(hash, (a + c) * (d - a) / (c * c) - a * b);
        return hash;
    }

    static int conversions(int hash) {
        // 2^24 + 1 is the first int a float cannot hold.
        hash = mix(hash, (float) oddInt);
        // Above 2^53 a long reaches a float through a double only by rounding
        // twice; this one sits just past a float midpoint, where twice is wrong.
        hash = mix(hash, (float) oddLong);
        hash = mix(hash, (float) negLong);
        hash = mix(hash, (float) smallLong);
        hash = mix(hash, (float) tenthDouble);
        // Constants: FCONST and LDC.
        hash = mix(hash, 2f);
        hash = mix(hash, 0.1f);
        hash = mix(hash, 16777216.5f + 1f);
        return hash;
    }

    /// A frame timer: the float nearest 1/60, added 600 times.
    static int accumulate(int hash) {
        float t = 0f;
        int crossed = 0;
        for (int i = 0; i < 600; i++) {
            t += 1f / 60f;
            if (t >= 5f && crossed == 0) {
                crossed = i;
            }
        }
        hash = mix(hash, t);
        return hash * 31 + crossed;
    }

    public static int compute() {
        int hash = 17;
        hash = operations(hash);
        hash = conversions(hash);
        hash = accumulate(hash);
        float[] cells = new float[2];
        cells[0] = tenth * third;
        cells[1] = cells[0] + seven;
        hash = mix(hash, cells[1]);
        return hash;
    }

    public static void main(String[] args) {
        result = compute();
    }
}
