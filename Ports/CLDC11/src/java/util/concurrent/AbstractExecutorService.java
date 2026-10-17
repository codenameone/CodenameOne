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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/// The [ExecutorService] methods that need nothing but `execute`: a
/// subclass provides execution and the life cycle, and gets `submit`,
/// `invokeAll` and `invokeAny` from here.
public abstract class AbstractExecutorService implements ExecutorService {

    public AbstractExecutorService() {
    }

    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<T>(runnable, value);
    }

    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<T>(callable);
    }

    public Future<?> submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<Void> ftask = newTaskFor(task, null);
        execute(ftask);
        return ftask;
    }

    public <T> Future<T> submit(Runnable task, T result) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<T> ftask = newTaskFor(task, result);
        execute(ftask);
        return ftask;
    }

    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<T> ftask = newTaskFor(task);
        execute(ftask);
        return ftask;
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
        boolean done = false;
        try {
            for (Callable<T> t : tasks) {
                RunnableFuture<T> f = newTaskFor(t);
                futures.add(f);
                execute(f);
            }
            for (int i = 0; i < futures.size(); i++) {
                Future<T> f = futures.get(i);
                if (!f.isDone()) {
                    try {
                        f.get();
                    } catch (CancellationException ignore) {
                        // The future reports it to whoever asks.
                    } catch (ExecutionException ignore) {
                        // The future reports it to whoever asks.
                    }
                }
            }
            done = true;
            return futures;
        } finally {
            if (!done) {
                cancelAll(futures);
            }
        }
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (tasks == null || unit == null) {
            throw new NullPointerException();
        }
        long deadline = BlockingWait.deadline(BlockingWait.millis(timeout, unit));
        List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
        boolean done = false;
        try {
            for (Callable<T> t : tasks) {
                futures.add(newTaskFor(t));
            }
            for (int i = 0; i < futures.size(); i++) {
                if (BlockingWait.now() >= deadline) {
                    return futures;
                }
                execute((Runnable) futures.get(i));
            }
            for (int i = 0; i < futures.size(); i++) {
                Future<T> f = futures.get(i);
                if (!f.isDone()) {
                    long left = deadline - BlockingWait.now();
                    if (left <= 0) {
                        return futures;
                    }
                    try {
                        f.get(left, TimeUnit.MILLISECONDS);
                    } catch (CancellationException ignore) {
                        // The future reports it to whoever asks.
                    } catch (ExecutionException ignore) {
                        // The future reports it to whoever asks.
                    } catch (TimeoutException timedOut) {
                        return futures;
                    }
                }
            }
            done = true;
            return futures;
        } finally {
            if (!done) {
                cancelAll(futures);
            }
        }
    }

    /// Cancels every future that is not done, the LAST first.
    ///
    /// The order matters. Workers take tasks in the order they were
    /// submitted, so cancelling from the front frees a worker -- a task
    /// cancelled before it started returns at once -- which then starts the
    /// next task in line before this loop has reached it: a task the caller
    /// was promised would not run, runs. Cancelling from the back means
    /// every task behind the one being cancelled is already cancelled.
    static <T> void cancelAll(List<Future<T>> futures) {
        for (int i = futures.size() - 1; i >= 0; i--) {
            futures.get(i).cancel(true);
        }
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        try {
            return doInvokeAny(tasks, BlockingWait.FOREVER);
        } catch (TimeoutException cannotHappen) {
            throw new ExecutionException(cannotHappen);
        }
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null) {
            throw new NullPointerException();
        }
        return doInvokeAny(tasks, BlockingWait.deadline(BlockingWait.millis(timeout, unit)));
    }

    private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks, long deadline)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        int n = tasks.size();
        if (n == 0) {
            throw new IllegalArgumentException();
        }
        Finished finished = new Finished();
        List<Future<T>> futures = new ArrayList<Future<T>>(n);
        try {
            for (Callable<T> t : tasks) {
                if (t == null) {
                    throw new NullPointerException();
                }
                AnyTask<T> f = new AnyTask<T>(t, finished);
                futures.add(f);
                execute(f);
            }
            ExecutionException last = null;
            for (int seen = 0; seen < n; seen++) {
                Future<?> f = finished.next(deadline);
                if (f == null) {
                    throw new TimeoutException();
                }
                try {
                    @SuppressWarnings("unchecked")
                    T value = ((Future<T>) f).get();
                    return value;
                } catch (ExecutionException e) {
                    last = e;
                } catch (CancellationException e) {
                    last = new ExecutionException(e);
                }
            }
            throw last;
        } finally {
            cancelAll(futures);
        }
    }

    /// The futures of an `invokeAny` in the order they completed.
    private static final class Finished {
        private final List<Future<?>> queue = new ArrayList<Future<?>>();

        void add(Future<?> f) {
            synchronized (this) {
                queue.add(f);
                notifyAll();
            }
        }

        /// The next completed future, or null when none completed by `deadline`.
        Future<?> next(long deadline) throws InterruptedException {
            synchronized (this) {
                if (!queue.isEmpty()) {
                    return queue.remove(0);
                }
            }
            new Wait(this, deadline).await();
            synchronized (this) {
                return queue.isEmpty() ? null : queue.remove(0);
            }
        }

        boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    private static final class Wait extends BlockingWait {
        private final Finished finished;

        Wait(Finished finished, long deadline) {
            super(finished, deadline);
            this.finished = finished;
        }

        boolean ready() {
            return !finished.isEmpty();
        }
    }

    private static final class AnyTask<T> extends FutureTask<T> {
        private final Finished finished;

        AnyTask(Callable<T> c, Finished finished) {
            super(c);
            this.finished = finished;
        }

        protected void done() {
            finished.add(this);
        }
    }
}
