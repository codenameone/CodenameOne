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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

import static org.junit.jupiter.api.Assertions.*;

/// UITimer reads AnimationTime, so a frozen clock (the agent clock) holds a timer still
/// however much wall time passes and fires it exactly when the clock is moved past it.
class AnimationTimeFreezeTest extends UITestBase {

    @FormTest
    void aFrozenClockHoldsATimerUntilItIsAdvanced() throws Exception {
        Form form = new Form("Clock", new BorderLayout());
        form.show();
        DisplayTest.flushEdt();
        try {
            AnimationTime.setTime(1000000L);
            final boolean[] fired = new boolean[1];
            UITimer.timer(500, false, form, new Runnable() {
                @Override
                public void run() {
                    fired[0] = true;
                }
            });
            for (int i = 0; i < 20; i++) {
                form.repaintAnimations();
                Thread.sleep(40);
            }
            assertFalse(fired[0], "800 ms of wall time must not move a frozen clock");
            AnimationTime.setTime(1000000L + 499L);
            form.repaintAnimations();
            assertFalse(fired[0], "one millisecond short of the interval");
            AnimationTime.setTime(1000000L + 500L);
            form.repaintAnimations();
            assertTrue(fired[0], "the timer fires on the advanced clock");
        } finally {
            AnimationTime.reset();
        }
    }
}
