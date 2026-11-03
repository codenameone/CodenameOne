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

import java.util.ArrayDeque;

/// Dart's microtask queue: what an asynchronous StreamController delivers through (ONE
/// event per microtask, the controller re-queued behind whatever else is waiting, so two
/// controllers fed back to back interleave a1, b1, a2, b2), scheduleMicrotask, and the
/// start of an async* body.
///
/// A microtask runs only on the thread that owns the Dart isolate, once its synchronous
/// code has ended. With a Display that is the EDT: the first task queued posts one
/// Display.callSerially turn that drains the queue, and a drain asked for anywhere else
/// leaves the work to that turn. Headless there is no event loop, so the queue is
/// drained where Dart's synchronous code ends: at every await (before the frame
/// blocks), after a timer callback, and after main() returns (FlutterRegistry and the
/// test runners call {@link Async#runMicrotasks()}). No thread is started for it.
///
/// Headless timers do run on threads of their own, so the queue itself is guarded and
/// only one thread drains at a time: a second asking while another drains leaves the
/// tasks to it (a thread parked in an await keeps draining while it waits -- Dart's
/// event loop running under a suspended frame). Tasks are taken one at a time and run
/// outside the lock, so a task that awaits (and so drains, nested on the same thread)
/// neither deadlocks nor reorders what is left.
final class Microtasks {
    private static final Object LOCK = new Object();
    private static final ArrayDeque<Runnable> QUEUE = new ArrayDeque<Runnable>();
    /// A Display turn that will drain is already posted.
    private static boolean turnPosted;
    /// The thread draining now, or null; drains on it may nest.
    private static Thread drainer;

    private Microtasks() {
    }

    static void schedule(Runnable task) {
        boolean post;
        synchronized (LOCK) {
            QUEUE.addLast(task);
            post = !turnPosted && com.codename1.ui.Display.isInitialized();
            if (post) {
                turnPosted = true;
            }
        }
        if (post) {
            com.codename1.ui.Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    synchronized (LOCK) {
                        turnPosted = false;
                    }
                    drain();
                }
            });
        }
    }

    /// async* bodies running headless on a frame of their own (see AsyncStar) that are
    /// not parked in an await: main() cannot exit while one may still yield, which is
    /// how a bare {@code Stream.error(e).listen(...)} at the end of main() lost its
    /// event. A frame parked in an await does not count, so a body waiting on a quiet
    /// source does not keep the program alive -- as in Dart.
    private static int runningFrames;

    private static final ThreadLocal<Boolean> BACKGROUND_FRAME = new ThreadLocal<Boolean>();

    /// Called on the starting thread, before the frame's thread starts.
    static void frameStarted() {
        synchronized (LOCK) {
            runningFrames++;
        }
    }

    /// Called on the frame's own thread when it starts running.
    static void enterFrame() {
        BACKGROUND_FRAME.set(Boolean.TRUE);
    }

    static void frameEnded() {
        synchronized (LOCK) {
            runningFrames--;
            LOCK.notifyAll();
        }
    }

    /// One pending timer: its deadline, and its creation order to break ties -- Dart
    /// fires timers by deadline and, for equal deadlines, in the order they were made.
    static final class TimerEntry {
        final long deadline;
        final long seq;
        final Runnable task;
        /// The EDT wake-up on the Display path; null headless.
        Timers.Handle wake;

        TimerEntry(long deadline, long seq, Runnable task) {
            this.deadline = deadline;
            this.seq = seq;
            this.task = task;
        }
    }

    /// Every pending timer of the isolate, ordered as Dart orders them. There is no
    /// thread per timer: the event loop (drain, parkUntil, runUntilIdle) fires the
    /// earliest due entry itself, and sleeps until the earliest deadline. Two timers
    /// with one deadline used to be two sleeping threads that woke in whichever
    /// order the scheduler chose.
    private static final java.util.TreeSet<TimerEntry> TIMERS = new java.util.TreeSet<TimerEntry>(
            new java.util.Comparator<TimerEntry>() {
                @Override
                public int compare(TimerEntry x, TimerEntry y) {
                    if (x.deadline != y.deadline) {
                        return x.deadline < y.deadline ? -1 : 1;
                    }
                    return x.seq < y.seq ? -1 : (x.seq == y.seq ? 0 : 1);
                }
            });
    private static long timerSeq;

    private static final Runnable WAKE = new Runnable() {
        @Override
        public void run() {
            drain();
        }
    };

    private static long nowMs() {
        return System.nanoTime() / 1000000L;
    }

    /// Queues {@code task} to run as a timer event {@code ms} from now. With a Display
    /// an EDT timeout only wakes the loop; which timer runs is still decided by the
    /// queue, so equal deadlines keep their creation order there too.
    static TimerEntry scheduleTimer(long ms, Runnable task) {
        long delay = Math.max(0L, ms);
        TimerEntry e;
        synchronized (LOCK) {
            e = new TimerEntry(nowMs() + delay, timerSeq++, task);
            TIMERS.add(e);
            LOCK.notifyAll();
        }
        if (com.codename1.ui.Display.isInitialized()) {
            e.wake = Timers.schedule(delay, WAKE);
        }
        return e;
    }

    /// Removes a timer that has not fired; a no-op for one that has.
    static void cancelTimer(TimerEntry e) {
        synchronized (LOCK) {
            TIMERS.remove(e);
            LOCK.notifyAll();
        }
        Timers.Handle h = e.wake;
        e.wake = null;
        if (h != null) {
            h.cancel();
        }
    }

    /// The earliest timer whose deadline has passed, removed from the queue; or null.
    private static Runnable pollDueTimerLocked() {
        if (TIMERS.isEmpty()) {
            return null;
        }
        TimerEntry first = TIMERS.first();
        if (first.deadline > nowMs()) {
            return null;
        }
        TIMERS.pollFirst();
        return first.task;
    }

    /// How long the loop may sleep before the next timer is due, at most {@code cap}.
    private static long waitMsLocked(long cap) {
        if (TIMERS.isEmpty()) {
            return cap;
        }
        return Math.max(1L, Math.min(cap, TIMERS.first().deadline - nowMs()));
    }

    static void drain() {
        if (com.codename1.ui.Display.isInitialized()
                && !com.codename1.ui.Display.getInstance().isEdt()) {
            return;
        }
        Thread self = Thread.currentThread();
        Thread outer;
        synchronized (LOCK) {
            if (drainer != null && drainer != self) {
                return;
            }
            outer = drainer;
            drainer = self;
        }
        boolean released = false;
        try {
            while (true) {
                Runnable next;
                synchronized (LOCK) {
                    next = QUEUE.pollFirst();
                    if (next == null) {
                        next = pollDueTimerLocked();
                    }
                    if (next == null) {
                        // Released under the same lock that found the queues empty: a
                        // task another thread queues after this either is seen above or
                        // finds no drainer and is drained by that thread's own caller.
                        // Releasing in a finally after the lock was dropped lost it.
                        drainer = outer;
                        released = true;
                        return;
                    }
                }
                next.run();
            }
        } finally {
            if (!released) {
                synchronized (LOCK) {
                    drainer = outer;
                }
            }
        }
    }

    /// Headless: whether anything is queued for the isolate's thread.
    private static boolean idleLocked() {
        return QUEUE.isEmpty()
                && (TIMERS.isEmpty() || TIMERS.first().deadline > nowMs());
    }

    /// Headless park of an awaiting frame: runs the event loop until f completes.
    static void parkUntil(Future<?> f) {
        boolean frame = BACKGROUND_FRAME.get() != null;
        if (frame) {
            frameEnded();
        }
        try {
            parkUntilImpl(f);
        } finally {
            if (frame) {
                frameStarted();
            }
        }
    }

    private static void parkUntilImpl(Future<?> f) {
        while (!f.isDone()) {
            drain();
            synchronized (LOCK) {
                if (idleLocked() && !f.isDone()) {
                    try {
                        // Woken by a posted event; the timeout covers a future some
                        // other thread completes directly (an I/O callback).
                        LOCK.wait(20);
                    } catch (InterruptedException ignore) {
                        // the loop condition decides
                    }
                }
            }
        }
    }

    /// The end of main(): runs microtasks and timer events until none is queued and no
    /// timer is pending, as Dart's VM does before it exits. Off the EDT of a live
    /// Display only the (empty) drain happens; the EDT is the loop there.
    static void runUntilIdle() {
        if (com.codename1.ui.Display.isInitialized()) {
            drain();
            return;
        }
        while (true) {
            drain();
            synchronized (LOCK) {
                if (idleLocked()) {
                    if (TIMERS.isEmpty() && runningFrames <= 0) {
                        return;
                    }
                    try {
                        LOCK.wait(waitMsLocked(20));
                    } catch (InterruptedException ignore) {
                        // the loop condition decides
                    }
                }
            }
        }
    }
}
