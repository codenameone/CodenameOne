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

/// A call whose `float` or `double` result is an operand of `FCMPx` or `DCMPx`.
/// The comparison reads each operand more than once, so the call must already
/// have happened by the time the comparison starts: every method here counts
/// its calls, and a comparison that runs one twice shows in the count and, for
/// the generator, in the answer as well.
public class JsFloatCompareOnceApp {
    public static int result;

    static int calls;
    static int seed;
    int ownCalls;

    static float nextFloat() {
        calls++;
        seed = seed * 1103515245 + 12345;
        return ((seed >>> 8) & 0xffff) / 65535f;
    }

    static double quarter() {
        calls++;
        return 0.25d;
    }

    static double scaled(double v) {
        calls++;
        return v * 2d;
    }

    float own() {
        ownCalls++;
        return 0.75f;
    }

    // INVOKESTATIC, F2D, LDC2_W, DCMPL
    static int floatCallAgainstDouble() {
        return nextFloat() > 0.5 ? 1 : 2;
    }

    // INVOKESTATIC, LDC, FCMPG
    static int floatCallAgainstFloat() {
        return nextFloat() < 0.5f ? 1 : 2;
    }

    // INVOKESTATIC, LDC2_W, DCMPL
    static int doubleCall() {
        return quarter() > 0.5d ? 1 : 2;
    }

    // DLOAD, INVOKESTATIC, DCMPG: the call is the right operand
    static int callOnTheRight(double limit) {
        return limit < quarter() ? 1 : 2;
    }

    // Both operands are calls.
    static int twoCalls() {
        return quarter() >= quarter() ? 1 : 2;
    }

    // The call's result goes through arithmetic before it is compared.
    static int callInArithmetic(double bias) {
        return quarter() * 4d + bias > 1.5d ? 1 : 2;
    }

    // A call with an argument, and a negated result.
    static int callWithArgument(double v) {
        return -scaled(v) < -1d ? 1 : 2;
    }

    // INVOKEVIRTUAL, LDC, FCMPL
    int virtualCall() {
        return own() > 0.5f ? 1 : 2;
    }

    public static void main(String[] args) {
        int mask = 0;

        // The JVM's answers for this seed, one call each: 0.8597..., 0.0162...
        seed = 12345;
        calls = 0;
        int a = floatCallAgainstDouble();
        if (a == 1 && calls == 1) {
            mask |= 1;
        }
        calls = 0;
        int b = floatCallAgainstFloat();
        if (b == 1 && calls == 1) {
            mask |= 2;
        }
        calls = 0;
        if (doubleCall() == 2 && calls == 1) {
            mask |= 4;
        }
        calls = 0;
        if (callOnTheRight(0.125d) == 1 && calls == 1) {
            mask |= 8;
        }
        calls = 0;
        if (twoCalls() == 1 && calls == 2) {
            mask |= 16;
        }
        calls = 0;
        if (callInArithmetic(1d) == 1 && calls == 1) {
            mask |= 32;
        }
        calls = 0;
        if (callWithArgument(0.75d) == 1 && calls == 1) {
            mask |= 64;
        }
        JsFloatCompareOnceApp app = new JsFloatCompareOnceApp();
        if (app.virtualCall() == 1 && app.ownCalls == 1) {
            mask |= 128;
        }
        result = mask;
    }
}
