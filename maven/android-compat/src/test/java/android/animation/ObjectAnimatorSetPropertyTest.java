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

import android.view.animation.LinearInterpolator;

import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// `setProperty` on an animator made with a property name renames it after
/// the new property. The old name used to stay, so the animator reported,
/// looked up and auto-cancelled as the property it no longer animated.
public class ObjectAnimatorSetPropertyTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void theNameFollowsTheNewProperty() {
        AnimatorMathTest.Holder h = new AnimatorMathTest.Holder();
        ObjectAnimator a = ObjectAnimator.ofFloat(h, "other", 0f, 50f);
        a.setProperty(AnimatorMathTest.VALUE);
        a.setInterpolator(new LinearInterpolator());
        a.setCurrentFraction(0.5f);
        assertEquals(25f, h.value, 1e-3f);
        assertEquals("value", a.getPropertyName());
        assertEquals("value", a.getValues()[0].getPropertyName());
        assertEquals(25f, ((Float) a.getAnimatedValue("value")).floatValue(), 1e-3f);
        assertNull(a.getAnimatedValue("other"));
    }

    @Test
    public void cancelFromStartListenerDoesNotStartChildrenOrLaterListeners() {
        final int[] starts = {0};
        final int[] childStarts = {0};
        ValueAnimator child = ValueAnimator.ofFloat(0f, 1f);
        child.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) {
                childStarts[0]++;
            }
        });
        AnimatorSet set = new AnimatorSet();
        set.playTogether(child);
        set.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) {
                animation.cancel();
            }
        });
        set.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) {
                starts[0]++;
            }
        });
        set.start();
        assertEquals(0, starts[0]);
        assertEquals(0, childStarts[0]);
    }

    @Test
    public void cancelFromFirstChildStartDoesNotStartAnotherReadyChild() {
        com.codename1.androidcompat.testing.AndroidTestSupport.context();
        final int[] secondStarts = {0};
        final AnimatorSet set = new AnimatorSet();
        ValueAnimator first = ValueAnimator.ofFloat(0f, 1f);
        first.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) {
                set.cancel();
            }
        });
        ValueAnimator second = ValueAnimator.ofFloat(0f, 1f);
        second.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) {
                secondStarts[0]++;
            }
        });
        set.playTogether(first, second);
        set.start();
        assertEquals(0, secondStarts[0]);
    }

    @Test
    public void endingFromStartListenerKeepsTheFinalPropertyValue() {
        com.codename1.androidcompat.testing.AndroidTestSupport.context();
        AnimatorMathTest.Holder h = new AnimatorMathTest.Holder();
        ObjectAnimator a = ObjectAnimator.ofFloat(h, AnimatorMathTest.VALUE, 0f, 50f);
        a.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) {
                animation.end();
            }
        });
        a.start();
        assertFalse(a.isStarted());
        assertEquals(50f, h.value, 1e-3f);
    }

    @Test
    public void cancellingFromStartListenerDoesNotApplyAnInitialValue() {
        com.codename1.androidcompat.testing.AndroidTestSupport.context();
        AnimatorMathTest.Holder h = new AnimatorMathTest.Holder();
        h.value = 7f;
        ObjectAnimator a = ObjectAnimator.ofFloat(h, AnimatorMathTest.VALUE, 0f, 50f);
        a.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) {
                animation.cancel();
            }
        });
        a.start();
        assertFalse(a.isStarted());
        assertEquals(7f, h.value, 1e-3f);
    }

    @Test
    public void autoCancelBelongsToTheRunningAnimator() {
        com.codename1.androidcompat.testing.AndroidTestSupport.context();
        AnimatorMathTest.Holder h = new AnimatorMathTest.Holder();
        ObjectAnimator optedIn = ObjectAnimator.ofFloat(h, AnimatorMathTest.VALUE, 0f, 10f);
        optedIn.setAutoCancel(true);
        optedIn.start();
        ObjectAnimator defaultNext = ObjectAnimator.ofFloat(h, AnimatorMathTest.VALUE, 10f, 20f);
        defaultNext.start();
        assertFalse(optedIn.isStarted());
        assertTrue(defaultNext.isStarted());
        defaultNext.cancel();

        ObjectAnimator plain = ObjectAnimator.ofFloat(h, AnimatorMathTest.VALUE, 0f, 10f);
        plain.start();
        ObjectAnimator optedInNext = ObjectAnimator.ofFloat(h, AnimatorMathTest.VALUE, 10f, 20f);
        optedInNext.setAutoCancel(true);
        optedInNext.start();
        assertTrue(plain.isStarted());
        plain.cancel();
        optedInNext.cancel();
    }
}
