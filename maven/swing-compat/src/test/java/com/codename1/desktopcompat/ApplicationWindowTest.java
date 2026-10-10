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
package com.codename1.desktopcompat;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.rt.Units;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/// The frame that fills the application's own window on a desktop: the
/// window takes the frame's title and size, and the first frame painted
/// for a display size is followed by another.
public class ApplicationWindowTest extends KernelTestBase {

    @After
    public void reset() {
        HeadlessImplementation.setDesktop(false);
        HeadlessImplementation.nativeTitle = false;
        HeadlessImplementation.windowTitle = null;
        HeadlessImplementation.windowSize = null;
    }

    private static void desktop() {
        HeadlessImplementation.setDesktop(true);
        HeadlessImplementation.nativeTitle = true;
        HeadlessImplementation.windowTitle = null;
        HeadlessImplementation.windowSize = null;
    }

    /// The port pushes a title only when asked to, and Codename One asks
    /// only for a title set on the form that is showing. A frame is titled
    /// before it is shown.
    @Test
    public void theTitleGivenBeforeShowingReachesTheWindow() {
        desktop();
        JFrame f = new JFrame("Ledger");
        f.getContentPane().add(new JLabel("content"));
        f.setSize(640, 480);
        show(f);
        assertEquals("Ledger", HeadlessImplementation.windowTitle);
        f.setTitle("Ledger - accounts.txt");
        assertEquals("Ledger - accounts.txt", HeadlessImplementation.windowTitle);
    }

    @Test
    public void theWindowIsAskedForTheSizeOfTheFrameInDevicePixels() {
        desktop();
        // A frame sizes the window only when the window is the
        // application's own: nothing showing yet, or another frame. An
        // earlier test of this JVM may have left a form that is no frame's,
        // which would make this frame a guest, so a frame is shown first.
        show(new JFrame("Earlier"));
        HeadlessImplementation.windowSize = null;
        JFrame f = new JFrame("Ledger");
        f.getContentPane().add(new JLabel("content"));
        f.setSize(400, 300);
        show(f);
        assertArrayEquals(new int[] {Units.toDevice(400), Units.toDevice(300)}, HeadlessImplementation.windowSize);
    }

    /// Off a desktop the display is the screen, and nothing resizes it.
    @Test
    public void aScreenIsNotResized() {
        HeadlessImplementation.windowSize = null;
        JFrame f = new JFrame("Ledger");
        f.setSize(400, 300);
        show(f);
        assertNull(HeadlessImplementation.windowSize);
    }

    /// The native Linux port shows an empty buffer after the first frame
    /// of a window whose size it has just changed, so the first paint for
    /// a display size asks for one more; a paint for the same size does
    /// not, or the form would paint for ever.
    @Test
    public void theFirstPaintForADisplaySizeAsksForAnother() {
        JFrame f = new JFrame("Ledger");
        f.getContentPane().add(new JLabel("content"));
        f.setSize(400, 300);
        show(f);
        paint(f);
        assertEquals(1, f.cn1Form().cn1PaintedAgain());
        paint(f);
        paint(f);
        assertEquals(1, f.cn1Form().cn1PaintedAgain());
    }
}
