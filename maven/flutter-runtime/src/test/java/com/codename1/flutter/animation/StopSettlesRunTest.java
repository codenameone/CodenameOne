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

import dart.async.Completer;
import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// stop(canceled: false) completes the run's future; a canceled stop leaves it
/// pending. Tested on the decision itself: headless, a run finishes inside forward(),
/// so no headless test can stop one mid-flight.
class StopSettlesRunTest {

    private static String settle(Boolean canceled) {
        Completer<Object> run = new Completer<Object>();
        final String[] state = {"pending"};
        run.future().then(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                state[0] = "completed";
            }
        });
        AnimationController.settleStoppedRun(run, canceled);
        return state[0];
    }

    @Test
    void notCanceledCompletes() {
        assertEquals("completed", settle(Boolean.FALSE));
    }

    @Test
    void canceledOrDefaultStaysPending() {
        assertEquals("pending", settle(Boolean.TRUE));
        assertEquals("pending", settle(null), "Flutter's default is canceled: true");
    }
}
