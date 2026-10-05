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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/// An immutable bitmap made from a mutable one is a snapshot: it shares no
/// image the source keeps drawing into. An immutable source may be shared.
public class BitmapMutableSnapshotTest {

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

    /// An 8x6 bitmap; the headless port gives an image real dimensions only
    /// when it is made from pixels.
    private static Bitmap bitmap(boolean mutable) {
        AndroidTestSupport.context();
        Bitmap b = new Bitmap(Image.createImage(new int[8 * 6], 8, 6), mutable, Bitmap.Config.ARGB_8888);
        assertEquals(8, b.getWidth());
        assertEquals(6, b.getHeight());
        return b;
    }

    @Test
    public void fullSizeCropOfAMutableBitmapIsACopy() {
        Bitmap src = bitmap(true);
        Bitmap crop = Bitmap.createBitmap(src, 0, 0, 8, 6);
        assertNotSame(src, crop);
        assertFalse(crop.isMutable());
        assertNotSame(src.getImage(), crop.getImage());
    }

    @Test
    public void copiesOfAMutableBitmapShareNoImage() {
        Bitmap src = bitmap(true);
        assertNotSame(src.getImage(), Bitmap.createBitmap(src).getImage());
        assertNotSame(src.getImage(), src.copy(Bitmap.Config.ARGB_8888, false).getImage());
    }

    @Test
    public void fullSizeCropOfAnImmutableBitmapIsTheBitmap() {
        Bitmap immutable = bitmap(false);
        assertSame(immutable, Bitmap.createBitmap(immutable, 0, 0, 8, 6));
    }
}
