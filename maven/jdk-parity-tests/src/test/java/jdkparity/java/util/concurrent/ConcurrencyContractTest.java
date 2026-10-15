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
package jdkparity.java.util.concurrent;

import com.codename1.impl.JdkHost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The thread-related contracts of the JavaAPI java.util.concurrent that a
/// race test can only hit sometimes, driven deterministically: the order
/// invokeAll cancels in, the UI-thread wait through `JdkHost.blockUi`, and
/// mutation from many threads at once.
public class ConcurrencyContractTest {

    /// An executor whose single worker is driven by the test: it is waiting
    /// to take the first queued task, and moves only when that task is
    /// cancelled -- the moment a real worker is freed. It then skips every
    /// cancelled task and runs the first one that is not.
    private static final class SteppedExecutor extends AbstractExecutorService {
        final List<FutureTask<?>> queue = new ArrayList<FutureTask<?>>();

        @Override
        public void execute(Runnable command) {
            if (!(command instanceof FutureTask)) {
                throw new IllegalArgumentException();
            }
            queue.add((FutureTask<?>) command);
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
            return new FutureTask<T>(callable) {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    boolean cancelled = super.cancel(mayInterruptIfRunning);
                    if (cancelled) {
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
        public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) {
            return false;
        }
    }

    /// Cancelling from the front freed the worker, which started the next
    /// task before the loop reached it: compat-jdk's ExecutorsTest saw
    /// "a cancelled task ran" now and then. From the back it cannot happen.
    @Test
    public void anInterruptedInvokeAllRunsNoTaskItWasGivenAfterAll() {
        SteppedExecutor executor = new SteppedExecutor();
        final AtomicInteger ran = new AtomicInteger();
        List<Callable<String>> tasks = new ArrayList<Callable<String>>();
        for (int i = 0; i < 4; i++) {
            tasks.add(new Callable<String>() {
                @Override
                public String call() {
                    ran.incrementAndGet();
                    return "ran";
                }
            });
        }
        Thread.currentThread().interrupt();
        try {
            executor.invokeAll(tasks);
            fail("invokeAll returned although interrupted");
        } catch (InterruptedException expected) {
            assertFalse(Thread.currentThread().isInterrupted());
        }
        assertEquals(0, ran.get(), "a task invokeAll cancelled ran while it was cancelling");
        assertTrue(executor.queue.isEmpty(), "the worker did not get past the cancelled tasks");
    }

    /// The same for the timed overload, which gives up at its deadline.
    @Test
    public void aTimedInvokeAllThatRunsOutCancelsWhatIsLeft() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger later = new AtomicInteger();
        List<Callable<String>> tasks = new ArrayList<Callable<String>>();
        tasks.add(new Callable<String>() {
            @Override
            public String call() throws Exception {
                release.await(10, TimeUnit.SECONDS);
                return "first";
            }
        });
        tasks.add(new Callable<String>() {
            @Override
            public String call() {
                later.incrementAndGet();
                return "second";
            }
        });
        List<Future<String>> futures = pool.invokeAll(tasks, 50, TimeUnit.MILLISECONDS);
        release.countDown();
        assertTrue(futures.get(0).isCancelled());
        assertTrue(futures.get(1).isCancelled());
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(0, later.get());
    }

    @Test
    public void invokeAnyAnswersTheFirstSuccessAndCancelsTheRest() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            List<Callable<String>> tasks = new ArrayList<Callable<String>>();
            tasks.add(new Callable<String>() {
                @Override
                public String call() {
                    throw new IllegalStateException("fails");
                }
            });
            tasks.add(new Callable<String>() {
                @Override
                public String call() {
                    return "wins";
                }
            });
            assertEquals("wins", pool.invokeAny(tasks));
            List<Callable<String>> failing = new ArrayList<Callable<String>>();
            failing.add(tasks.get(0));
            ExecutionException e = assertThrows(ExecutionException.class, () -> pool.invokeAny(failing));
            assertTrue(e.getCause() instanceof IllegalStateException);
            List<Callable<String>> slow = new ArrayList<Callable<String>>();
            slow.add(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    Thread.sleep(5000);
                    return "late";
                }
            });
            assertThrows(TimeoutException.class, () -> pool.invokeAny(slow, 20, TimeUnit.MILLISECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    /// A host whose UI thread is the test's thread, and whose blockUi runs
    /// the wait on another thread, as `Display.invokeAndBlock` does.
    private static final class UiHost extends JdkHost {
        final Thread ui;
        final AtomicInteger blocked = new AtomicInteger();

        UiHost(Thread ui) {
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

    /// Every wait in the package, on the UI thread, goes through blockUi
    /// and never blocks that thread itself.
    @Test
    public void waitsOnTheUiThreadGoThroughBlockUi() throws Exception {
        UiHost host = new UiHost(Thread.currentThread());
        JdkHost.install(host);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            final CountDownLatch go = new CountDownLatch(1);
            Future<String> f = pool.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    go.await();
                    return "done";
                }
            });
            CompletableFuture<String> cf = CompletableFuture.supplyAsync(new java.util.function.Supplier<String>() {
                @Override
                public String get() {
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        return "interrupted";
                    }
                    go.countDown();
                    return "async";
                }
            });
            assertEquals("done", f.get());
            assertEquals("async", cf.join());
            assertTrue(go.await(1, TimeUnit.SECONDS));
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
            assertTrue(host.blocked.get() >= 2, "blockUi was used " + host.blocked.get() + " times");
        } finally {
            JdkHost.install(null);
            pool.shutdownNow();
        }
    }

    /// An interrupt that arrives while the UI thread's wait runs elsewhere
    /// still reaches the caller as an InterruptedException.
    @Test
    public void aWaitInterruptedOffTheUiThreadIsReportedOnIt() throws Exception {
        final Thread[] waiter = new Thread[1];
        JdkHost.install(new JdkHost() {
            @Override
            public boolean isUiThread() {
                return true;
            }

            @Override
            public void blockUi(Runnable task) {
                Thread t = new Thread(task, "blockUi");
                waiter[0] = t;
                t.start();
                try {
                    Thread.sleep(50);
                    t.interrupt();
                    t.join();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        try {
            FutureTask<String> never = new FutureTask<String>(new Callable<String>() {
                @Override
                public String call() {
                    return "never run";
                }
            });
            assertThrows(InterruptedException.class, never::get);
        } finally {
            JdkHost.install(null);
        }
    }

    /// Many threads mutating at once lose nothing.
    @Test
    public void concurrentMutationLosesNothing() throws Exception {
        final ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<Integer, Integer>();
        final CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<Integer>();
        final CopyOnWriteArraySet<Integer> set = new CopyOnWriteArraySet<Integer>();
        final int threads = 8;
        final int each = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<Future<?>>();
        for (int t = 0; t < threads; t++) {
            final int id = t;
            futures.add(pool.submit(new Runnable() {
                @Override
                public void run() {
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
                }
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(pool.isTerminated());
        assertEquals(each, map.size());
        for (int i = 0; i < each; i++) {
            assertEquals(Integer.valueOf(threads), map.get(i));
        }
        assertEquals(threads * each, list.size());
        assertEquals(threads * each, new HashSet<Integer>(list).size());
        Set<Integer> expected = new HashSet<Integer>();
        for (int i = 0; i < each; i++) {
            expected.add(i);
        }
        assertEquals(expected, new HashSet<Integer>(Arrays.asList(set.toArray(new Integer[0]))));
    }

    /// cancel(true) interrupts the running task, and the interrupt does not
    /// leak into the next task that worker runs.
    @Test
    public void cancelInterruptsTheRunnerAndOnlyIt() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            final CountDownLatch started = new CountDownLatch(1);
            final CountDownLatch interrupted = new CountDownLatch(1);
            Future<?> sleeper = pool.submit(new Runnable() {
                @Override
                public void run() {
                    started.countDown();
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        interrupted.countDown();
                    }
                }
            });
            assertTrue(started.await(10, TimeUnit.SECONDS));
            assertTrue(sleeper.cancel(true));
            assertTrue(sleeper.isCancelled());
            assertTrue(interrupted.await(10, TimeUnit.SECONDS));
            assertThrows(CancellationException.class, sleeper::get);
            Future<Boolean> next = pool.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    return Thread.currentThread().isInterrupted();
                }
            });
            assertFalse(next.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    /// The thread factory is asked for every worker.
    @Test
    public void aThreadFactoryMakesTheWorkers() throws Exception {
        final AtomicInteger made = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                made.incrementAndGet();
                return new Thread(r, "made-" + made.get());
            }
        });
        try {
            Future<String> name = pool.submit(new Callable<String>() {
                @Override
                public String call() {
                    return Thread.currentThread().getName();
                }
            });
            assertTrue(name.get(10, TimeUnit.SECONDS).startsWith("made-"));
            assertTrue(made.get() >= 1);
        } finally {
            pool.shutdown();
        }
    }

    /// Scheduled futures compare by their remaining delay.
    @Test
    public void scheduledFuturesAreOrderedByDelay() throws Exception {
        ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
        try {
            ScheduledFuture<?> soon = pool.schedule(new Runnable() {
                @Override
                public void run() {
                }
            }, 10, TimeUnit.SECONDS);
            ScheduledFuture<?> later = pool.schedule(new Runnable() {
                @Override
                public void run() {
                }
            }, 20, TimeUnit.SECONDS);
            assertTrue(soon.compareTo(later) < 0);
            assertTrue(later.compareTo(soon) > 0);
            assertEquals(0, soon.compareTo(soon));
        } finally {
            pool.shutdownNow();
        }
    }
}
