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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        // Tall enough to scroll: this is the case where a drag IS activated, so the release
        // goes to the scrolling container and never reaches the row. That was already true
        // before the fix -- this one GUARDS the good case rather than demonstrating the bug,
        // which only appears when the gesture activates no drag at all, exactly as the
        // report described it ("there was nothing available to scroll to").
        f.getContentPane().setScrollableY(true);
        for (int i = 0; i < 60; i++) {
            f.getContentPane().add(filler());
        }
        f.revalidate();
        assertTrue(f.getContentPane().isScrollableY(),
                "the content has to overflow, or this repeats the case above");
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

    @FormTest
    void aWheelOverAButtonInAWindowDoesNotStrandItAwaitingRelease() {
        implementation.setMultiWindowSupported(true);
        final int[] fired = new int[1];
        Window w = new Window("wheel then press", new BorderLayout());
        Button b = new Button("press me");
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fired[0]++;
            }
        });
        w.add(BorderLayout.CENTER, b);
        w.setWindowSize(300, 200);
        w.show();
        w.revalidate();
        DisplayTest.flushEdt();

        // The wheel gesture presses the button and is then denied its release, which is
        // the point of the fix above -- but Button takes itself off the window's
        // awaiting-release list in its OWN pointerReleased. Left there, the next real
        // press makes that list hold two, autoRelease leaves its single component branch,
        // and a press dragged off the button stops being cancelled.
        Display.impl.windowPointerWheelMoved(w.getWindowId(), 150, 120, 0,
                -Display.getInstance().convertToPixels(20), false, 0);
        for (int i = 0; i < 6; i++) {
            DisplayTest.flushEdt();
        }
        assertEquals(0, fired[0], "the wheel itself must not fire the button");

        // Now the ordinary gesture that has to keep working: press, drag well clear of
        // the button, release out there.
        w.pointerPressed(150, 120);
        w.pointerDragged(-20, -20);
        w.pointerReleased(-20, -20);
        int firedCount = fired[0];
        w.dispose();

        assertEquals(0, firedCount,
                "releasing outside the button must still not fire its action after a wheel");
    }

    @FormTest
    void aWheelOverAnOnOffSwitchNeitherFlipsItNorSpoilsTheNextTap() {
        Form f = new Form("switch", new BorderLayout());
        com.codename1.components.OnOffSwitch sw = new com.codename1.components.OnOffSwitch();
        sw.setValue(true);
        f.getContentPane().setLayout(BoxLayout.y());
        f.getContentPane().add(sw);
        f.getContentPane().add(filler());
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();
        assertTrue(sw.getHeight() > 0, "the switch has to be laid out for a pointer test to mean anything");

        wheel(sw, -Display.getInstance().convertToPixels(20));

        assertTrue(sw.isValue(), "a wheel must not change the switch it scrolled past");

        // And the drag half must not be left applied either. The switch reads its own
        // "dragged" flag on release to tell a tap from the end of a slide, so a latched
        // one turns the next real tap into the tail of a gesture nobody made -- it stops
        // toggling and settles on whatever the stale coordinates say.
        int x = sw.getAbsoluteX() + sw.getWidth() / 2;
        int y = sw.getAbsoluteY() + sw.getHeight() / 2;
        f.pointerPressed(x, y);
        f.pointerReleased(x, y);
        DisplayTest.flushEdt();

        assertFalse(sw.isValue(), "the tap after the wheel still has to toggle the switch");
    }

    @FormTest
    void aWheelStillSettlesAComponentThatKeptTheDrag() {
        Form f = new Form("sticky", new BorderLayout());
        StickyDragComponent sticky = new StickyDragComponent();
        sticky.setPreferredH(Display.getInstance().convertToPixels(15));
        f.getContentPane().setLayout(BoxLayout.y());
        f.getContentPane().add(sticky);
        f.getContentPane().add(filler());
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();

        wheel(sticky, -Display.getInstance().convertToPixels(20));

        // A Spinner rolls its value from the drag and commits it on the release, and it
        // keeps the gesture through the form's sticky-drag path rather than by becoming
        // the scrolling container. Suppressing that release left the roll uncommitted.
        assertEquals(1, sticky.releases(), "a drag that was activated still has to be settled");
        assertTrue(sticky.drags() > 0, "the drag has to have reached it for this to mean anything");
    }

    @FormTest
    void aWheelSettlesAKeptDragInAWindowToo() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("sticky", new BorderLayout());
        StickyDragComponent sticky = new StickyDragComponent();
        w.add(BorderLayout.CENTER, sticky);
        w.setWindowSize(300, 200);
        w.show();
        w.revalidate();
        DisplayTest.flushEdt();

        Display.impl.windowPointerWheelMoved(w.getWindowId(),
                sticky.getAbsoluteX() + sticky.getWidth() / 2,
                sticky.getAbsoluteY() + sticky.getHeight() / 2,
                0, -Display.getInstance().convertToPixels(20), false, 0);
        for (int i = 0; i < 6; i++) {
            DisplayTest.flushEdt();
        }

        assertEquals(1, sticky.releases(), "a window has no sticky-drag list, so the pressed"
                + " component is where a kept drag arrives");
        w.dispose();
    }

    @FormTest
    void aWheelStillScrollsThroughAChildThatForwardsItsDrags() {
        // The trap the ImageViewer guard has to avoid, and the reason EditorView has none at
        // all: a component can be the only thing that forwards a drag to the scrollable
        // ancestor -- ImageViewer does exactly this for a vertical drag -- so declining the
        // whole gesture there would stop a wheel scrolling the page it sits on.
        Form f = new Form("forwarding", new BorderLayout());
        // The scroller has to actually overflow: isScrollableY() reports false for a
        // container whose content fits, and a child looking for a scrollable ancestor would
        // find none.
        Container scroller = f.getContentPane();
        scroller.setLayout(BoxLayout.y());
        scroller.setScrollableY(true);
        ForwardingComponent child = new ForwardingComponent();
        child.setPreferredH(Display.getInstance().convertToPixels(15));
        scroller.add(child);
        for (int i = 0; i < 60; i++) {
            scroller.add(filler());
        }
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();
        assertTrue(scroller.isScrollableY(), "the container has to have something to scroll");
        assertEquals(0, scroller.getScrollY(), "nothing is scrolled before the wheel");

        wheel(child, -Display.getInstance().convertToPixels(20));

        assertTrue(child.forwarded(), "the child has to have forwarded for this to mean anything");
        assertTrue(scroller.getScrollY() > 0,
                "a wheel forwarded by a child still has to scroll the container it names");
    }

    /// Forwards its drags to the scrollable ancestor instead of handling them, the way
    /// `ImageViewer` forwards a vertical drag.
    private static final class ForwardingComponent extends Component {
        private boolean forwarded;

        boolean forwarded() {
            return forwarded;
        }

        @Override
        public void pointerDragged(int x, int y) {
            Container p = getParent();
            while (p != null && !p.isScrollableY()) {
                p = p.getParent();
            }
            if (p != null) {
                if (!forwarded) {
                    p.pointerPressed(x, y);
                }
                forwarded = true;
                p.pointerDragged(x, y);
            }
        }
    }

    /// Keeps the gesture the way `Spinner` does, and activates a drag on the first move --
    /// which is what tells a settle apart from a tap.
    private static final class StickyDragComponent extends Component {
        private int releases;
        private int drags;

        int releases() {
            return releases;
        }

        int drags() {
            return drags;
        }

        @Override
        protected boolean isStickyDrag() {
            return true;
        }

        @Override
        public void pointerDragged(int x, int y) {
            drags++;
            setDragActivated(true);
        }

        @Override
        public void pointerReleased(int x, int y) {
            releases++;
        }
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

    /// Tall enough that a screenful of them overflows. A container whose content fits
    /// reports isScrollableY() false however it was configured, so a test that means to
    /// exercise scrolling has to make it actually overflow -- and say so.
    private Component filler() {
        Label l = new Label("filler");
        l.setPreferredH(Display.getInstance().convertToPixels(30));
        return l;
    }
}
