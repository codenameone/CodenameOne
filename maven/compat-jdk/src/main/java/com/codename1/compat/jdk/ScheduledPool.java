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

/// The [ScheduledExecutorService] that [Executors] hands out: a timer that
/// passes each piece of work, when it is due, to an ordinary executor.
final class ScheduledPool implements ScheduledExecutorService {

    private final ExecutorService pool;
    private final Timer timer = new Timer();
    private final AtomicBoolean shutdown = new AtomicBoolean();

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
            if (outcome.isDone() || shutdown.get()) {
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
        checkOpen();
        long millis = Math.max(0, unit.toMillis(delay));
        Scheduled<V> task = new Scheduled<V>(callable, System.currentTimeMillis() + millis, false, 0);
        timer.schedule(task, millis);
        return task;
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
        checkOpen();
        long millis = Math.max(0, unit.toMillis(initialDelay));
        Scheduled<Object> task = new Scheduled<Object>(Executors.callable(command, null),
                System.currentTimeMillis() + millis, true, 0);
        timer.scheduleAtFixedRate(task, millis, Math.max(1, unit.toMillis(period)));
        return task;
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
        checkOpen();
        long millis = Math.max(0, unit.toMillis(initialDelay));
        Scheduled<Object> task = new Scheduled<Object>(Executors.callable(command, null),
                System.currentTimeMillis() + millis, true, Math.max(1, unit.toMillis(delay)));
        timer.schedule(task, millis);
        return task;
    }

    @Override
    public void execute(Runnable command) {
        pool.execute(command);
    }

    @Override
    public void shutdown() {
        shutdown.set(true);
        timer.cancel();
        pool.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown.set(true);
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
        return pool.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return pool.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return pool.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return pool.invokeAll(tasks);
    }
}
