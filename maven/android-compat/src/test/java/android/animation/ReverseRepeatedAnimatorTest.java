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
package android.animation;

import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;

import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// reverse() on a running animator that has entered a later iteration
/// carries the current value over. It used to mirror the play time against
/// a single iteration, which put the start time in the future and snapped
/// the value to an endpoint.
public class ReverseRepeatedAnimatorTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    /// Ten seconds per iteration, so the milliseconds a test takes move the
    /// value by well under the tolerance.
    private static ValueAnimator running(int repeatCount, int repeatMode) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 100f);
        a.setDuration(10000);
        a.setInterpolator(new LinearInterpolator());
        a.setRepeatCount(repeatCount);
        a.setRepeatMode(repeatMode);
        a.start();
        a.setCurrentFraction(1.25f);
        return a;
    }

    private static float value(ValueAnimator a) {
        return ((Float) a.getAnimatedValue()).floatValue();
    }

    private static void reverseAndStep(ValueAnimator a, float expected) {
        assertEquals(expected, value(a), 0.5f);
        a.reverse();
        a.doAnimationFrame(AnimationUtils.currentAnimationTimeMillis());
        assertEquals("value jumped on reverse", expected, value(a), 0.5f);
        a.cancel();
    }

    @Test
    public void finiteRestartKeepsItsValue() {
        reverseAndStep(running(2, ValueAnimator.RESTART), 25f);
    }

    @Test
    public void finiteReverseModeKeepsItsValue() {
        reverseAndStep(running(2, ValueAnimator.REVERSE), 75f);
    }

    @Test
    public void infiniteKeepsItsValue() {
        reverseAndStep(running(ValueAnimator.INFINITE, ValueAnimator.RESTART), 25f);
        reverseAndStep(running(ValueAnimator.INFINITE, ValueAnimator.REVERSE), 75f);
    }

    @Test
    public void reversedFiniteRunsBackTowardTheStart() {
        ValueAnimator a = running(2, ValueAnimator.RESTART);
        a.reverse();
        a.doAnimationFrame(AnimationUtils.currentAnimationTimeMillis() + 2000);
        // Two seconds back from the 25% point of an iteration: 5%.
        assertEquals(5f, value(a), 0.5f);
        a.cancel();
    }
}
