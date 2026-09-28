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

import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/// A gesture the platform cancels -- a touch lost to palm rejection, focus moving away, the
/// system claiming it -- delivers no release. `Display#pointerCancelled(int, int)` ends it
/// without firing anything: a port that delivered a release instead activated whatever the
/// finger was on, and one that delivered nothing left it pressed.
class PointerCancelTest extends UITestBase {

    private Button button;
    private int[] fired;

    private void showButton() {
        Form f = new Form("cancel", new BorderLayout());
        button = new Button("Press");
        fired = new int[1];
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired[0]++;
            }
        });
        f.add(BorderLayout.CENTER, button);
        f.show();
        f.revalidate();
        flushSerialCalls();
    }

    private int cx() {
        return button.getAbsoluteX() + button.getWidth() / 2;
    }

    private int cy() {
        return button.getAbsoluteY() + button.getHeight() / 2;
    }

    @Test
    void aCancelledPressIsUnpressedWithoutFiring() {
        showButton();
        display.pointerPressed(new int[]{cx()}, new int[]{cy()});
        flushSerialCalls();
        assertEquals(Button.STATE_PRESSED, button.getState(), "the press has to land for this to mean anything");

        display.pointerCancelled(cx(), cy());
        flushSerialCalls();

        assertNotEquals(Button.STATE_PRESSED, button.getState(), "a cancelled gesture leaves nothing pressed");
        assertEquals(0, fired[0], "and fires nothing: the user never finished the tap");
    }

    @Test
    void aCancelQueuedRightBehindItsPressStillEndsIt() {
        // The cancel can reach the framework while its press is still queued; it is queued
        // behind it, so it is handled after the press rather than before and lost.
        showButton();
        display.pointerPressed(new int[]{cx()}, new int[]{cy()});
        display.pointerCancelled(cx(), cy());
        flushSerialCalls();

        assertNotEquals(Button.STATE_PRESSED, button.getState());
        assertEquals(0, fired[0]);
    }

    @Test
    void theNextTapAfterACancelWorks() {
        showButton();
        display.pointerPressed(new int[]{cx()}, new int[]{cy()});
        display.pointerCancelled(cx(), cy());
        flushSerialCalls();

        display.pointerPressed(new int[]{cx()}, new int[]{cy()});
        display.pointerReleased(new int[]{cx()}, new int[]{cy()});
        flushSerialCalls();

        assertEquals(1, fired[0], "a cancel must not leave the next gesture half handled");
    }

    @Test
    void aCancelWithNoGestureDoesNothing() {
        showButton();
        display.pointerCancelled(cx(), cy());
        flushSerialCalls();

        assertNotEquals(Button.STATE_PRESSED, button.getState());
        assertEquals(0, fired[0]);
    }
}
