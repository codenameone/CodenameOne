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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.flutter.animation.MotionClock;
import com.codename1.flutter.testsupport.RasterDisplay;

import dart.async.Future;
import dart.core.Duration;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/// A ScrollController with more than one scrollable attached drives them all, and an
/// animateTo in flight gives way to the user's own scroll -- both as in Flutter.
class ScrollControllerClientsTest {

    /// A list that records where it was moved and, like a mounted ListView, reports
    /// every move straight back as a scroll.
    static final class EchoingList implements ScrollController.Client {
        final List<Double> moves = new ArrayList<Double>();
        ScrollController controller;

        @Override
        public void scrollToOffset(double offset) {
            moves.add(offset);
            if (controller != null) {
                // What ListViewRenderElement's scroll listener does when setScrollY moves
                // the pane: an echo of the controller's own move, not a user scroll.
                controller.userScrolled(this, offset, 1000, 100);
            }
        }

        double last() {
            return moves.isEmpty() ? Double.NaN : moves.get(moves.size() - 1);
        }
    }

    private static boolean done(Future<Object> f) {
        final boolean[] completed = {false};
        f.then(new dart.runtime.Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                completed[0] = true;
            }
        });
        return completed[0];
    }

    private static Duration ms(int v) {
        return Duration.of(0, 0, 0, 0, v, 0);
    }

    // ------------------------------------------------------------------
    // Several attached scrollables
    // ------------------------------------------------------------------

    @Test
    void jumpToMovesEveryAttachedList() {
        ScrollController c = new ScrollController();
        EchoingList a = new EchoingList();
        EchoingList b = new EchoingList();
        c.attach(a);
        c.attach(b);
        assertTrue(c.hasClients());
        c.jumpTo(120);
        assertEquals(120.0, a.last(), 0.0, "the first list is driven too");
        assertEquals(120.0, b.last(), 0.0);
    }

    @Test
    void animateToMovesEveryAttachedList() {
        ScrollController c = new ScrollController();
        EchoingList a = new EchoingList();
        EchoingList b = new EchoingList();
        c.attach(a);
        c.attach(b);
        c.userScrolled(a, 0, 1000, 100);
        c.userScrolled(b, 0, 1000, 100);
        assertTrue(done(c.animateTo(200, ms(300), null)));
        assertEquals(200.0, a.last(), 0.0);
        assertEquals(200.0, b.last(), 0.0);
    }

    /// Flutter's offset and position read `_positions.single`: with two attached there is
    /// no one answer, and asking is an error rather than a silent pick.
    @Test
    void singlePositionGettersRefuseWithTwoAttached() {
        ScrollController c = new ScrollController();
        EchoingList a = new EchoingList();
        EchoingList b = new EchoingList();
        c.attach(a);
        c.attach(b);
        assertThrows(dart.core.StateError.class, c::offset);
        assertThrows(dart.core.StateError.class, c::position);
        c.detach(a);
        assertTrue(c.hasClients(), "one list is still attached");
        c.userScrolled(b, 40, 1000, 100);
        assertEquals(40.0, c.offset(), 0.0, "with one left, offset is that list's");
        c.detach(b);
        assertFalse(c.hasClients());
    }

    /// Each list keeps its own offset: a scroll of one is not the other's.
    @Test
    void eachListKeepsItsOwnOffset() {
        ScrollController c = new ScrollController();
        EchoingList a = new EchoingList();
        EchoingList b = new EchoingList();
        c.attach(a);
        c.attach(b);
        final int[] notified = {0};
        c.addListener(() -> notified[0]++);
        c.userScrolled(a, 30, 1000, 100);
        assertEquals(1, notified[0], "a scroll of any attached list notifies");
        c.detach(a);
        assertEquals(0.0, c.offset(), 0.0, "b never moved");
    }

    // ------------------------------------------------------------------
    // A user scroll interrupts animateTo
    // ------------------------------------------------------------------

    @Test
    void aUserScrollStopsAnAnimateToInFlight() {
        RasterDisplay.install();
        MotionClock.freeze();
        try {
            ScrollController c = new ScrollController();
            EchoingList list = new EchoingList();
            list.controller = c;
            c.attach(list);
            c.userScrolled(list, 0, 1000, 100);
            Future<Object> f = c.animateTo(200, ms(300), null);
            MotionClock.advanceAndPump(16);
            MotionClock.advanceAndPump(100);
            assertTrue(c.offset() > 0 && c.offset() < 200, "mid-flight, got " + c.offset());
            assertFalse(done(f));

            // The user grabs the list and drags it somewhere of their own.
            c.userScrolled(list, 50, 1000, 100);
            int moves = list.moves.size();
            MotionClock.advanceAndPump(100);
            MotionClock.advanceAndPump(300);
            assertEquals(moves, list.moves.size(), "the animation no longer drives the list");
            assertEquals(50.0, c.offset(), 0.0, "the user's scroll stands");
            assertTrue(done(f), "an interrupted animateTo completes, as Flutter's does");
        } finally {
            MotionClock.release();
            RasterDisplay.uninstall();
        }
    }

    /// The list's report of a move the controller itself made is not a user scroll: the
    /// animation runs to its end.
    @Test
    void theListsEchoDoesNotStopTheAnimation() {
        RasterDisplay.install();
        MotionClock.freeze();
        try {
            ScrollController c = new ScrollController();
            EchoingList list = new EchoingList();
            list.controller = c;
            c.attach(list);
            c.userScrolled(list, 0, 1000, 100);
            Future<Object> f = c.animateTo(200, ms(300), null);
            for (int i = 0; i < 30 && !done(f); i++) {
                MotionClock.advanceAndPump(16);
            }
            assertTrue(done(f));
            assertEquals(200.0, list.last(), 0.0);
            assertEquals(200.0, c.offset(), 0.0);
        } finally {
            MotionClock.release();
            RasterDisplay.uninstall();
        }
    }
}
