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

import com.codename1.flutter.testsupport.RasterDisplay;

import dart.async.Future;
import dart.core.Duration;
import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flutter's {@code AnimationController.repeat({count})}: {@code int? count} stops the
 * run after that many legs, completing its future exactly like {@link
 * AnimationController#forward}'s does -- and, without a count, {@code repeat()} never
 * completes on its own (a canceled {@code stop()} does not complete it either,
 * matching Flutter's {@code TickerFuture}). Before this, the Dart stub declared
 * {@code repeat} as {@code void} with no {@code count} parameter at all.
 */
class RepeatCountTest {

    private static boolean done(Future<Object> f) {
        final boolean[] completed = {false};
        f.then(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                completed[0] = true;
            }
        });
        return completed[0];
    }

    @Test
    void repeatWithCountCompletesAfterThatManyLegs() {
        RasterDisplay.install();
        MotionClock.freeze();
        try {
            AnimationController c = new AnimationController();
            c.duration(Duration.of(0, 0, 0, 0, 50, 0));
            Future<Object> f = c.repeat(null, null, null, null, Integer.valueOf(2));
            assertFalse(done(f), "two legs have not played yet");
            for (int i = 0; i < 50 && !done(f); i++) {
                MotionClock.advanceAndPump(16);
            }
            assertTrue(done(f),
                    "repeat() with a count must complete once that many legs have played");
            assertEquals(AnimationStatus.completed, c.status());
        } finally {
            MotionClock.release();
            RasterDisplay.uninstall();
        }
    }

    @Test
    void repeatWithoutCountNeverCompletesOnItsOwn() {
        RasterDisplay.install();
        MotionClock.freeze();
        try {
            AnimationController c = new AnimationController();
            c.duration(Duration.of(0, 0, 0, 0, 50, 0));
            Future<Object> f = c.repeat(null, null, null, null, null);
            for (int i = 0; i < 50; i++) {
                MotionClock.advanceAndPump(16);
            }
            assertFalse(done(f), "without a count, repeat() must run until stop()");
            c.stop(Boolean.FALSE);
            assertTrue(done(f), "stop(canceled: false) settles it, as Flutter's TickerFuture");
        } finally {
            MotionClock.release();
            RasterDisplay.uninstall();
        }
    }
}
