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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.util.FloatProperty;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

/// Value computation of the property animation classes, without running
/// them on a frame clock: seeking sets values exactly as a frame would.
public class AnimatorMathTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final float EPS = 1e-3f;

    static final class Holder {
        float value;
    }

    static final FloatProperty<Holder> VALUE = new FloatProperty<Holder>("value") {
        @Override
        public void setValue(Holder object, float v) {
            object.value = v;
        }

        @Override
        public Float get(Holder object) {
            return Float.valueOf(object.value);
        }
    };

    @Test
    public void evaluators() {
        assertEquals(25f, new FloatEvaluator().evaluate(0.25f, Float.valueOf(0), Float.valueOf(100)).floatValue(), EPS);
        assertEquals(1, new IntEvaluator().evaluate(0.5f, Integer.valueOf(0), Integer.valueOf(3)).intValue());
        ArgbEvaluator argb = ArgbEvaluator.getInstance();
        assertEquals(0xff000000, ((Integer) argb.evaluate(0f, Integer.valueOf(0xff000000), Integer.valueOf(0xffffffff))).intValue());
        assertEquals(0xffffffff, ((Integer) argb.evaluate(1f, Integer.valueOf(0xff000000), Integer.valueOf(0xffffffff))).intValue());
        // Linear-light blending: the midpoint of black and white is not 0x80.
        int mid = ((Integer) argb.evaluate(0.5f, Integer.valueOf(0xff000000), Integer.valueOf(0xffffffff))).intValue();
        assertEquals(0xffbababa, mid);
    }

    @Test
    public void keyframesSplitTheTimeline() {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 10f, 0f);
        a.setInterpolator(new LinearInterpolator());
        a.setCurrentFraction(0.25f);
        assertEquals(5f, ((Float) a.getAnimatedValue()).floatValue(), EPS);
        a.setCurrentFraction(0.5f);
        assertEquals(10f, ((Float) a.getAnimatedValue()).floatValue(), EPS);
        a.setCurrentFraction(0.75f);
        assertEquals(5f, ((Float) a.getAnimatedValue()).floatValue(), EPS);
    }

    @Test
    public void reverseRepeatPlaysBackward() {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 100f);
        a.setInterpolator(new LinearInterpolator());
        a.setRepeatCount(1);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setCurrentFraction(1.25f);
        assertEquals(75f, ((Float) a.getAnimatedValue()).floatValue(), EPS);
        a.setRepeatMode(ValueAnimator.RESTART);
        a.setCurrentFraction(1.25f);
        assertEquals(25f, ((Float) a.getAnimatedValue()).floatValue(), EPS);
    }

    @Test
    public void currentPlayTimeSeeks() {
        ValueAnimator a = ValueAnimator.ofInt(0, 100);
        a.setDuration(200);
        a.setInterpolator(new LinearInterpolator());
        a.setCurrentPlayTime(50);
        assertEquals(25, ((Integer) a.getAnimatedValue()).intValue());
        assertEquals(50, a.getCurrentPlayTime());
    }

    @Test
    public void totalDuration() {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(100);
        a.setStartDelay(30);
        a.setRepeatCount(2);
        assertEquals(330, a.getTotalDuration());
        a.setRepeatCount(ValueAnimator.INFINITE);
        assertEquals(Animator.DURATION_INFINITE, a.getTotalDuration());
    }

    @Test
    public void keyframeInterpolator() {
        Keyframe k0 = Keyframe.ofFloat(0f, 0f);
        Keyframe k1 = Keyframe.ofFloat(1f, 100f);
        k1.setInterpolator(new AccelerateInterpolator());
        ValueAnimator a = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofKeyframe("x", k0, k1));
        a.setInterpolator(new LinearInterpolator());
        a.setCurrentFraction(0.5f);
        assertEquals(25f, ((Float) a.getAnimatedValue("x")).floatValue(), EPS);
    }

    @Test
    public void objectAnimatorSetsAPropertyAndReadsItsStart() {
        Holder h = new Holder();
        ObjectAnimator a = ObjectAnimator.ofFloat(h, VALUE, 0f, 50f);
        a.setInterpolator(new LinearInterpolator());
        a.setCurrentFraction(0.5f);
        assertEquals(25f, h.value, EPS);

        Holder h2 = new Holder();
        h2.value = 20f;
        ObjectAnimator b = ObjectAnimator.ofFloat(h2, VALUE, 80f);
        b.setInterpolator(new LinearInterpolator());
        b.setCurrentFraction(0.5f);
        assertEquals(50f, h2.value, EPS);
        assertEquals("value", b.getPropertyName());
    }

    @Test
    public void argbAnimator() {
        ValueAnimator a = ValueAnimator.ofArgb(0xffff0000, 0xffff0000);
        a.setCurrentFraction(0.3f);
        assertEquals(0xffff0000, ((Integer) a.getAnimatedValue()).intValue());
    }

    @Test
    public void setTotalDurationFollowsDependencies() {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(100);
        ValueAnimator b = ValueAnimator.ofFloat(0f, 1f);
        b.setDuration(50);
        ValueAnimator c = ValueAnimator.ofFloat(0f, 1f);
        c.setDuration(70);
        AnimatorSet set = new AnimatorSet();
        set.play(a).with(b).before(c);
        assertEquals(170, set.getTotalDuration());
        AnimatorSet seq = new AnimatorSet();
        seq.playSequentially(a.clone(), b.clone(), c.clone());
        assertEquals(220, seq.getTotalDuration());
        AnimatorSet together = new AnimatorSet();
        together.playTogether(a.clone(), b.clone(), c.clone());
        together.setStartDelay(10);
        assertEquals(110, together.getTotalDuration());
        assertEquals(3, together.clone().getChildAnimations().size());
        assertTrue(together.clone() != together);
    }
}
