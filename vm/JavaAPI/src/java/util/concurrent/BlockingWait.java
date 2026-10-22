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
package java.util.concurrent;

import com.codename1.impl.JdkHost;

/// A wait on a monitor until a condition holds or a deadline passes, for
/// every blocking call in this package: `Future.get`, `awaitTermination`,
/// `CountDownLatch.await` and the like.
///
/// The UI thread must never block. Called there, the wait runs through
/// `JdkHost.blockUi`, which on a Codename One device is
/// `Display.invokeAndBlock`: events keep being dispatched while it waits.
/// Everywhere else -- a worker, the backend, a VM test -- it is a plain
/// `Object.wait` on the caller's thread and is interruptible.
///
/// Whoever makes the condition true calls `notifyAll()` on the same monitor
/// while holding it.
abstract class BlockingWait implements Runnable {

    /// A deadline that never comes.
    static final long FOREVER = Long.MAX_VALUE;

    private final Object monitor;
    private final long deadline;
    /// Set by [#run] when the wait was interrupted on the thread `blockUi`
    /// lent it; read back by [#await] on the UI thread. Guarded by `this`.
    private InterruptedException interrupted;

    /// A wait on `monitor` until `deadline` ([#FOREVER], or a time from
    /// [#deadline]).
    BlockingWait(Object monitor, long deadline) {
        this.monitor = monitor;
        this.deadline = deadline;
    }

    /// The clock every deadline and delay in this package is read from:
    /// monotonic milliseconds. The wall clock moves when the user or the
    /// network changes the time, and a timed wait or a scheduled task must
    /// not fire early or late because of it.
    static long now() {
        return System.nanoTime() / 1000000L;
    }

    /// `timeout` in milliseconds, rounded UP: a positive timeout shorter
    /// than a millisecond still waits rather than being due immediately.
    static long millis(long timeout, TimeUnit unit) {
        long ms = unit.toMillis(timeout);
        if (timeout > 0 && ms < Long.MAX_VALUE / 1000000L && unit.toNanos(timeout) > ms * 1000000L) {
            ms++;
        }
        return ms;
    }

    /// The deadline `millis` from now, saturating at [#FOREVER]; a negative
    /// duration is due now.
    static long deadline(long millis) {
        long now = now();
        if (millis <= 0) {
            return now;
        }
        // Overflow-safe whatever the clock's origin: a negative reading
        // would make FOREVER - now wrap.
        long due = now + millis;
        return due < now ? FOREVER : due;
    }

    /// Whether the wait is over. Called holding the monitor.
    abstract boolean ready();

    /// Waits until [#ready] or the deadline, whichever is first. The caller
    /// asks [#ready] again (or reads its own state) to learn which.
    ///
    /// A thread that is interrupted does not start to wait: the interrupt is
    /// consumed by the `InterruptedException`, as in the JDK.
    final void await() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        JdkHost host = JdkHost.get();
        if (!host.isUiThread()) {
            block();
            return;
        }
        host.blockUi(this);
        InterruptedException e;
        synchronized (this) {
            e = interrupted;
            interrupted = null;
        }
        if (e != null) {
            throw e;
        }
    }

    /// The wait, on the thread `JdkHost.blockUi` runs it on.
    @Override
    public final void run() {
        try {
            block();
        } catch (InterruptedException e) {
            synchronized (this) {
                interrupted = e;
            }
        }
    }

    private void block() throws InterruptedException {
        synchronized (monitor) {
            while (!ready()) {
                if (deadline == FOREVER) {
                    monitor.wait();
                } else {
                    long left = deadline - now();
                    if (left <= 0) {
                        return;
                    }
                    monitor.wait(left);
                }
            }
        }
    }
}
