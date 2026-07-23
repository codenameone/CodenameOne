/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase.bluetooth;

import java.util.PriorityQueue;

/**
 * A fully deterministic {@link SimScheduler} driven by an explicit virtual
 * clock. Nothing runs until the owner pumps it:
 *
 * <ul>
 *   <li>{@link #pump()} runs the single next task that is due at the
 *       current virtual time.</li>
 *   <li>{@link #pumpAll()} runs due tasks until none remain due (delayed
 *       tasks stay queued).</li>
 *   <li>{@link #advance(long)} moves the virtual clock forward, releasing
 *       and running delayed tasks in order (due time first, submission
 *       order for ties) -- including tasks scheduled by the tasks it runs,
 *       as long as they fall inside the advanced window.</li>
 * </ul>
 *
 * <p>The class never reads real time; the clock only moves via
 * {@link #advance(long)}.</p>
 */
public final class ManualScheduler implements SimScheduler {

    /**
     * Safety valve against runaway task loops wedging a test forever: a
     * single drain (pumpAll/advance) refuses to run more than this many
     * tasks and fails loudly instead.
     */
    private static final int MAX_TASKS_PER_DRAIN = 100000;

    private static final class ScheduledTask
            implements Comparable<ScheduledTask> {
        final long due;
        final long seq;
        final Runnable task;

        ScheduledTask(long due, long seq, Runnable task) {
            this.due = due;
            this.seq = seq;
            this.task = task;
        }

        public int compareTo(ScheduledTask o) {
            if (due != o.due) {
                return due < o.due ? -1 : 1;
            }
            if (seq != o.seq) {
                return seq < o.seq ? -1 : 1;
            }
            return 0;
        }
    }

    private final PriorityQueue<ScheduledTask> queue =
            new PriorityQueue<ScheduledTask>();
    private long clock;
    private long seq;
    private boolean shutdown;

    public void post(Runnable task) {
        postDelayed(task, 0);
    }

    public synchronized void postDelayed(Runnable task, long millis) {
        if (task == null || shutdown) {
            return;
        }
        queue.add(new ScheduledTask(clock + Math.max(0, millis), seq++, task));
    }

    public synchronized void shutdown() {
        shutdown = true;
        queue.clear();
    }

    /**
     * The current virtual time in milliseconds; starts at {@code 0}.
     */
    public synchronized long getVirtualTimeMillis() {
        return clock;
    }

    /**
     * Runs the single next task due at the current virtual time.
     *
     * @return {@code true} when a task ran, {@code false} when nothing is
     * due
     */
    public boolean pump() {
        Runnable next;
        synchronized (this) {
            ScheduledTask t = queue.peek();
            if (t == null || t.due > clock) {
                return false;
            }
            queue.poll();
            next = t.task;
        }
        next.run();
        return true;
    }

    /**
     * Runs due tasks (including tasks they post without delay) until the
     * queue is quiet at the current virtual time.
     *
     * @return the number of tasks that ran
     */
    public int pumpAll() {
        int n = 0;
        while (pump()) {
            n++;
            if (n > MAX_TASKS_PER_DRAIN) {
                throw new IllegalStateException(
                        "ManualScheduler did not go quiet after "
                                + MAX_TASKS_PER_DRAIN + " tasks");
            }
        }
        return n;
    }

    /**
     * Moves the virtual clock forward by the given number of milliseconds,
     * running every task that becomes due along the way in deterministic
     * order (due time, then submission order). Tasks scheduled while
     * advancing also run if they land inside the window. The clock ends
     * exactly {@code millis} later.
     */
    public void advance(long millis) {
        long target;
        synchronized (this) {
            target = clock + Math.max(0, millis);
        }
        int n = 0;
        while (true) {
            Runnable next;
            synchronized (this) {
                ScheduledTask t = queue.peek();
                if (t == null || t.due > target) {
                    clock = target;
                    return;
                }
                queue.poll();
                if (t.due > clock) {
                    clock = t.due;
                }
                next = t.task;
            }
            next.run();
            n++;
            if (n > MAX_TASKS_PER_DRAIN) {
                throw new IllegalStateException(
                        "ManualScheduler did not go quiet after "
                                + MAX_TASKS_PER_DRAIN + " tasks");
            }
        }
    }
}
