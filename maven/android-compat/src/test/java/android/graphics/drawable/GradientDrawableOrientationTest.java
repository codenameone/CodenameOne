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

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.androidcompat.testing.MainThreadRule;
import com.codename1.ui.Image;
import com.codename1.ui.LinearGradient;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// A linear gradient drawable runs the way its orientation says, diagonals
/// included. The four diagonal orientations (an XML `angle` of 45, 135, 225
/// or 315) used to be drawn as a plain top-to-bottom gradient.
public class GradientDrawableOrientationTest {

    private static final int RED = 0xffff0000;
    private static final int BLUE = 0xff0000ff;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void stopRecording() {
        HeadlessImplementation.recordDraws = false;
        HeadlessImplementation.draws.clear();
        HeadlessImplementation.gradients.clear();
    }

    private static Object[] draw(GradientDrawable.Orientation o) {
        AndroidTestSupport.context();
        HeadlessImplementation.recordDraws = true;
        HeadlessImplementation.draws.clear();
        HeadlessImplementation.gradients.clear();
        Canvas c = new Canvas(Image.createImage(200, 200).getGraphics(), 0, 0, 200, 200);
        GradientDrawable d = new GradientDrawable(o, new int[]{RED, BLUE});
        d.setBounds(0, 0, 100, 100);
        d.draw(c);
        assertEquals(1, HeadlessImplementation.gradients.size());
        return HeadlessImplementation.gradients.get(0);
    }

    private static void assertAxis(GradientDrawable.Orientation o, int start, int end, boolean horizontal) {
        Object[] g = draw(o);
        assertEquals(o.name(), "fillLinearGradient", g[0]);
        assertEquals(o.name(), Integer.valueOf(start & 0xffffff), g[1]);
        assertEquals(o.name(), Integer.valueOf(end & 0xffffff), g[2]);
        assertEquals(o.name(), Boolean.valueOf(horizontal), g[3]);
    }

    private static void assertDiagonal(GradientDrawable.Orientation o, float angle) {
        Object[] g = draw(o);
        assertEquals(o.name(), "fillGradient", g[0]);
        LinearGradient lg = (LinearGradient) g[1];
        assertEquals(o.name(), angle, (lg.getAngleDegrees() + 360) % 360, 0.01f);
        assertEquals(o.name(), RED, lg.getColors()[0]);
        assertEquals(o.name(), BLUE, lg.getColors()[1]);
    }

    @Test
    public void axisOrientationsKeepTheTwoColorFill() {
        assertAxis(GradientDrawable.Orientation.TOP_BOTTOM, RED, BLUE, false);
        assertAxis(GradientDrawable.Orientation.BOTTOM_TOP, BLUE, RED, false);
        assertAxis(GradientDrawable.Orientation.LEFT_RIGHT, RED, BLUE, true);
        assertAxis(GradientDrawable.Orientation.RIGHT_LEFT, BLUE, RED, true);
    }

    @Test
    public void diagonalOrientationsRunCornerToCorner() {
        // CSS angles on a square: 0 up, 90 right, so top left to bottom right is 135.
        assertDiagonal(GradientDrawable.Orientation.TL_BR, 135);
        assertDiagonal(GradientDrawable.Orientation.BL_TR, 45);
        assertDiagonal(GradientDrawable.Orientation.TR_BL, 225);
        assertDiagonal(GradientDrawable.Orientation.BR_TL, 315);
    }
}
