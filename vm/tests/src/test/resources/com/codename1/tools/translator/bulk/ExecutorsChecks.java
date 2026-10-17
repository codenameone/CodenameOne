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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/// The executor services of java.util.concurrent, on real threads.
///
/// Every pool a check starts is shut down and waited for before the check
/// ends, and every wait is bounded generously, so that a loaded machine
/// changes how long a check takes and never what it prints.
public final class ExecutorsChecks {
    private static final Runnable NOTHING = () -> {
    };

    private ExecutorsChecks() {
    }

    /// Shuts `pool` down and prints whether it stopped.
    private static void terminate(String key, ExecutorService pool) throws InterruptedException {
        pool.shutdown();
        Bulk.p(key + ".isShutdown", pool.isShutdown());
        Bulk.p(key + ".awaitTermination", pool.awaitTermination(20, TimeUnit.SECONDS));
        Bulk.p(key + ".isTerminated", pool.isTerminated());
    }

    /// Stops `pool` for good, whatever the check printed.
    private static void stop(ExecutorService pool) throws InterruptedException {
        pool.shutdownNow();
        pool.awaitTermination(20, TimeUnit.SECONDS);
    }

    /// Waits up to 30 seconds for `runs` to reach `target`.
    private static boolean waitForRuns(AtomicInteger runs, int target) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30000;
        while (runs.get() < target && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        return runs.get() >= target;
    }

    private static Callable<String> answering(String value, AtomicBoolean ran) {
        return () -> {
            ran.set(true);
            return value;
        };
    }

    /// Waits until `flag` is set, for at most 30 seconds, ignoring every
    /// interrupt (and keeping none of them).
    private static void awaitIgnoringInterrupts(AtomicBoolean flag) {
        long deadline = System.currentTimeMillis() + 30000;
        while (!flag.get() && System.currentTimeMillis() < deadline) {
            Thread.interrupted();
            Thread.yield();
        }
        Thread.interrupted();
    }

    /// What `work` answers, computed on a thread of its own that has ended
    /// by the time this returns.
    ///
    /// Every object a collection check watches is made, and every weak
    /// reference to one is read, this way. ParparVM scans native stacks
    /// conservatively: a stale machine word left on a living thread's stack by
    /// a call that once held the object keeps it marked, which the JDK's
    /// contract allows (a weakly reachable object MAY be cleared) and which
    /// made these checks answer false on ParparVM for an object nothing
    /// reached. A thread that has ended leaves no stack to scan. (Measured: the
    /// same plain `Object` was never cleared when made on the check's thread,
    /// and was cleared when made on one that ended.)
    @SuppressWarnings("unchecked")
    private static <T> T offThread(final Callable<T> work) throws Exception {
        final Object[] out = new Object[1];
        final Throwable[] failure = new Throwable[1];
        Thread t = new Thread(() -> {
            try {
                out[0] = work.call();
            } catch (Throwable e) {
                failure[0] = e;
            }
        });
        t.start();
        t.join();
        if (failure[0] != null) {
            throw new RuntimeException(failure[0]);
        }
        return (T) out[0];
    }

    /// How many of `refs` still answer a referent, read off this thread.
    private static int uncleared(final List<? extends WeakReference<?>> refs) throws Exception {
        Integer left = offThread(() -> {
            int n = 0;
            for (WeakReference<?> r : refs) {
                if (r.get() != null) {
                    n++;
                }
            }
            return Integer.valueOf(n);
        });
        return left.intValue();
    }

    /// How many of `refs` are not cleared after asking the collector for up
    /// to about twelve seconds.
    ///
    /// The references are read only after several collections in a row: on
    /// ParparVM a read marks its referent as in use for the next collection,
    /// which then keeps it (see cn1GcProcessReferences), so reading after
    /// every one would keep the referent forever.
    private static int notCollected(List<? extends WeakReference<?>> refs) throws Exception {
        int left = uncleared(refs);
        for (int round = 0; round < 40 && left > 0; round++) {
            for (int i = 0; i < 4; i++) {
                System.gc();
                Thread.sleep(75);
            }
            left = uncleared(refs);
        }
        return left;
    }

    /// True once `ref` is cleared, asking the collector the same way.
    private static boolean collected(WeakReference<?> ref) throws Exception {
        return notCollected(Collections.singletonList(ref)) == 0;
    }

    /// Schedules, an hour out, a task whose result is a large array, and
    /// answers a weak reference to that array: in a method of its own so
    /// that no frame of the check holds it.
    private static WeakReference<Object> scheduleHolding(ScheduledExecutorService pool) {
        final Object held = new byte[1 << 16];
        ScheduledFuture<Object> future = pool.schedule(() -> held, 1, TimeUnit.HOURS);
        Bulk.p("holding.isDone", future.isDone());
        return new WeakReference<Object>(held);
    }

    private static WeakReference<Object> completedOrTimeout() {
        CompletableFuture<String> failing = new CompletableFuture<String>();
        Bulk.p("orTimeout.same", failing == failing.orTimeout(1, TimeUnit.HOURS));
        Bulk.p("orTimeout.complete", failing.complete("in time"));
        return new WeakReference<Object>(failing);
    }

    private static WeakReference<Object> cancelledCompleteOnTimeout() {
        CompletableFuture<String> defaulting = new CompletableFuture<String>();
        defaulting.completeOnTimeout("late", 1, TimeUnit.HOURS);
        Bulk.p("completeOnTimeout.cancel", defaulting.cancel(false));
        return new WeakReference<Object>(defaulting);
    }

    /// Makes 1000 timeouts of an hour, completes every future in time, and
    /// answers weak references to the futures: none of them may stay held
    /// by a wait nobody needs any more.
    private static List<WeakReference<Object>> manyCompletedTimeouts(int threadsBefore) {
        List<CompletableFuture<String>> all = new ArrayList<CompletableFuture<String>>();
        for (int i = 0; i < 500; i++) {
            all.add(new CompletableFuture<String>().orTimeout(1, TimeUnit.HOURS));
            all.add(new CompletableFuture<String>().completeOnTimeout("late", 1, TimeUnit.HOURS));
        }
        Bulk.p("oneThreadWaitsForAll", Thread.activeCount() <= threadsBefore + 1);
        int completed = 0;
        List<WeakReference<Object>> refs = new ArrayList<WeakReference<Object>>();
        for (CompletableFuture<String> f : all) {
            if (f.complete("in time")) {
                completed++;
            }
            refs.add(new WeakReference<Object>(f));
        }
        Bulk.p("completedInTime", completed);
        return refs;
    }

    /// Schedules, at a delay `pool.shutdownNow()` will drop, a task holding
    /// a large array, and answers a weak reference to the array.
    private static WeakReference<Object> scheduleDroppedPayload(ScheduledExecutorService pool) {
        final Object held = new byte[1 << 16];
        pool.schedule(() -> held, 1, TimeUnit.HOURS);
        return new WeakReference<Object>(held);
    }

    /// An executor that runs each task on the calling thread: only
    /// `AbstractExecutorService`'s own `submit` and `FutureTask` between it
    /// and the task.
    private static final class DirectExecutorService extends AbstractExecutorService {
        private boolean shutdown;

        @Override
        public void execute(Runnable command) {
            if (shutdown) {
                throw new RejectedExecutionException();
            }
            command.run();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return new ArrayList<Runnable>();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("Executors.aFactoryThatThrowsLeavesNoSubmissionBehind", () -> {
            final java.util.concurrent.atomic.AtomicInteger made = new java.util.concurrent.atomic.AtomicInteger();
            final AtomicInteger ran = new AtomicInteger();
            ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
                if (made.incrementAndGet() == 1) {
                    throw new IllegalStateException("no thread");
                }
                return new Thread(r);
            });
            try {
                try {
                    pool.execute(() -> ran.addAndGet(1));
                    Bulk.p("threw", false);
                } catch (IllegalStateException expected) {
                    Bulk.p("threw", expected.getMessage());
                }
                CountDownLatch second = new CountDownLatch(1);
                pool.execute(() -> {
                    ran.addAndGet(10);
                    second.countDown();
                });
                Bulk.p("secondRan", second.await(20, TimeUnit.SECONDS));
                pool.shutdown();
                Bulk.p("ended", pool.awaitTermination(20, TimeUnit.SECONDS));
                Bulk.p("ran", ran.get());
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Executors.cancelTrueInterruptsARunningScheduledCallable", () -> {
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
            try {
                final CountDownLatch started = new CountDownLatch(1);
                final CountDownLatch interrupted = new CountDownLatch(1);
                ScheduledFuture<String> f = pool.schedule(() -> {
                    started.countDown();
                    try {
                        Thread.sleep(20000);
                        return "slept";
                    } catch (InterruptedException e) {
                        interrupted.countDown();
                        return "interrupted";
                    }
                }, 0, TimeUnit.MILLISECONDS);
                Bulk.p("started", started.await(20, TimeUnit.SECONDS));
                Bulk.p("cancelled", f.cancel(true));
                Bulk.p("interrupted", interrupted.await(20, TimeUnit.SECONDS));
            } finally {
                stop(pool);
            }
        });
        // Was ExecutorIntegrationTest, disabled for a crash in the one-off
        // harness it ran in; the two programs it never ran are this check.
        Bulk.check("Executors.aSubclassOfAbstractExecutorServiceRunsWhatItIsSubmitted", () -> {
            ExecutorService executor = new DirectExecutorService();
            Future<String> f1 = executor.submit(() -> "Hello");
            Future<String> f2 = executor.submit(() -> "World");
            Bulk.p("callables", f1.get() + " " + f2.get());
            final AtomicInteger counter = new AtomicInteger(0);
            Runnable task = () -> counter.set(counter.get() + 1);
            Future<?> r1 = executor.submit(task);
            Future<?> r2 = executor.submit(task);
            Bulk.p("runnableResults", String.valueOf(r1.get()) + "/" + r2.get());
            Bulk.p("counter", counter.get());
            Bulk.p("done", r1.isDone() + "/" + r2.isDone());
            executor.shutdown();
            Bulk.p("isShutdown", executor.isShutdown());
            Bulk.pThrown("afterShutdown", RejectedExecutionException.class, () -> executor.submit(task));
        });
        Bulk.check("Executors.aSingleThreadRunsTasksInOrderOffTheSubmittingThread", () -> {
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                final List<Integer> order = Collections.synchronizedList(new ArrayList<Integer>());
                final Thread caller = Thread.currentThread();
                final AtomicBoolean onCaller = new AtomicBoolean();
                List<Future<Integer>> futures = new ArrayList<Future<Integer>>();
                for (int i = 0; i < 50; i++) {
                    final int n = i;
                    futures.add(pool.submit(() -> {
                        order.add(Integer.valueOf(n));
                        onCaller.compareAndSet(false, Thread.currentThread() == caller);
                        return Integer.valueOf(n * 2);
                    }));
                }
                StringBuilder values = new StringBuilder();
                boolean allDone = true;
                boolean noneCancelled = true;
                for (int i = 0; i < 50; i++) {
                    values.append(futures.get(i).get(20, TimeUnit.SECONDS)).append(',');
                    allDone &= futures.get(i).isDone();
                    noneCancelled &= !futures.get(i).isCancelled();
                }
                Bulk.p("values", values.toString());
                Bulk.p("allDone", allDone);
                Bulk.p("noneCancelled", noneCancelled);
                Bulk.p("order", order);
                Bulk.p("ranOnCaller", onCaller.get());
                Bulk.p("terminatedEarly", pool.isTerminated());
                terminate("pool", pool);
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Executors.aFailingTaskSurfacesAsAnExecutionException", () -> {
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                final IllegalStateException boom = new IllegalStateException("boom");
                Future<String> failed = pool.submit(() -> {
                    throw boom;
                });
                try {
                    failed.get(20, TimeUnit.SECONDS);
                    Bulk.p("get", "returned");
                } catch (ExecutionException expected) {
                    Bulk.p("causeIsTheThrown", expected.getCause() == boom);
                }
                Bulk.p("isDone", failed.isDone());
                // The worker survives a task that throws.
                Bulk.p("after", pool.submit(Executors.callable(NOTHING, "after")).get(20, TimeUnit.SECONDS));
                terminate("pool", pool);
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Executors.aPoolRunsEveryTaskAndInvokeAllWaitsForThem", () -> {
            ExecutorService pool = Executors.newCachedThreadPool();
            try {
                final AtomicInteger ran = new AtomicInteger();
                List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>();
                for (int i = 0; i < 40; i++) {
                    final int n = i;
                    tasks.add(() -> {
                        ran.incrementAndGet();
                        if (n == 13) {
                            throw new IllegalArgumentException("unlucky");
                        }
                        return Integer.valueOf(n);
                    });
                }
                List<Future<Integer>> futures = pool.invokeAll(tasks);
                Bulk.p("ran", ran.get());
                Bulk.p("size", futures.size());
                StringBuilder values = new StringBuilder();
                boolean allDone = true;
                for (int i = 0; i < 40; i++) {
                    allDone &= futures.get(i).isDone();
                    try {
                        values.append(futures.get(i).get()).append(',');
                    } catch (ExecutionException e) {
                        values.append("failed:").append(e.getCause().getMessage()).append(',');
                    }
                }
                Bulk.p("allDone", allDone);
                Bulk.p("values", values.toString());
                final AtomicBoolean plain = new AtomicBoolean();
                pool.submit(() -> plain.set(true)).get(20, TimeUnit.SECONDS);
                Bulk.p("plain", plain.get());
                terminate("pool", pool);
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Executors.shutdownRefusesNewTasksButFinishesAcceptedOnes", () -> {
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                final AtomicInteger ran = new AtomicInteger();
                for (int i = 0; i < 20; i++) {
                    pool.execute(ran::incrementAndGet);
                }
                pool.shutdown();
                Bulk.pThrown("executeAfterShutdown", RejectedExecutionException.class,
                        () -> pool.execute(() -> ran.addAndGet(1000)));
                Bulk.p("awaitTermination", pool.awaitTermination(20, TimeUnit.SECONDS));
                Bulk.p("ran", ran.get());
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Executors.shutdownNowHandsBackWhatNeverStarted", () -> {
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                final AtomicBoolean release = new AtomicBoolean();
                final AtomicBoolean started = new AtomicBoolean();
                // The running task ignores the interrupt cancel(true) sends it, so
                // the worker stays busy until the check releases it: on the JDK
                // an interruptible wait freed the worker at once, and it took the
                // queued task before shutdownNow() could hand it back.
                Future<String> running = pool.submit(() -> {
                    started.set(true);
                    awaitIgnoringInterrupts(release);
                    return "finished";
                });
                for (int i = 0; i < 10000 && !started.get(); i++) {
                    Thread.sleep(2);
                }
                Bulk.p("started", started.get());
                final AtomicBoolean queuedRan = new AtomicBoolean();
                Future<String> queued = pool.submit(() -> {
                    queuedRan.set(true);
                    return "queued";
                });
                // The worker is busy with the first task.
                Bulk.pThrown("queuedGet", TimeoutException.class, () -> queued.get(50, TimeUnit.MILLISECONDS));
                // The JDK interrupts a running FutureTask on cancel(true) and answers true.
                Bulk.p("cancelRunning", running.cancel(true));
                Bulk.p("runningIsCancelled", running.isCancelled());
                List<Runnable> dropped = pool.shutdownNow();
                Bulk.p("dropped", dropped.size());
                // The dropped task is handed back as the very future submitted,
                // neither cancelled nor completed.
                Bulk.p("droppedIsTheFuture", dropped.contains(queued));
                Bulk.p("queuedIsCancelled", queued.isCancelled());
                Bulk.p("queuedIsDone", queued.isDone());
                release.set(true);
                Bulk.pThrown("runningGet", CancellationException.class, () -> running.get(20, TimeUnit.SECONDS));
                Bulk.p("awaitTermination", pool.awaitTermination(20, TimeUnit.SECONDS));
                Bulk.p("queuedRan", queuedRan.get());
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Executors.anUnboundedAwaitTerminationWaits", () -> {
            // A deadline of now plus Long.MAX_VALUE overflowed into the past
            // and the wait answered false at once.
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                pool.execute(() -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                pool.shutdown();
                Bulk.p("awaitTermination", pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS));
                Bulk.p("isTerminated", pool.isTerminated());
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Executors.theArgumentsAreChecked", () -> {
            Bulk.pThrown("fixed0", IllegalArgumentException.class, () -> Executors.newFixedThreadPool(0));
            Bulk.pThrown("fixedNegative", IllegalArgumentException.class, () -> Executors.newFixedThreadPool(-1));
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                Bulk.pThrown("executeNull", NullPointerException.class, () -> pool.execute(null));
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Executors.anOrderlyShutdownStillRunsDelayedWorkAndCancelsWhatRepeats", () -> {
            // What a JDK scheduled executor does by default: delayed work that
            // was accepted still runs after shutdown(), repeating work is
            // cancelled. The delay is long enough that the shutdown below
            // comes first on a loaded machine too.
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
            ScheduledExecutorService idle = Executors.newSingleThreadScheduledExecutor();
            ScheduledExecutorService cancelled = Executors.newSingleThreadScheduledExecutor();
            try {
                AtomicBoolean ran = new AtomicBoolean();
                ScheduledFuture<String> once = pool.schedule(answering("ran", ran), 1500, TimeUnit.MILLISECONDS);
                ScheduledFuture<?> repeating = pool.scheduleAtFixedRate(NOTHING, 50, 50, TimeUnit.MILLISECONDS);
                ScheduledFuture<?> spaced = pool.scheduleWithFixedDelay(NOTHING, 50, 50, TimeUnit.MILLISECONDS);
                pool.shutdown();
                Bulk.p("isShutdown", pool.isShutdown());
                Bulk.p("isTerminatedWithWorkDue", pool.isTerminated());
                Bulk.p("repeatingIsCancelled", repeating.isCancelled());
                Bulk.p("spacedIsCancelled", spaced.isCancelled());
                Bulk.p("onceIsDone", once.isDone());
                Bulk.pThrown("scheduleAfterShutdown", RejectedExecutionException.class,
                        () -> pool.schedule(answering("late", new AtomicBoolean()), 1, TimeUnit.MILLISECONDS));
                Bulk.pThrown("executeAfterShutdown", RejectedExecutionException.class, () -> pool.execute(NOTHING));
                Bulk.pThrown("submitAfterShutdown", RejectedExecutionException.class, () -> pool.submit(NOTHING));
                Bulk.p("once", once.get(20, TimeUnit.SECONDS));
                Bulk.p("ran", ran.get());
                Bulk.p("awaitTermination", pool.awaitTermination(20, TimeUnit.SECONDS));
                Bulk.p("isTerminated", pool.isTerminated());

                // With nothing due, it stops at once.
                idle.scheduleAtFixedRate(NOTHING, 50, 50, TimeUnit.MILLISECONDS);
                terminate("idle", idle);

                // Work cancelled before the shutdown is not waited for.
                Bulk.p("cancelBeforeShutdown",
                        cancelled.schedule(answering("never", new AtomicBoolean()), 1, TimeUnit.HOURS).cancel(false));
                terminate("cancelled", cancelled);
            } finally {
                stop(pool);
                stop(idle);
                stop(cancelled);
            }
        });
        Bulk.check("Executors.shutdownNowDropsDelayedWork", () -> {
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
            try {
                AtomicBoolean ran = new AtomicBoolean();
                ScheduledFuture<String> once = pool.schedule(answering("ran", ran), 1000, TimeUnit.MILLISECONDS);
                ScheduledFuture<?> repeating = pool.scheduleAtFixedRate(NOTHING, 1, 1, TimeUnit.HOURS);
                // Work still waiting for its time never reached the pool, and is
                // handed back all the same: as the futures it was scheduled as.
                List<Runnable> dropped = pool.shutdownNow();
                Bulk.p("dropped", dropped.size());
                Bulk.p("droppedOnce", dropped.contains(once));
                Bulk.p("droppedRepeating", dropped.contains(repeating));
                Bulk.p("isShutdown", pool.isShutdown());
                Bulk.p("awaitTermination", pool.awaitTermination(20, TimeUnit.SECONDS));
                Thread.sleep(1300);
                Bulk.p("ran", ran.get());
                // As the JDK leaves the future of a dropped task.
                Bulk.p("onceIsDone", once.isDone());
                Bulk.p("onceIsCancelled", once.isCancelled());
            } finally {
                stop(pool);
            }
            // The public face of "a dropped wait is still queued" (the original
            // asked the package-private Delays.pending()): a wait that stayed
            // queued would still hold its task, and the task its payload.
            ScheduledExecutorService second = Executors.newScheduledThreadPool(1);
            try {
                WeakReference<Object> payload = offThread(() -> scheduleDroppedPayload(second));
                // Off this thread too: the list shutdownNow() answers holds the task.
                Bulk.p("droppedCount", offThread(() -> Integer.valueOf(second.shutdownNow().size())));
                Bulk.p("secondAwaitTermination", second.awaitTermination(20, TimeUnit.SECONDS));
                Bulk.p("droppedWaitLetGo", collected(payload));
            } finally {
                stop(second);
            }
        });
        Bulk.check("Executors.whenCompleteKeepsAFailingActionAsSuppressed", () -> {
            // An action of whenComplete that fails on a stage that failed: the
            // stage's own failure stays the one reported, and since JDK 9 the
            // action's rides along suppressed in it.
            final RuntimeException source = new RuntimeException("source");
            final IllegalStateException action = new IllegalStateException("action");
            CompletableFuture<String> f = new CompletableFuture<String>();
            CompletableFuture<String> g = f.whenComplete((value, failure) -> {
                throw action;
            });
            f.completeExceptionally(source);
            try {
                g.join();
                Bulk.p("join", "returned");
            } catch (CompletionException e) {
                Bulk.p("causeIsSource", e.getCause() == source);
                Throwable[] suppressed = source.getSuppressed();
                Bulk.p("suppressed", suppressed.length);
                Bulk.p("suppressedIsAction", suppressed.length == 1 && suppressed[0] == action);
            }

            // An action that rethrows the very failure it was given adds nothing.
            CompletableFuture<String> h = new CompletableFuture<String>();
            CompletableFuture<String> k = h.whenComplete((value, failure) -> {
                throw (RuntimeException) failure;
            });
            final RuntimeException same = new RuntimeException("same");
            h.completeExceptionally(same);
            Bulk.p("rethrowIsCompletedExceptionally", k.isCompletedExceptionally());
            Bulk.p("rethrowSuppressed", same.getSuppressed().length);
        });
        Bulk.check("Executors.shutdownNowLetsGoOfTheWorkItDrops", () -> {
            // A dropped task's future never completes, so nothing ever took it
            // out of the pool's own list: a pool kept after shutdownNow() held
            // every dropped task, and whatever its work held, for good.
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
            try {
                WeakReference<Object> payload = offThread(() -> scheduleHolding(pool));
                // Off this thread too: the list shutdownNow() answers holds the task.
                Bulk.p("dropped", offThread(() -> Integer.valueOf(pool.shutdownNow().size())));
                Bulk.p("awaitTermination", pool.awaitTermination(20, TimeUnit.SECONDS));
                Bulk.p("collected", collected(payload));
                // The pool itself is still reachable here, which is the point.
                Bulk.p("isShutdown", pool.isShutdown());
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Executors.aFixedRateCatchesUpAfterAnOverrun", () -> {
            // A fixed rate whose run overruns its period owes the firings that
            // fell inside it, as the JDK's does: they run late, one after
            // another, straight after the long run, and never two at once.
            // The overrun is 400 ms of a 20 ms period: about nineteen firings
            // owed. Within 150 ms of its end a pool that catches up starts all
            // of them and then some, one that dropped them at most eight; the
            // threshold of twelve sits between the two with room on either
            // side, so a loaded machine cannot move the answer.
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);
            try {
                final AtomicInteger runs = new AtomicInteger();
                final AtomicInteger active = new AtomicInteger();
                final AtomicInteger overlap = new AtomicInteger();
                final AtomicLong ended = new AtomicLong();
                final List<Long> starts = Collections.synchronizedList(new ArrayList<Long>());
                pool.scheduleAtFixedRate(() -> {
                    if (active.incrementAndGet() > 1) {
                        overlap.incrementAndGet();
                    }
                    starts.add(Long.valueOf(System.currentTimeMillis()));
                    if (runs.incrementAndGet() == 1) {
                        try {
                            Thread.sleep(400);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        ended.set(System.currentTimeMillis());
                    }
                    active.decrementAndGet();
                }, 0, 20, TimeUnit.MILLISECONDS);
                for (int i = 0; i < 4000 && runs.get() < 25; i++) {
                    Thread.sleep(5);
                }
                pool.shutdownNow();
                Bulk.p("awaitTermination", pool.awaitTermination(20, TimeUnit.SECONDS));
                Bulk.p("overlap", overlap.get());
                int soon = 0;
                synchronized (starts) {
                    for (Long at : starts) {
                        if (at.longValue() >= ended.get() && at.longValue() - ended.get() < 150) {
                            soon++;
                        }
                    }
                }
                Bulk.p("overrunFiringsRunLate", soon >= 12);
                if (soon < 12) {
                    System.out.println("# only " + soon + " of " + runs.get() + " runs right after the overrun");
                }
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Executors.aTaskCancelledWhileWaitingForAWorkerNeverStarts", () -> {
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
            try {
                final CountDownLatch busy = new CountDownLatch(1);
                final CountDownLatch release = new CountDownLatch(1);
                pool.execute(() -> {
                    busy.countDown();
                    try {
                        release.await(20, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                Bulk.p("busy", busy.await(20, TimeUnit.SECONDS));
                final AtomicInteger ran = new AtomicInteger();
                ScheduledFuture<?> waiting = pool.schedule(ran::incrementAndGet, 1, TimeUnit.MILLISECONDS);
                Thread.sleep(50);
                Bulk.p("cancel", waiting.cancel(false));
                release.countDown();
                // Something after it on the same worker shows the queue drained.
                Bulk.p("after", pool.submit(() -> "after").get(20, TimeUnit.SECONDS));
                Bulk.p("ran", ran.get());
                Bulk.p("isCancelled", waiting.isCancelled());
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Executors.aZeroSizedScheduledPoolRunsItsWork", () -> {
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(0);
            try {
                Bulk.p("ran", pool.schedule(() -> "ran", 5, TimeUnit.MILLISECONDS).get(20, TimeUnit.SECONDS));
            } finally {
                stop(pool);
            }
            Bulk.pThrown("negative", IllegalArgumentException.class, () -> Executors.newScheduledThreadPool(-1));
        });
        Bulk.check("Executors.aFixedRateOutlivesARunLongerThanItsPeriod", () -> {
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
            try {
                final AtomicInteger runs = new AtomicInteger();
                final CountDownLatch five = new CountDownLatch(5);
                pool.scheduleAtFixedRate(() -> {
                    if (runs.incrementAndGet() == 1) {
                        try {
                            Thread.sleep(120);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    five.countDown();
                }, 0, 20, TimeUnit.MILLISECONDS);
                Bulk.p("fiveRuns", five.await(20, TimeUnit.SECONDS));
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Executors.aDelayPastTheEndOfTheClockNeverFires", () -> {
            // A delay too long to add to the clock means never, not the past.
            CompletableFuture<String> never = new CompletableFuture<String>().orTimeout(Long.MAX_VALUE,
                    TimeUnit.MILLISECONDS);
            CompletableFuture<String> soon = new CompletableFuture<String>().completeOnTimeout("late", 30,
                    TimeUnit.MILLISECONDS);
            Bulk.p("soon", soon.get(20, TimeUnit.SECONDS));
            Bulk.p("neverIsDone", never.isDone());
            Bulk.p("neverComplete", never.complete("in time"));
        });
        Bulk.check("Executors.manyTimeoutsThatAreNotNeededHoldNoThread", () -> {
            // A device's timer slept a thread through every delay it was given,
            // cancelled or not. The original counted the queued waits through
            // the package-private Delays.pending() (1000, then 0); here a wait
            // given up must hold on to nothing: every completed future is
            // collectable.
            // Counted on the thread that makes the timeouts, which is one of the
            // living threads it compares with.
            List<WeakReference<Object>> refs = offThread(() -> manyCompletedTimeouts(Thread.activeCount()));
            Bulk.p("notCollected", notCollected(refs));
            // And the waits that are due still fire, in the order of their
            // time (the original scheduled raw Delays.after() actions; these
            // are the public timeouts built on them).
            final List<String> order = Collections.synchronizedList(new ArrayList<String>());
            final CountDownLatch done = new CountDownLatch(3);
            for (final int millis : new int[] {600, 200, 400}) {
                new CompletableFuture<String>().completeOnTimeout(String.valueOf(millis), millis, TimeUnit.MILLISECONDS)
                        .thenAccept(v -> {
                            order.add(v);
                            done.countDown();
                        });
            }
            Bulk.p("fired", done.await(20, TimeUnit.SECONDS));
            Bulk.p("order", order);
        });
        Bulk.check("Executors.aTimeoutNoLongerNeededLetsGoOfItsFuture", () -> {
            // A timeout set for a future that completed in time is cancelled,
            // and holds on to nothing: a timer keeps a cancelled task until
            // its time.
            Bulk.p("orTimeoutLetGo", collected(offThread(() -> completedOrTimeout())));
            Bulk.p("completeOnTimeoutLetGo", collected(offThread(() -> cancelledCompleteOnTimeout())));
            // And one that is needed still fires.
            Bulk.p("late", new CompletableFuture<String>().completeOnTimeout("late", 30, TimeUnit.MILLISECONDS)
                    .get(20, TimeUnit.SECONDS));
            try {
                new CompletableFuture<String>().orTimeout(30, TimeUnit.MILLISECONDS).get(20, TimeUnit.SECONDS);
                Bulk.p("orTimeout", "returned");
            } catch (ExecutionException expected) {
                Bulk.p("orTimeoutCause", expected.getCause() instanceof TimeoutException);
            }
        });
        Bulk.check("Executors.aMinimalCompletionStageIsACopy", () -> {
            // Completing the stage leaves the future alone.
            CompletableFuture<String> source = new CompletableFuture<String>();
            CompletionStage<String> stage = source.minimalCompletionStage();
            Bulk.p("stageComplete", stage.toCompletableFuture().complete("the stage's own"));
            Bulk.p("sourceIsDone", source.isDone());
            Bulk.p("sourceComplete", source.complete("the source's"));
            Bulk.p("source", source.get());

            CompletableFuture<String> second = new CompletableFuture<String>();
            CompletionStage<String> follows = second.minimalCompletionStage();
            second.complete("value");
            Bulk.p("follows", follows.toCompletableFuture().get(20, TimeUnit.SECONDS));
        });
        Bulk.check("Executors.submissionsFromSeveralThreadsAreAllAccounted", () -> {
            final ExecutorService pool = Executors.newFixedThreadPool(3);
            try {
                final AtomicInteger ran = new AtomicInteger();
                final AtomicInteger failed = new AtomicInteger();
                Thread[] submitters = new Thread[8];
                for (int t = 0; t < submitters.length; t++) {
                    submitters[t] = new Thread(() -> {
                        try {
                            for (int i = 0; i < 500; i++) {
                                pool.execute(ran::incrementAndGet);
                            }
                        } catch (Throwable e) {
                            failed.incrementAndGet();
                        }
                    });
                    submitters[t].start();
                }
                boolean joined = true;
                for (Thread submitter : submitters) {
                    submitter.join(30000);
                    joined &= !submitter.isAlive();
                }
                Bulk.p("joined", joined);
                Bulk.p("failed", failed.get());
                terminate("pool", pool);
                Bulk.p("ran", ran.get());
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Executors.aPoolShutDownWhileItIsSubmittedToTerminates", () -> {
            // A submission racing a shutdown may start a worker after the
            // shutdown has looked for workers to stop; the pool must still
            // terminate.
            int terminated = 0;
            boolean joined = true;
            for (int round = 0; round < 40; round++) {
                final ExecutorService pool = Executors.newFixedThreadPool(4);
                try {
                    Thread[] submitters = new Thread[4];
                    for (int t = 0; t < submitters.length; t++) {
                        submitters[t] = new Thread(() -> {
                            try {
                                for (int i = 0; i < 4; i++) {
                                    pool.execute(NOTHING);
                                }
                            } catch (RejectedExecutionException expected) {
                                return;
                            }
                        });
                    }
                    for (Thread t : submitters) {
                        t.start();
                    }
                    pool.shutdown();
                    for (Thread t : submitters) {
                        t.join(30000);
                        joined &= !t.isAlive();
                    }
                    if (pool.awaitTermination(10, TimeUnit.SECONDS)) {
                        terminated++;
                    }
                } finally {
                    stop(pool);
                }
            }
            Bulk.p("joined", joined);
            Bulk.p("terminatedRounds", terminated);
        });
        Bulk.check("Executors.aVeryShortFixedDelayKeepsRepeating", () -> {
            // A fixed delay shorter than the time a run takes to wind up must
            // not end the repetition: the next run is armed once this one is over.
            // The evidence is that runs keep coming after a first batch, not how
            // many fit in a time: every run is two thread hand-offs, and a loaded
            // two-core CI runner managed fewer than 300 in 30 seconds.
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
            try {
                final AtomicInteger runs = new AtomicInteger();
                ScheduledFuture<?> spaced = pool.scheduleWithFixedDelay(runs::incrementAndGet, 0, 1,
                        TimeUnit.MILLISECONDS);
                boolean first = waitForRuns(runs, 20);
                int seen = runs.get();
                boolean more = waitForRuns(runs, seen + 20);
                Bulk.p("keepsRepeating", first && more);
                Bulk.p("isDone", spaced.isDone());
                spaced.cancel(false);
            } finally {
                stop(pool);
            }
        });
        // invokeAll interrupted while it waits cancels the tasks that have not
        // finished, so none of them runs after it has returned. ParparVM only:
        // the interrupt is already pending, so invokeAll gives up before the
        // worker has necessarily taken the first task, and the JDK -- which
        // cancels from the FRONT -- then frees the worker to start the second
        // before that one is cancelled (HotSpot answered later=1 on one run in
        // three). JavaAPI cancels from the back, which is the contract here.
        Bulk.cn1Only("Executors.anInterruptedInvokeAllCancelsWhatIsLeft", () -> {
            ExecutorService pool = Executors.newFixedThreadPool(1);
            final AtomicBoolean go = new AtomicBoolean();
            try {
                final AtomicInteger later = new AtomicInteger();
                List<Callable<String>> tasks = new ArrayList<Callable<String>>();
                // The first task ignores the interrupt cancel(true) sends it, so
                // a started first task keeps the worker busy while invokeAll
                // cancels.
                tasks.add(() -> {
                    awaitIgnoringInterrupts(go);
                    return "first";
                });
                tasks.add(() -> {
                    later.incrementAndGet();
                    return "second";
                });
                Thread.currentThread().interrupt();
                boolean threw = false;
                try {
                    pool.invokeAll(tasks);
                } catch (InterruptedException expected) {
                    threw = true;
                    Bulk.expect(!Thread.currentThread().isInterrupted(), "the interrupt was not cleared");
                } finally {
                    Thread.interrupted();
                    go.set(true);
                }
                Bulk.expect(threw, "invokeAll returned although interrupted");
                Bulk.expectEquals("after", pool.submit(() -> "after").get(20, TimeUnit.SECONDS), "a task after it");
                Bulk.expectEquals(Integer.valueOf(0), Integer.valueOf(later.get()), "a cancelled task ran");
            } finally {
                go.set(true);
                Thread.interrupted();
                stop(pool);
            }
        });
    }
}
