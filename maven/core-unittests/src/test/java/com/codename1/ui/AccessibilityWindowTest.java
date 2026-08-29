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
import com.codename1.ui.accessibility.AccessibilityInspector;
import com.codename1.ui.accessibility.AccessibilityManager;
import com.codename1.ui.accessibility.AccessibilityNodeSnapshot;
import com.codename1.ui.accessibility.AccessibilityTreeSnapshot;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The semantic tree of a secondary window.
class AccessibilityWindowTest extends UITestBase {

    private static boolean mentions(AccessibilityTreeSnapshot snap, String label) {
        for (Long id : snap.getRootIds()) {
            if (walk(snap, id, label)) {
                return true;
            }
        }
        return false;
    }

    private static boolean walk(AccessibilityTreeSnapshot snap, Long id, String label) {
        AccessibilityNodeSnapshot n = snap.getNode(id);
        if (n == null) {
            return false;
        }
        if (label.equals(n.getLabel())) {
            return true;
        }
        for (Long child : n.getChildIds()) {
            if (walk(snap, child, label)) {
                return true;
            }
        }
        return false;
    }

    @FormTest
    void aWindowHasItsOwnTreeSeparateFromTheMainForm() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.add(BorderLayout.CENTER, new Label("on the form"));
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.add(BorderLayout.CENTER, new Label("in the window"));
        w.show();
        DisplayTest.flushEdt();

        AccessibilityManager.getInstance().invalidateAll();
        AccessibilityTreeSnapshot windowTree = AccessibilityInspector.snapshot(w);
        assertNotNull(windowTree);
        assertTrue(mentions(windowTree, "in the window"),
                "a window's tree has to describe the window");
        assertFalse(mentions(windowTree, "on the form"),
                "and not the main form behind it");

        AccessibilityManager.getInstance().invalidateAll();
        AccessibilityTreeSnapshot formTree = AccessibilityInspector.snapshot(main);
        assertTrue(mentions(formTree, "on the form"));
        assertFalse(mentions(formTree, "in the window"),
                "asking for one surface must not have poisoned the other");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void disposingAWindowReleasesItsCachedTree() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(400, 300);
        w.add(BorderLayout.CENTER, new Label("transient"));
        w.show();
        DisplayTest.flushEdt();

        AccessibilityManager.getInstance().invalidateAll();
        assertNotNull(AccessibilityInspector.snapshot(w));

        w.dispose();
        DisplayTest.flushEdt();

        // Nothing to assert about the contents; the point is that disposing does not
        // leave the whole disposed hierarchy reachable through the cache, and does not
        // throw on the way out.
        assertNotNull(AccessibilityInspector.currentSnapshot());
    }

    @FormTest
    void anOffEdtReaderGetsTheTreeOfTheSurfaceItAsksAbout() throws Exception {
        // Swing's accessibility callbacks run on the AWT thread, not the Codename One
        // EDT, and every window bridge is notified whenever any one root is rebuilt.
        // Returning the last tree built would have a reader on one window announce
        // another window's contents.
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.add(BorderLayout.CENTER, new Button("on the main form"));
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("second", new BorderLayout());
        w.setWindowSize(400, 300);
        w.add(BorderLayout.CENTER, new Button("in the window"));
        w.show();
        DisplayTest.flushEdt();

        AccessibilityManager mgr = AccessibilityManager.getInstance();
        // Both built on the EDT, the window last, so the global snapshot is its tree.
        assertTrue(mentions(mgr.getSnapshot(main), "on the main form"));
        assertTrue(mentions(mgr.getSnapshot(w), "in the window"));

        final AccessibilityTreeSnapshot[] offEdt = new AccessibilityTreeSnapshot[2];
        Thread t = new Thread(new Runnable() {
            public void run() {
                offEdt[0] = AccessibilityManager.getInstance().getSnapshot(main);
                offEdt[1] = AccessibilityManager.getInstance().getSnapshot(w);
            }
        });
        t.start();
        t.join(5000);

        assertNotNull(offEdt[0]);
        assertTrue(mentions(offEdt[0], "on the main form"),
                "a reader on the main form must get the main form's tree");
        assertFalse(mentions(offEdt[0], "in the window"),
                "and must not be told about the window that was built more recently");
        assertTrue(mentions(offEdt[1], "in the window"));

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void achangeOnOneSurfaceLeavesTheOtherSurfacesReadable() throws Exception {
        // An eager refresh rebuilds exactly one root and then notifies every window
        // bridge. Dropping the cached tree of every other surface left a screen reader
        // on those windows with nothing at all until its own window happened to change.
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        Button mainButton = new Button("on the main form");
        main.add(BorderLayout.CENTER, mainButton);
        main.show();
        DisplayTest.flushEdt();

        final Window w = new Window("second", new BorderLayout());
        w.setWindowSize(400, 300);
        w.add(BorderLayout.CENTER, new Button("in the window"));
        w.show();
        DisplayTest.flushEdt();

        AccessibilityManager mgr = AccessibilityManager.getInstance();
        assertTrue(mentions(mgr.getSnapshot(main), "on the main form"));
        assertTrue(mentions(mgr.getSnapshot(w), "in the window"));

        // Something on the main form changes; the window did not. The eager refresh
        // then rebuilds that one root, which is what getSnapshot(main) does here --
        // leaving the main form as the most recently built surface.
        mainButton.setText("renamed on the main form");
        DisplayTest.flushEdt();
        assertTrue(mentions(mgr.getSnapshot(main), "renamed on the main form"));

        final AccessibilityTreeSnapshot[] offEdt = new AccessibilityTreeSnapshot[1];
        Thread t = new Thread(new Runnable() {
            public void run() {
                offEdt[0] = AccessibilityManager.getInstance().getSnapshot(w);
            }
        });
        t.start();
        t.join(5000);

        assertNotNull(offEdt[0]);
        assertTrue(mentions(offEdt[0], "in the window"),
                "the untouched window must still have a tree to hand a screen reader");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aSecondRootInvalidatedBeforeTheRefreshRunsIsRebuiltToo() {
        // The refreshScheduled flag makes a second invalidation share the first one's
        // queued callback. That callback used to close over a single root, and
        // rebuilding it cleared the global dirty flag -- so the second root's tree
        // stayed stale for good, which is what an off-EDT screen reader on it reads.
        implementation.setMultiWindowSupported(true);
        implementation.setAccessibilityTreeSupported(true);
        try {
            Form main = new Form("main", new BorderLayout());
            Button mainButton = new Button("on the main form");
            main.add(BorderLayout.CENTER, mainButton);
            main.show();
            DisplayTest.flushEdt();

            final Window w = new Window("second", new BorderLayout());
            w.setWindowSize(400, 300);
            Button windowButton = new Button("in the window");
            w.add(BorderLayout.CENTER, windowButton);
            w.show();
            DisplayTest.flushEdt();

            AccessibilityManager mgr = AccessibilityManager.getInstance();
            assertTrue(mentions(mgr.getSnapshot(main), "on the main form"));
            assertTrue(mentions(mgr.getSnapshot(w), "in the window"));

            // Both change before the queued refresh gets to run.
            mainButton.setText("renamed on the main form");
            windowButton.setText("renamed in the window");
            DisplayTest.flushEdt();

            final AccessibilityTreeSnapshot[] offEdt = new AccessibilityTreeSnapshot[1];
            Thread t = new Thread(new Runnable() {
                public void run() {
                    offEdt[0] = AccessibilityManager.getInstance().getSnapshot(w);
                }
            });
            t.start();
            t.join(5000);

            assertNotNull(offEdt[0]);
            assertTrue(mentions(offEdt[0], "renamed in the window"),
                    "the second root to change has to be rebuilt as well");

            w.dispose();
            DisplayTest.flushEdt();
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(err);
        } finally {
            implementation.setAccessibilityTreeSupported(false);
        }
    }
}
