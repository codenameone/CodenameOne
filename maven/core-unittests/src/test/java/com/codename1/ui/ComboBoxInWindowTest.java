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
}
