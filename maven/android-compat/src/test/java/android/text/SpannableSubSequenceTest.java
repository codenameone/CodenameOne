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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// A subsequence keeps the spans of the text it was cut from, clipped to it
/// and moved to start at 0, as on Android. `SpannableString.subSequence`
/// used to drop every span, and a `SpannableStringBuilder` cut from anywhere
/// but 0 did too (one cut from 0 kept spans running past its end).
public class SpannableSubSequenceTest {

    private static final Object BOLD = new Object();
    private static final Object LINK = new Object();
    private static final Object TAIL = new Object();

    private static void styled(Spannable s) {
        // "Hello brave world": BOLD over "Hello brave", LINK over "brave",
        // TAIL over "world".
        s.setSpan(BOLD, 0, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(LINK, 6, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(TAIL, 12, 17, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static void assertCut(Spanned cut) {
        // Cut "lo brave" = [3, 11).
        assertEquals("lo brave", cut.toString());
        assertEquals(0, cut.getSpanStart(BOLD));
        assertEquals(8, cut.getSpanEnd(BOLD));
        assertEquals(3, cut.getSpanStart(LINK));
        assertEquals(8, cut.getSpanEnd(LINK));
        assertEquals("a span outside the cut is left behind", -1, cut.getSpanStart(TAIL));
    }

    @Test
    public void spannableStringSubSequenceKeepsSpans() {
        SpannableString s = new SpannableString("Hello brave world");
        styled(s);
        assertCut((Spanned) s.subSequence(3, 11));
    }

    @Test
    public void builderSubSequenceKeepsSpans() {
        SpannableStringBuilder b = new SpannableStringBuilder("Hello brave world");
        styled(b);
        assertCut((Spanned) b.subSequence(3, 11));
    }

    @Test
    public void builderCutFromStartClipsSpans() {
        SpannableStringBuilder b = new SpannableStringBuilder("Hello brave world");
        styled(b);
        Spanned cut = (Spanned) b.subSequence(0, 8);
        assertEquals(0, cut.getSpanStart(BOLD));
        assertEquals(8, cut.getSpanEnd(BOLD));
        assertEquals(6, cut.getSpanStart(LINK));
        assertEquals(8, cut.getSpanEnd(LINK));
        assertEquals(-1, cut.getSpanStart(TAIL));
    }
}
