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
package android.text;

import com.codename1.androidcompat.testing.MainThreadRule;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// `TextUtils.split` takes a regular expression, as on Android, and keeps
/// trailing empty strings (`String.split(expression, -1)`).
public class TextUtilsSplitRegexTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static String split(String text, String expression) {
        return Arrays.asList(TextUtils.split(text, expression)).toString();
    }

    @Test
    public void regularExpressionsSplit() {
        assertEquals("[a, b, c]", split("a  b\tc", "\\s+"));
        assertEquals("[a, b, c]", split("a,b;c", "[,;]"));
        assertEquals("[a, b]", split("a|b", "\\|"));
        assertEquals("[1, 2, 3]", split("1.2.3", "\\."));
        assertEquals("[, a, , ]", split(";a;;", "[;]"));
    }

    @Test
    public void literalSeparatorsAndEmptyText() {
        assertEquals("[, a, b, ]", split(",a,b,", ","));
        assertEquals("[a, b, ]", split("a::b::", "::"));
        assertEquals("[abc]", split("abc", "x"));
        assertEquals(0, TextUtils.split("", "\\s+").length);
    }

    @Test
    public void emptyExpressionSplitsBetweenCharacters() {
        assertEquals("[a, b, c, ]", split("abc", ""));
        assertEquals("[x, ]", split("x", ""));
    }
}
