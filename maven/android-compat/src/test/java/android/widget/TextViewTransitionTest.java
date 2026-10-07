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
import android.text.InputType;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
public class TextViewTransitionTest {
    @Rule public final MainThreadRule mainThread = new MainThreadRule();
    @Test public void textInputTypeCanRestoreMultilineEditing() {
        EditText text = new EditText(AndroidTestSupport.context());
        text.setInputType(InputType.TYPE_CLASS_TEXT);
        assertTrue(text.isSingleLine()); assertEquals(1,text.getMaxLines());
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        assertFalse(text.isSingleLine()); assertTrue(text.getMaxLines()>1);
        assertFalse(((com.codename1.ui.Container)text.getPeer()).getComponentAt(0) instanceof com.codename1.ui.TextField);
        text.setInputType(InputType.TYPE_CLASS_TEXT); assertTrue(text.isSingleLine());
    }
    @Test public void relativeDrawableSwapKeepsBothCallbacks() {
        TextView text = new TextView(AndroidTestSupport.context());
        ColorDrawable start = new ColorDrawable(1), end = new ColorDrawable(2);
        text.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        text.setCompoundDrawablesRelative(start,null,end,null);
        text.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        assertSame(end,text.getCompoundDrawables()[0]); assertSame(start,text.getCompoundDrawables()[2]);
        assertSame(text,start.getCallback()); assertSame(text,end.getCallback());
        text.setCompoundDrawables(null,null,null,null);
        assertNull(start.getCallback()); assertNull(end.getCallback());
    }
    @Test public void selectionUsesTheCurrentEndpoints() {
        EditText text = new EditText(AndroidTestSupport.context()); text.setText("abcd");
        text.setSelection(1,3); assertTrue(text.hasSelection());
        text.setSelection(2); assertFalse(text.hasSelection());
        text.selectAll(); assertTrue(text.hasSelection());
        assertFalse(new TextView(AndroidTestSupport.context()).hasSelection());
    }
    @Test public void backgroundsFollowVisibilityInBothDirections() {
        View view = new View(AndroidTestSupport.context()); ColorDrawable bg = new ColorDrawable(1);
        view.setBackground(bg); assertTrue(bg.isVisible());
        view.setVisibility(View.INVISIBLE); assertFalse(bg.isVisible());
        view.setVisibility(View.VISIBLE); assertTrue(bg.isVisible());
        view.setVisibility(View.GONE); assertFalse(bg.isVisible());
    }
}
