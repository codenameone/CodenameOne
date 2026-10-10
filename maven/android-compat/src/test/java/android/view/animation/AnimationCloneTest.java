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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.animation.ObjectAnimator;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;

import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

/// Codename One's Object.clone() copies nothing (it answers null on the iOS
/// VM), so every framework animation copies itself explicitly. The copies
/// must keep their class and configuration and be independent of the
/// original, as Android's clones are.
public class AnimationCloneTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final float EPS = 1e-4f;

    private static float alphaAt(Animation a, long t) {
        Transformation out = new Transformation();
        a.getTransformation(t, out);
        return out.getAlpha();
    }

    @Test
    public void viewAnimationsCopyTheirConfiguration() {
        AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
        alpha.setDuration(200);
        alpha.setStartOffset(50);
        alpha.setRepeatCount(2);
        alpha.setInterpolator(new LinearInterpolator());
        Animation copy = alpha.clone();
        assertTrue(copy instanceof AlphaAnimation);
        assertNotSame(alpha, copy);
        assertEquals(200, copy.getDuration());
        assertEquals(50, copy.getStartOffset());
        assertEquals(2, copy.getRepeatCount());
        assertSame(alpha.getInterpolator(), copy.getInterpolator());
        copy.setStartTime(0);
        assertEquals(0.5f, alphaAt(copy, 150), EPS);

        assertTrue(new TranslateAnimation(0, 10, 0, 20).clone() instanceof TranslateAnimation);
        assertTrue(new ScaleAnimation(0, 1, 0, 1).clone() instanceof ScaleAnimation);
        assertTrue(new RotateAnimation(0, 90).clone() instanceof RotateAnimation);
    }

    @Test
    public void animationSetCopiesItsChildren() {
        AnimationSet set = new AnimationSet(true);
        AlphaAnimation child = new AlphaAnimation(0f, 1f);
        child.setDuration(100);
        set.addAnimation(child);
        AnimationSet copy = set.clone();
        assertEquals(1, copy.getAnimations().size());
        assertNotSame(child, copy.getAnimations().get(0));
        assertEquals(100, copy.getAnimations().get(0).getDuration());
        copy.getAnimations().get(0).setDuration(999);
        assertEquals(100, child.getDuration());
    }

    @Test
    public void animatorsCopyTheirConfiguration() {
        ValueAnimator v = ValueAnimator.ofFloat(0f, 1f);
        v.setDuration(400);
        v.setRepeatCount(3);
        ValueAnimator vc = v.clone();
        assertEquals(400, vc.getDuration());
        assertEquals(3, vc.getRepeatCount());
        assertNotSame(v.getValues()[0], vc.getValues()[0]);
        assertEquals(v.getValues()[0].getPropertyName(), vc.getValues()[0].getPropertyName());

        Object target = new Object();
        ObjectAnimator o = ObjectAnimator.ofFloat(target, "alpha", 0f, 1f);
        ObjectAnimator oc = o.clone();
        assertSame(target, oc.getTarget());
        assertEquals("alpha", oc.getPropertyName());

        assertTrue(new TimeAnimator().clone() instanceof TimeAnimator);
    }
}
