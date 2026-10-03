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
public class JsIntMultiplyApp {
    public static int result;

    // Not inlined into constants: the multiply has to happen at run time.
    static int mul(int a, int b) {
        return a * b;
    }

    public static void main(String[] args) {
        int mask = 0;
        // An int product past 2^53 used to be computed as a double and truncated
        // afterwards, which rounds away the low bits Java keeps.
        if (mul(0x7fffffff, 0x7fffffff) == 1) mask |= 1;
        if (mul(123456789, 987654321) == -67153019) mask |= 2;
        int seed = 12345;
        for (int i = 0; i < 5; i++) {
            seed = seed * 1103515245 + 12345;
        }
        if (seed == -1038148470) mask |= 4;
        int hash = 0x811c9dc5;
        String text = "hello, world";
        for (int i = 0; i < text.length(); i++) {
            hash = (hash ^ text.charAt(i)) * 0x01000193;
        }
        if (hash == 1292805149) mask |= 8;
        if (mul(Integer.MIN_VALUE, -1) == Integer.MIN_VALUE) mask |= 16;
        result = mask;
        System.exit(mask);
    }
}
