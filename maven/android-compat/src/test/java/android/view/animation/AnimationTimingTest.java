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
package android.view.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Matrix;

import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

/// The timing rules of the legacy view animations, checked against the
/// values AOSP's getTransformation produces.
public class AnimationTimingTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final float EPS = 1e-4f;

    private static final class Events implements Animation.AnimationListener {
        int starts;
        int ends;
        int repeats;

        @Override
        public void onAnimationStart(Animation animation) {
            starts++;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            ends++;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            repeats++;
        }
    }

    private static AlphaAnimation linearFade(long duration) {
        AlphaAnimation a = new AlphaAnimation(0f, 1f);
        a.setDuration(duration);
        a.setInterpolator(new LinearInterpolator());
        a.initialize(10, 10, 100, 100);
        return a;
    }

    @Test
    public void runsOverItsDurationAndReportsOneMoreFrame() {
        AlphaAnimation a = linearFade(100);
        Events e = new Events();
        a.setAnimationListener(e);
        a.setStartTime(1000);
        Transformation t = new Transformation();
        assertTrue(a.getTransformation(1000, t));
        assertEquals(0f, t.getAlpha(), EPS);
        assertEquals(1, e.starts);
        assertTrue(a.getTransformation(1050, t));
        assertEquals(0.5f, t.getAlpha(), EPS);
        // The frame that reaches the end still answers true: AOSP draws it.
        assertTrue(a.getTransformation(1100, t));
        assertEquals(1f, t.getAlpha(), EPS);
        assertTrue(a.hasEnded());
        assertEquals(1, e.ends);
        assertFalse(a.getTransformation(1110, t));
        assertEquals(1, e.ends);
    }

    @Test
    public void firstFrameFixesTheStartTime() {
        AlphaAnimation a = linearFade(100);
        a.setStartTime(Animation.START_ON_FIRST_FRAME);
        Transformation t = new Transformation();
        a.getTransformation(5000, t);
        assertEquals(5000, a.getStartTime());
        a.getTransformation(5025, t);
        assertEquals(0.25f, t.getAlpha(), EPS);
    }

    @Test
    public void startOffsetHoldsTheFirstValue() {
        AlphaAnimation a = linearFade(100);
        a.setStartOffset(50);
        a.setStartTime(1000);
        Transformation t = new Transformation();
        a.getTransformation(1020, t);
        assertEquals(0f, t.getAlpha(), EPS);
        a.getTransformation(1100, t);
        assertEquals(0.5f, t.getAlpha(), EPS);
    }

    @Test
    public void fillEnabledWithoutFillBeforeSkipsTheOffset() {
        AlphaAnimation a = linearFade(100);
        a.setStartOffset(50);
        a.setFillEnabled(true);
        a.setFillBefore(false);
        a.setStartTime(1000);
        Transformation t = new Transformation();
        t.setAlpha(0.33f);
        a.getTransformation(1020, t);
        // Nothing applied before the offset elapses.
        assertEquals(0.33f, t.getAlpha(), EPS);
        assertFalse(a.hasStarted());
    }

    @Test
    public void reverseRepeatRestartsOnTheNextFrame() {
        AlphaAnimation a = linearFade(100);
        a.setRepeatCount(1);
        a.setRepeatMode(Animation.REVERSE);
        Events e = new Events();
        a.setAnimationListener(e);
        a.setStartTime(1000);
        Transformation t = new Transformation();
        a.getTransformation(1000, t);
        assertEquals(0f, t.getAlpha(), EPS);
        assertTrue(a.getTransformation(1100, t));
        assertEquals(1f, t.getAlpha(), EPS);
        assertEquals(1, e.repeats);
        a.getTransformation(1150, t);
        assertEquals(1f, t.getAlpha(), EPS);
        a.getTransformation(1200, t);
        assertEquals(0.5f, t.getAlpha(), EPS);
        a.getTransformation(1250, t);
        assertEquals(0f, t.getAlpha(), EPS);
        assertTrue(a.hasEnded());
        assertEquals(1, e.ends);
    }

    @Test
    public void infiniteRepeatNeverEnds() {
        AlphaAnimation a = linearFade(10);
        a.setRepeatCount(Animation.INFINITE);
        a.setStartTime(0);
        Transformation t = new Transformation();
        long now = 0;
        for (int i = 0; i < 50; i++) {
            now += 10;
            assertTrue(a.getTransformation(now, t));
        }
        assertFalse(a.hasEnded());
    }

    @Test
    public void cancelEndsOnce() {
        AlphaAnimation a = linearFade(100);
        Events e = new Events();
        a.setAnimationListener(e);
        a.setStartTime(0);
        Transformation t = new Transformation();
        a.getTransformation(10, t);
        a.cancel();
        assertEquals(1, e.ends);
        assertFalse(a.getTransformation(20, t));
        assertEquals(1, e.ends);
    }

    @Test
    public void translateRelativeToParent() {
        TranslateAnimation a = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, -0.5f,
                Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_SELF, 1f, Animation.ABSOLUTE, 0f);
        a.setDuration(100);
        a.setInterpolator(new LinearInterpolator());
        a.initialize(100, 50, 400, 300);
        a.setStartTime(0);
        Transformation t = new Transformation();
        a.getTransformation(0, t);
        float[] p = {0, 0};
        t.getMatrix().mapPoints(p);
        assertEquals(-200f, p[0], EPS);
        assertEquals(50f, p[1], EPS);
        a.getTransformation(50, t);
        p = new float[] {0, 0};
        t.getMatrix().mapPoints(p);
        assertEquals(-100f, p[0], EPS);
        assertEquals(25f, p[1], EPS);
    }

    @Test
    public void scaleAboutTheCenter() {
        ScaleAnimation a = new ScaleAnimation(1f, 2f, 1f, 2f, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        a.setDuration(100);
        a.setInterpolator(new LinearInterpolator());
        a.initialize(100, 100, 400, 400);
        a.setStartTime(0);
        Transformation t = new Transformation();
        a.getTransformation(100, t);
        float[] p = {0, 0, 50, 50};
        t.getMatrix().mapPoints(p);
        assertEquals(-50f, p[0], EPS);
        assertEquals(-50f, p[1], EPS);
        assertEquals(50f, p[2], EPS);
        assertEquals(50f, p[3], EPS);
    }

    @Test
    public void rotateAboutAPivot() {
        RotateAnimation a = new RotateAnimation(0f, 90f, 10f, 0f);
        a.setDuration(100);
        a.setInterpolator(new LinearInterpolator());
        a.initialize(20, 20, 100, 100);
        a.setStartTime(0);
        Transformation t = new Transformation();
        a.getTransformation(100, t);
        float[] p = {20, 0};
        t.getMatrix().mapPoints(p);
        assertEquals(10f, p[0], 1e-3f);
        assertEquals(10f, p[1], 1e-3f);
    }

    @Test
    public void setComposesAndPushesItsDuration() {
        AnimationSet set = new AnimationSet(true);
        set.setInterpolator(new LinearInterpolator());
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        TranslateAnimation move = new TranslateAnimation(0f, 100f, 0f, 0f);
        set.addAnimation(fade);
        set.addAnimation(move);
        set.setDuration(200);
        set.initialize(10, 10, 100, 100);
        assertEquals(200, fade.getDuration());
        assertEquals(200, move.getDuration());
        set.setStartTime(0);
        Transformation t = new Transformation();
        set.getTransformation(100, t);
        assertEquals(0.5f, t.getAlpha(), EPS);
        float[] p = {0, 0};
        t.getMatrix().mapPoints(p);
        assertEquals(50f, p[0], EPS);
    }

    @Test
    public void setWithoutDurationSpansItsChildren() {
        AnimationSet set = new AnimationSet(false);
        AlphaAnimation a = new AlphaAnimation(0f, 1f);
        a.setDuration(100);
        AlphaAnimation b = new AlphaAnimation(0f, 1f);
        b.setDuration(100);
        b.setStartOffset(150);
        set.addAnimation(a);
        set.addAnimation(b);
        // As AOSP: the longest child, offsets not counted; the hint counts them.
        assertEquals(100, set.getDuration());
        assertEquals(250, set.computeDurationHint());
    }

    @Test
    public void cloneIsIndependent() {
        AlphaAnimation a = linearFade(100);
        Animation c = a.clone();
        c.setDuration(500);
        assertEquals(100, a.getDuration());
        AnimationSet set = new AnimationSet(false);
        set.addAnimation(a);
        AnimationSet copy = set.clone();
        copy.getAnimations().get(0).setDuration(700);
        assertEquals(100, a.getDuration());
    }

    @Test
    public void interpolatorCurves() {
        assertEquals(0.25f, new AccelerateInterpolator().getInterpolation(0.5f), EPS);
        assertEquals(0.125f, new AccelerateInterpolator(1.5f).getInterpolation(0.5f), EPS);
        assertEquals(0.75f, new DecelerateInterpolator().getInterpolation(0.5f), EPS);
        assertEquals(0.5f, new AccelerateDecelerateInterpolator().getInterpolation(0.5f), EPS);
        assertEquals(1f, new CycleInterpolator(1f).getInterpolation(0.25f), EPS);
        AnticipateOvershootInterpolator ao = new AnticipateOvershootInterpolator();
        assertEquals(0f, ao.getInterpolation(0f), EPS);
        assertEquals(0.5f, ao.getInterpolation(0.5f), EPS);
        assertEquals(1f, ao.getInterpolation(1f), EPS);
        assertTrue(ao.getInterpolation(0.1f) < 0);
        assertTrue(ao.getInterpolation(0.9f) > 1);
        assertEquals(1f, new BounceInterpolator().getInterpolation(1f), 0.01f);
        assertEquals(1f, new OvershootInterpolator().getInterpolation(1f), EPS);
        assertEquals(0f, new AnticipateInterpolator().getInterpolation(0f), EPS);
    }

    @Test
    public void pathInterpolatorFollowsTheBezier() {
        PathInterpolator p = new PathInterpolator(0.4f, 0f, 0.2f, 1f);
        assertEquals(0f, p.getInterpolation(0f), EPS);
        assertEquals(1f, p.getInterpolation(1f), EPS);
        float prev = 0;
        for (int i = 1; i <= 20; i++) {
            float x = i / 20f;
            float y = p.getInterpolation(x);
            assertTrue("monotonic at " + x, y >= prev - 1e-4f);
            assertEquals("at " + x, cubicY(x, 0.4f, 0f, 0.2f, 1f), y, 0.01f);
            prev = y;
        }
    }

    /// y of a unit cubic Bezier at the parameter where x is reached.
    private static float cubicY(float x, float x1, float y1, float x2, float y2) {
        double lo = 0;
        double hi = 1;
        for (int i = 0; i < 60; i++) {
            double t = (lo + hi) / 2;
            double u = 1 - t;
            double bx = 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t;
            if (bx < x) {
                lo = t;
            } else {
                hi = t;
            }
        }
        double t = (lo + hi) / 2;
        double u = 1 - t;
        return (float) (3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t);
    }

    @Test
    public void transformationComposes() {
        Transformation a = new Transformation();
        a.setAlpha(0.5f);
        a.getMatrix().setTranslate(10, 0);
        Transformation b = new Transformation();
        b.setAlpha(0.5f);
        b.getMatrix().setScale(2, 2);
        a.compose(b);
        assertEquals(0.25f, a.getAlpha(), EPS);
        float[] p = {1, 1};
        a.getMatrix().mapPoints(p);
        assertEquals(12f, p[0], EPS);
        assertEquals(2f, p[1], EPS);
        Matrix m = new Matrix();
        assertTrue(m.isIdentity());
    }
}
