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
package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/// A selector hands its tint list and tint mode to every child, including
/// states added after the tint was set.
public class StateListTintModeTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void start() {
        AndroidTestSupport.context();
    }

    @Test
    public void tintModeReachesExistingAndLaterChildren() {
        StateListDrawable sld = new StateListDrawable();
        ColorDrawable first = new ColorDrawable(0xff000000);
        sld.addState(new int[] {android.R.attr.state_pressed}, first);
        ColorStateList tint = ColorStateList.valueOf(0xffff0000);
        sld.setTintList(tint);
        sld.setTintMode(PorterDuff.Mode.MULTIPLY);
        assertEquals(PorterDuff.Mode.MULTIPLY, first.tintMode);
        assertSame(tint, first.tintList);

        ColorDrawable later = new ColorDrawable(0xff00ff00);
        sld.addState(new int[0], later);
        assertEquals(PorterDuff.Mode.MULTIPLY, later.tintMode);
        assertSame(tint, later.tintList);
    }
}
