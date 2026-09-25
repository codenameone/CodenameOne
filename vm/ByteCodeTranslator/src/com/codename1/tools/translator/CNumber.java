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

/** Exact C99 literals, independent of the host VM's decimal rendering algorithm. */
public final class CNumber {
    private CNumber() { }

    public static String literal(float value) {
        if (Float.isNaN(value)) return "(0.0f/0.0f)";
        if (Float.isInfinite(value)) return value < 0 ? "(-1.0f/0.0f)" : "(1.0f/0.0f)";
        int bits = Float.floatToRawIntBits(value);
        int exponent = (bits >>> 23) & 255;
        int significand = bits & 0x7fffff;
        if (exponent != 0) significand |= 0x800000;
        return (bits < 0 ? "-0x" : "0x") + Integer.toHexString(significand)
                + "p" + (exponent == 0 ? -149 : exponent - 150) + "f";
    }

    public static String literal(double value) {
        if (Double.isNaN(value)) return "(0.0/0.0)";
        if (Double.isInfinite(value)) return value < 0 ? "(-1.0/0.0)" : "(1.0/0.0)";
        long bits = Double.doubleToRawLongBits(value);
        int exponent = (int) ((bits >>> 52) & 2047);
        long significand = bits & 0xfffffffffffffL;
        if (exponent != 0) significand |= 0x10000000000000L;
        return (bits < 0 ? "-0x" : "0x") + Long.toString(significand, 16)
                + "p" + (exponent == 0 ? -1074 : exponent - 1075);
    }
}
