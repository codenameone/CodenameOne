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
import com.codename1.ui.accessibility.AccessibilityAction;
import com.codename1.ui.accessibility.AccessibilityInspector;
import com.codename1.ui.accessibility.AccessibilityManager;
import com.codename1.ui.accessibility.AccessibilityNodeSnapshot;
import com.codename1.ui.accessibility.AccessibilityTreeSnapshot;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @FormTest
    void aLiveWindowKeepsItsTreeEvenPastTheCacheLimit() {
        // The cache is capped because a root is only released explicitly when a window
        // is disposed, so forms would accumulate. Evicting by age alone dropped live
        // windows once there were more of them than the cap, and an off-EDT reader on
        // an evicted window gets an empty tree -- a screen reader losing everything.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        AccessibilityManager mgr = AccessibilityManager.getInstance();
        final Window first = new Window("first", new BorderLayout());
        first.setWindowSize(300, 200);
        first.add(BorderLayout.CENTER, new Button("in the first window"));
        first.show();
        DisplayTest.flushEdt();
        assertTrue(mentions(mgr.getSnapshot(first), "in the first window"));

        // Comfortably more surfaces than the cache holds.
        Window[] others = new Window[12];
        for (int iter = 0; iter < others.length; iter++) {
            Window w = new Window("w" + iter, new BorderLayout());
            w.setWindowSize(300, 200);
            w.add(BorderLayout.CENTER, new Button("in window " + iter));
            w.show();
            others[iter] = w;
            DisplayTest.flushEdt();
            mgr.getSnapshot(w);
        }

        final AccessibilityTreeSnapshot[] offEdt = new AccessibilityTreeSnapshot[1];
        Thread t = new Thread(new Runnable() {
            public void run() {
                offEdt[0] = AccessibilityManager.getInstance().getSnapshot(first);
            }
        });
        t.start();
        try {
            t.join(5000);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(err);
        }

        assertNotNull(offEdt[0]);
        assertTrue(mentions(offEdt[0], "in the first window"),
                "a window the user can still see must keep its tree");

        first.dispose();
        for (int iter = 0; iter < others.length; iter++) {
            others[iter].dispose();
        }
        DisplayTest.flushEdt();
    }

    @FormTest
    void disposingTheOnlyQueuedWindowLeavesTheOthersReadable() {
        // Releasing a root takes it out of the refresh queue, which can empty it. An
        // empty queue used to mean "a mutation with no surface", whose refresh clears
        // every cached surface -- so one window closing blanked the trees of all the
        // others for any off-EDT reader.
        implementation.setMultiWindowSupported(true);
        implementation.setAccessibilityTreeSupported(true);
        try {
            new Form("main", new BorderLayout()).show();
            DisplayTest.flushEdt();

            final Window keep = new Window("keep", new BorderLayout());
            keep.setWindowSize(400, 300);
            keep.add(BorderLayout.CENTER, new Button("in the window that stays"));
            keep.show();
            DisplayTest.flushEdt();

            Window going = new Window("going", new BorderLayout());
            going.setWindowSize(400, 300);
            Button doomed = new Button("in the window that closes");
            going.add(BorderLayout.CENTER, doomed);
            going.show();
            DisplayTest.flushEdt();

            AccessibilityManager mgr = AccessibilityManager.getInstance();
            assertTrue(mentions(mgr.getSnapshot(keep), "in the window that stays"));
            assertTrue(mentions(mgr.getSnapshot(going), "in the window that closes"));

            // The doomed window is the only thing queued, and then it goes away.
            doomed.setText("changed just before closing");
            going.dispose();
            DisplayTest.flushEdt();

            final AccessibilityTreeSnapshot[] offEdt = new AccessibilityTreeSnapshot[1];
            Thread t = new Thread(new Runnable() {
                public void run() {
                    offEdt[0] = AccessibilityManager.getInstance().getSnapshot(keep);
                }
            });
            t.start();
            t.join(5000);

            assertNotNull(offEdt[0]);
            assertTrue(mentions(offEdt[0], "in the window that stays"),
                    "a window that did not close keeps its tree");

            keep.dispose();
            DisplayTest.flushEdt();
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(err);
        } finally {
            implementation.setAccessibilityTreeSupported(false);
        }
    }

    @FormTest
    void surfaceZeroDescribesTheMainFormWhateverHasFocus() {
        // Surface zero is the main canvas by contract. Resolving it through "the
        // current surface" answered with the focused top level on the event dispatch
        // thread and with the last tree built anywhere off it, either of which hands
        // the main canvas a secondary window's labels and actions.
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
        Desktop.getInstance().windowFocusChanged(w.getWindowId(), true);
        DisplayTest.flushEdt();
        // Built last, and focused, so both readings of "current" point at the window.
        AccessibilityManager.getInstance().getSnapshot(w);

        AccessibilityTreeSnapshot zero = implementation.getAccessibilityTreeSnapshot(0);
        assertNotNull(zero);
        assertTrue(mentions(zero, "on the main form"),
                "surface zero is the main form");
        assertFalse(mentions(zero, "in the window"),
                "and never the window that happens to be focused");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void invalidatingEverythingRebuildsEverySurfaceNotJustTheFocusedOne() {
        // invalidateAll() has no root, and substituting the focused one turned it into
        // a single-root refresh -- which then cleared the global dirty flag, so every
        // other surface stayed stale.
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
            mgr.getSnapshot(main);
            mgr.getSnapshot(w);
            DisplayTest.flushEdt();

            // Mutated with eager projection off, so nothing queues these roots by
            // itself -- otherwise setText would queue the window and the test would
            // pass however invalidateAll behaved.
            implementation.setAccessibilityTreeSupported(false);
            Desktop.getInstance().windowFocusChanged(w.getWindowId(), false);
            DisplayTest.flushEdt();
            mainButton.setText("renamed on the main form");
            windowButton.setText("renamed in the window");
            DisplayTest.flushEdt();

            // Now everything is declared stale at once, with the main form focused.
            implementation.setAccessibilityTreeSupported(true);
            mgr.invalidateAll();
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
                    "an unfocused surface has to be rebuilt by an all-root invalidation");

            w.dispose();
            DisplayTest.flushEdt();
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(err);
        } finally {
            implementation.setAccessibilityTreeSupported(false);
        }
    }

    @FormTest
    void rebuildingOneSurfaceDoesNotDeclareTheOthersFresh() {
        // Staleness was one flag for the whole manager. Invalidating one root and then
        // pulling another cleared it, so the first root's cached tree came back as
        // though it were current -- which is what a pull-on-demand consumer and the
        // inspector both read.
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        Button mainButton = new Button("on the main form");
        main.add(BorderLayout.CENTER, mainButton);
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("second", new BorderLayout());
        w.setWindowSize(400, 300);
        w.add(BorderLayout.CENTER, new Button("in the window"));
        w.show();
        DisplayTest.flushEdt();

        AccessibilityManager mgr = AccessibilityManager.getInstance();
        assertTrue(mentions(mgr.getSnapshot(main), "on the main form"));
        assertTrue(mentions(mgr.getSnapshot(w), "in the window"));

        // The main form changes; the window does not. Then the window is pulled first.
        mainButton.setText("renamed on the main form");
        DisplayTest.flushEdt();
        mgr.getSnapshot(w);

        assertTrue(mentions(mgr.getSnapshot(main), "renamed on the main form"),
                "the surface that changed has to be rebuilt however the pulls are ordered");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void navigatingThroughFormsDoesNotPinThemForever() {
        // Display.setCurrent invalidates every newly shown form, and the dirty list is
        // held by a singleton. Recording a root that has no cached tree pinned every
        // form the application had ever shown, with its whole hierarchy, for the life
        // of the process -- only a disposed window ever releases a root explicitly.
        // The growth, not the absolute count: the manager is a singleton and earlier
        // tests in this class have left cached roots behind.
        AccessibilityManager mgr = AccessibilityManager.getInstance();
        int before = mgr.dirtyRootCount();
        for (int iter = 0; iter < 30; iter++) {
            Form f = new Form("form " + iter, new BorderLayout());
            f.add(BorderLayout.CENTER, new Button("button " + iter));
            f.show();
            DisplayTest.flushEdt();
        }

        assertEquals(before, mgr.dirtyRootCount(),
                "a form with no cached tree must not be recorded as stale, "
                        + "and so must not be held by the manager");
    }

    @FormTest
    void aDirtyFormEvictedFromTheCacheIsNotHeldByTheDirtySet() {
        // Dirtiness is only recorded for a root that has a cached tree, so an entry
        // left behind by eviction describes a tree that no longer exists -- and a form
        // never releases its root explicitly, so that entry held the form and its whole
        // hierarchy for good.
        AccessibilityManager mgr = AccessibilityManager.getInstance();
        int before = mgr.dirtyRootCount();

        Form doomed = new Form("doomed", new BorderLayout());
        Button b = new Button("in the doomed form");
        doomed.add(BorderLayout.CENTER, b);
        doomed.show();
        DisplayTest.flushEdt();
        // Cached, then made stale while it still has a tree.
        assertTrue(mentions(mgr.getSnapshot(doomed), "in the doomed form"));
        b.setText("renamed");
        DisplayTest.flushEdt();

        // Enough other surfaces, each cached, to push it out of the cache. It is no
        // longer the current form, so it is the one eviction is allowed to take.
        for (int iter = 0; iter < 12; iter++) {
            Form f = new Form("filler " + iter, new BorderLayout());
            f.add(BorderLayout.CENTER, new Button("filler " + iter));
            f.show();
            DisplayTest.flushEdt();
            mgr.getSnapshot(f);
        }

        // The invariant, rather than an exact count: dirtiness is only ever recorded
        // for a root that has a cached tree, so the dirty set can never be larger than
        // the cache. Eviction is the only thing that can break that, and it did --
        // the set kept one entry per form shown rather than at most one per cached
        // tree.
        assertTrue(mgr.dirtyRootCount() <= mgr.cachedRootCount(),
                "the dirty set must not outgrow the cache it describes: "
                        + mgr.dirtyRootCount() + " dirty vs " + mgr.cachedRootCount()
                        + " cached");
    }

    @FormTest
    void aDisposedWindowsSurfaceDescribesNothing() throws Exception {
        // A platform accessibility bridge outlives the window it describes and keeps
        // asking. Answering with the last tree built anywhere hands that dead surface
        // another window's labels and node ids, which is the isolation this work exists
        // to provide.
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.add(BorderLayout.CENTER, new Button("on the main form"));
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("going", new BorderLayout());
        w.setWindowSize(400, 300);
        w.add(BorderLayout.CENTER, new Button("in the window"));
        w.show();
        DisplayTest.flushEdt();
        int id = w.getWindowId();
        assertTrue(mentions(AccessibilityManager.getInstance().getSnapshot(w),
                "in the window"));

        w.dispose();
        DisplayTest.flushEdt();

        // Another surface is described after it goes, so the last tree built anywhere
        // is somebody else's. Without this the disposal itself has already emptied the
        // shared snapshot and the test would pass whatever the lookup returned.
        assertTrue(mentions(AccessibilityManager.getInstance().getSnapshot(main),
                "on the main form"));

        final int disposedId = id;
        final AccessibilityTreeSnapshot[] offEdt = new AccessibilityTreeSnapshot[1];
        Thread t = new Thread(new Runnable() {
            public void run() {
                offEdt[0] = implementation.getAccessibilityTreeSnapshot(disposedId);
            }
        });
        t.start();
        t.join(5000);

        assertNotNull(offEdt[0]);
        assertTrue(offEdt[0].getRootIds().isEmpty(),
                "a surface whose window is gone describes nothing");
        assertFalse(mentions(offEdt[0], "on the main form"),
                "and certainly not whatever window is still open");
    }

    /// A container that invalidates something else the first time the accessibility
    /// walk descends into it -- i.e. from inside a refresh pass that is already running.
    private static final class InvalidatesMidWalk extends Container {
        private Runnable once;

        @Override
        public int getComponentCount() {
            Runnable r = once;
            if (r != null) {
                once = null;
                r.run();
            }
            return super.getComponentCount();
        }
    }

    @FormTest
    void aRootInvalidatedWhileTheRefreshIsRunningIsStillRebuilt() {
        // Snapshots are built outside the queue's lock, so an invalidation can land
        // after the pass has taken and emptied the queue. It finds refreshScheduled
        // still set and so schedules nothing of its own; the pass then cleared the flag
        // without looking at what had arrived, and that root stayed stale until some
        // unrelated later invalidation happened along. An off-EDT screen reader on that
        // window reads the stale tree in the meantime.
        implementation.setMultiWindowSupported(true);
        implementation.setAccessibilityTreeSupported(true);
        try {
            Form main = new Form("main", new BorderLayout());
            final Button mainButton = new Button("on the main form");
            InvalidatesMidWalk trigger = new InvalidatesMidWalk();
            trigger.add(mainButton);
            main.add(BorderLayout.CENTER, trigger);
            main.show();
            DisplayTest.flushEdt();

            final Window w = new Window("second", new BorderLayout());
            w.setWindowSize(400, 300);
            final Button windowButton = new Button("in the window");
            w.add(BorderLayout.CENTER, windowButton);
            w.show();
            DisplayTest.flushEdt();

            AccessibilityManager mgr = AccessibilityManager.getInstance();
            assertTrue(mentions(mgr.getSnapshot(main), "on the main form"));
            assertTrue(mentions(mgr.getSnapshot(w), "in the window"));

            // Fires while the pass below is building the main form's tree.
            trigger.once = new Runnable() {
                @Override
                public void run() {
                    windowButton.setText("renamed in the window");
                }
            };
            mainButton.setText("renamed on the main form");
            DisplayTest.flushEdt();
            // A second turn of the loop, which is where the re-post lands.
            DisplayTest.flushEdt();

            assertNull(trigger.once, "precondition: the walk did descend into the trigger");

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
                    "a root queued while the refresh was running still has to be rebuilt");

            w.dispose();
            DisplayTest.flushEdt();
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(err);
        } finally {
            implementation.setAccessibilityTreeSupported(false);
        }
    }

    @FormTest
    void anActionOnAFormNavigatedAwayFromIsRefused() {
        // A tree stays cached after the main form has navigated away -- deliberately, so
        // a live secondary window keeps its own. Ids retained from the old one still
        // resolved, so a reader holding one could invoke a command on a form nobody can
        // see, which for a button means running whatever it navigates to.
        implementation.setMultiWindowSupported(true);
        implementation.setAccessibilityTreeSupported(true);
        try {
            Form first = new Form("first", new BorderLayout());
            final int[] fired = new int[1];
            Button onFirst = new Button("on the first form");
            onFirst.addActionListener(new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                    fired[0]++;
                }
            });
            first.add(BorderLayout.CENTER, onFirst);
            first.show();
            DisplayTest.flushEdt();

            AccessibilityManager mgr = AccessibilityManager.getInstance();
            AccessibilityTreeSnapshot tree = mgr.getSnapshot(first);
            long id = 0;
            String actionId = null;
            for (Long candidate : tree.getNodes().keySet()) {
                AccessibilityNodeSnapshot n = tree.getNode(candidate.longValue());
                if (n != null && n.getComponent() == onFirst //NOPMD CompareObjectsWithEquals
                        && n.getAction(AccessibilityAction.ACTIVATE) != null) {
                    id = candidate.longValue();
                    actionId = AccessibilityAction.ACTIVATE;
                    break;
                }
            }
            assertNotNull(actionId, "precondition: the button has an action to invoke");

            // It works while that form is the one on screen.
            assertTrue(mgr.performAction(id, actionId, null));
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();
            assertEquals(1, fired[0], "sanity: the action reaches a showing form");

            // Navigate away. The old tree stays cached.
            Form second = new Form("second", new BorderLayout());
            second.show();
            DisplayTest.flushEdt();

            assertFalse(mgr.performAction(id, actionId, null),
                    "an id from a form that is no longer showing must not resolve");
            DisplayTest.flushEdt();
            assertEquals(1, fired[0],
                    "and nothing may run on the form the user navigated away from");
        } finally {
            implementation.setAccessibilityTreeSupported(false);
        }
    }

    @FormTest
    void anActionOnTheTreeJustBuiltIsAcceptedEvenBeforeItsFormIsCurrent() {
        // A caller routinely acts immediately after asking for a form to be shown, so the
        // most recently built tree has to answer whether or not its root is the current
        // surface at that instant. Gating this one the same way as the cached trees below
        // it refused those actions outright -- the device suite caught it, having taken a
        // snapshot and performed an action on it without waiting.
        implementation.setAccessibilityTreeSupported(true);
        try {
            Form other = new Form("other", new BorderLayout());
            other.show();
            DisplayTest.flushEdt();

            // Built for a form that is not the one on screen.
            Form target = new Form("target", new BorderLayout());
            final int[] fired = new int[1];
            Button save = new Button("save");
            save.addActionListener(new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                    fired[0]++;
                }
            });
            target.add(BorderLayout.CENTER, save);
            AccessibilityTreeSnapshot tree = AccessibilityInspector.snapshot(target);

            long id = 0;
            for (Long candidate : tree.getNodes().keySet()) {
                AccessibilityNodeSnapshot n = tree.getNode(candidate.longValue());
                if (n != null && n.getComponent() == save //NOPMD CompareObjectsWithEquals
                        && n.getAction(AccessibilityAction.ACTIVATE) != null) {
                    id = candidate.longValue();
                    break;
                }
            }
            assertTrue(id != 0L, "precondition: the tree describes the button");

            assertTrue(AccessibilityManager.getInstance()
                            .performAction(id, AccessibilityAction.ACTIVATE, null),
                    "the tree just built has to answer for its own ids");
            DisplayTest.flushEdt();
            assertEquals(1, fired[0], "and the action has to run");
        } finally {
            implementation.setAccessibilityTreeSupported(false);
        }
    }

    @FormTest
    void aChangeBitArrivingDuringARefreshSurvivesToTheNextPass() {
        // Building a tree used to clear the shared change mask as a side effect, so a bit
        // raised while a pass was walking an earlier root was wiped before the pass it
        // queued could report it. On iOS that is the pane change VoiceOver needs to move
        // focus to a newly opened pane.
        implementation.setMultiWindowSupported(true);
        implementation.setAccessibilityTreeSupported(true);
        try {
            Form main = new Form("main", new BorderLayout());
            Button b = new Button("on the main form");
            main.add(BorderLayout.CENTER, b);
            main.show();
            DisplayTest.flushEdt();

            AccessibilityManager mgr = AccessibilityManager.getInstance();
            mgr.getSnapshot(main);
            DisplayTest.flushEdt();

            // Raise a bit and rebuild without letting the queued pass run in between,
            // which is what a rebuild during a pass does to it.
            mgr.invalidate(b, AccessibilityManager.CHANGE_PANE);
            mgr.getSnapshot(main);
            assertTrue((mgr.getPendingChanges() & AccessibilityManager.CHANGE_PANE) != 0,
                    "a bit not yet announced has to survive a rebuild");
        } finally {
            implementation.setAccessibilityTreeSupported(false);
        }
    }
    @FormTest
    void describingATreeDoesNotQueueAnotherRefreshOfIt() {
        // A component's semantic node is created the first time anything reads it, and
        // seeding its label from the accessibility text used to report itself as a
        // content change. So the walk that described a tree invalidated that same tree.
        // A finished pass simply stopped, which hid it; once the pass re-posts itself
        // whenever work is queued, it never runs out of work, and on a surface that
        // keeps making components it holds the event thread for as long as that lasts.
        implementation.setAccessibilityTreeSupported(true);
        try {
            Form main = new Form("main", new BorderLayout());
            main.add(BorderLayout.NORTH, new Button("already here"));
            main.show();
            DisplayTest.flushEdt();

            AccessibilityManager mgr = AccessibilityManager.getInstance();
            for (int i = 0; i < 4; i++) {
                DisplayTest.flushEdt();
            }
            long settled = mgr.getSnapshot(main).getGeneration();

            // A component nobody has read the semantics of yet, so the refresh pass
            // that this change schedules is the thing that creates and seeds its node.
            Button fresh = new Button("fresh");
            fresh.setAccessibilityText("fresh label");
            main.add(BorderLayout.CENTER, fresh);
            DisplayTest.flushEdt();
            for (int i = 0; i < 4; i++) {
                DisplayTest.flushEdt();
            }

            // One change, one rebuild. Two means the rebuild described the tree, the
            // description invalidated it, and the pass came round again -- which is
            // unbounded on a surface that is still making components.
            assertEquals(settled + 1, mgr.getSnapshot(main).getGeneration(),
                    "describing a tree must not queue another refresh of that tree");
        } finally {
            implementation.setAccessibilityTreeSupported(false);
        }
    }
    /// The tree an off-event-thread reader -- a screen reader bridge -- would be handed
    /// for this surface. On the event thread a cache miss rebuilds on demand, so an
    /// eviction is only observable from another thread.
    private static AccessibilityTreeSnapshot offEdtSnapshot(final TopLevelContainer root) {
        final AccessibilityTreeSnapshot[] out = new AccessibilityTreeSnapshot[1];
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                out[0] = AccessibilityManager.getInstance().getSnapshot(root);
            }
        }, "cn1-test-a11y-reader");
        t.start();
        try {
            t.join(5000);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(err);
        }
        assertNotNull(out[0], "the off-thread read has to complete");
        return out[0];
    }

    @FormTest
    void aRestoredWindowGetsItsTreeBackAfterBeingEvicted() {
        // Minimizing clears nativeVisible, so a minimized window reads as not showing
        // and the snapshot cache is free to evict it to stay under its cap. Nothing
        // would then have rebuilt it: the tree did not change while the window was
        // down, so no mutation was ever recorded, and an off-EDT reader is answered
        // with an empty tree on a cache miss -- a screen reader would find the restored
        // window empty until something unrelated happened to it.
        implementation.setMultiWindowSupported(true);
        implementation.setAccessibilityTreeSupported(true);
        final Window w = new Window("second", new BorderLayout());
        try {
            Form main = new Form("main", new BorderLayout());
            final Button mainButton = new Button("on the main form");
            main.add(BorderLayout.CENTER, mainButton);
            main.show();
            DisplayTest.flushEdt();

            w.setWindowSize(400, 300);
            w.add(BorderLayout.CENTER, new Button("in the window"));
            w.show();
            DisplayTest.flushEdt();

            AccessibilityManager mgr = AccessibilityManager.getInstance();
            assertTrue(mentions(offEdtSnapshot(w), "in the window"),
                    "precondition: the window has a tree to lose");

            // Down, and evicted from behind its back while it is down. The main form
            // is rebuilt in between so that it, not the window, is the last tree built
            // anywhere -- otherwise an off-EDT read of the window falls back to that
            // last tree and the eviction is invisible.
            w.hideNotify();
            DisplayTest.flushEdt();
            mainButton.setText("renamed on the main form");
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();
            mgr.releaseRoot(w);
            // Read off the event thread, which is the reader this is about: on the
            // event thread a cache miss simply rebuilds on demand, so nothing an
            // eviction does is visible from there at all.
            assertFalse(mentions(offEdtSnapshot(w), "in the window"),
                    "precondition: the cached tree is actually gone");

            w.showNotify();
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();

            assertTrue(mentions(offEdtSnapshot(w), "in the window"),
                    "a window the user can see again has to have a tree again");
        } finally {
            implementation.setAccessibilityTreeSupported(false);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }
    @FormTest
    void anActionOnAWindowHiddenSinceTheTreeWasBuiltDoesNotRun() {
        // A reader holds an id from the last tree built anywhere. If that surface is
        // hidden -- a window hidden without being disposed -- acting on it would press
        // a button on a window the user cannot see.
        implementation.setMultiWindowSupported(true);
        implementation.setAccessibilityTreeSupported(true);
        // A port that is pulled from rather than projected to -- the desktop and iOS
        // bridges ask for a tree when they want one. Nothing then rebuilds the window's
        // tree behind the manager's back when it is hidden, so the ids in the tree that
        // was already built are exactly what a reader is still holding.
        implementation.setAccessibilityTreeUpdateRequired(Boolean.FALSE);
        final Window w = new Window("second", new BorderLayout());
        try {
            new Form("main", new BorderLayout()).show();
            DisplayTest.flushEdt();

            w.setWindowSize(400, 300);
            final boolean[] pressed = new boolean[1];
            Button b = new Button("in the window");
            b.addActionListener(new com.codename1.ui.events.ActionListener() {
                @Override
                public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                    pressed[0] = true;
                }
            });
            w.add(BorderLayout.CENTER, b);
            w.show();
            DisplayTest.flushEdt();

            AccessibilityManager mgr = AccessibilityManager.getInstance();
            long id = idOf(mgr.getSnapshot(w), "in the window");
            assertTrue(id != 0, "precondition: the button is in the tree");
            // Still reachable while the window is up.
            assertTrue(mgr.performAction(id, "activate", null));
            DisplayTest.flushEdt();
            assertTrue(pressed[0], "precondition: the action works while it is showing");

            pressed[0] = false;
            w.hide();
            DisplayTest.flushEdt();
            assertFalse(mgr.performAction(id, "activate", null),
                    "an id from a window that has been taken off screen must not resolve");
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();
            assertFalse(pressed[0],
                    "an id retained from a hidden window must not press its buttons");
        } finally {
            implementation.setAccessibilityTreeSupported(false);
            implementation.setAccessibilityTreeUpdateRequired(null);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    /// The id of the first node carrying this label, or 0.
    private static long idOf(AccessibilityTreeSnapshot snap, String label) {
        for (Long id : snap.getRootIds()) {
            long found = idWalk(snap, id, label);
            if (found != 0) {
                return found;
            }
        }
        return 0;
    }

    private static long idWalk(AccessibilityTreeSnapshot snap, Long id, String label) {
        AccessibilityNodeSnapshot n = snap.getNode(id);
        if (n == null) {
            return 0;
        }
        if (label.equals(n.getLabel())) {
            return id.longValue();
        }
        for (Long child : n.getChildIds()) {
            long found = idWalk(snap, child, label);
            if (found != 0) {
                return found;
            }
        }
        return 0;
    }
    @FormTest
    void aChangeInsideAWindowNamesThatWindowToThePort() {
        // A port that pushes the tree into a native view has to be told which surface it
        // just described. Told only that something changed, it reads whatever was
        // rebuilt last and installs it on the main view -- so a change inside a window
        // replaced the main surface's elements with the window's, and left the window
        // itself exposing nothing.
        implementation.setMultiWindowSupported(true);
        implementation.setAccessibilityTreeSupported(true);
        final Window w = new Window("second", new BorderLayout());
        try {
            Form main = new Form("main", new BorderLayout());
            main.add(BorderLayout.CENTER, new Button("on the main form"));
            main.show();
            DisplayTest.flushEdt();

            w.setWindowSize(400, 300);
            final Button windowButton = new Button("in the window");
            w.add(BorderLayout.CENTER, windowButton);
            w.show();
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();

            implementation.clearAccessibilityNotifications();
            windowButton.setText("renamed in the window");
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();

            boolean namedTheWindow = false;
            for (int[] n : implementation.getAccessibilityNotifications()) {
                if (n[1] == w.getWindowId()) {
                    namedTheWindow = true;
                }
                assertNotEquals(0, n[1],
                        "a change inside a window must not be reported as the main surface");
            }
            assertTrue(namedTheWindow,
                    "the window that changed has to be named to the port");
        } finally {
            implementation.setAccessibilityTreeSupported(false);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }
    @FormTest
    void anActionOnTheFormNavigatedAwayFromDoesNotRun() {
        // The tree of the surface just left stays the last one built until a refresh
        // replaces it, so its ids went on resolving in that interval and a reader could
        // press a button on a form the user had already navigated away from. A form has
        // no mapped state to ask, so what separates this from a form that was never on
        // screen -- which is a live surface a caller acts on -- is whether it was the
        // surface on screen when it was described.
        implementation.setAccessibilityTreeSupported(true);
        // Pulled from rather than projected to, so nothing rebuilds behind the test's
        // back and the form left behind stays the last tree built.
        implementation.setAccessibilityTreeUpdateRequired(Boolean.FALSE);
        try {
            Form first = new Form("first", new BorderLayout());
            final boolean[] pressed = new boolean[1];
            Button b = new Button("on the first form");
            b.addActionListener(new com.codename1.ui.events.ActionListener() {
                @Override
                public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                    pressed[0] = true;
                }
            });
            first.add(BorderLayout.CENTER, b);
            first.show();
            DisplayTest.flushEdt();

            AccessibilityManager mgr = AccessibilityManager.getInstance();
            // Described while it is the surface on screen, which is the case that has to
            // stop working once the application moves on.
            long id = idOf(AccessibilityInspector.snapshot(first), "on the first form");
            assertTrue(id != 0, "precondition: the button is in the tree");
            assertTrue(mgr.performAction(id, "activate", null),
                    "precondition: it works while that form is the one on screen");
            DisplayTest.flushEdt();
            assertTrue(pressed[0], "precondition: and the action actually ran");

            pressed[0] = false;
            new Form("second", new BorderLayout()).show();
            DisplayTest.flushEdt();

            assertFalse(mgr.performAction(id, "activate", null),
                    "an id from the form navigated away from must not resolve");
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();
            assertFalse(pressed[0],
                    "and nothing may run on a form the user has already left");
        } finally {
            implementation.setAccessibilityTreeSupported(false);
            implementation.setAccessibilityTreeUpdateRequired(null);
        }
    }
    @FormTest
    void eachSurfaceIsToldOnlyItsOwnChanges() {
        // The mask goes to the port with the surface it describes, and a port acts on
        // the bits: iOS reads a pane change as "move the reader to the top of this
        // screen". Sending the union to every queued surface moved the reader on a
        // window where nothing of the sort had happened.
        implementation.setMultiWindowSupported(true);
        implementation.setAccessibilityTreeSupported(true);
        final Window w = new Window("second", new BorderLayout());
        try {
            Form main = new Form("main", new BorderLayout());
            final Button mainButton = new Button("on the main form");
            main.add(BorderLayout.CENTER, mainButton);
            main.show();
            DisplayTest.flushEdt();

            w.setWindowSize(400, 300);
            final Button windowButton = new Button("in the window");
            w.add(BorderLayout.CENTER, windowButton);
            w.show();
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();

            implementation.clearAccessibilityNotifications();
            AccessibilityManager mgr = AccessibilityManager.getInstance();
            // A pane change on the window and a plain content change on the main form,
            // both queued before the pass runs.
            mgr.invalidate(windowButton, AccessibilityManager.CHANGE_PANE);
            mgr.invalidate(mainButton, AccessibilityManager.CHANGE_CONTENT);
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();

            boolean sawMain = false;
            for (int[] n : implementation.getAccessibilityNotifications()) {
                if (n[1] == 0) {
                    sawMain = true;
                    assertEquals(0, n[0] & AccessibilityManager.CHANGE_PANE,
                            "the main form must not be told about the window's pane change");
                }
            }
            assertTrue(sawMain, "precondition: the main form was rebuilt too");
        } finally {
            implementation.setAccessibilityTreeSupported(false);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    @FormTest
    void anActionIsDroppedIfItsWindowGoesBeforeItRuns() {
        // A window can be hidden between the id being resolved and the action running,
        // and the action would then press a button on a surface that has gone.
        implementation.setMultiWindowSupported(true);
        implementation.setAccessibilityTreeSupported(true);
        implementation.setAccessibilityTreeUpdateRequired(Boolean.FALSE);
        final Window w = new Window("second", new BorderLayout());
        try {
            new Form("main", new BorderLayout()).show();
            DisplayTest.flushEdt();

            w.setWindowSize(400, 300);
            final boolean[] pressed = new boolean[1];
            Button b = new Button("in the window");
            b.addActionListener(new com.codename1.ui.events.ActionListener() {
                @Override
                public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                    pressed[0] = true;
                }
            });
            w.add(BorderLayout.CENTER, b);
            w.show();
            DisplayTest.flushEdt();

            AccessibilityManager mgr = AccessibilityManager.getInstance();
            long id = idOf(AccessibilityInspector.snapshot(w), "in the window");
            assertTrue(id != 0, "precondition: the button is in the tree");

            // Accepted while the window is up, then the window goes before the queued
            // action reaches the event thread.
            assertTrue(mgr.performAction(id, "activate", null),
                    "precondition: the action is accepted while the window is showing");
            w.hide();
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();

            assertFalse(pressed[0],
                    "an action must not run on a window that has gone in the meantime");
        } finally {
            implementation.setAccessibilityTreeSupported(false);
            implementation.setAccessibilityTreeUpdateRequired(null);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }
    @FormTest
    void anActionOnAComponentTakenOutOfItsFormDoesNotRun() {
        // The component can be removed between the id being resolved and the action
        // running -- a rebuilt form, a list that replaced its rows -- and pressing it
        // then acts on something no longer on screen, on a surface that is otherwise
        // still perfectly visible.
        implementation.setAccessibilityTreeSupported(true);
        implementation.setAccessibilityTreeUpdateRequired(Boolean.FALSE);
        try {
            Form form = new Form("main", new BorderLayout());
            final boolean[] pressed = new boolean[1];
            Button b = new Button("press me");
            b.addActionListener(new com.codename1.ui.events.ActionListener() {
                @Override
                public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                    pressed[0] = true;
                }
            });
            form.add(BorderLayout.CENTER, b);
            form.show();
            DisplayTest.flushEdt();

            AccessibilityManager mgr = AccessibilityManager.getInstance();
            long id = idOf(AccessibilityInspector.snapshot(form), "press me");
            assertTrue(id != 0, "precondition: the button is in the tree");

            assertTrue(mgr.performAction(id, "activate", null),
                    "precondition: accepted while the button is still in the form");
            b.remove();
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();

            assertFalse(pressed[0],
                    "an action must not run on a component taken out of its surface");
        } finally {
            implementation.setAccessibilityTreeSupported(false);
            implementation.setAccessibilityTreeUpdateRequired(null);
        }
    }
    @FormTest
    void anActionIsJudgedAgainstItsOwnFormNotTheLastOneDescribed() {
        // The action's surface is routinely not the one described most recently: a tree
        // for the form on screen sits in the cache while a form being prepared off it is
        // the latest one built. Judged from a single flag recorded for that latest tree,
        // an action on the visible form was measured against the prepared form's history
        // -- so navigating away before it ran left it looking live, and it pressed a
        // button on a form the user had already left.
        implementation.setAccessibilityTreeSupported(true);
        implementation.setAccessibilityTreeUpdateRequired(Boolean.FALSE);
        try {
            Form onScreen = new Form("on screen", new BorderLayout());
            final boolean[] pressed = new boolean[1];
            Button b = new Button("press me");
            b.addActionListener(new com.codename1.ui.events.ActionListener() {
                @Override
                public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                    pressed[0] = true;
                }
            });
            onScreen.add(BorderLayout.CENTER, b);
            onScreen.show();
            DisplayTest.flushEdt();

            AccessibilityManager mgr = AccessibilityManager.getInstance();
            long id = idOf(AccessibilityInspector.snapshot(onScreen), "press me");
            assertTrue(id != 0, "precondition: the button is in the tree");

            // A form that is prepared but never shown becomes the latest tree built, so
            // the last-described history now describes it rather than the visible form.
            Form prepared = new Form("prepared", new BorderLayout());
            prepared.add(BorderLayout.CENTER, new Button("not on screen"));
            AccessibilityInspector.snapshot(prepared);

            assertTrue(mgr.performAction(id, "activate", null),
                    "precondition: accepted while its own form is still on screen");
            new Form("second", new BorderLayout()).show();
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();

            assertFalse(pressed[0],
                    "the action belongs to the form navigated away from, not the last "
                            + "tree built, and must not run");
        } finally {
            implementation.setAccessibilityTreeSupported(false);
            implementation.setAccessibilityTreeUpdateRequired(null);
        }
    }
    @FormTest
    void aWindowShownAgainAfterHidingGetsItsTreeBack() {
        // Guards behaviour rather than a fix: show() revalidates and re-focuses, and
        // both of those invalidate this root, so a tree evicted while the window was
        // hidden comes back without the restore path having to ask for it. Asserted so
        // that stays true -- a show that stopped invalidating would leave it with none.
        implementation.setMultiWindowSupported(true);
        implementation.setAccessibilityTreeSupported(true);
        final Window w = new Window("second", new BorderLayout());
        try {
            Form main = new Form("main", new BorderLayout());
            final Button mainButton = new Button("on the main form");
            main.add(BorderLayout.CENTER, mainButton);
            main.show();
            DisplayTest.flushEdt();

            w.setWindowSize(400, 300);
            w.add(BorderLayout.CENTER, new Button("in the window"));
            w.show();
            DisplayTest.flushEdt();
            assertTrue(mentions(offEdtSnapshot(w), "in the window"),
                    "precondition: the window has a tree to lose");

            // Hidden, and evicted while it is away, with the main form rebuilt in between
            // so the window is not simply the last tree built.
            w.hide();
            DisplayTest.flushEdt();
            mainButton.setText("renamed on the main form");
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();
            AccessibilityManager.getInstance().releaseRoot(w);
            assertFalse(mentions(offEdtSnapshot(w), "in the window"),
                    "precondition: the cached tree is actually gone");

            w.show();
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();

            assertTrue(mentions(offEdtSnapshot(w), "in the window"),
                    "a window shown again has to have a tree again");
        } finally {
            implementation.setAccessibilityTreeSupported(false);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    /// One invalidation has to settle, even when describing the surface mutates it.
    ///
    /// `List.getAccessibilityItemText` runs the shared cell renderer to find out what an
    /// item says, and the renderer's own `setText` invalidates accessibility in turn. On
    /// a port that projects eagerly that made the refresh pass queue itself again from
    /// its own output: every pass dirtied the tree it had just built, re-posted, and the
    /// serial-call queue grew without bound until the surface stopped being painted.
    @FormTest
    void describingAListDoesNotQueueRefreshesForever() {
        implementation.setAccessibilityTreeSupported(true);
        implementation.setAccessibilityTreeUpdateRequired(Boolean.TRUE);
        try {
            Form f = new Form("list", new BorderLayout());
            com.codename1.ui.List<String> list =
                    new com.codename1.ui.List<String>(new String[]{"alpha", "beta", "gamma"});
            f.add(BorderLayout.CENTER, list);
            f.show();
            DisplayTest.flushEdt();
            DisplayTest.flushEdt();

            implementation.clearAccessibilityNotifications();
            AccessibilityManager.getInstance().invalidate(list, AccessibilityManager.CHANGE_CONTENT);
            // Well past the one pass the invalidation is owed. A self-feeding pass
            // produces one more notification for every drain, so the count tracks the
            // number of flushes instead of standing still.
            for (int iter = 0; iter < 12; iter++) {
                DisplayTest.flushEdt();
            }

            assertTrue(implementation.getAccessibilityNotifications().size() <= 2,
                    "one invalidation has to settle, but the refresh pass reported "
                            + implementation.getAccessibilityNotifications().size()
                            + " times -- it is queueing itself from its own output");
        } finally {
            implementation.setAccessibilityTreeUpdateRequired(null);
            implementation.setAccessibilityTreeSupported(false);
            DisplayTest.flushEdt();
        }
    }
}
