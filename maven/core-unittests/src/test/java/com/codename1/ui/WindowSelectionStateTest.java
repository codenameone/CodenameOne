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
import com.codename1.testing.TestWindowManager;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the pressed-selection state against leaking between windows.
 *
 * <p>In pureTouch mode a component shows its selection only while a contact is down
 * on it, which {@code Display.shouldRenderSelection(Component)} answers. That used to
 * read one singleton flag and one pair of global pointer coordinates, so with a
 * contact down in two windows whichever window's packet ran last owned both: a
 * release in one window dropped the other's still-held selection, and a component in
 * one window ended up tested against the other's coordinates -- which are window
 * relative, so the two are not even the same origin.</p>
 *
 * @author Shai Almog
 */
class WindowSelectionStateTest extends UITestBase {

    /// Every window this test opened, so none outlives it. A window left showing
    /// keeps work queued on the event dispatch thread, and the next test's setup then
    /// times out waiting for a queue that never drains -- which is a failure in a
    /// test that has nothing to do with windows.
    private final java.util.List<Window> opened = new java.util.ArrayList<Window>();

    private void disposeAll() {
        for (int iter = 0; iter < opened.size(); iter++) {
            opened.get(iter).dispose();
        }
        opened.clear();
        DisplayTest.flushEdt();
    }

    /// A shown window with one component filling it, and its real laid-out geometry --
    /// set coordinates by hand and the layout pass just overwrites them.
    private Component content(Window w) {
        Label l = new Label("x");
        w.add(BorderLayout.CENTER, l);
        w.show();
        opened.add(w);
        DisplayTest.flushEdt();
        return l;
    }

    /// A point inside the component, in its own window's coordinates.
    private int[] insideX(Component c) {
        return new int[] { c.getAbsoluteX() + c.getWidth() / 2 };
    }

    private int[] insideY(Component c) {
        return new int[] { c.getAbsoluteY() + c.getHeight() / 2 };
    }

    @FormTest
    void aReleaseInOneWindowLeavesTheOtherWindowsSelectionAlone() {
        implementation.setMultiWindowSupported(true);
        Display d = Display.getInstance();
        d.setPureTouch(true);
        try {
            Window a = new Window("a", new BorderLayout());
            a.setWindowSize(400, 300);
            Component inA = content(a);
            Window b = new Window("b", new BorderLayout());
            b.setWindowSize(400, 300);
            Component inB = content(b);

            // A contact goes down on the component in A and stays down.
            com.codename1.ui.Desktop.getInstance().windowPointerPressed(a.getWindowId(), insideX(inA), insideY(inA));
            DisplayTest.flushEdt();
            assertTrue(d.shouldRenderSelection(inA),
                    "the component under the held contact must show its selection");

            // A whole press/release cycle happens in B while A is still held.
            com.codename1.ui.Desktop.getInstance().windowPointerPressed(b.getWindowId(), insideX(inB), insideY(inB));
            DisplayTest.flushEdt();
            com.codename1.ui.Desktop.getInstance().windowPointerReleased(b.getWindowId(), insideX(inB), insideY(inB));
            DisplayTest.flushEdt();

            assertFalse(d.shouldRenderSelection(inB),
                    "B was released, so its component must stop showing selection");
            assertTrue(d.shouldRenderSelection(inA),
                    "A is still held: releasing in another window must not clear it");
        } finally {
            disposeAll();
            d.setPureTouch(false);
        }
    }

    @FormTest
    void aComponentIsTestedAgainstItsOwnWindowsCoordinates() {
        implementation.setMultiWindowSupported(true);
        Display d = Display.getInstance();
        d.setPureTouch(true);
        try {
            Window a = new Window("a", new BorderLayout());
            a.setWindowSize(200, 200);
            Component inA = content(a);
            // Deliberately much larger, so a press in the middle of B lands well
            // outside A's component. Window coordinates are window relative, so the
            // two windows' coordinate spaces are not comparable.
            Window b = new Window("b", new BorderLayout());
            b.setWindowSize(900, 700);
            Component inB = content(b);

            com.codename1.ui.Desktop.getInstance().windowPointerPressed(a.getWindowId(), insideX(inA), insideY(inA));
            DisplayTest.flushEdt();
            assertTrue(d.shouldRenderSelection(inA), "held in A, inside its component");

            int bx = insideX(inB)[0];
            int by = insideY(inB)[0];
            assertFalse(inA.contains(bx, by),
                    "the test is only meaningful if B's press point is outside A's component");
            com.codename1.ui.Desktop.getInstance().windowPointerPressed(b.getWindowId(), new int[] { bx }, new int[] { by });
            DisplayTest.flushEdt();

            assertTrue(d.shouldRenderSelection(inA),
                    "A's component must still be tested against A's own press, not B's");
        } finally {
            disposeAll();
            d.setPureTouch(false);
        }
    }
}
