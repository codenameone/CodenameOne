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
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A `ComboBox` inside a `Window`.
///
/// The popup is a `Dialog`, so before the dialog learned about top levels clicking
/// one of these inside a window did nothing whatsoever -- the placement code
/// dereferenced the null `getComponentForm()` and the press was swallowed.
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

    @FormTest
    void clickingAComboBoxInAWindowOpensItsPopupOnThatWindow() throws Exception {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        final Window w = new Window("host", new BorderLayout());
        w.setWindowSize(600, 500);
        final ComboBox<String> combo = new ComboBox<String>("one", "two", "three");
        w.add(BorderLayout.NORTH, combo);
        w.show();
        DisplayTest.flushEdt();

        // fireClicked blocks on the popup, so drive it from a background thread.
        Thread caller = new Thread(new Runnable() {
            @Override
            public void run() {
                combo.fireClicked();
            }
        }, "cn1-test-combo");
        caller.start();
        try {
            for (int i = 0; i < 300 && !combo.isShowingPopupDialog(); i++) {
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            assertTrue(combo.isShowingPopupDialog(),
                    "a combo box in a window has to open its popup, not swallow the press");

            Container layer = w.getFormLayeredPaneIfExists();
            assertNotNull(layer, "and the popup belongs to the window it was clicked in");
            assertTrue(layer.getComponentCount() > 0);
        } finally {
            // Dismiss whatever went up so the thread can finish.
            for (int i = 0; i < 300 && caller.isAlive(); i++) {
                Container layer = w.getFormLayeredPaneIfExists();
                if (layer != null) {
                    for (Component c : layer.getChildrenAsList(true)) {
                        for (Component g : ((Container) c).getChildrenAsList(true)) {
                            if (g instanceof Dialog) {
                                ((Dialog) g).dispose();
                            }
                        }
                    }
                }
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            caller.join(2000);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    @FormTest
    void aComboBoxOnAFormStillUsesTheMainSurface() throws Exception {
        // The compatibility half: nothing about the single window path may move.
        final Form main = new Form("main", new BorderLayout());
        final ComboBox<String> combo = new ComboBox<String>("a", "b");
        main.add(BorderLayout.NORTH, combo);
        main.show();
        DisplayTest.flushEdt();

        Thread caller = new Thread(new Runnable() {
            @Override
            public void run() {
                combo.fireClicked();
            }
        }, "cn1-test-combo-form");
        caller.start();
        try {
            for (int i = 0; i < 300 && !combo.isShowingPopupDialog(); i++) {
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            assertTrue(combo.isShowingPopupDialog());
            Form current = Display.getInstance().getCurrent();
            assertTrue(current instanceof Dialog || current == main,
                    "on a form the popup is still a dialog that takes over the surface");
        } finally {
            for (int i = 0; i < 300 && caller.isAlive(); i++) {
                Form cur = Display.getInstance().getCurrent();
                if (cur instanceof Dialog) {
                    ((Dialog) cur).dispose();
                }
                Form up = Display.getInstance().getCurrentUpcoming();
                if (up instanceof Dialog) {
                    ((Dialog) up).dispose();
                }
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            caller.join(2000);
            DisplayTest.flushEdt();
        }
    }
}
