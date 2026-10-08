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

import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// `AllCaps` keeps the styling of the text it uppercases.
public class AllCapsSpansTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void uppercasedStyledTextKeepsItsSpans() {
        SpannableString src = new SpannableString("xabcdx");
        Object inner = new Object();
        Object spanning = new Object();
        Object outside = new Object();
        src.setSpan(inner, 2, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        src.setSpan(spanning, 0, 6, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        src.setSpan(outside, 5, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        CharSequence r = new InputFilter.AllCaps().filter(src, 1, 5, new SpannableStringBuilder(""), 0, 0);
        assertEquals("ABCD", r.toString());
        assertTrue(r instanceof Spanned);
        Spanned out = (Spanned) r;
        assertEquals(1, out.getSpanStart(inner));
        assertEquals(2, out.getSpanEnd(inner));
        assertEquals(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, out.getSpanFlags(inner));
        assertEquals(0, out.getSpanStart(spanning));
        assertEquals(4, out.getSpanEnd(spanning));
        assertEquals(-1, out.getSpanStart(outside));
    }
}
