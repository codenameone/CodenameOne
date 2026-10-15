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

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The executor services, on real Codename One background threads.
public class ExecutorsTest {

    @BeforeClass
    public static void startCodenameOne() {
        HeadlessImplementation.install();
    }

    private static void terminate(ExecutorService pool) throws Exception {
        pool.shutdown();
        assertTrue(pool.isShutdown());
        assertTrue("the workers did not stop", pool.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(pool.isTerminated());
    }

    @Test
    public void aSingleThreadRunsTasksInOrderOffTheEventDispatchThread() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final List<Integer> order = Collections.synchronizedList(new ArrayList<Integer>());
        final AtomicBoolean onEdt = new AtomicBoolean();
        final Thread caller = Thread.currentThread();
        final AtomicBoolean onCaller = new AtomicBoolean();
        List<Future<Integer>> futures = new ArrayList<Future<Integer>>();
        for (int i = 0; i < 50; i++) {
            final int n = i;
            futures.add(pool.submit(new Callable<Integer>() {
                @Override
                public Integer call() {
                    order.add(Integer.valueOf(n));
                    onEdt.compareAndSet(false, Display.getInstance().isEdt());
                    onCaller.compareAndSet(false, Thread.currentThread() == caller);
                    return Integer.valueOf(n * 2);
                }
            }));
        }
        for (int i = 0; i < 50; i++) {
            assertEquals(Integer.valueOf(i * 2), futures.get(i).get(10, TimeUnit.SECONDS));
            assertTrue(futures.get(i).isDone());
            assertFalse(futures.get(i).isCancelled());
        }
        List<Integer> expected = new ArrayList<Integer>();
        for (int i = 0; i < 50; i++) {
            expected.add(Integer.valueOf(i));
        }
        assertEquals(expected, order);
        assertFalse("a task ran on the event dispatch thread", onEdt.get());
        assertFalse("a task ran on the submitting thread", onCaller.get());
        assertFalse(pool.isTerminated());
        terminate(pool);
    }

    @Test
    public void aFailingTaskSurfacesAsAnExecutionException() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        final IllegalStateException boom = new IllegalStateException("boom");
        Future<String> failed = pool.submit(new Callable<String>() {
            @Override
            public String call() {
                throw boom;
            }
        });
        try {
            failed.get(10, TimeUnit.SECONDS);
            fail("The task threw");
        } catch (ExecutionException expected) {
            assertSame(boom, expected.getCause());
        }
        assertTrue(failed.isDone());
        // The worker survives a task that throws.
        assertEquals("after", pool.submit(Executors.callable(new Runnable() {
            @Override
            public void run() {
            }
        }, "after")).get(10, TimeUnit.SECONDS));
        terminate(pool);
    }

    @Test
    public void aPoolRunsEveryTaskAndInvokeAllWaitsForThem() throws Exception {
        ExecutorService pool = Executors.newCachedThreadPool();
        final AtomicInteger ran = new AtomicInteger();
        List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>();
        for (int i = 0; i < 40; i++) {
            final int n = i;
            tasks.add(new Callable<Integer>() {
                @Override
                public Integer call() {
                    ran.incrementAndGet();
                    if (n == 13) {
                        throw new IllegalArgumentException("unlucky");
                    }
                    return Integer.valueOf(n);
                }
            });
        }
        List<Future<Integer>> futures = pool.invokeAll(tasks);
        assertEquals(40, ran.get());
        assertEquals(40, futures.size());
        for (int i = 0; i < 40; i++) {
            assertTrue(futures.get(i).isDone());
            if (i == 13) {
                try {
                    futures.get(i).get();
                    fail();
                } catch (ExecutionException expected) {
                    assertEquals("unlucky", expected.getCause().getMessage());
                }
            } else {
                assertEquals(Integer.valueOf(i), futures.get(i).get());
            }
        }
        final AtomicBoolean plain = new AtomicBoolean();
        Future<?> done = pool.submit(new Runnable() {
            @Override
            public void run() {
                plain.set(true);
            }
        });
        done.get(10, TimeUnit.SECONDS);
        assertTrue(plain.get());
        terminate(pool);
    }

    @Test
    public void shutdownRefusesNewTasksButFinishesAcceptedOnes() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final AtomicInteger ran = new AtomicInteger();
        for (int i = 0; i < 20; i++) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    ran.incrementAndGet();
                }
            });
        }
        pool.shutdown();
        try {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    ran.addAndGet(1000);
                }
            });
            fail("A shut down executor accepts nothing");
        } catch (RejectedExecutionException expected) {
            assertNotNull(expected);
        }
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(20, ran.get());
    }

    @Test
    public void shutdownNowHandsBackWhatNeverStartedAndCancelsItsFutures() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final AtomicBoolean release = new AtomicBoolean();
        final AtomicBoolean started = new AtomicBoolean();
        Future<String> running = pool.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                started.set(true);
                while (!release.get()) {
                    Thread.sleep(2);
                }
                return "finished";
            }
        });
        while (!started.get()) {
            Thread.sleep(2);
        }
        final AtomicBoolean queuedRan = new AtomicBoolean();
        Future<String> queued = pool.submit(new Callable<String>() {
            @Override
            public String call() {
                queuedRan.set(true);
                return "queued";
            }
        });
        try {
            queued.get(50, TimeUnit.MILLISECONDS);
            fail("The worker is busy with the first task");
        } catch (TimeoutException expected) {
            assertNotNull(expected);
        }
        assertFalse("A started task cannot be cancelled", running.cancel(true));

        List<Runnable> dropped = pool.shutdownNow();
        assertEquals(1, dropped.size());
        assertTrue(queued.isCancelled());
        try {
            queued.get();
            fail("A dropped task is a cancelled one");
        } catch (CancellationException expected) {
            assertNotNull(expected);
        }
        release.set(true);
        assertEquals("finished", running.get(10, TimeUnit.SECONDS));
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertFalse(queuedRan.get());
    }

    /// The largest timeout waits: a deadline of now plus Long.MAX_VALUE
    /// overflowed into the past and the wait answered false at once.
    @Test
    public void anUnboundedAwaitTerminationWaits() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        pool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        pool.shutdown();
        assertTrue(pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS));
        assertTrue(pool.isTerminated());
    }

    @Test
    public void theArgumentsAreChecked() {
        for (int bad : Arrays.asList(Integer.valueOf(0), Integer.valueOf(-1))) {
            try {
                Executors.newFixedThreadPool(bad);
                fail();
            } catch (IllegalArgumentException expected) {
                assertNotNull(expected);
            }
        }
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.execute(null);
            fail();
        } catch (NullPointerException expected) {
            assertNotNull(expected);
        }
        pool.shutdown();
    }

    private static Callable<String> answering(final String value, final AtomicBoolean ran) {
        return new Callable<String>() {
            @Override
            public String call() {
                ran.set(true);
                return value;
            }
        };
    }

    private static final Runnable NOTHING = new Runnable() {
        @Override
        public void run() {
        }
    };

    /// What a JDK scheduled executor does by default: delayed work that was
    /// accepted still runs after `shutdown()`, repeating work is cancelled.
    @Test
    public void anOrderlyShutdownStillRunsDelayedWorkAndCancelsWhatRepeats() throws Exception {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
        AtomicBoolean ran = new AtomicBoolean();
        ScheduledFuture<String> once = pool.schedule(answering("ran", ran), 300, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> repeating = pool.scheduleAtFixedRate(NOTHING, 50, 50, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> spaced = pool.scheduleWithFixedDelay(NOTHING, 50, 50, TimeUnit.MILLISECONDS);
        pool.shutdown();
        assertTrue(pool.isShutdown());
        assertFalse("Accepted work is still due", pool.isTerminated());
        assertTrue(repeating.isCancelled());
        assertTrue(spaced.isCancelled());
        assertFalse(once.isDone());
        try {
            pool.schedule(answering("late", new AtomicBoolean()), 1, TimeUnit.MILLISECONDS);
            fail("Nothing new is accepted");
        } catch (RejectedExecutionException expected) {
            assertNotNull(expected);
        }
        try {
            pool.execute(NOTHING);
            fail("Nothing new is accepted");
        } catch (RejectedExecutionException expected) {
            assertNotNull(expected);
        }
        try {
            pool.submit(NOTHING);
            fail("Nothing new is accepted");
        } catch (RejectedExecutionException expected) {
            assertNotNull(expected);
        }
        assertEquals("ran", once.get(10, TimeUnit.SECONDS));
        assertTrue(ran.get());
        assertTrue("the executor did not stop after its last task", pool.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(pool.isTerminated());

        // With nothing due, it stops at once.
        ScheduledExecutorService idle = Executors.newSingleThreadScheduledExecutor();
        idle.scheduleAtFixedRate(NOTHING, 50, 50, TimeUnit.MILLISECONDS);
        terminate(idle);

        // Work cancelled before the shutdown is not waited for.
        ScheduledExecutorService cancelled = Executors.newSingleThreadScheduledExecutor();
        assertTrue(cancelled.schedule(answering("never", new AtomicBoolean()), 1, TimeUnit.HOURS).cancel(false));
        terminate(cancelled);
    }

    @Test
    public void shutdownNowDropsDelayedWork() throws Exception {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
        AtomicBoolean ran = new AtomicBoolean();
        ScheduledFuture<String> once = pool.schedule(answering("ran", ran), 150, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> repeating = pool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
            }
        }, 1, 1, TimeUnit.HOURS);
        // Work still waiting for its time never reached the pool, and is
        // handed back all the same: as the futures it was scheduled as.
        List<Runnable> dropped = pool.shutdownNow();
        assertEquals(2, dropped.size());
        assertTrue(dropped.contains(once));
        assertTrue(dropped.contains(repeating));
        assertEquals("a dropped wait is still queued", 0, Delays.pending());
        assertTrue(pool.isShutdown());
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        Thread.sleep(400);
        assertFalse(ran.get());
        // As the JDK leaves the future of a dropped task.
        assertFalse(once.isDone());
        assertFalse(once.isCancelled());
    }

    private static boolean collected(java.lang.ref.WeakReference<?> ref) throws Exception {
        for (int i = 0; i < 100 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(20);
        }
        return ref.get() == null;
    }

    /// A fixed rate whose run overruns its period owes the firings that
    /// fell inside it, as the JDK's does: they run late, one after another,
    /// straight after the long run, and never two at once.
    @Test
    public void aFixedRateCatchesUpAfterAnOverrun() throws Exception {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);
        final AtomicInteger runs = new AtomicInteger();
        final AtomicInteger active = new AtomicInteger();
        final AtomicInteger overlap = new AtomicInteger();
        final AtomicLong ended = new AtomicLong();
        final List<Long> starts = java.util.Collections.synchronizedList(new ArrayList<Long>());
        pool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (active.incrementAndGet() > 1) {
                    overlap.incrementAndGet();
                }
                starts.add(Long.valueOf(System.currentTimeMillis()));
                if (runs.incrementAndGet() == 1) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    ended.set(System.currentTimeMillis());
                }
                active.decrementAndGet();
            }
        }, 0, 20, TimeUnit.MILLISECONDS);
        // Bounded generously: a loaded runner can hold the timer's thread
        // for a while, and fifteen runs is what the count below needs.
        for (int i = 0; i < 2000 && runs.get() < 15; i++) {
            Thread.sleep(5);
        }
        pool.shutdownNow();
        assertEquals("runs overlapped", 0, overlap.get());
        int soon = 0;
        synchronized (starts) {
            for (Long at : starts) {
                if (at.longValue() >= ended.get() && at.longValue() - ended.get() < 40) {
                    soon++;
                }
            }
        }
        // About nine firings fell inside the 200 ms run. Dropped, at most
        // two would start within 40 ms of its end.
        assertTrue("the overrun's firings were not run late: " + soon + " of " + runs.get() + " runs, "
                + starts + " after the overrun ended at " + ended.get(), soon >= 5);
    }

    /// A task cancelled after it was handed to a busy pool, before it got a
    /// worker, never starts.
    @Test
    public void aTaskCancelledWhileWaitingForAWorkerNeverStarts() throws Exception {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
        final CountDownLatch busy = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        pool.execute(new Runnable() {
            @Override
            public void run() {
                busy.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        assertTrue(busy.await(10, TimeUnit.SECONDS));
        final AtomicInteger ran = new AtomicInteger();
        ScheduledFuture<?> waiting = pool.schedule(new Runnable() {
            @Override
            public void run() {
                ran.incrementAndGet();
            }
        }, 1, TimeUnit.MILLISECONDS);
        Thread.sleep(50);
        assertTrue(waiting.cancel(false));
        release.countDown();
        // Something after it on the same worker shows the queue drained.
        assertEquals("after", pool.submit(new Callable<String>() {
            @Override
            public String call() {
                return "after";
            }
        }).get(10, TimeUnit.SECONDS));
        assertEquals(0, ran.get());
        assertTrue(waiting.isCancelled());
        pool.shutdownNow();
    }

    @Test
    public void aZeroSizedScheduledPoolRunsItsWork() throws Exception {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(0);
        assertEquals("ran", pool.schedule(new Callable<String>() {
            @Override
            public String call() {
                return "ran";
            }
        }, 5, TimeUnit.MILLISECONDS).get(10, TimeUnit.SECONDS));
        pool.shutdownNow();
        try {
            Executors.newScheduledThreadPool(-1);
            fail("a negative size was accepted");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected);
        }
    }

    /// A fixed rate whose run takes longer than its period keeps going.
    @Test
    public void aFixedRateOutlivesARunLongerThanItsPeriod() throws Exception {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
        final AtomicInteger runs = new AtomicInteger();
        final java.util.concurrent.CountDownLatch five = new java.util.concurrent.CountDownLatch(5);
        pool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (runs.incrementAndGet() == 1) {
                    try {
                        Thread.sleep(120);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                five.countDown();
            }
        }, 0, 20, TimeUnit.MILLISECONDS);
        assertTrue("the schedule stopped after an overrun, at " + runs.get(),
                five.await(10, java.util.concurrent.TimeUnit.SECONDS));
        pool.shutdownNow();
    }

    /// Two threads adding keys to one node, each through an instance of its
    /// own: every key is kept, and a key may hold a line break.
    @Test
    public void preferenceKeysAddedFromSeveralThreadsAreAllIndexed() throws Exception {
        final String node = "/cn1/test/concurrent";
        Thread[] writers = new Thread[4];
        for (int t = 0; t < writers.length; t++) {
            final int id = t;
            writers[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 50; i++) {
                        Preferences.userRoot().node(node).put("k" + id + "." + i, "v");
                    }
                }
            });
            writers[t].start();
        }
        for (Thread w : writers) {
            w.join();
        }
        Preferences prefs = Preferences.userRoot().node(node);
        assertEquals(200, prefs.keys().length);
        // Every value too: the store under them is not made for several
        // threads, and a save that failed in it was only logged.
        for (int t = 0; t < writers.length; t++) {
            for (int i = 0; i < 50; i++) {
                assertEquals("k" + t + "." + i, "v", prefs.get("k" + t + "." + i, null));
            }
        }

        prefs.put("a\nb", "1");
        prefs.put("c\\nd", "2");
        List<String> keys = Arrays.asList(prefs.keys());
        assertTrue(keys.toString(), keys.contains("a\nb"));
        assertTrue(keys.toString(), keys.contains("c\\nd"));
        assertEquals(202, keys.size());
        prefs.remove("a\nb");
        assertFalse(Arrays.asList(prefs.keys()).contains("a\nb"));
        prefs.clear();
        assertEquals(0, prefs.keys().length);
        assertEquals("gone", prefs.get("c\\nd", "gone"));
    }

    /// A key may hold a `/` or a line break, and a node name a line break:
    /// none of it makes one entry another's.
    @Test
    public void preferenceKeysNeverAliasAnotherNodesEntries() throws Exception {
        Preferences a = Preferences.userRoot().node("/cn1/test/alias/a");
        Preferences ab = Preferences.userRoot().node("/cn1/test/alias/a/b");
        a.put("b/c", "x");
        ab.put("c", "y");
        a.put("\nkeys", "not the index");
        a.put("\n", "line break");
        assertEquals("x", a.get("b/c", null));
        assertEquals("y", ab.get("c", null));
        assertEquals("not the index", a.get("\nkeys", null));
        assertEquals("line break", a.get("\n", null));
        assertEquals(Arrays.asList("b/c", "\nkeys", "\n"), Arrays.asList(a.keys()));
        assertEquals(Arrays.asList("c"), Arrays.asList(ab.keys()));
        a.clear();
        ab.clear();
        assertEquals("y", ab.get("c", "y"));
        assertEquals(0, a.keys().length);
    }

    /// Desktop code sets properties from its workers; the JDK's table takes
    /// that, and a plain map written from several threads loses entries.
    @Test
    public void propertiesSetFromSeveralThreadsAreAllKept() throws Exception {
        final int each = 2000;
        Thread[] writers = new Thread[4];
        for (int t = 0; t < writers.length; t++) {
            final int id = t;
            writers[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < each; i++) {
                        JdkSystem.setProperty("cn1.test.concurrent." + id + "." + i, "v");
                    }
                }
            });
            writers[t].start();
        }
        for (Thread w : writers) {
            w.join();
        }
        for (int t = 0; t < writers.length; t++) {
            for (int i = 0; i < each; i++) {
                assertEquals("v", JdkSystem.clearProperty("cn1.test.concurrent." + t + "." + i));
            }
        }
    }

    /// A device's timer sleeps a thread through every delay it is given,
    /// cancelled or not, so a timeout per request was a thread per request
    /// until the deadline. A wait that is given up leaves the queue at
    /// once, and one thread serves whatever is left.
    /// A delay too long to add to the clock means never, not the past.
    @Test
    public void aDelayPastTheEndOfTheClockNeverFires() throws Exception {
        CompletableFuture<String> never = new CompletableFuture<String>().orTimeout(Long.MAX_VALUE,
                TimeUnit.MILLISECONDS);
        CompletableFuture<String> soon = new CompletableFuture<String>().completeOnTimeout("late", 30,
                TimeUnit.MILLISECONDS);
        assertEquals("late", soon.get(10, TimeUnit.SECONDS));
        assertFalse(never.isDone());
        assertTrue(never.complete("in time"));
    }

    @Test
    public void manyTimeoutsThatAreNotNeededHoldNoThread() throws Exception {
        int threads = Thread.activeCount();
        List<CompletableFuture<String>> all = new ArrayList<CompletableFuture<String>>();
        for (int i = 0; i < 500; i++) {
            all.add(new CompletableFuture<String>().orTimeout(1, TimeUnit.HOURS));
            all.add(new CompletableFuture<String>().completeOnTimeout("late", 1, TimeUnit.HOURS));
        }
        assertEquals(1000, Delays.pending());
        assertTrue("one thread waits for all of them", Thread.activeCount() <= threads + 1);
        for (CompletableFuture<String> f : all) {
            assertTrue(f.complete("in time"));
        }
        assertEquals(0, Delays.pending());
        // And the waits that are due still fire, in the order of their time.
        final List<String> order = Collections.synchronizedList(new ArrayList<String>());
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(3);
        for (final int millis : new int[] {120, 20, 70}) {
            Delays.after(millis, new Runnable() {
                @Override
                public void run() {
                    order.add(String.valueOf(millis));
                    done.countDown();
                }
            });
        }
        assertTrue(done.await(10, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(Arrays.asList("20", "70", "120"), order);
    }

    /// A timeout set for a future that completed in time is cancelled, and
    /// holds on to nothing: the timer keeps a cancelled task until its time.
    @Test
    public void aTimeoutNoLongerNeededLetsGoOfItsFuture() throws Exception {
        CompletableFuture<String> failing = new CompletableFuture<String>();
        assertSame(failing, failing.orTimeout(1, TimeUnit.HOURS));
        assertTrue(failing.complete("in time"));
        java.lang.ref.WeakReference<Object> first = new java.lang.ref.WeakReference<Object>(failing);
        failing = null;
        assertTrue("orTimeout still holds a future that completed", collected(first));

        CompletableFuture<String> defaulting = new CompletableFuture<String>();
        defaulting.completeOnTimeout("late", 1, TimeUnit.HOURS);
        assertTrue(defaulting.cancel(false));
        java.lang.ref.WeakReference<Object> second = new java.lang.ref.WeakReference<Object>(defaulting);
        defaulting = null;
        assertTrue("completeOnTimeout still holds a future that completed", collected(second));

        // And one that is needed still fires.
        assertEquals("late", new CompletableFuture<String>().completeOnTimeout("late", 30, TimeUnit.MILLISECONDS)
                .get(10, TimeUnit.SECONDS));
        try {
            new CompletableFuture<String>().orTimeout(30, TimeUnit.MILLISECONDS).get(10, TimeUnit.SECONDS);
            fail("The time passed");
        } catch (ExecutionException expected) {
            assertTrue(String.valueOf(expected.getCause()), expected.getCause() instanceof TimeoutException);
        }
    }

    /// The stage answered completes as the future does, and completing it
    /// leaves the future alone. That it is an ordinary future, not one that
    /// refuses to be completed, is documented on the method.
    @Test
    public void aMinimalCompletionStageIsACopy() throws Exception {
        CompletableFuture<String> source = new CompletableFuture<String>();
        CompletionStage<String> stage = source.minimalCompletionStage();
        assertTrue(stage.toCompletableFuture().complete("the stage's own"));
        assertFalse(source.isDone());
        assertTrue(source.complete("the source's"));
        assertEquals("the source's", source.get());

        CompletableFuture<String> second = new CompletableFuture<String>();
        CompletionStage<String> follows = second.minimalCompletionStage();
        second.complete("value");
        assertEquals("value", follows.toCompletableFuture().get(10, TimeUnit.SECONDS));
    }

    /// An executor is submitted to from several threads at once, and none of
    /// the submissions may be lost: every task runs, and the pool still
    /// knows each one that did not when it is told to stop.
    @Test
    public void submissionsFromSeveralThreadsAreAllAccounted() throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(3);
        final AtomicInteger ran = new AtomicInteger();
        final AtomicInteger failed = new AtomicInteger();
        Thread[] submitters = new Thread[8];
        for (int t = 0; t < submitters.length; t++) {
            submitters[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < 500; i++) {
                            pool.execute(new Runnable() {
                                @Override
                                public void run() {
                                    ran.incrementAndGet();
                                }
                            });
                        }
                    } catch (Throwable e) {
                        failed.incrementAndGet();
                    }
                }
            });
            submitters[t].start();
        }
        for (Thread submitter : submitters) {
            submitter.join(20000);
        }
        assertEquals(0, failed.get());
        terminate(pool);
        assertEquals(4000, ran.get());
    }

    /// A submission racing a shutdown may start a worker after the shutdown
    /// has looked for workers to stop; the pool must still terminate.
    @Test
    public void aPoolShutDownWhileItIsSubmittedToTerminates() throws Exception {
        for (int round = 0; round < 40; round++) {
            final ExecutorService pool = Executors.newFixedThreadPool(4);
            Thread[] submitters = new Thread[4];
            for (int t = 0; t < submitters.length; t++) {
                submitters[t] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (int i = 0; i < 4; i++) {
                                pool.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                    }
                                });
                            }
                        } catch (RejectedExecutionException expected) {
                            return;
                        }
                    }
                });
            }
            for (Thread t : submitters) {
                t.start();
            }
            pool.shutdown();
            for (Thread t : submitters) {
                t.join();
            }
            assertTrue("round " + round, pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    /// A fixed delay shorter than the time a run takes to wind up must not
    /// end the repetition: the next run is armed only once this one is over.
    @Test
    public void aVeryShortFixedDelayKeepsRepeating() throws Exception {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
        final AtomicInteger runs = new AtomicInteger();
        ScheduledFuture<?> spaced = pool.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                runs.incrementAndGet();
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
        long deadline = System.currentTimeMillis() + 15000;
        while (runs.get() < 300 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertTrue("stopped after " + runs.get() + " runs", runs.get() >= 300);
        assertFalse(spaced.isDone());
        spaced.cancel(false);
        pool.shutdownNow();
    }

    /// invokeAll interrupted while it waits cancels the tasks that have not
    /// finished, so none of them runs after it has returned.
    @Test
    public void anInterruptedInvokeAllCancelsWhatIsLeft() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);
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
        Thread.currentThread().interrupt();
        try {
            pool.invokeAll(tasks);
            fail("invokeAll returned although interrupted");
        } catch (InterruptedException expected) {
            assertFalse(Thread.currentThread().isInterrupted());
        } finally {
            release.countDown();
        }
        assertEquals("after", pool.submit(new Callable<String>() {
            @Override
            public String call() {
                return "after";
            }
        }).get(10, TimeUnit.SECONDS));
        assertEquals("a cancelled task ran", 0, later.get());
        pool.shutdownNow();
    }
}
