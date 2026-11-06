/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.runtime;

/// Python's character properties and case mappings, from [UnicodeTables].
///
/// The str methods answer from here rather than from `java.lang.Character`,
/// which on a device is the CLDC class: it knows ASCII and at most Latin-1,
/// so lowering a U with diaeresis left it unchanged there while the JVM,
/// asking the JDK, was right. The tables are CPython's own, so every target answers as
/// the CPython the conformance suite records against. Only the Basic
/// Multilingual Plane is covered; a surrogate has no properties and maps to
/// itself.
final class Unicode {
    static final int UPPER = 1;
    static final int LOWER = 2;
    static final int ALPHA = 4;
    static final int DECIMAL = 8;
    static final int DIGIT = 16;
    static final int NUMERIC = 32;
    static final int SPACE = 64;
    static final int CASED = 128;

    private static final byte[] FLAGS = new byte[0x10000];
    static final Mapping LOWER_MAP = new Mapping(UnicodeTables.LOWER);
    static final Mapping UPPER_MAP = new Mapping(UnicodeTables.UPPER);
    static final Mapping TITLE_MAP = new Mapping(UnicodeTables.TITLE);
    static final Mapping FOLD_MAP = new Mapping(UnicodeTables.FOLD);

    static {
        String runs = join(UnicodeTables.FLAGS);
        for (int i = 0; i < runs.length(); i += 2) {
            int start = runs.charAt(i);
            int end = i + 2 < runs.length() ? runs.charAt(i + 2) : 0x10000;
            byte f = (byte) runs.charAt(i + 1);
            for (int c = start; c < end; c++) {
                FLAGS[c] = f;
            }
        }
    }

    private Unicode() {
    }

    /// True when `c` has any of the property bits in `mask`.
    static boolean is(char c, int mask) {
        return (FLAGS[c] & mask) != 0;
    }

    private static String join(String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(p);
        }
        return sb.toString();
    }

    /// One case mapping: the chars that change, sorted, and what each becomes
    /// (more than one char for some: the sharp s upper-cases to "SS").
    static final class Mapping {
        private final char[] keys;
        private final String[] values;

        Mapping(String[] table) {
            String t = join(table);
            int n = 0;
            for (int i = 0; i < t.length(); i += 2 + t.charAt(i + 1)) {
                n++;
            }
            keys = new char[n];
            values = new String[n];
            int k = 0;
            for (int i = 0; i < t.length(); i += 2 + t.charAt(i + 1)) {
                keys[k] = t.charAt(i);
                values[k] = t.substring(i + 2, i + 2 + t.charAt(i + 1));
                k++;
            }
        }

        /// What `c` maps to, or null when it maps to itself.
        String get(char c) {
            int lo = 0;
            int hi = keys.length - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                char m = keys[mid];
                if (m < c) {
                    lo = mid + 1;
                } else if (m > c) {
                    hi = mid - 1;
                } else {
                    return values[mid];
                }
            }
            return null;
        }

        /// Appends what `c` maps to.
        void append(StringBuilder sb, char c) {
            String m = get(c);
            if (m == null) {
                sb.append(c);
            } else {
                sb.append(m);
            }
        }
    }
}
