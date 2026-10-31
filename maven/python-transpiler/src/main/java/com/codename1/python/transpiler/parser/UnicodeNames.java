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
package com.codename1.python.transpiler.parser;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/// Resolves the name in a `\N{...}` escape at transpile time, so the
/// generated source holds the character itself and the runtime needs no
/// name database.
///
/// The table is the JDK's `Character.getName`, read backwards and built only
/// the first time a source uses the escape. The names the Unicode standard
/// derives by algorithm are computed instead, because the JDK spells them
/// differently from Python ("CJK UNIFIED IDEOGRAPHS 4E00" rather than
/// "CJK UNIFIED IDEOGRAPH-4E00"). Python's name aliases are not covered.
final class UnicodeNames {
    private static final String[] JAMO_L = {"G", "GG", "N", "D", "DD", "R", "M", "B", "BB", "S", "SS", "", "J", "JJ",
        "C", "K", "T", "P", "H"};
    private static final String[] JAMO_V = {"A", "AE", "YA", "YAE", "EO", "E", "YEO", "YE", "O", "WA", "WAE", "OE",
        "YO", "U", "WEO", "WE", "WI", "YU", "EU", "YI", "I"};
    private static final String[] JAMO_T = {"", "G", "GG", "GS", "N", "NJ", "NH", "D", "L", "LG", "LM", "LB", "LS",
        "LT", "LP", "LH", "M", "B", "BS", "S", "SS", "NG", "J", "C", "K", "T", "P", "H"};

    private static Map<String, Integer> table;

    private UnicodeNames() {
    }

    /// The code point named `name`, case-insensitively, or -1.
    static int lookup(String name) {
        String key = name.toUpperCase(Locale.ROOT);
        int cp = derived(key);
        if (cp >= 0) {
            return cp;
        }
        Integer v = table().get(key);
        return v == null ? -1 : v.intValue();
    }

    private static int derived(String key) {
        String[] hexPrefixes = {"CJK UNIFIED IDEOGRAPH-", "CJK COMPATIBILITY IDEOGRAPH-", "TANGUT IDEOGRAPH-",
            "KHITAN SMALL SCRIPT CHARACTER-", "NUSHU CHARACTER-"};
        for (String p : hexPrefixes) {
            if (key.startsWith(p) && key.length() > p.length()) {
                try {
                    int cp = Integer.parseInt(key.substring(p.length()), 16);
                    return cp <= Character.MAX_CODE_POINT && Character.isDefined(cp) ? cp : -1;
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        String hangul = "HANGUL SYLLABLE ";
        if (key.startsWith(hangul)) {
            String rest = key.substring(hangul.length());
            for (int l = 0; l < JAMO_L.length; l++) {
                for (int v = 0; v < JAMO_V.length; v++) {
                    for (int t = 0; t < JAMO_T.length; t++) {
                        if (rest.equals(JAMO_L[l] + JAMO_V[v] + JAMO_T[t])) {
                            return 0xAC00 + (l * JAMO_V.length + v) * JAMO_T.length + t;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private static synchronized Map<String, Integer> table() {
        if (table == null) {
            Map<String, Integer> m = new HashMap<String, Integer>();
            for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
                String n = Character.getName(cp);
                if (n != null && !m.containsKey(n)) {
                    m.put(n, Integer.valueOf(cp));
                }
            }
            table = m;
        }
        return table;
    }
}
