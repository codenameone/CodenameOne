package com.codename1.ui.animations;

import com.codename1.test.UITestBase;
import com.codename1.ui.Image;
import com.codename1.util.LazyValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CommonTransitions}.
 */
public class CommonTransitionsTest extends UITestBase {
    private boolean originalDefaultLinearMotion;

    @BeforeEach
    public void captureDefaultLinearMotion() {
        originalDefaultLinearMotion = CommonTransitions.isDefaultLinearMotion();
    }

    @AfterEach
    public void restoreDefaultLinearMotion() {
        CommonTransitions.setDefaultLinearMotion(originalDefaultLinearMotion);
    }

    @Test
    public void testCreateSlideHorizontal() {
        CommonTransitions transition = CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 300);
        assertTrue(transition.isHorizontalSlide());
        assertFalse(transition.isVerticalSlide());
        assertTrue(transition.isForwardSlide());
        assertEquals(300, transition.getTransitionSpeed());
    }

    @Test
    public void testCreateSlideVerticalCopyReverse() {
        CommonTransitions transition = CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, false, 400);
        assertTrue(transition.isVerticalSlide());
        assertFalse(transition.isHorizontalSlide());
        assertFalse(transition.isForwardSlide());

        CommonTransitions reversed = (CommonTransitions) transition.copy(true);
        assertNotSame(transition, reversed);
        assertTrue(reversed.isVerticalSlide());
        assertTrue(reversed.isForwardSlide());
        assertEquals(transition.getTransitionSpeed(), reversed.getTransitionSpeed());
    }

    @Test
    public void testCreateCoverAndUncoverCopies() {
        CommonTransitions cover = CommonTransitions.createCover(CommonTransitions.SLIDE_HORIZONTAL, true, 200);
        assertTrue(cover.isHorizontalCover());
        CommonTransitions coverCopy = (CommonTransitions) cover.copy(false);
        assertNotSame(cover, coverCopy);
        assertTrue(coverCopy.isHorizontalCover());
        assertEquals(cover.getTransitionSpeed(), coverCopy.getTransitionSpeed());

        CommonTransitions uncover = CommonTransitions.createUncover(CommonTransitions.SLIDE_VERTICAL, false, 220);
        CommonTransitions reverseUncover = (CommonTransitions) uncover.copy(true);
        assertTrue(reverseUncover.isForwardSlide());
        assertEquals(CommonTransitions.SLIDE_VERTICAL, getPrivateInt(reverseUncover, "slideType"));
    }

    @Test
    public void testFadeAndTimelineCopies() throws Exception {
        CommonTransitions fade = CommonTransitions.createFade(250);
        CommonTransitions fadeCopy = (CommonTransitions) fade.copy(false);
        assertNotSame(fade, fadeCopy);
        assertEquals(250, fadeCopy.getTransitionSpeed());

        Image timeline = Image.createImage(10, 10);
        CommonTransitions timelineTransition = CommonTransitions.createTimeline(timeline);
        CommonTransitions timelineCopy = (CommonTransitions) timelineTransition.copy(false);
        assertNotSame(timelineTransition, timelineCopy);

        Field timelineField = CommonTransitions.class.getDeclaredField("timeline");
        timelineField.setAccessible(true);
        assertSame(timeline, timelineField.get(timelineCopy));
    }

    @Test
    public void testDialogPulsateAndEmptyCopy() {
        CommonTransitions empty = CommonTransitions.createEmpty();
        CommonTransitions emptyCopy = (CommonTransitions) empty.copy(false);
        assertNotSame(empty, emptyCopy);

        CommonTransitions pulsate = CommonTransitions.createDialogPulsate();
        CommonTransitions pulsateCopy = (CommonTransitions) pulsate.copy(false);
        assertNotSame(pulsate, pulsateCopy);
    }

    @Test
    public void testSlideFadeTitleCopyReverse() {
        CommonTransitions transition = CommonTransitions.createSlideFadeTitle(true, 180);
        assertTrue(transition.isForwardSlide());

        CommonTransitions reversed = (CommonTransitions) transition.copy(true);
        assertNotSame(transition, reversed);
        assertFalse(reversed.isForwardSlide());
        assertEquals(transition.getTransitionSpeed(), reversed.getTransitionSpeed());
    }

    @Test
    public void testDefaultLinearMotionPropagation() {
        CommonTransitions.setDefaultLinearMotion(true);
        CommonTransitions linearTransition = CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 120);
        assertTrue(linearTransition.isLinearMotion());

        CommonTransitions.setDefaultLinearMotion(false);
        CommonTransitions easedTransition = CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 120);
        assertFalse(easedTransition.isLinearMotion());

        linearTransition.setLinearMotion(false);
        assertFalse(linearTransition.isLinearMotion());
    }

    @Test
    public void testManualMotionUsage() throws Exception {
        CommonTransitions transition = CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 100);
        Motion manualMotion = Motion.createLinearMotion(0, 10, 10);
        manualMotion.start();
        manualMotion.setCurrentMotionTime(20);
        transition.setMotion(manualMotion);

        assertTrue(transition.animate());

        Field firstFinishedField = CommonTransitions.class.getDeclaredField("firstFinished");
        firstFinishedField.setAccessible(true);
        assertTrue(firstFinishedField.getBoolean(transition));

        Field positionField = CommonTransitions.class.getDeclaredField("position");
        positionField.setAccessible(true);
        assertEquals(manualMotion.getValue(), positionField.getInt(transition));

        assertFalse(transition.animate());
    }

    @Test
    public void testLazyMotionFactory() throws Exception {
        CommonTransitions transition = CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 100);
        final int expectedStart = 5;
        final int expectedDest = 15;
        final int expectedSpeed = 25;
        transition.setMotion(new LazyValue<Motion>() {
            public Motion get(Object... args) {
                assertEquals(expectedStart, ((Integer) args[0]).intValue());
                assertEquals(expectedDest, ((Integer) args[1]).intValue());
                assertEquals(expectedSpeed, ((Integer) args[2]).intValue());
                Motion m = Motion.createLinearMotion(expectedStart, expectedDest, expectedSpeed);
                m.start();
                return m;
            }
        });

        Method createMotion = CommonTransitions.class.getDeclaredMethod("createMotion", int.class, int.class, int.class);
        createMotion.setAccessible(true);
        Motion produced = (Motion) createMotion.invoke(transition, expectedStart, expectedDest, expectedSpeed);
        assertNotNull(produced);
        assertEquals(expectedStart, produced.getSourceValue());
        assertEquals(expectedDest, produced.getDestinationValue());
    }

    @Test
    public void testManualMotionDirectInstance() throws Exception {
        CommonTransitions transition = CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 100);
        Motion manual = Motion.createLinearMotion(3, 9, 30);
        transition.setMotion(manual);

        Method createMotion = CommonTransitions.class.getDeclaredMethod("createMotion", int.class, int.class, int.class);
        createMotion.setAccessible(true);
        Motion produced = (Motion) createMotion.invoke(transition, 1, 2, 3);
        assertSame(manual, produced);
    }

    @Test
    public void testCreateFastSlideUsesMutableImageHint() {
        when(implementation.areMutableImagesFast()).thenReturn(false);
        CommonTransitions slow = CommonTransitions.createFastSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 90);
        assertTrue(slow.isHorizontalSlide());

        when(implementation.areMutableImagesFast()).thenReturn(true);
        CommonTransitions fast = CommonTransitions.createFastSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 90);
        assertTrue(fast.isHorizontalSlide());
    }

    private int getPrivateInt(CommonTransitions transition, String fieldName) {
        try {
            Field f = CommonTransitions.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.getInt(transition);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
