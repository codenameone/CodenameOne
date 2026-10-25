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

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/// Drawables given as start and end follow a later layout direction change.
public class TextViewRelativeDrawablesTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void relativeDrawablesFlipWithTheViewsDirection() {
        TextView tv = new TextView(AndroidTestSupport.context());
        tv.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        Drawable start = new ColorDrawable(0xffff0000);
        Drawable end = new ColorDrawable(0xff00ff00);
        tv.setCompoundDrawablesRelative(start, null, end, null);
        assertSame(start, tv.getCompoundDrawables()[0]);
        tv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        assertSame("start moves to the right", start, tv.getCompoundDrawables()[2]);
        assertSame(end, tv.getCompoundDrawables()[0]);
        assertSame(start, tv.getCompoundDrawablesRelative()[0]);
        assertSame(end, tv.getCompoundDrawablesRelative()[2]);
    }

    @Test
    public void relativeDrawablesFollowAnInheritedDirection() {
        FrameLayout parent = new FrameLayout(AndroidTestSupport.context());
        parent.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        TextView tv = new TextView(AndroidTestSupport.context());
        parent.addView(tv);
        Drawable start = new ColorDrawable(0xffff0000);
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(start, null, null, null);
        parent.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        assertSame(start, tv.getCompoundDrawables()[2]);
        assertNull(tv.getCompoundDrawables()[0]);
        assertSame(start, tv.getCompoundDrawablesRelative()[0]);
    }

    @Test
    public void absoluteDrawablesStayPut() {
        TextView tv = new TextView(AndroidTestSupport.context());
        tv.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        Drawable left = new ColorDrawable(0xffff0000);
        tv.setCompoundDrawablesRelative(new ColorDrawable(0), null, null, null);
        tv.setCompoundDrawables(left, null, null, null);
        tv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        assertSame(left, tv.getCompoundDrawables()[0]);
    }
}
