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

import com.codename1.impl.JdkHost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/// The thread-related contracts of java.util.concurrent that a race test can
/// only hit sometimes, driven deterministically: the order invokeAll cancels
/// in, the UI-thread wait through `JdkHost.blockUi`, and mutation from many
/// threads at once.
public final class ConcurrencyChecks {
    private ConcurrencyChecks() {
    }

    /// An executor whose single worker is driven by the check, built on
    /// public API only (an AbstractExecutorService with its own `execute`
    /// and `newTaskFor`): the worker is waiting to take the first queued
    /// task, and moves only when that task is cancelled -- the moment a real
    /// worker is freed. It then skips every cancelled task and runs the
    /// first one that is not.
    private static final class SteppedExecutor extends AbstractExecutorService {
        final List<FutureTask<?>> queue = new ArrayList<FutureTask<?>>();
        /// The index of each task in the order invokeAll cancelled it.
        final List<Integer> cancelOrder = new ArrayList<Integer>();
        private int submitted;

        @Override
        public void execute(Runnable command) {
            if (!(command instanceof FutureTask)) {
                throw new IllegalArgumentException();
            }
            queue.add((FutureTask<?>) command);
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
            final int index = submitted++;
            return new FutureTask<T>(callable) {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    boolean cancelled = super.cancel(mayInterruptIfRunning);
                    if (cancelled) {
                        cancelOrder.add(Integer.valueOf(index));
                        step();
                    }
                    return cancelled;
                }
            };
        }

        void step() {
            if (queue.isEmpty() || !queue.get(0).isDone()) {
                return;
            }
            while (!queue.isEmpty()) {
                FutureTask<?> next = queue.remove(0);
                if (!next.isDone()) {
                    next.run();
                    return;
                }
            }
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return Collections.<Runnable>emptyList();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }
    }

    /// A host whose UI thread is the check's thread, and whose blockUi runs
    /// the wait on another thread, as `Display.invokeAndBlock` does.
    private static final class ConcurrencyUiHost extends JdkHost {
        final Thread ui;
        final AtomicInteger blocked = new AtomicInteger();

        ConcurrencyUiHost(Thread ui) {
            this.ui = ui;
        }

        @Override
        public boolean isUiThread() {
            return Thread.currentThread() == ui;
        }

        @Override
        public void blockUi(Runnable task) {
            blocked.incrementAndGet();
            Thread t = new Thread(task, "blockUi");
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /// A host for which every thread is the UI thread, and whose blockUi
    /// interrupts the thread it runs the wait on. Bounded: when the
    /// interrupt does not end the wait within ten seconds it says so, then
    /// runs the task waited for so the wait ends and the program goes on.
    private static final class ConcurrencyInterruptingHost extends JdkHost {
        final FutureTask<String> waitedFor;
        boolean interruptEndedTheWait;

        ConcurrencyInterruptingHost(FutureTask<String> waitedFor) {
            this.waitedFor = waitedFor;
        }

        @Override
        public boolean isUiThread() {
            return true;
        }

        @Override
        public void blockUi(Runnable task) {
            Thread t = new Thread(task, "blockUi");
            t.start();
            try {
                Thread.sleep(50);
                t.interrupt();
                t.join(10000);
                interruptEndedTheWait = !t.isAlive();
                if (t.isAlive()) {
                    waitedFor.run();
                    t.join(10000);
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static void stop(ExecutorService pool) throws InterruptedException {
        pool.shutdownNow();
        pool.awaitTermination(20, TimeUnit.SECONDS);
    }

    /// Runs the group.
    public static void run() {
        // JavaAPI cancels what is left of an interrupted invokeAll from the
        // BACK; the JDK cancels from the front, and with this executor that
        // runs a task the caller was told would not run. Codename One's own
        // contract, so ParparVM only.
        Bulk.cn1Only("Concurrency.anInterruptedInvokeAllRunsNoTaskItWasGivenAfterAll", () -> {
            SteppedExecutor executor = new SteppedExecutor();
            final AtomicInteger ran = new AtomicInteger();
            List<Callable<String>> tasks = new ArrayList<Callable<String>>();
            for (int i = 0; i < 4; i++) {
                tasks.add(() -> {
                    ran.incrementAndGet();
                    return "ran";
                });
            }
            Thread.currentThread().interrupt();
            boolean threw = false;
            try {
                executor.invokeAll(tasks);
            } catch (InterruptedException expected) {
                threw = true;
                Bulk.expect(!Thread.currentThread().isInterrupted(), "the interrupt was not cleared");
            } finally {
                Thread.interrupted();
            }
            Bulk.expect(threw, "invokeAll returned although interrupted");
            Bulk.expectEquals(Integer.valueOf(0), Integer.valueOf(ran.get()),
                    "a task invokeAll cancelled ran while it was cancelling");
            Bulk.expect(executor.queue.isEmpty(), "the worker did not get past the cancelled tasks");
            Bulk.expectEquals(Arrays.asList(Integer.valueOf(3), Integer.valueOf(2), Integer.valueOf(1),
                    Integer.valueOf(0)), executor.cancelOrder, "the order invokeAll cancelled in");
        });
        Bulk.check("Concurrency.aTimedInvokeAllThatRunsOutCancelsWhatIsLeft", () -> {
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                // Half a second, so the worker has started the first task on a
                // loaded machine too before the deadline cancels anything.
                // The first task ignores the interrupt the timed invokeAll's
                // cancel(true) sends it, so its worker is not freed to start the
                // second before that is cancelled too: the JDK cancels from the
                // front, so an interruptible first task makes this a race there.
                final AtomicBoolean release = new AtomicBoolean();
                final AtomicInteger later = new AtomicInteger();
                List<Callable<String>> tasks = new ArrayList<Callable<String>>();
                tasks.add(() -> {
                    long deadline = System.currentTimeMillis() + 30000;
                    while (!release.get() && System.currentTimeMillis() < deadline) {
                        Thread.interrupted();
                        Thread.yield();
                    }
                    Thread.interrupted();
                    return "first";
                });
                tasks.add(() -> {
                    later.incrementAndGet();
                    return "second";
                });
                List<Future<String>> futures = pool.invokeAll(tasks, 500, TimeUnit.MILLISECONDS);
                release.set(true);
                Bulk.p("firstIsCancelled", futures.get(0).isCancelled());
                Bulk.p("secondIsCancelled", futures.get(1).isCancelled());
                pool.shutdown();
                Bulk.p("awaitTermination", pool.awaitTermination(20, TimeUnit.SECONDS));
                Bulk.p("later", later.get());
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Concurrency.invokeAnyAnswersTheFirstSuccessAndCancelsTheRest", () -> {
            ExecutorService pool = Executors.newFixedThreadPool(3);
            try {
                List<Callable<String>> tasks = new ArrayList<Callable<String>>();
                tasks.add(() -> {
                    throw new IllegalStateException("fails");
                });
                tasks.add(() -> "wins");
                Bulk.p("invokeAny", pool.invokeAny(tasks));
                List<Callable<String>> failing = new ArrayList<Callable<String>>();
                failing.add(tasks.get(0));
                try {
                    pool.invokeAny(failing);
                    Bulk.p("failing", "returned");
                } catch (ExecutionException e) {
                    Bulk.p("failingCause", e.getCause() instanceof IllegalStateException);
                }
                List<Callable<String>> slow = new ArrayList<Callable<String>>();
                slow.add(() -> {
                    Thread.sleep(5000);
                    return "late";
                });
                Bulk.pThrown("slow", TimeoutException.class, () -> pool.invokeAny(slow, 20, TimeUnit.MILLISECONDS));
            } finally {
                stop(pool);
            }
        });
        // Every wait in the package, on the UI thread, goes through blockUi
        // and never blocks that thread itself. JdkHost is Codename One's.
        Bulk.cn1Only("Concurrency.waitsOnTheUiThreadGoThroughBlockUi", () -> {
            ConcurrencyUiHost host = new ConcurrencyUiHost(Thread.currentThread());
            JdkHost.install(host);
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                final CountDownLatch go = new CountDownLatch(1);
                Future<String> f = pool.submit(() -> {
                    go.await();
                    return "done";
                });
                CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        return "interrupted";
                    }
                    go.countDown();
                    return "async";
                });
                Bulk.expectEquals("done", f.get(), "the future");
                Bulk.expectEquals("async", cf.join(), "the completable future");
                Bulk.expect(go.await(1, TimeUnit.SECONDS), "the latch");
                pool.shutdown();
                Bulk.expect(pool.awaitTermination(20, TimeUnit.SECONDS), "the pool did not stop");
                Bulk.expect(host.blocked.get() >= 2, "blockUi was used " + host.blocked.get() + " times");
            } finally {
                JdkHost.install(null);
                stop(pool);
            }
        });
        // An interrupt that arrives while the UI thread's wait runs elsewhere
        // still reaches the caller as an InterruptedException.
        Bulk.cn1Only("Concurrency.aWaitInterruptedOffTheUiThreadIsReportedOnIt", () -> {
            FutureTask<String> never = new FutureTask<String>(() -> "never run");
            ConcurrencyInterruptingHost host = new ConcurrencyInterruptingHost(never);
            JdkHost.install(host);
            try {
                String thrown = Bulk.thrown(InterruptedException.class, never::get);
                Bulk.expect(host.interruptEndedTheWait, "the interrupt did not end the wait on the blockUi thread");
                Bulk.expectEquals("InterruptedException", thrown, "what the wait threw");
            } finally {
                JdkHost.install(null);
            }
        });
        Bulk.check("Concurrency.concurrentMutationLosesNothing", () -> {
            final ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<Integer, Integer>();
            final CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<Integer>();
            final CopyOnWriteArraySet<Integer> set = new CopyOnWriteArraySet<Integer>();
            final int threads = 8;
            final int each = 500;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            try {
                final CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> futures = new ArrayList<Future<?>>();
                for (int t = 0; t < threads; t++) {
                    final int id = t;
                    futures.add(pool.submit(() -> {
                        try {
                            start.await();
                        } catch (InterruptedException e) {
                            return;
                        }
                        for (int i = 0; i < each; i++) {
                            map.merge(i, 1, Integer::sum);
                            list.add(id * each + i);
                            set.add(i);
                        }
                    }));
                }
                start.countDown();
                for (Future<?> f : futures) {
                    f.get(60, TimeUnit.SECONDS);
                }
                pool.shutdown();
                Bulk.p("awaitTermination", pool.awaitTermination(20, TimeUnit.SECONDS));
                Bulk.p("isTerminated", pool.isTerminated());
                Bulk.p("mapSize", map.size());
                boolean everyCount = true;
                for (int i = 0; i < each; i++) {
                    everyCount &= Integer.valueOf(threads).equals(map.get(i));
                }
                Bulk.p("everyKeyCountedByEveryThread", everyCount);
                Bulk.p("listSize", list.size());
                Bulk.p("listDistinct", new HashSet<Integer>(list).size());
                Set<Integer> expected = new HashSet<Integer>();
                for (int i = 0; i < each; i++) {
                    expected.add(i);
                }
                Bulk.p("setIsEveryKey", expected.equals(new HashSet<Integer>(Arrays.asList(set.toArray(new Integer[0])))));
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Concurrency.cancelInterruptsTheRunnerAndOnlyIt", () -> {
            // cancel(true) interrupts the running task, and the interrupt does
            // not leak into the next task that worker runs.
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                final CountDownLatch started = new CountDownLatch(1);
                final CountDownLatch interrupted = new CountDownLatch(1);
                Future<?> sleeper = pool.submit(() -> {
                    started.countDown();
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        interrupted.countDown();
                    }
                });
                Bulk.p("started", started.await(20, TimeUnit.SECONDS));
                Bulk.p("cancel", sleeper.cancel(true));
                Bulk.p("isCancelled", sleeper.isCancelled());
                Bulk.p("interrupted", interrupted.await(20, TimeUnit.SECONDS));
                Bulk.pThrown("get", CancellationException.class, sleeper::get);
                Future<Boolean> next = pool.submit(() -> Thread.currentThread().isInterrupted());
                Bulk.p("nextInterrupted", next.get(20, TimeUnit.SECONDS));
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Concurrency.aThreadFactoryMakesTheWorkers", () -> {
            final AtomicInteger made = new AtomicInteger();
            ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
                made.incrementAndGet();
                return new Thread(r, "made-" + made.get());
            });
            try {
                Future<String> name = pool.submit(() -> Thread.currentThread().getName());
                Bulk.p("nameFromFactory", name.get(20, TimeUnit.SECONDS).startsWith("made-"));
                Bulk.p("factoryAsked", made.get() >= 1);
            } finally {
                stop(pool);
            }
        });
        Bulk.check("Concurrency.scheduledFuturesAreOrderedByDelay", () -> {
            ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
            try {
                ScheduledFuture<?> soon = pool.schedule(() -> {
                }, 10, TimeUnit.SECONDS);
                ScheduledFuture<?> later = pool.schedule(() -> {
                }, 20, TimeUnit.SECONDS);
                Bulk.p("soonBeforeLater", soon.compareTo(later) < 0);
                Bulk.p("laterAfterSoon", later.compareTo(soon) > 0);
                Bulk.p("self", soon.compareTo(soon));
            } finally {
                stop(pool);
            }
        });
    }
}
