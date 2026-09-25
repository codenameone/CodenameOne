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
package com.bench;

/**
 * Torture for the toCharArray() elision pass (BytecodeMethod.elideToCharArrayScans).
 *
 * Half of these shapes the pass MUST rewrite and half it MUST REFUSE, and the output
 * has to be byte-identical to the host JVM either way -- a pass that silently rewrites
 * a mutation or an escape produces a wrong answer, not a crash.
 *
 * Covers both representations on purpose: a Latin-1 source is compact (byte[]) and a
 * CJK one is not, and the rewritten charAt has to decode both.
 *
 * VERIFIED NON-VACUOUS by reading the emitted C per method rather than trusting the
 * checksum. A pass that rewrote NOTHING would also produce a matching checksum:
 *
 *     indexed          REWRITTEN   toCharArray gone, charAt/length in
 *     forEach          REWRITTEN   toCharArray gone, charAt/length in
 *     constIndex       refused     mixed constant and computed index
 *     mutates          refused     CASTORE
 *     escapesToField   refused     stored to a static
 *     escapesByReturn  refused     returned
 *     computedIndex    refused     computed index
 */
public class ToCharT {
    static char[] keep;

    // MUST REWRITE: indexed scan reading length and elements
    static long indexed(String s) {
        char[] a = s.toCharArray();
        long n = 0;
        for (int i = 0; i < a.length; i++) { n += a[i] * 3 + i; }
        return n;
    }

    // MUST REWRITE: array for-each
    static long forEach(String s) {
        long n = 0;
        for (char c : s.toCharArray()) { n += c; }
        return n;
    }

    // REFUSED, and deliberately so: a[0] alone would qualify, but a[s.length() - 1] is a
    // COMPUTED index, and the pass refuses a site wholesale unless EVERY use qualifies.
    // Widening this needs operand-stack tracking, which is exactly the infrastructure
    // whose absence got the earlier concat rewrite withdrawn as a miscompile.
    static long constIndex(String s) {
        if (s.length() == 0) { return -1; }
        char[] a = s.toCharArray();
        return a[0] + a[s.length() - 1];
    }

    // MUST REFUSE: the array is mutated, and the String must NOT change
    static String mutates(String s) {
        char[] a = s.toCharArray();
        if (a.length > 0) { a[0] = 'Z'; }
        return new String(a) + "|" + s;
    }

    // MUST REFUSE: the array escapes to a field
    static long escapesToField(String s) {
        char[] a = s.toCharArray();
        keep = a;
        return a.length + keep.length;
    }

    // MUST REFUSE: the array is returned
    static char[] escapesByReturn(String s) { return s.toCharArray(); }

    // MUST REFUSE: computed index (the pass refuses rather than tracking the stack)
    static long computedIndex(String s) {
        char[] a = s.toCharArray();
        long n = 0;
        for (int i = 0; i + 1 < a.length; i++) { n += a[(i * 2) % a.length]; }
        return n;
    }

    public static void main(String[] args) {
        String[] in = { "", "a", "hello world", "MiXeD 123",
                        "\u00E9\u00E0\u00FF", "\u4E2D\u6587abc",   // Latin-1 (compact) and CJK (not)
                        "0123456789012345678901234567890123456789" };
        long ck = 0;
        for (String s : in) {
            ck += indexed(s) * 2;
            ck += forEach(s) * 3;
            ck += constIndex(s) * 5;
            ck += mutates(s).hashCode();
            ck += escapesToField(s) * 7;
            ck += escapesByReturn(s).length * 11;
            ck += computedIndex(s) * 13;
            // the source must be untouched by any of the above
            ck += s.hashCode() * 17 + s.length();
        }
        System.out.println("checksum=" + ck);
    }
}
