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
}
