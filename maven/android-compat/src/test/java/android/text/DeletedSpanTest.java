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
import static org.junit.Assert.*;

public class DeletedSpanTest {
    @Test public void deletedExclusiveSpanIsRemovedWhileOtherSpansSurvive() {
        SpannableStringBuilder text = new SpannableStringBuilder("abcdef");
        Object deleted = new Object();
        Object partial = new Object();
        Object inclusive = new Object();
        text.setSpan(deleted, 2, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | (3 << Spanned.SPAN_PRIORITY_SHIFT));
        text.setSpan(partial, 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(inclusive, 2, 4, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        text.delete(1, 5);
        assertEquals("af", text.toString());
        assertEquals(-1, text.getSpanStart(deleted));
        assertEquals(0, text.getSpanStart(partial));
        assertEquals(1, text.getSpanEnd(partial));
        assertEquals(1, text.getSpanStart(inclusive));
        text.insert(1, "new");
        assertEquals(-1, text.getSpanStart(deleted));
        assertEquals(1, text.getSpanEnd(partial));
        assertEquals(4, text.getSpanEnd(inclusive));
    }
}
