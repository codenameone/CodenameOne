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
package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Window;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The `com.codename1.components` overlays that resolve a surface for themselves.
///
/// These live outside `com.codename1.ui`, so they cannot reach the package private
/// helpers and go through the public top level contract instead. That is worth
/// exercising separately from the components that can.
class OverlayHostInWindowTest extends UITestBase {

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

    // ---- FloatingActionButton --------------------------------------------------

    /// Records that the submenu got as far as being shown, without showing it.
    ///
    /// The submenu blocks on a popup dialog, so intercepting the show is what makes
    /// this deterministic. The assertion is the one that matters: the bail-out that
    /// used to return before this point is gone.
    private static final class ProbeFab extends FloatingActionButton {
        private int submenusShown;

        ProbeFab() {
            super(FontImage.MATERIAL_ADD, null, "FloatingActionButton", 3.5f);
        }

        @Override
        protected void showPopupDialog(Dialog dialog) {
            submenusShown++;
        }
    }

    @FormTest
    void aFloatingActionButtonSubmenuInAWindowOpensInsteadOfDoingNothing() {
        // It used to bail out on purpose, with a comment saying it would stay that way
        // until Dialog was window aware: releasing one in a window opened nothing.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(600, 500);
        ProbeFab fab = new ProbeFab();
        fab.createSubFAB(FontImage.MATERIAL_PEOPLE, "People");
        w.add(BorderLayout.CENTER, fab);
        w.show();
        DisplayTest.flushEdt();

        fab.pressed();
        fab.released();
        DisplayTest.flushEdt();

        assertEquals(1, fab.submenusShown,
                "a floating action button with a submenu has to open it inside a window");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aFloatingActionButtonSubmenuStillWorksOnAForm() {
        Form main = new Form("main", new BorderLayout());
        ProbeFab fab = new ProbeFab();
        fab.createSubFAB(FontImage.MATERIAL_PEOPLE, "People");
        main.add(BorderLayout.CENTER, fab);
        main.show();
        DisplayTest.flushEdt();

        fab.pressed();
        fab.released();
        DisplayTest.flushEdt();
        assertEquals(1, fab.submenusShown, "and the single surface path is unchanged");
    }

    @FormTest
    void aPlainFloatingActionButtonStillWorksOnAForm() {
        Form main = new Form("main", new BorderLayout());
        FloatingActionButton fab = FloatingActionButton.createFAB(FontImage.MATERIAL_ADD);
        final boolean[] fired = new boolean[1];
        fab.addActionListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                fired[0] = true;
            }
        });
        main.add(BorderLayout.CENTER, fab);
        main.show();
        DisplayTest.flushEdt();

        fab.pressed();
        fab.released();
        DisplayTest.flushEdt();
        assertTrue(fired[0], "a floating action button with no submenu is untouched");
    }

    // ---- InfiniteProgress ------------------------------------------------------

    @FormTest
    void infiniteProgressBlocksTheSurfaceTheUserIsIn() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();
        com.codename1.ui.Desktop.getInstance().windowFocusChanged(w.getWindowId(), true);
        DisplayTest.flushEdt();

        Dialog d = new InfiniteProgress().showInfiniteBlocking();
        DisplayTest.flushEdt();
        try {
            assertNotNull(d);
            assertTrue(isUnder(w, d),
                    "the spinner dims the window the user is waiting in, not the main form");
            assertSame(main, Display.getInstance().getCurrent(),
                    "and the main surface is not taken over to do it");
        } finally {
            d.dispose();
            DisplayTest.flushEdt();
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    @FormTest
    void infiniteProgressOnAFormStillTakesOverTheMainSurface() {
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Dialog d = new InfiniteProgress().showInfiniteBlocking();
        DisplayTest.flushEdt();
        try {
            assertNull(d.getParent(),
                    "with one surface the spinner is still a dialog that takes it over");
            assertSame(d, Display.getInstance().getCurrent());
        } finally {
            d.dispose();
            DisplayTest.flushEdt();
        }
    }

    @FormTest
    void aSpinnerInAWindowKnowsItsSurfaceIsOnScreen() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(400, 300);
        InfiniteProgress ip = new InfiniteProgress();
        w.add(BorderLayout.CENTER, ip);
        w.show();
        DisplayTest.flushEdt();

        // Comparing against Display.getCurrent() -- which only ever names a Form --
        // reported false for every spinner in a window and stopped it animating.
        assertTrue(w.isTopLevelShowing());
        assertSame(w, ip.getTopLevelContainer());

        w.dispose();
        DisplayTest.flushEdt();
        assertFalse(w.isTopLevelShowing());
    }

    // ---- ToastBar --------------------------------------------------------------

    @FormTest
    void theStaticToastHelpersTargetTheSurfaceTheUserIsIn() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();
        com.codename1.ui.Desktop.getInstance().windowFocusChanged(w.getWindowId(), true);
        DisplayTest.flushEdt();

        assertSame(ToastBar.getInstance(w), ToastBar.getInstance(com.codename1.ui.CN.getCurrentTopLevel()),
                "the static helpers resolve the same instance the window itself gets");
        assertNotSame(ToastBar.getInstance(), ToastBar.getInstance(w));

        w.dispose();
        DisplayTest.flushEdt();
    }
}
