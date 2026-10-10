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
package com.codename1.compat.jdk;

import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.EasyThread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/// `java.util.concurrent.Executors` for the Codename One runtime: executor
/// services that run their tasks on Codename One background threads.
///
/// #### Where a task runs
///
/// Never on the event dispatch thread. A task that touches the user
/// interface has to hand that part back, the way desktop code does with
/// `SwingUtilities.invokeLater` or `Platform.runLater`.
///
/// Each worker is a `com.codename1.util.EasyThread`, started when a task
/// first needs it. Tasks are dealt to the workers in turn, so a pool of one
/// runs them strictly in the order they were submitted, and a larger pool
/// runs as many at once as it has workers -- but a long task delays the ones
/// dealt to the same worker behind it even while another worker is idle.
///
/// #### Waiting for a result
///
/// `Future.get()` blocks the caller until the task is done. Called on the
/// event dispatch thread it does so through Codename One's
/// `invokeAndBlock`, which keeps the user interface responsive while it
/// waits. A running task cannot be interrupted: `cancel(true)` on one that
/// has started answers false.
///
/// The scheduled services, the work stealing pool and the overloads that
/// take a `ThreadFactory` are not provided.
public final class Executors {

    /// How many workers [#newCachedThreadPool()] deals its tasks to. The JDK
    /// pool grows without limit; a device has neither the threads nor the
    /// use for that.
    private static final int CACHED_POOL_WORKERS = 4;

    private Executors() {
    }

    public static ExecutorService newSingleThreadExecutor() {
        return new Pool(1);
    }

    public static ExecutorService newFixedThreadPool(int nThreads) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException();
        }
        return new Pool(nThreads);
    }

    /// A pool of a few workers; see `CACHED_POOL_WORKERS` in the source and
    /// the class description.
    public static ExecutorService newCachedThreadPool() {
        return new Pool(CACHED_POOL_WORKERS);
    }

    /// A `Callable` that runs `task` and answers `result`.
    public static <T> Callable<T> callable(final Runnable task, final T result) {
        if (task == null) {
            throw new NullPointerException();
        }
        return new Callable<T>() {
            @Override
            public T call() {
                task.run();
                return result;
            }
        };
    }

    public static Callable<Object> callable(Runnable task) {
        return callable(task, null);
    }

    /// One accepted task and whether it has been started or dropped. The
    /// state is the one thing two threads decide between -- the worker that
    /// would run the task and the caller of `shutdownNow` that would drop it
    /// -- so it is an atomic.
    private static final class Job implements Runnable {
        private static final int QUEUED = 0;
        private static final int STARTED = 1;
        private static final int DROPPED = 2;

        private final Runnable task;
        private final AtomicInteger state = new AtomicInteger(QUEUED);

        Job(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            if (state.compareAndSet(QUEUED, STARTED)) {
                task.run();
            }
        }

        boolean drop() {
            return state.compareAndSet(QUEUED, DROPPED);
        }

        boolean isQueued() {
            return state.get() == QUEUED;
        }
    }

    private static final class Pool implements ExecutorService {
        private final EasyThread[] workers;
        private final AtomicBoolean shutdown = new AtomicBoolean();
        /// Jobs that may not have started yet, for `shutdownNow` to drop.
        private final List<Job> outstanding = new ArrayList<Job>();
        private int next;

        Pool(int size) {
            workers = new EasyThread[size];
        }

        @Override
        public void execute(Runnable command) {
            if (command == null) {
                throw new NullPointerException();
            }
            if (shutdown.get()) {
                throw new RejectedExecutionException("The executor has been shut down");
            }
            // Forget the jobs that have started since the last call.
            for (int i = outstanding.size() - 1; i >= 0; i--) {
                if (!outstanding.get(i).isQueued()) {
                    outstanding.remove(i);
                }
            }
            Job job = new Job(command);
            outstanding.add(job);
            int slot = next;
            next = (next + 1) % workers.length;
            if (workers[slot] == null) {
                workers[slot] = EasyThread.start("cn1-executor-" + slot);
            }
            workers[slot].run(job);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            if (task == null) {
                throw new NullPointerException();
            }
            Task<T> future = new Task<T>(task);
            execute(future);
            return future;
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return submit(callable(task, result));
        }

        @Override
        public Future<?> submit(Runnable task) {
            return submit(callable(task, null));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
            for (Callable<T> task : tasks) {
                futures.add(submit(task));
            }
            for (Future<T> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    // The future holds the failure for whoever asks it.
                    continue;
                } catch (CancellationException e) {
                    continue;
                }
            }
            return futures;
        }

        @Override
        public void shutdown() {
            shutdown.set(true);
            for (EasyThread worker : workers) {
                if (worker != null) {
                    worker.killWhenIdle();
                }
            }
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown.set(true);
            List<Runnable> dropped = new ArrayList<Runnable>();
            for (Job job : outstanding) {
                if (job.drop()) {
                    if (job.task instanceof Task) {
                        ((Task<?>) job.task).cancel(false);
                    }
                    dropped.add(job.task);
                }
            }
            outstanding.clear();
            for (EasyThread worker : workers) {
                if (worker != null) {
                    worker.killWhenIdle();
                }
            }
            return dropped;
        }

        @Override
        public boolean isShutdown() {
            return shutdown.get();
        }

        @Override
        public boolean isTerminated() {
            if (!shutdown.get()) {
                return false;
            }
            for (EasyThread worker : workers) {
                if (worker != null && !worker.isFinished()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            final long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
            if (Display.isInitialized() && Display.getInstance().isEdt()) {
                // The event dispatch thread must not sleep: wait off it.
                Display.getInstance().invokeAndBlock(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            poll(deadline);
                        } catch (InterruptedException e) {
                            // The caller reads the outcome from isTerminated.
                            return;
                        }
                    }
                });
            } else {
                poll(deadline);
            }
            return isTerminated();
        }

        private void poll(long deadline) throws InterruptedException {
            while (!isTerminated() && System.currentTimeMillis() < deadline) {
                Thread.sleep(5);
            }
        }
    }

    /// The future of one submitted task. `AsyncResource` holds the outcome
    /// and does the waiting, including the wait that keeps the event
    /// dispatch thread alive.
    private static final class Task<V> implements Future<V>, Runnable {
        private final Callable<V> task;
        private final AsyncResource<V> outcome = new AsyncResource<V>();
        private final AtomicBoolean claimed = new AtomicBoolean();
        private final AtomicBoolean cancelled = new AtomicBoolean();

        Task(Callable<V> task) {
            this.task = task;
        }

        @Override
        public void run() {
            if (!claimed.compareAndSet(false, true)) {
                return;
            }
            V value;
            try {
                value = task.call();
            } catch (Throwable t) {
                outcome.error(t);
                return;
            }
            outcome.complete(value);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (!claimed.compareAndSet(false, true)) {
                return false;
            }
            cancelled.set(true);
            outcome.cancel(false);
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public boolean isDone() {
            return outcome.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            if (cancelled.get()) {
                throw new CancellationException();
            }
            try {
                return outcome.get();
            } catch (AsyncResource.AsyncExecutionException e) {
                throw failure(e);
            }
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (cancelled.get()) {
                throw new CancellationException();
            }
            long millis = unit.toMillis(timeout);
            if (!outcome.isDone() && millis <= 0) {
                throw new TimeoutException();
            }
            try {
                // AsyncResource reads a timeout of zero or less as "forever".
                return outcome.get((int) Math.max(1, Math.min(millis, Integer.MAX_VALUE)));
            } catch (AsyncResource.AsyncExecutionException e) {
                throw failure(e);
            } catch (InterruptedException e) {
                // AsyncResource reports running out of time this way.
                if (!outcome.isDone()) {
                    throw new TimeoutException();
                }
                throw e;
            }
        }

        private ExecutionException failure(AsyncResource.AsyncExecutionException e) {
            Throwable cause = e.getCause();
            if (cancelled.get() || AsyncResource.isCancelled(cause)) {
                throw new CancellationException();
            }
            return new ExecutionException(cause);
        }
    }

    // ------------------------------------------------------------------
    // The overloads that take a thread factory, and the executors that
    // schedule. A factory is accepted and not asked for a thread: the work
    // runs on Codename One's own threads.
    // ------------------------------------------------------------------

    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        requireFactory(threadFactory);
        return newSingleThreadExecutor();
    }

    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        requireFactory(threadFactory);
        return newFixedThreadPool(nThreads);
    }

    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        requireFactory(threadFactory);
        return newCachedThreadPool();
    }

    /// A pool for work that splits itself up: here, a fixed pool.
    public static ExecutorService newWorkStealingPool() {
        return new Pool(CACHED_POOL_WORKERS);
    }

    public static ExecutorService newWorkStealingPool(int parallelism) {
        return newFixedThreadPool(parallelism);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException();
        }
        return new ScheduledPool(new Pool(Math.max(1, corePoolSize)));
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        requireFactory(threadFactory);
        return newScheduledThreadPool(corePoolSize);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return new ScheduledPool(new Pool(1));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        requireFactory(threadFactory);
        return newSingleThreadScheduledExecutor();
    }

    /// The factory the JDK's executors use by default: a plain thread.
    public static ThreadFactory defaultThreadFactory() {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        };
    }

    private static void requireFactory(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }
    }
}
