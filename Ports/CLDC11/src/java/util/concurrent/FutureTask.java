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

/// A cancellable computation whose result is read through [Future].
///
/// The state lives under this object's monitor, so it may be run, cancelled
/// and waited for from any number of threads. `get()` on the UI thread waits
/// through `JdkHost.blockUi`, which keeps events being dispatched.
///
/// `cancel(true)` interrupts the thread running the task, as in the JDK.
public class FutureTask<V> implements RunnableFuture<V> {
    private static final int NEW = 0;
    private static final int NORMAL = 1;
    private static final int EXCEPTIONAL = 2;
    private static final int CANCELLED = 3;

    /// Null once the task has run, so that it does not hold what it captured.
    private Callable<V> callable;
    private int state;
    private Object outcome;
    /// The thread inside [#run], so that `cancel(true)` can interrupt it.
    private Thread runner;

    public FutureTask(Callable<V> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        this.callable = callable;
    }

    public FutureTask(Runnable runnable, V result) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        this.callable = Executors.callable(runnable, result);
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (this) {
            if (state != NEW) {
                return false;
            }
            state = CANCELLED;
            callable = null;
            if (mayInterruptIfRunning && runner != null) {
                runner.interrupt();
            }
            notifyAll();
        }
        done();
        return true;
    }

    public boolean isCancelled() {
        synchronized (this) {
            return state == CANCELLED;
        }
    }

    public boolean isDone() {
        synchronized (this) {
            return state != NEW;
        }
    }

    /// Called once when the task becomes done, by whichever thread made it
    /// so: completion, failure or cancellation. Does nothing here.
    protected void done() {
    }

    /// Completes the future with `v`, unless it is already done.
    protected void set(V v) {
        if (finish(NORMAL, v)) {
            done();
        }
    }

    /// Fails the future with `t`, unless it is already done.
    protected void setException(Throwable t) {
        if (finish(EXCEPTIONAL, t)) {
            done();
        }
    }

    private boolean finish(int to, Object value) {
        synchronized (this) {
            if (state != NEW) {
                return false;
            }
            state = to;
            outcome = value;
            callable = null;
            notifyAll();
            return true;
        }
    }

    public V get() throws InterruptedException, ExecutionException {
        if (!isDone()) {
            new Wait(this, BlockingWait.FOREVER).await();
        }
        return report();
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null) {
            throw new NullPointerException();
        }
        if (!isDone()) {
            new Wait(this, BlockingWait.deadline(unit.toMillis(timeout))).await();
            if (!isDone()) {
                throw new TimeoutException();
            }
        }
        return report();
    }

    @SuppressWarnings("unchecked")
    private V report() throws ExecutionException {
        int s;
        Object o;
        synchronized (this) {
            s = state;
            o = outcome;
        }
        if (s == CANCELLED) {
            throw new CancellationException();
        }
        if (s == EXCEPTIONAL) {
            throw new ExecutionException((Throwable) o);
        }
        return (V) o;
    }

    public void run() {
        Callable<V> c = claim();
        if (c == null) {
            return;
        }
        V v;
        try {
            v = c.call();
        } catch (Throwable t) {
            release();
            setException(t);
            return;
        }
        release();
        set(v);
    }

    /// Runs the task without setting a result, leaving the future ready to
    /// run again: for work that repeats. False when the task failed, was
    /// cancelled, or was not runnable.
    protected boolean runAndReset() {
        Callable<V> c = claim();
        if (c == null) {
            return false;
        }
        try {
            c.call();
        } catch (Throwable t) {
            release();
            setException(t);
            return false;
        }
        release();
        return !isDone();
    }

    /// The callable, with this thread recorded as its runner; null when the
    /// task is done or another thread is running it.
    private Callable<V> claim() {
        synchronized (this) {
            if (state != NEW || runner != null) {
                return null;
            }
            runner = Thread.currentThread();
            return callable;
        }
    }

    /// Forgets the runner. A cancel(true) that arrived during the run has
    /// interrupted this thread; that interrupt is meant for the task, so it
    /// is consumed here rather than leaking into the next task the thread runs.
    private void release() {
        boolean cancelled;
        synchronized (this) {
            runner = null;
            cancelled = state == CANCELLED;
        }
        if (cancelled) {
            Thread.interrupted();
        }
    }

    public String toString() {
        String status;
        synchronized (this) {
            switch (state) {
                case NORMAL:
                    status = "[Completed normally]";
                    break;
                case EXCEPTIONAL:
                    status = "[Completed exceptionally: " + outcome + "]";
                    break;
                case CANCELLED:
                    status = "[Cancelled]";
                    break;
                default:
                    status = "[Not completed]";
                    break;
            }
        }
        return super.toString() + status;
    }

    /// Waits for the task to be done.
    private static final class Wait extends BlockingWait {
        private final FutureTask<?> task;

        Wait(FutureTask<?> task, long deadline) {
            super(task, deadline);
            this.task = task;
        }

        boolean ready() {
            return task.state != NEW;
        }
    }
}
