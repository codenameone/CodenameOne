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
package com.codename1.android.rescompiler;

/// A typed resource value, the build-time twin of `android.util.TypedValue`.
///
/// The type codes and the packed "complex" encoding of dimensions and
/// fractions are Android's own, bit for bit, so the runtime can hand the data
/// word straight to `TypedValue.complexToDimension` and get the same pixel
/// answer an Android device would.
public final class Value {

    public static final int TYPE_NULL = 0x00;
    public static final int TYPE_REFERENCE = 0x01;
    public static final int TYPE_ATTRIBUTE = 0x02;
    public static final int TYPE_STRING = 0x03;
    public static final int TYPE_FLOAT = 0x04;
    public static final int TYPE_DIMENSION = 0x05;
    public static final int TYPE_FRACTION = 0x06;
    public static final int TYPE_INT_DEC = 0x10;
    public static final int TYPE_INT_HEX = 0x11;
    public static final int TYPE_INT_BOOLEAN = 0x12;
    public static final int TYPE_INT_COLOR_ARGB8 = 0x1c;
    public static final int TYPE_INT_COLOR_RGB8 = 0x1d;
    public static final int TYPE_INT_COLOR_ARGB4 = 0x1e;
    public static final int TYPE_INT_COLOR_RGB4 = 0x1f;

    /// `@null` is a reference to nothing; `@empty` is TYPE_NULL with data 1.
    public static final int DATA_NULL_UNDEFINED = 0;
    public static final int DATA_NULL_EMPTY = 1;

    public static final int COMPLEX_UNIT_SHIFT = 0;
    public static final int COMPLEX_UNIT_MASK = 0xf;
    public static final int COMPLEX_UNIT_PX = 0;
    public static final int COMPLEX_UNIT_DIP = 1;
    public static final int COMPLEX_UNIT_SP = 2;
    public static final int COMPLEX_UNIT_PT = 3;
    public static final int COMPLEX_UNIT_IN = 4;
    public static final int COMPLEX_UNIT_MM = 5;
    public static final int COMPLEX_UNIT_FRACTION = 0;
    public static final int COMPLEX_UNIT_FRACTION_PARENT = 1;
    public static final int COMPLEX_RADIX_SHIFT = 4;
    public static final int COMPLEX_RADIX_23p0 = 0;
    public static final int COMPLEX_RADIX_16p7 = 1;
    public static final int COMPLEX_RADIX_8p15 = 2;
    public static final int COMPLEX_RADIX_0p23 = 3;
    public static final int COMPLEX_MANTISSA_SHIFT = 8;
    public static final int COMPLEX_MANTISSA_MASK = 0xffffff;

    public final int type;
    public final int data;
    /// The string for TYPE_STRING, and the raw source text for every other
    /// type, so `AttributeSet.getAttributeValue` can still answer with it.
    public final String string;

    public Value(int type, int data, String string) {
        this.type = type;
        this.data = data;
        this.string = string;
    }

    public static Value string(String s) {
        return new Value(TYPE_STRING, 0, s);
    }

    public static Value reference(int id, String raw) {
        return new Value(TYPE_REFERENCE, id, raw);
    }

    public static Value integer(int v, String raw) {
        return new Value(TYPE_INT_DEC, v, raw);
    }

    public static Value bool(boolean v, String raw) {
        // Android stores true as -1 (all bits set) and false as 0.
        return new Value(TYPE_INT_BOOLEAN, v ? -1 : 0, raw);
    }

    public boolean isString() {
        return type == TYPE_STRING;
    }

    /// Packs a float with a unit into Android's complex format. The radix is
    /// chosen exactly as `TypedValue.floatToComplex` chooses it, so a value
    /// that is integral stays integral and `complexToFloat` round-trips it.
    public static int floatToComplex(float value, int unit) {
        int mantissa;
        int radix;
        if (value == (float) (int) value) {
            mantissa = (int) value;
            radix = COMPLEX_RADIX_23p0;
        } else {
            float abs = Math.abs(value);
            if (abs < 1f) {
                mantissa = Math.round(value * (1 << 23));
                radix = COMPLEX_RADIX_0p23;
            } else if (abs < (float) (1 << 8)) {
                mantissa = Math.round(value * (1 << 15));
                radix = COMPLEX_RADIX_8p15;
            } else if (abs < (float) (1 << 16)) {
                mantissa = Math.round(value * (1 << 7));
                radix = COMPLEX_RADIX_16p7;
            } else {
                mantissa = Math.round(value);
                radix = COMPLEX_RADIX_23p0;
            }
        }
        if (mantissa < -COMPLEX_MANTISSA_MASK / 2 - 1 || mantissa > COMPLEX_MANTISSA_MASK / 2) {
            throw new IllegalArgumentException("Value out of range for a resource dimension: " + value);
        }
        return ((mantissa & COMPLEX_MANTISSA_MASK) << COMPLEX_MANTISSA_SHIFT)
                | (radix << COMPLEX_RADIX_SHIFT) | (unit & COMPLEX_UNIT_MASK);
    }

    private static final float MANTISSA_MULT = 1.0f / (1 << COMPLEX_MANTISSA_SHIFT);
    private static final float[] RADIX_MULTS = {
            1.0f * MANTISSA_MULT, 1.0f / (1 << 7) * MANTISSA_MULT,
            1.0f / (1 << 15) * MANTISSA_MULT, 1.0f / (1 << 23) * MANTISSA_MULT
    };

    /// Inverse of [#floatToComplex(float,int)], ignoring the unit.
    public static float complexToFloat(int complex) {
        return (complex & (COMPLEX_MANTISSA_MASK << COMPLEX_MANTISSA_SHIFT))
                * RADIX_MULTS[(complex >> COMPLEX_RADIX_SHIFT) & 3];
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Value)) {
            return false;
        }
        Value v = (Value) o;
        if (type != v.type || data != v.data) {
            return false;
        }
        return string == null ? v.string == null : string.equals(v.string);
    }

    @Override
    public int hashCode() {
        return type * 31 + data + (string == null ? 0 : string.hashCode() * 7);
    }

    @Override
    public String toString() {
        return "Value[0x" + Integer.toHexString(type) + ", 0x" + Integer.toHexString(data)
                + (string == null ? "" : ", \"" + string + "\"") + "]";
    }
}
