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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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

package com.bench;

/** Does `1L << n` keep long semantics for n >= 31 on the clean target? */
public class LongShift {
    public static void main(String[] args) {
        for(int b = 29 ; b <= 34 ; b++) {
            long viaLiteral = 1L << b;
            long one = 1L;
            long viaVariable = one << b;
            System.out.println("SHIFT b=" + b
                    + " literal=" + viaLiteral
                    + " variable=" + viaVariable
                    + " expected=" + expected(b));
        }
    }
    /** Built by doubling, so it cannot share a shift bug with what it checks. */
    static long expected(int b) {
        long v = 1;
        for(int i = 0 ; i < b ; i++) {
            v = v + v;
        }
        return v;
    }
}
