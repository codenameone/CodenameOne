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

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;
import com.codename1.ui.Image;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// Drawables that hold children keep those children configured: a ripple
/// puts back the child's color filter after its pressed overlay, and an
/// InsetDrawable brings a replacement child up to its own bounds, state and
/// level.
public class WrapperChildStateTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void start() {
        AndroidTestSupport.context();
    }

    @Test
    public void ripplePutsBackTheChildFilterAfterAPressedDraw() {
        ColorDrawable content = new ColorDrawable(0xff00ff00);
        RippleDrawable ripple = new RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x80ff0000), content, null);
        ColorFilter filter = new PorterDuffColorFilter(0xff0000ff, PorterDuff.Mode.SRC_IN);
        ripple.setColorFilter(filter);
        ripple.setBounds(0, 0, 20, 20);
        ripple.setState(new int[] {android.R.attr.state_pressed});
        ripple.draw(new Canvas(Image.createImage(20, 20).getGraphics(), 0, 0, 20, 20));
        assertSame(filter, content.getColorFilter());
    }

    @Test
    public void insetDrawableConfiguresAReplacementChild() {
        InsetDrawable inset = new InsetDrawable(new ColorDrawable(0xff000000), 5);
        inset.setBounds(0, 0, 100, 60);
        int[] pressed = {android.R.attr.state_pressed};
        inset.setState(pressed);
        inset.setLevel(3);
        final int[] invalidations = new int[1];
        inset.setCallback(new Drawable.Callback() {
            @Override
            public void invalidateDrawable(Drawable who) {
                invalidations[0]++;
            }

            @Override
            public void scheduleDrawable(Drawable who, Runnable what, long when) {
            }

            @Override
            public void unscheduleDrawable(Drawable who, Runnable what) {
            }
        });
        ColorDrawable replacement = new ColorDrawable(0xffffffff);
        inset.setDrawable(replacement);
        assertEquals(new Rect(5, 5, 95, 55), replacement.getBounds());
        assertArrayEquals(pressed, replacement.getState());
        assertEquals(3, replacement.getLevel());
        assertTrue("the wrapper is invalidated", invalidations[0] > 0);
    }
}
