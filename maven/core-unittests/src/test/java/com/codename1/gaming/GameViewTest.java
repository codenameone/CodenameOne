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
package com.codename1.gaming;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link GameView}'s host-side logic that does not need a GPU device:
/// accessors, the start/stop/pause/resume lifecycle and the variable/fixed timestep
/// driving of {@code update(double)}.
class GameViewTest extends UITestBase {

    /// A GameView that records how its update was driven.
    private static class CountingView extends GameView {
        int updates;
        double lastDt;
        double totalDt;
        @Override
        protected void update(double deltaSeconds) {
            updates++;
            lastDt = deltaSeconds;
            totalDt += deltaSeconds;
        }
    }

    @Test
    void accessorsAreWired() {
        CountingView v = new CountingView();
        assertNotNull(v.getScene());
        assertNotNull(v.getInput());
        assertNotNull(v.getControls());
        assertNotNull(v.getCamera());
        assertNotNull(v.getLight());
        // the camera starts in 2D mode
        assertEquals(GameCamera.MODE_ORTHO_2D, v.getCamera().getMode());
    }

    @Test
    void lifecycleFlags() {
        CountingView v = new CountingView();
        assertFalse(v.isRunning());
        v.start();
        assertTrue(v.isRunning());
        assertFalse(v.isPaused());
        v.pause();
        assertTrue(v.isPaused());
        v.resume();
        assertFalse(v.isPaused());
        v.stop();
        assertFalse(v.isRunning());
    }

    @Test
    void startIsIdempotent() {
        CountingView v = new CountingView();
        v.start();
        v.start();   // no error / state stays running
        assertTrue(v.isRunning());
        v.stop();
        v.stop();
        assertFalse(v.isRunning());
    }

    @Test
    void variableTimestepCallsUpdateOncePerFrame() {
        CountingView v = new CountingView();
        v.start();
        v.frame(0.1);
        assertEquals(1, v.updates);
        assertEquals(0.1, v.lastDt, 0.001);
        v.frame(0.05);
        assertEquals(2, v.updates);
        assertEquals(0.05, v.lastDt, 0.001);
    }

    @Test
    void pausedViewDoesNotUpdate() {
        CountingView v = new CountingView();
        v.start();
        v.pause();
        v.frame(0.1);
        assertEquals(0, v.updates);   // frame still "runs" but update is skipped
    }

    @Test
    void fixedTimestepStepsMultipleTimes() {
        CountingView v = new CountingView();
        assertEquals(0.0, v.getFixedTimestep(), 0.001);
        v.setFixedTimestep(0.1);
        assertEquals(0.1, v.getFixedTimestep(), 0.001);
        v.start();
        v.frame(0.25);                       // 0.25 / 0.1 -> 2 fixed steps, 0.05 left over
        assertEquals(2, v.updates);
        assertEquals(0.1, v.lastDt, 0.001);  // each step is exactly the fixed dt
        assertEquals(0.5, v.getInterpolationAlpha(), 0.001);   // 0.05 / 0.1
    }

    /// Two fingers, two on-screen buttons: both are down together. The view hosts
    /// a child that fills it, the GPU surface on a device, and a form hands a
    /// pointer event to the deepest component under the finger that takes focus,
    /// which a native peer does. Before the view marked that child as passing
    /// pointer events on, the only thing it heard was the form-level listener,
    /// which carries the first finger alone: the second button could never be
    /// pressed while the first was. The placeholder stands in for the peer here,
    /// made focusable the way a peer is.
    @Test
    void everyFingerReachesTheControls() {
        CountingView v = new CountingView();
        Form f = new Form("Game", new BorderLayout());
        f.add(BorderLayout.CENTER, v);
        f.show();
        flushSerialCalls();
        v.getComponentAt(0).setFocusable(true);
        f.revalidate();
        int w = v.getWidth();
        int h = v.getHeight();
        assertTrue(w > 40 && h > 40, "the view was laid out");
        int ax = w / 4;
        int bx = w * 3 / 4;
        int cy = h / 2;
        v.getControls().addButton(1001, ax, cy, 10);
        v.getControls().addButton(1002, bx, cy, 10);
        int ox = v.getAbsoluteX();
        int oy = v.getAbsoluteY();

        f.pointerPressed(new int[] {ox + ax}, new int[] {oy + cy});
        flushSerialCalls();
        assertTrue(v.getInput().isKeyDown(1001));
        assertFalse(v.getInput().isKeyDown(1002));
        assertTrue(v.getInput().isPointerDown());

        // The second finger lands: every finger is in the report.
        f.pointerDragged(new int[] {ox + ax, ox + bx}, new int[] {oy + cy, oy + cy});
        flushSerialCalls();
        assertTrue(v.getInput().isKeyDown(1001), "the first button stays down");
        assertTrue(v.getInput().isKeyDown(1002), "the second finger presses the second button");

        f.pointerReleased(new int[] {ox + ax}, new int[] {oy + cy});
        // Off the event dispatch thread, as a test is, the form's listeners are
        // queued rather than called; the release is heard through one.
        flushSerialCalls();
        assertFalse(v.getInput().isKeyDown(1001));
        assertFalse(v.getInput().isKeyDown(1002));
        assertFalse(v.getInput().isPointerDown());
    }

    /// A key held past the display's repeat delay stays down. The display calls
    /// `Form.keyRepeated` for it, 800 ms after the press and every few milliseconds
    /// from then on, and a component's own `keyRepeated` is a press followed by a
    /// release: without the view's override the held key read as up after the
    /// first repeat, and as released and pressed again in every frame after it.
    @Test
    void aHeldKeyThatRepeatsStaysDown() {
        final int[] releases = new int[1];
        CountingView v = new CountingView() {
            @Override
            public void keyReleased(int keyCode) {
                releases[0]++;
                super.keyReleased(keyCode);
            }
        };
        Form f = new Form("Game", new BorderLayout());
        f.add(BorderLayout.CENTER, v);
        f.show();
        flushSerialCalls();
        v.start();
        v.setFocusable(true);
        v.requestFocus();
        assertSame(v, f.getFocused(), "the view has the focus, so the form hands it the keys");

        f.keyPressed('d');
        assertTrue(v.getInput().isKeyDown('d'));
        assertTrue(v.getInput().wasKeyPressed('d'));
        v.frame(1.0 / 60);
        assertFalse(v.getInput().wasKeyPressed('d'), "the press was one frame's edge");

        for (int i = 0; i < 3; i++) {
            f.keyRepeated('d');
            assertTrue(v.getInput().isKeyDown('d'), "a repeat of a held key let go of it");
            assertFalse(v.getInput().wasKeyReleased('d'), "a repeat of a held key released it");
            assertFalse(v.getInput().wasKeyPressed('d'), "a repeat of a held key pressed it again");
            v.frame(1.0 / 60);
        }
        assertEquals(0, releases[0], "a subclass was told of a release while the key was held");

        f.keyReleased('d');
        assertFalse(v.getInput().isKeyDown('d'));
        assertTrue(v.getInput().wasKeyReleased('d'));
        assertEquals(1, releases[0]);
    }
}
