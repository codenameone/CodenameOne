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
package com.codename1.flutter.animation;

import dart.core.Duration;
import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A value listener that starts another run as the current one lands -- reverse() the
 * moment forward() reaches 1.0 -- used to let the finishing run carry on with the NEW
 * run's state: its terminal status came from the new run's direction, the new run's
 * future was the one completed, and the finished forward() future never completed.
 */
class ListenerStartedRunTest {

    private static boolean completes(dart.async.Future<Object> f) {
        final boolean[] done = {false};
        f.then(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                done[0] = true;
            }
        });
        return done[0];
    }

    @Test
    void reversingAtTheEndOfForwardKeepsEachRunsOwnCompletion() {
        final AnimationController c = new AnimationController();
        c.duration(Duration.of(0, 0, 0, 0, 200, 0));
        final List<dart.async.Future<Object>> reverse = new ArrayList<dart.async.Future<Object>>();
        final boolean[] reversed = {false};
        c.addListener(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (!reversed[0] && c.value() >= 1.0) {
                    reversed[0] = true;   // before reverse(), which notifies re-entrantly
                    reverse.add(c.reverse(null));
                }
            }
        });
        final List<AnimationStatus> statuses = new ArrayList<AnimationStatus>();
        c.addStatusListener(new Funcs.VoidFunc1<AnimationStatus>() {
            @Override
            public void call(AnimationStatus s) {
                statuses.add(s);
            }
        });

        // Headless, each run collapses synchronously, so the reverse run started by the
        // listener finishes inside the forward run's final notification.
        dart.async.Future<Object> forward = c.forward(null);

        assertEquals(1, reverse.size());
        assertEquals(true, completes(forward), "the forward run reached its target");
        assertEquals(true, completes(reverse.get(0)), "the reverse run finished too");
        assertEquals(AnimationStatus.dismissed, c.status(), "the LAST run decides the status");
        assertEquals(0.0, c.value(), 1e-9);
        assertEquals(AnimationStatus.dismissed, statuses.get(statuses.size() - 1));
    }

    /// A counted repeat completes headless: every leg collapses synchronously, and the
    /// future used to stay pending after the first.
    @Test
    void aCountedRepeatCompletesHeadless() {
        AnimationController c = new AnimationController();
        c.duration(Duration.of(0, 0, 0, 0, 100, 0));
        assertEquals(true, completes(c.repeat(null, null, Boolean.TRUE, null, Integer.valueOf(3))));
        assertEquals(true, completes(c.repeat(null, null, Boolean.TRUE, null, Integer.valueOf(2))));
    }
}
