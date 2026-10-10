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

import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/// The [ScheduledExecutorService] that [Executors] hands out: a timer that
/// passes each piece of work, when it is due, to an ordinary executor.
///
/// #### After `shutdown()`
///
/// As a JDK `ScheduledThreadPoolExecutor` with its default policies: work
/// already scheduled to run once still runs when it is due, and its future
/// completes; work that repeats is cancelled; nothing new is accepted. The
/// executor is terminated once the last of the accepted work has run.
/// `shutdownNow()` drops what has not started.
///
/// Not provided: the two policies themselves
/// (`setExecuteExistingDelayedTasksAfterShutdownPolicy` and its periodic
/// twin are methods of the JDK's class, not of the interface this is handed
/// out as).
final class ScheduledPool implements ScheduledExecutorService {

    private static final Scheduled<?>[] NONE = new Scheduled<?>[0];

    private final ExecutorService pool;
    private final Timer timer = new Timer();
    private final AtomicBoolean shutdown = new AtomicBoolean();
    /// Set once the timer and the pool have been told to stop.
    private final AtomicBoolean stopped = new AtomicBoolean();
    /// The scheduled work that has not completed, which is what an orderly
    /// shutdown waits for (the work that runs once) or cancels (the work
    /// that repeats). Replaced, never changed: this is read and written by
    /// the timer's thread, the workers and the caller's.
    private final AtomicReference<Scheduled<?>[]> live = new AtomicReference<Scheduled<?>[]>(NONE);

    ScheduledPool(ExecutorService pool) {
        this.pool = pool;
    }

    private final class Scheduled<V> extends TimerTask implements ScheduledFuture<V> {
        private final Callable<V> work;
        private final boolean periodic;
        /// The gap to keep after a run ends, or zero for a fixed rate.
        private final long delayAfter;
        private final AtomicLong due;
        private final AtomicBoolean running = new AtomicBoolean();
        private final CompletableFuture<V> outcome = new CompletableFuture<V>();

        Scheduled(Callable<V> work, long dueAt, boolean periodic, long delayAfter) {
            this.work = work;
            this.periodic = periodic;
            this.delayAfter = delayAfter;
            this.due = new AtomicLong(dueAt);
        }

        @Override
        public void run() {
            // Work that runs once and was accepted before a shutdown still
            // runs; work that repeats does not.
            if (outcome.isDone() || (periodic && shutdown.get()) || stopped.get()) {
                super.cancel();
                return;
            }
            if (!running.compareAndSet(false, true)) {
                // The run before this one has not ended; a fixed rate does
                // not pile runs up.
                return;
            }
            try {
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        once();
                    }
                });
            } catch (RuntimeException e) {
                running.set(false);
                outcome.completeExceptionally(e);
                super.cancel();
            }
        }

        private void once() {
            try {
                V value;
                try {
                    value = work.call();
                } catch (Throwable t) {
                    // Work that fails is not run again.
                    outcome.completeExceptionally(t);
                    Scheduled.super.cancel();
                    return;
                }
                if (!periodic) {
                    outcome.complete(value);
                } else if (delayAfter > 0 && !outcome.isDone() && !shutdown.get()) {
                    again();
                }
            } finally {
                running.set(false);
            }
        }

        private void again() {
            final Scheduled<V> self = this;
            due.set(System.currentTimeMillis() + delayAfter);
            try {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        self.run();
                    }
                }, delayAfter);
            } catch (IllegalStateException e) {
                // The timer was cancelled by a shutdown in between.
                outcome.cancel(false);
            }
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(due.get() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            super.cancel();
            return outcome.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return outcome.isCancelled();
        }

        @Override
        public boolean isDone() {
            return outcome.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return outcome.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return outcome.get(timeout, unit);
        }
    }

    /// Keeps `task` until it completes, and schedules it with `start`.
    private <V> Scheduled<V> accept(final Scheduled<V> task, Runnable start) {
        checkOpen();
        while (true) {
            Scheduled<?>[] before = live.get();
            Scheduled<?>[] after = new Scheduled<?>[before.length + 1];
            System.arraycopy(before, 0, after, 0, before.length);
            after[before.length] = task;
            if (live.compareAndSet(before, after)) {
                break;
            }
        }
        task.outcome.whenComplete(new BiConsumer<V, Throwable>() {
            @Override
            public void accept(V value, Throwable failure) {
                forget(task);
            }
        });
        if (shutdown.get()) {
            // A shutdown came in between: this one was not accepted.
            task.cancel(false);
            throw new RejectedExecutionException("The executor has been shut down");
        }
        try {
            start.run();
        } catch (IllegalStateException e) {
            // The timer was cancelled by a shutdown in between.
            task.cancel(false);
            throw new RejectedExecutionException("The executor has been shut down");
        }
        return task;
    }

    private void forget(Scheduled<?> task) {
        while (true) {
            Scheduled<?>[] before = live.get();
            int at = -1;
            for (int i = 0; i < before.length; i++) {
                if (before[i] == task) {
                    at = i;
                }
            }
            if (at < 0) {
                break;
            }
            Scheduled<?>[] after = new Scheduled<?>[before.length - 1];
            System.arraycopy(before, 0, after, 0, at);
            System.arraycopy(before, at + 1, after, at, after.length - at);
            if (live.compareAndSet(before, after.length == 0 ? NONE : after)) {
                break;
            }
        }
        stopWhenIdle();
    }

    /// After a shutdown, stops the timer and the pool once nothing accepted
    /// is left to run.
    private void stopWhenIdle() {
        if (shutdown.get() && live.get().length == 0 && stopped.compareAndSet(false, true)) {
            timer.cancel();
            pool.shutdown();
        }
    }

    private void checkOpen() {
        if (shutdown.get()) {
            throw new RejectedExecutionException("The executor has been shut down");
        }
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        if (callable == null || unit == null) {
            throw new NullPointerException();
        }
        final long millis = Math.max(0, unit.toMillis(delay));
        final Scheduled<V> task = new Scheduled<V>(callable, System.currentTimeMillis() + millis, false, 0);
        return accept(task, new Runnable() {
            @Override
            public void run() {
                timer.schedule(task, millis);
            }
        });
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return schedule(Executors.callable(command, null), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }
        if (period <= 0) {
            throw new IllegalArgumentException();
        }
        final long millis = Math.max(0, unit.toMillis(initialDelay));
        final long every = Math.max(1, unit.toMillis(period));
        final Scheduled<Object> task = new Scheduled<Object>(Executors.callable(command, null),
                System.currentTimeMillis() + millis, true, 0);
        return accept(task, new Runnable() {
            @Override
            public void run() {
                timer.scheduleAtFixedRate(task, millis, every);
            }
        });
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
            TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }
        if (delay <= 0) {
            throw new IllegalArgumentException();
        }
        final long millis = Math.max(0, unit.toMillis(initialDelay));
        final Scheduled<Object> task = new Scheduled<Object>(Executors.callable(command, null),
                System.currentTimeMillis() + millis, true, Math.max(1, unit.toMillis(delay)));
        return accept(task, new Runnable() {
            @Override
            public void run() {
                timer.schedule(task, millis);
            }
        });
    }

    @Override
    public void execute(Runnable command) {
        // The pool itself stays open after a shutdown for as long as
        // delayed work is due to be handed to it, so it cannot be the one
        // to refuse.
        checkOpen();
        pool.execute(command);
    }

    @Override
    public void shutdown() {
        shutdown.set(true);
        for (Scheduled<?> task : live.get()) {
            if (task.periodic) {
                task.cancel(false);
            }
        }
        stopWhenIdle();
    }

    /// Nothing scheduled that has not started runs after this. The futures
    /// of the dropped work stay as they are, neither done nor cancelled,
    /// as the JDK leaves them.
    @Override
    public List<Runnable> shutdownNow() {
        shutdown.set(true);
        stopped.set(true);
        timer.cancel();
        return pool.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public boolean isTerminated() {
        return pool.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return pool.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        checkOpen();
        return pool.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        checkOpen();
        return pool.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        checkOpen();
        return pool.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        checkOpen();
        return pool.invokeAll(tasks);
    }
}
