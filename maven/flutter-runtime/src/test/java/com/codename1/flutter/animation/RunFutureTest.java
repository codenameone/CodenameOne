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

import dart.async.Future;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// The playback controls return the run's future, as Flutter's TickerFuture:
/// `await controller.forward()` waits for the run instead of returning at once.
/// Headless, a run collapses to its end state, so each future is complete by the
/// time the call returns; on a device it completes from the frame clock.
class RunFutureTest {

    @Test
    void everyControlReturnsTheCompletedRunFuture() {
        AnimationController c = new AnimationController();
        c.duration(dart.core.Duration.of(0, 0, 0, 0, 200, 0));
        assertDone(c.forward());
        assertDone(c.reverse());
        assertDone(c.animateTo(0.5, null, null));
        assertDone(c.animateBack(0.2, null, null));
        assertDone(c.fling(1.0, null, null));
    }

    private static void assertDone(Future<Object> f) {
        assertNotNull(f, "a control must return its run's future");
        // Through then(), not getNow(): getNow() answers null for a future that is
        // not done, which is indistinguishable from one completed with null.
        final boolean[] completed = {false};
        f.then(new dart.runtime.Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                completed[0] = true;
            }
        });
        assertTrue(completed[0], "the run's future must be complete once the run has ended");
    }
}
