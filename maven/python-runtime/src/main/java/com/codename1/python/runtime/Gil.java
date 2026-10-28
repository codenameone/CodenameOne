/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.runtime;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/// The global interpreter lock: Python code runs on one thread at a time.
///
/// The runtime's caches, containers and module state assume a single
/// running thread, as CPython's do, and a torn update of one could hand an
/// object of the wrong class to the next instruction -- a native crash on a
/// device, where casts are not checked. Every way into Python takes this
/// lock (the program's start, a callback from Java, a `threading` thread),
/// and every operation that waits releases it for the wait (`time.sleep`, a
/// lock or condition, an HTTP request), so threads take turns at the points
/// where a Python program waits. CPU-bound threads do not run in parallel,
/// as under CPython.
///
/// Reentrant: a callback into Python from Java code that Python called runs
/// on the same thread, which already holds the lock.
public final class Gil {
    /// How many failed attempts a platform thread spins through
    /// `Thread.yield()` before it starts sleeping between attempts.
    private static final int SPINS = 64;

    /// Who holds the lock: 0 when nobody does, a virtual thread's handle
    /// (positive) or a platform thread's negated id. A key rather than the
    /// `Thread`, because every virtual thread on a backend host answers the
    /// host's `Thread` -- two of them would read as one reentrant owner.
    private static final AtomicLong OWNER = new AtomicLong();

    /// Reentry depth. Read and written only by the owner; ownership passes
    /// through a compare-and-set on [#OWNER], which orders it.
    private static int depth;

    /// How many threads are inside [#acquire(long)] waiting for the lock.
    /// [#yieldNow()] reads it to hand the lock over rather than take it
    /// straight back.
    private static final AtomicInteger WAITERS = new AtomicInteger();

    /// How many pauses [#yieldNow()] waits at most for a waiter to take the
    /// lock before it competes for it again.
    private static final long HANDOFF_NANOS = 2000000L;

    /// Set while [#yieldNow()] leaves the lock free for a waiter: waiters
    /// then poll instead of sleeping, so a platform thread asleep in
    /// `Thread.sleep(1)` is not outwaited by the yielder.
    private static final java.util.concurrent.atomic.AtomicBoolean HANDOFF =
            new java.util.concurrent.atomic.AtomicBoolean();

    private static Scheduler scheduler;

    /// What a server with virtual threads tells the lock about them.
    ///
    /// A virtual thread must never block in `Object.wait` or on a contended
    /// monitor: that blocks its HOST thread, and with tasks queued on every
    /// host the whole server wedges. So the lock never blocks -- it is a
    /// compare-and-set, and a waiter steps aside through [#pause()] between
    /// attempts.
    public interface Scheduler {
        /// The running virtual thread's handle, or 0 on a platform thread.
        long current();

        /// Steps the running virtual thread aside so its host can run
        /// another; answers false (and does nothing) on a platform thread.
        boolean pause();
    }

    private Gil() {
    }

    /// Installed once, before the server starts any thread.
    public static void setScheduler(Scheduler s) {
        scheduler = s;
    }

    private static long key() {
        Scheduler s = scheduler;
        if (s != null) {
            long vt = s.current();
            if (vt != 0) {
                return vt;
            }
        }
        return -(Thread.currentThread().getId() + 1);
    }

    /// Takes the lock, waiting for the thread that holds it to wait or
    /// leave Python.
    public static void enter() {
        long me = key();
        if (OWNER.get() == me) {
            depth++;
            return;
        }
        acquire(me);
        depth = 1;
    }

    private static void acquire(long me) {
        if (OWNER.compareAndSet(0, me)) {
            return;
        }
        WAITERS.incrementAndGet();
        try {
            int attempt = 0;
            while (!OWNER.compareAndSet(0, me)) {
                pauseWaiter(HANDOFF.get() ? 0 : attempt++);
            }
        } finally {
            WAITERS.decrementAndGet();
        }
    }

    /// One step aside for a thread waiting on something another thread
    /// will change: the virtual thread yields its host, a platform thread
    /// yields and, after [#SPINS] attempts, sleeps a millisecond. Never a
    /// monitor wait, which would block a virtual thread's host.
    public static void pauseWaiter(int attempt) {
        Scheduler s = scheduler;
        if (s != null && s.pause()) {
            return;
        }
        if (attempt < SPINS) {
            Thread.yield();
        } else {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // keep waiting: the caller re-checks its condition
            }
        }
    }

    /// Leaves one [#enter()].
    public static void exit() {
        if (OWNER.get() != key()) {
            return;
        }
        depth--;
        if (depth == 0) {
            OWNER.set(0);
        }
    }

    /// Releases the lock entirely before a wait, answering what
    /// [#restore(int)] takes back afterwards (0 when this thread did not
    /// hold it).
    public static int release() {
        if (OWNER.get() != key()) {
            return 0;
        }
        int d = depth;
        depth = 0;
        OWNER.set(0);
        return d;
    }

    /// Takes the lock back after a wait.
    public static void restore(int saved) {
        if (saved > 0) {
            acquire(key());
            depth = saved;
        }
    }

    /// Lets another thread waiting for the lock run, as `time.sleep(0)`
    /// does.
    ///
    /// Fair to a waiter: a bare yield followed by a compare-and-set lets
    /// the yielding thread win the lock straight back almost every time,
    /// starving the others. So when a thread is waiting, the lock is left
    /// free until somebody else has taken it -- for at most
    /// [#HANDOFF_NANOS], longer than a platform waiter's one-millisecond
    /// sleep, and waiters stop sleeping while [#HANDOFF] is set -- and
    /// when nobody is waiting the lock is not released at all.
    public static void yieldNow() {
        if (WAITERS.get() == 0) {
            pauseWaiter(0);
            return;
        }
        int saved = release();
        HANDOFF.set(true);
        long end = System.nanoTime() + HANDOFF_NANOS;
        while (OWNER.get() == 0 && WAITERS.get() > 0 && System.nanoTime() - end < 0) {
            pauseWaiter(0);
        }
        HANDOFF.set(false);
        restore(saved);
    }
}
