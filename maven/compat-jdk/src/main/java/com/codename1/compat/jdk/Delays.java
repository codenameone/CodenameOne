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
package com.codename1.compat.jdk;

import java.util.ArrayList;
import java.util.List;

/// Runs an action after a delay, for everything in this package that
/// waits: the timeouts of [CompletableFuture] and the work of a
/// [ScheduledPool]. One thread serves them all, and it ends when nothing
/// is waiting.
///
/// #### Why not `java.util.Timer`
///
/// A device's `Timer` starts a thread for every task it is given, which
/// sleeps for the whole delay; a cancelled task is not looked at again
/// until that sleep is over. A request that sets a long timeout on a
/// future which then completes at once -- the ordinary case of a timeout --
/// left a thread asleep until the deadline, one per request, and enough of
/// them use up the threads a process may have. Here a cancelled wait is
/// taken out of the queue at once and holds nothing.
///
/// An action runs on the waiting thread, so it must be short: hand real
/// work to a pool, as [ScheduledPool] does.
final class Delays {

    /// One wait: [#cancel] it to give it up.
    static final class Handle {
        private final long due;
        private Runnable action;

        Handle(long due, Runnable action) {
            this.due = due;
            this.action = action;
        }

        /// Gives the wait up. Does nothing once the action has started.
        void cancel() {
            synchronized (LOCK) {
                action = null;
                // Woken so that a thread with nothing left to wait for ends
                // now and not at a deadline nobody is waiting on.
                if (QUEUE.remove(this)) {
                    LOCK.notifyAll();
                }
            }
        }
    }

    private static final Object LOCK = new Object();
    /// Ordered by the time each is due; equal times in the order given.
    private static final List<Handle> QUEUE = new ArrayList<Handle>();
    private static boolean running;

    private Delays() {
    }

    /// Runs `action` once `millis` have passed.
    static Handle after(long millis, Runnable action) {
        Handle handle = new Handle(System.currentTimeMillis() + Math.max(0, millis), action);
        synchronized (LOCK) {
            int at = QUEUE.size();
            while (at > 0 && QUEUE.get(at - 1).due > handle.due) {
                at--;
            }
            QUEUE.add(at, handle);
            if (running) {
                LOCK.notifyAll();
            } else {
                running = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        serve();
                    }
                }, "cn1-delays").start();
            }
        }
        return handle;
    }

    /// How many waits are queued: what a test asks to see that a cancelled
    /// one is gone.
    static int pending() {
        synchronized (LOCK) {
            return QUEUE.size();
        }
    }

    private static void serve() {
        while (true) {
            Runnable action = next();
            if (action == null) {
                return;
            }
            try {
                action.run();
            } catch (Throwable t) {
                // One action that fails must not end the thread the others
                // wait on.
                com.codename1.io.Log.e(t);
            }
        }
    }

    /// The next action that is due, waiting for it; null when the queue is
    /// empty, with the thread marked as gone under the same lock a new wait
    /// would start one under.
    private static Runnable next() {
        synchronized (LOCK) {
            while (true) {
                if (QUEUE.isEmpty()) {
                    running = false;
                    return null;
                }
                Handle first = QUEUE.get(0);
                long wait = first.due - System.currentTimeMillis();
                if (wait <= 0) {
                    QUEUE.remove(0);
                    Runnable action = first.action;
                    first.action = null;
                    if (action != null) {
                        return action;
                    }
                    continue;
                }
                try {
                    LOCK.wait(wait);
                } catch (InterruptedException e) {
                    // Nothing interrupts this thread to stop it: the queue
                    // says when it is done.
                    continue;
                }
            }
        }
    }
}
