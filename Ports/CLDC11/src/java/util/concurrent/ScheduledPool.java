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

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
final class ScheduledPool extends AbstractExecutorService implements ScheduledExecutorService {

    private static final Scheduled<?>[] NONE = new Scheduled<?>[0];

    private final ExecutorService pool;
    private final AtomicBoolean shutdown = new AtomicBoolean();
    /// Set once the pool has been told to stop and nothing waits any more.
    private final AtomicBoolean stopped = new AtomicBoolean();
    /// The scheduled work that has not completed, which is what an orderly
    /// shutdown waits for (the work that runs once) or cancels (the work
    /// that repeats). Replaced, never changed: this is read and written by
    /// the timer's thread, the workers and the caller's.
    private final AtomicReference<Scheduled<?>[]> live = new AtomicReference<Scheduled<?>[]>(NONE);

    ScheduledPool(ExecutorService pool) {
        this.pool = pool;
    }

    private final class Scheduled<V> implements Runnable, ScheduledFuture<V> {
        private final Callable<V> work;
        private final boolean periodic;
        /// The gap to keep after a run ends, or zero for a fixed rate.
        private final long delayAfter;
        /// The period of a fixed rate, or zero.
        private final long rate;
        /// The wait this is in, in [Delays], or null: see [#arm].
        private final AtomicReference<Delays.Handle> wait = new AtomicReference<Delays.Handle>();
        private final AtomicLong due;
        private final AtomicBoolean running = new AtomicBoolean();
        /// The worker inside the work right now, for cancel(true) to interrupt.
        private final AtomicReference<Thread> runner = new AtomicReference<Thread>();
        /// A fixed rate's firings that have come and not yet run, counting
        /// the one running now. See [#run].
        private final AtomicInteger owed = new AtomicInteger();
        private final CompletableFuture<V> outcome = new CompletableFuture<V>();

        Scheduled(Callable<V> work, long dueAt, boolean periodic, long delayAfter, long rate) {
            this.work = work;
            this.periodic = periodic;
            this.delayAfter = delayAfter;
            this.rate = rate;
            this.due = new AtomicLong(dueAt);
        }

        @Override
        public void run() {
            // Work that runs once and was accepted before a shutdown still
            // runs; work that repeats does not.
            if (outcome.isDone() || (periodic && shutdown.get()) || stopped.get()) {
                disarm();
                return;
            }
            if (rate > 0) {
                // A fixed rate keeps its beat whatever a run takes.
                due.set(due.get() + rate);
                arm(due.get() - BlockingWait.now());
                // A firing that comes while a run is under way is owed, not
                // dropped: as with the JDK's scheduleAtFixedRate, an overrun
                // makes the next runs start late and one after another,
                // never at once. The worker already running takes it.
                if (owed.getAndIncrement() > 0) {
                    return;
                }
            } else if (!running.compareAndSet(false, true)) {
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
                owed.set(0);
                outcome.completeExceptionally(e);
                disarm();
            }
        }

        /// Waits `millis` and then runs. The handle is kept so that a
        /// cancelled task leaves the queue at once.
        void arm(long millis) {
            Delays.Handle next = Delays.after(millis, this);
            Delays.Handle before = wait.getAndSet(next);
            if (before != null) {
                if (before.dueAt() > next.dueAt()) {
                    // The wait armed here fired and re-armed before this thread
                    // stored it: the first arm, from the scheduling thread, with
                    // a delay of zero is the case. The later wait is the live one,
                    // and cancelling it would stop a fixed rate after one run.
                    // A failed swap means it was re-armed or disarmed meanwhile,
                    // and what is stored now is newer still. Either way the
                    // left-over wait, when it fires, finds the run done or re-arms
                    // as the live chain would.
                    wait.compareAndSet(next, before);
                } else {
                    before.cancel();
                }
            }
            if (outcome.isDone() || stopped.get()) {
                // Cancelled, or stopped, while this was being armed.
                disarm();
            }
        }

        void disarm() {
            Delays.Handle before = wait.getAndSet(null);
            if (before != null) {
                before.cancel();
            }
        }

        /// A fixed rate runs every firing it owes, in turn, on this worker.
        private void once() {
            if (rate == 0) {
                step();
                return;
            }
            do {
                running.set(true);
                if (!step()) {
                    return;
                }
            } while (owed.decrementAndGet() > 0);
        }

        /// One run; false when the work is not to run again.
        private boolean step() {
            // Cancelled, or stopped, while it waited for a worker: a future
            // that answers isCancelled() must never start its work after.
            if (outcome.isDone() || (periodic && shutdown.get()) || stopped.get()) {
                running.set(false);
                disarm();
                return false;
            }
            boolean rearm = false;
            try {
                V value;
                runner.set(Thread.currentThread());
                try {
                    value = work.call();
                } catch (Throwable t) {
                    // Work that fails is not run again.
                    outcome.completeExceptionally(t);
                    disarm();
                    return false;
                }
                if (!periodic) {
                    outcome.complete(value);
                } else if (delayAfter > 0 && !outcome.isDone() && !shutdown.get()) {
                    rearm = true;
                }
            } finally {
                runner.set(null);
                if (outcome.isCancelled()) {
                    // An interrupt cancel(true) aimed at this work is not handed on
                    // to whatever task the worker runs next.
                    Thread.interrupted();
                }
                running.set(false);
            }
            if (rearm) {
                // Only once the flag is down. Armed before it, a short delay
                // let the timer fire while this run still held the flag: the
                // next run saw a run in progress and returned, nothing armed
                // another, and the task stopped for good with its future
                // never completing.
                again();
            }
            return !outcome.isDone();
        }

        private void again() {
            due.set(BlockingWait.now() + delayAfter);
            arm(delayAfter);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(due.get() - BlockingWait.now(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this) {
                return 0;
            }
            long mine = getDelay(TimeUnit.MILLISECONDS);
            long theirs = other.getDelay(TimeUnit.MILLISECONDS);
            return mine < theirs ? -1 : (mine == theirs ? 0 : 1);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            disarm();
            boolean cancelled = outcome.cancel(mayInterruptIfRunning);
            if (cancelled && mayInterruptIfRunning) {
                // The future cannot reach the work on its own: interrupt the worker
                // running it, as FutureTask does. The worker clears that interrupt
                // when the cancelled work returns.
                Thread t = runner.get();
                if (t != null) {
                    t.interrupt();
                }
            }
            return cancelled;
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

    /// Keeps `task` until it completes, and runs it after `millis`.
    private <V> Scheduled<V> accept(final Scheduled<V> task, long millis) {
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
        task.arm(millis);
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
        final long millis = Math.max(0, BlockingWait.millis(delay, unit));
        final Scheduled<V> task = new Scheduled<V>(callable, BlockingWait.now() + millis, false, 0, 0);
        return accept(task, millis);
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
        final long millis = Math.max(0, BlockingWait.millis(initialDelay, unit));
        final long every = Math.max(1, BlockingWait.millis(period, unit));
        final Scheduled<Object> task = new Scheduled<Object>(Executors.callable(command, null),
                BlockingWait.now() + millis, true, 0, every);
        return accept(task, millis);
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
        final long millis = Math.max(0, BlockingWait.millis(initialDelay, unit));
        final Scheduled<Object> task = new Scheduled<Object>(Executors.callable(command, null),
                BlockingWait.now() + millis, true, Math.max(1, BlockingWait.millis(delay, unit)), 0);
        return accept(task, millis);
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
    ///
    /// What is answered is all of it, as the contract of the interface
    /// has it: the work the pool had been handed and had not begun, and
    /// the work that was still waiting for its time, which the pool never
    /// saw. The JDK answers its delayed tasks the same way, as the futures
    /// `schedule` returned.
    @Override
    public List<Runnable> shutdownNow() {
        shutdown.set(true);
        stopped.set(true);
        List<Runnable> dropped = new ArrayList<Runnable>(pool.shutdownNow());
        // Taken out of `live` here: a dropped task's future never completes,
        // so the forget() its completion would run never comes, and the
        // pool would hold every dropped task (and what its work holds) for
        // as long as the pool itself is reachable. Nothing reads `live`
        // after this -- `stopped` is set, so stopWhenIdle() is done with it
        // -- and a task still running forgets itself into an empty array.
        for (Scheduled<?> task : live.getAndSet(NONE)) {
            task.disarm();
            if (!task.outcome.isDone() && !task.running.get()) {
                dropped.add(task);
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
