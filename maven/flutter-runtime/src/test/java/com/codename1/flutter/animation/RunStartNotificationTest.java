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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A run must notify its listeners once while it is still at its starting value.
 *
 * <p>Flutter's Ticker calls its callback on the first frame with an elapsed of zero and
 * {@code AnimationController._tick} notifies unconditionally, so a listener is guaranteed
 * one call before the value has moved. Apps build on that: Reply's bottom drawer animates
 * a controller from 0, and the rebuild that makes the drawer visible comes from a listener
 * that only calls setState while the value is below 0.01. Without the notification at the
 * start of the run, the first call a listener ever saw was already past that threshold --
 * the drawer's state flipped, its arrow turned, and no panel was ever built.</p>
 */
class RunStartNotificationTest {

    private static List<Double> valuesSeenDuring(AnimationController c) {
        final List<Double> seen = new ArrayList<Double>();
        c.addListener(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                seen.add(c.value());
            }
        });
        return seen;
    }

    @Test
    void aRunNotifiesWhileStillAtItsStartingValue() {
        AnimationController c = new AnimationController();
        c.duration(Duration.of(0, 0, 0, 0, 300, 0));
        List<Double> seen = valuesSeenDuring(c);

        c.forward();

        assertTrue(seen.size() >= 2,
                "expected a notification at the start of the run and one at its end, got "
                        + seen);
        assertEquals(0.0, seen.get(0).doubleValue(), 1e-9,
                "the first notification of a run carries the value the run started from");
        assertEquals(1.0, seen.get(seen.size() - 1).doubleValue(), 1e-9,
                "and the run still ends where it was headed");
    }

    /// Reply's exact shape: the listener acts only while the value is near zero, so it
    /// depends entirely on being called before the animation has moved.
    @Test
    void aListenerGatedOnTheStartingValueRuns() {
        final AnimationController c = new AnimationController();
        c.duration(Duration.of(0, 0, 0, 0, 300, 0));
        final int[] fired = {0};
        c.addListener(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (c.value().doubleValue() < 0.01) {
                    fired[0]++;
                }
            }
        });

        c.forward();

        assertEquals(1, fired[0],
                "a listener gated on the run's starting value must be called exactly once");
    }

    /// A run with nowhere to go still reports; it just never had a first tick distinct
    /// from its last.
    @Test
    void aRunThatCannotMoveStillReportsOnce() {
        AnimationController c = new AnimationController();
        c.duration(Duration.of(0, 0, 0, 0, 300, 0));
        c.value(1.0);
        List<Double> seen = valuesSeenDuring(c);

        c.forward();

        assertEquals(1, seen.size(), "already at the upper bound: one notification, got " + seen);
        assertEquals(1.0, seen.get(0).doubleValue(), 1e-9);
    }
}
