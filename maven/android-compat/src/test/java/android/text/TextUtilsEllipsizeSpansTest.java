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

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class TextUtilsEllipsizeSpansTest {
    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void retainedTextKeepsClippedAndShiftedSpansInEveryMode() {
        AndroidTestSupport.context();
        TextPaint paint = new TextPaint() {
            @Override
            public float measureText(String text) { return text.length(); }
        };
        TextUtils.TruncateAt[] modes = {TextUtils.TruncateAt.START,
                TextUtils.TruncateAt.MIDDLE, TextUtils.TruncateAt.END};
        String[] expected = {"\u2026ghij", "ab\u2026ij", "abcd\u2026"};
        for (int i = 0; i < modes.length; i++) {
            SpannableString source = new SpannableString("abcdefghij");
            Object whole = new Object();
            Object head = new Object();
            Object tail = new Object();
            Object removed = new Object();
            source.setSpan(whole, 0, 10, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            source.setSpan(head, 0, 3, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            source.setSpan(tail, 7, 10, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            source.setSpan(removed, 4, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            CharSequence result = TextUtils.ellipsize(source, paint, 5, modes[i]);
            assertEquals(expected[i], result.toString());
            assertTrue(result instanceof Spanned);
            Spanned spans = (Spanned) result;
            assertEquals(-1, spans.getSpanStart(removed));
            assertEquals(i == 0 ? 1 : 0, spans.getSpanStart(whole));
            assertEquals(i == 2 ? 4 : 5, spans.getSpanEnd(whole));
            assertEquals(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, spans.getSpanFlags(whole));
            assertEquals(i == 0 ? -1 : 0, spans.getSpanStart(head));
            assertEquals(i == 0 ? -1 : i == 1 ? 2 : 3, spans.getSpanEnd(head));
            assertEquals(i == 2 ? -1 : i == 0 ? 2 : 3, spans.getSpanStart(tail));
            assertEquals(i == 2 ? -1 : 5, spans.getSpanEnd(tail));
            assertEquals(0, source.getSpanStart(whole));
            assertEquals(10, source.getSpanEnd(whole));
            assertSame(source, TextUtils.ellipsize(source, paint, 10, modes[i]));
        }
    }
    @Test
    public void everyModeKeepsSurrogatePairsWholeAtEveryCutoff() {
        AndroidTestSupport.context();
        TextPaint paint = new TextPaint() {
            @Override public float measureText(String text) { return text.length(); }
        };
        for (String text : new String[]{"\ud83d\ude00\ud83d\ude00\ud83d\ude00\ud83d\ude00", "ab\ud83d\ude00cd\ud83d\ude00ef"}) {
            for (TextUtils.TruncateAt mode : new TextUtils.TruncateAt[]{TextUtils.TruncateAt.START,
                    TextUtils.TruncateAt.MIDDLE, TextUtils.TruncateAt.END}) {
                for (int width = 1; width < text.length(); width++) {
                    String result = TextUtils.ellipsize(text, paint, width, mode).toString();
                    assertTrue(result.length() <= width);
                    for (int j = 0; j < result.length(); j++) {
                        char c = result.charAt(j);
                        if (Character.isHighSurrogate(c)) {
                            assertTrue(mode + " width " + width, j + 1 < result.length()
                                    && Character.isLowSurrogate(result.charAt(++j)));
                        } else {
                            assertFalse(mode + " width " + width, Character.isLowSurrogate(c));
                        }
                    }
                }
            }
        }
    }

}
