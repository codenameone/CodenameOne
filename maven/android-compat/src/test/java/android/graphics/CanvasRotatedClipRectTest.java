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
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// `clipRect` under a rotation clips to the rotated rectangle, not to its
/// axis-aligned bounding box, whose corners lie outside what Android clips.
public class CanvasRotatedClipRectTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void stopTracking() {
        HeadlessImplementation.trackClip = false;
        HeadlessImplementation.shapeClip = null;
    }

    private static Canvas canvas() {
        AndroidTestSupport.context();
        HeadlessImplementation.trackClip = true;
        Graphics g = Image.createImage(400, 400).getGraphics();
        g.setClip(0, 0, HeadlessImplementation.WIDTH, HeadlessImplementation.HEIGHT);
        return new Canvas(g, 0, 0, 400, 400);
    }

    @Test
    public void aRotatedRectangleClipsAsItsShape() {
        Canvas c = canvas();
        c.rotate(45, 100, 100);
        c.clipRect(50, 50, 150, 150);
        assertNotNull("a shape clip was installed", HeadlessImplementation.shapeClip);
        // The square rotated about its centre is a diamond reaching about
        // 70px from (100, 100) along the axes; (35, 35) is a corner of its
        // bounding box and lies outside it.
        assertTrue(HeadlessImplementation.shapeClip.contains(100, 100));
        assertTrue(HeadlessImplementation.shapeClip.contains(40, 100));
        assertFalse("a bounding-box corner is clipped", HeadlessImplementation.shapeClip.contains(35, 35));
    }

    @Test
    public void anUnrotatedRectangleStaysARectangleClip() {
        Canvas c = canvas();
        c.translate(10, 10);
        c.clipRect(50, 50, 150, 150);
        assertNull(HeadlessImplementation.shapeClip);
    }
}
