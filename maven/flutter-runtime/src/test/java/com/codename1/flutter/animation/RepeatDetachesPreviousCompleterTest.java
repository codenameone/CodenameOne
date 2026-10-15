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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code repeat()} replacing an active {@code forward()}/{@code reverse()}/{@code
 * animateTo()} run must detach that run's completer, not leave it installed.
 *
 * <p>Before this fix, {@code repeat()} never touched {@link AnimationController}'s
 * {@code runCompleter} field at all -- unlike {@code forward}/{@code reverse}/{@code
 * animateTo}/{@code animateBack}/{@code fling}, which all call {@code newRun()} first.
 * So a {@code forward()} still in flight when {@code repeat()} started left forward's
 * own {@code Completer} installed, and a later {@code stop(canceled: false)} -- meant
 * to settle the REPEATING run -- completed forward's future successfully instead, even
 * though forward() was superseded and never reached its target. A canceled TickerFuture
 * must never complete, in Flutter or here.</p>
 *
 * <p>Needs a run that is still ACTIVE when {@code repeat()} replaces it. Headless, every
 * run collapses to its end state inside {@code beginRun} and the run's completer is
 * already consumed before the call returns, so this installs {@link RasterDisplay} to
 * make {@code Display.isInitialized()} true: {@code beginRun} then joins the frame
 * clock instead of finishing synchronously, and the run stays pending until something
 * ticks it (nothing does here, which is exactly what keeps it in flight for the test).</p>
 */
class RepeatDetachesPreviousCompleterTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    @Test
    void repeatDetachesTheSupersededForwardRunsCompleter() {
        AnimationController c = new AnimationController();
        c.duration(Duration.of(0, 0, 0, 0, 300, 0));

        Future<Object> forwardFuture = c.forward();
        final boolean[] forwardCompleted = {false};
        forwardFuture.then(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                forwardCompleted[0] = true;
            }
        });
        assertFalse(forwardCompleted[0],
                "sanity: forward's run must still be ACTIVE for this test to mean anything "
                        + "-- RasterDisplay must have kept beginRun from finishing synchronously");

        // repeat() supersedes the still-active forward() run.
        c.repeat(null, null, null, Duration.of(0, 0, 0, 0, 100, 0), null);
        // A non-canceled stop settles whatever run is CURRENTLY installed -- the
        // repeat run repeat() just started, never forward's superseded one.
        c.stop(Boolean.FALSE);

        assertFalse(forwardCompleted[0],
                "repeat() must detach forward()'s completer; a later stop(canceled: false) "
                        + "of the REPEATING run must not complete forward()'s unrelated future");
    }

    @Test
    void stopStillSettlesARunStartedAfterARepeatDetach() {
        // The flip side of the detach: it must be a SWAP, not "stop() never
        // completes anything again". A fresh forward() started after the
        // repeat()/stop() pair above must still behave normally.
        AnimationController c = new AnimationController();
        c.duration(Duration.of(0, 0, 0, 0, 300, 0));
        c.forward();
        c.repeat(null, null, null, Duration.of(0, 0, 0, 0, 100, 0), null);
        c.stop(Boolean.FALSE);

        Future<Object> laterFuture = c.forward();
        final boolean[] laterCompleted = {false};
        laterFuture.then(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                laterCompleted[0] = true;
            }
        });
        c.stop(Boolean.FALSE);
        assertTrue(laterCompleted[0],
                "a run started after a repeat()/stop() pair must still settle normally");
    }
}
