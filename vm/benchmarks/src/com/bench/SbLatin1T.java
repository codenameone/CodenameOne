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
 * Torture for the COMPACT (Latin-1) representation of StringBuilder and StringBuffer.
 *
 * <p>Both hold either a byte per code unit or a char, chosen by a `wide` flag that
 * starts narrow and is supposed to flip the first time a unit above 0xFF is stored.
 * That makes every mutator a place where the flag can be missed, and a missed flip
 * is SILENT: the char is truncated to its low byte and the builder keeps working,
 * so the damage surfaces later as wrong text rather than as a crash.
 *
 * <p>The transitions worth covering, and why each is here:
 * <ul>
 * <li>Every mutator applied to a NARROW builder with a wide argument -- append(char),
 *     append(String), append(char[]), appendCodePoint, insert in all its overloads,
 *     setCharAt, replace. Each is a separate widen site in the implementation.
 * <li>The reverse direction: a WIDE builder taking narrow content must stay wide and
 *     must not re-compact behind the reader's back.
 * <li>A char[]-backed String whose content is entirely Latin-1. ParparVM can produce
 *     one (String.replace does not re-compact), so the builder cannot decide from the
 *     source's array kind alone -- it has to look at the content. OpenJDK never has
 *     this case because it compresses at construction, so this is a place our
 *     implementation must do MORE than the reference, while still agreeing with it.
 * <li>Surrogate pairs via appendCodePoint and reverse(), which is specified to keep
 *     pairs intact rather than reversing their halves.
 * <li>setLength growth, which zero-fills, and delete/replace spanning the boundary.
 * </ul>
 *
 * <p>NOT COVERED, because vm/JavaAPI's StringBuilder does not declare them at all:
 * appendCodePoint, replace(int,int,String), indexOf(String), lastIndexOf(String),
 * codePointAt and codePointCount. That gap is not this file's to fix and predates
 * the compact representation (HEAD does not have them either), but it is why
 * surrogates are appended here as explicit char pairs and why the searching and
 * replacing cases route through String instead.
 *
 * <p>capacity() is deliberately never printed: it is implementation-defined and
 * would diverge from the reference JDK for reasons that have nothing to do with
 * the representation under test.
 */
public class SbLatin1T {
    private static final StringBuilder OUT = new StringBuilder();

    private static final char WIDE = '\u4e2d';
    private static final char HIGH = '\u00ff';
    private static final char ABOVE = '\u0100';

    private static void p(String label, CharSequence v) {
        OUT.append(label).append('=').append(v.toString())
           .append(" len=").append(v.length())
           .append(" hash=").append(v.toString().hashCode())
           .append('\n');
    }

    private static void p(String label, int v) {
        OUT.append(label).append('=').append(v).append('\n');
    }

    /** A String that is all Latin-1 but whose backing array may still be char[]. */
    private static String narrowTextWideArray() {
        return ("ab" + ABOVE + "cd").replace(ABOVE, 'x');
    }

    private static StringBuilder sb(String seed) {
        return new StringBuilder(seed);
    }

    private static void mutators(String tag, String seed) {
        // append, one mutator per fresh builder so each widen site is isolated
        p(tag + ".appendChar.narrow", sb(seed).append('z'));
        p(tag + ".appendChar.high", sb(seed).append(HIGH));
        p(tag + ".appendChar.wide", sb(seed).append(WIDE));
        p(tag + ".appendChar.above", sb(seed).append(ABOVE));
        p(tag + ".appendStr.narrow", sb(seed).append("plain"));
        p(tag + ".appendStr.wide", sb(seed).append("w" + WIDE + "x"));
        p(tag + ".appendStr.latin1ViaWideArray", sb(seed).append(narrowTextWideArray()));
        p(tag + ".appendChars.wide", sb(seed).append(new char[] {'a', WIDE, 'b'}));
        p(tag + ".appendChars.narrow", sb(seed).append(new char[] {'a', 'b'}));
        p(tag + ".appendCharsRange", sb(seed).append(new char[] {'a', WIDE, 'b', 'c'}, 1, 2));
        p(tag + ".appendCs.wide", sb(seed).append((CharSequence) ("q" + WIDE)));
        p(tag + ".appendCsRange", sb(seed).append((CharSequence) ("q" + WIDE + "r"), 0, 2));
        p(tag + ".appendNullStr", sb(seed).append((String) null));
        p(tag + ".appendNullObj", sb(seed).append((Object) null));
        p(tag + ".appendObj", sb(seed).append((Object) ("o" + WIDE)));
        p(tag + ".appendSb", sb(seed).append(new StringBuilder("s" + WIDE)));
        p(tag + ".appendInt", sb(seed).append(-12345));
        p(tag + ".appendLong", sb(seed).append(-9007199254740993L));
        p(tag + ".appendBool", sb(seed).append(true).append(false));
        p(tag + ".appendFloat", sb(seed).append(1.5f));
        p(tag + ".appendDouble", sb(seed).append(0.25d));
        p(tag + ".appendCodePointBmp", sb(seed).append(WIDE));
        p(tag + ".appendSurrogatePair", sb(seed).append('\ud83d').append('\ude00'));
        p(tag + ".appendCodePointAscii", sb(seed).append('A'));

        // insert at the front, the middle and the end -- different move paths
        for (int at = 0; at <= seed.length(); at += Math.max(1, seed.length())) {
            p(tag + ".insertStr.wide@" + at, sb(seed).insert(at, "i" + WIDE));
            p(tag + ".insertStr.narrow@" + at, sb(seed).insert(at, "ii"));
            p(tag + ".insertChar.wide@" + at, sb(seed).insert(at, WIDE));
            p(tag + ".insertChar.narrow@" + at, sb(seed).insert(at, 'c'));
            p(tag + ".insertInt@" + at, sb(seed).insert(at, 42));
            p(tag + ".insertBool@" + at, sb(seed).insert(at, true));
            p(tag + ".insertObj@" + at, sb(seed).insert(at, (Object) ("O" + WIDE)));
            p(tag + ".insertCs@" + at, sb(seed).insert(at, (CharSequence) ("C" + WIDE)));
            p(tag + ".insertCsRange@" + at,
              sb(seed).insert(at, (CharSequence) ("C" + WIDE + "D"), 0, 2));
            p(tag + ".insertChars@" + at, sb(seed).insert(at, new char[] {'p', WIDE}));
            p(tag + ".insertLatin1ViaWideArray@" + at,
              sb(seed).insert(at, narrowTextWideArray()));
        }

        // in-place overwrite: the widen decision is per stored unit
        if (seed.length() > 0) {
            StringBuilder a = sb(seed); a.setCharAt(0, WIDE);
            p(tag + ".setCharAt.wide", a);
            StringBuilder b = sb(seed); b.setCharAt(0, 'n');
            p(tag + ".setCharAt.narrow", b);
            StringBuilder c = sb(seed); c.setCharAt(seed.length() - 1, HIGH);
            p(tag + ".setCharAt.high", c);
            p(tag + ".deleteCharAt0", sb(seed).deleteCharAt(0));
            p(tag + ".replace.wide", sb(seed).delete(0, 1).insert(0, "R" + WIDE));
            p(tag + ".replace.narrow", sb(seed).delete(0, 1).insert(0, "RR"));
            p(tag + ".delete.all", sb(seed).delete(0, seed.length()));
            p(tag + ".delete.mid", sb(seed).delete(0, 1));
        }

        p(tag + ".reverse", sb(seed).reverse());
        p(tag + ".reverse.twice", sb(seed).reverse().reverse());
        p(tag + ".setLength.grow", sb(seed).append('x'));
        StringBuilder grown = sb(seed); grown.setLength(seed.length() + 3);
        p(tag + ".setLength.padded", grown);
        StringBuilder shrunk = sb(seed); shrunk.setLength(Math.max(0, seed.length() - 1));
        p(tag + ".setLength.shrunk", shrunk);
        StringBuilder zeroed = sb(seed); zeroed.setLength(0);
        p(tag + ".setLength.zero", zeroed);

        // readers must agree with the text whatever the coder is
        StringBuilder r = sb(seed).append(WIDE).append('a').append(HIGH);
        p(tag + ".read.toString", r);
        p(tag + ".read.substring", r.toString().substring(0, Math.min(3, r.length())));
        p(tag + ".read.subSequence", r.subSequence(0, Math.min(3, r.length())));
        p(tag + ".read.indexOfWide", r.toString().indexOf(String.valueOf(WIDE)));
        p(tag + ".read.lastIndexOfA", r.toString().lastIndexOf("a"));
        for (int i = 0; i < r.length(); i++) {
            OUT.append(tag).append(".ch").append(i).append('=')
               .append((int) r.charAt(i)).append('\n');
        }
        char[] dst = new char[r.length()];
        r.getChars(0, r.length(), dst, 0);
        p(tag + ".read.getChars", new String(dst));
    }

    /** The same surface on StringBuffer, which is a separate implementation. */
    private static void bufferMutators(String tag, String seed) {
        p(tag + ".appendChar.wide", new StringBuffer(seed).append(WIDE));
        p(tag + ".appendChar.narrow", new StringBuffer(seed).append('z'));
        p(tag + ".appendStr.wide", new StringBuffer(seed).append("w" + WIDE));
        p(tag + ".appendStr.latin1ViaWideArray",
          new StringBuffer(seed).append(narrowTextWideArray()));
        p(tag + ".appendChars.wide", new StringBuffer(seed).append(new char[] {WIDE, 'a'}));
        p(tag + ".appendSurrogatePair", new StringBuffer(seed).append('\ud83d').append('\ude00'));
        p(tag + ".insertStr.wide", new StringBuffer(seed).insert(0, "i" + WIDE));
        p(tag + ".insertChar.wide", new StringBuffer(seed).insert(0, WIDE));
        p(tag + ".reverse", new StringBuffer(seed).reverse());
        if (seed.length() > 0) {
            StringBuffer a = new StringBuffer(seed); a.setCharAt(0, WIDE);
            p(tag + ".setCharAt.wide", a);
            p(tag + ".replace.wide", new StringBuffer(seed).delete(0, 1).insert(0, "R" + WIDE));
            p(tag + ".delete.mid", new StringBuffer(seed).delete(0, 1));
        }
        StringBuffer padded = new StringBuffer(seed);
        padded.setLength(seed.length() + 2);
        p(tag + ".setLength.padded", padded);
        p(tag + ".toString", new StringBuffer(seed).append(WIDE).append('a'));
    }

    public static void main(String[] args) {
        // Seeds chosen so the builder starts in each coder state, including empty.
        String[] seeds = {
            "",                       // empty, narrow
            "abc",                    // narrow
            "caf\u00e9",              // narrow, but uses the top of Latin-1
            "ab" + WIDE + "cd",       // already wide
            narrowTextWideArray(),    // Latin-1 text that may sit in a char[]
        };
        String[] names = {"empty", "ascii", "latin1", "wide", "narrowViaWideArray"};
        for (int i = 0; i < seeds.length; i++) {
            mutators("sb." + names[i], seeds[i]);
            bufferMutators("buf." + names[i], seeds[i]);
        }

        // Surrogate handling: reverse() must keep a pair intact.
        StringBuilder surro = new StringBuilder();
        surro.append('\ud83d').append('\ude00').append('a').append('\ud83d').append('\ude01');
        p("surro", surro);
        p("surro.reverse", new StringBuilder(surro.toString()).reverse());
        p("surro.charAt0", (int) surro.charAt(0));
        p("surro.charAt1", (int) surro.charAt(1));

        // A narrow builder that widens mid-stream and then keeps taking narrow
        // content must not lose or re-compact anything already written.
        StringBuilder mixed = new StringBuilder();
        int acc = 0;
        for (int i = 0; i < 300; i++) {
            mixed.append((char) ('a' + (i % 26)));
            if (i == 100) mixed.append(WIDE);
            if (i == 150) mixed.append(HIGH);
            if (i % 37 == 0) mixed.insert(0, (char) ('A' + (i % 26)));
            acc += mixed.length();
        }
        p("mixed", mixed);
        p("mixed.acc", acc);
        p("mixed.reversed", new StringBuilder(mixed.toString()).reverse());

        // Churn, so the fused/inline builder storage runs under real GC pressure.
        long ck = 0;
        for (int round = 0; round < 20000; round++) {
            StringBuilder b = new StringBuilder();
            b.append(round).append('-');
            if ((round & 3) == 0) b.append(WIDE);
            if ((round & 7) == 0) b.append(HIGH);
            b.append("tail");
            String s = b.toString();
            ck += s.hashCode() + s.length();
            ck += s.indexOf(WIDE) + new StringBuilder(s).reverse().toString().hashCode();
        }
        p("churn", (int) ck);

        System.out.println(OUT.toString());
    }
}
