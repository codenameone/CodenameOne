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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A `Sheet` shown on a `Window`, and the two latent bugs the migration fixed.
class SheetInWindowTest extends UITestBase {

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

    /// Drives the host's animations to completion.
    ///
    /// A sheet shows and hides through `Container#animateUnlayout`, and the runnable
    /// that actually takes it back out of the layer is that animation's completion
    /// callback. Flushing serial calls alone never runs it, so the sheet would look
    /// like it had failed to tear down when nothing had driven the animation at all.
    private static void settle(TopLevelContainer top) {
        AnimationManager am = top.getAnimationManager();
        for (int i = 0; i < 200 && am.isAnimating(); i++) {
            am.updateAnimations();
            DisplayTest.flushEdt();
        }
        am.updateAnimations();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aSheetShownOnAWindowAttachesToThatWindow() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();

        Sheet sheet = new Sheet(null, "options");
        sheet.setTopLevelHost(w);
        sheet.show(0);
        settle(w);

        assertTrue(isUnder(w, sheet), "the sheet belongs to the window it was shown on");
        assertNull(main.getFormLayeredPaneIfExists(),
                "and the main form must not have grown a layer for it");
        assertSame(sheet, Sheet.getCurrentSheet(w));
        assertNull(Sheet.getCurrentSheet(main), "the main form has no sheet on it");

        sheet.back(0);
        settle(w);
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void hidingASheetInAWindowActuallyTearsItDown() {
        // The first latent bug. The teardown gated on getComponentForm(), which is null
        // in a window, so the whole block was skipped: the sheet stayed on screen and
        // its close event never fired.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();

        Sheet sheet = new Sheet(null, "options");
        sheet.setTopLevelHost(w);
        sheet.show(0);
        settle(w);
        assertNotNull(sheet.getParent(), "precondition: the sheet is up");

        sheet.back(0);
        settle(w);
        assertNull(Sheet.getCurrentSheet(w),
                "hiding a sheet in a window has to take it back out again");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aSheetIsHiddenFromTheSurfaceItWasShownOn() {
        // The second latent bug. show() and hide() resolved the current form
        // independently, so navigating between them tore down the wrong layered pane
        // and left the sheet behind on the surface it was actually on.
        Form first = new Form("first", new BorderLayout());
        first.show();
        DisplayTest.flushEdt();

        Sheet sheet = new Sheet(null, "options");
        sheet.show(0);
        settle(first);
        assertSame(sheet, Sheet.getCurrentSheet(first));

        Form second = new Form("second", new BorderLayout());
        second.show();
        DisplayTest.flushEdt();

        sheet.back(0);
        settle(first);
        assertNull(Sheet.getCurrentSheet(first),
                "the sheet has to leave the form it was shown on, not the current one");
        assertFalse(isUnder(second, sheet),
                "and it must never have been moved onto the form navigated to");
    }

    @FormTest
    void aSheetWithNoSurfaceSaysSoRatherThanThrowingNull() {
        // It used to dereference the null current form. Both are unchecked, but one of
        // them says what went wrong.
        implementation.setMultiWindowSupported(true);
        Window w = new Window("never shown", new BorderLayout());
        final Sheet sheet = new Sheet(null, "orphan");
        // A host that exists but was never shown has no layered pane geometry; the
        // interesting case is no host at all, which the explicit host makes reachable.
        sheet.setTopLevelHost(null);
        assertNull(sheet.getTopLevelHost());
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void anExplicitHostBeatsTheFocusedWindow() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window a = new Window("a", new BorderLayout());
        a.setWindowSize(500, 400);
        a.show();
        Window b = new Window("b", new BorderLayout());
        b.setWindowSize(500, 400);
        b.show();
        DisplayTest.flushEdt();
        Desktop.getInstance().windowFocusChanged(b.getWindowId(), true);
        DisplayTest.flushEdt();

        Sheet sheet = new Sheet(null, "options");
        sheet.setTopLevelHost(a);
        sheet.show(0);
        settle(a);

        assertSame(sheet, Sheet.getCurrentSheet(a), "the named host wins over the focused one");
        assertNull(Sheet.getCurrentSheet(b));

        sheet.back(0);
        settle(a);
        a.dispose();
        b.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void getCurrentSheetWithNoArgumentFollowsTheSurfaceTheUserIsIn() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();
        Desktop.getInstance().windowFocusChanged(w.getWindowId(), true);
        DisplayTest.flushEdt();

        Sheet sheet = new Sheet(null, "options");
        sheet.setTopLevelHost(w);
        sheet.show(0);
        settle(w);

        assertSame(sheet, Sheet.getCurrentSheet(),
                "the no-argument form asks the surface the user is actually in");

        sheet.back(0);
        settle(w);
        w.dispose();
        DisplayTest.flushEdt();
    }
}
