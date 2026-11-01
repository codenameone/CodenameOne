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

    /// Runs queued tasks, in order, until none is left (tasks may queue more). Off the
    /// EDT of a live Display this does nothing: the posted turn drains on the EDT.
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
        try {
            while (true) {
                Runnable next;
                synchronized (LOCK) {
                    next = QUEUE.pollFirst();
                }
                if (next == null) {
                    return;
                }
                next.run();
            }
        } finally {
            synchronized (LOCK) {
                drainer = outer;
            }
        }
    }
}
