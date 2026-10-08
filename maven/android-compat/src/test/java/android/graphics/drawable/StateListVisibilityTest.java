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
package android.graphics.drawable;

import org.junit.Test;
import static org.junit.Assert.*;

public class StateListVisibilityTest {
    @Test public void onlyTheSelectedChildIsVisibleAndSchedulesWork() {
        StateListDrawable states = new StateListDrawable();
        ColorDrawable pressed = new ColorDrawable(1), normal = new ColorDrawable(2);
        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[0], normal);
        assertFalse(pressed.isVisible());
        assertTrue(normal.isVisible());
        final int[] schedules = {0};
        states.setCallback(new Drawable.Callback() {
            public void invalidateDrawable(Drawable d) { }
            public void scheduleDrawable(Drawable d, Runnable r, long t) { schedules[0]++; }
            public void unscheduleDrawable(Drawable d, Runnable r) { }
        });
        Runnable tick = () -> { };
        pressed.scheduleSelf(tick, 0);
        assertEquals(0, schedules[0]);
        states.setState(new int[]{android.R.attr.state_pressed});
        assertFalse(normal.isVisible());
        assertTrue(pressed.isVisible());
        pressed.scheduleSelf(tick, 0);
        assertEquals(1, schedules[0]);
        states.setVisible(false, false);
        assertFalse(pressed.isVisible());
        states.setVisible(true, false);
        assertTrue(pressed.isVisible());
        assertFalse(normal.isVisible());
    }
}
