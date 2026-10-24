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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Drawing a recycled bitmap throws, as AOSP's `BaseCanvas` does. The
/// recycled bitmap still holds its image, so every overload used to keep
/// painting the stale pixels.
public class CanvasRecycledBitmapTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private Canvas canvas;
    private Bitmap recycled;

    @Before
    public void start() {
        AndroidTestSupport.context();
        canvas = new Canvas(Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888));
        recycled = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        recycled.recycle();
    }

    private static void assertRefused(Runnable draw) {
        try {
            draw.run();
            fail("drew a recycled bitmap");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("recycled bitmap"));
        }
    }

    @Test
    public void everyOverloadRefusesARecycledBitmap() {
        assertRefused(new Runnable() {
            public void run() {
                canvas.drawBitmap(recycled, 0f, 0f, null);
            }
        });
        assertRefused(new Runnable() {
            public void run() {
                canvas.drawBitmap(recycled, null, new Rect(0, 0, 4, 4), null);
            }
        });
        assertRefused(new Runnable() {
            public void run() {
                canvas.drawBitmap(recycled, null, new RectF(0, 0, 4, 4), null);
            }
        });
        final Matrix shift = new Matrix();
        shift.setTranslate(3, 3);
        assertRefused(new Runnable() {
            public void run() {
                canvas.drawBitmap(recycled, shift, null);
            }
        });
        Matrix after = new Matrix();
        canvas.getMatrix(after);
        assertEquals(new Matrix(), after);
    }

    @Test
    public void aLiveBitmapStillDraws() {
        canvas.drawBitmap(Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888), 0f, 0f, null);
    }

    @Test
    public void aRecycledTargetIsRefused() {
        Object originalGraphics = canvas.getGraphics();
        assertRefused(new Runnable() {
            public void run() {
                new Canvas(recycled);
            }
        });
        assertRefused(new Runnable() {
            public void run() {
                canvas.setBitmap(recycled);
            }
        });
        assertSame(originalGraphics, canvas.getGraphics());
    }

    @Test
    public void clearingTargetResetsCanvasStateBeforeRebind() {
        com.codename1.ui.Graphics graphics = canvas.getGraphics();
        canvas.bind(graphics, 0, 0, 8, 8);
        canvas.save();
        canvas.translate(3, 4);
        canvas.setBitmap(null);
        assertEquals(0, canvas.getWidth());
        assertEquals(0, canvas.getHeight());
        assertEquals(null, canvas.getGraphics());
        Matrix transform = new Matrix();
        canvas.getMatrix(transform);
        assertEquals(new Matrix(), transform);

        canvas.bind(graphics, 0, 0, 5, 6);
        assertEquals(5, canvas.getWidth());
        assertEquals(6, canvas.getHeight());
        assertEquals(1, canvas.getSaveCount());
    }
}
