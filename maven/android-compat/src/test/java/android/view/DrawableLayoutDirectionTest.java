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
package android.view;

import android.graphics.drawable.ColorDrawable;
import android.widget.FrameLayout;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// A view's background and foreground take its resolved layout direction,
/// so start/end gravity and auto-mirrored vectors draw the right way round
/// in an RTL view and follow a later direction change, inherited or direct.
public class DrawableLayoutDirectionTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void drawablesTakeAndFollowTheViewDirection() {
        View v = new View(AndroidTestSupport.context());
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ColorDrawable bg = new ColorDrawable(0xff00ff00);
        ColorDrawable fg = new ColorDrawable(0x8000ff00);
        v.setBackground(bg);
        v.setForeground(fg);
        assertEquals(View.LAYOUT_DIRECTION_RTL, bg.getLayoutDirection());
        assertEquals(View.LAYOUT_DIRECTION_RTL, fg.getLayoutDirection());

        v.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        assertEquals(View.LAYOUT_DIRECTION_LTR, bg.getLayoutDirection());
        assertEquals(View.LAYOUT_DIRECTION_LTR, fg.getLayoutDirection());
    }

    @Test
    public void inheritedDirectionReachesTheBackground() {
        FrameLayout parent = new FrameLayout(AndroidTestSupport.context());
        parent.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        View child = new View(AndroidTestSupport.context());
        ColorDrawable bg = new ColorDrawable(0xff00ff00);
        child.setBackground(bg);
        parent.addView(child);
        assertEquals(View.LAYOUT_DIRECTION_RTL, bg.getLayoutDirection());

        parent.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        assertEquals(View.LAYOUT_DIRECTION_LTR, bg.getLayoutDirection());
    }
}
