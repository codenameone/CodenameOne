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
package dart.runtime;

import dart.core.TypeError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DartRuntimeTest {

    @Test
    public void modIsNonNegativeLikeDart() {
        assertEquals(2, DartRuntime.mod(-3, 5));
        assertEquals(3, DartRuntime.mod(3, 5));
        assertEquals(2, DartRuntime.mod(-3, -5));
        assertEquals(1.5, DartRuntime.mod(-3.5, 5.0), 1e-9);
    }

    @Test
    public void tdivTruncatesTowardZero() {
        assertEquals(-2, DartRuntime.tdiv(-7, 3));
        assertEquals(2, DartRuntime.tdiv(7, 3));
        assertEquals(-2, DartRuntime.tdiv(-7.5, 3.0));
    }

    @Test
    public void doubleStrMatchesDartFormatting() {
        assertEquals("1.0", DartRuntime.doubleStr(1.0));
        assertEquals("-1.0", DartRuntime.doubleStr(-1.0));
        assertEquals("0.0", DartRuntime.doubleStr(0.0));
        assertEquals("-0.0", DartRuntime.doubleStr(-0.0));
        assertEquals("2.5", DartRuntime.doubleStr(2.5));
        assertEquals("NaN", DartRuntime.doubleStr(Double.NaN));
        assertEquals("Infinity", DartRuntime.doubleStr(Double.POSITIVE_INFINITY));
        assertEquals("-Infinity", DartRuntime.doubleStr(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void strHandlesNullAndDoubles() {
        assertEquals("null", DartRuntime.str((Object) null));
        assertEquals("3.0", DartRuntime.str((Object) Double.valueOf(3)));
        assertEquals("7", DartRuntime.str(7L));
        assertEquals("true", DartRuntime.str(true));
    }

    @Test
    public void nnThrowsDartTypeError() {
        assertEquals("x", DartRuntime.nn("x"));
        assertThrows(TypeError.class, () -> DartRuntime.nn(null));
    }

    @Test
    public void eqIsNullSafe() {
        assertTrue(DartRuntime.eq(null, null));
        assertFalse(DartRuntime.eq(null, "a"));
        assertFalse(DartRuntime.eq("a", null));
        assertTrue(DartRuntime.eq("a", "a"));
    }

    @Test
    public void printSinkCapturesOutput() {
        StringBuilder sb = new StringBuilder();
        DartRuntime.setPrintSink(s -> sb.append(s).append('\n'));
        try {
            DartRuntime.print("hello");
            DartRuntime.print(42L);
            DartRuntime.print(1.0);
        } finally {
            DartRuntime.setPrintSink(null);
        }
        assertEquals("hello\n42\n1.0\n", sb.toString());
    }

    // --- == across int and double ---------------------------------------------

    @Test
    public void eqComparesBoxedNumbersByValueAcrossIntAndDouble() {
        assertTrue(DartRuntime.eq(Long.valueOf(1), Double.valueOf(1.0)), "1 == 1.0");
        assertTrue(DartRuntime.eq(Double.valueOf(1.0), Long.valueOf(1)));
        assertTrue(DartRuntime.eq(Double.valueOf(0.0), Double.valueOf(-0.0)), "0.0 == -0.0");
        assertFalse(DartRuntime.eq(Double.valueOf(Double.NaN), Double.valueOf(Double.NaN)), "NaN is never == itself");
        // Recorded from the Dart 3.9 VM: == converts the int to double first.
        assertTrue(DartRuntime.eq(Long.valueOf(9007199254740993L), Double.valueOf(9007199254740992.0)),
                "2^53 + 1 == 2^53.0 on the VM");
        assertTrue(DartRuntime.eq(Long.valueOf(Long.MAX_VALUE), Double.valueOf(9.223372036854775807E18)),
                "the largest int == 2^63 on the VM");
        assertFalse(DartRuntime.eq(Long.valueOf(1), Double.valueOf(1.5)));
        dart.core.DartList<Object> nums = new dart.core.DartList<Object>();
        nums.add(Long.valueOf(1));
        assertTrue(nums.contains(Double.valueOf(1.0)), "List<num>.contains(1.0) finds the int 1");
    }

    // --- toStringAsFixed ------------------------------------------------------

    @Test
    public void toStringAsFixedUsesTheExactValueAcrossTheWholeRange() {
        assertEquals("1.00000000000000000000", DartRuntime.toStringAsFixed(1.0, 20),
                "20 digits no longer overflows a long");
        assertEquals("0.10000000000000000555", DartRuntime.toStringAsFixed(0.1, 20), "the double's exact digits");
        assertEquals("1.00", DartRuntime.toStringAsFixed(1.005, 2), "1.005 is really 1.00499...");
        assertEquals("4321.123", DartRuntime.toStringAsFixed(4321.12345678, 3));
        assertEquals("4321.12346", DartRuntime.toStringAsFixed(4321.12345678, 5));
        assertEquals("123456789012345.000", DartRuntime.toStringAsFixed(123456789012345.0, 3));
        assertEquals("10000000000000000.0000", DartRuntime.toStringAsFixed(1e16, 4));
        assertEquals("3", DartRuntime.toStringAsFixed(2.5, 0), "an exact tie rounds up");
        assertEquals("-1.5", DartRuntime.toStringAsFixed(-1.5, 1));
        assertEquals("-0.00", DartRuntime.toStringAsFixed(-0.001, 2));
        assertEquals("0.00000000000000000000", DartRuntime.toStringAsFixed(Double.MIN_VALUE, 20));
        assertEquals("1e+21", DartRuntime.toStringAsFixed(1e21, 2), "1e21 and beyond print as toString");
        assertEquals("999999999999999868928.00", DartRuntime.toStringAsFixed(999999999999999900000.0, 2));
        assertThrows(dart.core.RangeError.class, () -> DartRuntime.toStringAsFixed(1.0, 21));
        assertThrows(dart.core.RangeError.class, () -> DartRuntime.toStringAsFixed(1.0, -1));
    }

    // --- shifts and rounding ---------------------------------------------------

    @Test
    public void shiftsFollowDartsCountRules() {
        assertEquals(0, DartRuntime.shr(1, 64));
        assertEquals(-1, DartRuntime.shr(-8, 64));
        assertEquals(0, DartRuntime.shl(1, 64));
        assertEquals(0, DartRuntime.ushr(-1, 64));
        assertEquals(8, DartRuntime.shl(1, 3));
        assertThrows(dart.core.ArgumentError.class, () -> DartRuntime.shl(1, -1));
    }

    @Test
    public void roundingGoesHalfAwayFromZero() {
        assertEquals(-2, DartRuntime.round(-1.5));
        assertEquals(3, DartRuntime.round(2.5));
        assertEquals(0, DartRuntime.round(0.49999999999999994), "not floor(d + 0.5)");
        assertEquals(-2.0, DartRuntime.roundToDouble(-1.5), 0.0);
        assertEquals("-0.0", DartRuntime.doubleStr(DartRuntime.roundToDouble(-0.4)), "the sign of zero is kept");
        assertThrows(dart.core.UnsupportedError.class, () -> DartRuntime.round(Double.NaN));
    }

    @Test
    public void dynamicShiftsFollowTheTypedRules() {
        assertEquals(Long.valueOf(0), DartRuntime.dynBinary("<<", 1L, 64L));
        assertThrows(dart.core.ArgumentError.class, () -> DartRuntime.dynBinary("<<", 1L, -1L));
    }
}
