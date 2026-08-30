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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A `ComboBox` inside a `Window`.
///
/// The popup is a `Dialog`, so before the dialog learned about top levels clicking
/// one of these inside a window did nothing whatsoever: the placement code
/// dereferenced the null `getComponentForm()`, and the EDT violation guard added for
/// issue #4726 swallowed the resulting null and returned.
class ComboBoxInWindowTest extends UITestBase {

    private static boolean isUnder(Component ancestor, Component c) {
        Component probe = c;
        while (probe != null) {
            if (probe == ancestor) {
                return true;
            }
            probe = probe.getParent();
        }
        return false;
    }

    /// A combo box whose popup reports where it landed and then dismisses itself.
    ///
    /// The popup blocks its caller until it goes, so dismissing it from its own show
    /// listener keeps the whole case on one thread. A background caller parked in
    /// invokeAndBlock outlives the test if anything goes wrong with it.
    private static final class ProbeComboBox extends ComboBox<String> {
        private final Component expectedAncestor;
        private boolean everShown;
        /// Whether the popup was under the expected ancestor while it was up. Recorded
        /// there rather than afterwards: the popup dismisses itself, so by the time the
        /// assertions run it has already left the hierarchy.
        private boolean shownUnderAncestor;

        ProbeComboBox(Component expectedAncestor, String... items) {
            super(items);
            this.expectedAncestor = expectedAncestor;
        }

        @Override
        protected Dialog createPopupDialog(List<String> l) {
            final Dialog d = super.createPopupDialog(l);
            d.addShowListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    everShown = true;
                    shownUnderAncestor = isUnder(expectedAncestor, d);
                    d.dispose();
                }
            });
            return d;
        }
    }

    /// Runs a modal popup off the event dispatch thread and waits for it to finish.
    ///
    /// A modal show parks its caller, so it cannot be driven from the dispatch thread
    /// the test itself runs on. This is the same shape `WindowTest` uses for modal
    /// windows. The popup dismisses itself from its show listener, so the wait is
    /// short, and the thread is asserted to have ended rather than silently abandoned:
    /// a caller left parked in invokeAndBlock outlives the test.
    private static void clickOffEdt(final ProbeComboBox combo) throws Exception {
        Thread caller = new Thread(new Runnable() {
            @Override
            public void run() {
                combo.fireClicked();
            }
        }, "cn1-test-combo");
        caller.start();
        for (int i = 0; i < 400 && caller.isAlive(); i++) {
            DisplayTest.flushEdt();
            Thread.sleep(5);
        }
        caller.join(2000);
        assertFalse(caller.isAlive(),
                "the popup has to come back down, or its caller is parked for good");
    }

    @FormTest
    void clickingAComboBoxInAWindowOpensItsPopupOnThatWindow() throws Exception {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(600, 500);
        ProbeComboBox combo = new ProbeComboBox(w, "one", "two", "three");
        w.add(BorderLayout.NORTH, combo);
        w.show();
        DisplayTest.flushEdt();

        clickOffEdt(combo);
        DisplayTest.flushEdt();

        assertTrue(combo.everShown,
                "a combo box in a window has to open its popup, not swallow the press");
        assertTrue(combo.shownUnderAncestor,
                "and the popup belongs to the window it was clicked in, not the main form");
        assertFalse(combo.isShowingPopupDialog(), "the popup is gone again afterwards");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aComboBoxOnAFormStillTakesOverTheMainSurface() throws Exception {
        // The compatibility half: nothing about the single window path may move.
        Form main = new Form("main", new BorderLayout());
        ProbeComboBox combo = new ProbeComboBox(main, "a", "b");
        main.add(BorderLayout.NORTH, combo);
        main.show();
        DisplayTest.flushEdt();

        clickOffEdt(combo);
        DisplayTest.flushEdt();

        assertTrue(combo.everShown, "the popup still opens on a plain form");
        assertFalse(combo.shownUnderAncestor,
                "and on a form it is still a dialog that takes over the surface rather "
                        + "than a component inside the form");
    }

    /// A combo box whose popup selects an item instead of being dismissed from outside.
    ///
    /// The probe above dismisses its popup from a show listener, which is exactly why
    /// it could not catch this: selecting an item goes down a different path, and that
    /// path used to leave the popup open and its caller blocked.
    private static final class SelectingComboBox extends ComboBox<String> {
        private Dialog popup;

        SelectingComboBox(String... items) {
            super(items);
        }

        @Override
        protected Dialog createPopupDialog(List<String> l) {
            popup = super.createPopupDialog(l);
            final List<String> list = l;
            popup.addShowListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    // Pick the second item the way a tap does.
                    list.setSelectedIndex(1);
                    list.fireActionEvent();
                }
            });
            return popup;
        }
    }

    @FormTest
    void selectingAnItemClosesAComboPopupInsideAWindow() throws Exception {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(600, 500);
        final SelectingComboBox combo = new SelectingComboBox("one", "two", "three");
        w.add(BorderLayout.NORTH, combo);
        w.show();
        DisplayTest.flushEdt();

        Thread caller = new Thread(new Runnable() {
            @Override
            public void run() {
                combo.fireClicked();
            }
        }, "cn1-test-combo-select");
        caller.start();
        try {
            for (int i = 0; i < 400 && caller.isAlive(); i++) {
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            caller.join(2000);
            assertFalse(caller.isAlive(),
                    "selecting an item has to close the popup, or its caller is blocked "
                            + "for good");
            assertEquals(1, combo.getSelectedIndex(), "and the selection sticks");
        } finally {
            if (combo.popup != null) {
                combo.popup.dispose();
            }
            DisplayTest.flushEdt();
            caller.join(2000);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    @FormTest
    void selectingAnItemStillClosesAComboPopupOnAForm() throws Exception {
        Form main = new Form("main", new BorderLayout());
        final SelectingComboBox combo = new SelectingComboBox("a", "b", "c");
        main.add(BorderLayout.NORTH, combo);
        main.show();
        DisplayTest.flushEdt();

        Thread caller = new Thread(new Runnable() {
            @Override
            public void run() {
                combo.fireClicked();
            }
        }, "cn1-test-combo-select-form");
        caller.start();
        try {
            for (int i = 0; i < 400 && caller.isAlive(); i++) {
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            caller.join(2000);
            assertFalse(caller.isAlive(), "the single surface path is unchanged");
            assertEquals(1, combo.getSelectedIndex());
        } finally {
            if (combo.popup != null) {
                combo.popup.dispose();
            }
            DisplayTest.flushEdt();
            caller.join(2000);
        }
    }

    /// A combo whose popup stays up until the test takes it down, and which hands the
    /// popup back so a command can be pushed through it while it is still showing.
    private static final class HoldingComboBox extends ComboBox<String> {
        private Dialog popup;
        private boolean shown;

        HoldingComboBox(String... items) {
            super(items);
        }

        @Override
        protected Dialog createPopupDialog(List<String> l) {
            Dialog d = super.createPopupDialog(l);
            popup = d;
            d.addShowListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    shown = true;
                }
            });
            return d;
        }
    }

    @FormTest
    void aPopupClosingInOneWindowLeavesAnothersPopupRouting() throws Exception {
        // A popup is modal only to the surface it is on, so with windows two can be
        // open at once. They shared one process-wide flag, and whichever came back
        // first cleared it: the popup still on screen silently stopped routing its own
        // select and cancel.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window a = new Window("a", new BorderLayout());
        a.setWindowSize(400, 300);
        final HoldingComboBox comboA = new HoldingComboBox("one", "two");
        comboA.setIncludeSelectCancel(true);
        a.add(BorderLayout.NORTH, comboA);
        a.show();

        Window b = new Window("b", new BorderLayout());
        b.setWindowSize(400, 300);
        ProbeComboBox comboB = new ProbeComboBox(b, "one", "two");
        comboB.setIncludeSelectCancel(true);
        b.add(BorderLayout.NORTH, comboB);
        b.show();
        DisplayTest.flushEdt();

        Thread callerA = new Thread(new Runnable() {
            @Override
            public void run() {
                comboA.fireClicked();
            }
        }, "cn1-test-combo-a");
        callerA.start();
        for (int i = 0; i < 400 && !comboA.shown; i++) {
            DisplayTest.flushEdt();
            Thread.sleep(5);
        }
        try {
            assertTrue(comboA.shown, "precondition: the first window's popup is up");

            // The second window's popup opens and closes underneath it.
            clickOffEdt(comboB);
            DisplayTest.flushEdt();

            // Now push a command through the popup that is still showing. While it
            // routes its own commands an unrelated command goes to the focused
            // component, not to the command listener; once the routing is lost the
            // listener hears it.
            final int[] heard = new int[1];
            comboA.popup.addCommandListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    heard[0]++;
                }
            });
            Command unrelated = new Command("unrelated");
            comboA.popup.actionCommandImpl(unrelated, new ActionEvent(unrelated));
            assertEquals(0, heard[0],
                    "the popup still on screen has to keep routing its own commands");
        } finally {
            if (comboA.popup != null) {
                comboA.popup.dispose();
            }
            for (int i = 0; i < 400 && callerA.isAlive(); i++) {
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            callerA.join(2000);
            assertFalse(callerA.isAlive(), "its caller must not stay parked");
            a.dispose();
            b.dispose();
            DisplayTest.flushEdt();
        }
    }

    /// Opens a holding popup off the event thread and waits until it is on screen.
    private static Thread openHolding(final HoldingComboBox combo) throws Exception {
        Thread caller = new Thread(new Runnable() {
            @Override
            public void run() {
                combo.fireClicked();
            }
        }, "cn1-test-combo-hold");
        caller.start();
        for (int i = 0; i < 400 && !combo.shown; i++) {
            DisplayTest.flushEdt();
            Thread.sleep(5);
        }
        return caller;
    }

    /// Takes a holding popup back down and waits for its parked caller to return.
    private static void closeHolding(HoldingComboBox combo, Thread caller) throws Exception {
        if (combo.popup != null) {
            combo.popup.dispose();
        }
        for (int i = 0; i < 400 && caller.isAlive(); i++) {
            DisplayTest.flushEdt();
            Thread.sleep(5);
        }
        caller.join(2000);
        assertFalse(caller.isAlive(), "its caller must not stay parked");
    }

    @FormTest
    void twoPopupsClosingOutOfOrderGiveTheBlurBack() throws Exception {
        // The blur radius is one process-wide value and a popup is modal only to the
        // surface it is on, so two windows can each have one open. Saved and restored per
        // call, the second popup captured the -1 the first had already installed; the
        // first then put the real value back while the second was still up, and the
        // second wrote -1 back last -- blur off for every dialog made afterwards.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        float original = Dialog.getDefaultBlurBackgroundRadius();
        Window a = new Window("a", new BorderLayout());
        Window b = new Window("b", new BorderLayout());
        Thread callerA = null;
        Thread callerB = null;
        final HoldingComboBox comboA = new HoldingComboBox("one", "two");
        final HoldingComboBox comboB = new HoldingComboBox("one", "two");
        try {
            Dialog.setDefaultBlurBackgroundRadius(7f);

            a.setWindowSize(400, 300);
            a.add(BorderLayout.NORTH, comboA);
            a.show();
            b.setWindowSize(400, 300);
            b.add(BorderLayout.NORTH, comboB);
            b.show();
            DisplayTest.flushEdt();
            callerA = openHolding(comboA);
            assertTrue(comboA.shown, "precondition: the first popup is up");
            callerB = openHolding(comboB);
            assertTrue(comboB.shown, "precondition: the second is up as well");
            assertEquals(-1f, Dialog.getDefaultBlurBackgroundRadius(), 0.001f,
                    "precondition: a popup turns the blur off while it is up");

            // The first one closes first, while the second is still showing.
            closeHolding(comboA, callerA);
            callerA = null;
            DisplayTest.flushEdt();
            assertEquals(-1f, Dialog.getDefaultBlurBackgroundRadius(), 0.001f,
                    "the popup still up has to keep it off");
            closeHolding(comboB, callerB);
            callerB = null;
            DisplayTest.flushEdt();
            assertEquals(7f, Dialog.getDefaultBlurBackgroundRadius(), 0.001f,
                    "and once both have gone the application's own value is back");
        } finally {
            if (callerA != null) {
                closeHolding(comboA, callerA);
            }
            if (callerB != null) {
                closeHolding(comboB, callerB);
            }
            Dialog.setDefaultBlurBackgroundRadius(original);
            a.dispose();
            b.dispose();
            DisplayTest.flushEdt();
        }
    }
}
