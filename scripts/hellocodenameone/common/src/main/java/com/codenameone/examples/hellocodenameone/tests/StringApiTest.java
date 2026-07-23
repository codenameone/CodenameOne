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
package com.codenameone.examples.hellocodenameone.tests;

/**
 * End-to-end coverage for the String regex/literal helpers that Codename One
 * routes through the bytecode rewriter (replaceAll/replaceFirst) and the
 * literal CharSequence overload that lives directly on the String API
 * (replace(CharSequence, CharSequence)). Exercises the call sites on the
 * device so we catch any platform-specific divergence on iOS, Android and
 * JavaScript.
 */
public class StringApiTest extends BaseTest {

    @Override
    public boolean runTest() {
        try {
            // String.replace(CharSequence, CharSequence) - literal substitution
            assertEqual("ba", "aaa".replace("aa", "b"), "replace(aa,b) on aaa should be ba (left-to-right)");
            assertEqual("hello world", "hello there".replace("there", "world"), "single token replacement failed");
            assertEqual("X.X.X", "a.a.a".replace("a", "X"), "all-occurrence char-sequence replace failed");
            assertEqual("abc", "abc".replace("z", "Q"), "replace with no match should return original");
            CharSequence target = new StringBuilder("a");
            CharSequence repl = new StringBuilder("XY");
            assertEqual("XYbXY", "aba".replace(target, repl), "non-String CharSequence overload failed");

            // String.replaceAll - regex-driven substitution via JdkApiRewriteHelper -> RE
            assertEqual("XbXcX", "aabacaa".replaceAll("a+", "X"), "replaceAll greedy + quantifier failed");
            assertEqual("xBxB", "aBaB".replaceAll("a", "x"), "replaceAll literal token failed");
            assertEqual("ABC", "abc".replaceAll("[a-z]", "$0").toUpperCase(),
                    "replaceAll with $0 backreference / toUpperCase pipeline failed");
            assertEqual("--", "ab".replaceAll(".", "-"), "replaceAll '.' should match every character");
            assertEqual("nochange", "nochange".replaceAll("zzz", "X"),
                    "replaceAll with no matches should return original");

            // String.replaceFirst - regex-driven first-only substitution
            assertEqual("XbacaaB", "aabacaaB".replaceFirst("a+", "X"),
                    "replaceFirst should only replace the first regex match");
            assertEqual("xbab", "abab".replaceFirst("a", "x"),
                    "replaceFirst literal token failed");
            assertEqual("nochange", "nochange".replaceFirst("zzz", "X"),
                    "replaceFirst with no match should return original");

            // A ParparVM String can be fused with its backing array. A slice must
            // therefore own its backing storage: sharing the parent's inline array
            // leaves a dangling pointer after the parent is collected. The native
            // clean-target regression forces collection; these assertions keep the
            // compact-slice semantics covered on every device port.
            String latin1Slice = makeSlice("prefix-LATIN1-suffix", 7, 13);
            String utf16Slice = makeSlice("prefix-\u20acuro-suffix", 7, 11);
            assertEqual("LATIN1", latin1Slice, "Latin-1 substring lost its fused parent backing");
            assertEqual("\u20acuro", utf16Slice, "UTF-16 substring lost its fused parent backing");
            assertEqual("[LATIN1][\u20acuro]",
                    new StringBuilder().append('[').append(latin1Slice).append("][")
                            .append(utf16Slice).append(']').toString(),
                    "StringBuilder append should consume surviving slices safely");
        } catch (Throwable t) {
            fail("String API test failed: " + t);
            return false;
        }
        done();
        return true;
    }

    private String makeSlice(String text, int start, int end) {
        String parent = new StringBuilder(text.length()).append(text).toString();
        return parent.substring(start, end);
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
