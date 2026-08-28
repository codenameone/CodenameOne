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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tooltips on a component inside a window.
///
/// `TooltipManager` scheduled against `getComponentForm()`, which is null in a window,
/// so a tooltip on anything in one was scheduled against nothing and never appeared.
class TooltipInWindowTest extends UITestBase {

    /// Counts what the manager decided, without needing a real hover.
    private static final class ProbeTooltipManager extends TooltipManager {
        private int shown;

        @Override
        protected void showTooltip(String tip, Component cmp) {
            shown++;
            super.showTooltip(tip, cmp);
        }
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

        ProbeTooltipManager mgr = new ProbeTooltipManager();
        TooltipManager.enableTooltips(mgr);
        try {
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

        ProbeTooltipManager mgr = new ProbeTooltipManager();
        TooltipManager.enableTooltips(mgr);
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
        ProbeTooltipManager mgr = new ProbeTooltipManager();
        TooltipManager.enableTooltips(mgr);

        Button orphan = new Button("detached");
        orphan.setTooltip("a tip");
        mgr.showTooltip("a tip", orphan);
        DisplayTest.flushEdt();

        assertNull(main.getLayeredPaneIfExists(),
                "nothing to attach to, so nothing is built and nothing throws");
    }
}
