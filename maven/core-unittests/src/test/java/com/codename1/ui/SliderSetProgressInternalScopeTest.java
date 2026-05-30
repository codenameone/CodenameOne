package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression for
 * https://github.com/codenameone/CodenameOne/issues/1523
 * -- the 2015 reporter could not implement a range slider on top of
 * {@link Slider} because setProgressInternal(int) was private. This test
 * pins the method to protected (visible to subclasses) and verifies that a
 * subclass override actually intercepts the calls the built-in pointer/
 * keyboard handlers funnel through.
 */
class SliderSetProgressInternalScopeTest extends UITestBase {

    /// A subclass-visible probe: counts how many times setProgressInternal is
    /// called and what values it sees. If the method ever regresses back to
    /// private, this class will fail to compile.
    private static class ProbeSlider extends Slider {
        int callCount;
        int lastValue;

        @Override
        protected void setProgressInternal(int value) {
            callCount++;
            lastValue = value;
            super.setProgressInternal(value);
        }
    }

    @Test
    void subclassCanOverrideSetProgressInternal() {
        ProbeSlider s = new ProbeSlider();
        s.setMinValue(0);
        s.setMaxValue(100);

        // setProgress goes through setProgressInternal -- the subclass must
        // observe the call.
        s.setProgress(42);
        assertEquals(1, s.callCount,
                "subclass override of setProgressInternal must intercept setProgress");
        assertEquals(42, s.lastValue);
        assertEquals(42, s.getProgress());
    }

    @Test
    void subclassOverrideStaysOnTheValueSetterPath() {
        ProbeSlider s = new ProbeSlider();
        s.setMinValue(0);
        s.setMaxValue(10);

        s.setProgress(3);
        s.setProgress(7);
        s.setProgress(0);
        assertEquals(3, s.callCount);
        assertEquals(0, s.lastValue);
        assertEquals(0, s.getProgress());
    }
}
