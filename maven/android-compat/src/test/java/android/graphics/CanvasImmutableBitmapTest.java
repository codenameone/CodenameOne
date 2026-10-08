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
package android.graphics;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.ui.Image;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.fail;

/// A canvas refuses an immutable bitmap, as Android's does, so drawing
/// cannot change pixels the bitmap (and any bitmap sharing its image)
/// promises are fixed.
public class CanvasImmutableBitmapTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void immutableTargetIsRejected() {
        AndroidTestSupport.context();
        Bitmap fixed = new Bitmap(Image.createImage(new int[4], 2, 2), false, Bitmap.Config.ARGB_8888);
        try {
            new Canvas(fixed);
            fail("new Canvas on an immutable bitmap");
        } catch (IllegalStateException expected) {
            // as on Android
        }
        Canvas c = new Canvas();
        try {
            c.setBitmap(fixed);
            fail("setBitmap with an immutable bitmap");
        } catch (IllegalStateException expected) {
            // as on Android
        }
        Bitmap mutable = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        // A mutable target is still accepted.
        c.setBitmap(mutable);
        new Canvas(mutable).drawColor(0xff00ff00);
    }
}
