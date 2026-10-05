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

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;
import com.codename1.ui.Image;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// An indeterminate ProgressBar draws the configured indeterminate drawable
/// over its content box. It used to store it and always draw the built-in
/// indicator instead.
public class ProgressBarIndeterminateDrawableTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void start() {
        AndroidTestSupport.context();
    }

    static final class Counting extends Drawable {
        int draws;
        Rect drawnBounds;

        @Override
        public void draw(Canvas canvas) {
            draws++;
            drawnBounds = new Rect(getBounds());
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    @Test
    public void theIndeterminateDrawableIsDrawnInTheContentBox() {
        ProgressBar p = new ProgressBar(AndroidTestSupport.context());
        Counting d = new Counting();
        p.setIndeterminateDrawable(d);
        p.setPadding(2, 3, 4, 5);
        p.layout(0, 0, 60, 60);
        p.onDraw(new Canvas(Image.createImage(60, 60).getGraphics(), 0, 0, 60, 60));
        assertEquals(1, d.draws);
        assertEquals(new Rect(2, 3, 56, 55), d.drawnBounds);
    }
}
