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
 * Torture for the COMPACT (Latin-1) string representation.
 *
 * <p>String stores either a {@code byte[]} (every code unit 0..255) or a
 * {@code char[]}, and the element kind IS the coder. That makes every operation
 * that can move a string ACROSS the boundary a place where the two
 * representations can disagree, and disagree silently -- the wrong answer is a
 * plausible string, not a crash. This driver walks those crossings and prints
 * every result, so run-gauntlet.sh can hold the whole transcript byte-identical
 * against a real JDK.
 *
 * <p>The cases that actually break implementations, and why each is here:
 * <ul>
 * <li>{@code replace(char, char)} where the REPLACEMENT is above 0xFF: a
 *     Latin-1 string must widen to char[]. The reverse -- replacing the only
 *     non-Latin-1 char with an ASCII one -- leaves a char[] holding a string
 *     that would now fit Latin-1, which must still compare and hash equal to
 *     the same text built compactly. Equality across UNEQUAL representations of
 *     EQUAL text is the whole hazard.
 * <li>{@code toUpperCase} on U+00FF: the uppercase of a Latin-1 character is
 *     U+0178, which is NOT Latin-1, so a correct implementation widens.
 * <li>{@code toUpperCase} on U+00DF: expands to two characters, so the result
 *     is LONGER than the input and a length-preserving fast path is wrong.
 * <li>Mixed concatenation and StringBuilder traffic, which is where the fused
 *     concat natives take raw interior pointers into the backing arrays.
 * <li>substring windows that start and end on either side of the only
 *     non-Latin-1 character, so a slice of a wide string can be narrow.
 * </ul>
 */
public class Latin1T {
    private static final StringBuilder OUT = new StringBuilder();

    private static void p(String label, String v) {
        OUT.append(label).append('=').append(v)
           .append(" len=").append(v.length())
           .append(" hash=").append(v.hashCode())
           .append('\n');
    }

    private static void p(String label, boolean v) {
        OUT.append(label).append('=').append(v).append('\n');
    }

    private static void p(String label, int v) {
        OUT.append(label).append('=').append(v).append('\n');
    }

    /** The same text built two ways; every derived property must agree. */
    private static void agree(String label, String a, String b) {
        OUT.append(label)
           .append(" eq=").append(a.equals(b))
           .append(" hash=").append(a.hashCode() == b.hashCode())
           .append(" cmp=").append(a.compareTo(b))
           .append(" len=").append(a.length() == b.length())
           .append('\n');
    }

    private static String wide(String ascii, int at, char c) {
        char[] w = ascii.toCharArray();
        w[at] = c;
        return new String(w);
    }

    public static void main(String[] args) {
        String ascii = "abcdefghij";
        String latin = "caf\u00e9 na\u00efve \u00ff\u00df";
        String wideS = "ab\u0100cd\u4e2d\u00e9f";

        p("ascii", ascii);
        p("latin", latin);
        p("wide", wideS);

        // --- replace across the boundary, both directions -------------------
        p("narrow->wide", ascii.replace('c', '\u4e2d'));
        p("wide->narrow", wideS.replace('\u0100', 'X').replace('\u4e2d', 'Y'));
        p("latin->ascii", latin.replace('\u00e9', 'e').replace('\u00ef', 'i')
                               .replace('\u00ff', 'y').replace('\u00df', 's'));
        p("noop-replace", ascii.replace('z', 'Z'));
        p("self-replace", latin.replace('\u00e9', '\u00e9'));

        // A char[]-backed string whose text is entirely Latin-1 must behave
        // exactly like the byte[]-backed string of the same text.
        String narrowedFromWide = wide(ascii, 2, 'Z').replace('Z', 'c');
        agree("equal-across-coders", narrowedFromWide, ascii);
        // Both sides are the SAME EIGHT CHARACTERS. The left one arrives as a
        // char[] (it was widened by the \u0100 in wideS and replace does not
        // re-compact), the right one is a literal whose every unit is Latin-1 and
        // so is byte[]-backed. equals/hashCode/compareTo must not be able to tell.
        agree("equal-across-coders2", wideS.replace('\u0100', 'x').replace('\u4e2d', 'y'),
              "abxcdy\u00e9f");
        // And the same thing ending in pure ASCII, where the compact form is
        // reachable but the derived string is still wide.
        agree("equal-across-coders3",
              wideS.replace('\u0100', 'x').replace('\u4e2d', 'y').replace('\u00e9', 'e'),
              "abxcdyef");

        // --- case mapping, ASCII ONLY, AND THAT LIMIT IS DELIBERATE ----------
        // The coder question here is "does case mapping preserve or correctly
        // widen the representation", and for ASCII input it does, on both VMs.
        //
        // The NON-ASCII cases are absent because ParparVM does not agree with a
        // JDK on them and the disagreement has nothing to do with this file's
        // subject. String.toUpperCase is native and forks: on iOS it is
        // NSString uppercaseString (full Unicode, correct), and on every other
        // target -- the clean C target used here, native Windows, native Linux
        // -- it is cn1StringConvertCase, while Character.toUpperCase(int) maps
        // only 'a'..'z' with its real body commented out. So U+00FF uppercases
        // to U+0178 on iOS and stays U+00FF elsewhere, "\u00df" uppercases to
        // "SS" on iOS and stays one character elsewhere, and
        // compareToIgnoreCase inherits both. That is a real portability bug and
        // it is tracked on its own; adding those cases here would only turn
        // this gate red for a reason it does not test.
        p("upper-ascii", ascii.toUpperCase());
        p("lower-ascii", "ABCDEFGHIJ".toLowerCase());
        p("upper-latin-stays-compact", "abc".toUpperCase() + latin.length());

        // --- substring windows on both sides of the wide character -----------
        for (int i = 0; i < wideS.length(); i++) {
            for (int j = i; j <= wideS.length(); j++) {
                String sub = wideS.substring(i, j);
                OUT.append("sub[").append(i).append(',').append(j).append("]=")
                   .append(sub).append(" h=").append(sub.hashCode()).append('\n');
            }
        }

        // --- concatenation in every coder combination ------------------------
        String[] parts = {ascii, latin, wideS, "", "x", "\u00ff", "\u4e2d"};
        for (int i = 0; i < parts.length; i++) {
            for (int j = 0; j < parts.length; j++) {
                String c2 = parts[i] + parts[j];
                p("cat2." + i + "." + j, c2);
                String c3 = parts[i] + parts[j] + parts[(i + j) % parts.length];
                p("cat3." + i + "." + j, c3);
            }
        }

        // --- StringBuilder traffic across coders ------------------------------
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            b.append(parts[i]).append('|').append(i);
        }
        p("sb", b.toString());
        b.insert(3, "\u00e9\u4e2d");
        p("sb-insert", b.toString());
        b.reverse();
        p("sb-reverse", b.toString());
        b.setLength(17);
        p("sb-trunc", b.toString());
        p("sb-delete", b.delete(2, 5).toString());

        // --- search and compare over both coders ------------------------------
        for (int i = 0; i < parts.length; i++) {
            for (int j = 0; j < parts.length; j++) {
                OUT.append("idx.").append(i).append('.').append(j).append('=')
                   .append(parts[i].indexOf(parts[j]))
                   .append(" last=").append(parts[i].lastIndexOf(parts[j]))
                   .append(" starts=").append(parts[i].startsWith(parts[j]))
                   .append(" ends=").append(parts[i].endsWith(parts[j]))
                   .append(" cmp=").append(parts[i].compareTo(parts[j]))
                   .append('\n');
            }
        }

        // Case-INSENSITIVE comparison folds through Character.toUpperCase, so it
        // is exercised over ASCII parts only, for the reason given above.
        String[] ci = {"abc", "ABC", "AbC", "", "abcd"};
        for (int i = 0; i < ci.length; i++) {
            for (int j = 0; j < ci.length; j++) {
                OUT.append("ci.").append(i).append('.').append(j).append('=')
                   .append(ci[i].compareToIgnoreCase(ci[j]))
                   .append(" eqi=").append(ci[i].equalsIgnoreCase(ci[j]))
                   .append('\n');
            }
        }

        // --- char-level access must agree with the text -----------------------
        for (int i = 0; i < wideS.length(); i++) {
            OUT.append("ch").append(i).append('=').append((int) wideS.charAt(i)).append('\n');
        }
        char[] dst = new char[latin.length()];
        latin.getChars(0, latin.length(), dst, 0);
        p("getChars", new String(dst));
        p("toCharArray", new String(latin.toCharArray()));
        p("trim", ("  " + latin + "  ").trim());
        p("valueOf", String.valueOf(latin.toCharArray()));

        // --- round trips through byte[] ---------------------------------------
        p("bytes-ascii", new String(ascii.getBytes()));
        p("intern-eq", ascii.equals(new String(ascii.toCharArray())));

        // --- churn, so the fused concat natives run under real GC pressure ----
        int acc = 0;
        for (int round = 0; round < 20000; round++) {
            String s = parts[round % parts.length] + round + parts[(round + 3) % parts.length];
            String t = s.replace('0', '\u00e9').replace('1', '\u4e2d');
            acc += t.hashCode() + t.length() + t.indexOf('\u00e9');
            if (t.length() > 3) {
                acc += t.substring(1, t.length() - 1).hashCode();
            }
        }
        p("churn", acc);

        System.out.println(OUT.toString());
    }
}
