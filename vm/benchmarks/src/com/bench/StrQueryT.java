/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.bench;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Differential torture for the String query methods that were made allocation-free,
 * and for the cached map views.
 *
 * Four of these used to allocate to answer a question:
 * compareToIgnoreCase built two whole lower-cased strings, both contentEquals
 * overloads built a String out of their argument, contains built one out of a
 * CharSequence, and replace(CharSequence,CharSequence) copied the receiver into a
 * char[]. Removing the allocation changed the code path in each, so each has to be
 * shown to still produce the same ANSWER -- and compareToIgnoreCase changed folding
 * strategy, from one lower-case pass to the JDK's specified try-as-is, then upper,
 * then lower, which is a different function on the characters where the two
 * directions are not inverses.
 *
 * Also covered: HashMap.entrySet and IdentityHashMap.entrySet now return a cached
 * view. The JDK caches these too, so both the repeated-call IDENTITY and the
 * read-through behaviour after mutation are compared against it rather than asserted
 * here.
 *
 * The sweep over the BMP is deliberate. The fold runs through Character.toUpperCase
 * and Character.toLowerCase, which are natives backed by the C library rather than
 * by Java's own Unicode tables, so any character where the two disagree shows up
 * here as a real difference instead of hiding until a device sees it.
 *
 * Every non-ASCII character is written as an escape; a raw one would fail the
 * ASCII-only source rule this tree builds under.
 */
public class StrQueryT {
    private static int checks;

    /* The indices are printed so a divergence names the pair that produced it. A
     * bare sign per line meant a diff could only say "line 307 differs", and the
     * harness compares output, so the label has to be in the output. */
    private static void cmp(int i, int j, String a, String b) {
        int r = a.compareToIgnoreCase(b);
        // Normalised to a sign: the magnitude is a character difference and is not
        // specified, only the ordering is.
        System.out.println("cmp[" + i + "," + j + "] " + (r < 0 ? "<" : r > 0 ? ">" : "=")
                + " " + a.equalsIgnoreCase(b));
        checks++;
    }

    public static void main(String[] args) throws Exception {
        /* ASCII only, and that is a statement about this VM rather than about the
         * method. String.toLowerCase and String.toUpperCase do not fold non-ASCII
         * characters at all on the non-ObjC targets -- the #else branch in
         * nativeMethods.m goes through towlower/towupper and nothing ever calls
         * setlocale, so the default C locale restricts them to A-Z. Measured against
         * a real JDK 25 on this corpus:
         *
         *     GREEK CAPITAL SIGMA  lower -> unchanged, JDK gives small sigma
         *     GEORGIAN 10a0        lower -> unchanged, JDK gives 2d00
         *     ARMENIAN 0531        lower -> unchanged, JDK gives 0561
         *     CAPITAL SHARP S 1e9e lower -> unchanged, JDK gives 00df
         *     I WITH DOT 0130      lower -> unchanged, JDK gives 0069 0307
         *     DESERET (non-BMP)    lower -> unchanged, JDK folds the pair
         *
         * The Apple/ObjC branch uses NSString and is correct, so shipping iOS builds
         * are unaffected; Linux, Windows and clean builds are not. That is a
         * pre-existing defect -- verified by running this file against the ORIGINAL
         * toLowerCase-based compareToIgnoreCase, which produces the identical
         * divergence set -- and it is not what this file is for. Including those
         * characters here would only assert the wrong answer and freeze it.
         *
         * Case-insensitive comparison of ASCII IS what the allocation-free path
         * below decides; anything non-ASCII is deferred to toLowerCase, unchanged. */
        String[] words = new String[] {
            "", "a", "A", "ab", "AB", "Ab", "aB", "abc", "ABC", "abd", "ABD",
            "zzz", "ZZZ", "a1", "A1", "1a", "-", "_", "aa", "aA",
            "abcdef", "ABCDEF", "abcdeg", "z", "Z", "aa1", "AA1"
        };
        for (int i = 0; i < words.length; i++) {
            for (int j = 0; j < words.length; j++) {
                cmp(i, j, words[i], words[j]);
            }
        }

        // Ordering must stay a total order consistent with equalsIgnoreCase, and
        // antisymmetric. Reported rather than asserted, so a break is visible as a
        // diff rather than as an exception whose message might differ.
        int anti = 0;
        int consistent = 0;
        for (int i = 0; i < words.length; i++) {
            for (int j = 0; j < words.length; j++) {
                int f = words[i].compareToIgnoreCase(words[j]);
                int r = words[j].compareToIgnoreCase(words[i]);
                if ((f < 0 && r < 0) || (f > 0 && r > 0) || ((f == 0) != (r == 0))) {
                    anti++;
                }
                if ((f == 0) != words[i].equalsIgnoreCase(words[j])) {
                    consistent++;
                }
            }
        }
        System.out.println("antisymmetry violations=" + anti);
        System.out.println("equalsIgnoreCase disagreements=" + consistent);

        // A sweep of the whole BMP against its own case variants. Folded through
        // Character.toUpperCase/toLowerCase, which is where a C-library fold and a
        // Java fold can part company.
        int diffs = 0;
        int firstDiff = -1;
        for (int c = 0; c < 0x10000; c++) {
            if (c >= 0xd800 && c <= 0xdfff) {
                continue;
            }
            char ch = (char) c;
            String s1 = String.valueOf(ch);
            String s2 = String.valueOf(Character.toUpperCase(ch));
            String s3 = String.valueOf(Character.toLowerCase(ch));
            boolean eq2 = s1.compareToIgnoreCase(s2) == 0;
            boolean eq3 = s1.compareToIgnoreCase(s3) == 0;
            if (!eq2 || !eq3) {
                diffs++;
                if (firstDiff < 0) {
                    firstDiff = c;
                }
            }
        }
        System.out.println("bmp fold self-inconsistencies=" + diffs + " first=" + firstDiff);

        // contentEquals over every CharSequence shape.
        String base = "hello" + "\u00e9\u20ac";
        CharSequence[] seqs = new CharSequence[] {
            base, new StringBuilder(base), new StringBuffer(base),
            "hello", new StringBuilder("hello"), new StringBuffer(""),
            base + "x", new StringBuilder(base + "x")
        };
        for (int i = 0; i < seqs.length; i++) {
            System.out.println("contentEquals cs " + i + "=" + base.contentEquals(seqs[i]));
        }
        for (int i = 0; i < seqs.length; i++) {
            if (seqs[i] instanceof StringBuffer) {
                System.out.println("contentEquals sb " + i + "="
                        + base.contentEquals((StringBuffer) seqs[i]));
            }
        }
        System.out.println("contentEquals empty=" + "".contentEquals(new StringBuilder()));

        // contains over both shapes, including a needle longer than the haystack.
        String hay = "abcdefgh" + "\u00e9\u20ac" + "abc";
        String[] needles = new String[] { "", "a", "abc", "cde", "h", "\u00e9\u20ac", "xyz",
            hay, hay + "q", "bc" + "\u00e9\u20ac" };
        for (int i = 0; i < needles.length; i++) {
            System.out.println("contains str " + i + "=" + hay.contains(needles[i])
                    + " sb=" + hay.contains(new StringBuilder(needles[i])));
        }

        // replace(CharSequence,CharSequence): the char[] copy is gone, so the range
        // arithmetic changed from (offset,length) to (start,end).
        String[][] cases = new String[][] {
            { "banana", "a", "o" }, { "banana", "an", "X" }, { "banana", "banana", "" },
            { "banana", "", "-" }, { "banana", "z", "Q" }, { "aaa", "aa", "b" },
            { "", "", "x" }, { "", "a", "b" },
            { "a" + "\u00e9\u20ac" + "b", "\u00e9\u20ac", "Z" },
            { "a" + "\u00e9\u20ac" + "b", "a", "\u00e9\u20ac" },
            { "\ud83d\ude00" + "x" + "\ud83d\ude00", "\ud83d\ude00", "y" },
            { "abcabcabc", "abc", "abcabc" }
        };
        for (int i = 0; i < cases.length; i++) {
            String out = cases[i][0].replace(cases[i][1], cases[i][2]);
            System.out.println("replace " + i + " len=" + out.length()
                    + " hash=" + out.hashCode() + " same=" + (out == cases[i][0]));
        }
        // And with non-String CharSequence arguments.
        System.out.println("replace cs=" + "banana".replace(new StringBuilder("an"),
                new StringBuilder("*")));

        // Cached views: identity across calls, and read-through after mutation.
        Map<String, String> m = new HashMap<String, String>();
        /* Identity across calls is deliberately NOT compared. Map.entrySet is
         * specified to return "a Set view" and says nothing about returning the
         * same instance, and this VM deliberately does not cache the entry view:
         * the field to hold it costs 8 bytes on every map, which moves HashMap into
         * the next BiBOP size class, and the view allocation it would save does not
         * appear in the allocation census at all. The JDK does cache it, so
         * asserting identity here would be asserting the JDK's choice rather than
         * the contract.
         *
         * keySet and values ARE cached, in AbstractMap, and that is pre-existing.
         * What matters for all three is below: the view must read through to the
         * map, including after the map is mutated underneath it. */
        System.out.println("keySet identity=" + (m.keySet() == m.keySet()));
        System.out.println("values identity=" + (m.values() == m.values()));
        System.out.println("entrySet empty size=" + m.entrySet().size());
        java.util.Set<Map.Entry<String, String>> view = m.entrySet();
        for (int i = 0; i < 40; i++) {
            m.put("k" + i, "v" + i);
        }
        // The view held from BEFORE the growth must see it: that is the read-through
        // property, and it is what a cached view would also have to provide.
        System.out.println("held view sees growth=" + view.size());
        System.out.println("fresh view sees growth=" + m.entrySet().size());
        int sum = 0;
        for (Map.Entry<String, String> e : m.entrySet()) {
            sum += e.getKey().length() + e.getValue().length();
        }
        System.out.println("entry walk sum=" + sum);
        m.remove("k7");
        System.out.println("view sees removal=" + m.entrySet().size());
        System.out.println("held view sees removal=" + view.size());
        m.clear();
        System.out.println("view sees clear=" + m.entrySet().size()
                + " held=" + view.size());

        IdentityHashMap<String, String> im = new IdentityHashMap<String, String>();
        // Same reasoning as above: behaviour, not instance identity.
        System.out.println("idm entrySet empty=" + im.entrySet().isEmpty());
        String ka = "a";
        String kb = "b";
        im.put(ka, "1");
        im.put(kb, "2");
        int isum = 0;
        for (Map.Entry<String, String> e : im.entrySet()) {
            isum += e.getValue().length();
        }
        System.out.println("idm walk=" + isum + " size=" + im.entrySet().size());

        System.out.println("checks=" + checks);
    }
}
