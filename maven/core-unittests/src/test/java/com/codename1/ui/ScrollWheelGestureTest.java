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
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.events.WheelEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A wheel scrolls the container under it, and dispatches nothing else.
///
/// It used to be emulated: a press, three drags and a release played into the component
/// tree so the scroll would animate like a finger drag. Every component that reacts to a
/// pointer then had to recognise the impostor and refuse it -- Button, Slider and ComboBox
/// carry that guard to this day -- and every component that did not was a bug. Scrolling
/// past a switch toggled it; a row of a table opened its dialog (issue #5655).
///
/// The ports report real wheel deltas, so these tests say what the wheel now does: it moves
/// the nearest scrollable ancestor, a component that wants it takes it through
/// `Component#mouseWheel`, and no pointer event is invented for anybody.
class ScrollWheelGestureTest extends UITestBase {

    @FormTest
    void aWheelScrollsTheContainerUnderIt() {
        Form f = scrollingForm();
        Container content = f.getContentPane();
        assertTrue(content.isScrollableY(), "the content has to overflow for this to mean anything");

        wheel(content.getComponentAt(0), 0, -px(20));

        assertTrue(content.getScrollY() > 0, "a wheel notch scrolls the container it is over");
    }

    @FormTest
    void aWheelDispatchesNoPointerEventAtAll() {
        Form f = new Form("no pointers", new BorderLayout());
        PointerCountingComponent target = new PointerCountingComponent();
        target.setPreferredH(px(15));
        f.getContentPane().setLayout(BoxLayout.y());
        f.getContentPane().add(target);
        f.getContentPane().add(filler());
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();

        wheel(target, 0, -px(20));

        assertEquals(0, target.pointerEvents(),
                "a wheel is a scroll: nothing may be told a pointer was pressed, dragged or released");
    }

    @FormTest
    void aWheelOverASwitchLeavesItAlone() {
        Form f = new Form("switch", new BorderLayout());
        com.codename1.components.OnOffSwitch sw = new com.codename1.components.OnOffSwitch();
        sw.setValue(true);
        f.getContentPane().setLayout(BoxLayout.y());
        f.getContentPane().add(sw);
        f.getContentPane().add(filler());
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();

        wheel(sw, 0, -px(20));

        // No guard inside the switch says so any more. Nothing reaches it to guard against.
        assertTrue(sw.isValue(), "a wheel must not change a switch it scrolled past");

        int x = sw.getAbsoluteX() + sw.getWidth() / 2;
        int y = sw.getAbsoluteY() + sw.getHeight() / 2;
        f.pointerPressed(x, y);
        f.pointerReleased(x, y);
        DisplayTest.flushEdt();
        assertFalse(sw.isValue(), "and a real tap still toggles it");
    }

    @FormTest
    void aWheelOverAButtonDoesNotFireIt() {
        Form f = new Form("button", new BorderLayout());
        final int[] fired = new int[1];
        Button b = new Button("press me");
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fired[0]++;
            }
        });
        f.getContentPane().setLayout(BoxLayout.y());
        f.getContentPane().add(b);
        f.getContentPane().add(filler());
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();

        wheel(b, 0, -px(20));

        assertEquals(0, fired[0], "a wheel over a button is not a click on it");
    }

    @FormTest
    void aComponentThatWantsTheWheelTakesIt() {
        Form f = scrollingForm();
        Container content = f.getContentPane();
        WheelHandlingComponent target = new WheelHandlingComponent();
        target.setPreferredH(px(15));
        content.addComponent(0, target);
        f.revalidate();
        DisplayTest.flushEdt();

        wheel(target, 0, -px(20));

        assertEquals(1, target.wheels(), "the component under the cursor is offered the wheel first");
        assertEquals(0, content.getScrollY(), "and consuming it stops the container scrolling");
    }

    @FormTest
    void aListenerRunsBeforeTheComponentsOwnHandling() {
        Form f = new Form("listener first", new BorderLayout());
        final WheelHandlingComponent target = new WheelHandlingComponent();
        target.setPreferredH(px(15));
        // Consuming is documented to prevent the DEFAULT behaviour, and a component that
        // pans itself on a wheel is exactly that: control plus wheel to zoom an image viewer
        // has to be able to stop the viewer's own pan.
        target.addMouseWheelListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                evt.consume();
            }
        });
        f.getContentPane().setLayout(BoxLayout.y());
        f.getContentPane().add(target);
        f.getContentPane().add(filler());
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();

        wheel(target, 0, -px(20));

        assertEquals(0, target.wheels(), "a consumed wheel never reaches the component's own handler");
    }

    @FormTest
    void aWheelPastTheEndOfAnInnerScrollerMovesThePage() {
        Form f = scrollingForm();
        Container page = f.getContentPane();
        Container inner = new Container(BoxLayout.y());
        inner.setScrollableY(true);
        inner.setPreferredH(px(40));
        for (int i = 0; i < 6; i++) {
            inner.add(filler());
        }
        page.addComponent(0, inner);
        f.revalidate();
        DisplayTest.flushEdt();
        assertTrue(inner.isScrollableY(), "the inner container has to overflow too");

        // One point, held still. A container's absolute position follows its own scroll,
        // so recomputing "the middle of inner" after each notch walks the cursor off it --
        // the pointer does not move when the content under it does.
        int x = inner.getAbsoluteX() + inner.getWidth() / 2;
        int y = inner.getAbsoluteY() + inner.getHeight() / 2;
        int innerMax = inner.getScrollDimension().getHeight() - inner.getHeight();

        // While the inner container can move, it takes the wheel and the page stays put.
        wheelAt(x, y, 0, -px(20));
        assertTrue(inner.getScrollY() > 0, "the inner container scrolls first");
        assertEquals(0, page.getScrollY(), "and the page does not move under it");

        // Drive it to its end.
        for (int i = 0; i < 40 && inner.getScrollY() < innerMax; i++) {
            wheelAt(x, y, 0, -px(20));
        }
        assertEquals(innerMax, inner.getScrollY(), "the inner container reaches its end");
        int pageBefore = page.getScrollY();

        wheelAt(x, y, 0, -px(20));

        assertEquals(innerMax, inner.getScrollY(), "the inner container has nothing left to give");
        assertTrue(page.getScrollY() > pageBefore,
                "so the wheel carries on to the page instead of stopping dead");
    }

    @FormTest
    void aWheelReachesWhatTheKeyboardIsCovering() {
        Form f = scrollingForm();
        Container page = f.getContentPane();
        int x = page.getAbsoluteX() + page.getWidth() / 2;
        int y = page.getAbsoluteY() + page.getHeight() / 2;
        int plainMax = page.getScrollDimension().getHeight() - page.getHeight();
        for (int i = 0; i < 200 && page.getScrollY() < plainMax; i++) {
            wheelAt(x, y, 0, -px(30));
        }
        assertEquals(plainMax, page.getScrollY(), "the bottom of the content with no keyboard up");

        // With the keyboard covering the bottom, the range grows by what it hides -- which
        // is how the drag path and setScrollY compute it. A wheel clamped to the smaller
        // range leaves whatever is behind the keyboard unreachable with a trackpad.
        f.setOverrideInvisibleAreaUnderVKB(px(30));
        wheelAt(x, y, 0, -px(30));

        assertTrue(page.getScrollY() > plainMax,
                "the wheel has to reach past the keyboard, got " + page.getScrollY()
                        + " with the plain end at " + plainMax);
        f.setOverrideInvisibleAreaUnderVKB(-1);
    }

    @FormTest
    void aWheelLandsOnTheGridWhenTheTargetSnapsToOne() {
        Form f = new Form("snapping", new BorderLayout());
        Container page = f.getContentPane();
        page.setLayout(BoxLayout.y());
        page.setScrollableY(true);
        int row = px(30);
        for (int i = 0; i < 60; i++) {
            Label l = new Label("row");
            l.setPreferredH(row);
            page.add(l);
        }
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();
        // After show, not before: initialising a component resets this to the look and
        // feel's default, so setting it earlier is silently discarded.
        page.setSnapToGrid(true);
        assertTrue(page.isSnapToGrid(), "the container has to actually be snapping");

        // A notch that is not a whole number of rows. The snap used to come from the
        // deceleration a drag left behind, and there is no deceleration in a wheel.
        wheelAt(page.getAbsoluteX() + page.getWidth() / 2, page.getAbsoluteY() + page.getHeight() / 2,
                0, -(row + row / 3));

        assertTrue(page.getScrollY() > 0, "the wheel still moves it");
        boolean onARow = false;
        for (int i = 0; i < page.getComponentCount(); i++) {
            if (page.getComponentAt(i).getY() == page.getScrollY()) {
                onARow = true;
                break;
            }
        }
        assertTrue(onARow, "a snapping container has to land on a row, got " + page.getScrollY()
                + " between " + page.getComponentAt(0).getY() + " and " + page.getComponentAt(1).getY());
    }

    @FormTest
    void aTrackpadKeepsMovingASnappingContainer() {
        Form f = new Form("snapping", new BorderLayout());
        Container page = snappingRows(f);
        int x = page.getAbsoluteX() + page.getWidth() / 2;
        int y = page.getAbsoluteY() + page.getHeight() / 2;

        // Deltas far smaller than a row, the way a trackpad sends them. Snapping each one to
        // the nearest row and forgetting the rest pulls every one of them back where it
        // started, and claiming the wheel while doing it stops the page moving either --
        // scrolling looks frozen. What is too small to show is carried to the next event.
        int start = page.getScrollY();
        for (int i = 0; i < 40 && page.getScrollY() == start; i++) {
            wheelPrecise(x, y, 0, -px(3));
        }

        assertTrue(page.getScrollY() > start,
                "small precise deltas accumulate until they move it, got " + page.getScrollY());
        assertTrue(settledOnGrid(page), "and it lands settled on the grid rather than "
                + "between two rows, got " + page.getScrollY());
    }

    @FormTest
    void aSnappingContainerAtItsEndPassesTheWheelOn() {
        Form f = new Form("snapping inside a page", new BorderLayout());
        Container page = f.getContentPane();
        page.setLayout(BoxLayout.y());
        page.setScrollableY(true);
        Container snapping = new Container(BoxLayout.y());
        snapping.setScrollableY(true);
        snapping.setPreferredH(px(60));
        for (int i = 0; i < 8; i++) {
            Label l = new Label("row");
            l.setPreferredH(px(30));
            snapping.add(l);
        }
        page.add(snapping);
        for (int i = 0; i < 60; i++) {
            page.add(filler());
        }
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();
        snapping.setSnapToGrid(true);
        assertTrue(snapping.isSnapToGrid(), "the inner container has to actually be snapping");

        int x = snapping.getAbsoluteX() + snapping.getWidth() / 2;
        int y = snapping.getAbsoluteY() + snapping.getHeight() / 2;
        int innerMax = snapping.getScrollDimension().getHeight() - snapping.getHeight();
        for (int i = 0; i < 60 && snapping.getScrollY() < innerMax - px(30); i++) {
            wheelAt(x, y, 0, -px(30));
        }
        assertEquals(0, page.getScrollY(), "the inner container takes the wheel while it can move");
        int end = snapping.getScrollY();

        // Pinned at the end, so the carry cannot grow either: there is genuinely nothing
        // left to give and the page takes over. A component that merely cannot show THIS
        // notch keeps it for the next one; one that can never show another passes it on.
        for (int i = 0; i < 10; i++) {
            wheelAt(x, y, 0, -px(30));
        }

        assertTrue(snapping.getScrollY() >= end, "the inner container is at its end");
        assertTrue(page.getScrollY() > 0, "so the page took the wheel instead of it being swallowed");
    }

    @FormTest
    void aWheelBringsAFadedScrollbarBack() {
        boolean pureTouch = Display.getInstance().isPureTouch();
        Form f = scrollingForm();
        Container page = f.getContentPane();
        boolean fading = page.getUIManager().getLookAndFeel().isFadeScrollBar();
        try {
            // The fade is off in this look and feel and selection rendering pins the
            // opacity, so both are switched on to have something to fade at all.
            page.getUIManager().getLookAndFeel().setFadeScrollBar(true);
            Display.getInstance().setPureTouch(true);
            fadeOut(page);
            assertEquals(0, page.getScrollOpacity(), "the scrollbar has to have faded out first");
            // One tick past the fade is where the component drops its own animation, so
            // there is nothing left to bring the scrollbar back on its own.
            assertFalse(f.hasAnimations(), "the finished fade deregisters itself");

            wheelAt(page.getAbsoluteX() + page.getWidth() / 2,
                    page.getAbsoluteY() + page.getHeight() / 2, 0, -px(30));

            // A press or a release restores this, and the wheel used to be both. Without it
            // the first fade is permanent for anyone using a wheel: the content moves and
            // nothing says where in it they are.
            assertEquals(0xff, page.getScrollOpacity(),
                    "a wheel that moved the content brings its scrollbar back");

            // And something has to be registered to fade it out again. Asserting that it
            // CAN fade would prove nothing here -- this test drives animate() itself, and
            // the opacity comes down whether or not the framework would ever call it.
            assertTrue(f.hasAnimations(),
                    "the restored scrollbar has an animation to fade it out again");

            fadeOut(page);
            assertEquals(0, page.getScrollOpacity(),
                    "and it does fade out again instead of staying lit");
            // Nothing may be left registered on a form this test is about to walk away
            // from. An abandoned form that still animates starves the event thread, and
            // the next class to want it fails with display-not-initialized -- which is a
            // failure in someone else's test that this one caused.
            assertFalse(f.hasAnimations(),
                    "the fade deregisters again, leaving nothing animating on an abandoned form");
        } finally {
            page.getUIManager().getLookAndFeel().setFadeScrollBar(fading);
            Display.getInstance().setPureTouch(pureTouch);
        }
    }

    @FormTest
    void aListenerAboveTheComponentStillBeatsItsBuiltInHandling() {
        Form f = new Form("ancestor listener", new BorderLayout());
        final WheelHandlingComponent target = new WheelHandlingComponent();
        target.setPreferredH(px(15));
        Container holder = new Container(BoxLayout.y());
        holder.add(target);
        // On the ancestor, not on the component that handles the wheel itself. An
        // application that binds control plus wheel to its own zoom does it once, high up.
        holder.addMouseWheelListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                evt.consume();
            }
        });
        f.getContentPane().setLayout(BoxLayout.y());
        f.getContentPane().add(holder);
        f.getContentPane().add(filler());
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();

        wheel(target, 0, -px(20));

        assertEquals(0, target.wheels(),
                "a listener above the component consumes before the component's own handling");
    }

    @FormTest
    void aListenerThatChangesTheScreenStopsTheGestureThere() {
        Form f = scrollingForm();
        final Container page = f.getContentPane();
        final WheelHandlingComponent target = new WheelHandlingComponent();
        target.setPreferredH(px(15));
        page.addComponent(0, target);
        // Does not consume, but shows another form: the tree the wheel was aimed at is no
        // longer what anyone is looking at by the time the built-in handlers would run.
        target.addMouseWheelListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                new Form("elsewhere", new BorderLayout()).show();
            }
        });
        f.revalidate();
        DisplayTest.flushEdt();
        int before = page.getScrollY();

        wheel(target, 0, -px(20));

        assertEquals(0, target.wheels(),
                "nothing built in may act on a component whose form has been left behind");
        assertEquals(before, page.getScrollY(), "and the form nobody is looking at is not scrolled");
        f.show();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aNotchTooSmallToChangeTheRowNeverPublishesAPositionOffTheGrid() {
        // Spinner3D mirrors this container's scroll into SpinnerNode, which derives its
        // selected index from the position by rounding down. So every position published
        // here is a selection, and one that is off the grid is a selection nobody chose:
        // computing the snap by scrolling to the raw position and coming back fired a
        // change to the previous row and a second one back, into application listeners and
        // into the list model, for a notch that moved nothing.
        Form f = new Form("grid", new BorderLayout());
        ExactGridContainer page = new ExactGridContainer();
        page.setScrollableY(true);
        for (int i = 0; i < 60; i++) {
            Label l = new Label("row");
            l.setPreferredH(px(30));
            page.add(l);
        }
        f.getContentPane().setLayout(BoxLayout.y());
        f.getContentPane().add(page);
        f.getContentPane().setScrollableY(false);
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();
        page.setSnapToGrid(true);
        int x = page.getAbsoluteX() + page.getWidth() / 2;
        int y = page.getAbsoluteY() + page.getHeight() / 2;
        int pitch = page.getComponentAt(1).getY() - page.getComponentAt(0).getY();
        assertTrue(pitch > 4, "the rows have to be tall enough for a quarter of one to be a real delta");

        // Off row zero, on an exact row, carrying nothing. Scrolled there directly rather
        // than by wheel: a run-up of partial notches leaves a remainder behind, and the
        // remainder moving the next notch a whole row is correct behaviour that would hide
        // what this test is about. A scroll that is not the wheel's own drops the carry.
        int settled = page.getComponentAt(2).getY();
        page.setScrollY(settled);
        DisplayTest.flushEdt();
        assertTrue(settledOnGrid(page), "the setup has to leave it settled on a row");
        assertEquals(0, page.wheelSnapRemainderY, "and carrying nothing into the notch below");

        final List<Integer> published = new ArrayList<Integer>();
        page.addScrollListener(new ScrollListener() {
            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                published.add(Integer.valueOf(scrollY));
            }
        });

        // A quarter of a row back towards the previous one: too little to leave this row.
        wheelPrecise(x, y, 0, pitch / 4);

        assertEquals(settled, page.getScrollY(), "it stays on the row it was on");
        assertTrue(published.isEmpty(),
                "a notch that leaves the row alone has to publish nothing at all, got " + published);
    }

    @FormTest
    void aDragBetweenNotchesDropsWhatTheWheelWasCarrying() {
        Form f = new Form("carry", new BorderLayout());
        // An exact grid, the way Spinner3D overrides it. A plain Container calls anything
        // within two pixels of a row settled, and that tolerance is noise in a test about
        // what the carry is worth.
        ExactGridContainer page = new ExactGridContainer();
        page.setScrollableY(true);
        for (int i = 0; i < 60; i++) {
            Label l = new Label("row");
            l.setPreferredH(px(30));
            page.add(l);
        }
        f.getContentPane().setLayout(BoxLayout.y());
        f.getContentPane().add(page);
        f.getContentPane().setScrollableY(false);
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();
        page.setSnapToGrid(true);
        assertTrue(page.isScrollableY() && page.isSnapToGrid(), "the container has to scroll and snap");
        int x = page.getAbsoluteX() + page.getWidth() / 2;
        int y = page.getAbsoluteY() + page.getHeight() / 2;
        int pitch = page.getComponentAt(1).getY() - page.getComponentAt(0).getY();

        // One notch short of the row: carried, not shown.
        int carried = 0;
        while (page.getScrollY() == 0 && carried < 40) {
            wheelPrecise(x, y, 0, -(pitch / 4));
            carried++;
        }
        assertTrue(page.getScrollY() > 0, "it advances a row once enough has accumulated");
        int onARow = page.getScrollY();
        wheelPrecise(x, y, 0, -(pitch / 4));
        assertEquals(onARow, page.getScrollY(), "and the next quarter-notch is carried, not shown");

        // Something else moves it. The carry describes a distance from a position this
        // component is no longer at, so keeping it would make the next notch travel further
        // than the notch asked for.
        page.setScrollY(0);
        wheelPrecise(x, y, 0, -(pitch / 4));

        // Less than a row: a fresh quarter-notch settles on the first row, while a carry
        // left over from before the drag would have been nearly a full row and jumped one.
        assertTrue(page.getScrollY() < pitch,
                "a quarter-notch after a drag moves no further than a quarter-notch does, got "
                        + page.getScrollY() + " with rows " + pitch + " apart");
    }

    @FormTest
    void aDisabledComponentTakesNoWheel() {
        Form f = new Form("disabled", new BorderLayout());
        Container page = f.getContentPane();
        page.setLayout(BoxLayout.y());
        page.setScrollableY(true);
        Container inner = new Container(BoxLayout.y());
        inner.setScrollableY(true);
        inner.setPreferredH(px(60));
        WheelHandlingComponent handler = new WheelHandlingComponent();
        handler.setPreferredH(px(15));
        inner.add(handler);
        for (int i = 0; i < 8; i++) {
            inner.add(filler());
        }
        page.add(inner);
        for (int i = 0; i < 60; i++) {
            page.add(filler());
        }
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();
        // Disabling used to stop the wheel because the gesture it was emulated with went
        // through Form.pointerDragged, which gates on isEnabled.
        handler.setEnabled(false);
        inner.setEnabled(false);

        wheel(handler, 0, -px(30));

        assertEquals(0, handler.wheels(), "a disabled component is offered no wheel");
        assertEquals(0, inner.getScrollY(), "and a disabled container does not scroll");
        assertTrue(page.getScrollY() > 0, "the enabled ancestor takes it instead");
    }

    @FormTest
    void aListenerThatMovesTheComponentDropsTheCarry() {
        Form f = new Form("carry vs listener", new BorderLayout());
        final ExactGridContainer page = new ExactGridContainer();
        page.setScrollableY(true);
        for (int i = 0; i < 60; i++) {
            Label l = new Label("row");
            l.setPreferredH(px(30));
            page.add(l);
        }
        f.getContentPane().setLayout(BoxLayout.y());
        f.getContentPane().add(page);
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();
        page.setSnapToGrid(true);
        int x = page.getAbsoluteX() + page.getWidth() / 2;
        int y = page.getAbsoluteY() + page.getHeight() / 2;
        int pitch = page.getComponentAt(1).getY() - page.getComponentAt(0).getY();

        // Build up an actual carry. Read rather than assumed: whether a notch leaves
        // something over, and with which sign, depends on where the snap rounded to.
        for (int i = 0; i < 20 && page.wheelSnapRemainderY == 0; i++) {
            wheelPrecise(x, y, 0, -(pitch / 4));
        }
        assertTrue(page.wheelSnapRemainderY != 0, "there has to be something carried");

        // A listener consumes the next one and moves the component itself. That happens
        // while a wheel is dispatching, so "is a wheel in flight" cannot tell it from the
        // framework's own snap -- but it is not this component being moved BY the scroll.
        final boolean[] moved = new boolean[1];
        ActionListener consumer = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (!moved[0]) {
                    moved[0] = true;
                    page.setScrollY(0);
                }
                evt.consume();
            }
        };
        page.addMouseWheelListener(consumer);
        wheelPrecise(x, y, 0, -(pitch / 4));
        assertTrue(moved[0], "the listener has to have run for this to mean anything");
        page.removeMouseWheelListener(consumer);

        // The component was moved by something that is not the wheel scrolling it, so what
        // the wheel was carrying no longer describes anything.
        assertEquals(0, page.wheelSnapRemainderY,
                "a listener moving the component itself drops the carry");

        wheelPrecise(x, y, 0, -(pitch / 4));

        assertTrue(page.getScrollY() < pitch,
                "so the next notch travels a notch, got " + page.getScrollY()
                        + " with rows " + pitch + " apart");
    }

    @FormTest
    void aWheelInADesktopWindowScrollsThatWindow() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("scroller", new BorderLayout());
        Container content = new Container(BoxLayout.y());
        content.setScrollableY(true);
        PointerCountingComponent target = new PointerCountingComponent();
        target.setPreferredH(px(15));
        content.add(target);
        for (int i = 0; i < 40; i++) {
            content.add(filler());
        }
        w.add(BorderLayout.CENTER, content);
        w.setWindowSize(300, 200);
        w.show();
        w.revalidate();
        DisplayTest.flushEdt();

        Display.impl.windowPointerWheelMoved(w.getWindowId(),
                target.getAbsoluteX() + target.getWidth() / 2,
                target.getAbsoluteY() + target.getHeight() / 2, 0, -px(20), false, 0);
        DisplayTest.flushEdt();

        assertTrue(content.getScrollY() > 0, "a window's own content scrolls");
        assertEquals(0, target.pointerEvents(), "and nothing in it is told about a pointer");
        w.dispose();
    }

    private Form scrollingForm() {
        Form f = new Form("scroller", new BorderLayout());
        Container content = f.getContentPane();
        content.setLayout(BoxLayout.y());
        content.setScrollableY(true);
        for (int i = 0; i < 60; i++) {
            content.add(filler());
        }
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();
        return f;
    }

    /// Plays one wheel notch over the middle of `over` and lets it be handled.
    private void wheel(Component over, int deltaX, int deltaY) {
        wheelAt(over.getAbsoluteX() + over.getWidth() / 2,
                over.getAbsoluteY() + over.getHeight() / 2, deltaX, deltaY);
    }

    private void wheelAt(int x, int y, int deltaX, int deltaY) {
        Display.impl.pointerWheelMoved(x, y, deltaX, deltaY, false, 0);
        DisplayTest.flushEdt();
    }

    /// Runs the fade to completion and one tick past it, which is where the component
    /// deregisters its own animation -- and leaves nothing registered on a form the test is
    /// about to abandon.
    private void fadeOut(Component c) {
        for (int i = 0; i < 200 && c.getScrollOpacity() > 0; i++) {
            c.animate();
        }
        c.animate();
    }

    /// Whether the component is where its own grid says it should be. Asked of the
    /// component rather than measured against its children on purpose: a plain Container
    /// treats anything within a couple of pixels of a row as settled, while Spinner3D
    /// overrides the grid with exact arithmetic -- and the invariant that matters is that
    /// the wheel leaves it wherever IT considers settled, because that is the position its
    /// selected index is read from.
    private boolean settledOnGrid(Container c) {
        return c.getGridPosY() == c.getScrollY();
    }

    /// A trackpad's deltas, which arrive small and often and are flagged precise.
    private void wheelPrecise(int x, int y, int deltaX, int deltaY) {
        Display.impl.pointerWheelMoved(x, y, deltaX, deltaY, true, 0);
        DisplayTest.flushEdt();
    }

    /// A scrollable page of equal rows that snaps to them. Snapping is switched on after
    /// show, because initialising a component resets it to the look and feel's default.
    private Container snappingRows(Form f) {
        Container page = f.getContentPane();
        page.setLayout(BoxLayout.y());
        page.setScrollableY(true);
        for (int i = 0; i < 60; i++) {
            Label l = new Label("row");
            l.setPreferredH(px(30));
            page.add(l);
        }
        f.show();
        f.revalidate();
        DisplayTest.flushEdt();
        page.setSnapToGrid(true);
        assertTrue(page.isSnapToGrid(), "the container has to actually be snapping");
        return page;
    }

    private static int px(int mm) {
        return Display.getInstance().convertToPixels(mm);
    }

    private Component filler() {
        Label l = new Label("filler");
        l.setPreferredH(px(30));
        return l;
    }

    /// Snaps to its own rows exactly, with none of the tolerance a plain Container allows
    /// -- which is what Spinner3D does, and what makes a grid assertion mean one thing.
    private static final class ExactGridContainer extends Container {
        ExactGridContainer() {
            super(BoxLayout.y());
        }

        @Override
        protected int getGridPosY() {
            if (getComponentCount() < 2) {
                return getScrollY();
            }
            int first = getComponentAt(0).getY();
            int pitch = getComponentAt(1).getY() - first;
            if (pitch <= 0) {
                return getScrollY();
            }
            int rows = Math.round((getScrollY() - first) / (float) pitch);
            return Math.max(first, first + rows * pitch);
        }
    }

    /// Counts every pointer event it is told about, so a test can assert none arrived.
    private static final class PointerCountingComponent extends Component {
        private int pointerEvents;

        int pointerEvents() {
            return pointerEvents;
        }

        @Override
        public void pointerPressed(int x, int y) {
            pointerEvents++;
        }

        @Override
        public void pointerDragged(int x, int y) {
            pointerEvents++;
        }

        @Override
        public void pointerReleased(int x, int y) {
            pointerEvents++;
        }
    }

    /// Moves content of its own, the way an editor or a viewer does.
    private static final class WheelHandlingComponent extends Component {
        private int wheels;

        int wheels() {
            return wheels;
        }

        @Override
        protected boolean mouseWheel(WheelEvent ev) {
            wheels++;
            return true;
        }
    }
}
