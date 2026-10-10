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
package android.widget;
import android.text.SpannableString;
import android.text.Spanned;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class TextViewAppendSpansTest {
    @Rule public final MainThreadRule mainThread = new MainThreadRule();
    @Test public void appendPreservesExistingAndClippedIncomingSpans() {
        TextView view = new TextView(AndroidTestSupport.context());
        SpannableString first = new SpannableString("first");
        SpannableString second = new SpannableString("_next_");
        Object firstStyle = new Object();
        Object secondStyle = new Object();
        first.setSpan(firstStyle, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        second.setSpan(secondStyle, 0, 6, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        view.setText(first);
        view.append(second, 1, 5);
        assertEquals("firstnext", view.getText().toString());
        assertTrue(view.getText() instanceof Spanned);
        Spanned result = (Spanned) view.getText();
        assertEquals(0, result.getSpanStart(firstStyle));
        assertEquals(5, result.getSpanEnd(firstStyle));
        assertEquals(5, result.getSpanStart(secondStyle));
        assertEquals(9, result.getSpanEnd(secondStyle));
        assertEquals(Spanned.SPAN_INCLUSIVE_INCLUSIVE, result.getSpanFlags(secondStyle));
        assertEquals(0, second.getSpanStart(secondStyle));
        assertEquals(6, second.getSpanEnd(secondStyle));
    }
}
