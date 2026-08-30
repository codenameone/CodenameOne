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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tooltips on a component inside a window.
///
/// `TooltipManager` scheduled against `getComponentForm()`, which is null in a window,
/// so a tooltip on anything in one was scheduled against nothing and never appeared.
class TooltipInWindowTest extends UITestBase {

    /// The manager installed by the test in progress, so it can be taken back out.
    private ProbeTooltipManager installed;

    /// Installs a manager and remembers it.
    ///
    /// `TooltipManager.enableTooltips` writes a static with no counterpart to remove
    /// it, so a manager installed here stays installed for the rest of the JVM: every
    /// later test's forms get tooltip hooks, and a pending tooltip timer or an
    /// InteractionDialog can outlive the test that created it. That is how an
    /// unrelated class several hundred tests later fails with a dispatch timeout.
    private ProbeTooltipManager install() {
        installed = new ProbeTooltipManager();
        TooltipManager.enableTooltips(installed);
        return installed;
    }

    @org.junit.jupiter.api.AfterEach
    void uninstallTooltips() throws Exception {
        if (installed != null) {
            installed.clearTooltip();
            installed = null;
        }
        DisplayTest.flushEdt();
        java.lang.reflect.Field f = TooltipManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    /// Counts what the manager decided.
    private static final class ProbeTooltipManager extends TooltipManager {
        private int shown;
        private int prepared;

        @Override
        protected void prepareTooltip(String tip, Component cmp) {
            prepared++;
            super.prepareTooltip(tip, cmp);
        }

        @Override
        protected void showTooltip(String tip, Component cmp) {
            shown++;
            super.showTooltip(tip, cmp);
        }
    }

    /// Hovers the pointer over the centre of a component, the way the port does.
    private static void hover(Window w, Component cmp) {
        int[] x = new int[] {cmp.getAbsoluteX() + cmp.getWidth() / 2};
        int[] y = new int[] {cmp.getAbsoluteY() + cmp.getHeight() / 2};
        w.pointerHover(x, y);
    }

    @FormTest
    void aTooltipOnAComponentInAWindowIsShownThere() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        Button b = new Button("hover me");
        b.setTooltip("a tip");
        w.add(BorderLayout.CENTER, b);
        w.show();
        DisplayTest.flushEdt();

        ProbeTooltipManager mgr = install();
        try {
            // Through the window's own hover dispatch, not by calling the manager.
            // Window.pointerHover is the only hover path a window has, and it used to
            // drop tooltips on the floor -- so a test that called showTooltip directly
            // passed while no real hover could ever produce a tooltip.
            hover(w, b);
            DisplayTest.flushEdt();
            assertTrue(mgr.prepared > 0,
                    "hovering a component in a window has to start the tooltip timer");

            mgr.showTooltip("a tip", b);
            DisplayTest.flushEdt();
            assertTrue(mgr.shown > 0);
            assertNotNull(w.getLayeredPaneIfExists(),
                    "the tooltip has to land on the window the component is in");
            assertNull(main.getLayeredPaneIfExists(),
                    "and not on the main form behind it");
        } finally {
            mgr.clearTooltip();
            DisplayTest.flushEdt();
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    @FormTest
    void aTooltipForASurfaceTheUserCannotSeeIsDropped() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setCloseOperation(Window.HIDE_ON_CLOSE);
        Button b = new Button("hover me");
        b.setTooltip("a tip");
        w.add(BorderLayout.CENTER, b);
        w.show();
        DisplayTest.flushEdt();
        w.hide();
        DisplayTest.flushEdt();

        ProbeTooltipManager mgr = install();
        mgr.showTooltip("a tip", b);
        DisplayTest.flushEdt();

        assertNull(w.getLayeredPaneIfExists(),
                "a tooltip for a hidden surface is dropped rather than built");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aTooltipOnADetachedComponentIsDroppedRatherThanThrowing() {
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();
        ProbeTooltipManager mgr = install();

        Button orphan = new Button("detached");
        orphan.setTooltip("a tip");
        mgr.showTooltip("a tip", orphan);
        DisplayTest.flushEdt();

        assertNull(main.getLayeredPaneIfExists(),
                "nothing to attach to, so nothing is built and nothing throws");
    }

    @FormTest
    void hoveringOffATooltippedComponentClearsTheTooltip() {
        // The other half of the hover contract: moving onto something with no tooltip
        // has to take the pending one down, or it would fire over the wrong component.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        Button tipped = new Button("hover me");
        tipped.setTooltip("a tip");
        Button bare = new Button("nothing here");
        w.add(BorderLayout.NORTH, tipped);
        w.add(BorderLayout.SOUTH, bare);
        w.show();
        DisplayTest.flushEdt();

        ProbeTooltipManager mgr = install();
        try {
            hover(w, tipped);
            DisplayTest.flushEdt();
            assertTrue(mgr.prepared > 0);

            int preparedSoFar = mgr.prepared;
            hover(w, bare);
            DisplayTest.flushEdt();
            assertEquals(preparedSoFar, mgr.prepared,
                    "a component with no tooltip must not schedule one");
        } finally {
            mgr.clearTooltip();
            DisplayTest.flushEdt();
            w.dispose();
            DisplayTest.flushEdt();
        }
    }
    @FormTest
    void aTooltipPendingOnAWindowIsCancelledWhenThePointerLeaves() {
        // What the port does when the pointer leaves a canvas: there is no further
        // hover to clear the armed timer, so the exit has to. The timer is scheduled
        // against the window rather than a form, so this asserts the cancellation
        // actually reaches a window-hosted timer -- if it did not, the tooltip would
        // still open over a surface the pointer had left.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        Button b = new Button("hover me");
        b.setTooltip("a tip");
        w.add(BorderLayout.CENTER, b);
        w.show();
        DisplayTest.flushEdt();

        ProbeTooltipManager mgr = install();
        try {
            mgr.setTooltipShowDelay(1);
            hover(w, b);
            DisplayTest.flushEdt();
            assertTrue(mgr.prepared > 0, "precondition: the timer is armed");

            // What the port now does on the way out of the canvas.
            TooltipManager.hideTooltip();
            DisplayTest.flushEdt();

            // Pumped well past the show delay: without the cancel this opens a tooltip.
            for (int i = 0; i < 300 && mgr.shown == 0; i++) {
                w.repaintAnimations();
                DisplayTest.flushEdt();
                try {
                    Thread.sleep(2);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                }
            }
            assertEquals(0, mgr.shown,
                    "a tooltip must not open once the pointer has left the surface");
        } finally {
            mgr.clearTooltip();
            DisplayTest.flushEdt();
            w.dispose();
            DisplayTest.flushEdt();
        }
    }
}
