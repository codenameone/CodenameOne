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

import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// `setText(text, BufferType.SPANNABLE)` keeps a Spannable, so a caller can
/// add spans through `getText()`; the text used to stay a plain String.
public class TextViewSpannableBufferTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void spannableModeReturnsASpannable() {
        TextView tv = new TextView(AndroidTestSupport.context());
        tv.setText("hello world", TextView.BufferType.SPANNABLE);
        CharSequence text = tv.getText();
        assertTrue("getText() is not a Spannable: " + text.getClass().getName(), text instanceof Spannable);
        ForegroundColorSpan span = new ForegroundColorSpan(0xffff0000);
        ((Spannable) text).setSpan(span, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertEquals(1, ((Spanned) tv.getText()).getSpans(0, 11, ForegroundColorSpan.class).length);
        assertEquals("hello world", tv.getText().toString());
    }
}
