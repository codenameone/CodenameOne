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
package dart.async;

import dart.core.Duration;
import dart.runtime.Funcs;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Cancelling a timer unschedules the task CN.setTimeout created, including the
/// current link of a wait longer than an int of milliseconds, and releases the
/// callback.
public class TimerCancelTest {

    private final Timers.Scheduler original = Timers.scheduler;
    private final List<java.util.Timer> timers = new ArrayList<java.util.Timer>();
    private final List<Integer> delays = new ArrayList<Integer>();
    private final List<Runnable> tasks = new ArrayList<Runnable>();

    private void recordSchedules() {
        Timers.scheduler = new Timers.Scheduler() {
            @Override
            public java.util.Timer schedule(int ms, Runnable r) {
                java.util.Timer t = new java.util.Timer(true);
                timers.add(t);
                delays.add(Integer.valueOf(ms));
                tasks.add(r);
                return t;
            }
        };
    }

    @AfterEach
    public void restore() {
        Timers.scheduler = original;
        for (java.util.Timer t : timers) {
            t.cancel();
        }
    }

    /** A cancelled java.util.Timer refuses new tasks; a live one accepts them. */
    private static boolean isCancelled(java.util.Timer t) {
        try {
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                }
            }, 3600000L);
            return false;
        } catch (IllegalStateException cancelled) {
            return true;
        }
    }

    @Test
    public void cancelUnschedulesTheTask() {
        recordSchedules();
        Timers.Handle h = Timers.schedule(10, new Runnable() {
            @Override
            public void run() {
            }
        });
        assertEquals(1, timers.size());
        assertFalse(isCancelled(timers.get(0)));
        h.cancel();
        assertTrue(isCancelled(timers.get(0)), "the scheduled task must be cancelled, not just flagged");
    }

    @Test
    public void cancelReachesTheCurrentLinkOfAChainedWait() {
        recordSchedules();
        final int[] ran = new int[1];
        Timers.Handle h = Timers.schedule(Integer.MAX_VALUE + 5L, new Runnable() {
            @Override
            public void run() {
                ran[0]++;
            }
        });
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), delays.get(0));
        tasks.get(0).run();
        assertEquals(2, timers.size());
        assertEquals(Integer.valueOf(5), delays.get(1));
        h.cancel();
        assertTrue(isCancelled(timers.get(1)), "the link scheduled now is the one to cancel");
        assertEquals(0, ran[0]);
    }

    @Test
    public void aLinkFiringAfterCancelSchedulesNothing() {
        recordSchedules();
        Timers.Handle h = Timers.schedule(Integer.MAX_VALUE + 5L, new Runnable() {
            @Override
            public void run() {
            }
        });
        h.cancel();
        assertTrue(isCancelled(timers.get(0)));
        // a link already handed to the EDT when cancel() ran
        tasks.get(0).run();
        assertEquals(1, timers.size(), "a cancelled chain must not schedule its next link");
    }

    @Test
    public void cancelReleasesTheCallback() {
        Timer t = new Timer(Duration.of(0, 1, 0, 0, 0, 0), new Funcs.VoidFunc0() {
            @Override
            public void call() {
            }
        });
        assertTrue(t.isActive());
        t.cancel();
        assertFalse(t.isActive());
        assertNull(t.callback, "a cancelled timer must not keep its callback reachable");
    }

    @Test
    public void aNegativeDelayIsScheduledAsZero() {
        recordSchedules();
        Timers.schedule(-5, new Runnable() {
            @Override
            public void run() {
            }
        });
        assertEquals(1, delays.size());
        assertEquals(Integer.valueOf(0), delays.get(0));
    }
}
