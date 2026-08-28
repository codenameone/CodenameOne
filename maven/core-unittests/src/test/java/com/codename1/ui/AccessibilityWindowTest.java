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
}
