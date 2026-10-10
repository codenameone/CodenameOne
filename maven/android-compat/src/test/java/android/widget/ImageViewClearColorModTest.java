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

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/// Clearing an ImageView's color filter or tint clears it on the drawable.
/// Both used to be applied only while non-null, so the drawable kept
/// rendering with the stale value. A drawable's own tint is still left alone
/// while the view never set one.
public class ImageViewClearColorModTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void start() {
        AndroidTestSupport.context();
    }

    static final class Recording extends Drawable {
        ColorFilter filter;

        @Override
        public void draw(Canvas canvas) {
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            filter = colorFilter;
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        ColorStateList tint() {
            return tintList;
        }
    }

    @Test
    public void clearColorFilterReachesTheDrawable() {
        ImageView v = new ImageView(AndroidTestSupport.context());
        Recording d = new Recording();
        v.setImageDrawable(d);
        ColorFilter cf = new PorterDuffColorFilter(0xffff0000, PorterDuff.Mode.SRC_ATOP);
        v.setColorFilter(cf);
        assertSame(cf, d.filter);
        v.clearColorFilter();
        assertNull(d.filter);
    }

    @Test
    public void nullTintListReachesTheDrawable() {
        ImageView v = new ImageView(AndroidTestSupport.context());
        Recording d = new Recording();
        v.setImageDrawable(d);
        v.setImageTintList(ColorStateList.valueOf(0xff00ff00));
        assertNotNull(d.tint());
        v.setImageTintList(null);
        assertNull(d.tint());
    }

    @Test
    public void aDrawablesOwnTintIsKeptWhileTheViewSetsNone() {
        ImageView v = new ImageView(AndroidTestSupport.context());
        Recording d = new Recording();
        d.setTint(0xff0000ff);
        v.setImageDrawable(d);
        v.setImageAlpha(128);
        assertNotNull(d.tint());
    }
}
