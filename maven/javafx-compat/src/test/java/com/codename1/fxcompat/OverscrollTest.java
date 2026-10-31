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
package com.codename1.fxcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.FrameClock;
import com.codename1.fxcompat.runtime.Overscroll;
import com.codename1.fxcompat.runtime.Units;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

/// A touch scroll stretches past its ends, less the further it is pulled,
/// and springs back once the finger lifts; a glide that reaches an end
/// carries on a little way and comes back.
public class OverscrollTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
        FrameClock.setManual(true);
    }

    @After
    public void tearDown() {
        FrameClock.reset();
        Units.setScale(0);
    }

    private static final class Count implements Overscroll.Target {
        int changes;

        @Override
        public void overscrollChanged() {
            changes++;
        }
    }

    @Test
    public void aDragPastAnEndStretchesLessAndLessAndSpringsBack() {
        Count c = new Count();
        Overscroll o = new Overscroll(c);
        o.drag(-100, 400);
        double first = o.value();
        assertTrue("pulled down past the top: " + first, first < 0 && first > -100);
        o.drag(-200, 400);
        double second = o.value();
        assertTrue("the second hundred pixels stretch less than the first",
                second - first > -(-first));
        assertTrue(second < first);
        // Grabbing a stretched view starts the drag where it shows.
        assertEquals(-200, o.grab(), 0.5);
        o.release();
        assertTrue(o.isActive());
        for (int i = 0; i < 100 && o.isActive(); i++) {
            FrameClock.advance(16);
            assertTrue("it springs back without swinging past the end", o.value() <= 0);
        }
        assertFalse(o.isActive());
        assertEquals(0, o.value(), 0);
        assertTrue(c.changes > 5);
    }

    @Test
    public void aGlideThatReachesTheEndBouncesBack() {
        Overscroll o = new Overscroll(new Count());
        o.bounce(2, 400);
        double most = 0;
        for (int i = 0; i < 100 && o.isActive(); i++) {
            FrameClock.advance(16);
            most = Math.max(most, o.value());
        }
        assertTrue("it carries on past the end: " + most, most > 10);
        assertTrue("a share of the view at most: " + most, most <= 0.35 * 400 + 1);
        assertFalse(o.isActive());
        assertEquals(0, o.value(), 0);
    }

    @Test
    public void aScrollPanePulledPastTheTopMovesItsContentDownAndBack() {
        Pane content = new Pane();
        content.setPrefSize(200, 4000);
        ScrollPane pane = new ScrollPane(content);
        pane.setPannable(true);
        Scene scene = new Scene(pane, 200, 400);
        scene.cn1Layout(200, 400);
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 100, 50, MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_DRAGGED, 100, 250, MouseButton.PRIMARY);
        double y = content.getLayoutY();
        assertTrue("the content follows the finger, held back: " + y, y > 0 && y < 200);
        assertEquals(0, pane.getVvalue(), 0);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 100, 250, MouseButton.PRIMARY);
        for (int i = 0; i < 100; i++) {
            FrameClock.advance(16);
        }
        assertEquals(0, content.getLayoutY(), 0);
    }
}
