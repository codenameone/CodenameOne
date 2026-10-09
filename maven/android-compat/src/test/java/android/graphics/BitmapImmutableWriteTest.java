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
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.androidcompat.testing.MainThreadRule;
import com.codename1.ui.Image;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/// Pixel writes to an immutable or recycled bitmap throw
/// `IllegalStateException`, as on Android. An immutable bitmap can be the
/// very object another `createBitmap` call handed back, so a write used to
/// change both.
public class BitmapImmutableWriteTest {

    private static final int RED = 0xffff0000;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void pixels() {
        HeadlessImplementation.pixelImages = true;
    }

    @After
    public void reset() {
        HeadlessImplementation.pixelImages = false;
    }

    private static Bitmap red(boolean mutable) {
        AndroidTestSupport.context();
        int[] px = new int[4 * 3];
        java.util.Arrays.fill(px, RED);
        return new Bitmap(Image.createImage(px, 4, 3), mutable, Bitmap.Config.ARGB_8888);
    }

    private static Bitmap immutableRed() {
        return red(false);
    }

    @Test
    public void immutableBitmapRejectsEveryWrite() {
        Bitmap b = immutableRed();
        Bitmap alias = Bitmap.createBitmap(b, 0, 0, 4, 3);
        assertSame(b, alias);
        try {
            b.setPixel(0, 0, 0);
            fail("setPixel on an immutable bitmap");
        } catch (IllegalStateException expected) {
            // as on Android
        }
        try {
            b.setPixels(new int[12], 0, 4, 0, 0, 4, 3);
            fail("setPixels on an immutable bitmap");
        } catch (IllegalStateException expected) {
            // as on Android
        }
        try {
            b.eraseColor(0);
            fail("eraseColor on an immutable bitmap");
        } catch (IllegalStateException expected) {
            // as on Android
        }
        assertEquals(RED, alias.getPixel(0, 0));
        assertEquals(RED, alias.getPixel(3, 2));
    }

    @Test
    public void mutableBitmapAcceptsWrites() {
        Bitmap m = red(true);
        m.setPixel(0, 0, 0xff00ff00);
        m.setPixels(new int[]{0xff0000ff}, 0, 1, 1, 1, 1, 1);
        assertEquals(0xff00ff00, m.getPixel(0, 0));
        assertEquals(0xff0000ff, m.getPixel(1, 1));
        m.eraseColor(0);
        assertEquals(0, m.getPixel(1, 1));
    }

    @Test
    public void recycledBitmapRejectsWrites() {
        Bitmap m = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        m.recycle();
        try {
            m.setPixel(0, 0, RED);
            fail("setPixel on a recycled bitmap");
        } catch (IllegalStateException expected) {
            assertEquals("Can't call setPixel() on a recycled bitmap", expected.getMessage());
        }
    }
    /// Reads refuse too. `recycle()` dropped only the cached pixel array, so
    /// the next read used to fill it again from the retained image.
    @Test
    public void recycledBitmapRejectsReads() {
        Bitmap m = red(true);
        assertEquals(RED, m.getPixel(0, 0));
        m.recycle();
        try {
            m.getPixel(0, 0);
            fail("getPixel on a recycled bitmap");
        } catch (IllegalStateException expected) {
            assertEquals("Can't call getPixel() on a recycled bitmap", expected.getMessage());
        }
        try {
            m.getPixels(new int[12], 0, 4, 0, 0, 4, 3);
            fail("getPixels on a recycled bitmap");
        } catch (IllegalStateException expected) {
            assertEquals("Can't call getPixels() on a recycled bitmap", expected.getMessage());
        }
        try {
            m.copy(Bitmap.Config.ARGB_8888, true);
            fail("copy of a recycled bitmap");
        } catch (IllegalStateException expected) {
            assertEquals("Can't copy a recycled bitmap", expected.getMessage());
        }
        try {
            m.compress(Bitmap.CompressFormat.PNG, 100, new java.io.ByteArrayOutputStream());
            fail("compress of a recycled bitmap");
        } catch (IllegalStateException expected) {
            assertEquals("Can't compress a recycled bitmap", expected.getMessage());
        }
    }
}
