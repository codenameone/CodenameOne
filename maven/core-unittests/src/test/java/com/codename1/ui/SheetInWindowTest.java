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
import com.codename1.ui.plaf.UIManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void aSheetOnAWindowIsNotTrackedForTheMainSurfaceHitTest() {
        // The bounds list exists for one reader: the iOS native peer hit test, which
        // resolves against Display.getCurrent() and has no notion of any other top
        // level. A window-local rectangle offered to it is compared against
        // main-surface coordinates it has nothing to do with, so a peer there stops
        // receiving input because an unrelated window happens to hold a sheet over the
        // same numbers.
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();

        Sheet onWindow = new Sheet(null, "options");
        onWindow.setTopLevelHost(w);
        onWindow.show(0);
        settle(w);
        assertTrue(onWindow.getWidth() > 0 && onWindow.getHeight() > 0,
                "the sheet is laid out, so it has bounds that could be offered");
        int x = onWindow.getAbsoluteX() + onWindow.getWidth() / 2;
        int y = onWindow.getAbsoluteY() + onWindow.getHeight() / 2;
        assertFalse(Sheet.isSheetVisibleAt(x, y),
                "a sheet in a secondary window must not answer for the main surface");

        onWindow.back(0);
        settle(w);
        w.dispose();
        DisplayTest.flushEdt();

        // The same query still works for the surface it was written for, so this is a
        // scoping fix and not a switch that turns the feature off.
        Sheet onForm = new Sheet(null, "options");
        onForm.show(0);
        settle(main);
        int fx = onForm.getAbsoluteX() + onForm.getWidth() / 2;
        int fy = onForm.getAbsoluteY() + onForm.getHeight() / 2;
        assertTrue(Sheet.isSheetVisibleAt(fx, fy),
                "a sheet on the main form still covers the peers underneath it");
        onForm.back(0);
        settle(main);
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

    @FormTest
    void nestedSheetsShowAndUnwindOnAForm() {
        // What SheetScreenshotTest does on a device, and what none of the tests above
        // covered: a sheet, then a child sheet on top of it, then back to the parent.
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Sheet parent = new Sheet(null, "Options");
        parent.getContentPane().add(new Label("parent content"));
        parent.show(0);
        settle(main);
        assertSame(parent, Sheet.getCurrentSheet(main));

        Sheet child = new Sheet(parent, "Details");
        child.getContentPane().add(new Label("child content"));
        child.show(0);
        settle(main);
        assertSame(child, Sheet.getCurrentSheet(main),
                "the child is the current sheet while it is up");
        assertSame(parent, child.getParentSheet());

        child.back(0);
        settle(main);
        assertSame(parent, Sheet.getCurrentSheet(main),
                "going back from a child sheet re-shows its parent");

        parent.back(0);
        settle(main);
        assertNull(Sheet.getCurrentSheet(main), "and back from the parent closes it");
    }

    @FormTest
    void nestedSheetsShowAndUnwindOnAWindow() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(600, 500);
        w.show();
        DisplayTest.flushEdt();

        Sheet parent = new Sheet(null, "Options");
        parent.getContentPane().add(new Label("parent content"));
        parent.setTopLevelHost(w);
        parent.show(0);
        settle(w);

        Sheet child = new Sheet(parent, "Details");
        child.getContentPane().add(new Label("child content"));
        child.setTopLevelHost(w);
        child.show(0);
        settle(w);
        assertSame(child, Sheet.getCurrentSheet(w));

        child.back(0);
        settle(w);
        assertSame(parent, Sheet.getCurrentSheet(w));

        parent.back(0);
        settle(w);
        assertNull(Sheet.getCurrentSheet(w));

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aSheetOnAFormWorksOnAPortThatHasAWindowSystemButNoOpenWindow() {
        // The state every desktop port is in for most of its life, and the one the
        // unit tests above never reproduce: a window manager exists, so the host
        // resolution consults Desktop, but nothing is open and the answer has to fall
        // through to the current form.
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();
        assertNull(Desktop.getInstance().getFocusedWindow(),
                "precondition: a window system with nothing open");
        assertSame(main, CN.getCurrentTopLevel());

        Sheet parent = new Sheet(null, "Options");
        parent.getContentPane().add(new Label("parent content"));
        parent.show(0);
        settle(main);
        assertSame(parent, Sheet.getCurrentSheet(main));

        Sheet child = new Sheet(parent, "Details");
        child.getContentPane().add(new Label("child content"));
        child.show(0);
        settle(main);
        assertSame(child, Sheet.getCurrentSheet(main));

        child.back(0);
        settle(main);
        assertSame(parent, Sheet.getCurrentSheet(main));

        parent.back(0);
        settle(main);
        assertNull(Sheet.getCurrentSheet(main));
    }

    @FormTest
    void restoringAParentSheetDoesNotPinItToThatWindowForGood() {
        // back() puts the parent back where the child was, but for that showing only:
        // writing it as the parent's explicit host would overwrite one the application
        // chose and keep the sheet on a window that may be long gone.
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(600, 500);
        w.show();
        DisplayTest.flushEdt();

        Sheet parent = new Sheet(null, "Options");
        parent.getContentPane().add(new Label("parent"));
        parent.setTopLevelHost(w);
        parent.show(0);
        settle(w);

        Sheet child = new Sheet(parent, "Details");
        child.getContentPane().add(new Label("child"));
        child.setTopLevelHost(w);
        child.show(0);
        settle(w);

        child.back(0);
        settle(w);
        assertSame(parent, Sheet.getCurrentSheet(w), "the parent comes back on the window");
        assertSame(w, parent.getTopLevelHost(),
                "and the host the application set is still the one it configured");

        parent.back(0);
        settle(w);
        w.dispose();
        DisplayTest.flushEdt();

        // With the window gone the parent must be free to show somewhere else.
        parent.setTopLevelHost(null);
        parent.show(0);
        settle(main);
        assertSame(parent, Sheet.getCurrentSheet(main),
                "a sheet reused after its window closed shows on the current surface");
        parent.back(0);
        settle(main);
    }

    @FormTest
    void aShowDeferredBehindAnAnimationStaysOnTheSurfaceItResolved() {
        // A show that lands while the host is animating queues itself and runs again
        // later. The retry used to resolve a surface of its own, so focus moving to
        // another window while the animation drained attached the sheet to that one
        // even though the show had already settled on the first.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window a = new Window("a", new BorderLayout());
        a.setWindowSize(500, 400);
        a.show();
        DisplayTest.flushEdt();
        Window b = new Window("b", new BorderLayout());
        b.setWindowSize(500, 400);
        b.show();
        DisplayTest.flushEdt();

        Desktop.getInstance().windowFocusChanged(a.getWindowId(), true);
        DisplayTest.flushEdt();

        // The host has to be mid-animation when show() is called, or the deferred
        // branch is never reached and this test would pass whatever the retry did.
        a.add(BorderLayout.NORTH, new Label("something to animate"));
        a.animateLayout(1);

        // No explicit host: an ordinary show resolves the focused window, which is
        // exactly the case that can change under it while the show waits.
        Sheet sheet = new Sheet(null, "deferred");
        sheet.show(0);
        assertNull(sheet.getParent(),
                "the show has to have been deferred for this test to mean anything");

        // Focus moves to the other window while the show is still queued.
        Desktop.getInstance().windowFocusChanged(b.getWindowId(), true);
        // Ticked by hand: core-unittests has no rasterizer, so nothing ever drives an
        // animation to completion on its own and the queued show would never run.
        for (int iter = 0; iter < 200 && sheet.getParent() == null; iter++) {
            a.getAnimationManager().updateAnimations();
            DisplayTest.flushEdt();
        }

        assertNotNull(sheet.getParent(), "the sheet is attached");
        assertTrue(isUnder(a, sheet), "and it is on the surface the show resolved");
        assertFalse(isUnder(b, sheet), "not on whichever window took focus later");

        sheet.back(0);
        DisplayTest.flushEdt();
        a.dispose();
        b.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aSheetInAWindowGetsNoInsetForChromeTheWindowDoesNotHave() {
        // StatusBar and TitleArea padding describe a phone's chrome. A desktop window
        // has neither -- its title bar is outside the drawable -- so reading those
        // styles pushed every window sheet down by an inset with nothing behind it.
        implementation.setMultiWindowSupported(true);
        UIManager uim = UIManager.getInstance();
        uim.getComponentStyle("TitleArea").setPadding(Component.TOP, 20);
        uim.getComponentStyle("StatusBar").setPadding(Component.TOP, 15);
        try {
            new Form("main", new BorderLayout()).show();
            DisplayTest.flushEdt();

            Window w = new Window("host", new BorderLayout());
            w.setWindowSize(500, 400);
            w.show();
            DisplayTest.flushEdt();

            Sheet south = new Sheet(null, "south");
            south.setPosition(BorderLayout.SOUTH);
            south.setTopLevelHost(w);
            south.show(0);
            DisplayTest.flushEdt();
            assertEquals(0, south.getStyle().getMarginTop(),
                    "a south sheet in a window must not be pushed down by phone chrome");
            south.back(0);
            DisplayTest.flushEdt();

            Sheet east = new Sheet(null, "east");
            east.setPosition(BorderLayout.EAST);
            east.setTopLevelHost(w);
            east.show(0);
            DisplayTest.flushEdt();
            assertEquals(0, east.getStyle().getPaddingTop(),
                    "and neither must one that bleeds to the top edge");
            east.back(0);
            DisplayTest.flushEdt();

            w.dispose();
            DisplayTest.flushEdt();
        } finally {
            uim.getComponentStyle("TitleArea").setPadding(Component.TOP, 0);
            uim.getComponentStyle("StatusBar").setPadding(Component.TOP, 0);
        }
    }
    @FormTest
    void aSheetDeferredOnAWindowThatGoesAwayCanStillBeShownElsewhere() {
        // A show that arrives mid-animation is pinned to the surface it resolved, and
        // the retry that clears the pin is driven by that surface's own animation
        // manager. A host hidden before the animation drained therefore never ran it,
        // and the pin outlived the show it belonged to -- every later show went at a
        // surface that is not there any more, even after the caller named another.
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(400, 300);
        w.add(BorderLayout.CENTER, new Button("filler"));
        w.show();
        DisplayTest.flushEdt();

        Sheet sheet = new Sheet(null, "deferred");
        sheet.getContentPane().add(new Label("body"));
        try {
            // An animation in flight on the window, so the show defers and pins.
            w.getContentPane().animateLayout(600);
            sheet.setTopLevelHost(w);
            sheet.show();
            DisplayTest.flushEdt();

            // The window goes before the animation drains, so the retry never runs.
            w.hide();
            DisplayTest.flushEdt();

            // Named a live surface and shown again.
            sheet.setTopLevelHost(main);
            sheet.show();
            for (int i = 0; i < 40; i++) {
                DisplayTest.flushEdt();
            }

            assertNotNull(main.getFormLayeredPaneIfExists(),
                    "the sheet has to attach to the surface it was just given");
            assertSame(main, sheet.getTopLevelContainer(),
                    "and not to the one its abandoned show had pinned");
        } finally {
            sheet.setTopLevelHost(null);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }
}
