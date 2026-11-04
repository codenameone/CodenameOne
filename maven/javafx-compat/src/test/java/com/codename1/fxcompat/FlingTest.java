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
import com.codename1.fxcompat.runtime.Fling;
import com.codename1.fxcompat.runtime.FrameClock;
import com.codename1.fxcompat.runtime.Units;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

/// A touch scroll glides on after the finger lifts, slows down, and stops
/// at the end of the content or when a new press arrives.
public class FlingTest {

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

    /// Records every distance it is asked to move, up to a limit.
    private static final class Track implements Fling.Target {
        double position;
        double limit = Double.MAX_VALUE;

        @Override
        public boolean flingBy(double delta) {
            double to = Math.min(limit, position + delta);
            boolean whole = to == position + delta;
            position = to;
            return whole;
        }
    }

    private static void drag(Fling fling, double from, double perFrame, int frames) {
        for (int i = 0; i <= frames; i++) {
            fling.track(from + i * perFrame);
            FrameClock.advance(16);
        }
    }

    @Test
    public void aQuickDragGlidesOnAndSlowsToAStop() {
        Track t = new Track();
        Fling fling = new Fling(t);
        // 1 pixel per millisecond.
        drag(fling, 0, 16, 6);
        fling.release();
        assertTrue(fling.isRunning());
        FrameClock.advance(16);
        double first = t.position;
        assertEquals(16 * 0.984, first, 1);
        FrameClock.advance(16);
        double second = t.position - first;
        assertTrue("the glide slows: " + first + " then " + second, second < first);
        for (int i = 0; i < 400 && fling.isRunning(); i++) {
            FrameClock.advance(16);
        }
        assertFalse(fling.isRunning());
        // v / -ln(0.998), about 500 pixels for 1 pixel per millisecond.
        assertEquals(500, t.position, 15);
    }

    @Test
    public void aFingerThatPausedBeforeLiftingDoesNotGlide() {
        Track t = new Track();
        Fling fling = new Fling(t);
        drag(fling, 0, 16, 6);
        FrameClock.advance(200);
        fling.release();
        assertFalse(fling.isRunning());
        assertEquals(0, t.position, 0);
    }

    @Test
    public void theGlideStopsAtTheEndAndOnANewPress() {
        Track t = new Track();
        t.limit = 40;
        Fling fling = new Fling(t);
        drag(fling, 0, 16, 6);
        fling.release();
        for (int i = 0; i < 10; i++) {
            FrameClock.advance(16);
        }
        assertFalse(fling.isRunning());
        assertEquals(40, t.position, 0);

        Track u = new Track();
        Fling again = new Fling(u);
        drag(again, 0, 16, 6);
        again.release();
        FrameClock.advance(16);
        again.stop();
        double at = u.position;
        FrameClock.advance(16);
        assertEquals(at, u.position, 0);
    }

    @Test
    public void aScrollPaneGlidesAfterATouchPan() {
        Pane content = new Pane();
        content.setPrefSize(200, 4000);
        ScrollPane pane = new ScrollPane(content);
        pane.setPannable(true);
        Scene scene = new Scene(pane, 200, 400);
        scene.cn1Layout(200, 400);
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 100, 350, MouseButton.PRIMARY);
        for (int i = 1; i <= 6; i++) {
            FrameClock.advance(16);
            scene.cn1Pointer(MouseEvent.MOUSE_DRAGGED, 100, 350 - 16 * i, MouseButton.PRIMARY);
        }
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 100, 254, MouseButton.PRIMARY);
        double released = pane.getVvalue();
        assertTrue(released > 0);
        for (int i = 0; i < 20; i++) {
            FrameClock.advance(16);
        }
        assertTrue("the pane glides on: " + released + " then " + pane.getVvalue(),
                pane.getVvalue() > released + 0.02);
    }
}
