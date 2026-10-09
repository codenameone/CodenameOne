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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.androidcompat.testing.MainThreadRule;
import com.codename1.ui.Image;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// A tiled bitmap drawable applies each axis's tile mode on its own, as
/// Android's bitmap shader does. It used to tile both axes when either one
/// repeated, and to stretch the bitmap for `MIRROR`.
public class BitmapDrawableTileModeTest {

    private static final int A = 0xff000001;
    private static final int B = 0xff000002;
    private static final int C = 0xff000003;
    private static final int D = 0xff000004;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void plainImages() {
        HeadlessImplementation.pixelImages = false;
    }

    /// Records every image drawn: its destination and its pixels.
    private static final class Recorder extends Canvas {
        final List<RectF> dst = new ArrayList<RectF>();
        final List<int[]> rgb = new ArrayList<int[]>();

        Recorder() {
            super(Image.createImage(20, 20).getGraphics(), 0, 0, 20, 20);
        }

        @Override
        public void drawImage(Image img, RectF d, Paint paint) {
            dst.add(new RectF(d));
            rgb.add(img.getRGB());
        }
    }

    /// A 2x2 bitmap, top row A B and bottom row C D, drawn at 2x2.
    private static BitmapDrawable drawable(Shader.TileMode x, Shader.TileMode y) {
        Resources res = AndroidTestSupport.context().getResources();
        HeadlessImplementation.pixelImages = true;
        Bitmap b = Bitmap.createBitmap(new int[]{A, B, C, D}, 2, 2, Bitmap.Config.ARGB_8888);
        BitmapDrawable d = new BitmapDrawable(res, b);
        d.setTileModeXY(x, y);
        d.setBounds(0, 0, 5, 5);
        return d;
    }

    @Test
    public void aRepeatedXAxisClampsAnUnsetYAxis() {
        Recorder c = new Recorder();
        drawable(Shader.TileMode.REPEAT, null).draw(c);
        // Three tiles across the top, then the bottom row stretched below them.
        assertEquals(6, c.dst.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(new RectF(i * 2, 0, i * 2 + 2, 2), c.dst.get(i));
            assertEquals(4, c.rgb.get(i).length);
            assertEquals(new RectF(i * 2, 2, i * 2 + 2, 5), c.dst.get(3 + i));
            int[] edge = c.rgb.get(3 + i);
            assertEquals(2, edge.length);
            assertEquals(C, edge[0]);
            assertEquals(D, edge[1]);
        }
    }

    @Test
    public void aMirroredAxisFlipsEveryOtherTile() {
        Recorder c = new Recorder();
        drawable(Shader.TileMode.MIRROR, Shader.TileMode.CLAMP).draw(c);
        assertEquals(new RectF(0, 0, 2, 2), c.dst.get(0));
        assertEquals(A, c.rgb.get(0)[0]);
        assertEquals(new RectF(2, 0, 4, 2), c.dst.get(1));
        int[] flipped = c.rgb.get(1);
        assertEquals(B, flipped[0]);
        assertEquals(A, flipped[1]);
        assertEquals(D, flipped[2]);
        assertEquals(C, flipped[3]);
        assertEquals(A, c.rgb.get(2)[0]);
    }
}
