/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.junit;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.ImplementationFactory;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The handover from one Display generation to the next.
 *
 * <p>An EDT that has left its dispatch loop is still {@code isAlive()} for as
 * long as its teardown runs. {@code Display.init} tested exactly that, so a
 * second init landing in the gap adopted a thread that would never dispatch
 * again -- and the departing thread then deinitialized whatever implementation
 * was current by then, which was the new one. The display reports
 * {@code codenameOneRunning} true with an implementation that says it is not
 * initialized, every call queues onto nothing, and the suite reports
 * "timed out after 5000ms; edt=display-not-initialized" for a whole class.</p>
 *
 * <p>The window is a few instructions wide on an idle machine, which is why it
 * only ever appeared on loaded CI runners. Here it is held open on purpose.</p>
 */
class EdtHandoverTest {

    /// Sits inside deinitialize() until released, which is exactly the state the
    /// race needs: out of the dispatch loop, into the teardown, still alive.
    private static final class SlowToDie extends TestCodenameOneImplementation {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private volatile boolean sawEdt;

        @Override
        public void deinitialize() {
            // The teardown is meant to run AS the EDT -- disposeAll() above it
            // exists to dispose windows on the thread their tree expects. So the
            // departing thread cannot stop being the EDT to avoid adoption; it
            // has to say it stopped dispatching and stay the EDT until the end.
            sawEdt = Display.getInstance().isEdt();
            entered.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            super.deinitialize();
        }
    }

    private static void useImplementation(final CodenameOneImplementation impl) {
        ImplementationFactory.setInstance(new ImplementationFactory() {
            @Override
            public Object createImplementation() {
                return impl;
            }
        });
    }

    @Test
    void aSecondInitDuringTeardownGetsAWorkingDispatchThread() throws Exception {
        SlowToDie dying = new SlowToDie();
        useImplementation(dying);
        Display.deinitialize();
        Display.init(null);
        assertTrue(Display.isInitialized(), "precondition: the display is up");

        // A form has to be showing, or the EDT never leaves the loop it runs
        // before the first Form.show() -- that one has no codenameOneRunning in
        // its condition, so deinitialize() alone does not end it.
        final CountDownLatch shown = new CountDownLatch(1);
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                new Form("handover").show();
                shown.countDown();
            }
        });
        assertTrue(shown.await(10, TimeUnit.SECONDS),
                "precondition: a form is showing");

        // The old generation goes away, and stops in its teardown.
        Display.deinitialize();
        assertTrue(dying.entered.await(10, TimeUnit.SECONDS),
                "precondition: the departing EDT reached its teardown");

        TestCodenameOneImplementation live = new TestCodenameOneImplementation();
        useImplementation(live);
        Display.init(null);

        final CountDownLatch ran = new CountDownLatch(1);
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                ran.countDown();
            }
        });
        dying.release.countDown();

        try {
            assertTrue(ran.await(5, TimeUnit.SECONDS),
                    "the new generation never dispatched: init adopted the EDT that "
                            + "was on its way out");
            assertTrue(Display.isInitialized(),
                    "the departing EDT deinitialized the LIVE implementation instead "
                            + "of its own");
            assertTrue(dying.sawEdt,
                    "the teardown ran as a non-EDT caller, so disposeAll() disposed "
                            + "the window tree off the thread it belongs to");
        } finally {
            Display.deinitialize();
        }
    }
}
