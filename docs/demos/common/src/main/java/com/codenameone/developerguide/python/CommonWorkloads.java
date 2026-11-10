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
// tag::python-interop-java-003[]
package com.codenameone.developerguide.python;

public final class CommonWorkloads {
    public static long intArithmetic() {
        int a = 0x12345678;
        int b = 0x9E3779B9;
        long checksum = 0;
        for (int i = 0; i < 40000000; i++) {
            a = (a * 1103515245 + 12345) ^ (b >>> 3);
            b = (b + a) * 5 - (a << 7);
            checksum += (a ^ b) & 0xFFFF;
        }
        return checksum + a + b;
    }
}
// end::python-interop-java-003[]
