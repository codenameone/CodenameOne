package com.codename1.ui.animations;

import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Dimension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnimationTimeTest extends UITestBase {

    @AfterEach
    void resetClock() {
        AnimationTime.reset();
    }

    @Test
    void nowDefaultsToSystemClock() {
        long before = System.currentTimeMillis();
        long now = AnimationTime.now();
        long after = System.currentTimeMillis();
        assertFalse(AnimationTime.isOverridden());
        assertTrue(now >= before && now <= after);
    }

    @Test
    void setTimeOverridesNow() {
        AnimationTime.setTime(123_456L);
        assertTrue(AnimationTime.isOverridden());
        assertEquals(123_456L, AnimationTime.now());
        assertEquals(123_456L, AnimationTime.now());
    }

    @Test
    void setTimeAcceptsZero() {
        AnimationTime.setTime(0L);
        assertTrue(AnimationTime.isOverridden());
        assertEquals(0L, AnimationTime.now());
    }

    @Test
    void setTimeAcceptsNegative() {
        AnimationTime.setTime(-100L);
        assertTrue(AnimationTime.isOverridden());
        assertEquals(-100L, AnimationTime.now());
    }

    @Test
    void resetRestoresSystemClock() {
        AnimationTime.setTime(42L);
        assertEquals(42L, AnimationTime.now());

        AnimationTime.reset();
        assertFalse(AnimationTime.isOverridden());

        long before = System.currentTimeMillis();
        long now = AnimationTime.now();
        long after = System.currentTimeMillis();
        assertTrue(now >= before && now <= after);
    }

    @Test
    void motionHonorsOverriddenClock() {
        AnimationTime.setTime(1000L);
        Motion m = Motion.createLinearMotion(0, 100, 1000);
        m.start();
        assertEquals(0L, m.getCurrentMotionTime());

        AnimationTime.setTime(1500L);
        assertEquals(500L, m.getCurrentMotionTime());
        assertEquals(50, m.getValue());

        // advance past duration so isFinished() trips on the time check
        AnimationTime.setTime(2001L);
        assertTrue(m.isFinished());
        assertEquals(100, m.getValue());
    }

    @Test
    void motionFinishUsesOverriddenClock() {
        AnimationTime.setTime(5000L);
        Motion m = Motion.createLinearMotion(0, 200, 1000);
        m.start();
        AnimationTime.setTime(5100L);
        assertFalse(m.isFinished());

        m.finish();
        // finish() rewinds startTime to (now - duration); reading the value latches
        // lastReturnedValue to destinationValue, which makes isFinished() true.
        assertEquals(200, m.getValue());
        assertTrue(m.isFinished());
    }

    @Test
    void timelineAnimateHonorsOverriddenClock() {
        AnimationTime.setTime(10_000L);
        Timeline timeline = Timeline.createTimeline(1000, new AnimationObject[0], new Dimension(1, 1));
        timeline.setAnimationDelay(0);

        // first animate() seeds the clock and sets time to 0
        assertTrue(timeline.animate());
        assertEquals(0, timeline.getTime());

        // advance the clock and re-animate; timeline should advance by the same delta
        AnimationTime.setTime(10_250L);
        assertTrue(timeline.animate());
        assertEquals(250, timeline.getTime());

        AnimationTime.setTime(10_750L);
        assertTrue(timeline.animate());
        assertEquals(750, timeline.getTime());
    }
}
