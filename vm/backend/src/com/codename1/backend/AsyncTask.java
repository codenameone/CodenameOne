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
package com.codename1.backend;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * One call of an {@code @Async} method, and the {@link Future} its caller holds.
 *
 * <p>The build generates a subclass per {@code @Async} method whose fields are the
 * call's arguments and whose {@link #call} invokes the method's original body.
 * The rewritten method constructs one, hands it to its executor and returns it,
 * so the caller's {@code Future} is this object. A method whose body itself
 * returns a Future -- {@link AsyncResult#of} -- completes this one with that
 * Future's value.
 *
 * <p>The caller's span, if it was being traced, is the parent of the span the
 * call runs in, so the background work shows up under the request that started
 * it.
 */
public abstract class AsyncTask implements Runnable, Future {
    private final String name;
    private final boolean returnsVoid;
    private final Span parent;
    private boolean done;
    private boolean cancelled;
    private boolean started;
    private Object value;
    private Throwable failure;

    /**
     * @param name what the span is called: the class and the method
     * @param returnsVoid whether the method returns nothing, so a failure has
     *        nobody to be delivered to and is reported instead
     */
    protected AsyncTask(String name, boolean returnsVoid) {
        this.name = name;
        this.returnsVoid = returnsVoid;
        this.parent = Tracing.captureParent();
    }

    /** Runs the method's body. Generated. */
    protected abstract Object call() throws Exception;

    public final void run() {
        synchronized(this) {
            if(cancelled || started) {
                return;
            }
            started = true;
        }
        Object result = null;
        Throwable error = null;
        try {
            result = Tracing.inBackground(name, parent, new Tracing.Work() {
                public Object run(Span span) throws Exception {
                    return call();
                }
            });
            if(result instanceof Future) {
                result = ((Future)result).get();
            }
        } catch (ExecutionException err) {
            error = err.getCause() != null ? err.getCause() : err;
        } catch (Throwable err) {
            error = err;
        }
        synchronized(this) {
            value = result;
            failure = error;
            done = true;
            notifyAll();
        }
        if(error != null && returnsVoid) {
            // Nobody holds a Future for a void method, so the failure goes to the
            // executor running this, which reports and counts it -- the only
            // place it is ever seen.
            if(error instanceof RuntimeException) {
                throw (RuntimeException)error;
            }
            if(error instanceof Error) {
                throw (Error)error;
            }
            throw new RuntimeException(name + " failed: " + error, error);
        }
    }

    /** Cancels the call if it has not started. A running call is never interrupted. */
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if(started || done) {
            return false;
        }
        cancelled = true;
        done = true;
        notifyAll();
        return true;
    }

    public synchronized boolean isCancelled() {
        return cancelled;
    }

    public synchronized boolean isDone() {
        return done;
    }

    public Object get() throws InterruptedException, ExecutionException {
        if(VirtualThread.isVirtual()) {
            awaitCooperatively(Long.MAX_VALUE);
            return completed();
        }
        synchronized(this) {
            while(!done) {
                wait();
            }
            return result();
        }
    }

    public Object get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        if(VirtualThread.isVirtual()) {
            if(!awaitCooperatively(deadline)) {
                throw new TimeoutException(name + " did not finish in time");
            }
            return completed();
        }
        synchronized(this) {
            while(!done) {
                long left = deadline - System.currentTimeMillis();
                if(left <= 0) {
                    throw new TimeoutException(name + " did not finish in time");
                }
                wait(left);
            }
            return result();
        }
    }

    /**
     * Waits on a virtual thread without blocking its host.
     *
     * A virtual thread that called wait() would block the OS thread under it,
     * and every other virtual thread that host runs with it -- including,
     * quite possibly, the virtual thread running the very task being waited
     * for. With every host stuck that way the server stops: measured, 32
     * concurrent requests each waiting on an @Async(thread = VIRTUAL) task
     * wedged a native server within seconds. Yielding instead gives the host
     * back after each check, OUTSIDE the monitor, so the task can finish.
     *
     * @return false when the deadline passed first
     */
    private boolean awaitCooperatively(long deadline) {
        while(!isDone()) {
            if(System.currentTimeMillis() >= deadline) {
                return false;
            }
            VirtualThread.yieldNow();
        }
        return true;
    }

    private synchronized Object completed() throws ExecutionException {
        return result();
    }

    private Object result() throws ExecutionException {
        if(cancelled) {
            throw new java.util.concurrent.CancellationException(name + " was cancelled");
        }
        if(failure != null) {
            throw new ExecutionException(failure);
        }
        return value;
    }

    public String toString() {
        return name;
    }
}
