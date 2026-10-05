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

/// Text inserted into a `SpannableStringBuilder` brings its spans along,
/// moved to where it lands.
public class SpannableInsertSpansTest {

    @Test
    public void appendedStyledTextKeepsItsSpans() {
        SpannableString red = new SpannableString("red");
        ForegroundColorSpan span = new ForegroundColorSpan(0xffff0000);
        red.setSpan(span, 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableStringBuilder sb = new SpannableStringBuilder("a ");
        sb.append(red);
        assertEquals(1, sb.getSpans(0, sb.length(), ForegroundColorSpan.class).length);
        assertEquals(2, sb.getSpanStart(span));
        assertEquals(5, sb.getSpanEnd(span));
        assertEquals(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, sb.getSpanFlags(span));
    }

    @Test
    public void concatKeepsTheSpansOfEverySegment() {
        SpannableString a = new SpannableString("ab");
        Object first = new Object();
        a.setSpan(first, 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableStringBuilder b = new SpannableStringBuilder("cd");
        Object second = new Object();
        b.setSpan(second, 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Spanned joined = (Spanned) TextUtils.concat(a, "-", b);
        assertEquals("ab-cd", joined.toString());
        assertEquals(1, joined.getSpanStart(first));
        assertEquals(2, joined.getSpanEnd(first));
        assertEquals(3, joined.getSpanStart(second));
        assertEquals(5, joined.getSpanEnd(second));
    }
}
