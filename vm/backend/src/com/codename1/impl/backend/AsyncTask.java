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
package com.codename1.impl.backend;

import com.codename1.backend.Tracing;
import com.codename1.backend.VirtualThread;

import com.codename1.backend.Span;
import com.codename1.backend.Tracer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/// One call of an `@Async` method, and the [Future] its caller holds.
///
/// The build generates a subclass per `@Async` method whose fields are the
/// call's arguments and whose [#call] invokes the method's original body.
/// The rewritten method constructs one, hands it to its executor and returns it,
/// so the caller's `Future` is this object. A method whose body itself
/// returns a Future -- [AsyncResult#of] -- completes this one with that
/// Future's value.
///
/// The caller's span, if it was being traced, is the parent of the span the
/// call runs in, so the background work shows up under the request that started
/// it.
public abstract class AsyncTask implements Runnable, Future {
    private final String name;
    private final boolean returnsVoid;
    private final Span parent;
    /// The caller's server's tracer when there is no parent span to carry it --
    /// in particular the untraced marker of a server with tracing off.
    private final Tracer owner;
    private boolean done;
    private boolean cancelled;
    private boolean started;
    private Object value;
    private Throwable failure;
    /// Run once this completes, outside its lock; see [#whenDone].
    private java.util.List listeners;

    /// #### Parameters
    ///
    /// - `name`: what the span is called: the class and the method
    ///
    /// - `returnsVoid`: @param returnsVoid whether the method returns nothing, so a failure has
    /// nobody to be delivered to and is reported instead
    protected AsyncTask(String name, boolean returnsVoid) {
        this.name = name;
        this.returnsVoid = returnsVoid;
        this.parent = BackendAccess.get().captureParent();
        this.owner = BackendAccess.get().captureOwner();
    }

    /// Runs the method's body. Generated.
    protected abstract Object call() throws Exception;

    @Override
    public final void run() {
        synchronized (this) {
            if (cancelled || started) {
                return;
            }
            started = true;
        }
        Object result = null;
        Throwable error = null;
        AsyncTask chained = null;
        try {
            result = BackendAccess.get().inBackground(name, parent, owner, new Tracing.Work() {
                @Override
                public Object run(Span span) throws Exception {
                    return call();
                }
            });
            if (result instanceof AsyncTask && result != this //NOPMD CompareObjectsWithEquals - itself, by identity
                    && !((AsyncTask) result).isDone()) {
                // Another @Async call's pending future: finished when that one
                // is, instead of waiting for it here. Waiting held this worker,
                // and with the inner call queued on the same executor a
                // one-thread pool -- or enough concurrent outer calls on any
                // pool -- waited for itself for ever. Spring's interceptor does
                // block here; the result is the same either way.
                chained = (AsyncTask) result;
            } else if (result instanceof Future) {
                // Any other Future is awaited, as Spring awaits it.
                result = ((Future) result).get();
            }
        } catch (ExecutionException err) {
            error = err.getCause() != null ? err.getCause() : err;
        } catch (Throwable err) {
            error = err;
        }
        if (chained != null) {
            final AsyncTask inner = chained;
            inner.whenDone(new Runnable() {
                @Override
                public void run() {
                    adopt(inner);
                }
            });
            return;
        }
        java.util.List fire;
        synchronized (this) {
            value = result;
            failure = error;
            fire = completeLocked();
        }
        fire(fire);
        if (error != null && returnsVoid) {
            // Nobody holds a Future for a void method, so the failure goes to the
            // executor running this, which reports and counts it -- the only
            // place it is ever seen.
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            if (error instanceof Error) {
                throw (Error) error;
            }
            throw new RuntimeException(name + " failed: " + error, error);
        }
    }

    /// Ends a call whose virtual thread the server freed at shutdown before it
    /// finished: nothing will ever complete it otherwise, and a caller blocked
    /// in get() would wait for ever.
    public void abandon(String reason) {
        java.util.List fire;
        synchronized (this) {
            if (done) {
                return;
            }
            failure = new IllegalStateException(name + ": " + reason);
            fire = completeLocked();
        }
        fire(fire);
    }

    /// Marks this done and wakes its waiters; answers the listeners to run once
    /// the lock is released. Called under this object's lock.
    private java.util.List completeLocked() {
        done = true;
        notifyAll();
        java.util.List out = listeners;
        listeners = null;
        return out;
    }

    private static void fire(java.util.List listeners) {
        for (int iter = 0 ; listeners != null && iter < listeners.size() ; iter++) {
            ((Runnable) listeners.get(iter)).run();
        }
    }

    /// Runs `listener` once this completes -- at once when it already has.
    void whenDone(Runnable listener) {
        synchronized (this) {
            if (!done) {
                if (listeners == null) {
                    listeners = new java.util.ArrayList(1);
                }
                listeners.add(listener);
                return;
            }
        }
        listener.run();
    }

    /// Completes this with the outcome of `inner`, the pending call it returned.
    private void adopt(AsyncTask inner) {
        Object v = null;
        Throwable f = null;
        synchronized (inner) {
            if (inner.cancelled) {
                f = new java.util.concurrent.CancellationException(inner.name
                        + " was cancelled");
            } else {
                v = inner.value;
                f = inner.failure;
            }
        }
        java.util.List fire;
        synchronized (this) {
            if (done) {
                return;
            }
            value = v;
            failure = f;
            fire = completeLocked();
        }
        fire(fire);
    }

    /// Cancels the call if it has not started. A running call is never interrupted.
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        java.util.List fire;
        synchronized (this) {
            if (started || done) {
                return false;
            }
            cancelled = true;
            fire = completeLocked();
        }
        fire(fire);
        return true;
    }

    @Override
    public synchronized boolean isCancelled() {
        return cancelled;
    }

    @Override
    public synchronized boolean isDone() {
        return done;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        if (VirtualThread.isVirtual()) {
            awaitCooperatively(Long.MAX_VALUE);
            return completed();
        }
        synchronized (this) {
            while (!done) {
                wait();
            }
            return result();
        }
    }

    @Override
    public Object get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        long deadline = deadline(System.currentTimeMillis(), unit.toMillis(timeout));
        if (VirtualThread.isVirtual()) {
            if (!awaitCooperatively(deadline)) {
                throw new TimeoutException(name + " did not finish in time");
            }
            return completed();
        }
        synchronized (this) {
            while (!done) {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) {
                    throw new TimeoutException(name + " did not finish in time");
                }
                wait(left);
            }
            return result();
        }
    }

    /// `now + millis`, saturated at Long.MAX_VALUE -- every wait the backend
    /// takes from a caller goes through it (Future.get, the executor, scheduler
    /// and task shutdowns, a pool borrow), because each accepts a long. A wait
    /// that only ever takes `deadline - now` survives the overflow by
    /// two's-complement wraparound; one that COMPARES the clock to the deadline,
    /// as the virtual-thread wait does, gave up at once. Saturating here keeps
    /// every one of them correct whichever way it is written. toMillis() already
    /// saturates a huge timeout -- get(Long.MAX_VALUE, DAYS) -- to Long.MAX_VALUE,
    /// and adding the clock to that wrapped to a deadline in the past, so the
    /// longest possible wait timed out at once.
    public static long deadline(long now, long millis) {
        if (millis > 0 && now > Long.MAX_VALUE - millis) {
            return Long.MAX_VALUE;
        }
        return now + millis;
    }

    /// Waits on a virtual thread without blocking its host.
    ///
    /// A virtual thread that called wait() would block the OS thread under it,
    /// and every other virtual thread that host runs with it -- including,
    /// quite possibly, the virtual thread running the very task being waited
    /// for. With every host stuck that way the server stops: measured, 32
    /// concurrent requests each waiting on an @Async(thread = VIRTUAL) task
    /// wedged a native server within seconds. Yielding instead gives the host
    /// back after each check, OUTSIDE the monitor, so the task can finish.
    ///
    /// #### Returns
    ///
    /// false when the deadline passed first
    ///
    /// Each check naps rather than yields: a plain yield put the waiter straight
    /// back on its host's run ring, so the host polled with a zero timeout and
    /// resumed it again at once -- a whole core spent re-checking for as long as a
    /// slow task ran. The nap grows from a millisecond to 20, so a quick task is
    /// still seen almost at once and a slow one costs a few wake-ups a second.
    private boolean awaitCooperatively(long deadline) {
        long nap = 1;
        while (!isDone()) {
            long now = System.currentTimeMillis();
            if (now >= deadline) {
                return false;
            }
            BackendAccess.get().napUntil(Math.min(deadline, now + nap));
            nap = Math.min(20, nap * 2);
        }
        return true;
    }

    private synchronized Object completed() throws ExecutionException {
        return result();
    }

    private Object result() throws ExecutionException {
        if (cancelled) {
            throw new java.util.concurrent.CancellationException(name + " was cancelled");
        }
        if (failure != null) {
            throw new ExecutionException(failure);
        }
        return value;
    }

    @Override
    public String toString() {
        return name;
    }
}
