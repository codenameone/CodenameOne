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

/**
 * Java int arithmetic wraps at 32 bits, and a JavaScript number does not. Each
 * check sets one bit, so a failure names the operation that went wrong. The
 * operands come from static fields so javac cannot fold them into constants.
 */
public class JsIntOverflowApp {
    static int result;
    static int seed = 0x12345678;
    static int one = 1;
    static int max = Integer.MAX_VALUE;
    static int min = Integer.MIN_VALUE;
    static int big = 65536;

    public static void main(String[] args) {
        int score = 0;

        // IMUL whose true product is past 2^53: JavaScript rounds it before any
        // |0 can wrap it, so the low 32 bits Java keeps are already gone.
        if (seed * 1103515245 == 191979800) {
            score |= 1;
        }
        // IADD / ISUB overflow must wrap before it is compared.
        if (max + one < 0) {
            score |= 2;
        }
        if (min - one > 0) {
            score |= 4;
        }
        // ...and before it is widened or printed.
        if ((long) (max + one) == -2147483648L) {
            score |= 8;
        }
        if (String.valueOf(max + one).equals("-2147483648")) {
            score |= 16;
        }
        // 2^16 * 2^16 is exactly 2^32, which wraps to 0.
        if (big * big == 0) {
            score |= 32;
        }
        // The first thousand steps of vm/benchmarks' intArithmetic workload.
        int a = seed;
        int b = 0x9E3779B9;
        long checksum = 0;
        for (int i = 0; i < 1000; i++) {
            a = (a * 1103515245 + 12345) ^ (b >>> 3);
            b = (b + a) * 5 - (a << 7);
            checksum += (a ^ b) & 0xFFFF;
        }
        if (checksum + a + b == -141689531L) {
            score |= 64;
        }
        result = score;
    }
}
