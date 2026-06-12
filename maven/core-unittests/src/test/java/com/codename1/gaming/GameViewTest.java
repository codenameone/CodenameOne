package com.codename1.gaming;

import com.codename1.junit.UITestBase;
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
}
