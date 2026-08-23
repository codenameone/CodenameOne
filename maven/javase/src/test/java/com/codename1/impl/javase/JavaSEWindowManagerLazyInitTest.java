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
package com.codename1.impl.javase;

import com.codename1.testing.junit.CodenameOneTest;

import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Guards the lazy creation of the port's {@link JavaSEWindowManager}.
 *
 * <p>The manager is created on first use, and both of its entry points are reachable
 * off the EDT: {@code Desktop.isSupported()} and the {@code Window} constructor are
 * callable from any thread. An unsynchronized lazy init therefore lets two threads
 * both see a null field and both construct a manager.</p>
 *
 * <p>The cost is not a wasted allocation. The constructor starts the monitor-topology
 * poller, a daemon timer that wakes every two seconds. Only the last manager stays
 * reachable through the field, so only that one can ever be stopped -- by
 * {@code deinitialize()} or anything else. The loser's poller keeps running for the
 * life of the process, reporting every monitor change a second time and outliving the
 * teardown that was supposed to end it.</p>
 *
 * <p>Asserting that concurrent callers get one <em>identical</em> manager is what rules
 * that out: a second instance is precisely a second poller.</p>
 *
 * @author Shai Almog
 */
@CodenameOneTest
class JavaSEWindowManagerLazyInitTest {

    /** Racers per round. Comfortably more than the cores on a CI box, to force overlap. */
    private static final int THREADS = 12;

    /**
     * Rounds. The window is wide -- the constructor samples the graphics environment --
     * but it is still a race, so one round is not a measurement.
     */
    private static final int ROUNDS = 25;

    @Test
    void concurrentCallersAllGetTheSameManager() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        final JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port, "the port should be booted by CodenameOneTest");
        assumeTrue(port.getWindowManager() != null, "this port reports no window manager");

        Field field = JavaSEPort.class.getDeclaredField("windowManager");
        field.setAccessible(true);
        // Never stopped or replaced: it is put back at the end so the rest of the suite
        // keeps the manager, and its poller, that it started with.
        JavaSEWindowManager original = (JavaSEWindowManager) field.get(port);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                field.set(port, null);
                final Set<JavaSEWindowManager> created = Collections.newSetFromMap(
                        Collections.synchronizedMap(
                                new IdentityHashMap<JavaSEWindowManager, Boolean>()));
                final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
                final CyclicBarrier startTogether = new CyclicBarrier(THREADS);
                final CountDownLatch finished = new CountDownLatch(THREADS);
                for (int i = 0; i < THREADS; i++) {
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            try {
                                startTogether.await();
                                created.add((JavaSEWindowManager) port.getWindowManager());
                            } catch (Throwable err) {
                                failure.compareAndSet(null, err);
                            } finally {
                                finished.countDown();
                            }
                        }
                    }, "window-manager-racer");
                    t.setDaemon(true);
                    t.start();
                }
                finished.await();
                try {
                    if (failure.get() != null) {
                        throw new AssertionError("a racing caller failed", failure.get());
                    }
                    assertEquals(1, created.size(),
                            "round " + round + ": concurrent getWindowManager() calls built "
                                    + created.size() + " managers, so " + (created.size() - 1)
                                    + " monitor poller(s) are now unreachable and unstoppable");
                } finally {
                    // Stop every poller this round started, the orphans included -- the
                    // field can only ever reach one of them.
                    for (JavaSEWindowManager manager : created) {
                        manager.stopWatchingMonitorTopology();
                    }
                }
            }
        } finally {
            field.set(port, original);
        }
    }
}
