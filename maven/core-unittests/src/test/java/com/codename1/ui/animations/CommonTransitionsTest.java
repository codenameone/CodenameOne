/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.ui.animations;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
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

    /// copy() carries the caller's one Motion into every copy, and the first run leaves
    /// it finished; start() alone only moves the start time, so every later run of the
    /// transition completed at once.
    @Test
    public void aManualMotionRunsAgainForTheNextCopy() throws Exception {
        CommonTransitions transition = CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 100);
        Motion manualMotion = Motion.createLinearMotion(0, 10, 10);
        manualMotion.start();
        manualMotion.setCurrentMotionTime(20);
        manualMotion.getValue();
        assertTrue(manualMotion.isFinished(), "the first run reached its destination");
        transition.setMotion(manualMotion);

        Method createMotion = CommonTransitions.class.getDeclaredMethod("createMotion", int.class, int.class, int.class);
        createMotion.setAccessible(true);
        // Pin the animation clock: on a wall clock a millisecond can pass between start()
        // and getValue(), and a 10ms motion then legitimately answers 1 instead of 0.
        AnimationTime.setTime(1000L);
        try {
            Motion again = (Motion) createMotion.invoke(transition, 0, 10, 10);
            again.start();
            assertFalse(again.isFinished(), "the next run starts from the source again");
            assertEquals(0, again.getValue());
        } finally {
            AnimationTime.reset();
        }
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

    @FormTest
    public void testCreateFastSlideUsesMutableImageHint() {
        implementation.setMutableImagesFast(false);
        CommonTransitions slow = CommonTransitions.createFastSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 90);
        assertTrue(slow.isHorizontalSlide());

        implementation.setMutableImagesFast(true);
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
