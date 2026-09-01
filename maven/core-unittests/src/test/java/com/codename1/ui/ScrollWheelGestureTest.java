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
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A wheel notch is a scroll, never a tap.
///
/// The implementation plays a wheel movement as a synthetic press, three drags and a
/// release, so the scroll animates the way a finger drag does. That reads as a scroll
/// only while something actually scrolls: over content that is already fully visible no
/// drag is ever activated, and the release then fell through to whatever sat under the
/// cursor and activated it. In the signing wizard a wheel over a row of the profile
/// table opened that row's details dialog (issue #5655).
class ScrollWheelGestureTest extends UITestBase {

    @FormTest
    void aWheelOverAnUnscrollableRowIsNotDeliveredAsATap() {
        int[] taps = new int[1];
        Form f = form(taps);
        Component row = f.getContentPane().getComponentAt(0);

        wheel(row, -Display.getInstance().convertToPixels(20));

        assertEquals(0, taps[0], "a wheel notch must not activate the component under the cursor");
    }

    @FormTest
    void aWheelOverAScrollableRowIsNotDeliveredAsATapEither() {
        int[] taps = new int[1];
        Form f = form(taps);
        // Tall enough to scroll: this is the case where a drag IS activated, and the
        // release goes to the scrolling container instead of the row. It has to stay
        // that way -- the container needs its release to settle the momentum.
        f.getContentPane().setScrollableY(true);
        for (int i = 0; i < 40; i++) {
            f.getContentPane().add(filler());
        }
        f.revalidate();
        Component row = f.getContentPane().getComponentAt(0);

        wheel(row, -Display.getInstance().convertToPixels(20));

        assertEquals(0, taps[0], "scrolling past a row must not activate it");
    }

    @FormTest
    void anOrdinaryTapStillReachesTheRow() {
        int[] taps = new int[1];
        Form f = form(taps);
        Component row = f.getContentPane().getComponentAt(0);
        int x = row.getAbsoluteX() + row.getWidth() / 2;
        int y = row.getAbsoluteY() + row.getHeight() / 2;

        f.pointerPressed(x, y);
        f.pointerReleased(x, y);
        DisplayTest.flushEdt();

        assertEquals(1, taps[0], "without a wheel gesture the release is still a tap");
    }

    @FormTest
    void aWheelInADesktopWindowIsNotATapEither() {
        implementation.setMultiWindowSupported(true);
        final int[] taps = new int[1];
        Window w = new Window("wheel", new BorderLayout());
        Label row = new Label("row");
        row.setPreferredH(Display.getInstance().convertToPixels(10));
        row.addPointerReleasedListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                taps[0]++;
            }
        });
        w.add(BorderLayout.NORTH, row);
        w.add(BorderLayout.CENTER, new Label("body"));
        w.setWindowSize(300, 200);
        w.show();
        w.revalidate();
        DisplayTest.flushEdt();

        // A window resolves the release itself rather than through Form, so the same rule
        // has to be written there too: its own copy is what a desktop window's wheel goes
        // through.
        Display.impl.windowPointerWheelMoved(w.getWindowId(),
                row.getAbsoluteX() + row.getWidth() / 2, row.getAbsoluteY() + row.getHeight() / 2,
                0, -Display.getInstance().convertToPixels(20), false, 0);
        for (int i = 0; i < 6; i++) {
            DisplayTest.flushEdt();
        }

        assertEquals(0, taps[0], "a wheel notch in a window must not activate the row under it");
        w.dispose();
    }

    /// Plays one wheel notch over the middle of `over`, then drains the queued steps.
    /// Through the implementation entry point rather than by fabricating pointer events,
    /// because the gesture it queues -- and the isScrollWheeling window around it -- is
    /// exactly what is under test.
    private void wheel(Component over, int amount) {
        Display.impl.pointerWheelMoved(over.getAbsoluteX() + over.getWidth() / 2,
                over.getAbsoluteY() + over.getHeight() / 2, 0, amount, false, 0);
        // One flush per queued step, plus the one that dispatches the listeners.
        for (int i = 0; i < 6; i++) {
            DisplayTest.flushEdt();
        }
    }

    /// A form holding one row that counts the releases delivered to it, the way the
    /// wizard's table rows report a click into their details dialog.
    ///
    /// The listener sits on the leaf, because that is where a release lands: the form
    /// hit tests to the deepest component under the pointer, and Component fires only
    /// its own listeners. The wizard reaches the same place from the other end, by
    /// adding its listener to a row and to everything inside it.
    private Form form(final int[] taps) {
        Form f = new Form("wheel", new BorderLayout());
        Container content = f.getContentPane();
        content.setLayout(BoxLayout.y());
        Label row = new Label("row");
        row.setPreferredH(Display.getInstance().convertToPixels(10));
        row.addPointerReleasedListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                taps[0]++;
            }
        });
        content.add(row);
        content.add(filler());
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();
        assertTrue(row.getHeight() > 0, "the row has to be laid out for a pointer test to mean anything");
        return f;
    }

    private Component filler() {
        Label l = new Label("filler");
        l.setPreferredH(Display.getInstance().convertToPixels(10));
        return l;
    }
}
