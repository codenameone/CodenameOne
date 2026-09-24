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

import com.codename1.flutter.animation.Curves;

import dart.async.Future;
import dart.core.Duration;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// animateTo drives the attached list through a motion and completes when it ends.
/// Headless, a run collapses to its end state, so what is checked here is the
/// plumbing -- the list is driven through the controller's run and reaches the target,
/// the future completes, a jump interrupts -- not the frames in between.
class Round11ScrollTest {

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

    @Test
    void animateToDrivesTheListToTheTargetAndCompletes() {
        ScrollController c = new ScrollController();
        final List<Double> moves = new ArrayList<Double>();
        c.attach(new ScrollController.Client() {
            @Override
            public void scrollToOffset(double offset) {
                moves.add(offset);
            }
        });
        c.userScrolled(0, 1000, 100);   // the list reports its extents, as a mounted one does
        Future<Object> f = c.animateTo(200, Duration.of(0, 0, 0, 0, 300, 0), Curves.easeInOut);
        assertTrue(done(f));
        assertEquals(200.0, moves.get(moves.size() - 1), 0.0);
        assertEquals(200.0, c.offset(), 0.0);
    }

    @Test
    void aZeroDurationOrNoListJumps() {
        ScrollController c = new ScrollController();
        c.userScrolled(0, 1000, 100);
        assertTrue(done(c.animateTo(50, Duration.of(0, 0, 0, 0, 300, 0), null)), "no list: nothing to animate");
        assertEquals(50.0, c.offset(), 0.0);
        final List<Double> moves = new ArrayList<Double>();
        c.attach(new ScrollController.Client() {
            @Override
            public void scrollToOffset(double offset) {
                moves.add(offset);
            }
        });
        moves.clear();
        assertTrue(done(c.animateTo(80, Duration.of(0, 0, 0, 0, 0, 0), null)));
        assertEquals("[80.0]", moves.toString());
    }

    @Test
    void aJumpDuringAnAnimationEndsItAndTheJumpWins() {
        ScrollController c = new ScrollController();
        c.attach(new ScrollController.Client() {
            @Override
            public void scrollToOffset(double offset) {
            }
        });
        c.userScrolled(0, 1000, 100);
        c.animateTo(100, Duration.of(0, 0, 0, 0, 300, 0), null);
        c.jumpTo(10);
        assertEquals(10.0, c.offset(), 0.0);
        c.dispose();
    }
}
