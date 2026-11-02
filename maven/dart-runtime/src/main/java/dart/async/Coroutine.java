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

import dart.runtime.Funcs;
import java.util.ArrayList;
import java.util.List;

/// One running Dart async body, on a thread of its own that only ever runs while the
/// thread that handed it control waits: a coroutine, not a concurrent task.
///
/// Dart runs an async body synchronously up to its first {@code await}, hands the
/// caller its Future there, and resumes the rest from the event loop once the awaited
/// future completes -- always after the synchronous code that was running, and even
/// when that future had completed already. The body here is ordinary Java (the await
/// is a call, not a continuation), so it runs on its own stack: {@link #start} hands
/// control to that stack and waits, {@link #suspendUntil} hands it back and queues the
/// resume as a microtask, and the microtask hands control over again. Exactly one of
/// the two threads runs at any moment, and the handoff is a happens-before edge, so
/// the body sees everything its caller did and the other way round.
///
/// {@link Zone#current} is per isolate, not per stack, so each side's zone is saved and
/// restored at every switch: a body suspended inside runZoned resumes in that zone
/// while its caller goes on in its own.
final class Coroutine implements Runnable {
    private static final ThreadLocal<Coroutine> CURRENT = new ThreadLocal<Coroutine>();

    /// Idle body threads, reused: a thread per call costs a stack and a start each time.
    private static final List<Worker> IDLE = new ArrayList<Worker>();

    private final Object lock = new Object();
    private final Funcs.Func0<?> body;
    private Zone zone;
    private boolean bodyTurn;
    private boolean finished;
    private Object result;
    private Throwable failure;
    private Runnable onFinish;

    private Coroutine(Funcs.Func0<?> body) {
        this.body = body;
        this.zone = Zone.current;
    }

    /// The coroutine whose body is running on this thread, or null.
    static Coroutine current() {
        return CURRENT.get();
    }

    /// Runs body until it finishes or first suspends.
    static Coroutine start(Funcs.Func0<?> body) {
        Coroutine c = new Coroutine(body);
        c.transfer(true);
        return c;
    }

    boolean isFinished() {
        synchronized (lock) {
            return finished;
        }
    }

    /// Runs r on the resuming thread once a later segment of the body finishes.
    void onFinish(Runnable r) {
        onFinish = r;
    }

    Object result() {
        return result;
    }

    Throwable failure() {
        return failure;
    }

    /// The zone the body was in when it suspended or finished.
    Zone zone() {
        return zone;
    }

    /// Hands control to the body and waits until it suspends or finishes.
    private void transfer(boolean first) {
        Zone saved = Zone.current;
        Zone.current = zone;
        synchronized (lock) {
            bodyTurn = true;
            if (first) {
                Worker.run(this);
            } else {
                lock.notifyAll();
            }
            while (bodyTurn) {
                try {
                    lock.wait();
                } catch (InterruptedException ignore) {
                    // the loop condition decides
                }
            }
        }
        zone = Zone.current;
        Zone.current = saved;
        if (!first && onFinish != null && isFinished()) {
            onFinish.run();
        }
    }

    @Override
    public void run() {
        CURRENT.set(this);
        try {
            result = body.call();
        } catch (Throwable t) {
            failure = t;
        } finally {
            CURRENT.set(null);
            synchronized (lock) {
                finished = true;
                bodyTurn = false;
                lock.notifyAll();
            }
        }
    }

    /// Called on the body's thread by an await: gives control back and returns once the
    /// event loop resumes it, which happens in a microtask after f completes -- never
    /// before the code that was running finishes, even when f is complete already.
    void suspendUntil(Future<?> f) {
        // A suspended body keeps a headless program alive, as the old blocking await
        // did: the end of main() waits for what it is waiting for.
        Microtasks.frameStarted();
        f.onComplete(new Runnable() {
            @Override
            public void run() {
                Microtasks.schedule(new Runnable() {
                    @Override
                    public void run() {
                        Microtasks.frameEnded();
                        transfer(false);
                    }
                });
            }
        });
        synchronized (lock) {
            bodyTurn = false;
            lock.notifyAll();
            while (!bodyTurn) {
                try {
                    lock.wait();
                } catch (InterruptedException ignore) {
                    // the loop condition decides
                }
            }
        }
    }

    /// A pooled body thread.
    private static final class Worker extends Thread {
        private final Object wlock = new Object();
        private Runnable task;

        Worker() {
            super("dart-async");
            setDaemon(true);
        }

        static void run(Runnable r) {
            Worker w = null;
            synchronized (IDLE) {
                if (!IDLE.isEmpty()) {
                    w = IDLE.remove(IDLE.size() - 1);
                }
            }
            if (w == null) {
                w = new Worker();
                w.task = r;
                w.start();
                return;
            }
            synchronized (w.wlock) {
                w.task = r;
                w.wlock.notifyAll();
            }
        }

        @Override
        public void run() {
            while (true) {
                Runnable r;
                synchronized (wlock) {
                    while (task == null) {
                        try {
                            wlock.wait();
                        } catch (InterruptedException ignore) {
                            // the loop condition decides
                        }
                    }
                    r = task;
                    task = null;
                }
                r.run();
                synchronized (IDLE) {
                    IDLE.add(this);
                }
            }
        }
    }
}
