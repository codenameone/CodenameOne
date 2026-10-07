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
import org.junit.Test;
import static org.junit.Assert.fail;

public class BitmapRecycledFactoryTest {
    @Test public void allSourceFactoriesRejectRecycledBitmaps() {
        AndroidTestSupport.context();
        for (boolean mutable : new boolean[]{false, true}) {
            final Bitmap source = mutable ? Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                    : Bitmap.createBitmap(new int[16], 4, 4, Bitmap.Config.ARGB_8888);
            source.recycle();
            rejected(() -> Bitmap.createBitmap(source));
            rejected(() -> Bitmap.createBitmap(source, 0, 0, 4, 4));
            rejected(() -> Bitmap.createBitmap(source, 1, 1, 2, 2));
            rejected(() -> Bitmap.createBitmap(source, 0, 0, 4, 4, new Matrix(), true));
            rejected(() -> Bitmap.createScaledBitmap(source, 4, 4, true));
            rejected(() -> Bitmap.createScaledBitmap(source, 2, 2, true));
        }
    }
    private void rejected(Runnable factory) {
        try { factory.run(); fail("Recycled source was accepted"); }
        catch (IllegalArgumentException expected) { }
    }
}
