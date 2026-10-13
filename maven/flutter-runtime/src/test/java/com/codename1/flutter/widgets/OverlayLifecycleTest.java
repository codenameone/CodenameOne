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
package com.codename1.flutter.widgets;

import com.codename1.flutter.foundation.FlutterError;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Three lifecycle bugs in the {@link Overlay}/{@link OverlayEntry}/{@link OverlayState}
 * trio, all reachable through their plain public API with no mounted widget tree needed
 * (same pattern as {@link OverlayInitialEntriesTest}).
 */
class OverlayLifecycleTest {

    // ------------------------------------------------------------------
    // Overlay.of with no ancestor Overlay
    // ------------------------------------------------------------------

    @Test
    void ofThrowsFlutterErrorWithNoAncestorOverlay() {
        // maybeOf must still answer null -- Overlay.of's fallback is the bug, not this.
        assertNull(Overlay.maybeOf(null, false));

        FlutterError err = assertThrows(FlutterError.class, () -> Overlay.of(null, false, null),
                "Flutter's Overlay.of throws rather than handing back a USABLE overlay; a "
                        + "fresh, detached OverlayState let insert() silently do nothing");
        assertTrue(err.getMessage().contains("No Overlay widget found"));
    }

    // ------------------------------------------------------------------
    // OverlayEntry re-insertion
    // ------------------------------------------------------------------

    @Test
    void reInsertingARemovedEntryComesBackMounted() {
        OverlayState state = new OverlayState();
        OverlayEntry entry = new OverlayEntry();
        state.insert(entry, null, null);
        assertTrue(entry.mounted(), "freshly inserted");

        entry.remove();
        assertFalse(entry.mounted(), "removed");

        // insert() permits re-adding an entry whose owner is gone (remove() cleared
        // it); only attach() runs again, which is exactly where mounted must reset.
        state.insert(entry, null, null);
        assertTrue(entry.mounted(),
                "a re-inserted entry must come back mounted, or OverlayElement.build() "
                        + "skips it forever even though it is back in the overlay");
        assertTrue(state.entries().contains(entry));
    }

    // ------------------------------------------------------------------
    // OverlayState.insertAll ordering
    // ------------------------------------------------------------------

    @Test
    void insertAllWithAnAboveAnchorPreservesCallerOrder() {
        OverlayState state = new OverlayState();
        OverlayEntry anchor = new OverlayEntry();
        state.insert(anchor, null, null);

        OverlayEntry a = new OverlayEntry();
        OverlayEntry b = new OverlayEntry();
        DartList<OverlayEntry> group = new DartList<OverlayEntry>();
        group.add(a);
        group.add(b);

        state.insertAll(group, null, anchor);

        assertEquals(Arrays.asList(anchor, a, b), state.entries(),
                "insertAll(above: anchor) must land the group in CALLER order right "
                        + "after the anchor, not reversed");
    }

    @Test
    void insertAllWithABelowAnchorAlsoPreservesCallerOrder() {
        // The mirror case: below's own index shifts forward as each entry lands
        // just before it, so this already worked -- kept as a guard against
        // breaking it while fixing the `above` case.
        OverlayState state = new OverlayState();
        OverlayEntry anchor = new OverlayEntry();
        state.insert(anchor, null, null);

        OverlayEntry a = new OverlayEntry();
        OverlayEntry b = new OverlayEntry();
        DartList<OverlayEntry> group = new DartList<OverlayEntry>();
        group.add(a);
        group.add(b);

        state.insertAll(group, anchor, null);

        assertEquals(Arrays.asList(a, b, anchor), state.entries());
    }
}
