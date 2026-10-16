/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

import com.codename1.flutter.testsupport.RasterDisplay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The decision FlutterUI.wrap's container makes when Codename One deinitializes
/// it: REMOVED, so the tree must be unmounted, or merely HIDDEN, so it must not.
public class EmbeddedLifetimeTest {

    private static FlutterUI.EmbeddedLifetime mounted() {
        FlutterUI.EmbeddedLifetime l = new FlutterUI.EmbeddedLifetime();
        l.mounted();
        return l;
    }

    /// The attachment question asked a cycle after a deinitialize: removing an
    /// ANCESTOR leaves a nested root's own parent link in place, so the old
    /// getParent() test read a removal as "the form is merely hidden".
    @Test
    public void aRootNestedInARemovedAncestorIsNoLongerAttached() {
        RasterDisplay.install();
        try {
            com.codename1.ui.Form form = new com.codename1.ui.Form();
            com.codename1.ui.Container outer = new com.codename1.ui.Container();
            com.codename1.ui.Container root = new com.codename1.ui.Container();
            outer.add(root);
            form.add(outer);
            assertTrue(FlutterUI.stillAttached(root));

            // What removing an ancestor leaves behind (Container.removeComponent clears
            // only the removed ancestor's parent): the root still has ITS parent, but the
            // chain no longer reaches a form.
            com.codename1.ui.Container removedAncestor = new com.codename1.ui.Container();
            com.codename1.ui.Container nested = new com.codename1.ui.Container();
            removedAncestor.add(nested);
            assertTrue(nested.getParent() == removedAncestor, "the nested root keeps its own parent");
            assertFalse(FlutterUI.stillAttached(nested), "but it no longer belongs to a form");
        } finally {
            RasterDisplay.uninstall();
        }
    }

    @Test
    public void removalUnmounts() {
        FlutterUI.EmbeddedLifetime l = mounted();
        assertTrue(l.onDeinit(), "a deinitialize must schedule the check");
        assertTrue(l.settle(false), "no parent a cycle later means it was removed");
    }

    @Test
    public void hidingTheFormKeepsTheTree() {
        // Navigating to another form deinitializes every component, but they
        // keep their parent -- and Flutter keeps the state of a route it can
        // come back to.
        FlutterUI.EmbeddedLifetime l = mounted();
        assertTrue(l.onDeinit());
        assertFalse(l.settle(true));
        assertFalse(l.onInit(), "shown again, the same tree is still mounted");
    }

    @Test
    public void aMoveWithinOneCycleKeepsTheTree() {
        FlutterUI.EmbeddedLifetime l = mounted();
        assertTrue(l.onDeinit());
        assertFalse(l.onInit(), "re-added before the check ran");
        assertFalse(l.settle(false), "the pending unmount was cancelled by the re-add");
    }

    @Test
    public void addingBackAfterRemovalMountsAFreshTree() {
        FlutterUI.EmbeddedLifetime l = mounted();
        l.onDeinit();
        assertTrue(l.settle(false));
        assertTrue(l.onInit(), "an unmounted tree cannot be shown; it must be mounted again");
        l.mounted();
        assertTrue(l.onDeinit(), "and the fresh tree is tracked like the first");
        assertTrue(l.settle(false));
    }

    @Test
    public void repeatedDeinitializeSchedulesOnce() {
        FlutterUI.EmbeddedLifetime l = mounted();
        assertTrue(l.onDeinit());
        assertFalse(l.onDeinit(), "one pending check is enough");
    }

    @Test
    public void neverUnmountsTwice() {
        FlutterUI.EmbeddedLifetime l = mounted();
        l.onDeinit();
        assertTrue(l.settle(false));
        assertFalse(l.onDeinit(), "nothing is mounted any more");
        assertFalse(l.settle(false));
    }
}
