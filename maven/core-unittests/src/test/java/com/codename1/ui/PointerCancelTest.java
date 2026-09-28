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
import com.codename1.ui.layouts.BoxLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void aCancelledScrollSettlesWithoutAReleaseCallback() {
        // The scroll the gesture was dragging still settles, but through the cancellation path:
        // the release a component's listeners would see is exactly what a cancel is not.
        Form f = new Form("scroll", BoxLayout.y());
        Container content = f.getContentPane();
        content.setScrollableY(true);
        final int[] released = new int[1];
        ActionListener onRelease = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                released[0]++;
            }
        };
        content.addPointerReleasedListener(onRelease);
        for (int i = 0; i < 200; i++) {
            Label l = new Label("Row " + i);
            l.addPointerReleasedListener(onRelease);
            content.add(l);
        }
        f.show();
        f.revalidate();
        flushSerialCalls();

        int x = content.getAbsoluteX() + content.getWidth() / 2;
        int y = content.getAbsoluteY() + content.getHeight() * 3 / 4;
        // The press goes through Display's queue, which records it for the cancel to find; the
        // drags go the way the drag tests drive them, past the drag threshold.
        display.pointerPressed(new int[]{x}, new int[]{y});
        flushSerialCalls();
        implementation.setHasDragStarted(true);
        for (int i = 1; i <= 10; i++) {
            implementation.dispatchPointerDrag(x, y - i * content.getHeight() / 20);
        }
        flushSerialCalls();
        assertTrue(content.getScrollY() > 0, "the drag has to scroll for this to mean anything");

        display.pointerCancelled(x, y - content.getHeight() / 2);
        flushSerialCalls();

        assertEquals(0, released[0], "a cancelled scroll reports no release");
        int max = content.getScrollDimension().getHeight() - content.getHeight();
        assertTrue(content.getScrollY() >= 0 && content.getScrollY() <= max,
                "and the scroll is left somewhere it can rest, not mid-gesture");
    }

    @Test
    void aCancelledDragAndDropDropsNothingAndShowsTheSourceAgain() {
        Form f = new Form("dnd", BoxLayout.y());
        final int[] drops = new int[1];
        final int[] finished = new int[1];
        Container target = new Container(BoxLayout.y()) {
            @Override
            public void drop(Component dragged, int x, int y) {
                drops[0]++;
            }
        };
        target.setDropTarget(true);
        target.setPreferredSize(new com.codename1.ui.geom.Dimension(200, 200));
        final Label source = new Label("Drag me");
        source.setDraggable(true);
        source.setPreferredSize(new com.codename1.ui.geom.Dimension(200, 100));
        source.addDragFinishedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                finished[0]++;
            }
        });
        f.add(source);
        f.add(target);
        f.show();
        f.revalidate();
        flushSerialCalls();

        int x = source.getAbsoluteX() + source.getWidth() / 2;
        int y = source.getAbsoluteY() + source.getHeight() / 2;
        int tx = target.getAbsoluteX() + target.getWidth() / 2;
        int ty = target.getAbsoluteY() + target.getHeight() / 2;
        display.pointerPressed(new int[]{x}, new int[]{y});
        flushSerialCalls();
        implementation.setHasDragStarted(true);
        for (int i = 1; i <= 10; i++) {
            implementation.dispatchPointerDrag(x + (tx - x) * i / 10, y + (ty - y) * i / 10);
        }
        // Polled rather than flushed: a lightweight drag in progress animates the form every
        // frame, so the EDT never goes idle and a flush waits on it for good.
        waitFor(new Condition() {
            public boolean holds() {
                return !source.isVisible();
            }
        }, "the drag has to have started (the source is hidden while it is carried)");

        display.pointerCancelled(tx, ty);
        waitFor(new Condition() {
            public boolean holds() {
                return source.isVisible();
            }
        }, "a cancelled drag shows its source again");
        flushSerialCalls();

        assertTrue(source.isVisible(), "a cancelled drag shows its source again");
        assertEquals(0, drops[0], "and drops nothing where the finger happened to be");
        assertEquals(0, finished[0], "nor reports the drag as finished");
    }

    private interface Condition {
        boolean holds();
    }

    private static void waitFor(Condition c, String message) {
        long deadline = System.currentTimeMillis() + 10000;
        while (!c.holds()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError(message);
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(message);
            }
        }
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
