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

import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// An auto-cancel ObjectAnimator that finishes on its own leaves the static
/// auto-cancel registry. Only end() and cancel() used to remove it, so every
/// naturally completed one kept its target alive for good.
public class AutoCancelReleaseTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void naturalCompletionUnregisters() {
        AnimatorMathTest.Holder h = new AnimatorMathTest.Holder();
        ObjectAnimator a = ObjectAnimator.ofFloat(h, AnimatorMathTest.VALUE, 0f, 50f);
        a.setDuration(100);
        a.setAutoCancel(true);
        a.start();
        assertTrue(a.isAutoCancelRegistered());
        assertFalse(a.doAnimationFrame(Long.MAX_VALUE / 4));
        assertFalse(a.isStarted());
        assertEquals(50f, h.value, 1e-3f);
        assertFalse("finished animator still registered", a.isAutoCancelRegistered());
    }

    @Test
    public void restartRegistersOnce() {
        AnimatorMathTest.Holder h = new AnimatorMathTest.Holder();
        ObjectAnimator a = ObjectAnimator.ofFloat(h, AnimatorMathTest.VALUE, 0f, 50f);
        a.setDuration(100);
        a.setAutoCancel(true);
        a.start();
        a.start();
        a.end();
        assertFalse(a.isAutoCancelRegistered());
    }
}
