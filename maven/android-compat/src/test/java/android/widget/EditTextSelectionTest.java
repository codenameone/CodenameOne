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

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// A requested selection range is kept, and one requested before the native
/// field exists is applied when it is created. The start used to be dropped
/// (selectAll() reported a cursor at the end) and an early call was lost.
public class EditTextSelectionTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static String selection(EditText e) {
        return e.getSelectionStart() + "," + e.getSelectionEnd();
    }

    @Test
    public void aRangeRequestedBeforeThePeerExistsIsKept() {
        EditText e = new EditText(AndroidTestSupport.context());
        e.setText("hello");
        e.setSelection(1, 3);
        assertEquals("1,3", selection(e));
        e.getPeer();
        assertEquals("1,3", selection(e));
    }

    @Test
    public void aCursorRequestedBeforeThePeerExistsIsApplied() {
        EditText e = new EditText(AndroidTestSupport.context());
        e.setText("hello");
        e.setSelection(2);
        e.getPeer();
        assertEquals("2,2", selection(e));
    }

    @Test
    public void selectAllReportsTheWholeText() {
        EditText e = new EditText(AndroidTestSupport.context());
        e.setText("hello");
        e.getPeer();
        e.selectAll();
        assertEquals("0,5", selection(e));
    }

    /// A single-line field has a native cursor, which sits at the range's end.
    @Test
    public void aSingleLineFieldKeepsTheRangeWithItsCursorAtTheEnd() {
        EditText e = new EditText(AndroidTestSupport.context());
        e.setSingleLine(true);
        e.setText("hello");
        e.setSelection(1, 4);
        e.getPeer();
        assertEquals("1,4", selection(e));
        e.selectAll();
        assertEquals("0,5", selection(e));
    }

    @Test
    public void changingTheTextDropsTheRange() {
        EditText e = new EditText(AndroidTestSupport.context());
        e.setText("hello");
        e.getPeer();
        e.selectAll();
        e.setText("hi");
        assertEquals(e.getSelectionStart(), e.getSelectionEnd());
    }
}
