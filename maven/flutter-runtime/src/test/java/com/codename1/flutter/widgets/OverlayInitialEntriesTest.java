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

import dart.core.DartList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Entries given to Overlay.initialEntries have the same lifecycle as inserted ones.
public class OverlayInitialEntriesTest {

    @Test
    public void anInitialEntryCanBeRemoved() {
        OverlayEntry entry = new OverlayEntry();
        DartList<OverlayEntry> initial = new DartList<OverlayEntry>();
        initial.add(entry);
        Overlay overlay = new Overlay();
        overlay.initialEntries(initial);
        Overlay.OverlayElement element = (Overlay.OverlayElement) overlay.createElement();
        OverlayState state = element.state();
        assertTrue(state.entries().contains(entry));

        entry.remove();

        // Unattached, remove() only marked the entry unmounted: it stayed in the
        // overlay's list, and no rebuild was asked for to take it off screen.
        assertFalse(state.entries().contains(entry), "removed from the overlay it belongs to");
    }

    @Test
    public void aRepeatedInitialEntryIsAddedOnce() {
        OverlayEntry entry = new OverlayEntry();
        DartList<OverlayEntry> initial = new DartList<OverlayEntry>();
        initial.add(entry);
        initial.add(entry);
        Overlay overlay = new Overlay();
        overlay.initialEntries(initial);
        OverlayState state = ((Overlay.OverlayElement) overlay.createElement()).state();
        assertEquals(1, state.entries().size());
    }
}
