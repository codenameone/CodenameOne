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

import android.text.style.ForegroundColorSpan;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

/// Spanned text is equal only to spanned text with the same characters and
/// spans. `SpannableString` used to compare the characters alone, so
/// differently styled text collapsed in sets and maps, and it answered
/// equal to a String that answered not equal back.
public class SpannableStringEqualityTest {

    @Test
    public void spansTakePartInEquality() {
        ForegroundColorSpan red = new ForegroundColorSpan(0xffff0000);
        SpannableString a = new SpannableString("hello");
        SpannableString b = new SpannableString("hello");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        a.setSpan(red, 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertNotEquals(a, b);
        b.setSpan(red, 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertNotEquals(a, b);
        b.setSpan(red, 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void neverEqualToAPlainString() {
        SpannableString s = new SpannableString("hello");
        assertFalse(s.equals("hello"));
        assertFalse("hello".equals(s));
    }

    @Test
    public void builderAgreesInBothDirections() {
        ForegroundColorSpan red = new ForegroundColorSpan(0xffff0000);
        SpannableString s = new SpannableString("hello");
        SpannableStringBuilder b = new SpannableStringBuilder("hello");
        b.setSpan(red, 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertNotEquals(s, b);
        assertNotEquals(b, s);
        s.setSpan(red, 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertEquals(s, b);
        assertEquals(b, s);
    }
}
