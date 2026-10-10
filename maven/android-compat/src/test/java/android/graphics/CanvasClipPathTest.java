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
import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Rectangle;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/// `clipPath` narrows the clip, as on Android. `Graphics.setClip(Shape)`
/// replaces the clip, so a path reaching past the bounds the parent clipped
/// a view to used to widen the clip and let the view paint over its siblings.
public class CanvasClipPathTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void stopTracking() {
        HeadlessImplementation.trackClip = false;
        HeadlessImplementation.shapeClip = null;
    }

    private static Graphics clippedGraphics() {
        AndroidTestSupport.context();
        HeadlessImplementation.trackClip = true;
        Graphics g = Image.createImage(200, 200).getGraphics();
        g.setClip(0, 0, HeadlessImplementation.WIDTH, HeadlessImplementation.HEIGHT);
        // The parent clips the view at (10, 10) to its 50x50 bounds.
        g.clipRect(10, 10, 50, 50);
        return g;
    }

    @Test
    public void aPathLargerThanTheClipIsIntersectedWithIt() {
        Graphics g = clippedGraphics();
        Canvas c = new Canvas(g, 10, 10, 50, 50);
        Path p = new Path();
        p.addRect(0, 0, 300, 300, Path.Direction.CW);
        c.clipPath(p);
        assertNotNull("a shape clip was installed", HeadlessImplementation.shapeClip);
        Rectangle b = HeadlessImplementation.shapeClip.getBounds();
        assertEquals(10, b.getX());
        assertEquals(10, b.getY());
        assertEquals(50, b.getWidth());
        assertEquals(50, b.getHeight());
    }

    @Test
    public void aPathInsideTheClipIsUsedAsIs() {
        Graphics g = clippedGraphics();
        Canvas c = new Canvas(g, 10, 10, 50, 50);
        Path p = new Path();
        p.addRect(5, 5, 25, 25, Path.Direction.CW);
        c.clipPath(p);
        assertNotNull("a shape clip was installed", HeadlessImplementation.shapeClip);
        Rectangle b = HeadlessImplementation.shapeClip.getBounds();
        assertEquals(15, b.getX());
        assertEquals(15, b.getY());
        assertEquals(20, b.getWidth());
        assertEquals(20, b.getHeight());
    }
}
