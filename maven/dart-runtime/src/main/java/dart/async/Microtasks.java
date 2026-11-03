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

    /// Headless only: timer callbacks whose deadline passed, waiting for the isolate's
    /// thread. A timer used to run its callback on its own sleeping thread, so it raced
    /// the thread parked in an await: a zero-delay Future.delayed completed and drained
    /// microtasks there while the awaiting frame woke and ran on, and an event a
    /// controller had queued was delivered after the code that followed the await, or
    /// not at all (the drainer check let one thread abandon a task the other had just
    /// stopped looking for). Now the timer thread only posts here, and the event runs
    /// where the microtasks run, one at a time, after the microtask queue is empty --
    /// Dart's event loop order.
    private static final ArrayDeque<Runnable> EVENTS = new ArrayDeque<Runnable>();
    /// Headless timers that have neither fired nor been cancelled. Dart's VM does not
    /// exit while one is pending, so neither does {@link #runUntilIdle()}.
    private static int pendingTimers;

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

    /// Timer events on the isolate thread's stack right now. Each is still counted in
    /// pendingTimers until it returns, so a runUntilIdle() called from inside one (an
    /// async body resumed by a timer that then runs the loop nested, as native code
    /// answering a channel call does) waited for its own event to end: a deadlock
    /// whenever the resumption happened to arrive by timer rather than by microtask.
    private static int runningTimerEvents;

    static void timerStarted() {
        synchronized (LOCK) {
            pendingTimers++;
        }
    }

    /// A pending timer was cancelled before its event ran.
    static void timerEnded() {
        synchronized (LOCK) {
            pendingTimers--;
            LOCK.notifyAll();
        }
    }

    /// Posts a fired headless timer's callback to the isolate's thread. The pending
    /// count drops when the event has run, not now, so exit cannot slip between.
    static void postTimerEvent(final Runnable r) {
        synchronized (LOCK) {
            EVENTS.addLast(new Runnable() {
                @Override
                public void run() {
                    synchronized (LOCK) {
                        runningTimerEvents++;
                    }
                    try {
                        r.run();
                    } finally {
                        synchronized (LOCK) {
                            runningTimerEvents--;
                        }
                        timerEnded();
                    }
                }
            });
            LOCK.notifyAll();
        }
    }

    /// Runs queued tasks, in order, until none is left (tasks may queue more), then
    /// one fired timer event at a time, each followed by the microtasks it queued. Off
    /// the EDT of a live Display this does nothing: the posted turn drains on the EDT.
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
                        next = EVENTS.pollFirst();
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
        return QUEUE.isEmpty() && EVENTS.isEmpty();
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
                    if (pendingTimers - runningTimerEvents <= 0 && runningFrames <= 0) {
                        return;
                    }
                    try {
                        LOCK.wait(20);
                    } catch (InterruptedException ignore) {
                        // the loop condition decides
                    }
                }
            }
        }
    }
}
