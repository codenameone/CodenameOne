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
/** Exercises native buffer ownership, including the double-realloc in issue #5963.
 * StringBuilder remains an unsynchronized Java API; this pins native memory safety. */
public class StringBuilderGrowthApp {
    private static volatile boolean start;
    private static volatile Throwable failure;
    private static StringBuilder builder;
    private static final String TEXT = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ\n";
    private static final int APPENDS = 1000;

    public static void main(String[] args) throws Exception {
        for (int round = 0; round < 16; round++) {
            final int mode = round % 8;
            final String token = token(mode);
            builder = new StringBuilder(mode == 0 ? 12638 : 16);
            Runnable[] jobs = new Runnable[5];
            for (int i = 0; i < 4; i++) jobs[i] = new Runnable() {
                public void run() {
                    char[] chars = token.toCharArray();
                    for (int n = 0; n < APPENDS; n++) {
                        switch (mode) {
                            case 0: builder.append(TEXT); break;
                            case 1: builder.append('x'); break;
                            case 2: builder.append(12345); break;
                            case 3: builder.append(Long.MIN_VALUE); break;
                            case 4: builder.append("short"); break;
                            case 5: builder.append("\u4321\u05d0"); break;
                            case 6: builder.append(chars, 0, chars.length); break;
                            default: builder.append((CharSequence) token, 0, token.length());
                        }
                    }
                }
            };
            jobs[4] = new Runnable() {
                public void run() {
                    for (int n = 0; n < 100; n++) {
                        String copy = builder.toString();
                        check(copy.length() % token.length() == 0, "partial native append");
                        for (int i = 0; i < copy.length(); i += 97)
                            check(copy.charAt(i) == token.charAt(i % token.length()), "corrupt snapshot");
                        Thread.yield();
                    }
                }
            };
            parallel(jobs);
            String result = builder.toString();
            check(result.length() == 4 * APPENDS * token.length(), "lost append");
            for (int i = 0; i < result.length(); i++)
                check(result.charAt(i) == token.charAt(i % token.length()), "corrupt output");
        }
        mixedGrowth();
        oppositeCopies();
        exceptionsReleaseAccess();
        System.out.println("BUILDER_GROWTH_OK: 16 growth rounds, snapshots, bidirectional copies, exception cleanup");
    }

    private static String token(int mode) {
        switch (mode) {
            case 0: return TEXT;
            case 1: return "x";
            case 2: return "12345";
            case 3: return "-9223372036854775808";
            case 4: return "short";
            case 5: return "\u4321\u05d0";
            case 6: return "array\u4321";
            default: return "range";
        }
    }

    private static void mixedGrowth() throws Exception {
        builder = new StringBuilder();
        parallel(new Runnable[] {
            new Runnable() { public void run() {
                for (int n = 0; n < APPENDS; n++) builder.append('x');
            }},
            new Runnable() { public void run() {
                for (int n = 0; n < APPENDS; n++) builder.append('\u4321');
            }},
            new Runnable() { public void run() {
                for (int n = 0; n < APPENDS; n++) builder.ensureCapacity(2000 + n * 40);
            }}
        });
        String text = builder.toString();
        check(text.length() == APPENDS * 2, "mixed growth length");
        int narrow = 0, wide = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == 'x') narrow++;
            else if (text.charAt(i) == '\u4321') wide++;
        }
        check(narrow == APPENDS && wide == APPENDS, "mixed growth changed coder");
        builder.setLength(4);
        builder.trimToSize();
        builder.append("\u4321");
        check(builder.charAt(4) == '\u4321', "wide buffer shrink and regrow");
    }

    private static void oppositeCopies() throws Exception {
        final StringBuilder left = new StringBuilder("abcdef");
        final StringBuilder right = new StringBuilder("abcdef");
        parallel(new Runnable[] {
            new Runnable() { public void run() {
                for (int n = 0; n < APPENDS; n++) left.append(right, 0, 6);
            }},
            new Runnable() { public void run() {
                for (int n = 0; n < APPENDS; n++) right.append(left, 0, 6);
            }}
        });
        check(left.length() == 6006 && right.length() == 6006, "builder range copies");
        check(left.toString().equals(right.toString()), "range copy content");
        left.append(left, 0, 6);
        check(left.length() == 6012, "self append");
    }

    private static void exceptionsReleaseAccess() throws Exception {
        builder = new StringBuilder("safe");
        try { builder.charAt(-1); throw new AssertionError("missing index exception"); }
        catch (IndexOutOfBoundsException expected) { }
        try { builder.append((char[]) null, 0, 1); throw new AssertionError("missing null exception"); }
        catch (NullPointerException expected) { }
        try { builder.getChars(0, 4, new char[1], 0); throw new AssertionError("missing destination exception"); }
        catch (IndexOutOfBoundsException expected) { }
        // A leaked reentrant monitor is invisible on this thread; another must enter.
        parallel(new Runnable[] { new Runnable() { public void run() { builder.append("!"); }} });
        check("safe!".equals(builder.toString()), "lock retained after exception");
    }

    private static void parallel(final Runnable[] jobs) throws Exception {
        start = false;
        failure = null;
        Thread[] workers = new Thread[jobs.length];
        for (int i = 0; i < jobs.length; i++) {
            final Runnable job = jobs[i];
            workers[i] = new Thread(new Runnable() {
                public void run() {
                    while (!start) Thread.yield();
                    try { job.run(); } catch (Throwable ex) { failure = ex; }
                }
            });
            workers[i].start();
        }
        start = true;
        for (int i = 0; i < workers.length; i++) workers[i].join();
        if (failure != null) throw new AssertionError(failure.toString());
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
