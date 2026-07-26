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
package com.codename1.health.sensors;

/// Decoders for the two IEEE 11073-20601 floating-point formats used by
/// Bluetooth SIG medical characteristics.
///
/// The two formats are **not** interchangeable and appear on different
/// characteristics: blood pressure (0x2A35) and glucose (0x2A18) use the
/// 16-bit SFLOAT, while the health thermometer (0x2A1C) uses the 32-bit
/// FLOAT. Decoding one as the other yields plausible-looking nonsense
/// rather than an obvious failure, which is why they live here together
/// and are named for exactly what they are.
///
/// Both formats reserve values for "not a number", "not at this
/// resolution" and the infinities. A device that cannot obtain a reading
/// sends one of them, and a decoder that ignores that returns a blood
/// pressure of 2047 mmHg. Both methods here map every reserved value to
/// `Double.NaN`, so a caller that checks `isNaN` -- as the parsers in this
/// package do -- cannot propagate one.
final class Ieee11073 {

    private Ieee11073() {
    }

    /// Decodes a 16-bit SFLOAT: a signed 4-bit exponent in the top nibble
    /// and a signed 12-bit mantissa in the remainder.
    ///
    /// Returns `Double.NaN` for the five reserved mantissa values
    /// (0x07FF NaN, 0x0800 NRes, 0x07FE +INFINITY, 0x0802 -INFINITY,
    /// 0x0801 reserved).
    static double sfloat(int raw) {
        int mantissa = raw & 0x0FFF;
        int exponent = (raw >> 12) & 0x0F;
        if (mantissa >= 0x07FE && mantissa <= 0x0802) {
            return Double.NaN;
        }
        if (exponent > 7) {
            exponent -= 16;
        }
        if (mantissa > 0x07FF) {
            mantissa -= 0x1000;
        }
        return mantissa * Math.pow(10, exponent);
    }

    /// Decodes a 32-bit FLOAT: a signed 8-bit exponent in the top byte and
    /// a signed 24-bit mantissa in the remainder.
    ///
    /// Returns `Double.NaN` for the reserved mantissa values
    /// (0x007FFFFF NaN, 0x00800000 NRes, 0x007FFFFE +INFINITY,
    /// 0x00800002 -INFINITY, 0x00800001 reserved).
    static double float32(long raw) {
        int mantissa = (int) (raw & 0x00FFFFFFL);
        int exponent = (int) ((raw >> 24) & 0xFF);
        if (mantissa >= 0x007FFFFE && mantissa <= 0x00800002) {
            return Double.NaN;
        }
        if (exponent > 127) {
            exponent -= 256;
        }
        if (mantissa > 0x007FFFFF) {
            mantissa -= 0x01000000;
        }
        return mantissa * Math.pow(10, exponent);
    }
}
