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

import android.content.Context;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import com.codename1.androidcompat.testing.AndroidTestSupport;

import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// A TextView with no appearance of its own takes the theme's
/// textAppearanceSmall, as AOSP's Widget.TextView says: on a device the
/// gallery's plain labels are 54% black, and they rendered primary black.
public class TextViewDefaultAppearanceTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void aPlainTextViewIsSmallAndTertiaryInMaterialLight() {
        Context c = new ContextThemeWrapper(AndroidTestSupport.context(),
                android.R.style.Theme_Material_Light_DarkActionBar);
        TextView t = new TextView(c);
        assertEquals(0x8a000000, t.getCurrentTextColor());
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14,
                c.getResources().getDisplayMetrics());
        assertEquals(px, t.getTextSize(), 0.5f);
    }
}
